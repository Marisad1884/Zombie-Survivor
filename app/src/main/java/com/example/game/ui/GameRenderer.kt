package com.example.game.ui

import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import com.example.game.engine.ActiveFireZone
import com.example.game.engine.GameEngine
import com.example.game.model.*
import kotlin.math.*

object GameRenderer {

    fun render(
        drawScope: DrawScope,
        engine: GameEngine,
        aimTarget: Offset?
    ) {
        val w = drawScope.size.width
        val h = drawScope.size.height

        engine.screenWidth = w
        engine.screenHeight = h

        val t = sceneTime()
        val waterTop = engine.waterTop

        drawScope.withTransform({
            translate(left = engine.shakeX, top = engine.shakeY)
        }) {
            // 1. Night sky, moon, clouds, cypress trees and fireflies
            drawNightSky(w, h, t)
            drawFarShore(w, h, waterTop, t)

            // 2. Swamp water: moonlight reflection, waves, lily pads and fog
            drawSwampWaterLayer(w, h, waterTop, t)
            drawWaterDetails(w, h, waterTop, t)

            // 3. Blood stains on the water (flat decals, under every character)
            drawDecals(engine.particles)

            // 4. Cabin, porch, barricade and roof sentry turret
            drawCabinAndPorch(engine.houseWidth, h, waterTop, engine)

            // 5. Ammo girl (behind the heroine) and the heroine herself
            drawAmmoRunner(engine, t)
            drawHeroSoldier(engine, aimTarget)

            // 6. Fire zones (Molotov pools)
            engine.fireZones.forEach { fire ->
                drawFireZoneFx(fire, t)
            }

            // 7. Zombies sorted by Y for perspective layering
            val sortedZombies = engine.zombies.sortedBy { it.y }
            sortedZombies.forEach { zombie ->
                drawZombie(zombie, waterTop)
            }

            // 8. Projectiles with tracers and glow
            engine.projectiles.forEach { p ->
                drawProjectileFx(p, t)
            }

            // 9. Particles (explosions, blood, sparks, smoke, floating text...)
            drawParticlesFx(engine.particles)

            // 10. Foreground frame (reeds, hanging moss) for depth
            drawForegroundFrame(w, h, t)

            // 11. Light spilling from the muzzle + a cold night tint over the scene
            drawMuzzleLight(engine.playerMuzzleOffset, engine.muzzleFlashTimer)
            drawNightTint(w, h)
        }

        // Vignette is drawn outside the shake transform so the screen edges stay put
        drawScope.drawVignette(w, h)
    }

    private fun DrawScope.drawHeroSoldier(engine: GameEngine, aimTarget: Offset?) {
        val porchY = engine.porchY

        // Heroine positioned right at the defense post behind the waist-high barricade
        val bodyX = engine.houseWidth - 42f
        val bodyY = porchY - 54f

        val anchor = engine.gunAnchor
        val target = aimTarget ?: Offset(engine.screenWidth * 0.75f, porchY - 20f)
        val targetDx = (target.x - anchor.x).coerceAtLeast(3f)
        val rawAngle = atan2(target.y - anchor.y, targetDx)
        // Clamp from -48 deg (-0.85 rad) up to +86 deg (+1.50 rad) down so she can aim and shoot under the house
        val aimAngle = rawAngle.coerceIn(-0.85f, 1.50f)

        val weapon = engine.currentWeapon
        val recoil = (engine.muzzleFlashTimer / 0.08f).coerceIn(0f, 1f) * 4f

        // Body, head, hair, scarf... (see HeroRenderer.kt)
        drawHeroine(bodyX, bodyY, porchY, sceneTime(), engine.muzzleFlashTimer, aimAngle, recoil)

        // Arms and weapon, anchored to her shoulder (see WeaponRenderer.kt)
        drawHeroWeapon(
            bodyX = bodyX,
            bodyY = bodyY,
            anchor = anchor,
            aimAngle = aimAngle,
            type = weapon.type,
            muzzleFlash = engine.muzzleFlashTimer,
            reloading = weapon.isReloading,
            reloadProgress = weapon.reloadProgress
        )
    }

