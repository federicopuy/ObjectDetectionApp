package com.federicopuy.objectdetectionapp.objectdetection.tflite

import android.content.Context
import android.graphics.Bitmap
import android.os.SystemClock
import android.util.Log
import androidx.camera.core.ExperimentalGetImage
import com.federicopuy.objectdetectionapp.objectdetection.DetectedObject
import com.federicopuy.objectdetectionapp.objectdetection.DetectionResult
import com.federicopuy.objectdetectionapp.objectdetection.ObjectDetector
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import org.tensorflow.lite.support.image.ImageProcessor
import org.tensorflow.lite.support.image.TensorImage
import org.tensorflow.lite.support.image.ops.Rot90Op
import org.tensorflow.lite.task.core.BaseOptions
import org.tensorflow.lite.task.vision.detector.ObjectDetector as TensorFlowLiteObjectDetector

@ExperimentalGetImage
class TFLiteObjectDetector(
    private var threshold: Float = 0.5f,
    private var numThreads: Int = 2,
    private var maxResults: Int = 3,
    private var model: String = "mobilenetv1.tflite",
    private val context: Context,
    private val computeSettings: ComputeSettings = ComputeSettings(),
) : ObjectDetector {

    private var detector: TensorFlowLiteObjectDetector? = null

    // Initialize the object detector using current settings on the thread that is using it.
    override fun setup() {
        val optionsBuilder =
            TensorFlowLiteObjectDetector.ObjectDetectorOptions.builder()
                .setScoreThreshold(threshold)
                .setMaxResults(maxResults)

        // Set general detection options, including number of used threads
        val baseOptionsBuilder = BaseOptions.builder().setNumThreads(numThreads)
        computeSettings.setDelegate(baseOptionsBuilder)
        optionsBuilder.setBaseOptions(baseOptionsBuilder.build())

        runCatching {
            detector =
                TensorFlowLiteObjectDetector.createFromFileAndOptions(
                    context,
                    model,
                    optionsBuilder.build()
                )
        }.onFailure {
            Log.e(TAG, "TFLite failed to load model with error: " + it.message)
        }
    }

    override fun cleanup() {
        detector = null
    }

    override fun detectionResults(bitmap: Bitmap, imageRotation: Int): Flow<DetectionResult> =
        flow {
            if (detector == null) {
                setup()
            }

            // Inference time is the difference between the system time at the start and finish of the
            // detection process
            var inferenceTime = SystemClock.uptimeMillis()

            // Create preprocessor for the image.
            // See https://www.tensorflow.org/lite/inference_with_metadata/
            //            lite_support#imageprocessor_architecture
            val imageProcessor =
                ImageProcessor.Builder()
                    .add(Rot90Op(-imageRotation / 90))
                    .build()

            // Preprocess the image and convert it into a TensorImage for detection.
            val tensorImage = imageProcessor.process(TensorImage.fromBitmap(bitmap))

            val results = detector?.detect(tensorImage) ?: emptyList()

            inferenceTime = SystemClock.uptimeMillis() - inferenceTime
            Log.d(TAG, "Inference time: $inferenceTime")

            val detectedObjects = results.mapNotNull { result ->
                DetectedObject(
                    boundingBox = result.boundingBox,
                    label = result.categories.first().label,
                    confidence = result.categories.first().score,
                )
            }

            emit(
                DetectionResult(
                    detectedObjects = detectedObjects,
                    imageWidth = tensorImage.width,
                    imageHeight = tensorImage.height,
                )
            )
        }

    companion object {
        private const val TAG = "TFLiteObjectDetector"
    }

}
