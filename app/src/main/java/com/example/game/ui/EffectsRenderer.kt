package com.example.game.ui

import android.graphics.Paint
import android.graphics.Typeface
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.sp
import com.example.game.engine.ActiveFireZone
import com.example.game.model.Particle
import com.example.game.model.ParticleType
import com.example.game.model.Projectile
import com.example.game.model.ProjectileType
import kotlin.math.PI
import kotlin.math.absoluteValue
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.sin

/**
 * Projectiles, particles, fire and lighting effects. Light-emitting things (muzzle flashes,
 * explosions, tracers, flames, embers) are drawn with additive blending so they actually glow
 * against the dark swamp.
 */

private val textFillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
    textAlign = Paint.Align.CENTER
    typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
}

private val textStrokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
    textAlign = Paint.Align.CENTER
    typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
    style = Paint.Style.STROKE
    strokeJoin = Paint.Join.ROUND
}

/** Soft additive glow: bright core fading to nothing. */
internal fun DrawScope.glow(
    cx: Float,
    cy: Float,
    radius: Float,
    core: Color,
    additive: Boolean = true
) {
    if (radius <= 0f || core.alpha <= 0f) return
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(core, core.copy(alpha = core.alpha * 0.35f), Color.Transparent),
            center = Offset(cx, cy),
            radius = radius
        ),
        radius = radius,
        center = Offset(cx, cy),
        blendMode = if (additive) BlendMode.Plus else BlendMode.SrcOver
    )
}

// ---------------------------------------------------------------------------------------------
// Lighting
// ---------------------------------------------------------------------------------------------

/** Warm light that spills around the muzzle for the few frames after every shot. */
internal fun DrawScope.drawMuzzleLight(origin: Offset, intensity: Float) {
    if (intensity <= 0f) return
    val k = (intensity / 0.08f).coerceIn(0f, 1f)
    glow(origin.x, origin.y, 130f * (0.6f + 0.4f * k), Color(0x55FFB74D).copy(alpha = 0.33f * k))
}

/** Pulsing aura on the boss and the big brutes so they read at a glance. */
internal fun DrawScope.drawEliteAura(x: Float, y: Float, width: Float, color: Color, t: Float) {
    val pulse = 0.75f + 0.25f * sin(t * 3.2f)
    glow(x, y - 20f, width * 1.5f * pulse, color.copy(alpha = 0.38f), additive = true)
    // ring on the water around its feet
    drawOval(
        color = color.copy(alpha = 0.35f * pulse),
        topLeft = Offset(x - width * 0.9f, y - 10f),
        size = Size(width * 1.8f, 22f),
        style = Stroke(width = 2.4f)
    )
}

// ---------------------------------------------------------------------------------------------
// Fire zones (Molotov pools)
// ---------------------------------------------------------------------------------------------

internal fun DrawScope.drawFireZoneFx(fire: ActiveFireZone, t: Float) {
    val r = fire.radius
    val fade = fire.remainingSeconds.coerceIn(0f, 1f) // flames die down during the last second

    // Scorched water / oil slick
    drawOval(
        color = Color(0x55120A06).copy(alpha = 0.33f * fade),
        topLeft = Offset(fire.x - r, fire.y - r * 0.28f),
        size = Size(r * 2f, r * 0.56f)
    )

    // Big flickering glow
    val pulse = 0.85f + 0.15f * sin(t * 9f)
    glow(fire.x, fire.y - 10f, r * 1.25f * pulse, Color(0xFFFF6D00).copy(alpha = 0.55f * fade))

    // Flame tongues, three layers each (red -> orange -> yellow core)
    for (i in -4..4) {
        val seed = i * 1.7f
        val fx = fire.x + i * (r * 0.19f) + sin(t * 3f + seed) * 4f
        val flick = 0.65f + 0.35f * sin(t * 8f + seed).absoluteValue
        val baseH = (34f - i.absoluteValue * 3.2f).coerceAtLeast(16f)
        val fh = baseH * flick * (0.4f + 0.6f * fade)
        val fy = fire.y + (i.absoluteValue % 2) * 6f
        val sway = sin(t * 5f + seed) * 6f

        flameTongue(fx, fy, 15f, fh, sway, Color(0xCCD84315).copy(alpha = 0.8f * fade))
        flameTongue(fx, fy, 10.5f, fh * 0.78f, sway * 0.8f, Color(0xDDFF9800).copy(alpha = fade))
        flameTongue(fx, fy, 5.5f, fh * 0.5f, sway * 0.5f, Color(0xEEFFF59D).copy(alpha = fade))
    }
}

