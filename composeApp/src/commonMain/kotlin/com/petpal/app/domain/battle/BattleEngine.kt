package com.petpal.app.domain.battle

import com.petpal.app.domain.model.*
import kotlin.math.hypot
import kotlin.math.roundToInt
import kotlin.random.Random

/** Arena is ARENA_W x ARENA_H in normalized units. */
const val ARENA_W = 1f
const val ARENA_H = 1.2f
const val BATTLE_MS = 90_000L
private const val ENERGY_PER_SEC = 6f

data class FighterInfo(
    val name: String,
    val species: Species,
    val stage: Stage,
    val level: Int = 1,
    val moves: List<Move> = emptyList(),
) {
    val passive get() = SpeciesKit.passive(species)
    /** Learned moves the species can really use at this level, plus the stage's unique. */
    val loadout: List<Move> = SpeciesKit.loadout(species, stage, level, moves)
}

/** Stat rules shared by both phones, so each can predict cooldowns and reach locally. */
object BattleStats {
    fun power(stage: Stage) = when (stage) {
        Stage.EGG -> 0.85f
        Stage.BABY -> 1.0f
        Stage.KID -> 1.15f
        Stage.ADULT -> 1.3f
    }

    fun maxHp(f: FighterInfo): Int {
        val base = 100 * power(f.stage)
        return (if (f.passive == Passive.THICK_FUR) base * 1.2f else base).roundToInt()
    }

    fun baseDamage(f: FighterInfo, m: Move): Float {
        val boost = when {
            f.passive == Passive.BLAZE && m.isFire -> 1.25f
            f.passive == Passive.ICE_BODY && m.element == Element.ICE -> 1.15f
            else -> 1f
        }
        return m.damage * power(f.stage) * boost
    }

    fun range(f: FighterInfo, m: Move) = m.range * if (f.passive == Passive.KEEN_EYE) 1.2f else 1f

    fun cooldown(f: FighterInfo, m: Move) = when {
        f.passive == Passive.QUICK_FEET -> (m.cooldownMs * 0.9f).toLong()
        f.passive == Passive.BURROWER && m.effect == Effect.VANISH -> (m.cooldownMs * 0.7f).toLong()
        else -> m.cooldownMs
    }

    fun speed(f: FighterInfo) = if (f.passive == Passive.QUICK_FEET) 0.25f else 0.2f
}

object Status {
    const val BURN = 1
    const val SLOW = 2
    const val STUN = 4
    const val RAGE = 8
    const val GUARD = 16
    const val HASTE = 32
    const val HARDEN = 64
    const val VANISH = 128
    const val ROOT = 256
    const val WEAKEN = 512
    const val PARALYZE = 1024
    const val LEECH = 2048
    const val COUNTER = 4096
    const val EVADE = 8192
}

/** Built-in backstep every pet has: a quick dash away from the opponent with brief invulnerability. */
object Evade {
    const val COOLDOWN_MS = 3000L
    const val DURATION_MS = 350L // dash time, also the i-frame window
    const val DISTANCE = 0.25f
}

/** How far each Fury Swipes step moves forward. */
const val TWIN_STEP = 0.09f

/** Smokescreen: a cloud left behind that slows enemies standing in or near it. */
object Smoke {
    const val RADIUS = 0.16f
    const val DURATION_MS = 4000L
}

data class SmokeSnap(val x: Float, val y: Float, val ageMs: Long)

data class FighterSnap(
    val x: Float,
    val y: Float,
    val hp: Int,
    val maxHp: Int,
    val action: Move?,
    val actionAgeMs: Long,
    val status: Int, // Status bit flags
    val hurtAgeMs: Long,
    val facing: Int, // 1 = right, -1 = left
    val energy: Int,
    val uniqueLeft: Int,
    val cooldowns: List<Long>, // remaining ms, same order as FighterInfo.loadout
    val evadeCdMs: Long = 0,
    val aimX: Float = x, // where the current move is aimed (target position when it was started)
    val aimY: Float = y,
    val originX: Float = x, // where the move was fired from
    val originY: Float = y,
    val projectileAgeMs: Long = -1, // flight time of a live projectile, -1 when none
) {
    fun has(flag: Int) = status and flag != 0
}

data class BattleSnapshot(
    val timeLeftMs: Long,
    val ended: Boolean,
    val winner: Int, // fighter index, -1 = draw or ongoing
    val fighters: List<FighterSnap>,
    val smokes: List<SmokeSnap> = emptyList(),
)

/**
 * Host-authoritative battle simulation. Pets wander on their own; players only pick moves,
 * so the skill is timing a move for when the opponent drifts into range.
 * [now] is ms since the battle started.
 */
