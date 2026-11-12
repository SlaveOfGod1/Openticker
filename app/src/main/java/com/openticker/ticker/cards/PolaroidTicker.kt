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
/* 04 — Polaroid                                                        */
/* ==================================================================== */

@Composable
fun CuratedPolaroidTicker(
    data: TickerData,
    interactions: TickerInteractions?,
    animateIn: Boolean,
    exitFraction: () -> Float
) {
    val context = LocalContext.current
    val opts = Prefs.themeOptions(context, "polaroid", 0xFFC0653E.toInt())
    val accent = if (opts.accentArgb == 0xFFC0653E.toInt()) Color(0xFFC0653E) else Color(opts.accentArgb)
    val lines = opts.lines
    val intensity = opts.intensity
    val showTime = opts.showTime
    val showLogo = opts.showLogo
    val logoSize = opts.avatarSize.dp
    val scale = opts.buttonScale / 100f

    val devE = rememberCardEnter(animateIn, 1600, 250)
    val textE = rememberCardEnter(animateIn, 600, 1050)
    val btnE = listOf(
        rememberCardEnter(animateIn, 450, 1350),
        rememberCardEnter(animateIn, 450, 1450)
    )
    val ex = exitFraction()

    CardRoot(
        animateIn = animateIn,
        exitFraction = exitFraction,
        enterDurationMs = 550,
        enter = { t ->
            CardLayer(
                alpha = keyframe(t, 0f to 0f, 0.7f to 1f, 1f to 1f),
                ty = keyframe(t, 0f to -18f, 0.7f to 2f, 1f to 0f),
                rot = keyframe(t, 0f to -3f, 0.7f to -0.6f, 1f to -1f),
                sx = keyframe(t, 0f to 1.06f, 0.7f to 0.99f, 1f to 1f),
                sy = keyframe(t, 0f to 1.06f, 0.7f to 0.99f, 1f to 1f)
            )
        },
        exit = { p ->
            val d = exitDelay(p, 0.25f)
            CardLayer(
                alpha = 1f - d,
                ty = 34f * d,
                rot = -1f + 2.5f * d,
                sx = 1f - 0.06f * d,
                sy = 1f - 0.06f * d
            )
        }
    ) {
        CardFrame(
            lines = lines,
            hasDock = data.actions.isNotEmpty(),
            // The photo is a fixed 118dp frame above the dock.
            baseHeight = 170.dp,
            dockSpace = 39.dp,
            decorationAlpha = intensity / 100f,
            shape = RoundedCornerShape(6.dp),
            background = Brush.verticalGradient(listOf(Color(0xFFF7F2E6), Color(0xFFEFE8D6)))
        ) {
            // photo
            Box(
                Modifier
                    .align(Alignment.TopStart)
                    .padding(start = 12.dp, end = 12.dp, top = 12.dp)
                    .fillMaxWidth()
                    .height(118.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .cardLayer(
                        if (ex > 0f) {
                            CardLayer(alpha = 1f - 0.5f * exitDelay(ex, 0f))
                        } else CardLayer(alpha = 0.35f + 0.65f * devE)
                    )
            ) {
                Canvas(Modifier.fillMaxSize().graphicsLayer { alpha = intensity / 100f }) {
                    drawRect(
                        Brush.linearGradient(
                            listOf(Color(0xFFFFE9C7), Color(0xFFFFC9A3), Color(0xFFF49B7A))
                        )
                    )
                    drawRect(
                        Brush.radialGradient(
                            listOf(Color(0xD9FFF4C8), Color.Transparent),
                            center = Offset(size.width * 0.82f, size.height * 0.22f),
                            radius = size.width * 0.34f
                        )
                    )
                    // sun
                    drawCircle(
                        Brush.radialGradient(
                            listOf(Color(0xFFFFF8E0), Color(0xFFFFD98A)),
                            center = Offset(size.width - 26.dp.toPx() - 14.dp.toPx(), 16.dp.toPx() + 14.dp.toPx()),
                            radius = 30.dp.toPx()
                        ),
                        radius = 17.dp.toPx(),
                        center = Offset(size.width - 26.dp.toPx() - 0f, 16.dp.toPx() + 17.dp.toPx())
                    )
                    // birds
                    fun bird(x: Float, y: Float, scale: Float) {
                        drawArc(
                            Color(0xBF5A321E), 200f, 140f, false,
                            topLeft = Offset(x, y), size = Size(9.dp.toPx() * scale, 8.dp.toPx() * scale),
                            style = Stroke(1.6.dp.toPx(), cap = StrokeCap.Round)
                        )
                        drawArc(
                            Color(0xBF5A321E), 200f, 140f, false,
                            topLeft = Offset(x + 8.dp.toPx() * scale, y), size = Size(9.dp.toPx() * scale, 8.dp.toPx() * scale),
                            style = Stroke(1.6.dp.toPx(), cap = StrokeCap.Round)
                        )
                    }
                    bird(size.width * 0.24f, size.height * 0.24f, 1f)
                    bird(size.width * 0.40f, size.height * 0.16f, 0.7f)
                    // grain
                    var y = 0f
                    while (y < size.height) {
                        drawRect(Color(0x06000000), Offset(0f, y), Size(size.width, 1.dp.toPx()))
                        y += 3.dp.toPx()
                    }
                    drawRect(
                        Brush.radialGradient(
                            listOf(Color.Transparent, Color(0x2E783C14)),
                            center = Offset(size.width / 2f, size.height / 2f),
                            radius = size.width * 0.7f
                        )
                    )
                }
                // developing wash
                val wash = if (ex > 0f) 0.5f * exitDelay(ex, 0f) else (1f - devE) * 0.75f
                if (wash > 0.001f) {
                    Box(Modifier.fillMaxSize().background(Color(0xFFF3EAD8).copy(alpha = wash)))
                }
            }

            // tape
            Box(
                Modifier
                    .align(Alignment.TopCenter)
                    .offset(y = (-7).dp)
                    .size(width = 72.dp, height = 18.dp)
                    .graphicsLayer { rotationZ = 2f }
                    .background(Color(0x8CFFFCEB))
                    .border(1.dp, Color(0x66A08C5A))
            )

            // notification text over the photo
            Column(
                Modifier
                    .align(Alignment.TopStart)
                    .padding(start = 26.dp, end = 26.dp, top = 22.dp)
                    .fillMaxWidth()
                    .cardLayer(
                        if (ex > 0f) CardLayer(alpha = 1f - exitDelay(ex, 0.1f))
                        else CardLayer(alpha = textE, ty = 6f * (1f - textE))
                    )
            ) {
                CardSenderRow(
                    title = data.title,
                    showTime = showTime,
                    showLogo = showLogo,
                    logoSize = logoSize,
                    logoData = data,
                    time = data.time,
                    titleColor = Color(0xFF4A3226),
                    timeColor = accent,
                    titleFont = ThemeFonts.Caveat,
                    timeFont = ThemeFonts.Nunito,
                    titleSize = 21f,
                    titleLetter = 0.3f,
                    titleWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(4.dp))
                CardBody(
                    text = data.body,
                    lines = lines,
                    color = Color(0xFF5C4434),
                    font = ThemeFonts.Nunito,
                    weight = FontWeight.Bold,
                    sizeSp = 11.5f,
                    lineHeightFactor = 1.3f,
                    style = TextStyle(shadow = Shadow(Color(0x59FFFFFF), Offset(0f, 1f), 0f))
                )
            }

            CardDock(
                data = data,
                interactions = interactions,
                modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 11.dp, start = 26.dp, end = 26.dp),
                background = Color(0xFFEFE9DA),
                foreground = Color(0xFF5A4A3A),
                border = Color(0xFFC9BFA8),
                shape = RoundedCornerShape(50),
                font = ThemeFonts.Nunito,
                fontSize = 10.5f,
                letterSpacing = 0.7f,
                uppercase = true,
                height = 28.dp,
                horizontalPadding = 5.dp,
                verticalPadding = 4.dp,
                gap = 6.dp,
                buttonScale = scale,
                borderFor = { Color(0xFFC9BFA8) },
                foregroundFor = { Color(0xFF5A4A3A) },
                backgroundFor = { Brush.verticalGradient(listOf(Color(0xFFEFE9DA), Color(0xFFE6DDC9))) }
            )
        }
    }
}
