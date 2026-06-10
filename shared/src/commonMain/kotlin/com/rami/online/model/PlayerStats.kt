package com.rami.online.model

import kotlinx.serialization.Serializable

@Serializable
data class PlayerStats(
    val uid: String = "",
    val gamesPlayed: Int = 0,
    val gamesWon: Int = 0,
    val gamesLost: Int = 0,
    val totalScore: Int = 0,
    val bestScore: Int = 0,
    val currentStreak: Int = 0,
    val bestStreak: Int = 0,
    val normalWins: Int = 0,
    val tafdhilWins: Int = 0,
    val aiGamesPlayed: Int = 0,
    val onlineGamesPlayed: Int = 0,
    val weeklyGamesPlayed: Int = 0,
    val weeklyGamesWon: Int = 0
) {
    val winRate: Float
        get() = if (gamesPlayed == 0) 0f else gamesWon.toFloat() / gamesPlayed
}
