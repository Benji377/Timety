import 'package:flutter/material.dart';
import '../data/focus/focus_models.dart';

class PhaseEditorDialog extends StatefulWidget {
  final SessionPhase? initialPhase;

  const PhaseEditorDialog({super.key, this.initialPhase});

  @override
  State<PhaseEditorDialog> createState() => _PhaseEditorDialogState();
}

class _PhaseEditorDialogState extends State<PhaseEditorDialog> {
  late PhaseType _selectedType;
  late TextEditingController _timeController;

  @override
  void initState() {
    super.initState();
    final isNew = widget.initialPhase == null;
    _selectedType = isNew ? PhaseType.focus : widget.initialPhase!.type;
    _timeController = TextEditingController(
      text: isNew ? '25' : widget.initialPhase!.durationMinutes.toString(),
    );
  }

  @override
  void dispose() {
    _timeController.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    final isNew = widget.initialPhase == null;
    
    return AlertDialog(
      title: Text(isNew ? "Add Phase" : "Edit Phase"),
      content: Column(
        mainAxisSize: MainAxisSize.min,
        children: [
          SegmentedButton<PhaseType>(
            segments: const [
              ButtonSegment(value: PhaseType.focus, label: Text("Focus"), icon: Icon(Icons.center_focus_strong)),
              ButtonSegment(value: PhaseType.rest, label: Text("Rest"), icon: Icon(Icons.coffee)),
            ],
            selected: {_selectedType},
            onSelectionChanged: (Set<PhaseType> newSelection) {
              setState(() => _selectedType = newSelection.first);
            },
          ),
          const SizedBox(height: 24),
          TextField(
            controller: _timeController,
            keyboardType: TextInputType.number,
            decoration: const InputDecoration(
              labelText: "Duration (Minutes)",
              border: OutlineInputBorder(),
              suffixText: "min",
            ),
          ),
        ],
      ),
      actions: [
        if (!isNew)
          TextButton(
            // Return a special string flag so the parent knows to delete this node
            onPressed: () => Navigator.pop(context, 'DELETE'),
            child: const Text("Delete", style: TextStyle(color: Colors.red)),
          ),
        TextButton(
          onPressed: () => Navigator.pop(context),
          child: const Text("Cancel"),
        ),
        ElevatedButton(
          onPressed: () {
            int? mins = int.tryParse(_timeController.text);
            if (mins != null && mins > 0) {
              if (mins > 240) mins = 240; 
              // Return the brand new SessionPhase object back to the parent
              Navigator.pop(context, SessionPhase(type: _selectedType, durationMinutes: mins));
            }
          },
          child: const Text("Save"),
        ),
      ],
    );
  }
}