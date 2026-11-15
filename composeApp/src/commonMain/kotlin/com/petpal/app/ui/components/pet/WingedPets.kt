package com.petpal.app.ui.components.pet

import androidx.compose.ui.graphics.Color
import com.petpal.app.domain.model.Species
import com.petpal.app.domain.model.Stage

/** Wings fanning out from both sides; column i (counting outward) covers rows [rows]. */
private fun PetCanvas.wings(span: Int, rows: (Int) -> IntRange) {
    for (i in 0..span) for (y in rows(i)) for (x in b.left - 1 - i..b.left) g.mirror(x, y, 'd')
}

internal class Drake : PetModel(Species.DRAKE, Color(0xFF7CC98A), Color(0xFF3E8A55), Color(0xFFF3E3A6)) {
    override fun PetCanvas.behind() { val wy = cy - 2; wings(byStage(1, 2, 3)) { i -> wy - i..wy + 2 } }
    override fun PetCanvas.onTop() = g.spike(b.left + 3, b.top + 1, if (big) 4 else 3, 2, 'h')
    override fun PetCanvas.front() { if (big) listOf(0, 2, 4).forEach { g.set(c - 3 + it, b.top - 1, 'd') } }
}

internal class Owl : PetModel(Species.OWL, Color(0xFFA9825A), Color(0xFF6E5034), Color(0xFFF1E2C2)) {
    override val build = 0.86f to 1.1f
    override val blush = false
    override val wideEyes = true
    override fun PetCanvas.onTop() = g.spike(b.left + 2, b.top + 1, if (big) 4 else 3, 2, 'd')
    override fun PetCanvas.markings() = listOf(-1, 1).forEach { s ->
        g.ellipse(b.cx + s * b.rx * 0.4f, b.cy - b.ry * 0.1f, 1.9f, 1.9f, 'l', 'b')
    }
    override fun PetCanvas.mouth() = beak(hooked = false)
}

internal class Bat : PetModel(Species.BAT, Color(0xFF8A6FA8), Color(0xFF4E3A66), Color(0xFFD8C8E8)) {
    override val build = 0.76f to 0.8f
    override val eyes = EyeStyle.SHINY
    override fun PetCanvas.behind() { val wy = cy - 3; wings(byStage(2, 3, 4)) { i -> wy - i / 2..wy + 3 - i % 2 } }
    override fun PetCanvas.onTop() = g.spike(b.left + 2, b.top + 1, if (big) 5 else 4, 3, 'b', 'p')
    override fun PetCanvas.front() { g.set(c - 2, my + 1, 'h'); g.set(c + 1, my + 1, 'h') } // little fangs
}

internal class Parrot : PetModel(Species.PARROT, Color(0xFF5CC46A), Color(0xFF2F8A44), Color(0xFFF4E27A)) {
    override val build = 0.8f to 1.04f
    override val blush = false
    override fun PetCanvas.behind() { // folded wings and a long tail feather
        val wy = cy - 1
        wings(if (big) 2 else 1) { i -> wy..wy + 3 + i }
        g.rect(b.cx.toInt() - 1, b.bottom - 1, b.cx.toInt(), b.bottom + (if (grown) 2 else 1), 'r')
    }
    override fun PetCanvas.onTop() = g.spike(b.cx.toInt() - 1, b.top + 1, if (big) 4 else 3, 2, 'r') // crest
    override fun PetCanvas.markings() = g.ellipse(b.cx, b.cy + b.ry * 0.2f, b.rx * 0.6f, b.ry * 0.7f, 'l', 'b')
    override fun PetCanvas.mouth() = beak(hooked = true)
}

internal class Penguin : PetModel(Species.PENGUIN, Color(0xFF3E4C6E), Color(0xFF232B40), Color(0xFFF7F7F2)) {
    override val build = 0.78f to 1.12f
    override val eyes = EyeStyle.BEADY
    override val blush = false
    override fun PetCanvas.markings() = g.ellipse(b.cx, b.cy + b.ry * 0.15f, b.rx * 0.72f, b.ry * 0.8f, 'l', 'b')
    override fun PetCanvas.mouth() = beak(hooked = false)
    override fun PetCanvas.front() { if (grown) g.mirror(b.left, cy + 1, 'd') } // flippers
}

internal class Eagle : PetModel(Species.EAGLE, Color(0xFF7A5434), Color(0xFF4A3220), Color(0xFFFAF7F0)) {
    override val build = 0.84f to 1.08f
    override val eyes = EyeStyle.SLIT
    override val blush = false
    override fun handDrawn(stage: Stage) = when (stage) {
        Stage.BABY -> BABY; Stage.KID -> KID; Stage.TEEN -> TEEN; else -> ADULT
    }
    override fun PetCanvas.mouth() = beak(hooked = true)

