package com.app.vv.infscan.ui.create

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.app.vv.infscan.analytics.AnalyticsEvents
import com.app.vv.infscan.analytics.AnalyticsParams
import com.app.vv.infscan.analytics.AnalyticsTracker
import com.app.vv.infscan.data.repo.HistoryRepository
import com.app.vv.infscan.domain.create.CreateType
import com.app.vv.infscan.domain.create.FormValues
import com.app.vv.infscan.domain.create.PayloadBuilder
import com.app.vv.infscan.domain.create.PayloadResult
import com.app.vv.infscan.domain.model.CodeFormat
import com.app.vv.infscan.domain.model.HistoryKind
import com.app.vv.infscan.domain.model.NewHistoryItem
import com.app.vv.infscan.domain.model.ScanSource
import com.app.vv.infscan.domain.parser.CodeParser
import com.app.vv.infscan.ui.nav.CreateFormDest
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Clock
import java.util.concurrent.TimeUnit
import javax.inject.Inject

@HiltViewModel
class CreateFormViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val history: HistoryRepository,
    private val analytics: AnalyticsTracker,
    private val clock: Clock,
) : ViewModel() {

    private val type: CreateType = CreateType.valueOf(savedStateHandle.toRoute<CreateFormDest>().type)

    private val _state = MutableStateFlow(CreateFormUiState(type, initialValues(type)))
    val state: StateFlow<CreateFormUiState> = _state.asStateFlow()

    private val _effects = Channel<CreateFormEffect>(Channel.BUFFERED)
    val effects = _effects.receiveAsFlow()

    init {
        analytics.log(AnalyticsEvents.CREATE_START, mapOf(AnalyticsParams.TYPE to type.name.lowercase()))
    }

    fun onEvent(event: CreateFormEvent) {
        when (event) {
            is CreateFormEvent.FieldChanged -> _state.update {
                it.copy(values = it.values.copy(text = it.values.text + (event.field to event.text)), errors = it.errors - event.field)
            }
            is CreateFormEvent.SecuritySelected -> _state.update { it.copy(values = it.values.copy(security = event.security)) }
            is CreateFormEvent.HiddenChanged -> _state.update { it.copy(values = it.values.copy(hidden = event.hidden)) }
            is CreateFormEvent.StartChanged -> _state.update { s ->
                val end = s.values.endMillis?.takeIf { it >= event.millis } ?: (event.millis + HOUR_MS)
                s.copy(values = s.values.copy(startMillis = event.millis, endMillis = end), errors = emptyMap())
            }
            is CreateFormEvent.EndChanged -> _state.update { it.copy(values = it.values.copy(endMillis = event.millis), errors = emptyMap()) }
            CreateFormEvent.TogglePassword -> _state.update { it.copy(passwordVisible = !it.passwordVisible) }
            CreateFormEvent.Submit -> submit()
        }
    }

    private fun submit() {
        when (val result = PayloadBuilder.build(type, _state.value.values)) {
            is PayloadResult.Invalid -> _state.update { it.copy(errors = result.errors) }
            is PayloadResult.Valid -> viewModelScope.launch {
                val parsed = CodeParser.parse(result.payload, CodeFormat.QR)
                val id = history.add(
                    NewHistoryItem(
                        kind = HistoryKind.CREATED,
                        codeKind = parsed.kind,
                        raw = result.payload,
                        title = CodeParser.title(parsed),
                        format = CodeFormat.QR,
                        source = ScanSource.CREATED,
                        createdAt = clock.millis(),
                    ),
                )
                analytics.log(AnalyticsEvents.CREATE_COMPLETE, mapOf(AnalyticsParams.TYPE to type.name.lowercase()))
                _effects.trySend(CreateFormEffect.OpenPreview(id))
            }
        }
    }

    private fun initialValues(type: CreateType): FormValues {
        if (type != CreateType.EVENT) return FormValues()
        val now = clock.millis()
        val nextHour = (now / HOUR_MS + 1) * HOUR_MS
        return FormValues(startMillis = nextHour, endMillis = nextHour + HOUR_MS)
    }

    private companion object {
        val HOUR_MS = TimeUnit.HOURS.toMillis(1)
    }
}
