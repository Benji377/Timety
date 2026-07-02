package io.github.benji377.timety.ui.screens.focus

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import io.github.benji377.timety.data.model.focus.FocusModeType
import io.github.benji377.timety.ui.theme.FocusColor

data class MockFocusMode(
    val id: String,
    val name: String,
    val type: FocusModeType,
    val phases: List<MockSessionPhase>
)

data class MockSessionPhase(
    val isFocus: Boolean,
    val durationMinutes: Int
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FocusModesScreen(
    onNavigateBack: () -> Unit
) {
    var modes by remember {
        mutableStateOf(
            listOf(
                MockFocusMode(
                    id = "1",
                    name = "Pomodoro",
                    type = FocusModeType.POMODORO,
                    phases = listOf(MockSessionPhase(true, 25), MockSessionPhase(false, 5))
                ),
                MockFocusMode(
                    id = "2",
                    name = "52/17 Rule",
                    type = FocusModeType.CUSTOM,
                    phases = listOf(MockSessionPhase(true, 52), MockSessionPhase(false, 17))
                )
            )
        )
    }

    var pendingMode by remember { mutableStateOf<MockFocusMode?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Focus Modes", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Filled.ArrowBack, "Back")
                    }
                }
            )
        },
        floatingActionButton = {
            if (pendingMode == null) {
                ExtendedFloatingActionButton(
                    onClick = {
                        pendingMode = MockFocusMode(
                            id = "new",
                            name = "New Mode",
                            type = FocusModeType.CUSTOM,
                            phases = listOf(MockSessionPhase(true, 25), MockSessionPhase(false, 5))
                        )
                    },
                    icon = { Icon(Icons.Filled.Add, null) },
                    text = { Text("New Mode") },
                    containerColor = FocusColor,
                    contentColor = Color.White
                )
            }
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            if (pendingMode != null) {
                item {
                    FocusModeEditCard(
                        mode = pendingMode!!,
                        isNewMode = true,
                        onCancel = { pendingMode = null },
                        onSave = { 
                            modes = modes + it
                            pendingMode = null 
                        }
                    )
                }
            }

            items(modes) { mode ->
                FocusModeEditCard(
                    mode = mode,
                    isNewMode = false,
                    onCancel = { },
                    onSave = { updatedMode ->
                        modes = modes.map { if (it.id == updatedMode.id) updatedMode else it }
                    }
                )
            }
            item { Spacer(modifier = Modifier.height(80.dp)) }
        }
    }
}

@Composable
fun FocusModeEditCard(
    mode: MockFocusMode,
    isNewMode: Boolean,
    onCancel: () -> Unit,
    onSave: (MockFocusMode) -> Unit
) {
    var isEditing by remember { mutableStateOf(isNewMode) }
    var name by remember(mode) { mutableStateOf(mode.name) }
    var phases by remember(mode) { mutableStateOf(mode.phases) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            if (isEditing) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Mode Name") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(16.dp))
                
                phases.forEachIndexed { index, phase ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (phase.isFocus) Icons.Filled.Whatshot else Icons.Filled.Coffee,
                            contentDescription = null,
                            tint = if (phase.isFocus) FocusColor else Color.Gray,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Text(
                            if (phase.isFocus) "Focus Phase" else "Rest Phase",
                            modifier = Modifier.weight(1f),
                            fontWeight = FontWeight.Medium
                        )
                        OutlinedTextField(
                            value = phase.durationMinutes.toString(),
                            onValueChange = { 
                                val newVal = it.toIntOrNull() ?: 0
                                phases = phases.toMutableList().apply { set(index, phase.copy(durationMinutes = newVal)) }
                            },
                            modifier = Modifier.width(80.dp),
                            singleLine = true
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("m", color = Color.Gray)
                    }
                }
                
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = {
                        if (isNewMode) onCancel() else isEditing = false
                    }) {
                        Text("Cancel")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            onSave(mode.copy(name = name, phases = phases))
                            isEditing = false
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = FocusColor)
                    ) {
                        Text("Save")
                    }
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(mode.name, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    IconButton(onClick = { isEditing = true }) {
                        Icon(Icons.Filled.Edit, "Edit")
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    phases.forEachIndexed { index, phase ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = if (phase.isFocus) Icons.Filled.Whatshot else Icons.Filled.Coffee,
                                contentDescription = null,
                                tint = if (phase.isFocus) FocusColor else Color.Gray,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("${phase.durationMinutes}m", fontWeight = FontWeight.Bold)
                            
                            if (index < phases.size - 1) {
                                Icon(
                                    Icons.Filled.ArrowForward,
                                    contentDescription = null,
                                    modifier = Modifier.padding(horizontal = 8.dp).size(16.dp),
                                    tint = Color.Gray
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
