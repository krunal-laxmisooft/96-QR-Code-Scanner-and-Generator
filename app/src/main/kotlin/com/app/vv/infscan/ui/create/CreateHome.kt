package com.app.vv.infscan.ui.create

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.background
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.ui.text.font.FontWeight
import com.app.vv.infscan.ui.common.relativeTimeLabel
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.app.vv.infscan.R
import com.app.vv.infscan.ads.AdPlacement
import com.app.vv.infscan.ads.BannerAdSlot
import com.app.vv.infscan.data.repo.HistoryRepository
import com.app.vv.infscan.domain.create.CreateType
import com.app.vv.infscan.domain.model.HistoryItem
import com.app.vv.infscan.domain.model.HistoryKind
import com.app.vv.infscan.ui.common.icon
import com.app.vv.infscan.ui.common.label
import com.app.vv.infscan.ui.components.IconCircle
import com.app.vv.infscan.ui.components.InfTopBar
import com.app.vv.infscan.ui.components.SectionHeader
import com.app.vv.infscan.ui.theme.InfTheme
import com.app.vv.infscan.ui.theme.Shapes
import com.app.vv.infscan.ui.theme.Space
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class CreateHomeViewModel @Inject constructor(history: HistoryRepository) : ViewModel() {
    val recent: StateFlow<HistoryItem?> = history.observe(HistoryKind.CREATED)
        .map { it.firstOrNull() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), null)

    private companion object {
        const val STOP_TIMEOUT_MS = 5_000L
    }
}

@Composable
fun CreateHomeRoute(onSelect: (CreateType) -> Unit, onOpenRecent: (Long) -> Unit, viewModel: CreateHomeViewModel = hiltViewModel()) {
    val recent by viewModel.recent.collectAsStateWithLifecycle()
    CreateHomeScreen(recent, System.currentTimeMillis(), onSelect, onOpenRecent, banner = { BannerAdSlot(AdPlacement.BANNER_CREATE) })
}

@Composable
fun CreateHomeScreen(
    recent: HistoryItem?,
    now: Long,
    onSelect: (CreateType) -> Unit,
    onOpenRecent: (Long) -> Unit,
    banner: @Composable () -> Unit,
    modifier: Modifier = Modifier,
) {
    val c = InfTheme.colors
    Column(modifier.fillMaxSize().background(c.background)) {
        InfTopBar(stringResource(R.string.tab_create))
        Text(stringResource(R.string.create_subtitle), style = InfTheme.type.bodyMedium.copy(fontSize = InfTheme.type.tab.fontSize), color = c.inkSecondary, modifier = Modifier.padding(start = Space.screen, end = Space.screen, bottom = 14.dp))
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            modifier = Modifier.weight(1f),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = Space.screen),
            horizontalArrangement = Arrangement.spacedBy(Space.md),
            verticalArrangement = Arrangement.spacedBy(Space.md),
        ) {
            items(CreateType.entries, key = { it.name }) { type -> TypeTile(type, onSelect) }
            if (recent != null) {
                item(span = { GridItemSpan(maxLineSpan) }) { RecentRow(recent, now, onOpenRecent) }
            }
        }
        Box(Modifier.fillMaxWidth().padding(bottom = 10.dp, top = Space.sm), contentAlignment = Alignment.Center) { banner() }
    }
}

@Composable
private fun TypeTile(type: CreateType, onSelect: (CreateType) -> Unit) {
    val c = InfTheme.colors
    Surface(
        onClick = { onSelect(type) },
        shape = Shapes.card,
        color = c.surface,
        border = androidx.compose.foundation.BorderStroke(1.dp, c.outline),
        modifier = Modifier.height(96.dp),
    ) {
        Column(Modifier.padding(Space.md), verticalArrangement = Arrangement.SpaceBetween) {
            IconCircle(type.icon, tint = c.onSoft)
            Column {
                Text(stringResource(type.title), style = InfTheme.type.rowTitle, color = c.ink)
                Text(stringResource(type.hint), style = InfTheme.type.caption.copy(fontWeight = androidx.compose.ui.text.font.FontWeight.Normal), color = c.inkSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}

@Composable
private fun RecentRow(item: HistoryItem, now: Long, onOpen: (Long) -> Unit) {
    val c = InfTheme.colors
    Column(Modifier.padding(top = Space.sm), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        SectionHeader(stringResource(R.string.create_recent))
        Surface(
            onClick = { onOpen(item.id) },
            shape = androidx.compose.foundation.shape.RoundedCornerShape(14.dp),
            color = c.surface,
            border = androidx.compose.foundation.BorderStroke(1.dp, c.outline),
            modifier = Modifier.fillMaxWidth().height(56.dp),
        ) {
            Row(Modifier.padding(horizontal = Space.md), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Space.md)) {
                com.app.vv.infscan.ui.components.InfIcon(item.codeKind.icon(), null, tint = c.primary, size = 22.dp)
                Text(item.title, style = InfTheme.type.rowTitle.copy(fontSize = InfTheme.type.bodyLarge.fontSize, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold), color = c.ink, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                Text(
                    stringResource(R.string.history_row_subtitle, stringResource(item.codeKind.label()), relativeTimeLabel(item.createdAt, now, withTime = false)),
                    style = InfTheme.type.caption.copy(fontWeight = FontWeight.Normal),
                    color = c.inkSecondary,
                )
            }
        }
    }
}
