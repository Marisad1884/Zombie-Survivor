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

  @Test
  fun `test game engine lower cabin porch and steep downward shooting`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val soundManager = com.example.game.audio.SoundManager(context)
    val engine = com.example.game.engine.GameEngine(soundManager) { _, _, _, _ -> }
    engine.screenWidth = 1200f
    engine.screenHeight = 700f

    // Verify porch is positioned lower down on screen (64% of screen height)
    assertEquals(700f * 0.64f, engine.porchY, 0.1f)

    // Verify touch shoot allows shooting downward under the house
    val muzzle = engine.playerMuzzleOffset
    // Touch below the house into the water
    engine.onTouchShoot(muzzle.x + 20f, engine.screenHeight * 0.90f)

    // A projectile should have been created firing downwards
    assertTrue(engine.projectiles.isNotEmpty())
    val proj = engine.projectiles.first()
    assertTrue("Bullet should have positive downward vy", proj.vy > 0)
  }

  @Test
  fun `test throw molotov at threat creates fire zone`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val soundManager = com.example.game.audio.SoundManager(context)
    val engine = com.example.game.engine.GameEngine(soundManager) { _, _, _, _ -> }
    engine.defenseUpgrades.molotovCount = 3
    engine.molotovCooldownRemaining = 0f

    engine.throwMolotovAtThreat()
    assertEquals(2, engine.defenseUpgrades.molotovCount)
    assertTrue("A fire zone should be created", engine.fireZones.isNotEmpty())
  }
}
