package com.rami.online.service

import com.rami.model.GameMode
import com.rami.online.model.EmoteEvent
import com.rami.online.model.GameRoom
import kotlinx.coroutines.flow.Flow

interface RoomService {
    /** Live stream of a room's state. */
    fun observeRoom(roomId: String): Flow<GameRoom?>

    /** Create a private room and return it. */
    suspend fun createPrivateRoom(mode: GameMode, scoreLimit: Int, maxPlayers: Int): Result<GameRoom>

    /** Join a private room via 6-digit invite code. */
    suspend fun joinByCode(code: String): Result<GameRoom>

    /** Enter the matchmaking queue; emits the room once matched. */
    suspend fun findMatch(mode: GameMode): Flow<GameRoom?>

    /** Cancel matchmaking. */
    suspend fun cancelMatchmaking()

    /** Mark yourself ready in the waiting room. */
    suspend fun setReady(roomId: String, ready: Boolean)

    /** Host starts the game (all players must be ready). */
    suspend fun startGame(roomId: String): Result<Unit>

    /** Push an updated serialized game state to Firestore. */
    suspend fun pushGameState(roomId: String, stateJson: String)

    /** Send an emote reaction. */
    suspend fun sendEmote(roomId: String, event: EmoteEvent)

    /** Observe emotes in real time. */
    fun observeEmotes(roomId: String): Flow<EmoteEvent?>

    /** Record a timeout strike; returns new strike count. */
    suspend fun recordStrike(roomId: String, uid: String): Int

    /** Remove a player who hit 3 strikes. */
    suspend fun kickPlayer(roomId: String, uid: String)

    /** Mark the game finished with the winner's UID. */
    suspend fun finishGame(roomId: String, winnerUid: String)

    /** Join as a spectator. */
    suspend fun joinAsSpectator(roomId: String)

    /** Leave spectator mode. */
    suspend fun leaveSpectator(roomId: String)
}
