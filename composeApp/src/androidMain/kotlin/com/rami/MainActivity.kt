package com.rami

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.rami.online.service.FirebaseAuthService
import com.rami.online.service.FirebaseFriendService
import com.rami.online.service.FirebaseLeagueService
import com.rami.online.service.FirebaseRoomService
import com.rami.online.service.FirebaseStatsService
import com.rami.online.service.FirebaseTournamentService
import com.rami.ui.theme.RamiColors

class MainActivity : ComponentActivity() {

    private val authService       by lazy { FirebaseAuthService() }
    private val roomService       by lazy { FirebaseRoomService() }
    private val leagueService     by lazy { FirebaseLeagueService() }
    private val friendService     by lazy { FirebaseFriendService() }
    private val statsService      by lazy { FirebaseStatsService() }
    private val tournamentService by lazy { FirebaseTournamentService() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            Surface(
                modifier = Modifier.fillMaxSize(),
                color    = RamiColors.DarkGreen
            ) {
                App(
                    authService       = authService,
                    roomService       = roomService,
                    leagueService     = leagueService,
                    friendService     = friendService,
                    statsService      = statsService,
                    tournamentService = tournamentService
                )
            }
        }
    }
}
