package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.model.OperationalLevel
import com.example.ui.AppScreen
import com.example.ui.SpaceMindViewModel
import com.example.ui.components.*
import com.example.ui.theme.*
import com.example.voice.VoiceLanguage

@Composable
fun DashboardScreen(
  viewModel: SpaceMindViewModel,
  modifier: Modifier = Modifier
) {
  val profile by viewModel.astronautProfile.collectAsState()
  val telemetry by viewModel.telemetry.collectAsState()
  val anxiety by viewModel.currentAnxiety.collectAsState()
  val sleepQuality by viewModel.currentSleepQuality.collectAsState()
  val baseline by viewModel.baseline.collectAsState()
  val sessionLogs by viewModel.sessionLogs.collectAsState()
  val interventions by viewModel.interventions.collectAsState()
  val voiceEvalState by viewModel.voiceEvalState.collectAsState()

  val operationalState = remember(anxiety, sleepQuality, sessionLogs) {
    viewModel.getOperationalClassification()
  }

  val recommendedProg = remember(operationalState, interventions) {
    val recId = operationalState.recommendedInterventions.firstOrNull() ?: "INT-01"
    interventions.find { it.id == recId } ?: interventions.first()
  }

  SpaceStarfieldBackground(modifier = modifier.fillMaxSize()) {
    LazyColumn(
      modifier = Modifier
        .fillMaxSize()
        .padding(horizontal = 16.dp),
      verticalArrangement = Arrangement.spacedBy(14.dp),
      contentPadding = PaddingValues(top = 12.dp, bottom = 28.dp)
    ) {
      // 1. HERO: SPACE MIND + Astronaut & Earth + Psychological State Indicator
      item {
        val levelColor = when (operationalState.level) {
          OperationalLevel.ESTABLE -> StatusStableGreen
          OperationalLevel.ACTIVACION_LEVE -> StatusMildYellow
          OperationalLevel.ACTIVACION_MODERADA -> StatusModerateOrange
          OperationalLevel.ACTIVACION_ELEVADA -> StatusSevereRed
        }

        Box(
          modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(SpaceSurfaceCard)
            .border(
              BorderStroke(
                1.dp,
                Brush.verticalGradient(
                  listOf(
                    CyanNeon.copy(alpha = 0.65f),
                    VioletNeon.copy(alpha = 0.35f),
                    SpaceBorder
                  )
                )
              ),
              RoundedCornerShape(24.dp)
            )
        ) {
          Column {
            Box(
              modifier = Modifier
                .fillMaxWidth()
                .height(175.dp)
            ) {
              Image(
                painter = painterResource(id = R.drawable.astronaut_earth),
                contentDescription = "Astronauta flotando en el espacio con la Tierra de fondo",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
              )
              Box(
                modifier = Modifier
                  .fillMaxSize()
                  .background(
                    Brush.verticalGradient(
                      listOf(
                        Color.Transparent,
                        SpaceDeepNavy.copy(alpha = 0.35f),
                        SpaceSurfaceCard.copy(alpha = 0.95f),
                        SpaceSurfaceCard
                      )
                    )
                  )
              )

              Row(
                modifier = Modifier
                  .fillMaxWidth()
                  .padding(12.dp),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
              ) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                  IconButton(
                    onClick = { viewModel.setShowPrivacyDialog(true) },
                    modifier = Modifier
                      .size(40.dp)
                      .clip(CircleShape)
                      .background(SpaceDeepNavy.copy(alpha = 0.85f))
                      .border(1.dp, VioletNeon.copy(alpha = 0.55f), CircleShape)
                      .testTag("privacy_protocol_button")
                  ) {
                    Icon(
                      imageVector = Icons.Default.Lock,
                      contentDescription = "Privacidad Médica",
                      tint = VioletSoft,
                      modifier = Modifier.size(18.dp)
                    )
                  }

                  IconButton(
                    onClick = { viewModel.setShowMedicalProtocol(true) },
                    modifier = Modifier
                      .size(40.dp)
                      .clip(CircleShape)
                      .background(SpaceDeepNavy.copy(alpha = 0.85f))
                      .border(1.dp, StatusSevereRed.copy(alpha = 0.65f), CircleShape)
                      .testTag("medical_protocol_button")
                  ) {
                    Icon(
                      imageVector = Icons.Default.MedicalServices,
                      contentDescription = "Protocolo de Emergencia Médica",
                      tint = StatusSevereRed,
                      modifier = Modifier.size(18.dp)
                    )
                  }
                }
              }
            }

            Column(
              modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 12.dp),
              horizontalAlignment = Alignment.CenterHorizontally
            ) {
              Text(
                text = "SPACE MIND",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.ExtraBold,
                color = Color.White,
                letterSpacing = 3.sp,
                fontSize = 30.sp
              )
              Text(
                text = "ESTADO PSICOLÓGICO",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = CyanNeon,
                letterSpacing = 2.sp,
                fontSize = 13.sp
              )
              Text(
                text = "Panel de seguimiento del astronauta",
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary,
                fontSize = 12.sp
              )

              Spacer(modifier = Modifier.height(12.dp))

              Box(
                modifier = Modifier
                  .fillMaxWidth()
                  .clip(RoundedCornerShape(16.dp))
                  .background(SpaceSurface)
                  .border(1.dp, levelColor.copy(alpha = 0.45f), RoundedCornerShape(16.dp))
                  .padding(12.dp)
              ) {
                Column {
                  Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                  ) {
                    Column {
                      Text(
                        text = "ACTIVACIÓN ACTUAL",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextMuted,
                        fontWeight = FontWeight.Bold,
                        fontSize = 9.sp
                      )
                      Text(
                        text = "${operationalState.compositeScore} / 100",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.ExtraBold,
                        color = levelColor
                      )
                    }
                    OperationalLevelBadge(level = operationalState.level)
                  }

                  Spacer(modifier = Modifier.height(8.dp))

                  LinearProgressIndicator(
                    progress = { operationalState.compositeScore / 100f },
                    modifier = Modifier
                      .fillMaxWidth()
                      .height(6.dp)
                      .clip(RoundedCornerShape(3.dp)),
                    color = levelColor,
                    trackColor = SpaceSurfaceCard
                  )

                  Spacer(modifier = Modifier.height(8.dp))

                  Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                  ) {
                    Text(
                      text = "Última eval: Día ${profile.missionDay}",
                      style = MaterialTheme.typography.labelSmall,
                      color = TextMuted,
                      fontSize = 10.sp
                    )
                    Text(
                      text = "Basal: ${baseline.traitAnxiety}/10",
                      style = MaterialTheme.typography.labelSmall,
                      color = TealLight,
                      fontSize = 10.sp
                    )
                  }
                }
              }
            }
          }
        }
      }

      // 2. PROMINENT MODULE: SPACE MIND VOICE AI
      item {
        SpaceMindVoiceAiCard(
          selectedLanguageCode = voiceEvalState.language.label,
          onSelectLanguage = { code ->
            viewModel.setVoiceLanguage(if (code == "EN") VoiceLanguage.EN else VoiceLanguage.ES)
          },
          onStartVoiceEvaluation = { viewModel.startVoiceEvaluation() }
        )
      }

      // 3. Semicircular Activation Gauge
      item {
        SpaceCard(
          borderColor = when (operationalState.level) {
            OperationalLevel.ESTABLE -> StatusStableGreen.copy(alpha = 0.55f)
            OperationalLevel.ACTIVACION_LEVE -> StatusMildYellow.copy(alpha = 0.55f)
            OperationalLevel.ACTIVACION_MODERADA -> StatusModerateOrange.copy(alpha = 0.55f)
            OperationalLevel.ACTIVACION_ELEVADA -> StatusSevereRed.copy(alpha = 0.55f)
          },
          backgroundColor = SpaceSurfaceCard,
          onClick = { viewModel.navigateTo(AppScreen.LEVEL_4_CLASSIFICATION) }
        ) {
          SemicircularActivationGauge(
            score = operationalState.compositeScore,
            level = operationalState.level
          )
          Spacer(modifier = Modifier.height(6.dp))
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Text(
              text = "Ver niveles y criterios",
              style = MaterialTheme.typography.labelSmall,
              color = CyanNeon,
              fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.width(4.dp))
            Icon(
              imageVector = Icons.AutoMirrored.Filled.ArrowForward,
              contentDescription = null,
              tint = CyanNeon,
              modifier = Modifier.size(14.dp)
            )
          }
        }
      }

      // 4. Compact Telemetry Strip
      item {
        TelemetryStrip(telemetry = telemetry)
      }

      // 5. Quick Visual Gauges: Subjective Anxiety & Sleep Quality
      item {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
          SpaceCard(
            modifier = Modifier.weight(1f),
            borderColor = VioletNeon.copy(alpha = 0.4f)
          ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Icon(
                imageVector = Icons.Default.Psychology,
                contentDescription = null,
                tint = VioletSoft,
                modifier = Modifier.size(16.dp)
              )
              Spacer(modifier = Modifier.width(6.dp))
              Text(
                text = "Ansiedad",
                style = MaterialTheme.typography.labelMedium,
                color = VioletSoft,
                fontWeight = FontWeight.Bold
              )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.Bottom) {
              Text(
                text = "%.1f".format(anxiety),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = if (anxiety > 6) StatusSevereRed else if (anxiety > 3.5) StatusModerateOrange else StatusStableGreen
              )
              Text(
                text = " /10",
                style = MaterialTheme.typography.labelSmall,
                color = TextMuted,
                modifier = Modifier.padding(bottom = 4.dp)
              )
            }
            Slider(
              value = anxiety,
              onValueChange = { viewModel.updateAnxiety(it) },
              valueRange = 0f..10f,
              steps = 9,
              colors = SliderDefaults.colors(
                thumbColor = VioletNeon,
                activeTrackColor = VioletNeon,
                inactiveTrackColor = SpaceBorder
              ),
              modifier = Modifier.testTag("anxiety_slider")
            )
          }

          SpaceCard(
            modifier = Modifier.weight(1f),
            borderColor = TealLight.copy(alpha = 0.4f)
          ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Icon(
                imageVector = Icons.Default.Bedtime,
                contentDescription = null,
                tint = TealLight,
                modifier = Modifier.size(16.dp)
              )
              Spacer(modifier = Modifier.width(6.dp))
              Text(
                text = "Sueño",
                style = MaterialTheme.typography.labelMedium,
                color = TealLight,
                fontWeight = FontWeight.Bold
              )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.Bottom) {
              Text(
                text = "%.1f".format(sleepQuality),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = if (sleepQuality < 5) StatusSevereRed else if (sleepQuality < 7) StatusMildYellow else StatusStableGreen
              )
              Text(
                text = " /10",
                style = MaterialTheme.typography.labelSmall,
                color = TextMuted,
                modifier = Modifier.padding(bottom = 4.dp)
              )
            }
            Slider(
              value = sleepQuality,
              onValueChange = { viewModel.updateSleep(it) },
              valueRange = 0f..10f,
              steps = 9,
              colors = SliderDefaults.colors(
                thumbColor = TealLight,
                activeTrackColor = TealLight,
                inactiveTrackColor = SpaceBorder
              ),
              modifier = Modifier.testTag("sleep_slider")
            )
          }
        }
      }

      // 6. Core Navigation Hubs (3 Pillars)
      item {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
          HubNavigationBox(
            title = "Evaluación",
            subtitle = "Niveles 1-4",
            icon = Icons.Default.Speed,
            accentColor = CyanNeon,
            modifier = Modifier.weight(1f),
            onClick = { viewModel.navigateTo(AppScreen.LEVEL_4_CLASSIFICATION) }
          )
          HubNavigationBox(
            title = "Técnicas",
            subtitle = "10 Protocolos",
            icon = Icons.Default.SelfImprovement,
            accentColor = TealLight,
            modifier = Modifier.weight(1f),
            onClick = { viewModel.navigateTo(AppScreen.LEVEL_5_INTERVENTIONS) }
          )
          HubNavigationBox(
            title = "Seguimiento",
            subtitle = "Evolución",
            icon = Icons.Default.Timeline,
            accentColor = VioletNeon,
            modifier = Modifier.weight(1f),
            onClick = { viewModel.navigateTo(AppScreen.HISTORY_ANALYTICS) }
          )
        }
      }

      // 7. Recommended Intervention Spotlight (Direct + Voice Launch)
      item {
        SpaceCard(
          borderColor = CyanNeon.copy(alpha = 0.65f),
          backgroundColor = SpaceSurfaceVariant
        ) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Icon(
                imageVector = Icons.Default.Recommend,
                contentDescription = null,
                tint = CyanNeon,
                modifier = Modifier.size(18.dp)
              )
              Spacer(modifier = Modifier.width(6.dp))
              Text(
                text = "INTERVENCIÓN SUGERIDA",
                style = MaterialTheme.typography.labelMedium,
                color = CyanNeon,
                fontWeight = FontWeight.Bold
              )
            }
            Text(
              text = "${recommendedProg.durationMin} MIN",
              style = MaterialTheme.typography.labelSmall,
              color = TealLight,
              fontWeight = FontWeight.Bold
            )
          }

          Spacer(modifier = Modifier.height(6.dp))
          Text(
            text = recommendedProg.title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = TextPrimary
          )
          Spacer(modifier = Modifier.height(10.dp))

          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            Button(
              onClick = {
                viewModel.startIntervention(recommendedProg, withVoice = false)
                viewModel.navigateTo(AppScreen.LEVEL_5_INTERVENTIONS)
              },
              modifier = Modifier
                .weight(1f)
                .heightIn(min = 48.dp)
                .testTag("start_recommended_button"),
              colors = ButtonDefaults.buttonColors(containerColor = SpaceSurfaceCard, contentColor = CyanNeon),
              border = BorderStroke(1.dp, CyanNeon),
              shape = RoundedCornerShape(10.dp)
            ) {
              Icon(imageVector = Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
              Spacer(modifier = Modifier.width(4.dp))
              Text("GUIADO", fontWeight = FontWeight.Bold, fontSize = 12.sp)
            }

            Button(
              onClick = {
                viewModel.startIntervention(recommendedProg, withVoice = true)
                viewModel.navigateTo(AppScreen.LEVEL_5_INTERVENTIONS)
              },
              modifier = Modifier
                .weight(1f)
                .heightIn(min = 48.dp)
                .testTag("start_recommended_voice_button"),
              colors = ButtonDefaults.buttonColors(containerColor = CyanNeon, contentColor = SpaceDeepNavy),
              shape = RoundedCornerShape(10.dp)
            ) {
              Icon(imageVector = Icons.Default.RecordVoiceOver, contentDescription = null, modifier = Modifier.size(18.dp))
              Spacer(modifier = Modifier.width(4.dp))
              Text("POR VOZ", fontWeight = FontWeight.Bold, fontSize = 12.sp)
            }
          }
        }
      }

      // 8. Compact Evaluation Levels Grid
      item {
        Text(
          text = "NIVELES DE EVALUACIÓN",
          style = MaterialTheme.typography.labelMedium,
          color = CyanNeon,
          fontWeight = FontWeight.Bold,
          letterSpacing = 1.sp
        )
      }

      item {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
          Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            CompactLevelTile(
              code = "N1",
              title = "Línea Base",
              icon = Icons.Default.AssignmentInd,
              color = CyanNeon,
              modifier = Modifier.weight(1f),
              onClick = { viewModel.navigateTo(AppScreen.LEVEL_1_BASELINE) }
            )
            CompactLevelTile(
              code = "N2",
              title = "Somático",
              icon = Icons.Default.Accessibility,
              color = VioletNeon,
              modifier = Modifier.weight(1f),
              onClick = { viewModel.navigateTo(AppScreen.LEVEL_2_SOMATIC) }
            )
          }
          Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            CompactLevelTile(
              code = "N3",
              title = "Cognitivo TCC",
              icon = Icons.Default.PsychologyAlt,
              color = TealLight,
              modifier = Modifier.weight(1f),
              onClick = { viewModel.navigateTo(AppScreen.LEVEL_3_COGNITIVE) }
            )
            CompactLevelTile(
              code = "N4",
              title = "Clasificación",
              icon = Icons.Default.Speed,
              color = StatusMildYellow,
              modifier = Modifier.weight(1f),
              onClick = { viewModel.navigateTo(AppScreen.LEVEL_4_CLASSIFICATION) }
            )
          }
        }
      }

      // 9. Concise Recent Logs
      item {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Text(
            text = "ÚLTIMAS SESIONES",
            style = MaterialTheme.typography.labelMedium,
            color = CyanNeon,
            fontWeight = FontWeight.Bold
          )
          TextButton(onClick = { viewModel.navigateTo(AppScreen.HISTORY_ANALYTICS) }) {
            Text(text = "Ver todo", color = CyanNeon, style = MaterialTheme.typography.labelSmall)
          }
        }
      }

      items(sessionLogs.take(2)) { log ->
        SpaceCard(
          borderColor = SpaceBorder,
          backgroundColor = SpaceSurface
        ) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Text(
              text = log.moduleName,
              style = MaterialTheme.typography.labelMedium,
              fontWeight = FontWeight.Bold,
              color = CyanGlow,
              modifier = Modifier.weight(1f)
            )
            Text(
              text = log.timestamp,
              style = MaterialTheme.typography.labelSmall,
              color = TextMuted
            )
          }
          Spacer(modifier = Modifier.height(2.dp))
          Text(
            text = log.description,
            style = MaterialTheme.typography.bodySmall,
            color = TextSecondary,
            maxLines = 1
          )
        }
      }
    }
  }
}

