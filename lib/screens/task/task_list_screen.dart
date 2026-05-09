import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import '../../data/task/task.dart';
import '../../providers/task_provider.dart';
import '../../providers/user_provider.dart';
import '../../widgets/expansion_section.dart';
import '../../widgets/list_tiles/task_list_tile.dart';
import '../calendar_screen.dart';
import '../statistics_screen.dart';
import 'task_detail_screen.dart';

enum SortOption { dueDate, priority, size, alphabetical, category }

class TaskListScreen extends StatefulWidget {
  const TaskListScreen({super.key});

  @override
  State<TaskListScreen> createState() => _TaskListScreenState();
}

class _TaskListScreenState extends State<TaskListScreen> {
  // --- Search & Sort State ---
  String _searchQuery = '';
  String? _selectedCategoryFilter;
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

  // --- DATA PIPELINE: Filter & Sort ---
  List<Task> _getProcessedTasks(List<Task> rawTasks) {
    // Search & Category Filter
    var processed = rawTasks.where((task) {
      // Category Filter Check
      if (_selectedCategoryFilter != null &&
          _selectedCategoryFilter!.isNotEmpty) {
        if (task.category != _selectedCategoryFilter) return false;
      }

      // Text Search Check
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

  Widget _buildTaskSection(
    String title,
    Color color,
    List<Task> tasks, {
    bool initExpanded = true,
  }) {
    return ExpansionSection(
      title: '$title (${tasks.length})',
      color: color,
      initiallyExpanded: initExpanded,
      children: tasks
          .map(
            (task) => TaskListTile(
              task: task,
              onToggleCompleted: () => context.read<TaskProvider>().toggleTask(
                task.id,
                userProvider: context.read<UserProvider>(),
              ),
              onDelete: () => context.read<TaskProvider>().removeTask(task.id),
              onTap: () {
                Navigator.push(
                  context,
                  MaterialPageRoute(
                    builder: (_) =>
                        TaskDetailScreen(task: task, isEditing: false),
                  ),
                );
              },
            ),
          )
          .toList(),
    );
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: const Text('My Tasks'),
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
      body: Consumer<TaskProvider>(
        builder: (context, provider, child) {
          // Extract unique categories from tasks
          final allCategories =
              provider.tasks
                  .map((t) => t.category.trim())
                  .where((c) => c.isNotEmpty)
                  .toSet()
                  .toList()
                ..sort();

          return Column(
            children: [
              // --- TOP BAR: SEARCH & SORT ---
              Padding(
                padding: const EdgeInsets.fromLTRB(8, 8, 8, 0),
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
                          contentPadding: const EdgeInsets.symmetric(
                            vertical: 0,
                          ),
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
                        _isAscending
                            ? Icons.arrow_upward
                            : Icons.arrow_downward,
                      ),
                      tooltip: _isAscending ? 'Ascending' : 'Descending',
                      onPressed: () =>
                          setState(() => _isAscending = !_isAscending),
                    ),
                  ],
                ),
              ),

              // --- CATEGORY PILLS (NEW) ---
              if (allCategories.isNotEmpty)
                Container(
                  height: 56,
                  padding: const EdgeInsets.symmetric(vertical: 8),
                  child: ListView.builder(
                    scrollDirection: Axis.horizontal,
                    padding: const EdgeInsets.symmetric(horizontal: 8.0),
                    itemCount:
                        allCategories.length + 1, // +1 for the "All" pill
                    itemBuilder: (context, index) {
                      if (index == 0) {
                        final isSelected = _selectedCategoryFilter == null;
                        return Padding(
                          padding: const EdgeInsets.only(right: 8.0),
                          child: FilterChip(
                            label: const Text('All'),
                            selected: isSelected,
                            onSelected: (_) =>
                                setState(() => _selectedCategoryFilter = null),
                          ),
                        );
                      }

                      final category = allCategories[index - 1];
                      final isSelected = _selectedCategoryFilter == category;

                      return Padding(
                        padding: const EdgeInsets.only(right: 8.0),
                        child: FilterChip(
                          label: Text(category),
                          selected: isSelected,
                          onSelected: (_) {
                            setState(() {
                              // If tapped again, clear the filter. Otherwise, select it.
                              _selectedCategoryFilter = isSelected
                                  ? null
                                  : category;
                            });
                          },
                        ),
                      );
                    },
                  ),
                ),

              // --- MAIN LIST AREA ---
              Expanded(
                child: Builder(
                  builder: (context) {
                    if (provider.tasks.isEmpty) {
                      return const Center(
                        child: Text("No tasks yet! Tap + to add one."),
                      );
                    }

                    // Apply pipeline
                    final processedTasks = _getProcessedTasks(provider.tasks);

                    if (processedTasks.isEmpty) {
                      return const Center(
                        child: Text("No tasks match your filters."),
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
                      padding: const EdgeInsets.only(bottom: 80),
                      children: [
                        _buildTaskSection('Overdue', Colors.red, overdue),
                        _buildTaskSection(
                          'Due Today',
                          Colors.amber.shade700,
                          dueToday,
                        ),
                        _buildTaskSection('To Do', Colors.blue, todo),
                        _buildTaskSection(
                          'Done',
                          Colors.green,
                          done,
                          initExpanded: false,
                        ),
                      ],
                    );
                  },
                ),
              ),
            ],
          );
        },
      ),
      floatingActionButton: FloatingActionButton(
        heroTag: 'task_list_add_button',
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
