package com.openticker.ticker.cards

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.openticker.data.Prefs
import com.openticker.data.TickerData
import com.openticker.data.TickerInteractions
import com.openticker.ticker.styles.notificationActionSpecs

/* ==================================================================== */
/* 21 — Cat Paws                                                       */
/* ==================================================================== */
/*
 * Crisp white card with a blushing pink rim. On the way in a ball of yarn
 * drops fast from the top, unrolls into the card as it lands, four paw
 * prints sprint diagonally across it (three ink steps, one pink), then the
 * message and the three wool-ball buttons pop in. On the way out the yarn
 * balls pop off and the card winds itself back into a ball and shoots up
 * off the screen. The card hugs the message: a one-liner is short, more
 * wrapped lines make it taller (CardFrame reports the real line count).
 */

private data class YarnColours(
    val light: Color,
    val mid: Color,
    val deep: Color,
    val thread: Color
)

/**
 * One paw in the sprinting trail: position as a fraction of the card, its
 * box size in dp, the CSS rotation, and which ink it was stamped with.
 */
private data class PawStep(
    val x: Float,
    val y: Float,
    val sizeDp: Float,
    val degrees: Float,
    val colour: Color
)

private val YarnPalette = listOf(
    YarnColours(
        light = Color(0xFFFFD3DF),
        mid = Color(0xFFF792B0),
        deep = Color(0xFFE56A92),
        thread = Color(0xFFF08CAD)
    ),
    YarnColours(
        light = Color(0xFFD6EBFF),
        mid = Color(0xFF8FC1F2),
        deep = Color(0xFF5D9BE0),
        thread = Color(0xFF7FB3EC)
    ),
    YarnColours(
        light = Color(0xFFD3F4E4),
        mid = Color(0xFF8FD8B8),
        deep = Color(0xFF5FB894),
        thread = Color(0xFF6FBFA0)
    )
)

/** The shared paw print: one pad and four toes in a 100 x 100 box. */
private fun DrawScope.drawPaw(
    colour: Color,
    boxPx: Float,
    left: Float,
    top: Float,
    alpha: Float
) {
    val s = boxPx / 100f
    fun pad(cx: Float, cy: Float, rx: Float, ry: Float, degrees: Float) {
        rotate(degrees, Offset(left + cx * s, top + cy * s)) {
            drawOval(
                color = colour,
                topLeft = Offset(left + (cx - rx) * s, top + (cy - ry) * s),
                size = Size(2f * rx * s, 2f * ry * s),
                alpha = alpha
            )
        }
    }
    pad(50f, 66f, 24f, 19f, 0f)
    pad(23f, 42f, 9.5f, 12.5f, -22f)
    pad(43f, 31f, 10f, 13f, -5f)
    pad(62f, 32f, 10f, 13f, 9f)
    pad(80f, 45f, 9f, 12f, 24f)
}

/** A wool ball: radial gradient, wound thread, highlight. */
private fun DrawScope.drawYarnBall(colours: YarnColours, ballPx: Float) {
    val half = ballPx / 2f
    val centre = Offset(half, half)
    drawCircle(
        brush = Brush.radialGradient(
            listOf(colours.light, colours.mid, colours.deep),
            center = Offset(ballPx * 0.35f, ballPx * 0.30f),
            radius = ballPx * 0.80f
        ),
        radius = half,
        center = centre
    )

    val s = ballPx / 64f
    val thread = Stroke(width = 2.4f * s, cap = StrokeCap.Round)
    val threadColour = Color(0x80FFFFFF)
    val clip = Path().apply { addOval(Rect(0f, 0f, ballPx, ballPx)) }

    clipPath(clip) {
        fun ring(rx: Float, ry: Float, degrees: Float) {
            rotate(degrees, centre) {
                drawOval(
                    color = threadColour,
                    topLeft = Offset(half - rx * s, half - ry * s),
                    size = Size(2f * rx * s, 2f * ry * s),
                    style = thread
                )
            }
        }
        ring(34f, 18f, 22f)
        ring(34f, 18f, -26f)
        ring(34f, 11f, 65f)
        ring(34f, 24f, -7f)

        val wave = Path().apply {
            moveTo(-2f * s, 40f * s)
            quadraticBezierTo(20f * s, 34f * s, 34f * s, 44f * s)
            quadraticBezierTo(48f * s, 54f * s, 70f * s, 40f * s)
        }
        drawPath(wave, threadColour, style = thread)
    }

    rotate(-24f, Offset(24f * s, 19f * s)) {
        drawOval(
            color = Color(0x59FFFFFF),
            topLeft = Offset(18f * s, 15f * s),
            size = Size(12f * s, 8f * s)
        )
    }
}

