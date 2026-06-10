package com.rami.online.service

import com.rami.online.model.Friend
import com.rami.online.model.FriendRequest
import kotlinx.coroutines.flow.Flow

interface FriendService {
    fun observeFriends(uid: String): Flow<List<Friend>>
    fun observePendingRequests(uid: String): Flow<List<FriendRequest>>

    suspend fun sendFriendRequest(toUid: String): Result<Unit>
    suspend fun acceptRequest(requestId: String): Result<Unit>
    suspend fun declineRequest(requestId: String): Result<Unit>
    suspend fun removeFriend(friendUid: String): Result<Unit>
    suspend fun searchByUsername(query: String): List<Friend>

    /** Invite a friend directly into a private room. */
    suspend fun inviteToRoom(friendUid: String, roomId: String, inviteCode: String): Result<Unit>
}
