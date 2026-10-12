package com.example.game.ui

import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import kotlin.math.sin

/**
 * Original heroine: "Mara", a swamp scout. Dark braided hair, rust-orange scarf, amber goggles
 * pushed up on her forehead, olive jacket with a chest rig and fingerless gloves.
 *
 * She is drawn facing right, seated behind the barricade. The weapon and the arms holding it are
 * drawn separately by the caller (see GameRenderer.drawTacticalGun) so the same body works with
 * every weapon.
 */

private val JACKET_LIGHT = Color(0xFF47573F)
private val JACKET_DARK = Color(0xFF26301F)
private val PANTS = Color(0xFF3A3F36)
private val PANTS_DARK = Color(0xFF282C25)
private val SKIN_LIGHT = Color(0xFFE2B18A)
private val SKIN_MID = Color(0xFFC98E66)
private val SKIN_SHADE = Color(0xFFA86F4D)
private val HAIR = Color(0xFF2A1A14)
private val HAIR_LIGHT = Color(0xFF4D3225)
private val SCARF = Color(0xFFB4491F)
private val SCARF_DARK = Color(0xFF7F2F12)
private val LEATHER = Color(0xFF1B1511)

internal fun DrawScope.drawHeroine(
    bodyX: Float,
    bodyY: Float,
    porchY: Float,
    t: Float,
    muzzleFlash: Float
) {
    val breath = sin(t * 2.1f) * 0.8f
    val flap = sin(t * 3.4f) * 3f

    // ---- Crate she is sitting on ------------------------------------------------------------
    drawRoundRect(
        color = Color(0xFF4A3322),
        topLeft = Offset(bodyX - 26f, porchY - 24f),
        size = Size(24f, 24f),
        cornerRadius = CornerRadius(2f, 2f)
    )
    drawLine(Color(0xFF2B1D13), Offset(bodyX - 26f, porchY - 16f), Offset(bodyX - 2f, porchY - 16f), 2f)
    drawLine(Color(0xFF2B1D13), Offset(bodyX - 26f, porchY - 8f), Offset(bodyX - 2f, porchY - 8f), 2f)
    drawLine(Color(0xFF6B4B33), Offset(bodyX - 25f, porchY - 23f), Offset(bodyX - 3f, porchY - 23f), 1.5f)

    // ---- Scarf tail streaming behind her ----------------------------------------------------
    val tail = Path().apply {
        moveTo(bodyX - 2f, bodyY + 1f + breath)
        cubicTo(bodyX - 14f, bodyY + 3f + flap, bodyX - 24f, bodyY + 10f - flap, bodyX - 36f, bodyY + 8f + flap * 1.4f)
        lineTo(bodyX - 32f, bodyY + 16f + flap)
        cubicTo(bodyX - 23f, bodyY + 18f - flap, bodyX - 12f, bodyY + 13f, bodyX - 1f, bodyY + 8f + breath)
        close()
    }
    drawPath(tail, SCARF)
    drawLine(
        SCARF_DARK,
        Offset(bodyX - 4f, bodyY + 6f + breath),
        Offset(bodyX - 30f, bodyY + 12f + flap),
        1.4f
    )

    // ---- Braid hanging down her back ---------------------------------------------------------
    val sway = sin(t * 1.6f) * 1.5f
    for (i in 0 until 7) {
        val bx = bodyX - 9f - i * 1.3f + sway * (i / 6f)
        val by = bodyY - 2f + i * 5.2f + breath
        val r = 3.6f - i * 0.22f
        drawOval(HAIR, Offset(bx - r, by - r * 1.25f), Size(r * 2f, r * 2.5f))
        drawLine(HAIR_LIGHT, Offset(bx - r * 0.2f, by - r), Offset(bx - r * 0.5f, by + r * 0.6f), 0.9f)
    }
    drawCircle(Color(0xFFA03A1C), 2.2f, Offset(bodyX - 9f - 6 * 1.3f + sway, bodyY - 2f + 6 * 5.2f + 7f + breath))

    // ---- Legs (back leg darker) --------------------------------------------------------------
    // back leg
    val backThigh = Path().apply {
        moveTo(bodyX - 4f, bodyY + 30f)
        lineTo(bodyX + 12f, bodyY + 32f)
        lineTo(bodyX + 12f, bodyY + 41f)
        lineTo(bodyX - 4f, bodyY + 40f)
        close()
    }
    drawPath(backThigh, PANTS_DARK)
    drawRoundRect(PANTS_DARK, Offset(bodyX + 7f, bodyY + 36f), Size(8f, porchY - bodyY - 40f), CornerRadius(3f, 3f))
    drawRoundRect(LEATHER, Offset(bodyX + 5f, porchY - 8f), Size(14f, 8f), CornerRadius(3f, 3f))

    // front leg
    val frontThigh = Path().apply {
        moveTo(bodyX - 8f, bodyY + 28f)
        lineTo(bodyX + 8f, bodyY + 30f)
        lineTo(bodyX + 9f, bodyY + 40f)
        lineTo(bodyX - 8f, bodyY + 40f)
        close()
    }
    drawPath(
        frontThigh,
        Brush.linearGradient(
            listOf(Color(0xFF454B40), PANTS),
            start = Offset(bodyX - 8f, bodyY + 28f),
            end = Offset(bodyX + 9f, bodyY + 40f)
        )
    )
    drawRoundRect(PANTS, Offset(bodyX + 2f, bodyY + 36f), Size(9f, porchY - bodyY - 40f), CornerRadius(3f, 3f))
    // knee pad
    drawRoundRect(Color(0xFF1E2321), Offset(bodyX + 2f, bodyY + 34f), Size(10f, 8f), CornerRadius(3f, 3f))
    drawLine(Color(0xFF3B4440), Offset(bodyX + 3f, bodyY + 36f), Offset(bodyX + 11f, bodyY + 36f), 1f)
    // boot with laces
    drawRoundRect(LEATHER, Offset(bodyX - 1f, porchY - 9f), Size(16f, 9f), CornerRadius(3f, 3f))
    drawLine(Color(0xFF5A4A3E), Offset(bodyX + 4f, porchY - 8f), Offset(bodyX + 9f, porchY - 8f), 1f)
    drawLine(Color(0xFF5A4A3E), Offset(bodyX + 4f, porchY - 5f), Offset(bodyX + 9f, porchY - 5f), 1f)
    drawLine(Color(0xFF0B0907), Offset(bodyX - 1f, porchY - 0.5f), Offset(bodyX + 15f, porchY - 0.5f), 2f)

    // ---- Upper body (breathes) ---------------------------------------------------------------
    withTransform({ translate(left = 0f, top = breath) }) {
        // torso / jacket
        val torso = Path().apply {
            moveTo(bodyX - 13f, bodyY + 6f)
            quadraticTo(bodyX - 13f, bodyY + 2f, bodyX - 8f, bodyY + 2f)
            lineTo(bodyX + 8f, bodyY + 1f)
            quadraticTo(bodyX + 14f, bodyY + 2f, bodyX + 13f, bodyY + 8f)
            lineTo(bodyX + 11f, bodyY + 33f)
            lineTo(bodyX - 11f, bodyY + 34f)
            close()
        }
        drawPath(
            torso,
            Brush.linearGradient(
                listOf(JACKET_LIGHT, JACKET_DARK),
                start = Offset(bodyX + 12f, bodyY),
                end = Offset(bodyX - 12f, bodyY + 34f)
            )
        )
        // jacket center zip + hem seam
        drawLine(Color(0xFF151A12), Offset(bodyX + 2f, bodyY + 6f), Offset(bodyX + 1f, bodyY + 33f), 1.2f)
        drawLine(Color(0xFF151A12), Offset(bodyX - 11f, bodyY + 30f), Offset(bodyX + 11f, bodyY + 30f), 1.2f)

        // chest rig: two diagonal straps + magazine pouches
        drawLine(LEATHER, Offset(bodyX - 9f, bodyY + 3f), Offset(bodyX + 8f, bodyY + 30f), 2.6f)
        drawLine(LEATHER, Offset(bodyX + 9f, bodyY + 3f), Offset(bodyX - 8f, bodyY + 30f), 2.6f)
        for (i in 0..1) {
            val px = bodyX - 8f + i * 9f
            drawRoundRect(Color(0xFF1C2118), Offset(px, bodyY + 16f), Size(7f, 9f), CornerRadius(1.5f, 1.5f))
            drawRect(Color(0xFFB78A3A), Offset(px + 1.2f, bodyY + 15f), Size(4.6f, 1.8f))
        }
        // belt
        drawRect(Color(0xFF14110E), Offset(bodyX - 11f, bodyY + 31f), Size(22f, 3.5f))
        drawRect(Color(0xFF9B9487), Offset(bodyX - 1.5f, bodyY + 31f), Size(4f, 3.5f))
        // arm patch (orange stripe on a dark square)
        drawRect(Color(0xFF14181A), Offset(bodyX - 12f, bodyY + 9f), Size(6f, 6f))
        drawRect(Color(0xFFE0762B), Offset(bodyX - 12f, bodyY + 11f), Size(6f, 1.8f))

        // neck
        drawRect(SKIN_SHADE, Offset(bodyX - 2f, bodyY - 5f), Size(7f, 8f))

        // ---- Head ----
        val hx = bodyX + 1f
        val hy = bodyY - 12f

        // face
        drawOval(
            Brush.radialGradient(
                listOf(SKIN_LIGHT, SKIN_MID, SKIN_SHADE),
                center = Offset(hx + 5f, hy - 4f),
                radius = 20f
            ),
            Offset(hx - 11f, hy - 12.5f),
            Size(22f, 25f)
        )
        // jaw definition + cheek shadow
        drawOval(Color(0x22000000), Offset(hx - 6f, hy + 3f), Size(15f, 9f))
        // dirt smudge + small scar over the brow
        drawOval(Color(0x2A3B2A1A), Offset(hx + 1f, hy + 2f), Size(7f, 3.4f))
        drawLine(Color(0x66F2D2B4), Offset(hx + 4f, hy - 8f), Offset(hx + 7.5f, hy - 5.5f), 1f)

        // ear + earpiece
        drawOval(SKIN_SHADE, Offset(hx - 7f, hy - 1f), Size(5f, 7f))
        drawRoundRect(Color(0xFF14181A), Offset(hx - 7.5f, hy + 0.5f), Size(3.2f, 4.2f), CornerRadius(1f, 1f))

        // eye: white, amber iris, pupil, highlight, lash line
        drawOval(Color(0xFFF1E8DC), Offset(hx + 2.5f, hy - 3f), Size(6.8f, 4.2f))
        drawCircle(Color(0xFFB8741F), 2.1f, Offset(hx + 6.4f, hy - 0.9f))
        drawCircle(Color(0xFF1A0F08), 1.0f, Offset(hx + 6.8f, hy - 0.9f))
        drawCircle(Color.White, 0.7f, Offset(hx + 5.8f, hy - 1.6f))
        drawLine(Color(0xFF1B100B), Offset(hx + 2.2f, hy - 2.6f), Offset(hx + 9.6f, hy - 3.4f), 1.4f, StrokeCap.Round)
        // eyebrow (a little furrowed)
        drawLine(Color(0xFF241510), Offset(hx + 1.5f, hy - 5f), Offset(hx + 9.5f, hy - 6.6f), 1.8f, StrokeCap.Round)

        // nose + mouth
        val nose = Path().apply {
            moveTo(hx + 9.5f, hy - 0.5f)
            lineTo(hx + 12.6f, hy + 3.4f)
            lineTo(hx + 9.2f, hy + 3.9f)
        }
        drawPath(nose, SKIN_SHADE, style = Stroke(width = 1.2f, cap = StrokeCap.Round))
        drawLine(Color(0xFF8E3D3A), Offset(hx + 6f, hy + 7.4f), Offset(hx + 10.4f, hy + 7f), 1.7f, StrokeCap.Round)

        // hair: cap of dark hair swept to one side with a few lighter strands
        val hair = Path().apply {
            moveTo(hx - 12f, hy + 3f)
            cubicTo(hx - 14f, hy - 14f, hx - 2f, hy - 17f, hx + 8f, hy - 12f)
            cubicTo(hx + 12f, hy - 10f, hx + 11f, hy - 7f, hx + 10f, hy - 6f)
            cubicTo(hx + 6f, hy - 9f, hx + 1f, hy - 8f, hx - 3f, hy - 3f)
            cubicTo(hx - 5f, hy + 0f, hx - 7f, hy + 3f, hx - 8f, hy + 5f)
            close()
        }
        drawPath(hair, HAIR)
        drawLine(HAIR_LIGHT, Offset(hx - 8f, hy - 12f), Offset(hx + 3f, hy - 12.5f), 1f)
        drawLine(HAIR_LIGHT, Offset(hx - 11f, hy - 6f), Offset(hx - 6f, hy - 11f), 1f)
        // loose strand in front of the ear
        drawLine(HAIR, Offset(hx - 4f, hy - 3f), Offset(hx - 3f, hy + 5f), 1.3f, StrokeCap.Round)

        // goggles pushed up on the forehead: strap, back lens, front lens with a glass glint
        drawLine(Color(0xFF191919), Offset(hx - 11.5f, hy - 8f), Offset(hx + 10f, hy - 10.5f), 3.4f, StrokeCap.Round)
        withTransform({ rotate(degrees = -6f, pivot = Offset(hx + 3f, hy - 12.5f)) }) {
            drawOval(Color(0xFF2A2A2A), Offset(hx - 8f, hy - 16.5f), Size(10f, 7f))
            drawOval(Color(0xAA6B3B14), Offset(hx - 6.6f, hy - 15.4f), Size(7.4f, 4.6f))
            drawOval(Color(0xFF2A2A2A), Offset(hx - 1f, hy - 17f), Size(12f, 8f))
            drawOval(
                Brush.linearGradient(
                    listOf(Color(0xFFFFC04D), Color(0xFFB8661A)),
                    start = Offset(hx - 1f, hy - 17f),
                    end = Offset(hx + 11f, hy - 9f)
                ),
                Offset(hx + 0.4f, hy - 15.8f),
                Size(9.2f, 5.6f)
            )
            drawLine(Color(0xAAFFFFFF), Offset(hx + 2f, hy - 14.8f), Offset(hx + 5.5f, hy - 15.4f), 1.2f, StrokeCap.Round)
        }
        glow(hx + 5f, hy - 13f, 9f, Color(0xFFFFA726).copy(alpha = 0.18f))

        // scarf wrapped around the neck, knot in front
        drawRoundRect(SCARF, Offset(bodyX - 5f, bodyY - 2f), Size(15f, 7f), CornerRadius(3.5f, 3.5f))
        drawLine(SCARF_DARK, Offset(bodyX - 3f, bodyY + 2f), Offset(bodyX + 8f, bodyY + 2.5f), 1.2f)
        val knot = Path().apply {
            moveTo(bodyX + 6f, bodyY + 3f)
            lineTo(bodyX + 12f, bodyY + 11f + flap * 0.3f)
            lineTo(bodyX + 8f, bodyY + 12f + flap * 0.3f)
            lineTo(bodyX + 3f, bodyY + 4f)
            close()
        }
        drawPath(knot, SCARF_DARK)

        // warm light on her face while firing
        if (muzzleFlash > 0f) {
            val k = (muzzleFlash / 0.08f).coerceIn(0f, 1f)
            glow(hx + 8f, hy, 22f, Color(0xFFFFB74D).copy(alpha = 0.32f * k))
        }
    }
}
