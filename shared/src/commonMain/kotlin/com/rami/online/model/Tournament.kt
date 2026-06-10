package com.rami.online.model

import com.rami.model.GameMode
import kotlinx.serialization.Serializable

@Serializable
data class Tournament(
    val id: String = "",
    val titleAr: String = "",
    val titleEn: String = "",
    val mode: GameMode = GameMode.NORMAL,
    val maxPlayers: Int = 8,
    val registeredPlayers: List<String> = emptyList(),
    val status: TournamentStatus = TournamentStatus.OPEN,
    val bracket: List<TournamentMatch> = emptyList(),
    val winnerId: String? = null,
    val startTime: Long = 0L,
    val prizeDescription: String = ""
)

@Serializable
data class TournamentMatch(
    val matchId: String = "",
    val round: Int = 0,
    val playerA: String = "",
    val playerB: String = "",
    val winnerId: String? = null,
    val roomId: String = ""
)

@Serializable
enum class TournamentStatus {
    OPEN, IN_PROGRESS, FINISHED
}
