package com.petpal.app

import com.petpal.app.domain.battle.*
import com.petpal.app.domain.model.*
import kotlin.random.Random
import kotlin.test.*

class BattleTest {
    @Test
    fun duoBattleRunsToATeamResult() {
        repeat(10) { seed ->
            val rng = Random(seed)
            val base = FighterInfo("Me", Species.PENGUIN, Stage.KID, 30)
            val infos = listOf(base) + List(3) { CpuOpponent.random(base, Random(seed * 7 + it)) }
            val engine = BattleEngine(infos, rng)
            assertEquals(listOf(0, 0, 1, 1), infos.indices.map(engine::teamOf))
            var now = 0L
            while (!engine.ended) {
                for (i in infos.indices) BattleBot.pick(engine, i, now, rng)?.let { engine.command(i, it, now) }
                engine.tick(now)
                now += 50
            }
            val snap = engine.snapshot(now)
            assertTrue(snap.winner in -1..1)
            if (snap.winner >= 0 && now < BATTLE_MS) { // the losing team is fully down
                assertTrue(infos.indices.filter { engine.teamOf(it) != snap.winner }.all { snap.fighters[it].hp == 0 })
            }
            val enc = Protocol.parse(Protocol.setup(2, infos)) as Protocol.Setup
            assertEquals(2, enc.you)
            assertEquals(infos.map { it.species to it.moves }, enc.infos.map { it.species to it.moves })
        }
    }

    @Test
    fun cpuOpponentMatchesLevelWithValidMoves() {
        val me = FighterInfo("Me", Species.DRAKE, Stage.KID, 30)
        repeat(50) { seed ->
            val cpu = CpuOpponent.random(me, Random(seed))
            assertEquals(30, cpu.level)
            assertEquals(Stage.KID, cpu.stage)
            assertEquals(SpeciesKit.MAX_MOVES, cpu.moves.size)
            assertTrue(cpu.moves.all { SpeciesKit.canLearn(cpu.species, it) && it.level <= 30 })
        }
    }

    private val a = FighterInfo("A", Species.DRAKE, Stage.KID, 40, listOf(Move.PUNCH, Move.EMBER, Move.FIRE_BREATH, Move.GUARD))
    private val b = FighterInfo("B", Species.KITTY, Stage.BABY, 40, listOf(Move.PUNCH, Move.BITE, Move.GUARD))

    @Test fun cooldownBlocksSpam() {
        val e = BattleEngine(listOf(a, b), Random(1))
        assertNotNull(e.command(0, Move.PUNCH, 0))
        assertNull(e.command(0, Move.PUNCH, 100))
    }

    @Test fun rejectsMovesOutsideLoadout() {
        val e = BattleEngine(listOf(a, b), Random(1))
        assertNull(e.command(1, Move.FIRE_BREATH, 0)) // Kitty can't use Fire Breath
        assertNotNull(e.command(0, Move.PUNCH, 0))
    }

    @Test fun loadoutDropsIllegalMoves() {
        val cheat = FighterInfo("C", Species.KITTY, Stage.BABY, 5,
            listOf(Move.FIRE_BREATH, Move.GIGA_IMPACT, Move.HOWL, Move.SPARK, Move.INFERNO, Move.TACKLE))
        // wrong type, level too low, banned for Kitty, unique
        assertEquals(listOf(Move.SPARK, Move.TACKLE, Move.SHADOW_SWIPE), cheat.loadout) // + Baby unique
    }

    @Test fun uniqueIsSlotFive() {
        val kid = FighterInfo("K", Species.DRAKE, Stage.KID, 40,
            listOf(Move.PUNCH, Move.EMBER, Move.FIRE_BREATH, Move.GUARD, Move.TACKLE))
        assertEquals(listOf(Move.PUNCH, Move.EMBER, Move.FIRE_BREATH, Move.GUARD, Move.INFERNO), kid.loadout)
    }

    @Test fun uniqueUpgradesWithStage() {
        fun u(st: Stage) = FighterInfo("D", Species.DRAKE, st, 30).loadout.filter { it.category == MoveCategory.UNIQUE }
        assertEquals(emptyList(), u(Stage.EGG))
        assertEquals(listOf(Move.FLAME_BURST), u(Stage.BABY))
        assertEquals(listOf(Move.INFERNO), u(Stage.KID))
        assertEquals(listOf(Move.DRAGON_FURY), u(Stage.ADULT))
    }

    @Test fun moveCatalog() {
        assertTrue(Move.entries.size >= 100)
        val uniques = Species.entries.flatMap { SpeciesKit.uniques(it).toList() }
        assertEquals(uniques.toSet(), Move.entries.filter { it.category == MoveCategory.UNIQUE }.toSet())
        Species.entries.forEach { s ->
            val set = SpeciesKit.learnset(s).map { it.move }
            assertEquals(SpeciesKit.STARTERS, SpeciesKit.learnset(s).count { it.level <= 1 }, s.name)
            assertTrue(set.size < Move.entries.size - uniques.size, s.name) // some moves are off-limits
            assertTrue(set.any { it.element != Element.NORMAL && it.element !in SpeciesKit.affinity(s) }, s.name)
            assertTrue(SpeciesKit.banned(s).none { SpeciesKit.canLearn(s, it) }, s.name)
            assertTrue(set.all { it.level <= com.petpal.app.domain.logic.PetEngine.MAX_LEVEL }, s.name)
        }
    }

