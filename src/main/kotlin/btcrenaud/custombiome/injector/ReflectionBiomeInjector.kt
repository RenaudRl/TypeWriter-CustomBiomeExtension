package btcrenaud.custombiome.injector

import btcrenaud.custombiome.model.BiomeAttributes
import btcrenaud.custombiome.model.CustomBiomeDefinition
import com.google.gson.JsonObject
import org.bukkit.Bukkit
import org.bukkit.NamespacedKey
import org.slf4j.LoggerFactory
import java.lang.reflect.Field
import java.lang.reflect.Method
import java.util.Optional

/**
 * Reflection reports whatever a called method threw as an [InvocationTargetException], whose own
 * message is always null. Unwrapping is what turns "injection failed: null" into a usable report.
 */
private fun Throwable.unwrap(): Throwable = when (this) {
    is java.lang.reflect.InvocationTargetException -> targetException ?: this
    is ExceptionInInitializerError -> cause ?: this
    else -> this
}

private fun Throwable.describe(): String =
    "${javaClass.simpleName}: ${message ?: "no message"}"

/**
 * Live biome registration through server internals.
 *
 * Every reflective lookup this needs is resolved once, up front, in [Internals]. If any of them
 * is missing the injector reports itself unsupported with a precise reason instead of throwing
 * halfway through a registration and leaving the registry unfrozen.
 *
 * Signatures verified against Minecraft 26.2 (`Biome$BiomeBuilder`, `EnvironmentAttributeMap$Builder`,
 * `EnvironmentAttributes`, `SharedConstants`). Vanilla biomes are no longer shipped as JSON, so the
 * datapack representation is produced by encoding the built biome with `Biome.DIRECT_CODEC`.
 */
class ReflectionBiomeInjector private constructor(private val internals: Internals) : BiomeInjector {

    override val isSupported: Boolean = true

    override val describe: String = "live registry injection (reflection)"

    private val logger = LoggerFactory.getLogger(ReflectionBiomeInjector::class.java)

    companion object {
        private val logger = LoggerFactory.getLogger(ReflectionBiomeInjector::class.java)

        /**
         * Builds an injector for the running server, or an [UnsupportedBiomeInjector] carrying the
         * reason why live injection is impossible here.
         */
        fun create(): BiomeInjector = try {
            ReflectionBiomeInjector(Internals())
        } catch (error: Throwable) {
            val reason = "${error.javaClass.simpleName}: ${error.message}"
            logger.warn("Live biome injection unavailable on this server ({}). " +
                "Custom biomes will only exist after a restart, through the generated datapack.", reason)
            UnsupportedBiomeInjector(reason)
        }
    }

    override fun inject(definition: CustomBiomeDefinition): BiomeInjectionResult {
        return try {
            val registry = internals.biomeRegistry()
            val key = internals.resourceKey(definition.key)

            if (internals.isRegistered(registry, key)) return BiomeInjectionResult.AlreadyPresent

            val baseHolder = internals.baseHolder(registry, definition.baseKey)
                ?: return BiomeInjectionResult.Failed(
                    "base biome ${definition.baseKey ?: "minecraft:plains"} does not exist on this server"
                )

            val biome = buildBiome(baseHolder, definition)

            // Registering a biome the server cannot serialise breaks registry sync, which rejects
            // every player login. Encoding it first turns a bad field value into a refused biome
            // and a precise message, instead of an unreachable server.
            try {
                internals.encodeBiome(biome)
            } catch (invalid: Throwable) {
                val reason = invalid.unwrap()
                return BiomeInjectionResult.Failed("rejected by the server: ${reason.message}", reason)
            }

            internals.register(registry, key, biome, baseHolder)

            // An entry that is present but unbound is worse than an absent one: it crashes
            // registry sync when a player joins. Never report success without checking.
            if (!internals.isBound(registry, key)) {
                return BiomeInjectionResult.Failed(
                    "registered but left unbound; the server would reject player logins"
                )
            }

            BiomeInjectionResult.Injected
        } catch (error: Throwable) {
            val cause = error.unwrap()
            // Log the stack trace: the message alone rarely says which reflective call gave up.
            logger.warn("Live injection of ${definition.key} failed", cause)
            BiomeInjectionResult.Failed("injection failed: ${cause.describe()}", cause)
        }
    }

