import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import '../providers/focus_provider.dart';
import '../data/focus_models.dart';
import '../widgets/interactive_gauge.dart';
import '../widgets/focus_mode_timeline.dart';

class FocusScreen extends StatelessWidget {
  const FocusScreen({super.key});

  String _formatDigitalTime(int totalSeconds) {
    int minutes = totalSeconds ~/ 60;
    int seconds = totalSeconds % 60;
    return '${minutes.toString().padLeft(2, '0')}:${seconds.toString().padLeft(2, '0')}';
  }

  // --- TAG MANAGEMENT POPUPS ---
  void _showTagSelector(BuildContext context, FocusProvider provider) {
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
                  itemCount: provider.tags.length,
                  itemBuilder: (context, index) {
                    final tag = provider.tags[index];
                    final isSelected = provider.selectedTag?.id == tag.id;
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
                          ? const Icon(Icons.check, color: Colors.green)
                          : null,
                      onTap: () {
                        provider.setSelectedTag(tag);
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
                  _showCreateTagDialog(context, provider);
                },
              ),
            ],
          ),
        );
      },
    );
  }

  void _showCreateTagDialog(BuildContext context, FocusProvider provider) {
    final TextEditingController controller = TextEditingController();
    Color selectedColor = Colors.blue;

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
                  provider.createTag(controller.text.trim(), selectedColor);
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

  @override
  Widget build(BuildContext context) {
    final focusProvider = context.watch<FocusProvider>();
    final isRunning = focusProvider.isRunning;
    final isPaused = focusProvider.isPaused;
    final activeMode = focusProvider.activeMode;
    final modes = focusProvider.modes;

    bool isFlexibleMode = activeMode?.type == FocusModeType.flexible;
    bool canDrag = isFlexibleMode && !isRunning && !isPaused;

    int currentMinutes = 25;
    int maxMinutes = 120;
    String label = "FOCUS";
    String centerText = "25:00";

    if (activeMode != null && activeMode.phases.isNotEmpty) {
      final currentPhase = activeMode.phases[focusProvider.currentPhaseIndex];

      if (canDrag) {
        currentMinutes = currentPhase.durationMinutes == -1
            ? 25
            : currentPhase.durationMinutes;
        label = "SET TIME";
        centerText = _formatDigitalTime(currentMinutes * 60);
      } else if (currentPhase.durationMinutes > 0) {
        currentMinutes = (focusProvider.secondsRemainingInPhase / 60).ceil();
        maxMinutes = currentPhase.durationMinutes;
        label = currentPhase.type == PhaseType.rest ? "REST" : "FOCUS";
        centerText = _formatDigitalTime(focusProvider.secondsRemainingInPhase);
      } else {
        currentMinutes = (focusProvider.currentSecondsFocussed / 60).floor();
        maxMinutes = 0;
        label = "STOPWATCH";
        centerText = _formatDigitalTime(focusProvider.currentSecondsFocussed);
      }
    }

    void cycleMode(int direction) {
      if (isRunning || isPaused || activeMode == null) return;
      int currentIndex = modes.indexWhere((m) => m.id == activeMode.id);
      int nextIndex = (currentIndex + direction) % modes.length;
      if (nextIndex < 0) nextIndex = modes.length - 1;
      focusProvider.setActiveMode(modes[nextIndex]);
    }

    return Scaffold(
      // RESTORED APP BAR
      appBar: AppBar(
        title: const Text('Focus'),
        actions: [
          IconButton(
            icon: const Icon(Icons.bar_chart),
            tooltip: 'Statistics',
            onPressed: () {}, // TODO: Navigate to Statistics
          ),
          IconButton(
            icon: const Icon(Icons.calendar_month),
            tooltip: 'Calendar',
            onPressed: () {}, // TODO: Navigate to Calendar
          ),
          const SizedBox(width: 8),
        ],
      ),
      body: Column(
        children: [
          // --- TOP RIGHT: DAILY TARGET ---
          Align(
            alignment: Alignment.topRight,
            child: Padding(
              padding: const EdgeInsets.only(top: 8.0, right: 24.0),
              child: Text(
                '${focusProvider.getMinutesFocusedToday()} / ${focusProvider.dailyTargetMinutes} min',
                style: const TextStyle(
                  fontWeight: FontWeight.w900,
                  fontSize: 20,
                ),
              ),
            ),
          ),

          const SizedBox(height: 10),

          // --- CAROUSEL MODE SELECTOR ---
          Row(
            mainAxisAlignment: MainAxisAlignment.center,
            children: [
              IconButton(
                icon: const Icon(
                  Icons.arrow_back_ios,
                  size: 20,
                  color: Colors.grey,
                ),
                onPressed: (isRunning || isPaused) ? null : () => cycleMode(-1),
              ),
              SizedBox(
                width: 180,
                child: Text(
                  activeMode?.name.toUpperCase() ?? 'SELECT MODE',
                  textAlign: TextAlign.center,
                  style: const TextStyle(
                    fontSize: 16,
                    fontWeight: FontWeight.bold,
                    letterSpacing: 1.2,
                  ),
                ),
              ),
              IconButton(
                icon: const Icon(
                  Icons.arrow_forward_ios,
                  size: 20,
                  color: Colors.grey,
                ),
                onPressed: (isRunning || isPaused) ? null : () => cycleMode(1),
              ),
            ],
          ),

          const Spacer(),

          // --- GAUGE & SIDE BUTTONS IN A RESTRICTED STACK ---
          SizedBox(
            height: 320,
            width: 600,
            child: Stack(
              alignment: Alignment.center,
              children: [
                // Center Gauge
                InteractiveGauge(
                  key: ValueKey(activeMode?.id),
                  maxMinutes: maxMinutes,
                  initialMinutes: currentMinutes,
                  centerText: centerText,

                  bottomText: focusProvider.selectedTag?.name ?? "No Tag",
                  bottomTextColor: focusProvider.selectedTag != null
                      ? Color(focusProvider.selectedTag!.colorValue)
                      : Colors.grey,
                  onBottomTextTapped: (isRunning || isPaused)
                      ? null
                      : () => _showTagSelector(context, focusProvider),

                  isInteractive: canDrag,
                  label: label,
                  onChanged: (newMinutes) {
                    if (canDrag && newMinutes > 0) {
                      focusProvider.setFlexibleDuration(newMinutes);
                    }
                  },
                ),

                // Left Button (History) - Fixed strictly to the left edge of the 380px box
                Positioned(
                  left: 0,
                  child: IconButton.filledTonal(
                    icon: const Icon(Icons.history),
                    iconSize: 28,
                    padding: const EdgeInsets.all(12),
                    onPressed: (isRunning || isPaused) ? null : () {},
                  ),
                ),

                // Right Button (Distraction) - Fixed strictly to the right edge of the 380px box
                Positioned(
                  right: 0,
                  child: IconButton.filledTonal(
                    icon: const Icon(Icons.warning_amber),
                    iconSize: 28,
                    padding: const EdgeInsets.all(12),
                    onPressed: (isRunning || isPaused) ? () {} : null,
                  ),
                ),
              ],
            ),
          ),

          // --- TIMELINE WIDGET ---
          ModeTimeline(
            phases: activeMode?.phases ?? [],
            currentPhaseIndex: focusProvider.currentPhaseIndex,
            isRunning: isRunning || isPaused,
          ),

          const Spacer(),

          // --- BOTTOM CONTROLS ---
          Row(
            mainAxisAlignment: MainAxisAlignment.center,
            children: [
              Opacity(
                opacity: (isRunning || isPaused) ? 1.0 : 0.0,
                child: IconButton(
                  icon: const Icon(Icons.refresh),
                  iconSize: 32,
                  color: Colors.grey.shade600,
                  onPressed: (isRunning || isPaused)
                      ? () => focusProvider.resetSession()
                      : null,
                ),
              ),

              const SizedBox(width: 32),

              SizedBox(
                height: 80,
                width: 80,
                child: FloatingActionButton(
                  shape: const CircleBorder(),
                  elevation: 4,
                  backgroundColor: isRunning
                      ? Colors.red.shade100
                      : Theme.of(context).colorScheme.primaryContainer,
                  foregroundColor: isRunning
                      ? Colors.red
                      : Theme.of(context).colorScheme.primary,
                  onPressed: () {
                    if (isRunning) {
                      focusProvider.stopSession();
                    } else {
                      focusProvider.startSession();
                    }
                  },
                  child: Icon(
                    isRunning ? Icons.stop : Icons.play_arrow,
                    size: 40,
                  ),
                ),
              ),

              const SizedBox(width: 32),

              Opacity(
                opacity: (isRunning || isPaused) ? 1.0 : 0.0,
                child: IconButton(
                  icon: Icon(isPaused ? Icons.play_circle_fill : Icons.pause),
                  iconSize: 32,
                  color: Colors.grey.shade600,
                  onPressed: (isRunning || isPaused)
                      ? () {
                          if (isPaused) {
                            focusProvider.startSession();
                          } else {
                            focusProvider.pauseSession();
                          }
                        }
                      : null,
                ),
              ),
            ],
          ),

          const SizedBox(height: 40),
        ],
      ),
    );
  }
}
