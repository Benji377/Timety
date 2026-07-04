package io.github.benji377.timety.ui.utils

import io.github.benji377.timety.data.model.task.TaskSize
import org.junit.Assert.assertEquals
import org.junit.Test

class AppUtilsTest {

    @Test
    fun testGetSizeEmoji() {
        assertEquals("S", AppUtils.getSizeEmoji(TaskSize.SMALL))
        assertEquals("M", AppUtils.getSizeEmoji(TaskSize.MEDIUM))
        assertEquals("L", AppUtils.getSizeEmoji(TaskSize.LARGE))
        assertEquals("XL", AppUtils.getSizeEmoji(TaskSize.VERY_LARGE))
    }
}
