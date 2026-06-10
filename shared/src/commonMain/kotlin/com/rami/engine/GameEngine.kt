package com.rami.engine

import com.rami.ai.AiDifficulty
import com.rami.ai.AiPlayer
import com.rami.ai.AiTurnAction
import com.rami.model.*
import com.rami.scorer.ScoreCalculator
import com.rami.validator.FormationValidator
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Central game state machine for Rami Tunisien.
 *
 * Flow of one turn (GDD §3):
 *   DRAW  → drawFromDeck() | drawFromDiscard()
 *   ACTION → layDown() | addCardToFormation() | stealJoker()  (any order, repeatable)
 *   DISCARD → discard()   (ends the turn)
 *
 * UI layer observes [state] and calls action methods in response to user input.
 * All mutations return a new [GameState] snapshot; never mutate in place.
 */
class GameEngine {

    internal val _state = MutableStateFlow(
        GameState(mode = GameMode.NORMAL, players = emptyList())
    )
    val state: StateFlow<GameState> = _state.asStateFlow()

    /**
     * **Test-only** — injects an arbitrary [GameState] so integration tests can
     * set up precise card scenarios without going through the random deal.
     * Never call this from production code.
     */
    internal fun restoreStateForTesting(state: GameState) {
        _state.value = state
    }

    private val normalRules  = NormalRules()
    private val tafdhilRules = TafdhilRules()

