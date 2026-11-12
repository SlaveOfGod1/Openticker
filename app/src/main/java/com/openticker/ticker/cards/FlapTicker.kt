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
/* 02 — Split-Flap Board                                                */
/* ==================================================================== */

private val FlapAmber = Color(0xFFFFB627)

@Composable
fun CuratedFlapTicker(
    data: TickerData,
    interactions: TickerInteractions?,
    animateIn: Boolean,
    exitFraction: () -> Float
) {
    val context = LocalContext.current
    val opts = Prefs.themeOptions(context, "flap", 0xFFFFB627.toInt())
    val amber = if (opts.accentArgb == 0xFFFFB627.toInt()) FlapAmber else Color(opts.accentArgb)
    val lines = opts.lines
    val intensity = opts.intensity
    val showTime = opts.showTime
    val showLogo = opts.showLogo
    val logoSize = opts.avatarSize.dp
    val scale = opts.buttonScale / 100f
    val effect = opts.effect

    val textE = rememberCardEnter(animateIn, 450, 320)
    val btnE = listOf(
        rememberCardEnter(animateIn, 400, 580),
        rememberCardEnter(animateIn, 400, 700)
    )
    val ledP = ambientLoop(1400)
    val flipP = ambientLoop(5000)
    val ex = exitFraction()

    val cells = listOf("08:40", "ROME", "", "GATE 4", "ON TIME", "", "09:15", "")

    CardRoot(
        animateIn = animateIn,
        exitFraction = exitFraction,
        enterDurationMs = 700,
        enter = { t ->
            CardLayer(
                alpha = keyframe(t, 0f to 0f, 0.6f to 1f, 1f to 1f),
                rotX = keyframe(t, 0f to -70f, 0.6f to 10f, 0.8f to -4f, 1f to 0f),
                originY = 0f
            )
        },
        exit = { p ->
            CardLayer(
                alpha = keyframe(p, 0f to 1f, 1f to 0f),
                rotX = -86f * p,
                ty = -8f * p,
                originY = 0f
            )
        }
    ) {
        CardFrame(
            lines = lines,
            hasDock = data.actions.isNotEmpty(),
            baseHeight = 161.dp,
            dockSpace = 41.dp,
            decorationAlpha = intensity / 100f,
            shape = RoundedCornerShape(8.dp),
            background = Brush.verticalGradient(
                0f to Color(0xFF1B1E24), 0.7f to Color(0xFF101216), 1f to Color(0xFF14161A)
            ),
            border = Color.Black,
            borderWidth = 3.dp
        ) {
            Canvas(Modifier.fillMaxSize().graphicsLayer { alpha = intensity / 100f }) {
                var y = 0f
                while (y < size.height) {
                    drawRect(Color(0x80000000), Offset(0f, y), Size(size.width, 1.5.dp.toPx()))
                    drawRect(Color(0x0BFFFFFF), Offset(0f, y + 1.5.dp.toPx()), Size(size.width, 1.5.dp.toPx()))
                    y += 24.dp.toPx()
                }
                drawRect(Color(0x12FFFFFF), Offset.Zero, Size(size.width, 1.dp.toPx()))
                drawRect(
                    Brush.verticalGradient(
                        listOf(Color.Transparent, Color(0x73000000)),
                        startY = size.height - 24.dp.toPx(),
                        endY = size.height
                    ),
                    Offset(0f, size.height - 24.dp.toPx()),
                    Size(size.width, 24.dp.toPx())
                )
            }

            Row(
                Modifier.align(Alignment.TopStart).padding(start = 14.dp, end = 14.dp, top = 10.dp).fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "OPENTICKER CENTRAL · DEPARTURES",
                    color = Color(0xFF8F7434),
                    fontSize = 7.5.sp,
                    fontFamily = ThemeFonts.Oswald,
                    letterSpacing = 2.4.sp,
                    modifier = Modifier.weight(1f)
                )
                Box(
                    Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFFF5A4E).copy(alpha = if (effect && ledP >= 0.5f) 0.2f else 1f))
                )
            }

            Row(
                Modifier.align(Alignment.TopStart).padding(start = 14.dp, end = 14.dp, top = 24.dp).fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                cells.forEachIndexed { i, label ->
                    Box(
                        Modifier
                            .weight(1f)
                            .height(17.dp)
                            .graphicsLayer {
                                if (i == 2 && effect) {
                                    rotationX = keyframe(
                                        flipP,
                                        0f to 0f, 0.86f to 0f, 0.90f to -88f, 0.95f to 12f, 1f to 0f
                                    )
                                }
                            }
                            .clip(RoundedCornerShape(2.dp))
                            .background(
                                Brush.verticalGradient(
                                    0f to Color(0xFF2B2F36), 0.47f to Color(0xFF2B2F36),
                                    0.53f to Color(0xFF131519), 1f to Color(0xFF131519)
                                )
                            )
                            .border(1.dp, Color(0xFF060708), RoundedCornerShape(2.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            label,
                            color = Color(0xFFC9A85C),
                            fontSize = 6.5.sp,
                            fontFamily = ThemeFonts.Oswald,
                            letterSpacing = 0.8.sp,
                            maxLines = 1
                        )
                    }
                }
            }

            Column(
                Modifier
                    .align(Alignment.TopStart)
                    .padding(start = 20.dp, end = 20.dp, top = 50.dp)
                    .fillMaxWidth()
                    .cardLayer(
                        if (ex > 0f) {
                            CardLayer(alpha = 1f - ex, rotX = -84f * ex, originY = 1f)
                        } else {
                            CardLayer(
                                alpha = textE,
                                rotX = 88f * (1f - textE),
                                originY = 0f
                            )
                        }
                    )
            ) {
                CardSenderRow(
                    title = data.title,
                    showTime = showTime,
                    showLogo = showLogo,
                    logoSize = logoSize,
                    logoData = data,
                    time = data.time,
                    titleColor = amber,
                    timeColor = amber,
                    titleFont = ThemeFonts.Oswald,
                    timeFont = ThemeFonts.Oswald,
                    titleSize = 17f,
                    titleLetter = 1.5f,
                    uppercaseTitle = true,
                    titleWeight = FontWeight.SemiBold
                )
                Spacer(Modifier.height(5.dp))
                CardBody(
                    text = data.body,
                    lines = lines,
                    color = Color(0xFFE8E4D8),
                    font = ThemeFonts.Oswald,
                    weight = FontWeight.Normal,
                    sizeSp = 11.5f,
                    lineHeightFactor = 1.3f,
                    letterSpacing = 0.6f
                )
            }

            CardDock(
                data = data,
                interactions = interactions,
                modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 11.dp, start = 14.dp, end = 14.dp),
                background = Color(0xFF23262C),
                foreground = amber,
                border = Color(0xFF0A0C0E),
                shape = RoundedCornerShape(3.dp),
                font = ThemeFonts.Oswald,
                fontSize = 9.5f,
                letterSpacing = 1.4f,
                uppercase = true,
                height = 30.dp,
                horizontalPadding = 6.dp,
                verticalPadding = 5.dp,
                buttonScale = scale,
                borderFor = { Color(0xFF0A0C0E) },
                foregroundFor = { amber },
                backgroundFor = {
                    Brush.verticalGradient(
                        0f to Color(0xFF2B2F36), 0.47f to Color(0xFF2B2F36),
                        0.53f to Color(0xFF15171B), 1f to Color(0xFF15171B)
                    )
                }
            )
        }
    }
}
