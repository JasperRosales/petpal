package com.petpal.app.domain.battle

/**
 * Battle maps. Matches rotate through the standard arena and one terrain per element.
 * Each terrain has its own rule that applies to every pet on it (see [effect]),
 * and pets of the terrain's type fight on home turf (faster energy and movement).
 * Every terrain is solid ground, so any pet can fight on it (Flying gets windy cliffs, not open sky).
 */
enum class Terrain(val label: String, val element: Element?, val scenery: String, val effectName: String, val effect: String) {
    ARENA("Ember Arena", null, "The standard stage. No terrain effects.", "", "No terrain effects"),
    VOLCANO("Volcano Rim", Element.FIRE, "Lava cracks and hot ash.",
        "Scorching Heat", "Every hit has a 20% chance to burn"),
    LAKESHORE("Lakeshore", Element.WATER, "A shallow shore beside a calm lake.",
        "Cool Breeze", "Every pet charges energy 40% faster"),
    FOREST("Deep Forest", Element.GRASS, "A mossy clearing among old trees.",
        "Life Regen", "Every pet regenerates 1 HP per second"),
    TUNDRA("Frozen Tundra", Element.ICE, "Packed snow and slick ice.",
        "Slick Ice", "Every pet moves 20% slower; slows and freezes last 50% longer"),
    THUNDER_PLAINS("Thunder Plains", Element.ELECTRIC, "Static crackles over the grass.",
        "Static Charge", "Every pet's cooldowns are 20% shorter"),
    CANYON("Dusty Canyon", Element.GROUND, "Red rock and loose sand.",
        "Sandstorm", "Every pet loses 0.6 HP per second (never knocks out)"),
    WINDY_CLIFFS("Windy Cliffs", Element.FLYING, "A clifftop above the clouds, with strong gusts.",
        "Tailwind", "Every pet moves 25% faster; knockbacks push twice as far"),
    DOJO("Mountain Dojo", Element.FIGHTING, "Worn wooden floor in a mountain hall.",
        "Fighting Spirit", "Every hit has a 15% chance to crit for 1.5× damage"),
    ;

    fun isHomeTurf(f: FighterInfo) = element != null && element in SpeciesKit.affinity(f.species)

    /** Cooldown multiplier for every pet. */
    val cooldownScale get() = if (this == THUNDER_PLAINS) 0.8f else 1f
    /** Energy charge multiplier for every pet. */
    val energyScale get() = if (this == LAKESHORE) 1.4f else 1f
    /** Movement speed multiplier for every pet. */
    val speedScale get() = when (this) { TUNDRA -> 0.8f; WINDY_CLIFFS -> 1.25f; else -> 1f }
    /** HP per second for every pet: positive heals, negative is chip damage. */
    val hpPerSec get() = when (this) { FOREST -> 1f; CANYON -> -0.6f; else -> 0f }
    /** Duration multiplier for slows and freezes. */
    val chillScale get() = if (this == TUNDRA) 1.5f else 1f
    val knockbackScale get() = if (this == WINDY_CLIFFS) 2f else 1f
    val burnChance get() = if (this == VOLCANO) 0.2f else 0f
    val critChance get() = if (this == DOJO) 0.15f else 0f

    companion object {
        const val CRIT = 1.5f
        const val HOME_ENERGY = 1.25f
        const val HOME_SPEED = 1.1f

        /** The [n]th map in the rotation: arena, then each element's terrain, and around again. */
        fun rotation(n: Int): Terrain = entries[((n % entries.size) + entries.size) % entries.size]
    }
}
