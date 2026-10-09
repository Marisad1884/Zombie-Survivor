package com.example

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
}
