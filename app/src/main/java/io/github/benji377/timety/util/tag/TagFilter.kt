package io.github.benji377.timety.util.tag

import io.github.benji377.timety.data.model.tag.TagEntity

/** Pure logic behind the tag filter pills: several selected tags narrow the result. */
object TagFilter {
    /** The tags on any of [itemIds], so a pill is only offered when it can match something. */
    fun usedTags(
        itemIds: Iterable<String>,
        tagsByItem: Map<String, List<TagEntity>>,
    ): List<TagEntity> =
        itemIds.flatMap { tagsByItem[it].orEmpty() }
            .distinctBy { it.id }
            .sortedBy { it.name.lowercase() }

    /** [selected] without tags no longer on offer, so a hidden pill can't keep filtering the list. */
    fun activeIds(selected: Set<String>, used: List<TagEntity>): Set<String> =
        used.mapTo(HashSet()) { it.id }.intersect(selected)

    /** Whether [itemTags] include every id in [selected]; nothing selected matches everything. */
    fun matches(itemTags: List<TagEntity>, selected: Set<String>): Boolean =
        selected.all { id -> itemTags.any { it.id == id } }

    /** Keeps the [items] carrying every id in [selected]. */
    fun <T> filter(
        items: List<T>,
        selected: Set<String>,
        tagsByItem: Map<String, List<TagEntity>>,
        idOf: (T) -> String,
    ): List<T> =
        if (selected.isEmpty()) items
        else items.filter { matches(tagsByItem[idOf(it)].orEmpty(), selected) }
}
