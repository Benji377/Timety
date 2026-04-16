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

@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    onSettingsClick: () -> Unit,
    onTaskClick: (Int) -> Unit,
    onFocusClick: () -> Unit,
    onAddTaskClick: () -> Unit
) {
    val greeting by viewModel.greeting.collectAsState()
    val user by viewModel.user.collectAsState()
    val tasks by viewModel.todayTasks.collectAsState()
    val todayFocusTime by viewModel.todayFocusTime.collectAsState()

    Scaffold(
        topBar = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                contentAlignment = Alignment.TopEnd
            ) {
                IconButton(onClick = onSettingsClick) {
                    Icon(Icons.Default.Settings, contentDescription = "Settings")
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = greeting,
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.padding(bottom = 24.dp)
            )

            val focusTarget = user?.dailyFocusTarget ?: (2 * 60 * 60 * 1000L) // Default 2h
            val progress = if (focusTarget > 0) todayFocusTime.toFloat() / focusTarget else 0f

            Box(modifier = Modifier.clickable { onFocusClick() }) {
                RadialGraph(
                    progress = progress,
                    text = "${todayFocusTime / 60000} / ${focusTarget / 60000} min",
                    modifier = Modifier.padding(vertical = 24.dp)
                )
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))

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
                        "No tasks for today, take a break or add some!",
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
