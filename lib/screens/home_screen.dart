import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import '../l10n/app_localizations.dart';
import '../providers/user_provider.dart';
import '../data/habit/habit_models.dart';
import '../providers/habit_provider.dart';
import '../data/task/task.dart';
import '../providers/task_provider.dart';
import '../providers/focus_provider.dart';
import '../providers/settings_provider.dart';
import '../theme/app_theme.dart';
import '../utils/habit/habit_utils.dart';
import '../widgets/focus/interactive_gauge.dart';
import '../widgets/habit/grouped_habits_section.dart';
import '../widgets/list_tiles/habit_list_tile.dart';
import '../widgets/list_tiles/task_list_tile.dart';
import 'calendar_screen.dart';
import 'statistics_screen.dart';
import 'task/task_detail_screen.dart';
import 'habit/habit_detail_screen.dart';
import '../widgets/common/styled_expansion_tile.dart';

/// The main dashboard screen showing today's overview, goals, and upcoming tasks.
class HomeScreen extends StatelessWidget {
  final VoidCallback onNavigateToFocus;

  const HomeScreen({super.key, required this.onNavigateToFocus});

  @override
  Widget build(BuildContext context) {
    final userName = context.watch<UserProvider>().name;
    final focusProvider = context.watch<FocusProvider>();
    final taskProvider = context.watch<TaskProvider>();
    final habitProvider = context.watch<HabitProvider>();
    final settings = context.watch<SettingsProvider>();

    final l10n = AppLocalizations.of(context)!;

    final int focusMinsToday = focusProvider.getMinutesFocusedToday();
    final int dailyTarget = settings.dailyGoalMins;
    final double focusProgress = (focusMinsToday / dailyTarget).clamp(0.0, 1.0);
    final today = DateTime.now();
    final todayDate = DateTime(today.year, today.month, today.day);
    final List<Habit> todaysHabits = habitProvider.getHabitsForDay(today).where(
      (habit) {
        final completionsThisWeek = habitProvider.getCompletionsThisWeek(
          habit,
          includeToday: false,
        );
        final targetDays = habit.targetDaysPerWeek;

        return targetDays == null || completionsThisWeek < targetDays;
      },
    ).toList();

    // Urgent Tasks
    final List<Task> urgentTasks = taskProvider.tasks.where((task) {
      if (task.isCompleted || task.dueDate == null) return false;
      final dueDay = DateTime(
        task.dueDate!.year,
        task.dueDate!.month,
        task.dueDate!.day,
      );
      return dueDay.isBefore(today) || dueDay.isAtSameMomentAs(today);
    }).toList();
    urgentTasks.sort((a, b) => a.dueDate!.compareTo(b.dueDate!));

    final upcomingWindowDays = settings.upcomingTasksDays;
    final upcomingEndDate = todayDate.add(Duration(days: upcomingWindowDays));
    final List<Task> upcomingTasks = taskProvider.tasks.where((task) {
      if (task.isCompleted || task.dueDate == null) return false;

      final dueDay = DateTime(
        task.dueDate!.year,
        task.dueDate!.month,
        task.dueDate!.day,
      );

      return dueDay.isAfter(todayDate) &&
          (dueDay.isBefore(upcomingEndDate) ||
              dueDay.isAtSameMomentAs(upcomingEndDate));
    }).toList();
    upcomingTasks.sort((a, b) => a.dueDate!.compareTo(b.dueDate!));

    return Scaffold(
      appBar: AppBar(
        title: Text(l10n.appTitle),
        actions: [
          IconButton(
            icon: const Icon(Icons.bar_chart),
            tooltip: l10n.commonTooltipStats,
            onPressed: () {
              Navigator.push(
                context,
                MaterialPageRoute(
                  builder: (context) =>
                      const StatisticsScreen(),
                ),
              );
            },
          ),
          IconButton(
            icon: const Icon(Icons.calendar_today),
            tooltip: l10n.commonTooltipCalendar,
            onPressed: () {
              Navigator.push(
                context,
                MaterialPageRoute(builder: (context) => const CalendarScreen()),
              );
            },
          ),
        ],
      ),
      body: SafeArea(
        child: Column(
          children: [
            // --- GREETING & MOTIVATION SECTION ---
            Padding(
              padding: const EdgeInsets.all(AppTheme.spaceXLarge),
              child: SizedBox(
                width: double.infinity,
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Text(
                      _getGreeting(l10n, userName),
                      style: const TextStyle(
                        fontSize: AppTheme.fsHeadingLarge,
                        fontWeight: AppTheme.fwExtraBold,
                      ),
                    ),
                    const SizedBox(height: 8),
                    Text(
                      _getDailyMotivationText(l10n),
                      style: const TextStyle(
                        fontSize: AppTheme.fsBodyLarge,
                        fontWeight: AppTheme.fwBold,
                        color: AppTheme.taskColor,
                      ),
                    ),
                  ],
                ),
              ),
            ),
            // --- DAILY GOAL GAUGE SECTION ---
            Expanded(
              flex: 5,
              child: Center(
                child: GestureDetector(
                  onTap: onNavigateToFocus,
                  child: InteractiveGauge(
                    progress: focusProgress,
                    isInteractive: false,
                    label: l10n.homeDailyGoal.toUpperCase(),
                    centerText: "${(focusProgress * 100).toInt()}%",
                    centerTextColor: AppTheme.focusColor,
                    color: AppTheme.focusColor,
                    bottomText: "$focusMinsToday / $dailyTarget m",
                    bottomTextColor: AppTheme.focusColor,
                  ),
                ),
              ),
            ),
            const SizedBox(height: 16),
            const Divider(height: 1),
            // --- TASKS & HABITS LIST SECTION ---
            Expanded(
              flex: 6,
              child: Material(
                color: Theme.of(
                  context,
                ).colorScheme.surfaceContainerHighest.withValues(alpha: 0.3),
                child: (urgentTasks.isEmpty && todaysHabits.isEmpty)
                    ? Center(child: Text(l10n.homeDailyGoalDone))
                    : ListView(
                        padding: const EdgeInsets.only(
                          top: AppTheme.spaceLarge,
                          bottom: 80,
                        ),
                        children: [
                          // --- DUE TASKS ACCORDION ---
                          if (urgentTasks.isNotEmpty) ...[
                            StyledExpansionTile(
                              initiallyExpanded: true,
                              title: Text(
                                l10n.homeSectionTasksDue(urgentTasks.length),
                                style: const TextStyle(
                                  fontWeight: AppTheme.fwBold,
                                  color: AppTheme.warningColor,
                                ),
                              ),
                              iconColor: AppTheme.warningColor,
                              children: [
                                ...urgentTasks.map(
                                  (task) => TaskListTile(
                                    task: task,
                                    isOverdue:
                                        task.dueDate != null &&
                                        task.dueDate!.isBefore(today),
                                    enableDismissible: false,
                                    showDescription: false,
                                    onToggleCompleted: () =>
                                        context.read<TaskProvider>().toggleTask(
                                          task.id,
                                          userProvider: context
                                              .read<UserProvider>(),
                                        ),
                                    onTap: () => Navigator.push(
                                      context,
                                      MaterialPageRoute(
                                        builder: (_) =>
                                            TaskDetailScreen(task: task),
                                      ),
                                    ),
                                  ),
                                ),
                                const SizedBox(height: AppTheme.spaceSmall),
                              ],
                            ),
                          ],
                          // --- TODAY'S HABITS ACCORDION ---
                          if (todaysHabits.isNotEmpty) ...[
                            StyledExpansionTile(
                              title: Text(
                                l10n.homeSectionHabitsDue(todaysHabits.length),
                                style: const TextStyle(
                                  fontWeight: AppTheme.fwBold,
                                  color: AppTheme.typeHabitColor,
                                ),
                              ),
                              iconColor: AppTheme.typeHabitColor,
                              children: [
                                GroupedHabitsSection(
                                  habits: todaysHabits,
                                  habitProvider: habitProvider,
                                  targetDate: today,
                                  habitBuilder:
                                      (habit, isDone, isStacked, isLocked) {
                                        return HabitListTile(
                                          habit: habit,
                                          isCompleted: isDone,
                                          isStacked: isStacked,
                                          isLocked: isLocked,
                                          enableDismissible: false,
                                          subtitleText:
                                              HabitUtils.buildHabitSubtitle(
                                                habit,
                                                l10n,
                                                habitProvider
                                                    .getCompletionsThisWeek(
                                                      habit,
                                                    ),
                                              ),
                                          onToggleCompleted: () => habitProvider
                                              .toggleCompletionToday(
                                                habit,
                                                userProvider: context
                                                    .read<UserProvider>(),
                                              ),
                                          onTap: () => Navigator.push(
                                            context,
                                            MaterialPageRoute(
                                              builder: (_) => HabitDetailScreen(
                                                habit: habit,
                                              ),
                                            ),
                                          ),
                                        );
                                      },
                                ),
                                const SizedBox(height: AppTheme.spaceSmall),
                              ],
                            ),
                          ],
                          // --- UPCOMING TASKS ACCORDION ---
                          if (upcomingTasks.isNotEmpty) ...[
                            StyledExpansionTile(
                              title: Text(
                                l10n.homeSectionTasksUpcoming(
                                  upcomingTasks.length,
                                ),
                                style: const TextStyle(
                                  fontWeight: AppTheme.fwBold,
                                  color: AppTheme.typeTaskColor,
                                ),
                              ),
                              iconColor: AppTheme.typeTaskColor,
                              children: [
                                ...upcomingTasks.map(
                                  (task) => TaskListTile(
                                    task: task,
                                    enableDismissible: false,
                                    showDescription: false,
                                    onToggleCompleted: () =>
                                        context.read<TaskProvider>().toggleTask(
                                          task.id,
                                          userProvider: context
                                              .read<UserProvider>(),
                                        ),
                                    onTap: () => Navigator.push(
                                      context,
                                      MaterialPageRoute(
                                        builder: (_) =>
                                            TaskDetailScreen(task: task),
                                      ),
                                    ),
                                  ),
                                ),
                                const SizedBox(height: AppTheme.spaceSmall),
                              ],
                            ),
                          ],
                        ],
                      ),
              ),
            ),
          ],
        ),
      ),
      floatingActionButton: FloatingActionButton(
        heroTag: "home_fab",
        onPressed: () => Navigator.push(
          context,
          MaterialPageRoute(builder: (context) => const TaskDetailScreen()),
        ),
        child: const Icon(Icons.add),
      ),
    );
  }

  // Generates a short contextual greeting based on time of day.
  String _getGreeting(AppLocalizations l10n, String name) {
    final hour = DateTime.now().hour;

    if (hour < 5) return l10n.greetingDeepNight(name);
    if (hour < 12) return l10n.greetingMorning(name);
    if (hour < 17) return l10n.greetingAfternoon(name);
    if (hour < 21) return l10n.greetingEvening(name);
    return l10n.greetingNight(name);
  }

  /// Returns a short home-screen title based on the time of day.
  String _getDailyMotivationText(AppLocalizations l10n) {
    final hour = DateTime.now().hour;

    if (hour < 5) return l10n.greetingDeepNightMotivation;
    if (hour < 12) return l10n.greetingMorningMotivation;
    if (hour < 17) return l10n.greetingAfternoonMotivation;
    if (hour < 21) return l10n.greetingEveningMotivation;
    return l10n.greetingNightMotivation;
  }
}
