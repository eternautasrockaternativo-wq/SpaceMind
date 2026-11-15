package com.example.voice

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale
import java.util.UUID

private const val TAG = "SpaceVoiceEngine"

class SpaceVoiceEngine(private val context: Context) {

  private val mainHandler = Handler(Looper.getMainLooper())

  private val _isTtsReady = MutableStateFlow(false)
  val isTtsReady: StateFlow<Boolean> = _isTtsReady.asStateFlow()

  private val _isSpeaking = MutableStateFlow(false)
  val isSpeaking: StateFlow<Boolean> = _isSpeaking.asStateFlow()

  private val _isListening = MutableStateFlow(false)
  val isListening: StateFlow<Boolean> = _isListening.asStateFlow()

  private val _rmsLevel = MutableStateFlow(0f)
  val rmsLevel: StateFlow<Float> = _rmsLevel.asStateFlow()

  val isSpeechRecognitionAvailable: Boolean
    get() = runCatching { SpeechRecognizer.isRecognitionAvailable(context) }.getOrDefault(false)

  private var textToSpeech: TextToSpeech? = null
  private var speechRecognizer: SpeechRecognizer? = null
  private var activeLanguage: VoiceLanguage = VoiceLanguage.ES
  private var onSpeakCompletedCallback: (() -> Unit)? = null

  init {
    initTts()
  }

