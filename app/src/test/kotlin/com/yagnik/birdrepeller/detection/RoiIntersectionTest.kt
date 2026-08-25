package com.yagnik.birdrepeller.detection

import androidx.compose.ui.geometry.Offset
import com.yagnik.birdrepeller.data.settings.NormalizedRoi
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RoiIntersectionTest {

    private val defaultRoi = NormalizedRoi(
        topLeft = Offset(0.2f, 0.2f),
        topRight = Offset(0.8f, 0.2f),
        bottomLeft = Offset(0.2f, 0.8f),
        bottomRight = Offset(0.8f, 0.8f)
    )

    @Test
    fun `full overlap should return true`() {
        // Detection is same as ROI
        assertTrue(RoiIntersection.intersects(0.2f, 0.2f, 0.8f, 0.8f, defaultRoi))
    }

    @Test
    fun `partial overlap should return true`() {
        // Detection overlaps top-left corner
        assertTrue(RoiIntersection.intersects(0.1f, 0.1f, 0.3f, 0.3f, defaultRoi))
    }

    @Test
    fun `no overlap should return false`() {
        // Detection is to the right
        assertFalse(RoiIntersection.intersects(0.9f, 0.2f, 1.0f, 0.8f, defaultRoi))
    }

    @Test
    fun `detection fully inside ROI should return true`() {
        assertTrue(RoiIntersection.intersects(0.4f, 0.4f, 0.6f, 0.6f, defaultRoi))
    }

    @Test
    fun `detection fully containing ROI should return true`() {
        assertTrue(RoiIntersection.intersects(0.0f, 0.0f, 1.0f, 1.0f, defaultRoi))
    }

    @Test
    fun `edge touching should return false`() {
        // Just touching the right edge (using < and > in code, so exact touch is false)
        assertFalse(RoiIntersection.intersects(0.8f, 0.2f, 0.9f, 0.8f, defaultRoi))
    }
}
