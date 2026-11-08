package com.petpal.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private val MeadowColors = lightColorScheme(
    primary = MeadowGreen,
    onPrimary = Color.White,
    primaryContainer = FreshLeaf,
    onPrimaryContainer = Color(0xFF0B3912),
    secondary = ClearSky,
    onSecondary = Color.White,
    secondaryContainer = LightSky,
    onSecondaryContainer = Color(0xFF0C3448),
    tertiary = Sunflower,
    onTertiary = Color.White,
    tertiaryContainer = MorningSun,
    onTertiaryContainer = Color(0xFF4A2F00),
    background = MorningDew,
    onBackground = Color(0xFF1A2219),
    surface = Color.White,
    onSurface = Color(0xFF1A2219),
    surfaceVariant = PaleGrass,
    onSurfaceVariant = Color(0xFF424D40),
    outline = Color(0xFFB7C4B0),
    error = Clay,
)

private val NightFieldColors = darkColorScheme(
    primary = MoonlitGrass,
    onPrimary = Color(0xFF0B3912),
    primaryContainer = DeepLeaf,
    onPrimaryContainer = FreshLeaf,
    secondary = NightSky,
    onSecondary = Color(0xFF0C3448),
    secondaryContainer = DeepSky,
    onSecondaryContainer = LightSky,
    tertiary = Moon,
    onTertiary = Color(0xFF4A2F00),
    tertiaryContainer = DuskSun,
    onTertiaryContainer = MorningSun,
    background = NightField,
    onBackground = Color(0xFFE0E8DC),
    surface = NightSurface,
    onSurface = Color(0xFFE0E8DC),
    surfaceVariant = NightGrass,
    onSurfaceVariant = Color(0xFFC0CBBB),
    outline = Color(0xFF4B5B4D),
    error = SoftClay,
)

private val PetPalShapes = Shapes(
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(20.dp),
    large = RoundedCornerShape(28.dp),
)

/** Dynamic color is intentionally off so the grassy field look is consistent. */
@Composable
fun PetPalTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) NightFieldColors else MeadowColors,
        shapes = PetPalShapes,
        typography = Typography(), // swap in a pixel font family later
        content = content,
    )
}
