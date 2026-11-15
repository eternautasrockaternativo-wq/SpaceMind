package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ai.configureAppCheck
import com.example.ui.AppScreen
import com.example.ui.SpaceMindViewModel
import com.example.ui.components.MedicalSafetyProtocolDialog
import com.example.ui.components.PrivacyProtocolDialog
import com.example.ui.screens.*
import com.example.ui.theme.*
import com.example.voice.SpaceVoiceEngine

class MainActivity : ComponentActivity() {

  private val viewModel: SpaceMindViewModel by viewModels()

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    // Configure Firebase App Check before UI, ViewModels, or AI calls
    configureAppCheck(this, intent)

    enableEdgeToEdge()
    val voiceEngine = SpaceVoiceEngine(applicationContext)
    viewModel.attachVoiceEngine(voiceEngine)

    setContent {
      SpaceMindTheme {
        SpaceMindApp(viewModel = viewModel)
      }
    }
  }

  override fun onPause() {
    super.onPause()
    viewModel.onAppPaused()
  }
}

@Composable
fun SpaceMindApp(viewModel: SpaceMindViewModel) {
  val currentScreen by viewModel.currentScreen.collectAsState()
  val showMedicalProtocol by viewModel.showMedicalProtocol.collectAsState()
  val showPrivacyDialog by viewModel.showPrivacyDialog.collectAsState()

  Scaffold(
    modifier = Modifier
      .fillMaxSize()
      .background(SpaceDeepNavy),
    bottomBar = {
      if (currentScreen != AppScreen.VOICE_EVALUATION) {
        NavigationBar(
          modifier = Modifier
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.navigationBars)
            .border(1.dp, SpaceBorder)
            .testTag("main_navigation_bar"),
          containerColor = SpaceSurface,
          contentColor = TextPrimary
        ) {
          NavigationBarItem(
            selected = currentScreen == AppScreen.DASHBOARD,
            onClick = { viewModel.navigateTo(AppScreen.DASHBOARD) },
            icon = {
              Icon(
                imageVector = Icons.Default.Dashboard,
                contentDescription = "Dashboard"
              )
            },
            label = { Text("Panel", fontSize = 10.sp) },
            colors = NavigationBarItemDefaults.colors(
              selectedIconColor = SpaceDeepNavy,
              selectedTextColor = CyanNeon,
              indicatorColor = CyanNeon,
              unselectedIconColor = TextSecondary,
              unselectedTextColor = TextSecondary
            ),
            modifier = Modifier.testTag("nav_dashboard")
          )

          NavigationBarItem(
            selected = currentScreen in listOf(
              AppScreen.LEVEL_1_BASELINE,
              AppScreen.LEVEL_2_SOMATIC,
              AppScreen.LEVEL_3_COGNITIVE,
              AppScreen.LEVEL_4_CLASSIFICATION
            ),
            onClick = { viewModel.navigateTo(AppScreen.LEVEL_4_CLASSIFICATION) },
            icon = {
              Icon(
                imageVector = Icons.Default.Speed,
                contentDescription = "Evaluación"
              )
            },
            label = { Text("Niveles 1-4", fontSize = 10.sp) },
            colors = NavigationBarItemDefaults.colors(
              selectedIconColor = SpaceDeepNavy,
              selectedTextColor = CyanNeon,
              indicatorColor = CyanNeon,
              unselectedIconColor = TextSecondary,
              unselectedTextColor = TextSecondary
            ),
            modifier = Modifier.testTag("nav_levels")
          )

          NavigationBarItem(
            selected = currentScreen == AppScreen.LEVEL_5_INTERVENTIONS,
            onClick = { viewModel.navigateTo(AppScreen.LEVEL_5_INTERVENTIONS) },
            icon = {
              Icon(
                imageVector = Icons.Default.SelfImprovement,
                contentDescription = "Intervenciones"
              )
            },
            label = { Text("Técnicas", fontSize = 10.sp) },
            colors = NavigationBarItemDefaults.colors(
              selectedIconColor = SpaceDeepNavy,
              selectedTextColor = CyanNeon,
              indicatorColor = CyanNeon,
              unselectedIconColor = TextSecondary,
              unselectedTextColor = TextSecondary
            ),
            modifier = Modifier.testTag("nav_interventions")
          )

          NavigationBarItem(
            selected = currentScreen == AppScreen.HISTORY_ANALYTICS,
            onClick = { viewModel.navigateTo(AppScreen.HISTORY_ANALYTICS) },
            icon = {
              Icon(
                imageVector = Icons.Default.Timeline,
                contentDescription = "Seguimiento"
              )
            },
            label = { Text("Seguimiento", fontSize = 10.sp) },
            colors = NavigationBarItemDefaults.colors(
              selectedIconColor = SpaceDeepNavy,
              selectedTextColor = TealLight,
              indicatorColor = TealLight,
              unselectedIconColor = TextSecondary,
              unselectedTextColor = TextSecondary
            ),
            modifier = Modifier.testTag("nav_history")
          )
        }
      }
    },
    containerColor = SpaceDeepNavy
  ) { innerPadding ->
    Box(
      modifier = Modifier
        .fillMaxSize()
        .padding(innerPadding)
    ) {
      when (currentScreen) {
        AppScreen.DASHBOARD -> DashboardScreen(viewModel = viewModel)
        AppScreen.VOICE_EVALUATION -> VoiceEvaluationScreen(viewModel = viewModel)
        AppScreen.LEVEL_1_BASELINE -> Level1BaselineScreen(viewModel = viewModel)
        AppScreen.LEVEL_2_SOMATIC -> Level2SomaticScreen(viewModel = viewModel)
        AppScreen.LEVEL_3_COGNITIVE -> Level3CognitiveScreen(viewModel = viewModel)
        AppScreen.LEVEL_4_CLASSIFICATION -> Level4ClassificationScreen(viewModel = viewModel)
        AppScreen.LEVEL_5_INTERVENTIONS -> Level5InterventionsScreen(viewModel = viewModel)
        AppScreen.HISTORY_ANALYTICS -> HistoryAnalyticsScreen(viewModel = viewModel)
      }
    }
  }

  if (showMedicalProtocol) {
    MedicalSafetyProtocolDialog(
      onDismiss = { viewModel.setShowMedicalProtocol(false) }
    )
  }

  if (showPrivacyDialog) {
    PrivacyProtocolDialog(
      onDismiss = { viewModel.setShowPrivacyDialog(false) }
    )
  }
}
