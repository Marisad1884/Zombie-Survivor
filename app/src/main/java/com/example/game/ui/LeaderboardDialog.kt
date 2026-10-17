package com.example.game.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.game.data.LeaderboardRepository
import com.example.game.model.LeaderboardEntry

@Composable
fun LeaderboardDialog(
    repository: LeaderboardRepository,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    var playerName by remember { mutableStateOf(repository.getPlayerName()) }
    var isEditingName by remember { mutableStateOf(false) }

    val localScores = remember(isEditingName) { repository.getLocalTopScores() }
    val globalScores = remember(isEditingName) { repository.getGlobalLeaderboard() }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.92f)
                .clip(RoundedCornerShape(20.dp)),
            color = Color(0xFF121E18),
            border = androidx.compose.foundation.BorderStroke(2.dp, Color(0xFF2E7D32)),
            shadowElevation = 14.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.EmojiEvents,
                            contentDescription = "Trofeo",
                            tint = Color(0xFFFFD700),
                            modifier = Modifier.size(28.dp)
                        )
                        Column {
                            Text(
                                text = "TABLA DE CLASIFICACIÓN",
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Black,
                                color = Color.White
                            )
                            Text(
                                text = "Supervivencia y Récords en el Pantano",
                                fontSize = 11.sp,
                                color = Color(0xFFA5D6A7)
                            )
                        }
                    }

                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Cerrar",
                            tint = Color.White
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Player Nickname Row
                Surface(
                    color = Color(0xFF1B2E24),
                    shape = RoundedCornerShape(10.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF2E5E45)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        if (isEditingName) {
                            OutlinedTextField(
                                value = playerName,
                                onValueChange = { playerName = it },
                                label = { Text("Tu Apodo de Superviviente") },
                                singleLine = true,
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("player_name_input"),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White,
                                    focusedBorderColor = Color(0xFFFFD54F),
                                    unfocusedBorderColor = Color(0xFF81C784)
                                )
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Button(
                                onClick = {
                                    repository.savePlayerName(playerName)
                                    isEditingName = false
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32))
                            ) {
                                Text("Guardar")
                            }
                        } else {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Person,
                                    contentDescription = "Jugador",
                                    tint = Color(0xFFFFD54F),
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Jugador: ",
                                    fontSize = 12.sp,
                                    color = Color(0xFFB0BEC5)
                                )
                                Text(
                                    text = playerName,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                            TextButton(onClick = { isEditingName = true }) {
                                Icon(
                                    imageVector = Icons.Default.Edit,
                                    contentDescription = "Editar",
                                    tint = Color(0xFFA5D6A7),
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Editar", fontSize = 11.sp, color = Color(0xFFA5D6A7))
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Tab Switcher
                TabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = Color(0xFF16271F),
                    contentColor = Color(0xFFFFD54F)
                ) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        text = {
                            Text(
                                "Tus 5 Mejores Marcas",
                                fontWeight = if (selectedTab == 0) FontWeight.Bold else FontWeight.Normal,
                                fontSize = 12.sp
                            )
                        }
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        text = {
                            Text(
                                "Clasificación Global",
                                fontWeight = if (selectedTab == 1) FontWeight.Bold else FontWeight.Normal,
                                fontSize = 12.sp
                            )
                        }
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Leaderboard List
                val displayList = if (selectedTab == 0) localScores else globalScores

                if (displayList.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Default.HourglassEmpty,
                                contentDescription = "Sin récords",
                                tint = Color(0xFF546E7A),
                                modifier = Modifier.size(40.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "¡Aún no hay récords registrados!",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Text(
                                text = "Sobrevive a las oleadas para guardar tu primera marca.",
                                fontSize = 11.sp,
                                color = Color(0xFF90A4AE)
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        itemsIndexed(displayList) { index, entry ->
                            LeaderboardRowItem(
                                rank = index + 1,
                                entry = entry,
                                isHighlight = entry.isLocalPlayer
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Bottom Close Button
                Button(
                    onClick = onDismiss,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp)
                        .testTag("leaderboard_close_button"),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("CERRAR Y VOLVER", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
            }
        }
    }
}

@Composable
private fun LeaderboardRowItem(
    rank: Int,
    entry: LeaderboardEntry,
    isHighlight: Boolean
) {
    val rankColor = when (rank) {
        1 -> Color(0xFFFFD700) // Gold
        2 -> Color(0xFFCFD8DC) // Silver
        3 -> Color(0xFFCD7F32) // Bronze
        else -> Color(0xFF81C784)
    }

    Surface(
        color = if (isHighlight) Color(0xFF254B35) else Color(0xFF182B21),
        shape = RoundedCornerShape(10.dp),
        border = androidx.compose.foundation.BorderStroke(
            width = if (isHighlight) 1.5.dp else 1.dp,
            color = if (isHighlight) Color(0xFFFFD54F) else Color(0xFF274333)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Rank + Name
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Surface(
                    color = rankColor.copy(alpha = 0.2f),
                    shape = RoundedCornerShape(6.dp),
                    modifier = Modifier.size(28.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = "#$rank",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Black,
                            color = rankColor
                        )
                    }
                }

                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = entry.playerName,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isHighlight) Color(0xFFFFD54F) else Color.White
                        )
                        if (isHighlight) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Surface(
                                color = Color(0xFF2E7D32),
                                shape = RoundedCornerShape(4.dp)
                            ) {
                                Text(
                                    text = "TÚ",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Black,
                                    color = Color.White,
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                )
                            }
                        }
                    }
                    Text(
                        text = "Oleada ${entry.waveReached} • ${entry.zombiesKilled} zombis",
                        fontSize = 11.sp,
                        color = Color(0xFF90A4AE)
                    )
                }
            }

            // Time & Coins
            Column(horizontalAlignment = Alignment.End) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Timer,
                        contentDescription = "Tiempo",
                        tint = Color(0xFF80DEEA),
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = entry.formattedTime,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Black,
                        color = Color.White
                    )
                }
                Text(
                    text = "${entry.coinsEarned} pts",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFFFD700)
                )
            }
        }
    }
}
