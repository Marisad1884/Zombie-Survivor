package com.example.game.ui

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.withTransform
import com.example.game.engine.GameEngine
import com.example.game.engine.GameStatus
import com.example.game.engine.RunnerState
import com.example.game.model.*
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin

/**
 * Draws one frame of the game in world units (the canvas is scaled so the screen is always 340 units tall).
 * `aimTarget` is the finger position in world units, or null when nothing is touching the screen.
 */
object GameRenderer {

    private val WALK_ORDER = intArrayOf(0, 1, 2, 1)

    fun render(drawScope: DrawScope, engine: GameEngine, aimTarget: Offset?) {
        val widthPx = drawScope.size.width
        val heightPx = drawScope.size.height
        engine.setWorldSize(widthPx, heightPx)
        val s = heightPx / Balance.WORLD_H
        val w = engine.worldWidth
        val t = sceneTime()

        drawScope.withTransform({
            scale(s, s, Offset.Zero)
            translate(engine.shakeX, engine.shakeY)
        }) {
            drawHallway(w, engine.floorY, engine.doorCx, t)

            if (engine.status != GameStatus.GAME_OVER && engine.currentWeapon.type == WeaponType.SHOTGUN) {
                drawShotgunLimit(engine)
            }

            drawAmmoGirl(engine)
            drawHeroine(engine)

            // far zombies first so near ones overlap them
            for (z in engine.zombies.sortedBy { it.x }) drawZombie(z, engine.floorY)

            drawGrenades(engine)
            drawTracers(engine)
            drawParticles(engine)
            drawMuzzleFlash(engine)

            drawHallwayShade(w)

            // white flash when a grenade goes off
            if (engine.blastFlash > 0f) {
                drawRect(Color.White.copy(alpha = 0.85f * engine.blastFlash), size = Size(w, Balance.WORLD_H))
            }

            if (aimTarget != null && engine.status == GameStatus.PLAYING) drawCrosshair(aimTarget)
            if (engine.status == GameStatus.GAME_OVER) drawCaughtOverlay(engine, w, t)
        }
    }

    // ------------------------------------------------------------------------------------ zombies
    internal fun DrawScope.drawZombie(z: Zombie, floorY: Float) {
        val palette = when (z.type) {
            ZombieType.NORMAL -> Sprites.BASE_PALETTE
            ZombieType.RUNNER -> Sprites.RUNNER_PALETTE
            ZombieType.BRUTE -> Sprites.BRUTE_PALETTE
        }
        val px = Balance.PX * z.type.scale
        val frame = WALK_ORDER[((z.walkTimer.toInt() % 4) + 4) % 4]
        val left = z.x - 12f * px
        val top = floorY - 53f * px

        // shadow
        drawRect(Color(0x44000000), Offset(z.x - 14f * z.type.scale, floorY - 1f), Size(34f * z.type.scale, 4f))

        if (z.dying) {
            val p = (z.deathTimer / GameEngine.DEATH_TIME).coerceIn(0f, 1f)
            val fall = min(1f, p * (if (z.headshotDeath) 4f else 3f))
            val alpha = 1f - max(0f, (p - 0.55f) / 0.45f)
            withTransform({ rotate(-84f * fall, Offset(z.x - 6f * px, floorY)) }) {
                drawPixelSprite(Sprites.ZOMBIE[1], palette, left, top, px, alpha = alpha)
            }
            return
        }
        val lean = if (z.type == ZombieType.RUNNER) 7f else 0f
        withTransform({ rotate(lean, Offset(z.x, floorY)) }) {
            drawPixelSprite(Sprites.ZOMBIE[frame], palette, left, top, px, flash = z.hitFlash > 0f)
        }
    }

    // ------------------------------------------------------------------------------------ heroine
    private fun DrawScope.drawHeroine(engine: GameEngine) {
        val type = engine.currentWeapon.type
        val kick = (engine.muzzleFlashTimer / GameEngine.MUZZLE_FLASH_TIME).coerceIn(0f, 1f) * (if (type == WeaponType.SHOTGUN) 5f else 3f)
        val lift = if (type == WeaponType.GRENADE && engine.throwTimer > 0f) 55f * (engine.throwTimer / 0.3f).coerceIn(0f, 1f) else 0f
        drawHeroPose(engine.heroX, engine.floorY, engine.shoulder, type, engine.aimAngle, kick, lift)
    }

