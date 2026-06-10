package com.rami.online.service

import com.rami.online.model.Tournament
import com.rami.online.model.TournamentStatus
import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.auth.auth
import dev.gitlive.firebase.firestore.firestore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class FirebaseTournamentService : TournamentService {

    private val db   get() = Firebase.firestore
    private val auth get() = Firebase.auth

    private fun tournamentsRef() = db.collection("tournaments")

    override fun observeOpenTournaments(): Flow<List<Tournament>> =
        tournamentsRef()
            .where { "status" notEqualTo TournamentStatus.FINISHED.name }
            .snapshots
            .map { snap -> snap.documents.mapNotNull { it.data<Tournament?>() } }

    override fun observeTournament(id: String): Flow<Tournament?> =
        tournamentsRef().document(id).snapshots.map { it.data<Tournament?>() }

    override suspend fun register(tournamentId: String, uid: String): Result<Unit> = runCatching {
        val ref = tournamentsRef().document(tournamentId)
        val t = ref.get().data<Tournament>() ?: error("Tournament not found")
        if (uid in t.registeredPlayers) return@runCatching
        if (t.registeredPlayers.size >= t.maxPlayers) error("التسجيل ممتلئ  •  Tournament full")
        ref.update("registeredPlayers" to t.registeredPlayers + uid)
    }

    override suspend fun unregister(tournamentId: String, uid: String): Result<Unit> = runCatching {
        val ref = tournamentsRef().document(tournamentId)
        val t = ref.get().data<Tournament>() ?: error("Tournament not found")
        ref.update("registeredPlayers" to t.registeredPlayers - uid)
    }

    override suspend fun reportMatchResult(tournamentId: String, matchId: String, winnerUid: String): Result<Unit> =
        runCatching {
            val ref = tournamentsRef().document(tournamentId)
            val t = ref.get().data<Tournament>() ?: error("Tournament not found")
            val updatedBracket = t.bracket.map { match ->
                if (match.matchId == matchId) match.copy(winnerId = winnerUid) else match
            }
            val allDone = updatedBracket.all { it.winnerId != null }
            ref.update(
                "bracket" to updatedBracket,
                "status"  to if (allDone) TournamentStatus.FINISHED.name else t.status.name
            )
        }
}
