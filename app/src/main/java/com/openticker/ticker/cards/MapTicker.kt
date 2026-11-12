package com.openticker.ticker.cards

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.openticker.data.Prefs
import com.openticker.data.TickerData
import com.openticker.data.TickerInteractions
import kotlin.math.cos
import kotlin.math.sin

/* ==================================================================== */
/* 13 — Treasure Map                                                    */
/* ==================================================================== */

@Composable
fun CuratedMapTicker(
    data: TickerData,
    interactions: TickerInteractions?,
    animateIn: Boolean,
    exitFraction: () -> Float
) {
    val context = LocalContext.current
    val opts = Prefs.themeOptions(context, "map", 0xFFA9402E.toInt())
    val accent = if (opts.accentArgb == 0xFFA9402E.toInt()) Color(0xFFA9402E) else Color(opts.accentArgb)
    val lines = opts.lines
    val intensity = opts.intensity
    val showTime = opts.showTime
    val showLogo = opts.showLogo
    val logoSize = opts.avatarSize.dp
    val scale = opts.buttonScale / 100f
    val effect = opts.effect

    val routeE = rememberCardEnter(animateIn, 900, 300)
    val xE = rememberCardEnter(animateIn, 450, 700)
    val lateE = rememberCardEnter(animateIn, 500, 200)
    val textE = rememberCardEnter(animateIn, 700, 550)
    val compassE = rememberCardEnter(animateIn, 1100, 250)
    val btnE = listOf(
        rememberCardEnter(animateIn, 400, 850),
        rememberCardEnter(animateIn, 400, 950)
    )
    val wobble = if (effect) ambientAlternate(6000) else 0.5f
    val dash = if (effect) ambientLoop(2600) else 0f
    val ex = exitFraction()

    CardRoot(
        animateIn = animateIn,
        exitFraction = exitFraction,
        enterDurationMs = 550,
        enter = { t ->
            CardLayer(
                alpha = keyframe(t, 0f to 0f, 0.7f to 1f, 1f to 1f),
                sx = keyframe(t, 0f to 1.07f, 0.7f to 0.99f, 1f to 1f),
                sy = keyframe(t, 0f to 1.07f, 0.7f to 0.99f, 1f to 1f),
                rot = keyframe(t, 0f to -1.6f, 0.7f to 0.4f, 1f to 0f)
            )
        },
        exit = { p ->
            val d = exitDelay(p, 0.18f)
            CardLayer(
                alpha = 1f - d,
                rot = -2.4f * d,
                sx = 1f - 0.12f * d,
                sy = 1f - 0.12f * d,
                ty = 16f * d
            )
        }
    ) {
        CardFrame(
            lines = lines,
            hasDock = data.actions.isNotEmpty(),
            // One line of text plus the dock; every extra line adds 19dp.
            baseHeight = 152.dp,
            dockSpace = 41.dp,
            decorationAlpha = intensity / 100f,
            shape = RoundedCornerShape(6.dp),
            background = Brush.verticalGradient(listOf(Color(0xFFEDDDB0), Color(0xFFE4CFA0)))
        ) {
            Canvas(Modifier.fillMaxSize().graphicsLayer { alpha = intensity / 100f }) {
                drawRect(
                    Brush.radialGradient(
                        listOf(Color(0x2EA07830), Color.Transparent),
                        center = Offset(size.width * 0.18f, size.height * 0.2f),
                        radius = size.width * 0.3f
                    )
                )
                drawRect(
                    Brush.radialGradient(
                        listOf(Color(0x29966932), Color.Transparent),
                        center = Offset(size.width * 0.8f, size.height * 0.68f),
                        radius = size.width * 0.35f
                    )
                )

                // creases
                listOf(0.33f, 0.66f).forEach { fx ->
                    drawRect(
                        Brush.verticalGradient(
                            listOf(Color.Transparent, Color(0x38785523), Color(0x38785523), Color.Transparent)
                        ),
                        Offset(size.width * fx, 0f),
                        Size(1.5.dp.toPx(), size.height)
                    )
                }

                // compass — it swings loose and settles pointing north
                val ccx = size.width - 12.dp.toPx() - 17.dp.toPx()
                val ccy = 10.dp.toPx() + 17.dp.toPx()
                val needle = keyframe(
                    compassE,
                    0f to -42f, 0.22f to 26f, 0.45f to -14f,
                    0.68f to 7f, 0.86f to -3f, 1f to 0f
                ) + (wobble - 0.5f) * 2.4f
                rotate(needle, Offset(ccx, ccy)) {
                    drawCircle(Color(0x66FFF4D6), radius = 17.dp.toPx(), center = Offset(ccx, ccy))
                    drawCircle(
                        Color(0xFF8A6B3E),
                        radius = 17.dp.toPx(),
                        center = Offset(ccx, ccy),
                        style = Stroke(1.5.dp.toPx())
                    )
                    val north = Path().apply {
                        moveTo(ccx, ccy - 13.dp.toPx())
                        lineTo(ccx - 4.dp.toPx(), ccy)
                        lineTo(ccx + 4.dp.toPx(), ccy)
                        close()
                    }
                    drawPath(north, Color(0xFFA9402E))
                    val south = Path().apply {
                        moveTo(ccx, ccy + 13.dp.toPx())
                        lineTo(ccx - 4.dp.toPx(), ccy)
                        lineTo(ccx + 4.dp.toPx(), ccy)
                        close()
                    }
                    drawPath(south, Color(0xFF5C4426))
                }

                // route — it ends exactly on the X
                val xcx = size.width * 0.6791f
                val xcy = size.height * 0.6009f
                val route = Path().apply {
                    moveTo(size.width * 0.0186f, size.height * 0.9423f)
                    cubicTo(
                        size.width * 0.1628f, size.height * 0.8077f,
                        size.width * 0.2233f, size.height * 0.8942f,
                        size.width * 0.3209f, size.height * 0.7212f
                    )
                    cubicTo(
                        size.width * 0.4000f, size.height * 0.5865f,
                        size.width * 0.3488f, size.height * 0.4615f,
                        size.width * 0.4558f, size.height * 0.4423f
                    )
                    cubicTo(
                        size.width * 0.5674f, size.height * 0.4231f,
                        size.width * 0.5767f, size.height * 0.6442f,
                        xcx, xcy
                    )
                }
                drawPath(
                    route,
                    Color(0xFFA9402E),
                    alpha = routeE,
                    style = Stroke(
                        width = 2.5.dp.toPx(),
                        cap = StrokeCap.Round,
                        pathEffect = PathEffect.dashPathEffect(
                            floatArrayOf(7.dp.toPx(), 7.dp.toPx()),
                            -56.dp.toPx() * dash
                        )
                    )
                )

                // X marks the spot — the end of the trail
                rotate(0f) {
                    val s = keyframe(xE, 0f to 2.3f, 0.65f to 0.94f, 1f to 1f)
                    val ra = keyframe(xE, 0f to -24f, 0.65f to 9f, 1f to 0f)
                    rotate(ra, Offset(xcx, xcy)) {
                        rotate(45f, Offset(xcx, xcy)) {
                            drawRoundRect(
                                Color(0xFFA9402E),
                                Offset(xcx - 12.dp.toPx() * s, xcy - 2.25.dp.toPx() * s),
                                Size(24.dp.toPx() * s, 4.5.dp.toPx() * s),
                                CornerRadius(2.dp.toPx()),
                                alpha = xE
                            )
                        }
                        rotate(-45f, Offset(xcx, xcy)) {
                            drawRoundRect(
                                Color(0xFFA9402E),
                                Offset(xcx - 12.dp.toPx() * s, xcy - 2.25.dp.toPx() * s),
                                Size(24.dp.toPx() * s, 4.5.dp.toPx() * s),
                                CornerRadius(2.dp.toPx()),
                                alpha = xE
                            )
                        }
                    }
                }

                // ship — it sits just above the dock so it stays in view
                val shipX = 20.dp.toPx()
                val shipY = size.height - 46.dp.toPx() - 22.dp.toPx()
                drawPath(
                    Path().apply {
                        moveTo(shipX + 4.dp.toPx(), shipY + 14.dp.toPx())
                        quadraticBezierTo(
                            shipX + 17.dp.toPx(), shipY + 19.dp.toPx(),
                            shipX + 30.dp.toPx(), shipY + 14.dp.toPx()
                        )
                        lineTo(shipX + 26.dp.toPx(), shipY + 20.dp.toPx())
                        lineTo(shipX + 8.dp.toPx(), shipY + 20.dp.toPx())
                        close()
                    },
                    Color(0xFF5C4426),
                    alpha = 0.85f * lateE
                )
                drawRect(
                    Color(0xFF5C4426),
                    Offset(shipX + 15.dp.toPx(), shipY + 3.dp.toPx()),
                    Size(2.dp.toPx(), 11.dp.toPx()),
                    alpha = 0.85f * lateE
                )
                drawPath(
                    Path().apply {
                        moveTo(shipX + 17.dp.toPx(), shipY + 4.dp.toPx())
                        lineTo(shipX + 26.dp.toPx(), shipY + 8.dp.toPx())
                        lineTo(shipX + 17.dp.toPx(), shipY + 12.dp.toPx())
                        close()
                    },
                    Color(0xFF8A6B3E),
                    alpha = 0.85f * lateE
                )

                // burn + ash
                if (ex > 0f) {
                    val burn = keyframe(ex, 0f to 0f, 0.25f to 1f, 1f to 1f)
                    val scale = 0.2f + 3.2f * burn
                    drawCircle(
                        Brush.radialGradient(
                            listOf(
                                Color(0xF70F0602), Color(0xE03C1405),
                                Color(0x8C78320A), Color.Transparent
                            ),
                            center = Offset(size.width * 0.08f, size.height * 0.92f),
                            radius = size.width * 0.5f * scale
                        ),
                        radius = size.width * 0.5f * scale,
                        center = Offset(size.width * 0.08f, size.height * 0.92f)
                    )
                    val ashP = exitDelay(ex, 0.30f)
                    val ashes = listOf(
                        20f to 20f, 34f to 34f, 48f to 16f,
                        62f to 30f, 74f to 22f, 44f to 44f
                    )
                    ashes.forEach { (ax, ay) ->
                        drawCircle(
                            Color(0xFF8A8178),
                            radius = 2.5.dp.toPx(),
                            center = Offset(
                                ax.dp.toPx() + 10.dp.toPx() * ashP,
                                size.height - ay.dp.toPx() - 64.dp.toPx() * ashP
                            ),
                            alpha = (if (ashP < 0.25f) ashP / 0.25f * 0.9f else (1f - ashP)).coerceIn(0f, 1f)
                        )
                    }
                }
            }

            Column(
                Modifier
                    .align(Alignment.TopStart)
                    .padding(start = 20.dp, top = 18.dp)
                    .fillMaxWidth()
                    .cardLayer(
                        if (ex > 0f) CardLayer(alpha = 1f - exitDelay(ex, 0.15f))
                        else CardLayer(alpha = textE)
                    )
            ) {
                CardSenderRow(
                    title = data.title,
                    showTime = showTime,
                    showLogo = showLogo,
                    logoSize = logoSize,
                    logoData = data,
                    time = data.time,
                    titleColor = Color(0xFF4A331C),
                    timeColor = accent,
                    titleFont = ThemeFonts.IMFell,
                    timeFont = ThemeFonts.IMFell,
                    titleSize = 25f,
                    timeSize = 12.5f,
                    titleWeight = FontWeight.Normal,
                    modifier = Modifier.padding(end = 58.dp)
                )
                Spacer(Modifier.height(6.dp))
                CardBody(
                    text = data.body,
                    lines = lines,
                    color = Color(0xFF5C4426),
                    font = ThemeFonts.IMFell,
                    weight = FontWeight.Normal,
                    sizeSp = 14.5f,
                    lineHeightFactor = 1.28f,
                    modifier = Modifier.padding(end = 116.dp)
                )
            }

            CardDock(
                data = data,
                interactions = interactions,
                modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 11.dp, start = 20.dp, end = 20.dp),
                background = Color(0xFFF2E4C0),
                foreground = Color(0xFF6B4E26),
                border = Color(0xFFA8834F),
                borderWidth = 1.5.dp,
                shape = RoundedCornerShape(4.dp),
                font = ThemeFonts.IMFell,
                fontSize = 10.5f,
                letterSpacing = 1f,
                uppercase = true,
                showIcons = true,
                iconSize = 11.dp,
                height = 30.dp,
                horizontalPadding = 6.dp,
                verticalPadding = 5.dp,
                gap = 8.dp,
                buttonScale = scale,
                borderFor = { Color(0xFFA8834F) },
                foregroundFor = { Color(0xFF6B4E26) },
                backgroundFor = { Brush.verticalGradient(listOf(Color(0xFFF2E4C0), Color(0xFFE7D6AC))) }
            )
        }
    }
}
