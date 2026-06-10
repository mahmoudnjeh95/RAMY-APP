package com.rami.online.model

import kotlinx.serialization.Serializable

@Serializable
data class Achievement(
    val id: String,
    val titleAr: String,
    val titleEn: String,
    val descAr: String,
    val descEn: String,
    val emoji: String,
    val xpReward: Int,
    val unlockedAt: Long = 0L,
    val isUnlocked: Boolean = false
)

object Achievements {
    val all = listOf(
        Achievement("first_win",       "أول فوز",        "First Win",          "فز بأول لعبة",               "Win your first game",             "🏆", 50),
        Achievement("win_streak_3",    "3 انتصارات",     "Hat-trick",          "فز 3 مرات متتالية",           "Win 3 games in a row",            "🔥", 100),
        Achievement("win_streak_10",   "عشرة بعشرة",    "Unstoppable",        "فز 10 مرات متتالية",          "Win 10 games in a row",           "⚡", 300),
        Achievement("rami_master",     "أستاذ رامي",     "Rami Master",        "فز 100 لعبة",                 "Win 100 games total",             "👑", 500),
        Achievement("league_gold",     "بطل الدوري",     "League Champion",    "وصل لرتبة الذهب",             "Reach Gold tier",                 "🥇", 200),
        Achievement("league_diamond",  "الأفضل",         "Diamond Elite",      "وصل لرتبة الألماس",           "Reach Diamond tier",              "💎", 1000),
        Achievement("beat_hard_ai",    "قهر الآلة",      "AI Slayer",          "فز ضد الذكاء الاصطناعي الصعب","Beat Hard AI",                    "🤖", 150),
        Achievement("private_host",    "مضيف",           "Party Host",         "أنشئ 10 طاولات خاصة",         "Host 10 private tables",          "🎉", 75),
        Achievement("daily_10",        "مواظب",          "Dedicated",          "أكمل 10 مهام يومية",          "Complete 10 daily missions",      "📅", 100),
        Achievement("tournament_win",  "بطل البطولة",    "Tournament Winner",  "فز ببطولة",                   "Win a tournament",                "🏅", 400),
        Achievement("tafdhil_master",  "أستاذ تفضيل",   "Tafdhil Master",     "فز 50 لعبة تفضيل",            "Win 50 Tafdhil games",            "⭐", 250),
        Achievement("comeback_king",   "ملك العودة",     "Comeback King",      "فز وأنت آخر في النقاط",       "Win from last place",             "🔄", 200)
    )

    fun byId(id: String) = all.find { it.id == id }
}
