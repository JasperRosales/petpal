package com.petpal.app.ui.components.pet

import androidx.compose.ui.graphics.Color
import com.petpal.app.domain.model.Species
import com.petpal.app.domain.model.Stage

/** Little dragon with horns and bat-like wings. */
internal class Drake : PetModel(Species.DRAKE, Color(0xFF7CC98A), Color(0xFF3E8A55), Color(0xFFF3E3A6)) {
    override fun art(stage: Stage) = if (stage == Stage.BABY) BABY else ADULT

    private companion object {
        val BABY = listOf(
            "....................",
            "....................",
            "....................",
            "....................",
            "....................",
            "....oo........oo....",
            "....ohooooooooho....",
            "...obbbbbbbbbbbbo...",
            "..obbbbbbbbbbbbbbo..",
            "..obEEbbbbbbbbEEbo..",
            "..obEwbbbbbbbbwEbo..",
            "o.obbbblleellbbbbo.o",
            "do.obbbllmmllbbbo.od",
            "ddooobbbbbbbbbbooodd",
            "dddoobllllllllbooddd",
            ".ddoobllllllllboodd.",
            "..o.obbbbbbbbbbo.o..",
            "....oooooooooooo....",
            "....................",
            "....................",
        )
        val ADULT = listOf(
            "....................",
            "...o............o...",
            "...oho........oho...",
            "....oho......oho....",
            ".....oo..oo..oo.....",
            "....obbbobbobbbo....",
            "...obbbbbbbbbbbbo...",
            "..obbbbbbbbbbbbbbo..",
            "..obEEbbbbbbbbEEbo..",
            "..obEwbbbbbbbbwEbo..",
            "..obbbbbbbbbbbbbbo..",
            "..obbbllleelllbbbo..",
            "o..obbblmmmmlbbbo..o",
            "do..oobbboobbboo..od",
            "ddo.obllllllllbo.odd",
            "dddoobllllllllbooddd",
            "ddddobllllllllbodddd",
            ".dddobbbbbbbbbboddd.",
            "..odobbobbbbobbodo..",
            "...oooooooooooooo...",
        )
    }
}

/** Ear tufts, a pale facial disc and spotted chest. */
internal class Owl : PetModel(Species.OWL, Color(0xFFA9825A), Color(0xFF6E5034), Color(0xFFF1E2C2)) {
    override fun art(stage: Stage) = if (stage == Stage.BABY) BABY else ADULT

    private companion object {
        val BABY = listOf(
            "....................",
            "....................",
            "....................",
            "....................",
            "....................",
            "...o............o...",
            "...odoooooooooodo...",
            "...obbbbbbbbbbbbo...",
            "..obhhhbbbbbbhhhbo..",
            "..ohhEEhbbbbhEEhho..",
            "..ohhEwhbbbbhwEhho..",
            "..obhhhhbyybhhhhbo..",
            "..odbbbbbyybbbbbdo..",
            "..odblbbbllbbblbdo..",
            "..odbbbblbblbbbbdo..",
            "...odbbbbbbbbbbdo...",
            "....oooyyooyyooo....",
            "....................",
            "....................",
            "....................",
        )
        val ADULT = listOf(
            "....................",
            "..o..............o..",
            "..odo..........odo..",
            "..obdoooooooooodbo..",
            "..obbbbbbbbbbbbbbo..",
            ".obhhhbbbbbbbbhhhbo.",
            ".ohhhhhbbbbbbhhhhho.",
            ".ohhEEhhbbbbhhEEhho.",
            ".ohhEwhhbbbbhhwEhho.",
            ".obhhhhhbyybhhhhhbo.",
            ".odbbbbbbyybbbbbbdo.",
            ".odblblbbbbbblblbdo.",
            "odbbbbbbbllbbbbbbbdo",
            "odbblblbllllblblbbdo",
            "odbbbbbbbbbbbbbbbbdo",
            "odblblblbllblblblbdo",
            ".odbbbbbbbbbbbbbbdo.",
            "..odbbbbbbbbbbbbdo..",
            "...oooyyooooyyooo...",
            "....................",
        )
    }
}

