package com.petpal.app.domain.battle

import com.petpal.app.domain.model.Species
import com.petpal.app.domain.model.Stage

enum class Effect(val label: String, val self: Boolean = false) {
    NONE(""),
    LUNGE("lunge"),            // dashes at the target during wind-up
    TWIN_SLASH("2 stepping slashes"), // short step forward + slash, then another step + slash
    DRAIN("drain"),            // heals for half the damage dealt
    BURN("burn"),              // damage over time
    SLOW("slow"),              // halves target speed
    STUN("stun"),              // target can't move or act briefly
    FREEZE("freeze"),          // long stun
    KNOCKBACK("knockback"),    // big push away
    PULL("pull"),              // drags the target closer
    VANISH("vanish"),          // disappears (can't be hit) during wind-up, reappears next to the target
    RECOIL("recoil"),          // user takes a quarter of the damage dealt
    MULTI("hits 2-4x"),        // damage is per hit
    LEECH("leech"),            // target slowly loses HP to the user
    PARALYZE("paralyze"),      // target moves slower and its cooldowns run 50% longer
    WEAKEN("weaken"),          // target deals 30% less damage
    ROOT("root"),              // target can't move (can still act)
    SIPHON("steal energy"),    // takes energy from the target
    EXECUTE("execute"),        // double damage on targets below 30% HP
    RECHARGE("recharge"),      // user is stunned after using it
    MARK("mark"),              // target takes 25% more damage from everyone
    CHAIN("chain"),            // arcs to a second enemy (or zaps the same one again)
    SWAP("swap places"),       // user and target trade spots
    DISRUPT("disrupt"),        // adds 2s to all the target's cooldowns
    TRAP("trap", true),         // lays a snare at the target's feet that roots whoever steps on it
    // Self buffs
    SHIELD("shield", true),     // barrier that soaks up damage
    AID("team heal", true),     // heals the user and its teammates
    HEAL("heal", true),        // restore 18% HP
    RAGE("rage", true),         // +30% damage for 4s
    GUARD("guard", true),       // take 60% less damage for 1.2s
    HASTE("haste", true),       // +50% speed and 30% shorter cooldowns for 5s
    HARDEN("harden", true),     // take 40% less damage for 5s
    COUNTER("counter", true),   // next hit within 1.5s is reflected
    BLINK("teleport", true),    // jumps to the far side of the arena
    CLEANSE("cleanse", true),   // clears debuffs, heals 10%
    ENERGIZE("energy +50", true),
    BATTLE_CRY("rage + haste", true),
    PACK_CALL("rage + heal", true),
    FORTRESS("guard + heal", true),
    ALPHA("rage + haste + heal", true),
    CITADEL("guard + harden + heal", true),
    SMOKE("smoke cloud + back off", true), // leaves a cloud that slows enemies near it, then dashes away
}

/**
 * NORMAL: cooldown only. SPECIAL: needs a full energy bar (and empties it).
 * UNIQUE: species signature in slot 5, shares [BattleRules.UNIQUE_USES] uses per battle.
 */
enum class MoveCategory(val label: String) { NORMAL("Normal"), SPECIAL("Special"), UNIQUE("Unique") }

enum class Element(val label: String) {
    NORMAL("Normal"), FIRE("Fire"), WATER("Water"), GRASS("Grass"),
    ICE("Ice"), ELECTRIC("Electric"), FLYING("Flying"), GROUND("Ground"), FIGHTING("Fighting"),
}

private val N = MoveCategory.NORMAL
private val S = MoveCategory.SPECIAL
private val U = MoveCategory.UNIQUE

