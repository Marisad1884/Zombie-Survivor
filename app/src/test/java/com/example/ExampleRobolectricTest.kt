package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.game.data.LeaderboardRepository
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("Swamp Zombie", appName)
  }

  @Test
  fun `test leaderboard repository saves and loads top scores`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val repo = LeaderboardRepository(context)

    repo.savePlayerName("TestHunter")
    assertEquals("TestHunter", repo.getPlayerName())

    // Save run score
    val isTop = repo.saveRunScore(
      survivedSeconds = 180,
      waveReached = 4,
      zombiesKilled = 25,
      coinsEarned = 350
    )
    assertTrue(isTop)

    val topScores = repo.getLocalTopScores()
    assertTrue(topScores.isNotEmpty())
    assertEquals(180, topScores.first().survivedSeconds)
    assertEquals("TestHunter", topScores.first().playerName)
  }
}
