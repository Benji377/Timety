import 'package:timety/commons.dart';
import 'package:timety/features/focus/application/focus_summery_provider.dart';
import 'package:timety/features/focus/presentation/stop_watcher.dart'; // Import FocusViewModel




class FocusPage extends ConsumerWidget {
  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final summary = ref.watch(focusSummeryProvider);
    final charMap = summary.getFocusDataMap(); // Initialize or assign a value to 'focus'
    return Scaffold(
      body: Center(
        child: Column(
          children: [
            Padding(
              padding: const EdgeInsets.all(20.0),
              child: Text("Studying..."),
            ),
            FocusPieChart(dataMap: charMap),
            Padding(
              padding: const EdgeInsets.all(40.0),
              child: IconButton(onPressed: () { StopWatcher().timeStart();}, icon: Icon(Icons.play_arrow)),
            ),
            Padding(
              padding: const EdgeInsets.all(40.0),
              child: IconButton(onPressed: () { StopWatcher().endFocusTime();}, icon: Icon(Icons.stop_circle)),
            ),
          ],
        ),
      ),
    );

    
  }
}
