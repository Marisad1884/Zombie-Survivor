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

            // 5. Hero soldier
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

        val gunAnchorX = bodyX + 16f
        val gunAnchorY = bodyY + 16f

        val target = aimTarget ?: Offset(engine.screenWidth * 0.75f, porchY - 20f)
        val targetDx = (target.x - gunAnchorX).coerceAtLeast(3f)
        val rawAngle = atan2(target.y - gunAnchorY, targetDx)
        // Clamp from -48 deg (-0.85 rad) up to +86 deg (+1.50 rad) down so she can aim and shoot under the house
        val aimAngle = rawAngle.coerceIn(-0.85f, 1.50f)

        // Body, head, hair, scarf... (see HeroRenderer.kt)
        drawHeroine(bodyX, bodyY, porchY, sceneTime(), engine.muzzleFlashTimer)

        // Arms and gun anchored directly to her hands and shoulder
        drawTacticalGun(gunAnchorX, gunAnchorY, aimAngle, engine.currentWeapon.type, engine.muzzleFlashTimer)
    }

    private fun DrawScope.drawTacticalGun(
        anchorX: Float,
        anchorY: Float,
        angle: Float,
        weaponType: WeaponType,
        muzzleFlashTimer: Float
    ) {
        // Kick the weapon back along its axis right after each shot
        val recoil = (muzzleFlashTimer / 0.08f).coerceIn(0f, 1f) * 4f
        withTransform({
            rotate(degrees = angle * (180f / PI.toFloat()), pivot = Offset(anchorX, anchorY))
            translate(left = -recoil, top = 0f)
        }) {
            val gunLength = when (weaponType) {
                WeaponType.PISTOL -> 28f
                WeaponType.CROSSBOW -> 42f
                WeaponType.SHOTGUN -> 46f
                WeaponType.RIFLE -> 52f
                WeaponType.FLAMETHROWER -> 48f
                WeaponType.LAUNCHER -> 48f
                WeaponType.MINIGUN -> 54f
                WeaponType.SNIPER -> 64f
            }
            val gunWidth = when (weaponType) {
                WeaponType.PISTOL -> 6f
                WeaponType.CROSSBOW -> 7f
                WeaponType.SHOTGUN -> 9f
                WeaponType.RIFLE -> 8f
                WeaponType.FLAMETHROWER -> 12f
                WeaponType.LAUNCHER -> 13f
                WeaponType.MINIGUN -> 12f
                WeaponType.SNIPER -> 8f
            }

            // 1. Right Arm & Shoulder Stock (Anchored to body)
            // Tactical Camo Right Arm sleeve
            drawRoundRect(
                color = Color(0xFF3A4733),
                topLeft = Offset(anchorX - 14f, anchorY - 6f),
                size = Size(14f, 10f),
                cornerRadius = CornerRadius(3f, 3f)
            )
            // Stock braced against shoulder
            drawRoundRect(
                color = Color(0xFF263238),
                topLeft = Offset(anchorX - 16f, anchorY - 4f),
                size = Size(16f, 8f),
                cornerRadius = CornerRadius(2f, 2f)
            )
            // Pistol grip
            drawRoundRect(
                color = Color(0xFF1B2428),
                topLeft = Offset(anchorX, anchorY - 1f),
                size = Size(7f, 13f),
                cornerRadius = CornerRadius(2f, 2f)
            )
            // Right Gloved Hand on grip & trigger
            drawCircle(
                color = Color(0xFF212121),
                center = Offset(anchorX + 3f, anchorY + 4f),
                radius = 5f
            )

            // 2. Weapon Main Receiver & Barrel
            drawRoundRect(
                color = Color(0xFF212121),
                topLeft = Offset(anchorX - 2f, anchorY - gunWidth / 2f),
                size = Size(gunLength, gunWidth),
                cornerRadius = CornerRadius(2f, 2f)
            )

            // 3. Left Arm & Hand supporting weapon foregrip / barrel
            // Left tactical arm reaching forward
            drawLine(
                color = Color(0xFF3A4733),
                start = Offset(anchorX - 10f, anchorY + 5f),
                end = Offset(anchorX + 16f, anchorY + 3f),
                strokeWidth = 6f
            )
            // Bare forearm below the rolled sleeve
            drawLine(
                color = Color(0xFFC98E66),
                start = Offset(anchorX + 6f, anchorY + 4f),
                end = Offset(anchorX + 16f, anchorY + 3f),
                strokeWidth = 4.4f
            )
            // Left Gloved Hand holding barrel/handguard
            drawCircle(
                color = Color(0xFF212121),
                center = Offset(anchorX + 16f, anchorY + 3f),
                radius = 4.5f
            )

            // 4. Weapon Type Specific Details
            when (weaponType) {
                WeaponType.PISTOL -> {
                    // Brushed metal slide top
                    drawRect(
                        color = Color(0xFF546E7A),
                        topLeft = Offset(anchorX + 4f, anchorY - gunWidth / 2f - 1.5f),
                        size = Size(gunLength - 4f, 3f)
                    )
                    // Tactical red dot optic
                    drawCircle(color = Color(0xFFFF1744), center = Offset(anchorX + 7f, anchorY - gunWidth / 2f - 2f), radius = 1.5f)
                }

                WeaponType.CROSSBOW -> {
                    // Curved bow limbs extending vertically
                    drawLine(
                        color = Color(0xFF455A64),
                        start = Offset(anchorX + 28f, anchorY - 18f),
                        end = Offset(anchorX + 34f, anchorY),
                        strokeWidth = 3f
                    )
                    drawLine(
                        color = Color(0xFF455A64),
                        start = Offset(anchorX + 28f, anchorY + 18f),
                        end = Offset(anchorX + 34f, anchorY),
                        strokeWidth = 3f
                    )
                    // High-tension bowstring
                    drawLine(
                        color = Color(0xFFB0BEC5),
                        start = Offset(anchorX + 28f, anchorY - 18f),
                        end = Offset(anchorX + 12f, anchorY),
                        strokeWidth = 1.5f
                    )
                    drawLine(
                        color = Color(0xFFB0BEC5),
                        start = Offset(anchorX + 28f, anchorY + 18f),
                        end = Offset(anchorX + 12f, anchorY),
                        strokeWidth = 1.5f
                    )
                    // Glowing loaded hunting bolt
                    drawLine(
                        color = Color(0xFF76FF03),
                        start = Offset(anchorX + 12f, anchorY),
                        end = Offset(anchorX + 38f, anchorY),
                        strokeWidth = 2f
                    )
                }

                WeaponType.SHOTGUN -> {
                    // Under-barrel tubular magazine
                    drawRoundRect(
                        color = Color(0xFF37474F),
                        topLeft = Offset(anchorX + 8f, anchorY + 1f),
                        size = Size(gunLength - 10f, 4f),
                        cornerRadius = CornerRadius(1.5f, 1.5f)
                    )
                    // Ribbed pump fore-end slide
                    drawRoundRect(
                        color = Color(0xFF5D4037), // wooden/rugged pump
                        topLeft = Offset(anchorX + 16f, anchorY + 0.5f),
                        size = Size(13f, 6f),
                        cornerRadius = CornerRadius(2f, 2f)
                    )
                    // Large 12-gauge muzzle choke
                    drawRect(
                        color = Color(0xFF263238),
                        topLeft = Offset(anchorX + gunLength - 2f, anchorY - gunWidth / 2f - 1f),
                        size = Size(4f, gunWidth + 2f)
                    )
                }

                WeaponType.RIFLE -> {
                    // Curved military 30-round magazine
                    drawRoundRect(
                        color = Color(0xFF263238),
                        topLeft = Offset(anchorX + 10f, anchorY + gunWidth / 2f),
                        size = Size(8f, 15f),
                        cornerRadius = CornerRadius(2f, 2f)
                    )
                    // Tactical carrying handle / ACOG scope
                    drawRoundRect(
                        color = Color(0xFF37474F),
                        topLeft = Offset(anchorX + 8f, anchorY - gunWidth / 2f - 6f),
                        size = Size(12f, 6f),
                        cornerRadius = CornerRadius(1.5f, 1.5f)
                    )
                    drawCircle(color = Color(0xFF00E676), center = Offset(anchorX + 14f, anchorY - gunWidth / 2f - 3f), radius = 1.5f)
                    // Birdcage flash hider
                    drawRect(
                        color = Color(0xFF455A64),
                        topLeft = Offset(anchorX + gunLength, anchorY - 2.5f),
                        size = Size(5f, 5f)
                    )
                }

                WeaponType.FLAMETHROWER -> {
                    // Fuel line hose
                    drawLine(
                        color = Color(0xFFFF8F00),
                        start = Offset(anchorX - 6f, anchorY + 8f),
                        end = Offset(anchorX + 14f, anchorY + 6f),
                        strokeWidth = 3f
                    )
                    // Pressure gauge
                    drawCircle(color = Color(0xFFFFD54F), center = Offset(anchorX + 12f, anchorY - gunWidth / 2f - 2f), radius = 3.5f)
                    // Flared combustion nozzle & pilot flame
                    drawRoundRect(
                        color = Color(0xFF3E2723),
                        topLeft = Offset(anchorX + gunLength - 4f, anchorY - gunWidth / 2f - 2f),
                        size = Size(8f, gunWidth + 4f),
                        cornerRadius = CornerRadius(2f, 2f)
                    )
                    // Small glowing pilot light
                    drawCircle(color = Color(0xFFFF6F00), center = Offset(anchorX + gunLength + 3f, anchorY), radius = 2.5f)
                }

                WeaponType.LAUNCHER -> {
                    // Heavy cylindrical tube
                    drawRoundRect(
                        color = Color(0xFF2E4E37),
                        topLeft = Offset(anchorX + 2f, anchorY - gunWidth / 2f),
                        size = Size(gunLength, gunWidth),
                        cornerRadius = CornerRadius(3f, 3f)
                    )
                    // Launcher muzzle ring
                    drawRoundRect(
                        color = Color(0xFF1B2428),
                        topLeft = Offset(anchorX + gunLength - 2f, anchorY - gunWidth / 2f - 1.5f),
                        size = Size(5f, gunWidth + 3f),
                        cornerRadius = CornerRadius(2f, 2f)
                    )
                    // Elevation sight
                    drawRect(
                        color = Color(0xFFFFD54F),
                        topLeft = Offset(anchorX + 14f, anchorY - gunWidth / 2f - 5f),
                        size = Size(3f, 5f)
                    )
                }

                WeaponType.MINIGUN -> {
                    // Rotary 6-barrel cluster
                    for (b in -2..2) {
                        drawLine(
                            color = Color(0xFF37474F),
                            start = Offset(anchorX + 12f, anchorY + b * 2.5f),
                            end = Offset(anchorX + gunLength, anchorY + b * 2.5f),
                            strokeWidth = 1.8f
                        )
                    }
                    // Rotating clamping collar rings
                    drawRect(color = Color(0xFF212121), topLeft = Offset(anchorX + 24f, anchorY - gunWidth / 2f), size = Size(3f, gunWidth))
                    drawRect(color = Color(0xFF212121), topLeft = Offset(anchorX + 42f, anchorY - gunWidth / 2f), size = Size(3f, gunWidth))
                    // Top carrying handle
                    drawRoundRect(
                        color = Color(0xFF263238),
                        topLeft = Offset(anchorX + 10f, anchorY - gunWidth / 2f - 6f),
                        size = Size(16f, 5f),
                        cornerRadius = CornerRadius(1.5f, 1.5f)
                    )
                }

                WeaponType.SNIPER -> {
                    // High-magnification sniper optic scope with mount
                    drawRoundRect(
                        color = Color(0xFF263238),
                        topLeft = Offset(anchorX + 10f, anchorY - gunWidth / 2f - 8f),
                        size = Size(20f, 7f),
                        cornerRadius = CornerRadius(2f, 2f)
                    )
                    drawCircle(color = Color(0xFF00E5FF), center = Offset(anchorX + 28f, anchorY - gunWidth / 2f - 4.5f), radius = 2.5f)
                    // Fluted long heavy barrel
                    drawLine(
                        color = Color(0xFF37474F),
                        start = Offset(anchorX + 22f, anchorY),
                        end = Offset(anchorX + gunLength, anchorY),
                        strokeWidth = 3.5f
                    )
                    // Heavy recoil muzzle brake
                    drawRect(
                        color = Color(0xFF212121),
                        topLeft = Offset(anchorX + gunLength, anchorY - 3f),
                        size = Size(6f, 6f)
                    )
                    // Folded bipod
                    drawRect(
                        color = Color(0xFF455A64),
                        topLeft = Offset(anchorX + 38f, anchorY + 3f),
                        size = Size(10f, 2.5f)
                    )
                }
            }

            // 5. Dynamic Muzzle Flash when firing
            if (muzzleFlashTimer > 0f) {
                val flashX = anchorX + gunLength + 2f
                val flashY = anchorY
                val flashRadius = (10f + muzzleFlashTimer * 35f).coerceAtMost(22f)

                drawCircle(
                    color = Color(0xFFFFD54F),
                    center = Offset(flashX + 3f, flashY),
                    radius = flashRadius
                )
                drawCircle(
                    color = Color(0xFFFFF9C4),
                    center = Offset(flashX + 1f, flashY),
                    radius = flashRadius * 0.55f
                )
                // Flash sparks
                drawLine(color = Color(0xFFFF9800), start = Offset(flashX, flashY), end = Offset(flashX + flashRadius * 1.5f, flashY - flashRadius * 0.6f), strokeWidth = 2f)
                drawLine(color = Color(0xFFFF9800), start = Offset(flashX, flashY), end = Offset(flashX + flashRadius * 1.5f, flashY + flashRadius * 0.6f), strokeWidth = 2f)
                drawLine(color = Color(0xFFFFEB3B), start = Offset(flashX, flashY), end = Offset(flashX + flashRadius * 1.8f, flashY), strokeWidth = 2.5f)
            }
        }
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
