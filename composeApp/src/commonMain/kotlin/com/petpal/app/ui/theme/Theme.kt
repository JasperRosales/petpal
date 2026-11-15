package com.petpal.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.petpal.app.ui.components.PixelLarge
import com.petpal.app.ui.components.PixelMedium
import com.petpal.app.ui.components.PixelSmall
import org.jetbrains.compose.resources.Font
import petpal.composeapp.generated.resources.Res
import petpal.composeapp.generated.resources.pixel_body
import petpal.composeapp.generated.resources.pixel_display

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

/** Every Material surface gets stepped pixel corners instead of rounded ones. */
private val PetPalShapes = Shapes(
    extraSmall = PixelSmall,
    small = PixelSmall,
    medium = PixelMedium,
    large = PixelLarge,
    extraLarge = PixelLarge,
)

/**
 * Two bitmap-style fonts: Press Start 2P (chunky, wide) for big headings, Pixelify Sans for
 * everything you actually read. Both OFL, bundled in composeResources/font.
 */
@Composable
private fun pixelTypography(): Typography {
    val display = FontFamily(Font(Res.font.pixel_display))
    val body = FontFamily(
        Font(Res.font.pixel_body, FontWeight.Normal),
        Font(Res.font.pixel_body, FontWeight.Medium),
        Font(Res.font.pixel_body, FontWeight.SemiBold),
        Font(Res.font.pixel_body, FontWeight.Bold),
    )
    fun d(size: Int, line: Int) = TextStyle(fontFamily = display, fontSize = size.sp, lineHeight = line.sp)
    fun b(size: Int, line: Int, w: FontWeight = FontWeight.Normal) =
        TextStyle(fontFamily = body, fontSize = size.sp, lineHeight = line.sp, fontWeight = w)
    return Typography(
        displayLarge = d(34, 44), displayMedium = d(28, 36), displaySmall = d(22, 30),
        headlineLarge = d(20, 28), headlineMedium = d(17, 24), headlineSmall = d(14, 22),
        titleLarge = d(13, 20), titleMedium = b(18, 22, FontWeight.SemiBold), titleSmall = b(16, 20, FontWeight.SemiBold),
        bodyLarge = b(17, 22), bodyMedium = b(15, 20), bodySmall = b(13, 17),
        labelLarge = b(15, 18, FontWeight.SemiBold), labelMedium = b(13, 16, FontWeight.Medium), labelSmall = b(12, 14, FontWeight.Medium),
    )
}

/** Always dark + orange; [darkTheme] is kept for call-site compatibility. */
@Composable
fun PetPalTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = EmberColors,
        shapes = PetPalShapes,
        typography = pixelTypography(),
        content = content,
    )
}
