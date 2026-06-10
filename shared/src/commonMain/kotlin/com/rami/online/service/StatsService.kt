package com.rami.online.service

import com.rami.online.model.Achievement
import com.rami.online.model.DailyMission
import com.rami.online.model.MissionType
import com.rami.online.model.PlayerStats
import kotlinx.coroutines.flow.Flow

interface StatsService {
    fun observeStats(uid: String): Flow<PlayerStats?>
    fun observeAchievements(uid: String): Flow<List<Achievement>>
    fun observeDailyMissions(uid: String): Flow<List<DailyMission>>

    suspend fun recordWin(uid: String, isOnline: Boolean, isTafdhil: Boolean, score: Int)
    suspend fun recordLoss(uid: String, isOnline: Boolean)

    /** Increment a mission counter; returns list of newly completed missions. */
    suspend fun incrementMission(uid: String, type: MissionType, by: Int = 1): List<DailyMission>

    /** Unlock an achievement if not already unlocked; returns true if it was newly unlocked. */
    suspend fun unlockAchievement(uid: String, achievementId: String): Boolean

    /** Called at start of day to ensure missions exist for today's date key. */
    suspend fun ensureDailyMissions(uid: String, dateKey: String)
}
