package com.example.game.ui

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.withTransform
import com.example.game.model.Balance
import com.example.game.model.HeroGeometry
import com.example.game.model.WeaponType
import com.example.game.model.Zombie
import com.example.game.model.ZombieType
import kotlin.math.max

private val menuZombies = listOf(
    Zombie(1, ZombieType.NORMAL, 0f, 1f),
    Zombie(2, ZombieType.BRUTE, 0f, 1f),
    Zombie(3, ZombieType.RUNNER, 0f, 1f),
    Zombie(4, ZombieType.NORMAL, 0f, 1f)
)

/**
 * The hallway as a living background for the main menu: Mara waits at the right end with her shotgun,
 * Nina stands by the door, and a line of zombies keeps shuffling in from the dark on the left.
 * The scene is drawn in world units, like the game itself, so the menu looks like the real thing.
 */
internal fun DrawScope.drawMenuScene(widthPx: Float, heightPx: Float) {
    if (widthPx <= 0f || heightPx <= 0f) return
    val s = heightPx / Balance.WORLD_H
    val w = Balance.WORLD_H * widthPx / heightPx
    val t = sceneTime()
    val floorY = Balance.FLOOR_Y
    val heroX = w - Balance.HERO_FROM_RIGHT
    val doorCx = w - Balance.DOOR_FROM_RIGHT

    withTransform({ scale(s, s, Offset.Zero) }) {
        drawHallway(w, floorY, doorCx, t)

        // Nina waits beside the door
        with(GameRenderer) {
            drawGirlSprite(Sprites.GIRL[1], doorCx, floorY, false)
        }

        // the zombies loop along the hallway but never get anywhere near her
        val track = max(120f, heroX - 170f)
        for ((i, z) in menuZombies.withIndex()) {
            val speed = z.type.speed * 0.55f
            val phase = (t * speed + i * track / menuZombies.size) % track
            z.x = -30f + phase
            z.walkTimer = t * z.type.speed * 0.11f * 0.55f + i
            with(GameRenderer) { drawZombie(z, floorY) }
        }

        // Mara, shotgun up
        val shoulder = Offset(
            heroX + Balance.PX,
            floorY - HeroGeometry.GROUND_ROW * Balance.PX + HeroGeometry.SHOULDER_ROW * Balance.PX
        )
        with(GameRenderer) {
            drawHeroPose(heroX, floorY, shoulder, WeaponType.SHOTGUN, 0.02f)
        }

        drawHallwayShade(w)
    }
}
