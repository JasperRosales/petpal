package com.petpal.app.ui.battle

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotateRad
import androidx.compose.ui.unit.dp
import com.petpal.app.domain.battle.Effect
import com.petpal.app.domain.battle.Element
import com.petpal.app.domain.battle.Move
import com.petpal.app.domain.battle.MoveCategory
import com.petpal.app.ui.theme.EmberGold
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin

/** Big hand-drawn set pieces that only unique moves get, on top of their particles. */
internal enum class Piece {
    VORTEX, PILLAR, METEOR, SHOCKWAVE, CROSS_SLASH, DOME, STARBURST, LIGHTNING, BLOOM,
    CRESCENT, GEYSER, CRYSTAL, RUNE, RIBBON, JAWS, HALO,
}

/** A unique move's look: a main set piece, a smaller second one, and how many parts each has. */
internal data class Signature(val main: Piece, val accent: Piece, val parts: Int, val spin: Int)

private val PI2 = (2 * PI).toFloat()
private val PIf = PI.toFloat()

/** The set piece a unique move's name, effect or type calls for. */
private fun Move.preferredPiece(): Piece {
    val w = label.lowercase()
    fun has(vararg k: String) = k.any { it in w }
    return when {
        effect in setOf(Effect.SHIELD, Effect.HARDEN, Effect.FORTRESS, Effect.CITADEL, Effect.GUARD, Effect.COUNTER) -> Piece.DOME
        effect in setOf(Effect.HEAL, Effect.AID, Effect.PACK_CALL, Effect.ALPHA, Effect.BATTLE_CRY) -> Piece.HALO
        has("moon", "lunar", "crescent", "night", "midnight") -> Piece.CRESCENT
        has("fang", "jaw", "bite", "maul", "snatch", "frenzy", "chomp") -> Piece.JAWS
        has("claw", "swipe", "slash", "slice", "talon", "lash", "cut") -> Piece.CROSS_SLASH
        has("eye", "glare", "mirage", "dream", "hex", "curse", "rune", "pollen", "spell") -> Piece.RUNE
        has("dive", "meteor", "comet", "phoenix", "toss", "drop", "fall") -> Piece.METEOR
        has("bloom", "lotus", "petal", "flower", "blossom", "garden") -> Piece.BLOOM
        has("blizzard", "frost", "polar", "arctic", "floe", "crystal", "glacier", "ice") -> Piece.CRYSTAL
        has("wave", "tide", "tsunami", "torrent", "current", "geyser", "splash", "spout") -> Piece.GEYSER
        has("storm", "tempest", "thunder", "volt", "spark", "static", "lightning", "zap") -> Piece.LIGHTNING
        has("quake", "stomp", "crash", "slam", "tremor", "smash", "rampage") -> Piece.SHOCKWAVE
        has("sun", "solar", "ray", "beam", "prism", "flare", "halo") -> Piece.HALO
        has("wind", "gust", "tornado", "cyclone", "spin", "wheel", "roll", "whirl") -> Piece.VORTEX
        has("dash", "charge", "stampede", "rush", "swing", "slide", "dart", "spit") -> Piece.RIBBON
        has("inferno", "fire", "flame", "blaze", "volcano", "magma", "burst", "wisp") -> Piece.PILLAR
        else -> when (element) {
            Element.FIRE -> Piece.PILLAR
            Element.WATER -> Piece.GEYSER
            Element.GRASS -> Piece.BLOOM
            Element.ICE -> Piece.CRYSTAL
            Element.ELECTRIC -> Piece.LIGHTNING
            Element.FLYING -> Piece.VORTEX
            Element.GROUND -> Piece.SHOCKWAVE
            Element.FIGHTING -> Piece.STARBURST
            Element.NORMAL -> Piece.STARBURST
        }
    }
}

