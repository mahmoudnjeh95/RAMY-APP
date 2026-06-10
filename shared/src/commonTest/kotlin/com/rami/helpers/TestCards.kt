package com.rami.helpers

import com.rami.model.*

/**
 * Deterministic card and state factories for integration tests.
 * All cards have fixed IDs so set/list operations are reproducible.
 */
object TC {

    // ── Suit aliases ──────────────────────────────────────────────────────────
    val S = Suit.SPADES; val H = Suit.HEARTS; val D = Suit.DIAMONDS; val C = Suit.CLUBS

    // ── Card factories ─────────────────────────────────────────────────────────

    fun reg(rank: Rank, suit: Suit) =
        Card.Regular(rank, suit, id = "${rank.name}_${suit.name}_T")

    fun joker(n: Int = 0) = Card.Joker(id = "JKR_T$n")

    // ── Preset cards ───────────────────────────────────────────────────────────

    val A_S  = reg(Rank.ACE,   S);  val A_H  = reg(Rank.ACE,   H)
    val A_D  = reg(Rank.ACE,   D);  val A_C  = reg(Rank.ACE,   C)
    val K_S  = reg(Rank.KING,  S);  val K_H  = reg(Rank.KING,  H)
    val K_D  = reg(Rank.KING,  D);  val K_C  = reg(Rank.KING,  C)
    val Q_S  = reg(Rank.QUEEN, S);  val Q_H  = reg(Rank.QUEEN, H)
    val TEN_H = reg(Rank.TEN,  H);  val TEN_C = reg(Rank.TEN,  C)
    val NINE_H= reg(Rank.NINE, H);  val NINE_C= reg(Rank.NINE, C)
    val EIGHT_H=reg(Rank.EIGHT,H);  val EIGHT_C=reg(Rank.EIGHT,C)
    val SEVEN_H=reg(Rank.SEVEN,H);  val SEVEN_C=reg(Rank.SEVEN,C)
    val SIX_H = reg(Rank.SIX,  H);  val FIVE_H= reg(Rank.FIVE, H)
    val FOUR_H= reg(Rank.FOUR, H);  val TWO_C = reg(Rank.TWO,  C)
    val JKR0  = joker(0);           val JKR1  = joker(1)

    // ── Valid formation presets ────────────────────────────────────────────────

    /** SET: A♠ A♥ A♦ → 30 pts (Normal), 30 pts (Tafdhil) */
    fun set3Aces()  = listOf(A_S, A_H, A_D)

    /** SET: K♠ K♥ K♦ → 30 pts */
    fun set3Kings() = listOf(K_S, K_H, K_D)

    /** SET with Joker: K♠ K♥ ★ → 10+10+10(N)/50(T) */
    fun set2KingsJoker() = listOf(K_S, K_H, JKR0)

    /** SEQUENCE: 7♥ 8♥ 9♥ → 24 pts */
    fun seq7to9Hearts()  = listOf(SEVEN_H, EIGHT_H, NINE_H)

    /** SEQUENCE: 5♥ 6♥ 7♥ → 18 pts */
    fun seq5to7Hearts()  = listOf(FIVE_H, SIX_H, SEVEN_H)

    /** SEQUENCE with Joker: 5♥ ★ 7♥ → 18+10(N)/18+50(T) */
    fun seqJokerMiddle() = listOf(FIVE_H, JKR0, SEVEN_H)

    // ── Hands meeting Nazoul threshold ─────────────────────────────────────────

    /**
     * Hand + formations that total ≥ 51 pts for Normal mode.
     * set3Aces(30) + seq7to9Hearts(24) = 54 pts ✓
     * Returns the two formation lists ready to pass to [GameEngine.layDown].
     */
    fun formations51Normal()  = listOf(set3Aces(), seq7to9Hearts())

    /**
     * Hand that totals < 51 pts — should be REJECTED in Normal mode.
     * seq5to7Hearts = 18 pts only.
     */
    fun formationBelow51()    = listOf(seq5to7Hearts())

    /**
     * Formations totalling ≥ 71 pts for Tafdhil mode (Joker = 50 pts).
     * set3Aces(30) + set2KingsJoker(10+10+50=70) = 100 pts ✓
     */
    fun formations71Tafdhil() = listOf(set3Aces(), set2KingsJoker())

    /**
     * Formations totalling < 71 pts — should be REJECTED for Tafdhil first lay-down.
     * set3Aces(30) + seq7to9Hearts(24) = 54 pts only.
     */
    fun formationBelow71()    = listOf(set3Aces(), seq7to9Hearts())

    // ── State builders ─────────────────────────────────────────────────────────

    /**
     * Returns a list of all cards referenced in [formations] — used to
     * pre-populate a player's hand so layDown() can remove them.
     */
    fun allCards(vararg formations: List<Card>): List<Card> =
        formations.flatMap { it }

    /**
     * Builds a minimal [GameState] with exactly the provided players and
     * an almost-empty deck, ready for controlled integration tests.
     */
    fun minimalState(
        mode: GameMode = GameMode.NORMAL,
        players: List<Player>,
        deck: List<Card> = List(30) { reg(Rank.TWO, C) },  // filler cards
        discardPile: List<Card> = listOf(reg(Rank.THREE, C)),
        tableFormations: List<Formation> = emptyList(),
        currentPlayerIndex: Int = 0,
        turnPhase: TurnPhase = TurnPhase.DRAW,
        lastNazoulValue: Int = 0,
        scoreLimit: Int = 150
    ) = GameState(
        mode               = mode,
        players            = players,
        deck               = deck,
        discardPile        = discardPile,
        tableFormations    = tableFormations,
        currentPlayerIndex = currentPlayerIndex,
        turnPhase          = turnPhase,
        lastNazoulValue    = lastNazoulValue,
        scoreLimit         = scoreLimit,
        gamePhase          = GamePhase.IN_ROUND
    )

    /**
     * Creates a [Player] with a preset [hand].
     */
    fun player(
        id: String = "p0",
        name: String = "Test",
        hand: List<Card> = emptyList(),
        hasLaidDown: Boolean = false,
        nazoulValue: Int = 0,
        score: Int = 0,
        jokerBankCount: Int = 0
    ) = Player(
        id             = id,
        name           = name,
        hand           = hand,
        hasLaidDown    = hasLaidDown,
        nazoulValue    = nazoulValue,
        score          = score,
        jokerBankCount = jokerBankCount
    )
}
