package com.example

import com.example.game.model.DefenseUpgrades
import com.example.game.model.Weapon
import com.example.game.model.WeaponType
import com.example.game.model.Zombie
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
  fun zombieHitboxesFollowTheBodyAndCatchFastProjectiles() {
    val zombie = Zombie(
      id = 1L,
      type = ZombieType.WALKER,
      x = 100f,
      y = 100f,
      currentHp = ZombieType.WALKER.maxHp,
      maxHp = ZombieType.WALKER.maxHp
    )

    assertTrue(zombie.intersectsProjectileSegment(0f, 68f, 200f, 68f, 4f))
    assertFalse(zombie.intersectsProjectileSegment(121f, 102f, 121f, 102f, 4f))
  }

  @Test
  fun zombieHitboxesAccountForLowProfileEnemiesAtTheBarricade() {
    val dog = Zombie(
      id = 2L,
      type = ZombieType.ZOMBIE_DOG,
      x = 100f,
      y = 100f,
      currentHp = ZombieType.ZOMBIE_DOG.maxHp,
      maxHp = ZombieType.ZOMBIE_DOG.maxHp
    )

    assertTrue(dog.hitboxLeft < dog.x)
    assertTrue(dog.intersectsProjectileSegment(58f, 88f, 140f, 88f, 3f))
    assertFalse(dog.intersectsProjectileSegment(60f, 60f, 140f, 60f, 3f))
  }
}
