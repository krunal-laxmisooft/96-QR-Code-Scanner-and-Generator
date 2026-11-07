package com.app.vv.infscan.ui.result

import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.app.vv.infscan.R
import com.app.vv.infscan.domain.model.HistoryItem
import com.app.vv.infscan.domain.model.ParsedCode
import com.app.vv.infscan.domain.model.ScanSource
import com.app.vv.infscan.domain.model.WifiSecurity
import com.app.vv.infscan.ui.common.icon
import com.app.vv.infscan.ui.common.label
import com.app.vv.infscan.ui.common.relativeTimeLabel
import com.app.vv.infscan.ui.components.ButtonKind
import com.app.vv.infscan.ui.components.InfButton
import com.app.vv.infscan.ui.components.InfCard
import com.app.vv.infscan.ui.components.InfIconButton
import com.app.vv.infscan.ui.components.InfTopBar
import com.app.vv.infscan.ui.components.TypeChip
import com.app.vv.infscan.ui.theme.InfTheme
import com.app.vv.infscan.ui.theme.Space

@Composable
fun ResultScreen(
    state: ResultUiState,
    onEvent: (ResultEvent) -> Unit,
    onBack: () -> Unit,
    adSlot: @Composable () -> Unit,
    modifier: Modifier = Modifier,
) {
    val c = InfTheme.colors
    val item = state.item
    val isProduct = state.parsed is ParsedCode.Product
    Column(modifier.fillMaxSize().background(c.background)) {
        InfTopBar(
            title = stringResource(if (isProduct) R.string.result_title_product else R.string.result_title),
            onBack = onBack,
            backLabel = stringResource(R.string.action_back),
            actions = {
                if (item != null) {
                    InfIconButton(
                        R.drawable.ic_star,
                        stringResource(if (item.favorite) R.string.action_unfavorite else R.string.action_favorite),
                        { onEvent(ResultEvent.ToggleFavorite) },
                        tint = if (item.favorite) c.primary else c.ink,
                    )
                }
            },
        )
        when {
            state.loading -> Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = c.primary) }
            item == null || state.parsed == null -> MissingResult(onBack)
            else -> Column(
                Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(start = Space.screen, end = Space.screen, top = Space.xs, bottom = Space.screen),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                when (val parsed = state.parsed) {
                    is ParsedCode.Product -> ProductContent(item, parsed, state, onEvent)
                    is ParsedCode.Url -> LinkContent(item, parsed, state, onEvent)
                    is ParsedCode.Wifi -> WifiContent(item, parsed, state, onEvent)
                    else -> GenericContent(item, parsed, onEvent)
                }
                val standard = state.parsed !is ParsedCode.Product && state.parsed !is ParsedCode.Wifi
                if (standard) ActionRow(item.favorite, { onEvent(ResultEvent.Copy) }, { onEvent(ResultEvent.Share) }, { onEvent(ResultEvent.ToggleFavorite) })
                adSlot()
                if (standard) {
                    Text(
                        stringResource(R.string.result_saved_footer, relativeTimeLabel(item.createdAt, state.now)),
                        style = InfTheme.type.caption,
                        color = c.inkSecondary,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }
    }
}

@Composable
private fun MissingResult(onBack: () -> Unit) {
    val c = InfTheme.colors
    Column(Modifier.fillMaxSize().padding(Space.xxl), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(Space.lg, Alignment.CenterVertically)) {
        Text(stringResource(R.string.result_missing), style = InfTheme.type.titleSmall, color = c.ink, textAlign = TextAlign.Center)
        InfButton(stringResource(R.string.action_back), onBack, kind = ButtonKind.Tonal)
    }
}

@Composable
fun ResultHeader(item: HistoryItem, parsed: ParsedCode, @StringRes kindLabel: Int = parsed.kind.label()) {
    val c = InfTheme.colors
    val source = when (item.source) {
        ScanSource.CAMERA -> R.string.source_camera
        ScanSource.GALLERY -> R.string.source_gallery
        ScanSource.CREATED -> R.string.source_created
    }
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        TypeChip(parsed.kind.icon(), stringResource(kindLabel))
        Text(stringResource(R.string.result_source, stringResource(item.format.label()), stringResource(source)), style = InfTheme.type.caption.copy(fontSize = InfTheme.type.bodySmall.fontSize), color = c.inkSecondary)
    }
}

