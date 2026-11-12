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
/* 20 — Carnival Marquee                                                */
/* ==================================================================== */

@Composable
fun CuratedMarqueeTicker(
    data: TickerData,
    interactions: TickerInteractions?,
    animateIn: Boolean,
    exitFraction: () -> Float
) {
    val context = LocalContext.current
    val opts = Prefs.themeOptions(context, "marquee", 0xFFE8BC4E.toInt())
    val accent = if (opts.accentArgb == 0xFFE8BC4E.toInt()) Color(0xFFE8BC4E) else Color(opts.accentArgb)
    val lines = opts.lines
    val intensity = opts.intensity
    val showTime = opts.showTime
    val showLogo = opts.showLogo
    val logoSize = opts.avatarSize.dp
    val scale = opts.buttonScale / 100f
    val effect = opts.effect

    val bulbE = rememberCardEnter(animateIn, 700, 120)
    val curtainE = rememberCardEnter(animateIn, 800, 180)
    val textE = rememberCardEnter(animateIn, 700, 700)
    val btnE = listOf(
        rememberCardEnter(animateIn, 450, 950),
        rememberCardEnter(animateIn, 450, 1050)
    )
    val chase = if (effect) ambientLoop(900) else 0f
    val beam = if (effect) ambientAlternate(8000) else 0.5f
    val ex = exitFraction()

    CardRoot(
        animateIn = animateIn,
        exitFraction = exitFraction,
        enterDurationMs = 450,
        enter = { t -> CardLayer(alpha = t) },
        exit = { p ->
            val d = exitDelay(p, 0.40f)
            CardLayer(alpha = 1f - d, ty = 10f * d)
        }
    ) {
        CardFrame(
            lines = lines,
            hasDock = data.actions.isNotEmpty(),
            baseHeight = 128.dp,
            dockSpace = 42.dp,
            decorationAlpha = intensity / 100f,
            shape = RoundedCornerShape(14.dp),
            background = Brush.verticalGradient(
                0f to Color(0xFF7E1420), 0.60f to Color(0xFF5E0E18), 1f to Color(0xFF470A12)
            ),
            border = Color(0xFFC9A227),
            borderWidth = 3.dp
        ) {
            Canvas(Modifier.fillMaxSize().graphicsLayer { alpha = intensity / 100f }) {
                // velvet ribs
                var x = 0f
                while (x < size.width) {
                    drawRect(Color(0x42000000), Offset(x, 0f), Size(10.dp.toPx(), size.height))
                    drawRect(Color(0x0BFFFFFF), Offset(x + 10.dp.toPx(), 0f), Size(5.dp.toPx(), size.height))
                    x += 28.dp.toPx()
                }

                // spotlight
                rotate(-9f + 18f * beam, Offset(size.width / 2f, 0f)) {
                    val cone = Path().apply {
                        moveTo(size.width / 2f - 14.dp.toPx(), 0f)
                        lineTo(size.width / 2f + 14.dp.toPx(), 0f)
                        lineTo(size.width * 0.86f, size.height)
                        lineTo(size.width * 0.14f, size.height)
                        close()
                    }
                    drawPath(
                        cone,
                        Brush.verticalGradient(
                            listOf(Color(0x1AFFEBB4), Color(0x08FFEBB4), Color.Transparent)
                        )
                    )
                }

                // bulb strips
                val bulbCore = Color(0xFFFFE9A8)
                val bulbRim = Color(0xF28C6419)
                fun bulb(cx: Float, cy: Float, alpha: Float) {
                    drawCircle(bulbRim, radius = 3.4.dp.toPx(), center = Offset(cx, cy), alpha = alpha)
                    drawCircle(bulbCore, radius = 2.4.dp.toPx(), center = Offset(cx, cy), alpha = alpha)
                }
                val step = 19.dp.toPx()
                val bulbIn = keyframe(bulbE, 0f to 0f, 0.25f to 1f, 0.35f to 0.15f, 0.45f to 1f, 0.55f to 0.3f, 0.65f to 1f, 1f to 1f)
                val bulbOut = if (ex > 0f) {
                    keyframe(ex, 0f to 1f, 0.3f to 0.2f, 0.45f to 1f, 0.6f to 0.15f, 0.75f to 0.6f, 1f to 0f)
                } else 1f
                val topY = 5.dp.toPx() + 4.5.dp.toPx()
                val bottomY = size.height - 5.dp.toPx() - 4.5.dp.toPx()
                var bx = 12.dp.toPx()
                var i = 0
                while (bx < size.width - 12.dp.toPx()) {
                    val chaseA = 0.35f + 0.65f * ((sin((i - chase * 10f) * 1.2f) + 1f) / 2f)
                    bulb(bx, topY, bulbIn * bulbOut * chaseA)
                    val chaseB = 0.35f + 0.65f * ((sin((i + chase * 10f) * 1.2f) + 1f) / 2f)
                    bulb(bx, bottomY, bulbIn * bulbOut * chaseB)
                    bx += step
                    i++
                }
                val leftX = 5.dp.toPx() + 4.5.dp.toPx()
                val rightX = size.width - 5.dp.toPx() - 4.5.dp.toPx()
                var by = 12.dp.toPx()
                var j = 0
                while (by < size.height - 12.dp.toPx()) {
                    val chaseA = 0.35f + 0.65f * ((sin((j - chase * 10f) * 1.2f) + 1f) / 2f)
                    bulb(leftX, by, bulbIn * bulbOut * chaseA)
                    val chaseB = 0.35f + 0.65f * ((sin((j + chase * 10f) * 1.2f) + 1f) / 2f)
                    bulb(rightX, by, bulbIn * bulbOut * chaseB)
                    by += step
                    j++
                }
            }

            // the clock, hung in the top right corner
            if (showTime) {
                Text(
                    data.time.uppercase(),
                    color = Color(0xFFFFE9A8),
                    fontSize = 10.sp,
                    fontFamily = ThemeFonts.Nunito,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 1.6.sp,
                    style = TextStyle(shadow = Shadow(Color(0xB3FFDC82), Offset.Zero, 12f)),
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(top = 20.dp, end = 22.dp)
                        .graphicsLayer { alpha = textE }
                )
            }

            Column(
                Modifier
                    .align(Alignment.TopStart)
                    .padding(start = 20.dp, end = 20.dp, top = 16.dp)
                    .fillMaxWidth()
                    .cardLayer(
                        if (ex > 0f) CardLayer(alpha = 1f - ex)
                        else CardLayer(alpha = textE)
                    )
            ) {
                CardSenderRow(
                    title = data.title,
                    showTime = false,
                    showLogo = showLogo,
                    logoSize = logoSize,
                    logoData = data,
                    time = data.time,
                    titleColor = Color(0xFFF8E3A8),
                    timeColor = accent,
                    titleFont = ThemeFonts.Yeseva,
                    timeFont = ThemeFonts.Nunito,
                    titleSize = 17f,
                    titleLetter = 0.3f + 2.7f * (1f - textE),
                    titleWeight = FontWeight.Normal,
                    titleStyle = TextStyle(shadow = Shadow(Color(0x8CE8BC4E), Offset.Zero, 14f))
                )
                Spacer(Modifier.height(5.dp))
                CardBody(
                    text = data.body,
                    lines = lines,
                    color = Color(0xFFE8C9C0),
                    font = ThemeFonts.Nunito,
                    weight = FontWeight.Bold,
                    sizeSp = 11.5f,
                    lineHeightFactor = 1.3f
                )
            }

            CardDock(
                data = data,
                interactions = interactions,
                modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 12.dp, start = 20.dp, end = 20.dp),
                background = Color(0xFFB8903A),
                foreground = Color(0xFF4A2E08),
                border = Color(0xFF8F6E22),
                borderWidth = 1.5.dp,
                shape = RoundedCornerShape(8.dp),
                font = ThemeFonts.Nunito,
                fontSize = 9f,
                letterSpacing = 1f,
                uppercase = true,
                height = 30.dp,
                horizontalPadding = 6.dp,
                verticalPadding = 5.dp,
                gap = 8.dp,
                buttonScale = scale,
                borderFor = { shell -> if (shell) Color(0x38FFFFFF) else Color(0xFF8F6E22) },
                foregroundFor = { shell -> if (shell) Color(0xFFF2F7FF) else Color(0xFF4A2E08) },
                backgroundFor = { shell ->
                    if (shell) Brush.verticalGradient(listOf(Color(0x17FFFFFF), Color(0x17FFFFFF)))
                    else Brush.verticalGradient(listOf(Color(0xFFE8C877), Color(0xFFB8903A)))
                }
            )

            // curtains
            val open = if (ex > 0f) {
                1f - keyframe(ex, 0f to 0f, 1f to 1f)
            } else {
                keyframe(curtainE, 0f to 0f, 1f to 1f)
            }
            if (open < 0.99f) {
                val curtainBrush = Brush.verticalGradient(
                    0f to Color(0xFF8C1622), 0.8f to Color(0xFF560C15), 1f to Color(0xFF560C15)
                )
                Box(
                    Modifier
                        .fillMaxSize()
                        .graphicsLayer { translationX = -size.width * 1.06f * open }
                ) {
                    Box(
                        Modifier
                            .fillMaxHeight(1f)
                            .fillMaxWidth(0.54f)
                            .clip(RoundedCornerShape(11.dp, 0.dp, 0.dp, 11.dp))
                            .background(curtainBrush)
                    ) {
                        Canvas(Modifier.fillMaxSize().graphicsLayer { alpha = intensity / 100f }) {
                            var cx = 0f
                            while (cx < size.width) {
                                drawRect(Color(0x52000000), Offset(cx, 0f), Size(7.dp.toPx(), size.height))
                                drawRect(Color(0x0DFFFFFF), Offset(cx + 7.dp.toPx(), 0f), Size(3.dp.toPx(), size.height))
                                cx += 20.dp.toPx()
                            }
                            drawRect(Color(0xFFC9A227), Offset(size.width - 2.dp.toPx(), 0f), Size(2.dp.toPx(), size.height))
                        }
                    }
                }
                Box(
                    Modifier
                        .fillMaxSize()
                        .graphicsLayer { translationX = size.width * 1.06f * open }
                ) {
                    Box(
                        Modifier
                            .fillMaxHeight(1f)
                            .fillMaxWidth(0.54f)
                            .align(Alignment.CenterEnd)
                            .clip(RoundedCornerShape(0.dp, 11.dp, 11.dp, 0.dp))
                            .background(curtainBrush)
                    ) {
                        Canvas(Modifier.fillMaxSize().graphicsLayer { alpha = intensity / 100f }) {
                            var cx = 0f
                            while (cx < size.width) {
                                drawRect(Color(0x52000000), Offset(cx, 0f), Size(7.dp.toPx(), size.height))
                                drawRect(Color(0x0DFFFFFF), Offset(cx + 7.dp.toPx(), 0f), Size(3.dp.toPx(), size.height))
                                cx += 20.dp.toPx()
                            }
                            drawRect(Color(0xFFC9A227), Offset(0f, 0f), Size(2.dp.toPx(), size.height))
                        }
                    }
                }
            }
        }
    }
}
