package io.github.benji377.timety.data.model.focus

import java.time.LocalDate

/**
 * The user's daily focus target: [minutes] to focus on each of the ISO [weekdays] (1 = Monday).
 * Zero minutes turns the target off. When on, at least one weekday is always selected.
 */
data class DailyFocusTarget(
    val minutes: Int = DEFAULT_MINUTES,
    val weekdays: Set<Int> = ALL_WEEKDAYS,
) {
    val isEnabled: Boolean get() = minutes > 0

    /** Target minutes for [date], or null when the target is off or [date] is not a target day. */
    fun minutesOn(date: LocalDate): Int? =
        minutes.takeIf { isEnabled && date.dayOfWeek.value in weekdays }

    companion object {
        const val DEFAULT_MINUTES = 90
        const val MAX_MINUTES = 480
        val ALL_WEEKDAYS: Set<Int> = (1..7).toSet()
    }
}
