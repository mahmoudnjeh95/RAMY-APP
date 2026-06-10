package com.rami.online.service

import com.rami.model.GameMode
import com.rami.online.model.EmoteEvent
import com.rami.online.model.GameRoom
import com.rami.online.model.RoomSlot
import com.rami.online.model.RoomStatus
import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.auth.auth
import dev.gitlive.firebase.database.database
import dev.gitlive.firebase.firestore.firestore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class FirebaseRoomService : RoomService {

    private val db       get() = Firebase.firestore
    private val rtdb     get() = Firebase.database
    private val auth     get() = Firebase.auth
    private val uid      get() = auth.currentUser?.uid ?: error("Not signed in")

    private fun roomsRef()    = db.collection("rooms")
    private fun roomRef(id: String) = roomsRef().document(id)
    private fun emotesRef(id: String) = rtdb.reference("emotes/$id")
    private fun queueRef()    = db.collection("matchmaking")

    override fun observeRoom(roomId: String): Flow<GameRoom?> =
        roomRef(roomId).snapshots.map { it.data<GameRoom?>() }

    override suspend fun createPrivateRoom(mode: GameMode, scoreLimit: Int, maxPlayers: Int): Result<GameRoom> =
        runCatching {
            val code = generateCode()
            val slot = RoomSlot(uid = uid, username = fetchUsername(), isReady = false)
            val room = GameRoom(
                roomId      = "",
                inviteCode  = code,
                hostUid     = uid,
                mode        = mode,
                scoreLimit  = scoreLimit,
                maxPlayers  = maxPlayers,
                status      = RoomStatus.WAITING,
                players     = mapOf(uid to slot),
                isPrivate   = true,
                createdAt   = System.currentTimeMillis()
            )
            val ref = roomsRef().add(room)
            val withId = room.copy(roomId = ref.id)
            roomRef(ref.id).set(withId)
            withId
        }

    override suspend fun joinByCode(code: String): Result<GameRoom> = runCatching {
        val query = roomsRef()
            .where { "inviteCode" equalTo code }
            .where { "status" equalTo RoomStatus.WAITING.name }
            .limit(1)
            .get()
        val snap = query.documents.firstOrNull() ?: error("Room not found or already started")
        val room = snap.data<GameRoom>()
        val slot = RoomSlot(uid = uid, username = fetchUsername())
        roomRef(room.roomId).update("players.$uid" to slot)
        room.copy(players = room.players + (uid to slot))
    }

    override suspend fun findMatch(mode: GameMode): Flow<GameRoom?> {
        val entry = mapOf("uid" to uid, "mode" to mode.name, "joinedAt" to System.currentTimeMillis())
        queueRef().document(uid).set(entry)
        return queueRef()
            .where { "mode" equalTo mode.name }
            .snapshots
            .map { snap ->
                if (snap.documents.size >= 2) {
                    val uids = snap.documents.take(4).map { it.id }
                    if (uids.contains(uid)) {
                        uids.forEach { queueRef().document(it).delete() }
                        createRankedRoom(mode, uids).getOrNull()
                    } else null
                } else null
            }
    }

    override suspend fun cancelMatchmaking() {
        queueRef().document(uid).delete()
    }

    override suspend fun setReady(roomId: String, ready: Boolean) {
        roomRef(roomId).update("players.$uid.isReady" to ready)
    }

    override suspend fun startGame(roomId: String): Result<Unit> = runCatching {
        roomRef(roomId).update(
            "status"    to RoomStatus.IN_GAME.name,
            "startedAt" to System.currentTimeMillis()
        )
    }

    override suspend fun pushGameState(roomId: String, stateJson: String) {
        roomRef(roomId).update("gameStateJson" to stateJson)
    }

    override suspend fun sendEmote(roomId: String, event: EmoteEvent) {
        emotesRef(roomId).push().setValue(event)
    }

    override fun observeEmotes(roomId: String): Flow<EmoteEvent?> =
        emotesRef(roomId).valueEvents.map { it.value<EmoteEvent?>() }

    override suspend fun recordStrike(roomId: String, uid: String): Int {
        val doc = roomRef(roomId).get().data<GameRoom>() ?: return 0
        val current = doc.players[uid]?.strikeCount ?: 0
        val updated = current + 1
        roomRef(roomId).update("players.$uid.strikeCount" to updated)
        return updated
    }

    override suspend fun kickPlayer(roomId: String, uid: String) {
        roomRef(roomId).update("players.$uid.isConnected" to false)
    }

    override suspend fun finishGame(roomId: String, winnerUid: String) {
        roomRef(roomId).update("status" to RoomStatus.FINISHED.name)
    }

    override suspend fun joinAsSpectator(roomId: String) {
        val doc = roomRef(roomId).get().data<GameRoom>() ?: return
        roomRef(roomId).set(doc.copy(spectators = doc.spectators + uid))
    }

    override suspend fun leaveSpectator(roomId: String) {
        val doc = roomRef(roomId).get().data<GameRoom>() ?: return
        roomRef(roomId).set(doc.copy(spectators = doc.spectators - uid))
    }

    private suspend fun createRankedRoom(mode: GameMode, uids: List<String>): Result<GameRoom> =
        runCatching {
            val slots = uids.associateWith { RoomSlot(uid = it, username = it, isReady = true) }
            val room = GameRoom(
                inviteCode = generateCode(),
                hostUid    = uids.first(),
                mode       = mode,
                status     = RoomStatus.STARTING,
                players    = slots,
                createdAt  = System.currentTimeMillis()
            )
            val ref = roomsRef().add(room)
            val withId = room.copy(roomId = ref.id)
            roomRef(ref.id).set(withId)
            withId
        }

    private suspend fun fetchUsername(): String =
        db.collection("players").document(uid).get()
            .data<Map<String, String>>()?.get("username") ?: "Player"

    private fun generateCode(): String =
        (100_000..999_999).random().toString()
}