  private fun initTts() {
    runCatching {
      textToSpeech = TextToSpeech(context.applicationContext) { status ->
        if (status == TextToSpeech.SUCCESS) {
          applyTtsLocale(activeLanguage)
          textToSpeech?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) {
              mainHandler.post { _isSpeaking.value = true }
            }

            override fun onDone(utteranceId: String?) {
              mainHandler.post {
                _isSpeaking.value = false
                onSpeakCompletedCallback?.invoke()
                onSpeakCompletedCallback = null
              }
            }

            @Deprecated("Deprecated in Java")
            override fun onError(utteranceId: String?) {
              mainHandler.post {
                _isSpeaking.value = false
                onSpeakCompletedCallback = null
              }
            }

            override fun onError(utteranceId: String?, errorCode: Int) {
              mainHandler.post {
                _isSpeaking.value = false
                onSpeakCompletedCallback = null
              }
            }
          })
          _isTtsReady.value = true
        } else {
          Log.w(TAG, "TTS initialization returned status=$status")
          _isTtsReady.value = false
        }
      }
    }.onFailure { e ->
      Log.e(TAG, "TTS initialization failed", e)
      _isTtsReady.value = false
    }
  }

  fun setLanguage(language: VoiceLanguage) {
    activeLanguage = language
    if (_isTtsReady.value) {
      applyTtsLocale(language)
    }
  }

  private fun applyTtsLocale(language: VoiceLanguage) {
    val tts = textToSpeech ?: return
    val preferred = if (language == VoiceLanguage.ES) Locale("es", "ES") else Locale.US
    val res = tts.setLanguage(preferred)
    if (res == TextToSpeech.LANG_MISSING_DATA || res == TextToSpeech.LANG_NOT_SUPPORTED) {
      val fallback = if (language == VoiceLanguage.ES) Locale("es") else Locale.ENGLISH
      tts.setLanguage(fallback)
    }
    // Calm, warm, neutral aerospace voice profile
    tts.setPitch(0.98f)
    tts.setSpeechRate(0.94f)
  }

  fun speak(text: String, onDone: (() -> Unit)? = null) {
    stopListening()
    val tts = textToSpeech
    if (!_isTtsReady.value || tts == null) {
      onDone?.invoke()
      return
    }
    onSpeakCompletedCallback = onDone
    val utteranceId = UUID.randomUUID().toString()
    mainHandler.post {
      _isSpeaking.value = true
      val result = tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, utteranceId)
      if (result == TextToSpeech.ERROR) {
        _isSpeaking.value = false
        onSpeakCompletedCallback = null
        onDone?.invoke()
      }
    }
  }

  fun stopSpeaking() {
    onSpeakCompletedCallback = null
    mainHandler.post {
      runCatching { textToSpeech?.stop() }
      _isSpeaking.value = false
    }
  }

  fun startListening(
    language: VoiceLanguage,
    onPartial: (String) -> Unit,
    onResult: (String) -> Unit,
    onError: (String) -> Unit
  ) {
    stopSpeaking()
    if (!isSpeechRecognitionAvailable) {
      onError(
        if (language == VoiceLanguage.ES)
          "Reconocimiento de voz no disponible en este dispositivo. Usa los controles rápidos."
        else
          "Speech recognition unavailable on this device. Use quick-tap controls."
      )
      return
    }

    mainHandler.post {
      runCatching {
        speechRecognizer?.destroy()
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context.applicationContext).apply {
          setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {
              _isListening.value = true
              _rmsLevel.value = 0.15f
            }

            override fun onBeginningOfSpeech() {
              _isListening.value = true
            }

            override fun onRmsChanged(rmsdB: Float) {
              // Normalize typical speech RMS (-2..10 dB) into 0.05..1.0
              val normalized = ((rmsdB + 2f) / 12f).coerceIn(0.05f, 1f)
              _rmsLevel.value = normalized
            }

            override fun onBufferReceived(buffer: ByteArray?) {}

            override fun onEndOfSpeech() {
              _isListening.value = false
              _rmsLevel.value = 0f
            }

            override fun onError(error: Int) {
              _isListening.value = false
              _rmsLevel.value = 0f
              val msg = mapSpeechError(error, language)
              onError(msg)
            }

            override fun onResults(results: Bundle?) {
              _isListening.value = false
              _rmsLevel.value = 0f
              val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
              val best = matches?.firstOrNull()?.trim().orEmpty()
              if (best.isNotEmpty()) {
                onResult(best)
              } else {
                onError(
                  if (language == VoiceLanguage.ES) "No se detectó respuesta clara."
                  else "No clear response detected."
                )
              }
            }

            override fun onPartialResults(partialResults: Bundle?) {
              val partial = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                ?.firstOrNull()?.trim().orEmpty()
              if (partial.isNotEmpty()) {
                onPartial(partial)
              }
            }

            override fun onEvent(eventType: Int, params: Bundle?) {}
          })
        }

        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
          putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
          putExtra(RecognizerIntent.EXTRA_LANGUAGE, language.localeTag)
          putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
          putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
          // Silence detection window (~1.8s after speech stops)
          putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 1800L)
          putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS, 1500L)
        }
        speechRecognizer?.startListening(intent)
      }.onFailure { e ->
        Log.e(TAG, "Failed to start SpeechRecognizer", e)
        _isListening.value = false
        _rmsLevel.value = 0f
        onError(
          if (language == VoiceLanguage.ES) "No fue posible iniciar el micrófono. Usa la entrada manual."
          else "Could not start microphone. Use manual controls."
        )
      }
    }
  }

  fun stopListening() {
    mainHandler.post {
      runCatching {
        speechRecognizer?.stopListening()
        speechRecognizer?.cancel()
      }
      _isListening.value = false
      _rmsLevel.value = 0f
    }
  }

  fun pauseAll() {
    stopSpeaking()
    stopListening()
  }

  fun release() {
    pauseAll()
    mainHandler.post {
      runCatching { speechRecognizer?.destroy() }
      speechRecognizer = null
      runCatching { textToSpeech?.shutdown() }
      textToSpeech = null
    }
  }

  private fun mapSpeechError(error: Int, lang: VoiceLanguage): String {
    val es = lang == VoiceLanguage.ES
    return when (error) {
      SpeechRecognizer.ERROR_NO_MATCH ->
        if (es) "No te escuché con claridad. Pulsa el micrófono o elige una opción."
        else "Didn't catch that clearly. Tap mic or select an option."
      SpeechRecognizer.ERROR_SPEECH_TIMEOUT ->
        if (es) "Silencio detectado. Pulsa el micrófono cuando estés listo."
        else "Silence detected. Tap the microphone when ready."
      SpeechRecognizer.ERROR_NETWORK, SpeechRecognizer.ERROR_NETWORK_TIMEOUT ->
        if (es) "Red de voz limitada. Usa los botones rápidos o reintenta."
        else "Voice network limited. Use quick buttons or retry."
      SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS ->
        if (es) "Se requiere permiso de micrófono para usar voz."
        else "Microphone permission required for voice input."
      SpeechRecognizer.ERROR_RECOGNIZER_BUSY ->
        if (es) "Micrófono ocupado. Intenta nuevamente."
        else "Microphone busy. Please try again."
      else ->
        if (es) "Error de reconocimiento. Usa el micrófono o botones rápidos."
        else "Recognition error. Use mic or quick buttons."
    }
  }
}