@Composable
private fun HubNavigationBox(
  title: String,
  subtitle: String,
  icon: ImageVector,
  accentColor: Color,
  onClick: () -> Unit,
  modifier: Modifier = Modifier
) {
  Box(
    modifier = modifier
      .clip(RoundedCornerShape(14.dp))
      .background(SpaceSurfaceCard)
      .border(1.dp, accentColor.copy(alpha = 0.4f), RoundedCornerShape(14.dp))
      .clickable { onClick() }
      .padding(vertical = 14.dp, horizontal = 8.dp),
    contentAlignment = Alignment.Center
  ) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
      Box(
        modifier = Modifier
          .size(36.dp)
          .clip(CircleShape)
          .background(accentColor.copy(alpha = 0.15f))
          .border(1.dp, accentColor.copy(alpha = 0.5f), CircleShape),
        contentAlignment = Alignment.Center
      ) {
        Icon(
          imageVector = icon,
          contentDescription = null,
          tint = accentColor,
          modifier = Modifier.size(18.dp)
        )
      }
      Spacer(modifier = Modifier.height(6.dp))
      Text(
        text = title,
        style = MaterialTheme.typography.titleSmall,
        color = TextPrimary,
        fontWeight = FontWeight.Bold,
        fontSize = 12.sp
      )
      Text(
        text = subtitle,
        style = MaterialTheme.typography.labelSmall,
        color = TextSecondary,
        fontSize = 9.sp
      )
    }
  }
}

