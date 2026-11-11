package com.petpal.app.ui.battle

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.petpal.app.data.repository.PetRepository
import com.petpal.app.data.repository.RewardRepository
import com.petpal.app.domain.logic.Events
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

enum class BattleScreenMode { LOBBY, HOSTING, SEARCHING, CONNECTING, ROOM, FIGHT, MATCHMAKING, LEADERBOARD }

/** Most people in one room (host included). */
const val MAX_PLAYERS = 6

/**
 * SOLO = 1v1. DUO_RIVAL = you + CPU vs friend + CPU. DUO_PARTNER = you + friend vs 2 CPUs.
 * TEAMS / FFA: a room of up to [MAX_PLAYERS]; players pick teams (or it's everyone for themselves) and the host starts.
 */
enum class BattleFormat(val label: String, val guests: Int = 1) {
    SOLO("1v1"), DUO_RIVAL("Duo: friend as rival"), DUO_PARTNER("Duo: friend as partner"),
    TEAMS("Team battle", guests = MAX_PLAYERS - 1), FFA("Free-for-all", guests = MAX_PLAYERS - 1);

    val isRoom get() = this == TEAMS || this == FFA
}

/** Waiting room before a team or free-for-all match. [you] = your entry in [players]. */
data class RoomUi(
    val ffa: Boolean,
    val players: List<Protocol.RoomEntry>,
    val you: Int,
    val host: Boolean,
    val canStart: Boolean,
)

private sealed interface RoomEv {
    class Joined(val c: BattleConnection, val info: FighterInfo) : RoomEv
    class Left(val c: BattleConnection) : RoomEv
    class Switch(val c: BattleConnection) : RoomEv
    class HostSwitch(val index: Int) : RoomEv
    data object Start : RoomEv
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
    /** Team id per fighter. */
    val teams: List<Int> = BattleEngine.defaultTeams(fighters.size),
)

data class BattleUiState(
    val mode: BattleScreenMode = BattleScreenMode.LOBBY,
    val link: LinkKind? = null,
    val peers: List<Peer> = emptyList(),
    val message: String? = null,
    val fight: FightState? = null,
    val format: BattleFormat = BattleFormat.SOLO,
    val room: RoomUi? = null,
    /** Online leaderboard screen: null while loading. */
    val board: Leaderboard? = null,
    val boardError: String? = null,
)

