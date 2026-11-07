package com.app.vv.infscan.ui.scan

import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.app.vv.infscan.domain.model.DetectedCode
import com.app.vv.infscan.platform.toDetectedCode
import com.google.mlkit.vision.barcode.BarcodeScanner
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

private class BarcodeAnalyzer(
    private val onCodes: (List<DetectedCode>) -> Unit,
    private val onFailure: () -> Unit,
) : ImageAnalysis.Analyzer {
    private val scanner: BarcodeScanner = BarcodeScanning.getClient(
        BarcodeScannerOptions.Builder().setBarcodeFormats(Barcode.FORMAT_ALL_FORMATS).build(),
    )

    @androidx.annotation.OptIn(androidx.camera.core.ExperimentalGetImage::class)
    override fun analyze(image: ImageProxy) {
        val media = image.image
        if (media == null) {
            image.close()
            return
        }
        val input = InputImage.fromMediaImage(media, image.imageInfo.rotationDegrees)
        scanner.process(input)
            .addOnSuccessListener { barcodes -> barcodes.mapNotNull(::toDetectedCode).takeIf { it.isNotEmpty() }?.let(onCodes) }
            .addOnFailureListener { onFailure() }
            .addOnCompleteListener { image.close() }
    }

    fun close() = scanner.close()
}

/** Live camera preview that reports every barcode ML Kit finds. Binds to the lifecycle of its composition. */
@Composable
fun CameraScanner(
    torchOn: Boolean,
    zoomRatio: Float,
    onCodes: (List<DetectedCode>) -> Unit,
    onFailure: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val currentOnCodes by rememberUpdatedState(onCodes)
    val currentOnFailure by rememberUpdatedState(onFailure)
    var camera by remember { mutableStateOf<Camera?>(null) }
    val previewView = remember { PreviewView(context).apply { scaleType = PreviewView.ScaleType.FILL_CENTER } }

    DisposableEffect(lifecycleOwner) {
        val executor: ExecutorService = Executors.newSingleThreadExecutor()
        val analyzer = BarcodeAnalyzer({ currentOnCodes(it) }, { currentOnFailure() })
        val providerFuture = ProcessCameraProvider.getInstance(context)
        var provider: ProcessCameraProvider? = null
        providerFuture.addListener({
            runCatching {
                val p = providerFuture.get()
                provider = p
                val preview = Preview.Builder().build().also { it.surfaceProvider = previewView.surfaceProvider }
                val analysis = ImageAnalysis.Builder()
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .build()
                    .also { it.setAnalyzer(executor, analyzer) }
                p.unbindAll()
                camera = p.bindToLifecycle(lifecycleOwner, CameraSelector.DEFAULT_BACK_CAMERA, preview, analysis)
            }.onFailure { currentOnFailure() }
        }, ContextCompat.getMainExecutor(context))
        onDispose {
            provider?.unbindAll()
            camera = null
            executor.shutdown()
            analyzer.close()
        }
    }

    LaunchedEffect(camera, torchOn) {
        val cam = camera ?: return@LaunchedEffect
        if (cam.cameraInfo.hasFlashUnit()) cam.cameraControl.enableTorch(torchOn)
    }
    LaunchedEffect(camera, zoomRatio) {
        val cam = camera ?: return@LaunchedEffect
        val range = cam.cameraInfo.zoomState.value
        val clamped = range?.let { zoomRatio.coerceIn(it.minZoomRatio, it.maxZoomRatio) } ?: zoomRatio
        cam.cameraControl.setZoomRatio(clamped)
    }

    AndroidView(factory = { previewView }, modifier = modifier)
}

