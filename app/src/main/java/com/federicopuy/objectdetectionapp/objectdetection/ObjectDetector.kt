package com.federicopuy.objectdetectionapp.objectdetection

import android.graphics.Bitmap
import android.graphics.RectF
import kotlinx.coroutines.flow.Flow

/* Abstraction for object detection implementations. */
interface ObjectDetector {

    /* Setup and initialize the object detector. */
    fun setup()

    /* Cleanup and release resources. */
    fun cleanup()

    /* Detect objects in the given bitmap. */
    fun detectionResults(bitmap: Bitmap, imageRotation: Int): Flow<DetectionResult>
}

data class DetectionResult(
    val detectedObjects: List<DetectedObject> = emptyList(),
    val imageWidth: Int = 0,
    val imageHeight: Int = 0,
)

data class DetectedObject(
    val boundingBox: RectF,
    val label: String,
    /* Confidence score of the prediction, should be between 0 and 1. */
    val confidence: Float,
)