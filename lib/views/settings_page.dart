import 'package:timety/commons.dart';

class SettingsPage extends StatelessWidget {
  @override
  Widget build(BuildContext context) {

    return Scaffold(
      appBar: AppBar(title: Text('Settings Page')),
      body: Center(
        child: Column(
          children: [
            Text("SettingsPage - Not implemented!"),
            IconButton(onPressed: () => Navigator.of(context).pop(), icon: Icon(Icons.home))
          ],
        ),
      ),
    );
  }
}