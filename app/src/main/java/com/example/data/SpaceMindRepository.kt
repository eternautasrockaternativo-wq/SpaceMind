package com.example.data

import com.example.model.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class SpaceMindRepository {

  private val _astronautProfile = MutableStateFlow(AstronautProfile())
  val astronautProfile: StateFlow<AstronautProfile> = _astronautProfile.asStateFlow()

  private val _telemetry = MutableStateFlow(PhysiologicalTelemetry())
  val telemetry: StateFlow<PhysiologicalTelemetry> = _telemetry.asStateFlow()

  private val _currentAnxiety = MutableStateFlow(3.8f)
  val currentAnxiety: StateFlow<Float> = _currentAnxiety.asStateFlow()

  private val _currentSleepQuality = MutableStateFlow(7.2f)
  val currentSleepQuality: StateFlow<Float> = _currentSleepQuality.asStateFlow()

  private val _baseline = MutableStateFlow(BaselineAssessment())
  val baseline: StateFlow<BaselineAssessment> = _baseline.asStateFlow()

  private val _somaticSymptoms = MutableStateFlow(createInitialSomaticSymptoms())
  val somaticSymptoms: StateFlow<List<SomaticSymptom>> = _somaticSymptoms.asStateFlow()

  private val _thoughtRecords = MutableStateFlow(createInitialThoughtRecords())
  val thoughtRecords: StateFlow<List<ThoughtRecord>> = _thoughtRecords.asStateFlow()

  private val _sessionLogs = MutableStateFlow(createInitialSessionLogs())
  val sessionLogs: StateFlow<List<SessionLog>> = _sessionLogs.asStateFlow()

  private val _interventions = MutableStateFlow(createAllInterventions())
  val interventions: StateFlow<List<InterventionProgram>> = _interventions.asStateFlow()

  val cognitiveDistortions: List<CognitiveDistortion> = createCognitiveDistortions()

  fun updateCurrentAnxiety(newScore: Float) {
    _currentAnxiety.value = newScore.coerceIn(0f, 10f)
  }

  fun updateCurrentSleepQuality(newScore: Float) {
    _currentSleepQuality.value = newScore.coerceIn(0f, 10f)
  }

  fun updateBaseline(newBaseline: BaselineAssessment) {
    _baseline.value = newBaseline
  }

  fun updateSymptom(symptomId: String, isReported: Boolean, intensity: Float, frequency: String, duration: String, context: String, triggers: String) {
    val updated = _somaticSymptoms.value.map { item ->
      if (item.id == symptomId) {
        item.copy(
          isReported = isReported,
          intensity = intensity,
          frequency = frequency,
          duration = duration,
          context = context,
          possibleTrigger = triggers
        )
      } else item
    }
    _somaticSymptoms.value = updated
  }

  fun addThoughtRecord(record: ThoughtRecord) {
    _thoughtRecords.value = listOf(record) + _thoughtRecords.value
    // If post-anxiety was reduced, update current anxiety state
    if (record.postAnxiety < _currentAnxiety.value) {
      _currentAnxiety.value = record.postAnxiety
    }
    addSessionLog(
      SessionLog(
        id = "LOG-${System.currentTimeMillis()}",
        timestamp = getCurrentTimestamp(),
        moduleName = "NIVEL 3: Reestructuración TCC",
        description = "Registro de pensamiento: \"${record.automaticThought.take(35)}...\"",
        preValue = record.preAnxiety,
        postValue = record.postAnxiety,
        statusLevel = calculateOperationalState().level
      )
    )
  }

  fun recordInterventionResult(interventionId: String, preValue: Float, postValue: Float, notes: String = "") {
    val prog = _interventions.value.find { it.id == interventionId }
    val title = prog?.title ?: "Intervención de Regulación"
    _currentAnxiety.value = postValue.coerceIn(0f, 10f)

    addSessionLog(
      SessionLog(
        id = "LOG-${System.currentTimeMillis()}",
        timestamp = getCurrentTimestamp(),
        moduleName = "NIVEL 5: $title",
        description = "Completado protocolo guiado. Alivio delta: ${(preValue - postValue).toInt()} pts. $notes",
        preValue = preValue,
        postValue = postValue,
        statusLevel = calculateOperationalState().level
      )
    )
  }

  fun addSessionLog(log: SessionLog) {
    _sessionLogs.value = listOf(log) + _sessionLogs.value
  }

  fun applyVoiceEvaluationResults(
    answers: Map<String, Float>,
    redFlagDetected: Boolean
  ): StateClassification {
    val prevAnxiety = _currentAnxiety.value
    val newAnxiety = answers["Q_ANXIETY"] ?: prevAnxiety
    val newSleep = answers["Q_SLEEP"] ?: _currentSleepQuality.value
    val newEmotion = answers["Q_EMOTION"] ?: _baseline.value.emotionalState
    val newConcentration = answers["Q_CONCENTRATION"] ?: _baseline.value.concentrationAbility
    val newIsolation = answers["Q_ISOLATION"] ?: _baseline.value.isolationPerception

    _currentAnxiety.value = newAnxiety.coerceIn(0f, 10f)
    _currentSleepQuality.value = newSleep.coerceIn(0f, 10f)
    _baseline.value = _baseline.value.copy(
      emotionalState = newEmotion.coerceIn(0f, 10f),
      concentrationAbility = newConcentration.coerceIn(0f, 10f),
      isolationPerception = newIsolation.coerceIn(0f, 10f),
      lastUpdated = "Evaluación por voz (Hoy)"
    )

    // Update somatic indicators reported during voice evaluation
    val updatedSymptoms = _somaticSymptoms.value.map { sym ->
      when (sym.id) {
        "SYM-CV-01" -> answers["Q_PALPITATIONS"]?.let { v ->
          sym.copy(isReported = v >= 2f, intensity = v, context = "Evaluación por voz")
        } ?: sym
        "SYM-RS-01" -> answers["Q_DYSPNEA"]?.let { v ->
          sym.copy(isReported = v >= 2f, intensity = v, context = "Evaluación por voz")
        } ?: sym
        "SYM-MN-01" -> answers["Q_TENSION"]?.let { v ->
          sym.copy(isReported = v >= 2f, intensity = v, context = "Evaluación por voz")
        } ?: sym
        "SYM-MN-03" -> answers["Q_DIZZINESS_ACUTE"]?.let { v ->
          sym.copy(isReported = v >= 2f, intensity = v, context = "Evaluación por voz")
        } ?: sym
        "SYM-CV-04" -> if (redFlagDetected) {
          sym.copy(isReported = true, intensity = 8f, context = "Alerta en evaluación por voz")
        } else sym
        else -> sym
      }
    }
    _somaticSymptoms.value = updatedSymptoms

    val state = calculateOperationalState()
    addSessionLog(
      SessionLog(
        id = "VOICE-${System.currentTimeMillis()}",
        timestamp = "Hoy ${getCurrentTimeOnly()}",
        moduleName = "VOICE AI: Chequeo Clínico",
        description = if (redFlagDetected) {
          "Alerta física prioritaria • Protocolo médico indicado"
        } else {
          "Evaluación por voz (${answers.size}/11) • ${state.level.label}"
        },
        preValue = prevAnxiety,
        postValue = _currentAnxiety.value,
        statusLevel = state.level
      )
    )
    return state
  }

  fun calculateOperationalState(): StateClassification {
    val anxiety = _currentAnxiety.value
    val sleep = _currentSleepQuality.value
    val base = _baseline.value
    val reportedSymptoms = _somaticSymptoms.value.filter { it.isReported }
    val severeSomatic = reportedSymptoms.count { it.intensity >= 6f }

    // Check medical red flags: chest oppression or intense shortness of breath
    val hasChestPain = reportedSymptoms.any {
      it.name.contains("opresión", ignoreCase = true) || it.name.contains("torácica", ignoreCase = true) && it.intensity >= 5f
    }
    val hasSevereDyspnea = reportedSymptoms.any {
      it.name.contains("aire", ignoreCase = true) && it.intensity >= 7f
    }
    val medicalRedFlag = hasChestPain || hasSevereDyspnea

    // Composite operational score (0-100)
    val anxietyComponent = (anxiety / 10f) * 40f
    val somaticComponent = (reportedSymptoms.size.coerceAtMost(6) / 6f) * 25f + (severeSomatic * 5f).coerceAtMost(15f)
    val sleepPenalty = ((10f - sleep) / 10f) * 15f
    val baselineDeviation = ((anxiety - base.traitAnxiety).coerceAtLeast(0f) / 10f) * 10f

    val compositeScore = (anxietyComponent + somaticComponent + sleepPenalty + baselineDeviation).toInt().coerceIn(0, 100)

    val rationale = mutableListOf<String>()
    val recommended = mutableListOf<String>()

    val level: OperationalLevel = when {
      medicalRedFlag || compositeScore >= 70 -> {
        rationale.add("Puntaje compuesto elevado ($compositeScore/100). Malestar intenso o síntomas físicos que demandan atención prioritaria.")
        if (medicalRedFlag) {
          rationale.add("ALERTA DE SEGURIDAD CLÍNICA: Síntomas torácicos/respiratorios reportados requieren descarte médico de vuelo antes de considerarlos puramente ansiosos.")
        }
        recommended.add("INT-01") // Respiración lenta
        recommended.add("INT-06") // Regulación Emocional STOP
        recommended.add("INT-10") // Autoinstrucciones
        OperationalLevel.ACTIVACION_ELEVADA
      }
      compositeScore >= 45 -> {
        rationale.add("Activación moderada ($compositeScore/100): Ansiedad subjetiva de $anxiety/10 y ${reportedSymptoms.size} síntomas somáticos registrados.")
        rationale.add("Desviación detectable sobre la línea de base individual (${base.traitAnxiety}/10 habitual).")
        recommended.add("INT-03") // Grounding
        recommended.add("INT-04") // TCC Reestructuración
        recommended.add("INT-05") // Relajación muscular microgravedad
        recommended.add("INT-07") // Manejo de sueño
        OperationalLevel.ACTIVACION_MODERADA
      }
      compositeScore >= 25 -> {
        rationale.add("Activación leve ($compositeScore/100): Variaciones controlables dentro del rango operativo de la misión espacial.")
        rationale.add("Homeostasis general preservada; se sugiere pausa de descarga sensorial.")
        recommended.add("INT-01") // Respiración
        recommended.add("INT-02") // Mindfulness
        recommended.add("INT-03") // Grounding
        OperationalLevel.ACTIVACION_LEVE
      }
      else -> {
        rationale.add("Estado estable ($compositeScore/100): Indicadores psicológicos y autonómicos en concordancia con el perfil basal.")
        rationale.add("No se observan distorsiones cognitivas activas ni interferencia operativa.")
        recommended.add("INT-02") // Mindfulness mantenimiento
        recommended.add("INT-08") // Aislamiento positivo
        OperationalLevel.ESTABLE
      }
    }

    val summary = when (level) {
      OperationalLevel.ESTABLE -> "Homeostasis psicológica y fisiológica óptima para operaciones orbitales."
      OperationalLevel.ACTIVACION_LEVE -> "Tensión psicofisiológica transitoria; recursos de micro-regulación recomendados."
      OperationalLevel.ACTIVACION_MODERADA -> "Activación sostenida con repercusión en descanso o rumiación cognitiva."
      OperationalLevel.ACTIVACION_ELEVADA -> "Desregulación aguda o presencia de síntomas cardinales de alerta médica de vuelo."
    }

    return StateClassification(
      level = level,
      compositeScore = compositeScore,
      summary = summary,
      rationale = rationale,
      recommendedInterventions = recommended,
      medicalRedFlagCheck = medicalRedFlag
    )
  }

  fun refreshTelemetrySimulation() {
    val current = _telemetry.value
    val deltaHr = (-2..2).random()
    val deltaHrv = (-3..3).random()
    val deltaResp = if (Math.random() > 0.5) (-1..1).random() else 0
    val newHr = (current.heartRate + deltaHr).coerceIn(60, 98)
    val newHrv = (current.hrvRmssd + deltaHrv).coerceIn(40, 85)
    val newResp = (current.respRate + deltaResp).coerceIn(12, 18)

    _telemetry.value = current.copy(
      heartRate = newHr,
      hrvRmssd = newHrv,
      respRate = newResp,
      timestamp = "UTC ${SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())}"
    )
  }

  private fun getCurrentTimestamp(): String {
    return SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date())
  }

  private fun getCurrentTimeOnly(): String {
    return SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
  }

  companion object {
    private fun createInitialSomaticSymptoms(): List<SomaticSymptom> {
      return listOf(
        // Cardiovascular
        SomaticSymptom("SYM-CV-01", "Palpitaciones", SomaticCategory.CARDIOVASCULAR, "Sensación de latidos aislados con mayor fuerza o sensación de salto en el pecho.", intensity = 3f, isReported = true, frequency = "Esporádico", duration = "2-5 min", context = "Post-ejercicio y litera", possibleTrigger = "Ruido del módulo / Cafeína"),
        SomaticSymptom("SYM-CV-02", "Sensación de aceleración cardíaca", SomaticCategory.CARDIOVASCULAR, "Percepción de taquicardia subjetiva en reposo sin causa física evidente.", intensity = 4f, isReported = true, frequency = "Ocasional", duration = "10 min", context = "Antes de conferencia de misión", possibleTrigger = "Expectativa operativa"),
        SomaticSymptom("SYM-CV-03", "Percepción de latidos intensos", SomaticCategory.CARDIOVASCULAR, "Conciencia sonora o pulsátil de los latidos en la cabeza o cuello en ingravidez.", intensity = 2f, isReported = false),
        SomaticSymptom("SYM-CV-04", "Molestias u opresión torácica", SomaticCategory.CARDIOVASCULAR, "Sensación difusa de peso en el centro del pecho. ¡Requiere protocolo médico estricto!", intensity = 0f, isReported = false),

        // Respiratorio
        SomaticSymptom("SYM-RS-01", "Sensación de falta de aire", SomaticCategory.RESPIRATORY, "Sensación de que el aire inspirado es insuficiente en el módulo cerrado.", intensity = 2f, isReported = true, frequency = "Esporádico", duration = "5 min", context = "En esclusa de despresurización", possibleTrigger = "Espacio reducido"),
        SomaticSymptom("SYM-RS-02", "Respiración acelerada (Taquipnea)", SomaticCategory.RESPIRATORY, "Frecuencia respiratoria rápida y superficial involuntaria.", intensity = 0f, isReported = false),
        SomaticSymptom("SYM-RS-03", "Sensación de respiración insuficiente", SomaticCategory.RESPIRATORY, "Necesidad constante de tomar aire profundamente para satisfacer la oxigenación.", intensity = 3f, isReported = true, frequency = "Ocasional", duration = "15 min", context = "Litera acústica", possibleTrigger = "Ventilación deficiente local"),
        SomaticSymptom("SYM-RS-04", "Suspiros frecuentes", SomaticCategory.RESPIRATORY, "Emisión involuntaria repetida de suspiros para aliviar tensión pectoral.", intensity = 4f, isReported = true, frequency = "Varias veces al día", duration = "Continuo", context = "Durante trabajo de laboratorio", possibleTrigger = "Concentración prolongada"),

        // Muscular y Neurológico
        SomaticSymptom("SYM-MN-01", "Tensión muscular cervical y dorsal", SomaticCategory.MUSCULAR_NEUROLOGICAL, "Contractura y rigidez en trapecios y cuello agravada por postura flotante.", intensity = 5f, isReported = true, frequency = "Diario", duration = "Horas", context = "Frente a consolas de monitoreo", possibleTrigger = "Postura de trabajo"),
        SomaticSymptom("SYM-MN-02", "Temblores finos en manos", SomaticCategory.MUSCULAR_NEUROLOGICAL, "Fasciculaciones o temblor distal leve al manipular instrumental delicado.", intensity = 1f, isReported = false),
        SomaticSymptom("SYM-MN-03", "Mareos / Cinetosis espacial residual", SomaticCategory.MUSCULAR_NEUROLOGICAL, "Sensación ilusoria de movimiento cefálico al girar en microgravedad.", intensity = 2f, isReported = false),
        SomaticSymptom("SYM-MN-04", "Cefaleas tensionales", SomaticCategory.MUSCULAR_NEUROLOGICAL, "Dolor opresivo frontal o en banda debido a redistribución de fluidos y tensión.", intensity = 3f, isReported = true, frequency = "Ocasional", duration = "1-2 horas", context = "Fin de jornada", possibleTrigger = "Fatiga visual y fluid shift"),
        SomaticSymptom("SYM-MN-05", "Sensación de inestabilidad espacial", SomaticCategory.MUSCULAR_NEUROLOGICAL, "Desorientación visuo-vestibular momentánea.", intensity = 1f, isReported = false),

        // Gastrointestinal y General
        SomaticSymptom("SYM-GG-01", "Náuseas leves", SomaticCategory.GASTROINTESTINAL_GENERAL, "Malestar gástrico sin emesis asociado a estrés o alimentación rehidratada.", intensity = 1f, isReported = false),
        SomaticSymptom("SYM-GG-02", "Molestias o nudo abdominal", SomaticCategory.GASTROINTESTINAL_GENERAL, "Sensación de contracción o vacío en el epigastrio ante situaciones tensas.", intensity = 3f, isReported = true, frequency = "Esporádico", duration = "20 min", context = "Notificaciones de contingencia", possibleTrigger = "Alerta de telemetría"),
        SomaticSymptom("SYM-GG-03", "Sudoración palmar fría", SomaticCategory.GASTROINTESTINAL_GENERAL, "Hiperhidrosis refleja en manos sin incremento de temperatura ambiental.", intensity = 3f, isReported = true, frequency = "Ocasional", duration = "10 min", context = "Maniobras críticas de navegación", possibleTrigger = "Hiperactivación simpática"),
        SomaticSymptom("SYM-GG-04", "Fatiga cognitiva extrema", SomaticCategory.GASTROINTESTINAL_GENERAL, "Agotamiento mental temprano que dificulta el procesamiento de datos.", intensity = 4f, isReported = true, frequency = "Fin de jornada", duration = "Horas", context = "Días de alta carga experimental", possibleTrigger = "Déficit de sueño acumulado"),
        SomaticSymptom("SYM-GG-05", "Alteraciones del apetito", SomaticCategory.GASTROINTESTINAL_GENERAL, "Pérdida de interés en raciones calóricas o apetito selectivo.", intensity = 2f, isReported = false)
      )
    }

    private fun createCognitiveDistortions(): List<CognitiveDistortion> {
      return listOf(
        CognitiveDistortion(
          id = "DIST-01",
          name = "Pensamiento Catastrófico",
          definition = "Anticipar el peor desenlace imaginable como si fuera una certeza inminente e irreversible.",
          spaceExample = "\"Si la telemetría secundaria de soporte vital parpadea de nuevo, el sistema entero va a colapsar y no sobreviviremos a la órbita.\"",
          clinicalCounterStrategy = "Identificar la probabilidad real objetiva. Recordar redundancias triples del subsistema ECLSS y protocolos de contingencia probados en simuladores."
        ),
        CognitiveDistortion(
          id = "DIST-02",
          name = "Sobregeneralización",
          definition = "Extraer una regla universal y negativa a partir de un único incidente o error aislado.",
          spaceExample = "\"Me equivoqué en la calibración del cromatógrafo; esto demuestra que no estoy a la altura de esta misión de exploración profunda.\"",
          clinicalCounterStrategy = "Aislar el evento como un hecho técnico específico. Analizar el histórico de 140 días de calibraciones exitosas."
        ),
        CognitiveDistortion(
          id = "DIST-03",
          name = "Pensamiento Dicotómico (Todo o Nada)",
          definition = "Ver las situaciones en categorías polarizadas absolutas, sin matices intermedios ni tolerancia a la imperfección.",
          spaceExample = "\"O cumplo con el 100% de los experimentos programados hoy o la misión de la tripulación habrá sido un fracaso.\"",
          clinicalCounterStrategy = "Evaluar el continuo de rendimiento. El 80% completado en condiciones de órbita hostil sigue siendo un éxito científico sobresaliente."
        ),
        CognitiveDistortion(
          id = "DIST-04",
          name = "Lectura de Pensamientos (Proyección)",
          definition = "Asumir que los compañeros de tripulación o el equipo en Tierra juzgan negativamente sin evidencia comunicada.",
          spaceExample = "\"El comandante me miró en silencio durante la maniobra; seguro piensa que soy incompetente y que no manejo la presión.\"",
          clinicalCounterStrategy = "Verificar directamente antes de inferir. En microgravedad la fatiga facial y la concentración se confunden con desaprobación."
        ),
        CognitiveDistortion(
          id = "DIST-05",
          name = "Sobreestimación del Peligro",
          definition = "Magnificar la probabilidad de catástrofe y minimizar los recursos y entrenamiento propios para resolverla.",
          spaceExample = "\"Estamos a 400.000 kilómetros de casa, ante cualquier imprevisto estamos totalmente indefensos y solos.\"",
          clinicalCounterStrategy = "Hacer un inventario de capacidades: 3 años de entrenamiento intensivo, soporte continuo de cientos de especialistas en Houston y protocolos ensayados."
        ),
        CognitiveDistortion(
          id = "DIST-06",
          name = "Intolerancia a la Incertidumbre",
          definition = "Necesidad compulsiva de certeza absoluta en un entorno dinámico, interpretando la falta de control como catástrofe.",
          spaceExample = "\"No puedo tolerar no saber exactamente qué causó la vibración en el panel solar; necesito verificarlo cada 5 minutos.\"",
          clinicalCounterStrategy = "Aceptar que la exploración espacial implica márgenes de incertidumbre gestionables. Confiar en los sensores de telemetría pasiva."
        )
      )
    }

    private fun createInitialThoughtRecords(): List<ThoughtRecord> {
      return listOf(
        ThoughtRecord(
          id = "TR-101",
          timestamp = "Día 141 - 21:15",
          situation = "Retardo de 4 segundos en la comunicación por voz con Houston durante revisión de checklist.",
          automaticThought = "\"Algo crítico está pasando en Tierra o se averió nuestra antena de alta ganancia y quedaremos incomunicados para siempre.\"",
          distortion = "Pensamiento Catastrófico",
          evidenceFor = "La señal de audio tuvo un corte momentáneo y estática inusual.",
          evidenceAgainst = "El retardo por distancia orbital a L2 es físicamente inevitable. Las antenas phased-array redundantes muestran haz verde estable.",
          alternativeThought = "\"El retardo es un fenómeno físico esperado a esta distancia lunar. Houston está procesando el reporte y la telemetría de respaldo está activa.\"",
          preAnxiety = 7.5f,
          postAnxiety = 3.2f
        ),
        ThoughtRecord(
          id = "TR-102",
          timestamp = "Día 138 - 09:40",
          situation = "Fátiga intensa al despertar y sensación de piernas pesadas en la litera.",
          automaticThought = "\"Mi cuerpo se está deteriorando irreparablemente por la ingravidez y no resistiré los meses restantes.\"",
          distortion = "Sobreestimación del Peligro",
          evidenceFor = "Siento pesadez y dormí menos de 6 horas continuas.",
          evidenceAgainst = "Mis pruebas de densidad ósea y fuerza muscular semanal están en el 96% de la línea de base. Solo necesito cumplir el protocolo de ejercicio aeróbico.",
          alternativeThought = "\"La adaptación en vuelo tiene altibajos. Es fatiga acumulada puntual, no fallo sistémico. El descanso de hoy normalizará mis niveles.\"",
          preAnxiety = 6.0f,
          postAnxiety = 2.8f
        )
      )
    }

    private fun createInitialSessionLogs(): List<SessionLog> {
      return listOf(
        SessionLog("LOG-01", "Hoy 14:10", "NIVEL 5: Regulación Respiratoria", "Respiración 4-7-8 con ciclo de descompresión simpática (5 minutos).", 6.8f, 3.4f, OperationalLevel.ACTIVACION_LEVE),
        SessionLog("LOG-02", "Ayer 22:30", "NIVEL 5: Higiene del Sueño Orbital", "Desaceleración cognitiva previa a litera acústica y reducción de luz azul.", 5.5f, 2.8f, OperationalLevel.ESTABLE),
        SessionLog("LOG-03", "Día 140 18:00", "NIVEL 3: Reestructuración TCC", "Registro y refutación de pensamiento automático sobre retardo de comunicaciones.", 7.5f, 3.2f, OperationalLevel.ACTIVACION_MODERADA),
        SessionLog("LOG-04", "Día 139 11:20", "NIVEL 2: Detección Somática", "Evaluación de síntomas cardiovasculares y tensión cervical post-EVA simulada.", 4.2f, 4.2f, OperationalLevel.ACTIVACION_LEVE)
      )
    }

    private fun createAllInterventions(): List<InterventionProgram> {
      return listOf(
        InterventionProgram(
          id = "INT-01",
          orderIndex = 1,
          title = "Regulación Respiratoria",
          subtitle = "Respiración Diafragmática & 4-7-8 en Microgravedad",
          category = "Fisiológica",
          durationMin = 5,
          scientificBasis = "La prolongación voluntaria de la espiración estimula el nervio vago y la respuesta acetilcolinérgica, disminuyendo la descarga simpática, normalizando el gasto cardíaco y elevando el tono vagal medido por HRV (RMSSD). En microgravedad evita la alcalosis respiratoria al prevenir la hiperventilación.",
          indications = "Palpitaciones funcionales, sensación de falta de aire por hipervigilancia, previo a maniobras operativas críticas o tras recibir alertas.",
          contraindications = "No forzar apnea en caso de afección pulmonar activa o cuando la concentración ambiental de CO2 de cabina supere los 0.8 mmHg.",
          steps = listOf(
            "Anclarse suavemente con la sujeción de pies para no flotar involuntariamente.",
            "Colocar una mano sobre el diafragma y otra en el tórax.",
            "Inhalar suavemente por la nariz durante 4 segundos expandiendo el abdomen sin elevar los hombros.",
            "Mantener el aire durante 4 a 7 segundos de forma confortable y sin tensión glótica.",
            "Exhalar por la boca de manera relajada y sostenida durante 8 segundos, como soplando una vela suavemente.",
            "Completar 4 a 6 ciclos continuos sintiendo cómo el ritmo cardíaco desacelera."
          ),
          exerciseType = InteractiveExerciseType.BREATHING_TIMER
        ),
        InterventionProgram(
          id = "INT-02",
          orderIndex = 2,
          title = "Mindfulness Espacial",
          subtitle = "Atención Plena y Desidentificación de Pensamientos",
          category = "Atencional",
          durationMin = 8,
          scientificBasis = "Basado en la Red Neuronal por Defecto (DMN). El entrenamiento en observación no enjuiciadora reduce la rumiación autorreferencial y desactiva bucles de ansiedad anticipatoria comunes en confinamiento prolongado.",
          indications = "Rumiación mental repetitiva, hipervigilancia de sensores, sensación de sobrecarga cognitiva.",
          contraindications = "No aplicar durante maniobras técnicas que demanden atención ejecutiva activa al 100%.",
          steps = listOf(
            "Cierra suavemente los ojos o fija la vista en un punto neutro de la consola.",
            "Toma conciencia del aire recirculado que acaricia tu rostro y de la sensación de ingravidez en tu cuerpo.",
            "Observa los pensamientos que surgen como si fueran fragmentos orbitales que cruzan tu campo de visión: nótalos, nómbralos ('pensamiento de preocupación', 'recuerdo') y déjalos continuar su órbita.",
            "Si tu mente se engancha con una alarma imaginaria, regresa amablemente tu ancla a la sensación del flujo de aire en la punta de tu nariz."
          ),
          exerciseType = InteractiveExerciseType.MINDFUL_OBSERVATION
        ),
        InterventionProgram(
          id = "INT-03",
          orderIndex = 3,
          title = "Grounding Sensorial 5-4-3-2-1",
          subtitle = "Anclaje en el Entorno Físico de la Estación",
          category = "Somato-sensorial",
          durationMin = 4,
          scientificBasis = "Activa la corteza prefrontal y las áreas sensoriales primarias a través de la aferencia exteroceptiva, interrumpiendo abruptamente el secuestro amigdalino y la despersonalización/desrealización por aislamiento.",
          indications = "Crisis de angustia aguda, sensación de desrealización o extrañeza del entorno espacial, pánico incipiente.",
          contraindications = "Ninguna; completamente seguro en cualquier fase operativa.",
          steps = listOf(
            "5 COSAS QUE VES: Nombra 5 objetos específicos en el módulo (la pantalla de telemetría, el clip de velcro, el conector azul, el indicador LED, la textura de la escotilla).",
            "4 COSAS QUE SIENTES FÍSICAMENTE: Nota 4 sensaciones táctiles (la presión del arnés en tus muslos, la temperatura del pasamanos metálico, el roce de tu camiseta, la vibración del suelo).",
            "3 SONIDOS QUE ESCUCHAS: Identifica 3 sonidos presentes (el zumbido constante de los ventiladores ECLSS, la bomba de refrigerante, tu propia respiración tranquila).",
            "2 OLORES QUE DETECTAS: Percibe 2 matices del aire de cabina (el aroma a filtro HEPA limpio, el tejido de polímero).",
            "1 SENSACIÓN AGRADABLE O RECURSO POSITIVO: Recuerda el sabor de tu bebida matutina o la certeza de estar cumpliendo una misión histórica."
          ),
          exerciseType = InteractiveExerciseType.GROUNDING_54321
        ),
        InterventionProgram(
          id = "INT-04",
          orderIndex = 4,
          title = "Terapia Cognitivo-Conductual (TCC)",
          subtitle = "Reestructuración Cognitiva & Prueba de Realidad",
          category = "Cognitiva",
          durationMin = 10,
          scientificBasis = "Modelo cognitivo de Beck y Ellis adaptado a factores humanos aeroespaciales. La ansiedad surge de la interpretación catastrofizante del estímulo, no del evento en sí. Modificar las cogniciones desadaptativas normaliza el afecto.",
          indications = "Pensamientos anticipatorios de fallo, sensación de incompetencia o pérdida de control.",
          contraindications = "Situaciones de emergencia técnica real activa donde se requiere ejecución inmediata de checklist de emergencia.",
          steps = listOf(
            "Identificar la situación detonante precisa.",
            "Capturar el pensamiento automático exacto que cruzó la mente ('vamos a quedarnos sin energía').",
            "Etiquetar la distorsión cognitiva (catastrofismo, blanco y negro, sobreestimación de amenaza).",
            "Someter el pensamiento a tribunal científico: ¿Qué hechos verificables lo sostienen? ¿Qué datos de redundancia lo contradicen?",
            "Formular una conclusión racional y adaptativa fundamentada en datos.",
            "Re-evaluar el malestar de 0 a 10."
          ),
          exerciseType = InteractiveExerciseType.COGNITIVE_RESTRUCTURING
        ),
        InterventionProgram(
          id = "INT-05",
          orderIndex = 5,
          title = "Relajación Muscular Progresiva en Microgravedad",
          subtitle = "Descompresión Somática sin Sobrecarga Articular",
          category = "Fisiológica",
          durationMin = 7,
          scientificBasis = "Basado en la técnica de Jacobson adaptada a microgravedad por medicina aeroespacial. La tensión muscular isométrica deliberada de 5 segundos seguida de relajación refleja reduce la propiocepción de amenaza y el tono simpático.",
          indications = "Tensión cervical, dolor de espalda por estiramiento de columna en ingravidez, rigidez postural.",
          contraindications = "Evitar tensionar grupos musculares con lesiones agudas o contracturas espasmódicas severas.",
          steps = listOf(
            "Sujetarse holgadamente de los estribos para mantener una flotabilidad neutra sin esfuerzo.",
            "Manos y antebrazos: apretar los puños con un 60% de fuerza durante 5 segundos... y soltar completamente sintiendo el calor del flujo sanguíneo.",
            "Brazos y hombros: encoger los hombros hacia arriba hacia las orejas durante 5 segundos... y dejar caer suavemente.",
            "Rostro: fruncir suavemente la frente y apretar los párpados 4 segundos... relajar los músculos mandibulares separando los dientes.",
            "Abdomen y espalda baja: contraer suavemente el corsé lumbar 5 segundos... y soltar en ingravidez.",
            "Respirar con profundidad dos veces integrando la relajación de todo el cuerpo."
          ),
          exerciseType = InteractiveExerciseType.MUSCLE_RELAXATION
        ),
        InterventionProgram(
          id = "INT-06",
          orderIndex = 6,
          title = "Regulación Emocional: Técnica S.T.O.P.",
          subtitle = "Gestión de Malestar Agudo e Irritabilidad",
          category = "Emocional",
          durationMin = 3,
          scientificBasis = "Estrategia de la Terapia Dialéctica Conductual (DBT) para la tolerancia al malestar. Previene conductas impulsivas o fricciones interpersonales con compañeros de tripulación bajo estrés extremo.",
          indications = "Irritabilidad con un compañero de cabina, frustración ante fallos de hardware, enojo.",
          contraindications = "Ninguna.",
          steps = listOf(
            "S - STOP (Deténte): Congela tus palabras y tus manos inmediatamente. No reacciones desde la frustración.",
            "T - TAKE A BREATH (Respira): Haz una inhalación profunda y una espiración larga sintiendo tus pies anclados.",
            "O - OBSERVE (Observa): Pregúntate: ¿Qué está ocurriendo dentro de mí? (Siento enojo, mi mandíbula está tensa). ¿Qué está pasando fuera objetivamente?",
            "P - PROCEED (Procede con efectividad): Elige la respuesta que protege la misión y la cohesión de la tripulación."
          ),
          exerciseType = InteractiveExerciseType.EMOTIONAL_STOP
        ),
        InterventionProgram(
          id = "INT-07",
          orderIndex = 7,
          title = "Manejo del Sueño Orbital",
          subtitle = "Protocolo de Desaceleración Cognitiva Pre-Litera",
          category = "Cronobiológica",
          durationMin = 10,
          scientificBasis = "El sueño en microgravedad sufre por la ausencia de ciclo solar natural de 24h, ruido acústico de bombas y pérdida de peso de la cabeza. Este protocolo sincroniza la melatonina y reduce la latencia de sueño.",
          indications = "Insomnio de conciliación, despertares frecuentes, cansancio acumulado.",
          contraindications = "No realizar actividad física vigorosa en los 90 minutos previos a este protocolo.",
          steps = listOf(
            "Atenuar iluminación del módulo a modo nocturno (luz roja/ámbar cálida sin componente azul).",
            "Asegurar el saco de dormir y la banda de sujeción cefálica para evitar balanceos involuntarios en microgravedad.",
            "Colocar protectores auditivos de reducción activa de ruido contra los ventiladores.",
            "Vaciado mental: registrar cualquier tarea pendiente en la bitácora para 'guardarla fuera de la mente'.",
            "Realizar 10 respiraciones con cuenta regresiva del 10 al 1, soltando el peso corporal en el saco."
          ),
          exerciseType = InteractiveExerciseType.SLEEP_HYGIENE
        ),
        InterventionProgram(
          id = "INT-08",
          orderIndex = 8,
          title = "Afrontamiento del Aislamiento",
          subtitle = "Resignificación del Confinamiento y Conexión Asíncrona",
          category = "Psicosocial",
          durationMin = 8,
          scientificBasis = "Psicología positiva y existencial (Viktor Frankl) aplicada a astronautas en misiones de larga duración. La atribución de sentido profundo al aislamiento neutraliza la sintomatología depresiva y la astenia espacial.",
          indications = "Sensación de soledad profunda, añoranza de la Tierra, distancia familiar insuperable.",
          contraindications = "Ninguna.",
          steps = listOf(
            "Reconocer y validar la emoción: sentir nostalgia no disminuye tu profesionalismo como astronauta.",
            "Resignificación del espacio: este módulo no es una celda; es una cápsula de vanguardia científica donde representas a toda la humanidad.",
            "Conexión asíncrona afectiva: redactar un mensaje o audio para enviar a tus seres queridos en la siguiente ventana de enlace.",
            "Apreciación del momento: contemplar mentalmente la inmensidad del cosmos y tu contribución histórica."
          ),
          exerciseType = InteractiveExerciseType.ISOLATION_REFRAME
        ),
        InterventionProgram(
          id = "INT-09",
          orderIndex = 9,
          title = "Resolución de Problemas Espaciales",
          subtitle = "Desglose Algorítmico y Recuperación de Control",
          category = "Ejecutiva",
          durationMin = 12,
          scientificBasis = "Terapia de Resolución de Problemas (Nezu). La percepción de desbordamiento se neutraliza al segmentar contingencias masivas en subtareas atómicas secuenciales con criterio SMART.",
          indications = "Sobrecarga de trabajo de mantenimiento, procedimientos concurrentes con plazos ajustados.",
          contraindications = "Emergencias que ya cuentan con procedimiento de ejecución rápida mandatorio (Flight Rules).",
          steps = listOf(
            "Definir el problema de forma ultra-específica en una sola frase.",
            "Listar todas las soluciones posibles sin juzgarlas al inicio (lluvia de ideas orbital).",
            "Evaluar viabilidad técnica: consumo de tiempo, recursos de soporte vital y riesgo operacional.",
            "Seleccionar la mejor alternativa viable y desglosarla en tres micro-pasos inmediatos.",
            "Ejecutar el primer micro-paso ahora mismo y evaluar el resultado."
          ),
          exerciseType = InteractiveExerciseType.PROBLEM_SOLVING
        ),
        InterventionProgram(
          id = "INT-10",
          orderIndex = 10,
          title = "Autoinstrucciones Adaptativas",
          subtitle = "Entrenamiento Cognitivo de Frases de Poder Bajo Presión",
          category = "Cognitiva",
          durationMin = 4,
          scientificBasis = "Inoculación de Estrés de Meichenbaum. El diálogo interno auto-dirigido programa respuestas cognitivas de contención amigdalina durante eventos de alta exigencia técnica.",
          indications = "Antes de maniobras de acoplamiento, EVA (caminatas espaciales), o tras una alarma de sistema.",
          contraindications = "Ninguna.",
          steps = listOf(
            "Identificar el diálogo interno negativo habitual ('no voy a poder', 'es demasiado peligroso').",
            "Sustituirlo por la tríada de autoinstrucción de astronauta:",
            "1. 'Un paso a la vez. Sé exactamente lo que dice el procedimiento.'",
            "2. 'He entrenado cientos de horas para esta contingencia; mi equipo y yo estamos preparados.'",
            "3. 'El estrés es solo adrenalina ayudándome a enfocar la atención. Estoy en calma y en control.'"
          ),
          exerciseType = InteractiveExerciseType.AUTO_INSTRUCTIONS
        )
      )
    }
  }
}
