import 'package:timety/commons.dart';

class TaskItem {
  String title = "";
  String description = "";
  IconData icon = Icons.task; // Used to set a specific category
  DateTime? dueDate; // DateTime object
  TaskItemState state = TaskItemState.todo;
  List<DateTime> reminders = []; // A dictionary of dateTimes
  Uri? location; // A Google Maps location
}

enum TaskItemState {
  done, // The task has been done
  todo, // The task has not yet been done
  overdue // The task is overdue, aka dueDate < todayDate
}