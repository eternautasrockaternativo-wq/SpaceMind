package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.model.OperationalLevel
import com.example.ui.AppScreen
import com.example.ui.SpaceMindViewModel
import com.example.ui.components.OperationalLevelBadge
import com.example.ui.components.SemicircularActivationGauge
import com.example.ui.components.SpaceCard
import com.example.ui.components.VoiceWaveformVisualizer
import com.example.ui.theme.*
import com.example.voice.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VoiceEvaluationScreen(
  viewModel: SpaceMindViewModel,
  modifier: Modifier = Modifier
) {
  val context = LocalContext.current
  val voiceState by viewModel.voiceEvalState.collectAsState()
  val interventions by viewModel.interventions.collectAsState()
  val anxiety by viewModel.currentAnxiety.collectAsState()
  val sleepQuality by viewModel.currentSleepQuality.collectAsState()
  val operationalState = remember(anxiety, sleepQuality, voiceState.phase) {
    viewModel.getOperationalClassification()
  }

  val permissionLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.RequestPermission()
  ) { granted ->
    viewModel.updateMicPermission(granted)
    if (granted) {
      viewModel.toggleVoiceEvaluationMic()
    }
  }

  LaunchedEffect(Unit) {
    val granted = ContextCompat.checkSelfPermission(
      context,
      Manifest.permission.RECORD_AUDIO
    ) == PackageManager.PERMISSION_GRANTED
    viewModel.updateMicPermission(granted)
  }

  BackHandler {
    viewModel.closeVoiceEvaluation()
  }

  val isEs = voiceState.language == VoiceLanguage.ES
  val currentQ = voiceState.questions.getOrNull(voiceState.currentIndex)

  Scaffold(
    topBar = {
      TopAppBar(
        title = {
          Column {
            Text(
              text = "SPACE MIND VOICE AI",
              style = MaterialTheme.typography.titleMedium,
              color = CyanNeon,
              fontWeight = FontWeight.ExtraBold
            )
            Text(
              text = if (voiceState.phase == VoicePhase.SUMMARY) {
                if (isEs) "Resumen de Evaluación" else "Assessment Summary"
              } else {
                val idx = (voiceState.currentIndex + 1).coerceAtMost(voiceState.questions.size)
                if (isEs) "Pregunta $idx de ${voiceState.questions.size}" else "Question $idx of ${voiceState.questions.size}"
              },
              style = MaterialTheme.typography.labelSmall,
              color = TextSecondary
            )
          }
        },
        navigationIcon = {
          IconButton(onClick = { viewModel.closeVoiceEvaluation() }) {
            Icon(
              imageVector = Icons.AutoMirrored.Filled.ArrowBack,
              contentDescription = "Volver",
              tint = CyanNeon
            )
          }
        },
        actions = {
          // ES / EN quick language switcher
          Surface(
            shape = RoundedCornerShape(10.dp),
            color = SpaceSurface,
            border = BorderStroke(1.dp, SpaceBorder),
            modifier = Modifier.padding(end = 8.dp)
          ) {
            Row(modifier = Modifier.padding(2.dp)) {
              VoiceLanguage.entries.forEach { lang ->
                val sel = voiceState.language == lang
                Box(
                  modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (sel) CyanNeon else Color.Transparent)
                    .clickable { viewModel.setVoiceLanguage(lang) }
                    .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                  Text(
                    text = lang.label,
                    style = MaterialTheme.typography.labelSmall,
                    color = if (sel) SpaceDeepNavy else TextSecondary,
                    fontWeight = FontWeight.Bold
                  )
                }
              }
            }
          }
        },
        colors = TopAppBarDefaults.topAppBarColors(containerColor = SpaceDeepNavy)
      )
    },
    containerColor = SpaceDeepNavy
  ) { innerPadding ->
    Column(
      modifier = modifier
        .fillMaxSize()
        .padding(innerPadding)
        .padding(horizontal = 16.dp)
        .verticalScroll(rememberScrollState()),
      verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
      // Progress bar
      LinearProgressIndicator(
        progress = {
          if (voiceState.phase == VoicePhase.SUMMARY) 1f
          else ((voiceState.currentIndex + 1).toFloat() / voiceState.questions.size.toFloat()).coerceIn(0f, 1f)
        },
        modifier = Modifier
          .fillMaxWidth()
          .height(6.dp)
          .clip(RoundedCornerShape(3.dp)),
        color = CyanNeon,
        trackColor = SpaceSurfaceCard
      )

      when (voiceState.phase) {
        VoicePhase.MEDICAL_ALERT -> {
          MedicalRedFlagSummaryCard(
            isEs = isEs,
            onOpenProtocol = { viewModel.setShowMedicalProtocol(true) },
            onClose = { viewModel.closeVoiceEvaluation() }
          )
        }

        VoicePhase.SUMMARY -> {
          val recProg = interventions.find { it.id == voiceState.recommendedInterventionId } ?: interventions.first()
          VoiceSummaryView(
            isEs = isEs,
            score = operationalState.compositeScore,
            level = operationalState.level,
            summary = operationalState.summary,
            recommendedTitle = recProg.title,
            recommendedDurationMin = recProg.durationMin,
            onStartVoiceIntervention = {
              viewModel.startIntervention(recProg, withVoice = true)
              viewModel.navigateTo(AppScreen.LEVEL_5_INTERVENTIONS)
            },
            onReturnDashboard = { viewModel.closeVoiceEvaluation() }
          )
        }

        else -> {
          if (currentQ != null) {
            // 1. Voice Status + Waveform Card
            SpaceCard(
              borderColor = when (voiceState.phase) {
                VoicePhase.LISTENING -> TealLight
                VoicePhase.PROCESSING -> VioletNeon
                VoicePhase.SPEAKING -> CyanNeon
                else -> SpaceBorder
              },
              backgroundColor = SpaceSurfaceCard
            ) {
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
              ) {
                VoiceStatusPill(phase = voiceState.phase, isEs = isEs)
                if (voiceState.isOfflineMode) {
                  Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = StatusMildYellow.copy(alpha = 0.15f),
                    border = BorderStroke(1.dp, StatusMildYellow.copy(alpha = 0.5f))
                  ) {
                    Text(
                      text = if (isEs) "Modo Local" else "Local Mode",
                      style = MaterialTheme.typography.labelSmall,
                      color = StatusMildYellow,
                      fontSize = 10.sp,
                      modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                  }
                }
              }

              Spacer(modifier = Modifier.height(10.dp))

              VoiceWaveformVisualizer(
                isSpeaking = voiceState.phase == VoicePhase.SPEAKING,
                isListening = voiceState.phase == VoicePhase.LISTENING,
                isProcessing = voiceState.phase == VoicePhase.PROCESSING,
                rmsLevel = voiceState.rmsLevel
              )

              Spacer(modifier = Modifier.height(12.dp))

              // Single focused question prompt
              Text(
                text = currentQ.title(voiceState.language).uppercase(),
                style = MaterialTheme.typography.labelSmall,
                color = CyanGlow,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.8.sp
              )
              Spacer(modifier = Modifier.height(4.dp))
              Text(
                text = currentQ.prompt(voiceState.language),
                style = MaterialTheme.typography.titleMedium,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                lineHeight = 24.sp
              )

              if (voiceState.lastTranscript.isNotBlank()) {
                Spacer(modifier = Modifier.height(10.dp))
                Box(
                  modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(SpaceSurface)
                    .padding(10.dp)
                ) {
                  Text(
                    text = "“${voiceState.lastTranscript}”",
                    style = MaterialTheme.typography.bodySmall,
                    color = TealLight
                  )
                }
              }

              voiceState.errorNotice?.let { notice ->
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                  text = notice,
                  style = MaterialTheme.typography.labelSmall,
                  color = StatusMildYellow
                )
              }
            }

            // 2. Central Push-to-Speak Microphone Button (never open permanently)
            Column(
              modifier = Modifier.fillMaxWidth(),
              horizontalAlignment = Alignment.CenterHorizontally
            ) {
              val isListening = voiceState.phase == VoicePhase.LISTENING
              Box(
                modifier = Modifier
                  .size(80.dp)
                  .clip(CircleShape)
                  .background(
                    Brush.linearGradient(
                      if (isListening) listOf(TealLight, CyanNeon)
                      else listOf(CyanNeon, VioletNeon)
                    )
                  )
                  .border(3.dp, Color.White.copy(alpha = 0.35f), CircleShape)
                  .clickable {
                    if (voiceState.hasMicPermission) {
                      viewModel.toggleVoiceEvaluationMic()
                    } else {
                      permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                    }
                  }
                  .testTag("voice_mic_button"),
                contentAlignment = Alignment.Center
              ) {
                Icon(
                  imageVector = if (isListening) Icons.Default.Stop else Icons.Default.Mic,
                  contentDescription = if (isListening) "Detener micrófono" else "Hablar",
                  tint = SpaceDeepNavy,
                  modifier = Modifier.size(34.dp)
                )
              }

              Spacer(modifier = Modifier.height(6.dp))
              Text(
                text = when {
                  isListening -> if (isEs) "Escuchando... Toca para detener" else "Listening... Tap to stop"
                  !voiceState.hasMicPermission -> if (isEs) "Toca para activar micrófono (bajo demanda)" else "Tap to enable mic (on demand)"
                  else -> if (isEs) "Toca para responder por voz" else "Tap to answer by voice"
                },
                style = MaterialTheme.typography.labelSmall,
                color = if (isListening) TealLight else TextSecondary,
                textAlign = TextAlign.Center
              )
            }

            // 3. Fast Visual Tap Alternative (works offline / without STT)
            SpaceCard(
              borderColor = SpaceBorder,
              backgroundColor = SpaceSurface
            ) {
              Text(
                text = if (isEs) "RESPUESTA RÁPIDA EN PANTALLA" else "QUICK SCREEN RESPONSE",
                style = MaterialTheme.typography.labelSmall,
                color = TextMuted,
                fontWeight = FontWeight.Bold
              )
              Spacer(modifier = Modifier.height(8.dp))

              QuickResponseControls(
                question = currentQ,
                isEs = isEs,
                onSelect = { label, score, isRedFlag ->
                  viewModel.submitManualOrTextResponse(label, score, isRedFlag)
                }
              )
            }

            // 4. Voice Protocol Control Bar: Repetir, Corregir, Pausar, Finalizar
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
              VoiceControlButton(
                icon = Icons.Default.Replay,
                label = if (isEs) "Repetir" else "Repeat",
                modifier = Modifier.weight(1f),
                onClick = { viewModel.repeatCurrentVoiceQuestion() }
              )
              VoiceControlButton(
                icon = Icons.Default.Undo,
                label = if (isEs) "Corregir" else "Correct",
                modifier = Modifier.weight(1f),
                onClick = { viewModel.correctPreviousVoiceQuestion() }
              )
              VoiceControlButton(
                icon = if (voiceState.phase == VoicePhase.PAUSED) Icons.Default.PlayArrow else Icons.Default.Pause,
                label = if (voiceState.phase == VoicePhase.PAUSED) {
                  if (isEs) "Seguir" else "Resume"
                } else {
                  if (isEs) "Pausar" else "Pause"
                },
                modifier = Modifier.weight(1f),
                onClick = { viewModel.pauseOrResumeVoiceEvaluation() }
              )
              VoiceControlButton(
                icon = Icons.Default.Check,
                label = if (isEs) "Finalizar" else "Finish",
                accent = TealLight,
                modifier = Modifier.weight(1f),
                onClick = { viewModel.finishVoiceEvaluationNow() }
              )
            }

            Spacer(modifier = Modifier.height(16.dp))
          }
        }
      }
    }
  }
}

