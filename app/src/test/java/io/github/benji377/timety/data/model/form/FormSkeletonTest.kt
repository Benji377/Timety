package io.github.benji377.timety.data.model.form

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** The visibility rule and storage format of Settings > Forms. */
class FormSkeletonTest {

    @Test
    fun missingSetting_keepsTheInputsFromBeforeForms() {
        // An update from 3.1.0: every input the editors had then stays on.
        assertEquals(TaskInput.entries.toSet(), FormSkeleton.parse<TaskInput>(null).on)
        assertEquals(HabitInput.entries.toSet(), FormSkeleton.parse<HabitInput>(null).on)
    }

    @Test
    fun storedSetting_leavesUnlistedInputsOff() {
        // What an input added in a later version sees: it isn't in the stored list, so it's off.
        val skeleton = FormSkeleton.parse<TaskInput>("tags,due")
        assertEquals(setOf(TaskInput.TAGS, TaskInput.DUE), skeleton.on)
        assertFalse(skeleton.isOn(TaskInput.CHECKLIST))
    }

    @Test
    fun storage_roundTrips() {
        val on = setOf(HabitInput.STACK, HabitInput.NOTES)
        val stored = FormSkeleton.serialize(on)
        assertEquals(on, FormSkeleton.parse<HabitInput>(stored).on)
    }

    @Test
    fun storage_ignoresUnknownAndEmptyEntries() {
        // A value written by a newer version must not break parsing; empty means all off.
        assertEquals(setOf(TaskInput.LOCATION), FormSkeleton.parse<TaskInput>("location,somethingNew").on)
        assertEquals(emptySet<TaskInput>(), FormSkeleton.parse<TaskInput>("").on)
    }

    @Test
    fun shows_whenOn_withOrWithoutValue() {
        val skeleton = FormSkeleton(setOf(TaskInput.LOCATION))
        assertTrue(skeleton.shows(TaskInput.LOCATION, hasValue = false))
        assertTrue(skeleton.shows(TaskInput.LOCATION, hasValue = true))
    }

    @Test
    fun shows_whenOff_onlyWithValue() {
        val skeleton = FormSkeleton<TaskInput>(emptySet())
        assertTrue(skeleton.shows(TaskInput.LOCATION, hasValue = true))
        assertFalse(skeleton.shows(TaskInput.LOCATION, hasValue = false))
    }

    @Test
    fun newInstallPreset_leavesThePowerInputsOff() {
        assertEquals(
            setOf(TaskInput.PRIORITY_EFFORT, TaskInput.LOCATION, TaskInput.WINDOW),
            TaskInput.entries.toSet() - FormSkeleton.newInstallPreset<TaskInput>(),
        )
        assertEquals(
            setOf(HabitInput.STACK, HabitInput.TIMES_PER_DAY),
            HabitInput.entries.toSet() - FormSkeleton.newInstallPreset<HabitInput>(),
        )
    }

    @Test
    fun storageValues_areUnique() {
        assertEquals(TaskInput.entries.size, TaskInput.entries.map { it.storageValue }.toSet().size)
        assertEquals(HabitInput.entries.size, HabitInput.entries.map { it.storageValue }.toSet().size)
    }
}
