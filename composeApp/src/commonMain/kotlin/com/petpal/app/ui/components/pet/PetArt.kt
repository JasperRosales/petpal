package com.petpal.app.ui.components.pet

import androidx.compose.ui.graphics.Color
import com.petpal.app.domain.model.Mood
import com.petpal.app.domain.model.Species
import com.petpal.app.ui.theme.SpriteInk
import com.petpal.app.ui.theme.SpriteLeaf

/** One model per species, built once. */
internal val Species.model: PetModel get() = MODELS.getValue(this)

private val MODELS: Map<Species, PetModel> = Species.entries.associateWith { modelFor(it) }

/** Exhaustive, so adding a species without a model fails to compile. */
private fun modelFor(s: Species): PetModel = when (s) {
    Species.SPROUT -> Sprout()
    Species.KITTY -> Kitty()
    Species.DRAKE -> Drake()
    Species.BUNNY -> Bunny()
    Species.PUP -> Pup()
    Species.OWL -> Owl()
    Species.TURTLE -> Turtle()
    Species.FOX -> Fox()
    Species.FROG -> Frog()
    Species.BEAR -> Bear()
    Species.PENGUIN -> Penguin()
    Species.HEDGEHOG -> Hedgehog()
    Species.BAT -> Bat()
    Species.DEER -> Deer()
    Species.OTTER -> Otter()
    Species.PANDA -> Panda()
    Species.LIZARD -> Lizard()
    Species.RACCOON -> Raccoon()
    Species.HAMSTER -> Hamster()
    Species.PARROT -> Parrot()
    Species.AXOLOTL -> Axolotl()
    Species.MOLE -> Mole()
    Species.KOALA -> Koala()
    Species.SHARK -> Shark()
    Species.CHAMELEON -> Chameleon()
    Species.BEAVER -> Beaver()
    Species.MONKEY -> Monkey()
    Species.KANGAROO -> Kangaroo()
    Species.EAGLE -> Eagle()
    Species.SEAL -> Seal()
    Species.ELEPHANT -> Elephant()
    Species.TIGER -> Tiger()
    Species.TILAPIA -> Tilapia()
    Species.CAPYBARA -> Capybara()
    Species.CRAB -> Crab()
    Species.FLAMINGO -> Flamingo()
}

/** Sprites that aren't a species: eggs, raid skeletons, the Fried Tilapia form. */
internal object PetArt {
    val FRIED_PALETTE = mapOf(
        'o' to Color(0xFF5A2E0C), 'e' to SpriteInk,
        'b' to Color(0xFFD08A2E), 'd' to Color(0xFF8A4A16), 'l' to Color(0xFFF0C062),
        'h' to Color(0xFFEDEDED), 'y' to Color(0xFFF7E15A), 'g' to SpriteLeaf,
    )

    /** Raid skeletons: every body colour becomes bone, while the ink stays dark. */
    val BONE_PALETTE = mapOf(
        'o' to SpriteInk, 'e' to SpriteInk, 'm' to SpriteInk,
        'b' to Color(0xFFE7E1D1), 'd' to Color(0xFFB6AE99), 'l' to Color(0xFFF8F4E8),
        'c' to Color(0xFFD8D2C2), 'p' to Color(0xFFD8D2C2), 'y' to Color(0xFFE7E1D1),
        'h' to Color(0xFFF8F4E8), 'g' to Color(0xFFC9C2AF), 'r' to Color(0xFFB6AE99), 'w' to Color.White,
    )

    /** Skull with hollow eyes, ribcage, bony arms and legs. */
    val SKELETON = listOf(
        "....................",
        "......oooooooo......",
        ".....obbbbbbbbo.....",
        "....obbbbbbbbbbo....",
        "....obeebbbbeebo....",
        "....oeeebbbbeeeo....",
        "....obbbbeebbbbo....",
        ".....obbbbbbbbo.....",
        ".....obebeebebo.....",
        "......oooooooo......",
        "........obbo........",
        "...oooooobboooooo...",
        "..obbbbbobbobbbbbo..",
        "..ob.oooobboooo.bo..",
        "..ob.obbbbbbbbo.bo..",
        "..ob..ooobbooo..bo..",
        "..ob.obbbbbbbbo.bo..",
        "..oo..oobbbboo..oo..",
        "......obboobbo......",
        "......ob....bo......",
    )

    /** Crispy golden fish with an X eye, still steaming. */
    fun friedFish(): List<String> {
        val g = PixelGrid()
        g.ellipse(8.5f, 11.5f, 6.5f, 3.8f, 'b')
        for (i in 0..3) for (y in 11 - i - 1..11 + i + 1) g.set(15 + i, y, 'd') // tail
        for (k in -2..2) g.set(8 + k * 2, 7, 'd') // crispy fin spikes
        g.ellipse(8.5f, 13.2f, 5f, 1.6f, 'l', onlyOver = 'b')
        g.outline()
        listOf(7, 10, 13).forEach { x -> for (d in 0..2) g.setIf(x - d, 10 + d, 'b', 'd') } // grill marks
        listOf(4 to 10, 6 to 10, 5 to 11, 4 to 12, 6 to 12).forEach { (x, y) -> g.set(x, y, 'e') } // X eye
        listOf(6 to 5, 7 to 4, 6 to 3, 10 to 5, 11 to 4, 10 to 3).forEach { (x, y) -> g.set(x, y, 'h') } // steam
        return g.rows()
    }

    fun egg(species: Species): List<String> {
        val g = PixelGrid()
        g.ellipse(PixelGrid.N / 2f, 11f, 5.5f, 7f, 'h')
        // Species-colored spots, placed differently for each species
        val spots = listOf(7 to 7, 11 to 9, 8 to 12, 12 to 14, 6 to 15, 10 to 5, 13 to 11, 7 to 10)
        spots.filterIndexed { i, _ -> (i + species.ordinal) % 3 != 0 }.forEach { (x, y) ->
            g.ellipse(x + 0.5f, y + 0.5f, 1.2f, 1f, 'b', onlyOver = 'h')
        }
        g.outline()
        return g.rows()
    }

    /** Opens or closes hand-drawn eyes ('E'): awake = ink with a shine, sleepy = a closed line. */
    fun withMood(rows: List<String>, mood: Mood): List<String> {
        val eyeRows = rows.indices.filter { 'E' in rows[it] }
        val lid = if (mood == Mood.SLEEPY && eyeRows.size > 1) eyeRows.first() else -1
        return rows.mapIndexed { y, r ->
            val chars = r.toCharArray()
            for (x in chars.indices) when (chars[x]) {
                'E' -> chars[x] = if (y == lid) chars.getOrNull(x - 1)?.takeIf { it != 'E' } ?: chars.getOrElse(x - 2) { 'b' } else 'e'
                'w' -> if (mood == Mood.SLEEPY && 'E' in r) chars[x] = 'e'
            }
            chars.concatToString()
        }
    }
}
