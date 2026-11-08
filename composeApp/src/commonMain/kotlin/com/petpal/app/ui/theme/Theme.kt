package com.petpal.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/** One look everywhere: dark canvas + orange, matching Isha and Zoih. */
private val EmberColors = darkColorScheme(
    primary = EmberOrange,
    onPrimary = EmberCanvas,
    primaryContainer = EmberOrangeDeep,
    onPrimaryContainer = Color(0xFFFFD9C2),
    secondary = EmberOrangeBright,
    onSecondary = EmberCanvas,
    secondaryContainer = EmberWell,
    onSecondaryContainer = EmberCream,
    tertiary = EmberGold,
    onTertiary = EmberCanvas,
    tertiaryContainer = Color(0xFF3A2A16),
    onTertiaryContainer = EmberCream,
    background = EmberCanvas,
    onBackground = EmberCream,
    surface = EmberSurface,
    onSurface = EmberCream,
    surfaceVariant = EmberWell,
    onSurfaceVariant = EmberCreamDim,
    surfaceContainerLowest = EmberCanvas,
    surfaceContainerLow = EmberSurface,
    surfaceContainer = EmberSurface,
    surfaceContainerHigh = EmberWell,
    surfaceContainerHighest = Color(0xFF2C2720),
    outline = EmberLine,
    outlineVariant = Color(0xFF2C2720),
    error = EmberRed,
    onError = EmberCanvas,
)

private val PetPalShapes = Shapes(
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(20.dp),
    large = RoundedCornerShape(28.dp),
)

/** Always dark + orange; [darkTheme] is kept for call-site compatibility. */
@Composable
fun PetPalTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = EmberColors,
        shapes = PetPalShapes,
        typography = Typography(), // swap in a pixel font family later
        content = content,
    )
}
