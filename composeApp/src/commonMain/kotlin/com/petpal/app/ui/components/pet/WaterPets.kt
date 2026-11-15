package com.petpal.app.ui.components.pet

import androidx.compose.ui.graphics.Color
import com.petpal.app.domain.model.Mood
import com.petpal.app.domain.model.Species
import com.petpal.app.ui.components.pet.PixelGrid.Companion.N

internal class Turtle : PetModel(Species.TURTLE, Color(0xFF8FCFB0), Color(0xFF7B5A3A), Color(0xFFE9F2C8)) {
    /** Smaller body tucked under a shell dome. */
    override fun shapeBody(b0: Body) = b0.copy(cy = b0.cy + 1f, rx = b0.rx - 0.5f, ry = b0.ry - 1f)
    override fun PetCanvas.behind() = g.ellipse(b0.cx, b0.cy - 1.2f, b0.rx + 0.8f, b0.ry, 'd')
    override fun PetCanvas.afterBody() {
        if (!grown) return
        val sy = b0.cy - b0.ry - 0.2f // shell spots
        listOf(-3f, 0f, 3f).forEach { dx -> g.ellipse(b.cx + dx, sy + if (dx == 0f) 0f else 1.2f, 1f, 0.9f, 'y', onlyOver = 'd') }
    }
}

internal class Frog : PetModel(Species.FROG, Color(0xFF8BD06A), Color(0xFF4E9A3E), Color(0xFFF0F2B0)) {
    override val build = 1.08f to 0.76f
    override fun PetCanvas.onTop() {
        val r = if (big) 2.2f else 1.8f
        g.ellipse(b.cx - b.rx * 0.5f, b.top + 0.8f, r, r, 'b'); g.ellipse(b.cx + b.rx * 0.5f, b.top + 0.8f, r, r, 'b')
    }
    override fun PetCanvas.eyeY(default: Int) = b.top + 1
    override fun PetCanvas.eyeX() = (b.cx - b.rx * 0.5f).toInt()
    override fun PetCanvas.front() { if (grown) { g.set(c - 4, cy + 2, 'd'); g.set(c + 3, cy + 1, 'd') } }
}

internal class Axolotl : PetModel(Species.AXOLOTL, Color(0xFFF6B8CC), Color(0xFFE07A9E), Color(0xFFFFE4EC)) {
    override val build = 1.02f to 0.8f
    override val eyes = EyeStyle.SHINY
    override fun PetCanvas.behind() { // feathery gills fanning out from the head
        val n = if (big) 3 else 2
        for (k in 0 until n) {
            val len = if (k == 1 || n == 2) 3 else 2
            for (x in b.left - len..b.left + 1) g.mirror(x, b.top + 1 + k * 2, 'd')
        }
    }
    override fun PetCanvas.front() { if (grown) listOf(-2, 1).forEach { dx -> g.set(c + dx, cy + 2, 'd') } } // freckles
}

internal class Shark : PetModel(Species.SHARK, Color(0xFF6E8FB0), Color(0xFF3F5C7A), Color(0xFFF2F5F8)) {
    override val build = 1.06f to 0.74f
    override val sideTail = true
    override val eyes = EyeStyle.BEADY
    override fun PetCanvas.behind() { // tail fin sticking out to the side
        val tx = b.tailX
        val ty = cy + 1
        g.rect(tx - 1, ty, tx + 1, ty + 1, 'b')
        for (i in 0..(if (big) 3 else 2)) { g.set(tx + 1 + i / 2, ty - i, 'd'); g.set(tx + 1 + i / 2, ty + 1 + i, 'd') }
    }
    override fun PetCanvas.onTop() = g.spike(b.cx.toInt() - 1, b.top + 1, byStage(3, 4, 5), 2, 'd') // dorsal fin
    override fun PetCanvas.front() = listOf(0, 1).forEach { dy -> g.mirror(b.left + 1, cy + dy, 'd') } // gills
}

internal class Seal : PetModel(Species.SEAL, Color(0xFFB8C2CC), Color(0xFF6E7C8A), Color(0xFFEEF2F5)) {
    override val build = 1.08f to 0.78f
    override val eyes = EyeStyle.SHINY
    override val hasNose = true
    override fun PetCanvas.behind() { // tail flippers
        val w = if (big) 3 else 2
        g.rect(b.cx.toInt() - w, b.bottom - 1, b.cx.toInt() - 1, b.bottom, 'd')
        g.rect(b.cx.toInt(), b.bottom - 1, b.cx.toInt() + w - 1, b.bottom, 'd')
    }
    override fun PetCanvas.markings() = g.ellipse(b.cx, b.cy + b.ry * 0.25f, b.rx * 0.6f, b.ry * 0.6f, 'l', 'b')
    override fun PetCanvas.front() { // whiskers and front flippers
        g.mirror(c - 3, my, 'd'); g.mirror(c - 4, my - 1, 'd')
        g.mirror(b.left + 1, cy + 2, 'd')
    }
}

