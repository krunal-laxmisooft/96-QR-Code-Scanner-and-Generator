package com.app.vv.infscan.ui.scan

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import androidx.core.net.toUri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.app.vv.infscan.platform.Sharing
import com.app.vv.infscan.ads.findActivity
import com.app.vv.infscan.ui.components.InfSnackbarHost
import com.app.vv.infscan.R

@Composable
fun ScanRoute(
    onOpenResult: (Long) -> Unit,
    onOpenHistory: () -> Unit,
    viewModel: ScanViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val resources = LocalResources.current
    val activity = context.findActivity()
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    var requested by remember { mutableStateOf(false) }

    fun currentAccess(): CameraAccess {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) return CameraAccess.GRANTED
        if (!requested) return CameraAccess.UNKNOWN
        val canAskAgain = activity != null && ActivityCompat.shouldShowRequestPermissionRationale(activity, Manifest.permission.CAMERA)
        return if (canAskAgain) CameraAccess.DENIED else CameraAccess.PERMANENTLY_DENIED
    }

    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        requested = true
        viewModel.onEvent(ScanEvent.AccessChanged(currentAccess()))
    }
    val photoLauncher = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri: Uri? ->
        if (uri != null) viewModel.onEvent(ScanEvent.ImagePicked(uri.toString()))
    }

    LifecycleResumeEffect(Unit) {
        val access = currentAccess()
        viewModel.onEvent(ScanEvent.AccessChanged(access))
        if (access == CameraAccess.UNKNOWN) permissionLauncher.launch(Manifest.permission.CAMERA)
        onPauseOrDispose { }
    }

    LaunchedEffect(viewModel) {
        viewModel.effects.collect { effect ->
            when (effect) {
                is ScanEffect.OpenResult -> onOpenResult(effect.id)
                is ScanEffect.OpenUrl -> Sharing.openUrl(context, effect.url)
                is ScanEffect.Message -> snackbar.showSnackbar(resources.getString(effect.text))
                is ScanEffect.ShareCsv -> Sharing.shareCsv(context, effect.csv, resources.getString(R.string.share_chooser))
                ScanEffect.OpenHistory -> onOpenHistory()
            }
        }
    }

    Box(Modifier.fillMaxSize()) {
        ScanScreen(
            state = state,
            onEvent = viewModel::onEvent,
            onRequestAccess = { permissionLauncher.launch(Manifest.permission.CAMERA) },
            onOpenSettings = {
                context.startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, "package:${context.packageName}".toUri()))
            },
            onPickImage = { photoLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
        ) {
            CameraScanner(
                torchOn = state.torchOn,
                zoomRatio = state.zoomRatio,
                onCodes = { viewModel.onEvent(ScanEvent.CodesDetected(it)) },
                onFailure = { viewModel.onEvent(ScanEvent.CameraFailed) },
                modifier = Modifier.fillMaxSize(),
            )
        }
        InfSnackbarHost(snackbar, Modifier.align(Alignment.BottomCenter).padding(bottom = 96.dp))
    }
}
