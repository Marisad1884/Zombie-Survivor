package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.game.data.LeaderboardRepository
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
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

  private fun newEngine(): com.example.game.engine.GameEngine {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val soundManager = com.example.game.audio.SoundManager(context)
    val engine = com.example.game.engine.GameEngine(soundManager) { _, _, _, _ -> }
    engine.screenWidth = 1200f
    engine.screenHeight = 700f
    return engine
  }

  @Test
  fun `empty magazine does not reload by itself`() {
    val engine = newEngine()
    val weapon = engine.currentWeapon
    weapon.currentAmmo = 0
    val reserveBefore = weapon.reserveAmmo

    engine.onTouchShoot(900f, 400f)
    engine.update(0.5f)

    assertFalse("Reload must be manual now", weapon.isReloading)
    assertEquals(0, weapon.currentAmmo)
    assertEquals(reserveBefore, weapon.reserveAmmo)
    assertTrue(engine.projectiles.isEmpty())
  }

  @Test
  fun `manual reload moves rounds from reserve to magazine`() {
    val engine = newEngine()
    val weapon = engine.currentWeapon
    weapon.currentAmmo = 0
    val reserveBefore = weapon.reserveAmmo
    assertTrue(reserveBefore > 0)

    assertTrue(engine.manualReload())
    assertTrue(weapon.isReloading)
    repeat(100) { engine.update(0.05f) }

    assertFalse(weapon.isReloading)
    val moved = minOf(weapon.maxAmmo, reserveBefore)
    assertEquals(moved, weapon.currentAmmo)
    assertEquals(reserveBefore - moved, weapon.reserveAmmo)
  }

  @Test
  fun `cannot reload without reserve or with a full magazine`() {
    val engine = newEngine()
    val weapon = engine.currentWeapon
    assertFalse("Full magazine", engine.manualReload())

    weapon.currentAmmo = 0
    weapon.reserveAmmo = 0
    assertFalse("No reserve", engine.manualReload())
    assertFalse(weapon.isReloading)
  }

  @Test
  fun `ammo girl brings ammo and then has to cool down`() {
    val engine = newEngine()
    val weapon = engine.currentWeapon
    weapon.reserveAmmo = 0
    assertEquals(com.example.game.engine.RunnerState.IDLE, engine.runnerState)

    assertTrue(engine.callAmmoRunner())
    assertEquals(com.example.game.engine.RunnerState.GOING, engine.runnerState)
    assertFalse("Already on her way", engine.callAmmoRunner())

    // Nothing arrives right away
    engine.update(0.5f)
    assertEquals(0, weapon.reserveAmmo)

    var guard = 0
    while (engine.runnerState != com.example.game.engine.RunnerState.IDLE && guard++ < 2000) {
      engine.update(0.05f)
    }
    assertEquals(com.example.game.engine.RunnerState.IDLE, engine.runnerState)
    assertTrue("She should have delivered rounds", weapon.reserveAmmo > 0)
    assertTrue("Reserve never exceeds the cap", weapon.reserveAmmo <= weapon.maxReserve)
    assertTrue("Cooldown after a delivery", engine.runnerCooldown > 0f)
    assertFalse(engine.callAmmoRunner())
  }

  @Test
  fun `ammo girl refuses when the reserve is already full`() {
    val engine = newEngine()
    val weapon = engine.currentWeapon
    weapon.reserveAmmo = weapon.maxReserve
    assertFalse(engine.callAmmoRunner())
    assertEquals(com.example.game.engine.RunnerState.IDLE, engine.runnerState)
  }

  @Test
  fun `bullets leave from the barrel tip`() {
    val engine = newEngine()
    engine.onTouchShoot(900f, engine.gunAnchor.y)
    assertTrue(engine.projectiles.isNotEmpty())
    val p = engine.projectiles.first()
    val tip = engine.muzzleFor(0f)
    assertEquals(tip.x, p.x, 6f)
  }
}
