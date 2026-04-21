import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import 'package:intl/intl.dart';
import '../providers/task_provider.dart';
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
      return date.year == _selectedDate.year && date.month == _selectedDate.month && date.day == _selectedDate.day;
    }).toList();

    return Scaffold(
      appBar: AppBar(title: const Text('Calendar')),
      body: Column(
        children: [
          _buildCalendarHeader(),
          _buildCalendarGrid(),
          const Divider(),
          Padding(
            padding: const EdgeInsets.all(16.0),
            child: Row(
              mainAxisAlignment: MainAxisAlignment.spaceBetween,
              children: [
                Text('Tasks for ${DateFormat('MMM d').format(_selectedDate)}', style: Theme.of(context).textTheme.titleMedium),
                TextButton(
                  onPressed: () => Navigator.push(context, MaterialPageRoute(builder: (_) => DailyStatsScreen(initialDate: _selectedDate))),
                  child: const Text('View Stats'),
                ),
              ],
            ),
          ),
          Expanded(
            child: tasksForSelectedDate.isEmpty
                ? const Center(child: Text('No tasks for this day'))
                : ListView.builder(
                    itemCount: tasksForSelectedDate.length,
                    itemBuilder: (context, index) {
                      final task = tasksForSelectedDate[index];
                      return ListTile(
                        leading: Icon(task.priority.icon, color: Theme.of(context).colorScheme.primary),
                        title: Text(task.title, style: TextStyle(decoration: task.status == TaskStatus.done ? TextDecoration.lineThrough : null)),
                        subtitle: Text(task.size.label),
                        onTap: () => Navigator.push(context, MaterialPageRoute(builder: (_) => TaskDetailScreen(taskId: task.id!))),
                      );
                    },
                  ),
          ),
        ],
      ),
    );
  }

  Widget _buildCalendarHeader() {
    return Padding(
      padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 8),
      child: Row(
        mainAxisAlignment: MainAxisAlignment.spaceBetween,
        children: [
          Text(DateFormat('MMMM yyyy').format(_selectedDate), style: Theme.of(context).textTheme.titleLarge),
          Row(
            children: [
              IconButton(icon: const Icon(Icons.chevron_left), onPressed: () => setState(() => _selectedDate = DateTime(_selectedDate.year, _selectedDate.month - 1, _selectedDate.day))),
              IconButton(icon: const Icon(Icons.chevron_right), onPressed: () => setState(() => _selectedDate = DateTime(_selectedDate.year, _selectedDate.month + 1, _selectedDate.day))),
            ],
          ),
        ],
      ),
    );
  }

  Widget _buildCalendarGrid() {
    final daysInMonth = DateUtils.getDaysInMonth(_selectedDate.year, _selectedDate.month);
    final firstDayOffset = DateTime(_selectedDate.year, _selectedDate.month, 1).weekday % 7;

    return GridView.builder(
      shrinkWrap: true,
      physics: const NeverScrollableScrollPhysics(),
      padding: const EdgeInsets.symmetric(horizontal: 16),
      gridDelegate: const SliverGridDelegateWithFixedCrossAxisCount(crossAxisCount: 7),
      itemCount: daysInMonth + firstDayOffset,
      itemBuilder: (context, index) {
        if (index < firstDayOffset) return const SizedBox();
        final day = index - firstDayOffset + 1;
        final date = DateTime(_selectedDate.year, _selectedDate.month, day);
        final isSelected = date.year == _selectedDate.year && date.month == _selectedDate.month && date.day == _selectedDate.day;
        final isToday = date.year == DateTime.now().year && date.month == DateTime.now().month && date.day == DateTime.now().day;

        return GestureDetector(
          onTap: () => setState(() => _selectedDate = date),
          child: Container(
            margin: const EdgeInsets.all(4),
            decoration: BoxDecoration(
              color: isSelected ? Theme.of(context).colorScheme.primary : (isToday ? Theme.of(context).colorScheme.primaryContainer : null),
              borderRadius: BorderRadius.circular(8),
              border: isToday ? Border.all(color: Theme.of(context).colorScheme.primary) : null,
            ),
            child: Center(
              child: Text(
                '$day',
                style: TextStyle(
                  color: isSelected ? Theme.of(context).colorScheme.onPrimary : null,
                  fontWeight: isSelected || isToday ? FontWeight.bold : null,
                ),
              ),
            ),
          ),
        );
      },
    );
  }
}
