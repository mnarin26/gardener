package com.mustafa.gardener.modules.vision

import android.content.Context

/**
 * Real-path placeholder engine used when no on-device model file is present.
 *
 * Reports clearly that inference is not running. Does not paint fake detections.
 * When a model file exists, [VisionEngineFactory] prefers [TfliteSegVisionEngine] instead.
 */
class UnloadedVisionEngine(
    private val engineState: VisionEngineState = VisionEngineState.UNLOADED,
    private val technicalReason: String = DEFAULT_REASON,
) : VisionEngine {

    init {
        require(
            engineState == VisionEngineState.UNLOADED ||
                engineState == VisionEngineState.NOT_CONFIGURED
        ) {
            "UnloadedVisionEngine only supports UNLOADED or NOT_CONFIGURED (got $engineState)"
        }
    }

    override fun state(): VisionEngineState = engineState

    override fun modelLabel(): String = "none (unloaded)"

    override fun statusDetail(): String =
        "$technicalReason; expected ${VisionModelPaths.describeExpectedLocations()}; " +
            "Phase1 labels: ${VisionLabels.phase1Summary()}; Phase2 later: ${VisionLabels.PHASE2_GRATE}"

    override fun uiStatusTr(): String = "Model yok — sadece önizleme"

    override fun wantsFrameAnalysis(): Boolean = false

    override fun classLabels(): List<String> = VisionLabels.PHASE1

    override fun onFrame(frame: VisionFrame) {
        // Intentionally empty — no model, no inference, no fake boxes.
    }

    companion object {
        const val DEFAULT_REASON: String =
            "model file not present — not running inference"

        /** Explicit "not configured yet" variant (same UI; distinct engine state). */
        fun notConfigured(
            reason: String = "vision runtime not configured — not running inference",
        ): UnloadedVisionEngine = UnloadedVisionEngine(
            engineState = VisionEngineState.NOT_CONFIGURED,
            technicalReason = reason,
        )

        /** File missing → UNLOADED. (Presence is handled by [VisionEngineFactory].) */
        fun forContext(context: Context): UnloadedVisionEngine {
            return if (VisionModelPaths.anyModelFilePresent(context)) {
                notConfigured(
                    reason = "model file present but engine factory not used — not running inference",
                )
            } else {
                UnloadedVisionEngine(
                    engineState = VisionEngineState.UNLOADED,
                    technicalReason = DEFAULT_REASON,
                )
            }
        }
    }
}
