package com.mustafa.gardener.modules.vision

/**
 * Backing state of a [VisionEngine].
 *
 * READY is reserved for a future loaded runtime — not used until a model is wired.
 */
enum class VisionEngineState {
    /** Simulation stub — no real inference. */
    SIM,

    /** Real path chosen but no model configured / file missing. */
    NOT_CONFIGURED,

    /** Model path known but file not on device (same user-facing outcome as NOT_CONFIGURED). */
    UNLOADED,

    /** Reserved: model loaded and inference path active. Not claimed in this scaffold. */
    READY,
}
