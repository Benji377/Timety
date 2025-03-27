import 'package:timety/commons.dart';

class HomePage extends StatelessWidget {
  @override
  Widget build(BuildContext context) {
    var appState = context.watch<MainState>();

    return Center(
      child: Column(
        children: [
          Row(children: [Text("Hello ${appState.userName}!")]),
          Expanded(
            child: Column(
              mainAxisAlignment: MainAxisAlignment.center,
              children: [
                Padding(
                  padding: const EdgeInsets.all(20),
                  child: FocusPieChart(dataMap: appState.getFocusDataMap()),
                ),
                SimpleTaskListWidget(taskList: appState.taskList),
              ],
            ),
          ),
        ],
      ),
    );
  }
}
