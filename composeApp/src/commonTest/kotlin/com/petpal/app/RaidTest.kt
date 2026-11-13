package com.petpal.app

import com.petpal.app.domain.battle.*
import com.petpal.app.domain.model.Stage
import kotlin.random.Random
import kotlin.test.*

class RaidTest {
    @Test fun planHasTenFloorsAndScalesSkeletonsWithTheParty() {
        listOf(2, 3, 4).forEach { party ->
            val plan = Raid.plan(party, Random(party))
            assertEquals(Raid.FLOORS, plan.floors.size)
            assertEquals((1..10).toList(), plan.floors.map { it.number })
            plan.floors.take(4).forEach { fl ->
                assertEquals(party, fl.enemies.count { !it.reserve })
                assertEquals(party, fl.enemies.count { it.reserve })
            }
            plan.floors.drop(4).forEach { assertEquals(1, it.enemies.size) }
        }
    }

    @Test fun wildFloorsRampFrom35To50() {
        val wild = Raid.plan(2, Random(2)).floors.drop(4).dropLast(1)
        assertEquals(Raid.WILD_LEVELS, wild.map { it.enemies.single().level })
        assertEquals(listOf(Stage.TEEN, Stage.TEEN, Stage.TEEN, Stage.TEEN, Stage.ADULT), wild.map { it.enemies.single().stage })
    }

    @Test fun bossIsLv50With1000HpAnd15Damage() {
        val boss = Raid.boss(Random(3))
        assertEquals(Raid.BOSS_LEVEL, boss.level)
        assertEquals(Stage.ADULT, boss.stage)
        assertEquals(Raid.BOSS_HP, BattleStats.maxHp(boss))
        assertEquals(Raid.BOSS_DAMAGE, boss.damageScale)
        val move = boss.loadout.first { it.damage > 0 }
        assertEquals(BattleStats.baseDamage(boss.copy(damageScale = 1f), move) * Raid.BOSS_DAMAGE,
            BattleStats.baseDamage(boss, move))
    }

    @Test fun skeletonsHaveNoPassiveFixedMovesAndFloorStats() {
        for (floor in 1..4) {
            val s = Raid.skeleton(floor)
            assertEquals(FighterVariant.SKELETON, s.variant)
            assertEquals(Passive.NEUTRAL, s.passive)
            assertEquals(Raid.SKELETON_MOVES, s.loadout)
            assertEquals(Raid.SKELETON_LEVELS[floor - 1], s.level)
            assertEquals(Raid.SKELETON_HP[floor - 1], BattleStats.maxHp(s))
            assertEquals(Raid.SKELETON_DAMAGE[floor - 1], s.damageScale)
        }
    }

    @Test fun survivorsHealTwentyPercentAndTheDownStayDown() {
        val snap = BattleSnapshot(
            timeLeftMs = 0, ended = true, winner = 0,
            fighters = listOf(
                FighterSnap(0f, 0f, hp = 50, maxHp = 100, action = null, actionAgeMs = 0, status = 0,
                    hurtAgeMs = 0, facing = 1, energy = 0, cooldowns = emptyList()),
                FighterSnap(0f, 0f, hp = 0, maxHp = 120, action = null, actionAgeMs = 0, status = 0,
                    hurtAgeMs = 0, facing = 1, energy = 0, cooldowns = emptyList()),
            ),
        )
        assertContentEquals(intArrayOf(70, 0), Raid.carryHp(snap, 2))
    }

    @Test fun faintedRaidersStartTheNextFloorDown() {
        val down = FighterInfo("Down", com.petpal.app.domain.model.Species.PUP, Stage.TEEN, 40).copy(startHp = 0)
        val up = FighterInfo("Up", com.petpal.app.domain.model.Species.PUP, Stage.TEEN, 40)
        val engine = BattleEngine(listOf(down, up, Raid.skeleton(1)))
        assertTrue(engine.isDown(0))
        assertFalse(engine.isDown(1))
        assertEquals(0, engine.snapshot(0).fighters[0].hp)
    }

    @Test fun bossFloorUsesItsOwnTimeLimit() {
        val a = FighterInfo("A", com.petpal.app.domain.model.Species.TURTLE, Stage.ADULT, 50)
        val b = FighterInfo("B", com.petpal.app.domain.model.Species.TURTLE, Stage.ADULT, 50)
        val engine = BattleEngine(listOf(a, b), timeLimitMs = 1_000)
        engine.tick(1_000)
        assertTrue(engine.ended)
        assertEquals(0, engine.snapshot(1_000).timeLeftMs)
    }

