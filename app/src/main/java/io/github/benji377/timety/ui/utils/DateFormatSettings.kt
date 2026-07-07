package io.github.benji377.timety.ui.utils

import androidx.compose.runtime.staticCompositionLocalOf


data class DateFormatSettings(
    val use24HourFormat: Boolean = true,
    val dateFormatCode: String = "System",
)

val LocalDateFormatSettings = staticCompositionLocalOf { DateFormatSettings() }
