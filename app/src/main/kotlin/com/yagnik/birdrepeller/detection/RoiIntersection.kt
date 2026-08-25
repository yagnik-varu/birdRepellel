package com.yagnik.birdrepeller.detection

import com.yagnik.birdrepeller.data.settings.NormalizedRoi

/**
 * Logic for checking if a detection bounding box intersects with the Region of Interest.
 */
object RoiIntersection {

    /**
     * Checks if the given bounding box intersects with the ROI.
     * 
     * OVERLAP RULE: Any overlap (Standard AABB intersection).
     * This rule treats the quadrilateral ROI as its axis-aligned bounding box (AABB) 
     * for simplicity in Phase 5. This is an open tuning parameter for Phase 9.
     */
    fun intersects(
        boxLeft: Float, boxTop: Float, boxRight: Float, boxBottom: Float,
        roi: NormalizedRoi
    ): Boolean {
        // Calculate AABB of the quadrilateral ROI
        val roiMinX = minOf(roi.topLeft.x, roi.topRight.x, roi.bottomLeft.x, roi.bottomRight.x)
        val roiMaxX = maxOf(roi.topLeft.x, roi.topRight.x, roi.bottomLeft.x, roi.bottomRight.x)
        val roiMinY = minOf(roi.topLeft.y, roi.topRight.y, roi.bottomLeft.y, roi.bottomRight.y)
        val roiMaxY = maxOf(roi.topLeft.y, roi.topRight.y, roi.bottomLeft.y, roi.bottomRight.y)

        // Standard AABB intersection test
        return boxLeft < roiMaxX && 
               boxRight > roiMinX && 
               boxTop < roiMaxY && 
               boxBottom > roiMinY
    }
}
