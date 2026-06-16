package btcrenaud.custombiome.nms

import btcrenaud.custombiome.model.BiomeAttributes
import btcrenaud.custombiome.model.BiomeColors
import btcrenaud.custombiome.util.ReflectionUtil
import org.bukkit.Bukkit
import org.bukkit.NamespacedKey
import java.lang.reflect.Method
import java.util.IdentityHashMap
import java.util.Optional

class NMSHandler {

    private val craftServerClass = ReflectionUtil.getCraftClass("CraftServer")
    private val minecraftServerClass = ReflectionUtil.getNMSClass("net.minecraft.server.MinecraftServer")
    private val registryAccessClass = ReflectionUtil.getNMSClass("net.minecraft.core.RegistryAccess")
    private val registryClass = ReflectionUtil.getNMSClass("net.minecraft.core.Registry")
    private val mappedRegistryClass = ReflectionUtil.getNMSClass("net.minecraft.core.MappedRegistry")
    private val registriesClass = ReflectionUtil.getNMSClass("net.minecraft.core.registries.Registries")
    private val resourceKeyClass = ReflectionUtil.getNMSClass("net.minecraft.resources.ResourceKey")
    private val identifierClass = try {
        ReflectionUtil.getNMSClass("net.minecraft.resources.Identifier")
    } catch (e: ClassNotFoundException) {
        try {
            ReflectionUtil.getNMSClass("net.minecraft.resources.ResourceLocation")
        } catch (e2: ClassNotFoundException) {
            ReflectionUtil.getNMSClass("net.minecraft.resources.MinecraftKey")
        }
    }
    private val biomeClass = ReflectionUtil.getNMSClass("net.minecraft.world.level.biome.Biome")
    private val holderClass = ReflectionUtil.getNMSClass("net.minecraft.core.Holder")
    private val biomeBuilderClass = ReflectionUtil.getNMSClass("net.minecraft.world.level.biome.Biome\$BiomeBuilder")
    
    // BiomeSpecialEffects
    private val biomeSpecialEffectsBuilderClass = ReflectionUtil.getNMSClass("net.minecraft.world.level.biome.BiomeSpecialEffects\$Builder")
    private val biomeSpecialEffectsClass = ReflectionUtil.getNMSClass("net.minecraft.world.level.biome.BiomeSpecialEffects")
    
    // EnvironmentAttributes (New for 1.21+)
    private val environmentAttributeMapClass = ReflectionUtil.getNMSClass("net.minecraft.world.attribute.EnvironmentAttributeMap")
    private val environmentAttributeMapBuilderClass = ReflectionUtil.getNMSClass("net.minecraft.world.attribute.EnvironmentAttributeMap\$Builder")
    private val environmentAttributesClass = ReflectionUtil.getNMSClass("net.minecraft.world.attribute.EnvironmentAttributes")
    
    private val registrationInfoClass = try { ReflectionUtil.getNMSClass("net.minecraft.core.RegistrationInfo") } catch (e: Exception) { null }

    // Fields
    private val frozenField = try { ReflectionUtil.getField(mappedRegistryClass, "frozen") } catch (e: Exception) { ReflectionUtil.getField(mappedRegistryClass, "l") }
    private val intrusiveField = try { ReflectionUtil.getField(mappedRegistryClass, "unregisteredIntrusiveHolders") } catch (e: Exception) { ReflectionUtil.getField(mappedRegistryClass, "m") }