/** Big pointed ears and wide membrane wings. */
internal class Bat : PetModel(Species.BAT, Color(0xFF8A6FA8), Color(0xFF4E3A66), Color(0xFFD8C8E8)) {
    override fun art(stage: Stage) = if (stage == Stage.BABY) BABY else ADULT

    private companion object {
        val BABY = listOf(
            "....................",
            "....................",
            "....................",
            "....................",
            "....................",
            "....o..........o....",
            "....obo......obo....",
            "....opboooooobpo....",
            "o...obbbbbbbbbbo...o",
            "do..oEEbbbbbbEEo..od",
            "ddo.oEwbbbbbbwEo.odd",
            "dddoobbbbbbbbbbooddd",
            "ddddobbbmhhmbbbodddd",
            ".dddoobbllllbbooddd.",
            "..do..obllllbo..od..",
            "...o...obbbbo...o...",
            "........oooo........",
            "....................",
            "....................",
            "....................",
        )
        val ADULT = listOf(
            "....................",
            "....................",
            "....o..........o....",
            "....obo......obo....",
            "....opbo....obpo....",
            "o...opboooooobpo...o",
            "do..obbbbbbbbbbo..od",
            "ddo.obbbbbbbbbbo.odd",
            "dddoobEEbbbbEEbooddd",
            "ddddobEwbbbbwEbodddd",
            "dddddobbbbbbbboddddd",
            "ddddddbbbmmbbbdddddd",
            "ddddddobbmmbbodddddd",
            "dddodddobbbbodddoddd",
            ".do.oddobllboddo.od.",
            ".o...odobllbodo...o.",
            "......oobbbboo......",
            ".......oboobo.......",
            ".......oo..oo.......",
            "....................",
        )
    }
}

/** Side-on on a branch: hooked beak, red crown, yellow chest, blue flight feathers. */
internal class Parrot : PetModel(Species.PARROT, Color(0xFF5CC46A), Color(0xFF2F8A44), Color(0xFFF4E27A)) {
    override fun art(stage: Stage) = if (stage == Stage.BABY) BABY else ADULT

    private companion object {
        val BABY = listOf(
            "....................",
            "....................",
            "....................",
            "....................",
            "....................",
            ".........oooo.......",
            "........orrbbo......",
            ".......owwbbbbo.....",
            "......ohwEbbbbbo....",
            ".....ohhwwbbbbbo....",
            ".....ohoobbbbbbo....",
            "......oooyybbddo....",
            ".......oyyyybddo....",
            ".......oyyyybduo....",
            ".......olyyybduo....",
            "........olyybbdoo...",
            ".........oyoyoodro..",
            "....oooooooooooooo..",
            "...onnnnnnnnnnnnno..",
            "....ooooooooooooo...",
        )
        val ADULT = listOf(
            "....................",
            "........oooo........",
            ".......orrrroo......",
            "......orrbbbbbo.....",
            ".....oohwwbbbbbo....",
            "....ohhhwEbbbbbo....",
            "...ohhhowwbbbbbo....",
            "...ohoohbbbbbbbo....",
            "...oho.oyybbbbddo...",
            "....o..oyyybbdddo...",
            ".......oyyyybdddo...",
            "......oyyyyybduddo..",
            "......oyyyyybduudo..",
            "......olyyyybduudo..",
            ".......olyyybbduuo..",
            "....oooooyoyooduro..",
            "...onnnnnnnnnnodro..",
            "....oooooooooo.oro..",
            "...............oro..",
            "................o...",
        )
    }
}

