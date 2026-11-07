package com.app.vv.infscan.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.app.vv.infscan.ui.theme.InfTheme
import com.app.vv.infscan.ui.theme.Shapes
import com.app.vv.infscan.ui.theme.Sizes

@Composable
fun InfTextField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    error: String? = null,
    mono: Boolean = false,
    singleLine: Boolean = true,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    readOnly: Boolean = false,
    trailing: @Composable (() -> Unit)? = null,
) {
    val c = InfTheme.colors
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label, style = InfTheme.type.caption) },
        isError = error != null,
        supportingText = error?.let { { Text(it, style = InfTheme.type.caption) } },
        singleLine = singleLine,
        minLines = if (singleLine) 1 else 3,
        readOnly = readOnly,
        keyboardOptions = keyboardOptions,
        visualTransformation = visualTransformation,
        trailingIcon = trailing,
        textStyle = if (mono) InfTheme.type.mono.copy(fontSize = InfTheme.type.bodyLarge.fontSize) else InfTheme.type.bodyLarge,
        shape = Shapes.input,
        colors = OutlinedTextFieldDefaults.colors(
            focusedTextColor = c.ink,
            unfocusedTextColor = c.ink,
            focusedContainerColor = c.surface,
            unfocusedContainerColor = c.surface,
            errorContainerColor = c.surface,
            focusedBorderColor = c.primary,
            unfocusedBorderColor = c.inkSecondary,
            errorBorderColor = c.error,
            focusedLabelColor = c.primary,
            unfocusedLabelColor = c.inkSecondary,
            errorLabelColor = c.error,
            errorSupportingTextColor = c.error,
            cursorColor = c.primary,
            errorCursorColor = c.error,
        ),
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = Sizes.field),
    )
}

@Composable
fun InfSwitch(checked: Boolean, onCheckedChange: (Boolean) -> Unit, modifier: Modifier = Modifier) {
    val c = InfTheme.colors
    Switch(
        checked = checked,
        onCheckedChange = onCheckedChange,
        modifier = modifier,
        colors = SwitchDefaults.colors(
            checkedThumbColor = c.onPrimary,
            checkedTrackColor = c.primary,
            checkedBorderColor = c.primary,
            uncheckedThumbColor = c.inkSecondary,
            uncheckedTrackColor = c.surfaceVariant,
            uncheckedBorderColor = c.inkSecondary,
        ),
    )
}

@Composable
fun <T> SegmentedControl(
    options: List<T>,
    selected: T,
    label: @Composable (T) -> String,
    onSelect: (T) -> Unit,
    modifier: Modifier = Modifier,
) {
    val c = InfTheme.colors
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(Sizes.touch)
            .clip(Shapes.pill)
            .border(BorderStroke(1.5.dp, c.inkSecondary), Shapes.pill)
            .selectableGroup(),
    ) {
        options.forEach { option ->
            val on = option == selected
            Row(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .height(Sizes.touch)
                    .background(if (on) c.soft else Color.Transparent)
                    .selectable(selected = on, role = Role.RadioButton, onClick = { onSelect(option) }),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(label(option), style = InfTheme.type.chipLabel.copy(fontWeight = FontWeight.Bold), color = if (on) c.onSoft else c.ink)
            }
        }
    }
}