/** Every unique move gets its own signature, so no two of them look alike. Assigned once, in move order. */
private val signatures: Map<Move, Signature> by lazy {
    val used = HashSet<Triple<Piece, Piece, Int>>()
    val out = HashMap<Move, Signature>()
    val pieces = Piece.entries
    Move.entries.filter { it.category == MoveCategory.UNIQUE }.forEach { m ->
        val main = m.preferredPiece()
        val h = m.variant(9973)
        val accents = pieces.indices.map { pieces[(h + it * 7) % pieces.size] }.filter { it != main }.distinct()
        val counts = (0 until 6).map { 3 + (h / 3 + it) % 6 }
        val pick = counts.firstNotNullOfOrNull { n -> accents.firstOrNull { Triple(main, it, n) !in used }?.let { it to n } }
            ?: (accents.first() to 3 + h % 6)
        used += Triple(main, pick.first, pick.second)
        out[m] = Signature(main, pick.first, pick.second, if (h % 2 == 0) 1 else -1)
    }
    out
}

internal val Move.signature: Signature? get() = signatures[this]

private fun srnd(seed: Int): Float {
    var x = seed * 0x27d4eb2d + 0x165667b1
    x = x xor (x ushr 15)
    x *= 0x2c1b3c6d
    x = x xor (x ushr 12)
    return (x and 0xFFFF) / 65535f
}

private fun ease(t: Float) = 1f - (1f - t).let { it * it * it }
private fun bell(t: Float) = sin(PIf * t.coerceIn(0f, 1f))

/** How long a unique's set piece plays after the hit, in ms. */
private const val UNIQUE_FX_MS = 700f

/**
 * Draws [m]'s set pieces. During the wind-up ([after] < 0) a golden aura gathers on the user so
 * every unique is announced; after the hit the main piece plays at the impact with its accent.
 */
internal fun DrawScope.drawUniqueFx(m: Move, origin: Offset, impact: Offset, after: Long) {
    val sig = m.signature ?: return
    val R = 40.dp.toPx()
    val col = m.tint()
    val acc = m.coreTint()
    if (after < 0) {
        val prog = ((m.windupMs + after) / m.windupMs.toFloat().coerceAtLeast(1f)).coerceIn(0f, 1f)
        drawCircle(Brush.radialGradient(listOf(EmberGold.copy(alpha = 0.35f * prog), col.copy(alpha = 0.2f * prog), Color.Transparent),
            origin, R * 1.2f), R * 1.2f, origin)
        for (i in 0 until sig.parts) {
            val a = PI2 * i / sig.parts + prog * 3f * sig.spin
            drawCircle(EmberGold.copy(alpha = prog), 2.5.dp.toPx(), origin + Offset(cos(a), sin(a) * 0.45f) * R * (1.1f - 0.4f * prog))
        }
        return
    }
    val t = after / UNIQUE_FX_MS
    if (t > 1f) return
    piece(sig.main, origin, impact, t, R, col, acc, sig.parts, sig.spin, m.ordinal)
    val at = (t - 0.15f) / 0.85f
    if (at > 0f) piece(sig.accent, origin, impact, at, R * 0.65f, acc, col, sig.parts, -sig.spin, m.ordinal + 1)
}

