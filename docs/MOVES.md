# Move list

_Generated from the game data by `DocsGenerator`. Don't edit by hand._

191 moves. Ranges are in arena widths (the arena is 1.0 wide and 1.2 tall).
Normal moves only have a cooldown, Specials also need a full energy bar, and Uniques are
species signatures in slot 5, usable 2x per battle.

## Effects

| Effect | What it does |
|---|---|
| lunge | Dashes at the target during the wind-up |
| drain | Heals for half the damage dealt |
| burn | Damage over time for 3s |
| slow | Halves the target's speed for 3s |
| stun | Target can't move or act for 0.8s |
| freeze | Target can't move or act for 1.5s |
| knockback | Pushes the target away |
| pull | Drags the target closer |
| vanish | Untouchable during the wind-up, then reappears next to the target |
| recoil | User takes a quarter of the damage dealt |
| hits 2-4x | Hits 2-4 times (damage is per hit) |
| leech | Target loses HP to the user for 5s |
| paralyze | Target moves slower and its cooldowns run 50% longer for 4s |
| weaken | Target deals 30% less damage for 5s |
| root | Target can't move for 2.5s (can still act) |
| steal energy | Steals up to 40 energy |
| execute | Double damage on targets below 30% HP |
| recharge | User is stunned for 1.5s afterwards |
| heal | Restores 18% HP |
| rage | +30% damage for 4s |
| guard | Takes 60% less damage for 1.2s |
| haste | +50% speed and 30% shorter cooldowns for 5s |
| harden | Takes 40% less damage for 5s |
| counter | Reflects the next hit within 1.5s |
| teleport | Teleports to the far side of the arena |
| cleanse | Clears debuffs and heals 10% |
| energy +50 | +50 energy |
| rage + haste | Rage + haste for 5s |
| rage + heal | Rage for 6s and heals 25% |
| guard + heal | Guard for 3s and heals 15% |
| rage + haste + heal | Rage + haste for 8s and heals 35% |
| guard + harden + heal | Guard 4s, harden 8s, heals 25% |

## Normal (31)

| Move | Type | Category | Lv | Dmg | Range | Cooldown | Wind-up | Hitbox | Effect |
|---|---|---|---|---|---|---|---|---|---|
| Tackle | Normal | Normal | 1 | 8 | 0.24 | 1.3s | 250ms | melee | lunge |
| Guard | Normal | Normal | 1 | - | self | 3.5s | 0ms | self | guard |
| Growl | Normal | Normal | 2 | - | 0.45 | 6.0s | 250ms | cone | weaken |
| Punch | Normal | Normal | 3 | 6 | 0.16 | 0.7s | 150ms | melee | - |
| Claw Swipe | Normal | Normal | 4 | 7 | 0.18 | 0.85s | 150ms | melee | - |
| Quick Attack | Normal | Normal | 5 | 6 | 0.30 | 1.0s | 100ms | melee | lunge |
| Bite | Normal | Normal | 6 | 8 | 0.15 | 1.2s | 200ms | melee | drain |
| Howl | Normal | Normal | 8 | - | self | 9.0s | 300ms | self | rage |
| Fury Swipes | Normal | Normal | 9 | 4 | 0.20 | 1.6s | 150ms | melee | hits 2-4x |
| Pounce | Normal | Normal | 11 | 9 | 0.32 | 1.8s | 300ms | melee | lunge |
| Quick Jab | Normal | Normal | 12 | 5 | 0.17 | 0.5s | 100ms | melee | - |
| Headbutt | Normal | Normal | 14 | 9 | 0.18 | 2.4s | 300ms | melee | stun |
| Glare | Normal | Normal | 15 | - | 0.50 | 7.0s | 250ms | cone | paralyze |
| Zoomies | Normal | Normal | 17 | - | self | 9.0s | 200ms | self | haste |
| Counter | Normal | Normal | 18 | - | self | 7.0s | 100ms | self | counter |
| Heal | Normal | Normal | 20 | - | self | 8.0s | 400ms | self | heal |
| Double-Edge | Normal | Normal | 22 | 15 | 0.22 | 2.4s | 300ms | melee | recoil |
| Body Slam | Normal | Normal | 23 | 12 | 0.20 | 2.6s | 350ms | melee | knockback |
| Harden | Normal | Normal | 25 | - | self | 9.0s | 300ms | self | harden |
| Teleport | Normal | Normal | 27 | - | self | 6.0s | 150ms | self | teleport |
| Double Kick | Normal | Normal | 28 | 5 | 0.22 | 1.6s | 200ms | melee | hits 2-4x |
| Focus Energy | Normal | Normal | 30 | - | self | 12.0s | 400ms | self | energy +50 |
| Finishing Blow | Normal | Normal | 31 | 10 | 0.20 | 2.6s | 250ms | melee | execute |
| Refresh | Normal | Normal | 33 | - | self | 9.0s | 300ms | self | cleanse |
| Battle Cry | Normal | Special | 35 | - | self | 6.0s | 300ms | self | rage + haste |
| Hyper Fang | Normal | Special | 38 | 16 | 0.20 | 3.5s | 300ms | melee | drain |
| Second Wind | Normal | Normal | 42 | - | self | 6.5s | 300ms | self | heal |
| Giga Impact | Normal | Special | 45 | 22 | 0.25 | 4.5s | 600ms | melee | knockback |
| Last Stand | Normal | Special | 50 | - | self | 6.0s | 200ms | self | guard + heal |
| Hyper Beam | Normal | Special | 52 | 30 | 0.60 | 4.5s | 700ms | beam | recharge |
| Mega Punch | Normal | Special | 55 | 20 | 0.18 | 4.0s | 450ms | melee | stun |

