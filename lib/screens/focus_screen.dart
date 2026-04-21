import 'dart:async';
import 'dart:math' as math;
import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import '../providers/focus_provider.dart';
import '../providers/task_provider.dart';
import '../providers/user_provider.dart';
import '../data/focus_mode.dart';
import '../data/focus_session.dart';
import '../data/task.dart';
import 'focus_modes_screen.dart';
import 'daily_stats_screen.dart';

class FocusScreen extends StatefulWidget {
  const FocusScreen({super.key});

  @override
  State<FocusScreen> createState() => _FocusScreenState();
}

class _FocusScreenState extends State<FocusScreen> {
  FocusMode? _selectedMode;
  Task? _selectedTask;
  String? _selectedTag;
  DateTime _selectedDate = DateTime.now();

  bool _isRunning = false;
  bool _isPaused = false;
  int _currentStepIndex = 0;
  int _secondsRemaining = 0;
  Timer? _timer;
  DateTime? _startTime;
  late PageController _modeCarouselController;

  @override
  void initState() {
    super.initState();
    _modeCarouselController = PageController();
  }

  @override
  void dispose() {
    _timer?.cancel();
    _modeCarouselController.dispose();
    super.dispose();
  }

  void _startFocus() {
    if (_selectedMode == null) return;

    setState(() {
      _isRunning = true;
      _currentStepIndex = 0;
      _secondsRemaining =
          _selectedMode!.steps[_currentStepIndex].durationMins * 60;
      _startTime = DateTime.now();
      _isPaused = false;
    });

    _timer = Timer.periodic(const Duration(seconds: 1), (timer) {
      if (_secondsRemaining > 0 && !_isPaused) {
        setState(() => _secondsRemaining--);
      } else if (_secondsRemaining == 0 && !_isPaused) {
        _nextStep();
      }
    });
  }

  void _pauseResume() {
    setState(() => _isPaused = !_isPaused);
  }

  void _cancelSession() {
    showDialog(
      context: context,
      builder: (context) => AlertDialog(
        title: const Text('Cancel Session?'),
        content: const Text('Are you sure you want to cancel this session?'),
        actions: [
          TextButton(
            onPressed: () => Navigator.pop(context),
            child: const Text('Keep Going'),
          ),
          TextButton(
            onPressed: () {
              _timer?.cancel();
              setState(() => _isRunning = false);
              Navigator.pop(context);
            },
            child: const Text('Cancel'),
          ),
        ],
      ),
    );
  }

  void _stopSession() {
    showDialog(
      context: context,
      builder: (context) => AlertDialog(
        title: const Text('End Session?'),
        content: const Text('Are you sure you want to end this session now?'),
        actions: [
          TextButton(
            onPressed: () => Navigator.pop(context),
            child: const Text('Keep Going'),
          ),
          TextButton(
            onPressed: () {
              _completeSession();
              Navigator.pop(context);
            },
            child: const Text('End Session'),
          ),
        ],
      ),
    );
  }

  void _nextStep() {
    if (_currentStepIndex < _selectedMode!.steps.length - 1) {
      setState(() {
        _currentStepIndex++;
        _secondsRemaining =
            _selectedMode!.steps[_currentStepIndex].durationMins * 60;
      });
    } else {
      _completeSession();
    }
  }

  void _completeSession() async {
    _timer?.cancel();
    final endTime = DateTime.now();
    final duration = endTime.difference(_startTime!);
    final durationMinutes = duration.inMinutes;

    // Award XP: 10 XP per minute of focus
    const int xpPerMinute = 10;
    int xpEarned = 0;

    if (_selectedMode != null) {
      // Add the session to database
      await context.read<FocusProvider>().addSession(
        FocusSession(
          categoryId: _selectedTask?.categoryId ?? 0,
          taskId: _selectedTask?.id,
          startTime: _startTime!.millisecondsSinceEpoch,
          endTime: endTime.millisecondsSinceEpoch,
          duration: duration.inMilliseconds,
          rating: FocusRating.great,
        ),
      );

      xpEarned = durationMinutes * xpPerMinute;

      final userProvider = context.read<UserProvider>();
      final user = userProvider.user;

      if (user != null) {
        // Add XP with streak bonus
        await userProvider.addXp(xpEarned, streakBonus: user.currentStreak);

        // Check and update streak
        await userProvider.checkAndUpdateStreak(
          todayFocusMinutes: durationMinutes,
          completedTaskToday: false,
        );
      }
    }

    setState(() => _isRunning = false);
    ScaffoldMessenger.of(context).showSnackBar(
      SnackBar(
        content: Text('Focus Session Complete! +$xpEarned XP'),
        duration: const Duration(seconds: 3),
      ),
    );
  }

