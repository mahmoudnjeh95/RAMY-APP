package com.rami.online.model

import kotlinx.serialization.Serializable

@Serializable
data class OnlinePlayer(
    val uid: String = "",
    val username: String = "",
    val avatarId: Int = 0,
    val rating: Int = 1000,
    val leaguePoints: Int = 0,
    val leagueTier: LeagueTier = LeagueTier.BRONZE,
    val isOnline: Boolean = false,
    val fcmToken: String = ""
)

@Serializable
enum class LeagueTier(val displayAr: String, val displayEn: String, val minPoints: Int) {
    BRONZE(  "برونز",   "Bronze",   0),
    SILVER(  "فضة",     "Silver",   500),
    GOLD(    "ذهب",     "Gold",     1000),
    PLATINUM("بلاتين",  "Platinum", 2000),
    DIAMOND( "ألماس",   "Diamond",  3500)
}
