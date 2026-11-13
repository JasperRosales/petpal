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
 * Every real passive belongs to exactly one species and stage: one passive per stage per pet.
 * The species' signature is its Baby passive. No effect repeats: a pet never has the same kind of
 * perk at two stages, no two pets share an identical perk, and each type boost/resist set is used once.
 *
 * [NEUTRAL] is the sentinel for raid-only combatants such as skeletons. It has no perks and is not
 * part of any species' four-stage kit.
 */
enum class Passive(val species: Species, val stage: Stage, vararg perks: Perk) {
    NEUTRAL(SPROUT, EGG),
    PHOTOSYNTHESIS(SPROUT, BABY, regen(0.6f)),
    DEEP_ROOTS(SPROUT, KID, boost(0.2f,GRASS,GROUND), endurance(0.3f)),
    SAP_MENDER(SPROUT, TEEN, healBoost(0.4f), shieldBoost(0.2f)),
    ANCIENT_GROVE(SPROUT, ADULT, regenIdle(2.5f), maxHp(0.1f)),
    NINE_LIVES(KITTY, BABY, lastStand()),
    STATIC_WHISKERS(KITTY, KID, boost(0.2f,ELECTRIC,ICE), staticTouch(0.15f)),
    CATS_GRACE(KITTY, TEEN, dodge(0.12f), evadeCd(0.25f)),
    PROWLER(KITTY, ADULT, execute(0.25f), crit(0.08f)),
    BLAZE(DRAKE, BABY, boost(0.25f,FIRE), burnTime(1f)),
    UPDRAFT(DRAKE, KID, speed(0.08f), range(0.08f)),
    DRAGON_HIDE(DRAKE, TEEN, resist(0.25f,GRASS,ELECTRIC), maxHp(0.11f)),
    INFERNO_LORD(DRAKE, ADULT, special(0.2f), flameTouch(0.2f)),
    QUICK_FEET(BUNNY, BABY, speed(0.25f), cooldown(0.1f)),
    SNOWBOUND(BUNNY, KID, boost(0.2f,ICE,GROUND), dodge(0.05f)),
    SPRING_LEGS(BUNNY, TEEN, brawler(0.15f), evadeCd(0.2f)),
    MOONLEAP(BUNNY, ADULT, opener(0.2f), energyRate(0.15f)),
    LOYAL_HEART(PUP, BABY, lowHpPower(0.25f)),
    DIGGER(PUP, KID, boost(0.2f,GROUND), vanishCd(0.15f)),
    PACK_SPIRIT(PUP, TEEN, healBoost(0.3f), grit(0.15f)),
    GUARDIAN_HOUND(PUP, ADULT, armor(0.1f), guts(0.2f)),
    KEEN_EYE(OWL, BABY, range(0.2f)),
    SILENT_WINGS(OWL, KID, boost(0.2f,FLYING,ELECTRIC), dodge(0.06f)),
    SAGE_MIND(OWL, TEEN, special(0.21f), cooldown(0.06f)),
    ELDER_OWL(OWL, ADULT, sniper(0.25f), weakenTouch(0.1f)),
    HARD_SHELL(TURTLE, BABY, armor(0.2f)),
    TIDE_SHELL(TURTLE, KID, boost(0.2f,WATER), shieldBoost(0.25f)),
    SLOW_AND_STEADY(TURTLE, TEEN, endurance(0.5f), maxHp(0.09f)),
    FORTRESS(TURTLE, ADULT, startShield(0.2f), grit(0.12f)),
    TRICKSTER(FOX, BABY, dodge(0.2f)),
    FOXFIRE(FOX, KID, burnTime(0.5f), flameTouch(0.15f)),
    ILLUSION(FOX, TEEN, adapt(0.2f), evadeCd(0.3f)),
    NINE_TAILS(FOX, ADULT, boost(0.15f,FIRE,ICE), crit(0.1f)),
    SLIPPERY(FROG, BABY, unstoppable()),
    LILY_PAD(FROG, KID, boost(0.2f,WATER,GRASS), regen(0.3f)),
    LONG_TONGUE(FROG, TEEN, range(0.15f), energySteal(4f)),
    TOAD_KING(FROG, ADULT, maxHp(0.15f), thorns(0.1f)),
    THICK_FUR(BEAR, BABY, maxHp(0.2f)),
    SALMON_SWIPE(BEAR, KID, boost(0.2f,GROUND,FIGHTING), brawler(0.08f)),
    HIBERNATE(BEAR, TEEN, regenIdle(2f), endurance(0.2f)),
    GRIZZLY(BEAR, ADULT, berserk(0.35f), armor(0.06f)),
    ICE_BODY(PENGUIN, BABY, burnImmune(), boost(0.15f,ICE)),
    BELLY_SLIDE(PENGUIN, KID, speed(0.1f), evadeCd(0.15f)),
    HUDDLE(PENGUIN, TEEN, endurance(0.4f), healBoost(0.25f)),
    EMPEROR(PENGUIN, ADULT, maxHp(0.16f), grit(0.16f)),
    SPIKY(HEDGEHOG, BABY, thorns(0.2f)),
    BRAMBLE_QUILLS(HEDGEHOG, KID, boost(0.2f,GRASS), armor(0.05f)),
    STATIC_QUILLS(HEDGEHOG, TEEN, staticTouch(0.2f), grit(0.1f)),
    PINCUSHION(HEDGEHOG, ADULT, weakenTouch(0.15f), maxHp(0.12f)),
    VAMPIRE(BAT, BABY, lifesteal(0.1f)),
    NIGHT_WINGS(BAT, KID, boost(0.2f,FLYING), dodge(0.04f)),
    SONAR_PING(BAT, TEEN, sniper(0.2f), weakenTouch(0.11f)),
    NOSFERATU(BAT, ADULT, desperate(0.15f), regenLow(0.6f)),
    FLEET(DEER, BABY, evadeCd(0.5f)),
    MEADOW_GRAZE(DEER, KID, boost(0.2f,GRASS,ICE), regen(0.25f)),
    FOREST_SPIRIT(DEER, TEEN, healBoost(0.5f), regenLow(0.7f)),
    STAG_CHARGE(DEER, ADULT, opener(0.3f), brawler(0.1f)),
    PLAYFUL(OTTER, BABY, energyRate(0.3f)),
    RIVER_DANCE(OTTER, KID, boost(0.2f,WATER,ELECTRIC), speed(0.06f)),
    SLICK_FUR(OTTER, TEEN, dodge(0.08f), evadeCd(0.21f)),
    TIDE_TRICKSTER(OTTER, ADULT, energySteal(6f), cooldown(0.08f)),
    BAMBOO_SNACK(PANDA, BABY, snack(0.2f)),
    BAMBOO_STAFF(PANDA, KID, boost(0.2f,GRASS,FIGHTING), brawler(0.06f)),
    LAZY_DAY(PANDA, TEEN, regenIdle(2.1f), endurance(0.31f)),
    ZEN_MASTER(PANDA, ADULT, ironWill(), special(0.15f)),
    SUN_SCALES(LIZARD, BABY, resist(0.3f,FIRE,ICE)),
    SUNBATH(LIZARD, KID, boost(0.2f,FIRE,GROUND), regen(0.4f)),
    COLD_BLOOD(LIZARD, TEEN, endurance(0.51f), regenIdle(1.2f)),
    MAGMA_SKIN(LIZARD, ADULT, flameTouch(0.3f), armor(0.08f)),
    STICKY_PAWS(RACCOON, BABY, energySteal(8f)),
    WIRE_CHEWER(RACCOON, KID, boost(0.2f,ELECTRIC), staticTouch(0.08f)),
    TRASH_PANDA(RACCOON, TEEN, regenLow(0.8f), crit(0.06f)),
    MASTER_THIEF(RACCOON, ADULT, energyRate(0.2f), dodge(0.1f)),
    CHEEK_POUCH(HAMSTER, BABY, startEnergy(30f)),
    SPARK_WHEEL(HAMSTER, KID, boost(0.2f,ELECTRIC,GRASS), energyRate(0.1f)),
    FLUFFBALL(HAMSTER, TEEN, maxHp(0.08f), dodge(0.07f)),
    HAMSTER_DASH(HAMSTER, ADULT, speed(0.15f), cooldown(0.11f)),
    CHATTER(PARROT, BABY, weakenTouch(0.25f)),
    TROPIC_WINGS(PARROT, KID, boost(0.2f,FLYING,FIRE), range(0.06f)),
    SQUAWK(PARROT, TEEN, speed(0.07f), energySteal(3f)),
    ECHO_SONG(PARROT, ADULT, special(0.18f), cooldown(0.07f)),
    REGROW(AXOLOTL, BABY, regenLow(1.2f)),
    STREAM_SMILE(AXOLOTL, KID, regen(0.5f), healBoost(0.15f)),
    LIMB_REGEN(AXOLOTL, TEEN, maxHp(0.13f), endurance(0.25f)),
    ENDLESS_SMILE(AXOLOTL, ADULT, snack(0.25f), adapt(0.15f)),
    BURROWER(MOLE, BABY, vanishCd(0.3f)),
    TUNNELER(MOLE, KID, boost(0.2f,GROUND,ELECTRIC), armor(0.04f)),
    EARTHEN_HIDE(MOLE, TEEN, grit(0.13f), maxHp(0.07f)),
    DEEP_DELVER(MOLE, ADULT, opener(0.21f), resist(0.2f,ELECTRIC)),
    SNOOZE(KOALA, BABY, regenIdle(1.9f)),
    GUM_LEAF(KOALA, KID, boost(0.2f,NORMAL,GRASS), regen(0.2f)),
    TREE_HUGGER(KOALA, TEEN, grit(0.14f), armor(0.07f)),
    DEEP_SLEEP(KOALA, ADULT, healBoost(0.35f), regenLow(0.9f)),
    BLOOD_SCENT(SHARK, BABY, execute(0.3f)),
    RIPTIDE(SHARK, KID, boost(0.2f,NORMAL,WATER), speed(0.05f)),
    FEEDING_FRENZY(SHARK, TEEN, berserk(0.25f), lifesteal(0.05f)),
    MEGALODON(SHARK, ADULT, maxHp(0.14f), crit(0.11f)),
    COLOR_SHIFT(CHAMELEON, BABY, adapt(0.3f)),
    LEAF_SKIN(CHAMELEON, KID, boost(0.2f,GRASS,FIRE), regenIdle(1.3f)),
    SLOW_STALK(CHAMELEON, TEEN, opener(0.19f), range(0.1f)),
    VANISHING_ACT(CHAMELEON, ADULT, vanishCd(0.2f), evadeCd(0.31f)),
    DAM_BUILDER(BEAVER, BABY, startShield(0.15f), shieldBoost(0.5f)),
    MUD_PACK(BEAVER, KID, boost(0.2f,GROUND,WATER), endurance(0.21f)),
    LODGE(BEAVER, TEEN, armor(0.11f), regenIdle(1f)),
    MASTER_BUILDER(BEAVER, ADULT, maxHp(0.17f), healBoost(0.2f)),
    IRON_FIST(MONKEY, BABY, punch(0.25f)),
    JUNGLE_SWING(MONKEY, KID, boost(0.2f,NORMAL,FIGHTING), speed(0.09f)),
    MISCHIEF(MONKEY, TEEN, energySteal(5f), dodge(0.09f)),
    SIMIAN_KING(MONKEY, ADULT, crit(0.09f), cooldown(0.09f)),
    STEADFAST(KANGAROO, BABY, steadfast()),
    BOXER(KANGAROO, KID, boost(0.2f,FIGHTING), punch(0.1f)),
    BIG_LEAP(KANGAROO, TEEN, evadeCd(0.35f), opener(0.1f)),
    CHAMPION_BOXER(KANGAROO, ADULT, guts(0.25f), brawler(0.12f)),
    FIRST_STRIKE(EAGLE, BABY, opener(0.25f)),
    THERMAL_RIDER(EAGLE, KID, boost(0.2f,FLYING,FIGHTING), range(0.09f)),
    EAGLE_EYE(EAGLE, TEEN, sniper(0.26f), crit(0.05f)),
    APEX_TALONS(EAGLE, ADULT, execute(0.26f), speed(0.11f)),
    BLUBBER(SEAL, BABY, resist(0.3f,WATER,ICE)),
    ICE_FLOE(SEAL, KID, boost(0.2f,ICE,WATER), maxHp(0.05f)),
    BEACH_NAP(SEAL, TEEN, regenIdle(1.8f), healBoost(0.16f)),
    WALRUS_HIDE(SEAL, ADULT, endurance(0.41f), grit(0.11f)),
    TOUGH_HIDE(ELEPHANT, BABY, armor(0.12f)),
    TRUNK_POWER(ELEPHANT, KID, boost(0.2f,GROUND,NORMAL), brawler(0.09f)),
    NEVER_FORGETS(ELEPHANT, TEEN, adapt(0.25f), cooldown(0.05f)),
    MATRIARCH(ELEPHANT, ADULT, maxHp(0.21f), healBoost(0.31f)),
    STRIPES(TIGER, BABY, dodge(0.11f), opener(0.11f)),
    TIGER_BLAZE(TIGER, KID, boost(0.2f,FIRE,FIGHTING), crit(0.04f)),
    AMBUSH(TIGER, TEEN, execute(0.2f), energyRate(0.14f)),
    MAN_EATER(TIGER, ADULT, lifesteal(0.08f), berserk(0.2f)),
    FRIED_TILAPIA(TILAPIA, BABY, fried()),
    NILE_SWIMMER(TILAPIA, KID, regen(0.35f), energyRate(0.18f)),
    SCHOOLING(TILAPIA, TEEN, dodge(0.13f), evadeCd(0.19f)),
    GOLDEN_FRY(TILAPIA, ADULT, regenLow(1f), armor(0.09f)),
    UNBOTHERED(CAPYBARA, BABY, grit(0.2f)),
    HOT_SPRING(CAPYBARA, KID, boost(0.2f,NORMAL), regenIdle(1.4f)),
    BEST_FRIEND(CAPYBARA, TEEN, healBoost(0.21f), adapt(0.16f)),
    RIVER_ELDER(CAPYBARA, ADULT, resist(0.2f,WATER,GRASS), endurance(0.29f)),
    BIG_PINCERS(CRAB, BABY, brawler(0.2f)),
    ROCK_POOL(CRAB, KID, boost(0.2f,WATER,FIGHTING), maxHp(0.06f)),
    SIDESTEP(CRAB, TEEN, evadeCd(0.33f), adapt(0.12f)),
    KING_CRAB(CRAB, ADULT, resist(0.2f,NORMAL,FIGHTING), thorns(0.15f)),
    ONE_LEG_STAND(FLAMINGO, BABY, dodge(0.14f), evadeCd(0.22f)),
    FLOCK_DANCE(FLAMINGO, KID, boost(0.2f,FLYING,WATER), range(0.11f)),
    PINK_PLUMAGE(FLAMINGO, TEEN, special(0.16f), energyRate(0.11f)),
    LAGOON_QUEEN(FLAMINGO, ADULT, resist(0.2f,WATER), sniper(0.15f)),
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
