package io.github.benji377.timety.util.habit

import io.github.benji377.timety.data.model.habit.HabitCompletionEntity
import io.github.benji377.timety.data.model.habit.HabitEntity
import io.github.benji377.timety.data.model.habit.HabitFrequency
import io.github.benji377.timety.data.model.habit.HabitWithCompletions
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

class HabitUtilsTest {
    private fun habit(id: String = "h1") = HabitEntity(
        id = id,
        name = "Test Habit",
        frequency = HabitFrequency.DAILY,
        createdAt = Instant.EPOCH,
        colorValue = 0,
    )

    private fun completedOn(date: LocalDate) = HabitCompletionEntity(
        habitId = "h1",
        completionDate = date.atStartOfDay(ZoneId.systemDefault()).plusHours(12).toInstant(),
    )

    @Test
    fun testParseWeekdays() {
        assertEquals(emptySet<Int>(), HabitUtils.parseWeekdays(null))
        assertEquals(emptySet<Int>(), HabitUtils.parseWeekdays(""))
        assertEquals(emptySet<Int>(), HabitUtils.parseWeekdays("  "))
        assertEquals(setOf(1, 3, 5), HabitUtils.parseWeekdays("[1,3,5]"))
        assertEquals(setOf(1, 3), HabitUtils.parseWeekdays("[1, 3]"))
        // Non-numeric entries are skipped rather than crashing.
        assertEquals(setOf(2), HabitUtils.parseWeekdays("[2,x]"))
    }

    @Test
    fun testSerializeWeekdaysSortsAndRoundtrips() {
        assertEquals("[1,3,5]", HabitUtils.serializeWeekdays(setOf(5, 1, 3)))
        assertEquals("[]", HabitUtils.serializeWeekdays(emptySet()))
        val days = setOf(2, 4, 7)
        assertEquals(days, HabitUtils.parseWeekdays(HabitUtils.serializeWeekdays(days)))
    }

    private fun step(id: String, done: Boolean, day: LocalDate) = HabitWithCompletions(
        habit(id),
        if (done) listOf(completedOn(day).copy(habitId = id)) else emptyList(),
    )

    @Test
    fun anInOrderStack_locksCheckingForwardAndUncheckingBackward() {
        val day = LocalDate.of(2026, 11, 21)
        val stack = listOf(step("a", true, day), step("b", true, day), step("c", false, day), step("d", false, day))

        assertEquals(
            listOf(StackLock.LATER_STEP_DONE, StackLock.NONE, StackLock.NONE, StackLock.WAITING_FOR_PREVIOUS),
            HabitUtils.stackLocks(stack, day, inOrder = true),
        )
    }

    @Test
    fun aStackThatIsNotInOrder_hasNoLocks() {
        val day = LocalDate.of(2026, 11, 21)
        val stack = listOf(step("a", true, day), step("b", true, day), step("c", false, day), step("d", false, day))

        assertEquals(List(4) { StackLock.NONE }, HabitUtils.stackLocks(stack, day, inOrder = false))
    }

    @Test
    fun locksLookAtTheWholeStack_notJustTheStepsAListShows() {
        val day = LocalDate.now()
        // a done, b open, c done: the Done section only shows a and c, but b is in between.
        val members = listOf(step("a", true, day), step("b", false, day), step("c", true, day))
            .mapIndexed { i, hwc -> hwc.copy(habit = hwc.habit.copy(stackName = "S", stackOrder = i)) }

        val locks = HabitUtils.stackLocksOn(members, day, inOrder = true)

        assertEquals(StackLock.NONE, locks["a"])
        assertEquals(StackLock.NONE, locks["c"])
    }

    @Test
    fun aHabitOffTodaysSchedule_neverBlocksTheRestOfItsStack() {
        val day = LocalDate.now()
        val offToday = HabitWithCompletions(
            habit("off").copy(
                frequency = HabitFrequency.WEEKLY_EXACT,
                targetWeekdays = HabitUtils.serializeWeekdays(setOf(day.plusDays(1).dayOfWeek.value)),
                stackName = "S",
                stackOrder = 0,
            ),
            emptyList(),
        )
        val next = step("next", false, day).let { it.copy(habit = it.habit.copy(stackName = "S", stackOrder = 1)) }

        assertEquals(StackLock.NONE, HabitUtils.stackLocksOn(listOf(offToday, next), day, inOrder = true)["next"])
    }

