package io.github.benji377.timety.util.task

import io.github.benji377.timety.data.model.task.TaskEntity
import io.github.benji377.timety.data.model.task.TaskSortOption


/**
 * Immutable filter/sort configuration for a task list, applied via [process]. Works over any item
 * type `T` by taking a [selector] to the underlying [TaskEntity], so it can filter/sort either raw
 * tasks or wrapper types that carry a task alongside other data.
 */
data class TaskFilterEngine(
    val searchQuery: String = "",
    val categoryFilter: String? = null,
    val sortOption: TaskSortOption = TaskSortOption.DUE_DATE,
    val isAscending: Boolean = true,
) {


    /** Applies the category/search filters, then the configured sort, to [items]. */
    fun <T> process(items: List<T>, selector: (T) -> TaskEntity): List<T> {
        val filtered = applyFilters(items, selector)
        return applySorting(filtered, selector)
    }


    private fun <T> applyFilters(items: List<T>, selector: (T) -> TaskEntity): List<T> {
        return items.filter { item ->
            val task = selector(item)

            if (!categoryFilter.isNullOrEmpty() && task.category != categoryFilter) {
                return@filter false
            }

            if (searchQuery.isEmpty()) return@filter true
            val q = searchQuery.lowercase()
            task.title.lowercase().contains(q) || task.description.lowercase().contains(q)
        }
    }


    private fun <T> applySorting(items: List<T>, selector: (T) -> TaskEntity): List<T> {
        val comparator = Comparator<T> { a, b ->
            val result = compareByOption(selector(a), selector(b))
            if (isAscending) result else -result
        }
        return items.sortedWith(comparator)
    }


    private fun compareByOption(a: TaskEntity, b: TaskEntity): Int = when (sortOption) {
        TaskSortOption.ALPHABETICAL -> a.title.lowercase().compareTo(b.title.lowercase())
        TaskSortOption.CATEGORY -> a.category.lowercase().compareTo(b.category.lowercase())
        TaskSortOption.PRIORITY -> a.priority.ordinal.compareTo(b.priority.ordinal)
        TaskSortOption.SIZE -> a.size.ordinal.compareTo(b.size.ordinal)
        TaskSortOption.DUE_DATE -> compareDueDates(a, b)
    }


    /** Tasks without a due date sort after tasks with one. */
    private fun compareDueDates(a: TaskEntity, b: TaskEntity): Int {
        if (a.dueDate == null && b.dueDate == null) return 0
        if (a.dueDate == null) return 1
        if (b.dueDate == null) return -1
        return a.dueDate.compareTo(b.dueDate)
    }
}
