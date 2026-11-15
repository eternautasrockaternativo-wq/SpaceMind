package com.example.ui.screens

import androidx.activity.compose.BackHandler
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.OperationalLevel
import com.example.ui.AppScreen
import com.example.ui.SpaceMindViewModel
import com.example.ui.components.ExpandableClinicalInfoCard
import com.example.ui.components.OperationalLevelBadge
import com.example.ui.components.SemicircularActivationGauge
import com.example.ui.components.SpaceCard
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Level4ClassificationScreen(
  viewModel: SpaceMindViewModel,
  modifier: Modifier = Modifier
) {
  BackHandler {
    viewModel.navigateTo(AppScreen.DASHBOARD)
  }

  val anxiety by viewModel.currentAnxiety.collectAsState()
  val sleepQuality by viewModel.currentSleepQuality.collectAsState()
  val symptoms by viewModel.somaticSymptoms.collectAsState()
  val baseline by viewModel.baseline.collectAsState()
  val interventions by viewModel.interventions.collectAsState()

  val classification = remember(anxiety, sleepQuality, symptoms, baseline) {
    viewModel.getOperationalClassification()
  }

  Scaffold(
    topBar = {
      TopAppBar(
        title = {
          Column {
            Text(
              text = "EVALUACIÓN CLÍNICA",
              style = MaterialTheme.typography.titleMedium,
              color = CyanNeon,
              fontWeight = FontWeight.Bold
            )
            Text(
              text = "Niveles 1-4 y Clasificación",
              style = MaterialTheme.typography.labelSmall,
              color = TextSecondary
            )
          }
        },
        navigationIcon = {
          IconButton(onClick = { viewModel.navigateTo(AppScreen.DASHBOARD) }) {
            Icon(
              imageVector = Icons.AutoMirrored.Filled.ArrowBack,
              contentDescription = "Volver",
              tint = CyanNeon
            )
          }
        },
        actions = {
          IconButton(onClick = { viewModel.setShowMedicalProtocol(true) }) {
            Icon(
              imageVector = Icons.Default.MedicalServices,
              contentDescription = "Protocolo de Emergencia",
              tint = StatusSevereRed
            )
          }
        },
        colors = TopAppBarDefaults.topAppBarColors(containerColor = SpaceDeepNavy)
      )
    },
    containerColor = SpaceDeepNavy
  ) { innerPadding ->
    LazyColumn(
      modifier = modifier
        .fillMaxSize()
        .padding(innerPadding)
        .padding(horizontal = 16.dp),
      verticalArrangement = Arrangement.spacedBy(14.dp),
      contentPadding = PaddingValues(top = 8.dp, bottom = 32.dp)
    ) {
      // Quick Level Navigation Bar
      item {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
          LevelPillButton(
            label = "N1 Basal",
            isSelected = false,
            color = CyanNeon,
            modifier = Modifier.weight(1f),
            onClick = { viewModel.navigateTo(AppScreen.LEVEL_1_BASELINE) }
          )
          LevelPillButton(
            label = "N2 Somático",
            isSelected = false,
            color = VioletNeon,
            modifier = Modifier.weight(1f),
            onClick = { viewModel.navigateTo(AppScreen.LEVEL_2_SOMATIC) }
          )
          LevelPillButton(
            label = "N3 TCC",
            isSelected = false,
            color = TealLight,
            modifier = Modifier.weight(1f),
            onClick = { viewModel.navigateTo(AppScreen.LEVEL_3_COGNITIVE) }
          )
          LevelPillButton(
            label = "N4 Estado",
            isSelected = true,
            color = StatusMildYellow,
            modifier = Modifier.weight(1f),
            onClick = { }
          )
        }
      }

      // Active Computed Status Gauge
      item {
        val levelBorder = when (classification.level) {
          OperationalLevel.ESTABLE -> StatusStableGreen
          OperationalLevel.ACTIVACION_LEVE -> StatusMildYellow
          OperationalLevel.ACTIVACION_MODERADA -> StatusModerateOrange
          OperationalLevel.ACTIVACION_ELEVADA -> StatusSevereRed
        }

        SpaceCard(
          borderColor = levelBorder,
          backgroundColor = SpaceSurfaceVariant
        ) {
          SemicircularActivationGauge(
            score = classification.compositeScore,
            level = classification.level
          )
          Spacer(modifier = Modifier.height(6.dp))
          Text(
            text = classification.summary,
            style = MaterialTheme.typography.bodySmall,
            color = TextSecondary
          )
        }
      }

      // Progressive 4-Level Visual Scale (Concise)
      item {
        Text(
          text = "ESCALA PROGRESIVA",
          style = MaterialTheme.typography.labelMedium,
          color = CyanNeon,
          fontWeight = FontWeight.Bold
        )
      }

      item {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
          CompactCategoryCard(
            name = "Estado Estable",
            code = "N1-EST",
            color = StatusStableGreen,
            indicators = "Ansiedad ≤ 3/10 • Sueño ≥ 8/10 • En rango basal",
            action = "Mindfulness preventivo",
            isCurrent = classification.level == OperationalLevel.ESTABLE
          )
          CompactCategoryCard(
            name = "Activación Leve",
            code = "N2-LEV",
            color = StatusMildYellow,
            indicators = "Ansiedad 3-5/10 • Tensión leve • Fatiga puntual",
            action = "Respiración 4-7-8 / Grounding",
            isCurrent = classification.level == OperationalLevel.ACTIVACION_LEVE
          )
          CompactCategoryCard(
            name = "Activación Moderada",
            code = "N3-MOD",
            color = StatusModerateOrange,
            indicators = "Ansiedad 5-7/10 • Alteración de sueño • Rumiación",
            action = "TCC / Relajación microgravedad",
            isCurrent = classification.level == OperationalLevel.ACTIVACION_MODERADA
          )
          CompactCategoryCard(
            name = "Activación Elevada",
            code = "N4-ELE",
            color = StatusSevereRed,
            indicators = "Ansiedad > 7/10 • Síntomas físicos intensos",
            action = "Protocolo STOP + Control Médico",
            isCurrent = classification.level == OperationalLevel.ACTIVACION_ELEVADA
          )
        }
      }

      // Recommended Interventions with Voice Option
      item {
        Text(
          text = "INTERVENCIONES RECOMENDADAS",
          style = MaterialTheme.typography.labelMedium,
          color = CyanNeon,
          fontWeight = FontWeight.Bold
        )
      }

      val recommendedList = interventions.filter { it.id in classification.recommendedInterventions }
      items(recommendedList) { prog ->
        SpaceCard(
          borderColor = CyanNeon.copy(alpha = 0.5f),
          backgroundColor = SpaceSurfaceCard
        ) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Column(modifier = Modifier.weight(1f)) {
              Text(
                text = prog.title,
                style = MaterialTheme.typography.titleSmall,
                color = TextPrimary,
                fontWeight = FontWeight.Bold
              )
              Text(
                text = "${prog.durationMin} min • ${prog.category}",
                style = MaterialTheme.typography.labelSmall,
                color = TextSecondary
              )
            }
            Button(
              onClick = {
                viewModel.startIntervention(prog, withVoice = true)
                viewModel.navigateTo(AppScreen.LEVEL_5_INTERVENTIONS)
              },
              colors = ButtonDefaults.buttonColors(containerColor = CyanNeon, contentColor = SpaceDeepNavy),
              shape = RoundedCornerShape(10.dp),
              modifier = Modifier
                .heightIn(min = 48.dp)
                .testTag("launch_rec_${prog.id}")
            ) {
              Icon(imageVector = Icons.Default.RecordVoiceOver, contentDescription = null, modifier = Modifier.size(16.dp))
              Spacer(modifier = Modifier.width(4.dp))
              Text("VOZ", fontWeight = FontWeight.Bold, fontSize = 11.sp)
            }
          }
        }
      }

      // Secondary Collapsible Non-Diagnostic Criterion
      item {
        ExpandableClinicalInfoCard(
          title = "Criterio no diagnóstico y factores",
          content = "Categorías operativas de factores humanos, no diagnósticos psiquiátricos. La frecuencia cardíaca elevada en microgravedad nunca se interpreta aisladamente como ansiedad.\n\nFactores activos:\n" + classification.rationale.joinToString("\n• ", prefix = "• ")
        )
      }
    }
  }
}

