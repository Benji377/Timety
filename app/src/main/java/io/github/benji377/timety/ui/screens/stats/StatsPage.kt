package io.github.benji377.timety.ui.screens.stats

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import io.github.benji377.timety.R
import io.github.benji377.timety.ui.components.common.BackNavigationIcon
import io.github.benji377.timety.ui.components.common.NeoTopBar
import io.github.benji377.timety.ui.navigation.AppRoute

/**
 * A page opened from the statistics overview. The nav graph is built from [entries], so a new page
 * needs only an entry here and a branch in [StatsPageScreen].
 */
enum class StatsPage(private val routeSegment: String, @param:StringRes val titleRes: Int) {
    TIME("time", R.string.statsPageTime),
    TASKS("tasks", R.string.globalLabelTasks),
    HABITS("habits", R.string.globalLabelHabits),
    GOALS("goals", R.string.globalLabelGoals),
    PATTERNS("patterns", R.string.statsPagePatterns);

    /** Nav route of this page, nested under [AppRoute.STATISTICS]. */
    val route: String get() = "${AppRoute.STATISTICS.route}/$routeSegment"
}

/** The top bar and content of [page]. */
@Composable
fun StatsPageScreen(
    page: StatsPage,
    onNavigateBack: () -> Unit,
    onOpenHabit: (String) -> Unit,
    onOpenGoal: (String) -> Unit,
) {
    Scaffold(
        topBar = {
            NeoTopBar(
                title = stringResource(page.titleRes),
                navigationIcon = { BackNavigationIcon(onClick = onNavigateBack) },
            )
        },
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            when (page) {
                StatsPage.TIME -> StatsTimeScreen()
                StatsPage.TASKS -> StatsTasksScreen()
                StatsPage.HABITS -> StatsHabitsScreen(onOpenHabit)
                StatsPage.GOALS -> StatsGoalsScreen(onOpenGoal)
                StatsPage.PATTERNS -> StatsPatternsScreen()
            }
        }
    }
}
