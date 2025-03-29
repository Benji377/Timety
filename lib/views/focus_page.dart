import 'package:timety/commons.dart';

class FocusPage extends StatelessWidget {
  @override
  Widget build(BuildContext context) {
    var appState = context.watch<MainState>();

    return Column(
      children: [
        Padding(
          padding: const EdgeInsets.all(20.0),
          child: Text("Studying..."),
        ),
        FocusPieChart(dataMap: appState.getFocusDataMap()),
        Padding(
          padding: const EdgeInsets.all(40.0),
          child: IconButton(onPressed: () {}, icon: Icon(Icons.play_arrow)),
        )
      ],
    );
  }
}
