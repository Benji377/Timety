import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import '../providers/task_provider.dart';
import '../providers/user_provider.dart';
import '../data/task.dart';
import '../widgets/task_card.dart';
import 'add_task_screen.dart';
import 'task_detail_screen.dart';

class TasksScreen extends StatefulWidget {
  const TasksScreen({super.key});

  @override
  State<TasksScreen> createState() => _TasksScreenState();
}

class _TasksScreenState extends State<TasksScreen> {
  String _searchQuery = '';
  int? _selectedCategoryId;
  bool _doneExpanded = false;

  @override
  Widget build(BuildContext context) {
    final taskProvider = context.watch<TaskProvider>();
    final allTasks = taskProvider.allTasks;
    final categories = taskProvider.categories;

    final filteredTasks = allTasks.where((task) {
      final matchesSearch = task.title.toLowerCase().contains(_searchQuery.toLowerCase());
      final matchesCategory = _selectedCategoryId == null || task.categoryId == _selectedCategoryId;
      return matchesSearch && matchesCategory;
    }).toList();

    final now = DateTime.now();
    final today = DateTime(now.year, now.month, now.day);
    final tomorrow = today.add(const Duration(days: 1));

    final overdueTasks = filteredTasks.where((t) => t.status != TaskStatus.done && t.dueDate != null && t.dueDate! < today.millisecondsSinceEpoch).toList();
    final todayTasks = filteredTasks.where((t) => t.status != TaskStatus.done && t.dueDate != null && t.dueDate! >= today.millisecondsSinceEpoch && t.dueDate! < tomorrow.millisecondsSinceEpoch).toList();
    final upcomingTasks = filteredTasks.where((t) => t.status != TaskStatus.done && (t.dueDate == null || t.dueDate! >= tomorrow.millisecondsSinceEpoch)).toList();
    final doneTasks = filteredTasks.where((t) => t.status == TaskStatus.done).toList();

    return Scaffold(
      appBar: AppBar(
        title: TextField(
          onChanged: (val) => setState(() => _searchQuery = val),
          decoration: const InputDecoration(
            hintText: 'Search tasks...',
            border: InputBorder.none,
          ),
        ),
        bottom: PreferredSize(
          preferredSize: const Size.fromHeight(50),
          child: SizedBox(
            height: 50,
            child: ListView(
              scrollDirection: Axis.horizontal,
              padding: const EdgeInsets.symmetric(horizontal: 16),
              children: [
                Padding(
                  padding: const EdgeInsets.only(right: 8.0),
                  child: FilterChip(
                    selected: _selectedCategoryId == null,
                    label: const Text('All'),
                    onSelected: (_) => setState(() => _selectedCategoryId = null),
                  ),
                ),
                ...categories.map((cat) => Padding(
                  padding: const EdgeInsets.only(right: 8.0),
                  child: FilterChip(
                    selected: _selectedCategoryId == cat.id,
                    label: Text(cat.name),
                    onSelected: (_) => setState(() => _selectedCategoryId = cat.id),
                  ),
                )),
              ],
            ),
          ),
        ),
      ),
      body: ListView(
        padding: const EdgeInsets.all(16),
        children: [
          if (overdueTasks.isNotEmpty) ...[
            _buildSectionHeader('Overdue', Colors.red),
            ...overdueTasks.map((t) => _buildTaskCard(context, t)),
          ],
          if (todayTasks.isNotEmpty) ...[
            _buildSectionHeader('Today', Colors.orange),
            ...todayTasks.map((t) => _buildTaskCard(context, t)),
          ],
          if (upcomingTasks.isNotEmpty) ...[
            _buildSectionHeader('Upcoming', Colors.blue),
            ...upcomingTasks.map((t) => _buildTaskCard(context, t)),
          ],
          if (doneTasks.isNotEmpty) ...[
            ListTile(
              title: Text('Done (${doneTasks.length})', style: const TextStyle(color: Colors.green, fontWeight: FontWeight.bold)),
              trailing: Icon(_doneExpanded ? Icons.keyboard_arrow_up : Icons.keyboard_arrow_down, color: Colors.green),
              onTap: () => setState(() => _doneExpanded = !_doneExpanded),
            ),
            if (_doneExpanded)
              ...doneTasks.map((t) => _buildTaskCard(context, t)),
          ],
        ],
      ),
      floatingActionButton: FloatingActionButton(
        onPressed: () => Navigator.push(context, MaterialPageRoute(builder: (_) => const AddTaskScreen())),
        child: const Icon(Icons.add),
      ),
    );
  }

  Widget _buildSectionHeader(String title, Color color) {
    return Padding(
      padding: const EdgeInsets.symmetric(vertical: 8.0),
      child: Text(title, style: TextStyle(color: color, fontWeight: FontWeight.bold, fontSize: 18)),
    );
  }

  Widget _buildTaskCard(BuildContext context, Task task) {
    return TaskCard(
      task: task,
      onCheckedChange: () {
        final newStatus = task.status == TaskStatus.done ? TaskStatus.todo : TaskStatus.done;
        context.read<TaskProvider>().updateTaskStatus(
          task.id!,
          newStatus,
          onXpGain: (xp) => context.read<UserProvider>().addXp(xp),
        );
      },
      onClick: () => Navigator.push(context, MaterialPageRoute(builder: (_) => TaskDetailScreen(taskId: task.id!))),
    );
  }
}
