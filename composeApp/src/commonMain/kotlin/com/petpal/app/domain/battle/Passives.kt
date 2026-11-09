package com.petpal.app.domain.battle

import com.petpal.app.domain.battle.Element.*
import com.petpal.app.domain.model.Species
import com.petpal.app.domain.model.Species.*
import com.petpal.app.domain.model.Stage
import com.petpal.app.domain.model.Stage.*
import kotlin.math.roundToInt

/** Building blocks of passives. Amounts are fractions (0.2 = 20%) unless noted. */
enum class Trait {
    REGEN, REGEN_LOW, REGEN_IDLE, // HP per second
    LAST_STAND, SNACK, BOOST, PUNCH, SPECIAL, BURN_TIME, SPEED, COOLDOWN, VANISH_CD, RANGE, MAX_HP,
    ARMOR, DODGE, UNSTOPPABLE, BURN_IMMUNE, THORNS, LIFESTEAL, EVADE_CD, ENERGY_RATE,
    START_ENERGY, ENERGY_STEAL, // flat energy
    WEAKEN_TOUCH, STATIC_TOUCH, FLAME_TOUCH, RESIST, ADAPT, DESPERATE, LOW_HP_POWER, EXECUTE, OPENER,
    SNIPER, BRAWLER, GUTS, BERSERK, CRIT, GRIT, ENDURANCE, HEAL_BOOST, IRON_WILL, STEADFAST,
    START_SHIELD, SHIELD_BOOST,
    FRIED, // Tilapia: comes back once as a Fried Tilapia that sizzles away
}

/** Fried Tilapia: HP it comes back with, and HP it loses every second. */
object Fried {
    const val HP = 30f
    const val DECAY_PER_SEC = 5f
}

data class Perk(val trait: Trait, val amount: Float = 0f, val elements: Set<Element> = emptySet()) {
    val text: String get() {
        val p = "${(amount * 100).roundToInt()}%"
        val n = ((amount * 10).roundToInt() / 10f).let { if (it % 1f == 0f) it.toInt().toString() else it.toString() }
        val els = elements.joinToString(" and ") { it.label }
        return when (trait) {
            Trait.REGEN -> "Regenerates $n HP every second."
            Trait.REGEN_LOW -> "Regenerates $n HP every second while below half HP."
            Trait.REGEN_IDLE -> "Regenerates $n HP every second after 4s without getting hit."
            Trait.LAST_STAND -> "Survives the first knockout blow with 1 HP."
            Trait.SNACK -> "Once per battle, heals $p HP when it drops below 40%."
            Trait.BOOST -> "$els moves deal $p more damage."
            Trait.PUNCH -> "Punching moves deal $p more damage."
            Trait.SPECIAL -> "Special and unique moves deal $p more damage."
            Trait.BURN_TIME -> "Its burns last $p longer."
            Trait.SPEED -> "Moves $p faster."
            Trait.COOLDOWN -> "All cooldowns are $p shorter."
            Trait.VANISH_CD -> "Vanishing moves (Dig, Bounce...) recharge $p faster."
            Trait.RANGE -> "All moves reach $p further."
            Trait.MAX_HP -> "$p more max HP."
            Trait.ARMOR -> "Takes $p less damage from every hit."
            Trait.DODGE -> "$p chance to dodge an attack completely."
            Trait.UNSTOPPABLE -> "Immune to slow and stun."
            Trait.BURN_IMMUNE -> "Can't be burned."
            Trait.THORNS -> "Close-range attackers take $p of the damage back."
            Trait.LIFESTEAL -> "Heals for $p of all damage dealt."
            Trait.EVADE_CD -> "Evade recharges $p sooner."
            Trait.ENERGY_RATE -> "Energy charges $p faster."
            Trait.START_ENERGY -> "Starts every battle with $n extra energy."
            Trait.ENERGY_STEAL -> "Every hit steals $n energy from the target."
            Trait.WEAKEN_TOUCH -> "Hits have a $p chance to weaken the target for 3s."
            Trait.STATIC_TOUCH -> "Close-range attackers have a $p chance to be paralyzed."
            Trait.FLAME_TOUCH -> "Close-range attackers have a $p chance to be burned."
            Trait.RESIST -> "Takes $p less damage from $els moves."
            Trait.ADAPT -> "Takes $p less damage from the type it was last hit by."
            Trait.DESPERATE -> "Below 30% HP: deals $p more damage and moves $p faster."
            Trait.LOW_HP_POWER -> "Deals $p more damage while below half HP."
            Trait.EXECUTE -> "Deals $p more damage to targets below half HP."
            Trait.OPENER -> "Deals $p more damage to targets above 70% HP."
            Trait.SNIPER -> "Hits from long range deal $p more damage."
            Trait.BRAWLER -> "Close-range hits deal $p more damage."
            Trait.GUTS -> "Deals $p more damage while burned, slowed or paralyzed."
            Trait.BERSERK -> "Deals up to $p more damage the lower its HP."
            Trait.CRIT -> "$p chance to land a critical hit for 1.5x damage."
            Trait.GRIT -> "Takes $p less damage while below half HP."
            Trait.ENDURANCE -> "Takes $p less damage from burn, leech seed and terrain."
            Trait.HEAL_BOOST -> "Healing moves heal $p more."
            Trait.IRON_WILL -> "Can't be weakened or disrupted."
            Trait.STEADFAST -> "Can't be knocked back or pulled."
            Trait.START_SHIELD -> "Starts every battle with a shield worth $p of its max HP."
            Trait.SHIELD_BOOST -> "Its shields are $p stronger."
            Trait.FRIED -> "When knocked out, it comes back as a Fried Tilapia with ${Fried.HP.toInt()} HP that loses ${Fried.DECAY_PER_SEC.toInt()} HP every second."
        }
    }
}

