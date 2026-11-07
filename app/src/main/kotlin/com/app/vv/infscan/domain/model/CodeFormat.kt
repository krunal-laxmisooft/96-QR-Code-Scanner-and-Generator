package com.app.vv.infscan.domain.model

enum class CodeFormat {
    QR, EAN_13, EAN_8, UPC_A, UPC_E, CODE_128, CODE_39, CODE_93, DATA_MATRIX, PDF_417, AZTEC, ITF, CODABAR, UNKNOWN;

    val isProduct: Boolean
        get() = this == EAN_13 || this == EAN_8 || this == UPC_A || this == UPC_E
}

enum class CodeKind { URL, WIFI, CONTACT, EVENT, TEXT, PRODUCT, SMS, EMAIL, PHONE, LOCATION }

enum class HistoryKind { SCANNED, CREATED }

enum class ScanSource { CAMERA, GALLERY, CREATED }
