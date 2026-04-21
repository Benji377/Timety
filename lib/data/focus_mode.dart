import 'dart:convert';

enum FocusStepType { start, focus, rest, end, loop, stopwatch, flexible }

enum FocusStepBehavior { countUp, countDown }

class FocusStep {
  final int durationMins;
  final FocusStepType type;
  final FocusStepBehavior behavior;

  FocusStep({
    required this.durationMins,
    required this.type,
    this.behavior = FocusStepBehavior.countDown,
  });

  Map<String, dynamic> toMap() {
    return {
      'durationMins': durationMins,
      'type': type.index,
      'behavior': behavior.index,
    };
  }

  factory FocusStep.fromMap(Map<String, dynamic> map) {
    return FocusStep(
      durationMins: map['durationMins'],
      type: FocusStepType.values[map['type']],
      behavior: FocusStepBehavior.values[map['behavior']],
    );
  }
}

class FocusMode {
  final int? id;
  final String title;
  final bool isCustom;
  final List<FocusStep> steps;

  FocusMode({
    this.id,
    required this.title,
    this.isCustom = true,
    required this.steps,
  });

  Map<String, dynamic> toMap() {
    return {
      if (id != null) 'id': id,
      'title': title,
      'isCustom': isCustom ? 1 : 0,
      'steps': jsonEncode(steps.map((s) => s.toMap()).toList()),
    };
  }

  factory FocusMode.fromMap(Map<String, dynamic> map) {
    return FocusMode(
      id: map['id'],
      title: map['title'],
      isCustom: (map['isCustom'] ?? 1) == 1,
      steps: map['steps'] != null 
          ? (jsonDecode(map['steps']) as List).map((s) => FocusStep.fromMap(s)).toList()
          : [],
    );
  }
}
