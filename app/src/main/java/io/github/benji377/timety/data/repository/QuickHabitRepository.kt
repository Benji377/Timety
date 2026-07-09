package io.github.benji377.timety.data.repository

import io.github.benji377.timety.data.local.dao.QuickHabitDao
import io.github.benji377.timety.data.model.habit.QuickHabitEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

/** Repository for quick habits (interval reminders), wrapping [QuickHabitDao] with IO dispatching. */
class QuickHabitRepository(
    private val quickHabitDao: QuickHabitDao
) {
    val allQuickHabits: Flow<List<QuickHabitEntity>> = quickHabitDao.getAll()

    suspend fun insert(quickHabit: QuickHabitEntity) = withContext(Dispatchers.IO) {
        quickHabitDao.insert(quickHabit)
    }

    suspend fun update(quickHabit: QuickHabitEntity) = withContext(Dispatchers.IO) {
        quickHabitDao.update(quickHabit)
    }

    suspend fun delete(quickHabit: QuickHabitEntity) = withContext(Dispatchers.IO) {
        quickHabitDao.delete(quickHabit)
    }

    suspend fun clearAll() = withContext(Dispatchers.IO) {
        quickHabitDao.clearAll()
    }
}
