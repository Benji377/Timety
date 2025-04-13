import 'package:timety/commons.dart';

class TasksPage extends StatefulWidget {
  @override
  State<TasksPage> createState() => _TasksPageState();
}

class _TasksPageState extends State<TasksPage> {
  late final ValueNotifier<List<Event>> _selectedEvents;

  @override
  Widget build(BuildContext context) {

    return Scaffold(
      body: Center(
        child: Padding(
          padding: EdgeInsets.all(8.0),
          child: Column(
            children: [
              CalendarWidget(),
              Divider(),
              Expanded(child: EventsListWidget()),
            ],
          ),
          )
        ),
    );
  }
}