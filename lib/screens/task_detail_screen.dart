// lib/screens/task_detail_screen.dart
import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import '../data/task.dart';
import '../providers/task_provider.dart';

class TaskDetailScreen extends StatefulWidget {
  final Task task;
  final bool isEditing;

  const TaskDetailScreen({super.key, required this.task, required this.isEditing});

  @override
  State<TaskDetailScreen> createState() => _TaskDetailScreenState();
}

class _TaskDetailScreenState extends State<TaskDetailScreen> {
  late bool _isEditing;
  
  // Controllers
  late TextEditingController _titleController;
  late TextEditingController _descController;
  late TextEditingController _locationController;
  
  // State variables
  late Priority _priority;
  DateTime? _dueDate;
  late String _category;

  @override
  void initState() {
    super.initState();
    _isEditing = widget.isEditing;
    _titleController = TextEditingController(text: widget.task.title);
    _descController = TextEditingController(text: widget.task.description);
    _locationController = TextEditingController(text: widget.task.location);
    _priority = widget.task.priority;
    _dueDate = widget.task.dueDate;
    _category = widget.task.category;
  }

  @override
  void dispose() {
    _titleController.dispose();
    _descController.dispose();
    _locationController.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: Text(_isEditing ? "Edit Task" : "Task Details"),
        actions: [
          if (!_isEditing)
            IconButton(
              icon: const Icon(Icons.edit),
              onPressed: () => setState(() => _isEditing = true),
            ),
        ],
      ),
      body: ListView(
        padding: const EdgeInsets.all(16.0),
        children: [
          // Category Chip at the top
          if (!_isEditing && _category.isNotEmpty)
            Align(
              alignment: Alignment.centerLeft,
              child: Chip(
                label: Text(_category),
                backgroundColor: Theme.of(context).colorScheme.primaryContainer,
              ),
            ),
          
          const SizedBox(height: 10),

          // Title
          TextField(
            controller: _titleController,
            enabled: _isEditing,
            style: const TextStyle(fontSize: 22, fontWeight: FontWeight.bold),
            decoration: InputDecoration(
              labelText: _isEditing ? 'Title' : null,
              border: _isEditing ? const OutlineInputBorder() : InputBorder.none,
            ),
          ),
          const SizedBox(height: 16),

          // Priority
          if (_isEditing)
            DropdownButtonFormField<Priority>(
              initialValue: _priority,
              decoration: const InputDecoration(labelText: 'Priority', border: OutlineInputBorder()),
              items: Priority.values.map((p) => DropdownMenuItem(value: p, child: Text(p.name.toUpperCase()))).toList(),
              onChanged: (val) => setState(() => _priority = val!),
            )
          else
            ListTile(
              leading: const Icon(Icons.flag),
              title: const Text('Priority'),
              subtitle: Text(_priority.name.toUpperCase()),
              contentPadding: EdgeInsets.zero,
            ),
            
          const Divider(),

          // Description
          TextField(
            controller: _descController,
            enabled: _isEditing,
            maxLines: null,
            decoration: InputDecoration(
              labelText: _isEditing ? 'Description' : 'Description',
              border: _isEditing ? const OutlineInputBorder() : InputBorder.none,
            ),
          ),
          
          const Divider(),

          // Location & Map Placeholder
          TextField(
            controller: _locationController,
            enabled: _isEditing,
            decoration: InputDecoration(
              labelText: 'Location',
              prefixIcon: const Icon(Icons.location_on),
              border: _isEditing ? const OutlineInputBorder() : InputBorder.none,
            ),
          ),
          
          // Google Map Placeholder
          if (!_isEditing && _locationController.text.isNotEmpty)
            Container(
              height: 150,
              width: double.infinity,
              margin: const EdgeInsets.only(top: 10),
              decoration: BoxDecoration(
                color: Colors.grey.shade300,
                borderRadius: BorderRadius.circular(8),
              ),
              child: const Center(
                child: Text('🌍 Google Maps Widget Goes Here\n(Requires API Key)', textAlign: TextAlign.center,),
              ),
            ),

          const SizedBox(height: 24),

          if (_isEditing)
            ElevatedButton.icon(
              icon: const Icon(Icons.save),
              onPressed: () {
                // Show confirmation dialog
                showDialog(
                  context: context,
                  builder: (ctx) => AlertDialog(
                    title: const Text('Confirm Save'),
                    content: const Text('Are you sure you want to save changes to this task?'),
                    actions: [
                      TextButton(
                        onPressed: () => Navigator.of(ctx).pop(),
                        child: const Text('Cancel'),
                      ),
                      ElevatedButton(
                        onPressed: () {
                          // Update the task in the provider
                          final updatedTask = Task(
                            id: widget.task.id,
                            title: _titleController.text,
                            description: _descController.text,
                            location: _locationController.text,
                            priority: _priority,
                            dueDate: _dueDate,
                            category: _category,
                            isCompleted: widget.task.isCompleted,
                          );
                          context.read<TaskProvider>().updateTask(updatedTask);
                          Navigator.of(ctx).pop(); // Close dialog
                          setState(() => _isEditing = false); // Exit edit mode
                        },
                        child: const Text('Save'),
                      ),
                    ],
                  ),
                );
                // Save logic here via Provider
                setState(() => _isEditing = false);
              },
              label: const Text('Save Changes'),
              style: ElevatedButton.styleFrom(padding: const EdgeInsets.all(16)),
            ),
        ],
      ),
    );
  }
}