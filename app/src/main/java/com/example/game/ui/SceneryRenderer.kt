package com.example.game.ui

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import kotlin.math.absoluteValue
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin

/**
 * Background scenery for the swamp: night sky, moon, clouds, cypress trees, fireflies,
 * the water with its moonlight reflection, fog, lily pads and a final vignette.
 *
 * Everything is deterministic (no allocations of random numbers per frame) so the scene never
 * flickers and stays cheap to draw.
 */

/** Seconds since an arbitrary origin, small enough to keep Float precision for animations. */
internal fun sceneTime(): Float = (System.currentTimeMillis() % 10_000_000L) / 1000f

/** Cheap deterministic pseudo random number in 0..1 from an index and a salt. */
internal fun hash01(i: Int, salt: Float = 0f): Float {
    val v = sin(i * 12.9898f + salt * 78.233f) * 43758.5453f
    return v - floor(v)
}

internal fun DrawScope.drawNightSky(w: Float, h: Float, t: Float) {
    val horizon = h * 0.45f

    // Sky gradient: nearly black at the top, cold green glow near the horizon
    drawRect(
        brush = Brush.verticalGradient(
            colors = listOf(
                Color(0xFF02060A),
                Color(0xFF061219),
                Color(0xFF0B2220),
                Color(0xFF14352B)
            ),
            startY = 0f,
            endY = horizon
        ),
        size = Size(w, horizon)
    )

    // Stars
    for (i in 0 until 46) {
        val sx = hash01(i, 1f) * w
        val sy = hash01(i, 2f) * h * 0.30f
        val twinkle = 0.35f + (sin(t * (1.2f + hash01(i, 3f) * 2f) + i).absoluteValue) * 0.65f
        val big = hash01(i, 4f) > 0.82f
        drawCircle(
            color = Color.White.copy(alpha = (0.25f + twinkle * 0.6f) * (1f - sy / (h * 0.36f)).coerceIn(0.2f, 1f)),
            center = Offset(sx, sy),
            radius = if (big) 1.9f else 1.1f
        )
        if (big) {
            // little cross sparkle
            val len = 3.5f * twinkle
            drawLine(Color.White.copy(alpha = 0.35f * twinkle), Offset(sx - len, sy), Offset(sx + len, sy), 1f)
            drawLine(Color.White.copy(alpha = 0.35f * twinkle), Offset(sx, sy - len), Offset(sx, sy + len), 1f)
        }
    }

    // Moon
    val moonX = w * 0.74f
    val moonY = h * 0.16f
    val moonR = min(w, h) * 0.085f

    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(Color(0x55FFFDE7), Color(0x22C8E6C9), Color.Transparent),
            center = Offset(moonX, moonY),
            radius = moonR * 4.2f
        ),
        center = Offset(moonX, moonY),
        radius = moonR * 4.2f
    )
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(Color(0x99FFFDE7), Color(0x33FFF9C4), Color.Transparent),
            center = Offset(moonX, moonY),
            radius = moonR * 2.2f
        ),
        center = Offset(moonX, moonY),
        radius = moonR * 2.2f
    )
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(Color(0xFFFFFFF0), Color(0xFFFFF59D), Color(0xFFE6D98A)),
            center = Offset(moonX - moonR * 0.25f, moonY - moonR * 0.25f),
            radius = moonR * 1.4f
        ),
        center = Offset(moonX, moonY),
        radius = moonR
    )
    // Craters (soft)
    val crater = Color(0x33A89B4A)
    drawCircle(crater, moonR * 0.22f, Offset(moonX - moonR * 0.28f, moonY - moonR * 0.18f))
    drawCircle(crater, moonR * 0.17f, Offset(moonX + moonR * 0.38f, moonY + moonR * 0.26f))
    drawCircle(crater, moonR * 0.13f, Offset(moonX + moonR * 0.08f, moonY - moonR * 0.34f))
    drawCircle(crater, moonR * 0.10f, Offset(moonX - moonR * 0.1f, moonY + moonR * 0.42f))

    // Faint light shafts coming down from the moon through the haze
    for (i in 0..1) {
        val spread = 0.10f + i * 0.09f
        val shaft = Path().apply {
            moveTo(moonX - moonR * 0.4f, moonY + moonR * 0.6f)
            lineTo(moonX + moonR * 0.4f, moonY + moonR * 0.6f)
            lineTo(moonX + w * spread + 60f, horizon)
            lineTo(moonX - w * spread - 60f, horizon)
            close()
        }
        drawPath(
            path = shaft,
            brush = Brush.verticalGradient(
                colors = listOf(Color(0x14FFF9C4), Color.Transparent),
                startY = moonY,
                endY = horizon
            )
        )
    }

    // Drifting clouds that glow along their upper edge
    for (i in 0 until 4) {
        val speed = 5f + i * 2.5f
        val span = w + 500f
        val cx = ((t * speed + i * 330f) % span) - 250f
        val cy = h * (0.07f + i * 0.065f)
        val cw = 190f + i * 45f
        drawOval(
            color = Color(0x2A0A1A20),
            topLeft = Offset(cx - cw / 2f, cy),
            size = Size(cw, 26f + i * 4f)
        )
        drawOval(
            color = Color(0x1AB0BEC5),
            topLeft = Offset(cx - cw * 0.4f, cy - 3f),
            size = Size(cw * 0.8f, 14f)
        )
    }

    // Far treeline silhouette (parallax layer behind the cypresses)
    val line = Path().apply {
        moveTo(0f, horizon)
        var x = 0f
        while (x <= w + 20f) {
            val y = h * 0.37f - (sin(x * 0.021f) * 9f + sin(x * 0.0073f + 1.7f) * 14f).absoluteValue
            lineTo(x, y)
            x += 18f
        }
        lineTo(w, horizon)
        close()
    }
    drawPath(line, color = Color(0xFF071410))

    // Cypress silhouettes with swaying Spanish moss
    drawCypressTree(w * 0.36f, h * 0.395f, 130f, 190f, t, 0)
    drawCypressTree(w * 0.55f, h * 0.395f, 105f, 150f, t, 1)
    drawCypressTree(w * 0.70f, h * 0.395f, 90f, 130f, t, 2)
    drawCypressTree(w * 0.89f, h * 0.395f, 165f, 235f, t, 3)

    // Fireflies with soft pulsing halos
    for (i in 0 until 14) {
        val baseX = w * 0.28f + hash01(i, 5f) * w * 0.68f
        val baseY = h * 0.22f + hash01(i, 6f) * h * 0.40f
        val fx = baseX + sin(t * 0.55f + i * 1.7f) * 38f
        val fy = baseY + sin(t * 0.8f + i * 2.3f) * 22f
        val pulse = (0.35f + (sin(t * 2.2f + i * 3.1f) * 0.5f + 0.5f) * 0.65f)
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(Color(0x8876FF03).copy(alpha = 0.55f * pulse), Color.Transparent),
                center = Offset(fx, fy),
                radius = 13f
            ),
            center = Offset(fx, fy),
            radius = 13f
        )
        drawCircle(Color(0xFFE6FFB0).copy(alpha = 0.55f + 0.45f * pulse), 2.1f, Offset(fx, fy))
    }
}

