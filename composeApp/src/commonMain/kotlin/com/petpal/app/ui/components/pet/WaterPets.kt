package com.petpal.app.ui.components.pet

import androidx.compose.ui.graphics.Color
import com.petpal.app.domain.model.Species
import com.petpal.app.domain.model.Stage

/** Side-on: patterned shell over a head, legs and tail. */
internal class Turtle : PetModel(Species.TURTLE, Color(0xFF8FCFB0), Color(0xFF7B5A3A), Color(0xFFE9F2C8)) {
    override fun art(stage: Stage) = if (stage == Stage.BABY) BABY else ADULT

    private companion object {
        val BABY = listOf(
            "....................",
            "....................",
            "....................",
            "....................",
            "....................",
            "....................",
            "....................",
            ".......ooooo........",
            ".....oodhdhdoo......",
            "....odhhdhhhddo.....",
            "...odddhhhdhhddo....",
            "..oooddddddddooo....",
            ".obbbooooooooobbo...",
            ".obEbbbllllllbbbo...",
            ".obEwbbbbbbbbbbbo...",
            ".obbbmo.obbo.obbo...",
            "..oooo..oooo.oooo...",
            "....................",
            "....................",
            "....................",
        )
        val ADULT = listOf(
            "....................",
            "....................",
            "....................",
            "....................",
            "....................",
            ".......oooooo.......",
            ".....oodddddoo......",
            "....odhdddhdddo.....",
            "...odhhdhhhdhhdo....",
            "..oddhdhhhhhdhddo...",
            "..odhhhdddddhhhdo...",
            ".oooddddhhhddddooo..",
            "obbboooooooooooobbo.",
            "obEbbbbllllllbbbbo..",
            "obEwbbbbbbbbbbbbbbo.",
            "obbbboooobbbbbo.obo.",
            ".oooo.oobbo.obbo.o..",
            "......obbo...obbo...",
            "......oooo...oooo...",
            "....................",
        )
    }
}

/** Bulging eyes on top and a wide smile. */
internal class Frog : PetModel(Species.FROG, Color(0xFF8BD06A), Color(0xFF4E9A3E), Color(0xFFF0F2B0)) {
    override fun art(stage: Stage) = if (stage == Stage.BABY) BABY else ADULT

    private companion object {
        val BABY = listOf(
            "....................",
            "....................",
            "....................",
            "....................",
            "....................",
            "...oooo......oooo...",
            "..obbbbo....obbbbo..",
            "..obEEbo....obEEbo..",
            "..obEwboooooobwEbo..",
            ".obbbbbbbbbbbbbbbbo.",
            ".obcbbbbbbbbbbbbcbo.",
            ".obbmbbbbbbbbbbmbbo.",
            "..obbmmmmmmmmmmbbo..",
            "..obbbllllllllbbbo..",
            ".obdbbllllllllbbdbo.",
            ".obbbbllllllllbbbbo.",
            ".oobbbobbbbbbobbboo.",
            ".oooooooooooooooooo.",
            "....................",
            "....................",
        )
        val ADULT = listOf(
            "....................",
            "....................",
            "..oooo........oooo..",
            ".obbbbo......obbbbo.",
            ".obEEbo......obEEbo.",
            ".obEwbo......obwEbo.",
            "obbbbbboooooobbbbbbo",
            "obbbbbbbbbbbbbbbbbbo",
            "obcbbbbbbbbbbbbbbcbo",
            "obbmbbbbbbbbbbbbmbbo",
            ".obbmmmmmmmmmmmmbbo.",
            "..obbbbbbbbbbbbbbo..",
            ".obbbbllllllllbbbbo.",
            ".obdbbllllllllbbdbo.",
            "obbbbbllllllllbbbbbo",
            "obdbbbllllllllbbbdbo",
            "obbbdbbbbbbbbbbdbbbo",
            "oobbbbobbbbbbobbbboo",
            "obobboobboobboobbobo",
            "oooooo.oooooo.oooooo",
        )
    }
}

