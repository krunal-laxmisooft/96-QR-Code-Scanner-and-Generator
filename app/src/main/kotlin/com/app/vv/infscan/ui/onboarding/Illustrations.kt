package com.app.vv.infscan.ui.onboarding

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.app.vv.infscan.R
import com.app.vv.infscan.platform.QrEncoder
import com.app.vv.infscan.ui.components.InfIcon
import com.app.vv.infscan.ui.theme.InfTheme
import kotlin.random.Random

private val IllustrationSize = 300.dp to 270.dp

private fun DrawScope.rr(x: Float, y: Float, w: Float, h: Float, r: Float, fill: Color, stroke: Color? = null, strokeWidth: Float = 0f) {
    val d = density
    drawRoundRect(fill, Offset(x * d, y * d), Size(w * d, h * d), CornerRadius(r * d))
    if (stroke != null) drawRoundRect(stroke, Offset(x * d, y * d), Size(w * d, h * d), CornerRadius(r * d), style = Stroke(strokeWidth * d))
}

private fun DrawScope.qr(content: String, x: Float, y: Float, side: Float, fg: Color, bg: Color) {
    val m = QrEncoder.matrix(content)
    val d = density
    drawRect(bg, Offset(x * d, y * d), Size(side * d, side * d))
    val inner = side - 8f
    val cell = inner / m.size
    for (row in 0 until m.size) for (col in 0 until m.size) {
        if (m[col, row]) drawRect(fg, Offset((x + 4 + col * cell) * d, (y + 4 + row * cell) * d), Size(cell * d + 0.5f, cell * d + 0.5f))
    }
}

@Composable
private fun Floating(x: Float, y: Float, w: Float, h: Float, @DrawableRes icon: Int, bg: Color, tint: Color, border: Color, rounded: Float) {
    Canvas(Modifier.offset(x.dp, y.dp).size(w.dp, h.dp)) {
        drawRoundRect(bg, size = size, cornerRadius = CornerRadius(rounded.dp.toPx()))
        drawRoundRect(border, size = size, cornerRadius = CornerRadius(rounded.dp.toPx()), style = Stroke(2.dp.toPx()))
    }
    Box(Modifier.offset((x + w / 2 - 12).dp, (y + h / 2 - 12).dp)) { InfIcon(icon, null, tint = tint) }
}

@Composable
fun ScanIllustration(modifier: Modifier = Modifier) {
    val c = InfTheme.colors
    Box(modifier.size(IllustrationSize.first, IllustrationSize.second)) {
        Canvas(Modifier.size(IllustrationSize.first, IllustrationSize.second)) {
            val d = density
            drawCircle(c.soft, 118 * d, Offset(150 * d, 135 * d))
            rr(96f, 22f, 108f, 226f, 20f, c.surface, c.ink, 3f)
            rr(106f, 40f, 88f, 172f, 10f, c.cameraMid)
            qr("InfScan", 122f, 78f, 56f, c.paper, c.cameraMid)
            val corner = 14f
            listOf(114f to 70f to 1, 186f to 70f to 2, 186f to 150f to 3, 114f to 150f to 4).forEach { (pos, which) ->
                val (cx, cy) = pos
                val sx = if (which == 2 || which == 3) -1f else 1f
                val sy = if (which == 3 || which == 4) -1f else 1f
                val p = androidx.compose.ui.graphics.Path().apply {
                    moveTo(cx * d, (cy + sy * corner) * d)
                    lineTo(cx * d, (cy + sy * 4f) * d)
                    quadraticTo(cx * d, cy * d, (cx + sx * 4f) * d, cy * d)
                    lineTo((cx + sx * corner) * d, cy * d)
                }
                drawPath(p, c.accent, style = Stroke(4 * d, cap = StrokeCap.Round))
            }
            rr(120f, 108f, 60f, 3f, 1.5f, c.accent)
            rr(131f, 232f, 38f, 5f, 2.5f, c.outline)
        }
        Floating(22f, 62f, 62f, 44f, R.drawable.ic_link, c.surface, c.primary, c.outline, 14f)
        Floating(218f, 132f, 62f, 44f, R.drawable.ic_wifi, c.surface, c.primary, c.outline, 14f)
        Floating(30f, 170f, 62f, 44f, R.drawable.ic_tag, c.surface, c.primary, c.outline, 14f)
    }
}

