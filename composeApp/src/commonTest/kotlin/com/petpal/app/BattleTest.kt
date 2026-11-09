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
            val (normal, unique) = cpu.moves.partition { it.category != MoveCategory.UNIQUE }
            assertEquals(SpeciesKit.MAX_MOVES, normal.size)
            assertTrue(normal.all { SpeciesKit.canLearn(cpu.species, it) && it.level <= 30 })
            assertEquals(1, unique.size) // one of its Kid options
            assertEquals(Stage.KID, SpeciesKit.uniqueStage(cpu.species, unique.single()))
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
        assertEquals(listOf(Move.WING_BLAZE), u(Stage.TEEN))
        assertEquals(listOf(Move.DRAGON_FURY), u(Stage.ADULT))
    }

    @Test fun pickedUniqueReplacesTheDefault() {
        val (a, b) = SpeciesKit.uniqueOptions(Species.DRAKE, Stage.KID)!!
        assertEquals(Move.INFERNO, a)
        val picked = FighterInfo("D", Species.DRAKE, Stage.KID, 30, listOf(b, Move.SMOKE_PUFF))
        assertEquals(listOf(b), picked.loadout.filter { it.category == MoveCategory.UNIQUE }) // other stages' picks ignored
        // every pet has a buff-or-debuff unique option somewhere
        Species.entries.forEach { s ->
            assertTrue(SpeciesKit.allUniques(s).any { it.damage == 0 }, s.name)
        }
    }

    @Test fun moveCatalog() {
        assertTrue(Move.entries.size >= 100)
        val uniques = Species.entries.flatMap { SpeciesKit.allUniques(it) }
        assertEquals(uniques.size, uniques.toSet().size) // no unique is shared
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

    @Test fun smokescreenLeavesCloudBacksOffAndSlows() {
        val r = FighterInfo("R", Species.RACCOON, Stage.KID, 40, listOf(Move.PUNCH, Move.SMOKESCREEN))
        assertTrue(Move.SMOKESCREEN in r.loadout)
        val e = BattleEngine(listOf(r, b), Random(1))
        var t = 0L
        while (e.distance(0) > 0.12f && t < BATTLE_MS) { e.tick(t); t += 50 }
        assertNotNull(e.command(0, Move.SMOKESCREEN, t))
        e.tick(t + Move.SMOKESCREEN.windupMs)
        val snap = e.snapshot(t + Move.SMOKESCREEN.windupMs + 50)
        assertEquals(1, snap.smokes.size)
        assertTrue(snap.fighters[0].has(Status.EVADE))
        assertTrue(e.snapshot(t + Move.SMOKESCREEN.windupMs).fighters[0].evadeCdMs == 0L) // own Evade untouched
        e.tick(t + 400)
        assertTrue(e.snapshot(t + 400).fighters[1].has(Status.SLOW)) // enemy is in the smoke
        val back = Protocol.parse(Protocol.state(snap)) as Protocol.State
        assertEquals(snap.smokes.size, back.snap.smokes.size)
        assertEquals(snap.fighters.size, back.snap.fighters.size)
        e.tick(t + Smoke.DURATION_MS + 500)
        assertTrue(e.snapshot(t + Smoke.DURATION_MS + 500).smokes.isEmpty())
    }

    @Test fun shieldSoaksDamage() {
        val s = FighterInfo("S", Species.PUP, Stage.KID, 40, listOf(Move.BARRIER))
        val e = BattleEngine(listOf(s, b), Random(1))
        assertNotNull(e.command(0, Move.BARRIER, 0))
        e.tick(Move.BARRIER.windupMs)
        assertTrue(e.snapshot(Move.BARRIER.windupMs).fighters[0].has(Status.SHIELD))
        e.tick(Move.BARRIER.windupMs + Mechanics.SHIELD_MS + 100)
        assertFalse(e.snapshot(Move.BARRIER.windupMs + Mechanics.SHIELD_MS + 100).fighters[0].has(Status.SHIELD))
        val beaver = BattleEngine(listOf(FighterInfo("B", Species.BEAVER, Stage.BABY, 1), b), Random(1))
        assertTrue(beaver.snapshot(0).fighters[0].has(Status.SHIELD)) // Dam Builder (its Baby passive) starts shielded
    }

    @Test fun trapRootsWhoeverStepsOnIt() {
        val t = FighterInfo("T", Species.HEDGEHOG, Stage.KID, 40, listOf(Move.CALTROPS))
        val e = BattleEngine(listOf(t, b), Random(1), demo = true)
        e.place(0, 0.3f, 0.6f); e.place(1, 0.6f, 0.6f)
        assertNotNull(e.command(0, Move.CALTROPS, 0))
        val now = Move.CALTROPS.windupMs
        e.tick(now)
        assertEquals(1, e.snapshot(now).traps.size)
        val back = Protocol.parse(Protocol.state(e.snapshot(now))) as Protocol.State
        assertEquals(1, back.snap.traps.size)
        val ev = e.tick(now + Trap.ARM_MS + 50)
        assertTrue(ev.any { "caught" in it }, ev.toString())
        val snap = e.snapshot(now + Trap.ARM_MS + 60)
        assertTrue(snap.fighters[1].has(Status.ROOT))
        assertTrue(snap.traps.isEmpty())
    }

    @Test fun markDisruptAndSwap() {
        val m = FighterInfo("M", Species.PUP, Stage.KID, 40, listOf(Move.LOCK_ON, Move.DISTRACT, Move.SWITCHEROO))
        val e = BattleEngine(listOf(m, b), Random(1), demo = true)
        e.place(0, 0.3f, 0.6f); e.place(1, 0.6f, 0.6f)
        e.command(0, Move.LOCK_ON, 0); e.tick(Move.LOCK_ON.windupMs)
        assertTrue(e.snapshot(Move.LOCK_ON.windupMs).fighters[1].has(Status.MARKED))
        var now = 2000L
        e.command(0, Move.DISTRACT, now); now += Move.DISTRACT.windupMs; e.tick(now)
        assertTrue(e.snapshot(now).fighters[1].cooldowns.all { it >= Mechanics.DISRUPT_MS - 10 })
        now += 2000
        e.command(0, Move.SWITCHEROO, now); now += Move.SWITCHEROO.windupMs
        repeat(20) { e.tick(now); now += 50 } // projectile flight
        assertTrue(e.snapshot(now).fighters[0].x > 0.5f)
    }

    @Test fun newSpeciesHaveFullKits() {
        listOf(Species.KOALA, Species.SHARK, Species.CHAMELEON, Species.BEAVER).forEach { s ->
            assertEquals(SpeciesKit.STARTERS, SpeciesKit.starters(s).size)
            assertTrue(SpeciesKit.learnset(s).size > 40)
        }
    }

    @Test fun elementalMovesCostEnergy() {
        assertEquals(0, Move.PUNCH.energyCost)
        assertTrue(Move.EMBER.energyCost > 0)
        val e = BattleEngine(listOf(a, b), Random(1))
        val before = e.snapshot(0).fighters[0].energy
        assertNotNull(e.command(0, Move.EMBER, 0))
        assertEquals(before - Move.EMBER.energyCost, e.snapshot(0).fighters[0].energy)
    }

    @Test fun demoPreviewLandsTheMove() {
        val e = BattleEngine(listOf(a, b), Random(1), demo = true)
        e.place(0, 0.4f, ARENA_H / 2); e.place(1, 0.52f, ARENA_H / 2)
        assertNotNull(e.command(0, Move.PUNCH, 0)) // any move works, even outside the loadout check
        val log = (0..20).flatMap { e.tick(it * 50L) }
        assertTrue(log.any { it.startsWith("Punch hits") }, log.toString())
    }

    @Test fun furySwipesStepsForwardAndSlashesTwice() {
        val k = FighterInfo("K", Species.KITTY, Stage.KID, 40, listOf(Move.FURY_SWIPES))
        val e = BattleEngine(listOf(k, b), Random(1), demo = true)
        e.place(0, 0.3f, ARENA_H / 2); e.place(1, 0.55f, ARENA_H / 2)
        assertNotNull(e.command(0, Move.FURY_SWIPES, 0))
        val xs = mutableListOf<Float>()
        val log = (0..30).flatMap { t -> e.tick(t * 25L).also { xs += e.snapshot(t * 25L).fighters[0].x } }
        assertEquals(2, log.count { it.contains("Fury Swipes") && !it.contains("uses") }, log.toString())
        assertTrue(xs.last() > 0.3f + TWIN_STEP * 1.5f, xs.toString()) // two steps forward
    }

    @Test fun terrainRotatesAndHasItsOwnRule() {
        val maps = (0 until Terrain.entries.size).map(Terrain::rotation)
        assertEquals(Terrain.entries.toSet(), maps.toSet())
        assertEquals(Terrain.ARENA, Terrain.rotation(Terrain.entries.size))
        // every element except Normal has its own terrain, each with its own named effect
        assertEquals(Element.entries.toSet() - Element.NORMAL, Terrain.entries.mapNotNull { it.element }.toSet())
        assertEquals(Terrain.entries.size - 1, Terrain.entries.filter { it.effectName.isNotEmpty() }.map { it.effectName }.toSet().size)
        assertTrue(Terrain.VOLCANO.isHomeTurf(a)) // Drake is Fire type
        assertFalse(Terrain.ARENA.isHomeTurf(a))

        // Forest heals everyone over time, Canyon chips everyone down
        fun hpAfter(t: Terrain): Int {
            val e = BattleEngine(listOf(a, b), Random(1), demo = true, terrain = t)
            e.place(0, 0.4f, ARENA_H / 2); e.place(1, 0.6f, ARENA_H / 2)
            e.command(0, Move.TACKLE, 0)
            (0..200).forEach { e.tick(it * 50L) }
            return e.snapshot(10_000).fighters[1].hp
        }
        assertTrue(hpAfter(Terrain.FOREST) > hpAfter(Terrain.ARENA))
        assertTrue(hpAfter(Terrain.CANYON) < hpAfter(Terrain.ARENA))
        assertTrue(BattleStats.cooldown(a, Move.EMBER, Terrain.THUNDER_PLAINS) < BattleStats.cooldown(a, Move.EMBER))

        val setup = Protocol.parse(Protocol.setup(1, listOf(a, b), Terrain.DOJO)) as Protocol.Setup
        assertEquals(Terrain.DOJO, setup.terrain)
    }

    @Test fun comboMovesHaveTwoTypes() {
        assertEquals(setOf(Element.ELECTRIC, Element.FIGHTING), Move.THUNDER_PUNCH.elements)
        assertEquals("Electric/Fighting", Move.THUNDER_PUNCH.typeLabel)
        assertTrue(SpeciesKit.canLearn(Species.KITTY, Move.THUNDER_PUNCH)) // Electric
        assertTrue(SpeciesKit.canLearn(Species.BEAR, Move.THUNDER_PUNCH)) // Fighting
        assertFalse(SpeciesKit.canLearn(Species.OWL, Move.THUNDER_PUNCH)) // Electric, but no fists
        assertFalse(SpeciesKit.canLearn(Species.SPROUT, Move.THUNDER_PUNCH))
        assertTrue(SpeciesKit.canLearn(Species.BUNNY, Move.KARATE_CHOP))
    }

    @Test fun homeTurfChargesEnergyFaster() {
        assertFalse(Terrain.TUNDRA.isHomeTurf(a))
        val home = BattleEngine(listOf(a, b), Random(1), terrain = Terrain.VOLCANO) // Drake is Fire
        val away = BattleEngine(listOf(a, b), Random(1), terrain = Terrain.TUNDRA)
        for (t in 0..2000L step 50) { home.tick(t); away.tick(t) }
        assertTrue(home.snapshot(2000).fighters[0].energy > away.snapshot(2000).fighters[0].energy)
    }

    @Test fun evadeCommandRoundTrips() {
        assertEquals(Protocol.EvadeCmd, Protocol.parse(Protocol.EVADE))
    }

    @Test fun tilapiaComesBackFriedThenSizzlesAway() {
        val fish = FighterInfo("T", Species.TILAPIA, Stage.BABY, 10)
        val bully = FighterInfo("D", Species.DRAKE, Stage.ADULT, 60, listOf(Move.TACKLE))
        val e = BattleEngine(listOf(bully, fish), Random(1), demo = true)
        e.place(0, 0.45f, 0.6f); e.place(1, 0.55f, 0.6f)
        val log = mutableListOf<String>()
        var t = 0L
        while (t < 60_000 && !e.snapshot(t).fighters[1].has(Status.FRIED)) {
            e.command(0, Move.TACKLE, t); log += e.tick(t); t += 50
        }
        val s = e.snapshot(t).fighters[1]
        assertTrue(s.has(Status.FRIED))
        assertTrue(s.hp in 1..30)
        assertFalse(e.ended)
        assertTrue(log.any { "Fried Tilapia" in it })
        val start = t
        while (!e.ended && t < start + 10_000) { e.tick(t); t += 50 } // left alone: loses 5 HP a second
        assertTrue(e.ended)
        assertEquals(0, e.winner)
        assertTrue(t - start in 1000..6600, "${t - start}")
    }

    @Test fun freeForAllAndUnevenTeamsPlayToOneWinner() {
        val pets = listOf(Species.DRAKE, Species.TIGER, Species.ELEPHANT, Species.FOX, Species.BEAR, Species.OWL)
        val infos = pets.mapIndexed { i, sp -> FighterInfo("P$i", sp, Stage.KID, 30, SpeciesKit.starters(sp)) }
        for (teams in listOf(BattleEngine.freeForAll(6), listOf(0, 0, 0, 0, 1, 1), listOf(1, 0, 1))) {
            val n = teams.size
            val e = BattleEngine(infos.take(n), Random(3), teamIds = teams)
            val s0 = e.snapshot(0).fighters
            for (i in 0 until n) for (j in i + 1 until n) // nobody spawns on top of anyone
                assertTrue(kotlin.math.hypot(s0[i].x - s0[j].x, s0[i].y - s0[j].y) > 0.1f, "$teams $i $j")
            var t = 0L
            val rng = Random(5)
            while (!e.ended) {
                for (i in 0 until n) BattleBot.pick(e, i, t, rng)?.let { e.command(i, it, t) }
                e.tick(t); t += 50
            }
            val snap = e.snapshot(t)
            assertTrue(snap.winner == -1 || snap.winner in teams, "$teams ${snap.winner}")
            if (snap.winner >= 0 && t < BATTLE_MS) assertTrue(teams.indices.filter { teams[it] != snap.winner }.all { snap.fighters[it].hp == 0 })
        }
    }

    @Test fun roomMessagesRoundTrip() {
        val players = listOf(Protocol.RoomEntry("Host", 12, 0), Protocol.RoomEntry("A#b~c", 3, 1))
        val lobby = Protocol.parse(Protocol.lobby(1, false, players)) as Protocol.Lobby
        assertEquals(1, lobby.you); assertFalse(lobby.ffa)
        assertEquals(listOf(0, 1), lobby.players.map { it.team })
        assertEquals(Protocol.SwitchTeam, Protocol.parse(Protocol.SWITCH_TEAM))
        val infos = List(3) { FighterInfo("P$it", Species.TILAPIA, Stage.BABY, 5) }
        val setup = Protocol.parse(Protocol.setup(2, infos, Terrain.ARENA, setOf(0, 1, 2), BattleEngine.freeForAll(3))) as Protocol.Setup
        assertEquals(listOf(0, 1, 2), setup.teams)
        assertEquals(setOf(0, 1, 2), setup.humans)
    }
}
