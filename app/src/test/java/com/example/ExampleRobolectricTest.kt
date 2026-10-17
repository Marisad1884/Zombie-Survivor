package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.game.data.LeaderboardRepository
import com.example.game.engine.GameEngine
import com.example.game.engine.GameStatus
import com.example.game.engine.RunnerState
import com.example.game.model.WeaponType
import com.example.game.model.Zombie
import com.example.game.model.ZombieType
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

  private fun newEngine(): GameEngine {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val engine = GameEngine(com.example.game.audio.SoundManager(context)) { _, _, _, _ -> }
    engine.setWorldSize(1360f, 680f) // 2:1 screen -> 680 x 340 world units
    return engine
  }

  /** Puts a still zombie on the floor and returns it. */
  private fun GameEngine.addZombie(x: Float, type: ZombieType = ZombieType.NORMAL): Zombie {
    val z = Zombie(1000L + zombies.size, type, x, 1f)
    zombies.add(z)
    return z
  }

  private fun GameEngine.headY(z: Zombie) = floorY - z.height * 0.9f
  private fun GameEngine.bodyY(z: Zombie) = floorY - z.height * 0.5f

  @Test
  fun `a zombie that reaches her catches her instantly`() {
    val engine = newEngine()
    engine.addZombie(engine.heroX - 4f)
    engine.update(0.05f)
    assertEquals(GameStatus.GAME_OVER, engine.status)
  }

  @Test
  fun `reload is instant and puts in one round per press`() {
    val engine = newEngine()
    val w = engine.currentWeapon
    w.currentAmmo = 0
    val reserve = w.reserveAmmo

    assertTrue(engine.reloadOne())
    assertEquals(1, w.currentAmmo) // no waiting, no loading bar
    assertEquals(reserve - 1, w.reserveAmmo)

    assertTrue(engine.reloadOne())
    assertEquals(2, w.currentAmmo)
  }

  @Test
  fun `empty revolver does not reload by itself`() {
    val engine = newEngine()
    val w = engine.currentWeapon
    w.currentAmmo = 0
    engine.onTouchShoot(200f, engine.floorY - 50f)
    repeat(40) { engine.update(0.05f) }
    assertEquals(0, w.currentAmmo)
  }

  @Test
  fun `headshot hurts more than a body shot`() {
    val engine = newEngine()
    val body = engine.addZombie(300f)
    engine.onTouchShoot(body.x + 10f, engine.bodyY(body))
    val bodyDamage = body.type.maxHp - body.hp
    assertTrue(bodyDamage > 0f)

    val engine2 = newEngine()
    val head = engine2.addZombie(300f)
    engine2.onTouchShoot(head.x + 10f, engine2.headY(head))
    val headDamage = head.type.maxHp - head.hp
    assertTrue("head=$headDamage body=$bodyDamage", headDamage > bodyDamage * 2f)
  }

  @Test
  fun `shotgun does not reach past the middle of the map`() {
    val engine = newEngine()
    engine.selectWeapon(1)
    assertEquals(WeaponType.SHOTGUN, engine.currentWeapon.type)
    val far = engine.addZombie(engine.shotgunLimitX - 60f)
    engine.onTouchShoot(far.x, engine.bodyY(far))
    assertEquals(far.type.maxHp, far.hp, 0.001f)

    val near = engine.addZombie(engine.shotgunLimitX + 30f)
    engine.update(1f) // let the cooldown pass
    engine.onTouchShoot(near.x, engine.bodyY(near))
    assertTrue(near.hp < near.type.maxHp)
  }

  @Test
  fun `grenade clears every zombie on screen and you only hold one`() {
    val engine = newEngine()
    engine.selectWeapon(2)
    assertEquals(WeaponType.GRENADE, engine.currentWeapon.type)
    assertEquals(1, engine.currentWeapon.currentAmmo)
    assertEquals(0, engine.currentWeapon.reserveAmmo)

    val zs = listOf(40f, 150f, 260f, 330f).map { engine.addZombie(it) }
    engine.onTouchShoot(230f, engine.floorY - 20f)
    assertEquals(0, engine.currentWeapon.currentAmmo)
    repeat(40) { engine.update(0.05f) }
    assertTrue("every zombie on screen dies", zs.all { it.dying })

    // no second grenade and nothing to reload with
    assertFalse(engine.reloadOne())
    engine.update(1f)
    engine.onTouchShoot(230f, engine.floorY - 20f)
    assertTrue(engine.grenades.isEmpty())
  }

  @Test
  fun `ammo girl brings the next grenade`() {
    val engine = newEngine()
    engine.selectWeapon(2)
    engine.onTouchShoot(230f, engine.floorY - 20f)
    repeat(30) { engine.update(0.05f) }
    assertEquals(0, engine.currentWeapon.currentAmmo)

    assertTrue(engine.callAmmoRunner())
    var guard = 0
    while (engine.runnerState != RunnerState.IDLE && guard++ < 1000) engine.update(0.05f)
    assertEquals(1, engine.currentWeapon.currentAmmo)
    assertFalse("only one at a time", engine.callAmmoRunner())
  }

  @Test
  fun `ammo girl only brings ammo for the weapon equipped when called`() {
    val engine = newEngine()
    val revolver = engine.weapons[0]
    val shotgun = engine.weapons[1]
    revolver.reserveAmmo = 0
    shotgun.reserveAmmo = 0

    engine.selectWeapon(1)
    assertTrue(engine.callAmmoRunner())
    engine.selectWeapon(0) // switching afterwards must not change what she brings

    var guard = 0
    while (engine.runnerState != RunnerState.IDLE && guard++ < 1000) engine.update(0.05f)
    assertEquals(RunnerState.IDLE, engine.runnerState)
    assertEquals(shotgun.type.delivery, shotgun.reserveAmmo)
    assertEquals(0, revolver.reserveAmmo)
    assertFalse("she has to cool down before the next trip", engine.callAmmoRunner())
  }
}
