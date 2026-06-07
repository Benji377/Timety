import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import '../../l10n/app_localizations.dart';
import '../../providers/focus_provider.dart';
import '../../data/focus/focus_models.dart';
import '../../widgets/focus/focus_mode_edit_card.dart';

class FocusModesScreen extends StatelessWidget {
  const FocusModesScreen({super.key});

  @override
  Widget build(BuildContext context) {
    final focusProvider = context.watch<FocusProvider>();
    final modes = focusProvider.modes;

    return Scaffold(
      appBar: AppBar(
        title: Text(AppLocalizations.of(context)!.focusModesTitle),
      ),
      body: ListView.builder(
        padding: const EdgeInsets.only(bottom: 100, top: 16),
        itemCount: modes.length,
        itemBuilder: (context, index) {
          final mode = modes[index];
          return ModeEditCard(key: ValueKey(mode.id), mode: mode);
        },
      ),
      floatingActionButton: FloatingActionButton.extended(
        onPressed: () {
          final newMode = FocusMode(
            id: DateTime.now().toString(),
            name: AppLocalizations.of(context)!.focusModeLabelNew,
            type: FocusModeType.custom,
            phases: [
              SessionPhase(type: PhaseType.focus, durationMinutes: 25),
              SessionPhase(type: PhaseType.rest, durationMinutes: 5),
            ],
          );
          focusProvider.saveCustomMode(newMode);
        },
        icon: const Icon(Icons.add),
        label: Text(AppLocalizations.of(context)!.focusModeLabelNew),
      ),
    );
  }
}
