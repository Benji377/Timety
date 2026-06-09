import 'package:flutter/material.dart';
import 'package:package_info_plus/package_info_plus.dart';
import 'package:provider/provider.dart';
import 'package:url_launcher/url_launcher.dart';
import 'package:flutter_svg/flutter_svg.dart';
import '../l10n/app_localizations.dart';
import '../theme/app_theme.dart';
import '../providers/settings_provider.dart';
import '../providers/focus_provider.dart';
import '../providers/task_provider.dart';
import '../services/backup_service.dart';
import '../utils/datetime/date_time_pickers.dart';
import '../widgets/focus/focus_tags_widget.dart';
import '../widgets/task/task_categories_widget.dart';
import '../widgets/location/location_picker_dialog.dart';

/// Application settings for theme, notifications, API, and backups.
class SettingsScreen extends StatefulWidget {
  const SettingsScreen({super.key});

  @override
  State<SettingsScreen> createState() => _SettingsScreenState();
}

class _SettingsScreenState extends State<SettingsScreen> {
  late final Future<String> _appVersionFuture = _loadAppVersion();

  @override
  Widget build(BuildContext context) {
    final settings = context.watch<SettingsProvider>();
    final focusProvider = context.watch<FocusProvider>();
    final taskProvider = context.watch<TaskProvider>();

    final l10n = AppLocalizations.of(context)!;

    return Scaffold(
      appBar: AppBar(title: Text(l10n.settingsTitle), centerTitle: true),
      body: ListView(
        padding: const EdgeInsets.only(bottom: 40),
        children: [
          // --- APPEARANCE ---
          _buildSectionHeader(l10n.settingsSectionAppearance),
          ListTile(
            leading: const Icon(Icons.dark_mode_outlined),
            title: Text(l10n.settingsLabelTheme),
            trailing: DropdownButton<ThemeMode>(
              alignment: AlignmentDirectional.centerEnd,
              value: settings.themeMode,
              underline: const SizedBox(),
              items: [
                DropdownMenuItem(
                  value: ThemeMode.light,
                  child: Text(l10n.settingsLabelThemeLight),
                ),
                DropdownMenuItem(
                  value: ThemeMode.dark,
                  child: Text(l10n.settingsLabelThemeDark),
                ),
                DropdownMenuItem(
                  value: ThemeMode.system,
                  child: Text(l10n.settingsLabelThemeSystem),
                ),
              ],
              onChanged: (val) {
                if (val != null) settings.setThemeMode(val);
              },
            ),
          ),

          const Divider(height: 32),

          // --- LOCALIZATION & FORMATTING ---
          _buildSectionHeader(l10n.settingsSectionLocaleFormat),
          ListTile(
            leading: const Icon(Icons.language, color: AppTheme.taskColor),
            title: Text(l10n.settingsLabelLanguage),
            trailing: DropdownButton<String>(
              alignment: AlignmentDirectional.centerEnd,
              value: settings.appLocaleCode,
              underline: const SizedBox(),
              items: [
                // System Default (Uses an icon instead of a flag)
                _buildLangItem(
                  'system',
                  l10n.settingsLabelLanguageSystem,
                  isSystem: true,
                ),
                _buildLangItem('en', 'English', asset: 'assets/flags/gb.svg'),
                _buildLangItem('de', 'Deutsch', asset: 'assets/flags/de.svg'),
                _buildLangItem('it', 'Italiano', asset: 'assets/flags/it.svg'),
                _buildLangItem('lld', 'Ladin', asset: 'assets/flags/lld.svg'),
              ],
              onChanged: (val) {
                if (val != null) settings.setAppLocaleCode(val);
              },
            ),
          ),
          SwitchListTile(
            secondary: const Icon(
              Icons.access_time,
              color: AppTheme.focusColor,
            ),
            title: Text(l10n.settingsLabelTimeFormat),
            value: settings.use24HourFormat,
            onChanged: settings.set24HourFormat,
            activeTrackColor: Colors.green,
            inactiveTrackColor: Colors.redAccent,
          ),
          ListTile(
            leading: const Icon(
              Icons.calendar_today,
              color: AppTheme.habitColor,
            ),
            title: Text(l10n.settingsLabelDateFormat),
            trailing: DropdownButton<String>(
              value: settings.dateFormatCode,
              alignment: AlignmentDirectional.centerEnd,
              underline: const SizedBox(),
              items: [
                DropdownMenuItem(
                  value: 'system',
                  child: Text(l10n.settingsLabelDateFormatSystem),
                ),
                DropdownMenuItem(
                  value: 'dd/MM/yyyy',
                  child: Text(l10n.settingsLabelDateFormatSystemDMY),
                ),
                DropdownMenuItem(
                  value: 'MM/dd/yyyy',
                  child: Text(l10n.settingsLabelDateFormatSystemMDY),
                ),
                DropdownMenuItem(
                  value: 'yyyy-MM-dd',
                  child: Text(l10n.settingsLabelDateFormatSystemYMD),
                ),
                DropdownMenuItem(
                  value: 'dd.MM.yyyy',
                  child: Text(l10n.settingsLabelDateFormatSystemDotDMY),
                ),
              ],
              onChanged: (val) {
                if (val != null) settings.setDateFormatCode(val);
              },
            ),
          ),

          const Divider(height: 32),

          // --- FOCUS & PRODUCTIVITY ---
          _buildSectionHeader(l10n.settingsSectionFocusProductivity),
          ListTile(
            leading: const Icon(
              Icons.track_changes,
              color: AppTheme.focusColor,
            ),
            title: Text(l10n.settingsLabelFocusGoal),
            subtitle: Text(l10n.nMinutesCount(settings.dailyGoalMins)),
            trailing: const Icon(Icons.chevron_right),
            onTap: () => _showNumberPickerDialog(
              l10n.settingsLabelFocusGoal,
              settings.dailyGoalMins,
              10,
              480,
              (val) => settings.setDailyGoal(val),
              unit: l10n.settingsDialogUnitMinutes,
            ),
          ),
          SwitchListTile(
            secondary: const Icon(Icons.task_alt, color: AppTheme.taskColor),
            title: Text(l10n.settingsLabelFocusAutocomplete),
            subtitle: Text(l10n.settingsLabelFocusAutocompleteSubtitle),
            value: settings.autoCompleteFocusTargetOnFinish,
            onChanged: settings.setAutoCompleteFocusTargetOnFinish,
            activeTrackColor: Colors.green,
            inactiveTrackColor: Colors.redAccent,
          ),
          ListTile(
            leading: const Icon(
              Icons.timer_outlined,
              color: AppTheme.warningAccent,
            ),
            title: Text(l10n.settingsLabelFocusStopwatch),
            subtitle: Text(l10n.settingsLabelFocusStopwatchSubtitle),
            trailing: const Icon(Icons.chevron_right),
            onTap: () => _showNumberPickerDialog(
              l10n.settingsLabelFocusStopwatch,
              settings.maxStopwatchMins,
              30,
              480,
              (val) => settings.setMaxStopwatch(val),
              unit: l10n.settingsDialogUnitMinutes,
            ),
          ),
          ListTile(
            leading: const Icon(Icons.linear_scale, color: AppTheme.taskColor),
            title: Text(l10n.settingsLabelFocusNodeTime),
            subtitle: Text(l10n.settingsLabelFocusNodeTimeSubtitle),
            trailing: const Icon(Icons.chevron_right),
            onTap: () => _showNumberPickerDialog(
              l10n.settingsLabelFocusNodeTime,
              settings.maxNodeMins,
              10,
              480,
              (val) => settings.setMaxNode(val),
              unit: l10n.settingsDialogUnitMinutes,
            ),
          ),
          ListTile(
            leading: const Icon(
              Icons.schedule_outlined,
              color: AppTheme.taskColor,
            ),
            title: Text(l10n.settingsLabelUpcomingTask),
            subtitle: Text(
              l10n.settingsLabelUpcomingTaskSubtitle(
                settings.upcomingTasksDays,
              ),
            ),
            trailing: const Icon(Icons.chevron_right),
            onTap: () => _showNumberPickerDialog(
              l10n.settingsLabelUpcomingTask,
              settings.upcomingTasksDays,
              1,
              60,
              (val) => settings.setUpcomingTasksDays(val),
              unit: l10n.settingsDialogUnitDays,
            ),
          ),

          const Divider(height: 32),

          // --- TAGS & CATEGORIES ---
          _buildSectionHeader(l10n.settingsSectionOrganization),
          ListTile(
            leading: const Icon(
              Icons.local_offer_outlined,
              color: AppTheme.focusColor,
            ),
            title: Text(l10n.settingsLabelTags),
            subtitle: Text(
              l10n.settingsLabelTagsSubtitle(focusProvider.tags.length),
            ),
            trailing: const Icon(Icons.chevron_right),
            onTap: () => Navigator.push(
              context,
              MaterialPageRoute(builder: (_) => const TagsWidget()),
            ),
          ),
          ListTile(
            leading: const Icon(
              Icons.label_outlined,
              color: AppTheme.taskColor,
            ),
            title: Text(l10n.settingsLabelCategories),
            subtitle: Text(
              l10n.settingsLabelCategoriesSubtitle(
                taskProvider.getAllCategories().length,
              ),
            ),
            trailing: const Icon(Icons.chevron_right),
            onTap: () => Navigator.push(
              context,
              MaterialPageRoute(builder: (_) => const CategoriesWidget()),
            ),
          ),

          const Divider(height: 32),

          // --- API & SERVICES ---
          _buildSectionHeader(l10n.settingsSectionApi),
          ListTile(
            leading: const Icon(Icons.cloud, color: AppTheme.taskColor),
            title: Text(l10n.settingsLabelLocationApi),
            subtitle: Text(
              settings.locationApiEndpoint.length > 40
                  ? '${settings.locationApiEndpoint.substring(0, 40)}...'
                  : settings.locationApiEndpoint,
              maxLines: 1,
              overflow: TextOverflow.ellipsis,
            ),
            trailing: const Icon(Icons.edit),
            onTap: () => showLocationApiDialog(
              context: context,
              isStateMounted: () => mounted,
              settings: settings,
            ),
          ),

          const Divider(height: 32),

          // --- NOTIFICATIONS ---
          _buildSectionHeader(l10n.settingsSectionNotifications),
          ListTile(
            leading: const Icon(Icons.schedule, color: AppTheme.warningAccent),
            title: Text(l10n.settingsLabelDailyMotivation),
            subtitle: Text(settings.getFormattedTimeOfDay(settings.notificationTime),),
            trailing: const Icon(Icons.edit),
            onTap: () async {
              final TimeOfDay? time = await AppDatePickers.pickTime(
                context: context,
                initialTime: settings.notificationTime,
              );
              if (time != null && mounted) {
                settings.setNotificationTime(time);
              }
            },
          ),
          ListTile(
            leading: const Icon(
              Icons.nightlight_round,
              color: AppTheme.habitColor,
            ),
            title: Text(l10n.settingsLabelEodCheckup),
            subtitle: Text(settings.getFormattedTimeOfDay(settings.endOfDayTime),),
            trailing: const Icon(Icons.edit),
            onTap: () async {
              final TimeOfDay? time = await AppDatePickers.pickTime(
                context: context,
                initialTime: settings.endOfDayTime,
              );
              if (time != null && mounted) {
                settings.setEndOfDayTime(time);
              }
            },
          ),

          const Divider(height: 32),

          // --- DATA & BACKUP ---
          _buildSectionHeader(l10n.settingsSectionDataBackup),
          ListTile(
            leading: const Icon(
              Icons.upload_file_outlined,
              color: AppTheme.taskColor,
            ),
            title: Text(l10n.settingsLabelExportData),
            subtitle: Text(l10n.settingsLabelExportDataSubtitle),
            trailing: const Icon(Icons.chevron_right),
            onTap: () => BackupService.exportUserData(context),
          ),
          ListTile(
            leading: const Icon(
              Icons.download_outlined,
              color: AppTheme.focusColor,
            ),
            title: Text(l10n.settingsLabelImportData),
            subtitle: Text(l10n.settingsLabelImportDataSubtitle),
            trailing: const Icon(Icons.chevron_right),
            onTap: () => BackupService.importUserData(context),
          ),

          const Divider(height: 32),

          // --- SUPPORT & FEEDBACK ---
          _buildSectionHeader(l10n.settingsSectionSupport),
          ListTile(
            leading: const Icon(
              Icons.forum_outlined,
              color: AppTheme.focusColor,
            ),
            title: Text(l10n.settingsLabelCommunity),
            subtitle: Text(l10n.settingsLabelCommunitySubtitle),
            trailing: const Icon(
              Icons.open_in_new,
              size: 16,
              color: Colors.grey,
            ),
            onTap: () async {
              final url = Uri.parse(
                'https://github.com/Benji377/Timety/discussions',
              );
              if (await canLaunchUrl(url)) {
                await launchUrl(url, mode: LaunchMode.externalApplication);
              }
            },
          ),
          ListTile(
            leading: const Icon(
              Icons.bug_report_outlined,
              color: AppTheme.habitColor,
            ),
            title: Text(l10n.settingsLabelFeedback),
            subtitle: Text(l10n.settingsLabelFeedbackSubtitle),
            trailing: const Icon(
              Icons.open_in_new,
              size: 16,
              color: Colors.grey,
            ),
            onTap: () async {
              final url = Uri.parse('https://tally.so/r/ODbEoA');
              if (await canLaunchUrl(url)) {
                await launchUrl(url, mode: LaunchMode.externalApplication);
              }
            },
          ),

          const Divider(height: 32),

          // --- ABOUT & INFO SECTION ---
          Padding(
            padding: const EdgeInsets.symmetric(horizontal: 16.0),
            child: Card(
              color: Theme.of(
                context,
              ).colorScheme.surfaceContainerHighest.withValues(alpha: 0.5),
              elevation: 0,
              shape: RoundedRectangleBorder(
                borderRadius: BorderRadius.circular(16),
              ),
              child: Column(
                children: [
                  const Padding(
                    padding: EdgeInsets.only(top: 24.0, bottom: 8.0),
                    child: CircleAvatar(
                      radius: 30,
                      backgroundColor: Colors.white,
                      foregroundImage: AssetImage('assets/logo.png'),
                    ),
                  ),
                  Text(
                    l10n.appTitle,
                    style: const TextStyle(
                      fontSize: 20,
                      fontWeight: FontWeight.bold,
                    ),
                  ),
                  FutureBuilder<String>(
                    future: _appVersionFuture,
                    builder: (context, snapshot) {
                      // Fallback if loading, then inject into the localized string
                      final rawVersion = snapshot.data ?? '...';
                      return Text(
                        l10n.settingsLabelVersion(rawVersion),
                        style: const TextStyle(
                          color: Colors.grey,
                          fontSize: 12,
                        ),
                      );
                    },
                  ),
                  const SizedBox(height: 16),

                  ListTile(
                    leading: const Icon(Icons.person, color: Colors.deepOrange),
                    title: Text(l10n.settingsLabelBuiltBy),
                    subtitle: Text(l10n.settingsLabelMaintainer),
                  ),
                  ListTile(
                    leading: const Icon(Icons.favorite, color: Colors.red),
                    title: Text(l10n.settingsLabelDonate),
                    subtitle: Text(l10n.settingsLabelDonateSubtitle),
                    trailing: const Icon(
                      Icons.open_in_new,
                      size: 16,
                      color: Colors.grey,
                    ),
                    onTap: () async {
                      final url = Uri.parse(
                        'https://github.com/sponsors/Benji377',
                      );
                      if (await canLaunchUrl(url)) {
                        await launchUrl(
                          url,
                          mode: LaunchMode.externalApplication,
                        );
                      }
                    },
                  ),
                  ListTile(
                    leading: const Icon(Icons.code, color: Colors.blue),
                    title: Text(l10n.settingsLabelSourceCode),
                    subtitle: Text(l10n.settingsLabelSourceCodeSubtitle),
                    trailing: const Icon(
                      Icons.open_in_new,
                      size: 16,
                      color: Colors.grey,
                    ),
                    onTap: () async {
                      final url = Uri.parse(
                        'https://github.com/Benji377/Timety',
                      );
                      if (await canLaunchUrl(url)) {
                        await launchUrl(
                          url,
                          mode: LaunchMode.externalApplication,
                        );
                      }
                    },
                  ),
                  const SizedBox(height: 8),
                ],
              ),
            ),
          ),
        ],
      ),
    );
  }

