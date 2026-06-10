package com.rami.online.service

import com.rami.online.model.Friend
import com.rami.online.model.FriendRequest
import com.rami.online.model.FriendRequestStatus
import com.rami.online.model.OnlinePlayer
import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.auth.auth
import dev.gitlive.firebase.firestore.firestore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class FirebaseFriendService : FriendService {

    private val db   get() = Firebase.firestore
    private val auth get() = Firebase.auth
    private val uid  get() = auth.currentUser?.uid ?: error("Not signed in")

    private fun friendsRef(uid: String)   = db.collection("friends").document(uid).collection("list")
    private fun requestsRef(uid: String)  = db.collection("friendRequests").document(uid).collection("incoming")
    private fun playersRef()              = db.collection("players")

    override fun observeFriends(uid: String): Flow<List<Friend>> =
        friendsRef(uid).snapshots.map { snap ->
            snap.documents.mapNotNull { it.data<Friend?>() }
        }

    override fun observePendingRequests(uid: String): Flow<List<FriendRequest>> =
        requestsRef(uid).where { "status" equalTo FriendRequestStatus.PENDING.name }
            .snapshots.map { snap ->
                snap.documents.mapNotNull { it.data<FriendRequest?>() }
            }

    override suspend fun sendFriendRequest(toUid: String): Result<Unit> = runCatching {
        val me = playersRef().document(uid).get().data<OnlinePlayer>()
            ?: error("Player not found")
        val request = FriendRequest(
            id           = "",
            fromUid      = uid,
            fromUsername = me.username,
            toUid        = toUid,
            status       = FriendRequestStatus.PENDING,
            createdAt    = System.currentTimeMillis()
        )
        val ref = requestsRef(toUid).add(request)
        requestsRef(toUid).document(ref.id).update("id" to ref.id)
    }

    override suspend fun acceptRequest(requestId: String): Result<Unit> = runCatching {
        val ref = requestsRef(uid).document(requestId)
        val request = ref.get().data<FriendRequest>() ?: error("Request not found")
        ref.update("status" to FriendRequestStatus.ACCEPTED.name)
        val them = playersRef().document(request.fromUid).get().data<OnlinePlayer>()
        val me   = playersRef().document(uid).get().data<OnlinePlayer>()
        if (them != null && me != null) {
            friendsRef(uid).document(them.uid).set(
                Friend(uid = them.uid, username = them.username, avatarId = them.avatarId, rating = them.rating)
            )
            friendsRef(them.uid).document(uid).set(
                Friend(uid = uid, username = me.username, avatarId = me.avatarId, rating = me.rating)
            )
        }
    }

    override suspend fun declineRequest(requestId: String): Result<Unit> = runCatching {
        requestsRef(uid).document(requestId).update("status" to FriendRequestStatus.DECLINED.name)
    }

    override suspend fun removeFriend(friendUid: String): Result<Unit> = runCatching {
        friendsRef(uid).document(friendUid).delete()
        friendsRef(friendUid).document(uid).delete()
    }

    override suspend fun searchByUsername(query: String): List<Friend> {
        val snap = playersRef()
            .where { "username" greaterThanOrEqualTo query }
            .where { "username" lessThan query + "" }
            .limit(20)
            .get()
        return snap.documents.mapNotNull { doc ->
            doc.data<OnlinePlayer?>()?.let {
                Friend(uid = it.uid, username = it.username, avatarId = it.avatarId, rating = it.rating)
            }
        }
    }

    override suspend fun inviteToRoom(friendUid: String, roomId: String, inviteCode: String): Result<Unit> =
        runCatching {
            val notification = mapOf(
                "type"       to "ROOM_INVITE",
                "roomId"     to roomId,
                "inviteCode" to inviteCode,
                "fromUid"    to uid,
                "createdAt"  to System.currentTimeMillis()
            )
            db.collection("notifications").document(friendUid).collection("inbox").add(notification)
        }
}
