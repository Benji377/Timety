package io.github.benji377.timety.ui.viewmodel

import androidx.glance.appwidget.updateAll
import androidx.lifecycle.viewModelScope
import io.github.benji377.timety.data.model.habit.HabitCompletionEntity
import io.github.benji377.timety.data.model.habit.HabitEntity
import io.github.benji377.timety.data.model.habit.HabitWithCompletions
import io.github.benji377.timety.data.repository.HabitRepository
import io.github.benji377.timety.data.repository.UserRepository
import io.github.benji377.timety.services.ReminderScheduler
import io.github.benji377.timety.util.stats.ExperienceEngine
import io.github.benji377.timety.widget.HabitWidget
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId


/** Exposes habits and their completions, and applies XP/reminder/widget side effects for changes to them. */
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
            HabitWidget().updateAll(application)
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
        ReminderScheduler.create(application)
            .scheduleHabitReminder(habit)
    }

    private suspend fun cancelHabitReminder(habitId: String) {
        ReminderScheduler.create(application)
            .cancelHabitReminder(habitId)
    }


    /**
     * Commits a drag-reorder of the standalone habits visible in one list section.
     * [newSectionOrder] is only that section's post-drag order; habits outside it (in other
     * sections, or stacked) keep their exact relative position untouched.
     */
    fun commitStandaloneReorder(newSectionOrder: List<HabitEntity>) {
        viewModelScope.launch {
            val allStandalone = habitsWithCompletions.value.map { it.habit }
                .filter { it.stackName.isNullOrBlank() }
                .sortedBy { it.sortOrder }
            val renumbered = spliceOrder(allStandalone, HabitEntity::id, newSectionOrder)
                .mapIndexed { index, habit -> habit.copy(sortOrder = index) }
            habitRepository.updateHabits(renumbered)
            updateWidgets()
        }
    }

    /**
     * Commits a drag-reorder of [stackName]'s habits visible in one list section. A stack's
     * members can be split across sections (e.g. one already done today), so this splices the
     * new order into the full stack, not just the visible subset.
     */
    fun commitStackReorder(stackName: String, newSectionOrder: List<HabitEntity>) {
        viewModelScope.launch {
            val fullStack = habitsWithCompletions.value.map { it.habit }
                .filter { it.stackName?.trim() == stackName }
                .sortedBy { it.stackOrder ?: Int.MAX_VALUE }
            val renumbered = spliceOrder(fullStack, HabitEntity::id, newSectionOrder)
                .mapIndexed { index, habit -> habit.copy(stackOrder = index) }
            habitRepository.updateHabits(renumbered)
            updateWidgets()
        }
    }


    // Serializes the read-check-write completion updates below: two quick taps otherwise both
    // read "not completed" and insert a duplicate completion plus double XP.
    private val completionMutex = Mutex()

    /** Fresh-from-DB completions for [habitId]; [habitsWithCompletions] may not have emitted yet. */
    private suspend fun completionsOf(habitId: String): List<HabitCompletionEntity>? {
        habitRepository.getHabitById(habitId) ?: return null
        return habitRepository.getCompletionsForHabit(habitId).first()
    }

    /** Toggles today's completion for [habitId] and awards/reverts XP accordingly. */
    fun toggleCompletionToday(habitId: String) {
        viewModelScope.launch {
            completionMutex.withLock {
                val completions = completionsOf(habitId) ?: return@launch
                val today = LocalDate.now()
                val todayCompletion = completions.find {
                    it.completionDate.atZone(ZoneId.systemDefault()).toLocalDate() == today
                }
                if (todayCompletion != null) {
                    habitRepository.deleteCompletion(todayCompletion)
                    userRepository.addXp(-ExperienceEngine.XP_PER_HABIT)
                } else {
                    habitRepository.insertCompletion(
                        HabitCompletionEntity(habitId = habitId, completionDate = Instant.now())
                    )
                    userRepository.addXp(ExperienceEngine.XP_PER_HABIT)
                }
            }
            updateWidgets()
        }
    }


    /** Marks [habitId] complete on [date] and awards XP, unless it is already marked complete that day. */
    fun markCompletionOnDate(habitId: String, date: Instant) {
        viewModelScope.launch {
            completionMutex.withLock {
                val completions = completionsOf(habitId) ?: return@launch
                val targetDay = date.atZone(ZoneId.systemDefault()).toLocalDate()
                val alreadyCompleted = completions.any {
                    it.completionDate.atZone(ZoneId.systemDefault()).toLocalDate() == targetDay
                }
                if (alreadyCompleted) return@launch
                habitRepository.insertCompletion(
                    HabitCompletionEntity(habitId = habitId, completionDate = date)
                )
                userRepository.addXp(ExperienceEngine.XP_PER_HABIT)
            }
            updateWidgets()
        }
    }


    /** Removes [habitId]'s completion on [date], if any, and reverts the XP it had granted. */
    fun unmarkCompletionOnDate(habitId: String, date: LocalDate) {
        viewModelScope.launch {
            completionMutex.withLock {
                val completions = completionsOf(habitId) ?: return@launch
                val completion = completions.find {
                    it.completionDate.atZone(ZoneId.systemDefault()).toLocalDate() == date
                } ?: return@launch
                habitRepository.deleteCompletion(completion)
                userRepository.addXp(-ExperienceEngine.XP_PER_HABIT)
            }
            updateWidgets()
        }
    }
}


/**
 * Replaces the subsequence of [fullOrdered] identified by [key] matching an item in
 * [newVisibleOrder] with [newVisibleOrder]'s order, leaving every other item's position
 * untouched. Used to commit a drag-reorder of a visible subset back into its full domain list.
 */
private fun <T> spliceOrder(fullOrdered: List<T>, key: (T) -> String, newVisibleOrder: List<T>): List<T> {
    val visibleIds = newVisibleOrder.map(key).toSet()
    val iterator = newVisibleOrder.iterator()
    return fullOrdered.map { if (key(it) in visibleIds) iterator.next() else it }
}
