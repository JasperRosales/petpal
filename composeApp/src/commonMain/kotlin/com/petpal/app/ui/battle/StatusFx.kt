package com.petpal.app.ui.battle

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.petpal.app.domain.battle.FighterSnap
import com.petpal.app.domain.battle.Mechanics
import com.petpal.app.domain.battle.Status
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/** Status bits that hurt the pet carrying them. */
internal const val DEBUFFS = Status.BURN or Status.SLOW or Status.STUN or Status.ROOT or Status.WEAKEN or
    Status.PARALYZE or Status.LEECH or Status.MARKED

private val BurnRed = Color(0xFFE5532D)
private val BurnGold = Color(0xFFFFC14A)
private val SlowBlue = Color(0xFF7FC8E8)
private val StunGold = Color(0xFFFFD54F)
private val ParaYellow = Color(0xFFFFEB3B)
private val RootGreen = Color(0xFF3F9A4A)
private val LeafGreen = Color(0xFF7CC46A)
private val WeakGrey = Color(0xFF9E9E9E)
private val LeechGreen = Color(0xFF4CAF6A)
private val MarkRed = Color(0xFFE53935)
private const val TAU = (2 * PI).toFloat()

/** Ms clock that ticks every frame, for looping status animations. */
@Composable
internal fun rememberFxClock(): Long {
    val start = remember { kotlin.time.TimeSource.Monotonic.markNow() }
    var now by remember { mutableLongStateOf(0L) }
    LaunchedEffect(Unit) { while (true) withFrameMillis { now = start.elapsedNow().inWholeMilliseconds } }
    return now
}

/** Paralysis jolts and stun wobble applied to the sprite, in px. */
internal fun statusShake(s: FighterSnap, ms: Long, px: Float): Offset = when {
    s.has(Status.PARALYZE) && ms % Mechanics.PARALYZE_TWITCH_EVERY < Mechanics.PARALYZE_TWITCH_MS ->
        Offset(if ((ms / 40) % 2 == 0L) px * 2.5f else -px * 2.5f, if ((ms / 60) % 2 == 0L) px else -px)
    s.has(Status.STUN) -> Offset(sin(ms / 45f) * px * 1.5f, 0f)
    s.has(Status.SLOW) -> Offset(0f, sin(ms / 260f) * px) // heavy, sluggish sway
    else -> Offset.Zero
}

/** Weakened pets visibly shrink a little; slowed ones squash. Returns (scaleX, scaleY). */
internal fun statusScale(s: FighterSnap): Pair<Float, Float> = when {
    s.has(Status.WEAKEN) -> 0.9f to 0.88f
    s.has(Status.SLOW) -> 1.04f to 0.94f
    else -> 1f to 1f
}

