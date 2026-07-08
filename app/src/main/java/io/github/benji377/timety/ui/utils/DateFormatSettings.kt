package io.github.benji377.timety.ui.utils

import androidx.compose.runtime.staticCompositionLocalOf


/** The user's preferred time/date display format, resolved from settings. */
data class DateFormatSettings(
    val use24HourFormat: Boolean = true,
    val dateFormatCode: String = "System",
)

/** Composition local providing the current [DateFormatSettings] to date/time-rendering composables. */
val LocalDateFormatSettings = staticCompositionLocalOf { DateFormatSettings() }
