package com.app.vv.infscan.ui.history

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.app.vv.infscan.R
import com.app.vv.infscan.domain.model.HistoryItem
import com.app.vv.infscan.domain.model.HistoryKind
import com.app.vv.infscan.ui.common.icon
import com.app.vv.infscan.ui.common.label
import com.app.vv.infscan.ui.common.relativeTimeLabel
import com.app.vv.infscan.ui.components.IconCircle
import com.app.vv.infscan.ui.components.InfButton
import com.app.vv.infscan.ui.components.InfChip
import com.app.vv.infscan.ui.components.InfIcon
import com.app.vv.infscan.ui.components.InfIconButton
import com.app.vv.infscan.ui.components.InfTextField
import com.app.vv.infscan.ui.components.InfTopBar
import com.app.vv.infscan.ui.onboarding.EmptyHistoryIllustration
import com.app.vv.infscan.ui.theme.InfTheme
import com.app.vv.infscan.ui.theme.Space

private const val NATIVE_AD_INTERVAL = 8

@Composable
fun HistoryScreen(
    state: HistoryUiState,
    onEvent: (HistoryEvent) -> Unit,
    onOpen: (HistoryItem) -> Unit,
    onScan: () -> Unit,
    banner: @Composable () -> Unit,
    nativeAd: @Composable () -> Unit,
    modifier: Modifier = Modifier,
) {
    val c = InfTheme.colors
    val fullyEmpty = state.totalCount == 0 && !state.loading
    Column(modifier.fillMaxSize().background(c.background)) {
        if (fullyEmpty) {
            InfTopBar(stringResource(R.string.tab_history))
            EmptyHistory(HistoryKind.SCANNED, onScan, Modifier.weight(1f))
        } else if (state.searching) {
            Row(Modifier.padding(horizontal = Space.xs).height(72.dp), verticalAlignment = Alignment.CenterVertically) {
                InfIconButton(R.drawable.ic_back, stringResource(R.string.history_close_search), { onEvent(HistoryEvent.ToggleSearch) })
                InfTextField(stringResource(R.string.history_search_hint), state.query, { onEvent(HistoryEvent.QueryChanged(it)) }, Modifier.weight(1f).padding(end = Space.md))
            }
        } else {
            InfTopBar(
                stringResource(R.string.tab_history),
                actions = {
                    InfIconButton(R.drawable.ic_search, stringResource(R.string.history_search), { onEvent(HistoryEvent.ToggleSearch) })
                    InfIconButton(R.drawable.ic_download, stringResource(R.string.history_export), { onEvent(HistoryEvent.ExportClicked) })
                },
            )
        }
        if (!fullyEmpty) Tabs(state.tab, onEvent)
        if (fullyEmpty) {
            Unit
        } else if (state.items.isEmpty() && !state.loading && !state.isFiltered) {
            EmptyHistory(state.tab, onScan, Modifier.weight(1f))
        } else {
            Filters(state, onEvent)
            if (state.items.isEmpty() && !state.loading) {
                Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Text(stringResource(R.string.history_no_matches), style = InfTheme.type.bodyLarge, color = c.inkSecondary)
                }
            } else {
                HistoryList(state, onEvent, onOpen, nativeAd, Modifier.weight(1f))
            }
        }
        Box(Modifier.fillMaxWidth().padding(vertical = Space.sm), contentAlignment = Alignment.Center) { banner() }
    }
}

@Composable
private fun Tabs(selected: HistoryKind, onEvent: (HistoryEvent) -> Unit) {
    val c = InfTheme.colors
    Row(Modifier.fillMaxWidth()) {
        listOf(HistoryKind.SCANNED to R.string.history_tab_scanned, HistoryKind.CREATED to R.string.history_tab_created).forEach { (kind, label) ->
            val on = kind == selected
            Column(
                Modifier.weight(1f).height(48.dp).selectable(selected = on, role = Role.Tab, onClick = { onEvent(HistoryEvent.SelectTab(kind)) }),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                    Text(stringResource(label), style = InfTheme.type.tab, color = if (on) c.primary else c.inkSecondary)
                }
                Box(Modifier.fillMaxWidth().height(3.dp).background(if (on) c.primary else androidx.compose.ui.graphics.Color.Transparent))
            }
        }
    }
    HorizontalDivider(color = c.outline)
}

@Composable
private fun Filters(state: HistoryUiState, onEvent: (HistoryEvent) -> Unit) {
    val labels = mapOf(
        HistoryFilter.ALL to R.string.filter_all, HistoryFilter.LINKS to R.string.filter_links,
        HistoryFilter.WIFI to R.string.filter_wifi, HistoryFilter.PRODUCTS to R.string.filter_products,
    )
    Row(Modifier.padding(horizontal = Space.screen, vertical = Space.md), horizontalArrangement = Arrangement.spacedBy(Space.sm)) {
        HistoryFilter.entries.forEach { filter ->
            InfChip(stringResource(labels.getValue(filter)), state.filter == filter, { onEvent(HistoryEvent.SelectFilter(filter)) })
        }
    }
}