/** The loose thread curl dangling under each ball. */
private fun DrawScope.drawThreadCurl(colour: Color, strokePx: Float) {
    val inset = strokePx / 2f
    val path = Path().apply {
        moveTo(inset, 0f)
        lineTo(inset, size.height * 0.45f)
        quadraticBezierTo(inset, size.height, size.width / 2f, size.height)
        quadraticBezierTo(size.width - inset, size.height, size.width - inset, size.height * 0.45f)
        lineTo(size.width - inset, 0f)
    }
    drawPath(
        path = path,
        color = colour,
        style = Stroke(width = strokePx, cap = StrokeCap.Round)
    )
}

/* ------------------------------------------------------------------ */
/* The dock: three wool balls instead of pills                         */
/* ------------------------------------------------------------------ */

@Composable
private fun PawsDock(
    data: TickerData,
    interactions: TickerInteractions?,
    ball: Dp,
    scale: Float,
    btnE: List<Float>,
    ex: Float,
    effect: Boolean,
    modifier: Modifier = Modifier
) {
    val specs = notificationActionSpecs(data, interactions)
    if (specs.isEmpty()) return

    val sway = if (effect) ambientAlternate(2600) else 0.5f
    val threadWidth = 14.dp
    val threadHeight = 12.dp

    Row(
        modifier,
        horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.Top
    ) {
        specs.forEachIndexed { index, spec ->
            val source = remember { MutableInteractionSource() }
            val pressed by source.collectIsPressedAsState()
            val colours = YarnPalette[index % YarnPalette.size]
            val p = btnE.getOrElse(index) { 1f }
            val d = if (ex > 0f) exitDelay(ex, index * 0.06f) else 0f

            Box(
                Modifier
                    .size(width = ball, height = ball + 9.dp)
                    .cardLayer(
                        if (ex > 0f) {
                            CardLayer(
                                alpha = keyframe(d, 0f to 1f, 0.40f to 1f, 1f to 0f),
                                tx = 0f,
                                ty = keyframe(d, 0f to 0f, 0.40f to 0f, 1f to 28f),
                                sx = keyframe(d, 0f to 1f, 0.40f to 1.15f, 1f to 0.1f),
                                sy = keyframe(d, 0f to 1f, 0.40f to 0.82f, 1f to 0.1f),
                                rot = keyframe(d, 0f to 0f, 0.40f to 0f, 1f to 55f)
                            )
                        } else {
                            CardLayer(
                                alpha = keyframe(p, 0f to 0f, 0.70f to 1f, 1f to 1f),
                                tx = 0f,
                                ty = 0f,
                                sx = keyframe(p, 0f to 0.2f, 0.70f to 1.12f, 1f to 1f),
                                sy = keyframe(p, 0f to 0.2f, 0.70f to 1.12f, 1f to 1f),
                                rot = keyframe(p, 0f to -32f, 0.70f to 6f, 1f to 0f)
                            )
                        }
                    )
                    .graphicsLayer {
                        if (pressed) {
                            scaleX = 0.93f
                            scaleY = 0.93f
                        }
                    }
                    .clickable(
                        interactionSource = source,
                        indication = null,
                        enabled = spec.onClick != null
                    ) { spec.onClick?.invoke() }
            ) {
                Canvas(Modifier.size(ball)) {
                    drawYarnBall(colours, size.minDimension)
                }

                Canvas(
                    Modifier
                        .offset(
                            x = (ball - threadWidth) / 2f,
                            y = ball - 2.dp
                        )
                        .size(threadWidth, threadHeight)
                        .graphicsLayer {
                            rotationZ = -9f + 18f * sway
                            transformOrigin = TransformOrigin(0.5f, 0f)
                        }
                ) {
                    drawThreadCurl(colours.thread, 2.4.dp.toPx())
                }

                Box(Modifier.size(ball), contentAlignment = Alignment.Center) {
                    if (spec.iconOnly) {
                        Icon(
                            spec.icon,
                            contentDescription = spec.label,
                            tint = Color(0xFF3A2B31),
                            modifier = Modifier.size(ball * 0.38f)
                        )
                    } else {
                        Text(
                            spec.label.uppercase(),
                            color = Color(0xFF3A2B31),
                            fontSize = (9f * scale).coerceIn(7.5f, 12f).sp,
                            fontFamily = ThemeFonts.QuicksandBold,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.9.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            textAlign = TextAlign.Center,
                            style = TextStyle(
                                shadow = Shadow(Color(0x73FFFFFF), Offset(0f, 1f), 0f)
                            )
                        )
                    }
                }
            }
        }
    }
}

