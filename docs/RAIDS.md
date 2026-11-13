# Raids

A raid is a dungeon for **1–4 players**: play it solo from the Battle tab, or host a room
for up to three allies, linked over a hotspot like every other battle. The party fights through **10 floors** of increasing danger
and ends with a single raid boss. Pets that faint stay down for the rest of the run. Clearing
floor 10 unlocks a random pet.

Raids reuse the real-time battle engine, the room/link layer, and the reward-claim system. The
host runs the simulation and streams snapshots; guests only send commands, the same as other
linked battles.

| | |
|---|---|
| Party | 1–4 players (solo or a room), no CPU allies |
| Entry | any level |
| Floors | 10 (4 skeleton floors, 5 wild-pet floors, 1 boss) |
| Floor timer | 90s, 120s for the boss |
| Run length | roughly 15–25 minutes |
| Knockouts | a fainted pet is out until the raid ends |
| Reward | random pet unlock, first clear per day |

Four is a deliberate cap: the 1000 HP boss is tuned for 1–4 raiders. Opening raids to six would
need to scale the boss or it dies in seconds.

## Lobby

- The host picks **Raid** on the Battle tab, then opens a room with the usual hotspot
  transport.
- Allies join the room. The room shows each raider's active pet name and level.
- The host can start at any time, even alone. The raid plan (skeleton count, wild
  species, boss species) is rolled once at start from the host's RNG, so every phone sees the
  same dungeon.
- No mid-run joins. A raid is a single sealed run; leaving the room before Start is fine.

Any pet level can enter. Wild floors start at Lv 35 and the boss is Lv 50, so low-level pets
will struggle, but that is the raider's call.

**Solo:** with Raid selected, *Solo raid* starts immediately with no room. A room can also be
started with only the host in it. Solo floors 1–4 have one skeleton each.

## The run

| Floor | Encounter | Enemies |
|---|---|---|
| 1 | Skeleton pack | one skeleton per raider who started the raid, plus as many buried ones |
| 2 | Skeleton pack | same |
| 3 | Skeleton pack | same |
| 4 | Skeleton pack | same |
| 5 | Wild pet, Lv 35 | one |
| 6 | Wild pet, Lv 38 | one |
| 7 | Wild pet, Lv 42 | one |
| 8 | Wild pet, Lv 46 | one |
| 9 | Wild pet, Lv 50 | one |
| 10 | Raid boss, Lv 50 | one |

The skeleton count is fixed when the raid starts. With three raiders, every skeleton floor has
three skeletons, even if someone fainted on an earlier floor. Losing allies makes the rest of
the dungeon harder instead of easier.

### Floors 1–4: skeletons

Skeletons are raid-only combatants, **not a discoverable species**: they have no dex entry, no
quiz profile, no passive, and can never be unlocked. They are drawn as a pixel skeleton:
skull with hollow eyes, ribcage, bony arms and legs.

| Floor | Level | HP each | Damage |
|---|---|---|---|
| 1 | 20 | 120 | 0.9× |
| 2 | 24 | 150 | 0.95× |
| 3 | 28 | 180 | 1.0× |
| 4 | 32 | 210 | 1.05× |

- Type: Normal. Fixed loadout: Bone Rush, Headbutt, Guard, Glare, plus their own unique **Raise Dead**.
- No passives. They gain 4 energy per second on their own; Raise Dead costs 40 and pulls one of the
  floor's **buried skeletons** out of the ground at full HP next to the caster.
- Each skeleton floor buries one extra skeleton per raider, so a pack can at most double in size.
  Buried skeletons are hidden and untargetable until raised.
