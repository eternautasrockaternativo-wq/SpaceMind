package com.example.voice

enum class VoiceLanguage(val code: String, val label: String, val localeTag: String) {
  ES("es", "ES", "es-ES"),
  EN("en", "EN", "en-US")
}

enum class VoicePhase {
  IDLE,
  SPEAKING,
  LISTENING,
  PROCESSING,
  PAUSED,
  SUMMARY,
  MEDICAL_ALERT
}

enum class VoiceInputKind {
  EMOTIONAL_STATE,
  SCALE_0_10,
  SYMPTOM_SEVERITY,
  RED_FLAG_CHECK
}

enum class VoiceCommandIntent {
  ANSWER,
  REPEAT,
  CORRECT,
  PAUSE,
  FINISH,
  CONTINUE
}

data class VoiceQuestion(
  val id: String,
  val index: Int,
  val shortTitleEs: String,
  val shortTitleEn: String,
  val promptEs: String,
  val promptEn: String,
  val inputKind: VoiceInputKind,
  val linkedSymptomId: String? = null
) {
  fun title(lang: VoiceLanguage): String = if (lang == VoiceLanguage.ES) shortTitleEs else shortTitleEn
  fun prompt(lang: VoiceLanguage): String = if (lang == VoiceLanguage.ES) promptEs else promptEn
}

data class VoiceNluResult(
  val numericValue: Float,
  val severityLevel: Int, // 0=none/low, 1=mild, 2=moderate, 3=high
  val intent: VoiceCommandIntent,
  val medicalRedFlag: Boolean,
  val briefReply: String,
  val usedCloudAi: Boolean
)

data class VoiceEvaluationUiState(
  val isActive: Boolean = false,
  val language: VoiceLanguage = VoiceLanguage.ES,
  val phase: VoicePhase = VoicePhase.IDLE,
  val currentIndex: Int = 0,
  val questions: List<VoiceQuestion> = VoiceProtocolCatalog.evaluationQuestions,
  val answers: Map<String, Float> = emptyMap(),
  val transcripts: Map<String, String> = emptyMap(),
  val lastTranscript: String = "",
  val aiFeedback: String = "",
  val errorNotice: String? = null,
  val isOfflineMode: Boolean = false,
  val medicalRedFlagTriggered: Boolean = false,
  val hasMicPermission: Boolean = false,
  val isSpeechRecognitionAvailable: Boolean = true,
  val isTtsReady: Boolean = false,
  val rmsLevel: Float = 0f,
  val recommendedInterventionId: String = "INT-01"
)

data class VoiceInterventionUiState(
  val isVoiceModeEnabled: Boolean = false,
  val isSpeaking: Boolean = false,
  val isListening: Boolean = false,
  val isPaused: Boolean = false,
  val statusText: String = "",
  val lastHeardCommand: String = "",
  val rmsLevel: Float = 0f
)

