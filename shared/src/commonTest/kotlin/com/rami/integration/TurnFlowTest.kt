package com.rami.integration

import com.rami.engine.GameEngine
import com.rami.helpers.*
import com.rami.model.*
import kotlin.test.*

/**
 * Integration tests covering the GDD §3 turn structure:
 *   DRAW → ACTION → DISCARD → (next player's DRAW)
 *
 * Runs on JVM, Android (unit), and iOS simulator via commonTest.
 */
class TurnFlowTest {

    private lateinit var engine: GameEngine

    @BeforeTest
    fun setup() { engine = GameEngine() }

    // ═══════════════════════════════════════════════════════════════════════════
    // DEAL
    // ═══════════════════════════════════════════════════════════════════════════

    @Test
    fun deal_firstPlayer_gets15Cards_othersGet14() {
        engine.startGame(listOf("Ali", "Brahim"), GameMode.NORMAL, 150)
        val s = engine.state.value
        s.players[0].assertHandSize(15)  // first player
        s.players[1].assertHandSize(14)  // others
    }

    @Test
    fun deal_4players_deckSizeIsCorrect() {
        engine.startGame(listOf("A","B","C","D"), GameMode.NORMAL, 150)
        val s = engine.state.value
        // 108 total − 15 − 14 − 14 − 14 = 51 remaining in deck
        s.assertDeckSize(51)
        s.assertGamePhase(GamePhase.IN_ROUND)
        s.assertPhase(TurnPhase.DRAW)
    }