/** [level] is when a species that can learn it gets it (uniques come from the pet's stage instead). */
enum class Move(
    val label: String,
    val category: MoveCategory,
    val element: Element,
    val level: Int,
    val damage: Int,
    val range: Float,
    val cooldownMs: Long,
    val windupMs: Long,
    val recoverMs: Long,
    val effect: Effect = Effect.NONE,
    /** Also dashes at the target during the wind-up, on top of [effect] (slams). */
    private val alsoLunges: Boolean = false,
    /** Second type for combo moves (Thunder Punch = Electric + Fighting). */
    val element2: Element? = null,
) {
    // Normal
    TACKLE("Tackle", N, Element.NORMAL, 1, 8, 0.24f, 1300, 250, 200, Effect.LUNGE),
    GUARD("Guard", N, Element.NORMAL, 1, 0, 0f, 3500, 0, 0, Effect.GUARD),
    GROWL("Growl", N, Element.NORMAL, 2, 0, 0.45f, 6000, 250, 200, Effect.WEAKEN),
    PUNCH("Punch", N, Element.NORMAL, 3, 6, 0.16f, 700, 150, 150),
    CLAW("Claw Swipe", N, Element.NORMAL, 4, 7, 0.18f, 850, 150, 150),
    QUICK_ATTACK("Quick Attack", N, Element.NORMAL, 5, 6, 0.30f, 1000, 100, 100, Effect.LUNGE),
    BITE("Bite", N, Element.NORMAL, 6, 8, 0.15f, 1200, 200, 200, Effect.DRAIN),
    HOWL("Howl", N, Element.NORMAL, 8, 0, 0f, 9000, 300, 200, Effect.RAGE),
    FURY_SWIPES("Fury Swipes", N, Element.NORMAL, 9, 7, 0.20f, 1800, 180, 150, Effect.TWIN_SLASH),
    POUNCE("Pounce", N, Element.NORMAL, 11, 9, 0.32f, 1800, 300, 250, Effect.LUNGE),
    QUICK_JAB("Quick Jab", N, Element.NORMAL, 12, 5, 0.17f, 500, 100, 100),
    HEADBUTT("Headbutt", N, Element.NORMAL, 14, 9, 0.18f, 2400, 300, 250, Effect.STUN),
    GLARE("Glare", N, Element.NORMAL, 15, 0, 0.50f, 7000, 250, 200, Effect.PARALYZE),
    ZOOMIES("Zoomies", N, Element.NORMAL, 17, 0, 0f, 9000, 200, 100, Effect.HASTE),
    COUNTER("Counter", N, Element.NORMAL, 18, 0, 0f, 7000, 100, 200, Effect.COUNTER),
    HEAL("Heal", N, Element.NORMAL, 20, 0, 0f, 8000, 400, 200, Effect.HEAL),
    DOUBLE_EDGE("Double-Edge", N, Element.NORMAL, 22, 15, 0.22f, 2400, 300, 250, Effect.RECOIL),
    BODY_SLAM("Body Slam", N, Element.NORMAL, 23, 12, 0.20f, 2600, 350, 250, Effect.KNOCKBACK, alsoLunges = true),
    HARDEN("Harden", N, Element.NORMAL, 25, 0, 0f, 9000, 300, 200, Effect.HARDEN),
    TELEPORT("Teleport", N, Element.NORMAL, 27, 0, 0f, 6000, 150, 100, Effect.BLINK),
    DOUBLE_KICK("Double Kick", N, Element.NORMAL, 28, 5, 0.22f, 1600, 200, 200, Effect.MULTI),
    FOCUS_ENERGY("Focus Energy", N, Element.NORMAL, 30, 0, 0f, 12000, 400, 200, Effect.ENERGIZE),
    FINISHING_BLOW("Finishing Blow", N, Element.NORMAL, 31, 10, 0.20f, 2600, 250, 250, Effect.EXECUTE),
    REFRESH("Refresh", N, Element.NORMAL, 33, 0, 0f, 9000, 300, 200, Effect.CLEANSE),
    BATTLE_CRY("Battle Cry", S, Element.NORMAL, 35, 0, 0f, 6000, 300, 200, Effect.BATTLE_CRY),
    HYPER_FANG("Hyper Fang", S, Element.NORMAL, 38, 16, 0.20f, 3500, 300, 250, Effect.DRAIN),
    SECOND_WIND("Second Wind", N, Element.NORMAL, 42, 0, 0f, 6500, 300, 200, Effect.HEAL),
    GIGA_IMPACT("Giga Impact", S, Element.NORMAL, 45, 22, 0.25f, 4500, 600, 400, Effect.KNOCKBACK),
    LAST_STAND("Last Stand", S, Element.NORMAL, 50, 0, 0f, 6000, 200, 200, Effect.FORTRESS),
    HYPER_BEAM("Hyper Beam", S, Element.NORMAL, 52, 30, 0.60f, 4500, 700, 300, Effect.RECHARGE),
    MEGA_PUNCH("Mega Punch", S, Element.NORMAL, 55, 20, 0.18f, 4000, 450, 300, Effect.STUN),
    DISTRACT("Distract", N, Element.NORMAL, 13, 0, 0.45f, 8000, 250, 200, Effect.DISRUPT),
    LOCK_ON("Lock-On", N, Element.NORMAL, 16, 0, 0.50f, 7000, 250, 200, Effect.MARK),
    SWITCHEROO("Switcheroo", N, Element.NORMAL, 19, 5, 0.45f, 6000, 200, 200, Effect.SWAP),
    CHEER("Cheer", N, Element.NORMAL, 21, 0, 0f, 9000, 300, 200, Effect.AID),
    BARRIER("Barrier", N, Element.NORMAL, 26, 0, 0f, 10_000, 250, 200, Effect.SHIELD),

    // Fighting
    KARATE_CHOP("Karate Chop", N, Element.FIGHTING, 1, 7, 0.17f, 1000, 150, 150),
    LOW_KICK("Low Kick", N, Element.FIGHTING, 6, 8, 0.19f, 1600, 200, 200, Effect.SLOW),
    BULK_UP("Bulk Up", N, Element.FIGHTING, 10, 0, 0f, 9000, 300, 200, Effect.RAGE),
    BRICK_BREAK("Brick Break", N, Element.FIGHTING, 15, 11, 0.17f, 2000, 250, 200, Effect.DISRUPT),
    DRAIN_PUNCH("Drain Punch", N, Element.FIGHTING, 22, 10, 0.17f, 2200, 250, 200, Effect.DRAIN),
    CROSS_CHOP("Cross Chop", N, Element.FIGHTING, 30, 14, 0.18f, 2600, 300, 250, Effect.STUN),
    DYNAMIC_PUNCH("Dynamic Punch", S, Element.FIGHTING, 38, 18, 0.18f, 4000, 450, 300, Effect.PARALYZE, alsoLunges = true),
    CLOSE_COMBAT("Close Combat", S, Element.FIGHTING, 48, 26, 0.20f, 4200, 400, 300, Effect.RECOIL),
    FOCUS_BLAST("Focus Blast", S, Element.FIGHTING, 56, 24, 0.55f, 4500, 550, 300, Effect.KNOCKBACK),
    // Combo types: learnable by pets of either type
    THUNDER_PUNCH("Thunder Punch", N, Element.ELECTRIC, 18, 11, 0.18f, 2300, 200, 200, Effect.PARALYZE, element2 = Element.FIGHTING),
    FIRE_PUNCH("Fire Punch", N, Element.FIRE, 18, 11, 0.18f, 2300, 200, 200, Effect.BURN, element2 = Element.FIGHTING),
    ROCK_SMASH("Rock Smash", N, Element.GROUND, 12, 9, 0.17f, 1800, 200, 200, Effect.KNOCKBACK, element2 = Element.FIGHTING),
    SKY_UPPERCUT("Sky Uppercut", N, Element.FLYING, 26, 12, 0.18f, 2400, 250, 200, Effect.KNOCKBACK, element2 = Element.FIGHTING),
    AQUA_FIST("Aqua Fist", N, Element.WATER, 20, 11, 0.18f, 2300, 200, 200, Effect.SLOW, element2 = Element.FIGHTING),

    // More moves
    RAPID_SPIN("Rapid Spin", N, Element.NORMAL, 7, 6, 0.20f, 2500, 200, 200, Effect.KNOCKBACK),
    SUCKER_PUNCH("Sucker Punch", N, Element.NORMAL, 24, 10, 0.25f, 2200, 100, 200, Effect.LUNGE),
    HEAT_WAVE("Heat Wave", S, Element.FIRE, 42, 17, 0.60f, 4200, 500, 300, Effect.BURN),
    SCALD("Scald", N, Element.WATER, 26, 10, 0.45f, 2200, 250, 200, Effect.BURN),
    SPORE_CLOUD("Spore Cloud", N, Element.GRASS, 18, 0, 0.45f, 8000, 300, 200, Effect.WEAKEN),
    HAIL_STORM("Hail Storm", S, Element.ICE, 36, 15, 0.60f, 4000, 500, 300, Effect.SLOW),
    VOLT_SWITCH("Volt Switch", N, Element.ELECTRIC, 22, 8, 0.45f, 2600, 200, 200, Effect.SWAP),
    MUD_BOMB("Mud Bomb", N, Element.GROUND, 16, 9, 0.45f, 2000, 300, 200, Effect.WEAKEN),
    COMET_PUNCH("Comet Punch", N, Element.FIGHTING, 4, 4, 0.18f, 1600, 150, 200, Effect.MULTI),
    VACUUM_WAVE("Vacuum Wave", N, Element.FIGHTING, 12, 7, 0.45f, 1600, 100, 150),
    SEISMIC_TOSS("Seismic Toss", S, Element.FIGHTING, 44, 20, 0.20f, 4200, 400, 300, Effect.KNOCKBACK, element2 = Element.GROUND),

    // Fire
    EMBER("Ember", N, Element.FIRE, 1, 6, 0.40f, 1800, 250, 200, Effect.BURN),
    SMOKESCREEN("Smokescreen", N, Element.FIRE, 5, 0, 0f, 7000, 150, 150, Effect.SMOKE),
    FIRE_FANG("Fire Fang", N, Element.FIRE, 8, 9, 0.16f, 1300, 200, 200, Effect.BURN),
    FLAME_DASH("Flame Dash", N, Element.FIRE, 11, 9, 0.30f, 1800, 300, 250, Effect.LUNGE),
    INCINERATE("Incinerate", N, Element.FIRE, 14, 8, 0.35f, 2000, 250, 200, Effect.SIPHON),
    HEAT_UP("Heat Up", N, Element.FIRE, 17, 0, 0f, 8000, 300, 200, Effect.RAGE),
    FIRE_SPIN("Fire Spin", N, Element.FIRE, 20, 6, 0.35f, 2600, 300, 250, Effect.ROOT),
    FLAME_WHEEL("Flame Wheel", N, Element.FIRE, 24, 11, 0.24f, 2000, 300, 250, Effect.BURN),
    WILL_O_WISP("Will-o-Wisp", N, Element.FIRE, 28, 0, 0.45f, 7000, 300, 200, Effect.WEAKEN),
    FIRE_BREATH("Fire Breath", S, Element.FIRE, 32, 16, 0.50f, 4000, 450, 350, Effect.BURN),
    FIREBALL("Fireball", N, Element.FIRE, 36, 10, 0.50f, 2200, 350, 250, Effect.BURN),
    FLARE_BLITZ("Flare Blitz", N, Element.FIRE, 40, 18, 0.25f, 2800, 350, 300, Effect.RECOIL),
    MAGMA_BURST("Magma Burst", S, Element.FIRE, 46, 19, 0.40f, 4200, 500, 300, Effect.KNOCKBACK),
    OVERHEAT("Overheat", S, Element.FIRE, 52, 28, 0.50f, 4500, 600, 350, Effect.RECHARGE),
    BLAST_FLAME("Blast Flame", S, Element.FIRE, 57, 22, 0.55f, 4500, 550, 350, Effect.BURN),
    SCORCH_MARK("Scorch Mark", N, Element.FIRE, 26, 8, 0.40f, 2200, 250, 200, Effect.MARK),

    // Water
    BUBBLE("Bubble", N, Element.WATER, 1, 6, 0.45f, 1600, 250, 200, Effect.SLOW),
    WATER_GUN("Water Gun", N, Element.WATER, 5, 7, 0.45f, 1400, 200, 200),
    WITHDRAW("Withdraw", N, Element.WATER, 8, 0, 0f, 9000, 300, 200, Effect.HARDEN),
    AQUA_JET("Aqua Jet", N, Element.WATER, 11, 7, 0.32f, 1100, 100, 100, Effect.LUNGE),
    AQUA_TAIL("Aqua Tail", N, Element.WATER, 14, 10, 0.20f, 1800, 250, 250, Effect.KNOCKBACK),
    RAIN_DANCE("Rain Dance", N, Element.WATER, 17, 0, 0f, 9000, 300, 200, Effect.HASTE),
    WHIRLPOOL("Whirlpool", N, Element.WATER, 20, 6, 0.35f, 2800, 300, 250, Effect.ROOT),
    BRINE("Brine", N, Element.WATER, 24, 10, 0.40f, 2000, 300, 200, Effect.EXECUTE),
    DIVE("Dive", N, Element.WATER, 28, 14, 0.30f, 3500, 900, 300, Effect.VANISH),
    WATER_JET("Water Jet", S, Element.WATER, 32, 14, 0.55f, 3500, 350, 250, Effect.KNOCKBACK),
    AQUA_RING("Aqua Ring", N, Element.WATER, 36, 0, 0f, 9000, 300, 200, Effect.CLEANSE),
    WATERFALL("Waterfall", N, Element.WATER, 40, 12, 0.22f, 2400, 300, 250, Effect.STUN),
    HYDRO_PUMP("Hydro Pump", S, Element.WATER, 46, 20, 0.60f, 4500, 550, 350, Effect.SLOW),
    HYDRO_CANNON("Hydro Cannon", S, Element.WATER, 52, 28, 0.55f, 4500, 600, 350, Effect.RECHARGE),
    UNDERTOW("Undertow", S, Element.WATER, 57, 22, 0.60f, 4500, 550, 350, Effect.PULL),
    BUBBLE_SHIELD("Bubble Shield", N, Element.WATER, 22, 0, 0f, 10_000, 300, 200, Effect.SHIELD),
    HEALING_RAIN("Healing Rain", N, Element.WATER, 34, 0, 0f, 10_000, 400, 200, Effect.AID),

    // Grass
    LEAF_TOSS("Leaf Toss", N, Element.GRASS, 1, 7, 0.42f, 1500, 250, 200),
    ABSORB("Absorb", N, Element.GRASS, 5, 6, 0.35f, 1400, 200, 200, Effect.DRAIN),
    LEECH_SEED("Leech Seed", N, Element.GRASS, 8, 2, 0.45f, 7000, 300, 200, Effect.LEECH),
    RAZOR_LEAF("Razor Leaf", N, Element.GRASS, 11, 9, 0.45f, 1800, 250, 200),
    STUN_SPORE("Stun Spore", N, Element.GRASS, 14, 0, 0.40f, 7000, 300, 200, Effect.PARALYZE),
    GROWTH("Growth", N, Element.GRASS, 17, 0, 0f, 9000, 300, 200, Effect.RAGE),
    BULLET_SEED("Bullet Seed", N, Element.GRASS, 20, 4, 0.40f, 1900, 250, 200, Effect.MULTI),
    SEED_BOMB("Seed Bomb", N, Element.GRASS, 24, 11, 0.40f, 2200, 300, 250, Effect.KNOCKBACK),
    VINE_WHIP("Vine Whip", N, Element.GRASS, 28, 10, 0.45f, 2000, 300, 200, Effect.PULL),
    SYNTHESIS("Synthesis", N, Element.GRASS, 32, 0, 0f, 7500, 400, 200, Effect.HEAL),
    GIGA_DRAIN("Giga Drain", N, Element.GRASS, 36, 10, 0.40f, 2600, 350, 250, Effect.DRAIN),
    SLEEP_POWDER("Sleep Powder", N, Element.GRASS, 40, 0, 0.40f, 9000, 400, 200, Effect.FREEZE),
    PETAL_STORM("Petal Storm", S, Element.GRASS, 46, 18, 0.50f, 4200, 450, 300, Effect.SLOW),
    FRENZY_PLANT("Frenzy Plant", S, Element.GRASS, 52, 28, 0.50f, 4500, 600, 350, Effect.RECHARGE),
    LEAF_BLADE("Leaf Blade", S, Element.GRASS, 57, 22, 0.25f, 4000, 350, 300, Effect.LUNGE),
    BRAMBLE_SNARE("Bramble Snare", N, Element.GRASS, 18, 6, 0.60f, 7000, 300, 200, Effect.TRAP),

    // Ice
    POWDER_SNOW("Powder Snow", N, Element.ICE, 1, 6, 0.40f, 1600, 250, 200, Effect.SLOW),
    ICE_FANG("Ice Fang", N, Element.ICE, 5, 9, 0.16f, 1300, 200, 200, Effect.SLOW),
    MIST("Mist", N, Element.ICE, 8, 0, 0f, 9000, 300, 200, Effect.CLEANSE),
    ICY_WIND("Icy Wind", N, Element.ICE, 11, 8, 0.50f, 1900, 300, 200, Effect.SLOW),
    ICICLE_SPEAR("Icicle Spear", N, Element.ICE, 14, 3, 0.40f, 1800, 250, 200, Effect.MULTI),
    FROST_ARMOR("Frost Armor", N, Element.ICE, 17, 0, 0f, 9000, 300, 200, Effect.HARDEN),
    ICE_PUNCH("Ice Punch", N, Element.ICE, 20, 11, 0.18f, 2300, 200, 200, Effect.STUN, element2 = Element.FIGHTING),
    FROST_BREATH("Frost Breath", N, Element.ICE, 24, 10, 0.45f, 2400, 300, 250),
    ICE_SHARD("Ice Shard", S, Element.ICE, 28, 14, 0.50f, 3500, 350, 200, Effect.SLOW),
    AVALANCHE("Avalanche", N, Element.ICE, 32, 12, 0.30f, 2600, 400, 300, Effect.KNOCKBACK),
    MIRROR_ICE("Mirror Ice", N, Element.ICE, 36, 0, 0f, 8000, 100, 200, Effect.COUNTER),
    FREEZE_RAY("Freeze Ray", N, Element.ICE, 40, 8, 0.55f, 5000, 400, 250, Effect.FREEZE),
    BLIZZARD("Blizzard", S, Element.ICE, 46, 19, 0.60f, 4500, 550, 350, Effect.SLOW),
    GLACIER_CRASH("Glacier Crash", S, Element.ICE, 52, 22, 0.25f, 4500, 500, 350, Effect.STUN),
    ABSOLUTE_ZERO("Absolute Zero", S, Element.ICE, 57, 18, 0.50f, 5000, 700, 300, Effect.FREEZE),
    FROST_MINE("Frost Mine", N, Element.ICE, 30, 8, 0.60f, 7500, 300, 200, Effect.TRAP),

    // Electric
    SPARK("Spark", N, Element.ELECTRIC, 1, 7, 0.20f, 1000, 150, 150),
    THUNDER_WAVE("Thunder Wave", N, Element.ELECTRIC, 5, 0, 0.50f, 7000, 250, 200, Effect.PARALYZE),
    THUNDER_FANG("Thunder Fang", N, Element.ELECTRIC, 8, 9, 0.16f, 2300, 200, 200, Effect.STUN),
    SHOCK_WAVE("Shock Wave", N, Element.ELECTRIC, 11, 8, 0.50f, 1800, 250, 200),
    VOLT_SAP("Volt Sap", N, Element.ELECTRIC, 14, 6, 0.30f, 1800, 200, 200, Effect.SIPHON),
    CHARGE("Charge", N, Element.ELECTRIC, 17, 0, 0f, 12000, 300, 200, Effect.ENERGIZE),
    NUZZLE("Nuzzle", N, Element.ELECTRIC, 20, 5, 0.20f, 1800, 150, 150, Effect.PARALYZE),
    WILD_CHARGE("Wild Charge", N, Element.ELECTRIC, 24, 12, 0.30f, 2200, 300, 250, Effect.LUNGE),
    MAGNET_PULL("Magnet Pull", N, Element.ELECTRIC, 28, 7, 0.55f, 2600, 300, 200, Effect.PULL),
    THUNDER("Thunder", S, Element.ELECTRIC, 32, 18, 0.60f, 4500, 550, 300, Effect.STUN),
    DISCHARGE("Discharge", N, Element.ELECTRIC, 36, 10, 0.35f, 2800, 300, 250, Effect.STUN),
    VOLT_TACKLE("Volt Tackle", N, Element.ELECTRIC, 40, 18, 0.30f, 2800, 300, 250, Effect.RECOIL),
    ZAP_CANNON("Zap Cannon", S, Element.ELECTRIC, 46, 20, 0.55f, 4500, 600, 350, Effect.PARALYZE),
    THUNDERBOLT("Thunderbolt", S, Element.ELECTRIC, 52, 22, 0.60f, 4500, 500, 300, Effect.KNOCKBACK),
    GIGAVOLT("Gigavolt", S, Element.ELECTRIC, 57, 30, 0.65f, 4500, 700, 300, Effect.RECHARGE),
    STATIC_JAM("Static Jam", N, Element.ELECTRIC, 22, 6, 0.45f, 2400, 250, 200, Effect.DISRUPT),
    CHAIN_LIGHTNING("Chain Lightning", N, Element.ELECTRIC, 30, 10, 0.50f, 2600, 300, 250, Effect.CHAIN),

    // Flying
    PECK("Peck", N, Element.FLYING, 1, 6, 0.22f, 800, 150, 150),
    AIR_CUTTER("Air Cutter", N, Element.FLYING, 5, 6, 0.42f, 1600, 250, 200),
    WING_ATTACK("Wing Attack", N, Element.FLYING, 8, 9, 0.25f, 1600, 200, 200),
    AERIAL_ACE("Aerial Ace", N, Element.FLYING, 11, 8, 0.30f, 1500, 200, 200, Effect.LUNGE),
    FEATHER_DANCE("Feather Dance", N, Element.FLYING, 14, 0, 0.45f, 7000, 300, 200, Effect.WEAKEN),
    TAILWIND("Tailwind", N, Element.FLYING, 17, 0, 0f, 9000, 200, 100, Effect.HASTE),
    ROOST("Roost", N, Element.FLYING, 20, 0, 0f, 7000, 300, 200, Effect.HEAL),
    TWISTER("Twister", N, Element.FLYING, 24, 8, 0.45f, 2000, 300, 200, Effect.KNOCKBACK),
    FLY("Fly", N, Element.FLYING, 28, 16, 0.30f, 4000, 1200, 300, Effect.VANISH),
    BOUNCE("Bounce", N, Element.FLYING, 30, 13, 0.30f, 3600, 1000, 300, Effect.VANISH),
    GUST("Gust", S, Element.FLYING, 32, 13, 0.55f, 3500, 300, 200, Effect.KNOCKBACK),
    DRILL_PECK("Drill Peck", N, Element.FLYING, 36, 4, 0.22f, 1800, 200, 200, Effect.MULTI),
    BRAVE_BIRD("Brave Bird", N, Element.FLYING, 40, 20, 0.30f, 3000, 400, 300, Effect.RECOIL),
    HURRICANE("Hurricane", S, Element.FLYING, 46, 20, 0.60f, 4500, 550, 350, Effect.KNOCKBACK),
    SKY_DROP("Sky Drop", S, Element.FLYING, 52, 22, 0.30f, 4500, 1000, 350, Effect.VANISH),
    SKY_ATTACK("Sky Attack", S, Element.FLYING, 57, 32, 0.40f, 5000, 700, 300, Effect.RECHARGE),
    CROSSWIND("Crosswind", N, Element.FLYING, 22, 8, 0.40f, 3000, 250, 200, Effect.SWAP),

    // Ground
    MUD_SLAP("Mud Slap", N, Element.GROUND, 1, 6, 0.38f, 1600, 250, 200, Effect.SLOW),
    SAND_ATTACK("Sand Attack", N, Element.GROUND, 5, 0, 0.45f, 6000, 250, 200, Effect.WEAKEN),
    ROCK_THROW("Rock Throw", N, Element.GROUND, 8, 8, 0.42f, 1700, 250, 200),
    SAND_TOMB("Sand Tomb", N, Element.GROUND, 11, 6, 0.40f, 2600, 300, 250, Effect.ROOT),
    STOMP("Stomp", N, Element.GROUND, 14, 10, 0.20f, 2400, 300, 250, Effect.STUN),
    STONE_SKIN("Stone Skin", N, Element.GROUND, 17, 0, 0f, 9000, 300, 200, Effect.HARDEN),
    BONE_RUSH("Bone Rush", N, Element.GROUND, 20, 4, 0.25f, 1800, 200, 200, Effect.MULTI),
    DIG("Dig", N, Element.GROUND, 24, 15, 0.30f, 3500, 900, 300, Effect.VANISH),
    BULLDOZE("Bulldoze", N, Element.GROUND, 28, 9, 0.45f, 2400, 300, 250, Effect.SLOW),
    ROCK_SLAM("Rock Slam", S, Element.GROUND, 32, 18, 0.22f, 4000, 500, 300, Effect.STUN, alsoLunges = true),
    SANDSTORM("Sandstorm", N, Element.GROUND, 36, 9, 0.60f, 2400, 350, 250),
    HEAD_SMASH("Head Smash", N, Element.GROUND, 40, 20, 0.20f, 3000, 350, 300, Effect.RECOIL),
    EARTHQUAKE("Earthquake", S, Element.GROUND, 46, 20, 0.90f, 4500, 600, 350, Effect.STUN),
    FISSURE("Fissure", S, Element.GROUND, 52, 26, 0.40f, 4500, 600, 350, Effect.EXECUTE),
    STONE_EDGE("Stone Edge", S, Element.GROUND, 57, 22, 0.40f, 4200, 500, 300, Effect.KNOCKBACK),
    RICOCHET("Ricochet", N, Element.GROUND, 21, 8, 0.45f, 2000, 250, 200, Effect.CHAIN),
    CALTROPS("Caltrops", N, Element.GROUND, 26, 8, 0.60f, 7500, 300, 200, Effect.TRAP),

    // Uniques: Kid form, then the Adult upgrade (slot 5)
    SOLAR_BEAM("Solar Beam", U, Element.GRASS, 0, 26, 0.75f, 15_000, 700, 300),
    SOLAR_FLARE("Solar Flare", U, Element.GRASS, 0, 34, 0.80f, 15_000, 700, 300, Effect.STUN),
    SHADOW_CLAW("Shadow Claw", U, Element.NORMAL, 0, 24, 0.35f, 15_000, 300, 250, Effect.LUNGE),
    NIGHT_TERROR("Night Terror", U, Element.NORMAL, 0, 32, 0.40f, 15_000, 300, 250, Effect.LUNGE),
    INFERNO("Inferno", U, Element.FIRE, 0, 28, 0.55f, 15_000, 600, 300, Effect.BURN),
    DRAGON_FURY("Dragon Fury", U, Element.FIRE, 0, 36, 0.60f, 15_000, 600, 300, Effect.BURN),
    MOON_KICK("Moon Kick", U, Element.NORMAL, 0, 22, 0.40f, 15_000, 350, 250, Effect.STUN),
    LUNAR_STRIKE("Lunar Strike", U, Element.NORMAL, 0, 30, 0.45f, 15_000, 350, 250, Effect.STUN),
    PACK_CALL("Pack Call", U, Element.NORMAL, 0, 0, 0f, 15_000, 400, 200, Effect.PACK_CALL),
    ALPHA_HOWL("Alpha Howl", U, Element.NORMAL, 0, 0, 0f, 15_000, 400, 200, Effect.ALPHA),
    STORM_EYE("Storm Eye", U, Element.ELECTRIC, 0, 25, 0.85f, 15_000, 650, 300, Effect.STUN),
    TEMPEST_EYE("Tempest Eye", U, Element.ELECTRIC, 0, 32, 0.90f, 15_000, 650, 300, Effect.PARALYZE),
    FORTRESS("Fortress", U, Element.GROUND, 0, 0, 0f, 15_000, 200, 200, Effect.FORTRESS),
    CITADEL("Citadel", U, Element.GROUND, 0, 0, 0f, 15_000, 200, 200, Effect.CITADEL),
    FOXFIRE("Foxfire", U, Element.FIRE, 0, 22, 0.60f, 15_000, 450, 250, Effect.BURN),
    NINE_TAILS("Nine Tails", U, Element.FIRE, 0, 30, 0.65f, 15_000, 450, 250, Effect.BURN),
    TIDAL_WAVE("Tidal Wave", U, Element.WATER, 0, 22, 0.70f, 15_000, 550, 300, Effect.KNOCKBACK),
    TSUNAMI("Tsunami", U, Element.WATER, 0, 30, 0.75f, 15_000, 550, 300, Effect.PULL),
    MAUL("Maul", U, Element.NORMAL, 0, 30, 0.22f, 15_000, 450, 300, Effect.STUN),
    GRIZZLY_RAMPAGE("Grizzly Rampage", U, Element.NORMAL, 0, 38, 0.24f, 15_000, 450, 300, Effect.STUN),

    // Baby-form uniques (first tier, upgraded at Kid and again at Adult)
    SUN_RAY("Sun Ray", U, Element.GRASS, 0, 18, 0.65f, 15_000, 600, 300),
    SHADOW_SWIPE("Shadow Swipe", U, Element.NORMAL, 0, 17, 0.30f, 15_000, 300, 250, Effect.LUNGE),
    FLAME_BURST("Flame Burst", U, Element.FIRE, 0, 20, 0.50f, 15_000, 550, 300, Effect.BURN),
    MOON_HOP("Moon Hop", U, Element.NORMAL, 0, 16, 0.35f, 15_000, 350, 250, Effect.STUN),
    RALLY_BARK("Rally Bark", U, Element.NORMAL, 0, 0, 0f, 15_000, 400, 200, Effect.BATTLE_CRY),
    STATIC_EYE("Static Eye", U, Element.ELECTRIC, 0, 18, 0.75f, 15_000, 600, 300, Effect.PARALYZE),
    SHELL_UP("Shell Up", U, Element.GROUND, 0, 0, 0f, 15_000, 200, 200, Effect.HARDEN),
    WISP_FLAME("Wisp Flame", U, Element.FIRE, 0, 16, 0.55f, 15_000, 450, 250, Effect.BURN),
    SPLASH_WAVE("Splash Wave", U, Element.WATER, 0, 16, 0.60f, 15_000, 500, 300, Effect.KNOCKBACK),
    BEAR_SWIPE("Bear Swipe", U, Element.NORMAL, 0, 22, 0.20f, 15_000, 450, 300, Effect.STUN),
    FROST_SLIDE("Frost Slide", U, Element.ICE, 0, 17, 0.35f, 15_000, 300, 250, Effect.LUNGE),
    POLAR_CRASH("Polar Crash", U, Element.ICE, 0, 25, 0.30f, 15_000, 400, 300, Effect.KNOCKBACK),
    EMPEROR_BLIZZARD("Emperor Blizzard", U, Element.ICE, 0, 32, 0.70f, 15_000, 650, 300, Effect.FREEZE),
    SPIKE_ROLL("Spike Roll", U, Element.GROUND, 0, 16, 0.32f, 15_000, 300, 250, Effect.LUNGE),
    NEEDLE_STORM("Needle Storm", U, Element.GRASS, 0, 8, 0.50f, 15_000, 450, 300, Effect.MULTI),
    QUILL_BARRAGE("Quill Barrage", U, Element.GRASS, 0, 11, 0.60f, 15_000, 450, 300, Effect.MULTI),
    ECHO_SHRIEK("Echo Shriek", U, Element.NORMAL, 0, 16, 0.55f, 15_000, 400, 250, Effect.WEAKEN),
    VAMPIRE_FANG("Vampire Fang", U, Element.NORMAL, 0, 24, 0.25f, 15_000, 300, 250, Effect.DRAIN),
    BLOOD_MOON("Blood Moon", U, Element.NORMAL, 0, 32, 0.60f, 15_000, 600, 300, Effect.DRAIN),
    ANTLER_JAB("Antler Jab", U, Element.GRASS, 0, 17, 0.22f, 15_000, 300, 250, Effect.KNOCKBACK),
    VERDANT_CHARGE("Verdant Charge", U, Element.GRASS, 0, 25, 0.35f, 15_000, 350, 250, Effect.LUNGE),
    SPIRIT_STAMPEDE("Spirit Stampede", U, Element.ICE, 0, 33, 0.45f, 15_000, 450, 300, Effect.STUN),
    PEBBLE_TOSS("Pebble Toss", U, Element.WATER, 0, 16, 0.45f, 15_000, 300, 250, Effect.SLOW),
    RIVER_RUSH("River Rush", U, Element.WATER, 0, 24, 0.35f, 15_000, 300, 250, Effect.LUNGE),
    RAPID_CURRENT("Rapid Current", U, Element.WATER, 0, 32, 0.65f, 15_000, 550, 300, Effect.PULL),
    BAMBOO_SMACK("Bamboo Smack", U, Element.GRASS, 0, 18, 0.22f, 15_000, 350, 300, Effect.STUN),
    BAMBOO_STORM("Bamboo Storm", U, Element.GRASS, 0, 26, 0.50f, 15_000, 450, 300, Effect.KNOCKBACK),
    PANDA_QUAKE("Panda Quake", U, Element.GROUND, 0, 34, 0.80f, 15_000, 600, 350, Effect.STUN),
    TAIL_LASH("Tail Lash", U, Element.GROUND, 0, 17, 0.22f, 15_000, 300, 250, Effect.KNOCKBACK),
    MAGMA_TAIL("Magma Tail", U, Element.FIRE, 0, 25, 0.30f, 15_000, 350, 250, Effect.BURN),
    VOLCANO_ROAR("Volcano Roar", U, Element.FIRE, 0, 33, 0.60f, 15_000, 600, 300, Effect.BURN),
    SNATCH("Snatch", U, Element.NORMAL, 0, 16, 0.22f, 15_000, 250, 250, Effect.SIPHON),
    TRASH_TOSS("Trash Toss", U, Element.NORMAL, 0, 24, 0.45f, 15_000, 400, 250, Effect.WEAKEN),
    MIDNIGHT_HEIST("Midnight Heist", U, Element.ELECTRIC, 0, 31, 0.30f, 15_000, 900, 300, Effect.VANISH),
    SEED_SPIT("Seed Spit", U, Element.NORMAL, 0, 16, 0.50f, 15_000, 300, 250, Effect.SLOW),
    WHEEL_DASH("Wheel Dash", U, Element.ELECTRIC, 0, 24, 0.35f, 15_000, 300, 250, Effect.LUNGE),
    THUNDER_WHEEL("Thunder Wheel", U, Element.ELECTRIC, 0, 32, 0.40f, 15_000, 350, 250, Effect.LUNGE),
    SQUAWK("Squawk", U, Element.NORMAL, 0, 15, 0.55f, 15_000, 350, 250, Effect.WEAKEN),
    FEATHER_BLAZE("Feather Blaze", U, Element.FIRE, 0, 24, 0.55f, 15_000, 450, 250, Effect.BURN),
    PHOENIX_DIVE("Phoenix Dive", U, Element.FIRE, 0, 33, 0.40f, 15_000, 900, 300, Effect.VANISH),
    BUBBLE_GILL("Bubble Gill", U, Element.WATER, 0, 16, 0.50f, 15_000, 400, 250, Effect.SLOW),
    TIDE_POOL("Tide Pool", U, Element.WATER, 0, 23, 0.55f, 15_000, 450, 300, Effect.DRAIN),
    LOTUS_TORRENT("Lotus Torrent", U, Element.GRASS, 0, 31, 0.70f, 15_000, 600, 300, Effect.LEECH),
    BURROW_JAB("Burrow Jab", U, Element.GROUND, 0, 17, 0.25f, 15_000, 800, 250, Effect.VANISH),
    DRILL_CLAW("Drill Claw", U, Element.GROUND, 0, 9, 0.22f, 15_000, 300, 250, Effect.MULTI),
    TREMOR_SPARK("Tremor Spark", U, Element.ELECTRIC, 0, 32, 0.80f, 15_000, 650, 300, Effect.STUN),
    EUCALYPTUS_DART("Eucalyptus Dart", U, Element.GRASS, 0, 16, 0.50f, 15_000, 300, 250, Effect.SLOW),
    DROWSY_SLAM("Drowsy Slam", U, Element.NORMAL, 0, 24, 0.22f, 15_000, 400, 300, Effect.STUN, alsoLunges = true),
    DREAM_STORM("Dream Storm", U, Element.ELECTRIC, 0, 32, 0.70f, 15_000, 600, 300, Effect.CHAIN),
    FIN_SLICE("Fin Slice", U, Element.WATER, 0, 17, 0.30f, 15_000, 250, 250, Effect.LUNGE),
    JAW_LOCK("Jaw Lock", U, Element.WATER, 0, 25, 0.22f, 15_000, 300, 250, Effect.MARK, alsoLunges = true),
    FEEDING_FRENZY("Feeding Frenzy", U, Element.WATER, 0, 33, 0.25f, 15_000, 350, 300, Effect.DRAIN, alsoLunges = true),
    TONGUE_SNAP("Tongue Snap", U, Element.NORMAL, 0, 16, 0.50f, 15_000, 250, 250, Effect.PULL),
    MIRAGE_SWAP("Mirage Swap", U, Element.GRASS, 0, 24, 0.50f, 15_000, 300, 250, Effect.SWAP),
    PRISM_BLAST("Prism Blast", U, Element.FIRE, 0, 32, 0.65f, 15_000, 550, 300, Effect.DISRUPT),
    LOG_TOSS("Log Toss", U, Element.GROUND, 0, 17, 0.45f, 15_000, 300, 250, Effect.KNOCKBACK),
    TIMBER_TRAP("Timber Trap", U, Element.GROUND, 0, 25, 0.65f, 15_000, 300, 250, Effect.TRAP),
    GREAT_DAM("Great Dam", U, Element.WATER, 0, 0, 0f, 15_000, 250, 200, Effect.SHIELD),
    BANANA_TOSS("Banana Toss", U, Element.GRASS, 0, 16, 0.50f, 15_000, 300, 250, Effect.SLOW),
    VINE_SWING("Vine Swing", U, Element.FIGHTING, 0, 24, 0.35f, 15_000, 350, 250, Effect.TWIN_SLASH),
    CANOPY_FURY("Canopy Fury", U, Element.FIGHTING, 0, 32, 0.30f, 15_000, 400, 300, Effect.STUN, alsoLunges = true, element2 = Element.GRASS),
    POUCH_JAB("Pouch Jab", U, Element.FIGHTING, 0, 17, 0.22f, 15_000, 250, 250, Effect.KNOCKBACK),
    BOXING_FLURRY("Boxing Flurry", U, Element.FIGHTING, 0, 8, 0.22f, 15_000, 300, 250, Effect.MULTI),
    OUTBACK_STOMP("Outback Stomp", U, Element.GROUND, 0, 32, 0.45f, 15_000, 500, 300, Effect.STUN, element2 = Element.FIGHTING),
    TALON_GRAB("Talon Grab", U, Element.FLYING, 0, 16, 0.40f, 15_000, 300, 250, Effect.PULL),
    SKY_DIVE("Sky Dive", U, Element.FLYING, 0, 25, 0.50f, 15_000, 500, 250, Effect.VANISH),
    STORM_TALON("Storm Talon", U, Element.FLYING, 0, 33, 0.55f, 15_000, 450, 300, Effect.KNOCKBACK, element2 = Element.FIGHTING),
    FLIPPER_SLAP("Flipper Slap", U, Element.WATER, 0, 16, 0.22f, 15_000, 250, 250, Effect.SLOW),
    ICE_FLOE("Ice Floe", U, Element.ICE, 0, 24, 0.40f, 15_000, 300, 250, Effect.LUNGE),
    ARCTIC_TIDE("Arctic Tide", U, Element.WATER, 0, 32, 0.65f, 15_000, 600, 300, Effect.FREEZE, element2 = Element.ICE),
    // Second unique options (one per stage) and the Teen tier
    POLLEN_PUFF("Pollen Puff", U, Element.GRASS, 0, 0, 0.50f, 15_000, 300, 200, Effect.WEAKEN),
    ROOT_BIND("Root Bind", U, Element.GRASS, 0, 20, 0.45f, 15_000, 350, 250, Effect.ROOT),
    BLOOM_BURST("Bloom Burst", U, Element.GRASS, 0, 28, 0.60f, 15_000, 500, 250, Effect.LEECH),
    GAIA_SHIELD("Gaia Shield", U, Element.GROUND, 0, 0, 0.00f, 15_000, 250, 200, Effect.FORTRESS),
    WORLD_TREE("World Tree", U, Element.GRASS, 0, 0, 0.00f, 15_000, 400, 200, Effect.ALPHA),
    CAT_NAP("Cat Nap", U, Element.NORMAL, 0, 0, 0.00f, 15_000, 300, 200, Effect.HEAL),
    HISS("Hiss", U, Element.NORMAL, 0, 0, 0.50f, 15_000, 250, 200, Effect.PARALYZE),
    PHANTOM_POUNCE("Phantom Pounce", U, Element.NORMAL, 0, 28, 0.35f, 15_000, 700, 250, Effect.VANISH),
    STATIC_PURR("Static Purr", U, Element.ELECTRIC, 0, 0, 0.00f, 15_000, 250, 200, Effect.HASTE),
    NINTH_LIFE("Ninth Life", U, Element.NORMAL, 0, 0, 0.00f, 15_000, 250, 200, Effect.CITADEL),
    SMOKE_PUFF("Smoke Puff", U, Element.FIRE, 0, 0, 0.00f, 15_000, 200, 200, Effect.SMOKE),
    DRAGON_ROAR("Dragon Roar", U, Element.FIRE, 0, 0, 0.50f, 15_000, 300, 200, Effect.WEAKEN),
    WING_BLAZE("Wing Blaze", U, Element.FLYING, 0, 28, 0.45f, 15_000, 400, 250, Effect.KNOCKBACK, element2 = Element.FIRE),
    SCALE_ARMOR("Scale Armor", U, Element.FIRE, 0, 0, 0.00f, 15_000, 250, 200, Effect.HARDEN),
    DRAGON_ASCENT("Dragon Ascent", U, Element.FIRE, 0, 0, 0.00f, 15_000, 350, 200, Effect.BATTLE_CRY),
    THUMP("Thump", U, Element.GROUND, 0, 0, 0.45f, 15_000, 250, 200, Effect.SLOW),
    LUCKY_FOOT("Lucky Foot", U, Element.NORMAL, 0, 0, 0.00f, 15_000, 200, 200, Effect.HASTE),
    CRESCENT_KICK("Crescent Kick", U, Element.FIGHTING, 0, 28, 0.30f, 15_000, 350, 250, Effect.KNOCKBACK, alsoLunges = true),
    MOONLIGHT("Moonlight", U, Element.NORMAL, 0, 0, 0.00f, 15_000, 350, 200, Effect.HEAL),
    ECLIPSE("Eclipse", U, Element.NORMAL, 0, 0, 0.60f, 15_000, 400, 200, Effect.DISRUPT),
    PUPPY_EYES("Puppy Eyes", U, Element.NORMAL, 0, 0, 0.50f, 15_000, 250, 200, Effect.WEAKEN),
    FETCH_DASH("Fetch Dash", U, Element.GROUND, 0, 24, 0.35f, 15_000, 300, 250, Effect.LUNGE),
    GUARD_DOG("Guard Dog", U, Element.NORMAL, 0, 0, 0.00f, 15_000, 100, 200, Effect.COUNTER),
    FIRE_HOWL("Fire Howl", U, Element.FIRE, 0, 28, 0.50f, 15_000, 450, 250, Effect.BURN),
    LOYAL_GUARDIAN("Loyal Guardian", U, Element.NORMAL, 0, 0, 0.00f, 15_000, 350, 200, Effect.AID),
    HOOT("Hoot", U, Element.NORMAL, 0, 0, 0.55f, 15_000, 250, 200, Effect.MARK),
    SILENT_GLIDE("Silent Glide", U, Element.FLYING, 0, 0, 0.00f, 15_000, 150, 200, Effect.BLINK),
    FEATHER_VOLLEY("Feather Volley", U, Element.FLYING, 0, 10, 0.60f, 15_000, 400, 250, Effect.MULTI),
    WISE_GAZE("Wise Gaze", U, Element.ELECTRIC, 0, 0, 0.60f, 15_000, 300, 200, Effect.DISRUPT),
    NIGHT_WATCH("Night Watch", U, Element.FLYING, 0, 0, 0.00f, 15_000, 100, 200, Effect.COUNTER),
    SHELL_BASH("Shell Bash", U, Element.GROUND, 0, 16, 0.22f, 15_000, 300, 250, Effect.KNOCKBACK),
    AQUA_SHELL("Aqua Shell", U, Element.WATER, 0, 0, 0.00f, 15_000, 250, 200, Effect.SHIELD),
    TIDAL_SHELL("Tidal Shell", U, Element.WATER, 0, 28, 0.50f, 15_000, 450, 250, Effect.SLOW),
    BEDROCK("Bedrock", U, Element.GROUND, 0, 0, 0.00f, 15_000, 250, 200, Effect.HARDEN),
    ANCIENT_WAVE("Ancient Wave", U, Element.WATER, 0, 32, 0.70f, 15_000, 550, 250, Effect.KNOCKBACK),
    FOX_TRICK("Fox Trick", U, Element.NORMAL, 0, 0, 0.50f, 15_000, 250, 200, Effect.DISRUPT),
    ILLUSION("Illusion", U, Element.NORMAL, 0, 0, 0.00f, 15_000, 150, 200, Effect.BLINK),
    FROST_FOX("Frost Fox", U, Element.ICE, 0, 28, 0.50f, 15_000, 400, 250, Effect.SLOW),
    MIRAGE_TAILS("Mirage Tails", U, Element.NORMAL, 0, 0, 0.00f, 15_000, 100, 200, Effect.COUNTER),
    KITSUNE_VEIL("Kitsune Veil", U, Element.FIRE, 0, 0, 0.00f, 15_000, 200, 200, Effect.SMOKE),
    CROAK("Croak", U, Element.NORMAL, 0, 0, 0.50f, 15_000, 250, 200, Effect.WEAKEN),
    TONGUE_LASH("Tongue Lash", U, Element.WATER, 0, 22, 0.50f, 15_000, 250, 250, Effect.PULL),
    LILY_LEAP("Lily Leap", U, Element.GRASS, 0, 28, 0.40f, 15_000, 700, 250, Effect.VANISH),
    RAIN_SONG("Rain Song", U, Element.WATER, 0, 0, 0.00f, 15_000, 350, 200, Effect.HEAL),
    SWAMP_KING("Swamp King", U, Element.GRASS, 0, 0, 0.60f, 15_000, 350, 200, Effect.ROOT),
    BEAR_HUG("Bear Hug", U, Element.FIGHTING, 0, 16, 0.20f, 15_000, 300, 250, Effect.ROOT),
    HIBERNATE("Hibernate", U, Element.NORMAL, 0, 0, 0.00f, 15_000, 300, 200, Effect.FORTRESS),
    HAMMER_PAW("Hammer Paw", U, Element.FIGHTING, 0, 30, 0.22f, 15_000, 400, 250, Effect.KNOCKBACK, alsoLunges = true),
    MIGHTY_ROAR("Mighty Roar", U, Element.NORMAL, 0, 0, 0.50f, 15_000, 300, 200, Effect.WEAKEN),
    URSA_MAJOR("Ursa Major", U, Element.NORMAL, 0, 0, 0.00f, 15_000, 400, 200, Effect.ALPHA),
    HUDDLE("Huddle", U, Element.ICE, 0, 0, 0.00f, 15_000, 250, 200, Effect.SHIELD),
    SNOWBALL("Snowball", U, Element.ICE, 0, 22, 0.50f, 15_000, 300, 250, Effect.SLOW),
    GLACIER_DIVE("Glacier Dive", U, Element.WATER, 0, 28, 0.50f, 15_000, 700, 250, Effect.VANISH),
    COLD_SNAP("Cold Snap", U, Element.ICE, 0, 0, 0.50f, 15_000, 350, 200, Effect.FREEZE),
    ICEBERG_WALL("Iceberg Wall", U, Element.ICE, 0, 0, 0.00f, 15_000, 250, 200, Effect.CITADEL),
    CURL_UP("Curl Up", U, Element.GROUND, 0, 0, 0.00f, 15_000, 200, 200, Effect.HARDEN),
    SPINY_SHIELD("Spiny Shield", U, Element.GRASS, 0, 0, 0.00f, 15_000, 100, 200, Effect.COUNTER),
    PIN_MISSILE("Pin Missile", U, Element.GRASS, 0, 9, 0.55f, 15_000, 400, 250, Effect.MULTI),
    HEDGE_MAZE("Hedge Maze", U, Element.GRASS, 0, 26, 0.60f, 15_000, 300, 250, Effect.TRAP),
    THORN_FORTRESS("Thorn Fortress", U, Element.GRASS, 0, 0, 0.00f, 15_000, 250, 200, Effect.FORTRESS),
    SONAR("Sonar", U, Element.NORMAL, 0, 0, 0.60f, 15_000, 250, 200, Effect.MARK),
    NIGHT_FLIGHT("Night Flight", U, Element.FLYING, 0, 0, 0.00f, 15_000, 200, 200, Effect.HASTE),
    LEECH_LIFE("Leech Life", U, Element.NORMAL, 0, 26, 0.25f, 15_000, 300, 250, Effect.LEECH),
    SONIC_SCREECH("Sonic Screech", U, Element.NORMAL, 0, 0, 0.55f, 15_000, 300, 200, Effect.DISRUPT),
    DUSK_SWARM("Dusk Swarm", U, Element.FLYING, 0, 12, 0.60f, 15_000, 450, 250, Effect.MULTI),
    FAWN_LEAP("Fawn Leap", U, Element.NORMAL, 0, 0, 0.00f, 15_000, 150, 200, Effect.BLINK),
    FOREST_BLESSING("Forest Blessing", U, Element.GRASS, 0, 0, 0.00f, 15_000, 350, 200, Effect.AID),
    FROST_ANTLERS("Frost Antlers", U, Element.ICE, 0, 28, 0.30f, 15_000, 350, 250, Effect.STUN, alsoLunges = true),
    GRACE("Grace", U, Element.GRASS, 0, 0, 0.00f, 15_000, 350, 200, Effect.HEAL),
    SACRED_GROVE("Sacred Grove", U, Element.GRASS, 0, 0, 0.60f, 15_000, 400, 200, Effect.LEECH),
    CLAM_SMASH("Clam Smash", U, Element.WATER, 0, 16, 0.20f, 15_000, 300, 250, Effect.STUN),
    PLAYTIME("Playtime", U, Element.NORMAL, 0, 0, 0.00f, 15_000, 300, 200, Effect.ENERGIZE),
    VOLT_SPLASH("Volt Splash", U, Element.ELECTRIC, 0, 28, 0.55f, 15_000, 450, 250, Effect.CHAIN, element2 = Element.WATER),
    FLOAT("Float", U, Element.WATER, 0, 0, 0.00f, 15_000, 200, 200, Effect.HASTE),
    OTTER_PARTY("Otter Party", U, Element.WATER, 0, 0, 0.00f, 15_000, 350, 200, Effect.AID),
    LAZY_ROLL("Lazy Roll", U, Element.NORMAL, 0, 0, 0.00f, 15_000, 300, 200, Effect.HEAL),
    PANDA_PALM("Panda Palm", U, Element.FIGHTING, 0, 24, 0.20f, 15_000, 300, 250, Effect.KNOCKBACK),
    BAMBOO_WALL("Bamboo Wall", U, Element.GRASS, 0, 0, 0.00f, 15_000, 250, 200, Effect.SHIELD),
    KUNG_FU_KICK("Kung Fu Kick", U, Element.FIGHTING, 0, 28, 0.30f, 15_000, 350, 250, Effect.STUN, alsoLunges = true),
    ZEN_MASTER("Zen Master", U, Element.FIGHTING, 0, 0, 0.00f, 15_000, 250, 200, Effect.CITADEL),
    SUNBATHE("Sunbathe", U, Element.FIRE, 0, 0, 0.00f, 15_000, 300, 200, Effect.RAGE),
    SAND_SPIT("Sand Spit", U, Element.GROUND, 0, 20, 0.45f, 15_000, 300, 250, Effect.WEAKEN),
    MOLTEN_SCALES("Molten Scales", U, Element.FIRE, 0, 0, 0.00f, 15_000, 250, 200, Effect.HARDEN),
    DUNE_LASH("Dune Lash", U, Element.GROUND, 0, 28, 0.30f, 15_000, 300, 250, Effect.KNOCKBACK),
    BASILISK_GAZE("Basilisk Gaze", U, Element.FIRE, 0, 0, 0.60f, 15_000, 350, 200, Effect.PARALYZE),
    SCAVENGE("Scavenge", U, Element.NORMAL, 0, 0, 0.00f, 15_000, 300, 200, Effect.ENERGIZE),
    MASKED_FEINT("Masked Feint", U, Element.NORMAL, 0, 0, 0.50f, 15_000, 250, 200, Effect.DISRUPT),
    GARBAGE_SHOCK("Garbage Shock", U, Element.ELECTRIC, 0, 28, 0.40f, 15_000, 350, 250, Effect.PARALYZE),
    SNEAK("Sneak", U, Element.NORMAL, 0, 0, 0.00f, 15_000, 150, 200, Effect.BLINK),
    GRAND_LARCENY("Grand Larceny", U, Element.NORMAL, 0, 30, 0.30f, 15_000, 300, 250, Effect.SIPHON, alsoLunges = true),
    CHEEK_STUFF("Cheek Stuff", U, Element.NORMAL, 0, 0, 0.00f, 15_000, 300, 200, Effect.HEAL),
    SPARK_ROLL("Spark Roll", U, Element.ELECTRIC, 0, 22, 0.35f, 15_000, 300, 250, Effect.PARALYZE, alsoLunges = true),
    SUNFLOWER_SEED("Sunflower Seed", U, Element.GRASS, 0, 28, 0.50f, 15_000, 350, 250, Effect.LEECH),
    TURBO_WHEEL("Turbo Wheel", U, Element.ELECTRIC, 0, 0, 0.00f, 15_000, 200, 200, Effect.HASTE),
    HOARD("Hoard", U, Element.NORMAL, 0, 0, 0.00f, 15_000, 300, 200, Effect.FORTRESS),
    MIMIC("Mimic", U, Element.NORMAL, 0, 0, 0.55f, 15_000, 250, 200, Effect.DISRUPT),
    TROPICAL_WIND("Tropical Wind", U, Element.FLYING, 0, 22, 0.55f, 15_000, 350, 250, Effect.KNOCKBACK),
    PLUMAGE_FLARE("Plumage Flare", U, Element.FIRE, 0, 0, 0.00f, 15_000, 300, 200, Effect.BATTLE_CRY),
    ECHO_TALK("Echo Talk", U, Element.NORMAL, 0, 0, 0.55f, 15_000, 250, 200, Effect.MARK),
    RAINBOW_WING("Rainbow Wing", U, Element.FLYING, 0, 0, 0.00f, 15_000, 400, 200, Effect.ALPHA),
    REGEN_BUBBLE("Regen Bubble", U, Element.WATER, 0, 0, 0.00f, 15_000, 300, 200, Effect.HEAL),
    GILL_MIST("Gill Mist", U, Element.WATER, 0, 0, 0.50f, 15_000, 300, 200, Effect.SLOW),
    LOTUS_SHIELD("Lotus Shield", U, Element.GRASS, 0, 0, 0.00f, 15_000, 250, 200, Effect.SHIELD),
    CORAL_WHIP("Coral Whip", U, Element.WATER, 0, 28, 0.30f, 15_000, 300, 250, Effect.KNOCKBACK),
    REBIRTH("Rebirth", U, Element.WATER, 0, 0, 0.00f, 15_000, 400, 200, Effect.PACK_CALL),
    MUD_FLICK("Mud Flick", U, Element.GROUND, 0, 15, 0.45f, 15_000, 300, 250, Effect.SLOW),
    TUNNEL_NET("Tunnel Net", U, Element.GROUND, 0, 22, 0.60f, 15_000, 300, 250, Effect.TRAP),
    SEISMIC_SENSE("Seismic Sense", U, Element.GROUND, 0, 0, 0.60f, 15_000, 300, 200, Effect.MARK),
    LIVE_WIRE("Live Wire", U, Element.ELECTRIC, 0, 28, 0.30f, 15_000, 300, 250, Effect.PARALYZE),
    EARTH_CORE("Earth Core", U, Element.GROUND, 0, 0, 0.00f, 15_000, 250, 200, Effect.FORTRESS),
    SNUGGLE("Snuggle", U, Element.NORMAL, 0, 0, 0.00f, 15_000, 250, 200, Effect.SHIELD),
    LULLABY("Lullaby", U, Element.GRASS, 0, 0, 0.50f, 15_000, 400, 200, Effect.FREEZE),
    GUM_LEAF("Gum Leaf", U, Element.GRASS, 0, 26, 0.50f, 15_000, 350, 250, Effect.DRAIN),
    STATIC_SNOOZE("Static Snooze", U, Element.ELECTRIC, 0, 0, 0.00f, 15_000, 300, 200, Effect.ENERGIZE),
    EUCALYPTUS_GROVE("Eucalyptus Grove", U, Element.GRASS, 0, 0, 0.00f, 15_000, 350, 200, Effect.AID),
    BLOOD_TRAIL("Blood Trail", U, Element.WATER, 0, 0, 0.55f, 15_000, 250, 200, Effect.MARK),
    RIPTIDE("Riptide", U, Element.WATER, 0, 22, 0.50f, 15_000, 300, 250, Effect.PULL),
    SHARK_FRENZY("Shark Frenzy", U, Element.WATER, 0, 0, 0.00f, 15_000, 250, 200, Effect.BATTLE_CRY),
    ICE_FIN("Ice Fin", U, Element.ICE, 0, 28, 0.35f, 15_000, 300, 250, Effect.SLOW, alsoLunges = true),
    MEGALODON("Megalodon", U, Element.WATER, 0, 34, 0.25f, 15_000, 350, 250, Effect.EXECUTE, alsoLunges = true),
    CAMOUFLAGE("Camouflage", U, Element.GRASS, 0, 0, 0.00f, 15_000, 150, 200, Effect.BLINK),
    COLOR_FLASH("Color Flash", U, Element.NORMAL, 0, 0, 0.50f, 15_000, 250, 200, Effect.DISRUPT),
    STICKY_TONGUE("Sticky Tongue", U, Element.GRASS, 0, 26, 0.55f, 15_000, 300, 250, Effect.ROOT),
    BLEND_IN("Blend In", U, Element.GRASS, 0, 0, 0.00f, 15_000, 200, 200, Effect.SMOKE),
    SPECTRUM_VEIL("Spectrum Veil", U, Element.NORMAL, 0, 0, 0.00f, 15_000, 250, 200, Effect.CITADEL),
    GNAW("Gnaw", U, Element.NORMAL, 0, 16, 0.20f, 15_000, 250, 250, Effect.WEAKEN),
    BEAVER_TAIL_SLAP("Beaver Tail Slap", U, Element.WATER, 0, 24, 0.25f, 15_000, 300, 250, Effect.KNOCKBACK),
    LODGE("Lodge", U, Element.GROUND, 0, 0, 0.00f, 15_000, 250, 200, Effect.FORTRESS),
    FLOOD_GATE("Flood Gate", U, Element.WATER, 0, 28, 0.60f, 15_000, 450, 250, Effect.SLOW),
    TIMBER_FALL("Timber Fall", U, Element.GROUND, 0, 34, 0.40f, 15_000, 500, 250, Effect.STUN),
    MONKEY_BUSINESS("Monkey Business", U, Element.NORMAL, 0, 0, 0.50f, 15_000, 250, 200, Effect.DISRUPT),
    CHEST_BEAT("Chest Beat", U, Element.FIGHTING, 0, 0, 0.00f, 15_000, 300, 200, Effect.RAGE),
    JUNGLE_KICK("Jungle Kick", U, Element.FIGHTING, 0, 28, 0.30f, 15_000, 300, 250, Effect.KNOCKBACK),
    PRANKSTER("Prankster", U, Element.NORMAL, 0, 0, 0.55f, 15_000, 250, 200, Effect.MARK),
    APE_KING("Ape King", U, Element.FIGHTING, 0, 0, 0.00f, 15_000, 400, 200, Effect.ALPHA),
    HOP_AWAY("Hop Away", U, Element.NORMAL, 0, 0, 0.00f, 15_000, 150, 200, Effect.BLINK),
    TAIL_STAND("Tail Stand", U, Element.FIGHTING, 0, 0, 0.00f, 15_000, 100, 200, Effect.COUNTER),
    MEGA_KICK("Mega Kick", U, Element.FIGHTING, 0, 30, 0.25f, 15_000, 400, 250, Effect.KNOCKBACK, alsoLunges = true),
    SAND_KICK("Sand Kick", U, Element.GROUND, 0, 0, 0.45f, 15_000, 250, 200, Effect.WEAKEN),
    CHAMPION("Champion", U, Element.FIGHTING, 0, 0, 0.00f, 15_000, 350, 200, Effect.BATTLE_CRY),
    KEEN_GLARE("Keen Glare", U, Element.NORMAL, 0, 0, 0.60f, 15_000, 250, 200, Effect.MARK),
    UPDRAFT("Updraft", U, Element.FLYING, 0, 0, 0.00f, 15_000, 200, 200, Effect.HASTE),
    AERIAL_STRIKE("Aerial Strike", U, Element.FLYING, 0, 28, 0.50f, 15_000, 400, 250, Effect.KNOCKBACK),
    RAPTOR_CRY("Raptor Cry", U, Element.FLYING, 0, 0, 0.55f, 15_000, 300, 200, Effect.WEAKEN),
    SKY_SOVEREIGN("Sky Sovereign", U, Element.FLYING, 0, 0, 0.00f, 15_000, 400, 200, Effect.ALPHA),
    BARK_BARK("Bark Bark", U, Element.NORMAL, 0, 0, 0.50f, 15_000, 250, 200, Effect.WEAKEN),
    ICE_BALL("Ice Ball", U, Element.ICE, 0, 22, 0.50f, 15_000, 300, 250, Effect.SLOW),
    SEAL_SPIN("Seal Spin", U, Element.WATER, 0, 28, 0.30f, 15_000, 300, 250, Effect.KNOCKBACK),
    BLUBBER_UP("Blubber Up", U, Element.ICE, 0, 0, 0.00f, 15_000, 250, 200, Effect.HARDEN),
    POLAR_SPLASH("Polar Splash", U, Element.WATER, 0, 0, 0.60f, 15_000, 400, 200, Effect.FREEZE),
    // Elephant
    TRUNK_SLAP("Trunk Slap", U, Element.NORMAL, 0, 16, 0.24f, 15_000, 250, 250, Effect.KNOCKBACK),
    TRUNK_SPRAY("Trunk Spray", U, Element.WATER, 0, 0, 0.50f, 15_000, 300, 200, Effect.SLOW),
    TUSK_GORE("Tusk Gore", U, Element.GROUND, 0, 24, 0.28f, 15_000, 350, 250, Effect.STUN, alsoLunges = true),
    MUD_SHOWER("Mud Shower", U, Element.GROUND, 0, 0, 0.00f, 15_000, 300, 200, Effect.CLEANSE),
    STAMPEDE("Stampede", U, Element.GROUND, 0, 28, 0.35f, 15_000, 400, 250, Effect.KNOCKBACK, alsoLunges = true),
    TRUMPET("Trumpet", U, Element.NORMAL, 0, 0, 0.55f, 15_000, 300, 200, Effect.DISRUPT),
    MAMMOTH_QUAKE("Mammoth Quake", U, Element.GROUND, 0, 34, 0.45f, 15_000, 550, 300, Effect.STUN, element2 = Element.ICE),
    HERD_GUARDIAN("Herd Guardian", U, Element.GROUND, 0, 0, 0.00f, 15_000, 400, 200, Effect.CITADEL),
    // Tiger
    STRIPE_SLASH("Stripe Slash", U, Element.FIGHTING, 0, 10, 0.20f, 15_000, 200, 200, Effect.TWIN_SLASH),
    TIGER_GROWL("Tiger Growl", U, Element.NORMAL, 0, 0, 0.50f, 15_000, 250, 200, Effect.WEAKEN),
    TIGER_POUNCE("Tiger Pounce", U, Element.FIGHTING, 0, 24, 0.40f, 15_000, 300, 250, Effect.LUNGE),
    STALK("Stalk", U, Element.NORMAL, 0, 0, 0.00f, 15_000, 200, 200, Effect.HASTE),
    BLAZING_CLAW("Blazing Claw", U, Element.FIRE, 0, 28, 0.24f, 15_000, 350, 250, Effect.BURN, alsoLunges = true, element2 = Element.FIGHTING),
    PREDATOR_EYE("Predator Eye", U, Element.NORMAL, 0, 0, 0.60f, 15_000, 250, 200, Effect.MARK),
    SAVAGE_MAUL("Savage Maul", U, Element.FIGHTING, 0, 34, 0.25f, 15_000, 450, 300, Effect.EXECUTE, alsoLunges = true, element2 = Element.FIRE),
    TIGER_KING("Tiger King", U, Element.FIGHTING, 0, 0, 0.00f, 15_000, 400, 200, Effect.ALPHA),
    // Tilapia
    FIN_FLICK("Fin Flick", U, Element.WATER, 0, 16, 0.24f, 15_000, 250, 250, Effect.SLOW),
    BUBBLE_NEST("Bubble Nest", U, Element.WATER, 0, 0, 0.00f, 15_000, 250, 200, Effect.SHIELD),
    SCHOOL_RUSH("School Rush", U, Element.WATER, 0, 9, 0.30f, 15_000, 300, 250, Effect.MULTI),
    MOUTH_BROOD("Mouth Brood", U, Element.WATER, 0, 0, 0.00f, 15_000, 300, 200, Effect.FORTRESS),
    ALGAE_BLOOM("Algae Bloom", U, Element.GRASS, 0, 26, 0.55f, 15_000, 450, 250, Effect.LEECH),
    SCALE_SHED("Scale Shed", U, Element.NORMAL, 0, 0, 0.00f, 15_000, 250, 200, Effect.CLEANSE),
    NILE_FLOOD("Nile Flood", U, Element.WATER, 0, 34, 0.65f, 15_000, 600, 300, Effect.KNOCKBACK, element2 = Element.GRASS),
    FISH_FRY("Fish Fry", U, Element.FIRE, 0, 30, 0.45f, 15_000, 450, 250, Effect.BURN),
    ;

    /** Buffs target the user; everything else (including 0-damage debuffs) targets the opponent. */
    val isBuff get() = effect.self
    val elements: Set<Element> get() = setOfNotNull(element, element2)
    val isFire get() = Element.FIRE in elements
    /** "Electric/Fighting" for combo moves. */
    val typeLabel get() = elements.joinToString("/") { it.label }
    val isPunch get() = this in PUNCHES
    val lunges get() = effect == Effect.LUNGE || alsoLunges

    /** e.g. "knockback", or "lunge + knockback" for slams. */
    val effectLabel: String
        get() = if (alsoLunges) "${Effect.LUNGE.label} + ${effect.label}" else effect.label

    /**
     * Energy a normal elemental move spends (scales with its power), so they can't be spammed.
     * Normal-type moves are free; Specials already empty the whole bar; Uniques have limited uses.
     */
    val energyCost: Int
        get() = if (category == MoveCategory.NORMAL && element != Element.NORMAL) (10 + damage).coerceIn(15, 35) else 0

    fun describe(): String = buildString {
        append(typeLabel).append(" · ")
        append(when { damage > 0 -> "$damage dmg"; isBuff -> "Buff"; else -> "Debuff" })
        if (effect != Effect.NONE && effect != Effect.GUARD) append(", ").append(effectLabel)
        append(", ${cooldownMs / 1000.0}s cd")
        if (energyCost > 0) append(", $energyCost energy")
    }
}

