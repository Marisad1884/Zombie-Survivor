package com.example.game.ui

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.lerp
import com.example.game.model.Zombie
import com.example.game.model.ZombieType
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin

/**
 * Human-shaped zombies (walker, runner, spitter, armored, brute, boss).
 *
 * Every zombie gets its own look from its id: skin tone, clothes, hair, missing arm, exposed
 * ribs, how far the jaw hangs. Limbs are tapered and jointed instead of single thick lines, the
 * torso leans forward, the lower body is hidden under the waterline and the head lolls.
 */

private class ZombieLook(
    val skin: Color,
    val skinShade: Color,
    val skinLight: Color,
    val shirt: Color,
    val shirtShade: Color,
    val pants: Color,
    val pantsShade: Color,
    val hair: Color?,
    val longHair: Boolean,
    val missingArm: Boolean,
    val exposedRibs: Boolean,
    val jawHang: Float
)

private val SKIN_TONES = arrayOf(
    Color(0xFF8E9C7C), // pallid grey-green
    Color(0xFF9B9A8A), // ashen
    Color(0xFFA29A6C), // sallow
    Color(0xFF7F7886), // bruised
    Color(0xFF6A5444), // dark, decayed
    Color(0xFF72888A)  // drowned blue-grey
)
private val HAIR_TONES = arrayOf(Color(0xFF1E1714), Color(0xFF3A2A1F), Color(0xFF55504A), Color(0xFF6B5A3C))
private val WALKER_SHIRTS = arrayOf(Color(0xFF6B3B2E), Color(0xFF4E5A63), Color(0xFF7A7568), Color(0xFF3E5236))
private val WALKER_PANTS = arrayOf(Color(0xFF3A4150), Color(0xFF4A4034), Color(0xFF2E2E2E))
private val RUNNER_HOODIES = arrayOf(Color(0xFF8A2A24), Color(0xFF2F4A6B), Color(0xFF55624A))

private fun <T> pick(arr: Array<T>, id: Int, salt: Float): T =
    arr[(hash01(id, salt) * arr.size).toInt().coerceIn(0, arr.size - 1)]

private fun lookFor(z: Zombie): ZombieLook {
    val id = z.id.toInt()
    val type = z.type

    val baseSkin = when (type) {
        ZombieType.COLOSSUS -> Color(0xFF4A3A34)
        ZombieType.TANK -> lerp(pick(SKIN_TONES, id, 1f), Color(0xFF5E6660), 0.35f)
        ZombieType.SPITTER -> lerp(pick(SKIN_TONES, id, 1f), Color(0xFFA7A24A), 0.35f)
        else -> pick(SKIN_TONES, id, 1f)
    }

    val shirt = when (type) {
        ZombieType.RUNNER -> pick(RUNNER_HOODIES, id, 2f)
        ZombieType.SPITTER -> Color(0xFF4D5A33)
        ZombieType.ARMORED -> Color(0xFF2A3A2E)
        ZombieType.TANK -> Color(0xFF8A8570)
        ZombieType.COLOSSUS -> Color(0xFF3A1A20)
        else -> pick(WALKER_SHIRTS, id, 2f)
    }
    val pants = when (type) {
        ZombieType.RUNNER -> Color(0xFF24262B)
        ZombieType.SPITTER -> Color(0xFF3A3C28)
        ZombieType.ARMORED -> Color(0xFF2A3328)
        ZombieType.TANK -> Color(0xFF4B5560)
        ZombieType.COLOSSUS -> Color(0xFF1E1214)
        else -> pick(WALKER_PANTS, id, 3f)
    }

    val canLoseArm = type == ZombieType.WALKER || type == ZombieType.SPITTER || type == ZombieType.RUNNER
    val bald = hash01(id, 4f) < 0.30f || type == ZombieType.ARMORED

    return ZombieLook(
        skin = baseSkin,
        skinShade = lerp(baseSkin, Color.Black, 0.38f),
        skinLight = lerp(baseSkin, Color.White, 0.22f),
        shirt = shirt,
        shirtShade = lerp(shirt, Color.Black, 0.42f),
        pants = pants,
        pantsShade = lerp(pants, Color.Black, 0.42f),
        hair = if (bald) null else pick(HAIR_TONES, id, 5f),
        longHair = hash01(id, 6f) < 0.35f,
        missingArm = canLoseArm && hash01(id, 7f) < 0.16f,
        exposedRibs = type == ZombieType.WALKER && hash01(id, 8f) < 0.40f,
        jawHang = 0.14f + hash01(id, 9f) * 0.34f
    )
}

