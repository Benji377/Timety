package io.github.benji377.timety.util.datetime

import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale


object AppDateFormatUtils {


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


    fun formatTimeWithSeconds(
        instant: Instant,
        use24Hour: Boolean,
        zone: ZoneId = ZoneId.systemDefault(),
        locale: Locale = Locale.getDefault()
    ): String = instant.atZone(zone).toLocalTime().format(
        DateTimeFormatter.ofPattern(if (use24Hour) "HH:mm:ss" else "h:mm:ss a", locale)
    )


    fun formatTimeOfDay(
        hour: Int,
        minute: Int,
        use24Hour: Boolean,
        locale: Locale = Locale.getDefault()
    ): String = formatTime(LocalTime.of(hour, minute), use24Hour, locale)


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


    fun formatMonthYear(
        date: LocalDate,
        locale: Locale = Locale.getDefault()
    ): String = date.format(DateTimeFormatter.ofPattern("MMMM yyyy", locale))


    fun formatShortDate(
        date: LocalDate,
        locale: Locale = Locale.getDefault()
    ): String = date.format(DateTimeFormatter.ofPattern("MMM d", locale))


    fun formatWeekdayDay(
        date: LocalDate,
        locale: Locale = Locale.getDefault()
    ): String = date.format(DateTimeFormatter.ofPattern("EEE d", locale))
}