private fun DrawScope.drawCypressTree(x: Float, baseY: Float, width: Float, height: Float, t: Float, seed: Int) {
    val dark = Color(0xFF050D0A)
    val mid = Color(0xFF0A1913)
    val trunkW = width * 0.2f

    // Buttressed trunk (wider at the waterline)
    val trunk = Path().apply {
        moveTo(x - trunkW * 0.9f, baseY)
        lineTo(x - trunkW * 0.4f, baseY - height * 0.55f)
        lineTo(x + trunkW * 0.4f, baseY - height * 0.55f)
        lineTo(x + trunkW * 0.9f, baseY)
        close()
    }
    drawPath(trunk, color = dark)

    // Crown built from overlapping ovals
    drawOval(dark, Offset(x - width * 0.5f, baseY - height), Size(width, height * 0.62f))
    drawOval(dark, Offset(x - width * 0.38f, baseY - height * 1.06f), Size(width * 0.62f, height * 0.4f))
    drawOval(mid, Offset(x - width * 0.34f, baseY - height * 0.88f), Size(width * 0.62f, height * 0.36f))

    // Hanging Spanish moss that sways slowly
    for (i in -3..3) {
        val mx = x + i * (width * 0.14f)
        val len = height * (0.18f + hash01(i + seed * 11, 7f) * 0.18f)
        val sway = sin(t * 0.9f + i * 0.9f + seed) * 4f
        val topY = baseY - height * 0.5f + (i.absoluteValue) * 4f
        val moss = Path().apply {
            moveTo(mx, topY)
            quadraticTo(mx + sway, topY + len * 0.5f, mx + sway * 1.6f, topY + len)
        }
        drawPath(
            path = moss,
            color = Color(0xAA1B3A2D),
            style = Stroke(width = 3f, cap = StrokeCap.Round)
        )
    }
}

private val LILIES = arrayOf(
    floatArrayOf(0.42f, 0.55f, 76f, 0f),
    floatArrayOf(0.68f, 0.76f, 96f, 1f),
    floatArrayOf(0.84f, 0.48f, 64f, 2f),
    floatArrayOf(0.30f, 0.82f, 84f, 3f),
    floatArrayOf(0.58f, 0.64f, 58f, 4f),
    floatArrayOf(0.92f, 0.86f, 70f, 5f)
)

