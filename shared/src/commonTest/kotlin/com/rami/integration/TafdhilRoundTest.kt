package com.rami.integration

import com.rami.engine.GameEngine
import com.rami.helpers.*
import com.rami.model.*
import kotlin.test.*

/**
 * Integration tests covering Tafdhil-specific rules (GDD §§4.2, 5.2–5.3, 6.2, 9).
 */
class TafdhilRoundTest {

    private lateinit var engine: GameEngine

    @BeforeTest fun setup() { engine = GameEngine() }

    // ═══════════════════════════════════════════════════════════════════════════
    // ESCALATING NAZOUL (GDD §4.2)
    // ═══════════════════════════════════════════════════════════════════════════

    @Test
    fun firstPlayer_minNazoul_71pts() {
        // formations71Tafdhil = set3Aces(30) + set2KingsJoker(10+10+50=70) = 100 pts ✓
        val formations = TC.formations71Tafdhil()
        val p0 = TC.player("p0", hand = TC.allCards(*formations.toTypedArray()) + List(5){ TC.reg(Rank.TWO, TC.C) })
        val p1 = TC.player("p1", hand = List(14){ TC.reg(Rank.TWO, TC.S) })

        engine.restoreStateForTesting(
            TC.minimalState(
                mode           = GameMode.TAFDHIL,
                players        = listOf(p0, p1),
                turnPhase      = TurnPhase.ACTION,
                lastNazoulValue = 0   // first lay-down this round
            )
        )

        val ok = engine.layDown(formations)

        assertLayDownSuccess(ok, "100 pts should be accepted for Tafdhil first Nazoul (min 71)")
        engine.state.value.assertLastNazoulValue(100)
    }

    @Test
    fun firstPlayer_below71pts_rejected_inTafdhil() {
        // formations51Normal = 54 pts — fine for Normal but NOT for Tafdhil
        val formations = TC.formations51Normal()
        val p0 = TC.player("p0", hand = TC.allCards(*formations.toTypedArray()) + List(5){ TC.reg(Rank.TWO, TC.C) })
        val p1 = TC.player("p1", hand = List(14){ TC.reg(Rank.TWO, TC.S) })

        engine.restoreStateForTesting(
            TC.minimalState(
                mode           = GameMode.TAFDHIL,
                players        = listOf(p0, p1),
                turnPhase      = TurnPhase.ACTION,
                lastNazoulValue = 0
            )
        )

        val ok = engine.layDown(formations)
        assertLayDownFail(ok, "54 pts should be rejected for Tafdhil first Nazoul (min 71)")
    }

    @Test
    fun secondPlayer_mustExceed_previousNazoulValue() {
        // Previous player laid 100 pts → second player needs ≥ 101 pts
        val formations100 = TC.formations71Tafdhil()  // = 100 pts

        val handCards = TC.allCards(*formations100.toTypedArray()) + List(5){ TC.reg(Rank.TWO, TC.C) }
        val p0 = TC.player("p1", hand = handCards)
        val p1 = TC.player("p0", hand = List(14){ TC.reg(Rank.TWO, TC.S) })

        engine.restoreStateForTesting(
            TC.minimalState(
                mode            = GameMode.TAFDHIL,
                players         = listOf(p0, p1),
                turnPhase       = TurnPhase.ACTION,
                lastNazoulValue = 100,  // previous player laid 100 pts
                currentPlayerIndex = 0
            )
        )

        // 100 pts is NOT > 100 → must fail
        val okEqual = engine.layDown(formations100)
        assertLayDownFail(okEqual, "100 pts should not exceed lastNazoulValue of 100 in Tafdhil")
    }

