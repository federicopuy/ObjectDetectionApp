package com.federicopuy.objectdetectionapp.objectdetection.pytorch

import android.graphics.RectF
import kotlin.math.max
import kotlin.math.min

class Result(val classIndex: Int, val score: Float, val boundingBox: RectF)

class PrePostProcessor(
    private val threshold: Float = 0.5f,
) {

    // model output is of size 25200*(num_of_class+5)
    private val outputRow = 25200 // as decided by the YOLOv5 model for input image of size 640*640
    private val outputColumn = 85 // left, top, right, bottom, score and 80 class probability

    fun outputsToNMSPredictions(
        outputs: FloatArray,
        imgScaleX: Float,
        imgScaleY: Float,
        startX: Float = 0.0F,
        startY: Float = 0.0F,
    ): List<Result> {
        val results = mutableListOf<Result>()

        for (i in 0 until outputRow) {
            if (outputs[i * outputColumn + 4] > threshold) {
                val x = outputs[i * outputColumn]
                val y = outputs[i * outputColumn + 1]
                val w = outputs[i * outputColumn + 2]
                val h = outputs[i * outputColumn + 3]
                val left = imgScaleX * (x - w / 2)
                val top = imgScaleY * (y - h / 2)
                val right = imgScaleX * (x + w / 2)
                val bottom = imgScaleY * (y + h / 2)
                var max = outputs[i * outputColumn + 5]
                var classIndex = 0
                for (j in 0 until outputColumn - 5) {
                    if (outputs[i * outputColumn + 5 + j] > max) {
                        max = outputs[i * outputColumn + 5 + j]
                        classIndex = j
                    }
                }
                val rect = RectF(
                    startX + left, startY + top, startX + right, startY + bottom
                )
                results.add(Result(classIndex, outputs[i * outputColumn + 4], rect))
            }
        }
        return nonMaxSuppression(results, 3, threshold)
    }

    // The two methods nonMaxSuppression and IOU below are ported from
    // https://github.com/hollance/YOLO-CoreML-MPSNNGraph/blob/master/Common/Helpers.swift
    /**
     * Removes bounding boxes that overlap too much with other boxes that have
     * a higher score.
     * - Parameters:
     * - boxes: an array of bounding boxes and their scores
     * - limit: the maximum number of boxes that will be selected
     * - threshold: used to decide whether boxes overlap too much
     */
    private fun nonMaxSuppression(
        boxes: MutableList<Result>,
        limit: Int,
        threshold: Float,
    ): MutableList<Result> {
        boxes.sortByDescending { it.score }
        val selected = mutableListOf<Result>()
        val active = BooleanArray(boxes.size) { true }
        var numActive = active.size

        // The algorithm is simple: Start with the box that has the highest score.
        // Remove any remaining boxes that overlap it more than the given threshold
        // amount. If there are any boxes left (i.e. these did not overlap with any
        // previous boxes), then repeat this procedure, until no more boxes remain
        // or the limit has been reached.
        var done = false
        var i = 0
        while (i < boxes.size && !done) {
            if (active[i]) {
                val boxA = boxes[i]
                selected.add(boxA)
                if (selected.size >= limit) break
                for (j in i + 1 until boxes.size) {
                    if (active[j]) {
                        val boxB = boxes[j]
                        if (intersectionOverUnion(boxA.boundingBox, boxB.boundingBox) > threshold) {
                            active[j] = false
                            numActive -= 1
                            if (numActive <= 0) {
                                done = true
                                break
                            }
                        }
                    }
                }
            }
            i++
        }
        return selected
    }

    /** Computes intersection-over-union overlap between two bounding boxes.*/
    private fun intersectionOverUnion(boxA: RectF, boxB: RectF): Float {
        val areaA = calculateArea(boxA)
        val areaB = calculateArea(boxB)
        if (areaA < 0.0 || areaB < 0.0) return 0.0f
        val intersectionMinX = max(boxA.left, boxB.left)
        val intersectionMinY = max(boxA.top, boxB.top)
        val intersectionMaxX = min(boxA.right, boxB.right)
        val intersectionMaxY = min(boxA.bottom, boxB.bottom)
        val intersectionArea = max(intersectionMaxY - intersectionMinY, 0f) *
            max(intersectionMaxX - intersectionMinX, 0f)
        return intersectionArea / (areaA + areaB - intersectionArea)
    }

    private fun calculateArea(box: RectF): Float {
        return (box.right - box.left) * (box.bottom - box.top)
    }

    companion object {
        // model input image size
        var inputWidth = 640
        var inputHeight = 640

        // for yolov5 model, no need to apply MEAN and STD
        var NO_MEAN_RGB = floatArrayOf(0.0f, 0.0f, 0.0f)
        var NO_STD_RGB = floatArrayOf(1.0f, 1.0f, 1.0f)
    }
}