@Composable
private fun CompactLevelTile(
  code: String,
  title: String,
  icon: ImageVector,
  color: Color,
  onClick: () -> Unit,
  modifier: Modifier = Modifier
) {
  SpaceCard(
    modifier = modifier,
    borderColor = color.copy(alpha = 0.35f),
    backgroundColor = SpaceSurfaceCard,
    onClick = onClick
  ) {
    Row(verticalAlignment = Alignment.CenterVertically) {
      Box(
        modifier = Modifier
          .size(34.dp)
          .clip(RoundedCornerShape(10.dp))
          .background(color.copy(alpha = 0.15f))
          .border(1.dp, color.copy(alpha = 0.5f), RoundedCornerShape(10.dp)),
        contentAlignment = Alignment.Center
      ) {
        Icon(
          imageVector = icon,
          contentDescription = null,
          tint = color,
          modifier = Modifier.size(18.dp)
        )
      }
      Spacer(modifier = Modifier.width(10.dp))
      Column {
        Text(
          text = code,
          style = MaterialTheme.typography.labelSmall,
          color = color,
          fontWeight = FontWeight.Bold,
          fontSize = 10.sp
        )
        Text(
          text = title,
          style = MaterialTheme.typography.titleSmall,
          color = TextPrimary,
          fontWeight = FontWeight.Bold,
          fontSize = 12.sp
        )
      }
    }
  }
}
