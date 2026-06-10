package com.rami.online.service

import com.rami.online.model.LeagueEntry
import com.rami.online.model.LeagueSeason
import kotlinx.coroutines.flow.Flow

interface LeagueService {
    /** Live top-100 leaderboard for the current week. */
    fun observeLeaderboard(limit: Int = 100): Flow<List<LeagueEntry>>

    /** Live entry for a specific player this week. */
    fun observeMyEntry(uid: String): Flow<LeagueEntry?>

    /** Add points after a win. */
    suspend fun addPoints(uid: String, points: Int)

    /** Deduct points after a loss. */
    suspend fun deductPoints(uid: String, points: Int)

    /** Get current active season. */
    suspend fun getCurrentSeason(): LeagueSeason?

    /** Archive the current week and start a new season (called by Cloud Function, but exposed for admin). */
    suspend fun rolloverSeason()
}

object LeaguePointRules {
    const val WIN_ONLINE   = 25
    const val LOSS_ONLINE  = -10
    const val WIN_PRIVATE  = 10
    const val LOSS_PRIVATE = 0
}
