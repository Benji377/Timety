package io.github.benji377.timety.util.stats


object StatsUtils {


    fun maxValue(values: Iterable<Number>, minimum: Double = 0.0): Double {
        var maxValue = minimum
        for (value in values) {
            val d = value.toDouble()
            if (d > maxValue) maxValue = d
        }
        return maxValue
    }
}
