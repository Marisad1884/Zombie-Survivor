package com.example.game.model

enum class ZombieType(
    val displayName: String,
    val maxHp: Float,
    val speed: Float, // pixels per second
    val attackDamage: Float,
    val attackIntervalMs: Long,
    val coinReward: Int,
    val width: Float,
    val height: Float,
    val armorDamageReduction: Float = 0f,
    val isBoss: Boolean = false
) {
    WALKER(
        displayName = "Caminante",
        maxHp = 60f,
        speed = 62f,
        attackDamage = 8.5f,
        attackIntervalMs = 1100L,
        coinReward = 2,
        width = 44f,
        height = 68f
    ),
    RUNNER(
        displayName = "Zombi Veloz",
        maxHp = 38f,
        speed = 135f,
        attackDamage = 6.5f,
        attackIntervalMs = 700L,
        coinReward = 3,
        width = 38f,
        height = 56f
    ),
    ZOMBIE_DOG(
        displayName = "Perro Rabioso",
        maxHp = 32f,
        speed = 160f,
        attackDamage = 10f,
        attackIntervalMs = 580L,
        coinReward = 4,
        width = 48f,
        height = 36f
    ),
    SPITTER(
        displayName = "Zombi Tóxico",
        maxHp = 80f,
        speed = 52f,
        attackDamage = 12f,
        attackIntervalMs = 2100L,
        coinReward = 5,
        width = 46f,
        height = 66f
    ),
    ARMORED(
        displayName = "Soldado Blindado",
        maxHp = 145f,
        speed = 50f,
        attackDamage = 14f,
        attackIntervalMs = 1200L,
        coinReward = 7,
        width = 48f,
        height = 70f,
        armorDamageReduction = 0.45f
    ),
    GATOR(
        displayName = "Caimán Zombi",
        maxHp = 125f,
        speed = 76f,
        attackDamage = 20f,
        attackIntervalMs = 1050L,
        coinReward = 8,
        width = 64f,
        height = 34f
    ),
    TANK(
        displayName = "Bruto del Pantano",
        maxHp = 320f,
        speed = 34f,
        attackDamage = 28f,
        attackIntervalMs = 1600L,
        coinReward = 14,
        width = 72f,
        height = 100f
    ),
    COLOSSUS(
        displayName = "★ Megatitán Boss ★",
        maxHp = 950f,
        speed = 26f,
        attackDamage = 50f,
        attackIntervalMs = 1900L,
        coinReward = 40,
        width = 96f,
        height = 130f,
        armorDamageReduction = 0.25f,
        isBoss = true
    )
}

data class Zombie(
    val id: Long,
    val type: ZombieType,
    var x: Float,
    var y: Float, // vertical position in swamp lane
    var currentHp: Float,
    var maxHp: Float,
    var lastAttackTime: Long = 0L,
    var isAttacking: Boolean = false,
    var walkAnimTimer: Float = 0f,
    var hitFlashTimer: Float = 0f,
    var slowTimer: Float = 0f
) {
    val isDead: Boolean
        get() = currentHp <= 0

    private val hitboxHalfWidth: Float
        get() = when (type) {
            ZombieType.ZOMBIE_DOG -> 32f
            ZombieType.GATOR -> 41f
            else -> type.width * 0.42f
        }

    private val hitboxHalfHeight: Float
        get() = when (type) {
            ZombieType.ZOMBIE_DOG -> 14f
            ZombieType.GATOR -> 11f
            else -> type.height * 0.54f
        }

    private val hitboxCenterOffsetX: Float
        get() = when (type) {
            ZombieType.ZOMBIE_DOG -> -10f
            ZombieType.GATOR -> 3f
            else -> 0f
        }

    private val hitboxCenterOffsetY: Float
        get() = when (type) {
            ZombieType.ZOMBIE_DOG -> -12f
            ZombieType.GATOR -> -9f
            else -> -type.height * 0.48f
        }

    val hitboxLeft: Float
        get() = x + hitboxCenterOffsetX - hitboxHalfWidth

    fun intersectsProjectileSegment(
        startX: Float,
        startY: Float,
        endX: Float,
        endY: Float,
        projectileRadius: Float
    ): Boolean {
        val centerX = x + hitboxCenterOffsetX
        val centerY = y + hitboxCenterOffsetY
        val radius = projectileRadius.coerceAtLeast(0f)
        val normalizedStartX = (startX - centerX) / (hitboxHalfWidth + radius)
        val normalizedStartY = (startY - centerY) / (hitboxHalfHeight + radius)
        val normalizedEndX = (endX - centerX) / (hitboxHalfWidth + radius)
        val normalizedEndY = (endY - centerY) / (hitboxHalfHeight + radius)
        val dx = normalizedEndX - normalizedStartX
        val dy = normalizedEndY - normalizedStartY
        val segmentLengthSquared = dx * dx + dy * dy
        val closestPointT = if (segmentLengthSquared == 0f) {
            0f
        } else {
            ((-normalizedStartX * dx - normalizedStartY * dy) / segmentLengthSquared)
                .coerceIn(0f, 1f)
        }
        val closestX = normalizedStartX + dx * closestPointT
        val closestY = normalizedStartY + dy * closestPointT

        return closestX * closestX + closestY * closestY <= 1f
    }
}
