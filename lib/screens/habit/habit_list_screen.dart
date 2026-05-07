import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import 'package:timety/screens/statistics_screen.dart';
import '../../data/habit/habit_models.dart';
import '../../providers/habit_provider.dart';
import '../../theme/app_theme.dart';
import '../../widgets/app_dialogs.dart';
import '../calendar_screen.dart';
import 'habit_detail_screen.dart';

class HabitListScreen extends StatelessWidget {
  const HabitListScreen({super.key});

  // --- INDIVIDUAL HABIT TILE ---
  Widget _buildHabitTile(
    BuildContext context,
    Habit habit,
    HabitProvider provider,
    bool isDoneToday,
  ) {
    final color = Color(habit.colorValue);

    // Subtitle logic
    String subtitle = "";
    if (habit.frequency == HabitFrequency.daily) subtitle = "Daily";
    if (habit.frequency == HabitFrequency.weeklyExact) {
      subtitle = "Specific Days";
    }
    if (habit.frequency == HabitFrequency.weeklyFlexible) {
      int doneThisWeek = provider.getCompletionsThisWeek(habit);
      subtitle = "$doneThisWeek / ${habit.targetDaysPerWeek} this week";
    }
    if (habit.targetTime != null) {
      subtitle += " • ${habit.targetTime!.format(context)}";
    }

    return Dismissible(
      key: Key(habit.id),
      background: Container(
        color: AppTheme.errorColor,
        alignment: Alignment.centerRight,
        padding: const EdgeInsets.only(right: AppTheme.spaceXLarge),
        margin: const EdgeInsets.symmetric(
          horizontal: AppTheme.spaceSmall,
          vertical: AppTheme.spaceXSmall,
        ),
        child: const Icon(Icons.delete, color: Colors.white),
      ),
      direction: DismissDirection.endToStart,
      onDismissed: (_) => provider.deleteHabit(habit.id),
      confirmDismiss: (_) async =>
          await AppDialogs.showConfirmation(
            context: context,
            title: 'Delete Habit',
            content: 'Are you sure you want to delete this habit?',
          ) ??
          false,
      child: Card(
        margin: const EdgeInsets.symmetric(
          horizontal: AppTheme.spaceSmall,
          vertical: AppTheme.spaceXSmall,
        ),
        elevation: 0,
        shape: RoundedRectangleBorder(
          side: BorderSide(
            color: isDoneToday
                ? color.withValues(alpha: AppTheme.opacityLight)
                : color,
            width: 2,
          ),
          borderRadius: AppTheme.brMedium,
        ),
        child: ListTile(
          leading: InkWell(
            onTap: () => provider.toggleCompletionToday(habit),
            borderRadius: AppTheme.brCircle,
            child: Container(
              width: 28,
              height: 28,
              decoration: BoxDecoration(
                color: isDoneToday ? color : Colors.transparent,
                shape: BoxShape.circle,
                border: Border.all(color: color, width: 2),
              ),
              child: isDoneToday
                  ? const Icon(Icons.check, size: 18, color: Colors.white)
                  : null,
            ),
          ),
          title: Row(
            children: [
              // Show habit icon (fallback to circle) colored by habit
              Icon(
                habit.iconData ?? Icons.circle,
                size: 18,
                color: isDoneToday ? Colors.grey : color,
              ),
              const SizedBox(width: AppTheme.spaceSmall),
              Expanded(
                child: Text(
                  habit.name,
                  style: TextStyle(
                    fontWeight: AppTheme.fwBold,
                    decoration: isDoneToday ? TextDecoration.lineThrough : null,
                    color: isDoneToday ? Colors.grey : null,
                  ),
                ),
              ),
            ],
          ),
          subtitle: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              if (habit.notes != null && habit.notes!.isNotEmpty)
                Text(
                  habit.notes!,
                  maxLines: 1,
                  overflow: TextOverflow.ellipsis,
                  style: const TextStyle(fontSize: AppTheme.fsLabel),
                ),
              Text(
                subtitle,
                style: const TextStyle(
                  fontSize: AppTheme.fsLabel,
                  color: Colors.grey,
                ),
              ),
              if (habit.frequency == HabitFrequency.weeklyFlexible &&
                  !isDoneToday)
                Padding(
                  padding: const EdgeInsets.only(top: AppTheme.spaceXSmall),
                  child: LinearProgressIndicator(
                    value:
                        provider.getCompletionsThisWeek(habit) /
                        (habit.targetDaysPerWeek ?? 1),
                    backgroundColor: color.withValues(
                      alpha: AppTheme.opacityVeryLight,
                    ),
                    color: color,
                    borderRadius: AppTheme.brSmall,
                  ),
                ),
            ],
          ),
          onTap: () {
            Navigator.push(
              context,
              MaterialPageRoute(
                builder: (_) =>
                    HabitDetailScreen(habit: habit, isEditing: true),
              ),
            );
          },
        ),
      ),
    );
  }

  // --- ACCORDION BUILDER ---
  Widget _buildAccordion(
    BuildContext context,
    String title,
    Color color,
    List<Habit> habits,
    HabitProvider provider, {
    bool initExpanded = true,
    bool isDone = false,
  }) {
    if (habits.isEmpty) return const SizedBox.shrink();

    return Theme(
      data: Theme.of(context).copyWith(dividerColor: Colors.transparent),
      child: ExpansionTile(
        initiallyExpanded: initExpanded,
        iconColor: color,
        collapsedIconColor: color,
        title: Row(
          children: [
            Icon(Icons.circle, size: AppTheme.fsBodySmall, color: color),
            const SizedBox(width: AppTheme.spaceSmall),
            Text(
              "$title (${habits.length})",
              style: TextStyle(fontWeight: AppTheme.fwBold, color: color),
            ),
          ],
        ),
        children: habits
            .map((h) => _buildHabitTile(context, h, provider, isDone))
            .toList(),
      ),
    );
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: const Text('My Habits'),
        actions: [
          IconButton(
            icon: const Icon(Icons.bar_chart),
            tooltip: 'Insights',
            onPressed: () {
              Navigator.push(
                context,
                MaterialPageRoute(
                  builder: (context) => const StatisticsScreen(),
                ),
              );
            },
          ),
          IconButton(
            icon: const Icon(Icons.calendar_today),
            tooltip: 'Calendar View',
            onPressed: () {
              Navigator.push(
                context,
                MaterialPageRoute(builder: (context) => const CalendarScreen()),
              );
            },
          ),
        ],
      ),
      body: Consumer<HabitProvider>(
        builder: (context, provider, child) {
          if (provider.habits.isEmpty) {
            return const Center(
              child: Text("No habits yet! Tap + to build a routine."),
            );
          }

          final today = DateTime.now();
          final todoToday = <Habit>[];
          final doneToday = <Habit>[];
          final weeklyGoalsMet = <Habit>[];

          for (var habit in provider.habits) {
            bool isDoneToday = provider.isCompletedOn(habit, today);

            if (isDoneToday) {
              doneToday.add(habit);
              continue;
            }

            // Grouping Logic for "To Do"
            if (habit.frequency == HabitFrequency.daily) {
              todoToday.add(habit);
            } else if (habit.frequency == HabitFrequency.weeklyExact) {
              if (habit.targetWeekdays?.contains(today.weekday) ?? false) {
                todoToday.add(habit);
              }
            } else if (habit.frequency == HabitFrequency.weeklyFlexible) {
              int doneThisWeek = provider.getCompletionsThisWeek(habit);
              if (doneThisWeek >= (habit.targetDaysPerWeek ?? 1)) {
                weeklyGoalsMet.add(habit);
              } else {
                todoToday.add(habit); // Keep showing it until goal is met!
              }
            }
          }

          return ListView(
            padding: const EdgeInsets.only(bottom: 80),
            children: [
              _buildAccordion(
                context,
                "To Do Today",
                AppTheme.infoColor,
                todoToday,
                provider,
              ),
              _buildAccordion(
                context,
                "Done Today",
                AppTheme.successColor,
                doneToday,
                provider,
                isDone: true,
              ),
              _buildAccordion(
                context,
                "Weekly Goal Met",
                AppTheme.warningColor,
                weeklyGoalsMet,
                provider,
                initExpanded: false,
              ),
            ],
          );
        },
      ),
      floatingActionButton: FloatingActionButton(
        heroTag: "habit_list_add_button",
        onPressed: () => Navigator.push(
          context,
          MaterialPageRoute(builder: (_) => const HabitDetailScreen()),
        ),
        child: const Icon(Icons.add),
      ),
    );
  }
}
