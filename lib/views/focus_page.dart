import 'package:timety/commons.dart';
import 'package:timety/viewmodel/focus_view_model.dart';

class FocusPage extends StatefulWidget {
  @override
  State<FocusPage> createState() => _FocusPageState();
}


class _FocusPageState extends State<FocusPage> {
  @override
  Widget build(BuildContext context) {
    var appState = context.watch<MainState>();
    return Scaffold(
      body: Center(
        child: Column(
          children: [
            Padding(
              padding: const EdgeInsets.all(20.0),
              child: Text("Studying..."),
            ),
            FocusPieChart(dataMap: appState.getFocusDataMap()),
            Padding(
              padding: const EdgeInsets.all(40.0),
              child: IconButton(onPressed: () { FocusViewModel().timeStart();}, icon: Icon(Icons.play_arrow)),
            ),
            Padding(
              padding: const EdgeInsets.all(40.0),
              child: IconButton(onPressed: () { FocusViewModel().endFocusTime();}, icon: Icon(Icons.stop_circle)),
            ),
          ],
        ),
      ),
    );

    
  }
}
