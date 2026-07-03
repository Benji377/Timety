package io.github.benji377.timety.util.datetime

/** Utility functions for consistent date and time formatting. Mirrors date_format_utils.dart. */
object AppDateFormatUtils {

    /**
     * Formats a duration in MM:SS format. Useful for timer values.
     * Example: 125 seconds -> "02:05".
     */
    fun formatDuration(totalSeconds: Int): String {
        val minutes = totalSeconds / 60
        val seconds = totalSeconds % 60
        return "%02d:%02d".format(minutes, seconds)
    }
}
