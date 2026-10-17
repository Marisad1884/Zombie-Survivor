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
    PAUSED,
    GAME_OVER
}

/** What the girl who brings ammo is doing. She lives behind the door. */
enum class RunnerState { IDLE, FETCHING, BRINGING, HANDING, HOMING }

/**
 * Hallway shooter. Zombies shuffle along one line from the left; if one reaches the heroine she is caught
 * and the run is over, instantly. There is no money and no base health: only bullets, which are scarce.
 *
 * Everything is measured in world units (the screen is 340 units tall, see Balance).
 */
class GameEngine(
    private val soundManager: SoundManager,
    private val onGameOver: (survivedSeconds: Int, wave: Int, kills: Int, score: Int) -> Unit
) {
    // ---------------------------------------------------------------- observable state (read by the HUD)
    var status: GameStatus by mutableStateOf(GameStatus.PLAYING)
        internal set
    var score: Int by mutableIntStateOf(0)
        internal set
    var zombiesKilled: Int by mutableIntStateOf(0)
        internal set
    var headshots: Int by mutableIntStateOf(0)
        internal set
    var survivedSeconds: Float by mutableFloatStateOf(0f)
        internal set
    var selectedWeaponIndex: Int by mutableIntStateOf(0)
        internal set

    val wave: Int get() = 1 + zombiesKilled / Balance.KILLS_PER_WAVE

    val weapons: List<Weapon> = WeaponType.values().map { Weapon(it) }
    val currentWeapon: Weapon get() = weapons[selectedWeaponIndex]

    // ---------------------------------------------------------------- the girl who brings ammo
    var runnerState: RunnerState by mutableStateOf(RunnerState.IDLE)
        internal set
    var runnerCooldown: Float by mutableFloatStateOf(0f)
        internal set
    /** 0..1 over the whole trip, for the button's progress ring. */
    var runnerTripProgress: Float by mutableFloatStateOf(0f)
        internal set
    var runnerX: Float = 0f
        private set
    /** +1 faces right, -1 faces left. */
    var runnerFacing: Int = -1
        private set
    var runnerWalkPhase: Float = 0f
        private set
    var runnerCarrying: Boolean = false
        private set
    var runnerDelivered: Boolean = false
        private set
    var runnerTargetType: WeaponType = WeaponType.REVOLVER
        private set
    private var runnerTimer = 0f

    /** She is inside the house while she fetches the ammo. */
    val runnerHidden: Boolean get() = runnerState == RunnerState.FETCHING

    // ---------------------------------------------------------------- world, effects
    val zombies = mutableListOf<Zombie>()
    val particles = mutableListOf<Particle>()
    val tracers = mutableListOf<Tracer>()
    val grenades = mutableListOf<ThrownGrenade>()

    var worldWidth: Float = 600f
        private set
    val floorY: Float = Balance.FLOOR_Y
    val heroX: Float get() = worldWidth - Balance.HERO_FROM_RIGHT
    val doorCx: Float get() = worldWidth - Balance.DOOR_FROM_RIGHT
    val runnerDeliverX: Float get() = heroX + 60f
    /** The shotgun cannot hit anything left of this line (the middle of the map). */
    val shotgunLimitX: Float get() = worldWidth * (1f - Balance.SHOTGUN_RANGE_FRACTION)

    /** Pivot of the arm and gun: her front shoulder. */
    val shoulder: Offset
        get() = Offset(
            heroX + Balance.PX,
            floorY - HeroGeometry.GROUND_ROW * Balance.PX + HeroGeometry.SHOULDER_ROW * Balance.PX
        )

    /** Aim angle in radians; positive aims downward. She always faces left. */
    var aimAngle: Float = 0f
        private set
    var muzzleFlashTimer: Float = 0f
        private set
    var throwTimer: Float = 0f
        private set
    var shakeAmount: Float = 0f
        private set
    var shakeX: Float = 0f
        private set
    var shakeY: Float = 0f
        private set
    /** 1 right after she is caught, fades out. */
    var caughtFlash: Float = 0f
        private set
    /** Seconds since she was caught. */
    var gameOverTimer: Float = 0f
        private set

    private var clock = 0f
    private var spawnTimer = 0f
    private var nextId = 1L
    private var lastWarningAt = -10f

    init {
        startNewRun()
    }

    fun setWorldSize(widthPx: Float, heightPx: Float) {
        if (widthPx > 0f && heightPx > 0f) {
            worldWidth = Balance.WORLD_H * widthPx / heightPx
            if (runnerState == RunnerState.IDLE) runnerX = doorCx
        }
    }

    // ---------------------------------------------------------------- run control
    fun startNewRun() {
        status = GameStatus.PLAYING
        score = 0
        zombiesKilled = 0
        headshots = 0
        survivedSeconds = 0f
        selectedWeaponIndex = 0
        weapons.forEach { it.reset() }
        zombies.clear()
        particles.clear()
        tracers.clear()
        grenades.clear()
        clock = 0f
        spawnTimer = Balance.FIRST_SPAWN_DELAY
        aimAngle = 0f
        muzzleFlashTimer = 0f
        throwTimer = 0f
        shakeAmount = 0f
        shakeX = 0f
        shakeY = 0f
        caughtFlash = 0f
        gameOverTimer = 0f
        runnerState = RunnerState.IDLE
        runnerCooldown = 0f
        runnerTripProgress = 0f
        runnerCarrying = false
        runnerDelivered = false
        runnerFacing = -1
        runnerWalkPhase = 0f
        runnerTimer = 0f
        runnerX = doorCx
    }

    fun pauseGame() {
        if (status == GameStatus.PLAYING) status = GameStatus.PAUSED
    }

    fun resumeGame() {
        if (status == GameStatus.PAUSED) status = GameStatus.PLAYING
    }

    // ---------------------------------------------------------------- weapons
    fun selectWeapon(index: Int) {
        if (status != GameStatus.PLAYING || index !in weapons.indices || index == selectedWeaponIndex) return
        currentWeapon.cancelReload()
        selectedWeaponIndex = index
        soundManager.playReload()
    }

    /** One press loads ONE round. Filling the gun takes several presses. Returns true if a reload began. */
    fun reloadOne(): Boolean {
        if (status != GameStatus.PLAYING) return false
        val w = currentWeapon
        if (w.isReloading || w.currentAmmo >= w.magazine) return false
        if (w.reserveAmmo <= 0) {
            warn("¡SIN BALAS!")
            return false
        }
        w.isReloading = true
        w.reloadProgress = 0f
        return true
    }

    /**
     * Points the weapon at a world position without firing. The barrel tip moves as the arm swings around
     * her shoulder, so instead of a simple atan2 the angle is searched until the shot really passes through
     * the finger (this also behaves for targets very close to her).
     */
    fun aimAt(wx: Float, wy: Float) {
        // Height at which a shot fired with this angle crosses x = wx, minus the height that was asked for
        fun miss(a: Float): Float {
            val o = muzzle(a)
            return o.y + tan(a) * max(o.x - wx, 30f) - wy
        }
        var lo = -MAX_AIM
        var hi = MAX_AIM
        aimAngle = when {
            miss(lo) >= 0f -> lo
            miss(hi) <= 0f -> hi
            else -> {
                repeat(16) {
                    val mid = (lo + hi) / 2f
                    if (miss(mid) > 0f) hi = mid else lo = mid
                }
                (lo + hi) / 2f
            }
        }
    }

    /** Aims at (wx, wy) and fires the selected weapon if it can. Coordinates are in world units. */
    fun onTouchShoot(wx: Float, wy: Float) {
        if (status != GameStatus.PLAYING) return
        aimAt(wx, wy)

        val w = currentWeapon
        if (w.cooldownLeft > 0f) return
        if (w.isReloading) {
            if (w.currentAmmo > 0) w.cancelReload() else return
        }
        if (w.currentAmmo <= 0) {
            warn(if (w.reserveAmmo > 0) "¡RECARGA!" else "¡SIN BALAS!")
            w.cooldownLeft = 0.25f
            return
        }

        when (w.type) {
            WeaponType.REVOLVER -> fireRevolver(w)
            WeaponType.SHOTGUN -> fireShotgun(w)
            WeaponType.GRENADE -> throwGrenade(w, wx)
        }
    }

    /** Where the barrel tip is for a given aim angle. */
    fun muzzle(angle: Float = aimAngle, type: WeaponType = currentWeapon.type): Offset {
        val off = HeroGeometry.muzzleOffset(type)
        val phi = -angle
        return Offset(
            shoulder.x + off.x * cos(phi) - off.y * sin(phi),
            shoulder.y + off.x * sin(phi) + off.y * cos(phi)
        )
    }

    private class RayHit(val zombie: Zombie, val x: Float, val y: Float, val dist: Float, val head: Boolean)

    /** First living zombie a ray going left from (ox, oy) with slope angle `angle` touches, not past limitX. */
    private fun traceRay(ox: Float, oy: Float, angle: Float, limitX: Float): RayHit? {
        val dx = -cos(angle)
        val dy = sin(angle)
        var best: RayHit? = null
        for (z in zombies) {
            if (!z.isAlive) continue
            val hx = z.x + 10f * z.type.scale
            if (hx > ox || hx < limitX) continue
            val t = (hx - ox) / dx
            if (t < 0f) continue
            val y = oy + dy * t
            val top = floorY - z.height
            if (y < top || y > floorY) continue
            if (best == null || t < best.dist) {
                best = RayHit(z, hx, y, t, y < top + z.height * Balance.HEAD_FRACTION)
            }
        }
        return best
    }

    private fun fireRevolver(w: Weapon) {
        w.currentAmmo -= 1
        w.cooldownLeft = w.type.fireCooldown
        val o = muzzle()
        muzzleFlashTimer = MUZZLE_FLASH_TIME
        soundManager.playShotSound(w.type)

        val hit = traceRay(o.x, o.y, aimAngle, -50f)
        val endX = hit?.x ?: -50f
        val endY = hit?.y ?: (o.y + tan(aimAngle) * (o.x - endX))
        tracers.add(Tracer(o.x, o.y, endX, endY))
        if (hit != null) {
            damage(hit.zombie, Balance.REVOLVER_DAMAGE * (if (hit.head) Balance.HEADSHOT_MULTIPLIER else 1f), hit.head, hit.x, hit.y)
        }
        sparks(o.x - 3f, o.y, 4)
        addShake(1.2f)
    }

    private fun fireShotgun(w: Weapon) {
        w.currentAmmo -= 1
        w.cooldownLeft = w.type.fireCooldown
        val o = muzzle()
        muzzleFlashTimer = MUZZLE_FLASH_TIME * 1.6f
        soundManager.playShotSound(w.type)

        val perPellet = Balance.SHOTGUN_DAMAGE_PER_SHELL / Balance.SHOTGUN_PELLETS
        val limit = shotgunLimitX
        repeat(Balance.SHOTGUN_PELLETS) {
            val a = aimAngle + (Random.nextFloat() + Random.nextFloat() - 1f) * Balance.SHOTGUN_SPREAD * 1.5f
            val hit = traceRay(o.x, o.y, a, limit)
            val endX = hit?.x ?: limit
            val endY = hit?.y ?: (o.y + tan(a) * (o.x - endX))
            tracers.add(Tracer(o.x, o.y, endX, endY, life = 0.05f, color = Color(0xFFFFD27A)))
            if (hit != null) {
                hit.zombie.stagger = 0.45f
                damage(hit.zombie, perPellet * (if (hit.head) Balance.HEADSHOT_MULTIPLIER else 1f), hit.head, hit.x, hit.y)
            }
        }
        smoke(o.x - 6f, o.y, 5)
        sparks(o.x - 3f, o.y, 8)
        // spent shell pops out
        particles.add(
            Particle(ParticleKind.CASING, shoulder.x - 20f, shoulder.y - 4f, 40f + Random.nextFloat() * 20f, -70f, 0.9f,
                Color(0xFFC62828), size = 4f, gravity = 380f)
        )
        addShake(3.5f)
    }

    private fun throwGrenade(w: Weapon, wx: Float) {
        w.currentAmmo -= 1
        w.cooldownLeft = w.type.fireCooldown
        throwTimer = 0.3f
        val from = muzzle(0f, WeaponType.GRENADE)
        val targetX = wx.coerceIn(20f, heroX - Balance.GRENADE_MAX_DISTANCE_FROM_HERO)
        val dist = abs(from.x - targetX)
        val flight = (0.55f + dist / 700f).coerceIn(0.6f, 1.15f)
        grenades.add(ThrownGrenade(from.x, from.y, targetX, floorY - 6f, flight))
        soundManager.playShotSound(w.type)
    }

    private fun explode(x: Float, y: Float) {
        soundManager.playExplosion()
        addShake(9f)
        for (z in zombies.toList()) {
            if (!z.isAlive) continue
            if (abs(z.x + 4f * z.type.scale - x) <= Balance.GRENADE_RADIUS + 8f * z.type.scale) {
                damage(z, Balance.GRENADE_DAMAGE, false, z.x, floorY - z.height * 0.5f)
            }
        }
        repeat(34) {
            val a = Random.nextFloat() * (2f * PI.toFloat())
            val sp = 40f + Random.nextFloat() * 170f
            particles.add(
                Particle(
                    ParticleKind.FIRE, x + (Random.nextFloat() - 0.5f) * 30f, y - 6f,
                    cos(a) * sp, sin(a) * sp * 0.6f - 50f, 0.45f + Random.nextFloat() * 0.35f,
                    if (Random.nextBoolean()) Color(0xFFFFB300) else Color(0xFFFF5722),
                    size = 4f + Random.nextFloat() * 5f
                )
            )
        }
        smoke(x, y - 10f, 12)
        sparks(x, y - 10f, 14)
        spawnText(x, y - 40f, "¡BOOM!", Color(0xFFFFEB3B), 0.9f)
    }

    // ---------------------------------------------------------------- damage and kills
    private fun damage(z: Zombie, amount: Float, head: Boolean, hx: Float, hy: Float) {
        if (!z.isAlive) return
        z.hp -= amount
        z.hitFlash = 0.09f
        blood(hx, hy, if (head) 9 else 5)
        if (z.hp <= 0f) {
            kill(z, head)
        } else {
            soundManager.playZombieHit()
            if (head) spawnText(hx, hy - 10f, "¡CABEZA!", Color(0xFFFF5252), 0.7f)
        }
    }

    private fun kill(z: Zombie, head: Boolean) {
        z.dying = true
        z.deathTimer = 0f
        z.headshotDeath = head
        zombiesKilled += 1
        var points = z.type.score
        if (head) {
            points += Balance.SCORE_PER_KILL_HEADSHOT_BONUS
            headshots += 1
        }
        score += points
        soundManager.playZombieDeath()
        blood(z.x + 6f, floorY - z.height * 0.7f, if (head) 16 else 10)
        spawnText(z.x, floorY - z.height - 6f, if (head) "¡CABEZA! +$points" else "+$points", if (head) Color(0xFFFF5252) else Color(0xFFFFF59D), 0.9f)
        if (zombiesKilled % Balance.KILLS_PER_WAVE == 0) {
            soundManager.playWaveCompleted()
            spawnText(worldWidth * 0.5f, 110f, "OLEADA $wave", Color(0xFFFFFFFF), 1.6f)
        }
    }

    private fun caught(z: Zombie) {
        status = GameStatus.GAME_OVER
        gameOverTimer = 0f
        caughtFlash = 1f
        addShake(10f)
        blood(heroX, floorY - 50f, 22)
        soundManager.playHouseDamage()
        soundManager.playGameOver()
        onGameOver(survivedSeconds.toInt(), wave, zombiesKilled, score)
    }

    // ---------------------------------------------------------------- the ammo girl
    /** Sends her to fetch ammo for the weapon that is equipped right now. Returns true if she set off. */
    fun callAmmoRunner(): Boolean {
        if (status != GameStatus.PLAYING) return false
        if (runnerState != RunnerState.IDLE || runnerCooldown > 0f) return false
        val w = currentWeapon
        if (w.reserveSpace <= 0) {
            spawnText(doorCx, floorY - 112f, "Reserva llena", Color(0xFF9CCC65), 1.0f)
            return false
        }
        runnerTargetType = w.type
        runnerState = RunnerState.FETCHING
        runnerTimer = Balance.RUNNER_FETCH_SECONDS
        runnerTripProgress = 0f
        runnerCarrying = false
        runnerDelivered = false
        spawnText(doorCx, floorY - 112f, "¡Voy por ${ammoWord(w.type)}!", Color(0xFF80CBC4), 1.1f)
        return true
    }

    private fun ammoWord(type: WeaponType) = when (type) {
        WeaponType.REVOLVER -> "balas"
        WeaponType.SHOTGUN -> "cartuchos"
        WeaponType.GRENADE -> "granadas"
    }

    private fun updateRunner(dt: Float) {
        if (runnerCooldown > 0f) runnerCooldown = (runnerCooldown - dt).coerceAtLeast(0f)
        val home = doorCx
        val deliver = runnerDeliverX
        val speed = Balance.RUNNER_SPEED

        when (runnerState) {
            RunnerState.IDLE -> {
                runnerX = home
                runnerFacing = -1
            }

            RunnerState.FETCHING -> {
                runnerX = home
                runnerTimer -= dt
                runnerTripProgress = 0.6f * (1f - (runnerTimer / Balance.RUNNER_FETCH_SECONDS).coerceIn(0f, 1f))
                if (runnerTimer <= 0f) {
                    runnerCarrying = true
                    runnerFacing = -1
                    runnerState = RunnerState.BRINGING
                }
            }

            RunnerState.BRINGING -> {
                runnerFacing = -1
                runnerX = max(deliver, runnerX - speed * dt)
                runnerWalkPhase += dt * 9f
                runnerTripProgress = 0.6f + 0.25f * ((home - runnerX) / (home - deliver)).coerceIn(0f, 1f)
                if (runnerX <= deliver) {
                    runnerState = RunnerState.HANDING
                    runnerTimer = Balance.RUNNER_HAND_SECONDS
                }
            }

            RunnerState.HANDING -> {
                runnerFacing = -1
                runnerTimer -= dt
                runnerTripProgress = 0.85f + 0.15f * (1f - (runnerTimer / Balance.RUNNER_HAND_SECONDS).coerceIn(0f, 1f))
                if (!runnerDelivered && runnerTimer <= Balance.RUNNER_HAND_SECONDS * 0.5f) deliverRunnerAmmo()
                if (runnerTimer <= 0f) {
                    runnerCarrying = false
                    runnerState = RunnerState.HOMING
                }
            }

            RunnerState.HOMING -> {
                runnerFacing = 1
                runnerX = min(home, runnerX + speed * dt)
                runnerWalkPhase += dt * 9f
                runnerTripProgress = 1f
                if (runnerX >= home) runnerState = RunnerState.IDLE
            }
        }
    }

    /** She brings ammo ONLY for the weapon that was equipped when the button was pressed. */
    private fun deliverRunnerAmmo() {
        runnerDelivered = true
        runnerCooldown = Balance.RUNNER_COOLDOWN_SECONDS
        val w = weapons.first { it.type == runnerTargetType }
        val added = w.addReserve(w.type.delivery)
        soundManager.playReload()
        spawnText(
            runnerDeliverX, floorY - 112f,
            if (added > 0) "+$added ${ammoWord(w.type)}" else "Reserva llena",
            Color(0xFF9CCC65), 1.3f
        )
    }

    // ---------------------------------------------------------------- the main loop
    fun update(dt: Float) {
        if (status == GameStatus.PAUSED) return

        if (status == GameStatus.GAME_OVER) {
            gameOverTimer += dt
            caughtFlash = (caughtFlash - dt * 0.6f).coerceAtLeast(0f)
            updateEffects(dt)
            return
        }

        survivedSeconds += dt
        clock += dt

        // weapons: cooldowns and the one-round-at-a-time reload
        for (w in weapons) {
            if (w.cooldownLeft > 0f) w.cooldownLeft = (w.cooldownLeft - dt).coerceAtLeast(0f)
            if (w.isReloading) {
                w.reloadProgress += dt / w.type.reloadSeconds
                if (w.reloadProgress >= 1f) {
                    w.cancelReload()
                    if (w.loadOne()) soundManager.playReload()
                }
            }
        }

        updateRunner(dt)
        updateSpawning(dt)
        updateZombies(dt)
        updateGrenades(dt)
        updateEffects(dt)
    }

    private fun updateSpawning(dt: Float) {
        spawnTimer -= dt
        if (spawnTimer > 0f) return
        // do not spawn on top of another zombie
        if (zombies.any { it.isAlive && it.x < Balance.SPAWN_X + 70f }) return

        val w = wave
        val roll = Random.nextFloat()
        val runnerChance = if (w >= Balance.RUNNER_FROM_WAVE) min(0.35f, 0.06f * (w - 1)) else 0f
        val bruteChance = if (w >= Balance.BRUTE_FROM_WAVE) min(0.22f, 0.04f * (w - 2)) else 0f
        val type = when {
            roll < bruteChance -> ZombieType.BRUTE
            roll < bruteChance + runnerChance -> ZombieType.RUNNER
            else -> ZombieType.NORMAL
        }
        val speedMult = min(Balance.SPEED_MULT_CAP, 1f + Balance.SPEED_GROWTH_PER_WAVE * (w - 1))
        zombies.add(Zombie(nextId++, type, Balance.SPAWN_X, speedMult))

        val interval = max(Balance.SPAWN_INTERVAL_MIN, Balance.SPAWN_INTERVAL_START - Balance.SPAWN_INTERVAL_STEP * (w - 1))
        spawnTimer = interval * (0.8f + Random.nextFloat() * 0.45f)
    }

    private fun updateZombies(dt: Float) {
        val iterator = zombies.iterator()
        while (iterator.hasNext()) {
            val z = iterator.next()
            if (z.hitFlash > 0f) z.hitFlash = (z.hitFlash - dt).coerceAtLeast(0f)
            if (z.dying) {
                z.deathTimer += dt
                if (z.deathTimer > DEATH_TIME) iterator.remove()
                continue
            }
            if (z.stagger > 0f) z.stagger = (z.stagger - dt).coerceAtLeast(0f)
            val speed = z.type.speed * z.speedMult * (if (z.stagger > 0f) 0.12f else 1f)
            z.x += speed * dt
            z.walkTimer += dt * speed * 0.11f
        }
        // anyone who reaches her catches her
        val catcher = zombies.firstOrNull { it.isAlive && it.frontX >= heroX - Balance.CATCH_MARGIN }
        if (catcher != null) caught(catcher)
    }

    private fun updateGrenades(dt: Float) {
        val iterator = grenades.iterator()
        val landed = mutableListOf<ThrownGrenade>()
        while (iterator.hasNext()) {
            val g = iterator.next()
            g.t += dt
            if (g.t >= g.flightTime) {
                landed.add(g)
                iterator.remove()
            }
        }
        landed.forEach { explode(it.x1, it.y1) }
    }

    private fun updateEffects(dt: Float) {
        if (muzzleFlashTimer > 0f) muzzleFlashTimer = (muzzleFlashTimer - dt).coerceAtLeast(0f)
        if (throwTimer > 0f) throwTimer = (throwTimer - dt).coerceAtLeast(0f)

        // dead zombies keep animating during game over
        if (status == GameStatus.GAME_OVER) {
            zombies.forEach { if (it.dying) it.deathTimer += dt }
            zombies.removeAll { it.dying && it.deathTimer > DEATH_TIME }
        }

        val ti = tracers.iterator()
        while (ti.hasNext()) {
            val t = ti.next()
            t.life -= dt
            if (t.life <= 0f) ti.remove()
        }

        val pi = particles.iterator()
        while (pi.hasNext()) {
            val p = pi.next()
            p.life -= dt
            if (p.isDead) {
                pi.remove()
                continue
            }
            p.vy += p.gravity * dt
            p.x += p.vx * dt
            p.y += p.vy * dt
            if (p.kind == ParticleKind.BLOOD && p.y >= floorY - 1f) {
                p.y = floorY - 1f
                p.vx *= 0.4f
                p.vy = 0f
            }
        }

        if (shakeAmount > 0.05f) {
            shakeX = (Random.nextFloat() - 0.5f) * shakeAmount
            shakeY = (Random.nextFloat() - 0.5f) * shakeAmount
            shakeAmount *= 0.86f
        } else {
            shakeAmount = 0f
            shakeX = 0f
            shakeY = 0f
        }
    }

    // ---------------------------------------------------------------- little effect helpers
    private fun addShake(amount: Float) {
        shakeAmount = max(shakeAmount, amount)
    }

    private fun warn(text: String) {
        if (clock - lastWarningAt < 0.9f) return
        lastWarningAt = clock
        spawnText(heroX - 30f, floorY - 130f, text, Color(0xFFFF5252), 0.8f)
    }

    fun spawnText(x: Float, y: Float, text: String, color: Color, life: Float = 1f) {
        particles.add(Particle(ParticleKind.TEXT, x, y, 0f, -22f, life, color, size = 9f, text = text))
    }

    private fun blood(x: Float, y: Float, count: Int) {
        repeat(count) {
            val sp = 30f + Random.nextFloat() * 120f
            val a = (Random.nextFloat() - 0.5f) * 2.4f
            particles.add(
                Particle(
                    ParticleKind.BLOOD, x, y, -cos(a) * sp, -abs(sin(a)) * sp - 20f, 0.5f + Random.nextFloat() * 0.5f,
                    if (Random.nextInt(3) == 0) Color(0xFF8E0E0E) else Color(0xFFC21818),
                    size = 2f + Random.nextInt(2) * 2f, gravity = 380f
                )
            )
        }
    }

    private fun sparks(x: Float, y: Float, count: Int) {
        repeat(count) {
            val a = (Random.nextFloat() - 0.5f) * 1.6f
            val sp = 40f + Random.nextFloat() * 120f
            particles.add(
                Particle(ParticleKind.SPARK, x, y, -cos(a) * sp, sin(a) * sp, 0.12f + Random.nextFloat() * 0.12f,
                    Color(0xFFFFE082), size = 2f)
            )
        }
    }

    private fun smoke(x: Float, y: Float, count: Int) {
        repeat(count) {
            particles.add(
                Particle(
                    ParticleKind.SMOKE, x + (Random.nextFloat() - 0.5f) * 12f, y + (Random.nextFloat() - 0.5f) * 8f,
                    -10f - Random.nextFloat() * 25f, -12f - Random.nextFloat() * 14f, 0.6f + Random.nextFloat() * 0.5f,
                    Color(0xFFB0B0B0), size = 5f + Random.nextFloat() * 4f
                )
            )
        }
    }

    companion object {
        const val MAX_AIM = 0.55f
        const val MUZZLE_FLASH_TIME = 0.07f
        const val DEATH_TIME = 0.9f
    }
}
