package com.app.vv.infscan.ui.common

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.app.vv.infscan.R
import com.app.vv.infscan.domain.model.CodeFormat
import com.app.vv.infscan.domain.model.CodeKind

@DrawableRes
fun CodeKind.icon(): Int = when (this) {
    CodeKind.URL -> R.drawable.ic_link
    CodeKind.WIFI -> R.drawable.ic_wifi
    CodeKind.CONTACT -> R.drawable.ic_contact
    CodeKind.EVENT -> R.drawable.ic_event
    CodeKind.TEXT -> R.drawable.ic_text
    CodeKind.PRODUCT -> R.drawable.ic_barcode
    CodeKind.SMS -> R.drawable.ic_sms
    CodeKind.EMAIL -> R.drawable.ic_email
    CodeKind.PHONE -> R.drawable.ic_contact
    CodeKind.LOCATION -> R.drawable.ic_location
}

@StringRes
fun CodeKind.label(): Int = when (this) {
    CodeKind.URL -> R.string.kind_website
    CodeKind.WIFI -> R.string.kind_wifi
    CodeKind.CONTACT -> R.string.kind_contact
    CodeKind.EVENT -> R.string.kind_event
    CodeKind.TEXT -> R.string.kind_text
    CodeKind.PRODUCT -> R.string.kind_product
    CodeKind.SMS -> R.string.kind_message
    CodeKind.EMAIL -> R.string.kind_email
    CodeKind.PHONE -> R.string.kind_phone
    CodeKind.LOCATION -> R.string.kind_location
}

@StringRes
fun CodeFormat.label(): Int = when (this) {
    CodeFormat.QR -> R.string.format_qr
    CodeFormat.EAN_13 -> R.string.format_ean13
    CodeFormat.EAN_8 -> R.string.format_ean8
    CodeFormat.UPC_A -> R.string.format_upca
    CodeFormat.UPC_E -> R.string.format_upce
    CodeFormat.CODE_128 -> R.string.format_code128
    CodeFormat.CODE_39 -> R.string.format_code39
    CodeFormat.CODE_93 -> R.string.format_code93
    CodeFormat.DATA_MATRIX -> R.string.format_data_matrix
    CodeFormat.PDF_417 -> R.string.format_pdf417
    CodeFormat.AZTEC -> R.string.format_aztec
    CodeFormat.ITF -> R.string.format_itf
    CodeFormat.CODABAR -> R.string.format_codabar
    CodeFormat.UNKNOWN -> R.string.format_unknown
}
