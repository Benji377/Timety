// lib/screens/user_screen.dart
import 'dart:io';
import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import 'package:image_picker/image_picker.dart';
import 'package:timety/providers/habit_provider.dart';
import '../providers/task_provider.dart';
import '../providers/focus_provider.dart';
import '../providers/settings_provider.dart';
import '../theme/app_theme.dart';
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

  // --- STREAK ALGORITHM ---
  Map<String, int> _calculateStreaks(
    TaskProvider taskProv,
    FocusProvider focusProv,
    HabitProvider habitProv,
  ) {
    Set<String> activeDates = {};
    String formatDate(DateTime dt) =>
        "${dt.year}-${dt.month.toString().padLeft(2, '0')}-${dt.day.toString().padLeft(2, '0')}";

    for (var task in taskProv.tasks) {
      if (task.isCompleted && task.completedAt != null) {
        activeDates.add(formatDate(task.completedAt!));
      }
    }

    for (var session in focusProv.history) {
      if (session.totalSecondsFocused >= 60) {
        activeDates.add(formatDate(session.startTime));
      }
    }

    for (var habit in habitProv.habits) {
      for (var completion in habit.completions) {
        activeDates.add(formatDate(completion));
      }
    }

    if (activeDates.isEmpty) return {'current': 0, 'highest': 0};

    List<String> sortedDates = activeDates.toList()..sort();

    int highest = 1;
    int currentRun = 1;
    for (int i = 1; i < sortedDates.length; i++) {
      DateTime prev = DateTime.parse(sortedDates[i - 1]);
      DateTime curr = DateTime.parse(sortedDates[i]);

      if (curr.difference(prev).inDays == 1) {
        currentRun++;
        if (currentRun > highest) highest = currentRun;
      } else {
        currentRun = 1;
      }
    }

    int current = 0;
    DateTime checkDate = DateTime.now();

    if (activeDates.contains(formatDate(checkDate))) {
      // Keep checking
    } else if (activeDates.contains(
      formatDate(checkDate.subtract(const Duration(days: 1))),
    )) {
      checkDate = checkDate.subtract(const Duration(days: 1));
    } else {
      return {'current': 0, 'highest': highest};
    }

    while (activeDates.contains(formatDate(checkDate))) {
      current++;
      checkDate = checkDate.subtract(const Duration(days: 1));
    }

    return {'current': current, 'highest': highest};
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
    final streaks = _calculateStreaks(tasks, focus, habits);

    final currentStreak = streaks['current'] ?? 0;
    final highestStreak = streaks['highest'] ?? 0;

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
                  Text(
                    currentStreak > 0 ? "🔥" : "🩶",
                    style: const TextStyle(fontSize: 24),
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
                  _buildStatCard(
                    context,
                    "Tasks Done",
                    "$totalTasks",
                    Icons.check_circle_outline,
                    Colors.blue,
                  ),
                  _buildStatCard(
                    context,
                    "Habits Met",
                    "$totalHabitsDone",
                    Icons.repeat,
                    Colors.purple,
                  ),
                  _buildStatCard(
                    context,
                    "Focus Mins",
                    "$totalFocusMins",
                    Icons.timer_outlined,
                    Colors.green,
                  ),
                  _buildStatCard(
                    context,
                    "Sessions",
                    "$totalSessions",
                    Icons.coffee_outlined,
                    Colors.brown,
                  ),
                  _buildStatCard(
                    context,
                    "Best Streak",
                    "$highestStreak",
                    Icons.military_tech,
                    AppTheme.warningColor,
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

  // Updated to be a fixed-size compact box
  Widget _buildStatCard(
    BuildContext context,
    String title,
    String value,
    IconData icon,
    Color color,
  ) {
    return SizedBox(
      width: 140, // Fixed compact width
      height: 100, // Fixed compact height
      child: Card(
        elevation: 0,
        shape: RoundedRectangleBorder(
          side: BorderSide(
            color: color.withValues(alpha: AppTheme.opacityLight),
            width: 1.5,
          ),
          borderRadius: AppTheme.brXLarge,
        ),
        color: color.withValues(alpha: 0.05),
        child: Padding(
          padding: const EdgeInsets.all(AppTheme.spaceMedium),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.center,
            mainAxisAlignment: MainAxisAlignment.center,
            children: [
              Row(
                mainAxisAlignment: MainAxisAlignment.center,
                children: [
                  Icon(icon, color: color, size: AppTheme.iconSizeSmall),
                  const SizedBox(width: AppTheme.spaceXSmall),
                  Text(
                    title,
                    style: TextStyle(
                      fontSize: AppTheme.fsCaption,
                      color: Colors.grey.shade700,
                      fontWeight: AppTheme.fwBold,
                    ),
                  ),
                ],
              ),
              const Spacer(),
              Text(
                value,
                style: const TextStyle(
                  fontSize: AppTheme.fsHeadingMedium,
                  fontWeight: AppTheme.fwExtraBold,
                ),
              ),
            ],
          ),
        ),
      ),
    );
  }
}
