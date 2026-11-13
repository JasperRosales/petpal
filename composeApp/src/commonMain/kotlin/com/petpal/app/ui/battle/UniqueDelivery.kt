package com.petpal.app.ui.battle

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.unit.dp
import com.petpal.app.domain.battle.Effect
import com.petpal.app.domain.battle.Element
import com.petpal.app.domain.battle.HitShape
import com.petpal.app.domain.battle.Move
import com.petpal.app.domain.battle.MoveCategory
import com.petpal.app.domain.battle.shape
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin

/**
 * How a unique move gets to its target: something thrown, something summoned, something that falls
 * from the sky or erupts from the ground. Drawn on top of the move's particles and set piece, so
 * every unique reads as its own little scene instead of just another blast.
 */
internal enum class Delivery {
    // thrown
    LOG, ROCKS, SNOWBALL, BANANA, FRUIT, TRASH, SEEDS, BAMBOO, SHELL, NET, DARTS, BUBBLES, POLLEN,
    // summoned / environmental
    SHARK, BIRDS, BATS, STAMPEDE, FISH, TREE_FALL, DIVE, BURROW, WAVE, VINES, HEDGE, SPIKES, ICICLES,
    SKY_BOLT, MOON, CLONES, SOUND, GAZE, FEATHERS, PAW, CLAWS, KICK, FIST, JAWS, TONGUE, TORNADO, VOLCANO, FIRE_RAIN, AFTERIMAGE,
    // on yourself
    GRAVE, BARRIER, ROCK_ARMOR, ICE_WALL, STAKES, BUBBLE_DOME, HEARTS, BUDDIES, CROWN, WAR_BANNER, RAGE, SPEED, MIRROR, PORTAL, BOLTS, SHOWER,
}

private val keywords: List<Pair<List<String>, Delivery>> = listOf(
    listOf("log toss", "timber trap") to Delivery.LOG,
    listOf("timber fall", "world tree", "great dam") to Delivery.TREE_FALL,
    listOf("banana") to Delivery.BANANA,
    listOf("yuzu") to Delivery.FRUIT,
    listOf("trash", "garbage", "scavenge", "grand larceny") to Delivery.TRASH,
    listOf("snowball", "ice ball") to Delivery.SNOWBALL,
    listOf("pebble", "sand spit", "mud flick", "sand kick", "bedrock") to Delivery.ROCKS,
    listOf("seed", "cheek stuff", "hoard") to Delivery.SEEDS,
    listOf("bamboo") to Delivery.BAMBOO,
    listOf("clam", "shell bash", "tidal shell") to Delivery.SHELL,
    listOf("tunnel net") to Delivery.NET,
    listOf("hedge maze", "thorn fortress") to Delivery.HEDGE,
    listOf("dart", "needle", "quill", "pin missile") to Delivery.DARTS,
    listOf("bubble", "gill", "regen") to Delivery.BUBBLES,
    listOf("pollen", "lullaby", "dream") to Delivery.POLLEN,
    listOf("shark", "megalodon", "feeding frenzy", "blood trail", "fin slice", "jaw lock") to Delivery.SHARK,
    listOf("flock", "squawk", "tropical", "rainbow", "flamingo waltz", "pink twister") to Delivery.BIRDS,
    listOf("bat", "dusk swarm", "echo shriek", "night flight", "vampire", "blood moon", "leech life", "sonar") to Delivery.BATS,
    listOf("stampede", "herd", "spirit", "pack", "rally bark", "mammoth") to Delivery.STAMPEDE,
    listOf("school rush", "fish fry", "nile flood", "mouth brood") to Delivery.FISH,
    listOf("dive", "phoenix", "aerial strike", "storm talon", "sky sovereign") to Delivery.DIVE,
    listOf("burrow", "drill", "tunnel", "lily leap", "seismic", "earth core") to Delivery.BURROW,
    listOf("tsunami", "wave", "tide", "flood", "torrent", "current", "riptide", "splash", "spring", "river", "lagoon") to Delivery.WAVE,
    listOf("vine", "root", "sticky", "swamp king", "jungle", "canopy", "gum leaf", "lotus", "algae", "bloom", "grove", "sacred") to Delivery.VINES,
    listOf("quake", "stomp", "tremor", "outback", "polar crash", "shell up", "spike roll", "dune", "tusk") to Delivery.SPIKES,
    listOf("blizzard", "cold snap", "frost", "arctic", "glacier", "iceberg", "ice fin", "ice floe", "polar splash") to Delivery.ICICLES,
    listOf("thunder", "storm", "volt", "static eye", "tremor spark", "live wire", "spark") to Delivery.SKY_BOLT,
    listOf("moon", "eclipse", "midnight", "night watch", "crescent") to Delivery.MOON,
    listOf("mirage", "illusion", "phantom", "trick", "kitsune", "mimic", "camouflage", "blend in", "monkey business", "masked", "prankster", "sneak") to Delivery.CLONES,
    listOf("roar", "growl", "howl", "shriek", "screech", "hoot", "bark", "croak", "trumpet", "cry", "echo talk", "rain song", "hiss", "chest beat") to Delivery.SOUND,
    listOf("eye", "glare", "gaze", "stare", "puppy", "keen", "wise", "color flash", "spectrum", "prism", "sun ray") to Delivery.GAZE,
    listOf("feather", "plumage", "wing", "talon", "updraft", "silent glide") to Delivery.FEATHERS,
    listOf("claw", "swipe", "slash", "stripe", "savage", "maul", "rampage") to Delivery.CLAWS,
    listOf("kick", "stilt", "outback") to Delivery.KICK,
    listOf("punch", "boxing", "palm", "pouch", "jab", "hammer", "fury") to Delivery.FIST,
    listOf("maul", "swipe", "paw", "palm", "punch", "kick", "slap", "smash", "hammer", "boxing", "pouch", "bear hug", "grizzly", "pinch", "claw", "nudge", "thump", "stilt") to Delivery.PAW,
    listOf("fang", "jaw", "bite", "chomp", "gnaw", "snatch", "frenzy") to Delivery.JAWS,
    listOf("tongue") to Delivery.TONGUE,
    listOf("wind", "twister", "tornado", "cyclone", "spin", "wheel", "roll") to Delivery.TORNADO,
    listOf("volcano", "magma", "molten", "basilisk", "dragon roar") to Delivery.VOLCANO,
    listOf("flame", "blaze", "fire", "wisp", "burst", "sunbathe") to Delivery.FIRE_RAIN,
)

private val selfKeywords: List<Pair<List<String>, Delivery>> = listOf(
    listOf("bedrock", "scale", "blubber", "curl", "shell up", "earth core", "molten") to Delivery.ROCK_ARMOR,
    listOf("iceberg", "huddle", "glacier") to Delivery.ICE_WALL,
    listOf("dam", "lodge", "bamboo wall", "thorn", "spiny", "hoard") to Delivery.STAKES,
    listOf("aqua", "bubble", "lotus", "zen float", "mouth brood", "regen") to Delivery.BUBBLE_DOME,
)

private fun Move.pickDelivery(): Delivery {
    val w = label.lowercase()
    if (isBuff) selfKeywords.firstOrNull { (k, _) -> k.any { it in w } }?.let { return it.second }
    if (isBuff) return when (effect) {
        Effect.SHIELD, Effect.HARDEN, Effect.FORTRESS, Effect.CITADEL, Effect.GUARD -> Delivery.BARRIER
        Effect.HEAL -> Delivery.HEARTS
        Effect.AID, Effect.PACK_CALL -> Delivery.BUDDIES
        Effect.ALPHA -> Delivery.CROWN
        Effect.BATTLE_CRY -> Delivery.WAR_BANNER
        Effect.RAGE -> Delivery.RAGE
        Effect.HASTE -> Delivery.SPEED
        Effect.COUNTER -> Delivery.MIRROR
        Effect.BLINK, Effect.SMOKE -> Delivery.PORTAL
        Effect.ENERGIZE -> Delivery.BOLTS
        Effect.CLEANSE -> Delivery.SHOWER
        Effect.SUMMON -> Delivery.GRAVE
        else -> Delivery.CROWN
    }
    keywords.firstOrNull { (k, _) -> k.any { it in w } }?.let { return it.second }
    return when (effect) {
        Effect.VANISH -> if (element == Element.GROUND) Delivery.BURROW else Delivery.DIVE
        Effect.LUNGE, Effect.TWIN_SLASH -> Delivery.AFTERIMAGE
        Effect.MULTI -> Delivery.DARTS
        Effect.CHAIN -> Delivery.SKY_BOLT
        Effect.PULL -> Delivery.TONGUE
        Effect.SWAP -> Delivery.PORTAL
        Effect.MARK, Effect.DISRUPT, Effect.PARALYZE -> Delivery.GAZE
        Effect.WEAKEN -> Delivery.SOUND
        Effect.FREEZE -> Delivery.ICICLES
        Effect.BURN -> Delivery.FIRE_RAIN
        Effect.LEECH, Effect.ROOT -> Delivery.VINES
        Effect.DRAIN, Effect.EXECUTE -> Delivery.JAWS
        Effect.TRAP -> Delivery.NET
        else -> when (element) {
            Element.WATER -> Delivery.WAVE
            Element.ICE -> Delivery.ICICLES
            Element.GRASS -> Delivery.VINES
            Element.GROUND -> Delivery.SPIKES
            Element.ELECTRIC -> Delivery.SKY_BOLT
            Element.FLYING -> Delivery.FEATHERS
            Element.FIRE -> Delivery.FIRE_RAIN
            else -> Delivery.PAW
        }
    }
}

private val deliveries: Map<Move, Delivery> by lazy {
    Move.entries.filter { it.category == MoveCategory.UNIQUE }.associateWith { it.pickDelivery() }
}

internal val Move.delivery: Delivery? get() = deliveries[this]

private const val TAU = (2 * PI).toFloat()
private const val AFTER_MS = 650f

private val Wood = Color(0xFF8D5A2B)
private val WoodLight = Color(0xFFC89156)
private val Stone = Color(0xFF8E8A84)
private val Snow = Color(0xFFF4FBFF)
private val Leaf = Color(0xFF5DBB4C)
private val Sea = Color(0xFF3D8BD9)
private val Foam = Color(0xFFE3F4FF)
private val Ink = Color(0xFF2B2B33)
private val Gold = Color(0xFFFFC940)
private val Pink = Color(0xFFFF8FB1)