private fun DrawScope.flameTongue(x: Float, baseY: Float, halfWidth: Float, height: Float, sway: Float, color: Color) {
    val path = Path().apply {
        moveTo(x - halfWidth, baseY)
        quadraticTo(x - halfWidth * 0.5f, baseY - height * 0.55f, x + sway, baseY - height)
        quadraticTo(x + halfWidth * 0.5f, baseY - height * 0.55f, x + halfWidth, baseY)
        close()
    }
    drawPath(path = path, color = color, blendMode = BlendMode.Plus)
}

// ---------------------------------------------------------------------------------------------
// Projectiles
// ---------------------------------------------------------------------------------------------

internal fun DrawScope.drawProjectileFx(p: Projectile, t: Float) {
    val speed = hypot(p.vx, p.vy).coerceAtLeast(1f)
    val dx = p.vx / speed
    val dy = p.vy / speed
    val head = Offset(p.x, p.y)

    when (p.type) {
        ProjectileType.BULLET -> {
            val tail = Offset(p.x - dx * 26f, p.y - dy * 26f)
            drawLine(
                brush = Brush.linearGradient(
                    colors = listOf(Color.Transparent, Color(0xFFFFB300), Color(0xFFFFF59D)),
                    start = tail,
                    end = head
                ),
                start = tail,
                end = head,
                strokeWidth = 3.2f,
                cap = StrokeCap.Round,
                blendMode = BlendMode.Plus
            )
            glow(p.x, p.y, 11f, Color(0xFFFFE082).copy(alpha = 0.55f))
            drawCircle(Color(0xFFFFFDE7), p.radius * 0.6f, head)
        }

        ProjectileType.PELLET -> {
            val tail = Offset(p.x - dx * 12f, p.y - dy * 12f)
            drawLine(
                brush = Brush.linearGradient(
                    colors = listOf(Color.Transparent, Color(0xFFFF9800)),
                    start = tail,
                    end = head
                ),
                start = tail,
                end = head,
                strokeWidth = 2.6f,
                cap = StrokeCap.Round,
                blendMode = BlendMode.Plus
            )
            drawCircle(Color(0xFFFFCC80), p.radius * 0.75f, head)
        }

        ProjectileType.GRENADE -> {
            glow(p.x, p.y, 20f, Color(0xFFFF6D00).copy(alpha = 0.35f))
            val spin = t * 14f
            withTransform({ rotate(degrees = (spin * 180f / PI.toFloat()) % 360f, pivot = head) }) {
                drawCircle(Color(0xFF37474F), p.radius, head)
                drawCircle(Color(0xFF546E7A), p.radius * 0.55f, Offset(p.x - p.radius * 0.25f, p.y - p.radius * 0.25f))
                drawLine(Color(0xFF1B2428), Offset(p.x - p.radius, p.y), Offset(p.x + p.radius, p.y), 2f)
            }
            // blinking warning light
            val blink = if (((t * 12f).toInt() % 2) == 0) 1f else 0.35f
            drawCircle(Color(0xFFFF1744).copy(alpha = blink), 2.6f, Offset(p.x + dx * 4f, p.y + dy * 4f - 3f))
        }

        ProjectileType.ACID_SPIT -> {
            val wob = 1f + 0.18f * sin(t * 22f + p.id)
            // dripping tail
            drawCircle(Color(0x8864DD17), p.radius * 0.7f, Offset(p.x - dx * 9f, p.y - dy * 9f))
            drawCircle(Color(0x5564DD17), p.radius * 0.45f, Offset(p.x - dx * 17f, p.y - dy * 17f))
            glow(p.x, p.y, p.radius * 3.4f, Color(0xFF76FF03).copy(alpha = 0.5f))
            drawCircle(Color(0xFF76FF03), p.radius * wob, head)
            drawCircle(Color(0xFFE6FFB0), p.radius * 0.45f, Offset(p.x - 1.5f, p.y - 1.5f))
        }

        ProjectileType.BOLT -> {
            val tail = Offset(p.x - dx * 30f, p.y - dy * 30f)
            // shaft
            drawLine(Color(0xFF8D6E63), tail, head, 3.2f, StrokeCap.Round)
            // steel head
            val px = -dy
            val py = dx
            val tip = Offset(p.x + dx * 9f, p.y + dy * 9f)
            val headPath = Path().apply {
                moveTo(tip.x, tip.y)
                lineTo(p.x + px * 4.5f, p.y + py * 4.5f)
                lineTo(p.x - px * 4.5f, p.y - py * 4.5f)
                close()
            }
            drawPath(headPath, Color(0xFFCFD8DC))
            // fletching
            for (s in intArrayOf(-1, 1)) {
                drawLine(
                    Color(0xFFB0BEC5),
                    Offset(tail.x + dx * 7f, tail.y + dy * 7f),
                    Offset(tail.x - dx * 2f + px * 4.5f * s, tail.y - dy * 2f + py * 4.5f * s),
                    1.8f
                )
            }
            glow(tip.x, tip.y, 9f, Color(0xFFB2FF59).copy(alpha = 0.25f))
        }

        ProjectileType.FLAME -> {
            val age = (1f - p.lifetime / 0.55f).coerceIn(0f, 1f)
            val r = p.radius * (1.1f + age * 1.9f)
            val color = when {
                age < 0.35f -> Color(0xFFFFEB3B)
                age < 0.7f -> Color(0xFFFF9800)
                else -> Color(0xFFD84315)
            }
            glow(p.x, p.y, r * 1.8f, color.copy(alpha = (0.75f - age * 0.55f).coerceAtLeast(0.1f)))
            drawCircle(
                color = Color(0xFFFFF8E1).copy(alpha = (1f - age).coerceAtLeast(0f) * 0.8f),
                radius = r * 0.35f,
                center = head,
                blendMode = BlendMode.Plus
            )
        }

        ProjectileType.SNIPER_BULLET -> {
            val tail = Offset(p.x - dx * 70f, p.y - dy * 70f)
            drawLine(
                brush = Brush.linearGradient(
                    colors = listOf(Color.Transparent, Color(0x8800E5FF), Color.White),
                    start = tail,
                    end = head
                ),
                start = tail,
                end = head,
                strokeWidth = 6f,
                cap = StrokeCap.Round,
                blendMode = BlendMode.Plus
            )
            drawLine(
                brush = Brush.linearGradient(
                    colors = listOf(Color.Transparent, Color(0xFFFFD54F)),
                    start = tail,
                    end = head
                ),
                start = tail,
                end = head,
                strokeWidth = 2.6f,
                cap = StrokeCap.Round,
                blendMode = BlendMode.Plus
            )
            glow(p.x, p.y, 22f, Color(0xFF80DEEA).copy(alpha = 0.55f))
            drawCircle(Color.White, p.radius * 0.7f, head)
        }
    }
}

