import 'dart:io';
import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import 'package:image_picker/image_picker.dart';
import 'package:timety/providers/habit_provider.dart';
import '../providers/task_provider.dart';
import '../providers/focus_provider.dart';
import '../providers/settings_provider.dart';
import '../theme/app_theme.dart';
import '../utils/streak_calculator.dart';
import '../widgets/stat_cards.dart';
import 'statistics_screen.dart';
import 'settings_screen.dart';

class UserScreen extends StatefulWidget {
  const UserScreen({super.key});

  @override
  State<UserScreen> createState() => _UserScreenState();
}

class _UserScreenState extends State<UserScreen> {
  // --- IMAGE PICKER ---
  Future<void> _pickImage(SettingsProvider settings) async {
    final picker = ImagePicker();
    final pickedFile = await picker.pickImage(source: ImageSource.gallery);

    if (pickedFile != null && mounted) {
      settings.setProfileImagePath(pickedFile.path);
    }
  }

  // --- NAME EDITOR ---
  void _editName(BuildContext context, SettingsProvider settings) {
    final controller = TextEditingController(text: settings.userName);
    showDialog(
      context: context,
      builder: (context) => AlertDialog(
        title: const Text("Edit Name"),
        content: TextField(
          controller: controller,
          autofocus: true,
          decoration: const InputDecoration(hintText: "Enter your name"),
          textCapitalization: TextCapitalization.words,
        ),
        actions: [
          TextButton(
            onPressed: () => Navigator.pop(context),
            child: const Text("Cancel"),
          ),
          ElevatedButton(
            onPressed: () {
              if (controller.text.trim().isNotEmpty) {
                settings.setUserName(controller.text.trim());
              }
              Navigator.pop(context);
            },
            child: const Text("Save"),
          ),
        ],
      ),
    );
  }

