package io.github.benji377.timety.util.datetime

import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap


object AppDateFormatUtils {


    fun formatDuration(totalSeconds: Int): String {
        val minutes = totalSeconds / 60
        val seconds = totalSeconds % 60
        return "%02d:%02d".format(minutes, seconds)
    }


    /** Compact human duration: "45m", "2h", "2h 30m". */
    fun formatMinutesCompact(minutes: Int): String {
        if (minutes < 60) return "${minutes}m"
        val hours = minutes / 60
        val rem = minutes % 60
        return if (rem == 0) "${hours}h" else "${hours}h ${rem}m"
    }


    /** Parses "HH:mm" leniently; falls back to the given default and clamps to valid ranges. */
    fun parseHHmm(value: String, defaultHour: Int = 8, defaultMinute: Int = 0): Pair<Int, Int> {
        val parts = value.split(":")
        val hour = parts.getOrNull(0)?.trim()?.toIntOrNull() ?: defaultHour
        val minute = parts.getOrNull(1)?.trim()?.toIntOrNull() ?: defaultMinute
        return hour.coerceIn(0, 23) to minute.coerceIn(0, 59)
    }

    // Formatters are immutable and thread-safe but not cheap to build, and these run per
    // list row on every recomposition - cache them per (pattern, locale).
    private val formatterCache = ConcurrentHashMap<String, DateTimeFormatter>()

    private fun cached(key: String, create: () -> DateTimeFormatter): DateTimeFormatter =
        formatterCache.getOrPut(key, create)

    private fun dateFormatter(dateFormatCode: String, locale: Locale): DateTimeFormatter =
        cached("d|$dateFormatCode|$locale") {
            if (dateFormatCode == "System" || dateFormatCode.isBlank()) {
                DateTimeFormatter.ofLocalizedDate(FormatStyle.SHORT).withLocale(locale)
            } else {
                DateTimeFormatter.ofPattern(dateFormatCode, locale)
            }
        }

    private fun timeFormatter(use24Hour: Boolean, locale: Locale): DateTimeFormatter =
        cached("t|$use24Hour|$locale") {
            DateTimeFormatter.ofPattern(if (use24Hour) "HH:mm" else "h:mm a", locale)
        }


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
        cached("ts|$use24Hour|$locale") {
            DateTimeFormatter.ofPattern(if (use24Hour) "HH:mm:ss" else "h:mm:ss a", locale)
        }
    )


    fun formatTimeOfDay(
        hour: Int,
        minute: Int,
        use24Hour: Boolean,
        locale: Locale = Locale.getDefault()
    ): String = formatTime(LocalTime.of(hour, minute), use24Hour, locale)

    fun formatTimeOfDay(
        hhmm: String,
        use24Hour: Boolean,
        locale: Locale = Locale.getDefault()
    ): String {
        val (hour, minute) = parseHHmm(hhmm)
        return formatTimeOfDay(hour, minute, use24Hour, locale)
    }


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
    ): String = date.format(cached("my|$locale") { DateTimeFormatter.ofPattern("MMMM yyyy", locale) })


    fun formatShortDate(
        date: LocalDate,
        locale: Locale = Locale.getDefault()
    ): String = date.format(cached("sd|$locale") { DateTimeFormatter.ofPattern("MMM d", locale) })


    fun formatWeekdayDay(
        date: LocalDate,
        locale: Locale = Locale.getDefault()
    ): String = date.format(cached("wd|$locale") { DateTimeFormatter.ofPattern("EEE d", locale) })
}