/** Drawn over the pet sprite: animated, readable at a glance. Cheap shapes only, no allocations per frame beyond Offsets. */
internal fun DrawScope.drawStatusFx(s: FighterSnap, c: Offset, r: Float, ms: Long) {
    if (s.hp <= 0 || s.has(Status.VANISH) || s.status and DEBUFFS == 0) return
    val dp = 1.dp.toPx()
    val t = ms / 1000f

    if (s.has(Status.WEAKEN)) {
        drawCircle(WeakGrey.copy(alpha = 0.32f), r * 0.8f, c) // drained, washed-out body
        for (k in 0..1) { // arrows sinking on both sides
            val x = c.x + (if (k == 0) -1 else 1) * r * 0.95f
            val y = c.y - r * 0.3f + ((t * 0.9f + k * 0.5f) % 1f) * r * 0.8f
            val a = 1f - ((t * 0.9f + k * 0.5f) % 1f)
            drawLine(WeakGrey.copy(alpha = a), Offset(x, y - 9 * dp), Offset(x, y + 5 * dp), 3 * dp, StrokeCap.Round)
            drawLine(WeakGrey.copy(alpha = a), Offset(x - 5 * dp, y), Offset(x, y + 6 * dp), 3 * dp, StrokeCap.Round)
            drawLine(WeakGrey.copy(alpha = a), Offset(x + 5 * dp, y), Offset(x, y + 6 * dp), 3 * dp, StrokeCap.Round)
        }
    }
    if (s.has(Status.SLOW)) {
        drawCircle(Brush.radialGradient(listOf(SlowBlue.copy(alpha = 0.45f), Color.Transparent), c, r), r, c)
        for (k in 0..4) { // syrupy drips falling off the body
            val p = (t * 0.7f + k * 0.21f) % 1f
            val x = c.x - r * 0.6f + k * r * 0.3f
            val y = c.y + r * 0.1f + p * r * 0.8f
            drawOval(SlowBlue.copy(alpha = 0.85f * (1f - p)), Offset(x - 2.5f * dp, y - 4 * dp), Size(5 * dp, 8 * dp))
        }
        // drag lines behind
        drawArc(SlowBlue.copy(alpha = 0.6f), 200f, 140f, false, Offset(c.x - r, c.y - r * 0.2f), Size(r * 2, r * 1.4f),
            style = Stroke(3 * dp, cap = StrokeCap.Round))
    }
    if (s.has(Status.ROOT)) { // vines curling up around the legs
        for (k in 0..3) {
            val sx = c.x - r * 0.75f + k * r * 0.5f
            val sway = sin(t * 3f + k) * 3 * dp
            val top = c.y + r * (0.05f + 0.1f * (k % 2))
            drawLine(RootGreen, Offset(sx, c.y + r * 0.75f), Offset(sx + sway, top), 5 * dp, StrokeCap.Round)
            drawOval(LeafGreen, Offset(sx + sway - 2 * dp, top - 6 * dp), Size(8 * dp, 5 * dp))
        }
        drawArc(RootGreen, 10f, 160f, false, Offset(c.x - r * 0.85f, c.y), Size(r * 1.7f, r * 0.8f), style = Stroke(5 * dp, cap = StrokeCap.Round))
        drawOval(Color(0xFF5D4037).copy(alpha = 0.5f), Offset(c.x - r * 0.8f, c.y + r * 0.6f), Size(r * 1.6f, r * 0.3f))
    }
    if (s.has(Status.BURN)) {
        drawCircle(Brush.radialGradient(listOf(BurnRed.copy(alpha = 0.3f), Color.Transparent), c, r * 1.1f), r * 1.1f, c)
        for (k in 0..4) { // flames licking up the body
            val p = (t * 1.8f + k * 0.37f) % 1f
            val x = c.x - r * 0.7f + k * r * 0.35f + sin(t * 9f + k) * 2 * dp
            val y = c.y + r * 0.2f - p * r * 1.1f
            val fw = (9 - 5 * p) * dp
            drawOval(BurnRed.copy(alpha = 1f - p), Offset(x - fw / 2, y - fw * 0.9f), Size(fw, fw * 1.8f))
            drawOval(BurnGold.copy(alpha = (1f - p) * 0.9f), Offset(x - fw / 4, y - fw * 0.3f), Size(fw / 2, fw))
        }
    }
    if (s.has(Status.PARALYZE)) {
        val locked = ms % Mechanics.PARALYZE_TWITCH_EVERY < Mechanics.PARALYZE_TWITCH_MS
        if (locked) drawCircle(ParaYellow.copy(alpha = 0.35f), r * 0.9f, c)
        val seed = (ms / 90).toInt()
        for (k in 0 until if (locked) 4 else 2) { // crackling zigzags that re-roll several times a second
            val a0 = ((seed * 37 + k * 91) % 360) / 360f * TAU
            var p = c + Offset(cos(a0), sin(a0)) * r * 0.9f
            val step = Offset(-cos(a0), -sin(a0)) * r * 0.22f
            for (j in 0..3) {
                val side = if ((j + k + seed) % 2 == 0) 1f else -1f
                val n = p + step + Offset(-step.y, step.x) * 0.8f * side
                drawLine(ParaYellow, p, n, 2.5f * dp, StrokeCap.Round)
                p = n
            }
        }
    }
    if (s.has(Status.LEECH)) { // seeds pulsing, life dripping out
        for (k in 0..2) {
            val a = t * 1.5f + k * TAU / 3
            val o = c + Offset(cos(a) * r * 0.7f, sin(a) * r * 0.45f)
            val pulse = 0.6f + 0.4f * sin(t * 6f + k)
            drawCircle(LeechGreen.copy(alpha = 0.9f), 4 * dp * pulse + 2 * dp, o)
            drawCircle(Color(0xFFE57373).copy(alpha = 0.7f), 2 * dp, o + Offset(0f, -8 * dp * ((t * 2 + k * 0.3f) % 1f)))
        }
    }
    if (s.has(Status.STUN)) { // stars circling the head
        val hy = c.y - r * 0.95f
        drawOval(StunGold.copy(alpha = 0.5f), Offset(c.x - r * 0.55f, hy - r * 0.12f), Size(r * 1.1f, r * 0.24f), style = Stroke(1.5f * dp))
        for (k in 0..2) {
            val a = t * 7f + k * TAU / 3
            val o = Offset(c.x + cos(a) * r * 0.55f, hy + sin(a) * r * 0.12f)
            star(o, 5 * dp, StunGold, a)
        }
    }
    if (s.has(Status.MARKED)) { // rotating, pulsing reticle locked on
        val pulse = 1f + 0.1f * sin(t * 8f)
        val rr = r * 0.95f * pulse
        rotate(t * 90f, c) {
            for (k in 0..3) drawArc(MarkRed.copy(alpha = 0.9f), k * 90f + 15f, 60f, false,
                Offset(c.x - rr, c.y - rr), Size(rr * 2, rr * 2), style = Stroke(2.5f * dp, cap = StrokeCap.Round))
        }
        for (k in 0..3) {
            val a = k * TAU / 4
            drawLine(MarkRed, c + Offset(cos(a), sin(a)) * rr * 1.15f, c + Offset(cos(a), sin(a)) * rr * 0.8f, 2.5f * dp, StrokeCap.Round)
        }
    }
}

