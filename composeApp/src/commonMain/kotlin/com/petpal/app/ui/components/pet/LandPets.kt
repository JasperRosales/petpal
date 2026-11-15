package com.petpal.app.ui.components.pet

import androidx.compose.ui.graphics.Color
import com.petpal.app.domain.model.Species
import com.petpal.app.domain.model.Stage
import com.petpal.app.ui.theme.SpriteCream

/** Leafy seedling; the grown form wears a flower. */
internal class Sprout : PetModel(Species.SPROUT, Color(0xFFB9E07A), Color(0xFF6FAE4E), SpriteCream) {
    override fun art(stage: Stage) = if (stage == Stage.BABY) BABY else ADULT

    private companion object {
        val BABY = listOf(
            "....................",
            "....................",
            "....................",
            "....................",
            "....................",
            ".......oooooo.......",
            "......oggooggo......",
            "......oggooggo......",
            ".......ooggoo.......",
            "....ooooobbooooo....",
            "...obbbbbbbbbbbbo...",
            "..obbbbbbbbbbbbbbo..",
            "..obEEbbbbbbbbEEbo..",
            "..obEwbbbbbbbbwEbo..",
            "..obcbbbbmmbbbbcbo..",
            "..obbbllllllllbbbo..",
            "...obbllllllllbbo...",
            "...odbbbbbbbbbbdo...",
            "....ooooddddoooo....",
            "........oooo........",
        )
        val ADULT = listOf(
            "........oooo........",
            "....oo.opyypo.oo....",
            "...oggoopyypooggo...",
            "...oggo.oggo.oggo...",
            "....oggooggooggo....",
            "....ooggoggoggoo....",
            "......oooooooo......",
            "...oooobbbbbboooo...",
            "..obbbbbbbbbbbbbbo..",
            ".obbbbbbbbbbbbbbbbo.",
            ".obbbbbbbbbbbbbbbbo.",
            "obbbEEbbbbbbbbEEbbbo",
            "obbbEwbbbbbbbbwEbbbo",
            "obbcbbbbbbbbbbbbcbbo",
            "obbbbbbbmmmmbbbbbbbo",
            ".obbbbllllllllbbbbo.",
            ".odbbbllllllllbbbdo.",
            "..odbbbbbbbbbbbbdo..",
            "...oodooddddoodoo...",
            ".....oo.oooo.oo.....",
        )
    }
}

/** Cat with pointy ears, striped forehead and a curled tail. */
internal class Kitty : PetModel(Species.KITTY, Color(0xFFF0A868), Color(0xFFC97B3E), SpriteCream) {
    override fun art(stage: Stage) = if (stage == Stage.BABY) BABY else ADULT

    private companion object {
        val BABY = listOf(
            "....................",
            "....................",
            "....................",
            "....................",
            "....................",
            "...oo..........oo...",
            "...obo........obo...",
            "...opboooooooobpo...",
            "..obbbbbbbbbbbbbbo..",
            "..obbbbbbddbbbbbbo..",
            "..obEEbbbbbbbbEEbo..",
            "..obEwbbbbbbbbwEbo..",
            "..obcbbbbppbbbbcbo..",
            "..obbbbllmmllbbbbo..",
            "...oobbbbbbbbbboo...",
            "...obbllllllllbbo...",
            "...obbllllllllbbo...",
            "...odbbbbbbbbbbdo...",
            "...oooooooooooooo...",
            "....................",
        )
        val ADULT = listOf(
            "....................",
            "..oo............oo..",
            "..obo..........obo..",
            "..opbo........obpo..",
            "..opbboooooooobbpo..",
            "..opbbbbbbbbbbbbpo..",
            ".obbbbbbbddbbbbbbbo.",
            ".obbbbbbbbbbbbbbbbo.",
            ".obbEEbbbbbbbbEEbbo.",
            ".obbEwbbbbbbbbwEbbo.",
            "oobcbbbbbppbbbbbcboo",
            ".obbbbbllmmllbbbbbo.",
            "..obbbllmllmllbbbo..",
            "...oobbbbbbbbbboooo.",
            "...obbllllllllbboodo",
            "..obbbllllllllbbbodo",
            "..obbbllllllllbbodo.",
            "..odbbbbbbbbbbbodo..",
            "..obbobbbbbbbboooo..",
            "..oooooooooooooooo..",
        )
    }
}

/** Floppy-eared puppy with a dark nose. */
internal class Pup : PetModel(Species.PUP, Color(0xFFE3C08D), Color(0xFF9C6B3F), SpriteCream) {
    override fun art(stage: Stage) = if (stage == Stage.BABY) BABY else ADULT

