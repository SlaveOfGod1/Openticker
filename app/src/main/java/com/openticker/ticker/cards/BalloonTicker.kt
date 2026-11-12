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
/* 19 — Balloon Party                                                   */
/* ==================================================================== */

@Composable
fun CuratedBalloonTicker(
    data: TickerData,
    interactions: TickerInteractions?,
    animateIn: Boolean,
    exitFraction: () -> Float
) {
    val context = LocalContext.current
    val opts = Prefs.themeOptions(context, "balloon", 0xFFE8788F.toInt())
    val accent = if (opts.accentArgb == 0xFFE8788F.toInt()) Color(0xFFE8788F) else Color(opts.accentArgb)
    val lines = opts.lines
    val intensity = opts.intensity
    val showTime = opts.showTime
    val showLogo = opts.showLogo
    val logoSize = opts.avatarSize.dp
    val scale = opts.buttonScale / 100f
    val effect = opts.effect

    val b1E = rememberCardEnter(animateIn, 600, 250)
    val b2E = rememberCardEnter(animateIn, 600, 380)
    val b3E = rememberCardEnter(animateIn, 600, 500)
    val textE = rememberCardEnter(animateIn, 500, 450)
    val btnE = listOf(
        rememberCardEnter(animateIn, 500, 620),
        rememberCardEnter(animateIn, 500, 720)
    )
    val sway = if (effect) ambientAlternate(5000) else 0.5f
    val bob1 = if (effect) ambientAlternate(4500) else 0f
    val bob2 = if (effect) ambientAlternate(4500) else 0.34f
    val bob3 = if (effect) ambientAlternate(4500) else 0.68f
    val conf = (0 until 6).map { i -> if (effect) ambientLoop(7000, i * 0.143f) else 0f }
    val ex = exitFraction()

    CardRoot(
        animateIn = animateIn,
        exitFraction = exitFraction,
        enterDurationMs = 550,
        enter = { t ->
            CardLayer(
                alpha = keyframe(t, 0f to 0f, 0.65f to 1f, 1f to 1f),
                sx = keyframe(t, 0f to 0.72f, 0.65f to 1.05f, 1f to 1f),
                sy = keyframe(t, 0f to 0.72f, 0.65f to 1.05f, 1f to 1f)
            )
        },
        exit = { p ->
            val d = exitDelay(p, 0.10f)
            CardLayer(
                alpha = 1f - d,
                ty = keyframe(d, 0f to 0f, 0.35f to 2f, 1f to 64f),
                rot = keyframe(d, 0f to 0f, 1f to 3f),
                sx = keyframe(d, 0f to 1f, 0.35f to 1.06f, 1f to 0.85f),
                sy = keyframe(d, 0f to 1f, 0.35f to 0.90f, 1f to 0.90f)
            )
        }
    ) {
        CardFrame(
            lines = lines,
            hasDock = data.actions.isNotEmpty(),
            // The note and the buttons sit together at the top of the card and
            // the balloons fill the space beside them, so the card is snug.
            baseHeight = 136.dp,
            dockSpace = 42.dp,
            decorationAlpha = intensity / 100f,
            shape = RoundedCornerShape(18.dp),
            background = Brush.verticalGradient(
                0f to Color(0xFFFFF3F8), 0.48f to Color(0xFFFFE4EE), 1f to Color(0xFFE9F5FF)
            )
        ) {
            Canvas(Modifier.fillMaxSize().graphicsLayer { alpha = intensity / 100f }) {
                // bunting
                val flagH = 13.dp.toPx()
                rotate(-0.8f + 1.6f * sway, Offset(size.width / 2f, 0f)) {
                    val stripes = listOf(
                        Color(0xFFFF9EC0), Color(0xFF9ED9FF), Color(0xFFFFE08A)
                    )
                    val period = 78.dp.toPx()
                    var x = -period
                    var i = 0
                    while (x < size.width + period) {
                        val c = stripes[i % 3]
                        val path = Path().apply {
                            moveTo(x, 0f)
                            lineTo(x + 26.dp.toPx(), 0f)
                            lineTo(x + 26.dp.toPx(), flagH)
                            lineTo(x + 39.dp.toPx(), 0f)
                            lineTo(x + 52.dp.toPx(), flagH)
                            lineTo(x + 65.dp.toPx(), 0f)
                            lineTo(x + 78.dp.toPx(), 0f)
                            lineTo(x + 78.dp.toPx(), 0f)
                            lineTo(x, 0f)
                            close()
                        }
                        drawPath(path, c)
                        x += period
                        i++
                    }
                }

                // balloons
                fun balloon(
                    right: Float, top: Float, w: Float, h: Float,
                    body: Color, dark: Color, bob: Float, inflate: Float
                ) {
                    val left = size.width - right.dp.toPx() - w.dp.toPx()
                    val topY = top.dp.toPx()
                    val sx = keyframe(inflate, 0f to 0.05f, 0.6f to 1.15f, 0.8f to 0.94f, 1f to 1f)
                    val sy = keyframe(inflate, 0f to 0.1f, 0.6f to 0.92f, 0.8f to 1.06f, 1f to 1f)
                    val dx = 2.dp.toPx() * sin(bob * 2f * Math.PI.toFloat())
                    val dy = 3.dp.toPx() - 7.dp.toPx() * bob
                    val cx = left + w.dp.toPx() / 2f + dx
                    val cy = topY + h.dp.toPx() / 2f + dy
                    rotate(-2f + 4f * bob, Offset(cx, cy)) {
                        drawOval(
                            Brush.radialGradient(
                                listOf(Color(0xD9FFFFFF), body, dark),
                                center = Offset(cx - w.dp.toPx() * 0.16f, cy - h.dp.toPx() * 0.24f),
                                radius = w.dp.toPx() * 0.7f
                            ),
                            topLeft = Offset(cx - w.dp.toPx() * 0.5f * sx, cy - h.dp.toPx() * 0.5f * sy),
                            size = Size(w.dp.toPx() * sx, h.dp.toPx() * sy),
                            alpha = inflate
                        )
                        drawLine(
                            Color(0x597A5A64),
                            Offset(cx, cy + h.dp.toPx() * 0.5f),
                            Offset(cx, cy + h.dp.toPx() * 0.5f + 12.dp.toPx()),
                            strokeWidth = 1.5.dp.toPx(),
                            alpha = inflate
                        )
                    }
                }
                val pop = exitDelay(ex, 0f)
                val popScale = keyframe(pop, 0f to 1f, 0.55f to 1.3f, 1f to 0f)
                val popAlpha = keyframe(pop, 0f to 1f, 0.55f to 1f, 1f to 0f)
                balloon(26f, 14f, 46f, 54f, Color(0xFFFF9EC0), Color(0xFFE8689A), bob1, if (ex > 0f) popAlpha * popScale else b1E)
                balloon(18f, 34f, 38f, 46f, Color(0xFF9ED9FF), Color(0xFF5EB4E0), bob2, if (ex > 0f) popAlpha * popScale else b2E)
                balloon(86f, 48f, 30f, 37f, Color(0xFFFFE08A), Color(0xFFE8BC4E), bob3, if (ex > 0f) popAlpha * popScale else b3E)

                // confetti
                if (effect) {
                    val cols = listOf(
                        Color(0xFFFF9EC0), Color(0xFF9ED9FF), Color(0xFFFFE08A),
                        Color(0xFFB79BE8), Color(0xFF7ADBB5), Color(0xFFFF9EC0)
                    )
                    val xs = listOf(0.12f, 0.30f, 0.47f, 0.64f, 0.82f, 0.90f)
                    conf.forEachIndexed { i, p ->
                        val a = when {
                            p < 0.1f -> p / 0.1f
                            else -> 1f - (p - 0.1f) / 0.9f
                        }
                        val y = -8.dp.toPx() + 200.dp.toPx() * p
                        rotate(320f * p, Offset(size.width * xs[i], y)) {
                            drawRoundRect(
                                cols[i],
                                topLeft = Offset(size.width * xs[i], y),
                                size = Size(7.dp.toPx(), 4.dp.toPx()),
                                cornerRadius = CornerRadius(1.dp.toPx()),
                                alpha = a.coerceIn(0f, 1f)
                            )
                        }
                    }
                }

                // burst confetti on enter and exit
                val burst = if (ex > 0f) exitDelay(ex, 0.12f) else 0f
                if (burst > 0f) {
                    val cols = listOf(
                        Color(0xFFFF9EC0), Color(0xFF9ED9FF), Color(0xFFFFE08A),
                        Color(0xFFB79BE8), Color(0xFF7ADBB5), Color(0xFFFF9EC0),
                        Color(0xFFFFE08A), Color(0xFFB79BE8), Color(0xFF7ADBB5), Color(0xFFFF9EC0)
                    )
                    val positions = listOf(
                        0.30f to 0.40f, 0.40f to 0.52f, 0.50f to 0.38f, 0.58f to 0.50f,
                        0.36f to 0.46f, 0.54f to 0.44f, 0.44f to 0.44f, 0.34f to 0.42f,
                        0.62f to 0.56f, 0.48f to 0.40f
                    )
                    val deltas = listOf(
                        -60f to -50f, -30f to 44f, 16f to -62f, 58f to 36f,
                        -14f to -24f, 52f to -28f, -4f to 56f, -66f to -34f,
                        40f to 50f, 8f to -66f
                    )
                    positions.forEachIndexed { i, (fx, fy) ->
                        val dx = deltas[i].first.dp.toPx() * burst
                        val dy = deltas[i].second.dp.toPx() * burst
                        rotate(240f * burst, Offset(size.width * fx + dx, size.height * fy + dy)) {
                            drawRoundRect(
                                cols[i],
                                topLeft = Offset(size.width * fx + dx, size.height * fy + dy),
                                size = Size(9.dp.toPx(), 6.dp.toPx()),
                                cornerRadius = CornerRadius(2.dp.toPx()),
                                alpha = 1f - burst
                            )
                        }
                    }
                }
            }

            // The note and the buttons travel together, so there is never a
            // gap between the message and the dock.
            Column(
                Modifier
                    .align(Alignment.TopStart)
                    .padding(start = 20.dp, end = 20.dp, top = 20.dp)
                    .fillMaxWidth()
            ) {
                Column(
                    Modifier
                        .fillMaxWidth()
                        .padding(end = 66.dp)
                        .cardLayer(
                            if (ex > 0f) CardLayer(alpha = 1f - exitDelay(ex, 0.10f))
                            else CardLayer(alpha = textE, ty = 10f * (1f - textE), rot = -2f * (1f - textE))
                        )
                ) {
                    CardSenderRow(
                        title = data.title,
                        showTime = showTime,
                        showLogo = showLogo,
                        logoSize = logoSize,
                        logoData = data,
                        time = data.time,
                        titleColor = Color(0xFFD6336C),
                        timeColor = accent,
                        titleFont = ThemeFonts.BalooBlack,
                        timeFont = ThemeFonts.Nunito,
                        titleSize = 18f,
                        titleWeight = FontWeight.ExtraBold
                    )
                    Spacer(Modifier.height(5.dp))
                    CardBody(
                        text = data.body,
                        lines = lines,
                        color = Color(0xFF7A5C68),
                        font = ThemeFonts.Nunito,
                        weight = FontWeight.Bold,
                        sizeSp = 11.5f,
                        lineHeightFactor = 1.3f
                    )
                }

                Spacer(Modifier.height(12.dp))

                CardDock(
                    data = data,
                    interactions = interactions,
                    modifier = Modifier
                        .fillMaxWidth()
                        .cardLayer(
                            if (ex > 0f) CardLayer(alpha = 1f - exitDelay(ex, 0.10f))
                            else CardLayer(alpha = btnE[0], ty = -8f * (1f - btnE[0]))
                        ),
                    background = Color(0xFFFF7BAC),
                    foreground = Color.White,
                    border = null,
                    shape = RoundedCornerShape(50),
                    font = ThemeFonts.Nunito,
                    fontSize = 10.5f,
                    letterSpacing = 0.7f,
                    uppercase = true,
                    height = 30.dp,
                    horizontalPadding = 6.dp,
                    verticalPadding = 5.dp,
                    gap = 8.dp,
                    buttonScale = scale,
                    borderFor = { null },
                    foregroundFor = { shell -> if (shell) Color(0xFF1E5C46) else Color.White },
                    backgroundFor = { shell ->
                        if (shell) Brush.verticalGradient(listOf(Color(0xFF7ADBB5), Color(0xFF68CAA4)))
                        else Brush.verticalGradient(listOf(Color(0xFFFF7BAC), Color(0xFFF06A9B)))
                    }
                )
            }
        }
    }
}
