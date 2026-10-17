package com.example

import com.example.game.model.Balance
import com.example.game.model.Weapon
import com.example.game.model.WeaponType
import com.example.game.model.Zombie
import com.example.game.model.ZombieType
import org.junit.Assert.*
import org.junit.Test

class ExampleUnitTest {

  @Test
  fun onlyThreeWeapons() {
    assertEquals(listOf("REVOLVER", "SHOTGUN", "GRENADE"), WeaponType.values().map { it.name })
  }

  @Test
  fun magazineSizes() {
    assertEquals(6, WeaponType.REVOLVER.magazine)
    assertEquals(3, WeaponType.SHOTGUN.magazine)
  }

  @Test
  fun shotgunDoesTripleDamagePerShell() {
    assertEquals(Balance.REVOLVER_DAMAGE * 3f, Balance.SHOTGUN_DAMAGE_PER_SHELL, 0.001f)
    assertEquals(0.5f, Balance.SHOTGUN_RANGE_FRACTION, 0.0001f)
  }

  @Test
  fun reloadLoadsOneRoundAtATime() {
    val w = Weapon(WeaponType.REVOLVER)
    w.currentAmmo = 0
    val reserve = w.reserveAmmo
    assertTrue(w.loadOne())
    assertEquals(1, w.currentAmmo)
    assertEquals(reserve - 1, w.reserveAmmo)
  }

  @Test
  fun cannotOverfillOrLoadWithoutReserve() {
    val full = Weapon(WeaponType.SHOTGUN)
    assertFalse(full.loadOne())

    val empty = Weapon(WeaponType.SHOTGUN)
    empty.currentAmmo = 0
    empty.reserveAmmo = 0
    assertFalse(empty.loadOne())
  }

  @Test
  fun reserveIsCapped() {
    val w = Weapon(WeaponType.REVOLVER)
    val added = w.addReserve(1000)
    assertEquals(w.type.maxReserve, w.reserveAmmo)
    assertTrue(added > 0)
    assertEquals(0, w.addReserve(1))
  }

  @Test
  fun zombiesComeInDifferentSizes() {
    assertTrue(ZombieType.BRUTE.maxHp > ZombieType.NORMAL.maxHp)
    assertTrue(ZombieType.RUNNER.speed > ZombieType.NORMAL.speed)
    val z = Zombie(1, ZombieType.NORMAL, 10f, 1f)
    assertTrue(z.isAlive)
    assertTrue(z.frontX > z.x)
  }

  @Test
  fun grenadeIsASingleGrenadeInHand() {
    val g = Weapon(WeaponType.GRENADE)
    assertEquals(1, g.currentAmmo)
    assertEquals(0, g.reserveAmmo)
    assertEquals(0, g.refillSpace)
    g.currentAmmo = 0
    assertEquals(1, g.refillSpace)
    assertEquals(1, g.addDelivered(5))
    assertEquals(1, g.currentAmmo)
    assertEquals(0, g.addDelivered(1))
  }
}
