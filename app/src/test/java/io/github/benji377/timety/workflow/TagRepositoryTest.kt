package io.github.benji377.timety.workflow

import android.content.Context
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import io.github.benji377.timety.data.model.tag.TagEntity
import io.github.benji377.timety.data.model.tag.TaggedItemType
import io.github.benji377.timety.data.model.task.TaskEntity
import io.github.benji377.timety.testutil.TestAppContainer
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.time.Instant

/** Tags stay unique by name, and attachments follow the tag and the item they connect. */
@RunWith(AndroidJUnit4::class)
class TagRepositoryTest {
    private val context: Context = InstrumentationRegistry.getInstrumentation().targetContext
    private lateinit var container: TestAppContainer
    private val tags get() = container.tagRepository

    private val work = TagEntity("work", "Work", 1)
    private val meeting = TagEntity("meeting", "Meeting", 2)
    private val task = TaskEntity(id = "t1", title = "Standup", createdAt = Instant.EPOCH)

    @Before
    fun setUp() {
        container = TestAppContainer(context)
        runBlocking {
            tags.getOrCreateTag(work)
            tags.getOrCreateTag(meeting)
            container.taskRepository.insertTask(task)
        }
    }

    @After
    fun tearDown() = container.close()

    private suspend fun taskTagIds() =
        tags.tagsByItem(TaggedItemType.TASK).first()[task.id].orEmpty().map { it.id }.toSet()

    @Test
    fun creatingATagWithATakenName_returnsTheExistingOne() = runBlocking {
        val created = tags.getOrCreateTag(TagEntity("other", "WORK", 9))

        assertEquals(work, created)
        assertEquals(2, tags.allTags.first().size)
    }

    @Test
    fun settingTags_replacesTheOldOnes() = runBlocking {
        tags.setItemTags(TaggedItemType.TASK, task.id, setOf(work.id))
        tags.setItemTags(TaggedItemType.TASK, task.id, setOf(meeting.id))

        assertEquals(setOf(meeting.id), taskTagIds())
    }

    @Test
    fun settingATagDeletedInTheMeantime_skipsIt() = runBlocking {
        tags.deleteTag(work)

        tags.setItemTags(TaggedItemType.TASK, task.id, setOf(work.id, meeting.id))

        assertEquals(setOf(meeting.id), taskTagIds())
    }

    @Test
    fun editingATag_keepsItsAttachments() = runBlocking {
        tags.setItemTags(TaggedItemType.TASK, task.id, setOf(work.id))

        tags.updateTag(work.copy(name = "Job", colorValue = 3))

        assertEquals(setOf(work.id), taskTagIds())
        assertEquals("Job", tags.tagsByItem(TaggedItemType.TASK).first()[task.id]!!.single().name)
    }

    @Test
    fun deletingATag_removesItFromItsItems() = runBlocking {
        tags.setItemTags(TaggedItemType.TASK, task.id, setOf(work.id, meeting.id))

        tags.deleteTag(work)

        assertEquals(setOf(meeting.id), taskTagIds())
    }

    @Test
    fun deletingAnItem_removesItsAttachmentsButNotTheTags() = runBlocking {
        tags.setItemTags(TaggedItemType.TASK, task.id, setOf(work.id))

        container.taskRepository.deleteTask(task)

        assertEquals(emptyMap<String, List<TagEntity>>(), tags.tagsByItem(TaggedItemType.TASK).first())
        assertEquals(2, tags.allTags.first().size)
    }
}