    private companion object {
        val BABY = listOf(
            "....................",
            "....................",
            "....................",
            "....................",
            "....................",
            "....................",
            ".....oooooooooo.....",
            "...oobbbbbbbbbboo...",
            "..oddbbbbbbbbbbddo..",
            ".odddbEEbbbbEEbdddo.",
            ".oddobEwbbbbwEboddo.",
            ".oddobcbllllbcboddo.",
            "..oo.obbleelbbo.oo..",
            ".....oblmllmlbo.....",
            ".....oobbbbbboo.....",
            "....obbllllllbbo....",
            "....obbllllllbbo....",
            "....odbbbbbbbbdo....",
            "....oooooooooooo....",
            "....................",
        )
        val ADULT = listOf(
            "....................",
            "....................",
            "....oooooooooooo....",
            "..oobbbbbbbbbbbboo..",
            ".oddbbbbbbbbbbbbddo.",
            "odddbbbbbbbbbbbbdddo",
            "odddbEEbbbbbbEEbdddo",
            "oddobEwbbbbbbwEboddo",
            "oddobbbbllllbbbboddo",
            ".oobcbllleelllbcboo.",
            "...obbllmllmllbbo...",
            "...obbllllllllbbo...",
            "...obbbbllllbbbbo...",
            "....oobbbbbbbboo....",
            "...obbllllllllbboo..",
            "...obbllllllllbodo..",
            "..obbbllllllllbooo..",
            "..odbbbbbbbbbbbbdo..",
            "..obbobbbbbbbbobbo..",
            "..oooooooooooooooo..",
        )
    }
}

/** Long pink-lined ears and a cotton tail. */
internal class Bunny : PetModel(Species.BUNNY, Color(0xFFE9E1F2), Color(0xFFBDAED6), Color(0xFFFFFBF5)) {
    override fun art(stage: Stage) = if (stage == Stage.BABY) BABY else ADULT

    private companion object {
        val BABY = listOf(
            "....................",
            "....................",
            "....................",
            "....oo........oo....",
            "...obbo......obbo...",
            "...obpbo....obpbo...",
            "...obpbo....obpbo...",
            "...obbboooooobbbo...",
            "..obbbbbbbbbbbbbbo..",
            "..obbbbbbbbbbbbbbo..",
            ".obbEEbbbbbbbbEEbbo.",
            ".obbEwbbbbbbbbwEbbo.",
            ".obcbbbbbppbbbbbcbo.",
            ".obbbbbbmllmbbbbbbo.",
            "..oobbbbbbbbbbbboo..",
            "...obbllllllllbbo...",
            "...obbllllllllbbo...",
            "...obbbbbbbbbbbbo...",
            "...oooooooooooooo...",
            "....................",
        )
        val ADULT = listOf(
            "...oo..........oo...",
            "..obbo........obbo..",
            "..obpbo......obpbo..",
            "..obpbo......obpbo..",
            "..obpbo......obpbo..",
            "..obpboooooooobpbo..",
            "..obbbbbbbbbbbbbbo..",
            ".obbbbbbbbbbbbbbbbo.",
            ".obbbbbbbbbbbbbbbbo.",
            ".obbEEbbbbbbbbEEbbo.",
            ".obbEwbbbbbbbbwEbbo.",
            ".obcbbbbbppbbbbbcbo.",
            ".obbbbbbmllmbbbbbbo.",
            "..oobbbbbbbbbbbboo..",
            "...obbllllllllbbo...",
            "..obbbllllllllbbboo.",
            "..obbbllllllllbbohho",
            "..obbbbbbbbbbbbbohho",
            "..obbobbbbbbbbobboo.",
            "..oooooooooooooooo..",
        )
    }
}

/** Pointed ears, white muzzle and a big white-tipped tail. */
internal class Fox : PetModel(Species.FOX, Color(0xFFE9824A), Color(0xFFB5552A), Color(0xFFFFF4E6)) {
    override fun art(stage: Stage) = if (stage == Stage.BABY) BABY else ADULT

    private companion object {
        val BABY = listOf(
            "....................",
            "....................",
            "....................",
            "....................",
            "..o..............o..",
            "..obo..........obo..",
            "..opboooooooooobpo..",
            "..opbbbbbbbbbbbbpo..",
            ".obbbbbbbbbbbbbbbbo.",
            ".obbEEbbbbbbbbEEbbo.",
            ".obbEwbbbbbbbbwEbbo.",
            ".ohhbbbbbbbbbbbbhho.",
            "..ohhhhhheehhhhhho..",
            "...oohhhhmmhhhhoo...",
            ".....obbbbbbbbobbo..",
            ".....obhhhhhhbobhho.",
            ".....obhhhhhhbohho..",
            ".....odbbbbbbdoo....",
            ".....oooooooooo.....",
            "....................",
        )
        val ADULT = listOf(
            "....................",
            ".o................o.",
            ".obo............obo.",
            ".opbo..........obpo.",
            ".opbboooooooooobbpo.",
            ".opbbbbbbbbbbbbbbpo.",
            "obbbbbbbbbbbbbbbbbbo",
            "obbbbbbbbbbbbbbbbbbo",
            "obbbEEbbbbbbbbEEbbbo",
            "obbbEwbbbbbbbbwEbbbo",
            "ohhbbbbbbbbbbbbbbhho",
            ".ohhhhhhhhhhhhhhhho.",
            "..ohhhhhheehhhhhhooo",
            "...oohhhhmmhhhhoobbb",
            ".....obbbbbbbboobbbb",
            "....obbhhhhhhbobbbbh",
            "....obbhhhhhhobbbhhh",
            "....obbbbbbbbobbhhho",
            "....odbodbbdoboooooo",
            "....oooooooooooo....",
        )
    }
}

