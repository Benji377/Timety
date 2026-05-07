import 'dart:convert';
import 'dart:io';
import 'package:flutter/material.dart';
import 'package:path_provider/path_provider.dart';
import 'package:share_plus/share_plus.dart';
import 'package:file_picker/file_picker.dart';
import 'package:shared_preferences/shared_preferences.dart';
import 'package:archive/archive_io.dart';
import '../widgets/app_dialogs.dart';

class BackupService {
  // --- EXPORT BACKUP ---
  static Future<void> exportBackup(BuildContext context) async {
    try {
      final appDir = await getApplicationDocumentsDirectory();
      final tempDir = await getTemporaryDirectory();
      final backupDirPath = '${tempDir.path}/timety_backup';
      final backupDir = Directory(backupDirPath);

      // 1. Create a fresh temporary backup folder
      if (backupDir.existsSync()) {
        backupDir.deleteSync(recursive: true);
      }
      backupDir.createSync();

      // 2. Export SharedPreferences to a JSON file
      final prefs = await SharedPreferences.getInstance();
      final prefsMap = <String, dynamic>{};
      for (var key in prefs.getKeys()) {
        prefsMap[key] = prefs.get(key);
      }
      final prefsFile = File('$backupDirPath/prefs.json');
      await prefsFile.writeAsString(jsonEncode(prefsMap));

      // 3. Copy all Hive database files (.hive)
      final files = appDir.listSync();
      for (var file in files) {
        if (file is File && file.path.endsWith('.hive')) {
          final fileName = file.path.split(Platform.pathSeparator).last;
          file.copySync('$backupDirPath/$fileName');
        }
      }

      // 4. Zip the folder
      final encoder = ZipFileEncoder();
      final zipPath =
          '${tempDir.path}/Timety_Backup_${DateTime.now().millisecondsSinceEpoch}.zip';
      encoder.create(zipPath);
      encoder.addDirectory(backupDir);
      encoder.close();

      // 5. Open native share sheet (Save to Files, Google Drive, Email, etc.)
      final xFile = XFile(zipPath, mimeType: 'application/zip');
      await SharePlus.instance.share(
        ShareParams(subject: 'Timety Backup', files: [xFile]),
      );
    } catch (e) {
      if (context.mounted) {
        ScaffoldMessenger.of(
          context,
        ).showSnackBar(SnackBar(content: Text('Export failed: $e')));
      }
    }
  }

  // --- IMPORT BACKUP ---
  static Future<void> importBackup(BuildContext context) async {
    try {
      // 1. Pick the zip file
      FilePickerResult? result = await FilePicker.pickFiles(
        type: FileType.custom,
        allowedExtensions: ['zip'],
      );

      if (result == null || result.files.single.path == null) return;
      if (!context.mounted) return;

      final confirm = await AppDialogs.showConfirmation(
        context: context,
        title: "Restore Backup?",
        content:
            "This will OVERWRITE all your current data. This cannot be undone. Are you sure?",
      );

      if (confirm != true) return;

      final zipFile = File(result.files.single.path!);
      final appDir = await getApplicationDocumentsDirectory();

      // 2. Unzip the file
      final bytes = zipFile.readAsBytesSync();
      final archive = ZipDecoder().decodeBytes(bytes);

      for (final file in archive) {
        final filename = file.name;
        if (file.isFile) {
          final data = file.content as List<int>;

          if (filename == 'prefs.json') {
            // Restore SharedPreferences
            final jsonStr = utf8.decode(data);
            final Map<String, dynamic> prefsMap = jsonDecode(jsonStr);
            final prefs = await SharedPreferences.getInstance();
            await prefs.clear(); // Clear existing

            for (var entry in prefsMap.entries) {
              if (entry.value is bool) {
                await prefs.setBool(entry.key, entry.value);
              } else if (entry.value is int) {
                await prefs.setInt(entry.key, entry.value);
              } else if (entry.value is double) {
                await prefs.setDouble(entry.key, entry.value);
              } else if (entry.value is String) {
                await prefs.setString(entry.key, entry.value);
              } else if (entry.value is List) {
                await prefs.setStringList(
                  entry.key,
                  (entry.value as List).map((e) => e.toString()).toList(),
                );
              }
            }
          } else if (filename.endsWith('.hive')) {
            // Overwrite Hive files
            final outFile = File('${appDir.path}/$filename');
            outFile.writeAsBytesSync(data);
          }
        }
      }

      // 3. Force App Restart Notification
      if (context.mounted) {
        showDialog(
          context: context,
          barrierDismissible: false,
          builder: (context) => AlertDialog(
            title: const Text("Restore Successful!"),
            content: const Text(
              "Your data has been restored. Please completely close and restart Timety to load your backups.",
            ),
            actions: [
              ElevatedButton(
                onPressed: () => Navigator.of(context).pop(),
                child: const Text("Got it"),
              ),
            ],
          ),
        );
      }
    } catch (e) {
      if (context.mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(content: Text('Import failed: Check file format')),
        );
      }
    }
  }
}
