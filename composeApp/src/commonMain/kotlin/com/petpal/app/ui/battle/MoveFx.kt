package com.petpal.app.ui.battle

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotateRad
import androidx.compose.ui.unit.dp
import com.petpal.app.domain.battle.Effect
import com.petpal.app.domain.battle.Element
import com.petpal.app.domain.battle.Move
import com.petpal.app.ui.battle.Glyph.*
import com.petpal.app.ui.battle.Motion.*
import com.petpal.app.ui.theme.EmberGold
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin

/** Particle shapes a move's effect is built from. */
internal enum class Glyph {
    EMBER, FLAME, BUBBLE, DROP, WAVE, LEAF, PETAL, SEED, SPORE, VINE, SNOW, SHARD, SPARK, BOLT,
    FEATHER, WIND, ROCK, SAND, MUD, CRACK, BONE, FANG, CLAW, FIST, PAW, STAR, MOON, SUN, SHADOW,
    RING, SMOKE, HEART, PLUS, ARROW, NOTE, EYE, SHIELD,
}

/** How the particles of a layer move over the effect's lifetime. */
internal enum class Motion { BURST, TRAIL, RAIN, RISE, SPIRAL, CONVERGE, ORBIT, STAMP, SWEEP, GROUND }

internal class Layer(val glyph: Glyph, val motion: Motion, val count: Int, val size: Float = 1f, val color: Color? = null)

private fun L(g: Glyph, m: Motion, n: Int, size: Float = 1f, color: Color? = null) = Layer(g, m, n, size, color)

private val Pink = Color(0xFFF48FB1)
private val Lavender = Color(0xFFB39DDB)
private val Snow = Color(0xFFE8F8FF)
private val Stone = Color(0xFF9E9384)
private val Dirt = Color(0xFF8B6B4A)
private val Bone = Color(0xFFF2EAD8)
private val Blue = Color(0xFF6C8CFF)
private val Gold = Color(0xFFFFD54F)
private val Mist = Color(0xFFDDE6EA)
private val Leaf = Color(0xFF5DBB63)
private val Shade = Color(0xFF4A2C7A)

