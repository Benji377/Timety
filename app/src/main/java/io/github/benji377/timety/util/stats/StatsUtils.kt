package io.github.benji377.timety.util.stats

/** Utility functions for statistical calculations. Mirrors stats_utils.dart. */
object StatsUtils {

    /** Finds the maximum value in an iterable, with an optional minimum baseline. */
    fun maxValue(values: Iterable<Number>, minimum: Double = 0.0): Double {
        var maxValue = minimum
        for (value in values) {
            val d = value.toDouble()
            if (d > maxValue) maxValue = d
        }
        return maxValue
    }
}
