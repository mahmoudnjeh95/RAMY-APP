package com.rami.helpers

import com.rami.model.*
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

// ─── GameState assertions ─────────────────────────────────────────────────────

fun GameState.assertPhase(expected: TurnPhase) =
    assertEquals(expected, turnPhase, "Expected turn phase $expected but was $turnPhase")

fun GameState.assertGamePhase(expected: GamePhase) =
    assertEquals(expected, gamePhase, "Expected game phase $expected but was $gamePhase")

fun GameState.assertCurrentPlayerIndex(expected: Int) =
    assertEquals(expected, currentPlayerIndex, "Expected currentPlayerIndex $expected but was $currentPlayerIndex")

fun GameState.assertDeckSize(expected: Int) =
    assertEquals(expected, deck.size, "Expected deck size $expected but was ${deck.size}")

fun GameState.assertDiscardSize(expected: Int) =
    assertEquals(expected, discardPile.size, "Expected discard size $expected but was ${discardPile.size}")

fun GameState.assertFormationCount(expected: Int) =
    assertEquals(expected, tableFormations.size, "Expected $expected formations on table but found ${tableFormations.size}")

fun GameState.assertLastNazoulValue(expected: Int) =
    assertEquals(expected, lastNazoulValue, "Expected lastNazoulValue $expected but was $lastNazoulValue")

// ─── Player assertions ────────────────────────────────────────────────────────

fun Player.assertHandSize(expected: Int) =
    assertEquals(expected, hand.size, "Player $name: expected $expected cards in hand but had ${hand.size}")

fun Player.assertHasLaidDown(expected: Boolean = true) =
    assertEquals(expected, hasLaidDown, "Player $name: hasLaidDown should be $expected")

fun Player.assertScore(expected: Int) =
    assertEquals(expected, score, "Player $name: expected score $expected but was $score")

fun Player.assertJokerBank(expected: Int) =
    assertEquals(expected, jokerBankCount, "Player $name: expected jokerBankCount $expected but was $jokerBankCount")

// ─── Boolean shortcuts ────────────────────────────────────────────────────────

fun assertLayDownSuccess(result: Boolean, msg: String = "layDown() should have succeeded") =
    assertTrue(result, msg)

fun assertLayDownFail(result: Boolean, msg: String = "layDown() should have been rejected") =
    assertFalse(result, msg)
