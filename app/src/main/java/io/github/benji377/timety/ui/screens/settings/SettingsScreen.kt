package io.github.benji377.timety.ui.screens.settings

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.BugReport
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.Forum
import androidx.compose.material.icons.outlined.Label
import androidx.compose.material.icons.outlined.LocalOffer
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material.icons.outlined.UploadFile
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.benji377.timety.R
import io.github.benji377.timety.ui.components.common.TextInputDialog
import io.github.benji377.timety.ui.theme.FocusColor
import io.github.benji377.timety.ui.theme.HabitColor
import io.github.benji377.timety.ui.theme.TaskColor
import io.github.benji377.timety.ui.theme.WarningAccent
import io.github.benji377.timety.ui.utils.quantityString
import io.github.benji377.timety.ui.viewmodel.AppViewModelProvider
import io.github.benji377.timety.ui.viewmodel.FocusViewModel
import io.github.benji377.timety.ui.viewmodel.SettingsViewModel
import io.github.benji377.timety.ui.viewmodel.TaskViewModel
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Application settings for theme, notifications, API, and backups. Mirrors `screens/settings_screen.dart`.
 *
 * Deviations from Flutter, noted per section below where relevant:
 * - Flutter renders Theme/Language/Date-Format as an inline [DropdownButton] embedded in the row's
 *   trailing slot. Material3 `ListItem` has no equivalent inline dropdown affordance, so (matching
 *   this codebase's existing convention for such rows) the row shows the current selection as
 *   trailing text and opens a simple option-list dialog on tap. Behavior/labels/values match Flutter.
 * - Focus Tags / Task Categories / Export / Import have no wired destination yet in the Kotlin nav
 *   graph (no route registered in `MainScreen`, no `BackupService` port); these show a short
 *   "coming soon" toast on tap, same placeholder approach as before this port.
 * - Location API editing uses the shared [io.github.benji377.timety.ui.components.common.TextInputDialog]
 *   without Flutter's live endpoint validation (no validator exists yet on `SettingsRepository`).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit,
    settingsViewModel: SettingsViewModel = viewModel(factory = AppViewModelProvider.Factory),
    taskViewModel: TaskViewModel = viewModel(factory = AppViewModelProvider.Factory),
    focusViewModel: FocusViewModel = viewModel(factory = AppViewModelProvider.Factory)
) {
    val context = LocalContext.current

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
    var showThemeDialog by remember { mutableStateOf(false) }
    var showLanguageDialog by remember { mutableStateOf(false) }
    var showDateFormatDialog by remember { mutableStateOf(false) }
    var showLocationDialog by remember { mutableStateOf(false) }
    var numberDialogSpec by remember { mutableStateOf<NumberDialogSpec?>(null) }
    var timeDialogSpec by remember { mutableStateOf<TimeDialogSpec?>(null) }

    // --- OPTION LABEL MAPS (value -> display, mirrors the Flutter dropdown item lists exactly) ---
    val themeOptions = listOf(
        stringResource(R.string.settingsLabelThemeLight) to "Light",
        stringResource(R.string.settingsLabelThemeDark) to "Dark",
        stringResource(R.string.settingsLabelThemeSystem) to "System Default"
    )
    val currentThemeLabel = themeOptions.firstOrNull { it.second == themePref }?.first
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
    OptionsDialog(
        visible = showThemeDialog,
        title = stringResource(R.string.settingsLabelTheme),
        options = themeOptions,
        onSelect = { settingsViewModel.setThemePref(it) },
        onDismiss = { showThemeDialog = false }
    )
    OptionsDialog(
        visible = showLanguageDialog,
        title = stringResource(R.string.settingsLabelLanguage),
        options = languageOptions,
        onSelect = { settingsViewModel.setAppLocaleCode(it) },
        onDismiss = { showLanguageDialog = false }
    )
    OptionsDialog(
        visible = showDateFormatDialog,
        title = stringResource(R.string.settingsLabelDateFormat),
        options = dateFormatOptions,
        onSelect = { settingsViewModel.setDateFormat(it) },
        onDismiss = { showDateFormatDialog = false }
    )
    TextInputDialog(
        visible = showLocationDialog,
        title = stringResource(R.string.settingsLabelLocationApi),
        labelText = stringResource(R.string.settingsLabelLocationApi),
        initialValue = locationApiEndpoint,
        onConfirm = { settingsViewModel.setLocationApiEndpoint(it) },
        onDismiss = { showLocationDialog = false }
    )
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
            TopAppBar(
                title = { Text(stringResource(R.string.settingsTitle), fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = null)
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
                ListItem(
                    headlineContent = { Text(stringResource(R.string.settingsLabelTheme)) },
                    leadingContent = { Icon(Icons.Outlined.DarkMode, null) },
                    trailingContent = { Text(currentThemeLabel, color = Color.Gray) },
                    modifier = Modifier.clickable { showThemeDialog = true }
                )
            }

            item { HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp)) }

            // --- LOCALIZATION & FORMATTING ---
            item { SettingsHeader(stringResource(R.string.settingsSectionLocaleFormat)) }
            item {
                ListItem(
                    headlineContent = { Text(stringResource(R.string.settingsLabelLanguage)) },
                    leadingContent = { Icon(Icons.Filled.Language, null, tint = TaskColor) },
                    trailingContent = { Text(currentLanguageLabel, color = Color.Gray) },
                    modifier = Modifier.clickable { showLanguageDialog = true }
                )
                ListItem(
                    headlineContent = { Text(stringResource(R.string.settingsLabelTimeFormat)) },
                    leadingContent = { Icon(Icons.Filled.AccessTime, null, tint = FocusColor) },
                    trailingContent = {
                        Switch(
                            checked = use24HourFormat,
                            onCheckedChange = { settingsViewModel.setUse24HourFormat(it) },
                            colors = SwitchDefaults.colors(
                                checkedTrackColor = Color(0xFF4CAF50),
                                uncheckedTrackColor = Color(0xFFFF5252)
                            )
                        )
                    }
                )
                ListItem(
                    headlineContent = { Text(stringResource(R.string.settingsLabelDateFormat)) },
                    leadingContent = { Icon(Icons.Filled.CalendarToday, null, tint = HabitColor) },
                    trailingContent = { Text(currentDateFormatLabel, color = Color.Gray) },
                    modifier = Modifier.clickable { showDateFormatDialog = true }
                )
            }

            item { HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp)) }

            // --- FOCUS & PRODUCTIVITY ---
            item { SettingsHeader(stringResource(R.string.settingsSectionFocusProductivity)) }
            item {
                ListItem(
                    headlineContent = { Text(stringResource(R.string.settingsLabelFocusGoal)) },
                    supportingContent = {
                        Text(quantityString(R.plurals.nMinutesCount, dailyGoalMins, R.string.nMinutesCountZero, dailyGoalMins))
                    },
                    leadingContent = { Icon(Icons.Filled.TrackChanges, null, tint = FocusColor) },
                    trailingContent = { Icon(Icons.Filled.ChevronRight, null) },
                    modifier = Modifier.clickable {
                        numberDialogSpec = NumberDialogSpec(
                            title = context.getString(R.string.settingsLabelFocusGoal),
                            current = dailyGoalMins, min = 10, max = 480, unit = minutesUnit,
                            onSave = { settingsViewModel.setDailyGoalMins(it) }
                        )
                    }
                )
                ListItem(
                    headlineContent = { Text(stringResource(R.string.settingsLabelFocusAutocomplete)) },
                    supportingContent = { Text(stringResource(R.string.settingsLabelFocusAutocompleteSubtitle)) },
                    leadingContent = { Icon(Icons.Filled.TaskAlt, null, tint = TaskColor) },
                    trailingContent = {
                        Switch(
                            checked = autoCompleteFocus,
                            onCheckedChange = { settingsViewModel.setAutoCompleteFocus(it) },
                            colors = SwitchDefaults.colors(
                                checkedTrackColor = Color(0xFF4CAF50),
                                uncheckedTrackColor = Color(0xFFFF5252)
                            )
                        )
                    }
                )
                ListItem(
                    headlineContent = { Text(stringResource(R.string.settingsLabelFocusStopwatch)) },
                    supportingContent = { Text(stringResource(R.string.settingsLabelFocusStopwatchSubtitle)) },
                    leadingContent = { Icon(Icons.Outlined.Timer, null, tint = WarningAccent) },
                    trailingContent = { Icon(Icons.Filled.ChevronRight, null) },
                    modifier = Modifier.clickable {
                        numberDialogSpec = NumberDialogSpec(
                            title = context.getString(R.string.settingsLabelFocusStopwatch),
                            current = maxStopwatchMins, min = 30, max = 480, unit = minutesUnit,
                            onSave = { settingsViewModel.setMaxStopwatchMins(it) }
                        )
                    }
                )
                ListItem(
                    headlineContent = { Text(stringResource(R.string.settingsLabelFocusNodeTime)) },
                    supportingContent = { Text(stringResource(R.string.settingsLabelFocusNodeTimeSubtitle)) },
                    leadingContent = { Icon(Icons.Filled.LinearScale, null, tint = TaskColor) },
                    trailingContent = { Icon(Icons.Filled.ChevronRight, null) },
                    modifier = Modifier.clickable {
                        numberDialogSpec = NumberDialogSpec(
                            title = context.getString(R.string.settingsLabelFocusNodeTime),
                            current = maxNodeMins, min = 10, max = 480, unit = minutesUnit,
                            onSave = { settingsViewModel.setMaxNodeMins(it) }
                        )
                    }
                )
                ListItem(
                    headlineContent = { Text(stringResource(R.string.settingsLabelUpcomingTask)) },
                    supportingContent = { Text(stringResource(R.string.settingsLabelUpcomingTaskSubtitle, upcomingTasksHorizon)) },
                    leadingContent = { Icon(Icons.Outlined.Schedule, null, tint = TaskColor) },
                    trailingContent = { Icon(Icons.Filled.ChevronRight, null) },
                    modifier = Modifier.clickable {
                        numberDialogSpec = NumberDialogSpec(
                            title = context.getString(R.string.settingsLabelUpcomingTask),
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
                    headlineContent = { Text(stringResource(R.string.settingsLabelTags)) },
                    supportingContent = { Text(stringResource(R.string.settingsLabelTagsSubtitle, focusTags.size)) },
                    leadingContent = { Icon(Icons.Outlined.LocalOffer, null, tint = FocusColor) },
                    trailingContent = { Icon(Icons.Filled.ChevronRight, null) },
                    modifier = Modifier.clickable {
                        Toast.makeText(context, "Focus tag management coming soon", Toast.LENGTH_SHORT).show()
                    }
                )
                ListItem(
                    headlineContent = { Text(stringResource(R.string.settingsLabelCategories)) },
                    supportingContent = { Text(stringResource(R.string.settingsLabelCategoriesSubtitle, categoryCount)) },
                    leadingContent = { Icon(Icons.Outlined.Label, null, tint = TaskColor) },
                    trailingContent = { Icon(Icons.Filled.ChevronRight, null) },
                    modifier = Modifier.clickable {
                        Toast.makeText(context, "Category management coming soon", Toast.LENGTH_SHORT).show()
                    }
                )
            }

            item { HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp)) }

            // --- API & SERVICES ---
            item { SettingsHeader(stringResource(R.string.settingsSectionApi)) }
            item {
                ListItem(
                    headlineContent = { Text(stringResource(R.string.settingsLabelLocationApi)) },
                    supportingContent = {
                        Text(
                            text = if (locationApiEndpoint.length > 40) locationApiEndpoint.take(40) + "..." else locationApiEndpoint,
                            maxLines = 1
                        )
                    },
                    leadingContent = { Icon(Icons.Filled.Cloud, null, tint = TaskColor) },
                    trailingContent = { Icon(Icons.Filled.Edit, null) },
                    modifier = Modifier.clickable { showLocationDialog = true }
                )
            }

            item { HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp)) }

            // --- NOTIFICATIONS ---
            item { SettingsHeader(stringResource(R.string.settingsSectionNotifications)) }
            item {
                ListItem(
                    headlineContent = { Text(stringResource(R.string.settingsLabelDailyMotivation)) },
                    supportingContent = { Text(formatTimeOfDay(dailyMotivationTime, use24HourFormat)) },
                    leadingContent = { Icon(Icons.Filled.Schedule, null, tint = WarningAccent) },
                    trailingContent = { Icon(Icons.Filled.Edit, null) },
                    modifier = Modifier.clickable {
                        timeDialogSpec = TimeDialogSpec(
                            title = context.getString(R.string.settingsLabelDailyMotivation),
                            current = dailyMotivationTime,
                            onSave = { settingsViewModel.setDailyMotivationTime(it) }
                        )
                    }
                )
                ListItem(
                    headlineContent = { Text(stringResource(R.string.settingsLabelEodCheckup)) },
                    supportingContent = { Text(formatTimeOfDay(endOfDayCheckupTime, use24HourFormat)) },
                    leadingContent = { Icon(Icons.Filled.NightlightRound, null, tint = HabitColor) },
                    trailingContent = { Icon(Icons.Filled.Edit, null) },
                    modifier = Modifier.clickable {
                        timeDialogSpec = TimeDialogSpec(
                            title = context.getString(R.string.settingsLabelEodCheckup),
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
                    headlineContent = { Text(stringResource(R.string.settingsLabelExportData)) },
                    supportingContent = { Text(stringResource(R.string.settingsLabelExportDataSubtitle)) },
                    leadingContent = { Icon(Icons.Outlined.UploadFile, null, tint = TaskColor) },
                    modifier = Modifier.clickable {
                        Toast.makeText(context, "Data export coming soon", Toast.LENGTH_SHORT).show()
                    }
                )
                ListItem(
                    headlineContent = { Text(stringResource(R.string.settingsLabelImportData)) },
                    supportingContent = { Text(stringResource(R.string.settingsLabelImportDataSubtitle)) },
                    leadingContent = { Icon(Icons.Outlined.Download, null, tint = FocusColor) },
                    modifier = Modifier.clickable {
                        Toast.makeText(context, "Data import coming soon", Toast.LENGTH_SHORT).show()
                    }
                )
            }

            item { HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp)) }

            // --- SUPPORT & FEEDBACK ---
            item { SettingsHeader(stringResource(R.string.settingsSectionSupport)) }
            item {
                ListItem(
                    headlineContent = { Text(stringResource(R.string.settingsLabelCommunity)) },
                    supportingContent = { Text(stringResource(R.string.settingsLabelCommunitySubtitle)) },
                    leadingContent = { Icon(Icons.Outlined.Forum, null, tint = FocusColor) },
                    trailingContent = { Icon(Icons.Filled.OpenInNew, null, modifier = Modifier.size(16.dp), tint = Color.Gray) },
                    modifier = Modifier.clickable {
                        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/Benji377/Timety/discussions")))
                    }
                )
                ListItem(
                    headlineContent = { Text(stringResource(R.string.settingsLabelFeedback)) },
                    supportingContent = { Text(stringResource(R.string.settingsLabelFeedbackSubtitle)) },
                    leadingContent = { Icon(Icons.Outlined.BugReport, null, tint = HabitColor) },
                    trailingContent = { Icon(Icons.Filled.OpenInNew, null, modifier = Modifier.size(16.dp), tint = Color.Gray) },
                    modifier = Modifier.clickable {
                        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://tally.so/r/ODbEoA")))
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
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
                    ) {
                        Spacer(modifier = Modifier.height(24.dp))
                        Box(
                            modifier = Modifier
                                .size(60.dp)
                                .clip(CircleShape)
                                .background(Color.White),
                            contentAlignment = Alignment.Center
                        ) {
                            Image(
                                painter = painterResource(id = R.mipmap.ic_launcher),
                                contentDescription = null,
                                modifier = Modifier.size(48.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(stringResource(R.string.appTitle), fontWeight = FontWeight.Bold, fontSize = 20.sp)
                        Text(
                            stringResource(R.string.settingsLabelVersion, versionName ?: "1.0.0"),
                            color = Color.Gray,
                            fontSize = 12.sp
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        ListItem(
                            headlineContent = { Text(stringResource(R.string.settingsLabelBuiltBy)) },
                            supportingContent = { Text(stringResource(R.string.settingsLabelMaintainer)) },
                            leadingContent = { Icon(Icons.Filled.Person, null, tint = Color(0xFFFF5722)) },
                            colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                        )
                        ListItem(
                            headlineContent = { Text(stringResource(R.string.settingsLabelDonate)) },
                            supportingContent = { Text(stringResource(R.string.settingsLabelDonateSubtitle)) },
                            leadingContent = { Icon(Icons.Filled.Favorite, null, tint = Color.Red) },
                            trailingContent = { Icon(Icons.Filled.OpenInNew, null, modifier = Modifier.size(16.dp), tint = Color.Gray) },
                            colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                            modifier = Modifier.clickable {
                                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/sponsors/Benji377")))
                            }
                        )
                        ListItem(
                            headlineContent = { Text(stringResource(R.string.settingsLabelSourceCode)) },
                            supportingContent = { Text(stringResource(R.string.settingsLabelSourceCodeSubtitle)) },
                            leadingContent = { Icon(Icons.Filled.Code, null, tint = Color.Blue) },
                            trailingContent = { Icon(Icons.Filled.OpenInNew, null, modifier = Modifier.size(16.dp), tint = Color.Gray) },
                            colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                            modifier = Modifier.clickable {
                                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/Benji377/Timety")))
                            }
                        )
                    }
                }
            }
        }
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

/** Simple option-list picker used in place of Flutter's inline [DropdownButton]s (see file header note). */
@Composable
private fun OptionsDialog(
    visible: Boolean,
    title: String,
    options: List<Pair<String, String>>,
    onSelect: (String) -> Unit,
    onDismiss: () -> Unit
) {
    if (!visible) return
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column {
                options.forEach { (label, value) ->
                    Text(
                        text = label,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                onSelect(value)
                                onDismiss()
                            }
                            .padding(vertical = 12.dp),
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.commonLabelCancel)) }
        }
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
    var value by remember(spec) { mutableStateOf(spec.current.toFloat()) }
    val steps = (((spec.max - spec.min) / 5) - 1).coerceAtLeast(0)
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(spec.title) },
        text = {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("${value.toInt()} ${spec.unit}", fontWeight = FontWeight.Bold, fontSize = 24.sp)
                Slider(
                    value = value,
                    onValueChange = { value = it },
                    valueRange = spec.min.toFloat()..spec.max.toFloat(),
                    steps = steps
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
    val context = LocalContext.current
    val (initialHour, initialMinute) = parseHHmm(spec.current)
    val is24Hour = android.text.format.DateFormat.is24HourFormat(context)
    val timePickerState = rememberTimePickerState(initialHour = initialHour, initialMinute = initialMinute, is24Hour = is24Hour)
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

private fun parseHHmm(value: String): Pair<Int, Int> {
    return try {
        val parts = value.split(":")
        parts[0].trim().toInt() to parts[1].trim().toInt()
    } catch (e: Exception) {
        8 to 0
    }
}

/**
 * NOTE (date formatting): mirrors `SettingsProvider.getFormattedTimeOfDay`, which honors the
 * 24h flag + device locale. No centralized Kotlin equivalent exists yet, so this formats the
 * stored "HH:mm" string directly via [DateTimeFormatter] with the device locale.
 */
private fun formatTimeOfDay(hhmm: String, use24Hour: Boolean): String {
    val (h, m) = parseHHmm(hhmm)
    val time = LocalTime.of(h, m)
    val pattern = if (use24Hour) "HH:mm" else "h:mm a"
    return time.format(DateTimeFormatter.ofPattern(pattern, Locale.getDefault()))
}
