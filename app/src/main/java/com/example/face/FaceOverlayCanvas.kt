package com.example.face

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.CyanScanner
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.ObsidianDeep
import com.example.ui.theme.SkyElectric
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

@Composable
fun FaceOverlayCanvas(
    faces: List<DetectedFaceUi>,
    sourceImageWidth: Int,
    sourceImageHeight: Int,
    isFrontCamera: Boolean,
    similarity: Float? = null,
    modifier: Modifier = Modifier,
) {
    val textMeasurer = rememberTextMeasurer()

    Canvas(modifier = modifier.fillMaxSize()) {
        if (faces.isEmpty() || sourceImageWidth <= 0 || sourceImageHeight <= 0) return@Canvas

        // PreviewView uses FILL_CENTER (centerCrop), so compute uniform scale + centering offset.
        val scale = max(
            size.width / sourceImageWidth.toFloat(),
            size.height / sourceImageHeight.toFloat(),
        )
        val scaledWidth = sourceImageWidth * scale
        val scaledHeight = sourceImageHeight * scale
        val offsetX = (size.width - scaledWidth) / 2f
        val offsetY = (size.height - scaledHeight) / 2f

        faces.forEachIndexed { index, face ->
            val accentColor = if (index == 0) CyanScanner else SkyElectric

            val rawLeft = face.boundingBox.left * scale + offsetX
            val rawRight = face.boundingBox.right * scale + offsetX
            val left = if (isFrontCamera) size.width - rawRight else rawLeft
            val right = if (isFrontCamera) size.width - rawLeft else rawRight
            val top = face.boundingBox.top * scale + offsetY
            val bottom = face.boundingBox.bottom * scale + offsetY

            val boxWidth = (right - left).coerceAtLeast(1f)
            val boxHeight = (bottom - top).coerceAtLeast(1f)

            drawRect(
                color = accentColor.copy(alpha = 0.08f),
                topLeft = Offset(left, top),
                size = Size(boxWidth, boxHeight),
            )

            drawCornerBrackets(
                left = left,
                top = top,
                right = right,
                bottom = bottom,
                color = accentColor,
                strokeWidth = 3.dp.toPx(),
                cornerLen = min(boxWidth, boxHeight) * 0.22f,
            )

            face.landmarks.forEach { point ->
                val rawX = point.x * scale + offsetX
                val px = if (isFrontCamera) size.width - rawX else rawX
                val py = point.y * scale + offsetY
                drawCircle(
                    color = accentColor.copy(alpha = 0.35f),
                    radius = 5.dp.toPx(),
                    center = Offset(px, py),
                )
                drawCircle(
                    color = accentColor,
                    radius = 2.4.dp.toPx(),
                    center = Offset(px, py),
                )
            }

            val idLabel = face.trackingId?.let { "#$it" } ?: "#${index + 1}"
            val simSuffix = if (index == 0 && similarity != null) {
                " • ${(similarity.coerceAtLeast(0f) * 100).roundToInt()}%"
            } else {
                ""
            }
            drawBadgeLabel(
                textMeasurer = textMeasurer,
                label = "YUZ $idLabel$simSuffix",
                anchorX = left,
                anchorY = top,
                accentColor = accentColor,
            )
        }
    }
}

private fun DrawScope.drawCornerBrackets(
    left: Float,
    top: Float,
    right: Float,
    bottom: Float,
    color: Color,
    strokeWidth: Float,
    cornerLen: Float,
) {
    val len = cornerLen.coerceIn(14.dp.toPx(), 42.dp.toPx())

    drawLine(color, Offset(left, top), Offset(left + len, top), strokeWidth, StrokeCap.Round)
    drawLine(color, Offset(left, top), Offset(left, top + len), strokeWidth, StrokeCap.Round)

    drawLine(color, Offset(right - len, top), Offset(right, top), strokeWidth, StrokeCap.Round)
    drawLine(color, Offset(right, top), Offset(right, top + len), strokeWidth, StrokeCap.Round)

    drawLine(color, Offset(left, bottom - len), Offset(left, bottom), strokeWidth, StrokeCap.Round)
    drawLine(color, Offset(left, bottom), Offset(left + len, bottom), strokeWidth, StrokeCap.Round)

    drawLine(color, Offset(right - len, bottom), Offset(right, bottom), strokeWidth, StrokeCap.Round)
    drawLine(color, Offset(right, bottom - len), Offset(right, bottom), strokeWidth, StrokeCap.Round)
}

private fun DrawScope.drawBadgeLabel(
    textMeasurer: TextMeasurer,
    label: String,
    anchorX: Float,
    anchorY: Float,
    accentColor: Color,
) {
    val style = TextStyle(
        color = ObsidianDeep,
        fontSize = 11.sp,
        fontWeight = FontWeight.Bold,
        fontFamily = JetBrainsMonoFontFamily,
    )
    val layoutResult = textMeasurer.measure(text = label, style = style)
    val padH = 8.dp.toPx()
    val padV = 4.dp.toPx()
    val pillW = layoutResult.size.width + padH * 2f
    val pillH = layoutResult.size.height + padV * 2f

    val badgeLeft = anchorX.coerceIn(
        8.dp.toPx(),
        (size.width - pillW - 8.dp.toPx()).coerceAtLeast(8.dp.toPx()),
    )
    val badgeTop = (anchorY - pillH - 6.dp.toPx()).coerceAtLeast(8.dp.toPx())

    drawRoundRect(
        color = accentColor,
        topLeft = Offset(badgeLeft, badgeTop),
        size = Size(pillW, pillH),
        cornerRadius = CornerRadius(6.dp.toPx(), 6.dp.toPx()),
    )
    drawText(
        textLayoutResult = layoutResult,
        topLeft = Offset(badgeLeft + padH, badgeTop + padV),
    )
}
