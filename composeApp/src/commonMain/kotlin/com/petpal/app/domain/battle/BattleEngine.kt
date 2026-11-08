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
        val blaze = if (f.passive == Passive.BLAZE && m.isFire) 1.25f else 1f
        return m.damage * power(f.stage) * blaze
    }

    fun range(f: FighterInfo, m: Move) = m.range * if (f.passive == Passive.KEEN_EYE) 1.2f else 1f

    fun cooldown(f: FighterInfo, m: Move) =
        if (f.passive == Passive.QUICK_FEET) (m.cooldownMs * 0.9f).toLong() else m.cooldownMs

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
}

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
) {
    fun has(flag: Int) = status and flag != 0
}

data class BattleSnapshot(
    val timeLeftMs: Long,
    val ended: Boolean,
    val winner: Int, // fighter index, -1 = draw or ongoing
    val fighters: List<FighterSnap>,
)

/**
 * Host-authoritative battle simulation. Pets wander on their own; players only pick moves,
 * so the skill is timing a move for when the opponent drifts into range.
 * [now] is ms since the battle started.
 */
class BattleEngine(val infos: List<FighterInfo>, private val rng: Random = Random.Default) {
    private class Fighter(val info: FighterInfo, var x: Float, var y: Float) {
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
        var energy = 0f
        var uniqueUsed = 0
        var hurtAt = -10_000L
        var pendingHit = false
        var lifeSaved = false
        var facing = 1
        val readyAt = LongArray(Move.entries.size)
        val passive get() = info.passive
    }

    private val f = infos.mapIndexed { i, info -> Fighter(info, if (i == 0) 0.25f else 0.75f, ARENA_H / 2) }
    private var lastTick = 0L
    var ended = false; private set
    var winner = -1; private set

    fun distance() = hypot(f[0].x - f[1].x, f[0].y - f[1].y)
    fun hpFraction(i: Int) = f[i].hp / f[i].maxHp

    fun isReady(i: Int, m: Move, now: Long): Boolean {
        val me = f[i]
        return !ended && m in me.info.loadout && now >= me.busyUntil && now >= me.stunUntil &&
            now >= me.readyAt[m.ordinal] &&
            (m.category != MoveCategory.SPECIAL || me.energy >= BattleRules.MAX_ENERGY) &&
            (m.category != MoveCategory.UNIQUE || me.uniqueUsed < BattleRules.UNIQUE_USES)
    }

    /** Returns an event line, or null if the move isn't allowed right now. */
    fun command(i: Int, m: Move, now: Long): String? {
        if (!isReady(i, m, now)) return null
        val me = f[i]
        me.action = m
        me.actionAt = now
        var cdScale = if (now < me.hasteUntil) 0.7f else 1f
        if (now < me.paralyzeUntil) cdScale *= 1.5f
        me.readyAt[m.ordinal] = now + (BattleStats.cooldown(me.info, m) * cdScale).toLong()
        if (m.effect == Effect.VANISH) me.vanishUntil = now + m.windupMs
        if (m.category == MoveCategory.SPECIAL) me.energy = 0f
        if (m.category == MoveCategory.UNIQUE) me.uniqueUsed++
        me.busyUntil = now + m.windupMs + m.recoverMs
        me.pendingHit = true
        return "${me.info.name} uses ${m.label}!"
    }