    fun createAndRegisterBiome(key: NamespacedKey, colors: BiomeColors, attributes: BiomeAttributes) {
        val server = Bukkit.getServer()
        val getServerMethod = craftServerClass.getMethod("getServer")
        val minecraftServer = getServerMethod.invoke(server)

        val registryAccessMethod = minecraftServerClass.getMethod("registryAccess")
        val registryAccess = registryAccessMethod.invoke(minecraftServer)

        // Get BIOME registry key
        val biomeRegistryKeyField = registriesClass.getField("BIOME")
        val biomeRegistryKey = biomeRegistryKeyField.get(null)

        // Lookup registry
        val lookupMethod = registryAccessClass.getMethod("lookupOrThrow", resourceKeyClass)
        val biomeRegistry = lookupMethod.invoke(registryAccess, biomeRegistryKey)

        // Get Base Biome (Plains)
        val createResourceKeyMethod = resourceKeyClass.getMethod("create", resourceKeyClass, identifierClass)
        
        val identifierFactoryMethod = try {
            identifierClass.getMethod("fromNamespaceAndPath", String::class.java, String::class.java)
        } catch (e: NoSuchMethodException) {
            identifierClass.getMethod("of", String::class.java, String::class.java)
        }
        
        val plainsId = identifierFactoryMethod.invoke(null, "minecraft", "plains")
        val plainsKey = createResourceKeyMethod.invoke(null, biomeRegistryKey, plainsId)
        
        val getMethod = registryClass.getMethod("get", resourceKeyClass)
        val optionalHolder = getMethod.invoke(biomeRegistry, plainsKey) as Optional<*>
        val holder = optionalHolder.get()
        val valueMethod = holderClass.getMethod("value")
        val baseBiome = valueMethod.invoke(holder)

        // --- Build Special Effects (Water, Grass, Foliage) ---
        val effectsBuilder = biomeSpecialEffectsBuilderClass.getConstructor().newInstance()
        
        val getSpecialEffectsMethod = biomeClass.getMethod("getSpecialEffects")
        val originalEffects = getSpecialEffectsMethod.invoke(baseBiome)
        
        // Methods: waterColor(int), grassColorOverride(int), foliageColorOverride(int)
        val waterColorMethod = biomeSpecialEffectsBuilderClass.getMethod("waterColor", Int::class.javaPrimitiveType)
        val foliageColorOverrideMethod = biomeSpecialEffectsBuilderClass.getMethod("foliageColorOverride", Int::class.javaPrimitiveType)
        val grassColorOverrideMethod = biomeSpecialEffectsBuilderClass.getMethod("grassColorOverride", Int::class.javaPrimitiveType)
        val grassColorModifierMethod = biomeSpecialEffectsBuilderClass.getMethod("grassColorModifier", ReflectionUtil.getNMSClass("net.minecraft.world.level.biome.BiomeSpecialEffects\$GrassColorModifier"))
        
        // Getters
        val getWaterColor = biomeSpecialEffectsClass.getMethod("waterColor")
        val getFoliageColorOverride = biomeSpecialEffectsClass.getMethod("foliageColorOverride") // Optional<Integer>
        val getGrassColorOverride = biomeSpecialEffectsClass.getMethod("grassColorOverride") // Optional<Integer>
        val getGrassColorModifier = biomeSpecialEffectsClass.getMethod("grassColorModifier")

        // Set Special Effects
        waterColorMethod.invoke(effectsBuilder, colors.water ?: getWaterColor.invoke(originalEffects))

        if (colors.foliage != null) {
            foliageColorOverrideMethod.invoke(effectsBuilder, colors.foliage)
        } else {
            @Suppress("UNCHECKED_CAST")
            val opt = getFoliageColorOverride.invoke(originalEffects) as Optional<Int>
            if (opt.isPresent) foliageColorOverrideMethod.invoke(effectsBuilder, opt.get())
        }

        if (colors.grass != null) {
            grassColorOverrideMethod.invoke(effectsBuilder, colors.grass)
        } else {
            @Suppress("UNCHECKED_CAST")
            val opt = getGrassColorOverride.invoke(originalEffects) as Optional<Int>
            if (opt.isPresent) grassColorOverrideMethod.invoke(effectsBuilder, opt.get())
        }
        
        grassColorModifierMethod.invoke(effectsBuilder, getGrassColorModifier.invoke(originalEffects))
        val effects = biomeSpecialEffectsBuilderClass.getMethod("build").invoke(effectsBuilder)

        // --- Build Environment Attributes (Fog, Sky, Water Fog) ---
        // EnvironmentAttributeMap.builder()
        val builderMethod = environmentAttributeMapClass.getMethod("builder")
        val attributesBuilder = builderMethod.invoke(null) // EnvironmentAttributeMap.Builder

        // putAll(biome.getAttributes())
        val getAttributesMethod = biomeClass.getMethod("getAttributes")
        val originalAttributes = getAttributesMethod.invoke(baseBiome) // EnvironmentAttributeMap
        
        val putAllMethod = environmentAttributeMapBuilderClass.getMethod("putAll", environmentAttributeMapClass)
        putAllMethod.invoke(attributesBuilder, originalAttributes)

        // set(Attribute, value)
        // Attributes are static fields in EnvironmentAttributes
        val setAttributeMethod = environmentAttributeMapBuilderClass.getMethod("set", 
            ReflectionUtil.getNMSClass("net.minecraft.world.attribute.EnvironmentAttribute"), 
            Any::class.java // Value is Object? Or Integer? Attributes seem to be typed.
        )
        // Wait, set(EnvironmentAttribute<T>, T value)
        // Let's check generic erasure. It's likely Object in reflection.

        // Helpers to get Attribute Keys
        fun getAttrKey(name: String): Any = environmentAttributesClass.getField(name).get(null)

        if (colors.fog != null) {
             setAttributeMethod.invoke(attributesBuilder, getAttrKey("FOG_COLOR"), colors.fog)
        }
        if (colors.sky != null) {
             setAttributeMethod.invoke(attributesBuilder, getAttrKey("SKY_COLOR"), colors.sky)
        }
        if (colors.waterFog != null) {
             setAttributeMethod.invoke(attributesBuilder, getAttrKey("WATER_FOG_COLOR"), colors.waterFog)
        }

        // --- Build Biome ---
        val biomeBuilder = biomeBuilderClass.getConstructor().newInstance()
        
        // Copy basic props
        val hasPrecipitationMethod = biomeClass.getMethod("hasPrecipitation")
        biomeBuilderClass.getMethod("hasPrecipitation", Boolean::class.javaPrimitiveType).invoke(biomeBuilder, hasPrecipitationMethod.invoke(baseBiome))
        
        // Temperature/Downfall
        // For 1.21.11, assume defaults if getters hard to find, or rely on climateSettings if public.
        // Based on analysis, we can safe-set these or try to fetch.
        // Let's use standard values for now to avoid looking up protected fields.
        biomeBuilderClass.getMethod("temperature", Float::class.javaPrimitiveType).invoke(biomeBuilder, 0.8f)
        biomeBuilderClass.getMethod("downfall", Float::class.javaPrimitiveType).invoke(biomeBuilder, 0.4f)
        
        // Apply Effects
        biomeBuilderClass.getMethod("specialEffects", biomeSpecialEffectsClass).invoke(biomeBuilder, effects)
        
        // Apply Attributes
        // putAttributes(EnvironmentAttributeMap.Builder)
        val putAttributesMethod = biomeBuilderClass.getMethod("putAttributes", environmentAttributeMapBuilderClass)
        putAttributesMethod.invoke(biomeBuilder, attributesBuilder)
        
        // Mob settings & Gen settings
        val getMobSettings = biomeClass.getMethod("getMobSettings")
        val getGenerationSettings = biomeClass.getMethod("getGenerationSettings")
        
        biomeBuilderClass.getMethod("mobSpawnSettings", ReflectionUtil.getNMSClass("net.minecraft.world.level.biome.MobSpawnSettings"))
            .invoke(biomeBuilder, getMobSettings.invoke(baseBiome))
            
        biomeBuilderClass.getMethod("generationSettings", ReflectionUtil.getNMSClass("net.minecraft.world.level.biome.BiomeGenerationSettings"))
            .invoke(biomeBuilder, getGenerationSettings.invoke(baseBiome))
            
        val customBiome = biomeBuilderClass.getMethod("build").invoke(biomeBuilder)

        // --- Register ---
        val newId = identifierFactoryMethod.invoke(null, key.namespace, key.key)
        val newKey = createResourceKeyMethod.invoke(null, biomeRegistryKey, newId)
        
        register(biomeRegistry, newKey, customBiome, holder)
    }

