package com.mustafa.gardener.core

/**
 * Lightweight status snapshot for UI / diagnostics.
 * Values are descriptive only — not proof of hardware readiness.
 */
data class ModuleStatus(
    val moduleName: String,
    val mode: DriverMode,
    val running: Boolean,
    val detail: String,
)
