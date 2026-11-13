# Adding and changing game content

This is the working guide for changing the battle content in PetPal: adding a pet, adding or
retuning a move, and changing passives. The source of truth is normal Kotlin code; the two big
reference docs are generated from it, so never edit them by hand.

| I want to... | Start here |
|---|---|
| Add a move or change a move's numbers | [`domain/battle/Moves.kt`](../composeApp/src/commonMain/kotlin/com/petpal/app/domain/battle/Moves.kt) — `Move` enum |
| Add a pet | [`domain/model/Models.kt`](../composeApp/src/commonMain/kotlin/com/petpal/app/domain/model/Models.kt) — `Species` enum, then `SpeciesKit` in `Moves.kt` |
| Change a passive | [`domain/battle/Passives.kt`](../composeApp/src/commonMain/kotlin/com/petpal/app/domain/battle/Passives.kt) — `Passive`, `Trait`, `Perk` |
| Make a passive or effect actually do something | [`domain/battle/BattleEngine.kt`](../composeApp/src/commonMain/kotlin/com/petpal/app/domain/battle/BattleEngine.kt) |
| Change how a move looks, reads or sounds | [`ui/battle/`](../composeApp/src/commonMain/kotlin/com/petpal/app/ui/battle) |
| Give a pet its personality-quiz odds | [`domain/logic/PersonalityQuiz.kt`](../composeApp/src/commonMain/kotlin/com/petpal/app/domain/logic/PersonalityQuiz.kt) |
| Give a pet colors or new art features | [`ui/components/PetSprite.kt`](../composeApp/src/commonMain/kotlin/com/petpal/app/ui/components/PetSprite.kt) |
| Rebuild the reference docs | `DocsGenerator` (command at the end) |

`docs/PETS.md` and `docs/MOVES.md` are generated. Edit the Kotlin, then regenerate.

## Safe changes vs. breaking changes

Enum constant **names** are the save format and the link protocol:

- the pet's `Species` is stored in Room by name (`species = species.name` in `Mappers.kt`);
- a fighter's species, stage and moves are sent over the battle link by name (`Protocol.kt`);
- passives are resolved from species + stage, so their enum names are only compile-time.

Adding new constants at the end of an enum is safe. Renaming or removing a constant breaks
existing saves and battles against an older install. If you must rename one, add a database
migration (Room stores the old string) and bump the link protocol version. The existing
`enumValueOrDefault(...)` fallback turns an unknown species into Sprout, but moves and stages
are parsed with `valueOf(...)`.

## Add a move

Moves live in the `Move` enum in `Moves.kt`. The constructor is:

```kotlin
Move(
    label: String,
    category: MoveCategory,   // NORMAL, SPECIAL, or UNIQUE
    element: Element,         // primary type
    level: Int,               // level a species learns it at; 0 for uniques
    damage: Int,              // 0 for a pure buff/debuff
    range: Float,             // arena widths; 0f for self buffs
    cooldownMs: Long,
    windupMs: Long,
    recoverMs: Long,
    effect: Effect = Effect.NONE,
    private val alsoLunges: Boolean = false,   // add a dash on top of effect (slams)
    element2: Element? = null,     // second type for combo moves
)
```

`N`, `S` and `U` are local aliases for the three categories. Put the new constant in the block
for its primary element:

```kotlin
// Normal move with a slow
BODY_CHECK("Body Check", N, Element.NORMAL, 24, 11, 0.20f, 2400, 300, 250, Effect.SLOW),

// Special: long cooldown, no energy needed
IRON_TEMPEST("Iron Tempest", S, Element.NORMAL, 46, 19, 0.30f, 12_600, 500, 300, Effect.KNOCKBACK),

// Combo move: learnable by a pet of either type
EMBER_FIST("Ember Fist", N, Element.FIRE, 26, 12, 0.18f, 2400, 250, 200, Effect.BURN, element2 = Element.FIGHTING),

// Slamming move: lunge dash plus knockback
IRON_TAIL("Iron Tail", S, Element.NORMAL, 44, 18, 0.22f, 12_600, 400, 300, Effect.KNOCKBACK, alsoLunges = true),
```

