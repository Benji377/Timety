import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import 'package:timety/utils/utils.dart';
import '../theme/app_theme.dart';
import '../providers/task_provider.dart';
import 'task/task_detail_screen.dart';
import '../providers/focus_provider.dart';
import '../data/habit/habit_models.dart';
import '../providers/habit_provider.dart';

class CalendarScreen extends StatefulWidget {
  const CalendarScreen({super.key});

  @override
  State<CalendarScreen> createState() => _CalendarScreenState();
}

class _CalendarScreenState extends State<CalendarScreen> {
  DateTime _focusedMonth = DateTime.now();
  DateTime? _selectedDate;

  bool _isSameDay(DateTime? a, DateTime? b) {
    if (a == null || b == null) return false;
    return a.year == b.year && a.month == b.month && a.day == b.day;
  }

  // --- TIME HELPER ---
  String _formatTime(DateTime time) {
    return '${time.hour.toString().padLeft(2, '0')}:${time.minute.toString().padLeft(2, '0')}';
  }

  List<List<DateTime>> _generateWeeks(DateTime month) {
    final firstDayOfMonth = DateTime(month.year, month.month, 1);
    final lastDayOfMonth = DateTime(month.year, month.month + 1, 0);

    int offsetToMonday = firstDayOfMonth.weekday - DateTime.monday;
    DateTime currentDay = firstDayOfMonth.subtract(
      Duration(days: offsetToMonday),
    );

    List<List<DateTime>> weeks = [];

    while (currentDay.isBefore(lastDayOfMonth) ||
        currentDay.weekday != DateTime.monday) {
      List<DateTime> week = [];
      for (int i = 0; i < 7; i++) {
        week.add(currentDay);
        currentDay = currentDay.add(const Duration(days: 1));
      }
      weeks.add(week);
    }

    return weeks;
  }