/** Pink, with feathery gills fanning out both sides. */
internal class Axolotl : PetModel(Species.AXOLOTL, Color(0xFFF6B8CC), Color(0xFFE07A9E), Color(0xFFFFE4EC)) {
    override fun art(stage: Stage) = if (stage == Stage.BABY) BABY else ADULT

    private companion object {
        val BABY = listOf(
            "....................",
            "....................",
            "....................",
            "....................",
            "....................",
            "....................",
            "o..................o",
            "doo..oooooooooo..ood",
            ".odoobbbbbbbbbboodo.",
            "o.odbbbbbbbbbbbbdo.o",
            "doodbEEbbbbbbEEbdood",
            ".oddbEwbbbbbbwEbddo.",
            "..obcbbbbbbbbbbcbo..",
            "..obbbmbbbbbbmbbbo..",
            "...obbbmmmmmmbbbo...",
            "....obbbbbbbbbbo....",
            "....obllllllllbo....",
            "....oooooooooooo....",
            "....................",
            "....................",
        )
        val ADULT = listOf(
            "....................",
            "....................",
            "....................",
            "o..................o",
            "dooo............oood",
            ".oddo.oooooooo.oddo.",
            "o.odobbbbbbbbbbodo.o",
            "doodbbbbbbbbbbbbdood",
            ".oddbbbbbbbbbbbbddo.",
            "o.odbEEbbbbbbEEbdo.o",
            "doodbEwbbbbbbwEbdood",
            ".oddbbbbbbbbbbbbddo.",
            "..obcbbbbbbbbbbcbo..",
            "..obbbmbbbbbbmbbbo..",
            "...obbbmmmmmmbbbo...",
            "....obbbbbbbbbbo....",
            "...obbllllllllbbo...",
            "...obbllllllllbbo...",
            "...odbbbbbbbbbbdo...",
            "....oooooooooooo....",
        )
    }
}

/** Side-on: dorsal fin, toothy grin and a forked tail. */
internal class Shark : PetModel(Species.SHARK, Color(0xFF6E8FB0), Color(0xFF3F5C7A), Color(0xFFF2F5F8)) {
    override fun art(stage: Stage) = if (stage == Stage.BABY) BABY else ADULT

    private companion object {
        val BABY = listOf(
            "....................",
            "....................",
            "....................",
            "....................",
            "....................",
            "....................",
            "..........o.........",
            ".........obo........",
            "......oooobo....oo..",
            "....oobbbbbboo.obo..",
            "...obwEbbbbbbbobbo..",
            "..obbEEbbbbbbbbbo...",
            "..obbbbbbbbbbbbbbo..",
            "..olllllllllllobbo..",
            "...ohwhwllllloo.oo..",
            "....ooolllldoo......",
            ".......oooodo.......",
            "..........oo........",
            "....................",
            "....................",
        )
        val ADULT = listOf(
            "....................",
            "....................",
            "....................",
            "....................",
            ".........oo.........",
            "........obo.........",
            ".......obbo.........",
            ".....ooobbo......oo.",
            "...oobbbbbbooo..obo.",
            "..obbbbbbbbbbboobbo.",
            ".obwEbbbbbbbbbbbbbo.",
            "obbEEbbbbbbdbbbbbo..",
            "obbbbbbbbbbbbbbbbbo.",
            "ollllllllllllllobbbo",
            ".ohwhwhwlllllloo.obo",
            "..oolllllllldooo..oo",
            "....oooooooodo......",
            "...........ooo......",
            "....................",
            "....................",
        )
    }
}

/** Grey seal lying down with a raised tail; the baby is a white pup. */
internal class Seal : PetModel(Species.SEAL, Color(0xFFB8C2CC), Color(0xFF6E7C8A), Color(0xFFEEF2F5)) {
    override fun art(stage: Stage) = if (stage == Stage.BABY) BABY else ADULT

