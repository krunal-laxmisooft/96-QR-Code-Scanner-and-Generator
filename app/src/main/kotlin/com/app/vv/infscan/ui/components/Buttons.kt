package com.app.vv.infscan.ui.components

import androidx.annotation.DrawableRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.app.vv.infscan.ui.theme.InfTheme
import com.app.vv.infscan.ui.theme.Shapes
import com.app.vv.infscan.ui.theme.Sizes
import com.app.vv.infscan.ui.theme.Space

enum class ButtonKind { Primary, Tonal, Outline, Text, Danger }

@Composable
fun InfButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    kind: ButtonKind = ButtonKind.Primary,
    @DrawableRes icon: Int? = null,
    enabled: Boolean = true,
    height: Dp = Sizes.button,
    fillWidth: Boolean = true,
) {
    val c = InfTheme.colors
    val container: Color
    val content: Color
    var border: BorderStroke? = null
    when {
        !enabled -> { container = c.surfaceVariant; content = c.inkSecondary }
        kind == ButtonKind.Primary -> { container = c.primary; content = c.onPrimary }
        kind == ButtonKind.Tonal -> { container = c.soft; content = c.onSoft }
        kind == ButtonKind.Outline -> {
            container = Color.Transparent; content = c.primary
            border = BorderStroke(1.5.dp, c.primary)
        }
        kind == ButtonKind.Text -> { container = Color.Transparent; content = c.primary }
        else -> { container = c.error; content = c.onError }
    }
    Surface(
        onClick = onClick,
        enabled = enabled,
        shape = Shapes.pill,
        color = container,
        contentColor = content,
        border = border,
        modifier = modifier
            .then(if (fillWidth) Modifier.fillMaxWidth() else Modifier)
            .height(height)
            .sizeIn(minWidth = Sizes.touch),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = if (fillWidth) 0.dp else Space.xl),
            horizontalArrangement = Arrangement.spacedBy(Space.sm, Alignment.CenterHorizontally),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (icon != null) InfIcon(icon, null, tint = content, size = 22.dp)
            Text(label, style = InfTheme.type.button, color = content)
        }
    }
}

/** Square tonal action used under a result: icon over label. */
@Composable
fun ActionTile(
    label: String,
    @DrawableRes icon: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val c = InfTheme.colors
    Surface(
        onClick = onClick,
        shape = Shapes.card,
        color = c.soft,
        contentColor = c.onSoft,
        modifier = modifier.height(64.dp),
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(Space.xxs, Alignment.CenterVertically),
        ) {
            InfIcon(icon, null, tint = c.onSoft, size = 22.dp)
            Text(label, style = InfTheme.type.actionLabel, color = c.onSoft)
        }
    }
}

@Composable
fun InfIconButton(
    @DrawableRes icon: Int,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    tint: Color = InfTheme.colors.ink,
    background: Color = Color.Transparent,
    size: Dp = Sizes.touch,
) {
    Surface(
        onClick = onClick,
        shape = Shapes.pill,
        color = background,
        contentColor = tint,
        modifier = modifier.size(size).semantics { role = Role.Button },
    ) {
        Box(contentAlignment = Alignment.Center) {
            InfIcon(icon, contentDescription, tint = tint)
        }
    }
}
