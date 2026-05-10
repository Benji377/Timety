import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import '../../theme/app_theme.dart';
import '../../providers/focus_provider.dart';
import '../../data/focus/focus_models.dart';
import '../../widgets/focus_mode_timeline.dart';
import '../../widgets/app_dialogs.dart';
import '../../widgets/phase_editor_dialog.dart';

class FocusModesScreen extends StatelessWidget {
  const FocusModesScreen({super.key});

  @override
  Widget build(BuildContext context) {
    final focusProvider = context.watch<FocusProvider>();
    final modes = focusProvider.modes;

    return Scaffold(
      appBar: AppBar(title: const Text('Manage Modes')),
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
            name: "New Custom Mode",
            type: FocusModeType.custom,
            phases: [
              SessionPhase(type: PhaseType.focus, durationMinutes: 25),
              SessionPhase(type: PhaseType.rest, durationMinutes: 5),
            ],
          );
          focusProvider.saveCustomMode(newMode);
        },
        icon: const Icon(Icons.add),
        label: const Text("New Mode"),
      ),
    );
  }
}

// --- INDIVIDUAL MODE EDIT CARD ---
class ModeEditCard extends StatefulWidget {
  final FocusMode mode;

  const ModeEditCard({super.key, required this.mode});

  @override
  State<ModeEditCard> createState() => _ModeEditCardState();
}

class _ModeEditCardState extends State<ModeEditCard> {
  late TextEditingController _nameController;
  late List<SessionPhase> _tempPhases;
  bool _isEditing = false;

  @override
  void initState() {
    super.initState();
    _nameController = TextEditingController(text: widget.mode.name);
    _tempPhases = widget.mode.phases
        .map(
          (p) => SessionPhase(type: p.type, durationMinutes: p.durationMinutes),
        )
        .toList();
  }

  @override
  void didUpdateWidget(covariant ModeEditCard oldWidget) {
    super.didUpdateWidget(oldWidget);
    if (oldWidget.mode.id != widget.mode.id && !_isEditing) {
      _nameController.text = widget.mode.name;
      _tempPhases = widget.mode.phases
          .map(
            (p) =>
                SessionPhase(type: p.type, durationMinutes: p.durationMinutes),
          )
          .toList();
    }
  }

  @override
  void dispose() {
    _nameController.dispose();
    super.dispose();
  }

  void _showPhaseDialog({int? index}) async {
    final initialPhase = index != null ? _tempPhases[index] : null;

    // Await the result from our standalone widget
    final result = await showDialog<dynamic>(
      context: context,
      builder: (context) => PhaseEditorDialog(initialPhase: initialPhase),
    );

    // Process the result
    if (result == 'DELETE' && index != null) {
      setState(() => _tempPhases.removeAt(index));
    } else if (result is SessionPhase) {
      setState(() {
        if (index == null) {
          _tempPhases.add(result);
        } else {
          _tempPhases[index] = result;
        }
      });
    }
  }

  // --- INTEGRATED APP DIALOGS ---
  void _confirmDelete() async {
    final confirm = await AppDialogs.showConfirmation(
      context: context,
      title: "Delete Mode?",
      content: "Are you sure you want to delete this custom mode?",
    );

    if (confirm == true) {
      if (!mounted) return;
      context.read<FocusProvider>().deleteMode(widget.mode.id);
    }
  }

  void _saveMode() {
    if (_nameController.text.trim().isEmpty || _tempPhases.isEmpty) return;

    final updatedMode = FocusMode(
      id: widget.mode.id,
      name: _nameController.text.trim(),
      type: widget.mode.type,
      isSystem: widget.mode.isSystem,
      phases: _tempPhases,
    );

    context.read<FocusProvider>().saveCustomMode(updatedMode);
    setState(() => _isEditing = false);
    ScaffoldMessenger.of(
      context,
    ).showSnackBar(const SnackBar(content: Text("Mode Saved!")));
  }

  void _cancelEdit() {
    _nameController.text = widget.mode.name;
    _tempPhases = widget.mode.phases
        .map(
          (p) => SessionPhase(type: p.type, durationMinutes: p.durationMinutes),
        )
        .toList();
    setState(() => _isEditing = false);
  }

  @override
  Widget build(BuildContext context) {
    return Card(
      margin: const EdgeInsets.symmetric(horizontal: 16, vertical: 8),
      clipBehavior: Clip.antiAlias,
      elevation: _isEditing ? 4 : 1,
      child: AnimatedSize(
        duration: const Duration(milliseconds: 300),
        alignment: Alignment.topCenter,
        curve: Curves.easeInOut,
        child: Padding(
          padding: const EdgeInsets.all(16.0),
          child: _isEditing ? _buildEditorView() : _buildOverview(),
        ),
      ),
    );
  }

