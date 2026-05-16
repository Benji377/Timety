import 'dart:convert';
import 'dart:io';
import 'package:flutter/material.dart';
import 'package:path_provider/path_provider.dart';
import 'package:share_plus/share_plus.dart';
import 'package:file_picker/file_picker.dart';
import 'package:shared_preferences/shared_preferences.dart';
import 'package:archive/archive.dart';
import 'package:archive/archive_io.dart';
import 'package:hive/hive.dart';
import '../widgets/dialogs.dart';

class BackupService {
  // --- EXPORT BACKUP ---
  static Future<void> exportBackup(
    BuildContext context, {
    required List<String> boxNames,
  }) async {
    try {
      final tempDir = await getTemporaryDirectory();
      final backupDirPath = '${tempDir.path}/timety_backup';
      final backupDir = Directory(backupDirPath);

      if (backupDir.existsSync()) backupDir.deleteSync(recursive: true);
      backupDir.createSync();

      // 1. Export SharedPreferences
      final prefs = await SharedPreferences.getInstance();
      final prefsMap = <String, dynamic>{};
      for (var key in prefs.getKeys()) {
        prefsMap[key] = prefs.get(key);
      }
      final prefsFile = File('$backupDirPath/prefs.json');
      await prefsFile.writeAsString(jsonEncode(prefsMap));

      // 2. Export Hive box data by copying their database files
      final appDir = await getApplicationDocumentsDirectory();
      final appSupportDir = await getApplicationSupportDirectory();

      // Hive boxes can be stored in different locations - try both
      final possibleHiveDirs = [
        appDir,
        appSupportDir,
        Directory('${appDir.path}/hive'),
        Directory('${appSupportDir.path}/hive'),
      ];

      for (String boxName in boxNames) {
        boxName = boxName.toLowerCase();
        try {
          bool found = false;
          // Look for the hive file in various locations
          for (var dir in possibleHiveDirs) {
            if (!dir.existsSync()) continue;

            final hiveFile = File('${dir.path}/$boxName.hive');
            if (hiveFile.existsSync()) {
              final backupFile = File('$backupDirPath/$boxName.hive');
              hiveFile.copySync(backupFile.path);
              found = true;
              break;
            }
          }

          if (!found) {
            debugPrint('Warning: Could not find file for $boxName');
          }
        } catch (e) {
          debugPrint('Error exporting $boxName: $e');
        }
      }

      // 3. Zip the backup folder
      final zipFileName =
          'Timety_Backup_${DateTime.now().millisecondsSinceEpoch}.zip';
      final zipPath = '${tempDir.path}/$zipFileName';

      // Use Archive API for zipping
      final archive = Archive();
      final backupFiles = backupDir.listSync();

      for (var file in backupFiles) {
        if (file is File) {
          final filename = file.path.split('/').last;
          final bytes = file.readAsBytesSync();
          archive.addFile(ArchiveFile(filename, bytes.length, bytes));
        }
      }

      // Encode and write the archive
      final zipBytes = ZipEncoder().encode(archive);
      await File(zipPath).writeAsBytes(zipBytes);

      if (!context.mounted) return;

      // 4. Ask the user: save locally or share to cloud?
      await _showExportOptions(context, zipPath, zipFileName);
    } catch (e) {
      debugPrint('Error exporting backup: $e');
      if (context.mounted) {
        ScaffoldMessenger.of(
          context,
        ).showSnackBar(SnackBar(content: Text('Export failed: $e')));
      }
    }
  }

