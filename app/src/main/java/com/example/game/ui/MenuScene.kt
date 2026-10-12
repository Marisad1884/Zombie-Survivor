package com.example.game.ui

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.withTransform
import com.example.game.model.Zombie
import com.example.game.model.ZombieType
import kotlin.math.sin

/**
 * Animated backdrop for the main menu: the same swamp as the game, with the heroine sitting on
 * a dock next to a lantern while a few zombies shuffle through the water in the distance.
 */

private class MenuZombie(val zombie: Zombie, val fx: Float, val fy: Float, val scale: Float)

private fun menuZombie(id: Long, type: ZombieType, fx: Float, fy: Float, scale: Float) =
    MenuZombie(
        Zombie(id = id, type = type, x = 0f, y = 0f, currentHp = type.maxHp, maxHp = type.maxHp),
        fx,
        fy,
        scale
    )

// Far ones first so nearer ones draw on top
private val MENU_ZOMBIES = listOf(
    menuZombie(11L, ZombieType.WALKER, 0.50f, 0.56f, 0.72f),
    menuZombie(4L, ZombieType.TANK, 0.58f, 0.60f, 0.78f),
    menuZombie(7L, ZombieType.RUNNER, 0.47f, 0.66f, 0.88f),
    menuZombie(2L, ZombieType.WALKER, 0.56f, 0.72f, 1.0f)
)

internal fun DrawScope.drawMenuScene(w: Float, h: Float) {
    val t = sceneTime()
    val waterTop = h * 0.40f

    drawNightSky(w, h, t)
    drawFarShore(w, h, waterTop, t)
    drawSwampWaterLayer(w, h, waterTop, t)
    drawWaterDetails(w, h, waterTop, t)

    // zombies loitering in the water, drifting very slowly toward the dock
    MENU_ZOMBIES.forEach { m ->
        val z = m.zombie
        z.x = w * m.fx + sin(t * 0.25f + z.id) * 8f
        z.y = h * m.fy
        z.walkAnimTimer = t * 2.4f + z.id
        withTransform({ scale(scaleX = m.scale, scaleY = m.scale, pivot = Offset(z.x, z.y)) }) {
            drawOval(
                color = Color(0x44000000),
                topLeft = Offset(z.x - z.type.width * 0.55f, z.y - 6f),
                size = Size(z.type.width * 1.1f, 14f)
            )
            drawHumanoidZombie(z, false, t)
        }
    }

    // low mist so the far ones sit back in the distance
    drawRect(
        brush = Brush.verticalGradient(
            colors = listOf(Color.Transparent, Color(0x33405F52), Color.Transparent),
            startY = h * 0.52f,
            endY = h * 0.78f
        ),
        topLeft = Offset(0f, h * 0.52f),
        size = Size(w, h * 0.26f)
    )

    // dock
    val porchY = h * 0.80f
    val dockX = w * 0.60f
    val plankH = (h * 0.035f).coerceAtLeast(14f)
    for (i in 0..2) {
        val px = dockX + 40f + i * (w - dockX) * 0.36f
        drawRect(Color(0xFF231810), Offset(px, porchY), Size(12f, h - porchY + 6f))
        drawOval(Color(0x33B8E0D8), Offset(px - 8f, h * 0.93f), Size(28f, 7f))
    }
    drawRect(Color(0xFF5A4331), Offset(dockX, porchY), Size(w - dockX + 12f, plankH))
    drawRect(Color(0xFF3B2A1D), Offset(dockX, porchY + plankH - 4f), Size(w - dockX + 12f, 4f))
    var sx = dockX + 30f
    var k = 0
    while (sx < w) {
        drawLine(Color(0xFF2A1D13), Offset(sx, porchY), Offset(sx, porchY + plankH - 3f), 1.6f)
        drawLine(Color(0x22FFFFFF), Offset(sx + 2f, porchY + 2f), Offset(sx + 22f + hash01(k, 71f) * 20f, porchY + 2f), 1.2f)
        sx += 46f
        k++
    }
    drawLine(Color(0x33FFFFFF), Offset(dockX, porchY + 1f), Offset(w, porchY + 1f), 1.4f)

    // the heroine, scaled up so she reads as the star of the screen
    val sc = (h / 420f).coerceIn(1f, 2.4f)
    val bodyX = w * 0.80f
    val bodyY = porchY - 54f
    withTransform({ scale(scaleX = sc, scaleY = sc, pivot = Offset(bodyX, porchY)) }) {
        drawHeroine(bodyX, bodyY, porchY, t, 0f)

        // forearms resting on her knees, gloved hands clasped
        val sleeve = Color(0xFF3A4733)
        drawLine(sleeve, Offset(bodyX + 2f, bodyY + 9f), Offset(bodyX + 9f, bodyY + 25f), 6.5f, StrokeCap.Round)
        drawLine(sleeve, Offset(bodyX + 9f, bodyY + 25f), Offset(bodyX + 17f, bodyY + 33f), 5.5f, StrokeCap.Round)
        drawCircle(Color(0xFF1B1511), 3.6f, Offset(bodyX + 18f, bodyY + 33.5f))

        // lantern on the planks
        val flick = 0.82f + 0.18f * sin(t * 7.3f) * sin(t * 2.7f + 1f)
        val lx = bodyX + 46f
        val ly = porchY
        glow(lx, ly - 9f, 70f, Color(0xFFFFB347).copy(alpha = 0.42f * flick))
        drawRect(Color(0xFF1B1410), Offset(lx - 5f, ly - 15f), Size(10f, 3f))
        drawRect(Color(0xFFE6A545).copy(alpha = 0.9f * flick), Offset(lx - 4f, ly - 12f), Size(8f, 10f))
        drawRect(Color(0xFF1B1410), Offset(lx - 5f, ly - 2f), Size(10f, 2.5f))
        drawLine(Color(0xFF1B1410), Offset(lx - 3f, ly - 15f), Offset(lx, ly - 21f), 1.3f)
        drawLine(Color(0xFF1B1410), Offset(lx + 3f, ly - 15f), Offset(lx, ly - 21f), 1.3f)
    }

    drawForegroundFrame(w, h, t)

    // darken the left side so the title and buttons stay readable
    drawRect(
        brush = Brush.horizontalGradient(
            colors = listOf(Color(0xEB040A08), Color(0x99040A08), Color.Transparent),
            startX = 0f,
            endX = w * 0.62f
        ),
        size = Size(w * 0.62f, h)
    )

    drawNightTint(w, h)
    drawVignette(w, h)
}
