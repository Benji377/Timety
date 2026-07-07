package io.github.benji377.timety.migration

import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.time.Instant
import java.util.zip.CRC32

/**
 * TEMPORARY — part of the one-shot Flutter→Kotlin data migration.
 * See docs/flutter-migration.md for how this works and when it can be deleted.
 *
 * Read-only parser for the Hive 2.2.3 box files written by the Flutter app.
 * Ported from hive's `frame.dart` / `binary_reader_impl.dart` (the local copy in
 * ~/.pub-cache was used as the format reference). A box file is a log of frames:
 *
 *   [uint32 LE frameLength][key][value?][uint32 LE crc32]
 *
 * `frameLength` covers the whole frame including the length and CRC fields. A frame
 * with no value bytes is a deletion tombstone. Later frames override earlier ones
 * with the same key, so replaying the log yields the live box contents.
 *
 * Values are decoded straight into the JSON shapes produced by the Flutter app's
 * `BackupService._xxxToJson`, so the output can be fed to the Kotlin
 * [io.github.benji377.timety.services.BackupService] restore path unchanged.
 */
internal object HiveBoxReader {

    /** Live values of the box in key order of last write, tombstones applied. */
    fun readBox(file: File): List<Any?> {
        val buf = ByteBuffer.wrap(file.readBytes()).order(ByteOrder.LITTLE_ENDIAN)
        val entries = LinkedHashMap<Any, Any?>()

        while (buf.remaining() >= 4) {
            val frameStart = buf.position()
            val frameLength = buf.int.toUInt().toLong()
            // Mirrors hive's recovery behavior: an undersized or truncated trailing
            // frame (e.g. a write interrupted by process death) ends the log.
            if (frameLength < 8 || buf.remaining() < frameLength - 4) break

            val crcOffset = frameStart + frameLength.toInt() - 4
            val storedCrc = buf.getInt(crcOffset).toUInt().toLong()
            val crc = CRC32().apply {
                update(buf.array(), frameStart, frameLength.toInt() - 4)
            }.value
            if (crc != storedCrc) break

            val key = readKey(buf)
            if (buf.position() == crcOffset) {
                entries.remove(key) // tombstone
            } else {
                entries[key] = readValue(buf)
            }
            buf.position(crcOffset + 4)
        }
        return entries.values.toList()
    }

    private fun readKey(buf: ByteBuffer): Any = when (val keyType = buf.get().toInt()) {
        0 -> buf.int.toUInt().toLong() // uint key
        1 -> {
            val len = buf.get().toInt() and 0xFF
            val bytes = ByteArray(len).also { buf.get(it) }
            String(bytes, Charsets.UTF_8)
        }

        else -> throw IllegalStateException("Unsupported Hive key type $keyType")
    }

    // FrameValueType constants from hive's frame.dart, plus the adapter typeIds.
    // Registered adapters are stored as @HiveType(typeId) + 32 (TypeRegistryImpl
    // .reservedTypeIds); the built-in DateTime adapters are registered internally as
    // 16 (legacy) and 18 (with timezone) — hive 2.x writes DateTimes with 18.
    private fun readValue(buf: ByteBuffer): Any? = when (val typeId = buf.get().toInt() and 0xFF) {
        0 -> null
        1 -> buf.double.toLong() // hive stores Dart ints as float64
        2 -> buf.double
        3 -> buf.get().toInt() > 0
        4 -> readString(buf)
        5 -> ByteArray(readLength(buf)).also { buf.get(it) }
        6 -> List(readLength(buf)) { buf.double.toLong() }
        7 -> List(readLength(buf)) { buf.double }
        8 -> List(readLength(buf)) { buf.get().toInt() > 0 }
        9 -> List(readLength(buf)) { readString(buf) }
        10 -> List(readLength(buf)) { readValue(buf) }
        11 -> buildMap { repeat(readLength(buf)) { put(readValue(buf), readValue(buf)) } }

        16 -> Instant.ofEpochMilli(buf.double.toLong()) // DateTimeAdapter
        18 -> Instant.ofEpochMilli(buf.double.toLong()) // DateTimeWithTimezoneAdapter
            .also { buf.get() } // trailing isUtc bool; irrelevant for epoch millis

        // Task models (task.g.dart)
        42 -> taskToJson(readFields(buf))
        43 -> enumName(buf, "low", "medium", "high", "veryHigh")
        44 -> enumName(buf, "small", "medium", "large", "veryLarge")
        45 -> readFields(buf).let { f ->
            JSONObject().apply {
                putOrNull("id", f[0])
                putOrNull("title", f[1])
                putOrNull("isCompleted", f[2])
            }
        }

        // Focus models (focus_models.g.dart)
        52 -> focusSessionToJson(readFields(buf))
        53 -> enumName(buf, "stopwatch", "pomodoro", "flexible", "custom")
        54 -> enumName(buf, "focus", "rest")
        55 -> readFields(buf).let { f ->
            JSONObject().apply {
                putOrNull("type", f[0])
                putOrNull("durationMinutes", f[1])
            }
        }

        56 -> readFields(buf).let { f ->
            JSONObject().apply {
                putOrNull("id", f[0])
                putOrNull("name", f[1])
                putOrNull("type", f[2])
                putOrNull("phases", f[3])
                putOrNull("isSystem", f[4])
            }
        }

        57 -> readFields(buf).let { f ->
            JSONObject().apply {
                putOrNull("time", f[0])
                putOrNull("note", f[1])
            }
        }

        58 -> readFields(buf).let { f ->
            JSONObject().apply {
                putOrNull("id", f[0])
                putOrNull("name", f[1])
                putOrNull("colorValue", f[2])
            }
        }

        59 -> enumName(buf, "tag", "task", "habit")

        // Habit models (habit_models.g.dart)
        62 -> habitToJson(readFields(buf))
        63 -> enumName(buf, "daily", "weeklyExact", "weeklyFlexible")

        // User models (user.g.dart)
        72 -> readFields(buf).let { f ->
            JSONObject().apply {
                putOrNull("name", f[0])
                putOrNull("profileImagePath", f[1])
                putOrNull("accountCreated", f[2])
                putOrNull("unlockedAchievements", f[3])
                putOrNull("totalXp", f[4])
            }
        }

        else -> throw IllegalStateException("Unknown Hive typeId $typeId")
    }

