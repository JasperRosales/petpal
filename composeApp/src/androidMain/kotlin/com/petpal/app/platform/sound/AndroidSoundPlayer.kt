package com.petpal.app.platform.sound

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import com.petpal.app.domain.platform.Sfx
import com.petpal.app.domain.platform.SoundPlayer
import java.io.File
import java.util.concurrent.ConcurrentHashMap
import kotlin.concurrent.thread

/** Synthesizes every [Sfx] to a small WAV in the cache once, then plays them through a SoundPool. */
class AndroidSoundPlayer(context: Context) : SoundPlayer {
    private val prefs = context.getSharedPreferences("sound", Context.MODE_PRIVATE)
    private val pool = SoundPool.Builder()
        .setMaxStreams(10)
        .setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_GAME)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()
        ).build()
    private val ids = ConcurrentHashMap<Sfx, Int>()

    override var enabled: Boolean
        get() = prefs.getBoolean("enabled", true)
        set(v) { prefs.edit().putBoolean("enabled", v).apply() }

    init {
        val dir = File(context.cacheDir, "sfx-v1").apply { mkdirs() }
        thread(name = "sfx-synth", isDaemon = true) {
            Sfx.entries.forEach { sfx ->
                val f = File(dir, "${sfx.name.lowercase()}.wav")
                if (!f.exists()) f.writeBytes(Synth.wav(Synth.render(sfx)))
                ids[sfx] = pool.load(f.path, 1)
            }
        }
    }

    override fun play(sfx: Sfx, pitch: Float, volume: Float) {
        if (!enabled) return
        val id = ids[sfx] ?: return
        val v = volume.coerceIn(0f, 1f)
        pool.play(id, v, v, 1, 0, pitch.coerceIn(0.5f, 2f))
    }
}
