package io.github.benji377.timety.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.automirrored.outlined.Label
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.ArrowDropUp
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.LinearScale
import androidx.compose.material.icons.filled.NightlightRound
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.TaskAlt
import androidx.compose.material.icons.filled.TrackChanges
import androidx.compose.material.icons.outlined.BugReport
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.Forum
import androidx.compose.material.icons.outlined.LocalOffer
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material.icons.outlined.UploadFile
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.net.toUri
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.benji377.timety.R
import io.github.benji377.timety.TimetyApplication
import io.github.benji377.timety.data.repository.ThemeMode
import io.github.benji377.timety.ui.components.common.ConfirmationDialog
import io.github.benji377.timety.ui.components.common.TimetyTopBar
import io.github.benji377.timety.ui.theme.AppTheme
import io.github.benji377.timety.ui.theme.ErrorColor
import io.github.benji377.timety.ui.theme.FocusColor
import io.github.benji377.timety.ui.theme.HabitColor
import io.github.benji377.timety.ui.theme.LocalSnackbarHostState
import io.github.benji377.timety.ui.theme.SuccessColor
import io.github.benji377.timety.ui.theme.TaskColor
import io.github.benji377.timety.ui.theme.WarningAccent
import io.github.benji377.timety.ui.utils.LocalDateFormatSettings
import io.github.benji377.timety.ui.utils.quantityString
import io.github.benji377.timety.ui.viewmodel.AppViewModelProvider
import io.github.benji377.timety.ui.viewmodel.FocusViewModel
import io.github.benji377.timety.ui.viewmodel.SettingsViewModel
import io.github.benji377.timety.ui.viewmodel.TaskViewModel
import io.github.benji377.timety.ui.viewmodel.activityScopedViewModel
import io.github.benji377.timety.util.datetime.AppDateFormatUtils
import kotlinx.coroutines.launch
import kotlin.math.roundToInt
import io.github.benji377.timety.ui.components.common.TimetyOutlinedTextField as OutlinedTextField


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit,
    onNavigateToTags: () -> Unit = {},
    onNavigateToCategories: () -> Unit = {},
    settingsViewModel: SettingsViewModel = viewModel(factory = AppViewModelProvider.Factory),
    taskViewModel: TaskViewModel = viewModel(factory = AppViewModelProvider.Factory),
    focusViewModel: FocusViewModel = activityScopedViewModel()
) {
    val context = LocalContext.current
    val backupExportSuccessStr = stringResource(R.string.backupExportSuccess)
    val backupExportFailureRaw = stringResource(R.string.backupExportFailure)
    val focusGoalTitle = stringResource(R.string.settingsLabelFocusGoal)
    val focusStopwatchTitle = stringResource(R.string.settingsLabelFocusStopwatch)
    val focusNodeTimeTitle = stringResource(R.string.settingsLabelFocusNodeTime)
    val upcomingTaskTitle = stringResource(R.string.settingsLabelUpcomingTask)
    val dailyMotivationTitle = stringResource(R.string.settingsLabelDailyMotivation)
    val eodCheckupTitle = stringResource(R.string.settingsLabelEodCheckup)
    val backupImportFailureStr = stringResource(R.string.backupImportFailure)
    val backupExportSubjectStr = stringResource(R.string.backupExportSubject)
    val backupShareFailureRaw = stringResource(R.string.backupShareFailure)
    val scope = rememberCoroutineScope()
    val snackbarHostState = LocalSnackbarHostState.current
    val backupService = remember {
        (context.applicationContext as TimetyApplication).container.backupService
    }
    // Import runs in two steps to mirror Flutter: pick file -> confirm overwrite -> restore -> restart dialog.
    var pendingImportUri by remember { mutableStateOf<Uri?>(null) }
    var showRestartDialog by remember { mutableStateOf(false) }
    var showExportOptions by remember { mutableStateOf(false) }
    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            val result = backupService.exportToUri(uri)
            val msg = if (result.isSuccess) {
                backupExportSuccessStr
            } else {
                backupExportFailureRaw.format(result.exceptionOrNull()?.message ?: "")
            }
            snackbarHostState.showSnackbar(msg)
        }
    }
    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) pendingImportUri = uri
    }

    val themePref by settingsViewModel.themePref.collectAsState()
    val appLocaleCode by settingsViewModel.appLocaleCode.collectAsState()
    val use24HourFormat by settingsViewModel.use24HourFormat.collectAsState()
    val dateFormat by settingsViewModel.dateFormat.collectAsState()
    val autoCompleteFocus by settingsViewModel.autoCompleteFocus.collectAsState()
    val dailyGoalMins by settingsViewModel.dailyGoalMins.collectAsState()
    val maxStopwatchMins by settingsViewModel.maxStopwatchMins.collectAsState()
    val maxNodeMins by settingsViewModel.maxNodeMins.collectAsState()
    val upcomingTasksHorizon by settingsViewModel.upcomingTasksHorizon.collectAsState()
    val dailyMotivationTime by settingsViewModel.dailyMotivationTime.collectAsState()
    val endOfDayCheckupTime by settingsViewModel.endOfDayCheckupTime.collectAsState()
    val locationApiEndpoint by settingsViewModel.locationApiEndpoint.collectAsState()

    val focusTags by focusViewModel.allTags.collectAsState()
    val categoryCount = taskViewModel.getAllCategories().size

    val versionName = try {
        context.packageManager.getPackageInfo(context.packageName, 0).versionName
    } catch (e: Exception) {
        "1.0.0"
    }

    // --- DIALOG STATE ---

    var showLocationDialog by remember { mutableStateOf(false) }
    var pendingLocationUrl by remember { mutableStateOf(locationApiEndpoint) }
    var locationError by remember { mutableStateOf<String?>(null) }
    var isCheckingLocation by remember { mutableStateOf(false) }
    var numberDialogSpec by remember { mutableStateOf<NumberDialogSpec?>(null) }
    var timeDialogSpec by remember { mutableStateOf<TimeDialogSpec?>(null) }

    // --- OPTION LABEL MAPS (value -> display, mirrors the Flutter dropdown item lists exactly) ---
    val themeOptions = listOf(
        stringResource(R.string.settingsLabelThemeLight) to ThemeMode.LIGHT.storageValue,
        stringResource(R.string.settingsLabelThemeDark) to ThemeMode.DARK.storageValue,
        stringResource(R.string.settingsLabelThemeSystem) to ThemeMode.SYSTEM.storageValue
    )
    val currentThemeLabel = themeOptions.firstOrNull { it.second == themePref.storageValue }?.first
        ?: stringResource(R.string.settingsLabelThemeSystem)

    val languageOptions = listOf(
        stringResource(R.string.settingsLabelLanguageSystem) to "system",
        "English" to "en",
        "Deutsch" to "de",
        "Italiano" to "it",
        "Ladin" to "lld"
    )
    val currentLanguageLabel = languageOptions.firstOrNull { it.second == appLocaleCode }?.first
        ?: stringResource(R.string.settingsLabelLanguageSystem)

    val dateFormatOptions = listOf(
        stringResource(R.string.settingsLabelDateFormatSystem) to "System",
        stringResource(R.string.settingsLabelDateFormatSystemDMY) to "dd/MM/yyyy",
        stringResource(R.string.settingsLabelDateFormatSystemMDY) to "MM/dd/yyyy",
        stringResource(R.string.settingsLabelDateFormatSystemYMD) to "yyyy-MM-dd",
        stringResource(R.string.settingsLabelDateFormatSystemDotDMY) to "dd.MM.yyyy"
    )
    val currentDateFormatLabel = dateFormatOptions.firstOrNull { it.second == dateFormat }?.first
        ?: stringResource(R.string.settingsLabelDateFormatSystem)

    val minutesUnit = stringResource(R.string.settingsDialogUnitMinutes)
    val daysUnit = stringResource(R.string.settingsDialogUnitDays)

    // --- DIALOGS ---

    if (showLocationDialog) {
        AlertDialog(
            onDismissRequest = {
                showLocationDialog = false
                locationError = null
                pendingLocationUrl = locationApiEndpoint
            },
            title = { Text(stringResource(R.string.settingsLabelLocationApi)) },
            text = {
                Column {
                    OutlinedTextField(
                        value = pendingLocationUrl,
                        onValueChange = {
                            pendingLocationUrl = it
                            locationError = null
                        },
                        label = { Text(stringResource(R.string.settingsLabelLocationApi)) },
                        singleLine = true,
                        isError = locationError != null,
                        supportingText = locationError?.let {
                            {
                                Text(
                                    it,
                                    color = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    )
                    if (isCheckingLocation) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                stringResource(R.string.settingsLabelValidating),
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showLocationDialog = false
                        locationError = null
                        pendingLocationUrl = locationApiEndpoint
                    },
                    enabled = !isCheckingLocation
                ) { Text(stringResource(R.string.commonLabelCancel)) }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val trimmed = pendingLocationUrl.trim()
                        if (trimmed.isEmpty()) {
                            settingsViewModel.setLocationApiEndpoint(trimmed)
                            showLocationDialog = false
                        } else {
                            scope.launch {
                                isCheckingLocation = true
                                val isValid = settingsViewModel.validateLocationApiEndpoint(trimmed)
                                isCheckingLocation = false
                                if (isValid) {
                                    settingsViewModel.setLocationApiEndpoint(trimmed)
                                    showLocationDialog = false
                                } else {
                                    locationError = "Invalid endpoint or not reachable"
                                }
                            }
                        }
                    },
                    enabled = !isCheckingLocation
                ) { Text(stringResource(R.string.commonLabelSave)) }
            }
        )
    }
    numberDialogSpec?.let { spec ->
        NumberPickerDialog(
            spec = spec,
            onDismiss = { numberDialogSpec = null }
        )
    }
    timeDialogSpec?.let { spec ->
        TimePickerDialogRow(
            spec = spec,
            onDismiss = { timeDialogSpec = null }
        )
    }

    Scaffold(
        topBar = {
            TimetyTopBar(
                title = stringResource(R.string.settingsTitle),
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                    }
                }
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(bottom = 40.dp)
        ) {
            // --- APPEARANCE ---
            item { SettingsHeader(stringResource(R.string.settingsSectionAppearance)) }
            item {
                DropdownSettingsItem(
                    headlineText = stringResource(R.string.settingsLabelTheme),
                    leadingIcon = { Icon(Icons.Outlined.DarkMode, null) },
                    currentLabel = currentThemeLabel,
                    options = themeOptions,
                    onSelect = { settingsViewModel.setThemePref(ThemeMode.fromStorage(it)) }
                )
            }

            item { HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp)) }

            // --- LOCALIZATION & FORMATTING ---
            item { SettingsHeader(stringResource(R.string.settingsSectionLocaleFormat)) }
            item {
                DropdownSettingsItem(
                    headlineText = stringResource(R.string.settingsLabelLanguage),
                    leadingIcon = { Icon(Icons.Filled.Language, null, tint = TaskColor) },
                    currentLabel = currentLanguageLabel,
                    options = languageOptions,
                    onSelect = { settingsViewModel.setAppLocaleCode(it) },
                    optionIcon = { value ->
                        val flagName = when (value) {
                            "en" -> "gb"
                            "de" -> "de"
                            "it" -> "it"
                            "lld" -> "lld"
                            else -> null
                        }
                        if (flagName != null) {
                            coil.compose.AsyncImage(
                                model = coil.request.ImageRequest.Builder(LocalContext.current)
                                    .data("file:///android_asset/flags/$flagName.svg")
                                    .decoderFactory(coil.decode.SvgDecoder.Factory())
                                    .build(),
                                contentDescription = null,
                                modifier = Modifier.size(24.dp)
                            )
                        } else {
                            Icon(
                                Icons.Filled.Language,
                                contentDescription = null,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                )
                ListItem(
                    colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                    headlineContent = { Text(stringResource(R.string.settingsLabelTimeFormat)) },
                    leadingContent = { Icon(Icons.Filled.AccessTime, null, tint = FocusColor) },
                    trailingContent = {
                        Switch(
                            checked = use24HourFormat,
                            onCheckedChange = { settingsViewModel.setUse24HourFormat(it) },
                            colors = SwitchDefaults.colors(
                                checkedTrackColor = SuccessColor,
                                uncheckedTrackColor = ErrorColor
                            )
                        )
                    }
                )
                DropdownSettingsItem(
                    headlineText = stringResource(R.string.settingsLabelDateFormat),
                    leadingIcon = { Icon(Icons.Filled.CalendarToday, null, tint = HabitColor) },
                    currentLabel = currentDateFormatLabel,
                    options = dateFormatOptions,
                    onSelect = { settingsViewModel.setDateFormat(it) }
                )
            }

            item { HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp)) }

            // --- FOCUS & PRODUCTIVITY ---
            item { SettingsHeader(stringResource(R.string.settingsSectionFocusProductivity)) }
            item {
                ListItem(
                    colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                    headlineContent = { Text(stringResource(R.string.settingsLabelFocusGoal)) },
                    supportingContent = {
                        Text(
                            quantityString(
                                R.plurals.nMinutesCount,
                                dailyGoalMins,
                                R.string.nMinutesCountZero,
                                dailyGoalMins
                            )
                        )
                    },
                    leadingContent = { Icon(Icons.Filled.TrackChanges, null, tint = FocusColor) },
                    trailingContent = { Icon(Icons.Filled.ChevronRight, null) },
                    modifier = Modifier.clickable {
                        numberDialogSpec = NumberDialogSpec(
                            title = focusGoalTitle,
                            current = dailyGoalMins, min = 10, max = 480, unit = minutesUnit,
                            onSave = { settingsViewModel.setDailyGoalMins(it) }
                        )
                    }
                )
                ListItem(
                    colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                    headlineContent = { Text(stringResource(R.string.settingsLabelFocusAutocomplete)) },
                    supportingContent = { Text(stringResource(R.string.settingsLabelFocusAutocompleteSubtitle)) },
                    leadingContent = { Icon(Icons.Filled.TaskAlt, null, tint = TaskColor) },
                    trailingContent = {
                        Switch(
                            checked = autoCompleteFocus,
                            onCheckedChange = { settingsViewModel.setAutoCompleteFocus(it) },
                            colors = SwitchDefaults.colors(
                                checkedTrackColor = SuccessColor,
                                uncheckedTrackColor = ErrorColor
                            )
                        )
                    }
                )
                ListItem(
                    colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                    headlineContent = { Text(stringResource(R.string.settingsLabelFocusStopwatch)) },
                    supportingContent = { Text(stringResource(R.string.settingsLabelFocusStopwatchSubtitle)) },
                    leadingContent = { Icon(Icons.Outlined.Timer, null, tint = WarningAccent) },
                    trailingContent = { Icon(Icons.Filled.ChevronRight, null) },
                    modifier = Modifier.clickable {
                        numberDialogSpec = NumberDialogSpec(
                            title = focusStopwatchTitle,
                            current = maxStopwatchMins, min = 30, max = 480, unit = minutesUnit,
                            onSave = { settingsViewModel.setMaxStopwatchMins(it) }
                        )
                    }
                )
                ListItem(
                    colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                    headlineContent = { Text(stringResource(R.string.settingsLabelFocusNodeTime)) },
                    supportingContent = { Text(stringResource(R.string.settingsLabelFocusNodeTimeSubtitle)) },
                    leadingContent = { Icon(Icons.Filled.LinearScale, null, tint = TaskColor) },
                    trailingContent = { Icon(Icons.Filled.ChevronRight, null) },
                    modifier = Modifier.clickable {
                        numberDialogSpec = NumberDialogSpec(
                            title = focusNodeTimeTitle,
                            current = maxNodeMins, min = 10, max = 480, unit = minutesUnit,
                            onSave = { settingsViewModel.setMaxNodeMins(it) }
                        )
                    }
                )
                ListItem(
                    colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                    headlineContent = { Text(stringResource(R.string.settingsLabelUpcomingTask)) },
                    supportingContent = {
                        Text(
                            stringResource(
                                R.string.settingsLabelUpcomingTaskSubtitle,
                                upcomingTasksHorizon
                            )
                        )
                    },
                    leadingContent = { Icon(Icons.Outlined.Schedule, null, tint = TaskColor) },
                    trailingContent = { Icon(Icons.Filled.ChevronRight, null) },
                    modifier = Modifier.clickable {
                        numberDialogSpec = NumberDialogSpec(
                            title = upcomingTaskTitle,
                            current = upcomingTasksHorizon, min = 1, max = 60, unit = daysUnit,
                            onSave = { settingsViewModel.setUpcomingTasksHorizon(it) }
                        )
                    }
                )
            }

            item { HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp)) }

            // --- ORGANIZATION ---
            item { SettingsHeader(stringResource(R.string.settingsSectionOrganization)) }
            item {
                ListItem(
                    colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                    headlineContent = { Text(stringResource(R.string.settingsLabelTags)) },
                    supportingContent = {
                        Text(
                            stringResource(
                                R.string.settingsLabelTagsSubtitle,
                                focusTags.size
                            )
                        )
                    },
                    leadingContent = { Icon(Icons.Outlined.LocalOffer, null, tint = FocusColor) },
                    trailingContent = { Icon(Icons.Filled.ChevronRight, null) },
                    modifier = Modifier.clickable { onNavigateToTags() }
                )
                ListItem(
                    colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                    headlineContent = { Text(stringResource(R.string.settingsLabelCategories)) },
                    supportingContent = {
                        Text(
                            stringResource(
                                R.string.settingsLabelCategoriesSubtitle,
                                categoryCount
                            )
                        )
                    },
                    leadingContent = {
                        Icon(
                            Icons.AutoMirrored.Outlined.Label,
                            null,
                            tint = TaskColor
                        )
                    },
                    trailingContent = { Icon(Icons.Filled.ChevronRight, null) },
                    modifier = Modifier.clickable { onNavigateToCategories() }
                )
            }

            item { HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp)) }

            // --- API & SERVICES ---
            item { SettingsHeader(stringResource(R.string.settingsSectionApi)) }
            item {
                ListItem(
                    colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                    headlineContent = { Text(stringResource(R.string.settingsLabelLocationApi)) },
                    supportingContent = {
                        Text(
                            text = if (locationApiEndpoint.length > 40) locationApiEndpoint.take(40) + "..." else locationApiEndpoint,
                            maxLines = 1
                        )
                    },
                    leadingContent = { Icon(Icons.Filled.Cloud, null, tint = TaskColor) },
                    trailingContent = { Icon(Icons.Filled.Edit, null) },
                    modifier = Modifier.clickable {
                        pendingLocationUrl = locationApiEndpoint
                        locationError = null
                        showLocationDialog = true
                    }
                )
            }

            item { HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp)) }

            // --- NOTIFICATIONS ---
            item { SettingsHeader(stringResource(R.string.settingsSectionNotifications)) }
            item {
                ListItem(
                    colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                    headlineContent = { Text(stringResource(R.string.settingsLabelDailyMotivation)) },
                    supportingContent = {
                        Text(
                            AppDateFormatUtils.formatTimeOfDay(
                                dailyMotivationTime,
                                use24HourFormat
                            )
                        )
                    },
                    leadingContent = { Icon(Icons.Filled.Schedule, null, tint = WarningAccent) },
                    trailingContent = { Icon(Icons.Filled.Edit, null) },
                    modifier = Modifier.clickable {
                        timeDialogSpec = TimeDialogSpec(
                            title = dailyMotivationTitle,
                            current = dailyMotivationTime,
                            onSave = { settingsViewModel.setDailyMotivationTime(it) }
                        )
                    }
                )
                ListItem(
                    colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                    headlineContent = { Text(stringResource(R.string.settingsLabelEodCheckup)) },
                    supportingContent = {
                        Text(
                            AppDateFormatUtils.formatTimeOfDay(
                                endOfDayCheckupTime,
                                use24HourFormat
                            )
                        )
                    },
                    leadingContent = {
                        Icon(
                            Icons.Filled.NightlightRound,
                            null,
                            tint = HabitColor
                        )
                    },
                    trailingContent = { Icon(Icons.Filled.Edit, null) },
                    modifier = Modifier.clickable {
                        timeDialogSpec = TimeDialogSpec(
                            title = eodCheckupTitle,
                            current = endOfDayCheckupTime,
                            onSave = { settingsViewModel.setEndOfDayCheckupTime(it) }
                        )
                    }
                )
            }

            item { HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp)) }

            // --- DATA & BACKUP ---
            item { SettingsHeader(stringResource(R.string.settingsSectionDataBackup)) }
            item {
                ListItem(
                    colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                    headlineContent = { Text(stringResource(R.string.settingsLabelExportData)) },
                    supportingContent = { Text(stringResource(R.string.settingsLabelExportDataSubtitle)) },
                    leadingContent = { Icon(Icons.Outlined.UploadFile, null, tint = TaskColor) },
                    modifier = Modifier.clickable { showExportOptions = true }
                )
                ListItem(
                    colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                    headlineContent = { Text(stringResource(R.string.settingsLabelImportData)) },
                    supportingContent = { Text(stringResource(R.string.settingsLabelImportDataSubtitle)) },
                    leadingContent = { Icon(Icons.Outlined.Download, null, tint = FocusColor) },
                    modifier = Modifier.clickable {
                        importLauncher.launch(arrayOf("application/json"))
                    }
                )
            }

            item { HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp)) }

            // --- SUPPORT & FEEDBACK ---
            item { SettingsHeader(stringResource(R.string.settingsSectionSupport)) }
            item {
                ListItem(
                    colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                    headlineContent = { Text(stringResource(R.string.settingsLabelCommunity)) },
                    supportingContent = { Text(stringResource(R.string.settingsLabelCommunitySubtitle)) },
                    leadingContent = { Icon(Icons.Outlined.Forum, null, tint = FocusColor) },
                    trailingContent = {
                        Icon(
                            Icons.AutoMirrored.Filled.OpenInNew,
                            null,
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    },
                    modifier = Modifier.clickable {
                        context.startActivity(
                            Intent(
                                Intent.ACTION_VIEW,
                                "https://github.com/Benji377/Timety/discussions".toUri()
                            )
                        )
                    }
                )
                ListItem(
                    colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                    headlineContent = { Text(stringResource(R.string.settingsLabelFeedback)) },
                    supportingContent = { Text(stringResource(R.string.settingsLabelFeedbackSubtitle)) },
                    leadingContent = { Icon(Icons.Outlined.BugReport, null, tint = HabitColor) },
                    trailingContent = {
                        Icon(
                            Icons.AutoMirrored.Filled.OpenInNew,
                            null,
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    },
                    modifier = Modifier.clickable {
                        context.startActivity(
                            Intent(
                                Intent.ACTION_VIEW,
                                "https://tally.so/r/ODbEoA".toUri()
                            )
                        )
                    }
                )
            }

            item { HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp)) }

            // --- ABOUT & INFO SECTION ---
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.Transparent),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp)
                    ) {
                        Spacer(modifier = Modifier.height(24.dp))
                        Box(
                            modifier = Modifier
                                .size(60.dp)
                                .clip(CircleShape)
                                .background(Color.White),
                            contentAlignment = Alignment.Center
                        ) {
                            coil.compose.AsyncImage(
                                model = R.mipmap.ic_launcher,
                                contentDescription = null,
                                modifier = Modifier.size(48.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            stringResource(R.string.appTitle),
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp
                        )
                        Text(
                            stringResource(R.string.settingsLabelVersion, versionName ?: "1.0.0"),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 12.sp
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        ListItem(
                            headlineContent = { Text(stringResource(R.string.settingsLabelBuiltBy)) },
                            supportingContent = { Text(stringResource(R.string.settingsLabelMaintainer)) },
                            leadingContent = {
                                Icon(
                                    Icons.Filled.Person,
                                    null,
                                    tint = WarningAccent
                                )
                            },
                            colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                        )
                        ListItem(
                            headlineContent = { Text(stringResource(R.string.settingsLabelDonate)) },
                            supportingContent = { Text(stringResource(R.string.settingsLabelDonateSubtitle)) },
                            leadingContent = {
                                Icon(
                                    Icons.Filled.Favorite,
                                    null,
                                    tint = Color.Red
                                )
                            },
                            trailingContent = {
                                Icon(
                                    Icons.AutoMirrored.Filled.OpenInNew,
                                    null,
                                    modifier = Modifier.size(16.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            },
                            colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                            modifier = Modifier.clickable {
                                context.startActivity(
                                    Intent(
                                        Intent.ACTION_VIEW,
                                        "https://github.com/sponsors/Benji377".toUri()
                                    )
                                )
                            }
                        )
                        ListItem(
                            headlineContent = { Text(stringResource(R.string.settingsLabelSourceCode)) },
                            supportingContent = { Text(stringResource(R.string.settingsLabelSourceCodeSubtitle)) },
                            leadingContent = { Icon(Icons.Filled.Code, null, tint = Color.Blue) },
                            trailingContent = {
                                Icon(
                                    Icons.AutoMirrored.Filled.OpenInNew,
                                    null,
                                    modifier = Modifier.size(16.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            },
                            colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                            modifier = Modifier.clickable {
                                context.startActivity(
                                    Intent(
                                        Intent.ACTION_VIEW,
                                        "https://github.com/Benji377/Timety".toUri()
                                    )
                                )
                            }
                        )
                    }
                }
            }
        }
    }

    // Import confirmation (overwrite warning), mirrors Flutter's showConfirmation before restore.
    ConfirmationDialog(
        visible = pendingImportUri != null,
        title = stringResource(R.string.backupImportConfirmTitle),
        content = stringResource(R.string.backupImportConfirmBody),
        onConfirm = {
            val uri = pendingImportUri
            pendingImportUri = null
            if (uri != null) {
                scope.launch {
                    val result = backupService.importFromUri(uri)
                    if (result.isSuccess) {
                        showRestartDialog = true
                    } else {
                        snackbarHostState.showSnackbar(backupImportFailureStr)
                    }
                }
            }
        },
        onDismiss = { pendingImportUri = null }
    )

    // Restore-success dialog telling the user to restart the app, mirrors Flutter's success dialog.
    if (showExportOptions) {
        AlertDialog(
            onDismissRequest = { showExportOptions = false },
            title = { Text(stringResource(R.string.backupExportTitle)) },
            text = {
                Column {
                    ListItem(
                        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                        headlineContent = { Text(stringResource(R.string.backupActionSaveDevice)) },
                        supportingContent = { Text(stringResource(R.string.backupActionSaveDeviceSubtitle)) },
                        leadingContent = { Icon(Icons.Outlined.Download, null, tint = TaskColor) },
                        modifier = Modifier.clickable {
                            showExportOptions = false
                            exportLauncher.launch(backupService.suggestedFileName())
                        }
                    )
                    ListItem(
                        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                        headlineContent = { Text(stringResource(R.string.backupActionShareCloud)) },
                        supportingContent = { Text(stringResource(R.string.backupActionShareCloudSubtitle)) },
                        leadingContent = {
                            Icon(
                                Icons.Outlined.UploadFile,
                                null,
                                tint = FocusColor
                            )
                        },
                        modifier = Modifier.clickable {
                            showExportOptions = false
                            scope.launch {
                                backupService.exportToShareUri().fold(
                                    onSuccess = { uri ->
                                        val intent =
                                            Intent(Intent.ACTION_SEND)
                                                .apply {
                                                    type = "application/json"
                                                    putExtra(
                                                        Intent.EXTRA_STREAM,
                                                        uri
                                                    )
                                                    putExtra(
                                                        Intent.EXTRA_SUBJECT,
                                                        backupExportSubjectStr
                                                    )
                                                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                                }
                                        context.startActivity(
                                            Intent.createChooser(
                                                intent,
                                                backupExportSubjectStr
                                            )
                                        )
                                    },
                                    onFailure = { e ->
                                        snackbarHostState.showSnackbar(
                                            backupShareFailureRaw.format(e.message ?: "")
                                        )
                                    }
                                )
                            }
                        }
                    )
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showExportOptions = false }) {
                    Text(stringResource(R.string.commonLabelCancel))
                }
            }
        )
    }

    if (showRestartDialog) {
        AlertDialog(
            onDismissRequest = { showRestartDialog = false },
            title = { Text(stringResource(R.string.backupRestoreSuccessTitle)) },
            text = { Text(stringResource(R.string.backupRestoreSuccessBody)) },
            confirmButton = {
                TextButton(onClick = { showRestartDialog = false }) {
                    Text(stringResource(R.string.commonButtonGotIt))
                }
            }
        )
    }
}

@Composable
private fun SettingsHeader(title: String) {
    Text(
        text = title.uppercase(),
        style = MaterialTheme.typography.bodySmall.copy(
            color = TaskColor,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.2.sp
        ),
        modifier = Modifier.padding(start = 24.dp, top = 24.dp, end = 24.dp, bottom = 8.dp)
    )
}


@Composable
private fun DropdownSettingsItem(
    headlineText: String,
    leadingIcon: @Composable () -> Unit,
    currentLabel: String,
    options: List<Pair<String, String>>,
    onSelect: (String) -> Unit,
    optionIcon: (@Composable (String) -> Unit)? = null
) {
    var expanded by remember { mutableStateOf(false) }

    ListItem(
        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
        headlineContent = { Text(headlineText) },
        leadingContent = leadingIcon,
        trailingContent = {
            Box {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        currentLabel,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 14.sp
                    )
                    Spacer(Modifier.width(8.dp))
                    Icon(
                        if (expanded) Icons.Filled.ArrowDropUp else Icons.Filled.ArrowDropDown,
                        contentDescription = null
                    )
                }
                DropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false },
                    modifier = Modifier
                        .background(MaterialTheme.colorScheme.surface)
                        .border(
                            1.dp,
                            MaterialTheme.colorScheme.outline,
                            AppTheme.brNeo
                        )
                ) {
                    options.forEach { (label, value) ->
                        DropdownMenuItem(
                            text = { Text(label) },
                            leadingIcon = optionIcon?.let { { it(value) } },
                            onClick = {
                                onSelect(value)
                                expanded = false
                            }
                        )
                    }
                }
            }
        },
        modifier = Modifier.clickable { expanded = !expanded }
    )
}

