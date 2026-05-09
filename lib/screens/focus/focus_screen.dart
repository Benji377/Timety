import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import 'package:timety/screens/statistics_screen.dart';
import '../../theme/app_theme.dart';
import '../../providers/focus_provider.dart';
import '../../providers/user_provider.dart';
import '../../providers/settings_provider.dart';
import '../../data/focus/focus_models.dart';
import '../../widgets/focus_mode_timeline.dart';
import '../../widgets/interactive_gauge.dart';
import '../../widgets/app_dialogs.dart';
import '../calendar_screen.dart';
import 'focus_modes_screen.dart';
import '../settings_screen.dart';

class FocusScreen extends StatelessWidget {
  const FocusScreen({super.key});

  String _formatDigitalTime(int totalSeconds) {
    int minutes = totalSeconds ~/ 60;
    int seconds = totalSeconds % 60;
    return '${minutes.toString().padLeft(2, '0')}:${seconds.toString().padLeft(2, '0')}';
  }

  // --- 1. ALERTS / DISTRACTIONS BOTTOM SHEET ---
  void _showDistractionSheet(BuildContext context, FocusProvider provider) {
    // Fixed list of possible events
    final events = [
      {'name': 'Distracted', 'icon': Icons.warning_amber, 'color': Colors.red},
      {
        'name': 'Hydrated / Drink',
        'icon': Icons.water_drop,
        'color': Colors.blue,
      },
      {
        'name': 'Stretched',
        'icon': Icons.accessibility_new,
        'color': Colors.orange,
      },
      {'name': 'Snack', 'icon': Icons.restaurant, 'color': Colors.green},
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
                    provider.logDistraction(e['name'] as String);
                    Navigator.pop(context);
                    ScaffoldMessenger.of(context).showSnackBar(
                      SnackBar(
                        content: Text('Logged: ${e['name']}'),
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

  // --- TAG MANAGEMENT ---
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
    focusProvider.attachUserProvider(context.read<UserProvider>());
    final isRunning = focusProvider.isRunning;
    final isPaused = focusProvider.isPaused;
    final activeMode = focusProvider.activeMode;
    final modes = focusProvider.modes;

    bool isFlexibleMode = activeMode?.type == FocusModeType.flexible;
    bool canDrag = isFlexibleMode && !isRunning && !isPaused;

    double gaugeProgress = 1.0;
    bool isStopwatchMode = false;
    String label = "FOCUS";
    String centerText = "25:00";

    if (activeMode != null && activeMode.phases.isNotEmpty) {
      final currentPhase = activeMode.phases[focusProvider.currentPhaseIndex];

      if (canDrag) {
        int currentMinutes = currentPhase.durationMinutes == -1
            ? 25
            : currentPhase.durationMinutes;
        gaugeProgress = currentMinutes / 120.0;
        label = "SET TIME";
        centerText = _formatDigitalTime(currentMinutes * 60);
      } else if (currentPhase.durationMinutes > 0 ||
          currentPhase.durationMinutes == -1) {
        int totalPhaseSeconds = currentPhase.durationMinutes > 0
            ? currentPhase.durationMinutes * 60
            : 25 * 60;
        if (totalPhaseSeconds == 0) totalPhaseSeconds = 1;

        gaugeProgress =
            focusProvider.secondsRemainingInPhase / totalPhaseSeconds;
        label = currentPhase.type == PhaseType.rest ? "REST" : "FOCUS";
        centerText = _formatDigitalTime(focusProvider.secondsRemainingInPhase);
      } else {
        isStopwatchMode = isRunning;
        gaugeProgress = 0.0;
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
      appBar: AppBar(
        title: const Text('Focus'),
        actions: [
          IconButton(
            icon: const Icon(Icons.bar_chart),
            onPressed: () {
              Navigator.push(
                context,
                MaterialPageRoute(
                  builder: (context) => const StatisticsScreen(),
                ),
              );
            },
          ),
          IconButton(
            icon: const Icon(Icons.calendar_today),
            tooltip: 'Calendar View',
            onPressed: () {
              Navigator.push(
                context,
                MaterialPageRoute(builder: (context) => const CalendarScreen()),
              );
            },
          ),
          const SizedBox(width: 8),
        ],
      ),
      body: Column(
        children: [
          const SizedBox(height: 10),

          // --- MODE SELECTOR ---
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
                child: InkWell(
                  onTap: (isRunning || isPaused)
                      ? null
                      : () {
                          Navigator.push(
                            context,
                            MaterialPageRoute(
                              builder: (context) => const FocusModesScreen(),
                            ),
                          );
                        },
                  borderRadius: AppTheme.brMedium,
                  child: Padding(
                    padding: const EdgeInsets.symmetric(vertical: 8.0),
                    child: Text(
                      activeMode?.name.toUpperCase() ?? 'SELECT MODE',
                      textAlign: TextAlign.center,
                      style: TextStyle(
                        fontSize: 16,
                        fontWeight: FontWeight.bold,
                        letterSpacing: 1.2,
                        color: (isRunning || isPaused)
                            ? Colors.grey
                            : Theme.of(context).colorScheme.primary,
                      ),
                    ),
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

          // --- DAILY GOAL BADGE ---
          Align(
            alignment: Alignment.topCenter,
            child: Padding(
              padding: const EdgeInsets.only(bottom: 12.0),
              child: GestureDetector(
                onTap: () {
                  Navigator.push(
                    context,
                    MaterialPageRoute(
                      builder: (context) => const SettingsScreen(),
                    ),
                  );
                },
                child: Container(
                  padding: const EdgeInsets.symmetric(
                    horizontal: 16,
                    vertical: 8,
                  ),
                  decoration: BoxDecoration(
                    color: Theme.of(
                      context,
                    ).colorScheme.primaryContainer.withValues(alpha: 0.5),
                    borderRadius: BorderRadius.circular(20),
                    border: Border.all(
                      color: Theme.of(
                        context,
                      ).colorScheme.primary.withValues(alpha: 0.3),
                    ),
                  ),
                  child: Row(
                    mainAxisSize: MainAxisSize.min,
                    children: [
                      Icon(
                        Icons.track_changes,
                        size: 18,
                        color: Theme.of(context).colorScheme.primary,
                      ),
                      const SizedBox(width: 8),
                      Text(
                        '${focusProvider.getMinutesFocusedToday()} / ${context.watch<SettingsProvider>().dailyGoalMins} m',
                        style: TextStyle(
                          fontWeight: FontWeight.w900,
                          fontSize: 15,
                          color: Theme.of(context).colorScheme.primary,
                        ),
                      ),
                    ],
                  ),
                ),
              ),
            ),
          ),

          // --- GAUGE & SIDE BUTTONS ---
          SizedBox(
            height: 320,
            width: double.infinity,
            child: Stack(
              alignment: Alignment.center,
              children: [
                InteractiveGauge(
                  key: ValueKey(activeMode?.id),
                  progress: gaugeProgress,
                  isStopwatch: isStopwatchMode,
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
                  onChanged: (newProgress) {
                    if (canDrag) {
                      int newMins = (newProgress * 120).round();
                      if (newMins < 1) newMins = 1;
                      focusProvider.setFlexibleDuration(newMins);
                    }
                  },
                ),

                // TIME MACHINE BUTTON
                Positioned(
                  top: 16,
                  left: 16,
                  child: IconButton.filledTonal(
                    icon: const Icon(Icons.history),
                    tooltip: "Log Past Session",
                    iconSize: 28,
                    padding: const EdgeInsets.all(12),
                    onPressed: (isRunning || isPaused)
                        ? null
                        : () => AppDialogs.showTimeMachineDialog(
                            context,
                            focusProvider,
                          ),
                  ),
                ),

                // DISTRACTIONS BUTTON
                Positioned(
                  top: 16,
                  right: 16,
                  child: IconButton.filledTonal(
                    icon: const Icon(Icons.warning_amber),
                    tooltip: "Log Distraction/Event",
                    iconSize: 28,
                    padding: const EdgeInsets.all(12),
                    onPressed: (isRunning || isPaused)
                        ? () => _showDistractionSheet(context, focusProvider)
                        : null,
                  ),
                ),
              ],
            ),
          ),

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
                  heroTag: "focus_main_button",
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
                      focusProvider.stopSession(
                        userProvider: context.read<UserProvider>(),
                      );
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
