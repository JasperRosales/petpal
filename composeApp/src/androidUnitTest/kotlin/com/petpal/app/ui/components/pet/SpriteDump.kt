package com.petpal.app.ui.components.pet

import androidx.compose.ui.graphics.toArgb
import com.petpal.app.domain.model.Mood
import com.petpal.app.domain.model.Species
import com.petpal.app.domain.model.Stage
import java.io.File
import kotlin.test.Test

/** Dumps every sprite as text when SPRITE_DUMP is set, for previewing art off-device. */
class SpriteDump {
    @Test
    fun dump() {
        val out = System.getenv("SPRITE_DUMP") ?: return
        File(out).printWriter().use { w ->
            for (s in Species.entries) for (st in Stage.entries) {
                val m = s.model
                w.println("# ${s.name} ${st.name}")
                w.println(m.palette().entries.joinToString(" ") { (k, v) -> "$k=%08x".format(v.toArgb()) })
                m.draw(st, Mood.HAPPY).forEach(w::println)
            }
        }
    }
}
