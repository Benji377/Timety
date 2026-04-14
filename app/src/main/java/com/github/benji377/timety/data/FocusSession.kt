package com.github.benji377.timety.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

@Entity(
    tableName = "focus_session",
    foreignKeys = [
        ForeignKey(
            entity = Category::class,
            parentColumns = ["id"],
            childColumns = ["categoryId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = Task::class,
            parentColumns = ["id"],
            childColumns = ["taskId"],
            onDelete = ForeignKey.SET_NULL
        )
    ]
)
data class FocusSession(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    @ColumnInfo(index = true) val categoryId: Int,
    @ColumnInfo(index = true) val taskId: Int? = null,
    val startTime: Long,
    val endTime: Long,
    val duration: Long,
    val rating: FocusRating? = null,
    val note: String? = null
)

enum class FocusRating {
    GREAT, OKAY, DISTRACTED
}
