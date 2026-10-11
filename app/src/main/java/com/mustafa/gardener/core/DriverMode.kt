package com.mustafa.gardener.core

/**
 * How a module is backed right now.
 *
 * SIMULATION = stub/fake data only. Never treat as hardware evidence.
 * REAL = future live driver (not wired in this skeleton).
 */
enum class DriverMode {
    SIMULATION,
    REAL,
}
