package com.terralite.engine.input

@JvmRecord
data class InputAction(val name: String) {
    init {
        require(name.isNotBlank()) { "Input action name cannot be blank" }
    }

    companion object {
        @JvmStatic fun of(name: String): InputAction = InputAction(name)
    }
}