/** Round ears and a pale muzzle. */
internal class Bear : PetModel(Species.BEAR, Color(0xFFB07A4F), Color(0xFF6E4A2E), Color(0xFFE8CFA8)) {
    override fun art(stage: Stage) = if (stage == Stage.BABY) BABY else ADULT

    private companion object {
        val BABY = listOf(
            "....................",
            "....................",
            "....................",
            "....................",
            "....................",
            "...ooo........ooo...",
            "..obdboooooooobdbo..",
            "..obbbbbbbbbbbbbbo..",
            "..obbbbbbbbbbbbbbo..",
            ".obbEEbbbbbbbbEEbbo.",
            ".obbEwbbbbbbbbwEbbo.",
            ".obcbbbllllllbbbcbo.",
            ".obbbbblleellbbbbbo.",
            "..oobbbllmmllbbboo..",
            "...obbbbbbbbbbbbo...",
            "..obbbllllllllbbbo..",
            "..obbbllllllllbbbo..",
            "..odddobbbbbbodddo..",
            "...oooooooooooooo...",
            "....................",
        )
        val ADULT = listOf(
            "....................",
            "..ooo..........ooo..",
            ".obbbo........obbbo.",
            ".obdboooooooooobdbo.",
            ".obbbbbbbbbbbbbbbbo.",
            "..obbbbbbbbbbbbbbo..",
            ".obbbbbbbbbbbbbbbbo.",
            ".obbEEbbbbbbbbEEbbo.",
            ".obbEwbbbbbbbbwEbbo.",
            ".obcbbbllllllbbbcbo.",
            ".obbbbllleelllbbbbo.",
            "..obbbbllmmllbbbbo..",
            "..oobbbbbbbbbbbboo..",
            ".obbbbllllllllbbbbo.",
            ".obbbbllllllllbbbbo.",
            "obbbbbllllllllbbbbbo",
            "obbbbbllllllllbbbbbo",
            ".obbbbbbbbbbbbbbbbo.",
            ".odddobbbbbbbbodddo.",
            "..oooooooooooooooo..",
        )
    }
}

/** Branching antlers and a spotted coat. */
internal class Deer : PetModel(Species.DEER, Color(0xFFC98E5A), Color(0xFF8A5A32), Color(0xFFF6E7CF)) {
    override fun art(stage: Stage) = if (stage == Stage.BABY) BABY else ADULT

    private companion object {
        val BABY = listOf(
            "....................",
            "....................",
            "....................",
            "....................",
            "....................",
            "....................",
            "oo................oo",
            "obboooooooooooooobbo",
            ".obbbbbbbbbbbbbbbbo.",
            "..obbbbhbbbbhbbbbo..",
            "..obEEbbbbbbbbEEbo..",
            "..obEwbbbbbbbbwEbo..",
            "..obcbbbllllbbbcbo..",
            "...obbblleellbbbo...",
            "....obbbbbbbbbbo....",
            "....obhbllllbhbo....",
            "....obbbllllbbbo....",
            "....odobboobbodo....",
            "....oooooooooooo....",
            "....................",
        )
        val ADULT = listOf(
            "o.o..............o.o",
            "ono..............ono",
            ".onoo..........oono.",
            "..ono..........ono..",
            "...ono........ono...",
            "oo..ono......ono..oo",
            "obboooooooooooooobbo",
            ".obbbbbbbbbbbbbbbbo.",
            "..obbbbbbbbbbbbbbo..",
            "..obEEbbbbbbbbEEbo..",
            "..obEwbbbbbbbbwEbo..",
            "..obcbbbllllbbbcbo..",
            "...obbblleellbbbo...",
            "....obbllmmllbbo....",
            "....obbbbbbbbbbo....",
            "...obbhllllllhbbo...",
            "...obbhllllllhbbo...",
            "...obbbbbbbbbbbbo...",
            "...odobboddobbodo...",
            "...oooooo..oooooo...",
        )
    }
}

