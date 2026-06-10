package com.terralite.engine.weather

class WeatherState @JvmOverloads constructor(
    private var type: WeatherType = WeatherType.CLEAR,
    private var intensity: Double = 0.0
) {
    init {
        validateIntensity(intensity)
    }

    fun type(): WeatherType = type
    fun intensity(): Double = intensity

    fun set(type: WeatherType, intensity: Double) {
        validateIntensity(intensity)
        this.type = type
        this.intensity = intensity
    }

    private companion object {
        fun validateIntensity(intensity: Double) {
            require(intensity in 0.0..1.0) { "Weather intensity must be between 0 and 1" }
        }
    }
}
