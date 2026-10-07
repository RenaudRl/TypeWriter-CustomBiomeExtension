package btcrenaud.custombiome.injector

import java.lang.reflect.Field
import java.lang.reflect.Method
import java.lang.reflect.ParameterizedType

/**
 * How a colour environment attribute stores its value on the running server.
 *
 * Up to Minecraft 26.2 every colour attribute is a packed `Integer`. 26.3 replaced them with JOML
 * vectors: `Vector3fc` for RGB attributes (sky, fog, water fog, sky light) and `Vector4fc` for ARGB
 * ones (cloud, sunrise/sunset). Handing an `Integer` to a vector-typed attribute is not rejected
 * when it is set; it only blows up when the biome is encoded or sent.
 */
internal enum class ColorForm {
    PACKED_INT,
    RGB_VECTOR,
    ARGB_VECTOR,
}

internal object AttributeValues {

    private const val RGB_VECTOR_TYPE = "Vector3fc"
    private const val ARGB_VECTOR_TYPE = "Vector4fc"

    private const val OPAQUE_ALPHA = 0xFF shl 24

    /**
     * Matched on the simple name so the extension needs no JOML dependency of its own: the class
     * only has to be recognised, never loaded or instantiated.
     */
    fun colorFormOf(valueClass: Class<*>?): ColorForm = when (valueClass?.simpleName) {
        RGB_VECTOR_TYPE -> ColorForm.RGB_VECTOR
        ARGB_VECTOR_TYPE -> ColorForm.ARGB_VECTOR
        else -> ColorForm.PACKED_INT
    }

    /**
     * The `Value` of an `EnvironmentAttribute<Value>` constant, read from its declared generic type,
     * or null when the declaration does not carry a plain class (older servers, raw types).
     */
    fun valueClassOf(attributeConstant: Field): Class<*>? {
        val declared = attributeConstant.genericType as? ParameterizedType ?: return null
        return declared.actualTypeArguments.firstOrNull() as? Class<*>
    }

    /**
     * A colour the entry holds as `0xRRGGBB` made fully opaque, for attributes that carry an alpha
     * channel. The editor only accepts six hex digits, so an alpha of zero would hide the colour.
     */
    fun opaque(rgb: Int): Int = OPAQUE_ALPHA or (rgb and 0xFFFFFF)

    /** The public method [name] of [owner], or null when this server version does not have it. */
    fun methodOrNull(owner: Class<*>, name: String, vararg parameterTypes: Class<*>): Method? =
        try {
            owner.getMethod(name, *parameterTypes)
        } catch (_: NoSuchMethodException) {
            null
        }
}
