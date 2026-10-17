package com.example.game.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.game.engine.GameEngine
import com.example.game.engine.GameStatus
import com.example.game.engine.RunnerState
import com.example.game.model.Balance
import com.example.game.model.WeaponType
import kotlin.math.min

private val BAR_TOP = Color(0xFF8A8A8A)
private val BAR_BOTTOM = Color(0xFF5E5E5E)
private val BAR_EDGE = Color(0xFF262626)
private val MONO = FontFamily.Monospace

private val outlinedWhite = TextStyle(
    fontFamily = MONO,
    fontWeight = FontWeight.Black,
    color = Color.White,
    shadow = Shadow(color = Color.Black, offset = Offset(2f, 2f), blurRadius = 0f)
)

/**
 * Retro HUD: a grey bar across the top with the score, the three weapons, the spare ammo and the rounds
 * loaded in the gun, plus two square buttons for the one-round reload and the girl who brings ammo.
 */
@Composable
fun GameHUD(
    engine: GameEngine,
    onPauseClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val weapon = engine.currentWeapon
    val playing = engine.status == GameStatus.PLAYING

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val barHeight = maxHeight * Balance.BAR_FRACTION

        // ------------------------------------------------------------ top bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(barHeight)
                .background(Brush.verticalGradient(listOf(BAR_TOP, BAR_BOTTOM)))
                .drawBehind {
                    drawRect(BAR_EDGE, Offset(0f, size.height - 3.dp.toPx()), Size(size.width, 3.dp.toPx()))
                }
                .padding(horizontal = 10.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // score and wave
            Column(modifier = Modifier.widthIn(min = 104.dp)) {
                Text(
                    text = "Score:${engine.score}",
                    style = outlinedWhite.copy(fontSize = 20.sp),
                    modifier = Modifier.testTag("hud_score")
                )
                Text(
                    text = "Oleada ${engine.wave}  ·  ${formatTime(engine.survivedSeconds)}",
                    style = outlinedWhite.copy(fontSize = 10.sp, color = Color(0xFFE0E0E0))
                )
            }

            // the three weapons
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                engine.weapons.forEachIndexed { index, w ->
                    val selected = engine.selectedWeaponIndex == index
                    val sprite = when (w.type) {
                        WeaponType.REVOLVER -> Sprites.ICON_REVOLVER
                        WeaponType.SHOTGUN -> Sprites.ICON_SHOTGUN
                        WeaponType.GRENADE -> Sprites.ICON_GRENADE
                    }
                    Box(
                        modifier = Modifier
                            .testTag("weapon_slot_$index")
                            .width(if (w.type == WeaponType.SHOTGUN) 66.dp else 50.dp)
                            .fillMaxHeight()
                            .background(if (selected) Color(0x55000000) else Color(0x22000000))
                            .border(2.dp, if (selected) Color(0xFFD32F2F) else Color.Transparent)
                            .clickable { engine.selectWeapon(index) }
                            .padding(5.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        PixelIcon(sprite, Modifier.fillMaxSize(), alpha = if (selected) 1f else 0.7f)
                    }
                }
            }

            // spare rounds of the equipped weapon (the grenade has none: you hold one, or you wait for Nina)
            if (weapon.type != WeaponType.GRENADE) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    PixelIcon(roundSprite(weapon.type), Modifier.size(width = 14.dp, height = 28.dp))
                    Text(
                        text = "x${weapon.reserveAmmo}",
                        style = outlinedWhite.copy(
                            fontSize = 20.sp,
                            color = if (weapon.reserveAmmo == 0) Color(0xFFFF5252) else Color.White
                        ),
                        modifier = Modifier.testTag("hud_reserve")
                    )
                }
            } else if (weapon.currentAmmo == 0) {
                Text(
                    text = "¡SIN GRANADA!",
                    style = outlinedWhite.copy(fontSize = 14.sp, color = Color(0xFFFF5252)),
                    modifier = Modifier.testTag("hud_reserve")
                )
            }

            Spacer(Modifier.weight(1f))

            // rounds loaded in the gun
            Row(
                modifier = Modifier.testTag("hud_loaded"),
                horizontalArrangement = Arrangement.spacedBy(3.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val slots = weapon.magazine
                for (i in 0 until slots) {
                    val loaded = i < weapon.currentAmmo
                    val a = if (loaded) 1f else 0.2f
                    PixelIcon(
                        roundSprite(weapon.type),
                        Modifier.size(
                            width = if (weapon.type == WeaponType.REVOLVER) 16.dp else 22.dp,
                            height = if (weapon.type == WeaponType.REVOLVER) 38.dp else 42.dp
                        ),
                        alpha = a
                    )
                }
            }

            // pause
            Box(
                modifier = Modifier
                    .testTag("hud_pause_button")
                    .size(34.dp)
                    .background(Color(0x55000000))
                    .border(2.dp, Color(0xFF262626))
                    .clickable(onClick = onPauseClick),
                contentAlignment = Alignment.Center
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Box(Modifier.size(width = 5.dp, height = 14.dp).background(Color.White))
                    Box(Modifier.size(width = 5.dp, height = 14.dp).background(Color.White))
                }
            }
        }

        // ------------------------------------------------------------ right-hand buttons (on the empty wall above the heroine)
        Row(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = barHeight + 10.dp, end = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val magFull = weapon.currentAmmo >= weapon.magazine
            val canReload = playing && !magFull && weapon.reserveAmmo > 0
            val urgent = canReload && weapon.currentAmmo == 0

            SquareButton(
                tag = "hud_reload_button",
                label = "RECARGAR",
                enabled = canReload,
                highlight = urgent,
                fill = 0f,
                onClick = { engine.reloadOne() }
            ) {
                PixelIcon(
                    roundSprite(weapon.type),
                    Modifier.size(width = 18.dp, height = 30.dp),
                    alpha = if (canReload) 1f else 0.4f
                )
            }

            val busy = engine.runnerState != RunnerState.IDLE
            val cooling = !busy && engine.runnerCooldown > 0f
            val full = weapon.refillSpace <= 0
            val ready = playing && !busy && !cooling && !full
            SquareButton(
                tag = "hud_ammo_runner_button",
                label = "MUNICIÓN",
                enabled = ready,
                highlight = ready && (if (weapon.type == WeaponType.GRENADE) weapon.currentAmmo == 0 else weapon.reserveAmmo <= weapon.type.delivery / 3),
                fill = if (busy) engine.runnerTripProgress else 0f,
                onClick = { engine.callAmmoRunner() }
            ) {
                if (cooling) {
                    Text(
                        text = "${engine.runnerCooldown.toInt() + 1}",
                        style = outlinedWhite.copy(fontSize = 20.sp)
                    )
                } else {
                    PixelIcon(
                        Sprites.CRATE,
                        Modifier.size(width = 34.dp, height = 28.dp),
                        alpha = if (ready || busy) 1f else 0.4f
                    )
                }
            }
        }
    }
}

