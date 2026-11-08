package com.petpal.app.data.local

import com.petpal.app.domain.battle.Move
import com.petpal.app.domain.logic.PetEngine
import com.petpal.app.domain.logic.StepDay
import com.petpal.app.domain.model.*

fun PetEntity.toDomain() = Pet(
    id = id,
    name = name,
    // Old saves used TEEN for what is now KID: only keep TEEN if the XP backs it up.
    stage = if (stage == "TEEN") maxOf(Stage.KID, PetEngine.stageFor(xp)) else enumValueOrDefault(stage, Stage.EGG),
    path = enumValueOrDefault(path, EvolutionPath.NONE),
    species = enumValueOrDefault(species, Species.SPROUT),
    xp = xp,
    lastUpdated = lastUpdated,
    moves = moveList(moves),
    seenMoves = moveList(seenMoves).toSet(),
    relearnsUsed = relearnsUsed,
    stepsAtReset = stepsAtReset,
)

/** Unknown names (removed moves) are dropped. */
private fun moveList(csv: String): List<Move> =
    csv.split(',').mapNotNull { n -> Move.entries.firstOrNull { it.name == n } }

fun Pet.toEntity() = PetEntity(
    id = id,
    name = name,
    stage = stage.name,
    path = path.name,
    species = species.name,
    hunger = 100.0, // needs were removed; columns kept to avoid a destructive migration
    happiness = 100.0,
    energy = 100.0,
    xp = xp,
    lastUpdated = lastUpdated,
    moves = moves.joinToString(",") { it.name },
    seenMoves = seenMoves.joinToString(",") { it.name },
    relearnsUsed = relearnsUsed,
    stepsAtReset = stepsAtReset,
)

/** Null for habit types that no longer exist (water, sleep, treat). */
fun HabitLogEntity.toDomain(): HabitLog? =
    HabitType.entries.firstOrNull { it.name == type }?.let { HabitLog(id, it, value, timestamp) }

fun BattleLogEntity.toDomain() = BattleRecord(
    id = id,
    opponent = opponent.ifEmpty { "Unknown" },
    outcome = enumValueOrDefault(outcome, BattleOutcome.DRAW),
    xp = xp,
    timestamp = timestamp,
)

fun StepDayEntity.toDomain() = StepDay(date, baseline, offset, steps, rewardedThousands)
fun StepDay.toEntity() = StepDayEntity(date, baseline, offset, steps, rewardedThousands)

private inline fun <reified T : Enum<T>> enumValueOrDefault(name: String, default: T): T =
    enumValues<T>().firstOrNull { it.name == name } ?: default
