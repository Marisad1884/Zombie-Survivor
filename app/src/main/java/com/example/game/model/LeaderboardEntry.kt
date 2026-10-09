package com.example.game.model

data class LeaderboardEntry(
    val id: String,
    val playerName: String,
    val survivedSeconds: Int,
    val waveReached: Int,
    val zombiesKilled: Int,
    val coinsEarned: Int,
    val timestamp: Long,
    val isLocalPlayer: Boolean = false
) {
    val formattedTime: String
        get() {
            val minutes = survivedSeconds / 60
            val seconds = survivedSeconds % 60
            return "%02d:%02d".format(minutes, seconds)
        }
}
