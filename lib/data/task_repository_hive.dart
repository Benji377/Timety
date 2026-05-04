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

    // 1. Convert the List into a Map where the Key is the Task ID
    final Map<String, Task> taskMap = {for (var t in tasks) t.id: t};

    // 2. Find any keys in the database that are NOT in the new list (meaning the user deleted them)
    final keysToDelete = box.keys
        .where((key) => !taskMap.containsKey(key))
        .toList();

    // 3. Batch delete removed tasks
    if (keysToDelete.isNotEmpty) {
      await box.deleteAll(keysToDelete);
    }

    // 4. Batch update/insert remaining tasks.
    // Because we use t.id as the key, Hive will only overwrite tasks that actually changed!
    await box.putAll(taskMap);
  }
}
