package com.rami.integration

import com.rami.ai.AiDifficulty
import com.rami.ai.AiPlayer
import com.rami.ai.FormationFinder
import com.rami.engine.GameEngine
import com.rami.helpers.*
import com.rami.model.*
import kotlinx.coroutines.test.runTest
import kotlin.test.*

/**
 * AI integration tests using [kotlinx.coroutines.test.runTest].
 * `runTest` automatically skips [kotlinx.coroutines.delay] calls, so each test
 * runs in milliseconds even though the AI normally waits ~900 ms between actions.
 *
 * These tests run on JVM, Android (unit), and iOS simulator.
 */
class AiSimulationTest {

    private lateinit var engine: GameEngine

    @BeforeTest fun setup() { engine = GameEngine() }

    // ═══════════════════════════════════════════════════════════════════════════
    // FORMATION FINDER
    // ═══════════════════════════════════════════════════════════════════════════

    @Test
    fun formationFinder_detectsValidSet() {
        val hand = TC.set3Aces() + listOf(TC.TWO_C, TC.FOUR_H)
        val found = FormationFinder.findAllFormations(hand, GameMode.NORMAL)
        assertTrue(found.isNotEmpty(), "Should find at least one formation in hand")
        assertTrue(found.any { it.type == FormationType.SET }, "Should detect the ace SET")
    }

    @Test
    fun formationFinder_detectsValidSequence() {
        val hand = TC.seq7to9Hearts() + listOf(TC.K_S, TC.A_D)
        val found = FormationFinder.findAllFormations(hand, GameMode.NORMAL)
        assertTrue(found.any { it.type == FormationType.SEQUENCE }, "Should detect the heart SEQUENCE")
    }

    @Test
    fun formationFinder_detectsJokerSequence() {
        val hand = listOf(TC.FIVE_H, TC.JKR0, TC.SEVEN_H, TC.K_S)
        val found = FormationFinder.findAllFormations(hand, GameMode.NORMAL)
        assertTrue(found.any { it.cards.any { c -> c.isJoker() } }, "Should detect Joker-gap sequence")
    }

    @Test
    fun formationFinder_nazoulCombination_meets51Normal() {
        val hand  = TC.set3Aces() + TC.seq7to9Hearts() + listOf(TC.K_C, TC.FOUR_H)
        val combo = FormationFinder.findNazoulCombination(hand, GameMode.NORMAL, 0)
        assertNotNull(combo, "Should find a Nazoul combination meeting 51 pts")
        val total = combo.flatten().sumOf { it.pointValue(GameMode.NORMAL) }
        assertTrue(total >= 51, "Combo total $total should be ≥ 51")
    }

    @Test
    fun formationFinder_nazoulCombination_returnsNullIfImpossible() {
        val hand  = listOf(TC.TWO_C, TC.FOUR_H, TC.SIX_H)  // can't form any valid group
        val combo = FormationFinder.findNazoulCombination(hand, GameMode.NORMAL, 0)
        assertNull(combo, "Should return null when no valid Nazoul combination exists")
    }

    @Test
    fun formationFinder_tafdhilThreshold_requires71pts() {
        val hand  = TC.set3Aces() + TC.seq7to9Hearts()   // 54 pts — not enough for Tafdhil
        val combo = FormationFinder.findNazoulCombination(hand, GameMode.TAFDHIL, 0)
        assertNull(combo, "54 pts should not meet 71 pts Tafdhil threshold")
    }