    override fun encodeToJson(definition: CustomBiomeDefinition): JsonObject? {
        return try {
            val registry = internals.biomeRegistry()
            val baseHolder = internals.baseHolder(registry, definition.baseKey) ?: return null
            val biome = buildBiome(baseHolder, definition)
            internals.encodeBiome(biome)
        } catch (error: Throwable) {
            logger.warn("Could not encode biome ${definition.key} for the datapack", error.unwrap())
            null
        }
    }

    override fun dataPackFormat(): Int? = internals.dataPackFormat()

    override fun networkId(key: NamespacedKey): Int? = try {
        internals.networkId(key)
    } catch (error: Throwable) {
        logger.debug("Could not read the network id of {}: {}", key, error.message)
        null
    }

    /**
     * Builds a biome that inherits everything from [baseHolder] and overrides only what the
     * definition actually sets. A field left empty in the entry must keep the base value — that
     * contract is enforced here rather than in each caller.
     */
    private fun buildBiome(baseHolder: Any, definition: CustomBiomeDefinition): Any = with(internals) {
        val base = holderValue(baseHolder)
        val builder = biomeBuilderClass.getConstructor().newInstance()

        // Climate: the base biome's own values unless overridden, including the temperature
        // modifier — dropping it would silently thaw a frozen base biome.
        val climate = climateSettingsField.get(base)
        val climateClass = climate.javaClass
        val baseTemperature = climateClass.getMethod("temperature").invoke(climate) as Float
        val baseDownfall = climateClass.getMethod("downfall").invoke(climate) as Float

        biomeBuilderClass.getMethod("hasPrecipitation", Boolean::class.javaPrimitiveType)
            .invoke(builder, biomeClass.getMethod("hasPrecipitation").invoke(base))
        biomeBuilderClass.getMethod("temperature", Float::class.javaPrimitiveType)
            .invoke(builder, definition.temperature?.toFloat() ?: baseTemperature)
        biomeBuilderClass.getMethod("downfall", Float::class.javaPrimitiveType)
            .invoke(builder, definition.downfall?.toFloat() ?: baseDownfall)
        biomeBuilderClass.getMethod("temperatureAdjustment", temperatureModifierClass)
            .invoke(builder, climateClass.getMethod("temperatureModifier").invoke(climate))

        biomeBuilderClass.getMethod("specialEffects", biomeSpecialEffectsClass)
            .invoke(builder, buildSpecialEffects(base, definition))
        biomeBuilderClass.getMethod("putAttributes", environmentAttributeMapBuilderClass)
            .invoke(builder, buildAttributes(base, definition.attributes))

        // Mobs and world generation always come from the base biome: this extension retextures
        // biomes, it does not redefine what spawns or generates in them.
        biomeBuilderClass.getMethod("mobSpawnSettings", mobSpawnSettingsClass)
            .invoke(builder, biomeClass.getMethod("getMobSettings").invoke(base))
        biomeBuilderClass.getMethod("generationSettings", biomeGenerationSettingsClass)
            .invoke(builder, biomeClass.getMethod("getGenerationSettings").invoke(base))

        biomeBuilderClass.getMethod("build").invoke(builder)
    }

    private fun buildSpecialEffects(base: Any, definition: CustomBiomeDefinition): Any = with(internals) {
        val builder = biomeSpecialEffectsBuilderClass.getConstructor().newInstance()
        val original = biomeClass.getMethod("getSpecialEffects").invoke(base)
        val colors = definition.colors

        val waterColor = colors.water
            ?: biomeSpecialEffectsClass.getMethod("waterColor").invoke(original)
        biomeSpecialEffectsBuilderClass.getMethod("waterColor", Int::class.javaPrimitiveType)
            .invoke(builder, waterColor)

        copyOptionalColor(builder, original, colors.foliage, "foliageColorOverride")
        copyOptionalColor(builder, original, colors.grass, "grassColorOverride")

        biomeSpecialEffectsBuilderClass.getMethod("grassColorModifier", grassColorModifierClass)
            .invoke(builder, biomeSpecialEffectsClass.getMethod("grassColorModifier").invoke(original))

        biomeSpecialEffectsBuilderClass.getMethod("build").invoke(builder)
    }