/** Shared frame state for one delivery. [pre] runs 0..1 over the wind-up; [t] is 0..1 after the hit, or -1 before it. */
private class Shot(val o: Offset, val c: Offset, val pre: Float, val t: Float, val r: Float, val col: Color, val acc: Color, val seed: Int) {
    val v = c - o
    val dist = hypot(v.x, v.y)
    val dir = if (dist < 1f) Offset(1f, 0f) else v / dist
    val perp = Offset(-dir.y, dir.x)
    val aimDeg = atan2(dir.y, dir.x) * 180f / PI.toFloat()
    val hit get() = t >= 0f
    val fade get() = if (t < 0f) 1f else 1f - t
}

private fun lerp(a: Offset, b: Offset, t: Float) = Offset(a.x + (b.x - a.x) * t, a.y + (b.y - a.y) * t)
private fun ease(t: Float) = 1f - (1f - t).let { it * it * it }
private fun Color.a(x: Float) = copy(alpha = (alpha * x).coerceIn(0f, 1f))
private fun rnd(seed: Int): Float {
    var x = seed * 0x27d4eb2d + 0x165667b1
    x = x xor (x ushr 15)
    x *= 0x2c1b3c6d
    x = x xor (x ushr 12)
    return (x and 0xFFFF) / 65535f
}

/** Draws [m]'s delivery. [after] < 0 during the wind-up. Projectiles keep [impact] on the flying shot. */
internal fun DrawScope.drawDelivery(m: Move, origin: Offset, impact: Offset, after: Long) {
    val d = m.delivery ?: return
    if (after > AFTER_MS) return
    val wind = m.windupMs.coerceAtLeast(1).toFloat()
    val pre = if (after < 0) ((wind + after) / wind).coerceIn(0f, 1f) else 1f
    val t = if (after < 0) -1f else after / AFTER_MS
    val s = Shot(origin, impact, pre, t, 40.dp.toPx(), m.tint(), m.coreTint(), m.ordinal)
    val flying = m.shape == HitShape.PROJECTILE
    when (d) {
        Delivery.LOG -> toss(s, flying, 1, Wood) { p, rot, k -> log(p, s.r * 0.5f * k, rot) }
        Delivery.ROCKS -> toss(s, flying, 3, Stone) { p, rot, k -> rock(p, s.r * 0.18f * k, rot) }
        Delivery.SNOWBALL -> toss(s, flying, 1, Snow) { p, rot, k -> snowball(p, s.r * 0.3f * k, rot) }
        Delivery.BANANA -> toss(s, flying, 2, Color(0xFFFFE066)) { p, rot, k -> banana(p, s.r * 0.3f * k, rot) }
        Delivery.FRUIT -> toss(s, flying, 1, Color(0xFFFFB534)) { p, rot, k -> fruit(p, s.r * 0.26f * k, rot, Color(0xFFFFC233)) }
        Delivery.TRASH -> toss(s, flying, 3, Stone) { p, rot, k -> trash(p, s.r * 0.22f * k, rot, s.seed) }
        Delivery.SEEDS -> toss(s, flying, 5, Color(0xFF6D4C2F)) { p, rot, k -> seed(p, s.r * 0.12f * k, rot) }
        Delivery.BAMBOO -> toss(s, flying, if (s.seed % 2 == 0) 3 else 1, Leaf) { p, rot, k -> bamboo(p, s.r * 0.55f * k, rot) }
        Delivery.SHELL -> toss(s, flying, 1, Color(0xFFF2C9A0)) { p, rot, k -> shell(p, s.r * 0.3f * k, rot) }
        Delivery.NET -> net(s)
        Delivery.DARTS -> darts(s)
        Delivery.BUBBLES -> bubbles(s)
        Delivery.POLLEN -> pollen(s)
        Delivery.SHARK -> shark(s)
        Delivery.BIRDS -> birds(s)
        Delivery.BATS -> bats(s)
        Delivery.STAMPEDE -> stampede(s)
        Delivery.FISH -> fishSchool(s)
        Delivery.TREE_FALL -> treeFall(s)
        Delivery.DIVE -> dive(s)
        Delivery.BURROW -> burrow(s)
        Delivery.WAVE -> wave(s)
        Delivery.VINES -> vines(s)
        Delivery.HEDGE -> hedge(s)
        Delivery.SPIKES -> spikes(s)
        Delivery.ICICLES -> icicles(s)
        Delivery.SKY_BOLT -> skyBolt(s)
        Delivery.MOON -> moon(s)
        Delivery.CLONES -> clones(s)
        Delivery.SOUND -> sound(s)
        Delivery.GAZE -> gaze(s)
        Delivery.FEATHERS -> feathers(s)
        Delivery.PAW -> paw(s)
        Delivery.CLAWS -> claws(s)
        Delivery.KICK -> kick(s)
        Delivery.FIST -> fist(s)
        Delivery.ROCK_ARMOR -> rockArmor(s)
        Delivery.ICE_WALL -> iceWall(s)
        Delivery.STAKES -> stakes(s)
        Delivery.BUBBLE_DOME -> bubbleDome(s)
        Delivery.WAR_BANNER -> warBanner(s)
        Delivery.JAWS -> jaws(s)
        Delivery.TONGUE -> tongue(s)
        Delivery.TORNADO -> tornado(s)
        Delivery.VOLCANO -> volcano(s)
        Delivery.FIRE_RAIN -> fireRain(s)
        Delivery.AFTERIMAGE -> afterimage(s)
        Delivery.GRAVE -> grave(s)
        Delivery.BARRIER -> barrier(s)
        Delivery.HEARTS -> hearts(s)
        Delivery.BUDDIES -> buddies(s)
        Delivery.CROWN -> crown(s)
        Delivery.RAGE -> rage(s)
        Delivery.SPEED -> speed(s)
        Delivery.MIRROR -> mirror(s)
        Delivery.PORTAL -> portal(s)
        Delivery.BOLTS -> bolts(s)
        Delivery.SHOWER -> shower(s)
    }
}

// ---------- thrown objects ----------

/**
 * Wind-up: the object is hoisted over the thrower's head (projectiles) or lobbed in an arc that
 * lands on the hit (everything else). After the hit it rides the projectile or bursts into chips.
 */
private inline fun DrawScope.toss(s: Shot, flying: Boolean, n: Int, chip: Color, obj: DrawScope.(Offset, Float, Float) -> Unit) {
    val lift = Offset(0f, -s.r * 1.1f)
    if (!s.hit) {
        if (flying || s.dist < s.r) { // held aloft, wobbling, growing as the throw winds up
            for (i in 0 until n) {
                val off = Offset((i - (n - 1) / 2f) * s.r * 0.35f, 0f)
                obj(s.o + lift + off, sin(s.pre * 12f + i) * 25f, 0.4f + 0.6f * ease(s.pre))
            }
        } else {
            for (i in 0 until n) {
                val p = ((s.pre - i * 0.1f) / (1f - (n - 1) * 0.1f)).coerceIn(0f, 1f)
                if (p <= 0f) continue
                val side = s.perp * ((i - (n - 1) / 2f) * s.r * 0.25f * p)
                val ground = lerp(s.o, s.c, p) + side
                val h = sin(PI.toFloat() * p) * (s.r * 0.8f + s.dist * 0.35f)
                drawOval(Color.Black.a(0.18f * p), ground - Offset(s.r * 0.25f, -s.r * 0.35f), Size(s.r * 0.5f, s.r * 0.14f))
                obj(ground - Offset(0f, h), p * 540f + i * 40f, 1f)
            }
        }
        return
    }
    if (flying && s.t < 0.75f) { // riding the shot
        for (i in 0 until n) obj(s.c + s.perp * ((i - (n - 1) / 2f) * s.r * 0.3f), s.t * 900f + i * 30f, 1f)
        return
    }
    val k = if (flying) ((s.t - 0.75f) / 0.25f) else s.t
    if (k < 0.3f) obj(s.c, 0f, 1f - k * 2f) // squash on impact
    for (j in 0 until 8) {
        val a = j * TAU / 8 + rnd(s.seed + j) * 0.6f
        val dd = s.r * (0.3f + 1.1f * ease(k))
        val p = s.c + Offset(cos(a), sin(a) * 0.7f) * dd + Offset(0f, s.r * k * k * 0.8f)
        drawCircle(chip.a(1f - k), s.r * (0.09f - 0.05f * k), p)
    }
}

private fun DrawScope.log(c: Offset, len: Float, rot: Float) = rotate(rot, c) {
    val h = len * 0.38f
    drawRoundRect(Wood, c - Offset(len / 2, h / 2), Size(len, h), androidx.compose.ui.geometry.CornerRadius(h / 2))
    drawLine(Color(0xFF6B4220), c - Offset(len * 0.35f, h * 0.1f), c + Offset(len * 0.2f, -h * 0.1f), h * 0.08f)
    drawOval(WoodLight, c + Offset(len / 2 - h * 0.35f, -h / 2), Size(h * 0.7f, h))
    drawOval(Wood, c + Offset(len / 2 - h * 0.15f, -h * 0.22f), Size(h * 0.3f, h * 0.44f), style = Stroke(h * 0.06f))
}

private fun DrawScope.rock(c: Offset, r: Float, rot: Float) = rotate(rot, c) {
    drawCircle(Stone, r, c)
    drawCircle(Stone, r * 0.7f, c + Offset(r * 0.6f, -r * 0.2f))
    drawCircle(Color(0xFFB5B0A8), r * 0.35f, c + Offset(-r * 0.3f, -r * 0.35f))
}

private fun DrawScope.snowball(c: Offset, r: Float, rot: Float) {
    drawCircle(Snow, r, c)
    drawCircle(Color(0xFFBFD9EA), r, c, style = Stroke(r * 0.12f))
    rotate(rot, c) { drawCircle(Color.White, r * 0.3f, c + Offset(-r * 0.35f, -r * 0.35f)) }
}

private fun DrawScope.banana(c: Offset, r: Float, rot: Float) = rotate(rot, c) {
    drawArc(Color(0xFFFFD93B), 20f, 140f, false, c - Offset(r, r * 1.3f), Size(r * 2, r * 2), style = Stroke(r * 0.5f, cap = StrokeCap.Round))
    drawCircle(Color(0xFF6B4A1E), r * 0.14f, c + Offset(r * 0.95f, -r * 0.05f))
}

