package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val SpaceMindColorScheme = darkColorScheme(
  primary = CyanNeon,
  onPrimary = SpaceDeepNavy,
  primaryContainer = SpaceSurfaceVariant,
  onPrimaryContainer = CyanNeon,
  secondary = VioletNeon,
  onSecondary = Color.White,
  secondaryContainer = SpaceSurfaceVariant,
  onSecondaryContainer = VioletSoft,
  tertiary = TealLight,
  onTertiary = SpaceDeepNavy,
  tertiaryContainer = SpaceSurfaceCard,
  onTertiaryContainer = TealLight,
  background = SpaceDeepNavy,
  onBackground = TextPrimary,
  surface = SpaceSurface,
  onSurface = TextPrimary,
  surfaceVariant = SpaceSurfaceCard,
  onSurfaceVariant = TextSecondary,
  outline = SpaceBorder,
  error = StatusSevereRed,
  onError = Color.White
)

@Composable
fun SpaceMindTheme(
  content: @Composable () -> Unit
) {
  // SPACE MIND is strictly an immersive deep-space clinical command center UI
  MaterialTheme(
    colorScheme = SpaceMindColorScheme,
    typography = Typography,
    content = content
  )
}