@Composable
private fun VoiceStatusPill(phase: VoicePhase, isEs: Boolean) {
  val (label, color) = when (phase) {
    VoicePhase.SPEAKING -> (if (isEs) "Hablando..." else "Speaking...") to CyanNeon
    VoicePhase.LISTENING -> (if (isEs) "Escuchando..." else "Listening...") to TealLight
    VoicePhase.PROCESSING -> (if (isEs) "Procesando IA..." else "Processing AI...") to VioletSoft
    VoicePhase.PAUSED -> (if (isEs) "En pausa" else "Paused") to StatusMildYellow
    else -> (if (isEs) "Listo" else "Ready") to TextSecondary
  }

  Surface(
    shape = RoundedCornerShape(12.dp),
    color = color.copy(alpha = 0.16f),
    border = BorderStroke(1.dp, color.copy(alpha = 0.6f))
  ) {
    Text(
      text = label.uppercase(),
      style = MaterialTheme.typography.labelSmall,
      color = color,
      fontWeight = FontWeight.Bold,
      fontSize = 10.sp,
      modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
    )
  }
}

@Composable
private fun QuickResponseControls(
  question: VoiceQuestion,
  isEs: Boolean,
  onSelect: (label: String, score: Float, isRedFlag: Boolean) -> Unit
) {
  when (question.inputKind) {
    VoiceInputKind.SCALE_0_10 -> {
      Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
          (0..5).forEach { n ->
            QuickScoreChip(
              text = "$n",
              modifier = Modifier.weight(1f),
              onClick = { onSelect("$n/10", n.toFloat(), false) }
            )
          }
        }
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
          (6..10).forEach { n ->
            QuickScoreChip(
              text = "$n",
              modifier = Modifier.weight(1f),
              onClick = { onSelect("$n/10", n.toFloat(), false) }
            )
          }
        }
      }
    }

    VoiceInputKind.EMOTIONAL_STATE -> {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        val opts = if (isEs) {
          listOf(Triple("Calma", 8.5f, StatusStableGreen), Triple("Normal", 7f, CyanNeon), Triple("Tenso", 4.5f, StatusMildYellow), Triple("Mal", 2.5f, StatusModerateOrange))
        } else {
          listOf(Triple("Calm", 8.5f, StatusStableGreen), Triple("Okay", 7f, CyanNeon), Triple("Tense", 4.5f, StatusMildYellow), Triple("Low", 2.5f, StatusModerateOrange))
        }
        opts.forEach { (lbl, score, col) ->
          QuickChoiceButton(
            text = lbl,
            color = col,
            modifier = Modifier.weight(1f),
            onClick = { onSelect(lbl, score, false) }
          )
        }
      }
    }

    VoiceInputKind.SYMPTOM_SEVERITY -> {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        QuickChoiceButton(
          text = if (isEs) "No" else "None",
          color = StatusStableGreen,
          modifier = Modifier.weight(1f),
          onClick = { onSelect("No", 0f, false) }
        )
        QuickChoiceButton(
          text = if (isEs) "Leve" else "Mild",
          color = StatusMildYellow,
          modifier = Modifier.weight(1f),
          onClick = { onSelect("Leve", 3f, false) }
        )
        QuickChoiceButton(
          text = if (isEs) "Fuerte" else "Strong",
          color = StatusModerateOrange,
          modifier = Modifier.weight(1f),
          onClick = { onSelect("Fuerte", 7f, false) }
        )
      }
    }

    VoiceInputKind.RED_FLAG_CHECK -> {
      Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          QuickChoiceButton(
            text = if (isEs) "No" else "None",
            color = StatusStableGreen,
            modifier = Modifier.weight(1f),
            onClick = { onSelect("No", 0f, false) }
          )
          QuickChoiceButton(
            text = if (isEs) "Leve" else "Mild",
            color = StatusMildYellow,
            modifier = Modifier.weight(1f),
            onClick = { onSelect("Leve", 3f, false) }
          )
        }
        QuickChoiceButton(
          text = if (isEs) "Dolor torácico / Ahogo / Desmayo" else "Chest pain / Severe breath / Fainting",
          color = StatusSevereRed,
          modifier = Modifier.fillMaxWidth(),
          onClick = { onSelect("Síntoma agudo", 9f, true) }
        )
      }
    }
  }
}

