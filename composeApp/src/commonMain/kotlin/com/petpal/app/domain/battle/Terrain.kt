package com.petpal.app.domain.battle

/**
 * Battle maps. Matches rotate through the standard arena and one terrain per element.
 * A terrain boosts its element's moves, and pets of that type fight on home turf.
 * Every terrain is solid ground, so any pet can fight on it (Flying gets windy cliffs, not open sky).
 */
enum class Terrain(val label: String, val element: Element?, val scenery: String) {
    ARENA("Ember Arena", null, "The standard stage. No terrain effects."),
    VOLCANO("Volcano Rim", Element.FIRE, "Lava cracks and hot ash."),
    LAKESHORE("Lakeshore", Element.WATER, "A shallow shore beside a calm lake."),
    FOREST("Deep Forest", Element.GRASS, "A mossy clearing among old trees."),
    TUNDRA("Frozen Tundra", Element.ICE, "Packed snow and slick ice."),
    THUNDER_PLAINS("Thunder Plains", Element.ELECTRIC, "Static crackles over the grass."),
    CANYON("Dusty Canyon", Element.GROUND, "Red rock and loose sand."),
    WINDY_CLIFFS("Windy Cliffs", Element.FLYING, "A clifftop above the clouds, with strong gusts."),
    ;

    /** One-line summary of what the terrain does. */
    val effect: String
        get() = element?.let { "${it.label} moves +${((MOVE_BOOST - 1) * 100).toInt()}% · ${it.label} pets charge energy faster" }
            ?: "No bonuses"

    fun boosts(m: Move) = element != null && m.element == element
    fun isHomeTurf(f: FighterInfo) = element != null && element in SpeciesKit.affinity(f.species)

    companion object {
        const val MOVE_BOOST = 1.2f
        const val HOME_ENERGY = 1.25f
        const val HOME_SPEED = 1.1f

        /** The [n]th map in the rotation: arena, then each element's terrain, and around again. */
        fun rotation(n: Int): Terrain = entries[((n % entries.size) + entries.size) % entries.size]
    }
}