private fun DrawScope.fruit(c: Offset, r: Float, rot: Float, col: Color) {
    drawCircle(col, r, c)
    drawCircle(Color.White.a(0.5f), r * 0.28f, c + Offset(-r * 0.35f, -r * 0.35f))
    rotate(rot, c) { drawOval(Leaf, c + Offset(-r * 0.1f, -r * 1.35f), Size(r * 0.7f, r * 0.4f)) }
}

private fun DrawScope.trash(c: Offset, r: Float, rot: Float, seed: Int) = rotate(rot, c) {
    when (seed % 3) {
        0 -> { // tin can
            drawRect(Color(0xFFB0BEC5), c - Offset(r * 0.6f, r), Size(r * 1.2f, r * 2))
            drawRect(Color(0xFFE53935), c - Offset(r * 0.6f, r * 0.35f), Size(r * 1.2f, r * 0.7f))
        }
        1 -> { // bin bag
            drawCircle(Ink, r, c)
            drawLine(Ink, c - Offset(0f, r), c - Offset(r * 0.3f, r * 1.5f), r * 0.25f, StrokeCap.Round)
            drawLine(Ink, c - Offset(0f, r), c - Offset(-r * 0.3f, r * 1.5f), r * 0.25f, StrokeCap.Round)
        }
        else -> { // fish bone
            drawLine(Color.White, c - Offset(r, 0f), c + Offset(r, 0f), r * 0.18f, StrokeCap.Round)
            for (k in -1..1) drawLine(Color.White, c + Offset(k * r * 0.4f, -r * 0.5f), c + Offset(k * r * 0.4f, r * 0.5f), r * 0.12f)
            drawCircle(Color.White, r * 0.4f, c + Offset(r, 0f))
        }
    }
}

private fun DrawScope.seed(c: Offset, r: Float, rot: Float) = rotate(rot, c) {
    drawOval(Color(0xFF3E2C1C), c - Offset(r * 0.6f, r), Size(r * 1.2f, r * 2))
    drawLine(Color(0xFFF2E6C9), c - Offset(0f, r * 0.7f), c + Offset(0f, r * 0.7f), r * 0.25f)
}

private fun DrawScope.bamboo(c: Offset, len: Float, rot: Float) = rotate(rot, c) {
    val h = len * 0.18f
    drawRect(Color(0xFF7CB342), c - Offset(len / 2, h / 2), Size(len, h))
    for (k in 1..3) {
        val x = c.x - len / 2 + len * k / 4
        drawLine(Color(0xFF4E7D23), Offset(x, c.y - h / 2), Offset(x, c.y + h / 2), h * 0.25f)
    }
    drawOval(Leaf, c + Offset(len * 0.2f, -h * 1.6f), Size(len * 0.3f, h * 1.1f))
}

private fun DrawScope.shell(c: Offset, r: Float, rot: Float) = rotate(rot, c) {
    drawArc(Color(0xFFF2C9A0), 180f, 180f, true, c - Offset(r, r), Size(r * 2, r * 2))
    for (k in 0..4) {
        val a = PI.toFloat() + k * PI.toFloat() / 4
        drawLine(Color(0xFFC9946A), c, c + Offset(cos(a), sin(a)) * r, r * 0.1f)
    }
    drawLine(Color(0xFFC9946A), c - Offset(r, 0f), c + Offset(r, 0f), r * 0.14f)
}

/** A net that sails out, opens wide and drops over the target, staying pinned for a beat. */
private fun DrawScope.net(s: Shot) {
    val p = if (s.hit) 1f else s.pre
    val c = lerp(s.o, s.c, ease(p)) - Offset(0f, sin(PI.toFloat() * p) * s.r * 1.2f)
    val half = s.r * (0.3f + 0.6f * p)
    val col = Color(0xFFD7C49E).a(s.fade)
    rotate(if (s.hit) 0f else p * 200f, c) {
        for (k in -2..2) {
            val f = k / 2f * half
            drawLine(col, c + Offset(f, -half), c + Offset(f, half), 2.dp.toPx())
            drawLine(col, c + Offset(-half, f), c + Offset(half, f), 2.dp.toPx())
        }
        drawRect(Color(0xFF8D6E63).a(s.fade), c - Offset(half, half), Size(half * 2, half * 2), style = Stroke(3.dp.toPx()))
        for (k in 0..3) drawCircle(Stone.a(s.fade), 4.dp.toPx(), c + Offset(if (k % 2 == 0) -half else half, if (k < 2) -half else half))
    }
}

/** Darts or quills fired in a staggered volley. */
private fun DrawScope.darts(s: Shot) {
    val n = 6
    for (i in 0 until n) {
        val start = i * 0.08f
        val p = if (s.hit) 1f else ((s.pre - start) / 0.45f).coerceIn(0f, 1f)
        if (p <= 0f) continue
        val jitter = s.perp * ((rnd(s.seed + i) - 0.5f) * s.r * 0.9f)
        val head = lerp(s.o, s.c + jitter, p)
        val tail = head - s.dir * s.r * 0.45f
        val a = if (s.hit) s.fade else 1f
        drawLine(s.col.a(a), tail, head, 2.5f.dp.toPx(), StrokeCap.Round)
        drawLine(Leaf.a(a), tail, tail - s.dir * s.r * 0.1f + s.perp * s.r * 0.1f, 2.dp.toPx())
        drawLine(Leaf.a(a), tail, tail - s.dir * s.r * 0.1f - s.perp * s.r * 0.1f, 2.dp.toPx())
    }
}

private fun DrawScope.bubbles(s: Shot) {
    for (i in 0 until 7) {
        val off = rnd(s.seed + i * 7)
        val p = if (s.hit) 1f else ((s.pre - off * 0.4f) / 0.6f).coerceIn(0f, 1f)
        if (p <= 0f) continue
        val wob = sin(p * 9f + i) * s.r * 0.25f
        val c = lerp(s.o, s.c, p) + s.perp * wob + s.perp * ((off - 0.5f) * s.r * 0.8f)
        val r = s.r * (0.12f + 0.1f * off)
        if (s.hit) { // pop: a ring snaps outward and vanishes
            val k = (s.t * 2.5f - off * 0.5f).coerceIn(0f, 1f)
            drawCircle(Foam.a(1f - k), r * (1f + k), c, style = Stroke(2.dp.toPx() * (1f - k) + 0.5f))
        } else {
            drawCircle(Sea.a(0.25f), r, c)
            drawCircle(Foam, r, c, style = Stroke(1.5f.dp.toPx()))
            drawCircle(Color.White, r * 0.25f, c + Offset(-r * 0.35f, -r * 0.35f))
        }
    }
}

private fun DrawScope.pollen(s: Shot) {
    val cloud = lerp(s.o, s.c, if (s.hit) 1f else ease(s.pre))
    val spread = s.r * (0.5f + if (s.hit) s.t * 0.8f else 0f)
    for (i in 0 until 16) {
        val a = i * 2.4f + s.pre * 2f + (if (s.hit) s.t * 3f else 0f)
        val d = spread * (0.3f + rnd(s.seed + i))
        val p = cloud + Offset(cos(a) * d, sin(a) * d * 0.6f)
        drawCircle((if (i % 3 == 0) Pink else Gold).a(0.85f * s.fade), s.r * 0.06f, p)
    }
}

// ---------- summoned ----------

/** A fin circles the target during the wind-up, then the shark bursts out jaws-first. */
private fun DrawScope.shark(s: Shot) {
    if (!s.hit) {
        val a = s.pre * TAU * 1.5f
        val rr = s.r * (1.2f - 0.4f * s.pre)
        val p = s.c + Offset(cos(a) * rr, sin(a) * rr * 0.45f + s.r * 0.4f)
        drawArc(Sea.a(0.6f), 0f, 180f, false, p - Offset(s.r * 0.5f, s.r * 0.12f), Size(s.r, s.r * 0.25f), style = Stroke(2.dp.toPx()))
        val fin = Path().apply {
            moveTo(p.x - s.r * 0.25f, p.y); lineTo(p.x + s.r * 0.1f, p.y - s.r * 0.5f); lineTo(p.x + s.r * 0.25f, p.y); close()
        }
        drawPath(fin, Color(0xFF546E7A))
        return
    }
    val up = ease(minOf(1f, s.t * 3f))
    val c = s.c + Offset(0f, s.r * (0.6f - 0.9f * up))
    val gape = s.r * 0.55f * (1f - minOf(1f, s.t * 2.5f)).coerceAtLeast(0.05f)
    val a = s.fade
    drawOval(Color(0xFF607D8B).a(a), c - Offset(s.r * 0.75f, s.r * 0.65f), Size(s.r * 1.5f, s.r * 1.3f))
    drawArc(Color(0xFFB71C1C).a(a), 200f, 140f, true, c - Offset(s.r * 0.6f, gape + s.r * 0.2f), Size(s.r * 1.2f, gape * 2 + s.r * 0.4f))
    for (k in 0..4) {
        val x = c.x - s.r * 0.45f + k * s.r * 0.22f
        drawLine(Color.White.a(a), Offset(x, c.y - gape), Offset(x + s.r * 0.05f, c.y - gape + s.r * 0.16f), 3.dp.toPx())
        drawLine(Color.White.a(a), Offset(x, c.y + gape), Offset(x + s.r * 0.05f, c.y + gape - s.r * 0.16f), 3.dp.toPx())
    }
    drawCircle(Ink.a(a), s.r * 0.07f, c + Offset(s.r * 0.3f, -s.r * 0.35f))
    for (k in 0..6) { // splash
        val ang = PI.toFloat() + k * PI.toFloat() / 6
        drawCircle(Foam.a(a), s.r * 0.09f, s.c + Offset(cos(ang), sin(ang)) * s.r * (0.6f + s.t) + Offset(0f, s.r * 0.6f))
    }
}

private fun DrawScope.bird(c: Offset, size: Float, flap: Float, col: Color) {
    val w = 2.5f.dp.toPx()
    drawLine(col, c, c + Offset(-size, -size * flap), w, StrokeCap.Round)
    drawLine(col, c, c + Offset(size, -size * flap), w, StrokeCap.Round)
    drawCircle(col, w * 0.9f, c)
}

