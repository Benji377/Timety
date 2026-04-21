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
  late PageController _pageController;

  @override
  void initState() {
    super.initState();
    _pageController = PageController();
  }

  @override
  void dispose() {
    _pageController.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      body: PageView(
        controller: _pageController,
        onPageChanged: (index) => setState(() => _currentIndex = index),
        children: [
          _HomeScreenContent(pageController: _pageController),
          FocusScreen(),
          TasksScreen(),
          CalendarScreen(),
          StatsScreen(),
        ],
      ),
      bottomNavigationBar: BottomNavigationBar(
        currentIndex: _currentIndex,
        onTap: (index) {
          _pageController.animateToPage(
            index,
            duration: const Duration(milliseconds: 300),
            curve: Curves.easeInOut,
          );
        },
        type: BottomNavigationBarType.fixed,
        items: const [
          BottomNavigationBarItem(icon: Icon(Icons.home), label: 'Home'),
          BottomNavigationBarItem(icon: Icon(Icons.local_cafe), label: 'Focus'),
          BottomNavigationBarItem(icon: Icon(Icons.assignment), label: 'Tasks'),
          BottomNavigationBarItem(
            icon: Icon(Icons.calendar_month),
            label: 'Calendar',
          ),
          BottomNavigationBarItem(icon: Icon(Icons.person), label: 'Profile'),
        ],
      ),
    );
  }
}

class _HomeScreenContent extends StatelessWidget {
  final PageController pageController;

  const _HomeScreenContent({required this.pageController});

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
      return t.dueDate! >= today.millisecondsSinceEpoch &&
          t.dueDate! < tomorrow.millisecondsSinceEpoch;
    }).toList();

    final todayFocusTime = focusProvider
        .getSessionsForDay(today)
        .fold(0, (sum, s) => sum + s.duration);
    final focusTarget = user?.dailyFocusTarget ?? 7200000;
    final progress = focusTarget > 0 ? todayFocusTime / focusTarget : 0.0;

    return Scaffold(
      body: SafeArea(
        child: SingleChildScrollView(
          child: Padding(
            padding: const EdgeInsets.all(24.0),
            child: Column(
              mainAxisAlignment: MainAxisAlignment.start,
              children: [
                // Header with Greeting and Settings Button
                Row(
                  mainAxisAlignment: MainAxisAlignment.spaceBetween,
                  children: [
                    Expanded(
                      child: Text(
                        getGreeting(user?.name),
                        style: Theme.of(context).textTheme.headlineSmall,
                      ),
                    ),
                    IconButton(
                      onPressed: () => Navigator.push(
                        context,
                        MaterialPageRoute(
                          builder: (_) => const SettingsScreen(),
                        ),
                      ),
                      icon: const Icon(Icons.settings),
                      tooltip: 'Settings',
                    ),
                  ],
                ),
                const SizedBox(height: 32),

                // Focus Progress Circle - clickable
                GestureDetector(
                  onTap: () => Navigator.of(context).push(
                    PageRouteBuilder(
                      pageBuilder: (_, __, ___) => const FocusScreen(),
                      transitionsBuilder: (_, animation, __, child) {
                        return FadeTransition(opacity: animation, child: child);
                      },
                    ),
                  ),
                  child: Column(
                    children: [
                      RadialGraph(
                        progress: progress.clamp(0.0, 1.0),
                        text:
                            '${todayFocusTime ~/ 60000} / ${focusTarget ~/ 60000} min',
                        size: 180,
                      ),
                      const SizedBox(height: 8),
                      Text(
                        'Tap to start focusing',
                        style: Theme.of(context).textTheme.labelSmall?.copyWith(
                          color: Theme.of(context).colorScheme.outline,
                        ),
                      ),
                    ],
                  ),
                ),
                const SizedBox(height: 32),

                const Divider(),
                const SizedBox(height: 24),

                // Tasks Section
                Align(
                  alignment: Alignment.centerLeft,
                  child: Text(
                    "Today's Tasks",
                    style: Theme.of(context).textTheme.titleLarge,
                  ),
                ),
                const SizedBox(height: 12),
                if (todayTasks.isEmpty)
                  GestureDetector(
                    onTap: () {
                      pageController.animateToPage(
                        2,
                        duration: const Duration(milliseconds: 300),
                        curve: Curves.easeInOut,
                      );
                    },
                    child: Center(
                      child: Padding(
                        padding: const EdgeInsets.symmetric(vertical: 32),
                        child: Column(
                          children: [
                            Icon(
                              Icons.check_circle_outline,
                              size: 48,
                              color: Theme.of(context).colorScheme.primary,
                            ),
                            const SizedBox(height: 16),
                            Text(
                              'No tasks for today!\nTap to add some',
                              textAlign: TextAlign.center,
                              style: Theme.of(context).textTheme.bodyLarge
                                  ?.copyWith(
                                    color: Theme.of(
                                      context,
                                    ).colorScheme.primary,
                                  ),
                            ),
                          ],
                        ),
                      ),
                    ),
                  )
                else
                  ListView.builder(
                    shrinkWrap: true,
                    physics: const NeverScrollableScrollPhysics(),
                    itemCount: todayTasks.length,
                    itemBuilder: (context, index) {
                      final task = todayTasks[index];
                      return Padding(
                        padding: const EdgeInsets.only(bottom: 8),
                        child: TaskCard(
                          task: task,
                          onCheckedChange: () async {
                            context.read<TaskProvider>().updateTaskStatus(
                              task.id!,
                              TaskStatus.done,
                              onXpGain: (xp) =>
                                  context.read<UserProvider>().addXp(xp),
                            );

                            // Update streak when task is completed
                            await context
                                .read<UserProvider>()
                                .checkAndUpdateStreak(
                                  todayFocusMinutes: 0,
                                  completedTaskToday: true,
                                );
                          },
                          onClick: () => Navigator.push(
                            context,
                            MaterialPageRoute(
                              builder: (_) =>
                                  TaskDetailScreen(taskId: task.id!),
                            ),
                          ),
                        ),
                      );
                    },
                  ),
                const SizedBox(height: 24),
              ],
            ),
          ),
        ),
      ),
    );
  }
}
