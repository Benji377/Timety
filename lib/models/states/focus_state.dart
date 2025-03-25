import 'package:timety/commons.dart';

class FocusState extends ChangeNotifier {
  var dailyFocusTime = 0; // The focus time we want to reach daily (seconds)
  var currentDailyFocusTime = 0; // The focus time we have reached today (seconds)
}