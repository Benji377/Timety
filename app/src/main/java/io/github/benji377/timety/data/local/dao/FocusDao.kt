package io.github.benji377.timety.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import io.github.benji377.timety.data.model.focus.*
import kotlinx.coroutines.flow.Flow

@Dao
interface FocusDao {
    // Modes
    @Query("SELECT * FROM focus_modes")
    fun getAllModes(): Flow<List<FocusModeEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMode(mode: FocusModeEntity)

    @Delete
    suspend fun deleteMode(mode: FocusModeEntity)

    // Phases
    @Query("SELECT * FROM session_phases WHERE modeId = :modeId ORDER BY orderIndex ASC")
    fun getPhasesForMode(modeId: String): Flow<List<SessionPhaseEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPhases(phases: List<SessionPhaseEntity>)

    @Query("DELETE FROM session_phases WHERE modeId = :modeId")
    suspend fun deletePhasesForMode(modeId: String)

    // Sessions
    @Query("SELECT * FROM focus_sessions ORDER BY startTime DESC")
    fun getAllSessions(): Flow<List<FocusSessionEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSession(session: FocusSessionEntity)

    @Update
    suspend fun updateSession(session: FocusSessionEntity)

    @Delete
    suspend fun deleteSession(session: FocusSessionEntity)

    // Distractions
    @Query("SELECT * FROM distractions WHERE sessionId = :sessionId")
    fun getDistractionsForSession(sessionId: String): Flow<List<DistractionEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDistraction(distraction: DistractionEntity)

    // Tags
    @Query("SELECT * FROM focus_tags")
    fun getAllTags(): Flow<List<FocusTagEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTag(tag: FocusTagEntity)

    @Delete
    suspend fun deleteTag(tag: FocusTagEntity)
}
