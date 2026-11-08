package com.petpal.app.ui.battle

import androidx.compose.runtime.*
import com.petpal.app.domain.battle.*
import com.petpal.app.domain.model.Stage
import com.petpal.app.domain.platform.Sfx
import com.petpal.app.domain.platform.SoundPlayer

/** The sound a move makes when it fires, with a per-move pitch so moves sharing a sound still differ. */
internal fun Move.sound(): Pair<Sfx, Float> {
    val sfx = when {
        effect in setOf(Effect.HEAL, Effect.CLEANSE, Effect.PACK_CALL, Effect.ALPHA, Effect.AID) -> Sfx.HEAL
        effect in setOf(Effect.GUARD, Effect.HARDEN, Effect.FORTRESS, Effect.CITADEL, Effect.COUNTER, Effect.SHIELD) -> Sfx.SHIELD
        effect == Effect.SWAP -> Sfx.WHOOSH
        effect == Effect.BLINK -> Sfx.WHOOSH
        isBuff -> Sfx.BUFF
        shape == HitShape.BEAM -> Sfx.BEAM
        element == Element.FIRE -> Sfx.FIRE
        element == Element.WATER -> Sfx.WATER
        element == Element.GRASS -> Sfx.LEAF
        element == Element.ICE -> Sfx.ICE
        element == Element.ELECTRIC -> Sfx.ZAP
        element == Element.FLYING -> Sfx.WIND
        element == Element.FIGHTING -> if (category != MoveCategory.NORMAL) Sfx.BOOM else Sfx.PUNCH
        element == Element.GROUND -> if (effect == Effect.STUN || category != MoveCategory.NORMAL) Sfx.BOOM else Sfx.ROCK
        damage == 0 -> Sfx.WHOOSH
        effect == Effect.DRAIN || label.contains("Fang") || label.contains("Bite") -> Sfx.BITE
        label.contains("Claw") || label.contains("Swipe") || label.contains("Slash") -> Sfx.SLASH
        effect == Effect.KNOCKBACK && category != MoveCategory.NORMAL -> Sfx.BOOM
        shape == HitShape.MELEE -> Sfx.PUNCH
        else -> Sfx.WHOOSH
    }
    val vary = 0.9f + (ordinal * 37 % 21) / 100f // 0.90 .. 1.10
    val weight = when (category) { MoveCategory.NORMAL -> 1f; MoveCategory.SPECIAL -> 0.85f; MoveCategory.UNIQUE -> 0.78f }
    return sfx to vary * weight
}

/** Plays move, hit, knockout and evade sounds by watching snapshots change. */
@Composable
internal fun BattleSounds(f: FightState, sound: SoundPlayer) {
    val prev = remember { mutableMapOf<Int, FighterSnap>() }
    LaunchedEffect(f.snap) {
        f.snap.fighters.forEachIndexed { i, s ->
            val p = prev[i]
            prev[i] = s
            if (p == null) return@forEachIndexed
            val near = if (i == f.me) 1f else 0.75f
            val m = s.action
            val started = m != null && (p.action != m || s.actionAgeMs < p.actionAgeMs)
            if (m != null) {
                if (started && m.windupMs >= 450 && !m.isBuff) sound.play(Sfx.CHARGE, 1f, 0.35f * near)
                if ((started || p.actionAgeMs < m.windupMs) && s.actionAgeMs >= m.windupMs) {
                    val (sfx, pitch) = m.sound()
                    sound.play(sfx, pitch, (if (m.category == MoveCategory.NORMAL) 0.7f else 1f) * near)
                }
            }
            if (s.hurtAgeMs < p.hurtAgeMs && s.hurtAgeMs < 300) {
                val big = p.hp - s.hp >= s.maxHp * 0.12f
                sound.play(if (big) Sfx.HIT_BIG else Sfx.HIT, 1f, 0.9f)
                // the pet's own little cry: smaller pets squeak higher, each species a bit different
                val info = f.fighters[i]
                val size = when (info.stage) { Stage.EGG, Stage.BABY -> 1.3f; Stage.KID -> 1.1f; Stage.TEEN -> 1.0f; Stage.ADULT -> 0.9f }
                sound.play(Sfx.CRY, size * (0.85f + info.species.ordinal % 6 * 0.06f), 0.6f * near)
            }
            if (p.hp > 0 && s.hp <= 0) sound.play(Sfx.KO, 1f, 1f)
            if (!p.has(Status.EVADE) && s.has(Status.EVADE)) sound.play(Sfx.EVADE, 1f, 0.6f * near)
        }
    }
}
