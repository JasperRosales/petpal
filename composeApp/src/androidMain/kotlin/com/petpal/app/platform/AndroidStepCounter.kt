package com.petpal.app.platform

import android.content.Context
import android.hardware.*
import com.petpal.app.domain.platform.StepCounter
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.*

class AndroidStepCounter(context: Context) : StepCounter {
    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val sensor: Sensor? = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER)

    override val isAvailable: Boolean get() = sensor != null

    override fun stepsSinceBoot(): Flow<Long> = callbackFlow {
        val s = sensor ?: run { close(); return@callbackFlow }
        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                trySend(event.values[0].toLong())
            }
            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
        }
        sensorManager.registerListener(listener, s, SensorManager.SENSOR_DELAY_NORMAL)
        awaitClose { sensorManager.unregisterListener(listener) }
    }
}
