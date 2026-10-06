package btcrenaud.custombiome.entries.objective

import org.bukkit.event.Listener
import org.bukkit.plugin.Plugin
import org.bukkit.plugin.PluginManager
import java.lang.reflect.Proxy
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertSame

class ExploreBiomeListenerTest {

    private val registered = mutableListOf<Pair<Listener, Plugin>>()

    private inline fun <reified T : Any> stub(crossinline onCall: (String, Array<Any?>) -> Any?): T =
        Proxy.newProxyInstance(T::class.java.classLoader, arrayOf(T::class.java)) { _, method, args ->
            onCall(method.name, args ?: emptyArray())
        } as T

    private val plugin: Plugin = stub { name, _ -> error("unexpected Plugin call: $name") }

    private val pluginManager: PluginManager = stub { name, args ->
        check(name == "registerEvents") { "unexpected PluginManager call: $name" }
        registered += (args[0] as Listener) to (args[1] as Plugin)
        null
    }

    @Test
    fun `a page without explore objective registers nothing, so movement costs nothing`() {
        val listener = ExploreBiomeListener.register(pluginManager, plugin, emptyList())

        assertNull(listener)
        assertEquals(0, registered.size)
    }

    @Test
    fun `a page with an explore objective registers one movement listener with the extension plugin`() {
        val objective = ExploreBiomeObjectiveEntry(id = "explore", biomes = listOf("minecraft:desert"))

        val listener = ExploreBiomeListener.register(pluginManager, plugin, listOf(objective))

        assertIs<ExploreBiomeListener>(listener)
        assertEquals(1, registered.size)
        assertSame(listener, registered.single().first)
        assertSame(plugin, registered.single().second)
    }
}