private data class NumberDialogSpec(
    val title: String,
    val current: Int,
    val min: Int,
    val max: Int,
    val unit: String,
    val onSave: (Int) -> Unit
)

@Composable
private fun NumberPickerDialog(spec: NumberDialogSpec, onDismiss: () -> Unit) {
    var value by remember(spec) { mutableFloatStateOf(spec.current.toFloat()) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(spec.title) },
        text = {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    "${value.toInt()} ${spec.unit}",
                    fontWeight = FontWeight.Bold,
                    fontSize = 24.sp
                )
                // Continuous slider rounded to whole units: precise selection without
                // the coarse 5-unit jumps (or dozens of tick marks) a stepped slider gives.
                Slider(
                    value = value,
                    onValueChange = { value = it.roundToInt().toFloat() },
                    valueRange = spec.min.toFloat()..spec.max.toFloat()
                )
            }
        },
        confirmButton = {
            TextButton(onClick = {
                spec.onSave(value.toInt())
                onDismiss()
            }) { Text(stringResource(R.string.commonLabelSave)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.commonLabelCancel)) }
        }
    )
}

private data class TimeDialogSpec(
    val title: String,
    val current: String,
    val onSave: (String) -> Unit
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TimePickerDialogRow(spec: TimeDialogSpec, onDismiss: () -> Unit) {
    val (initialHour, initialMinute) = AppDateFormatUtils.parseHHmm(spec.current)
    val is24Hour = LocalDateFormatSettings.current.use24HourFormat
    val timePickerState = rememberTimePickerState(
        initialHour = initialHour,
        initialMinute = initialMinute,
        is24Hour = is24Hour
    )
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(spec.title) },
        text = {
            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                TimePicker(state = timePickerState)
            }
        },
        confirmButton = {
            TextButton(onClick = {
                spec.onSave("%02d:%02d".format(timePickerState.hour, timePickerState.minute))
                onDismiss()
            }) { Text(stringResource(R.string.commonLabelSave)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.commonLabelCancel)) }
        }
    )
}



