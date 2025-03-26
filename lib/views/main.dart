import 'package:timety/commons.dart';

class MainPage extends StatefulWidget {
  @override
  State<MainPage> createState() => _MainPageState();
}

class _MainPageState extends State<MainPage> {
  var _selectedIndex = 0;
  var _showAppBar = false;

  void _onItemTapped(int index) {
    setState(() {
      _selectedIndex = index;
      if (_selectedIndex == 0) {
        _showAppBar = true;
      } else {
        _showAppBar = false;
      }
    });
  }

  @override
  Widget build(BuildContext context) {
    Widget page;
    switch (_selectedIndex) {
      case 0:
        page = HomePage();
      case 1:
        page = CalenderPage();
      case 2:
        page = TasksPage();
      case 3:
        page = StatisticsPage();
      case 4:
        page = SettingsPage();
      default:
        throw UnimplementedError('no widget for $_selectedIndex');
    }

    return LayoutBuilder(
        builder: (context, constraints) {
          return Scaffold(
            appBar: _showAppBar ? AppBar(
              actions: [
                IconButton(
                  onPressed: () {_onItemTapped(4);},
                  icon: Icon(Icons.settings),
                  tooltip: 'Navigate to settings',
                )
              ],
            ) : null,
            body: Row(
              children: [
                SafeArea(
                  child: NavigationBar(
                      selectedIndex: _selectedIndex,
                      onDestinationSelected: _onItemTapped,
                      indicatorColor: Colors.deepPurple,
                      destinations: [
                        NavigationDestination(
                            icon: Icon(Icons.home),
                            label: "Home",
                        ),
                        NavigationDestination(
                          icon: Icon(Icons.calendar_month),
                          label: "Calendar",
                        ),
                        NavigationDestination(
                          icon: Icon(Icons.check_circle_outline),
                          label: "Tasks",
                        ),
                        NavigationDestination(
                          icon: Icon(Icons.pie_chart),
                          label: "Stats",
                        ),
                      ],
                  ),
                ),
                Expanded(
                  child: Container(
                    color: Theme.of(context).colorScheme.primaryContainer,
                    child: page,
                  ),
                ),
              ],
            ),
          );
        }
    );
  }
}