    private companion object {
        val BABY = listOf(
            "....................",
            "....................",
            "....................",
            "....................",
            "........o..o........",
            ".......ohooho.......",
            "......oohhhhoo......",
            "....oohhhhhhhhoo....",
            "...ohhhhhhhhhhhho...",
            "...ohhEEhhhhEEhho...",
            "...ohhEwoyyowEhho...",
            "...ohhhhoyyohhhho...",
            "..odhhhhheehhhhhdo..",
            "..odhhllllllllhhdo..",
            "..oddhllllllllhddo..",
            "...odhllllllllhdo...",
            "...ohhllllllllhho...",
            "....oohhhhhhhhoo....",
            "....oyyooooooyyo....",
            "....................",
        )
        val KID = listOf(
            "....................",
            "....................",
            "....................",
            ".......oooooo.......",
            ".....oobbbbbboo.....",
            "....obblbbbblbbo....",
            "...obbbbbllbbbbbo...",
            "...odddbbbbbbdddo...",
            "...obbEEoyyoEEbbo...",
            "...obbEwoyyowEbbo...",
            "..oobbbboyyobbbboo..",
            ".oddobbboeeobbboddo.",
            ".odddoblbhhblbodddo.",
            "odbddobhbbbbhboddbdo",
            "oddbdbbbhbbhbbbdbddo",
            ".odbdbbhbbbbhbbdbdo.",
            ".ododbbbbbbbbbbdodo.",
            "..o.oobbllllbboo.o..",
            "....oyyoollooyyo....",
            "...oyoyooooooyoyo...",
        )
        val TEEN = listOf(
            "....................",
            "....................",
            "......oooooooo......",
            "....oolllllllloo....",
            "...ollbllbbllbllo...",
            "..olllbllbbllblllo..",
            "..oldddlllllldddlo..",
            "..ollEEoyyyyoEEllo..",
            "..ollEwoyyyyowEllo..",
            ".oobllbloyyolbllboo.",
            "oddolllloeeolllloddo",
            "odddoblbboobblbodddo",
            "oddbdobbbbbbbbodbddo",
            "odbdbdbhbbbbhbdbdbdo",
            "oddbdbbbhbbhbbbdbddo",
            ".odbdbbbbbbbbbbdbdo.",
            ".ododbdbbbbbbdbdodo.",
            "..o.oobboooobboo.o..",
            "....oyyolllloyyo....",
            "...oyoyoollooyoyo...",
        )
        val ADULT = listOf(
            "....................",
            "....................",
            "......oooooooo......",
            "....oolllllllloo....",
            "...ollllllllllllo...",
            "..ollllllllllllllo..",
            "..oldddlllllldddlo..",
            "..ollEEoyyyyoEEllo..",
            "..ollEwoyyyyowEllo..",
            ".oollllloyyollllloo.",
            "oddolllloeeolllloddo",
            "odddoblbboobblbodddo",
            "oddbdobbbbbbbbodbddo",
            "odbdbdbhbbbbhbdbdbdo",
            "oddbdbbbhbbhbbbdbddo",
            ".odbdbbbbbbbbbbdbdo.",
            ".ododbdbbbbbbdbdodo.",
            "..o.oobboooobboo.o..",
            "....oyyolllloyyo....",
            "...oyoyoollooyoyo...",
        )
    }
}

internal class Flamingo : PetModel(Species.FLAMINGO, Color(0xFFF59AB5), Color(0xFFD9668C), Color(0xFFFFD6E2)) {
    override val build = 0.8f to 1.1f
    override val eyes = EyeStyle.BEADY
    override val blush = false
    override fun handDrawn(stage: Stage) = if (stage == Stage.BABY) BABY else ADULT

    private companion object {
        val BABY = listOf(
            "....................",
            "....................",
            "....................",
            "....................",
            "....................",
            ".......oooo.........",
            "......ollllo........",
            "......olEllhoo......",
            "......ollllhheo.....",
            ".......ollooo.......",
            "......olllloooo.....",
            ".....ollllllllloo...",
            ".....olllhhhllllo...",
            ".....ollhhhhhllllo..",
            ".....olllhhhlllllo..",
            "......olllllllllo...",
            ".......oollllloo....",
            "........od..od......",
            "........od..od......",
            ".......odd.odd......",
        )
        val ADULT = listOf(
            "....................",
            "......oooo..........",
            ".....obbbbo.........",
            ".....obEbbhoo.......",
            ".....obbbbhhho......",
            "......oobooeeo......",
            "........obo.oo......",
            ".......obo..........",
            "......obo...........",
            "......obo...........",
            "......obboooooo.....",
            ".......obbbbbbboooo.",
            "......obbbdddbbbddo.",
            "......obbdddddbbbddo",
            "......obbbdddbbbbbo.",
            ".......obbbbbbbbbo..",
            "........oobbbboooo..",
            "..........od.oddo...",
            "..........od...o....",
            ".........oddd.......",
        )
    }
}
