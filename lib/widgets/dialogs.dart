import 'package:flutter/material.dart';
import 'package:provider/provider.dart';

import '../theme/app_theme.dart';
import '../data/focus/focus_models.dart';
import '../providers/focus_provider.dart';
import '../providers/user_provider.dart';
import '../utils/l10n_utils.dart';

class AppDialogs {
  // Generic confirmation dialog
  static Future<bool?> showConfirmation({
    required BuildContext context,
    required String title,
    required String content,
    String confirmLabel = 'Confirm',
    Color? confirmColor,
  }) {
    return showDialog<bool>(
      context: context,
      builder: (context) => AlertDialog(
        title: Text(title),
        content: Text(content),
        actions: [
          TextButton(
            onPressed: () => Navigator.pop(context, false),
            child: const Text('Cancel'),
          ),
          FilledButton(
            style: confirmColor == null
                ? null
                : FilledButton.styleFrom(
                    backgroundColor: confirmColor,
                    foregroundColor: Colors.white,
                  ),
            onPressed: () => Navigator.pop(context, true),
            child: Text(confirmLabel),
          ),
        ],
      ),
    );
  }

  static Future<String?> showTextInputDialog({
    required BuildContext context,
    required String title,
    required String labelText,
    String initialValue = '',
    String confirmLabel = 'Save',
    String? hintText,
    TextInputType keyboardType = TextInputType.text,
    bool autofocus = true,
  }) {
    final controller = TextEditingController(text: initialValue);

    return showDialog<String>(
      context: context,
      builder: (context) => AlertDialog(
        title: Text(title),
        content: TextField(
          controller: controller,
          autofocus: autofocus,
          keyboardType: keyboardType,
          decoration: InputDecoration(
            labelText: labelText,
            hintText: hintText,
            border: const OutlineInputBorder(),
          ),
          onSubmitted: (value) {
            final trimmed = value.trim();
            if (trimmed.isNotEmpty) {
              Navigator.pop(context, trimmed);
            }
          },
        ),
        actions: [
          TextButton(
            onPressed: () => Navigator.pop(context),
            child: const Text('Cancel'),
          ),
          ElevatedButton(
            onPressed: () {
              final trimmed = controller.text.trim();
              if (trimmed.isNotEmpty) {
                Navigator.pop(context, trimmed);
              }
            },
            child: Text(confirmLabel),
          ),
        ],
      ),
    );
  }

