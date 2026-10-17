package com.example.game.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.game.data.LeaderboardRepository

private val BONE = Color(0xFFE9E0C8)
private val BONE_DIM = Color(0xFFB8B09A)
private val LAMP = Color(0xFFC9A55B)
private val INK = Color(0xFF2B1D12)

@Composable
fun MainMenuScreen(
    repository: LeaderboardRepository,
    onStartGame: () -> Unit,
    onOpenLeaderboard: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showHowToPlay by remember { mutableStateOf(false) }
    val bestSeconds = remember { repository.getBestSurvivalTimeSeconds() }
    val formattedBest = remember(bestSeconds) {
        "%02d:%02d".format(bestSeconds / 60, bestSeconds % 60)
    }

    // The scene is animated, so redraw every frame
    var frame by remember { mutableLongStateOf(0L) }
    LaunchedEffect(Unit) {
        while (true) {
            withFrameNanos { frame = it }
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF050C0A))
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            @Suppress("UNUSED_VARIABLE")
            val tick = frame
            drawMenuScene(size.width, size.height)
        }

        Column(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(start = 28.dp, end = 28.dp, top = 18.dp, bottom = 18.dp),
            verticalArrangement = Arrangement.Center
        ) {
            Column(modifier = Modifier.widthIn(max = 340.dp)) {
                Text(
                    text = "NOCHE EN LA CASA",
                    style = TextStyle(
                        fontFamily = FontFamily.Serif,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 12.sp,
                        letterSpacing = 3.sp,
                        color = LAMP
                    )
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "SWAMP\nZOMBIE",
                    modifier = Modifier.rotate(-2f),
                    style = TextStyle(
                        fontFamily = FontFamily.Serif,
                        fontWeight = FontWeight.Black,
                        fontSize = 54.sp,
                        lineHeight = 50.sp,
                        letterSpacing = 1.sp,
                        color = BONE,
                        shadow = Shadow(color = Color(0xFF3A0F0A), offset = Offset(4f, 5f), blurRadius = 0f)
                    )
                )
                // rough brush stroke under the title
                Spacer(
                    Modifier
                        .padding(top = 6.dp)
                        .fillMaxWidth(0.78f)
                        .height(8.dp)
                        .drawBehind {
                            val p = Path().apply {
                                moveTo(0f, size.height * 0.6f)
                                cubicTo(
                                    size.width * 0.25f, size.height * 0.1f,
                                    size.width * 0.6f, size.height * 1.0f,
                                    size.width, size.height * 0.35f
                                )
                            }
                            drawPath(p, Color(0xFF8A3A1F), style = Stroke(width = 4.dp.toPx(), cap = StrokeCap.Round))
                        }
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "Defiende la cabaña. Aguanta hasta el amanecer.",
                    style = TextStyle(
                        fontFamily = FontFamily.Serif,
                        fontStyle = FontStyle.Italic,
                        fontSize = 14.sp,
                        color = BONE_DIM
                    )
                )

                Spacer(Modifier.height(20.dp))

                PlankButton(
                    text = "JUGAR",
                    tag = "menu_start_button",
                    onClick = onStartGame,
                    tilt = -1.2f,
                    primary = true,
                    seed = 1
                )
                Spacer(Modifier.height(10.dp))
                PlankButton(
                    text = "RÉCORDS",
                    tag = "menu_leaderboard_button",
                    onClick = onOpenLeaderboard,
                    tilt = 0.8f,
                    seed = 2
                )
                Spacer(Modifier.height(10.dp))
                PlankButton(
                    text = "CÓMO SOBREVIVIR",
                    tag = "menu_help_button",
                    onClick = { showHowToPlay = true },
                    tilt = -0.5f,
                    seed = 3
                )

                Spacer(Modifier.height(16.dp))
                if (bestSeconds > 0) {
                    Text(
                        text = "MEJOR MARCA  $formattedBest",
                        style = TextStyle(
                            fontFamily = FontFamily.Serif,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            letterSpacing = 2.sp,
                            color = LAMP
                        )
                    )
                    Spacer(Modifier.height(4.dp))
                }
                Text(
                    text = "Toca o arrastra por el pasillo para disparar.",
                    style = TextStyle(
                        fontFamily = FontFamily.Serif,
                        fontStyle = FontStyle.Italic,
                        fontSize = 11.sp,
                        color = BONE_DIM.copy(alpha = 0.75f)
                    )
                )
            }
        }

        if (showHowToPlay) {
            Dialog(onDismissRequest = { showHowToPlay = false }) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.96f)
                        .rotate(-0.6f)
                        .drawBehind { drawPaperNote() }
                        .padding(horizontal = 24.dp, vertical = 26.dp)
                ) {
                    Column(
                        modifier = Modifier.verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(9.dp)
                    ) {
                        Text(
                            text = "CÓMO SOBREVIVIR",
                            style = TextStyle(
                                fontFamily = FontFamily.Serif,
                                fontWeight = FontWeight.Black,
                                fontSize = 20.sp,
                                letterSpacing = 1.sp,
                                color = INK
                            )
                        )
                        listOf(
                            "Toca o arrastra por el pasillo: Mara apunta hacia tu dedo y dispara mientras lo mantengas.",
                            "Solo hay tres armas. Revólver: 6 balas. Escopeta: 3 cartuchos, triple de daño, pero solo llega hasta la mitad del pasillo. Granada: limpia a todos los zombis de la pantalla, pero solo llevas UNA hasta que Nina te traiga otra.",
                            "Los disparos a la cabeza hacen mucho más daño.",
                            "RECARGAR es al instante, pero mete UNA sola bala por pulsación: toca varias veces para llenar el arma.",
                            "La munición de reserva es limitada. Pulsa MUNICIÓN y Nina irá por una caja detrás de la puerta. Solo trae munición del arma que tengas equipada en ese momento.",
                            "Si un zombi te alcanza, te han atrapado: la partida termina al instante."
                        ).forEach { line ->
                            Text(
                                text = "—  $line",
                                style = TextStyle(
                                    fontFamily = FontFamily.Serif,
                                    fontSize = 13.sp,
                                    lineHeight = 18.sp,
                                    color = Color(0xFF3A2A1B)
                                )
                            )
                        }
                        Spacer(Modifier.height(4.dp))
                        PlankButton(
                            text = "ENTENDIDO",
                            tag = "menu_help_close",
                            onClick = { showHowToPlay = false },
                            tilt = 0.5f,
                            primary = true,
                            seed = 4
                        )
                    }
                }
            }
        }
    }
}