/** Black ears, eye patches, arms and legs. */
internal class Panda : PetModel(Species.PANDA, Color(0xFFF7F5EF), Color(0xFF2B2B2B), Color.White) {
    override fun art(stage: Stage) = if (stage == Stage.BABY) BABY else ADULT

    private companion object {
        val BABY = listOf(
            "....................",
            "....................",
            "....................",
            "....................",
            "....................",
            "...ooo........ooo...",
            "..oeeeooooooooeeeo..",
            "..oeebbbbbbbbbbeeo..",
            "..obbbbbbbbbbbbbbo..",
            ".obeeebbbbbbbbeeebo.",
            ".obeEEbbbbbbbbEEebo.",
            ".obeEwbbbbbbbbwEebo.",
            ".obbeebbeeeebbeebbo.",
            ".obcbbbbbmmbbbbbcbo.",
            "..oobbbbbbbbbbbboo..",
            "..oeeebbbbbbbbeeeo..",
            "..oeebbbbbbbbbbeeo..",
            "..oeeeobeeeeboeeeo..",
            "..oooooooooooooooo..",
            "....................",
        )
        val ADULT = listOf(
            "....................",
            "..ooo..........ooo..",
            ".oeeeooooooooooeeeo.",
            ".oeeebbbbbbbbbbeeeo.",
            ".oeebbbbbbbbbbbbeeo.",
            "..obbbbbbbbbbbbbbo..",
            ".obbeeebbbbbbeeebbo.",
            ".obeeEEbbbbbbEEeebo.",
            ".obeeEwbbbbbbwEeebo.",
            ".obbeebbbbbbbbeebbo.",
            ".obcbbbbeeeebbbbcbo.",
            "..obbbbbbmmbbbbbbo..",
            "...oobbbbbbbbbboo...",
            "..oeeeeeeeeeeeeeeo..",
            ".oeeebbbbbbbbbbeeeo.",
            ".oeebbbbbbbbbbbbeeo.",
            ".oeebbbbbbbbbbbbeeo.",
            "..obbbbbbbbbbbbbbo..",
            "..oeeeobeeeeboeeeo..",
            "..oooooooooooooooo..",
        )
    }
}

/** Bandit mask and a ringed tail. */
internal class Raccoon : PetModel(Species.RACCOON, Color(0xFFA3A3A8), Color(0xFF55555C), Color(0xFFE6E6EA)) {
    override fun art(stage: Stage) = if (stage == Stage.BABY) BABY else ADULT

    private companion object {
        val BABY = listOf(
            "....................",
            "....................",
            "....................",
            "....................",
            "....................",
            "...oo..........oo...",
            "..obpoooooooooopbo..",
            "..obbbbbbbbbbbbbbo..",
            ".obbdddbbhhbbdddbbo.",
            ".obdEEddhhhhddEEdbo.",
            ".obdEwddhhhhddwEdbo.",
            ".obbdddhllllhdddbbo.",
            "..obbbblleellbbbbo..",
            "...oobbllmmllbboo...",
            "....obbbbbbbbbbo....",
            "...obbllllllllbbo...",
            "...obbllllllllbbo...",
            "...odbbodbbdobbdo...",
            "...oooooooooooooo...",
            "....................",
        )
        val ADULT = listOf(
            "....................",
            "..oo............oo..",
            ".obbo..........obbo.",
            ".obpboooooooooobpbo.",
            ".obbbbbbbbbbbbbbbbo.",
            ".obbbbbbbbbbbbbbbbo.",
            "obbbdddbbhhbbdddbbbo",
            "obbdddddbhhbdddddbbo",
            "obdeEEddhhhhddEEedbo",
            "obdeEwddhhhhddwEedbo",
            "obbdddbhllllhbdddbbo",
            ".obbbbblleellbbbbbo.",
            "..obbbbllmmllbbbbo..",
            "...oobbbbbbbbbboo...",
            "...obbllllllllbbo.oo",
            "..obbbllllllllbbbodo",
            "..obbbllllllllbbobo.",
            "..obbbbbbbbbbbbodo..",
            "..odbbodbbbbdooodo..",
            "..oooooooooooooooo..",
        )
    }
}

/** Puffy cheeks and tiny pink paws. */
internal class Hamster : PetModel(Species.HAMSTER, Color(0xFFF2C27A), Color(0xFFB9824A), Color(0xFFFFF4E2)) {
    override fun art(stage: Stage) = if (stage == Stage.BABY) BABY else ADULT

