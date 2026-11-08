package com.petpal.app.platform.sound

import com.petpal.app.domain.platform.Sfx
import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.sin
import kotlin.random.Random

/** Tiny procedural synth: oscillators, noise and envelopes rendered to 16-bit mono PCM. */
internal object Synth {
    const val RATE = 22_050

    private class Buf(seconds: Float) {
        val s = FloatArray((RATE * seconds).toInt())
        val n get() = s.size
        fun t(i: Int) = i.toFloat() / RATE
    }

    private fun Buf.tone(
        from: Float, to: Float = from, start: Float = 0f, len: Float = n.toFloat() / RATE, amp: Float = 0.5f,
        decay: Float = 6f, wave: (Double) -> Double = { sin(it) }, vibrato: Float = 0f,
    ) {
        var phase = 0.0
        val a = (start * RATE).toInt()
        val b = minOf(n, ((start + len) * RATE).toInt())
        for (i in a until b) {
            val k = (i - a).toFloat() / (b - a)
            val f = from + (to - from) * k + vibrato * sin(2 * PI * 7 * t(i)).toFloat()
            phase += 2 * PI * f / RATE
            val env = minOf(1f, (i - a) / (RATE * 0.004f)) * exp(-decay * k)
            s[i] += (wave(phase) * amp * env).toFloat()
        }
    }

    /** Noise through a one-pole low-pass; [cutoff] 0..1 (higher = brighter), [hiPass] removes rumble. */
    private fun Buf.noise(
        start: Float = 0f, len: Float = n.toFloat() / RATE, amp: Float = 0.5f, cutoff: Float = 0.5f,
        decay: Float = 6f, swell: Boolean = false, hiPass: Boolean = false, crackle: Float = 0f, seed: Int = 1,
    ) {
        val r = Random(seed)
        var lp = 0f
        var prev = 0f
        val a = (start * RATE).toInt()
        val b = minOf(n, ((start + len) * RATE).toInt())
        for (i in a until b) {
            val k = (i - a).toFloat() / (b - a)
            var x = r.nextFloat() * 2 - 1
            if (crackle > 0 && r.nextFloat() < crackle) x *= 6f
            lp += cutoff * (x - lp)
            val y = if (hiPass) lp - prev else lp
            prev = lp
            val env = if (swell) sin(PI * k).toFloat() else minOf(1f, (i - a) / (RATE * 0.003f)) * exp(-decay * k)
            s[i] += y * amp * env * (if (hiPass) 3f else 1f)
        }
    }

    private val square: (Double) -> Double = { if (sin(it) >= 0) 0.6 else -0.6 }
    private val saw: (Double) -> Double = { ((it / (2 * PI)) % 1.0) * 2 - 1 }

