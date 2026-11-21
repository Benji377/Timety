package io.github.benji377.timety.workflow

import android.content.Context
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import io.github.benji377.timety.data.model.habit.HabitEntity
import io.github.benji377.timety.data.model.habit.HabitFrequency
import io.github.benji377.timety.data.model.habit.HabitStackEntity
import io.github.benji377.timety.data.repository.StackRenameResult
import io.github.benji377.timety.services.backup.habitStackFromBackup
import io.github.benji377.timety.services.backup.toBackupJson
import io.github.benji377.timety.testutil.TestAppContainer
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.time.Instant

/** A stack's settings row follows its habits, and renaming moves the whole stack. */
@RunWith(AndroidJUnit4::class)
class HabitStackSettingsTest {
    private val context: Context = InstrumentationRegistry.getInstrumentation().targetContext
    private lateinit var container: TestAppContainer
    private val repository get() = container.habitRepository

    @Before
    fun setUp() {
        container = TestAppContainer(context)
    }

    @After
    fun tearDown() {
        container.close()
    }

    private fun habit(id: String, stack: String?) = HabitEntity(
        id = id,
        name = id,
        frequency = HabitFrequency.DAILY,
        createdAt = Instant.EPOCH,
        colorValue = 0,
        stackName = stack,
        stackOrder = stack?.let { 0 },
    )

    private fun stacks() = runBlocking { repository.getStacks() }.sortedBy { it.name }

    @Test
    fun aNewStack_isInOrder_andGoesAwayWithItsLastHabit() = runBlocking {
        repository.insertHabit(habit("a", "Morning"))
        repository.insertHabit(habit("b", "Morning"))
        assertEquals(listOf(HabitStackEntity("Morning", inOrder = true)), stacks())

        repository.updateHabit(habit("a", null))
        assertEquals("one member is left", 1, stacks().size)

        repository.deleteHabit(habit("b", "Morning"))
        assertTrue("the stack has no habits left", stacks().isEmpty())
    }

    @Test
    fun renaming_movesEveryMember_andKeepsTheSetting() = runBlocking {
        repository.insertHabit(habit("a", "Morning"))
        repository.insertHabit(habit("b", "Morning"))

        assertEquals(StackRenameResult.SAVED, repository.saveStack("Morning", " Wake up ", inOrder = false))

        assertEquals(listOf(HabitStackEntity("Wake up", inOrder = false)), stacks())
        assertEquals("Wake up", repository.getHabitById("a")!!.stackName)
        assertEquals("Wake up", repository.getHabitById("b")!!.stackName)
    }

    @Test
    fun renamingToAnotherStacksName_orToNothing_isRefused() = runBlocking {
        repository.insertHabit(habit("a", "Morning"))
        repository.insertHabit(habit("b", "Evening"))

        assertEquals(StackRenameResult.NAME_TAKEN, repository.saveStack("Morning", "Evening", inOrder = true))
        assertEquals(StackRenameResult.NAME_EMPTY, repository.saveStack("Morning", "  ", inOrder = true))

        assertEquals("nothing was merged", "Morning", repository.getHabitById("a")!!.stackName)
        assertEquals(2, stacks().size)
    }

    @Test
    fun keepingTheName_justChangesTheSetting() = runBlocking {
        repository.insertHabit(habit("a", "Morning"))

        assertEquals(StackRenameResult.SAVED, repository.saveStack("Morning", "Morning", inOrder = false))

        assertEquals(listOf(HabitStackEntity("Morning", inOrder = false)), stacks())
    }

    @Test
    fun stackSettings_surviveABackupRoundTrip() {
        val restored = habitStackFromBackup(HabitStackEntity("Morning", inOrder = false).toBackupJson())

        assertEquals(HabitStackEntity("Morning", inOrder = false), restored)
        assertNull("an entry without a name is dropped", habitStackFromBackup(JSONObject("""{"inOrder": false}""")))
        assertEquals(
            "an entry without the flag is in order",
            true,
            habitStackFromBackup(JSONObject("""{"name": "Morning"}"""))!!.inOrder,
        )
    }
}
