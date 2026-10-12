package com.example.game.ui

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import kotlin.math.absoluteValue
import kotlin.math.sin

/**
 * Extra scenery that gives the swamp depth and a "lived in" feel: a derelict shack on the far
 * shore, bats, a floating log, a half-sunken rowboat, rotten dock posts with a crow, cypress
 * knees, and a foreground frame of reeds and hanging moss.
 *
 * Three entry points are called from GameRenderer at different depths:
 *  - [drawFarShore]      right after the sky, before the water
 *  - [drawWaterDetails]  right after the water, before zombies
 *  - [drawForegroundFrame] last, just before the vignette
 */

private val SILHOUETTE = Color(0xFF060E0B)
private val SILHOUETTE_SOFT = Color(0xFF0A1713)
private val MOON_RIM = Color(0x22CFE8E0)

/** Derelict fishing shack on stilts, dead trees and bats near the horizon. */
internal fun DrawScope.drawFarShore(w: Float, h: Float, waterTop: Float, t: Float) {
    val s = h / 700f
    val base = waterTop + 2f

    // dead snags
    drawSnag(w * 0.47f, base, 70f * s, 1f, SILHOUETTE)
    drawSnag(w * 0.83f, base, 90f * s, -1f, SILHOUETTE)

    // shack on stilts
    val sx = w * 0.60f
    val bodyW = 76f * s
    val bodyH = 38f * s
    val floorY = base - 22f * s
    // stilts
    for (i in 0..3) {
        val px = sx - bodyW / 2f + 4f * s + i * (bodyW - 8f * s) / 3f
        drawLine(SILHOUETTE, Offset(px, floorY), Offset(px, base + 4f), 3f * s)
    }
    // walkway planks out to the water
    drawRect(SILHOUETTE, Offset(sx - bodyW / 2f - 46f * s, floorY - 1f), Size(46f * s, 3f * s))
    drawLine(SILHOUETTE, Offset(sx - bodyW / 2f - 40f * s, floorY), Offset(sx - bodyW / 2f - 40f * s, base + 4f), 2f * s)
    // body
    drawRect(SILHOUETTE_SOFT, Offset(sx - bodyW / 2f, floorY - bodyH), Size(bodyW, bodyH))
    // sagging tin roof
    val roof = Path().apply {
        moveTo(sx - bodyW / 2f - 8f * s, floorY - bodyH + 3f * s)
        lineTo(sx - 4f * s, floorY - bodyH - 15f * s)
        lineTo(sx + bodyW / 2f + 9f * s, floorY - bodyH + 6f * s)
        lineTo(sx + bodyW / 2f + 2f * s, floorY - bodyH + 9f * s)
        lineTo(sx - bodyW / 2f - 2f * s, floorY - bodyH + 8f * s)
        close()
    }
    drawPath(roof, SILHOUETTE)
    drawLine(MOON_RIM, Offset(sx - 4f * s, floorY - bodyH - 15f * s), Offset(sx + bodyW / 2f + 9f * s, floorY - bodyH + 6f * s), 1.2f)
    // chimney
    drawRect(SILHOUETTE, Offset(sx + bodyW * 0.22f, floorY - bodyH - 14f * s), Size(6f * s, 12f * s))
    // one dim window with a weak flicker, like someone left a candle burning
    val flick = 0.55f + 0.25f * sin(t * 6.1f) * sin(t * 2.3f + 1f)
    drawRect(
        Color(0xFFC08A35).copy(alpha = flick),
        Offset(sx - bodyW * 0.18f, floorY - bodyH * 0.72f),
        Size(9f * s, 8f * s)
    )
    glow(sx - bodyW * 0.18f + 4.5f * s, floorY - bodyH * 0.72f + 4f * s, 26f * s, Color(0xFFC08A35).copy(alpha = 0.16f * flick))

    drawBats(w, h, t)
}

private fun DrawScope.drawSnag(x: Float, baseY: Float, height: Float, dir: Float, color: Color) {
    drawLine(color, Offset(x, baseY), Offset(x + dir * 3f, baseY - height), 4.2f, StrokeCap.Round)
    drawLine(color, Offset(x + dir * 1f, baseY - height * 0.55f), Offset(x + dir * 20f, baseY - height * 0.78f), 2.6f, StrokeCap.Round)
    drawLine(color, Offset(x + dir * 2f, baseY - height * 0.75f), Offset(x - dir * 15f, baseY - height * 0.92f), 2.2f, StrokeCap.Round)
    drawLine(color, Offset(x + dir * 20f, baseY - height * 0.78f), Offset(x + dir * 28f, baseY - height * 0.70f), 1.6f, StrokeCap.Round)
}

