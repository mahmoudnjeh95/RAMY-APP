package com.rami

import androidx.compose.runtime.*
import com.rami.engine.GameEngine
import com.rami.model.GameMode
import com.rami.navigation.Screen
import com.rami.screens.*

/**
 * Root composable.
 * Holds the navigation stack and a single [GameEngine] instance
 * that persists across round transitions.
 */
@Composable
fun App() {
    var screen by remember { mutableStateOf<Screen>(Screen.Home) }
    // Engine is remembered so state survives recomposition
    val engine  = remember { GameEngine() }
    val state   by engine.state.collectAsState()

    when (val s = screen) {
        is Screen.Home -> {
            HomeScreen(onNavigate = { screen = it })
        }

        is Screen.Lobby -> {
            LobbyScreen(
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
        }

        is Screen.Game -> {
            GameScreen(
                engine     = engine,
                onRoundEnd = { screen = Screen.Score },
                onGameOver = { screen = Screen.Score }
            )
        }

        is Screen.Score -> {
            ScoreScreen(
                state        = state,
                onNextRound  = {
                    engine.startNextRound()
                    screen = Screen.Game
                },
                onSecondLife = { playerId ->
                    engine.buySecondLife(playerId)
                    screen = Screen.Game
                },
                onMainMenu   = { screen = Screen.Home }
            )
        }
    }
}
