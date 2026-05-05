import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import '../providers/focus_provider.dart';
import '../data/focus_models.dart';
import '../widgets/focus_mode_timeline.dart';

class FocusModesScreen extends StatelessWidget {
  const FocusModesScreen({super.key});

  @override
  Widget build(BuildContext context) {
    final focusProvider = context.watch<FocusProvider>();
    final modes = focusProvider.modes;

    return Scaffold(
      appBar: AppBar(
        title: const Text('Manage Modes'),
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
  
  // Replaces _isExpanded. Controls whether we see the Overview or the Editor.
  bool _isEditing = false; 

  @override
  void initState() {
    super.initState();
    _nameController = TextEditingController(text: widget.mode.name);
    _tempPhases = widget.mode.phases.map((p) => SessionPhase(type: p.type, durationMinutes: p.durationMinutes)).toList();
  }

  @override
  void didUpdateWidget(covariant ModeEditCard oldWidget) {
    super.didUpdateWidget(oldWidget);
    if (oldWidget.mode.id != widget.mode.id && !_isEditing) {
      _nameController.text = widget.mode.name;
      _tempPhases = widget.mode.phases.map((p) => SessionPhase(type: p.type, durationMinutes: p.durationMinutes)).toList();
    }
  }

  @override
  void dispose() {
    _nameController.dispose();
    super.dispose();
  }

  // --- DIALOG: ADD / EDIT NODE ---
  void _showPhaseDialog({int? index}) {
    bool isNew = index == null;
    PhaseType selectedType = isNew ? PhaseType.focus : _tempPhases[index].type;
    TextEditingController timeController = TextEditingController(
      text: isNew ? '25' : _tempPhases[index].durationMinutes.toString(),
    );

    showDialog(
      context: context,
      builder: (context) {
        return StatefulBuilder(
          builder: (context, setDialogState) {
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
                    selected: {selectedType},
                    onSelectionChanged: (Set<PhaseType> newSelection) {
                      setDialogState(() => selectedType = newSelection.first);
                    },
                  ),
                  const SizedBox(height: 24),
                  TextField(
                    controller: timeController,
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
                    onPressed: () {
                      setState(() => _tempPhases.removeAt(index));
                      Navigator.pop(context);
                    },
                    child: const Text("Delete", style: TextStyle(color: Colors.red)),
                  ),
                TextButton(
                  onPressed: () => Navigator.pop(context),
                  child: const Text("Cancel"),
                ),
                ElevatedButton(
                  onPressed: () {
                    int? mins = int.tryParse(timeController.text);
                    if (mins != null && mins > 0) {
                      if (mins > 240) mins = 240; 
                      
                      setState(() {
                        if (isNew) {
                          _tempPhases.add(SessionPhase(type: selectedType, durationMinutes: mins!));
                        } else {
                          _tempPhases[index] = SessionPhase(type: selectedType, durationMinutes: mins!);
                        }
                      });
                      Navigator.pop(context);
                    }
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

  void _confirmDelete() async {
    final confirm = await showDialog<bool>(
      context: context,
      builder: (ctx) => AlertDialog(
        title: const Text("Delete Mode?"),
        content: const Text("Are you sure you want to delete this custom mode?"),
        actions: [
          TextButton(onPressed: () => Navigator.pop(ctx, false), child: const Text("Cancel")),
          TextButton(onPressed: () => Navigator.pop(ctx, true), child: const Text("Delete", style: TextStyle(color: Colors.red))),
        ],
      )
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
    setState(() => _isEditing = false); // Hide the editor after saving
    ScaffoldMessenger.of(context).showSnackBar(const SnackBar(content: Text("Mode Saved!")));
  }

  void _cancelEdit() {
    // Reset inputs back to original mode data
    _nameController.text = widget.mode.name;
    _tempPhases = widget.mode.phases.map((p) => SessionPhase(type: p.type, durationMinutes: p.durationMinutes)).toList();
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

  // --- OVERVIEW: The Clean Default State ---
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
                  Text(widget.mode.name, style: const TextStyle(fontWeight: FontWeight.bold, fontSize: 18)),
                  const SizedBox(height: 4),
                  Text(
                    widget.mode.isSystem ? "System Mode" : "Custom Mode", 
                    style: TextStyle(color: widget.mode.isSystem ? Colors.grey : Colors.blue, fontSize: 13, fontWeight: FontWeight.w500)
                  ),
                ],
              ),
            ),
            
            // Trailing Actions
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
                    icon: const Icon(Icons.edit, color: Colors.blue),
                    tooltip: "Edit Mode",
                    onPressed: () => setState(() => _isEditing = true),
                  ),
                  IconButton(
                    icon: const Icon(Icons.delete, color: Colors.red),
                    tooltip: "Delete Mode",
                    onPressed: _confirmDelete,
                  ),
                ],
              ),
          ],
        ),
        
        const SizedBox(height: 8),
        
        // The permanently visible small timeline
        // Wrapped in an Align to ensure it stays neat on the left
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

  // --- EDITOR: Only visible when "Edit" is tapped ---
  Widget _buildEditorView() {
    return Column(
      crossAxisAlignment: CrossAxisAlignment.stretch,
      children: [
        Row(
          mainAxisAlignment: MainAxisAlignment.spaceBetween,
          children: [
            const Text("Editing Mode", style: TextStyle(fontWeight: FontWeight.bold, fontSize: 16, color: Colors.blue)),
            IconButton(
              icon: const Icon(Icons.close),
              onPressed: _cancelEdit,
            )
          ],
        ),
        const SizedBox(height: 16),
        
        TextField(
          controller: _nameController,
          decoration: const InputDecoration(labelText: "Mode Name", border: OutlineInputBorder()),
        ),
        const SizedBox(height: 24),
        
        const Text("Timeline Phases:", style: TextStyle(fontWeight: FontWeight.bold)),
        const SizedBox(height: 8),
        const Text("Tap a phase to edit or delete it.", style: TextStyle(fontSize: 12, color: Colors.grey)),
        const SizedBox(height: 16),

        // Interactive Timeline Nodes
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
                          color: phase.type == PhaseType.focus ? Colors.green : Colors.orange,
                          boxShadow: const [BoxShadow(color: Colors.black12, blurRadius: 4, offset: Offset(0, 2))],
                        ),
                        child: Center(
                          child: Text(
                            "${phase.durationMinutes == -1 ? 'Flex' : phase.durationMinutes}\nm", 
                            textAlign: TextAlign.center,
                            style: const TextStyle(color: Colors.white, fontWeight: FontWeight.bold, fontSize: 12),
                          ),
                        ),
                      ),
                    ),
                    Container(width: 24, height: 2, color: Colors.grey.shade300),
                  ],
                );
              }),
              // "Add Phase" Node
              GestureDetector(
                onTap: () => _showPhaseDialog(),
                child: Container(
                  width: 56,
                  height: 56,
                  decoration: BoxDecoration(
                    shape: BoxShape.circle,
                    color: Colors.grey.shade200,
                    border: Border.all(color: Colors.grey.shade400, style: BorderStyle.solid, width: 2),
                  ),
                  child: const Icon(Icons.add, color: Colors.grey),
                ),
              ),
            ],
          ),
        ),
        
        const SizedBox(height: 24),

        // Action Buttons
        Row(
          mainAxisAlignment: MainAxisAlignment.end,
          children: [
            TextButton(
              onPressed: _cancelEdit,
              child: const Text("Cancel"),
            ),
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