/** Fist moves: species that can't Punch can't learn these either. */
private val PUNCHES by lazy {
    setOf(Move.ICE_PUNCH, Move.THUNDER_PUNCH, Move.FIRE_PUNCH, Move.DRAIN_PUNCH, Move.DYNAMIC_PUNCH,
        Move.SKY_UPPERCUT, Move.AQUA_FIST, Move.KARATE_CHOP, Move.BRICK_BREAK, Move.CROSS_CHOP,
        Move.SUCKER_PUNCH, Move.COMET_PUNCH, Move.MEGA_PUNCH, Move.PUNCH, Move.QUICK_JAB)
}

object BattleRules {
    const val MAX_ENERGY = 100
    const val UNIQUE_USES = 2
}
data class MoveUnlock(val move: Move, val level: Int)

/**
 * Each species' passive, learnset and unique. A pet keeps [MAX_MOVES] learned moves;
 * slot 5 is its unique: one of two options per stage (Baby, Kid, Teen, Adult). Each stage also has its own passive.
 *
 * Learnable = Normal + own types + a few off-type picks, minus moves the species can't do.
 */
object SpeciesKit {
    const val MAX_MOVES = 4
    const val SLOTS = MAX_MOVES + 1

    /** The species' signature passive: its first Baby option. */
    fun passive(s: Species) = passiveOptions(s).first().first