  static void showTimeMachineDialog(
    BuildContext context,
    FocusProvider provider,
  ) {
    FocusMode selectedMode = provider.modes.isNotEmpty
        ? provider.modes.first
        : FocusMode.stopwatch();
    FocusTag? selectedTag = provider.selectedTag;

    DateTime startDateTime = DateTime.now().subtract(
      const Duration(minutes: 25),
    );
    DateTime endDateTime = DateTime.now();

    String formatDT(DateTime dt) {
      return "${dt.month}/${dt.day}  ${dt.hour.toString().padLeft(2, '0')}:${dt.minute.toString().padLeft(2, '0')}";
    }

    showDialog(
      context: context,
      builder: (context) {
        return StatefulBuilder(
          builder: (context, setDialogState) {
            return AlertDialog(
              title: const Row(
                children: [
                  Icon(Icons.history, color: AppTheme.taskColor),
                  SizedBox(width: 8),
                  Text("Log Past Session"),
                ],
              ),
              content: SingleChildScrollView(
                child: Column(
                  mainAxisSize: MainAxisSize.min,
                  children: [
                    DropdownButtonFormField<FocusMode>(
                      initialValue: selectedMode,
                      decoration: const InputDecoration(
                        labelText: "Focus Mode",
                        border: OutlineInputBorder(),
                      ),
                      items: provider.modes
                          .map(
                            (m) =>
                                DropdownMenuItem(value: m, child: Text(getLocalizedFocusModeName(context, m))),
                          )
                          .toList(),
                      onChanged: (val) {
                        if (val != null) {
                          setDialogState(() => selectedMode = val);
                        }
                      },
                    ),
                    const SizedBox(height: AppTheme.spaceLarge),
                    DropdownButtonFormField<FocusTag?>(
                      initialValue: selectedTag,
                      decoration: const InputDecoration(
                        labelText: "Tag (Optional)",
                        border: OutlineInputBorder(),
                      ),
                      items: [
                        const DropdownMenuItem<FocusTag?>(
                          child: Text("No Tag"),
                        ),
                        ...provider.tags.map(
                          (t) =>
                              DropdownMenuItem(value: t, child: Text(t.name)),
                        ),
                      ],
                      onChanged: (val) =>
                          setDialogState(() => selectedTag = val),
                    ),
                    const SizedBox(height: AppTheme.spaceLarge),
                    ListTile(
                      shape: const RoundedRectangleBorder(
                        borderRadius: AppTheme.brMedium,
                        side: BorderSide(color: AppTheme.borderLight),
                      ),
                      title: const Text(
                        "Start Time",
                        style: TextStyle(
                          fontSize: AppTheme.fsLabel,
                          color: Colors.black54,
                        ),
                      ),
                      subtitle: Text(
                        formatDT(startDateTime),
                        style: const TextStyle(fontWeight: AppTheme.fwBold),
                      ),
                      trailing: const Icon(Icons.edit_calendar),
                      onTap: () async {
                        final d = await showDatePicker(
                          context: context,
                          initialDate: startDateTime,
                          firstDate: DateTime(2020),
                          lastDate: DateTime.now(),
                        );
                        if (!context.mounted) return;
                        if (d != null) {
                          final t = await showTimePicker(
                            context: context,
                            initialTime: TimeOfDay.fromDateTime(startDateTime),
                          );
                          if (t != null) {
                            setDialogState(
                              () => startDateTime = DateTime(
                                d.year,
                                d.month,
                                d.day,
                                t.hour,
                                t.minute,
                              ),
                            );
                          }
                        }
                      },
                    ),
                    const SizedBox(height: AppTheme.spaceSmall),
                    ListTile(
                      shape: const RoundedRectangleBorder(
                        borderRadius: AppTheme.brMedium,
                        side: BorderSide(color: AppTheme.borderLight),
                      ),
                      title: const Text(
                        "End Time",
                        style: TextStyle(
                          fontSize: AppTheme.fsLabel,
                          color: Colors.black54,
                        ),
                      ),
                      subtitle: Text(
                        formatDT(endDateTime),
                        style: const TextStyle(fontWeight: AppTheme.fwBold),
                      ),
                      trailing: const Icon(Icons.edit_calendar),
                      onTap: () async {
                        final d = await showDatePicker(
                          context: context,
                          initialDate: endDateTime,
                          firstDate: DateTime(2020),
                          lastDate: DateTime.now(),
                        );
                        if (!context.mounted) return;
                        if (d != null) {
                          final t = await showTimePicker(
                            context: context,
                            initialTime: TimeOfDay.fromDateTime(endDateTime),
                          );
                          if (t != null) {
                            setDialogState(
                              () => endDateTime = DateTime(
                                d.year,
                                d.month,
                                d.day,
                                t.hour,
                                t.minute,
                              ),
                            );
                          }
                        }
                      },
                    ),
                  ],
                ),
              ),
              actions: [
                TextButton(
                  onPressed: () => Navigator.pop(context),
                  child: const Text("Cancel"),
                ),
                ElevatedButton(
                  onPressed: () {
                    if (endDateTime.isBefore(startDateTime)) {
                      ScaffoldMessenger.of(context).showSnackBar(
                        const SnackBar(
                          content: Text("End time must be after start time"),
                        ),
                      );
                      return;
                    }
                    provider.logPastSession(
                      mode: selectedMode,
                      startTime: startDateTime,
                      endTime: endDateTime,
                      tag: selectedTag,
                      userProvider: context.read<UserProvider>(),
                    );
                    Navigator.pop(context);
                    ScaffoldMessenger.of(context).showSnackBar(
                      const SnackBar(content: Text("Session Logged!")),
                    );
                  },
                  child: const Text("Save"),
                ),
              ],
            );
          },
        );
      },
    );
  }
}