    private companion object {
        val BABY = listOf(
            "....................",
            "....................",
            "....................",
            "....................",
            "....................",
            "....................",
            "....................",
            "....oooooo..........",
            "...ohhhhhho.........",
            "..ohhhhhhhho........",
            "..ohEEhhEEho........",
            "..ohEwhhEwho........",
            "..ohhhheehhho.......",
            "..ohhhhmmhhhoooo....",
            "..ohhhhhhhhhhhhhoo..",
            ".ohhhhhhhhhhhhhhhooo",
            ".ooohhhhhhhhhhhhohho",
            "....oooooooooooooo..",
            "....................",
            "....................",
        )
        val ADULT = listOf(
            "....................",
            "....................",
            "....................",
            "....................",
            "....................",
            "....oooooo..........",
            "...obbbbbbo.........",
            "..obbbbbbbbo........",
            "..obEEbbEEbo........",
            "..obEwbbEwbo........",
            ".obbbbeebbbbo.......",
            ".obllbmmbllbo.......",
            ".obllllllllbo.......",
            "..obbbbbbbbbooo.....",
            "..obbbbbbbbbbbboo...",
            ".obdbbbbbbbbbbbbbo..",
            "obdobllllllbbbbbbooo",
            "ooo.ollllllllbbbobdo",
            ".....ooooooooooooodo",
            ".................oo.",
        )
    }
}

/** Side-on fish with a spiny dorsal fin and banded body. */
internal class Tilapia : PetModel(Species.TILAPIA, Color(0xFF8FA8A0), Color(0xFF4F6B66), Color(0xFFE6EEDF)) {
    override fun art(stage: Stage) = if (stage == Stage.BABY) BABY else ADULT

    private companion object {
        val BABY = listOf(
            "....................",
            "....................",
            "....................",
            "....................",
            "....................",
            "....................",
            "........o.o.o.......",
            ".......ododod.......",
            ".....oobdbdbdo..oo..",
            "....obbbbbbbbbo.odo.",
            "...obwEbbdbbdbboddo.",
            "...obEEbbbbbbbbbddo.",
            "..ombbbbbbbbbbbbddo.",
            "...obllllllllbbbodo.",
            "....oollllllbbo..oo.",
            "......oooooooo......",
            "....................",
            "....................",
            "....................",
            "....................",
        )
        val ADULT = listOf(
            "....................",
            "....................",
            "....................",
            "....................",
            ".......o.o.o.o......",
            "......odododod......",
            ".....oodbdbdbdoo....",
            "...oobbbbbbbbbbbo.oo",
            "..obbbbdbbbdbbbbbodo",
            ".obwEbbbdbbbdbbbbddo",
            ".obEEbbdbbbdbbbbbddo",
            "obbbbbbbbbbbbbbbbbdo",
            "ombbbllllllllllbbddo",
            ".oblllllllllllbbbodo",
            "..oolllllllllbbo..oo",
            "....ooodoooddoo.....",
            "......odo..odo......",
            ".......o....o.......",
            "....................",
            "....................",
        )
    }
}

/** Raised claws, eyes on stalks and little legs. */
internal class Crab : PetModel(Species.CRAB, Color(0xFFE8634A), Color(0xFFA83A2A), Color(0xFFF6C7A8)) {
    override fun art(stage: Stage) = if (stage == Stage.BABY) BABY else ADULT

    private companion object {
        val BABY = listOf(
            "....................",
            "....................",
            "....................",
            "....................",
            "....................",
            "....................",
            "..ooo..........ooo..",
            ".obbbo........obbbo.",
            ".obooo.oo..oo.ooobo.",
            ".obbo.oEwoowEo.obbo.",
            "..obo.oEEooEEo.obo..",
            "...ooooobbbbooooo...",
            "..obbbbbbbbbbbbbbo..",
            ".obcbbbbbbbbbbbbcbo.",
            ".obbbbbbmmmmbbbbbbo.",
            ".obllllllllllllllbo.",
            "..oooooooooooooooo..",
            "..obo.obo..obo.obo..",
            "..oo..oo....oo..oo..",
            "....................",
        )
        val ADULT = listOf(
            "....................",
            "....................",
            ".oooo..........oooo.",
            "obbbbo........obbbbo",
            "obo.oo........oo.obo",
            "obboo..oo..oo..oobbo",
            ".obbo.oEwoowEo.obbo.",
            "..obo.oEEooEEo.obo..",
            "..obo..oboobo..obo..",
            "...ooooobbbbooooo...",
            "..obbbbbbbbbbbbbbo..",
            ".obbbbbbbbbbbbbbbbo.",
            "obbcbbbbbbbbbbbbcbbo",
            "obbbbbbmbbbbmbbbbbbo",
            "obbbbbbbmmmmbbbbbbbo",
            ".obllllllllllllllbo.",
            "oboooooooooooooooobo",
            "obo.obo.obbo.obo.obo",
            "oo..oo..oooo..oo..oo",
            "....................",
        )
    }
}

