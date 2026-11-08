package com.petpal.app.domain.battle

import com.petpal.app.domain.model.*
import kotlin.math.hypot
import kotlin.math.roundToInt
import kotlin.random.Random

/** Arena is ARENA_W x ARENA_H in normalized units. */
const val ARENA_W = 1f
const val ARENA_H = 0.6f
const val BATTLE_MS = 90_000L

data class FighterInfo(val name: String, val species: Species, val stage: Stage) {
    val passive get() = SpeciesKit.passive(species)
    val loadout get() = SpeciesKit.loadout(species)
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
        val blaze = if (f.passive == Passive.BLAZE && m == Move.FIRE_BREATH) 1.25f else 1f
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
    val cooldowns: List<Long>, // remaining ms, indexed by Move.ordinal
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
            now >= me.readyAt[m.ordinal]
    }

    /** Returns an event line, or null if the move isn't allowed right now. */
    fun command(i: Int, m: Move, now: Long): String? {
        if (!isReady(i, m, now)) return null
        val me = f[i]
        me.action = m
        me.actionAt = now
        me.readyAt[m.ordinal] = now + BattleStats.cooldown(me.info, m)
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
            if (me.passive == Passive.PHOTOSYNTHESIS && me.hp > 0) me.hp = minOf(me.maxHp.toFloat(), me.hp + 0.6f * dt)
            move(me, opp, now, dt)
        }

        f.forEachIndexed { i, me ->
            val m = me.action ?: return@forEachIndexed
            if (!me.pendingHit || now < me.actionAt + m.windupMs) return@forEachIndexed
            me.pendingHit = false
            if (m.isSelf) selfEffect(me, m, now, events) else strike(me, f[1 - i], m, now, events)
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
        if (now < me.stunUntil) return
        val lunging = me.action?.effect == Effect.LUNGE && now - me.actionAt < (me.action?.windupMs ?: 0)
        if (lunging) {
            me.tx = opp.x; me.ty = opp.y
        } else if (now >= me.nextWanderAt || hypot(me.tx - me.x, me.ty - me.y) < 0.02f) {
            me.tx = rng.nextFloat() * 0.84f + 0.08f
            me.ty = rng.nextFloat() * (ARENA_H - 0.16f) + 0.08f
            me.nextWanderAt = now + 700 + rng.nextLong(1500)
        }
        var speed = when {
            lunging -> 0.9f
            now < me.busyUntil -> 0.06f
            else -> BattleStats.speed(me.info)
        }
        if (now < me.slowUntil) speed *= 0.5f
        val dx = me.tx - me.x
        val dy = me.ty - me.y
        val d = hypot(dx, dy)
        if (d > 0.001f) {
            val step = minOf(d, speed * dt)
            me.x = (me.x + dx / d * step).coerceIn(0.05f, ARENA_W - 0.05f)
            me.y = (me.y + dy / d * step).coerceIn(0.05f, ARENA_H - 0.05f)
        }
    }

    private fun selfEffect(me: Fighter, m: Move, now: Long, events: MutableList<String>) {
        when (m.effect) {
            Effect.HEAL -> {
                val amount = me.maxHp * 0.18f
                me.hp = minOf(me.maxHp.toFloat(), me.hp + amount)
                events += "${me.info.name} heals ${amount.roundToInt()} HP"
            }
            Effect.RAGE -> { me.rageUntil = now + 4000; events += "${me.info.name} is fired up!" }
            Effect.GUARD -> me.guardUntil = now + 1200
            else -> Unit
        }
    }

    private fun strike(me: Fighter, opp: Fighter, m: Move, now: Long, events: MutableList<String>) {
        if (distance() > BattleStats.range(me.info, m)) {
            events += "${me.info.name}'s ${m.label} missed"
            return
        }
        if (opp.passive == Passive.TRICKSTER && rng.nextFloat() < 0.2f) {
            events += "${opp.info.name} dodged!"
            return
        }
        var dmg = BattleStats.baseDamage(me.info, m) * (0.85f + rng.nextFloat() * 0.3f)
        if (now < me.rageUntil) dmg *= 1.3f
        if (me.passive == Passive.LOYAL_HEART && me.hp < me.maxHp / 2f) dmg *= 1.25f
        val guarded = now < opp.guardUntil
        if (guarded) dmg *= 0.4f
        if (opp.passive == Passive.HARD_SHELL) dmg *= 0.8f
        val dealt = dmg.roundToInt().coerceAtLeast(1)
        hurt(opp, dealt.toFloat(), now, events)
        events += "${m.label} hits ${opp.info.name} for $dealt${if (guarded) " (guarded)" else ""}"

        val immune = opp.passive == Passive.SLIPPERY
        when (m.effect) {
            Effect.DRAIN -> me.hp = minOf(me.maxHp.toFloat(), me.hp + dealt / 2f)
            Effect.BURN -> opp.burnUntil = now + if (me.passive == Passive.BLAZE) 6000 else 3000
            Effect.SLOW -> if (!immune) opp.slowUntil = now + 3000
            Effect.STUN -> if (!immune) {
                opp.stunUntil = now + 800
                opp.busyUntil = maxOf(opp.busyUntil, now + 800)
                opp.pendingHit = false
            }
            else -> Unit
        }
        knockback(me, opp, if (m.effect == Effect.KNOCKBACK) 0.15f else 0.05f)
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
            FighterSnap(
                x = me.x, y = me.y,
                hp = kotlin.math.ceil(me.hp).toInt(), maxHp = me.maxHp,
                action = me.action.takeIf { now - me.actionAt < 1200 },
                actionAgeMs = now - me.actionAt,
                status = status,
                hurtAgeMs = now - me.hurtAt,
                facing = me.facing,
                cooldowns = me.readyAt.map { (it - now).coerceAtLeast(0) },
            )
        },
    )
}

/** Simple practice opponent. */
object BattleBot {
    fun pick(engine: BattleEngine, me: Int, now: Long, rng: Random): Move? {
        if (rng.nextFloat() > 0.06f) return null // think roughly 1x/second at 20 ticks/s
        val info = engine.infos[me]
        val ready = info.loadout.filter { engine.isReady(me, it, now) }
        if (Move.HEAL in ready && engine.hpFraction(me) < 0.45f) return Move.HEAL
        if (Move.HOWL in ready && rng.nextFloat() < 0.25f) return Move.HOWL
        val d = engine.distance()
        return ready.filter { !it.isSelf && d <= BattleStats.range(info, it) * 0.9f }.maxByOrNull { it.damage }
            ?: Move.GUARD.takeIf { it in ready && d < 0.3f && rng.nextFloat() < 0.3f }
    }
}
