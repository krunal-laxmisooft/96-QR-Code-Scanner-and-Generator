package com.app.vv.infscan.ui.create

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.app.vv.infscan.R
import com.app.vv.infscan.ads.AdPlacement
import com.app.vv.infscan.ads.LocalAdManager
import com.app.vv.infscan.ads.findActivity
import com.app.vv.infscan.domain.model.ParsedCode
import com.app.vv.infscan.platform.QrMatrix
import com.app.vv.infscan.platform.Sharing
import com.app.vv.infscan.ui.components.ButtonKind
import com.app.vv.infscan.ui.components.InfButton
import com.app.vv.infscan.ui.components.InfSnackbarHost
import com.app.vv.infscan.ui.components.InfTopBar
import com.app.vv.infscan.ui.components.StaticChip
import com.app.vv.infscan.ui.result.label
import com.app.vv.infscan.ui.theme.InfTheme
import com.app.vv.infscan.ui.theme.Shapes
import com.app.vv.infscan.ui.theme.Space

private const val QUIET_ZONE_MODULES = 2

@Composable
fun CreatePreviewRoute(onBack: () -> Unit, viewModel: CreatePreviewViewModel = hiltViewModel()) {
    val context = LocalContext.current
    val resources = LocalResources.current
    val adManager = LocalAdManager.current
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    val storage = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) viewModel.onEvent(CreatePreviewEvent.Save)
    }

    LaunchedEffect(state.item?.id) {
        if (state.item == null) return@LaunchedEffect
        val result = snackbar.showSnackbar(resources.getString(R.string.create_saved_history), resources.getString(R.string.action_undo))
        if (result == SnackbarResult.ActionPerformed) viewModel.onEvent(CreatePreviewEvent.Undo)
    }
    LaunchedEffect(viewModel) {
        viewModel.effects.collect { effect ->
            when (effect) {
                CreatePreviewEffect.Saved -> {
                    snackbar.showSnackbar(resources.getString(R.string.create_saved_gallery))
                    val activity = context.findActivity()
                    if (activity != null) adManager?.showInterstitial(activity, AdPlacement.INTERSTITIAL_CREATE_DONE) {}
                }
                is CreatePreviewEffect.ShareImage -> Sharing.shareImage(context, effect.bitmap, effect.name, resources.getString(R.string.share_chooser))
                is CreatePreviewEffect.CopyText -> {
                    Sharing.copy(context, resources.getString(R.string.app_name), effect.text)
                    snackbar.showSnackbar(resources.getString(R.string.result_copied))
                }
                is CreatePreviewEffect.Message -> snackbar.showSnackbar(resources.getString(effect.text))
                CreatePreviewEffect.Closed -> onBack()
            }
        }
    }

    Box(Modifier.fillMaxSize()) {
        CreatePreviewScreen(
            state = state,
            onEvent = { event ->
                val needsLegacyPermission = event == CreatePreviewEvent.Save && Build.VERSION.SDK_INT < Build.VERSION_CODES.Q &&
                    ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED
                if (needsLegacyPermission) storage.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE) else viewModel.onEvent(event)
            },
            onBack = onBack,
        )
        InfSnackbarHost(snackbar, Modifier.align(Alignment.BottomCenter).padding(bottom = Space.screen))
    }
}

@Composable
fun CreatePreviewScreen(state: CreatePreviewUiState, onEvent: (CreatePreviewEvent) -> Unit, onBack: () -> Unit, modifier: Modifier = Modifier) {
    val c = InfTheme.colors
    Column(modifier.fillMaxSize().background(c.background)) {
        InfTopBar(stringResource(R.string.create_preview_title), onBack = onBack, backLabel = stringResource(R.string.action_back))
        val item = state.item
        val matrix = state.matrix
        if (item == null || matrix == null) return@Column
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = Space.screen), verticalArrangement = Arrangement.spacedBy(Space.lg)) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .padding(top = Space.sm)
                    .clip(Shapes.hero)
                    .background(c.surface)
                    .border(1.dp, c.outline, Shapes.hero)
                    .padding(Space.xl),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Box(Modifier.clip(Shapes.card).background(c.qrBackground).border(1.dp, c.outline, Shapes.card).padding(10.dp)) {
                    QrCanvas(matrix, Modifier.size(232.dp))
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(Space.xxs)) {
                    Text(item.title, style = InfTheme.type.titleSmall.copy(fontSize = InfTheme.type.titleMedium.fontSize * 0.91f), color = c.ink)
                    Text(caption(state.parsed), style = InfTheme.type.bodyMedium, color = c.inkSecondary)
                }
                StaticChip(stringResource(R.string.create_image_spec, CreatePreviewViewModel.IMAGE_SIZE_PX, CreatePreviewViewModel.IMAGE_SIZE_PX))
            }
            Column(verticalArrangement = Arrangement.spacedBy(Space.md)) {
                InfButton(stringResource(R.string.create_save_gallery), { onEvent(CreatePreviewEvent.Save) }, icon = R.drawable.ic_download)
                Row(horizontalArrangement = Arrangement.spacedBy(Space.md)) {
                    InfButton(stringResource(R.string.action_share), { onEvent(CreatePreviewEvent.Share) }, Modifier.weight(1f), kind = ButtonKind.Tonal, icon = R.drawable.ic_share)
                    InfButton(stringResource(R.string.action_copy), { onEvent(CreatePreviewEvent.Copy) }, Modifier.weight(1f), kind = ButtonKind.Tonal, icon = R.drawable.ic_copy)
                }
            }
        }
    }
}

@Composable
private fun caption(parsed: ParsedCode?): String = when (parsed) {
    is ParsedCode.Wifi -> stringResource(R.string.create_caption_wifi, stringResource(parsed.security.label()))
    is ParsedCode.Url -> stringResource(R.string.create_caption_url)
    is ParsedCode.Contact -> stringResource(R.string.create_caption_contact)
    is ParsedCode.Sms -> stringResource(R.string.create_caption_message)
    is ParsedCode.Email -> stringResource(R.string.create_caption_email)
    is ParsedCode.Event -> stringResource(R.string.create_caption_event)
    is ParsedCode.Location -> stringResource(R.string.create_caption_location)
    else -> stringResource(R.string.create_caption_text)
}

@Composable
fun QrCanvas(matrix: QrMatrix, modifier: Modifier = Modifier) {
    val c = InfTheme.colors
    val description = stringResource(R.string.create_qr_description)
    Canvas(modifier.semantics { contentDescription = description }) {
        val modules = matrix.size + 2 * QUIET_ZONE_MODULES
        val cell = size.width / modules
        drawRect(c.qrBackground, Offset.Zero, size)
        for (y in 0 until matrix.size) {
            for (x in 0 until matrix.size) {
                if (matrix[x, y]) {
                    drawRect(c.qrForeground, Offset((x + QUIET_ZONE_MODULES) * cell, (y + QUIET_ZONE_MODULES) * cell), Size(cell + 0.5f, cell + 0.5f))
                }
            }
        }
    }
}