class BattleEngine(
    val infos: List<FighterInfo>,
    private val rng: Random = Random.Default,
    /** Move previews: pets hold still, any move is allowed and the energy bar starts full. */
    private val demo: Boolean = false,
    val terrain: Terrain = Terrain.ARENA,
) {
    private inner class Fighter(val info: FighterInfo, var x: Float, var y: Float) {
        val maxHp = BattleStats.maxHp(info)
        var hp = maxHp.toFloat()
        var tx = x; var ty = y
        var nextWanderAt = 0L
        var action: Move? = null
        var actionAt = -10_000L
        var busyUntil = 0L
        var guardUntil = 0L
        var burnUntil = 0L
        var slowUntil = 0L
        var stunUntil = 0L
        var rageUntil = 0L
        var hasteUntil = 0L
        var hardenUntil = 0L
        var vanishUntil = 0L
        var rootUntil = 0L
        var weakenUntil = 0L
        var paralyzeUntil = 0L
        var leechUntil = 0L
        var counterUntil = 0L
        var evadeUntil = 0L
        var stepUntil = 0L // short forward step (Fury Swipes)
        var slashesLeft = 0
        var evadeReadyAt = 0L
        // enough for one elemental move right away
        var energy = if (demo) BattleRules.MAX_ENERGY.toFloat() else 35f + if (info.passive == Passive.CHEEK_POUCH) 30f else 0f
        var uniqueUsed = 0
        var hurtAt = -10_000L
        var leechBy = 0
        var knockedOut = false
        var snacked = false
        var pendingHit = false
        var lifeSaved = false
        var facing = 1
        var aimX = x; var aimY = y
        var originX = x; var originY = y
        var projectile: Move? = null
        var launchAt = 0L
        val readyAt = LongArray(Move.entries.size)
        val passive get() = info.passive
        val homeTurf = terrain.isHomeTurf(info)
    }

    /** 2 fighters = 1v1; 4 = duo vs duo (0,1 vs 2,3). */
    fun teamOf(i: Int) = i * 2 / infos.size
    private val f = infos.mapIndexed { i, info ->
        val perTeam = infos.size / 2
        val slot = i % perTeam
        val y = if (perTeam == 1) ARENA_H / 2 else ARENA_H * (0.3f + 0.4f * slot)
        Fighter(info, if (teamOf(i) == 0) 0.25f else 0.75f, y)
    }
    private val teams = IntArray(f.size) { teamOf(it) }
    private class Cloud(val team: Int, val x: Float, val y: Float, val at: Long)
    private val clouds = mutableListOf<Cloud>()
    private var lastTick = 0L
    var ended = false; private set
    /** Winning team (same as the fighter index in 1v1), -1 = draw or ongoing. */
    var winner = -1; private set

    /** Demo setup: puts fighter [i] at a spot and keeps it there. */
    fun place(i: Int, x: Float, y: Float) {
        f[i].x = x; f[i].y = y; f[i].tx = x; f[i].ty = y
    }

    private fun alive(i: Int) = f[i].hp > 0f
    fun isDown(i: Int) = !alive(i)

    /** Nearest standing enemy of [i] (falls back to any enemy once they're all down). */
    fun targetOf(i: Int): Int {
        val enemies = f.indices.filter { teams[it] != teams[i] }
        return enemies.filter { alive(it) }.minByOrNull { hypot(f[it].x - f[i].x, f[it].y - f[i].y) } ?: enemies.first()
    }
    private fun target(me: Fighter) = f[targetOf(f.indexOf(me))]
    private fun enemiesOf(me: Fighter): List<Fighter> {
        val t = teams[f.indexOf(me)]
        return f.indices.filter { teams[it] != t && alive(it) }.map { f[it] }
    }

    fun distance(i: Int) = targetOf(i).let { hypot(f[i].x - f[it].x, f[i].y - f[it].y) }
    fun hpFraction(i: Int) = f[i].hp / f[i].maxHp

    fun isReady(i: Int, m: Move, now: Long): Boolean {
        val me = f[i]
        return !ended && alive(i) && (demo || m in me.info.loadout) && now >= me.busyUntil && now >= me.stunUntil &&
            now >= me.readyAt[m.ordinal] && me.projectile == null &&
            (m.category != MoveCategory.SPECIAL || me.energy >= BattleRules.MAX_ENERGY) &&
            me.energy >= m.energyCost &&
            (m.category != MoveCategory.UNIQUE || me.uniqueUsed < BattleRules.UNIQUE_USES)
    }

    fun canEvade(i: Int, now: Long): Boolean {
        val me = f[i]
        return !ended && alive(i) && now >= me.evadeReadyAt && now >= me.stunUntil && now >= me.rootUntil && now >= me.vanishUntil
    }

    /**
     * Backs off away from the opponent, cancelling a move that's still winding up.
     * Hits and projectiles pass through while dashing. Returns an event line, or null if not allowed.
     */
    fun evade(i: Int, now: Long): String? {
        if (!canEvade(i, now)) return null
        val me = f[i]
        val opp = target(me)
        val m = me.action
        if (me.pendingHit && m != null && now < me.actionAt + m.windupMs) { // cancel the wind-up
            me.pendingHit = false
            me.actionAt = now - 10_000
        }
        dash(me, opp, now)
        me.evadeReadyAt = now + if (me.passive == Passive.FLEET) Evade.COOLDOWN_MS / 2 else Evade.COOLDOWN_MS
        return "${me.info.name} backs off!"
    }

    /** Quick dash straight away from [opp] with i-frames (Evade and Smokescreen). */
    private fun dash(me: Fighter, opp: Fighter, now: Long) {
        var dx = me.x - opp.x
        var dy = me.y - opp.y
        val d = hypot(dx, dy)
        if (d < 0.001f) { dx = -me.facing.toFloat(); dy = 0f } else { dx /= d; dy /= d }
        var tx = me.x + dx * Evade.DISTANCE
        var ty = me.y + dy * Evade.DISTANCE
        if (tx !in 0.08f..ARENA_W - 0.08f) { // backed into a side wall: slide along it instead
            ty += if (me.y < ARENA_H / 2) Evade.DISTANCE else -Evade.DISTANCE
        }
        if (ty !in 0.08f..ARENA_H - 0.08f) {
            tx += if (me.x < ARENA_W / 2) Evade.DISTANCE else -Evade.DISTANCE
        }
        me.tx = tx.coerceIn(0.08f, ARENA_W - 0.08f)
        me.ty = ty.coerceIn(0.08f, ARENA_H - 0.08f)
        me.nextWanderAt = now + Evade.DURATION_MS + 400
        me.evadeUntil = now + Evade.DURATION_MS
        me.busyUntil = maxOf(minOf(me.busyUntil, now), now + Evade.DURATION_MS)
    }

    /** True when an enemy attack is about to land near [i] (used by the bot). */
    fun threatened(i: Int, now: Long): Boolean {
        val me = f[i]
        return enemiesOf(me).any { opp ->
            val m = opp.action ?: return@any false
            val d = hypot(me.x - opp.x, me.y - opp.y)
            if (opp.projectile != null) d < BattleStats.range(opp.info, m)
            else opp.pendingHit && !m.isBuff && now >= opp.actionAt + m.windupMs / 3 &&
                d <= BattleStats.range(opp.info, m) * 1.1f
        }
    }

    /** Returns an event line, or null if the move isn't allowed right now. */
    fun command(i: Int, m: Move, now: Long): String? {
        if (!isReady(i, m, now)) return null
        val me = f[i]
        me.action = m
        me.actionAt = now
        val opp = target(me)
        me.aimX = opp.x; me.aimY = opp.y
        me.originX = me.x; me.originY = me.y
        var cdScale = if (now < me.hasteUntil) 0.7f else 1f
        if (now < me.paralyzeUntil) cdScale *= 1.5f
        me.readyAt[m.ordinal] = now + (BattleStats.cooldown(me.info, m) * cdScale).toLong()
        if (m.effect == Effect.VANISH) me.vanishUntil = now + m.windupMs
        if (m.category == MoveCategory.SPECIAL) me.energy = 0f
        me.energy -= m.energyCost
        if (m.category == MoveCategory.UNIQUE) me.uniqueUsed++
        me.busyUntil = now + m.windupMs + m.recoverMs
        me.pendingHit = true
        if (m.effect == Effect.TWIN_SLASH) { me.slashesLeft = 2; stepForward(me, opp, now, m.windupMs) }
        return "${me.info.name} uses ${m.label}!"
    }

    /** A short hop toward [opp] that never runs into it (each Fury Swipes slash). */
    private fun stepForward(me: Fighter, opp: Fighter, now: Long, ms: Long) {
        val dx = opp.x - me.x
        val dy = opp.y - me.y
        val d = hypot(dx, dy)
        val dist = minOf(TWIN_STEP, (d - PET_RADIUS * 1.4f).coerceAtLeast(0f))
        if (d > 0.001f) {
            me.tx = (me.x + dx / d * dist).coerceIn(0.05f, ARENA_W - 0.05f)
            me.ty = (me.y + dy / d * dist).coerceIn(0.05f, ARENA_H - 0.05f)
        }
        me.stepUntil = now + ms
        me.nextWanderAt = maxOf(me.nextWanderAt, now + ms + 300)
    }

    fun tick(now: Long): List<String> {
        if (ended) return emptyList()
        val dt = (now - lastTick).coerceIn(0, 200) / 1000f
        lastTick = now
        val events = mutableListOf<String>()

        clouds.removeAll { now - it.at >= Smoke.DURATION_MS }
        for (c in clouds) f.forEachIndexed { i, e ->
            if (teams[i] != c.team && alive(i) && e.passive != Passive.SLIPPERY &&
                hypot(e.x - c.x, e.y - c.y) <= Smoke.RADIUS + PET_RADIUS
            ) e.slowUntil = maxOf(e.slowUntil, now + 400)
        }

        f.forEachIndexed { i, me ->
            if (!alive(i)) return@forEachIndexed
            val opp = target(me)
            if (now < me.burnUntil) hurt(me, 2f * dt, now, events, silent = true)
            if (now < me.leechUntil && me.hp > 0) {
                hurt(me, 1.5f * dt, now, events, silent = true)
                val by = f[me.leechBy]
                if (by.hp > 0) by.hp = minOf(by.maxHp.toFloat(), by.hp + 1.5f * dt)
            }
            var charge = if (me.passive == Passive.PLAYFUL) ENERGY_PER_SEC * 1.3f else ENERGY_PER_SEC
            if (me.homeTurf) charge *= Terrain.HOME_ENERGY
            me.energy = minOf(BattleRules.MAX_ENERGY.toFloat(), me.energy + charge * dt)
            if (me.passive == Passive.PHOTOSYNTHESIS && me.hp > 0) me.hp = minOf(me.maxHp.toFloat(), me.hp + 0.6f * dt)
            if (me.passive == Passive.REGROW && me.hp > 0 && me.hp < me.maxHp / 2f) me.hp += 1.2f * dt
            move(me, opp, now, dt)
        }

        f.forEachIndexed { i, me ->
            val m = me.action ?: return@forEachIndexed
            if (!me.pendingHit || now < me.actionAt + m.windupMs) return@forEachIndexed
            me.pendingHit = false
            if (!alive(i)) return@forEachIndexed
            val opp = target(me)
            if (m.effect == Effect.VANISH) { // reappear right next to the target
                me.x = (opp.x - me.facing * 0.12f).coerceIn(0.05f, ARENA_W - 0.05f)
                me.y = opp.y
                me.vanishUntil = 0
            }
            if (m.effect == Effect.LUNGE || m.effect == Effect.VANISH || m.effect == Effect.TWIN_SLASH) {
                me.aimX = opp.x; me.aimY = opp.y
            }
            me.originX = me.x; me.originY = me.y
            when {
                m.isBuff -> selfEffect(me, opp, m, now, events)
                m.shape == HitShape.PROJECTILE -> { me.projectile = m; me.launchAt = now }
                else -> {
                    // Area moves can catch both enemies; the aimed-at one reports the miss.
                    val range = BattleStats.range(me.info, m)
                    val caught = enemiesOf(me).filter { e ->
                        now >= e.vanishUntil && m.hits(me.x, me.y, me.aimX, me.aimY, me.facing, e.x, e.y, range)
                    }
                    if (caught.isEmpty()) strike(me, opp, m, now, events, connected = false)
                    else caught.forEach { strike(me, it, m, now, events, connected = true) }
                }
            }
            if (m.effect == Effect.TWIN_SLASH && --me.slashesLeft > 0) { // second step + slash, replaying the wind-up
                me.pendingHit = true
                me.actionAt = now
                me.busyUntil = now + m.windupMs + m.recoverMs
                stepForward(me, opp, now, m.windupMs)
            }
            if (m.effect == Effect.RECHARGE) {
                me.stunUntil = now + 1500
                me.busyUntil = maxOf(me.busyUntil, now + 1500)
                events += "${me.info.name} must recharge"
            }
        }

        f.forEach { me -> flyProjectile(me, now, events) }

        f.indices.filter { !alive(it) && !f[it].knockedOut }.forEach {
            f[it].knockedOut = true
            f[it].action = null; f[it].projectile = null; f[it].pendingHit = false
            if (f.size > 2) events += "${f[it].info.name} is knocked out!"
        }
        val standing = (0..1).map { t -> f.indices.any { teams[it] == t && alive(it) } }
        if (!standing[0] || !standing[1]) {
            ended = true
            winner = when { standing[0] -> 0; standing[1] -> 1; else -> -1 }
        } else if (now >= BATTLE_MS) {
            ended = true
            val p = (0..1).map { t -> f.indices.filter { teams[it] == t }.sumOf { hpFraction(it).toDouble() } }
            winner = when { p[0] > p[1] -> 0; p[1] > p[0] -> 1; else -> -1 }
        }
        if (ended) events += when {
            winner < 0 -> "Time! It's a draw"
            f.size == 2 -> "${f[winner].info.name} wins!"
            else -> f.indices.filter { teams[it] == winner }.joinToString(" & ") { f[it].info.name } + " win!"
        }
        return events
    }

    private fun move(me: Fighter, opp: Fighter, now: Long, dt: Float) {
        if (me.hp <= 0f) return
        me.facing = if (opp.x >= me.x) 1 else -1
        if (now < me.stunUntil || now < me.vanishUntil || now < me.rootUntil) return
        val evading = now < me.evadeUntil || now < me.stepUntil
        val lunging = me.action?.effect == Effect.LUNGE && now - me.actionAt < (me.action?.windupMs ?: 0)
        if (evading) {
            Unit // heading for the spot picked in evade()
        } else if (lunging) {
            me.tx = opp.x; me.ty = opp.y
        } else if (demo) {
            me.tx = me.x; me.ty = me.y
        } else if (now >= me.nextWanderAt || hypot(me.tx - me.x, me.ty - me.y) < 0.02f) {
            if (rng.nextFloat() < 0.5f) { // drift toward the opponent so fights happen in the big arena
                me.tx = (opp.x + (rng.nextFloat() - 0.5f) * 0.6f).coerceIn(0.08f, ARENA_W - 0.08f)
                me.ty = (opp.y + (rng.nextFloat() - 0.5f) * 0.6f).coerceIn(0.08f, ARENA_H - 0.08f)
            } else {
                me.tx = rng.nextFloat() * (ARENA_W - 0.16f) + 0.08f
                me.ty = rng.nextFloat() * (ARENA_H - 0.16f) + 0.08f
            }
            me.nextWanderAt = now + 700 + rng.nextLong(1500)
        }
        var speed = when {
            now < me.evadeUntil -> Evade.DISTANCE * 1000f / Evade.DURATION_MS
            evading -> TWIN_STEP * 1000f / 150f // quick hop
            lunging -> 0.9f
            now < me.busyUntil -> 0.06f
            else -> BattleStats.speed(me.info)
        }
        if (!evading) {
            if (now < me.hasteUntil) speed *= 1.5f
            if (now < me.slowUntil) speed *= 0.5f
            if (now < me.paralyzeUntil) speed *= 0.7f
            if (me.homeTurf) speed *= Terrain.HOME_SPEED
        }
        val dx = me.tx - me.x
        val dy = me.ty - me.y
        val d = hypot(dx, dy)
        if (d > 0.001f) {
            val step = minOf(d, speed * dt)
            me.x = (me.x + dx / d * step).coerceIn(0.05f, ARENA_W - 0.05f)
            me.y = (me.y + dy / d * step).coerceIn(0.05f, ARENA_H - 0.05f)
        }
    }

    private fun selfEffect(me: Fighter, opp: Fighter, m: Move, now: Long, events: MutableList<String>) {
        fun heal(pct: Float) {
            val amount = me.maxHp * pct
            me.hp = minOf(me.maxHp.toFloat(), me.hp + amount)
            events += "${me.info.name} heals ${amount.roundToInt()} HP"
        }
        val name = me.info.name
        when (m.effect) {
            Effect.HEAL -> heal(0.18f)
            Effect.RAGE -> { me.rageUntil = now + 4000; events += "$name is fired up!" }
            Effect.GUARD -> me.guardUntil = now + 1200
            Effect.HASTE -> { me.hasteUntil = now + 5000; events += "$name speeds up!" }
            Effect.HARDEN -> { me.hardenUntil = now + 5000; events += "$name hardens up!" }
            Effect.BATTLE_CRY -> {
                me.rageUntil = now + 5000; me.hasteUntil = now + 5000
                events += "$name lets out a Battle Cry!"
            }
            Effect.PACK_CALL -> { me.rageUntil = now + 6000; heal(0.25f) }
            Effect.FORTRESS -> { me.guardUntil = now + 3000; heal(0.15f) }
            Effect.ALPHA -> {
                me.rageUntil = now + 8000; me.hasteUntil = now + 8000; heal(0.35f)
                events += "$name leads the pack!"
            }
            Effect.COUNTER -> { me.counterUntil = now + 1500; events += "$name braces to counter" }
            Effect.BLINK -> {
                me.x = if (opp.x > ARENA_W / 2) 0.1f else ARENA_W - 0.1f
                me.y = rng.nextFloat() * (ARENA_H - 0.2f) + 0.1f
                me.tx = me.x; me.ty = me.y
                events += "$name teleports away!"
            }
            Effect.CLEANSE -> {
                me.burnUntil = 0; me.slowUntil = 0; me.rootUntil = 0
                me.weakenUntil = 0; me.paralyzeUntil = 0; me.leechUntil = 0
                heal(0.10f)
            }
            Effect.ENERGIZE -> {
                me.energy = minOf(BattleRules.MAX_ENERGY.toFloat(), me.energy + 50f)
                events += "$name charges up!"
            }
            Effect.SMOKE -> {
                clouds += Cloud(teams[f.indexOf(me)], me.x, me.y, now)
                dash(me, opp, now)
                events += "$name hides behind a smokescreen!"
            }
            Effect.CITADEL -> { me.guardUntil = now + 4000; me.hardenUntil = now + 8000; heal(0.25f) }
            else -> Unit
        }
    }

    /** Moves a live projectile along its aim line; it hits the first time it overlaps the target's hurtbox. */
    /** Moves a live projectile along its aim line; it hits the first enemy whose hurtbox it overlaps. */
    private fun flyProjectile(me: Fighter, now: Long, events: MutableList<String>) {
        val m = me.projectile ?: return
        val range = BattleStats.range(me.info, m)
        val travelled = PROJECTILE_SPEED * (now - me.launchAt) / 1000f
        val (dx, dy) = aimDir(me.originX, me.originY, me.aimX, me.aimY, me.facing)
        val d = minOf(travelled, range)
        val px = me.originX + dx * d
        val py = me.originY + dy * d
        val hit = enemiesOf(me).firstOrNull { opp ->
            now >= opp.vanishUntil && now >= opp.evadeUntil && hypot(opp.x - px, opp.y - py) <= m.hitSize(range) + PET_RADIUS
        }
        if (hit != null || travelled >= range) {
            me.projectile = null
            strike(me, hit ?: target(me), m, now, events, connected = hit != null)
        }
    }

    private fun strike(me: Fighter, opp: Fighter, m: Move, now: Long, events: MutableList<String>, connected: Boolean) {
        val name = me.info.name
        val target = opp.info.name
        if (now < opp.vanishUntil) {
            events += "$target is out of reach!"
            return
        }
        if (now < opp.evadeUntil) { // dashed out of the way, whether or not it would have connected
            events += "$target evades ${m.label}!"
            return
        }
        if (!connected) {
            events += "$name's ${m.label} missed"
            return
        }
        if (opp.passive == Passive.TRICKSTER && rng.nextFloat() < 0.2f) {
            events += "$target dodged!"
            return
        }
        if (m.damage == 0) { // pure debuff
            debuff(opp, m, now, events, by = me)
            return
        }
        val hits = if (m.effect == Effect.MULTI) 2 + rng.nextInt(3) else 1
        var dmg = BattleStats.baseDamage(me.info, m) * hits * (0.85f + rng.nextFloat() * 0.3f)
        if (now < me.rageUntil) dmg *= 1.3f
        if (terrain.boosts(m)) dmg *= Terrain.MOVE_BOOST
        if (now < me.weakenUntil) dmg *= 0.7f
        if (me.passive == Passive.LOYAL_HEART && me.hp < me.maxHp / 2f) dmg *= 1.25f
        if (m.effect == Effect.EXECUTE && opp.hp < opp.maxHp * 0.3f) dmg *= 2f
        val guarded = now < opp.guardUntil
        if (guarded) dmg *= 0.4f
        if (now < opp.hardenUntil) dmg *= 0.6f
        if (opp.passive == Passive.HARD_SHELL) dmg *= 0.8f
        if (opp.passive == Passive.SUN_SCALES && (m.element == Element.FIRE || m.element == Element.ICE)) dmg *= 0.7f
        val dealt = dmg.roundToInt().coerceAtLeast(1)

        if (now < opp.counterUntil) {
            opp.counterUntil = 0
            hurt(opp, dealt / 2f, now, events)
            hurt(me, dealt.toFloat(), now, events)
            events += "$target counters ${m.label} for $dealt!"
            return
        }
        hurt(opp, dealt.toFloat(), now, events)
        if (me.passive == Passive.VAMPIRE) me.hp = minOf(me.maxHp.toFloat(), me.hp + dealt * 0.1f)
        if (me.passive == Passive.CHATTER && rng.nextFloat() < 0.25f) opp.weakenUntil = maxOf(opp.weakenUntil, now + 3000)
        if (opp.passive == Passive.SPIKY && m.shape == HitShape.MELEE) hurt(me, dealt * 0.2f, now, events, silent = true)
        if (me.passive == Passive.STICKY_PAWS) {
            val stolen = minOf(8f, opp.energy)
            opp.energy -= stolen
            me.energy = minOf(BattleRules.MAX_ENERGY.toFloat(), me.energy + stolen)
        }
        me.energy = minOf(BattleRules.MAX_ENERGY.toFloat(), me.energy + dealt * 1.5f)
        opp.energy = minOf(BattleRules.MAX_ENERGY.toFloat(), opp.energy + dealt * 0.8f)
        val times = if (hits > 1) " ($hits hits)" else ""
        events += "${m.label} hits $target for $dealt$times${if (guarded) " (guarded)" else ""}"

        when (m.effect) {
            Effect.DRAIN -> me.hp = minOf(me.maxHp.toFloat(), me.hp + dealt / 2f)
            Effect.BURN -> if (opp.passive != Passive.ICE_BODY) opp.burnUntil = now + if (me.passive == Passive.BLAZE) 6000 else 3000
            Effect.RECOIL -> {
                hurt(me, dealt / 4f, now, events)
                events += "$name is hurt by recoil"
            }
            Effect.LEECH -> { opp.leechUntil = now + 5000; opp.leechBy = f.indexOf(me) }
            Effect.SIPHON -> {
                val stolen = minOf(40f, opp.energy)
                opp.energy -= stolen
                me.energy = minOf(BattleRules.MAX_ENERGY.toFloat(), me.energy + stolen)
            }
            else -> debuff(opp, m, now, events, quiet = true)
        }
        when (m.effect) {
            Effect.KNOCKBACK -> knockback(me, opp, 0.15f)
            Effect.PULL -> knockback(me, opp, -minOf(0.15f, (hypot(me.x - opp.x, me.y - opp.y) - 0.1f).coerceAtLeast(0f)))
            else -> knockback(me, opp, 0.05f)
        }
    }

    /** Status effects on the target. Slippery is immune to the ones that stop movement. */
    private fun debuff(opp: Fighter, m: Move, now: Long, events: MutableList<String>, quiet: Boolean = false, by: Fighter? = null) {
        val immune = opp.passive == Passive.SLIPPERY
        val target = opp.info.name
        fun stun(ms: Long) {
            opp.stunUntil = now + ms
            opp.busyUntil = maxOf(opp.busyUntil, now + ms)
            opp.pendingHit = false
            opp.vanishUntil = 0
        }
        val text = when (m.effect) {
            Effect.SLOW -> if (immune) null else { opp.slowUntil = now + 3000; "$target is slowed" }
            Effect.STUN -> if (immune) null else { stun(800); null }
            Effect.FREEZE -> if (immune) null else { stun(1500); "$target is frozen!" }
            Effect.ROOT -> if (immune) null else { opp.rootUntil = now + 2500; "$target is trapped!" }
            Effect.PARALYZE -> if (immune) null else { opp.paralyzeUntil = now + 4000; "$target is paralyzed!" }
            Effect.WEAKEN -> { opp.weakenUntil = now + 5000; "$target is weakened" }
            Effect.LEECH -> { opp.leechUntil = now + 5000; if (by != null) opp.leechBy = f.indexOf(by); "$target is seeded" }
            else -> null
        }
        if (immune && m.damage == 0 && m.effect != Effect.WEAKEN) events += "$target slips free"
        else if (text != null && (!quiet || m.effect == Effect.FREEZE)) events += text
    }

    private fun hurt(who: Fighter, amount: Float, now: Long, events: MutableList<String>, silent: Boolean = false) {
        who.hp -= amount
        if (!silent) who.hurtAt = now
        if (who.hp <= 0f && who.passive == Passive.NINE_LIVES && !who.lifeSaved) {
            who.lifeSaved = true
            who.hp = 1f
            events += "${who.info.name} hangs on with Nine Lives!"
        }
        who.hp = who.hp.coerceAtLeast(0f)
        if (who.hp > 0f && who.passive == Passive.BAMBOO_SNACK && !who.snacked && who.hp < who.maxHp * 0.4f) {
            who.snacked = true
            who.hp = minOf(who.maxHp.toFloat(), who.hp + who.maxHp * 0.2f)
            events += "${who.info.name} munches a bamboo snack!"
        }
    }

    private fun knockback(me: Fighter, opp: Fighter, dist: Float) {
        val kx = opp.x - me.x
        val ky = opp.y - me.y
        val kd = hypot(kx, ky).coerceAtLeast(0.001f)
        opp.x = (opp.x + kx / kd * dist).coerceIn(0.05f, ARENA_W - 0.05f)
        opp.y = (opp.y + ky / kd * dist).coerceIn(0.05f, ARENA_H - 0.05f)
    }

    fun snapshot(now: Long) = BattleSnapshot(
        timeLeftMs = (BATTLE_MS - now).coerceAtLeast(0),
        ended = ended,
        winner = winner,
        smokes = clouds.map { SmokeSnap(it.x, it.y, now - it.at) },
        fighters = f.map { me ->
            var status = 0
            if (now < me.burnUntil) status = status or Status.BURN
            if (now < me.slowUntil) status = status or Status.SLOW
            if (now < me.stunUntil) status = status or Status.STUN
            if (now < me.rageUntil) status = status or Status.RAGE
            if (now < me.guardUntil) status = status or Status.GUARD
            if (now < me.hasteUntil) status = status or Status.HASTE
            if (now < me.hardenUntil) status = status or Status.HARDEN
            if (now < me.vanishUntil) status = status or Status.VANISH
            if (now < me.rootUntil) status = status or Status.ROOT
            if (now < me.weakenUntil) status = status or Status.WEAKEN
            if (now < me.paralyzeUntil) status = status or Status.PARALYZE
            if (now < me.leechUntil) status = status or Status.LEECH
            if (now < me.counterUntil) status = status or Status.COUNTER
            if (now < me.evadeUntil) status = status or Status.EVADE
            FighterSnap(
                x = me.x, y = me.y,
                hp = kotlin.math.ceil(me.hp).toInt(), maxHp = me.maxHp,
                action = me.action?.takeIf { now - me.actionAt < it.windupMs + 700 || me.projectile != null },
                actionAgeMs = now - me.actionAt,
                status = status,
                hurtAgeMs = now - me.hurtAt,
                facing = me.facing,
                energy = me.energy.toInt(),
                uniqueLeft = BattleRules.UNIQUE_USES - me.uniqueUsed,
                cooldowns = me.info.loadout.map { (me.readyAt[it.ordinal] - now).coerceAtLeast(0) },
                evadeCdMs = (me.evadeReadyAt - now).coerceAtLeast(0),
                aimX = me.aimX, aimY = me.aimY,
                originX = me.originX, originY = me.originY,
                projectileAgeMs = if (me.projectile != null) now - me.launchAt else -1,
            )
        },
    )
}

