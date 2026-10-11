package com.mustafa.gardener.modules.vision

import android.graphics.Bitmap

/**
 * Latest semantic mask ready for Compose / Canvas overlay.
 *
 * [bitmap] is ARGB_8888 at model mask resolution with transparent background;
 * only highlight classes (road / sidewalk / wall / fence) are painted.
 */
data class SegmentationOverlay(
    val bitmap: Bitmap,
    val maskWidth: Int,
    val maskHeight: Int,
    val timestampNs: Long,
    val classPixelCounts: Map<String, Int>,
)
