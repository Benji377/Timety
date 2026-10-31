package io.github.benji377.timety.util.stats

import io.github.benji377.timety.data.model.focus.FocusSessionEntity
import io.github.benji377.timety.data.model.tag.TagEntity
import java.time.LocalDate
import java.time.ZoneId

/** One tag's total, in whatever unit the stat counts. */
data class TagTotal(val tag: TagEntity, val value: Int)

/**
 * Totals per tag, largest first, plus what has no tag. An item with several tags counts under each,
 * so the totals overlap and must not be read as shares of a whole.
 */
data class TagBreakdown(val totals: List<TagTotal>, val untagged: Int) {
    /** The largest single value, for scaling bars against each other. */
    val maxValue: Int get() = maxOf(totals.maxOfOrNull { it.value } ?: 0, untagged)
    val isEmpty: Boolean get() = maxValue == 0
}

object TagStats {
    /** Minutes per tag for [sessions], counting only the part of each that fell in [from]..[to]. */
    fun minutesByTag(
        sessions: List<FocusSessionEntity>,
        sessionTags: Map<String, List<TagEntity>>,
        from: LocalDate,
        to: LocalDate,
        zone: ZoneId,
    ): TagBreakdown = breakdown(
        sessions.map { session ->
            val minutes = FocusSessionDayStats.minutesByDay(session, zone)
                .filterKeys { !it.isBefore(from) && !it.isAfter(to) }
                .values.sum()
            sessionTags[session.id].orEmpty() to minutes
        },
    )

    /** How many of [itemIds] carry each tag. */
    fun countsByTag(
        itemIds: List<String>,
        tagsByItem: Map<String, List<TagEntity>>,
    ): TagBreakdown = breakdown(itemIds.map { tagsByItem[it].orEmpty() to 1 })

    private fun breakdown(items: List<Pair<List<TagEntity>, Int>>): TagBreakdown {
        val totals = LinkedHashMap<String, TagTotal>()
        var untagged = 0
        items.forEach { (tags, value) ->
            if (tags.isEmpty()) untagged += value
            tags.forEach { tag ->
                totals[tag.id] = TagTotal(tag, (totals[tag.id]?.value ?: 0) + value)
            }
        }
        return TagBreakdown(
            totals = totals.values.filter { it.value > 0 }
                .sortedWith(compareBy({ -it.value }, { it.tag.name.lowercase() })),
            untagged = untagged,
        )
    }
}
