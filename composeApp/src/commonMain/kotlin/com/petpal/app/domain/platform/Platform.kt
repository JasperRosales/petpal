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

/** The user's "track my steps" switch (also flipped by the notification's "Don't track now"). */
interface StepTracking {
    val enabled: kotlinx.coroutines.flow.StateFlow<Boolean>
    fun setEnabled(on: Boolean)
    /** Set while paused, cleared by the first reading after resuming: steps walked while paused don't count. */
    var resyncPending: Boolean
}
