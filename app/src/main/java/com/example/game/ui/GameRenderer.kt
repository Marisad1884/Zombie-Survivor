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

            // 2. Swamp water: moonlight reflection, waves, lily pads and fog
            drawSwampWaterLayer(w, h, waterTop, t)

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

            // 10. Light spilling from the muzzle + a cold night tint over the scene
            drawMuzzleLight(engine.playerMuzzleOffset, engine.muzzleFlashTimer)
            drawNightTint(w, h)
        }

        // Vignette is drawn outside the shake transform so the screen edges stay put
        drawScope.drawVignette(w, h)
    }

    private fun DrawScope.drawHeroSoldier(engine: GameEngine, aimTarget: Offset?) {
        val porchY = engine.porchY

        // Soldier positioned right at the defense post behind waist-high barricade
        val bodyX = engine.houseWidth - 42f
        val bodyY = porchY - 54f

        val gunAnchorX = bodyX + 16f
        val gunAnchorY = bodyY + 16f

        val target = aimTarget ?: Offset(engine.screenWidth * 0.75f, porchY - 20f)
        val targetDx = (target.x - gunAnchorX).coerceAtLeast(3f)
        val rawAngle = atan2(target.y - gunAnchorY, targetDx)
        // Clamp from -48° (-0.85 rad) up to +86° (+1.50 rad) down so character can aim and shoot under the house
        val aimAngle = rawAngle.coerceIn(-0.85f, 1.50f)

        // Stool/defense bench
        drawRoundRect(
            color = Color(0xFF3E2723),
            topLeft = Offset(bodyX - 22f, porchY - 26f),
            size = Size(20f, 26f),
            cornerRadius = CornerRadius(3f, 3f)
        )

        // Legs in combat camo pants
        drawRoundRect(
            color = Color(0xFF2E4E37),
            topLeft = Offset(bodyX - 12f, bodyY + 32f),
            size = Size(22f, 22f),
            cornerRadius = CornerRadius(3f, 3f)
        )
        // Combat Boots firmly planted on porch deck
        drawRoundRect(
            color = Color(0xFF1B2428),
            topLeft = Offset(bodyX - 13f, porchY - 8f),
            size = Size(12f, 8f),
            cornerRadius = CornerRadius(2f, 2f)
        )
        drawRoundRect(
            color = Color(0xFF1B2428),
            topLeft = Offset(bodyX - 1f, porchY - 8f),
            size = Size(12f, 8f),
            cornerRadius = CornerRadius(2f, 2f)
        )

        // Torso: Olive Drab Military Tactical Combat Vest
        drawRoundRect(
            color = Color(0xFF2E4E37),
            topLeft = Offset(bodyX - 14f, bodyY + 4f),
            size = Size(28f, 30f),
            cornerRadius = CornerRadius(5f, 5f)
        )
        drawRoundRect(
            color = Color(0xFF1E3526),
            topLeft = Offset(bodyX - 10f, bodyY + 8f),
            size = Size(20f, 22f),
            cornerRadius = CornerRadius(4f, 4f)
        )

        // USA Flag Badge on Chest
        drawRect(
            color = Color(0xFF1A237E),
            topLeft = Offset(bodyX - 7f, bodyY + 12f),
            size = Size(6f, 6f)
        )
        drawRect(
            color = Color(0xFFB71C1C),
            topLeft = Offset(bodyX - 1f, bodyY + 12f),
            size = Size(8f, 6f)
        )
        drawLine(
            color = Color.White,
            start = Offset(bodyX - 1f, bodyY + 14f),
            end = Offset(bodyX + 7f, bodyY + 14f),
            strokeWidth = 1.5f
        )
        drawLine(
            color = Color.White,
            start = Offset(bodyX - 1f, bodyY + 16f),
            end = Offset(bodyX + 7f, bodyY + 16f),
            strokeWidth = 1.5f
        )

        // Head position
        val headX = bodyX
        val headY = bodyY - 12f
        val headRadius = 15f

        // Gas Mask Faceplate
        drawCircle(
            color = Color(0xFF203B2E),
            center = Offset(headX, headY),
            radius = headRadius
        )

        // AMERICAN FLAG HELMET
        val helmetCenter = Offset(headX, headY - 4f)
        val helmetRadius = headRadius + 3.5f

        // Blue field covering front-top
        drawArc(
            color = Color(0xFF154360),
            startAngle = 170f,
            sweepAngle = 110f,
            useCenter = true,
            topLeft = Offset(helmetCenter.x - helmetRadius, helmetCenter.y - helmetRadius),
            size = Size(helmetRadius * 2, helmetRadius * 2)
        )
        // Red and White curved stripes on right/back
        drawArc(
            color = Color(0xFFB71C1C),
            startAngle = 280f,
            sweepAngle = 90f,
            useCenter = true,
            topLeft = Offset(helmetCenter.x - helmetRadius, helmetCenter.y - helmetRadius),
            size = Size(helmetRadius * 2, helmetRadius * 2)
        )
        drawLine(
            color = Color.White,
            start = Offset(headX + 5f, headY - 18f),
            end = Offset(headX + 16f, headY - 6f),
            strokeWidth = 3f
        )
        drawLine(
            color = Color.White,
            start = Offset(headX + 1f, headY - 19f),
            end = Offset(headX + 12f, headY - 11f),
            strokeWidth = 2.5f
        )

        // White stars on the blue field
        drawCircle(color = Color.White, center = Offset(headX - 6f, headY - 14f), radius = 1.8f)
        drawCircle(color = Color.White, center = Offset(headX - 11f, headY - 9f), radius = 1.8f)
        drawCircle(color = Color.White, center = Offset(headX - 2f, headY - 10f), radius = 1.8f)
        drawCircle(color = Color.White, center = Offset(headX - 7f, headY - 6f), radius = 1.6f)

        // Red Brim around helmet base
        drawArc(
            color = Color(0xFFB71C1C),
            startAngle = 160f,
            sweepAngle = 150f,
            useCenter = false,
            topLeft = Offset(helmetCenter.x - helmetRadius - 1f, helmetCenter.y - helmetRadius + 4f),
            size = Size(helmetRadius * 2 + 2f, helmetRadius * 2),
            style = Stroke(width = 4f)
        )

        // NVG mount bracket
        drawRoundRect(
            color = Color(0xFF263238),
            topLeft = Offset(headX - 2f, headY - 15f),
            size = Size(7f, 7f),
            cornerRadius = CornerRadius(1.5f, 1.5f)
        )

        // GAS MASK GOGGLES: Glowing Neon Green Circular Lenses!
        drawCircle(color = Color(0xFF1B2A22), center = Offset(headX - 4f, headY - 1f), radius = 6.5f)
        drawCircle(color = Color(0xFF1B2A22), center = Offset(headX + 7f, headY - 1f), radius = 6.5f)

        // Glowing Neon Green Lenses with Outer Halo
        drawCircle(color = Color(0x4476FF03), center = Offset(headX - 4f, headY - 1f), radius = 8.5f)
        drawCircle(color = Color(0x4476FF03), center = Offset(headX + 7f, headY - 1f), radius = 8.5f)
        drawCircle(color = Color(0xFF76FF03), center = Offset(headX - 4f, headY - 1f), radius = 5.2f)
        drawCircle(color = Color(0xFF76FF03), center = Offset(headX + 7f, headY - 1f), radius = 5.2f)
        drawCircle(color = Color(0xFFCCFF90), center = Offset(headX - 5f, headY - 2.5f), radius = 2.2f)
        drawCircle(color = Color(0xFFCCFF90), center = Offset(headX + 6f, headY - 2.5f), radius = 2.2f)

        // Snout Filter Grill
        drawRoundRect(
            color = Color(0xFF193025),
            topLeft = Offset(headX + 2f, headY + 5f),
            size = Size(11f, 10f),
            cornerRadius = CornerRadius(3f, 3f)
        )
        drawLine(color = Color(0xFF0F1E17), start = Offset(headX + 4f, headY + 7f), end = Offset(headX + 11f, headY + 7f), strokeWidth = 1.5f)
        drawLine(color = Color(0xFF0F1E17), start = Offset(headX + 4f, headY + 10f), end = Offset(headX + 11f, headY + 10f), strokeWidth = 1.5f)
        drawLine(color = Color(0xFF0F1E17), start = Offset(headX + 4f, headY + 13f), end = Offset(headX + 11f, headY + 13f), strokeWidth = 1.5f)

        // Side Canister
        drawCircle(color = Color(0xFF2E4E37), center = Offset(headX + 14f, headY + 8f), radius = 5.5f)
        drawCircle(color = Color(0xFF193025), center = Offset(headX + 14f, headY + 8f), radius = 3.5f)

        // Arms and Gun anchored directly to the soldier's hands and shoulder
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
                color = Color(0xFF2E4E37),
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
                color = Color(0xFF2E4E37),
                start = Offset(anchorX - 10f, anchorY + 5f),
                end = Offset(anchorX + 16f, anchorY + 3f),
                strokeWidth = 6f
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
        val animOffset = sin(z.walkAnimTimer) * 4.5f
        val legAnim = cos(z.walkAnimTimer) * 14f

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
            drawEliteAura(zX, zY, w, Color(0xFFFF1744), sceneTime())
        } else if (z.type == ZombieType.TANK) {
            drawEliteAura(zX, zY, w, Color(0xFF76FF03), sceneTime())
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

        // Color palettes per zombie type
        val skinColor = if (isHit) Color.White else when (z.type) {
            ZombieType.WALKER -> Color(0xFF558B2F)
            ZombieType.RUNNER -> Color(0xFF6D4C41)
            ZombieType.SPITTER -> Color(0xFF689F38)
            ZombieType.ARMORED -> Color(0xFF455A64)
            ZombieType.TANK -> Color(0xFF37474F)
            ZombieType.COLOSSUS -> Color(0xFF212121)
            else -> Color(0xFF558B2F)
        }

        val skinShadowColor = if (isHit) Color.LightGray else when (z.type) {
            ZombieType.WALKER -> Color(0xFF33691E)
            ZombieType.RUNNER -> Color(0xFF4E342E)
            ZombieType.SPITTER -> Color(0xFF33691E)
            ZombieType.ARMORED -> Color(0xFF263238)
            ZombieType.TANK -> Color(0xFF263238)
            ZombieType.COLOSSUS -> Color(0xFF0D0D0D)
            else -> Color(0xFF33691E)
        }

        val clothesColor = if (isHit) Color.White else when (z.type) {
            ZombieType.WALKER -> Color(0xFF1976D2) // Tattered denim
            ZombieType.RUNNER -> Color(0xFFC62828) // Shredded athletic red hoodie
            ZombieType.SPITTER -> Color(0xFF33691E) // Swamp muck rags
            ZombieType.ARMORED -> Color(0xFF1B2E24) // Tactical military camo
            ZombieType.TANK -> Color(0xFF455A64) // Overalls
            ZombieType.COLOSSUS -> Color(0xFF880E4F) // Warped demonic garments
            else -> Color(0xFF1976D2)
        }

        // 1. Water Ripples & Wake
        drawOval(
            color = Color(0x6680CBC4),
            topLeft = Offset(zX - w * 0.65f, zY - 8f),
            size = Size(w * 1.3f, 12f),
            style = Stroke(width = 2.2f)
        )
        drawOval(
            color = Color(0x334DB6AC),
            topLeft = Offset(zX - w * 0.8f, zY - 11f),
            size = Size(w * 1.6f, 16f),
            style = Stroke(width = 1.4f)
        )
        // Splash water droplets
        val splashX = zX + (sin(z.walkAnimTimer * 3f) * 12f)
        drawCircle(color = Color(0x88B2DFDB), center = Offset(splashX, zY - 3f), radius = 2.5f)

        // 2. Legs & Feet
        val hipY = zY - h * 0.36f
        val legStroke = if (z.type == ZombieType.COLOSSUS) 18f else if (z.type == ZombieType.TANK) 14f else 7.5f

        // Back leg
        drawLine(
            color = clothesColor,
            start = Offset(zX + w * 0.16f, hipY),
            end = Offset(zX + w * 0.16f + legAnim, zY),
            strokeWidth = legStroke,
            cap = StrokeCap.Round
        )
        // Back foot muddy boot
        drawRect(
            color = Color(0xFF1B1B1B),
            topLeft = Offset(zX + w * 0.16f + legAnim - 6f, zY - 4f),
            size = Size(10f, 5f)
        )

        // Front leg
        drawLine(
            color = clothesColor,
            start = Offset(zX - w * 0.16f, hipY),
            end = Offset(zX - w * 0.16f - legAnim, zY),
            strokeWidth = legStroke,
            cap = StrokeCap.Round
        )
        // Front foot muddy boot
        drawRect(
            color = Color(0xFF1B1B1B),
            topLeft = Offset(zX - w * 0.16f - legAnim - 8f, zY - 4f),
            size = Size(12f, 5f)
        )

        // 3. Torso & Upper Body
        val torsoY = zY - h * 0.74f + animOffset
        val torsoH = h * 0.42f
        val torsoW = w * 0.72f

        // Base torso
        drawRoundRect(
            color = clothesColor,
            topLeft = Offset(zX - torsoW / 2f, torsoY),
            size = Size(torsoW, torsoH),
            cornerRadius = CornerRadius(5f, 5f)
        )

        // Shading on back of torso
        drawRoundRect(
            color = Color(0x33000000),
            topLeft = Offset(zX + torsoW * 0.1f, torsoY),
            size = Size(torsoW * 0.4f, torsoH),
            cornerRadius = CornerRadius(4f, 4f)
        )

        // Exposed decayed chest/ribs or armor details
        if (z.type == ZombieType.WALKER && !isHit) {
            // Torn shirt revealing rotting green ribs
            drawRect(
                color = skinShadowColor,
                topLeft = Offset(zX - torsoW * 0.35f, torsoY + 6f),
                size = Size(torsoW * 0.4f, torsoH * 0.6f)
            )
            // Exposed bone ribs
            drawLine(color = Color(0xFFFFF9C4), start = Offset(zX - torsoW * 0.32f, torsoY + 10f), end = Offset(zX - torsoW * 0.05f, torsoY + 10f), strokeWidth = 2f)
            drawLine(color = Color(0xFFFFF9C4), start = Offset(zX - torsoW * 0.32f, torsoY + 16f), end = Offset(zX - torsoW * 0.08f, torsoY + 16f), strokeWidth = 2f)
            drawLine(color = Color(0xFFFFF9C4), start = Offset(zX - torsoW * 0.30f, torsoY + 22f), end = Offset(zX - torsoW * 0.10f, torsoY + 22f), strokeWidth = 2f)
        }

        // Armored: Kevlar vest with silver steel rivets & tactical plate
        if (z.type == ZombieType.ARMORED && !isHit) {
            drawRoundRect(
                color = Color(0xFF263238),
                topLeft = Offset(zX - torsoW * 0.48f, torsoY + 2f),
                size = Size(torsoW * 0.96f, torsoH * 0.88f),
                cornerRadius = CornerRadius(4f, 4f)
            )
            // Tactical ceramic plate
            drawRoundRect(
                color = Color(0xFF37474F),
                topLeft = Offset(zX - torsoW * 0.35f, torsoY + 8f),
                size = Size(torsoW * 0.7f, torsoH * 0.55f),
                cornerRadius = CornerRadius(3f, 3f)
            )
            // Silver rivets
            drawCircle(color = Color(0xFFCFD8DC), center = Offset(zX - torsoW * 0.25f, torsoY + 10f), radius = 1.5f)
            drawCircle(color = Color(0xFFCFD8DC), center = Offset(zX + torsoW * 0.25f, torsoY + 10f), radius = 1.5f)
            drawCircle(color = Color(0xFFCFD8DC), center = Offset(zX - torsoW * 0.25f, torsoY + torsoH * 0.55f), radius = 1.5f)
            drawCircle(color = Color(0xFFCFD8DC), center = Offset(zX + torsoW * 0.25f, torsoY + torsoH * 0.55f), radius = 1.5f)
        }

        // Spitter: Glowing toxic pustules
        if (z.type == ZombieType.SPITTER && !isHit) {
            drawCircle(color = Color(0x6676FF03), center = Offset(zX + torsoW * 0.25f, torsoY + 8f), radius = 9f)
            drawCircle(color = Color(0xFF76FF03), center = Offset(zX + torsoW * 0.25f, torsoY + 8f), radius = 6f)
            drawCircle(color = Color(0xFFCCFF90), center = Offset(zX + torsoW * 0.22f, torsoY + 6f), radius = 2.5f)

            drawCircle(color = Color(0xFF64DD17), center = Offset(zX - 2f, torsoY + 16f), radius = 4.5f)
            drawCircle(color = Color(0xFFCCFF90), center = Offset(zX - 3f, torsoY + 15f), radius = 1.8f)
        }

        // Tank / Colossus: Mutated bulging veins & bone spikes
        if ((z.type == ZombieType.TANK || z.type == ZombieType.COLOSSUS) && !isHit) {
            // Shoulder bone spikes
            drawLine(
                color = Color(0xFFE0E0E0),
                start = Offset(zX + torsoW * 0.45f, torsoY + 4f),
                end = Offset(zX + torsoW * 0.65f, torsoY - 8f),
                strokeWidth = 4f,
                cap = StrokeCap.Round
            )
            // Toxic mutant veins
            val veinColor = if (z.type == ZombieType.COLOSSUS) Color(0xFFFF1744) else Color(0xFF76FF03)
            drawLine(color = veinColor, start = Offset(zX - torsoW * 0.2f, torsoY + 8f), end = Offset(zX + torsoW * 0.1f, torsoY + 16f), strokeWidth = 2.2f)
            drawLine(color = veinColor, start = Offset(zX + torsoW * 0.1f, torsoY + 16f), end = Offset(zX - torsoW * 0.1f, torsoY + 26f), strokeWidth = 1.8f)
        }

        // Fresh wounds appear as the zombie takes damage
        if (!isHit && z.currentHp < z.maxHp * 0.6f) {
            val wound = Color(0xFF7B0D12)
            drawLine(wound, Offset(zX - torsoW * 0.1f, torsoY + 4f), Offset(zX + torsoW * 0.22f, torsoY + 15f), 3f, StrokeCap.Round)
            drawCircle(Color(0xFFB71C1C), 2.4f, Offset(zX + torsoW * 0.22f, torsoY + 15f))
            if (z.currentHp < z.maxHp * 0.3f) {
                drawLine(wound, Offset(zX - torsoW * 0.3f, torsoY + torsoH * 0.55f), Offset(zX + torsoW * 0.05f, torsoY + torsoH * 0.8f), 3.2f, StrokeCap.Round)
                drawCircle(Color(0xFFB71C1C), 2.8f, Offset(zX - torsoW * 0.3f, torsoY + torsoH * 0.55f))
            }
        }

        // 4. Arms & Claws
        val armY = torsoY + 8f
        val armStroke = if (z.type == ZombieType.COLOSSUS) 15f else if (z.type == ZombieType.TANK) 11f else 6.5f

        if (z.isAttacking) {
            // Ferocious double-handed swiping attack animation!
            val attackSwipe = sin(System.currentTimeMillis() * 0.012f) * 12f
            // Front arm clawing forward
            drawLine(
                color = skinColor,
                start = Offset(zX - torsoW * 0.2f, armY),
                end = Offset(zX - torsoW * 0.6f - 24f + attackSwipe, armY + 4f),
                strokeWidth = armStroke,
                cap = StrokeCap.Round
            )
            // Claw hand
            drawCircle(color = skinColor, center = Offset(zX - torsoW * 0.6f - 25f + attackSwipe, armY + 4f), radius = armStroke * 0.55f)
            drawLine(color = Color(0xFF212121), start = Offset(zX - torsoW * 0.6f - 25f + attackSwipe, armY + 2f), end = Offset(zX - torsoW * 0.6f - 31f + attackSwipe, armY + 1f), strokeWidth = 2f)
            drawLine(color = Color(0xFF212121), start = Offset(zX - torsoW * 0.6f - 25f + attackSwipe, armY + 5f), end = Offset(zX - torsoW * 0.6f - 32f + attackSwipe, armY + 5f), strokeWidth = 2f)

            // Back arm clawing
            drawLine(
                color = skinShadowColor,
                start = Offset(zX + torsoW * 0.1f, armY + 4f),
                end = Offset(zX - torsoW * 0.4f - 18f - attackSwipe, armY - 4f),
                strokeWidth = armStroke * 0.85f,
                cap = StrokeCap.Round
            )
        } else {
            // Shambling / lunging arms
            val armBob = sin(z.walkAnimTimer * 2f) * 5f
            val reach = if (z.type == ZombieType.RUNNER) 24f else 18f
            // Front reaching arm
            drawLine(
                color = skinColor,
                start = Offset(zX - torsoW * 0.2f, armY),
                end = Offset(zX - torsoW * 0.5f - reach, armY + armBob),
                strokeWidth = armStroke,
                cap = StrokeCap.Round
            )
            // Claw fingertips
            drawCircle(color = skinColor, center = Offset(zX - torsoW * 0.5f - reach, armY + armBob), radius = armStroke * 0.5f)
            drawLine(
                color = Color(0xFF212121),
                start = Offset(zX - torsoW * 0.5f - reach, armY + armBob),
                end = Offset(zX - torsoW * 0.5f - reach - 5f, armY + armBob),
                strokeWidth = 2f
            )

            // Back trailing arm
            drawLine(
                color = skinShadowColor,
                start = Offset(zX + torsoW * 0.2f, armY + 3f),
                end = Offset(zX - torsoW * 0.2f - reach * 0.6f, armY + 6f - armBob),
                strokeWidth = armStroke * 0.85f,
                cap = StrokeCap.Round
            )
        }

        // 5. Head & Grotesque Face
        val headRadius = if (z.type == ZombieType.COLOSSUS) 22f else if (z.type == ZombieType.TANK) 17f else 12f
        val headLean = if (z.type == ZombieType.RUNNER) 10f else 3f
        val headX = zX - headLean
        val headY = torsoY - headRadius + 2f

        // Head base
        drawCircle(
            color = skinColor,
            center = Offset(headX, headY),
            radius = headRadius
        )

        // Head shadow contour
        drawArc(
            color = skinShadowColor,
            startAngle = 270f,
            sweepAngle = 180f,
            useCenter = false,
            topLeft = Offset(headX - headRadius, headY - headRadius),
            size = Size(headRadius * 2, headRadius * 2),
            style = Stroke(width = 2.5f)
        )

        // Helmet for Armored Zombie
        if (z.type == ZombieType.ARMORED && !isHit) {
            drawArc(
                color = Color(0xFF1B2E24),
                startAngle = 160f,
                sweepAngle = 220f,
                useCenter = true,
                topLeft = Offset(headX - headRadius - 3f, headY - headRadius - 4f),
                size = Size((headRadius + 3f) * 2, (headRadius + 3f) * 2)
            )
            // Visor rim & glass reflection
            drawLine(
                color = Color(0xFF37474F),
                start = Offset(headX - headRadius - 2f, headY),
                end = Offset(headX - 1f, headY + 2f),
                strokeWidth = 3f
            )
            drawRect(
                color = Color(0x664FC3F7),
                topLeft = Offset(headX - headRadius * 0.8f, headY - 2f),
                size = Size(headRadius * 0.65f, 4f)
            )
        }

        // Hair strands (sparse rotten hair)
        if (z.type != ZombieType.ARMORED && !isHit) {
            val hairColor = Color(0xFF263238)
            drawLine(color = hairColor, start = Offset(headX - 4f, headY - headRadius), end = Offset(headX - 9f, headY - headRadius + 4f), strokeWidth = 2f)
            drawLine(color = hairColor, start = Offset(headX + 2f, headY - headRadius), end = Offset(headX + 5f, headY - headRadius + 6f), strokeWidth = 2.2f)
            drawLine(color = hairColor, start = Offset(headX + 7f, headY - headRadius * 0.7f), end = Offset(headX + 11f, headY - headRadius * 0.2f), strokeWidth = 1.8f)
        }

        // Gaping rotten mouth cavity
        val mouthX = headX - headRadius * 0.8f
        val mouthY = headY + headRadius * 0.25f
        val mouthW = headRadius * 0.75f
        val mouthH = headRadius * 0.55f

        drawRoundRect(
            color = Color(0xFF1A0A0A),
            topLeft = Offset(mouthX, mouthY),
            size = Size(mouthW, mouthH),
            cornerRadius = CornerRadius(2f, 2f)
        )
        // Sharp crooked teeth
        drawLine(color = Color(0xFFFFF9C4), start = Offset(mouthX + 2f, mouthY), end = Offset(mouthX + 2f, mouthY + 3.5f), strokeWidth = 1.8f)
        drawLine(color = Color(0xFFFFF9C4), start = Offset(mouthX + 6f, mouthY), end = Offset(mouthX + 6f, mouthY + 4f), strokeWidth = 1.8f)
        drawLine(color = Color(0xFFFFF9C4), start = Offset(mouthX + 3.5f, mouthY + mouthH), end = Offset(mouthX + 3.5f, mouthY + mouthH - 3.5f), strokeWidth = 1.8f)

        // Spitter acidic drool
        if (z.type == ZombieType.SPITTER && !isHit) {
            drawLine(
                color = Color(0xFF76FF03),
                start = Offset(mouthX + 2f, mouthY + mouthH),
                end = Offset(mouthX + 1f, mouthY + mouthH + 8f + sin(z.walkAnimTimer * 4f) * 3f),
                strokeWidth = 2.2f
            )
        }

        // Terrifying asymmetrical Eyes!
        val eyeColor = when (z.type) {
            ZombieType.RUNNER -> Color(0xFFFF1744)
            ZombieType.SPITTER -> Color(0xFF76FF03)
            ZombieType.COLOSSUS -> Color(0xFFFF1744)
            else -> Color(0xFFFFEB3B)
        }

        // Left eye (bulging, glowing)
        val eyeCenterX = headX - headRadius * 0.42f
        val eyeCenterY = headY - headRadius * 0.15f
        glow(eyeCenterX, eyeCenterY, 11f, eyeColor.copy(alpha = 0.55f))
        drawCircle(color = eyeColor, center = Offset(eyeCenterX, eyeCenterY), radius = 2.8f)
        drawCircle(color = Color(0xFF1B0000), center = Offset(eyeCenterX - 0.7f, eyeCenterY), radius = 1.2f)

        // Right eye (sunken, darkened hollow socket)
        drawCircle(color = Color(0xFF1A1A1A), center = Offset(headX + 2f, eyeCenterY + 1f), radius = 2.2f)

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
