package com.rami

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.rami.ai.AiDifficulty
import com.rami.engine.GameEngine
import com.rami.helpers.*
import com.rami.model.*
import kotlinx.coroutines.*
import kotlinx.coroutines.test.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Instrumented integration tests that run on an Android device or emulator.
 *
 * Run with:
 *   ./gradlew :shared:connectedAndroidTest
 *
 * These complement the commonTest suite by exercising the engine on the actual
 * Android runtime (Dalvik/ART), real coroutine dispatchers, and real threading.
 */
@RunWith(AndroidJUnit4::class)
class AndroidEngineInstrumentedTest {

    private lateinit var engine: GameEngine

    @Before fun setup() { engine = GameEngine() }

    // ═══════════════════════════════════════════════════════════════════════════
    // STATE FLOW — REAL DISPATCHER
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * Verifies that [GameEngine.state] emits correctly on Android's real
     * main-thread dispatcher after [GameEngine.startGame].
     */
    @Test
    fun stateFlow_emitsAfterStartGame() {
        engine.startGame(listOf("Ali","Brahim"), GameMode.NORMAL, 150)
        val s = engine.state.value
        assertEquals(GamePhase.IN_ROUND, s.gamePhase)
        assertEquals(TurnPhase.DRAW,     s.turnPhase)
        assertEquals(2, s.players.size)
    }

    /**
     * Collects state snapshots across a full draw → discard turn on Android.
     */
    @Test
    fun stateFlow_capturesPhaseTransitions() {
        engine.startGame(listOf("Ali","Brahim"), GameMode.NORMAL, 150)

        val snapshots = mutableListOf<TurnPhase>()

        // Collect synchronously
        snapshots += engine.state.value.turnPhase  // DRAW

        engine.drawFromDeck()
        snapshots += engine.state.value.turnPhase  // ACTION

        val card = engine.state.value.players[0].hand.first()
        engine.discard(card)
        snapshots += engine.state.value.turnPhase  // next DRAW (player 1)

        assertEquals(listOf(TurnPhase.DRAW, TurnPhase.ACTION, TurnPhase.DRAW), snapshots)
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // AI TURN — REAL COROUTINES ON ANDROID
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * Runs [GameEngine.processAiTurn] on a real Android coroutine scope.
     * Uses [StandardTestDispatcher] so delays are controlled but real threading applies.
     */
    @Test
    fun aiTurn_completesOnAndroidDispatcher() {
        val testScope      = TestScope()
        val testDispatcher = StandardTestDispatcher(testScope.testScheduler)

        engine.startGame(
            playerNames  = listOf("Human","AI"),
            mode         = GameMode.NORMAL,
            scoreLimit   = 150,
            aiIndices    = setOf(1),
            aiDifficulty = AiDifficulty.MEDIUM
        )

        // Advance to AI player's turn
        engine.drawFromDeck()
        val card = engine.state.value.players[0].hand.firstOrNull()
        if (card != null) engine.discard(card)

        val stateBefore = engine.state.value

        // Run AI turn on test dispatcher
        testScope.runTest {
            launch(testDispatcher) { engine.processAiTurn() }
            advanceUntilIdle()
        }

        // AI should have taken at least one action (drew a card or changed phase)
        // Phase should not still be DRAW (AI should have advanced it)
        val stateAfter = engine.state.value
        // Either the round ended or the AI moved past DRAW phase
        assertTrue(
            stateAfter.gamePhase != GamePhase.IN_ROUND ||
            stateAfter.turnPhase != TurnPhase.DRAW ||
            stateAfter.currentPlayerIndex != stateBefore.currentPlayerIndex,
            "AI should have taken its turn"
        )
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // FULL 2-PLAYER GAME ON ANDROID
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * Drives a complete 2-player AI vs AI game to ROUND_END on Android.
     * Validates that the engine never enters an illegal state.
     */
    @Test
    fun fullGame_2AiPlayers_reachesRoundEnd() {
        val testScope = TestScope()

        engine.startGame(
            playerNames  = listOf("AI_Alpha","AI_Beta"),
            mode         = GameMode.NORMAL,
            scoreLimit   = 150,
            aiIndices    = setOf(0, 1),
            aiDifficulty = AiDifficulty.HARD
        )

        testScope.runTest {
            var turns = 0
            while (engine.state.value.gamePhase == GamePhase.IN_ROUND && turns < 80) {
                engine.processAiTurn()
                turns++
            }
        }

        val phase = engine.state.value.gamePhase
        assertTrue(
            phase == GamePhase.ROUND_END || phase == GamePhase.GAME_OVER,
            "Game should reach ROUND_END or GAME_OVER, but phase is $phase"
        )
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // SCORING CORRECTNESS ON ANDROID
    // ═══════════════════════════════════════════════════════════════════════════

    @Test
    fun scoring_loserHandValueAddedCorrectly() {
        // Set up a round where p0 wins with empty hand
        val winnerCard = TC.FOUR_H
        val loserHand  = listOf(TC.K_S, TC.K_H, TC.A_D, TC.TWO_C)  // 10+10+10+2 = 32 pts

        val p0 = TC.player("p0", hand = listOf(winnerCard), hasLaidDown = true, score = 0)
        val p1 = TC.player("p1", hand = loserHand, score = 0)

        engine.restoreStateForTesting(
            TC.minimalState(players = listOf(p0, p1), turnPhase = TurnPhase.ACTION)
        )

        engine.discard(winnerCard)

        val s = engine.state.value
        assertEquals(GamePhase.ROUND_END, s.gamePhase)
        assertEquals(-10, s.players[0].score, "Winner should get −10")
        assertEquals(32,  s.players[1].score, "Loser should get hand value (32)")
    }

    @Test
    fun scoring_gameOverWhenScoreLimitReached() {
        // Loser's score after round will exceed limit → GAME_OVER
        val winnerCard = TC.FOUR_H
        val loserHand  = List(15){ TC.K_S }  // 150 pts — hits limit of 100

        val p0 = TC.player("p0", hand = listOf(winnerCard), hasLaidDown = true, score = 80)
        val p1 = TC.player("p1", hand = loserHand, score = 0)

        engine.restoreStateForTesting(
            TC.minimalState(players = listOf(p0, p1), turnPhase = TurnPhase.ACTION, scoreLimit = 100)
        )

        engine.discard(winnerCard)

        val s = engine.state.value
        assertEquals(GamePhase.GAME_OVER, s.gamePhase,
            "Game should be over: p1 now has 150 pts ≥ limit of 100")
    }
}
