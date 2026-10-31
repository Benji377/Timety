package io.github.benji377.timety.data.model.tag

import io.github.benji377.timety.data.model.focus.FocusTargetType
import java.util.UUID

// Id and name of the placeholder focus tag older versions seeded when no tag existed.
private const val LEGACY_DEFAULT_TAG_ID = "default_tag"
private const val LEGACY_DEFAULT_TAG_NAME = "None"

/** A task category or focus tag from before global tags. */
data class LegacyTag(
    val id: String,
    val name: String,
    val colorValue: Int
)

/** The target fields of a focus session from before global tags. */
data class LegacySession(
    val targetType: FocusTargetType,
    val targetId: String?,
    val tagId: String?,
    val targetLabel: String?
) {
    /**
     * The label of a tag session whose tag no longer exists, which keeps that tag's name alive.
     * Null for every other session.
     */
    fun deletedTagLabel(focusTags: List<LegacyTag>): String? {
        if (targetType != FocusTargetType.TAG) return null
        val id = targetId ?: tagId
        if (id == LEGACY_DEFAULT_TAG_ID || focusTags.any { it.id == id && it.name.isNotBlank() }) return null
        return targetLabel?.takeIf { it.isNotBlank() }
    }
}

/** Global tags merged from the old categories and focus tags, with lookups for the old references. */
class MergedTags(
    val tags: List<TagEntity>,
    private val focusTagIds: Map<String, String>
) {
    /** The tag an old task category name became, or null for an empty name. */
    fun idForName(name: String): String? =
        tags.firstOrNull { TagEntity.isSameName(it.name, name) }?.id

    /**
     * The tag an old session should carry: its own tag, or the category its task or recurring task
     * has now, read through [categoryOf]. Habits and goals had no labels, so their sessions get none.
     */
    fun idForSession(session: LegacySession, categoryOf: (FocusTargetType, String) -> String?): String? {
        val id = session.targetId ?: session.tagId
        return when (session.targetType) {
            FocusTargetType.TAG ->
                id?.let { focusTagIds[it] }
                    ?: session.targetLabel?.takeIf { id != LEGACY_DEFAULT_TAG_ID }?.let(::idForName)

            FocusTargetType.TASK, FocusTargetType.RECURRING_TASK ->
                id?.let { categoryOf(session.targetType, it) }?.let(::idForName)

            FocusTargetType.HABIT, FocusTargetType.GOAL -> null
        }
    }
}

/**
 * Merges [categories] and [focusTags] into one list, shared by the database migration and the
 * restore of old backups. Names that read the same become one tag, keeping the category's color.
 * The seeded "None" tag only stays if the user renamed it. [usedNames] are names referenced without
 * a matching row (category names on tasks, labels of deleted tags) and get the default color.
 */
fun mergeLegacyTags(
    categories: List<LegacyTag>,
    focusTags: List<LegacyTag>,
    usedNames: Collection<String>,
    newId: () -> String = { UUID.randomUUID().toString() },
): MergedTags {
    val tags = mutableListOf<TagEntity>()
    fun find(name: String) = tags.firstOrNull { TagEntity.isSameName(it.name, name) }

    categories.filter { it.name.isNotBlank() }.forEach { category ->
        if (find(category.name) == null) {
            tags += TagEntity(category.id, category.name.trim(), category.colorValue)
        }
    }

    val focusTagIds = mutableMapOf<String, String>()
    focusTags.filter { it.name.isNotBlank() }.forEach { tag ->
        val isUnrenamedDefault = tag.id == LEGACY_DEFAULT_TAG_ID && tag.name == LEGACY_DEFAULT_TAG_NAME
        if (!isUnrenamedDefault) {
            val merged = find(tag.name) ?: TagEntity(tag.id, tag.name.trim(), tag.colorValue).also { tags += it }
            focusTagIds[tag.id] = merged.id
        }
    }

    usedNames.filter { it.isNotBlank() }.forEach { name ->
        if (find(name) == null) {
            tags += TagEntity(newId(), name.trim(), TagEntity.DEFAULT_COLOR_VALUE)
        }
    }
    return MergedTags(tags, focusTagIds)
}