    private companion object {
        val BABY = listOf(
            "....................",
            "....................",
            "....................",
            "....................",
            "....................",
            "....................",
            "...oo..........oo...",
            "..opboooooooooobpo..",
            "..obbbbbbbbbbbbbbo..",
            ".obbbbbbbbbbbbbbbbo.",
            ".obbEEbbbbbbbbEEbbo.",
            ".obbEwbbbbbbbbwEbbo.",
            ".ohcbbbbbppbbbbbcho.",
            ".ohhhhhhhmmhhhhhhho.",
            ".ohhhhhhhhhhhhhhhho.",
            "..ohhhllllllllhhho..",
            "..obllllllllllllbo..",
            "...obbbbbbbbbbbbo...",
            "...oooooooooooooo...",
            "....................",
        )
        val ADULT = listOf(
            "....................",
            "....................",
            "....................",
            "...oo..........oo...",
            "..opbo........obpo..",
            "..obboooooooooobbo..",
            ".obbbbbbbbbbbbbbbbo.",
            ".obbbbbbbbbbbbbbbbo.",
            "obbbEEbbbbbbbbEEbbbo",
            "obbbEwbbbbbbbbwEbbbo",
            "ohhcbbbbbppbbbbbchho",
            "ohhhhhhhhmmhhhhhhhho",
            "ohhhhhhhhhhhhhhhhhho",
            ".ohhhhhhhhhhhhhhhho.",
            ".obhhllllllllllhhbo.",
            ".obbllllllllllllbbo.",
            ".obbllllllllllllbbo.",
            "..obbbbbbbbbbbbbbo..",
            "...opooooppoooopo...",
            "...ooo..oooo..ooo...",
        )
    }
}

/** Fluffy round ears and a big dark nose. */
internal class Koala : PetModel(Species.KOALA, Color(0xFF9EA3AB), Color(0xFF5E636B), Color(0xFFE4E6EA)) {
    override fun art(stage: Stage) = if (stage == Stage.BABY) BABY else ADULT

    private companion object {
        val BABY = listOf(
            "....................",
            "....................",
            "....................",
            "....................",
            "....................",
            "..ooo..........ooo..",
            ".obbbo........obbbo.",
            "obllbboooooooobbllbo",
            "obllbbbbbbbbbbbbllbo",
            ".obbbbbbbbbbbbbbbbo.",
            "..obEEbbbbbbbbEEbo..",
            "..obEwbbeeeebbwEbo..",
            "..obcbbeeeeeebbcbo..",
            "...obbbbbmmbbbbbo...",
            "...oobbbbbbbbbboo...",
            "...obbllllllllbbo...",
            "...obbllllllllbbo...",
            "...odbbodbbdobbdo...",
            "...oooooooooooooo...",
            "....................",
        )
        val ADULT = listOf(
            "....................",
            "..ooo..........ooo..",
            ".obbbo........obbbo.",
            "obllbbo......obbllbo",
            "oblllbboooooobblllbo",
            "obllbbbbbbbbbbbbllbo",
            ".obbbbbbbbbbbbbbbbo.",
            "..obbbbbbbbbbbbbbo..",
            "..obEEbbbbbbbbEEbo..",
            "..obEwbbeeeebbwEbo..",
            "..obcbbeeeeeebbcbo..",
            "...obbbeeeeeebbbo...",
            "...obbbbbmmbbbbbo...",
            "....oobbbbbbbboo....",
            "...obbllllllllbbo...",
            "..obbbllllllllbbbo..",
            "..obbbllllllllbbbo..",
            "..obbbbbbbbbbbbbbo..",
            "..odbbodbbbbdobbdo..",
            "..oooooooooooooooo..",
        )
    }
}

/** Pale heart-shaped face, side ears and a curling tail. */
internal class Monkey : PetModel(Species.MONKEY, Color(0xFF9A6A44), Color(0xFF5E3D24), Color(0xFFF0D2AE)) {
    override fun art(stage: Stage) = if (stage == Stage.BABY) BABY else ADULT

    private companion object {
        val BABY = listOf(
            "....................",
            "....................",
            "....................",
            "....................",
            "....................",
            "......oooooooo......",
            "....oobbbbbbbboo....",
            "..oobbllbbbbllbboo..",
            ".ollobllllllllbollo.",
            ".ollolEEllllEElollo.",
            "..oollEwllllwElloo..",
            "....olllleellllo....",
            "....olllmllmlllo....",
            "....oobbbbbbbboo....",
            "....obllllllllbo....",
            "....obllllllllbo....",
            "....odbbbbbbbbdo....",
            "....oooooooooooo....",
            "....................",
            "....................",
        )
        val ADULT = listOf(
            "....................",
            "....................",
            "......oooooooo......",
            "....oobbbbbbbboo....",
            "...obbbbbbbbbbbbo...",
            ".oobblllbbbblllbboo.",
            "ollobllllllllllbollo",
            "ollolEEllllllEElollo",
            "ollllEwllllllwEllllo",
            ".oolllllllllllllloo.",
            "...ollllleelllllo...",
            "...ollllllllllllo...",
            "....oollmllmlloo.ooo",
            "....oobbbbbbbbooobbb",
            "...obbllllllllbbobob",
            "..obbbllllllllbobo.o",
            "..obbbllllllllbobo..",
            "..obbbbbbbbbbbbooo..",
            "..odbbodbbbbdobbdo..",
            "..oooooooooooooooo..",
        )
    }
}

