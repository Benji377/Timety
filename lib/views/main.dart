import 'package:timety/commons.dart';

class MainPage extends StatefulWidget {
  @override
  State<MainPage> createState() => _MainPageState();
}

class _MainPageState extends State<MainPage> {
  var _selectedIndex = 0;

  final List<Widget> _screens = [
    HomePage(),
    FocusPage(),
    TasksPage(),
    StatisticsPage(),
  ];
  void _onItemTapped(int index) {
    setState(() {
      _selectedIndex = index;
    });
  }


  @override
  Widget build(BuildContext context) {

    
    return Scaffold(
      body: Column(
        children: [
          Expanded(
            child: SafeArea(
              child: Container(
                color: Theme.of(context).colorScheme.primaryContainer,
                child: _screens[_selectedIndex],
              ),
            ),
          ),
        ],
      ),
      bottomNavigationBar: NavigationBar(
        selectedIndex: _selectedIndex,
        onDestinationSelected: _onItemTapped,
        destinations: [
          NavigationDestination(
            icon: Icon(Icons.home),
            label: "HOME",
          ),
          NavigationDestination(
            icon: Icon(Icons.coffee),
            label: "FOCUS",
          ),
          NavigationDestination(
            icon: Icon(Icons.check_circle_outline),
            label: "TASKS",
          ),
          NavigationDestination(
            icon: Icon(Icons.pie_chart),
            label: "STATS",
          ),
        ],
      ),
    );
        
  }
}
