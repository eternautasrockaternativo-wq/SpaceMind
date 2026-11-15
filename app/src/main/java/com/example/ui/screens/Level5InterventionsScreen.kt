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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.model.InteractiveExerciseType
import com.example.model.InterventionProgram
import com.example.ui.AppScreen
import com.example.ui.SpaceMindViewModel
import com.example.ui.components.ExpandableClinicalInfoCard
import com.example.ui.components.InteractiveBreathingVisualizer
import com.example.ui.components.SpaceCard
import com.example.ui.components.VoiceWaveformVisualizer
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Level5InterventionsScreen(
  viewModel: SpaceMindViewModel,
  modifier: Modifier = Modifier
) {
  val activeProgram by viewModel.activeIntervention.collectAsState()

  BackHandler {
    if (activeProgram != null) {
      viewModel.closeIntervention()
    } else {
      viewModel.navigateTo(AppScreen.DASHBOARD)
    }
  }

  val interventions by viewModel.interventions.collectAsState()

  Scaffold(
    topBar = {
      TopAppBar(
        title = {
          Column {
            Text(
              text = if (activeProgram != null) activeProgram!!.title.uppercase() else "INTERVENCIONES",
              style = MaterialTheme.typography.titleMedium,
              color = CyanNeon,
              fontWeight = FontWeight.Bold
            )
            Text(
              text = if (activeProgram != null) "Protocolo Activo" else "10 Técnicas de Regulación",
              style = MaterialTheme.typography.labelSmall,
              color = TextSecondary
            )
          }
        },
        navigationIcon = {
          IconButton(onClick = {
            if (activeProgram != null) viewModel.closeIntervention() else viewModel.navigateTo(AppScreen.DASHBOARD)
          }) {
            Icon(
              imageVector = Icons.AutoMirrored.Filled.ArrowBack,
              contentDescription = "Volver",
              tint = CyanNeon
            )
          }
        },
        colors = TopAppBarDefaults.topAppBarColors(containerColor = SpaceDeepNavy)
      )
    },
    containerColor = SpaceDeepNavy
  ) { innerPadding ->
    if (activeProgram != null) {
      ActiveInterventionSessionView(
        program = activeProgram!!,
        viewModel = viewModel,
        modifier = modifier.padding(innerPadding)
      )
    } else {
      InterventionsCatalogView(
        interventions = interventions,
        onSelectProgram = { prog, withVoice -> viewModel.startIntervention(prog, withVoice) },
        modifier = modifier.padding(innerPadding)
      )
    }
  }
}

@Composable
private fun InterventionsCatalogView(
  interventions: List<InterventionProgram>,
  onSelectProgram: (InterventionProgram, Boolean) -> Unit,
  modifier: Modifier = Modifier
) {
  LazyColumn(
    modifier = modifier
      .fillMaxSize()
      .padding(horizontal = 16.dp),
    verticalArrangement = Arrangement.spacedBy(12.dp),
    contentPadding = PaddingValues(top = 8.dp, bottom = 32.dp)
  ) {
    items(interventions) { prog ->
      InterventionProgramListItem(
        program = prog,
        onStart = { onSelectProgram(prog, false) },
        onStartVoice = { onSelectProgram(prog, true) }
      )
    }
  }
}