@Composable
fun CreateIllustration(modifier: Modifier = Modifier) {
    val c = InfTheme.colors
    Box(modifier.size(IllustrationSize.first, IllustrationSize.second)) {
        Canvas(Modifier.size(IllustrationSize.first, IllustrationSize.second)) {
            val d = density
            drawCircle(c.soft, 118 * d, Offset(150 * d, 135 * d))
            rr(92f, 55f, 116f, 140f, 18f, c.surface, c.ink, 3f)
            qr("InfScan create", 108f, 71f, 84f, c.ink, c.surface)
            rr(108f, 172f, 84f, 6f, 3f, c.outline)
        }
        listOf(
            Triple(52f to 62f, R.drawable.ic_wifi, false), Triple(248f to 70f, R.drawable.ic_contact, true),
            Triple(50f to 190f, R.drawable.ic_event, true), Triple(250f to 192f, R.drawable.ic_location, false),
        ).forEach { (pos, icon, accent) ->
            val (cx, cy) = pos
            Canvas(Modifier.offset((cx - 30).dp, (cy - 30).dp).size(60.dp)) {
                drawCircle(if (accent) c.accent else c.surface, 30.dp.toPx())
                drawCircle(c.outline, 30.dp.toPx(), style = Stroke(2.dp.toPx()))
            }
            Box(Modifier.offset((cx - 14).dp, (cy - 14).dp)) { InfIcon(icon, null, tint = if (accent) c.onAccent else c.primary, size = 28.dp) }
        }
    }
}

@Composable
fun PriceIllustration(modifier: Modifier = Modifier) {
    val c = InfTheme.colors
    val bars = remember { Random(9).let { r -> List(30) { 1 + r.nextInt(3) } } }
    Box(modifier.size(IllustrationSize.first, IllustrationSize.second)) {
        Canvas(Modifier.size(IllustrationSize.first, IllustrationSize.second)) {
            val d = density
            drawCircle(c.soft, 118 * d, Offset(150 * d, 135 * d))
            rr(28f, 72f, 104f, 126f, 14f, c.surface, c.ink, 3f)
            val unit = 64f / (bars.sum() * 2)
            var x = 48f
            bars.forEach { w ->
                drawRect(c.ink, Offset(x * d, 94f * d), Size(w * unit * d, 34f * d))
                x += w * unit * 2
            }
            drawLine(c.inkSecondary, Offset(132 * d, 130 * d), Offset(142 * d, 130 * d), 2 * d, StrokeCap.Round)
            rr(40f, 160f, 80f, 8f, 4f, c.outline)
            rr(40f, 176f, 52f, 8f, 4f, c.outline)
            listOf(70f to true, 120f to false, 170f to false).forEach { (y, best) ->
                rr(146f, y, 132f, 38f, 12f, if (best) c.accent else c.surface, c.outline, 2f)
            }
        }
        Text(stringResource(R.string.onboarding_barcode_digits), color = c.ink, style = InfTheme.type.monoSmall, modifier = Modifier.offset(48.dp, 132.dp))
        listOf(
            Triple(70f, Triple(R.string.onboarding_store_1, R.string.onboarding_price_1, true), 0),
            Triple(120f, Triple(R.string.onboarding_store_2, R.string.onboarding_price_2, false), 0),
            Triple(170f, Triple(R.string.onboarding_store_3, R.string.onboarding_price_3, false), 0),
        ).forEach { (y, info, _) ->
            val (name, price, best) = info
            val tint = if (best) c.onAccent else c.ink
            Text(stringResource(name), color = tint, style = InfTheme.type.actionLabel, modifier = Modifier.offset(158.dp, (y + 11).dp))
            Text(stringResource(price), color = tint, style = InfTheme.type.monoSmall, modifier = Modifier.offset(228.dp, (y + 10).dp))
        }
    }
}

@Composable
fun EmptyHistoryIllustration(modifier: Modifier = Modifier) {
    val c = InfTheme.colors
    Canvas(modifier.size(200.dp, 160.dp)) {
        val d = density
        drawCircle(c.soft, 70 * d, Offset(100 * d, 80 * d))
        rr(62f, 38f, 76f, 84f, 14f, c.surface, c.ink, 3f)
        qr("InfScan empty", 74f, 50f, 52f, c.inkSecondary, c.surface)
        rr(72f, 106f, 56f, 5f, 2.5f, c.outline)
        listOf(Triple(58f, 30f, 1f), Triple(142f, 30f, -1f)).forEach { (x, y, sx) ->
            val p = androidx.compose.ui.graphics.Path().apply {
                moveTo(x * d, y * d)
                lineTo(x * d, (y - 8) * d)
                quadraticTo(x * d, (y - 14) * d, (x + sx * 6) * d, (y - 14) * d)
                lineTo((x + sx * 14) * d, (y - 14) * d)
            }
            drawPath(p, c.accent, style = Stroke(4 * d, cap = StrokeCap.Round))
        }
    }
}
