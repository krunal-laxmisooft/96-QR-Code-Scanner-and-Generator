package com.app.vv.infscan.ui.components

import androidx.annotation.DrawableRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.app.vv.infscan.R
import com.app.vv.infscan.ui.theme.InfTheme
import com.app.vv.infscan.ui.theme.Shapes
import com.app.vv.infscan.ui.theme.Sizes
import com.app.vv.infscan.ui.theme.Space

@Composable
fun InfIcon(
    @DrawableRes id: Int,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    tint: Color = InfTheme.colors.ink,
    size: Dp = Sizes.icon,
) {
    Icon(painterResource(id), contentDescription, modifier.size(size), tint)
}

@Composable
fun InfTopBar(
    title: String,
    modifier: Modifier = Modifier,
    onBack: (() -> Unit)? = null,
    backLabel: String = "",
    actions: @Composable RowScope.() -> Unit = {},
) {
    Row(
        modifier = modifier
            .statusBarsPadding()
            .fillMaxWidth()
            .height(Sizes.topBar)
            .padding(start = if (onBack != null) Space.xs else Space.screen, end = Space.xs),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Space.xs),
    ) {
        if (onBack != null) InfIconButton(R.drawable.ic_back, backLabel, onBack)
        Text(
            text = title,
            style = InfTheme.type.titleMedium,
            color = InfTheme.colors.ink,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .weight(1f)
                .semantics { heading() },
        )
        actions()
    }
}

@Composable
fun SectionHeader(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text.uppercase(),
        style = InfTheme.type.label,
        color = InfTheme.colors.inkSecondary,
        modifier = modifier
            .padding(horizontal = Space.xs)
            .semantics { heading() },
    )
}

@Composable
fun AdLabel(modifier: Modifier = Modifier) {
    val c = InfTheme.colors
    Text(
        text = stringResource(R.string.ad_label),
        style = InfTheme.type.adLabel,
        color = c.onAccent,
        modifier = modifier
            .clip(RoundedCornerShape(4.dp))
            .background(c.accent)
            .padding(horizontal = 6.dp, vertical = 2.dp),
    )
}

@Composable
fun TypeChip(@DrawableRes icon: Int, label: String, modifier: Modifier = Modifier) {
    val c = InfTheme.colors
    Row(
        modifier = modifier
            .height(32.dp)
            .clip(Shapes.pill)
            .background(c.soft)
            .padding(start = 10.dp, end = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        InfIcon(icon, null, tint = c.onSoft, size = Sizes.iconSmall)
        Text(label, style = InfTheme.type.typeChip, color = c.onSoft)
    }
}

@Composable
fun InfChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    @DrawableRes icon: Int? = null,
) {
    val c = InfTheme.colors
    val container = if (selected) c.soft else Color.Transparent
    val content = if (selected) c.onSoft else c.ink
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(10.dp),
        color = container,
        contentColor = content,
        border = if (selected) null else BorderStroke(1.dp, c.outline),
        modifier = modifier.height(Sizes.chip),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = Space.md),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            val lead = if (selected) R.drawable.ic_check else icon
            if (lead != null) InfIcon(lead, null, tint = content, size = 16.dp)
            Text(label, style = InfTheme.type.chipLabel, color = content)
        }
    }
}

@Composable
fun InfCard(
    modifier: Modifier = Modifier,
    padding: Dp = 18.dp,
    spacing: Dp = Space.md,
    content: @Composable ColumnScope.() -> Unit,
) {
    val c = InfTheme.colors
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(Shapes.cardLarge)
            .background(c.surface)
            .border(1.dp, c.outline, Shapes.cardLarge)
            .padding(padding),
        verticalArrangement = Arrangement.spacedBy(spacing),
        content = content,
    )
}

@Composable
fun IconCircle(@DrawableRes icon: Int, modifier: Modifier = Modifier, size: Dp = Sizes.avatar, tint: Color = InfTheme.colors.onSoft, background: Color = InfTheme.colors.soft) {
    Box(
        modifier = modifier
            .size(size)
            .clip(Shapes.pill)
            .background(background),
        contentAlignment = Alignment.Center,
    ) {
        InfIcon(icon, null, tint = tint, size = size / 2)
    }
}

/** A non-interactive label in the shape of a chip. */
@Composable
fun StaticChip(label: String, modifier: Modifier = Modifier) {
    val c = InfTheme.colors
    Text(
        label,
        style = InfTheme.type.chipLabel,
        color = c.ink,
        modifier = modifier
            .height(Sizes.chip)
            .border(1.dp, c.outline, RoundedCornerShape(10.dp))
            .padding(horizontal = Space.md)
            .wrapContentHeight(Alignment.CenterVertically),
    )
}
