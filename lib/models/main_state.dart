import 'package:timety/commons.dart';
class MainState extends ChangeNotifier {
  String userName = "Benji"; // homepage
  List<TaskItem> taskList = []; // for taskpage
  double dailyFocusTime = 0.0; // The focus time we want to reach daily (seconds) //focuspage
  double currentDailyFocusTime = 0.0; // The focus time we have reached today (seconds)
  ValueNotifier<List<Event>> selectedEvents = ValueNotifier([]);

  void updateSelectedEvents(List<Event> events) { // for eventpage
    selectedEvents.value = events;
    notifyListeners();
  }
  //for focuspage
  Map<String, double> getFocusDataMap() {
    // The PieChart we use requires a data map in this format
    return { "time": currentDailyFocusTime, "target": dailyFocusTime };
  }
}
