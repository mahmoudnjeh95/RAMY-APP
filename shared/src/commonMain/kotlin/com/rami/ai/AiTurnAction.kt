package com.rami.ai

import com.rami.model.Card

/**
 * One discrete action the AI can take during its turn.
 * The [com.rami.engine.GameEngine] exposes typed methods that map 1-to-1 to these.
 */
sealed class AiTurnAction {
    // ── Draw phase ────────────────────────────────────────────────────────────
    object DrawFromDeck    : AiTurnAction()
    object DrawFromDiscard : AiTurnAction()

    // ── Action phase ──────────────────────────────────────────────────────────
    /** Lay down one or more formations in a single Nazoul move */
    data class LayDown(val formations: List<List<Card>>) : AiTurnAction()

    /** Add a card from hand to an existing table formation */
    data class AddToFormation(val card: Card, val formationId: String) : AiTurnAction()

    /** Steal a Joker from a table formation using a replacement card */
    data class StealJoker(
        val formationId: String,
        val jokerIndex: Int,
        val replacement: Card.Regular
    ) : AiTurnAction()

    // ── Discard phase ─────────────────────────────────────────────────────────
    data class Discard(val card: Card) : AiTurnAction()
}
