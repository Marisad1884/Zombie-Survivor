package com.example.game.ui

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import com.example.game.model.Balance
import kotlin.math.sin

/** The inside of an old wooden house: planked walls, a worn floor, a window, a picture, lamps and a door. */

internal fun sceneTime(): Float = (System.currentTimeMillis() % 10_000_000L) / 1000f

/** Cheap deterministic noise in 0..1, so the planks look the same every frame. */
internal fun hash01(i: Int, salt: Int): Float {
    var h = i * 374761393 + salt * 668265263
    h = (h xor (h ushr 13)) * 1274126177.toInt()
    return ((h xor (h ushr 16)) and 0xFFFF) / 65535f
}

private val PLANK_COLORS = longArrayOf(0xFF8E4D20, 0xFF84461C, 0xFF92532A, 0xFF7B4119)

internal fun DrawScope.drawGlow(cx: Float, cy: Float, radius: Float, color: Color) {
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(color, color.copy(alpha = color.alpha * 0.35f), Color.Transparent),
            center = Offset(cx, cy),
            radius = radius
        ),
        radius = radius,
        center = Offset(cx, cy),
        blendMode = BlendMode.Plus
    )
}

internal fun DrawScope.drawHallway(w: Float, floorY: Float, doorCx: Float, t: Float) {
    val h = Balance.WORLD_H

    // ---- planked wall ----
    val plankW = 20f
    var i = 0
    var x = 0f
    while (x < w) {
        drawRect(Color(PLANK_COLORS[(hash01(i, 1) * PLANK_COLORS.size).toInt().coerceIn(0, PLANK_COLORS.size - 1)]), Offset(x, 0f), Size(plankW + 0.5f, floorY))
        // seam, highlight and a bit of grain
        drawRect(Color(0xFF4B240D), Offset(x, 0f), Size(1.6f, floorY))
        drawRect(Color(0x22FFFFFF), Offset(x + 2.6f, 0f), Size(1.2f, floorY))
        for (g in 0..1) {
            val gx = x + 5f + hash01(i, 10 + g) * 11f
            val gy = hash01(i, 20 + g) * (floorY - 90f)
            drawRect(Color(0x1A000000), Offset(gx, gy), Size(1f, 40f + hash01(i, 30 + g) * 60f))
        }
        // knot
        if (hash01(i, 40) > 0.72f) {
            val ky = 40f + hash01(i, 41) * (floorY - 100f)
            drawRect(Color(0xFF5A2B10), Offset(x + 8f, ky), Size(5f, 7f))
            drawRect(Color(0xFF3F1D09), Offset(x + 9f, ky + 2f), Size(3f, 3f))
        }
        // nails
        drawRect(Color(0xFF3A1B09), Offset(x + 9f, 74f), Size(2f, 2f))
        drawRect(Color(0xFF3A1B09), Offset(x + 9f, floorY - 22f), Size(2f, 2f))
        x += plankW
        i++
    }

    // ---- window with a night sky ----
    val wx = w * 0.27f
    drawRect(Color(0xFF3B1D0B), Offset(wx - 30f, 84f), Size(60f, 78f))
    drawRect(Color(0xFF16233F), Offset(wx - 25f, 89f), Size(50f, 68f))
    drawRect(
        brush = Brush.verticalGradient(listOf(Color(0xFF0E1830), Color(0xFF223A63)), startY = 89f, endY = 157f),
        topLeft = Offset(wx - 25f, 89f),
        size = Size(50f, 68f)
    )
    drawCircle(Color(0xFFE9EDF7), 6f, Offset(wx + 10f, 108f))
    drawCircle(Color(0xFF16233F), 5f, Offset(wx + 12f, 106f))
    for (s in 0 until 6) {
        drawRect(Color(0xCCFFFFFF), Offset(wx - 20f + hash01(s, 50) * 36f, 94f + hash01(s, 51) * 30f), Size(1.6f, 1.6f))
    }
    drawRect(Color(0xFF3B1D0B), Offset(wx - 1.5f, 89f), Size(3f, 68f))
    drawRect(Color(0xFF3B1D0B), Offset(wx - 25f, 121f), Size(50f, 3f))
    drawRect(Color(0xFF5A2F14), Offset(wx - 33f, 160f), Size(66f, 5f))

    // ---- framed picture ----
    val px = w * 0.56f
    drawRect(Color(0xFF2B1507), Offset(px - 22f, 96f), Size(44f, 34f))
    drawRect(Color(0xFFD6C591), Offset(px - 18f, 100f), Size(36f, 26f))
    drawRect(Color(0xFF8DA9B8), Offset(px - 18f, 100f), Size(36f, 12f))
    drawRect(Color(0xFF4F6B3C), Offset(px - 18f, 112f), Size(36f, 14f))
    drawRect(Color(0xFF3A5230), Offset(px - 10f, 108f), Size(14f, 8f))

    // ---- wall lamps with a gentle flicker ----
    val lamps = floatArrayOf(w * 0.12f, w * 0.43f, w * 0.72f)
    for ((n, lx) in lamps.withIndex()) {
        val flick = 0.85f + 0.15f * sin(t * (6f + n) + n * 1.7f) * sin(t * 2.3f + n)
        drawRect(Color(0xFF2A1608), Offset(lx - 2f, 118f), Size(4f, 10f))
        drawRect(Color(0xFFE8C26A), Offset(lx - 7f, 104f), Size(14f, 14f))
        drawRect(Color(0xFFFFE9A8), Offset(lx - 4f, 107f), Size(8f, 8f))
        drawRect(Color(0xFF3A1B09), Offset(lx - 8f, 102f), Size(16f, 3f))
        drawGlow(lx, 112f, 90f, Color(0xFFFFB347).copy(alpha = 0.26f * flick))
    }

    // ---- baseboard and floor ----
    drawRect(Color(0xFF5A2C10), Offset(0f, floorY - 9f), Size(w, 11f))
    drawRect(Color(0xFF7A4220), Offset(0f, floorY - 9f), Size(w, 1.6f))
    drawRect(Color(0xFFB97C3E), Offset(0f, floorY + 2f), Size(w, h - floorY))
    drawRect(Color(0xFFD49A5A), Offset(0f, floorY + 2f), Size(w, 1.6f))
    drawRect(Color(0xFF8D5A2A), Offset(0f, floorY + 12f), Size(w, 1.4f))
    var jx = 0f
    var k = 0
    while (jx < w) {
        drawRect(Color(0xFF8D5A2A), Offset(jx + (k % 2) * 30f, floorY + 2f), Size(1.4f, 10f))
        drawRect(Color(0xFF8D5A2A), Offset(jx + ((k + 1) % 2) * 30f, floorY + 13f), Size(1.4f, h - floorY))
        jx += 60f
        k++
    }

    // ---- the door she lives behind ----
    val dw = Balance.DOOR_WIDTH
    val dh = Balance.DOOR_HEIGHT
    drawRect(Color(0xFF3A1D0B), Offset(doorCx - dw / 2f - 6f, floorY - dh - 6f), Size(dw + 12f, dh + 6f))
    drawRect(Color(0xFF5A2F14), Offset(doorCx - dw / 2f - 6f, floorY - dh - 6f), Size(dw + 12f, 2f))
    drawRect(
        brush = Brush.verticalGradient(listOf(Color(0xFF0B0705), Color(0xFF241810)), startY = floorY - dh, endY = floorY),
        topLeft = Offset(doorCx - dw / 2f, floorY - dh),
        size = Size(dw, dh)
    )
    drawRect(Color(0xFF3A2616), Offset(doorCx - dw / 2f, floorY - 6f), Size(dw, 6f))
}

/** Dark corners and the creeping dark on the side the zombies come from. */
internal fun DrawScope.drawHallwayShade(w: Float) {
    val h = Balance.WORLD_H
    drawRect(
        brush = Brush.horizontalGradient(listOf(Color(0x99000000), Color.Transparent), startX = 0f, endX = w * 0.38f),
        size = Size(w * 0.38f, h)
    )
    drawRect(
        brush = Brush.verticalGradient(listOf(Color(0x66000000), Color.Transparent), startY = 0f, endY = 110f),
        size = Size(w, 110f)
    )
    drawRect(
        brush = Brush.horizontalGradient(listOf(Color.Transparent, Color(0x55000000)), startX = w * 0.9f, endX = w),
        topLeft = Offset(w * 0.9f, 0f),
        size = Size(w * 0.1f, h)
    )
}
