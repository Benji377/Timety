import 'commons.dart';

void main() {
  runApp(App());
}



class App extends StatelessWidget {
  const App({super.key});

  @override
  Widget build(BuildContext context) {
    return ChangeNotifierProvider(
      create: (context) => MainState(),
      child: MaterialApp(
        title: 'Timety',
        theme: timetyTheme(),
        home: MainPage(),
        routes: {
          '/home': (context) => HomePage(),
          '/settings': (context) => SettingsPage(),
          '/tasks': (context) => TasksPage(),
          '/focus': (context) => FocusPage(),
          '/stats': (context) => StatisticsPage(),
        },
      ),
      
      
    );
  }
}












