package com.example

import com.example.game.model.Balance
import com.example.game.model.DefenseUpgrades
import com.example.game.model.Weapon
import com.example.game.model.WeaponType
import com.example.game.model.ZombieType
import org.junit.Assert.*
import org.junit.Test

class ExampleUnitTest {

  @Test
  fun testWeaponUpgrades() {
    val shotgun = Weapon(WeaponType.SHOTGUN, initialUnlocked = true)
    assertEquals(1, shotgun.level)
    assertEquals(shotgun.type.baseMagazineSize, shotgun.maxAmmo)

    shotgun.level = 2
    assertTrue(shotgun.damage > shotgun.type.baseDamage)
    assertTrue(shotgun.maxAmmo > shotgun.type.baseMagazineSize)
    assertEquals(10, shotgun.maxLevel)
  }

  @Test
  fun testDefenseUpgrades() {
    val defense = DefenseUpgrades()
    assertEquals(100f, defense.maxHouseHp)

    defense.maxHouseHpLevel = 2
    assertEquals(140f, defense.maxHouseHp)

    assertEquals(0f, defense.spikeDamagePerSec)
    defense.barbedWireLevel = 1
    assertTrue(defense.spikeDamagePerSec > 0f)
    assertTrue(defense.spikeSlowFactor < 1.0f)

    defense.turretLevel = 1
    assertTrue(defense.turretDamage > 0f)
  }

  @Test
  fun testZombieTypes() {
    assertTrue(ZombieType.TANK.maxHp > ZombieType.WALKER.maxHp)
    assertTrue(ZombieType.RUNNER.speed > ZombieType.WALKER.speed)
    assertTrue(ZombieType.COLOSSUS.maxHp > ZombieType.TANK.maxHp)
    assertTrue(ZombieType.COLOSSUS.isBoss)
    assertTrue(ZombieType.ARMORED.armorDamageReduction > 0f)
    assertTrue(ZombieType.ZOMBIE_DOG.speed > ZombieType.WALKER.speed)
  }

  @Test
  fun testReserveAmmoIsLimited() {
    val rifle = Weapon(WeaponType.RIFLE, initialUnlocked = true)
    assertEquals(rifle.maxAmmo * Balance.START_RESERVE_MAGS, rifle.reserveAmmo)
    assertEquals(rifle.maxAmmo * Balance.MAX_RESERVE_MAGS, rifle.maxReserve)

    val added = rifle.addReserve(10_000)
    assertEquals(rifle.maxReserve, rifle.reserveAmmo)
    assertTrue(added > 0)
    assertEquals(0, rifle.addReserve(5))

    rifle.currentAmmo = 0
    rifle.reserveAmmo = rifle.maxAmmo / 2
    val moved = rifle.finishReload()
    assertEquals(rifle.maxAmmo / 2, moved)
    assertEquals(0, rifle.reserveAmmo)
  }

  @Test
  fun testEconomyIsTighter() {
    assertTrue(ZombieType.WALKER.coinReward <= 1)
    assertTrue(ZombieType.TANK.coinReward <= 8)
    assertTrue(WeaponType.SHOTGUN.unlockCost >= 100)
    assertTrue(Balance.WAVE_BONUS_BASE + Balance.WAVE_BONUS_PER_WAVE < 15)
  }
}
