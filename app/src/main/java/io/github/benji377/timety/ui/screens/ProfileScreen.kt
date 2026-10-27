package io.github.benji377.timety.ui.screens

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import io.github.benji377.timety.R
import io.github.benji377.timety.data.model.focus.FocusSessionEntity
import io.github.benji377.timety.data.model.habit.HabitWithCompletions
import io.github.benji377.timety.data.model.task.RecurringTaskWithOccurrences
import io.github.benji377.timety.data.model.task.TaskWithSubtasks
import io.github.benji377.timety.ui.components.common.NeoAlertDialog
import io.github.benji377.timety.ui.components.common.NeoIconButton
import io.github.benji377.timety.ui.components.common.NeoTopBar
import io.github.benji377.timety.ui.components.common.TextInputDialog
import io.github.benji377.timety.ui.theme.AppTheme
import io.github.benji377.timety.ui.theme.UserColor
import io.github.benji377.timety.ui.theme.hairlineBorder
import io.github.benji377.timety.ui.utils.WrapUpImageGenerator
import io.github.benji377.timety.ui.viewmodel.AppViewModelProvider
import io.github.benji377.timety.ui.viewmodel.FocusViewModel
import io.github.benji377.timety.ui.viewmodel.HabitViewModel
import io.github.benji377.timety.ui.viewmodel.RecurringTaskViewModel
import io.github.benji377.timety.ui.viewmodel.TaskViewModel
import io.github.benji377.timety.ui.viewmodel.UserViewModel
import io.github.benji377.timety.util.ProfileImageStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Displays the user's profile: avatar and name, with options to edit them or share a "wrap-up"
 * summary image.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    onNavigateToSettings: () -> Unit,
    userViewModel: UserViewModel = viewModel(factory = AppViewModelProvider.Factory),
    taskViewModel: TaskViewModel = viewModel(factory = AppViewModelProvider.Factory),
    habitViewModel: HabitViewModel = viewModel(factory = AppViewModelProvider.Factory),
    recurringViewModel: RecurringTaskViewModel = viewModel(factory = AppViewModelProvider.Factory),
    focusViewModel: FocusViewModel = io.github.benji377.timety.ui.viewmodel.activityScopedViewModel()
) {
    val userProfile by userViewModel.userProfile.collectAsState()
    val tasks by taskViewModel.allTasks.collectAsState()
    val habitsWithCompletions by habitViewModel.habitsWithCompletions.collectAsState()
    val recurringItems by recurringViewModel.allRecurringTasks.collectAsState()
    val sessions by focusViewModel.allSessions.collectAsState()

    val userName = userProfile?.name ?: "User"

    // Recomputed only when the data changes, not on every recomposition of this screen.
    val stats = remember(tasks, habitsWithCompletions, recurringItems, sessions) {
        ProfileStats.of(tasks, habitsWithCompletions, recurringItems, sessions)
    }
    val totalTasksDone = stats.totalTasksDone
    val totalHabitsMet = stats.totalHabitsMet
    val totalFocusMins = stats.totalFocusMins

    val context = LocalContext.current
    val shareSubjectStr = stringResource(R.string.userShareSubject)
    val shareTextStr = stringResource(R.string.userShareText)
    val shareWrapUpTooltipStr = stringResource(R.string.userTooltipShareWrapUp)
    val errorGenerateImageStr = stringResource(R.string.userErrorGenerateImage)
    var showEditNameDialog by remember { mutableStateOf(false) }
    var tempName by remember { mutableStateOf("") }
    var showShareWrapupDialog by remember { mutableStateOf(false) }

    val coroutineScope = rememberCoroutineScope()
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        uri?.let {
            coroutineScope.launch(Dispatchers.IO) {
                ProfileImageStore.persist(context, it)?.let { path ->
                    userViewModel.updateProfileImage(path)
                }
            }
        }
    }

    if (showShareWrapupDialog) {
        NeoAlertDialog(
            onDismissRequest = { showShareWrapupDialog = false },
            title = { Text(stringResource(R.string.userTooltipShareWrapUp)) },
            text = {
                Column {
                    Text(stringResource(R.string.userShareSummary), fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("${stringResource(R.string.userStatTasksDone)}: $totalTasksDone")
                    Text("${stringResource(R.string.userStatHabitsMet)}: $totalHabitsMet")
                    Text("${stringResource(R.string.userStatFocusMins)}: $totalFocusMins")
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    showShareWrapupDialog = false
                    coroutineScope.launch(Dispatchers.IO) {
                        try {
                            val bytes =
                                WrapUpImageGenerator.generate(
                                    context,
                                    userName,
                                    totalTasksDone,
                                    totalFocusMins,
                                    totalHabitsMet
                                )
                            val cachePath = File(context.cacheDir, "shared")
                            cachePath.mkdirs()
                            val file = File(cachePath, "wrapup.png")
                            file.writeBytes(bytes)
                            val uri = FileProvider.getUriForFile(
                                context,
                                "${context.packageName}.fileprovider",
                                file
                            )

                            withContext(Dispatchers.Main) {
                                val intent = Intent(Intent.ACTION_SEND).apply {
                                    type = "image/png"
                                    putExtra(Intent.EXTRA_STREAM, uri)
                                    putExtra(
                                        Intent.EXTRA_SUBJECT,
                                        shareSubjectStr
                                    )
                                    putExtra(
                                        Intent.EXTRA_TEXT,
                                        shareTextStr
                                    )
                                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                }
                                context.startActivity(
                                    Intent.createChooser(
                                        intent,
                                        shareWrapUpTooltipStr
                                    )
                                )
                            }
                        } catch (_: Exception) {
                            withContext(Dispatchers.Main) {
                                android.widget.Toast.makeText(
                                    context,
                                    errorGenerateImageStr,
                                    android.widget.Toast.LENGTH_SHORT
                                ).show()
                            }
                        }
                    }
                }) {
                    Text(stringResource(R.string.commonLabelShare))
                }
            },
            dismissButton = {
                TextButton(onClick = { showShareWrapupDialog = false }) {
                    Text(stringResource(R.string.commonLabelClose))
                }
            }
        )
    }

    TextInputDialog(
        visible = showEditNameDialog,
        title = stringResource(R.string.userEditNameTitle),
        labelText = stringResource(R.string.userEditNameHint),
        initialValue = tempName,
        onConfirm = { newName ->
            userViewModel.updateName(newName)
            showEditNameDialog = false
        },
        onDismiss = { showEditNameDialog = false },
    )

    Scaffold(
        topBar = {
            NeoTopBar(
                title = stringResource(R.string.userProfileTitle),
                actions = {
                    NeoIconButton(
                        onClick = { showShareWrapupDialog = true },
                        icon = Icons.Filled.Share,
                        contentDescription = stringResource(R.string.userTooltipShareWrapUp),
                        modifier = Modifier.padding(end = AppTheme.spaceSmall),
                    )
                    NeoIconButton(
                        onClick = onNavigateToSettings,
                        icon = Icons.Filled.Settings,
                        contentDescription = stringResource(R.string.settingsTitle),
                        modifier = Modifier.padding(end = AppTheme.spaceSmall),
                    )
                }
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            item { Spacer(modifier = Modifier.height(24.dp)) }

            // Avatar Section
            item {
                Box(contentAlignment = Alignment.BottomEnd) {
                    Box(
                        modifier = Modifier
                            .size(120.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceContainerHighest)
                            .hairlineBorder(),
                        contentAlignment = Alignment.Center
                    ) {
                        // validOrNull drops stale entries (pre-2.1.0 content:// URIs whose read
                        // grant expired) so they fall back to the placeholder icon.
                        val avatarPath = remember(userProfile?.profileImagePath) {
                            ProfileImageStore.validOrNull(userProfile?.profileImagePath)
                        }
                        if (avatarPath != null) {
                            AsyncImage(
                                model = avatarPath,
                                contentDescription = null,
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Filled.Person,
                                contentDescription = null,
                                modifier = Modifier.size(60.dp),
                                tint = UserColor
                            )
                        }
                    }
                    val avatarBadgeInteractionSource = remember { MutableInteractionSource() }
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(UserColor)
                            .hairlineBorder()
                            .clickable(
                                interactionSource = avatarBadgeInteractionSource,
                                indication = ripple(),
                            ) {
                                photoPickerLauncher.launch(
                                    PickVisualMediaRequest(
                                        ActivityResultContracts.PickVisualMedia.ImageOnly
                                    )
                                )
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.CameraAlt,
                            contentDescription = stringResource(R.string.userTooltipEditAvatar),
                            tint = Color.White,
                            modifier = Modifier.size(AppTheme.iconSizeMedium)
                        )
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(24.dp)) }

            // Name Section
            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = userName,
                        style = MaterialTheme.typography.headlineLarge.copy(
                            fontWeight = FontWeight.Bold
                        )
                    )
                    Spacer(modifier = Modifier.width(AppTheme.spaceSmall))
                    NeoIconButton(
                        onClick = {
                            tempName = userName
                            showEditNameDialog = true
                        },
                        icon = Icons.Filled.Edit,
                        contentDescription = stringResource(R.string.userEditNameTitle),
                        contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            item { Spacer(modifier = Modifier.height(40.dp)) }
        }
    }
}

/** Lifetime totals shown in the share wrap-up. */
private data class ProfileStats(
    val totalTasksDone: Int,
    val totalHabitsMet: Int,
    val totalFocusMins: Int,
) {
    companion object {
        fun of(
            tasks: List<TaskWithSubtasks>,
            habits: List<HabitWithCompletions>,
            recurring: List<RecurringTaskWithOccurrences>,
            sessions: List<FocusSessionEntity>,
        ): ProfileStats = ProfileStats(
            // Each logged recurring occurrence counts as a completed task.
            totalTasksDone = tasks.count { it.task.isCompleted } + recurring.sumOf { it.occurrences.size },
            totalHabitsMet = habits.sumOf { it.completions.size },
            totalFocusMins = sessions.sumOf { it.totalSecondsFocused } / 60,
        )
    }
}