/** Black back, white front and orange beak and feet. */
internal class Penguin : PetModel(Species.PENGUIN, Color(0xFF3E4C6E), Color(0xFF232B40), Color(0xFFF7F7F2)) {
    override fun art(stage: Stage) = if (stage == Stage.BABY) BABY else ADULT

    private companion object {
        val BABY = listOf(
            "....................",
            "....................",
            "....................",
            "....................",
            "....................",
            ".......oooooo.......",
            ".....oobbbbbboo.....",
            "....obbbbbbbbbbo....",
            "...obbhhhhhhhhbbo...",
            "...obhEEhhhhEEhbo...",
            "...obhEwhyyhwEhbo...",
            "..obbhhhyyyyhhhbbo..",
            ".obobhllllllllhbobo.",
            ".oo.ollllllllllo.oo.",
            "....ollllllllllo....",
            "....obllllllllbo....",
            ".....obbbbbbbbo.....",
            "....oyyyooooyyyo....",
            "....oooo....oooo....",
            "....................",
        )
        val ADULT = listOf(
            "....................",
            "......oooooooo......",
            ".....obbbbbbbbo.....",
            "....obbbbbbbbbbo....",
            "...obbbbbbbbbbbbo...",
            "...obbhhhhhhhhbbo...",
            "...obhEEhhhhEEhbo...",
            "...obhEwhhhhwEhbo...",
            "...obhhhhyyhhhhbo...",
            "..obbhhhyyyyhhhbbo..",
            "..obbhllllllllhbbo..",
            ".obobhllllllllhbobo.",
            ".obobllllllllllbobo.",
            "obbobllllllllllbobbo",
            "obo.ollllllllllo.obo",
            "oo..ollllllllllo..oo",
            "....obllllllllbo....",
            ".....obbbbbbbbo.....",
            "....oyyyooooyyyo....",
            "....oooo....oooo....",
        )
    }
}

/** Bald eagle: white head, hooked yellow beak, brown wings; a fluffy chick as a baby. */
internal class Eagle : PetModel(Species.EAGLE, Color(0xFF7A5434), Color(0xFF4A3220), Color(0xFFFAF7F0)) {
    override fun art(stage: Stage) = when (stage) {
        Stage.BABY -> BABY; Stage.KID -> KID; Stage.TEEN -> TEEN; else -> ADULT
    }

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

/** S-necked, with a bent black-tipped beak, standing on one leg. */
internal class Flamingo : PetModel(Species.FLAMINGO, Color(0xFFF59AB5), Color(0xFFD9668C), Color(0xFFFFD6E2)) {
    override fun art(stage: Stage) = if (stage == Stage.BABY) BABY else ADULT

    private companion object {
        val BABY = listOf(
            "....................",
            "....................",
            "....................",
            "....................",
            "......oooo..........",
            ".....ollllo.........",
            "....ollEllo.........",
            "..oohhllllo.........",
            "..oeeooollo.........",
            "...oo..ollo.........",
            ".......ollo.........",
            "......ollllooooo....",
            "......olllllllllo...",
            ".....ollllhhhlllloo.",
            ".....olllhhhhhlllo..",
            "......ollllllllloo..",
            ".......oooooooooo...",
            ".........ododdo.....",
            ".........odooo......",
            "........oddo........",
        )
        val ADULT = listOf(
            ".......oooo.........",
            "......obbbbo........",
            ".....obbEbbo........",
            "...oohhbbbbo........",
            "..ohhhoobbo.........",
            "..oeeo.obbo.........",
            "..oeo.obbo..........",
            "...o..obo...........",
            ".....obbo...........",
            ".....obo............",
            ".....obbo...........",
            "......obboooooo.....",
            ".....obbbbbbbbbooo..",
            ".....obbbbbdddddddo.",
            "......obbbbbddeeeo..",
            ".......ooooodooooo..",
            "..........ododdddo..",
            "..........odoooodo..",
            "..........odo..oo...",
            ".........oddo.......",
        )
    }
}
