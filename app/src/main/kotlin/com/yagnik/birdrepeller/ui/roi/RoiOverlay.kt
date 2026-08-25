package com.yagnik.birdrepeller.ui.roi

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import com.yagnik.birdrepeller.data.settings.NormalizedRoi

enum class DragType {
    TopLeft, TopRight, BottomLeft, BottomRight
}

@Composable
fun RoiOverlay(
    roi: NormalizedRoi,
    onRoiChange: (NormalizedRoi) -> Unit,
    onRoiSave: () -> Unit,
    isEnabled: Boolean,
    modifier: Modifier = Modifier
) {
    var dragType by remember { mutableStateOf<DragType?>(null) }
    val currentRoi by rememberUpdatedState(roi)

    Box(modifier = modifier.fillMaxSize()) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .then(
                    if (isEnabled) {
                        Modifier.pointerInput(Unit) {
                            detectDragGestures(
                                onDragStart = { offset ->
                                    val canvasWidth = size.width.toFloat()
                                    val canvasHeight = size.height.toFloat()
                                    
                                    val handleSize = 40.dp.toPx()
                                    val r = currentRoi
                                    
                                    dragType = when {
                                        (offset - Offset(r.topLeft.x * canvasWidth, r.topLeft.y * canvasHeight)).getDistance() < handleSize -> DragType.TopLeft
                                        (offset - Offset(r.topRight.x * canvasWidth, r.topRight.y * canvasHeight)).getDistance() < handleSize -> DragType.TopRight
                                        (offset - Offset(r.bottomLeft.x * canvasWidth, r.bottomLeft.y * canvasHeight)).getDistance() < handleSize -> DragType.BottomLeft
                                        (offset - Offset(r.bottomRight.x * canvasWidth, r.bottomRight.y * canvasHeight)).getDistance() < handleSize -> DragType.BottomRight
                                        else -> null
                                    }
                                },
                                onDragEnd = { 
                                    dragType = null
                                },
                                onDragCancel = { 
                                    dragType = null
                                },
                                onDrag = { change, dragAmount ->
                                    if (dragType == null) return@detectDragGestures
                                    change.consume()
                                    
                                    val canvasWidth = size.width.toFloat()
                                    val canvasHeight = size.height.toFloat()
                                    
                                    val dx = dragAmount.x / canvasWidth
                                    val dy = dragAmount.y / canvasHeight

                                    val r = currentRoi
                                    val updatedRoi = when (dragType) {
                                        DragType.TopLeft -> r.copy(topLeft = Offset((r.topLeft.x + dx).coerceIn(0f, 1f), (r.topLeft.y + dy).coerceIn(0f, 1f)))
                                        DragType.TopRight -> r.copy(topRight = Offset((r.topRight.x + dx).coerceIn(0f, 1f), (r.topRight.y + dy).coerceIn(0f, 1f)))
                                        DragType.BottomLeft -> r.copy(bottomLeft = Offset((r.bottomLeft.x + dx).coerceIn(0f, 1f), (r.bottomLeft.y + dy).coerceIn(0f, 1f)))
                                        DragType.BottomRight -> r.copy(bottomRight = Offset((r.bottomRight.x + dx).coerceIn(0f, 1f), (r.bottomRight.y + dy).coerceIn(0f, 1f)))
                                        null -> r
                                    }

                                    onRoiChange(updatedRoi)
                                }
                            )
                        }
                    } else Modifier
                )
        ) {
            val canvasWidth = size.width
            val canvasHeight = size.height

            val tl = Offset(roi.topLeft.x * canvasWidth, roi.topLeft.y * canvasHeight)
            val tr = Offset(roi.topRight.x * canvasWidth, roi.topRight.y * canvasHeight)
            val bl = Offset(roi.bottomLeft.x * canvasWidth, roi.bottomLeft.y * canvasHeight)
            val br = Offset(roi.bottomRight.x * canvasWidth, roi.bottomRight.y * canvasHeight)

            // Draw Quadrilateral Path
            val path = Path().apply {
                moveTo(tl.x, tl.y)
                lineTo(tr.x, tr.y)
                lineTo(br.x, br.y)
                lineTo(bl.x, bl.y)
                close()
            }

            val strokeColor = if (isEnabled) Color.Yellow else Color.Yellow.copy(alpha = 0.5f)
            val fillColor = if (isEnabled) Color.Yellow.copy(alpha = 0.2f) else Color.Transparent

            drawPath(path, fillColor, style = Fill)
            drawPath(path, strokeColor, style = Stroke(width = if (isEnabled) 4f else 2f))

            if (isEnabled) {
                // Draw handles
                val handleRadius = 12.dp.toPx()
                val handleColor = Color.Yellow
                drawCircle(handleColor, handleRadius, tl)
                drawCircle(handleColor, handleRadius, tr)
                drawCircle(handleColor, handleRadius, bl)
                drawCircle(handleColor, handleRadius, br)
            }
        }
    }
}
