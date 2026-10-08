# Changelog

## 0.9 — 2026-10-08

- **Custom biomes work on Paper 26.3 again.** 26.3 removed `Biome.getMobSettings()` (mob spawns are
  now the `natural_mob_spawns` environment attribute, which a custom biome already inherits from its
  base biome) and turned the colour attributes (`sky`, `fog`, `water fog`, `cloud`, `sky light`,
  `sunrise/sunset`) from packed integers into vectors. Both made every biome fail to register live and
  to be written to the datapack. The injector now follows the type the running server declares for
  each colour, so 1.21.11 to 26.2 behave as before.
- Fixed the generated datapack logging "Error reading pack metadata, attempting fallback type" at every
  start. Its `pack.mcmeta` declared a bare `pack_format`, which the server refuses above format 81; it now
  declares `min_format` and `max_format`, the limit being read from the server.

- **`explore_biome_objective` now reads `biomes` and `requireAll`.** Until now `isComplete` had no
  caller, so the two fields did nothing. When a player crosses into a biome (the check is skipped
  unless they crossed a 4x4x4 cell boundary, so walking inside a biome costs nothing), every
  objective listing that biome whose requirement is met, and which is active for the player, fires
  its new `triggers` field. The objective also records discovery itself, so it works without any
  `enter_biome_event` on the page.
- Fixed `enter_biome_event` firing twice when it was the only biome event on the page, and
  `leave_biome_event` or the new objective being starved when a third listener existed: the
  transition of a move is now shared by identity of the move event instead of by a read counter.
  The shared state is locked per player instead of globally.

## 0.5 — 2026-08-12

- **Custom biomes exist immediately.** A definition is registered into the live registry as soon
  as it is saved, instead of only after a server restart. The generated datapack is now the
  persistence layer rather than the only path.
- Fixed the generated datapack being ignored by the server: its pack format was hardcoded to an
  old value, and it was written to a folder the server never reads.
- Fixed `baseBiome` having no effect other than plains. Biomes are built from the base biome held
  by the server and encoded with its own codec, so mob spawns, features and carvers are inherited
  instead of lost.
- Fixed biome changes punching holes in the world: refreshing sent chunk *unload* packets, which
  the server never followed with a resend. Clients now receive a biome update built from the real
  chunk data.
- Fixed `leave_biome_event` never firing. The enter and leave listeners overwrote each other's
  state, so the outcome depended on registration order.
- Fixed painting writing every block of a column instead of one per 4x4x4 cell, and doing it from
  the calling thread. Writes are now scheduled on the region owning each chunk.
- A biome the server would refuse to serialise is rejected at registration, with the offending
  value named. Previously it was accepted and then broke every player login.
- **Per-player biome overlays** (`player_biome_overlay_action`): show one player a different biome
  without touching the world or affecting anyone else.
- **Region painting** (`paint_biome_region_action`) with snapshots, and `restore_biome_action` to
  put a region back.
- **`biome_transition_cinematic`**: change the biome a player sees over the course of a cinematic.
- **`custom_biome_preset`**: share colours and visual attributes between biomes, overridden per
  definition.
- **`explore_biome_objective`** and **`biome_discovery_fact`**: quests completed by visiting
  biomes.
- Every read entry gained a `source` option, choosing between what the player is shown and what
  the world contains.
- Removed `/tw biome setcolor`, which created throwaway biomes that were never persisted, and the
  `HASH` fact mode, whose value was not stable across restarts.
- The extension jar dropped from 2.6 MB to 230 KB: it no longer shades libraries the server
  already provides.
