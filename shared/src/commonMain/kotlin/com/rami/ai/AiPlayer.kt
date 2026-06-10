package com.rami.ai

import com.rami.model.*
import com.rami.validator.FormationValidator

/**
 * Pure AI decision-maker. All methods take a [GameState] snapshot and return
 * the best [AiTurnAction] for each phase. No side-effects; the engine applies
 * the actions.
 *
 * Difficulty levels:
 *  EASY   — random draw, greedy formation only, discards highest-value card
 *  MEDIUM — smart draw (checks discard utility), greedy formation, expendability discard
 *  HARD   — MEDIUM + Joker steal, avoids feeding opponents, prefers holding Jokers in Tafdhil
 */
class AiPlayer(val difficulty: AiDifficulty) {

    // ═══════════════════════════════════════════════════════════════════════════
    // DRAW PHASE
    // ═══════════════════════════════════════════════════════════════════════════

    fun decideDraw(state: GameState): AiTurnAction {
        val player  = state.currentPlayer
        val discard = state.discardTop

        // Without Nazoul, drawing from discard triggers a penalty → always deck
        if (!player.hasLaidDown) return AiTurnAction.DrawFromDeck

        if (discard == null) return AiTurnAction.DrawFromDeck

        return when (difficulty) {
            AiDifficulty.EASY -> AiTurnAction.DrawFromDeck  // Easy always draws blind

            AiDifficulty.MEDIUM,
            AiDifficulty.HARD -> {
                // Take discard if it improves formation potential
                if (FormationFinder.improvesPotential(discard, player.hand, state.mode))
                    AiTurnAction.DrawFromDiscard
                else
                    AiTurnAction.DrawFromDeck
            }
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // ACTION PHASE  (can return multiple ordered actions)
    // ═══════════════════════════════════════════════════════════════════════════

    fun decideActions(state: GameState): List<AiTurnAction> {
        val actions     = mutableListOf<AiTurnAction>()
        val player      = state.currentPlayer
        // Track card availability as actions are planned (AI looks ahead)
        val remainingIds = player.hand.map { it.id }.toMutableSet()

        // ── 1. Lay down (Nazoul) ─────────────────────────────────────────────
        if (!player.hasLaidDown) {
            val combo = FormationFinder.findNazoulCombination(
                player.hand, state.mode, state.lastNazoulValue
            )
            if (combo != null) {
                actions += AiTurnAction.LayDown(combo)
                combo.flatten().forEach { remainingIds -= it.id }
            }
        }

        val willHaveLaidDown = player.hasLaidDown || actions.any { it is AiTurnAction.LayDown }

        // ── 2. Add cards to existing table formations ─────────────────────────
        if (willHaveLaidDown) {
            val remainingCards = player.hand.filter { it.id in remainingIds }
            for (card in remainingCards) {
                for (formation in state.tableFormations) {
                    if (FormationValidator.canAddCard(card, formation, state.mode)) {
                        actions += AiTurnAction.AddToFormation(card, formation.id)
                        remainingIds -= card.id
                        break
                    }
                }
            }

            // ── 3. Joker steal (Medium+ only) ─────────────────────────────────
            if (difficulty != AiDifficulty.EASY) {
                val remaining = player.hand.filter { it.id in remainingIds }
                val steal = findBestSteal(remaining, state)
                if (steal != null) actions += steal
            }
        }

        return actions
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // DISCARD PHASE
    // ═══════════════════════════════════════════════════════════════════════════

    fun decideDiscard(state: GameState): AiTurnAction.Discard {
        val hand = state.currentPlayer.hand
        if (hand.isEmpty()) return AiTurnAction.Discard(Card.Joker()) // shouldn't happen

        val card = when (difficulty) {
            AiDifficulty.EASY -> {
                // Discard highest-value non-Joker; or a Joker if nothing else
                hand.filterNot { it.isJoker() }.maxByOrNull { it.pointValue(state.mode) }
                    ?: hand.first()
            }

            AiDifficulty.MEDIUM -> {
                // Most expendable (not involved in any formation candidate)
                hand.maxByOrNull { FormationFinder.expendabilityScore(it, hand, state.mode) }
                    ?: hand.first()
            }

            AiDifficulty.HARD -> {
                // Same as Medium but never discard Jokers in Tafdhil (their bank value)
                val nonJokers = if (state.mode == GameMode.TAFDHIL) hand.filterNot { it.isJoker() }
                                else hand
                val pool = nonJokers.ifEmpty { hand }
                pool.maxByOrNull { FormationFinder.expendabilityScore(it, hand, state.mode) }
                    ?: pool.first()
            }
        }

        return AiTurnAction.Discard(card)
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // HELPERS
    // ═══════════════════════════════════════════════════════════════════════════

    private fun findBestSteal(
        hand: List<Card>,
        state: GameState
    ): AiTurnAction.StealJoker? {
        for (formation in state.tableFormations) {
            for ((jokerIdx, _) in formation.jokerPositions()) {
                val replacement = hand.filterIsInstance<Card.Regular>().firstOrNull { card ->
                    FormationValidator.canReplaceJoker(jokerIdx, card, formation, state.mode)
                } ?: continue

                // In Hard mode: only steal if it benefits our bank in Tafdhil
                if (difficulty == AiDifficulty.HARD && state.mode == GameMode.TAFDHIL) {
                    val player = state.currentPlayer
                    if (player.nazoulValue <= state.lastNazoulValue) continue  // bank won't count
                }

                return AiTurnAction.StealJoker(formation.id, jokerIdx, replacement)
            }
        }
        return null
    }
}
