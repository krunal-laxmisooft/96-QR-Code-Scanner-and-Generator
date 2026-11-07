package com.app.vv.infscan.ui.create

import android.graphics.Bitmap
import androidx.annotation.StringRes
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.app.vv.infscan.R
import com.app.vv.infscan.analytics.AnalyticsEvents
import com.app.vv.infscan.analytics.AnalyticsParams
import com.app.vv.infscan.analytics.AnalyticsTracker
import com.app.vv.infscan.data.repo.HistoryRepository
import com.app.vv.infscan.domain.model.CodeFormat
import com.app.vv.infscan.domain.model.HistoryItem
import com.app.vv.infscan.domain.model.ParsedCode
import com.app.vv.infscan.domain.parser.CodeParser
import com.app.vv.infscan.platform.ImageSaver
import com.app.vv.infscan.platform.QrEncoder
import com.app.vv.infscan.platform.QrImageFactory
import com.app.vv.infscan.platform.QrMatrix
import com.app.vv.infscan.ui.nav.CreatePreviewDest
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class CreatePreviewUiState(
    val item: HistoryItem? = null,
    val parsed: ParsedCode? = null,
    val matrix: QrMatrix? = null,
    val loading: Boolean = true,
)

sealed interface CreatePreviewEvent {
    data object Save : CreatePreviewEvent
    data object Share : CreatePreviewEvent
    data object Copy : CreatePreviewEvent
    data object Undo : CreatePreviewEvent
}

sealed interface CreatePreviewEffect {
    data object Saved : CreatePreviewEffect
    data class ShareImage(val bitmap: Bitmap, val name: String) : CreatePreviewEffect
    data class CopyText(val text: String) : CreatePreviewEffect
    data class Message(@StringRes val text: Int) : CreatePreviewEffect
    data object Closed : CreatePreviewEffect
}

@HiltViewModel
class CreatePreviewViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val history: HistoryRepository,
    private val images: QrImageFactory,
    private val saver: ImageSaver,
    private val analytics: AnalyticsTracker,
) : ViewModel() {

    private val id: Long = savedStateHandle.toRoute<CreatePreviewDest>().id

    private val _state = MutableStateFlow(CreatePreviewUiState())
    val state: StateFlow<CreatePreviewUiState> = _state.asStateFlow()

    private val _effects = Channel<CreatePreviewEffect>(Channel.BUFFERED)
    val effects = _effects.receiveAsFlow()

    init {
        viewModelScope.launch {
            val item = history.get(id)
            _state.value = if (item == null) {
                CreatePreviewUiState(loading = false)
            } else {
                CreatePreviewUiState(item, CodeParser.parse(item.raw, CodeFormat.QR), QrEncoder.matrix(item.raw), loading = false)
            }
        }
    }

    fun onEvent(event: CreatePreviewEvent) {
        val item = _state.value.item ?: return
        when (event) {
            CreatePreviewEvent.Save -> viewModelScope.launch {
                val ok = saver.savePng(images.create(item.raw, IMAGE_SIZE_PX), fileName(item))
                _effects.trySend(if (ok) CreatePreviewEffect.Saved else CreatePreviewEffect.Message(R.string.create_save_failed))
            }
            CreatePreviewEvent.Share -> {
                analytics.log(AnalyticsEvents.CREATE_SHARE, mapOf(AnalyticsParams.TYPE to item.codeKind.name.lowercase()))
                _effects.trySend(CreatePreviewEffect.ShareImage(images.create(item.raw, IMAGE_SIZE_PX), fileName(item)))
            }
            CreatePreviewEvent.Copy -> _effects.trySend(CreatePreviewEffect.CopyText(item.raw))
            CreatePreviewEvent.Undo -> viewModelScope.launch {
                history.delete(item.id)
                _state.update { CreatePreviewUiState(loading = false) }
                _effects.trySend(CreatePreviewEffect.Closed)
            }
        }
    }

    private fun fileName(item: HistoryItem): String = "infscan-${item.codeKind.name.lowercase()}-${item.id}"

    companion object {
        const val IMAGE_SIZE_PX = 1024
    }
}
