plugins {
    kotlin("jvm") version "2.2.10"
    id("com.typewritermc.module-plugin") version "2.1.0"
}

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
    maven("https://repo.codemc.io/repository/maven-public/")
    maven("https://maven.typewritermc.com/external")
    maven("https://maven.enginehub.org/repo/")
    mavenLocal()
}

dependencies {
    // Paper API 1.21.11
    compileOnly("io.papermc.paper:paper-api:1.21.11-R0.1-SNAPSHOT")
    
    // PacketEvents for biome refresh packets
    compileOnly("com.github.retrooper:packetevents-spigot:2.11.0")

    // WorldEdit / FAWE
    compileOnly("com.fastasyncworldedit:FastAsyncWorldEdit-Bukkit:2.11.1") { isTransitive = false }
    compileOnly("com.fastasyncworldedit:FastAsyncWorldEdit-Core:2.11.1") { isTransitive = false }
    
    // Kotlin Coroutines & Serialization
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.8.1")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.8.1")
    
    // JSON
    implementation("com.google.code.gson:gson:2.11.0")
    
    // PlaceholderAPI
    compileOnly("me.clip:placeholderapi:2.11.5")
    
    // Testing
    testImplementation(kotlin("test"))
}



group = "btc.renaud"
version = "0.0.1"

typewriter {
    namespace = "custombiome"

    extension {
        name = "CustomBiome"
        shortDescription = "Create and manage custom biomes with full color and climate customization."
        description = """
            Custom Biome Extension allows you to define and manage custom biomes directly through TypeWriter.
            
            Key Features:
            - Define custom biomes with full color customization (fog, water, sky, grass, foliage)
            - Set temperature and downfall/humidity values
            - Actions to apply biomes to locations with radius support
            - Events for biome enter/leave detection
            - Facts and variables for biome state tracking
            - PlaceholderAPI integration for biome information
            - Integration with BTCSky and ProtectionExtension
            
            Note: Custom biome definitions require a server restart to take effect.
        """.trimIndent()
        engineVersion = "0.9.0-beta-172"
        channel = com.typewritermc.moduleplugin.ReleaseChannel.BETA
        
        paper()
    }
}

kotlin {
    jvmToolchain(21)
}

