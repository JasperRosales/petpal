package com.petpal.app

import com.petpal.app.domain.battle.Move
import com.petpal.app.domain.battle.MoveCategory
import com.petpal.app.ui.battle.signature
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class UniqueFxTest {
    @Test fun everyUniqueHasItsOwnSetPiece() {
        val uniques = Move.entries.filter { it.category == MoveCategory.UNIQUE }
        val sigs = uniques.map { assertNotNull(it.signature, "$it has no set piece") }
        sigs.forEach { assert(it.main != it.accent) }
        val looks = sigs.map { Triple(it.main, it.accent, it.parts) }
        assertEquals(looks.size, looks.toSet().size, "two uniques share a set piece")
    }

    @Test fun normalMovesHaveNoSetPiece() {
        assertNull(Move.TACKLE.signature)
    }
}