    private fun readLength(buf: ByteBuffer): Int = buf.int.toUInt().toInt()

    private fun readString(buf: ByteBuffer): String {
        val bytes = ByteArray(readLength(buf)).also { buf.get(it) }
        return String(bytes, Charsets.UTF_8)
    }

    /** Generated adapters write: [numOfFields byte] then per field [fieldId byte][value]. */
    private fun readFields(buf: ByteBuffer): Map<Int, Any?> {
        val numOfFields = buf.get().toInt() and 0xFF
        return buildMap {
            repeat(numOfFields) { put(buf.get().toInt() and 0xFF, readValue(buf)) }
        }
    }

    /** Generated enum adapters write a single index byte; decode to the Dart `.name`. */
    private fun enumName(buf: ByteBuffer, vararg names: String): String {
        val index = buf.get().toInt() and 0xFF
        return names.getOrElse(index) { names[0] }
    }

    private fun taskToJson(f: Map<Int, Any?>) = JSONObject().apply {
        putOrNull("id", f[0])
        putOrNull("title", f[1])
        putOrNull("description", f[2])
        putOrNull("dueDate", f[3])
        putOrNull("location", f[4])
        putOrNull("priority", f[5])
        putOrNull("reminders", f[6])
        putOrNull("category", f[7])
        putOrNull("size", f[8])
        putOrNull("isCompleted", f[9])
        putOrNull("completedAt", f[10])
        putOrNull("createdAt", f[11])
        putOrNull("subtasks", f[12])
    }

    private fun focusSessionToJson(f: Map<Int, Any?>) = JSONObject().apply {
        putOrNull("id", f[0])
        putOrNull("modeId", f[1])
        putOrNull("startTime", f[2])
        putOrNull("endTime", f[3])
        putOrNull("totalSecondsFocused", f[4])
        putOrNull("distractions", f[5])
        putOrNull("isCompleted", f[6])
        putOrNull("tagId", f[7])
        putOrNull("targetType", f[8])
        putOrNull("targetId", f[9])
        putOrNull("targetLabel", f[10])
    }

    private fun habitToJson(f: Map<Int, Any?>) = JSONObject().apply {
        putOrNull("id", f[0])
        putOrNull("name", f[1])
        putOrNull("frequency", f[2])
        putOrNull("targetDaysPerWeek", f[3])
        putOrNull("targetWeekdays", f[4])
        putOrNull("targetTimeMinutes", f[5])
        putOrNull("completions", f[6])
        putOrNull("createdAt", f[7])
        putOrNull("colorValue", f[8])
        putOrNull("notes", f[9])
        putOrNull("iconCodePoint", f[10])
        putOrNull("stackName", f[11])
        putOrNull("stackOrder", f[12])
    }

    /**
     * Puts a decoded value into the JSON object, converting to the representations
     * the Flutter export used: DateTimes as ISO-8601 strings, lists as JSON arrays.
     */
    private fun JSONObject.putOrNull(key: String, value: Any?) {
        put(key, toJsonValue(value))
    }

    private fun toJsonValue(value: Any?): Any = when (value) {
        null -> JSONObject.NULL
        is Instant -> value.toString()
        is List<*> -> JSONArray().apply { value.forEach { put(toJsonValue(it)) } }
        else -> value
    }
}
