package com.example.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.OperationalLevel
import com.example.model.PhysiologicalTelemetry
import com.example.ui.theme.*

@Composable
fun SpaceCard(
  modifier: Modifier = Modifier,
  borderColor: Color = SpaceBorder,
  backgroundColor: Color = SpaceSurfaceCard,
  onClick: (() -> Unit)? = null,
  content: @Composable ColumnScope.() -> Unit
) {
  Card(
    modifier = modifier
      .fillMaxWidth()
      .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier),
    shape = RoundedCornerShape(16.dp),
    colors = CardDefaults.cardColors(containerColor = backgroundColor),
    border = BorderStroke(1.dp, borderColor)
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(16.dp),
      content = content
    )
  }
}

@Composable
fun OperationalLevelBadge(
  level: OperationalLevel,
  modifier: Modifier = Modifier
) {
  val (bgColor, textColor, icon) = when (level) {
    OperationalLevel.ESTABLE -> Triple(StatusStableGreen.copy(alpha = 0.2f), StatusStableGreen, Icons.Default.CheckCircle)
    OperationalLevel.ACTIVACION_LEVE -> Triple(StatusMildYellow.copy(alpha = 0.2f), StatusMildYellow, Icons.Default.Info)
    OperationalLevel.ACTIVACION_MODERADA -> Triple(StatusModerateOrange.copy(alpha = 0.2f), StatusModerateOrange, Icons.Default.Warning)
    OperationalLevel.ACTIVACION_ELEVADA -> Triple(StatusSevereRed.copy(alpha = 0.2f), StatusSevereRed, Icons.Default.ReportProblem)
  }

  Surface(
    modifier = modifier.clip(RoundedCornerShape(20.dp)),
    color = bgColor,
    border = BorderStroke(1.dp, textColor.copy(alpha = 0.6f))
  ) {
    Row(
      modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
      verticalAlignment = Alignment.CenterVertically
    ) {
      Icon(
        imageVector = icon,
        contentDescription = null,
        tint = textColor,
        modifier = Modifier.size(14.dp)
      )
      Spacer(modifier = Modifier.width(6.dp))
      Text(
        text = level.label.uppercase(),
        style = MaterialTheme.typography.labelMedium,
        color = textColor,
        fontWeight = FontWeight.Bold,
        fontSize = 11.sp
      )
    }
  }
}

@Composable
fun TelemetryStrip(
  telemetry: PhysiologicalTelemetry,
  modifier: Modifier = Modifier
) {
  SpaceCard(
    modifier = modifier,
    borderColor = CyanNeon.copy(alpha = 0.3f),
    backgroundColor = SpaceSurface
  ) {
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
          imageVector = Icons.Default.Sensors,
          contentDescription = null,
          tint = CyanNeon,
          modifier = Modifier.size(18.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
          text = "TELEMETRÍA FISIOLÓGICA ACTIVA",
          style = MaterialTheme.typography.labelMedium,
          color = CyanNeon,
          fontWeight = FontWeight.Bold
        )
      }
      Text(
        text = telemetry.timestamp,
        style = MaterialTheme.typography.labelSmall,
        color = TextSecondary
      )
    }

    Spacer(modifier = Modifier.height(12.dp))

    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween
    ) {
      TelemetryMetricItem(
        icon = Icons.Default.Favorite,
        label = "FC (Ritmo)",
        value = "${telemetry.heartRate}",
        unit = "lpm",
        color = TelemetryHrColor
      )
      TelemetryMetricItem(
        icon = Icons.Default.Timeline,
        label = "HRV (RMSSD)",
        value = "${telemetry.hrvRmssd}",
        unit = "ms",
        color = TelemetryHrvColor
      )
      TelemetryMetricItem(
        icon = Icons.Default.Air,
        label = "Respiración",
        value = "${telemetry.respRate}",
        unit = "rpm",
        color = TelemetryRespColor
      )
      TelemetryMetricItem(
        icon = Icons.Default.Thermostat,
        label = "Temp Cutánea",
        value = "%.1f".format(telemetry.skinTemp),
        unit = "°C",
        color = TelemetryTempColor
      )
      TelemetryMetricItem(
        icon = Icons.Default.Bloodtype,
        label = "SpO2",
        value = "${telemetry.spO2}",
        unit = "%",
        color = TelemetrySpo2Color
      )
    }
  }
}

