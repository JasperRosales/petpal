package com.petpal.app.ui.battle

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.petpal.app.data.repository.PetRepository
import com.petpal.app.domain.battle.*
import com.petpal.app.domain.logic.PetEngine
import com.petpal.app.domain.model.BattleOutcome
import com.petpal.app.domain.platform.*
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.flow.*
import kotlin.random.Random
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.TimeSource

enum class BattleScreenMode { LOBBY, HOSTING, SEARCHING, CONNECTING, FIGHT }

/**
 * SOLO = 1v1. DUO_RIVAL = you + CPU vs friend + CPU. DUO_PARTNER = you + friend vs 2 CPUs.
 * QUAD = four players: you + the first friend to join vs the next two.
 */
enum class BattleFormat(val label: String, val guests: Int = 1) {
    SOLO("1v1"), DUO_RIVAL("Duo: friend as rival"), DUO_PARTNER("Duo: friend as partner"), QUAD("2v2: 4 players", guests = 3),
}

data class FightState(
    val me: Int,
    val fighters: List<FighterInfo>,
    val humans: Set<Int> = setOf(me),
    val snap: BattleSnapshot,
    val log: List<String> = emptyList(),
    val practice: Boolean = false,
    val result: String? = null, // set when over
    val terrain: Terrain = Terrain.ARENA,
)

data class BattleUiState(
    val mode: BattleScreenMode = BattleScreenMode.LOBBY,
    val link: LinkKind? = null,
    val peers: List<Peer> = emptyList(),
    val message: String? = null,
    val fight: FightState? = null,
    val format: BattleFormat = BattleFormat.SOLO,
)