internal class Tilapia : PetModel(Species.TILAPIA, Color(0xFF8FA8A0), Color(0xFF4F6B66), Color(0xFFE6EEDF)) {
    override val build = 1.0f to 0.72f
    override val sideTail = true
    override val eyes = EyeStyle.BEADY
    override val hasFeet = false
    override fun PetCanvas.behind() { // fanned tail fin
        for (i in 0..(if (big) 4 else 3)) for (y in cy - i..cy + i) g.set(b.tailX - 1 + i, y, 'd')
    }
    override fun PetCanvas.onTop() { // spiny dorsal fin
        for (k in -3..2) {
            val h = if ((k + 3) % 2 == 0) (if (big) 3 else 2) else 1
            for (i in 0..h) g.set(N / 2 + k, b.top - i + 1, 'd')
        }
    }
    override fun PetCanvas.front() { // gills, faint bars and side fins
        listOf(0, 1).forEach { dy -> g.mirror(b.left + 1, cy + dy, 'd') }
        if (grown) listOf(-3, 2).forEach { dx -> for (dy in -2..-1) g.setIf(c + dx, cy + dy, 'b', 'd') }
        g.mirror(b.left + 1, cy + 2, 'd')
    }
}

internal class Crab : PetModel(Species.CRAB, Color(0xFFE8634A), Color(0xFFA83A2A), Color(0xFFF6C7A8)) {
    override val build = 0.98f to 0.7f
    override val eyes = EyeStyle.BEADY
    override val blush = false
    override val hasFeet = false
    override fun PetCanvas.behind() { // big claws held up at the sides
        val r = byStage(1.5f, 1.9f, 2.2f)
        listOf(-1, 1).forEach { s ->
            val x = b.cx + s * (b.rx + 0.4f)
            g.ellipse(x, b.cy - b.ry * 0.6f, r, r, 'd')
            g.ellipse(x + s * r * 0.3f, b.cy - b.ry * 0.6f - r * 0.6f, r * 0.35f, r * 0.55f, '.', onlyOver = 'd')
        }
        for (k in 0..2) g.mirror(b.left + 1 + k, b.bottom, 'd') // little legs
    }
    override fun PetCanvas.onTop() { for (i in 0..(if (big) 3 else 2)) g.mirror(N / 2 - 3, b.top - i + 1, 'b') } // eye stalks
    override fun PetCanvas.eyeY(default: Int) = b.top - (if (big) 2 else 1)
    override fun PetCanvas.eyeX() = N / 2 - 3
    override fun PetCanvas.front() {
        if (mood != Mood.SLEEPY) { g.mirror(ex - 1, ey, 'w'); g.mirror(ex, ey, 'e') } // eyes on the stalk tips
        if (grown) listOf(-3, 2).forEach { dx -> g.setIf(c + dx, cy, 'b', 'd') } // shell spots
    }
}

internal class Lizard : PetModel(Species.LIZARD, Color(0xFF7FB85A), Color(0xFF4B7A33), Color(0xFFF2D08A)) {
    override val build = 1.02f to 0.74f
    override val sideTail = true
    override val eyes = EyeStyle.SLIT
    override fun PetCanvas.behind() { // long tail curling out to the side
        val ty = cy + 1
        val reach = if (grown) 2 else 1
        g.rect(b.tailX - 1, ty, b.tailX + reach, ty + 1, 'b')
        g.rect(b.tailX + reach, ty - 2, b.tailX + reach + 1, ty + 1, 'b')
    }
    override fun PetCanvas.onTop() { if (big) g.spike(b.left + 3, b.top + 1, 2, 2, 'd') }
    override fun PetCanvas.front() { if (grown) listOf(-3, 0, 3).forEach { dx -> g.set(c + dx, cy - 1, 'd') } } // back scales
}

internal class Chameleon : PetModel(Species.CHAMELEON, Color(0xFF7ACB5E), Color(0xFF3F8A3A), Color(0xFFE8F2A0)) {
    override val build = 1.0f to 0.8f
    override val sideTail = true
    override val eyes = EyeStyle.SLIT
    override fun PetCanvas.behind() { // curled tail
        val tx = b.tailX
        val ty = cy + 2
        val r = if (big) 1.8f else 1.4f
        g.rect(tx - 1, ty, tx + 1, ty + 1, 'b')
        g.ellipse(tx + 1.5f + r * 0.5f, ty - r * 0.6f, r, r, 'b')
        g.ellipse(tx + 1.5f + r * 0.5f, ty - r * 0.6f, r * 0.45f, r * 0.45f, 'd', onlyOver = 'b')
    }
    override fun PetCanvas.onTop() = g.spike(b.cx.toInt() - 1, b.top + 1, if (big) 3 else 2, 3, 'd') // head crest
    override fun PetCanvas.front() {
        if (grown) listOf(-3, 0, 3).forEach { dx -> for (dy in 0..1) g.setIf(c + dx, cy + dy, 'b', 'd') } // stripes
    }
}
