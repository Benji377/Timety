package io.github.benji377.timety.data.repository

import io.github.benji377.timety.data.local.dao.FocusDao
import io.github.benji377.timety.data.model.focus.DistractionEntity
import io.github.benji377.timety.data.model.focus.FocusModeEntity
import io.github.benji377.timety.data.model.focus.FocusSessionEntity
import io.github.benji377.timety.data.model.focus.FocusTagEntity
import io.github.benji377.timety.data.model.focus.SessionPhaseEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

class FocusRepository(
    private val focusDao: FocusDao
) {
    val allModes: Flow<List<FocusModeEntity>> = focusDao.getAllModes()
    val allSessions: Flow<List<FocusSessionEntity>> = focusDao.getAllSessions()
    val allTags: Flow<List<FocusTagEntity>> = focusDao.getAllTags()

    suspend fun insertModeWithPhases(mode: FocusModeEntity, phases: List<SessionPhaseEntity>) =
        withContext(Dispatchers.IO) {
            focusDao.insertMode(mode)
            focusDao.deletePhasesForMode(mode.id)
            focusDao.insertPhases(phases)
        }

    suspend fun deleteMode(mode: FocusModeEntity) = withContext(Dispatchers.IO) {
        focusDao.deleteMode(mode)
    }

    fun getPhasesForMode(modeId: String): Flow<List<SessionPhaseEntity>> =
        focusDao.getPhasesForMode(modeId)

    suspend fun insertSession(session: FocusSessionEntity) = withContext(Dispatchers.IO) {
        focusDao.insertSession(session)
    }

    suspend fun updateSession(session: FocusSessionEntity) = withContext(Dispatchers.IO) {
        focusDao.updateSession(session)
    }

    suspend fun deleteSession(session: FocusSessionEntity) = withContext(Dispatchers.IO) {
        focusDao.deleteSession(session)
    }

    fun getDistractionsForSession(sessionId: String): Flow<List<DistractionEntity>> =
        focusDao.getDistractionsForSession(sessionId)

    suspend fun insertDistraction(distraction: DistractionEntity) = withContext(Dispatchers.IO) {
        focusDao.insertDistraction(distraction)
    }

    suspend fun insertTag(tag: FocusTagEntity) = withContext(Dispatchers.IO) {
        focusDao.insertTag(tag)
    }

    suspend fun deleteTag(tag: FocusTagEntity) = withContext(Dispatchers.IO) {
        focusDao.deleteTag(tag)
    }
}
