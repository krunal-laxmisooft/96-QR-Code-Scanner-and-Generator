package com.app.vv.infscan.ui.components

import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SnackbarData
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.app.vv.infscan.ui.theme.InfTheme
import com.app.vv.infscan.ui.theme.Shapes
import com.app.vv.infscan.ui.theme.Space

@Composable
fun InfSnackbarHost(state: SnackbarHostState, modifier: Modifier = Modifier) {
    SnackbarHost(state, modifier) { data -> InfSnackbar(data) }
}

@Composable
private fun InfSnackbar(data: SnackbarData) {
    val c = InfTheme.colors
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = c.snackbar,
        contentColor = c.onSnackbar,
        shadowElevation = 6.dp,
        modifier = Modifier
            .padding(horizontal = Space.lg)
            .fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier
                .height(52.dp)
                .padding(start = Space.lg, end = Space.xs),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(data.visuals.message, style = InfTheme.type.rowTitle.copy(fontSize = InfTheme.type.bodyLarge.fontSize), color = c.onSnackbar, modifier = Modifier.weight(1f))
            data.visuals.actionLabel?.let { label ->
                Surface(onClick = { data.performAction() }, color = Color.Transparent, shape = Shapes.pill, modifier = Modifier.height(48.dp)) {
                    Box(Modifier.padding(horizontal = Space.md), contentAlignment = Alignment.Center) {
                        Text(label, style = InfTheme.type.button, color = c.snackbarAction)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InfDialog(
    @DrawableRes icon: Int,
    iconTint: Color,
    title: String,
    body: String,
    dismissLabel: String,
    confirmLabel: String,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    BasicAlertDialog(onDismissRequest = onDismiss) {
        InfDialogContent(icon, iconTint, title, body, dismissLabel, confirmLabel, onDismiss, onConfirm)
    }
}

@Composable
fun InfDialogContent(
    @DrawableRes icon: Int,
    iconTint: Color,
    title: String,
    body: String,
    dismissLabel: String,
    confirmLabel: String,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val c = InfTheme.colors
    Surface(modifier = modifier, shape = Shapes.dialog, color = c.surface, shadowElevation = 12.dp) {
        Column(Modifier.padding(Space.xl), verticalArrangement = Arrangement.spacedBy(Space.md)) {
            IconCircle(icon, size = 48.dp, tint = iconTint)
            Text(title, style = InfTheme.type.titleMedium.copy(fontWeight = FontWeight.ExtraBold), color = c.ink)
            Text(body, style = InfTheme.type.bodyMedium.copy(fontSize = 15.sp, lineHeight = 22.sp), color = c.inkSecondary)
            Row(Modifier.fillMaxWidth().padding(top = Space.sm), horizontalArrangement = Arrangement.spacedBy(Space.sm, Alignment.End)) {
                InfButton(dismissLabel, onDismiss, kind = ButtonKind.Text, height = 48.dp, fillWidth = false)
                InfButton(confirmLabel, onConfirm, kind = ButtonKind.Danger, height = 48.dp, fillWidth = false)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InfBottomSheet(onDismiss: () -> Unit, content: @Composable () -> Unit) {
    val c = InfTheme.colors
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        shape = Shapes.sheet,
        containerColor = c.surface,
        contentColor = c.ink,
        scrimColor = c.scrim,
        dragHandle = {
            Box(
                Modifier
                    .padding(top = Space.md, bottom = Space.sm)
                    .width(36.dp)
                    .height(4.dp)
                    .background(c.outline, RoundedCornerShape(2.dp)),
            )
        },
    ) {
        Column(Modifier.navigationBarsPadding().padding(start = Space.screen, end = Space.screen, bottom = Space.xl)) { content() }
    }
}
