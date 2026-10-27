package io.github.benji377.timety.services.backup

import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.benji377.timety.data.model.task.Priority
import io.github.benji377.timety.data.model.task.RecurrenceUnit
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import java.time.Instant

// org.json is an Android class, so these run under Robolectric.
@RunWith(AndroidJUnit4::class)
class JsonReadTest {
    @Test
    fun missingNullAndUnreadableValues_readAsAbsent() {
        val json = JSONObject("""{"a": null, "when": "not a date"}""")
        assertNull(json.stringOrNull("a"))
        assertNull(json.stringOrNull("missing"))
        assertNull(json.intOrNull("a"))
        assertNull(json.instantOrNull("when"))
    }

    @Test
    fun enumOr_acceptsLegacyCamelCase_andFallsBack() {
        val json = JSONObject("""{"p": "high", "x": "nope", "w": "WEEK"}""")
        assertEquals(Priority.HIGH, json.enumOr("p", Priority.MEDIUM))
        assertEquals(Priority.MEDIUM, json.enumOr("x", Priority.MEDIUM))
        assertEquals(RecurrenceUnit.WEEK, json.enumOr("w", RecurrenceUnit.DAY))
    }

    @Test
    fun instants_acceptUtcAndOffsetForms_andListsSkipBadEntries() {
        val expected = Instant.parse("2026-10-27T10:00:00Z")
        assertEquals(expected, parseInstantFlexible("2026-10-27T10:00:00Z"))
        assertEquals(expected, parseInstantFlexible("2026-10-27T12:00:00+02:00"))
        val json = JSONObject("""{"l": ["2026-10-27T10:00:00Z", "garbage"]}""")
        assertEquals(listOf(expected), json.instantList("l"))
    }
}
