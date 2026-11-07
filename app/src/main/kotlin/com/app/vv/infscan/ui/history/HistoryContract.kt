package com.app.vv.infscan.ui.history

import com.app.vv.infscan.domain.model.CodeKind
import com.app.vv.infscan.domain.model.HistoryItem
import com.app.vv.infscan.domain.model.HistoryKind

enum class HistoryFilter(val kinds: Set<CodeKind>?) {
    ALL(null),
    LINKS(setOf(CodeKind.URL)),
    WIFI(setOf(CodeKind.WIFI)),
    PRODUCTS(setOf(CodeKind.PRODUCT)),
}

data class HistoryUiState(
    val loading: Boolean = true,
    val tab: HistoryKind = HistoryKind.SCANNED,
    val filter: HistoryFilter = HistoryFilter.ALL,
    val query: String = "",
    val searching: Boolean = false,
    val items: List<HistoryItem> = emptyList(),
    val totalCount: Int = 0,
    val exportSheetCount: Int? = null,
    val now: Long = 0L,
) {
    val isFiltered: Boolean get() = filter != HistoryFilter.ALL || query.isNotBlank()
}

sealed interface HistoryEvent {
    data class SelectTab(val tab: HistoryKind) : HistoryEvent
    data class SelectFilter(val filter: HistoryFilter) : HistoryEvent
    data class QueryChanged(val query: String) : HistoryEvent
    data object ToggleSearch : HistoryEvent
    data class ToggleFavorite(val item: HistoryItem) : HistoryEvent
    data class Delete(val item: HistoryItem) : HistoryEvent
    data class Restore(val item: HistoryItem) : HistoryEvent
    data object ExportClicked : HistoryEvent
    data object ExportAll : HistoryEvent
    data object ExportLatest : HistoryEvent
    data object WatchVideoToExport : HistoryEvent
    data object DismissExport : HistoryEvent
}

sealed interface HistoryEffect {
    data class Deleted(val item: HistoryItem) : HistoryEffect
    data class ShareCsv(val csv: String) : HistoryEffect
    data object ShowRewardedThenExport : HistoryEffect
}
