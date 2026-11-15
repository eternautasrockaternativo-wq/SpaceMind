package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.SessionLog
import com.example.model.SomaticCategory
import com.example.ui.AppScreen
import com.example.ui.SpaceMindViewModel
import com.example.ui.components.OperationalLevelBadge
import com.example.ui.components.SpaceCard
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryAnalyticsScreen(
  viewModel: SpaceMindViewModel,
  modifier: Modifier = Modifier
) {
  BackHandler {
    viewModel.navigateTo(AppScreen.DASHBOARD)
  }

  val sessionLogs by viewModel.sessionLogs.collectAsState()
  val baseline by viewModel.baseline.collectAsState()
  val anxiety by viewModel.currentAnxiety.collectAsState()
  val symptoms by viewModel.somaticSymptoms.collectAsState()
  val telemetry by viewModel.telemetry.collectAsState()
  var timeFilter by remember { mutableStateOf("7 Días") }

  Scaffold(
    topBar = {
      TopAppBar(
        title = {
          Column {
            Text(
              text = "SEGUIMIENTO TEMPORAL",
              style = MaterialTheme.typography.titleMedium,
              color = CyanNeon,
              fontWeight = FontWeight.Bold
            )
            Text(
              text = "Tendencias, Síntomas e Intervenciones",
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
          IconButton(onClick = { viewModel.setShowPrivacyDialog(true) }) {
            Icon(
              imageVector = Icons.Default.Lock,
              contentDescription = "Privacidad",
              tint = VioletSoft
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
      // Time Range Filter Bar
      item {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          listOf("24h", "7 Días", "30 Días", "Misión").forEach { filter ->
            val isSel = timeFilter == filter
            Surface(
              shape = RoundedCornerShape(10.dp),
              color = if (isSel) CyanNeon.copy(alpha = 0.2f) else SpaceSurface,
              border = BorderStroke(1.dp, if (isSel) CyanNeon else SpaceBorder),
              modifier = Modifier
                .weight(1f)
                .heightIn(min = 42.dp)
                .clickable { timeFilter = filter }
            ) {
              Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(vertical = 8.dp)) {
                Text(
                  text = filter,
                  style = MaterialTheme.typography.labelSmall,
                  color = if (isSel) CyanNeon else TextSecondary,
                  fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal,
                  fontSize = 11.sp
                )
              }
            }
          }
        }
      }

      // 1. Graphic: Anxiety Evolution (Pre vs Post)
      item {
        SpaceCard(
          borderColor = CyanNeon.copy(alpha = 0.5f),
          backgroundColor = SpaceSurfaceCard
        ) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Text(
              text = "EVOLUCIÓN ANSIEDAD ($timeFilter)",
              style = MaterialTheme.typography.labelMedium,
              color = CyanNeon,
              fontWeight = FontWeight.Bold
            )
            Text(
              text = "Basal: ${baseline.traitAnxiety}/10",
              style = MaterialTheme.typography.labelSmall,
              color = TealLight
            )
          }

          Spacer(modifier = Modifier.height(14.dp))

          val weeklyDays = listOf(
            Triple("D136", 2.2f, 2.0f),
            Triple("D137", 3.0f, 2.1f),
            Triple("D138", 6.0f, 2.8f),
            Triple("D139", 4.2f, 2.5f),
            Triple("D140", 7.5f, 3.2f),
            Triple("D141", 6.8f, 3.4f),
            Triple("Hoy", anxiety, (anxiety - 2.2f).coerceAtLeast(1.5f))
          )

          Row(
            modifier = Modifier
              .fillMaxWidth()
              .height(124.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom
          ) {
            weeklyDays.forEach { (day, pre, post) ->
              Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Bottom,
                modifier = Modifier.weight(1f)
              ) {
                Row(
                  horizontalArrangement = Arrangement.spacedBy(3.dp),
                  verticalAlignment = Alignment.Bottom
                ) {
                  Box(
                    modifier = Modifier
                      .width(11.dp)
                      .height((pre * 10).dp)
                      .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                      .background(if (pre > 6) StatusSevereRed else StatusModerateOrange)
                  )
                  Box(
                    modifier = Modifier
                      .width(11.dp)
                      .height((post * 10).dp)
                      .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                      .background(StatusStableGreen)
                  )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                  text = day,
                  style = MaterialTheme.typography.labelSmall,
                  color = if (day == "Hoy") CyanNeon else TextSecondary,
                  fontWeight = if (day == "Hoy") FontWeight.Bold else FontWeight.Normal,
                  fontSize = 9.sp
                )
              }
            }
          }

          Spacer(modifier = Modifier.height(8.dp))
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center
          ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Box(modifier = Modifier.size(8.dp).background(StatusModerateOrange))
              Spacer(modifier = Modifier.width(4.dp))
              Text(text = "Pre", style = MaterialTheme.typography.labelSmall, color = TextMuted, fontSize = 10.sp)
            }
            Spacer(modifier = Modifier.width(16.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
              Box(modifier = Modifier.size(8.dp).background(StatusStableGreen))
              Spacer(modifier = Modifier.width(4.dp))
              Text(text = "Post", style = MaterialTheme.typography.labelSmall, color = TextMuted, fontSize = 10.sp)
            }
          }
        }
      }

      // 2. Symptom Frequency by Somatic Dimension
      item {
        SpaceCard(
          borderColor = VioletNeon.copy(alpha = 0.4f),
          backgroundColor = SpaceSurface
        ) {
          Text(
            text = "SÍNTOMAS ACTIVOS",
            style = MaterialTheme.typography.labelMedium,
            color = VioletSoft,
            fontWeight = FontWeight.Bold
          )
          Spacer(modifier = Modifier.height(8.dp))

          val cardCount = symptoms.count { it.category == SomaticCategory.CARDIOVASCULAR && it.isReported }
          val respCount = symptoms.count { it.category == SomaticCategory.RESPIRATORY && it.isReported }
          val muscCount = symptoms.count { it.category == SomaticCategory.MUSCULAR_NEUROLOGICAL && it.isReported }
          val gastCount = symptoms.count { it.category == SomaticCategory.GASTROINTESTINAL_GENERAL && it.isReported }

          SymptomDimensionProgressBar("Cardiovascular", cardCount, 4, TelemetryHrColor)
          Spacer(modifier = Modifier.height(6.dp))
          SymptomDimensionProgressBar("Respiratorio", respCount, 4, TelemetryRespColor)
          Spacer(modifier = Modifier.height(6.dp))
          SymptomDimensionProgressBar("Muscular", muscCount, 5, VioletNeon)
          Spacer(modifier = Modifier.height(6.dp))
          SymptomDimensionProgressBar("General", gastCount, 5, TealLight)
        }
      }

      // 3. Physiological Snapshot
      item {
        SpaceCard(
          borderColor = TealLight.copy(alpha = 0.4f),
          backgroundColor = SpaceSurfaceCard
        ) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceAround
          ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
              Text(text = "HRV", style = MaterialTheme.typography.labelSmall, color = TextMuted)
              Text(
                text = "${telemetry.hrvRmssd} ms",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = TelemetryHrvColor
              )
            }
            Box(modifier = Modifier.width(1.dp).height(38.dp).background(SpaceBorder))
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
              Text(text = "Pulso", style = MaterialTheme.typography.labelSmall, color = TextMuted)
              Text(
                text = "${telemetry.heartRate} lpm",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = TelemetryHrColor
              )
            }
            Box(modifier = Modifier.width(1.dp).height(38.dp).background(SpaceBorder))
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
              Text(text = "Sueño", style = MaterialTheme.typography.labelSmall, color = TextMuted)
              Text(
                text = "87%",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = CyanNeon
              )
            }
          }
        }
      }

      // 4. Timeline of Interventions & Responses
      item {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Text(
            text = "REGISTRO DE INTERVENCIONES",
            style = MaterialTheme.typography.labelMedium,
            color = CyanNeon,
            fontWeight = FontWeight.Bold
          )
          Text(
            text = "${sessionLogs.size} total",
            style = MaterialTheme.typography.labelSmall,
            color = TextMuted
          )
        }
      }

      items(sessionLogs) { log ->
        TimelineSessionLogCard(log = log)
      }
    }
  }
}

