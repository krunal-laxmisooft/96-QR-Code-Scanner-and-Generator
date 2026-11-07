package com.app.vv.infscan.ui.history

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.app.vv.infscan.R
import com.app.vv.infscan.ads.AdPlacement
import com.app.vv.infscan.ads.BannerAdSlot
import com.app.vv.infscan.ads.LocalAdManager
import com.app.vv.infscan.ads.NativeAdSlot
import com.app.vv.infscan.ads.findActivity
import com.app.vv.infscan.platform.Sharing
import com.app.vv.infscan.ui.components.InfSnackbarHost

@Composable
fun HistoryRoute(onOpen: (Long) -> Unit, onScan: () -> Unit, viewModel: HistoryViewModel = hiltViewModel()) {
    val context = LocalContext.current
    val resources = LocalResources.current
    val ads = LocalAdManager.current
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }

    LaunchedEffect(viewModel) {
        viewModel.effects.collect { effect ->
            when (effect) {
                is HistoryEffect.Deleted -> {
                    val result = snackbar.showSnackbar(resources.getString(R.string.history_deleted), resources.getString(R.string.action_undo))
                    if (result == SnackbarResult.ActionPerformed) viewModel.onEvent(HistoryEvent.Restore(effect.item))
                }
                is HistoryEffect.ShareCsv -> Sharing.shareCsv(context, effect.csv, resources.getString(R.string.share_chooser))
                HistoryEffect.ShowRewardedThenExport -> {
                    val activity = context.findActivity()
                    if (ads != null && activity != null && ads.isRewardedReady()) {
                        ads.showRewarded(activity, onReward = { viewModel.onEvent(HistoryEvent.ExportAll) }, onFinished = {})
                    } else {
                        viewModel.onEvent(HistoryEvent.ExportAll)
                    }
                }
            }
        }
    }

    Box(Modifier.fillMaxSize()) {
        HistoryScreen(
            state = state,
            onEvent = viewModel::onEvent,
            onOpen = { onOpen(it.id) },
            onScan = onScan,
            banner = { BannerAdSlot(AdPlacement.BANNER_HISTORY) },
            nativeAd = { NativeAdSlot(AdPlacement.NATIVE_RESULT) },
        )
        InfSnackbarHost(snackbar, Modifier.align(Alignment.BottomCenter).padding(bottom = 80.dp))
    }
    state.exportSheetCount?.let { count ->
        ExportSheet(
            count = count,
            onWatch = { viewModel.onEvent(HistoryEvent.WatchVideoToExport) },
            onExportLatest = { viewModel.onEvent(HistoryEvent.ExportLatest) },
            onDismiss = { viewModel.onEvent(HistoryEvent.DismissExport) },
        )
    }
}