- Every enemy is CPU-controlled and fights back. Clear every standing skeleton (buried ones don't count)
  to open the next floor.
- Because they are Normal-type and numerous, area moves (cones, beams, explosions) are the
  intended answer — this is the floor set that teaches parties to fight groups.

### Floors 5–9: wild pets

- One wild pet per floor, generated the same way as a CPU opponent: random species, stage from
  its level, normal stage passive, normal unique.
- Levels ramp **35 → 38 → 42 → 46 → 50**, so the last wild floor is already boss-level.
- Unlike skeleton floors, the wild floors are single-enemy encounters. The pressure comes from
  carrying HP forward, not from being outnumbered. If playtesting shows four raiders breeze
  through them, the tuning lever is one wild pet per raider, matching the skeleton rule.

### Floor 10: the raid boss

- A single random species at **Lv 50 (Adult)**.
- **1000 HP**, implemented as a fixed max HP override so it does not depend on the species.
- **1.5× damage** on every move.
- Keeps its normal stage passive and unique, so the boss species changes how the fight plays.
- One boss regardless of party size. The boss floor gets **120 seconds** instead of 90.
- Boss species is rolled at raid start and is the same for everyone. Boss HP and damage are
  visible on the boss health bar.

## Down and out

- When a raider's pet reaches 0 HP, it is **down for the rest of the raid**. That player cannot
  give commands again; their phone switches to a spectator view showing the party's HP, the
  floor and the boss bar.
- There are no revives, no items, and no mid-run substitutions.
- **Team wipe** — every raider's pet is down — ends the raid immediately in failure.
- Fainted raiders still count as participants: if the party clears floor 10, they receive the
  completion reward. Spectating is the penalty, not losing the reward they helped earn.

## Between floors

- A short 5-second floor banner shows the next encounter.
- **Surviving pets heal 20% of max HP.** Fainted pets stay fainted.
- Energy, cooldowns and statuses reset for the new floor; HP does not.
- No other healing between floors, so heal moves and regen passives keep their value.

## Ending a run

| Situation | Result |
|---|---|
| Boss defeated | **Clear.** Rewards are granted. |
| All raider pets down | **Failure.** No pet unlock. |
| Floor timer expires with enemies alive | **Failure.** The dungeon pushes the party out. |
| Host leaves or the app closes | Run ends for everyone; no reward. |
| An ally leaves mid-run | A CPU takes over their pet (no reward for the absent player); the run continues. |

Failure still grants the XP earned for floors already cleared, but never the pet unlock.

## Rewards

- **First clear of the day:** every connected human raider, including fainted ones, unlocks one
  **random pet** through the existing random-pet grant. If the collection is already complete,
  the unlock is skipped and the result screen says so.
- **Repeat clears the same day:** XP only, no second pet. The daily key (`raid:<date>`) reuses
  the same claim-once mechanism as the boss and monthly rewards, so it works offline and cannot
  be farmed by replaying.
- **XP:** every connected raider's pet earns 5 XP per floor cleared, plus 25 XP on a clear.
  These numbers are deliberately modest — raids should not outpace walking as the main
  progression source.

The daily cap is a balance guardrail, not a hard rule: it keeps raids from replacing the
10,000-step pet unlock as the fastest way to fill the collection. It can be relaxed later.

## Why a raid instead of the event boss

The existing event boss is a one-time solo fight. A raid is repeatable, cooperative, and built
around party composition and attrition: shared floors, fixed skeleton counts, persistent damage,
and a boss that one player alone is unlikely to finish. It is still fully local — no server, no
accounts, host-authoritative like every other linked battle.

## How it fits the code

- **Domain:** `domain/battle/Raid.kt` owns the floor plan, skeleton stats, wild/boss rolls and
  the between-floor HP carry. Skeletons and bosses are `FighterInfo`s, not new `Species`, so the
  dex, quiz and save format stay untouched.
- **Fighter state:** `FighterInfo` gained `variant` (bone-white skeleton with a neutral
  passive), `fixedMoves`, `damageScale`, `maxHpOverride` and `startHp`. The boss uses the last
  three; only `variant` and `fixedMoves` need to cross the link.
- **Battle engine:** the engine already supports arbitrary fighter lists and team ids; a raid is
  one team of raiders versus one team of enemies. `timeLimitMs` lets the boss floor use 120s
  while every other floor uses 90s. Floor transitions build a fresh engine with the surviving HP.
- **Link/room:** `BattleViewModel`'s room flow gains a raid format capped at four seats and a
  per-floor host loop. `Protocol.VERSION` is 23; `Lobby` carries the raid flag, `Setup` is resent
  each floor with the floor number and enemy roster, and `RaidEnd` carries the final result.
- **Rewards:** `PetRepository.recordRaid()` saves XP and a battle log entry;
  `RewardRepository.claimRaid()` uses the daily `raid:<date>` key and the existing
  `grantRandomPet()`. Each device records and claims locally, so a host and every guest get
  their own pet.
- **UI:** a Raid format button, a Solo raid button, raid room, floor/ boss HUD, a spectator panel
  for downed raiders, downed markers on ally bars, a "floor cleared" banner, and a bone-white
  sprite palette for skeletons.
- **Tests:** floor plan (skeleton count = party size, levels 35–50), boss stats (Lv 50, 1000 HP,
  1.5× damage), skeleton passives/loadout, floor-transition HP carry, and protocol round-trip.

## Open tuning questions

- Party cap: 4 now; 6 is possible if boss HP/damage scale with the party.
- Wild floors: one enemy per floor now; one per raider is the fallback if too easy.
- Between-floor heal: 20% now; 0% makes the run much harsher, 35% much friendlier.
- Skeleton numbers above are first-pass and should be tuned after a few playtests.
- Daily reward cadence: first clear per day; could become first clear per week if unlocks come
  too fast.
