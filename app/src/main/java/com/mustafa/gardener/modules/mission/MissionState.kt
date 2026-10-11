package com.mustafa.gardener.modules.mission

/**
 * Target mission states from project context.
 * Transitions in this skeleton are SIM-only stub events.
 */
enum class MissionState {
    IDLE,
    GO_TO_GARDEN,
    ENTER_GARDEN,
    MAP_GARDEN,
    MOW,
    RETURN_TO_GATE,
    RETURN_TO_STATION,
    CHARGE,
    EMPTY_GRASS,
    WAITING,
    ERROR,
    EMERGENCY_STOP,
}