    @Test fun raidMessagesRoundTrip() {
        val skeletons = List(3) { Raid.skeleton(1) }
        val infos = listOf(
            FighterInfo("Host", com.petpal.app.domain.model.Species.PUP, Stage.TEEN, 40),
            FighterInfo("Guest", com.petpal.app.domain.model.Species.KITTY, Stage.TEEN, 40),
        ) + skeletons
        val setup = Protocol.parse(Protocol.setup(1, infos, Terrain.ARENA, setOf(0, 1), listOf(0, 0, 1, 1, 1),
            raid = true, floor = 2, floors = Raid.FLOORS)) as Protocol.Setup
        assertTrue(setup.raid)
        assertEquals(2, setup.floor)
        assertEquals(Raid.FLOORS, setup.floors)
        assertEquals(listOf(0, 0, 1, 1, 1), setup.teams)
        assertEquals(FighterVariant.SKELETON, setup.infos.last().variant)
        assertEquals(Raid.SKELETON_MOVES, setup.infos.last().loadout)

        val lobby = Protocol.parse(Protocol.lobby(1, ffa = false, listOf(Protocol.RoomEntry("Host", 40, 0)), raid = true)) as Protocol.Lobby
        assertTrue(lobby.raid)

        val end = Protocol.parse(Protocol.raidEnd(cleared = true, floors = 10)) as Protocol.RaidEnd
        assertTrue(end.cleared)
        assertEquals(10, end.floors)
    }

    @Test fun floorTitlesMatchThePlanAndPacksAreNumbered() {
        val plan = Raid.plan(3, Random(4))
        plan.floors.forEach { assertEquals(Raid.title(it.number), it.title) }
        assertEquals("Raid boss", Raid.title(10))
        assertEquals("Wild pet Lv 42", Raid.title(7))
        assertEquals(listOf("Skeleton 1", "Skeleton 2", "Skeleton 3"), plan.floors[0].enemies.take(3).map { it.name })
    }

    @Test fun failReasonTellsWipeFromTimeout() {
        fun snap(vararg hp: Int) = BattleSnapshot(timeLeftMs = 0, ended = true, winner = -1,
            fighters = hp.map { FighterSnap(0f, 0f, hp = it, maxHp = 100, action = null, actionAgeMs = 0, status = 0,
                hurtAgeMs = 0, facing = 1, energy = 0, cooldowns = emptyList()) })
        assertEquals("The party was wiped out", Raid.failReason(snap(0, 0, 50), 2))
        assertEquals("Time ran out", Raid.failReason(snap(0, 10, 50), 2))
    }
}

class OnlineBotTest {
    @Test fun onlineBotIsLv50WithDoubleHp() {
        val bot = CpuOpponent.onlineBot(Random(7))
        assertEquals(50, bot.level)
        assertEquals(Stage.ADULT, bot.stage)
        assertEquals(BattleStats.maxHp(bot.copy(maxHpOverride = 0)) * 2, BattleStats.maxHp(bot))
    }
}

class RaiseDeadTest {
    @Test fun skeletonsRaiseABuriedSkeleton() {
        val raider = FighterInfo("Up", com.petpal.app.domain.model.Species.PUP, Stage.TEEN, 40)
        val a = Raid.skeleton(1, 1, 2)
        val buried = Raid.skeleton(1, 2, 2).copy(reserve = true)
        val e = BattleEngine(listOf(raider, a, buried), teamIds = listOf(0, 1, 1))
        val snap0 = e.snapshot(0)
        assertTrue(snap0.fighters[2].has(Status.DORMANT))
        assertTrue(e.isDown(2))
        assertFalse(e.isReady(1, Move.RAISE_DEAD, 0)) // no energy yet
        var t = 0L
        while (!e.isReady(1, Move.RAISE_DEAD, t) && t < 30_000) { t += 50; e.tick(t) }
        assertTrue(e.isReady(1, Move.RAISE_DEAD, t), "skeleton never charged Raise Dead")
        e.command(1, Move.RAISE_DEAD, t)
        repeat(30) { t += 50; e.tick(t) }
        assertFalse(e.isDown(2))
        assertFalse(e.snapshot(t).fighters[2].has(Status.DORMANT))
        e.snapshot(t).fighters[2].let { assertEquals(it.maxHp / 2, it.hp) } // raised at half HP
        assertFalse(e.isReady(1, Move.RAISE_DEAD, t + 60_000)) // nobody left to raise
    }
}

class StatusCooldownTest {
    @Test fun buffsAndDebuffsCoolDownLonger() {
        assertEquals((Move.GROWL.cooldownMs * BattleRules.STATUS_COOLDOWN_SCALE).toLong(), Move.GROWL.cooldown)
        val hit = Move.entries.first { it.damage > 0 && it.category != MoveCategory.UNIQUE }
        assertEquals(hit.cooldownMs, hit.cooldown)
        assertEquals(12_000L, Move.FOCUS_ENERGY.cooldown)
        assertEquals(12_000L, Move.CHARGE.cooldown)
    }
}