private fun DrawScope.star(c: Offset, size: Float, col: Color, spin: Float) {
    for (k in 0..3) {
        val a = spin + k * TAU / 8 * 2
        drawLine(col, c - Offset(cos(a), sin(a)) * size, c + Offset(cos(a), sin(a)) * size, size * 0.45f, StrokeCap.Round)
    }
    drawCircle(Color.White, size * 0.3f, c)
}

/** The heaviest debuff on [bits], for the alert colour and text. */
private fun headline(bits: Int): Pair<String, Color>? = when {
    bits and Status.STUN != 0 -> "STUNNED!" to StunGold
    bits and Status.ROOT != 0 -> "ROOTED!" to RootGreen
    bits and Status.PARALYZE != 0 -> "PARALYZED!" to ParaYellow
    bits and Status.MARKED != 0 -> "MARKED!" to MarkRed
    bits and Status.BURN != 0 -> "BURNING!" to BurnRed
    bits and Status.SLOW != 0 -> "SLOWED!" to SlowBlue
    bits and Status.WEAKEN != 0 -> "WEAKENED!" to WeakGrey
    bits and Status.LEECH != 0 -> "SEEDED!" to LeechGreen
    else -> null
}

/**
 * Makes debuffs on your own pet impossible to miss: a pulsing coloured vignette for as long as one
 * lasts, a big call-out and a buzz the moment a new one lands.
 */
@Composable
internal fun BoxScope.DebuffAlert(me: FighterSnap?, ms: Long) {
    val bits = if (me == null || me.hp <= 0) 0 else me.status and DEBUFFS
    val haptic = LocalHapticFeedback.current
    var prev by remember { mutableIntStateOf(0) }
    var callout by remember { mutableStateOf<Pair<String, Color>?>(null) }
    val pop = remember { Animatable(1f) }
    LaunchedEffect(bits) {
        val fresh = bits and prev.inv()
        prev = bits
        if (fresh != 0) {
            callout = headline(fresh)
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            pop.snapTo(1.5f)
            pop.animateTo(1f, tween(220))
        }
        kotlinx.coroutines.delay(1000)
        callout = null
    }
    val main = headline(bits)
    if (main != null) Canvas(Modifier.matchParentSize()) {
        val a = 0.28f + 0.12f * sin(ms / 160f)
        val edge = size.minDimension * 0.22f
        val col = main.second.copy(alpha = a)
        drawRect(Brush.verticalGradient(listOf(col, Color.Transparent), 0f, edge))
        drawRect(Brush.verticalGradient(listOf(Color.Transparent, col), size.height - edge, size.height))
        drawRect(Brush.horizontalGradient(listOf(col, Color.Transparent), 0f, edge))
        drawRect(Brush.horizontalGradient(listOf(Color.Transparent, col), size.width - edge, size.width))
    }
    callout?.let { (text, col) ->
        Box(Modifier.align(Alignment.TopCenter).padding(top = 28.dp)) {
            Text(
                text,
                style = MaterialTheme.typography.headlineSmall.copy(
                    fontWeight = FontWeight.Black,
                    shadow = Shadow(Color.Black.copy(alpha = 0.7f), Offset(0f, 3f), 6f),
                ),
                color = col,
                modifier = Modifier.graphicsLayer { scaleX = pop.value; scaleY = pop.value },
            )
        }
    }
}