object VoiceProtocolCatalog {
  val evaluationQuestions: List<VoiceQuestion> = listOf(
    VoiceQuestion(
      id = "Q_EMOTION",
      index = 0,
      shortTitleEs = "Estado emocional",
      shortTitleEn = "Emotional state",
      promptEs = "Hola. Iniciemos tu chequeo breve. ¿Cómo te sientes emocionalmente en este momento?",
      promptEn = "Hello. Let's begin your brief check-in. How are you feeling emotionally right now?",
      inputKind = VoiceInputKind.EMOTIONAL_STATE
    ),
    VoiceQuestion(
      id = "Q_ANXIETY",
      index = 1,
      shortTitleEs = "Ansiedad actual (0-10)",
      shortTitleEn = "Current anxiety (0-10)",
      promptEs = "Del 0 al 10, ¿cuál es tu nivel actual de ansiedad o tensión interna?",
      promptEn = "From 0 to 10, what is your current level of anxiety or inner tension?",
      inputKind = VoiceInputKind.SCALE_0_10
    ),
    VoiceQuestion(
      id = "Q_PALPITATIONS",
      index = 2,
      shortTitleEs = "Palpitaciones o pulso",
      shortTitleEn = "Palpitations or pulse",
      promptEs = "¿Percibes palpitaciones o cambios inusuales en tu pulso ahora?",
      promptEn = "Do you notice palpitations or unusual changes in your pulse right now?",
      inputKind = VoiceInputKind.SYMPTOM_SEVERITY,
      linkedSymptomId = "SYM-CV-01"
    ),
    VoiceQuestion(
      id = "Q_DYSPNEA",
      index = 3,
      shortTitleEs = "Falta de aire",
      shortTitleEn = "Shortness of breath",
      promptEs = "¿Sientes falta de aire o dificultad al respirar?",
      promptEn = "Do you feel short of breath or any difficulty breathing?",
      inputKind = VoiceInputKind.RED_FLAG_CHECK,
      linkedSymptomId = "SYM-RS-01"
    ),
    VoiceQuestion(
      id = "Q_TENSION",
      index = 4,
      shortTitleEs = "Tensión muscular",
      shortTitleEn = "Muscle tension",
      promptEs = "¿Notas rigidez o tensión muscular en cuello, espalda o manos?",
      promptEn = "Do you notice muscle stiffness or tension in your neck, back, or hands?",
      inputKind = VoiceInputKind.SYMPTOM_SEVERITY,
      linkedSymptomId = "SYM-MN-01"
    ),
    VoiceQuestion(
      id = "Q_DIZZINESS_ACUTE",
      index = 5,
      shortTitleEs = "Mareos o molestias físicas",
      shortTitleEn = "Dizziness or physical discomfort",
      promptEs = "¿Presentas mareos, dolor en el pecho, confusión o sensación de desmayo?",
      promptEn = "Are you experiencing dizziness, chest pain, confusion, or faintness?",
      inputKind = VoiceInputKind.RED_FLAG_CHECK,
      linkedSymptomId = "SYM-MN-03"
    ),
    VoiceQuestion(
      id = "Q_SLEEP",
      index = 6,
      shortTitleEs = "Calidad del sueño (0-10)",
      shortTitleEn = "Sleep quality (0-10)",
      promptEs = "Del 0 al 10, ¿cómo evaluarias la calidad de tu último descanso?",
      promptEn = "From 0 to 10, how would you rate the quality of your recent sleep?",
      inputKind = VoiceInputKind.SCALE_0_10
    ),
    VoiceQuestion(
      id = "Q_WORRIES",
      index = 7,
      shortTitleEs = "Preocupaciones",
      shortTitleEn = "Worries",
      promptEs = "¿Tienes preocupaciones intensas o pensamientos repetitivos difíciles de detener?",
      promptEn = "Are you having intense worries or repetitive thoughts that are hard to stop?",
      inputKind = VoiceInputKind.SYMPTOM_SEVERITY
    ),
    VoiceQuestion(
      id = "Q_IRRITABILITY",
      index = 8,
      shortTitleEs = "Irritabilidad",
      shortTitleEn = "Irritability",
      promptEs = "¿Has sentido irritabilidad o frustración con la rutina o la tripulación?",
      promptEn = "Have you felt irritability or frustration with mission routine or crew?",
      inputKind = VoiceInputKind.SYMPTOM_SEVERITY
    ),
    VoiceQuestion(
      id = "Q_ISOLATION",
      index = 9,
      shortTitleEs = "Sensación de aislamiento",
      shortTitleEn = "Feeling of isolation",
      promptEs = "¿Percibes sensación de aislamiento o distancia emocional significativa?",
      promptEn = "Do you feel significant isolation or emotional distance?",
      inputKind = VoiceInputKind.SYMPTOM_SEVERITY
    ),
    VoiceQuestion(
      id = "Q_CONCENTRATION",
      index = 10,
      shortTitleEs = "Concentración (0-10)",
      shortTitleEn = "Concentration (0-10)",
      promptEs = "Por último, del 0 al 10, ¿cómo está tu capacidad de concentración hoy?",
      promptEn = "Finally, from 0 to 10, how is your ability to concentrate today?",
      inputKind = VoiceInputKind.SCALE_0_10
    )
  )

  fun welcomeMessage(lang: VoiceLanguage): String = if (lang == VoiceLanguage.ES) {
    "SPACE MIND activo. Evaluación breve iniciada. Puedes responder con tu voz o tocar una opción en pantalla."
  } else {
    "SPACE MIND active. Brief check-in started. Speak your answer or tap an option on screen."
  }

  fun medicalAlertVoiceMessage(lang: VoiceLanguage): String = if (lang == VoiceLanguage.ES) {
    "Atención. Reportaste un síntoma físico prioritario. No lo atribuimos a ansiedad. Por protocolo de seguridad, contacta de inmediato con Control Médico de la misión."
  } else {
    "Attention. You reported a priority physical symptom. We do not attribute this to anxiety. Per mission safety protocol, contact Flight Medical Control immediately."
  }

  fun summaryVoiceMessage(levelLabel: String, interventionTitle: String, lang: VoiceLanguage): String = if (lang == VoiceLanguage.ES) {
    "Evaluación completada. Estado actual: $levelLabel. Te recomiendo iniciar: $interventionTitle."
  } else {
    "Check-in complete. Current state: $levelLabel. Recommended protocol: $interventionTitle."
  }
}