@Composable
private fun QuickScoreChip(
  text: String,
  onClick: () -> Unit,
  modifier: Modifier = Modifier
) {
  Surface(
    shape = RoundedCornerShape(10.dp),
    color = SpaceSurfaceCard,
    border = BorderStroke(1.dp, CyanNeon.copy(alpha = 0.4f)),
    modifier = modifier
      .heightIn(min = 48.dp)
      .clickable { onClick() }
  ) {
    Box(contentAlignment = Alignment.Center) {
      Text(
        text = text,
        style = MaterialTheme.typography.titleSmall,
        color = TextPrimary,
        fontWeight = FontWeight.Bold
      )
    }
  }
}

@Composable
private fun QuickChoiceButton(
  text: String,
  color: Color,
  onClick: () -> Unit,
  modifier: Modifier = Modifier
) {
  Surface(
    shape = RoundedCornerShape(12.dp),
    color = color.copy(alpha = 0.16f),
    border = BorderStroke(1.dp, color.copy(alpha = 0.65f)),
    modifier = modifier
      .heightIn(min = 50.dp)
      .clickable { onClick() }
  ) {
    Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(horizontal = 8.dp, vertical = 10.dp)) {
      Text(
        text = text,
        style = MaterialTheme.typography.labelMedium,
        color = color,
        fontWeight = FontWeight.Bold,
        textAlign = TextAlign.Center
      )
    }
  }
}

