package com.rami.engine

import com.rami.model.*
import com.rami.validator.FormationValidator

/**
 * Rule helpers specific to Normal mode (GDD §§ 4.2, 6.1, 9).
 */
class NormalRules {

    /**
     * Validates whether [formations] meet the minimum Nazoul threshold (51 pts).
     * In Normal mode every subsequent lay-down also requires ≥ 51 pts.
     */
    fun canLayDown(formations: List<Formation>, mode: GameMode): Boolean {
        val total = FormationValidator.totalValue(formations, mode)
        return total >= GameMode.NORMAL.minFirstNazoul
    }

    /**
     * Penalty for drawing from discard pile without having laid down (GDD §9):
     *  → Return the card (discard pile unchanged); player must draw from deck this turn.
     * Returns an updated [GameState] with [TurnPhase.DRAW] so the UI can prompt again.
     */
    fun applyDrawPenalty(state: GameState): GameState =
        state.copy(
            turnPhase = TurnPhase.DRAW,
            message   = "⚠️ يجب النزول أولاً — يجب السحب من الأوراق الخفية"
        )
}