// ---------------------------------------------------------------------------------------------
// Particles
// ---------------------------------------------------------------------------------------------

/** Flat stains on the water. Drawn BEFORE the zombies so bodies walk over them. */
internal fun DrawScope.drawDecals(particles: List<Particle>) {
    for (part in particles) {
        if (part.type != ParticleType.BLOOD_POOL) continue
        val a = (part.lifeRatio * 1.6f).coerceIn(0f, 1f)
        val grow = 1f + (1f - part.lifeRatio) * 0.35f
        val rw = part.radius * 2.6f * grow
        val rh = part.radius * 0.95f * grow
        drawOval(
            color = part.color.copy(alpha = 0.5f * a),
            topLeft = Offset(part.x - rw / 2f, part.y - rh / 2f),
            size = Size(rw, rh)
        )
        drawOval(
            color = part.color.copy(alpha = 0.28f * a),
            topLeft = Offset(part.x - rw * 0.3f, part.y - rh * 0.3f),
            size = Size(rw * 0.6f, rh * 0.6f)
        )
    }
}

/** Everything that is not a decal; floating texts go last so they are always readable. */
internal fun DrawScope.drawParticlesFx(particles: List<Particle>) {
    for (part in particles) {
        if (part.type == ParticleType.BLOOD_POOL || part.type == ParticleType.FLOATING_TEXT) continue
        drawParticleFx(part)
    }
    for (part in particles) {
        if (part.type == ParticleType.FLOATING_TEXT) drawFloatingText(part)
    }
}

