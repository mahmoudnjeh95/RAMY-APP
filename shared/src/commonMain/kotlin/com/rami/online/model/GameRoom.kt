package com.rami.online.model

import com.rami.model.GameMode
import kotlinx.serialization.Serializable

@Serializable
data class GameRoom(
    val roomId: String = "",
    val inviteCode: String = "",
    val hostUid: String = "",
    val mode: GameMode = GameMode.NORMAL,
    val scoreLimit: Int = 150,
    val maxPlayers: Int = 4,
    val status: RoomStatus = RoomStatus.WAITING,
    val players: Map<String, RoomSlot> = emptyMap(),
    val spectators: List<String> = emptyList(),
    val isPrivate: Boolean = false,
    val createdAt: Long = 0L,
    val startedAt: Long = 0L,
    val gameStateJson: String = ""
)

@Serializable
data class RoomSlot(
    val uid: String = "",
    val username: String = "",
    val avatarId: Int = 0,
    val isReady: Boolean = false,
    val isAi: Boolean = false,
    val strikeCount: Int = 0,
    val isConnected: Boolean = true
)

@Serializable
enum class RoomStatus {
    WAITING, STARTING, IN_GAME, FINISHED
}
