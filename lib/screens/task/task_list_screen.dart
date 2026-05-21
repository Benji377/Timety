import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import '../../data/task/task.dart';
import '../../theme/app_theme.dart';
import '../../providers/task_provider.dart';
import '../../providers/user_provider.dart';
import '../../utils/task_filter_engine.dart';
import '../../widgets/expansion_section.dart';
import '../../widgets/list_tiles/task_list_tile.dart';
import '../calendar_screen.dart';
import '../statistics_screen.dart';
import 'task_detail_screen.dart';

class TaskListScreen extends StatefulWidget {
  const TaskListScreen({super.key});

  @override
  State<TaskListScreen> createState() => _TaskListScreenState();
}

class _TaskListScreenState extends State<TaskListScreen> {
  // --- Search & Sort State ---
  String _searchQuery = '';
  String? _selectedCategoryFilter;
  TaskSortOption _sortOption = TaskSortOption.dueDate;
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
    final engine = TaskFilterEngine(
      searchQuery: _searchQuery,
      categoryFilter: _selectedCategoryFilter,
      sortOption: _sortOption,
      isAscending: _isAscending,
    );
    return engine.process(rawTasks);
  }

  Widget _buildTaskSection(
    String title,
    Color color,
    List<Task> tasks, {
    bool initExpanded = true,
    bool isOverdue = false,
  }) {
    return ExpansionSection(
      title: '$title (${tasks.length})',
      color: color,
      initiallyExpanded: initExpanded,
      children: tasks
          .map(
            (task) => TaskListTile(
              task: task,
              isOverdue: isOverdue,
              onToggleCompleted: () => context.read<TaskProvider>().toggleTask(
                task.id,
                userProvider: context.read<UserProvider>(),
              ),
              onDelete: () => context.read<TaskProvider>().removeTask(task.id),
              onTap: () {
                Navigator.push(
                  context,
                  MaterialPageRoute(
                    builder: (_) => TaskDetailScreen(task: task),
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
                  builder: (context) =>
                      const StatisticsScreen(initialTabIndex: 1),
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
                        decoration: const InputDecoration(
                          hintText: 'Search title or description...',
                          prefixIcon: Icon(Icons.search),
                          border: OutlineInputBorder(
                            borderRadius: AppTheme.brNeo,
                          ),
                          contentPadding: EdgeInsets.symmetric(
                            horizontal: AppTheme.spaceMedium,
                            vertical: AppTheme.spaceSmall,
                          ),
                        ),
                        onChanged: (val) => setState(() => _searchQuery = val),
                      ),
                    ),
                    const SizedBox(width: 8),

                    // Sort Dropdown
                    PopupMenuButton<TaskSortOption>(
                      icon: const Icon(Icons.sort),
                      tooltip: 'Sort by',
                      onSelected: (TaskSortOption result) {
                        setState(() => _sortOption = result);
                      },
                      itemBuilder: (BuildContext context) =>
                          <PopupMenuEntry<TaskSortOption>>[
                            const PopupMenuItem(
                              value: TaskSortOption.dueDate,
                              child: Text('Due Date'),
                            ),
                            const PopupMenuItem(
                              value: TaskSortOption.priority,
                              child: Text('Priority'),
                            ),
                            const PopupMenuItem(
                              value: TaskSortOption.size,
                              child: Text('Size'),
                            ),
                            const PopupMenuItem(
                              value: TaskSortOption.category,
                              child: Text('Category'),
                            ),
                            const PopupMenuItem(
                              value: TaskSortOption.alphabetical,
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

              // --- CATEGORY PILLS ---
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
                        if (task.dueDate!.isBefore(now)) {
                          overdue.add(task);
                        } else if (task.dueDate!.year == today.year &&
                            task.dueDate!.month == today.month &&
                            task.dueDate!.day == today.day) {
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
                        _buildTaskSection(
                          'Overdue',
                          AppTheme.errorColor,
                          overdue,
                          isOverdue: true,
                        ),
                        _buildTaskSection(
                          'Due Today',
                          AppTheme.warningColor,
                          dueToday,
                        ),
                        _buildTaskSection('To Do', AppTheme.taskColor, todo),
                        _buildTaskSection(
                          'Done',
                          AppTheme.successColor,
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
