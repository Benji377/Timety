package io.github.benji377.timety.ui.utils

import androidx.compose.runtime.staticCompositionLocalOf

/**
 * App-wide date/time formatting preferences, exposed via [LocalDateFormatSettings] so any composable
 * can honor the user's 24-hour toggle and custom date-format code without threading them through every
 * screen. This mirrors the old Flutter setup where `SettingsProvider` was globally reachable and its
 * `getFormatted*` helpers were used across the UI.
 *
 * [dateFormatCode] matches Settings' stored values: "System" (locale default) or a concrete pattern
 * such as "dd/MM/yyyy", "MM/dd/yyyy", "yyyy-MM-dd", "dd.MM.yyyy".
 */
data class DateFormatSettings(
    val use24HourFormat: Boolean = true,
    val dateFormatCode: String = "System",
)

val LocalDateFormatSettings = staticCompositionLocalOf { DateFormatSettings() }