    @Test
    fun formationFinder_expendabilityScore_jokerNeverDiscarded() {
        val hand  = listOf(TC.JKR0, TC.TWO_C, TC.FOUR_H)
        val score = FormationFinder.expendabilityScore(TC.JKR0, hand, GameMode.NORMAL)
        assertTrue(score < 0, "Joker expendability should be MIN_VALUE (never discard)")
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // AI PLAYER DECISIONS
    // ═══════════════════════════════════════════════════════════════════════════

    @Test
    fun aiEasy_drawDecision_alwaysDrawsFromDeck() {
        val ai = AiPlayer(AiDifficulty.EASY)
        val p0 = TC.player("p0", hand = TC.seq7to9Hearts(), hasLaidDown = true)
        val p1 = TC.player("p1", hand = List(8){ TC.TWO_C })
        val state = TC.minimalState(
            players     = listOf(p0, p1),
            discardPile = listOf(TC.SEVEN_H)  // useful card on top
        )
        // Easy AI ignores discard even when useful
        val decision = ai.decideDraw(state.copy(currentPlayerIndex = 0))
        assertEquals(com.rami.ai.AiTurnAction.DrawFromDeck, decision)
    }

    @Test
    fun aiMedium_drawDecision_picksDiscardWhenUseful() {
        val ai = AiPlayer(AiDifficulty.MEDIUM)
        // p0 has 5♥ 6♥ in hand; 7♥ is on discard → completes a sequence
        val hand    = listOf(TC.FIVE_H, TC.SIX_H, TC.K_S)
        val p0      = TC.player("p0", hand = hand, hasLaidDown = true)
        val p1      = TC.player("p1", hand = List(8){ TC.TWO_C })
        val state   = TC.minimalState(
            players     = listOf(p0, p1),
            discardPile = listOf(TC.SEVEN_H),
            turnPhase   = TurnPhase.DRAW
        )
        val decision = ai.decideDraw(state)
        assertEquals(
            com.rami.ai.AiTurnAction.DrawFromDiscard, decision,
            "Medium AI should pick 7♥ from discard to complete 5-6-7♥ sequence"
        )
    }

    @Test
    fun aiHard_discard_prefersMostExpendableCard() {
        val ai   = AiPlayer(AiDifficulty.HARD)
        // 5♥ 6♥ 7♥ form a sequence (keeper); K♠ is isolated (discard candidate)
        val hand = TC.seq5to7Hearts() + listOf(TC.K_S)
        val p0   = TC.player("p0", hand = hand, hasLaidDown = true)
        val p1   = TC.player("p1", hand = List(8){ TC.TWO_C })
        val state = TC.minimalState(players = listOf(p0, p1), turnPhase = TurnPhase.ACTION)

        val decision = ai.decideDiscard(state) as com.rami.ai.AiTurnAction.Discard
        assertEquals(TC.K_S.id, decision.card.id, "Hard AI should discard isolated K♠")
    }

    @Test
    fun aiHard_doesNotDiscardJokerInTafdhil() {
        val ai   = AiPlayer(AiDifficulty.HARD)
        val hand = listOf(TC.JKR0, TC.K_S, TC.Q_S)  // Joker + isolated regulars
        val p0   = TC.player("p0", hand = hand, hasLaidDown = true)
        val p1   = TC.player("p1", hand = List(8){ TC.TWO_C })
        val state = TC.minimalState(
            mode = GameMode.TAFDHIL, players = listOf(p0, p1), turnPhase = TurnPhase.ACTION
        )

        val decision = ai.decideDiscard(state) as com.rami.ai.AiTurnAction.Discard
        assertFalse(decision.card.isJoker(), "Hard AI should never discard a Joker in Tafdhil")
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // FULL AI ROUND — suspend tests via runTest
    // ═══════════════════════════════════════════════════════════════════════════

    @Test
    fun aiRound_2Players_completesWithoutCrash() = runTest {
        engine.startGame(
            playerNames  = listOf("Human", "AI"),
            mode         = GameMode.NORMAL,
            scoreLimit   = 150,
            aiIndices    = setOf(1),
            aiDifficulty = AiDifficulty.MEDIUM
        )

        // Simulate the AI taking its turn for up to 50 rounds before giving up
        var turns = 0
        while (engine.state.value.gamePhase == GamePhase.IN_ROUND && turns < 50) {
            val s = engine.state.value
            if (s.currentPlayer.isAI) {
                engine.processAiTurn()
            } else {
                // Drive the human player manually (draw + discard first card)
                engine.drawFromDeck()
                val card = engine.state.value.currentPlayer.hand.firstOrNull() ?: break
                engine.discard(card)
            }
            turns++
        }

        // Round must eventually end (no infinite loop)
        val finalPhase = engine.state.value.gamePhase
        assertTrue(
            finalPhase == GamePhase.ROUND_END || finalPhase == GamePhase.GAME_OVER,
            "AI round should finish within 50 turns, but game phase is $finalPhase after $turns turns"
        )
    }

    @Test
    fun aiRound_4AiPlayers_tafdhil_completesWithoutCrash() = runTest {
        engine.startGame(
            playerNames  = listOf("AI1","AI2","AI3","AI4"),
            mode         = GameMode.TAFDHIL,
            scoreLimit   = 200,
            aiIndices    = setOf(0,1,2,3),
            aiDifficulty = AiDifficulty.HARD
        )

        var turns = 0
        while (engine.state.value.gamePhase == GamePhase.IN_ROUND && turns < 100) {
            engine.processAiTurn()
            turns++
        }

        val finalPhase = engine.state.value.gamePhase
        assertTrue(
            finalPhase == GamePhase.ROUND_END || finalPhase == GamePhase.GAME_OVER,
            "4-AI Tafdhil round should finish within 100 turns, but phase is $finalPhase"
        )
    }

    @Test
    fun aiRound_winnerGetsNegativeTenPoints() = runTest {
        engine.startGame(
            playerNames  = listOf("Human","AI"),
            mode         = GameMode.NORMAL,
            scoreLimit   = 150,
            aiIndices    = setOf(1),
            aiDifficulty = AiDifficulty.EASY
        )
        val scoresBefore = engine.state.value.players.map { it.score }

        var turns = 0
        while (engine.state.value.gamePhase == GamePhase.IN_ROUND && turns < 60) {
            val s = engine.state.value
            if (s.currentPlayer.isAI) {
                engine.processAiTurn()
            } else {
                engine.drawFromDeck()
                val card = engine.state.value.currentPlayer.hand.firstOrNull() ?: break
                engine.discard(card)
            }
            turns++
        }

        if (engine.state.value.gamePhase == GamePhase.ROUND_END) {
            val winner = engine.state.value.roundWinnerId
            assertNotNull(winner)
            val winnerPlayer = engine.state.value.players.first { it.id == winner }
            // Winner's score must have decreased by 10 compared to before
            val beforeScore = scoresBefore[engine.state.value.players.indexOf(winnerPlayer)]
            assertEquals(beforeScore - 10, winnerPlayer.score,
                "Winner should receive −10 points")
        }
    }
}
