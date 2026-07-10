package io.github.benji377.timety.ui.screens

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarDefaults
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import io.github.benji377.timety.ui.components.focus.FocusTagsWidget
import io.github.benji377.timety.ui.navigation.AppRoute
import io.github.benji377.timety.ui.navigation.BottomNavItem
import io.github.benji377.timety.ui.navigation.BottomNavItems
import io.github.benji377.timety.ui.screens.focus.FocusModesScreen
import io.github.benji377.timety.ui.screens.focus.FocusScreen
import io.github.benji377.timety.ui.screens.habit.HabitDetailScreen
import io.github.benji377.timety.ui.screens.habit.HabitListScreen
import io.github.benji377.timety.ui.screens.habit.QuickHabitsScreen
import io.github.benji377.timety.ui.screens.task.RecurringTaskDetailScreen
import io.github.benji377.timety.ui.screens.task.RecurringTasksScreen
import io.github.benji377.timety.ui.screens.task.TaskCategoriesScreen
import io.github.benji377.timety.ui.screens.task.TaskDetailScreen
import io.github.benji377.timety.ui.screens.task.TaskListScreen
import io.github.benji377.timety.ui.theme.LocalSnackbarHostState

/**
 * Root screen hosting the navigation graph and bottom navigation bar for the app's main sections.
 */
