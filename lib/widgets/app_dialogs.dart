import 'package:flutter/material.dart';

import '../data/focus_models.dart';
import '../providers/focus_provider.dart';

class AppDialogs {
  // Generic confirmation dialog
  static Future<bool?> showConfirmation({
    required BuildContext context,
    required String title,
    required String content,
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
          FilledButton( // Material 3 style
            onPressed: () => Navigator.pop(context, true),
            child: const Text('Confirm'),
          ),
        ],
      ),
    );
  }

  // Generic info dialog
  static void showInfo({
    required BuildContext context,
    required String title,
    required String message,
  }) {
    showDialog(
      context: context,
      builder: (context) => AlertDialog(
        title: Text(title),
        content: Text(message),
        actions: [
          TextButton(
            onPressed: () => Navigator.pop(context),
            child: const Text('OK'),
          ),
        ],
      ),
    );
  }

static void showTimeMachineDialog(BuildContext context, FocusProvider provider) {
    FocusMode selectedMode = provider.modes.isNotEmpty ? provider.modes.first : FocusMode.stopwatch();
    FocusTag? selectedTag = provider.selectedTag;
    
    DateTime startDateTime = DateTime.now().subtract(const Duration(minutes: 25));
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
                  Icon(Icons.history, color: Colors.blue),
                  SizedBox(width: 8),
                  Text("Log Past Session"),
                ],
              ),
              content: SingleChildScrollView(
                child: Column(
                  mainAxisSize: MainAxisSize.min,
                  children: [
                    DropdownButtonFormField<FocusMode>(
                      initialValue: selectedMode, // Use value instead of initialValue
                      decoration: const InputDecoration(labelText: "Focus Mode", border: OutlineInputBorder()),
                      items: provider.modes.map((m) => DropdownMenuItem(value: m, child: Text(m.name))).toList(),
                      onChanged: (val) { if (val != null) setDialogState(() => selectedMode = val); },
                    ),
                    const SizedBox(height: 16),
                    DropdownButtonFormField<FocusTag?>(
                      initialValue: selectedTag, // Use value instead of initialValue
                      decoration: const InputDecoration(labelText: "Tag (Optional)", border: OutlineInputBorder()),
                      items: [
                        const DropdownMenuItem<FocusTag?>(value: null, child: Text("No Tag")),
                        ...provider.tags.map((t) => DropdownMenuItem(value: t, child: Text(t.name)))
                      ],
                      onChanged: (val) => setDialogState(() => selectedTag = val),
                    ),
                    const SizedBox(height: 16),
                    ListTile(
                      shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(8), side: BorderSide(color: Colors.grey.shade300)),
                      title: const Text("Start Time", style: TextStyle(fontSize: 12, color: Colors.grey)),
                      subtitle: Text(formatDT(startDateTime), style: const TextStyle(fontWeight: FontWeight.bold)),
                      trailing: const Icon(Icons.edit_calendar),
                      onTap: () async {
                        final d = await showDatePicker(context: context, initialDate: startDateTime, firstDate: DateTime(2020), lastDate: DateTime.now());
                        if (!context.mounted) return;
                        if (d != null) {
                          final t = await showTimePicker(context: context, initialTime: TimeOfDay.fromDateTime(startDateTime));
                          if (t != null) setDialogState(() => startDateTime = DateTime(d.year, d.month, d.day, t.hour, t.minute));
                        }
                      },
                    ),
                    const SizedBox(height: 8),
                    ListTile(
                      shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(8), side: BorderSide(color: Colors.grey.shade300)),
                      title: const Text("End Time", style: TextStyle(fontSize: 12, color: Colors.grey)),
                      subtitle: Text(formatDT(endDateTime), style: const TextStyle(fontWeight: FontWeight.bold)),
                      trailing: const Icon(Icons.edit_calendar),
                      onTap: () async {
                        final d = await showDatePicker(context: context, initialDate: endDateTime, firstDate: DateTime(2020), lastDate: DateTime.now());
                        if (!context.mounted) return;
                        if (d != null) {
                          final t = await showTimePicker(context: context, initialTime: TimeOfDay.fromDateTime(endDateTime));
                          if (t != null) setDialogState(() => endDateTime = DateTime(d.year, d.month, d.day, t.hour, t.minute));
                        }
                      },
                    ),
                  ],
                ),
              ),
              actions: [
                TextButton(onPressed: () => Navigator.pop(context), child: const Text("Cancel")),
                ElevatedButton(
                  onPressed: () {
                    if (endDateTime.isBefore(startDateTime)) {
                      ScaffoldMessenger.of(context).showSnackBar(const SnackBar(content: Text("End time must be after start time")));
                      return;
                    }
                    provider.logPastSession(mode: selectedMode, startTime: startDateTime, endTime: endDateTime, tag: selectedTag);
                    Navigator.pop(context);
                    ScaffoldMessenger.of(context).showSnackBar(const SnackBar(content: Text("Session Logged!")));
                  },
                  child: const Text("Save"),
                ),
              ],
            );
          }
        );
      }
    );
  }
}