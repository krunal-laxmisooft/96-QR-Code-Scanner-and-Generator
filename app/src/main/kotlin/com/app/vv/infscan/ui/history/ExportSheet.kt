package com.app.vv.infscan.ui.history

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.app.vv.infscan.R
import com.app.vv.infscan.ui.components.ButtonKind
import com.app.vv.infscan.ui.components.IconCircle
import com.app.vv.infscan.ui.components.InfBottomSheet
import com.app.vv.infscan.ui.components.InfButton
import com.app.vv.infscan.ui.theme.InfTheme
import com.app.vv.infscan.ui.theme.Space

@Composable
fun ExportSheet(count: Int, onWatch: () -> Unit, onExportLatest: () -> Unit, onDismiss: () -> Unit) {
    InfBottomSheet(onDismiss) { ExportSheetContent(count, onWatch, onExportLatest, onDismiss) }
}

@Composable
fun ExportSheetContent(count: Int, onWatch: () -> Unit, onExportLatest: () -> Unit, onDismiss: () -> Unit) {
    val c = InfTheme.colors
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        IconCircle(R.drawable.ic_download, size = 56.dp, tint = c.onSoft)
        Text(stringResource(R.string.export_title), style = InfTheme.type.titleMedium.copy(fontWeight = FontWeight.ExtraBold), color = c.ink)
        Text(
            pluralStringResource(R.plurals.export_body, count, count, HistoryViewModel.FREE_EXPORT_LIMIT),
            style = InfTheme.type.bodyLarge.copy(lineHeight = InfTheme.type.bodyLarge.fontSize * 1.5f),
            color = c.inkSecondary,
        )
        Column(Modifier.padding(top = Space.xs), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            InfButton(stringResource(R.string.export_watch), onWatch, icon = R.drawable.ic_video)
            InfButton(stringResource(R.string.export_latest, HistoryViewModel.FREE_EXPORT_LIMIT), onExportLatest, kind = ButtonKind.Outline, icon = R.drawable.ic_download)
            InfButton(stringResource(R.string.action_not_now), onDismiss, kind = ButtonKind.Text, height = 44.dp)
        }
        Text(stringResource(R.string.export_optional), style = InfTheme.type.caption.copy(fontWeight = FontWeight.Normal), color = c.inkSecondary, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
    }
}
