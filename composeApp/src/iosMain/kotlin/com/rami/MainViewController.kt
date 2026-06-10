package com.rami

import androidx.compose.ui.window.ComposeUIViewController
import com.rami.online.service.*

/**
 * Called from Swift/SwiftUI via the generated Kotlin/Native framework.
 * Firebase iOS services are injected here.
 */
fun MainViewController() = ComposeUIViewController {
    App(
        authService       = FirebaseAuthService(),
        roomService       = FirebaseRoomService(),
        leagueService     = FirebaseLeagueService(),
        friendService     = FirebaseFriendService(),
        statsService      = FirebaseStatsService(),
        tournamentService = FirebaseTournamentService()
    )
}
