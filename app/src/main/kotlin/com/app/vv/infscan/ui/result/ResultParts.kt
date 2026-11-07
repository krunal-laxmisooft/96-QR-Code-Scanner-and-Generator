package com.app.vv.infscan.ui.result

import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.app.vv.infscan.R
import com.app.vv.infscan.ui.components.ActionTile
import com.app.vv.infscan.ui.components.InfIcon
import com.app.vv.infscan.ui.theme.InfTheme
import com.app.vv.infscan.ui.theme.Shapes
import com.app.vv.infscan.ui.theme.Space

enum class NoticeTone { Success, Warning }

@Composable
fun NoticeBanner(@DrawableRes icon: Int, title: String, body: String, tone: NoticeTone, modifier: Modifier = Modifier) {
    val c = InfTheme.colors
    val container = if (tone == NoticeTone.Success) c.successSoft else c.warningSoft
    val content = if (tone == NoticeTone.Success) c.onSuccessSoft else c.onWarningSoft
    Row(
        modifier.fillMaxWidth().clip(Shapes.input).background(container).padding(Space.md),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        InfIcon(icon, null, tint = content, size = 22.dp)
        Column(verticalArrangement = Arrangement.spacedBy(Space.xxs)) {
            Text(title, style = InfTheme.type.rowTitle.copy(fontSize = InfTheme.type.tab.fontSize), color = content)
            Text(body, style = InfTheme.type.bodySmall, color = content)
        }
    }
}

@Composable
fun DetailRow(label: String, value: String, modifier: Modifier = Modifier, mono: Boolean = false, trailing: @Composable (() -> Unit)? = null) {
    val c = InfTheme.colors
    Column(modifier) {
        HorizontalDivider(color = c.outline)
        Row(Modifier.fillMaxWidth().heightIn(min = 52.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label, style = InfTheme.type.caption.copy(fontSize = InfTheme.type.bodyMedium.fontSize), color = c.inkSecondary)
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Space.sm)) {
                val style: TextStyle = if (mono) InfTheme.type.mono.copy(fontSize = InfTheme.type.bodyLarge.fontSize) else InfTheme.type.rowTitle
                Text(value, style = style, color = c.ink, maxLines = 3, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f, fill = false))
                trailing?.invoke()
            }
        }
    }
}

@Composable
fun ActionRow(favorite: Boolean, onCopy: () -> Unit, onShare: () -> Unit, onSave: () -> Unit, modifier: Modifier = Modifier) {
    Row(modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        ActionTile(stringResource(R.string.action_copy), R.drawable.ic_copy, onCopy, Modifier.weight(1f))
        ActionTile(stringResource(R.string.action_share), R.drawable.ic_share, onShare, Modifier.weight(1f))
        ActionTile(stringResource(if (favorite) R.string.action_saved else R.string.action_save), R.drawable.ic_star, onSave, Modifier.weight(1f))
    }
}