/** Wooden plank with grain, a knot, nails and a slight tilt. Sinks a little when pressed. */
@Composable
private fun PlankButton(
    text: String,
    tag: String,
    onClick: () -> Unit,
    tilt: Float,
    seed: Int,
    primary: Boolean = false
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()

    Box(
        modifier = Modifier
            .testTag(tag)
            .fillMaxWidth()
            .rotate(tilt)
            .offset(y = if (pressed) 2.dp else 0.dp)
            .height(if (primary) 58.dp else 46.dp)
            .drawBehind { drawPlank(pressed, primary, seed) }
            .clickable(interactionSource = interaction, indication = null, onClick = onClick),
        contentAlignment = Alignment.CenterStart
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(start = if (primary) 30.dp else 26.dp),
            style = TextStyle(
                fontFamily = FontFamily.Serif,
                fontWeight = FontWeight.Black,
                fontSize = if (primary) 22.sp else 16.sp,
                letterSpacing = 2.sp,
                color = if (pressed) BONE_DIM else BONE,
                shadow = Shadow(color = Color(0xCC140D08), offset = Offset(2f, 2f), blurRadius = 0f)
            )
        )
    }
}

private fun DrawScope.drawPlank(pressed: Boolean, primary: Boolean, seed: Int) {
    val base = if (primary) Color(0xFF6A3F25) else Color(0xFF4A3524)
    val top = lerp(base, Color.White, if (pressed) 0.02f else 0.10f)
    val bottom = lerp(base, Color.Black, 0.30f)
    val r = CornerRadius(4.dp.toPx(), 4.dp.toPx())

    // drop shadow
    drawRoundRect(Color(0x66000000), Offset(3f, if (pressed) 3f else 6f), size, r)
    // wood body
    drawRoundRect(Brush.verticalGradient(listOf(top, base, bottom)), size = size, cornerRadius = r)

    // grain lines
    for (i in 0 until 8) {
        val y = size.height * (0.10f + 0.11f * i) + hash01(seed * 10 + i, 1) * 3f
        val x0 = hash01(seed * 10 + i, 2) * size.width * 0.12f
        val x1 = size.width * (0.5f + hash01(seed * 10 + i, 3) * 0.5f)
        drawLine(Color(0x2E000000), Offset(x0, y), Offset(x1, y + (hash01(seed * 10 + i, 4) - 0.5f) * 3.5f), 1.2f)
    }
    // knot
    val kx = size.width * (0.55f + hash01(seed, 5) * 0.3f)
    val ky = size.height * (0.35f + hash01(seed, 6) * 0.3f)
    drawOval(Color(0x44000000), Offset(kx - 6f, ky - 4f), Size(12f, 8f))
    drawOval(Color(0x33000000), Offset(kx - 9f, ky - 6.5f), Size(18f, 13f), style = Stroke(1f))

    // worn top edge
    drawLine(Color(0x22FFFFFF), Offset(5f, 2f), Offset(size.width - 5f, 2f), 1.5f)

    // painted rust stripe on the primary plank
    if (primary) {
        drawRect(Color(0xFFB4491F), Offset(10.dp.toPx(), 8.dp.toPx()), Size(4.dp.toPx(), size.height - 16.dp.toPx()))
    }

    // nails
    val nailY = size.height / 2f
    val nailXs = floatArrayOf(10.dp.toPx() + (if (primary) 10.dp.toPx() else 0f), size.width - 12.dp.toPx())
    for (nx in nailXs) {
        drawCircle(Color(0xFF16100B), 3.2f, Offset(nx, nailY))
        drawCircle(Color(0xFF857B6D), 1.3f, Offset(nx - 0.7f, nailY - 0.8f))
    }

    // dark outline
    drawRoundRect(Color(0xAA140D08), size = size, cornerRadius = r, style = Stroke(1.6f))
}