    @Test
    fun aDoneStepBeforeAnOpenOne_canBeUnchecked() {
        val day = LocalDate.of(2026, 11, 21)
        // The first step is never waiting, and the last done step is never blocked.
        val stack = listOf(step("a", false, day), step("b", true, day))

        assertEquals(listOf(StackLock.NONE, StackLock.NONE), HabitUtils.stackLocks(stack, day, inOrder = true))
    }

    @Test
    fun testIsCompletedOn() {
        val today = LocalDate.now()
        val hwc = HabitWithCompletions(habit(), listOf(completedOn(today)))
        assertTrue(HabitUtils.isCompletedOn(hwc, today))
        assertFalse(HabitUtils.isCompletedOn(hwc, today.minusDays(1)))
        assertFalse(HabitUtils.isCompletedOn(HabitWithCompletions(habit(), emptyList()), today))
    }

    @Test
    fun testStackCompletion() {
        val today = LocalDate.now()
        val done = HabitWithCompletions(habit("a"), listOf(completedOn(today)))
        val notDone = HabitWithCompletions(habit("b"), emptyList())

        assertFalse(HabitUtils.isStackFullyCompleted(emptyList(), today))
        assertFalse(HabitUtils.isStackFullyCompleted(listOf(done, notDone), today))
        assertTrue(HabitUtils.isStackFullyCompleted(listOf(done, done), today))
        assertEquals(1, HabitUtils.getStackCompletionCount(listOf(done, notDone), today))
    }

    @Test
    fun testReminderTimesRoundTrip() {
        assertEquals(emptyList<Int>(), HabitUtils.parseReminderTimes(null))
        assertEquals(emptyList<Int>(), HabitUtils.parseReminderTimes("[]"))
        assertEquals(listOf(480, 1200), HabitUtils.parseReminderTimes("[1200, 480, 480]"))
        assertEquals(listOf(480), HabitUtils.parseReminderTimes("[480,1440,-5]"))
        assertEquals(
            HabitUtils.MAX_REMINDER_TIMES,
            HabitUtils.parseReminderTimes("[1,2,3,4,5,6,7]").size,
        )
        assertEquals(null, HabitUtils.serializeReminderTimes(emptyList()))
        assertEquals("[480,1200]", HabitUtils.serializeReminderTimes(listOf(1200, 480)))
    }

    @Test
    fun testNeedsReminderToday() {
        val today = LocalDate.now()
        val open = HabitWithCompletions(habit(), emptyList())
        val done = HabitWithCompletions(habit(), listOf(completedOn(today)))
        assertTrue(HabitUtils.needsReminderToday(open))
        assertFalse(HabitUtils.needsReminderToday(done))
    }

    @Test
    fun testNeedsReminderTodayForWeeklyHabits() {
        val today = LocalDate.now()
        val otherDay = (1..7).first { it != today.dayOfWeek.value }
        val exactElsewhere = habit().copy(
            frequency = HabitFrequency.WEEKLY_EXACT,
            targetWeekdays = HabitUtils.serializeWeekdays(setOf(otherDay)),
        )
        assertFalse(HabitUtils.needsReminderToday(HabitWithCompletions(exactElsewhere, emptyList())))

        val exactToday = exactElsewhere.copy(
            targetWeekdays = HabitUtils.serializeWeekdays(setOf(today.dayOfWeek.value)),
        )
        assertTrue(HabitUtils.needsReminderToday(HabitWithCompletions(exactToday, emptyList())))

        // Target met on earlier days this week: the habit drops off the list and stops nagging.
        val monday = today.minusDays((today.dayOfWeek.value - 1).toLong())
        val flexible = habit().copy(frequency = HabitFrequency.WEEKLY_FLEXIBLE, targetCount = 1)
        if (monday != today) {
            val met = HabitWithCompletions(flexible, listOf(completedOn(monday)))
            assertFalse(HabitUtils.needsReminderToday(met))
        }
        assertTrue(HabitUtils.needsReminderToday(HabitWithCompletions(flexible, emptyList())))
    }

    @Test
    fun monthlyHabit_isNeverDueAndNeverReminds() {
        val monthly = habit().copy(frequency = HabitFrequency.MONTHLY_FLEXIBLE, targetCount = 2)
        val open = HabitWithCompletions(monthly, emptyList())
        assertFalse(HabitUtils.isHabitDueToday(open))
        assertFalse(HabitUtils.needsReminderToday(open))
        assertEquals(
            null,
            HabitUtils.nextReminderAt(HabitWithCompletions(monthly.copy(reminderTimes = "[540]"), emptyList()), noon),
        )
    }

