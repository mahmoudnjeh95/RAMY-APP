package com.rami.validator

import com.rami.model.*

/**
 * Pure-function validator for card formations (GDD §4).
 *
 * Rules recap:
 *  SET      — 3+ cards, same rank, each from a different suit. Jokers substitute freely.
 *             Max 4 cards (one per suit).
 *  SEQUENCE — 3+ cards, same suit, consecutive ranks. Jokers fill gaps.
 *             No duplicate ranks.
 */
object FormationValidator {

    // ─── Public API ───────────────────────────────────────────────────────────

    /** Returns true if [cards] form a valid Set OR Sequence in [mode]. */
    fun isValid(cards: List<Card>, mode: GameMode): Boolean =
        cards.size >= 3 && (checkSet(cards) || checkSequence(cards))

    /** Detects the formation type, or null if invalid. */
    fun detectType(cards: List<Card>): FormationType? {
        if (cards.size < 3) return null
        return when {
            checkSet(cards)      -> FormationType.SET
            checkSequence(cards) -> FormationType.SEQUENCE
            else                 -> null
        }
    }

    /**
     * Returns true if [card] can legally be appended to [formation].
     * Used for "add to existing formation" action and Joker-steal validation.
     */
    fun canAddCard(card: Card, formation: Formation, mode: GameMode): Boolean =
        isValid(formation.cards + card, mode)

    /**
     * Validates that [replacement] can occupy the position held by [joker] inside
     * [formation] — the core of the Joker steal logic (GDD §5.2).
     */
    fun canReplaceJoker(
        jokerIndex: Int,
        replacement: Card.Regular,
        formation: Formation,
        mode: GameMode
    ): Boolean {
        val newCards = formation.cards.toMutableList()
        newCards[jokerIndex] = replacement
        return isValid(newCards, mode)
    }

    // ─── Set validation ───────────────────────────────────────────────────────

    private fun checkSet(cards: List<Card>): Boolean {
        val regulars = cards.filterIsInstance<Card.Regular>()
        val jokers   = cards.count { it.isJoker() }
        val total    = cards.size

        // All non-joker cards must share the same rank
        if (regulars.isNotEmpty()) {
            val rank = regulars.first().rank
            if (regulars.any { it.rank != rank }) return false
        }

        // All non-joker cards must have distinct suits
        val suits = regulars.map { it.suit }
        if (suits.size != suits.toSet().size) return false

        // Max total = 4 (one per suit); min = 3
        return total in 3..4
    }

    // ─── Sequence validation ─────────────────────────────────────────────────

    private fun checkSequence(cards: List<Card>): Boolean {
        val regulars = cards.filterIsInstance<Card.Regular>()
        val jokers   = cards.count { it.isJoker() }

        // All regular cards must share the same suit
        if (regulars.isNotEmpty()) {
            val suit = regulars.first().suit
            if (regulars.any { it.suit != suit }) return false
        }

        // No duplicate ranks
        val orders = regulars.map { it.rank.order }.sorted()
        if (orders.size != orders.toSet().size) return false

        // Jokers must be able to fill all gaps in the consecutive run
        return canJokersFillGaps(orders, jokers, cards.size)
    }

    /**
     * Checks whether [jokerCount] jokers can fill all gaps in [sortedOrders]
     * to form an unbroken consecutive run of length [totalCards].
     */
    private fun canJokersFillGaps(
        sortedOrders: List<Int>,
        jokerCount: Int,
        totalCards: Int
    ): Boolean {
        if (sortedOrders.isEmpty()) return jokerCount >= 3

        val min   = sortedOrders.first()
        val max   = sortedOrders.last()
        val span  = max - min + 1          // required consecutive slots
        val gaps  = span - sortedOrders.size  // slots that need a joker

        // Jokers must cover exactly the gaps (no extras left dangling in the span)
        // AND the total card count must match the span exactly
        return gaps <= jokerCount && totalCards == span
    }

    // ─── Utility ─────────────────────────────────────────────────────────────

    /** Total point value of a list of formations */
    fun totalValue(formations: List<Formation>, mode: GameMode): Int =
        formations.sumOf { it.pointValue(mode) }
}
