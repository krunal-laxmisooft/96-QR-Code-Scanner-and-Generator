package com.app.vv.infscan.platform

import com.app.vv.infscan.domain.model.CodeFormat

object BarcodeFormats {
    // Values of com.google.mlkit.vision.barcode.common.Barcode.FORMAT_*
    private const val CODE_128 = 1
    private const val CODE_39 = 2
    private const val CODE_93 = 4
    private const val CODABAR = 8
    private const val DATA_MATRIX = 16
    private const val EAN_13 = 32
    private const val EAN_8 = 64
    private const val ITF = 128
    private const val QR_CODE = 256
    private const val UPC_A = 512
    private const val UPC_E = 1024
    private const val PDF417 = 2048
    private const val AZTEC = 4096

    fun fromMlKit(format: Int): CodeFormat = when (format) {
        CODE_128 -> CodeFormat.CODE_128
        CODE_39 -> CodeFormat.CODE_39
        CODE_93 -> CodeFormat.CODE_93
        CODABAR -> CodeFormat.CODABAR
        DATA_MATRIX -> CodeFormat.DATA_MATRIX
        EAN_13 -> CodeFormat.EAN_13
        EAN_8 -> CodeFormat.EAN_8
        ITF -> CodeFormat.ITF
        QR_CODE -> CodeFormat.QR
        UPC_A -> CodeFormat.UPC_A
        UPC_E -> CodeFormat.UPC_E
        PDF417 -> CodeFormat.PDF_417
        AZTEC -> CodeFormat.AZTEC
        else -> CodeFormat.UNKNOWN
    }
}