private fun DrawScope.drawPaperNote() {
    val r = CornerRadius(3.dp.toPx(), 3.dp.toPx())
    drawRoundRect(Color(0x66000000), Offset(4f, 6f), size, r)
    drawRoundRect(
        Brush.verticalGradient(listOf(Color(0xFFDDD1B3), Color(0xFFCFC2A2))),
        size = size,
        cornerRadius = r
    )
    // stains and fold marks
    drawOval(Color(0x22704A22), Offset(size.width * 0.62f, size.height * 0.10f), Size(size.width * 0.3f, size.height * 0.18f))
    drawOval(Color(0x1A704A22), Offset(size.width * 0.08f, size.height * 0.72f), Size(size.width * 0.22f, size.height * 0.14f))
    drawLine(Color(0x22000000), Offset(0f, size.height * 0.5f), Offset(size.width, size.height * 0.5f), 1.2f)
    drawLine(Color(0x14FFFFFF), Offset(0f, size.height * 0.5f + 1.5f), Offset(size.width, size.height * 0.5f + 1.5f), 1.2f)
    // tape on the top corners
    val tape = Color(0xAAB9A877)
    drawRect(tape, Offset(-6f, -4f), Size(46f, 16f))
    drawRect(tape, Offset(size.width - 40f, -4f), Size(46f, 16f))
    drawRoundRect(Color(0x55342414), size = size, cornerRadius = r, style = Stroke(1.2f))
}
