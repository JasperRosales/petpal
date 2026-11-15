package com.petpal.app.ui.components.pet

import androidx.compose.ui.graphics.Color
import com.petpal.app.domain.model.Mood
import com.petpal.app.domain.model.Species
import com.petpal.app.domain.model.Stage
import com.petpal.app.ui.theme.*

/**
 * One species' look: a front-facing 24-wide pixel sprite per stage (baby, kid, teen, adult each
 * gain their own features) plus its three body colours. Sprite chars are palette keys
 * ('o' outline, 'b' body, 'd' dark, 'l' belly, 'E' eye, 'w' shine, ...); '.' is empty.
 */
internal abstract class PetModel(
    val species: Species,
    val body: Color,
    val dark: Color,
    val belly: Color,
) {
    /** The sprite for a hatched stage. 'E' marks eye pixels so moods can close them. */
    abstract fun art(stage: Stage): List<String>

    fun palette(): Map<Char, Color> = mapOf(
        'o' to SpriteInk, 'e' to SpriteInk, 'm' to SpriteInk,
        'b' to body, 'd' to dark, 'l' to belly,
        'c' to SpriteCheek, 'p' to SpritePink, 'y' to SpriteYellow,
        'h' to SpriteCream, 'g' to SpriteLeaf, 'r' to SpriteRed, 'w' to Color.White,
        'u' to SpriteBlue, 'n' to SpriteBranch,
    )

    fun draw(stage: Stage, mood: Mood): List<String> =
        if (stage == Stage.EGG) PetArt.egg(species) else PetArt.withMood(art(stage), mood)
}
