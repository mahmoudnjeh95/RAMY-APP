package com.rami.online.service

import com.rami.model.GameMode
import com.rami.online.model.Tournament
import kotlinx.coroutines.flow.Flow

interface TournamentService {
    fun observeOpenTournaments(): Flow<List<Tournament>>
    fun observeTournament(id: String): Flow<Tournament?>

    suspend fun register(tournamentId: String, uid: String): Result<Unit>
    suspend fun unregister(tournamentId: String, uid: String): Result<Unit>
    suspend fun reportMatchResult(tournamentId: String, matchId: String, winnerUid: String): Result<Unit>
}
