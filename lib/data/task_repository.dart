import 'task.dart';

abstract class TaskRepository {
  Future<List<Task>> fetchTasks();
  Future<void> saveTasks(List<Task> tasks);
}