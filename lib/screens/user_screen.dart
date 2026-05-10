import 'dart:io';
import 'package:flutter/material.dart';
import 'package:path_provider/path_provider.dart';
import 'package:provider/provider.dart';
import 'package:image_picker/image_picker.dart';
import 'package:share_plus/share_plus.dart';
import 'package:timety/providers/habit_provider.dart';
import 'package:timety/providers/user_provider.dart';
import 'package:timety/utils/xp_calculator.dart';
import 'package:timety/utils/wrapup_image_generator.dart';
import '../providers/task_provider.dart';
import '../providers/focus_provider.dart';
import '../theme/app_theme.dart';
import '../utils/streak_calculator.dart';
import '../widgets/stat_cards.dart';
import 'statistics_screen.dart';

class UserScreen extends StatefulWidget {
  const UserScreen({super.key});

  @override
  State<UserScreen> createState() => _UserScreenState();
}

class _UserScreenState extends State<UserScreen> {
  bool _isExporting = false;

  Future<void> _shareWrapUp(
    String name,
    int level,
    String title,
    int tasks,
    int habits,
    int focus,
    int streak,
  ) async {
    if (_isExporting) return;
    setState(() => _isExporting = true);

    try {
      // Wrap-up image generation
      final pngBytes = await WrapUpImageGenerator.generate(
        name: name,
        level: level,
        levelTitle: title,
        streak: streak,
        tasksCompleted: tasks,
        focusMins: focus,
        habitsMet: habits,
      );

      final directory = await getTemporaryDirectory();
      final file = File('${directory.path}/timety_wrap_up.png');
      await file.writeAsBytes(pngBytes);

      await SharePlus.instance.share(
        ShareParams(
          subject: 'My Timety Wrap-Up',
          files: [XFile(file.path)],
          text: 'Master your time with Timety!',
        ),
      );
    } catch (e) {
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          const SnackBar(content: Text('Failed to generate wrap-up image.')),
        );
      }
    } finally {
      if (mounted) setState(() => _isExporting = false);
    }
  }

  // --- IMAGE PICKER ---
  Future<void> _pickImage(UserProvider user) async {
    final picker = ImagePicker();
    final pickedFile = await picker.pickImage(source: ImageSource.gallery);

    if (pickedFile != null && mounted) {
      user.updateProfileImage(pickedFile.path);
    }
  }

  // --- NAME EDITOR ---
  void _editName(BuildContext context, UserProvider user) {
    final controller = TextEditingController(text: user.name);
    showDialog(
      context: context,
      builder: (context) => AlertDialog(
        title: const Text("Edit Name"),
        content: TextField(
          controller: controller,
          autofocus: true,
          maxLength: 20,
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
                user.updateName(controller.text.trim());
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
    final userProvider = context.watch<UserProvider>();
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
    final nextLevelXp = ExperienceEngine.getXpForLevel(
      userProvider.currentLevel + 1,
    );
    final xpToNextLevel = nextLevelXp - userProvider.totalXp;
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
          // --- SHARE BUTTON ---
          IconButton(
            icon: _isExporting
                ? const SizedBox(
                    width: 20,
                    height: 20,
                    child: CircularProgressIndicator(strokeWidth: 2),
                  )
                : const Icon(Icons.ios_share),
            tooltip: 'Share Weekly Wrap-up',
            onPressed: () => _shareWrapUp(
              userProvider.name,
              userProvider.currentLevel,
              userProvider.levelTitle,
              totalTasks,
              totalHabitsDone,
              totalFocusMins,
              currentStreak,
            ),
          ),
          IconButton(
            icon: const Icon(Icons.bar_chart),
            tooltip: 'Statistics',
            onPressed: () => Navigator.push(
              context,
              MaterialPageRoute(builder: (_) => const StatisticsScreen()),
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
              onTap: () => _pickImage(userProvider),
              child: Stack(
                alignment: Alignment.bottomRight,
                children: [
                  CircleAvatar(
                    radius: 60,
                    backgroundColor: AppTheme.userColor.withValues(alpha: 0.15),
                    backgroundImage: userProvider.profileImagePath != null
                        ? FileImage(File(userProvider.profileImagePath!))
                        : null,
                    child: userProvider.profileImagePath == null
                        ? const Icon(
                            Icons.person,
                            size: AppTheme.profileImageSize,
                            color: AppTheme.userColor,
                          )
                        : null,
                  ),
                  Container(
                    padding: const EdgeInsets.all(AppTheme.spaceSmall),
                    decoration: BoxDecoration(
                      color: AppTheme.userColor,
                      shape: BoxShape.circle,
                      border: Border.all(
                        color: Theme.of(context).scaffoldBackgroundColor,
                        width: 3,
                      ),
                    ),
                    child: const Icon(
                      Icons.camera_alt,
                      size: AppTheme.iconSizeMedium,
                      color: Colors.white,
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
                  userProvider.name,
                  style: const TextStyle(
                    fontSize: AppTheme.fsLargeNumber,
                    fontWeight: AppTheme.fwBold,
                  ),
                ),
                IconButton(
                  icon: const Icon(
                    Icons.edit,
                    size: AppTheme.iconSizeMedium,
                    color: AppTheme.wifiOffColor,
                  ),
                  onPressed: () => _editName(context, userProvider),
                ),
              ],
            ),

            // --- LEVEL & TITLE ---
            Text(
              "Level ${userProvider.currentLevel} • ${userProvider.levelTitle}",
              style: TextStyle(
                fontSize: AppTheme.fsBodyLarge,
                color: Theme.of(context).colorScheme.primary,
                fontWeight: FontWeight.bold,
              ),
            ),

            const SizedBox(height: AppTheme.spaceXLarge),

            // --- ELIXIR XP BAR ---
            Padding(
              padding: const EdgeInsets.symmetric(
                horizontal: AppTheme.space2XLarge,
              ),
              child: Column(
                children: [
                  Row(
                    mainAxisAlignment: MainAxisAlignment.spaceBetween,
                    children: [
                      Text(
                        "${userProvider.totalXp} XP",
                        style: const TextStyle(
                          fontWeight: FontWeight.bold,
                          color: Colors.grey,
                        ),
                      ),
                      Row(
                        mainAxisSize: MainAxisSize.min,
                        children: [
                          const Icon(
                            Icons.arrow_upward,
                            color: AppTheme.taskColor,
                            size: 18,
                          ),
                          const SizedBox(width: 4),
                          Text(
                            '$xpToNextLevel XP',
                            style: const TextStyle(
                              fontWeight: FontWeight.bold,
                              color: Colors.grey,
                            ),
                          ),
                        ],
                      ),
                    ],
                  ),
                  const SizedBox(height: 8),
                  ClipRRect(
                    borderRadius: BorderRadius.circular(10),
                    child: LinearProgressIndicator(
                      value: userProvider.levelProgress,
                      minHeight: 12,
                      backgroundColor: Theme.of(
                        context,
                      ).colorScheme.surfaceContainerHighest,
                      color: AppTheme.taskColor,
                    ),
                  ),
                ],
              ),
            ),

            const SizedBox(height: AppTheme.space2XLarge),

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
                    : Theme.of(context).colorScheme.surfaceContainerHighest
                          .withValues(alpha: AppTheme.opacityVeryLight),
                borderRadius: AppTheme.brCircle,
                border: Border.all(
                  color: currentStreak > 0
                      ? AppTheme.warningColor.withValues(
                          alpha: AppTheme.opacityLight,
                        )
                      : Theme.of(context).dividerColor,
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
                        : Theme.of(context).colorScheme.onSurfaceVariant,
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
                          : Theme.of(context).colorScheme.onSurfaceVariant,
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
                    color: AppTheme.taskColor,
                  ),
                  CompactHeaderStatCard(
                    title: "Habits Met",
                    value: "$totalHabitsDone",
                    icon: Icons.repeat,
                    color: AppTheme.habitColor,
                  ),
                  CompactHeaderStatCard(
                    title: "Focus Mins",
                    value: "$totalFocusMins",
                    icon: Icons.timer_outlined,
                    color: AppTheme.focusColor,
                  ),
                  CompactHeaderStatCard(
                    title: "Sessions",
                    value: "$totalSessions",
                    icon: Icons.coffee_outlined,
                    color: AppTheme.userColor,
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
