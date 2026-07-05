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
import io.github.benji377.timety.ui.navigation.AppRoute
import io.github.benji377.timety.ui.navigation.BottomNavItems

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

    val snackbarHostState = io.github.benji377.timety.ui.theme.LocalSnackbarHostState.current
    Scaffold(
        snackbarHost = { androidx.compose.material3.SnackbarHost(snackbarHostState) },
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
            composable(io.github.benji377.timety.ui.navigation.BottomNavItem.Home.route) {
                io.github.benji377.timety.ui.screens.HomeScreen(
                    onNavigateToFocus = { navController.navigate(io.github.benji377.timety.ui.navigation.BottomNavItem.Focus.route) },
                    onNavigateToTaskDetail = { taskId ->
                        navController.navigate(AppRoute.taskDetail(taskId))
                    },
                    onNavigateToHabitDetail = { habitId ->
                        navController.navigate(AppRoute.habitDetail(habitId))
                    },
                    onNavigateToCalendar = { navController.navigate(io.github.benji377.timety.ui.navigation.BottomNavItem.Calendar.route) },
                    onNavigateToStatistics = { navController.navigate(io.github.benji377.timety.ui.navigation.BottomNavItem.Statistics.route) }
                )
            }
            composable(io.github.benji377.timety.ui.navigation.BottomNavItem.Focus.route) {
                io.github.benji377.timety.ui.screens.focus.FocusScreen(
                    onNavigateToModes = { navController.navigate(AppRoute.FOCUS_MODES.route) }
                )
            }
            composable(AppRoute.FOCUS_MODES.route) {
                io.github.benji377.timety.ui.screens.focus.FocusModesScreen(
                    onNavigateBack = { navController.popBackStack() }
                )
            }
            composable(io.github.benji377.timety.ui.navigation.BottomNavItem.Tasks.route) {
                io.github.benji377.timety.ui.screens.task.TaskListScreen(
                    onNavigateToTaskDetail = { taskId ->
                        navController.navigate(AppRoute.taskDetail(taskId))
                    }
                )
            }
            composable(io.github.benji377.timety.ui.navigation.BottomNavItem.Habits.route) {
                io.github.benji377.timety.ui.screens.habit.HabitListScreen(
                    onNavigateToHabitDetail = { habitId ->
                        navController.navigate(AppRoute.habitDetail(habitId))
                    }
                )
            }
            composable(io.github.benji377.timety.ui.navigation.BottomNavItem.Profile.route) {
                io.github.benji377.timety.ui.screens.ProfileScreen(
                    onNavigateToSettings = { navController.navigate(AppRoute.SETTINGS.route) }
                )
            }
            composable(io.github.benji377.timety.ui.navigation.BottomNavItem.Calendar.route) {
                io.github.benji377.timety.ui.screens.CalendarScreen(
                    onNavigateToTask = { taskId -> navController.navigate(AppRoute.taskDetail(taskId)) },
                    onNavigateToHabit = { habitId ->
                        navController.navigate(
                            AppRoute.habitDetail(
                                habitId
                            )
                        )
                    },
                    onNavigateBack = { navController.popBackStack() }
                )
            }
            composable(io.github.benji377.timety.ui.navigation.BottomNavItem.Statistics.route) {
                io.github.benji377.timety.ui.screens.StatisticsScreen(
                    onNavigateBack = { navController.popBackStack() }
                )
            }
            composable(AppRoute.SETTINGS.route) {
                io.github.benji377.timety.ui.screens.SettingsScreen(
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToTags = { navController.navigate(AppRoute.FOCUS_TAGS.route) },
                    onNavigateToCategories = { navController.navigate(AppRoute.TASK_CATEGORIES.route) }
                )
            }
            composable(AppRoute.FOCUS_TAGS.route) {
                io.github.benji377.timety.ui.components.focus.FocusTagsWidget(
                    onNavigateBack = { navController.popBackStack() }
                )
            }
            composable(AppRoute.TASK_CATEGORIES.route) {
                io.github.benji377.timety.ui.screens.task.TaskCategoriesScreen(
                    onNavigateBack = { navController.popBackStack() }
                )
            }
            composable(AppRoute.TASK_DETAIL.route) {
                io.github.benji377.timety.ui.screens.task.TaskDetailScreen(
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
                io.github.benji377.timety.ui.screens.task.TaskDetailScreen(
                    taskId = taskId,
                    onNavigateBack = { navController.popBackStack() }
                )
            }
            composable(AppRoute.HABIT_DETAIL.route) {
                io.github.benji377.timety.ui.screens.habit.HabitDetailScreen(
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
                io.github.benji377.timety.ui.screens.habit.HabitDetailScreen(
                    habitId = habitId,
                    onNavigateBack = { navController.popBackStack() }
                )
            }
        }
    }
}
