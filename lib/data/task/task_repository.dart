import 'task.dart';

/// Repository interface for managing tasks
abstract class TaskRepository {
  /// Fetches all tasks from the data source.
  Future<List<Task>> fetchTasks();

  /// Saves or updates a single task.
  Future<void> saveTask(Task task);

  /// Deletes a single task by ID.
  Future<void> deleteTask(String id);

  /// Clears all tasks (useful for testing or data resets).
  Future<void> clearAll();
}
