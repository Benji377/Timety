package io.github.benji377.timety.ui.components.mood

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.SentimentNeutral
import androidx.compose.material.icons.filled.SentimentVeryDissatisfied
import androidx.compose.material.icons.filled.SentimentVerySatisfied
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import io.github.benji377.timety.R
import io.github.benji377.timety.data.model.user.DayRating
import io.github.benji377.timety.ui.theme.ErrorColor
import io.github.benji377.timety.ui.theme.SuccessColor
import io.github.benji377.timety.ui.theme.WarningColor

/** The mood's name, shared by the profile and the evening checkup's actions. */
@get:StringRes
val DayRating.labelRes: Int
    get() = when (this) {
        DayRating.BAD -> R.string.dayRatingBad
        DayRating.OK -> R.string.dayRatingOk
        DayRating.GREAT -> R.string.dayRatingGreat
    }

val DayRating.icon: ImageVector
    get() = when (this) {
        DayRating.BAD -> Icons.Filled.SentimentVeryDissatisfied
        DayRating.OK -> Icons.Filled.SentimentNeutral
        DayRating.GREAT -> Icons.Filled.SentimentVerySatisfied
    }

val DayRating.color: Color
    get() = when (this) {
        DayRating.BAD -> ErrorColor
        DayRating.OK -> WarningColor
        DayRating.GREAT -> SuccessColor
    }

/** Best mood first: the order of the picker's buttons and the distribution bar's segments. */
val MoodsBestFirst: List<DayRating> = DayRating.entries.sortedByDescending { it.value }