    @Test fun cantHitAFlyingPet() {
        val owl = FighterInfo("O", Species.OWL, Stage.BABY, 40, listOf(Move.FLY, Move.PECK))
        val bear = FighterInfo("B", Species.BEAR, Stage.BABY, 40, listOf(Move.PUNCH))
        val e = BattleEngine(listOf(owl, bear), Random(1))
        assertNotNull(e.command(0, Move.FLY, 0))
        assertNotNull(e.command(1, Move.PUNCH, 0))
        assertTrue(e.snapshot(0).fighters[0].has(Status.VANISH))
        val hp = e.snapshot(0).fighters[0].hp
        val log = mutableListOf<String>()
        var t = 0L
        while (t <= 1300) { log += e.tick(t); t += 50 }
        assertEquals(hp, e.snapshot(t).fighters[0].hp) // punch whiffed while it was in the air
        assertTrue(log.any { "out of reach" in it })
        assertTrue(log.any { "Fly hits" in it }) // reappears next to the bear, so it always connects
        assertFalse(e.snapshot(t).fighters[0].has(Status.VANISH))
    }

    @Test fun specialNeedsFullEnergy() {
        val e = BattleEngine(listOf(a, b), Random(1))
        assertNull(e.command(0, Move.FIRE_BREATH, 0))
        var t = 0L
        while (t < 20_000) { e.tick(t); t += 50 } // passive regen fills the bar
        assertNotNull(e.command(0, Move.FIRE_BREATH, t))
        assertEquals(0, e.snapshot(t).fighters[0].energy)
    }

    @Test fun uniqueOnlyTwicePerBattle() {
        val e = BattleEngine(listOf(a, b), Random(1))
        var t = 0L
        repeat(2) {
            assertNotNull(e.command(0, Move.INFERNO, t))
            t += 16_000
            e.tick(t)
        }
        assertNull(e.command(0, Move.INFERNO, t))
        assertEquals(0, e.snapshot(t).fighters[0].uniqueLeft)
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
        assertEquals(snap.fighters.map { it.evadeCdMs }, parsed.fighters.map { it.evadeCdMs })
        assertEquals(snap.fighters.map { it.energy to it.uniqueLeft }, parsed.fighters.map { it.energy to it.uniqueLeft })
        assertEquals(Protocol.Hello(Protocol.VERSION, a), Protocol.parse(Protocol.hello(a)))
        assertEquals(Protocol.Command(Move.GUARD), Protocol.parse(Protocol.command(Move.GUARD)))
    }
}

class HitboxTest {
    @Test fun projectileFollowsAimNotFacing() {
        // target straight below: an aimed line hits it, a left/right line would not
        assertTrue(Move.EMBER.hits(0.5f, 0.3f, 0.5f, 0.7f, 1, 0.5f, 0.7f, Move.EMBER.range))
        assertFalse(Move.EMBER.hits(0.5f, 0.3f, 0.9f, 0.3f, 1, 0.5f, 0.7f, Move.EMBER.range))
    }

    @Test fun meleeNeedsOverlap() {
        assertTrue(Move.PUNCH.hits(0.5f, 0.5f, 0.6f, 0.5f, 1, 0.6f, 0.5f, Move.PUNCH.range))
        assertFalse(Move.PUNCH.hits(0.5f, 0.5f, 0.6f, 0.5f, 1, 0.6f, 0.8f, Move.PUNCH.range))
    }

    @Test fun coneCoversSides() {
        assertTrue(Move.TIDAL_WAVE.hits(0.2f, 0.5f, 0.6f, 0.5f, 1, 0.5f, 0.75f, Move.TIDAL_WAVE.range))
    }

    private val a = FighterInfo("A", Species.DRAKE, Stage.KID, 40, listOf(Move.PUNCH, Move.EMBER, Move.FIRE_BREATH, Move.GUARD))
    private val b = FighterInfo("B", Species.KITTY, Stage.BABY, 40, listOf(Move.PUNCH, Move.BITE, Move.GUARD))

    @Test fun evadeDodgesAndHasCooldown() {
        val e = BattleEngine(listOf(a, b), Random(1))
        var t = 0L
        while (e.distance(0) > 0.1f && t < BATTLE_MS) { e.tick(t); t += 50 } // wait until they're close
        assertNotNull(e.command(1, Move.PUNCH, t))
        assertNotNull(e.evade(0, t))
        assertNull(e.evade(0, t + 50)) // on cooldown
        val log = e.tick(t + 100) + e.tick(t + 200)
        assertTrue(log.any { it == "A evades Punch!" }, log.toString())
        assertTrue(e.snapshot(t + 200).fighters[0].has(Status.EVADE))
        assertTrue(e.snapshot(t + 200).fighters[0].evadeCdMs > 0)
        assertNotNull(e.evade(0, t + Evade.COOLDOWN_MS))
    }

    @Test fun evadeCancelsWindup() {
        val e = BattleEngine(listOf(a, b), Random(1))
        assertNotNull(e.command(0, Move.PUNCH, 0))
        assertNotNull(e.evade(0, 50))
        assertTrue(e.tick(300).none { "Punch" in it })
    }

    @Test fun evadeCommandRoundTrips() {
        assertEquals(Protocol.EvadeCmd, Protocol.parse(Protocol.EVADE))
    }
}
