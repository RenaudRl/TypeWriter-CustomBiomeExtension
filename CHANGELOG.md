# Changelog

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
