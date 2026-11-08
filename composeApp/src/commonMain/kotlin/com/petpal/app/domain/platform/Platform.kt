package com.petpal.app.domain.platform

import kotlinx.coroutines.flow.Flow

interface StepCounter {
    val isAvailable: Boolean
    /** Total steps reported by the sensor since device boot. */
    fun stepsSinceBoot(): Flow<Long>
}

fun interface TimeProvider {
    fun nowMillis(): Long
}
