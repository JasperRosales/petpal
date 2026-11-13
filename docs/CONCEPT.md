# Concept

PetPal is an offline habit pet. You raise one pixel pet, and it only grows from things the phone can check: steps counted by the hardware sensor, and battles. There's no account, no server, and no internet needed to play (the app only goes online to check GitHub for updates). Battles run over Bluetooth or a phone hotspot.

## Your pet

- **One pet per device.** A short personality quiz picks its species the first time you open the app. See [PETS.md](PETS.md) for all species.
- **Pets tab.** Walk 10,000 steps to unlock a random new pet, or 50,000 to pick one yourself; the counter restarts after each unlock. New pets start as Lv 1 eggs. You keep every pet and can switch the active one any time; only the active pet earns XP and battles.
- **Relearning moves.** Every 10 levels your pet earns a relearn point. Spend it on the Home tab to bring back a move it forgot or skipped.
- **Stages.** Egg → Baby (Lv 3) → Kid (Lv 20) → Teen (Lv 35) → Adult (Lv 50). The stage sets max HP and damage (×0.85 / ×1.0 / ×1.15 / ×1.22 / ×1.3). Pets never devolve.
- **Levels.** Level *n* needs 10·(n−1)² XP, up to Lv 100.
- **Mood** follows your day: happy when you've been active, sleepy at night.

## Earning XP

| Source | XP |
|---|---|
| Every 1,000 steps (hardware step counter, max 20k steps/day) | 20 |
| Linked battle vs a friend: win / draw / loss | 30 / 15 / 10 |
| CPU battle: win / draw / loss | 8 / 4 / 2 |

Only verifiable activity pays, so shaking the phone or farming the CPU won't get you far.

## Moves

- A new pet knows 2 moves: Tackle and its first own-type attack.
- Leveling up unlocks more of its learnset. A pet keeps 4 moves. When a new one unlocks with all 4 slots full, you pick which one to forget.
- **Slot 5 is the unique.** Each stage (Baby, Kid, Teen, Adult) offers two unique options; when the pet reaches the stage you pick one and keep it for good (the first option is used until you pick). Uniques can be attacks, buffs or debuffs, and can be used twice per battle.
- **Stage passives.** Every species has exactly one passive per stage (Baby, Kid, Teen, Adult). Hatching or evolving swaps in the new stage's passive.
- Each species learns Normal moves, its own types (Bunny, Bear and Panda also have Fighting), and a few off-type moves. Some moves are banned for the body type (no punches without fists, no flying without wings).
- Combo moves have two types (Thunder Punch is Electric/Fighting, also Fire Punch, Ice Punch, Aqua Fist, Rock Smash, Sky Uppercut) and can be learned by a pet of either type.
- **Categories:** Normal moves just have a cooldown. Specials also need a full energy bar, which they empty. Uniques are slot 5 and need a full energy bar; trap uniques (Timber Trap, Hedge Maze, Tunnel Net) need only 50.

The full list with stats is in [MOVES.md](MOVES.md).

## Battles

Battles are real-time and last 90 seconds.

- **Pets roam on their own.** You don't steer. You pick moves and time them for when the target drifts into range.
- **Aiming and hitboxes.** A move is aimed where the target stood when you pressed it. A dashed outline shows the hitbox while it winds up, so a pet that moves away can dodge. Hitbox shapes:
  - melee circle in front
  - projectile that flies and stops on the first hit
  - beam
  - cone (breaths and waves)
  - strike that drops from above (lightning)
- **Energy** builds over time and from hits you deal or take. Specials need a full bar.
- **Evade** is built into every pet. It backs off from the target with a brief moment where it can't be hit, cancels your wind-up, and has a 3s cooldown.
- **Status effects:** burn, slow, stun, freeze, paralyze, root, weaken, leech, plus self buffs like rage, haste, guard and harden. Passives change these rules per species and stage (for example, Slippery can't be slowed or stunned).
- **Sound.** Every move has a sound for its type and shape (crackling fire, bubbles, zaps, slashes, rumbles), pitched a little differently per move. Hits thud and the hurt pet squeaks (smaller pets squeak higher). There are also sounds for knockouts and evades. The speaker icon under the timer mutes them. All sounds are synthesized on the phone, so there are no audio files.
- **Winning.** Knock out the other side. If time runs out, the side with more HP left wins (it compares the HP fraction).

### Maps (terrain rotation)

Each match moves on to the next map: the standard Ember Arena, then one terrain per element. Each terrain has its own rule that applies to **every** pet, and pets of the terrain's type are on **home turf** (energy charges 25% faster, 10% faster movement). Every map is solid ground, so any pet can fight anywhere.

| Map | Element | Effect (all pets) |
|---|---|---|
| Ember Arena | none | none |
| Volcano Rim | Fire | Scorching Heat: hits have a 20% chance to burn |
| Lakeshore | Water | Cool Breeze: energy charges 40% faster |
| Deep Forest | Grass | Life Regen: +1 HP per second |
| Frozen Tundra | Ice | Slick Ice: 20% slower movement, slows/freezes last 50% longer |
| Thunder Plains | Electric | Static Charge: cooldowns 20% shorter |
| Dusty Canyon | Ground | Sandstorm: -0.6 HP per second (never knocks out) |
| Windy Cliffs | Flying | Tailwind: 25% faster movement, knockbacks push twice as far |
| Mountain Dojo | Fighting | Fighting Spirit: hits have a 15% chance to crit for 1.5× |

After a match, **Battle again** starts a rematch on the next map. Linked: both players tap it; the host re-hosts and the guest reconnects automatically.

### Formats

| Format | Seats | Notes |
|---|---|---|
| 1v1 | 2 | You vs a friend, or vs a random CPU pet at your level |
| Duo 2v2 | 4 | Two teams of two. Pets target the nearest enemy, and cones, beams and other area moves can hit both. A team loses when both its pets are knocked out. |
| Raid | 1–4 | 10-floor solo or co-op dungeon: skeleton packs, wild pets, then a 1000 HP boss. Fainted pets stay out. See [RAIDS.md](RAIDS.md). |

Any seat without a person gets a **CPU**: a random species at the matching level with a random learned loadout. In Duo you can practice with a CPU partner against 2 CPUs. With a friend linked, they join as your rival (you + CPU vs friend + CPU) or your partner (you + friend vs 2 CPUs).

**4 players:** every seat is a person. The host waits for 3 friends to join; the first to join partners the host, the next two form the other team. If a player drops mid-match a CPU takes over their pet, as long as both teams still have a player.

### Links

- **Bluetooth:** one phone hosts, the other joins.
- **Hotspot / Wi-Fi:** the host phone turns on its hotspot (or both join the same Wi-Fi). The guest finds the host automatically.

The host phone runs the simulation and streams the state to the guest. Both phones must run the same PetPal version.
