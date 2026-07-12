package io.github.benji377.timety.data.model.goal

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.Instant

@Entity(
    tableName = "goal_entries",
    foreignKeys = [
        ForeignKey(
            entity = GoalEntity::class,
            parentColumns = ["id"],
            childColumns = ["goalId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("goalId")]
)
/** A single logged progress increment (always ≥ 1) toward a goal; corrections are per-entry deletes. */
data class GoalEntryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val goalId: String,
    val value: Int,
    val timestamp: Instant
)
