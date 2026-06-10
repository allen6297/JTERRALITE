package com.terralite.core.registry

/**
 * A namespaced identifier, e.g. `terralite:blocks/stone`.
 *
 * Both components must be lowercase alphanumeric with `_`, `.`, `/`, or `-`
 * (namespace also allows `-`; path also allows `/`).
 */
@JvmRecord
data class ResourceId(val namespace: String, val path: String) {

    init {
        require(VALID_NAMESPACE.matches(namespace)) { "Invalid namespace: $namespace" }
        require(VALID_PATH.matches(path)) { "Invalid path: $path" }
    }

    override fun toString(): String = "$namespace:$path"

    companion object {
        private val VALID_NAMESPACE = Regex("[a-z0-9_.-]+")
        private val VALID_PATH = Regex("[a-z0-9_./-]+")

        @JvmStatic
        fun of(namespace: String, path: String): ResourceId = ResourceId(namespace, path)

        /** Alias for [parse] — matches the Java API shorthand `ResourceId.id("ns:path")`. */
        @JvmStatic
        fun id(value: String): ResourceId = parse(value)

        @JvmStatic
        fun parse(value: String): ResourceId {
            val sep = value.indexOf(':')
            require(sep > 0 && sep < value.length - 1 && value.indexOf(':', sep + 1) == -1) {
                "Invalid resource id: $value"
            }
            return ResourceId(value.substring(0, sep), value.substring(sep + 1))
        }
    }
}