@Composable
private fun TelemetryMetricItem(
  icon: ImageVector,
  label: String,
  value: String,
  unit: String,
  color: Color
) {
  Column(
    horizontalAlignment = Alignment.CenterHorizontally,
    modifier = Modifier.padding(horizontal = 2.dp)
  ) {
    Icon(
      imageVector = icon,
      contentDescription = null,
      tint = color,
      modifier = Modifier.size(16.dp)
    )
    Spacer(modifier = Modifier.height(4.dp))
    Row(verticalAlignment = Alignment.Bottom) {
      Text(
        text = value,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        color = TextPrimary
      )
      Text(
        text = " $unit",
        style = MaterialTheme.typography.labelSmall,
        color = TextMuted,
        fontSize = 9.sp
      )
    }
    Text(
      text = label,
      style = MaterialTheme.typography.labelSmall,
      color = TextSecondary,
      fontSize = 9.sp
    )
  }
}

@Composable
fun InteractiveBreathingVisualizer(
  isRunning: Boolean,
  modifier: Modifier = Modifier
) {
  val infiniteTransition = rememberInfiniteTransition(label = "breathing")

  // 4s inhale, 4s hold, 8s exhale = 16s total cycle
  val scale by infiniteTransition.animateFloat(
    initialValue = 0.55f,
    targetValue = 1.0f,
    animationSpec = infiniteRepeatable(
      animation = keyframes {
        durationMillis = 16000
        0.55f at 0 using FastOutSlowInEasing // start
        1.0f at 4000 using LinearEasing      // inhaled (4s)
        1.0f at 8000 using FastOutSlowInEasing // hold (4s)
        0.55f at 16000 using FastOutSlowInEasing // exhale (8s)
      },
      repeatMode = RepeatMode.Restart
    ),
    label = "breathingScale"
  )

  val phaseText = remember(scale, isRunning) {
    if (!isRunning) "Pausa / Listo para Iniciar"
    else when {
      scale < 0.95f && scale > 0.6f -> "Inhalando (4s)..."
      scale >= 0.95f -> "Sostén suavemente (4s)"
      else -> "Exhala relajadamente (8s)..."
    }
  }

  Column(
    modifier = modifier.fillMaxWidth(),
    horizontalAlignment = Alignment.CenterHorizontally
  ) {
    Box(
      modifier = Modifier
        .size(190.dp)
        .padding(16.dp),
      contentAlignment = Alignment.Center
    ) {
      // Outer glow ring
      Box(
        modifier = Modifier
          .size(180.dp * (if (isRunning) scale else 0.75f))
          .clip(CircleShape)
          .background(
            Brush.radialGradient(
              listOf(CyanNeon.copy(alpha = 0.35f), VioletDeep.copy(alpha = 0.15f), Color.Transparent)
            )
          )
      )
      // Intermediate ring
      Box(
        modifier = Modifier
          .size(130.dp * (if (isRunning) scale else 0.75f))
          .border(2.dp, CyanNeon.copy(alpha = 0.7f), CircleShape)
      )
      // Core circle
      Box(
        modifier = Modifier
          .size(75.dp)
          .clip(CircleShape)
          .background(Brush.linearGradient(listOf(CyanNeon, VioletNeon))),
        contentAlignment = Alignment.Center
      ) {
        Icon(
          imageVector = Icons.Default.Air,
          contentDescription = null,
          tint = SpaceDeepNavy,
          modifier = Modifier.size(32.dp)
        )
      }
    }
    Text(
      text = phaseText,
      style = MaterialTheme.typography.titleMedium,
      color = CyanNeon,
      fontWeight = FontWeight.Bold
    )
    Text(
      text = "Ritmo Vagal Aeroespacial (4-4-8)",
      style = MaterialTheme.typography.labelSmall,
      color = TextSecondary
    )
  }
}

