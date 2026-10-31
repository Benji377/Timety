package io.github.benji377.timety.services.backup

import org.json.JSONObject
import java.time.Instant
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.ZoneId
import java.util.UUID

// Lenient readers for backup payloads: a missing, null or unreadable value reads as absent, so
// old and hand-edited backups still restore.

internal fun JSONObject.stringOrNull(key: String): String? =
    if (has(key) && !isNull(key)) optString(key) else null

internal fun JSONObject.intOrNull(key: String): Int? =
    if (has(key) && !isNull(key)) optInt(key) else null

internal fun JSONObject.instantOrNull(key: String): Instant? =
    stringOrNull(key)?.let(::parseInstantFlexible)

internal fun JSONObject.intList(key: String): List<Int> {
    val array = optJSONArray(key) ?: return emptyList()
    return (0 until array.length()).map { array.getInt(it) }
}

internal fun JSONObject.stringList(key: String): List<String> {
    val array = optJSONArray(key) ?: return emptyList()
    return (0 until array.length()).map { array.getString(it) }
}

/** The array's timestamps, skipping any that don't parse. */
internal fun JSONObject.instantList(key: String): List<Instant> {
    val array = optJSONArray(key) ?: return emptyList()
    return (0 until array.length()).mapNotNull { parseInstantFlexible(array.getString(it)) }
}

internal fun JSONObject.objects(key: String): List<JSONObject> {
    val array = optJSONArray(key) ?: return emptyList()
    return (0 until array.length()).map { array.getJSONObject(it) }
}

/**
 * The enum constant named at [key], or [default]. Legacy backups wrote camelCase names, so those
 * are tried as UPPER_SNAKE_CASE first.
 */
internal inline fun <reified T : Enum<T>> JSONObject.enumOr(key: String, default: T): T {
    val name = stringOrNull(key) ?: return default
    val snake = name.replace(Regex("([a-z])([A-Z]+)"), "$1_$2").uppercase()
    return enumValues<T>().firstOrNull { it.name == snake }
        ?: enumValues<T>().firstOrNull { it.name == name }
        ?: default
}

/** Accepts UTC instants, offset date-times, and local date-times (read in the device's zone). */
internal fun parseInstantFlexible(raw: String): Instant? =
    runCatching { Instant.parse(raw) }.getOrNull()
        ?: runCatching { OffsetDateTime.parse(raw).toInstant() }.getOrNull()
        ?: runCatching { LocalDateTime.parse(raw).atZone(ZoneId.systemDefault()).toInstant() }.getOrNull()

/** An id for a backup row that carries none. */
internal fun newId(): String = UUID.randomUUID().toString()

/** [value], or JSON null for a missing one. */
internal fun orNull(value: Any?): Any = value ?: JSONObject.NULL
