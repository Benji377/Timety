package io.github.benji377.timety.ui.screens.main

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.compose.ui.res.stringResource
import io.github.benji377.timety.ui.navigation.BottomNavItems
import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts

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

    Scaffold(
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
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(io.github.benji377.timety.ui.navigation.BottomNavItem.Home.route) {
                io.github.benji377.timety.ui.screens.home.HomeScreen(
                    onNavigateToFocus = { navController.navigate(io.github.benji377.timety.ui.navigation.BottomNavItem.Focus.route) },
                    onNavigateToTaskDetail = { taskId -> 
                        if (taskId == null) navController.navigate("task_detail")
                        else navController.navigate("task_detail/$taskId")
                    },
                    onNavigateToHabitDetail = { habitId -> 
                        if (habitId == null) navController.navigate("habit_detail")
                        else navController.navigate("habit_detail/$habitId")
                    },
                    onNavigateToCalendar = { navController.navigate(io.github.benji377.timety.ui.navigation.BottomNavItem.Calendar.route) },
                    onNavigateToStatistics = { navController.navigate(io.github.benji377.timety.ui.navigation.BottomNavItem.Statistics.route) }
                )
            }
            composable(io.github.benji377.timety.ui.navigation.BottomNavItem.Focus.route) {
                io.github.benji377.timety.ui.screens.focus.FocusScreen(
                    onNavigateToModes = { navController.navigate("focus_modes") }
                )
            }
            composable("focus_modes") {
                io.github.benji377.timety.ui.screens.focus.FocusModesScreen(
                    onNavigateBack = { navController.popBackStack() }
                )
            }
            composable(io.github.benji377.timety.ui.navigation.BottomNavItem.Tasks.route) {
                io.github.benji377.timety.ui.screens.tasks.TaskListScreen(
                    onNavigateToTaskDetail = { taskId -> 
                        if (taskId == null) navController.navigate("task_detail")
                        else navController.navigate("task_detail/$taskId")
                    }
                )
            }
            composable(io.github.benji377.timety.ui.navigation.BottomNavItem.Habits.route) {
                io.github.benji377.timety.ui.screens.habits.HabitListScreen(
                    onNavigateToHabitDetail = { habitId -> 
                        if (habitId == null) navController.navigate("habit_detail")
                        else navController.navigate("habit_detail/$habitId")
                    }
                )
            }
            composable(io.github.benji377.timety.ui.navigation.BottomNavItem.Profile.route) {
                io.github.benji377.timety.ui.screens.profile.ProfileScreen(
                    onNavigateToSettings = { navController.navigate("settings") }
                )
            }
            composable(io.github.benji377.timety.ui.navigation.BottomNavItem.Calendar.route) {
                io.github.benji377.timety.ui.screens.calendar.CalendarScreen(
                    onNavigateToTask = { taskId -> navController.navigate("task_detail/$taskId") },
                    onNavigateBack = { navController.popBackStack() }
                )
            }
            composable(io.github.benji377.timety.ui.navigation.BottomNavItem.Statistics.route) {
                io.github.benji377.timety.ui.screens.statistics.StatisticsScreen(
                    onNavigateBack = { navController.popBackStack() }
                )
            }
            composable("focus_history") {
                io.github.benji377.timety.ui.screens.focus.FocusHistoryScreen(
                    onNavigateBack = { navController.popBackStack() }
                )
            }
            composable("settings") {
                io.github.benji377.timety.ui.screens.settings.SettingsScreen(
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToTags = { navController.navigate("focus_tags") },
                    onNavigateToCategories = { navController.navigate("task_categories") }
                )
            }
            composable("focus_tags") {
                io.github.benji377.timety.ui.components.focus.FocusTagsWidget(
                    onNavigateBack = { navController.popBackStack() }
                )
            }
            composable("task_categories") {
                io.github.benji377.timety.ui.screens.task.TaskCategoriesScreen(
                    onNavigateBack = { navController.popBackStack() }
                )
            }
            composable("task_detail") {
                io.github.benji377.timety.ui.screens.task.TaskDetailScreen(
                    taskId = null,
                    onNavigateBack = { navController.popBackStack() }
                )
            }
            composable(
                route = "task_detail/{taskId}",
                arguments = listOf(androidx.navigation.navArgument("taskId") { type = androidx.navigation.NavType.StringType })
            ) { backStackEntry ->
                val taskId = backStackEntry.arguments?.getString("taskId")
                io.github.benji377.timety.ui.screens.task.TaskDetailScreen(
                    taskId = taskId,
                    onNavigateBack = { navController.popBackStack() }
                )
            }
            composable("habit_detail") {
                io.github.benji377.timety.ui.screens.habit.HabitDetailScreen(
                    habitId = null,
                    onNavigateBack = { navController.popBackStack() }
                )
            }
            composable(
                route = "habit_detail/{habitId}",
                arguments = listOf(androidx.navigation.navArgument("habitId") { type = androidx.navigation.NavType.StringType })
            ) { backStackEntry ->
                val habitId = backStackEntry.arguments?.getString("habitId")
                io.github.benji377.timety.ui.screens.habit.HabitDetailScreen(
                    habitId = habitId,
                    onNavigateBack = { navController.popBackStack() }
                )
            }
        }
    }
}
