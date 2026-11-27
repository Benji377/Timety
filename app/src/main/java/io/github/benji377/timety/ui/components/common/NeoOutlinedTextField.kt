package io.github.benji377.timety.ui.components.common

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.InputTransformation
import androidx.compose.foundation.text.input.KeyboardActionHandler
import androidx.compose.foundation.text.input.TextFieldBuffer
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.setTextAndPlaceCursorAtEnd
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldColors
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import io.github.benji377.timety.ui.theme.AppTheme
import io.github.benji377.timety.ui.theme.ErrorColor
import io.github.benji377.timety.ui.theme.forText

/**
 * Outlined text field built on [BasicTextField] with M3's [OutlinedTextFieldDefaults] decorator,
 * so the app's custom [shape] and border widths can be applied without fighting M3's own OutlinedTextField.
 *
 * Focused and unfocused borders share the same [AppTheme.borderHairline] thickness; focus is
 * signalled by the border *color* from [colors], not by the stroke growing. An editable field is
 * likewise told apart from a read-only one purely by its container fill (see [detailFieldColors]).
 *
 * [errorText] marks the field as invalid and shows the message below it; with no error, no line is
 * reserved, so fields keep the same spacing whether or not they can fail validation.
 *
 * The caller owns [value] and must accept every text passed to [onValueChange]; input that is not
 * allowed, such as letters in a number field, is filtered with [inputTransformation] instead.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NeoOutlinedTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    readOnly: Boolean = false,
    textStyle: TextStyle = LocalTextStyle.current,
    label: @Composable (() -> Unit)? = null,
    placeholder: @Composable (() -> Unit)? = null,
    leadingIcon: @Composable (() -> Unit)? = null,
    trailingIcon: @Composable (() -> Unit)? = null,
    prefix: @Composable (() -> Unit)? = null,
    suffix: @Composable (() -> Unit)? = null,
    supportingText: @Composable (() -> Unit)? = null,
    isError: Boolean = false,
    errorText: String? = null,
    inputTransformation: InputTransformation? = null,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    onKeyboardAction: KeyboardActionHandler? = null,
    singleLine: Boolean = false,
    maxLines: Int = if (singleLine) 1 else Int.MAX_VALUE,
    minLines: Int = 1,
    shape: Shape = AppTheme.brNeo,
    colors: TextFieldColors = detailFieldColors(),
    selectAllInitially: Boolean = false,
) {
    val mergedTextStyle = textStyle.merge(TextStyle(color = MaterialTheme.colorScheme.onSurface))
    val shownSupportingText = supportingText ?: errorText?.let { { Text(it, color = ErrorColor.forText()) } }
    val showsError = isError || errorText != null
    val interactionSource = remember { MutableInteractionSource() }
    val lineLimits = if (singleLine) TextFieldLineLimits.SingleLine else TextFieldLineLimits.MultiLine(minLines, maxLines)

    // A field focused from code starts with the cursor after its text, or with it all selected
    // so typing replaces a prefilled value.
    val state = remember {
        TextFieldState(value, TextRange(if (selectAllInitially) 0 else value.length, value.length))
    }
    // Texts reported to the caller that have not come back as [value] yet. A value in this list is
    // the caller catching up, not a change from outside, so it must not reset what was typed since.
    val pendingEchoes = remember { ArrayDeque<String>() }
    val currentValue by rememberUpdatedState(value)
    val currentOnValueChange by rememberUpdatedState(onValueChange)

    LaunchedEffect(state) {
        snapshotFlow { state.text.toString() }.collect { text ->
            if (text != currentValue) {
                pendingEchoes.addLast(text)
                currentOnValueChange(text)
            }
        }
    }
    LaunchedEffect(value) {
        val echoIndex = pendingEchoes.indexOf(value)
        if (echoIndex >= 0) {
            repeat(echoIndex + 1) { pendingEchoes.removeFirst() }
        } else if (state.text.toString() != value) {
            // Changed by the caller, such as a picker filling the field or a submit clearing it.
            pendingEchoes.clear()
            state.setTextAndPlaceCursorAtEnd(value)
        }
    }

    // The container background comes from [colors] via the decorator's Container below,
    // so callers can restyle the disabled state.
    BasicTextField(
        state = state,
        modifier = modifier
            // Room for the floating label's top half, like M3's OutlinedTextFieldTopPadding;
            // without it the label gets clipped at the top.
            .then(if (label != null) Modifier.padding(top = 8.dp) else Modifier)
            .defaultMinSize(
                minWidth = OutlinedTextFieldDefaults.MinWidth,
                minHeight = OutlinedTextFieldDefaults.MinHeight
            ),
        enabled = enabled,
        readOnly = readOnly,
        inputTransformation = inputTransformation,
        textStyle = mergedTextStyle,
        keyboardOptions = keyboardOptions,
        onKeyboardAction = onKeyboardAction,
        lineLimits = lineLimits,
        interactionSource = interactionSource,
        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
        decorator = OutlinedTextFieldDefaults.decorator(
            state = state,
            enabled = enabled,
            // The decorator only reads this to align the text. Single-row fields (minLines == 1)
            // center their text on the leading icon; real text areas (minLines > 1) keep it top-aligned.
            lineLimits = if (minLines == 1) TextFieldLineLimits.SingleLine else lineLimits,
            outputTransformation = null,
            interactionSource = interactionSource,
            label = label?.let { { it() } },
            placeholder = placeholder,
            leadingIcon = leadingIcon,
            trailingIcon = trailingIcon,
            prefix = prefix,
            suffix = suffix,
            supportingText = shownSupportingText,
            isError = showsError,
            colors = colors,
            contentPadding = PaddingValues(AppTheme.spaceMedium),
            container = {
                OutlinedTextFieldDefaults.Container(
                    enabled = enabled,
                    isError = showsError,
                    interactionSource = interactionSource,
                    colors = colors,
                    shape = shape,
                    focusedBorderThickness = AppTheme.borderHairline,
                    unfocusedBorderThickness = AppTheme.borderHairline
                )
            }
        ),
    )
}

/** Accepts only digits, at most [maxLength] of them; any other edit is rejected whole. */
data class DigitsOnly(private val maxLength: Int) : InputTransformation {
    override fun TextFieldBuffer.transformInput() {
        if (length > maxLength || !asCharSequence().all(Char::isDigit)) revertAllChanges()
    }
}