/** Side-on: tall ears, joey pouch, big feet and a long tail. */
internal class Kangaroo : PetModel(Species.KANGAROO, Color(0xFFD08A52), Color(0xFF8E5530), Color(0xFFF6DDBF)) {
    override fun art(stage: Stage) = if (stage == Stage.BABY) BABY else ADULT

    private companion object {
        val BABY = listOf(
            "....................",
            "....................",
            "....................",
            "....................",
            "....................",
            "....oo.oo...........",
            "...obpobpo..........",
            "...obbobbo..........",
            "..obbbbbbbo.........",
            ".obbbwEbbbo.........",
            "ebbbbEEbbbo.........",
            "ombbbbbbbbo.........",
            ".ooooblbbbbo........",
            "....obllbbbbo.......",
            "....obllbbbbbo......",
            "....obbbbbbbbbo.....",
            "...oobbbooobbbbo....",
            "..obbbbbo...ooooo...",
            "..ooooooo...........",
            "....................",
        )
        val ADULT = listOf(
            "....................",
            "....oo.oo...........",
            "...obpobpo..........",
            "...obpobpo..........",
            "...obbobbo..........",
            "..obbbbbbbo.........",
            ".obbbwEbbbbo........",
            "ebbbbEEbbbbo........",
            "ombbbbbbbbbo........",
            ".oooobbbbbbo........",
            "....obbbbbbo........",
            "...oobllbbbbo.......",
            "..obbblllbbbbo......",
            "..ooblhhllbbbbo.....",
            "....obhhllbbbbbo....",
            "....oblllbbbbbbbo...",
            "....obbbbbbbbbbbbo..",
            "...oobbboooobbbbbbo.",
            "..obbbbbo...oobbbbbo",
            "..ooooooo.....oooooo",
        )
    }
}

/** Big pink-lined ears, tusks and a hanging trunk. */
internal class Elephant : PetModel(Species.ELEPHANT, Color(0xFFA7AEB8), Color(0xFF6B7380), Color(0xFFD8DCE2)) {
    override fun art(stage: Stage) = if (stage == Stage.BABY) BABY else ADULT

    private companion object {
        val BABY = listOf(
            "....................",
            "....................",
            "....................",
            "....................",
            "....................",
            "...oooooooooooooo...",
            "..obbbbbbbbbbbbbbo..",
            ".obppbbbbbbbbbbppbo.",
            ".obppbEEbbbbEEbppbo.",
            ".obpbbEwbbbbwEbbpbo.",
            ".obbbbbcbbbbcbbbbbo.",
            "..obbbbhobbohbbbbo..",
            "...obbbbobbobbbbo...",
            "...obbo.obbo.obbo...",
            "...obbo..oo..obbo...",
            "...oddo......oddo...",
            "...oooo......oooo...",
            "....................",
            "....................",
            "....................",
        )
        val ADULT = listOf(
            "....................",
            "....................",
            "....................",
            "..oooooooooooooooo..",
            ".obbbbbbbbbbbbbbbbo.",
            "obppbbbbbbbbbbbbppbo",
            "obppbbbbbbbbbbbbppbo",
            "obppbbEEbbbbEEbbppbo",
            "obpbbbEwbbbbwEbbbpbo",
            "obbbbbbbbbbbbbbbbbbo",
            ".obbbbbcbbbbcbbbbbo.",
            "..obbbbhobbohbbbbo..",
            "..obbbbbobbobbbbbo..",
            "..obbbbbobbobbbbbo..",
            "..obbbbbobbobbbbbo..",
            "..obbbo.obbo.obbbo..",
            "..obbbo.obbo.obbbo..",
            "..obbbo..oo..obbbo..",
            "..odddo......odddo..",
            "..ooooo......ooooo..",
        )
    }
}

/** Orange with black stripes and a white muzzle. */
internal class Tiger : PetModel(Species.TIGER, Color(0xFFF08A2C), Color(0xFF2B2420), Color(0xFFFFF4E6)) {
    override fun art(stage: Stage) = if (stage == Stage.BABY) BABY else ADULT

