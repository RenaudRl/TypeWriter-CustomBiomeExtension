# CustomBiome Extension

![Java Version](https://img.shields.io/badge/Java-21-orange)
![Build Status](https://img.shields.io/badge/build-passing-brightgreen)
![Target](https://img.shields.io/badge/Target-Paper%20/%20Folia%20/%20BTC--CORE-blue)

**CustomBiome Extension** is a powerful environment management solution for **TypeWriter**, engineered for **BTC Studio** infrastructure. It allows developers to define and apply unique biomes with fully customizable colors and climate properties.

---

## 🚀 Key Features

### 🎨 Visual Customization
- **Custom Colors**: Define unique colors for Fog, Water, Sky, Grass, and Foliage.
- **Atmospheric Control**: Create immersive environments with specific fog and sky settings.

### 🌦️ Climate & Environment
- **Climate Settings**: Configure temperature and downfall for every custom biome.
- **Dynamic Application**: Apply biomes to specific locations, radii, or WorldEdit selections.

### ⚡ Performance & Synchronization
- **Packet-Based Refresh**: Instant visual updates for players without requiring a server re-log.
- **Folia Compatible**: Designed to work seamlessly with region-based threading.

---

## ⚙️ Configuration

CustomBiome Extension configuration is managed via TypeWriter's manifest system (`custom_biome_definition`).

> [!IMPORTANT]
> Creating new biome definitions requires a **server restart** as it generates a secondary datapack that must be loaded during the server's startup phase.

---

## 🛠 Building & Deployment

Requires **Java 21**.

```bash
# Clone the repository
git clone https://github.com/RenaudRl/TypeWriter-CustomBiomeExtension.git
cd TypeWriter-CustomBiomeExtension

# Build the project
./gradlew clean build
```

### Artifact Locations:
- `build/libs/CustomBiome-[Version].jar`

---

## 🤝 Credits & Inspiration
- **[TypeWriter](https://github.com/gabber235/Typewriter)** - The engine this extension is built for.
- **[BTC Studio](https://github.com/RenaudRl)** - Maintenance and specialized optimizations.

---

## 📜 License
Licensed under the **MIT License**.