  void _showTimeMachine() {
    TimeOfDay? selectedTime;
    int durationMins = 30;
    final durationController = TextEditingController(text: '30');

    showDialog(
      context: context,
      builder: (context) => StatefulBuilder(
        builder: (context, setState) => AlertDialog(
          title: const Text('Log Past Session'),
          content: Column(
            mainAxisSize: MainAxisSize.min,
            children: [
              Row(
                children: [
                  const Text('Start Time: '),
                  const Spacer(),
                  TextButton(
                    onPressed: () async {
                      final time = await showTimePicker(
                        context: context,
                        initialTime: TimeOfDay.now(),
                      );
                      if (time != null) {
                        setState(() => selectedTime = time);
                      }
                    },
                    child: Text(selectedTime?.format(context) ?? 'Select'),
                  ),
                ],
              ),
              const SizedBox(height: 16),
              Row(
                children: [
                  const Text('Duration (mins): '),
                  const Spacer(),
                  SizedBox(
                    width: 100,
                    child: TextField(
                      controller: durationController,
                      keyboardType: TextInputType.number,
                      onChanged: (val) {
                        durationMins = int.tryParse(val) ?? 30;
                      },
                    ),
                  ),
                ],
              ),
              const SizedBox(height: 16),
              if (selectedTime != null)
                Text(
                  'Ends at: ${selectedTime!.format(context)}',
                  style: const TextStyle(fontSize: 12, color: Colors.grey),
                ),
            ],
          ),
          actions: [
            TextButton(
              onPressed: () => Navigator.pop(context),
              child: const Text('Cancel'),
            ),
            TextButton(
              onPressed: selectedTime != null
                  ? () {
                      // Calculate start and end times
                      final now = DateTime.now();
                      final startDateTime = DateTime(
                        now.year,
                        now.month,
                        now.day,
                        selectedTime!.hour,
                        selectedTime!.minute,
                      );
                      final endDateTime = startDateTime.add(
                        Duration(minutes: durationMins),
                      );

                      // If end time is in the future, start a timer with remaining duration
                      if (endDateTime.isAfter(now)) {
                        final remainingMins = endDateTime
                            .difference(now)
                            .inMinutes;
                        _selectedMode = context
                            .read<FocusProvider>()
                            .focusModes
                            .firstWhere(
                              (m) => m.title == 'Flexible',
                              orElse: () => context
                                  .read<FocusProvider>()
                                  .focusModes
                                  .first,
                            );

                        setState(() {
                          _startTime = now;
                          _isRunning = true;
                          _currentStepIndex = 0;
                          _secondsRemaining = remainingMins * 60;
                          _isPaused = false;
                        });

                        _timer = Timer.periodic(const Duration(seconds: 1), (
                          timer,
                        ) {
                          if (_secondsRemaining > 0) {
                            setState(() => _secondsRemaining--);
                          } else {
                            _completeSession();
                          }
                        });
                      } else {
                        // Log past session immediately
                        context.read<FocusProvider>().addSession(
                          FocusSession(
                            categoryId: _selectedTask?.categoryId ?? 0,
                            taskId: _selectedTask?.id,
                            startTime: startDateTime.millisecondsSinceEpoch,
                            endTime: endDateTime.millisecondsSinceEpoch,
                            duration: Duration(
                              minutes: durationMins,
                            ).inMilliseconds,
                            rating: FocusRating.okay,
                          ),
                        );

                        // Award XP
                        const int xpPerMinute = 10;
                        final int xpEarned = durationMins * xpPerMinute;
                        final userProvider = context.read<UserProvider>();
                        final user = userProvider.user;

                        if (user != null) {
                          userProvider.addXp(
                            xpEarned,
                            streakBonus: user.currentStreak,
                          );
                        }
                      }

                      Navigator.pop(context);
                    }
                  : null,
              child: const Text('Log'),
            ),
          ],
        ),
      ),
    );
  }

