package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.CognitiveDistortion
import com.example.model.ThoughtRecord
import com.example.ui.AppScreen
import com.example.ui.SpaceMindViewModel
import com.example.ui.components.ExpandableClinicalInfoCard
import com.example.ui.components.SpaceCard
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Level3CognitiveScreen(
  viewModel: SpaceMindViewModel,
  modifier: Modifier = Modifier
) {
  BackHandler {
    viewModel.navigateTo(AppScreen.DASHBOARD)
  }

  val thoughtRecords by viewModel.thoughtRecords.collectAsState()
  val distortions = viewModel.cognitiveDistortions
  var showNewRecordSheet by remember { mutableStateOf(false) }

  Scaffold(
    topBar = {
      TopAppBar(
        title = {
          Column {
            Text(
              text = "NIVEL 3: COGNITIVO Y TCC",
              style = MaterialTheme.typography.titleMedium,
              color = CyanNeon,
              fontWeight = FontWeight.Bold
            )
            Text(
              text = "Reestructuración y Distorsiones",
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
    floatingActionButton = {
      ExtendedFloatingActionButton(
        onClick = { showNewRecordSheet = true },
        containerColor = CyanNeon,
        contentColor = SpaceDeepNavy,
        icon = { Icon(Icons.Default.Add, contentDescription = null) },
        text = { Text("NUEVO REGISTRO", fontWeight = FontWeight.Bold) },
        modifier = Modifier.testTag("fab_new_thought_record")
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
      contentPadding = PaddingValues(top = 8.dp, bottom = 84.dp)
    ) {
      item {
        Text(
          text = "DISTORSIONES FRECUENTES (TOCA PARA VER ESTRATEGIA)",
          style = MaterialTheme.typography.labelSmall,
          color = CyanNeon,
          fontWeight = FontWeight.Bold
        )
      }

      items(distortions) { dist ->
        CognitiveDistortionCard(distortion = dist)
      }

      item {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Text(
            text = "REGISTROS TCC",
            style = MaterialTheme.typography.labelMedium,
            color = TealLight,
            fontWeight = FontWeight.Bold
          )
          Text(
            text = "${thoughtRecords.size} guardados",
            style = MaterialTheme.typography.labelSmall,
            color = TextMuted
          )
        }
      }

      items(thoughtRecords) { record ->
        ThoughtRecordCard(record = record)
      }

      item {
        ExpandableClinicalInfoCard(
          title = "Base clínica TCC en órbita",
          content = "En TCC examinamos los pensamientos anticipatorios como hipótesis frente a datos objetivos de telemetría y entrenamiento, reduciendo la activación emocional."
        )
      }
    }
  }

  if (showNewRecordSheet) {
    NewThoughtRecordDialog(
      distortions = distortions,
      onDismiss = { showNewRecordSheet = false },
      onSave = { sit, thought, dist, evFor, evAg, alt, pre, post ->
        viewModel.addThoughtRecord(sit, thought, dist, evFor, evAg, alt, pre, post)
        showNewRecordSheet = false
      }
    )
  }
}

@Composable
private fun CognitiveDistortionCard(
  distortion: CognitiveDistortion
) {
  var isExpanded by remember { mutableStateOf(false) }

  SpaceCard(
    borderColor = VioletNeon.copy(alpha = 0.35f),
    backgroundColor = SpaceSurface,
    onClick = { isExpanded = !isExpanded }
  ) {
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
        Box(
          modifier = Modifier
            .size(8.dp)
            .clip(RoundedCornerShape(4.dp))
            .background(VioletNeon)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
          text = distortion.name,
          style = MaterialTheme.typography.titleSmall,
          color = TextPrimary,
          fontWeight = FontWeight.Bold
        )
      }
      Icon(
        imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
        contentDescription = null,
        tint = VioletSoft
      )
    }

    if (isExpanded) {
      Spacer(modifier = Modifier.height(8.dp))
      Text(
        text = distortion.definition,
        style = MaterialTheme.typography.bodySmall,
        color = TextSecondary
      )
      Spacer(modifier = Modifier.height(8.dp))
      Box(
        modifier = Modifier
          .fillMaxWidth()
          .clip(RoundedCornerShape(8.dp))
          .background(SpaceSurfaceCard)
          .border(1.dp, SpaceBorder, RoundedCornerShape(8.dp))
          .padding(10.dp)
      ) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
          Text(
            text = "Ejemplo: ${distortion.spaceExample}",
            style = MaterialTheme.typography.bodySmall,
            color = StatusMildYellow,
            fontSize = 11.sp
          )
          Text(
            text = "Refutación: ${distortion.clinicalCounterStrategy}",
            style = MaterialTheme.typography.bodySmall,
            color = TealLight,
            fontSize = 11.sp
          )
        }
      }
    }
  }
}

@Composable
private fun ThoughtRecordCard(
  record: ThoughtRecord
) {
  SpaceCard(
    borderColor = TealLight.copy(alpha = 0.4f),
    backgroundColor = SpaceSurfaceCard
  ) {
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      Text(
        text = record.timestamp,
        style = MaterialTheme.typography.labelSmall,
        color = TextMuted
      )
      Surface(
        shape = RoundedCornerShape(10.dp),
        color = VioletNeon.copy(alpha = 0.2f),
        border = BorderStroke(1.dp, VioletNeon.copy(alpha = 0.5f))
      ) {
        Text(
          text = record.distortion,
          style = MaterialTheme.typography.labelSmall,
          color = VioletSoft,
          fontWeight = FontWeight.Bold,
          modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
          fontSize = 10.sp
        )
      }
    }

    Spacer(modifier = Modifier.height(6.dp))
    Text(
      text = record.automaticThought,
      style = MaterialTheme.typography.bodySmall,
      color = TextPrimary,
      maxLines = 2
    )

    Spacer(modifier = Modifier.height(6.dp))
    Box(
      modifier = Modifier
        .fillMaxWidth()
        .clip(RoundedCornerShape(8.dp))
        .background(SpaceSurface)
        .padding(8.dp)
    ) {
      Text(
        text = record.alternativeThought,
        style = MaterialTheme.typography.bodySmall,
        color = StatusStableGreen,
        fontSize = 12.sp
      )
    }

    Spacer(modifier = Modifier.height(6.dp))
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween
    ) {
      Text(
        text = "${record.preAnxiety.toInt()}/10 → ${record.postAnxiety.toInt()}/10",
        style = MaterialTheme.typography.labelSmall,
        color = CyanNeon,
        fontWeight = FontWeight.Bold
      )
      Text(
        text = "Alivio -${(record.preAnxiety - record.postAnxiety).toInt()} pts",
        style = MaterialTheme.typography.labelSmall,
        color = StatusStableGreen,
        fontWeight = FontWeight.Bold
      )
    }
  }
}