@Composable
private fun HistoryList(state: HistoryUiState, onEvent: (HistoryEvent) -> Unit, onOpen: (HistoryItem) -> Unit, nativeAd: @Composable () -> Unit, modifier: Modifier) {
    val listState = rememberLazyListState()
    LazyColumn(modifier, state = listState) {
        items(state.items.size + state.items.size / NATIVE_AD_INTERVAL, key = { position -> rowKey(state.items, position) }) { position ->
            val itemIndex = position - position / (NATIVE_AD_INTERVAL + 1)
            if ((position + 1) % (NATIVE_AD_INTERVAL + 1) == 0) {
                Box(Modifier.padding(horizontal = Space.screen, vertical = Space.sm)) { nativeAd() }
            } else {
                HistoryRow(state.items[itemIndex], state.now, onEvent, onOpen)
            }
        }
    }
}

private fun rowKey(items: List<HistoryItem>, position: Int): Any {
    val isAd = (position + 1) % (NATIVE_AD_INTERVAL + 1) == 0
    val itemIndex = position - position / (NATIVE_AD_INTERVAL + 1)
    return if (isAd) "ad-$position" else items[itemIndex].id
}

@Composable
private fun HistoryRow(item: HistoryItem, now: Long, onEvent: (HistoryEvent) -> Unit, onOpen: (HistoryItem) -> Unit) {
    val c = InfTheme.colors
    val deleteLabel = stringResource(R.string.action_delete)
    val dismiss = rememberSwipeToDismissBoxState()
    var handled by remember { mutableStateOf(false) }
    LaunchedEffect(dismiss.currentValue) {
        if (dismiss.currentValue == SwipeToDismissBoxValue.EndToStart && !handled) {
            handled = true
            onEvent(HistoryEvent.Delete(item))
        }
    }
    SwipeToDismissBox(
        state = dismiss,
        enableDismissFromStartToEnd = false,
        backgroundContent = {
            Box(Modifier.fillMaxSize().background(c.error), contentAlignment = Alignment.CenterEnd) {
                InfIcon(R.drawable.ic_delete, deleteLabel, tint = c.onError, modifier = Modifier.padding(end = Space.screen))
            }
        },
    ) {
        Column(Modifier.background(c.background)) {
            HorizontalDivider(color = c.outline)
            Row(
                Modifier
                    .fillMaxWidth()
                    .height(64.dp)
                    .clickable(role = Role.Button) { onOpen(item) }
                    .semantics { customActions = listOf(CustomAccessibilityAction(deleteLabel) { onEvent(HistoryEvent.Delete(item)); true }) }
                    .padding(start = Space.screen, end = Space.xs),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Space.md),
            ) {
                IconCircle(item.codeKind.icon())
                Column(Modifier.weight(1f)) {
                    Text(item.title, style = InfTheme.type.rowTitle, color = c.ink, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(
                        stringResource(R.string.history_row_subtitle, stringResource(item.codeKind.label()), relativeTimeLabel(item.createdAt, now)),
                        style = InfTheme.type.bodySmall,
                        color = c.inkSecondary,
                    )
                }
                InfIconButton(
                    R.drawable.ic_star,
                    stringResource(if (item.favorite) R.string.action_unfavorite else R.string.action_favorite),
                    { onEvent(HistoryEvent.ToggleFavorite(item)) },
                    tint = if (item.favorite) c.primary else c.inkSecondary,
                )
            }
        }
    }
}

@Composable
private fun EmptyHistory(tab: HistoryKind, onScan: () -> Unit, modifier: Modifier) {
    val c = InfTheme.colors
    Column(
        modifier.fillMaxWidth().padding(horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(14.dp, Alignment.CenterVertically),
    ) {
        EmptyHistoryIllustration()
        Text(stringResource(if (tab == HistoryKind.SCANNED) R.string.history_empty_title else R.string.history_empty_created_title), style = InfTheme.type.titleLarge.copy(fontSize = InfTheme.type.titleMedium.fontSize * 1.09f), color = c.ink)
        Text(
            stringResource(if (tab == HistoryKind.SCANNED) R.string.history_empty_body else R.string.history_empty_created_body),
            style = InfTheme.type.bodyLarge.copy(lineHeight = InfTheme.type.bodyLarge.fontSize * 1.5f),
            color = c.inkSecondary,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
        )
        Box(Modifier.fillMaxWidth().padding(top = Space.sm)) { InfButton(stringResource(R.string.history_scan_a_code), onScan, icon = R.drawable.ic_scan) }
    }
}
