package com.petpal.app.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.unit.dp
import com.petpal.app.ui.theme.*

@Composable
fun GrassyFieldScene(
    isNight: Boolean,
    modifier: Modifier = Modifier,
    pet: @Composable () -> Unit,
) {
    val skyTop = if (isNight) NightSkyTop else DaySkyTop
    val skyBottom = if (isNight) NightSkyBottom else DaySkyBottom
    val hill = if (isNight) HillDark else HillLight
    val soil = if (isNight) NightSoil else Soil

    Box(modifier, contentAlignment = Alignment.BottomCenter) {
        Canvas(Modifier.matchParentSize()) {
            drawRect(Brush.verticalGradient(listOf(skyTop, skyBottom)))

            drawCircle(
                color = if (isNight) Moon else Sunflower,
                radius = size.minDimension * 0.08f,
                center = Offset(size.width * 0.8f, size.height * 0.2f),
            )

            if (isNight) {
                // A few fixed fireflies
                listOf(0.15f to 0.62f, 0.32f to 0.78f, 0.68f to 0.72f, 0.88f to 0.82f).forEach { (x, y) ->
                    drawCircle(Moon.copy(alpha = 0.8f), 3.dp.toPx(), Offset(size.width * x, size.height * y))
                }
            }

            val hillPath = Path().apply {
                moveTo(0f, size.height * 0.7f)
                cubicTo(
                    size.width * 0.3f, size.height * 0.55f,
                    size.width * 0.7f, size.height * 0.6f,
                    size.width, size.height * 0.68f,
                )
                lineTo(size.width, size.height)
                lineTo(0f, size.height)
                close()
            }
            drawPath(hillPath, hill)

            drawRect(
                color = soil,
                topLeft = Offset(0f, size.height * 0.94f),
                size = size.copy(height = size.height * 0.06f),
            )
        }
        Box(Modifier.padding(bottom = 32.dp)) { pet() }
    }
}
