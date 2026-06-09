import 'package:flutter/material.dart';
import 'package:flutter/cupertino.dart';
import 'package:flutter_localizations/flutter_localizations.dart';

/// Provides Italian Material localizations as a fallback for Ladin
class LldMaterialLocalizationsDelegate
    extends LocalizationsDelegate<MaterialLocalizations> {
  const LldMaterialLocalizationsDelegate();

  @override
  bool isSupported(Locale locale) => locale.languageCode == 'lld';

  @override
  Future<MaterialLocalizations> load(Locale locale) async {
    // Borrow Italian for system UI like DatePickers and Dialogs
    return await GlobalMaterialLocalizations.delegate.load(const Locale('it'));
  }

  @override
  bool shouldReload(
    covariant LocalizationsDelegate<MaterialLocalizations> old,
  ) => false;
}

/// Provides Italian Cupertino localizations as a fallback for Ladin
class LldCupertinoLocalizationsDelegate
    extends LocalizationsDelegate<CupertinoLocalizations> {
  const LldCupertinoLocalizationsDelegate();

  @override
  bool isSupported(Locale locale) => locale.languageCode == 'lld';

  @override
  Future<CupertinoLocalizations> load(Locale locale) async {
    return await GlobalCupertinoLocalizations.delegate.load(const Locale('it'));
  }

  @override
  bool shouldReload(
    covariant LocalizationsDelegate<CupertinoLocalizations> old,
  ) => false;
}
