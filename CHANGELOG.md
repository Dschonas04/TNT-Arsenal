# Changelog

All notable changes to TNT Arsenal. The format follows
[Keep a Changelog](https://keepachangelog.com/en/1.1.0/), versions follow
[Semantic Versioning](https://semver.org/).

## [Unreleased]

## [1.8.1+26.1] - 2026-10-29

- Backport of 1.8.1 to Minecraft **26.1.x** (Fabric API 0.155.3): `EntityType` instead of `EntityTypes`, wool and concrete by colour and the short fuse in `Compat`.

## [1.8.1] - 2026-10-22

### Changed
- Flak TNT fires three Giga TNT into the sky, trailing fire, where they burst; under
  a roof it still fires its single harmless flak burst.
- New figures for drill (a drilling rig over its hole), tunnel (a portal in the rock
  with rails and a mine cart), leveler (a bulldozer), miner (a mine cart full of ore),
  bridge (an arched bridge over a stream), wall (a castle wall with gate and towers)
  and staircase (a spiral staircase in its shaft).

## [1.8.0] - 2026-10-22

### Added
- Nine particles of the mod's own, with their own sprites: fuse sparks, a shock wave
  ring flat on the ground, swelling smoke balls, glowing embers, tumbling confetti,
  frost twinkles, dark motes that spiral into black holes and ender TNT, crackling
  electric arcs and green radiation motes.
- Every TNT goes off with a shock wave ring and smoke sized to how hard it hits;
  fire, frost, lightning, ender and party kinds add their own particles.
- Big detonations shake the camera of players nearby, and kinds with a mood of
  their own tint the screen (icy blue, poison green, blessing gold, fire orange ...).
- In contaminated land green motes of radiation float around the player.

## [1.7.0] - 2026-10-22

### Added
- Five party kinds that break nothing and hurt no one: confetti, cake (with candles
  and Happy Birthday), disco, chicken rain and sheep dye with rainbow sheep.
- TNT sling: throws lit TNT in an arc; sneak to throw it sticky, so it clings to the
  first creature it touches.
- Handbook: a book and a TNT give a written book with one page per kind.
- An advancement tab of its own and two statistics (TNT set off, TNT thrown).

### Changed
- The remote detonator links up to sixteen bombs and fires them as a salvo; holding
  it marks each linked bomb, sneaking clears them.
- The timer fuse shows its countdown floating over the bomb and can be set to 5 s.

## [1.6.0] - 2026-10-21

### Added
- A detailed 3D figure on top of every TNT block that shows what it does: a
  castle tower, torn ground with a lava crack, a wheat field, a sitting wolf,
  a firework battery, a horseshoe magnet and so on. It is part of the block model, so it shows on the
  placed block, on the lit TNT and in the inventory.
- Every arsenal TNT sparks its own way while its fuse burns (snow, flames,
  hearts, portal shimmer …), more and more towards the end.

### Changed
- Calmer textures: clean 16×16 faces without noise, each kind with its own
  colours and emblem, so the kinds are easy to tell apart.
- More spectacular and more useful effects: fire TNT rolls out a ring of
  napalm, lightning TNT chains into the nearest monsters and kills them,
  storm TNT aims at monsters, ender TNT only sends monsters away, miner TNT
  drops its ores in one pile, water TNT puts out fire, lava TNT spouts a
  fountain, big blasts throw up debris and dust.
- Hazmat suit in the style of vanilla armour: worn it is leather dyed yellow
  with a visor, respirator, belt and rubber boots; new item icons.
- The bucket of contaminated water is a real bucket with glowing green water.
- One nuke: the arsenal's "Nuke TNT" is gone, the atomic bomb of the nuclear
  part is the only one. Nuke TNT in worlds and inventories turns into it.
- Only the nuclear bombs sound the siren; the other big kinds are quiet again.
- Radiation lasts two Minecraft days (`radiationTicks`, 48000) and then is gone
  for good, instead of halving every four minutes. Fallout, irradiated earth and
  contaminated water turn back to normal once it is over.

### Fixed
- If the server stops mid-detonation, stasis no longer leaves mobs frozen for
  good, the x-ray TNT puts its glass back, and gravity and black-hole rubble no
  longer hangs in the air.
- The bee TNT no longer plays the bees' looping drone once at full volume.

## [1.5.0] - 2026-10-12

### Changed
- One mod, one tab: the nuclear items follow the sixty kinds in the TNT Arsenal
  creative tab; the separate nuclear tab is gone.
- The remote detonator and the timer fuse work on every arsenal TNT and on plain
  TNT, not only on the nuclear bombs.
- The heavy kinds (giga, nuke, antimatter, black hole, earthquake, volcano)
  sound the siren while their fuse burns, like the nuclear bombs.
- Radiation is one effect, *Radiation* I–IV with its own icon, instead of
  nausea, weakness, hunger, slowness, poison and wither. RadAway no longer
  makes you sick.
- Radiation fades faster: chunks halve every four minutes by default (was three
  Minecraft days), a player's dose falls by 2 % a second out of the radiation
  (was 0.2 mSv), the nuke kind's fallout lasts two minutes (was five).

### Added
- Sounds of its own, synthesised by `tools/sounds.py`: siren, Geiger clicks,
  nuclear blast, countdown beep, detonator.

### Fixed
- Meteor and arrow rain under a roof (Nether, caves, sky islands) start just
  below the ceiling instead of not at all or inside the rock.
- Storm lightning and the earthquake no longer aim at the Nether roof.
- Miner, x-ray, nether, sculk, end and the nuke's scorching recognise the rock
  and soil of modded dimensions (Aerial Hell, Limbo, Chrono Dawn …).

## [1.4.1] - 2026-10-08

### Added
- `/nuclear clear <radius>` removes all radiation around the caller, including
  blasts that would otherwise come back when a chunk loads.

## [1.4.0] - 2026-10-08

### Added
- Iodine tablets (an iodine effect that halves the dose taken in), RadAway
  (-200 mSv), a four-piece hazmat suit (a fifth per piece, 90 % as a set) and
  lead blocks that shield four times as well as stone.
- Irradiated zombies: zombies in contaminated land turn tougher and faster,
  and every hit adds 25 mSv.
- Timer fuse with 10 s to 5 min and a countdown above the hotbar.
- Siren block, and a siren over every lit bomb while its fuse burns.
- EMP: detonators, timer fuses and Geiger counters go dead for a minute,
  lamps and copper bulbs go out.
- Fallout rain after every detonation; standing in it doubles the intake.
- Bunker door made of lead.
- Eight advancements in a tab "TNT Arsenal: Nuclear".
- `/nuclear config [key value]` lists and changes the settings.

## [1.3.1] - 2026-10-08

### Fixed
- The nuclear bombs no longer destroy chests, other blocks with contents or
  obsidian-hard blocks in their core, as TNT Arsenal promises for every kind.
- At most two nuclear explosions or fallouts work per dimension at a time;
  further ones wait their turn. Before, a row of bombs lit together could
  freeze the server.

## [1.3.0] - 2026-10-08

### Added
- The nuclear arsenal, formerly the separate mod Nuclear TNT, is now part of
  TNT Arsenal: mini nuke, nuke and tsar bomba with a tick-spread explosion,
  remote detonator, flash, shaking ground and mushroom cloud, radiation in
  chunks and players, fallout, irradiated earth, trinitite, contaminated
  water, green fog and the commands under `/nuclear`.
- Geiger counter: shows the dose rate and your dose above the hotbar and
  ticks faster the hotter it gets.
- The nuke from the sixty kinds leaves lasting radiation in its chunks, so
  the Geiger counter reads its crater too.

### Changed
- The mod now has client code (flash, mushroom cloud, green fog), and the
  build compiles against the client jar as well; `tools/fetch-libs.sh`
  fetches it with the game's libraries and all Fabric API modules.

## [1.2.2] - 2026-10-05

### Fixed
- The nuke releases the chunks it keeps loaded even when its crater or
  scorching task fails or the server stops mid-detonation. Before, up to
  100 chunks could stay force-loaded for good.

## [1.2.1] - 2026-10-05

### Changed
- The nuke carves a crater 110 wide and about 37 deep, with a heat flash
  within 72 blocks, three rings of blasts, a blast wave to 96 blocks, a
  bigger mushroom cloud, scorched land to 70 blocks and five minutes of
  fallout within 96. Its chunks stay loaded until the crater is finished.
- Gravity TNT tears the ground loose, whirls the blocks up and hurls them
  away; the black hole swallows blocks one by one.

### Fixed
- Grass counts as ground again. Minecraft 26.2 moved it from `#dirt` to
  `#substrate_overworld`, so miner, x-ray, sculk, desert, end and nether
  TNT skipped grass blocks.

## [1.2.0] - 2026-10-05

### Added
- 18 new kinds: sculk, desert, end, rainbow, earthquake (fissures down to
  just above bedrock), volcano, spiral staircase, bunker, platform, tower,
  curse, blessing, stasis, bees, wolf pack, golems, flak and halloween.
- The nuke scorches the land and leaves three minutes of radiation: poison,
  hunger, weakness, wither near ground zero, green motes and a Geiger
  counter for every player inside.

### Changed
- Lava TNT gets its own drop emblem; the volcano emblem goes to the volcano.

## [1.1.0] - 2026-10-05

### Added
- 23 new kinds: black hole, antigravity, bridge, wall, horde, sonic, smoke,
  x-ray, nature, antimatter, nether, meteor, arrow rain, cobweb, storm,
  magnet, sponge, torch, shockwave, dome, chain reaction, lumber and harvest.
- New hand-drawn textures for every kind, generated by `tools/textures.py`.

### Changed
- Drill and tunnel dig clean 3×3 passages with ladder or torches, drop ores
  and seal liquids; the leveler grows to radius 10, 16 high.
- Giga and nuke use block-explosion drop rules; the nuke gets a round crater
  and a mushroom cloud; gravity really pulls before it explodes; fireworks
  are real fireworks.
- Long detonations run over several ticks and never load chunks.

### Fixed
- Blocks that remove or swap blocks never touch bedrock, obsidian-hard
  blocks, blocks with contents or other TNT. The leveler used to delete
  obsidian.

## [1.0.0] - 2026-10-04

### Added
- Nineteen kinds of TNT with their own textures, crafted at the table so
  JEI and REI show the recipes: mega, giga, nuke, fire, lightning, cluster,
  drill, tunnel, leveler, miner, water, lava, frost, poison, gravity, ender,
  bounce, firework and healing.

[Unreleased]: https://github.com/Dschonas04/tnt-arsenal/compare/v1.2.2...HEAD
[1.2.2]: https://github.com/Dschonas04/tnt-arsenal/compare/v1.2.1...v1.2.2
[1.2.1]: https://github.com/Dschonas04/tnt-arsenal/compare/v1.2.0...v1.2.1
[1.2.0]: https://github.com/Dschonas04/tnt-arsenal/compare/v1.1.0...v1.2.0
[1.1.0]: https://github.com/Dschonas04/tnt-arsenal/compare/v1.0.0...v1.1.0
[1.0.0]: https://github.com/Dschonas04/tnt-arsenal/releases/tag/v1.0.0