## Fire (15)

| Move | Type | Category | Lv | Dmg | Range | Cooldown | Wind-up | Hitbox | Effect |
|---|---|---|---|---|---|---|---|---|---|
| Ember | Fire | Normal | 1 | 6 | 0.40 | 1.8s | 250ms | projectile | burn |
| Smokescreen | Fire | Normal | 5 | - | 0.45 | 6.0s | 250ms | cone | slow |
| Fire Fang | Fire | Normal | 8 | 9 | 0.16 | 1.3s | 200ms | melee | burn |
| Flame Dash | Fire | Normal | 11 | 9 | 0.30 | 1.8s | 300ms | melee | lunge |
| Incinerate | Fire | Normal | 14 | 8 | 0.35 | 2.0s | 250ms | projectile | steal energy |
| Heat Up | Fire | Normal | 17 | - | self | 8.0s | 300ms | self | rage |
| Fire Spin | Fire | Normal | 20 | 6 | 0.35 | 2.6s | 300ms | projectile | root |
| Flame Wheel | Fire | Normal | 24 | 11 | 0.24 | 2.0s | 300ms | melee | burn |
| Will-o-Wisp | Fire | Normal | 28 | - | 0.45 | 7.0s | 300ms | cone | weaken |
| Fire Breath | Fire | Special | 32 | 16 | 0.50 | 4.0s | 450ms | cone | burn |
| Fireball | Fire | Normal | 36 | 10 | 0.50 | 2.2s | 350ms | projectile | burn |
| Flare Blitz | Fire | Normal | 40 | 18 | 0.25 | 2.8s | 350ms | melee | recoil |
| Magma Burst | Fire | Special | 46 | 19 | 0.40 | 4.2s | 500ms | cone | knockback |
| Overheat | Fire | Special | 52 | 28 | 0.50 | 4.5s | 600ms | beam | recharge |
| Blast Flame | Fire | Special | 57 | 22 | 0.55 | 4.5s | 550ms | cone | burn |

## Water (15)

| Move | Type | Category | Lv | Dmg | Range | Cooldown | Wind-up | Hitbox | Effect |
|---|---|---|---|---|---|---|---|---|---|
| Bubble | Water | Normal | 1 | 6 | 0.45 | 1.6s | 250ms | projectile | slow |
| Water Gun | Water | Normal | 5 | 7 | 0.45 | 1.4s | 200ms | projectile | - |
| Withdraw | Water | Normal | 8 | - | self | 9.0s | 300ms | self | harden |
| Aqua Jet | Water | Normal | 11 | 7 | 0.32 | 1.1s | 100ms | melee | lunge |
| Aqua Tail | Water | Normal | 14 | 10 | 0.20 | 1.8s | 250ms | melee | knockback |
| Rain Dance | Water | Normal | 17 | - | self | 9.0s | 300ms | self | haste |
| Whirlpool | Water | Normal | 20 | 6 | 0.35 | 2.8s | 300ms | projectile | root |
| Brine | Water | Normal | 24 | 10 | 0.40 | 2.0s | 300ms | projectile | execute |
| Dive | Water | Normal | 28 | 14 | 0.30 | 3.5s | 900ms | melee | vanish |
| Water Jet | Water | Special | 32 | 14 | 0.55 | 3.5s | 350ms | cone | knockback |
| Aqua Ring | Water | Normal | 36 | - | self | 9.0s | 300ms | self | cleanse |
| Waterfall | Water | Normal | 40 | 12 | 0.22 | 2.4s | 300ms | melee | stun |
| Hydro Pump | Water | Special | 46 | 20 | 0.60 | 4.5s | 550ms | cone | slow |
| Hydro Cannon | Water | Special | 52 | 28 | 0.55 | 4.5s | 600ms | cone | recharge |
| Undertow | Water | Special | 57 | 22 | 0.60 | 4.5s | 550ms | cone | pull |

