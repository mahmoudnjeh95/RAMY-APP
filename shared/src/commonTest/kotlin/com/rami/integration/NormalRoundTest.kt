package com.rami.integration

import com.rami.engine.GameEngine
import com.rami.helpers.*
import com.rami.model.*
import kotlin.test.*

/**
 * End-to-end Normal mode round integration tests (GDD §§4–7).
 * Each test drives the [GameEngine] directly — no mocking.
 */
class NormalRoundTest {

    private lateinit var engine: GameEngine

    @BeforeTest fun setup() { engine = GameEngine() }

    // ═══════════════════════════════════════════════════════════════════════════
    // NAZOUL (LAY-DOWN) — NORMAL MODE
    // ═══════════════════════════════════════════════════════════════════════════

    @Test
    fun layDown_exactly51pts_succeeds() {
        // set3Aces(30) + seq7to9Hearts(24) = 54 pts ≥ 51 ✓
        val formations = TC.formations51Normal()
        val handCards  = TC.allCards(*formations.toTypedArray()) +
                         listOf(TC.TWO_C, TC.FOUR_H)   // extra filler

        val p0 = TC.player("p0", hand = handCards)
        val p1 = TC.player("p1", "Opp", hand = List(14){ TC.reg(Rank.TWO, TC.S) })

        engine.restoreStateForTesting(
            TC.minimalState(players = listOf(p0, p1), turnPhase = TurnPhase.ACTION)
        )

        val ok = engine.layDown(formations)

        assertLayDownSuccess(ok)
        val s = engine.state.value
        s.players[0].assertHasLaidDown(true)
        s.assertFormationCount(2)
    }

    @Test
    fun layDown_below51pts_rejected() {
        // seq5to7Hearts = 18 pts — must be rejected
        val formations = TC.formationBelow51()
        val handCards  = TC.allCards(*formations.toTypedArray()) +
                         List(12){ TC.reg(Rank.TWO, TC.C) }

        val p0 = TC.player("p0", hand = handCards)
        val p1 = TC.player("p1", "Opp", hand = List(14){ TC.reg(Rank.TWO, TC.S) })

        engine.restoreStateForTesting(
            TC.minimalState(players = listOf(p0, p1), turnPhase = TurnPhase.ACTION)
        )

        val ok = engine.layDown(formations)

        assertLayDownFail(ok, "layDown with only 18 pts should be rejected (min 51)")
        engine.state.value.players[0].assertHasLaidDown(false)
        engine.state.value.assertFormationCount(0)
    }

    @Test
    fun layDown_invalidFormation_rejected() {
        // Two cards of same suit — invalid set
        val invalid = listOf(TC.K_S, TC.A_S, TC.Q_S)   // all Spades → invalid SET
        val p0 = TC.player("p0", hand = invalid + List(10){ TC.reg(Rank.TWO, TC.C) })
        val p1 = TC.player("p1", "Opp", hand = List(14){ TC.reg(Rank.TWO, TC.S) })

        engine.restoreStateForTesting(
            TC.minimalState(players = listOf(p0, p1), turnPhase = TurnPhase.ACTION)
        )

        val ok = engine.layDown(listOf(invalid))
        assertLayDownFail(ok, "Same-suit set should be invalid")
    }

