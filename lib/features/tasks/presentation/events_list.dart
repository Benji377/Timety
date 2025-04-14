import 'package:timety/commons.dart';
import '../presentation/presentation.dart';
import '../application/application.dart';

class EventsListWidget extends StatelessWidget {

  @override
  Widget build(BuildContext context) {
    final mainState = Provider.of<MainState>(context);
    final eventsList = mainState.selectedEvents;

    if (eventsList.value.isEmpty) {
      return Center(
        child: Text("No events for today"),
      );
    }

    return ValueListenableBuilder<List<Event>>(
        valueListenable: eventsList,
        builder: (context, value, _) {
          return ListView.builder(
            itemCount: value.length,
            itemBuilder: (context, index) {
              return Container(
                margin: const EdgeInsets.symmetric(
                  horizontal: 12.0,
                  vertical: 4.0,
                ),
                decoration: BoxDecoration(
                  border: Border.all(),
                  borderRadius: BorderRadius.circular(12.0),
                ),
                child: ListTile(
                  onTap: () => print('${value[index]}'),
                  title: Text('${value[index]}'),
                ),
              );
            },
          );
        }
    );
  }
}