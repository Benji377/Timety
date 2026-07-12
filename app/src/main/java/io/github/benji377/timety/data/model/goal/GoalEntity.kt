package io.github.benji377.timety.data.model.goal

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Relation
import java.time.Instant

/**
 * A quantified target with a deadline: "ride 30 km by September". Progress is logged as
 * [GoalEntryEntity] increments and is always derived from their sum, never cached here.
 */
@Entity(tableName = "goals")
data class GoalEntity(
    @PrimaryKey
    val id: String,
    val name: String,
    val description: String = "",
    val colorValue: Int,
    val iconCodePoint: Int? = null,
    val targetValue: Int,
    val unitLabel: String, // User-defined, e.g. "km", "books".
    val targetDate: Instant,
    val createdAt: Instant,
    val completedAt: Instant? = null // Null while the goal is still active.
)

/** A goal paired with all of its progress entries, for a Room `@Relation` query. */
data class GoalWithEntries(
    @Embedded
    val goal: GoalEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "goalId"
    )
    val entries: List<GoalEntryEntity>
) {
    /** Total progress toward [GoalEntity.targetValue]; always the sum, never a cached column. */
    val progress: Int get() = entries.sumOf { it.value }
}
