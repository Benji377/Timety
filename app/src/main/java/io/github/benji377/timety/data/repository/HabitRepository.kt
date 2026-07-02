package io.github.benji377.timety.data.repository

import io.github.benji377.timety.data.local.dao.HabitDao
import io.github.benji377.timety.data.model.habit.HabitCompletionEntity
import io.github.benji377.timety.data.model.habit.HabitEntity
import kotlinx.coroutines.flow.Flow
class HabitRepository(
    private val habitDao: HabitDao
) {
    val allHabits: Flow<List<HabitEntity>> = habitDao.getAllHabits()

    suspend fun getHabitById(id: String): HabitEntity? = habitDao.getHabitById(id)

    suspend fun insertHabit(habit: HabitEntity) {
        habitDao.insertHabit(habit)
    }

    suspend fun updateHabit(habit: HabitEntity) {
        habitDao.updateHabit(habit)
    }

    suspend fun deleteHabit(habit: HabitEntity) {
        habitDao.deleteHabit(habit)
    }

    // Completions
    fun getCompletionsForHabit(habitId: String): Flow<List<HabitCompletionEntity>> = habitDao.getCompletionsForHabit(habitId)

    suspend fun insertCompletion(completion: HabitCompletionEntity) {
        habitDao.insertCompletion(completion)
    }

    suspend fun deleteCompletion(completion: HabitCompletionEntity) {
        habitDao.deleteCompletion(completion)
    }
    
    suspend fun deleteCompletionByDate(habitId: String, date: Long) {
        habitDao.deleteCompletionByDate(habitId, date)
    }
}
