package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.shape.RoundedCornerShape

private val FacePulseColorScheme = darkColorScheme(
    primary = CyanScanner,
    onPrimary = OnCyanScanner,
    primaryContainer = CyanScannerContainer,
    onPrimaryContainer = CyanScanner,
    secondary = SkyElectric,
    onSecondary = OnSkyElectric,
    tertiary = AmberSignal,
    background = ObsidianDeep,
    onBackground = IceText,
    surface = SlateSurface,
    onSurface = IceText,
    surfaceVariant = SlateElevated,
    onSurfaceVariant = CoolMuted,
    surfaceContainerHigh = SlateElevated,
    outline = SlateBorder,
    error = CoralStop,
    onError = OnCoralStop,
    errorContainer = CoralStopContainer,
    onErrorContainer = OnCoralStop,
)

private val FacePulseShapes = Shapes(
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(24.dp),
)

@Composable
fun FacePulseTheme(
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = FacePulseColorScheme,
        typography = Typography,
        shapes = FacePulseShapes,
        content = content,
    )
}
