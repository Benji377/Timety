package io.github.benji377.timety.util.datetime

import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale

/**
 * Utility functions for consistent date and time formatting. Mirrors date_format_utils.dart and the
 * old Flutter `SettingsProvider.getFormatted*` helpers so the user-configurable date-format code and
 * 24-hour toggle are honored everywhere (not just in Settings).
 *
 * [dateFormatCode] matches the values offered in Settings: "System" (locale default) or a concrete
 * pattern such as "dd/MM/yyyy", "MM/dd/yyyy", "yyyy-MM-dd", "dd.MM.yyyy".
 */
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

    private fun dateFormatter(dateFormatCode: String, locale: Locale): DateTimeFormatter =
        if (dateFormatCode == "System" || dateFormatCode.isBlank()) {
            DateTimeFormatter.ofLocalizedDate(FormatStyle.SHORT).withLocale(locale)
        } else {
            DateTimeFormatter.ofPattern(dateFormatCode, locale)
        }

    private fun timeFormatter(use24Hour: Boolean, locale: Locale): DateTimeFormatter =
        DateTimeFormatter.ofPattern(if (use24Hour) "HH:mm" else "h:mm a", locale)

    /** Mirrors getFormattedDate: honors the user's date-format code. */
    fun formatDate(
        date: LocalDate,
        dateFormatCode: String,
        locale: Locale = Locale.getDefault()
    ): String = date.format(dateFormatter(dateFormatCode, locale))

    fun formatDate(
        instant: Instant,
        dateFormatCode: String,
        zone: ZoneId = ZoneId.systemDefault(),
        locale: Locale = Locale.getDefault()
    ): String = formatDate(instant.atZone(zone).toLocalDate(), dateFormatCode, locale)

    /** Mirrors getFormattedTime: honors the user's 24-hour toggle. */
    fun formatTime(
        time: LocalTime,
        use24Hour: Boolean,
        locale: Locale = Locale.getDefault()
    ): String = time.format(timeFormatter(use24Hour, locale))

    fun formatTime(
        instant: Instant,
        use24Hour: Boolean,
        zone: ZoneId = ZoneId.systemDefault(),
        locale: Locale = Locale.getDefault()
    ): String = formatTime(instant.atZone(zone).toLocalTime(), use24Hour, locale)

    /** Mirrors getFormattedTimeWithSeconds: honors the 24-hour toggle, keeps seconds precision. */
    fun formatTimeWithSeconds(
        instant: Instant,
        use24Hour: Boolean,
        zone: ZoneId = ZoneId.systemDefault(),
        locale: Locale = Locale.getDefault()
    ): String = instant.atZone(zone).toLocalTime().format(
        DateTimeFormatter.ofPattern(if (use24Hour) "HH:mm:ss" else "h:mm:ss a", locale)
    )

    /** Mirrors getFormattedTimeOfDay for an hour/minute pair. */
    fun formatTimeOfDay(
        hour: Int,
        minute: Int,
        use24Hour: Boolean,
        locale: Locale = Locale.getDefault()
    ): String = formatTime(LocalTime.of(hour, minute), use24Hour, locale)

    /** Mirrors getFormattedDateTime: "<date> <time>". */
    fun formatDateTime(
        instant: Instant,
        dateFormatCode: String,
        use24Hour: Boolean,
        zone: ZoneId = ZoneId.systemDefault(),
        locale: Locale = Locale.getDefault()
    ): String {
        val ldt: LocalDateTime = instant.atZone(zone).toLocalDateTime()
        return "${ldt.toLocalDate().format(dateFormatter(dateFormatCode, locale))} " +
            ldt.toLocalTime().format(timeFormatter(use24Hour, locale))
    }

    /** Mirrors getFormattedMonthYear (e.g. "July 2026"). */
    fun formatMonthYear(
        date: LocalDate,
        locale: Locale = Locale.getDefault()
    ): String = date.format(DateTimeFormatter.ofPattern("MMMM yyyy", locale))

    /** Mirrors getFormattedShortDate (e.g. "Jul 4"). */
    fun formatShortDate(
        date: LocalDate,
        locale: Locale = Locale.getDefault()
    ): String = date.format(DateTimeFormatter.ofPattern("MMM d", locale))

    /** Mirrors getFormattedWeekdayDay (e.g. "Sat 4"). */
    fun formatWeekdayDay(
        date: LocalDate,
        locale: Locale = Locale.getDefault()
    ): String = date.format(DateTimeFormatter.ofPattern("EEE d", locale))
}