@Composable
private fun SymptomDimensionProgressBar(
  categoryName: String,
  count: Int,
  total: Int,
  color: Color
) {
  Column {
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween
    ) {
      Text(text = categoryName, style = MaterialTheme.typography.bodySmall, color = TextPrimary)
      Text(text = "$count/$total", style = MaterialTheme.typography.labelSmall, color = color, fontWeight = FontWeight.Bold)
    }
    Spacer(modifier = Modifier.height(2.dp))
    LinearProgressIndicator(
      progress = { if (total > 0) count.toFloat() / total else 0f },
      modifier = Modifier
        .fillMaxWidth()
        .height(6.dp)
        .clip(RoundedCornerShape(3.dp)),
      color = color,
      trackColor = SpaceSurfaceCard
    )
  }
}

@Composable
private fun TimelineSessionLogCard(log: SessionLog) {
  SpaceCard(
    borderColor = SpaceBorder,
    backgroundColor = SpaceSurfaceCard
  ) {
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
        Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(CyanNeon))
        Spacer(modifier = Modifier.width(8.dp))
        Text(
          text = log.moduleName,
          style = MaterialTheme.typography.labelMedium,
          fontWeight = FontWeight.Bold,
          color = CyanNeon
        )
      }
      Text(
        text = log.timestamp,
        style = MaterialTheme.typography.labelSmall,
        color = TextMuted
      )
    }

    Spacer(modifier = Modifier.height(4.dp))
    Text(
      text = log.description,
      style = MaterialTheme.typography.bodySmall,
      color = TextSecondary,
      maxLines = 2
    )

    Spacer(modifier = Modifier.height(6.dp))
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      val delta = log.preValue - log.postValue
      Text(
        text = "${log.preValue.toInt()}/10 → ${log.postValue.toInt()}/10 (Alivio -${delta.toInt()})",
        style = MaterialTheme.typography.labelSmall,
        color = TealLight,
        fontWeight = FontWeight.Bold
      )
      OperationalLevelBadge(level = log.statusLevel)
    }
  }
}