    /** Applies an override when set, otherwise carries the base biome's value over when it had one. */
    private fun copyOptionalColor(builder: Any, original: Any, override: Int?, name: String) {
        val setter = internals.biomeSpecialEffectsBuilderClass.getMethod(name, Int::class.javaPrimitiveType)
        if (override != null) {
            setter.invoke(builder, override)
            return
        }
        val inherited = internals.biomeSpecialEffectsClass.getMethod(name).invoke(original) as? Optional<*> ?: return
        if (inherited.isPresent) setter.invoke(builder, inherited.get())
    }

    private fun buildAttributes(base: Any, attributes: BiomeAttributes): Any = with(internals) {
        val builder = environmentAttributeMapClass.getMethod("builder").invoke(null)
        environmentAttributeMapBuilderClass.getMethod("putAll", environmentAttributeMapClass)
            .invoke(builder, biomeClass.getMethod("getAttributes").invoke(base))

        fun set(field: String, value: Any?) {
            if (value == null) return
            setAttributeMethod.invoke(builder, environmentAttribute(field), value)
        }

        set("SKY_COLOR", attributes.sky)
        set("FOG_COLOR", attributes.fog)
        set("WATER_FOG_COLOR", attributes.waterFog)
        set("CLOUD_COLOR", attributes.cloud)
        set("SKY_LIGHT_COLOR", attributes.skyLight)
        set("SUNRISE_SUNSET_COLOR", attributes.sunriseSunset)

        set("FOG_START_DISTANCE", attributes.fogStartDistance)
        set("FOG_END_DISTANCE", attributes.fogEndDistance)
        set("SKY_FOG_END_DISTANCE", attributes.skyFogEndDistance)
        set("WATER_FOG_START_DISTANCE", attributes.waterFogStartDistance)
        set("WATER_FOG_END_DISTANCE", attributes.waterFogEndDistance)
        set("CLOUD_FOG_END_DISTANCE", attributes.cloudFogEndDistance)

        set("CLOUD_HEIGHT", attributes.cloudHeight)
        set("SKY_LIGHT_FACTOR", attributes.skyLightFactor)

        set("SUN_ANGLE", attributes.sunAngle)
        set("MOON_ANGLE", attributes.moonAngle)
        set("STAR_ANGLE", attributes.starAngle)
        set("STAR_BRIGHTNESS", attributes.starBrightness)
        set("MOON_PHASE", attributes.moonPhase?.let { moonPhase(it) })

        builder
    }

    /**
     * Reflective handles onto the server, resolved eagerly so an unsupported platform is detected
     * before any registry is touched.
     */
    internal class Internals {

        private fun nms(name: String): Class<*> = Class.forName(name)

        val biomeClass: Class<*> = nms("net.minecraft.world.level.biome.Biome")
        val biomeBuilderClass: Class<*> = nms("net.minecraft.world.level.biome.Biome\$BiomeBuilder")
        val temperatureModifierClass: Class<*> = nms("net.minecraft.world.level.biome.Biome\$TemperatureModifier")
        val biomeSpecialEffectsClass: Class<*> = nms("net.minecraft.world.level.biome.BiomeSpecialEffects")
        val biomeSpecialEffectsBuilderClass: Class<*> = nms("net.minecraft.world.level.biome.BiomeSpecialEffects\$Builder")
        val grassColorModifierClass: Class<*> = nms("net.minecraft.world.level.biome.BiomeSpecialEffects\$GrassColorModifier")
        val mobSpawnSettingsClass: Class<*> = nms("net.minecraft.world.level.biome.MobSpawnSettings")
        val biomeGenerationSettingsClass: Class<*> = nms("net.minecraft.world.level.biome.BiomeGenerationSettings")

        val environmentAttributeMapClass: Class<*> = nms("net.minecraft.world.attribute.EnvironmentAttributeMap")
        val environmentAttributeMapBuilderClass: Class<*> = nms("net.minecraft.world.attribute.EnvironmentAttributeMap\$Builder")
        private val environmentAttributeClass: Class<*> = nms("net.minecraft.world.attribute.EnvironmentAttribute")
        private val environmentAttributesClass: Class<*> = nms("net.minecraft.world.attribute.EnvironmentAttributes")
        private val moonPhaseClass: Class<*> = nms("net.minecraft.world.level.MoonPhase")