    @Test
    fun monthlyHabit_countsTheCalendarMonth() {
        val today = LocalDate.now()
        val firstOfMonth = today.withDayOfMonth(1)
        val monthly = habit().copy(frequency = HabitFrequency.MONTHLY_FLEXIBLE, targetCount = 2)
        // The last day of the previous month doesn't count.
        val hwc = HabitWithCompletions(
            monthly,
            listOf(completedOn(firstOfMonth.minusDays(1)), completedOn(firstOfMonth), completedOn(today)),
        )
        assertEquals(2, HabitUtils.getCompletionsThisPeriod(hwc))
        if (firstOfMonth != today) {
            assertEquals(1, HabitUtils.getCompletionsThisPeriod(hwc, includeToday = false))
        }
    }

    @Test
    fun monthlyHabit_goalMetOnEarlierDaysOfTheMonth() {
        val today = LocalDate.now()
        val firstOfMonth = today.withDayOfMonth(1)
        val monthly = habit().copy(frequency = HabitFrequency.MONTHLY_FLEXIBLE, targetCount = 1)
        assertFalse(HabitUtils.isPeriodGoalMet(HabitWithCompletions(monthly, emptyList())))
        if (firstOfMonth != today) {
            assertTrue(HabitUtils.isPeriodGoalMet(HabitWithCompletions(monthly, listOf(completedOn(firstOfMonth)))))
        }
    }

    // Next reminder: one pending reminder per habit, today's skipped once it's done.

    private val zone = ZoneId.systemDefault()
    private val noon = LocalDate.now().atTime(12, 0).atZone(zone)

    private fun reminding(
        times: String = "[540,1080]",
        frequency: HabitFrequency = HabitFrequency.DAILY,
        weekdays: Set<Int>? = null,
        completions: List<HabitCompletionEntity> = emptyList(),
    ) = HabitWithCompletions(
        habit().copy(
            reminderTimes = times,
            frequency = frequency,
            targetWeekdays = weekdays?.let { HabitUtils.serializeWeekdays(it) },
        ),
        completions,
    )

    private fun at(day: LocalDate, hour: Int) = day.atTime(hour, 0).atZone(zone)

    @Test
    fun nextReminder_isTodaysNextTime() {
        assertEquals(at(LocalDate.now(), 18), HabitUtils.nextReminderAt(reminding(), noon))
    }

    @Test
    fun nextReminder_skipsTodayOnceDone() {
        val done = reminding(completions = listOf(completedOn(LocalDate.now())))
        assertEquals(at(LocalDate.now().plusDays(1), 9), HabitUtils.nextReminderAt(done, noon))
    }

    @Test
    fun nextReminder_movesToTomorrowAfterTheLastTime() {
        val evening = LocalDate.now().atTime(19, 0).atZone(zone)
        assertEquals(at(LocalDate.now().plusDays(1), 9), HabitUtils.nextReminderAt(reminding(), evening))
    }

    @Test
    fun nextReminder_weeklyHabitWaitsForItsWeekday() {
        val tomorrow = LocalDate.now().plusDays(1)
        val habit = reminding(frequency = HabitFrequency.WEEKLY_EXACT, weekdays = setOf(tomorrow.dayOfWeek.value))
        assertEquals(at(tomorrow, 9), HabitUtils.nextReminderAt(habit, noon))
    }

    @Test
    fun nextReminder_weeklyHabitWhoseTimesPassedTodayRingsNextWeek() {
        val today = LocalDate.now()
        val habit = reminding(
            times = "[540]",
            frequency = HabitFrequency.WEEKLY_EXACT,
            weekdays = setOf(today.dayOfWeek.value),
        )
        assertEquals(at(today.plusDays(7), 9), HabitUtils.nextReminderAt(habit, noon))
    }

    @Test
    fun nextReminder_isTheEarliestTime_whateverTheStoredOrder() {
        val unordered = reminding(times = "[1080,840]")
        assertEquals(at(LocalDate.now(), 14), HabitUtils.nextReminderAt(unordered, noon))
    }

