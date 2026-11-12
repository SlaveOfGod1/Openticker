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
/* 03 — Vinyl Turntable                                                 */
/* ==================================================================== */

@Composable
fun CuratedVinylTicker(
    data: TickerData,
    interactions: TickerInteractions?,
    animateIn: Boolean,
    exitFraction: () -> Float
) {
    val context = LocalContext.current
    val opts = Prefs.themeOptions(context, "vinyl", 0xFFFF8A5C.toInt())
    val accent = if (opts.accentArgb == 0xFFFF8A5C.toInt()) Color(0xFFFF8A5C) else Color(opts.accentArgb)
    val lines = opts.lines
    val intensity = opts.intensity
    val showTime = opts.showTime
    val showLogo = opts.showLogo
    val logoSize = opts.avatarSize.dp
    val scale = opts.buttonScale / 100f
    val effect = opts.effect

    val recE = rememberCardEnter(animateIn, 600, 250)
    val armE = rememberCardEnter(animateIn, 800, 500)
    val textE = rememberCardEnter(animateIn, 500, 600)
    val btnE = listOf(
        rememberCardEnter(animateIn, 450, 750),
        rememberCardEnter(animateIn, 450, 850)
    )
    val spin = if (effect) ambientLoop(5000) else 0f
    val ledP = if (effect) ambientAlternate(2200) else 1f
    val ex = exitFraction()

    CardRoot(
        animateIn = animateIn,
        exitFraction = exitFraction,
        enterDurationMs = 600,
        enter = { t -> CardLayer(alpha = t, ty = 26f * (1f - t), sx = 0.96f + 0.04f * t, sy = 0.96f + 0.04f * t) },
        exit = { p ->
            val d = exitDelay(p, 0.35f)
            CardLayer(alpha = 1f - d, ty = 16f * d)
        }
    ) {
        CardFrame(
            lines = lines,
            hasDock = data.actions.isNotEmpty(),
            // The record sits directly above the dock.
            baseHeight = 170.dp,
            dockSpace = 40.dp,
            decorationAlpha = intensity / 100f,
            shape = RoundedCornerShape(18.dp),
            background = Brush.linearGradient(
                listOf(Color(0xFF5A3A28), Color(0xFF3E2417), Color(0xFF4A2C1C)),
                start = Offset.Zero,
                end = Offset(1200f, 900f)
            )
        ) {
            Canvas(Modifier.fillMaxSize().graphicsLayer { alpha = intensity / 100f }) {
                var x = 0f
                while (x < size.width) {
                    drawRect(Color(0x17000000), Offset(x, 0f), Size(2.dp.toPx(), size.height))
                    x += 9.dp.toPx()
                }
                drawRect(Color(0x24FFEBC8), Offset.Zero, Size(size.width, 2.dp.toPx()))
                drawRect(
                    Brush.verticalGradient(
                        listOf(Color.Transparent, Color(0x99000000)),
                        startY = size.height - 34.dp.toPx(),
                        endY = size.height
                    ),
                    Offset(0f, size.height - 34.dp.toPx()),
                    Size(size.width, 34.dp.toPx())
                )
            }

            // spinning record
            Box(
                Modifier
                    .align(Alignment.TopEnd)
                    .padding(end = 14.dp, top = 26.dp)
                    .size(104.dp)
                    .cardLayer(
                        if (ex > 0f) {
                            val d = exitDelay(ex, 0.15f)
                            CardLayer(alpha = 1f - d, tx = 95f * d, sx = 1f - 0.1f * d, sy = 1f - 0.1f * d)
                        } else {
                            CardLayer(
                                alpha = recE,
                                tx = keyframe(recE, 0f to 70f, 0.7f to -4f, 1f to 0f),
                                sx = keyframe(recE, 0f to 0.9f, 0.7f to 1.02f, 1f to 1f),
                                sy = keyframe(recE, 0f to 0.9f, 0.7f to 1.02f, 1f to 1f)
                            )
                        }
                    )
            ) {
                Canvas(Modifier.fillMaxSize().graphicsLayer { rotationZ = 360f * spin }) {
                    val r = size.minDimension / 2f
                    drawCircle(Color(0xFF121212), radius = r)
                    var rr = r
                    var i = 0
                    while (rr > r * 0.42f) {
                        drawCircle(
                            if (i % 2 == 0) Color(0xFF1F1F1F) else Color(0xFF121212),
                            radius = rr,
                            style = Stroke(1.4.dp.toPx())
                        )
                        rr -= 1.6.dp.toPx()
                        i++
                    }
                    rotate(210f) {
                        drawArc(
                            Color(0x22FFFFFF), 0f, 44f, false,
                            topLeft = Offset.Zero, size = size,
                            style = Stroke(7.dp.toPx())
                        )
                    }
                    drawCircle(
                        Brush.radialGradient(
                            listOf(Color(0xFFFF8A6B), Color(0xFFE2593F)),
                            center = Offset(size.width * 0.4f, size.height * 0.35f),
                            radius = r * 0.5f
                        ),
                        radius = r * 0.33f
                    )
                    drawCircle(Color(0xFF24140C), radius = 2.5.dp.toPx())
                }
            }

            // tonearm
            Box(
                Modifier
                    .align(Alignment.TopEnd)
                    .padding(end = 16.dp, top = 8.dp)
                    .size(width = 86.dp, height = 9.dp)
                    .graphicsLayer {
                        val rot = if (ex > 0f) {
                            keyframe(ex, 0f to -33f, 1f to -2f)
                        } else {
                            keyframe(armE, 0f to -6f, 0.7f to -37f, 1f to -33f)
                        }
                        rotationZ = rot
                        transformOrigin = TransformOrigin(0.907f, 0.5f)
                    }
            ) {
                Box(
                    Modifier
                        .align(Alignment.Center)
                        .fillMaxWidth()
                        .height(9.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(Brush.verticalGradient(listOf(Color(0xFFE2DCCE), Color(0xFF9E9689))))
                )
                Box(
                    Modifier
                        .align(Alignment.CenterStart)
                        .offset(x = (-4).dp, y = (-1).dp)
                        .size(width = 15.dp, height = 11.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(Brush.verticalGradient(listOf(Color(0xFF3A3230), Color(0xFF191412))))
                )
                Box(
                    Modifier
                        .align(Alignment.CenterEnd)
                        .offset(x = 5.dp, y = (-4).dp)
                        .size(17.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.radialGradient(
                                listOf(Color(0xFFF2EDE2), Color(0xFF8D857A)),
                                center = Offset(6f, 5f),
                                radius = 14f
                            )
                        )
                )
            }

            Box(
                Modifier
                    .align(Alignment.BottomStart)
                    .padding(start = 16.dp, bottom = 14.dp)
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFFF4B3E).copy(alpha = 0.5f + 0.5f * ledP))
            )
            Text(
                "33 RPM",
                color = Color(0xFFC9B394),
                fontSize = 7.sp,
                fontFamily = ThemeFonts.Oswald,
                letterSpacing = 1.8.sp,
                modifier = Modifier.align(Alignment.BottomStart).padding(start = 32.dp, bottom = 11.dp)
            )

            Column(
                Modifier
                    .align(Alignment.TopStart)
                    .padding(start = 20.dp, end = 112.dp, top = 16.dp)
                    .fillMaxWidth()
                    .cardLayer(
                        if (ex > 0f) {
                            val d = exitDelay(ex, 0f)
                            CardLayer(alpha = 1f - d, ty = 10f * d)
                        } else CardLayer(alpha = textE, ty = 12f * (1f - textE))
                    )
            ) {
                CardSenderRow(
                    title = data.title,
                    showTime = showTime,
                    showLogo = showLogo,
                    logoSize = logoSize,
                    logoData = data,
                    time = data.time,
                    titleColor = Color(0xFFFFF6E8),
                    timeColor = accent,
                    titleFont = ThemeFonts.Bebas,
                    timeFont = ThemeFonts.Nunito,
                    titleSize = 21f,
                    titleLetter = 1.6f,
                    titleWeight = FontWeight.Normal,
                    titleStyle = TextStyle(shadow = Shadow(Color(0x99140802), Offset(0f, 2f), 6f))
                )
                Spacer(Modifier.height(4.dp))
                CardBody(
                    text = data.body,
                    lines = lines,
                    color = Color(0xFFEBCFAF),
                    font = ThemeFonts.Nunito,
                    weight = FontWeight.Bold,
                    sizeSp = 11.5f,
                    lineHeightFactor = 1.3f
                )
            }

            CardDock(
                data = data,
                interactions = interactions,
                modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 10.dp, start = 90.dp, end = 14.dp),
                background = Color(0xFF241610),
                foreground = Color(0xFFF3E4CE),
                border = Color(0xFF7A5230),
                shape = RoundedCornerShape(50),
                font = ThemeFonts.Nunito,
                fontSize = 10.5f,
                letterSpacing = 0.7f,
                uppercase = true,
                height = 30.dp,
                horizontalPadding = 5.dp,
                verticalPadding = 5.dp,
                gap = 6.dp,
                buttonScale = scale,
                borderFor = { shell -> if (shell) Color(0xFFA03A24) else Color(0xFF7A5230) },
                foregroundFor = { shell -> if (shell) Color(0xFFFFF3E8) else Color(0xFFF3E4CE) },
                backgroundFor = { shell ->
                    if (shell) Brush.verticalGradient(listOf(Color(0xFFE8644A), Color(0xFFC74B33)))
                    else Brush.verticalGradient(listOf(Color(0xFF241610), Color(0xFF1B0F0A)))
                }
            )
        }
    }
}
