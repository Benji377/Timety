import '../data/task/task.dart';

/// Utility for filtering and sorting tasks
/// This separates business logic from UI layer concerns
class TaskFilterEngine {
  final String searchQuery;
  final String? categoryFilter;
  final TaskSortOption sortOption;
  final bool isAscending;

  const TaskFilterEngine({
    this.searchQuery = '',
    this.categoryFilter,
    this.sortOption = TaskSortOption.dueDate,
    this.isAscending = true,
  });

  /// Filters and sorts tasks based on configured criteria
  ///
  /// Applies filters in this order:
  /// 1. Category filter (if specified)
  /// 2. Text search query
  /// 3. Sorts by the configured option
  List<Task> process(List<Task> tasks) {
    // Apply filters
    final filtered = _applyFilters(tasks);

    // Apply sorting
    return _applySorting(filtered);
  }

  /// Filters tasks by category and search query
  List<Task> _applyFilters(List<Task> tasks) {
    return tasks.where((task) {
      // Category filter
      if (categoryFilter != null && categoryFilter!.isNotEmpty) {
        if (task.category != categoryFilter) return false;
      }

      // Text search
      if (searchQuery.isEmpty) return true;
      final q = searchQuery.toLowerCase();
      return task.title.toLowerCase().contains(q) ||
          task.description.toLowerCase().contains(q);
    }).toList();
  }

  /// Sorts tasks according to the configured sort option
  List<Task> _applySorting(List<Task> tasks) {
    tasks.sort((a, b) {
      final result = _compareByOption(a, b);
      return isAscending ? result : -result;
    });
    return tasks;
  }

  /// Compares two tasks based on the current sort option
  int _compareByOption(Task a, Task b) {
    return switch (sortOption) {
      TaskSortOption.alphabetical => a.title.toLowerCase().compareTo(
        b.title.toLowerCase(),
      ),
      TaskSortOption.category => a.category.toLowerCase().compareTo(
        b.category.toLowerCase(),
      ),
      TaskSortOption.priority => a.priority.index.compareTo(b.priority.index),
      TaskSortOption.size => a.size.index.compareTo(b.size.index),
      TaskSortOption.dueDate => _compareDueDates(a, b),
    };
  }

  /// Compares due dates with null handling
  /// Tasks without due dates sort to the bottom
  int _compareDueDates(Task a, Task b) {
    if (a.dueDate == null && b.dueDate == null) return 0;
    if (a.dueDate == null) return 1;
    if (b.dueDate == null) return -1;
    return a.dueDate!.compareTo(b.dueDate!);
  }

  /// Creates a copy with modified values
  TaskFilterEngine copyWith({
    String? searchQuery,
    String? categoryFilter,
    TaskSortOption? sortOption,
    bool? isAscending,
  }) {
    return TaskFilterEngine(
      searchQuery: searchQuery ?? this.searchQuery,
      categoryFilter: categoryFilter ?? this.categoryFilter,
      sortOption: sortOption ?? this.sortOption,
      isAscending: isAscending ?? this.isAscending,
    );
  }
}
