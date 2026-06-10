package com.terralite.engine.input

class InputState {
    private val pressed: MutableSet<InputAction> = LinkedHashSet()

    fun press(action: InputAction) { pressed += action }
    fun release(action: InputAction) { pressed -= action }

    fun setPressed(action: InputAction, isPressed: Boolean) {
        if (isPressed) press(action) else release(action)
    }

    fun isPressed(action: InputAction): Boolean = action in pressed

    fun clear() { pressed.clear() }

    fun pressedActions(): Set<InputAction> = pressed.toSet()
}
