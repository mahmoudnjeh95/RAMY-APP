package com.rami.online.model

import kotlinx.serialization.Serializable

@Serializable
data class LeagueEntry(
    val uid: String = "",
    val username: String = "",
    val avatarId: Int = 0,
    val points: Int = 0,
    val wins: Int = 0,
    val losses: Int = 0,
    val tier: LeagueTier = LeagueTier.BRONZE,
    val rank: Int = 0,
    val weekStart: Long = 0L
)

@Serializable
data class LeagueSeason(
    val weekStart: Long = 0L,
    val weekEnd: Long = 0L,
    val isActive: Boolean = true
)