private fun DrawScope.drawParticleFx(part: Particle) {
    val life = part.lifeRatio
    val a = part.alpha.coerceIn(0f, 1f)

    when (part.type) {
        ParticleType.BLOOD -> {
            val r = part.radius * (0.45f + 0.55f * life)
            drawCircle(part.color.copy(alpha = a), r, Offset(part.x, part.y))
            drawCircle(Color(0x44000000).copy(alpha = 0.25f * a), r * 0.55f, Offset(part.x + r * 0.2f, part.y + r * 0.2f))
        }

        ParticleType.TOXIC_SLIME -> {
            val r = part.radius * (0.5f + 0.5f * life)
            glow(part.x, part.y, r * 2.4f, part.color.copy(alpha = 0.35f * a))
            drawCircle(part.color.copy(alpha = a), r, Offset(part.x, part.y))
            drawCircle(Color(0xFFE6FFB0).copy(alpha = 0.7f * a), r * 0.35f, Offset(part.x - r * 0.25f, part.y - r * 0.25f))
        }

        ParticleType.WATER_SPLASH -> {
            val r = part.radius * (0.5f + 0.5f * life)
            drawCircle(part.color.copy(alpha = 0.75f * a), r, Offset(part.x, part.y))
            drawCircle(Color.White.copy(alpha = 0.6f * a), r * 0.4f, Offset(part.x - r * 0.25f, part.y - r * 0.3f))
        }

        ParticleType.EXPLOSION_FIRE -> {
            // Fireball that expands while it fades: white-hot core -> orange -> transparent
            val r = part.radius * part.scale * (0.55f + (1f - life) * 0.9f)
            glow(part.x, part.y, r * 1.6f, part.color.copy(alpha = 0.7f * a))
            drawCircle(
                color = Color(0xFFFFF3C4).copy(alpha = a * life),
                radius = r * 0.4f,
                center = Offset(part.x, part.y),
                blendMode = BlendMode.Plus
            )
        }

        ParticleType.SMOKE -> {
            val r = part.radius * part.scale
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(part.color.copy(alpha = 0.55f * a), part.color.copy(alpha = 0.18f * a), Color.Transparent),
                    center = Offset(part.x, part.y),
                    radius = r
                ),
                radius = r,
                center = Offset(part.x, part.y)
            )
        }

        ParticleType.EMBER -> {
            val flicker = 0.6f + 0.4f * sin(part.life * 40f + part.id)
            glow(part.x, part.y, part.radius * 3f, Color(0xFFFF9100).copy(alpha = 0.5f * a * flicker))
            drawCircle(
                color = Color(0xFFFFE082).copy(alpha = a * flicker),
                radius = part.radius * 0.55f,
                center = Offset(part.x, part.y),
                blendMode = BlendMode.Plus
            )
        }

        ParticleType.SPARK -> {
            val len = 0.035f
            drawLine(
                color = part.color.copy(alpha = a),
                start = Offset(part.x, part.y),
                end = Offset(part.x - part.vx * len, part.y - part.vy * len),
                strokeWidth = part.radius * 0.7f,
                cap = StrokeCap.Round,
                blendMode = BlendMode.Plus
            )
        }

        ParticleType.SHELL_CASING -> {
            val spin = (part.rotation + (1f - life) * 14f) * 180f / PI.toFloat()
            val c = Offset(part.x, part.y)
            withTransform({ rotate(degrees = spin, pivot = c) }) {
                drawRect(
                    color = Color(0xFFE0B040).copy(alpha = a),
                    topLeft = Offset(part.x - 3.5f, part.y - 1.4f),
                    size = Size(7f, 2.8f)
                )
                drawRect(
                    color = Color(0xFFFFE9A0).copy(alpha = 0.8f * a),
                    topLeft = Offset(part.x - 3.5f, part.y - 1.4f),
                    size = Size(7f, 1f)
                )
            }
        }

        ParticleType.MUZZLE_FLASH -> {
            val r = part.radius * part.scale * (0.7f + 0.5f * life)
            glow(part.x, part.y, r * 2.6f, Color(0xFFFFB300).copy(alpha = 0.6f * a))
            // starburst spikes
            for (i in 0 until 6) {
                val ang = (i - 2.5f) * 0.32f
                val len = r * (if (i % 2 == 0) 2.3f else 1.5f)
                drawLine(
                    color = Color(0xFFFFF59D).copy(alpha = a),
                    start = Offset(part.x, part.y),
                    end = Offset(part.x + cos(ang) * len, part.y + sin(ang) * len),
                    strokeWidth = 2.6f,
                    cap = StrokeCap.Round,
                    blendMode = BlendMode.Plus
                )
            }
            drawCircle(Color(0xFFFFFDE7).copy(alpha = a), r * 0.5f, Offset(part.x, part.y), blendMode = BlendMode.Plus)
        }

        ParticleType.BLOOD_POOL, ParticleType.FLOATING_TEXT -> Unit // handled elsewhere
    }
}

private fun DrawScope.drawFloatingText(part: Particle) {
    val age = part.maxLife - part.life
    val pop = if (age < 0.14f) 0.6f + (age / 0.14f) * 0.4f else 1f
    val big = part.text.length > 9
    val sizePx = (if (big) 24.sp else 16.sp).toPx() * pop
    val a = (part.lifeRatio * 2.2f).coerceIn(0f, 1f)

    textFillPaint.textSize = sizePx
    textStrokePaint.textSize = sizePx
    textStrokePaint.strokeWidth = max(2f, sizePx * 0.2f)
    textStrokePaint.color = android.graphics.Color.argb((a * 220f).toInt(), 0, 0, 0)
    textFillPaint.color = part.color.copy(alpha = a).toArgb()

    drawIntoCanvas { canvas ->
        canvas.nativeCanvas.drawText(part.text, part.x, part.y, textStrokePaint)
        canvas.nativeCanvas.drawText(part.text, part.x, part.y, textFillPaint)
    }
}
