package com.federicopuy.objectdetectionapp.ui.views

import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.max

@Composable
fun BoundingBox(
    box: RectF,
    boxColor: Color = Color.Red,
    label: String,
    labelColor: Color = Color.White,
    labelBackgroundColor: Color = Color.Black,
    modifier: Modifier = Modifier,
    imageWidth: Int,
    imageHeight: Int,
    textBackgroundPaint: Paint = Paint(),
    bounds: Rect = Rect(),
    textSize: TextUnit = 14.sp,
    textPadding: Float = 30f,
) {
    val textMeasurer = rememberTextMeasurer()

    Canvas(modifier = modifier.fillMaxSize()) {

        // PreviewView is in FILL_START mode. So we need to scale up the bounding box to match with
        // the size that the captured images will be displayed.
        val scaleFactor = max(size.width / imageWidth, size.height / imageHeight)

        val top = box.top * scaleFactor
        val left = box.left * scaleFactor

        val boxWidth = box.width() * scaleFactor
        val boxHeight = box.height() * scaleFactor

        drawRoundRect(
            color = boxColor,
            size = Size(width = boxWidth, height = boxHeight),
            topLeft = Offset(x = left, y = top),
            style = Stroke(width = 2.dp.toPx())
        )

        // Draw rect behind display text, we need a contrasting background to make the text easier to read.
        textBackgroundPaint.textSize = textSize.toPx()
        textBackgroundPaint.getTextBounds(label, 0, label.length, bounds)
        val textWidth = bounds.width()
        val textHeight = bounds.height()
        drawRect(
            color = labelBackgroundColor,
            size = Size(
                width = textWidth.toFloat() + textPadding,
                height = textHeight.toFloat() + textPadding
            ),
            topLeft = Offset(x = left, y = top),
        )

        drawText(
            textMeasurer = textMeasurer,
            text = label,
            topLeft = Offset(x = left, y = top),
            style = TextStyle(color = labelColor, fontSize = textSize),
            size = Size(
                width = textWidth.toFloat() + textPadding,
                height = textHeight.toFloat() + textPadding
            )
        )
    }

}