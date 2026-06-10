package com.terralite.engine.time

/**
 * Tracks in-game time. All durations are in nanoseconds.
 */
class GameClock(private val dayLengthNanos: Long) {
    private var elapsedNanos: Long = 0L

    init {
        require(dayLengthNanos > 0) { "Day length must be positive" }
    }

    /** @return day length in nanoseconds */
    fun dayLength(): Long = dayLengthNanos

    /** @return elapsed time in nanoseconds */
    fun elapsed(): Long = elapsedNanos

    fun day(): Long = Math.floorDiv(elapsedNanos, dayLengthNanos)

    fun dayProgress(): Double = (elapsedNanos % dayLengthNanos).toDouble() / dayLengthNanos.toDouble()

    fun advance(deltaNanos: Long) {
        require(deltaNanos >= 0) { "Clock delta cannot be negative" }
        elapsedNanos += deltaNanos
    }

    companion object {
        private const val TWENTY_MINUTES_NANOS = 20L * 60L * 1_000_000_000L

        @JvmStatic fun defaultClock(): GameClock = GameClock(TWENTY_MINUTES_NANOS)
    }
}