    @Test
    fun layDown_removesCardsFromHand() {
        val formations = TC.formations51Normal()
        val extra      = List(5){ TC.reg(Rank.TWO, TC.C) }
        val p0         = TC.player("p0", hand = TC.allCards(*formations.toTypedArray()) + extra)
        val p1         = TC.player("p1", hand = List(14){ TC.reg(Rank.THREE, TC.S) })

        engine.restoreStateForTesting(
            TC.minimalState(players = listOf(p0, p1), turnPhase = TurnPhase.ACTION)
        )

        engine.layDown(formations)

        // Hand should only have the filler cards left
        engine.state.value.players[0].assertHandSize(extra.size)
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // ADD TO FORMATION
    // ═══════════════════════════════════════════════════════════════════════════

    @Test
    fun addCardToFormation_validCard_succeeds() {
        // Existing formation: A♠ A♥ A♦ (valid set, 3 aces)
        // Player holds A♣ — can be added as 4th ace
        val existingFormation = Formation(
            id      = "F_TEST",
            type    = FormationType.SET,
            cards   = TC.set3Aces(),
            ownerId = "p1"
        )
        val p0 = TC.player("p0", hand = listOf(TC.A_C, TC.TWO_C), hasLaidDown = true)
        val p1 = TC.player("p1", hand = List(10){ TC.reg(Rank.THREE, TC.S) })

        engine.restoreStateForTesting(
            TC.minimalState(
                players          = listOf(p0, p1),
                tableFormations  = listOf(existingFormation),
                turnPhase        = TurnPhase.ACTION
            )
        )

        val ok = engine.addCardToFormation(TC.A_C, "F_TEST")

        assertTrue(ok)
        val s = engine.state.value
        // A♣ removed from hand
        assertFalse(s.players[0].hand.any { it.id == TC.A_C.id })
        // Formation now has 4 cards
        val updated = s.tableFormations.first { it.id == "F_TEST" }
        assertEquals(4, updated.cards.size)
    }

    @Test
    fun addCardToFormation_withoutNazoul_fails() {
        val formation = Formation("F_TEST", FormationType.SET, TC.set3Aces(), "p1")
        val p0 = TC.player("p0", hand = listOf(TC.A_C), hasLaidDown = false)  // not laid down
        val p1 = TC.player("p1", hand = List(10){ TC.reg(Rank.THREE, TC.S) })

        engine.restoreStateForTesting(
            TC.minimalState(
                players = listOf(p0, p1), tableFormations = listOf(formation),
                turnPhase = TurnPhase.ACTION
            )
        )

        val ok = engine.addCardToFormation(TC.A_C, "F_TEST")
        assertFalse(ok)
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // JOKER STEAL — NORMAL MODE
    // ═══════════════════════════════════════════════════════════════════════════

    @Test
    fun jokerSteal_validReplacement_succeeds() {
        // Formation: A♠ A♥ ★(Joker replacing A♦)
        val jokerInFormation = TC.JKR0
        val formation = Formation(
            id      = "F_STEAL",
            type    = FormationType.SET,
            cards   = listOf(TC.A_S, TC.A_H, jokerInFormation),
            ownerId = "p1"
        )
        // Player has A♦ — the exact card the Joker replaces → can steal
        val p0 = TC.player("p0", hand = listOf(TC.A_D, TC.TWO_C), hasLaidDown = true)
        val p1 = TC.player("p1", hand = List(8){ TC.reg(Rank.THREE, TC.S) })

        engine.restoreStateForTesting(
            TC.minimalState(
                players         = listOf(p0, p1),
                tableFormations = listOf(formation),
                turnPhase       = TurnPhase.ACTION
            )
        )

        val ok = engine.stealJoker("F_STEAL", jokerIndex = 2, replacement = TC.A_D)

        assertTrue(ok, "Joker steal with valid replacement should succeed")
        val s = engine.state.value
        // Player now holds the Joker
        assertTrue(s.players[0].hand.any { it.isJoker() }, "Stolen Joker should be in player's hand")
        // A♦ is now in the formation
        val updatedF = s.tableFormations.first { it.id == "F_STEAL" }
        assertTrue(updatedF.cards.any { it.id == TC.A_D.id })
        // A♦ removed from hand
        assertFalse(s.players[0].hand.any { it.id == TC.A_D.id })
    }

    @Test
    fun jokerSteal_invalidReplacement_fails() {
        val joker     = TC.JKR0
        val formation = Formation("F_STEAL", FormationType.SET,
            listOf(TC.A_S, TC.A_H, joker), "p1")
        // K_D cannot replace the Joker (different rank) → invalid
        val p0 = TC.player("p0", hand = listOf(TC.K_D), hasLaidDown = true)
        val p1 = TC.player("p1", hand = List(8){ TC.reg(Rank.THREE, TC.S) })

        engine.restoreStateForTesting(
            TC.minimalState(
                players = listOf(p0, p1), tableFormations = listOf(formation),
                turnPhase = TurnPhase.ACTION
            )
        )

        val ok = engine.stealJoker("F_STEAL", jokerIndex = 2, replacement = TC.K_D)
        assertFalse(ok, "Replacing Ace Joker with King should fail")
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // FULL ROUND SIMULATION
    // ═══════════════════════════════════════════════════════════════════════════

    @Test
    fun fullRound_winnerByDiscardLastCard_scoring() {
        // p0 has exactly 1 card left after laying down; discards it to win
        val formations  = TC.formations51Normal()
        val lastCard    = TC.FOUR_H
        val p0Hand      = TC.allCards(*formations.toTypedArray()) + listOf(lastCard)
        val p1Hand      = List(10){ TC.reg(Rank.TWO, TC.C) }   // 20 pts worth

        val p0 = TC.player("p0", "Winner", hand = p0Hand,  score = 0)
        val p1 = TC.player("p1", "Loser",  hand = p1Hand,  score = 0)

        engine.restoreStateForTesting(
            TC.minimalState(players = listOf(p0, p1), turnPhase = TurnPhase.ACTION)
        )

        // p0 lays down then discards last card
        engine.layDown(formations)
        engine.discard(engine.state.value.players[0].hand.first())  // discard lastCard

        val s = engine.state.value
        s.assertGamePhase(GamePhase.ROUND_END)
        assertEquals("p0", s.roundWinnerId)
        // Winner gets −10, loser gets hand value (10 × 2pts = 20)
        assertEquals(-10, s.players[0].score)
        assertEquals(20,  s.players[1].score)
    }

    @Test
    fun fullRound_winnerByAddingLastCardToFormation() {
        val lastCard = TC.A_C
        val existingF = Formation("F1", FormationType.SET, TC.set3Aces(), "p1")
        val p0 = TC.player("p0", hand = listOf(lastCard), hasLaidDown = true, score = 0)
        val p1 = TC.player("p1", hand = List(5){ TC.reg(Rank.FIVE, TC.H) }, score = 0)

        engine.restoreStateForTesting(
            TC.minimalState(
                players = listOf(p0, p1), tableFormations = listOf(existingF),
                turnPhase = TurnPhase.ACTION
            )
        )

        engine.addCardToFormation(lastCard, "F1")  // empties hand → round end

        val s = engine.state.value
        s.assertGamePhase(GamePhase.ROUND_END)
        assertEquals("p0", s.roundWinnerId)
    }

    @Test
    fun nextRound_resetsPerRoundState_preservesScores() {
        engine.startGame(listOf("Ali","Brahim"), GameMode.NORMAL, 150)
        // Drive a round to completion by giving p0 one card and discarding
        val patchedState = engine.state.value.let { s ->
            s.copy(
                players = s.players.mapIndexed { i, p ->
                    if (i == 0) p.copy(hand = listOf(TC.TWO_C), score = 0, hasLaidDown = true)
                    else        p.copy(hand = List(5){ TC.reg(Rank.FIVE, TC.H) }, score = 0)
                },
                turnPhase = TurnPhase.ACTION,
                gamePhase = GamePhase.IN_ROUND
            )
        }
        engine.restoreStateForTesting(patchedState)
        engine.discard(TC.TWO_C)   // wins round

        val afterRound = engine.state.value
        assertEquals(GamePhase.ROUND_END, afterRound.gamePhase)

        engine.startNextRound()

        val s = engine.state.value
        s.assertGamePhase(GamePhase.IN_ROUND)
        assertEquals(2, s.roundNumber)
        s.players.forEach { it.assertHasLaidDown(false) }   // reset
        // Scores preserved
        assertNotEquals(0, s.players.sumOf { it.score })
    }
}
