package com.app.vv.infscan.ui.scan

import android.provider.Settings
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.app.vv.infscan.R
import com.app.vv.infscan.ui.common.icon
import com.app.vv.infscan.ui.common.label
import com.app.vv.infscan.ui.components.ButtonKind
import com.app.vv.infscan.ui.components.InfButton
import com.app.vv.infscan.ui.components.InfIcon
import com.app.vv.infscan.ui.components.InfIconButton
import com.app.vv.infscan.ui.components.InfTopBar
import com.app.vv.infscan.ui.components.IconCircle
import com.app.vv.infscan.ui.theme.InfTheme
import com.app.vv.infscan.ui.theme.Shapes
import com.app.vv.infscan.ui.theme.Space

private val FrameSize = 214.dp
private const val FRAME_CENTER_FRACTION = 0.34f

@Composable
fun rememberReduceMotion(): Boolean {
    val resolver = LocalContext.current.contentResolver
    return remember { Settings.Global.getFloat(resolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f }
}

@Composable
fun ScanScreen(
    state: ScanUiState,
    onEvent: (ScanEvent) -> Unit,
    onRequestAccess: () -> Unit,
    onOpenSettings: () -> Unit,
    onPickImage: () -> Unit,
    modifier: Modifier = Modifier,
    camera: @Composable BoxScope.() -> Unit,
) {
    when (state.access) {
        CameraAccess.GRANTED -> CameraContent(state, onEvent, onPickImage, modifier, camera)
        else -> PermissionContent(state.access, onRequestAccess, onOpenSettings, onPickImage, modifier)
    }
}

@Composable
private fun CameraContent(
    state: ScanUiState,
    onEvent: (ScanEvent) -> Unit,
    onPickImage: () -> Unit,
    modifier: Modifier,
    camera: @Composable BoxScope.() -> Unit,
) {
    val c = InfTheme.colors
    val batch = state.mode == ScanMode.BATCH
    BoxWithFractions(
        modifier = modifier
            .fillMaxSize()
            .background(Brush.linearGradient(listOf(c.cameraTop, c.cameraMid, c.cameraBottom))),
    ) { height ->
        camera()
        Viewfinder(size = if (batch) 170.dp else FrameSize, centerY = height * if (batch) 0.28f else FRAME_CENTER_FRACTION)
        TopOverlay(state, onEvent)
        if (!batch) {
            HintPill(centerY = height * FRAME_CENTER_FRACTION + FrameSize / 2 + 28.dp)
            BottomControls(state, onEvent, onPickImage, Modifier.align(Alignment.BottomCenter))
        } else {
            BatchPanel(state, onEvent, Modifier.align(Alignment.BottomCenter))
        }
    }
}

@Composable
private fun BoxWithFractions(modifier: Modifier, content: @Composable BoxScope.(Dp) -> Unit) {
    BoxWithConstraints(modifier) { content(maxHeight) }
}

@Composable
private fun Viewfinder(size: Dp, centerY: Dp) {
    val c = InfTheme.colors
    val reduceMotion = rememberReduceMotion()
    val progress = if (reduceMotion) {
        0.5f
    } else {
        rememberInfiniteTransition(label = "scanline").animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(tween(2200, easing = LinearEasing), RepeatMode.Reverse),
            label = "scanline",
        ).value
    }
    Canvas(
        Modifier
            .fillMaxSize()
            .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen },
    ) {
        val s = size.toPx()
        val left = (this.size.width - s) / 2
        val top = centerY.toPx() - s / 2
        drawRect(c.cameraShade)
        drawRoundRect(Color.Black, Offset(left, top), Size(s, s), CornerRadius(26.dp.toPx()), blendMode = BlendMode.Clear)
        val arm = 22.dp.toPx()
        val r = 11.dp.toPx()
        val stroke = Stroke(5.dp.toPx(), cap = StrokeCap.Round)
        fun corner(x: Float, y: Float, sx: Float, sy: Float) {
            val p = Path().apply {
                moveTo(x, y + sy * (arm + r))
                lineTo(x, y + sy * r)
                quadraticTo(x, y, x + sx * r, y)
                lineTo(x + sx * (arm + r), y)
            }
            drawPath(p, c.accent, style = stroke)
        }
        val inset = 2.5.dp.toPx()
        corner(left + inset, top + inset, 1f, 1f)
        corner(left + s - inset, top + inset, -1f, 1f)
        corner(left + s - inset, top + s - inset, -1f, -1f)
        corner(left + inset, top + s - inset, 1f, -1f)
        val lineY = top + 14.dp.toPx() + progress * (s - 28.dp.toPx())
        drawRoundRect(c.accent.copy(alpha = 0.35f), Offset(left + 14.dp.toPx(), lineY - 6.dp.toPx()), Size(s - 28.dp.toPx(), 15.dp.toPx()), CornerRadius(8.dp.toPx()))
        drawRoundRect(c.accent, Offset(left + 14.dp.toPx(), lineY), Size(s - 28.dp.toPx(), 3.dp.toPx()), CornerRadius(1.5.dp.toPx()))
    }
}

