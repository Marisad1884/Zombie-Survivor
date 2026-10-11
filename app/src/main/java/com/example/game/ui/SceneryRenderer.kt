package com.example.game.ui

import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import com.example.game.engine.GameEngine
import kotlin.math.absoluteValue
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin

/**
 * Background scenery for the swamp: night sky, moon, clouds, cypress trees, fireflies,
 * the water with its moonlight reflection, fog, lily pads, cabin and a final vignette.
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

/** Rustic log cabin, porch deck, waist-high barricade and sandbags. */
internal fun DrawScope.drawCabinAndPorch(
    houseWidth: Float,
    h: Float,
    waterTop: Float,
    engine: GameEngine
) {
    val porchY = engine.porchY

    // 1. Heavy Weathered Stilt Pilings standing deep in swamp water
    val pilingColor = Color(0xFF261610)
    val algaeColor = Color(0x882E7D32)
    val pilings = listOf(houseWidth * 0.22f, houseWidth * 0.58f, houseWidth * 0.90f)
    pilings.forEachIndexed { idx, px ->
        val pWidth = 14f + idx * 2f
        drawLine(
            color = pilingColor,
            start = Offset(px, porchY + 14f),
            end = Offset(px, h * 0.98f),
            strokeWidth = pWidth
        )
        // Green swamp algae moss staining around waterline
        drawLine(
            color = algaeColor,
            start = Offset(px, porchY + 18f),
            end = Offset(px, porchY + 70f),
            strokeWidth = pWidth + 2f
        )
        // Water ripples around stilt base
        drawOval(
            color = Color(0x3380CBC4),
            topLeft = Offset(px - 14f, porchY + 30f),
            size = Size(28f, 8f),
            style = Stroke(width = 1.5f)
        )
    }

    // 2. Main Rustic Log Cabin Wall ("Cabaña de Troncos")
    val cabinLeft = 0f
    val cabinTop = porchY - (h * 0.35f)
    val cabinRight = houseWidth * 0.72f
    val cabinHeight = porchY - cabinTop

    // Dark background behind cabin
    drawRect(
        color = Color(0xFF2A1810),
        topLeft = Offset(cabinLeft, cabinTop),
        size = Size(cabinRight, cabinHeight)
    )

    // Stacked Horizontal Rounded Wood Logs
    val numLogs = 8
    val logHeight = cabinHeight / numLogs
    for (i in 0 until numLogs) {
        val logY = cabinTop + i * logHeight
        // Base rounded log body
        drawRoundRect(
            color = if (i % 2 == 0) Color(0xFF5D4037) else Color(0xFF4E342E),
            topLeft = Offset(cabinLeft, logY + 1f),
            size = Size(cabinRight, logHeight - 1.5f),
            cornerRadius = CornerRadius(4f, 4f)
        )
        // Log 3D upper highlight reflection (gives cylindrical log appearance)
        drawRect(
            color = Color(0xFF6D4C41),
            topLeft = Offset(cabinLeft, logY + 2f),
            size = Size(cabinRight, logHeight * 0.35f)
        )
        // Dark chinking/mortar line between logs
        drawLine(
            color = Color(0xFF1B0F09),
            start = Offset(cabinLeft, logY + logHeight),
            end = Offset(cabinRight, logY + logHeight),
            strokeWidth = 2.5f
        )
        // Wood knot on logs
        if (i % 3 == 1) {
            drawCircle(
                color = Color(0xFF24130B),
                center = Offset(cabinRight * 0.35f + (i * 12f), logY + logHeight * 0.5f),
                radius = 3.2f
            )
        }

        // Authentic Notched Log Ends protruding at the cabin corner joint
        drawOval(
            color = Color(0xFF5D4037),
            topLeft = Offset(cabinRight - 4f, logY + 1f),
            size = Size(16f, logHeight - 2f)
        )
        drawOval(
            color = Color(0xFF3E2723),
            topLeft = Offset(cabinRight + 1f, logY + 3f),
            size = Size(9f, logHeight - 6f)
        )
        drawCircle(
            color = Color(0xFF1B0F09),
            center = Offset(cabinRight + 5.5f, logY + logHeight * 0.5f),
            radius = 1.8f
        )
    }

    // 3. Rustic Stone Chimney on Roof
    val chimneyW = 20f
    val chimneyH = 42f
    val chimneyX = cabinRight * 0.22f
    val chimneyY = cabinTop - 42f
    drawRoundRect(
        color = Color(0xFF455A64),
        topLeft = Offset(chimneyX, chimneyY),
        size = Size(chimneyW, chimneyH),
        cornerRadius = CornerRadius(2f, 2f)
    )
    for (c in 1..3) {
        drawLine(
            color = Color(0xFF263238),
            start = Offset(chimneyX, chimneyY + c * 10f),
            end = Offset(chimneyX + chimneyW, chimneyY + c * 10f),
            strokeWidth = 2f
        )
    }
    // Chimney smoke
    drawCircle(
        color = Color(0x33ECEFF1),
        center = Offset(chimneyX + chimneyW * 0.5f, chimneyY - 8f),
        radius = 6f
    )
    drawCircle(
        color = Color(0x22ECEFF1),
        center = Offset(chimneyX + chimneyW * 0.6f + 4f, chimneyY - 18f),
        radius = 9f
    )

    // 4. Overhanging Shingle/Timber Roof
    val roofPath = Path().apply {
        moveTo(0f, cabinTop - 42f)
        lineTo(cabinRight + houseWidth * 0.18f, cabinTop + 16f)
        lineTo(cabinRight + houseWidth * 0.14f, cabinTop + 32f)
        lineTo(0f, cabinTop - 18f)
        close()
    }
    drawPath(roofPath, color = Color(0xFF37474F))
    drawLine(
        color = Color(0xFF24130B),
        start = Offset(0f, cabinTop - 18f),
        end = Offset(cabinRight + houseWidth * 0.14f, cabinTop + 32f),
        strokeWidth = 4f
    )

    // 5. Automated Roof Sentry Turret if unlocked
    if (engine.defenseUpgrades.turretLevel > 0) {
        val turretBase = engine.roofTurretOffset
        drawRoundRect(
            color = Color(0xFF263238),
            topLeft = Offset(turretBase.x - 14f, turretBase.y - 8f),
            size = Size(28f, 16f),
            cornerRadius = CornerRadius(4f, 4f)
        )
        drawLine(
            color = Color(0xFF37474F),
            start = Offset(turretBase.x + 8f, turretBase.y - 3f),
            end = Offset(turretBase.x + 24f, turretBase.y - 3f),
            strokeWidth = 4f
        )
        drawLine(
            color = Color(0xFF37474F),
            start = Offset(turretBase.x + 8f, turretBase.y + 3f),
            end = Offset(turretBase.x + 24f, turretBase.y + 3f),
            strokeWidth = 4f
        )
        drawCircle(
            color = Color(0xFFFF1744),
            center = Offset(turretBase.x + 4f, turretBase.y),
            radius = 3f
        )
    }

    // 6. Rustic Front Door on Porch
    val doorW = 28f
    val doorH = 58f
    val doorX = cabinRight * 0.12f
    val doorY = porchY - doorH
    drawRect(
        color = Color(0xFF261610),
        topLeft = Offset(doorX - 2f, doorY - 2f),
        size = Size(doorW + 4f, doorH + 2f)
    )
    drawRect(
        color = Color(0xFF3E2723),
        topLeft = Offset(doorX, doorY),
        size = Size(doorW, doorH)
    )
    drawLine(
        color = Color(0xFF4E342E),
        start = Offset(doorX + 3f, doorY + 6f),
        end = Offset(doorX + doorW - 3f, doorY + doorH - 6f),
        strokeWidth = 3.5f
    )
    drawCircle(color = Color(0xFFFFB300), center = Offset(doorX + doorW - 5f, doorY + doorH * 0.55f), radius = 2.5f)

    // 7. Cabin Window with Warm Candlelight Glow & Shutters
    val winW = houseWidth * 0.24f
    val winH = winW * 0.90f
    val winX = cabinRight * 0.46f
    val winY = cabinTop + cabinHeight * 0.22f

    val flickerT = sceneTime()
    val candle = (0.88f + 0.12f * sin(flickerT * 7.3f) * sin(flickerT * 2.9f + 1f)).coerceIn(0.7f, 1f)

    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(
                Color(0xFFFFA000).copy(alpha = 0.67f * candle),
                Color(0xFFFFB300).copy(alpha = 0.2f * candle),
                Color.Transparent
            ),
            center = Offset(winX + winW / 2, winY + winH / 2),
            radius = winW * 2.0f
        ),
        center = Offset(winX + winW / 2, winY + winH / 2),
        radius = winW * 2.0f,
        blendMode = BlendMode.Plus
    )
    drawRoundRect(
        color = Color(0xFF3E2723),
        topLeft = Offset(winX - 12f, winY),
        size = Size(10f, winH),
        cornerRadius = CornerRadius(2f, 2f)
    )
    drawRoundRect(
        color = Color(0xFF3E2723),
        topLeft = Offset(winX + winW + 2f, winY),
        size = Size(10f, winH),
        cornerRadius = CornerRadius(2f, 2f)
    )
    drawRect(
        color = Color(0xFF1B0F09),
        topLeft = Offset(winX - 2.5f, winY - 2.5f),
        size = Size(winW + 5f, winH + 5f)
    )
    drawRect(
        color = Color(0xFFFFD54F),
        topLeft = Offset(winX, winY),
        size = Size(winW, winH)
    )
    drawLine(
        color = Color(0xFF3E2723),
        start = Offset(winX + winW / 2, winY),
        end = Offset(winX + winW / 2, winY + winH),
        strokeWidth = 2.5f
    )
    drawLine(
        color = Color(0xFF3E2723),
        start = Offset(winX, winY + winH / 2),
        end = Offset(winX + winW, winY + winH / 2),
        strokeWidth = 2.5f
    )

    // 8. Porch Roof Overhang Support Strut & Hanging Storm Lantern
    drawLine(
        color = Color(0xFF3E2723),
        start = Offset(cabinRight, cabinTop + cabinHeight * 0.35f),
        end = Offset(houseWidth * 0.88f, porchY - 50f),
        strokeWidth = 5f
    )
    val lanternX = cabinRight + 10f
    val lanternY = porchY - 52f
    drawLine(color = Color(0xFF1B0F09), start = Offset(lanternX, lanternY - 10f), end = Offset(lanternX, lanternY), strokeWidth = 2f)
    drawCircle(color = Color(0xFFFFB300), center = Offset(lanternX, lanternY + 6f), radius = 5.5f)
    drawCircle(color = Color(0xFFFFF176), center = Offset(lanternX, lanternY + 6f), radius = 3.5f)
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(Color(0x66FFC107), Color.Transparent),
            center = Offset(lanternX, lanternY + 6f),
            radius = 32f
        ),
        center = Offset(lanternX, lanternY + 6f),
        radius = 32f
    )

    // 9. Porch Wooden Deck Planks
    drawRect(
        color = Color(0xFF5D4037),
        topLeft = Offset(0f, porchY),
        size = Size(houseWidth, 18f)
    )
    drawLine(
        color = Color(0xFF24130B),
        start = Offset(0f, porchY + 18f),
        end = Offset(houseWidth, porchY + 18f),
        strokeWidth = 4f
    )
    for (p in 1..8) {
        val plankX = p * (houseWidth / 8.5f)
        drawLine(
            color = Color(0xFF3E2723),
            start = Offset(plankX, porchY),
            end = Offset(plankX, porchY + 18f),
            strokeWidth = 1.5f
        )
    }

    // 10. Barricade & Sandbags (Waist-high tactical defense fortification)
    val barricadeX = houseWidth - 14f
    val barricadeH = 46f
    val barricadeTop = porchY - barricadeH + 16f

    val maxHp = engine.defenseUpgrades.maxHouseHp
    val hpRatio = (engine.houseHp / maxHp).coerceIn(0f, 1f)
    val barricadeColor = if (hpRatio < 0.35f) Color(0xFF4E342E) else Color(0xFF6D4C41)

    drawRoundRect(
        color = barricadeColor,
        topLeft = Offset(barricadeX - 12f, barricadeTop),
        size = Size(26f, barricadeH),
        cornerRadius = CornerRadius(4f, 4f)
    )
    drawLine(
        color = Color(0xFF3E2723),
        start = Offset(barricadeX - 14f, barricadeTop + 14f),
        end = Offset(barricadeX + 14f, barricadeTop + 14f),
        strokeWidth = 6f
    )
    drawLine(
        color = Color(0xFF3E2723),
        start = Offset(barricadeX - 14f, barricadeTop + 32f),
        end = Offset(barricadeX + 14f, barricadeTop + 32f),
        strokeWidth = 6f
    )

    if (hpRatio < 0.65f) {
        drawLine(
            color = Color(0xFF1B0000),
            start = Offset(barricadeX - 6f, barricadeTop + 16f),
            end = Offset(barricadeX + 4f, barricadeTop + 28f),
            strokeWidth = 2.5f
        )
    }
    if (hpRatio < 0.35f) {
        drawLine(
            color = Color(0xFF1B0000),
            start = Offset(barricadeX - 8f, barricadeTop + 6f),
            end = Offset(barricadeX + 6f, barricadeTop + 18f),
            strokeWidth = 3f
        )
    }

    // Floating HP Bar on Canvas above Barricade for clear visibility
    val houseBarW = 70f
    val houseBarH = 6f
    val houseBarX = barricadeX - houseBarW / 2f
    val houseBarY = barricadeTop - 12f
    drawRoundRect(
        color = Color(0xCC000000),
        topLeft = Offset(houseBarX, houseBarY),
        size = Size(houseBarW, houseBarH),
        cornerRadius = CornerRadius(2.5f, 2.5f)
    )
    drawRoundRect(
        color = if (hpRatio > 0.4f) Color(0xFF4CAF50) else Color(0xFFFF5252),
        topLeft = Offset(houseBarX, houseBarY),
        size = Size(houseBarW * hpRatio, houseBarH),
        cornerRadius = CornerRadius(2.5f, 2.5f)
    )

    // Sandbags
    drawOval(
        color = Color(0xFF8D6E63),
        topLeft = Offset(barricadeX - 22f, porchY - 14f),
        size = Size(36f, 18f)
    )
    drawOval(
        color = Color(0xFF795548),
        topLeft = Offset(barricadeX - 10f, porchY - 26f),
        size = Size(34f, 16f)
    )

    // Barbed wire coils
    val wireLevel = engine.defenseUpgrades.barbedWireLevel
    if (wireLevel > 0) {
        val wireColor = Color(0xFFCFD8DC)
        for (i in 0 until (wireLevel * 2 + 1)) {
            val coilY = barricadeTop + 10f + i * 14f
            drawCircle(
                color = wireColor,
                center = Offset(barricadeX + 12f, coilY),
                radius = 8f,
                style = Stroke(width = 2.5f)
            )
        }
    }
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