@Composable
fun SpaceStarfieldBackground(
  modifier: Modifier = Modifier,
  content: @Composable () -> Unit
) {
  val infiniteTransition = rememberInfiniteTransition(label = "stars")
  val starPulse by infiniteTransition.animateFloat(
    initialValue = 0.4f,
    targetValue = 0.9f,
    animationSpec = infiniteRepeatable(
      animation = tween(3500, easing = LinearEasing),
      repeatMode = RepeatMode.Reverse
    ),
    label = "starPulse"
  )

  Box(modifier = modifier) {
    androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxSize()) {
      val w = size.width
      val h = size.height

      // Subtle orbital grid rings
      drawCircle(
        color = CyanNeon.copy(alpha = 0.04f),
        radius = w * 0.7f,
        center = androidx.compose.ui.geometry.Offset(w * 0.5f, h * 0.2f),
        style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1f)
      )
      drawCircle(
        color = VioletNeon.copy(alpha = 0.03f),
        radius = w * 1.1f,
        center = androidx.compose.ui.geometry.Offset(w * 0.5f, h * 0.2f),
        style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1f)
      )

      // Fixed constellation stars
      val starPositions = listOf(
        Pair(0.12f, 0.08f) to 1.8f,
        Pair(0.85f, 0.12f) to 2.2f,
        Pair(0.25f, 0.22f) to 1.5f,
        Pair(0.72f, 0.28f) to 2.0f,
        Pair(0.08f, 0.45f) to 1.6f,
        Pair(0.92f, 0.48f) to 2.4f,
        Pair(0.35f, 0.62f) to 1.4f,
        Pair(0.68f, 0.70f) to 2.0f,
        Pair(0.18f, 0.82f) to 1.8f,
        Pair(0.82f, 0.88f) to 2.5f,
        Pair(0.50f, 0.95f) to 1.5f,
        Pair(0.40f, 0.15f) to 1.2f,
        Pair(0.60f, 0.38f) to 1.7f
      )

      starPositions.forEachIndexed { i, (pos, rad) ->
        val alphaMod = if (i % 2 == 0) starPulse else (1.3f - starPulse).coerceIn(0.2f, 1f)
        val color = if (i % 3 == 0) CyanNeon else if (i % 3 == 1) VioletSoft else Color.White
        drawCircle(
          color = color.copy(alpha = 0.25f * alphaMod),
          radius = rad * 1.8f,
          center = androidx.compose.ui.geometry.Offset(w * pos.first, h * pos.second)
        )
        drawCircle(
          color = color.copy(alpha = 0.7f * alphaMod),
          radius = rad,
          center = androidx.compose.ui.geometry.Offset(w * pos.first, h * pos.second)
        )
      }
    }
    content()
  }
}

@Composable
fun SemicircularActivationGauge(
  score: Int,
  level: OperationalLevel,
  modifier: Modifier = Modifier
) {
  val levelColor = when (level) {
    OperationalLevel.ESTABLE -> StatusStableGreen
    OperationalLevel.ACTIVACION_LEVE -> StatusMildYellow
    OperationalLevel.ACTIVACION_MODERADA -> StatusModerateOrange
    OperationalLevel.ACTIVACION_ELEVADA -> StatusSevereRed
  }

  Column(
    modifier = modifier.fillMaxWidth(),
    horizontalAlignment = Alignment.CenterHorizontally
  ) {
    Box(
      modifier = Modifier
        .fillMaxWidth()
        .height(130.dp),
      contentAlignment = Alignment.Center
    ) {
      androidx.compose.foundation.Canvas(
        modifier = Modifier
          .fillMaxWidth(0.85f)
          .height(125.dp)
      ) {
        val strokeWidth = 14.dp.toPx()
        val arcSize = androidx.compose.ui.geometry.Size(size.width, size.height * 1.9f)
        val topLeft = androidx.compose.ui.geometry.Offset(0f, 0f)

        // Background track (180 to 0 degrees clockwise)
        drawArc(
          color = SpaceBorder.copy(alpha = 0.4f),
          startAngle = 180f,
          sweepAngle = 180f,
          useCenter = false,
          topLeft = topLeft,
          size = arcSize,
          style = androidx.compose.ui.graphics.drawscope.Stroke(
            width = strokeWidth,
            cap = androidx.compose.ui.graphics.StrokeCap.Round
          )
        )

        // Progressive 4 segment ticks/markers
        val progressSweep = (score / 100f).coerceIn(0.03f, 1f) * 180f

        // Active glowing sweep
        drawArc(
          brush = Brush.horizontalGradient(
            listOf(
              StatusStableGreen,
              if (score > 25) StatusMildYellow else StatusStableGreen,
              if (score > 50) StatusModerateOrange else StatusMildYellow,
              levelColor
            )
          ),
          startAngle = 180f,
          sweepAngle = progressSweep,
          useCenter = false,
          topLeft = topLeft,
          size = arcSize,
          style = androidx.compose.ui.graphics.drawscope.Stroke(
            width = strokeWidth,
            cap = androidx.compose.ui.graphics.StrokeCap.Round
          )
        )
      }

      // Center Gauge Metrics
      Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.padding(top = 18.dp)
      ) {
        Row(verticalAlignment = Alignment.Bottom) {
          Text(
            text = "$score",
            style = MaterialTheme.typography.displaySmall,
            fontWeight = FontWeight.ExtraBold,
            color = levelColor
          )
          Text(
            text = " / 100",
            style = MaterialTheme.typography.labelSmall,
            color = TextSecondary,
            modifier = Modifier.padding(bottom = 6.dp)
          )
        }
        Text(
          text = "ÍNDICE DE ACTIVACIÓN",
          style = MaterialTheme.typography.labelSmall,
          color = CyanNeon,
          fontWeight = FontWeight.Bold,
          letterSpacing = 1.sp,
          fontSize = 9.sp
        )
        Spacer(modifier = Modifier.height(2.dp))
        OperationalLevelBadge(level = level)
      }
    }
  }
}