  /// Shows a bottom sheet letting the user choose between
  /// saving to a local folder (SAF) or sharing via another app.
  static Future<void> _showExportOptions(
    BuildContext context,
    String zipPath,
    String zipFileName,
  ) async {
    await showModalBottomSheet(
      context: context,
      builder: (ctx) => SafeArea(
        child: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            const ListTile(
              title: Text(
                'Save Backup',
                style: TextStyle(fontWeight: FontWeight.bold),
              ),
            ),
            ListTile(
              leading: const Icon(Icons.folder_outlined),
              title: const Text('Save to Device'),
              subtitle: const Text('Choose a local folder'),
              onTap: () async {
                Navigator.of(ctx).pop();
                await _saveToDevice(context, zipPath, zipFileName);
              },
            ),
            ListTile(
              leading: const Icon(Icons.share_outlined),
              title: const Text('Share / Upload to Cloud'),
              subtitle: const Text('Send to Drive, Dropbox, WhatsApp…'),
              onTap: () async {
                Navigator.of(ctx).pop();
                await _shareFile(context, zipPath);
              },
            ),
            const SizedBox(height: 8),
          ],
        ),
      ),
    );
  }

  /// SAF "Save As" picker — reliable across all Android versions.
  static Future<void> _saveToDevice(
    BuildContext context,
    String zipPath,
    String zipFileName,
  ) async {
    try {
      // Read the zip file bytes
      final bytes = await File(zipPath).readAsBytes();

      // Opens the Android SAF "Save As" dialog and saves the file
      final outputPath = await FilePicker.saveFile(
        dialogTitle: 'Choose where to save your backup',
        fileName: zipFileName,
        type: FileType.custom,
        allowedExtensions: ['zip'],
        bytes: bytes,
      );

      if (outputPath == null) return; // User cancelled

      if (context.mounted) {
        ScaffoldMessenger.of(
          context,
        ).showSnackBar(SnackBar(content: Text('Backup saved to: $outputPath')));
      }

      // Clean up temp file
      File(zipPath).delete().ignore();
    } catch (e) {
      debugPrint('Error saving backup: $e');
      if (context.mounted) {
        ScaffoldMessenger.of(
          context,
        ).showSnackBar(SnackBar(content: Text('Save failed: $e')));
      }
      // Clean up temp file on error
      File(zipPath).delete().ignore();
    }
  }

  /// SharePlus for cloud/app sharing (Drive, Dropbox, etc.)
  static Future<void> _shareFile(BuildContext context, String zipPath) async {
    try {
      final xFile = XFile(zipPath, mimeType: 'application/zip');
      await SharePlus.instance.share(
        ShareParams(subject: 'Timety Backup', files: [xFile]),
      );

      // Clean up temp file after sharing
      await Future.delayed(const Duration(seconds: 1));
      File(zipPath).delete().ignore();
    } catch (e) {
      if (context.mounted) {
        ScaffoldMessenger.of(
          context,
        ).showSnackBar(SnackBar(content: Text('Share failed: $e')));
      }
      // Clean up temp file on error
      File(zipPath).delete().ignore();
    }
  }

  // --- IMPORT BACKUP ---
  static Future<void> importBackup(
    BuildContext context, {
    required List<String> boxNames,
  }) async {
    try {
      final FilePickerResult? result = await FilePicker.pickFiles(
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
      await getApplicationDocumentsDirectory();

      final bytes = zipFile.readAsBytesSync();
      final archive = ZipDecoder().decodeBytes(bytes);

      await Hive.close();

      for (final file in archive) {
        final filename = file.name;
        if (file.isFile) {
          final data = file.content as List<int>;

          if (filename == 'prefs.json') {
            final jsonStr = utf8.decode(data);
            final Map<String, dynamic> prefsMap = jsonDecode(jsonStr);
            final prefs = await SharedPreferences.getInstance();
            await prefs.clear();

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
            // Restore Hive database files
            final appDir = await getApplicationDocumentsDirectory();
            final outFile = File('${appDir.path}/$filename');
            outFile.writeAsBytesSync(data);
          }
        }
      }

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
      debugPrint('Error importing backup: $e');
      if (context.mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          const SnackBar(content: Text('Import failed: Check file format')),
        );
      }
    }
  }
}
