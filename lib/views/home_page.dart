import 'package:timety/commons.dart';

class HomePage extends StatelessWidget {
  @override
  Widget build(BuildContext context) {
    var appState = context.watch<MainState>();

    return Center(
      child: Column(
        children: [
          Row(
              children: [
                Expanded(
                    child: Padding(
                      padding: const EdgeInsets.all(8.0),
                      child: Text("Hello ${appState.userName}!",
                      style: TextStyle(
                        fontSize: 20,
                      ),),
                    )),
                IconButton(
                  alignment: Alignment.topRight,
                    onPressed: () => Navigator.of(context).pushNamed('/settings'),
                    icon: Icon(Icons.settings)
                ),
              ]
          ),
          Column(
              children: [
                FocusPieChart(dataMap: appState.getFocusDataMap()),
                SimpleTaskListWidget(taskList: appState.taskList),
              ],
            ),
        ],
      ),
    );
  }
}
