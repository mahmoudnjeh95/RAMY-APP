package com.rami.online.model

import kotlinx.serialization.Serializable

@Serializable
data class FriendRequest(
    val id: String = "",
    val fromUid: String = "",
    val fromUsername: String = "",
    val toUid: String = "",
    val status: FriendRequestStatus = FriendRequestStatus.PENDING,
    val createdAt: Long = 0L
)

@Serializable
enum class FriendRequestStatus { PENDING, ACCEPTED, DECLINED }

@Serializable
data class Friend(
    val uid: String = "",
    val username: String = "",
    val avatarId: Int = 0,
    val isOnline: Boolean = false,
    val leagueTier: LeagueTier = LeagueTier.BRONZE,
    val rating: Int = 1000
)