/** Every move's own effect. Exhaustive so a new move can't ship without one. */
internal val Move.fx: List<Layer>
    get() = when (this) {
                Move.TACKLE -> listOf(L(SMOKE, BURST, 4), L(STAR, STAMP, 1))
                Move.GUARD -> listOf(L(SHIELD, STAMP, 1, 1.4f))
                Move.GROWL -> listOf(L(NOTE, TRAIL, 4, 1.3f))
                Move.PUNCH -> listOf(L(FIST, STAMP, 1))
                Move.CLAW -> listOf(L(Glyph.CLAW, STAMP, 1))
                Move.QUICK_ATTACK -> listOf(L(WIND, TRAIL, 5), L(STAR, STAMP, 1, 0.7f))
                Move.BITE -> listOf(L(FANG, STAMP, 1), L(HEART, CONVERGE, 3, 0.8f, Pink))
                Move.HOWL -> listOf(L(NOTE, BURST, 6, 1.2f), L(ARROW, RISE, 2))
                Move.FURY_SWIPES -> listOf(L(Glyph.CLAW, STAMP, 3, 0.8f))
                Move.POUNCE -> listOf(L(SAND, BURST, 8), L(Glyph.CLAW, STAMP, 1, 1.3f))
                Move.QUICK_JAB -> listOf(L(FIST, STAMP, 2, 0.7f), L(WIND, BURST, 3, 0.6f))
                Move.HEADBUTT -> listOf(L(STAR, SPIRAL, 3, 1.1f, Gold), L(RING, STAMP, 1))
                Move.GLARE -> listOf(L(EYE, STAMP, 1, 1.4f), L(SPARK, BURST, 4))
                Move.ZOOMIES -> listOf(L(STAR, ORBIT, 4), L(WIND, BURST, 4))
                Move.COUNTER -> listOf(L(SHIELD, STAMP, 1, 1.2f, Color.White), L(SPARK, ORBIT, 3))
                Move.HEAL -> listOf(L(PLUS, RISE, 5))
                Move.DOUBLE_EDGE -> listOf(L(STAR, BURST, 5), L(SPARK, BURST, 4, 1f, Color.White))
                Move.BODY_SLAM -> listOf(L(SMOKE, GROUND, 6), L(STAR, STAMP, 1, 1.4f))
                Move.HARDEN -> listOf(L(SHIELD, STAMP, 1, 1.6f), L(SHARD, ORBIT, 4, 1f, Stone))
                Move.TELEPORT -> listOf(L(STAR, SPIRAL, 6, 0.8f, Lavender), L(RING, STAMP, 2))
                Move.DOUBLE_KICK -> listOf(L(STAR, STAMP, 2), L(WIND, BURST, 4))
                Move.FOCUS_ENERGY -> listOf(L(STAR, CONVERGE, 8, 0.8f, Gold))
                Move.FINISHING_BLOW -> listOf(L(Glyph.CLAW, STAMP, 1, 1.5f), L(STAR, BURST, 6, 1f, Color(0xFFD9534F)))
                Move.REFRESH -> listOf(L(BUBBLE, RISE, 6, 1f, Mist))
                Move.BATTLE_CRY -> listOf(L(NOTE, BURST, 8, 1.3f), L(ARROW, RISE, 4))
                Move.HYPER_FANG -> listOf(L(FANG, STAMP, 1, 1.6f), L(HEART, CONVERGE, 5, 0.8f, Pink))
                Move.SECOND_WIND -> listOf(L(WIND, ORBIT, 5), L(PLUS, RISE, 3))
                Move.GIGA_IMPACT -> listOf(L(SMOKE, GROUND, 10, 1.4f), L(STAR, BURST, 8, 1.2f), L(RING, STAMP, 1, 2f))
                Move.LAST_STAND -> listOf(L(SHIELD, STAMP, 1, 2f), L(PLUS, RISE, 4), L(ARROW, RISE, 2))
                Move.HYPER_BEAM -> listOf(L(SPARK, TRAIL, 8), L(RING, STAMP, 2, 1.5f))
                Move.MEGA_PUNCH -> listOf(L(FIST, STAMP, 1, 2f), L(STAR, SPIRAL, 4, 1f, Gold))

                Move.KARATE_CHOP -> listOf(L(FIST, STAMP, 1, 1.1f), L(WIND, SWEEP, 3))
                Move.LOW_KICK -> listOf(L(STAR, STAMP, 1), L(SAND, GROUND, 5))
                Move.BULK_UP -> listOf(L(ARROW, RISE, 4, 1.2f, Color(0xFFC0503A)), L(FIST, ORBIT, 2))
                Move.BRICK_BREAK -> listOf(L(FIST, STAMP, 1, 1.3f), L(ROCK, BURST, 5, 0.8f, Stone))
                Move.DRAIN_PUNCH -> listOf(L(FIST, STAMP, 1, 1.2f), L(HEART, CONVERGE, 4, 0.8f, Pink))
                Move.CROSS_CHOP -> listOf(L(FIST, STAMP, 2, 1.2f), L(STAR, SPIRAL, 3, 1f, Gold))
                Move.DYNAMIC_PUNCH -> listOf(L(FIST, STAMP, 1, 2f), L(SPARK, BURST, 8, 1f, Gold), L(RING, STAMP, 1, 1.6f))
                Move.CLOSE_COMBAT -> listOf(L(FIST, BURST, 6, 1f), L(STAR, BURST, 6), L(SMOKE, GROUND, 4))
                Move.FOCUS_BLAST -> listOf(L(SUN, TRAIL, 3, 1.4f, Color(0xFFFFB74D)), L(RING, STAMP, 2, 1.8f))
                Move.THUNDER_PUNCH -> listOf(L(FIST, STAMP, 1, 1.2f, Gold), L(BOLT, BURST, 5))
                Move.FIRE_PUNCH -> listOf(L(FIST, STAMP, 1, 1.2f, EmberGold), L(FLAME, BURST, 5))
                Move.ROCK_SMASH -> listOf(L(FIST, STAMP, 1, 1.2f), L(ROCK, BURST, 6, 1f, Stone))
                Move.SKY_UPPERCUT -> listOf(L(FIST, RISE, 1, 1.3f), L(WIND, RISE, 5))
                Move.AQUA_FIST -> listOf(L(FIST, STAMP, 1, 1.2f, Color(0xFFBFE6FF)), L(DROP, BURST, 6))

                Move.EMBER -> listOf(L(Glyph.EMBER, TRAIL, 5))
                Move.SMOKESCREEN -> listOf(L(SMOKE, BURST, 8, 1.4f, Color(0xFF8A8A8A)))
                Move.FIRE_FANG -> listOf(L(FANG, STAMP, 1), L(FLAME, RISE, 4))
                Move.FLAME_DASH -> listOf(L(FLAME, TRAIL, 7))
                Move.INCINERATE -> listOf(L(FLAME, BURST, 6), L(SPARK, CONVERGE, 4, 1f, Gold))
                Move.HEAT_UP -> listOf(L(FLAME, ORBIT, 5))
                Move.FIRE_SPIN -> listOf(L(FLAME, SPIRAL, 8))
                Move.FLAME_WHEEL -> listOf(L(Glyph.EMBER, SPIRAL, 10, 0.8f), L(RING, STAMP, 1))
                Move.WILL_O_WISP -> listOf(L(Glyph.EMBER, SPIRAL, 3, 1.6f, Blue))
                Move.FIRE_BREATH -> listOf(L(FLAME, TRAIL, 10), L(SMOKE, RISE, 4))
                Move.FIREBALL -> listOf(L(Glyph.EMBER, TRAIL, 6, 1.3f), L(FLAME, BURST, 5))
                Move.FLARE_BLITZ -> listOf(L(FLAME, BURST, 10), L(STAR, STAMP, 1))
                Move.MAGMA_BURST -> listOf(L(ROCK, BURST, 6, 1f, Color(0xFF7A2E1A)), L(Glyph.EMBER, RAIN, 6))
                Move.OVERHEAT -> listOf(L(FLAME, TRAIL, 8, 1.3f), L(SMOKE, RISE, 6, 1.3f))
                Move.BLAST_FLAME -> listOf(L(FLAME, GROUND, 10), L(Glyph.EMBER, BURST, 10), L(SUN, STAMP, 1, 1.5f))

                Move.BUBBLE -> listOf(L(Glyph.BUBBLE, TRAIL, 6))
                Move.WATER_GUN -> listOf(L(DROP, TRAIL, 6))
                Move.WITHDRAW -> listOf(L(SHIELD, STAMP, 1, 1.4f), L(Glyph.BUBBLE, ORBIT, 3))
                Move.AQUA_JET -> listOf(L(DROP, TRAIL, 8, 0.8f), L(Glyph.BUBBLE, BURST, 4))
                Move.AQUA_TAIL -> listOf(L(DROP, SWEEP, 8))
                Move.RAIN_DANCE -> listOf(L(DROP, RAIN, 10))
                Move.WHIRLPOOL -> listOf(L(DROP, SPIRAL, 10), L(RING, STAMP, 2))
                Move.BRINE -> listOf(L(Glyph.BUBBLE, BURST, 6), L(DROP, RAIN, 4))
                Move.DIVE -> listOf(L(DROP, RISE, 8), L(RING, GROUND, 3))
                Move.WATER_JET -> listOf(L(DROP, TRAIL, 10, 1.2f), L(Glyph.BUBBLE, BURST, 6))
                Move.AQUA_RING -> listOf(L(RING, ORBIT, 3), L(Glyph.BUBBLE, RISE, 5))
                Move.WATERFALL -> listOf(L(DROP, RAIN, 12, 1.2f), L(Glyph.BUBBLE, BURST, 4))
                Move.HYDRO_PUMP -> listOf(L(DROP, TRAIL, 12, 1.3f), L(Glyph.BUBBLE, SPIRAL, 6))
                Move.HYDRO_CANNON -> listOf(L(Glyph.BUBBLE, TRAIL, 8, 1.5f), L(DROP, BURST, 10), L(RING, STAMP, 2, 2f))
                Move.UNDERTOW -> listOf(L(DROP, CONVERGE, 10), L(RING, SPIRAL, 4))

                Move.LEAF_TOSS -> listOf(L(LEAF, TRAIL, 4))
                Move.ABSORB -> listOf(L(HEART, CONVERGE, 4, 0.8f, Leaf))
                Move.LEECH_SEED -> listOf(L(SEED, STAMP, 3), L(LEAF, RISE, 3))
                Move.RAZOR_LEAF -> listOf(L(LEAF, TRAIL, 8, 0.9f))
                Move.STUN_SPORE -> listOf(L(SPORE, RAIN, 10, 1f, Gold))
                Move.GROWTH -> listOf(L(LEAF, RISE, 6))
                Move.BULLET_SEED -> listOf(L(SEED, TRAIL, 8))
                Move.SEED_BOMB -> listOf(L(SEED, BURST, 8), L(SMOKE, BURST, 3, 1f, Leaf))
                Move.VINE_WHIP -> listOf(L(VINE, STAMP, 2, 1.3f), L(LEAF, SWEEP, 6))
                Move.SYNTHESIS -> listOf(L(SUN, STAMP, 1, 1.2f, Gold), L(LEAF, ORBIT, 5))
                Move.GIGA_DRAIN -> listOf(L(HEART, CONVERGE, 8, 0.9f, Leaf), L(LEAF, SPIRAL, 5))
                Move.SLEEP_POWDER -> listOf(L(SPORE, RAIN, 12, 1f, Lavender), L(MOON, STAMP, 1, 0.8f, Lavender))
                Move.PETAL_STORM -> listOf(L(PETAL, SPIRAL, 14, 1f, Pink), L(PETAL, TRAIL, 8, 1f, Pink))
                Move.FRENZY_PLANT -> listOf(L(VINE, RISE, 6, 1.5f), L(LEAF, BURST, 10))
                Move.LEAF_BLADE -> listOf(L(LEAF, STAMP, 1, 2.5f), L(LEAF, SWEEP, 6))

                Move.POWDER_SNOW -> listOf(L(SNOW, TRAIL, 6))
                Move.ICE_FANG -> listOf(L(FANG, STAMP, 1, 1f, Snow), L(SHARD, BURST, 5))
                Move.MIST -> listOf(L(SMOKE, ORBIT, 6, 1.2f, Mist))
                Move.ICY_WIND -> listOf(L(WIND, TRAIL, 6), L(SNOW, TRAIL, 4))
                Move.ICICLE_SPEAR -> listOf(L(SHARD, TRAIL, 6, 1.2f))
                Move.FROST_ARMOR -> listOf(L(SHARD, ORBIT, 6), L(SHIELD, STAMP, 1, 1.4f))
                Move.ICE_PUNCH -> listOf(L(FIST, STAMP, 1, 1.2f, Snow), L(SHARD, BURST, 6))
                Move.FROST_BREATH -> listOf(L(SNOW, TRAIL, 10), L(SMOKE, BURST, 4, 1f, Mist))
                Move.ICE_SHARD -> listOf(L(SHARD, TRAIL, 4, 1.5f), L(SNOW, BURST, 6))
                Move.AVALANCHE -> listOf(L(ROCK, RAIN, 8, 1f, Snow), L(SNOW, BURST, 8))
                Move.MIRROR_ICE -> listOf(L(SHARD, ORBIT, 4, 1.4f), L(RING, STAMP, 1, 1.6f))
                Move.FREEZE_RAY -> listOf(L(SNOW, TRAIL, 8), L(SHARD, STAMP, 3))
                Move.BLIZZARD -> listOf(L(SNOW, SPIRAL, 14), L(WIND, TRAIL, 6))
                Move.GLACIER_CRASH -> listOf(L(SHARD, RISE, 8, 1.5f), L(SNOW, BURST, 8))
                Move.ABSOLUTE_ZERO -> listOf(L(SNOW, CONVERGE, 12), L(SHARD, STAMP, 6, 1.3f), L(RING, STAMP, 2, 2f))

                Move.SPARK -> listOf(L(Glyph.SPARK, BURST, 6))
                Move.THUNDER_WAVE -> listOf(L(RING, STAMP, 3), L(Glyph.SPARK, SPIRAL, 4))
                Move.THUNDER_FANG -> listOf(L(FANG, STAMP, 1), L(Glyph.SPARK, BURST, 6))
                Move.SHOCK_WAVE -> listOf(L(RING, GROUND, 3), L(Glyph.SPARK, BURST, 5))
                Move.VOLT_SAP -> listOf(L(Glyph.SPARK, CONVERGE, 8))
                Move.CHARGE -> listOf(L(Glyph.SPARK, CONVERGE, 10), L(BOLT, ORBIT, 3, 0.7f))
                Move.NUZZLE -> listOf(L(HEART, BURST, 3, 1f, Pink), L(Glyph.SPARK, BURST, 4))
                Move.WILD_CHARGE -> listOf(L(Glyph.SPARK, TRAIL, 10), L(BOLT, STAMP, 1))
                Move.MAGNET_PULL -> listOf(L(RING, SPIRAL, 3), L(Glyph.SPARK, TRAIL, 6))
                Move.THUNDER -> listOf(L(BOLT, RAIN, 3, 1.5f), L(Glyph.SPARK, BURST, 8))
                Move.DISCHARGE -> listOf(L(BOLT, BURST, 6, 0.8f), L(RING, STAMP, 1))
                Move.VOLT_TACKLE -> listOf(L(BOLT, TRAIL, 6, 0.8f), L(Glyph.SPARK, BURST, 8))
                Move.ZAP_CANNON -> listOf(L(Glyph.SPARK, SPIRAL, 8), L(RING, STAMP, 3, 1.5f))
                Move.THUNDERBOLT -> listOf(L(BOLT, RAIN, 5), L(RING, GROUND, 2))
                Move.GIGAVOLT -> listOf(L(BOLT, RAIN, 8, 1.6f), L(Glyph.SPARK, BURST, 14), L(SUN, STAMP, 1, 2f))

                Move.PECK -> listOf(L(STAR, STAMP, 1, 0.8f), L(FEATHER, BURST, 2))
                Move.AIR_CUTTER -> listOf(L(WIND, TRAIL, 4, 1.2f))
                Move.WING_ATTACK -> listOf(L(FEATHER, SWEEP, 6))
                Move.AERIAL_ACE -> listOf(L(STAR, TRAIL, 4), L(WIND, SWEEP, 3))
                Move.FEATHER_DANCE -> listOf(L(FEATHER, RAIN, 10))
                Move.TAILWIND -> listOf(L(WIND, ORBIT, 6))
                Move.ROOST -> listOf(L(FEATHER, RAIN, 6), L(PLUS, RISE, 3))
                Move.TWISTER -> listOf(L(WIND, SPIRAL, 8), L(LEAF, SPIRAL, 4))
                Move.FLY -> listOf(L(FEATHER, BURST, 6), L(WIND, RAIN, 3))
                Move.BOUNCE -> listOf(L(SMOKE, GROUND, 6), L(STAR, BURST, 4))
                Move.GUST -> listOf(L(WIND, TRAIL, 8, 1.4f))
                Move.DRILL_PECK -> listOf(L(WIND, SPIRAL, 6), L(STAR, STAMP, 3))
                Move.BRAVE_BIRD -> listOf(L(FEATHER, TRAIL, 8), L(STAR, BURST, 6))
                Move.HURRICANE -> listOf(L(WIND, SPIRAL, 14, 1.3f), L(FEATHER, SPIRAL, 6))
                Move.SKY_DROP -> listOf(L(FEATHER, RAIN, 6), L(SMOKE, GROUND, 8), L(STAR, STAMP, 1, 1.6f))
                Move.SKY_ATTACK -> listOf(L(FEATHER, TRAIL, 10, 1.2f), L(SUN, STAMP, 1, 1.6f, Color.White), L(WIND, BURST, 8))

                Move.MUD_SLAP -> listOf(L(MUD, TRAIL, 5))
                Move.SAND_ATTACK -> listOf(L(SAND, TRAIL, 12))
                Move.ROCK_THROW -> listOf(L(ROCK, TRAIL, 2, 1.3f), L(SAND, BURST, 4))
                Move.SAND_TOMB -> listOf(L(SAND, SPIRAL, 14))
                Move.STOMP -> listOf(L(SMOKE, GROUND, 6, 1f, Dirt), L(ROCK, RISE, 3))
                Move.STONE_SKIN -> listOf(L(ROCK, ORBIT, 5, 1f, Stone))
                Move.BONE_RUSH -> listOf(L(BONE, STAMP, 3))
                Move.DIG -> listOf(L(ROCK, RISE, 6), L(SAND, BURST, 8))
                Move.BULLDOZE -> listOf(L(SAND, GROUND, 10), L(ROCK, TRAIL, 4))
                Move.ROCK_SLAM -> listOf(L(ROCK, RAIN, 3, 2f), L(SMOKE, BURST, 5, 1f, Dirt))
                Move.SANDSTORM -> listOf(L(SAND, SPIRAL, 20), L(WIND, TRAIL, 4))
                Move.HEAD_SMASH -> listOf(L(ROCK, BURST, 6, 1f, Stone), L(STAR, SPIRAL, 3, 1.2f, Gold))
                Move.EARTHQUAKE -> listOf(L(RING, GROUND, 4, 1.5f), L(ROCK, RISE, 10))
                Move.FISSURE -> listOf(L(CRACK, STAMP, 1, 2f), L(ROCK, RISE, 6))
                Move.STONE_EDGE -> listOf(L(SHARD, RISE, 6, 1.6f, Stone), L(ROCK, BURST, 4))

                Move.SUN_RAY -> listOf(L(SUN, STAMP, 1, 1.2f, Gold), L(LEAF, TRAIL, 4))
                Move.SOLAR_BEAM -> listOf(L(SUN, STAMP, 1, 1.6f, Gold), L(STAR, TRAIL, 8, 1f, Gold))
                Move.SOLAR_FLARE -> listOf(L(SUN, STAMP, 1, 2.4f, Gold), L(STAR, BURST, 10, 1f, Gold), L(RING, GROUND, 3, 1f, Gold))
                Move.SHADOW_SWIPE -> listOf(L(Glyph.CLAW, STAMP, 1, 1.4f), L(SHADOW, BURST, 4))
                Move.SHADOW_CLAW -> listOf(L(Glyph.CLAW, STAMP, 2, 1.7f), L(SHADOW, TRAIL, 6))
                Move.NIGHT_TERROR -> listOf(L(EYE, STAMP, 2, 1.6f), L(SHADOW, SPIRAL, 10), L(Glyph.CLAW, STAMP, 1, 2.2f))
                Move.FLAME_BURST -> listOf(L(FLAME, BURST, 8), L(RING, STAMP, 1))
                Move.INFERNO -> listOf(L(FLAME, GROUND, 12, 1.3f), L(FLAME, RISE, 8))
                Move.DRAGON_FURY -> listOf(L(FLAME, SPIRAL, 16, 1.4f), L(FANG, STAMP, 1, 2.2f), L(Glyph.EMBER, RAIN, 8))
                Move.MOON_HOP -> listOf(L(MOON, STAMP, 1, 1.2f), L(STAR, BURST, 4))
                Move.MOON_KICK -> listOf(L(MOON, RAIN, 1, 1.8f), L(STAR, BURST, 6))
                Move.LUNAR_STRIKE -> listOf(L(MOON, RAIN, 3, 2f), L(STAR, SPIRAL, 8), L(RING, GROUND, 2))
                Move.RALLY_BARK -> listOf(L(NOTE, BURST, 6, 1.2f), L(ARROW, RISE, 3))
                Move.PACK_CALL -> listOf(L(NOTE, BURST, 10, 1.3f), L(PAW, ORBIT, 4), L(PLUS, RISE, 4))
                Move.ALPHA_HOWL -> listOf(L(MOON, STAMP, 1, 2f, Gold), L(NOTE, BURST, 12, 1.4f), L(PAW, ORBIT, 6))
                Move.STATIC_EYE -> listOf(L(EYE, STAMP, 1, 1.4f), L(Glyph.SPARK, BURST, 5))
                Move.STORM_EYE -> listOf(L(EYE, STAMP, 1, 2f), L(BOLT, RAIN, 3))
                Move.TEMPEST_EYE -> listOf(L(EYE, STAMP, 1, 2.6f), L(BOLT, RAIN, 6), L(WIND, SPIRAL, 8))
                Move.SHELL_UP -> listOf(L(SHIELD, STAMP, 1, 1.8f))
                Move.FORTRESS -> listOf(L(SHIELD, STAMP, 1, 2.2f), L(ROCK, ORBIT, 6, 1f, Stone), L(PLUS, RISE, 3, 1f, Leaf))
                Move.CITADEL -> listOf(L(SHIELD, STAMP, 2, 2.6f), L(ROCK, ORBIT, 8, 1f, Stone), L(PLUS, RISE, 5, 1f, Leaf))
                Move.WISP_FLAME -> listOf(L(Glyph.EMBER, SPIRAL, 4, 1.4f, Blue), L(FLAME, BURST, 4, 1f, Blue))
                Move.FOXFIRE -> listOf(L(FLAME, SPIRAL, 6, 1.3f, Blue), L(Glyph.EMBER, TRAIL, 6, 1f, Blue))
                Move.NINE_TAILS -> listOf(L(FLAME, SPIRAL, 9, 1.5f, Blue), L(FLAME, BURST, 9, 1f, Blue), L(RING, GROUND, 2, 1f, Blue))
                Move.SPLASH_WAVE -> listOf(L(DROP, BURST, 8), L(WAVE, TRAIL, 3))
                Move.TIDAL_WAVE -> listOf(L(WAVE, TRAIL, 5, 1.5f), L(DROP, RAIN, 8))
                Move.TSUNAMI -> listOf(L(WAVE, TRAIL, 7, 2f), L(DROP, CONVERGE, 10), L(Glyph.BUBBLE, BURST, 8))
                Move.BEAR_SWIPE -> listOf(L(Glyph.CLAW, STAMP, 1, 1.6f), L(SMOKE, BURST, 4))
                Move.MAUL -> listOf(L(Glyph.CLAW, STAMP, 2, 2f), L(FANG, STAMP, 1, 1.6f))
                Move.GRIZZLY_RAMPAGE -> listOf(L(Glyph.CLAW, STAMP, 3, 2.2f), L(STAR, SPIRAL, 4, 1f, Gold), L(ROCK, BURST, 6))
                Move.FROST_SLIDE -> listOf(L(SNOW, TRAIL, 8, 1f, Snow), L(SHARD, BURST, 4, 1f, Snow))
                Move.POLAR_CRASH -> listOf(L(SHARD, STAMP, 3, 1.4f, Snow), L(SNOW, GROUND, 8, 1f, Snow))
                Move.EMPEROR_BLIZZARD -> listOf(L(SNOW, SPIRAL, 18, 1.2f, Snow), L(SHARD, RAIN, 8, 1.3f, Snow), L(RING, GROUND, 3, 1f, Snow))
                Move.SPIKE_ROLL -> listOf(L(SHARD, SPIRAL, 8, 0.8f, Dirt), L(SAND, BURST, 6))
                Move.NEEDLE_STORM -> listOf(L(SHARD, TRAIL, 10, 0.7f, Leaf), L(STAR, BURST, 4, 0.8f))
                Move.QUILL_BARRAGE -> listOf(L(SHARD, TRAIL, 16, 0.8f, Leaf), L(SHARD, RAIN, 8, 0.8f, Dirt), L(STAR, BURST, 6))
                Move.ECHO_SHRIEK -> listOf(L(NOTE, TRAIL, 6, 1.4f, Lavender), L(RING, STAMP, 2, 1f, Lavender))
                Move.VAMPIRE_FANG -> listOf(L(FANG, STAMP, 1, 1.5f, Shade), L(HEART, CONVERGE, 5, 1f, Pink))
                Move.BLOOD_MOON -> listOf(L(MOON, STAMP, 1, 2.2f, Pink), L(SHADOW, SPIRAL, 10), L(HEART, CONVERGE, 8, 1f, Pink))
                Move.ANTLER_JAB -> listOf(L(STAR, STAMP, 1, 1.2f), L(LEAF, BURST, 5, 1f, Leaf))
                Move.VERDANT_CHARGE -> listOf(L(LEAF, TRAIL, 10, 1f, Leaf), L(VINE, STAMP, 1, 1.4f, Leaf))
                Move.PEBBLE_TOSS -> listOf(L(ROCK, TRAIL, 3, 0.8f, Stone), L(DROP, BURST, 6))
                Move.RIVER_RUSH -> listOf(L(WAVE, TRAIL, 4, 1.2f), L(BUBBLE, BURST, 6))
                Move.RAPID_CURRENT -> listOf(L(DROP, CONVERGE, 12), L(WAVE, SPIRAL, 4, 1.3f), L(RING, GROUND, 2))
                Move.BAMBOO_SMACK -> listOf(L(VINE, STAMP, 1, 1.6f, Leaf), L(STAR, SPIRAL, 3, 1f, Gold))
                Move.BAMBOO_STORM -> listOf(L(LEAF, SPIRAL, 12, 1f, Leaf), L(VINE, TRAIL, 4, 1f, Leaf))
                Move.PANDA_QUAKE -> listOf(L(RING, GROUND, 4, 1.6f, Dirt), L(ROCK, RISE, 10, 1f, Stone), L(LEAF, BURST, 6, 1f, Leaf))
                Move.TAIL_LASH -> listOf(L(WIND, SWEEP, 4, 1.2f), L(SAND, BURST, 6))
                Move.MAGMA_TAIL -> listOf(L(FLAME, SWEEP, 6), L(ROCK, BURST, 4, 0.8f, Dirt))
                Move.VOLCANO_ROAR -> listOf(L(FLAME, TRAIL, 12, 1.3f), L(ROCK, RAIN, 6, 1f, Dirt), L(SMOKE, RISE, 6))
                Move.SNATCH -> listOf(L(PAW, STAMP, 1, 1.4f, Stone), L(SPARK, CONVERGE, 5, 1f, Gold))
                Move.TRASH_TOSS -> listOf(L(ROCK, TRAIL, 4, 1f, Stone), L(SMOKE, BURST, 5))
                Move.MIDNIGHT_HEIST -> listOf(L(SHADOW, BURST, 6), L(SPARK, CONVERGE, 8, 1f, Gold), L(MOON, STAMP, 1, 1.4f, Lavender))
                Move.SPIRIT_STAMPEDE -> listOf(L(SNOW, TRAIL, 10, 1f, Snow), L(LEAF, SPIRAL, 8, 1f, Leaf), L(SMOKE, GROUND, 8, 1f, Mist))
                Move.SEED_SPIT -> listOf(L(SEED, TRAIL, 6, 1f, Bone), L(STAR, BURST, 3, 0.8f, Gold))
                Move.WHEEL_DASH -> listOf(L(RING, SPIRAL, 3, 1.2f, Gold), L(SPARK, TRAIL, 6))
                Move.THUNDER_WHEEL -> listOf(L(RING, SPIRAL, 4, 1.4f, Gold), L(BOLT, BURST, 5), L(SPARK, TRAIL, 8))
                Move.SQUAWK -> listOf(L(NOTE, BURST, 6, 1.1f), L(RING, STAMP, 2, 1.4f))
                Move.FEATHER_BLAZE -> listOf(L(FEATHER, TRAIL, 8, 1f, Gold), L(FLAME, BURST, 6))
                Move.PHOENIX_DIVE -> listOf(L(FEATHER, RAIN, 8, 1.2f, Gold), L(FLAME, SPIRAL, 10, 1.3f), L(STAR, STAMP, 1, 1.6f))
                Move.BUBBLE_GILL -> listOf(L(BUBBLE, TRAIL, 8, 1f, Pink))
                Move.TIDE_POOL -> listOf(L(WAVE, SWEEP, 4, 1.2f), L(HEART, CONVERGE, 4, 0.8f, Pink))
                Move.LOTUS_TORRENT -> listOf(L(PETAL, SPIRAL, 10, 1.1f, Pink), L(WAVE, TRAIL, 6), L(VINE, BURST, 4, 1f, Leaf))
                Move.BURROW_JAB -> listOf(L(MUD, GROUND, 6, 1f, Dirt), L(CLAW, STAMP, 1, 1.4f))
                Move.DRILL_CLAW -> listOf(L(CLAW, STAMP, 3, 1f), L(ROCK, BURST, 4, 0.8f, Dirt))
                Move.TREMOR_SPARK -> listOf(L(CRACK, GROUND, 4, 1.4f, Dirt), L(BOLT, RAIN, 6), L(SPARK, BURST, 8))
                Move.DISTRACT -> listOf(L(STAR, ORBIT, 4, 1f, Gold), L(NOTE, BURST, 3))
                Move.LOCK_ON -> listOf(L(RING, CONVERGE, 3, 1.2f), L(EYE, STAMP, 1, 1.2f))
                Move.SWITCHEROO -> listOf(L(ARROW, SPIRAL, 4), L(STAR, BURST, 4, 0.8f, Lavender))
                Move.CHEER -> listOf(L(NOTE, RISE, 5, 1f, Pink), L(PLUS, RISE, 4))
                Move.BARRIER -> listOf(L(SHIELD, STAMP, 1, 1.8f), L(RING, ORBIT, 3))
                Move.SCORCH_MARK -> listOf(L(FLAME, TRAIL, 5), L(RING, STAMP, 1, 1.2f))
                Move.BUBBLE_SHIELD -> listOf(L(BUBBLE, ORBIT, 6, 1.3f), L(SHIELD, STAMP, 1, 1.4f))
                Move.HEALING_RAIN -> listOf(L(DROP, RAIN, 10), L(PLUS, RISE, 4))
                Move.BRAMBLE_SNARE -> listOf(L(VINE, GROUND, 4, 1.2f), L(SEED, TRAIL, 3))
                Move.FROST_MINE -> listOf(L(SHARD, GROUND, 5, 1f, Snow), L(SNOW, BURST, 6, 1f, Snow))
                Move.STATIC_JAM -> listOf(L(SPARK, ORBIT, 6), L(RING, STAMP, 2, 0.8f))
                Move.CHAIN_LIGHTNING -> listOf(L(BOLT, TRAIL, 5, 1.1f), L(SPARK, BURST, 8))
                Move.CROSSWIND -> listOf(L(WIND, SPIRAL, 6, 1.1f), L(FEATHER, BURST, 4))
                Move.RICOCHET -> listOf(L(ROCK, TRAIL, 3, 1f, Stone), L(STAR, BURST, 4))
                Move.CALTROPS -> listOf(L(SHARD, GROUND, 6, 0.8f, Stone), L(SAND, BURST, 4))
                Move.EUCALYPTUS_DART -> listOf(L(LEAF, TRAIL, 6, 1f, Leaf), L(SPORE, BURST, 4))
                Move.DROWSY_SLAM -> listOf(L(MOON, STAMP, 1, 1.4f, Lavender), L(STAR, SPIRAL, 4, 1f, Gold), L(SMOKE, GROUND, 5))
                Move.DREAM_STORM -> listOf(L(BOLT, RAIN, 6, 1.3f), L(STAR, SPIRAL, 8, 1f, Lavender), L(SPARK, BURST, 10))
                Move.FIN_SLICE -> listOf(L(WAVE, SWEEP, 3, 1.2f), L(DROP, BURST, 5))
                Move.JAW_LOCK -> listOf(L(FANG, STAMP, 1, 1.8f), L(RING, STAMP, 1, 1.3f, Color(0xFFE53935)))
                Move.FEEDING_FRENZY -> listOf(L(FANG, STAMP, 3, 1.6f), L(WAVE, SPIRAL, 4, 1.2f), L(HEART, CONVERGE, 5, 1f, Pink))
                Move.TONGUE_SNAP -> listOf(L(HEART, TRAIL, 4, 0.8f, Pink), L(STAR, STAMP, 1))
                Move.MIRAGE_SWAP -> listOf(L(LEAF, SPIRAL, 8, 1f, Leaf), L(ARROW, ORBIT, 3), L(STAR, BURST, 4, 1f, Lavender))
                Move.PRISM_BLAST -> listOf(L(STAR, TRAIL, 8, 1.2f, Pink), L(SPARK, TRAIL, 8, 1f, Blue), L(SUN, STAMP, 1, 1.5f))
                Move.LOG_TOSS -> listOf(L(BONE, TRAIL, 2, 1.6f, Dirt), L(SAND, BURST, 6))
                Move.TIMBER_TRAP -> listOf(L(BONE, GROUND, 4, 1.4f, Dirt), L(LEAF, BURST, 4, 1f, Leaf))
                Move.GREAT_DAM -> listOf(L(SHIELD, STAMP, 1, 2.2f, Dirt), L(DROP, ORBIT, 6), L(BONE, ORBIT, 3, 1.2f, Dirt))

                Move.RAPID_SPIN -> listOf(L(WIND, SPIRAL, 8), L(STAR, BURST, 3))
                Move.SUCKER_PUNCH -> listOf(L(SHADOW, TRAIL, 4, 1f, Shade), L(FIST, STAMP, 1, 1.2f))
                Move.HEAT_WAVE -> listOf(L(FLAME, SWEEP, 10), L(SUN, STAMP, 1, 1.4f, EmberGold))
                Move.SCALD -> listOf(L(DROP, TRAIL, 8, 1f, Color(0xFFFF9A6B)), L(SMOKE, RISE, 4, 1f, Mist))
                Move.SPORE_CLOUD -> listOf(L(SPORE, BURST, 12, 1f, Leaf), L(SMOKE, ORBIT, 3, 1f, Leaf))
                Move.HAIL_STORM -> listOf(L(SHARD, RAIN, 12), L(SNOW, RAIN, 10))
                Move.VOLT_SWITCH -> listOf(L(BOLT, TRAIL, 4), L(SPARK, SPIRAL, 6))
                Move.MUD_BOMB -> listOf(L(MUD, TRAIL, 5, 1.3f), L(MUD, BURST, 6))
                Move.COMET_PUNCH -> listOf(L(FIST, STAMP, 3, 0.8f), L(STAR, BURST, 4, 1f, Gold))
                Move.VACUUM_WAVE -> listOf(L(RING, TRAIL, 3, 1.2f, Color.White), L(WIND, TRAIL, 4))
                Move.SEISMIC_TOSS -> listOf(L(FIST, RISE, 1, 1.5f), L(SMOKE, GROUND, 10, 1.3f), L(CRACK, STAMP, 1, 1.6f))

                Move.BANANA_TOSS -> listOf(L(MOON, TRAIL, 3, 1.2f, Gold), L(STAR, BURST, 4))
                Move.VINE_SWING -> listOf(L(VINE, SWEEP, 3, 1.3f), L(FIST, STAMP, 2, 1.1f))
                Move.CANOPY_FURY -> listOf(L(LEAF, SPIRAL, 12), L(FIST, STAMP, 1, 2f), L(STAR, BURST, 6, 1f, Gold))
                Move.POUCH_JAB -> listOf(L(FIST, STAMP, 1, 1.3f), L(WIND, BURST, 3))
                Move.BOXING_FLURRY -> listOf(L(FIST, BURST, 6, 0.9f, Color(0xFFD9534F)), L(STAR, BURST, 4))
                Move.OUTBACK_STOMP -> listOf(L(CRACK, STAMP, 1, 2f), L(SAND, GROUND, 12), L(RING, STAMP, 2, 1.8f))
                Move.TALON_GRAB -> listOf(L(Glyph.CLAW, STAMP, 1, 1.3f), L(FEATHER, CONVERGE, 4))
                Move.SKY_DIVE -> listOf(L(FEATHER, RAIN, 8), L(WIND, TRAIL, 6), L(STAR, STAMP, 1, 1.5f))
                Move.STORM_TALON -> listOf(L(Glyph.CLAW, STAMP, 2, 1.6f), L(WIND, SPIRAL, 12, 1.2f), L(BOLT, RAIN, 4))
                Move.FLIPPER_SLAP -> listOf(L(DROP, BURST, 6), L(STAR, STAMP, 1))
                Move.ICE_FLOE -> listOf(L(SHARD, TRAIL, 8), L(SNOW, BURST, 6))
                Move.ARCTIC_TIDE -> listOf(L(DROP, SWEEP, 12, 1.2f), L(SHARD, BURST, 8), L(SNOW, RAIN, 8))
                else -> genericFx() // stage-option uniques: built from their type and effect
    }

