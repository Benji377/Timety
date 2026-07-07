package io.github.benji377.timety.ui.screens.focus

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import io.github.benji377.timety.R
import io.github.benji377.timety.data.model.focus.FocusModeEntity
import io.github.benji377.timety.data.model.focus.FocusModeType
import io.github.benji377.timety.data.model.focus.PhaseType
import io.github.benji377.timety.data.model.focus.SessionPhaseEntity
import io.github.benji377.timety.ui.components.common.TimetyTopBar
import io.github.benji377.timety.ui.components.focus.FocusModeEditCard
import io.github.benji377.timety.ui.theme.AppTheme
import io.github.benji377.timety.ui.theme.FocusColor
import io.github.benji377.timety.ui.theme.LocalSnackbarHostState
import io.github.benji377.timety.ui.viewmodel.FocusViewModel
import io.github.benji377.timety.ui.viewmodel.activityScopedViewModel
import kotlinx.coroutines.launch
import java.util.UUID


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FocusModesScreen(
    onNavigateBack: () -> Unit,
    focusViewModel: FocusViewModel = activityScopedViewModel(),
) {
    val modes by focusViewModel.allModes.collectAsState()
    var pendingMode by remember { mutableStateOf<FocusModeEntity?>(null) }
    val newModeLabel = stringResource(R.string.focusModeLabelNew)
    val snackbarHostState = LocalSnackbarHostState.current
    val modeSavedScope = androidx.compose.runtime.rememberCoroutineScope()
    val modeSavedMessage = stringResource(R.string.focusModeSnackbarSaved)
    val modeInUseMessage = stringResource(R.string.focusModeSnackbarInUse)

    Scaffold(
        topBar = {
            TimetyTopBar(
                title = stringResource(R.string.focusModesTitle),
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                    }
                }
            )
        },
        floatingActionButton = {
            if (pendingMode == null) {
                ExtendedFloatingActionButton(
                    onClick = {
                        val newId = UUID.randomUUID().toString()
                        pendingMode = FocusModeEntity(
                            id = newId,
                            name = newModeLabel,
                            type = FocusModeType.CUSTOM
                        )
                    },
                    modifier = Modifier.border(
                        AppTheme.neoBorderWidth,
                        MaterialTheme.colorScheme.outline,
                        AppTheme.brNeo
                    ),
                    shape = AppTheme.brNeo,
                    elevation = FloatingActionButtonDefaults.elevation(
                        0.dp,
                        0.dp,
                        0.dp,
                        0.dp
                    ),
                    icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                    text = { Text(newModeLabel) },
                    containerColor = FocusColor,
                    contentColor = Color.White,
                )
            }
        },
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
        ) {
            item { Spacer(modifier = Modifier.height(16.dp)) }

            pendingMode?.let { mode ->
                item(key = mode.id) {
                    FocusModeEditCard(
                        mode = mode,
                        phases = listOf(
                            SessionPhaseEntity(
                                modeId = mode.id,
                                type = PhaseType.FOCUS,
                                durationMinutes = 25,
                                orderIndex = 0
                            ),
                            SessionPhaseEntity(
                                modeId = mode.id,
                                type = PhaseType.REST,
                                durationMinutes = 5,
                                orderIndex = 1
                            ),
                        ),
                        isNewMode = true,
                        onCancelNew = { pendingMode = null },
                        onSaveNew = { pendingMode = null },
                        onSave = { updatedMode, updatedPhases ->
                            focusViewModel.saveMode(
                                updatedMode,
                                updatedPhases
                            )
                            modeSavedScope.launch { snackbarHostState.showSnackbar(modeSavedMessage) }
                        },
                        onDelete = { },
                    )
                }
            }

            items(modes, key = { it.id }) { mode ->
                var phases by remember(mode.id) { mutableStateOf<List<SessionPhaseEntity>>(emptyList()) }
                LaunchedEffect(mode.id) {
                    focusViewModel.getPhasesForMode(mode.id).collect { phases = it }
                }
                FocusModeEditCard(
                    mode = mode,
                    phases = phases,
                    isNewMode = false,
                    onSave = { updatedMode, updatedPhases ->
                        focusViewModel.saveMode(
                            updatedMode,
                            updatedPhases
                        )
                        modeSavedScope.launch { snackbarHostState.showSnackbar(modeSavedMessage) }
                    },
                    onDelete = {
                        focusViewModel.deleteMode(it) {
                            modeSavedScope.launch { snackbarHostState.showSnackbar(modeInUseMessage) }
                        }
                    },
                )
            }

            item { Spacer(modifier = Modifier.height(100.dp)) }
        }
    }
}