/** A flock swoops in from the sky behind the user and dives through the target. */
private fun DrawScope.birds(s: Shot) {
    val n = 5
    val sky = s.o - Offset(0f, s.r * 3f) - s.dir * s.r
    for (i in 0 until n) {
        val lag = i * 0.06f
        val p = if (s.hit) (1f + s.t * 0.6f) else ((s.pre - lag) / (1f - lag)).coerceIn(0f, 1f)
        val form = Offset((i - n / 2) * s.r * 0.35f, abs(i - n / 2) * s.r * 0.25f)
        val pos = if (p <= 1f) lerp(sky + form, s.c + form * 0.3f, ease(p)) else s.c + form * 0.3f + Offset(0f, -s.r * 2f * (p - 1f)) + s.dir * s.r * 3f * (p - 1f)
        bird(pos, s.r * 0.28f, 0.2f + 0.6f * abs(sin((s.pre + s.t) * 18f + i)), s.col.a(s.fade))
    }
}

private fun DrawScope.bat(c: Offset, size: Float, flap: Float, col: Color) {
    drawCircle(col, size * 0.3f, c)
    for (side in listOf(-1f, 1f)) {
        val tip = c + Offset(side * size, -size * flap)
        drawLine(col, c, tip, size * 0.22f, StrokeCap.Round)
        drawLine(col, tip, c + Offset(side * size * 0.55f, size * 0.25f), size * 0.18f, StrokeCap.Round)
        drawLine(col, c + Offset(side * size * 0.55f, size * 0.25f), c + Offset(side * size * 0.2f, size * 0.05f), size * 0.18f, StrokeCap.Round)
    }
    drawCircle(Color(0xFFFF5252), size * 0.06f, c + Offset(-size * 0.1f, -size * 0.05f))
    drawCircle(Color(0xFFFF5252), size * 0.06f, c + Offset(size * 0.1f, -size * 0.05f))
}

/** A swarm gathers around the user, then pours onto the target and scatters. */
private fun DrawScope.bats(s: Shot) {
    for (i in 0 until 6) {
        val a = i * TAU / 6 + (s.pre + s.t) * 6f
        val home = s.o + Offset(cos(a) * s.r * 1.1f, sin(a) * s.r * 0.6f - s.r * 0.5f)
        val pos = if (!s.hit) home * 1f
        else {
            val k = ease(minOf(1f, s.t * 2.2f))
            lerp(home, s.c, k) + Offset(cos(a), sin(a)) * s.r * 1.4f * maxOf(0f, s.t * 2f - 1f)
        }
        bat(pos, s.r * 0.32f, 0.3f + 0.5f * abs(sin((s.pre + s.t) * 24f + i)), Color(0xFF3B2A4A).a(s.fade * (if (s.hit) 1f else s.pre)))
    }
}

private fun DrawScope.beast(c: Offset, size: Float, phase: Float, col: Color, facing: Float) {
    drawOval(col, c - Offset(size * 0.7f, size * 0.35f), Size(size * 1.4f, size * 0.7f))
    val head = c + Offset(facing * size * 0.75f, -size * 0.35f)
    drawCircle(col, size * 0.32f, head)
    drawLine(col, head + Offset(-facing * size * 0.05f, -size * 0.25f), head + Offset(-facing * size * 0.2f, -size * 0.55f), size * 0.12f, StrokeCap.Round)
    for (k in 0..3) {
        val x = c.x + (k - 1.5f) * size * 0.35f
        val swing = sin(phase + k * 1.6f) * size * 0.25f
        drawLine(col, Offset(x, c.y + size * 0.2f), Offset(x + swing, c.y + size * 0.65f), size * 0.13f, StrokeCap.Round)
    }
}

/** Ghostly herd charges out from behind the user and tramples straight through the target. */
private fun DrawScope.stampede(s: Shot) {
    val facing = if (s.dir.x >= 0f) 1f else -1f
    for (i in 0 until 3) {
        val lane = s.perp * ((i - 1) * s.r * 0.6f)
        val p = if (!s.hit) s.pre * 0.6f - i * 0.08f else 0.6f + s.t * 0.9f - i * 0.08f
        if (p < 0f) continue
        val start = s.o - s.dir * s.r * 1.5f
        val pos = start + (s.c + s.dir * s.r * 2f - start) * p + lane
        val a = if (!s.hit) minOf(1f, s.pre * 3f) else s.fade
        beast(pos, s.r * 0.6f, (s.pre + s.t) * 30f + i, s.col.a(0.55f * a), facing)
        drawLine(Color.White.a(0.3f * a), pos - s.dir * s.r * 0.8f, pos - s.dir * s.r * 1.6f, 3.dp.toPx(), StrokeCap.Round)
    }
    if (s.hit) for (k in 0..5) { // dust kicked up
        val p = s.c + Offset((rnd(s.seed + k) - 0.5f) * s.r * 2f, s.r * 0.5f - s.t * s.r * 0.4f)
        drawCircle(Color(0xFFBCAAA4).a(0.6f * s.fade), s.r * (0.12f + s.t * 0.15f), p)
    }
}

private fun DrawScope.fish(c: Offset, size: Float, ang: Float, col: Color) = rotate(ang, c) {
    drawOval(col, c - Offset(size * 0.6f, size * 0.28f), Size(size * 1.2f, size * 0.56f))
    drawLine(col, c - Offset(size * 0.55f, 0f), c - Offset(size * 0.95f, size * 0.3f), size * 0.18f, StrokeCap.Round)
    drawLine(col, c - Offset(size * 0.55f, 0f), c - Offset(size * 0.95f, -size * 0.3f), size * 0.18f, StrokeCap.Round)
    drawCircle(Ink, size * 0.06f, c + Offset(size * 0.32f, -size * 0.06f))
}

/** A school of fish streams in a weaving ribbon from the user into the target. */
private fun DrawScope.fishSchool(s: Shot) {
    for (i in 0 until 8) {
        val p = if (!s.hit) (s.pre * 1.1f - i * 0.07f) else 1f + s.t * 0.5f - i * 0.07f
        if (p < 0f) continue
        val wob = sin(p * 10f + i * 0.8f) * s.r * 0.35f
        val pos = lerp(s.o, s.c, p.coerceAtMost(1.3f)) + s.perp * wob
        val a = if (s.hit) s.fade else 1f
        fish(pos, s.r * 0.3f, s.aimDeg + cos(p * 10f + i * 0.8f) * 30f, (if (i % 2 == 0) s.col else s.acc).a(a))
    }
}

/** A tree grows up beside the target and topples onto it. */
private fun DrawScope.treeFall(s: Shot) {
    val side = if (s.dir.x >= 0f) 1f else -1f
    val base = s.c + Offset(side * s.r * 1.1f, s.r * 0.6f)
    val grow = if (s.hit) 1f else ease(minOf(1f, s.pre * 1.6f))
    val tip = when {
        !s.hit -> (s.pre - 0.6f).coerceAtLeast(0f) / 0.4f * 30f
        else -> 30f + 60f * ease(minOf(1f, s.t * 3f))
    }
    val h = s.r * 2.2f * grow
    val a = s.fade
    rotate(-side * tip, base) {
        drawRect(Wood.a(a), base - Offset(s.r * 0.14f, h), Size(s.r * 0.28f, h))
        for (k in 0..2) drawCircle(Leaf.a(a), s.r * 0.45f * grow, base - Offset((k - 1) * s.r * 0.35f, h + (if (k == 1) s.r * 0.2f else 0f)))
    }
    if (s.hit && s.t > 0.3f) for (k in 0..6) { // leaves and splinters fly on impact
        val ang = k * TAU / 7
        val d = s.r * (s.t - 0.3f) * 2.5f
        drawOval(Leaf.a(a), s.c + Offset(cos(ang), sin(ang) * 0.6f) * d, Size(s.r * 0.18f, s.r * 0.1f))
    }
}

/** A silhouette plummets from the sky onto the target, trailing fire or wind. */
private fun DrawScope.dive(s: Shot) {
    val sky = s.c - Offset(-s.dir.x * s.r * 2f, s.r * 5f)
    if (!s.hit) {
        if (s.pre < 0.45f) { // user rockets upward
            val k = s.pre / 0.45f
            val p = s.o - Offset(0f, s.r * 5f * ease(k))
            drawLine(s.col.a(1f - k), s.o, p, s.r * 0.25f * (1f - k), StrokeCap.Round)
        } else {
            val k = (s.pre - 0.45f) / 0.55f
            val p = lerp(sky, s.c, k * k)
            drawCircle(Color.Black.a(0.2f * k), s.r * (0.2f + 0.5f * k), s.c + Offset(0f, s.r * 0.5f)) // shadow grows
            for (j in 0..5) drawCircle(s.col.a(0.6f - j * 0.1f), s.r * (0.35f - j * 0.04f), lerp(p, sky, j * 0.06f))
            bird(p, s.r * 0.5f, -0.4f, s.acc)
        }
        return
    }
    val k = ease(minOf(1f, s.t * 2f))
    drawCircle(s.col.a(0.5f * s.fade), s.r * (0.6f + 1.4f * k), s.c, style = Stroke(6.dp.toPx() * s.fade + 1f))
    drawCircle(Color.White.a(0.7f * (1f - k)), s.r * 0.6f * (1f - k), s.c)
}

/** A dirt mound tunnels along the ground to the target, then erupts under it. */
private fun DrawScope.burrow(s: Shot) {
    val dirt = Color(0xFF8D6E63)
    val ground = Offset(0f, s.r * 0.55f)
    if (!s.hit) {
        val head = lerp(s.o, s.c, ease(s.pre)) + ground
        for (k in 0..8) {
            val p = lerp(s.o + ground, head, k / 8f)
            drawCircle(dirt.a(0.25f + 0.5f * k / 8f), s.r * (0.1f + 0.05f * (k % 2)), p)
        }
        drawArc(dirt, 180f, 180f, true, head - Offset(s.r * 0.35f, s.r * 0.25f), Size(s.r * 0.7f, s.r * 0.5f))
        return
    }
    for (k in 0..9) {
        val ang = -PI.toFloat() / 2 + (k - 4.5f) * 0.28f
        val d = s.r * 1.6f * ease(minOf(1f, s.t * 1.8f))
        val p = s.c + ground + Offset(cos(ang), sin(ang)) * d + Offset(0f, s.r * 2.5f * s.t * s.t)
        rock(p, s.r * 0.12f, k * 40f)
    }
    drawOval(dirt.a(s.fade), s.c + ground - Offset(s.r * 0.8f, s.r * 0.15f), Size(s.r * 1.6f, s.r * 0.3f))
}