private fun formatTime(seconds: Float): String {
    val s = seconds.toInt()
    return "%02d:%02d".format(s / 60, s % 60)
}

private fun roundSprite(type: WeaponType): PixelSprite = when (type) {
    WeaponType.REVOLVER -> Sprites.ICON_BULLET
    WeaponType.SHOTGUN -> Sprites.ICON_SHELL
    WeaponType.GRENADE -> Sprites.ICON_GRENADE
}

/** A pixel-art sprite scaled to fit its box, keeping the chunky pixels. */
@Composable
private fun PixelIcon(sprite: PixelSprite, modifier: Modifier, alpha: Float = 1f) {
    Canvas(modifier = modifier) {
        val px = min(size.width / sprite.width, size.height / sprite.height)
        val left = (size.width - sprite.width * px) / 2f
        val top = (size.height - sprite.height * px) / 2f
        drawPixelSprite(sprite, Sprites.BASE_PALETTE, left, top, px, alpha = alpha)
    }
}

/** Square retro button with a caption underneath. `fill` (0..1) shows progress as a bar along the bottom. */
@Composable
private fun SquareButton(
    tag: String,
    label: String,
    enabled: Boolean,
    highlight: Boolean,
    fill: Float,
    onClick: () -> Unit,
    content: @Composable () -> Unit
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .testTag(tag)
                .size(54.dp)
                .background(if (enabled) Color(0xDD4A4A4A) else Color(0xAA2E2E2E))
                .border(
                    3.dp,
                    when {
                        highlight -> Color(0xFFFFD54F)
                        enabled -> Color(0xFFBDBDBD)
                        else -> Color(0xFF555555)
                    }
                )
                .clickable(enabled = enabled, onClick = onClick),
            contentAlignment = Alignment.Center
        ) {
            content()
            if (fill > 0f) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .height(6.dp)
                        .fillMaxWidth(fill.coerceIn(0f, 1f))
                        .background(Color(0xFF66BB6A))
                )
            }
        }
        Text(
            text = label,
            style = outlinedWhite.copy(fontSize = 9.sp),
            modifier = Modifier.padding(top = 2.dp)
        )
    }
}