    private companion object {
        val BABY = listOf(
            "....................",
            "....................",
            "....................",
            "....................",
            "....................",
            "...oo..........oo...",
            "..obhoooooooooohbo..",
            "..obbbbbebbebbbbbo..",
            ".oebbbbbbbbbbbbbbeo.",
            ".obbEEbbbbbbbbEEbbo.",
            ".oebEwbbbbbbbbwEbeo.",
            ".obhhbbbhhhhbbbhhbo.",
            "..ohhhhhhpphhhhhho..",
            "...oohhhhmmhhhhoo...",
            "....obbbbbbbbbbo....",
            "....oehhhhhhhheo....",
            "....obbhhhhhhbbo....",
            "....oebobbbbobeo....",
            "....oooooooooooo....",
            "....................",
        )
        val ADULT = listOf(
            "....................",
            "..oo............oo..",
            ".obbo..........obbo.",
            ".obhboooooooooobhbo.",
            ".obbbbbedbbdebbbbbo.",
            ".obbbbbbebbebbbbbbo.",
            "oebbbbbbbbbbbbbbbbeo",
            "oebbbbbbbbbbbbbbbbeo",
            "obbbEEbbbbbbbbEEbbbo",
            "oebbEwbbbbbbbbwEbbeo",
            "obhhhbbbhhhhbbbhhhbo",
            "oehhhhhhhpphhhhhhheo",
            ".ohhhhhhhmmhhhhhhho.",
            "..oohhhhmhhmhhhhoo..",
            "...oobbbbbbbbbboo.oo",
            "...obehhhhhhhheboobo",
            "..obbbhhhhhhhhbbboeo",
            "..oebbhhhhhhhhbbobo.",
            "..obbobbbbbbbboboo..",
            "..oooooooooooooooo..",
        )
    }
}

/** Side-on: square snout, small ears and a yuzu on its head. */
internal class Capybara : PetModel(Species.CAPYBARA, Color(0xFFB08456), Color(0xFF6E4C2E), Color(0xFFD9B98E)) {
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
            "....oo..oooooo......",
            "...obboobbbbbbooo...",
            "..obEbbbbbbbbbbbbo..",
            "..obEwbbbbbbbbbbbo..",
            ".obbbbbbbbbbbbbbbo..",
            ".odebbbbbbbbbbbbbo..",
            ".omdbblllllbbbbbo...",
            "..oobbbbbbbbbbbo....",
            "....obo.obo.obo.....",
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
            "..........o.........",
            ".........oyo........",
            "....oo..ooyoo.......",
            "...obboobbbbbooo....",
            "..obbbbbbbbbbbbboo..",
            ".obEbbbbbbbbbbbbbbo.",
            ".obEwbbbbbbbbbbbbbo.",
            "obbbbbbbbbbbbbbbbbo.",
            "odebbbbbbbbbbbbbbbo.",
            "obbbbbbbbbbbbbbbbbo.",
            ".omdbblllllbbbbbbbo.",
            "..oobbbbbbbbbbbbbo..",
            "....obbo.obo.obbo...",
            "....oooo.ooo.oooo...",
            "....................",
        )
    }
}

/** Velvet body, pink star nose and big digging claws. */
internal class Mole : PetModel(Species.MOLE, Color(0xFF6B5A6E), Color(0xFF3E3342), Color(0xFFB8A9BC)) {
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
            ".....oooooooooo.....",
            "....obbbbbbbbbbo....",
            "...obbbbbbbbbbbbo...",
            "..obbEbbbbbbbbEbbo..",
            "..obbbbbppppbbbbbo..",
            "..obbbbppppppbbbbo..",
            "..obbbbbbmmbbbbbbo..",
            "..obllllllllllllbo..",
            "..ohhollllllllohho..",
            "..ohhhobbbbbbohhho..",
            "...oooooooooooooo...",
            "....................",
            "....................",
        )
        val ADULT = listOf(
            "....................",
            "....................",
            "....................",
            "....................",
            "....oooooooooooo....",
            "...obbbbbbbbbbbbo...",
            "..obbbbbbbbbbbbbbo..",
            ".obbbbbbbbbbbbbbbbo.",
            ".obbEbbbbbbbbbbEbbo.",
            ".obbbbbbbbbbbbbbbbo.",
            "obbbbbbbppppbbbbbbbo",
            "obbbbbbppppppbbbbbbo",
            "obbbbbbbppppbbbbbbbo",
            "obbbbbbbbmmbbbbbbbbo",
            "obbllllllbbllllllbbo",
            "obllllllllllllllllbo",
            "ohhobllllllllllbohho",
            "ohhhobbbbbbbbbbohhho",
            ".oooooooooooooooooo.",
            "....................",
        )
    }
}

