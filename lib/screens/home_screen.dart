import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import '../providers/user_provider.dart';
import '../providers/task_provider.dart';
import '../providers/focus_provider.dart';
import '../widgets/radial_graph.dart';
import '../widgets/task_card.dart';
import '../data/task.dart';
import 'settings_screen.dart';
import 'focus_screen.dart';
import 'task_detail_screen.dart';
import 'tasks_screen.dart';
import 'stats_screen.dart';
import 'calendar_screen.dart';

class HomeScreen extends StatefulWidget {
  const HomeScreen({super.key});

  @override
  State<HomeScreen> createState() => _HomeScreenState();
}

class _HomeScreenState extends State<HomeScreen> {
  int _currentIndex = 0;
  final PageController _pageController = PageController();

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      body: PageView(
        controller: _pageController,
        onPageChanged: (index) => setState(() => _currentIndex = index),
        children: [
          const _HomeScreenContent(),
          const FocusScreen(),
          const TasksScreen(),
          const CalendarScreen(),
          const StatsScreen(),
        ],
      ),
      bottomNavigationBar: BottomNavigationBar(
        currentIndex: _currentIndex,
        onTap: (index) {
          _pageController.animateToPage(index, duration: const Duration(milliseconds: 300), curve: Curves.easeInOut);
        },
        type: BottomNavigationBarType.fixed,
        items: const [
          BottomNavigationBarItem(icon: Icon(Icons.home), label: 'Home'),
          BottomNavigationBarItem(icon: Icon(Icons.timer), label: 'Focus'),
          BottomNavigationBarItem(icon: Icon(Icons.list), label: 'Tasks'),
          BottomNavigationBarItem(icon: Icon(Icons.calendar_month), label: 'Calendar'),
          BottomNavigationBarItem(icon: Icon(Icons.person), label: 'User'),
        ],
      ),
    );
  }
}

class _HomeScreenContent extends StatelessWidget {
  const _HomeScreenContent();

  String getGreeting(String? userName) {
    final hour = DateTime.now().hour;
    if (hour < 12) return 'Good morning, ${userName ?? "Hero"}';
    if (hour < 17) return 'Good afternoon, ${userName ?? "Hero"}';
    if (hour < 21) return 'Good evening, ${userName ?? "Hero"}';
    return 'Good night, ${userName ?? "Hero"}';
  }

  @override
  Widget build(BuildContext context) {
    final userProvider = context.watch<UserProvider>();
    final taskProvider = context.watch<TaskProvider>();
    final focusProvider = context.watch<FocusProvider>();

    final user = userProvider.user;
    final now = DateTime.now();
    final today = DateTime(now.year, now.month, now.day);
    final tomorrow = today.add(const Duration(days: 1));

    final todayTasks = taskProvider.todoTasks.where((t) {
      if (t.dueDate == null) return false;
      return t.dueDate! >= today.millisecondsSinceEpoch && t.dueDate! < tomorrow.millisecondsSinceEpoch;
    }).toList();

    final todayFocusTime = focusProvider.getSessionsForDay(today).fold(0, (sum, s) => sum + s.duration);
    final focusTarget = user?.dailyFocusTarget ?? 7200000;
    final progress = focusTarget > 0 ? todayFocusTime / focusTarget : 0.0;

    return Scaffold(
      appBar: AppBar(
        title: const Text('Timety'),
        actions: [
          IconButton(
            icon: const Icon(Icons.settings),
            onPressed: () => Navigator.push(context, MaterialPageRoute(builder: (_) => const SettingsScreen())),
          ),
        ],
      ),
      body: Padding(
        padding: const EdgeInsets.all(16.0),
        child: Column(
          children: [
            Text(getGreeting(user?.name), style: Theme.of(context).textTheme.headlineMedium),
            const SizedBox(height: 24),
            RadialGraph(
              progress: progress.clamp(0.0, 1.0),
              text: '${todayFocusTime ~/ 60000} / ${focusTarget ~/ 60000} min',
            ),
            const Divider(height: 32),
            Align(
              alignment: Alignment.centerLeft,
              child: Text("Today's Tasks", style: Theme.of(context).textTheme.titleLarge),
            ),
            const SizedBox(height: 8),
            Expanded(
              child: todayTasks.isEmpty
                  ? Center(
                      child: Text(
                        "No tasks for today!",
                        style: Theme.of(context).textTheme.bodyLarge?.copyWith(color: Theme.of(context).colorScheme.primary),
                      ),
                    )
                  : ListView.builder(
                      itemCount: todayTasks.length,
                      itemBuilder: (context, index) {
                        final task = todayTasks[index];
                        return TaskCard(
                          task: task,
                          onCheckedChange: () {
                            context.read<TaskProvider>().updateTaskStatus(
                              task.id!,
                              TaskStatus.done,
                              onXpGain: (xp) => context.read<UserProvider>().addXp(xp),
                            );
                          },
                          onClick: () => Navigator.push(context, MaterialPageRoute(builder: (_) => TaskDetailScreen(taskId: task.id!))),
                        );
                      },
                    ),
            ),
          ],
        ),
      ),
    );
  }
}