    fun render(sfx: Sfx): FloatArray = when (sfx) {
        Sfx.HIT -> Buf(0.16f).apply { noise(amp = 0.7f, cutoff = 0.6f, decay = 9f); tone(150f, 70f, amp = 0.7f, decay = 7f) }
        Sfx.HIT_BIG -> Buf(0.32f).apply {
            noise(amp = 0.9f, cutoff = 0.4f, decay = 6f); tone(110f, 45f, amp = 0.9f, decay = 4f)
        }
        Sfx.CRY -> Buf(0.14f).apply { tone(1100f, 650f, amp = 0.35f, decay = 3f, vibrato = 40f) }
        Sfx.KO -> Buf(0.7f).apply { tone(620f, 110f, amp = 0.4f, decay = 2f, wave = square, vibrato = 15f) }
        Sfx.EVADE -> Buf(0.24f).apply { noise(amp = 0.6f, cutoff = 0.25f, swell = true, seed = 3); tone(300f, 900f, amp = 0.08f) }
        Sfx.CHARGE -> Buf(0.45f).apply { tone(180f, 720f, amp = 0.25f, decay = 0.5f, wave = square); tone(360f, 1440f, amp = 0.1f, decay = 1f) }
        Sfx.SLASH -> Buf(0.16f).apply { noise(amp = 0.8f, cutoff = 0.9f, hiPass = true, decay = 7f, seed = 5); tone(2200f, 900f, amp = 0.08f) }
        Sfx.PUNCH -> Buf(0.14f).apply { noise(amp = 0.6f, cutoff = 0.3f, decay = 12f); tone(180f, 90f, amp = 0.8f, decay = 9f) }
        Sfx.BITE -> Buf(0.18f).apply {
            noise(len = 0.03f, amp = 0.8f, cutoff = 0.9f, hiPass = true, seed = 7)
            noise(start = 0.08f, len = 0.04f, amp = 0.9f, cutoff = 0.9f, hiPass = true, seed = 8)
            tone(320f, 220f, start = 0.08f, len = 0.08f, amp = 0.4f)
        }
        Sfx.BOOM -> Buf(0.6f).apply { noise(amp = 1f, cutoff = 0.12f, decay = 3.5f, seed = 9); tone(70f, 35f, amp = 0.9f, decay = 3f) }
        Sfx.BEAM -> Buf(0.55f).apply {
            tone(220f, 200f, amp = 0.25f, decay = 1.5f, wave = saw, vibrato = 18f); tone(440f, 400f, amp = 0.15f, decay = 2f)
            noise(amp = 0.2f, cutoff = 0.7f, hiPass = true, decay = 2f)
        }
        Sfx.WHOOSH -> Buf(0.3f).apply { noise(amp = 0.7f, cutoff = 0.18f, swell = true, seed = 11) }
        Sfx.FIRE -> Buf(0.45f).apply { noise(amp = 0.8f, cutoff = 0.2f, decay = 2.5f, crackle = 0.01f, seed = 13); tone(90f, 60f, amp = 0.3f, decay = 3f) }
        Sfx.WATER -> Buf(0.45f).apply {
            listOf(0f, 0.09f, 0.17f, 0.26f, 0.33f).forEachIndexed { k, st -> tone(380f + k * 60, 950f + k * 90, start = st, len = 0.07f, amp = 0.3f, decay = 3f) }
            noise(amp = 0.15f, cutoff = 0.4f, swell = true)
        }
        Sfx.LEAF -> Buf(0.32f).apply { noise(amp = 0.5f, cutoff = 0.8f, hiPass = true, swell = true, crackle = 0.02f, seed = 17) }
        Sfx.ICE -> Buf(0.4f).apply {
            tone(2100f, start = 0f, amp = 0.2f, decay = 5f); tone(2650f, start = 0.05f, len = 0.3f, amp = 0.18f, decay = 5f)
            tone(3150f, start = 0.1f, len = 0.28f, amp = 0.15f, decay = 5f); noise(len = 0.05f, amp = 0.4f, cutoff = 0.9f, hiPass = true)
        }
        Sfx.ZAP -> Buf(0.35f).apply {
            val r = Random(19)
            for (k in 0 until 7) tone(60f + r.nextFloat() * 900, 40f + r.nextFloat() * 300, start = k * 0.045f, len = 0.05f, amp = 0.35f, decay = 2f, wave = square)
            noise(amp = 0.3f, cutoff = 0.9f, hiPass = true, decay = 4f, crackle = 0.03f)
        }
        Sfx.WIND -> Buf(0.45f).apply { noise(amp = 0.8f, cutoff = 0.08f, swell = true, seed = 23); noise(amp = 0.2f, cutoff = 0.5f, hiPass = true, swell = true) }
        Sfx.ROCK -> Buf(0.4f).apply {
            tone(95f, 50f, amp = 0.8f, decay = 6f); noise(amp = 0.7f, cutoff = 0.25f, decay = 5f, seed = 29)
            noise(start = 0.08f, len = 0.3f, amp = 0.35f, cutoff = 0.5f, decay = 4f, crackle = 0.02f, seed = 31)
        }
        Sfx.BUFF -> Buf(0.36f).apply {
            listOf(440f, 554f, 659f, 880f).forEachIndexed { k, f -> tone(f, start = k * 0.07f, len = 0.14f, amp = 0.22f, decay = 3f, wave = square) }
        }
        Sfx.HEAL -> Buf(0.6f).apply {
            tone(660f, 880f, amp = 0.25f, decay = 2f, vibrato = 10f); tone(990f, 1320f, start = 0.12f, len = 0.45f, amp = 0.15f, decay = 2.5f)
        }
        Sfx.SHIELD -> Buf(0.35f).apply { tone(520f, amp = 0.3f, decay = 4f); tone(780f, amp = 0.2f, decay = 4f); noise(len = 0.04f, amp = 0.3f, cutoff = 0.8f) }
    }.let { b ->
        val peak = b.s.maxOf { kotlin.math.abs(it) }.coerceAtLeast(1e-3f)
        val gain = if (peak > 0.95f) 0.95f / peak else 1f
        FloatArray(b.n) { b.s[it] * gain }
    }

    fun wav(samples: FloatArray): ByteArray {
        val data = ByteBuffer.allocate(samples.size * 2).order(ByteOrder.LITTLE_ENDIAN)
        samples.forEach { data.putShort((it.coerceIn(-1f, 1f) * 32767).toInt().toShort()) }
        val h = ByteBuffer.allocate(44).order(ByteOrder.LITTLE_ENDIAN).apply {
            put("RIFF".toByteArray()); putInt(36 + data.capacity()); put("WAVE".toByteArray())
            put("fmt ".toByteArray()); putInt(16); putShort(1); putShort(1); putInt(RATE); putInt(RATE * 2)
            putShort(2); putShort(16)
            put("data".toByteArray()); putInt(data.capacity())
        }
        return ByteArrayOutputStream().apply { write(h.array()); write(data.array()) }.toByteArray()
    }
}
