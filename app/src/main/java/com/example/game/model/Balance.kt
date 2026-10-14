package com.example.game.model

/**
 * Every tunable number for difficulty, economy, ammo and the ammo runner, in one place.
 * Change a value here and the whole game follows; nothing else needs touching.
 */
object Balance {

    // ---------------------------------------------------------------- difficulty
    /** Enemy mix is picked as if the wave were this many waves further along. */
    const val TIER_OFFSET = 1

    const val WAVE_BASE_ZOMBIES = 14
    const val WAVE_ZOMBIES_PER_WAVE = 5
    const val WAVE_EXTRA_PER_WAVE = 2
    const val WAVE_EXTRA_CAP = 70

    /** Seconds between spawns: starts at START, shrinks by STEP per wave (up to 25 waves), never below MIN. */
    const val SPAWN_INTERVAL_START = 1.10f
    const val SPAWN_INTERVAL_STEP = 0.03f
    const val SPAWN_INTERVAL_MIN = 0.45f

    /** Extra health, speed and damage per wave, as a fraction of the base value. */
    const val HP_GROWTH_PER_WAVE = 0.07f
    const val SPEED_GROWTH_PER_WAVE = 0.012f
    const val SPEED_MULT_CAP = 1.5f
    const val DAMAGE_GROWTH_PER_WAVE = 0.03f

    // ---------------------------------------------------------------- economy
    /** Bonus coins for clearing a wave: BASE + PER_WAVE * wave. */
    const val WAVE_BONUS_BASE = 6
    const val WAVE_BONUS_PER_WAVE = 3

    // ---------------------------------------------------------------- ammo
    /** Spare magazines a weapon starts with (the pistol too: nothing is infinite any more). */
    const val START_RESERVE_MAGS = 3

    /** The most spare magazines she can carry in her pockets for one weapon. */
    const val MAX_RESERVE_MAGS = 4

    // ---------------------------------------------------------------- ammo runner (the girl)
    /** Walking speed in pixels per second (a bit slower while carrying the crate). */
    const val RUNNER_SPEED = 120f
    const val RUNNER_LOADED_SPEED_FACTOR = 0.85f

    /** Seconds she spends out of sight getting the crate. */
    const val RUNNER_FETCH_SECONDS = 2.6f

    /** Seconds she spends handing the crate over. The ammo lands halfway through. */
    const val RUNNER_HAND_SECONDS = 1.0f

    /** Seconds before she can be sent again, counted from the moment the ammo is delivered. */
    const val RUNNER_COOLDOWN_SECONDS = 7f

    /** How many magazines of the selected weapon she brings per trip. */
    const val RUNNER_DELIVERY_MAGS = 2f
}
