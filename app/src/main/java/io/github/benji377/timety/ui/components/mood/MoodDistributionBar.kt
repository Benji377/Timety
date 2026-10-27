package io.github.benji377.timety.ui.components.mood

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import io.github.benji377.timety.R
import io.github.benji377.timety.data.model.user.DayRating
import io.github.benji377.timety.ui.theme.AppTheme
import io.github.benji377.timety.util.stats.MoodStats

/** Every rated day's mood as one bar, best on the left, with each mood's share below it. */
@Composable
fun MoodDistributionBar(
    moods: Collection<DayRating>,
    modifier: Modifier = Modifier,
) {
    val percentages = remember(moods) { MoodStats.percentages(moods) }
    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = stringResource(R.string.moodDistributionTitle),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
        )
        Spacer(Modifier.height(AppTheme.spaceMedium))
        if (percentages.isEmpty()) {
            Text(
                text = stringResource(R.string.moodDistributionEmpty),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            return@Column
        }

        val labels = MoodsBestFirst.associateWith { stringResource(it.labelRes) }
        val summary = MoodsBestFirst.joinToString { "${labels.getValue(it)} ${percentages.getValue(it)}%" }
        Column(modifier = Modifier.clearAndSetSemantics { contentDescription = summary }) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(14.dp)
                    .clip(AppTheme.brPill),
                horizontalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                // Weights must be positive, so moods nobody picked get no segment.
                MoodsBestFirst.filter { percentages.getValue(it) > 0 }.forEach { mood ->
                    Box(
                        Modifier
                            .weight(percentages.getValue(mood).toFloat())
                            .fillMaxHeight()
                            .background(mood.color)
                    )
                }
            }
            Spacer(Modifier.height(AppTheme.spaceSmall))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                MoodsBestFirst.forEach { mood ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = mood.icon,
                            contentDescription = null,
                            tint = mood.color,
                            modifier = Modifier.size(AppTheme.iconSizeSmall),
                        )
                        Spacer(Modifier.width(AppTheme.spaceXSmall))
                        Text(
                            text = "${percentages.getValue(mood)}%",
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }
            }
        }
    }
}
