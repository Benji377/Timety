package io.github.benji377.timety.data.repository

import io.github.benji377.timety.data.local.dao.GoalDao
import io.github.benji377.timety.data.model.goal.GoalEntity
import io.github.benji377.timety.data.model.goal.GoalEntryEntity
import io.github.benji377.timety.data.model.goal.GoalWithEntries
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

/** Repository for goals and their progress entries, wrapping [GoalDao] with IO dispatching. */
class GoalRepository(
    private val goalDao: GoalDao
) {
    val allGoalsWithEntries: Flow<List<GoalWithEntries>> = goalDao.getAllGoalsWithEntries()

    suspend fun getGoalWithEntriesById(id: String): GoalWithEntries? =
        withContext(Dispatchers.IO) {
            goalDao.getGoalWithEntriesById(id)
        }

    suspend fun insertGoal(goal: GoalEntity) = withContext(Dispatchers.IO) {
        goalDao.insertGoal(goal)
    }

    suspend fun updateGoal(goal: GoalEntity) = withContext(Dispatchers.IO) {
        goalDao.updateGoal(goal)
    }

    suspend fun deleteGoal(goal: GoalEntity) = withContext(Dispatchers.IO) {
        goalDao.deleteGoal(goal)
    }

    suspend fun clearAll() = withContext(Dispatchers.IO) {
        goalDao.clearAll()
    }

    // Entries
    suspend fun insertEntry(entry: GoalEntryEntity) = withContext(Dispatchers.IO) {
        goalDao.insertEntry(entry)
    }

    suspend fun deleteEntry(entry: GoalEntryEntity) = withContext(Dispatchers.IO) {
        goalDao.deleteEntry(entry)
    }
}
