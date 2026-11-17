package io.github.benji377.timety.ui.components.common

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldColors
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import io.github.benji377.timety.ui.theme.AppTheme
import io.github.benji377.timety.ui.theme.ErrorColor
import io.github.benji377.timety.ui.theme.forText

/**
 * Outlined text field built on [BasicTextField] with M3's [OutlinedTextFieldDefaults] decoration,
 * so the app's custom [shape] and border widths can be applied without fighting M3's own OutlinedTextField.
 *
 * Focused and unfocused borders share the same [AppTheme.borderHairline] thickness; focus is
 * signalled by the border *color* from [colors], not by the stroke growing. An editable field is
 * likewise told apart from a read-only one purely by its container fill (see [detailFieldColors]).
 *
 * [errorText] marks the field as invalid and shows the message below it; with no error, no line is
 * reserved, so fields keep the same spacing whether or not they can fail validation.
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
    visualTransformation: VisualTransformation = VisualTransformation.None,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    singleLine: Boolean = false,
    maxLines: Int = if (singleLine) 1 else Int.MAX_VALUE,
    minLines: Int = 1,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    shape: Shape = AppTheme.brNeo,
    colors: TextFieldColors = detailFieldColors(),
    selectAllInitially: Boolean = false,
) {
    val mergedTextStyle = textStyle.merge(TextStyle(color = MaterialTheme.colorScheme.onSurface))
    val shownSupportingText = supportingText ?: errorText?.let { { Text(it, color = ErrorColor.forText()) } }
    val showsError = isError || errorText != null

    // Held as a TextFieldValue so a field focused from code starts with the cursor after its text,
    // or with it all selected so typing replaces a prefilled value.
    var fieldValue by remember {
        mutableStateOf(TextFieldValue(value, TextRange(if (selectAllInitially) 0 else value.length, value.length)))
    }
    val shownValue = if (fieldValue.text == value) fieldValue else fieldValue.copy(text = value)

    // The container background comes from [colors] via the DecorationBox Container below,
    // so callers can restyle the disabled state.
    BasicTextField(
        value = shownValue,
        modifier = modifier
            // Room for the floating label's top half, like M3's OutlinedTextFieldTopPadding;
            // without it the label gets clipped at the top.
            .then(if (label != null) Modifier.padding(top = 8.dp) else Modifier)
            .defaultMinSize(
                minWidth = OutlinedTextFieldDefaults.MinWidth,
                minHeight = OutlinedTextFieldDefaults.MinHeight
            ),
        onValueChange = { newValue ->
            fieldValue = newValue
            if (newValue.text != value) onValueChange(newValue.text)
        },
        enabled = enabled,
        readOnly = readOnly,
        textStyle = mergedTextStyle,
        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
        visualTransformation = visualTransformation,
        keyboardOptions = keyboardOptions,
        keyboardActions = keyboardActions,
        interactionSource = interactionSource,
        singleLine = singleLine,
        maxLines = maxLines,
        minLines = minLines,
        decorationBox = @Composable { innerTextField ->
            OutlinedTextFieldDefaults.DecorationBox(
                value = value,
                visualTransformation = visualTransformation,
                innerTextField = innerTextField,
                placeholder = placeholder,
                label = label,
                leadingIcon = leadingIcon,
                trailingIcon = trailingIcon,
                prefix = prefix,
                suffix = suffix,
                supportingText = shownSupportingText,
                // Single-row fields (minLines == 1) center their text on the leading icon;
                // real text areas (minLines > 1) keep the text top-aligned.
                singleLine = singleLine || minLines == 1,
                enabled = enabled,
                isError = showsError,
                interactionSource = interactionSource,
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
            )
        }
    )
}
