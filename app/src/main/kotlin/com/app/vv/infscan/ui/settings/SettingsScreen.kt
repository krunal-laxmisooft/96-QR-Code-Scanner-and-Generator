package com.app.vv.infscan.ui.settings

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.app.vv.infscan.BuildConfig
import com.app.vv.infscan.R
import com.app.vv.infscan.data.prefs.AppSettings
import com.app.vv.infscan.data.prefs.ThemeMode
import com.app.vv.infscan.ui.components.ButtonKind
import com.app.vv.infscan.ui.components.InfButton
import com.app.vv.infscan.ui.components.InfDialog
import com.app.vv.infscan.ui.components.InfIcon
import com.app.vv.infscan.ui.components.InfSwitch
import com.app.vv.infscan.ui.components.InfTopBar
import com.app.vv.infscan.ui.components.SectionHeader
import com.app.vv.infscan.ui.components.SegmentedControl
import com.app.vv.infscan.ui.theme.InfTheme
import com.app.vv.infscan.ui.theme.Shapes
import com.app.vv.infscan.ui.theme.Space

@Composable
fun SettingsScreen(
    state: SettingsUiState,
    onEvent: (SettingsEvent) -> Unit,
    onAdChoices: () -> Unit,
    onPrivacyPolicy: () -> Unit,
    onRate: () -> Unit,
    onFeedback: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val c = InfTheme.colors
    Column(modifier.fillMaxSize().background(c.background)) {
        InfTopBar(stringResource(R.string.tab_settings))
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(top = Space.xxs), verticalArrangement = Arrangement.spacedBy(Space.xs)) {
            Header(R.string.settings_appearance)
            SegmentedControl(
                options = ThemeMode.entries,
                selected = state.settings.themeMode,
                label = { stringResource(it.label()) },
                onSelect = { onEvent(SettingsEvent.Theme(it)) },
                modifier = Modifier.padding(horizontal = Space.screen),
            )
            Header(R.string.settings_scanning, top = Space.md)
            SettingRow(R.drawable.ic_vibrate, stringResource(R.string.settings_vibrate), trailing = {
                InfSwitch(state.settings.vibrateOnScan, { onEvent(SettingsEvent.Vibrate(it)) })
            })
            SettingRow(R.drawable.ic_link, stringResource(R.string.settings_open_links), stringResource(R.string.settings_open_links_hint), trailing = {
                InfSwitch(state.settings.openLinksWithoutAsking, { onEvent(SettingsEvent.OpenLinks(it)) })
            })
            Header(R.string.settings_privacy, top = Space.xs)
            SettingRow(R.drawable.ic_history, stringResource(R.string.settings_keep_history), onClick = { onEvent(SettingsEvent.ChooseRetention) }, trailing = {
                Text(retentionLabel(state.settings.retentionDays), style = InfTheme.type.chipLabel.copy(fontWeight = FontWeight.Bold), color = c.primary)
            })
            SettingRow(R.drawable.ic_delete, stringResource(R.string.settings_clear_history), onClick = { onEvent(SettingsEvent.ClearRequested) }, trailing = {
                InfIcon(R.drawable.ic_delete, null, tint = c.error, size = 22.dp)
            })
            if (state.privacyOptionsRequired) {
                SettingRow(R.drawable.ic_shield, stringResource(R.string.settings_ad_choices), stringResource(R.string.settings_ad_choices_hint), onClick = onAdChoices, trailing = Chevron)
            }
            SettingRow(R.drawable.ic_lock, stringResource(R.string.settings_privacy_policy), onClick = onPrivacyPolicy, trailing = {
                InfIcon(R.drawable.ic_open_in_new, null, tint = c.inkSecondary, size = 20.dp)
            })
            Header(R.string.settings_about, top = Space.xs)
            SettingRow(R.drawable.ic_star, stringResource(R.string.settings_rate), stringResource(R.string.settings_rate_hint), onClick = onRate, trailing = Chevron)
            SettingRow(R.drawable.ic_email, stringResource(R.string.settings_feedback), onClick = onFeedback, trailing = Chevron)
            SettingRow(R.drawable.ic_info, stringResource(R.string.settings_version), stringResource(R.string.settings_version_value, BuildConfig.VERSION_NAME, BuildConfig.VERSION_CODE))
        }
    }
    if (state.confirmClear) {
        InfDialog(
            icon = R.drawable.ic_delete,
            iconTint = c.error,
            title = stringResource(R.string.clear_title),
            body = stringResource(
                R.string.clear_body,
                pluralStringResource(R.plurals.count_scans, state.scannedCount, state.scannedCount),
                pluralStringResource(R.plurals.count_created, state.createdCount, state.createdCount),
            ),
            dismissLabel = stringResource(R.string.action_cancel),
            confirmLabel = stringResource(R.string.clear_confirm),
            onDismiss = { onEvent(SettingsEvent.ClearDismissed) },
            onConfirm = { onEvent(SettingsEvent.ClearConfirmed) },
        )
    }
    if (state.choosingRetention) RetentionDialog(state.settings.retentionDays, onEvent)
}