private fun DrawScope.piece(k: Piece, o: Offset, c: Offset, t: Float, R: Float, col: Color, acc: Color, n: Int, spin: Int, seed: Int) {
    val fade = 1f - t
    val e = ease(minOf(1f, t * 1.6f))
    val vx = c.x - o.x
    val vy = c.y - o.y
    val len = hypot(vx, vy)
    val aim = if (len < 1f) 0f else atan2(vy, vx)
    val dp = 1.dp.toPx()
    val white = Color.White
    fun a(cl: Color, x: Float) = cl.copy(alpha = (cl.alpha * x).coerceIn(0f, 1f))

    when (k) {
        Piece.VORTEX -> {
            for (arm in 0 until n) {
                val p = Path()
                for (j in 0..14) {
                    val f = j / 14f
                    val ang = PI2 * arm / n + spin * (t * 7f + f * 2.4f)
                    val r = R * 1.4f * f * e
                    val pt = c + Offset(cos(ang), sin(ang) * 0.6f) * r
                    if (j == 0) p.moveTo(pt.x, pt.y) else p.lineTo(pt.x, pt.y)
                }
                drawPath(p, a(col, fade), style = Stroke(5 * dp * fade + dp, cap = StrokeCap.Round))
                drawPath(p, a(white, 0.6f * fade), style = Stroke(1.5f * dp, cap = StrokeCap.Round))
            }
            drawCircle(a(acc, 0.7f * fade), R * 0.25f * e, c)
        }
        Piece.PILLAR -> {
            val h = R * 3.2f * minOf(1f, t * 3f)
            val w = R * 0.55f * bell(t * 0.9f + 0.1f)
            if (w > 0.5f && h > 0.5f) {
                val top = c.y - h
                drawRect(Brush.verticalGradient(listOf(Color.Transparent, a(col, 0.85f * fade), a(acc, fade)), top, c.y + R * 0.3f),
                    Offset(c.x - w, top), Size(w * 2, h + R * 0.3f))
                drawRect(a(white, 0.7f * fade), Offset(c.x - w * 0.3f, top + h * 0.2f), Size(w * 0.6f, h * 0.8f + R * 0.3f))
                for (i in 0 until n) {
                    val f = (t * 2f + i / n.toFloat()) % 1f
                    val y = c.y - h * f
                    drawOval(a(acc, fade * (1 - f)), Offset(c.x - w * 1.4f, y - w * 0.25f), Size(w * 2.8f, w * 0.5f), style = Stroke(2 * dp))
                }
            }
            drawOval(a(col, 0.5f * fade), Offset(c.x - R * 1.2f * e, c.y + R * 0.3f - R * 0.25f * e), Size(R * 2.4f * e, R * 0.5f * e))
        }
        Piece.METEOR -> {
            val fall = minOf(1f, t / 0.4f)
            if (t < 0.4f) {
                if (fall < 0.05f) return
                val from = c + Offset(-R * 2.4f * spin, -R * 3.2f)
                val head = from + (c - from) * (fall * fall)
                val tail = from + (c - from) * maxOf(0f, fall * fall - 0.35f)
                drawLine(Brush.linearGradient(listOf(Color.Transparent, a(col, 0.9f)), tail, head), tail, head, R * 0.35f, StrokeCap.Round)
                drawCircle(Brush.radialGradient(listOf(white, acc, a(col, 0f)), head, R * 0.4f), R * 0.4f, head)
            } else {
                val k2 = (t - 0.4f) / 0.6f
                drawCircle(Brush.radialGradient(listOf(a(white, 1 - k2), a(acc, 1 - k2), Color.Transparent), c, R * (0.4f + 1.3f * k2)),
                    R * (0.4f + 1.3f * k2), c)
                for (i in 0 until n * 2) {
                    val ang = PI2 * i / (n * 2) + srnd(seed + i) * 0.5f
                    val d = R * (0.3f + 1.6f * ease(k2))
                    drawCircle(a(col, 1 - k2), R * 0.08f * (1.5f - k2), c + Offset(cos(ang), sin(ang) * 0.5f - 0.4f * bell(k2)) * d)
                }
            }
        }
        Piece.SHOCKWAVE -> {
            for (i in 0 until n) {
                val q = t * 1.4f - i * 0.12f
                if (q <= 0f || q >= 1f) continue
                val r = R * (0.2f + 2.1f * ease(q))
                drawOval(a(if (i % 2 == 0) col else acc, 1 - q), Offset(c.x - r, c.y - r * 0.5f), Size(r * 2, r), style = Stroke(8 * dp * (1 - q) + dp))
            }
            for (i in 0 until n * 2) {
                val ang = PI2 * i / (n * 2)
                val d0 = R * (0.3f + 0.6f * e)
                val d1 = d0 + R * 0.5f * fade
                drawLine(a(acc, fade), c + Offset(cos(ang), sin(ang) * 0.5f) * d0, c + Offset(cos(ang), sin(ang) * 0.5f) * d1, 3 * dp, StrokeCap.Round)
            }
        }
        Piece.CROSS_SLASH -> {
            for (i in 0 until n) {
                val q = (t - i * 0.08f) * 2.2f
                if (q <= 0f || q >= 1.6f) continue
                val draw = minOf(1f, q)
                val gone = ((q - 1f) / 0.6f).coerceIn(0f, 1f)
                val ang = aim + PIf / 4 + spin * i * PIf / n
                val d = Offset(cos(ang), sin(ang)) * R * 1.4f
                val nrm = Offset(-d.y, d.x) * 0.15f
                val p0 = c - d
                val p = Path().apply {
                    moveTo(p0.x, p0.y)
                    val end = p0 + d * 2f * draw
                    quadraticTo(c.x + nrm.x, c.y + nrm.y, end.x, end.y)
                }
                drawPath(p, a(col, 1 - gone), style = Stroke(9 * dp * (1 - gone) + dp, cap = StrokeCap.Round))
                drawPath(p, a(white, 1 - gone), style = Stroke(2.5f * dp, cap = StrokeCap.Round))
            }
        }
        Piece.DOME -> {
            val p = c
            val r = R * 1.5f * e
            if (r > 1f) {
                drawArc(a(col, 0.18f * fade), 180f, 180f, true, Offset(p.x - r, p.y - r), Size(r * 2, r * 2))
                drawArc(a(col, fade), 180f, 180f, false, Offset(p.x - r, p.y - r), Size(r * 2, r * 2), style = Stroke(3 * dp))
                for (i in 1 until n) {
                    val w = r * i / n
                    drawArc(a(acc, 0.6f * fade), 180f, 180f, false, Offset(p.x - w, p.y - r), Size(w * 2, r * 2), style = Stroke(1.5f * dp))
                }
                val band = (t * 2f) % 1f
                val y = p.y - r * band
                val hw = r * kotlin.math.sqrt((1 - band * band).coerceAtLeast(0f))
                drawLine(a(white, 0.8f * fade), Offset(p.x - hw, y), Offset(p.x + hw, y), 2.5f * dp)
                drawOval(a(col, 0.6f * fade), Offset(p.x - r, p.y - r * 0.15f), Size(r * 2, r * 0.3f), style = Stroke(2 * dp))
            }
        }
        Piece.STARBURST -> {
            rotateRad(spin * t * 0.8f, c) {
                for (i in 0 until n * 2) {
                    val ang = PI2 * i / (n * 2)
                    val l = R * (0.4f + (if (i % 2 == 0) 2.2f else 1.3f) * e)
                    val dir = Offset(cos(ang), sin(ang))
                    val side = Offset(-dir.y, dir.x) * R * 0.1f
                    val p = Path().apply {
                        moveTo(c.x + side.x, c.y + side.y)
                        lineTo(c.x + dir.x * l, c.y + dir.y * l)
                        lineTo(c.x - side.x, c.y - side.y)
                        close()
                    }
                    drawPath(p, a(if (i % 2 == 0) col else acc, fade))
                }
            }
            drawCircle(Brush.radialGradient(listOf(a(white, fade), a(acc, 0.5f * fade), Color.Transparent), c, R * 0.6f), R * 0.6f, c)
        }
        Piece.LIGHTNING -> {
            val from = if (len < 1f) c - Offset(0f, R * 3f) else o
            val flick = (t * 14f).toInt()
            for (i in 0 until n) {
                val q = (t * 1.5f - i * 0.06f)
                if (q <= 0f || q >= 1f) continue
                val end = c + Offset((srnd(seed + i * 3) - 0.5f) * R, (srnd(seed + i * 5) - 0.5f) * R * 0.6f)
                val dx = end - from
                val nrm = Offset(-dx.y, dx.x) / (hypot(dx.x, dx.y).coerceAtLeast(1f))
                val p = Path().apply {
                    moveTo(from.x, from.y)
                    for (s in 1..8) {
                        val f = s / 8f
                        val j = if (s == 8) 0f else (srnd(seed + i * 31 + s * 7 + flick * 101) - 0.5f) * R * 0.7f
                        val pt = from + dx * f + nrm * j
                        lineTo(pt.x, pt.y)
                    }
                }
                val al = (1 - q) * (if (flick % 2 == 0) 1f else 0.6f)
                drawPath(p, a(col, al), style = Stroke(5 * dp, cap = StrokeCap.Round, join = StrokeJoin.Round))
                drawPath(p, a(white, al), style = Stroke(1.5f * dp, cap = StrokeCap.Round, join = StrokeJoin.Round))
            }
            drawCircle(a(acc, 0.6f * fade), R * 0.5f * e, c, style = Stroke(3 * dp))
        }
        Piece.BLOOM -> {
            for (ring in 0..1) {
                val count = n + ring * 2
                val rr = R * (0.55f - ring * 0.2f) * e
                for (i in 0 until count) {
                    val ang = PI2 * i / count + spin * t * 1.5f + ring * 0.4f
                    val pc = c + Offset(cos(ang), sin(ang)) * rr
                    rotateRad(ang, pc) {
                        val pw = R * (0.55f - ring * 0.15f) * e
                        drawOval(a(if (ring == 0) col else acc, fade), Offset(pc.x - pw / 2, pc.y - pw / 5), Size(pw, pw / 2.5f))
                    }
                }
            }
            drawCircle(a(EmberGold, fade), R * 0.18f * e, c)
        }
        Piece.CRESCENT -> {
            val r = R * 1.3f * e
            if (r > 1f) {
                rotateRad(-1.2f * spin + 2.4f * spin * ease(t), c) {
                    val outer = Path().apply { addOval(androidx.compose.ui.geometry.Rect(c, r)) }
                    val cut = Path().apply { addOval(androidx.compose.ui.geometry.Rect(c + Offset(r * 0.35f, -r * 0.1f), r * 0.9f)) }
                    val moon = Path().apply { op(outer, cut, PathOperation.Difference) }
                    drawPath(moon, a(col, 0.85f * fade))
                    drawPath(moon, a(white, 0.8f * fade), style = Stroke(2 * dp))
                }
            }
            for (i in 0 until n) {
                val ang = PI2 * i / n + spin * t * 4f
                drawCircle(a(acc, fade), R * 0.07f, c + Offset(cos(ang), sin(ang) * 0.6f) * R * 1.6f * e)
            }
        }
        Piece.GEYSER -> {
            val base = c + Offset(0f, R * 0.4f)
            drawOval(a(acc, 0.6f * fade), Offset(base.x - R * 1.1f * e, base.y - R * 0.2f), Size(R * 2.2f * e, R * 0.4f))
            for (i in 0 until n) {
                val spread = (i - (n - 1) / 2f) / n
                for (j in 0 until 7) {
                    val q = t * 1.3f - j * 0.05f
                    if (q <= 0f || q >= 1f) continue
                    val x = base.x + spread * R * 2.2f * q
                    val y = base.y - R * 3.4f * q + R * 3.2f * q * q
                    drawCircle(a(if (j % 2 == 0) col else white, 1 - q), R * (0.16f - j * 0.015f), Offset(x, y))
                }
            }
        }
        Piece.CRYSTAL -> {
            for (i in 0 until n) {
                val ang = PI2 * i / n + 0.3f * spin
                val base = c + Offset(cos(ang) * R * 1.1f, sin(ang) * R * 0.35f + R * 0.3f)
                if (t < 0.6f) {
                    val g = ease(t / 0.6f)
                    val h = R * (0.9f + 0.4f * srnd(seed + i)) * g
                    val w = R * 0.22f
                    val p = Path().apply {
                        moveTo(base.x, base.y - h); lineTo(base.x + w, base.y - h * 0.35f)
                        lineTo(base.x, base.y); lineTo(base.x - w, base.y - h * 0.35f); close()
                    }
                    drawPath(p, a(col, 0.85f))
                    drawPath(p, a(white, 0.9f), style = Stroke(1.5f * dp))
                } else {
                    val q = (t - 0.6f) / 0.4f
                    for (s in 0..2) {
                        val sa = ang + (s - 1) * 0.8f - PIf / 2
                        val sp = base + Offset(cos(sa), sin(sa)) * R * 0.9f * q - Offset(0f, R * 0.5f)
                        drawCircle(a(acc, 1 - q), R * 0.07f, sp)
                    }
                }
            }
        }
        Piece.RUNE -> {
            val g = c + Offset(0f, R * 0.35f)
            val r = R * 1.3f * e
            val al = bell(t)
            if (r > 1f) {
                drawOval(a(col, al), Offset(g.x - r, g.y - r * 0.4f), Size(r * 2, r * 0.8f), style = Stroke(2.5f * dp))
                drawOval(a(acc, al), Offset(g.x - r * 0.8f, g.y - r * 0.32f), Size(r * 1.6f, r * 0.64f), style = Stroke(1.5f * dp))
                val pts = maxOf(n, 5)
                val step = if (pts % 2 == 1) 2 else 1
                val rot = spin * t * 2.5f
                val p = Path()
                for (i in 0..pts) {
                    val ang = rot + PI2 * ((i * step) % pts) / pts
                    val pt = g + Offset(cos(ang) * r * 0.8f, sin(ang) * r * 0.32f)
                    if (i == 0) p.moveTo(pt.x, pt.y) else p.lineTo(pt.x, pt.y)
                }
                drawPath(p, a(col, al), style = Stroke(2 * dp))
                if (t > 0.5f) for (i in 0 until pts) {
                    val ang = rot + PI2 * i / pts
                    val pt = g + Offset(cos(ang) * r * 0.8f, sin(ang) * r * 0.32f)
                    val h = R * 1.5f * (t - 0.5f) * 2f
                    drawLine(a(acc, fade * 1.5f), pt, pt - Offset(0f, h), 3 * dp, StrokeCap.Round)
                }
            }
        }
        Piece.RIBBON -> {
            val head = minOf(1f, t * 2f)
            val tail = maxOf(0f, t * 2f - 1f)
            val p = Path()
            val steps = 24
            var first = true
            for (s in 0..steps) {
                val f = tail + (head - tail) * s / steps
                val pt = if (len < 1f) {
                    val ang = f * PI2 * 1.5f * spin
                    c + Offset(cos(ang) * R * 1.2f, sin(ang) * R * 0.4f - f * R * 1.2f)
                } else {
                    val wob = sin(f * PIf * n) * R * 0.45f
                    o + (c - o) * f + Offset(-vy / len, vx / len) * (wob * spin)
                }
                if (first) { p.moveTo(pt.x, pt.y); first = false } else p.lineTo(pt.x, pt.y)
            }
            drawPath(p, a(col, 0.9f), style = Stroke(R * 0.22f, cap = StrokeCap.Round, join = StrokeJoin.Round))
            drawPath(p, a(white, 0.8f), style = Stroke(R * 0.06f, cap = StrokeCap.Round, join = StrokeJoin.Round))
            if (t > 0.45f) {
                val q = (t - 0.45f) / 0.55f
                drawCircle(a(acc, 1 - q), R * (0.3f + 0.8f * q), c, style = Stroke(4 * dp * (1 - q) + dp))
            }
        }
        Piece.JAWS -> {
            val close = ease(minOf(1f, t * 2.8f))
            val gap = R * 1.1f * (1 - close) + R * 0.05f
            val w = R * 2.2f
            val tw = w / n
            for (row in 0..1) {
                val sgn = if (row == 0) -1f else 1f
                val y = c.y + sgn * gap
                rotateRad(aim * 0.2f, c) {
                    val gum = Path()
                    for (i in 0 until n) {
                        val x0 = c.x - w / 2 + i * tw
                        gum.moveTo(x0, y)
                        gum.lineTo(x0 + tw / 2, y - sgn * R * 0.55f)
                        gum.lineTo(x0 + tw, y)
                        gum.close()
                    }
                    drawPath(gum, a(white, fade + 0.2f))
                    drawPath(gum, a(col, fade + 0.2f), style = Stroke(2 * dp))
                }
            }
            if (close >= 1f) {
                val q = ((t - 1f / 2.8f) / (1f - 1f / 2.8f)).coerceIn(0f, 1f)
                for (i in 0 until n) {
                    val ang = PI2 * i / n
                    drawLine(a(col, 1 - q), c, c + Offset(cos(ang), sin(ang)) * R * (0.5f + q), 3 * dp, StrokeCap.Round)
                }
            }
        }
        Piece.HALO -> {
            val top = c - Offset(0f, R * 1.7f)
            val r = R * 0.9f * e
            if (r > 1f) {
                drawOval(a(EmberGold, fade), Offset(top.x - r, top.y - r * 0.3f), Size(r * 2, r * 0.6f), style = Stroke(4 * dp))
                for (i in 0 until n) {
                    val ang = PI2 * i / n + spin * t * 5f
                    val pt = top + Offset(cos(ang) * r, sin(ang) * r * 0.3f)
                    drawCircle(a(acc, fade), R * 0.08f, pt)
                    val beamW = R * 0.12f
                    drawRect(Brush.verticalGradient(listOf(a(col, 0.55f * fade), Color.Transparent), pt.y, c.y + R * 0.3f),
                        Offset(pt.x - beamW / 2, pt.y), Size(beamW, (c.y + R * 0.3f - pt.y).coerceAtLeast(1f)))
                }
            }
            drawCircle(Brush.radialGradient(listOf(a(white, 0.6f * fade), Color.Transparent), c, R * 0.7f), R * 0.7f, c)
        }
    }
}
