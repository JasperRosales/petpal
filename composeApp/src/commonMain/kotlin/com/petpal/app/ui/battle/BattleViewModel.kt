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
 * SOLO = 1v1.
 * TEAMS / FFA: a room of up to [MAX_PLAYERS]; players pick teams (or it's everyone for themselves) and the host starts.
 * RAID: a co-op room of up to [Raid.MAX_PLAYERS]; the host rolls a ten-floor dungeon.
 */
enum class BattleFormat(val label: String, val guests: Int = 1) {
    SOLO("1v1"),
    TEAMS("Team battle", guests = MAX_PLAYERS - 1), FFA("Free-for-all", guests = MAX_PLAYERS - 1),
    RAID("Raid", guests = Raid.MAX_PLAYERS - 1);

    val isRoom get() = this == TEAMS || this == FFA || this == RAID
    val isRaid get() = this == RAID
}

/** Waiting room before a team, free-for-all or raid run. [you] = your entry in [players]. */
data class RoomUi(
    val ffa: Boolean,
    val players: List<Protocol.RoomEntry>,
    val you: Int,
    val host: Boolean,
    val canStart: Boolean,
    val raid: Boolean = false,
)

/** Raid progress shown in the fight HUD. */
data class RaidUi(val floor: Int, val floors: Int = Raid.FLOORS)

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
    /** Non-null during a raid; [RaidUi.floor] is 1-based. */
    val raid: RaidUi? = null,
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

    /** 1v1 vs a CPU, 3v3 with CPU partners, or a 4-pet free-for-all. */
    fun practice(): Unit = start {
        again = ::practice
        val me = myInfo()
        if (format.isRaid) {
            again = null
            return@start runRaidHost(listOf(me), remotes = emptyMap(), terrain = nextTerrain())
        }
        val ally = { CpuOpponent.random(me, prefix = "CPU") }
        val foe = { CpuOpponent.random(me) }
        val (infos, teams) = when (format) {
            BattleFormat.SOLO -> listOf(me, foe()).let { it to BattleEngine.defaultTeams(2) }
            BattleFormat.FFA -> listOf(me, foe(), foe(), foe()).let { it to BattleEngine.freeForAll(4) }
            BattleFormat.TEAMS -> listOf(me, ally(), ally(), foe(), foe(), foe()).let { it to BattleEngine.defaultTeams(6) }
            BattleFormat.RAID -> error("handled above")
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
        val fmt = format
        again = if (fmt.isRaid) null else fun() { host(kind) }
        if (fmt.isRoom) {
            _state.value = BattleUiState(mode = BattleScreenMode.ROOM, link = kind, format = fmt)
            return@start hostRoom(kind, ffa = fmt == BattleFormat.FFA, raid = fmt.isRaid)
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
        val (infos, slots) = when (fmt) {
            BattleFormat.SOLO -> listOf(me, friend) to listOf(1)
            BattleFormat.TEAMS, BattleFormat.FFA, BattleFormat.RAID -> error("rooms are hosted by hostRoom")
        }
        val remotes = slots.zip(joined.map { it.first }).toMap()
        val terrain = nextTerrain()
        val humans = setOf(0) + remotes.keys
        remotes.forEach { (slot, c) -> c.send(Protocol.setup(slot, infos, terrain, humans)) }
        runHost(infos, remotes, terrain)
    }

    /**
     * Open room: friends join (up to [MAX_PLAYERS], or [Raid.MAX_PLAYERS] for a raid), switch teams,
     * and the host taps Start. The host is always slot 0; in team battles the host's team spawns on the left.
     */
    private suspend fun hostRoom(kind: LinkKind, ffa: Boolean, raid: Boolean = false) = coroutineScope {
        val me = myInfo()
        val maxMembers = if (raid) Raid.MAX_PLAYERS else MAX_PLAYERS
        class Member(val c: BattleConnection?, val info: FighterInfo, var team: Int)
        val members = mutableListOf(Member(null, me, 0))
        val events = Channel<RoomEv>(Channel.UNLIMITED)
        roomEvents = events
        fun canStart() = raid || members.size >= 2 && when {
            ffa -> true
            else -> members.map { it.team }.toSet().size == 2
        }
        suspend fun publish() {
            val entries = members.map { Protocol.RoomEntry(it.info.name, it.info.level, if (ffa || raid) 0 else it.team) }
            _state.update {
                it.copy(mode = BattleScreenMode.ROOM, room = RoomUi(ffa, entries, 0, host = true, canStart = canStart(), raid = raid))
            }
            members.forEachIndexed { i, m -> m.c?.let { c -> runCatching { c.send(Protocol.lobby(i, ffa, entries, raid)) } } }
        }
        val accept = launch {
            links[kind].hostMany(me.name, maxMembers - 1).collect { c ->
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
                is RoomEv.Joined -> if (members.size < maxMembers) {
                    // New players balance the teams (ties go to the other side, so 1v1 is the default).
                    val team = when {
                        raid || ffa -> 0
                        members.count { it.team == 1 } <= members.count { it.team == 0 } -> 1
                        else -> 0
                    }
                    members += Member(ev.c, ev.info, team)
                } else ev.c.close()
                is RoomEv.Left -> { members.removeAll { it.c === ev.c }; ev.c.close(); conns.remove(ev.c) }
                is RoomEv.Switch -> if (!ffa && !raid) members.firstOrNull { it.c === ev.c }?.let { it.team = 1 - it.team }
                is RoomEv.HostSwitch -> if (!ffa && !raid) members.getOrNull(ev.index)?.let { it.team = 1 - it.team }
                RoomEv.Start -> if (canStart()) break
            }
            publish()
        }
        roomEvents = null
        accept.cancelAndJoin() // stop accepting and stop the room readers before the match reads the links
        val inRoom = members.mapNotNull { it.c }.toSet()
        conns.filter { it !in inRoom }.forEach { it.close() }
        conns.retainAll(inRoom)
        val order = if (ffa || raid) members.toList() else members.sortedBy { if (it.team == members[0].team) 0 else 1 }
        val infos = order.map { it.info }
        val teams = when {
            raid -> List(order.size) { 0 }
            ffa -> BattleEngine.freeForAll(order.size)
            else -> order.map { it.team }
        }
        val remotes = order.withIndex().filter { it.value.c != null }.associate { it.index to it.value.c!! }
        val terrain = nextTerrain()
        _state.update { it.copy(room = null) }
        if (raid) {
            // runRaidHost sends the first floor setup itself, with skeletons on the enemy team.
            runRaidHost(infos, remotes, terrain)
        } else {
            remotes.forEach { (slot, c) -> c.send(Protocol.setup(slot, infos, terrain, order.indices.toSet(), teams)) }
            runHost(infos, remotes, terrain, teams)
        }
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
                    it.copy(mode = BattleScreenMode.ROOM,
                        room = RoomUi(m.ffa, m.players, m.you, host = false, canStart = false, raid = m.raid))
                }
                is Protocol.Setup -> emit(m)
                else -> Unit
            }
        }.firstOrNull() ?: error("Host closed the room")
        roomConn = null
        _state.update { it.copy(room = null) }
        runGuest(setup, c)
    }

    val hasOnline get() = online != null

    /** Ranked 1v1 against a random player online. The first one waiting hosts the battle. */
    fun findOnline(): Unit = start {
        val service = online ?: error("Online play isn't available")
        again = ::findOnline
        _state.value = BattleUiState(mode = BattleScreenMode.MATCHMAKING, format = BattleFormat.SOLO)
        val me = myInfo()
        // Nobody else queueing: a Lv 50 bot with twice the HP steps in (unranked, CPU rewards).
        val match = withTimeoutOrNull(ONLINE_WAIT_MS) { service.findMatch(me) }
        if (match == null) {
            _state.update { it.copy(mode = BattleScreenMode.CONNECTING, message = "a Lv ${CpuOpponent.ONLINE_BOT_LEVEL} bot (no players online)") }
            delay(1200)
            return@start runHost(listOf(me, CpuOpponent.onlineBot()), remotes = emptyMap(), terrain = nextTerrain())
        }
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
            runGuest(setup, c, ranked = true)
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

    /**
     * Host side of a raid: build a fresh engine per floor, carry surviving HP forward, and let a
     * CPU take over anyone who drops. Guests only receive setup/state/event messages.
     */
    private suspend fun runRaidHost(
        raiders: List<FighterInfo>, remotes: Map<Int, BattleConnection>, terrain: Terrain,
    ) = coroutineScope {
        val rng = Random.Default
        val plan = Raid.plan(raiders.size, rng)
        val humans = setOf(0) + remotes.keys
        val bots = mutableSetOf<Int>()
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
        var carried = IntArray(raiders.size) { -1 }
        var index = 0
        var floorsCleared = 0
        var cleared = false
        raid@ while (index < plan.floors.size) {
            val floor = plan.floors[index]
            val infos = raiders.mapIndexed { i, r -> r.copy(startHp = carried[i]) } + floor.enemies
            val teams = List(infos.size) { if (it < raiders.size) 0 else 1 }
            val engine = BattleEngine(infos, terrain = terrain, teamIds = teams, timeLimitMs = floor.timeLimitMs)
            // A fresh setup per floor also tells every phone which floor it is.
            live.forEach { (slot, c) ->
                runCatching {
                    c.send(Protocol.setup(slot, infos, terrain, humans, teams,
                        raid = true, floor = index + 1, floors = plan.floors.size))
                }
            }
            _state.value = BattleUiState(mode = BattleScreenMode.FIGHT, format = format,
                fight = FightState(0, infos, humans, engine.snapshot(0), terrain = terrain, teams = teams,
                    raid = RaidUi(index + 1, plan.floors.size)))
            val clock = TimeSource.Monotonic.markNow()
            val outboxes = live.mapValues { (_, c) -> Outbox(c).also { it.start(this) } }
            var nextTick = 0L
            withContext(Dispatchers.Default) {
                while (isActive) {
                    val now = clock.elapsedNow().inWholeMilliseconds
                    val events = mutableListOf<String>()
                    while (true) {
                        val slot = left.tryReceive().getOrNull() ?: break
                        if (!live.containsKey(slot)) continue
                        live.remove(slot)
                        bots += slot
                        events += "${infos[slot].name} left. A CPU takes over"
                    }
                    while (true) {
                        val (i, m) = commands.tryReceive().getOrNull() ?: break
                        if (i in bots) continue
                        (if (m == null) engine.evade(i, now) else engine.command(i, m, now))?.let(events::add)
                    }
                    for (b in bots) {
                        if (b >= raiders.size || engine.isDown(b)) continue
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
                    nextTick += TICK_MS
                    val wait = nextTick - clock.elapsedNow().inWholeMilliseconds
                    if (wait > 0) delay(wait) else { nextTick = clock.elapsedNow().inWholeMilliseconds; yield() }
                }
            }
            outboxes.values.forEach { it.stop() }
            val end = engine.snapshot(clock.elapsedNow().inWholeMilliseconds)
            val enemiesDown = (raiders.size until infos.size).all { end.fighters[it].hp <= 0 }
            val raidersDown = (0 until raiders.size).all { end.fighters[it].hp <= 0 }
            if (!enemiesDown || raidersDown) break@raid // wipe or floor timeout
            floorsCleared = index + 1
            if (index == plan.floors.size - 1) { cleared = true; break@raid }
            carried = Raid.carryHp(end, raiders.size)
            delay(Raid.BANNER_MS) // the ended snapshot is still on screen as a floor banner
            index++
        }
        readers.forEach { it.cancel() }
        sendMove = {}
        val result = Protocol.raidEnd(cleared, floorsCleared)
        live.keys.forEach { slot -> remotes[slot]?.let { runCatching { it.send(result) } } }
        finishRaid(cleared, floorsCleared)
        if (remotes.isNotEmpty()) delay(1500) // let the final message reach the guests
    }

    private suspend fun runGuest(setup: Protocol.Setup, c: BattleConnection, ranked: Boolean = false) {
        var infos = setup.infos
        var teams = setup.teams
        var terrain = setup.terrain
        var raidFloor = setup.floor
        var raidFloors = setup.floors
        val you = setup.you
        val humans = setup.humans
        var practice = humans.all { teams[it] == teams[0] } // every player on one team vs CPUs
        sendMove = { m -> viewModelScope.launch { runCatching { c.send(if (m == null) Protocol.EVADE else Protocol.command(m)) } } }
        var last: BattleSnapshot? = null
        var done = false
        fun fightState(snap: BattleSnapshot) = FightState(you, infos, humans, snap, practice = practice,
            terrain = terrain, teams = teams, raid = if (setup.raid) RaidUi(raidFloor, raidFloors) else null)
        c.incoming.map { Protocol.parse(it) }.flowOn(Dispatchers.Default).collect { msg ->
            when (msg) {
                is Protocol.Setup -> if (msg.raid) {
                    infos = msg.infos
                    teams = msg.teams
                    terrain = msg.terrain
                    raidFloor = msg.floor
                    raidFloors = msg.floors
                    practice = msg.humans.all { msg.teams[it] == msg.teams[0] }
                    // Keep the old snapshot and floor banner until the new floor's first state arrives,
                    // so the two never disagree about how many fighters are on the field.
                }
                is Protocol.State -> {
                    val cur = _state.value.fight
                    if (_state.value.mode != BattleScreenMode.FIGHT || cur == null || cur.fighters != infos ||
                        cur.teams != teams || cur.terrain != terrain || cur.raid?.floor != (if (setup.raid) raidFloor else null)
                    ) {
                        _state.value = BattleUiState(mode = BattleScreenMode.FIGHT, format = format, fight = fightState(msg.snap))
                    }
                    last = msg.snap
                    push(msg.snap, emptyList())
                    if (msg.snap.ended && !setup.raid) {
                        finish(you, msg.snap, practice)
                        sendMove = {}
                        done = true
                        return@collect
                    }
                }
                is Protocol.RaidEnd -> {
                    done = true
                    sendMove = {}
                    finishRaid(msg.cleared, msg.floors)
                    return@collect
                }
                is Protocol.Event -> push(null, listOf(msg.text))
                else -> Unit
            }
        }
        if (!done && last?.ended != true) endEarly(if (ranked) "Opponent left the battle" else "Host left the battle")
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

    /** Raid result: XP for every floor cleared, and one daily pet unlock when the boss goes down. */
    private suspend fun finishRaid(cleared: Boolean, floors: Int) {
        if (_state.value.fight?.result != null) return
        val xp = petRepository.recordRaid(cleared, floors)
        var detail = "+$xp XP"
        if (cleared) {
            val pet = rewards?.claimRaid()
            detail += if (pet != null) "\n${pet.label} egg unlocked!" else
                "\nNo new pet available (already claimed today or collection complete)."
        } else {
            val f = _state.value.fight
            val raiders = f?.teams?.count { it == f.teams.getOrNull(0) } ?: 0
            val why = f?.let { Raid.failReason(it.snap, raiders) }?.let { "$it on" } ?: "The raid ended on"
            detail += "\n$why floor ${floors + 1}: ${Raid.title(floors + 1)}."
        }
        val title = if (cleared) "Raid cleared!" else "Raid failed"
        _state.update { s -> s.copy(fight = s.fight?.copy(result = "$title  $detail")) }
    }
}

private const val TICK_MS = 50L
/** How long online matchmaking waits for a real player before a bot steps in. */
private const val ONLINE_WAIT_MS = 25_000L

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
