package com.example.game.ui

import android.graphics.Paint
import android.graphics.Typeface
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import com.example.game.model.Balance

/**
 * Pixel-art plumbing. A sprite is a list of text rows (see SpriteData), one letter per pixel. It is drawn as
 * horizontal runs of same-coloured rectangles, so the chunky retro look stays sharp at any resolution.
 */
internal class PixelSprite(val rows: Array<String>) {
    class Run(val row: Int, val col: Int, val len: Int, val ch: Char)

    val width: Int = rows.maxOf { it.length }
    val height: Int = rows.size
    val runs: List<Run>

    init {
        val list = ArrayList<Run>()
        for ((r, line) in rows.withIndex()) {
            var c = 0
            while (c < line.length) {
                val ch = line[c]
                if (ch == '.') {
                    c++
                    continue
                }
                var e = c
                while (e + 1 < line.length && line[e + 1] == ch) e++
                list.add(Run(r, c, e - c + 1, ch))
                c = e + 1
            }
        }
        runs = list
    }
}

/** Letter -> colour table. A palette can override a few letters to recolour a sprite (zombie variants). */
internal class PixelPalette(overrides: Map<Char, Long> = emptyMap()) {
    private val table = arrayOfNulls<Color>(128)
    private val flashTable = arrayOfNulls<Color>(128)

    init {
        for ((ch, argb) in SpriteData.PALETTE) set(ch, argb)
        for ((ch, argb) in overrides) set(ch, argb)
    }

    private fun set(ch: Char, argb: Long) {
        val c = Color(argb)
        table[ch.code] = c
        flashTable[ch.code] = Color(
            red = c.red + (1f - c.red) * 0.7f,
            green = c.green + (1f - c.green) * 0.7f,
            blue = c.blue + (1f - c.blue) * 0.7f,
            alpha = 1f
        )
    }

    fun color(ch: Char, flash: Boolean = false): Color {
        val i = ch.code
        if (i >= 128) return Color.Magenta
        return (if (flash) flashTable[i] else table[i]) ?: Color.Magenta
    }
}

/**
 * Draws a sprite with its top-left corner at (left, top). `pixel` is the size of one sprite pixel in world units.
 * `flipX` mirrors it around its own centre; `flash` paints it nearly white (hit feedback).
 */
internal fun DrawScope.drawPixelSprite(
    sprite: PixelSprite,
    palette: PixelPalette,
    left: Float,
    top: Float,
    pixel: Float = Balance.PX,
    flipX: Boolean = false,
    flash: Boolean = false,
    alpha: Float = 1f
) {
    for (run in sprite.runs) {
        val col = if (flipX) sprite.width - run.col - run.len else run.col
        var c = palette.color(run.ch, flash)
        if (alpha < 1f) c = c.copy(alpha = alpha)
        drawRect(
            color = c,
            topLeft = Offset(left + col * pixel, top + run.row * pixel),
            size = Size(run.len * pixel + 0.4f, pixel + 0.4f)
        )
    }
}

internal object Sprites {
    val ZOMBIE = arrayOf(PixelSprite(SpriteData.ZOMBIE_0), PixelSprite(SpriteData.ZOMBIE_1), PixelSprite(SpriteData.ZOMBIE_2))
    val HERO_BODY = PixelSprite(SpriteData.HERO_BODY)
    val GIRL = arrayOf(PixelSprite(SpriteData.GIRL_0), PixelSprite(SpriteData.GIRL_1), PixelSprite(SpriteData.GIRL_2))
    val GIRL_CARRY = PixelSprite(SpriteData.GIRL_CARRY)
    val ARM_REVOLVER = PixelSprite(SpriteData.ARM_REVOLVER)
    val ARM_SHOTGUN = PixelSprite(SpriteData.ARM_SHOTGUN)
    val ARM_GRENADE = PixelSprite(SpriteData.ARM_GRENADE)
    val CRATE = PixelSprite(SpriteData.CRATE)
    val ICON_REVOLVER = PixelSprite(SpriteData.ICON_REVOLVER)
    val ICON_SHOTGUN = PixelSprite(SpriteData.ICON_SHOTGUN)
    val ICON_GRENADE = PixelSprite(SpriteData.ICON_GRENADE)
    val ICON_BULLET = PixelSprite(SpriteData.ICON_BULLET)
    val ICON_SHELL = PixelSprite(SpriteData.ICON_SHELL)

    val BASE_PALETTE = PixelPalette()
    /** Gaunt and fast: pale skin, blond straggly hair, a teal hoodie and dark track pants. */
    val RUNNER_PALETTE = PixelPalette(
        mapOf(
            'j' to 0xFF34606CL, 'J' to 0xFF50828CL, 'k' to 0xFF224450L,
            'p' to 0xFF2E2E34L, 'P' to 0xFF464650L,
            's' to 0xFFAAB896L, 'S' to 0xFF82906EL,
            'g' to 0xFFA0B08AL, 'G' to 0xFF7C8C68L,
            'h' to 0xFFAA966EL, 'H' to 0xFFC8B48CL
        )
    )

    /** Big and slow: grey skin, bald-ish, a leather vest and heavy dark trousers. */
    val BRUTE_PALETTE = PixelPalette(
        mapOf(
            'j' to 0xFF5C4E34L, 'J' to 0xFF7A6846L, 'k' to 0xFF3E3222L,
            'p' to 0xFF3C3028L, 'P' to 0xFF544438L,
            's' to 0xFF889678L, 'S' to 0xFF647258L,
            'g' to 0xFF7A8A68L, 'G' to 0xFF5C6A4EL,
            'h' to 0xFF1E1E1CL, 'H' to 0xFF32322EL, 'b' to 0xFF1C1612L
        )
    )
}

// ------------------------------------------------------------------------------------------------ text

private val textFill = Paint(Paint.ANTI_ALIAS_FLAG).apply {
    textAlign = Paint.Align.CENTER
    typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
}
private val textStroke = Paint(Paint.ANTI_ALIAS_FLAG).apply {
    textAlign = Paint.Align.CENTER
    typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
    style = Paint.Style.STROKE
    strokeJoin = Paint.Join.ROUND
}

/** Bold text with a black outline, centred on x. `size` and `y` (baseline) are in world units. */
internal fun DrawScope.drawOutlinedText(
    text: String,
    x: Float,
    y: Float,
    size: Float,
    color: Color,
    alpha: Float = 1f,
    outlineColor: Color = Color.Black
) {
    drawIntoCanvas { canvas ->
        val native = canvas.nativeCanvas
        textStroke.textSize = size
        textStroke.strokeWidth = (size * 0.22f).coerceAtLeast(1.5f)
        textStroke.color = outlineColor.copy(alpha = alpha).toArgb()
        textFill.textSize = size
        textFill.color = color.copy(alpha = alpha).toArgb()
        native.drawText(text, x, y, textStroke)
        native.drawText(text, x, y, textFill)
    }
}
