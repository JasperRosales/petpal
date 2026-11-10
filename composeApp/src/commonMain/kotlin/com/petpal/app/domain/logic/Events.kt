package com.petpal.app.domain.logic

import com.petpal.app.domain.battle.CpuOpponent
import com.petpal.app.domain.battle.FighterInfo
import com.petpal.app.domain.model.Species
import com.petpal.app.domain.model.Stage
import kotlinx.datetime.LocalDate
import kotlin.random.Random

/** Limited-time boss fight and the free pet on the 1st of every month. */
object Events {
    /** One-time boss: beat it once before [BOSS_ENDS] for a random pet. */
    const val BOSS_KEY = "boss:2026-11"
    val BOSS_ENDS = LocalDate(2026, 12, 1) // first day it's gone
    const val BOSS_LEVEL = 35
    const val BOSS_BONUS_HP = 50

    fun bossOpen(today: LocalDate) = today < BOSS_ENDS

    /** A random Lv 35 pet with +50 HP. */
    fun boss(rng: Random = Random.Default): FighterInfo {
        val template = FighterInfo("", Species.SPROUT, Stage.TEEN, BOSS_LEVEL)
        return CpuOpponent.random(template, rng, prefix = "Boss").copy(bonusHp = BOSS_BONUS_HP)
    }

    /** The monthly gift can only be claimed on the 1st. */
    fun monthlyOpen(today: LocalDate) = today.dayOfMonth == 1
    fun monthlyKey(today: LocalDate) = "monthly:${today.year}-${today.monthNumber.toString().padStart(2, '0')}"
}