    private fun register(registry: Any, key: Any, biome: Any, originalHolder: Any) {
        try {
            frozenField.set(registry, false)
            intrusiveField.set(registry, IdentityHashMap<Any, Any>())

            val createIntrusiveHolderMethod = try {
                mappedRegistryClass.getMethod("createIntrusiveHolder", Any::class.java)
            } catch (e: NoSuchMethodException) {
                 // Fallback or different name? 
                 // It might be protected or have a different name in obfuscated/mapped jars.
                 // However, based on the reference using Mojang mappings, it should be createIntrusiveHolder.
                 // If it fails, we might need to look for methods with signature (Object) -> Holder
                 null
            }
            
            if (createIntrusiveHolderMethod != null) {
                 createIntrusiveHolderMethod.invoke(registry, biome)
            }

            val registerMethod = if (registrationInfoClass != null) {
                 mappedRegistryClass.getMethod("register", resourceKeyClass, Any::class.java, registrationInfoClass)
            } else {
                 mappedRegistryClass.getMethod("register", resourceKeyClass, Any::class.java, Class.forName("net.minecraft.core.Lifecycle"))
            }
            
            var holder: Any? = null
            if (registrationInfoClass != null) {
                 val builtIn = try {
                     registrationInfoClass.getField("BUILT_IN").get(null)
                 } catch (e: Exception) { null }
                 
                 holder = registerMethod.invoke(registry, key, biome, builtIn)
            } else {
                 // Fallback
            }
            
            // Bind tags
            // method$Holder$bindTags.invoke(holder, original.tags().toList());
            if (holder != null) {
                try {
                     // Holder.Reference?
                     // originalHolder is Holder.Reference
                     // originalHolder.tags() -> Stream<TagKey<T>>
                     val tagsMethod = holderClass.getMethod("tags")
                     val tagsStream = tagsMethod.invoke(originalHolder) // Stream
                     
                     val toListMethod = java.util.stream.Stream::class.java.getMethod("toList")
                     val tagsList = toListMethod.invoke(tagsStream)
                     
                     // bindTags(Collection<TagKey<T>>)
                     // It is likely on Holder.Reference or Holder?
                     // Reference code: method$Holder$bindTags.invoke(holder, ...)
                     // logic: ((Holder.Reference)holder).bindTags(tags)
                     val bindTagsMethod = holder.javaClass.getDeclaredMethod("bindTags", Collection::class.java)
                     bindTagsMethod.isAccessible = true
                     bindTagsMethod.invoke(holder, tagsList)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }

            intrusiveField.set(registry, null)
            frozenField.set(registry, true)
        } catch (e: Exception) {
            e.printStackTrace()
            try { frozenField.set(registry, true) } catch (_: Exception) {}
        }
    }
}
