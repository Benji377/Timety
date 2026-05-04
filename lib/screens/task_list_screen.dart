import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import '../data/task.dart';
import '../utils/utils.dart';
import '../providers/task_provider.dart';
import '../widgets/app_dialogs.dart';
import 'task_detail_screen.dart';

enum SortOption { dueDate, priority, size, alphabetical, category }

class TodoListScreen extends StatefulWidget {
  const TodoListScreen({super.key});

  @override
  State<TodoListScreen> createState() => _TodoListScreenState();
}

class _TodoListScreenState extends State<TodoListScreen> {
  // --- Search & Sort State ---
  String _searchQuery = '';
  SortOption _sortOption = SortOption.dueDate;
  bool _isAscending = true;

  @override
  void initState() {
    super.initState();
    WidgetsBinding.instance.addPostFrameCallback((_) {
      if (!mounted) return;
      context.read<TaskProvider>().loadTasks();
    });
  }

  // --- UI HELPERS ---
  Color _getTaskBorderColor(Task task) {
    if (task.isCompleted) return Colors.green;
    if (task.dueDate != null) {
      final now = DateTime.now();
      final today = DateTime(now.year, now.month, now.day);
      final dueDay = DateTime(
        task.dueDate!.year,
        task.dueDate!.month,
        task.dueDate!.day,
      );
      if (dueDay.isBefore(today)) return Colors.red;
      if (dueDay.isAtSameMomentAs(today)) return Colors.amber.shade600;
    }
    return Colors.blue;
  }

  // --- DATA PIPELINE: Filter & Sort ---
  List<Task> _getProcessedTasks(List<Task> rawTasks) {
    // 1. Search Filter
    var processed = rawTasks.where((task) {
      if (_searchQuery.isEmpty) return true;
      final q = _searchQuery.toLowerCase();
      return task.title.toLowerCase().contains(q) ||
          task.description.toLowerCase().contains(q);
    }).toList();

    // 2. Sort
    processed.sort((a, b) {
      int result = 0;
      switch (_sortOption) {
        case SortOption.alphabetical:
          result = a.title.toLowerCase().compareTo(b.title.toLowerCase());
          break;
        case SortOption.category:
          result = a.category.toLowerCase().compareTo(b.category.toLowerCase());
          break;
        case SortOption.priority:
          // Enums are sorted by their declaration index (0, 1, 2, 3)
          result = a.priority.index.compareTo(b.priority.index);
          break;
        case SortOption.size:
          result = a.size.index.compareTo(b.size.index);
          break;
        case SortOption.dueDate:
          if (a.dueDate == null && b.dueDate == null) {
            result = 0;
          } else if (a.dueDate == null) {
            result = 1; // Put tasks without due dates at the bottom
          } else if (b.dueDate == null) {
            result = -1;
          } else {
            result = a.dueDate!.compareTo(b.dueDate!);
          }
          break;
      }
      return _isAscending ? result : -result;
    });

    return processed;
  }