Rules of thumb:

- **Level** is the normal unlock gate. Keep it at or below `PetEngine.MAX_LEVEL` (100).
  Uniques are `level = 0` and use `cooldownMs = 0`; they run on a full energy bar instead.
- **Category.** `NORMAL` moves only have a cooldown. `SPECIAL` moves have a long cooldown but
  do **not** spend energy. `UNIQUE` moves are slot 5, need a full energy bar, and are spent on
  use. Category also affects damage (`SPECIAL`/`UNIQUE` get the `SPECIAL` passive boost).
- **Effect.** Reuse an existing `Effect` whenever you can. `Effect.self = true` marks buffs,
  which target the user; everything else targets the opponent. `alsoLunges` is only for attacks
  that should dash *and* do another effect.
- **Combo moves.** Use `element2`; `Move.elements` then covers both types for learnability,
  boosts and resistances.
- **Punches.** If the move is a fist move, add it to the `PUNCHES` set near the bottom of
  `Moves.kt`. Species with `Move.PUNCH` banned then can't learn it either.
- **Hit shape is derived, not set.** `Hitbox.kt` decides `MELEE`, `PROJECTILE`, `BEAM`, `CONE`
  or `STRIKE` from the move's range, damage, element, category and effect. If you need a new
  beam, explosive projectile or wave, add it to `BEAMS`, `EXPLOSIVE` or `WAVES` there.
- **Who learns it.** Any species whose `affinity` or `extras` includes the move's type learns
  it automatically at its `level`. To give it only to selected pets, put it in their
  `SpeciesKit.extras`. To exclude pets of a type, add it to their `SpeciesKit.banned` list.
