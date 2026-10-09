package com.example.game.data

import android.content.Context
import android.content.SharedPreferences
import com.example.game.model.LeaderboardEntry
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

class LeaderboardRepository(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("swamp_defense_prefs", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_TOP_RECORDS = "key_top_records"
        private const val KEY_PLAYER_NAME = "key_player_name"
        private const val DEFAULT_PLAYER_NAME = "Superviviente"
    }

    fun getPlayerName(): String {
        return prefs.getString(KEY_PLAYER_NAME, DEFAULT_PLAYER_NAME) ?: DEFAULT_PLAYER_NAME
    }

    fun savePlayerName(name: String) {
        val cleanName = name.trim().ifEmpty { DEFAULT_PLAYER_NAME }
        prefs.edit().putString(KEY_PLAYER_NAME, cleanName).apply()
    }

    fun getLocalTopScores(): List<LeaderboardEntry> {
        val raw = prefs.getString(KEY_TOP_RECORDS, null) ?: return getInitialStarterRecords()
        return try {
            val list = mutableListOf<LeaderboardEntry>()
            val array = JSONArray(raw)
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                list.add(
                    LeaderboardEntry(
                        id = obj.optString("id", UUID.randomUUID().toString()),
                        playerName = obj.optString("playerName", DEFAULT_PLAYER_NAME),
                        survivedSeconds = obj.optInt("survivedSeconds", 0),
                        waveReached = obj.optInt("waveReached", 1),
                        zombiesKilled = obj.optInt("zombiesKilled", 0),
                        coinsEarned = obj.optInt("coinsEarned", 0),
                        timestamp = obj.optLong("timestamp", System.currentTimeMillis()),
                        isLocalPlayer = true
                    )
                )
            }
            list.sortedByDescending { it.survivedSeconds }.take(5)
        } catch (e: Exception) {
            getInitialStarterRecords()
        }
    }

    fun saveRunScore(
        survivedSeconds: Int,
        waveReached: Int,
        zombiesKilled: Int,
        coinsEarned: Int
    ): Boolean {
        val currentTop = getLocalTopScores().toMutableList()
        val entry = LeaderboardEntry(
            id = UUID.randomUUID().toString(),
            playerName = getPlayerName(),
            survivedSeconds = survivedSeconds,
            waveReached = waveReached,
            zombiesKilled = zombiesKilled,
            coinsEarned = coinsEarned,
            timestamp = System.currentTimeMillis(),
            isLocalPlayer = true
        )

        currentTop.add(entry)
        val sorted = currentTop.sortedByDescending { it.survivedSeconds }.take(5)

        val array = JSONArray()
        for (item in sorted) {
            val obj = JSONObject().apply {
                put("id", item.id)
                put("playerName", item.playerName)
                put("survivedSeconds", item.survivedSeconds)
                put("waveReached", item.waveReached)
                put("zombiesKilled", item.zombiesKilled)
                put("coinsEarned", item.coinsEarned)
                put("timestamp", item.timestamp)
            }
            array.put(obj)
        }
        prefs.edit().putString(KEY_TOP_RECORDS, array.toString()).apply()

        // Return true if this entry made it into the top 5
        return sorted.any { it.id == entry.id }
    }

    fun getBestSurvivalTimeSeconds(): Int {
        val top = getLocalTopScores()
        return top.firstOrNull()?.survivedSeconds ?: 0
    }

    /**
     * Tabla de Clasificación Global que combina leyendas de la comunidad
     * con la mejor marca del jugador para comparar posiciones mundiales.
     */
    fun getGlobalLeaderboard(): List<LeaderboardEntry> {
        val playerBest = getLocalTopScores().firstOrNull()
        val communityLegends = listOf(
            LeaderboardEntry("g1", "Cazador_Gator99", 542, 14, 184, 1620, 1698200000000L),
            LeaderboardEntry("g2", "SwampMarshal_TX", 498, 12, 162, 1440, 1698300000000L),
            LeaderboardEntry("g3", "VooDooSniper", 435, 11, 145, 1280, 1698400000000L),
            LeaderboardEntry("g4", "RedneckRambo", 380, 10, 128, 1090, 1698500000000L),
            LeaderboardEntry("g5", "BayouBane", 312, 8, 104, 880, 1698600000000L),
            LeaderboardEntry("g6", "GrisGris_Doc", 265, 7, 86, 710, 1698700000000L),
            LeaderboardEntry("g7", "DeadShotLouie", 210, 6, 68, 560, 1698800000000L),
            LeaderboardEntry("g8", "SwampyJoe", 175, 5, 52, 430, 1698900000000L),
            LeaderboardEntry("g9", "MudCrawler_Hunter", 130, 4, 38, 310, 1699000000000L),
            LeaderboardEntry("g10", "RustyPorchBoy", 95, 3, 24, 210, 1699100000000L)
        )

        val combined = communityLegends.toMutableList()
        if (playerBest != null && playerBest.survivedSeconds > 0) {
            combined.add(playerBest.copy(isLocalPlayer = true))
        }

        return combined.sortedByDescending { it.survivedSeconds }
    }

    private fun getInitialStarterRecords(): List<LeaderboardEntry> {
        return emptyList()
    }
}
