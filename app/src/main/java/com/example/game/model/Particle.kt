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
    FLOATING_TEXT
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
    val radius: Float = 4f
) {
    val isDead: Boolean
        get() = life <= 0f
}
