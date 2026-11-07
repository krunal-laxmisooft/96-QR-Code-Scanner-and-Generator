package com.app.vv.infscan.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.app.vv.infscan.ads.AdManager
import com.app.vv.infscan.data.prefs.AppSettings
import com.app.vv.infscan.data.prefs.SettingsRepository
import com.app.vv.infscan.data.prefs.ThemeMode
import com.app.vv.infscan.data.repo.HistoryRepository
import com.app.vv.infscan.domain.model.HistoryKind
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SettingsUiState(
    val settings: AppSettings = AppSettings(),
    val privacyOptionsRequired: Boolean = false,
    val scannedCount: Int = 0,
    val createdCount: Int = 0,
    val confirmClear: Boolean = false,
    val choosingRetention: Boolean = false,
)

sealed interface SettingsEvent {
    data class Theme(val mode: ThemeMode) : SettingsEvent
    data class Vibrate(val enabled: Boolean) : SettingsEvent
    data class OpenLinks(val enabled: Boolean) : SettingsEvent
    data class Retention(val days: Int) : SettingsEvent
    data object ChooseRetention : SettingsEvent
    data object DismissRetention : SettingsEvent
    data object ClearRequested : SettingsEvent
    data object ClearConfirmed : SettingsEvent
    data object ClearDismissed : SettingsEvent
}

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settings: SettingsRepository,
    private val history: HistoryRepository,
    ads: AdManager,
) : ViewModel() {

    private val dialogs = MutableStateFlow(SettingsUiState())

    val state: StateFlow<SettingsUiState> = combine(
        settings.settings,
        ads.privacyOptionsRequired,
        history.observeCount(HistoryKind.SCANNED),
        history.observeCount(HistoryKind.CREATED),
        dialogs,
    ) { s, privacy, scanned, created, d ->
        d.copy(settings = s, privacyOptionsRequired = privacy, scannedCount = scanned, createdCount = created)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), SettingsUiState())

    fun onEvent(event: SettingsEvent) {
        when (event) {
            is SettingsEvent.Theme -> viewModelScope.launch { settings.setThemeMode(event.mode) }
            is SettingsEvent.Vibrate -> viewModelScope.launch { settings.setVibrateOnScan(event.enabled) }
            is SettingsEvent.OpenLinks -> viewModelScope.launch { settings.setOpenLinksWithoutAsking(event.enabled) }
            is SettingsEvent.Retention -> viewModelScope.launch {
                settings.setRetentionDays(event.days)
                history.purgeOlderThan(event.days, System.currentTimeMillis())
                dialogs.update { it.copy(choosingRetention = false) }
            }
            SettingsEvent.ChooseRetention -> dialogs.update { it.copy(choosingRetention = true) }
            SettingsEvent.DismissRetention -> dialogs.update { it.copy(choosingRetention = false) }
            SettingsEvent.ClearRequested -> dialogs.update { it.copy(confirmClear = true) }
            SettingsEvent.ClearDismissed -> dialogs.update { it.copy(confirmClear = false) }
            SettingsEvent.ClearConfirmed -> viewModelScope.launch {
                history.clear()
                dialogs.update { it.copy(confirmClear = false) }
            }
        }
    }

    private companion object {
        const val STOP_TIMEOUT_MS = 5_000L
    }
}
