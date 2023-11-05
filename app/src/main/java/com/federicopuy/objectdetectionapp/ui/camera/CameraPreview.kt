package com.federicopuy.objectdetectionapp.ui.camera

import android.content.Context
import android.graphics.Bitmap
import androidx.camera.core.AspectRatio
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview.Builder
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import java.util.concurrent.Executor
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

private suspend fun Context.getCameraProvider(): ProcessCameraProvider =
    suspendCoroutine { continuation ->
        ProcessCameraProvider.getInstance(this).also { future ->
            future.addListener({
                continuation.resume(future.get())
            }, executor)
        }
    }

val Context.executor: Executor
    get() = ContextCompat.getMainExecutor(this)

@Composable
fun CameraPreview(
    executor: Executor,
    onImageCaptured: (Bitmap, Int) -> Unit,
) {
    val lifecycleOwner = LocalLifecycleOwner.current
    val lensFacing = CameraSelector.LENS_FACING_BACK
    val context = LocalContext.current
    val aspectRatio = AspectRatio.RATIO_4_3

    val preview = Builder()
        .setTargetAspectRatio(aspectRatio)
        .build()

    val previewView =
        remember { PreviewView(context).apply { scaleType = PreviewView.ScaleType.FILL_START } }

    val cameraSelector = CameraSelector.Builder()
        .requireLensFacing(lensFacing)
        .build()

    var bitmapBuffer: Bitmap? = null

    val imageAnalyzer =
        ImageAnalysis.Builder()
            .setTargetAspectRatio(aspectRatio)
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_RGBA_8888)
            .build()
            .also {
                it.setAnalyzer(executor) { image ->
                    // The image rotation and RGB image buffer are initialized only once
                    // the analyzer has started running
                    val bitmap = bitmapBuffer ?: Bitmap.createBitmap(
                        image.width,
                        image.height,
                        Bitmap.Config.ARGB_8888
                    ).also { buffer -> bitmapBuffer = buffer }

                    // Copy out RGB bits to the shared bitmap buffer
                    image.use { bitmap.copyPixelsFromBuffer(image.planes[0].buffer) }

                    val imageRotation = image.imageInfo.rotationDegrees

                    // Pass Bitmap and rotation to the object detector helper for processing and detection
                    onImageCaptured(bitmap, imageRotation)
                }
            }

    LaunchedEffect(lensFacing) {
        val cameraProvider = context.getCameraProvider()
        cameraProvider.unbindAll()

        cameraProvider.bindToLifecycle(
            lifecycleOwner,
            cameraSelector,
            preview,
            imageAnalyzer
        )

        preview.setSurfaceProvider(previewView.surfaceProvider)
    }
    AndroidView(modifier = Modifier.fillMaxSize(), factory = { previewView })
}

