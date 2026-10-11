package com.example.game.model

import androidx.compose.ui.graphics.Color

enum class ParticleType {
    BLOOD,
    WATER_SPLASH,
    EXPLOSION_FIRE,
    SMOKE,
    SHELL_CASING,
    TOXIC_SLIME,
    MUZZLE_FLASH,
    FLOATING_TEXT,
    BLOOD_POOL, // flat decal left on the water where a zombie died
    SPARK,      // bright impact / ricochet spark
    EMBER       // glowing ember floating up from fires
}

data class Particle(
    val id: Long,
    val type: ParticleType,
    var x: Float,
    var y: Float,
    var vx: Float,
    var vy: Float,
    var alpha: Float = 1f,
    var scale: Float = 1f,
    var maxLife: Float = 0.6f,
    var life: Float = 0.6f,
    val color: Color = Color.White,
    val text: String = "",
    val radius: Float = 4f,
    val gravity: Float = 0f,     // pixels / s^2 applied to vy
    val growth: Float = 0f,      // scale units / s (smoke expands, sparks shrink with negative values)
    val rotation: Float = 0f     // initial rotation in radians (used by shell casings)
) {
    val isDead: Boolean
        get() = life <= 0f

    /** 1.0 right after spawning, 0.0 when about to disappear. */
    val lifeRatio: Float
        get() = if (maxLife <= 0f) 0f else (life / maxLife).coerceIn(0f, 1f)
}
