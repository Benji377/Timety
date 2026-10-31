package io.github.benji377.timety.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.benji377.timety.data.model.tag.TagEntity
import io.github.benji377.timety.data.model.tag.TaggedItemType
import io.github.benji377.timety.data.repository.TagRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID

/** Exposes the tag list and each item type's tags, and owns creating, editing, and deleting tags. */
class TagViewModel(
    private val tagRepository: TagRepository
) : ViewModel() {
    val allTags: StateFlow<List<TagEntity>> = tagRepository.allTags
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    private fun tagsOf(type: TaggedItemType): StateFlow<Map<String, List<TagEntity>>> =
        tagRepository.tagsByItem(type)
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = emptyMap()
            )

    val taskTags = tagsOf(TaggedItemType.TASK)
    val recurringTaskTags = tagsOf(TaggedItemType.RECURRING_TASK)
    val habitTags = tagsOf(TaggedItemType.HABIT)
    val goalTags = tagsOf(TaggedItemType.GOAL)
    val sessionTags = tagsOf(TaggedItemType.SESSION)

    /** How many items of any type use each tag, by tag id. Unused tags have no entry. */
    val usageCounts: StateFlow<Map<String, Int>> =
        combine(TaggedItemType.entries.map { tagRepository.tagsByItem(it) }) { byType ->
            byType.flatMap { it.values }.flatten().groupingBy { it.id }.eachCount()
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyMap()
        )

    /** Creates a tag and hands it to [onCreated]; a tag with the same name is reused instead. */
    fun createTag(name: String, colorValue: Int, onCreated: (TagEntity) -> Unit = {}) {
        viewModelScope.launch {
            val tag = TagEntity(
                id = UUID.randomUUID().toString(),
                name = name.trim(),
                colorValue = colorValue
            )
            onCreated(tagRepository.getOrCreateTag(tag))
        }
    }

    fun updateTag(tag: TagEntity) {
        viewModelScope.launch {
            tagRepository.updateTag(tag.copy(name = tag.name.trim()))
        }
    }

    fun deleteTag(tag: TagEntity) {
        viewModelScope.launch {
            tagRepository.deleteTag(tag)
        }
    }
}
