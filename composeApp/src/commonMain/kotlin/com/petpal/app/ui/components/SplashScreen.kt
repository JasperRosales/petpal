package com.petpal.app.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.petpal.app.domain.model.Mood
import com.petpal.app.domain.model.Species
import com.petpal.app.domain.model.Stage
import com.petpal.app.ui.theme.*
import kotlinx.coroutines.delay

/** Launch splash: the pet pops in, the title fades up, then [onDone]. */
@Composable
fun SplashScreen(onDone: () -> Unit) {
    val pop = remember { Animatable(0f) }
    val text = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        pop.animateTo(1f, spring(dampingRatio = 0.45f, stiffness = 260f))
        text.animateTo(1f, tween(350))
        delay(500)
        onDone()
    }
    val bob by rememberInfiniteTransition().animateFloat(0f, 1f, infiniteRepeatable(tween(600), RepeatMode.Reverse))
    Box(
        Modifier.fillMaxSize().background(EmberCanvas).drawBehind {
            drawCircle(Brush.radialGradient(listOf(EmberOrange.copy(alpha = 0.22f * pop.value), EmberCanvas.copy(alpha = 0f)),
                center, size.minDimension * 0.6f), size.minDimension * 0.6f, center)
        },
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            PetSprite(
                stage = Stage.BABY, mood = Mood.HAPPY, species = Species.DRAKE, size = 140.dp,
                modifier = Modifier.graphicsLayer {
                    scaleX = pop.value; scaleY = pop.value
                    translationY = -10.dp.toPx() * bob
                },
            )
            Spacer(Modifier.height(20.dp))
            Text("PetPal", color = EmberCream, fontSize = 40.sp, fontWeight = FontWeight.Black,
                modifier = Modifier.graphicsLayer { alpha = text.value; translationY = 16.dp.toPx() * (1 - text.value) })
            Text("grow together", color = EmberOrange, style = MaterialTheme.typography.labelLarge,
                modifier = Modifier.graphicsLayer { alpha = text.value })
        }
    }
}