- **New effect.** If no existing `Effect` covers it, see [Add a new effect](#add-a-new-effect).

## Change an existing move

Edit its fields in place. Everything else follows automatically: learnsets, docs, previews,
sounds and FX all read the enum. A few consequences worth checking:

- changing `range` can change its hitbox shape (see `Hitbox.kt`);
- changing `category` changes the cooldown convention, whether the move needs energy and whether
  the `SPECIAL` passive applies;
- changing `effect` changes both combat logic and the generated effect text in `MOVES.md`;
- if you move it to a different primary element, it changes which species can learn it, so check
  `extras` and `banned` for the affected pets.

`MoveFxRenderTest` draws every move and `MoveTextTest` writes a description for every move, so a
mistyped field usually shows up as a failing test rather than a silent bad move.

## Add a unique move

Uniques are species signatures in slot 5. Each species gets **eight** unique moves: two options
for each of the four stages (Baby, Kid, Teen, Adult).

1. Add the move to the unique section of `Moves.kt`, always with `U`, `level = 0` and
   `cooldownMs = 0`:

   ```kotlin
   SHELL_SPIKE("Shell Spike", U, Element.GROUND, 0, 30, 0.30f, 0, 350, 250, Effect.STUN),
   TIDAL_PULL("Tidal Pull", U, Element.WATER, 0, 0, 0.60f, 0, 300, 200, Effect.PULL),
   ```

2. Register both options for the stage in `SpeciesKit.uniqueOptions`:

   ```kotlin
   Species.TURTLE -> listOf(
       Move.SHELL_UP to Move.SHELL_BASH,
       Move.FORTRESS to Move.AQUA_SHELL,
       Move.TIDAL_SHELL to Move.BEDROCK,
       Move.CITADEL to Move.ANCIENT_WAVE, // Baby, Kid, Teen, Adult, in this order
   )
   ```

   The player picks one option per stage on evolution; the first option is the default until
   then. The first option should be an attack and the second a utility move, or vice versa, but
   the important part is one pair per stage.

3. Nothing else is required for visuals: `UniqueFx.kt` assigns every unique its own set piece
   automatically and `UniqueFxTest` verifies no two uniques look the same. No sound entry is
   needed either.

If a unique is missing from `uniqueOptions`, `BattleTest.moveCatalog` fails because the set of
`UNIQUE` moves no longer equals the set of moves registered to species.

## Add a new effect

This is rare, but here is the full path when an existing `Effect` doesn't fit.

1. Add the constant to `Effect` in `Moves.kt`, with its short label. Mark it `self = true` if it
   buffs the user (`self` also decides that it has a `SELF` hitbox and targets the user).

   ```kotlin
   TAUNT("taunt"), // target is forced to attack the user for 3s
   ```

2. Implement it in `BattleEngine.kt` in the right place:
   - **Self buffs** go in the `when (m.effect)` block around the `applyEffect` self-buff branch
     (search for `Effect.HEAL`). Set the relevant `Fighter` timer/field and add a log line.
   - **On-hit effects** go in the first `when (m.effect)` after damage (search for
     `Effect.DRAIN`), or in the second `when` that handles movement (`SWAP`, `KNOCKBACK`, `PULL`).
   - **Debuffs** belong in `debuff()` (search for `Effect.SLOW`). If it stops movement, also add
     it to `MOVEMENT_DEBUFFS` so Slippery ignores it.
   - **Damage/status math** such as `MULTI` or `EXECUTE` is handled around `calculate damage`.
   - **AI** priorities live in `BattleBot` at the bottom of `BattleEngine.kt` (`BUFFS`,
     `BIG_HEALS` and the ready-move heuristics in `pick`); a new buff or panic button usually
     wants a line there so practice CPUs actually use it.

3. Surface it in the UI:
   - `MovePreview.kt`: add the sentence shown when long-pressing a move.
   - `ui/battle/MoveFx.kt`: add the particle look in the `when (effect)` block.
   - `ui/battle/BattleScreen.kt`: add an icon (and color, if it's a buff).
   - `ui/battle/BattleSounds.kt`: route it to a sound family if the defaults are wrong.
   - `DocsGenerator.EFFECTS`: add the one-line explanation used by `MOVES.md`.

## Add a pet

Adding a species touches several files in a fixed order. The compiler will catch some gaps, but
the quiz and unique registration are easy to forget because nothing forces them.

### 1. The species itself

`Models.kt`:

```kotlin
CRAB("Crab", "Tough and stubborn. Hard on the outside, and you never let go of what matters."),
```

`label` is the display name; `trait` is the one-line personality shown in the pets screen and
docs.

### 2. Its battle kit

In `SpeciesKit` (`Moves.kt`), add one line to each of these `when` blocks:

```kotlin
// affinity: its own types besides Normal, 2–3 is the house style
Species.CRAB -> setOf(Element.WATER, Element.FIGHTING)

// extras: 3–5 off-type moves it can still learn
Species.CRAB -> setOf(Move.ICE_SHARD, Move.ROCK_THROW, Move.DIG, Move.MUD_SLAP)

// banned: Normal/own-type moves it physically can't do
Species.CRAB -> setOf(Move.HOWL, Move.BITE, Move.HYPER_FANG, Move.DOUBLE_KICK, Move.POUNCE,
    Move.TELEPORT, Move.ZOOMIES)
```

Keep the physical logic honest: no fists means include `Move.PUNCH` in `banned` (that also bans
every move in `PUNCHES`), no wings means no Flying moves, no mouth means no bites or howls.
`SpeciesKit.canLearn` combines affinity, extras and bans; a new species with no banned moves is
suspicious and probably should ban a few.

### 3. Four passives

In `Passives.kt`, add exactly one `Passive` per stage:

```kotlin
BIG_PINCERS(CRAB, BABY, brawler(0.2f)),
ROCK_POOL(CRAB, KID, boost(0.2f, WATER, FIGHTING), maxHp(0.06f)),
SIDESTEP(CRAB, TEEN, evadeCd(0.33f), adapt(0.12f)),
KING_CRAB(CRAB, ADULT, resist(0.2f, NORMAL, FIGHTING), thorns(0.15f)),
```

`SpeciesKit` builds the passive map with `single { it.stage == st }`, so a missing or doubled
stage throws the first time the kit is used. The Egg uses the Baby passive.

### 4. Eight unique moves

Follow [Add a unique move](#add-a-unique-move), then register the four pairs in
`SpeciesKit.uniqueOptions`. Every unique must belong to exactly one species; sharing is rejected
by `BattleTest.moveCatalog`.

### 5. Personality-quiz profile and bias

`PersonalityQuiz.kt` has two maps that are **not** checked by the compiler:

```kotlin
// ENERGY, SOCIAL, CURIOSITY, CALM, COURAGE, HEART, each 0..3
val profiles = mapOf(
    ...
    CRAB to intArrayOf(1, 2, 0, 1, 3, 2),
)

private val bias = mapOf(
    ...
    CRAB to 0.0f,
)
```

If a species has no `profiles` entry it can never be quiz result, and
`BattleTest.everySpeciesCanWinTheQuiz` fails. Start with a profile matching the personality
text, then tune `bias` so `quizSpreadsPetsEvenly` passes: every pet should land within roughly
1–5% of results. The comment on `bias` says the values were tuned by simulation, and the test
prints the min/max share when it fails, so iterate on the outliers.

Adding a species also shifts the odds of every other pet slightly, so re-run the distribution
test and adjust `bias` for the whole roster, not just the new pet.

### 6. Colors and art

`PetSprite.kt`:

- `Species.colors()` is an exhaustive `when`; the compiler fails until you add a palette:

  ```kotlin
  Species.CRAB -> Triple(Color(0xFFE8634A), Color(0xFFA83A2A), Color(0xFFF6C7A8))
  ```

  The triple is body, dark shade, belly/light shade.

- Optional shape hooks, all with safe `else` branches: `build()` (body width/height),
  `eyeStyle()`, the "behind the body", "ears/things on top" and "face" blocks, and the
  `SIDE_TAILS` set for long side tails. A new pet with only colors will render using the generic
  body, but give it at least one recognisable feature.

- Eggs are drawn separately per species in `PetArt.egg(species)`. Check the egg looks distinct
  from its neighbours.

### 7. Tests and docs

Run the full suite and regenerate the reference docs (see below). `PETS.md` is the best visual
check: it lists the new pet's types, all four passives, HP, starters, unique options, off-type
moves, bans and learnset.

## Change a passive

A passive is one line in `Passives.kt`:

```kotlin
Passive(species, stage, perk1, perk2, ...)
```

`Perk` amounts are fractions: `0.2f` is 20%, `regen(0.6f)` is 0.6 HP per second. The helper
functions (`boost`, `armor`, `regen`, `dodge`, `execute`, ...) are just typed wrappers around
`Perk(Trait.X, amount, elements)`. Use them instead of raw `Perk` calls; they keep the units and
element sets readable.

To retune one, change the argument:

```kotlin
// before
PHOTOSYNTHESIS(SPROUT, BABY, regen(0.6f)),
// after
PHOTOSYNTHESIS(SPROUT, BABY, regen(0.8f)),
```

To swap what it does, replace the perks. The description in `PETS.md` and the battle UI is
generated from `Perk.text`, so there is no second string to update:

```kotlin
// Sprout Kid: was Grass/Ground damage + endurance
DEEP_ROOTS(SPROUT, KID, boost(0.2f, GRASS, GROUND), endurance(0.3f)),
// now: Grass/Ground damage + healing boost
DEEP_ROOTS(SPROUT, KID, boost(0.2f, GRASS, GROUND), healBoost(0.25f)),
```

Keep these rules, which the test suite partly enforces:

- exactly one passive per species per stage (Baby, Kid, Teen, Adult), and every label unique;
- no two passives anywhere share an identical perk set (`everyPassiveIsUniqueToOnePetAndStage`);
- by design, a pet should not repeat the same kind of perk at two stages, and each type
  boost/resist set should be used by only one passive in the whole game. The suite doesn't catch
  these, so check them by eye.
- keep percentages in the same ballpark as the surrounding stages; a single perk is usually
  0.05–0.5, and two perks per passive is the norm.

### Add a new trait

`Perk.text` is an exhaustive `when`, so adding a `Trait` without text won't compile. The full
path is:

1. Add the constant to `Trait` in `Passives.kt`.
2. Add its sentence to `Perk.text` (this is what the docs and UI display).
3. If it will be reused, add a helper next to the others, e.g. `private fun taunt(a: Float) = p(Trait.TAUNT, a)`.
4. Implement it in `BattleEngine.kt`. Where depends on the family:
   - max HP / damage / cooldown / speed / range: `BattleStats` at the top of the file;
   - energy gain and start values: `gainEnergy`, `Fighter` init;
   - dodge, crit, armor, execute, low-HP bonuses: the damage calculation (`attack`);
   - on-hit effects: the lines just after damage (`LIFESTEAL`, `STATIC_TOUCH`, ...);
   - regen and heal boosts: the per-tick section and `heal()`;
   - shields: `absorb` / shield application;
   - knockout saves and comeback mechanics: `hurt()` (see `LAST_STAND`, `FRIED`, `SNACK`).
5. Add a test if the trait has a specific rule (the existing `PetEngineTest` and `BattleTest`
   are the places for stage and passive invariants).

## Validate your changes

```bash
# compile + logic and UI tests
./gradlew :composeApp:testDebugUnitTest

# rebuild docs/MOVES.md and docs/PETS.md from the Kotlin data
PETPAL_DOCS=docs ./gradlew :composeApp:testDebugUnitTest --tests '*DocsGenerator*'
```

The tests that usually catch content mistakes:

| Test | Catches |
|---|---|
| `BattleTest.moveCatalog` | unregistered or shared uniques, bad starter count, banned-but-learnable moves, missing off-type moves, levels past 100 |
| `PetEngineTest.everyPassiveIsUniqueToOnePetAndStage` | missing/duplicate stage passives, duplicated perk sets, duplicate labels |
| `BattleTest.everySpeciesCanWinTheQuiz` | a species missing from `PersonalityQuiz.profiles` |
| `BattleTest.quizSpreadsPetsEvenly` | badly tuned quiz `bias` after a roster change |
| `UniqueFxTest.everyUniqueHasItsOwnSetPiece` | unique FX collisions |
| `MoveTextTest` / `MoveFxRenderTest` | moves whose description or visuals break |

## Common gotchas

- **New pet, no quiz profile.** The pet is unreachable in onboarding and the quiz test fails.
  The compiler cannot help here because profiles are a `Map`.
- **New unique, not in `uniqueOptions`.** The move exists but nobody can learn it, and the
  catalog test fails.
- **New pet, no `colors()` entry.** Compile error (exhaustive `when`) — that's the good case.
- **New pet, no `banned` list.** It will learn every Normal move, including bites and howls it
  shouldn't have. The catalog test only checks that banned moves can't be learned, not that bad
  moves are banned.
- **New effect, not in `BattleEngine`.** It compiles but does nothing, or only deals damage.
  Follow the UI checklist too, or the move plays with no visual or wrong sound.
- **Hand-editing `PETS.md` or `MOVES.md`.** The next docs run overwrites it. Always fix the
  Kotlin and regenerate.
- **Renaming an existing enum constant.** Old saves and older linked installs break. Append
  instead; migrate only when you have to.
