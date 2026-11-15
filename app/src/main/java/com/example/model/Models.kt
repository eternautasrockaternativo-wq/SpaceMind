package com.example.model

enum class OperationalLevel(val label: String, val code: String) {
  ESTABLE("Estado Estable", "N1-EST"),
  ACTIVACION_LEVE("Activación Leve", "N2-LEV"),
  ACTIVACION_MODERADA("Activación Moderada", "N3-MOD"),
  ACTIVACION_ELEVADA("Activación Elevada", "N4-ELE")
}

data class AstronautProfile(
  val id: String = "ASTRO-704",
  val name: String = "Dra. Elena Rostova",
  val callsign: String = "Valkyrie",
  val role: String = "Especialista de Misión & Astrobióloga",
  val missionName: String = "Misión Orbital Deep Space",
  val missionDay: Int = 142,
  val totalDays: Int = 180,
  val flightPhysician: String = "Dra. S. Collins (Control Terrestre Houston)",
  val clearanceLevel: String = "Nivel 4 Confidencial - Telemetría Médica"
)

data class PhysiologicalTelemetry(
  val heartRate: Int = 74,
  val hrvRmssd: Int = 56,
  val respRate: Int = 14,
  val skinTemp: Float = 34.4f,
  val spO2: Int = 98,
  val cabinCo2: Float = 0.32f,
  val timestamp: String = "UTC 14:32:05"
)

data class BaselineAssessment(
  val traitAnxiety: Float = 2.0f,
  val baselineSleepQuality: Float = 8.5f,
  val perceivedStress: Float = 3.0f,
  val emotionalState: Float = 8.0f,
  val concentrationAbility: Float = 8.5f,
  val isolationPerception: Float = 3.5f,
  val pastStressorsNotes: String = "Adaptación inicial a microgravedad superada; tensión transitoria durante maniobra de acoplamiento no tripulada en día 48.",
  val typicalSomaticHabits: String = "Leve tensión en hombros post-sesión de ejercicio en trotadora T2 con arnés.",
  val isCompleted: Boolean = true,
  val lastUpdated: String = "Día 135 de Misión"
)

enum class SomaticCategory(val displayName: String, val iconName: String) {
  CARDIOVASCULAR("Cardiovascular", "favorite"),
  RESPIRATORY("Respiratorio", "air"),
  MUSCULAR_NEUROLOGICAL("Muscular y Neurológico", "fitness_center"),
  GASTROINTESTINAL_GENERAL("Gastrointestinal y General", "spa")
}

data class SomaticSymptom(
  val id: String,
  val name: String,
  val category: SomaticCategory,
  val description: String,
  var intensity: Float = 0f,
  var isReported: Boolean = false,
  var frequency: String = "Esporádico",
  var duration: String = "5-15 min",
  var context: String = "Durante período de descanso",
  var possibleTrigger: String = "Ruido de ventiladores / Carga operativa"
)

data class CognitiveDistortion(
  val id: String,
  val name: String,
  val definition: String,
  val spaceExample: String,
  val clinicalCounterStrategy: String
)

data class ThoughtRecord(
  val id: String,
  val timestamp: String,
  val situation: String,
  val automaticThought: String,
  val distortion: String,
  val evidenceFor: String,
  val evidenceAgainst: String,
  val alternativeThought: String,
  val preAnxiety: Float,
  val postAnxiety: Float
)

data class StateClassification(
  val level: OperationalLevel,
  val compositeScore: Int,
  val summary: String,
  val rationale: List<String>,
  val recommendedInterventions: List<String>,
  val medicalRedFlagCheck: Boolean = false
)

enum class InteractiveExerciseType {
  BREATHING_TIMER,
  GROUNDING_54321,
  MUSCLE_RELAXATION,
  COGNITIVE_RESTRUCTURING,
  EMOTIONAL_STOP,
  SLEEP_HYGIENE,
  ISOLATION_REFRAME,
  PROBLEM_SOLVING,
  AUTO_INSTRUCTIONS,
  MINDFUL_OBSERVATION
}

data class InterventionProgram(
  val id: String,
  val orderIndex: Int,
  val title: String,
  val subtitle: String,
  val category: String,
  val durationMin: Int,
  val scientificBasis: String,
  val indications: String,
  val contraindications: String,
  val steps: List<String>,
  val exerciseType: InteractiveExerciseType
)

data class SessionLog(
  val id: String,
  val timestamp: String,
  val moduleName: String,
  val description: String,
  val preValue: Float,
  val postValue: Float,
  val statusLevel: OperationalLevel
)
