package io.github.benji377.timety.data.model.focus

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessibilityNew
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.TaskAlt
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material.icons.filled.Wc
import androidx.compose.material.icons.outlined.LocalOffer
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import io.github.benji377.timety.R
import io.github.benji377.timety.data.model.tag.TagEntity
import io.github.benji377.timety.ui.theme.ErrorColor
import io.github.benji377.timety.ui.theme.FocusColor
import io.github.benji377.timety.ui.theme.GoalColor
import io.github.benji377.timety.ui.theme.HabitColor
import io.github.benji377.timety.ui.theme.SuccessColor
import io.github.benji377.timety.ui.theme.TaskColor
import io.github.benji377.timety.ui.theme.WarningColor

/**
 * The task, recurring task, habit or goal a focus session is linked to, carrying a display label
 * and color alongside the underlying id. A session with no linked item has no selection.
 */
data class FocusTargetSelection(
    val type: FocusTargetType,
    val id: String,
    val label: String,
    val colorValue: Int,
) {
    val color: Color get() = Color(colorValue)

    companion object {
        /** A target shown in its type's [FocusTargetType.color]. */
        fun of(type: FocusTargetType, id: String, label: String) = FocusTargetSelection(
            type = type,
            id = id,
            label = label,
            colorValue = type.color.toArgb(),
        )

        /** The item a logged [session] is linked to, or null when it has none. */
        fun fromSession(session: FocusSessionEntity): FocusTargetSelection? {
            val id = session.targetId ?: return null
            if (session.targetType == FocusTargetType.TAG) return null
            return of(session.targetType, id, session.targetLabel.orEmpty())
        }
    }
}

/**
 * What a logged session is called: its linked item's name, otherwise its [tags]' names, or null
 * when it has neither.
 */
fun FocusSessionEntity.displayName(tags: List<TagEntity>): String? =
    targetLabel?.trim()?.takeIf { it.isNotEmpty() && targetType != FocusTargetType.TAG }
        ?: tags.joinToString(", ") { it.name }.ifEmpty { null }

/** Icon shown next to a focus target of this type. */
val FocusTargetType.icon: ImageVector
    get() = when (this) {
        FocusTargetType.TAG -> Icons.Outlined.LocalOffer
        FocusTargetType.TASK -> Icons.Filled.TaskAlt
        FocusTargetType.RECURRING_TASK -> Icons.Filled.Repeat
        FocusTargetType.HABIT -> Icons.Filled.Alarm
        FocusTargetType.GOAL -> Icons.Filled.Flag
    }

/** Accent color of this target type; [FocusTargetType.TAG] means no linked item. */
val FocusTargetType.color: Color
    get() = when (this) {
        FocusTargetType.TAG -> FocusColor
        FocusTargetType.TASK, FocusTargetType.RECURRING_TASK -> TaskColor
        FocusTargetType.HABIT -> HabitColor
        FocusTargetType.GOAL -> GoalColor
    }

/** UI presentation (icon, color, localized label) for each [DistractionType]. */
enum class DistractionUIType(
    val entityType: DistractionType,
    val icon: ImageVector,
    val color: Color,
    val labelRes: Int,
) {
    DISTRACTED(
        DistractionType.DISTRACTED,
        Icons.Filled.WarningAmber,
        ErrorColor,
        R.string.distractionDistracted
    ),
    HYDRATED(
        DistractionType.HYDRATED,
        Icons.Filled.WaterDrop,
        TaskColor,
        R.string.distractionHydrated
    ),
    STRETCHED(
        DistractionType.STRETCHED,
        Icons.Filled.AccessibilityNew,
        WarningColor,
        R.string.distractionStretched
    ),
    SNACK(DistractionType.SNACK, Icons.Filled.Restaurant, SuccessColor, R.string.distractionSnack),
    RESTROOM(DistractionType.RESTROOM, Icons.Filled.Wc, Color.Gray, R.string.distractionRestroom);

    @Composable
    @ReadOnlyComposable
    fun getLocalizedName(): String = stringResource(labelRes)

    companion object {
        fun fromEntityType(type: DistractionType): DistractionUIType =
            entries.firstOrNull { it.entityType == type } ?: DISTRACTED
    }
}