    fun tick(now: Long): List<String> {
        if (ended) return emptyList()
        val dt = (now - lastTick).coerceIn(0, 200) / 1000f
        lastTick = now
        val events = mutableListOf<String>()

        f.forEachIndexed { i, me ->
            val opp = f[1 - i]
            if (now < me.burnUntil) hurt(me, 2f * dt, now, events, silent = true)
            if (now < me.leechUntil && me.hp > 0) {
                hurt(me, 1.5f * dt, now, events, silent = true)
                opp.hp = minOf(opp.maxHp.toFloat(), opp.hp + 1.5f * dt)
            }
            me.energy = minOf(BattleRules.MAX_ENERGY.toFloat(), me.energy + ENERGY_PER_SEC * dt)
            if (me.passive == Passive.PHOTOSYNTHESIS && me.hp > 0) me.hp = minOf(me.maxHp.toFloat(), me.hp + 0.6f * dt)
            move(me, opp, now, dt)
        }

        f.forEachIndexed { i, me ->
            val m = me.action ?: return@forEachIndexed
            if (!me.pendingHit || now < me.actionAt + m.windupMs) return@forEachIndexed
            me.pendingHit = false
            val opp = f[1 - i]
            if (m.effect == Effect.VANISH) { // reappear right next to the target
                me.x = (opp.x - me.facing * 0.12f).coerceIn(0.05f, ARENA_W - 0.05f)
                me.y = opp.y
                me.vanishUntil = 0
            }
            if (m.isBuff) selfEffect(me, opp, m, now, events) else strike(me, opp, m, now, events)
            if (m.effect == Effect.RECHARGE) {
                me.stunUntil = now + 1500
                me.busyUntil = maxOf(me.busyUntil, now + 1500)
                events += "${me.info.name} must recharge"
            }
        }

        val down = f.indexOfFirst { it.hp <= 0f }
        if (down >= 0) {
            ended = true; winner = 1 - down
        } else if (now >= BATTLE_MS) {
            ended = true
            val p0 = hpFraction(0)
            val p1 = hpFraction(1)
            winner = when { p0 > p1 -> 0; p1 > p0 -> 1; else -> -1 }
        }
        if (ended) events += if (winner < 0) "Time! It's a draw" else "${f[winner].info.name} wins!"
        return events
    }

    private fun move(me: Fighter, opp: Fighter, now: Long, dt: Float) {
        me.facing = if (opp.x >= me.x) 1 else -1
        if (now < me.stunUntil || now < me.vanishUntil || now < me.rootUntil) return
        val lunging = me.action?.effect == Effect.LUNGE && now - me.actionAt < (me.action?.windupMs ?: 0)
        if (lunging) {
            me.tx = opp.x; me.ty = opp.y
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
            lunging -> 0.9f
            now < me.busyUntil -> 0.06f
            else -> BattleStats.speed(me.info)
        }
        if (now < me.hasteUntil) speed *= 1.5f
        if (now < me.slowUntil) speed *= 0.5f
        if (now < me.paralyzeUntil) speed *= 0.7f
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
            Effect.CITADEL -> { me.guardUntil = now + 4000; me.hardenUntil = now + 8000; heal(0.25f) }
            else -> Unit
        }
    }

    private fun strike(me: Fighter, opp: Fighter, m: Move, now: Long, events: MutableList<String>) {
        val name = me.info.name
        val target = opp.info.name
        if (now < opp.vanishUntil) {
            events += "$target is out of reach!"
            return
        }
        if (distance() > BattleStats.range(me.info, m)) {
            events += "$name's ${m.label} missed"
            return
        }
        if (opp.passive == Passive.TRICKSTER && rng.nextFloat() < 0.2f) {
            events += "$target dodged!"
            return
        }
        if (m.damage == 0) { // pure debuff
            debuff(opp, m, now, events)
            return
        }
        val hits = if (m.effect == Effect.MULTI) 2 + rng.nextInt(3) else 1
        var dmg = BattleStats.baseDamage(me.info, m) * hits * (0.85f + rng.nextFloat() * 0.3f)
        if (now < me.rageUntil) dmg *= 1.3f
        if (now < me.weakenUntil) dmg *= 0.7f
        if (me.passive == Passive.LOYAL_HEART && me.hp < me.maxHp / 2f) dmg *= 1.25f
        if (m.effect == Effect.EXECUTE && opp.hp < opp.maxHp * 0.3f) dmg *= 2f
        val guarded = now < opp.guardUntil
        if (guarded) dmg *= 0.4f
        if (now < opp.hardenUntil) dmg *= 0.6f
        if (opp.passive == Passive.HARD_SHELL) dmg *= 0.8f
        val dealt = dmg.roundToInt().coerceAtLeast(1)

        if (now < opp.counterUntil) {
            opp.counterUntil = 0
            hurt(opp, dealt / 2f, now, events)
            hurt(me, dealt.toFloat(), now, events)
            events += "$target counters ${m.label} for $dealt!"
            return
        }
        hurt(opp, dealt.toFloat(), now, events)
        me.energy = minOf(BattleRules.MAX_ENERGY.toFloat(), me.energy + dealt * 1.5f)
        opp.energy = minOf(BattleRules.MAX_ENERGY.toFloat(), opp.energy + dealt * 0.8f)
        val times = if (hits > 1) " ($hits hits)" else ""
        events += "${m.label} hits $target for $dealt$times${if (guarded) " (guarded)" else ""}"

        when (m.effect) {
            Effect.DRAIN -> me.hp = minOf(me.maxHp.toFloat(), me.hp + dealt / 2f)
            Effect.BURN -> opp.burnUntil = now + if (me.passive == Passive.BLAZE) 6000 else 3000
            Effect.RECOIL -> {
                hurt(me, dealt / 4f, now, events)
                events += "$name is hurt by recoil"
            }
            Effect.LEECH -> opp.leechUntil = now + 5000
            Effect.SIPHON -> {
                val stolen = minOf(40f, opp.energy)
                opp.energy -= stolen
                me.energy = minOf(BattleRules.MAX_ENERGY.toFloat(), me.energy + stolen)
            }
            else -> debuff(opp, m, now, events, quiet = true)
        }
        when (m.effect) {
            Effect.KNOCKBACK -> knockback(me, opp, 0.15f)
            Effect.PULL -> knockback(me, opp, -minOf(0.15f, (distance() - 0.1f).coerceAtLeast(0f)))
            else -> knockback(me, opp, 0.05f)
        }
    }

    /** Status effects on the target. Slippery is immune to the ones that stop movement. */
    private fun debuff(opp: Fighter, m: Move, now: Long, events: MutableList<String>, quiet: Boolean = false) {
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
            Effect.LEECH -> { opp.leechUntil = now + 5000; "$target is seeded" }
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
            FighterSnap(
                x = me.x, y = me.y,
                hp = kotlin.math.ceil(me.hp).toInt(), maxHp = me.maxHp,
                action = me.action.takeIf { now - me.actionAt < 1200 },
                actionAgeMs = now - me.actionAt,
                status = status,
                hurtAgeMs = now - me.hurtAt,
                facing = me.facing,
                energy = me.energy.toInt(),
                uniqueLeft = BattleRules.UNIQUE_USES - me.uniqueUsed,
                cooldowns = me.info.loadout.map { (me.readyAt[it.ordinal] - now).coerceAtLeast(0) },
            )
        },
    )
}

