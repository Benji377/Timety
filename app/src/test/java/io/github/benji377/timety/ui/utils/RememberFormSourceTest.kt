package io.github.benji377.timety.ui.utils

import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Checks the editor form pattern: a field keyed on [rememberFormSource] follows the stored item
 * while viewing, and keeps the user's input while editing, also across a recreation after which
 * the item loads again.
 */
@RunWith(AndroidJUnit4::class)
class RememberFormSourceTest {
    @get:Rule
    val compose = createComposeRule()

    // The stored item as the screen's flow delivers it: null until it has loaded.
    private var stored by mutableStateOf<String?>("Saved title")
    private lateinit var startEditing: () -> Unit
    private lateinit var type: (String) -> Unit

    private fun setEditor(tester: StateRestorationTester) = tester.setContent {
        var isEditing by rememberSaveable { mutableStateOf(false) }
        val source = rememberFormSource(stored, isEditing)
        var title by rememberSaveable(source) { mutableStateOf(source ?: "") }
        startEditing = { isEditing = true }
        type = { title = it }
        BasicText(title, Modifier.testTag(TITLE))
    }

    @Test
    fun viewing_followsTheStoredItem() {
        setEditor(StateRestorationTester(compose))

        stored = "Renamed elsewhere"

        compose.onNodeWithTag(TITLE).assertTextEquals("Renamed elsewhere")
    }

    @Test
    fun editing_keepsTheInput_whenTheStoredItemChanges() {
        setEditor(StateRestorationTester(compose))
        compose.runOnIdle {
            startEditing()
            type("Typed title")
        }
        compose.waitForIdle()

        stored = "Renamed elsewhere"

        compose.onNodeWithTag(TITLE).assertTextEquals("Typed title")
    }

    @Test
    fun editing_keepsTheInput_acrossARecreationThatReloadsTheItem() {
        val tester = StateRestorationTester(compose)
        setEditor(tester)
        compose.runOnIdle {
            startEditing()
            type("Typed title")
        }
        compose.waitForIdle()

        stored = null
        tester.emulateSavedInstanceStateRestore()
        stored = "Saved title"

        compose.onNodeWithTag(TITLE).assertTextEquals("Typed title")
    }

    private companion object {
        const val TITLE = "title"
    }
}