@Composable
private fun LinkContent(item: HistoryItem, parsed: ParsedCode.Url, state: ResultUiState, onEvent: (ResultEvent) -> Unit) {
    val c = InfTheme.colors
    val link = state.link
    InfCard {
        ResultHeader(item, parsed)
        Text(link?.safety?.host?.ifEmpty { parsed.url } ?: parsed.url, style = InfTheme.type.result, color = c.ink)
        Text(parsed.url, style = InfTheme.type.monoSmall, color = c.inkSecondary)
        if (link != null) LinkNotice(link)
    }
    if (link?.needsWarning == true) {
        InfButton(stringResource(R.string.result_dont_open), { onEvent(ResultEvent.DontOpen) }, icon = R.drawable.ic_close)
        InfButton(stringResource(R.string.result_open_anyway), { onEvent(ResultEvent.OpenAnyway) }, kind = ButtonKind.Text, height = 44.dp)
    } else {
        InfButton(stringResource(R.string.result_open_link), { onEvent(ResultEvent.Primary) }, icon = R.drawable.ic_open_in_new)
    }
}

@Composable
private fun LinkNotice(link: LinkUiState) {
    when {
        link.safety.isShortened -> NoticeBanner(
            R.drawable.ic_warning,
            stringResource(R.string.link_shortened_title),
            when {
                link.resolving -> stringResource(R.string.link_shortened_checking)
                link.destination != null -> stringResource(R.string.link_shortened_destination, link.destination)
                else -> stringResource(R.string.link_shortened_unknown)
            },
            NoticeTone.Warning,
        )
        !link.safety.isHttps -> NoticeBanner(R.drawable.ic_warning, stringResource(R.string.link_http_title), stringResource(R.string.link_http_body), NoticeTone.Warning)
        else -> NoticeBanner(R.drawable.ic_shield, stringResource(R.string.link_secure_title), stringResource(R.string.link_secure_body), NoticeTone.Success)
    }
}

@Composable
private fun WifiContent(item: HistoryItem, parsed: ParsedCode.Wifi, state: ResultUiState, onEvent: (ResultEvent) -> Unit) {
    val c = InfTheme.colors
    InfCard(spacing = 6.dp) {
        ResultHeader(item, parsed, R.string.kind_wifi_network)
        Text(parsed.ssid, style = InfTheme.type.result, color = c.ink, modifier = Modifier.padding(top = 6.dp, bottom = 4.dp))
        DetailRow(stringResource(R.string.field_security), stringResource(parsed.security.label()))
        if (parsed.security != WifiSecurity.NONE) {
            DetailRow(
                stringResource(R.string.field_password),
                if (state.passwordVisible) parsed.password else "•".repeat(PASSWORD_MASK_LENGTH),
                mono = true,
                trailing = {
                    InfIconButton(R.drawable.ic_visibility, stringResource(R.string.result_toggle_password), { onEvent(ResultEvent.TogglePassword) }, tint = c.inkSecondary, size = 40.dp)
                },
            )
        }
        DetailRow(stringResource(R.string.field_hidden), stringResource(if (parsed.hidden) R.string.value_yes else R.string.value_no))
    }
    if (parsed.security != WifiSecurity.NONE) InfButton(stringResource(R.string.result_copy_password), { onEvent(ResultEvent.CopyPassword) }, icon = R.drawable.ic_copy)
    InfButton(stringResource(R.string.result_share_network), { onEvent(ResultEvent.Share) }, kind = ButtonKind.Tonal, icon = R.drawable.ic_share)
    Text(stringResource(R.string.result_wifi_hint), style = InfTheme.type.bodySmall, color = c.inkSecondary)
}

