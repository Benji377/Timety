import 'package:flutter/material.dart';
import '../theme/app_theme.dart';
import '../data/focus/focus_models.dart';
import '../../l10n/app_localizations.dart';

enum PhaseDialogAction { delete }

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
      title: Text(
        isNew
            ? AppLocalizations.of(context)!.focusPhasesTitleAdd
            : AppLocalizations.of(context)!.focusPhasesTitleEdit,
      ),
      content: Column(
        mainAxisSize: MainAxisSize.min,
        children: [
          SegmentedButton<PhaseType>(
            segments: [
              ButtonSegment(
                value: PhaseType.focus,
                label: Text(AppLocalizations.of(context)!.focusLabelDefault),
                icon: const Icon(Icons.center_focus_strong),
              ),
              ButtonSegment(
                value: PhaseType.rest,
                label: Text(AppLocalizations.of(context)!.focusLabelRest),
                icon: const Icon(Icons.coffee),
              ),
            ],
            selected: {_selectedType},
            onSelectionChanged: (Set<PhaseType> newSelection) {
              setState(() => _selectedType = newSelection.first);
            },
          ),
          const SizedBox(height: AppTheme.space2XLarge),
          TextField(
            controller: _timeController,
            keyboardType: TextInputType.number,
            decoration: InputDecoration(
              labelText: AppLocalizations.of(context)!.focusPhaseLabelDuration,
              border: const OutlineInputBorder(),
              suffixText: AppLocalizations.of(
                context,
              )!.focusPhaseLabelDurationSuffix,
            ),
          ),
        ],
      ),
      actions: [
        if (!isNew)
          TextButton(
            // Return a special string flag so the parent knows to delete this node
            onPressed: () => Navigator.pop(context, PhaseDialogAction.delete),
            child: Text(
              AppLocalizations.of(context)!.commonLabelDelete,
              style: const TextStyle(color: AppTheme.errorColor),
            ),
          ),
        TextButton(
          onPressed: () => Navigator.pop(context),
          child: Text(AppLocalizations.of(context)!.commonLabelCancel),
        ),
        ElevatedButton(
          onPressed: () {
            int? mins = int.tryParse(_timeController.text);
            if (mins != null && mins > 0) {
              if (mins > AppTheme.maxNodeMins) mins = AppTheme.maxNodeMins;
              // Return the brand new SessionPhase object back to the parent
              Navigator.pop(
                context,
                SessionPhase(type: _selectedType, durationMinutes: mins),
              );
            }
          },
          child: Text(AppLocalizations.of(context)!.commonLabelSave),
        ),
      ],
    );
  }
}
