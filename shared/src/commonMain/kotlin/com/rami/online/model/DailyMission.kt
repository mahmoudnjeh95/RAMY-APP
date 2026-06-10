package com.rami.online.model

import kotlinx.serialization.Serializable

@Serializable
data class DailyMission(
    val id: String,
    val titleAr: String,
    val titleEn: String,
    val descAr: String,
    val descEn: String,
    val target: Int,
    val current: Int = 0,
    val xpReward: Int,
    val type: MissionType,
    val isCompleted: Boolean = false,
    val dateKey: String = ""
) {
    val progress: Float get() = if (target == 0) 1f else (current.toFloat() / target).coerceIn(0f, 1f)
}

@Serializable
enum class MissionType {
    WIN_GAMES, PLAY_GAMES, WIN_VS_AI, WIN_ONLINE, PLAY_TAFDHIL, PLAY_NORMAL
}

object DailyMissions {
    fun generateForDay(dateKey: String): List<DailyMission> = listOf(
        DailyMission("daily_win_2",    "انتصاران",     "Two Wins",          "فز بلعبتين اليوم",      "Win 2 games today",           2,  xpReward = 50,  type = MissionType.WIN_GAMES,    dateKey = dateKey),
        DailyMission("daily_play_3",   "لاعب نشيط",   "Active Player",     "العب 3 لعبات اليوم",    "Play 3 games today",          3,  xpReward = 30,  type = MissionType.PLAY_GAMES,   dateKey = dateKey),
        DailyMission("daily_beat_ai",  "قهر الروبوت", "Beat the Bot",      "اهزم الذكاء الاصطناعي", "Beat an AI opponent",         1,  xpReward = 40,  type = MissionType.WIN_VS_AI,    dateKey = dateKey)
    )
}
