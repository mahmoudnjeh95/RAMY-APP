package com.rami.scorer

import com.rami.model.*

/**
 * Pure-function score calculator (GDD §7).
 *
 * Scoring table:
 *  Winner of round       → −10 pts
 *  Loser (cards in hand) → +sum of remaining card values
 *  4 banked Jokers (Tafdhil) → −100 pts for the holder, bank resets to 0
 *  Received Tafdhil Joker    → +50 pts per Joker assigned by winner
 */
object ScoreCalculator {

    // ─── Result ───────────────────────────────────────────────────────────────

    data class RoundResult(
        /** Map of playerId → raw point delta for this round (before bank bonus) */
        val baseDeltas: Map<String, Int>,
        /** Map of playerId → bonus delta from Joker-bank reaching 4 (always ≤ 0) */
        val bankBonusDeltas: Map<String, Int>,
        /** Map of playerId → updated jokerBankCount after this round */
        val updatedBanks: Map<String, Int>
    ) {
        /** Combined delta = base + bank bonus */
        fun totalDelta(playerId: String): Int =
            (baseDeltas[playerId] ?: 0) + (bankBonusDeltas[playerId] ?: 0)
    }

    // ─── Main entry point ─────────────────────────────────────────────────────

    /**
     * @param state                    Current [GameState] at round end
     * @param winnerId                 Id of the player who emptied their hand
     * @param tafdhilJokerAssignments  Tafdhil only — map of playerId to number of
     *                                 Jokers the winner assigned to them (GDD §6.2)
     * @param winnerBankedJokers       Tafdhil only — how many Jokers the winner
     *                                 threw to the centre (adds to their bank, GDD §5.3)
     */
    fun calculateRound(
        state: GameState,
        winnerId: String,
        tafdhilJokerAssignments: Map<String, Int> = emptyMap(),
        winnerBankedJokers: Int = 0
    ): RoundResult {
        val base    = mutableMapOf<String, Int>()
        val banks   = mutableMapOf<String, Int>()
        val bonuses = mutableMapOf<String, Int>()

        // ── 1. Winner reward ─────────────────────────────────────────────────
        base[winnerId] = -10

        // ── 2. Losers pay for remaining hand cards ───────────────────────────
        for (p in state.players) {
            if (p.id == winnerId) continue
            val handPenalty = p.hand.sumOf { it.pointValue(state.mode) }
            base[p.id] = handPenalty
        }

        // ── 3. Tafdhil-specific ───────────────────────────────────────────────
        if (state.mode == GameMode.TAFDHIL) {
            // 3a. Jokers assigned to opponents by winner → +50 each (GDD §6.2)
            for ((playerId, count) in tafdhilJokerAssignments) {
                base[playerId] = (base[playerId] ?: 0) + count * 50
            }

            // 3b. Update winner's joker bank
            for (p in state.players) {
                val incoming = if (p.id == winnerId) winnerBankedJokers else 0
                val newBank  = p.jokerBankCount + incoming
                if (newBank >= 4) {
                    // Every 4 accumulated → −100 and reset (GDD §5.3)
                    bonuses[p.id] = -100
                    banks[p.id]   = newBank % 4  // carry over remainder
                } else {
                    banks[p.id] = newBank
                }
            }
        } else {
            // Normal mode — bank is irrelevant, keep as-is
            for (p in state.players) banks[p.id] = p.jokerBankCount
        }

        return RoundResult(base, bonuses, banks)
    }

    // ─── Apply deltas ─────────────────────────────────────────────────────────

    fun applyRoundResult(players: List<Player>, result: RoundResult): List<Player> =
        players.map { p ->
            p.copy(
                score          = p.score + result.totalDelta(p.id),
                jokerBankCount = result.updatedBanks[p.id] ?: p.jokerBankCount
            )
        }

    // ─── Elimination check ────────────────────────────────────────────────────

    /** Players whose score has reached or exceeded [scoreLimit] and have not bought a second life */
    fun eliminatedPlayers(players: List<Player>, scoreLimit: Int): List<Player> =
        players.filter { it.score >= scoreLimit && !it.hasSecondLife && !it.isEliminated }

    /** True when the game should end (2-player: 1 eliminated; 4-player: 2 eliminated, unless buy-in) */
    fun isGameOver(players: List<Player>, scoreLimit: Int): Boolean {
        val eliminated = eliminatedPlayers(players, scoreLimit)
        return when (players.size) {
            2    -> eliminated.isNotEmpty()
            4    -> eliminated.size >= 2
            else -> eliminated.isNotEmpty()
        }
    }

    // ─── Second-life buy-in (GDD §8) ─────────────────────────────────────────

    /**
     * Applies a second-life purchase for [playerId]:
     *  • Raises their effective score limit by [extra] points
     *  • Clears all Joker banks that haven't yet reached 4 (GDD §8, buy-in trigger)
     */
    fun applySecondLife(
        players: List<Player>,
        playerId: String,
        currentLimit: Int,
        extra: Int = 50
    ): List<Player> =
        players.map { p ->
            if (p.id == playerId) {
                p.copy(
                    hasSecondLife      = true,
                    secondLifeThreshold = currentLimit + extra
                )
            } else {
                // Clear banks that haven't reached 4 yet (GDD §8 buy-in trigger)
                if (p.jokerBankCount < 4) p.copy(jokerBankCount = 0) else p
            }
        }
}
