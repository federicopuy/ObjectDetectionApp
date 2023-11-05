package com.federicopuy.objectdetectionapp.ui

import android.graphics.Bitmap
import android.util.Log
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.lifecycle.compose.collectAsStateWithLifecycle

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.viewmodel.compose.viewModel
import com.federicopuy.objectdetectionapp.R
import com.federicopuy.objectdetectionapp.ui.views.BoundingBox
import com.federicopuy.objectdetectionapp.ui.camera.CameraPreview
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.PermissionStatus
import com.google.accompanist.permissions.rememberPermissionState
import java.util.concurrent.Executor
import androidx.lifecycle.Lifecycle.State.*

private const val TAG = "ObjectDetectionApp"

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun ObjectDetectionApp(
    cameraExecutor: Executor,
    objectDetectionViewModel: ObjectDetectionViewModel = viewModel(),
) {
    val cameraPermissionState = rememberPermissionState(android.Manifest.permission.CAMERA)
    when (cameraPermissionState.status) {
        // If the camera permission is granted, then show screen with the feature enabled
        PermissionStatus.Granted -> {
            val objectDetectionUiState by objectDetectionViewModel.objectDetectionUiState.collectAsStateWithLifecycle(
                initialValue = ObjectDetectionUiState()
            )

            val onImageCaptured: (Bitmap, Int) -> Unit = { bitmap, rotationDegrees ->
                objectDetectionViewModel.detectObjectsInImage(
                    bitmap = bitmap,
                    rotationDegrees = rotationDegrees,
                    timestamp = System.currentTimeMillis(),
                )
            }

            val lifecycleOwner = LocalLifecycleOwner.current
            val lifecycleState by lifecycleOwner.lifecycle.currentStateFlow.collectAsState()
            LaunchedEffect(lifecycleState) {
                when (lifecycleState) {
                    DESTROYED -> {
                        Log.d(TAG, "onDestroy: Stopping detection")
                        objectDetectionViewModel.stopDetection()
                    }

                    INITIALIZED -> {}
                    CREATED -> {}
                    STARTED -> {}
                    RESUMED -> {}
                }
            }

            CameraPreview(
                executor = cameraExecutor,
                onImageCaptured = onImageCaptured,
            )

            SearchObjectsScreen(objectDetectionUiState)
        }

        is PermissionStatus.Denied -> {
            AlertDialog(
                onDismissRequest = { },
                title = { Text(stringResource(R.string.permission)) },
                text = { Text(stringResource(R.string.camera_permission_needed)) },
                confirmButton = {
                    Button(onClick = { cameraPermissionState.launchPermissionRequest() }) {
                        Text(stringResource(R.string.ok))
                    }
                },
            )
        }
    }
}

@Composable
fun SearchObjectsScreen(
    state: ObjectDetectionUiState,
) {
    val detectionResult = state.detectionResult
    detectionResult.detectedObjects.forEach {
        val label = it.label + " " + String.format("%.2f", it.confidence)
        BoundingBox(
            box = it.boundingBox,
            label = label,
            imageWidth = detectionResult.imageWidth,
            imageHeight = detectionResult.imageHeight
        )
    }
}