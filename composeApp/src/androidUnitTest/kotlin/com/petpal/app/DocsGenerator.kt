package com.petpal.app

import com.petpal.app.domain.battle.*
import com.petpal.app.domain.model.Species
import com.petpal.app.domain.model.Stage
import java.io.File
import kotlin.test.Test

/**
 * Regenerates docs/MOVES.md and docs/PETS.md from the game data, so the docs never drift.
 * Run: PETPAL_DOCS=docs ./gradlew :composeApp:testDebugUnitTest --tests '*DocsGenerator*'
 */
class DocsGenerator {
    @Test
    fun generate() {
        val out = System.getenv("PETPAL_DOCS") ?: return
        val dir = File(out).let { if (it.isAbsolute) it else File("..", out) }.apply { mkdirs() }
        File(dir, "MOVES.md").writeText(moves())
        File(dir, "PETS.md").writeText(pets())
    }

    private fun Move.row(level: String = this.level.toString()) =
        "| $label | ${element.label} | ${category.label} | $level | ${if (damage == 0) "-" else damage} | " +
            "${if (range == 0f) "self" else "%.2f".format(range)} | ${cooldownMs / 1000.0}s | ${windupMs}ms | " +
            "${shape.name.lowercase()} | ${effectLabel.ifEmpty { "-" }} |"

    private val header = "| Move | Type | Category | Lv | Dmg | Range | Cooldown | Wind-up | Hitbox | Effect |\n" +
        "|---|---|---|---|---|---|---|---|---|---|"

    private fun owner(m: Move): String {
        val s = Species.entries.first { m in SpeciesKit.allUniques(it) }
        val st = SpeciesKit.uniqueStage(s, m)!!
        return "${s.label} ${st.name.lowercase().replaceFirstChar { it.uppercase() }}"
    }

    private fun moves() = buildString {
        appendLine("# Move list")
        appendLine()
        appendLine("_Generated from the game data by `DocsGenerator`. Don't edit by hand._")
        appendLine()
        appendLine("${Move.entries.size} moves. Ranges are in arena widths (the arena is 1.0 wide and 1.2 tall).")
        appendLine("Normal moves have short cooldowns and Specials long ones. Uniques are species signatures in")
        appendLine("slot 5 with no cooldown: they need a full energy bar, which only fills by dealing and taking hits.")
        appendLine()
        appendLine("## Effects")
        appendLine()
        appendLine("| Effect | What it does |")
        appendLine("|---|---|")
        EFFECTS.forEach { (e, d) -> appendLine("| ${e.label} | $d |") }
        Element.entries.forEach { el ->
            val list = Move.entries.filter { it.element == el && it.category != MoveCategory.UNIQUE }.sortedBy { it.level }
            appendLine()
            appendLine("## ${el.label} (${list.size})")
            appendLine()
            appendLine(header)
            list.forEach { appendLine(it.row()) }
        }
        appendLine()
        appendLine("## Uniques")
        appendLine()
        appendLine("Each species has two unique options per stage (Baby, Kid, Teen, Adult). The player picks one when the pet reaches the stage and keeps it for good. Uniques can be attacks, buffs or debuffs.")
        appendLine()
        appendLine(header.replace("| Lv |", "| Owner |"))
        Species.entries.flatMap { SpeciesKit.allUniques(it) }.forEach { appendLine(it.row(owner(it))) }
    }

