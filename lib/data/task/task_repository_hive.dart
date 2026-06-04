import 'package:hive/hive.dart';
import 'task.dart';
import 'task_repository.dart';

class HiveTaskRepository implements TaskRepository {
  static const String boxName = 'tasksBox';

  @override
  Future<List<Task>> fetchTasks() async {
    final box = await Hive.openBox<Task>(boxName);
    return box.values.toList();
  }

  @override
  Future<void> saveTasks(List<Task> tasks) async {
    final box = await Hive.openBox<Task>(boxName);
    final Map<String, Task> taskMap = {for (var t in tasks) t.id: t};

    final keysToDelete = box.keys
        .where((key) => !taskMap.containsKey(key))
        .toList();

    if (keysToDelete.isNotEmpty) {
      await box.deleteAll(keysToDelete);
    }

    await box.putAll(taskMap);
  }
}
