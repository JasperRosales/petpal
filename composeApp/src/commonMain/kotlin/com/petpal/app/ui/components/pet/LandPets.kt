package com.petpal.app.ui.components.pet

import androidx.compose.ui.graphics.Color
import com.petpal.app.domain.model.Species
import com.petpal.app.domain.model.Stage
import com.petpal.app.ui.components.pet.PixelGrid.Companion.N
import com.petpal.app.ui.theme.SpriteCream
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

internal class Sprout : PetModel(Species.SPROUT, Color(0xFFB9E07A), Color(0xFF6FAE4E), SpriteCream) {
    override val build = 0.92f to 0.9f
    override fun PetCanvas.front() {
        val t = b.top - 1
        g.set(c, t, 'g'); g.set(c - 1, t, 'g')
        if (grown) listOf(-2, 1, -3, 2).forEach { g.set(c + it, t - 1, 'g') }
        if (big) { g.set(c - 1, t - 2, 'p'); g.set(c, t - 2, 'p'); g.set(c - 1, t - 3, 'y'); g.set(c, t - 3, 'y') }
    }
}

internal class Kitty : PetModel(Species.KITTY, Color(0xFFF0A868), Color(0xFFC97B3E), SpriteCream) {
    override val build = 0.9f to 0.94f
    override val sideTail = true
    override val eyes = EyeStyle.SHINY
    override val hasNose = true
    override fun PetCanvas.behind() = g.rect(b.tailX - 1, cy - (if (grown) 4 else 2), b.tailX, cy + 2, 'b')
    override fun PetCanvas.onTop() = g.spike(b.left + 2, b.top + 1, byStage(3, 4, 5), 4, 'b', 'p')
    override fun PetCanvas.front() {
        if (big) { g.set(c - 1, b.top + 1, 'd'); g.set(c, b.top + 1, 'd'); g.set(c, b.top + 2, 'd') }
    }
}

internal class Pup : PetModel(Species.PUP, Color(0xFFE3C08D), Color(0xFF9C6B3F), SpriteCream) {
    override val hasNose = true
    override fun PetCanvas.markings() {
        g.ellipse(b.cx, b.cy + b.ry * 0.2f, 2.2f, 1.5f, 'l', 'b')
        val h = byStage(3, 4, 5) // floppy ears hang over the sides of the head
        g.rect(b.left - 1, b.top + 1, b.left + 1, b.top + h, 'd')
        g.rect(N - 1 - b.left - 1, b.top + 1, N - 1 - b.left + 1, b.top + h, 'd')
    }
    override fun PetCanvas.front() { if (big) g.rect(c - 3, b.bottom - 3, c + 2, b.bottom - 3, 'r') } // collar
}

internal class Bunny : PetModel(Species.BUNNY, Color(0xFFE9E1F2), Color(0xFFBDAED6), Color(0xFFFFFBF5)) {
    override val build = 0.86f to 0.86f
    override val eyes = EyeStyle.SHINY
    override fun PetCanvas.onTop() {
        val h = when (stage) { Stage.BABY -> 3; Stage.KID -> 5; else -> 6 }
        val x = b.cx.toInt() - 3
        g.rect(x - 1, b.top - h + 1, x + 1, b.top + 1, 'b')
        g.rect(N - 1 - x - 1, b.top - h + 1, N - 1 - x + 1, b.top + 1, 'b')
        for (y in b.top - h + 2..b.top) g.mirror(x, y, 'p')
    }
}

internal class Fox : PetModel(Species.FOX, Color(0xFFE9824A), Color(0xFFB5552A), Color(0xFFFFF4E6)) {
    override val build = 0.86f to 1.02f
    override val sideTail = true
    override val eyes = EyeStyle.SLIT
    override val hasNose = true
    override fun PetCanvas.behind() {
        val r = byStage(2f, 2.6f, 3.2f)
        g.ellipse(b.cx + b.rx, b.cy + 1f, r, r * 1.3f, 'b')
        g.ellipse(b.cx + b.rx + r * 0.4f, b.cy - r * 0.6f, r * 0.6f, r * 0.6f, 'l')
    }
    override fun PetCanvas.onTop() = g.spike(b.left + 2, b.top + 1, byStage(4, 5, 6), 4, 'b', 'e')
    override fun PetCanvas.markings() = g.ellipse(b.cx, b.cy + b.ry * 0.25f, 2.6f, 1.6f, 'l', 'b')
}

