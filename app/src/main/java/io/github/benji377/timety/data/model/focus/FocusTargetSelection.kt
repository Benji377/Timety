package io.github.benji377.timety.data.model.focus

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessibilityNew
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material.icons.filled.Wc
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import io.github.benji377.timety.R
import io.github.benji377.timety.ui.theme.ErrorColor
import io.github.benji377.timety.ui.theme.SuccessColor
import io.github.benji377.timety.ui.theme.TaskColor
import io.github.benji377.timety.ui.theme.WarningColor

/**
 * A (non-persisted) UI-facing selection of what a focus session is linked to: a tag, a task,
 * or a habit. Mirrors `FocusTargetSelection` in `focus_models.dart`.
 */
data class FocusTargetSelection(
    val type: FocusTargetType,
    val id: String,
    val label: String,
    val colorValue: Int,
) {
    val color: Color get() = Color(colorValue)

    companion object {
        fun tag(tag: FocusTagEntity) = FocusTargetSelection(
            type = FocusTargetType.TAG,
            id = tag.id,
            label = tag.name,
            colorValue = tag.colorValue,
        )

        fun task(id: String, label: String, colorValue: Int) = FocusTargetSelection(
            type = FocusTargetType.TASK,
            id = id,
            label = label,
            colorValue = colorValue,
        )

        fun habit(id: String, label: String, colorValue: Int) = FocusTargetSelection(
            type = FocusTargetType.HABIT,
            id = id,
            label = label,
            colorValue = colorValue,
        )
    }
}

/**
 * Distraction event types loggable during a focus session. Mirrors the `DistractionType` enum in
 * `focus_models.dart` (icon + color per type); [dbId] is what gets persisted in
 * [DistractionEntity.note] (mirrors Flutter's `DistractionType.dbId`).
 *
 * NOTE (viewmodel addition - see report): this enum did not exist in the Kotlin port yet. Added
 * here in `data/model/focus` per the porting instructions.
 */
enum class DistractionUIType(
    val entityType: DistractionType,
    val icon: ImageVector,
    val color: Color
) {
    DISTRACTED(DistractionType.DISTRACTED, Icons.Filled.WarningAmber, ErrorColor),
    HYDRATED(DistractionType.HYDRATED, Icons.Filled.WaterDrop, TaskColor),
    STRETCHED(DistractionType.STRETCHED, Icons.Filled.AccessibilityNew, WarningColor),
    SNACK(DistractionType.SNACK, Icons.Filled.Restaurant, SuccessColor),
    RESTROOM(DistractionType.RESTROOM, Icons.Filled.Wc, Color.Gray);

    @Composable
    fun getLocalizedName(): String = when (this) {
        DISTRACTED -> stringResource(R.string.distractionDistracted)
        HYDRATED -> stringResource(R.string.distractionHydrated)
        STRETCHED -> stringResource(R.string.distractionStretched)
        SNACK -> stringResource(R.string.distractionSnack)
        RESTROOM -> stringResource(R.string.distractionRestroom)
    }

    companion object {
        fun fromEntityType(type: DistractionType): DistractionUIType =
            entries.firstOrNull { it.entityType == type } ?: DISTRACTED
    }
}

/** ARGB int for [io.github.benji377.timety.ui.theme.FocusColor], used to seed the default tag. */
val FocusColorArgb: Int
    get() = io.github.benji377.timety.ui.theme.FocusColor.toArgb()
