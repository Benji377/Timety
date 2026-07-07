package io.github.benji377.timety.data.repository

import io.github.benji377.timety.data.local.dao.HabitDao
import io.github.benji377.timety.data.model.habit.HabitCompletionEntity
import io.github.benji377.timety.data.model.habit.HabitEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

class HabitRepository(
    private val habitDao: HabitDao
) {
    val allHabits: Flow<List<HabitEntity>> = habitDao.getAllHabits()
    val allCompletions: Flow<List<HabitCompletionEntity>> = habitDao.getAllCompletions()

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

}