@Composable
private fun VoiceControlButton(
  icon: androidx.compose.ui.graphics.vector.ImageVector,
  label: String,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
  accent: Color = TextPrimary
) {
  Surface(
    shape = RoundedCornerShape(12.dp),
    color = SpaceSurfaceCard,
    border = BorderStroke(1.dp, SpaceBorder),
    modifier = modifier
      .heightIn(min = 54.dp)
      .clickable { onClick() }
  ) {
    Column(
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.Center,
      modifier = Modifier.padding(vertical = 8.dp, horizontal = 4.dp)
    ) {
      Icon(imageVector = icon, contentDescription = label, tint = accent, modifier = Modifier.size(18.dp))
      Spacer(modifier = Modifier.height(2.dp))
      Text(text = label, style = MaterialTheme.typography.labelSmall, color = accent, fontSize = 10.sp)
    }
  }
}

@Composable
private fun VoiceSummaryView(
  isEs: Boolean,
  score: Int,
  level: OperationalLevel,
  summary: String,
  recommendedTitle: String,
  recommendedDurationMin: Int,
  onStartVoiceIntervention: () -> Unit,
  onReturnDashboard: () -> Unit
) {
  Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
    SpaceCard(
      borderColor = CyanNeon.copy(alpha = 0.6f),
      backgroundColor = SpaceSurfaceCard
    ) {
      Text(
        text = if (isEs) "RESULTADO DEL CHEQUEO" else "CHECK-IN RESULT",
        style = MaterialTheme.typography.labelSmall,
        color = CyanNeon,
        fontWeight = FontWeight.Bold
      )
      Spacer(modifier = Modifier.height(10.dp))
      SemicircularActivationGauge(score = score, level = level)
      Spacer(modifier = Modifier.height(8.dp))
      Text(
        text = summary,
        style = MaterialTheme.typography.bodySmall,
        color = TextSecondary,
        textAlign = TextAlign.Center,
        modifier = Modifier.fillMaxWidth()
      )
    }

    SpaceCard(
      borderColor = TealLight,
      backgroundColor = SpaceSurfaceVariant
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Text(
          text = if (isEs) "INTERVENCIÓN RECOMENDADA" else "RECOMMENDED PROTOCOL",
          style = MaterialTheme.typography.labelSmall,
          color = TealLight,
          fontWeight = FontWeight.Bold
        )
        Text(
          text = "$recommendedDurationMin min",
          style = MaterialTheme.typography.labelSmall,
          color = CyanNeon,
          fontWeight = FontWeight.Bold
        )
      }
      Spacer(modifier = Modifier.height(6.dp))
      Text(
        text = recommendedTitle,
        style = MaterialTheme.typography.titleMedium,
        color = TextPrimary,
        fontWeight = FontWeight.Bold
      )
      Spacer(modifier = Modifier.height(12.dp))
      Button(
        onClick = onStartVoiceIntervention,
        modifier = Modifier
          .fillMaxWidth()
          .heightIn(min = 52.dp)
          .testTag("start_voice_recommended_intervention"),
        colors = ButtonDefaults.buttonColors(containerColor = CyanNeon, contentColor = SpaceDeepNavy),
        shape = RoundedCornerShape(12.dp)
      ) {
        Icon(imageVector = Icons.Default.RecordVoiceOver, contentDescription = null)
        Spacer(modifier = Modifier.width(8.dp))
        Text(
          text = if (isEs) "INICIAR INTERVENCIÓN POR VOZ" else "START VOICE INTERVENTION",
          fontWeight = FontWeight.Bold
        )
      }
    }

    OutlinedButton(
      onClick = onReturnDashboard,
      modifier = Modifier
        .fillMaxWidth()
        .heightIn(min = 48.dp),
      border = BorderStroke(1.dp, SpaceBorder),
      shape = RoundedCornerShape(12.dp)
    ) {
      Text(
        text = if (isEs) "VOLVER AL PANEL PRINCIPAL" else "RETURN TO DASHBOARD",
        color = TextSecondary,
        fontWeight = FontWeight.Bold
      )
    }
  }
}

