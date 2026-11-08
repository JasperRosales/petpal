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
import kotlin.time.TimeSource

enum class BattleScreenMode { LOBBY, HOSTING, SEARCHING, CONNECTING, FIGHT }

data class FightState(
    val me: Int,
    val fighters: List<FighterInfo>,
    val snap: BattleSnapshot,
    val log: List<String> = emptyList(),
    val practice: Boolean = false,
    val result: String? = null, // set when over
)

data class BattleUiState(
    val mode: BattleScreenMode = BattleScreenMode.LOBBY,
    val link: LinkKind? = null,
    val peers: List<Peer> = emptyList(),
    val message: String? = null,
    val fight: FightState? = null,
)

class BattleViewModel(
    private val links: BattleLinks,
    private val petRepository: PetRepository,
) : ViewModel() {
    private val _state = MutableStateFlow(BattleUiState())
    val state: StateFlow<BattleUiState> = _state.asStateFlow()

    private var job: Job? = null
    private var conn: BattleConnection? = null
    private var sendMove: (Move) -> Unit = {}

    fun isAvailable(kind: LinkKind) = links[kind].isAvailable

    private suspend fun myInfo(): FighterInfo =
        petRepository.refresh()?.let { FighterInfo(it.name, it.species, it.stage, PetEngine.levelFor(it.xp), it.moves) } ?: error("No pet yet")

    private fun start(block: suspend CoroutineScope.() -> Unit) {
        stop()
        job = viewModelScope.launch {
            try {
                block()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _state.update { BattleUiState(message = "Connection failed: ${e.message ?: e::class.simpleName}") }
            } finally {
                conn?.close(); conn = null
            }
        }
    }

    fun practice() = start {
        val me = myInfo()
        val cpu = me.copy(name = "Wild ${me.species.label}")
        runHost(listOf(me, cpu), remote = null)
    }

    fun host(kind: LinkKind) = start {
        _state.value = BattleUiState(mode = BattleScreenMode.HOSTING, link = kind)
        val me = myInfo()
        val c = links[kind].host(me.name).also { conn = it }
        val opp = handshake(c, me)
        runHost(listOf(me, opp), remote = c)
    }

    fun search(kind: LinkKind) = start {
        _state.value = BattleUiState(mode = BattleScreenMode.SEARCHING, link = kind)
        links[kind].discover().collect { peers -> _state.update { it.copy(peers = peers) } }
    }

    fun join(peer: Peer) {
        val kind = _state.value.link ?: return
        start {
            _state.value = BattleUiState(mode = BattleScreenMode.CONNECTING, link = kind, message = peer.name)
            val me = myInfo()
            val c = links[kind].connect(peer).also { conn = it }
            val opp = handshake(c, me)
            runGuest(listOf(opp, me), c)
        }
    }

    fun use(move: Move) = sendMove(move)

    fun leave() {
        stop()
        _state.value = BattleUiState()
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
    private suspend fun runHost(infos: List<FighterInfo>, remote: BattleConnection?) = coroutineScope {
        val engine = BattleEngine(infos)
        val rng = Random.Default
        val commands = Channel<Pair<Int, Move>>(Channel.UNLIMITED)
        sendMove = { commands.trySend(0 to it) }
        var opponentLeft = false
        val reader = remote?.let { c ->
            launch {
                c.incoming.collect { line ->
                    (Protocol.parse(line) as? Protocol.Command)?.let { commands.trySend(1 to it.move) }
                }
                opponentLeft = true
            }
        }
        _state.value = BattleUiState(
            mode = BattleScreenMode.FIGHT,
            fight = FightState(0, infos, engine.snapshot(0), practice = remote == null),
        )
        val clock = TimeSource.Monotonic.markNow()
        while (isActive) {
            val now = clock.elapsedNow().inWholeMilliseconds
            val events = mutableListOf<String>()
            while (true) {
                val (i, m) = commands.tryReceive().getOrNull() ?: break
                engine.command(i, m, now)?.let(events::add)
            }
            if (remote == null) BattleBot.pick(engine, 1, now, rng)?.let { engine.command(1, it, now)?.let(events::add) }
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
        finish(0, engine.snapshot(clock.elapsedNow().inWholeMilliseconds), remote == null)
        if (remote != null) delay(1500) // let the final state reach the guest
    }

    private suspend fun runGuest(infos: List<FighterInfo>, c: BattleConnection) {
        sendMove = { m -> viewModelScope.launch { runCatching { c.send(Protocol.command(m)) } } }
        var last: BattleSnapshot? = null
        c.incoming.collect { line ->
            when (val msg = Protocol.parse(line)) {
                is Protocol.State -> {
                    if (_state.value.mode != BattleScreenMode.FIGHT) {
                        _state.value = BattleUiState(mode = BattleScreenMode.FIGHT, fight = FightState(1, infos, msg.snap))
                    }
                    last = msg.snap
                    push(msg.snap, emptyList())
                    if (msg.snap.ended) {
                        finish(1, msg.snap, practice = false)
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
        val outcome = when (snap.winner) {
            -1 -> BattleOutcome.DRAW
            me -> BattleOutcome.WIN
            else -> BattleOutcome.LOSS
        }
        val title = when (outcome) {
            BattleOutcome.WIN -> "You win!"
            BattleOutcome.LOSS -> "You lost"
            BattleOutcome.DRAW -> "Draw"
        }
        val detail = if (practice) "Practice battles don't give XP." else {
            val xp = petRepository.recordBattle(outcome, _state.value.fight?.fighters?.getOrNull(1 - me)?.name ?: "")
            if (xp > 0) "+$xp XP" else "Daily battle XP limit reached"
        }
        _state.update { s -> s.copy(fight = s.fight?.copy(snap = snap, result = "$title  $detail")) }
    }
}
