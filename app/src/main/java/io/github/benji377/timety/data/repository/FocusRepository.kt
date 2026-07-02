package io.github.benji377.timety.data.repository

import io.github.benji377.timety.data.local.dao.FocusDao
import io.github.benji377.timety.data.model.focus.*
import kotlinx.coroutines.flow.Flow
class FocusRepository(
    private val focusDao: FocusDao
) {
    val allModes: Flow<List<FocusModeEntity>> = focusDao.getAllModes()
    val allSessions: Flow<List<FocusSessionEntity>> = focusDao.getAllSessions()
    val allTags: Flow<List<FocusTagEntity>> = focusDao.getAllTags()

    suspend fun insertModeWithPhases(mode: FocusModeEntity, phases: List<SessionPhaseEntity>) {
        focusDao.insertMode(mode)
        focusDao.deletePhasesForMode(mode.id)
        focusDao.insertPhases(phases)
    }

    suspend fun deleteMode(mode: FocusModeEntity) {
        focusDao.deleteMode(mode)
    }

    fun getPhasesForMode(modeId: String): Flow<List<SessionPhaseEntity>> = focusDao.getPhasesForMode(modeId)

    suspend fun insertSession(session: FocusSessionEntity) {
        focusDao.insertSession(session)
    }

    suspend fun updateSession(session: FocusSessionEntity) {
        focusDao.updateSession(session)
    }

    suspend fun deleteSession(session: FocusSessionEntity) {
        focusDao.deleteSession(session)
    }

    fun getDistractionsForSession(sessionId: String): Flow<List<DistractionEntity>> = focusDao.getDistractionsForSession(sessionId)

    suspend fun insertDistraction(distraction: DistractionEntity) {
        focusDao.insertDistraction(distraction)
    }

    suspend fun insertTag(tag: FocusTagEntity) {
        focusDao.insertTag(tag)
    }

    suspend fun deleteTag(tag: FocusTagEntity) {
        focusDao.deleteTag(tag)
    }
}
