package com.petpal.app

import com.petpal.app.domain.battle.*
import com.petpal.app.domain.model.*
import kotlin.random.Random
import kotlin.test.*

class BattleTest {
    private val a = FighterInfo("A", Species.DRAKE, Stage.KID)
    private val b = FighterInfo("B", Species.KITTY, Stage.BABY)

    @Test fun cooldownBlocksSpam() {
        val e = BattleEngine(listOf(a, b), Random(1))
        assertNotNull(e.command(0, Move.PUNCH, 0))
        assertNull(e.command(0, Move.PUNCH, 100))
    }

    @Test fun rejectsMovesOutsideLoadout() {
        val e = BattleEngine(listOf(a, b), Random(1))
        assertNull(e.command(1, Move.THUNDER, 0)) // Kitty can't use Thunder
        assertNotNull(e.command(0, Move.FIRE_BREATH, 0))
    }

    @Test fun everySpeciesHasFiveMovesPlusGuard() {
        Species.entries.forEach { assertEquals(6, SpeciesKit.loadout(it).toSet().size, it.name) }
    }

    @Test fun everySpeciesCanWinTheQuiz() {
        val reachable = mutableSetOf<Species>()
        repeat(4000) { seed ->
            val r = Random(seed)
            reachable += com.petpal.app.domain.logic.PersonalityQuiz.run {
                result(questions.map { r.nextInt(4) }, r)
            }
        }
        assertEquals(Species.entries.toSet(), reachable)
    }

    @Test fun battleEndsWithinTimeLimit() {
        val e = BattleEngine(listOf(a, b), Random(2))
        val rng = Random(3)
        var t = 0L
        while (!e.ended && t <= BATTLE_MS) {
            for (i in 0..1) BattleBot.pick(e, i, t, rng)?.let { e.command(i, it, t) }
            e.tick(t); t += 50
        }
        assertTrue(e.ended)
    }

    @Test fun protocolRoundTrip() {
        val e = BattleEngine(listOf(a, b), Random(4))
        e.command(1, Move.BITE, 0); e.tick(100)
        val snap = e.snapshot(100)
        val parsed = (Protocol.parse(Protocol.state(snap)) as Protocol.State).snap
        assertEquals(snap.fighters.map { it.hp to it.action }, parsed.fighters.map { it.hp to it.action })
        assertEquals(snap.fighters[1].cooldowns, parsed.fighters[1].cooldowns)
        assertEquals(Protocol.Hello(Protocol.VERSION, a), Protocol.parse(Protocol.hello(a)))
        assertEquals(Protocol.Command(Move.GUARD), Protocol.parse(Protocol.command(Move.GUARD)))
    }
}
