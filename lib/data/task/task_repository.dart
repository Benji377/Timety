import 'task.dart';

/// Repository interface for managing tasks
abstract class TaskRepository {
  /// Fetches all tasks from the data source.
  Future<List<Task>> fetchTasks();
  /// Saves a list of tasks to the data source. Existing tasks will be updated.
  Future<void> saveTasks(List<Task> tasks);
}
