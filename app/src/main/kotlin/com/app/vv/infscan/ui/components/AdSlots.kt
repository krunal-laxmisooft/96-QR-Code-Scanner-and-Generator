package com.app.vv.infscan.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.app.vv.infscan.R
import com.app.vv.infscan.ui.theme.InfTheme
import com.app.vv.infscan.ui.theme.Shapes
import com.app.vv.infscan.ui.theme.Sizes
import com.app.vv.infscan.ui.theme.Space

/** Reserved space shown while a banner loads, and used for design previews. */
@Composable
fun AdBannerFrame(modifier: Modifier = Modifier, text: String? = null, content: @Composable () -> Unit = {}) {
    val c = InfTheme.colors
    val dash = PathEffect.dashPathEffect(floatArrayOf(12f, 8f))
    Box(
        modifier = modifier
            .width(Sizes.bannerWidth)
            .height(Sizes.bannerHeight)
            .clip(RoundedCornerShape(8.dp))
            .background(c.surfaceVariant)
            .drawBehind {
                drawRoundRect(c.outline, cornerRadius = CornerRadius(8.dp.toPx()), style = Stroke(1.dp.toPx(), pathEffect = dash))
            },
        contentAlignment = Alignment.Center,
    ) {
        if (text != null) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Space.sm),
            ) {
                AdLabel()
                Text(text, style = InfTheme.type.chipLabel.copy(fontSize = InfTheme.type.bodySmall.fontSize), color = c.inkSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                Text(
                    stringResource(R.string.ad_shop),
                    style = InfTheme.type.caption.copy(fontWeight = FontWeight.Bold),
                    color = c.onSoft,
                    modifier = Modifier.clip(RoundedCornerShape(12.dp)).background(c.soft).padding(horizontal = 10.dp, vertical = 6.dp),
                )
            }
        } else {
            content()
        }
    }
}

/** Native ad card layout from the design, filled with ad assets or sample copy in previews. */
@Composable
fun NativeAdCardFrame(
    advertiser: String,
    headline: String,
    body: String,
    callToAction: String,
    modifier: Modifier = Modifier,
    media: @Composable () -> Unit,
) {
    val c = InfTheme.colors
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(Shapes.card)
            .background(c.surface)
            .border(1.dp, c.outline, Shapes.card)
            .padding(Space.md),
        horizontalArrangement = Arrangement.spacedBy(Space.md),
    ) {
        Box(Modifier.size(72.dp).clip(RoundedCornerShape(12.dp)).background(c.surfaceVariant), contentAlignment = Alignment.Center) { media() }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(Space.xxs)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Space.sm)) {
                AdLabel()
                Text(advertiser, style = InfTheme.type.caption, color = c.inkSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Text(headline, style = InfTheme.type.rowTitle.copy(fontSize = 15.sp, lineHeight = 19.sp, fontWeight = FontWeight.Bold), color = c.ink, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Text(body, style = InfTheme.type.bodySmall, color = c.inkSecondary, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Box(
                Modifier
                    .padding(top = Space.xs)
                    .height(36.dp)
                    .clip(Shapes.pill)
                    .background(c.soft)
                    .padding(horizontal = Space.lg),
                contentAlignment = Alignment.Center,
            ) { Text(callToAction, style = InfTheme.type.chipLabel.copy(fontWeight = FontWeight.Bold), color = c.onSoft) }
        }
    }
}