        private val registryClass: Class<*> = nms("net.minecraft.core.Registry")
        private val mappedRegistryClass: Class<*> = nms("net.minecraft.core.MappedRegistry")
        private val registryAccessClass: Class<*> = nms("net.minecraft.core.RegistryAccess")
        private val registriesClass: Class<*> = nms("net.minecraft.core.registries.Registries")
        private val resourceKeyClass: Class<*> = nms("net.minecraft.resources.ResourceKey")
        private val holderClass: Class<*> = nms("net.minecraft.core.Holder")
        private val minecraftServerClass: Class<*> = nms("net.minecraft.server.MinecraftServer")
        private val sharedConstantsClass: Class<*> = nms("net.minecraft.SharedConstants")

        private val identifierClass: Class<*> = firstAvailable(
            "net.minecraft.resources.Identifier",
            "net.minecraft.resources.ResourceLocation",
        )

        private val registrationInfoClass: Class<*> = nms("net.minecraft.core.RegistrationInfo")

        val holderReferenceClass: Class<*> = nms("net.minecraft.core.Holder\$Reference")

        /** Protected on Holder.Reference; resolved once so an unusable platform fails fast. */
        val bindValueMethod: Method = holderReferenceClass
            .getDeclaredMethod("bindValue", Any::class.java)
            .apply { isAccessible = true }

        val climateSettingsField: Field = biomeClass.getField("climateSettings")

        val setAttributeMethod: Method = environmentAttributeMapBuilderClass
            .getMethod("set", environmentAttributeClass, Any::class.java)

        private val identifierFactory: Method = try {
            identifierClass.getMethod("fromNamespaceAndPath", String::class.java, String::class.java)
        } catch (_: NoSuchMethodException) {
            identifierClass.getMethod("of", String::class.java, String::class.java)
        }

        private val createResourceKey: Method =
            resourceKeyClass.getMethod("create", resourceKeyClass, identifierClass)

        private val biomeRegistryKey: Any = registriesClass.getField("BIOME").get(null)

        // MappedRegistry keeps these private; unfreezing is the only way to add a biome after boot.
        private val frozenField: Field = declared(mappedRegistryClass, "frozen")
        private val intrusiveField: Field = declared(mappedRegistryClass, "unregisteredIntrusiveHolders")

        private fun firstAvailable(vararg names: String): Class<*> {
            for (name in names) runCatching { return nms(name) }
            throw ClassNotFoundException(names.joinToString(" / "))
        }

        private fun declared(owner: Class<*>, name: String): Field =
            owner.getDeclaredField(name).apply { isAccessible = true }

        fun registryAccess(): Any {
            val server = Bukkit.getServer().javaClass.getMethod("getServer").invoke(Bukkit.getServer())
            return minecraftServerClass.getMethod("registryAccess").invoke(server)
        }

        fun biomeRegistry(): Any = registryAccessClass.getMethod("lookupOrThrow", resourceKeyClass)
            .invoke(registryAccess(), biomeRegistryKey)

        fun resourceKey(key: NamespacedKey): Any {
            val id = identifierFactory.invoke(null, key.namespace, key.key)
            return createResourceKey.invoke(null, biomeRegistryKey, id)
        }

        fun isRegistered(registry: Any, key: Any): Boolean {
            val result = registryClass.getMethod("get", resourceKeyClass).invoke(registry, key) as Optional<*>
            return result.isPresent
        }

        fun baseHolder(registry: Any, base: NamespacedKey?): Any? {
            val key = resourceKey(base ?: NamespacedKey.minecraft("plains"))
            val result = registryClass.getMethod("get", resourceKeyClass).invoke(registry, key) as Optional<*>
            return result.orElse(null)
        }

        fun holderValue(holder: Any): Any = holderClass.getMethod("value").invoke(holder)

        fun environmentAttribute(field: String): Any = environmentAttributesClass.getField(field).get(null)

        @Suppress("UNCHECKED_CAST")
        fun moonPhase(name: String): Any? {
            val constant = name.trim().uppercase().replace(' ', '_').replace('-', '_')
            return runCatching {
                java.lang.Enum.valueOf(moonPhaseClass as Class<out Enum<*>>, constant)
            }.getOrNull()
        }