    /** The heroine standing at heroX with the given weapon raised. `kick` pushes the gun back, `lift` raises the arm (degrees). */
    internal fun DrawScope.drawHeroPose(
        heroX: Float,
        floorY: Float,
        shoulder: Offset,
        type: WeaponType,
        aimAngle: Float,
        kick: Float = 0f,
        lift: Float = 0f
    ) {
        val px = Balance.PX
        val left = heroX - HeroGeometry.BODY_ANCHOR_COL * px
        val top = floorY - HeroGeometry.GROUND_ROW * px

        drawRect(Color(0x44000000), Offset(heroX - 16f, floorY - 1f), Size(34f, 4f))
        drawPixelSprite(Sprites.HERO_BODY, Sprites.BASE_PALETTE, left, top, px)

        val arm = when (type) {
            WeaponType.REVOLVER -> Sprites.ARM_REVOLVER
            WeaponType.SHOTGUN -> Sprites.ARM_SHOTGUN
            WeaponType.GRENADE -> Sprites.ARM_GRENADE
        }
        val (ax, ay) = HeroGeometry.armAnchor(type)
        val degrees = -aimAngle * (180f / PI.toFloat()) - lift
        withTransform({ rotate(degrees, shoulder) }) {
            drawPixelSprite(arm, Sprites.BASE_PALETTE, shoulder.x - ax * px + kick, shoulder.y - ay * px, px)
        }
    }

    private fun DrawScope.drawMuzzleFlash(engine: GameEngine) {
        if (engine.muzzleFlashTimer <= 0f) return
        val type = engine.currentWeapon.type
        if (type == WeaponType.GRENADE) return
        val k = (engine.muzzleFlashTimer / (GameEngine.MUZZLE_FLASH_TIME * if (type == WeaponType.SHOTGUN) 1.6f else 1f)).coerceIn(0f, 1f)
        val m = engine.muzzle()
        val u = (if (type == WeaponType.SHOTGUN) 2.6f else 1.7f) * (0.6f + 0.4f * k)

        drawGlow(m.x - 4f * u, m.y, 26f * u * 0.5f + 10f, Color(0xFFFFB347).copy(alpha = 0.55f * k))
        drawRect(Color(0xFFFF9800), Offset(m.x - 8f * u, m.y - 1.2f * u), Size(9f * u, 2.4f * u))
        drawRect(Color(0xFFFFD54F), Offset(m.x - 4f * u, m.y - 3.5f * u), Size(6f * u, 7f * u))
        drawRect(Color(0xFFFF9800), Offset(m.x - 6f * u, m.y - 4.5f * u), Size(2f * u, 2f * u))
        drawRect(Color(0xFFFF9800), Offset(m.x - 6f * u, m.y + 2.5f * u), Size(2f * u, 2f * u))
        drawRect(Color(0xFFFFFFFF), Offset(m.x - 3f * u, m.y - 1.8f * u), Size(4f * u, 3.6f * u))
    }

    // ------------------------------------------------------------------------------------ ammo girl
    private fun DrawScope.drawAmmoGirl(engine: GameEngine) {
        if (engine.runnerHidden) return
        val px = Balance.PX
        val x = engine.runnerX
        val floorY = engine.floorY
        val moving = engine.runnerState == RunnerState.BRINGING || engine.runnerState == RunnerState.HOMING
        val flip = engine.runnerFacing > 0

        drawRect(Color(0x44000000), Offset(x - 14f, floorY - 1f), Size(30f, 4f))

        // the crate she has just handed over, resting on the floor beside her
        if (engine.runnerState == RunnerState.HANDING && engine.runnerDelivered) {
            drawPixelSprite(Sprites.CRATE, Sprites.BASE_PALETTE, x - 38f, floorY - 12f * px, px)
        }

        val sprite = when {
            engine.runnerCarrying -> Sprites.GIRL_CARRY
            moving -> Sprites.GIRL[WALK_ORDER[((engine.runnerWalkPhase.toInt() % 4) + 4) % 4]]
            else -> Sprites.GIRL[1]
        }
        drawGirlSprite(sprite, x, floorY, flip)

        if (engine.runnerCarrying) {
            val handingOut = engine.runnerState == RunnerState.HANDING && !engine.runnerDelivered
            val dir = if (flip) 1f else -1f
            val reach = if (handingOut) 12f else 0f
            val cx = x + dir * (16f + reach)
            val cy = floorY - 52f
            drawPixelSprite(Sprites.CRATE, Sprites.BASE_PALETTE, cx - Sprites.CRATE.width * px / 2f, cy - 6f * px, px)
            // her hands on the sides of the crate
            drawRect(Color(0xFFF0C8AA), Offset(cx - 14f, cy + 2f), Size(5f, 5f))
            drawRect(Color(0xFFF0C8AA), Offset(cx + 9f, cy + 2f), Size(5f, 5f))
        }
    }

    /** Sprites face left; mirrored when she walks right. */
    internal fun DrawScope.drawGirlSprite(sprite: PixelSprite, x: Float, floorY: Float, flip: Boolean) {
        val px = Balance.PX
        val anchorCol = 13
        val left = if (flip) x - (sprite.width - anchorCol) * px else x - anchorCol * px
        drawPixelSprite(sprite, Sprites.BASE_PALETTE, left, floorY - 53f * px, px, flipX = flip)
    }

