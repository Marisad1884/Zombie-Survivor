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
import com.example.game.model.WeaponType
import com.example.game.model.barrelLength
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * The heroine's weapon and the two arms holding it.
 *
 * Everything is drawn in a local frame whose origin is the shoulder anchor (GameEngine.gunAnchor) and
 * whose +x axis points down the barrel, so the barrel tip is always at x = type.barrelLength. That is the
 * same number the engine uses to spawn bullets, so shots leave exactly from the muzzle flash.
 *
 * Reloading is animated: the gun dips, the magazine (or the weapon's own ammo item) drops out, her free
 * hand fetches a fresh one from the chest rig and slots it back in.
 */

private const val RAD_TO_DEG = 180f / PI.toFloat()

private val OUTLINE = Color(0xFF0B0F10)
private val METAL_LIGHT = Color(0xFF74848D)
private val METAL = Color(0xFF3C484E)
private val METAL_DARK = Color(0xFF1F272B)
private val POLYMER = Color(0xFF262D30)
private val WOOD = Color(0xFF6E4C33)
private val WOOD_DARK = Color(0xFF45301F)
private val SLEEVE = Color(0xFF47573F)
private val SLEEVE_DARK = Color(0xFF2B3622)
private val SKIN = Color(0xFFC98E66)
private val GLOVE = Color(0xFF1B1511)
private val BRASS = Color(0xFFB78A3A)

private fun smooth(x: Float): Float {
    val k = x.coerceIn(0f, 1f)
    return k * k * (3f - 2f * k)
}

private fun lerp(a: Offset, b: Offset, k: Float) = Offset(a.x + (b.x - a.x) * k, a.y + (b.y - a.y) * k)

/** 0 = gun held up, 1 = gun dipped for the reload. */
private fun reloadDip(p: Float): Float = when {
    p < 0.12f -> smooth(p / 0.12f)
    p > 0.88f -> smooth((1f - p) / 0.12f)
    else -> 1f
}

/** 0 = free hand on the foregrip, 1 = free hand at the chest rig fetching a magazine. */
private fun handFetch(p: Float): Float = when {
    p < 0.20f -> 0f
    p < 0.40f -> smooth((p - 0.20f) / 0.20f)
    p < 0.50f -> 1f
    p < 0.65f -> 1f - smooth((p - 0.50f) / 0.15f)
    else -> 0f
}

/** Distance the old magazine has fallen / the new one still has to rise. Negative while it is out of the gun. */
private fun magDrop(p: Float): Float = when {
    p < 0.20f -> smooth(p / 0.20f) * 16f
    p < 0.65f -> -1f
    p < 0.85f -> (1f - smooth((p - 0.65f) / 0.20f)) * 16f
    else -> 0f
}

private fun foregripX(type: WeaponType): Float = when (type) {
    WeaponType.PISTOL -> 7f
    WeaponType.CROSSBOW -> 20f
    WeaponType.SHOTGUN -> 26f
    WeaponType.RIFLE -> 26f
    WeaponType.FLAMETHROWER -> 24f
    WeaponType.LAUNCHER -> 30f
    WeaponType.MINIGUN -> 20f
    WeaponType.SNIPER -> 28f
}

internal fun DrawScope.drawHeroWeapon(
    bodyX: Float,
    bodyY: Float,
    anchor: Offset,
    aimAngle: Float,
    type: WeaponType,
    muzzleFlash: Float,
    reloading: Boolean,
    reloadProgress: Float
) {
    val p = if (reloading) reloadProgress.coerceIn(0f, 1f) else 0f
    val dip = if (reloading) reloadDip(p) else 0f
    val angle = aimAngle * (1f - dip) + 0.62f * dip
    val recoil = (muzzleFlash / 0.08f).coerceIn(0f, 1f) * 4f
    val cosA = cos(angle)
    val sinA = sin(angle)

    fun local(lx: Float, ly: Float): Offset {
        val x = lx - recoil
        return Offset(anchor.x + x * cosA - ly * sinA, anchor.y + x * sinA + ly * cosA)
    }

    val fetch = if (reloading) handFetch(p) else 0f
    val drop = if (reloading) magDrop(p) else 0f
    val rig = Offset(bodyX - 3f, bodyY + 25f)

    // ---- far arm (behind the gun): reaches for the foregrip, or the chest rig while reloading ----
    val farShoulder = Offset(bodyX + 5f, bodyY + 9f)
    val farHand = lerp(local(foregripX(type), 4f), rig, fetch)
    drawArm(farShoulder, farHand, SLEEVE_DARK, SLEEVE_DARK)
    if (reloading && p in 0.38f..0.66f && type != WeaponType.FLAMETHROWER) {
        // the fresh magazine travelling in her hand
        withTransform({ rotate(angle * RAD_TO_DEG, pivot = farHand) }) {
            drawRoundRect(OUTLINE, Offset(farHand.x - 4f, farHand.y - 1f), Size(8f, 9f), CornerRadius(2f, 2f))
            drawRoundRect(METAL_DARK, Offset(farHand.x - 3f, farHand.y), Size(6f, 7f), CornerRadius(1.5f, 1.5f))
            drawRect(BRASS, Offset(farHand.x - 3f, farHand.y), Size(6f, 1.5f))
        }
    }

    // ---- the weapon ----
    withTransform({
        translate(left = anchor.x, top = anchor.y)
        rotate(degrees = angle * RAD_TO_DEG, pivot = Offset.Zero)
        translate(left = -recoil, top = 0f)
    }) {
        drawGun(type, drop)
        drawMuzzleFlash(type, muzzleFlash)
    }

    // ---- near arm (in front of the gun): rear hand on the grip ----
    val nearShoulder = Offset(bodyX - 1f, bodyY + 10f)
    drawArm(nearShoulder, local(2f, 5f), SLEEVE, SLEEVE_DARK)
}

/** Two-segment arm: sleeve, rolled sleeve edge, bare forearm and a gloved hand, with a dark outline. */
private fun DrawScope.drawArm(shoulder: Offset, hand: Offset, sleeve: Color, sleeveDark: Color) {
    val dx = hand.x - shoulder.x
    val dy = hand.y - shoulder.y
    val d = hypot(dx, dy).coerceAtLeast(1f)
    val seg = max(13f, d * 0.52f)
    val half = d / 2f
    val bend = sqrt(max(0f, seg * seg - half * half))
    val elbow = Offset(
        shoulder.x + dx / 2f - dy / d * bend,
        shoulder.y + dy / 2f + dx / d * bend
    )
    val wrist = lerp(elbow, hand, 0.82f)
    val cuff = lerp(elbow, hand, 0.38f)

    drawLine(OUTLINE, shoulder, elbow, 7.6f, StrokeCap.Round)
    drawLine(OUTLINE, elbow, wrist, 6.6f, StrokeCap.Round)

    drawLine(sleeve, shoulder, elbow, 6f, StrokeCap.Round)
    drawLine(sleeveDark, Offset(shoulder.x, shoulder.y + 2f), Offset(elbow.x, elbow.y + 2f), 1.6f, StrokeCap.Round)
    drawLine(sleeve, elbow, cuff, 5.2f, StrokeCap.Round)
    drawLine(Color(0xFF161B12), Offset(cuff.x, cuff.y - 2.4f), Offset(cuff.x, cuff.y + 2.4f), 1.2f)
    drawLine(SKIN, cuff, wrist, 4.2f, StrokeCap.Round)
    drawCircle(OUTLINE, 4.9f, hand)
    drawCircle(GLOVE, 4.1f, hand)
    drawCircle(Color(0xFF3A2E26), 1.4f, Offset(hand.x - 1f, hand.y - 1.4f))
}

// -------------------------------------------------------------------------------------------------
// local-frame drawing helpers
// -------------------------------------------------------------------------------------------------

private fun DrawScope.box(c: Color, x: Float, y: Float, w: Float, h: Float, r: Float = 1.5f) {
    drawRoundRect(c, Offset(x, y), Size(w, h), CornerRadius(r, r))
}

/** Dark outline, then a flat fill. */
private fun DrawScope.part(fill: Color, x: Float, y: Float, w: Float, h: Float, r: Float = 1.5f) {
    box(OUTLINE, x - 1f, y - 1f, w + 2f, h + 2f, r + 1f)
    box(fill, x, y, w, h, r)
}

/** Outlined bar of brushed metal: light on top, dark underneath, with a thin highlight. */
private fun DrawScope.metal(x: Float, y: Float, w: Float, h: Float, r: Float = 1.5f) {
    box(OUTLINE, x - 1f, y - 1f, w + 2f, h + 2f, r + 1f)
    drawRoundRect(
        brush = Brush.verticalGradient(listOf(METAL_LIGHT, METAL, METAL_DARK), startY = y, endY = y + h),
        topLeft = Offset(x, y),
        size = Size(w, h),
        cornerRadius = CornerRadius(r, r)
    )
    drawLine(Color(0x55FFFFFF), Offset(x + r, y + 0.9f), Offset(x + w - r, y + 0.9f), 0.9f)
}

private fun DrawScope.poly(fill: Color, vararg pts: Float) {
    val path = Path().apply {
        moveTo(pts[0], pts[1])
        var i = 2
        while (i < pts.size) {
            lineTo(pts[i], pts[i + 1])
            i += 2
        }
        close()
    }
    drawPath(path, OUTLINE, style = Stroke(width = 2.6f))
    drawPath(path, fill)
}

private fun DrawScope.drawGun(type: WeaponType, drop: Float) {
    val L = type.barrelLength
    val magIn = drop >= 0f
    val d = max(drop, 0f)

    when (type) {
        WeaponType.PISTOL -> {
            if (magIn) metal(2f, 11f + d, 7f, 3f, 1f)
            part(POLYMER, 1f, 1f, 8f, 12f, 2f)
            drawLine(Color(0xFF3B454A), Offset(3f, 4f), Offset(7f, 4f), 0.8f)
            drawLine(Color(0xFF3B454A), Offset(3f, 7f), Offset(7f, 7f), 0.8f)
            metal(-1f, -5f, L + 1f, 6.5f, 1.5f)
            part(METAL_DARK, 3f, 1.4f, L - 12f, 3.2f, 1f)
            for (i in 0..2) drawLine(OUTLINE, Offset(1f + i * 1.8f, -4.2f), Offset(1f + i * 1.8f, -0.4f), 0.8f)
            box(METAL_DARK, L - 3f, -7.5f, 2f, 2.6f, 0.6f)
            box(METAL_DARK, 0.5f, -7.5f, 3f, 2.6f, 0.6f)
            part(POLYMER, 6f, -10f, 9f, 4.4f, 1.6f)
            drawCircle(Color(0xFFFF1744), 1.2f, Offset(11f, -7.8f))
            glow(11f, -7.8f, 5f, Color(0x88FF1744))
        }

        WeaponType.CROSSBOW -> {
            poly(WOOD, -17f, -2.5f, -2f, -3.5f, 0f, 4.5f, -13f, 8.5f, -18f, 5f)
            drawLine(WOOD_DARK, Offset(-15f, 1f), Offset(-3f, 1.5f), 0.8f)
            metal(-2f, -3f, L - 4f, 5.5f, 1.5f)
            box(METAL_DARK, 2f, -3.8f, L - 14f, 1.4f, 0.5f)
            // prod (the bow): two curved limbs
            val prod = Path().apply {
                moveTo(L - 14f, -22f)
                quadraticTo(L - 6f, -10f, L - 10f, 0f)
                quadraticTo(L - 6f, 10f, L - 14f, 22f)
            }
            drawPath(prod, OUTLINE, style = Stroke(width = 5.4f, cap = StrokeCap.Round))
            drawPath(prod, Color(0xFF52646D), style = Stroke(width = 3.2f, cap = StrokeCap.Round))
            drawLine(Color(0xFFCFD8DC), Offset(L - 14f, -22f), Offset(12f, -0.5f), 1.2f)
            drawLine(Color(0xFFCFD8DC), Offset(L - 14f, 22f), Offset(12f, -0.5f), 1.2f)
            // stirrup
            drawCircle(OUTLINE, 3.6f, Offset(L - 2f, 0f), style = Stroke(width = 1.6f))
            if (magIn) {
                val by = -0.5f + d * 0.6f
                drawLine(OUTLINE, Offset(12f, by), Offset(L + 2f, by), 3.2f, StrokeCap.Round)
                drawLine(Color(0xFF8D6E63), Offset(12f, by), Offset(L - 3f, by), 1.8f, StrokeCap.Round)
                drawLine(Color(0xFF76FF03), Offset(L - 3f, by), Offset(L + 2f, by), 2.2f, StrokeCap.Round)
            }
        }

        WeaponType.SHOTGUN -> {
            poly(WOOD, -17f, -2f, -2f, -4f, 0f, 5f, -14f, 9f, -18f, 5f)
            drawLine(WOOD_DARK, Offset(-15f, 0f), Offset(-3f, 0.5f), 0.8f)
            drawLine(WOOD_DARK, Offset(-14f, 3f), Offset(-3f, 3.4f), 0.8f)
            metal(12f, -4.2f, L - 12f, 3.8f, 1f)
            metal(12f, 0.8f, L - 20f, 3f, 1f)
            metal(-2f, -5f, 17f, 10f, 2f)
            box(OUTLINE, 5f, -4.6f, 6f, 2f, 0.6f)
            part(WOOD, 21f, -1f, 14f, 7.5f, 3f)
            for (i in 0..3) drawLine(WOOD_DARK, Offset(24f + i * 3f, 0f), Offset(24f + i * 3f, 5.5f), 0.9f)
            box(METAL_DARK, L - 4f, -4.8f, 3.4f, 5.4f, 0.8f)
            box(METAL_DARK, L - 0.5f, -5.6f, 1.8f, 2.4f, 0.5f)
            part(POLYMER, 2f, 4f, 5f, 8f, 2f)
            if (d > 0f || !magIn) {
                // shell dropping out of the loading port
                val sy = 6f + d
                if (magIn) {
                    box(OUTLINE, 5f, sy - 1f, 5.4f, 9f, 1.2f)
                    box(Color(0xFFC62828), 5.8f, sy, 3.8f, 6f, 1f)
                    box(BRASS, 5.8f, sy + 5.4f, 3.8f, 2.4f, 0.6f)
                }
            }
        }

        WeaponType.RIFLE -> {
            poly(POLYMER, -17f, -2f, -1f, -3.5f, 0f, 4f, -14f, 7.5f, -17f, 5f)
            drawLine(Color(0xFF3B454A), Offset(-15f, 0f), Offset(-3f, 0.5f), 0.8f)
            metal(-1f, -4.4f, 19f, 8.4f, 1.5f)
            part(POLYMER, 3f, 3.5f, 5f, 9f, 2f)
            part(POLYMER, 17f, -4.2f, 21f, 8.4f, 2f)
            for (i in 0..3) box(OUTLINE, 20f + i * 4.6f, -2f, 2.4f, 3.6f, 0.6f)
            box(METAL_DARK, 6f, -6.6f, 31f, 2f, 0.6f)
            metal(37f, -2f, L - 37f, 3.6f, 1f)
            box(METAL_DARK, L - 1f, -3.4f, 6f, 6.8f, 0.8f)
            for (i in 0..1) drawLine(OUTLINE, Offset(L + 1f + i * 2f, -3f), Offset(L + 1f + i * 2f, 3f), 0.8f)
            // optic
            part(METAL_DARK, 9f, -12f, 14f, 6f, 2.6f)
            box(METAL, 12f, -7f, 8f, 1.6f, 0.4f)
            drawCircle(Color(0xFF00E676), 1.7f, Offset(22.4f, -9f))
            glow(22.4f, -9f, 6f, Color(0x6600E676))
            if (magIn) {
                val mag = Path().apply {
                    moveTo(11f, 3.5f + d)
                    lineTo(18.5f, 3.5f + d)
                    lineTo(21f, 18f + d)
                    lineTo(13.4f, 18f + d)
                    close()
                }
                drawPath(mag, OUTLINE, style = Stroke(width = 2.6f))
                drawPath(mag, METAL_DARK)
                drawLine(Color(0xFF3B454A), Offset(12f, 6f + d), Offset(18f, 6f + d), 0.8f)
            }
        }

        WeaponType.FLAMETHROWER -> {
            part(POLYMER, 2f, 4f, 6f, 10f, 2f)
            metal(-3f, -5.4f, 23f, 10.6f, 2.5f)
            metal(18f, -2.6f, L - 18f, 5.2f, 1.5f)
            drawLine(Color(0xFFFF8F00), Offset(-6f, 9f), Offset(8f, 14f), 3f, StrokeCap.Round)
            // pressure gauge
            drawCircle(OUTLINE, 4.4f, Offset(10f, -9f))
            drawCircle(Color(0xFFFFD54F), 3.4f, Offset(10f, -9f))
            drawLine(OUTLINE, Offset(10f, -9f), Offset(11.8f, -10.6f), 1f, StrokeCap.Round)
            // nozzle with grate and a small pilot light
            part(Color(0xFF3E2723), L - 7f, -5.4f, 9f, 10.8f, 2f)
            for (i in -1..1) drawLine(OUTLINE, Offset(L - 5f, i * 3f), Offset(L + 1f, i * 3f), 0.9f)
            drawCircle(Color(0xFFFF6F00), 2.2f, Offset(L + 4f, 0f))
            glow(L + 4f, 0f, 8f, Color(0x88FF8F00))
            // fuel canister underneath
            if (magIn) {
                val cy = 6f + d
                part(Color(0xFF8D5A2B), 8f, cy, 18f, 8f, 3.5f)
                drawLine(Color(0xFF5D3A1B), Offset(13f, cy + 0.6f), Offset(13f, cy + 7.4f), 1.2f)
                drawLine(Color(0xFF5D3A1B), Offset(20f, cy + 0.6f), Offset(20f, cy + 7.4f), 1.2f)
                drawLine(Color(0x55FFFFFF), Offset(10f, cy + 1.6f), Offset(24f, cy + 1.6f), 0.9f)
            }
        }

        WeaponType.LAUNCHER -> {
            part(METAL_DARK, -13f, -5.5f, 9f, 11f, 3f)
            // main tube
            box(OUTLINE, -5f, -8f, L + 5f, 16f, 5f)
            drawRoundRect(
                brush = Brush.verticalGradient(
                    listOf(Color(0xFF5C7A55), Color(0xFF34523B), Color(0xFF1B2F22)),
                    startY = -7f,
                    endY = 7f
                ),
                topLeft = Offset(-4f, -7f),
                size = Size(L + 3f, 14f),
                cornerRadius = CornerRadius(4f, 4f)
            )
            drawLine(Color(0x44FFFFFF), Offset(0f, -5.4f), Offset(L - 2f, -5.4f), 1f)
            for (x in listOf(9f, 21f, 35f)) drawLine(Color(0xFF14231A), Offset(x, -7f), Offset(x, 7f), 1.6f)
            part(METAL_DARK, L - 3f, -9f, 6f, 18f, 2f)
            // flip-up sight
            box(OUTLINE, 13f, -14f, 5.4f, 7f, 0.8f)
            box(Color(0xFFFFD54F), 14f, -13f, 3.4f, 5f, 0.5f)
            part(POLYMER, 6f, 6f, 6f, 10f, 2f)
            part(POLYMER, 29f, 6f, 5f, 9f, 2f)
            // grenade nose poking out of the muzzle
            if (magIn) {
                val nose = Path().apply {
                    moveTo(L + 2f, -4.4f + d)
                    lineTo(L + 9f, 0f + d)
                    lineTo(L + 2f, 4.4f + d)
                    close()
                }
                drawPath(nose, OUTLINE, style = Stroke(width = 2.4f))
                drawPath(nose, Color(0xFFB71C1C))
            }
        }

        WeaponType.MINIGUN -> {
            part(METAL, -5f, -8.5f, 20f, 17f, 4f)
            drawLine(Color(0x55FFFFFF), Offset(-2f, -6.6f), Offset(12f, -6.6f), 1f)
            for (b in -2..2) {
                drawLine(OUTLINE, Offset(15f, b * 2.7f), Offset(L, b * 2.7f), 3.2f)
            }
            for (b in -2..2) {
                drawLine(if (b % 2 == 0) METAL_LIGHT else METAL, Offset(15f, b * 2.7f), Offset(L, b * 2.7f), 1.9f)
            }
            for (x in listOf(27f, 43f)) part(METAL_DARK, x, -7.6f, 3f, 15.2f, 1f)
            part(METAL_DARK, L - 3f, -8f, 3.4f, 16f, 1f)
            // carry handle
            part(METAL, 8f, -14f, 17f, 4f, 1.5f)
            drawLine(OUTLINE, Offset(10f, -10f), Offset(10f, -8f), 1.6f)
            drawLine(OUTLINE, Offset(23f, -10f), Offset(23f, -8f), 1.6f)
            // ammo box with a belt feeding the gun
            if (magIn) {
                val by = 9f + d
                part(Color(0xFF4A5A3A), -3f, by, 15f, 11f, 2f)
                drawLine(Color(0xFF2D3A22), Offset(-1f, by + 4f), Offset(10f, by + 4f), 1f)
                drawLine(OUTLINE, Offset(9f, by), Offset(14f, 4f), 4.4f, StrokeCap.Round)
                drawLine(BRASS, Offset(9f, by), Offset(14f, 4f), 3f, StrokeCap.Round)
            }
        }

        WeaponType.SNIPER -> {
            poly(Color(0xFF3D4330), -18f, -2f, -2f, -3.6f, 0f, 4.5f, -12f, 9.5f, -18f, 6.5f)
            box(Color(0xFF2B3022), -16f, -3.8f, 9f, 2f, 1f)
            metal(-1f, -4.2f, 23f, 8.4f, 1.5f)
            metal(21f, -2.1f, L - 21f, 4.2f, 1f)
            for (i in 0..2) drawLine(OUTLINE, Offset(30f + i * 7f, -1.4f), Offset(30f + i * 7f + 3f, -1.4f), 0.8f)
            part(METAL_DARK, L - 2f, -3.8f, 7f, 7.6f, 1f)
            for (i in 0..1) drawLine(METAL_LIGHT, Offset(L + 1.5f + i * 2.4f, -3.2f), Offset(L + 1.5f + i * 2.4f, 3.2f), 0.7f)
            // scope: tube, bells, mounts and a lens glint
            part(METAL_DARK, 10f, -7.6f, 3f, 3.4f, 0.8f)
            part(METAL_DARK, 26f, -7.6f, 3f, 3.4f, 0.8f)
            metal(6f, -13f, 26f, 6f, 3f)
            part(METAL_DARK, 31f, -14.4f, 6f, 9f, 2f)
            part(METAL_DARK, 3f, -13.6f, 4f, 7.4f, 1.5f)
            drawCircle(Color(0xFF00E5FF), 2.4f, Offset(36f, -9.8f))
            glow(36f, -9.8f, 8f, Color(0x6600E5FF))
            // bolt handle
            drawLine(OUTLINE, Offset(11f, -3.4f), Offset(9f, -8.2f), 3.4f, StrokeCap.Round)
            drawLine(METAL_LIGHT, Offset(11f, -3.4f), Offset(9f, -8.2f), 1.8f, StrokeCap.Round)
            // folded bipod
            drawLine(OUTLINE, Offset(40f, 2f), Offset(54f, 7.5f), 4f, StrokeCap.Round)
            drawLine(METAL, Offset(40f, 2f), Offset(54f, 7.5f), 2.2f, StrokeCap.Round)
            if (magIn) part(METAL_DARK, 8f, 3.6f + d, 9f, 7f, 1.5f)
        }
    }
}

/** Star-shaped muzzle flash at the barrel tip (local frame), plus a soft additive glow. */
private fun DrawScope.drawMuzzleFlash(type: WeaponType, timer: Float) {
    if (timer <= 0f || type == WeaponType.CROSSBOW) return
    val k = (timer / 0.08f).coerceIn(0f, 1f)
    val tip = type.barrelLength + 3f
    val radius = 9f + 16f * k

    glow(tip + 2f, 0f, radius * 1.9f, Color(0xFFFFB74D).copy(alpha = 0.55f * k))
    if (type == WeaponType.FLAMETHROWER) return

    val spin = (timer * 500f) % 0.5f
    val star = Path().apply {
        for (i in 0 until 12) {
            val a = i * (PI.toFloat() / 6f) + spin
            val forward = max(0f, cos(a))
            val r = if (i % 2 == 0) radius * (0.4f + 0.6f * forward) else radius * 0.26f
            val x = tip + cos(a) * r
            val y = sin(a) * r
            if (i == 0) moveTo(x, y) else lineTo(x, y)
        }
        close()
    }
    drawPath(star, Color(0xFFFFB300))
    drawCircle(Color(0xFFFFF59D), radius * 0.46f, Offset(tip + 1f, 0f))
    drawCircle(Color.White, radius * 0.22f, Offset(tip, 0f))
}
