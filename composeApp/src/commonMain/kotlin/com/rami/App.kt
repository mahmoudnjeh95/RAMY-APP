package com.rami

import androidx.compose.runtime.*
import com.rami.engine.GameEngine
import com.rami.model.CurrencyBalance
import com.rami.model.VipTier
import com.rami.navigation.Screen
import com.rami.online.model.OnlinePlayer
import com.rami.online.service.*
import com.rami.screens.*

@Composable
fun App(
    authService:       AuthService,
    roomService:       RoomService,
    leagueService:     LeagueService,
    friendService:     FriendService,
    statsService:      StatsService,
    tournamentService: TournamentService
) {
    var screen by remember { mutableStateOf<Screen>(Screen.Splash) }
    val engine  = remember { GameEngine() }
    val state   by engine.state.collectAsState()

    var signedInPlayer by remember { mutableStateOf<OnlinePlayer?>(null) }

    // Track daily-reward state (persisted in production via StatsService/Firestore)
    var dailyCurrentDay  by remember { mutableStateOf(1) }
    var dailyLastClaimed by remember { mutableStateOf(0) }

    LaunchedEffect(Unit) {
        authService.currentPlayer.collect { signedInPlayer = it }
    }

    when (val s = screen) {

        is Screen.Splash -> SplashScreen(
            onFinished = { screen = Screen.Home }
        )

        is Screen.Home -> HomeScreen(
            onNavigate = { screen = it }
        )

        is Screen.Auth -> AuthScreen(
            authService = authService,
            onSuccess   = { screen = Screen.OnlineMenu }
        )

        is Screen.OnlineMenu -> {
            val player = signedInPlayer
            if (player == null) { screen = Screen.Auth } else {
                OnlineMenuScreen(
                    player     = player,
                    onNavigate = { screen = it },
                    onBack     = { screen = Screen.Home }
                )
            }
        }

        is Screen.PrivateTable -> PrivateTableScreen(
            roomService  = roomService,
            localUid     = authService.uid ?: "",
            onRoomReady  = { roomId -> screen = Screen.OnlineGame(roomId) },
            onBack       = { screen = Screen.OnlineMenu }
        )

        is Screen.Matchmaking -> MatchmakingScreen(
            mode         = s.mode,
            roomService  = roomService,
            onMatchFound = { roomId -> screen = Screen.OnlineGame(roomId) },
            onCancel     = { screen = Screen.OnlineMenu }
        )

        is Screen.OnlineGame -> GameScreen(
            engine     = engine,
            onRoundEnd = { screen = Screen.Score },
            onGameOver = { screen = Screen.Score }
        )

        is Screen.League -> LeagueScreen(
            leagueService = leagueService,
            localUid      = authService.uid ?: "",
            onBack        = { screen = Screen.OnlineMenu }
        )

        is Screen.Profile -> {
            val player = signedInPlayer
            if (player != null) {
                ProfileScreen(
                    player       = player,
                    statsService = statsService,
                    onBack       = { screen = Screen.OnlineMenu }
                )
            } else {
                screen = Screen.Auth
            }
        }

        is Screen.Friends -> FriendsScreen(
            friendService = friendService,
            localUid      = authService.uid ?: "",
            onBack        = { screen = Screen.OnlineMenu }
        )

        is Screen.Tournaments -> TournamentScreen(
            tournamentService = tournamentService,
            localUid          = authService.uid ?: "",
            onBack            = { screen = Screen.OnlineMenu }
        )

        is Screen.Shop -> ShopScreen(
            balance    = CurrencyBalance(),
            vipTier    = VipTier.NONE,
            onBuyCoins = { /* TODO: in-app purchase */ },
            onBuyGems  = { /* TODO: in-app purchase */ },
            onBuyVip   = { /* TODO: in-app purchase */ },
            onBuyItem  = { /* TODO: deduct balance */ },
            onWatchAd  = { /* TODO: show rewarded ad */ },
            onBack     = { screen = Screen.OnlineMenu }
        )

        is Screen.DailyReward -> DailyRewardScreen(
            currentDay   = dailyCurrentDay,
            lastClaimDay = dailyLastClaimed,
            onClaim      = {
                dailyLastClaimed = dailyCurrentDay
                if (dailyCurrentDay < 7) dailyCurrentDay++ else dailyCurrentDay = 1
                screen = Screen.OnlineMenu
            },
            onBack = { screen = Screen.OnlineMenu }
        )

        is Screen.Privacy -> PrivacyPolicyScreen(
            onBack = { screen = Screen.Home }
        )

        is Screen.Terms -> TermsScreen(
            onBack = { screen = Screen.Home }
        )

        is Screen.Lobby -> LobbyScreen(
            mode        = s.mode,
            onStartGame = { names, limit, aiIdx, difficulty ->
                engine.startGame(
                    playerNames  = names,
                    mode         = s.mode,
                    scoreLimit   = limit,
                    aiIndices    = aiIdx,
                    aiDifficulty = difficulty
                )
                screen = Screen.Game
            },
            onBack = { screen = Screen.Home }
        )

        is Screen.Game -> GameScreen(
            engine     = engine,
            onRoundEnd = { screen = Screen.Score },
            onGameOver = { screen = Screen.Score }
        )

        is Screen.Score -> ScoreScreen(
            state        = state,
            onNextRound  = { engine.startNextRound(); screen = Screen.Game },
            onSecondLife = { playerId -> engine.buySecondLife(playerId); screen = Screen.Game },
            onMainMenu   = { screen = Screen.Home }
        )

        is Screen.Rules -> RulesScreen(onBack = { screen = Screen.Home })
    }
}
