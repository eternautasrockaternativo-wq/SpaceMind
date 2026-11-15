package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import com.example.model.SomaticCategory
import com.example.model.SomaticSymptom
import com.example.ui.AppScreen
import com.example.ui.SpaceMindViewModel
import com.example.ui.components.ExpandableClinicalInfoCard
import com.example.ui.components.SpaceCard
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Level2SomaticScreen(
  viewModel: SpaceMindViewModel,
  modifier: Modifier = Modifier
) {
  BackHandler {
    viewModel.navigateTo(AppScreen.DASHBOARD)
  }

  val symptoms by viewModel.somaticSymptoms.collectAsState()
  val selectedCategory by viewModel.selectedSomaticCategory.collectAsState()
  var editingSymptom by remember { mutableStateOf<SomaticSymptom?>(null) }

  Scaffold(
    topBar = {
      TopAppBar(
        title = {
          Column {
            Text(
              text = "NIVEL 2: SÍNTOMAS SOMÁTICOS",
              style = MaterialTheme.typography.titleMedium,
              color = CyanNeon,
              fontWeight = FontWeight.Bold
            )
            Text(
              text = "Registro físico sin atribución automática",
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
      verticalArrangement = Arrangement.spacedBy(12.dp),
      contentPadding = PaddingValues(top = 8.dp, bottom = 32.dp)
    ) {
      // 4 Dimension Category Tabs
      item {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          SomaticCategoryTab(
            title = "Cardio",
            icon = Icons.Default.Favorite,
            isSelected = selectedCategory == SomaticCategory.CARDIOVASCULAR,
            activeColor = TelemetryHrColor,
            onClick = { viewModel.setSomaticCategory(SomaticCategory.CARDIOVASCULAR) },
            modifier = Modifier.weight(1f)
          )
          SomaticCategoryTab(
            title = "Respira",
            icon = Icons.Default.Air,
            isSelected = selectedCategory == SomaticCategory.RESPIRATORY,
            activeColor = TelemetryRespColor,
            onClick = { viewModel.setSomaticCategory(SomaticCategory.RESPIRATORY) },
            modifier = Modifier.weight(1f)
          )
          SomaticCategoryTab(
            title = "Muscular",
            icon = Icons.Default.FitnessCenter,
            isSelected = selectedCategory == SomaticCategory.MUSCULAR_NEUROLOGICAL,
            activeColor = VioletNeon,
            onClick = { viewModel.setSomaticCategory(SomaticCategory.MUSCULAR_NEUROLOGICAL) },
            modifier = Modifier.weight(1f)
          )
          SomaticCategoryTab(
            title = "General",
            icon = Icons.Default.Spa,
            isSelected = selectedCategory == SomaticCategory.GASTROINTESTINAL_GENERAL,
            activeColor = TealLight,
            onClick = { viewModel.setSomaticCategory(SomaticCategory.GASTROINTESTINAL_GENERAL) },
            modifier = Modifier.weight(1f)
          )
        }
      }

      val filteredSymptoms = symptoms.filter { it.category == selectedCategory }

      item {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Text(
            text = selectedCategory.displayName.uppercase(),
            style = MaterialTheme.typography.labelMedium,
            color = TextPrimary,
            fontWeight = FontWeight.Bold
          )
          Text(
            text = "${filteredSymptoms.count { it.isReported }}/${filteredSymptoms.size} activos",
            style = MaterialTheme.typography.labelSmall,
            color = CyanGlow
          )
        }
      }

      items(filteredSymptoms) { symptom ->
        SomaticSymptomCard(
          symptom = symptom,
          onToggleReported = { reported ->
            viewModel.updateSymptom(
              symptomId = symptom.id,
              isReported = reported,
              intensity = if (reported && symptom.intensity == 0f) 3f else symptom.intensity,
              frequency = symptom.frequency,
              duration = symptom.duration,
              context = symptom.context,
              triggers = symptom.possibleTrigger
            )
          },
          onEditDetails = { editingSymptom = symptom }
        )
      }

      // Secondary Collapsible Psychoeducation Card
      item {
        ExpandableClinicalInfoCard(
          title = "Criterio clínico y microgravedad",
          content = "Nunca atribuimos automáticamente un síntoma físico a ansiedad. Contrasta siempre los síntomas con CO2, O2 y presión de cabina. Dolor torácico o disnea severa activan protocolo médico inmediato."
        )
      }
    }
  }

  editingSymptom?.let { symptom ->
    SymptomDetailDialog(
      symptom = symptom,
      onDismiss = { editingSymptom = null },
      onSave = { updatedIntensity, freq, dur, ctx, trig ->
        viewModel.updateSymptom(
          symptomId = symptom.id,
          isReported = true,
          intensity = updatedIntensity,
          frequency = freq,
          duration = dur,
          context = ctx,
          triggers = trig
        )
        editingSymptom = null
      }
    )
  }
}

@Composable
private fun SomaticCategoryTab(
  title: String,
  icon: androidx.compose.ui.graphics.vector.ImageVector,
  isSelected: Boolean,
  activeColor: Color,
  onClick: () -> Unit,
  modifier: Modifier = Modifier
) {
  Box(
    modifier = modifier
      .heightIn(min = 54.dp)
      .clip(RoundedCornerShape(12.dp))
      .background(if (isSelected) activeColor.copy(alpha = 0.2f) else SpaceSurface)
      .border(1.dp, if (isSelected) activeColor else SpaceBorder, RoundedCornerShape(12.dp))
      .clickable { onClick() }
      .padding(vertical = 8.dp, horizontal = 4.dp),
    contentAlignment = Alignment.Center
  ) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
      Icon(
        imageVector = icon,
        contentDescription = title,
        tint = if (isSelected) activeColor else TextSecondary,
        modifier = Modifier.size(20.dp)
      )
      Spacer(modifier = Modifier.height(3.dp))
      Text(
        text = title,
        style = MaterialTheme.typography.labelSmall,
        color = if (isSelected) TextPrimary else TextSecondary,
        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
        fontSize = 10.sp
      )
    }
  }
}