@Composable
fun VoiceWaveformVisualizer(
  isSpeaking: Boolean,
  isListening: Boolean,
  isProcessing: Boolean,
  rmsLevel: Float,
  modifier: Modifier = Modifier,
  accentColor: Color = CyanNeon
) {
  val barCount = 18
  val infiniteTransition = rememberInfiniteTransition(label = "voiceWave")
  val phase by infiniteTransition.animateFloat(
    initialValue = 0f,
    targetValue = (Math.PI * 2).toFloat(),
    animationSpec = infiniteRepeatable(
      animation = tween(1200, easing = LinearEasing),
      repeatMode = RepeatMode.Restart
    ),
    label = "wavePhase"
  )

  val activeColor = when {
    isListening -> TealLight
    isProcessing -> VioletNeon
    isSpeaking -> accentColor
    else -> SpaceBorder
  }

  Row(
    modifier = modifier
      .fillMaxWidth()
      .height(52.dp),
    horizontalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterHorizontally),
    verticalAlignment = Alignment.CenterVertically
  ) {
    for (i in 0 until barCount) {
      val centerWeight = 1f - (kotlin.math.abs(i - barCount / 2f) / (barCount / 2f)) * 0.55f
      val wave = (kotlin.math.sin(phase + i * 0.55f) + 1f) / 2f
      val heightFraction = when {
        isListening -> (0.18f + (rmsLevel.coerceIn(0.1f, 1f) * 0.72f * wave * centerWeight)).coerceIn(0.14f, 1f)
        isSpeaking -> (0.22f + 0.65f * wave * centerWeight).coerceIn(0.16f, 1f)
        isProcessing -> (0.2f + 0.45f * wave).coerceIn(0.16f, 0.75f)
        else -> 0.14f + 0.12f * centerWeight
      }

      Box(
        modifier = Modifier
          .width(4.dp)
          .fillMaxHeight(heightFraction)
          .clip(RoundedCornerShape(4.dp))
          .background(
            Brush.verticalGradient(
              listOf(activeColor, activeColor.copy(alpha = 0.45f))
            )
          )
      )
    }
  }
}

