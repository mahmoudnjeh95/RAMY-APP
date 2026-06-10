package com.rami.model

// ─── Turn & Game phase enums ──────────────────────────────────────────────────

/** Steps within a single player's turn (GDD §3) */
enum class TurnPhase { DRAW, ACTION, DISCARD }

/** Top-level lifecycle of the overall game */
enum class GamePhase { SETUP, IN_ROUND, ROUND_END, GAME_OVER }

// ─── GameState ────────────────────────────────────────────────────────────────

/**
 * Complete, immutable snapshot of the game at any point in time.
 * The [com.rami.engine.GameEngine] emits a new [GameState] via [kotlinx.coroutines.flow.StateFlow]
 * for every state transition — UI observes and reacts.
 */
data class GameState(
    val mode: GameMode,
    val players: List<Player>,

    // ── Deck / piles ─────────────────────────────────────────────────────────
    val deck: List<Card> = emptyList(),
    /** Last card = top of pile (index [discardPile.lastIndex]) */
    val discardPile: List<Card> = emptyList(),
    val tableFormations: List<Formation> = emptyList(),

    // ── Turn tracking ─────────────────────────────────────────────────────────
    val currentPlayerIndex: Int = 0,
    val turnPhase: TurnPhase = TurnPhase.DRAW,
    val gamePhase: GamePhase = GamePhase.SETUP,
    val roundNumber: Int = 1,

    // ── Scoring config ────────────────────────────────────────────────────────
    val scoreLimit: Int = 150,

    /**
     * Tafdhil only — the Nazoul value of the last player who laid down.
     * Each subsequent player must exceed this (GDD §4.2).
     * Resets to 0 at the start of each round.
     */
    val lastNazoulValue: Int = 0,

    // ── Round outcome ─────────────────────────────────────────────────────────
    val roundWinnerId: String? = null,
    /** Human-readable status / toast message for the UI */
    val message: String = ""
) {
    // ── Convenience accessors ─────────────────────────────────────────────────

    val currentPlayer: Player
        get() = players[currentPlayerIndex]

    val activePlayers: List<Player>
        get() = players.filter { !it.isEliminated }

    val discardTop: Card?
        get() = discardPile.lastOrNull()

    val deckSize: Int
        get() = deck.size
}
