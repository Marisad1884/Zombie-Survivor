package com.example.game.model

import androidx.compose.ui.graphics.Color

enum class ParticleKind {
    BLOOD,
    SPARK,
    SMOKE,
    FIRE,
    CASING,
    TEXT
}

/** A little square of pixel-art effect. Sizes are in world units. */
class Particle(
    val kind: ParticleKind,
    var x: Float,
    var y: Float,
    var vx: Float,
    var vy: Float,
    var maxLife: Float,
    val color: Color,
    val size: Float = 2f,
    val gravity: Float = 0f,
    val text: String = ""
) {
    var life: Float = maxLife
    val isDead: Boolean get() = life <= 0f
    val lifeRatio: Float get() = if (maxLife <= 0f) 0f else (life / maxLife).coerceIn(0f, 1f)
}

/** A bullet trail that fades in a blink. */
class Tracer(
    val x1: Float,
    val y1: Float,
    val x2: Float,
    val y2: Float,
    var life: Float = 0.07f,
    val color: Color = Color(0xFFFFF3B0)
)

/** A grenade in the air: it follows an arc from the heroine's hand to where it was thrown. */
class ThrownGrenade(
    val x0: Float,
    val y0: Float,
    val x1: Float,
    val y1: Float,
    val flightTime: Float
) {
    var t: Float = 0f
    val progress: Float get() = (t / flightTime).coerceIn(0f, 1f)
    val x: Float get() = x0 + (x1 - x0) * progress
    val y: Float get() = y0 + (y1 - y0) * progress - 4f * ARC_HEIGHT * progress * (1f - progress)

    companion object {
        const val ARC_HEIGHT = 70f
    }
}
