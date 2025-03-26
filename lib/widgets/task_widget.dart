import 'package:timety/commons.dart';

class TaskItemWidget extends StatelessWidget {
  final TaskItem taskItem;
  const TaskItemWidget({super.key, required this.taskItem});

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);

    return Card(
      color: theme.colorScheme.primary,
      elevation: 10.0,
      child: Padding(
          padding: const EdgeInsets.all(20),
          child: ExpansionTile(
            leading: Icon(taskItem.icon),
            title: Text(taskItem.title),
            subtitle: Text(taskItem.dueDate),
            trailing: IconButton(
                onPressed: () {print("Pressed task edit button");},
                icon: Icon(Icons.edit)
            ),
            children: [
              Text(taskItem.description)
            ],
          ),
        ),
    );
  }
}