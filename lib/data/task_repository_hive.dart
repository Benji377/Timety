import 'package:hive/hive.dart';
import 'task.dart';
import 'task_repository.dart';

class HiveTaskRepository implements TaskRepository {
  // Hive stores data in "Boxes" (similar to a SQL table)
  static const String boxName = 'tasksBox';

  @override
  Future<List<Task>> fetchTasks() async {
    // Open the box (if it's already open, this just returns it instantly)
    final box = await Hive.openBox<Task>(boxName);
    
    // Hive returns an Iterable, so we convert it to a List
    return box.values.toList();
  }

  @override
  Future<void> saveTasks(List<Task> tasks) async {
    final box = await Hive.openBox<Task>(boxName);
    
    // Clear old data and save the new list. 
    // TODO: optimize this to only save/update individual tasks!
    await box.clear();
    await box.addAll(tasks);
  }
}