@Composable
private fun TopOverlay(state: ScanUiState, onEvent: (ScanEvent) -> Unit) {
    val c = InfTheme.colors
    Row(
        Modifier.statusBarsPadding().fillMaxWidth().padding(horizontal = Space.lg, vertical = Space.md),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            Modifier.height(44.dp).clip(Shapes.pill).background(c.cameraGlass).padding(start = Space.sm, end = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Space.sm),
        ) {
            Image(painterResource(R.drawable.ic_logo_mark), null, Modifier.size(28.dp))
            Text(stringResource(R.string.app_name), style = InfTheme.type.titleSmall, color = c.onCamera)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(Space.sm)) {
            InfIconButton(R.drawable.ic_help, stringResource(R.string.scan_help_label), { onEvent(ScanEvent.ShowHelp) }, tint = c.onCamera, background = c.cameraGlass, size = 52.dp)
            InfIconButton(
                R.drawable.ic_bolt,
                stringResource(if (state.torchOn) R.string.scan_torch_off else R.string.scan_torch_on),
                { onEvent(ScanEvent.ToggleTorch) },
                tint = if (state.torchOn) c.accent else c.onCamera,
                background = c.cameraGlass,
                size = 52.dp,
            )
        }
    }
}

@Composable
private fun BoxScope.HintPill(centerY: Dp) {
    val c = InfTheme.colors
    Box(Modifier.align(Alignment.TopCenter).padding(top = centerY - 18.dp)) {
        Text(
            stringResource(R.string.scan_hint),
            style = InfTheme.type.chipLabel,
            color = c.onCamera,
            modifier = Modifier.clip(Shapes.pill).background(c.cameraGlass).padding(horizontal = Space.lg, vertical = Space.sm),
        )
    }
}

