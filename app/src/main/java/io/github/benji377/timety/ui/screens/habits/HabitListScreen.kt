package io.github.benji377.timety.ui.screens.habits

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.benji377.timety.ui.components.habit.HabitListTile
import io.github.benji377.timety.ui.viewmodel.AppViewModelProvider
import io.github.benji377.timety.ui.viewmodel.HabitViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HabitListScreen(
    viewModel: HabitViewModel = viewModel(factory = AppViewModelProvider.Factory),
    onNavigateToHabitDetail: (String?) -> Unit
) {
    val habits by viewModel.allHabits.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Habits", fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { onNavigateToHabitDetail(null) },
                containerColor = MaterialTheme.colorScheme.tertiary,
                contentColor = MaterialTheme.colorScheme.onTertiary
            ) {
                Icon(Icons.Filled.Add, "Add Habit")
            }
        }
    ) { paddingValues ->
        if (habits.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = androidx.compose.ui.Alignment.Center
            ) {
                Text("No habits found. Tap + to add one.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentPadding = PaddingValues(bottom = 80.dp)
            ) {
                items(habits, key = { it.id }) { habit ->
                    // For the list view, we need to know if it's completed today.
                    // Assuming for now it's not completed. Real logic involves querying completions.
                    HabitListTile(
                        habit = habit,
                        isCompleted = false,
                        subtitleText = habit.frequency.name,
                        progressValue = null,
                        onToggleCompleted = { viewModel.logCompletion(habit.id) },
                        onTap = { onNavigateToHabitDetail(habit.id) },
                        onDelete = { viewModel.deleteHabit(habit) }
                    )
                }
            }
        }
    }
}
