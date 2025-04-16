import 'package:timety/commons.dart';
import 'package:timety/features/focus/domain/focus_summary.dart';

final focusSummeryProvider = Provider<FocusSummary>((ref){
  final goal = 120.0;
  final focused = 75.0;
  return FocusSummary(goal, focused);
});