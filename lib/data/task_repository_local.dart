import 'dart:convert';
import 'package:shared_preferences/shared_preferences.dart';
import 'task.dart';
import 'task_repository.dart';

class LocalTaskRepository implements TaskRepository {
  static const _key = 'tasks_list';

  @override
  Future<List<Task>> fetchTasks() async {
    final prefs = await SharedPreferences.getInstance();
    final String? jsonString = prefs.getString(_key);
    
    if (jsonString == null) return [];
    
    final List<dynamic> decoded = jsonDecode(jsonString);
    return decoded.map((item) => Task.fromJson(item)).toList();
  }

  @override
  Future<void> saveTasks(List<Task> tasks) async {
    final prefs = await SharedPreferences.getInstance();
    final String jsonString = jsonEncode(tasks.map((t) => t.toJson()).toList());
    await prefs.setString(_key, jsonString);
  }
}