    // ------------------------------------------------------------------------------------ effects
    private fun DrawScope.drawShotgunLimit(engine: GameEngine) {
        val x = engine.shotgunLimitX
        var y = 62f
        while (y < engine.floorY) {
            drawRect(Color(0x55FFFFFF), Offset(x - 0.8f, y), Size(1.6f, 6f))
            y += 12f
        }
        drawOutlinedText("ALCANCE ESCOPETA", x + 4f, 76f, 7f, Color(0xBBFFFFFF), alpha = 0.7f)
    }

    private fun DrawScope.drawGrenades(engine: GameEngine) {
        for (g in engine.grenades) {
            // blast radius marker on the floor where it will land
            val pulse = 0.5f + 0.5f * sin(sceneTime() * 12f)
            drawRect(
                Color(0xFFFF5252).copy(alpha = 0.18f + 0.12f * pulse),
                Offset(0f, engine.floorY - 1f),
                Size(engine.worldWidth, 5f)
            )
            drawRect(Color(0x44000000), Offset(g.x - 6f, engine.floorY - 1f), Size(12f, 3f))
            drawPixelSprite(Sprites.ICON_GRENADE, Sprites.BASE_PALETTE, g.x - 7f, g.y - 8f, 1f)
        }
    }

    private fun DrawScope.drawTracers(engine: GameEngine) {
        for (t in engine.tracers) {
            val a = (t.life / 0.07f).coerceIn(0f, 1f)
            drawLine(t.color.copy(alpha = a), Offset(t.x1, t.y1), Offset(t.x2, t.y2), strokeWidth = 1.3f)
        }
    }

    private fun DrawScope.drawParticles(engine: GameEngine) {
        for (p in engine.particles) {
            val r = p.lifeRatio
            when (p.kind) {
                ParticleKind.BLOOD -> drawRect(p.color.copy(alpha = min(1f, r * 3f)), Offset(p.x, p.y), Size(p.size, p.size))
                ParticleKind.SPARK -> drawRect(p.color, Offset(p.x, p.y), Size(p.size, p.size))
                ParticleKind.SMOKE -> {
                    val sz = p.size * (1f + (1f - r) * 1.4f)
                    drawRect(p.color.copy(alpha = 0.45f * r), Offset(p.x - sz / 2f, p.y - sz / 2f), Size(sz, sz))
                }
                ParticleKind.FIRE -> {
                    val sz = p.size * (0.4f + 0.6f * r)
                    drawRect(p.color.copy(alpha = min(1f, r * 2f)), Offset(p.x - sz / 2f, p.y - sz / 2f), Size(sz, sz))
                }
                ParticleKind.CASING -> drawRect(p.color, Offset(p.x, p.y), Size(p.size * 0.6f, p.size))
                ParticleKind.TEXT -> drawOutlinedText(p.text, p.x, p.y, p.size, p.color, alpha = min(1f, r * 2.2f))
            }
        }
    }

    private fun DrawScope.drawCrosshair(at: Offset) {
        val c = Color(0xFFFFFFFF)
        drawRect(Color.Black, Offset(at.x - 9f, at.y - 1.6f), Size(6f, 3.2f))
        drawRect(Color.Black, Offset(at.x + 3f, at.y - 1.6f), Size(6f, 3.2f))
        drawRect(Color.Black, Offset(at.x - 1.6f, at.y - 9f), Size(3.2f, 6f))
        drawRect(Color.Black, Offset(at.x - 1.6f, at.y + 3f), Size(3.2f, 6f))
        drawRect(c, Offset(at.x - 8f, at.y - 0.8f), Size(5f, 1.6f))
        drawRect(c, Offset(at.x + 3f, at.y - 0.8f), Size(5f, 1.6f))
        drawRect(c, Offset(at.x - 0.8f, at.y - 8f), Size(1.6f, 5f))
        drawRect(c, Offset(at.x - 0.8f, at.y + 3f), Size(1.6f, 5f))
        drawRect(Color(0xFFE53935), Offset(at.x - 1f, at.y - 1f), Size(2f, 2f))
    }

    private fun DrawScope.drawCaughtOverlay(engine: GameEngine, w: Float, t: Float) {
        val h = Balance.WORLD_H
        drawRect(Color(0xFF8B0000).copy(alpha = 0.26f + 0.3f * engine.caughtFlash), size = Size(w, h))
        val k = min(1f, engine.gameOverTimer * 4f)
        val pulse = 1f + 0.035f * sin(t * 9f)
        val size = 44f * (0.6f + 0.4f * k) * pulse
        drawOutlinedText("¡TE HAN ATRAPADO!", w / 2f, h * 0.52f, size, Color(0xFFE50000), alpha = k)
    }
}