internal fun DrawScope.drawSwampWaterLayer(w: Float, h: Float, waterTop: Float, t: Float) {
    val waterH = h - waterTop

    // Deep water gradient
    drawRect(
        brush = Brush.verticalGradient(
            colors = listOf(
                Color(0xFF17352B),
                Color(0xFF0F271F),
                Color(0xFF0A1B15),
                Color(0xFF050E0B)
            ),
            startY = waterTop,
            endY = h
        ),
        topLeft = Offset(0f, waterTop),
        size = Size(w, waterH)
    )

    // Moonlight reflection column, shimmering
    val moonX = w * 0.74f
    for (i in 0 until 16) {
        val p = i / 15f
        val y = waterTop + 10f + p * waterH * 0.62f
        val shimmer = 0.55f + 0.45f * sin(t * 2.4f + i * 1.3f)
        val half = (16f + p * 70f) * shimmer
        val xOff = sin(t * 0.9f + i * 0.7f) * (6f + p * 10f)
        drawLine(
            color = Color(0xFFFFF9C4).copy(alpha = (0.30f - p * 0.22f).coerceAtLeast(0.04f) * shimmer),
            start = Offset(moonX - half + xOff, y),
            end = Offset(moonX + half + xOff, y),
            strokeWidth = 2.2f + p * 2.2f,
            cap = StrokeCap.Round
        )
    }

    // Perspective wave strokes: denser and thinner near the horizon
    for (i in 0 until 8) {
        val p = (i + 1) / 9f
        val y = waterTop + (p * p) * waterH
        val drift = sin(t * 0.6f + i * 1.1f) * 14f
        var x = -((t * (6f + i * 2f)) % 110f)
        while (x < w) {
            val dash = 26f + p * 34f
            drawLine(
                color = Color(0xFF9FD8CF).copy(alpha = 0.05f + p * 0.07f),
                start = Offset(x + drift, y),
                end = Offset(x + drift + dash, y),
                strokeWidth = 1.2f + p * 1.6f,
                cap = StrokeCap.Round
            )
            x += 110f
        }
    }

    // Lily pads, flowers and expanding ripples
    for (l in LILIES) {
        val lx = w * l[0]
        val ly = h * l[1] + sin(t * 0.8f + l[3]) * 1.6f
        val lw = l[2]
        val lh = lw * 0.28f
        val seed = l[3].toInt()

        // ripple
        val ph = (t * 0.35f + seed * 0.23f) % 1f
        drawOval(
            color = Color(0xFF9FD8CF).copy(alpha = (1f - ph) * 0.18f),
            topLeft = Offset(lx - lw * (0.5f + ph * 0.5f), ly - lh * (0.5f + ph * 0.5f)),
            size = Size(lw * (1f + ph), lh * (1f + ph)),
            style = Stroke(width = 1.4f)
        )
        // pad + highlight
        drawOval(Color(0xFF1E5A2B).copy(alpha = 0.75f), Offset(lx - lw / 2f, ly - lh / 2f), Size(lw, lh))
        drawOval(Color(0xFF3F8F4C).copy(alpha = 0.45f), Offset(lx - lw * 0.38f, ly - lh * 0.42f), Size(lw * 0.62f, lh * 0.5f))
        // notch
        drawLine(Color(0xFF0A1B15), Offset(lx, ly), Offset(lx + lw * 0.5f, ly - lh * 0.15f), 2f)

        if (seed % 2 == 0) {
            val petal = if (seed % 4 == 0) Color(0xFFFF80AB) else Color(0xFFFFD54F)
            drawCircle(petal.copy(alpha = 0.35f), 7f, Offset(lx, ly - 2f))
            drawCircle(petal, 3.6f, Offset(lx, ly - 2f))
        }
    }

    // Drifting marsh fog in two layers (back layer slower)
    for (layer in 0..1) {
        val speed = 7f + layer * 6f
        val span = w + 600f
        val fx = ((t * speed + layer * 420f) % span) - 300f
        val fy = waterTop - 6f + layer * 26f
        drawOval(
            brush = Brush.verticalGradient(
                colors = listOf(Color.Transparent, Color(0x2A9CCC9F), Color.Transparent),
                startY = fy,
                endY = fy + 70f
            ),
            topLeft = Offset(fx - 320f, fy),
            size = Size(640f, 70f)
        )
    }
    // Low fog hugging the horizon line
    drawRect(
        brush = Brush.verticalGradient(
            colors = listOf(Color(0x3A4CAF50), Color(0x151B5E20), Color.Transparent),
            startY = waterTop - 28f,
            endY = waterTop + 80f
        ),
        topLeft = Offset(0f, waterTop - 28f),
        size = Size(w, 108f)
    )
}

/** Dark edges that pull the eye to the center and add mood. Drawn last, on top of everything. */
internal fun DrawScope.drawVignette(w: Float, h: Float) {
    drawRect(
        brush = Brush.radialGradient(
            0.0f to Color.Transparent,
            0.62f to Color.Transparent,
            1.0f to Color(0xB0020806),
            center = Offset(w / 2f, h / 2f),
            radius = max(w, h) * 0.72f
        ),
        size = Size(w, h)
    )
}

/** Subtle cold tint over the whole scene so the sprites sit in the same night lighting. */
internal fun DrawScope.drawNightTint(w: Float, h: Float) {
    drawRect(color = Color(0x120A2A3A), size = Size(w, h))
}
