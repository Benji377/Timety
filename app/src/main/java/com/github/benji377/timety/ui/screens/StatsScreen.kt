package com.github.benji377.timety.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
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
    val insights by viewModel.insights.collectAsState()
    val weeklyData by viewModel.weeklyFocusData.collectAsState()

    Scaffold(
        topBar = { TopAppBar(title = { Text("Stats & Profile") }) }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Profile Header
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = user?.name ?: "Hero",
                                    style = MaterialTheme.typography.headlineSmall
                                )
                                // Dynamic title based on streak
                                val title = when {
                                    user?.currentStreak ?: 0 >= 30 -> "🔥 Focus Master"
                                    user?.currentStreak ?: 0 >= 14 -> "⚡ Unstoppable"
                                    user?.currentStreak ?: 0 >= 7 -> "🌟 On Fire"
                                    user?.currentStreak ?: 0 >= 3 -> "💪 Building Momentum"
                                    (user?.currentStreak ?: 0) > 0 -> "🎯 Focused"
                                    else -> "🚀 Ready to Focus"
                                }
                                Text(
                                    text = title,
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

                        val currentXp = user?.xp ?: 0
                        val level = user?.level ?: 1
                        level * 100
                        val currentLevelXp = (level - 1) * 100
                        val xpInLevel = currentXp - currentLevelXp
                        val xpTarget = 100
                        val progress = (xpInLevel.toFloat() / xpTarget.toFloat()).coerceIn(0f, 1f)

                        XPBar(
                            progress = progress,
                            currentXp = xpInLevel,
                            targetXp = xpTarget,
                            level = level
                        )
                    }
                }
            }

            item {
                Text(text = "Today's Progress", style = MaterialTheme.typography.titleLarge)

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
            }

            // Weekly Chart
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("This Week's Focus", style = MaterialTheme.typography.titleMedium)
                        Spacer(modifier = Modifier.height(12.dp))

                        weeklyData.forEach { (day, duration) ->
                            Column(modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(day, style = MaterialTheme.typography.labelMedium)
                                    Text(
                                        "${duration / 60000} min",
                                        style = MaterialTheme.typography.labelSmall
                                    )
                                }
                                val maxDuration = weeklyData.values.maxOrNull() ?: 1L
                                val barProgress =
                                    if (maxDuration > 0) duration.toFloat() / maxDuration.toFloat() else 0f
                                LinearProgressIndicator(
                                    progress = { barProgress },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(8.dp),
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }
            }

            // Insights
            if (insights.isNotEmpty()) {
                item {
                    Text(text = "Insights", style = MaterialTheme.typography.titleLarge)
                }

                items(insights) { insight ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer
                        )
                    ) {
                        Text(
                            text = insight,
                            modifier = Modifier.padding(12.dp),
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }

            if (distribution.isNotEmpty()) {
                item {
                    Text(
                        text = "Category Distribution",
                        style = MaterialTheme.typography.titleLarge
                    )
                }

                items(distribution.size) { index ->
                    val (catId, duration) = distribution.toList()[index]
                    val category = categories.find { it.id == catId }
                    val catHours = duration / 3600000
                    val catMinutes = (duration % 3600000) / 60000

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(text = category?.name ?: "Unknown")
                        Text(text = "${catHours}h ${catMinutes}m")
                    }
                    LinearProgressIndicator(
                        progress = {
                            duration.toFloat() / sessions.sumOf { it.duration }.toFloat()
                        },
                        modifier = Modifier.fillMaxWidth(),
                        color = category?.let { Color(android.graphics.Color.parseColor(it.colorHex)) }
                            ?: MaterialTheme.colorScheme.primary
                    )
                }
            }

            item {
                Text(text = "Recent Sessions", style = MaterialTheme.typography.titleLarge)
            }

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