/** Effect built from a move's type and effect, used by the per-stage unique options. */
private fun Move.genericFx(): List<Layer> {
    val big = 1f + damage / 40f
    val pool = when (element) {
        Element.FIRE -> listOf(FLAME, Glyph.EMBER, SUN, FLAME)
        Element.WATER -> listOf(DROP, BUBBLE, WAVE)
        Element.GRASS -> listOf(LEAF, PETAL, SEED, VINE, SPORE)
        Element.ICE -> listOf(SNOW, SHARD, SNOW)
        Element.ELECTRIC -> listOf(BOLT, SPARK, BOLT)
        Element.FLYING -> listOf(FEATHER, WIND)
        Element.GROUND -> listOf(ROCK, SAND, MUD, ROCK)
        Element.FIGHTING -> listOf(FIST, STAR)
        Element.NORMAL -> listOf(STAR, PAW, Glyph.CLAW, SPARK)
    }
    // A word in the name wins (Moon Kick gets moons, Bone Club bones); otherwise a stable pick from the type's pool.
    val word = label.lowercase()
    val named = listOf(
        "moon" to MOON, "lunar" to MOON, "shadow" to SHADOW, "night" to SHADOW, "dark" to SHADOW, "fang" to FANG,
        "bite" to FANG, "claw" to Glyph.CLAW, "swipe" to Glyph.CLAW, "slash" to Glyph.CLAW, "bone" to BONE,
        "star" to STAR, "sun" to SUN, "solar" to SUN, "eye" to EYE, "glare" to EYE, "needle" to SHARD, "quill" to SHARD,
        "spike" to SHARD, "thorn" to SHARD, "punch" to FIST, "fist" to FIST, "kick" to STAR, "heart" to HEART,
        "song" to NOTE, "shriek" to NOTE, "roar" to NOTE, "howl" to NOTE, "bark" to NOTE, "cry" to NOTE, "paw" to PAW,
        "feather" to FEATHER, "wing" to FEATHER, "gust" to WIND, "wind" to WIND, "bubble" to BUBBLE, "mud" to MUD,
        "sand" to SAND, "rock" to ROCK, "stone" to ROCK, "quake" to CRACK, "fissure" to CRACK, "leaf" to LEAF,
        "petal" to PETAL, "seed" to SEED, "spore" to SPORE, "vine" to VINE, "frost" to SNOW, "snow" to SNOW,
        "ice" to SHARD, "spark" to SPARK, "volt" to BOLT, "thunder" to BOLT, "ember" to Glyph.EMBER, "flame" to FLAME,
    ).firstOrNull { it.first in word }?.second
    val typeGlyph = named ?: pool[variant(pool.size)]
    val alt = pool[(variant(997) / 7 + 1) % pool.size].takeIf { it != typeGlyph } ?: STAR
    val accent = when (variant(6)) { // one more flourish so moves with the same effect still differ
        0 -> L(RING, STAMP, 2, 1.2f)
        1 -> L(alt, SPIRAL, 4, 0.8f)
        2 -> L(alt, RAIN, 5, 0.9f)
        3 -> L(alt, GROUND, 6, 0.8f)
        4 -> L(alt, SWEEP, 4, 0.9f)
        else -> L(SPARK, BURST, 5, 0.7f, Color.White)
    }
    val second = element2?.let {
        when (it) {
            Element.FIRE -> Glyph.EMBER; Element.WATER -> Glyph.BUBBLE; Element.GRASS -> PETAL; Element.ICE -> SHARD
            Element.ELECTRIC -> SPARK; Element.FLYING -> WIND; Element.GROUND -> SAND; Element.FIGHTING -> FIST
            Element.NORMAL -> STAR
        }
    }
    val main = when (effect) {
        Effect.HEAL, Effect.AID, Effect.PACK_CALL -> listOf(L(PLUS, RISE, 6), L(typeGlyph, ORBIT, 4))
        Effect.SHIELD, Effect.HARDEN, Effect.FORTRESS, Effect.CITADEL, Effect.GUARD ->
            listOf(L(SHIELD, STAMP, 1, 1.8f), L(typeGlyph, ORBIT, 5))
        Effect.COUNTER -> listOf(L(SHIELD, STAMP, 1, 1.3f, Color.White), L(typeGlyph, ORBIT, 3))
        Effect.RAGE, Effect.BATTLE_CRY, Effect.ALPHA -> listOf(L(ARROW, RISE, 4, 1.2f), L(typeGlyph, BURST, 6))
        Effect.HASTE -> listOf(L(WIND, ORBIT, 5), L(typeGlyph, TRAIL, 4))
        Effect.ENERGIZE -> listOf(L(SPARK, CONVERGE, 8, 1f, Gold), L(typeGlyph, RISE, 3))
        Effect.BLINK, Effect.SMOKE -> listOf(L(SMOKE, BURST, 6), L(typeGlyph, SPIRAL, 4))
        Effect.WEAKEN, Effect.MARK, Effect.DISRUPT -> listOf(L(EYE, STAMP, 1, 1.3f), L(typeGlyph, SPIRAL, 5))
        Effect.SLOW, Effect.ROOT, Effect.FREEZE, Effect.PARALYZE -> listOf(L(typeGlyph, CONVERGE, 8), L(RING, STAMP, 2))
        Effect.LEECH, Effect.DRAIN, Effect.SIPHON -> listOf(L(typeGlyph, TRAIL, 5), L(HEART, CONVERGE, 4, 0.8f, Pink))
        Effect.MULTI -> listOf(L(typeGlyph, TRAIL, 8, 0.9f), L(STAR, BURST, 4))
        Effect.VANISH -> listOf(L(SMOKE, GROUND, 6), L(typeGlyph, RAIN, 6), L(STAR, STAMP, 1, big))
        Effect.TRAP -> listOf(L(typeGlyph, GROUND, 6), L(RING, STAMP, 1, 1.4f))
        Effect.KNOCKBACK, Effect.STUN -> listOf(L(typeGlyph, BURST, 8, big), L(STAR, SPIRAL, 3, 1f, Gold))
        else -> listOf(L(typeGlyph, TRAIL, 6, big), L(STAR, STAMP, 1, big))
    }
    return main + listOfNotNull(second?.let { L(it, BURST, 5) }) + accent
}

