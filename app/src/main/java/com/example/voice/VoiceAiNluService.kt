package com.example.voice

import android.util.Log
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.Schema
import com.google.firebase.ai.type.content
import com.google.firebase.ai.type.generationConfig
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.withTimeout
import org.json.JSONObject

private const val TAG = "VoiceAiNluService"

class VoiceAiNluService {

  private val nluSchema = Schema.obj(
    properties = mapOf(
      "intent" to Schema.enumeration(
        values = listOf("ANSWER", "REPEAT", "CORRECT", "PAUSE", "FINISH", "CONTINUE"),
        description = "Detected astronaut intent or navigation command"
      ),
      "numericValue" to Schema.double(
        description = "Normalized clinical value 0.0 to 10.0. For 0-10 scales use stated score. For severity: 0=none, 3=mild, 6=moderate, 9=severe. For emotion/sleep/concentration: 10=best, 0=worst."
      ),
      "severityLevel" to Schema.integer(
        description = "0 = none/optimal, 1 = mild, 2 = moderate, 3 = severe/high"
      ),
      "medicalRedFlag" to Schema.boolean(
        description = "True ONLY if chest pain/oppression, severe shortness of breath, fainting/syncope, acute confusion, or urgent physical distress is reported."
      ),
      "briefReply" to Schema.string(
        description = "Ultra-brief, calm, warm, non-diagnostic acknowledgment (max 18 words) in the active language."
      )
    )
  )

  private val model by lazy {
    Firebase.ai.generativeModel(
      modelName = "gemini-3.8-flash",
      generationConfig = generationConfig {
        responseMimeType = "application/json"
        responseSchema = nluSchema
        temperature = 0.2f
      },
      systemInstruction = content {
        text(
          """
          You are SPACE MIND VOICE AI, an aerospace psychological monitoring assistant.
          Strict clinical safety rules:
          1. NEVER diagnose psychiatric or medical disorders.
          2. NEVER attribute physical symptoms (chest pain, dyspnea, dizziness, palpitations) automatically to anxiety.
          3. If chest pain/oppression, severe difficulty breathing, fainting, or acute confusion is mentioned, ALWAYS set medicalRedFlag=true and advise contacting Mission Medical Control calmly.
          4. Keep briefReply calm, warm, professional, neutral, and under 18 words in the requested language (Spanish or English).
          5. Extract structured scores strictly matching the question context.
          """.trimIndent()
        )
      }
    )
  }

  suspend fun interpretEvaluationResponse(
    question: VoiceQuestion,
    transcript: String,
    language: VoiceLanguage
  ): VoiceNluResult {
    // 1. Always run deterministic local safety & command check first
    val localResult = parseLocally(question, transcript, language)
    if (localResult.intent != VoiceCommandIntent.ANSWER || localResult.medicalRedFlag) {
      return localResult
    }

    // 2. Try Firebase AI Logic with realistic timeout; fall back cleanly offline
    return try {
      val langName = if (language == VoiceLanguage.ES) "Spanish" else "English"
      val prompt = """
        Language: $langName
        Question ID: ${question.id} (${question.inputKind})
        Question asked: "${question.prompt(language)}"
        Astronaut spoken response: "$transcript"
        Extract the structured intent, score (0-10), severity (0-3), medicalRedFlag, and a brief calm acknowledgment in $langName.
      """.trimIndent()

      val response = withTimeout(12_000L) {
        model.generateContent(prompt)
      }
      val raw = response.text ?: throw IllegalStateException("Empty AI response")
      val json = JSONObject(raw)

      val intentStr = json.optString("intent", "ANSWER")
      val intent = runCatching { VoiceCommandIntent.valueOf(intentStr) }.getOrDefault(VoiceCommandIntent.ANSWER)
      val numeric = json.optDouble("numericValue", localResult.numericValue.toDouble()).toFloat().coerceIn(0f, 10f)
      val severity = json.optInt("severityLevel", localResult.severityLevel).coerceIn(0, 3)
      val redFlag = json.optBoolean("medicalRedFlag", false) || localResult.medicalRedFlag
      val reply = json.optString("briefReply", localResult.briefReply).ifBlank { localResult.briefReply }

      VoiceNluResult(
        numericValue = numeric,
        severityLevel = severity,
        intent = intent,
        medicalRedFlag = redFlag,
        briefReply = if (redFlag) VoiceProtocolCatalog.medicalAlertVoiceMessage(language) else reply,
        usedCloudAi = true
      )
    } catch (e: TimeoutCancellationException) {
      Log.w(TAG, "AI interpretation timed out; using deterministic local parser", e)
      localResult.copy(usedCloudAi = false)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      Log.e(TAG, "Firebase AI unavailable; falling back to local parser", e)
      localResult.copy(usedCloudAi = false)
    }
  }

