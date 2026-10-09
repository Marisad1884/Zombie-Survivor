package com.example.game.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.R
import com.example.game.data.LeaderboardRepository

@Composable
fun MainMenuScreen(
    repository: LeaderboardRepository,
    onStartGame: () -> Unit,
    onOpenLeaderboard: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showHowToPlay by remember { mutableStateOf(false) }
    val bestSeconds = remember { repository.getBestSurvivalTimeSeconds() }
    val formattedBest = remember(bestSeconds) {
        val m = bestSeconds / 60
        val s = bestSeconds % 60
        "%02d:%02d".format(m, s)
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(
                        Color(0xFF07120E),
                        Color(0xFF0D1E17),
                        Color(0xFF142B20)
                    )
                )
            )
            .padding(16.dp)
    ) {
        Column(
            modifier = Modifier
                .align(Alignment.Center)
                .fillMaxWidth(0.92f),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Header with Soldier Graphic and Game Title Badge
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Hero soldier avatar from drawable
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    border = androidx.compose.foundation.BorderStroke(2.dp, Color(0xFF76FF03)),
                    shadowElevation = 8.dp,
                    modifier = Modifier.size(80.dp)
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.hero_soldier_1791571690568),
                        contentDescription = "Héroe con máscara y casco USA",
                        modifier = Modifier.fillMaxSize()
                    )
                }

                // Game Title Badge
                Surface(
                    color = Color(0xFF1B3325),
                    shape = RoundedCornerShape(18.dp),
                    border = androidx.compose.foundation.BorderStroke(2.dp, Color(0xFF4CAF50)),
                    shadowElevation = 8.dp
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 22.dp, vertical = 12.dp),
                        horizontalAlignment = Alignment.Start
                    ) {
                        Text(
                            text = "SWAMP ZOMBIE",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Black,
                            color = Color(0xFFFFD54F),
                            letterSpacing = 1.5.sp
                        )
                        Text(
                            text = "DEFENSA MARATÓN 1H+",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color(0xFFA5D6A7),
                            letterSpacing = 1.2.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Best record banner
            if (bestSeconds > 0) {
                Surface(
                    color = Color(0x99182D22),
                    shape = RoundedCornerShape(12.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFFD700))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.EmojiEvents,
                            contentDescription = "Récord",
                            tint = Color(0xFFFFD700),
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = "Tu Mejor Marca: $formattedBest",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            } else {
                Spacer(modifier = Modifier.height(8.dp))
            }

            // Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Play Button
                Button(
                    onClick = onStartGame,
                    modifier = Modifier
                        .weight(1.4f)
                        .height(54.dp)
                        .testTag("menu_start_button"),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32)),
                    shape = RoundedCornerShape(16.dp),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = "Jugar",
                        modifier = Modifier.size(26.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "¡JUGAR RUN!",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Black,
                        color = Color.White
                    )
                }

                // Leaderboard Button
                OutlinedButton(
                    onClick = onOpenLeaderboard,
                    modifier = Modifier
                        .weight(1f)
                        .height(54.dp)
                        .testTag("menu_leaderboard_button"),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFFFD54F)),
                    border = androidx.compose.foundation.BorderStroke(2.dp, Color(0xFFFFB300))
                ) {
                    Icon(
                        imageVector = Icons.Default.Leaderboard,
                        contentDescription = "Récords",
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "RÉCORDS",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                // How to play button
                IconButton(
                    onClick = { showHowToPlay = true },
                    modifier = Modifier
                        .size(54.dp)
                        .background(Color(0xFF263238), RoundedCornerShape(16.dp))
                        .border(1.5.dp, Color(0xFF546E7A), RoundedCornerShape(16.dp))
                        .testTag("menu_help_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.HelpOutline,
                        contentDescription = "Cómo jugar",
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }

        // How to play dialog
        if (showHowToPlay) {
            Dialog(onDismissRequest = { showHowToPlay = false }) {
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = Color(0xFF14241B),
                    border = androidx.compose.foundation.BorderStroke(2.dp, Color(0xFF2E7D32)),
                    modifier = Modifier.fillMaxWidth(0.92f)
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = "GUÍA DE COMBATE Y SUPERVIVENCIA",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Black,
                            color = Color(0xFFFFD54F)
                        )

                        Text(
                            text = "• Recarga Automática: Tu personaje recarga sus armas automáticamente cuando dejas de disparar.",
                            fontSize = 12.sp,
                            color = Color.White
                        )
                        Text(
                            text = "• 8 Tipos de Zombis: Caminantes, Veloces, Perros Rabiosos, Zombis Tóxicos con ácido, Soldados Blindados (resistentes a balas), Caimanes del Pantano, Brutos y Megatitanes Boss cada 5 oleadas.",
                            fontSize = 12.sp,
                            color = Color.White
                        )
                        Text(
                            text = "• Mejoras de Nivel 10: Sube de nivel tus armas hasta nivel 10, repara la barricada e instala torretas centinela automáticas en el techo.",
                            fontSize = 12.sp,
                            color = Color.White
                        )
                        Text(
                            text = "• Partidas Largas (1 Hora+): La curva de oleadas continuas te permite resistir carreras maratónicas para alcanzar récords históricos.",
                            fontSize = 12.sp,
                            color = Color(0xFFA5D6A7)
                        )

                        Button(
                            onClick = { showHowToPlay = false },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("¡A LA BATALLA!")
                        }
                    }
                }
            }
        }
    }
}
