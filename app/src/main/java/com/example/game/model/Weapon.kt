package com.example.game.model

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/**
 * The only three weapons. Reloading puts ONE round in per press, so filling a gun takes several presses.
 */
enum class WeaponType(
    val title: String,
    val magazine: Int,
    /** Rounds she carries at the start. */
    val startReserve: Int,
    /** The most rounds she can carry for this weapon. */
    val maxReserve: Int,
    /** Rounds the ammo girl brings per trip. */
    val delivery: Int,
    /** Seconds to load one round. */
    val reloadSeconds: Float,
    /** Seconds between two shots. */
    val fireCooldown: Float
) {
    REVOLVER("Revólver", magazine = 6, startReserve = 12, maxReserve = 24, delivery = 12, reloadSeconds = 0.45f, fireCooldown = 0.30f),
    SHOTGUN("Escopeta", magazine = 3, startReserve = 6, maxReserve = 12, delivery = 6, reloadSeconds = 0.60f, fireCooldown = 0.80f),
    GRENADE("Granada", magazine = 1, startReserve = 2, maxReserve = 6, delivery = 2, reloadSeconds = 0.50f, fireCooldown = 0.90f)
}

class Weapon(val type: WeaponType) {
    /** Rounds loaded and ready to fire. */
    var currentAmmo by mutableIntStateOf(type.magazine)

    /** Spare rounds in her pockets. */
    var reserveAmmo by mutableIntStateOf(type.startReserve)

    var isReloading by mutableStateOf(false)
    var reloadProgress by mutableFloatStateOf(0f)

    /** Seconds left before the next shot is allowed. */
    var cooldownLeft = 0f

    val magazine: Int get() = type.magazine
    val reserveSpace: Int get() = (type.maxReserve - reserveAmmo).coerceAtLeast(0)

    /** Adds rounds to the reserve, up to the cap. Returns how many actually fit. */
    fun addReserve(amount: Int): Int {
        val added = amount.coerceAtMost(reserveSpace).coerceAtLeast(0)
        reserveAmmo += added
        return added
    }

    /** Moves one round from the reserve into the magazine. Returns true if one was loaded. */
    fun loadOne(): Boolean {
        if (currentAmmo >= type.magazine || reserveAmmo <= 0) return false
        currentAmmo += 1
        reserveAmmo -= 1
        return true
    }

    fun cancelReload() {
        isReloading = false
        reloadProgress = 0f
    }

    fun reset() {
        currentAmmo = type.magazine
        reserveAmmo = type.startReserve
        cancelReload()
        cooldownLeft = 0f
    }
}
