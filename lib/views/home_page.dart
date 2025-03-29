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
                    onPressed: () => Navigator.pushNamed(context, '/settings'),
                    icon: Icon(Icons.settings)
                ),
              ]
          ),
          Expanded(
            child: Column(
              children: [
                Padding(
                  padding: const EdgeInsets.all(60),
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