class BattleViewModel(
    private val links: BattleLinks,
    private val petRepository: PetRepository,
) : ViewModel() {
    private val _state = MutableStateFlow(BattleUiState())
    val state: StateFlow<BattleUiState> = _state.asStateFlow()

    private var job: Job? = null
    private val conns = mutableListOf<BattleConnection>()
    /** null = evade */
    private var sendMove: (Move?) -> Unit = {}
    /** Map rotation: each match hosted here moves on to the next terrain (random starting point). */
    private var mapIndex = Random.nextInt(Terrain.entries.size)
    private fun nextTerrain() = Terrain.rotation(mapIndex++)
    /** Re-runs the last kind of match (same CPU format, same link, same host). */
    private var again: (() -> Unit)? = null

    fun isAvailable(kind: LinkKind) = links[kind].isAvailable

    fun setFormat(f: BattleFormat) = _state.update { it.copy(format = f) }
    private val format get() = _state.value.format

    private suspend fun myInfo(): FighterInfo =
        petRepository.refresh()?.let { PetEngine.fighterInfo(it) } ?: error("No pet yet")

    private fun start(block: suspend CoroutineScope.() -> Unit) {
        stop()
        val keep = format
        job = viewModelScope.launch {
            try {
                block()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _state.update { BattleUiState(message = "Connection failed: ${e.message ?: e::class.simpleName}", format = keep) }
            } finally {
                // A cancelled old job can finish after a new one connected: only clean up our own link.
                if (job === coroutineContext[Job]) closeLinks()
            }
        }
    }

    /** 1v1 vs a CPU, or (any duo format) you + CPU partner vs 2 CPUs. */
    fun practice(): Unit = start {
        again = ::practice
        val me = myInfo()
        val infos = if (format == BattleFormat.SOLO) listOf(me, CpuOpponent.random(me))
        else listOf(me, CpuOpponent.random(me, prefix = "CPU"), CpuOpponent.random(me), CpuOpponent.random(me))
        runHost(infos, remotes = emptyMap(), terrain = nextTerrain())
    }

    /** "Battle again" on the result screen. Linked matches: the host re-hosts and the guest reconnects. */
    fun battleAgain() = again?.invoke() ?: leave()

    fun host(kind: LinkKind): Unit = start {
        again = { host(kind) }
        val fmt = format
        _state.value = BattleUiState(mode = BattleScreenMode.HOSTING, link = kind, format = fmt)
        val me = myInfo()
        val joined = mutableListOf<Pair<BattleConnection, FighterInfo>>()
        if (fmt.guests > 1) _state.update { it.copy(message = "0 of ${fmt.guests} friends joined") }
        links[kind].hostMany(me.name, fmt.guests).collect { c ->
            conns += c
            joined += c to handshake(c, me)
            if (fmt.guests > 1) _state.update {
                it.copy(message = "${joined.size} of ${fmt.guests} friends joined: " + joined.joinToString { j -> j.second.name })
            }
        }
        val friend = joined.first().second
        // Slots 0,1 are one team and 2,3 the other; empty slots are filled with CPUs.
        val (infos, slots) = when (fmt) {
            BattleFormat.SOLO -> listOf(me, friend) to listOf(1)
            BattleFormat.DUO_RIVAL ->
                listOf(me, CpuOpponent.random(me, prefix = "CPU"), friend, CpuOpponent.random(friend, prefix = "CPU")) to listOf(2)
            BattleFormat.DUO_PARTNER -> {
                val lead = if (friend.level > me.level) friend else me
                listOf(me, friend, CpuOpponent.random(lead), CpuOpponent.random(lead)) to listOf(1)
            }
            // Join order picks teams: the first friend partners the host, the next two team up.
            BattleFormat.QUAD -> listOf(me) + joined.map { it.second } to listOf(1, 2, 3)
        }
        val remotes = slots.zip(joined.map { it.first }).toMap()
        val terrain = nextTerrain()
        val humans = setOf(0) + remotes.keys
        remotes.forEach { (slot, c) -> c.send(Protocol.setup(slot, infos, terrain, humans)) }
        runHost(infos, remotes, terrain)
    }

    fun search(kind: LinkKind) = start {
        _state.value = BattleUiState(mode = BattleScreenMode.SEARCHING, link = kind, format = format)
        links[kind].discover().collect { peers -> _state.update { it.copy(peers = peers) } }
    }

    fun join(peer: Peer) {
        val kind = _state.value.link ?: return
        join(kind, peer, rematch = false)
    }

    private fun join(kind: LinkKind, peer: Peer, rematch: Boolean): Unit = start {
        again = { join(kind, peer, rematch = true) }
        _state.value = BattleUiState(mode = BattleScreenMode.CONNECTING, link = kind, format = format,
            message = if (rematch) "${peer.name} (waiting for the host to tap Battle again)" else peer.name)
        val me = myInfo()
        // On a rematch the host may not be listening yet, so keep knocking for a while.
        val giveUpAt = TimeSource.Monotonic.markNow() + (if (rematch) 30_000 else 0).milliseconds
        var c: BattleConnection
        while (true) {
            try {
                c = links[kind].connect(peer); break
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                if (giveUpAt.hasPassedNow()) throw e
                delay(1000)
            }
        }
        conns += c
        handshake(c, me)
        val setup = withTimeout(10_000) {
            c.incoming.map { Protocol.parse(it) }.filterIsInstance<Protocol.Setup>().first()
        }
        runGuest(setup.infos, setup.you, setup.humans, c, setup.terrain)
    }

    fun use(move: Move) = sendMove(move)
    fun evade() = sendMove(null)

    fun leave() {
        stop()
        _state.value = BattleUiState(format = format)
    }

    private fun stop() {
        job?.cancel(); job = null
        closeLinks()
        sendMove = {}
    }

    private fun closeLinks() {
        conns.forEach { it.close() }
        conns.clear()
    }

    override fun onCleared() = stop()

    private suspend fun handshake(c: BattleConnection, me: FighterInfo): FighterInfo {
        c.send(Protocol.hello(me))
        val hello = withTimeout(10_000) {
            c.incoming.map { Protocol.parse(it) }.filterIsInstance<Protocol.Hello>().first()
        }
        require(hello.version == Protocol.VERSION) { "Opponent has a different PetPal version" }
        return hello.info
    }

    /** Runs the simulation locally (practice, or host of a linked match). [remotes]: slot -> that player's link. */
    private suspend fun runHost(
        infos: List<FighterInfo>, remotes: Map<Int, BattleConnection>, terrain: Terrain,
    ) = coroutineScope {
        val engine = BattleEngine(infos, terrain = terrain)
        val rng = Random.Default
        val humans = setOf(0) + remotes.keys
        val bots = infos.indices.filter { it !in humans }.toMutableSet()
        // CPU-only enemies (no friend on the other team) give practice XP
        val practice = humans.all { engine.teamOf(it) == 0 }
        val commands = Channel<Pair<Int, Move?>>(Channel.UNLIMITED)
        sendMove = { commands.trySend(0 to it) }
        val left = Channel<Int>(Channel.UNLIMITED)
        val live = remotes.toMutableMap()
        val readers = remotes.map { (slot, c) ->
            launch {
                c.incoming.collect { line ->
                    when (val msg = Protocol.parse(line)) {
                        is Protocol.Command -> commands.trySend(slot to msg.move)
                        Protocol.EvadeCmd -> commands.trySend(slot to null)
                        else -> Unit
                    }
                }
                left.trySend(slot)
            }
        }
        var quitMsg: String? = null
        _state.value = BattleUiState(
            mode = BattleScreenMode.FIGHT,
            fight = FightState(0, infos, humans, engine.snapshot(0), practice = practice, terrain = terrain),
            format = format,
        )
        val clock = TimeSource.Monotonic.markNow()
        // One writer per guest so a slow link (Bluetooth) never stalls the sim; stale states are dropped.
        val outboxes = remotes.mapValues { (_, c) -> Outbox(c).also { it.start(this) } }
        var nextTick = 0L
        withContext(Dispatchers.Default) {
        while (isActive) {
            val now = clock.elapsedNow().inWholeMilliseconds
            val events = mutableListOf<String>()
            while (true) {
                val slot = left.tryReceive().getOrNull() ?: break
                live.remove(slot)
                // A CPU takes over while players remain on both teams (or on the team vs CPUs); otherwise it's off.
                val teams = (setOf(0) + live.keys).map { engine.teamOf(it) }.toSet()
                if (if (practice) live.isEmpty() else teams.size < 2) {
                    quitMsg = if (remotes.size > 1) "${infos[slot].name} left the battle" else "Opponent left the battle"
                } else {
                    bots += slot
                    events += "${infos[slot].name} left. A CPU takes over"
                }
            }
            while (true) {
                val (i, m) = commands.tryReceive().getOrNull() ?: break
                if (i in bots) continue
                (if (m == null) engine.evade(i, now) else engine.command(i, m, now))?.let(events::add)
            }
            for (b in bots) {
                if (engine.isDown(b)) continue
                if (BattleBot.shouldEvade(engine, b, now, rng)) engine.evade(b, now)?.let(events::add)
                else BattleBot.pick(engine, b, now, rng)?.let { engine.command(b, it, now)?.let(events::add) }
            }
            events += engine.tick(now)
            val snap = engine.snapshot(now)
            if (live.isNotEmpty()) {
                val stateLine = Protocol.state(snap)
                live.keys.forEach { outboxes[it]?.offer(events, stateLine) }
            }
            push(snap, events)
            if (snap.ended) break
            val quit = quitMsg
            if (quit != null) { endEarly(quit); break }
            // Fixed 20 Hz rate: sleep only what's left of this tick.
            nextTick += TICK_MS
            val wait = nextTick - clock.elapsedNow().inWholeMilliseconds
            if (wait > 0) delay(wait) else { nextTick = clock.elapsedNow().inWholeMilliseconds; yield() }
        }
        } // Default
        readers.forEach { it.cancel() }
        sendMove = {}
        finish(0, engine.snapshot(clock.elapsedNow().inWholeMilliseconds), practice)
        if (remotes.isNotEmpty()) delay(1500) // let the final state reach the guests
        outboxes.values.forEach { it.stop() }
    }

    private suspend fun runGuest(infos: List<FighterInfo>, you: Int, humans: Set<Int>, c: BattleConnection, terrain: Terrain) {
        val teamOf = { i: Int -> i * 2 / infos.size }
        val practice = humans.all { teamOf(it) == teamOf(0) } // every player on one team vs CPUs
        sendMove = { m -> viewModelScope.launch { runCatching { c.send(if (m == null) Protocol.EVADE else Protocol.command(m)) } } }
        var last: BattleSnapshot? = null
        c.incoming.map { Protocol.parse(it) }.flowOn(Dispatchers.Default).collect { msg ->
            when (msg) {
                is Protocol.State -> {
                    if (_state.value.mode != BattleScreenMode.FIGHT) {
                        _state.value = BattleUiState(mode = BattleScreenMode.FIGHT, format = format,
                            fight = FightState(you, infos, humans, msg.snap, practice = practice, terrain = terrain))
                    }
                    last = msg.snap
                    push(msg.snap, emptyList())
                    if (msg.snap.ended) {
                        finish(you, msg.snap, practice)
                        sendMove = {}
                        return@collect
                    }
                }
                is Protocol.Event -> push(null, listOf(msg.text))
                else -> Unit
            }
        }
        if (last?.ended != true) endEarly("Host left the battle")
    }

    private fun push(snap: BattleSnapshot?, events: List<String>) = _state.update { s ->
        val f = s.fight ?: return@update s
        if (f.result != null) return@update s
        s.copy(fight = f.copy(snap = snap ?: f.snap, log = (f.log + events).takeLast(4)))
    }

    private fun endEarly(msg: String) = _state.update { s ->
        s.copy(fight = s.fight?.copy(result = s.fight.result ?: "$msg. No XP this time."))
    }

    private suspend fun finish(me: Int, snap: BattleSnapshot, practice: Boolean) {
        if (_state.value.fight?.result != null) return
        val fighters = _state.value.fight?.fighters.orEmpty()
        val teamOf = { i: Int -> i * 2 / fighters.size.coerceAtLeast(2) }
        val outcome = when (snap.winner) {
            -1 -> BattleOutcome.DRAW
            teamOf(me) -> BattleOutcome.WIN
            else -> BattleOutcome.LOSS
        }
        val title = when (outcome) {
            BattleOutcome.WIN -> "You win!"
            BattleOutcome.LOSS -> "You lost"
            BattleOutcome.DRAW -> "Draw"
        }
        val rivals = fighters.indices.filter { teamOf(it) != teamOf(me) }.joinToString(" & ") { fighters[it].name }
        val xp = petRepository.recordBattle(outcome, rivals, practice)
        val detail = "+$xp XP${if (practice) " (CPU)" else ""}"
        _state.update { s -> s.copy(fight = s.fight?.copy(snap = snap, result = "$title  $detail")) }
    }
}

private const val TICK_MS = 50L

/** Latest-state-wins sender: events are kept, but only the newest state is written; one flush per batch. */
private class Outbox(private val c: BattleConnection) {
    private val lock = kotlinx.coroutines.sync.Mutex()
    private val events = mutableListOf<String>()
    private var state: String? = null
    private val wake = Channel<Unit>(Channel.CONFLATED)
    private var job: Job? = null

    suspend fun offer(newEvents: List<String>, stateLine: String) {
        lock.withLock {
            newEvents.mapTo(events) { Protocol.event(it) }
            state = stateLine
        }
        wake.trySend(Unit)
    }

    fun start(scope: CoroutineScope) {
        job = scope.launch(Dispatchers.Default) {
            for (u in wake) {
                val batch = lock.withLock {
                    val b = events + listOfNotNull(state)
                    events.clear(); state = null
                    b
                }
                if (batch.isNotEmpty()) runCatching { c.send(batch.joinToString("\n")) }
            }
        }
    }

    suspend fun stop() { wake.close(); job?.join() }
}
