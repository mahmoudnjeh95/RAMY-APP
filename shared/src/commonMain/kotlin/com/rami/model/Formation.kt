package com.rami.model

import kotlin.random.Random

// ─── Formation type ───────────────────────────────────────────────────────────

enum class FormationType(val arabicName: String) {
    SET("مجموعة"),         // same rank, different suits  (GDD §4.1)
    SEQUENCE("تسلسل")     // consecutive ranks, same suit (GDD §4.1)
}

// ─── Formation ────────────────────────────────────────────────────────────────

/**
 * An immutable snapshot of one formation lying on the table.
 * The engine replaces the whole [Formation] object when a card is added or
 * a Joker is stolen, keeping state transitions pure.
 */
data class Formation(
    val id: String = "F_${Random.nextInt(0, 999_999)}",
    val type: FormationType,
    val cards: List<Card>,
    val ownerId: String                        // player who originally laid this down
) {
    /** Point total of all cards in this formation */
    fun pointValue(mode: GameMode): Int =
        cards.sumOf { it.pointValue(mode) }

    /** True if this formation contains a Joker that can potentially be stolen */
    fun hasStealableJoker(): Boolean =
        cards.any { it.isJoker() }

    /** Returns the Joker cards with their position index for steal UI */
    fun jokerPositions(): List<Pair<Int, Card.Joker>> =
        cards.mapIndexedNotNull { i, c -> if (c is Card.Joker) i to c else null }
}
