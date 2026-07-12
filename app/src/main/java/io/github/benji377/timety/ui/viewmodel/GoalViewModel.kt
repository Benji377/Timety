package io.github.benji377.timety.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.benji377.timety.data.model.goal.GoalEntity
import io.github.benji377.timety.data.model.goal.GoalEntryEntity
import io.github.benji377.timety.data.model.goal.GoalWithEntries
import io.github.benji377.timety.data.repository.GoalRepository
import io.github.benji377.timety.data.repository.UserRepository
import io.github.benji377.timety.util.stats.ExperienceEngine
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.Instant

/** Exposes goals with their progress entries, and owns the completion/XP transitions for them. */
class GoalViewModel(
    private val goalRepository: GoalRepository,
    private val userRepository: UserRepository
) : ViewModel() {

    val goalsWithEntries: StateFlow<List<GoalWithEntries>> = goalRepository.allGoalsWithEntries
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun addGoal(goal: GoalEntity) {
        viewModelScope.launch {
            goalRepository.insertGoal(goal)
        }
    }

    /** Saves an edited goal; a changed target can flip completion, so the state is re-synced. */
    fun updateGoal(goal: GoalEntity) {
        viewModelScope.launch {
            goalRepository.updateGoal(goal)
            syncCompletionState(goal.id)
        }
    }

    fun deleteGoal(goal: GoalEntity) {
        viewModelScope.launch {
            goalRepository.deleteGoal(goal)
        }
    }

    /** Logs [value] (≥ 1) toward [goalId] at [timestamp]; backdated timestamps are allowed. */
    fun addEntry(goalId: String, value: Int, timestamp: Instant) {
        if (value < 1) return
        viewModelScope.launch {
            goalRepository.insertEntry(
                GoalEntryEntity(goalId = goalId, value = value, timestamp = timestamp)
            )
            syncCompletionState(goalId)
        }
    }

    fun deleteEntry(entry: GoalEntryEntity) {
        viewModelScope.launch {
            goalRepository.deleteEntry(entry)
            syncCompletionState(entry.goalId)
        }
    }

    /**
     * Single owner of both completion transitions - reaching the target sets [GoalEntity.completedAt]
     * and awards the XP, dropping back below clears it and reverts the XP - so the two can't diverge.
     */
    private suspend fun syncCompletionState(goalId: String) {
        val goalWithEntries = goalRepository.getGoalWithEntriesById(goalId) ?: return
        val goal = goalWithEntries.goal
        val targetReached = goalWithEntries.progress >= goal.targetValue
        when {
            targetReached && goal.completedAt == null -> {
                goalRepository.updateGoal(goal.copy(completedAt = Instant.now()))
                userRepository.addXp(ExperienceEngine.XP_PER_GOAL)
            }

            !targetReached && goal.completedAt != null -> {
                goalRepository.updateGoal(goal.copy(completedAt = null))
                userRepository.addXp(-ExperienceEngine.XP_PER_GOAL)
            }
        }
    }
}
