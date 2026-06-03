import 'package:flutter/material.dart';
import 'package:package_info_plus/package_info_plus.dart';
import 'package:provider/provider.dart';
import 'package:url_launcher/url_launcher.dart';
import '../theme/app_theme.dart';
import '../providers/settings_provider.dart';
import '../providers/focus_provider.dart';
import '../providers/task_provider.dart';
import '../services/backup_service.dart';
import '../widgets/tags.dart';
import '../widgets/categories.dart';
import '../widgets/location_picker_dialog.dart';

class SettingsScreen extends StatefulWidget {
  const SettingsScreen({super.key});

  @override
  State<SettingsScreen> createState() => _SettingsScreenState();
}

class _SettingsScreenState extends State<SettingsScreen> {
  late final Future<String> _appVersionFuture = _loadAppVersion();

  // --- UI HELPERS ---
  Future<String> _loadAppVersion() async {
    final packageInfo = await PackageInfo.fromPlatform();
    return 'Version ${packageInfo.version}';
  }

  void _showNumberPickerDialog(
    String title,
    int currentValue,
    int min,
    int max,
    Function(int) onSave, {
    String unit = 'minutes',
  }) {
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
                child: const Text("Cancel"),
              ),
              ElevatedButton(
                onPressed: () {
                  onSave(tempValue);
                  Navigator.pop(context);
                },
                child: const Text("Save"),
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

  @override
  Widget build(BuildContext context) {
    final settings = context.watch<SettingsProvider>();
    final focusProvider = context.watch<FocusProvider>();
    final taskProvider = context.watch<TaskProvider>();

    return Scaffold(
      appBar: AppBar(title: const Text('Settings'), centerTitle: true),
      body: ListView(
        padding: const EdgeInsets.only(bottom: 40),
        children: [
          // --- APPEARANCE ---
          _buildSectionHeader('Appearance'),
          ListTile(
            leading: const Icon(Icons.dark_mode_outlined),
            title: const Text('Theme'),
            trailing: DropdownButton<ThemeMode>(
              value: settings.themeMode,
              underline: const SizedBox(),
              items: const [
                DropdownMenuItem(value: ThemeMode.light, child: Text('Light')),
                DropdownMenuItem(value: ThemeMode.dark, child: Text('Dark')),
                DropdownMenuItem(
                  value: ThemeMode.system,
                  child: Text('System Default'),
                ),
              ],
              onChanged: (val) {
                if (val != null) settings.setThemeMode(val);
              },
            ),
          ),

          const Divider(height: 32),

          // --- LOCALIZATION & FORMATTING ---
          _buildSectionHeader('Localization & Formatting'),
          ListTile(
            leading: const Icon(Icons.language, color: AppTheme.taskColor),
            title: const Text('Language'),
            trailing: DropdownButton<String>(
              value: settings.appLocaleCode,
              underline: const SizedBox(),
              items: const [
                DropdownMenuItem(
                  value: 'system',
                  child: Text('System Default'),
                ),
                DropdownMenuItem(value: 'en', child: Text('English')),
                DropdownMenuItem(value: 'de', child: Text('Deutsch')),
                DropdownMenuItem(value: 'it', child: Text('Italiano')),
                DropdownMenuItem(value: 'lld', child: Text('Ladin')),
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
            title: const Text('Use 24-Hour Time'),
            value: settings.use24HourFormat,
            onChanged: settings.set24HourFormat,
          ),
          ListTile(
            leading: const Icon(
              Icons.calendar_today,
              color: AppTheme.habitColor,
            ),
            title: const Text('Date Format'),
            trailing: DropdownButton<String>(
              value: settings.dateFormatCode,
              underline: const SizedBox(),
              items: const [
                DropdownMenuItem(
                  value: 'system',
                  child: Text('System Default'),
                ),
                DropdownMenuItem(
                  value: 'dd/MM/yyyy',
                  child: Text('Day/Month/Year'),
                ),
                DropdownMenuItem(
                  value: 'MM/dd/yyyy',
                  child: Text('Month/Day/Year'),
                ),
                DropdownMenuItem(
                  value: 'yyyy-MM-dd',
                  child: Text('Year-Month-Day'),
                ),
                DropdownMenuItem(
                  value: 'dd.MM.yyyy',
                  child: Text('Day.Month.Year'),
                ),
              ],
              onChanged: (val) {
                if (val != null) settings.setDateFormatCode(val);
              },
            ),
          ),

          const Divider(height: 32),

          // --- FOCUS & PRODUCTIVITY ---
          _buildSectionHeader('Focus & Productivity'),
          ListTile(
            leading: const Icon(
              Icons.track_changes,
              color: AppTheme.focusColor,
            ),
            title: const Text('Daily Focus Goal'),
            subtitle: Text('${settings.dailyGoalMins} minutes'),
            trailing: const Icon(Icons.chevron_right),
            onTap: () => _showNumberPickerDialog(
              "Daily Goal",
              settings.dailyGoalMins,
              10,
              480,
              (val) => settings.setDailyGoal(val),
            ),
          ),
          SwitchListTile(
            secondary: const Icon(Icons.task_alt, color: AppTheme.taskColor),
            title: const Text('Auto-complete linked task or habit'),
            subtitle: const Text(
              'Marks the selected task or habit as complete when a focus timer finishes.',
            ),
            value: settings.autoCompleteFocusTargetOnFinish,
            onChanged: settings.setAutoCompleteFocusTargetOnFinish,
          ),
          ListTile(
            leading: const Icon(
              Icons.timer_outlined,
              color: AppTheme.warningAccent,
            ),
            title: const Text('Max Stopwatch Limit'),
            subtitle: Text(
              'Prevents accidentally leaving timer on\nCurrently: ${settings.maxStopwatchMins} mins',
            ),
            trailing: const Icon(Icons.chevron_right),
            onTap: () => _showNumberPickerDialog(
              "Stopwatch Limit",
              settings.maxStopwatchMins,
              30,
              480,
              (val) => settings.setMaxStopwatch(val),
            ),
          ),
          ListTile(
            leading: const Icon(Icons.linear_scale, color: AppTheme.taskColor),
            title: const Text('Max Phase Node Time'),
            subtitle: Text(
              'Maximum length for a single focus block\nCurrently: ${settings.maxNodeMins} mins',
            ),
            trailing: const Icon(Icons.chevron_right),
            onTap: () => _showNumberPickerDialog(
              "Max Node Time",
              settings.maxNodeMins,
              10,
              480,
              (val) => settings.setMaxNode(val),
            ),
          ),

          ListTile(
            leading: const Icon(
              Icons.schedule_outlined,
              color: AppTheme.taskColor,
            ),
            title: const Text('Upcoming Task Window'),
            subtitle: Text('${settings.upcomingTasksDays} days ahead'),
            trailing: const Icon(Icons.chevron_right),
            onTap: () => _showNumberPickerDialog(
              'Upcoming Task Window',
              settings.upcomingTasksDays,
              1,
              60,
              (val) => settings.setUpcomingTasksDays(val),
              unit: 'days',
            ),
          ),

          const Divider(height: 32),

          // --- TAGS & CATEGORIES ---
          _buildSectionHeader('Organization'),
          ListTile(
            leading: const Icon(
              Icons.local_offer_outlined,
              color: AppTheme.focusColor,
            ),
            title: const Text('Focus Tags'),
            subtitle: Text('${focusProvider.tags.length} tags'),
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
            title: const Text('Task Categories'),
            subtitle: Text(
              '${taskProvider.getAllCategories().length} categories',
            ),
            trailing: const Icon(Icons.chevron_right),
            onTap: () => Navigator.push(
              context,
              MaterialPageRoute(builder: (_) => const CategoriesWidget()),
            ),
          ),

          const Divider(height: 32),

          // --- API & SERVICES ---
          _buildSectionHeader('API & Services'),
          ListTile(
            leading: const Icon(Icons.cloud, color: AppTheme.taskColor),
            title: const Text('Location Search API'),
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
          _buildSectionHeader('Notifications'),
          ListTile(
            leading: const Icon(Icons.schedule, color: AppTheme.warningAccent),
            title: const Text('Daily Motivation Time'),
            subtitle: Text(settings.notificationTime.format(context)),
            trailing: const Icon(Icons.edit),
            onTap: () async {
              final TimeOfDay? time = await showTimePicker(
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
            title: const Text('End of Day Checkup Time'),
            subtitle: Text(settings.endOfDayTime.format(context)),
            trailing: const Icon(Icons.edit),
            onTap: () async {
              final TimeOfDay? time = await showTimePicker(
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
          _buildSectionHeader('Data & Backup'),
          ListTile(
            leading: const Icon(
              Icons.upload_file_outlined,
              color: AppTheme.taskColor,
            ),
            title: const Text('Export JSON Data'),
            subtitle: const Text('Save a snapshot of your data'),
            trailing: const Icon(Icons.chevron_right),
            onTap: () => BackupService.exportUserData(context),
          ),
          ListTile(
            leading: const Icon(
              Icons.download_outlined,
              color: AppTheme.focusColor,
            ),
            title: const Text('Import JSON Data'),
            subtitle: const Text('Restore data from a JSON export file'),
            trailing: const Icon(Icons.chevron_right),
            onTap: () => BackupService.importUserData(context),
          ),

          const Divider(height: 32),

          // --- SUPPORT & FEEDBACK ---
          _buildSectionHeader('Support & Feedback'),
          ListTile(
            leading: const Icon(
              Icons.forum_outlined,
              color: AppTheme.focusColor,
            ),
            title: const Text('Community & Help'),
            subtitle: const Text('Ask questions and share tips on GitHub'),
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
            title: const Text('Send Feedback'),
            subtitle: const Text('Report bugs or request new features'),
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
                  const Text(
                    "Timety",
                    style: TextStyle(fontSize: 20, fontWeight: FontWeight.bold),
                  ),
                  FutureBuilder<String>(
                    future: _appVersionFuture,
                    builder: (context, snapshot) {
                      final versionText = snapshot.data ?? 'Version';
                      return Text(
                        versionText,
                        style: const TextStyle(
                          color: Colors.grey,
                          fontSize: 12,
                        ),
                      );
                    },
                  ),
                  const SizedBox(height: 16),

                  const ListTile(
                    leading: Icon(Icons.person, color: Colors.deepOrange),
                    title: Text('Built by Benji377'),
                    subtitle: Text('Maintainer'),
                  ),
                  ListTile(
                    leading: const Icon(Icons.favorite, color: Colors.red),
                    title: const Text('Donate'),
                    subtitle: const Text('GitHub Sponsors'),
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
                    title: const Text('Source Code'),
                    subtitle: const Text('GitHub Repository'),
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
}