/** Simple practice opponent. */
object BattleBot {
    private val BUFFS = setOf(Effect.RAGE, Effect.HASTE, Effect.HARDEN, Effect.BATTLE_CRY, Effect.ENERGIZE)
    private val BIG_HEALS = setOf(Effect.PACK_CALL, Effect.FORTRESS, Effect.ALPHA, Effect.CITADEL)

    /** Back off from incoming attacks now and then, more often when hurt. */
    fun shouldEvade(engine: BattleEngine, me: Int, now: Long, rng: Random): Boolean =
        engine.canEvade(me, now) && engine.threatened(me, now) &&
            rng.nextFloat() < if (engine.hpFraction(me) < 0.4f) 0.12f else 0.06f

    fun pick(engine: BattleEngine, me: Int, now: Long, rng: Random): Move? {
        if (rng.nextFloat() > 0.06f) return null // think roughly 1x/second at 20 ticks/s
        val info = engine.infos[me]
        val ready = info.loadout.filter { engine.isReady(me, it, now) }
        val hp = engine.hpFraction(me)
        ready.firstOrNull { it.effect == Effect.HEAL && hp < 0.45f }?.let { return it }
        ready.firstOrNull { it.effect in BIG_HEALS && hp < 0.4f }?.let { return it }
        ready.firstOrNull { it.effect in BUFFS && rng.nextFloat() < 0.25f }?.let { return it }
        val d = engine.distance(me)
        ready.firstOrNull { it.effect == Effect.SMOKE && d < 0.25f && rng.nextFloat() < 0.3f }?.let { return it }
        ready.firstOrNull { it.effect == Effect.COUNTER && d < 0.3f && rng.nextFloat() < 0.2f }?.let { return it }
        ready.firstOrNull { it.effect == Effect.BLINK && hp < 0.3f && d < 0.25f }?.let { return it }
        ready.firstOrNull { !it.isBuff && it.damage == 0 && d <= BattleStats.range(info, it) * 0.9f && rng.nextFloat() < 0.3f }
            ?.let { return it }
        return ready.filter { !it.isBuff && d <= BattleStats.range(info, it) * 0.9f }.maxByOrNull { it.damage }
            ?: Move.GUARD.takeIf { it in ready && d < 0.3f && rng.nextFloat() < 0.3f }
    }
}
