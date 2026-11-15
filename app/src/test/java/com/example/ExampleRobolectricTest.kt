package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.SpaceMindRepository
import com.example.model.OperationalLevel
import com.example.voice.VoiceAiNluService
import com.example.voice.VoiceCommandIntent
import com.example.voice.VoiceLanguage
import com.example.voice.VoiceProtocolCatalog
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("SPACE MIND", appName)
  }

  @Test
  fun `verify operational state calculation default is within valid range`() {
    val repo = SpaceMindRepository()
    val state = repo.calculateOperationalState()
    assertNotNull(state.level)
    assertTrue(state.compositeScore in 0..100)
    assertTrue(state.rationale.isNotEmpty())
  }

  @Test
  fun `verify voice evaluation catalog has 11 clinical questions`() {
    val questions = VoiceProtocolCatalog.evaluationQuestions
    assertEquals(11, questions.size)
    assertEquals("Q_EMOTION", questions.first().id)
    assertEquals("Q_CONCENTRATION", questions.last().id)
  }

  @Test
  fun `verify deterministic local NLU extracts anxiety score and medical red flag`() {
    val nlu = VoiceAiNluService()
    val anxietyQ = VoiceProtocolCatalog.evaluationQuestions[1]
    val res = nlu.parseLocally(anxietyQ, "Creo que estoy en un 7 ahora", VoiceLanguage.ES)
    assertEquals(7f, res.numericValue, 0.1f)
    assertFalse(res.medicalRedFlag)

    val redFlagQ = VoiceProtocolCatalog.evaluationQuestions[5]
    val alertRes = nlu.parseLocally(redFlagQ, "Siento dolor en el pecho y mareo", VoiceLanguage.ES)
    assertTrue(alertRes.medicalRedFlag)
  }

  @Test
  fun `verify voice intervention command parsing`() {
    val nlu = VoiceAiNluService()
    assertEquals(VoiceCommandIntent.CONTINUE, nlu.parseInterventionCommand("continuar por favor"))
    assertEquals(VoiceCommandIntent.REPEAT, nlu.parseInterventionCommand("repetir el paso"))
    assertEquals(VoiceCommandIntent.PAUSE, nlu.parseInterventionCommand("pausar"))
    assertEquals(VoiceCommandIntent.FINISH, nlu.parseInterventionCommand("finalizar técnica"))
  }

  @Test
  fun `verify voice evaluation results update repository and red flag escalates level`() {
    val repo = SpaceMindRepository()
    val state = repo.applyVoiceEvaluationResults(
      answers = mapOf(
        "Q_EMOTION" to 4f,
        "Q_ANXIETY" to 8f,
        "Q_SLEEP" to 4f,
        "Q_PALPITATIONS" to 6f
      ),
      redFlagDetected = true
    )
    assertTrue(state.medicalRedFlagCheck)
    assertEquals(OperationalLevel.ACTIVACION_ELEVADA, state.level)
    assertEquals(8f, repo.currentAnxiety.value, 0.01f)
  }
}