## Grass (15)

| Move | Type | Category | Lv | Dmg | Range | Cooldown | Wind-up | Hitbox | Effect |
|---|---|---|---|---|---|---|---|---|---|
| Leaf Toss | Grass | Normal | 1 | 7 | 0.42 | 1.5s | 250ms | projectile | - |
| Absorb | Grass | Normal | 5 | 6 | 0.35 | 1.4s | 200ms | projectile | drain |
| Leech Seed | Grass | Normal | 8 | 2 | 0.45 | 7.0s | 300ms | projectile | leech |
| Razor Leaf | Grass | Normal | 11 | 9 | 0.45 | 1.8s | 250ms | projectile | - |
| Stun Spore | Grass | Normal | 14 | - | 0.40 | 7.0s | 300ms | cone | paralyze |
| Growth | Grass | Normal | 17 | - | self | 9.0s | 300ms | self | rage |
| Bullet Seed | Grass | Normal | 20 | 4 | 0.40 | 1.9s | 250ms | projectile | hits 2-4x |
| Seed Bomb | Grass | Normal | 24 | 11 | 0.40 | 2.2s | 300ms | projectile | knockback |
| Vine Whip | Grass | Normal | 28 | 10 | 0.45 | 2.0s | 300ms | projectile | pull |
| Synthesis | Grass | Normal | 32 | - | self | 7.5s | 400ms | self | heal |
| Giga Drain | Grass | Normal | 36 | 10 | 0.40 | 2.6s | 350ms | projectile | drain |
| Sleep Powder | Grass | Normal | 40 | - | 0.40 | 9.0s | 400ms | cone | freeze |
| Petal Storm | Grass | Special | 46 | 18 | 0.50 | 4.2s | 450ms | projectile | slow |
| Frenzy Plant | Grass | Special | 52 | 28 | 0.50 | 4.5s | 600ms | projectile | recharge |
| Leaf Blade | Grass | Special | 57 | 22 | 0.25 | 4.0s | 350ms | melee | lunge |

## Ice (15)

| Move | Type | Category | Lv | Dmg | Range | Cooldown | Wind-up | Hitbox | Effect |
|---|---|---|---|---|---|---|---|---|---|
| Powder Snow | Ice | Normal | 1 | 6 | 0.40 | 1.6s | 250ms | projectile | slow |
| Ice Fang | Ice | Normal | 5 | 9 | 0.16 | 1.3s | 200ms | melee | slow |
| Mist | Ice | Normal | 8 | - | self | 9.0s | 300ms | self | cleanse |
| Icy Wind | Ice | Normal | 11 | 8 | 0.50 | 1.9s | 300ms | projectile | slow |
| Icicle Spear | Ice | Normal | 14 | 3 | 0.40 | 1.8s | 250ms | projectile | hits 2-4x |
| Frost Armor | Ice | Normal | 17 | - | self | 9.0s | 300ms | self | harden |
| Ice Punch | Ice | Normal | 20 | 11 | 0.18 | 2.3s | 200ms | melee | stun |
| Frost Breath | Ice | Normal | 24 | 10 | 0.45 | 2.4s | 300ms | projectile | - |
| Ice Shard | Ice | Special | 28 | 14 | 0.50 | 3.5s | 350ms | cone | slow |
| Avalanche | Ice | Normal | 32 | 12 | 0.30 | 2.6s | 400ms | projectile | knockback |
| Mirror Ice | Ice | Normal | 36 | - | self | 8.0s | 100ms | self | counter |
| Freeze Ray | Ice | Normal | 40 | 8 | 0.55 | 5.0s | 400ms | projectile | freeze |
| Blizzard | Ice | Special | 46 | 19 | 0.60 | 4.5s | 550ms | cone | slow |
| Glacier Crash | Ice | Special | 52 | 22 | 0.25 | 4.5s | 500ms | melee | stun |
| Absolute Zero | Ice | Special | 57 | 18 | 0.50 | 5.0s | 700ms | cone | freeze |

