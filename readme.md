# CustomBiome Extension

![Java Version](https://img.shields.io/badge/Java-21-orange)
![Target](https://img.shields.io/badge/Target-Paper-blue)
![Typewriter](https://img.shields.io/badge/Typewriter-0.9.0--beta--177-purple)

**CustomBiome Extension** adds custom biomes to **Typewriter**: define colors and climate in the web editor, paint biomes into the world, or show a biome to a single player without touching the world.

---

## 🚀 Key Features

### 🎨 Visual Customization
- **Custom colors**: fog, water, sky, grass and foliage, with reusable presets shared between biomes.
- **Climate**: temperature and downfall for every custom biome.

### 🗺️ Applying biomes
- **Apply and paint**: set a biome at a location or radius, or paint a region, with an optional snapshot to restore it later.
- **Player overlays**: change what one player (or an audience) sees without changing the world.
- **Packet-based refresh**: players see changes without re-logging.
- **Transition cinematics**: change the biome a player sees during a cinematic.

### 🧭 Tracking and quests
- Enter and leave events, facts, variables and PlaceholderAPI placeholders.
- **Explore Biome Objective**: completes when the player discovers the listed biomes (`biomes`, `requireAll`) and runs its `triggers`. It records discovery itself, no Enter Biome Event is needed.

---

## 📦 Entries

| Category | Entries |
|---|---|
| Manifest | `custom_biome_definition`, `custom_biome_preset` |
| Actions | `apply_biome_action`, `paint_biome_region_action`, `restore_biome_action`, `player_biome_overlay_action`, `refresh_biome_chunks_action` |
| Events | `enter_biome_event`, `leave_biome_event` |
| Facts | `player_biome_fact`, `is_in_custom_biome_fact`, `biome_discovery_fact`, `custom_biome_count_fact` |
| Variables | `current_biome_variable`, `biome_property_variable`, `custom_biome_list_variable` |
| Audience | `biome_overlay_audience`, `biome_region_audience` |
| Cinematic | `biome_transition_cinematic` |
| Objective | `explore_biome_objective` |

Full field reference on the [wiki](https://docs.borntocraftstudio.net/extensions/free/custombiome/).

> [!IMPORTANT]
> Creating a new biome definition requires a **server restart**: the extension also writes a datapack that is read at startup.

---

## 🔣 Placeholders

PlaceholderAPI, prefix `%typewriter_custombiome_<key>%`. Keys: `current`, `id`, `name`, `key`, `namespace`, `is_custom`, `temperature`, `downfall`, `base`, `count`, `list`, `discovered_count`, `discovered_list`.

---

## ⌨️ Commands and permissions

| Command | Permission |
|---|---|
| `/typewriter biome` (parent) | `typewriter.biome` |
| `/typewriter biome list` | `typewriter.biome.list` |
| `/typewriter biome info [player]` | `typewriter.biome.info` |
| `/typewriter biome apply <biome> [radius]` | `typewriter.biome.apply` |
| `/typewriter biome refresh [radius]` (1 to 16) | `typewriter.biome.refresh` |
| `/typewriter biome region <entry>` (uses your WorldEdit selection) | `typewriter.biome.region` |

---

## 🧩 Requirements

- Typewriter engine `0.9.0-beta-177`, on **Paper**.
- **PacketEvents**: biome packets sent to clients.
- **Quest extension** (for the objective entry).
- Optional: WorldEdit / FastAsyncWorldEdit (region painting from a selection), PlaceholderAPI.

---

## 🛠 Building

Requires **Java 21**.

```bash
git clone https://github.com/RenaudRl/TypeWriter-CustomBiomeExtension.git
cd TypeWriter-CustomBiomeExtension
./gradlew clean build
```

Artifact: `build/libs/`.

---

## 🤝 Credits
- **[Typewriter](https://github.com/gabber235/Typewriter)**: the engine this extension is built for.
- **[BTC Studio](https://github.com/RenaudRl)**: maintenance.

## 📜 License
GNU GPLv3 with an additional exception, see `LICENSE` and `LICENSE-EXCEPTION.md`.

## Documentation

[BTC Studio Docs](https://docs.borntocraftstudio.net/extensions/free/custombiome/)
