package com.petpal.app.ui.battle

import com.petpal.app.data.local.*
import com.petpal.app.data.repository.PetRepository
import com.petpal.app.domain.model.Species
import com.petpal.app.domain.platform.*
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import java.util.concurrent.Executors
import kotlin.test.*

/** Two (or more) view models linked by an in-memory transport: catches handshake/setup bugs per format. */
class LinkedBattleTest {
    private val main = Executors.newSingleThreadExecutor().asCoroutineDispatcher()

    @BeforeTest fun setUp() = Dispatchers.setMain(main)
    @AfterTest fun tearDown() { Dispatchers.resetMain(); main.close() }

    private class Pipe : BattleConnection {
        val inbox = Channel<String>(Channel.UNLIMITED)
        lateinit var other: Pipe
        override val incoming: Flow<String> = inbox.receiveAsFlow()
        override suspend fun send(line: String) { line.split('\n').forEach { other.inbox.trySend(it) } }
        override fun close() { inbox.close(); other.inbox.close() }
    }

    private class FakeLink : BattleTransport {
        val pending = Channel<BattleConnection>(Channel.UNLIMITED)
        override val kind = LinkKind.HOTSPOT
        override val isAvailable = true
        override fun hostMany(name: String, count: Int) = pending.receiveAsFlow().take(count)
        override fun discover() = flowOf(listOf(Peer("host", "Host")))
        override suspend fun connect(peer: Peer): BattleConnection {
            val a = Pipe(); val b = Pipe(); a.other = b; b.other = a
            pending.send(a); return b
        }
    }

    private class Pets : PetDao {
        val pet = MutableStateFlow<PetEntity?>(null)
        override fun observePet() = pet
        override suspend fun getPet() = pet.value
        override fun observeAll() = pet.map { listOfNotNull(it) }
        override suspend fun getAll() = listOfNotNull(pet.value)
        override suspend fun setActive(id: Long) {}
        override suspend fun upsert(pet: PetEntity) { this.pet.value = pet }
    }
    private object Habits : HabitLogDao {
        override suspend fun insert(log: HabitLogEntity) {}
        override fun observeSince(from: Long) = flowOf(emptyList<HabitLogEntity>())
        override fun observeAllTimestamps() = flowOf(emptyList<Long>())
    }
    private object Battles : BattleLogDao {
        override suspend fun insert(log: BattleLogEntity) {}
        override fun observeRecent() = flowOf(emptyList<BattleLogEntity>())
        override fun observeSince(from: Long) = flowOf(emptyList<BattleLogEntity>())
        override suspend fun delete(id: Long) {}
        override suspend fun deleteAll() {}
        override suspend fun rewardedSince(from: Long) = 0
    }
    private object Steps : StepDayDao {
        override suspend fun latest(): StepDayEntity? = null
        override fun observeLatest() = flowOf<StepDayEntity?>(null)
        override fun observeLifetime() = flowOf(0L)
        override suspend fun lifetime() = 0L
        override fun observeFrom(from: String) = flowOf(emptyList<StepDayEntity>())
        override suspend fun upsert(day: StepDayEntity) {}
    }

    /** Pairs the first two callers like the server: first in line hosts. */
    private class FakeOnline : OnlineService {
        val waiting = Channel<Pair<String, CompletableDeferred<OnlineMatch>>>(1)
        override suspend fun findMatch(me: com.petpal.app.domain.battle.FighterInfo): OnlineMatch {
            val other = waiting.tryReceive().getOrNull()
            if (other == null) {
                val mine = CompletableDeferred<OnlineMatch>()
                waiting.send(me.name to mine)
                return mine.await()
            }
            val a = Pipe(); val b = Pipe(); a.other = b; b.other = a
            other.second.complete(OnlineMatch(true, me.name, a))
            return OnlineMatch(false, other.first, b)
        }
        override suspend fun leaderboard() = Leaderboard(emptyList(), RankEntry(3, "x", null, null, 1016, 1, 0, 0))
    }

    private fun vm(link: FakeLink, name: String, species: Species, online: OnlineService? = null): BattleViewModel {
        val repo = PetRepository(Pets(), Habits, Battles, Steps, { System.currentTimeMillis() })
        runBlocking { repo.adopt(name, species) }
        return BattleViewModel(BattleLinks(listOf(link)), repo, online)
    }

    @Test fun online() {
        val server = FakeOnline()
        val a = vm(FakeLink(), "Ann", Species.entries[0], server)
        val b = vm(FakeLink(), "Ben", Species.entries[1], server)
        runBlocking(main) { a.findOnline() }
        waitFor("queued") { a.state.value.mode == BattleScreenMode.MATCHMAKING }
        runBlocking(main) { b.findOnline() }
        waitFor("both fighting; a=${a.state.value} b=${b.state.value}") {
            a.state.value.mode == BattleScreenMode.FIGHT && b.state.value.mode == BattleScreenMode.FIGHT
        }
        assertEquals(listOf("Ann", "Ben"), b.state.value.fight!!.fighters.map { it.name })
        assertEquals(1, b.state.value.fight!!.me)
        runBlocking(main) { b.leave() }
        waitFor("host sees rating") { a.state.value.fight?.result?.contains("Rating 1016  #3") == true }
        runBlocking(main) { a.leave() }
    }

    private fun waitFor(what: String, ms: Long = 5000, ok: () -> Boolean) = runBlocking {
        withTimeoutOrNull(ms) { while (!ok()) delay(20) } ?: fail("timed out waiting for $what")
    }

    private fun linked(format: BattleFormat) {
        val link = FakeLink()
        val host = vm(link, "Hosty", Species.entries[0])
        val guest = vm(link, "Guesty", Species.entries[1])
        runBlocking(main) {
            host.setFormat(format)
            host.host(LinkKind.HOTSPOT)
            guest.search(LinkKind.HOTSPOT)
        }
        waitFor("peers") { guest.state.value.peers.isNotEmpty() }
        runBlocking(main) { guest.join(guest.state.value.peers.first()) }
        if (format.isRoom) {
            waitFor("room") { host.state.value.room?.canStart == true }
            runBlocking(main) { host.startRoom() }
        }
        waitFor("host fight; host=${host.state.value} guest=${guest.state.value}") { host.state.value.mode == BattleScreenMode.FIGHT }
        waitFor("guest fight; guest=${guest.state.value}") { guest.state.value.mode == BattleScreenMode.FIGHT }
        val g = guest.state.value.fight!!
        val h = host.state.value.fight!!
        assertEquals(h.fighters.map { it.name }, g.fighters.map { it.name })
        assertEquals(h.teams, g.teams)
        assertEquals(h.humans, g.humans)
        assertEquals("Guesty", g.fighters[g.me].name)
        // The guest's move reaches the host's sim.
        val before = h.snap.fighters[g.me].energy
        val move = g.fighters[g.me].loadout.first()
        runBlocking(main) { guest.use(move) }
        waitFor("guest move on host") {
            host.state.value.fight!!.snap.fighters[g.me].let { it.action == move || it.energy != before }
        }
        // Leaving ends the match for the other side.
        runBlocking(main) { guest.leave() }
        waitFor("host notices") { host.state.value.fight?.result != null || host.state.value.fight?.log.orEmpty().any { "left" in it } }
        runBlocking(main) { host.leave() }
    }

    @Test fun solo() = linked(BattleFormat.SOLO)
    @Test fun teams() = linked(BattleFormat.TEAMS)
    @Test fun ffa() = linked(BattleFormat.FFA)
}
