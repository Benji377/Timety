package com.github.benji377.timety.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.Color
import com.github.benji377.timety.data.Category
import com.github.benji377.timety.ui.components.RadialGraph
import com.github.benji377.timety.ui.components.XPBar
import com.github.benji377.timety.viewmodel.StatsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatsScreen(viewModel: StatsViewModel) {
    val user by viewModel.user.collectAsState()
    val sessions by viewModel.allSessions.collectAsState()
    val todayFocusTime by viewModel.todayFocusTime.collectAsState()
    val categories by viewModel.allCategories.collectAsState()
    val distribution by viewModel.categoryDistribution.collectAsState()

    Scaffold(
        topBar = { TopAppBar(title = { Text("Stats & Profile") }) }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(16.dp)
        ) {
            // Profile Header
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = user?.name ?: "Hero",
                                style = MaterialTheme.typography.headlineSmall
                            )
                            Text(
                                text = "Productivity Warrior",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.secondary
                            )
                        }
                        Icon(
                            imageVector = Icons.Default.LocalFireDepartment,
                            contentDescription = "Streak",
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(32.dp)
                        )
                        Text(
                            text = "${user?.currentStreak ?: 0}",
                            style = MaterialTheme.typography.titleLarge
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    XPBar(
                        currentXp = user?.xp ?: 0,
                        level = user?.level ?: 1
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(text = "Today's Progress", style = MaterialTheme.typography.titleLarge)
            Spacer(modifier = Modifier.height(16.dp))
            
            val target = user?.dailyFocusTarget ?: 1L
            val progress = (todayFocusTime.toFloat() / target.toFloat()).coerceIn(0f, 1f)
            val hours = todayFocusTime / 3600000
            val minutes = (todayFocusTime % 3600000) / 60000
            
            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                RadialGraph(
                    progress = progress,
                    text = "${hours}h ${minutes}m",
                    modifier = Modifier.size(150.dp)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            if (distribution.isNotEmpty()) {
                Text(text = "Category Distribution", style = MaterialTheme.typography.titleLarge)
                Spacer(modifier = Modifier.height(16.dp))
                
                distribution.forEach { (catId, duration) ->
                    val category = categories.find { it.id == catId }
                    val catHours = duration / 3600000
                    val catMinutes = (duration % 3600000) / 60000
                    
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(text = category?.name ?: "Unknown")
                        Text(text = "${catHours}h ${catMinutes}m")
                    }
                    LinearProgressIndicator(
                        progress = { duration.toFloat() / sessions.sumOf { it.duration }.toFloat() },
                        modifier = Modifier.fillMaxWidth(),
                        color = category?.let { Color(android.graphics.Color.parseColor(it.colorHex)) } ?: MaterialTheme.colorScheme.primary
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(text = "Recent Sessions", style = MaterialTheme.typography.titleLarge)
            
            LazyColumn(modifier = Modifier.weight(1f)) {
                items(sessions) { session ->
                    ListItem(
                        headlineContent = { Text("Focus Session") },
                        supportingContent = { Text("${session.duration / 60000} minutes") },
                        trailingContent = { Text(session.rating?.name ?: "") }
                    )
                }
            }
        }
    }
}
