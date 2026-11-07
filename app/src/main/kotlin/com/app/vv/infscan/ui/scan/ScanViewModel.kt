package com.app.vv.infscan.ui.scan

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.app.vv.infscan.R
import com.app.vv.infscan.analytics.AnalyticsEvents
import com.app.vv.infscan.analytics.AnalyticsParams
import com.app.vv.infscan.analytics.AnalyticsTracker
import com.app.vv.infscan.data.prefs.SettingsRepository
import com.app.vv.infscan.data.repo.CsvExporter
import com.app.vv.infscan.data.repo.HistoryRepository
import com.app.vv.infscan.domain.model.DetectedCode
import com.app.vv.infscan.domain.model.HistoryKind
import com.app.vv.infscan.domain.model.NewHistoryItem
import com.app.vv.infscan.domain.model.ParsedCode
import com.app.vv.infscan.domain.model.ScanSource
import com.app.vv.infscan.domain.parser.CodeParser
import com.app.vv.infscan.domain.parser.LinkSafetyChecker
import com.app.vv.infscan.platform.BarcodeImageDecoder
import com.app.vv.infscan.platform.Haptics
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Clock
import javax.inject.Inject

@HiltViewModel
class ScanViewModel @Inject constructor(
    private val history: HistoryRepository,
    private val settings: SettingsRepository,
    private val decoder: BarcodeImageDecoder,
    private val haptics: Haptics,
    private val analytics: AnalyticsTracker,
    private val clock: Clock,
) : ViewModel() {

    private val _state = MutableStateFlow(ScanUiState())
    val state: StateFlow<ScanUiState> = _state.asStateFlow()

    private val _effects = Channel<ScanEffect>(Channel.BUFFERED)
    val effects = _effects.receiveAsFlow()

    private var lastRaw: String? = null
    private var lastAt = 0L
    private var cameraFailureReported = false

    fun onEvent(event: ScanEvent) {
        when (event) {
            is ScanEvent.CodesDetected -> onCodes(event.codes, ScanSource.CAMERA)
            is ScanEvent.ImagePicked -> viewModelScope.launch { onImage(event.uri) }
            is ScanEvent.ModeSelected -> _state.update { it.copy(mode = event.mode, batch = if (event.mode == ScanMode.SCAN) emptyList() else it.batch) }
            is ScanEvent.AccessChanged -> {
                if (event.access != _state.value.access && event.access != CameraAccess.UNKNOWN) {
                    analytics.log(AnalyticsEvents.PERMISSION_CAMERA, mapOf(AnalyticsParams.GRANTED to (event.access == CameraAccess.GRANTED)))
                }
                _state.update { it.copy(access = event.access) }
            }
            ScanEvent.ToggleTorch -> _state.update { it.copy(torchOn = !it.torchOn) }
            ScanEvent.CycleZoom -> _state.update { it.copy(zoomIndex = (it.zoomIndex + 1) % ScanUiState.ZOOM_STEPS.size) }
            ScanEvent.ShowHelp -> send(ScanEffect.Message(R.string.scan_help))
            ScanEvent.ExportBatch -> viewModelScope.launch { exportBatch() }
            ScanEvent.FinishBatch -> {
                _state.update { it.copy(mode = ScanMode.SCAN, batch = emptyList()) }
                send(ScanEffect.OpenHistory)
            }
            ScanEvent.CameraFailed -> if (!cameraFailureReported) {
                cameraFailureReported = true
                analytics.log(AnalyticsEvents.SCAN_FAIL, mapOf(AnalyticsParams.REASON to "camera_error"))
                send(ScanEffect.Message(R.string.scan_camera_error))
            }
        }
    }

    private fun onCodes(codes: List<DetectedCode>, source: ScanSource) {
        if (codes.isEmpty()) return
        viewModelScope.launch {
            if (_state.value.mode == ScanMode.BATCH) {
                codes.forEach { addToBatch(it, source) }
            } else {
                val first = codes.first()
                val now = clock.millis()
                if (first.raw == lastRaw && now - lastAt < DEBOUNCE_MS) return@launch
                lastRaw = first.raw
                lastAt = now
                openSingle(first, source)
            }
        }
    }

    private suspend fun openSingle(code: DetectedCode, source: ScanSource) {
        val id = save(code, source, batch = false)
        tick()
        val parsed = CodeParser.parse(code.raw, code.format)
        val autoOpen = parsed is ParsedCode.Url && settings.current().openLinksWithoutAsking &&
            !LinkSafetyChecker.check(parsed.url).isShortened
        send(if (autoOpen) ScanEffect.OpenUrl(parsed.url) else ScanEffect.OpenResult(id))
    }

    private suspend fun addToBatch(code: DetectedCode, source: ScanSource) {
        if (_state.value.batch.any { it.value == code.raw }) return
        val id = save(code, source, batch = true)
        val parsed = CodeParser.parse(code.raw, code.format)
        tick()
        _state.update { it.copy(batch = listOf(BatchEntry(id, code.raw, parsed.kind)) + it.batch) }
    }

    private suspend fun save(code: DetectedCode, source: ScanSource, batch: Boolean): Long {
        val parsed = CodeParser.parse(code.raw, code.format)
        val id = history.add(
            NewHistoryItem(
                kind = HistoryKind.SCANNED,
                codeKind = parsed.kind,
                raw = code.raw,
                title = CodeParser.title(parsed),
                format = code.format,
                source = source,
                createdAt = clock.millis(),
            ),
        )
        analytics.log(
            AnalyticsEvents.SCAN_SUCCESS,
            mapOf(AnalyticsParams.TYPE to parsed.kind.name.lowercase(), AnalyticsParams.SOURCE to source.name.lowercase(), AnalyticsParams.BATCH to batch),
        )
        return id
    }

    private suspend fun onImage(uri: String) {
        val codes = decoder.decode(uri)
        if (codes.isEmpty()) {
            analytics.log(AnalyticsEvents.SCAN_FAIL, mapOf(AnalyticsParams.REASON to "no_code_in_image"))
            send(ScanEffect.Message(R.string.scan_image_no_code))
            return
        }
        if (_state.value.mode == ScanMode.BATCH) codes.forEach { addToBatch(it, ScanSource.GALLERY) } else openSingle(codes.first(), ScanSource.GALLERY)
    }

    private suspend fun exportBatch() {
        val ids = _state.value.batch.map { it.id }.toSet()
        val items = history.all().filter { it.id in ids }
        if (items.isEmpty()) return
        send(ScanEffect.ShareCsv(CsvExporter.toCsv(items)))
    }

    private suspend fun tick() {
        if (settings.current().vibrateOnScan) haptics.scanTick()
    }

    private fun send(effect: ScanEffect) {
        _effects.trySend(effect)
    }

    private companion object {
        const val DEBOUNCE_MS = 1_500L
    }
}

