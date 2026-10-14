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
import com.example.game.engine.GameEngine
import com.example.game.engine.RunnerState
import kotlin.math.abs
import kotlin.math.sin

/**
 * "Nina", the girl who runs ammo. Original character: rust-red ponytail, yellow headband, teal parka,
 * mustard backpack, dark jeans and muddy boots. Drawn facing right with feet on (x, porchY).
 * The engine decides where she is and what she is doing; this file only draws it.
 */

private val OUTLINE = Color(0xFF0B0F10)
private val PARKA = Color(0xFF2F7F7A)
private val PARKA_DARK = Color(0xFF1C524F)
private val PARKA_LIGHT = Color(0xFF55A7A0)
private val JEANS = Color(0xFF2B3550)
private val JEANS_DARK = Color(0xFF1B2236)
private val BOOT = Color(0xFF3B2A1E)
private val SKIN = Color(0xFFE8BC98)
private val SKIN_SHADE = Color(0xFFC8946F)
private val HAIR = Color(0xFFA64B24)
private val HAIR_DARK = Color(0xFF6E2D14)
private val HEADBAND = Color(0xFFF2C230)
private val PACK = Color(0xFFC79325)
private val PACK_DARK = Color(0xFF8A6317)
private val CRATE = Color(0xFF55633F)
private val CRATE_DARK = Color(0xFF38422A)
private val BRASS = Color(0xFFB78A3A)

internal fun DrawScope.drawAmmoRunner(engine: GameEngine, t: Float) {
    val state = engine.runnerState
    val x = engine.runnerX
    val footY = engine.porchY
    if (x < -30f) return // out of sight, fetching the crate

    val moving = state == RunnerState.GOING || state == RunnerState.BRINGING || state == RunnerState.HOMING
    val phase = engine.runnerWalkPhase
    val facing = engine.runnerFacing

    // ground shadow
    drawOval(Color(0x44000000), Offset(x - 13f, footY - 3f), Size(26f, 7f))

    // the crate she has just put down, with a little glint of brass
    if (engine.runnerCrateOnFloor) {
        drawCrate(x + 30f, footY - 12f, 1f, open = true)
        glow(x + 30f, footY - 15f, 16f, Color(0x66FFD54F))
    }

    withTransform({
        scale(scaleX = facing.toFloat(), scaleY = 1f, pivot = Offset(x, footY))
    }) {
        drawNina(
            x = x,
            footY = footY,
            t = t,
            phase = phase,
            moving = moving,
            carrying = engine.runnerCarrying,
            handing = state == RunnerState.HANDING && !engine.runnerDelivered
        )
    }
}

