package com.terralite.engine.input

class InputBindings {
    private val bindings: MutableMap<String, InputAction> = LinkedHashMap()

    fun bind(control: String, action: InputAction): InputBindings {
        require(control.isNotBlank()) { "Input control cannot be blank" }
        bindings[control] = action
        return this
    }

    fun actionFor(control: String): InputAction? = bindings[control]

    fun contains(control: String): Boolean = control in bindings

    fun size(): Int = bindings.size
}