@Composable
private fun LevelPillButton(
  label: String,
  isSelected: Boolean,
  color: Color,
  onClick: () -> Unit,
  modifier: Modifier = Modifier
) {
  Surface(
    shape = RoundedCornerShape(10.dp),
    color = if (isSelected) color.copy(alpha = 0.25f) else SpaceSurface,
    border = BorderStroke(1.dp, if (isSelected) color else SpaceBorder),
    modifier = modifier
      .heightIn(min = 48.dp)
      .clickable { onClick() }
  ) {
    Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(vertical = 8.dp)) {
      Text(
        text = label,
        style = MaterialTheme.typography.labelSmall,
        color = if (isSelected) color else TextSecondary,
        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
        fontSize = 10.sp
      )
    }
  }
}

@Composable
private fun CompactCategoryCard(
  name: String,
  code: String,
  color: Color,
  indicators: String,
  action: String,
  isCurrent: Boolean
) {
  Box(
    modifier = Modifier
      .fillMaxWidth()
      .clip(RoundedCornerShape(14.dp))
      .background(if (isCurrent) color.copy(alpha = 0.14f) else SpaceSurface)
      .border(if (isCurrent) 2.dp else 1.dp, if (isCurrent) color else SpaceBorder, RoundedCornerShape(14.dp))
      .padding(12.dp)
  ) {
    Column {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(color))
          Spacer(modifier = Modifier.width(8.dp))
          Text(
            text = name,
            style = MaterialTheme.typography.titleSmall,
            color = if (isCurrent) color else TextPrimary,
            fontWeight = FontWeight.Bold
          )
        }
        Text(
          text = code,
          style = MaterialTheme.typography.labelSmall,
          color = color,
          fontWeight = FontWeight.Bold
        )
      }
      Spacer(modifier = Modifier.height(4.dp))
      Text(text = indicators, style = MaterialTheme.typography.bodySmall, color = TextSecondary, fontSize = 11.sp)
      Text(text = "Acción: $action", style = MaterialTheme.typography.labelSmall, color = color, fontSize = 11.sp)
    }
  }
}
