import 'dart:async';
import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import '../providers/focus_provider.dart';
import '../providers/task_provider.dart';
import '../data/focus_mode.dart';
import '../data/focus_session.dart';
import '../data/task.dart';
import '../widgets/radial_graph.dart';
import 'focus_modes_screen.dart';

class FocusScreen extends StatefulWidget {
  const FocusScreen({super.key});

  @override
  State<FocusScreen> createState() => _FocusScreenState();
}

class _FocusScreenState extends State<FocusScreen> {
  FocusMode? _selectedMode;
  Task? _selectedTask;
  
  bool _isRunning = false;
  int _currentStepIndex = 0;
  int _secondsRemaining = 0;
  Timer? _timer;
  DateTime? _startTime;

  @override
  void dispose() {
    _timer?.cancel();
    super.dispose();
  }

  void _startFocus() {
    if (_selectedMode == null) return;
    
    setState(() {
      _isRunning = true;
      _currentStepIndex = 0;
      _secondsRemaining = _selectedMode!.steps[_currentStepIndex].durationMins * 60;
      _startTime = DateTime.now();
    });

    _timer = Timer.periodic(const Duration(seconds: 1), (timer) {
      if (_secondsRemaining > 0) {
        setState(() => _secondsRemaining--);
      } else {
        _nextStep();
      }
    });
  }

  void _nextStep() {
    if (_currentStepIndex < _selectedMode!.steps.length - 1) {
      setState(() {
        _currentStepIndex++;
        _secondsRemaining = _selectedMode!.steps[_currentStepIndex].durationMins * 60;
      });
    } else {
      _completeSession();
    }
  }

  void _completeSession() {
    _timer?.cancel();
    final endTime = DateTime.now();
    final duration = endTime.difference(_startTime!);

    if (_selectedMode != null) {
      context.read<FocusProvider>().addSession(
        FocusSession(
          categoryId: _selectedTask?.categoryId ?? 0, // Fallback category
          taskId: _selectedTask?.id,
          startTime: _startTime!.millisecondsSinceEpoch,
          endTime: endTime.millisecondsSinceEpoch,
          duration: duration.inMilliseconds,
          rating: FocusRating.great,
        ),
      );
    }

    setState(() => _isRunning = false);
    ScaffoldMessenger.of(context).showSnackBar(
      const SnackBar(content: Text('Focus Session Complete!')),
    );
  }

  @override
  Widget build(BuildContext context) {
    final focusProvider = context.watch<FocusProvider>();
    final taskProvider = context.watch<TaskProvider>();
    
    if (_isRunning) {
      final currentStep = _selectedMode!.steps[_currentStepIndex];
      final totalSeconds = currentStep.durationMins * 60;
      final progress = totalSeconds > 0 ? (_secondsRemaining / totalSeconds) : 0.0;
      
      return Scaffold(
        appBar: AppBar(title: Text(currentStep.type.name.toUpperCase())),
        body: Center(
          child: Column(
            mainAxisAlignment: MainAxisAlignment.center,
            children: [
              RadialGraph(
                progress: progress,
                text: '${_secondsRemaining ~/ 60}:${(_secondsRemaining % 60).toString().padLeft(2, '0')}',
                size: 250,
              ),
              const SizedBox(height: 40),
              Text(
                'Step ${_currentStepIndex + 1} of ${_selectedMode!.steps.length}',
                style: Theme.of(context).textTheme.titleLarge,
              ),
              const SizedBox(height: 20),
              ElevatedButton(
                onPressed: () {
                  _timer?.cancel();
                  setState(() => _isRunning = false);
                },
                style: ElevatedButton.styleFrom(backgroundColor: Colors.red, foregroundColor: Colors.white),
                child: const Text('STOP'),
              ),
            ],
          ),
        ),
      );
    }

    return Scaffold(
      appBar: AppBar(title: const Text('Focus Timer')),
      body: Padding(
        padding: const EdgeInsets.all(16.0),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.stretch,
          children: [
            DropdownButtonFormField<FocusMode>(
              decoration: const InputDecoration(labelText: 'Focus Mode', border: OutlineInputBorder()),
              initialValue: _selectedMode,
              items: focusProvider.focusModes.map((m) => DropdownMenuItem(value: m, child: Text(m.title))).toList(),
              onChanged: (val) => setState(() => _selectedMode = val),
            ),
            const SizedBox(height: 16),
            DropdownButtonFormField<Task>(
              decoration: const InputDecoration(labelText: 'Focus on Task (optional)', border: OutlineInputBorder()),
              initialValue: _selectedTask,
              items: taskProvider.todoTasks.map((t) => DropdownMenuItem(value: t, child: Text(t.title))).toList(),
              onChanged: (val) => setState(() => _selectedTask = val),
            ),
            const Spacer(),
            Row(
              children: [
                Expanded(
                  child: ElevatedButton(
                    onPressed: _selectedMode == null ? null : _startFocus,
                    style: ElevatedButton.styleFrom(padding: const EdgeInsets.all(20)),
                    child: const Text('START SESSION', style: TextStyle(fontSize: 18)),
                  ),
                ),
                const SizedBox(width: 8),
                IconButton.filledTonal(
                  onPressed: () => Navigator.push(context, MaterialPageRoute(builder: (_) => const FocusModesScreen())),
                  icon: const Icon(Icons.settings),
                  padding: const EdgeInsets.all(16),
                ),
              ],
            ),
          ],
        ),
      ),
    );
  }
}
