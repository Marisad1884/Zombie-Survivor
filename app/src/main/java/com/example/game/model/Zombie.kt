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
        coinReward = 1,
        width = 44f,
        height = 68f
    ),
    RUNNER(
        displayName = "Zombi Veloz",
        maxHp = 38f,
        speed = 135f,
        attackDamage = 6.5f,
        attackIntervalMs = 700L,
        coinReward = 2,
        width = 38f,
        height = 56f
    ),
    ZOMBIE_DOG(
        displayName = "Perro Rabioso",
        maxHp = 32f,
        speed = 160f,
        attackDamage = 10f,
        attackIntervalMs = 580L,
        coinReward = 2,
        width = 48f,
        height = 36f
    ),
    SPITTER(
        displayName = "Zombi Tóxico",
        maxHp = 80f,
        speed = 52f,
        attackDamage = 12f,
        attackIntervalMs = 2100L,
        coinReward = 3,
        width = 46f,
        height = 66f
    ),
    ARMORED(
        displayName = "Soldado Blindado",
        maxHp = 145f,
        speed = 50f,
        attackDamage = 14f,
        attackIntervalMs = 1200L,
        coinReward = 4,
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
        coinReward = 5,
        width = 64f,
        height = 34f
    ),
    TANK(
        displayName = "Bruto del Pantano",
        maxHp = 320f,
        speed = 34f,
        attackDamage = 28f,
        attackIntervalMs = 1600L,
        coinReward = 8,
        width = 72f,
        height = 100f
    ),
    COLOSSUS(
        displayName = "★ Megatitán Boss ★",
        maxHp = 950f,
        speed = 26f,
        attackDamage = 50f,
        attackIntervalMs = 1900L,
        coinReward = 25,
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
    var slowTimer: Float = 0f,
    /** Later waves bring faster, harder-hitting zombies. */
    val speedMult: Float = 1f,
    val damageMult: Float = 1f
) {
    val isDead: Boolean
        get() = currentHp <= 0
}
