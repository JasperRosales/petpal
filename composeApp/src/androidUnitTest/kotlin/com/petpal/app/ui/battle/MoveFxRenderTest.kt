package com.petpal.app.ui.battle

import android.graphics.Bitmap
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.CanvasDrawScope
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import com.petpal.app.domain.battle.*
import com.petpal.app.domain.model.*
import kotlin.random.Random
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** Draws every move's effects frame by frame with real Android graphics, so a bad radius or alpha throws here. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34])
class MoveFxRenderTest {
    @Test fun everyMoveDrawsWithoutCrashing() {
        val bmp = Bitmap.createBitmap(400, 300, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp.asImageBitmap())
        val scope = CanvasDrawScope()
        val aims = listOf(Offset(300f, 150f) to Offset(100f, 150f), Offset(200f, 150f) to Offset(200f, 150f),
            Offset(100f, 250f) to Offset(320f, 40f))
        for (m in Move.entries) for ((o, c) in aims) {
            var after = -m.windupMs.toLong()
            while (after <= 1300) {
                try {
                    scope.draw(Density(2.5f), LayoutDirection.Ltr, canvas, Size(400f, 300f)) {
                        drawMoveFx(m, o, c, after, isSelf = o == c)
                        if (after >= 0) drawBlast(c, 40f, after)
                    }
                } catch (e: Throwable) {
                    throw AssertionError("$m crashed at ${after}ms (origin $o, impact $c)", e)
                }
                after += 16
            }
        }
    }

    /** Every move used in a real (demo) fight, with the arena's own drawing code, for both a near and a far target. */
    @Test fun everyMoveDrawsInBattle() {
        val w = 400f; val h = 300f
        val bmp = Bitmap.createBitmap(w.toInt(), h.toInt(), Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp.asImageBitmap())
        val scope = CanvasDrawScope()
        val sx = w / ARENA_W; val sy = h / ARENA_H
        val infos = listOf(FighterInfo("A", Species.DRAKE, Stage.ADULT, 30), FighterInfo("B", Species.PENGUIN, Stage.ADULT, 30))
        for (m in Move.entries) for (gap in listOf(0.15f, 0.6f)) {
            val e = BattleEngine(infos, Random(m.ordinal), demo = true)
            e.place(0, 0.2f, 0.4f); e.place(1, 0.2f + gap, 0.45f)
            e.command(0, m, 0) ?: continue
            var t = 0L
            while (t <= m.windupMs + 1500) {
                e.tick(t)
                val snap = e.snapshot(t)
                try {
                    scope.draw(Density(2.5f), LayoutDirection.Ltr, canvas, Size(w, h)) {
                        snap.traps.forEach { tr -> drawTrap(tr, Offset(tr.x * sx, tr.y * sy), Trap.RADIUS * minOf(sx, sy)) }
                        snap.fighters.forEachIndexed { i, s -> drawMove(s, infos[i], sx, sy) }
                        snap.fighters.forEach { s -> if (s.hurtAgeMs < 260) drawImpact(Offset(s.x * sx, s.y * sy), s.hurtAgeMs / 260f) }
                        snap.blasts.forEach { b -> drawBlast(Offset(b.x * sx, b.y * sy), BLAST_RADIUS * minOf(sx, sy), b.ageMs) }
                    }
                } catch (ex: Throwable) {
                    throw AssertionError("$m crashed in battle at ${t}ms (gap $gap)", ex)
                }
                t += 16
            }
        }
    }
}
