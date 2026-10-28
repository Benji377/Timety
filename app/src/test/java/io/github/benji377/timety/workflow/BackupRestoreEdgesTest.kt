package io.github.benji377.timety.workflow

import android.content.Context
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import io.github.benji377.timety.data.model.user.UserProfileEntity
import io.github.benji377.timety.services.BackupService
import io.github.benji377.timety.testutil.TestAppContainer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.json.JSONArray
import org.json.JSONObject
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.time.Instant

/** Restore paths around the data step: what happens after it commits, and the avatar file. */
@RunWith(AndroidJUnit4::class)
class BackupRestoreEdgesTest {
    private val context: Context = InstrumentationRegistry.getInstrumentation().targetContext
    private lateinit var container: TestAppContainer

    @Before
    fun setUp() {
        container = TestAppContainer(context)
    }

    @After
    fun tearDown() = container.close()

    private fun backup(profileImagePath: String?) = JSONObject().apply {
        put("payloadType", "user_data")
        put("schemaVersion", 1)
        put("userProfile", JSONObject().apply {
            put("name", "Restored")
            put("profileImagePath", profileImagePath ?: JSONObject.NULL)
            put("accountCreated", "2026-01-01T00:00:00Z")
        })
        put("tasks", JSONArray().put(JSONObject().apply {
            put("id", "t1")
            put("title", "From the backup")
            put("createdAt", "2026-01-01T00:00:00Z")
        }))
    }

    @Test
    fun aFailureAfterTheDataCommitted_stillReportsSuccess() = runBlocking {
        val service = BackupService(context, container.database, container.settingsRepository) {
            error("alarm refresh failed")
        }
        val result = service.importFromJson(backup(profileImagePath = null))
        assertTrue("the data is in, so the import succeeded: $result", result.isSuccess)
        assertEquals("From the backup", container.taskRepository.getTaskById("t1")?.title)
    }

    @Test
    fun aStaleAvatarPath_keepsThisDevicesAvatar() = runBlocking(Dispatchers.IO) {
        val avatar = File(context.filesDir, "profile/avatar_test.img").apply {
            parentFile?.mkdirs()
            writeBytes(byteArrayOf(1, 2, 3))
        }
        try {
            container.database.userDao().insertUserProfile(
                UserProfileEntity(name = "Me", profileImagePath = avatar.absolutePath, accountCreated = Instant.EPOCH)
            )
            val result = container.backupService.importFromJson(backup("/data/gone/avatar_old.img"))
            assertTrue(result.isSuccess)
            val profile = container.database.userDao().getUserProfileSynchronous()
            assertEquals("Restored", profile?.name)
            assertEquals(avatar.absolutePath, profile?.profileImagePath)
        } finally {
            avatar.delete()
        }
    }
}
