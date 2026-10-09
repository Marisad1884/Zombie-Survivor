package com.example.game.model

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

enum class WeaponType(
    val title: String,
    val description: String,
    val baseDamage: Float,
    val baseCooldownMs: Long,
    val baseMagazineSize: Int,
    val baseReloadTimeMs: Long,
    val unlockCost: Int,
    val projectileSpeed: Float
) {
    PISTOL(
        title = "Pistola Táctica",
        description = "Arma básica confiable con munición infinita en reserva.",
        baseDamage = 26f,
        baseCooldownMs = 360L,
        baseMagazineSize = 12,
        baseReloadTimeMs = 1100L,
        unlockCost = 0,
        projectileSpeed = 1650f
    ),
    CROSSBOW(
        title = "Ballesta de Caza",
        description = "Dispara saetas veloces que perforan a múltiples zombis en fila.",
        baseDamage = 45f,
        baseCooldownMs = 520L,
        baseMagazineSize = 6,
        baseReloadTimeMs = 1300L,
        unlockCost = 40,
        projectileSpeed = 1750f
    ),
    SHOTGUN(
        title = "Escopeta de Asalto",
        description = "Dispara 6 perdigones con gran dispersión e impacto demoledor.",
        baseDamage = 22f, // Per pellet x6 = 132
        baseCooldownMs = 650L,
        baseMagazineSize = 8,
        baseReloadTimeMs = 1600L,
        unlockCost = 80,
        projectileSpeed = 1450f
    ),
    RIFLE(
        title = "Fusil M4 Militar",
        description = "Ráfaga continua de alta cadencia para repeler hordas.",
        baseDamage = 20f,
        baseCooldownMs = 110L,
        baseMagazineSize = 35,
        baseReloadTimeMs = 1800L,
        unlockCost = 150,
        projectileSpeed = 1850f
    ),
    FLAMETHROWER(
        title = "Lanzallamas Napalm",
        description = "Chorro continuo de fuego ardiente que incinera grupos de zombis.",
        baseDamage = 16f,
        baseCooldownMs = 80L,
        baseMagazineSize = 50,
        baseReloadTimeMs = 2000L,
        unlockCost = 220,
        projectileSpeed = 1200f
    ),
    LAUNCHER(
        title = "Lanzagranadas Pesado",
        description = "Proyectiles de alto impacto explosivo con tremendo radio de área.",
        baseDamage = 160f,
        baseCooldownMs = 1200L,
        baseMagazineSize = 5,
        baseReloadTimeMs = 2400L,
        unlockCost = 320,
        projectileSpeed = 1150f
    ),
    MINIGUN(
        title = "Ametralladora Gatling",
        description = "Tormenta de plomo masiva con 80 balas y cadencia extrema.",
        baseDamage = 24f,
        baseCooldownMs = 70L,
        baseMagazineSize = 80,
        baseReloadTimeMs = 2600L,
        unlockCost = 420,
        projectileSpeed = 1950f
    ),
    SNIPER(
        title = "Rifle Francotirador .50",
        description = "Calibre pesado antiblindaje que elimina tanques y jefes a gran distancia.",
        baseDamage = 280f,
        baseCooldownMs = 1400L,
        baseMagazineSize = 4,
        baseReloadTimeMs = 2100L,
        unlockCost = 500,
        projectileSpeed = 2200f
    )
}

class Weapon(
    val type: WeaponType,
    initialUnlocked: Boolean = false,
    initialLevel: Int = 1
) {
    var isUnlocked by mutableStateOf(initialUnlocked)
    var level by mutableIntStateOf(initialLevel)
    var currentAmmo by mutableIntStateOf(type.baseMagazineSize)
    var isReloading by mutableStateOf(false)
    var reloadProgress by mutableFloatStateOf(0f)
    var lastFireTime: Long = 0L

    val maxAmmo: Int
        get() = type.baseMagazineSize + (level - 1) * when (type) {
            WeaponType.PISTOL -> 2
            WeaponType.CROSSBOW -> 1
            WeaponType.SHOTGUN -> 2
            WeaponType.RIFLE -> 7
            WeaponType.FLAMETHROWER -> 10
            WeaponType.LAUNCHER -> 1
            WeaponType.MINIGUN -> 15
            WeaponType.SNIPER -> 1
        }

    val damage: Float
        get() = type.baseDamage * (1f + (level - 1) * 0.22f)

    val cooldownMs: Long
        get() = (type.baseCooldownMs * (1f - (level - 1) * 0.04f)).toLong().coerceAtLeast(45L)

    val reloadTimeMs: Long
        get() = (type.baseReloadTimeMs * (1f - (level - 1) * 0.05f)).toLong().coerceAtLeast(450L)

    val upgradeCost: Int
        get() = when (type) {
            WeaponType.PISTOL -> 35 * level
            WeaponType.CROSSBOW -> 50 * level
            WeaponType.SHOTGUN -> 70 * level
            WeaponType.RIFLE -> 110 * level
            WeaponType.FLAMETHROWER -> 140 * level
            WeaponType.LAUNCHER -> 180 * level
            WeaponType.MINIGUN -> 220 * level
            WeaponType.SNIPER -> 260 * level
        }

    val maxLevel: Int = 10
}