    @Test
    fun nextReminder_timeInADstGap_doesNotJumpAheadOfALaterOne() {
        // Europe/Rome skips 02:00-03:00 on March 29 2026, so 02:30 becomes 03:30, after 03:00.
        val rome = ZoneId.of("Europe/Rome")
        val dstDay = LocalDate.of(2026, 3, 29)
        val night = dstDay.atTime(0, 30).atZone(rome)
        assertEquals(
            dstDay.atTime(3, 0).atZone(rome),
            HabitUtils.nextReminderAt(reminding(times = "[150,180]"), night),
        )
    }

    @Test
    fun nextReminder_isNullWithoutTimesOrDays() {
        assertEquals(null, HabitUtils.nextReminderAt(reminding(times = "[]"), noon))
        val noDays = reminding(frequency = HabitFrequency.WEEKLY_EXACT, weekdays = emptySet())
        assertEquals(null, HabitUtils.nextReminderAt(noDays, noon))
    }

    private fun stacked(id: String, stack: String, stackOrder: Int, sortOrder: Int) =
        habit(id).copy(stackName = stack, stackOrder = stackOrder, sortOrder = sortOrder)

    @Test
    fun stackNamesInOrder_usesEachStacksLowestMember() {
        val habits = listOf(
            stacked("a1", "Morning", 0, 7),
            stacked("b1", "Evening", 0, 3),
            stacked("a2", " Morning ", 1, 9),
            habit("solo").copy(sortOrder = 0),
        )
        assertEquals(listOf("Evening", "Morning"), HabitUtils.stackNamesInOrder(habits))
    }

    @Test
    fun renumberStacks_movesWholeStacksAndKeepsMembersTogether() {
        val habits = listOf(
            stacked("a2", "A", 1, 1),
            stacked("a1", "A", 0, 0),
            stacked("b1", "B", 0, 2),
        )
        val result = HabitUtils.renumberStacks(habits, listOf("B", "A"))
        assertEquals(listOf("b1", "a1", "a2"), result.map { it.id })
        assertEquals(listOf(0, 1, 2), result.map { it.sortOrder })
    }

    @Test
    fun renumberStacks_leavesStacksOutsideTheSectionInPlace() {
        val habits = listOf(
            stacked("a", "A", 0, 0),
            stacked("b", "B", 0, 1),
            stacked("c", "C", 0, 2),
        )
        val result = HabitUtils.renumberStacks(habits, listOf("C", "A"))
        assertEquals(listOf("c", "b", "a"), result.map { it.id })
    }

    @Test
    fun renumberStacks_ignoresStandaloneHabits() {
        val habits = listOf(stacked("a", "A", 0, 0), habit("solo").copy(sortOrder = 5))
        val result = HabitUtils.renumberStacks(habits, listOf("A"))
        assertEquals(listOf("a"), result.map { it.id })
    }

    private fun checkIns(date: LocalDate, vararg hours: Long) = hours.mapIndexed { i, h ->
        HabitCompletionEntity(
            id = i + 1L,
            habitId = "h1",
            completionDate = date.atStartOfDay(ZoneId.systemDefault()).plusHours(h).toInstant(),
        )
    }

    @Test
    fun timesPerDay_countsADayDoneOnlyAtTheTarget() {
        val day = LocalDate.of(2026, 11, 16)
        val twice = habit().copy(timesPerDay = 2)
        val once = HabitWithCompletions(twice, checkIns(day, 8))
        assertEquals(1, HabitUtils.countOn(once, day))
        assertFalse(HabitUtils.isCompletedOn(once, day))
        assertTrue(HabitUtils.isCompletedOn(HabitWithCompletions(twice, checkIns(day, 8, 20)), day))
        assertFalse(HabitUtils.isCompletedOn(HabitWithCompletions(twice, checkIns(day, 8, 20)), day.plusDays(1)))
    }

    @Test
    fun tap_addsBelowTheTargetAndRemovesTheLatestAtIt() {
        val day = LocalDate.of(2026, 11, 16)
        val thrice = habit().copy(timesPerDay = 3)
        assertEquals(null, HabitUtils.completionToRemoveOnTap(thrice, checkIns(day, 8, 13), day))
        val full = checkIns(day, 8, 20, 13)
        assertEquals(full[1], HabitUtils.completionToRemoveOnTap(thrice, full, day))
        // A once-a-day habit toggles as before.
        assertEquals(null, HabitUtils.completionToRemoveOnTap(habit(), emptyList(), day))
        val single = checkIns(day, 9)
        assertEquals(single[0], HabitUtils.completionToRemoveOnTap(habit(), single, day))
    }
}