internal class Bear : PetModel(Species.BEAR, Color(0xFFB07A4F), Color(0xFF6E4A2E), Color(0xFFE8CFA8)) {
    override val build = 1.1f to 1.06f
    override val hasNose = true
    override fun PetCanvas.onTop() {
        val r = byStage(1.8f, 2.1f, 2.4f)
        g.ellipse(b.cx - b.rx * 0.62f, b.top + 0.6f, r, r, 'b'); g.ellipse(b.cx + b.rx * 0.62f, b.top + 0.6f, r, r, 'b')
    }
    override fun PetCanvas.markings() = g.ellipse(b.cx, b.cy + b.ry * 0.2f, 2.2f, 1.5f, 'l', 'b')
    override fun PetCanvas.front() { if (big) g.mirror(ex - 2, ey + 3, 'd') }
}

internal class Deer : PetModel(Species.DEER, Color(0xFFC98E5A), Color(0xFF8A5A32), Color(0xFFF6E7CF)) {
    override val build = 0.82f to 1.06f
    override val eyes = EyeStyle.SHINY
    override val hasNose = true
    override fun PetCanvas.onTop() {
        g.rect(b.left - 1, b.top + 1, b.left + 1, b.top + 2, 'b') // small sideways ears
        g.rect(N - 1 - b.left - 1, b.top + 1, N - 1 - b.left + 1, b.top + 2, 'b')
        if (!grown) return
        val h = if (big) 4 else 3 // antlers
        val ax = b.cx.toInt() - 3
        for (i in 0 until h) g.mirror(ax, b.top - i, 'h')
        g.mirror(ax - 1, b.top - h + 1, 'h'); g.mirror(ax + 1, b.top - h + 2, 'h')
        if (big) { g.mirror(ax - 2, b.top - h, 'h'); g.mirror(ax + 1, b.top - h, 'h') }
    }
    override fun PetCanvas.front() {
        if (!grown) listOf(-3 to -1, 2 to 0, -2 to 2, 3 to 2).forEach { (dx, dy) -> g.setIf(c + dx, cy + dy, 'b', 'h') } // fawn spots
    }
}

internal class Panda : PetModel(Species.PANDA, Color(0xFFF7F5EF), Color(0xFF2B2B2B), Color.White) {
    override val build = 1.08f to 1.04f
    override val hasNose = true
    override fun PetCanvas.onTop() {
        val r = byStage(1.6f, 1.9f, 2.2f)
        g.ellipse(b.cx - b.rx * 0.62f, b.top + 0.6f, r, r, 'd'); g.ellipse(b.cx + b.rx * 0.62f, b.top + 0.6f, r, r, 'd')
    }
    override fun PetCanvas.afterOutline() { // eye patches
        val (x, y) = patch()
        listOf(x to y, x - 1 to y, x to y - 1, x - 1 to y - 1, x to y + 1).forEach { (px, py) -> g.mirror(px, py, 'd') }
    }
}

internal class Raccoon : PetModel(Species.RACCOON, Color(0xFFA3A3A8), Color(0xFF55555C), Color(0xFFE6E6EA)) {
    override val sideTail = true
    override val hasNose = true
    override fun PetCanvas.behind() { // ringed bushy tail
        val r = byStage(1.8f, 2.2f, 2.6f)
        g.ellipse(b.cx + b.rx + 0.6f, b.cy, r, r * 1.4f, 'b')
        g.ellipse(b.cx + b.rx + 0.6f, b.cy - r * 0.3f, r, 0.6f, 'd', onlyOver = 'b')
        g.ellipse(b.cx + b.rx + 0.6f, b.cy + r * 0.6f, r, 0.6f, 'd', onlyOver = 'b')
    }
    override fun PetCanvas.onTop() = roundEars()
    override fun PetCanvas.afterOutline() { // bandit mask
        val (px, py) = patch()
        for (x in (px - 2)..(N - 1 - px + 2)) for (y in py - 1..py) {
            if (g[x, y] != '.' && g[x, y] != 'o') g.set(x, y, 'd')
        }
    }
}

