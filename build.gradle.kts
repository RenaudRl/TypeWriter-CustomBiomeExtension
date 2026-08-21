plugins {
    kotlin("jvm") version "2.3.20"
    id("com.typewritermc.module-plugin") version "2.1.0"
}

group = "btcrenaud"
version = "0.6"

repositories {
    maven("https://jitpack.io/")
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
    maven("https://repo.codemc.io/repository/maven-public/")
    maven("https://repo.extendedclip.com/releases/")
    maven("https://maven.enginehub.org/repo/")
    maven("https://maven.typewritermc.com/beta/")
    maven("https://maven.typewritermc.com/external/")
}

dependencies {
    // Biome packets sent to clients.
    compileOnly("com.github.retrooper:packetevents-spigot:2.13.0")

    // Optional: region painting reads a WorldEdit selection when the plugin is present.
    compileOnly("com.fastasyncworldedit:FastAsyncWorldEdit-Bukkit:2.11.1") { isTransitive = false }
    compileOnly("com.fastasyncworldedit:FastAsyncWorldEdit-Core:2.11.1") { isTransitive = false }

    // Gson ships with the server: shading it duplicates classes already on the classpath.
    compileOnly("com.google.code.gson:gson:2.11.0")

    compileOnly("me.clip:placeholderapi:2.11.5")

    // Public coordinate on purpose: this is the published Quest extension, not a sibling project.
    compileOnly("com.typewritermc:QuestExtension:0.9.0")

    testImplementation(kotlin("test"))
}

// The engine, Paper and the rest are compileOnly: the server provides them at runtime. Tests get
// no classpath from that configuration by default, so anything touching an engine type - a
// Position, a Ref - fails to compile in tests only.
configurations["testImplementation"].extendsFrom(configurations["compileOnly"])

typewriter {
    namespace = "custombiome"

    extension {
        name = "CustomBiome"
        shortDescription = "Create and manage custom biomes with full color and climate customization."
        description = """
            Custom Biome Extension lets you define and manage custom biomes directly through TypeWriter.

            Key Features:
            - Define custom biomes with full colour and visual attribute customisation
            - Reusable presets shared between biomes
            - Biomes registered live, with a generated datapack for persistence across restarts
            - Per-player biome overlays: change what one player sees without touching the world
            - Region painting with snapshots and one-action restore
            - Biome transition cinematics
            - Events, facts, variables and a discovery objective
            - PlaceholderAPI integration

            Works on Paper and Folia. WorldEdit is optional and only used for selection painting.
        """.trimIndent()
        engineVersion = "0.9.0-beta-175"
        channel = com.typewritermc.moduleplugin.ReleaseChannel.BETA
        paper()
    }
}

tasks.withType<Test> {
    useJUnitPlatform()
}

kotlin {
    jvmToolchain(21)
}
