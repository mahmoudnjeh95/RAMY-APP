package com.rami.ai

import com.rami.model.*
import com.rami.validator.FormationValidator

/**
 * Pure-function utility that finds valid card formations within a hand
 * and solves for the best Nazoul combination meeting a score threshold.
 *
 * Used by [AiPlayer] for decision-making and by the [FormationBuilderSheet]
 * for real-time validity hints in the UI.
 */
object FormationFinder {

    // ─── Public data ──────────────────────────────────────────────────────────

    data class CandidateFormation(
        val cards: List<Card>,
        val type: FormationType,
        val value: Int
    )

    // ─── Public API ───────────────────────────────────────────────────────────

    /**
     * Returns every valid Set and Sequence that can be formed from [hand].
     * Jokers are shared across candidates; the caller must ensure non-overlapping
     * usage when selecting a subset.
     */
    fun findAllFormations(hand: List<Card>, mode: GameMode): List<CandidateFormation> {
        val regulars = hand.filterIsInstance<Card.Regular>()
        val jokers   = hand.filterIsInstance<Card.Joker>()

        val results  = mutableListOf<CandidateFormation>()
        results += findSets(regulars, jokers, mode)
        results += findSequences(regulars, jokers, mode)

        // Deduplicate by sorted card-id fingerprint
        return results.distinctBy { c -> c.cards.map { it.id }.sorted().joinToString(",") }
    }

    /**
     * Finds a combination of non-overlapping formations from [hand] whose combined
     * point value meets the Nazoul threshold for the given [mode] and [lastNazoulValue].
     *
     * Returns the list-of-lists to pass to [GameEngine.layDown], or null if impossible.
     */
    fun findNazoulCombination(
        hand: List<Card>,
        mode: GameMode,
        lastNazoulValue: Int
    ): List<List<Card>>? {
        val threshold = nazoulThreshold(mode, lastNazoulValue)
        val candidates = findAllFormations(hand, mode).sortedByDescending { it.value }
        return greedySelect(candidates, threshold)
    }

    /**
     * Returns true if adding [card] to [hand] creates at least one new
     * formation candidate (used by AI to evaluate discard-pile picks).
     */
    fun improvesPotential(card: Card, hand: List<Card>, mode: GameMode): Boolean {
        val before = findAllFormations(hand, mode).size
        val after  = findAllFormations(hand + card, mode).size
        return after > before
    }

    /**
     * Scores how "expendable" a card is — higher = safer to discard.
     * Cards part of potential formations receive a large negative penalty.
     * Jokers are never expendable.
     */
    fun expendabilityScore(card: Card, hand: List<Card>, mode: GameMode): Int {
        if (card.isJoker()) return Int.MIN_VALUE  // never discard jokers

        val formationsUsingCard = findAllFormations(hand, mode)
            .count { f -> f.cards.any { it.id == card.id } }

        return if (formationsUsingCard == 0) {
            card.pointValue(mode) * 10   // isolated high-value → great to discard
        } else {
            -formationsUsingCard * 100 + card.pointValue(mode)
        }
    }

    /**
     * Returns the minimum Nazoul value required for the current player.
     */
    fun nazoulThreshold(mode: GameMode, lastNazoulValue: Int): Int = when {
        mode == GameMode.NORMAL  -> mode.minFirstNazoul
        lastNazoulValue == 0    -> mode.minFirstNazoul   // first lay-down this round
        else                    -> lastNazoulValue + 1   // Tafdhil: must exceed
    }

    // ─── Set finding ──────────────────────────────────────────────────────────

    private fun findSets(
        regulars: List<Card.Regular>,
        jokers: List<Card.Joker>,
        mode: GameMode
    ): List<CandidateFormation> {
        val results = mutableListOf<CandidateFormation>()

        regulars.groupBy { it.rank }.forEach { (_, cards) ->
            // Keep one card per suit (sets require distinct suits)
            val bySuit = cards.distinctBy { it.suit }

            // Pure sets (no jokers)
            for (size in 3..minOf(4, bySuit.size)) {
                bySuit.combinations(size).forEach { combo ->
                    val f = combo.map { it as Card }
                    if (FormationValidator.isValid(f, mode)) {
                        results += candidate(f, FormationType.SET, mode)
                    }
                }
            }

            // Sets requiring one joker (2 different suits + 1 joker)
            if (bySuit.size == 2 && jokers.isNotEmpty()) {
                val f = bySuit.map { it as Card } + jokers.first()
                if (FormationValidator.isValid(f, mode)) {
                    results += candidate(f, FormationType.SET, mode)
                }
            }
        }

        return results
    }