private fun p(t: Trait, a: Float = 0f, vararg e: Element) = Perk(t, a, e.toSet())
private fun regen(a: Float) = p(Trait.REGEN, a)
private fun regenLow(a: Float) = p(Trait.REGEN_LOW, a)
private fun regenIdle(a: Float) = p(Trait.REGEN_IDLE, a)
private fun lastStand() = p(Trait.LAST_STAND)
private fun snack(a: Float) = p(Trait.SNACK, a)
private fun boost(a: Float, vararg e: Element) = p(Trait.BOOST, a, *e)
private fun punch(a: Float) = p(Trait.PUNCH, a)
private fun special(a: Float) = p(Trait.SPECIAL, a)
private fun burnTime(a: Float) = p(Trait.BURN_TIME, a)
private fun speed(a: Float) = p(Trait.SPEED, a)
private fun cooldown(a: Float) = p(Trait.COOLDOWN, a)
private fun vanishCd(a: Float) = p(Trait.VANISH_CD, a)
private fun range(a: Float) = p(Trait.RANGE, a)
private fun maxHp(a: Float) = p(Trait.MAX_HP, a)
private fun armor(a: Float) = p(Trait.ARMOR, a)
private fun dodge(a: Float) = p(Trait.DODGE, a)
private fun unstoppable() = p(Trait.UNSTOPPABLE)
private fun burnImmune() = p(Trait.BURN_IMMUNE)
private fun thorns(a: Float) = p(Trait.THORNS, a)
private fun lifesteal(a: Float) = p(Trait.LIFESTEAL, a)
private fun evadeCd(a: Float) = p(Trait.EVADE_CD, a)
private fun energyRate(a: Float) = p(Trait.ENERGY_RATE, a)
private fun startEnergy(a: Float) = p(Trait.START_ENERGY, a)
private fun energySteal(a: Float) = p(Trait.ENERGY_STEAL, a)
private fun weakenTouch(a: Float) = p(Trait.WEAKEN_TOUCH, a)
private fun staticTouch(a: Float) = p(Trait.STATIC_TOUCH, a)
private fun flameTouch(a: Float) = p(Trait.FLAME_TOUCH, a)
private fun resist(a: Float, vararg e: Element) = p(Trait.RESIST, a, *e)
private fun adapt(a: Float) = p(Trait.ADAPT, a)
private fun desperate(a: Float) = p(Trait.DESPERATE, a)
private fun lowHpPower(a: Float) = p(Trait.LOW_HP_POWER, a)
private fun execute(a: Float) = p(Trait.EXECUTE, a)
private fun opener(a: Float) = p(Trait.OPENER, a)
private fun sniper(a: Float) = p(Trait.SNIPER, a)
private fun brawler(a: Float) = p(Trait.BRAWLER, a)
private fun guts(a: Float) = p(Trait.GUTS, a)
private fun berserk(a: Float) = p(Trait.BERSERK, a)
private fun crit(a: Float) = p(Trait.CRIT, a)
private fun grit(a: Float) = p(Trait.GRIT, a)
private fun endurance(a: Float) = p(Trait.ENDURANCE, a)
private fun healBoost(a: Float) = p(Trait.HEAL_BOOST, a)
private fun ironWill() = p(Trait.IRON_WILL)
private fun steadfast() = p(Trait.STEADFAST)
private fun startShield(a: Float) = p(Trait.START_SHIELD, a)
private fun shieldBoost(a: Float) = p(Trait.SHIELD_BOOST, a)
private fun fried() = p(Trait.FRIED)