    private fun DrawScope.drawZombie(z: Zombie, waterTop: Float) {
        val isHit = z.hitFlashTimer > 0f
        val zX = z.x
        val zY = z.y

        val w = z.type.width
        val h = z.type.height

        // Deep water shadow beneath zombie
        drawOval(
            color = Color(0x44000000),
            topLeft = Offset(zX - w * 0.55f, zY - 6f),
            size = Size(w * 1.1f, 14f)
        )

        // Glowing aura so bosses / brutes stand out in the dark
        if (z.type == ZombieType.COLOSSUS) {
            drawEliteAura(zX, zY, w, Color(0xFFB3261E), sceneTime())
        } else if (z.type == ZombieType.TANK) {
            drawEliteAura(zX, zY, w, Color(0xFF8E9A4A), sceneTime())
        }

        // Specialized non-humanoid zombies
        if (z.type == ZombieType.GATOR) {
            drawGatorZombie(z, isHit)
            drawZombieHpBar(z)
            return
        }

        if (z.type == ZombieType.ZOMBIE_DOG) {
            drawZombieDog(z, isHit)
            drawZombieHpBar(z)
            return
        }

        // Everything human-shaped lives in ZombieRenderer.kt
        drawHumanoidZombie(z, isHit, sceneTime())
        drawZombieHpBar(z)
    }

    private fun DrawScope.drawGatorZombie(z: Zombie, isHit: Boolean) {
        val color = if (isHit) Color.White else Color(0xFF2E4E37)
        val darkScaleColor = if (isHit) Color.LightGray else Color(0xFF1B3024)
        val eyeColor = Color(0xFFFFD600)
        val zX = z.x
        val zY = z.y

        // Water Wake Rings
        drawOval(
            color = Color(0x6680CBC4),
            topLeft = Offset(zX - 38f, zY - 10f),
            size = Size(80f, 18f),
            style = Stroke(width = 2.2f)
        )
        drawOval(
            color = Color(0x334DB6AC),
            topLeft = Offset(zX - 44f, zY - 13f),
            size = Size(92f, 24f),
            style = Stroke(width = 1.5f)
        )

        // Alligator Tail swaying
        val tailWiggle = sin(z.walkAnimTimer * 2f) * 6f
        val tailPath = Path().apply {
            moveTo(zX + 16f, zY - 4f)
            quadraticTo(zX + 30f, zY - 8f + tailWiggle, zX + 44f, zY - 4f + tailWiggle * 1.5f)
            lineTo(zX + 16f, zY)
            close()
        }
        drawPath(path = tailPath, color = color)

        // Main Alligator Body
        drawOval(
            color = color,
            topLeft = Offset(zX - 28f, zY - 16f),
            size = Size(54f, 20f)
        )

        // Dorsal Scales / Spiky Ridges along back
        for (i in 0..4) {
            val scuteX = zX - 16f + i * 8f
            drawRect(
                color = darkScaleColor,
                topLeft = Offset(scuteX, zY - 18f),
                size = Size(4f, 4f)
            )
        }

        // Massive Reptilian Snout & Jaws
        val jawWiggle = if (z.isAttacking) sin(System.currentTimeMillis() * 0.015f).absoluteValue * 6f else 0f
        // Upper Snout
        drawRoundRect(
            color = color,
            topLeft = Offset(zX - 38f, zY - 14f - jawWiggle),
            size = Size(26f, 8f),
            cornerRadius = CornerRadius(3f, 3f)
        )
        // Lower Jaw
        drawRoundRect(
            color = darkScaleColor,
            topLeft = Offset(zX - 36f, zY - 8f + jawWiggle),
            size = Size(24f, 6f),
            cornerRadius = CornerRadius(2f, 2f)
        )

        // Interlocking sharp conical teeth
        for (i in 0..3) {
            val toothX = zX - 34f + i * 5f
            drawLine(color = Color(0xFFFFFDE7), start = Offset(toothX, zY - 6f - jawWiggle), end = Offset(toothX + 1f, zY - 9f), strokeWidth = 1.8f)
            drawLine(color = Color(0xFFFFFDE7), start = Offset(toothX + 2.5f, zY - 8f + jawWiggle), end = Offset(toothX + 3.5f, zY - 5f), strokeWidth = 1.8f)
        }

        // Predatory slit eyes
        drawCircle(color = eyeColor, center = Offset(zX - 10f, zY - 14f), radius = 3.8f)
        drawLine(color = Color(0xFF1B0000), start = Offset(zX - 10f, zY - 17f), end = Offset(zX - 10f, zY - 11f), strokeWidth = 1.6f)
    }