  // --- INDIVIDUAL TASK TILE WIDGET ---
  Widget _buildTaskTile(Task task) {
    final borderColor = _getTaskBorderColor(task);

    return Dismissible(
      key: Key(task.id),
      background: Container(
        color: Colors.red,
        alignment: Alignment.centerRight,
        padding: const EdgeInsets.only(right: 20),
        margin: const EdgeInsets.symmetric(horizontal: 8, vertical: 4),
        child: const Icon(Icons.delete, color: Colors.white),
      ),
      direction: DismissDirection.endToStart,
      onDismissed: (direction) {
        context.read<TaskProvider>().removeTask(task.id);
      },
      confirmDismiss: (direction) async {
        return await AppDialogs.showConfirmation(
              context: context,
              title: 'Delete Task',
              content: 'Are you sure you want to delete this task?',
            ) ??
            false;
      },
      child: Card(
        margin: const EdgeInsets.symmetric(horizontal: 8, vertical: 4),
        elevation: 0,
        shape: RoundedRectangleBorder(
          side: BorderSide(color: borderColor, width: 2),
          borderRadius: BorderRadius.circular(8),
        ),
        child: ListTile(
          leading: Checkbox(
            value: task.isCompleted,
            activeColor: Colors.green,
            onChanged: (_) => context.read<TaskProvider>().toggleTask(task.id),
          ),
          title: Text(
            task.title,
            style: TextStyle(
              decoration: task.isCompleted ? TextDecoration.lineThrough : null,
              color: task.isCompleted ? Colors.grey : null,
            ),
          ),
          subtitle: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              if (task.description.isNotEmpty)
                Text(
                  task.description,
                  maxLines: 1,
                  overflow: TextOverflow.ellipsis,
                  style: const TextStyle(fontSize: 12),
                ),
              if (task.dueDate != null)
                Padding(
                  padding: const EdgeInsets.only(top: 4.0),
                  child: Row(
                    children: [
                      Icon(Icons.access_time, size: 14, color: borderColor),
                      const SizedBox(width: 4),
                      Text(
                        "${task.dueDate!.month.toString().padLeft(2, '0')}/${task.dueDate!.day.toString().padLeft(2, '0')} ${task.dueDate!.hour.toString().padLeft(2, '0')}:${task.dueDate!.minute.toString().padLeft(2, '0')}",
                        style: TextStyle(
                          fontSize: 12,
                          color: borderColor,
                          fontWeight: FontWeight.w500,
                        ),
                      ),
                    ],
                  ),
                ),
            ],
          ),
          trailing: Row(
            mainAxisSize: MainAxisSize.min,
            children: [
              Text(
                AppUtils().getSizeEmoji(task.size),
                style: const TextStyle(fontSize: 18),
              ),
              const SizedBox(width: 8),
              AppUtils().getPriorityIcon(task.priority),
            ],
          ),
          onTap: () {
            Navigator.push(
              context,
              MaterialPageRoute(
                builder: (_) => TaskDetailScreen(task: task, isEditing: false),
              ),
            );
          },
        ),
      ),
    );
  }

  // --- ACCORDION BUILDER ---
  Widget _buildAccordion(
    String title,
    Color color,
    List<Task> tasks, {
    bool initExpanded = true,
  }) {
    if (tasks.isEmpty) return const SizedBox.shrink(); // Hide if empty

    return Theme(
      // Remove the divider lines native to ExpansionTile
      data: Theme.of(context).copyWith(dividerColor: Colors.transparent),
      child: ExpansionTile(
        initiallyExpanded: initExpanded,
        iconColor: color,
        collapsedIconColor: color,
        title: Row(
          children: [
            Icon(Icons.circle, size: 12, color: color),
            const SizedBox(width: 8),
            Text(
              "$title (${tasks.length})",
              style: TextStyle(fontWeight: FontWeight.bold, color: color),
            ),
          ],
        ),
        children: tasks.map((t) => _buildTaskTile(t)).toList(),
      ),
    );
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(title: const Text('My Tasks')),
      body: Column(
        children: [
          // --- TOP BAR: SEARCH & SORT ---
          Padding(
            padding: const EdgeInsets.all(8.0),
            child: Row(
              children: [
                Expanded(
                  child: TextField(
                    decoration: InputDecoration(
                      hintText: 'Search title or description...',
                      prefixIcon: const Icon(Icons.search),
                      border: OutlineInputBorder(
                        borderRadius: BorderRadius.circular(8),
                      ),
                      contentPadding: const EdgeInsets.symmetric(vertical: 0),
                    ),
                    onChanged: (val) => setState(() => _searchQuery = val),
                  ),
                ),
                const SizedBox(width: 8),

                // Sort Dropdown
                PopupMenuButton<SortOption>(
                  icon: const Icon(Icons.sort),
                  tooltip: 'Sort by',
                  onSelected: (SortOption result) {
                    setState(() => _sortOption = result);
                  },
                  itemBuilder: (BuildContext context) =>
                      <PopupMenuEntry<SortOption>>[
                        const PopupMenuItem(
                          value: SortOption.dueDate,
                          child: Text('Due Date'),
                        ),
                        const PopupMenuItem(
                          value: SortOption.priority,
                          child: Text('Priority'),
                        ),
                        const PopupMenuItem(
                          value: SortOption.size,
                          child: Text('Size'),
                        ),
                        const PopupMenuItem(
                          value: SortOption.category,
                          child: Text('Category'),
                        ),
                        const PopupMenuItem(
                          value: SortOption.alphabetical,
                          child: Text('Alphabetical'),
                        ),
                      ],
                ),

                // Order Toggle (Asc/Desc)
                IconButton(
                  icon: Icon(
                    _isAscending ? Icons.arrow_upward : Icons.arrow_downward,
                  ),
                  tooltip: _isAscending ? 'Ascending' : 'Descending',
                  onPressed: () => setState(() => _isAscending = !_isAscending),
                ),
              ],
            ),
          ),

          // --- MAIN LIST AREA ---
          Expanded(
            child: Consumer<TaskProvider>(
              builder: (context, provider, child) {
                if (provider.tasks.isEmpty) {
                  return const Center(
                    child: Text("No tasks yet! Tap + to add one."),
                  );
                }

                // Apply pipeline
                final processedTasks = _getProcessedTasks(provider.tasks);

                if (processedTasks.isEmpty) {
                  return const Center(
                    child: Text("No tasks match your search."),
                  );
                }

                // Grouping Logic
                final overdue = <Task>[];
                final dueToday = <Task>[];
                final todo = <Task>[];
                final done = <Task>[];

                final now = DateTime.now();
                final today = DateTime(now.year, now.month, now.day);

                for (var task in processedTasks) {
                  if (task.isCompleted) {
                    done.add(task);
                  } else if (task.dueDate != null) {
                    final dueDay = DateTime(
                      task.dueDate!.year,
                      task.dueDate!.month,
                      task.dueDate!.day,
                    );
                    if (dueDay.isBefore(today)) {
                      overdue.add(task);
                    } else if (dueDay.isAtSameMomentAs(today)) {
                      dueToday.add(task);
                    } else {
                      todo.add(task);
                    }
                  } else {
                    todo.add(task);
                  }
                }

                return ListView(
                  padding: const EdgeInsets.only(bottom: 80), // Keep FAB clear
                  children: [
                    _buildAccordion("Overdue", Colors.red, overdue),
                    _buildAccordion(
                      "Due Today",
                      Colors.amber.shade700,
                      dueToday,
                    ),
                    _buildAccordion("To Do", Colors.blue, todo),
                    _buildAccordion(
                      "Done",
                      Colors.green,
                      done,
                      initExpanded: false,
                    ), // Default Closed
                  ],
                );
              },
            ),
          ),
        ],
      ),
      floatingActionButton: FloatingActionButton(
        onPressed: () {
          Navigator.push(
            context,
            MaterialPageRoute(builder: (context) => const TaskDetailScreen()),
          );
        },
        child: const Icon(Icons.add),
      ),
    );
  }
}
