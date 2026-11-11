package com.petpal.app.domain.battle

import kotlin.math.acos
import kotlin.math.asin
import kotlin.math.hypot

/** Every pet's hurtbox is a circle of this radius (arena units) around its center. */
const val PET_RADIUS = 0.07f

/** Projectiles travel this many arena units per second. */
const val PROJECTILE_SPEED = 1.6f

/**
 * How a move's hitbox is shaped. Moves are aimed at where the target stood when the move
 * was started, so a pet that drifts away during the wind-up (or while a projectile flies) dodges.
 */
enum class HitShape {
    SELF,       // buffs, no hitbox
    MELEE,      // circle just in front of the user
    PROJECTILE, // small ball that flies along the aim line and stops on the first hit (explosive ones blow up in a circle)
    BEAM,       // instant thick line along the aim
    CONE,       // breath / wave spreading from the user
    STRIKE,     // lands on the aimed spot from above (lightning)
}

private val BEAMS = setOf(Move.HYPER_BEAM, Move.ZAP_CANNON, Move.SUN_RAY, Move.SOLAR_BEAM, Move.SOLAR_FLARE, Move.OVERHEAT, Move.PRISM_BLAST)
/** Fireballs that explode on impact, hitting every enemy within [BLAST_RADIUS]. */
private val EXPLOSIVE = setOf(Move.INFERNO)
const val BLAST_RADIUS = 0.14f
val Move.explodes: Boolean get() = this in EXPLOSIVE

private val WAVES = setOf(Move.SPLASH_WAVE, Move.TIDAL_WAVE, Move.TSUNAMI, Move.ECHO_SHRIEK)

val Move.shape: HitShape
    get() = when {
        isBuff -> HitShape.SELF
        lunges || effect == Effect.VANISH -> HitShape.MELEE
        range < 0.3f -> HitShape.MELEE
        explodes -> HitShape.PROJECTILE
        this in BEAMS -> HitShape.BEAM
        this in WAVES -> HitShape.CONE
        element == Element.ELECTRIC -> HitShape.STRIKE
        damage == 0 -> HitShape.CONE // growls, glares, smoke: a spreading wave
        category != MoveCategory.NORMAL && (element == Element.FIRE || element == Element.WATER ||
            element == Element.ICE || element == Element.FLYING) -> HitShape.CONE
        else -> HitShape.PROJECTILE
    }

/** Half-width of a projectile / beam, radius of a melee or strike circle, in arena units. */
fun Move.hitSize(range: Float): Float = when (shape) {
    HitShape.SELF -> 0f
    HitShape.MELEE -> range * 0.4f // centered half-way out, so it still reaches ~range but is narrower
    HitShape.PROJECTILE -> if (explodes) 0.055f else if (category == MoveCategory.NORMAL) 0.03f else 0.04f
    HitShape.BEAM -> if (category == MoveCategory.UNIQUE) 0.06f else 0.045f
    HitShape.STRIKE -> if (category == MoveCategory.NORMAL) 0.06f else 0.08f
    HitShape.CONE -> 0f
}

/** Half-angle of a cone in radians. */
fun Move.coneHalfAngle(): Float = when {
    this in WAVES -> 0.6f
    damage == 0 -> 0.45f
    category == MoveCategory.UNIQUE -> 0.5f
    else -> 0.38f
}

/** Unit vector from (ox, oy) toward (ax, ay); falls back to [facing] if they overlap. */
fun aimDir(ox: Float, oy: Float, ax: Float, ay: Float, facing: Int): Pair<Float, Float> {
    val dx = ax - ox
    val dy = ay - oy
    val d = hypot(dx, dy)
    return if (d < 1e-4f) facing.toFloat() to 0f else dx / d to dy / d
}

/** Where a melee hit circle or strike circle is centered. */
fun Move.hitCenter(ox: Float, oy: Float, dx: Float, dy: Float, ax: Float, ay: Float, range: Float): Pair<Float, Float> =
    when (shape) {
        HitShape.STRIKE -> { // the aimed spot, but never further than the move reaches
            val d = hypot(ax - ox, ay - oy).coerceAtMost(range)
            ox + dx * d to oy + dy * d
        }
        else -> ox + dx * range * 0.5f to oy + dy * range * 0.5f
    }

/** Instant hit test (everything but projectiles, which are tested per tick while flying). */
fun Move.hits(
    ox: Float, oy: Float, // attacker
    ax: Float, ay: Float, // aim point
    facing: Int,
    tx: Float, ty: Float, // target center
    range: Float,
): Boolean {
    val (dx, dy) = aimDir(ox, oy, ax, ay, facing)
    return when (shape) {
        HitShape.SELF -> false
        HitShape.MELEE, HitShape.STRIKE -> {
            val (cx, cy) = hitCenter(ox, oy, dx, dy, ax, ay, range)
            hypot(tx - cx, ty - cy) <= hitSize(range) + PET_RADIUS
        }
        HitShape.BEAM, HitShape.PROJECTILE ->
            segmentDistance(tx, ty, ox, oy, ox + dx * range, oy + dy * range) <= hitSize(range) + PET_RADIUS
        HitShape.CONE -> {
            val px = tx - ox
            val py = ty - oy
            val d = hypot(px, py)
            when {
                d <= PET_RADIUS -> true
                d > range + PET_RADIUS -> false
                else -> {
                    val cos = ((px * dx + py * dy) / d).coerceIn(-1f, 1f)
                    acos(cos) <= coneHalfAngle() + asin((PET_RADIUS / d).coerceAtMost(1f))
                }
            }
        }
    }
}

fun segmentDistance(px: Float, py: Float, ax: Float, ay: Float, bx: Float, by: Float): Float {
    val vx = bx - ax
    val vy = by - ay
    val len2 = vx * vx + vy * vy
    val t = if (len2 < 1e-8f) 0f else (((px - ax) * vx + (py - ay) * vy) / len2).coerceIn(0f, 1f)
    return hypot(px - (ax + vx * t), py - (ay + vy * t))
}
