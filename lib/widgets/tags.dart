import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import 'package:flutter_colorpicker/flutter_colorpicker.dart';
import '../theme/app_theme.dart';
import '../providers/focus_provider.dart';
import '../data/focus/focus_models.dart';

class TagsWidget extends StatefulWidget {
  const TagsWidget({super.key});

  @override
  State<TagsWidget> createState() => _TagsWidgetState();
}

class _TagsWidgetState extends State<TagsWidget> {
  void _showAddTagDialog() {
    String tagName = '';
    Color selectedColor = AppTheme.focusColor;

    showDialog(
      context: context,
      builder: (context) => StatefulBuilder(
        builder: (context, setDialogState) {
          return AlertDialog(
            title: const Text('Add New Tag'),
            content: Column(
              mainAxisSize: MainAxisSize.min,
              children: [
                TextField(
                  decoration: const InputDecoration(
                    labelText: 'Tag Name',
                    border: OutlineInputBorder(),
                  ),
                  onChanged: (val) => tagName = val,
                ),
                const SizedBox(height: 16),
                const Text('Choose Color:'),
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
                child: const Text("Cancel"),
              ),
              ElevatedButton(
                onPressed: () {
                  if (tagName.isNotEmpty) {
                    context.read<FocusProvider>().createTag(
                      tagName,
                      selectedColor,
                    );
                    Navigator.pop(context);
                  }
                },
                child: const Text("Add"),
              ),
            ],
          );
        },
      ),
    );
  }

  Future<void> _showEditTagDialog(FocusTag tag) async {
    final controller = TextEditingController(text: tag.name);
    Color selectedColor = Color(tag.colorValue);

    await showDialog(
      context: context,
      builder: (context) => StatefulBuilder(
        builder: (context, setDialogState) {
          return AlertDialog(
            title: const Text('Edit Tag'),
            content: Column(
              mainAxisSize: MainAxisSize.min,
              children: [
                TextField(
                  controller: controller,
                  decoration: const InputDecoration(
                    labelText: 'Tag Name',
                    border: OutlineInputBorder(),
                  ),
                ),
                const SizedBox(height: 16),
                const Text('Choose Color:'),
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
                child: const Text("Cancel"),
              ),
              ElevatedButton(
                onPressed: () {
                  final tagName = controller.text.trim();
                  if (tagName.isNotEmpty) {
                    context.read<FocusProvider>().updateTag(
                      tag.id,
                      tagName,
                      selectedColor,
                    );
                    Navigator.pop(context);
                  }
                },
                child: const Text("Save"),
              ),
            ],
          );
        },
      ),
    );
  }

  @override
  Widget build(BuildContext context) {
    final focusProvider = context.watch<FocusProvider>();

    return Scaffold(
      appBar: AppBar(title: const Text('Focus Tags'), centerTitle: true),
      body: ListView(
        padding: const EdgeInsets.only(bottom: 40),
        children: [
          Padding(
            padding: const EdgeInsets.all(16),
            child: ElevatedButton.icon(
              onPressed: _showAddTagDialog,
              icon: const Icon(Icons.add),
              label: const Text('Add New Tag'),
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
                'No tags yet',
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
                              onPressed: () => _showEditTagDialog(tag),
                            ),
                            if (!tag.id.startsWith('default_tag'))
                              IconButton(
                                icon: const Icon(
                                  Icons.delete,
                                  color: Colors.red,
                                ),
                                onPressed: () {
                                  showDialog(
                                    context: context,
                                    builder: (context) => AlertDialog(
                                      title: const Text('Delete Tag?'),
                                      content: Text(
                                        'Are you sure you want to delete "${tag.name}"?',
                                      ),
                                      actions: [
                                        TextButton(
                                          onPressed: () =>
                                              Navigator.pop(context),
                                          child: const Text("Cancel"),
                                        ),
                                        ElevatedButton(
                                          style: ElevatedButton.styleFrom(
                                            backgroundColor: Colors.red,
                                          ),
                                          onPressed: () {
                                            focusProvider.deleteTag(tag.id);
                                            Navigator.pop(context);
                                          },
                                          child: const Text("Delete"),
                                        ),
                                      ],
                                    ),
                                  );
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
}