    /** The species' own types (besides Normal). */
    fun affinity(s: Species): Set<Element> = when (s) {
        Species.SPROUT -> setOf(Element.GRASS, Element.GROUND)
        Species.KITTY -> setOf(Element.ELECTRIC, Element.ICE)
        Species.DRAKE -> setOf(Element.FIRE, Element.FLYING)
        Species.BUNNY -> setOf(Element.ICE, Element.GROUND, Element.FIGHTING)
        Species.PUP -> setOf(Element.GROUND, Element.FIRE)
        Species.OWL -> setOf(Element.FLYING, Element.ELECTRIC)
        Species.TURTLE -> setOf(Element.WATER, Element.GROUND)
        Species.FOX -> setOf(Element.FIRE, Element.ICE)
        Species.FROG -> setOf(Element.WATER, Element.GRASS)
        Species.BEAR -> setOf(Element.GROUND, Element.WATER, Element.FIGHTING)
        Species.PENGUIN -> setOf(Element.ICE, Element.WATER)
        Species.HEDGEHOG -> setOf(Element.GROUND, Element.GRASS)
        Species.BAT -> setOf(Element.FLYING, Element.GROUND)
        Species.DEER -> setOf(Element.GRASS, Element.ICE)
        Species.OTTER -> setOf(Element.WATER, Element.ELECTRIC)
        Species.PANDA -> setOf(Element.GRASS, Element.GROUND, Element.FIGHTING)
        Species.MONKEY -> setOf(Element.FIGHTING, Element.GRASS)
        Species.KANGAROO -> setOf(Element.FIGHTING, Element.GROUND)
        Species.EAGLE -> setOf(Element.FLYING, Element.FIGHTING)
        Species.SEAL -> setOf(Element.WATER, Element.ICE)
        Species.ELEPHANT -> setOf(Element.GROUND, Element.WATER)
        Species.TIGER -> setOf(Element.FIRE, Element.FIGHTING)
        Species.TILAPIA -> setOf(Element.WATER, Element.GRASS)
        Species.LIZARD -> setOf(Element.FIRE, Element.GROUND)
        Species.RACCOON -> setOf(Element.ELECTRIC, Element.ICE)
        Species.HAMSTER -> setOf(Element.ELECTRIC, Element.GRASS)
        Species.PARROT -> setOf(Element.FLYING, Element.FIRE)
        Species.AXOLOTL -> setOf(Element.WATER, Element.GRASS)
        Species.MOLE -> setOf(Element.GROUND, Element.ELECTRIC)
        Species.KOALA -> setOf(Element.GRASS, Element.ELECTRIC)
        Species.SHARK -> setOf(Element.WATER, Element.ICE)
        Species.CHAMELEON -> setOf(Element.GRASS, Element.FIRE)
        Species.BEAVER -> setOf(Element.GROUND, Element.WATER)
    }

