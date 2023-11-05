package com.federicopuy.objectdetectionapp.objectdetection.pytorch

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Matrix
import android.util.Log
import androidx.camera.core.ExperimentalGetImage
import com.federicopuy.objectdetectionapp.R
import com.federicopuy.objectdetectionapp.objectdetection.DetectedObject
import com.federicopuy.objectdetectionapp.objectdetection.DetectionResult
import com.federicopuy.objectdetectionapp.objectdetection.ObjectDetector
import com.federicopuy.objectdetectionapp.objectdetection.assetFilePath
import com.federicopuy.objectdetectionapp.objectdetection.pytorch.PrePostProcessor.Companion.NO_MEAN_RGB
import com.federicopuy.objectdetectionapp.objectdetection.pytorch.PrePostProcessor.Companion.NO_STD_RGB
import com.federicopuy.objectdetectionapp.objectdetection.readStringsFromTxtAsset
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import org.pytorch.IValue
import org.pytorch.LiteModuleLoader
import org.pytorch.Module
import org.pytorch.torchvision.TensorImageUtils


private const val TAG = "PytorchObjectDetector"

@ExperimentalGetImage
class PytorchObjectDetector(
    private val threshold: Float = 0.5f,
    private val maxResults: Int = 5,
    private val context: Context,
    private val model: String = "yolov5s.`torchscript.ptl",
    classesFileName: String = "classes.txt",
    private val prePostProcessor: PrePostProcessor = PrePostProcessor(threshold),
) : ObjectDetector {

    private var module: Module? = null
    private val classes = readStringsFromTxtAsset(context, classesFileName)

    override fun setup() {
        runCatching {
            module = LiteModuleLoader.load(assetFilePath(context, model))
        }.onFailure {
            Log.e(TAG, "Error reading assets", it)
        }
    }

    override fun cleanup() {
        module = null
    }

    override fun detectionResults(bitmap: Bitmap, imageRotation: Int): Flow<DetectionResult> =
        flow {
            if (module == null) {
                setup()
            }

            val matrix = Matrix().apply {
                postRotate(imageRotation.toFloat())
            }
            val rotatedBitmap = Bitmap.createBitmap(
                bitmap,
                0,
                0,
                bitmap.width,
                bitmap.height,
                matrix,
                false
            )
            val resizedBitmap = Bitmap.createScaledBitmap(
                rotatedBitmap,
                PrePostProcessor.inputWidth,
                PrePostProcessor.inputHeight,
                true
            )

            val inputTensor = TensorImageUtils.bitmapToFloat32Tensor(
                resizedBitmap,
                NO_MEAN_RGB,
                NO_STD_RGB
            )
            val outputTuple = module?.forward(IValue.from(inputTensor))?.toTuple() ?: emptyArray()
            val outputTensor = outputTuple[0].toTensor()
            val outputs = outputTensor.dataAsFloatArray

            val imgScaleX = rotatedBitmap.width.toFloat() / PrePostProcessor.inputWidth
            val imgScaleY = rotatedBitmap.height.toFloat() / PrePostProcessor.inputHeight

            val results = prePostProcessor.outputsToNMSPredictions(
                outputs = outputs,
                imgScaleX = imgScaleX,
                imgScaleY = imgScaleY,
            )

            val detectedObjects = results.map { result ->
                DetectedObject(
                    boundingBox = result.boundingBox,
                    label = getClassLabel(result.classIndex),
                    confidence = result.score
                )
            }.sortedByDescending { it.confidence }
                .take(maxResults)

            emit(
                DetectionResult(
                    detectedObjects = detectedObjects,
                    imageWidth = rotatedBitmap.width,
                    imageHeight = rotatedBitmap.height
                )
            )
        }.flowOn(Dispatchers.IO)

    private fun getClassLabel(classIndex: Int): String {
        if (classes.isEmpty() || classIndex > classes.size) return context.getString(R.string.unknown)
        return classes[classIndex]
    }
    
}