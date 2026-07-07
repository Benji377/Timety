# Flutter → Kotlin data migration (temporary)

The Kotlin rewrite (v2.0.0, versionCode 240) installs **in place** over the old Flutter
app (same `applicationId`), so the Flutter app's on-device data survives the update:

| What | Where on device | Format |
|---|---|---|
| Tasks, habits, focus data, user profile | `<dataDir>/app_flutter/*.hive` (6 boxes: `tasksbox`, `habitsbox`, `userprofilebox`, `focusmodesbox`, `focussessionsbox`, `focustagsbox`) | Hive 2.2.3 binary box files |
| Settings | `<dataDir>/shared_prefs/FlutterSharedPreferences.xml` | SharedPreferences, keys prefixed `flutter.` |

On the first launch after the update, the app migrates this data automatically. Once all
users are on 2.x this whole mechanism can be deleted — see **How to remove** below.

## How it works

Entry point: `FlutterMigration.runIfNeeded()`, called from `TimetyApplication.onCreate()`
**before** `ReminderScheduler.resyncAll()` (so imported reminders get scheduled) and
before the UI composes (`MainActivity` waits on `TimetyApplication.startupComplete`,
because ViewModels seed default rows into an empty database, which would trip the
empty-database guard).

Two independent steps, each behind its own boolean flag in the
`flutter_migration` SharedPreferences file:

1. **Data** (`dataMigrated` flag): `HiveBoxReader` parses the six Hive box files
   (format ported from hive 2.2.3 `frame.dart`/`binary_reader_impl.dart`; the field IDs
   come from the old repo's generated `*.g.dart` adapters). It decodes them directly
   into the JSON dialect of the Flutter app's backup export, and feeds that payload to
   `BackupService.importFromJson()` — the same tested path used for manual backup
   imports (atomic via a Room transaction).
2. **Settings** (`settingsMigrated` flag): reads `FlutterSharedPreferences`, maps the
   old keys to the new DataStore keys (`themeMode` index → `theme` string,
   `notificationHour`+`Min` → `dailyMotivationTime` "HH:mm", `upcomingTasksDays` →
   `upcomingTasksHorizon`, etc.) and writes via `SettingsRepository`. Best-effort by
   design: a failure here is logged, flagged as done, and never blocks the data step.

### Safety properties

- **Fresh installs**: no `.hive` files → both flags set immediately, nothing else happens.
- **User-data guard**: data is only imported while Room holds no *user-created* data —
  no XP, tasks, habits, or focus sessions. This protects devices that already used the
  Kotlin app (the import path clears all tables before inserting). A bare seeded
  default profile deliberately does **not** trip the guard: ViewModels create one as
  soon as the UI composes, and it must not block a retry after a failed attempt.
- **Failure/retry**: a failed data migration leaves the Hive files and the flag
  untouched and retries on the next launch. Once the user creates real data in the new
  app, the guard stops further attempts — self-limiting, no crash, no data loss.
- **The old Hive files are never modified or deleted.** They stay on the device as a
  safety net until this mechanism is removed.

## How to remove (after a couple of releases)

1. Delete the package `app/src/main/java/io/github/benji377/timety/migration/`
   (`FlutterMigration.kt`, `HiveBoxReader.kt`).
2. In `TimetyApplication.onCreate()`: remove the `FlutterMigration.runIfNeeded()` call.
   Optionally simplify away the `startupComplete` flow and its gate in `MainActivity`
   (harmless to keep — it only delays composition by microseconds when idle).
3. Optionally add one-time cleanup to delete `<dataDir>/app_flutter/` and the
   `flutter_migration` prefs file to reclaim space on long-time users' devices.
4. `docs/flutter-migration.md` (this file) can go too.

Kept on purpose (not migration-specific): `BackupService.importFromJson()` and
`UserRepository.getUserProfileSnapshot()`.

## Verifying manually

1. `adb uninstall io.github.benji377.timety`
2. Install the old Flutter APK (GitHub release ≤ v1.5.1; re-sign with the debug key via
   `apksigner` when testing against a debug build of the Kotlin app).
3. Create data in the Flutter app: tasks (with subtasks/reminders), habits with
   completions, focus sessions, change settings.
4. `adb install -r` the Kotlin APK, launch, and verify everything shows up; check
   `run-as io.github.benji377.timety` → `databases/timety_database` and the
   `flutter_migration` prefs flags.
