package com.petpal.app.ui.components.pet

/**
 * A 20x20 character canvas for generated sprites (eggs, fried fish). Each char is a palette key
 * ('o' outline, 'b' body, 'd' dark, 'l' belly, ...); '.' is empty.
 */
internal class PixelGrid {
    val c = Array(N) { CharArray(N) { '.' } }

    operator fun get(x: Int, y: Int) = if (x in 0 until N && y in 0 until N) c[y][x] else '.'
    fun set(x: Int, y: Int, ch: Char) { if (x in 0 until N && y in 0 until N) c[y][x] = ch }

    /** Paints [ch] only where the cell currently holds [over]. */
    fun setIf(x: Int, y: Int, over: Char, ch: Char) { if (this[x, y] == over) set(x, y, ch) }

    fun ellipse(cx: Float, cy: Float, rx: Float, ry: Float, ch: Char, onlyOver: Char? = null) {
        for (y in 0 until N) for (x in 0 until N) {
            val dx = (x + 0.5f - cx) / rx
            val dy = (y + 0.5f - cy) / ry
            if (dx * dx + dy * dy <= 1f && (onlyOver == null || this[x, y] == onlyOver)) set(x, y, ch)
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
