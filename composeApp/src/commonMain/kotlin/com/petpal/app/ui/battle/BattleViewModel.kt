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
import kotlinx.coroutines.flow.*
import kotlin.random.Random
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.TimeSource

enum class BattleScreenMode { LOBBY, HOSTING, SEARCHING, CONNECTING, FIGHT }

/** SOLO = 1v1. DUO_RIVAL = you + CPU vs friend + CPU. DUO_PARTNER = you + friend vs 2 CPUs. */
enum class BattleFormat(val label: String) { SOLO("1v1"), DUO_RIVAL("Duo: friend as rival"), DUO_PARTNER("Duo: friend as partner") }

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
    private var conn: BattleConnection? = null
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
        petRepository.refresh()?.let { FighterInfo(it.name, it.species, it.stage, PetEngine.levelFor(it.xp), it.moves) } ?: error("No pet yet")

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
                if (job === coroutineContext[Job]) { conn?.close(); conn = null }
            }
        }
    }

    /** 1v1 vs a CPU, or (any duo format) you + CPU partner vs 2 CPUs. */
    fun practice(): Unit = start {
        again = ::practice
        val me = myInfo()
        val infos = if (format == BattleFormat.SOLO) listOf(me, CpuOpponent.random(me))
        else listOf(me, CpuOpponent.random(me, prefix = "CPU"), CpuOpponent.random(me), CpuOpponent.random(me))
        runHost(infos, remote = null, remoteSlot = -1, terrain = nextTerrain())
    }

    /** "Battle again" on the result screen. Linked matches: the host re-hosts and the guest reconnects. */
    fun battleAgain() = again?.invoke() ?: leave()

    fun host(kind: LinkKind): Unit = start {
        again = { host(kind) }
        val fmt = format
        _state.value = BattleUiState(mode = BattleScreenMode.HOSTING, link = kind, format = fmt)
        val me = myInfo()
        val c = links[kind].host(me.name).also { conn = it }
        val friend = handshake(c, me)
        // Slots 0,1 are one team and 2,3 the other; empty slots are filled with CPUs.
        val (infos, slot) = when (fmt) {
            BattleFormat.SOLO -> listOf(me, friend) to 1
            BattleFormat.DUO_RIVAL -> listOf(me, CpuOpponent.random(me, prefix = "CPU"), friend, CpuOpponent.random(friend, prefix = "CPU")) to 2
            BattleFormat.DUO_PARTNER -> {
                val lead = if (friend.level > me.level) friend else me
                listOf(me, friend, CpuOpponent.random(lead), CpuOpponent.random(lead)) to 1
            }
        }
        val terrain = nextTerrain()
        c.send(Protocol.setup(slot, infos, terrain))
        runHost(infos, remote = c, remoteSlot = slot, terrain = terrain)
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
        conn = c
        handshake(c, me)
        val setup = withTimeout(10_000) {
            c.incoming.map { Protocol.parse(it) }.filterIsInstance<Protocol.Setup>().first()
        }
        runGuest(setup.infos, setup.you, c, setup.terrain)
    }

    fun use(move: Move) = sendMove(move)
    fun evade() = sendMove(null)

    fun leave() {
        stop()
        _state.value = BattleUiState(format = format)
    }

    private fun stop() {
        job?.cancel(); job = null
        conn?.close(); conn = null
        sendMove = {}
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

    /** Runs the simulation locally (practice, or host of an online match). */
    private suspend fun runHost(
        infos: List<FighterInfo>, remote: BattleConnection?, remoteSlot: Int, terrain: Terrain,
    ) = coroutineScope {
        val engine = BattleEngine(infos, terrain = terrain)
        val rng = Random.Default
        val humans = setOfNotNull(0, remoteSlot.takeIf { remote != null })
        val bots = infos.indices.filter { it !in humans }
        // CPU-only enemies (no friend on the other team) give practice XP
        val practice = humans.all { engine.teamOf(it) == 0 }
        val commands = Channel<Pair<Int, Move?>>(Channel.UNLIMITED)
        sendMove = { commands.trySend(0 to it) }
        var opponentLeft = false
        val reader = remote?.let { c ->
            launch {
                c.incoming.collect { line ->
                    when (val msg = Protocol.parse(line)) {
                        is Protocol.Command -> commands.trySend(remoteSlot to msg.move)
                        Protocol.EvadeCmd -> commands.trySend(remoteSlot to null)
                        else -> Unit
                    }
                }
                opponentLeft = true
            }
        }
        _state.value = BattleUiState(
            mode = BattleScreenMode.FIGHT,
            fight = FightState(0, infos, humans, engine.snapshot(0), practice = practice, terrain = terrain),
            format = format,
        )
        val clock = TimeSource.Monotonic.markNow()
        while (isActive) {
            val now = clock.elapsedNow().inWholeMilliseconds
            val events = mutableListOf<String>()
            while (true) {
                val (i, m) = commands.tryReceive().getOrNull() ?: break
                (if (m == null) engine.evade(i, now) else engine.command(i, m, now))?.let(events::add)
            }
            for (b in bots) {
                if (engine.isDown(b)) continue
                if (BattleBot.shouldEvade(engine, b, now, rng)) engine.evade(b, now)?.let(events::add)
                else BattleBot.pick(engine, b, now, rng)?.let { engine.command(b, it, now)?.let(events::add) }
            }
            events += engine.tick(now)
            val snap = engine.snapshot(now)
            if (remote != null) runCatching {
                events.forEach { remote.send(Protocol.event(it)) }
                remote.send(Protocol.state(snap))
            }
            push(snap, events)
            if (snap.ended) break
            if (opponentLeft) { endEarly("Opponent left the battle"); break }
            delay(50)
        }
        reader?.cancel()
        sendMove = {}
        finish(0, engine.snapshot(clock.elapsedNow().inWholeMilliseconds), practice)
        if (remote != null) delay(1500) // let the final state reach the guest
    }

    private suspend fun runGuest(infos: List<FighterInfo>, you: Int, c: BattleConnection, terrain: Terrain) {
        val teamOf = { i: Int -> i * 2 / infos.size }
        val practice = teamOf(0) == teamOf(you) // partnered with the host vs CPUs
        sendMove = { m -> viewModelScope.launch { runCatching { c.send(if (m == null) Protocol.EVADE else Protocol.command(m)) } } }
        var last: BattleSnapshot? = null
        c.incoming.collect { line ->
            when (val msg = Protocol.parse(line)) {
                is Protocol.State -> {
                    if (_state.value.mode != BattleScreenMode.FIGHT) {
                        _state.value = BattleUiState(mode = BattleScreenMode.FIGHT, format = format,
                            fight = FightState(you, infos, setOf(0, you), msg.snap, practice = practice, terrain = terrain))
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
