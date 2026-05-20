import 'package:flutter/material.dart';
import 'package:url_launcher/url_launcher.dart';
import '../providers/settings_provider.dart';
import '../theme/app_theme.dart';

void showLocationApiDialog({
  required BuildContext context,
  required bool Function() isStateMounted,
  required SettingsProvider settings,
}) {
  final TextEditingController controller = TextEditingController(
    text: settings.locationApiEndpoint,
  );
  bool isTestingConnection = false;
  bool hasValidatedConnection = false;

  showDialog(
    context: context,
    builder: (dialogContext) => StatefulBuilder(
      builder: (dialogContext, setDialogState) {
        return AlertDialog(
          title: const Text('Location Search API'),
          content: SingleChildScrollView(
            child: Column(
              mainAxisSize: MainAxisSize.min,
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                TextField(
                  controller: controller,
                  decoration: const InputDecoration(
                    hintText: 'https://photon.komoot.io/api/',
                    labelText: 'API Endpoint URL',
                    border: OutlineInputBorder(),
                  ),
                  onChanged: (_) {
                    setDialogState(() {
                      hasValidatedConnection = false;
                    });
                  },
                ),
                const SizedBox(height: 16),
                // Information section with bullet points
                Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    // Supported APIs
                    _buildInfoItem(
                      context: dialogContext,
                      icon: Icons.check_circle,
                      title: 'Supported APIs',
                      subtitle: 'Photon-compatible endpoints',
                    ),
                    // Learn more link
                    Padding(
                      padding: const EdgeInsets.only(
                        left: 32,
                        top: 8,
                        bottom: 12,
                      ),
                      child: GestureDetector(
                        onTap: () => launchUrl(
                          Uri.parse('https://github.com/komoot/photon'),
                          mode: LaunchMode.externalApplication,
                        ),
                        child: const Row(
                          children: [
                            Icon(
                              Icons.link,
                              size: 16,
                              color: AppTheme.taskColor,
                            ),
                            SizedBox(width: 6),
                            Expanded(
                              child: Text(
                                'Learn about Photon on GitHub',
                                style: TextStyle(
                                  fontSize: 12,
                                  color: AppTheme.taskColor,
                                  decoration: TextDecoration.underline,
                                  decorationColor: AppTheme.taskColor,
                                ),
                              ),
                            ),
                          ],
                        ),
                      ),
                    ),
                    // Self-hosting suggestion
                    _buildInfoItem(
                      context: dialogContext,
                      icon: Icons.storage,
                      title: 'Best Practice',
                      subtitle:
                          'Self-host your own Photon instance for privacy',
                    ),
                  ],
                ),
                const SizedBox(height: 16),
                if (hasValidatedConnection)
                  Container(
                    padding: const EdgeInsets.all(12),
                    decoration: BoxDecoration(
                      color: AppTheme.statusCompleted.withValues(alpha: 0.1),
                      border: Border.all(
                        color: AppTheme.statusCompleted,
                        width: 2,
                      ),
                      borderRadius: AppTheme.brMedium,
                    ),
                    child: const Row(
                      children: [
                        Icon(
                          Icons.check_circle,
                          color: AppTheme.statusCompleted,
                          size: 20,
                        ),
                        SizedBox(width: 10),
                        Text(
                          'Connection successful',
                          style: TextStyle(
                            color: AppTheme.statusCompleted,
                            fontSize: 12,
                            fontWeight: FontWeight.w500,
                          ),
                        ),
                      ],
                    ),
                  ),
              ],
            ),
          ),
          actions: [
            if (settings.locationApiEndpoint != 'https://photon.komoot.io/api/')
              TextButton(
                onPressed: () {
                  controller.text = 'https://photon.komoot.io/api/';
                  setDialogState(() {
                    hasValidatedConnection = false;
                  });
                },
                child: const Text('Reset to Default'),
              ),
            TextButton(
              onPressed: isTestingConnection
                  ? null
                  : () async {
                      final url = controller.text.trim();
                      final messenger = ScaffoldMessenger.of(context);
                      setDialogState(() {
                        isTestingConnection = true;
                      });

                      final isValid = await settings
                          .validateLocationApiEndpoint(url);

                      if (isStateMounted()) {
                        setDialogState(() {
                          isTestingConnection = false;
                          hasValidatedConnection = isValid;
                        });

                        if (!isValid) {
                          messenger.showSnackBar(
                            const SnackBar(
                              content: Row(
                                children: [
                                  Icon(
                                    Icons.error,
                                    color: Colors.white,
                                    size: 20,
                                  ),
                                  SizedBox(width: 12),
                                  Expanded(
                                    child: Text(
                                      'Cannot reach endpoint. Check the URL and try again.',
                                      style: TextStyle(
                                        color: Colors.white,
                                        fontSize: 12,
                                      ),
                                    ),
                                  ),
                                ],
                              ),
                              backgroundColor: AppTheme.statusOverdue,
                              behavior: SnackBarBehavior.floating,
                            ),
                          );
                        }
                      }
                    },
              child: isTestingConnection
                  ? const SizedBox(
                      width: 20,
                      height: 20,
                      child: CircularProgressIndicator(strokeWidth: 2),
                    )
                  : const Text('Test Connection'),
            ),
            TextButton(
              onPressed: () => Navigator.pop(dialogContext),
              child: const Text('Cancel'),
            ),
            ElevatedButton(
              onPressed:
                  hasValidatedConnection || controller.text.trim().isEmpty
                  ? () {
                      settings.setLocationApiEndpoint(controller.text.trim());
                      Navigator.pop(dialogContext);
                    }
                  : null,
              child: const Text('Save'),
            ),
          ],
        );
      },
    ),
  );
}

/// Helper widget to build info items with icon and text
Widget _buildInfoItem({
  required BuildContext context,
  required IconData icon,
  required String title,
  required String subtitle,
}) {
  final theme = Theme.of(context);

  return Padding(
    padding: const EdgeInsets.only(bottom: 12),
    child: Row(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Icon(icon, size: 20, color: AppTheme.taskColor),
        const SizedBox(width: 12),
        Expanded(
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            mainAxisSize: MainAxisSize.min,
            children: [
              Text(
                title,
                style: const TextStyle(
                  fontSize: 12,
                  fontWeight: AppTheme.fwMedium,
                  color: AppTheme.taskColor,
                ),
              ),
              const SizedBox(height: 4),
              Text(
                subtitle,
                style: TextStyle(
                  fontSize: 12,
                  color:
                      theme.textTheme.bodySmall?.color ??
                      theme.colorScheme.onSurface,
                  height: 1.4,
                ),
              ),
            ],
          ),
        ),
      ],
    ),
  );
}
