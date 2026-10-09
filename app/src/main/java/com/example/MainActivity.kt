package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.example.game.audio.SoundManager
import com.example.game.data.LeaderboardRepository
import com.example.game.engine.GameEngine
import com.example.game.ui.GameScreen
import com.example.game.ui.LeaderboardDialog
import com.example.game.ui.MainMenuScreen
import com.example.ui.theme.MyApplicationTheme

enum class ScreenState {
    MENU,
    PLAYING
}

class MainActivity : ComponentActivity() {

    private lateinit var soundManager: SoundManager
    private lateinit var repository: LeaderboardRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        soundManager = SoundManager(this)
        repository = LeaderboardRepository(this)

        setContent {
            MyApplicationTheme {
                var currentScreen by remember { mutableStateOf(ScreenState.MENU) }
                var showMenuLeaderboard by remember { mutableStateOf(false) }

                val gameEngine = remember {
                    GameEngine(
                        soundManager = soundManager,
                        onGameOver = { _, _, _, _ -> }
                    )
                }

                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = Color(0xFF0F1E1B)
                ) {
                    when (currentScreen) {
                        ScreenState.MENU -> {
                            MainMenuScreen(
                                repository = repository,
                                onStartGame = {
                                    gameEngine.startNewRun()
                                    currentScreen = ScreenState.PLAYING
                                },
                                onOpenLeaderboard = {
                                    showMenuLeaderboard = true
                                }
                            )

                            if (showMenuLeaderboard) {
                                LeaderboardDialog(
                                    repository = repository,
                                    onDismiss = { showMenuLeaderboard = false }
                                )
                            }
                        }

                        ScreenState.PLAYING -> {
                            BackHandler {
                                gameEngine.pauseGame()
                                currentScreen = ScreenState.MENU
                            }

                            GameScreen(
                                engine = gameEngine,
                                repository = repository,
                                onExitToMenu = {
                                    currentScreen = ScreenState.MENU
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        soundManager.release()
    }
}
