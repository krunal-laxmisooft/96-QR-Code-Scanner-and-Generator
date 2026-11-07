package com.app.vv.infscan.platform

import android.graphics.Bitmap
import javax.inject.Inject
import javax.inject.Singleton

interface QrImageFactory {
    fun create(payload: String, sizePx: Int): Bitmap
}

@Singleton
class ZxingQrImageFactory @Inject constructor() : QrImageFactory {
    override fun create(payload: String, sizePx: Int): Bitmap =
        QrEncoder.bitmap(payload, sizePx, foreground = FOREGROUND, background = BACKGROUND)

    private companion object {
        const val FOREGROUND = 0xFF10201F.toInt()
        const val BACKGROUND = 0xFFFFFFFF.toInt()
    }
}