  // --- UI HELPERS ---
  Future<String> _loadAppVersion() async {
    final packageInfo = await PackageInfo.fromPlatform();
    return packageInfo.version;
  }

  void _showNumberPickerDialog(
    String title,
    int currentValue,
    int min,
    int max,
    Function(int) onSave, {
    required String unit,
  }) {
    final l10n = AppLocalizations.of(context)!;
    int tempValue = currentValue;
    showDialog(
      context: context,
      builder: (context) => StatefulBuilder(
        builder: (context, setDialogState) {
          return AlertDialog(
            title: Text(title),
            content: Column(
              mainAxisSize: MainAxisSize.min,
              children: [
                Text(
                  '$tempValue $unit',
                  style: const TextStyle(
                    fontSize: 24,
                    fontWeight: FontWeight.bold,
                  ),
                ),
                Slider(
                  value: tempValue.toDouble(),
                  min: min.toDouble(),
                  max: max.toDouble(),
                  divisions: (max - min) ~/ 5,
                  onChanged: (val) =>
                      setDialogState(() => tempValue = val.toInt()),
                ),
              ],
            ),
            actions: [
              TextButton(
                onPressed: () => Navigator.pop(context),
                child: Text(l10n.commonLabelCancel),
              ),
              ElevatedButton(
                onPressed: () {
                  onSave(tempValue);
                  Navigator.pop(context);
                },
                child: Text(l10n.commonLabelSave),
              ),
            ],
          );
        },
      ),
    );
  }

  Widget _buildSectionHeader(String title) {
    return Padding(
      padding: const EdgeInsets.fromLTRB(24, 24, 24, 8),
      child: Text(
        title.toUpperCase(),
        style: const TextStyle(
          color: AppTheme.taskColor,
          fontWeight: FontWeight.bold,
          letterSpacing: 1.2,
          fontSize: 12,
        ),
      ),
    );
  }

  DropdownMenuItem<String> _buildLangItem(
    String val,
    String text, {
    String? asset,
    bool isSystem = false,
  }) {
    return DropdownMenuItem(
      value: val,
      child: Row(
        mainAxisSize: MainAxisSize.min,
        children: [
          if (asset != null)
            ClipRRect(
              borderRadius: BorderRadius.circular(2),
              child: 
                SvgPicture.asset(
                  asset,
                  width: 24,
                  height: 16,
                  fit: BoxFit.cover,
                  semanticsLabel: "$text flag",
                ),
            )

          else
            const SizedBox(width: 24),
          const SizedBox(width: 12),
          Text(text),
        ],
      ),
    );
  }
}
