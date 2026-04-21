import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import 'package:intl/intl.dart';
import '../providers/task_provider.dart';
import '../providers/focus_provider.dart';
import '../data/task.dart';
import 'task_detail_screen.dart';
import 'daily_stats_screen.dart';

class CalendarScreen extends StatefulWidget {
  const CalendarScreen({super.key});

  @override
  State<CalendarScreen> createState() => _CalendarScreenState();
}

class _CalendarScreenState extends State<CalendarScreen> {
  DateTime _selectedDate = DateTime.now();

  @override
  Widget build(BuildContext context) {
    final taskProvider = context.watch<TaskProvider>();
    final tasks = taskProvider.allTasks;

    final tasksForSelectedDate = tasks.where((t) {
      if (t.dueDate == null) return false;
      final date = DateTime.fromMillisecondsSinceEpoch(t.dueDate!);
      return date.year == _selectedDate.year &&
          date.month == _selectedDate.month &&
          date.day == _selectedDate.day;
    }).toList();

    return Scaffold(
      body: SafeArea(
        child: Column(
          children: [
            Padding(
              padding: const EdgeInsets.all(16),
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Text(
                    'Calendar',
                    style: Theme.of(context).textTheme.headlineSmall,
                  ),
                  const SizedBox(height: 16),
                  _buildCalendarHeader(),
                ],
              ),
            ),
            Padding(
              padding: const EdgeInsets.symmetric(horizontal: 16),
              child: _buildCalendarGrid(),
            ),
            const Divider(),
            Padding(
              padding: const EdgeInsets.all(16.0),
              child: Row(
                mainAxisAlignment: MainAxisAlignment.spaceBetween,
                children: [
                  Text(
                    'Tasks for ${DateFormat('MMM d').format(_selectedDate)}',
                    style: Theme.of(context).textTheme.titleMedium,
                  ),
                  TextButton(
                    onPressed: () => Navigator.push(
                      context,
                      MaterialPageRoute(
                        builder: (_) =>
                            DailyStatsScreen(initialDate: _selectedDate),
                      ),
                    ),
                    child: const Text('View Stats'),
                  ),
                ],
              ),
            ),
            Expanded(
              child: tasksForSelectedDate.isEmpty
                  ? const Center(child: Text('No tasks for this day'))
                  : ListView.builder(
                      padding: const EdgeInsets.symmetric(horizontal: 16),
                      itemCount: tasksForSelectedDate.length,
                      itemBuilder: (context, index) {
                        final task = tasksForSelectedDate[index];
                        return ListTile(
                          leading: Icon(
                            task.priority.icon,
                            color: Theme.of(context).colorScheme.primary,
                          ),
                          title: Text(
                            task.title,
                            style: TextStyle(
                              decoration: task.status == TaskStatus.done
                                  ? TextDecoration.lineThrough
                                  : null,
                            ),
                          ),
                          subtitle: Text(task.size.label),
                          onTap: () => Navigator.push(
                            context,
                            MaterialPageRoute(
                              builder: (_) =>
                                  TaskDetailScreen(taskId: task.id!),
                            ),
                          ),
                        );
                      },
                    ),
            ),
          ],
        ),
      ),
    );
  }

  Widget _buildCalendarHeader() {
    return Padding(
      padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 8),
      child: Row(
        mainAxisAlignment: MainAxisAlignment.spaceBetween,
        children: [
          Text(
            DateFormat('MMMM yyyy').format(_selectedDate),
            style: Theme.of(context).textTheme.titleLarge,
          ),
          Row(
            children: [
              IconButton(
                icon: const Icon(Icons.chevron_left),
                onPressed: () => setState(
                  () => _selectedDate = DateTime(
                    _selectedDate.year,
                    _selectedDate.month - 1,
                    _selectedDate.day,
                  ),
                ),
              ),
              IconButton(
                icon: const Icon(Icons.chevron_right),
                onPressed: () => setState(
                  () => _selectedDate = DateTime(
                    _selectedDate.year,
                    _selectedDate.month + 1,
                    _selectedDate.day,
                  ),
                ),
              ),
            ],
          ),
        ],
      ),
    );
  }

  Widget _buildCalendarGrid() {
    final taskProvider = context.watch<TaskProvider>();
    final focusProvider = context.watch<FocusProvider>();
    final daysInMonth = DateUtils.getDaysInMonth(
      _selectedDate.year,
      _selectedDate.month,
    );
    final firstDayOffset =
        DateTime(_selectedDate.year, _selectedDate.month, 1).weekday % 7;

    return SingleChildScrollView(
      scrollDirection: Axis.horizontal,
      child: Row(
        children: [
          // Calendar grid
          Column(
            children: [
              // Header row with day names
              Row(
                children: [
                  ...['M', 'T', 'W', 'T', 'F', 'S', 'S'].map((day) {
                    return SizedBox(
                      width: 50,
                      height: 40,
                      child: Center(
                        child: Text(
                          day,
                          style: const TextStyle(fontWeight: FontWeight.bold),
                        ),
                      ),
                    );
                  }).toList(),
                ],
              ),
              // Calendar days
              GridView.builder(
                shrinkWrap: true,
                physics: const NeverScrollableScrollPhysics(),
                gridDelegate: const SliverGridDelegateWithFixedCrossAxisCount(
                  crossAxisCount: 7,
                  childAspectRatio: 1,
                  mainAxisSpacing: 4,
                  crossAxisSpacing: 4,
                ),
                itemCount: daysInMonth + firstDayOffset,
                itemBuilder: (context, index) {
                  if (index < firstDayOffset) {
                    return const SizedBox(width: 50);
                  }
                  final day = index - firstDayOffset + 1;
                  final date = DateTime(
                    _selectedDate.year,
                    _selectedDate.month,
                    day,
                  );
                  final isSelected =
                      date.year == _selectedDate.year &&
                      date.month == _selectedDate.month &&
                      date.day == _selectedDate.day;
                  final isToday =
                      date.year == DateTime.now().year &&
                      date.month == DateTime.now().month &&
                      date.day == DateTime.now().day;

                  // Count tasks for this day
                  final tasksForDay = taskProvider.allTasks.where((t) {
                    if (t.dueDate == null) return false;
                    final dueDate = DateTime.fromMillisecondsSinceEpoch(
                      t.dueDate!,
                    );
                    return dueDate.year == date.year &&
                        dueDate.month == date.month &&
                        dueDate.day == date.day;
                  }).length;

                  // Count focus minutes for this day
                  final sessionsForDay = focusProvider.getSessionsForDay(date);
                  final focusMinutes = sessionsForDay.fold<int>(
                    0,
                    (sum, session) => sum + session.duration ~/ 60000,
                  );

                  return GestureDetector(
                    onTap: () => setState(() => _selectedDate = date),
                    child: Container(
                      width: 50,
                      decoration: BoxDecoration(
                        color: isSelected
                            ? Theme.of(context).colorScheme.primary
                            : (isToday
                                  ? Theme.of(
                                      context,
                                    ).colorScheme.primaryContainer
                                  : null),
                        borderRadius: BorderRadius.circular(8),
                        border: isToday
                            ? Border.all(
                                color: Theme.of(context).colorScheme.primary,
                              )
                            : null,
                      ),
                      child: Column(
                        mainAxisAlignment: MainAxisAlignment.center,
                        children: [
                          Text(
                            '$day',
                            style: TextStyle(
                              color: isSelected
                                  ? Theme.of(context).colorScheme.onPrimary
                                  : null,
                              fontWeight: isSelected || isToday
                                  ? FontWeight.bold
                                  : null,
                              fontSize: 14,
                            ),
                          ),
                          if (tasksForDay > 0 || focusMinutes > 0)
                            SizedBox(
                              height: 16,
                              child: Row(
                                mainAxisAlignment: MainAxisAlignment.center,
                                children: [
                                  if (tasksForDay > 0)
                                    Container(
                                      padding: const EdgeInsets.symmetric(
                                        horizontal: 3,
                                        vertical: 1,
                                      ),
                                      decoration: BoxDecoration(
                                        color: Colors.blue,
                                        borderRadius: BorderRadius.circular(2),
                                      ),
                                      child: Text(
                                        '$tasksForDay',
                                        style: const TextStyle(
                                          color: Colors.white,
                                          fontSize: 8,
                                        ),
                                      ),
                                    ),
                                  if (focusMinutes > 0)
                                    Padding(
                                      padding: const EdgeInsets.only(left: 2),
                                      child: Container(
                                        padding: const EdgeInsets.symmetric(
                                          horizontal: 3,
                                          vertical: 1,
                                        ),
                                        decoration: BoxDecoration(
                                          color: Colors.green,
                                          borderRadius: BorderRadius.circular(
                                            2,
                                          ),
                                        ),
                                        child: Text(
                                          '${focusMinutes}m',
                                          style: const TextStyle(
                                            color: Colors.white,
                                            fontSize: 8,
                                          ),
                                        ),
                                      ),
                                    ),
                                ],
                              ),
                            ),
                        ],
                      ),
                    ),
                  );
                },
              ),
            ],
          ),
          // Weekly summary column
          Column(
            children: [
              SizedBox(
                width: 60,
                height: 40,
                child: Center(
                  child: Text(
                    'WE',
                    style: Theme.of(context).textTheme.labelMedium,
                  ),
                ),
              ),
              ...List.generate((daysInMonth + firstDayOffset + 6) ~/ 7, (
                weekIndex,
              ) {
                // Calculate the dates for this week
                final weekStart = DateTime(
                  _selectedDate.year,
                  _selectedDate.month,
                  1,
                ).add(Duration(days: firstDayOffset + weekIndex * 7));

                // Get tasks and focus minutes for this week
                int weekTasks = 0;
                int weekFocusMinutes = 0;

                for (int dayOffset = 0; dayOffset < 7; dayOffset++) {
                  final dayDate = weekStart.add(Duration(days: dayOffset));
                  if (dayDate.month != _selectedDate.month) continue;

                  weekTasks += taskProvider.allTasks.where((t) {
                    if (t.dueDate == null) return false;
                    final dueDate = DateTime.fromMillisecondsSinceEpoch(
                      t.dueDate!,
                    );
                    return dueDate.year == dayDate.year &&
                        dueDate.month == dayDate.month &&
                        dueDate.day == dayDate.day;
                  }).length;

                  final sessionsForDay = focusProvider.getSessionsForDay(
                    dayDate,
                  );
                  weekFocusMinutes += sessionsForDay.fold<int>(
                    0,
                    (sum, session) => sum + session.duration ~/ 60000,
                  );
                }

                return SizedBox(
                  width: 60,
                  height: 50,
                  child: Card(
                    child: Column(
                      mainAxisAlignment: MainAxisAlignment.center,
                      children: [
                        Text(
                          '$weekTasks',
                          style: const TextStyle(
                            fontSize: 12,
                            fontWeight: FontWeight.bold,
                            color: Colors.blue,
                          ),
                        ),
                        Text(
                          '${weekFocusMinutes}m',
                          style: const TextStyle(
                            fontSize: 12,
                            fontWeight: FontWeight.bold,
                            color: Colors.green,
                          ),
                        ),
                      ],
                    ),
                  ),
                );
              }),
            ],
          ),
        ],
      ),
    );
  }
}
