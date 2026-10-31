package io.github.benji377.timety.services.backup

import io.github.benji377.timety.data.model.focus.FocusTargetType
import io.github.benji377.timety.data.model.tag.LegacySession
import io.github.benji377.timety.data.model.tag.LegacyTag
import io.github.benji377.timety.data.model.tag.MergedTags
import io.github.benji377.timety.data.model.tag.TagEntity
import io.github.benji377.timety.data.model.tag.TaggedItemType
import io.github.benji377.timety.data.model.tag.mergeLegacyTags
import org.json.JSONObject

// Backup JSON for tags. Items list their tags in a "tagIds" array; older backups instead have
// task categories and focus tags, which are merged the same way the database migration does it.

internal fun TagEntity.toBackupJson(): JSONObject = JSONObject().apply {
    put("id", id)
    put("name", name)
    put("colorValue", colorValue)
}

/** Null for a tag without a name, which can't be shown or picked. */
private fun tagFromBackup(json: JSONObject): TagEntity? {
    val name = json.stringOrNull("name")?.trim()?.takeIf { it.isNotEmpty() } ?: return null
    return TagEntity(
        id = json.stringOrNull("id") ?: newId(),
        name = name,
        colorValue = json.optInt("colorValue", TagEntity.DEFAULT_COLOR_VALUE),
    )
}

/** The tags to restore, and the tag ids of each item by item type and item id. */
internal class RestoredTags(
    val tags: List<TagEntity>,
    val attachments: Map<TaggedItemType, Map<String, List<String>>>,
)

/**
 * Reads the tags from a backup. [items] holds the JSON of each restored item by its restored id,
 * by item type. Attachments to tags the backup doesn't have are skipped.
 */
internal fun tagsFromBackup(
    json: JSONObject,
    items: Map<TaggedItemType, Map<String, JSONObject>>,
): RestoredTags = if (json.has("tags")) currentTags(json, items) else legacyTags(json, items)

private fun currentTags(
    json: JSONObject,
    items: Map<TaggedItemType, Map<String, JSONObject>>,
): RestoredTags {
    val tags = json.objects("tags").mapNotNull(::tagFromBackup).distinctBy { it.name.lowercase() }
    val knownIds = tags.mapTo(HashSet()) { it.id }
    return RestoredTags(
        tags = tags,
        attachments = items.mapValues { (_, byId) ->
            byId.mapValues { (_, item) -> item.stringList("tagIds").filter { it in knownIds } }
        },
    )
}

private fun legacyTags(
    json: JSONObject,
    items: Map<TaggedItemType, Map<String, JSONObject>>,
): RestoredTags {
    fun categories(type: TaggedItemType) = items[type].orEmpty()
        .mapValues { (_, item) -> item.stringOrNull("category").orEmpty() }
        .filterValues { it.isNotBlank() }

    val taskCategories = categories(TaggedItemType.TASK)
    val recurringCategories = categories(TaggedItemType.RECURRING_TASK)
    val focusTags = (json.optJSONObject("focus") ?: JSONObject()).objects("tags").map(::legacyTagFromBackup)
    val sessions = items[TaggedItemType.SESSION].orEmpty().mapValues { (_, item) -> legacySession(item) }

    val merged: MergedTags = mergeLegacyTags(
        categories = json.objects("taskCategories").map(::legacyTagFromBackup),
        focusTags = focusTags,
        usedNames = taskCategories.values + recurringCategories.values +
                sessions.values.mapNotNull { it.deletedTagLabel(focusTags) },
    )
    fun attachments(categories: Map<String, String>) =
        categories.mapValues { (_, category) -> listOfNotNull(merged.idForName(category)) }

    return RestoredTags(
        tags = merged.tags,
        attachments = mapOf(
            TaggedItemType.TASK to attachments(taskCategories),
            TaggedItemType.RECURRING_TASK to attachments(recurringCategories),
            TaggedItemType.SESSION to sessions.mapValues { (_, session) ->
                listOfNotNull(
                    merged.idForSession(session) { type, id ->
                        if (type == FocusTargetType.TASK) taskCategories[id] else recurringCategories[id]
                    }
                )
            },
        ),
    )
}

private fun legacyTagFromBackup(json: JSONObject) = LegacyTag(
    id = json.stringOrNull("id") ?: newId(),
    name = json.stringOrNull("name") ?: "",
    colorValue = json.optInt("colorValue", TagEntity.DEFAULT_COLOR_VALUE),
)

private fun legacySession(json: JSONObject) = LegacySession(
    targetType = json.enumOr("targetType", FocusTargetType.TAG),
    targetId = json.stringOrNull("targetId"),
    tagId = json.stringOrNull("tagId"),
    targetLabel = json.stringOrNull("targetLabel"),
)
