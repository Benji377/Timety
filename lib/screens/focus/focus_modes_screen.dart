import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import '../../l10n/app_localizations.dart';
import '../../providers/focus_provider.dart';
import '../../data/focus/focus_models.dart';
import '../../widgets/focus/focus_mode_edit_card.dart';

class FocusModesScreen extends StatefulWidget {
  const FocusModesScreen({super.key});

  @override
  State<FocusModesScreen> createState() => _FocusModesScreenState();
}

class _FocusModesScreenState extends State<FocusModesScreen> {
  FocusMode? _pendingMode; // Holds the temporary mode until saved or canceled

  @override
  Widget build(BuildContext context) {
    final focusProvider = context.watch<FocusProvider>();
    final modes = focusProvider.modes;
    final itemCount = modes.length + (_pendingMode != null ? 1 : 0);

    return Scaffold(
      appBar: AppBar(
        title: Text(AppLocalizations.of(context)!.focusModesTitle),
      ),
      body: ListView.builder(
        padding: const EdgeInsets.only(bottom: 100, top: 16),
        itemCount: itemCount,
        itemBuilder: (context, index) {
          if (_pendingMode != null && index == 0) {
            return ModeEditCard(
              key: ValueKey(_pendingMode!.id),
              mode: _pendingMode!,
              isNewMode: true,
              onCancelNew: () => setState(() => _pendingMode = null),
              onSaveNew: () => setState(() => _pendingMode = null),
            );
          }
          final modeIndex = _pendingMode != null ? index - 1 : index;
          final mode = modes[modeIndex];
          return ModeEditCard(key: ValueKey(mode.id), mode: mode);
        },
      ),
      floatingActionButton: _pendingMode != null
          ? null
          : FloatingActionButton.extended(
              onPressed: () {
                setState(() {
                  _pendingMode = FocusMode(
                    id: DateTime.now().toString(),
                    name: AppLocalizations.of(context)!.focusModeLabelNew,
                    type: FocusModeType.custom,
                    phases: [
                      SessionPhase(type: PhaseType.focus, durationMinutes: 25),
                      SessionPhase(type: PhaseType.rest, durationMinutes: 5),
                    ],
                  );
                });
              },
              icon: const Icon(Icons.add),
              label: Text(AppLocalizations.of(context)!.focusModeLabelNew),
            ),
    );
  }
}