    /** Off-type moves the species can still learn. */
    fun extras(s: Species): Set<Move> = when (s) {
        Species.SPROUT -> setOf(Move.WATER_GUN, Move.RAIN_DANCE, Move.AQUA_RING, Move.TWISTER)
        Species.KITTY -> setOf(Move.FIRE_FANG, Move.AERIAL_ACE, Move.BOUNCE, Move.DIG, Move.SAND_ATTACK)
        Species.DRAKE -> setOf(Move.ROCK_THROW, Move.DIG, Move.EARTHQUAKE, Move.THUNDER_FANG, Move.STONE_EDGE)
        Species.BUNNY -> setOf(Move.BOUNCE, Move.AERIAL_ACE, Move.NUZZLE, Move.BULLET_SEED, Move.SYNTHESIS)
        Species.PUP -> setOf(Move.THUNDER_FANG, Move.ICE_FANG, Move.WILD_CHARGE, Move.AQUA_JET)
        Species.OWL -> setOf(Move.ICY_WIND, Move.MIST, Move.FREEZE_RAY, Move.RAZOR_LEAF, Move.SANDSTORM)
        Species.TURTLE -> setOf(Move.ICY_WIND, Move.ICE_SHARD, Move.BLIZZARD, Move.SEED_BOMB, Move.MIRROR_ICE)
        Species.FOX -> setOf(Move.THUNDER_WAVE, Move.DIG, Move.BOUNCE, Move.AERIAL_ACE, Move.SHOCK_WAVE)
        Species.FROG -> setOf(Move.BOUNCE, Move.SAND_TOMB, Move.ICY_WIND, Move.ICE_FANG, Move.THUNDER_WAVE)
        Species.BEAR -> setOf(Move.ICE_PUNCH, Move.ICE_FANG, Move.FIRE_FANG, Move.THUNDER_FANG, Move.AVALANCHE)
        Species.PENGUIN -> setOf(Move.MUD_SLAP, Move.BULLDOZE, Move.SHOCK_WAVE)
        Species.HEDGEHOG -> setOf(Move.FIRE_FANG, Move.FLAME_WHEEL, Move.THUNDER_FANG)
        Species.BAT -> setOf(Move.ICY_WIND, Move.SHOCK_WAVE, Move.THUNDER_WAVE)
        Species.DEER -> setOf(Move.WATER_GUN, Move.RAIN_DANCE, Move.BOUNCE, Move.SAND_ATTACK)
        Species.OTTER -> setOf(Move.ICY_WIND, Move.ICE_FANG, Move.ROCK_THROW, Move.DIG)
        Species.PANDA -> setOf(Move.ICE_PUNCH, Move.FIRE_FANG, Move.WATERFALL, Move.AVALANCHE)
        Species.MONKEY -> setOf(Move.THUNDER_WAVE, Move.SHOCK_WAVE, Move.AERIAL_ACE, Move.DIG)
        Species.KANGAROO -> setOf(Move.BOUNCE, Move.AERIAL_ACE, Move.WATER_GUN)
        Species.EAGLE -> setOf(Move.ROCK_THROW, Move.SHOCK_WAVE, Move.ICY_WIND)
        Species.SEAL -> setOf(Move.MUD_SLAP, Move.SHOCK_WAVE, Move.BULLDOZE)
        Species.ELEPHANT -> setOf(Move.SEED_BOMB, Move.RAZOR_LEAF, Move.AVALANCHE, Move.BRAMBLE_SNARE)
        Species.TIGER -> setOf(Move.THUNDER_FANG, Move.ICE_FANG, Move.DIG, Move.BOUNCE)
        Species.TILAPIA -> setOf(Move.MUD_SLAP, Move.ICY_WIND, Move.THUNDER_WAVE)
        Species.LIZARD -> setOf(Move.THUNDER_FANG, Move.LEECH_SEED, Move.WATER_GUN, Move.AERIAL_ACE)
        Species.RACCOON -> setOf(Move.DIG, Move.MUD_SLAP, Move.FIRE_FANG, Move.SMOKESCREEN, Move.BOUNCE)
        Species.HAMSTER -> setOf(Move.DIG, Move.MUD_SLAP, Move.SAND_ATTACK, Move.ROCK_THROW)
        Species.PARROT -> setOf(Move.SHOCK_WAVE, Move.THUNDER_WAVE, Move.ICY_WIND, Move.SEED_BOMB)
        Species.AXOLOTL -> setOf(Move.ICY_WIND, Move.ICE_SHARD, Move.MIST, Move.MUD_SLAP)
        Species.MOLE -> setOf(Move.FIRE_FANG, Move.SMOKESCREEN, Move.SEED_BOMB, Move.ICE_FANG)
        Species.KOALA -> setOf(Move.MUD_SLAP, Move.STONE_SKIN, Move.RAIN_DANCE, Move.AQUA_RING)
        Species.SHARK -> setOf(Move.THUNDER_FANG, Move.FIRE_FANG, Move.BULLDOZE, Move.DIG)
        Species.CHAMELEON -> setOf(Move.THUNDER_WAVE, Move.SHOCK_WAVE, Move.AERIAL_ACE, Move.BUBBLE)
        Species.BEAVER -> setOf(Move.RAZOR_LEAF, Move.SEED_BOMB, Move.BRAMBLE_SNARE, Move.ICY_WIND)
    }