@Composable
private fun NewThoughtRecordDialog(
  distortions: List<CognitiveDistortion>,
  onDismiss: () -> Unit,
  onSave: (sit: String, th: String, dist: String, evFor: String, evAg: String, alt: String, pre: Float, post: Float) -> Unit
) {
  var situation by remember { mutableStateOf("") }
  var automaticThought by remember { mutableStateOf("") }
  val selectedDistortion = distortions.firstOrNull()?.name ?: "Pensamiento Catastrófico"
  var alternativeThought by remember { mutableStateOf("") }
  var preAnxiety by remember { mutableFloatStateOf(7f) }
  var postAnxiety by remember { mutableFloatStateOf(3f) }

  AlertDialog(
    onDismissRequest = onDismiss,
    title = {
      Text(
        text = "Nuevo Registro TCC",
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
        OutlinedTextField(
          value = situation,
          onValueChange = { situation = it },
          label = { Text("Situación breve") },
          modifier = Modifier.fillMaxWidth(),
          textStyle = MaterialTheme.typography.bodySmall
        )
        OutlinedTextField(
          value = automaticThought,
          onValueChange = { automaticThought = it },
          label = { Text("Pensamiento automático") },
          modifier = Modifier.fillMaxWidth(),
          textStyle = MaterialTheme.typography.bodySmall
        )
        OutlinedTextField(
          value = alternativeThought,
          onValueChange = { alternativeThought = it },
          label = { Text("Pensamiento alternativo objetivo") },
          modifier = Modifier.fillMaxWidth(),
          textStyle = MaterialTheme.typography.bodySmall
        )
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween
        ) {
          Text(text = "Antes: ${preAnxiety.toInt()}/10", style = MaterialTheme.typography.labelSmall, color = StatusSevereRed)
          Text(text = "Después: ${postAnxiety.toInt()}/10", style = MaterialTheme.typography.labelSmall, color = StatusStableGreen)
        }
        Slider(
          value = postAnxiety,
          onValueChange = { postAnxiety = it },
          valueRange = 0f..10f,
          steps = 9,
          colors = SliderDefaults.colors(thumbColor = StatusStableGreen, activeTrackColor = StatusStableGreen)
        )
      }
    },
    confirmButton = {
      Button(
        onClick = {
          if (automaticThought.isNotBlank() && alternativeThought.isNotBlank()) {
            onSave(situation, automaticThought, selectedDistortion, "", "", alternativeThought, preAnxiety, postAnxiety)
          }
        },
        colors = ButtonDefaults.buttonColors(containerColor = CyanNeon, contentColor = SpaceDeepNavy),
        enabled = automaticThought.isNotBlank() && alternativeThought.isNotBlank()
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