  @override
  Widget build(BuildContext context) {
    final settings = context.watch<SettingsProvider>();
    final tasks = context.watch<TaskProvider>();
    final focus = context.watch<FocusProvider>();
    final habits = context.watch<HabitProvider>();

    // Compute stats
    final totalTasks = tasks.tasks.where((t) => t.isCompleted).length;
    final totalSessions = focus.history.length;
    final totalFocusMins = focus.history.fold(
      0,
      (sum, s) => sum + (s.totalSecondsFocused ~/ 60),
    );
    final totalHabitsDone = habits.habits.fold(
      0,
      (sum, h) => sum + h.completions.length,
    );
    final activityDates = <DateTime>[
      ...tasks.tasks
          .where((t) => t.isCompleted && t.completedAt != null)
          .map((t) => t.completedAt!),
      ...focus.history
          .where((s) => s.totalSecondsFocused >= 60)
          .map((s) => s.startTime),
      ...habits.habits.expand((h) => h.completions),
    ];
    final streaks = StreakCalculator.calculateBoth(activityDates);

    final currentStreak = streaks.current;
    final highestStreak = streaks.highest;

    return Scaffold(
      appBar: AppBar(
        title: const Text('Profile'),
        actions: [
          IconButton(
            icon: const Icon(Icons.bar_chart),
            tooltip: 'Statistics',
            onPressed: () => Navigator.push(
              context,
              MaterialPageRoute(builder: (_) => const StatisticsScreen()),
            ),
          ),
          IconButton(
            icon: const Icon(Icons.settings),
            tooltip: 'Settings',
            onPressed: () => Navigator.push(
              context,
              MaterialPageRoute(builder: (_) => const SettingsScreen()),
            ),
          ),
        ],
      ),
      body: SingleChildScrollView(
        child: Column(
          children: [
            const SizedBox(height: AppTheme.space2XLarge),

            // --- PROFILE HEADER ---
            GestureDetector(
              onTap: () => _pickImage(settings),
              child: Stack(
                alignment: Alignment.bottomRight,
                children: [
                  CircleAvatar(
                    radius: 60,
                    backgroundColor: Theme.of(
                      context,
                    ).colorScheme.primaryContainer,
                    backgroundImage: settings.profileImagePath != null
                        ? FileImage(File(settings.profileImagePath!))
                        : null,
                    child: settings.profileImagePath == null
                        ? Icon(
                            Icons.person,
                            size: AppTheme.profileImageSize,
                            color: Theme.of(context).colorScheme.primary,
                          )
                        : null,
                  ),
                  Container(
                    padding: const EdgeInsets.all(AppTheme.spaceSmall),
                    decoration: BoxDecoration(
                      color: Theme.of(context).colorScheme.primary,
                      shape: BoxShape.circle,
                      border: Border.all(
                        color: Theme.of(context).scaffoldBackgroundColor,
                        width: 3,
                      ),
                    ),
                    child: Icon(
                      Icons.camera_alt,
                      size: AppTheme.iconSizeMedium,
                      color: Theme.of(context).colorScheme.onPrimary,
                    ),
                  ),
                ],
              ),
            ),
            const SizedBox(height: AppTheme.spaceXLarge),

            Row(
              mainAxisAlignment: MainAxisAlignment.center,
              children: [
                Text(
                  settings.userName,
                  style: const TextStyle(
                    fontSize: AppTheme.fsLargeNumber,
                    fontWeight: AppTheme.fwBold,
                  ),
                ),
                IconButton(
                  icon: const Icon(
                    Icons.edit,
                    size: AppTheme.iconSizeMedium,
                    color: Colors.grey,
                  ),
                  onPressed: () => _editName(context, settings),
                ),
              ],
            ),

            const SizedBox(height: AppTheme.spaceLarge),

            // --- STREAK BADGE ---
            Container(
              padding: const EdgeInsets.symmetric(
                horizontal: AppTheme.spaceXLarge,
                vertical: AppTheme.spaceMedium,
              ),
              decoration: BoxDecoration(
                color: currentStreak > 0
                    ? AppTheme.warningColor.withValues(
                        alpha: AppTheme.opacityVeryLight,
                      )
                    : Colors.grey.withValues(alpha: AppTheme.opacityVeryLight),
                borderRadius: AppTheme.brCircle,
                border: Border.all(
                  color: currentStreak > 0
                      ? AppTheme.warningColor.withValues(
                          alpha: AppTheme.opacityLight,
                        )
                      : Colors.grey.shade300,
                ),
              ),
              child: Row(
                mainAxisSize: MainAxisSize.min,
                children: [
                  Icon(
                    currentStreak > 0 ? Icons.whatshot : Icons.heart_broken,
                    size: 24,
                    color: currentStreak > 0
                        ? AppTheme.warningColor
                        : Colors.grey.shade600,
                  ),
                  const SizedBox(width: AppTheme.spaceSmall),
                  Text(
                    currentStreak > 0
                        ? "$currentStreak Day Streak!"
                        : "Start a streak today!",
                    style: TextStyle(
                      fontSize: AppTheme.fsBodyLarge,
                      fontWeight: AppTheme.fwBold,
                      color: currentStreak > 0
                          ? AppTheme.warningColor
                          : Colors.grey.shade600,
                    ),
                  ),
                ],
              ),
            ),

            const SizedBox(height: AppTheme.space2XLarge),
            const Divider(
              indent: AppTheme.space2XLarge,
              endIndent: AppTheme.space2XLarge,
            ),
            const SizedBox(height: AppTheme.spaceXLarge),

            // --- COMPACT TOTALS OVERVIEW ---
            const Text(
              "All-Time Stats",
              style: TextStyle(
                fontSize: AppTheme.fsHeadingSmall,
                fontWeight: AppTheme.fwBold,
              ),
            ),
            const SizedBox(height: AppTheme.spaceLarge),

            // Replaced GridView with a centered Wrap
            Padding(
              padding: const EdgeInsets.symmetric(
                horizontal: AppTheme.spaceLarge,
              ),
              child: Wrap(
                alignment: WrapAlignment.center,
                spacing: AppTheme.spaceMedium,
                runSpacing: AppTheme.spaceMedium,
                children: [
                  CompactHeaderStatCard(
                    title: "Tasks Done",
                    value: "$totalTasks",
                    icon: Icons.check_circle_outline,
                    color: Colors.blue,
                  ),
                  CompactHeaderStatCard(
                    title: "Habits Met",
                    value: "$totalHabitsDone",
                    icon: Icons.repeat,
                    color: Colors.purple,
                  ),
                  CompactHeaderStatCard(
                    title: "Focus Mins",
                    value: "$totalFocusMins",
                    icon: Icons.timer_outlined,
                    color: Colors.green,
                  ),
                  CompactHeaderStatCard(
                    title: "Sessions",
                    value: "$totalSessions",
                    icon: Icons.coffee_outlined,
                    color: Colors.brown,
                  ),
                  CompactHeaderStatCard(
                    title: "Best Streak",
                    value: "$highestStreak",
                    icon: Icons.military_tech,
                    color: AppTheme.warningColor,
                  ),
                ],
              ),
            ),
            const SizedBox(height: AppTheme.space3XLarge),
          ],
        ),
      ),
    );
  }
}
