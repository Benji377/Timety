import 'dart:io';
import 'package:flutter/material.dart';
import 'package:path_provider/path_provider.dart';
import 'package:provider/provider.dart';
import 'package:image_picker/image_picker.dart';
import 'package:share_plus/share_plus.dart';
import '../providers/habit_provider.dart';
import '../providers/user_provider.dart';
import '../utils/assets/wrapup_image_utils.dart';
import '../providers/task_provider.dart';
import '../providers/focus_provider.dart';
import '../theme/app_theme.dart';
import '../utils/logic/streak_utils.dart';
import '../widgets/stat_cards/stat_cards.dart';
import '../data/other/stat_card_style.dart';
import '../widgets/user_profile/streak_status_badge.dart';
import '../widgets/user_profile/user_streak_timeline_card.dart';
import '../widgets/user_profile/user_xp_breakdown_card.dart';
import './statistics_screen.dart';

class UserScreen extends StatefulWidget {
  const UserScreen({super.key});

  @override
  State<UserScreen> createState() => _UserScreenState();
}

class _UserScreenState extends State<UserScreen> {
  bool _isExporting = false;

  @override
  Widget build(BuildContext context) {
    final userProvider = context.watch<UserProvider>();
    final tasks = context.watch<TaskProvider>();
    final focus = context.watch<FocusProvider>();
    final habits = context.watch<HabitProvider>();

    final taskDates = tasks.tasks
        .where((task) => task.isCompleted && task.completedAt != null)
        .map((task) => task.completedAt!)
        .toList();
    final focusDates = focus.history
        .where((session) => session.totalSecondsFocused >= 60)
        .map((session) => session.startTime)
        .toList();
    final habitDates = habits.habits
        .expand((habit) => habit.completions)
        .toList();
    final activityDates = [...taskDates, ...focusDates, ...habitDates];

    final totalTasks = taskDates.length;
    final totalSessions = focus.history.length;
    final totalFocusMins = focus.history.fold(
      0,
      (sum, s) => sum + (s.totalSecondsFocused ~/ 60),
    );
    final totalHabitsDone = habitDates.length;
    final streaks = StreakCalculator.calculateBoth(activityDates);

    final currentStreak = streaks.current;
    final highestStreak = streaks.highest;
    final isStreakActive =
        currentStreak > 0 &&
            taskDates.any(
              (date) =>
                  date.year == DateTime.now().year &&
                  date.month == DateTime.now().month &&
                  date.day == DateTime.now().day,
            ) ||
        focusDates.any(
          (date) =>
              date.year == DateTime.now().year &&
              date.month == DateTime.now().month &&
              date.day == DateTime.now().day,
        ) ||
        habitDates.any(
          (date) =>
              date.year == DateTime.now().year &&
              date.month == DateTime.now().month &&
              date.day == DateTime.now().day,
        );

    return Scaffold(
      appBar: AppBar(
        title: const Text('Profile'),
        actions: [
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
              mainAxisSize: MainAxisSize.min,
              children: [
                StreakStatusBadge(isActive: isStreakActive),
                const SizedBox(width: 10),
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

            const SizedBox(height: AppTheme.spaceLarge),

            UserXpBreakdownCard(
              currentLevel: userProvider.currentLevel,
              levelTitle: userProvider.levelTitle,
              totalXp: userProvider.totalXp,
              levelProgress: userProvider.levelProgress,
            ),

            const SizedBox(height: AppTheme.spaceLarge),

            UserStreakTimelineCard(
              activityDates: activityDates,
              taskDates: taskDates,
              focusDates: focusDates,
              habitDates: habitDates,
              currentStreak: currentStreak,
              highestStreak: highestStreak,
            ),

            const SizedBox(height: AppTheme.spaceLarge),

            _buildAllTimeStatsSection(
              context,
              totalTasks,
              totalHabitsDone,
              totalFocusMins,
              totalSessions,
              highestStreak,
            ),

            const SizedBox(height: AppTheme.space3XLarge),
          ],
        ),
      ),
    );
  }

  Widget _buildAllTimeStatsSection(
    BuildContext context,
    int totalTasks,
    int totalHabitsDone,
    int totalFocusMins,
    int totalSessions,
    int highestStreak,
  ) {
    return Card(
      elevation: 0,
      shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(24)),
      clipBehavior: Clip.antiAlias,
      child: Container(
        padding: const EdgeInsets.all(20),
        decoration: BoxDecoration(
          gradient: LinearGradient(
            begin: Alignment.topLeft,
            end: Alignment.bottomRight,
            colors: [
              Theme.of(
                context,
              ).colorScheme.surfaceContainerHighest.withValues(alpha: 0.42),
              Theme.of(context).colorScheme.surface,
            ],
          ),
        ),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            const Text(
              'All-Time Stats',
              textAlign: TextAlign.left,
              style: TextStyle(
                fontSize: AppTheme.fsHeadingSmall,
                fontWeight: AppTheme.fwBold,
              ),
            ),
            const SizedBox(height: 4),
            Text(
              'Lifetime progress at a glance',
              style: TextStyle(
                fontSize: 12,
                color: Theme.of(context).colorScheme.onSurfaceVariant,
              ),
            ),
            const SizedBox(height: 16),
            Wrap(
              spacing: AppTheme.spaceMedium,
              runSpacing: AppTheme.spaceMedium,
              children: [
                StatCard(
                  title: 'Tasks Done',
                  value: '$totalTasks',
                  icon: Icons.check_circle_outline,
                  color: AppTheme.taskColor,
                  style: StatCardStyle.compactVertical,
                ),
                StatCard(
                  title: 'Habits Met',
                  value: '$totalHabitsDone',
                  icon: Icons.repeat,
                  color: AppTheme.habitColor,
                  style: StatCardStyle.compactVertical,
                ),
                StatCard(
                  title: 'Focus Mins',
                  value: '$totalFocusMins',
                  icon: Icons.timer_outlined,
                  color: AppTheme.focusColor,
                  style: StatCardStyle.compactVertical,
                ),
                StatCard(
                  title: 'Sessions',
                  value: '$totalSessions',
                  icon: Icons.coffee_outlined,
                  color: AppTheme.userColor,
                  style: StatCardStyle.compactVertical,
                ),
                StatCard(
                  title: 'Best Streak',
                  value: '$highestStreak',
                  icon: Icons.military_tech,
                  color: AppTheme.warningColor,
                  style: StatCardStyle.compactVertical,
                ),
              ],
            ),
          ],
        ),
      ),
    );
  }

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
  Future<void> _editName(BuildContext context, UserProvider user) async {
    final controller = TextEditingController(text: user.name);
    await showDialog(
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
}
