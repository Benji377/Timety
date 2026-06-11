import 'package:hive/hive.dart';
import 'task.dart';
import 'task_repository.dart';

class HiveTaskRepository implements TaskRepository {
  static const String boxName = 'tasksBox';
  
  Future<Box<Task>> get _box async => await Hive.openBox<Task>(boxName);

  @override
  Future<List<Task>> fetchTasks() async {
    final box = await _box;
    return box.values.toList();
  }

  @override
  Future<void> saveTask(Task task) async {
    final box = await _box;
    await box.put(task.id, task);
  }

  @override
  Future<void> deleteTask(String id) async {
    final box = await _box;
    await box.delete(id);
  }

  @override
  Future<void> clearAll() async {
    final box = await _box;
    await box.clear();
  }
}
