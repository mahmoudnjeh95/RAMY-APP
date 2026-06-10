package com.rami.online.engine

import com.rami.online.model.MAX_STRIKES
import com.rami.online.model.TURN_DURATION_SECONDS
import com.rami.online.model.StrikeRecord
import com.rami.online.model.TurnTimer
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class TurnTimerEngine(private val scope: CoroutineScope) {

    private val _timer = MutableStateFlow(TurnTimer())
    val timer: StateFlow<TurnTimer> = _timer.asStateFlow()

    private val _strikes = MutableStateFlow<Map<String, StrikeRecord>>(emptyMap())
    val strikes: StateFlow<Map<String, StrikeRecord>> = _strikes.asStateFlow()

    private val _timedOutPlayer = MutableStateFlow<String?>(null)
    val timedOutPlayer: StateFlow<String?> = _timedOutPlayer.asStateFlow()

    private var tickJob: Job? = null

    fun startFor(playerId: String) {
        tickJob?.cancel()
        _timedOutPlayer.value = null
        _timer.value = TurnTimer(
            remainingSeconds = TURN_DURATION_SECONDS,
            isRunning = true,
            activePlayerId = playerId
        )
        tickJob = scope.launch {
            var remaining = TURN_DURATION_SECONDS
            while (remaining > 0) {
                delay(1_000)
                remaining--
                _timer.value = _timer.value.copy(remainingSeconds = remaining)
            }
            onTimeout(playerId)
        }
    }

    fun stop() {
        tickJob?.cancel()
        _timer.value = _timer.value.copy(isRunning = false, remainingSeconds = TURN_DURATION_SECONDS)
        _timedOutPlayer.value = null
    }

    private fun onTimeout(playerId: String) {
        _timer.value = _timer.value.copy(isRunning = false)
        val current = _strikes.value[playerId] ?: StrikeRecord(uid = playerId)
        val updated = current.copy(strikes = current.strikes + 1)
        _strikes.value = _strikes.value + (playerId to updated)
        _timedOutPlayer.value = playerId
    }

    fun isKicked(playerId: String): Boolean =
        (_strikes.value[playerId]?.strikes ?: 0) >= MAX_STRIKES

    fun strikeCount(playerId: String): Int =
        _strikes.value[playerId]?.strikes ?: 0

    fun resetStrikes(playerId: String) {
        _strikes.value = _strikes.value - playerId
    }

    fun acknowledgeTimeout() {
        _timedOutPlayer.value = null
    }
}
