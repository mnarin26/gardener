package com.mustafa.gardener.core

/**
 * How a module is backed right now.
 *
 * SIMULATION = stub/fake data only. Never treat as hardware evidence.
 * REAL = live driver path (CameraX preview exists; device success unverified until phone test).
 */
enum class DriverMode {
    SIMULATION,
    REAL,
}