@StringRes
fun WifiSecurity.label(): Int = when (this) {
    WifiSecurity.WPA -> R.string.security_wpa
    WifiSecurity.WEP -> R.string.security_wep
    WifiSecurity.NONE -> R.string.security_none
}

private const val PASSWORD_MASK_LENGTH = 10

@Composable
private fun GenericContent(item: HistoryItem, parsed: ParsedCode, onEvent: (ResultEvent) -> Unit) {
    val c = InfTheme.colors
    InfCard {
        ResultHeader(item, parsed)
        val title = when (parsed) {
            is ParsedCode.Contact -> parsed.name.ifBlank { parsed.phone }
            is ParsedCode.Event -> parsed.title
            is ParsedCode.Sms -> parsed.number
            is ParsedCode.Email -> parsed.address
            is ParsedCode.Phone -> parsed.number
            is ParsedCode.Location -> parsed.label.ifBlank { "%.5f, %.5f".format(parsed.latitude, parsed.longitude) }
            is ParsedCode.Text -> parsed.text
            else -> item.title
        }
        Text(title, style = if (parsed is ParsedCode.Text) InfTheme.type.bodyLarge else InfTheme.type.result, color = c.ink)
        when (parsed) {
            is ParsedCode.Contact -> {
                if (parsed.phone.isNotBlank()) DetailRow(stringResource(R.string.field_phone), parsed.phone)
                if (parsed.email.isNotBlank()) DetailRow(stringResource(R.string.field_email), parsed.email)
                if (parsed.organization.isNotBlank()) DetailRow(stringResource(R.string.field_organization), parsed.organization)
            }
            is ParsedCode.Event -> {
                parsed.startMillis?.let { DetailRow(stringResource(R.string.field_starts), formatDateTime(it)) }
                parsed.endMillis?.let { DetailRow(stringResource(R.string.field_ends), formatDateTime(it)) }
                if (parsed.location.isNotBlank()) DetailRow(stringResource(R.string.field_event_location), parsed.location)
            }
            is ParsedCode.Sms -> if (parsed.body.isNotBlank()) DetailRow(stringResource(R.string.field_message), parsed.body)
            is ParsedCode.Email -> {
                if (parsed.subject.isNotBlank()) DetailRow(stringResource(R.string.field_subject), parsed.subject)
                if (parsed.body.isNotBlank()) DetailRow(stringResource(R.string.field_body), parsed.body)
            }
            is ParsedCode.Location -> DetailRow(stringResource(R.string.field_coordinates), "%.5f, %.5f".format(parsed.latitude, parsed.longitude), mono = true)
            else -> Unit
        }
    }
    primaryAction(parsed)?.let { (label, icon) ->
        InfButton(stringResource(label), { onEvent(ResultEvent.Primary) }, icon = icon)
    }
}

private fun primaryAction(parsed: ParsedCode): Pair<Int, Int>? = when (parsed) {
    is ParsedCode.Contact -> R.string.result_add_contact to R.drawable.ic_contact
    is ParsedCode.Event -> R.string.result_add_event to R.drawable.ic_event
    is ParsedCode.Sms -> R.string.result_send_message to R.drawable.ic_sms
    is ParsedCode.Email -> R.string.result_write_email to R.drawable.ic_email
    is ParsedCode.Phone -> R.string.result_call to R.drawable.ic_contact
    is ParsedCode.Location -> R.string.result_open_map to R.drawable.ic_location
    else -> null
}

private fun formatDateTime(millis: Long): String {
    val formatter = java.time.format.DateTimeFormatter.ofLocalizedDateTime(java.time.format.FormatStyle.MEDIUM, java.time.format.FormatStyle.SHORT)
    return formatter.format(java.time.Instant.ofEpochMilli(millis).atZone(java.time.ZoneId.systemDefault()))
}