@Composable
private fun InterventionProgramListItem(
  program: InterventionProgram,
  onStart: () -> Unit,
  onStartVoice: () -> Unit
) {
  val icon = getInterventionIcon(program.id)
  val categoryColor = when (program.orderIndex % 4) {
    0 -> VioletNeon
    1 -> CyanNeon
    2 -> TealLight
    else -> StatusMildYellow
  }

  SpaceCard(
    borderColor = categoryColor.copy(alpha = 0.45f),
    backgroundColor = SpaceSurfaceCard
  ) {
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
        Box(
          modifier = Modifier
            .size(44.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(categoryColor.copy(alpha = 0.15f))
            .border(1.dp, categoryColor.copy(alpha = 0.6f), RoundedCornerShape(12.dp)),
          contentAlignment = Alignment.Center
        ) {
          Icon(
            imageVector = icon,
            contentDescription = null,
            tint = categoryColor,
            modifier = Modifier.size(22.dp)
          )
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
              text = "#0${program.orderIndex} • ${program.category.uppercase()}",
              style = MaterialTheme.typography.labelSmall,
              color = categoryColor,
              fontWeight = FontWeight.Bold,
              fontSize = 10.sp
            )
          }
          Text(
            text = program.title,
            style = MaterialTheme.typography.titleMedium,
            color = TextPrimary,
            fontWeight = FontWeight.Bold
          )
        }
      }

      Surface(
        shape = RoundedCornerShape(10.dp),
        color = categoryColor.copy(alpha = 0.15f),
        border = BorderStroke(1.dp, categoryColor.copy(alpha = 0.4f))
      ) {
        Text(
          text = "${program.durationMin}m",
          style = MaterialTheme.typography.labelSmall,
          color = categoryColor,
          fontWeight = FontWeight.Bold,
          modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
      }
    }

    Spacer(modifier = Modifier.height(6.dp))
    Text(
      text = program.indications,
      style = MaterialTheme.typography.bodySmall,
      color = TextSecondary,
      maxLines = 2
    )

    Spacer(modifier = Modifier.height(10.dp))
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
      Button(
        onClick = onStart,
        modifier = Modifier
          .weight(1f)
          .heightIn(min = 48.dp)
          .testTag("start_int_${program.id}"),
        colors = ButtonDefaults.buttonColors(containerColor = SpaceSurface, contentColor = categoryColor),
        border = BorderStroke(1.dp, categoryColor.copy(alpha = 0.7f)),
        shape = RoundedCornerShape(10.dp)
      ) {
        Icon(imageVector = Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
        Spacer(modifier = Modifier.width(4.dp))
        Text(text = "INICIAR", fontWeight = FontWeight.Bold, fontSize = 12.sp)
      }

      Button(
        onClick = onStartVoice,
        modifier = Modifier
          .weight(1f)
          .heightIn(min = 48.dp)
          .testTag("start_voice_int_${program.id}"),
        colors = ButtonDefaults.buttonColors(containerColor = categoryColor, contentColor = SpaceDeepNavy),
        shape = RoundedCornerShape(10.dp)
      ) {
        Icon(imageVector = Icons.Default.RecordVoiceOver, contentDescription = null, modifier = Modifier.size(16.dp))
        Spacer(modifier = Modifier.width(4.dp))
        Text(text = "GUÍA VOZ", fontWeight = FontWeight.Bold, fontSize = 12.sp)
      }
    }
  }
}

