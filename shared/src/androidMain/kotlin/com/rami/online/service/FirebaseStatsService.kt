package com.rami.online.service

import com.rami.online.model.Achievement
import com.rami.online.model.Achievements
import com.rami.online.model.DailyMission
import com.rami.online.model.DailyMissions
import com.rami.online.model.MissionType
import com.rami.online.model.PlayerStats
import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.firestore.firestore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class FirebaseStatsService : StatsService {

    private val db get() = Firebase.firestore

    private fun statsRef(uid: String)        = db.collection("stats").document(uid)
    private fun achievementsRef(uid: String) = db.collection("achievements").document(uid).collection("unlocked")
    private fun missionsRef(uid: String)     = db.collection("missions").document(uid).collection("daily")

    override fun observeStats(uid: String): Flow<PlayerStats?> =
        statsRef(uid).snapshots.map { it.data<PlayerStats?>() }

    override fun observeAchievements(uid: String): Flow<List<Achievement>> =
        achievementsRef(uid).snapshots.map { snap ->
            val unlockedIds = snap.documents.map { it.id }.toSet()
            Achievements.all.map { a -> a.copy(isUnlocked = a.id in unlockedIds) }
        }

    override fun observeDailyMissions(uid: String): Flow<List<DailyMission>> =
        missionsRef(uid).snapshots.map { snap ->
            snap.documents.mapNotNull { it.data<DailyMission?>() }
        }

    override suspend fun recordWin(uid: String, isOnline: Boolean, isTafdhil: Boolean, score: Int) {
        val ref = statsRef(uid)
        val stats = ref.get().data<PlayerStats?>() ?: PlayerStats(uid = uid)
        val streak = stats.currentStreak + 1
        ref.set(stats.copy(
            gamesPlayed       = stats.gamesPlayed + 1,
            gamesWon          = stats.gamesWon + 1,
            totalScore        = stats.totalScore + score,
            bestScore         = maxOf(stats.bestScore, score),
            currentStreak     = streak,
            bestStreak        = maxOf(stats.bestStreak, streak),
            normalWins        = if (!isTafdhil) stats.normalWins + 1 else stats.normalWins,
            tafdhilWins       = if (isTafdhil) stats.tafdhilWins + 1 else stats.tafdhilWins,
            onlineGamesPlayed = if (isOnline) stats.onlineGamesPlayed + 1 else stats.onlineGamesPlayed,
            weeklyGamesPlayed = stats.weeklyGamesPlayed + 1,
            weeklyGamesWon    = stats.weeklyGamesWon + 1
        ))
        checkAchievements(uid, stats)
    }

    override suspend fun recordLoss(uid: String, isOnline: Boolean) {
        val ref = statsRef(uid)
        val stats = ref.get().data<PlayerStats?>() ?: PlayerStats(uid = uid)
        ref.set(stats.copy(
            gamesPlayed       = stats.gamesPlayed + 1,
            gamesLost         = stats.gamesLost + 1,
            currentStreak     = 0,
            onlineGamesPlayed = if (isOnline) stats.onlineGamesPlayed + 1 else stats.onlineGamesPlayed,
            weeklyGamesPlayed = stats.weeklyGamesPlayed + 1
        ))
    }

    override suspend fun incrementMission(uid: String, type: MissionType, by: Int): List<DailyMission> {
        val snap = missionsRef(uid).where { "type" equalTo type.name }.get()
        val completed = mutableListOf<DailyMission>()
        snap.documents.forEach { doc ->
            val mission = doc.data<DailyMission>() ?: return@forEach
            if (mission.isCompleted) return@forEach
            val newCurrent = mission.current + by
            val nowComplete = newCurrent >= mission.target
            doc.reference.update("current" to newCurrent, "isCompleted" to nowComplete)
            if (nowComplete) completed.add(mission.copy(current = newCurrent, isCompleted = true))
        }
        return completed
    }

    override suspend fun unlockAchievement(uid: String, achievementId: String): Boolean {
        val ref = achievementsRef(uid).document(achievementId)
        if (ref.get().exists) return false
        val achievement = Achievements.byId(achievementId) ?: return false
        ref.set(achievement.copy(isUnlocked = true, unlockedAt = System.currentTimeMillis()))
        return true
    }

    override suspend fun ensureDailyMissions(uid: String, dateKey: String) {
        val existing = missionsRef(uid).where { "dateKey" equalTo dateKey }.get()
        if (existing.documents.isNotEmpty()) return
        DailyMissions.generateForDay(dateKey).forEach { mission ->
            missionsRef(uid).document(mission.id).set(mission)
        }
    }

    private suspend fun checkAchievements(uid: String, stats: PlayerStats) {
        if (stats.gamesWon == 0) unlockAchievement(uid, "first_win")
        if (stats.currentStreak >= 3) unlockAchievement(uid, "win_streak_3")
        if (stats.currentStreak >= 10) unlockAchievement(uid, "win_streak_10")
        if (stats.gamesWon >= 100) unlockAchievement(uid, "rami_master")
        if (stats.tafdhilWins >= 50) unlockAchievement(uid, "tafdhil_master")
    }
}
