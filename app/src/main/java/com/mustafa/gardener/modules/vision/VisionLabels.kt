package com.mustafa.gardener.modules.vision

/**
 * Phase-1 outdoor navigation class names for a future YOLO-seg (or similar) model.
 *
 * Matches the fine-tune target in docs: wall / sidewalk / road / fence.
 * `grate` (ızgara) is intentionally deferred to Phase 2 — not in this list.
 */
object VisionLabels {
    /** Phase 1 labels expected after fine-tune + export. */
    val PHASE1: List<String> = listOf(
        "wall",
        "sidewalk",
        "road",
        "fence",
    )

    /** Documented Phase 2 addition — not loaded or claimed present. */
    const val PHASE2_GRATE: String = "grate"

    fun phase1Summary(): String = PHASE1.joinToString(", ")
}
