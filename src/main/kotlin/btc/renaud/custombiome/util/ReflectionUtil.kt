package btc.renaud.custombiome.util

import java.lang.reflect.Field
import java.lang.reflect.Method
import org.bukkit.Bukkit

object ReflectionUtil {
    
    fun getField(clazz: Class<*>, fieldName: String): Field {
        val field = clazz.getDeclaredField(fieldName)
        field.isAccessible = true
        return field
    }

    fun getMethod(clazz: Class<*>, methodName: String, vararg parameterTypes: Class<*>): Method {
        val method = clazz.getDeclaredMethod(methodName, *parameterTypes)
        method.isAccessible = true
        return method
    }

    fun getNMSClass(name: String): Class<*> {
        return Class.forName(name)
    }

    fun getCraftClass(name: String): Class<*> {
        val packageName = Bukkit.getServer().javaClass.getPackage().name
        val parts = packageName.split(".")
        if (parts.size > 3) {
            val version = parts[3]
            return Class.forName("org.bukkit.craftbukkit.$version.$name")
        }
        return Class.forName("org.bukkit.craftbukkit.$name")
    }
}
