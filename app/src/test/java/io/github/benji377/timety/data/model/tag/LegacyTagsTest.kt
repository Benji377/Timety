package io.github.benji377.timety.data.model.tag

import io.github.benji377.timety.data.model.focus.FocusTargetType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class LegacyTagsTest {
    private val noCategory: (FocusTargetType, String) -> String? = { _, _ -> null }

    private fun merge(
        categories: List<LegacyTag> = emptyList(),
        focusTags: List<LegacyTag> = emptyList(),
        usedNames: List<String> = emptyList(),
    ) = mergeLegacyTags(categories, focusTags, usedNames, newId = { "new" })

    @Test
    fun sameNameInBothSystems_becomesOneTagWithTheCategoryColor() {
        val merged = merge(
            categories = listOf(LegacyTag("c1", "Work", 1)),
            focusTags = listOf(LegacyTag("f1", " work ", 2)),
        )

        assertEquals(listOf(TagEntity("c1", "Work", 1)), merged.tags)
        val session = LegacySession(FocusTargetType.TAG, "f1", "f1", "work")
        assertEquals("c1", merged.idForSession(session, noCategory))
    }

    @Test
    fun unrenamedDefaultTag_isDroppedAndItsSessionsAreUntagged() {
        val merged = merge(focusTags = listOf(LegacyTag("default_tag", "None", 1)))

        assertEquals(emptyList<TagEntity>(), merged.tags)
        val session = LegacySession(FocusTargetType.TAG, "default_tag", "default_tag", "None")
        assertNull(session.deletedTagLabel(listOf(LegacyTag("default_tag", "None", 1))))
        assertNull(merged.idForSession(session, noCategory))
    }

    @Test
    fun renamedDefaultTag_staysAsANormalTag() {
        val merged = merge(focusTags = listOf(LegacyTag("default_tag", "Study", 1)))

        assertEquals(listOf(TagEntity("default_tag", "Study", 1)), merged.tags)
        val session = LegacySession(FocusTargetType.TAG, "default_tag", "default_tag", "Study")
        assertEquals("default_tag", merged.idForSession(session, noCategory))
    }

    @Test
    fun categoryNameWithoutARow_getsTheDefaultColor() {
        val merged = merge(usedNames = listOf("Home", "home ", ""))

        assertEquals(listOf(TagEntity("new", "Home", TagEntity.DEFAULT_COLOR_VALUE)), merged.tags)
        assertEquals("new", merged.idForName("HOME"))
        assertNull(merged.idForName(""))
    }

    @Test
    fun sessionOfADeletedTag_recreatesTheTagFromItsLabel() {
        val session = LegacySession(FocusTargetType.TAG, "gone", "gone", "Reading")
        val label = session.deletedTagLabel(emptyList())

        assertEquals("Reading", label)
        val merged = merge(usedNames = listOfNotNull(label))
        assertEquals("new", merged.idForSession(session, noCategory))
    }

    @Test
    fun taskSession_takesItsTasksCategory_andDeletedTasksStayUntagged() {
        val merged = merge(categories = listOf(LegacyTag("c1", "Work", 1)))
        val categories = mapOf("t1" to "Work")
        val categoryOf: (FocusTargetType, String) -> String? = { _, id -> categories[id] }

        val onTask = LegacySession(FocusTargetType.TASK, "t1", null, "Report")
        val onDeletedTask = LegacySession(FocusTargetType.TASK, "t2", null, "Old")

        assertEquals("c1", merged.idForSession(onTask, categoryOf))
        assertNull(merged.idForSession(onDeletedTask, categoryOf))
        assertNull(onTask.deletedTagLabel(emptyList()))
    }

    @Test
    fun habitAndGoalSessions_stayUntagged() {
        val merged = merge(categories = listOf(LegacyTag("c1", "Run", 1)))

        val onHabit = LegacySession(FocusTargetType.HABIT, "h1", null, "Run")
        val onGoal = LegacySession(FocusTargetType.GOAL, "g1", null, "Run")

        assertNull(merged.idForSession(onHabit, noCategory))
        assertNull(merged.idForSession(onGoal, noCategory))
    }
}