    @Test
    fun deal_tafdhilMode_110Cards_distributed() {
        engine.startGame(listOf("A","B"), GameMode.TAFDHIL, 150)
        val s = engine.state.value
        // 110 − 15 − 14 = 81 in deck
        s.assertDeckSize(81)
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // DRAW FROM DECK
    // ═══════════════════════════════════════════════════════════════════════════

    @Test
    fun drawFromDeck_addsOneCardToHand_decrementsDeck() {
        engine.startGame(listOf("Ali","Brahim"), GameMode.NORMAL, 150)
        val before = engine.state.value

        engine.drawFromDeck()
        val after = engine.state.value

        after.players[0].assertHandSize(16)          // 15+1
        assertEquals(before.deck.size - 1, after.deck.size)
        after.assertPhase(TurnPhase.ACTION)
    }

    @Test
    fun drawFromDeck_ignoredIfNotDrawPhase() {
        // Put engine into ACTION phase
        engine.startGame(listOf("Ali","Brahim"), GameMode.NORMAL, 150)
        engine.drawFromDeck()                         // → ACTION
        val handAfterFirstDraw = engine.state.value.players[0].hand.size

        engine.drawFromDeck()                         // should be ignored
        assertEquals(handAfterFirstDraw, engine.state.value.players[0].hand.size)
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // DRAW FROM DISCARD
    // ═══════════════════════════════════════════════════════════════════════════

    @Test
    fun drawFromDiscard_withNazoul_succeeds() {
        val p0Hand = TC.allCards(*TC.formations51Normal().toTypedArray()) +
                     List(5) { TC.reg(Rank.TWO, TC.C) }
        val p0 = TC.player("p0", "Ali",   hand = p0Hand)
        val p1 = TC.player("p1", "Brahim", hand = List(14) { TC.reg(Rank.THREE, TC.S) })
        val discardCard = TC.K_C

        engine.restoreStateForTesting(
            TC.minimalState(
                players     = listOf(p0, p1),
                discardPile = listOf(discardCard),
                turnPhase   = TurnPhase.DRAW
            )
        )

        // Lay down first (so hasLaidDown = true)
        engine.drawFromDeck()
        val formations = TC.formations51Normal()
        engine.layDown(formations)

        // Move to next turn and make sure p0 can draw from discard on a subsequent turn
        // For simplicity: manually restore state with hasLaidDown=true
        val withLaid = engine.state.value.copy(
            turnPhase = TurnPhase.DRAW,
            players   = engine.state.value.players.mapIndexed { i, p ->
                if (i == 0) p.copy(hasLaidDown = true) else p
            },
            discardPile = listOf(discardCard),
            currentPlayerIndex = 0
        )
        engine.restoreStateForTesting(withLaid)

        engine.drawFromDiscard()

        val s = engine.state.value
        s.assertPhase(TurnPhase.ACTION)
        // Discard top card is now in player's hand
        assertTrue(s.players[0].hand.any { it.id == discardCard.id })
        s.assertDiscardSize(0)
    }

    @Test
    fun drawFromDiscard_withoutNazoul_normalMode_returnsCard() {
        val p0 = TC.player("p0", "Ali",    hand = List(15){ TC.reg(Rank.TWO, TC.C) })
        val p1 = TC.player("p1", "Brahim", hand = List(14){ TC.reg(Rank.THREE, TC.S) })

        engine.restoreStateForTesting(
            TC.minimalState(
                players     = listOf(p0, p1),
                discardPile = listOf(TC.K_S),
                turnPhase   = TurnPhase.DRAW
            )
        )

        engine.drawFromDiscard()   // penalty — card stays in discard

        val s = engine.state.value
        // Player's hand must NOT grow
        s.players[0].assertHandSize(15)
        // Discard pile must NOT shrink
        s.assertDiscardSize(1)
        // Phase resets to DRAW (must draw from deck)
        s.assertPhase(TurnPhase.DRAW)
    }

    @Test
    fun drawFromDiscard_withoutNazoul_tafdhilMode_allCardsLost() {
        val hand = List(10) { TC.reg(Rank.TWO, TC.C) }
        val p0   = TC.player("p0", "Ali",    hand = hand)
        val p1   = TC.player("p1", "Brahim", hand = List(14){ TC.reg(Rank.THREE, TC.S) })

        engine.restoreStateForTesting(
            TC.minimalState(
                mode        = GameMode.TAFDHIL,
                players     = listOf(p0, p1),
                discardPile = listOf(TC.K_S),
                turnPhase   = TurnPhase.DRAW
            )
        )

        engine.drawFromDiscard()   // Tafdhil penalty

        val s = engine.state.value
        // All hand cards go to discard pile (GDD §9)
        s.players[0].assertHandSize(0)
        assertTrue(s.discardPile.size >= hand.size, "All hand cards should be in discard")
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // DISCARD
    // ═══════════════════════════════════════════════════════════════════════════

    @Test
    fun discard_removesCardFromHand_advancesTurn() {
        engine.startGame(listOf("Ali","Brahim"), GameMode.NORMAL, 150)
        engine.drawFromDeck()     // → ACTION, 16 cards in hand
        val card = engine.state.value.players[0].hand.first()
        engine.discard(card)

        val s = engine.state.value
        s.assertPhase(TurnPhase.DRAW)           // next player's turn
        s.assertCurrentPlayerIndex(1)
        assertFalse(s.players[0].hand.any { it.id == card.id })
        assertEquals(card.id, s.discardPile.last().id)
    }

    @Test
    fun discard_lastCard_endsRound() {
        // Give player exactly 1 card in hand after draw
        val theCard = TC.K_C
        val p0 = TC.player("p0", "Ali",    hand = listOf(theCard), hasLaidDown = true)
        val p1 = TC.player("p1", "Brahim", hand = List(14){ TC.reg(Rank.TWO, TC.C) })

        engine.restoreStateForTesting(
            TC.minimalState(players = listOf(p0, p1), turnPhase = TurnPhase.ACTION)
        )

        engine.discard(theCard)

        val s = engine.state.value
        assertEquals(GamePhase.ROUND_END, s.gamePhase)
        assertEquals("p0", s.roundWinnerId)
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // DECK RESHUFFLE (GDD §3)
    // ═══════════════════════════════════════════════════════════════════════════

    @Test
    fun deck_reshufflesFromDiscardWhenEmpty() {
        val discardCards = List(10) { i -> TC.reg(Rank.TWO, TC.C).copy(id = "DISCARD_$i") }
        val topCard      = TC.K_H

        val p0 = TC.player("p0", "Ali",    hand = listOf(TC.A_S))
        val p1 = TC.player("p1", "Brahim", hand = List(5){ TC.reg(Rank.THREE, TC.S) })

        engine.restoreStateForTesting(
            TC.minimalState(
                players     = listOf(p0, p1),
                deck        = emptyList(),          // empty deck forces reshuffle
                discardPile = discardCards + topCard
            )
        )

        engine.drawFromDeck()   // should trigger reshuffle internally

        val s = engine.state.value
        // Top discard card stays; others become the new deck
        assertTrue(s.deck.isNotEmpty() || s.players[0].hand.size > 1,
            "Deck should have been replenished from discard pile")
        // Top card preserved
        assertEquals(topCard.id, s.discardPile.last().id)
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // TURN ADVANCEMENT
    // ═══════════════════════════════════════════════════════════════════════════

    @Test
    fun turn_advancesCircularlyAmongPlayers() {
        engine.startGame(listOf("A","B","C","D"), GameMode.NORMAL, 150)

        // Drive turns for 4 full cycles and verify index wraps
        repeat(4) { cycle ->
            repeat(4) { i ->
                engine.state.value.assertCurrentPlayerIndex(i)
                engine.drawFromDeck()
                engine.discard(engine.state.value.players[i].hand.first())
            }
        }
    }
}
