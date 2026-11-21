package io.github.benji377.timety.data.repository

import io.github.benji377.timety.data.local.dao.HabitDao
import io.github.benji377.timety.data.model.habit.HabitCompletionEntity
import io.github.benji377.timety.data.model.habit.HabitEntity
import io.github.benji377.timety.data.model.habit.HabitWithCompletions
import io.github.benji377.timety.util.habit.HabitUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.time.Instant
import java.time.ZoneId

/** What one check-in tap changed: the row it added or removed, and the day's count after it. */
sealed interface CheckInChange {
    val completion: HabitCompletionEntity
    val countAfter: Int

    data class Added(override val completion: HabitCompletionEntity, override val countAfter: Int) : CheckInChange
    data class Removed(override val completion: HabitCompletionEntity, override val countAfter: Int) : CheckInChange
}

/** Repository for habits and their completions, wrapping [HabitDao] with IO dispatching. */
class HabitRepository(
    private val habitDao: HabitDao
) {
    val allHabits: Flow<List<HabitEntity>> = habitDao.getAllHabits()
    val allCompletions: Flow<List<HabitCompletionEntity>> = habitDao.getAllCompletions()

    // Serializes every read-check-write on completions ([tapCheckIn], backfill, focus logging).
    // This repository is a process-wide singleton (see AppContainer), so the lock also covers
    // app-vs-widget taps; two unsynchronized taps could both read the same count and overshoot.
    val completionMutex = Mutex()

    suspend fun getHabitById(id: String): HabitEntity? = withContext(Dispatchers.IO) {
        habitDao.getHabitById(id)
    }

    suspend fun getCompletionsSince(cutoff: java.time.Instant): List<HabitCompletionEntity> =
        withContext(Dispatchers.IO) {
            habitDao.getCompletionsSince(cutoff)
        }

    suspend fun insertHabit(habit: HabitEntity) = withContext(Dispatchers.IO) {
        habitDao.insertHabit(habit)
    }

    suspend fun updateHabit(habit: HabitEntity) = withContext(Dispatchers.IO) {
        habitDao.updateHabit(habit)
    }

    suspend fun updateHabits(habits: List<HabitEntity>) = withContext(Dispatchers.IO) {
        habitDao.updateHabits(habits)
    }

    suspend fun deleteHabit(habit: HabitEntity) = withContext(Dispatchers.IO) {
        habitDao.deleteHabit(habit)
    }

    suspend fun clearAll() = withContext(Dispatchers.IO) {
        habitDao.clearAll()
    }

    // Completions
    fun getCompletionsForHabit(habitId: String): Flow<List<HabitCompletionEntity>> =
        habitDao.getCompletionsForHabit(habitId)

    suspend fun insertCompletion(completion: HabitCompletionEntity) = withContext(Dispatchers.IO) {
        habitDao.insertCompletion(completion)
    }

    suspend fun deleteCompletion(completion: HabitCompletionEntity) = withContext(Dispatchers.IO) {
        habitDao.deleteCompletion(completion)
    }

    /**
     * One check-in tap on [habitId] at [now]: below the day's target it adds one, at the target it
     * removes the latest. Null when the habit no longer exists.
     */
    suspend fun tapCheckIn(habitId: String, now: Instant = Instant.now()): CheckInChange? {
        val habit = getHabitById(habitId) ?: return null
        return completionMutex.withLock {
            val completions = getCompletionsForHabit(habitId).first()
            val today = now.atZone(ZoneId.systemDefault()).toLocalDate()
            val count = HabitUtils.countOn(HabitWithCompletions(habit, completions), today)
            val toRemove = HabitUtils.completionToRemoveOnTap(habit, completions, today)
            if (toRemove != null) {
                deleteCompletion(toRemove)
                CheckInChange.Removed(toRemove, count - 1)
            } else {
                val added = HabitCompletionEntity(habitId = habitId, completionDate = now)
                CheckInChange.Added(added.copy(id = insertCompletion(added)), count + 1)
            }
        }
    }

    /** Reverts [change]: deletes the row it added or puts back the one it removed. */
    suspend fun undoCheckIn(change: CheckInChange) = completionMutex.withLock {
        when (change) {
            is CheckInChange.Added -> deleteCompletion(change.completion)
            is CheckInChange.Removed -> insertCompletion(change.completion)
        }
    }

    suspend fun setPinnedAt(id: String, pinnedAt: Instant?) = withContext(Dispatchers.IO) {
        habitDao.setPinnedAt(id, pinnedAt)
    }
}
