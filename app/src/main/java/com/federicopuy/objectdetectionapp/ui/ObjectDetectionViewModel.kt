package com.federicopuy.objectdetectionapp.ui

import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.federicopuy.objectdetectionapp.objectdetection.DetectionResult
import com.federicopuy.objectdetectionapp.objectdetection.ObjectDetector
import com.federicopuy.objectdetectionapp.objectdetection.pytorch.PytorchObjectDetector
import com.federicopuy.objectdetectionapp.objectdetection.tflite.TFLiteObjectDetector
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapConcat
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import javax.inject.Inject
import kotlin.time.Duration.Companion.seconds

data class ObjectDetectionUiState(
    val detectionResult: DetectionResult = DetectionResult(),
)

data class CapturedImage(
    val timestamp: Long,
    val bitmap: Bitmap,
    val rotationDegrees: Int,
)

@HiltViewModel
class ObjectDetectionViewModel @Inject constructor(
    pytorchObjectDetector: PytorchObjectDetector,
    tfLiteObjectDetector: TFLiteObjectDetector,
) :
    ViewModel() {

    // TODO: Replace with pytorchObjectDetector if you want to use PyTorch Mobile instead
    private val objectDetector: ObjectDetector = tfLiteObjectDetector

    private val lastCapturedImage = MutableStateFlow<CapturedImage?>(null)

    private val _objectDetectionUiState: Flow<ObjectDetectionUiState> =
        // This flow will emit a new value every time a new image is captured
        lastCapturedImage.flatMapConcat {
            if (it == null) {
                // No image captured yet
                return@flatMapConcat flow { emit(ObjectDetectionUiState()) }
            }
            objectDetector.detectionResults(it.bitmap, it.rotationDegrees).map { detection ->
                ObjectDetectionUiState(
                    detectionResult = detection
                )
            }
        }

    val objectDetectionUiState: StateFlow<ObjectDetectionUiState> =
        _objectDetectionUiState.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(FLOW_SHARING_STOP_MILLIS),
            initialValue = ObjectDetectionUiState()
        )

    init {
        objectDetector.setup()
    }

    fun detectObjectsInImage(bitmap: Bitmap, rotationDegrees: Int, timestamp: Long) {
        lastCapturedImage.update {
            CapturedImage(timestamp = timestamp, bitmap = bitmap, rotationDegrees = rotationDegrees)
        }
    }

    fun stopDetection() {
        objectDetector.cleanup()
    }

    companion object {
        private val FLOW_SHARING_STOP_MILLIS = 5.seconds.inWholeMilliseconds
    }
}