    private fun pets() = buildString {
        appendLine("# Pets")
        appendLine()
        appendLine("_Generated from the game data by `DocsGenerator`. Don't edit by hand._")
        appendLine()
        appendLine("${Species.entries.size} species. Every pet starts with ${SpeciesKit.STARTERS} moves, keeps up to ${SpeciesKit.MAX_MOVES}, and has its unique in slot 5.")
        appendLine("Base HP: Egg 85, Baby 100, Kid 115, Teen 122, Adult 130 (the same multiplier scales damage).")
        appendLine()
        appendLine("Every stage (Baby, Kid, Teen, Adult) has one passive of its own, which the pet gets on hatching or evolving.")
        appendLine()
        appendLine("| Pet | Types | Baby | Kid | Teen | Adult |")
        appendLine("|---|---|---|---|---|---|")
        Species.entries.forEach { s ->
            appendLine("| [${s.label}](#${s.label.lowercase()}) | ${types(s)} | " +
                SpeciesKit.passives(s).joinToString(" | ") { it.label } + " |")
        }
        Species.entries.forEach { s ->
            appendLine()
            appendLine("## ${s.label}")
            appendLine()
            appendLine("_${s.trait}_")
            appendLine()
            appendLine("- **Types:** ${types(s)}")
            val stages = listOf(Stage.BABY, Stage.KID, Stage.TEEN, Stage.ADULT)
            appendLine("- **Passive by stage:**")
            stages.forEach { st -> SpeciesKit.passive(s, st).let { a -> appendLine("  - ${st.name.lowercase().replaceFirstChar { it.uppercase() }}: **${a.label}** (${a.description})") } }
            appendLine("- **Max HP:** ${Stage.entries.joinToString(" / ") { st -> "${st.name.lowercase().replaceFirstChar { it.uppercase() }} ${BattleStats.maxHp(FighterInfo("", s, st))}" }}")
            appendLine("- **Starters:** ${SpeciesKit.starters(s).joinToString { it.label }}")
            appendLine("- **Unique options by stage (pick one):**")
            stages.forEach { st -> SpeciesKit.uniqueOptions(s, st)!!.let { (a, b) -> appendLine("  - ${st.name.lowercase().replaceFirstChar { it.uppercase() }}: ${a.label} or ${b.label}") } }
            val extras = SpeciesKit.extras(s)
            if (extras.isNotEmpty()) appendLine("- **Off-type moves it can learn:** ${extras.joinToString { it.label }}")
            appendLine("- **Can never learn:** ${SpeciesKit.banned(s).joinToString { it.label }}")
            appendLine()
            appendLine("<details><summary>Learnset (${SpeciesKit.learnset(s).size} moves)</summary>")
            appendLine()
            appendLine("| Lv | Move | Type | Category |")
            appendLine("|---|---|---|---|")
            SpeciesKit.learnset(s).forEach { appendLine("| ${it.level} | ${it.move.label} | ${it.move.element.label} | ${it.move.category.label} |") }
            appendLine()
            appendLine("</details>")
        }
    }

    private fun types(s: Species) = (listOf("Normal") + SpeciesKit.affinity(s).map { it.label }).joinToString(", ")

    private companion object {
        val EFFECTS = listOf(
            Effect.LUNGE to "Dashes at the target during the wind-up",
            Effect.TWIN_SLASH to "Two short steps forward, slashing after each one",
            Effect.DRAIN to "Heals for half the damage dealt",
            Effect.BURN to "Damage over time for 3s",
            Effect.SLOW to "Halves the target's speed for 3s",
            Effect.STUN to "Target can't move or act for 0.8s",
            Effect.FREEZE to "Target can't move or act for 1.5s",
            Effect.KNOCKBACK to "Pushes the target away",
            Effect.PULL to "Drags the target closer",
            Effect.VANISH to "Untouchable during the wind-up, then reappears next to the target",
            Effect.RECOIL to "User takes a quarter of the damage dealt",
            Effect.MULTI to "Hits 2-4 times (damage is per hit)",
            Effect.LEECH to "Target loses HP to the user for 5s",
            Effect.PARALYZE to "Target moves slower and its cooldowns run 50% longer for 4s",
            Effect.WEAKEN to "Target deals 30% less damage for 5s",
            Effect.ROOT to "Target can't move for 2.5s (can still act)",
            Effect.SIPHON to "Steals up to 40 energy",
            Effect.EXECUTE to "Double damage on targets below 30% HP",
            Effect.RECHARGE to "User is stunned for 1.5s afterwards",
            Effect.MARK to "Target takes 25% more damage from everyone for 5s",
            Effect.CHAIN to "Arcs to another enemy within reach for half damage; with nobody to jump to, zaps the target again for a third",
            Effect.SWAP to "User and target trade places",
            Effect.DISRUPT to "Adds 2s to every cooldown the target has",
            Effect.TRAP to "Lays a snare at the target's feet (up to the move's range). It arms after 0.5s, lasts 8s, " +
                "and the first enemy to step on it takes the damage and is rooted for 2s",
            Effect.SHIELD to "Barrier that soaks up 20% max HP (Special 30%, Unique 35%) for 6s",
            Effect.AID to "Heals the user and its teammates 12%",
            Effect.HEAL to "Restores 18% HP",
            Effect.RAGE to "+30% damage for 4s",
            Effect.GUARD to "Takes 60% less damage for 1.2s",
            Effect.HASTE to "+50% speed and 30% shorter cooldowns for 5s",
            Effect.HARDEN to "Takes 40% less damage for 5s",
            Effect.COUNTER to "Reflects the next hit within 1.5s",
            Effect.BLINK to "Teleports to the far side of the arena",
            Effect.CLEANSE to "Clears debuffs and heals 10%",
            Effect.ENERGIZE to "+50 energy",
            Effect.BATTLE_CRY to "Rage + haste for 5s",
            Effect.PACK_CALL to "Rage for 6s and heals 25%",
            Effect.FORTRESS to "Guard for 3s and heals 15%",
            Effect.ALPHA to "Rage + haste for 8s and heals 35%",
            Effect.CITADEL to "Guard 4s, harden 8s, heals 25%",
            Effect.SMOKE to "Leaves a smoke cloud for 4s that slows enemies near it, then backs off like Evade",
        )
    }
}
