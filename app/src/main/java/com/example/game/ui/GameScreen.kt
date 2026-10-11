package com.example.game.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.game.data.LeaderboardRepository
import com.example.game.engine.GameEngine
import com.example.game.engine.GameStatus

@Composable
fun GameScreen(
    engine: GameEngine,
    repository: LeaderboardRepository,
    onExitToMenu: () -> Unit = {},
    onExitApplication: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var frameTick by remember { mutableLongStateOf(0L) }
    var aimTarget by remember { mutableStateOf<Offset?>(null) }
    var isTouching by remember { mutableStateOf(false) }

    // Dialog flags
    var showShopDialog by remember { mutableStateOf(false) }
    var showPauseDialog by remember { mutableStateOf(false) }
    var showLeaderboardDialog by remember { mutableStateOf(false) }

    // 60 FPS Game Loop with withFrameNanos
    var lastFrameTimeNanos by remember { mutableLongStateOf(0L) }
    LaunchedEffect(Unit) {
        while (true) {
            withFrameNanos { frameTimeNanos ->
                if (lastFrameTimeNanos != 0L) {
                    val dt = ((frameTimeNanos - lastFrameTimeNanos) / 1_000_000_000f).coerceIn(0.001f, 0.05f)
                    engine.update(dt)

                    // Continuous fire if touching/holding down
                    if (isTouching && aimTarget != null && engine.status == GameStatus.PLAYING) {
                        engine.onTouchShoot(aimTarget!!.x, aimTarget!!.y)
                    }
                }
                lastFrameTimeNanos = frameTimeNanos
                frameTick = frameTimeNanos
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .testTag("game_screen_root")
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = { offset ->
                        val isInsideWeaponHUD = offset.x < engine.houseWidth * 0.65f && offset.y < engine.screenHeight * 0.50f
                        if (!isInsideWeaponHUD) {
                            isTouching = true
                            aimTarget = offset
                            engine.onTouchShoot(offset.x, offset.y)
                        }
                        tryAwaitRelease()
                        isTouching = false
                    }
                )
            }
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = { offset ->
                        val isInsideWeaponHUD = offset.x < engine.houseWidth * 0.65f && offset.y < engine.screenHeight * 0.50f
                        if (!isInsideWeaponHUD) {
                            isTouching = true
                            aimTarget = offset
                            engine.onTouchShoot(offset.x, offset.y)
                        }
                    },
                    onDragEnd = {
                        isTouching = false
                    },
                    onDragCancel = {
                        isTouching = false
                    },
                    onDrag = { change, _ ->
                        val isInsideWeaponHUD = change.position.x < engine.houseWidth * 0.65f && change.position.y < engine.screenHeight * 0.50f
                        if (!isInsideWeaponHUD) {
                            aimTarget = change.position
                            engine.onTouchShoot(change.position.x, change.position.y)
                        }
                    }
                )
            }
    ) {
        // Compose Canvas Game Renderer
        Canvas(modifier = Modifier.fillMaxSize().testTag("game_canvas")) {
            @Suppress("UNUSED_VARIABLE")
            val tick = frameTick

            GameRenderer.render(
                drawScope = this,
                engine = engine,
                aimTarget = aimTarget
            )
        }

        // Red Screen Damage Flash when base takes hit
        if (engine.houseDamageFlash > 0f) {
            val flashAlpha = (engine.houseDamageFlash * 2f).coerceIn(0f, 0.7f)
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .border(8.dp, Color(0xFFFF1744).copy(alpha = flashAlpha))
                    .background(Color(0xFFB71C1C).copy(alpha = flashAlpha * 0.25f))
            )
        }

        // HUD Overlay (Reads Compose State directly and updates in real time)
        GameHUD(
            engine = engine,
            onPauseClick = {
                engine.pauseGame()
                showPauseDialog = true
            },
            onShopClick = {
                engine.openShop()
                showShopDialog = true
            }
        )

        // Dialogs
        if (showShopDialog) {
            ShopDialog(
                engine = engine,
                onDismiss = {
                    showShopDialog = false
                }
            )
        }

        if (showPauseDialog) {
            PauseDialog(
                onResume = {
                    showPauseDialog = false
                    engine.resumeGame()
                },
                onOpenShop = {
                    showPauseDialog = false
                    engine.openShop()
                    showShopDialog = true
                },
                onQuickRestart = {
                    showPauseDialog = false
                    engine.startNewRun()
                },
                onOpenLeaderboard = {
                    showPauseDialog = false
                    showLeaderboardDialog = true
                }
            )
        }

        if (showLeaderboardDialog) {
            LeaderboardDialog(
                repository = repository,
                onDismiss = {
                    showLeaderboardDialog = false
                    if (engine.status == GameStatus.PAUSED) {
                        engine.resumeGame()
                    }
                }
            )
        }

        if (engine.status == GameStatus.GAME_OVER) {
            val survivedSecs = engine.survivedSeconds.toInt()
            val isNewRecord = remember(engine.status) {
                repository.saveRunScore(
                    survivedSeconds = survivedSecs,
                    waveReached = engine.wave,
                    zombiesKilled = engine.zombiesKilled,
                    coinsEarned = engine.coins
                )
            }

            GameOverDialog(
                survivedSeconds = survivedSecs,
                waveReached = engine.wave,
                zombiesKilled = engine.zombiesKilled,
                coinsEarned = engine.coins,
                isNewRecord = isNewRecord,
                onQuickRestart = {
                    engine.startNewRun()
                },
                onOpenLeaderboard = {
                    showLeaderboardDialog = true
                },
                onExitToMenu = {
                    onExitToMenu()
                },
                onExitApplication = {
                    onExitApplication()
                }
            )
        }
    }
}