  @override
  Widget build(BuildContext context) {
    final taskProvider = context.watch<TaskProvider>();
    final focusProvider = context.watch<FocusProvider>();
    final habitProvider = context.watch<HabitProvider>(); // <--- ADDED

    final allTasks = taskProvider.tasks;
    final allSessions = focusProvider.history;
    final allHabits = habitProvider.habits; // <--- ADDED

    final weeks = _generateWeeks(_focusedMonth);

    // --- FILTER & SORT ACCORDION DATA ---

    // 1. Tasks
    final selectedDayTasks = allTasks
        .where((t) => _isSameDay(t.dueDate, _selectedDate))
        .toList();
    selectedDayTasks.sort(
      (a, b) => b.priority.index.compareTo(a.priority.index),
    );

    // 2. Focus
    final selectedDaySessions = allSessions
        .where((s) => _isSameDay(s.startTime, _selectedDate))
        .toList();
    selectedDaySessions.sort((a, b) => a.startTime.compareTo(b.startTime));

    // 3. Habits (Combine habits scheduled for this day + habits actually completed on this day)
    List<Habit> selectedDayHabits = [];
    if (_selectedDate != null) {
      final scheduled = habitProvider.getHabitsForDay(_selectedDate!);
      final completed = allHabits
          .where((h) => habitProvider.isCompletedOn(h, _selectedDate!))
          .toList();
      selectedDayHabits = {
        ...scheduled,
        ...completed,
      }.toList(); // Use a Set spread to remove duplicates
    }

    return Scaffold(
      appBar: AppBar(
        title: const Text('Calendar'),
        actions: [
          IconButton(
            icon: const Icon(Icons.today),
            tooltip: 'Go to Today',
            onPressed: () {
              setState(() {
                _focusedMonth = DateTime.now();
                _selectedDate = DateTime.now();
              });
            },
          ),
        ],
      ),
      body: Column(
        children: [
          // --- TOP HALF: THE CALENDAR ---
          Expanded(
            flex: 1,
            child: Container(
              color: Theme.of(context).colorScheme.surface,
              padding: const EdgeInsets.all(16.0),
              child: Column(
                children: [
                  Row(
                    mainAxisAlignment: MainAxisAlignment.spaceBetween,
                    children: [
                      IconButton(
                        icon: const Icon(Icons.chevron_left),
                        onPressed: () => setState(
                          () => _focusedMonth = DateTime(
                            _focusedMonth.year,
                            _focusedMonth.month - 1,
                          ),
                        ),
                      ),
                      Text(
                        "${_monthName(_focusedMonth.month)} ${_focusedMonth.year}",
                        style: const TextStyle(
                          fontSize: 18,
                          fontWeight: FontWeight.bold,
                        ),
                      ),
                      IconButton(
                        icon: const Icon(Icons.chevron_right),
                        onPressed: () => setState(
                          () => _focusedMonth = DateTime(
                            _focusedMonth.year,
                            _focusedMonth.month + 1,
                          ),
                        ),
                      ),
                    ],
                  ),
                  const SizedBox(height: 16),

                  Expanded(
                    child: Table(
                      defaultVerticalAlignment:
                          TableCellVerticalAlignment.middle,
                      children: [
                        const TableRow(
                          children: [
                            _CalendarHeaderCell('M'),
                            _CalendarHeaderCell('T'),
                            _CalendarHeaderCell('W'),
                            _CalendarHeaderCell('T'),
                            _CalendarHeaderCell('F'),
                            _CalendarHeaderCell('S'),
                            _CalendarHeaderCell('S'),
                            _CalendarHeaderCell('Weekly', color: Colors.blue),
                          ],
                        ),
                        ...weeks.map((week) {
                          final weekStart = week.first.subtract(
                            const Duration(seconds: 1),
                          );
                          final weekEnd = week.last.add(
                            const Duration(days: 1),
                          );

                          // Weekly Analytics Calculations
                          int weeklyTaskCount = allTasks
                              .where(
                                (t) =>
                                    t.dueDate != null &&
                                    t.dueDate!.isAfter(weekStart) &&
                                    t.dueDate!.isBefore(weekEnd),
                              )
                              .length;

                          int weeklyFocusSeconds = allSessions
                              .where(
                                (s) =>
                                    s.startTime.isAfter(weekStart) &&
                                    s.startTime.isBefore(weekEnd),
                              )
                              .fold(0, (sum, s) => sum + s.totalSecondsFocused);
                          int weeklyFocusMins = weeklyFocusSeconds ~/ 60;

                          int weeklyHabitCount = allHabits.fold(0, (sum, h) {
                            return sum +
                                h.completions
                                    .where(
                                      (c) =>
                                          c.isAfter(weekStart) &&
                                          c.isBefore(weekEnd),
                                    )
                                    .length;
                          });

                          return TableRow(
                            children: [
                              ...week.map((day) {
                                final isCurrentMonth =
                                    day.month == _focusedMonth.month;
                                final isSelected = _isSameDay(
                                  day,
                                  _selectedDate,
                                );
                                final isToday = _isSameDay(day, DateTime.now());

                                final hasTasks = allTasks.any(
                                  (t) => _isSameDay(t.dueDate, day),
                                );
                                final hasFocus = allSessions.any(
                                  (s) => _isSameDay(s.startTime, day),
                                );
                                final hasHabits = allHabits.any(
                                  (h) => habitProvider.isCompletedOn(h, day),
                                );

                                return TableCell(
                                  child: GestureDetector(
                                    onTap: () =>
                                        setState(() => _selectedDate = day),
                                    child: Container(
                                      margin: const EdgeInsets.all(4),
                                      decoration: BoxDecoration(
                                        color: isSelected
                                            ? Colors.blue.withValues(alpha: 0.2)
                                            : Colors.transparent,
                                        border: isToday
                                            ? Border.all(
                                                color: Colors.blue,
                                                width: 2,
                                              )
                                            : null,
                                        borderRadius: AppTheme.brMedium,
                                      ),
                                      height: 45,
                                      child: Column(
                                        mainAxisAlignment:
                                            MainAxisAlignment.center,
                                        children: [
                                          Text(
                                            '${day.day}',
                                            style: TextStyle(
                                              fontWeight: isSelected || isToday
                                                  ? FontWeight.bold
                                                  : FontWeight.normal,
                                              color: isCurrentMonth
                                                  ? Theme.of(
                                                      context,
                                                    ).textTheme.bodyLarge?.color
                                                  : Colors.grey.shade400,
                                            ),
                                          ),
                                          const SizedBox(height: 2),
                                          // --- MICRO DOTS ---
                                          Row(
                                            mainAxisAlignment:
                                                MainAxisAlignment.center,
                                            children: [
                                              if (hasTasks)
                                                Container(
                                                  width: 5,
                                                  height: 5,
                                                  decoration:
                                                      const BoxDecoration(
                                                        color: Colors.blue,
                                                        shape: BoxShape.circle,
                                                      ),
                                                ),
                                              if (hasTasks &&
                                                  (hasFocus || hasHabits))
                                                const SizedBox(width: 2),

                                              if (hasFocus)
                                                Container(
                                                  width: 5,
                                                  height: 5,
                                                  decoration:
                                                      const BoxDecoration(
                                                        color: Colors.green,
                                                        shape: BoxShape.circle,
                                                      ),
                                                ),
                                              if (hasFocus && hasHabits)
                                                const SizedBox(width: 2),

                                              if (hasHabits)
                                                Container(
                                                  width: 5,
                                                  height: 5,
                                                  decoration:
                                                      const BoxDecoration(
                                                        color: Colors.purple,
                                                        shape: BoxShape.circle,
                                                      ),
                                                ),

                                              if (!hasTasks &&
                                                  !hasFocus &&
                                                  !hasHabits)
                                                const SizedBox(height: 5),
                                            ],
                                          ),
                                        ],
                                      ),
                                    ),
                                  ),
                                );
                              }),
                              // --- WEEKLY SUMMARY COLUMN ---
                              TableCell(
                                child: Column(
                                  mainAxisAlignment: MainAxisAlignment.center,
                                  children: [
                                    if (weeklyTaskCount > 0)
                                      Text(
                                        weeklyTaskCount.toString(),
                                        style: const TextStyle(
                                          fontWeight: FontWeight.bold,
                                          color: Colors.blue,
                                          fontSize: 12,
                                        ),
                                      ),
                                    if (weeklyFocusMins > 0)
                                      Text(
                                        '${weeklyFocusMins}m',
                                        style: const TextStyle(
                                          fontWeight: FontWeight.bold,
                                          color: Colors.green,
                                          fontSize: 12,
                                        ),
                                      ),
                                    if (weeklyHabitCount > 0)
                                      Row(
                                        mainAxisAlignment:
                                            MainAxisAlignment.center,
                                        mainAxisSize: MainAxisSize.min,
                                        children: [
                                          Text(
                                            '$weeklyHabitCount',
                                            style: const TextStyle(
                                              fontWeight: FontWeight.bold,
                                              color: Colors.purple,
                                              fontSize: 11,
                                            ),
                                          ),
                                          const SizedBox(width: 2),
                                          const Icon(
                                            Icons.repeat,
                                            color: Colors.purple,
                                            size: 12,
                                          ),
                                        ],
                                      ),
                                  ],
                                ),
                              ),
                            ],
                          );
                        }),
                      ],
                    ),
                  ),
                ],
              ),
            ),
          ),

          const Divider(height: 1, thickness: 1),

          // --- BOTTOM HALF: ACCORDION LISTS ---
          Expanded(
            flex: 1,
            child: Container(
              color: Theme.of(
                context,
              ).colorScheme.surfaceContainerHighest.withValues(alpha: 0.3),
              child: _selectedDate == null
                  ? const Center(
                      child: Text(
                        "Select a day to view details.",
                        style: TextStyle(color: Colors.grey),
                      ),
                    )
                  : ListView(
                      padding: const EdgeInsets.all(8),
                      children: [
                        // --- HABITS ACCORDION ---
                        ExpansionTile(
                          initiallyExpanded: true,
                          title: Text(
                            "Habits (${selectedDayHabits.length})",
                            style: const TextStyle(
                              fontWeight: FontWeight.bold,
                              color: Colors.purple,
                            ),
                          ),
                          iconColor: Colors.purple,
                          collapsedIconColor: Colors.purple,
                          children: selectedDayHabits.isEmpty
                              ? [
                                  const Padding(
                                    padding: EdgeInsets.all(16.0),
                                    child: Text(
                                      "No habits scheduled or logged.",
                                      style: TextStyle(color: Colors.grey),
                                    ),
                                  ),
                                ]
                              : selectedDayHabits.map((habit) {
                                  final isCompleted = habitProvider
                                      .isCompletedOn(habit, _selectedDate!);
                                  return Card(
                                    margin: const EdgeInsets.symmetric(
                                      horizontal: 8,
                                      vertical: 4,
                                    ),
                                    elevation: 0,
                                    shape: RoundedRectangleBorder(
                                      side: BorderSide(
                                        color: isCompleted
                                            ? Colors.purple.withValues(
                                                alpha: 0.3,
                                              )
                                            : Colors.purple,
                                        width: 1,
                                      ),
                                      borderRadius: BorderRadius.circular(8),
                                    ),
                                    child: ListTile(
                                      leading: Checkbox(
                                        value: isCompleted,
                                        activeColor: Colors.purple,
                                        onChanged: (_) {
                                          // Time-Travel Logging!
                                          if (isCompleted) {
                                            habit.completions.removeWhere(
                                              (c) =>
                                                  _isSameDay(c, _selectedDate!),
                                            );
                                          } else {
                                            // Keep the current time, but force the date to be the selected calendar date
                                            final now = DateTime.now();
                                            final retroDate = DateTime(
                                              _selectedDate!.year,
                                              _selectedDate!.month,
                                              _selectedDate!.day,
                                              now.hour,
                                              now.minute,
                                            );
                                            habit.completions.add(retroDate);
                                          }
                                          habitProvider.saveHabit(habit);
                                        },
                                      ),
                                      title: Text(
                                        habit.name,
                                        style: TextStyle(
                                          decoration: isCompleted
                                              ? TextDecoration.lineThrough
                                              : null,
                                          color: isCompleted
                                              ? Colors.grey
                                              : null,
                                        ),
                                      ),
                                    ),
                                  );
                                }).toList(),
                        ),

                        // --- TASKS ACCORDION ---
                        ExpansionTile(
                          initiallyExpanded: true,
                          title: Text(
                            "Tasks (${selectedDayTasks.length})",
                            style: const TextStyle(
                              fontWeight: FontWeight.bold,
                              color: Colors.blue,
                            ),
                          ),
                          iconColor: Colors.blue,
                          collapsedIconColor: Colors.blue,
                          children: selectedDayTasks.isEmpty
                              ? [
                                  const Padding(
                                    padding: EdgeInsets.all(16.0),
                                    child: Text(
                                      "No tasks scheduled.",
                                      style: TextStyle(color: Colors.grey),
                                    ),
                                  ),
                                ]
                              : selectedDayTasks.map((task) {
                                  return Card(
                                    margin: const EdgeInsets.symmetric(
                                      horizontal: 8,
                                      vertical: 4,
                                    ),
                                    elevation: 0,
                                    shape: RoundedRectangleBorder(
                                      side: BorderSide(
                                        color: task.isCompleted
                                            ? Colors.green
                                            : Colors.blue,
                                        width: 1,
                                      ),
                                      borderRadius: BorderRadius.circular(8),
                                    ),
                                    child: ListTile(
                                      leading: Checkbox(
                                        value: task.isCompleted,
                                        onChanged: (_) => context
                                            .read<TaskProvider>()
                                            .toggleTask(task.id),
                                      ),
                                      title: Text(
                                        task.title,
                                        style: TextStyle(
                                          decoration: task.isCompleted
                                              ? TextDecoration.lineThrough
                                              : null,
                                        ),
                                      ),
                                      trailing: AppUtils().getPriorityIcon(
                                        task.priority,
                                      ),
                                      onTap: () => Navigator.push(
                                        context,
                                        MaterialPageRoute(
                                          builder: (_) =>
                                              TaskDetailScreen(task: task),
                                        ),
                                      ),
                                    ),
                                  );
                                }).toList(),
                        ),

                        // --- FOCUS SESSIONS ACCORDION ---
                        ExpansionTile(
                          initiallyExpanded: false,
                          title: Text(
                            "Focus Sessions (${selectedDaySessions.length})",
                            style: const TextStyle(
                              fontWeight: FontWeight.bold,
                              color: Colors.green,
                            ),
                          ),
                          iconColor: Colors.green,
                          collapsedIconColor: Colors.green,
                          children: selectedDaySessions.isEmpty
                              ? [
                                  const Padding(
                                    padding: EdgeInsets.all(16.0),
                                    child: Text(
                                      "No focus sessions logged.",
                                      style: TextStyle(color: Colors.grey),
                                    ),
                                  ),
                                ]
                              : selectedDaySessions.map((session) {
                                  final mode = focusProvider.modes.firstWhere(
                                    (m) => m.id == session.modeId,
                                    orElse: () => focusProvider.modes.first,
                                  );
                                  final tag = session.tagId != null
                                      ? focusProvider.tags
                                            .where((t) => t.id == session.tagId)
                                            .firstOrNull
                                      : null;

                                  String timeString = _formatTime(
                                    session.startTime,
                                  );
                                  if (session.endTime != null) {
                                    timeString +=
                                        " - ${_formatTime(session.endTime!)}";
                                  } else {
                                    timeString += " - Ongoing";
                                  }

                                  int focusMins =
                                      session.totalSecondsFocused ~/ 60;
                                  return Card(
                                    margin: const EdgeInsets.symmetric(
                                      horizontal: 8,
                                      vertical: 4,
                                    ),
                                    elevation: 0,
                                    shape: RoundedRectangleBorder(
                                      side: BorderSide(
                                        color: Colors.grey.shade300,
                                        width: 1,
                                      ),
                                      borderRadius: BorderRadius.circular(8),
                                    ),
                                    child: ListTile(
                                      leading: Icon(
                                        Icons.circle,
                                        color: tag != null
                                            ? Color(tag.colorValue)
                                            : Colors.grey.shade400,
                                      ),
                                      title: Text(
                                        tag?.name ?? "Untagged",
                                        style: const TextStyle(
                                          fontWeight: FontWeight.bold,
                                        ),
                                      ),
                                      subtitle: Padding(
                                        padding: const EdgeInsets.only(
                                          top: 4.0,
                                        ),
                                        child: Column(
                                          crossAxisAlignment:
                                              CrossAxisAlignment.start,
                                          children: [
                                            Text(
                                              mode.name,
                                              style: const TextStyle(
                                                fontSize: 13,
                                              ),
                                            ),
                                            const SizedBox(height: 2),
                                            Text(
                                              timeString,
                                              style: TextStyle(
                                                color: Colors.grey.shade600,
                                                fontSize: 12,
                                                fontWeight: FontWeight.w500,
                                              ),
                                            ),
                                          ],
                                        ),
                                      ),
                                      trailing: Text(
                                        '${focusMins}m focus',
                                        style: const TextStyle(
                                          color: Colors.green,
                                          fontWeight: FontWeight.bold,
                                        ),
                                      ),
                                    ),
                                  );
                                }).toList(),
                        ),
                      ],
                    ),
            ),
          ),
        ],
      ),
    );
  }

  String _monthName(int month) {
    const months = [
      'January',
      'February',
      'March',
      'April',
      'May',
      'June',
      'July',
      'August',
      'September',
      'October',
      'November',
      'December',
    ];
    return months[month - 1];
  }
}

class _CalendarHeaderCell extends StatelessWidget {
  final String text;
  final Color? color;

  const _CalendarHeaderCell(this.text, {this.color});

  @override
  Widget build(BuildContext context) {
    return Padding(
      padding: const EdgeInsets.only(bottom: 8.0),
      child: Center(
        child: Text(
          text,
          style: TextStyle(
            fontWeight: FontWeight.bold,
            color: color ?? Colors.grey,
          ),
        ),
      ),
    );
  }
}
