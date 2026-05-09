import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import 'package:url_launcher/url_launcher.dart';
import '../providers/settings_provider.dart';
import '../services/backup_service.dart';

class SettingsScreen extends StatefulWidget {
  const SettingsScreen({super.key});

  @override
  State<SettingsScreen> createState() => _SettingsScreenState();
}

class _SettingsScreenState extends State<SettingsScreen> {
  // --- UI HELPERS ---
  void _showNumberPickerDialog(
    String title,
    int currentValue,
    int min,
    int max,
    Function(int) onSave,
  ) {
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
                  "$tempValue minutes",
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
        style: TextStyle(
          color: Theme.of(context).colorScheme.primary,
          fontWeight: FontWeight.bold,
          letterSpacing: 1.2,
          fontSize: 12,
        ),
      ),
    );
  }

  @override
  Widget build(BuildContext context) {
    // Watch the provider here!
    final settings = context.watch<SettingsProvider>();

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
          ListTile(
            leading: const Icon(Icons.palette_outlined),
            title: const Text('Accent Color'),
            trailing: Row(
              mainAxisSize: MainAxisSize.min,
              children:
                  [Colors.blue, Colors.green, Colors.purple, Colors.orange].map(
                    (color) {
                      final isSelected =
                          settings.seedColor.toARGB32() == color.toARGB32();
                      return GestureDetector(
                        onTap: () => settings.setSeedColor(color),
                        child: Container(
                          margin: const EdgeInsets.symmetric(horizontal: 4),
                          width: 24,
                          height: 24,
                          decoration: BoxDecoration(
                            color: color,
                            shape: BoxShape.circle,
                            border: isSelected
                                ? Border.all(color: Colors.white, width: 2)
                                : null,
                            boxShadow: isSelected
                                ? [
                                    const BoxShadow(
                                      color: Colors.black26,
                                      blurRadius: 4,
                                    ),
                                  ]
                                : null,
                          ),
                        ),
                      );
                    },
                  ).toList(),
            ),
          ),

          const Divider(height: 32),

          // --- FOCUS & PRODUCTIVITY ---
          _buildSectionHeader('Focus & Productivity'),
          ListTile(
            leading: const Icon(Icons.track_changes),
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
          ListTile(
            leading: const Icon(Icons.timer_outlined),
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
            leading: const Icon(Icons.linear_scale),
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

          const Divider(height: 32),

          // --- NOTIFICATIONS ---
          _buildSectionHeader('Notifications'),
          SwitchListTile(
            secondary: const Icon(Icons.notifications_active_outlined),
            title: const Text('Daily Motivation'),
            subtitle: const Text('Get a reminder to crush your goals'),
            value: settings.notificationsEnabled,
            activeThumbColor: Theme.of(context).colorScheme.primary,
            onChanged: (val) => settings.setNotificationsEnabled(val),
          ),
          ListTile(
            enabled: settings.notificationsEnabled,
            leading: const Icon(Icons.schedule),
            title: const Text('Notification Time'),
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

          // --- DATA & BACKUP ---
          _buildSectionHeader('Data & Backup'),
          ListTile(
            leading: const Icon(
              Icons.cloud_upload_outlined,
              color: Colors.blue,
            ),
            title: const Text('Export Backup'),
            subtitle: const Text(
              'Save your data locally or share it to the cloud',
            ),
            onTap: () => BackupService.exportBackup(context),
          ),
          ListTile(
            leading: const Icon(Icons.restore, color: Colors.orange),
            title: const Text('Restore Backup'),
            subtitle: const Text(
              'Overwrite current data from a backup zip file',
            ),
            onTap: () => BackupService.importBackup(context),
          ),

          const SizedBox(height: 32),

          // --- ABOUT & INFO SECTION ---
          Padding(
            padding: const EdgeInsets.symmetric(horizontal: 16.0),
            child: Card(
              color: Theme.of(
                context,
              ).colorScheme.surfaceContainerHighest.withValues(alpha: 0.3),
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
                      backgroundColor: Colors.blue,
                      child: Icon(
                        Icons.rocket_launch,
                        size: 30,
                        color: Colors.white,
                      ),
                    ),
                  ),
                  const Text(
                    "Timety",
                    style: TextStyle(fontSize: 20, fontWeight: FontWeight.bold),
                  ),
                  const Text(
                    "Version 1.1.1",
                    style: TextStyle(color: Colors.grey, fontSize: 12),
                  ),
                  const SizedBox(height: 16),
                  ListTile(
                    leading: const Icon(Icons.person_outline),
                    title: const Text(
                      'Built by Benji377',
                    ), // Change to your name!
                    subtitle: const Text('Solo Developer & Maintainer'),
                  ),
                  ListTile(
                    leading: const Icon(Icons.favorite),
                    title: const Text('Donate'),
                    subtitle: const Text('GitHub Sponsors'),
                    trailing: const Icon(
                      Icons.open_in_new,
                      size: 16,
                      color: Colors.grey,
                    ),
                    onTap: () {
                      // Add url_launcher logic here: launchUrl(Uri.parse('mailto:support@timety.app'));
                      launchUrl(
                        Uri.parse('https://github.com/sponsors/Benji377'),
                      );
                    },
                  ),
                  ListTile(
                    leading: const Icon(Icons.code),
                    title: const Text('Source Code'),
                    subtitle: const Text('GitHub Repository'),
                    trailing: const Icon(
                      Icons.open_in_new,
                      size: 16,
                      color: Colors.grey,
                    ),
                    onTap: () {
                      // Add url_launcher logic here: launchUrl(Uri.parse('https://github.com/yourusername/timety'));
                      launchUrl(
                        Uri.parse('https://github.com/Benji377/timety'),
                      );
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
