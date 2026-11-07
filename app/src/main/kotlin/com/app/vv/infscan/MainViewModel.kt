package com.app.vv.infscan

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.app.vv.infscan.data.prefs.AppSettings
import com.app.vv.infscan.data.prefs.SettingsRepository
import com.app.vv.infscan.data.repo.HistoryRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class MainUiState(
    val ready: Boolean = false,
    val settings: AppSettings = AppSettings(),
    /** Ads stay off in the session where onboarding is finished, so a new user never sees one on first launch. */
    val adsAllowedThisSession: Boolean = false,
)

@HiltViewModel
class MainViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val history: HistoryRepository,
) : ViewModel() {

    private var onboardedAtStart: Boolean? = null

    val uiState: StateFlow<MainUiState> = settingsRepository.settings
        .map { settings ->
            val atStart = onboardedAtStart ?: settings.onboardingDone.also { onboardedAtStart = it }
            MainUiState(ready = true, settings = settings, adsAllowedThisSession = atStart)
        }
        .stateIn(viewModelScope, SharingStarted.Eagerly, MainUiState())

    init {
        viewModelScope.launch {
            settingsRepository.firstLaunchAt()
            history.purgeOlderThan(settingsRepository.current().retentionDays, System.currentTimeMillis())
        }
    }
}
