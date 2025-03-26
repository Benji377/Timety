import 'package:timety/commons.dart';

class TaskItem {
  var title = "";
  var description = "";
  var icon = Icons.task; // Used to set a specific category
  var dueDate = ""; // DateTime object
  var state = TaskItemState.todo;
  var reminders = {}; // A dictionary of dateTimes
  var location = ""; // A Google Maps location
}

enum TaskItemState {
  done, // The task has been done
  todo, // The task has not yet been done
  overdue // The task is overdue, aka dueDate < todayDate
}