    // ═══════════════════════════════════════════════════════════════════════════
    // SETUP
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * Initialises a new game.
     * @param playerNames ordered list of display names; index 0 is the local player
     * @param aiIndices   indices in [playerNames] that should be controlled by AI
     */
    fun startGame(
        playerNames: List<String>,
        mode: GameMode,
        scoreLimit: Int,
        aiIndices: Set<Int> = emptySet(),
        aiDifficulty: AiDifficulty = AiDifficulty.MEDIUM
    ) {
        require(playerNames.size in setOf(2, 4)) { "Only 2 or 4 players supported" }

        val deck    = Deck(mode)
        val players = playerNames.mapIndexed { i, name ->
            val handSize = if (i == 0) 15 else 14  // GDD §1.1 — first player gets 15
            Player(
                id           = "player_$i",
                name         = name,
                isAI         = i in aiIndices,
                aiDifficulty = if (i in aiIndices) aiDifficulty else null,
                hand         = deck.deal(handSize)
            )
        }

        _state.value = GameState(
            mode               = mode,
            players            = players,
            deck               = deck.toList(),
            discardPile        = emptyList(),
            tableFormations    = emptyList(),
            currentPlayerIndex = 0,
            turnPhase          = TurnPhase.DRAW,
            gamePhase          = GamePhase.IN_ROUND,
            scoreLimit         = scoreLimit,
            roundNumber        = 1,
            lastNazoulValue    = 0
        )
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // DRAW PHASE
    // ═══════════════════════════════════════════════════════════════════════════

    /** Draw the top card from the face-down deck (GDD §3, Step 1). */
    fun drawFromDeck() {
        val s = _state.value
        if (s.turnPhase != TurnPhase.DRAW || s.gamePhase != GamePhase.IN_ROUND) return

        val deck = ensureNonEmptyDeck(s)
        val card = deck.first()
        val updatedPlayer = s.currentPlayer.copy(hand = s.currentPlayer.hand + card)

        _state.value = s.copy(
            deck      = deck.drop(1),
            players   = s.players.replaced(s.currentPlayerIndex, updatedPlayer),
            turnPhase = TurnPhase.ACTION
        )
    }

    /**
     * Draw the top card from the discard pile (GDD §3, Step 1).
     * Only allowed AFTER the player has laid down (Nazoul active).
     * Otherwise applies a mode-specific penalty.
     */
    fun drawFromDiscard() {
        val s = _state.value
        if (s.turnPhase != TurnPhase.DRAW || s.gamePhase != GamePhase.IN_ROUND) return
        if (s.discardPile.isEmpty()) return

        if (!s.currentPlayer.hasLaidDown) {
            // ── Penalty branch ─────────────────────────────────────────────
            _state.value = when (s.mode) {
                GameMode.NORMAL  -> normalRules.applyDrawPenalty(s)
                GameMode.TAFDHIL -> {
                    val penalised = tafdhilRules.applyDrawPenalty(s, s.currentPlayer.id)
                    // In Tafdhil the player skips the rest of the round
                    penalised.also { advanceTurn(it) }
                    return
                }
            }
            return
        }

        val card          = s.discardPile.last()
        val updatedPlayer = s.currentPlayer.copy(hand = s.currentPlayer.hand + card)

        _state.value = s.copy(
            discardPile = s.discardPile.dropLast(1),
            players     = s.players.replaced(s.currentPlayerIndex, updatedPlayer),
            turnPhase   = TurnPhase.ACTION
        )
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // ACTION PHASE — Lay Down (Nazoul)
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * First lay-down by the current player (GDD §4).
     * [formationCards] is a list-of-lists, each inner list is one formation.
     * Returns false if validation fails (caller can show error).
     */
    fun layDown(formationCards: List<List<Card>>): Boolean {
        val s = _state.value
        if (s.turnPhase != TurnPhase.ACTION) return false
        if (s.currentPlayer.hasLaidDown) return false

        // Validate every formation individually
        val validated = formationCards.mapNotNull { cards ->
            val type = FormationValidator.detectType(cards) ?: return false
            Formation(type = type, cards = cards, ownerId = s.currentPlayer.id)
        }
        if (validated.size != formationCards.size) return false

        // Check minimum threshold
        val valid = when (s.mode) {
            GameMode.NORMAL  -> normalRules.canLayDown(validated, s.mode)
            GameMode.TAFDHIL -> tafdhilRules.canLayDown(validated, s.lastNazoulValue, s.mode)
        }
        if (!valid) return false

        val laidCards   = formationCards.flatten()
        val totalValue  = validated.sumOf { it.pointValue(s.mode) }
        val newHand     = s.currentPlayer.hand.minus(laidCards.toSet())
        val updatedPlayer = s.currentPlayer.copy(
            hand        = newHand,
            hasLaidDown = true,
            nazoulValue = totalValue
        )

        _state.value = s.copy(
            players          = s.players.replaced(s.currentPlayerIndex, updatedPlayer),
            tableFormations  = s.tableFormations + validated,
            lastNazoulValue  = if (s.mode == GameMode.TAFDHIL) totalValue else s.lastNazoulValue
        )

        checkHandEmpty()
        return true
    }

    // ─── Add card to existing table formation ─────────────────────────────────

    /**
     * Add a card from the current player's hand to any existing formation (GDD §3, Step 2).
     * Only allowed after Nazoul.
     */
    fun addCardToFormation(card: Card, formationId: String): Boolean {
        val s = _state.value
        if (!s.currentPlayer.hasLaidDown) return false

        val fIdx = s.tableFormations.indexOfFirst { it.id == formationId }
        if (fIdx == -1) return false
        if (!FormationValidator.canAddCard(card, s.tableFormations[fIdx], s.mode)) return false

        val updatedFormation = s.tableFormations[fIdx].copy(
            cards = s.tableFormations[fIdx].cards + card
        )
        val newHand       = s.currentPlayer.hand - card
        val updatedPlayer = s.currentPlayer.copy(hand = newHand)
        val newFormations = s.tableFormations.replaced(fIdx, updatedFormation)

        _state.value = s.copy(
            players         = s.players.replaced(s.currentPlayerIndex, updatedPlayer),
            tableFormations = newFormations
        )

        checkHandEmpty()
        return true
    }

    // ─── Joker steal ──────────────────────────────────────────────────────────

    /**
     * Steal a Joker from a table formation (GDD §5.2).
     * [jokerIndex] = position of the Joker inside [formationId].
     * [replacement] = the card from the player's hand that replaces the Joker.
     */
    fun stealJoker(
        formationId: String,
        jokerIndex: Int,
        replacement: Card.Regular
    ): Boolean {
        val s = _state.value
        if (!s.currentPlayer.hasLaidDown) return false

        val fIdx = s.tableFormations.indexOfFirst { it.id == formationId }
        if (fIdx == -1) return false
        val formation = s.tableFormations[fIdx]

        if (!FormationValidator.canReplaceJoker(jokerIndex, replacement, formation, s.mode)) return false

        val stolenJoker = formation.cards[jokerIndex] as? Card.Joker ?: return false

        // Remove replacement from hand, add Joker to hand
        val handWithout  = s.currentPlayer.hand - (replacement as Card)
        if (handWithout.size == s.currentPlayer.hand.size) return false  // wasn't in hand
        val newHand      = handWithout + stolenJoker

        val newFormationCards = formation.cards.replaced(jokerIndex, replacement)
        val updatedFormation  = formation.copy(cards = newFormationCards)

        // Tafdhil: check if steal counts towards bank (GDD §5.2)
        val banksForThief = if (
            s.mode == GameMode.TAFDHIL &&
            tafdhilRules.doesStealCountForBank(s.currentPlayer, s.lastNazoulValue)
        ) {
            s.currentPlayer.jokerBankCount + 1
        } else {
            s.currentPlayer.jokerBankCount
        }

        val updatedPlayer = s.currentPlayer.copy(
            hand           = newHand,
            jokerBankCount = banksForThief
        )

        _state.value = s.copy(
            players         = s.players.replaced(s.currentPlayerIndex, updatedPlayer),
            tableFormations = s.tableFormations.replaced(fIdx, updatedFormation)
        )
        return true
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // DISCARD PHASE
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * Discard [card] to end the current turn (GDD §3, Step 3).
     * If the hand is empty after discard → triggers round end.
     */
    fun discard(card: Card) {
        val s = _state.value
        if (s.gamePhase != GamePhase.IN_ROUND) return
        if (card !in s.currentPlayer.hand) return

        val newHand       = s.currentPlayer.hand - card
        val updatedPlayer = s.currentPlayer.copy(hand = newHand)
        val newState      = s.copy(
            players     = s.players.replaced(s.currentPlayerIndex, updatedPlayer),
            discardPile = s.discardPile + card,
            turnPhase   = TurnPhase.DISCARD
        )

        if (newHand.isEmpty()) {
            endRound(newState, updatedPlayer.id)
        } else {
            _state.value = newState
            advanceTurn(newState)
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // TAFDHIL — FINISH BY THROWING JOKERS (GDD §6.2 Option A)
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * Tafdhil only: finish round by throwing remaining Jokers to the centre.
     * Winner assigns each Joker to one opponent (+50 per Joker to that opponent).
     *
     * @param assignments  Map of opponentId → number of Jokers assigned to them.
     *                     Sum of values must equal number of Jokers in hand.
     */
    fun finishByThrowingJokers(assignments: Map<String, Int>): Boolean {
        val s = _state.value
        if (s.mode != GameMode.TAFDHIL) return false
        if (!tafdhilRules.canFinishByThrowingJokers(s.currentPlayer)) return false

        val jokerCount = s.currentPlayer.hand.count { it.isJoker() }
        if (assignments.values.sum() != jokerCount) return false

        val thrownJokers  = s.currentPlayer.hand.filterIsInstance<Card.Joker>()
        val newBankCount  = s.currentPlayer.jokerBankCount + jokerCount
        val updatedPlayer = s.currentPlayer.copy(hand = emptyList(), jokerBankCount = newBankCount)
        val newState      = s.copy(
            players     = s.players.replaced(s.currentPlayerIndex, updatedPlayer),
            discardPile = s.discardPile + thrownJokers
        )

        endRound(newState, updatedPlayer.id, tafdhilJokerAssignments = assignments, winnerBankedJokers = jokerCount)
        return true
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // ROUND / GAME MANAGEMENT
    // ═══════════════════════════════════════════════════════════════════════════

    /** Start the next round, preserving cumulative scores. */
    fun startNextRound() {
        val s   = _state.value
        val deck = Deck(s.mode)

        // Reset per-round fields; keep scores & joker banks
        val resetPlayers = s.players.mapIndexed { i, p ->
            p.copy(
                hand        = deck.deal(if (i == 0) 15 else 14),
                hasLaidDown = false,
                nazoulValue = 0
            )
        }

        // Lowest score starts next round (GDD §1.1)
        val startIdx = resetPlayers.indexOfMinScore()

        _state.value = s.copy(
            players            = resetPlayers,
            deck               = deck.toList(),
            discardPile        = emptyList(),
            tableFormations    = emptyList(),
            currentPlayerIndex = startIdx,
            turnPhase          = TurnPhase.DRAW,
            gamePhase          = GamePhase.IN_ROUND,
            roundNumber        = s.roundNumber + 1,
            lastNazoulValue    = 0,
            roundWinnerId      = null,
            message            = ""
        )
    }

    /** Apply second-life buy-in for [playerId] (GDD §8). */
    fun buySecondLife(playerId: String, extraThreshold: Int = 50) {
        val s          = _state.value
        val newPlayers = ScoreCalculator.applySecondLife(s.players, playerId, s.scoreLimit, extraThreshold)
        _state.value   = s.copy(players = newPlayers, gamePhase = GamePhase.IN_ROUND)
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // AI TURN PROCESSOR
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * Called from a coroutine (LaunchedEffect) whenever it's an AI player's turn.
     * Executes the full draw → action → discard sequence with simulated think delays.
     *
     * Safe to call even when it's a human's turn — returns immediately.
     */
    suspend fun processAiTurn() {
        val s = _state.value
        if (!s.currentPlayer.isAI || s.gamePhase != GamePhase.IN_ROUND) return

        val difficulty = s.currentPlayer.aiDifficulty ?: AiDifficulty.MEDIUM
        val ai         = AiPlayer(difficulty)
        val think      = difficulty.thinkDelayMs

        // ── Draw ─────────────────────────────────────────────────────────────
        delay(think)
        if (_state.value.gamePhase != GamePhase.IN_ROUND) return
        when (ai.decideDraw(_state.value)) {
            is AiTurnAction.DrawFromDiscard -> drawFromDiscard()
            else                            -> drawFromDeck()
        }

        // ── Actions ───────────────────────────────────────────────────────────
        delay(think / 2)
        if (_state.value.gamePhase != GamePhase.IN_ROUND) return

        val actions = ai.decideActions(_state.value)
        for (action in actions) {
            delay(think / 2)
            if (_state.value.gamePhase != GamePhase.IN_ROUND) return

            when (action) {
                is AiTurnAction.LayDown        -> layDown(action.formations)
                is AiTurnAction.AddToFormation -> addCardToFormation(action.card, action.formationId)
                is AiTurnAction.StealJoker     -> stealJoker(action.formationId, action.jokerIndex, action.replacement)
                else                           -> {}
            }
        }

        // ── Discard ───────────────────────────────────────────────────────────
        delay(think)
        if (_state.value.gamePhase != GamePhase.IN_ROUND) return
        val discard = ai.decideDiscard(_state.value)
        discard(discard.card)
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // PRIVATE HELPERS
    // ═══════════════════════════════════════════════════════════════════════════

    /** Called after every action that might empty a hand mid-turn. */
    private fun checkHandEmpty() {
        val s = _state.value
        if (s.currentPlayer.handEmpty) endRound(s, s.currentPlayer.id)
    }

    private fun endRound(
        s: GameState,
        winnerId: String,
        tafdhilJokerAssignments: Map<String, Int> = emptyMap(),
        winnerBankedJokers: Int = 0
    ) {
        val result      = ScoreCalculator.calculateRound(s, winnerId, tafdhilJokerAssignments, winnerBankedJokers)
        val newPlayers  = ScoreCalculator.applyRoundResult(s.players, result)
        val gameOver    = ScoreCalculator.isGameOver(newPlayers, s.scoreLimit)

        _state.value = s.copy(
            players       = newPlayers,
            roundWinnerId = winnerId,
            gamePhase     = if (gameOver) GamePhase.GAME_OVER else GamePhase.ROUND_END,
            message       = if (gameOver) "🏆 انتهت اللعبة!" else "✅ انتهت الجولة!"
        )
    }

    private fun advanceTurn(s: GameState) {
        val nextIdx  = (s.currentPlayerIndex + 1) % s.players.size
        _state.value = s.copy(currentPlayerIndex = nextIdx, turnPhase = TurnPhase.DRAW)
    }

    /** If the deck is empty, reshuffle the discard pile (except top card) — GDD §3. */
    private fun ensureNonEmptyDeck(s: GameState): List<Card> {
        if (s.deck.isNotEmpty()) return s.deck
        if (s.discardPile.size <= 1) return s.deck  // nothing to reshuffle
        val top       = s.discardPile.last()
        val reshuffled = s.discardPile.dropLast(1).shuffled()
        _state.value = s.copy(deck = reshuffled, discardPile = listOf(top))
        return reshuffled
    }

    private fun List<Player>.indexOfMinScore(): Int =
        indexOfFirst { it.score == minOf { p -> p.score } }.coerceAtLeast(0)
}
