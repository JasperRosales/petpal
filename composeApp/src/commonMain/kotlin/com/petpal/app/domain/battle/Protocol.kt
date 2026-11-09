package com.petpal.app.domain.battle

import com.petpal.app.domain.model.Species
import com.petpal.app.domain.model.Stage
import kotlin.math.roundToInt

/** Line-based wire format shared by the Bluetooth and Wi-Fi links. */
object Protocol {
    const val VERSION = 21

    sealed interface Msg
    data class Hello(val version: Int, val info: FighterInfo) : Msg
    data class Command(val move: Move) : Msg
    data object EvadeCmd : Msg
    data class State(val snap: BattleSnapshot) : Msg
    data class Event(val text: String) : Msg
    /** Host -> guest before a match: every fighter in slot order, and which slot the guest controls. */
    data class Setup(
        val you: Int, val infos: List<FighterInfo>, val terrain: Terrain = Terrain.ARENA,
        /** Slots played by people (host is 0); the rest are CPUs. */
        val humans: Set<Int> = setOf(0, you),
        /** Team id per slot (free-for-all: each slot its own). */
        val teams: List<Int> = BattleEngine.defaultTeams(infos.size),
    ) : Msg
    /** Host -> guest while waiting in a room: everyone in it, and which entry is the guest. */
    data class Lobby(val you: Int, val ffa: Boolean, val players: List<RoomEntry>) : Msg
    /** Guest -> host: put me on the other team. */
    data object SwitchTeam : Msg

    data class RoomEntry(val name: String, val level: Int, val team: Int)

