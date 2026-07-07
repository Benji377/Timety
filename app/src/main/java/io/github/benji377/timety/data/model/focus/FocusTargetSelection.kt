package io.github.benji377.timety.data.model.focus

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessibilityNew
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material.icons.filled.Wc
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import io.github.benji377.timety.R
import io.github.benji377.timety.ui.theme.ErrorColor
import io.github.benji377.timety.ui.theme.SuccessColor
import io.github.benji377.timety.ui.theme.TaskColor
import io.github.benji377.timety.ui.theme.WarningColor


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
    fun getLocalizedName(): String = stringResource(labelRes)

    companion object {
        fun fromEntityType(type: DistractionType): DistractionUIType =
            entries.firstOrNull { it.entityType == type } ?: DISTRACTED
    }
}


