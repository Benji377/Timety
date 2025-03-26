import 'package:timety/commons.dart';

class HomePage extends StatelessWidget {
  @override
  Widget build(BuildContext context) {
    var appState = context.watch<MainState>();

    return Scaffold(
      appBar: AppBar(
        actions: [
          IconButton(
              onPressed: () {/* Navigate to settings page*/},
              icon: Icon(Icons.settings),
              tooltip: 'Navigate to settings',
          )
        ],
      ),
      body: Center(
        child: Text("Not implemented!"),
      ),

    );
  }
}