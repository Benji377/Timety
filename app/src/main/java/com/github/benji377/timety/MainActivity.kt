package com.github.benji377.timety

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.github.benji377.timety.ui.Screen
import com.github.benji377.timety.ui.screens.AddFocusModeScreen
import com.github.benji377.timety.ui.screens.AddTaskScreen
import com.github.benji377.timety.ui.screens.CalendarScreen
import com.github.benji377.timety.ui.screens.DailyStatsScreen
import com.github.benji377.timety.ui.screens.FocusModesScreen
import com.github.benji377.timety.ui.screens.FocusScreen
import com.github.benji377.timety.ui.screens.HomeScreen
import com.github.benji377.timety.ui.screens.SettingsScreen
import com.github.benji377.timety.ui.screens.TaskDetailScreen
import com.github.benji377.timety.ui.screens.TasksScreen
import com.github.benji377.timety.ui.screens.UserScreen
import com.github.benji377.timety.ui.theme.TimetyTheme
import com.github.benji377.timety.utils.NotificationHelper
import com.github.benji377.timety.viewmodel.CalendarViewModel
import com.github.benji377.timety.viewmodel.CalendarViewModelFactory
import com.github.benji377.timety.viewmodel.FocusViewModel
import com.github.benji377.timety.viewmodel.FocusViewModelFactory
import com.github.benji377.timety.viewmodel.HomeViewModel
import com.github.benji377.timety.viewmodel.HomeViewModelFactory
import com.github.benji377.timety.viewmodel.SettingsViewModel
import com.github.benji377.timety.viewmodel.SettingsViewModelFactory
import com.github.benji377.timety.viewmodel.StatsViewModel
import com.github.benji377.timety.viewmodel.StatsViewModelFactory
import com.github.benji377.timety.viewmodel.TasksViewModel
import com.github.benji377.timety.viewmodel.TasksViewModelFactory

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Initialize notification channels
        NotificationHelper.createNotificationChannels(this)

        setContent {
            val app = application as TimetyApplication
            val repository = app.repository
            val settingsViewModel: SettingsViewModel = viewModel(
                factory = SettingsViewModelFactory(repository)
            )
            val user by settingsViewModel.user.collectAsState()

            TimetyTheme(darkTheme = user?.isDarkMode ?: false) {
                val navController = rememberNavController()
                val items = listOf(
                    Screen.Home,
                    Screen.Focus,
                    Screen.Tasks,
                    Screen.Calendar,
                    Screen.User
                )

                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentDestination = navBackStackEntry?.destination
                val showBottomBar = items.any { screen ->
                    currentDestination?.route == screen.route
                }

                Scaffold(
                    bottomBar = {
                        if (showBottomBar) {
                            NavigationBar {
                                items.forEach { screen ->
                                    NavigationBarItem(
                                        icon = {
                                            screen.icon?.let {
                                                Icon(
                                                    it,
                                                    contentDescription = null
                                                )
                                            }
                                        },
                                        label = { Text(screen.title) },
                                        selected = currentDestination?.hierarchy?.any { it.route == screen.route } == true,
                                        onClick = {
                                            if (currentDestination?.route != screen.route) {
                                                navController.navigate(screen.route) {
                                                    popUpTo(navController.graph.findStartDestination().id) {
                                                        saveState = true
                                                    }
                                                    launchSingleTop = true
                                                    restoreState = true
                                                }
                                            }
                                        }
                                    )
                                }
                            }
                        }
                    }
                ) { innerPadding ->
                    NavHost(
                        navController = navController,
                        startDestination = Screen.Home.route,
                        modifier = Modifier.padding(innerPadding)
                    ) {
                        composable(Screen.Home.route) {
                            val homeViewModel: HomeViewModel = viewModel(
                                factory = HomeViewModelFactory(repository)
                            )
                            HomeScreen(
                                viewModel = homeViewModel,
                                onSettingsClick = { navController.navigate(Screen.Settings.route) },
                                onTaskClick = { taskId ->
                                    navController.navigate(
                                        Screen.TaskDetail.createRoute(
                                            taskId
                                        )
                                    )
                                },
                                onFocusClick = { navController.navigate(Screen.Focus.route) },
                                onAddTaskClick = { navController.navigate(Screen.Tasks.route) }
                            )
                        }
                        composable(Screen.Focus.route) {
                            val focusViewModel: FocusViewModel = viewModel(
                                factory = FocusViewModelFactory(repository, this@MainActivity)
                            )
                            FocusScreen(
                                viewModel = focusViewModel,
                                taskId = null,
                                onNavigateToStats = {
                                    navController.navigate(Screen.DailyStats.createRoute(System.currentTimeMillis()))
                                },
                                onNavigateToModes = {
                                    navController.navigate(Screen.FocusModes.route)
                                }
                            )
                        }
                        composable(
                            route = Screen.Focus.route + "?taskId={taskId}",
                            arguments = listOf(
                                navArgument("taskId") {
                                    type = NavType.StringType
                                    nullable = true
                                }
                            )
                        ) { backStackEntry ->
                            val taskId =
                                backStackEntry.arguments?.getString("taskId")?.toIntOrNull()
                            val focusViewModel: FocusViewModel = viewModel(
                                factory = FocusViewModelFactory(repository, this@MainActivity)
                            )
                            FocusScreen(
                                viewModel = focusViewModel,
                                taskId = taskId,
                                onNavigateToStats = {
                                    navController.navigate(Screen.DailyStats.createRoute(System.currentTimeMillis()))
                                },
                                onNavigateToModes = {
                                    navController.navigate(Screen.FocusModes.route)
                                }
                            )
                        }
                        composable(Screen.Tasks.route) {
                            val tasksViewModel: TasksViewModel = viewModel(
                                factory = TasksViewModelFactory(repository, this@MainActivity)
                            )
                            TasksScreen(
                                viewModel = tasksViewModel,
                                onTaskClick = { taskId ->
                                    navController.navigate(
                                        Screen.TaskDetail.createRoute(
                                            taskId
                                        )
                                    )
                                },
                                onAddTaskClick = { navController.navigate(Screen.AddTask.route) }
                            )
                        }
                        composable(Screen.AddTask.route) {
                            val tasksViewModel: TasksViewModel = viewModel(
                                factory = TasksViewModelFactory(repository, this@MainActivity)
                            )
                            AddTaskScreen(
                                viewModel = tasksViewModel,
                                onBack = { navController.popBackStack() }
                            )
                        }
                        composable(Screen.Calendar.route) {
                            val calendarViewModel: CalendarViewModel = viewModel(
                                factory = CalendarViewModelFactory(repository)
                            )
                            CalendarScreen(
                                viewModel = calendarViewModel,
                                onDayClick = { date: Long ->
                                    navController.navigate(
                                        Screen.DailyStats.createRoute(
                                            date
                                        )
                                    )
                                }
                            )
                        }
                        composable(Screen.User.route) {
                            val settingsViewModel: SettingsViewModel = viewModel(
                                factory = SettingsViewModelFactory(repository)
                            )
                            UserScreen(viewModel = settingsViewModel)
                        }
                        composable(Screen.FocusModes.route) {
                            val focusViewModel: FocusViewModel = viewModel(
                                factory = FocusViewModelFactory(repository, this@MainActivity)
                            )
                            FocusModesScreen(
                                viewModel = focusViewModel,
                                onBack = { navController.popBackStack() },
                                onAddMode = { navController.navigate(Screen.AddFocusMode.route) },
                                onEditMode = { modeId ->
                                    // navController.navigate(Screen.EditFocusMode.createRoute(modeId))
                                }
                            )
                        }
                        composable(Screen.AddFocusMode.route) {
                            val focusViewModel: FocusViewModel = viewModel(
                                factory = FocusViewModelFactory(repository, this@MainActivity)
                            )
                            AddFocusModeScreen(
                                viewModel = focusViewModel,
                                onBack = { navController.popBackStack() }
                            )
                        }
                        composable(
                            route = Screen.DailyStats.route,
                            arguments = listOf(
                                navArgument("date") { type = NavType.LongType }
                            )
                        ) { backStackEntry ->
                            val date = backStackEntry.arguments?.getLong("date") ?: 0L
                            val statsViewModel: StatsViewModel = viewModel(
                                factory = StatsViewModelFactory(repository)
                            )
                            DailyStatsScreen(
                                viewModel = statsViewModel,
                                initialDate = date,
                                onBack = { navController.popBackStack() }
                            )
                        }
                        composable(Screen.Settings.route) {
                            val settingsViewModel: SettingsViewModel = viewModel(
                                factory = SettingsViewModelFactory(repository)
                            )
                            SettingsScreen(
                                viewModel = settingsViewModel,
                                onBack = { navController.popBackStack() }
                            )
                        }
                        composable(Screen.TaskDetail.route) { backStackEntry ->
                            val taskId =
                                backStackEntry.arguments?.getString("taskId")?.toIntOrNull()
                            val tasksViewModel: TasksViewModel = viewModel(
                                factory = TasksViewModelFactory(repository, this@MainActivity)
                            )
                            if (taskId != null) {
                                TaskDetailScreen(
                                    taskId = taskId,
                                    viewModel = tasksViewModel,
                                    onBack = { navController.popBackStack() },
                                    onStartFocus = { id ->
                                        navController.navigate(Screen.Focus.route + "?taskId=$id")
                                    }
                                )
                            } else {
                                Box(
                                    modifier = Modifier.fillMaxSize(),
                                    contentAlignment = androidx.compose.ui.Alignment.Center
                                ) {
                                    Text("Invalid Task ID")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
