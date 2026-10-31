package io.github.benji377.timety.data.repository

import io.github.benji377.timety.data.local.dao.TagDao
import io.github.benji377.timety.data.model.tag.TagEntity
import io.github.benji377.timety.data.model.tag.TaggedItemType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

/** Repository for tags and their attachments to items, wrapping [TagDao] with IO dispatching. */
class TagRepository(
    private val tagDao: TagDao
) {
    val allTags: Flow<List<TagEntity>> = tagDao.getAllTags()

    /** The tags of every tagged item of [type], by item id. Items without tags have no entry. */
    fun tagsByItem(type: TaggedItemType): Flow<Map<String, List<TagEntity>>> =
        tagDao.getItemTags(type).map { rows ->
            rows.groupBy({ it.itemId }, { it.tag })
        }

    suspend fun getOrCreateTag(tag: TagEntity): TagEntity = withContext(Dispatchers.IO) {
        tagDao.getOrCreateTag(tag)
    }

    suspend fun updateTag(tag: TagEntity) = withContext(Dispatchers.IO) {
        tagDao.updateTag(tag)
    }

    suspend fun deleteTag(tag: TagEntity) = withContext(Dispatchers.IO) {
        tagDao.deleteTag(tag)
    }

    suspend fun setItemTags(type: TaggedItemType, itemId: String, tagIds: Collection<String>) =
        withContext(Dispatchers.IO) {
            tagDao.setItemTags(type, itemId, tagIds)
        }
}
