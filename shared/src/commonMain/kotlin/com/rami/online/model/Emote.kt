package com.rami.online.model

import kotlinx.serialization.Serializable

@Serializable
data class EmoteEvent(
    val fromUid: String = "",
    val fromUsername: String = "",
    val emote: Emote = Emote.THUMBS_UP,
    val timestamp: Long = 0L
)

enum class Emote(val emoji: String, val labelAr: String, val labelEn: String) {
    THUMBS_UP(  "👍", "أحسنت",    "Nice!"),
    FIRE(       "🔥", "رائع",     "Fire!"),
    LAUGH(      "😂", "هههه",     "Haha"),
    SURPRISED(  "😮", "واو",      "Wow"),
    THINKING(   "🤔", "هممم",     "Hmm"),
    SAD(        "😅", "آسف",      "Oops"),
    CLAP(       "👏", "تصفيق",    "Clap"),
    SUNGLASSES( "😎", "بارد",     "Cool")
}
