package io.github.benji377.timety.workflow

import android.app.Application
import android.content.Context
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import io.github.benji377.timety.data.model.habit.HabitEntity
import io.github.benji377.timety.data.model.habit.HabitFrequency
import io.github.benji377.timety.testutil.TestAppContainer
import io.github.benji377.timety.testutil.TestViewModels
import io.github.benji377.timety.testutil.awaitTrue
import io.github.benji377.timety.testutil.get
import io.github.benji377.timety.testutil.settle
import io.github.benji377.timety.testutil.value
import io.github.benji377.timety.ui.viewmodel.HabitViewModel
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.time.Instant
import java.time.temporal.ChronoUnit

/** Habits are done on the day itself: a completion dated in the future is ignored. */
@RunWith(AndroidJUnit4::class)
class HabitFutureCompletionTest {
    private val context: Context = InstrumentationRegistry.getInstrumentation().targetContext
    private lateinit var container: TestAppContainer
    private lateinit var viewModels: TestViewModels

    private val habit = HabitEntity(
        id = "habit-future",
        name = "Read",
        frequency = HabitFrequency.DAILY,
        createdAt = Instant.now().minus(7, ChronoUnit.DAYS),
        colorValue = 0,
    )

    @Before
    fun setUp() {
        container = TestAppContainer(context)
        viewModels = TestViewModels(context.applicationContext as Application, container)
        runBlocking { container.habitRepository.insertHabit(habit) }
    }

    @After
    fun tearDown() {
        viewModels.clear()
        container.close()
    }

    private fun completionCount() = container.habitRepository.getCompletionsForHabit(habit.id).value().size

    @Test
    fun aFutureCompletionIsIgnored_butTodaysIsLogged() {
        val viewModel = viewModels.get<HabitViewModel>()
        viewModel.markCompletionOnDate(habit.id, Instant.now().plus(2, ChronoUnit.DAYS))
        settle()
        assertTrue("tomorrow's check is ignored", completionCount() == 0)

        viewModel.markCompletionOnDate(habit.id, Instant.now())
        assertTrue("today's check is logged", awaitTrue { completionCount() == 1 })
    }
}
