
class TodoItem {
  var title = "";
  var description = "";
  var icon = ""; // Used to set a specific category
  var dueDate = ""; // DateTime object
  var state = TodoItemState.todo;
  var dateTime = {}; // A dictionary of dateTimes
  var location = ""; // A Google Maps location
}

enum TodoItemState {
  done, // The task has been done
  todo, // The task has not yet been done
  overdue // The task is overdue, aka dueDate < todayDate
}