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
import androidx.navigation.compose.*
import androidx.navigation.navArgument
import androidx.navigation.NavType
import com.github.benji377.timety.ui.Screen
import com.github.benji377.timety.ui.screens.*
import com.github.benji377.timety.ui.theme.TimetyTheme
import com.github.benji377.timety.utils.NotificationHelper
import com.github.benji377.timety.viewmodel.*

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
                    Screen.Stats
                )
                
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentDestination = navBackStackEntry?.destination
                val showBottomBar = items.any { screen -> 
                    currentDestination?.route?.startsWith(screen.route) == true
                }

                Scaffold(
                    bottomBar = {
                        if (showBottomBar) {
                            NavigationBar {
                                items.forEach { screen ->
                                    NavigationBarItem(
                                        icon = { screen.icon?.let { Icon(it, contentDescription = null) } },
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
                                onTaskClick = { taskId -> navController.navigate(Screen.TaskDetail.createRoute(taskId)) },
                                onFocusClick = { navController.navigate(Screen.Focus.route) },
                                onAddTaskClick = { navController.navigate(Screen.Tasks.route) }
                            )
                        }
                        composable(Screen.Focus.route) {
                            val focusViewModel: FocusViewModel = viewModel(
                                factory = FocusViewModelFactory(repository, this@MainActivity)
                            )
                            FocusScreen(focusViewModel, null)
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
                            val taskId = backStackEntry.arguments?.getString("taskId")?.toIntOrNull()
                            val focusViewModel: FocusViewModel = viewModel(
                                factory = FocusViewModelFactory(repository, this@MainActivity)
                            )
                            FocusScreen(focusViewModel, taskId)
                        }
                        composable(Screen.Tasks.route) {
                            val tasksViewModel: TasksViewModel = viewModel(
                                factory = TasksViewModelFactory(repository, this@MainActivity)
                            )
                            TasksScreen(
                                viewModel = tasksViewModel,
                                onTaskClick = { taskId -> navController.navigate(Screen.TaskDetail.createRoute(taskId)) },
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
                        composable(Screen.Stats.route) {
                            val statsViewModel: StatsViewModel = viewModel(
                                factory = StatsViewModelFactory(repository)
                            )
                            StatsScreen(statsViewModel)
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
                            val taskId = backStackEntry.arguments?.getString("taskId")?.toIntOrNull()
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
                                Box(modifier = Modifier.fillMaxSize(), contentAlignment = androidx.compose.ui.Alignment.Center) {
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