  void _showAlerts() {
    final alerts = ['Distracted', 'Stretch', 'Drink', 'Rest'];
    final selectedAlerts = <String>[];

    showDialog(
      context: context,
      builder: (context) => StatefulBuilder(
        builder: (context, setState) => AlertDialog(
          title: const Text('Log Event'),
          content: Column(
            mainAxisSize: MainAxisSize.min,
            children: alerts
                .map(
                  (alert) => CheckboxListTile(
                    title: Text(alert),
                    value: selectedAlerts.contains(alert),
                    onChanged: (val) {
                      setState(() {
                        if (val == true) {
                          selectedAlerts.add(alert);
                        } else {
                          selectedAlerts.remove(alert);
                        }
                      });
                    },
                  ),
                )
                .toList(),
          ),
          actions: [
            TextButton(
              onPressed: () => Navigator.pop(context),
              child: const Text('Cancel'),
            ),
            TextButton(
              onPressed: selectedAlerts.isNotEmpty
                  ? () {
                      // Log events to timeline
                      ScaffoldMessenger.of(context).showSnackBar(
                        SnackBar(
                          content: Text('Logged: ${selectedAlerts.join(", ")}'),
                          duration: const Duration(seconds: 2),
                        ),
                      );
                      Navigator.pop(context);
                    }
                  : null,
              child: const Text('Add'),
            ),
          ],
        ),
      ),
    );
  }

  void _showTagSelector() {
    final taskProvider = context.read<TaskProvider>();
    final categories = taskProvider.categories;

    showDialog(
      context: context,
      builder: (context) => AlertDialog(
        title: const Text('Select Category'),
        content: SingleChildScrollView(
          child: Column(
            mainAxisSize: MainAxisSize.min,
            children: categories
                .map(
                  (cat) => ListTile(
                    title: Text(cat.name),
                    leading: Container(
                      width: 16,
                      height: 16,
                      decoration: BoxDecoration(
                        color: Color(int.parse(cat.colorHex)),
                        shape: BoxShape.circle,
                      ),
                    ),
                    onTap: () {
                      setState(() => _selectedTag = cat.name);
                      Navigator.pop(context);
                    },
                  ),
                )
                .toList(),
          ),
        ),
      ),
    );
  }

  void _showDurationPicker() {
    if (!_isRunning) {
      showDialog(
        context: context,
        builder: (context) {
          int tempMinutes = (_secondsRemaining ~/ 60);
          final tempController = TextEditingController(
            text: tempMinutes.toString(),
          );

          return AlertDialog(
            title: const Text('Set Duration'),
            content: Row(
              mainAxisSize: MainAxisSize.min,
              children: [
                IconButton(
                  onPressed: () => setState(() {
                    if (tempMinutes > 1) {
                      tempMinutes--;
                      tempController.text = tempMinutes.toString();
                    }
                  }),
                  icon: const Icon(Icons.remove),
                ),
                Expanded(
                  child: TextField(
                    controller: tempController,
                    keyboardType: TextInputType.number,
                    textAlign: TextAlign.center,
                    onChanged: (val) {
                      tempMinutes = int.tryParse(val) ?? tempMinutes;
                    },
                  ),
                ),
                IconButton(
                  onPressed: () => setState(() {
                    if (tempMinutes < 120) {
                      tempMinutes++;
                      tempController.text = tempMinutes.toString();
                    }
                  }),
                  icon: const Icon(Icons.add),
                ),
              ],
            ),
            actions: [
              TextButton(
                onPressed: () => Navigator.pop(context),
                child: const Text('Cancel'),
              ),
              TextButton(
                onPressed: () {
                  setState(() => _secondsRemaining = tempMinutes * 60);
                  Navigator.pop(context);
                },
                child: const Text('Set'),
              ),
            ],
          );
        },
      );
    }
  }