/* ------------------------------------------------------------------ */
/* The card                                                            */
/* ------------------------------------------------------------------ */

@Composable
fun CuratedPawsTicker(
    data: TickerData,
    interactions: TickerInteractions?,
    animateIn: Boolean,
    exitFraction: () -> Float
) {
    val context = LocalContext.current
    val opts = Prefs.themeOptions(context, "paws", 0xFFF27FA5.toInt())
    val accent = Color(opts.accentArgb)
    val lines = opts.lines
    val intensity = opts.intensity
    val showTime = opts.showTime
    val showLogo = opts.showLogo
    val logoSize = opts.avatarSize.dp
    val scale = opts.buttonScale / 100f
    val effect = opts.effect

    // The wool balls grow with the button-size preference; the card keeps
    // the same breathing room above them so the message never collides.
    val ball = 56.dp * scale
    val baseHeight = 100.dp + ball
    val dockSpace = ball + 23.dp

    val pawE = listOf(
        rememberCardEnter(animateIn, 240, 520),
        rememberCardEnter(animateIn, 240, 640),
        rememberCardEnter(animateIn, 240, 760),
        rememberCardEnter(animateIn, 240, 880)
    )
    val textE = rememberCardEnter(animateIn, 420, 1020)
    val btnE = listOf(
        rememberCardEnter(animateIn, 400, 1180),
        rememberCardEnter(animateIn, 400, 1290),
        rememberCardEnter(animateIn, 400, 1400)
    )
    val ex = exitFraction()

    CardRoot(
        animateIn = animateIn,
        exitFraction = exitFraction,
        enterDurationMs = 620,
        enter = { t ->
            CardLayer(
                alpha = keyframe(t, 0f to 0f, 0.12f to 1f, 1f to 1f),
                ty = keyframe(
                    t,
                    0f to -360f,
                    0.30f to -210f,
                    0.46f to -45f,
                    0.56f to 0f,
                    0.70f to -7f,
                    0.85f to 2f,
                    1f to 0f
                ),
                sx = keyframe(
                    t,
                    0f to 0.12f,
                    0.30f to 0.30f,
                    0.46f to 0.60f,
                    0.56f to 1.07f,
                    0.70f to 0.955f,
                    0.85f to 1.015f,
                    1f to 1f
                ),
                sy = keyframe(
                    t,
                    0f to 0.12f,
                    0.30f to 0.30f,
                    0.46f to 0.60f,
                    0.56f to 0.88f,
                    0.70f to 1.045f,
                    0.85f to 0.99f,
                    1f to 1f
                ),
                rot = keyframe(t, 0f to -280f, 0.30f to -160f, 0.46f to -60f, 0.56f to 0f, 1f to 0f)
            )
        },
        exit = { p ->
            val d = exitDelay(p, 0.20f)
            CardLayer(
                alpha = keyframe(d, 0f to 1f, 0.72f to 1f, 1f to 0f),
                tx = keyframe(d, 0f to 0f, 0.14f to 0f, 0.32f to -7f, 0.52f to 5f, 0.72f to -3f, 1f to 0f),
                ty = keyframe(d, 0f to 0f, 0.14f to 0f, 0.32f to -55f, 0.52f to -150f, 0.72f to -260f, 1f to -430f),
                sx = keyframe(d, 0f to 1f, 0.14f to 1.08f, 0.32f to 0.55f, 0.52f to 0.32f, 0.72f to 0.28f, 1f to 0.22f),
                sy = keyframe(d, 0f to 1f, 0.14f to 0.84f, 0.32f to 0.55f, 0.52f to 0.32f, 0.72f to 0.28f, 1f to 0.22f),
                rot = keyframe(d, 0f to 0f, 0.14f to 3f, 0.32f to 90f, 0.52f to 200f, 0.72f to 310f, 1f to 430f)
            )
        }
    ) {
        CardFrame(
            lines = lines,
            hasDock = data.actions.isNotEmpty(),
            baseHeight = baseHeight,
            dockSpace = dockSpace,
            decorationAlpha = 1f,
            shape = RoundedCornerShape(22.dp),
            background = Brush.verticalGradient(listOf(Color(0xFFFFFFFF), Color(0xFFFFF8FA))),
            border = Color(0xFFF7DCE5),
            borderWidth = 1.5.dp
        ) {
            Canvas(Modifier.fillMaxSize()) {
                val alpha = intensity / 100f

                // two faint pink glows, bottom right and top left
                drawCircle(
                    brush = Brush.radialGradient(listOf(Color(0x29F7A8BE), Color.Transparent)),
                    radius = 95.dp.toPx(),
                    center = Offset(size.width * 0.88f, size.height * 1.14f),
                    alpha = alpha
                )
                drawCircle(
                    brush = Brush.radialGradient(listOf(Color(0x1FF7A8BE), Color.Transparent)),
                    radius = 85.dp.toPx(),
                    center = Offset(size.width * 0.05f, -size.height * 0.12f),
                    alpha = alpha
                )

                // faint paw watermark lounging in the corner
                val mark = 118.dp.toPx()
                val markLeft = size.width - mark + 14.dp.toPx()
                val markTop = size.height - mark + 20.dp.toPx()
                rotate(-18f, Offset(markLeft + mark / 2f, markTop + mark / 2f)) {
                    drawPaw(
                        colour = Color(0xFFF27FA5),
                        boxPx = mark,
                        left = markLeft,
                        top = markTop,
                        alpha = 0.07f * alpha
                    )
                }

                // the sprinting trail: lower right to upper left
                val trail = listOf(
                    PawStep(0.86f, 0.80f, 40f, -42f, Color(0xFF0F0C13)),
                    PawStep(0.62f, 0.56f, 38f, -56f, Color(0xFF0F0C13)),
                    PawStep(0.37f, 0.40f, 42f, -42f, Color(0xFFD9537F)),
                    PawStep(0.12f, 0.16f, 38f, -56f, Color(0xFF0F0C13))
                )
                trail.forEachIndexed { index, step ->
                    val p = pawE.getOrElse(index) { 1f }
                    val box = step.sizeDp.dp.toPx()
                    val left = size.width * step.x
                    val top = size.height * step.y
                    val centre = Offset(left + box / 2f, top + box / 2f)
                    rotate(step.degrees, centre) {
                        scale(
                            scale = keyframe(p, 0f to 1.7f, 0.55f to 0.88f, 1f to 1f),
                            pivot = centre
                        ) {
                            drawPaw(
                                colour = step.colour,
                                boxPx = box,
                                left = left,
                                top = top,
                                alpha = keyframe(p, 0f to 0f, 0.55f to 1f, 1f to 1f)
                            )
                        }
                    }
                }
            }

            Column(
                Modifier
                    .align(Alignment.TopStart)
                    .padding(start = 20.dp, end = 20.dp, top = 16.dp)
                    .fillMaxWidth()
                    .cardLayer(
                        if (ex > 0f) {
                            val d = exitDelay(ex, 0.04f)
                            CardLayer(
                                alpha = 1f - d,
                                ty = 10f * d,
                                sx = 1f - 0.04f * d,
                                sy = 1f - 0.04f * d
                            )
                        } else {
                            CardLayer(alpha = textE, ty = 12f * (1f - textE))
                        }
                    )
            ) {
                CardSenderRow(
                    title = data.title,
                    showTime = showTime,
                    // A real notification shows its own app icon; the lab
                    // preview falls back to the app's letter badge.
                    showLogo = showLogo,
                    logoSize = logoSize,
                    logoData = data,
                    time = data.time,
                    titleColor = Color(0xFF3A2B31),
                    timeColor = accent,
                    titleFont = ThemeFonts.Baloo,
                    timeFont = ThemeFonts.QuicksandBold,
                    titleSize = 18f,
                    titleLetter = 0.3f,
                    titleWeight = FontWeight.SemiBold,
                    timeSize = 12.5f,
                    timeLetter = 0.5f
                )
                Spacer(Modifier.height(5.dp))
                CardBody(
                    text = data.body,
                    lines = lines,
                    color = Color(0xFF93707C),
                    font = ThemeFonts.QuicksandBold,
                    weight = FontWeight.Bold,
                    sizeSp = 11.5f,
                    lineHeightFactor = 1.3043f
                )
            }

            PawsDock(
                data = data,
                interactions = interactions,
                ball = ball,
                scale = scale,
                btnE = btnE,
                ex = ex,
                effect = effect,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 14.dp)
            )
        }
    }
}
