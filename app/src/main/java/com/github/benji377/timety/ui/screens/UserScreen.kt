package com.github.benji377.timety.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.github.benji377.timety.ui.components.XPBar
import com.github.benji377.timety.viewmodel.SettingsViewModel

@Composable
fun UserScreen(viewModel: SettingsViewModel) {
    val user by viewModel.user.collectAsState()
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Profile Header
        Box(
            modifier = Modifier
                .size(120.dp)
                .clip(CircleShape)
                .border(2.dp, MaterialTheme.colorScheme.primary, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Person,
                contentDescription = "Profile Picture",
                modifier = Modifier.size(80.dp),
                tint = MaterialTheme.colorScheme.primary
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        var name by remember(user?.name) { mutableStateOf(user?.name ?: "") }
        
        OutlinedTextField(
            value = name,
            onValueChange = { 
                name = it
                user?.let { u -> viewModel.updateUser(u.copy(name = it)) }
            },
            label = { Text("Username") },
            modifier = Modifier.width(250.dp),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Gamification UI
        val currentXp = user?.xp ?: 0
        val level = user?.level ?: 1
        
        val (title, emoji) = when {
            level < 5 -> "Novice" to "🌱"
            level < 10 -> "Apprentice" to "📜"
            level < 20 -> "Focus Master" to "🎯"
            level < 35 -> "Deep Work Sage" to "🧘"
            level < 50 -> "Time Architect" to "🏗️"
            else -> "Timety Grandmaster" to "👑"
        }

        val xpForNextLevel = level * 100 // Simplified exponential logic can be added later
        val progress = currentXp.toFloat() / xpForNextLevel

        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = emoji,
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.padding(end = 8.dp)
            )
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.secondary,
                fontWeight = FontWeight.Bold
            )
        }
        
        Spacer(modifier = Modifier.height(8.dp))

        XPBar(
            progress = progress,
            currentXp = currentXp,
            targetXp = xpForNextLevel,
            level = level,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 32.dp)
        )

        Spacer(modifier = Modifier.height(16.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                Icons.Default.LocalFireDepartment,
                contentDescription = "Streak",
                tint = Color(0xFFFF5722)
            )
            Text(
                text = "${user?.currentStreak ?: 0} Day Streak",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(start = 8.dp)
            )
        }

        Spacer(modifier = Modifier.height(32.dp))

        // Stats Sections
        Card(
            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
            onClick = { /* Navigate to All-Time Task Stats */ }
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Task Stats", style = MaterialTheme.typography.titleLarge)
                Text("Total Done vs Upcoming", style = MaterialTheme.typography.bodyMedium)
                // Placeholder for high-level metrics
            }
        }

        Card(
            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
            onClick = { /* Navigate to All-Time Focus Stats */ }
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Focus Stats", style = MaterialTheme.typography.titleLarge)
                Text("Focus session length over time", style = MaterialTheme.typography.bodyMedium)
                // Placeholder for bar/line chart
            }
        }
    }
}
