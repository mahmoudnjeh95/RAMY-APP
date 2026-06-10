package com.rami.model

import kotlin.random.Random

// ─── Suit ────────────────────────────────────────────────────────────────────

enum class Suit(val symbol: String) {
    HEARTS("♥"),
    DIAMONDS("♦"),
    CLUBS("♣"),
    SPADES("♠")
}

// ─── Rank ────────────────────────────────────────────────────────────────────

enum class Rank(val display: String, val order: Int) {
    TWO("2", 2), THREE("3", 3), FOUR("4", 4),  FIVE("5", 5),
    SIX("6", 6), SEVEN("7", 7), EIGHT("8", 8), NINE("9", 9),
    TEN("10", 10), JACK("J", 11), QUEEN("Q", 12), KING("K", 13), ACE("A", 14)
}

// ─── Card ────────────────────────────────────────────────────────────────────

sealed class Card(open val id: String) {

    data class Regular(
        val rank: Rank,
        val suit: Suit,
        override val id: String = "${rank}_${suit}_${Random.nextInt(0, 999_999)}"
    ) : Card(id)

    data class Joker(
        override val id: String = "JOKER_${Random.nextInt(0, 999_999)}"
    ) : Card(id)

    // ── Point value depends on game mode (GDD §2) ─────────────────────────

    fun pointValue(mode: GameMode): Int = when (this) {
        is Joker   -> if (mode == GameMode.TAFDHIL) 50 else 10
        is Regular -> when (rank) {
            Rank.JACK, Rank.QUEEN, Rank.KING, Rank.ACE -> 10
            else                                        -> rank.order
        }
    }

    fun isJoker() = this is Joker
    fun isRegular() = this is Regular
}