/** A wall of water rises behind the user and rolls over the target. */
private fun DrawScope.wave(s: Shot) {
    val p = if (s.hit) 1f + s.t * 0.3f else s.pre
    val front = lerp(s.o - s.dir * s.r, s.c, ease(p.coerceAtMost(1f))) + s.dir * s.r * (p - 1f).coerceAtLeast(0f) * 2f
    val h = s.r * (0.6f + 1.2f * minOf(1f, s.pre * 1.5f))
    val a = s.fade
    rotate(s.aimDeg, front) {
        for (k in 0..3) {
            val back = Offset(-k * s.r * 0.35f, 0f)
            drawArc((if (k == 0) Foam else Sea).a(a * (0.9f - k * 0.15f)), 270f, 180f, false,
                front + back - Offset(s.r * 0.4f, h), Size(s.r * 0.8f, h * 2), style = Stroke(s.r * 0.22f, cap = StrokeCap.Round))
        }
        for (k in -2..2) drawCircle(Foam.a(a), s.r * 0.1f, front + Offset(s.r * 0.1f, k * h * 0.4f + sin(p * 12f + k) * 4.dp.toPx()))
    }
}

/** Vines burst out of the ground around the target and lash at it. */
private fun DrawScope.vines(s: Shot) {
    val grow = if (s.hit) 1f else ease(s.pre)
    for (i in 0 until 5) {
        val ang = i * TAU / 5 + 0.3f
        val base = s.c + Offset(cos(ang) * s.r * 0.9f, sin(ang) * s.r * 0.35f + s.r * 0.45f)
        var p = base
        val segs = 6
        for (k in 1..segs) {
            val f = k / segs.toFloat()
            if (f > grow) break
            val whip = sin(f * 5f + (s.pre + s.t) * 10f + i) * s.r * 0.2f * f
            val n = lerp(base, s.c - Offset(0f, s.r * 0.3f), f) + Offset(whip, -s.r * 0.6f * sin(f * PI.toFloat()))
            drawLine(Color(0xFF2E7D32).a(s.fade), p, n, s.r * 0.13f * (1.1f - f), StrokeCap.Round)
            if (k % 2 == 0) drawOval(Leaf.a(s.fade), n - Offset(s.r * 0.08f, s.r * 0.05f), Size(s.r * 0.18f, s.r * 0.1f))
            p = n
        }
    }
}

/** A ring of hedges sprouts around the target, boxing it in. */
private fun DrawScope.hedge(s: Shot) {
    val grow = if (s.hit) 1f else ease(s.pre)
    for (i in 0 until 10) {
        val ang = i * TAU / 10
        val p = s.c + Offset(cos(ang) * s.r * 1.1f, sin(ang) * s.r * 0.6f + s.r * 0.25f)
        drawCircle(Color(0xFF2E7D32).a(s.fade), s.r * 0.3f * grow, p)
        drawCircle(Leaf.a(s.fade), s.r * 0.17f * grow, p - Offset(s.r * 0.06f, s.r * 0.08f))
    }
}

/** Rock spikes erupt one after another along the ground from the user to the target. */
private fun DrawScope.spikes(s: Shot) {
    val n = 6
    val reach = if (s.hit) 1f else s.pre
    for (i in 0 until n) {
        val f = (i + 1) / n.toFloat()
        val rise = ((reach - f + 0.25f) / 0.25f).coerceIn(0f, 1f)
        if (rise <= 0f) continue
        val base = lerp(s.o, s.c, f) + Offset(0f, s.r * 0.5f) + s.perp * (if (i % 2 == 0) 1f else -1f) * s.r * 0.15f
        val h = s.r * (0.5f + 0.7f * f) * rise * (if (s.hit) s.fade else 1f)
        val w = s.r * 0.22f
        val path = Path().apply { moveTo(base.x - w, base.y); lineTo(base.x, base.y - h); lineTo(base.x + w, base.y); close() }
        drawPath(path, Color(0xFF8D6E63))
        drawLine(Color(0xFFBCAAA4), base - Offset(0f, h * 0.9f), base - Offset(w * 0.4f, 0f), 2.dp.toPx())
    }
}

/** Icicles form in the air over the target and rain down on the hit. */
private fun DrawScope.icicles(s: Shot) {
    for (i in 0 until 6) {
        val x = s.c.x + (i - 2.5f) * s.r * 0.35f
        val top = s.c.y - s.r * (2.6f + rnd(s.seed + i) * 0.6f)
        val y = if (!s.hit) top else top + (s.c.y - top) * ease(minOf(1f, s.t * 2.5f + i * 0.04f))
        val len = s.r * 0.55f * (if (s.hit) 1f else s.pre)
        val a = if (s.hit && s.t > 0.4f) (1f - s.t) / 0.6f else 1f
        val p = Path().apply { moveTo(x - len * 0.18f, y - len); lineTo(x, y); lineTo(x + len * 0.18f, y - len); close() }
        drawPath(p, Color(0xFFD7F1FF).a(a))
        drawLine(Color.White.a(a), Offset(x - len * 0.05f, y - len * 0.9f), Offset(x, y - len * 0.2f), 1.5f.dp.toPx())
    }
    if (s.hit && s.t > 0.35f) for (k in 0..7) {
        val ang = k * TAU / 8
        drawCircle(Snow.a(1f - s.t), s.r * 0.07f, s.c + Offset(cos(ang), sin(ang) * 0.5f) * s.r * (s.t * 1.4f))
    }
}

/** Storm clouds gather over the target and a bolt slams down. */
private fun DrawScope.skyBolt(s: Shot) {
    val cloud = s.c - Offset(0f, s.r * 2.6f)
    val g = if (s.hit) 1f else ease(s.pre)
    for (k in 0..4) drawCircle(Color(0xFF4A4F5C).a(0.85f * s.fade), s.r * (0.35f + 0.1f * (k % 2)) * g,
        cloud + Offset((k - 2) * s.r * 0.35f, if (k % 2 == 0) 0f else -s.r * 0.15f))
    if (!s.hit) {
        if ((s.pre * 10).toInt() % 3 == 0) drawCircle(Color(0xFFFFF59D).a(0.4f), s.r * 0.7f, cloud) // flicker inside
        return
    }
    if (s.t < 0.5f) {
        var p = cloud
        val steps = 6
        for (k in 1..steps) {
            val n = lerp(cloud, s.c, k / steps.toFloat()) + Offset((rnd(s.seed + k + (s.t * 8).toInt()) - 0.5f) * s.r * 0.6f, 0f)
            drawLine(Color.White, p, n, 5.dp.toPx(), StrokeCap.Round)
            drawLine(Color(0xFFFFEB3B), p, n, 2.5f.dp.toPx(), StrokeCap.Round)
            p = n
        }
        drawCircle(Color.White.a(1f - s.t * 2f), s.r * 0.8f, s.c)
    }
}

/** A huge moon rises behind the target; on the hit it flares and shines down a beam. */
private fun DrawScope.moon(s: Shot) {
    val rise = if (s.hit) 1f else ease(s.pre)
    val c = s.c - Offset(0f, s.r * (1.2f + 1.2f * rise))
    val a = s.fade
    val tint = if (s.col.red > 0.6f && s.col.green < 0.4f) Color(0xFFE53935) else Color(0xFFFFF5C4)
    drawCircle(Brush.radialGradient(listOf(tint.a(0.5f * a), Color.Transparent), c, s.r * 1.6f), s.r * 1.6f, c)
    drawCircle(tint.a(0.95f * a), s.r * 0.75f, c)
    drawCircle(Color.Black.a(0.12f * a), s.r * 0.15f, c + Offset(-s.r * 0.25f, -s.r * 0.15f))
    drawCircle(Color.Black.a(0.12f * a), s.r * 0.1f, c + Offset(s.r * 0.25f, s.r * 0.2f))
    if (s.hit) drawRect(Brush.verticalGradient(listOf(tint.a(0.5f * a), Color.Transparent), c.y, s.c.y + s.r),
        Offset(s.c.x - s.r * 0.5f, c.y), Size(s.r, s.c.y + s.r - c.y))
}

private fun DrawScope.ghost(c: Offset, size: Float, col: Color) {
    drawCircle(col, size * 0.55f, c)
    drawCircle(col, size * 0.2f, c + Offset(-size * 0.35f, -size * 0.5f))
    drawCircle(col, size * 0.2f, c + Offset(size * 0.35f, -size * 0.5f))
    drawCircle(Color.White.a(col.alpha), size * 0.09f, c + Offset(-size * 0.18f, -size * 0.08f))
    drawCircle(Color.White.a(col.alpha), size * 0.09f, c + Offset(size * 0.18f, -size * 0.08f))
}

/** Decoy copies of the user split off and close in on the target from different sides. */
private fun DrawScope.clones(s: Shot) {
    for (i in 0 until 3) {
        val ang = (i - 1) * 0.9f
        val side = s.dir * cos(ang) + s.perp * sin(ang)
        val spread = s.o + side * s.r * 1.6f
        val pos = if (!s.hit) lerp(s.o, spread, ease(s.pre)) else lerp(spread, s.c, ease(minOf(1f, s.t * 2.5f)))
        val a = if (!s.hit) 0.5f * s.pre else 0.5f * s.fade
        ghost(pos, s.r * 0.8f, s.col.a(a))
        if (s.hit && s.t > 0.35f) drawCircle(Color.White.a(1f - s.t), s.r * (s.t * 1.2f), s.c, style = Stroke(2.dp.toPx()))
    }
}

