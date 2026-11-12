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
/* 05 — Corkboard Sticky                                                */
/* ==================================================================== */

@Composable
fun CuratedStickyTicker(
    data: TickerData,
    interactions: TickerInteractions?,
    animateIn: Boolean,
    exitFraction: () -> Float
) {
    val context = LocalContext.current
    val opts = Prefs.themeOptions(context, "sticky", 0xFFC0392B.toInt())
    val accent = if (opts.accentArgb == 0xFFC0392B.toInt()) Color(0xFFC0392B) else Color(opts.accentArgb)
    val lines = opts.lines
    val intensity = opts.intensity
    val showTime = opts.showTime
    val showLogo = opts.showLogo
    val logoSize = opts.avatarSize.dp
    val scale = opts.buttonScale / 100f
    val hasActions = data.actions.isNotEmpty()
    // The note stops above the dock buttons. Without buttons that gap shrinks
    // so the card does not end in an empty cork band, and the bottom holder
    // moves up with it to keep peeking past the paper.
    val noteBottom = if (hasActions) 46.dp else 16.dp
    val holderOffset = if (hasActions) (-36).dp else (-6).dp

    val noteE = rememberCardEnter(animateIn, 550, 0)
    val pinE = rememberCardEnter(animateIn, 450, 450)
    val textE = rememberCardEnter(animateIn, 400, 500)
    val miniE = rememberCardEnter(animateIn, 400, 650)
    val btnE = listOf(
        rememberCardEnter(animateIn, 400, 720),
        rememberCardEnter(animateIn, 400, 800)
    )
    val shake = rememberCardEnter(animateIn, 500, 420)
    val ex = exitFraction()

    CardRoot(
        animateIn = animateIn,
        exitFraction = exitFraction,
        enterDurationMs = 1100,
        enter = { t ->
            CardLayer(
                alpha = keyframe(t, 0f to 0f, 0.05f to 1f, 1f to 1f),
                tx = if (t > 0.38f) {
                    keyframe(t, 0.42f to 0f, 0.47f to 1.5f, 0.52f to -1.5f, 0.58f to 1f, 0.62f to 0f, 1f to 0f)
                } else 0f,
                ty = if (t > 0.38f) {
                    keyframe(t, 0.42f to 0f, 0.47f to 1f, 0.52f to -1f, 0.58f to 0f, 1f to 0f)
                } else 0f,
                rot = if (t > 0.38f) {
                    keyframe(t, 0.42f to 0f, 0.47f to 0.2f, 0.52f to -0.2f, 0.58f to 0f, 1f to 0f)
                } else 0f
            )
        },
        exit = { p -> CardLayer(alpha = 1f - exitDelay(p, 0.2f)) }
    ) {
        CardFrame(
            lines = lines,
            hasDock = hasActions,
            decorationAlpha = intensity / 100f,
            shape = RoundedCornerShape(10.dp),
            background = Brush.verticalGradient(listOf(Color(0xFFB07E52), Color(0xFF96613C))),
            border = Color(0xFF6E4A2C),
            borderWidth = 7.dp
        ) {
            Canvas(Modifier.fillMaxSize().graphicsLayer { alpha = intensity / 100f }) {
                var y = 0f
                var row = 0
                while (y < size.height) {
                    var x = if (row % 2 == 0) 0f else 3.dp.toPx()
                    while (x < size.width) {
                        drawCircle(Color(0x24000000), radius = 0.8.dp.toPx(), center = Offset(x, y))
                        x += 7.dp.toPx()
                    }
                    y += 9.dp.toPx()
                    row++
                }
                y = 0f
                row = 0
                while (y < size.height) {
                    var x = if (row % 2 == 0) 3.dp.toPx() else 0f
                    while (x < size.width) {
                        drawCircle(Color(0x1AFFFFFF), radius = 0.8.dp.toPx(), center = Offset(x, y))
                        x += 11.dp.toPx()
                    }
                    y += 13.dp.toPx()
                    row++
                }
            }

            Box(Modifier.fillMaxSize().padding(7.dp)) {
                // the sticky note — fills the space the card gives it, so it
                // grows with the message and stops above the dock buttons
                Box(
                    Modifier
                        .align(Alignment.TopStart)
                        .padding(start = 9.dp, end = 9.dp, top = 9.dp)
                        .fillMaxWidth()
                        .fillMaxHeight()
                        .padding(bottom = noteBottom)
                        .graphicsLayer {
                            if (ex > 0f) {
                                val d = exitDelay(ex, 0.12f)
                                rotationZ = 1.5f + 12.5f * d
                                alpha = 1f - d
                                translationX = 34.dp.toPx() * d
                                translationY = -48.dp.toPx() * d
                                scaleX = 1f - 0.5f * d
                                scaleY = 1f - 0.5f * d
                            } else {
                                rotationZ = keyframe(noteE, 0f to -10f, 0.55f to 2.6f, 0.75f to 1.1f, 1f to 1.5f)
                                alpha = (noteE / 0.55f).coerceIn(0f, 1f)
                                scaleX = keyframe(noteE, 0f to 1.7f, 0.55f to 0.94f, 0.75f to 1.03f, 1f to 1f)
                                scaleY = scaleX
                            }
                        }
                        .background(Brush.verticalGradient(listOf(Color(0xFFFDF7A2), Color(0xFFFFEE85))))
                ) {
                    Canvas(Modifier.fillMaxSize().graphicsLayer { alpha = intensity / 100f }) {
                        var y = 17.dp.toPx()
                        while (y < size.height) {
                            drawRect(Color(0x29786428), Offset(0f, y), Size(size.width, 1.dp.toPx()))
                            y += 17.dp.toPx()
                        }
                        val s = 16.dp.toPx()
                        val fold = Path().apply {
                            moveTo(size.width, size.height)
                            lineTo(size.width - s, size.height)
                            lineTo(size.width, size.height - s)
                            close()
                        }
                        drawPath(fold, Color(0x59967820))
                    }
                    Column(
                        Modifier
                            .padding(start = 18.dp, end = 18.dp, top = 18.dp)
                            .fillMaxWidth()
                            .cardLayer(if (ex > 0f) CardLayer(alpha = 1f - exitDelay(ex, 0.1f)) else CardLayer(alpha = textE))
                    ) {
                        CardSenderRow(
                            title = data.title,
                            showTime = showTime,
                            showLogo = showLogo,
                            logoSize = logoSize,
                            logoData = data,
                            time = data.time,
                            titleColor = Color(0xFF4A4433),
                            timeColor = accent,
                            titleFont = ThemeFonts.Caveat,
                            timeFont = ThemeFonts.Nunito,
                            titleSize = 21f,
                            titleLetter = 0.3f,
                            titleWeight = FontWeight.Bold
                        )
                        Spacer(Modifier.height(3.dp))
                        CardBody(
                            text = data.body,
                            lines = lines,
                            color = Color(0xFF55503B),
                            font = ThemeFonts.Caveat,
                            weight = FontWeight.Bold,
                            sizeSp = 14.5f,
                            lineHeightFactor = 1.07f
                        )
                    }
                }

                // The two small notes that hold the paper: drawn after the
                // yellow note so they sit on top of it and stay visible.
                Box(
                    Modifier
                        .align(Alignment.TopEnd)
                        .offset(x = 6.dp, y = (-6).dp)
                        .size(30.dp)
                        .graphicsLayer {
                            rotationZ = -8f
                            alpha = if (ex > 0f) 1f - exitDelay(ex, 0f) else miniE
                            scaleX = if (ex > 0f) 1f else 0.4f + 0.6f * miniE
                            scaleY = scaleX
                        }
                        .background(Brush.verticalGradient(listOf(Color(0xFFD3E8AC), Color(0xFFC0DC92))))
                )
                Box(
                    Modifier
                        .align(Alignment.BottomStart)
                        .offset(x = (-4).dp, y = holderOffset)
                        .size(30.dp)
                        .graphicsLayer {
                            rotationZ = if (ex > 0f) 6f else -8f
                            alpha = if (ex > 0f) 1f - exitDelay(ex, 0f) else miniE
                            scaleX = if (ex > 0f) 1f else 0.4f + 0.6f * miniE
                            scaleY = scaleX
                        }
                        .background(Brush.verticalGradient(listOf(Color(0xFFC4DDF2), Color(0xFFAECDE8))))
                )

                // push pin
                Box(
                    Modifier
                        .align(Alignment.TopCenter)
                        .offset(y = 6.dp)
                        .size(18.dp)
                        .graphicsLayer {
                            if (ex > 0f) {
                                val d = exitDelay(ex, 0f)
                                alpha = 1f - d
                                translationY = -32.dp.toPx() * d
                                rotationZ = 24f * d
                            } else {
                                alpha = pinE
                                translationY = keyframe(pinE, 0f to -26f, 0.6f to 2f, 1f to 0f).dp.toPx()
                            }
                        }
                ) {
                    Box(
                        Modifier
                            .fillMaxSize()
                            .clip(CircleShape)
                            .background(
                                Brush.radialGradient(
                                    listOf(Color(0xFFFF9C8A), Color(0xFFD23B2A), Color(0xFF9E2A1C)),
                                    center = Offset(6f, 5f),
                                    radius = 16f
                                )
                            )
                    )
                    Box(
                        Modifier
                            .align(Alignment.BottomCenter)
                            .offset(y = 9.dp)
                            .size(width = 2.dp, height = 9.dp)
                            .background(Brush.verticalGradient(listOf(Color(0xFF6B6B6B), Color(0xFF3A3A3A))))
                    )
                }

                CardDock(
                    data = data,
                    interactions = interactions,
                    modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 3.dp, start = 9.dp, end = 9.dp),
                    background = Color(0xFFE9D9B8),
                    foreground = Color(0xFF6B4F2A),
                    border = Color(0xFFB08D5A),
                    shape = RoundedCornerShape(6.dp),
                    font = ThemeFonts.Nunito,
                    fontSize = 10.5f,
                    letterSpacing = 0.7f,
                    uppercase = true,
                    height = 28.dp,
                    horizontalPadding = 5.dp,
                    verticalPadding = 4.dp,
                    gap = 6.dp,
                    buttonScale = scale,
                    borderFor = { shell -> if (shell) Color(0xFFB08D5A) else Color(0xFF8E2A20) },
                    foregroundFor = { shell -> if (shell) Color(0xFF6B4F2A) else Color(0xFFFFE9D8) },
                    backgroundFor = { shell ->
                        if (shell) Brush.verticalGradient(listOf(Color(0xFFE9D9B8), Color(0xFFDFCDA8)))
                        else Brush.verticalGradient(listOf(Color(0xFFC0392B), Color(0xFFA92F23)))
                    },
                    shapeFor = { shell -> if (shell) RoundedCornerShape(6.dp) else RoundedCornerShape(50) }
                )
            }
        }
    }
}
