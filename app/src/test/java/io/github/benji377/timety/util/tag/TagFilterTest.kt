package io.github.benji377.timety.util.tag

import io.github.benji377.timety.data.model.tag.TagEntity
import org.junit.Assert.assertEquals
import org.junit.Test

class TagFilterTest {
    private val work = TagEntity("work", "work", 0)
    private val uni = TagEntity("uni", "University", 0)
    private val meeting = TagEntity("meeting", "meeting", 0)

    private val items = listOf("work meeting", "uni meeting", "report", "untagged")
    private val tagsByItem = mapOf(
        "work meeting" to listOf(work, meeting),
        "uni meeting" to listOf(uni, meeting),
        "report" to listOf(work),
    )

    private fun filter(vararg tags: TagEntity) =
        TagFilter.filter(items, tags.mapTo(HashSet()) { it.id }, tagsByItem) { it }

    @Test
    fun nothingSelected_keepsEverything() = assertEquals(items, filter())

    @Test
    fun oneTag_keepsAllItemsWithIt() {
        assertEquals(listOf("work meeting", "uni meeting"), filter(meeting))
        assertEquals(listOf("work meeting", "report"), filter(work))
    }

    @Test
    fun severalTags_narrowTheResult() {
        assertEquals(listOf("work meeting"), filter(work, meeting))
        assertEquals(emptyList<String>(), filter(work, uni))
    }

    @Test
    fun usedTags_areDistinctSortedAndLimitedToTheGivenItems() {
        assertEquals(listOf(meeting, uni, work), TagFilter.usedTags(items, tagsByItem))
        assertEquals(listOf(work), TagFilter.usedTags(listOf("report", "untagged"), tagsByItem))
    }

    @Test
    fun activeIds_dropTagsNoLongerOnOffer() {
        assertEquals(setOf("work"), TagFilter.activeIds(setOf("work", "gone"), listOf(work, uni)))
    }
}
