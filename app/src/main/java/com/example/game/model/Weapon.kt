package com.example.game.model

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue

/**
 * The only three weapons. Reloading is instant but puts ONE round in per press, so filling a gun takes several presses.
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
    /** Seconds between two shots. */
    val fireCooldown: Float
) {
    REVOLVER("Revólver", magazine = 6, startReserve = 12, maxReserve = 24, delivery = 12, fireCooldown = 0.30f),
    SHOTGUN("Escopeta", magazine = 3, startReserve = 6, maxReserve = 12, delivery = 6, fireCooldown = 0.80f),
    /** One grenade at a time, kept in her hand: there is no reserve, only what the ammo girl brings. */
    GRENADE("Granada", magazine = 1, startReserve = 0, maxReserve = 0, delivery = 1, fireCooldown = 0.90f)
}

class Weapon(val type: WeaponType) {
    /** Rounds loaded and ready to fire. */
    var currentAmmo by mutableIntStateOf(type.magazine)

    /** Spare rounds in her pockets. */
    var reserveAmmo by mutableIntStateOf(type.startReserve)

    /** Seconds left before the next shot is allowed. */
    var cooldownLeft = 0f

    val magazine: Int get() = type.magazine
    val reserveSpace: Int get() = (type.maxReserve - reserveAmmo).coerceAtLeast(0)

    /**
     * Room left for the ammo girl to fill. The grenade has no reserve: the single grenade goes straight into
     * her hand, so there is room only when her hand is empty.
     */
    val refillSpace: Int
        get() = if (type == WeaponType.GRENADE) (type.magazine - currentAmmo).coerceAtLeast(0) else reserveSpace

    /** What the ammo girl hands over. Returns how many rounds were actually taken. */
    fun addDelivered(amount: Int): Int {
        if (type != WeaponType.GRENADE) return addReserve(amount)
        val added = amount.coerceAtMost(refillSpace).coerceAtLeast(0)
        currentAmmo += added
        return added
    }

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

    fun reset() {
        currentAmmo = type.magazine
        reserveAmmo = type.startReserve
        cooldownLeft = 0f
    }
}