class BattleViewModel(
    private val links: BattleLinks,
    private val petRepository: PetRepository,
    private val online: OnlineService? = null,
    private val rewards: RewardRepository? = null,
) : ViewModel() {
    /** The limited-time boss is open and not beaten yet. */
    val bossOpen: StateFlow<Boolean> = (rewards?.claimed ?: flowOf(emptyList())).map { keys ->
        rewards != null && Events.BOSS_KEY !in keys && Events.bossOpen(rewards.today())
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    /** Current match is the event boss: a win unlocks a pet. */
    private var bossFight = false

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
    /** Host side of an open room. */
    private var roomEvents: Channel<RoomEv>? = null
    /** Guest side of an open room. */
    private var roomConn: BattleConnection? = null

    fun isAvailable(kind: LinkKind) = links[kind].isAvailable

    fun setFormat(f: BattleFormat) = _state.update { it.copy(format = f) }
    private val format get() = _state.value.format

    private suspend fun myInfo(): FighterInfo =
        petRepository.refresh()?.let { PetEngine.fighterInfo(it) } ?: error("No pet yet")

    private fun start(block: suspend CoroutineScope.() -> Unit) {
        stop()
        bossFight = false
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

    /** 1v1 vs a CPU, duo (you + CPU vs 2 CPUs), 3v3 with CPU partners, or a 4-pet free-for-all. */
    fun practice(): Unit = start {
        again = ::practice
        val me = myInfo()
        val ally = { CpuOpponent.random(me, prefix = "CPU") }
        val foe = { CpuOpponent.random(me) }
        val (infos, teams) = when (format) {
            BattleFormat.SOLO -> listOf(me, foe()).let { it to BattleEngine.defaultTeams(2) }
            BattleFormat.FFA -> listOf(me, foe(), foe(), foe()).let { it to BattleEngine.freeForAll(4) }
            BattleFormat.TEAMS -> listOf(me, ally(), ally(), foe(), foe(), foe()).let { it to BattleEngine.defaultTeams(6) }
            else -> listOf(me, ally(), foe(), foe()).let { it to BattleEngine.defaultTeams(4) }
        }
        runHost(infos, remotes = emptyMap(), terrain = nextTerrain(), teams = teams)
    }

    /** Event boss: 1v1 vs a random Lv 35 pet with bonus HP. Beat it once for a random pet. */
    fun boss(): Unit = start {
        again = ::boss
        bossFight = true
        runHost(listOf(myInfo(), Events.boss()), remotes = emptyMap(), terrain = nextTerrain())
    }

    /** "Battle again" on the result screen. Linked matches: the host re-hosts and the guest reconnects. */
    fun battleAgain() = again?.invoke() ?: leave()

    fun host(kind: LinkKind): Unit = start {
        again = { host(kind) }
        val fmt = format
        if (fmt.isRoom) {
            _state.value = BattleUiState(mode = BattleScreenMode.ROOM, link = kind, format = fmt)
            return@start hostRoom(kind, fmt == BattleFormat.FFA)
        }
        _state.value = BattleUiState(mode = BattleScreenMode.HOSTING, link = kind, format = fmt)
        val me = myInfo()
        val joined = mutableListOf<Pair<BattleConnection, FighterInfo>>()
        if (fmt.guests > 1) _state.update { it.copy(message = "0 of ${fmt.guests} friends joined") }
        // A phone that drops during the handshake is skipped; keep listening until everyone's in.
        links[kind].hostMany(me.name, Int.MAX_VALUE).mapNotNull { c ->
            conns += c
            try {
                c to handshake(c, me)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                if (e is VersionMismatch) _state.update { it.copy(message = e.message) }
                c.close(); conns.remove(c); null
            }
        }.take(fmt.guests).collect { j ->
            joined += j
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
            BattleFormat.TEAMS, BattleFormat.FFA -> error("rooms are hosted by hostRoom")
        }
        val remotes = slots.zip(joined.map { it.first }).toMap()
        val terrain = nextTerrain()
        val humans = setOf(0) + remotes.keys
        remotes.forEach { (slot, c) -> c.send(Protocol.setup(slot, infos, terrain, humans)) }
        runHost(infos, remotes, terrain)
    }

    /**
     * Open room: friends join (up to [MAX_PLAYERS] in all), switch teams, and the host taps Start.
     * The host is always slot 0; in team battles the host's team spawns on the left.
     */
    private suspend fun hostRoom(kind: LinkKind, ffa: Boolean) = coroutineScope {
        val me = myInfo()
        class Member(val c: BattleConnection?, val info: FighterInfo, var team: Int)
        val members = mutableListOf(Member(null, me, 0))
        val events = Channel<RoomEv>(Channel.UNLIMITED)
        roomEvents = events
        fun canStart() = members.size >= 2 && (ffa || members.map { it.team }.toSet().size == 2)
        suspend fun publish() {
            val entries = members.map { Protocol.RoomEntry(it.info.name, it.info.level, if (ffa) 0 else it.team) }
            _state.update { it.copy(mode = BattleScreenMode.ROOM, room = RoomUi(ffa, entries, 0, host = true, canStart = canStart())) }
            members.forEachIndexed { i, m -> m.c?.let { c -> runCatching { c.send(Protocol.lobby(i, ffa, entries)) } } }
        }
        val accept = launch {
            links[kind].hostMany(me.name, MAX_PLAYERS - 1).collect { c ->
                conns += c
                launch {
                    try {
                        val info = handshake(c, me)
                        events.send(RoomEv.Joined(c, info))
                        c.incoming.collect { if (Protocol.parse(it) == Protocol.SwitchTeam) events.send(RoomEv.Switch(c)) }
                    } catch (e: CancellationException) {
                        throw e
                    } catch (_: Exception) {
                    }
                    events.send(RoomEv.Left(c)) // also cleans up phones that never finished joining
                }
            }
        }
        publish()
        for (ev in events) {
            when (ev) {
                is RoomEv.Joined -> if (members.size < MAX_PLAYERS) {
                    // New players balance the teams (ties go to the other side, so 1v1 is the default)
                    val team = if (members.count { it.team == 1 } <= members.count { it.team == 0 }) 1 else 0
                    members += Member(ev.c, ev.info, team)
                } else ev.c.close()
                is RoomEv.Left -> { members.removeAll { it.c === ev.c }; ev.c.close(); conns.remove(ev.c) }
                is RoomEv.Switch -> if (!ffa) members.firstOrNull { it.c === ev.c }?.let { it.team = 1 - it.team }
                is RoomEv.HostSwitch -> if (!ffa) members.getOrNull(ev.index)?.let { it.team = 1 - it.team }
                RoomEv.Start -> if (canStart()) break
            }
            publish()
        }
        roomEvents = null
        accept.cancelAndJoin() // stop accepting and stop the room readers before the match reads the links
        val inRoom = members.mapNotNull { it.c }.toSet()
        conns.filter { it !in inRoom }.forEach { it.close() }
        conns.retainAll(inRoom)
        val order = if (ffa) members.toList() else members.sortedBy { if (it.team == members[0].team) 0 else 1 }
        val infos = order.map { it.info }
        val teams = if (ffa) BattleEngine.freeForAll(order.size) else order.map { it.team }
        val remotes = order.withIndex().filter { it.value.c != null }.associate { it.index to it.value.c!! }
        val terrain = nextTerrain()
        remotes.forEach { (slot, c) -> c.send(Protocol.setup(slot, infos, terrain, order.indices.toSet(), teams)) }
        _state.update { it.copy(room = null) }
        runHost(infos, remotes, terrain, teams)
    }

    /** Room: host taps a player to move them; a guest moves themselves. */
    fun switchTeam(index: Int) {
        roomEvents?.let { it.trySend(RoomEv.HostSwitch(index)); return }
        val c = roomConn ?: return
        viewModelScope.launch { runCatching { c.send(Protocol.SWITCH_TEAM) } }
    }

    fun startRoom() { roomEvents?.trySend(RoomEv.Start) }

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
        roomConn = c
        // Rooms send lobby updates until the host taps Start; other formats send the setup right away.
        val setup = c.incoming.map { Protocol.parse(it) }.transform { m ->
            when (m) {
                is Protocol.Lobby -> _state.update {
                    it.copy(mode = BattleScreenMode.ROOM, room = RoomUi(m.ffa, m.players, m.you, host = false, canStart = false))
                }
                is Protocol.Setup -> emit(m)
                else -> Unit
            }
        }.firstOrNull() ?: error("Host closed the room")
        roomConn = null
        _state.update { it.copy(room = null) }
        runGuest(setup.infos, setup.you, setup.humans, c, setup.terrain, setup.teams)
    }

    val hasOnline get() = online != null

    /** Ranked 1v1 against a random player online. The first one waiting hosts the battle. */
    fun findOnline(): Unit = start {
        val service = online ?: error("Online play isn't available")
        again = ::findOnline
        _state.value = BattleUiState(mode = BattleScreenMode.MATCHMAKING, format = BattleFormat.SOLO)
        val me = myInfo()
        val match = service.findMatch(me)
        val c = match.conn
        conns += c
        _state.update { it.copy(mode = BattleScreenMode.CONNECTING, message = "${match.opponent} (online)") }
        val rival = handshake(c, me)
        if (match.host) {
            val infos = listOf(me, rival)
            val terrain = nextTerrain()
            c.send(Protocol.setup(1, infos, terrain, setOf(0, 1)))
            runHost(infos, mapOf(1 to c), terrain, ranked = true)
        } else {
            val setup = c.incoming.map { Protocol.parse(it) }.filterIsInstance<Protocol.Setup>().firstOrNull()
                ?: error("${match.opponent} left")
            runGuest(setup.infos, setup.you, setup.humans, c, setup.terrain, setup.teams, ranked = true)
        }
    }

    fun showLeaderboard() {
        stop()
        _state.value = BattleUiState(mode = BattleScreenMode.LEADERBOARD, format = format)
        val service = online ?: return
        job = viewModelScope.launch {
            val board = runCatching { service.leaderboard() }
            _state.update {
                if (it.mode != BattleScreenMode.LEADERBOARD) it
                else it.copy(board = board.getOrNull(), boardError = board.exceptionOrNull()?.let { e -> "Couldn't load: ${e.message ?: "no connection"}" })
            }
        }
    }

    fun use(move: Move) = sendMove(move)
    fun evade() = sendMove(null)

    fun leave() {
        stop()
        _state.value = BattleUiState(format = format)
    }

    private fun stop() {
        job?.cancel(); job = null
        roomEvents = null; roomConn = null
        closeLinks()
        sendMove = {}
    }

    private fun closeLinks() {
        conns.forEach { it.close() }
        conns.clear()
    }

    override fun onCleared() = stop()

    /**
     * Swaps pet info. The link can drop or stall here (the other phone backed out, the host started
     * without it, a full room): that's a plain failure, never a bare "no element" or a silent cancel.
     */
    private suspend fun handshake(c: BattleConnection, me: FighterInfo): FighterInfo {
        c.send(Protocol.hello(me))
        val hello = withTimeoutOrNull(10_000) {
            c.incoming.map { Protocol.parse(it) }.filterIsInstance<Protocol.Hello>().firstOrNull()
                ?: throw LinkLost("the other phone disconnected (room full or already started?)")
        } ?: throw LinkLost("the other phone didn't answer")
        if (hello.version != Protocol.VERSION) throw VersionMismatch()
        return hello.info
    }

    /** Runs the simulation locally (practice, or host of a linked match). [remotes]: slot -> that player's link. */
    private suspend fun runHost(
        infos: List<FighterInfo>, remotes: Map<Int, BattleConnection>, terrain: Terrain,
        teams: List<Int> = BattleEngine.defaultTeams(infos.size),
        ranked: Boolean = false,
    ) = coroutineScope {
        val engine = BattleEngine(infos, terrain = terrain, teamIds = teams)
        val rng = Random.Default
        val humans = setOf(0) + remotes.keys
        val bots = infos.indices.filter { it !in humans }.toMutableSet()
        // CPU-only enemies (no friend on the other team) give practice XP
        val practice = humans.all { engine.teamOf(it) == engine.teamOf(0) }
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
            fight = FightState(0, infos, humans, engine.snapshot(0), practice = practice, terrain = terrain, teams = teams),
            format = format,
        )
        val clock = TimeSource.Monotonic.markNow()
        // One writer per guest so a slow link never stalls the sim; stale states are dropped.
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
        if (ranked) showRating()
    }

    private suspend fun runGuest(
        infos: List<FighterInfo>, you: Int, humans: Set<Int>, c: BattleConnection, terrain: Terrain, teams: List<Int>,
        ranked: Boolean = false,
    ) {
        val practice = humans.all { teams[it] == teams[0] } // every player on one team vs CPUs
        sendMove = { m -> viewModelScope.launch { runCatching { c.send(if (m == null) Protocol.EVADE else Protocol.command(m)) } } }
        var last: BattleSnapshot? = null
        c.incoming.map { Protocol.parse(it) }.flowOn(Dispatchers.Default).collect { msg ->
            when (msg) {
                is Protocol.State -> {
                    if (_state.value.mode != BattleScreenMode.FIGHT) {
                        _state.value = BattleUiState(mode = BattleScreenMode.FIGHT, format = format,
                            fight = FightState(you, infos, humans, msg.snap, practice = practice, terrain = terrain, teams = teams))
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
        if (last?.ended != true) endEarly(if (ranked) "Opponent left the battle" else "Host left the battle")
        if (ranked) showRating()
    }

    /** Adds your new rating and place to the result line (the server scores the match as it ends). */
    private suspend fun showRating() {
        val service = online ?: return
        delay(500)
        val me = runCatching { service.leaderboard().me }.getOrNull() ?: return
        val place = if (me.rank > 0) "  #${me.rank}" else ""
        _state.update { s -> s.copy(fight = s.fight?.copy(result = s.fight.result?.let { "$it\nRating ${me.rating}$place" })) }
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
        val fight = _state.value.fight ?: return
        val fighters = fight.fighters
        val teamOf = { i: Int -> fight.teams[i] }
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
        var detail = "+$xp XP${if (practice) " (CPU)" else ""}"
        if (bossFight && outcome == BattleOutcome.WIN) {
            detail += rewards?.claimBoss()?.let { "\nBoss defeated! ${it.label} egg unlocked" } ?: ""
        }
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

private class VersionMismatch : Exception("A friend has a different PetPal version. Both phones need the same update.")
private class LinkLost(msg: String) : Exception(msg)