    /** Normal or own-type moves this species can't do (no fists, no wings, too slow...). */
    fun banned(s: Species): Set<Move> = when (s) {
        Species.SPROUT -> setOf(Move.BITE, Move.HYPER_FANG, Move.HOWL, Move.CLAW, Move.FURY_SWIPES, Move.BONE_RUSH)
        Species.KITTY -> setOf(Move.HOWL, Move.BODY_SLAM, Move.GIGA_IMPACT, Move.MEGA_PUNCH)
        Species.DRAKE -> setOf(Move.ZOOMIES, Move.TELEPORT, Move.QUICK_JAB)
        Species.BUNNY -> setOf(Move.BITE, Move.HYPER_FANG, Move.HOWL, Move.BODY_SLAM, Move.HEAD_SMASH, Move.BONE_RUSH)
        Species.PUP -> setOf(Move.TELEPORT, Move.PUNCH, Move.QUICK_JAB, Move.MEGA_PUNCH, Move.FISSURE)
        Species.OWL -> setOf(Move.PUNCH, Move.QUICK_JAB, Move.MEGA_PUNCH, Move.DOUBLE_KICK, Move.BODY_SLAM, Move.ZOOMIES)
        Species.TURTLE -> setOf(Move.ZOOMIES, Move.POUNCE, Move.QUICK_ATTACK, Move.TELEPORT, Move.DOUBLE_KICK, Move.AQUA_JET)
        Species.FOX -> setOf(Move.PUNCH, Move.MEGA_PUNCH, Move.BODY_SLAM, Move.GIGA_IMPACT, Move.ABSOLUTE_ZERO)
        Species.FROG -> setOf(Move.CLAW, Move.FURY_SWIPES, Move.BITE, Move.HYPER_FANG, Move.HOWL)
        Species.BEAR -> setOf(Move.ZOOMIES, Move.TELEPORT, Move.QUICK_ATTACK, Move.AQUA_JET, Move.DIVE)
        Species.PENGUIN -> setOf(Move.CLAW, Move.FURY_SWIPES, Move.POUNCE, Move.HOWL, Move.TELEPORT)
        Species.HEDGEHOG -> setOf(Move.PUNCH, Move.MEGA_PUNCH, Move.HOWL, Move.TELEPORT, Move.QUICK_JAB)
        Species.BAT -> setOf(Move.PUNCH, Move.MEGA_PUNCH, Move.BODY_SLAM, Move.DOUBLE_KICK, Move.QUICK_JAB, Move.GIGA_IMPACT,
            Move.DIG, Move.HEAD_SMASH)
        Species.DEER -> setOf(Move.PUNCH, Move.MEGA_PUNCH, Move.QUICK_JAB, Move.BITE, Move.HYPER_FANG, Move.CLAW,
            Move.FURY_SWIPES, Move.HOWL)
        Species.OTTER -> setOf(Move.HOWL, Move.TELEPORT, Move.GIGA_IMPACT, Move.HEAD_SMASH)
        Species.PANDA -> setOf(Move.ZOOMIES, Move.QUICK_ATTACK, Move.TELEPORT, Move.QUICK_JAB, Move.HOWL)
        Species.MONKEY -> setOf(Move.BITE, Move.HYPER_FANG, Move.BODY_SLAM, Move.HOWL)
        Species.KANGAROO -> setOf(Move.TELEPORT, Move.HOWL, Move.BITE, Move.HYPER_FANG)
        Species.EAGLE -> setOf(Move.PUNCH, Move.MEGA_PUNCH, Move.QUICK_JAB, Move.BODY_SLAM, Move.ZOOMIES, Move.HOWL)
        Species.SEAL -> setOf(Move.PUNCH, Move.MEGA_PUNCH, Move.QUICK_JAB, Move.DOUBLE_KICK, Move.POUNCE, Move.CLAW, Move.FURY_SWIPES)
        Species.ELEPHANT -> setOf(Move.ZOOMIES, Move.QUICK_ATTACK, Move.TELEPORT, Move.POUNCE, Move.PUNCH, Move.QUICK_JAB,
            Move.MEGA_PUNCH, Move.CLAW, Move.FURY_SWIPES, Move.AQUA_JET)
        Species.TIGER -> setOf(Move.HOWL, Move.TELEPORT, Move.HARDEN, Move.BARRIER)
        Species.TILAPIA -> setOf(Move.PUNCH, Move.MEGA_PUNCH, Move.QUICK_JAB, Move.CLAW, Move.FURY_SWIPES, Move.HOWL,
            Move.DOUBLE_KICK, Move.POUNCE, Move.BODY_SLAM)
        Species.LIZARD -> setOf(Move.PUNCH, Move.MEGA_PUNCH, Move.HOWL, Move.BODY_SLAM, Move.QUICK_JAB)
        Species.RACCOON -> setOf(Move.HOWL, Move.BODY_SLAM, Move.GIGA_IMPACT, Move.ABSOLUTE_ZERO)
        Species.HAMSTER -> setOf(Move.HOWL, Move.BODY_SLAM, Move.GIGA_IMPACT, Move.MEGA_PUNCH, Move.HEAD_SMASH)
        Species.PARROT -> setOf(Move.PUNCH, Move.MEGA_PUNCH, Move.QUICK_JAB, Move.HOWL, Move.DOUBLE_KICK, Move.BODY_SLAM)
        Species.AXOLOTL -> setOf(Move.HOWL, Move.CLAW, Move.FURY_SWIPES, Move.PUNCH, Move.MEGA_PUNCH, Move.ZOOMIES)
        Species.MOLE -> setOf(Move.HOWL, Move.ZOOMIES, Move.TELEPORT, Move.AQUA_JET)
        Species.KOALA -> setOf(Move.ZOOMIES, Move.QUICK_ATTACK, Move.QUICK_JAB, Move.HOWL, Move.TELEPORT, Move.VOLT_TACKLE)
        Species.SHARK -> setOf(Move.PUNCH, Move.MEGA_PUNCH, Move.QUICK_JAB, Move.CLAW, Move.FURY_SWIPES, Move.HOWL,
            Move.DOUBLE_KICK, Move.ZOOMIES, Move.POUNCE)
        Species.CHAMELEON -> setOf(Move.HOWL, Move.BODY_SLAM, Move.GIGA_IMPACT, Move.MEGA_PUNCH, Move.FLARE_BLITZ)
        Species.BEAVER -> setOf(Move.HOWL, Move.ZOOMIES, Move.TELEPORT, Move.POUNCE, Move.DIVE)
    }