        /**
         * Adds the biome to the frozen registry and re-freezes it, whatever happens. Leaving the
         * registry unfrozen would corrupt every later registry read.
         */
        fun register(registry: Any, key: Any, biome: Any, baseHolder: Any) {
            val previousIntrusive = intrusiveField.get(registry)

            frozenField.set(registry, false)
            // MappedRegistry.register has two paths. A non-null intrusive-holder map means "this
            // value already owns a holder", and it throws when it does not. Biomes built here own
            // nothing, so the field must be null for the registry to create a standalone holder.
            intrusiveField.set(registry, null)

            try {
                val builtIn = registrationInfoClass.getField("BUILT_IN").get(null)
                val holder = mappedRegistryClass
                    .getMethod("register", resourceKeyClass, Any::class.java, registrationInfoClass)
                    .invoke(registry, key, biome, builtIn)

                // register() only binds the key. Binding the value is normally freeze()'s job, but
                // freeze() refuses to run once tags are loaded ("Tags already present before
                // freezing"), which is always the case on a live server. So the one thing freeze()
                // would have done for this entry is done here, and nothing else.
                bindValueMethod.invoke(holder, biome)
                bindTags(holder, baseHolder)
            } finally {
                intrusiveField.set(registry, previousIntrusive)
                frozenField.set(registry, true)
            }
        }

        /** True once the holder for [key] is bound to a value. */
        fun isBound(registry: Any, key: Any): Boolean = runCatching {
            val holder = (registryClass.getMethod("get", resourceKeyClass)
                .invoke(registry, key) as Optional<*>).orElse(null) ?: return false
            holderReferenceClass.getMethod("isBound").invoke(holder) as Boolean
        }.getOrDefault(false)

        /** Inherits the base biome's tags, so vanilla systems keyed on tags keep working. */
        private fun bindTags(holder: Any?, baseHolder: Any) {
            if (holder == null) return
            runCatching {
                val tags = holderClass.getMethod("tags").invoke(baseHolder)
                val list = java.util.stream.Stream::class.java.getMethod("toList").invoke(tags)
                holder.javaClass.getDeclaredMethod("bindTags", Collection::class.java)
                    .apply { isAccessible = true }
                    .invoke(holder, list)
            }
        }

        /** Encodes a built biome with the server's own codec — the only faithful JSON form. */
        fun encodeBiome(biome: Any): JsonObject? {
            val codec = biomeClass.getField("DIRECT_CODEC").get(null)
            val jsonOps = Class.forName("com.mojang.serialization.JsonOps").getField("INSTANCE").get(null)

            // Biomes reference other registries (spawns, features, carvers), which plain JsonOps
            // cannot resolve. Ops backed by the server's registries can.
            val ops = runCatching {
                val registryOps = Class.forName("net.minecraft.resources.RegistryOps")
                val dynamicOps = Class.forName("com.mojang.serialization.DynamicOps")
                val provider = Class.forName("net.minecraft.core.HolderLookup\$Provider")
                registryOps.getMethod("create", dynamicOps, provider)
                    .invoke(null, jsonOps, registryAccess())
            }.getOrDefault(jsonOps)

            val encoded = codec.javaClass.methods
                .first { it.name == "encodeStart" && it.parameterCount == 2 }
                .invoke(codec, ops, biome)

            val result = encoded.javaClass.getMethod("result").invoke(encoded) as Optional<*>
            result.orElse(null)?.let { return it as? JsonObject }

            // A codec rejection is a DataResult error, not an exception. Reading it is the only way
            // to learn *which* value the server refused.
            val reason = runCatching {
                val error = encoded.javaClass.getMethod("error").invoke(encoded) as Optional<*>
                error.orElse(null)?.let { it.javaClass.getMethod("message").invoke(it) as? String }
            }.getOrNull()

            throw IllegalStateException(reason ?: "the server rejected this biome without saying why")
        }

        /** Numeric id the client uses for this biome, read straight from the live registry. */
        fun networkId(key: NamespacedKey): Int? {
            val registry = biomeRegistry()
            val holder = baseHolder(registry, key) ?: return null
            val id = registryClass.getMethod("getId", Any::class.java)
                .invoke(registry, holderValue(holder)) as Int
            return id.takeIf { it >= 0 }
        }

        /** Data pack format of the running server, never hardcoded. */
        fun dataPackFormat(): Int? = runCatching {
            sharedConstantsClass.getField("DATA_PACK_FORMAT_MAJOR").getInt(null)
        }.getOrNull()
    }
}
