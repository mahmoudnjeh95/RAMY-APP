package com.rami.model

/**
 * Mutable deck used **only inside [com.rami.engine.GameEngine]** during setup
 * and reshuffle operations.  [GameState] stores the deck as an immutable
 * [List<Card>] for pure-state reasons.
 */
class Deck(mode: GameMode) {

    private val cards: ArrayDeque<Card> = ArrayDeque()

    init {
        // Two standard 52-card decks
        repeat(2) {
            for (suit in Suit.entries) {
                for (rank in Rank.entries) {
                    cards.addLast(Card.Regular(rank, suit))
                }
            }
        }
        // Mode-specific jokers (4 Normal / 6 Tafdhil)
        repeat(mode.jokerCount) { cards.addLast(Card.Joker()) }

        shuffle()
    }

    fun shuffle()              { val list = cards.toMutableList().also { it.shuffle() }; cards.clear(); cards.addAll(list) }
    fun drawOne(): Card?       = if (cards.isEmpty()) null else cards.removeFirst()
    fun deal(n: Int): List<Card> {
        require(cards.size >= n) { "Not enough cards: need $n, have ${cards.size}" }
        return (1..n).map { cards.removeFirst() }
    }
    fun addAll(more: List<Card>) { more.forEach { cards.addLast(it) } }

    val size  get() = cards.size
    val isEmpty get() = cards.isEmpty()

    /** Drain remaining cards to an immutable list for [GameState]. */
    fun toList(): List<Card> = cards.toList()
}