@Composable
private fun ActiveInterventionSessionView(
  program: InterventionProgram,
  viewModel: SpaceMindViewModel,
  modifier: Modifier = Modifier
) {
  val context = LocalContext.current
  val step by viewModel.exerciseStep.collectAsState()
  val timerSeconds by viewModel.timerSeconds.collectAsState()
  val isTimerRunning by viewModel.isTimerRunning.collectAsState()
  val preAnxiety by viewModel.preExerciseAnxiety.collectAsState()
  val postAnxiety by viewModel.postExerciseAnxiety.collectAsState()
  val voiceState by viewModel.voiceInterventionState.collectAsState()
  val voiceEvalState by viewModel.voiceEvalState.collectAsState()

  val permissionLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.RequestPermission()
  ) { granted ->
    viewModel.updateMicPermission(granted)
    if (granted) {
      viewModel.toggleInterventionVoiceCommandMic()
    }
  }

  LaunchedEffect(Unit) {
    val granted = ContextCompat.checkSelfPermission(
      context,
      Manifest.permission.RECORD_AUDIO
    ) == PackageManager.PERMISSION_GRANTED
    viewModel.updateMicPermission(granted)
  }

  LazyColumn(
    modifier = modifier
      .fillMaxSize()
      .padding(horizontal = 16.dp),
    verticalArrangement = Arrangement.spacedBy(14.dp),
    contentPadding = PaddingValues(top = 8.dp, bottom = 40.dp)
  ) {
    // 1. VOICE AI GUIDE BAR
    item {
      SpaceCard(
        borderColor = if (voiceState.isVoiceModeEnabled) CyanNeon else SpaceBorder,
        backgroundColor = SpaceSurfaceVariant
      ) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
              imageVector = Icons.Default.RecordVoiceOver,
              contentDescription = null,
              tint = CyanNeon,
              modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
              text = "GUÍA POR VOZ",
              style = MaterialTheme.typography.labelMedium,
              color = CyanNeon,
              fontWeight = FontWeight.Bold
            )
          }
          Switch(
            checked = voiceState.isVoiceModeEnabled,
            onCheckedChange = { viewModel.toggleInterventionVoiceMode() },
            colors = SwitchDefaults.colors(checkedThumbColor = SpaceDeepNavy, checkedTrackColor = CyanNeon)
          )
        }

        if (voiceState.isVoiceModeEnabled) {
          Spacer(modifier = Modifier.height(8.dp))
          VoiceWaveformVisualizer(
            isSpeaking = voiceState.isSpeaking,
            isListening = voiceState.isListening,
            isProcessing = false,
            rmsLevel = voiceState.rmsLevel
          )
          Spacer(modifier = Modifier.height(6.dp))
          Text(
            text = voiceState.statusText.ifBlank { "Di 'Continuar', 'Repetir', 'Pausar' o 'Finalizar'" },
            style = MaterialTheme.typography.labelSmall,
            color = TealLight
          )
          Spacer(modifier = Modifier.height(10.dp))

          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
          ) {
            IconButton(
              onClick = {
                if (voiceEvalState.hasMicPermission) {
                  viewModel.toggleInterventionVoiceCommandMic()
                } else {
                  permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                }
              },
              modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(if (voiceState.isListening) TealLight else CyanNeon)
                .testTag("intervention_voice_mic_button")
            ) {
              Icon(
                imageVector = if (voiceState.isListening) Icons.Default.Stop else Icons.Default.Mic,
                contentDescription = "Comando de voz",
                tint = SpaceDeepNavy
              )
            }

            VoiceQuickActionPill("Continuar", CyanNeon, Modifier.weight(1f)) {
              viewModel.interventionVoiceContinue()
            }
            VoiceQuickActionPill("Repetir", VioletSoft, Modifier.weight(1f)) {
              viewModel.interventionVoiceRepeat()
            }
            VoiceQuickActionPill("Pausar", StatusMildYellow, Modifier.weight(1f)) {
              viewModel.interventionVoicePause()
            }
            VoiceQuickActionPill("Finalizar", StatusStableGreen, Modifier.weight(1f)) {
              viewModel.interventionVoiceFinish()
            }
          }
        }
      }
    }

    // 2. Step by Step Card (1 focused instruction at a time)
    item {
      SpaceCard(
        borderColor = VioletNeon.copy(alpha = 0.55f),
        backgroundColor = SpaceSurfaceCard
      ) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Text(
            text = "PASO ${step + 1} DE ${program.steps.size}",
            style = MaterialTheme.typography.labelMedium,
            color = VioletSoft,
            fontWeight = FontWeight.Bold
          )
          LinearProgressIndicator(
            progress = { (step + 1).toFloat() / program.steps.size.toFloat() },
            modifier = Modifier
              .width(100.dp)
              .height(6.dp)
              .clip(RoundedCornerShape(3.dp)),
            color = VioletNeon,
            trackColor = SpaceBorder
          )
        }

        Spacer(modifier = Modifier.height(10.dp))
        Text(
          text = program.steps[step.coerceIn(0, program.steps.size - 1)],
          style = MaterialTheme.typography.titleMedium,
          color = Color.White,
          fontWeight = FontWeight.SemiBold,
          lineHeight = 24.sp
        )

        Spacer(modifier = Modifier.height(14.dp))
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          Button(
            onClick = { if (step > 0) viewModel.setExerciseStep(step - 1) },
            enabled = step > 0,
            modifier = Modifier
              .weight(1f)
              .heightIn(min = 48.dp),
            colors = ButtonDefaults.buttonColors(containerColor = SpaceBorder, contentColor = TextPrimary),
            shape = RoundedCornerShape(10.dp)
          ) {
            Text("ANTERIOR", fontWeight = FontWeight.Bold)
          }

          Button(
            onClick = {
              if (voiceState.isVoiceModeEnabled) viewModel.interventionVoiceContinue()
              else if (step < program.steps.size - 1) viewModel.setExerciseStep(step + 1)
            },
            enabled = step < program.steps.size - 1,
            modifier = Modifier
              .weight(1f)
              .heightIn(min = 48.dp),
            colors = ButtonDefaults.buttonColors(containerColor = VioletNeon, contentColor = Color.White),
            shape = RoundedCornerShape(10.dp)
          ) {
            Text("CONTINUAR", fontWeight = FontWeight.Bold)
          }
        }
      }
    }

    // 3. Breathing Visualizer & Active Timer
    item {
      SpaceCard(
        borderColor = CyanNeon.copy(alpha = 0.6f),
        backgroundColor = SpaceSurface
      ) {
        if (program.exerciseType == InteractiveExerciseType.BREATHING_TIMER) {
          InteractiveBreathingVisualizer(isRunning = isTimerRunning)
          Spacer(modifier = Modifier.height(8.dp))
        }

        val mins = timerSeconds / 60
        val secs = timerSeconds % 60
        Text(
          text = "%02d:%02d".format(mins, secs),
          style = MaterialTheme.typography.displaySmall,
          color = CyanNeon,
          fontWeight = FontWeight.ExtraBold,
          modifier = Modifier.align(Alignment.CenterHorizontally)
        )

        Spacer(modifier = Modifier.height(8.dp))
        Button(
          onClick = { viewModel.toggleTimer() },
          modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 50.dp)
            .testTag("toggle_timer_button"),
          colors = ButtonDefaults.buttonColors(
            containerColor = if (isTimerRunning) StatusSevereRed else CyanNeon,
            contentColor = SpaceDeepNavy
          ),
          shape = RoundedCornerShape(10.dp)
        ) {
          Icon(
            imageVector = if (isTimerRunning) Icons.Default.Pause else Icons.Default.PlayArrow,
            contentDescription = null
          )
          Spacer(modifier = Modifier.width(8.dp))
          Text(
            text = if (isTimerRunning) "PAUSAR" else "INICIAR TEMPORIZADOR",
            fontWeight = FontWeight.Bold
          )
        }
      }
    }

    // 4. Compact Pre & Post Anxiety Registration
    item {
      SpaceCard(
        borderColor = StatusStableGreen.copy(alpha = 0.7f),
        backgroundColor = SpaceSurfaceVariant
      ) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween
        ) {
          Text(text = "Ansiedad Pre: ${"%.0f".format(preAnxiety)}/10", style = MaterialTheme.typography.labelMedium, color = StatusModerateOrange, fontWeight = FontWeight.Bold)
          Text(text = "Post: ${"%.0f".format(postAnxiety)}/10", style = MaterialTheme.typography.labelMedium, color = StatusStableGreen, fontWeight = FontWeight.Bold)
        }
        Slider(
          value = preAnxiety,
          onValueChange = { viewModel.setPreExerciseAnxiety(it) },
          valueRange = 0f..10f,
          steps = 9,
          colors = SliderDefaults.colors(thumbColor = StatusModerateOrange, activeTrackColor = StatusModerateOrange),
          modifier = Modifier.testTag("pre_anxiety_slider")
        )
        Slider(
          value = postAnxiety,
          onValueChange = { viewModel.setPostExerciseAnxiety(it) },
          valueRange = 0f..10f,
          steps = 9,
          colors = SliderDefaults.colors(thumbColor = StatusStableGreen, activeTrackColor = StatusStableGreen),
          modifier = Modifier.testTag("post_anxiety_slider")
        )

        Spacer(modifier = Modifier.height(8.dp))
        Button(
          onClick = { viewModel.completeActiveIntervention() },
          modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 52.dp)
            .testTag("complete_intervention_button"),
          colors = ButtonDefaults.buttonColors(containerColor = StatusStableGreen, contentColor = SpaceDeepNavy),
          shape = RoundedCornerShape(10.dp)
        ) {
          Icon(imageVector = Icons.Default.CheckCircle, contentDescription = null)
          Spacer(modifier = Modifier.width(8.dp))
          Text(text = "FINALIZAR Y REGISTRAR", fontWeight = FontWeight.Bold)
        }
      }
    }

    // 5. Secondary Collapsible Clinical Details (reduces reading during active use)
    item {
      ExpandableClinicalInfoCard(
        title = "Información clínica ampliada",
        content = "Objetivo: ${program.scientificBasis}\n\nIndicado: ${program.indications}\n\nPrecaución: ${program.contraindications}"
      )
    }
  }
}

