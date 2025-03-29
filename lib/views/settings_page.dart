import 'package:timety/commons.dart';

class SettingsPage extends StatelessWidget {
  @override
  Widget build(BuildContext context) {

    return Center(
      child: Column(
        children: [
          Text("SettingsPage - Not implemented!"),
          IconButton(onPressed: () => Navigator.pushNamed(context, '/home'), icon: Icon(Icons.home))
        ],
      ),
    );
  }
}