  fun parseInterventionCommand(transcript: String): VoiceCommandIntent? {
    val norm = transcript.lowercase().trim()
    return when {
      norm.contains("paus") || norm.contains("espera") || norm.contains("stop") || norm.contains("wait") ->
        VoiceCommandIntent.PAUSE
      norm.contains("repet") || norm.contains("otra vez") || norm.contains("again") ->
        VoiceCommandIntent.REPEAT
      norm.contains("finaliz") || norm.contains("termin") || norm.contains("acabar") || norm.contains("finish") || norm.contains("end") ->
        VoiceCommandIntent.FINISH
      norm.contains("continu") || norm.contains("sigu") || norm.contains("adelante") || norm.contains("next") ->
        VoiceCommandIntent.CONTINUE
      else -> null
    }
  }

  fun parseLocally(
    question: VoiceQuestion,
    transcript: String,
    language: VoiceLanguage
  ): VoiceNluResult {
    val norm = transcript.lowercase().trim()

    // Voice navigation commands
    val navIntent = when {
      norm.contains("repet") || norm.contains("otra vez") || norm.contains("repeat") -> VoiceCommandIntent.REPEAT
      norm.contains("correg") || norm.contains("atrás") || norm.contains("volver") || norm.contains("correct") || norm.contains("back") -> VoiceCommandIntent.CORRECT
      norm.contains("paus") || norm.contains("detener") || norm.contains("pause") -> VoiceCommandIntent.PAUSE
      norm.contains("finaliz") || norm.contains("termin") || norm.contains("finish") || norm.contains("end") -> VoiceCommandIntent.FINISH
      else -> VoiceCommandIntent.ANSWER
    }
    if (navIntent != VoiceCommandIntent.ANSWER) {
      return VoiceNluResult(
        numericValue = 0f,
        severityLevel = 0,
        intent = navIntent,
        medicalRedFlag = false,
        briefReply = if (language == VoiceLanguage.ES) "De acuerdo." else "Understood.",
        usedCloudAi = false
      )
    }

    // Strict deterministic clinical red-flag scan (never auto-attributed to anxiety)
    val redFlagTerms = listOf(
      "dolor en el pecho", "dolor torácico", "dolor toracico", "opresión en el pecho", "opresion en el pecho",
      "me duele el pecho", "no puedo respirar", "me ahogo", "ahogo", "dificultad importante",
      "desmayo", "desmayar", "desvanec", "confusión", "confusion", "desorientad",
      "chest pain", "chest tightness", "can't breathe", "cannot breathe", "faint", "passed out", "confused"
    )
    val isRedFlag = redFlagTerms.any { norm.contains(it) } ||
      (question.id == "Q_DYSPNEA" && (norm.contains("mucha") || norm.contains("severa") || norm.contains("intensa") || norm.contains("severe")))

    if (isRedFlag) {
      return VoiceNluResult(
        numericValue = 9f,
        severityLevel = 3,
        intent = VoiceCommandIntent.ANSWER,
        medicalRedFlag = true,
        briefReply = VoiceProtocolCatalog.medicalAlertVoiceMessage(language),
        usedCloudAi = false
      )
    }

    // Extract explicit number 0..10 if spoken
    val explicitNumber = extractSpokenNumber(norm)

    val (numeric, severity) = when (question.inputKind) {
      VoiceInputKind.SCALE_0_10 -> {
        val score = explicitNumber ?: when {
          containsAny(norm, "nada", "cero", "excelente", "perfecto", "none", "zero", "great", "perfect") ->
            if (question.id == "Q_ANXIETY") 1f else 9f
          containsAny(norm, "bajo", "poco", "bien", "tranquilo", "low", "good", "calm") ->
            if (question.id == "Q_ANXIETY") 2f else 8f
          containsAny(norm, "medio", "moderado", "regular", "normal", "moderate", "okay", "average") -> 5f
          containsAny(norm, "alto", "mucho", "mal", "bastante", "high", "bad", "poor", "a lot") ->
            if (question.id == "Q_ANXIETY") 7f else 3f
          containsAny(norm, "extremo", "máximo", "horrible", "pésimo", "extreme", "terrible") ->
            if (question.id == "Q_ANXIETY") 9f else 1f
          else -> 5f
        }
        val sev = if (question.id == "Q_ANXIETY") {
          when {
            score >= 7f -> 3
            score >= 4.5f -> 2
            score >= 2.5f -> 1
            else -> 0
          }
        } else {
          when {
            score <= 3f -> 3
            score <= 5.5f -> 2
            score <= 7.5f -> 1
            else -> 0
          }
        }
        score to sev
      }

      VoiceInputKind.EMOTIONAL_STATE -> {
        val score = explicitNumber ?: when {
          containsAny(norm, "muy bien", "tranquilo", "calma", "estable", "positivo", "bien", "calm", "good", "stable", "great") -> 8.5f
          containsAny(norm, "cansad", "tenso", "algo", "regular", "neutro", "tired", "tense", "okay", "neutral") -> 6f
          containsAny(norm, "ansios", "nervios", "preocup", "triste", "agobi", "anxious", "nervous", "worried", "overwhelmed") -> 3.5f
          containsAny(norm, "mal", "desbord", "pánico", "panico", "bad", "panicking") -> 2f
          else -> 7f
        }
        val sev = when {
          score <= 3f -> 3
          score <= 5.5f -> 2
          score <= 7.5f -> 1
          else -> 0
        }
        score to sev
      }

      VoiceInputKind.SYMPTOM_SEVERITY, VoiceInputKind.RED_FLAG_CHECK -> {
        when {
          explicitNumber != null -> {
            val s = when {
              explicitNumber >= 7f -> 3
              explicitNumber >= 4f -> 2
              explicitNumber >= 1f -> 1
              else -> 0
            }
            explicitNumber to s
          }
          containsAny(norm, "no", "ningun", "nada", "negativo", "normal", "ausente", "none", "nope") -> 0f to 0
          containsAny(norm, "poco", "leve", "ligero", "algo", "ocasional", "a veces", "mild", "slight", "little") -> 3f to 1
          containsAny(norm, "sí", "si", "bastante", "moderado", "frecuente", "yes", "moderate", "often") -> 6f to 2
          containsAny(norm, "mucho", "fuerte", "intenso", "grave", "severo", "severe", "strong", "intense") -> 8.5f to 3
          else -> 2f to 1
        }
      }
    }

    val ack = if (language == VoiceLanguage.ES) {
      "Registrado. Pasemos a la siguiente pregunta."
    } else {
      "Recorded. Moving to the next question."
    }

    return VoiceNluResult(
      numericValue = numeric,
      severityLevel = severity,
      intent = VoiceCommandIntent.ANSWER,
      medicalRedFlag = false,
      briefReply = ack,
      usedCloudAi = false
    )
  }

  private fun containsAny(text: String, vararg terms: String): Boolean = terms.any { text.contains(it) }

  private fun extractSpokenNumber(norm: String): Float? {
    Regex("""\b(10|[0-9])([.,][0-9])?\b""").find(norm)?.let { match ->
      return match.value.replace(',', '.').toFloatOrNull()?.coerceIn(0f, 10f)
    }
    val wordMap = mapOf(
      "cero" to 0f, "zero" to 0f,
      "uno" to 1f, "una" to 1f, "one" to 1f,
      "dos" to 2f, "two" to 2f,
      "tres" to 3f, "three" to 3f,
      "cuatro" to 4f, "four" to 4f,
      "cinco" to 5f, "five" to 5f,
      "seis" to 6f, "six" to 6f,
      "siete" to 7f, "seven" to 7f,
      "ocho" to 8f, "eight" to 8f,
      "nueve" to 9f, "nine" to 9f,
      "diez" to 10f, "ten" to 10f
    )
    wordMap.forEach { (word, value) ->
      if (Regex("""\b$word\b""").containsMatchIn(norm)) return value
    }
    return null
  }
}
