package com.rami.engine

import com.rami.model.*
import com.rami.validator.FormationValidator

/**
 * Rule helpers specific to Tafdhil mode (GDD §§ 4.2, 5.2–5.3, 6.2, 9).
 */
class TafdhilRules {

    /**
     * In Tafdhil the first player needs ≥ 71 pts; every subsequent player must
     * exceed the previous player's Nazoul value (GDD §4.2).
     */
    fun canLayDown(
        formations: List<Formation>,
        lastNazoulValue: Int,
        mode: GameMode
    ): Boolean {
        val total = FormationValidator.totalValue(formations, mode)
        return if (lastNazoulValue == 0) {
            total >= GameMode.TAFDHIL.minFirstNazoul      // first lay-down this round
        } else {
            total > lastNazoulValue                        // must strictly exceed
        }
    }

    /**
     * Penalty for drawing from discard without Nazoul in Tafdhil mode (GDD §9):
     *  → ALL hand cards go to the discard pile; player skips the rest of the round.
     */
    fun applyDrawPenalty(state: GameState, playerId: String): GameState {
        val pIdx = state.players.indexOfFirst { it.id == playerId }
        val player = state.players[pIdx]
        val newPile = state.discardPile + player.hand
        val newPlayers = state.players.replaced(pIdx, player.copy(hand = emptyList()))
        return state.copy(
            players     = newPlayers,
            discardPile = newPile,
            message     = "⛔ عقوبة! جميع الأوراق في الكومة — تخطي الجولة"
        )
    }

    /**
     * Returns true when the player can finish by throwing Joker(s) to the centre.
     * Requires hand to contain ONLY Jokers (GDD §6.2 — Option A).
     */
    fun canFinishByThrowingJokers(player: Player): Boolean =
        player.hand.isNotEmpty() && player.hand.all { it.isJoker() }

    /**
     * Returns true when the player can finish by adding the last card to a
     * formation on the table (GDD §6.2 — Option B).
     */
    fun canFinishByAddingLastCard(
        card: Card,
        tableFormations: List<Formation>,
        mode: GameMode
    ): Boolean = tableFormations.any { f -> FormationValidator.canAddCard(card, f, mode) }

    /**
     * Determines whether a stolen Joker counts toward the thief's bank (GDD §5.2).
     * Counts only if the thief's current Nazoul value > [lastNazoulValue].
     */
    fun doesStealCountForBank(thief: Player, lastNazoulValue: Int): Boolean =
        thief.hasLaidDown && thief.nazoulValue > lastNazoulValue
}

// ── Internal helper ──────────────────────────────────────────────────────────

internal fun <T> List<T>.replaced(index: Int, item: T): List<T> =
    toMutableList().also { it[index] = item }
