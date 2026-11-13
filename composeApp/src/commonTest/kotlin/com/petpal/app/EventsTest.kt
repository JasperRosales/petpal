package com.petpal.app

import com.petpal.app.domain.battle.BattleStats
import com.petpal.app.domain.logic.Events
import com.petpal.app.domain.model.Stage
import kotlinx.datetime.LocalDate
import kotlin.random.Random
import kotlin.test.*

class EventsTest {
    @Test fun bossIsLv35WithBonusHp() {
        val boss = Events.boss(Random(1))
        assertEquals(35, boss.level)
        assertEquals(Stage.TEEN, boss.stage)
        assertEquals(BattleStats.maxHp(boss.copy(bonusHp = 0)) + 50, BattleStats.maxHp(boss))
    }

    @Test fun monthlyOnlyOnTheLastDay() {
        assertTrue(Events.monthlyOpen(LocalDate(2026, 11, 30)))
        assertTrue(Events.monthlyOpen(LocalDate(2026, 12, 31)))
        assertTrue(Events.monthlyOpen(LocalDate(2028, 2, 29)))
        assertFalse(Events.monthlyOpen(LocalDate(2026, 12, 1)))
        assertFalse(Events.monthlyOpen(LocalDate(2026, 11, 29)))
        assertEquals("monthly:2026-11", Events.monthlyKey(LocalDate(2026, 11, 30)))
    }

    @Test fun bossClosesInDecember() {
        assertTrue(Events.bossOpen(LocalDate(2026, 11, 30)))
        assertFalse(Events.bossOpen(LocalDate(2026, 12, 1)))
    }
}
