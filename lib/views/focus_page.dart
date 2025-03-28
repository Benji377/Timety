import 'package:timety/commons.dart';

class FocusPage extends StatelessWidget {
  @override
  Widget build(BuildContext context) {
    var appState = context.watch<MainState>();

    return Center(
      child: Column(
        children: [
          Padding(
            padding: EdgeInsets.all(20.0),
            child: FocusPieChart(dataMap: appState.getFocusDataMap()),
          ),
          Expanded(
            child: Padding(padding: EdgeInsets.all(8.0), child: SchedulingWidget()),
          ),
        ],
      ),
    );
  }
}
