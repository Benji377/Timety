import 'dart:ui' as ui;
import 'package:flutter/material.dart';
import '../data/focus/focus_models.dart';
import '../l10n/app_localizations.dart';
import '../providers/settings_provider.dart';

/// Global Translation Helper
/// Use this to get localized strings anywhere in the app where [BuildContext] is unavailable.
AppLocalizations getL10n({SettingsProvider? settings}) {
  // 1. Get the custom app locale from settings, or fallback to the OS system language
  final locale = settings?.appLocale ?? ui.PlatformDispatcher.instance.locale;

  // 2. Generate and return the translations synchronously!
  return lookupAppLocalizations(locale);
}

/// Similar to the function in the data/focus/focus_models.dart, but adapted to
/// use the Buildcontext for better efficiency
String getLocalizedFocusModeName(BuildContext context, FocusMode mode) {
  if (!mode.isSystem) {
    return mode.name;
  }

  final l10n = AppLocalizations.of(context)!;
  switch (mode.id) {
    case 'system_stopwatch':
      return l10n.focusModeStopwatch;
    case 'system_flexible':
      return l10n.focusModeFlexible;
    case 'system_pomodoro':
      return l10n.focusModePomodoro;
    default:
      return mode.name;
  }
}