private val Chevron: @Composable () -> Unit = {
    InfIcon(R.drawable.ic_chevron_right, null, tint = InfTheme.colors.inkSecondary, size = 22.dp)
}

@Composable
private fun Header(@StringRes text: Int, top: Dp = Space.xs) {
    SectionHeader(stringResource(text), Modifier.padding(start = Space.screen, end = Space.screen, top = top))
}

@Composable
private fun SettingRow(
    @DrawableRes icon: Int,
    title: String,
    subtitle: String? = null,
    onClick: (() -> Unit)? = null,
    trailing: @Composable (() -> Unit)? = null,
) {
    val c = InfTheme.colors
    Row(
        Modifier
            .fillMaxWidth()
            .heightIn(min = 52.dp)
            .let { if (onClick != null) it.clickable(role = Role.Button, onClick = onClick) else it }
            .padding(horizontal = Space.screen, vertical = Space.xs),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        InfIcon(icon, null, tint = c.inkSecondary, size = 22.dp, modifier = Modifier.padding(horizontal = 1.dp))
        Column(Modifier.weight(1f)) {
            Text(title, style = InfTheme.type.rowTitle, color = c.ink)
            if (subtitle != null) Text(subtitle, style = InfTheme.type.bodySmall, color = c.inkSecondary)
        }
        trailing?.invoke()
    }
}

@Composable
private fun retentionLabel(days: Int): String =
    if (days == AppSettings.KEEP_FOREVER) stringResource(R.string.retention_forever) else pluralStringResource(R.plurals.retention_days, days, days)

private fun ThemeMode.label(): Int = when (this) {
    ThemeMode.SYSTEM -> R.string.theme_system
    ThemeMode.LIGHT -> R.string.theme_light
    ThemeMode.DARK -> R.string.theme_dark
}

@Composable
private fun RetentionDialog(current: Int, onEvent: (SettingsEvent) -> Unit) {
    val c = InfTheme.colors
    AlertDialog(
        onDismissRequest = { onEvent(SettingsEvent.DismissRetention) },
        shape = Shapes.dialog,
        containerColor = c.surface,
        title = { Text(stringResource(R.string.settings_keep_history), style = InfTheme.type.titleMedium, color = c.ink) },
        text = {
            Column {
                AppSettings.RETENTION_OPTIONS.forEach { days ->
                    Row(
                        Modifier.fillMaxWidth().heightIn(min = 48.dp).selectable(selected = days == current, role = Role.RadioButton, onClick = { onEvent(SettingsEvent.Retention(days)) }),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(selected = days == current, onClick = null, colors = RadioButtonDefaults.colors(selectedColor = c.primary, unselectedColor = c.inkSecondary))
                        Text(retentionLabel(days), style = InfTheme.type.bodyLarge, color = c.ink, modifier = Modifier.padding(start = Space.md))
                    }
                }
            }
        },
        confirmButton = {
            InfButton(stringResource(R.string.action_cancel), { onEvent(SettingsEvent.DismissRetention) }, kind = ButtonKind.Text, height = 48.dp, fillWidth = false)
        },
    )
}
