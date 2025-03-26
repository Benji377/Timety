import 'package:timety/commons.dart';

class TodoList extends ChangeNotifier {
  var todoList = <TodoItem, bool>{}; // A dictionary with key = todoItem and value = expanded (bool)

  void toggleExpanded(TodoItem tItem) {
    // Iterate through the list and set the given item as expanded
    for (var item in todoList.keys) {
      if (item == tItem) {
        todoList[item] = true;
      } else {
        // Only one item should be expanded at a time
        todoList[item] = false;
      }
    }
  }
}