package io.github.benji377.timety.ui.viewmodel

import androidx.lifecycle.viewModelScope
import io.github.benji377.timety.data.model.habit.QuickHabitEntity
import io.github.benji377.timety.data.repository.QuickHabitRepository
import io.github.benji377.timety.services.ReminderScheduler
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** Exposes quick habits (interval reminders) and keeps their alarms in sync with edits. */
class QuickHabitViewModel(
    private val application: android.app.Application,
    private val quickHabitRepository: QuickHabitRepository,
) : androidx.lifecycle.AndroidViewModel(application) {

    val quickHabits: StateFlow<List<QuickHabitEntity>> = quickHabitRepository.allQuickHabits
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun addQuickHabit(quickHabit: QuickHabitEntity) {
        viewModelScope.launch {
            quickHabitRepository.insert(quickHabit)
            scheduleQuickHabit(quickHabit)
        }
    }

    fun updateQuickHabit(quickHabit: QuickHabitEntity) {
        viewModelScope.launch {
            quickHabitRepository.update(quickHabit)
            scheduleQuickHabit(quickHabit)
        }
    }

    /** Toggles the enabled flag, rescheduling or cancelling the alarm to match. */
    fun setEnabled(quickHabit: QuickHabitEntity, enabled: Boolean) {
        updateQuickHabit(quickHabit.copy(isEnabled = enabled))
    }

    fun deleteQuickHabit(quickHabit: QuickHabitEntity) {
        viewModelScope.launch {
            quickHabitRepository.delete(quickHabit)
            ReminderScheduler.create(application).cancelQuickHabit(quickHabit.id)
        }
    }

    private suspend fun scheduleQuickHabit(quickHabit: QuickHabitEntity) {
        ReminderScheduler.create(application).scheduleQuickHabit(quickHabit)
    }
}