private fun DrawScope.drawNina(
    x: Float,
    footY: Float,
    t: Float,
    phase: Float,
    moving: Boolean,
    carrying: Boolean,
    handing: Boolean
) {
    val swing = if (moving) sin(phase) else 0f
    val bob = if (moving) abs(sin(phase)) * 1.6f else sin(t * 2f) * 0.5f
    val lean = if (carrying) 3f else if (moving) 1.5f else 0f

    val hipY = footY - 25f - bob
    val shoulderY = hipY - 21f
    val headY = shoulderY - 9f
    val sx = x + lean

    // ---- ponytail (streams behind her: left side, since she faces right) ----
    val flutter = if (moving) sin(phase * 1.3f) * 3f else sin(t * 1.5f) * 1.2f
    val tail = Path().apply {
        moveTo(sx - 5f, headY - 7f)
        cubicTo(sx - 14f, headY - 8f + flutter, sx - 20f, headY + 2f - flutter, sx - 17f, headY + 12f + flutter)
        cubicTo(sx - 13f, headY + 6f, sx - 9f, headY + 2f, sx - 5f, headY - 1f)
        close()
    }
    drawPath(tail, OUTLINE, style = Stroke(width = 2.4f))
    drawPath(tail, HAIR)
    drawLine(HAIR_DARK, Offset(sx - 7f, headY - 5f), Offset(sx - 15f, headY + 8f + flutter), 1.2f)

    // ---- backpack ----
    drawRoundRect(OUTLINE, Offset(sx - 14f, shoulderY - 1f), Size(11f, 17f), CornerRadius(4f, 4f))
    drawRoundRect(PACK, Offset(sx - 13f, shoulderY), Size(9f, 15f), CornerRadius(3.5f, 3.5f))
    drawRoundRect(PACK_DARK, Offset(sx - 12f, shoulderY + 8f), Size(7f, 5f), CornerRadius(2f, 2f))
    drawRect(Color(0xFF3A2A12), Offset(sx - 13f, shoulderY + 5f), Size(9f, 1.2f))

    // ---- back leg ----
    val backFoot = Offset(x - swing * 7f - 1f, footY)
    drawLeg(Offset(x - 2f, hipY), backFoot, JEANS_DARK)

    // ---- far arm (behind the body) ----
    if (carrying || handing) {
        // both arms are wrapped around the crate in front of her
    } else {
        val handX = sx + swing * 7f + 1f
        drawLimb(Offset(sx + 1f, shoulderY + 3f), Offset(handX, hipY + 2f + abs(swing) * 2f), PARKA_DARK)
    }

    // ---- torso (parka) ----
    val torso = Path().apply {
        moveTo(sx - 8f, shoulderY + 1f)
        quadraticTo(sx - 9f, shoulderY - 2f, sx - 5f, shoulderY - 2f)
        lineTo(sx + 6f, shoulderY - 2f)
        quadraticTo(sx + 9f, shoulderY - 1f, sx + 8f, shoulderY + 4f)
        lineTo(sx + 8f, hipY + 3f)
        lineTo(sx - 8f, hipY + 5f)
        close()
    }
    drawPath(torso, OUTLINE, style = Stroke(width = 2.4f))
    drawPath(
        torso,
        Brush.linearGradient(
            listOf(PARKA_LIGHT, PARKA, PARKA_DARK),
            start = Offset(sx + 8f, shoulderY),
            end = Offset(sx - 8f, hipY + 5f)
        )
    )
    drawLine(PARKA_DARK, Offset(sx + 1f, shoulderY), Offset(sx + 1f, hipY + 4f), 1.2f)
    drawRect(Color(0xFF14302E), Offset(sx - 8f, hipY - 2f), Size(16f, 2.6f))
    // chest pocket and the strap of the backpack
    drawRoundRect(PARKA_DARK, Offset(sx + 2.5f, shoulderY + 5f), Size(5f, 5f), CornerRadius(1f, 1f))
    drawLine(PACK_DARK, Offset(sx - 5f, shoulderY - 1f), Offset(sx - 1f, hipY - 1f), 2f)
    // hood bunched around the neck
    drawRoundRect(PARKA_DARK, Offset(sx - 7f, shoulderY - 4f), Size(14f, 6f), CornerRadius(3f, 3f))
    drawLine(Color(0x55FFFFFF), Offset(sx - 5f, shoulderY - 2f), Offset(sx + 4f, shoulderY - 2.4f), 1f)

    // ---- front leg ----
    val frontFoot = Offset(x + swing * 7f + 2f, footY)
    drawLeg(Offset(x + 1f, hipY), frontFoot, JEANS)

    // ---- head ----
    drawOval(OUTLINE, Offset(sx - 8.6f, headY - 9.6f), Size(17.2f, 19.2f))
    drawOval(
        Brush.radialGradient(listOf(SKIN, SKIN, SKIN_SHADE), center = Offset(sx + 3f, headY - 2f), radius = 14f),
        Offset(sx - 7.6f, headY - 8.6f),
        Size(15.2f, 17.2f)
    )
    // hair cap and fringe
    val hair = Path().apply {
        moveTo(sx - 8.4f, headY + 1f)
        cubicTo(sx - 10f, headY - 12f, sx + 2f, headY - 14f, sx + 7f, headY - 8f)
        cubicTo(sx + 8f, headY - 6f, sx + 7.6f, headY - 5.4f, sx + 7f, headY - 4.6f)
        cubicTo(sx + 3f, headY - 8f, sx - 1f, headY - 6f, sx - 4f, headY - 2f)
        cubicTo(sx - 5f, headY, sx - 6f, headY + 1f, sx - 7f, headY + 3f)
        close()
    }
    drawPath(hair, HAIR)
    drawLine(Color(0xFFD27A45), Offset(sx - 6f, headY - 10f), Offset(sx + 2f, headY - 11f), 1f)
    // headband
    drawLine(HEADBAND, Offset(sx - 8f, headY - 7f), Offset(sx + 7.4f, headY - 8.4f), 2.6f, StrokeCap.Round)
    // eye, brow, nose, mouth
    drawOval(Color.White, Offset(sx + 1.6f, headY - 2.8f), Size(5.2f, 3.6f))
    drawCircle(Color(0xFF2E7D4F), 1.5f, Offset(sx + 5f, headY - 1f))
    drawCircle(Color(0xFF10201A), 0.8f, Offset(sx + 5.3f, headY - 1f))
    drawLine(Color(0xFF3B1E10), Offset(sx + 1.4f, headY - 4.4f), Offset(sx + 6.8f, headY - 5.2f), 1.3f, StrokeCap.Round)
    drawLine(SKIN_SHADE, Offset(sx + 7.4f, headY - 0.6f), Offset(sx + 9f, headY + 2.4f), 1f, StrokeCap.Round)
    drawLine(Color(0xFFA6483F), Offset(sx + 3.6f, headY + 5f), Offset(sx + 7f, headY + 4.6f), 1.4f, StrokeCap.Round)
    // freckles
    drawCircle(Color(0x66A8643C), 0.6f, Offset(sx + 3f, headY + 1.6f))
    drawCircle(Color(0x66A8643C), 0.6f, Offset(sx + 5f, headY + 2.2f))

    // ---- near arm / crate ----
    if (carrying || handing) {
        // when handing over she pushes the crate forward and a bit down
        val push = if (handing) 8f else 0f
        val crateX = sx + 10f + push
        val crateY = shoulderY + 12f + (if (handing) 3f else 0f)
        drawCrate(crateX, crateY, 1f, open = false)
        drawLimb(Offset(sx + 1f, shoulderY + 3f), Offset(crateX - 6f, crateY + 6f), PARKA)
        drawLimb(Offset(sx + 3f, shoulderY + 3f), Offset(crateX + 6f, crateY + 8f), PARKA)
    } else {
        val handX = sx - swing * 7f + 1f
        drawLimb(Offset(sx - 1f, shoulderY + 3f), Offset(handX, hipY + 2f + abs(swing) * 2f), PARKA)
    }
}

