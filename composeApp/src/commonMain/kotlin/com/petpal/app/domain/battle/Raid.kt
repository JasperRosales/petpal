package com.petpal.app.domain.battle

import com.petpal.app.domain.model.Species
import com.petpal.app.domain.model.Stage
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.random.Random

/**
 * Co-op raid rules: ten floors of skeletons, wild pets and one boss. The host rolls the whole
 * dungeon once, then both phones see the same floor plan. See docs/RAIDS.md.
 */
object Raid {
    const val FLOORS = 10
    const val MAX_PLAYERS = 4

    const val BOSS_LEVEL = 50
    const val BOSS_HP = 1000
    const val BOSS_DAMAGE = 1.5f

    const val FLOOR_MS = 90_000L
    const val BOSS_MS = 120_000L
    /** Pause on the "floor cleared" banner before the next encounter starts. */
    const val BANNER_MS = 5_000L
    /** Survivors heal this fraction of max HP between floors. */
    const val HEAL_BETWEEN = 0.2f

    /** Floors 1-4: one skeleton per raider who started the raid. */
    val SKELETON_LEVELS = intArrayOf(20, 24, 28, 32)
    val SKELETON_HP = intArrayOf(120, 150, 180, 210)
    val SKELETON_DAMAGE = floatArrayOf(0.9f, 0.95f, 1f, 1.05f)
    val SKELETON_MOVES = listOf(Move.BONE_RUSH, Move.HEADBUTT, Move.GUARD, Move.GLARE, Move.RAISE_DEAD)

    val WILD_LEVELS = listOf(35, 38, 42, 46, 50)

    data class Floor(
        val number: Int,
        val title: String,
        val enemies: List<FighterInfo>,
        val boss: Boolean = false,
    ) {
        val timeLimitMs: Long get() = if (boss) BOSS_MS else FLOOR_MS
    }

    data class Plan(val floors: List<Floor>)

    /** Rolls the dungeon from the host's [rng]. [raiders] is the party size at start, not the survivors. */
    fun plan(raiders: Int, rng: Random = Random.Default): Plan {
        require(raiders in 1..MAX_PLAYERS) { "a raid has 1..$MAX_PLAYERS raiders" }
        val floors = mutableListOf<Floor>()
        for (n in 1..4) {
            // The pack, plus as many buried skeletons waiting to be raised with Raise Dead.
            floors += Floor(n, title(n), List(raiders) { skeleton(n, it + 1, raiders * 2) } +
                List(raiders) { skeleton(n, raiders + it + 1, raiders * 2).copy(reserve = true) })
        }
        WILD_LEVELS.forEachIndexed { i, level ->
            floors += Floor(5 + i, title(5 + i), listOf(wild(level, rng)))
        }
        floors += Floor(FLOORS, title(FLOORS), listOf(boss(rng)), boss = true)
        return Plan(floors)
    }

    /** Banner/HUD title for a 1-based floor; both phones derive it from the floor number alone. */
    fun title(floor: Int): String = when {
        floor <= 4 -> "Skeleton pack"
        floor < FLOORS -> "Wild pet Lv ${WILD_LEVELS[(floor - 5).coerceIn(0, WILD_LEVELS.lastIndex)]}"
        else -> "Raid boss"
    }

    /** A raid-only skeleton: no dex entry, no passive, no unique, fixed moves. Numbered when in a pack. */
    fun skeleton(floor: Int, number: Int = 1, packSize: Int = 1): FighterInfo {
        val i = (floor - 1).coerceIn(0, SKELETON_LEVELS.lastIndex)
        return FighterInfo(
            name = if (packSize > 1) "Skeleton $number" else "Skeleton",
            species = Species.HEDGEHOG, // used only for a generic body shape; the palette is bone-white
            stage = Stage.KID,
            level = SKELETON_LEVELS[i],
            moves = SKELETON_MOVES,
            variant = FighterVariant.SKELETON,
            fixedMoves = SKELETON_MOVES,
            damageScale = SKELETON_DAMAGE[i],
            maxHpOverride = SKELETON_HP[i],
        )
    }

    /** One wild pet, generated like a CPU opponent. */
    fun wild(level: Int, rng: Random = Random.Default): FighterInfo =
        CpuOpponent.random(FighterInfo("", Species.SPROUT, stageFor(level), level), rng, prefix = "Wild")

    /** Lv 50, 1000 HP and 1.5x damage; keeps its normal passive and unique. */
    fun boss(rng: Random = Random.Default): FighterInfo {
        val template = FighterInfo("", Species.SPROUT, Stage.ADULT, BOSS_LEVEL)
        return CpuOpponent.random(template, rng, prefix = "Raid Boss").copy(
            maxHpOverride = BOSS_HP,
            damageScale = BOSS_DAMAGE,
        )
    }

    /** Carries surviving HP into the next floor, healing survivors by [heal] of their max HP. */
    fun carryHp(snap: BattleSnapshot, raiders: Int, heal: Float = HEAL_BETWEEN): IntArray =
        IntArray(raiders) { i ->
            val f = snap.fighters[i]
            if (f.hp <= 0) 0 else minOf(f.maxHp, f.hp + (f.maxHp * heal).roundToInt())
        }

    /** Why a run ended short of a clear, read from the final snapshot of the last floor. */
    fun failReason(snap: BattleSnapshot, raiders: Int): String =
        if ((0 until minOf(raiders, snap.fighters.size)).all { snap.fighters[it].hp <= 0 }) "The party was wiped out"
        else "Time ran out"

    fun stageFor(level: Int): Stage = when {
        level >= 50 -> Stage.ADULT
        level >= 35 -> Stage.TEEN
        level >= 20 -> Stage.KID
        level >= 3 -> Stage.BABY
        else -> Stage.EGG
    }
}