/** Simple practice opponent. */
object BattleBot {
    private val BUFFS = setOf(Effect.RAGE, Effect.HASTE, Effect.HARDEN, Effect.BATTLE_CRY, Effect.ENERGIZE)
    private val BIG_HEALS = setOf(Effect.PACK_CALL, Effect.FORTRESS, Effect.ALPHA, Effect.CITADEL)

    fun pick(engine: BattleEngine, me: Int, now: Long, rng: Random): Move? {
        if (rng.nextFloat() > 0.06f) return null // think roughly 1x/second at 20 ticks/s
        val info = engine.infos[me]
        val ready = info.loadout.filter { engine.isReady(me, it, now) }
        val hp = engine.hpFraction(me)
        ready.firstOrNull { it.effect == Effect.HEAL && hp < 0.45f }?.let { return it }
        ready.firstOrNull { it.effect in BIG_HEALS && hp < 0.4f }?.let { return it }
        ready.firstOrNull { it.effect in BUFFS && rng.nextFloat() < 0.25f }?.let { return it }
        val d = engine.distance()
        ready.firstOrNull { it.effect == Effect.COUNTER && d < 0.3f && rng.nextFloat() < 0.2f }?.let { return it }
        ready.firstOrNull { it.effect == Effect.BLINK && hp < 0.3f && d < 0.25f }?.let { return it }
        ready.firstOrNull { !it.isBuff && it.damage == 0 && d <= BattleStats.range(info, it) * 0.9f && rng.nextFloat() < 0.3f }
            ?.let { return it }
        return ready.filter { !it.isBuff && d <= BattleStats.range(info, it) * 0.9f }.maxByOrNull { it.damage }
            ?: Move.GUARD.takeIf { it in ready && d < 0.3f && rng.nextFloat() < 0.3f }
    }
}
