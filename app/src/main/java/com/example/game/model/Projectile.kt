package com.example.game.model

enum class ProjectileType {
    BULLET,
    PELLET,
    GRENADE,
    ACID_SPIT,
    BOLT,
    FLAME,
    SNIPER_BULLET
}

data class Projectile(
    val id: Long,
    val type: ProjectileType,
    var x: Float,
    var y: Float,
    var vx: Float,
    var vy: Float,
    val damage: Float,
    val radius: Float = 4f,
    val isExplosive: Boolean = false,
    val blastRadius: Float = 0f,
    val isEnemy: Boolean = false,
    var lifetime: Float = 3f,
    var pierceCount: Int = 1 // how many zombies it can pierce before disappearing
) {
    var hasHit: Boolean = false
}
