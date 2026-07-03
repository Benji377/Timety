package io.github.benji377.timety.ui.utils

import io.github.benji377.timety.data.model.task.TaskSize
import org.junit.Assert.assertEquals
import org.junit.Test

class AppUtilsTest {

    @Test
    fun testGetSizeEmoji() {
        assertEquals("🌱", AppUtils.getSizeEmoji(TaskSize.SMALL))
        assertEquals("🌿", AppUtils.getSizeEmoji(TaskSize.MEDIUM))
        assertEquals("🌳", AppUtils.getSizeEmoji(TaskSize.LARGE))
        assertEquals("🏔️", AppUtils.getSizeEmoji(TaskSize.VERY_LARGE))
    }
}