    /**
     * The two unique options for each stage (Baby, Kid, Teen, Adult). The pet gets one of each pair,
     * picked by the player when it reaches the stage; the first is the default until then.
     */
    fun uniqueOptions(s: Species): List<Pair<Move, Move>> = when (s) {
        Species.SPROUT -> listOf(
            Move.SUN_RAY to Move.POLLEN_PUFF, Move.SOLAR_BEAM to Move.ROOT_BIND,
            Move.BLOOM_BURST to Move.GAIA_SHIELD, Move.SOLAR_FLARE to Move.WORLD_TREE,
        )
        Species.KITTY -> listOf(
            Move.SHADOW_SWIPE to Move.CAT_NAP, Move.SHADOW_CLAW to Move.HISS,
            Move.PHANTOM_POUNCE to Move.STATIC_PURR, Move.NIGHT_TERROR to Move.NINTH_LIFE,
        )
        Species.DRAKE -> listOf(
            Move.FLAME_BURST to Move.SMOKE_PUFF, Move.INFERNO to Move.DRAGON_ROAR,
            Move.WING_BLAZE to Move.SCALE_ARMOR, Move.DRAGON_FURY to Move.DRAGON_ASCENT,
        )
        Species.BUNNY -> listOf(
            Move.MOON_HOP to Move.THUMP, Move.MOON_KICK to Move.LUCKY_FOOT,
            Move.CRESCENT_KICK to Move.MOONLIGHT, Move.LUNAR_STRIKE to Move.ECLIPSE,
        )
        Species.PUP -> listOf(
            Move.RALLY_BARK to Move.PUPPY_EYES, Move.PACK_CALL to Move.FETCH_DASH,
            Move.GUARD_DOG to Move.FIRE_HOWL, Move.ALPHA_HOWL to Move.LOYAL_GUARDIAN,
        )
        Species.OWL -> listOf(
            Move.STATIC_EYE to Move.HOOT, Move.STORM_EYE to Move.SILENT_GLIDE,
            Move.FEATHER_VOLLEY to Move.WISE_GAZE, Move.TEMPEST_EYE to Move.NIGHT_WATCH,
        )
        Species.TURTLE -> listOf(
            Move.SHELL_UP to Move.SHELL_BASH, Move.FORTRESS to Move.AQUA_SHELL,
            Move.TIDAL_SHELL to Move.BEDROCK, Move.CITADEL to Move.ANCIENT_WAVE,
        )
        Species.FOX -> listOf(
            Move.WISP_FLAME to Move.FOX_TRICK, Move.FOXFIRE to Move.ILLUSION,
            Move.FROST_FOX to Move.MIRAGE_TAILS, Move.NINE_TAILS to Move.KITSUNE_VEIL,
        )
        Species.FROG -> listOf(
            Move.SPLASH_WAVE to Move.CROAK, Move.TIDAL_WAVE to Move.TONGUE_LASH,
            Move.LILY_LEAP to Move.RAIN_SONG, Move.TSUNAMI to Move.SWAMP_KING,
        )
        Species.BEAR -> listOf(
            Move.BEAR_SWIPE to Move.BEAR_HUG, Move.MAUL to Move.HIBERNATE,
            Move.HAMMER_PAW to Move.MIGHTY_ROAR, Move.GRIZZLY_RAMPAGE to Move.URSA_MAJOR,
        )
        Species.PENGUIN -> listOf(
            Move.FROST_SLIDE to Move.HUDDLE, Move.POLAR_CRASH to Move.SNOWBALL,
            Move.GLACIER_DIVE to Move.COLD_SNAP, Move.EMPEROR_BLIZZARD to Move.ICEBERG_WALL,
        )
        Species.HEDGEHOG -> listOf(
            Move.SPIKE_ROLL to Move.CURL_UP, Move.NEEDLE_STORM to Move.SPINY_SHIELD,
            Move.PIN_MISSILE to Move.HEDGE_MAZE, Move.QUILL_BARRAGE to Move.THORN_FORTRESS,
        )
        Species.BAT -> listOf(
            Move.ECHO_SHRIEK to Move.SONAR, Move.VAMPIRE_FANG to Move.NIGHT_FLIGHT,
            Move.LEECH_LIFE to Move.SONIC_SCREECH, Move.BLOOD_MOON to Move.DUSK_SWARM,
        )
        Species.DEER -> listOf(
            Move.ANTLER_JAB to Move.FAWN_LEAP, Move.VERDANT_CHARGE to Move.FOREST_BLESSING,
            Move.FROST_ANTLERS to Move.GRACE, Move.SPIRIT_STAMPEDE to Move.SACRED_GROVE,
        )
        Species.OTTER -> listOf(
            Move.PEBBLE_TOSS to Move.CLAM_SMASH, Move.RIVER_RUSH to Move.PLAYTIME,
            Move.VOLT_SPLASH to Move.FLOAT, Move.RAPID_CURRENT to Move.OTTER_PARTY,
        )
        Species.PANDA -> listOf(
            Move.BAMBOO_SMACK to Move.LAZY_ROLL, Move.BAMBOO_STORM to Move.PANDA_PALM,
            Move.BAMBOO_WALL to Move.KUNG_FU_KICK, Move.PANDA_QUAKE to Move.ZEN_MASTER,
        )
        Species.MONKEY -> listOf(
            Move.BANANA_TOSS to Move.MONKEY_BUSINESS, Move.VINE_SWING to Move.CHEST_BEAT,
            Move.JUNGLE_KICK to Move.PRANKSTER, Move.CANOPY_FURY to Move.APE_KING,
        )
        Species.KANGAROO -> listOf(
            Move.POUCH_JAB to Move.HOP_AWAY, Move.BOXING_FLURRY to Move.TAIL_STAND,
            Move.MEGA_KICK to Move.SAND_KICK, Move.OUTBACK_STOMP to Move.CHAMPION,
        )
        Species.EAGLE -> listOf(
            Move.TALON_GRAB to Move.KEEN_GLARE, Move.SKY_DIVE to Move.UPDRAFT,
            Move.AERIAL_STRIKE to Move.RAPTOR_CRY, Move.STORM_TALON to Move.SKY_SOVEREIGN,
        )
        Species.SEAL -> listOf(
            Move.FLIPPER_SLAP to Move.BARK_BARK, Move.ICE_FLOE to Move.ICE_BALL,
            Move.SEAL_SPIN to Move.BLUBBER_UP, Move.ARCTIC_TIDE to Move.POLAR_SPLASH,
        )
        Species.LIZARD -> listOf(
            Move.TAIL_LASH to Move.SUNBATHE, Move.MAGMA_TAIL to Move.SAND_SPIT,
            Move.MOLTEN_SCALES to Move.DUNE_LASH, Move.VOLCANO_ROAR to Move.BASILISK_GAZE,
        )
        Species.RACCOON -> listOf(
            Move.SNATCH to Move.SCAVENGE, Move.TRASH_TOSS to Move.MASKED_FEINT,
            Move.GARBAGE_SHOCK to Move.SNEAK, Move.MIDNIGHT_HEIST to Move.GRAND_LARCENY,
        )
        Species.HAMSTER -> listOf(
            Move.SEED_SPIT to Move.CHEEK_STUFF, Move.WHEEL_DASH to Move.SPARK_ROLL,
            Move.SUNFLOWER_SEED to Move.TURBO_WHEEL, Move.THUNDER_WHEEL to Move.HOARD,
        )
        Species.PARROT -> listOf(
            Move.SQUAWK to Move.MIMIC, Move.FEATHER_BLAZE to Move.TROPICAL_WIND,
            Move.PLUMAGE_FLARE to Move.ECHO_TALK, Move.PHOENIX_DIVE to Move.RAINBOW_WING,
        )
        Species.AXOLOTL -> listOf(
            Move.BUBBLE_GILL to Move.REGEN_BUBBLE, Move.TIDE_POOL to Move.GILL_MIST,
            Move.LOTUS_SHIELD to Move.CORAL_WHIP, Move.LOTUS_TORRENT to Move.REBIRTH,
        )
        Species.MOLE -> listOf(
            Move.BURROW_JAB to Move.MUD_FLICK, Move.DRILL_CLAW to Move.TUNNEL_NET,
            Move.SEISMIC_SENSE to Move.LIVE_WIRE, Move.TREMOR_SPARK to Move.EARTH_CORE,
        )
        Species.KOALA -> listOf(
            Move.EUCALYPTUS_DART to Move.SNUGGLE, Move.DROWSY_SLAM to Move.LULLABY,
            Move.GUM_LEAF to Move.STATIC_SNOOZE, Move.DREAM_STORM to Move.EUCALYPTUS_GROVE,
        )
        Species.SHARK -> listOf(
            Move.FIN_SLICE to Move.BLOOD_TRAIL, Move.JAW_LOCK to Move.RIPTIDE,
            Move.SHARK_FRENZY to Move.ICE_FIN, Move.FEEDING_FRENZY to Move.MEGALODON,
        )
        Species.CHAMELEON -> listOf(
            Move.TONGUE_SNAP to Move.CAMOUFLAGE, Move.MIRAGE_SWAP to Move.COLOR_FLASH,
            Move.STICKY_TONGUE to Move.BLEND_IN, Move.PRISM_BLAST to Move.SPECTRUM_VEIL,
        )
        Species.BEAVER -> listOf(
            Move.LOG_TOSS to Move.GNAW, Move.TIMBER_TRAP to Move.BEAVER_TAIL_SLAP,
            Move.LODGE to Move.FLOOD_GATE, Move.GREAT_DAM to Move.TIMBER_FALL,
        )
        Species.ELEPHANT -> listOf(
            Move.TRUNK_SLAP to Move.TRUNK_SPRAY, Move.TUSK_GORE to Move.MUD_SHOWER,
            Move.STAMPEDE to Move.TRUMPET, Move.MAMMOTH_QUAKE to Move.HERD_GUARDIAN,
        )
        Species.TIGER -> listOf(
            Move.STRIPE_SLASH to Move.TIGER_GROWL, Move.TIGER_POUNCE to Move.STALK,
            Move.BLAZING_CLAW to Move.PREDATOR_EYE, Move.SAVAGE_MAUL to Move.TIGER_KING,
        )
        Species.TILAPIA -> listOf(
            Move.FIN_FLICK to Move.BUBBLE_NEST, Move.SCHOOL_RUSH to Move.MOUTH_BROOD,
            Move.ALGAE_BLOOM to Move.SCALE_SHED, Move.NILE_FLOOD to Move.FISH_FRY,
        )
    }