@Composable
fun MainScreen() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        val permissionLauncher = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.RequestPermission()
        ) { _ -> }
        LaunchedEffect(Unit) {
            permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val showBottomNav = currentRoute in BottomNavItems.map { it.route }

    val snackbarHostState = LocalSnackbarHostState.current
    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            if (showBottomNav) {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.background,
                    tonalElevation = NavigationBarDefaults.Elevation
                ) {
                    BottomNavItems.forEach { item ->
                        val isSelected = currentRoute == item.route
                        NavigationBarItem(
                            selected = isSelected,
                            onClick = {
                                navController.navigate(item.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = {
                                Icon(
                                    imageVector = if (isSelected) item.iconFilled else item.iconOutlined,
                                    contentDescription = stringResource(item.titleRes),
                                    tint = if (isSelected) item.activeColor else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            },
                            label = {
                                Text(
                                    text = stringResource(item.titleRes),
                                    color = if (isSelected) item.activeColor else MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                )
                            },
                            colors = NavigationBarItemDefaults.colors(
                                indicatorColor = item.activeColor.copy(alpha = 0.15f)
                            )
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = BottomNavItems[0].route,
            // consumeWindowInsets is required alongside padding: the screens' own TopAppBars
            // apply status-bar insets too, so without consumption the top inset is applied twice.
            modifier = Modifier
                .padding(innerPadding)
                .consumeWindowInsets(innerPadding)
        ) {
            composable(BottomNavItem.Home.route) {
                HomeScreen(
                    onNavigateToFocus = {
                        navController.navigate(BottomNavItem.Focus.route) {
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                    onNavigateToTaskDetail = { taskId ->
                        navController.navigate(AppRoute.taskDetail(taskId))
                    },
                    onNavigateToHabitDetail = { habitId ->
                        navController.navigate(AppRoute.habitDetail(habitId))
                    },
                    onNavigateToRecurringDetail = { taskId ->
                        navController.navigate(AppRoute.recurringTaskDetail(taskId))
                    },
                    onNavigateToCalendar = { navController.navigate(BottomNavItem.Calendar.route) },
                    onNavigateToStatistics = { navController.navigate(BottomNavItem.Statistics.route) }
                )
            }
            composable(BottomNavItem.Focus.route) {
                FocusScreen(
                    onNavigateToModes = { navController.navigate(AppRoute.FOCUS_MODES.route) },
                    onNavigateToSettings = { navController.navigate(AppRoute.SETTINGS.route) }
                )
            }
            composable(AppRoute.FOCUS_MODES.route) {
                FocusModesScreen(
                    onNavigateBack = { navController.popBackStack() }
                )
            }
            composable(BottomNavItem.Tasks.route) {
                TaskListScreen(
                    onNavigateToTaskDetail = { taskId ->
                        navController.navigate(AppRoute.taskDetail(taskId))
                    },
                    onNavigateToRecurring = { navController.navigate(AppRoute.RECURRING_TASKS.route) },
                    onNavigateToRecurringDetail = { taskId ->
                        navController.navigate(AppRoute.recurringTaskDetail(taskId))
                    }
                )
            }
            composable(BottomNavItem.Habits.route) {
                HabitListScreen(
                    onNavigateToHabitDetail = { habitId ->
                        navController.navigate(AppRoute.habitDetail(habitId))
                    },
                    onNavigateToQuickHabits = { navController.navigate(AppRoute.QUICK_HABITS.route) }
                )
            }
            composable(BottomNavItem.Profile.route) {
                ProfileScreen(
                    onNavigateToSettings = { navController.navigate(AppRoute.SETTINGS.route) }
                )
            }
            composable(BottomNavItem.Calendar.route) {
                CalendarScreen(
                    onNavigateToTask = { taskId -> navController.navigate(AppRoute.taskDetail(taskId)) },
                    onNavigateToHabit = { habitId ->
                        navController.navigate(
                            AppRoute.habitDetail(
                                habitId
                            )
                        )
                    },
                    onNavigateToRecurring = { taskId ->
                        navController.navigate(AppRoute.recurringTaskDetail(taskId))
                    },
                    onNavigateBack = { navController.popBackStack() }
                )
            }
            composable(BottomNavItem.Statistics.route) {
                StatisticsScreen(
                    onNavigateBack = { navController.popBackStack() }
                )
            }
            composable(AppRoute.SETTINGS.route) {
                SettingsScreen(
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToTags = { navController.navigate(AppRoute.FOCUS_TAGS.route) },
                    onNavigateToCategories = { navController.navigate(AppRoute.TASK_CATEGORIES.route) }
                )
            }
            composable(AppRoute.FOCUS_TAGS.route) {
                FocusTagsWidget(
                    onNavigateBack = { navController.popBackStack() }
                )
            }
            composable(AppRoute.TASK_CATEGORIES.route) {
                TaskCategoriesScreen(
                    onNavigateBack = { navController.popBackStack() }
                )
            }
            composable(AppRoute.QUICK_HABITS.route) {
                QuickHabitsScreen(
                    onNavigateBack = { navController.popBackStack() }
                )
            }
            composable(AppRoute.RECURRING_TASKS.route) {
                RecurringTasksScreen(
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToDetail = { taskId ->
                        navController.navigate(AppRoute.recurringTaskDetail(taskId))
                    }
                )
            }
            composable(AppRoute.RECURRING_TASK_DETAIL.route) {
                RecurringTaskDetailScreen(
                    recurringTaskId = null,
                    onNavigateBack = { navController.popBackStack() }
                )
            }
            composable(
                route = AppRoute.RECURRING_TASK_DETAIL_WITH_ID,
                arguments = listOf(androidx.navigation.navArgument(AppRoute.ARG_TASK_ID) {
                    type = androidx.navigation.NavType.StringType
                })
            ) { backStackEntry ->
                val taskId = backStackEntry.arguments?.getString(AppRoute.ARG_TASK_ID)
                RecurringTaskDetailScreen(
                    recurringTaskId = taskId,
                    onNavigateBack = { navController.popBackStack() }
                )
            }
            composable(AppRoute.TASK_DETAIL.route) {
                TaskDetailScreen(
                    taskId = null,
                    onNavigateBack = { navController.popBackStack() }
                )
            }
            composable(
                route = AppRoute.TASK_DETAIL_WITH_ID,
                arguments = listOf(androidx.navigation.navArgument(AppRoute.ARG_TASK_ID) {
                    type = androidx.navigation.NavType.StringType
                })
            ) { backStackEntry ->
                val taskId = backStackEntry.arguments?.getString(AppRoute.ARG_TASK_ID)
                TaskDetailScreen(
                    taskId = taskId,
                    onNavigateBack = { navController.popBackStack() }
                )
            }
            composable(AppRoute.HABIT_DETAIL.route) {
                HabitDetailScreen(
                    habitId = null,
                    onNavigateBack = { navController.popBackStack() }
                )
            }
            composable(
                route = AppRoute.HABIT_DETAIL_WITH_ID,
                arguments = listOf(androidx.navigation.navArgument(AppRoute.ARG_HABIT_ID) {
                    type = androidx.navigation.NavType.StringType
                })
            ) { backStackEntry ->
                val habitId = backStackEntry.arguments?.getString(AppRoute.ARG_HABIT_ID)
                HabitDetailScreen(
                    habitId = habitId,
                    onNavigateBack = { navController.popBackStack() }
                )
            }
        }
    }
}