@Composable
private fun SomaticSymptomCard(
  symptom: SomaticSymptom,
  onToggleReported: (Boolean) -> Unit,
  onEditDetails: () -> Unit
) {
  val isOpresion = symptom.name.contains("opresión", ignoreCase = true) || symptom.name.contains("torácica", ignoreCase = true)

  SpaceCard(
    borderColor = if (isOpresion && symptom.isReported) StatusSevereRed else if (symptom.isReported) CyanNeon.copy(alpha = 0.6f) else SpaceBorder,
    backgroundColor = if (symptom.isReported) SpaceSurfaceVariant else SpaceSurface
  ) {
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
        Checkbox(
          checked = symptom.isReported,
          onCheckedChange = { onToggleReported(it) },
          colors = CheckboxDefaults.colors(
            checkedColor = if (isOpresion) StatusSevereRed else CyanNeon,
            uncheckedColor = SpaceBorder,
            checkmarkColor = SpaceDeepNavy
          )
        )
        Text(
          text = symptom.name,
          style = MaterialTheme.typography.titleSmall,
          color = if (isOpresion) StatusSevereRed else TextPrimary,
          fontWeight = FontWeight.Bold
        )
      }

      if (symptom.isReported) {
        Surface(
          shape = RoundedCornerShape(8.dp),
          color = if (symptom.intensity >= 6f) StatusSevereRed.copy(alpha = 0.2f) else CyanNeon.copy(alpha = 0.2f),
          border = BorderStroke(1.dp, if (symptom.intensity >= 6f) StatusSevereRed else CyanNeon),
          modifier = Modifier
            .clickable { onEditDetails() }
            .testTag("edit_symptom_${symptom.id}")
        ) {
          Text(
            text = "${symptom.intensity.toInt()}/10 • Ajustar",
            style = MaterialTheme.typography.labelSmall,
            color = if (symptom.intensity >= 6f) StatusSevereRed else CyanNeon,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
          )
        }
      }
    }
  }
}

@Composable
private fun SymptomDetailDialog(
  symptom: SomaticSymptom,
  onDismiss: () -> Unit,
  onSave: (intensity: Float, freq: String, dur: String, ctx: String, trig: String) -> Unit
) {
  var intensity by remember { mutableFloatStateOf(symptom.intensity) }
  var frequency by remember { mutableStateOf(symptom.frequency) }
  var duration by remember { mutableStateOf(symptom.duration) }
  var context by remember { mutableStateOf(symptom.context) }
  var triggers by remember { mutableStateOf(symptom.possibleTrigger) }

  AlertDialog(
    onDismissRequest = onDismiss,
    title = {
      Text(
        text = symptom.name,
        style = MaterialTheme.typography.titleMedium,
        color = CyanNeon,
        fontWeight = FontWeight.Bold
      )
    },
    text = {
      Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
      ) {
        Text(
          text = "Intensidad: ${intensity.toInt()}/10",
          style = MaterialTheme.typography.bodySmall,
          color = TextPrimary,
          fontWeight = FontWeight.Bold
        )
        Slider(
          value = intensity,
          onValueChange = { intensity = it },
          valueRange = 0f..10f,
          steps = 9,
          colors = SliderDefaults.colors(thumbColor = CyanNeon, activeTrackColor = CyanNeon)
        )
        OutlinedTextField(
          value = frequency,
          onValueChange = { frequency = it },
          label = { Text("Frecuencia") },
          modifier = Modifier.fillMaxWidth(),
          textStyle = MaterialTheme.typography.bodySmall
        )
        OutlinedTextField(
          value = duration,
          onValueChange = { duration = it },
          label = { Text("Duración") },
          modifier = Modifier.fillMaxWidth(),
          textStyle = MaterialTheme.typography.bodySmall
        )
        OutlinedTextField(
          value = context,
          onValueChange = { context = it },
          label = { Text("Contexto") },
          modifier = Modifier.fillMaxWidth(),
          textStyle = MaterialTheme.typography.bodySmall
        )
        OutlinedTextField(
          value = triggers,
          onValueChange = { triggers = it },
          label = { Text("Desencadenante") },
          modifier = Modifier.fillMaxWidth(),
          textStyle = MaterialTheme.typography.bodySmall
        )
      }
    },
    confirmButton = {
      Button(
        onClick = { onSave(intensity, frequency, duration, context, triggers) },
        colors = ButtonDefaults.buttonColors(containerColor = CyanNeon, contentColor = SpaceDeepNavy)
      ) {
        Text("GUARDAR", fontWeight = FontWeight.Bold)
      }
    },
    dismissButton = {
      TextButton(onClick = onDismiss) {
        Text("CANCELAR", color = TextSecondary)
      }
    },
    containerColor = SpaceSurfaceCard
  )
}