    fun uniqueOptions(s: Species, stage: Stage): Pair<Move, Move>? =
        if (stage == Stage.EGG) null else uniqueOptions(s)[stage.ordinal - 1]

    /** Stage at which [m] is one of the species' unique options, or null. */
    fun uniqueStage(s: Species, m: Move): Stage? =
        uniqueOptions(s).indexOfFirst { m == it.first || m == it.second }.takeIf { it >= 0 }?.let { Stage.entries[it + 1] }

    /** Every unique the species could ever get. */
    fun allUniques(s: Species): List<Move> = uniqueOptions(s).flatMap { listOf(it.first, it.second) }

    /** The stage's unique: the picked option if [picks] holds one, otherwise the first option. */
    fun unique(s: Species, stage: Stage, picks: Collection<Move> = emptyList()): Move? =
        uniqueOptions(s, stage)?.let { (a, b) -> if (b in picks) b else a }

    /**
     * Two passive options per stage (Baby, Kid, Teen, Adult). When the pet hatches or evolves,
     * one of the stage's two is rolled at random. The Baby pair starts with the species' signature [passive].
     */
    fun passiveOptions(s: Species): List<Pair<Passive, Passive>> = PASSIVES.getValue(s)

    private val PASSIVES: Map<Species, List<Pair<Passive, Passive>>> = Passive.entries.groupBy { it.species }
        .mapValues { (_, ps) -> Stage.entries.drop(1).map { st -> ps.filter { it.stage == st }.let { it[0] to it[1] } } }

    fun passiveOptions(s: Species, stage: Stage): Pair<Passive, Passive> =
        passiveOptions(s)[(stage.ordinal - 1).coerceAtLeast(0)]

    /** The passive in play: [rolled] if it's one of the stage's options, otherwise the first option. */
    fun passiveFor(s: Species, stage: Stage, rolled: Passive?): Passive =
        passiveOptions(s, stage).let { (a, b) -> if (rolled == b) b else a }

    const val STARTERS = 2

    /** A new pet knows exactly [STARTERS] moves: Tackle and its first own-type attack. */
    fun starters(s: Species): List<Move> {
        val pool = Move.entries.filter { canLearn(s, it) && it.level <= 1 }
        val typed = affinity(s).firstNotNullOfOrNull { e -> pool.firstOrNull { e in it.elements && it.damage > 0 } }
        return (listOfNotNull(Move.TACKLE.takeIf { it in pool }, typed) + pool).distinct().take(STARTERS)
    }

    fun canLearn(s: Species, m: Move) =
        m.category != MoveCategory.UNIQUE && m !in banned(s) &&
            !(m.isPunch && Move.PUNCH in banned(s)) &&
            (m.element == Element.NORMAL || m.elements.any { it in affinity(s) } || m in extras(s))

    /**
     * Every move the species can ever learn, in unlock order. Level-1 moves beyond the
     * starters are spread over the next levels so a new pet starts with only [STARTERS].
     */
    fun learnset(s: Species): List<MoveUnlock> {
        val start = starters(s)
        var next = 2
        return Move.entries.filter { canLearn(s, it) }.sortedBy { it.level }.map {
            when {
                it in start -> MoveUnlock(it, 1)
                it.level <= 1 -> MoveUnlock(it, next++)
                else -> MoveUnlock(it, it.level)
            }
        }.sortedBy { it.level }
    }

    /** Battle buttons: up to [MAX_MOVES] valid learned moves, then the unique in slot 5 (picked via [moves]). */
    fun loadout(s: Species, stage: Stage, level: Int, moves: List<Move>): List<Move> =
        moves.distinct().filter { canLearn(s, it) && it.level <= level }.take(MAX_MOVES) + listOfNotNull(unique(s, stage, moves))
}
