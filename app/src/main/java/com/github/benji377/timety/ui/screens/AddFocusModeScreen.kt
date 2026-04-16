package com.github.benji377.timety.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.github.benji377.timety.data.FocusStep
import com.github.benji377.timety.data.FocusStepBehavior
import com.github.benji377.timety.data.FocusStepType
import com.github.benji377.timety.viewmodel.FocusViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddFocusModeScreen(
    viewModel: FocusViewModel,
    onBack: () -> Unit
) {
    var title by remember { mutableStateOf("") }
    val steps = remember { mutableStateListOf<FocusStep>() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Create Focus Mode") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    TextButton(
                        onClick = {
                            if (title.isNotBlank() && steps.isNotEmpty()) {
                                // Add START at beginning and END/LOOP at end if missing
                                val finalSteps = mutableListOf<FocusStep>()
                                if (steps.firstOrNull()?.type != FocusStepType.START) {
                                    finalSteps.add(FocusStep(0, FocusStepType.START))
                                }
                                finalSteps.addAll(steps)
                                if (finalSteps.last().type != FocusStepType.END && finalSteps.last().type != FocusStepType.LOOP) {
                                    finalSteps.add(FocusStep(0, FocusStepType.END))
                                }
                                viewModel.addFocusMode(title, finalSteps)
                                onBack()
                            }
                        },
                        enabled = title.isNotBlank() && steps.isNotEmpty()
                    ) {
                        Text("Save")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(16.dp)
        ) {
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("Title") },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(24.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Steps", style = MaterialTheme.typography.titleMedium)
                Button(onClick = {
                    steps.add(FocusStep(25, FocusStepType.FOCUS, FocusStepBehavior.COUNT_DOWN))
                }) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Add Step")
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                itemsIndexed(steps) { index, step ->
                    StepItem(
                        step = step,
                        onUpdate = { updated -> steps[index] = updated },
                        onDelete = { steps.removeAt(index) },
                        isLast = index == steps.size - 1
                    )
                }
            }
        }
    }
}

@Composable
fun StepItem(
    step: FocusStep,
    onUpdate: (FocusStep) -> Unit,
    onDelete: () -> Unit,
    isLast: Boolean
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = when(step.type) {
                                FocusStepType.FOCUS -> "F"
                                FocusStepType.REST -> "R"
                                FocusStepType.END -> "E"
                                FocusStepType.LOOP -> "L"
                                else -> step.type.name.first().toString()
                            },
                            color = MaterialTheme.colorScheme.onPrimary,
                            style = MaterialTheme.typography.labelLarge
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = step.type.name,
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete Step", tint = MaterialTheme.colorScheme.error)
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Type Selector
                var showTypeMenu by remember { mutableStateOf(false) }
                Box(modifier = Modifier.weight(1f)) {
                    OutlinedButton(
                        onClick = { showTypeMenu = true },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(step.type.name)
                    }
                    DropdownMenu(expanded = showTypeMenu, onDismissRequest = { showTypeMenu = false }) {
                        val types = if (isLast) {
                            listOf(FocusStepType.FOCUS, FocusStepType.REST, FocusStepType.END, FocusStepType.LOOP)
                        } else {
                            listOf(FocusStepType.FOCUS, FocusStepType.REST)
                        }
                        types.forEach { type ->
                            DropdownMenuItem(
                                text = { Text(type.name) },
                                onClick = {
                                    onUpdate(step.copy(type = type))
                                    showTypeMenu = false
                                }
                            )
                        }
                    }
                }

                if (step.type == FocusStepType.FOCUS || step.type == FocusStepType.REST) {
                    // Duration
                    OutlinedTextField(
                        value = step.durationMins.toString(),
                        onValueChange = { 
                            val duration = it.toIntOrNull() ?: 0
                            onUpdate(step.copy(durationMins = duration))
                        },
                        label = { Text("Mins") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.width(80.dp)
                    )

                    // Behavior
                    var showBehaviorMenu by remember { mutableStateOf(false) }
                    Box(modifier = Modifier.weight(1f)) {
                        OutlinedButton(
                            onClick = { showBehaviorMenu = true },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(if (step.behavior == FocusStepBehavior.COUNT_UP) "Up" else "Down")
                        }
                        DropdownMenu(expanded = showBehaviorMenu, onDismissRequest = { showBehaviorMenu = false }) {
                            FocusStepBehavior.entries.forEach { behavior ->
                                DropdownMenuItem(
                                    text = { Text(behavior.name) },
                                    onClick = {
                                        onUpdate(step.copy(behavior = behavior))
                                        showBehaviorMenu = false
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
