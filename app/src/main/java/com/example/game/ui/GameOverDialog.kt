package com.example.game.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog

@Composable
fun GameOverDialog(
    survivedSeconds: Int,
    waveReached: Int,
    zombiesKilled: Int,
    coinsEarned: Int,
    isNewRecord: Boolean,
    onQuickRestart: () -> Unit,
    onOpenLeaderboard: () -> Unit,
    onExitGame: () -> Unit,
    modifier: Modifier = Modifier
) {
    val minutes = survivedSeconds / 60
    val seconds = survivedSeconds % 60
    val formattedTime = "%02d:%02d".format(minutes, seconds)

    Dialog(onDismissRequest = {}) {
        Surface(
            modifier = modifier
                .fillMaxWidth(0.92f)
                .clip(RoundedCornerShape(22.dp)),
            color = Color(0xFF1A1212),
            border = androidx.compose.foundation.BorderStroke(2.dp, Color(0xFFB71C1C)),
            shadowElevation = 16.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header Icon
                Surface(
                    color = Color(0xFF2C1313),
                    shape = RoundedCornerShape(16.dp),
                    border = androidx.compose.foundation.BorderStroke(1.5.dp, Color(0xFFE53935)),
                    modifier = Modifier.size(54.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.Dangerous,
                            contentDescription = "Game Over",
                            tint = Color(0xFFFF5252),
                            modifier = Modifier.size(34.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = "¡LA CABAÑA HA CAÍDO!",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Black,
                    color = Color(0xFFFF5252),
                    textAlign = TextAlign.Center
                )

                Text(
                    text = "Los zombis del pantano han invadido tus defensas.",
                    fontSize = 12.sp,
                    color = Color(0xFFB0BEC5),
                    textAlign = TextAlign.Center
                )

                if (isNewRecord) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Surface(
                        color = Color(0xFF2E7D32),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = "★ ¡NUEVA MARCA EN EL TOP 5! ★",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Black,
                            color = Color(0xFFFFD54F),
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Stats Grid
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    StatCard(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Default.Timer,
                        label = "SUPERVIVENCIA",
                        value = formattedTime,
                        iconTint = Color(0xFF80DEEA)
                    )
                    StatCard(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Default.Warning,
                        label = "OLEADA FINAL",
                        value = "Oleada $waveReached",
                        iconTint = Color(0xFFFFB300)
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    StatCard(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Default.GppBad,
                        label = "ZOMBIS CAÍDOS",
                        value = "$zombiesKilled",
                        iconTint = Color(0xFFEF5350)
                    )
                    StatCard(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Default.MonetizationOn,
                        label = "MONEDAS TOTALES",
                        value = "$$coinsEarned",
                        iconTint = Color(0xFFFFD700)
                    )
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Menu Options: Reiniciar, Marcadores, Salir del Juego
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // 1. Reiniciar Run Button
                    Button(
                        onClick = onQuickRestart,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("quick_restart_button"),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Replay,
                            contentDescription = "Reiniciar",
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "REINICIAR PARTIDA",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Black,
                            color = Color.White
                        )
                    }

                    // 2. Marcadores & Salir Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Ver Marcadores
                        OutlinedButton(
                            onClick = onOpenLeaderboard,
                            modifier = Modifier
                                .weight(1f)
                                .height(44.dp)
                                .testTag("game_over_leaderboard_button"),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFFFD54F)),
                            border = androidx.compose.foundation.BorderStroke(1.5.dp, Color(0xFFFFB300))
                        ) {
                            Icon(
                                imageVector = Icons.Default.Leaderboard,
                                contentDescription = "Marcadores",
                                modifier = Modifier.size(17.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "MARCADORES",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        // Salir del Juego
                        OutlinedButton(
                            onClick = onExitGame,
                            modifier = Modifier
                                .weight(1f)
                                .height(44.dp)
                                .testTag("game_over_exit_button"),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFFF8A80)),
                            border = androidx.compose.foundation.BorderStroke(1.5.dp, Color(0xFFE53935))
                        ) {
                            Icon(
                                imageVector = Icons.Default.ExitToApp,
                                contentDescription = "Salir",
                                modifier = Modifier.size(17.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "SALIR AL MENÚ",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StatCard(
    modifier: Modifier = Modifier,
    icon: ImageVector,
    label: String,
    value: String,
    iconTint: Color
) {
    Surface(
        modifier = modifier,
        color = Color(0xFF221919),
        shape = RoundedCornerShape(12.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF3E2723))
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = iconTint,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = label,
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF90A4AE)
            )
            Text(
                text = value,
                fontSize = 14.sp,
                fontWeight = FontWeight.Black,
                color = Color.White
            )
        }
    }
}
