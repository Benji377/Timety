import 'package:flutter/material.dart';
import '../data/focus/focus_models.dart';
import '../theme/app_theme.dart';

/// Reusable bottom sheet builders for focus-related UIs
class FocusBottomSheetBuilders {
  /// Shows a bottom sheet for logging distraction events
  ///
  /// Returns after user selects an event, which is then logged via the [onEventSelected] callback
  static void showDistractionSheet({
    required BuildContext context,
    required Function(String eventName) onEventSelected,
  }) {
    const events = [
      {
        'name': 'Distracted',
        'icon': Icons.warning_amber,
        'color': AppTheme.errorColor,
      },
      {
        'name': 'Hydrated / Drink',
        'icon': Icons.water_drop,
        'color': AppTheme.taskColor,
      },
      {
        'name': 'Stretched',
        'icon': Icons.accessibility_new,
        'color': AppTheme.warningColor,
      },
      {
        'name': 'Snack',
        'icon': Icons.restaurant,
        'color': AppTheme.successColor,
      },
      {'name': 'Restroom', 'icon': Icons.wc, 'color': Colors.grey},
    ];

    showModalBottomSheet(
      context: context,
      shape: const RoundedRectangleBorder(
        borderRadius: BorderRadius.vertical(top: Radius.circular(20)),
      ),
      builder: (context) {
        return SafeArea(
          child: Column(
            mainAxisSize: MainAxisSize.min,
            children: [
              const Padding(
                padding: EdgeInsets.all(16.0),
                child: Text(
                  "Log an Event",
                  style: TextStyle(fontSize: 18, fontWeight: FontWeight.bold),
                ),
              ),
              ...events.map(
                (e) => ListTile(
                  leading: Icon(
                    e['icon'] as IconData,
                    color: e['color'] as Color,
                  ),
                  title: Text(
                    e['name'] as String,
                    style: const TextStyle(fontWeight: FontWeight.w500),
                  ),
                  onTap: () {
                    final eventName = e['name'] as String;
                    onEventSelected(eventName);
                    Navigator.pop(context);
                    ScaffoldMessenger.of(context).showSnackBar(
                      SnackBar(
                        content: Text('Logged: $eventName'),
                        duration: const Duration(seconds: 2),
                      ),
                    );
                  },
                ),
              ),
            ],
          ),
        );
      },
    );
  }

  /// Shows a bottom sheet for selecting or creating focus tags
  ///
  /// Allows user to select an existing tag or create a new one.
  /// Callbacks [onTagSelected] and [onCreateTag] handle the results.
  static void showTagSelector({
    required BuildContext context,
    required List<FocusTag> tags,
    required String? selectedTagId,
    required Function(FocusTag tag) onTagSelected,
    required Function() onCreateNewTag,
  }) {
    showModalBottomSheet(
      context: context,
      shape: const RoundedRectangleBorder(
        borderRadius: BorderRadius.vertical(top: Radius.circular(20)),
      ),
      builder: (context) {
        return SafeArea(
          child: Column(
            mainAxisSize: MainAxisSize.min,
            children: [
              const Padding(
                padding: EdgeInsets.all(16.0),
                child: Text(
                  "Select Tag",
                  style: TextStyle(fontSize: 18, fontWeight: FontWeight.bold),
                ),
              ),
              Expanded(
                child: ListView.builder(
                  shrinkWrap: true,
                  itemCount: tags.length,
                  itemBuilder: (context, index) {
                    final tag = tags[index];
                    final isSelected = selectedTagId == tag.id;
                    return ListTile(
                      leading: Icon(Icons.circle, color: Color(tag.colorValue)),
                      title: Text(
                        tag.name,
                        style: TextStyle(
                          fontWeight: isSelected
                              ? FontWeight.bold
                              : FontWeight.normal,
                        ),
                      ),
                      trailing: isSelected
                          ? const Icon(
                              Icons.check,
                              color: AppTheme.successColor,
                            )
                          : null,
                      onTap: () {
                        onTagSelected(tag);
                        Navigator.pop(context);
                      },
                    );
                  },
                ),
              ),
              const Divider(),
              ListTile(
                leading: const Icon(Icons.add_circle_outline),
                title: const Text("Create New Tag"),
                onTap: () {
                  Navigator.pop(context);
                  onCreateNewTag();
                },
              ),
            ],
          ),
        );
      },
    );
  }

  /// Shows a dialog for creating a new focus tag
  ///
  /// [onTagCreated] is called with the tag name when user confirms.
  static void showCreateTagDialog({
    required BuildContext context,
    required Function(String tagName) onTagCreated,
  }) {
    final TextEditingController controller = TextEditingController();

    showDialog(
      context: context,
      builder: (context) {
        return AlertDialog(
          title: const Text("New Tag"),
          content: TextField(
            controller: controller,
            decoration: const InputDecoration(
              hintText: "Tag Name (e.g. Reading)",
            ),
            autofocus: true,
          ),
          actions: [
            TextButton(
              onPressed: () => Navigator.pop(context),
              child: const Text("Cancel"),
            ),
            ElevatedButton(
              onPressed: () {
                if (controller.text.trim().isNotEmpty) {
                  onTagCreated(controller.text.trim());
                  Navigator.pop(context);
                }
              },
              child: const Text("Save"),
            ),
          ],
        );
      },
    );
  }
}
