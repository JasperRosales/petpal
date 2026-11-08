package com.petpal.app.domain.battle

import com.petpal.app.domain.model.Species
import com.petpal.app.domain.model.Stage
import kotlin.math.roundToInt

/** Line-based wire format shared by the Bluetooth and Wi-Fi links. */
object Protocol {
    const val VERSION = 8

    sealed interface Msg
    data class Hello(val version: Int, val info: FighterInfo) : Msg
    data class Command(val move: Move) : Msg
    data object EvadeCmd : Msg
    data class State(val snap: BattleSnapshot) : Msg
    data class Event(val text: String) : Msg

    fun hello(info: FighterInfo) = "H|$VERSION|${clean(info.name)}|${info.species.name}|${info.stage.name}|${info.level}|" +
        info.moves.joinToString(",") { it.name }
    fun command(m: Move) = "C|${m.ordinal}"
    const val EVADE = "E"
    fun event(text: String) = "V|${clean(text)}"

    fun state(s: BattleSnapshot) = buildString {
        append("S|${s.timeLeftMs},${if (s.ended) 1 else 0},${s.winner}")
        s.fighters.forEach { f ->
            append('|')
            append(
                listOf(
                    (f.x * 1000).roundToInt(), (f.y * 1000).roundToInt(), f.hp, f.maxHp,
                    f.action?.ordinal ?: -1, f.actionAgeMs.coerceAtMost(99_999), f.status,
                    f.hurtAgeMs.coerceAtMost(99_999), f.facing, f.energy, f.uniqueLeft,
                    (f.aimX * 1000).roundToInt(), (f.aimY * 1000).roundToInt(),
                    (f.originX * 1000).roundToInt(), (f.originY * 1000).roundToInt(),
                    f.projectileAgeMs.coerceIn(-1, 99_999), f.evadeCdMs.coerceIn(0, 99_999),
                ).joinToString(",")
            )
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
                    (p.getOrNull(5)?.toIntOrNull() ?: 1).coerceIn(1, 99),
                    p.getOrNull(6).orEmpty().split(',').mapNotNull { n -> Move.entries.firstOrNull { it.name == n } },
                ),
            )
            "C" -> Command(Move.entries[p[1].toInt()])
            "E" -> EvadeCmd
            "V" -> Event(p[1])
            "S" -> {
                val h = p[1].split(',')
                State(
                    BattleSnapshot(
                        timeLeftMs = h[0].toLong(),
                        ended = h[1] == "1",
                        winner = h[2].toInt(),
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
