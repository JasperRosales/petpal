package com.petpal.app.ui.battle

import com.petpal.app.domain.battle.*
import com.petpal.app.domain.model.*
import kotlin.test.*

class MoveTextTest {
    @Test fun everyMoveSaysWhatItDoes() {
        val me = FighterInfo("Pip", Species.PUP, Stage.BABY, 10)
        Move.entries.forEach { m -> assertTrue(whatItDoes(me, m).length > 5, m.name) }
        assertTrue(whatItDoes(me, Move.HEAL).startsWith("Heals you"))
    }
}