private fun DrawScope.drawBats(w: Float, h: Float, t: Float) {
    for (i in 0..2) {
        val progress = (t * (0.035f + i * 0.007f) + i * 0.37f) % 1f
        val bx = w * (1.08f - progress * 1.25f)
        val by = h * (0.10f + i * 0.065f) + sin(t * 1.4f + i * 2f) * 16f
        val flap = sin(t * 15f + i * 2.1f)
        val span = 11f + i
        val wing = Path().apply {
            moveTo(bx, by)
            quadraticTo(bx - span * 0.5f, by - 6f - 5f * flap, bx - span, by + 1f + 3f * flap)
            quadraticTo(bx - span * 0.55f, by + 1f, bx, by + 2.5f)
            quadraticTo(bx + span * 0.55f, by + 1f, bx + span, by + 1f + 3f * flap)
            quadraticTo(bx + span * 0.5f, by - 6f - 5f * flap, bx, by)
            close()
        }
        drawPath(wing, SILHOUETTE)
        drawCircle(SILHOUETTE, 2.2f, Offset(bx, by + 0.5f))
    }
}

/** Objects standing in the water. Drawn before zombies so they walk in front of them. */
internal fun DrawScope.drawWaterDetails(w: Float, h: Float, waterTop: Float, t: Float) {
    val s = h / 700f

    // cypress knees poking out of the water
    for (i in 0 until 14) {
        val kx = w * (0.26f + hash01(i, 21f) * 0.70f)
        val ky = waterTop + (h - waterTop) * (0.08f + hash01(i, 22f) * 0.80f)
        val kh = (7f + hash01(i, 23f) * 11f) * s
        val knee = Path().apply {
            moveTo(kx - 3.4f * s, ky)
            quadraticTo(kx - 1.6f * s, ky - kh * 0.7f, kx + hash01(i, 24f) * 2f - 1f, ky - kh)
            quadraticTo(kx + 1.8f * s, ky - kh * 0.6f, kx + 3.6f * s, ky)
            close()
        }
        drawPath(knee, Color(0xFF2A241D))
        drawLine(Color(0x22CFE8E0), Offset(kx + 0.5f, ky - kh * 0.9f), Offset(kx + 2.4f * s, ky - 1f), 1f)
        drawOval(Color(0x33B8E0D8), Offset(kx - 6f * s, ky - 1.5f), Size(12f * s, 3.4f * s), style = Stroke(1f))
    }

    // floating log with moss
    run {
        val lx = w * 0.64f
        val ly = h * 0.80f + sin(t * 0.7f) * 1.5f
        val len = 120f * s
        val th = 15f * s
        withTransform({ rotate(degrees = -3f + sin(t * 0.5f) * 0.8f, pivot = Offset(lx, ly)) }) {
            drawOval(Color(0x55000000), Offset(lx - len / 2f, ly + th * 0.1f), Size(len, th * 0.9f))
            drawRoundRect(
                Color(0xFF3A2C20),
                Offset(lx - len / 2f, ly - th / 2f),
                Size(len, th),
                androidx.compose.ui.geometry.CornerRadius(th / 2f, th / 2f)
            )
            drawLine(Color(0xFF52402F), Offset(lx - len * 0.45f, ly - th * 0.25f), Offset(lx + len * 0.42f, ly - th * 0.28f), 1.6f * s)
            drawLine(Color(0xFF1F1710), Offset(lx - len * 0.40f, ly + th * 0.20f), Offset(lx + len * 0.46f, ly + th * 0.18f), 1.4f * s)
            drawOval(Color(0xFF2C4A2C), Offset(lx - len * 0.18f, ly - th * 0.55f), Size(len * 0.30f, th * 0.45f))
            drawOval(Color(0xFF3F6A3A).copy(alpha = 0.7f), Offset(lx - len * 0.10f, ly - th * 0.52f), Size(len * 0.14f, th * 0.26f))
            // sawn end rings
            drawOval(Color(0xFF6B5238), Offset(lx + len / 2f - th * 0.45f, ly - th / 2f), Size(th * 0.9f, th))
            drawOval(Color(0xFF3A2C20), Offset(lx + len / 2f - th * 0.28f, ly - th * 0.32f), Size(th * 0.56f, th * 0.64f), style = Stroke(1f))
        }
        drawOval(
            Color(0xFFB8E0D8).copy(alpha = 0.16f),
            Offset(lx - len * 0.58f, ly + th * 0.1f),
            Size(len * 1.16f, th * 0.8f),
            style = Stroke(1.2f)
        )
    }

    // half-sunken rowboat
    run {
        val bx = w * 0.88f
        val by = h * 0.63f
        val bl = 92f * s
        val bh = 26f * s
        withTransform({ rotate(degrees = 7f, pivot = Offset(bx, by)) }) {
            val hull = Path().apply {
                moveTo(bx - bl / 2f, by - bh * 0.55f)
                quadraticTo(bx - bl * 0.25f, by + bh * 0.35f, bx + bl * 0.05f, by + bh * 0.20f)
                quadraticTo(bx + bl * 0.38f, by + bh * 0.05f, bx + bl / 2f, by - bh * 0.7f)
                lineTo(bx + bl * 0.40f, by - bh * 0.78f)
                quadraticTo(bx + bl * 0.1f, by - bh * 0.15f, bx - bl * 0.38f, by - bh * 0.62f)
                close()
            }
            drawPath(hull, Color(0xFF30251C))
            drawLine(Color(0xFF4B3B2C), Offset(bx - bl * 0.42f, by - bh * 0.56f), Offset(bx + bl * 0.42f, by - bh * 0.72f), 1.8f * s)
            for (i in 1..4) {
                val px = bx - bl * 0.34f + i * bl * 0.15f
                drawLine(Color(0xFF1B140F), Offset(px, by - bh * 0.5f), Offset(px - 2f, by - bh * 0.02f), 1.1f)
            }
            // broken oar sticking out
            drawLine(Color(0xFF3F3225), Offset(bx + bl * 0.1f, by - bh * 0.3f), Offset(bx + bl * 0.34f, by - bh * 1.5f), 2.4f * s, StrokeCap.Round)
        }
        drawOval(Color(0xFF0A1A17).copy(alpha = 0.7f), Offset(bx - bl * 0.52f, by - 2f), Size(bl * 1.04f, 10f * s))
        drawOval(Color(0xFFB8E0D8).copy(alpha = 0.18f), Offset(bx - bl * 0.58f, by - 3f), Size(bl * 1.16f, 13f * s), style = Stroke(1.2f))
    }

    // rotten dock posts, one with a crow
    run {
        val px = w * 0.36f
        val py = h * 0.55f
        val posts = arrayOf(floatArrayOf(0f, 0f, 52f), floatArrayOf(34f, 6f, 40f), floatArrayOf(66f, 2f, 58f))
        for (p in posts) {
            val x = px + p[0] * s
            val y = py + p[1] * s
            val ph = p[2] * s
            drawRect(Color(0xFF2B2119), Offset(x - 4f * s, y - ph), Size(8f * s, ph))
            drawLine(Color(0xFF3F3226), Offset(x - 2.5f * s, y - ph), Offset(x - 2.5f * s, y), 1.2f)
            drawLine(Color(0xFF16100B), Offset(x + 2.5f * s, y - ph + 4f), Offset(x + 2.5f * s, y), 1.2f)
            // jagged broken top
            val top = Path().apply {
                moveTo(x - 4f * s, y - ph)
                lineTo(x - 1.5f * s, y - ph - 5f * s)
                lineTo(x + 1f * s, y - ph - 1f)
                lineTo(x + 4f * s, y - ph - 4f * s)
                lineTo(x + 4f * s, y - ph)
                close()
            }
            drawPath(top, Color(0xFF2B2119))
            drawOval(Color(0xFF2F4A2D).copy(alpha = 0.7f), Offset(x - 5f * s, y - 6f * s), Size(10f * s, 6f * s))
            drawOval(Color(0xFFB8E0D8).copy(alpha = 0.16f), Offset(x - 9f * s, y - 2f), Size(18f * s, 5f * s), style = Stroke(1f))
        }
        // crow on the middle post, head bobbing now and then
        val cx = px + 34f * s
        val cy = py + 6f * s - 40f * s - 5f * s
        val peck = if (sin(t * 0.9f) > 0.86f) 2.4f * s else 0f
        drawOval(SILHOUETTE, Offset(cx - 6f * s, cy - 5f * s), Size(12f * s, 8f * s))
        drawCircle(SILHOUETTE, 3.2f * s, Offset(cx - 5f * s, cy - 5f * s + peck))
        drawLine(Color(0xFF14110C), Offset(cx - 7.5f * s, cy - 5f * s + peck), Offset(cx - 11f * s, cy - 4f * s + peck * 1.4f), 1.6f * s, StrokeCap.Round)
        drawLine(SILHOUETTE, Offset(cx + 5f * s, cy), Offset(cx + 12f * s, cy + 3f * s), 2.2f * s, StrokeCap.Round)
        drawCircle(Color(0xFFB9B29A), 0.8f * s, Offset(cx - 5.8f * s, cy - 5.6f * s + peck))
    }

    // cattail clusters along the far bank
    for (i in 0 until 5) {
        val cx = w * (0.30f + i * 0.17f + hash01(i, 31f) * 0.05f)
        val cy = waterTop + 6f * s + hash01(i, 32f) * 8f * s
        drawCattails(cx, cy, s, t, i)
    }
}

