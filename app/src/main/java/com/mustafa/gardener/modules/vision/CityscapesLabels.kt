package com.mustafa.gardener.modules.vision

import android.graphics.Color

/**
 * Cityscapes 19 train-id labels as shipped with Qualcomm FFNet TFLite.
 *
 * Highlight set maps to Phase-1 outdoor nav interest: road, sidewalk, wall, fence.
 */
object CityscapesLabels {
    val ALL: List<String> = listOf(
        "road",
        "sidewalk",
        "building",
        "wall",
        "fence",
        "pole",
        "traffic light",
        "traffic sign",
        "vegetation",
        "terrain",
        "sky",
        "person",
        "rider",
        "car",
        "truck",
        "bus",
        "train",
        "motorcycle",
        "bicycle",
    )

    /** Indices to paint on the overlay (Phase-1 closest classes). */
    val HIGHLIGHT_INDICES: Set<Int> = setOf(
        ALL.indexOf("road"),
        ALL.indexOf("sidewalk"),
        ALL.indexOf("wall"),
        ALL.indexOf("fence"),
    )

    fun overlayColorArgb(classIndex: Int): Int = when (ALL.getOrNull(classIndex)) {
        "road" -> Color.argb(140, 40, 90, 200)
        "sidewalk" -> Color.argb(140, 220, 170, 40)
        "wall" -> Color.argb(150, 190, 70, 55)
        "fence" -> Color.argb(150, 40, 160, 130)
        else -> Color.TRANSPARENT
    }

    fun isHighlight(classIndex: Int): Boolean = classIndex in HIGHLIGHT_INDICES
}