## Electric (15)

| Move | Type | Category | Lv | Dmg | Range | Cooldown | Wind-up | Hitbox | Effect |
|---|---|---|---|---|---|---|---|---|---|
| Spark | Electric | Normal | 1 | 7 | 0.20 | 1.0s | 150ms | melee | - |
| Thunder Wave | Electric | Normal | 5 | - | 0.50 | 7.0s | 250ms | strike | paralyze |
| Thunder Fang | Electric | Normal | 8 | 9 | 0.16 | 2.3s | 200ms | melee | stun |
| Shock Wave | Electric | Normal | 11 | 8 | 0.50 | 1.8s | 250ms | strike | - |
| Volt Sap | Electric | Normal | 14 | 6 | 0.30 | 1.8s | 200ms | strike | steal energy |
| Charge | Electric | Normal | 17 | - | self | 12.0s | 300ms | self | energy +50 |
| Nuzzle | Electric | Normal | 20 | 5 | 0.20 | 1.8s | 150ms | melee | paralyze |
| Wild Charge | Electric | Normal | 24 | 12 | 0.30 | 2.2s | 300ms | melee | lunge |
| Magnet Pull | Electric | Normal | 28 | 7 | 0.55 | 2.6s | 300ms | strike | pull |
| Thunder | Electric | Special | 32 | 18 | 0.60 | 4.5s | 550ms | strike | stun |
| Discharge | Electric | Normal | 36 | 10 | 0.35 | 2.8s | 300ms | strike | stun |
| Volt Tackle | Electric | Normal | 40 | 18 | 0.30 | 2.8s | 300ms | strike | recoil |
| Zap Cannon | Electric | Special | 46 | 20 | 0.55 | 4.5s | 600ms | beam | paralyze |
| Thunderbolt | Electric | Special | 52 | 22 | 0.60 | 4.5s | 500ms | strike | knockback |
| Gigavolt | Electric | Special | 57 | 30 | 0.65 | 4.5s | 700ms | strike | recharge |

## Flying (16)

| Move | Type | Category | Lv | Dmg | Range | Cooldown | Wind-up | Hitbox | Effect |
|---|---|---|---|---|---|---|---|---|---|
| Peck | Flying | Normal | 1 | 6 | 0.22 | 0.8s | 150ms | melee | - |
| Air Cutter | Flying | Normal | 5 | 6 | 0.42 | 1.6s | 250ms | projectile | - |
| Wing Attack | Flying | Normal | 8 | 9 | 0.25 | 1.6s | 200ms | melee | - |
| Aerial Ace | Flying | Normal | 11 | 8 | 0.30 | 1.5s | 200ms | melee | lunge |
| Feather Dance | Flying | Normal | 14 | - | 0.45 | 7.0s | 300ms | cone | weaken |
| Tailwind | Flying | Normal | 17 | - | self | 9.0s | 200ms | self | haste |
| Roost | Flying | Normal | 20 | - | self | 7.0s | 300ms | self | heal |
| Twister | Flying | Normal | 24 | 8 | 0.45 | 2.0s | 300ms | projectile | knockback |
| Fly | Flying | Normal | 28 | 16 | 0.30 | 4.0s | 1200ms | melee | vanish |
| Bounce | Flying | Normal | 30 | 13 | 0.30 | 3.6s | 1000ms | melee | vanish |
| Gust | Flying | Special | 32 | 13 | 0.55 | 3.5s | 300ms | cone | knockback |
| Drill Peck | Flying | Normal | 36 | 4 | 0.22 | 1.8s | 200ms | melee | hits 2-4x |
| Brave Bird | Flying | Normal | 40 | 20 | 0.30 | 3.0s | 400ms | projectile | recoil |
| Hurricane | Flying | Special | 46 | 20 | 0.60 | 4.5s | 550ms | cone | knockback |
| Sky Drop | Flying | Special | 52 | 22 | 0.30 | 4.5s | 1000ms | melee | vanish |
| Sky Attack | Flying | Special | 57 | 32 | 0.40 | 5.0s | 700ms | cone | recharge |

## Ground (15)

