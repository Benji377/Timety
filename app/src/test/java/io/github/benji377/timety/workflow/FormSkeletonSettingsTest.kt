package io.github.benji377.timety.workflow

import android.content.Context
import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import io.github.benji377.timety.data.local.TimetyDatabase
import io.github.benji377.timety.data.model.form.FormSkeleton
import io.github.benji377.timety.data.model.form.HabitInput
import io.github.benji377.timety.data.model.form.TaskInput
import io.github.benji377.timety.di.NewInstallCallback
import io.github.benji377.timety.testutil.TestAppContainer
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/** Settings > Forms: switches persist, travel with backups, and only new installs start short. */
@RunWith(AndroidJUnit4::class)
class FormSkeletonSettingsTest {
    private val context: Context = InstrumentationRegistry.getInstrumentation().targetContext
    private lateinit var container: TestAppContainer
    private val settings get() = container.settingsRepository

    @Before
    fun setUp() {
        container = TestAppContainer(context)
    }

    @After
    fun tearDown() {
        container.close()
        context.deleteDatabase(FILE_DB)
    }

    private fun taskOn() = runBlocking { settings.taskSkeletonFlow.first().on }
    private fun habitOn() = runBlocking { settings.habitSkeletonFlow.first().on }

    @Test
    fun switches_persistAndSwitchBackOn() = runBlocking {
        settings.setTaskInputOn(TaskInput.LOCATION, false)
        settings.setTaskInputOn(TaskInput.DESCRIPTION, false)
        settings.setHabitInputOn(HabitInput.ICON, false)
        assertEquals(TaskInput.entries.toSet() - TaskInput.LOCATION - TaskInput.DESCRIPTION, taskOn())
        assertEquals(HabitInput.entries.toSet() - HabitInput.ICON, habitOn())

        settings.setTaskInputOn(TaskInput.LOCATION, true)
        assertEquals(TaskInput.entries.toSet() - TaskInput.DESCRIPTION, taskOn())
    }

    @Test
    fun backup_carriesTheSwitches() = runBlocking {
        settings.setTaskInputOn(TaskInput.CHECKLIST, false)
        settings.setHabitInputOn(HabitInput.NOTES, false)
        val exported = settings.exportAll()

        settings.setTaskInputOn(TaskInput.CHECKLIST, true)
        settings.setHabitInputOn(HabitInput.NOTES, true)
        settings.restoreAll(exported)

        assertEquals(TaskInput.entries.toSet() - TaskInput.CHECKLIST, taskOn())
        assertEquals(HabitInput.entries.toSet() - HabitInput.NOTES, habitOn())
    }

    @Test
    fun oldBackup_withoutSwitches_turnsEverythingOn() = runBlocking {
        settings.applyNewInstallFormPreset()
        settings.restoreAll(mapOf("use24HourFormat" to true))
        assertEquals(TaskInput.entries.toSet(), taskOn())
        assertEquals(HabitInput.entries.toSet(), habitOn())
    }

    @Test
    fun newInstall_startsWithThePreset() {
        val db = Room.inMemoryDatabaseBuilder(context, TimetyDatabase::class.java)
            .addCallback(NewInstallCallback(settings))
            .build()
        try {
            db.openHelper.writableDatabase
            assertEquals(FormSkeleton.newInstallPreset<TaskInput>(), taskOn())
            assertEquals(FormSkeleton.newInstallPreset<HabitInput>(), habitOn())
        } finally {
            db.close()
        }
    }

    @Test
    fun existingDatabase_keepsEveryInputOn() {
        // The first open creates the file; clearing the settings then stands in for an install
        // from before the skeleton existed.
        openFileDatabase()
        runBlocking { settings.restoreAll(emptyMap()) }

        openFileDatabase()
        assertEquals(TaskInput.entries.toSet(), taskOn())
        assertEquals(HabitInput.entries.toSet(), habitOn())
    }

    private fun openFileDatabase() {
        val db = Room.databaseBuilder(context, TimetyDatabase::class.java, FILE_DB)
            .addCallback(NewInstallCallback(settings))
            .build()
        db.openHelper.writableDatabase
        db.close()
    }

    private companion object {
        const val FILE_DB = "form_skeleton_test_db"
    }
}
