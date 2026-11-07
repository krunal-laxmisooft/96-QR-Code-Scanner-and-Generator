package com.app.vv.infscan.ui.scan

import androidx.annotation.StringRes
import com.app.vv.infscan.domain.model.CodeKind
import com.app.vv.infscan.domain.model.DetectedCode

enum class ScanMode { SCAN, BATCH }

enum class CameraAccess { UNKNOWN, GRANTED, DENIED, PERMANENTLY_DENIED }

data class BatchEntry(val id: Long, val value: String, val kind: CodeKind)

data class ScanUiState(
    val mode: ScanMode = ScanMode.SCAN,
    val torchOn: Boolean = false,
    val zoomIndex: Int = 0,
    val access: CameraAccess = CameraAccess.UNKNOWN,
    val batch: List<BatchEntry> = emptyList(),
) {
    val zoomRatio: Float get() = ZOOM_STEPS[zoomIndex % ZOOM_STEPS.size]

    companion object {
        val ZOOM_STEPS = listOf(1f, 2f, 3f)
    }
}

sealed interface ScanEvent {
    data class CodesDetected(val codes: List<DetectedCode>) : ScanEvent
    data class ImagePicked(val uri: String) : ScanEvent
    data class ModeSelected(val mode: ScanMode) : ScanEvent
    data class AccessChanged(val access: CameraAccess) : ScanEvent
    data object ToggleTorch : ScanEvent
    data object CycleZoom : ScanEvent
    data object ShowHelp : ScanEvent
    data object ExportBatch : ScanEvent
    data object FinishBatch : ScanEvent
    data object CameraFailed : ScanEvent
}

sealed interface ScanEffect {
    data class OpenResult(val id: Long) : ScanEffect
    data class OpenUrl(val url: String) : ScanEffect
    data class Message(@StringRes val text: Int) : ScanEffect
    data class ShareCsv(val csv: String) : ScanEffect
    data object OpenHistory : ScanEffect
}
