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
import androidx.compose.material3.TextFieldColors
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import io.github.benji377.timety.ui.theme.AppTheme

/**
 * Outlined text field built on [BasicTextField] with M3's [OutlinedTextFieldDefaults] decoration,
 * so the app's custom [shape] and border widths can be applied without fighting M3's own OutlinedTextField.
 *
 * Both the focused and unfocused border use the same bold [AppTheme.neoBorderWidth] thickness -
 * per the reference sheet, a thin/gray unfocused border is the app's single worst neobrutalism
 * offender, so there is no "resting" thin state here. The field also casts a hard [neoShadow]
 * matching [shape]; see that function's KDoc for the trailing/bottom spacing it needs.
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
    visualTransformation: VisualTransformation = VisualTransformation.None,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    singleLine: Boolean = false,
    maxLines: Int = if (singleLine) 1 else Int.MAX_VALUE,
    minLines: Int = 1,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    shape: Shape = AppTheme.brNeo,
    colors: TextFieldColors = detailFieldColors(),
    active: Boolean = enabled,
) {
    val mergedTextStyle = textStyle.merge(TextStyle(color = MaterialTheme.colorScheme.onSurface))

    // The container background comes from [colors] via the DecorationBox Container below,
    // so callers can restyle the disabled state.
    BasicTextField(
        value = value,
        modifier = modifier
            // Room for the floating label's top half, like M3's OutlinedTextFieldTopPadding;
            // without it the label gets clipped at the top.
            .then(if (label != null) Modifier.padding(top = 8.dp) else Modifier)
            .defaultMinSize(
                minWidth = OutlinedTextFieldDefaults.MinWidth,
                minHeight = OutlinedTextFieldDefaults.MinHeight
            ),
        onValueChange = onValueChange,
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
                supportingText = supportingText,
                // Single-row fields (minLines == 1) center their text on the leading icon;
                // real text areas (minLines > 1) keep the text top-aligned.
                singleLine = singleLine || minLines == 1,
                enabled = enabled,
                isError = isError,
                interactionSource = interactionSource,
                colors = colors,
                contentPadding = PaddingValues(AppTheme.spaceMedium),
                container = {
                    OutlinedTextFieldDefaults.Container(
                        enabled = enabled,
                        isError = isError,
                        interactionSource = interactionSource,
                        // Only an active field is lifted off the page; a read-only one sits flat,
                        // which together with its recessed fill is what makes view mode read as
                        // view mode. The shadow hangs off the container rather than off the whole
                        // field, because the field's bounds also cover the floating label above it
                        // and any supporting text below - a shadow spanning those would paint the
                        // empty strips around the container solid black.
                        modifier = if (active) Modifier.neoShadow(shape = shape) else Modifier,
                        colors = colors,
                        shape = shape,
                        focusedBorderThickness = AppTheme.neoBorderWidth,
                        unfocusedBorderThickness = AppTheme.neoBorderWidth
                    )
                }
            )
        }
    )
}