    @Test
    fun secondPlayer_exceedsPrevious_succeeds() {
        // Previous player laid 54 pts → second player lays 100 pts → accepted
        val formations = TC.formations71Tafdhil()  // = 100 pts
        val hand       = TC.allCards(*formations.toTypedArray()) + List(5){ TC.reg(Rank.TWO, TC.C) }
        val p0 = TC.player("p0", hand = hand)
        val p1 = TC.player("p1", hand = List(14){ TC.reg(Rank.TWO, TC.S) })

        engine.restoreStateForTesting(
            TC.minimalState(
                mode            = GameMode.TAFDHIL,
                players         = listOf(p0, p1),
                turnPhase       = TurnPhase.ACTION,
                lastNazoulValue = 54   // previous player laid 54 pts
            )
        )

        val ok = engine.layDown(formations)  // 100 > 54 ✓
        assertLayDownSuccess(ok)
        engine.state.value.assertLastNazoulValue(100)
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // JOKER BANK (GDD §5.3)
    // ═══════════════════════════════════════════════════════════════════════════

    @Test
    fun jokerBank_4accumulated_gives100Deduction() {
        // Simulate winner having 3 banked Jokers + throwing 1 this round → bank = 4 → −100
        val joker = TC.JKR0
        val p0 = TC.player("p0", hand = listOf(joker), hasLaidDown = true,
                           jokerBankCount = 3, score = 50)
        val p1 = TC.player("p1", hand = List(8){ TC.reg(Rank.TWO, TC.C) }, score = 80)

        engine.restoreStateForTesting(
            TC.minimalState(
                mode      = GameMode.TAFDHIL,
                players   = listOf(p0, p1),
                turnPhase = TurnPhase.ACTION
            )
        )

        // Winner throws Joker to centre, assigning the +50 to p1
        engine.finishByThrowingJokers(mapOf("p1" to 1))

        val s = engine.state.value
        assertEquals(GamePhase.ROUND_END, s.gamePhase)

        // p0 (winner): score 50 + (−10 winner) + (−100 bank bonus) = −60
        assertEquals(-60, s.players[0].score)
        // p0 bank resets to 0 (4 % 4 = 0)
        s.players[0].assertJokerBank(0)

        // p1 (loser): 80 (hand value 2×8=16) + 50 (assigned Joker) = 146
        assertEquals(80 + 16 + 50, s.players[1].score)
    }

    @Test
    fun jokerBank_lessThan4_noBonus() {
        val joker = TC.JKR0
        val p0 = TC.player("p0", hand = listOf(joker), hasLaidDown = true,
                           jokerBankCount = 2, score = 0)
        val p1 = TC.player("p1", hand = List(5){ TC.reg(Rank.TWO, TC.C) }, score = 0)

        engine.restoreStateForTesting(
            TC.minimalState(
                mode = GameMode.TAFDHIL, players = listOf(p0, p1), turnPhase = TurnPhase.ACTION
            )
        )

        engine.finishByThrowingJokers(mapOf("p1" to 1))

        val s = engine.state.value
        // p0 bank should be 3 (no deduction)
        s.players[0].assertJokerBank(3)
        // p0 score = 0 − 10 (winner reward) = −10
        assertEquals(-10, s.players[0].score)
    }

    @Test
    fun jokerBank_clearOnSecondLifeBuyIn() {
        val p0 = TC.player("p0", hand = List(5){ TC.TWO_C }, jokerBankCount = 3, score = 160)
        val p1 = TC.player("p1", hand = List(5){ TC.TWO_C }, jokerBankCount = 2, score = 80)
        val p2 = TC.player("p2", hand = List(5){ TC.TWO_C }, jokerBankCount = 1, score = 40)
        val p3 = TC.player("p3", hand = List(5){ TC.TWO_C }, jokerBankCount = 0, score = 20)

        engine.restoreStateForTesting(
            TC.minimalState(mode = GameMode.TAFDHIL, players = listOf(p0, p1, p2, p3))
        )

        // p0 buys a second life → all banks < 4 are cleared
        engine.buySecondLife("p0")

        val s = engine.state.value
        s.players.forEach { p -> p.assertJokerBank(0) }  // all cleared (all were < 4)
        assertTrue(s.players[0].hasSecondLife)
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // FINISH BY THROWING JOKERS (GDD §6.2 Option A)
    // ═══════════════════════════════════════════════════════════════════════════

    @Test
    fun finishByThrowingJokers_assignsPenaltyToChosen() {
        val j0 = TC.joker(0)
        val j1 = TC.joker(1)
        val p0 = TC.player("p0", hand = listOf(j0, j1), hasLaidDown = true, score = 0)
        val p1 = TC.player("p1", hand = List(6){ TC.reg(Rank.TWO, TC.C) }, score = 0)
        val p2 = TC.player("p2", hand = List(6){ TC.reg(Rank.THREE, TC.C) }, score = 0)

        engine.restoreStateForTesting(
            TC.minimalState(
                mode = GameMode.TAFDHIL, players = listOf(p0, p1, p2), turnPhase = TurnPhase.ACTION
            )
        )

        // Winner assigns 1 Joker to p1 and 1 Joker to p2
        engine.finishByThrowingJokers(mapOf("p1" to 1, "p2" to 1))

        val s = engine.state.value
        assertEquals(GamePhase.ROUND_END, s.gamePhase)
        // Each opponent gets +50 for the assigned Joker + hand value
        val p1Score = 12 + 50   // 2×6=12 hand value + 50 joker
        val p2Score = 18 + 50   // 3×6=18 hand value + 50 joker
        assertEquals(p1Score, s.players[1].score)
        assertEquals(p2Score, s.players[2].score)
    }

    @Test
    fun finishByThrowingJokers_requiresJokersOnly() {
        // Hand has a regular card mixed with joker → cannot use this finish option
        val p0 = TC.player("p0", hand = listOf(TC.JKR0, TC.K_S), hasLaidDown = true)
        val p1 = TC.player("p1", hand = List(6){ TC.reg(Rank.TWO, TC.C) })

        engine.restoreStateForTesting(
            TC.minimalState(mode = GameMode.TAFDHIL, players = listOf(p0, p1), turnPhase = TurnPhase.ACTION)
        )

        val ok = engine.finishByThrowingJokers(mapOf("p1" to 1))
        assertFalse(ok, "Cannot throw jokers when hand contains regular cards")
        engine.state.value.assertGamePhase(GamePhase.IN_ROUND)
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // TAFDHIL PENALTY (GDD §9)
    // ═══════════════════════════════════════════════════════════════════════════

    @Test
    fun tafdhilPenalty_drawFromDiscard_withoutNazoul_losesAllCards() {
        val hand = List(8){ TC.reg(Rank.KING, TC.S) }
        val p0   = TC.player("p0", hand = hand, hasLaidDown = false)
        val p1   = TC.player("p1", hand = List(14){ TC.reg(Rank.TWO, TC.C) })

        engine.restoreStateForTesting(
            TC.minimalState(
                mode        = GameMode.TAFDHIL,
                players     = listOf(p0, p1),
                discardPile = listOf(TC.A_S),
                turnPhase   = TurnPhase.DRAW
            )
        )

        engine.drawFromDiscard()  // triggers Tafdhil penalty

        val s = engine.state.value
        s.players[0].assertHandSize(0)     // all cards lost
        assertTrue(s.discardPile.size >= hand.size, "Lost cards go to discard pile")
    }
}
