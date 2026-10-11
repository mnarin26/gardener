package com.mustafa.gardener.modules.vision

/**
 * Backing state of a [VisionEngine].
 */
enum class VisionEngineState {
    /** Simulation stub — no real inference. */
    SIM,

    /** Real path chosen but no model configured / file missing. */
    NOT_CONFIGURED,

    /** Model path known but file not on device (same user-facing outcome as NOT_CONFIGURED). */
    UNLOADED,

    /** Model file present but Interpreter / load failed. */
    FAILED,

    /** Model loaded; analysis frames may run inference. */
    READY,
}
