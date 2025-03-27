import 'package:timety/commons.dart';

class SimpleTaskListWidget extends StatelessWidget {
  final List<TaskItem> taskList;
  const SimpleTaskListWidget({super.key, required this.taskList});

  @override
  Widget build(BuildContext context) {

    if (taskList.isEmpty) {
      return Center(
        child: Text("No tasks for today"),
      );
    }

    return ListView(
      children: [
        for (var task in taskList)
          ListTile(
            leading: Icon(task.icon),
            title: Text(task.title),
            trailing: Checkbox(
                value: task.state == TaskItemState.done,
                onChanged: (isChecked) {
                  print("Task: ${task.title} isChecked: $isChecked");
                }
            ),
            onTap: () {
              print("Task: ${task.title} has been tapped");
            },
          ),
      ],
    );
  }
}