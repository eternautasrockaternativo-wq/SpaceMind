package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import com.example.model.BaselineAssessment
import com.example.ui.AppScreen
import com.example.ui.SpaceMindViewModel
import com.example.ui.components.ExpandableClinicalInfoCard
import com.example.ui.components.SpaceCard
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Level1BaselineScreen(
  viewModel: SpaceMindViewModel,
  modifier: Modifier = Modifier
) {
  BackHandler {
    viewModel.navigateTo(AppScreen.DASHBOARD)
  }

  val baselineState by viewModel.baseline.collectAsState()
  val currentAnxiety by viewModel.currentAnxiety.collectAsState()

  var traitAnxiety by remember(baselineState) { mutableFloatStateOf(baselineState.traitAnxiety) }
  var sleepQuality by remember(baselineState) { mutableFloatStateOf(baselineState.baselineSleepQuality) }
  var perceivedStress by remember(baselineState) { mutableFloatStateOf(baselineState.perceivedStress) }
  var emotionalState by remember(baselineState) { mutableFloatStateOf(baselineState.emotionalState) }
  var concentration by remember(baselineState) { mutableFloatStateOf(baselineState.concentrationAbility) }
  var isolation by remember(baselineState) { mutableFloatStateOf(baselineState.isolationPerception) }
  var pastStressors by remember(baselineState) { mutableStateOf(baselineState.pastStressorsNotes) }
  var typicalSomatic by remember(baselineState) { mutableStateOf(baselineState.typicalSomaticHabits) }
  var stepIndex by remember { mutableIntStateOf(0) }
  var savedSnackbar by remember { mutableStateOf(false) }

  val scaleItems = listOf(
    Triple("Ansiedad Habitual", traitAnxiety, VioletNeon),
    Triple("Calidad del Sueño", sleepQuality, TealLight),
    Triple("Estrés en Misión", perceivedStress, StatusMildYellow),
    Triple("Estado de Ánimo", emotionalState, CyanNeon),
    Triple("Concentración", concentration, CyanGlow),
    Triple("Aislamiento", isolation, VioletSoft)
  )

  Scaffold(
    topBar = {
      TopAppBar(
        title = {
          Column {
            Text(
              text = "NIVEL 1: LÍNEA DE BASE",
              style = MaterialTheme.typography.titleMedium,
              color = CyanNeon,
              fontWeight = FontWeight.Bold
            )
            Text(
              text = baselineState.lastUpdated,
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
      // Visual Comparison Glance
      item {
        SpaceCard(
          borderColor = VioletNeon.copy(alpha = 0.45f),
          backgroundColor = SpaceSurfaceCard
        ) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceAround
          ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
              Text(text = "Basal", style = MaterialTheme.typography.labelSmall, color = TextMuted)
              Text(
                text = "%.1f".format(baselineState.traitAnxiety),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = CyanNeon
              )
            }
            Box(modifier = Modifier.width(1.dp).height(40.dp).background(SpaceBorder))
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
              Text(text = "Hoy", style = MaterialTheme.typography.labelSmall, color = TextMuted)
              Text(
                text = "%.1f".format(currentAnxiety),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = if (currentAnxiety > baselineState.traitAnxiety + 2f) StatusModerateOrange else StatusStableGreen
              )
            }
            Box(modifier = Modifier.width(1.dp).height(40.dp).background(SpaceBorder))
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
              Text(text = "Delta", style = MaterialTheme.typography.labelSmall, color = TextMuted)
              val delta = currentAnxiety - baselineState.traitAnxiety
              Text(
                text = "${if (delta >= 0) "+" else ""}${"%.1f".format(delta)}",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = if (delta > 2f) StatusModerateOrange else TealLight
              )
            }
          }
        }
      }

      // Single-Question-at-a-Time Visual Scale Calibrator
      item {
        val currentTuple = scaleItems[stepIndex]
        val activeColor = currentTuple.third
        val currentVal = when (stepIndex) {
          0 -> traitAnxiety
          1 -> sleepQuality
          2 -> perceivedStress
          3 -> emotionalState
          4 -> concentration
          else -> isolation
        }

        SpaceCard(
          borderColor = activeColor.copy(alpha = 0.6f),
          backgroundColor = SpaceSurfaceVariant
        ) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Text(
              text = "ESCALA ${stepIndex + 1} / ${scaleItems.size}",
              style = MaterialTheme.typography.labelSmall,
              color = activeColor,
              fontWeight = FontWeight.Bold
            )
            Text(
              text = "%.1f / 10".format(currentVal),
              style = MaterialTheme.typography.titleLarge,
              fontWeight = FontWeight.ExtraBold,
              color = activeColor
            )
          }

          Spacer(modifier = Modifier.height(6.dp))
          Text(
            text = currentTuple.first,
            style = MaterialTheme.typography.titleMedium,
            color = Color.White,
            fontWeight = FontWeight.Bold
          )

          Spacer(modifier = Modifier.height(8.dp))
          Slider(
            value = currentVal,
            onValueChange = { v ->
              when (stepIndex) {
                0 -> traitAnxiety = v
                1 -> sleepQuality = v
                2 -> perceivedStress = v
                3 -> emotionalState = v
                4 -> concentration = v
                else -> isolation = v
              }
            },
            valueRange = 0f..10f,
            steps = 9,
            colors = SliderDefaults.colors(
              thumbColor = activeColor,
              activeTrackColor = activeColor,
              inactiveTrackColor = SpaceBorder
            ),
            modifier = Modifier.testTag("slider_base_step")
          )

          Spacer(modifier = Modifier.height(8.dp))
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            OutlinedButton(
              onClick = { if (stepIndex > 0) stepIndex-- },
              enabled = stepIndex > 0,
              modifier = Modifier.weight(1f).heightIn(min = 48.dp),
              border = BorderStroke(1.dp, SpaceBorder),
              shape = RoundedCornerShape(10.dp)
            ) {
              Text("ANTERIOR", color = TextPrimary, fontWeight = FontWeight.Bold)
            }
            Button(
              onClick = { if (stepIndex < scaleItems.lastIndex) stepIndex++ },
              enabled = stepIndex < scaleItems.lastIndex,
              modifier = Modifier.weight(1f).heightIn(min = 48.dp),
              colors = ButtonDefaults.buttonColors(containerColor = activeColor, contentColor = SpaceDeepNavy),
              shape = RoundedCornerShape(10.dp)
            ) {
              Text("SIGUIENTE", fontWeight = FontWeight.Bold)
            }
          }
        }
      }

      // Compact Summary Overview of All 6 Dimensions
      item {
        SpaceCard(
          borderColor = SpaceBorder,
          backgroundColor = SpaceSurface
        ) {
          Text(
            text = "RESUMEN BASAL (0-10)",
            style = MaterialTheme.typography.labelSmall,
            color = CyanNeon,
            fontWeight = FontWeight.Bold
          )
          Spacer(modifier = Modifier.height(8.dp))
          scaleItems.forEachIndexed { idx, (label, v, color) ->
            Row(
              modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 3.dp),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Text(text = label, style = MaterialTheme.typography.bodySmall, color = TextSecondary, modifier = Modifier.weight(1f))
              LinearProgressIndicator(
                progress = { v / 10f },
                modifier = Modifier.width(100.dp).height(6.dp).clip(RoundedCornerShape(3.dp)),
                color = color,
                trackColor = SpaceSurfaceCard
              )
              Spacer(modifier = Modifier.width(10.dp))
              Text(text = "%.1f".format(v), style = MaterialTheme.typography.labelMedium, color = color, fontWeight = FontWeight.Bold)
            }
          }
        }
      }

      // Brief Notes Fields (Collapsible/Compact)
      item {
        SpaceCard(
          borderColor = SpaceBorder,
          backgroundColor = SpaceSurface
        ) {
          Text(
            text = "NOTAS RÁPIDAS",
            style = MaterialTheme.typography.labelSmall,
            color = CyanNeon,
            fontWeight = FontWeight.Bold
          )
          Spacer(modifier = Modifier.height(8.dp))
          OutlinedTextField(
            value = pastStressors,
            onValueChange = { pastStressors = it },
            label = { Text("Estresores previos") },
            modifier = Modifier.fillMaxWidth().testTag("past_stressors_field"),
            maxLines = 2,
            textStyle = MaterialTheme.typography.bodySmall
          )
          Spacer(modifier = Modifier.height(8.dp))
          OutlinedTextField(
            value = typicalSomatic,
            onValueChange = { typicalSomatic = it },
            label = { Text("Patrón físico habitual") },
            modifier = Modifier.fillMaxWidth().testTag("typical_somatic_field"),
            maxLines = 2,
            textStyle = MaterialTheme.typography.bodySmall
          )
        }
      }

      // Save Action Button
      item {
        Button(
          onClick = {
            viewModel.updateBaseline(
              BaselineAssessment(
                traitAnxiety = traitAnxiety,
                baselineSleepQuality = sleepQuality,
                perceivedStress = perceivedStress,
                emotionalState = emotionalState,
                concentrationAbility = concentration,
                isolationPerception = isolation,
                pastStressorsNotes = pastStressors,
                typicalSomaticHabits = typicalSomatic,
                isCompleted = true,
                lastUpdated = "Actualizado Hoy"
              )
            )
            savedSnackbar = true
          },
          modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 52.dp)
            .testTag("save_baseline_button"),
          colors = ButtonDefaults.buttonColors(containerColor = CyanNeon, contentColor = SpaceDeepNavy),
          shape = RoundedCornerShape(12.dp)
        ) {
          Icon(imageVector = Icons.Default.Save, contentDescription = null)
          Spacer(modifier = Modifier.width(8.dp))
          Text(text = "GUARDAR LÍNEA DE BASE", fontWeight = FontWeight.Bold)
        }
      }

      if (savedSnackbar) {
        item {
          Box(
            modifier = Modifier
              .fillMaxWidth()
              .clip(RoundedCornerShape(8.dp))
              .background(StatusStableGreen.copy(alpha = 0.2f))
              .border(1.dp, StatusStableGreen, RoundedCornerShape(8.dp))
              .padding(12.dp)
          ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Icon(imageVector = Icons.Default.Check, contentDescription = null, tint = StatusStableGreen)
              Spacer(modifier = Modifier.width(8.dp))
              Text(
                text = "Línea de base actualizada.",
                style = MaterialTheme.typography.bodySmall,
                color = StatusStableGreen,
                fontWeight = FontWeight.Bold
              )
            }
          }
        }
      }

      // Secondary Collapsible Info
      item {
        ExpandableClinicalInfoCard(
          title = "Fundamento clínico (Info ampliada)",
          content = "La línea de base individual permite comparar cada medición futura frente al perfil habitual del astronauta, evitando falsos positivos ante adaptaciones normales en microgravedad."
        )
      }
    }
  }
}