    fun hello(info: FighterInfo) = "H|$VERSION|${clean(info.name)}|${info.species.name}|${info.stage.name}|${info.level}|" +
        info.moves.joinToString(",") { it.name }
    fun command(m: Move) = "C|${m.ordinal}"
    const val EVADE = "E"
    const val SWITCH_TEAM = "W"
    fun lobby(you: Int, ffa: Boolean, players: List<RoomEntry>) = "L|$you|${if (ffa) 1 else 0}|" +
        players.joinToString("#") { "${clean(it.name).replace(Regex("[#~]"), " ")}~${it.level}~${it.team}" }
    fun event(text: String) = "V|${clean(text)}"
    fun setup(
        you: Int, infos: List<FighterInfo>, terrain: Terrain = Terrain.ARENA, humans: Set<Int> = setOf(0, you),
        teams: List<Int> = BattleEngine.defaultTeams(infos.size),
    ) =
        "T|$you|" + infos.joinToString("#") {
            listOf(clean(it.name).replace(Regex("[#~]"), " "), it.species.name, it.stage.name, it.level,
                it.moves.joinToString(",") { m -> m.name }).joinToString("~")
        } + "|${terrain.name}|${humans.sorted().joinToString(",")}|${teams.joinToString(",")}"

    fun state(s: BattleSnapshot) = buildString {
        append("S|${s.timeLeftMs},${if (s.ended) 1 else 0},${s.winner},${s.smokes.size},${s.blasts.size}")
        (s.smokes + s.blasts).forEach { append(",${(it.x * 1000).roundToInt()},${(it.y * 1000).roundToInt()},${it.ageMs}") }
        s.traps.forEach { append(",${(it.x * 1000).roundToInt()},${(it.y * 1000).roundToInt()},${it.ageMs},${it.element.ordinal}") }
        s.fighters.forEach { f ->
            append('|')
            append((f.x * 1000).roundToInt()).append(',').append((f.y * 1000).roundToInt()).append(',')
            append(f.hp).append(',').append(f.maxHp).append(',')
            append(f.action?.ordinal ?: -1).append(',').append(f.actionAgeMs.coerceAtMost(99_999)).append(',')
            append(f.status).append(',').append(f.hurtAgeMs.coerceAtMost(99_999)).append(',')
            append(f.facing).append(',').append(f.energy).append(',').append(f.uniqueLeft).append(',')
            append((f.aimX * 1000).roundToInt()).append(',').append((f.aimY * 1000).roundToInt()).append(',')
            append((f.originX * 1000).roundToInt()).append(',').append((f.originY * 1000).roundToInt()).append(',')
            append(f.projectileAgeMs.coerceIn(-1, 99_999)).append(',').append(f.evadeCdMs.coerceIn(0, 99_999))
            f.cooldowns.forEach { append(',').append(it) }
        }
    }

    fun parse(line: String): Msg? = runCatching {
        val p = line.split('|')
        when (p[0]) {
            "H" -> Hello(
                p[1].toInt(),
                FighterInfo(
                    p[2], Species.valueOf(p[3]), Stage.valueOf(p[4]),
                    (p.getOrNull(5)?.toIntOrNull() ?: 1).coerceIn(1, 100),
                    p.getOrNull(6).orEmpty().split(',').mapNotNull { n -> Move.entries.firstOrNull { it.name == n } },
                ),
            )
            "C" -> Command(Move.entries[p[1].toInt()])
            "E" -> EvadeCmd
            "W" -> SwitchTeam
            "L" -> Lobby(p[1].toInt(), p[2] == "1", p[3].split('#').filter { it.isNotEmpty() }.map { e ->
                val v = e.split('~')
                RoomEntry(v[0], v[1].toInt(), v[2].toInt())
            })
            "V" -> Event(p[1])
            "T" -> {
                val you = p[1].toInt()
                val infos = p[2].split('#').map { fs ->
                    val v = fs.split('~')
                    FighterInfo(v[0], Species.valueOf(v[1]), Stage.valueOf(v[2]), v[3].toInt().coerceIn(1, 100),
                        v.getOrNull(4).orEmpty().split(',').mapNotNull { n -> Move.entries.firstOrNull { it.name == n } })
                }
                Setup(you, infos, Terrain.entries.firstOrNull { it.name == p.getOrNull(3) } ?: Terrain.ARENA,
                    p.getOrNull(4)?.split(',')?.mapNotNull { it.toIntOrNull() }?.toSet()?.plus(setOf(0, you)) ?: setOf(0, you),
                    p.getOrNull(5)?.split(',')?.mapNotNull { it.toIntOrNull() }?.takeIf { it.size == infos.size }
                        ?: BattleEngine.defaultTeams(infos.size))
            }
            "S" -> {
                val h = p[1].split(',')
                val nSmokes = h[3].toInt()
                val nBlasts = h[4].toInt()
                val extra = h.drop(5).map { it.toLong() }
                val clouds = extra.take((nSmokes + nBlasts) * 3).chunked(3).filter { it.size == 3 }
                    .map { (x, y, age) -> SmokeSnap(x / 1000f, y / 1000f, age) }
                State(
                    BattleSnapshot(
                        timeLeftMs = h[0].toLong(),
                        ended = h[1] == "1",
                        winner = h[2].toInt(),
                        smokes = clouds.take(nSmokes),
                        blasts = clouds.drop(nSmokes),
                        traps = extra.drop((nSmokes + nBlasts) * 3).chunked(4).filter { it.size == 4 }
                            .map { (x, y, age, el) -> TrapSnap(x / 1000f, y / 1000f, age, Element.entries[el.toInt()]) },
                        fighters = p.drop(2).map { fs ->
                            val v = fs.split(',').map { it.toLong() }
                            FighterSnap(
                                x = v[0] / 1000f, y = v[1] / 1000f, hp = v[2].toInt(), maxHp = v[3].toInt(),
                                action = Move.entries.getOrNull(v[4].toInt()), actionAgeMs = v[5],
                                status = v[6].toInt(), hurtAgeMs = v[7], facing = v[8].toInt(),
                                energy = v[9].toInt(), uniqueLeft = v[10].toInt(),
                                aimX = v[11] / 1000f, aimY = v[12] / 1000f,
                                originX = v[13] / 1000f, originY = v[14] / 1000f,
                                projectileAgeMs = v[15], evadeCdMs = v[16],
                                cooldowns = v.drop(17),
                            )
                        },
                    )
                )
            }
            else -> null
        }
    }.getOrNull()

    private fun clean(s: String) = s.replace(Regex("[|\\n\\r]"), " ").take(60)
}
