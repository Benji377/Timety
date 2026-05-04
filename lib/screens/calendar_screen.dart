import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import 'package:timety/utils/utils.dart';
import '../providers/task_provider.dart';
import 'task_detail_screen.dart';

class CalendarScreen extends StatefulWidget {
  const CalendarScreen({super.key});

  @override
  State<CalendarScreen> createState() => _CalendarScreenState();
}

class _CalendarScreenState extends State<CalendarScreen> {
  DateTime _focusedMonth = DateTime.now();
  DateTime? _selectedDate; // Starts as null (empty list)

  // Helper to check if two dates are the exact same day (ignoring time)
  bool _isSameDay(DateTime? a, DateTime? b) {
    if (a == null || b == null) return false;
    return a.year == b.year && a.month == b.month && a.day == b.day;
  }

  // Generates the grid of weeks for the currently focused month
  List<List<DateTime>> _generateWeeks(DateTime month) {
    final firstDayOfMonth = DateTime(month.year, month.month, 1);
    final lastDayOfMonth = DateTime(month.year, month.month + 1, 0);

    // Find the Monday before or on the 1st of the month
    int offsetToMonday = firstDayOfMonth.weekday - DateTime.monday;
    DateTime currentDay = firstDayOfMonth.subtract(Duration(days: offsetToMonday));

    List<List<DateTime>> weeks = [];
    
    // Keep generating weeks until we've passed the end of the month
    while (currentDay.isBefore(lastDayOfMonth) || currentDay.weekday != DateTime.monday) {
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
    final provider = context.watch<TaskProvider>();
    final allTasks = provider.tasks;
    
    final weeks = _generateWeeks(_focusedMonth);

    // Filter tasks for the bottom list
    final selectedDayTasks = allTasks.where((t) => _isSameDay(t.dueDate, _selectedDate)).toList();

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
          )
        ],
      ),
      body: Column(
        children: [
          // --- TOP HALF: THE CALENDAR ---
          Expanded(
            flex: 1, // Takes up half the screen
            child: Container(
              color: Theme.of(context).colorScheme.surface,
              padding: const EdgeInsets.all(16.0),
              child: Column(
                children: [
                  // Month Navigation Header
                  Row(
                    mainAxisAlignment: MainAxisAlignment.spaceBetween,
                    children: [
                      IconButton(
                        icon: const Icon(Icons.chevron_left),
                        onPressed: () => setState(() => _focusedMonth = DateTime(_focusedMonth.year, _focusedMonth.month - 1)),
                      ),
                      Text(
                        "${_monthName(_focusedMonth.month)} ${_focusedMonth.year}",
                        style: const TextStyle(fontSize: 18, fontWeight: FontWeight.bold),
                      ),
                      IconButton(
                        icon: const Icon(Icons.chevron_right),
                        onPressed: () => setState(() => _focusedMonth = DateTime(_focusedMonth.year, _focusedMonth.month + 1)),
                      ),
                    ],
                  ),
                  const SizedBox(height: 16),
                  
                  // The Custom Table
                  Expanded(
                    child: Table(
                      defaultVerticalAlignment: TableCellVerticalAlignment.middle,
                      children: [
                        // 1. Header Row (M T W T F S S X)
                        const TableRow(
                          children: [
                            _CalendarHeaderCell('M'), _CalendarHeaderCell('T'), _CalendarHeaderCell('W'),
                            _CalendarHeaderCell('T'), _CalendarHeaderCell('F'), _CalendarHeaderCell('S'),
                            _CalendarHeaderCell('S'), _CalendarHeaderCell('X', color: Colors.blue),
                          ],
                        ),
                        // 2. Week Rows
                        ...weeks.map((week) {
                          // Calculate total tasks for this specific week
                          int weeklyTaskCount = allTasks.where((t) {
                            if (t.dueDate == null) return false;
                            return t.dueDate!.isAfter(week.first.subtract(const Duration(seconds: 1))) &&
                                   t.dueDate!.isBefore(week.last.add(const Duration(days: 1)));
                          }).length;

                          return TableRow(
                            children: [
                              ...week.map((day) {
                                final isCurrentMonth = day.month == _focusedMonth.month;
                                final isSelected = _isSameDay(day, _selectedDate);
                                final isToday = _isSameDay(day, DateTime.now());
                                final hasTasks = allTasks.any((t) => _isSameDay(t.dueDate, day));

                                return TableCell(
                                  child: GestureDetector(
                                    onTap: () => setState(() => _selectedDate = day),
                                    child: Container(
                                      margin: const EdgeInsets.all(4),
                                      decoration: BoxDecoration(
                                        color: isSelected ? Colors.blue.withValues(alpha: 0.2) : Colors.transparent,
                                        border: isToday ? Border.all(color: Colors.blue, width: 2) : null,
                                        borderRadius: BorderRadius.circular(8),
                                      ),
                                      height: 45,
                                      child: Column(
                                        mainAxisAlignment: MainAxisAlignment.center,
                                        children: [
                                          Text(
                                            '${day.day}',
                                            style: TextStyle(
                                              fontWeight: isSelected || isToday ? FontWeight.bold : FontWeight.normal,
                                              color: isCurrentMonth ? Theme.of(context).textTheme.bodyLarge?.color : Colors.grey.shade400,
                                            ),
                                          ),
                                          const SizedBox(height: 2),
                                          // The Blue Dot
                                          if (hasTasks)
                                            Container(
                                              width: 6,
                                              height: 6,
                                              decoration: const BoxDecoration(
                                                color: Colors.blue,
                                                shape: BoxShape.circle,
                                              ),
                                            )
                                          else
                                            const SizedBox(height: 6), // Keep spacing consistent
                                        ],
                                      ),
                                    ),
                                  ),
                                );
                              }),
                              // The 'X' Column: Weekly Task Count
                              TableCell(
                                child: Center(
                                  child: Text(
                                    weeklyTaskCount.toString(),
                                    style: const TextStyle(fontWeight: FontWeight.bold, color: Colors.blue),
                                  ),
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

          // Divider between halves
          const Divider(height: 1, thickness: 1),

          // --- BOTTOM HALF: TASK LIST ---
          Expanded(
            flex: 1, // Takes up the other half
            child: Container(
              color: Theme.of(context).colorScheme.surfaceContainerHighest.withValues(alpha: 0.3),
              child: _selectedDate == null 
                ? const Center(child: Text("Select a day to view tasks.", style: TextStyle(color: Colors.grey)))
                : selectedDayTasks.isEmpty
                  ? Center(child: Text("No tasks for ${_selectedDate!.month}/${_selectedDate!.day}.", style: const TextStyle(color: Colors.grey)))
                  : ListView.builder(
                      padding: const EdgeInsets.all(8),
                      itemCount: selectedDayTasks.length,
                      itemBuilder: (context, index) {
                        final task = selectedDayTasks[index];
                        return Card(
                          margin: const EdgeInsets.symmetric(horizontal: 8, vertical: 4),
                          elevation: 0,
                          shape: RoundedRectangleBorder(
                            side: BorderSide(color: task.isCompleted ? Colors.green : Colors.blue, width: 1),
                            borderRadius: BorderRadius.circular(8),
                          ),
                          child: ListTile(
                            leading: Checkbox(
                              value: task.isCompleted,
                              onChanged: (_) => context.read<TaskProvider>().toggleTask(task.id),
                            ),
                            title: Text(
                              task.title,
                              style: TextStyle(decoration: task.isCompleted ? TextDecoration.lineThrough : null),
                            ),
                            trailing: AppUtils().getPriorityIcon(task.priority),
                            onTap: () {
                              Navigator.push(
                                context,
                                MaterialPageRoute(builder: (_) => TaskDetailScreen(task: task)),
                              );
                            },
                          ),
                        );
                      },
                    ),
            ),
          ),
        ],
      ),
    );
  }

  String _monthName(int month) {
    const months = ['January', 'February', 'March', 'April', 'May', 'June', 'July', 'August', 'September', 'October', 'November', 'December'];
    return months[month - 1];
  }
}

// Small helper widget for the calendar table header
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
          style: TextStyle(fontWeight: FontWeight.bold, color: color ?? Colors.grey),
        ),
      ),
    );
  }
}