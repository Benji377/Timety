package io.github.benji377.timety.ui.utils

import android.content.Context
import android.graphics.BitmapFactory
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.GraphicsMode

@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class WrapUpImageGeneratorTest {
    private val context: Context = InstrumentationRegistry.getInstrumentation().targetContext

    @Test
    fun rendersAStorySizedPng_withAndWithoutAJoinDate() {
        listOf("01.02.2025", null).forEach { since ->
            val bitmap = WrapUpImageGenerator.render(
                context, "A rather long name that has to shrink to fit", since, 1234, 98765, 42
            )
            val png = WrapUpImageGenerator.toPng(bitmap)
            val decoded = BitmapFactory.decodeByteArray(png, 0, png.size)
            assertEquals(1080, decoded.width)
            assertEquals(1920, decoded.height)
        }
    }
}