private fun DrawScope.drawCattails(x: Float, baseY: Float, s: Float, t: Float, seed: Int) {
    for (k in 0 until 5) {
        val kx = x + (k - 2) * 5f * s
        val kh = (24f + hash01(seed * 7 + k, 33f) * 16f) * s
        val sway = sin(t * 1.1f + seed + k * 0.8f) * 2.4f * s
        val stalk = Path().apply {
            moveTo(kx, baseY)
            quadraticTo(kx + sway * 0.4f, baseY - kh * 0.5f, kx + sway, baseY - kh)
        }
        drawPath(stalk, Color(0xFF0C1A12), style = Stroke(width = 1.6f * s, cap = StrokeCap.Round))
        if (k % 2 == 0) {
            drawLine(
                Color(0xFF3A2A1E),
                Offset(kx + sway * 0.9f, baseY - kh * 0.86f),
                Offset(kx + sway, baseY - kh * 0.98f),
                3.4f * s,
                StrokeCap.Round
            )
        }
    }
}

/** Reeds along the bottom edge and hanging moss in the top corner. Never covers the play area. */
internal fun DrawScope.drawForegroundFrame(w: Float, h: Float, t: Float) {
    val s = h / 700f

    // reeds along the bottom edge (zombies never walk below 90% of the height)
    var x = -6f
    var i = 0
    while (x < w + 10f) {
        val rh = (22f + hash01(i, 41f) * 40f) * s
        val sway = sin(t * (1.2f + hash01(i, 42f) * 0.5f) + i * 0.9f) * 4f * s
        val blade = Path().apply {
            moveTo(x, h + 3f)
            quadraticTo(x + sway * 0.3f + (hash01(i, 43f) - 0.5f) * 6f, h - rh * 0.55f, x + sway + (hash01(i, 44f) - 0.5f) * 10f, h - rh)
        }
        drawPath(blade, Color(0xFF040B07), style = Stroke(width = (2.2f + hash01(i, 45f) * 1.6f) * s, cap = StrokeCap.Round))
        if (hash01(i, 46f) > 0.86f) {
            // a cattail head
            drawLine(Color(0xFF2A1D14), Offset(x + sway * 0.9f, h - rh * 0.88f), Offset(x + sway, h - rh * 0.99f), 4f * s, StrokeCap.Round)
        }
        x += 9f * s + hash01(i, 47f) * 9f * s
        i++
    }

    // hanging moss / vines from the top edge, thicker toward the right corner
    for (k in 0 until 16) {
        val mx = w * (0.52f + k * 0.032f) + hash01(k, 51f) * 14f
        val reachDown = if (mx > w * 0.88f) 0.20f else 0.07f
        val len = h * (reachDown * 0.5f + hash01(k, 52f) * reachDown)
        val sway = sin(t * 0.8f + k * 1.3f) * 5f * s
        val strand = Path().apply {
            moveTo(mx, -2f)
            quadraticTo(mx + sway * 0.5f, len * 0.5f, mx + sway, len)
        }
        drawPath(strand, Color(0xFF050E0A).copy(alpha = 0.92f), style = Stroke(width = (2.4f + hash01(k, 53f) * 2f) * s, cap = StrokeCap.Round))
        if (k % 3 == 0) {
            drawOval(
                Color(0xFF0B1B13).copy(alpha = 0.9f),
                Offset(mx + sway - 5f * s, len - 6f * s),
                Size(10f * s, 12f * s)
            )
        }
    }
    // a branch crossing the top right corner
    val branch = Path().apply {
        moveTo(w + 4f, h * 0.02f)
        quadraticTo(w * 0.93f, h * 0.045f, w * 0.84f, h * 0.012f)
    }
    drawPath(branch, Color(0xFF050E0A), style = Stroke(width = 7f * s, cap = StrokeCap.Round))

    // dust motes drifting in the moonlight
    for (m in 0 until 18) {
        val mx = (hash01(m, 61f) * w + t * (4f + hash01(m, 62f) * 6f)) % w
        val my = h * (0.15f + hash01(m, 63f) * 0.7f) + sin(t * 0.6f + m) * 10f
        drawCircle(Color(0xFFE8F2E0).copy(alpha = 0.06f + 0.05f * sin(t * 1.5f + m).absoluteValue), 1.3f, Offset(mx, my))
    }
}