internal class Hamster : PetModel(Species.HAMSTER, Color(0xFFF2C27A), Color(0xFFB9824A), Color(0xFFFFF4E2)) {
    override val build = 1.08f to 0.94f
    override val eyes = EyeStyle.SHINY
    override val hasNose = true
    override fun PetCanvas.onTop() = roundEars()
    override fun PetCanvas.markings() = listOf(-1, 1).forEach { s -> // puffy cheek pouches
        g.ellipse(b.cx + s * b.rx * 0.62f, b.cy + b.ry * 0.2f, 1.8f, 1.5f, 'l', 'b')
    }
}

internal class Koala : PetModel(Species.KOALA, Color(0xFF9EA3AB), Color(0xFF5E636B), Color(0xFFE4E6EA)) {
    override val build = 0.96f to 1.0f
    override val eyes = EyeStyle.SHINY
    override val hasNose = true
    override fun PetCanvas.onTop() { // big fluffy ears
        val r = byStage(2f, 2.4f, 2.8f)
        listOf(-1, 1).forEach { s ->
            g.ellipse(b.cx + s * b.rx * 0.8f, b.top + 1f, r, r, 'b')
            g.ellipse(b.cx + s * b.rx * 0.8f, b.top + 1f, r * 0.5f, r * 0.5f, 'l')
        }
    }
    override fun PetCanvas.snout() { // big dark nose
        g.set(c - 1, my - 2, 'e'); g.set(c, my - 2, 'e'); g.set(c - 2, my - 1, 'e'); g.set(c + 1, my - 1, 'e')
    }
}

internal class Monkey : PetModel(Species.MONKEY, Color(0xFF9A6A44), Color(0xFF5E3D24), Color(0xFFF0D2AE)) {
    override val build = 0.84f to 1.0f
    override val sideTail = true
    override val hasNose = true
    override fun PetCanvas.behind() { // curly tail
        val ty = cy + 2
        val r = if (big) 1.8f else 1.4f
        g.rect(b.tailX - 1, ty, b.tailX + 1, ty, 'd')
        g.ellipse(b.tailX + 1.5f + r * 0.5f, ty - r, r, r, 'd')
        g.ellipse(b.tailX + 1.5f + r * 0.5f, ty - r, r * 0.45f, r * 0.45f, '.', onlyOver = 'd')
    }
    override fun PetCanvas.onTop() { // big round ears out to the sides
        val r = if (big) 2f else 1.6f
        listOf(-1, 1).forEach { s ->
            g.ellipse(b.cx + s * (b.rx + 0.2f), b.cy - b.ry * 0.3f, r, r, 'b')
            g.ellipse(b.cx + s * (b.rx + 0.2f), b.cy - b.ry * 0.3f, r * 0.5f, r * 0.5f, 'l')
        }
    }
    override fun PetCanvas.markings() = g.ellipse(b.cx, b.cy + b.ry * 0.05f, b.rx * 0.6f, b.ry * 0.55f, 'l', 'b') // face
    override fun PetCanvas.front() { if (big) g.mirror(c - 3, b.top + 1, 'd') } // brow tufts
}

