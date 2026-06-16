dependencies {
    compileOnly("com.github.retrooper:packetevents-spigot:2.11.0")
    compileOnly("com.fastasyncworldedit:FastAsyncWorldEdit-Bukkit:2.11.1")
    compileOnly("com.fastasyncworldedit:FastAsyncWorldEdit-Core:2.11.1")
    compileOnly("org.jetbrains.kotlinx:kotlinx-serialization-json:1.8.1")
    compileOnly("com.google.code.gson:gson:2.11.0")
    compileOnly("me.clip:placeholderapi:2.11.5")
}

plugins {
    kotlin("jvm") version "2.3.20"
    id("com.typewritermc.module-plugin") version "2.1.0"
}

group = "btcrenaud"
version = "0.0.3"

repositories {
    mavenLocal()
    maven("https://jitpack.io/")
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
    maven("https://repo.codemc.io/repository/maven-public/")
    maven("https://maven.typewritermc.com/beta/")
    maven("https://maven.typewritermc.com/external/")
}

typewriter {
    namespace = "custombiome"

    extension {
        name = "CustomBiome"
        shortDescription = "Create and manage custom biomes with full color and climate customization."
        description = """Typewriter extension module providing additional entries for the Typewriter plugin ecosystem. Supports Paper and Folia server platforms with full feature parity. This module extends the core functionality with specialized entries. Compatible with the official Typewriter engine and designed for standalone use."""
        engineVersion = "0.9.0-beta-174"
        channel = com.typewritermc.moduleplugin.ReleaseChannel.BETA
        paper()
    }
}

    

kotlin {
    jvmToolchain(25)
    
}
