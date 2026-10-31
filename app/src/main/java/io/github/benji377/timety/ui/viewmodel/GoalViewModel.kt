package io.github.benji377.timety.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.benji377.timety.data.model.goal.GoalEntity
import io.github.benji377.timety.data.model.goal.GoalEntryEntity
import io.github.benji377.timety.data.model.goal.GoalWithEntries
import io.github.benji377.timety.data.model.tag.TaggedItemType
import io.github.benji377.timety.data.repository.GoalRepository
import io.github.benji377.timety.data.repository.TagRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.time.Instant

/** Exposes goals with their progress entries, and owns their completion transitions. */
class GoalViewModel(
    private val goalRepository: GoalRepository,
    private val tagRepository: TagRepository
) : ViewModel() {
    val goalsWithEntries: StateFlow<List<GoalWithEntries>> = goalRepository.allGoalsWithEntries
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun addGoal(goal: GoalEntity, tagIds: Set<String> = emptySet()) {
        launchToCompletion {
            goalRepository.insertGoal(goal)
            tagRepository.setItemTags(TaggedItemType.GOAL, goal.id, tagIds)
        }
    }

    /**
     * Saves an edited goal; a changed target can flip completion, so the state is re-synced. Null
     * [tagIds] leaves its tags as they are.
     */
    fun updateGoal(goal: GoalEntity, tagIds: Set<String>? = null) {
        viewModelScope.launch {
            goalRepository.updateGoal(goal)
            tagIds?.let { tagRepository.setItemTags(TaggedItemType.GOAL, goal.id, it) }
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

    // Serializes the read-check-write below so overlapping syncs see each other's completedAt.
    private val syncMutex = Mutex()

    /**
     * Single owner of [GoalEntity.completedAt]: reaching the target sets it, dropping back below
     * clears it.
     */
    private suspend fun syncCompletionState(goalId: String) = syncMutex.withLock {
        val goalWithEntries = goalRepository.getGoalWithEntriesById(goalId) ?: return@withLock
        val goal = goalWithEntries.goal
        val targetReached = goalWithEntries.progress >= goal.targetValue
        when {
            targetReached && goal.completedAt == null -> {
                goalRepository.updateGoal(goal.copy(completedAt = Instant.now()))
            }

            !targetReached && goal.completedAt != null -> {
                goalRepository.updateGoal(goal.copy(completedAt = null))
            }
        }
    }
}