/** Sound waves bellow out of the user's mouth towards the target. */
private fun DrawScope.sound(s: Shot) {
    val phase = if (s.hit) 1f + s.t * 1.5f else s.pre
    for (k in 0 until 5) {
        val f = ((phase * 1.8f - k * 0.22f) % 1.2f).coerceAtLeast(0f)
        if (f <= 0f || f > 1f) continue
        val c = lerp(s.o, s.c, f)
        val rr = s.r * (0.4f + 0.9f * f)
        drawArc(s.col.a((1f - f) * s.fade), s.aimDeg - 40f, 80f, false, c - Offset(rr, rr), Size(rr * 2, rr * 2),
            style = Stroke(4.dp.toPx() * (1.2f - f), cap = StrokeCap.Round))
    }
    if (s.hit) for (k in 0..2) { // the target rattles
        val x = s.c.x + (k - 1) * s.r * 0.6f
        drawLine(s.col.a(s.fade), Offset(x, s.c.y - s.r * 1.1f), Offset(x + 6.dp.toPx(), s.c.y - s.r * 0.8f), 3.dp.toPx(), StrokeCap.Round)
    }
}

/** A giant eye opens over the target and fixes on it. */
private fun DrawScope.gaze(s: Shot) {
    val c = s.c - Offset(0f, s.r * 1.7f)
    val open = if (s.hit) (1f - s.t * 0.6f) else ease(s.pre)
    val w = s.r * 0.95f
    val h = s.r * 0.55f * open
    val a = s.fade
    drawOval(Color.White.a(0.95f * a), c - Offset(w, h), Size(w * 2, h * 2))
    drawOval(s.col.a(a), c - Offset(w, h), Size(w * 2, h * 2), style = Stroke(3.dp.toPx()))
    if (open > 0.2f) {
        drawCircle(s.col.a(a), s.r * 0.38f * open, c)
        drawOval(Ink.a(a), c - Offset(s.r * 0.07f, s.r * 0.3f * open), Size(s.r * 0.14f, s.r * 0.6f * open))
    }
    if (s.hit && s.t < 0.6f) drawLine(s.col.a(0.6f * (1f - s.t / 0.6f)), c, s.c, s.r * 0.5f, StrokeCap.Round)
}

/** Feathers fan out from the user and flutter down onto the target. */
private fun DrawScope.feathers(s: Shot) {
    for (i in 0 until 7) {
        val lag = i * 0.05f
        val p = if (s.hit) 1f else ((s.pre - lag) / (1f - lag)).coerceIn(0f, 1f)
        val spread = s.perp * ((i - 3) * s.r * 0.22f)
        val pos = lerp(s.o - Offset(0f, s.r), s.c + spread, ease(p)) + Offset(sin(p * 8f + i) * s.r * 0.2f, if (s.hit) s.t * s.r * 0.6f else 0f)
        val a = s.fade
        rotate(s.aimDeg + sin(p * 6f + i) * 40f, pos) {
            drawOval(s.acc.a(a), pos - Offset(s.r * 0.25f, s.r * 0.08f), Size(s.r * 0.5f, s.r * 0.16f))
            drawLine(s.col.a(a), pos - Offset(s.r * 0.3f, 0f), pos + Offset(s.r * 0.25f, 0f), 1.5f.dp.toPx())
        }
    }
}

/** A giant ghostly paw rears up over the target and slams down. */
private fun DrawScope.paw(s: Shot) {
    val hover = s.c - Offset(0f, s.r * 1.9f)
    val c = if (!s.hit) lerp(s.o - Offset(0f, s.r), hover, ease(s.pre)) else lerp(hover, s.c, ease(minOf(1f, s.t * 5f)))
    val size = s.r * (0.5f + 0.4f * (if (s.hit) 1f else s.pre))
    val a = if (s.hit && s.t > 0.3f) (1f - s.t) / 0.7f else 0.85f
    drawOval(s.col.a(a), c - Offset(size * 0.55f, size * 0.35f), Size(size * 1.1f, size * 0.85f))
    for (k in 0..3) {
        val ang = -PI.toFloat() / 2 + (k - 1.5f) * 0.55f
        drawCircle(s.col.a(a), size * 0.2f, c + Offset(cos(ang) * size * 0.72f, sin(ang) * size * 0.6f))
    }
    drawOval(s.acc.a(a * 0.7f), c - Offset(size * 0.3f, size * 0.15f), Size(size * 0.6f, size * 0.4f))
    if (s.hit && s.t > 0.15f) { // crater rings
        val k = (s.t - 0.15f) / 0.85f
        drawOval(Color.Black.a(0.25f * (1f - k)), s.c - Offset(s.r * (0.6f + k), s.r * 0.2f - s.r * 0.45f), Size(s.r * (1.2f + 2 * k), s.r * (0.4f + 0.4f * k)))
    }
}

/** Huge spectral jaws open around the target and snap shut on the hit. */
private fun DrawScope.jaws(s: Shot) {
    val gape = if (!s.hit) s.r * (0.3f + 0.7f * ease(s.pre)) else s.r * 1f * (1f - ease(minOf(1f, s.t * 6f)))
    val a = s.fade
    val col = s.col.a(0.85f * a)
    for (sign in listOf(-1f, 1f)) {
        val y = s.c.y + sign * (gape + s.r * 0.1f)
        drawArc(col, if (sign < 0) 180f else 0f, 180f, false, Offset(s.c.x - s.r, y - s.r * 0.35f), Size(s.r * 2, s.r * 0.7f),
            style = Stroke(5.dp.toPx(), cap = StrokeCap.Round))
        for (k in 0..4) {
            val x = s.c.x - s.r * 0.7f + k * s.r * 0.35f
            val p = Path().apply {
                moveTo(x - s.r * 0.1f, y); lineTo(x, y - sign * s.r * 0.3f); lineTo(x + s.r * 0.1f, y); close()
            }
            drawPath(p, Color.White.a(a))
        }
    }
    if (s.hit && s.t < 0.4f) drawCircle(Color.White.a(1f - s.t / 0.4f), s.r * 0.5f, s.c)
}

/** A long tongue whips out to the target and reels back. */
private fun DrawScope.tongue(s: Shot) {
    val reach = if (!s.hit) ease(s.pre) else 1f - ease(minOf(1f, s.t * 1.6f))
    val tip = lerp(s.o, s.c, reach)
    val mid = lerp(s.o, tip, 0.5f) + s.perp * sin(reach * PI.toFloat()) * s.r * 0.3f
    drawLine(Pink, s.o, mid, s.r * 0.16f, StrokeCap.Round)
    drawLine(Pink, mid, tip, s.r * 0.16f, StrokeCap.Round)
    drawCircle(Color(0xFFFF6B95), s.r * 0.18f, tip)
}

/** A twister spins up on the user and travels across to swallow the target. */
private fun DrawScope.tornado(s: Shot) {
    val p = if (s.hit) 1f else ease(s.pre)
    val base = lerp(s.o, s.c, p) + Offset(0f, s.r * 0.5f)
    val spin = (s.pre + s.t) * 20f
    val a = s.fade
    for (k in 0 until 6) {
        val f = k / 5f
        val w = s.r * (0.25f + 0.8f * f)
        val y = base.y - f * s.r * 2.2f
        val x = base.x + sin(spin + f * 3f) * s.r * 0.18f
        drawOval(s.col.a(0.55f * a), Offset(x - w, y - w * 0.2f), Size(w * 2, w * 0.4f), style = Stroke(3.dp.toPx()))
    }
    for (k in 0..3) { // debris orbiting
        val ang = spin * 1.5f + k * TAU / 4
        drawCircle(Color(0xFF9E9E9E).a(a), s.r * 0.07f, base + Offset(cos(ang) * s.r * 0.7f, -s.r * (0.6f + k * 0.35f) + sin(ang) * s.r * 0.15f))
    }
}

/** A small volcano pushes up under the target and erupts. */
private fun DrawScope.volcano(s: Shot) {
    val grow = if (s.hit) 1f else ease(s.pre)
    val base = s.c + Offset(0f, s.r * 0.6f)
    val h = s.r * 0.9f * grow
    val p = Path().apply {
        moveTo(base.x - s.r * 0.9f, base.y); lineTo(base.x - s.r * 0.25f, base.y - h)
        lineTo(base.x + s.r * 0.25f, base.y - h); lineTo(base.x + s.r * 0.9f, base.y); close()
    }
    drawPath(p, Color(0xFF5D4037).a(s.fade))
    drawLine(Color(0xFFFF7043).a(s.fade), base - Offset(s.r * 0.2f, h), base - Offset(0f, h * 0.2f), 4.dp.toPx(), StrokeCap.Round)
    if (s.hit) for (k in 0..8) {
        val ang = -PI.toFloat() / 2 + (k - 4) * 0.25f
        val v = s.r * (1.4f + rnd(s.seed + k) * 1.2f)
        val q = base - Offset(0f, h) + Offset(cos(ang) * v * s.t, sin(ang) * v * s.t + s.r * 3f * s.t * s.t)
        drawCircle((if (k % 2 == 0) Color(0xFFFF5722) else Gold).a(s.fade), s.r * 0.13f, q)
    }
}

/** Fireballs rain down around the target. */
private fun DrawScope.fireRain(s: Shot) {
    for (i in 0 until 5) {
        val land = s.c + Offset((rnd(s.seed + i) - 0.5f) * s.r * 1.6f, (rnd(s.seed + i * 3) - 0.5f) * s.r * 0.6f)
        val start = land - Offset(-s.r * 1.2f, s.r * 3f)
        val p = if (!s.hit) ((s.pre - 0.3f - i * 0.08f) / 0.5f).coerceIn(0f, 1f) else 1f
        if (p <= 0f) continue
        if (p < 1f || !s.hit) {
            val pos = lerp(start, land, p)
            drawLine(Color(0xFFFF7043).a(0.6f), pos, lerp(start, land, (p - 0.25f).coerceAtLeast(0f)), s.r * 0.18f, StrokeCap.Round)
            drawCircle(Gold, s.r * 0.14f, pos)
        }
        if (s.hit) {
            val k = (s.t * 1.5f - i * 0.05f).coerceIn(0f, 1f)
            drawCircle(Color(0xFFFF7043).a(1f - k), s.r * 0.4f * (0.3f + k), land, style = Stroke(3.dp.toPx()))
        }
    }
}

/** Fading after-images streak behind a dashing user. */
private fun DrawScope.afterimage(s: Shot) {
    val head = if (s.hit) s.c else lerp(s.o, s.c, ease(s.pre))
    for (k in 1..4) {
        val p = lerp(head, s.o, k * 0.18f)
        ghost(p, s.r * 0.75f, s.col.a(0.35f * (1f - k * 0.2f) * s.fade))
    }
    for (k in -1..1) drawLine(Color.White.a(0.5f * s.fade), head - s.dir * s.r * 0.6f + s.perp * (k * s.r * 0.35f),
        head - s.dir * s.r * 1.8f + s.perp * (k * s.r * 0.35f), 2.dp.toPx(), StrokeCap.Round)
}

