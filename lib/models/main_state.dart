import 'package:timety/commons.dart';

class MainState extends ChangeNotifier {
  String userName = "Benji";
  List<TaskItem> taskList = [];
  double dailyFocusTime = 0.0; // The focus time we want to reach daily (seconds)
  double currentDailyFocusTime = 0.0; // The focus time we have reached today (seconds)

  Map<String, double> getFocusDataMap() {
    // The PieChart we use requires a data map in this format
    return { "time": currentDailyFocusTime, "target": dailyFocusTime };
  }
}