| Move | Type | Category | Lv | Dmg | Range | Cooldown | Wind-up | Hitbox | Effect |
|---|---|---|---|---|---|---|---|---|---|
| Mud Slap | Ground | Normal | 1 | 6 | 0.38 | 1.6s | 250ms | projectile | slow |
| Sand Attack | Ground | Normal | 5 | - | 0.45 | 6.0s | 250ms | cone | weaken |
| Rock Throw | Ground | Normal | 8 | 8 | 0.42 | 1.7s | 250ms | projectile | - |
| Sand Tomb | Ground | Normal | 11 | 6 | 0.40 | 2.6s | 300ms | projectile | root |
| Stomp | Ground | Normal | 14 | 10 | 0.20 | 2.4s | 300ms | melee | stun |
| Stone Skin | Ground | Normal | 17 | - | self | 9.0s | 300ms | self | harden |
| Bone Rush | Ground | Normal | 20 | 4 | 0.25 | 1.8s | 200ms | melee | hits 2-4x |
| Dig | Ground | Normal | 24 | 15 | 0.30 | 3.5s | 900ms | melee | vanish |
| Bulldoze | Ground | Normal | 28 | 9 | 0.45 | 2.4s | 300ms | projectile | slow |
| Rock Slam | Ground | Special | 32 | 18 | 0.22 | 4.0s | 500ms | melee | stun |
| Sandstorm | Ground | Normal | 36 | 9 | 0.60 | 2.4s | 350ms | projectile | - |
| Head Smash | Ground | Normal | 40 | 20 | 0.20 | 3.0s | 350ms | melee | recoil |
| Earthquake | Ground | Special | 46 | 20 | 0.90 | 4.5s | 600ms | projectile | stun |
| Fissure | Ground | Special | 52 | 26 | 0.40 | 4.5s | 600ms | projectile | execute |
| Stone Edge | Ground | Special | 57 | 22 | 0.40 | 4.2s | 500ms | projectile | knockback |

## Uniques

Each species gets a Baby unique that upgrades at Kid (Lv 20) and Adult (Lv 50).

