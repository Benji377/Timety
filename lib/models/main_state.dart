import 'package:timety/commons.dart';

final userNameProvider = Provider<String>((ref) {
  return "Benji";
});

class MainState extends ChangeNotifier {

  List<TaskItem> taskList = []; // for taskpage
  ValueNotifier<List<Event>> selectedEvents = ValueNotifier([]);

  void updateSelectedEvents(List<Event> events) { // for eventpage
    selectedEvents.value = events;
    notifyListeners();
  }

}
