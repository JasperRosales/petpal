package com.petpal.app

import com.petpal.app.domain.model.Species
import com.petpal.app.domain.platform.Leaderboard
import kotlin.test.*

class LeaderboardTest {
    @Test fun parse() {
        val b = Leaderboard.parse("top\t1\tAce\tFOX\tKID\t1016\t1\t0\t0\nme\t0\tBo\tNOPE\tKID\t1000\t0\t0\t0\n")
        assertEquals(1, b.top.size)
        assertEquals(Species.FOX, b.top[0].species)
        assertEquals(0, b.me!!.rank)
        assertNull(b.me!!.species)
    }
}