/**
 * Every passive belongs to exactly one species and stage; each stage has two (listed A then B).
 * The species' signature is its first Baby passive.
 */
enum class Passive(val species: Species, val stage: Stage, vararg perks: Perk) {
    PHOTOSYNTHESIS(SPROUT, BABY, regen(0.6f)),
    SEEDLING_VIGOR(SPROUT, BABY, regenLow(1f), maxHp(0.05f)),
    DEEP_ROOTS(SPROUT, KID, boost(0.2f,GRASS), steadfast()),
    LOAM_GRIP(SPROUT, KID, boost(0.2f,GROUND), endurance(0.3f)),
    SAP_MENDER(SPROUT, TEEN, healBoost(0.4f), regen(0.3f)),
    BARK_SKIN(SPROUT, TEEN, armor(0.12f), maxHp(0.08f)),
    ANCIENT_GROVE(SPROUT, ADULT, regenIdle(2.5f), ironWill()),
    WORLD_TREE(SPROUT, ADULT, maxHp(0.2f), regen(0.4f)),
    NINE_LIVES(KITTY, BABY, lastStand()),
    CURIOUS_PAWS(KITTY, BABY, crit(0.1f), speed(0.05f)),
    STATIC_WHISKERS(KITTY, KID, boost(0.2f,ELECTRIC), staticTouch(0.15f)),
    FROST_PURR(KITTY, KID, boost(0.2f,ICE), dodge(0.05f)),
    CATS_GRACE(KITTY, TEEN, dodge(0.12f), evadeCd(0.25f)),
    POUNCE(KITTY, TEEN, opener(0.25f), speed(0.08f)),
    PROWLER(KITTY, ADULT, execute(0.25f), crit(0.08f)),
    PURRFECT_FORM(KITTY, ADULT, lastStand(), special(0.15f)),
    BLAZE(DRAKE, BABY, boost(0.25f,FIRE), burnTime(1f)),
    EMBER_SCALES(DRAKE, BABY, flameTouch(0.25f), armor(0.05f)),
    UPDRAFT(DRAKE, KID, boost(0.2f,FLYING), speed(0.08f)),
    KINDLED_WINGS(DRAKE, KID, boost(0.15f,FIRE,FLYING), burnTime(0.5f)),
    DRAGON_HIDE(DRAKE, TEEN, resist(0.25f,FIRE,ICE), maxHp(0.1f)),
    FURY_FLAME(DRAKE, TEEN, berserk(0.3f), boost(0.1f,FIRE)),
    INFERNO_LORD(DRAKE, ADULT, special(0.2f), burnTime(1f)),
    SKY_TYRANT(DRAKE, ADULT, desperate(0.25f), range(0.1f)),
    QUICK_FEET(BUNNY, BABY, speed(0.25f), cooldown(0.1f)),
    BUNNY_HOP(BUNNY, BABY, evadeCd(0.3f), dodge(0.05f)),
    SNOWBOUND(BUNNY, KID, boost(0.2f,ICE), speed(0.05f)),
    BURROW_KICK(BUNNY, KID, boost(0.15f,GROUND,FIGHTING), vanishCd(0.2f)),
    SPRING_LEGS(BUNNY, TEEN, brawler(0.15f), evadeCd(0.2f)),
    LUCKY_FOOT(BUNNY, TEEN, crit(0.12f), dodge(0.06f)),
    MOONLEAP(BUNNY, ADULT, speed(0.2f), opener(0.2f)),
    THUMPER(BUNNY, ADULT, boost(0.2f,FIGHTING), guts(0.2f)),
    LOYAL_HEART(PUP, BABY, lowHpPower(0.25f)),
    WAGGING_TAIL(PUP, BABY, energyRate(0.15f), regen(0.2f)),
    DIGGER(PUP, KID, boost(0.2f,GROUND), vanishCd(0.15f)),
    WARM_COAT(PUP, KID, boost(0.2f,FIRE), endurance(0.3f)),
    PACK_SPIRIT(PUP, TEEN, healBoost(0.3f), grit(0.15f)),
    TUG_OF_WAR(PUP, TEEN, brawler(0.12f), steadfast()),
    GUARDIAN_HOUND(PUP, ADULT, grit(0.2f), lowHpPower(0.2f)),
    HOWL(PUP, ADULT, weakenTouch(0.2f), opener(0.15f)),
    KEEN_EYE(OWL, BABY, range(0.2f)),
    NIGHT_VISION(OWL, BABY, sniper(0.15f), crit(0.05f)),
    SILENT_WINGS(OWL, KID, boost(0.2f,FLYING), dodge(0.06f)),
    STORM_FEATHERS(OWL, KID, boost(0.2f,ELECTRIC), range(0.08f)),
    SAGE_MIND(OWL, TEEN, special(0.2f), ironWill()),
    HOOT(OWL, TEEN, weakenTouch(0.2f), range(0.1f)),
    ELDER_OWL(OWL, ADULT, sniper(0.25f), cooldown(0.1f)),
    MOONLIT_HUNTER(OWL, ADULT, crit(0.12f), execute(0.2f)),
    HARD_SHELL(TURTLE, BABY, armor(0.2f)),
    SHELL_TUCK(TURTLE, BABY, grit(0.2f), regenIdle(1f)),
    TIDE_SHELL(TURTLE, KID, boost(0.2f,WATER), armor(0.05f)),
    MUD_SHELL(TURTLE, KID, boost(0.2f,GROUND), steadfast()),
    SLOW_AND_STEADY(TURTLE, TEEN, endurance(0.5f), maxHp(0.1f)),
    MOSSY_BACK(TURTLE, TEEN, regen(0.4f), armor(0.08f)),
    FORTRESS(TURTLE, ADULT, startShield(0.2f), ironWill()),
    ANCIENT_SHELL(TURTLE, ADULT, armor(0.15f), maxHp(0.15f)),
    TRICKSTER(FOX, BABY, dodge(0.2f)),
    SLY_GRIN(FOX, BABY, energySteal(5f), crit(0.05f)),
    FOXFIRE(FOX, KID, boost(0.2f,FIRE), dodge(0.05f)),
    FROST_TAIL(FOX, KID, boost(0.2f,ICE), weakenTouch(0.1f)),
    ILLUSION(FOX, TEEN, dodge(0.1f), adapt(0.2f)),
    CUNNING(FOX, TEEN, special(0.15f), energyRate(0.1f)),
    NINE_TAILS(FOX, ADULT, boost(0.15f,FIRE,ICE), dodge(0.1f)),
    FOX_GUILE(FOX, ADULT, crit(0.1f), energySteal(8f)),
    SLIPPERY(FROG, BABY, unstoppable()),
    DAMP_SKIN(FROG, BABY, regenLow(0.8f), resist(0.2f,FIRE)),
    LILY_PAD(FROG, KID, boost(0.2f,GRASS), regen(0.3f)),
    RAINCALLER(FROG, KID, boost(0.2f,WATER), energyRate(0.1f)),
    LONG_TONGUE(FROG, TEEN, range(0.15f), energySteal(4f)),
    POND_HOPPER(FROG, TEEN, evadeCd(0.3f), speed(0.08f)),
    TOAD_KING(FROG, ADULT, maxHp(0.15f), thorns(0.1f)),
    MUCUS_COAT(FROG, ADULT, unstoppable(), endurance(0.4f)),
    THICK_FUR(BEAR, BABY, maxHp(0.2f)),
    CUB_HUG(BEAR, BABY, brawler(0.1f), grit(0.1f)),
    SALMON_SWIPE(BEAR, KID, boost(0.2f,WATER), brawler(0.08f)),
    MAULER(BEAR, KID, boost(0.2f,FIGHTING), berserk(0.15f)),
    HIBERNATE(BEAR, TEEN, regenIdle(2f), maxHp(0.1f)),
    MAMA_BEAR(BEAR, TEEN, lowHpPower(0.2f), grit(0.15f)),
    GRIZZLY(BEAR, ADULT, berserk(0.35f), steadfast()),
    KODIAK(BEAR, ADULT, maxHp(0.25f), armor(0.08f)),
    ICE_BODY(PENGUIN, BABY, burnImmune(), boost(0.15f,ICE)),
    WADDLE(PENGUIN, BABY, steadfast(), resist(0.2f,ICE)),
    BELLY_SLIDE(PENGUIN, KID, boost(0.2f,ICE), speed(0.1f)),
    DEEP_DIVER(PENGUIN, KID, boost(0.2f,WATER), vanishCd(0.2f)),
    HUDDLE(PENGUIN, TEEN, endurance(0.4f), healBoost(0.25f)),
    TUXEDO(PENGUIN, TEEN, armor(0.1f), ironWill()),
    EMPEROR(PENGUIN, ADULT, maxHp(0.15f), grit(0.15f)),
    BLIZZARD_HEART(PENGUIN, ADULT, boost(0.2f,ICE,WATER), unstoppable()),
    SPIKY(HEDGEHOG, BABY, thorns(0.2f)),
    CURL_UP(HEDGEHOG, BABY, armor(0.1f), thorns(0.08f)),
    BRAMBLE_QUILLS(HEDGEHOG, KID, boost(0.2f,GRASS), thorns(0.1f)),
    BURROW_NOSE(HEDGEHOG, KID, boost(0.2f,GROUND), vanishCd(0.25f)),
    STATIC_QUILLS(HEDGEHOG, TEEN, staticTouch(0.2f), thorns(0.1f)),
    PRICKLY_MOOD(HEDGEHOG, TEEN, guts(0.25f), armor(0.05f)),
    PINCUSHION(HEDGEHOG, ADULT, thorns(0.3f), grit(0.1f)),
    SPINE_STORM(HEDGEHOG, ADULT, brawler(0.15f), thorns(0.15f)),
    VAMPIRE(BAT, BABY, lifesteal(0.1f)),
    ECHOLOCATION(BAT, BABY, range(0.1f), dodge(0.08f)),
    NIGHT_WINGS(BAT, KID, boost(0.2f,FLYING), lifesteal(0.04f)),
    CAVE_DWELLER(BAT, KID, boost(0.2f,GROUND), regenIdle(1f)),
    SONAR_PING(BAT, TEEN, sniper(0.2f), weakenTouch(0.1f)),
    BLOODLUST(BAT, TEEN, execute(0.2f), lifesteal(0.06f)),
    NOSFERATU(BAT, ADULT, lifesteal(0.15f), desperate(0.15f)),
    NIGHT_TERROR(BAT, ADULT, weakenTouch(0.25f), crit(0.08f)),
    FLEET(DEER, BABY, evadeCd(0.5f)),
    FAWN_STEP(DEER, BABY, speed(0.1f), dodge(0.05f)),
    MEADOW_GRAZE(DEER, KID, boost(0.2f,GRASS), regen(0.25f)),
    FROST_ANTLER(DEER, KID, boost(0.2f,ICE), brawler(0.08f)),
    FOREST_SPIRIT(DEER, TEEN, healBoost(0.5f), regenLow(0.6f)),
    WATCHFUL(DEER, TEEN, range(0.12f), evadeCd(0.2f)),
    STAG_CHARGE(DEER, ADULT, opener(0.3f), steadfast()),
    SPIRIT_DEER(DEER, ADULT, regen(0.5f), dodge(0.08f)),
    PLAYFUL(OTTER, BABY, energyRate(0.3f)),
    ROCK_CRACKER(OTTER, BABY, brawler(0.1f), energyRate(0.1f)),
    RIVER_DANCE(OTTER, KID, boost(0.2f,WATER), speed(0.06f)),
    EEL_FRIEND(OTTER, KID, boost(0.2f,ELECTRIC), energySteal(4f)),
    SLICK_FUR(OTTER, TEEN, unstoppable(), speed(0.05f)),
    FLOAT_NAP(OTTER, TEEN, regenIdle(1.5f), energyRate(0.1f)),
    TIDE_TRICKSTER(OTTER, ADULT, energySteal(6f), dodge(0.08f)),
    RIVER_KING(OTTER, ADULT, special(0.2f), energyRate(0.15f)),
    BAMBOO_SNACK(PANDA, BABY, snack(0.2f)),
    ROLY_POLY(PANDA, BABY, maxHp(0.1f), steadfast()),
    BAMBOO_STAFF(PANDA, KID, boost(0.2f,GRASS), brawler(0.06f)),
    KUNG_FU(PANDA, KID, boost(0.2f,FIGHTING), evadeCd(0.2f)),
    LAZY_DAY(PANDA, TEEN, regenIdle(2f), endurance(0.3f)),
    BIG_APPETITE(PANDA, TEEN, snack(0.3f), maxHp(0.05f)),
    ZEN_MASTER(PANDA, ADULT, ironWill(), special(0.15f)),
    DRAGON_PANDA(PANDA, ADULT, punch(0.25f), snack(0.2f)),
    SUN_SCALES(LIZARD, BABY, resist(0.3f,FIRE,ICE)),
    TAIL_DROP(LIZARD, BABY, dodge(0.1f), regenLow(0.6f)),
    SUNBATH(LIZARD, KID, boost(0.2f,FIRE), regen(0.3f)),
    SAND_RUNNER(LIZARD, KID, boost(0.2f,GROUND), speed(0.08f)),
    COLD_BLOOD(LIZARD, TEEN, endurance(0.5f), burnImmune()),
    BASKING(LIZARD, TEEN, startEnergy(20f), regen(0.2f)),
    MAGMA_SKIN(LIZARD, ADULT, flameTouch(0.3f), resist(0.2f,FIRE,ICE)),
    KOMODO(LIZARD, ADULT, execute(0.25f), weakenTouch(0.15f)),
    STICKY_PAWS(RACCOON, BABY, energySteal(8f)),
    MASKED_BANDIT(RACCOON, BABY, dodge(0.08f), energySteal(3f)),
    WIRE_CHEWER(RACCOON, KID, boost(0.2f,ELECTRIC), energySteal(3f)),
    ICEBOX_RAIDER(RACCOON, KID, boost(0.2f,ICE), startEnergy(15f)),
    TRASH_PANDA(RACCOON, TEEN, regenLow(0.8f), crit(0.06f)),
    NIMBLE_FINGERS(RACCOON, TEEN, cooldown(0.1f), energySteal(4f)),
    MASTER_THIEF(RACCOON, ADULT, energySteal(12f), speed(0.05f)),
    SCAVENGER(RACCOON, ADULT, lifesteal(0.08f), energyRate(0.15f)),
    CHEEK_POUCH(HAMSTER, BABY, startEnergy(30f)),
    WHEEL_RUNNER(HAMSTER, BABY, energyRate(0.2f), speed(0.05f)),
    SPARK_WHEEL(HAMSTER, KID, boost(0.2f,ELECTRIC), energyRate(0.1f)),
    SEED_HOARD(HAMSTER, KID, boost(0.2f,GRASS), startEnergy(15f)),
    FLUFFBALL(HAMSTER, TEEN, maxHp(0.1f), dodge(0.06f)),
    STASH(HAMSTER, TEEN, snack(0.25f), startEnergy(10f)),
    HAMSTER_DASH(HAMSTER, ADULT, speed(0.15f), cooldown(0.1f)),
    BIG_CHEEKS(HAMSTER, ADULT, startEnergy(40f), energyRate(0.1f)),
    CHATTER(PARROT, BABY, weakenTouch(0.25f)),
    MIMIC(PARROT, BABY, adapt(0.2f), special(0.1f)),
    TROPIC_WINGS(PARROT, KID, boost(0.2f,FLYING), weakenTouch(0.1f)),
    PHOENIX_PLUME(PARROT, KID, boost(0.2f,FIRE), regenLow(0.6f)),
    SQUAWK(PARROT, TEEN, weakenTouch(0.2f), speed(0.06f)),
    BRIGHT_FEATHERS(PARROT, TEEN, crit(0.08f), dodge(0.08f)),
    ECHO_SONG(PARROT, ADULT, weakenTouch(0.3f), ironWill()),
    SUNBIRD(PARROT, ADULT, boost(0.15f,FIRE,FLYING), healBoost(0.3f)),
    REGROW(AXOLOTL, BABY, regenLow(1.2f)),
    GILL_FRILLS(AXOLOTL, BABY, regen(0.3f), resist(0.2f,WATER)),
    STREAM_SMILE(AXOLOTL, KID, boost(0.2f,WATER), regenLow(0.5f)),
    ALGAE_COAT(AXOLOTL, KID, boost(0.2f,GRASS), endurance(0.3f)),
    LIMB_REGEN(AXOLOTL, TEEN, regenLow(1.5f), healBoost(0.2f)),
    NEOTENY(AXOLOTL, TEEN, maxHp(0.12f), regen(0.3f)),
    ENDLESS_SMILE(AXOLOTL, ADULT, lastStand(), regenLow(1f)),
    LAKE_GUARDIAN(AXOLOTL, ADULT, regen(0.6f), ironWill()),
    BURROWER(MOLE, BABY, vanishCd(0.3f)),
    DIGGING_CLAWS(MOLE, BABY, brawler(0.12f), vanishCd(0.1f)),
    TUNNELER(MOLE, KID, boost(0.2f,GROUND), armor(0.05f)),
    FUSE_BOX(MOLE, KID, boost(0.2f,ELECTRIC), staticTouch(0.1f)),
    EARTHEN_HIDE(MOLE, TEEN, armor(0.12f), steadfast()),
    BLIND_FURY(MOLE, TEEN, berserk(0.3f), ironWill()),
    DEEP_DELVER(MOLE, ADULT, vanishCd(0.4f), opener(0.2f)),
    SEISMIC_SENSE(MOLE, ADULT, dodge(0.1f), brawler(0.1f)),
    SNOOZE(KOALA, BABY, regenIdle(2f)),
    EUCALYPTUS(KOALA, BABY, regen(0.3f), endurance(0.2f)),
    GUM_LEAF(KOALA, KID, boost(0.2f,GRASS), regenIdle(1f)),
    STATIC_NAP(KOALA, KID, boost(0.2f,ELECTRIC), regenIdle(1f)),
    TREE_HUGGER(KOALA, TEEN, steadfast(), grit(0.15f)),
    DROWSY(KOALA, TEEN, startShield(0.1f), regenIdle(1.5f)),
    DEEP_SLEEP(KOALA, ADULT, regenIdle(3.5f)),
    POWER_NAP(KOALA, ADULT, energyRate(0.25f), regenIdle(1f)),
    BLOOD_SCENT(SHARK, BABY, execute(0.3f)),
    ROUGH_SKIN(SHARK, BABY, thorns(0.1f), armor(0.05f)),
    RIPTIDE(SHARK, KID, boost(0.2f,WATER), execute(0.1f)),
    COLD_DEEP(SHARK, KID, boost(0.2f,ICE), armor(0.05f)),
    FEEDING_FRENZY(SHARK, TEEN, berserk(0.25f), lifesteal(0.05f)),
    APEX_PREDATOR(SHARK, TEEN, opener(0.2f), brawler(0.1f)),
    MEGALODON(SHARK, ADULT, execute(0.35f), maxHp(0.1f)),
    JAWS(SHARK, ADULT, brawler(0.2f), lifesteal(0.08f)),
    COLOR_SHIFT(CHAMELEON, BABY, adapt(0.3f)),
    CAMOUFLAGE(CHAMELEON, BABY, dodge(0.12f)),
    LEAF_SKIN(CHAMELEON, KID, boost(0.2f,GRASS), adapt(0.1f)),
    EMBER_TONGUE(CHAMELEON, KID, boost(0.2f,FIRE), range(0.1f)),
    SLOW_STALK(CHAMELEON, TEEN, opener(0.2f), dodge(0.06f)),
    PRISM_SCALES(CHAMELEON, TEEN, adapt(0.2f), armor(0.08f)),
    VANISHING_ACT(CHAMELEON, ADULT, dodge(0.15f), vanishCd(0.2f)),
    MASTER_OF_HUES(CHAMELEON, ADULT, adapt(0.35f), special(0.1f)),
    DAM_BUILDER(BEAVER, BABY, startShield(0.15f), shieldBoost(0.5f)),
    TIMBER_TEETH(BEAVER, BABY, brawler(0.1f), armor(0.05f)),
    MUD_PACK(BEAVER, KID, boost(0.2f,GROUND), startShield(0.05f)),
    RIVER_DAM(BEAVER, KID, boost(0.2f,WATER), shieldBoost(0.3f)),
    LODGE(BEAVER, TEEN, armor(0.1f), shieldBoost(0.3f)),
    TAIL_SLAP(BEAVER, TEEN, steadfast(), weakenTouch(0.1f)),
    MASTER_BUILDER(BEAVER, ADULT, startShield(0.25f), ironWill()),
    FLOODGATE(BEAVER, ADULT, shieldBoost(0.5f), healBoost(0.3f)),
    IRON_FIST(MONKEY, BABY, punch(0.25f)),
    BANANA_BREAK(MONKEY, BABY, snack(0.15f), energyRate(0.1f)),
    JUNGLE_SWING(MONKEY, KID, boost(0.2f,GRASS), speed(0.08f)),
    MONKEY_FIST(MONKEY, KID, boost(0.2f,FIGHTING), crit(0.05f)),
    MISCHIEF(MONKEY, TEEN, energySteal(5f), dodge(0.06f)),
    SHOWOFF(MONKEY, TEEN, crit(0.1f), guts(0.2f)),
    SIMIAN_KING(MONKEY, ADULT, punch(0.3f), cooldown(0.08f)),
    JUNGLE_LORD(MONKEY, ADULT, boost(0.15f,FIGHTING,GRASS), evadeCd(0.3f)),
    STEADFAST(KANGAROO, BABY, steadfast()),
    POUCH_GUARD(KANGAROO, BABY, grit(0.2f), startEnergy(10f)),
    BOXER(KANGAROO, KID, boost(0.2f,FIGHTING), punch(0.1f)),
    OUTBACK(KANGAROO, KID, boost(0.2f,GROUND), guts(0.15f)),
    BIG_LEAP(KANGAROO, TEEN, evadeCd(0.35f), opener(0.1f)),
    TAIL_BALANCE(KANGAROO, TEEN, steadfast(), punch(0.15f)),
    CHAMPION_BOXER(KANGAROO, ADULT, punch(0.2f), guts(0.25f)),
    MOB_BOSS(KANGAROO, ADULT, maxHp(0.15f), brawler(0.15f)),
    FIRST_STRIKE(EAGLE, BABY, opener(0.25f)),
    TALON_GRIP(EAGLE, BABY, brawler(0.1f), crit(0.05f)),
    THERMAL_RIDER(EAGLE, KID, boost(0.2f,FLYING), range(0.08f)),
    DIVE_BOMB(EAGLE, KID, boost(0.2f,FIGHTING), opener(0.1f)),
    EAGLE_EYE(EAGLE, TEEN, sniper(0.25f), range(0.1f)),
    SKY_SOVEREIGN(EAGLE, TEEN, speed(0.1f), special(0.15f)),
    APEX_TALONS(EAGLE, ADULT, execute(0.25f), opener(0.15f)),
    STORM_CROWN(EAGLE, ADULT, crit(0.12f), sniper(0.15f)),
    BLUBBER(SEAL, BABY, resist(0.3f,WATER,ICE)),
    FLIPPER_FLOP(SEAL, BABY, energyRate(0.15f), maxHp(0.05f)),
    ICE_FLOE(SEAL, KID, boost(0.2f,ICE), maxHp(0.05f)),
    WAVE_RIDER(SEAL, KID, boost(0.2f,WATER), dodge(0.05f)),
    BEACH_NAP(SEAL, TEEN, regenIdle(1.8f), maxHp(0.08f)),
    WHISKER_SENSE(SEAL, TEEN, dodge(0.08f), range(0.08f)),
    WALRUS_HIDE(SEAL, ADULT, maxHp(0.2f), endurance(0.4f)),
    CIRCUS_STAR(SEAL, ADULT, energyRate(0.2f), crit(0.08f)),
    TOUGH_HIDE(ELEPHANT, BABY, armor(0.12f)),
    MUD_BATH(ELEPHANT, BABY, endurance(0.3f), resist(0.2f,FIRE)),
    TRUNK_POWER(ELEPHANT, KID, boost(0.2f,GROUND), brawler(0.08f)),
    WATER_HOSE(ELEPHANT, KID, boost(0.2f,WATER), maxHp(0.06f)),
    NEVER_FORGETS(ELEPHANT, TEEN, adapt(0.25f), ironWill()),
    HEAVYWEIGHT(ELEPHANT, TEEN, maxHp(0.15f), steadfast()),
    MATRIARCH(ELEPHANT, ADULT, maxHp(0.2f), healBoost(0.3f)),
    TITAN(ELEPHANT, ADULT, armor(0.15f), brawler(0.15f)),
    STRIPES(TIGER, BABY, dodge(0.08f), opener(0.1f)),
    CUB_CLAWS(TIGER, BABY, brawler(0.12f)),
    TIGER_BLAZE(TIGER, KID, boost(0.2f,FIRE), crit(0.05f)),
    STRIPED_FIST(TIGER, KID, boost(0.2f,FIGHTING), speed(0.05f)),
    AMBUSH(TIGER, TEEN, opener(0.3f)),
    PROWL(TIGER, TEEN, speed(0.12f), dodge(0.05f)),
    MAN_EATER(TIGER, ADULT, execute(0.3f), crit(0.1f)),
    STRIPED_TERROR(TIGER, ADULT, lifesteal(0.12f), berserk(0.2f)),
    FRIED_TILAPIA(TILAPIA, BABY, fried()),
    CRISPY_SKIN(TILAPIA, BABY, fried(), armor(0.05f)),
    NILE_SWIMMER(TILAPIA, KID, fried(), boost(0.2f,WATER)),
    POND_WEED(TILAPIA, KID, fried(), boost(0.2f,GRASS)),
    SCHOOLING(TILAPIA, TEEN, fried(), dodge(0.08f)),
    HARDY_FISH(TILAPIA, TEEN, fried(), endurance(0.4f)),
    GOLDEN_FRY(TILAPIA, ADULT, fried(), regen(0.5f)),
    MOUTH_BROODER(TILAPIA, ADULT, fried(), healBoost(0.3f), maxHp(0.1f)),
    ;

    val perks: List<Perk> = perks.toList()
    val label: String = name.lowercase().split('_').joinToString(" ") { it.replaceFirstChar(Char::uppercaseChar) }
    val description: String get() = perks.joinToString(" ") { it.text }

    /** Summed amount of [t]; 0 when the passive doesn't have it. */
    operator fun get(t: Trait): Float = perks.fold(0f) { acc, it -> if (it.trait == t) acc + it.amount else acc }
    fun has(t: Trait) = perks.any { it.trait == t }

    private fun hits(e: Set<Element>, m: Move) = m.elements.any { it in e } || (FIRE in e && m.isFire)

    /** Damage multiplier for [m] from type boosts. */
    fun boost(m: Move) = perks.fold(1f) { acc, it -> if (it.trait == Trait.BOOST && hits(it.elements, m)) acc * (1 + it.amount) else acc }

    /** Damage-taken multiplier for [m] from type resistances. */
    fun resist(m: Move) = perks.fold(1f) { acc, it -> if (it.trait == Trait.RESIST && hits(it.elements, m)) acc * (1 - it.amount) else acc }
}
