package com.example.game.model

/**
 * Every tunable number of the game in one place.
 *
 * The world is measured in "units": the screen is always 340 units tall and as wide as its aspect ratio
 * needs, so the game looks and plays the same on every phone. A sprite pixel is 2 units.
 */
object Balance {

    // ---------------------------------------------------------------- world layout
    const val WORLD_H = 340f
    /** Height of the grey HUD bar, as a fraction of the screen. */
    const val BAR_FRACTION = 0.17f
    /** Where everybody's feet are. */
    const val FLOOR_Y = 318f
    /** One sprite pixel, in world units. */
    const val PX = 2f
    /** How far from the right edge the heroine stands. */
    const val HERO_FROM_RIGHT = 150f
    /** Door: centre distance from the right edge, and width. */
    const val DOOR_FROM_RIGHT = 63f
    const val DOOR_WIDTH = 58f
    const val DOOR_HEIGHT = 104f

    // ---------------------------------------------------------------- zombies
    const val SPAWN_X = -40f
    /** Zombies are caught-up with the heroine when their arms get this close to her. */
    const val CATCH_MARGIN = 6f
    /** Top fraction of a zombie that counts as head. */
    const val HEAD_FRACTION = 0.26f
    const val HEADSHOT_MULTIPLIER = 2.5f

    const val FIRST_SPAWN_DELAY = 1.5f
    const val SPAWN_INTERVAL_START = 3.4f
    const val SPAWN_INTERVAL_STEP = 0.22f
    const val SPAWN_INTERVAL_MIN = 1.0f
    const val KILLS_PER_WAVE = 10
    const val SPEED_GROWTH_PER_WAVE = 0.03f
    const val SPEED_MULT_CAP = 1.9f
    const val RUNNER_FROM_WAVE = 2
    const val BRUTE_FROM_WAVE = 4

    // ---------------------------------------------------------------- score
    const val SCORE_PER_KILL_HEADSHOT_BONUS = 10

    // ---------------------------------------------------------------- weapons
    const val REVOLVER_DAMAGE = 34f

    /** The shotgun does triple the revolver's damage per shell, spread over its pellets. */
    const val SHOTGUN_PELLETS = 6
    const val SHOTGUN_DAMAGE_PER_SHELL = REVOLVER_DAMAGE * 3f
    const val SHOTGUN_SPREAD = 0.075f
    /** Shotgun reaches only this fraction of the map width, counted from the heroine. */
    const val SHOTGUN_RANGE_FRACTION = 0.5f

    /** Enough to kill anything: the grenade clears the whole screen. */
    const val GRENADE_DAMAGE = 100000f
    const val GRENADE_MAX_DISTANCE_FROM_HERO = 90f

    // ---------------------------------------------------------------- ammo girl
    /** Walking speed in units per second. */
    const val RUNNER_SPEED = 70f
    /** Seconds she spends inside the house getting the ammo. */
    const val RUNNER_FETCH_SECONDS = 4.0f
    const val RUNNER_HAND_SECONDS = 1.0f
    /** Seconds before she can be sent again, counted from the delivery. */
    const val RUNNER_COOLDOWN_SECONDS = 5f
}
