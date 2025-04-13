import 'package:timety/commons.dart';

class FocusSummary {
  final double dailyFocusTime;
  final double currentDailyFocusTime;

  FocusSummary(this.dailyFocusTime, this.currentDailyFocusTime);

  double get progressPercentage => currentDailyFocusTime / dailyFocusTime;

  Map<String, double> getFocusDataMap() {
  // The PieChart we use requires a data map in this format
    return { "time": currentDailyFocusTime, "target": dailyFocusTime };
  }
}