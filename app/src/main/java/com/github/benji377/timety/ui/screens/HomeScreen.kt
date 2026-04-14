package com.github.benji377.timety.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.github.benji377.timety.ui.components.RadialGraph
import com.github.benji377.timety.ui.components.TaskCard
import com.github.benji377.timety.viewmodel.HomeViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    onSettingsClick: () -> Unit,
    onTaskClick: (Int) -> Unit,
    onFocusClick: () -> Unit,
    onAddTaskClick: () -> Unit
) {
    val user by viewModel.user.collectAsState()
    val tasks by viewModel.todayTasks.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Good morning, ${user?.name ?: "Hero"}") },
                actions = {
                    IconButton(onClick = onSettingsClick) {
                        Icon(Icons.Default.Settings, contentDescription = "Settings")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            val focusTarget = user?.dailyFocusTarget ?: (2 * 60 * 60 * 1000L) // Default 2h
            // In a real app, we'd calculate focusTimeToday from FocusSessions
            val focusTimeToday = 0L 
            val progress = if (focusTarget > 0) focusTimeToday.toFloat() / focusTarget else 0f

            Box(modifier = Modifier.clickable { onFocusClick() }) {
                RadialGraph(
                    progress = progress,
                    text = "${focusTimeToday / 60000} / ${focusTarget / 60000} min",
                    modifier = Modifier.padding(vertical = 24.dp)
                )
            }

            Text(
                text = "Today's Tasks",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
            )

            if (tasks.isEmpty()) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .clickable { onAddTaskClick() },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "No tasks for today. Tap to add one!",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            } else {
                LazyColumn(modifier = Modifier.weight(1f)) {
                    items(tasks) { task ->
                        TaskCard(
                            task = task,
                            onCheckedChange = { viewModel.toggleTaskStatus(task) },
                            onClick = { onTaskClick(task.id) }
                        )
                    }
                }
            }
        }
    }
}
