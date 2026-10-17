package com.example.game.ui

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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog

@Composable
fun PauseDialog(
    onResume: () -> Unit,
    onQuickRestart: () -> Unit,
    onOpenLeaderboard: () -> Unit,
    modifier: Modifier = Modifier
) {
    Dialog(onDismissRequest = onResume) {
        Surface(
            modifier = modifier
                .fillMaxWidth(0.75f)
                .clip(RoundedCornerShape(20.dp)),
            color = Color(0xFF14241B),
            border = androidx.compose.foundation.BorderStroke(2.dp, Color(0xFF2E7D32)),
            shadowElevation = 14.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.PauseCircle,
                    contentDescription = "Pausa",
                    tint = Color(0xFFFFD54F),
                    modifier = Modifier.size(46.dp)
                )

                Text(
                    text = "JUEGO EN PAUSA",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Black,
                    color = Color.White
                )

                Spacer(modifier = Modifier.height(4.dp))

                // Resume
                Button(
                    onClick = onResume,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(46.dp)
                        .testTag("pause_resume_button"),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(imageVector = Icons.Default.PlayArrow, contentDescription = "Reanudar")
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("REANUDAR PARTIDA", fontWeight = FontWeight.Bold)
                }

                // Leaderboard
                OutlinedButton(
                    onClick = onOpenLeaderboard,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(46.dp)
                        .testTag("pause_leaderboard_button"),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF81C784)),
                    border = androidx.compose.foundation.BorderStroke(1.5.dp, Color(0xFF4CAF50))
                ) {
                    Icon(imageVector = Icons.Default.Leaderboard, contentDescription = "Récords")
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("TABLA DE CLASIFICACIÓN", fontWeight = FontWeight.Bold)
                }

                // Quick Restart
                Button(
                    onClick = onQuickRestart,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(46.dp)
                        .testTag("pause_restart_button"),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFC62828)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(imageVector = Icons.Default.Replay, contentDescription = "Reiniciar")
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("REINICIAR PARTIDA", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