/** Side-on: long low body, spotted back and four legs. */
internal class Lizard : PetModel(Species.LIZARD, Color(0xFF7FB85A), Color(0xFF4B7A33), Color(0xFFF2D08A)) {
    override fun art(stage: Stage) = if (stage == Stage.BABY) BABY else ADULT

    private companion object {
        val BABY = listOf(
            "....................",
            "....................",
            "....................",
            "....................",
            "....................",
            "....................",
            "....................",
            "....................",
            "....oooo............",
            "...obbbboo..........",
            "..obwEbbbbo.........",
            "..obEEbbdbbooooo....",
            ".obbbbbbbbbbbbbbo...",
            ".ombbllbbdbbbbbbbo..",
            "..obllllllllbbbbbo..",
            "...ooooooooooobdbo..",
            "....obo.obo.obo.oo..",
            "....ooo.ooo.ooo.....",
            "....................",
            "....................",
        )
        val ADULT = listOf(
            "....................",
            "....................",
            "....................",
            "....................",
            "....................",
            "....................",
            "....oooo............",
            "...obbbbooo.........",
            "..obwEbbbbbo........",
            ".obbEEbbdbdbooooo...",
            "obbbbbbbbbbbbbbbbo..",
            "ombbbllbbdbbdbbbbbo.",
            ".obbllllllllllbbbbbo",
            "..ooooooooooooobdbbo",
            "...obo.obo...obo.obo",
            "...ooo.ooo...ooo.oo.",
            "................oo..",
            "....................",
            "....................",
            "....................",
        )
    }
}

/** Side-on on a branch: casque head, turret eye and ridged back. */
internal class Chameleon : PetModel(Species.CHAMELEON, Color(0xFF7ACB5E), Color(0xFF3F8A3A), Color(0xFFE8F2A0)) {
    override fun art(stage: Stage) = if (stage == Stage.BABY) BABY else ADULT

    private companion object {
        val BABY = listOf(
            "....................",
            "....................",
            "....................",
            "....................",
            "....................",
            "....................",
            "....................",
            "....oooo............",
            "...obbbbdoo.........",
            "..obooobbbdoooooo...",
            "..obowEobbbbdbdbdo..",
            "..obEEEobbbbbbbbbbo.",
            ".obbooobbdbbbdbbbbo.",
            ".ombbbbbllllllbbbo..",
            "..oooooobooobooooo..",
            "...nnnnobonobonnnn..",
            "...nnnnnoonnoonnnn..",
            "....................",
            "....................",
            "....................",
        )
        val ADULT = listOf(
            "....................",
            "....................",
            "....................",
            "....................",
            "....ooooo...........",
            "...obbbbdoo.........",
            "..obbbbbbddooooooo..",
            ".obooobbbbbdbdbdbdo.",
            ".obowEobbbbbbbbbbbbo",
            ".obEEEobbbdbbdbbdbbo",
            "obbooobbbbbbbbbbbbbo",
            "ombbbbbblllllllbbbbo",
            ".ollllllllllllllbbo.",
            "..ooooobooooobooooo.",
            "....nnobonnnnobonnnn",
            "....nnnoonnnnnoonnnn",
            "....................",
            "....................",
            "....................",
            "....................",
        )
    }
}
