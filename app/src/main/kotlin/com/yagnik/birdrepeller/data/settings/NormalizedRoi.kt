package com.yagnik.birdrepeller.data.settings

import androidx.compose.ui.geometry.Offset

/**
 * Represents a Region of Interest as a Quadrilateral in normalized coordinates (0.0 to 1.0).
 */
data class NormalizedRoi(
    val topLeft: Offset = Offset(0.2f, 0.2f),
    val topRight: Offset = Offset(0.8f, 0.2f),
    val bottomLeft: Offset = Offset(0.2f, 0.8f),
    val bottomRight: Offset = Offset(0.8f, 0.8f)
)
