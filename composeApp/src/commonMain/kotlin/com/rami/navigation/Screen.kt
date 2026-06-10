package com.rami.navigation

import com.rami.model.GameMode

sealed class Screen {
    data object Home  : Screen()
    data class  Lobby(val mode: GameMode) : Screen()
    data object Game  : Screen()
    data object Score : Screen()
}