    // ─── Sequence finding ─────────────────────────────────────────────────────

    private fun findSequences(
        regulars: List<Card.Regular>,
        jokers: List<Card.Joker>,
        mode: GameMode
    ): List<CandidateFormation> {
        val results = mutableListOf<CandidateFormation>()

        regulars.groupBy { it.suit }.forEach { (_, cards) ->
            // Deduplicate ranks (can't have two 7♥ in one sequence)
            val sorted = cards.distinctBy { it.rank }.sortedBy { it.rank.order }

            // Try every [start, end] window
            for (startIdx in sorted.indices) {
                for (endIdx in (startIdx + 2)..sorted.lastIndex) {
                    val subset = sorted.subList(startIdx, endIdx + 1)
                    val span   = subset.last().rank.order - subset.first().rank.order + 1
                    val gaps   = span - subset.size   // jokers needed for gaps

                    if (gaps <= jokers.size) {
                        val jokersUsed = jokers.take(gaps)
                        val f = subset.map { it as Card } + jokersUsed
                        if (FormationValidator.isValid(f, mode)) {
                            results += candidate(f, FormationType.SEQUENCE, mode)
                        }
                    }
                }

                // Leading joker (joker + 2 consecutive)
                if (jokers.isNotEmpty() && startIdx + 1 < sorted.size) {
                    val next = sorted[startIdx + 1]
                    val cur  = sorted[startIdx]
                    if (next.rank.order == cur.rank.order + 1) {
                        val f = listOf<Card>(jokers.first(), cur, next)
                        if (FormationValidator.isValid(f, mode)) {
                            results += candidate(f, FormationType.SEQUENCE, mode)
                        }
                    }
                }

                // Trailing joker (2 consecutive + joker)
                if (jokers.isNotEmpty() && startIdx + 1 < sorted.size) {
                    val cur  = sorted[startIdx]
                    val next = sorted[startIdx + 1]
                    if (next.rank.order == cur.rank.order + 1) {
                        val f = listOf<Card>(cur, next, jokers.first())
                        if (FormationValidator.isValid(f, mode)) {
                            results += candidate(f, FormationType.SEQUENCE, mode)
                        }
                    }
                }
            }
        }

        return results
    }

    // ─── Greedy combination selector ──────────────────────────────────────────

    /**
     * Greedily picks the highest-value non-overlapping formations until
     * [threshold] is reached.  Returns null if impossible.
     */
    private fun greedySelect(
        sortedDesc: List<CandidateFormation>,
        threshold: Int
    ): List<List<Card>>? {
        val usedIds  = mutableSetOf<String>()
        val selected = mutableListOf<List<Card>>()
        var total    = 0

        for (candidate in sortedDesc) {
            val ids = candidate.cards.map { it.id }.toSet()
            if (ids.any { it in usedIds }) continue   // overlapping cards

            usedIds += ids
            selected += candidate.cards
            total    += candidate.value

            if (total >= threshold) return selected
        }

        return null   // couldn't reach threshold
    }

    // ─── Helpers ─────────────────────────────────────────────────────────────

    private fun candidate(cards: List<Card>, type: FormationType, mode: GameMode) =
        CandidateFormation(cards, type, cards.sumOf { it.pointValue(mode) })
}

// ── Extension: k-combinations of a list ──────────────────────────────────────

private fun <T> List<T>.combinations(r: Int): List<List<T>> {
    if (r == 0) return listOf(emptyList())
    if (isEmpty() || size < r) return emptyList()
    val head = first()
    val tail = drop(1)
    return tail.combinations(r - 1).map { listOf(head) + it } + tail.combinations(r)
}