/** Tapered limb segment from a to b with a darker strip on one side for volume. */
private fun DrawScope.limb(a: Offset, b: Offset, w0: Float, w1: Float, color: Color, shade: Color) {
    val dx = b.x - a.x
    val dy = b.y - a.y
    val len = hypot(dx, dy).coerceAtLeast(0.001f)
    val nx = -dy / len
    val ny = dx / len
    val p = Path().apply {
        moveTo(a.x + nx * w0 / 2f, a.y + ny * w0 / 2f)
        lineTo(b.x + nx * w1 / 2f, b.y + ny * w1 / 2f)
        lineTo(b.x - nx * w1 / 2f, b.y - ny * w1 / 2f)
        lineTo(a.x - nx * w0 / 2f, a.y - ny * w0 / 2f)
        close()
    }
    drawPath(p, color)
    drawCircle(color, w0 / 2f, a)
    drawCircle(color, w1 / 2f, b)
    drawLine(
        color = shade.copy(alpha = 0.5f),
        start = Offset(a.x + nx * w0 * 0.24f, a.y + ny * w0 * 0.24f),
        end = Offset(b.x + nx * w1 * 0.24f, b.y + ny * w1 * 0.24f),
        strokeWidth = max(1f, min(w0, w1) * 0.42f),
        cap = StrokeCap.Round
    )
}

