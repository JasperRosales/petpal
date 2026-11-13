package com.petpal.app.ui.battle

import com.petpal.app.domain.battle.Move
import com.petpal.app.domain.battle.MoveCategory
import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class UniqueDeliveryTest {
    private val uniques = Move.entries.filter { it.category == MoveCategory.UNIQUE }

    @Test fun everyUniqueHasADelivery() = uniques.forEach { assertNotNull(it.delivery, it.label) }

    @Test fun deliveriesAreSpreadOut() {
        val counts = uniques.groupingBy { it.delivery }.eachCount()
        println("DELIVERIES " + counts.entries.sortedByDescending { it.value }.joinToString { "${it.key}=${it.value}" })
        assertTrue(counts.size >= 40, "only ${counts.size} deliveries in use")
        assertTrue(counts.values.max() <= uniques.size / 12, "one delivery covers too many uniques: $counts")
    }
}