// ---------- on yourself ----------

/** Hex plates fly in from all sides and lock into a barrier. */
private fun DrawScope.barrier(s: Shot) {
    val lock = if (s.hit) 1f else ease(s.pre)
    for (k in 0 until 6) {
        val ang = k * TAU / 6 + (1f - lock) * 1.5f
        val d = s.r * (1.05f + (1f - lock) * 1.5f)
        val p = s.o + Offset(cos(ang), sin(ang)) * d
        rotate(ang * 180f / PI.toFloat() + 90f, p) {
            drawRect(s.col.a(0.35f * s.fade), p - Offset(s.r * 0.32f, s.r * 0.12f), Size(s.r * 0.64f, s.r * 0.24f))
            drawRect(Color.White.a(0.8f * s.fade), p - Offset(s.r * 0.32f, s.r * 0.12f), Size(s.r * 0.64f, s.r * 0.24f), style = Stroke(2.dp.toPx()))
        }
    }
    if (s.hit && s.t < 0.5f) drawCircle(Color.White.a(0.8f - s.t * 1.6f), s.r * 1.2f, s.o, style = Stroke(4.dp.toPx()))
}

private fun DrawScope.heart(c: Offset, size: Float, col: Color) {
    drawCircle(col, size * 0.3f, c + Offset(-size * 0.25f, -size * 0.1f))
    drawCircle(col, size * 0.3f, c + Offset(size * 0.25f, -size * 0.1f))
    val p = Path().apply { moveTo(c.x - size * 0.53f, c.y); lineTo(c.x, c.y + size * 0.55f); lineTo(c.x + size * 0.53f, c.y); close() }
    drawPath(p, col)
}

private fun DrawScope.hearts(s: Shot) {
    val time = s.pre + (if (s.hit) s.t * 1.5f else 0f)
    for (i in 0 until 6) {
        val f = (time * 0.9f + i / 6f) % 1f
        val p = s.o + Offset(sin(f * 6f + i) * s.r * 0.6f + (i - 2.5f) * s.r * 0.18f, s.r * 0.4f - f * s.r * 1.6f)
        heart(p, s.r * 0.3f, Color(0xFFFF6B8B).a((1f - f) * s.fade))
    }
}

/** Little spirit friends appear and circle the user. */
private fun DrawScope.buddies(s: Shot) {
    for (i in 0 until 4) {
        val ang = i * TAU / 4 + (s.pre + s.t) * 5f
        val p = s.o + Offset(cos(ang) * s.r * 1.3f, sin(ang) * s.r * 0.5f + s.r * 0.2f)
        ghost(p, s.r * 0.45f, s.col.a(0.6f * s.fade * (if (s.hit) 1f else s.pre)))
    }
}

/** A crown descends onto the user's head and flashes. */
private fun DrawScope.crown(s: Shot) {
    val head = s.o - Offset(0f, s.r * 1.05f)
    val c = if (s.hit) head else lerp(head - Offset(0f, s.r * 2f), head, ease(s.pre))
    val w = s.r * 0.55f
    val p = Path().apply {
        moveTo(c.x - w, c.y); lineTo(c.x - w, c.y - w * 0.8f); lineTo(c.x - w * 0.5f, c.y - w * 0.35f)
        lineTo(c.x, c.y - w); lineTo(c.x + w * 0.5f, c.y - w * 0.35f); lineTo(c.x + w, c.y - w * 0.8f); lineTo(c.x + w, c.y); close()
    }
    drawPath(p, Gold.a(s.fade))
    drawCircle(Color(0xFFE53935).a(s.fade), w * 0.14f, c - Offset(0f, w * 0.35f))
    if (s.hit) for (k in 0 until 8) {
        val ang = k * TAU / 8
        drawLine(Gold.a(1f - s.t), c + Offset(cos(ang), sin(ang)) * w * (1.2f + s.t), c + Offset(cos(ang), sin(ang)) * w * (1.6f + s.t * 1.5f), 3.dp.toPx(), StrokeCap.Round)
    }
}

/** Anger mark and steam puffs. */
private fun DrawScope.rage(s: Shot) {
    val c = s.o + Offset(s.r * 0.55f, -s.r * 0.8f)
    val pulse = 1f + 0.2f * sin((s.pre + s.t) * 25f)
    val w = s.r * 0.22f * pulse
    for (k in 0..3) {
        val ang = k * TAU / 4 + PI.toFloat() / 4
        drawArc(Color(0xFFE53935).a(s.fade), ang * 180f / PI.toFloat() - 45f, 90f, false,
            c + Offset(cos(ang), sin(ang)) * w - Offset(w * 0.6f, w * 0.6f), Size(w * 1.2f, w * 1.2f), style = Stroke(3.dp.toPx(), cap = StrokeCap.Round))
    }
    for (k in 0..1) {
        val f = ((s.pre + s.t) * 1.5f + k * 0.5f) % 1f
        drawCircle(Color.White.a(0.6f * (1f - f) * s.fade), s.r * (0.12f + 0.15f * f), s.o + Offset((k * 2 - 1) * s.r * 0.5f, -s.r * (0.6f + f)))
    }
}

/** Speed lines stream past and a gust whirls at the feet. */
private fun DrawScope.speed(s: Shot) {
    val time = s.pre + s.t
    for (k in 0 until 6) {
        val f = (time * 3f + k / 6f) % 1f
        val y = s.o.y - s.r * 0.6f + k * s.r * 0.25f
        val x = s.o.x + s.r * (1.4f - 2.8f * f)
        drawLine(Color.White.a(0.7f * (1f - abs(f - 0.5f) * 2f) * s.fade), Offset(x, y), Offset(x + s.r * 0.6f, y), 2.dp.toPx(), StrokeCap.Round)
    }
    drawArc(s.col.a(0.6f * s.fade), time * 720f, 270f, false, s.o + Offset(-s.r * 0.8f, s.r * 0.35f), Size(s.r * 1.6f, s.r * 0.4f),
        style = Stroke(3.dp.toPx(), cap = StrokeCap.Round))
}

/** Mirror panels orbit and glint. */
private fun DrawScope.mirror(s: Shot) {
    for (k in 0 until 4) {
        val ang = k * TAU / 4 + (s.pre + s.t) * 3f
        val p = s.o + Offset(cos(ang) * s.r * 1.15f, sin(ang) * s.r * 0.55f)
        val path = Path().apply {
            moveTo(p.x, p.y - s.r * 0.3f); lineTo(p.x + s.r * 0.15f, p.y); lineTo(p.x, p.y + s.r * 0.3f); lineTo(p.x - s.r * 0.15f, p.y); close()
        }
        drawPath(path, Color(0xFFE1F5FE).a(0.75f * s.fade))
        drawLine(Color.White.a(s.fade), p - Offset(s.r * 0.05f, s.r * 0.2f), p + Offset(s.r * 0.03f, -s.r * 0.02f), 2.dp.toPx())
    }
}

/** A swirling portal opens under the user (and over the target if there is one). */
private fun DrawScope.portal(s: Shot) {
    fun ring(c: Offset, k: Float) {
        for (j in 0..2) drawArc(s.col.a((0.8f - j * 0.2f) * s.fade), (s.pre + s.t) * 600f + j * 120f, 200f, false,
            c - Offset(s.r * k * (1f - j * 0.15f), s.r * k * 0.35f), Size(s.r * k * 2 * (1f - j * 0.15f), s.r * k * 0.7f),
            style = Stroke(3.dp.toPx(), cap = StrokeCap.Round))
    }
    val k = if (s.hit) 1f else ease(s.pre)
    ring(s.o + Offset(0f, s.r * 0.45f), k)
    if (s.dist > s.r) ring(s.c + Offset(0f, s.r * 0.45f), k)
}

/** Lightning bolts arc in from all sides into the user. */
private fun DrawScope.bolts(s: Shot) {
    val time = s.pre + s.t
    for (k in 0 until 4) {
        val ang = k * TAU / 4 + 0.4f
        val f = (time * 2f + k * 0.25f) % 1f
        val from = s.o + Offset(cos(ang), sin(ang)) * s.r * (1.8f - 1.2f * f)
        val mid = lerp(from, s.o, 0.5f) + Offset(-sin(ang), cos(ang)) * s.r * 0.2f
        drawLine(Color(0xFFFFEB3B).a(s.fade), from, mid, 3.dp.toPx(), StrokeCap.Round)
        drawLine(Color(0xFFFFEB3B).a(s.fade), mid, lerp(from, s.o, 0.85f), 3.dp.toPx(), StrokeCap.Round)
    }
}

/** A cleansing shower pours over the user under a little rainbow. */
private fun DrawScope.shower(s: Shot) {
    val time = s.pre + s.t
    for (k in 0 until 8) {
        val f = (time * 2.2f + rnd(s.seed + k)) % 1f
        val x = s.o.x - s.r * 0.7f + k * s.r * 0.2f
        val y = s.o.y - s.r * 1.6f + f * s.r * 1.8f
        drawLine(Color(0xFF81D4FA).a(0.85f * s.fade), Offset(x, y), Offset(x, y + s.r * 0.18f), 2.dp.toPx(), StrokeCap.Round)
    }
    val rb = listOf(Color(0xFFE57373), Color(0xFFFFD54F), Color(0xFF81C784), Color(0xFF64B5F6))
    rb.forEachIndexed { i, cl ->
        val rr = s.r * (1.1f - i * 0.1f)
        drawArc(cl.a(0.55f * s.fade), 180f, 180f, false, s.o - Offset(rr, rr + s.r * 0.6f), Size(rr * 2, rr * 2), style = Stroke(3.dp.toPx()))
    }
}

