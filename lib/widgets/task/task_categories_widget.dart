import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import '../../providers/task_provider.dart';
import '../../l10n/app_localizations.dart';
import '../common/app_dialogs.dart';

class CategoriesWidget extends StatefulWidget {
  const CategoriesWidget({super.key});

  @override
  State<CategoriesWidget> createState() => _CategoriesWidgetState();
}

class _CategoriesWidgetState extends State<CategoriesWidget> {
  Future<void> _showEditCategoryDialog(String oldName) async {
    final taskProvider = context.read<TaskProvider>();
    final newName = await AppDialogs.showTextInputDialog(
      context: context,
      title: AppLocalizations.of(context)!.categoryEditTitle,
      labelText: AppLocalizations.of(context)!.categoryNameLabel,
      initialValue: oldName,
    );

    if (newName != null && newName != oldName) {
      taskProvider.renameCategory(oldName, newName);
    }
  }

  @override
  Widget build(BuildContext context) {
    final taskProvider = context.watch<TaskProvider>();
    final categories = taskProvider.getAllCategories();
    final l10n = AppLocalizations.of(context)!;

    return Scaffold(
      appBar: AppBar(title: Text(l10n.categoryTitle), centerTitle: true),
      body: ListView(
        padding: const EdgeInsets.only(bottom: 40),
        children: [
          if (categories.isEmpty)
            Padding(
              padding: const EdgeInsets.all(16),
              child: Text(
                l10n.categoryEmpty,
                style: Theme.of(context).textTheme.bodyLarge,
                textAlign: TextAlign.center,
              ),
            )
          else
            ListView.builder(
              shrinkWrap: true,
              physics: const NeverScrollableScrollPhysics(),
              itemCount: categories.length,
              itemBuilder: (context, index) {
                final category = categories[index];
                final taskCount = taskProvider.tasks
                    .where((t) => t.category == category)
                    .length;

                return Padding(
                  padding: const EdgeInsets.symmetric(
                    horizontal: 8,
                    vertical: 4,
                  ),
                  child: Card(
                    child: ListTile(
                      leading: const Icon(Icons.label),
                      title: Text(category),
                      subtitle: Text(l10n.categoryTaskCount(taskCount)),
                      trailing: SizedBox(
                        width: 100,
                        child: Row(
                          mainAxisAlignment: MainAxisAlignment.end,
                          children: [
                            IconButton(
                              icon: const Icon(Icons.edit),
                              onPressed: () =>
                                  _showEditCategoryDialog(category),
                            ),
                            IconButton(
                              icon: const Icon(Icons.delete, color: Colors.red),
                              onPressed: () async {
                                final confirmed =
                                    await AppDialogs.showConfirmation(
                                      context: context,
                                      title: l10n.categoryDeleteTitle,
                                      content: l10n.categoryDeleteContent(
                                        category,
                                      ),
                                      confirmLabel: l10n.commonLabelDelete,
                                      confirmColor: Colors.red,
                                    ) ==
                                    true;

                                if (confirmed) {
                                  taskProvider.deleteCategory(category);
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
}
