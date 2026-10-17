package com.example.game.model

/**
 * Suit-wearing zombies that all walk along the same line (the hallway floor), left to right.
 * `scale` is how big the sprite is drawn; the hit box scales with it.
 */
enum class ZombieType(
    val maxHp: Float,
    /** Units per second at wave 1. */
    val speed: Float,
    val scale: Float,
    val score: Int
) {
    NORMAL(maxHp = 100f, speed = 30f, scale = 1.0f, score = 10),
    RUNNER(maxHp = 70f, speed = 62f, scale = 0.95f, score = 15),
    BRUTE(maxHp = 260f, speed = 22f, scale = 1.3f, score = 30)
}

class Zombie(
    val id: Long,
    val type: ZombieType,
    /** Horizontal position of the body centre. */
    var x: Float,
    /** Wave speed multiplier, fixed when it spawns. */
    val speedMult: Float
) {
    var hp: Float = type.maxHp
    var walkTimer: Float = 0f
    var hitFlash: Float = 0f
    /** While above zero the zombie is staggering and barely moves (shotgun blasts). */
    var stagger: Float = 0f
    var dying: Boolean = false
    var deathTimer: Float = 0f
    var headshotDeath: Boolean = false

    val isAlive: Boolean get() = !dying

    /** Front edge: where the outstretched arms end. */
    val frontX: Float get() = x + 16f * type.scale

    companion object {
        /** Sprite height (feet to hair) in world units at scale 1. */
        const val BASE_HEIGHT = 100f
    }

    val height: Float get() = BASE_HEIGHT * type.scale
}
