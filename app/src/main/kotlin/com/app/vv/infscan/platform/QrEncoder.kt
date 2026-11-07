package com.app.vv.infscan.platform

import android.graphics.Bitmap
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel

/** A QR code as a square grid of modules, without quiet zone. */
class QrMatrix(val size: Int, private val modules: BooleanArray) {
    operator fun get(x: Int, y: Int): Boolean = modules[y * size + x]
}

object QrEncoder {
    private const val QUIET_ZONE_MODULES = 2

    fun matrix(content: String): QrMatrix {
        val hints = mapOf(
            EncodeHintType.ERROR_CORRECTION to ErrorCorrectionLevel.M,
            EncodeHintType.MARGIN to 0,
            EncodeHintType.CHARACTER_SET to "UTF-8",
        )
        val bits = QRCodeWriter().encode(content, BarcodeFormat.QR_CODE, 0, 0, hints)
        val modules = BooleanArray(bits.width * bits.height) { i -> bits[i % bits.width, i / bits.width] }
        return QrMatrix(bits.width, modules)
    }

    /** Renders a square bitmap with a two-module quiet zone, scaled to whole pixels per module. */
    fun bitmap(content: String, targetPx: Int, foreground: Int, background: Int): Bitmap {
        val m = matrix(content)
        val total = m.size + 2 * QUIET_ZONE_MODULES
        val scale = (targetPx / total).coerceAtLeast(1)
        val side = total * scale
        val pixels = IntArray(side * side) { background }
        for (y in 0 until m.size) {
            for (x in 0 until m.size) {
                if (!m[x, y]) continue
                val left = (x + QUIET_ZONE_MODULES) * scale
                val top = (y + QUIET_ZONE_MODULES) * scale
                for (dy in 0 until scale) {
                    val row = (top + dy) * side
                    for (dx in 0 until scale) pixels[row + left + dx] = foreground
                }
            }
        }
        return Bitmap.createBitmap(pixels, side, side, Bitmap.Config.ARGB_8888)
    }
}