/** Three huge claw gashes rake across the target. */
private fun DrawScope.claws(s: Shot) {
    if (!s.hit) { // claws glint over the user, raised
        for (k in 0..2) {
            val p = s.o + Offset((k - 1) * s.r * 0.25f, -s.r * (0.9f + 0.3f * s.pre))
            drawLine(Color.White.a(s.pre), p, p - Offset(-s.r * 0.08f, s.r * 0.3f), 3.dp.toPx(), StrokeCap.Round)
        }
        return
    }
    val draw = ease(minOf(1f, s.t * 4f))
    val a = s.fade
    for (k in 0..2) {
        val off = s.perp * ((k - 1) * s.r * 0.35f)
        val from = s.c + off + Offset(-s.r * 0.9f, -s.r * 0.9f)
        val to = s.c + off + Offset(s.r * 0.9f, s.r * 0.9f)
        val end = lerp(from, to, draw)
        drawLine(s.col.a(0.5f * a), from, end, 12.dp.toPx() * a, StrokeCap.Round)
        drawLine(Color.White.a(a), from, end, 4.dp.toPx() * a, StrokeCap.Round)
    }
}

/** A sweeping kick arc with a big boot-print flash. */
private fun DrawScope.kick(s: Shot) {
    val sweep = if (s.hit) 1f else ease(s.pre)
    val c = lerp(s.o, s.c, 0.6f)
    val rr = s.r * 1.2f
    drawArc(s.col.a(0.7f * s.fade), s.aimDeg - 120f, 200f * sweep, false, c - Offset(rr, rr), Size(rr * 2, rr * 2),
        style = Stroke(8.dp.toPx() * s.fade, cap = StrokeCap.Round))
    drawArc(Color.White.a(0.8f * s.fade), s.aimDeg - 120f, 200f * sweep, false, c - Offset(rr, rr), Size(rr * 2, rr * 2),
        style = Stroke(3.dp.toPx() * s.fade, cap = StrokeCap.Round))
    if (s.hit && s.t < 0.5f) {
        val k = s.t / 0.5f
        drawOval(Color.White.a(1f - k), s.c - Offset(s.r * 0.3f, s.r * 0.5f), Size(s.r * 0.6f, s.r))
        for (j in 0 until 6) {
            val ang = j * TAU / 6
            drawLine(s.acc.a(1f - k), s.c + Offset(cos(ang), sin(ang)) * s.r * (0.5f + k), s.c + Offset(cos(ang), sin(ang)) * s.r * (0.8f + k * 1.2f), 3.dp.toPx(), StrokeCap.Round)
        }
    }
}

/** A giant glowing fist winds back and rockets into the target. */
private fun DrawScope.fist(s: Shot) {
    val back = s.o - s.dir * s.r * 0.8f - Offset(0f, s.r * 0.4f)
    val c = if (!s.hit) lerp(s.o, back, ease(s.pre)) else lerp(back, s.c, ease(minOf(1f, s.t * 5f)))
    val size = s.r * 0.55f
    val a = if (s.hit && s.t > 0.3f) (1f - s.t) / 0.7f else 0.9f
    rotate(s.aimDeg, c) {
        drawRoundRect(s.col.a(a), c - Offset(size * 0.6f, size * 0.55f), Size(size * 1.2f, size * 1.1f), androidx.compose.ui.geometry.CornerRadius(size * 0.35f))
        for (k in 0..3) drawLine(s.acc.a(a), c + Offset(size * 0.25f, -size * 0.4f + k * size * 0.27f), c + Offset(size * 0.55f, -size * 0.4f + k * size * 0.27f), 2.dp.toPx(), StrokeCap.Round)
        drawRoundRect(s.acc.a(a), c - Offset(size * 0.75f, size * 0.3f), Size(size * 0.35f, size * 0.6f), androidx.compose.ui.geometry.CornerRadius(size * 0.1f))
    }
    if (s.hit) for (k in 0..3) drawLine(Color.White.a(0.6f * s.fade), c - s.dir * s.r * (0.6f + k * 0.2f) + s.perp * ((k - 1.5f) * s.r * 0.2f),
        c - s.dir * s.r * (1.3f + k * 0.2f) + s.perp * ((k - 1.5f) * s.r * 0.2f), 2.dp.toPx(), StrokeCap.Round)
}

/** Rock plates clamp onto the user like armour. */
private fun DrawScope.rockArmor(s: Shot) {
    val lock = if (s.hit) 1f else ease(s.pre)
    for (k in 0 until 5) {
        val ang = -PI.toFloat() / 2 + (k - 2) * 0.7f
        val d = s.r * (0.75f + (1f - lock) * 1.2f)
        val p = s.o + Offset(cos(ang) * d, sin(ang) * d * 0.9f + s.r * 0.15f)
        rock(p, s.r * 0.2f, k * 50f)
    }
    if (s.hit && s.t < 0.4f) drawCircle(Color(0xFFBCAAA4).a(1f - s.t / 0.4f), s.r * 1.1f, s.o, style = Stroke(5.dp.toPx()))
}

/** Ice blocks rise out of the ground in a ring around the user. */
private fun DrawScope.iceWall(s: Shot) {
    val rise = if (s.hit) 1f else ease(s.pre)
    for (k in 0 until 6) {
        val ang = k * TAU / 6 + 0.25f
        val base = s.o + Offset(cos(ang) * s.r * 1.1f, sin(ang) * s.r * 0.45f + s.r * 0.5f)
        val h = s.r * (0.6f + 0.25f * (k % 2)) * rise
        drawRect(Color(0xFFB3E5FC).a(0.75f * s.fade), base - Offset(s.r * 0.18f, h), Size(s.r * 0.36f, h))
        drawLine(Color.White.a(s.fade), base - Offset(s.r * 0.1f, h * 0.9f), base - Offset(-s.r * 0.05f, h * 0.4f), 2.dp.toPx())
    }
}

/** Sharpened stakes or logs hammer into the ground around the user. */
private fun DrawScope.stakes(s: Shot) {
    for (k in 0 until 7) {
        val ang = k * TAU / 7
        val base = s.o + Offset(cos(ang) * s.r * 1.15f, sin(ang) * s.r * 0.45f + s.r * 0.5f)
        val drop = (if (s.hit) 1f else ((s.pre - k * 0.06f) / 0.6f).coerceIn(0f, 1f))
        if (drop <= 0f) continue
        val h = s.r * 0.75f
        val top = base - Offset(0f, h) - Offset(0f, s.r * 2f * (1f - ease(drop)))
        drawRect(Wood.a(s.fade), Offset(top.x - s.r * 0.1f, top.y), Size(s.r * 0.2f, h))
        val tip = Path().apply { moveTo(top.x - s.r * 0.1f, top.y); lineTo(top.x, top.y - s.r * 0.18f); lineTo(top.x + s.r * 0.1f, top.y); close() }
        drawPath(tip, WoodLight.a(s.fade))
    }
}

/** A wobbling water bubble wraps the user. */
private fun DrawScope.bubbleDome(s: Shot) {
    val g = if (s.hit) 1f else ease(s.pre)
    val wob = sin((s.pre + s.t) * 14f) * 0.05f
    val rx = s.r * 1.2f * g * (1f + wob)
    val ry = s.r * 1.2f * g * (1f - wob)
    drawOval(Sea.a(0.18f * s.fade), s.o - Offset(rx, ry), Size(rx * 2, ry * 2))
    drawOval(Foam.a(0.9f * s.fade), s.o - Offset(rx, ry), Size(rx * 2, ry * 2), style = Stroke(3.dp.toPx()))
    drawArc(Color.White.a(0.8f * s.fade), 200f, 50f, false, s.o - Offset(rx * 0.8f, ry * 0.8f), Size(rx * 1.6f, ry * 1.6f), style = Stroke(3.dp.toPx(), cap = StrokeCap.Round))
}

/** A war banner is planted behind the user and snaps in the wind. */
private fun DrawScope.warBanner(s: Shot) {
    val side = -1f
    val base = s.o + Offset(side * s.r * 0.9f, s.r * 0.5f)
    val h = s.r * 2.2f * (if (s.hit) 1f else ease(s.pre))
    drawLine(Wood.a(s.fade), base, base - Offset(0f, h), 3.dp.toPx(), StrokeCap.Round)
    val top = base - Offset(0f, h)
    val wav = sin((s.pre + s.t) * 18f) * s.r * 0.1f
    val flag = Path().apply {
        moveTo(top.x, top.y); lineTo(top.x + s.r * 0.9f, top.y + s.r * 0.15f + wav)
        lineTo(top.x + s.r * 0.7f, top.y + s.r * 0.35f); lineTo(top.x + s.r * 0.9f, top.y + s.r * 0.55f + wav)
        lineTo(top.x, top.y + s.r * 0.6f); close()
    }
    drawPath(flag, s.col.a(s.fade))
    drawCircle(Gold.a(s.fade), s.r * 0.1f, top + Offset(s.r * 0.35f, s.r * 0.3f))
}

/** A dark circle cracks open beside the caster and bony hands claw their way out. */
private fun DrawScope.grave(s: Shot) {
    val spot = s.o + Offset(-s.r * 0.9f, s.r * 0.5f)
    val g = if (s.hit) 1f else ease(s.pre)
    val purple = Color(0xFF6A3D9A)
    drawOval(Color.Black.a(0.55f * s.fade), spot - Offset(s.r * 0.8f * g, s.r * 0.25f * g), Size(s.r * 1.6f * g, s.r * 0.5f * g))
    drawOval(purple.a(0.8f * s.fade), spot - Offset(s.r * 0.8f * g, s.r * 0.25f * g), Size(s.r * 1.6f * g, s.r * 0.5f * g), style = Stroke(3.dp.toPx()))
    val bone = Color(0xFFEDE6D3)
    for (k in 0..1) { // two hands reaching up
        val x = spot.x + (k * 2 - 1) * s.r * 0.35f
        val up = s.r * 0.7f * g * (if (s.hit) 1f - s.t * 0.5f else 1f)
        val wrist = Offset(x, spot.y - up)
        drawLine(bone.a(s.fade), Offset(x, spot.y), wrist, 4.dp.toPx(), StrokeCap.Round)
        for (j in -1..1) drawLine(bone.a(s.fade), wrist, wrist + Offset(j * s.r * 0.1f, -s.r * 0.18f), 2.5f.dp.toPx(), StrokeCap.Round)
    }
    for (k in 0 until 6) { // ghostly wisps swirling up
        val f = ((s.pre + s.t) * 1.4f + k / 6f) % 1f
        val p = spot + Offset(sin(f * 8f + k) * s.r * 0.5f, -f * s.r * 1.6f)
        drawCircle(purple.a((1f - f) * 0.7f * s.fade), s.r * 0.08f, p)
    }
}
