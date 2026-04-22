import 'package:flutter/material.dart';
import '../data/task.dart';
import '../data/task_repository.dart';

class TaskProvider extends ChangeNotifier {
  final TaskRepository repository;
  List<Task> _tasks = [];

  List<Task> get tasks => _tasks;

  TaskProvider({required this.repository});

  // Load data initially
  Future<void> loadTasks() async {
    _tasks = await repository.fetchTasks();
    notifyListeners(); // Tells the UI to rebuild
  }

  // Add a new Task
  Future<void> addTask(String title, String description) async {
    final newTask = Task(id: DateTime.now().toString(), title: title, description: description);
    _tasks.add(newTask);
    await repository.saveTasks(_tasks);
    notifyListeners();
  }

  // Toggle completion
  Future<void> toggleTask(String id) async {
    final index = _tasks.indexWhere((t) => t.id == id);
    if (index != -1) {
      _tasks[index].isCompleted = !_tasks[index].isCompleted;
      await repository.saveTasks(_tasks);
      notifyListeners();
    }
  }

// Remove a Task
  Future<void> removeTask(String id) async {
    _tasks.removeWhere((task) => task.id == id);
    await repository.saveTasks(_tasks);
    notifyListeners();
  }

  // Update an existing Task
  Future<void> updateTask(Task updatedTask) async {
    final index = _tasks.indexWhere((t) => t.id == updatedTask.id);
    if (index != -1) {
      _tasks[index] = updatedTask;
      await repository.saveTasks(_tasks);
      notifyListeners();
    }
  }
}