  Widget _buildOverview() {
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Row(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Expanded(
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Text(
                    widget.mode.name,
                    style: const TextStyle(
                      fontWeight: FontWeight.bold,
                      fontSize: 18,
                    ),
                  ),
                  const SizedBox(height: 4),
                  Text(
                    widget.mode.isSystem ? "System Mode" : "Custom Mode",
                    style: TextStyle(
                      color: widget.mode.isSystem
                          ? Theme.of(context).colorScheme.onSurfaceVariant
                          : AppTheme.taskColor,
                      fontSize: 13,
                      fontWeight: FontWeight.w500,
                    ),
                  ),
                ],
              ),
            ),

            if (widget.mode.isSystem)
              const Padding(
                padding: EdgeInsets.all(8.0),
                child: Icon(Icons.lock_outline, color: Colors.grey),
              )
            else
              Row(
                mainAxisSize: MainAxisSize.min,
                children: [
                  IconButton(
                    icon: const Icon(Icons.edit, color: AppTheme.taskColor),
                    tooltip: "Edit Mode",
                    onPressed: () => setState(() => _isEditing = true),
                  ),
                  IconButton(
                    icon: const Icon(Icons.delete, color: AppTheme.errorColor),
                    tooltip: "Delete Mode",
                    onPressed: _confirmDelete,
                  ),
                ],
              ),
          ],
        ),

        const SizedBox(height: 8),

        Align(
          alignment: Alignment.centerLeft,
          child: ModeTimeline(
            phases: widget.mode.phases,
            currentPhaseIndex: 0,
            isRunning: false,
          ),
        ),
      ],
    );
  }

  Widget _buildEditorView() {
    return Column(
      crossAxisAlignment: CrossAxisAlignment.stretch,
      children: [
        Row(
          mainAxisAlignment: MainAxisAlignment.spaceBetween,
          children: [
            const Text(
              "Editing Mode",
              style: TextStyle(
                fontWeight: FontWeight.bold,
                fontSize: 16,
                color: AppTheme.taskColor,
              ),
            ),
            IconButton(icon: const Icon(Icons.close), onPressed: _cancelEdit),
          ],
        ),
        const SizedBox(height: 16),

        TextField(
          controller: _nameController,
          decoration: const InputDecoration(
            labelText: "Mode Name",
            border: OutlineInputBorder(),
          ),
        ),
        const SizedBox(height: 24),

        const Text(
          "Timeline Phases:",
          style: TextStyle(fontWeight: FontWeight.bold),
        ),
        const SizedBox(height: 8),
        const Text(
          "Tap a phase to edit or delete it.",
          style: TextStyle(fontSize: 12, color: Colors.grey),
        ),
        const SizedBox(height: 16),

        SingleChildScrollView(
          scrollDirection: Axis.horizontal,
          physics: const BouncingScrollPhysics(),
          child: Row(
            children: [
              ...List.generate(_tempPhases.length, (index) {
                final phase = _tempPhases[index];
                return Row(
                  children: [
                    GestureDetector(
                      onTap: () => _showPhaseDialog(index: index),
                      child: Container(
                        width: 56,
                        height: 56,
                        decoration: BoxDecoration(
                          shape: BoxShape.circle,
                          color: phase.type == PhaseType.focus
                              ? AppTheme.focusColor
                              : AppTheme.warningColor,
                          boxShadow: const [
                            BoxShadow(
                              color: Colors.black12,
                              blurRadius: 4,
                              offset: Offset(0, 2),
                            ),
                          ],
                        ),
                        child: Center(
                          child: Text(
                            "${phase.durationMinutes == -1 ? 'Flex' : phase.durationMinutes}\nm",
                            textAlign: TextAlign.center,
                            style: const TextStyle(
                              color: Colors.white,
                              fontWeight: FontWeight.bold,
                              fontSize: 12,
                            ),
                          ),
                        ),
                      ),
                    ),
                    Container(
                      width: 24,
                      height: 2,
                      color: Colors.grey.shade300,
                    ),
                  ],
                );
              }),
              GestureDetector(
                onTap: () => _showPhaseDialog(),
                child: Container(
                  width: 56,
                  height: 56,
                  decoration: BoxDecoration(
                    shape: BoxShape.circle,
                    color: Theme.of(
                      context,
                    ).colorScheme.surfaceContainerHighest,
                    border: Border.all(
                      color: Theme.of(context).dividerColor,
                      width: 2,
                    ),
                  ),
                  child: const Icon(Icons.add, color: AppTheme.wifiOffColor),
                ),
              ),
            ],
          ),
        ),

        const SizedBox(height: 24),

        Row(
          mainAxisAlignment: MainAxisAlignment.end,
          children: [
            TextButton(onPressed: _cancelEdit, child: const Text("Cancel")),
            const SizedBox(width: 8),
            ElevatedButton.icon(
              onPressed: _saveMode,
              icon: const Icon(Icons.save),
              label: const Text("Save Mode"),
            ),
          ],
        ),
      ],
    );
  }
}
