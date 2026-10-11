package com.example.game.engine

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import com.example.game.audio.SoundManager
import com.example.game.model.*
import kotlin.math.*
import kotlin.random.Random

enum class GameStatus {
    PLAYING,
    WAVE_CLEARED,
    PAUSED,
    GAME_OVER
}

data class ActiveFireZone(
    val x: Float,
    val y: Float,
    val radius: Float = 110f,
    var remainingSeconds: Float = 5.0f
)

class GameEngine(
    private val soundManager: SoundManager,
    private val onGameOver: (survivedSeconds: Int, wave: Int, kills: Int, coins: Int) -> Unit
) {
    // Observable Compose States for instant 60 FPS HUD and UI reactivity
    var status: GameStatus by mutableStateOf(GameStatus.PLAYING)
        internal set

    var wave: Int by mutableIntStateOf(1)
        internal set

    var houseHp: Float by mutableFloatStateOf(100f)
        internal set

    var defenseUpgrades: DefenseUpgrades by mutableStateOf(DefenseUpgrades())
        internal set

    var coins: Int by mutableIntStateOf(0)
        internal set

    var score: Int by mutableIntStateOf(0)
        internal set

    var survivedSeconds: Float by mutableFloatStateOf(0f)
        internal set

    var zombiesKilled: Int by mutableIntStateOf(0)
        internal set

    var selectedWeaponIndex: Int by mutableIntStateOf(0)
        internal set

    var molotovCooldownRemaining: Float by mutableFloatStateOf(0f)
        internal set

    var waveClearCountdown: Float by mutableFloatStateOf(0f)
        internal set

    var houseDamageFlash: Float by mutableFloatStateOf(0f)
        internal set

    var muzzleFlashTimer: Float by mutableFloatStateOf(0f)
        internal set

    // Complete Arsenal of 8 weapons!
    val weapons: List<Weapon> = listOf(
        Weapon(type = WeaponType.PISTOL, initialUnlocked = true),
        Weapon(type = WeaponType.CROSSBOW, initialUnlocked = false),
        Weapon(type = WeaponType.SHOTGUN, initialUnlocked = false),
        Weapon(type = WeaponType.RIFLE, initialUnlocked = false),
        Weapon(type = WeaponType.FLAMETHROWER, initialUnlocked = false),
        Weapon(type = WeaponType.LAUNCHER, initialUnlocked = false),
        Weapon(type = WeaponType.MINIGUN, initialUnlocked = false),
        Weapon(type = WeaponType.SNIPER, initialUnlocked = false)
    )

    val currentWeapon: Weapon
        get() = weapons[selectedWeaponIndex]

    // Screen Shake
    var shakeX: Float = 0f
        private set
    var shakeY: Float = 0f
        private set
    private var shakeIntensity: Float = 0f

    // Field entities
    val zombies = mutableListOf<Zombie>()
    val projectiles = mutableListOf<Projectile>()
    val particles = mutableListOf<Particle>()
    val fireZones = mutableListOf<ActiveFireZone>()

    // Wave spawning state
    private var zombiesToSpawnInWave: Int = 0
    var totalZombiesInWave: Int = 0
        private set
    private var timeSinceLastSpawn: Float = 0f
    private var currentSpawnInterval: Float = 1.6f
    private var nextZombieId: Long = 1L
    private var nextProjectileId: Long = 1L
    private var nextParticleId: Long = 1L

    val waveClearDuration: Float = 5.0f

    // Roof Sentry Turret timer
    private var timeSinceLastTurretShot: Float = 0f
    private val molotovCooldownMax: Float = 7f

    // Screen bounds
    var screenWidth: Float = 1200f
    var screenHeight: Float = 700f

    val houseWidth: Float
        get() = screenWidth * 0.22f

    val barricadeX: Float
        get() = houseWidth

    val playerMuzzleOffset: Offset
        get() {
            val porchY = screenHeight * 0.58f
            val bodyX = houseWidth - 42f
            val bodyY = porchY - 54f
            val gunAnchorX = bodyX + 16f
            val gunAnchorY = bodyY + 16f
            return Offset(gunAnchorX + 38f, gunAnchorY)
        }

    val roofTurretOffset: Offset
        get() = Offset(houseWidth * 0.42f, screenHeight * 0.28f)

    init {
        startNewRun()
    }

    fun startNewRun() {
        status = GameStatus.PLAYING
        wave = 1
        defenseUpgrades = DefenseUpgrades()
        houseHp = defenseUpgrades.maxHouseHp
        coins = 0
        score = 0
        survivedSeconds = 0f
        zombiesKilled = 0
        selectedWeaponIndex = 0
        waveClearCountdown = 0f
        houseDamageFlash = 0f
        timeSinceLastTurretShot = 0f

        // Reset weapons
        weapons.forEachIndexed { index, weapon ->
            weapon.isUnlocked = (index == 0)
            weapon.level = 1
            weapon.currentAmmo = weapon.maxAmmo
            weapon.isReloading = false
            weapon.reloadProgress = 0f
            weapon.lastFireTime = 0L
        }

        zombies.clear()
        projectiles.clear()
        particles.clear()
        fireZones.clear()
        molotovCooldownRemaining = 0f
        shakeIntensity = 0f
        shakeX = 0f
        shakeY = 0f

        initWave(1)
    }

    fun initWave(waveNumber: Int) {
        wave = waveNumber
        status = GameStatus.PLAYING
        waveClearCountdown = 0f

        totalZombiesInWave = 14 + (wave * 5) + min(wave * 2, 70)
        zombiesToSpawnInWave = totalZombiesInWave
        currentSpawnInterval = (1.35f - (min(wave, 25) * 0.03f)).coerceAtLeast(0.6f)
        timeSinceLastSpawn = currentSpawnInterval * 0.5f

        spawnFloatingText(
            screenWidth * 0.5f,
            screenHeight * 0.3f,
            if (wave % 5 == 0) "¡ALERTA: OLEADA BOSS #$wave!" else "¡OLEADA $wave!",
            if (wave % 5 == 0) Color(0xFFFF1744) else Color(0xFFFFD54F)
        )
    }

    fun selectWeapon(index: Int) {
        if (index in weapons.indices && weapons[index].isUnlocked) {
            selectedWeaponIndex = index
            soundManager.playReload()
        }
    }

    fun manualReload() {
        val w = currentWeapon
        if (!w.isReloading && w.currentAmmo < w.maxAmmo) {
            startReload(w)
        }
    }

    private fun startReload(weapon: Weapon) {
        weapon.isReloading = true
        weapon.reloadProgress = 0f
        soundManager.playReload()
        spawnFloatingText(playerMuzzleOffset.x, playerMuzzleOffset.y - 40f, "RECARGA", Color(0xFFFFD54F))
    }

    fun onTouchShoot(touchX: Float, touchY: Float) {
        if (status != GameStatus.PLAYING) return

        val porchY = screenHeight * 0.58f
        // Only block taps if inside top-left weapon selector HUD
        if (touchX < houseWidth * 0.65f && touchY < porchY - 50f) return

        val now = System.currentTimeMillis()
        val weapon = currentWeapon

        // If auto-reloading but we have bullets, allow instant interruption to fire!
        if (weapon.isReloading) {
            if (weapon.currentAmmo > 0) {
                weapon.isReloading = false
                weapon.reloadProgress = 0f
            } else {
                return
            }
        }

        if (weapon.currentAmmo <= 0) {
            startReload(weapon)
            return
        }

        if (now - weapon.lastFireTime < weapon.cooldownMs) {
            return
        }

        // Fire!
        weapon.lastFireTime = now
        weapon.currentAmmo--

        val origin = playerMuzzleOffset
        val targetX = touchX.coerceAtLeast(barricadeX + 8f)
        val dx = (targetX - origin.x).coerceAtLeast(4f)
        val dy = touchY - origin.y
        // Expanded vertical aim angle: from -48° (-0.85 rad) upwards to +80° (+1.40 rad) downwards into water!
        val baseAngle = atan2(dy, dx).coerceIn(-0.85f, 1.40f)

        soundManager.playShotSound(weapon.type)
        muzzleFlashTimer = 0.08f
        addMuzzleFlash(origin.x, origin.y)
        triggerScreenShake(
            when (weapon.type) {
                WeaponType.LAUNCHER -> 9f
                WeaponType.SNIPER -> 8f
                WeaponType.SHOTGUN -> 5f
                else -> 3f
            }
        )

        // Projectile creation for all 8 weapons
        when (weapon.type) {
            WeaponType.PISTOL -> {
                val speed = weapon.type.projectileSpeed
                projectiles.add(
                    Projectile(
                        id = nextProjectileId++,
                        type = ProjectileType.BULLET,
                        x = origin.x,
                        y = origin.y,
                        vx = cos(baseAngle) * speed,
                        vy = sin(baseAngle) * speed,
                        damage = weapon.damage,
                        radius = 4.5f
                    )
                )
            }
            WeaponType.CROSSBOW -> {
                val speed = weapon.type.projectileSpeed
                projectiles.add(
                    Projectile(
                        id = nextProjectileId++,
                        type = ProjectileType.BOLT,
                        x = origin.x,
                        y = origin.y,
                        vx = cos(baseAngle) * speed,
                        vy = sin(baseAngle) * speed,
                        damage = weapon.damage,
                        radius = 5f,
                        pierceCount = 3
                    )
                )
            }
            WeaponType.SHOTGUN -> {
                val pelletCount = 6
                val spreadStep = 0.08f
                val startSpread = -((pelletCount - 1) / 2f) * spreadStep
                for (i in 0 until pelletCount) {
                    val angle = baseAngle + startSpread + (i * spreadStep) + (Random.nextFloat() * 0.03f - 0.015f)
                    val speed = weapon.type.projectileSpeed * (0.95f + Random.nextFloat() * 0.1f)
                    projectiles.add(
                        Projectile(
                            id = nextProjectileId++,
                            type = ProjectileType.PELLET,
                            x = origin.x,
                            y = origin.y,
                            vx = cos(angle) * speed,
                            vy = sin(angle) * speed,
                            damage = weapon.damage,
                            radius = 4f
                        )
                    )
                }
            }
            WeaponType.RIFLE -> {
                val spread = (Random.nextFloat() - 0.5f) * 0.07f
                val angle = baseAngle + spread
                val speed = weapon.type.projectileSpeed
                projectiles.add(
                    Projectile(
                        id = nextProjectileId++,
                        type = ProjectileType.BULLET,
                        x = origin.x,
                        y = origin.y,
                        vx = cos(angle) * speed,
                        vy = sin(angle) * speed,
                        damage = weapon.damage,
                        radius = 4.5f
                    )
                )
            }
            WeaponType.FLAMETHROWER -> {
                for (i in 0..1) {
                    val spread = (Random.nextFloat() - 0.5f) * 0.14f
                    val angle = baseAngle + spread
                    val speed = weapon.type.projectileSpeed * (0.85f + Random.nextFloat() * 0.3f)
                    projectiles.add(
                        Projectile(
                            id = nextProjectileId++,
                            type = ProjectileType.FLAME,
                            x = origin.x,
                            y = origin.y,
                            vx = cos(angle) * speed,
                            vy = sin(angle) * speed,
                            damage = weapon.damage,
                            radius = 8f,
                            lifetime = 0.55f,
                            pierceCount = 2
                        )
                    )
                }
            }
            WeaponType.LAUNCHER -> {
                val speed = weapon.type.projectileSpeed
                projectiles.add(
                    Projectile(
                        id = nextProjectileId++,
                        type = ProjectileType.GRENADE,
                        x = origin.x,
                        y = origin.y,
                        vx = cos(baseAngle) * speed,
                        vy = sin(baseAngle) * speed,
                        damage = weapon.damage,
                        radius = 8f,
                        isExplosive = true,
                        blastRadius = 150f
                    )
                )
            }
            WeaponType.MINIGUN -> {
                val spread = (Random.nextFloat() - 0.5f) * 0.09f
                val angle = baseAngle + spread
                val speed = weapon.type.projectileSpeed
                projectiles.add(
                    Projectile(
                        id = nextProjectileId++,
                        type = ProjectileType.BULLET,
                        x = origin.x,
                        y = origin.y,
                        vx = cos(angle) * speed,
                        vy = sin(angle) * speed,
                        damage = weapon.damage,
                        radius = 4f
                    )
                )
            }
            WeaponType.SNIPER -> {
                val speed = weapon.type.projectileSpeed
                projectiles.add(
                    Projectile(
                        id = nextProjectileId++,
                        type = ProjectileType.SNIPER_BULLET,
                        x = origin.x,
                        y = origin.y,
                        vx = cos(baseAngle) * speed,
                        vy = sin(baseAngle) * speed,
                        damage = weapon.damage,
                        radius = 6f,
                        pierceCount = 4
                    )
                )
            }
        }

        if (weapon.currentAmmo <= 0) {
            startReload(weapon)
        }
    }

    fun throwMolotov(targetX: Float = screenWidth * 0.6f, targetY: Float = screenHeight * 0.65f) {
        if (status != GameStatus.PLAYING) return
        if (defenseUpgrades.molotovCount <= 0 || molotovCooldownRemaining > 0f) return

        defenseUpgrades.molotovCount--
        molotovCooldownRemaining = molotovCooldownMax

        soundManager.playExplosion()
        fireZones.add(ActiveFireZone(x = targetX.coerceAtLeast(barricadeX + 60f), y = targetY))
        spawnExplosion(targetX, targetY, isMolotov = true)
        spawnFloatingText(targetX, targetY - 40f, "¡FUEGO!", Color(0xFFFF5722))
    }

    fun repairHouse(): Boolean {
        if (coins >= defenseUpgrades.repairHpCost && houseHp < defenseUpgrades.maxHouseHp) {
            coins -= defenseUpgrades.repairHpCost
            houseHp = (houseHp + 50f).coerceAtMost(defenseUpgrades.maxHouseHp)
            soundManager.playReload()
            spawnFloatingText(barricadeX, screenHeight * 0.5f, "+50 HP", Color(0xFF4CAF50))
            return true
        }
        return false
    }

    fun repairHouseFull(): Boolean {
        if (coins >= defenseUpgrades.fullRepairCost && houseHp < defenseUpgrades.maxHouseHp) {
            coins -= defenseUpgrades.fullRepairCost
            houseHp = defenseUpgrades.maxHouseHp
            soundManager.playReload()
            spawnFloatingText(barricadeX, screenHeight * 0.5f, "HP MAX", Color(0xFF4CAF50))
            return true
        }
        return false
    }

    fun upgradeMaxHp(): Boolean {
        val cost = defenseUpgrades.maxHpUpgradeCost
        if (coins >= cost && defenseUpgrades.maxHouseHpLevel < 10) {
            coins -= cost
            defenseUpgrades.maxHouseHpLevel++
            houseHp = (houseHp + 40f).coerceAtMost(defenseUpgrades.maxHouseHp)
            soundManager.playWaveCompleted()
            return true
        }
        return false
    }

    fun upgradeBarbedWire(): Boolean {
        val cost = defenseUpgrades.barbedWireUpgradeCost
        if (coins >= cost && defenseUpgrades.barbedWireLevel < 5) {
            coins -= cost
            defenseUpgrades.barbedWireLevel++
            soundManager.playReload()
            return true
        }
        return false
    }

    fun upgradeTurret(): Boolean {
        val cost = defenseUpgrades.turretUpgradeCost
        if (coins >= cost && defenseUpgrades.turretLevel < 5) {
            coins -= cost
            defenseUpgrades.turretLevel++
            soundManager.playWaveCompleted()
            return true
        }
        return false
    }

    fun buyMolotov(): Boolean {
        if (coins >= defenseUpgrades.molotovBuyCost && defenseUpgrades.molotovCount < 5) {
            coins -= defenseUpgrades.molotovBuyCost
            defenseUpgrades.molotovCount++
            soundManager.playReload()
            return true
        }
        return false
    }

    fun unlockWeapon(weaponType: WeaponType): Boolean {
        val weapon = weapons.find { it.type == weaponType } ?: return false
        if (!weapon.isUnlocked && coins >= weapon.type.unlockCost) {
            coins -= weapon.type.unlockCost
            weapon.isUnlocked = true
            soundManager.playWaveCompleted()
            selectedWeaponIndex = weapons.indexOf(weapon)
            spawnFloatingText(playerMuzzleOffset.x, playerMuzzleOffset.y - 45f, "¡${weapon.type.title.split(" ").first()} DESBLOQUEADA!", Color(0xFFFFD54F))
            return true
        }
        return false
    }

    fun upgradeWeapon(weaponType: WeaponType): Boolean {
        val weapon = weapons.find { it.type == weaponType } ?: return false
        if (weapon.isUnlocked && weapon.level < weapon.maxLevel && coins >= weapon.upgradeCost) {
            coins -= weapon.upgradeCost
            weapon.level++
            weapon.currentAmmo = weapon.maxAmmo
            soundManager.playWaveCompleted()
            return true
        }
        return false
    }

    var isShopOpen: Boolean by mutableStateOf(false)
        private set

    var wasBetweenWavesWhenShopOpened: Boolean by mutableStateOf(false)
        private set

    private var previousStatusBeforeShop: GameStatus = GameStatus.PLAYING

    fun openShop() {
        if (!isShopOpen) {
            wasBetweenWavesWhenShopOpened = (status == GameStatus.WAVE_CLEARED)
            previousStatusBeforeShop = status
            isShopOpen = true
            status = GameStatus.PAUSED
        }
    }

    fun closeShop() {
        if (isShopOpen) {
            isShopOpen = false
            status = if (previousStatusBeforeShop == GameStatus.PLAYING) {
                GameStatus.PLAYING
            } else if (previousStatusBeforeShop == GameStatus.WAVE_CLEARED) {
                GameStatus.WAVE_CLEARED
            } else {
                previousStatusBeforeShop
            }
        }
    }

    fun continueToNextWave() {
        if (status == GameStatus.WAVE_CLEARED || wasBetweenWavesWhenShopOpened) {
            wasBetweenWavesWhenShopOpened = false
            initWave(wave + 1)
        }
    }

    fun pauseGame() {
        if (status == GameStatus.PLAYING) {
            status = GameStatus.PAUSED
        }
    }

    fun resumeGame() {
        if (status == GameStatus.PAUSED) {
            status = GameStatus.PLAYING
        }
    }

    fun update(dt: Float) {
        if (status == GameStatus.PAUSED || status == GameStatus.GAME_OVER || isShopOpen) return

        survivedSeconds += dt

        // Molotov cooldown
        if (molotovCooldownRemaining > 0f) {
            molotovCooldownRemaining = (molotovCooldownRemaining - dt).coerceAtLeast(0f)
        }

        // Damage flash decay
        if (houseDamageFlash > 0f) {
            houseDamageFlash = (houseDamageFlash - dt).coerceAtLeast(0f)
        }

        // Muzzle flash decay
        if (muzzleFlashTimer > 0f) {
            muzzleFlashTimer = (muzzleFlashTimer - dt).coerceAtLeast(0f)
        }

        // Screen shake decay
        if (shakeIntensity > 0f) {
            shakeIntensity = (shakeIntensity - 24f * dt).coerceAtLeast(0f)
            shakeX = (Random.nextFloat() - 0.5f) * shakeIntensity
            shakeY = (Random.nextFloat() - 0.5f) * shakeIntensity
        } else {
            shakeX = 0f
            shakeY = 0f
        }

        // Auto-Reload when idle (when not shooting for 1.1s)
        val now = System.currentTimeMillis()
        val curW = currentWeapon
        if (status == GameStatus.PLAYING && !curW.isReloading && curW.currentAmmo < curW.maxAmmo) {
            if (now - curW.lastFireTime >= 1100L) {
                startReload(curW)
            }
        }

        // Weapon reload progression
        weapons.forEach { w ->
            if (w.isReloading) {
                val speedBonus = 1f + (defenseUpgrades.reloadSpeedLevel - 1) * 0.15f
                val totalReloadTime = w.reloadTimeMs / 1000f / speedBonus
                w.reloadProgress += dt / totalReloadTime
                if (w.reloadProgress >= 1f) {
                    w.isReloading = false
                    w.reloadProgress = 0f
                    w.currentAmmo = w.maxAmmo
                    soundManager.playReload()
                }
            }
        }

        // If in WAVE_CLEARED state: advance countdown and transition to next wave automatically
        if (status == GameStatus.WAVE_CLEARED) {
            waveClearCountdown -= dt
            if (waveClearCountdown <= 0f) {
                continueToNextWave()
            }
        }

        // Roof Sentry Turret auto-firing at nearest zombie
        if (status == GameStatus.PLAYING && defenseUpgrades.turretLevel > 0 && zombies.isNotEmpty()) {
            timeSinceLastTurretShot += dt
            val interval = defenseUpgrades.turretIntervalMs / 1000f
            if (timeSinceLastTurretShot >= interval) {
                timeSinceLastTurretShot = 0f
                val target = zombies.minByOrNull { it.x }
                if (target != null && target.x < screenWidth) {
                    val turretOrigin = roofTurretOffset
                    val dx = target.x - turretOrigin.x
                    val dy = target.y - turretOrigin.y
                    val angle = atan2(dy, dx)
                    projectiles.add(
                        Projectile(
                            id = nextProjectileId++,
                            type = ProjectileType.BULLET,
                            x = turretOrigin.x,
                            y = turretOrigin.y,
                            vx = cos(angle) * 1700f,
                            vy = sin(angle) * 1700f,
                            damage = defenseUpgrades.turretDamage,
                            radius = 3.5f
                        )
                    )
                    addMuzzleFlash(turretOrigin.x + 8f, turretOrigin.y)
                    soundManager.playShotSound(WeaponType.PISTOL)
                }
            }
        }

        // Spawn Wave Zombies (only while in PLAYING mode)
        if (status == GameStatus.PLAYING && zombiesToSpawnInWave > 0) {
            timeSinceLastSpawn += dt
            if (timeSinceLastSpawn >= currentSpawnInterval) {
                timeSinceLastSpawn = 0f
                spawnZombieForCurrentWave()
                zombiesToSpawnInWave--
            }
        }

        // Update Fire Zones
        val fireIterator = fireZones.iterator()
        while (fireIterator.hasNext()) {
            val fire = fireIterator.next()
            fire.remainingSeconds -= dt

            if (Random.nextFloat() < 0.35f) {
                particles.add(
                    Particle(
                        id = nextParticleId++,
                        type = ParticleType.EXPLOSION_FIRE,
                        x = fire.x + (Random.nextFloat() - 0.5f) * fire.radius,
                        y = fire.y + (Random.nextFloat() - 0.5f) * 30f,
                        vx = (Random.nextFloat() - 0.5f) * 20f,
                        vy = -50f - Random.nextFloat() * 40f,
                        alpha = 0.9f,
                        scale = 1.2f,
                        maxLife = 0.5f,
                        life = 0.5f,
                        color = Color(0xFFFF5722)
                    )
                )
            }

            zombies.forEach { z ->
                val dist = hypot(z.x - fire.x, z.y - fire.y)
                if (dist <= fire.radius) {
                    z.currentHp -= 36f * dt
                    z.slowTimer = 0.5f
                    if (Random.nextFloat() < 0.08f) {
                        spawnBloodParticles(z.x, z.y, Color(0xFFFF9800), count = 2)
                    }
                }
            }

            if (fire.remainingSeconds <= 0f) {
                fireIterator.remove()
            }
        }

        // Update Zombies
        val zombieIterator = zombies.iterator()
        while (zombieIterator.hasNext()) {
            val z = zombieIterator.next()

            if (z.hitFlashTimer > 0f) {
                z.hitFlashTimer -= dt
            }
            if (z.slowTimer > 0f) {
                z.slowTimer -= dt
            }

            if (z.isDead) {
                onZombieKilled(z)
                zombieIterator.remove()
                continue
            }

            // Spitter ranged spit
            if (z.type == ZombieType.SPITTER && z.x < screenWidth * 0.78f && z.x > barricadeX + 80f) {
                if (now - z.lastAttackTime >= z.type.attackIntervalMs) {
                    z.lastAttackTime = now
                    shootAcidSpit(z)
                }
            }

            // Movement & Barricade Collision
            val targetBarricade = barricadeX + 12f
            if (z.x > targetBarricade) {
                val slowFactor = if (z.slowTimer > 0f) 0.5f else 1.0f
                z.x -= z.type.speed * slowFactor * dt
                z.walkAnimTimer += dt * 5.2f

                if (Random.nextFloat() < 0.08f) {
                    addWaterSplash(z.x + z.type.width * 0.4f, z.y + z.type.height * 0.85f)
                }
            } else {
                // At barricade! Attack house
                z.x = targetBarricade
                z.isAttacking = true

                // Barbed wire defense damage & slow
                if (defenseUpgrades.barbedWireLevel > 0) {
                    z.currentHp -= defenseUpgrades.spikeDamagePerSec * dt
                    if (Random.nextFloat() < 0.06f) {
                        spawnBloodParticles(z.x, z.y, Color(0xFF8BC34A), count = 2)
                    }
                }

                if (now - z.lastAttackTime >= z.type.attackIntervalMs) {
                    z.lastAttackTime = now
                    damageHouse(z.type.attackDamage)
                    spawnWoodSplinters(barricadeX, z.y)
                }
            }
        }

        // Update Projectiles
        val projIterator = projectiles.iterator()
        while (projIterator.hasNext()) {
            val p = projIterator.next()
            p.lifetime -= dt
            if (p.lifetime <= 0f || p.hasHit) {
                projIterator.remove()
                continue
            }

            p.x += p.vx * dt
            p.y += p.vy * dt

            if (p.x > screenWidth + 100f || p.y < 0f || p.y > screenHeight) {
                projIterator.remove()
                continue
            }

            if (p.isEnemy) {
                if (p.x <= barricadeX) {
                    p.hasHit = true
                    damageHouse(p.damage)
                    spawnToxicSplatter(p.x, p.y)
                    projIterator.remove()
                    continue
                }
            } else {
                var hitZombie: Zombie? = null
                val prevX = p.x - p.vx * dt
                val prevY = p.y - p.vy * dt

                for (z in zombies) {
                    val zCenterX = z.x
                    val zCenterY = z.y - z.type.height * 0.5f
                    // Generous hitboxes tailored to each zombie type with projectile radius margin
                    val extraH = if (z.type == ZombieType.ZOMBIE_DOG || z.type == ZombieType.GATOR) 16f else 8f
                    val halfW = z.type.width * 0.62f + p.radius + 8f
                    val halfH = z.type.height * 0.55f + extraH + p.radius + 8f

                    val zLeft = zCenterX - halfW
                    val zRight = zCenterX + halfW
                    val zTop = zCenterY - halfH
                    val zBottom = zCenterY + halfH

                    // Continuous collision check with 4 interpolation sub-steps (prevents fast bullet tunneling!)
                    var collides = false
                    for (step in 0..4) {
                        val fraction = step / 4f
                        val testX = prevX + (p.x - prevX) * fraction
                        val testY = prevY + (p.y - prevY) * fraction
                        if (testX in zLeft..zRight && testY in zTop..zBottom) {
                            collides = true
                            break
                        }
                    }

                    if (collides) {
                        hitZombie = z
                        break
                    }
                }

                if (hitZombie != null) {
                    if (p.isExplosive) {
                        p.hasHit = true
                        soundManager.playExplosion()
                        spawnExplosion(p.x, p.y)
                        triggerScreenShake(12f)

                        zombies.forEach { z ->
                            val dist = hypot(z.x - p.x, z.y - p.y)
                            if (dist <= p.blastRadius) {
                                val falloff = (1f - dist / p.blastRadius).coerceIn(0.35f, 1f)
                                z.currentHp -= p.damage * falloff
                                z.hitFlashTimer = 0.15f
                                z.x += (35f * falloff)
                            }
                        }
                        projIterator.remove()
                    } else {
                        // Normal or piercing projectile
                        val armorFactor = when (p.type) {
                            ProjectileType.SNIPER_BULLET -> 1f // Bypasses armor completely!
                            ProjectileType.FLAME -> 1f         // Fire bypasses armor
                            ProjectileType.PELLET -> 1f
                            else -> (1f - hitZombie.type.armorDamageReduction)
                        }
                        val effectiveDmg = p.damage * armorFactor
                        hitZombie.currentHp -= effectiveDmg
                        hitZombie.hitFlashTimer = 0.12f
                        hitZombie.x += (if (p.type == ProjectileType.SNIPER_BULLET) 25f else 12f)
                        soundManager.playZombieHit()

                        if (p.type == ProjectileType.FLAME) {
                            hitZombie.slowTimer = 0.6f
                        }

                        val isCrit = Random.nextFloat() < 0.15f
                        if (isCrit) {
                            hitZombie.currentHp -= p.damage * 0.5f
                            spawnFloatingText(hitZombie.x, hitZombie.y - 36f, "¡CRÍTICO!", Color(0xFFFF5252))
                        }

                        spawnBloodParticles(
                            p.x,
                            p.y,
                            when (hitZombie.type) {
                                ZombieType.SPITTER -> Color(0xFF64DD17)
                                ZombieType.COLOSSUS -> Color(0xFFFF1744)
                                else -> Color(0xFFB71C1C)
                            }
                        )

                        // Piercing deduction
                        p.pierceCount--
                        if (p.pierceCount <= 0) {
                            p.hasHit = true
                            projIterator.remove()
                        }
                    }
                }
            }
        }

        // Update Particles
        val partIterator = particles.iterator()
        while (partIterator.hasNext()) {
            val part = partIterator.next()
            part.life -= dt
            if (part.isDead) {
                partIterator.remove()
                continue
            }
            part.x += part.vx * dt
            part.y += part.vy * dt
            part.alpha = (part.life / part.maxLife).coerceIn(0f, 1f)
        }

        // Check Wave Completion
        if (status == GameStatus.PLAYING && zombiesToSpawnInWave == 0 && zombies.isEmpty() && houseHp > 0f) {
            status = GameStatus.WAVE_CLEARED
            waveClearCountdown = waveClearDuration
            val waveBonus = 12 + (wave * 6)
            coins += waveBonus
            score += waveBonus * 15
            soundManager.playWaveCompleted()
            spawnFloatingText(screenWidth * 0.5f, screenHeight * 0.45f, "+$$waveBonus BONUS", Color(0xFFFFD700))
        }
    }

    private fun spawnZombieForCurrentWave() {
        val waterTop = screenHeight * 0.48f
        val waterBottom = screenHeight * 0.94f
        val spawnY = waterTop + Random.nextFloat() * (waterBottom - waterTop)

        val type = when {
            wave == 1 -> if (Random.nextFloat() < 0.25f) ZombieType.RUNNER else ZombieType.WALKER
            wave == 2 -> {
                val r = Random.nextFloat()
                when {
                    r < 0.20f -> ZombieType.ZOMBIE_DOG
                    r < 0.50f -> ZombieType.RUNNER
                    else -> ZombieType.WALKER
                }
            }
            wave == 3 -> {
                val r = Random.nextFloat()
                when {
                    r < 0.15f -> ZombieType.SPITTER
                    r < 0.40f -> ZombieType.ZOMBIE_DOG
                    r < 0.65f -> ZombieType.RUNNER
                    else -> ZombieType.WALKER
                }
            }
            wave == 4 -> {
                val r = Random.nextFloat()
                when {
                    r < 0.15f -> ZombieType.ARMORED
                    r < 0.35f -> ZombieType.SPITTER
                    r < 0.55f -> ZombieType.ZOMBIE_DOG
                    r < 0.75f -> ZombieType.RUNNER
                    else -> ZombieType.WALKER
                }
            }
            wave % 5 == 0 && zombiesToSpawnInWave == 1 -> {
                ZombieType.COLOSSUS
            }
            wave in 5..8 -> {
                val r = Random.nextFloat()
                when {
                    r < 0.15f -> ZombieType.ARMORED
                    r < 0.30f -> ZombieType.GATOR
                    r < 0.45f -> ZombieType.SPITTER
                    r < 0.65f -> ZombieType.ZOMBIE_DOG
                    r < 0.82f -> ZombieType.RUNNER
                    else -> ZombieType.WALKER
                }
            }
            else -> {
                val r = Random.nextFloat()
                when {
                    r < 0.12f -> ZombieType.TANK
                    r < 0.25f -> ZombieType.ARMORED
                    r < 0.38f -> ZombieType.GATOR
                    r < 0.52f -> ZombieType.SPITTER
                    r < 0.68f -> ZombieType.ZOMBIE_DOG
                    r < 0.84f -> ZombieType.RUNNER
                    else -> ZombieType.WALKER
                }
            }
        }

        val hpMultiplier = 1f + (wave - 1) * 0.045f
        val scaledHp = type.maxHp * hpMultiplier

        zombies.add(
            Zombie(
                id = nextZombieId++,
                type = type,
                x = screenWidth + Random.nextFloat() * 70f + 30f,
                y = spawnY,
                currentHp = scaledHp,
                maxHp = scaledHp
            )
        )
    }

    private fun onZombieKilled(z: Zombie) {
        zombiesKilled++
        val reward = z.type.coinReward
        coins += reward
        score += reward * 15

        soundManager.playZombieDeath()
        soundManager.playCoinPickup()

        spawnGoreBurst(
            z.x,
            z.y,
            when (z.type) {
                ZombieType.SPITTER -> Color(0xFF64DD17)
                ZombieType.COLOSSUS -> Color(0xFFFF1744)
                else -> Color(0xFFC2185B)
            }
        )
        spawnFloatingText(z.x, z.y - 30f, "+$$reward", Color(0xFFFFD700))
    }

    private fun damageHouse(damage: Float) {
        houseHp = (houseHp - damage).coerceAtLeast(0f)
        houseDamageFlash = 0.35f
        soundManager.playHouseDamage()
        triggerScreenShake(8f)

        if (houseHp <= 0f) {
            status = GameStatus.GAME_OVER
            soundManager.playGameOver()
            onGameOver(survivedSeconds.toInt(), wave, zombiesKilled, coins)
        }
    }

    private fun shootAcidSpit(spitter: Zombie) {
        val speed = 370f
        val targetX = barricadeX
        val targetY = spitter.y
        val angle = atan2(targetY - spitter.y, targetX - spitter.x)

        projectiles.add(
            Projectile(
                id = nextProjectileId++,
                type = ProjectileType.ACID_SPIT,
                x = spitter.x - 20f,
                y = spitter.y - 20f,
                vx = cos(angle) * speed,
                vy = sin(angle) * speed,
                damage = spitter.type.attackDamage,
                radius = 6f,
                isEnemy = true
            )
        )
    }

    private fun triggerScreenShake(intensity: Float) {
        shakeIntensity = max(shakeIntensity, intensity)
    }

    private fun addMuzzleFlash(x: Float, y: Float) {
        particles.add(
            Particle(
                id = nextParticleId++,
                type = ParticleType.MUZZLE_FLASH,
                x = x + 15f,
                y = y,
                vx = 0f,
                vy = 0f,
                alpha = 1f,
                scale = 1.4f,
                maxLife = 0.08f,
                life = 0.08f,
                color = Color(0xFFFFEB3B),
                radius = 12f
            )
        )
    }

    private fun addWaterSplash(x: Float, y: Float) {
        for (i in 0..2) {
            particles.add(
                Particle(
                    id = nextParticleId++,
                    type = ParticleType.WATER_SPLASH,
                    x = x + (Random.nextFloat() - 0.5f) * 16f,
                    y = y,
                    vx = (Random.nextFloat() - 0.5f) * 35f,
                    vy = -20f - Random.nextFloat() * 25f,
                    alpha = 0.7f,
                    maxLife = 0.35f,
                    life = 0.35f,
                    color = Color(0xFF80CBC4),
                    radius = 3.5f
                )
            )
        }
    }

    private fun spawnBloodParticles(x: Float, y: Float, color: Color, count: Int = 4) {
        for (i in 0 until count) {
            particles.add(
                Particle(
                    id = nextParticleId++,
                    type = ParticleType.BLOOD,
                    x = x,
                    y = y,
                    vx = (Random.nextFloat() - 0.5f) * 110f,
                    vy = (Random.nextFloat() - 0.5f) * 90f,
                    alpha = 0.85f,
                    maxLife = 0.35f,
                    life = 0.35f,
                    color = color,
                    radius = 3f + Random.nextFloat() * 3f
                )
            )
        }
    }

    private fun spawnGoreBurst(x: Float, y: Float, color: Color) {
        for (i in 0..8) {
            particles.add(
                Particle(
                    id = nextParticleId++,
                    type = ParticleType.BLOOD,
                    x = x,
                    y = y,
                    vx = (Random.nextFloat() - 0.5f) * 180f,
                    vy = -60f - Random.nextFloat() * 100f,
                    alpha = 0.9f,
                    maxLife = 0.55f,
                    life = 0.55f,
                    color = color,
                    radius = 4f + Random.nextFloat() * 4f
                )
            )
        }
    }

    private fun spawnExplosion(x: Float, y: Float, isMolotov: Boolean = false) {
        for (i in 0..12) {
            val angle = Random.nextFloat() * (2 * PI.toFloat())
            val spd = 50f + Random.nextFloat() * 150f
            particles.add(
                Particle(
                    id = nextParticleId++,
                    type = ParticleType.EXPLOSION_FIRE,
                    x = x,
                    y = y,
                    vx = cos(angle) * spd,
                    vy = sin(angle) * spd,
                    alpha = 1f,
                    scale = 1.5f,
                    maxLife = 0.45f,
                    life = 0.45f,
                    color = if (isMolotov) Color(0xFFFF5722) else Color(0xFFFF9800),
                    radius = 8f + Random.nextFloat() * 10f
                )
            )
        }
    }

    private fun spawnWoodSplinters(x: Float, y: Float) {
        for (i in 0..4) {
            particles.add(
                Particle(
                    id = nextParticleId++,
                    type = ParticleType.SMOKE,
                    x = x,
                    y = y,
                    vx = -20f - Random.nextFloat() * 60f,
                    vy = (Random.nextFloat() - 0.5f) * 80f,
                    alpha = 0.8f,
                    maxLife = 0.4f,
                    life = 0.4f,
                    color = Color(0xFF8D6E63),
                    radius = 4f
                )
            )
        }
    }

    private fun spawnToxicSplatter(x: Float, y: Float) {
        for (i in 0..5) {
            particles.add(
                Particle(
                    id = nextParticleId++,
                    type = ParticleType.TOXIC_SLIME,
                    x = x,
                    y = y,
                    vx = (Random.nextFloat() - 0.5f) * 70f,
                    vy = (Random.nextFloat() - 0.5f) * 70f,
                    alpha = 0.85f,
                    maxLife = 0.4f,
                    life = 0.4f,
                    color = Color(0xFF76FF03),
                    radius = 5f
                )
            )
        }
    }

    private fun spawnFloatingText(x: Float, y: Float, text: String, color: Color) {
        particles.add(
            Particle(
                id = nextParticleId++,
                type = ParticleType.FLOATING_TEXT,
                x = x,
                y = y,
                vx = 0f,
                vy = -45f,
                alpha = 1f,
                maxLife = 0.85f,
                life = 0.85f,
                color = color,
                text = text
            )
        )
    }
}
