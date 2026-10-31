package io.github.benji377.timety.ui.screens.settings

import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Label
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.WavingHand
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.decode.SvgDecoder
import coil.request.ImageRequest
import io.github.benji377.timety.R
import io.github.benji377.timety.data.repository.ThemeMode
import io.github.benji377.timety.ui.theme.FocusColor
import io.github.benji377.timety.ui.theme.HabitColor
import io.github.benji377.timety.ui.theme.TaskColor
import io.github.benji377.timety.ui.utils.quantityString
import io.github.benji377.timety.ui.viewmodel.SettingsViewModel
import io.github.benji377.timety.ui.viewmodel.TagViewModel
import io.github.benji377.timety.ui.viewmodel.activityScopedViewModel

/** General settings: appearance, language and date/time formatting, then tags. */
@Composable
internal fun GeneralSettings(
    onNavigateToTags: () -> Unit,
    settingsViewModel: SettingsViewModel = activityScopedViewModel(),
    tagViewModel: TagViewModel = activityScopedViewModel(),
) {
    val tagCount = tagViewModel.allTags.collectAsState().value.size
    val themePref by settingsViewModel.themePref.collectAsState()
    val showHomeGreeting by settingsViewModel.showHomeGreeting.collectAsState()
    val appLocaleCode by settingsViewModel.appLocaleCode.collectAsState()
    val use24HourFormat by settingsViewModel.use24HourFormat.collectAsState()
    val dateFormat by settingsViewModel.dateFormat.collectAsState()

    // Option label maps: each pair is (display label, stored value) for a settings dropdown.
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
        "Ladin" to "lld",
        "Chinese" to "zh-CN"
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

    SettingsHeader(stringResource(R.string.settingsSectionAppearance))
    DropdownSettingsItem(
        headlineText = stringResource(R.string.settingsLabelTheme),
        leadingIcon = { Icon(Icons.Outlined.DarkMode, null) },
        currentLabel = currentThemeLabel,
        options = themeOptions,
        onSelect = { settingsViewModel.setThemePref(ThemeMode.fromStorage(it)) }
    )
    SwitchSettingsItem(
        title = stringResource(R.string.settingsLabelShowGreeting),
        subtitle = stringResource(R.string.settingsLabelShowGreetingSubtitle),
        icon = Icons.Outlined.WavingHand,
        checked = showHomeGreeting,
        onCheckedChange = { settingsViewModel.setShowHomeGreeting(it) }
    )

    SettingsGroupDivider()

    SettingsHeader(stringResource(R.string.settingsSectionLocaleFormat))
    DropdownSettingsItem(
        headlineText = stringResource(R.string.settingsLabelLanguage),
        leadingIcon = { Icon(Icons.Filled.Language, null, tint = TaskColor) },
        currentLabel = currentLanguageLabel,
        options = languageOptions,
        onSelect = { settingsViewModel.setAppLocaleCode(it) },
        optionIcon = { value -> LanguageFlag(value) }
    )
    SwitchSettingsItem(
        title = stringResource(R.string.settingsLabelTimeFormat),
        icon = Icons.Filled.AccessTime,
        iconTint = FocusColor,
        checked = use24HourFormat,
        onCheckedChange = { settingsViewModel.setUse24HourFormat(it) }
    )
    DropdownSettingsItem(
        headlineText = stringResource(R.string.settingsLabelDateFormat),
        leadingIcon = { Icon(Icons.Filled.CalendarToday, null, tint = HabitColor) },
        currentLabel = currentDateFormatLabel,
        options = dateFormatOptions,
        onSelect = { settingsViewModel.setDateFormat(it) }
    )

    SettingsGroupDivider()

    NavSettingsItem(
        title = stringResource(R.string.globalLabelTags),
        subtitle = quantityString(R.plurals.settingsTagsCount, tagCount, 0, tagCount),
        icon = Icons.AutoMirrored.Outlined.Label,
        iconTint = TaskColor,
        onClick = onNavigateToTags
    )
}

/** Flag for a stored locale code, or a globe for "system". */
@Composable
private fun LanguageFlag(localeCode: String) {
    val flagName = when (localeCode) {
        "en" -> "gb"
        "de" -> "de"
        "it" -> "it"
        "lld" -> "lld"
        "zh-CN" -> "zh"
        else -> null
    }
    if (flagName != null) {
        AsyncImage(
            model = ImageRequest.Builder(LocalContext.current)
                .data("file:///android_asset/flags/$flagName.svg")
                .decoderFactory(SvgDecoder.Factory())
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