  @override
  Widget build(BuildContext context) {
    final focusProvider = context.watch<FocusProvider>();
    final taskProvider = context.watch<TaskProvider>();
    final userProvider = context.watch<UserProvider>();

    if (_isRunning && _selectedMode != null) {
      final currentStep = _selectedMode!.steps[_currentStepIndex];
      final totalSeconds = currentStep.durationMins * 60;
      final progress = totalSeconds > 0
          ? ((_secondsRemaining / totalSeconds))
          : 0.0;
      final displayTime =
          '${_secondsRemaining ~/ 60}:${(_secondsRemaining % 60).toString().padLeft(2, '0')}';

      return Scaffold(
        body: SafeArea(
          child: Padding(
            padding: const EdgeInsets.all(16),
            child: Column(
              children: [
                // Header with back button
                Row(
                  children: [
                    IconButton(
                      icon: const Icon(Icons.arrow_back),
                      onPressed: _cancelSession,
                    ),
                    const Spacer(),
                    Text(
                      'Step ${_currentStepIndex + 1}/${_selectedMode!.steps.length}',
                      style: Theme.of(context).textTheme.titleSmall,
                    ),
                  ],
                ),
                const SizedBox(height: 24),
                // State and timer display
                Expanded(
                  child: Center(
                    child: Column(
                      mainAxisAlignment: MainAxisAlignment.center,
                      children: [
                        Text(
                          currentStep.type.name.toUpperCase(),
                          style: Theme.of(context).textTheme.headlineSmall
                              ?.copyWith(
                                color: currentStep.type.name == 'focus'
                                    ? Colors.green
                                    : Colors.orange,
                              ),
                        ),
                        const SizedBox(height: 40),
                        // Circular timer visualization
                        CustomPaint(
                          size: const Size(280, 280),
                          painter: _TimerPainter(
                            progress: progress,
                            isFocus: currentStep.type.name == 'focus',
                          ),
                          child: Center(
                            child: Column(
                              mainAxisAlignment: MainAxisAlignment.center,
                              children: [
                                GestureDetector(
                                  onTap: _showDurationPicker,
                                  child: Text(
                                    displayTime,
                                    style: Theme.of(context)
                                        .textTheme
                                        .displaySmall
                                        ?.copyWith(
                                          fontWeight: FontWeight.bold,
                                          fontSize: 60,
                                        ),
                                  ),
                                ),
                                const SizedBox(height: 12),
                                if (_selectedTag != null)
                                  Text(
                                    _selectedTag!,
                                    style: Theme.of(
                                      context,
                                    ).textTheme.labelSmall,
                                  ),
                              ],
                            ),
                          ),
                        ),
                      ],
                    ),
                  ),
                ),
                const SizedBox(height: 24),
                // Control buttons
                Row(
                  mainAxisAlignment: MainAxisAlignment.spaceEvenly,
                  children: [
                    // Cancel button
                    CircularButton(
                      icon: Icons.close,
                      color: Colors.red,
                      onPressed: _cancelSession,
                    ),
                    // Pause/Resume button
                    CircularButton(
                      icon: _isPaused ? Icons.play_arrow : Icons.pause,
                      color: Colors.orange,
                      onPressed: _pauseResume,
                    ),
                    // Stop button
                    CircularButton(
                      icon: Icons.stop,
                      color: Colors.green,
                      onPressed: _stopSession,
                    ),
                  ],
                ),
              ],
            ),
          ),
        ),
      );
    }

    return Scaffold(
      body: SafeArea(
        child: Column(
          children: [
            // Header with daily stats
            Padding(
              padding: const EdgeInsets.all(16),
              child: Row(
                mainAxisAlignment: MainAxisAlignment.spaceBetween,
                children: [
                  Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      Text(
                        'Focus',
                        style: Theme.of(context).textTheme.headlineSmall,
                      ),
                      const SizedBox(height: 8),
                      Text(
                        'Minutes Today',
                        style: Theme.of(context).textTheme.labelSmall,
                      ),
                    ],
                  ),
                  GestureDetector(
                    onTap: () => Navigator.push(
                      context,
                      MaterialPageRoute(
                        builder: (_) => const DailyStatsScreen(),
                      ),
                    ),
                    child: Column(
                      crossAxisAlignment: CrossAxisAlignment.end,
                      children: [
                        Text(
                          '0 / ${(userProvider.user?.dailyFocusTarget ?? 0) ~/ 60000}',
                          style: Theme.of(context).textTheme.headlineSmall
                              ?.copyWith(fontWeight: FontWeight.bold),
                        ),
                        const Icon(Icons.chevron_right, size: 16),
                      ],
                    ),
                  ),
                ],
              ),
            ),
            // Day navigation
            Padding(
              padding: const EdgeInsets.symmetric(horizontal: 16),
              child: Row(
                children: [
                  IconButton(
                    icon: const Icon(Icons.chevron_left),
                    onPressed: () {
                      setState(
                        () => _selectedDate = _selectedDate.subtract(
                          const Duration(days: 1),
                        ),
                      );
                    },
                  ),
                  Expanded(
                    child: Center(
                      child: Text(
                        '${_selectedDate.weekday == 1
                            ? 'Monday'
                            : _selectedDate.weekday == 2
                            ? 'Tuesday'
                            : _selectedDate.weekday == 3
                            ? 'Wednesday'
                            : _selectedDate.weekday == 4
                            ? 'Thursday'
                            : _selectedDate.weekday == 5
                            ? 'Friday'
                            : _selectedDate.weekday == 6
                            ? 'Saturday'
                            : 'Sunday'}, ${_selectedDate.month}/${_selectedDate.day}',
                        style: Theme.of(context).textTheme.titleMedium,
                      ),
                    ),
                  ),
                  IconButton(
                    icon: const Icon(Icons.chevron_right),
                    onPressed: () {
                      setState(
                        () => _selectedDate = _selectedDate.add(
                          const Duration(days: 1),
                        ),
                      );
                    },
                  ),
                ],
              ),
            ),
            const SizedBox(height: 16),
            // Mode carousel
            if (focusProvider.focusModes.isNotEmpty) ...[
              SizedBox(
                height: 200,
                child: PageView.builder(
                  controller: _modeCarouselController,
                  onPageChanged: (index) {
                    setState(() {
                      _selectedMode = focusProvider.focusModes[index];
                      _currentStepIndex = 0;
                    });
                  },
                  itemCount: focusProvider.focusModes.length,
                  itemBuilder: (context, index) {
                    final mode = focusProvider.focusModes[index];
                    final isSelected = _selectedMode?.id == mode.id;
                    return _buildModeCard(mode, isSelected);
                  },
                ),
              ),
              const SizedBox(height: 16),
              // Dots indicator
              Row(
                mainAxisAlignment: MainAxisAlignment.center,
                children: List.generate(
                  focusProvider.focusModes.length,
                  (index) => Container(
                    margin: const EdgeInsets.symmetric(horizontal: 4),
                    width: 8,
                    height: 8,
                    decoration: BoxDecoration(
                      shape: BoxShape.circle,
                      color:
                          _selectedMode?.id ==
                              focusProvider.focusModes[index].id
                          ? Theme.of(context).primaryColor
                          : Colors.grey[300],
                    ),
                  ),
                ),
              ),
              const SizedBox(height: 16),
            ],
            // Control buttons (Time Machine and Alerts)
            Padding(
              padding: const EdgeInsets.symmetric(horizontal: 16),
              child: Row(
                children: [
                  CircularButton(
                    icon: Icons.schedule,
                    tooltip: 'Log past session',
                    onPressed: _showTimeMachine,
                  ),
                  const Spacer(),
                  CircularButton(
                    icon: Icons.notifications_active,
                    tooltip: 'Log event',
                    onPressed: _showAlerts,
                  ),
                ],
              ),
            ),
            const SizedBox(height: 16),
            // Task selection
            Padding(
              padding: const EdgeInsets.symmetric(horizontal: 16),
              child: DropdownButtonFormField<Task>(
                decoration: const InputDecoration(
                  labelText: 'Focus on Task (optional)',
                  border: OutlineInputBorder(),
                ),
                value: _selectedTask,
                items: taskProvider.todoTasks
                    .map(
                      (t) => DropdownMenuItem(value: t, child: Text(t.title)),
                    )
                    .toList(),
                onChanged: (val) => setState(() => _selectedTask = val),
              ),
            ),
            const Spacer(),
            // Start button
            Padding(
              padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 16),
              child: ElevatedButton(
                onPressed: _selectedMode == null ? null : _startFocus,
                style: ElevatedButton.styleFrom(
                  padding: const EdgeInsets.symmetric(vertical: 16),
                  minimumSize: const Size.fromHeight(50),
                ),
                child: const Text(
                  'START SESSION',
                  style: TextStyle(fontSize: 16),
                ),
              ),
            ),
          ],
        ),
      ),
    );
  }

  Widget _buildModeCard(FocusMode mode, bool isSelected) {
    return GestureDetector(
      onTap: () => setState(() => _selectedMode = mode),
      child: Card(
        margin: const EdgeInsets.symmetric(horizontal: 16),
        color: isSelected
            ? Theme.of(context).primaryColor.withOpacity(0.1)
            : null,
        child: Padding(
          padding: const EdgeInsets.all(16),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Row(
                mainAxisAlignment: MainAxisAlignment.spaceBetween,
                children: [
                  GestureDetector(
                    onTap: () => Navigator.push(
                      context,
                      MaterialPageRoute(
                        builder: (_) => const FocusModesScreen(),
                      ),
                    ),
                    child: Text(
                      mode.title,
                      style: Theme.of(context).textTheme.titleLarge?.copyWith(
                        color: isSelected
                            ? Theme.of(context).primaryColor
                            : null,
                      ),
                    ),
                  ),
                  if (isSelected)
                    const Icon(Icons.check_circle, color: Colors.green),
                ],
              ),
              const SizedBox(height: 12),
              Expanded(
                child: Wrap(
                  spacing: 8,
                  runSpacing: 8,
                  children: mode.steps
                      .map(
                        (step) => Chip(
                          label: Text(
                            '${step.durationMins}m ${step.type.name}',
                            style: const TextStyle(fontSize: 12),
                          ),
                          backgroundColor: step.type.name == 'focus'
                              ? Colors.green.withOpacity(0.2)
                              : Colors.orange.withOpacity(0.2),
                        ),
                      )
                      .toList(),
                ),
              ),
            ],
          ),
        ),
      ),
    );
  }
}