internal fun DrawScope.drawHumanoidZombie(z: Zombie, isHit: Boolean, t: Float) {
    val type = z.type
    val id = z.id.toInt()
    val look = lookFor(z)
    fun c(col: Color): Color = if (isHit) lerp(col, Color.White, 0.72f) else col

    val w = type.width
    val h = type.height
    val x = z.x
    val footY = z.y
    val phase = z.walkAnimTimer
    val big = type == ZombieType.TANK || type == ZombieType.COLOSSUS
    val hpRatio = (z.currentHp / z.maxHp).coerceIn(0f, 1f)

    val lean = when (type) {
        ZombieType.RUNNER -> -17f
        ZombieType.SPITTER -> -12f
        ZombieType.ARMORED -> -5f
        ZombieType.TANK -> -7f
        ZombieType.COLOSSUS -> -5f
        else -> -9f
    }
    val bob = abs(cos(phase)) * h * 0.018f
    val hipX = x
    val hipY = footY - h * 0.40f - bob
    val torsoLen = h * 0.31f
    val shoulderW = w * (if (big) 0.84f else 0.62f)
    val waistW = shoulderW * 0.78f
    val thigh = w * (if (big) 0.21f else 0.17f)
    val waterLine = footY - h * 0.12f

    // ---------------------------------------------------------------- legs (clipped by water)
    clipRect(left = x - w * 2f, top = footY - h * 2f, right = x + w * 2f, bottom = waterLine) {
        for (leg in 0..1) {
            val off = if (leg == 0) 0f else PI.toFloat()
            val back = leg == 1
            val hip = Offset(hipX + (if (back) w * 0.08f else -w * 0.06f), hipY)
            val lift = max(0f, cos(phase + off)) * h * 0.05f
            val foot = Offset(hipX + sin(phase + off) * w * 0.30f - w * 0.05f, footY - lift)
            val knee = Offset((hip.x + foot.x) / 2f - w * 0.11f, (hip.y + foot.y) / 2f)
            val pc = c(if (back) look.pantsShade else look.pants)
            val ps = c(look.pantsShade)
            limb(hip, knee, thigh, thigh * 0.84f, pc, ps)
            limb(knee, foot, thigh * 0.78f, thigh * 0.58f, pc, ps)
            // torn trouser cuff + bare skin showing through
            drawLine(c(look.skinShade), Offset(knee.x - 1f, knee.y + h * 0.05f), Offset(knee.x + 2f, knee.y + h * 0.075f), thigh * 0.5f)
            // shoe
            drawOval(
                c(Color(0xFF16120F)),
                Offset(foot.x - w * 0.17f, foot.y - h * 0.035f),
                Size(w * 0.30f, h * 0.06f)
            )
        }
    }

    // ---------------------------------------------------------- upper body (leans forward)
    withTransform({ rotate(degrees = lean, pivot = Offset(hipX, hipY)) }) {
        val shoulderY = hipY - torsoLen
        val sway = sin(phase * 0.5f + id) * 1.2f

        // pelvis
        drawRoundRect(
            c(look.pants),
            Offset(hipX - waistW / 2f, hipY - h * 0.05f),
            Size(waistW, h * 0.10f),
            androidx.compose.ui.geometry.CornerRadius(3f, 3f)
        )

        // back arm first so the torso overlaps its shoulder
        val reach = if (type == ZombieType.RUNNER) h * 0.43f else h * 0.36f
        val attack = z.isAttacking
        val swipe = sin(t * 13f + id)
        val upper = h * 0.17f * (if (big) 1.15f else 1f)
        val armW = w * (if (big) 0.17f else 0.125f)

        fun arm(shoulder: Offset, dark: Boolean, phaseShift: Float) {
            val bobA = sin(phase * 2f + phaseShift) * h * 0.035f
            val hand = if (attack) {
                Offset(
                    shoulder.x - reach * (0.78f + 0.22f * swipe),
                    shoulder.y + h * (0.02f + 0.13f * (swipe * 0.5f + 0.5f)) * (if (phaseShift == 0f) 1f else 0.7f)
                )
            } else {
                Offset(shoulder.x - reach, shoulder.y + h * 0.05f + bobA)
            }
            val elbow = Offset(
                (shoulder.x + hand.x) / 2f + w * 0.02f,
                (shoulder.y + hand.y) / 2f + upper * 0.38f
            )
            val sleeve = c(if (dark) look.shirtShade else look.shirt)
            val skin = c(if (dark) look.skinShade else look.skin)
            limb(shoulder, elbow, armW, armW * 0.9f, sleeve, c(look.shirtShade))
            // torn sleeve: forearm is bare skin
            limb(
                Offset((elbow.x * 0.55f + shoulder.x * 0.45f), (elbow.y * 0.55f + shoulder.y * 0.45f)),
                elbow, armW * 0.9f, armW * 0.85f, sleeve, c(look.shirtShade)
            )
            limb(elbow, hand, armW * 0.82f, armW * 0.6f, skin, c(look.skinShade))
            // clawed hand
            drawCircle(skin, armW * 0.55f, hand)
            for (f in -1..1) {
                drawLine(
                    c(Color(0xFF2A2420)),
                    Offset(hand.x - armW * 0.3f, hand.y + f * armW * 0.28f),
                    Offset(hand.x - armW * 1.05f, hand.y + f * armW * 0.5f + 1f),
                    max(1.2f, armW * 0.18f),
                    StrokeCap.Round
                )
            }
        }

        if (!look.missingArm) {
            arm(Offset(hipX + shoulderW * 0.20f, shoulderY + h * 0.05f), true, 1.7f)
        }

        // torso
        val torso = Path().apply {
            moveTo(hipX - waistW / 2f, hipY)
            quadraticTo(hipX - shoulderW * 0.56f, hipY - torsoLen * 0.55f, hipX - shoulderW / 2f, shoulderY + h * 0.03f)
            quadraticTo(hipX, shoulderY - h * 0.02f + sway, hipX + shoulderW / 2f, shoulderY + h * 0.03f)
            quadraticTo(hipX + shoulderW * 0.56f, hipY - torsoLen * 0.55f, hipX + waistW / 2f, hipY)
            close()
        }
        drawPath(
            torso,
            Brush.horizontalGradient(
                listOf(c(look.shirtShade), c(look.shirt), c(lerp(look.shirt, Color.White, 0.10f))),
                startX = hipX - shoulderW / 2f,
                endX = hipX + shoulderW / 2f
            )
        )

        // jagged torn hem over the belt
        val hem = Path().apply {
            moveTo(hipX - waistW / 2f, hipY - h * 0.02f)
            val steps = 7
            for (i in 1..steps) {
                val px = hipX - waistW / 2f + waistW * i / steps
                val py = hipY + (if (i % 2 == 0) h * 0.045f else h * 0.005f) + hash01(id + i, 10f) * h * 0.02f
                lineTo(px, py)
            }
            lineTo(hipX + waistW / 2f, hipY - h * 0.05f)
            lineTo(hipX - waistW / 2f, hipY - h * 0.05f)
            close()
        }
        drawPath(hem, c(look.shirtShade))

        // moonlight rim along the right edge
        drawLine(
            Color(0x26CFE8E0),
            Offset(hipX + shoulderW / 2f, shoulderY + h * 0.03f),
            Offset(hipX + waistW / 2f, hipY),
            1.4f,
            StrokeCap.Round
        )

        // type specific torso details
        when (type) {
            ZombieType.WALKER -> if (look.exposedRibs && !isHit) {
                val rx = hipX - shoulderW * 0.30f
                val ry = shoulderY + torsoLen * 0.22f
                drawOval(Color(0xFF3B0E10), Offset(rx, ry), Size(shoulderW * 0.46f, torsoLen * 0.52f))
                for (i in 0..3) {
                    val yy = ry + torsoLen * (0.08f + i * 0.11f)
                    drawLine(
                        Color(0xFFCFC6A8),
                        Offset(rx + 2f, yy),
                        Offset(rx + shoulderW * 0.40f, yy + 1.6f),
                        1.6f,
                        StrokeCap.Round
                    )
                }
                drawLine(Color(0xFFB9B093), Offset(rx + shoulderW * 0.22f, ry + 1f), Offset(rx + shoulderW * 0.22f, ry + torsoLen * 0.50f), 1.4f)
            }

            ZombieType.RUNNER -> {
                // hoodie pocket, drawstrings and a bunched hood behind the neck
                drawRoundRect(
                    c(look.shirtShade),
                    Offset(hipX - waistW * 0.34f, hipY - torsoLen * 0.38f),
                    Size(waistW * 0.68f, torsoLen * 0.20f),
                    androidx.compose.ui.geometry.CornerRadius(3f, 3f)
                )
                drawLine(c(Color(0xFFD7D1C2)), Offset(hipX - 3f, shoulderY + h * 0.05f), Offset(hipX - 4f, shoulderY + h * 0.15f), 1.1f)
                drawLine(c(Color(0xFFD7D1C2)), Offset(hipX + 2f, shoulderY + h * 0.05f), Offset(hipX + 3f, shoulderY + h * 0.13f), 1.1f)
            }

            ZombieType.SPITTER -> if (!isHit) {
                // swollen belly full of toxic sacs
                val bx = hipX - shoulderW * 0.14f
                val by = hipY - torsoLen * 0.30f
                val br = w * 0.27f
                drawCircle(
                    Brush.radialGradient(
                        listOf(Color(0xFFB9B26A), Color(0xFF8C8B4C), Color(0xFF5C5A32)),
                        center = Offset(bx + br * 0.2f, by - br * 0.25f),
                        radius = br * 1.3f
                    ),
                    br,
                    Offset(bx, by)
                )
                // veins
                for (i in 0..2) {
                    drawLine(
                        Color(0x663C2B2E),
                        Offset(bx - br * 0.2f + i * br * 0.25f, by - br * 0.6f),
                        Offset(bx - br * 0.45f + i * br * 0.5f, by + br * 0.65f),
                        1.1f
                    )
                }
                val pulse = 0.55f + 0.45f * sin(t * 2.2f + id)
                for (i in 0..2) {
                    val px = bx - br * 0.45f + i * br * 0.42f
                    val py = by - br * 0.25f + (i % 2) * br * 0.45f
                    glow(px, py, 9f, Color(0xFFC9D14A).copy(alpha = 0.22f * pulse))
                    drawCircle(Color(0xFFB4BE3C), 3.4f, Offset(px, py))
                    drawCircle(Color(0xFFEFF5A0), 1.2f, Offset(px - 0.9f, py - 0.9f))
                }
            }

            ZombieType.ARMORED -> if (!isHit) {
                // riot vest with plates and straps
                val vest = Path().apply {
                    moveTo(hipX - shoulderW * 0.46f, shoulderY + h * 0.03f)
                    lineTo(hipX + shoulderW * 0.46f, shoulderY + h * 0.03f)
                    lineTo(hipX + waistW * 0.46f, hipY - h * 0.03f)
                    lineTo(hipX - waistW * 0.46f, hipY - h * 0.03f)
                    close()
                }
                drawPath(vest, Brush.horizontalGradient(listOf(Color(0xFF232C30), Color(0xFF3A464B)), hipX - shoulderW / 2f, hipX + shoulderW / 2f))
                drawRect(Color(0xFF2E383C), Offset(hipX - waistW * 0.3f, shoulderY + torsoLen * 0.18f), Size(waistW * 0.6f, torsoLen * 0.42f))
                drawLine(Color(0xFF596870), Offset(hipX - waistW * 0.3f, shoulderY + torsoLen * 0.18f), Offset(hipX + waistW * 0.3f, shoulderY + torsoLen * 0.18f), 1.2f)
                // scrapes and bullet scars
                drawLine(Color(0xFF8E9BA0), Offset(hipX - 4f, shoulderY + torsoLen * 0.3f), Offset(hipX + 6f, shoulderY + torsoLen * 0.46f), 1.2f)
                drawCircle(Color(0xFF14191B), 1.8f, Offset(hipX + 5f, shoulderY + torsoLen * 0.25f))
                // pouches
                for (i in 0..1) {
                    drawRect(Color(0xFF1B2124), Offset(hipX - waistW * 0.4f + i * waistW * 0.5f, hipY - h * 0.10f), Size(waistW * 0.3f, h * 0.05f))
                }
            }

            ZombieType.TANK -> if (!isHit) {
                // overalls: one strap hanging, bare gut, rusty chain across the chest
                drawRect(c(look.pants), Offset(hipX - waistW / 2f, hipY - torsoLen * 0.55f), Size(waistW, torsoLen * 0.55f))
                drawOval(
                    Brush.radialGradient(
                        listOf(look.skinLight, look.skin, look.skinShade),
                        center = Offset(hipX + 3f, shoulderY + torsoLen * 0.25f),
                        radius = shoulderW * 0.5f
                    ),
                    Offset(hipX - shoulderW * 0.3f, shoulderY + torsoLen * 0.05f),
                    Size(shoulderW * 0.6f, torsoLen * 0.5f)
                )
                drawLine(Color(0xFF3F3B38), Offset(hipX + shoulderW * 0.25f, shoulderY + h * 0.04f), Offset(hipX + shoulderW * 0.05f, hipY - torsoLen * 0.5f), 3.2f)
                for (i in 0..5) {
                    val px = hipX - shoulderW * 0.4f + i * shoulderW * 0.15f
                    val py = shoulderY + torsoLen * 0.12f + i * torsoLen * 0.07f
                    drawOval(Color(0xFF6B4A33), Offset(px, py), Size(5f, 3.2f), style = Stroke(1.6f))
                }
                // old stitched scar
                drawLine(Color(0xFF5B2E2B), Offset(hipX - 6f, shoulderY + torsoLen * 0.15f), Offset(hipX + 8f, shoulderY + torsoLen * 0.42f), 2f)
            }

            ZombieType.COLOSSUS -> if (!isHit) {
                val glowK = 0.45f + 0.35f * sin(t * 2.4f + id)
                // spinal bone spurs
                for (i in 0..3) {
                    val sx = hipX + shoulderW * 0.42f - i * 2f
                    val sy = shoulderY + torsoLen * (0.05f + i * 0.2f)
                    val spur = Path().apply {
                        moveTo(sx, sy)
                        lineTo(sx + 9f + i, sy - 5f)
                        lineTo(sx + 1f, sy + 7f)
                        close()
                    }
                    drawPath(spur, Color(0xFFCFC7B0))
                }
                // cracks in the burnt skin glowing from inside
                val crack = Color(0xFFFF4A1C).copy(alpha = glowK)
                drawLine(crack, Offset(hipX - shoulderW * 0.2f, shoulderY + torsoLen * 0.1f), Offset(hipX + shoulderW * 0.05f, shoulderY + torsoLen * 0.35f), 2.2f, StrokeCap.Round)
                drawLine(crack, Offset(hipX + shoulderW * 0.05f, shoulderY + torsoLen * 0.35f), Offset(hipX - shoulderW * 0.12f, shoulderY + torsoLen * 0.62f), 1.8f, StrokeCap.Round)
                drawLine(crack, Offset(hipX + shoulderW * 0.05f, shoulderY + torsoLen * 0.35f), Offset(hipX + shoulderW * 0.28f, shoulderY + torsoLen * 0.5f), 1.6f, StrokeCap.Round)
                glow(hipX, shoulderY + torsoLen * 0.38f, shoulderW * 0.5f, Color(0xFFFF4A1C).copy(alpha = 0.14f * glowK))
            }

            else -> Unit
        }

        // dried blood on the clothes + a bite wound near the neck
        if (!isHit && type != ZombieType.ARMORED) {
            val bx = hipX - shoulderW * 0.2f + hash01(id, 11f) * shoulderW * 0.3f
            val by = shoulderY + torsoLen * (0.25f + hash01(id, 12f) * 0.3f)
            drawOval(Color(0x993A0A0C), Offset(bx, by), Size(shoulderW * 0.28f, torsoLen * 0.22f))
            drawOval(Color(0x663A0A0C), Offset(bx + 4f, by + torsoLen * 0.18f), Size(shoulderW * 0.14f, torsoLen * 0.3f))
        }

        // fresh wounds that open up as the zombie takes damage
        if (!isHit && hpRatio < 0.6f) {
            val wound = Color(0xFF6E0C12)
            drawLine(wound, Offset(hipX - shoulderW * 0.1f, shoulderY + torsoLen * 0.2f), Offset(hipX + shoulderW * 0.25f, shoulderY + torsoLen * 0.5f), 3f, StrokeCap.Round)
            drawCircle(Color(0xFFA3151C), 2.4f, Offset(hipX + shoulderW * 0.25f, shoulderY + torsoLen * 0.5f))
            if (hpRatio < 0.3f) {
                drawLine(wound, Offset(hipX - shoulderW * 0.32f, shoulderY + torsoLen * 0.65f), Offset(hipX + shoulderW * 0.05f, shoulderY + torsoLen * 0.88f), 3.2f, StrokeCap.Round)
            }
        }

        // front arm over the torso
        arm(Offset(hipX - shoulderW * 0.22f, shoulderY + h * 0.05f), false, 0f)

        // missing arm: ragged stump
        if (look.missingArm) {
            val sx = hipX + shoulderW * 0.20f
            val sy = shoulderY + h * 0.06f
            drawCircle(c(look.shirtShade), armW * 0.6f, Offset(sx, sy))
            drawCircle(Color(0xFF5E0E14), armW * 0.45f, Offset(sx - armW * 0.3f, sy + armW * 0.5f))
            drawCircle(Color(0xFFCFC6A8), armW * 0.16f, Offset(sx - armW * 0.3f, sy + armW * 0.55f))
        }

        // ---------------------------------------------------------------- head
        val headR = h * (if (type == ZombieType.COLOSSUS) 0.125f else 0.135f)
        val neckBase = Offset(hipX - shoulderW * 0.06f, shoulderY + h * 0.02f)
        val tilt = -13f + sin(phase * 0.5f + id * 0.7f) * 5f
        withTransform({ rotate(degrees = tilt, pivot = neckBase) }) {
            val hx = neckBase.x - headR * 0.10f
            val hy = neckBase.y - headR * 0.95f

            // hood bunched behind the head for the runner
            if (type == ZombieType.RUNNER) {
                drawOval(c(look.shirtShade), Offset(hx - headR * 0.2f, hy + headR * 0.2f), Size(headR * 1.6f, headR * 1.1f))
            }

            // neck
            drawRect(c(look.skinShade), Offset(neckBase.x - headR * 0.32f, neckBase.y - headR * 0.9f), Size(headR * 0.64f, headR * 1.1f))
            // bite wound on the neck
            if (!isHit && hash01(id, 13f) < 0.5f) {
                drawCircle(Color(0xFF5E0E14), headR * 0.16f, Offset(neckBase.x - headR * 0.1f, neckBase.y - headR * 0.35f))
                drawCircle(Color(0xFF2A0508), headR * 0.08f, Offset(neckBase.x - headR * 0.1f, neckBase.y - headR * 0.35f))
            }

            // long stringy hair behind the head
            if (look.hair != null && look.longHair && type != ZombieType.ARMORED) {
                val hc = c(look.hair)
                for (i in 0..4) {
                    val sx = hx + headR * (0.55f - i * 0.28f)
                    drawLine(
                        hc,
                        Offset(sx, hy - headR * 0.3f),
                        Offset(sx + headR * 0.28f + sin(phase + i) * 1.4f, hy + headR * (1.2f + i * 0.08f)),
                        max(1.4f, headR * 0.17f),
                        StrokeCap.Round
                    )
                }
            }

            // skull
            val headW = headR * 1.82f
            val headH = headR * 2.1f
            drawOval(
                Brush.radialGradient(
                    listOf(c(look.skinLight), c(look.skin), c(look.skinShade)),
                    center = Offset(hx + headR * 0.35f, hy - headR * 0.35f),
                    radius = headR * 1.7f
                ),
                Offset(hx - headW / 2f, hy - headH / 2f),
                Size(headW, headH)
            )
            // sunken cheek + temple shadows, moonlit rim on the right
            drawOval(Color(0x33000000), Offset(hx - headR * 0.75f, hy + headR * 0.05f), Size(headR * 0.7f, headR * 0.55f))
            drawOval(Color(0x22000000), Offset(hx + headR * 0.1f, hy - headR * 0.1f), Size(headR * 0.6f, headR * 0.55f))
            drawArc(
                color = Color(0x2ED5EAE2),
                startAngle = -80f,
                sweepAngle = 110f,
                useCenter = false,
                topLeft = Offset(hx - headW / 2f, hy - headH / 2f),
                size = Size(headW, headH),
                style = Stroke(width = 1.4f)
            )
            // ear
            drawOval(c(look.skinShade), Offset(hx + headR * 0.55f, hy - headR * 0.1f), Size(headR * 0.3f, headR * 0.5f))

            // eyes: dark sunken sockets, milky eyeballs, tiny dead pupils
            val ex = hx - headR * 0.42f
            val ey = hy - headR * 0.12f
            val tint = when (type) {
                ZombieType.RUNNER, ZombieType.COLOSSUS -> Color(0xFFB5301F)
                ZombieType.SPITTER -> Color(0xFFB0B63A)
                else -> Color(0xFF8E7A52)
            }
            drawOval(Color(0xCC180F0E), Offset(ex - headR * 0.30f, ey - headR * 0.23f), Size(headR * 0.62f, headR * 0.50f))
            drawOval(c(Color(0xFFD9D4BC)), Offset(ex - headR * 0.20f, ey - headR * 0.15f), Size(headR * 0.42f, headR * 0.32f))
            drawCircle(c(tint), headR * 0.10f, Offset(ex - headR * 0.06f, ey + headR * 0.01f))
            drawCircle(Color(0xFF120A09), headR * 0.05f, Offset(ex - headR * 0.07f, ey + headR * 0.01f))
            if (type == ZombieType.RUNNER || type == ZombieType.COLOSSUS) {
                glow(ex - headR * 0.06f, ey, headR * 0.55f, tint.copy(alpha = 0.30f))
            }
            // far eye, mostly in shadow
            drawOval(Color(0xAA180F0E), Offset(hx + headR * 0.08f, ey - headR * 0.18f), Size(headR * 0.42f, headR * 0.36f))
            drawCircle(c(Color(0xFFBDB89E)), headR * 0.07f, Offset(hx + headR * 0.26f, ey))
            // brow ridge
            drawLine(Color(0x66000000), Offset(ex - headR * 0.32f, ey - headR * 0.27f), Offset(ex + headR * 0.40f, ey - headR * 0.20f), 1.6f, StrokeCap.Round)

            // nose: collapsed cartilage, two dark holes
            drawCircle(Color(0xCC1A0F0D), headR * 0.07f, Offset(hx - headR * 0.78f, hy + headR * 0.16f))
            drawCircle(Color(0xAA1A0F0D), headR * 0.06f, Offset(hx - headR * 0.6f, hy + headR * 0.22f))

            // mouth: dark cavity, uneven teeth, lower jaw hanging open
            val attackOpen = if (z.isAttacking) abs(sin(t * 10f + id)) * headR * 0.30f else 0f
            val drop = headR * look.jawHang + attackOpen
            val mx = hx - headR * 0.82f
            val my = hy + headR * 0.40f
            drawOval(Color(0xFF1D0A0C), Offset(mx, my - headR * 0.04f), Size(headR * 0.95f, headR * 0.20f + drop))
            // upper teeth
            for (i in 0..3) {
                val tx = mx + headR * (0.10f + i * 0.21f)
                drawLine(
                    c(Color(0xFFD2C9A4)),
                    Offset(tx, my - headR * 0.03f),
                    Offset(tx + (if (i % 2 == 0) 0.6f else -0.5f), my + headR * (0.14f + (i % 3) * 0.03f)),
                    max(1.2f, headR * 0.10f),
                    StrokeCap.Round
                )
            }
            // jaw
            val jaw = Path().apply {
                moveTo(hx + headR * 0.40f, hy + headR * 0.30f)
                quadraticTo(hx + headR * 0.10f, hy + headR * 1.00f + drop, hx - headR * 0.55f, hy + headR * 0.88f + drop)
                lineTo(hx - headR * 0.86f, hy + headR * 0.52f + drop * 0.7f)
                lineTo(hx - headR * 0.36f, hy + headR * 0.34f + drop * 0.25f)
                close()
            }
            drawPath(jaw, Brush.linearGradient(listOf(c(look.skin), c(look.skinShade)), Offset(hx, hy), Offset(hx, hy + headR * 1.2f)))
            // lower teeth + drool
            for (i in 0..2) {
                val tx = mx + headR * (0.18f + i * 0.24f)
                drawLine(
                    c(Color(0xFFC4BB98)),
                    Offset(tx, my + drop + headR * 0.20f),
                    Offset(tx, my + drop + headR * 0.04f),
                    max(1.1f, headR * 0.09f),
                    StrokeCap.Round
                )
            }
            val droolColor = if (type == ZombieType.SPITTER) Color(0xCCB4BE3C) else Color(0x88C9D4C8)
            val dl = headR * (0.45f + 0.25f * sin(phase * 2f + id))
            drawLine(droolColor, Offset(mx + headR * 0.3f, my + drop + headR * 0.22f), Offset(mx + headR * 0.28f, my + drop + headR * 0.22f + dl), 1.4f, StrokeCap.Round)

            // hair on top: patchy / receding
            if (look.hair != null && type != ZombieType.ARMORED) {
                val hc = c(look.hair)
                val patch = Path().apply {
                    moveTo(hx - headR * 0.20f, hy - headH * 0.48f)
                    cubicTo(hx + headR * 0.6f, hy - headH * 0.62f, hx + headR * 1.0f, hy - headR * 0.4f, hx + headR * 0.92f, hy + headR * 0.1f)
                    cubicTo(hx + headR * 0.6f, hy - headR * 0.35f, hx + headR * 0.2f, hy - headR * 0.55f, hx - headR * 0.20f, hy - headH * 0.48f)
                    close()
                }
                drawPath(patch, hc)
                for (i in 0..2) {
                    drawLine(
                        hc,
                        Offset(hx + headR * (0.3f - i * 0.35f), hy - headH * 0.47f),
                        Offset(hx + headR * (0.18f - i * 0.4f), hy - headH * 0.30f),
                        1.6f,
                        StrokeCap.Round
                    )
                }
            }

            // helmet for the armored one
            if (type == ZombieType.ARMORED) {
                val helmet = Path().apply {
                    moveTo(hx - headW * 0.56f, hy + headR * 0.05f)
                    cubicTo(hx - headW * 0.62f, hy - headH * 0.72f, hx + headW * 0.62f, hy - headH * 0.72f, hx + headW * 0.58f, hy + headR * 0.15f)
                    lineTo(hx + headW * 0.3f, hy + headR * 0.28f)
                    lineTo(hx - headW * 0.56f, hy + headR * 0.05f)
                    close()
                }
                drawPath(helmet, Brush.verticalGradient(listOf(Color(0xFF3E4A44), Color(0xFF1B2420)), hy - headH * 0.6f, hy + headR * 0.3f))
                drawLine(Color(0x55DDEEE8), Offset(hx - headW * 0.3f, hy - headH * 0.42f), Offset(hx + headW * 0.2f, hy - headH * 0.46f), 1.6f, StrokeCap.Round)
                // cracked visor
                drawRect(Color(0xAA0F1A1E), Offset(hx - headW * 0.55f, hy - headR * 0.30f), Size(headW * 0.8f, headR * 0.34f))
                drawLine(Color(0x99CFE8F0), Offset(hx - headW * 0.30f, hy - headR * 0.30f), Offset(hx - headW * 0.12f, hy + headR * 0.04f), 1f)
                drawLine(Color(0x99CFE8F0), Offset(hx - headW * 0.12f, hy + headR * 0.04f), Offset(hx + headW * 0.02f, hy - headR * 0.18f), 1f)
            }
        }
    }

    // ------------------------------------------------------------ water around the legs
    val ripple = 0.5f + 0.5f * sin(t * 3f + id)
    drawOval(
        color = Color(0xFF0A1A1A).copy(alpha = 0.55f),
        topLeft = Offset(x - w * 0.62f, waterLine - 3f),
        size = Size(w * 1.24f, 9f)
    )
    drawOval(
        color = Color(0xFFB8E0D8).copy(alpha = 0.16f + 0.10f * ripple),
        topLeft = Offset(x - w * 0.66f, waterLine - 4f),
        size = Size(w * 1.32f, 11f),
        style = Stroke(width = 1.6f)
    )
    drawOval(
        color = Color(0xFFB8E0D8).copy(alpha = 0.10f),
        topLeft = Offset(x - w * 0.86f, waterLine - 6f),
        size = Size(w * 1.72f, 16f),
        style = Stroke(width = 1.1f)
    )
}
