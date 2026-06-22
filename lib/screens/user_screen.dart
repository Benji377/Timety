import 'dart:io';
import 'package:flutter/material.dart';
import 'package:path_provider/path_provider.dart';
import 'package:provider/provider.dart';
import 'package:image_picker/image_picker.dart';
import 'package:share_plus/share_plus.dart';
import '../l10n/app_localizations.dart';
import '../providers/habit_provider.dart';
import '../providers/user_provider.dart';
import '../utils/ui/wrapup_image_generator.dart';
import '../providers/task_provider.dart';
import '../providers/focus_provider.dart';
import '../theme/app_theme.dart';
import '../utils/stats/streak_calculator.dart';
import '../widgets/stats/stat_card.dart';
import '../widgets/user_profile/streak_status_badge.dart';
import '../widgets/user_profile/user_streak_timeline_card.dart';
import '../widgets/user_profile/user_xp_breakdown_card.dart';
import 'settings_screen.dart';

/// Displays the user's profile, XP progress, and all-time statistics.
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
    final l10n = AppLocalizations.of(context)!;

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
        title: Text(l10n.userProfileTitle),
        actions: [
          IconButton(
            icon: _isExporting
                ? const SizedBox(
                    width: 20,
                    height: 20,
                    child: CircularProgressIndicator(strokeWidth: 2),
                  )
                : const Icon(Icons.share),
            tooltip: l10n.userTooltipShareWrapUp,
            onPressed: () => _shareWrapUp(
              context,
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
            icon: const Icon(Icons.settings),
            onPressed: () => Navigator.push(
              context,
              MaterialPageRoute(builder: (context) => const SettingsScreen()),
            ),
          ),
        ],
      ),
      body: SingleChildScrollView(
        child: Column(
          children: [
            const SizedBox(height: AppTheme.space2XLarge),

            // --- AVATAR & NAME SECTION ---
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

            // --- XP BREAKDOWN SECTION ---
            UserXpBreakdownCard(
              currentLevel: userProvider.currentLevel,
              levelTitle: userProvider.levelTitle,
              totalXp: userProvider.totalXp,
              levelProgress: userProvider.levelProgress,
            ),

            const SizedBox(height: AppTheme.spaceLarge),

            // --- STREAK TIMELINE SECTION ---
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
    final l10n = AppLocalizations.of(context)!;

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
            Text(
              l10n.userStatsAllTimeTitle,
              textAlign: TextAlign.left,
              style: const TextStyle(
                fontSize: AppTheme.fsHeadingSmall,
                fontWeight: AppTheme.fwBold,
              ),
            ),
            const SizedBox(height: 4),
            Text(
              l10n.userStatsAllTimeSubtitle,
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
                  title: l10n.userStatTasksDone,
                  value: '$totalTasks',
                  icon: Icons.check_circle_outline,
                  color: AppTheme.taskColor,
                  style: StatCardStyle.compactVertical,
                ),
                StatCard(
                  title: l10n.userStatHabitsMet,
                  value: '$totalHabitsDone',
                  icon: Icons.repeat,
                  color: AppTheme.habitColor,
                  style: StatCardStyle.compactVertical,
                ),
                StatCard(
                  title: l10n.userStatFocusMins,
                  value: '$totalFocusMins',
                  icon: Icons.timer_outlined,
                  color: AppTheme.focusColor,
                  style: StatCardStyle.compactVertical,
                ),
                StatCard(
                  title: l10n.userStatSessions,
                  value: '$totalSessions',
                  icon: Icons.coffee_outlined,
                  color: AppTheme.userColor,
                  style: StatCardStyle.compactVertical,
                ),
                StatCard(
                  title: l10n.userStatBestStreak,
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
    BuildContext context,
    String name,
    int level,
    String title,
    int tasks,
    int habits,
    int focus,
    int streak,
  ) async {
    if (_isExporting) return;

    // Grab localized strings before async gaps
    final l10n = AppLocalizations.of(context)!;
    final subjectString = l10n.userShareSubject;
    final textString = l10n.userShareText;
    final errorString = l10n.userErrorGenerateImage;

    setState(() => _isExporting = true);

    try {
      // Wrap-up image generation
      final pngBytes = await WrapUpImageGenerator.generate(
        l10n: l10n,
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
          subject: subjectString,
          files: [XFile(file.path)],
          text: textString,
        ),
      );
    } catch (e) {
      if (context.mounted) {
        ScaffoldMessenger.of(
          context,
        ).showSnackBar(SnackBar(content: Text(errorString)));
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
    final l10n = AppLocalizations.of(context)!;

    await showDialog(
      context: context,
      builder: (context) => AlertDialog(
        title: Text(l10n.userEditNameTitle),
        content: TextField(
          controller: controller,
          autofocus: true,
          maxLength: 20,
          decoration: InputDecoration(hintText: l10n.userEditNameHint),
          textCapitalization: TextCapitalization.words,
        ),
        actions: [
          TextButton(
            onPressed: () => Navigator.pop(context),
            child: Text(l10n.commonLabelCancel),
          ),
          ElevatedButton(
            onPressed: () {
              if (controller.text.trim().isNotEmpty) {
                user.updateName(controller.text.trim());
              }
              Navigator.pop(context);
            },
            child: Text(l10n.commonLabelSave),
          ),
        ],
      ),
    );
  }
}