private fun DrawScope.drawLeg(hip: Offset, foot: Offset, color: Color) {
    val knee = Offset((hip.x + foot.x) / 2f + 1.5f, (hip.y + foot.y) / 2f)
    drawLine(OUTLINE, hip, knee, 8.6f, StrokeCap.Round)
    drawLine(OUTLINE, knee, Offset(foot.x, foot.y - 4f), 7.6f, StrokeCap.Round)
    drawLine(color, hip, knee, 6.4f, StrokeCap.Round)
    drawLine(color, knee, Offset(foot.x, foot.y - 4f), 5.6f, StrokeCap.Round)
    // boot
    drawRoundRect(OUTLINE, Offset(foot.x - 4f, foot.y - 7f), Size(12f, 8f), CornerRadius(3f, 3f))
    drawRoundRect(BOOT, Offset(foot.x - 3f, foot.y - 6f), Size(10f, 6.4f), CornerRadius(2.5f, 2.5f))
    drawLine(Color(0xFF0B0907), Offset(foot.x - 3f, foot.y - 0.4f), Offset(foot.x + 7f, foot.y - 0.4f), 1.6f)
}

private fun DrawScope.drawLimb(shoulder: Offset, hand: Offset, sleeve: Color) {
    val elbow = Offset((shoulder.x + hand.x) / 2f + 1f, (shoulder.y + hand.y) / 2f + 2f)
    drawLine(OUTLINE, shoulder, elbow, 6.8f, StrokeCap.Round)
    drawLine(OUTLINE, elbow, hand, 6f, StrokeCap.Round)
    drawLine(sleeve, shoulder, elbow, 4.8f, StrokeCap.Round)
    drawLine(sleeve, elbow, hand, 4.2f, StrokeCap.Round)
    drawCircle(OUTLINE, 3.6f, hand)
    drawCircle(SKIN, 2.8f, hand)
}

/** Olive ammo crate with a rope handle, stencilled stripe and brass rounds when open. */
private fun DrawScope.drawCrate(cx: Float, cy: Float, scale: Float, open: Boolean) {
    val w = 22f * scale
    val h = 14f * scale
    drawRoundRect(OUTLINE, Offset(cx - w / 2f - 1f, cy - h / 2f - 1f), Size(w + 2f, h + 2f), CornerRadius(2.5f, 2.5f))
    drawRoundRect(
        Brush.verticalGradient(listOf(CRATE, CRATE_DARK), startY = cy - h / 2f, endY = cy + h / 2f),
        Offset(cx - w / 2f, cy - h / 2f),
        Size(w, h),
        CornerRadius(2f, 2f)
    )
    drawRect(Color(0xFF2A331F), Offset(cx - w / 2f, cy - 1f), Size(w, 1.6f))
    drawRect(Color(0xFFE6C34A), Offset(cx - w / 2f + 3f, cy + 2.4f), Size(7f, 2.2f))
    drawLine(Color(0xFF8D6E63), Offset(cx - 4f, cy - h / 2f), Offset(cx + 4f, cy - h / 2f), 1.6f)
    if (open) {
        for (i in 0..3) {
            val bx = cx - w / 2f + 3f + i * 5f
            drawRect(BRASS, Offset(bx, cy - h / 2f - 3.4f), Size(2.8f, 4f))
            drawRect(Color(0xFFE0B25A), Offset(bx, cy - h / 2f - 3.4f), Size(2.8f, 1.2f))
        }
    }
}
