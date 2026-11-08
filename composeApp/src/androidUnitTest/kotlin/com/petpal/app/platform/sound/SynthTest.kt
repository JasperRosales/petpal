package com.petpal.app.platform.sound

import com.petpal.app.domain.platform.Sfx
import kotlin.test.Test
import kotlin.test.assertTrue

class SynthTest {
    @Test
    fun everySoundRendersAudibleCleanPcm() {
        Sfx.entries.forEach { sfx ->
            val s = Synth.render(sfx)
            assertTrue(s.isNotEmpty() && s.size < Synth.RATE, "$sfx length")
            assertTrue(s.none { it.isNaN() } && s.all { it in -1f..1f }, "$sfx range")
            assertTrue(s.maxOf { kotlin.math.abs(it) } > 0.05f, "$sfx is silent")
            val wav = Synth.wav(s)
            assertTrue(wav.size == 44 + s.size * 2 && String(wav, 0, 4) == "RIFF", "$sfx wav")
        }
    }
}
