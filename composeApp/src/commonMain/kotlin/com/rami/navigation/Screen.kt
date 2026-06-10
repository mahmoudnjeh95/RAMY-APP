package com.rami.navigation

import com.rami.model.GameMode

sealed class Screen {
    data object Home         : Screen()
    data object Auth         : Screen()
    data object OnlineMenu   : Screen()
    data object PrivateTable : Screen()
    data object League       : Screen()
    data object Profile      : Screen()
    data object Friends      : Screen()
    data object Tournaments  : Screen()
    data class  Lobby(val mode: GameMode)        : Screen()
    data class  Matchmaking(val mode: GameMode)  : Screen()
    data class  OnlineGame(val roomId: String)   : Screen()
    data object Game  : Screen()
    data object Score : Screen()
}