/** Side-on: pointed snout and nose, brown spines with pale tips. */
internal class Hedgehog : PetModel(Species.HEDGEHOG, Color(0xFFD9B38C), Color(0xFF6B4A33), Color(0xFFF5E6D0)) {
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
            "..........o.o.o.....",
            "........o.ododdo....",
            ".......ododdhddoo...",
            "......oddhdddhddo...",
            ".....obbddhddddddo..",
            "....obbbbddhdhdddo..",
            "...obEwbbbddddhddo..",
            "..ebbbbbbbbddddddo..",
            "..oobbbbbbbbbdddo...",
            "....olllllllbbbbo...",
            ".....ollllllllloo...",
            "......obo...obo.....",
            "......ooo...ooo.....",
        )
        val ADULT = listOf(
            "....................",
            "....................",
            "....................",
            "....................",
            ".........o.o.o......",
            ".......o.ododdo.o...",
            "......ododdhdddoo...",
            ".....oddddhdddhddo..",
            "....odddhddddhdddo.o",
            "....oddhddddhddddddo",
            "...obbbddhdddhddhddo",
            "..obbbbbbddhddddddo.",
            ".obbEwbbbbddhdddhddo",
            "obbbbbbbbbbddddddddo",
            "ebbbbbbbbbbbddhddddo",
            "ooobbbbbbbbbbdddddo.",
            "..olllllbbbbbbbbbbo.",
            "...oolllllllllllloo.",
            "....obbo.....obbo...",
            "....oooo.....oooo...",
        )
    }
}

/** Pale face, small ears and a long tail. */
internal class Otter : PetModel(Species.OTTER, Color(0xFF9C6B4A), Color(0xFF6A4630), Color(0xFFEBD3B5)) {
    override fun art(stage: Stage) = if (stage == Stage.BABY) BABY else ADULT

    private companion object {
        val BABY = listOf(
            "....................",
            "....................",
            "....................",
            "....................",
            "....................",
            "...oo..........oo...",
            "..obboooooooooobbo..",
            "..obbbbbbbbbbbbbbo..",
            ".obllllbbbbbbllllbo.",
            ".olEEllllllllllEElo.",
            ".olEwllllllllllwElo.",
            ".olllllleeeellllllo.",
            "..ollllllmmllllllo..",
            "...oobbbbbbbbbboo...",
            "...obbllllllllbbo...",
            "...obbllllllllbbo...",
            "...odbbodbbdobbdo...",
            "...oooooooooooooo...",
            "....................",
            "....................",
        )
        val ADULT = listOf(
            "....................",
            "....................",
            "...oo..........oo...",
            "..obboo......oobbo..",
            "..obbbboooooobbbbo..",
            ".obbbbbbbbbbbbbbbbo.",
            ".obllllbbbbbbllllbo.",
            ".olEEllllllllllEElo.",
            ".olEwllllllllllwElo.",
            ".olllllleeeellllllo.",
            ".ohllllllmmllllllho.",
            "..oolllmllllmllloo..",
            "...oobbbbbbbbbboo...",
            "..obbbllllllllbbbo..",
            "..obbbllllllllbbbo..",
            ".obbbollllllllobboo.",
            ".obboollloolllooodo.",
            "..obbbbbbbbbbbbodbo.",
            "..odbbodbbbbdobooo..",
            "..oooooooooooooooo..",
        )
    }
}

/** Buck teeth and a flat paddle tail. */
internal class Beaver : PetModel(Species.BEAVER, Color(0xFFA0703F), Color(0xFF5E3F22), Color(0xFFE2C49A)) {
    override fun art(stage: Stage) = if (stage == Stage.BABY) BABY else ADULT

    private companion object {
        val BABY = listOf(
            "....................",
            "....................",
            "....................",
            "....................",
            "....................",
            "...oo..........oo...",
            "..obdoooooooooodbo..",
            "..obbbbbbbbbbbbbbo..",
            ".obbbbbbbbbbbbbbbbo.",
            ".obbEEbbbbbbbbEEbbo.",
            ".obbEwbbbbbbbbwEbbo.",
            ".obcbbllllllllbbcbo.",
            "..obbbllleelllbbbo..",
            "...obbllohhollbooo..",
            "....obbboooobbodddo.",
            "....obllllllllodddo.",
            "....obllllllllbooo..",
            "....odbbbbbbbbdo....",
            "....oooooooooooo....",
            "....................",
        )
        val ADULT = listOf(
            "....................",
            "....................",
            "....................",
            "..oo............oo..",
            ".obdo..........odbo.",
            ".obbboooooooooobbbo.",
            ".obbbbbbbbbbbbbbbbo.",
            "obbbbbbbbbbbbbbbbbbo",
            "obbbEEbbbbbbbbEEbbbo",
            "obbbEwbbbbbbbbwEbbbo",
            "obbcbbllllllllbbcbbo",
            ".obbblllleellllbbbo.",
            "..obbllllmmllllbbo..",
            "...oollwohhowlloo...",
            "...obbowohhowobbo...",
            "..obbboloooolobbooo.",
            "..obbbllllllllbodddo",
            "..obbbbbbbbbbbbodndo",
            "..odbbodbbbbdobodddo",
            "..ooooooooooooooooo.",
        )
    }
}
