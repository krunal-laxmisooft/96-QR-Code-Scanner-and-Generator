package com.app.vv.infscan.ui.history

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.app.vv.infscan.analytics.AnalyticsEvents
import com.app.vv.infscan.analytics.AnalyticsParams
import com.app.vv.infscan.analytics.AnalyticsTracker
import com.app.vv.infscan.data.repo.CsvExporter
import com.app.vv.infscan.data.repo.HistoryRepository
import com.app.vv.infscan.domain.model.HistoryItem
import com.app.vv.infscan.domain.model.HistoryKind
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Clock
import javax.inject.Inject

@HiltViewModel
class HistoryViewModel @Inject constructor(
    private val savedStateHandle: SavedStateHandle,
    private val history: HistoryRepository,
    private val analytics: AnalyticsTracker,
    private val clock: Clock,
) : ViewModel() {

    private data class Controls(
        val tab: HistoryKind = HistoryKind.SCANNED,
        val filter: HistoryFilter = HistoryFilter.ALL,
        val query: String = "",
        val searching: Boolean = false,
        val exportSheetCount: Int? = null,
    )

    private val controls = MutableStateFlow(restoreControls())

    private val _effects = Channel<HistoryEffect>(Channel.BUFFERED)
    val effects = _effects.receiveAsFlow()

    @OptIn(ExperimentalCoroutinesApi::class)
    val state: StateFlow<HistoryUiState> = combine(
        controls,
        controls.flatMapLatest { history.observe(it.tab) },
        history.observeCount(HistoryKind.SCANNED),
        history.observeCount(HistoryKind.CREATED),
    ) { c, items, scanned, created ->
        HistoryUiState(
            loading = false,
            tab = c.tab,
            filter = c.filter,
            query = c.query,
            searching = c.searching,
            items = items.applyControls(c.filter, c.query),
            totalCount = scanned + created,
            exportSheetCount = c.exportSheetCount,
            now = clock.millis(),
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), HistoryUiState())

    private fun restoreControls() = Controls(
        tab = savedStateHandle.get<String>(TAB_KEY)?.let { runCatching { HistoryKind.valueOf(it) }.getOrNull() } ?: HistoryKind.SCANNED,
        filter = savedStateHandle.get<String>(FILTER_KEY)?.let { runCatching { HistoryFilter.valueOf(it) }.getOrNull() } ?: HistoryFilter.ALL,
        query = savedStateHandle.get<String>(QUERY_KEY).orEmpty(),
        searching = savedStateHandle.get<Boolean>(SEARCHING_KEY) ?: false,
    )

    private fun updateControls(transform: (Controls) -> Controls) {
        controls.update(transform)
        val c = controls.value
        savedStateHandle[TAB_KEY] = c.tab.name
        savedStateHandle[FILTER_KEY] = c.filter.name
        savedStateHandle[QUERY_KEY] = c.query
        savedStateHandle[SEARCHING_KEY] = c.searching
    }

    fun onEvent(event: HistoryEvent) {
        when (event) {
            is HistoryEvent.SelectTab -> updateControls { it.copy(tab = event.tab, filter = HistoryFilter.ALL, query = "", searching = false) }
            is HistoryEvent.SelectFilter -> updateControls { it.copy(filter = event.filter) }
            is HistoryEvent.QueryChanged -> updateControls { it.copy(query = event.query) }
            HistoryEvent.ToggleSearch -> updateControls { it.copy(searching = !it.searching, query = "") }
            is HistoryEvent.ToggleFavorite -> viewModelScope.launch { history.setFavorite(event.item.id, !event.item.favorite) }
            is HistoryEvent.Delete -> viewModelScope.launch {
                history.delete(event.item.id)
                _effects.trySend(HistoryEffect.Deleted(event.item))
            }
            is HistoryEvent.Restore -> viewModelScope.launch { history.restore(event.item) }
            HistoryEvent.ExportClicked -> onExportClicked()
            HistoryEvent.ExportAll -> export(limit = null)
            HistoryEvent.ExportLatest -> export(limit = FREE_EXPORT_LIMIT)
            HistoryEvent.WatchVideoToExport -> {
                updateControls { it.copy(exportSheetCount = null) }
                _effects.trySend(HistoryEffect.ShowRewardedThenExport)
            }
            HistoryEvent.DismissExport -> updateControls { it.copy(exportSheetCount = null) }
        }
    }

    private fun onExportClicked() {
        val total = state.value.totalCount
        if (total == 0) return
        if (total <= FREE_EXPORT_LIMIT) export(limit = null) else updateControls { it.copy(exportSheetCount = total) }
    }

    private fun export(limit: Int?) {
        updateControls { it.copy(exportSheetCount = null) }
        viewModelScope.launch {
            val all = history.all()
            val rows = if (limit == null) all else all.take(limit)
            analytics.log(AnalyticsEvents.HISTORY_EXPORT, mapOf(AnalyticsParams.MODE to if (limit == null) "all" else "latest"))
            _effects.trySend(HistoryEffect.ShareCsv(CsvExporter.toCsv(rows)))
        }
    }

    companion object {
        const val FREE_EXPORT_LIMIT = 50
        private const val TAB_KEY = "tab"
        private const val FILTER_KEY = "filter"
        private const val QUERY_KEY = "query"
        private const val SEARCHING_KEY = "searching"
        private const val STOP_TIMEOUT_MS = 5_000L

        fun List<HistoryItem>.applyControls(filter: HistoryFilter, query: String): List<HistoryItem> {
            val q = query.trim()
            return filter { item ->
                (filter.kinds == null || item.codeKind in filter.kinds) &&
                    (q.isEmpty() || item.title.contains(q, ignoreCase = true) || item.raw.contains(q, ignoreCase = true))
            }
        }
    }
}
