package io.github.benji377.timety.ui.viewmodel

import androidx.glance.appwidget.updateAll
import androidx.lifecycle.viewModelScope
import io.github.benji377.timety.data.model.habit.HabitCompletionEntity
import io.github.benji377.timety.data.model.habit.HabitEntity
import io.github.benji377.timety.data.model.habit.HabitWithCompletions
import io.github.benji377.timety.data.repository.HabitRepository
import io.github.benji377.timety.data.repository.UserRepository
import io.github.benji377.timety.util.stats.ExperienceEngine
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId

/**
 * NOTE (viewmodel logic added for the Habits port): [toggleCompletionToday],
 * [markCompletionOnDate] and [unmarkCompletionOnDate] now also award/revert
 * [ExperienceEngine.xpPerHabit] XP via [userRepository], mirroring
 * `HabitProvider.toggleCompletionToday`/`markCompletionOnDate`/`unmarkCompletionOnDate`
 * (which take an optional `UserProvider` to add/subtract XP). The constructor gained a
 * [UserRepository] dependency for this - `AppViewModelProvider` was updated accordingly
 * (same pattern already used by `TaskViewModel`).
 */
class HabitViewModel(
    private val application: android.app.Application,
    private val habitRepository: HabitRepository,
    private val userRepository: UserRepository
) : androidx.lifecycle.AndroidViewModel(application) {

    val allHabits: StateFlow<List<HabitEntity>> = habitRepository.allHabits
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val habitsWithCompletions: StateFlow<List<HabitWithCompletions>> =
        kotlinx.coroutines.flow.combine(
            habitRepository.allHabits,
            habitRepository.allCompletions
        ) { habits, completions ->
            val completionsByHabit = completions.groupBy { it.habitId }
            habits.map { habit ->
                HabitWithCompletions(
                    habit = habit,
                    completions = completionsByHabit[habit.id].orEmpty()
                )
            }
        }.flowOn(kotlinx.coroutines.Dispatchers.Default).stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    private fun updateWidgets() {
        viewModelScope.launch {
            io.github.benji377.timety.widget.HabitWidget().updateAll(application)
        }
    }

    fun addHabit(habit: HabitEntity) {
        viewModelScope.launch {
            habitRepository.insertHabit(habit)
            scheduleHabitReminder(habit)
            updateWidgets()
        }
    }

    fun updateHabit(habit: HabitEntity) {
        viewModelScope.launch {
            habitRepository.updateHabit(habit)
            scheduleHabitReminder(habit)
            updateWidgets()
        }
    }

    fun deleteHabit(habit: HabitEntity) {
        viewModelScope.launch {
            habitRepository.deleteHabit(habit)
            cancelHabitReminder(habit.id)
            updateWidgets()
        }
    }

    private suspend fun scheduleHabitReminder(habit: HabitEntity) {
        io.github.benji377.timety.services.ReminderScheduler.create(application)
            .scheduleHabitReminder(habit)
    }

    private suspend fun cancelHabitReminder(habitId: String) {
        io.github.benji377.timety.services.ReminderScheduler.create(application)
            .cancelHabitReminder(habitId)
    }

    fun logCompletion(habitId: String, date: Instant = Instant.now()) {
        viewModelScope.launch {
            habitRepository.insertCompletion(
                HabitCompletionEntity(habitId = habitId, completionDate = date)
            )
            updateWidgets()
        }
    }

    /** Toggles today's completion for [habitId] and awards/revokes XP. Mirrors `HabitProvider.toggleCompletionToday`. */
    fun toggleCompletionToday(habitId: String) {
        viewModelScope.launch {
            val habitWithCompletions =
                habitsWithCompletions.value.find { it.habit.id == habitId } ?: return@launch
            val today = LocalDate.now()
            val todayCompletion = habitWithCompletions.completions.find {
                LocalDateTime.ofInstant(it.completionDate, ZoneId.systemDefault())
                    .toLocalDate() == today
            }
            if (todayCompletion != null) {
                habitRepository.deleteCompletion(todayCompletion)
                userRepository.addXp(-ExperienceEngine.xpPerHabit)
            } else {
                habitRepository.insertCompletion(
                    HabitCompletionEntity(habitId = habitId, completionDate = Instant.now())
                )
                userRepository.addXp(ExperienceEngine.xpPerHabit)
            }
            updateWidgets()
        }
    }

    /** Marks [habitId] completed on [date] (a specific point in time), awarding XP. Mirrors `HabitProvider.markCompletionOnDate`. */
    fun markCompletionOnDate(habitId: String, date: Instant) {
        viewModelScope.launch {
            val habitWithCompletions =
                habitsWithCompletions.value.find { it.habit.id == habitId } ?: return@launch
            val targetDay = date.atZone(ZoneId.systemDefault()).toLocalDate()
            val alreadyCompleted = habitWithCompletions.completions.any {
                it.completionDate.atZone(ZoneId.systemDefault()).toLocalDate() == targetDay
            }
            if (!alreadyCompleted) {
                habitRepository.insertCompletion(
                    HabitCompletionEntity(habitId = habitId, completionDate = date)
                )
                userRepository.addXp(ExperienceEngine.xpPerHabit)
                updateWidgets()
            }
        }
    }

    /** Unmarks [habitId]'s completion on [date], revoking XP. Mirrors `HabitProvider.unmarkCompletionOnDate`. */
    fun unmarkCompletionOnDate(habitId: String, date: LocalDate) {
        viewModelScope.launch {
            val habitWithCompletions =
                habitsWithCompletions.value.find { it.habit.id == habitId } ?: return@launch
            val completion = habitWithCompletions.completions.find {
                it.completionDate.atZone(ZoneId.systemDefault()).toLocalDate() == date
            }
            if (completion != null) {
                habitRepository.deleteCompletion(completion)
                userRepository.addXp(-ExperienceEngine.xpPerHabit)
                updateWidgets()
            }
        }
    }
}