class _TimerPainter extends CustomPainter {
  final double progress;
  final bool isFocus;

  _TimerPainter({required this.progress, required this.isFocus});

  @override
  void paint(Canvas canvas, Size size) {
    final center = Offset(size.width / 2, size.height / 2);
    final radius = size.width / 2 - 10;

    // Draw background circle
    canvas.drawCircle(
      center,
      radius,
      Paint()
        ..color = Colors.grey[300]!
        ..style = PaintingStyle.stroke
        ..strokeWidth = 20,
    );

    // Draw progress arc
    final progressColor = isFocus ? Colors.green : Colors.orange;
    canvas.drawArc(
      Rect.fromCircle(center: center, radius: radius),
      -math.pi / 2,
      (2 * math.pi * progress),
      false,
      Paint()
        ..color = progressColor
        ..style = PaintingStyle.stroke
        ..strokeWidth = 20
        ..strokeCap = StrokeCap.round,
    );
  }

  @override
  bool shouldRepaint(covariant CustomPainter oldDelegate) => true;
}

class CircularButton extends StatelessWidget {
  final IconData icon;
  final VoidCallback onPressed;
  final Color? color;
  final String? tooltip;

  const CircularButton({
    Key? key,
    required this.icon,
    required this.onPressed,
    this.color,
    this.tooltip,
  }) : super(key: key);

  @override
  Widget build(BuildContext context) {
    return Tooltip(
      message: tooltip ?? '',
      child: FloatingActionButton(
        backgroundColor: color,
        onPressed: onPressed,
        child: Icon(icon),
      ),
    );
  }
}
