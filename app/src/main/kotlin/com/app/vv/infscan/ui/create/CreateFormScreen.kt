package com.app.vv.infscan.ui.create

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.background
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.LaunchedEffect
import com.app.vv.infscan.R
import com.app.vv.infscan.domain.create.CreateType
import com.app.vv.infscan.domain.create.FormField
import com.app.vv.infscan.domain.create.PayloadBuilder
import com.app.vv.infscan.domain.model.WifiSecurity
import com.app.vv.infscan.ui.components.InfButton
import com.app.vv.infscan.ui.components.InfIcon
import com.app.vv.infscan.ui.components.InfIconButton
import com.app.vv.infscan.ui.components.InfSwitch
import com.app.vv.infscan.ui.components.InfTextField
import com.app.vv.infscan.ui.components.InfTopBar
import com.app.vv.infscan.ui.result.label
import com.app.vv.infscan.ui.theme.InfTheme
import com.app.vv.infscan.ui.theme.Space
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

@Composable
fun CreateFormRoute(onBack: () -> Unit, onPreview: (Long) -> Unit, viewModel: CreateFormViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    LaunchedEffect(viewModel) {
        viewModel.effects.collect { effect ->
            when (effect) {
                is CreateFormEffect.OpenPreview -> onPreview(effect.id)
            }
        }
    }
    CreateFormScreen(state, viewModel::onEvent, onBack)
}

@Composable
fun CreateFormScreen(state: CreateFormUiState, onEvent: (CreateFormEvent) -> Unit, onBack: () -> Unit, modifier: Modifier = Modifier) {
    val c = InfTheme.colors
    Column(modifier.fillMaxSize().background(c.background)) {
        InfTopBar(
            title = stringResource(R.string.create_form_title, stringResource(state.type.title)),
            onBack = onBack,
            backLabel = stringResource(R.string.action_back),
        )
        Column(
            Modifier.weight(1f).imePadding().verticalScroll(rememberScrollState()).padding(horizontal = Space.screen, vertical = Space.md),
            verticalArrangement = Arrangement.spacedBy(Space.fieldGap),
        ) {
            when (state.type) {
                CreateType.WIFI -> WifiFields(state, onEvent)
                CreateType.EVENT -> EventFields(state, onEvent)
                else -> PayloadBuilder.fields(state.type).forEach { field -> TextFormField(state, field, onEvent) }
            }
        }
        Column(Modifier.navigationBarsPadding().padding(start = Space.screen, end = Space.screen, top = Space.sm, bottom = Space.xl)) {
            InfButton(stringResource(R.string.create_submit), { onEvent(CreateFormEvent.Submit) }, icon = R.drawable.ic_qr)
        }
    }
}

@Composable
private fun TextFormField(state: CreateFormUiState, field: FormField, onEvent: (CreateFormEvent) -> Unit, trailing: @Composable (() -> Unit)? = null, visual: VisualTransformation = VisualTransformation.None) {
    val multiline = field == FormField.TEXT || field == FormField.BODY || field == FormField.MESSAGE
    InfTextField(
        label = stringResource(field.label),
        value = state.values.text[field].orEmpty(),
        onValueChange = { onEvent(CreateFormEvent.FieldChanged(field, it)) },
        error = state.errors[field]?.let { stringResource(it) },
        singleLine = !multiline,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardFor(field), capitalization = if (multiline || field == FormField.CONTACT_NAME || field == FormField.EVENT_TITLE) KeyboardCapitalization.Sentences else KeyboardCapitalization.None),
        mono = field == FormField.PASSWORD || field == FormField.LATITUDE || field == FormField.LONGITUDE,
        visualTransformation = visual,
        trailing = trailing,
    )
}

private fun keyboardFor(field: FormField): KeyboardType = when (field) {
    FormField.URL -> KeyboardType.Uri
    FormField.EMAIL, FormField.TO -> KeyboardType.Email
    FormField.PHONE, FormField.SMS_NUMBER -> KeyboardType.Phone
    FormField.LATITUDE, FormField.LONGITUDE -> KeyboardType.Decimal
    else -> KeyboardType.Text
}

@Composable
private fun WifiFields(state: CreateFormUiState, onEvent: (CreateFormEvent) -> Unit) {
    val c = InfTheme.colors
    TextFormField(state, FormField.SSID, onEvent)
    SecurityField(state.values.security) { onEvent(CreateFormEvent.SecuritySelected(it)) }
    if (state.values.security != WifiSecurity.NONE) {
        TextFormField(
            state, FormField.PASSWORD, onEvent,
            trailing = {
                InfIconButton(R.drawable.ic_visibility, stringResource(R.string.result_toggle_password), { onEvent(CreateFormEvent.TogglePassword) }, tint = c.inkSecondary)
            },
            visual = if (state.passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
        )
    }
    Row(Modifier.fillMaxWidth().heightIn(min = 52.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Text(stringResource(R.string.field_hidden_network), style = InfTheme.type.rowTitle, color = c.ink)
        InfSwitch(state.values.hidden, { onEvent(CreateFormEvent.HiddenChanged(it)) })
    }
}

@Composable
private fun SecurityField(selected: WifiSecurity, onSelect: (WifiSecurity) -> Unit) {
    var open by remember { mutableStateOf(false) }
    Box(Modifier.fillMaxWidth().wrapContentSize(Alignment.TopStart)) {
        InfTextField(
            label = stringResource(R.string.field_security),
            value = stringResource(selected.label()),
            onValueChange = {},
            readOnly = true,
            trailing = { InfIcon(R.drawable.ic_chevron_right, null, tint = InfTheme.colors.inkSecondary) },
        )
        Box(Modifier.matchParentSize().clickable(role = Role.DropdownList) { open = true })
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            WifiSecurity.entries.forEach { option ->
                DropdownMenuItem(text = { Text(stringResource(option.label())) }, onClick = { onSelect(option); open = false })
            }
        }
    }
}

@Composable
private fun EventFields(state: CreateFormUiState, onEvent: (CreateFormEvent) -> Unit) {
    TextFormField(state, FormField.EVENT_TITLE, onEvent)
    DateTimeField(stringResource(R.string.field_starts), state.values.startMillis) { onEvent(CreateFormEvent.StartChanged(it)) }
    DateTimeField(stringResource(R.string.field_ends), state.values.endMillis) { onEvent(CreateFormEvent.EndChanged(it)) }
    TextFormField(state, FormField.EVENT_LOCATION, onEvent)
}

@Composable
private fun DateTimeField(label: String, millis: Long?, onPicked: (Long) -> Unit) {
    var picking by remember { mutableStateOf(false) }
    val zone = ZoneId.systemDefault()
    val text = millis?.let { DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM, FormatStyle.SHORT).format(Instant.ofEpochMilli(it).atZone(zone)) }.orEmpty()
    Box(Modifier.fillMaxWidth()) {
        InfTextField(label, text, {}, readOnly = true, trailing = { InfIcon(R.drawable.ic_event, null, tint = InfTheme.colors.inkSecondary) })
        Box(Modifier.matchParentSize().clickable(role = Role.Button) { picking = true })
    }
    if (picking) {
        DateTimePickerDialog(initialMillis = millis ?: System.currentTimeMillis(), onDismiss = { picking = false }, onConfirm = { picking = false; onPicked(it) })
    }
}