/** Stable pick among [n] looks per move, so moves sharing a hitbox shape still look different. */
internal fun Move.variant(n: Int) = (name.hashCode() and 0x7fffffff) % n

/** The move's main particle (its first layer that isn't a plain ring or puff), for projectiles and sprays. */
internal fun DrawScope.drawSignature(m: Move, c: Offset, size: Float, rot: Float, alpha: Float) {
    val l = m.fx.firstOrNull { it.glyph != RING && it.glyph != SMOKE } ?: m.fx.first()
    glyph(l.glyph, c, size, rot, l.color ?: m.tint(), alpha)
}

private fun rnd(seed: Int): Float {
    var x = seed * 0x27d4eb2d + 0x165667b1
    x = x xor (x ushr 15)
    x *= 0x2c1b3c6d
    x = x xor (x ushr 12)
    return (x and 0xFFFF) / 65535f
}

private fun lerp(a: Offset, b: Offset, t: Float) = Offset(a.x + (b.x - a.x) * t, a.y + (b.y - a.y) * t)

/**
 * Draws [m]'s own particles. [origin]/[impact] are in pixels; [after] is ms since the hit
 * (negative while winding up, when a faint charge-up of the first layer gathers on the user).
 */
internal fun DrawScope.drawMoveFx(m: Move, origin: Offset, impact: Offset, after: Long, isSelf: Boolean) {
    val layers = m.fx
    val vx = impact.x - origin.x
    val vy = impact.y - origin.y
    val len = hypot(vx, vy)
    val dir = if (len < 1f) Offset(1f, 0f) else Offset(vx / len, vy / len)
    val perp = Offset(-dir.y, dir.x)
    val aim = atan2(dir.y, dir.x)
    val R = 40.dp.toPx()
    val u = 5.dp.toPx()
    val tint = m.tint()

    if (after < 0) { // charge-up
        if (m.windupMs < 250) return
        val l = layers.first()
        val prog = (m.windupMs + after) / m.windupMs.toFloat()
        for (i in 0 until 5) {
            val lt = (prog * 2f + i / 5f) % 1f
            val ang = i * 1.2566f + m.ordinal
            val from = origin + Offset(cos(ang), sin(ang) * 0.7f) * R * 1.1f
            glyph(l.glyph, lerp(from, origin, lt), u * 0.8f * l.size.coerceAtMost(1.4f), ang, l.color ?: tint, 0.5f * (1 - lt))
        }
        return
    }
    val dur = minOf(650L, 1200L - m.windupMs).coerceAtLeast(300L).toFloat()
    val t = after / dur
    if (t > 1f) return

    layers.forEachIndexed { li, l ->
        val color = l.color ?: tint
        val n = l.count
        for (i in 0 until n) {
            val seed = m.ordinal * 131 + li * 17 + i * 7
            val r1 = rnd(seed); val r2 = rnd(seed + 1); val r3 = rnd(seed + 2)
            val delay = if (l.motion == Motion.STAMP) i * 0.15f else r3 * 0.25f
            val lt = (t - delay) / (1f - delay)
            if (lt < 0f || lt > 1f) continue
            val sz = u * l.size
            val fade = 1f - lt
            when (l.motion) {
                Motion.BURST -> {
                    val ang = (2 * PI * i / n).toFloat() + r1
                    val d = R * (0.3f + 0.9f * lt) * (0.7f + 0.5f * r2)
                    glyph(l.glyph, impact + Offset(cos(ang), sin(ang) * 0.8f) * d, sz, ang + lt * 2, color, fade)
                }
                Motion.TRAIL -> {
                    val f = (i.toFloat() / n + lt * 0.6f).coerceAtMost(1f)
                    val wob = sin(f * PI.toFloat() * 3 + r2 * 6) * u * 1.5f * (r3 + 0.3f)
                    glyph(l.glyph, lerp(origin, impact, f) + perp * wob, sz, aim + lt, color, fade)
                }
                Motion.RAIN -> {
                    val x = impact.x + (r1 - 0.5f) * R * 1.6f * (if (n == 1) 0f else 1f)
                    val fall = minOf(1f, lt * 1.5f)
                    val y = impact.y - R * 2.2f * (1 - fall) + (r2 - 0.5f) * R * 0.3f
                    glyph(l.glyph, Offset(x, y), sz, (r1 - 0.5f) * 0.6f + lt * 3, color, if (lt < 0.7f) 1f else (1 - lt) / 0.3f)
                }
                Motion.RISE -> {
                    val x = impact.x + (r1 - 0.5f) * R * 1.4f + sin(lt * 6 + r2 * 6) * u
                    val y = impact.y + R * 0.4f - lt * R * (1.2f + r2)
                    glyph(l.glyph, Offset(x, y), sz, sin(lt * 5 + r1 * 6) * 0.4f, color, fade)
                }
                Motion.SPIRAL -> {
                    val ang = (2 * PI * i / n).toFloat() + lt * 4 * PI.toFloat()
                    val rad = R * (0.9f - 0.5f * lt) * (0.6f + 0.4f * r2)
                    glyph(l.glyph, impact + Offset(cos(ang), sin(ang) * 0.7f) * rad, sz, ang, color, sin(PI.toFloat() * lt))
                }
                Motion.CONVERGE -> {
                    val e = lt * lt * (3 - 2 * lt)
                    val pos = if (isSelf) {
                        val ang = (2 * PI * i / n).toFloat() + r1
                        lerp(origin + Offset(cos(ang), sin(ang) * 0.8f) * R * (0.9f + 0.6f * r2), origin, e)
                    } else {
                        val from = impact + Offset(r1 - 0.5f, r2 - 0.5f) * R * 0.8f
                        lerp(from, origin, e) + perp * sin(PI.toFloat() * e) * R * (r3 - 0.5f)
                    }
                    glyph(l.glyph, pos, sz, aim + PI.toFloat(), color, 1f - lt * 0.6f)
                }
                Motion.ORBIT -> {
                    val ang = (2 * PI * i / n).toFloat() + lt * 2.4f * PI.toFloat()
                    glyph(l.glyph, origin + Offset(cos(ang) * R, sin(ang) * R * 0.45f), sz, ang, color,
                        minOf(1f, sin(PI.toFloat() * lt) * 1.6f))
                }
                Motion.STAMP -> {
                    val jit = if (n > 1) Offset(r1 - 0.5f, r2 - 0.5f) * R * 0.7f else Offset.Zero
                    val grow = 0.6f + 0.6f * (1 - (1 - lt) * (1 - lt))
                    val rot = if (isSelf) 0f else aim + (r3 - 0.5f) * 0.6f
                    glyph(l.glyph, impact + jit, sz * 3 * grow, rot, color, 1 - lt * lt)
                }
                Motion.SWEEP -> {
                    val f = (lt * 1.3f - i.toFloat() / n * 0.3f).coerceIn(0f, 1f)
                    val a = -1.2f + 2.4f * f
                    val ra = Offset(dir.x * cos(a) - dir.y * sin(a), dir.x * sin(a) + dir.y * cos(a))
                    glyph(l.glyph, impact - dir * R * 0.3f + ra * R * 0.8f, sz, aim + a, color, fade)
                }
                Motion.GROUND -> {
                    val ground = impact + Offset(0f, 24.dp.toPx())
                    if (l.glyph == Glyph.RING) {
                        val k = lt - i * 0.15f
                        if (k > 0f) {
                            val rad = R * (0.3f + 1.4f * k) * l.size
                            drawOval(color.copy(alpha = (1 - k).coerceIn(0f, 1f) * 0.8f), Offset(ground.x - rad, ground.y - rad * 0.3f),
                                Size(rad * 2, rad * 0.6f), style = Stroke(3.dp.toPx()))
                        }
                    } else {
                        val ang = (2 * PI * i / n).toFloat() + r1 * 0.3f
                        val rad = R * (0.3f + 1.4f * lt)
                        val lift = sin(PI.toFloat() * lt) * u * 2 * r2
                        glyph(l.glyph, ground + Offset(cos(ang) * rad, sin(ang) * rad * 0.3f - lift), sz, ang, color, fade)
                    }
                }
            }
        }
    }
}