internal class Kangaroo : PetModel(Species.KANGAROO, Color(0xFFD08A52), Color(0xFF8E5530), Color(0xFFF6DDBF)) {
    override val build = 0.8f to 1.1f
    override val sideTail = true
    override val hasNose = true
    override fun PetCanvas.behind() = g.rect(b.tailX - 1, b.bottom - 2, b.tailX + byStage(1, 2, 3), b.bottom - 1, 'b')
    override fun PetCanvas.onTop() = g.spike(b.left + 2, b.top + 1, byStage(3, 4, 5), 3, 'b', 'l')
    override fun PetCanvas.front() {
        if (!grown) return
        val py = cy + 2 // pouch
        for (x in c - 2..c + 1) g.set(x, py, 'd')
        g.set(c - 2, py + 1, 'd'); g.set(c + 1, py + 1, 'd')
    }
}

internal class Elephant : PetModel(Species.ELEPHANT, Color(0xFFA7AEB8), Color(0xFF6B7380), Color(0xFFD8DCE2)) {
    override val build = 1.12f to 1.04f
    override val eyes = EyeStyle.BEADY
    override fun PetCanvas.onTop() { // big flappy ears
        val r = byStage(2.5f, 3f, 3.4f)
        listOf(-1, 1).forEach { s ->
            g.ellipse(b.cx + s * (b.rx + 0.2f), b.cy - b.ry * 0.25f, r, r * 1.2f, 'b')
            g.ellipse(b.cx + s * (b.rx + 0.4f), b.cy - b.ry * 0.25f, r * 0.55f, r * 0.75f, 'p')
        }
    }
    override fun PetCanvas.front() { // trunk, plus tusks once grown
        for (y in my - 1..my + (if (grown) 3 else 2)) { g.set(c - 1, y, 'd'); g.set(c, y, 'd') }
        g.set(c + 1, my + (if (grown) 3 else 2), 'd')
        if (grown) { g.set(c - 2, my + 1, 'h'); g.set(c + 1, my + 1, 'h') }
        if (big) { g.set(c - 3, my + 2, 'h'); g.set(c + 2, my + 2, 'h') }
    }
}

internal class Tiger : PetModel(Species.TIGER, Color(0xFFF08A2C), Color(0xFF2B2420), Color(0xFFFFF4E6)) {
    override val build = 1.04f to 0.96f
    override val sideTail = true
    override val eyes = EyeStyle.SLIT
    override val hasNose = true
    override fun PetCanvas.behind() { // striped tail
        g.rect(b.tailX - 1, cy - (if (grown) 4 else 2), b.tailX, cy + 2, 'b')
        g.set(b.tailX, cy - 1, 'd'); if (grown) g.set(b.tailX, cy - 3, 'd')
    }
    override fun PetCanvas.onTop() = g.spike(b.left + 2, b.top + 1, if (big) 4 else 3, 4, 'b', 'l')
    override fun PetCanvas.front() { // forehead and side stripes
        g.set(c - 1, b.top + 1, 'd'); g.set(c, b.top + 1, 'd'); g.set(c - 1, b.top + 2, 'd')
        if (grown) g.mirror(c - 3, b.top + 1, 'd')
        for (dy in 0..2) if (g[b.left + 1, cy + dy] == 'b') g.mirror(b.left + 1, cy + dy, 'd')
        if (big) for (dy in 0..1) if (g[b.left + 2, cy - 2 + dy] == 'b') g.mirror(b.left + 2, cy - 2 + dy, 'd')
    }
}

internal class Capybara : PetModel(Species.CAPYBARA, Color(0xFFB08456), Color(0xFF6E4C2E), Color(0xFFD9B98E)) {
    override val build = 1.12f to 0.9f
    override val eyes = EyeStyle.BEADY
    override fun PetCanvas.onTop() { // tiny round ears
        val r = if (big) 1.2f else 1f
        g.ellipse(b.cx - b.rx * 0.6f, b.top + 0.8f, r, r, 'd'); g.ellipse(b.cx + b.rx * 0.6f, b.top + 0.8f, r, r, 'd')
    }
    override fun PetCanvas.front() { // broad dark snout, plus a yuzu on its head once grown
        for (x in c - 2..c + 1) g.set(x, my - 1, 'd')
        if (grown) { g.set(c - 1, b.top - 1, 'y'); g.set(c, b.top - 1, 'y'); g.set(c - 1, b.top - 2, 'y'); g.set(c, b.top - 2, 'y') }
        if (big) g.set(c, b.top - 3, 'g')
    }
}

