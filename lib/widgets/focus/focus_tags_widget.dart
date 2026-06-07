import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import 'package:flutter_colorpicker/flutter_colorpicker.dart';
import '../../theme/app_theme.dart';
import '../../providers/focus_provider.dart';
import '../../data/focus/focus_models.dart';
import '../../../l10n/app_localizations.dart';
import '../common/app_dialogs.dart';

class TagsWidget extends StatefulWidget {
  const TagsWidget({super.key});

  @override
  State<TagsWidget> createState() => _TagsWidgetState();
}

class _TagsWidgetState extends State<TagsWidget> {
  @override
  Widget build(BuildContext context) {
    final focusProvider = context.watch<FocusProvider>();

    return Scaffold(
      appBar: AppBar(
        title: Text(AppLocalizations.of(context)!.focusTagsTitle),
        centerTitle: true,
      ),
      body: ListView(
        padding: const EdgeInsets.only(bottom: 40),
        children: [
          Padding(
            padding: const EdgeInsets.all(16),
            child: ElevatedButton.icon(
              onPressed: () => _showTagDialog(),
              icon: const Icon(Icons.add),
              label: Text(AppLocalizations.of(context)!.focusTagsLabelAdd),
              style: ElevatedButton.styleFrom(
                padding: const EdgeInsets.symmetric(vertical: 12),
                backgroundColor: AppTheme.focusColor,
              ),
            ),
          ),
          if (focusProvider.tags.isEmpty)
            Padding(
              padding: const EdgeInsets.all(16),
              child: Text(
                AppLocalizations.of(context)!.focusTagsLabelEmpty,
                style: Theme.of(context).textTheme.bodyLarge,
                textAlign: TextAlign.center,
              ),
            )
          else
            ListView.builder(
              shrinkWrap: true,
              physics: const NeverScrollableScrollPhysics(),
              itemCount: focusProvider.tags.length,
              itemBuilder: (context, index) {
                final tag = focusProvider.tags[index];
                return Padding(
                  padding: const EdgeInsets.symmetric(
                    horizontal: 8,
                    vertical: 4,
                  ),
                  child: Card(
                    child: ListTile(
                      leading: Container(
                        width: 40,
                        height: 40,
                        decoration: BoxDecoration(
                          color: Color(tag.colorValue),
                          borderRadius: BorderRadius.circular(8),
                        ),
                      ),
                      title: Text(tag.name),
                      trailing: SizedBox(
                        width: 100,
                        child: Row(
                          mainAxisAlignment: MainAxisAlignment.end,
                          children: [
                            IconButton(
                              icon: const Icon(Icons.edit),
                              onPressed: () => _showTagDialog(tag: tag),
                            ),
                            if (!tag.id.startsWith('default_tag'))
                              IconButton(
                                icon: const Icon(
                                  Icons.delete,
                                  color: Colors.red,
                                ),
                                onPressed: () async {
                                  final confirmed =
                                      await AppDialogs.showConfirmation(
                                        context: context,
                                        title: AppLocalizations.of(
                                          context,
                                        )!.focusTagsDialogTitleDelete,
                                        content: AppLocalizations.of(context)!
                                            .focusTagsDialogContentDelete(
                                              tag.name,
                                            ),
                                        confirmLabel: AppLocalizations.of(
                                          context,
                                        )!.commonLabelDelete,
                                        confirmColor: Colors.red,
                                      ) ==
                                      true;

                                  if (confirmed) {
                                    focusProvider.deleteTag(tag.id);
                                  }
                                },
                              ),
                          ],
                        ),
                      ),
                    ),
                  ),
                );
              },
            ),
        ],
      ),
    );
  }

  Future<void> _showTagDialog({FocusTag? tag}) async {
    final isEditing = tag != null;
    final currentTag = tag;
    final controller = TextEditingController(text: tag?.name ?? '');
    Color selectedColor = tag == null
        ? AppTheme.focusColor
        : Color(tag.colorValue);

    await showDialog<void>(
      context: context,
      builder: (context) => StatefulBuilder(
        builder: (context, setDialogState) {
          return AlertDialog(
            title: Text(
              isEditing
                  ? AppLocalizations.of(context)!.focusTagsLabelEdit
                  : AppLocalizations.of(context)!.focusTagsLabelAdd,
            ),
            content: Column(
              mainAxisSize: MainAxisSize.min,
              children: [
                TextField(
                  controller: controller,
                  decoration: InputDecoration(
                    labelText: AppLocalizations.of(context)!.focusTagsLabelName,
                    border: const OutlineInputBorder(),
                  ),
                ),
                const SizedBox(height: 16),
                Text(AppLocalizations.of(context)!.focusTagsLabelColor),
                const SizedBox(height: 8),
                BlockPicker(
                  pickerColor: selectedColor,
                  onColorChanged: (color) =>
                      setDialogState(() => selectedColor = color),
                ),
              ],
            ),
            actions: [
              TextButton(
                onPressed: () => Navigator.pop(context),
                child: Text(AppLocalizations.of(context)!.commonLabelCancel),
              ),
              ElevatedButton(
                onPressed: () {
                  final tagName = controller.text.trim();
                  if (tagName.isEmpty) return;

                  final focusProvider = context.read<FocusProvider>();
                  if (isEditing && currentTag != null) {
                    focusProvider.updateTag(
                      currentTag.id,
                      tagName,
                      selectedColor,
                    );
                  } else {
                    focusProvider.createTag(tagName, selectedColor);
                  }
                  Navigator.pop(context);
                },
                child: Text(
                  isEditing
                      ? AppLocalizations.of(context)!.commonLabelSave
                      : AppLocalizations.of(context)!.focusTagsLabelAdd,
                ),
              ),
            ],
          );
        },
      ),
    );

    controller.dispose();
  }
}
