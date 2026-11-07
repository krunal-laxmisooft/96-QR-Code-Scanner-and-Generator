package com.app.vv.infscan.platform

import android.content.Context
import android.net.Uri
import com.app.vv.infscan.domain.model.DetectedCode
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.suspendCancellableCoroutine
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume

interface BarcodeImageDecoder {
    suspend fun decode(uri: String): List<DetectedCode>
}

@Singleton
class MlKitBarcodeImageDecoder @Inject constructor(
    @ApplicationContext private val context: Context,
) : BarcodeImageDecoder {

    override suspend fun decode(uri: String): List<DetectedCode> {
        val image = runCatching { InputImage.fromFilePath(context, Uri.parse(uri)) }.getOrNull() ?: return emptyList()
        val scanner = BarcodeScanning.getClient(BarcodeScannerOptions.Builder().setBarcodeFormats(Barcode.FORMAT_ALL_FORMATS).build())
        return try {
            suspendCancellableCoroutine { cont ->
                scanner.process(image)
                    .addOnSuccessListener { barcodes -> cont.resume(barcodes.mapNotNull(::toDetectedCode)) }
                    .addOnFailureListener { cont.resume(emptyList()) }
            }
        } finally {
            scanner.close()
        }
    }
}

fun toDetectedCode(barcode: Barcode): DetectedCode? {
    val raw = barcode.rawValue?.takeIf { it.isNotBlank() } ?: return null
    return DetectedCode(raw, BarcodeFormats.fromMlKit(barcode.format))
}
