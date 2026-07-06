package io.github.benji377.timety.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import androidx.room.Upsert
import io.github.benji377.timety.data.model.focus.DistractionEntity
import io.github.benji377.timety.data.model.focus.FocusModeEntity
import io.github.benji377.timety.data.model.focus.FocusSessionEntity
import io.github.benji377.timety.data.model.focus.FocusTagEntity
import io.github.benji377.timety.data.model.focus.SessionPhaseEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface FocusDao {
    // Modes
    @Query("SELECT * FROM focus_modes")
    fun getAllModes(): Flow<List<FocusModeEntity>>

    // Upsert, not INSERT OR REPLACE: REPLACE deletes the conflicting row first, which trips the
    // RESTRICT foreign key of any focus_sessions logged against the mode being re-saved.
    @Upsert
    fun insertMode(mode: FocusModeEntity)

    @Delete
    fun deleteMode(mode: FocusModeEntity)

    // Phases
    @Query("SELECT * FROM session_phases WHERE modeId = :modeId ORDER BY orderIndex ASC")
    fun getPhasesForMode(modeId: String): Flow<List<SessionPhaseEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertPhases(phases: List<SessionPhaseEntity>)

    @Query("DELETE FROM session_phases WHERE modeId = :modeId")
    fun deletePhasesForMode(modeId: String): Int

    // Sessions
    @Query("SELECT * FROM focus_sessions ORDER BY startTime DESC")
    fun getAllSessions(): Flow<List<FocusSessionEntity>>

    @Query("SELECT COUNT(*) FROM focus_sessions WHERE modeId = :modeId")
    fun countSessionsForMode(modeId: String): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertSession(session: FocusSessionEntity)

    @Update
    fun updateSession(session: FocusSessionEntity)

    @Delete
    fun deleteSession(session: FocusSessionEntity)

    // Distractions
    @Query("SELECT * FROM distractions WHERE sessionId = :sessionId")
    fun getDistractionsForSession(sessionId: String): Flow<List<DistractionEntity>>

    @Query("SELECT * FROM distractions ORDER BY time DESC")
    fun getAllDistractions(): Flow<List<DistractionEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertDistraction(distraction: DistractionEntity)

    // Tags
    @Query("SELECT * FROM focus_tags")
    fun getAllTags(): Flow<List<FocusTagEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertTag(tag: FocusTagEntity)

    @Delete
    fun deleteTag(tag: FocusTagEntity)

    @Query("DELETE FROM focus_sessions")
    fun clearAllSessions()

    @Query("DELETE FROM focus_modes")
    fun clearAllModes()

    @Query("DELETE FROM focus_tags")
    fun clearAllTags()
}
