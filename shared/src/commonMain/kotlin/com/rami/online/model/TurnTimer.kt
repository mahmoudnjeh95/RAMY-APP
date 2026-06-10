package com.rami.online.model

import kotlinx.serialization.Serializable

const val TURN_DURATION_SECONDS = 30
const val MAX_STRIKES = 3

@Serializable
data class TurnTimer(
    val remainingSeconds: Int = TURN_DURATION_SECONDS,
    val isRunning: Boolean = false,
    val activePlayerId: String = ""
)

@Serializable
data class StrikeRecord(
    val uid: String = "",
    val strikes: Int = 0,
    val isKicked: Boolean = false
) {
    val hasWarning: Boolean get() = strikes in 1 until MAX_STRIKES
    val shouldKick: Boolean get() = strikes >= MAX_STRIKES
}