@Composable
fun SpaceMindVoiceAiCard(
  selectedLanguageCode: String,
  onSelectLanguage: (String) -> Unit,
  onStartVoiceEvaluation: () -> Unit,
  modifier: Modifier = Modifier
) {
  SpaceCard(
    modifier = modifier,
    borderColor = CyanNeon.copy(alpha = 0.7f),
    backgroundColor = SpaceSurfaceVariant
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
            .clip(CircleShape)
            .background(Brush.linearGradient(listOf(CyanNeon.copy(alpha = 0.25f), VioletNeon.copy(alpha = 0.25f))))
            .border(1.5.dp, CyanNeon, CircleShape),
          contentAlignment = Alignment.Center
        ) {
          Icon(
            imageVector = Icons.Default.GraphicEq,
            contentDescription = "SPACE MIND VOICE AI",
            tint = CyanNeon,
            modifier = Modifier.size(24.dp)
          )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column {
          Text(
            text = "SPACE MIND VOICE AI",
            style = MaterialTheme.typography.titleMedium,
            color = Color.White,
            fontWeight = FontWeight.ExtraBold,
            letterSpacing = 1.sp
          )
          Text(
            text = "Asistente de evaluación psicológica por voz",
            style = MaterialTheme.typography.bodySmall,
            color = CyanGlow,
            fontSize = 11.sp
          )
        }
      }

      // ES / EN quick switch
      Surface(
        shape = RoundedCornerShape(10.dp),
        color = SpaceSurface,
        border = BorderStroke(1.dp, SpaceBorder)
      ) {
        Row(modifier = Modifier.padding(2.dp)) {
          listOf("ES", "EN").forEach { lang ->
            val isSel = selectedLanguageCode.equals(lang, ignoreCase = true)
            Box(
              modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .background(if (isSel) CyanNeon else Color.Transparent)
                .clickable { onSelectLanguage(lang) }
                .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
              Text(
                text = lang,
                style = MaterialTheme.typography.labelSmall,
                color = if (isSel) SpaceDeepNavy else TextSecondary,
                fontWeight = FontWeight.Bold,
                fontSize = 10.sp
              )
            }
          }
        }
      }
    }

    Spacer(modifier = Modifier.height(12.dp))

    VoiceWaveformVisualizer(
      isSpeaking = true,
      isListening = false,
      isProcessing = false,
      rmsLevel = 0.35f,
      accentColor = CyanNeon
    )

    Spacer(modifier = Modifier.height(12.dp))

    Button(
      onClick = onStartVoiceEvaluation,
      modifier = Modifier
        .fillMaxWidth()
        .heightIn(min = 52.dp)
        .testTag("start_voice_ai_button"),
      colors = ButtonDefaults.buttonColors(
        containerColor = CyanNeon,
        contentColor = SpaceDeepNavy
      ),
      shape = RoundedCornerShape(14.dp)
    ) {
      Icon(
        imageVector = Icons.Default.Mic,
        contentDescription = null,
        modifier = Modifier.size(20.dp)
      )
      Spacer(modifier = Modifier.width(8.dp))
      Text(
        text = "INICIAR EVALUACIÓN POR VOZ",
        fontWeight = FontWeight.ExtraBold,
        fontSize = 13.sp,
        letterSpacing = 0.6.sp
      )
    }

    Spacer(modifier = Modifier.height(8.dp))

    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.Center,
      verticalAlignment = Alignment.CenterVertically
    ) {
      Icon(
        imageVector = Icons.Default.VerifiedUser,
        contentDescription = null,
        tint = TealLight,
        modifier = Modifier.size(13.dp)
      )
      Spacer(modifier = Modifier.width(6.dp))
      Text(
        text = "Micrófono bajo demanda • Respuesta rápida por voz o toque",
        style = MaterialTheme.typography.labelSmall,
        color = TextSecondary,
        fontSize = 10.sp
      )
    }
  }
}

@Composable
fun ExpandableClinicalInfoCard(
  title: String,
  content: String,
  accentColor: Color = CyanNeon,
  modifier: Modifier = Modifier
) {
  var expanded by remember { mutableStateOf(false) }

  SpaceCard(
    modifier = modifier,
    borderColor = accentColor.copy(alpha = 0.35f),
    backgroundColor = SpaceSurface,
    onClick = { expanded = !expanded }
  ) {
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
          imageVector = Icons.Default.Info,
          contentDescription = null,
          tint = accentColor,
          modifier = Modifier.size(18.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
          text = title,
          style = MaterialTheme.typography.labelMedium,
          color = accentColor,
          fontWeight = FontWeight.Bold
        )
      }
      Icon(
        imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
        contentDescription = if (expanded) "Contraer" else "Ver detalle",
        tint = accentColor
      )
    }
    if (expanded) {
      Spacer(modifier = Modifier.height(8.dp))
      Text(
        text = content,
        style = MaterialTheme.typography.bodySmall,
        color = TextSecondary
      )
    }
  }
}

