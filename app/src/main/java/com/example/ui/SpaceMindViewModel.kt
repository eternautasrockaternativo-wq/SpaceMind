package com.example.ui

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.SpaceMindRepository
import com.example.model.*
import com.example.voice.*

enum class AppScreen {
  DASHBOARD,
  VOICE_EVALUATION,
  LEVEL_1_BASELINE,
  LEVEL_2_SOMATIC,
  LEVEL_3_COGNITIVE,
  LEVEL_4_CLASSIFICATION,
  LEVEL_5_INTERVENTIONS,
  HISTORY_ANALYTICS
}

class SpaceMindViewModel(
  val repository: SpaceMindRepository = SpaceMindRepository()
) : ViewModel() {

  private val _currentScreen = MutableStateFlow(AppScreen.DASHBOARD)
  val currentScreen: StateFlow<AppScreen> = _currentScreen.asStateFlow()

  val astronautProfile = repository.astronautProfile
  val telemetry = repository.telemetry
  val currentAnxiety = repository.currentAnxiety
  val currentSleepQuality = repository.currentSleepQuality
  val baseline = repository.baseline
  val somaticSymptoms = repository.somaticSymptoms
  val thoughtRecords = repository.thoughtRecords
  val sessionLogs = repository.sessionLogs
  val interventions = repository.interventions
  val cognitiveDistortions = repository.cognitiveDistortions

  // Active intervention session
  private val _activeIntervention = MutableStateFlow<InterventionProgram?>(null)
  val activeIntervention: StateFlow<InterventionProgram?> = _activeIntervention.asStateFlow()

  private val _exerciseStep = MutableStateFlow(0)
  val exerciseStep: StateFlow<Int> = _exerciseStep.asStateFlow()

  private val _timerSeconds = MutableStateFlow(0)
  val timerSeconds: StateFlow<Int> = _timerSeconds.asStateFlow()

  private val _isTimerRunning = MutableStateFlow(false)
  val isTimerRunning: StateFlow<Boolean> = _isTimerRunning.asStateFlow()

  private val _preExerciseAnxiety = MutableStateFlow(5f)
  val preExerciseAnxiety: StateFlow<Float> = _preExerciseAnxiety.asStateFlow()

  private val _postExerciseAnxiety = MutableStateFlow(2f)
  val postExerciseAnxiety: StateFlow<Float> = _postExerciseAnxiety.asStateFlow()

  // Voice AI Evaluation & Voice Intervention states
  private val voiceAiService = VoiceAiNluService()
  private var voiceEngine: SpaceVoiceEngine? = null
  private var rmsObserverJob: Job? = null
  private var ttsObserverJob: Job? = null

  private val _voiceEvalState = MutableStateFlow(VoiceEvaluationUiState())
  val voiceEvalState: StateFlow<VoiceEvaluationUiState> = _voiceEvalState.asStateFlow()

  private val _voiceInterventionState = MutableStateFlow(VoiceInterventionUiState())
  val voiceInterventionState: StateFlow<VoiceInterventionUiState> = _voiceInterventionState.asStateFlow()

  // Dialogs
  private val _showMedicalProtocol = MutableStateFlow(false)
  val showMedicalProtocol: StateFlow<Boolean> = _showMedicalProtocol.asStateFlow()

  private val _showPrivacyDialog = MutableStateFlow(false)
  val showPrivacyDialog: StateFlow<Boolean> = _showPrivacyDialog.asStateFlow()

  private val _showNewThoughtDialog = MutableStateFlow(false)
  val showNewThoughtDialog: StateFlow<Boolean> = _showNewThoughtDialog.asStateFlow()

  // Selected Somatic Category
  private val _selectedSomaticCategory = MutableStateFlow(SomaticCategory.CARDIOVASCULAR)
  val selectedSomaticCategory: StateFlow<SomaticCategory> = _selectedSomaticCategory.asStateFlow()

  private var timerJob: Job? = null

  init {
    viewModelScope.launch {
      while (true) {
        delay(4000)
        repository.refreshTelemetrySimulation()
      }
    }
  }

  fun attachVoiceEngine(engine: SpaceVoiceEngine) {
    if (voiceEngine != null) return
    voiceEngine = engine
    _voiceEvalState.update {
      it.copy(
        isSpeechRecognitionAvailable = engine.isSpeechRecognitionAvailable,
        isTtsReady = engine.isTtsReady.value
      )
    }

    ttsObserverJob?.cancel()
    ttsObserverJob = viewModelScope.launch {
      engine.isTtsReady.collect { ready ->
        _voiceEvalState.update { it.copy(isTtsReady = ready) }
      }
    }

    rmsObserverJob?.cancel()
    rmsObserverJob = viewModelScope.launch {
      engine.rmsLevel.collect { rms ->
        _voiceEvalState.update { it.copy(rmsLevel = rms) }
        _voiceInterventionState.update { it.copy(rmsLevel = rms) }
      }
    }
  }

  fun onAppPaused() {
    voiceEngine?.pauseAll()
    _voiceEvalState.update { state ->
      if (state.phase == VoicePhase.LISTENING || state.phase == VoicePhase.SPEAKING) {
        state.copy(phase = VoicePhase.PAUSED, rmsLevel = 0f)
      } else state
    }
    _voiceInterventionState.update {
      it.copy(isListening = false, isSpeaking = false, rmsLevel = 0f)
    }
  }

  override fun onCleared() {
    super.onCleared()
    voiceEngine?.release()
  }

  fun navigateTo(screen: AppScreen) {
    if (screen != AppScreen.VOICE_EVALUATION) {
      voiceEngine?.stopListening()
      voiceEngine?.stopSpeaking()
    }
    _currentScreen.value = screen
  }

  fun setSomaticCategory(cat: SomaticCategory) {
    _selectedSomaticCategory.value = cat
  }

  fun updateSymptom(
    symptomId: String,
    isReported: Boolean,
    intensity: Float,
    frequency: String = "Esporádico",
    duration: String = "5-15 min",
    context: String = "En reposo",
    triggers: String = "Carga de trabajo"
  ) {
    repository.updateSymptom(symptomId, isReported, intensity, frequency, duration, context, triggers)
  }

  fun updateAnxiety(score: Float) {
    repository.updateCurrentAnxiety(score)
  }

  fun updateSleep(score: Float) {
    repository.updateCurrentSleepQuality(score)
  }

  fun updateBaseline(newBaseline: BaselineAssessment) {
    repository.updateBaseline(newBaseline)
  }

  fun getOperationalClassification(): StateClassification {
    return repository.calculateOperationalState()
  }

  // ==================== VOICE AI EVALUATION PROTOCOL ====================

  fun setVoiceLanguage(language: VoiceLanguage) {
    voiceEngine?.setLanguage(language)
    _voiceEvalState.update { it.copy(language = language, errorNotice = null) }
  }

  fun updateMicPermission(granted: Boolean) {
    _voiceEvalState.update { it.copy(hasMicPermission = granted) }
  }

  fun startVoiceEvaluation() {
    val lang = _voiceEvalState.value.language
    voiceEngine?.setLanguage(lang)
    _voiceEvalState.update {
      it.copy(
        isActive = true,
        phase = VoicePhase.SPEAKING,
        currentIndex = 0,
        answers = emptyMap(),
        transcripts = emptyMap(),
        lastTranscript = "",
        aiFeedback = VoiceProtocolCatalog.welcomeMessage(lang),
        errorNotice = null,
        medicalRedFlagTriggered = false
      )
    }
    _currentScreen.value = AppScreen.VOICE_EVALUATION
    speakQuestionAt(0, includeWelcome = true)
  }

  private fun speakQuestionAt(index: Int, includeWelcome: Boolean = false) {
    val state = _voiceEvalState.value
    val question = state.questions.getOrNull(index) ?: return
    val lang = state.language
    val textToSpeak = if (includeWelcome) {
      "${VoiceProtocolCatalog.welcomeMessage(lang)} ${question.prompt(lang)}"
    } else {
      question.prompt(lang)
    }

    _voiceEvalState.update {
      it.copy(
        currentIndex = index,
        phase = VoicePhase.SPEAKING,
        errorNotice = null
      )
    }

    val engine = voiceEngine
    if (engine != null && state.isTtsReady) {
      engine.speak(textToSpeak) {
        // After AI finishes speaking, enter IDLE ready for astronaut mic tap or quick button
        _voiceEvalState.update { cur ->
          if (cur.phase == VoicePhase.SPEAKING) cur.copy(phase = VoicePhase.IDLE) else cur
        }
      }
    } else {
      _voiceEvalState.update { it.copy(phase = VoicePhase.IDLE) }
    }
  }

  fun toggleVoiceEvaluationMic() {
    val state = _voiceEvalState.value
    if (state.phase == VoicePhase.LISTENING) {
      stopVoiceEvaluationMic()
      return
    }
    if (!state.hasMicPermission) {
      _voiceEvalState.update {
        it.copy(
          errorNotice = if (state.language == VoiceLanguage.ES)
            "Activa el permiso de micrófono para responder por voz."
          else "Enable microphone permission for voice input."
        )
      }
      return
    }
    val engine = voiceEngine ?: return
    val currentQuestion = state.questions.getOrNull(state.currentIndex) ?: return

    _voiceEvalState.update {
      it.copy(phase = VoicePhase.LISTENING, errorNotice = null, lastTranscript = "")
    }

    engine.startListening(
      language = state.language,
      onPartial = { partial ->
        _voiceEvalState.update { it.copy(lastTranscript = partial) }
      },
      onResult = { finalTranscript ->
        processSpokenEvaluationResponse(currentQuestion, finalTranscript)
      },
      onError = { errMsg ->
        _voiceEvalState.update {
          it.copy(phase = VoicePhase.IDLE, errorNotice = errMsg, rmsLevel = 0f)
        }
      }
    )
  }

  fun stopVoiceEvaluationMic() {
    voiceEngine?.stopListening()
    _voiceEvalState.update {
      if (it.phase == VoicePhase.LISTENING) it.copy(phase = VoicePhase.IDLE, rmsLevel = 0f) else it
    }
  }

  fun submitManualOrTextResponse(transcriptOrLabel: String, explicitScore: Float? = null, isRedFlag: Boolean = false) {
    val state = _voiceEvalState.value
    val question = state.questions.getOrNull(state.currentIndex) ?: return
    voiceEngine?.pauseAll()

    if (isRedFlag) {
      triggerVoiceMedicalAlert(question, transcriptOrLabel)
      return
    }

    if (explicitScore != null) {
      val lang = state.language
      val ack = if (lang == VoiceLanguage.ES) "Registrado." else "Recorded."
      recordAnswerAndAdvance(
        question = question,
        score = explicitScore.coerceIn(0f, 10f),
        transcript = transcriptOrLabel,
        feedback = ack,
        usedCloudAi = !state.isOfflineMode
      )
    } else if (transcriptOrLabel.isNotBlank()) {
      processSpokenEvaluationResponse(question, transcriptOrLabel)
    }
  }

  private fun processSpokenEvaluationResponse(question: VoiceQuestion, transcript: String) {
    val lang = _voiceEvalState.value.language
    _voiceEvalState.update {
      it.copy(phase = VoicePhase.PROCESSING, lastTranscript = transcript, errorNotice = null)
    }

    viewModelScope.launch {
      try {
        val result = voiceAiService.interpretEvaluationResponse(question, transcript, lang)
        _voiceEvalState.update { it.copy(isOfflineMode = !result.usedCloudAi) }

        when (result.intent) {
          VoiceCommandIntent.REPEAT -> repeatCurrentVoiceQuestion()
          VoiceCommandIntent.CORRECT -> correctPreviousVoiceQuestion()
          VoiceCommandIntent.PAUSE -> pauseOrResumeVoiceEvaluation()
          VoiceCommandIntent.FINISH -> finishVoiceEvaluationNow()
          VoiceCommandIntent.ANSWER, VoiceCommandIntent.CONTINUE -> {
            if (result.medicalRedFlag) {
              triggerVoiceMedicalAlert(question, transcript)
            } else {
              recordAnswerAndAdvance(
                question = question,
                score = result.numericValue,
                transcript = transcript,
                feedback = result.briefReply,
                usedCloudAi = result.usedCloudAi
              )
            }
          }
        }
      } catch (e: CancellationException) {
        throw e
      } catch (e: Exception) {
        val fallback = voiceAiService.parseLocally(question, transcript, lang)
        _voiceEvalState.update { it.copy(isOfflineMode = true) }
        if (fallback.medicalRedFlag) {
          triggerVoiceMedicalAlert(question, transcript)
        } else {
          recordAnswerAndAdvance(
            question = question,
            score = fallback.numericValue,
            transcript = transcript,
            feedback = fallback.briefReply,
            usedCloudAi = false
          )
        }
      }
    }
  }

  private fun triggerVoiceMedicalAlert(question: VoiceQuestion, transcript: String) {
    val state = _voiceEvalState.value
    val updatedAnswers = state.answers + (question.id to 9f)
    val updatedTranscripts = state.transcripts + (question.id to transcript)
    repository.applyVoiceEvaluationResults(updatedAnswers, redFlagDetected = true)
    val alertMsg = VoiceProtocolCatalog.medicalAlertVoiceMessage(state.language)

    _voiceEvalState.update {
      it.copy(
        answers = updatedAnswers,
        transcripts = updatedTranscripts,
        lastTranscript = transcript,
        medicalRedFlagTriggered = true,
        phase = VoicePhase.MEDICAL_ALERT,
        aiFeedback = alertMsg
      )
    }
    voiceEngine?.speak(alertMsg)
  }

  private fun recordAnswerAndAdvance(
    question: VoiceQuestion,
    score: Float,
    transcript: String,
    feedback: String,
    usedCloudAi: Boolean
  ) {
    val state = _voiceEvalState.value
    val updatedAnswers = state.answers + (question.id to score)
    val updatedTranscripts = state.transcripts + (question.id to transcript)
    val nextIdx = state.currentIndex + 1

    if (nextIdx >= state.questions.size) {
      completeVoiceEvaluation(updatedAnswers, updatedTranscripts, state.medicalRedFlagTriggered)
    } else {
      val nextQuestion = state.questions[nextIdx]
      _voiceEvalState.update {
        it.copy(
          answers = updatedAnswers,
          transcripts = updatedTranscripts,
          currentIndex = nextIdx,
          aiFeedback = feedback,
          isOfflineMode = !usedCloudAi,
          phase = VoicePhase.SPEAKING
        )
      }
      val engine = voiceEngine
      if (engine != null && state.isTtsReady) {
        engine.speak("$feedback ${nextQuestion.prompt(state.language)}") {
          _voiceEvalState.update { cur ->
            if (cur.phase == VoicePhase.SPEAKING) cur.copy(phase = VoicePhase.IDLE) else cur
          }
        }
      } else {
        _voiceEvalState.update { it.copy(phase = VoicePhase.IDLE) }
      }
    }
  }

  fun repeatCurrentVoiceQuestion() {
    val idx = _voiceEvalState.value.currentIndex
    speakQuestionAt(idx, includeWelcome = false)
  }

  fun correctPreviousVoiceQuestion() {
    val cur = _voiceEvalState.value
    val targetIdx = if (cur.phase == VoicePhase.SUMMARY || cur.currentIndex > 0 && !cur.answers.containsKey(cur.questions[cur.currentIndex].id)) {
      (cur.currentIndex - 1).coerceAtLeast(0)
    } else {
      cur.currentIndex
    }
    speakQuestionAt(targetIdx, includeWelcome = false)
  }

  fun pauseOrResumeVoiceEvaluation() {
    val cur = _voiceEvalState.value
    if (cur.phase == VoicePhase.PAUSED) {
      speakQuestionAt(cur.currentIndex, includeWelcome = false)
    } else {
      voiceEngine?.pauseAll()
      _voiceEvalState.update {
        it.copy(
          phase = VoicePhase.PAUSED,
          aiFeedback = if (cur.language == VoiceLanguage.ES) "Evaluación en pausa." else "Check-in paused."
        )
      }
    }
  }

  fun finishVoiceEvaluationNow() {
    val cur = _voiceEvalState.value
    if (cur.answers.isEmpty()) {
      closeVoiceEvaluation()
    } else {
      completeVoiceEvaluation(cur.answers, cur.transcripts, cur.medicalRedFlagTriggered)
    }
  }

  private fun completeVoiceEvaluation(
    answers: Map<String, Float>,
    transcripts: Map<String, String>,
    redFlag: Boolean
  ) {
    val classification = repository.applyVoiceEvaluationResults(answers, redFlag)
    val recId = classification.recommendedInterventions.firstOrNull() ?: "INT-01"
    val recProg = interventions.value.find { it.id == recId } ?: interventions.value.first()
    val lang = _voiceEvalState.value.language
    val summarySpeech = VoiceProtocolCatalog.summaryVoiceMessage(
      levelLabel = classification.level.label,
      interventionTitle = recProg.title,
      lang = lang
    )

    _voiceEvalState.update {
      it.copy(
        answers = answers,
        transcripts = transcripts,
        phase = if (redFlag) VoicePhase.MEDICAL_ALERT else VoicePhase.SUMMARY,
        recommendedInterventionId = recProg.id,
        aiFeedback = summarySpeech
      )
    }
    voiceEngine?.speak(summarySpeech)
  }

  fun closeVoiceEvaluation() {
    voiceEngine?.pauseAll()
    _voiceEvalState.update { it.copy(isActive = false, phase = VoicePhase.IDLE) }
    _currentScreen.value = AppScreen.DASHBOARD
  }

  // ==================== INTERVENTIONS & VOICE GUIDANCE ====================

  fun startIntervention(program: InterventionProgram, withVoice: Boolean = false) {
    _activeIntervention.value = program
    _exerciseStep.value = 0
    _preExerciseAnxiety.value = currentAnxiety.value
    _postExerciseAnxiety.value = (currentAnxiety.value - 2.5f).coerceAtLeast(1f)
    _timerSeconds.value = program.durationMin * 60
    _isTimerRunning.value = false
    timerJob?.cancel()

    _voiceInterventionState.value = VoiceInterventionUiState(
      isVoiceModeEnabled = withVoice,
      statusText = if (withVoice) "Guía por voz activa" else ""
    )
    if (withVoice) {
      speakCurrentInterventionStep()
    }
  }

  fun toggleInterventionVoiceMode() {
    val cur = _voiceInterventionState.value
    val nextEnabled = !cur.isVoiceModeEnabled
    if (!nextEnabled) {
      voiceEngine?.pauseAll()
      _voiceInterventionState.value = VoiceInterventionUiState(isVoiceModeEnabled = false)
    } else {
      _voiceInterventionState.value = cur.copy(
        isVoiceModeEnabled = true,
        isPaused = false,
        statusText = "Guía por voz activa"
      )
      speakCurrentInterventionStep()
    }
  }

  private fun speakCurrentInterventionStep() {
    val prog = _activeIntervention.value ?: return
    val idx = _exerciseStep.value.coerceIn(0, prog.steps.lastIndex)
    val stepText = prog.steps[idx]
    val lang = _voiceEvalState.value.language
    val spoken = if (lang == VoiceLanguage.ES) {
      "Paso ${idx + 1} de ${prog.steps.size}. $stepText"
    } else {
      "Step ${idx + 1} of ${prog.steps.size}. $stepText"
    }

    _voiceInterventionState.update {
      it.copy(isVoiceModeEnabled = true, isSpeaking = true, isPaused = false, statusText = "Hablando paso ${idx + 1}...")
    }

    val engine = voiceEngine
    if (engine != null && _voiceEvalState.value.isTtsReady) {
      engine.speak(spoken) {
        _voiceInterventionState.update { it.copy(isSpeaking = false, statusText = "Di 'Continuar', 'Repetir', 'Pausar' o 'Finalizar'") }
      }
    } else {
      _voiceInterventionState.update { it.copy(isSpeaking = false, statusText = "Paso ${idx + 1} listo") }
    }
  }

  fun interventionVoiceContinue() {
    val prog = _activeIntervention.value ?: return
    val next = _exerciseStep.value + 1
    if (next < prog.steps.size) {
      _exerciseStep.value = next
      speakCurrentInterventionStep()
    } else {
      _voiceInterventionState.update {
        it.copy(isSpeaking = false, statusText = "Protocolo completado. Registra tu nivel final.")
      }
      voiceEngine?.speak("Protocolo completado. Registra tu nivel final y guarda tu sesión.")
    }
  }

  fun interventionVoiceRepeat() {
    speakCurrentInterventionStep()
  }

  fun interventionVoicePause() {
    voiceEngine?.pauseAll()
    if (_isTimerRunning.value) {
      toggleTimer()
    }
    _voiceInterventionState.update {
      it.copy(isSpeaking = false, isListening = false, isPaused = true, statusText = "Guía en pausa")
    }
  }

  fun interventionVoiceFinish() {
    voiceEngine?.pauseAll()
    _voiceInterventionState.update {
      it.copy(isSpeaking = false, isListening = false, isPaused = false, statusText = "Listo para finalizar")
    }
    completeActiveIntervention()
  }

  fun toggleInterventionVoiceCommandMic() {
    val cur = _voiceInterventionState.value
    if (cur.isListening) {
      voiceEngine?.stopListening()
      _voiceInterventionState.update { it.copy(isListening = false, statusText = "Micrófono detenido") }
      return
    }
    if (!_voiceEvalState.value.hasMicPermission) {
      _voiceInterventionState.update { it.copy(statusText = "Permiso de micrófono requerido") }
      return
    }
    val engine = voiceEngine ?: return
    _voiceInterventionState.update { it.copy(isListening = true, statusText = "Escuchando comando...") }
    engine.startListening(
      language = _voiceEvalState.value.language,
      onPartial = { partial ->
        _voiceInterventionState.update { it.copy(lastHeardCommand = partial) }
      },
      onResult = { transcript ->
        _voiceInterventionState.update { it.copy(isListening = false, lastHeardCommand = transcript) }
        when (voiceAiService.parseInterventionCommand(transcript)) {
          VoiceCommandIntent.CONTINUE -> interventionVoiceContinue()
          VoiceCommandIntent.REPEAT -> interventionVoiceRepeat()
          VoiceCommandIntent.PAUSE -> interventionVoicePause()
          VoiceCommandIntent.FINISH -> interventionVoiceFinish()
          else -> {
            _voiceInterventionState.update {
              it.copy(statusText = "Comando no reconocido. Usa Continuar, Repetir, Pausar o Finalizar.")
            }
          }
        }
      },
      onError = { err ->
        _voiceInterventionState.update { it.copy(isListening = false, statusText = err) }
      }
    )
  }

  fun closeIntervention() {
    voiceEngine?.pauseAll()
    _activeIntervention.value = null
    _isTimerRunning.value = false
    _voiceInterventionState.value = VoiceInterventionUiState()
    timerJob?.cancel()
  }

  fun toggleTimer() {
    if (_isTimerRunning.value) {
      _isTimerRunning.value = false
      timerJob?.cancel()
    } else {
      _isTimerRunning.value = true
      timerJob = viewModelScope.launch {
        while (_timerSeconds.value > 0 && _isTimerRunning.value) {
          delay(1000)
          _timerSeconds.value -= 1
        }
        _isTimerRunning.value = false
      }
    }
  }

  fun setExerciseStep(step: Int) {
    _exerciseStep.value = step
  }

  fun setPreExerciseAnxiety(v: Float) {
    _preExerciseAnxiety.value = v
  }

  fun setPostExerciseAnxiety(v: Float) {
    _postExerciseAnxiety.value = v
  }

  fun completeActiveIntervention() {
    val prog = _activeIntervention.value ?: return
    repository.recordInterventionResult(
      interventionId = prog.id,
      preValue = _preExerciseAnxiety.value,
      postValue = _postExerciseAnxiety.value,
      notes = "Técnica: ${prog.title}"
    )
    closeIntervention()
  }

  fun setShowMedicalProtocol(show: Boolean) {
    _showMedicalProtocol.value = show
  }

  fun setShowPrivacyDialog(show: Boolean) {
    _showPrivacyDialog.value = show
  }

  fun setShowNewThoughtDialog(show: Boolean) {
    _showNewThoughtDialog.value = show
  }

  fun addThoughtRecord(
    situation: String,
    thought: String,
    distortion: String,
    evidenceFor: String,
    evidenceAgainst: String,
    alternative: String,
    pre: Float,
    post: Float
  ) {
    val record = ThoughtRecord(
      id = "TR-${System.currentTimeMillis()}",
      timestamp = "Día ${astronautProfile.value.missionDay} - Reciente",
      situation = situation,
      automaticThought = thought,
      distortion = distortion,
      evidenceFor = evidenceFor,
      evidenceAgainst = evidenceAgainst,
      alternativeThought = alternative,
      preAnxiety = pre,
      postAnxiety = post
    )
    repository.addThoughtRecord(record)
    _showNewThoughtDialog.value = false
  }
}
