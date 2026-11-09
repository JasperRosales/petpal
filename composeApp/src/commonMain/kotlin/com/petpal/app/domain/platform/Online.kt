package com.petpal.app.domain.platform

import com.petpal.app.domain.battle.FighterInfo
import com.petpal.app.domain.model.Species
import com.petpal.app.domain.model.Stage

/** One leaderboard row. [rank] 0 = no ranked matches yet. */
data class RankEntry(
    val rank: Int,
    val name: String,
    val species: Species?,
    val stage: Stage?,
    val rating: Int,
    val wins: Int,
    val losses: Int,
    val draws: Int,
)

data class Leaderboard(val top: List<RankEntry>, val me: RankEntry?) {
    companion object {
        /** Server format: "top|me" + tab-separated [RankEntry] fields, one per line. */
        fun parse(text: String): Leaderboard {
            val rows = text.lines().mapNotNull { line ->
                val f = line.split('\t')
                if (f.size < 9) return@mapNotNull null
                f[0] to RankEntry(
                    rank = f[1].toIntOrNull() ?: 0, name = f[2],
                    species = Species.entries.firstOrNull { it.name == f[3] },
                    stage = Stage.entries.firstOrNull { it.name == f[4] },
                    rating = f[5].toIntOrNull() ?: 0, wins = f[6].toIntOrNull() ?: 0,
                    losses = f[7].toIntOrNull() ?: 0, draws = f[8].toIntOrNull() ?: 0,
                )
            }
            return Leaderboard(rows.filter { it.first == "top" }.map { it.second }, rows.firstOrNull { it.first == "me" }?.second)
        }
    }
}

/** A random opponent found online. The [host] side runs the battle, like a local host. */
class OnlineMatch(val host: Boolean, val opponent: String, val conn: BattleConnection)

/** Online matchmaking and the ranked leaderboard (PetPal server on Render). */
interface OnlineService {
    /** Queues until someone else is looking; cancel to leave the queue. */
    suspend fun findMatch(me: FighterInfo): OnlineMatch
    suspend fun leaderboard(): Leaderboard
}
