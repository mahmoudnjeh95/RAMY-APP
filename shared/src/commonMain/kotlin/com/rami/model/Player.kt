package com.rami.model

import com.rami.ai.AiDifficulty

/**
 * Immutable snapshot of a player's state within a game.
 * All mutations return a new copy via [copy].
 */
data class Player(
    val id: String,
    val name: String,
    val isAI: Boolean = false,
    /** Non-null when [isAI] is true — controls AI strategy & think delay */
    val aiDifficulty: AiDifficulty? = null,

    // ── Per-round state ──────────────────────────────────────────────────────
    val hand: List<Card> = emptyList(),
    /** True once this player has executed their first Nazoul (lay-down) */
    val hasLaidDown: Boolean = false,
    /**
     * The cumulative point value of formations laid in the **current** turn
     * (relevant for Tafdhil's "must exceed previous" rule — GDD §4.2).
     */
    val nazoulValue: Int = 0,

    // ── Across-rounds state ───────────────────────────────────────────────────
    val score: Int = 0,
    /**
     * Tafdhil only — count of Jokers accumulated in the personal bank (GDD §5.3).
     * Every 4 Jokers → −100 pts, then resets.
     */
    val jokerBankCount: Int = 0,
    val isEliminated: Boolean = false,
    val hasSecondLife: Boolean = false,
    val secondLifeThreshold: Int = 0  // score limit after buying second life
) {
    /** Cards left in hand (count shown to opponents) */
    val handSize get() = hand.size

    /** True when the player's hand is empty (possible round-win state) */
    val handEmpty get() = hand.isEmpty()
}