    private fun DrawScope.drawZombieDog(z: Zombie, isHit: Boolean) {
        val color = if (isHit) Color.White else Color(0xFF4E342E)
        val shadowColor = if (isHit) Color.LightGray else Color(0xFF2E1C14)
        val zX = z.x
        val zY = z.y

        // Water Wake
        drawOval(
            color = Color(0x6680CBC4),
            topLeft = Offset(zX - 26f, zY - 8f),
            size = Size(54f, 15f),
            style = Stroke(width = 2f)
        )

        val pawCycle = z.walkAnimTimer * 3.5f
        val frontLegAnim = sin(pawCycle) * 10f
        val backLegAnim = cos(pawCycle) * 10f

        // 4 Running Legs
        drawLine(color = color, start = Offset(zX - 12f, zY - 10f), end = Offset(zX - 16f - frontLegAnim, zY), strokeWidth = 3.5f, cap = StrokeCap.Round)
        drawLine(color = shadowColor, start = Offset(zX - 8f, zY - 10f), end = Offset(zX - 12f + frontLegAnim, zY), strokeWidth = 3f, cap = StrokeCap.Round)
        drawLine(color = color, start = Offset(zX + 10f, zY - 10f), end = Offset(zX + 6f - backLegAnim, zY), strokeWidth = 3.5f, cap = StrokeCap.Round)
        drawLine(color = shadowColor, start = Offset(zX + 14f, zY - 10f), end = Offset(zX + 10f + backLegAnim, zY), strokeWidth = 3f, cap = StrokeCap.Round)

        // Lean, Emaciated Hound Body
        drawOval(
            color = color,
            topLeft = Offset(zX - 18f, zY - 20f),
            size = Size(36f, 16f)
        )

        // Exposed skeletal rib slits
        if (!isHit) {
            drawLine(color = Color(0xFFCFD8DC), start = Offset(zX - 6f, zY - 18f), end = Offset(zX - 6f, zY - 12f), strokeWidth = 1.8f)
            drawLine(color = Color(0xFFCFD8DC), start = Offset(zX - 1f, zY - 18f), end = Offset(zX - 1f, zY - 12f), strokeWidth = 1.8f)
            drawLine(color = Color(0xFFCFD8DC), start = Offset(zX + 4f, zY - 18f), end = Offset(zX + 4f, zY - 12f), strokeWidth = 1.8f)
        }

        // Bristled raised fur ridge on spine
        for (i in 0..4) {
            val spikeX = zX - 10f + i * 5f
            drawLine(color = shadowColor, start = Offset(spikeX, zY - 20f), end = Offset(spikeX - 2f, zY - 24f), strokeWidth = 2f)
        }

        // Snarling Hound Head & Jaws
        val headX = zX - 20f
        val headY = zY - 16f
        drawCircle(color = color, center = Offset(headX, headY), radius = 8.5f)

        // Snout
        drawRoundRect(
            color = shadowColor,
            topLeft = Offset(headX - 12f, headY - 2f),
            size = Size(14f, 6.5f),
            cornerRadius = CornerRadius(2f, 2f)
        )
        // Dripping rabid foam / fangs
        drawLine(color = Color(0xFFFFF9C4), start = Offset(headX - 10f, headY + 4f), end = Offset(headX - 9f, headY + 7.5f), strokeWidth = 1.8f)
        drawLine(color = Color(0xFFFFF9C4), start = Offset(headX - 6f, headY + 4f), end = Offset(headX - 5f, headY + 7f), strokeWidth = 1.8f)
        drawCircle(color = Color(0xCCEEEEEE), center = Offset(headX - 8f, headY + 7f), radius = 1.8f)

        // Piercing demonic red eye
        drawCircle(color = Color(0xFFFF1744), center = Offset(headX - 4f, headY - 3f), radius = 3f)
        drawCircle(color = Color(0xFF1B0000), center = Offset(headX - 5f, headY - 3f), radius = 1.2f)

        // Pointy wolf ears
        val earPath = Path().apply {
            moveTo(headX + 2f, headY - 7f)
            lineTo(headX + 6f, headY - 14f)
            lineTo(headX + 8f, headY - 6f)
            close()
        }
        drawPath(path = earPath, color = shadowColor)
    }

    private fun DrawScope.drawZombieHpBar(z: Zombie) {
        if (z.currentHp < z.maxHp || z.type.isBoss || z.type == ZombieType.TANK || z.type == ZombieType.ARMORED) {
            val barW = z.type.width * (if (z.type.isBoss) 1.5f else 1.1f)
            val barH = if (z.type.isBoss) 8f else 5f
            val barX = z.x - barW / 2f
            val barY = z.y - z.type.height - 14f

            drawRoundRect(
                color = Color(0xDD000000),
                topLeft = Offset(barX, barY),
                size = Size(barW, barH),
                cornerRadius = CornerRadius(2.5f, 2.5f)
            )

            val hpRatio = (z.currentHp / z.maxHp).coerceIn(0f, 1f)
            val hpColor = when {
                z.type.isBoss -> Color(0xFFFF1744)
                hpRatio > 0.5f -> Color(0xFF4CAF50)
                hpRatio > 0.25f -> Color(0xFFFFC107)
                else -> Color(0xFFF44336)
            }
            drawRoundRect(
                color = hpColor,
                topLeft = Offset(barX, barY),
                size = Size(barW * hpRatio, barH),
                cornerRadius = CornerRadius(2.5f, 2.5f)
            )
        }
    }
}
