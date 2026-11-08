package com.petpal.app.data.local

import com.petpal.app.domain.logic.StepDay
import com.petpal.app.domain.model.*

fun PetEntity.toDomain() = Pet(
    id = id,
    name = name,
    stage = enumValueOrDefault(stage, Stage.EGG),
    path = enumValueOrDefault(path, EvolutionPath.NONE),
    hunger = hunger,
    happiness = happiness,
    energy = energy,
    xp = xp,
    lastUpdated = lastUpdated,
)

fun Pet.toEntity() = PetEntity(
    id = id,
    name = name,
    stage = stage.name,
    path = path.name,
    hunger = hunger,
    happiness = happiness,
    energy = energy,
    xp = xp,
    lastUpdated = lastUpdated,
)

fun HabitLogEntity.toDomain() = HabitLog(
    id = id,
    type = enumValueOrDefault(type, HabitType.CUSTOM),
    value = value,
    timestamp = timestamp,
)

fun StepDayEntity.toDomain() = StepDay(date, baseline, offset, steps, rewardedThousands)
fun StepDay.toEntity() = StepDayEntity(date, baseline, offset, steps, rewardedThousands)

private inline fun <reified T : Enum<T>> enumValueOrDefault(name: String, default: T): T =
    enumValues<T>().firstOrNull { it.name == name } ?: default
