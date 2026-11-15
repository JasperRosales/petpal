package com.petpal.app.ui.components.pet

import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * A 20x20 character canvas pets are painted on. Each char is a palette key
 * ('o' outline, 'b' body, 'd' dark, 'l' belly, ...); '.' is empty.
 */
internal class PixelGrid {
    val c = Array(N) { CharArray(N) { '.' } }

    operator fun get(x: Int, y: Int) = if (x in 0 until N && y in 0 until N) c[y][x] else '.'
    fun set(x: Int, y: Int, ch: Char) { if (x in 0 until N && y in 0 until N) c[y][x] = ch }
    fun mirror(x: Int, y: Int, ch: Char) { set(x, y, ch); set(N - 1 - x, y, ch) }

    /** Paints [ch] only where the cell currently holds [over]. */
    fun setIf(x: Int, y: Int, over: Char, ch: Char) { if (this[x, y] == over) set(x, y, ch) }

    fun ellipse(cx: Float, cy: Float, rx: Float, ry: Float, ch: Char, onlyOver: Char? = null) {
        for (y in 0 until N) for (x in 0 until N) {
            val dx = (x + 0.5f - cx) / rx
            val dy = (y + 0.5f - cy) / ry
            if (dx * dx + dy * dy <= 1f && (onlyOver == null || this[x, y] == onlyOver)) set(x, y, ch)
        }
    }

    fun rect(x0: Int, y0: Int, x1: Int, y1: Int, ch: Char) {
        for (y in y0..y1) for (x in x0..x1) set(x, y, ch)
    }

    /** Pointed ear/horn from [baseY] upward, centered on column [cx] (and mirrored). */
    fun spike(cx: Int, baseY: Int, h: Int, w: Int, fill: Char, inner: Char? = null) {
        for (i in 0 until h) {
            val half = ((w / 2f) * (1f - i.toFloat() / h)).roundToInt()
            for (dx in -half..half) {
                val ch = if (inner != null && abs(dx) < half && i < h - 1) inner else fill
                mirror(cx + dx, baseY - i, ch)
            }
        }
    }

    /** Turns every filled cell that touches empty space into outline. */
    fun outline() {
        val edge = mutableListOf<Pair<Int, Int>>()
        for (y in 0 until N) for (x in 0 until N) {
            if (this[x, y] == '.') continue
            if (this[x - 1, y] == '.' || this[x + 1, y] == '.' || this[x, y - 1] == '.' || this[x, y + 1] == '.' ||
                x == 0 || y == 0 || x == N - 1 || y == N - 1
            ) edge += x to y
        }
        edge.forEach { (x, y) -> set(x, y, 'o') }
    }

    fun rows() = c.map { it.concatToString() }

    companion object {
        const val N = 20
    }
}

/** The body ellipse every blob-template pet is built around. */
internal data class Body(val cy: Float, val rx: Float, val ry: Float) {
    val cx = PixelGrid.N / 2f
    val top get() = (cy - ry).roundToInt()
    val bottom get() = (cy + ry).roundToInt()
    val left get() = (cx - rx).roundToInt()
    /** Column just past the right edge, where side tails attach. */
    val tailX get() = left + 2 * rx.toInt()
}