@Composable
private fun BottomControls(state: ScanUiState, onEvent: (ScanEvent) -> Unit, onPickImage: () -> Unit, modifier: Modifier) {
    val c = InfTheme.colors
    Row(
        modifier.fillMaxWidth().padding(start = Space.lg, end = Space.lg, bottom = Space.screen),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        InfIconButton(R.drawable.ic_image, stringResource(R.string.scan_from_photo), onPickImage, tint = c.onCamera, background = c.cameraGlass, size = 52.dp)
        Row(Modifier.height(52.dp).clip(Shapes.pill).background(c.cameraGlass).padding(4.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            listOf(ScanMode.SCAN to R.string.scan_mode_scan, ScanMode.BATCH to R.string.scan_mode_batch).forEach { (mode, label) ->
                val on = state.mode == mode
                Box(
                    Modifier
                        .width(96.dp)
                        .height(44.dp)
                        .clip(Shapes.pill)
                        .background(if (on) c.onCamera else Color.Transparent)
                        .selectable(selected = on, role = Role.Tab, onClick = { onEvent(ScanEvent.ModeSelected(mode)) }),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(stringResource(label), style = InfTheme.type.tab, color = if (on) c.onCameraSelected else c.onCamera)
                }
            }
        }
        InfIconButton(R.drawable.ic_plus, stringResource(R.string.scan_zoom, state.zoomRatio.toInt()), { onEvent(ScanEvent.CycleZoom) }, tint = c.onCamera, background = c.cameraGlass, size = 52.dp)
    }
}

@Composable
private fun BatchPanel(state: ScanUiState, onEvent: (ScanEvent) -> Unit, modifier: Modifier) {
    val c = InfTheme.colors
    Column(
        modifier
            .fillMaxWidth()
            .clip(Shapes.sheet)
            .background(c.surface)
            .padding(start = Space.screen, end = Space.screen, top = Space.md, bottom = Space.lg),
        verticalArrangement = Arrangement.spacedBy(Space.sm),
    ) {
        Box(Modifier.align(Alignment.CenterHorizontally).size(36.dp, 4.dp).clip(Shapes.pill).background(c.outline))
        Row(Modifier.height(48.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Space.sm)) {
            Text(stringResource(R.string.scan_batch_title, state.batch.size), style = InfTheme.type.titleSmall, color = c.ink, modifier = Modifier.weight(1f))
            if (state.batch.isNotEmpty()) {
                InfButton(stringResource(R.string.scan_batch_csv), { onEvent(ScanEvent.ExportBatch) }, kind = ButtonKind.Tonal, icon = R.drawable.ic_download, height = 40.dp, fillWidth = false, horizontalPadding = 14.dp)
            }
            InfButton(stringResource(R.string.scan_batch_done), { onEvent(ScanEvent.FinishBatch) }, height = 40.dp, fillWidth = false, horizontalPadding = 16.dp)
        }
        if (state.batch.isEmpty()) {
            Text(stringResource(R.string.scan_batch_empty), style = InfTheme.type.bodyMedium, color = c.inkSecondary, modifier = Modifier.padding(vertical = Space.sm))
        } else {
            LazyColumn(Modifier.heightIn(max = 232.dp)) {
                items(state.batch, key = { it.id }) { entry ->
                    HorizontalDivider(color = c.outline)
                    Row(Modifier.fillMaxWidth().height(56.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Space.md)) {
                        IconCircle(entry.kind.icon())
                        Column(Modifier.weight(1f)) {
                            Text(entry.value, style = InfTheme.type.mono, color = c.ink, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text(stringResource(entry.kind.label()), style = InfTheme.type.caption, color = c.inkSecondary)
                        }
                        Text("#${state.batch.size - state.batch.indexOf(entry)}", style = InfTheme.type.caption.copy(fontWeight = FontWeight.Bold), color = c.inkSecondary)
                    }
                }
            }
        }
    }
}

@Composable
private fun PermissionContent(
    access: CameraAccess,
    onRequestAccess: () -> Unit,
    onOpenSettings: () -> Unit,
    onPickImage: () -> Unit,
    modifier: Modifier,
) {
    val c = InfTheme.colors
    Column(modifier.fillMaxSize().background(c.background)) {
        InfTopBar(stringResource(R.string.tab_scan))
        Column(
            Modifier.weight(1f).padding(horizontal = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(Space.lg, Alignment.CenterVertically),
        ) {
            Box(Modifier.size(96.dp).clip(Shapes.pill).background(c.soft), contentAlignment = Alignment.Center) {
                InfIcon(R.drawable.ic_camera_off, null, tint = c.onSoft, size = 44.dp)
            }
            Text(stringResource(R.string.scan_denied_title), style = InfTheme.type.deniedTitle, color = c.ink)
            Text(
                stringResource(R.string.scan_denied_body),
                style = InfTheme.type.bodyLarge.copy(lineHeight = InfTheme.type.bodyLarge.fontSize * 1.5f),
                color = c.inkSecondary,
                textAlign = TextAlign.Center,
            )
            Column(Modifier.fillMaxWidth().padding(top = Space.md), verticalArrangement = Arrangement.spacedBy(Space.md)) {
                if (access == CameraAccess.PERMANENTLY_DENIED) {
                    InfButton(stringResource(R.string.scan_open_settings), onOpenSettings, icon = R.drawable.ic_settings)
                } else {
                    InfButton(stringResource(R.string.scan_allow_camera), onRequestAccess, icon = R.drawable.ic_scan)
                }
                InfButton(stringResource(R.string.scan_photo_instead), onPickImage, kind = ButtonKind.Tonal, icon = R.drawable.ic_image)
            }
        }
    }
}
