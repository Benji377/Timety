import 'package:flutter/material.dart';
import '../data/main_repository.dart';
import '../data/task.dart';
import '../data/category.dart';

class TaskProvider with ChangeNotifier {
  final MainRepository _repository;
  List<Task> _allTasks = [];
  List<Category> _categories = [];

  TaskProvider(this._repository) {
    refreshAll();
  }

  List<Task> get allTasks => _allTasks;
  List<Category> get categories => _categories;
  List<Task> get todoTasks => _allTasks.where((t) => t.status == TaskStatus.todo).toList();
  List<Task> get doneTasks => _allTasks.where((t) => t.status == TaskStatus.done).toList();
  List<Task> get overdueTasks => _allTasks.where((t) => t.status == TaskStatus.overdue).toList();

  Future<void> refreshAll() async {
    _allTasks = await _repository.getAllTasks();
    _categories = await _repository.getAllCategories();
    notifyListeners();
  }

  Future<void> addTask(Task task) async {
    await _repository.insertTask(task);
    await refreshAll();
  }

  Future<void> updateTask(Task task) async {
    await _repository.updateTask(task);
    await refreshAll();
  }

  Future<void> updateTaskStatus(int taskId, TaskStatus status, {required Function(int) onXpGain}) async {
    final task = _allTasks.firstWhere((t) => t.id == taskId);
    final isCompleting = status == TaskStatus.done && task.status != TaskStatus.done;
    
    await _repository.updateTaskStatus(taskId, status);
    
    if (isCompleting) {
      int baseXp = 50;
      double priorityMult = 1.0;
      switch (task.priority) {
        case TaskPriority.urgent: priorityMult = 2.0; break;
        case TaskPriority.high: priorityMult = 1.5; break;
        case TaskPriority.medium: priorityMult = 1.0; break;
        case TaskPriority.low: priorityMult = 1.0; break;
      }
      
      double sizeMult = 1.0;
      switch (task.size) {
        case TaskSize.xlarge: sizeMult = 2.0; break;
        case TaskSize.large: sizeMult = 1.5; break;
        case TaskSize.medium: sizeMult = 1.0; break;
        case TaskSize.small: sizeMult = 0.75; break;
        case TaskSize.tiny: sizeMult = 0.5; break;
      }
      
      final xpGained = (baseXp * priorityMult * sizeMult).toInt();
      onXpGain(xpGained);
    }
    
    await refreshAll();
  }

  Future<void> deleteTask(int taskId) async {
    await _repository.deleteTask(taskId);
    await refreshAll();
  }

  Future<void> addCategory(Category category) async {
    await _repository.insertCategory(category);
    await refreshAll();
  }

  Future<void> updateCategory(Category category) async {
    await _repository.updateCategory(category);
    await refreshAll();
  }

  Future<void> deleteCategory(int categoryId) async {
    await _repository.deleteCategory(categoryId);
    await refreshAll();
  }

  Future<void> updateOverdueTasks() async {
    await _repository.updateOverdueTasks(DateTime.now().millisecondsSinceEpoch);
    await refreshAll();
  }
}
