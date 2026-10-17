package com.example.game.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.testTag
import com.example.game.data.LeaderboardRepository
import com.example.game.engine.GameEngine
import com.example.game.engine.GameStatus
import com.example.game.model.Balance
import kotlinx.coroutines.delay

/** How long the big red "TE HAN ATRAPADO" stays alone on screen before the results show up. */
private const val CAUGHT_SCREEN_MILLIS = 2200L

@Composable
fun GameScreen(
    engine: GameEngine,
    repository: LeaderboardRepository,
    onExitToMenu: () -> Unit = {},
    onExitApplication: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var frameTick by remember { mutableLongStateOf(0L) }
    // Finger position in world units (null when nothing is touching the screen)
    var aimTarget by remember { mutableStateOf<Offset?>(null) }
    var isTouching by remember { mutableStateOf(false) }
    var screenHeightPx by remember { mutableFloatStateOf(1f) }

    var showPauseDialog by remember { mutableStateOf(false) }
    var showLeaderboardDialog by remember { mutableStateOf(false) }
    var showGameOverDialog by remember { mutableStateOf(false) }
    var isNewRecord by remember { mutableStateOf(false) }

    // Screen pixels -> world units. The HUD bar at the top never shoots.
    fun toWorld(p: Offset): Offset {
        val s = screenHeightPx / Balance.WORLD_H
        return Offset(p.x / s, p.y / s)
    }

    fun isOnHudBar(p: Offset): Boolean = p.y < screenHeightPx * Balance.BAR_FRACTION

    // 60 FPS game loop
    var lastFrameTimeNanos by remember { mutableLongStateOf(0L) }
    LaunchedEffect(Unit) {
        while (true) {
            withFrameNanos { frameTimeNanos ->
                if (lastFrameTimeNanos != 0L) {
                    val dt = ((frameTimeNanos - lastFrameTimeNanos) / 1_000_000_000f).coerceIn(0.001f, 0.05f)
                    engine.update(dt)

                    // Holding a finger down keeps firing as fast as the weapon allows
                    val target = aimTarget
                    if (isTouching && target != null && engine.status == GameStatus.PLAYING) {
                        engine.onTouchShoot(target.x, target.y)
                    }
                }
                lastFrameTimeNanos = frameTimeNanos
                frameTick = frameTimeNanos
            }
        }
    }

    // The results appear a moment after she is caught
    LaunchedEffect(engine.status) {
        if (engine.status == GameStatus.GAME_OVER) {
            showGameOverDialog = false
            // saved right away, so leaving early never loses the score
            isNewRecord = repository.saveRunScore(
                survivedSeconds = engine.survivedSeconds.toInt(),
                waveReached = engine.wave,
                zombiesKilled = engine.zombiesKilled,
                coinsEarned = engine.score
            )
            delay(CAUGHT_SCREEN_MILLIS)
            showGameOverDialog = true
        } else {
            showGameOverDialog = false
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .testTag("game_screen_root")
            .onSizeChanged { screenHeightPx = it.height.toFloat().coerceAtLeast(1f) }
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = { offset ->
                        if (!isOnHudBar(offset)) {
                            isTouching = true
                            aimTarget = toWorld(offset)
                            engine.onTouchShoot(aimTarget!!.x, aimTarget!!.y)
                        }
                        tryAwaitRelease()
                        isTouching = false
                        aimTarget = null
                    }
                )
            }
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = { offset ->
                        if (!isOnHudBar(offset)) {
                            isTouching = true
                            aimTarget = toWorld(offset)
                            engine.onTouchShoot(aimTarget!!.x, aimTarget!!.y)
                        }
                    },
                    onDragEnd = {
                        isTouching = false
                        aimTarget = null
                    },
                    onDragCancel = {
                        isTouching = false
                        aimTarget = null
                    },
                    onDrag = { change, _ ->
                        if (!isOnHudBar(change.position)) {
                            aimTarget = toWorld(change.position)
                            engine.onTouchShoot(aimTarget!!.x, aimTarget!!.y)
                        }
                    }
                )
            }
    ) {
        Canvas(modifier = Modifier.fillMaxSize().testTag("game_canvas")) {
            @Suppress("UNUSED_VARIABLE")
            val tick = frameTick

            GameRenderer.render(
                drawScope = this,
                engine = engine,
                aimTarget = aimTarget
            )
        }

        GameHUD(
            engine = engine,
            onPauseClick = {
                engine.pauseGame()
                showPauseDialog = true
            }
        )

        if (showPauseDialog) {
            PauseDialog(
                onResume = {
                    showPauseDialog = false
                    engine.resumeGame()
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

        if (engine.status == GameStatus.GAME_OVER && showGameOverDialog) {
            GameOverDialog(
                survivedSeconds = engine.survivedSeconds.toInt(),
                waveReached = engine.wave,
                zombiesKilled = engine.zombiesKilled,
                score = engine.score,
                headshots = engine.headshots,
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
