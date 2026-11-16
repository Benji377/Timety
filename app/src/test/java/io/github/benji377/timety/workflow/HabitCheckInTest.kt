package io.github.benji377.timety.workflow

import android.content.Context
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import io.github.benji377.timety.data.model.habit.HabitEntity
import io.github.benji377.timety.data.model.habit.HabitFrequency
import io.github.benji377.timety.data.repository.CheckInChange
import io.github.benji377.timety.testutil.TestAppContainer
import io.github.benji377.timety.testutil.value
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/** Check-in taps on a habit done several times a day, and undoing them. */
@RunWith(AndroidJUnit4::class)
class HabitCheckInTest {
    private val context: Context = InstrumentationRegistry.getInstrumentation().targetContext
    private lateinit var container: TestAppContainer

    private val habit = HabitEntity(
        id = "teeth",
        name = "Brush teeth",
        frequency = HabitFrequency.DAILY,
        timesPerDay = 2,
        createdAt = Instant.EPOCH,
        colorValue = 0,
    )

    // Mid-day, so the taps' few seconds apart never cross midnight.
    private val noon = LocalDate.now().atTime(12, 0).atZone(ZoneId.systemDefault()).toInstant()

    @Before
    fun setUp() {
        container = TestAppContainer(context)
        runBlocking { container.habitRepository.insertHabit(habit) }
    }

    @After
    fun tearDown() = container.close()

    private fun count() = container.habitRepository.getCompletionsForHabit(habit.id).value().size

    private fun tap(seconds: Long) = runBlocking {
        container.habitRepository.tapCheckIn(habit.id, noon.plusSeconds(seconds))!!
    }

    @Test
    fun tapsCountUpToTheTarget_thenRemoveTheLatest() {
        assertEquals(1, (tap(0) as CheckInChange.Added).countAfter)
        val second = tap(60)
        assertEquals(2, second.countAfter)
        val removed = tap(120)
        assertTrue(removed is CheckInChange.Removed)
        assertEquals(second.completion, removed.completion)
        assertEquals(1, count())
    }

    @Test
    fun undo_revertsExactlyTheTap() = runBlocking {
        tap(0)
        val added = tap(60)
        container.habitRepository.undoCheckIn(added)
        assertEquals(1, count())

        tap(60)
        val removed = tap(120)
        container.habitRepository.undoCheckIn(removed)
        assertEquals(2, count())
    }
}
