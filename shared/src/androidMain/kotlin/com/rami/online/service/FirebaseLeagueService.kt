package com.rami.online.service

import com.rami.online.model.LeagueEntry
import com.rami.online.model.LeagueSeason
import com.rami.online.model.LeagueTier
import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.firestore.firestore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class FirebaseLeagueService : LeagueService {

    private val db get() = Firebase.firestore

    private fun weekKey(): String {
        val now = System.currentTimeMillis()
        val dayMs = 86_400_000L
        val weekMs = 7 * dayMs
        val weekStart = (now / weekMs) * weekMs
        return weekStart.toString()
    }

    private fun leagueRef() = db.collection("league").document(weekKey()).collection("entries")

    override fun observeLeaderboard(limit: Int): Flow<List<LeagueEntry>> =
        leagueRef().orderBy("points", dev.gitlive.firebase.firestore.Direction.DESCENDING)
            .limit(limit.toLong())
            .snapshots
            .map { snap ->
                snap.documents.mapIndexed { idx, doc ->
                    doc.data<LeagueEntry>().copy(rank = idx + 1)
                }
            }

    override fun observeMyEntry(uid: String): Flow<LeagueEntry?> =
        leagueRef().document(uid).snapshots.map { it.data<LeagueEntry?>() }

    override suspend fun addPoints(uid: String, points: Int) = updatePoints(uid, points)

    override suspend fun deductPoints(uid: String, points: Int) = updatePoints(uid, -points)

    override suspend fun getCurrentSeason(): LeagueSeason? {
        val key = weekKey()
        val weekStart = key.toLong()
        return LeagueSeason(
            weekStart = weekStart,
            weekEnd   = weekStart + 7 * 86_400_000L,
            isActive  = true
        )
    }

    override suspend fun rolloverSeason() {
        // Typically invoked by a Cloud Function on Monday 00:00 UTC.
        // Client-side: no-op — Cloud Function handles archiving.
    }

    private suspend fun updatePoints(uid: String, delta: Int) {
        val ref = leagueRef().document(uid)
        val existing = ref.get().data<LeagueEntry?>()
        if (existing == null) {
            val entry = LeagueEntry(uid = uid, points = maxOf(0, delta),
                weekStart = weekKey().toLong())
            ref.set(entry)
        } else {
            val newPoints = maxOf(0, existing.points + delta)
            val tier = LeagueTier.entries.lastOrNull { newPoints >= it.minPoints } ?: LeagueTier.BRONZE
            ref.update("points" to newPoints, "tier" to tier.name,
                if (delta > 0) "wins" to existing.wins + 1 else "losses" to existing.losses + 1)
        }
    }
}