private fun DrawScope.glyph(g: Glyph, c: Offset, s: Float, rot: Float, color: Color, alpha: Float) {
    val a = alpha.coerceIn(0f, 1f)
    if (a <= 0.01f) return
    val col = color.copy(alpha = color.alpha * a)
    val white = Color.White.copy(alpha = a)
    fun path(vararg pts: Float) = Path().apply {
        moveTo(c.x + pts[0] * s, c.y + pts[1] * s)
        for (k in 2 until pts.size step 2) lineTo(c.x + pts[k] * s, c.y + pts[k + 1] * s)
        close()
    }
    fun o(x: Float, y: Float) = Offset(c.x + x * s, c.y + y * s)
    when (g) {
        Glyph.EMBER -> {
            drawCircle(Brush.radialGradient(listOf(white, EmberGold.copy(alpha = a), col, col.copy(alpha = 0f)), c, s * 1.5f), s * 1.5f, c)
        }
        Glyph.FLAME -> rotateRad(rot * 0.15f, c) {
            val p = Path().apply {
                moveTo(c.x, c.y - s * 1.8f)
                cubicTo(c.x + s * 1.2f, c.y - s * 0.4f, c.x + s * 1.1f, c.y + s, c.x, c.y + s)
                cubicTo(c.x - s * 1.1f, c.y + s, c.x - s * 1.2f, c.y - s * 0.4f, c.x, c.y - s * 1.8f)
            }
            drawPath(p, col)
            drawCircle(EmberGold.copy(alpha = a), s * 0.5f, o(0f, 0.35f))
        }
        Glyph.BUBBLE -> {
            drawCircle(col.copy(alpha = col.alpha * 0.25f), s, c)
            drawCircle(col, s, c, style = Stroke(s * 0.18f))
            drawCircle(white, s * 0.22f, o(-0.4f, -0.4f))
        }
        Glyph.DROP -> rotateRad(rot, c) {
            val p = Path().apply {
                moveTo(c.x + s * 1.6f, c.y)
                quadraticTo(c.x, c.y - s, c.x - s * 0.8f, c.y)
                quadraticTo(c.x, c.y + s, c.x + s * 1.6f, c.y)
            }
            drawPath(p, col)
            drawCircle(white.copy(alpha = a * 0.7f), s * 0.2f, o(-0.2f, -0.25f))
        }
        Glyph.WAVE -> rotateRad(rot, c) {
            val p = Path().apply {
                moveTo(c.x - s * 1.6f, c.y + s * 0.8f)
                cubicTo(c.x - s * 1.2f, c.y - s * 1.2f, c.x + s * 1.2f, c.y - s * 1.6f, c.x + s * 1.4f, c.y - s * 0.2f)
                quadraticTo(c.x + s * 0.5f, c.y - s * 0.7f, c.x + s * 0.3f, c.y + s * 0.8f)
                close()
            }
            drawPath(p, col)
            drawPath(p, white, style = Stroke(s * 0.15f))
        }
        Glyph.LEAF, Glyph.FEATHER -> rotateRad(rot, c) {
            val w = if (g == Glyph.LEAF) 0.75f else 0.4f
            val p = Path().apply {
                moveTo(c.x - s * 1.4f, c.y)
                quadraticTo(c.x, c.y - s * w * 1.6f, c.x + s * 1.4f, c.y)
                quadraticTo(c.x, c.y + s * w * 1.6f, c.x - s * 1.4f, c.y)
            }
            drawPath(p, if (g == Glyph.FEATHER) Color(0xFFF2F6FA).copy(alpha = a) else col)
            drawLine(if (g == Glyph.FEATHER) col else white.copy(alpha = a * 0.5f), o(-1.6f, 0f), o(1.3f, 0f), s * 0.12f)
        }
        Glyph.PETAL -> rotateRad(rot, c) {
            drawOval(col, Offset(c.x - s * 1.1f, c.y - s * 0.55f), Size(s * 2.2f, s * 1.1f))
        }
        Glyph.SEED -> {
            drawOval(Color(0xFF6B4F2A).copy(alpha = a), Offset(c.x - s * 0.8f, c.y - s * 0.55f), Size(s * 1.6f, s * 1.1f))
            drawCircle(col, s * 0.25f, o(-0.2f, -0.15f))
        }
        Glyph.SPORE -> {
            drawCircle(col.copy(alpha = col.alpha * 0.3f), s * 1.2f, c)
            drawCircle(col, s * 0.35f, o(-0.5f, 0f)); drawCircle(col, s * 0.3f, o(0.5f, -0.3f)); drawCircle(col, s * 0.3f, o(0.2f, 0.5f))
        }
        Glyph.VINE -> rotateRad(rot, c) {
            val p = Path().apply {
                moveTo(c.x - s * 2f, c.y)
                cubicTo(c.x - s, c.y - s, c.x, c.y + s, c.x + s * 2f, c.y)
            }
            drawPath(p, Color(0xFF3F9A4A).copy(alpha = a), style = Stroke(s * 0.35f, cap = StrokeCap.Round))
            drawCircle(Leaf.copy(alpha = a), s * 0.35f, o(-0.7f, -0.45f)); drawCircle(Leaf.copy(alpha = a), s * 0.35f, o(0.8f, 0.3f))
        }
        Glyph.SNOW -> for (k in 0..2) {
            val ang = rot + k * PI.toFloat() / 3
            val d = Offset(cos(ang), sin(ang)) * s * 1.3f
            drawLine(Snow.copy(alpha = a), c - d, c + d, s * 0.25f, StrokeCap.Round)
        }
        Glyph.SHARD -> rotateRad(rot, c) {
            val p = path(-1.6f, 0f, 0f, -0.45f, 1.6f, 0f, 0f, 0.45f)
            drawPath(p, col); drawPath(p, white.copy(alpha = a * 0.8f), style = Stroke(s * 0.12f))
        }
        Glyph.SPARK -> rotateRad(rot, c) {
            val p = Path().apply {
                moveTo(c.x - s * 1.4f, c.y); lineTo(c.x - s * 0.5f, c.y - s * 0.6f)
                lineTo(c.x + s * 0.2f, c.y + s * 0.4f); lineTo(c.x + s * 1.4f, c.y - s * 0.3f)
            }
            drawPath(p, col, style = Stroke(s * 0.4f, cap = StrokeCap.Round, join = StrokeJoin.Round))
            drawPath(p, white, style = Stroke(s * 0.15f, cap = StrokeCap.Round, join = StrokeJoin.Round))
        }
        Glyph.BOLT -> {
            val p = path(-0.3f, -2f, 0.7f, -2f, 0.15f, -0.4f, 0.9f, -0.4f, -0.6f, 2f, -0.1f, 0.2f, -0.8f, 0.2f)
            drawPath(p, col); drawPath(p, white, style = Stroke(s * 0.12f))
        }
        Glyph.WIND -> drawArc(col, rot * 57.3f, 200f, false, Offset(c.x - s * 1.4f, c.y - s * 0.8f),
            Size(s * 2.8f, s * 1.6f), style = Stroke(s * 0.3f, cap = StrokeCap.Round))
        Glyph.ROCK -> rotateRad(rot, c) {
            val p = path(-1f, -0.6f, 0.2f, -1.1f, 1.1f, -0.4f, 0.9f, 0.8f, -0.4f, 1f, -1.1f, 0.3f)
            drawPath(p, col); drawPath(p, Color.Black.copy(alpha = a * 0.35f), style = Stroke(s * 0.15f))
        }
        Glyph.SAND -> {
            val tan = Color(0xFFD9B77E).copy(alpha = a)
            drawCircle(tan, s * 0.3f, o(-0.6f, 0f)); drawCircle(tan, s * 0.25f, o(0.4f, -0.5f)); drawCircle(tan, s * 0.3f, o(0.3f, 0.5f))
        }
        Glyph.MUD -> {
            val mud = Color(0xFF6B4A2E).copy(alpha = a)
            drawCircle(mud, s * 0.9f, c); drawCircle(mud, s * 0.4f, o(1f, 0.6f)); drawCircle(mud, s * 0.3f, o(-1f, -0.7f))
        }
        Glyph.CRACK -> rotateRad(0f, c) {
            val p = Path().apply {
                moveTo(c.x - s * 1.8f, c.y); lineTo(c.x - s, c.y - s * 0.3f); lineTo(c.x - s * 0.4f, c.y + s * 0.3f)
                lineTo(c.x + s * 0.3f, c.y - s * 0.2f); lineTo(c.x + s, c.y + s * 0.3f); lineTo(c.x + s * 1.8f, c.y)
            }
            drawPath(p, Color(0xFF2B1D10).copy(alpha = a), style = Stroke(s * 0.25f, join = StrokeJoin.Round))
            drawPath(p, EmberGold.copy(alpha = a * 0.6f), style = Stroke(s * 0.08f, join = StrokeJoin.Round))
        }
        Glyph.BONE -> rotateRad(rot, c) {
            val b = Bone.copy(alpha = a)
            drawLine(b, o(-1.2f, 0f), o(1.2f, 0f), s * 0.35f)
            for (x in listOf(-1.3f, 1.3f)) for (y in listOf(-0.25f, 0.25f)) drawCircle(b, s * 0.28f, o(x, y))
        }
        Glyph.FANG -> rotateRad(rot, c) {
            for (k in -1..1) {
                val x = k * 0.6f
                drawPath(path(x - 0.3f, -0.7f, x + 0.3f, -0.7f, x, -0.05f), white)
                drawPath(path(x - 0.3f, 0.7f, x + 0.3f, 0.7f, x, 0.05f), white)
            }
            drawArc(col, 200f, 140f, false, o(-1.2f, -1f), Size(s * 2.4f, s * 1.2f), style = Stroke(s * 0.2f))
            drawArc(col, 20f, 140f, false, o(-1.2f, -0.2f), Size(s * 2.4f, s * 1.2f), style = Stroke(s * 0.2f))
        }
        Glyph.CLAW -> rotateRad(rot + 0.8f, c) {
            for (k in -1..1) {
                val y = k * 0.55f
                drawLine(col, o(-1.2f, y), o(1.2f, y + 0.2f), s * 0.3f, StrokeCap.Round)
                drawLine(white, o(-1.1f, y), o(1.1f, y + 0.2f), s * 0.1f, StrokeCap.Round)
            }
        }
        Glyph.FIST -> rotateRad(rot, c) {
            drawRoundRect(col, o(-0.8f, -0.65f), Size(s * 1.6f, s * 1.3f), androidx.compose.ui.geometry.CornerRadius(s * 0.4f))
            for (k in 0..2) drawLine(Color.Black.copy(alpha = a * 0.3f), o(0.45f, -0.4f + k * 0.4f), o(0.75f, -0.4f + k * 0.4f), s * 0.1f)
            drawLine(white.copy(alpha = a * 0.6f), o(-1.8f, -0.3f), o(-1.1f, -0.3f), s * 0.12f, StrokeCap.Round)
            drawLine(white.copy(alpha = a * 0.6f), o(-1.8f, 0.3f), o(-1.1f, 0.3f), s * 0.12f, StrokeCap.Round)
        }
        Glyph.PAW -> {
            drawOval(col, o(-0.6f, -0.1f), Size(s * 1.2f, s * 0.9f))
            for ((x, y) in listOf(-0.75f to -0.45f, -0.25f to -0.85f, 0.25f to -0.85f, 0.75f to -0.45f)) drawCircle(col, s * 0.25f, o(x, y))
        }
        Glyph.STAR -> rotateRad(rot, c) {
            val p = Path()
            for (k in 0 until 10) {
                val r = if (k % 2 == 0) s * 1.2f else s * 0.5f
                val ang = k * PI.toFloat() / 5 - PI.toFloat() / 2
                if (k == 0) p.moveTo(c.x + cos(ang) * r, c.y + sin(ang) * r) else p.lineTo(c.x + cos(ang) * r, c.y + sin(ang) * r)
            }
            p.close()
            drawPath(p, col); drawPath(p, white.copy(alpha = a * 0.7f), style = Stroke(s * 0.1f))
        }
        Glyph.MOON -> {
            val outer = Path().apply { addOval(Rect(c, s * 1.2f)) }
            val inner = Path().apply { addOval(Rect(o(0.5f, -0.3f), s * 1.0f)) }
            val moon = Path().apply { op(outer, inner, PathOperation.Difference) }
            drawCircle(col.copy(alpha = col.alpha * 0.2f), s * 1.8f, c)
            drawPath(moon, Color(0xFFF5E7C6).copy(alpha = a))
        }
        Glyph.SUN -> {
            drawCircle(Brush.radialGradient(listOf(white, col, col.copy(alpha = 0f)), c, s * 1.8f), s * 1.8f, c)
            for (k in 0 until 8) {
                val ang = rot + k * PI.toFloat() / 4
                drawLine(col, c + Offset(cos(ang), sin(ang)) * s * 1.3f, c + Offset(cos(ang), sin(ang)) * s * 2.2f, s * 0.2f, StrokeCap.Round)
            }
        }
        Glyph.SHADOW -> drawCircle(Brush.radialGradient(listOf(Color(0xFF1A0F2A).copy(alpha = a), Shade.copy(alpha = a * 0.7f),
            Color.Transparent), c, s * 1.6f), s * 1.6f, c)
        Glyph.RING -> drawCircle(col, s * 1.2f, c, style = Stroke(s * 0.2f))
        Glyph.SMOKE -> drawCircle(Brush.radialGradient(listOf(col.copy(alpha = col.alpha * 0.5f), col.copy(alpha = 0f)), c, s * 1.6f),
            s * 1.6f, c)
        Glyph.HEART -> {
            val p = Path().apply {
                moveTo(c.x, c.y + s)
                cubicTo(c.x - s * 1.6f, c.y - s * 0.2f, c.x - s * 0.7f, c.y - s * 1.3f, c.x, c.y - s * 0.4f)
                cubicTo(c.x + s * 0.7f, c.y - s * 1.3f, c.x + s * 1.6f, c.y - s * 0.2f, c.x, c.y + s)
            }
            drawPath(p, col)
        }
        Glyph.PLUS -> {
            drawRect(col, o(-0.3f, -1f), Size(s * 0.6f, s * 2f))
            drawRect(col, o(-1f, -0.3f), Size(s * 2f, s * 0.6f))
        }
        Glyph.ARROW -> {
            val p = Path().apply { moveTo(c.x - s, c.y + s * 0.4f); lineTo(c.x, c.y - s * 0.6f); lineTo(c.x + s, c.y + s * 0.4f) }
            drawPath(p, col, style = Stroke(s * 0.35f, cap = StrokeCap.Round, join = StrokeJoin.Round))
        }
        Glyph.NOTE -> for (k in 1..3) {
            val r = s * 0.6f * k
            drawArc(col.copy(alpha = col.alpha * (1.1f - k * 0.25f)), rot * 57.3f - 40f, 80f, false,
                Offset(c.x - r, c.y - r), Size(r * 2, r * 2), style = Stroke(s * 0.22f, cap = StrokeCap.Round))
        }
        Glyph.EYE -> {
            val p = Path().apply {
                moveTo(c.x - s * 1.6f, c.y)
                quadraticTo(c.x, c.y - s * 1.3f, c.x + s * 1.6f, c.y)
                quadraticTo(c.x, c.y + s * 1.3f, c.x - s * 1.6f, c.y)
            }
            drawPath(p, white.copy(alpha = a * 0.9f))
            drawCircle(col, s * 0.6f, c)
            drawCircle(Color.Black.copy(alpha = a), s * 0.28f, c)
            drawPath(p, col, style = Stroke(s * 0.15f))
        }
        Glyph.SHIELD -> {
            val p = Path()
            for (k in 0 until 6) {
                val ang = k * PI.toFloat() / 3 + PI.toFloat() / 6
                val q = c + Offset(cos(ang), sin(ang)) * s * 1.5f
                if (k == 0) p.moveTo(q.x, q.y) else p.lineTo(q.x, q.y)
            }
            p.close()
            drawPath(p, col.copy(alpha = col.alpha * 0.25f))
            drawPath(p, col, style = Stroke(s * 0.18f, join = StrokeJoin.Round))
        }
    }
}