@Composable
private fun VoiceQuickActionPill(
  text: String,
  color: Color,
  modifier: Modifier = Modifier,
  onClick: () -> Unit
) {
  Surface(
    shape = RoundedCornerShape(10.dp),
    color = color.copy(alpha = 0.16f),
    border = BorderStroke(1.dp, color.copy(alpha = 0.6f)),
    modifier = modifier
      .heightIn(min = 48.dp)
      .clickable { onClick() }
  ) {
    Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(horizontal = 4.dp)) {
      Text(
        text = text,
        style = MaterialTheme.typography.labelSmall,
        color = color,
        fontWeight = FontWeight.Bold,
        fontSize = 10.sp
      )
    }
  }
}

private fun getInterventionIcon(id: String): ImageVector {
  return when (id) {
    "INT-01" -> Icons.Default.Air
    "INT-02" -> Icons.Default.SelfImprovement
    "INT-03" -> Icons.Default.Explore
    "INT-04" -> Icons.Default.Psychology
    "INT-05" -> Icons.Default.FitnessCenter
    "INT-06" -> Icons.Default.Healing
    "INT-07" -> Icons.Default.Bedtime
    "INT-08" -> Icons.Default.Public
    "INT-09" -> Icons.Default.Handyman
    "INT-10" -> Icons.Default.RecordVoiceOver
    else -> Icons.Default.Spa
  }
}
