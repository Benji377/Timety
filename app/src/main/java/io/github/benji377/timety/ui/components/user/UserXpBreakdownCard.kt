package io.github.benji377.timety.ui.components.user

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.TaskAlt
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Flag
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.benji377.timety.R
import io.github.benji377.timety.ui.components.common.NeoProgressBar
import io.github.benji377.timety.ui.theme.AppTheme
import io.github.benji377.timety.ui.theme.FocusColor
import io.github.benji377.timety.ui.theme.GoalColor
import io.github.benji377.timety.ui.theme.HabitColor
import io.github.benji377.timety.ui.theme.TaskColor
import io.github.benji377.timety.util.stats.ExperienceEngine


/**
 * Card showing the user's level, title, and XP progress toward the next level, with a collapsible
 * breakdown of how XP is earned per activity type.
 */
@Composable
fun UserXpBreakdownCard(
    currentLevel: Int,
    levelTitle: String,
    totalXp: Int,
    levelProgress: Double,
    modifier: Modifier = Modifier,
) {
    var showXpSources by remember { mutableStateOf(false) }
    val nextLevelXp = remember(currentLevel) { ExperienceEngine.getXpForLevel(currentLevel + 1) }
    val xpToNextLevel = nextLevelXp - totalXp
    val titleColor = ExperienceEngine.getTitleColor(currentLevel)

    val sources = remember(currentLevel) {
        listOf(
            XpSourceRowData(
                icon = Icons.Filled.TaskAlt,
                labelRes = R.string.globalLabelTasks,
                descriptionRes = R.string.xpSourceTaskDesc,
                value = ExperienceEngine.XP_PER_TASK,
                color = TaskColor,
            ),
            XpSourceRowData(
                icon = Icons.Outlined.FavoriteBorder,
                labelRes = R.string.globalLabelHabits,
                descriptionRes = R.string.xpSourceHabitDesc,
                value = ExperienceEngine.XP_PER_HABIT,
                color = HabitColor,
            ),
            XpSourceRowData(
                icon = Icons.Filled.Timer,
                labelRes = R.string.focusTitle,
                descriptionRes = R.string.xpSourceFocusDesc,
                value = ExperienceEngine.XP_PER_FOCUS_MINS,
                color = FocusColor,
            ),
            XpSourceRowData(
                icon = Icons.Outlined.Flag,
                labelRes = R.string.globalLabelGoals,
                descriptionRes = R.string.xpSourceGoalDesc,
                value = ExperienceEngine.XP_PER_GOAL,
                color = GoalColor,
            ),
        )
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = AppTheme.brNeo,
        border = BorderStroke(AppTheme.neoBorderWidth, MaterialTheme.colorScheme.outline),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        elevation = AppTheme.neoCardElevation,
    ) {
        Column(
            modifier = Modifier
                .background(
                    Brush.linearGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                            MaterialTheme.colorScheme.surface,
                        ),
                        start = Offset(0f, 0f),
                        end = Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY),
                    )
                )
                .padding(20.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(titleColor.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = ExperienceEngine.getTitleIcon(currentLevel),
                        contentDescription = null,
                        tint = titleColor,
                    )
                }
                Spacer(Modifier.width(14.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.xpBreakdownLevel, currentLevel, levelTitle),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = titleColor,
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = stringResource(R.string.xpBreakdownProgress, totalXp, xpToNextLevel),
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            Spacer(Modifier.height(18.dp))

            NeoProgressBar(
                progress = { levelProgress.toFloat().coerceIn(0f, 1f) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(12.dp),
                color = TaskColor,
                trackColor = MaterialTheme.colorScheme.surfaceVariant,
            )

            Spacer(Modifier.height(18.dp))

            val rotation by animateFloatAsState(
                if (showXpSources) 180f else 0f,
                label = "xpSourcesChevron"
            )
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                    ) { showXpSources = !showXpSources }
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(R.string.xpBreakdownHowToEarn),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.width(6.dp))
                Icon(
                    imageVector = Icons.Filled.KeyboardArrowDown,
                    contentDescription = null,
                    modifier = Modifier
                        .size(18.dp)
                        .rotate(rotation),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            AnimatedVisibility(
                visible = showXpSources,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically(),
            ) {
                Column(modifier = Modifier.padding(top = 10.dp)) {
                    sources.forEachIndexed { index, data ->
                        XpSourceRow(data)
                        if (index < sources.size - 1) {
                            HorizontalDivider(
                                modifier = Modifier.padding(horizontal = 16.dp),
                                thickness = 1.dp,
                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f),
                            )
                        }
                    }
                }
            }
        }
    }
}

private data class XpSourceRowData(
    val icon: ImageVector,
    val labelRes: Int,
    val descriptionRes: Int,
    val value: Int,
    val color: Color,
)

@Composable
private fun XpSourceRow(data: XpSourceRowData) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(CircleShape)
                .background(data.color.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = data.icon,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
                tint = data.color,
            )
        }
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = stringResource(data.labelRes),
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = stringResource(data.descriptionRes),
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Spacer(Modifier.width(12.dp))
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(999.dp))
                .background(data.color.copy(alpha = 0.1f))
                .padding(horizontal = 10.dp, vertical = 6.dp),
        ) {
            Text(
                text = stringResource(R.string.xpSourceValue, data.value),
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = data.color,
            )
        }
    }
}