@Composable
private fun MedicalRedFlagSummaryCard(
  isEs: Boolean,
  onOpenProtocol: () -> Unit,
  onClose: () -> Unit
) {
  SpaceCard(
    borderColor = StatusSevereRed,
    backgroundColor = SpaceSurfaceCard
  ) {
    Row(verticalAlignment = Alignment.CenterVertically) {
      Icon(
        imageVector = Icons.Default.MedicalServices,
        contentDescription = null,
        tint = StatusSevereRed,
        modifier = Modifier.size(30.dp)
      )
      Spacer(modifier = Modifier.width(10.dp))
      Text(
        text = if (isEs) "PROTOCOLO MÉDICO DE MISIÓN" else "MISSION MEDICAL PROTOCOL",
        style = MaterialTheme.typography.titleMedium,
        color = StatusSevereRed,
        fontWeight = FontWeight.ExtraBold
      )
    }
    Spacer(modifier = Modifier.height(10.dp))
    Text(
      text = if (isEs)
        "Reportaste un síntoma físico prioritario. SPACE MIND no atribuye síntomas físicos a ansiedad ni realiza diagnósticos. Contacta de inmediato con el Cirujano de Vuelo (Flight Surgeon)."
      else
        "Priority physical symptom reported. SPACE MIND does not attribute physical symptoms to anxiety or diagnose. Contact the Flight Surgeon immediately.",
      style = MaterialTheme.typography.bodyMedium,
      color = TextPrimary
    )
    Spacer(modifier = Modifier.height(14.dp))
    Button(
      onClick = onOpenProtocol,
      modifier = Modifier
        .fillMaxWidth()
        .heightIn(min = 52.dp),
      colors = ButtonDefaults.buttonColors(containerColor = StatusSevereRed, contentColor = Color.White),
      shape = RoundedCornerShape(12.dp)
    ) {
      Text(
        text = if (isEs) "VER PROTOCOLO DE EMERGENCIA" else "VIEW EMERGENCY PROTOCOL",
        fontWeight = FontWeight.Bold
      )
    }
    Spacer(modifier = Modifier.height(8.dp))
    TextButton(onClick = onClose, modifier = Modifier.fillMaxWidth()) {
      Text(text = if (isEs) "CERRAR" else "CLOSE", color = TextSecondary)
    }
  }
}