| Move | Type | Category | Owner | Dmg | Range | Cooldown | Wind-up | Hitbox | Effect |
|---|---|---|---|---|---|---|---|---|---|
| Sun Ray | Grass | Unique | Sprout Baby | 18 | 0.65 | 15.0s | 600ms | beam | - |
| Solar Beam | Grass | Unique | Sprout Kid | 26 | 0.75 | 15.0s | 700ms | beam | - |
| Solar Flare | Grass | Unique | Sprout Adult | 34 | 0.80 | 15.0s | 700ms | beam | stun |
| Shadow Swipe | Normal | Unique | Kitty Baby | 17 | 0.30 | 15.0s | 300ms | melee | lunge |
| Shadow Claw | Normal | Unique | Kitty Kid | 24 | 0.35 | 15.0s | 300ms | melee | lunge |
| Night Terror | Normal | Unique | Kitty Adult | 32 | 0.40 | 15.0s | 300ms | melee | lunge |
| Flame Burst | Fire | Unique | Drake Baby | 20 | 0.50 | 15.0s | 550ms | cone | burn |
| Inferno | Fire | Unique | Drake Kid | 28 | 0.55 | 15.0s | 600ms | cone | burn |
| Dragon Fury | Fire | Unique | Drake Adult | 36 | 0.60 | 15.0s | 600ms | cone | burn |
| Moon Hop | Normal | Unique | Bunny Baby | 16 | 0.35 | 15.0s | 350ms | projectile | stun |
| Moon Kick | Normal | Unique | Bunny Kid | 22 | 0.40 | 15.0s | 350ms | projectile | stun |
| Lunar Strike | Normal | Unique | Bunny Adult | 30 | 0.45 | 15.0s | 350ms | projectile | stun |
| Rally Bark | Normal | Unique | Pup Baby | - | self | 15.0s | 400ms | self | rage + haste |
| Pack Call | Normal | Unique | Pup Kid | - | self | 15.0s | 400ms | self | rage + heal |
| Alpha Howl | Normal | Unique | Pup Adult | - | self | 15.0s | 400ms | self | rage + haste + heal |
| Static Eye | Electric | Unique | Owl Baby | 18 | 0.75 | 15.0s | 600ms | strike | paralyze |
| Storm Eye | Electric | Unique | Owl Kid | 25 | 0.85 | 15.0s | 650ms | strike | stun |
| Tempest Eye | Electric | Unique | Owl Adult | 32 | 0.90 | 15.0s | 650ms | strike | paralyze |
| Shell Up | Ground | Unique | Turtle Baby | - | self | 15.0s | 200ms | self | harden |
| Fortress | Ground | Unique | Turtle Kid | - | self | 15.0s | 200ms | self | guard + heal |
| Citadel | Ground | Unique | Turtle Adult | - | self | 15.0s | 200ms | self | guard + harden + heal |
| Wisp Flame | Fire | Unique | Fox Baby | 16 | 0.55 | 15.0s | 450ms | cone | burn |
| Foxfire | Fire | Unique | Fox Kid | 22 | 0.60 | 15.0s | 450ms | cone | burn |
| Nine Tails | Fire | Unique | Fox Adult | 30 | 0.65 | 15.0s | 450ms | cone | burn |
| Splash Wave | Water | Unique | Frog Baby | 16 | 0.60 | 15.0s | 500ms | cone | knockback |
| Tidal Wave | Water | Unique | Frog Kid | 22 | 0.70 | 15.0s | 550ms | cone | knockback |
| Tsunami | Water | Unique | Frog Adult | 30 | 0.75 | 15.0s | 550ms | cone | pull |
| Bear Swipe | Normal | Unique | Bear Baby | 22 | 0.20 | 15.0s | 450ms | melee | stun |
| Maul | Normal | Unique | Bear Kid | 30 | 0.22 | 15.0s | 450ms | melee | stun |
| Grizzly Rampage | Normal | Unique | Bear Adult | 38 | 0.24 | 15.0s | 450ms | melee | stun |
| Frost Slide | Ice | Unique | Penguin Baby | 17 | 0.35 | 15.0s | 300ms | melee | lunge |
| Polar Crash | Ice | Unique | Penguin Kid | 25 | 0.30 | 15.0s | 400ms | cone | knockback |
| Emperor Blizzard | Ice | Unique | Penguin Adult | 32 | 0.70 | 15.0s | 650ms | cone | freeze |
| Spike Roll | Ground | Unique | Hedgehog Baby | 16 | 0.32 | 15.0s | 300ms | melee | lunge |
| Needle Storm | Grass | Unique | Hedgehog Kid | 8 | 0.50 | 15.0s | 450ms | projectile | hits 2-4x |
| Quill Barrage | Grass | Unique | Hedgehog Adult | 11 | 0.60 | 15.0s | 450ms | projectile | hits 2-4x |
| Echo Shriek | Normal | Unique | Bat Baby | 16 | 0.55 | 15.0s | 400ms | cone | weaken |
| Vampire Fang | Normal | Unique | Bat Kid | 24 | 0.25 | 15.0s | 300ms | melee | drain |
| Blood Moon | Normal | Unique | Bat Adult | 32 | 0.60 | 15.0s | 600ms | projectile | drain |
| Antler Jab | Grass | Unique | Deer Baby | 17 | 0.22 | 15.0s | 300ms | melee | knockback |
| Verdant Charge | Grass | Unique | Deer Kid | 25 | 0.35 | 15.0s | 350ms | melee | lunge |
| Spirit Stampede | Ice | Unique | Deer Adult | 33 | 0.45 | 15.0s | 450ms | cone | stun |
| Pebble Toss | Water | Unique | Otter Baby | 16 | 0.45 | 15.0s | 300ms | cone | slow |
| River Rush | Water | Unique | Otter Kid | 24 | 0.35 | 15.0s | 300ms | melee | lunge |
| Rapid Current | Water | Unique | Otter Adult | 32 | 0.65 | 15.0s | 550ms | cone | pull |
| Bamboo Smack | Grass | Unique | Panda Baby | 18 | 0.22 | 15.0s | 350ms | melee | stun |
| Bamboo Storm | Grass | Unique | Panda Kid | 26 | 0.50 | 15.0s | 450ms | projectile | knockback |
| Panda Quake | Ground | Unique | Panda Adult | 34 | 0.80 | 15.0s | 600ms | projectile | stun |
| Tail Lash | Ground | Unique | Lizard Baby | 17 | 0.22 | 15.0s | 300ms | melee | knockback |
| Magma Tail | Fire | Unique | Lizard Kid | 25 | 0.30 | 15.0s | 350ms | cone | burn |
| Volcano Roar | Fire | Unique | Lizard Adult | 33 | 0.60 | 15.0s | 600ms | cone | burn |
| Snatch | Normal | Unique | Raccoon Baby | 16 | 0.22 | 15.0s | 250ms | melee | steal energy |
| Trash Toss | Normal | Unique | Raccoon Kid | 24 | 0.45 | 15.0s | 400ms | projectile | weaken |
| Midnight Heist | Electric | Unique | Raccoon Adult | 31 | 0.30 | 15.0s | 900ms | melee | vanish |