internal class Mole : PetModel(Species.MOLE, Color(0xFF6B5A6E), Color(0xFF3E3342), Color(0xFFB8A9BC)) {
    override val build = 1.1f to 0.8f
    override val eyes = EyeStyle.BEADY
    override fun PetCanvas.snout() { // big pink snout
        for (x in c - 2..c + 1) g.set(x, my - 1, 'p')
        g.set(c - 1, my - 2, 'p'); g.set(c, my - 2, 'p')
    }
    override fun PetCanvas.front() { // digging claws
        val y = cy + 2
        g.mirror(b.left, y, 'h'); g.mirror(b.left, y + 1, 'h'); g.mirror(b.left + 1, y + 1, 'h')
        if (grown) g.mirror(b.left - 1, y + 1, 'h')
    }
}

internal class Hedgehog : PetModel(Species.HEDGEHOG, Color(0xFFD9B38C), Color(0xFF6B4A33), Color(0xFFF5E6D0)) {
    override val build = 1.04f to 0.82f
    override val hasNose = true
    override fun PetCanvas.behind() { // spiky back peeking out all around the top
        g.ellipse(b.cx, b.cy - 0.6f, b.rx + 1.2f, b.ry + 1f, 'd')
        val n = if (big) 7 else 5
        for (k in 0 until n) {
            val a = PI * (0.1 + 0.8 * k / (n - 1))
            g.set((b.cx - cos(a) * (b.rx + 1.6f)).toInt(), (b.cy - 0.6f - sin(a) * (b.ry + 1.6f)).toInt(), 'd')
        }
    }
    override fun PetCanvas.markings() = g.ellipse(b.cx, b.cy + b.ry * 0.1f, b.rx * 0.7f, b.ry * 0.75f, 'l', 'b')
}

internal class Otter : PetModel(Species.OTTER, Color(0xFF9C6B4A), Color(0xFF6A4630), Color(0xFFEBD3B5)) {
    override val build = 0.88f to 1.04f
    override val hasNose = true
    override fun PetCanvas.onTop() = roundEars()
    override fun PetCanvas.markings() = g.ellipse(b.cx, b.cy + b.ry * 0.2f, 2.6f, 1.6f, 'l', 'b')
    override fun PetCanvas.front() { g.mirror(c - 3, my, 'd'); g.mirror(c - 4, my - 1, 'd') } // whiskers
}

internal class Beaver : PetModel(Species.BEAVER, Color(0xFFA0703F), Color(0xFF5E3F22), Color(0xFFE2C49A)) {
    override val build = 1.0f to 0.96f
    override val sideTail = true
    override val hasNose = true
    override fun PetCanvas.behind() { // flat paddle tail
        val r = byStage(1.6f, 2f, 2.4f)
        g.ellipse(b.cx + b.rx + r * 0.5f, b.bottom - r * 0.6f, r, r * 0.8f, 'd')
    }
    override fun PetCanvas.onTop() = roundEars()
    override fun PetCanvas.front() { g.set(c - 1, my + 2, 'h'); g.set(c, my + 2, 'h') } // buck teeth
}

/** Small round ears on top (otter, raccoon, hamster, beaver). */
internal fun PetCanvas.roundEars() {
    val r = if (big) 1.6f else 1.3f
    g.ellipse(b.cx - b.rx * 0.65f, b.top + 1f, r, r, 'b'); g.ellipse(b.cx + b.rx * 0.65f, b.top + 1f, r, r, 'b')
}

/** Where eye patches/masks sit: left column and row. */
internal fun PetCanvas.patch(): Pair<Int, Int> =
    (b.cx - b.rx * 0.4f).toInt() to (b.cy - b.ry * 0.15f).roundToInt()
