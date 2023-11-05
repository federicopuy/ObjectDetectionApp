package com.federicopuy.objectdetectionapp.objectdetection.tflite

import android.util.Log
import org.tensorflow.lite.gpu.CompatibilityList
import org.tensorflow.lite.task.core.BaseOptions

class ComputeSettings(
    private val compatibilityList: CompatibilityList = CompatibilityList(),
) {

    /* Set the delegate that will determine where to run the inference.  CPU and NNAPI delegates
    can be used with detectors that are created on the main thread and used on a background thread, but
    the GPU delegate needs to be used on the thread that initialized the detector */
    fun setDelegate(builder: BaseOptions.Builder, computeMode: ComputeMode = ComputeMode.NNAPI) {
        when (computeMode) {
            is ComputeMode.CPU -> {
                // Default, do nothing
            }

            is ComputeMode.GPU -> {
                if (compatibilityList.isDelegateSupportedOnThisDevice) {
                    builder.useGpu()
                } else {
                    Log.e(TAG, "GPU delegate is not supported on this device.")
                }
            }

            is ComputeMode.NNAPI -> {
                builder.useNnapi()
            }
        }
    }

    companion object {
        private const val TAG = "ComputeSettings"
    }

    sealed class ComputeMode {
        object CPU : ComputeMode()
        object GPU : ComputeMode()
        object NNAPI : ComputeMode()
    }
}

