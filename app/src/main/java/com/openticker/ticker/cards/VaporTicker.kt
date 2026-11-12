package com.openticker.ticker.cards

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.openticker.data.Prefs
import com.openticker.data.TickerData
import com.openticker.data.TickerInteractions
import com.openticker.ticker.styles.notificationActionSpecs

/* ==================================================================== */
/* 11 — Vaporwave                                                       */
/* ==================================================================== */
/*
 * A synthwave sunset: a slatted magenta sun, cyan wireframe peaks and a
 * grid floor rushing forward under you.
 *
 * The notification glitches before it arrives — the card tears, colour
 * splits and blows out, and only then does the message snap into the
 * wreckage. Leaving, it comes apart in reverse.
 */

private val VaporInk = Color(0xFFFFFFFF)
private val VaporCyan = Color(0xFF00E5FF)
private val VaporRed = Color(0xFFFF3E3E)
private val VaporPink = Color(0xFFFF71CE)
private val VaporSky0 = Color(0xFF04051A)
private val VaporSky1 = Color(0xFF080A30)
private val VaporSky2 = Color(0xFF220B4C)
private val VaporSky3 = Color(0xFF5C1154)
private val VaporSky4 = Color(0xFFA82674)
private val VaporSky5 = Color(0xFFD958A4)
private val VaporMount = Color(0xFF070B2E)

/** The whole choreography, in milliseconds. */
private const val VaporTimelineMs = 1300f

/** The way out. */
private const val VaporExitMs = 900f

/** How long the glitch runs before the message arrives. */
private const val VaporGlitchMs = 550f

/* ------------------------------------------------------------------ */
/* Stepped keyframes                                                   */
/* ------------------------------------------------------------------ */

private fun stepped(p: Float, stops: List<Pair<Float, Float>>): Float {
    if (stops.isEmpty()) return 0f
    for (i in 0 until stops.size - 1) {
        if (p < stops[i + 1].first) return stops[i].second
    }
    return stops.last().second
}

private val VaporGlitchDx = listOf(
    0f to -14f, 0.12f to 11f, 0.24f to -7f, 0.36f to 5f, 0.48f to -2f, 0.62f to 0f, 1f to 0f
)
private val VaporGlitchScaleY = listOf(
    0f to 1.08f, 0.12f to 0.98f, 0.24f to 1.02f, 0.36f to 1f, 0.48f to 0.99f, 0.62f to 1f, 1f to 1f
)
private val VaporGlitchSkew = listOf(
    0f to 0f, 0.12f to -5f, 0.24f to 0f, 0.36f to 2f, 0.48f to 0f, 1f to 0f
)
private val VaporGlitchFringe = listOf(
    0f to 8f, 0.12f to 6f, 0.24f to 4f, 0.36f to 3f, 0.48f to 2f, 0.62f to 0f, 1f to 0f
)
private val VaporGlitchFlash = listOf(
    0f to 0.6f, 0.06f to 0f, 0.24f to 0.32f, 0.29f to 0f, 1f to 0f
)
private val VaporGlitchBlow = listOf(
    0f to 0.35f, 0.12f to 0.2f, 0.24f to 0.45f, 0.36f to 0.1f, 0.62f to 0f, 1f to 0f
)

/** The exit runs the same glitch backwards. */
private val VaporOutDx = listOf(
    0f to 0f, 0.18f to 7f, 0.34f to -9f, 0.50f to 6f, 0.66f to -3f, 0.84f to 1f, 1f to 0f
)
private val VaporOutScaleY = listOf(
    0f to 1f, 0.18f to 1.05f, 0.34f to 0.96f, 0.50f to 1f, 0.66f to 1f, 0.84f to 1.01f, 1f to 1f
)
private val VaporOutSkew = listOf(
    0f to 0f, 0.18f to 0f, 0.34f to -4f, 0.50f to 0f, 0.66f to 0f, 1f to 0f
)
private val VaporOutFringe = listOf(
    0f to 0f, 0.18f to 5f, 0.34f to 9f, 0.50f to 6f, 0.66f to 4f, 0.84f to 2f, 1f to 14f
)
private val VaporOutFlash = listOf(
    0f to 0f, 0.30f to 0.2f, 0.44f to 0f, 0.78f to 0.5f, 1f to 0f
)
private val VaporOutBlow = listOf(
    0f to 0f, 0.18f to 0.3f, 0.34f to 0.5f, 0.50f to 0.25f, 0.84f to 0.1f, 1f to 0.3f
)

/* ------------------------------------------------------------------ */
/* The peaks either side of the sun                                     */
/* ------------------------------------------------------------------ */

private val VaporRidgeLeft = floatArrayOf(
    0f, 238f, 38f, 196f, 72f, 232f, 108f, 146f, 146f, 222f, 182f, 168f,
    224f, 246f, 262f, 182f, 304f, 238f, 344f, 202f, 382f, 252f, 418f, 300f
)
private val VaporRidgeRight = floatArrayOf(
    620f, 232f, 660f, 188f, 698f, 238f, 738f, 152f, 776f, 222f, 816f, 172f,
    858f, 244f, 898f, 198f, 948f, 248f, 1000f, 300f
)
private val VaporPeaksLeft = floatArrayOf(38f, 108f, 182f, 262f, 344f)
private val VaporPeaksRight = floatArrayOf(660f, 738f, 816f, 898f)

/** Plays a list of stepped poses backwards, so the exit mirrors the entrance. */
private fun reversedStops(stops: List<Pair<Float, Float>>): List<Pair<Float, Float>> {
    val flipped = stops.reversed()
    if (flipped.size < 2) return flipped
    val last = (flipped.size - 1).toFloat()
    return flipped.mapIndexed { i, (_, value) -> (i / last) to value }
}

/* ==================================================================== */
/* Card                                                                 */
/* ==================================================================== */

@Composable
fun CuratedVaporTicker(
    data: TickerData,
    interactions: TickerInteractions?,
    animateIn: Boolean,
    exitFraction: () -> Float
) {
    val context = LocalContext.current
    val opts = Prefs.themeOptions(context, "vapor", 0xFFFF71CE.toInt())
    val lines = opts.lines
    val intensity = opts.intensity
    val showTime = opts.showTime
    val showLogo = opts.showLogo
    val logoSize = opts.avatarSize.dp
    val scale = opts.buttonScale / 100f

    val hasDock = data.actions.isNotEmpty()
    val ex = exitFraction().coerceIn(0f, 1f)
    val leaving = ex > 0f

    val speed = rememberAnimationSpeed()
    val enter = remember { Animatable(if (animateIn) 0f else 1f) }
    LaunchedEffect(animateIn) {
        if (!animateIn) {
            enter.snapTo(1f)
            return@LaunchedEffect
        }
        enter.snapTo(0f)
        enter.animateTo(1f, tween(scaleDuration(VaporTimelineMs.toInt(), speed), easing = LinearEasing))
    }

    // The floor and the sun slats never stop.
    val gridPhase = rememberVaporPhase(850, 0f)
    val slatPhase = rememberVaporPhase(2600, 0f)
    var cardHeight by remember { mutableIntStateOf(0) }

    CardFrame(
        modifier = Modifier.vaporGlitch(enter, ex, leaving),
        lines = lines,
        hasDock = hasDock,
        // One line of text plus the dock; every extra line adds 15dp.
        baseHeight = 137.dp,
        dockSpace = 61.dp,
        decorationAlpha = intensity / 100f,
        shape = RoundedCornerShape(10.dp),
        background = Brush.linearGradient(listOf(Color.Transparent, Color.Transparent)),
        border = null
    ) {
        Box(Modifier.fillMaxSize().onSizeChanged { cardHeight = it.height }) {
            // The world behind the notification.
            Canvas(Modifier.fillMaxSize()) {
                val enterMs = enter.value * VaporTimelineMs
                val exitMs = ex * VaporExitMs

                // the sky
                drawRect(
                    Brush.radialGradient(
                        listOf(VaporPink.copy(alpha = 0.32f), Color.Transparent),
                        center = Offset(size.width * 0.5f, size.height * 0.62f),
                        radius = size.width * 0.34f
                    )
                )
                drawRect(
                    Brush.verticalGradient(
                        listOf(
                            VaporSky0, VaporSky1, VaporSky2,
                            VaporSky3, VaporSky4, VaporSky5
                        ),
                        startY = 0f,
                        endY = size.height
                    )
                )

                val horizon = size.height * 0.62f

                // the slatted sun, rising behind the peaks
                val sunR = size.height * 0.36f
                val sunCx = size.width * 0.5f
                val sunCy = size.height * 0.52f
                // a smooth ramp: the sun climbs out of the haze
                val raw = ((enterMs - 100f) / 1100f).coerceIn(0f, 1f)
                val sunIn = if (leaving) 1f else {
                    val u = 1f - (1f - raw) * (1f - raw) * (1f - raw)
                    u
                }
                if (sunIn > 0.01f) {
                    val slat = slatPhase.value
                    val r = sunR * (0.7f + 0.3f * sunIn)
                    val cy = sunCy + (1f - sunIn) * 70f * density
                    // the slats keep the sun's proportions as it grows
                    val period = (2f * r) / 8f
                    val slatH = period * 0.34f
                    val top = Color(0xFFFF6BD8)
                    val bottom = Color(0xFFC81FA8)
                    var y = -r + slat * period
                    while (y < r) {
                        // the horizon cuts the sun off — nothing shows below it
                        if (cy + y > horizon) break
                        val dy = y - cy
                        if (kotlin.math.abs(dy) < r) {
                            val half = kotlin.math.sqrt(r * r - dy * dy)
                            // the glass is lit from the top of the disc downwards
                            val t = ((cy + y) - (cy - r)) / (2f * r)
                            val col = Color(
                                red = top.red + (bottom.red - top.red) * t,
                                green = top.green + (bottom.green - top.green) * t,
                                blue = top.blue + (bottom.blue - top.blue) * t
                            )
                            drawRect(
                                color = col,
                                topLeft = Offset(sunCx - half, cy + y),
                                size = androidx.compose.ui.geometry.Size(half * 2f, slatH)
                            )
                        }
                        y += period
                    }
                }

                // the wireframe peaks
                val bandTop = horizon - size.height * 0.30f
                val bandH = size.height * 0.30f
                fun ridge(points: FloatArray, fromX: Float, toX: Float): Path {
                    val path = Path()
                    path.moveTo(fromX * size.width / 1000f, bandTop + bandH)
                    var i = 0
                    while (i < points.size) {
                        path.lineTo(
                            points[i] * size.width / 1000f,
                            bandTop + points[i + 1] / 300f * bandH
                        )
                        i += 2
                    }
                    path.lineTo(toX * size.width / 1000f, bandTop + bandH)
                    path.close()
                    return path
                }
                val leftRidge = ridge(VaporRidgeLeft, 0f, 418f)
                val rightRidge = ridge(VaporRidgeRight, 582f, 1000f)
                drawPath(leftRidge, VaporMount)
                drawPath(rightRidge, VaporMount)
                drawPath(
                    leftRidge,
                    VaporCyan.copy(alpha = 0.85f),
                    style = Stroke(width = 1.5.dp.toPx())
                )
                drawPath(
                    rightRidge,
                    VaporCyan.copy(alpha = 0.85f),
                    style = Stroke(width = 1.5.dp.toPx())
                )
                // the facet lines dropping from each peak
                listOf(VaporPeaksLeft to leftRidge, VaporPeaksRight to rightRidge)
                    .forEach { (peaks, path) ->
                        var i = 0
                        while (i < peaks.size) {
                            val x = peaks[i] * size.width / 1000f
                            drawLine(
                                color = VaporCyan.copy(alpha = 0.35f),
                                start = Offset(x, path.getBounds().top + (path.getBounds().height * 0.2f)),
                                end = Offset(x, bandTop + bandH),
                                strokeWidth = 1.dp.toPx()
                            )
                            i++
                        }
                    }

                // the horizon
                drawRect(
                    brush = Brush.horizontalGradient(
                        listOf(
                            Color.Transparent,
                            VaporPink.copy(alpha = 0.9f),
                            VaporCyan.copy(alpha = 0.9f),
                            Color.Transparent
                        ),
                        startX = 0f,
                        endX = size.width
                    ),
                    topLeft = Offset(0f, horizon - 0.5f),
                    size = androidx.compose.ui.geometry.Size(size.width, 1f)
                )

                // the grid floor, rushing forward
                val gridOut = if (leaving) {
                    ((exitMs / 600f).coerceIn(0f, 1f))
                } else {
                    0f
                }
                if (gridOut < 1f) {
                    val vanish = Offset(size.width * 0.5f, horizon)
                    // the lines running to the horizon
                    var k = -7
                    while (k <= 7) {
                        drawLine(
                            color = VaporCyan.copy(alpha = (0.5f - 0.02f * kotlin.math.abs(k)) * (1f - gridOut)),
                            start = vanish,
                            end = Offset(
                                vanish.x + k * size.width * 0.16f,
                                size.height + 30f * gridOut
                            ),
                            strokeWidth = 1.dp.toPx()
                        )
                        k++
                    }
                    // the rungs, spreading toward the viewer
                    val phase = gridPhase.value
                    var i = 0
                    while (i < 14) {
                        val t = ((i + phase) / 14f) % 1f
                        val y = horizon + (size.height - horizon) * (t * t)
                        drawLine(
                            color = VaporCyan.copy(alpha = (0.45f - 0.2f * t) * (1f - gridOut)),
                            start = Offset(0f, y),
                            end = Offset(size.width, y),
                            strokeWidth = 1.dp.toPx()
                        )
                        i++
                    }
                }

                // the chromatic fringe and the colour blow-out
                val glitchP = if (leaving) {
                    ((exitMs - 150f) / 750f).coerceIn(0f, 1f)
                } else {
                    (enterMs / VaporGlitchMs).coerceIn(0f, 1f)
                }
                val fringe = stepped(
                    if (leaving) glitchP else glitchP,
                    if (leaving) VaporOutFringe else VaporGlitchFringe
                ) * density
                if (fringe > 0.5f) {
                    drawRect(
                        color = VaporCyan.copy(alpha = 0.55f),
                        topLeft = Offset(-fringe, 0f),
                        size = androidx.compose.ui.geometry.Size(fringe * 1.6f, size.height)
                    )
                    drawRect(
                        color = VaporRed.copy(alpha = 0.55f),
                        topLeft = Offset(size.width - fringe * 0.6f, 0f),
                        size = androidx.compose.ui.geometry.Size(fringe * 1.6f, size.height)
                    )
                }
                val blow = stepped(
                    glitchP,
                    if (leaving) VaporOutBlow else VaporGlitchBlow
                )
                if (blow > 0.01f) {
                    drawRect(Color(0xFFFF3EC8).copy(alpha = 0.20f * blow))
                    drawRect(Color(0xFF00E5FF).copy(alpha = 0.13f * blow))
                }
            }

            // The tear bands that rip across the notification.
            VaporTears(
                enter = enter,
                ex = ex,
                leaving = leaving,
                cardHeight = cardHeight
            )

            // The note.
            Column(
                Modifier
                    .align(Alignment.TopStart)
                    .padding(start = 22.dp, end = 22.dp, top = 30.dp)
                    .fillMaxWidth()
                    .graphicsLayer {
                        val enterMs = enter.value * VaporTimelineMs
                        val local = ((enterMs - 520f) / 550f).coerceIn(0f, 1f)
                        if (leaving) {
                            val out = (ex * VaporExitMs / 300f).coerceIn(0f, 1f)
                            alpha = 1f - out
                            translationY = -8f * out
                        } else {
                            alpha = stepped(
                                local,
                                listOf(0f to 0f, 0.35f to 1f, 1f to 1f)
                            )
                        }
                    }
            ) {
                VaporSenderRow(
                    data = data,
                    showTime = showTime,
                    showLogo = showLogo,
                    logoSize = logoSize,
                    enter = enter,
                    ex = ex,
                    leaving = leaving
                )
                Spacer(Modifier.height(5.dp))
                CardBody(
                    text = data.body,
                    lines = lines,
                    color = Color(0xFFF6E9FF),
                    font = ThemeFonts.SpaceMono,
                    weight = FontWeight.Normal,
                    sizeSp = 11.5f,
                    lineHeightFactor = 15f / 11.5f,
                    style = TextStyle(shadow = Shadow(Color(0xE0460A3C), Offset(0f, 1f), 5f))
                )
            }

            if (hasDock) {
                VaporDock(
                    data = data,
                    interactions = interactions,
                    scale = scale,
                    enter = enter,
                    ex = ex,
                    leaving = leaving,
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(start = 22.dp, end = 22.dp, bottom = 18.dp)
                )
            }
        }
    }
}

/** An endless clock for the floor and the sun slats. */
@Composable
private fun rememberVaporPhase(periodMs: Int, offset: Float): State<Float> {
    val speed = rememberAnimationSpeed()
    val scaled = scaleDuration(periodMs, speed)
    val transition = rememberInfiniteTransition(label = "vapor")
    return transition.animateFloat(
        initialValue = offset,
        targetValue = offset + 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(scaled, easing = LinearEasing)
        ),
        label = "phase"
    )
}

/* ------------------------------------------------------------------ */
/* The glitch                                                           */
/* ------------------------------------------------------------------ */

/** The card tears sideways, colour splits and blows out. */
private fun Modifier.vaporGlitch(
    enter: Animatable<Float, *>,
    ex: Float,
    leaving: Boolean
): Modifier = graphicsLayer {
    val enterMs = enter.value * VaporTimelineMs
    val exitMs = ex * VaporExitMs
    if (leaving) {
        val p = ((exitMs - 150f) / 750f).coerceIn(0f, 1f)
        translationX = stepped(p, VaporOutDx) * density
        scaleY = stepped(p, VaporOutScaleY)
        transformOrigin = androidx.compose.ui.graphics.TransformOrigin(0.5f, 0.5f)
        alpha = if (p >= 1f) 0f else 1f
    } else {
        val p = (enterMs / VaporGlitchMs).coerceIn(0f, 1f)
        translationX = stepped(p, VaporGlitchDx) * density
        scaleY = stepped(p, VaporGlitchScaleY)
        transformOrigin = androidx.compose.ui.graphics.TransformOrigin(0.5f, 0.5f)
        alpha = if (p <= 0f) 0f else 1f
    }
}

/** The tear bands that rip across the notification. */
@Composable
private fun BoxScope.VaporTears(
    enter: Animatable<Float, *>,
    ex: Float,
    leaving: Boolean,
    cardHeight: Int
) {
    val bands = listOf(
        Triple(
            4.dp,
            listOf(0f to 18f, 0.10f to 34f, 0.27f to 12f, 0.45f to 52f),
            listOf(0f to -30f, 0.10f to 24f, 0.27f to -18f, 0.45f to 30f)
        ),
        Triple(
            10.dp,
            listOf(0f to 62f, 0.11f to 44f, 0.29f to 70f, 0.47f to 26f),
            listOf(0f to 0f, 0.11f to -34f, 0.29f to 28f, 0.47f to -16f)
        ),
        Triple(
            6.dp,
            listOf(0f to 8f, 0.13f to 24f, 0.31f to 4f, 0.49f to 40f),
            listOf(0f to 0f, 0.13f to 22f, 0.31f to -26f, 0.49f to 18f)
        )
    )
    bands.forEachIndexed { index, (height, tops, offsets) ->
        Box(
            Modifier
                .align(Alignment.TopStart)
                .fillMaxWidth()
                .height(height)
                .graphicsLayer {
                    val enterMs = enter.value * VaporTimelineMs
                    val exitMs = ex * VaporExitMs
                    val p = if (leaving) {
                        ((exitMs - 100f - index * 60f) / 550f).coerceIn(0f, 1f)
                    } else {
                        ((enterMs - 40f - index * 30f) / 550f).coerceIn(0f, 1f)
                    }
                    val topFrac = if (leaving) reversedStops(tops) else tops
                    val offFrac = if (leaving) reversedStops(offsets) else offsets
                    translationY = stepped(p, topFrac) / 100f * cardHeight
                    translationX = stepped(p, offFrac) * density
                    alpha = if (p <= 0f || p >= 1f) 0f else 1f
                }
                .background(
                    Brush.horizontalGradient(
                        listOf(
                            Color.Transparent,
                            VaporCyan.copy(alpha = 0.9f),
                            VaporRed.copy(alpha = 0.9f),
                            VaporCyan.copy(alpha = 0.7f),
                            Color.Transparent
                        ),
                        startX = 0f,
                        endX = Float.POSITIVE_INFINITY
                    )
                )
        )
    }

    // The flash.
    Box(
        Modifier
            .align(Alignment.TopStart)
            .fillMaxWidth()
            .fillMaxSize()
            .graphicsLayer {
                val enterMs = enter.value * VaporTimelineMs
                val exitMs = ex * VaporExitMs
                val p = if (leaving) {
                    ((exitMs - 150f) / 750f).coerceIn(0f, 1f)
                } else {
                    (enterMs / VaporGlitchMs).coerceIn(0f, 1f)
                }
                alpha = if (leaving) {
                    stepped(p, VaporOutFlash)
                } else {
                    stepped(p, VaporGlitchFlash)
                }
            }
            .background(Color.White)
    )
}

/* ------------------------------------------------------------------ */
/* The note                                                             */
/* ------------------------------------------------------------------ */

@Composable
private fun VaporSenderRow(
    data: TickerData,
    showTime: Boolean,
    showLogo: Boolean,
    logoSize: Dp,
    enter: Animatable<Float, *>,
    ex: Float,
    leaving: Boolean
) {
    Row(
        Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (showLogo) {
            Box(
                Modifier
                    .size(25.dp)
                    .clip(RoundedCornerShape(50.dp))
                    .background(
                        Brush.radialGradient(
                            listOf(Color.White, Color(0xFFFFD9F2), VaporPink, Color(0xFF7A2B8F))
                        )
                    )
                    .border(1.5.dp, Color.White.copy(alpha = 0.5f), RoundedCornerShape(50.dp)),
                contentAlignment = Alignment.Center
            ) {
                CardIcon(
                    data = data,
                    size = logoSize.coerceIn(12.dp, 16.dp),
                    shape = RoundedCornerShape(50.dp),
                    fallbackBg = Color.Transparent,
                    fallbackFg = Color(0xFF3A1152)
                )
            }
            Spacer(Modifier.width(10.dp))
        }
        // The sender, split into cyan and red copies that converge.
        VaporSplitText(
            text = data.title,
            enter = enter,
            ex = ex,
            leaving = leaving,
            fontSize = 17.sp,
            fontFamily = ThemeFonts.Righteous,
            letterSpacing = 1.sp,
            modifier = Modifier.weight(1f)
        )
        if (showTime) {
            Spacer(Modifier.width(10.dp))
            Text(
                data.time.uppercase(),
                color = VaporPink,
                fontSize = 9.5.sp,
                fontFamily = ThemeFonts.Nunito,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.4.sp,
                maxLines = 1,
                style = TextStyle(shadow = Shadow(VaporPink.copy(alpha = 0.8f), Offset.Zero, 10f))
            )
        }
    }
}

/** The same glyph three times: cyan left, red right, white on top. */
@Composable
private fun VaporSplitText(
    text: String,
    enter: Animatable<Float, *>,
    ex: Float,
    leaving: Boolean,
    fontSize: TextUnit,
    fontFamily: FontFamily,
    letterSpacing: TextUnit,
    modifier: Modifier = Modifier
) {
    val split = Modifier.graphicsLayer {
        val enterMs = enter.value * VaporTimelineMs
        val local = ((enterMs - 520f) / 550f).coerceIn(0f, 1f)
        val out = (ex * VaporExitMs / 300f).coerceIn(0f, 1f)
        if (leaving) {
            translationX = 13f * out * density
        } else {
            translationX = stepped(
                local,
                listOf(0f to 11f, 0.35f to 5f, 0.70f to 2.4f, 1f to 1.6f)
            ) * density
        }
    }
    Box(modifier) {
        Text(
            text,
            color = VaporCyan.copy(alpha = 0.85f),
            fontSize = fontSize,
            fontFamily = fontFamily,
            fontWeight = FontWeight.Bold,
            letterSpacing = letterSpacing,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.offset(x = (-1.6f).dp).then(split)
        )
        Text(
            text,
            color = VaporRed.copy(alpha = 0.85f),
            fontSize = fontSize,
            fontFamily = fontFamily,
            fontWeight = FontWeight.Bold,
            letterSpacing = letterSpacing,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.offset(x = 1.6f.dp).then(split)
        )
        Text(
            text,
            color = VaporInk,
            fontSize = fontSize,
            fontFamily = fontFamily,
            fontWeight = FontWeight.Bold,
            letterSpacing = letterSpacing,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .graphicsLayer {
                    val enterMs = enter.value * VaporTimelineMs
                    val local = ((enterMs - 520f) / 550f).coerceIn(0f, 1f)
                    val out = (ex * VaporExitMs / 300f).coerceIn(0f, 1f)
                    alpha = if (leaving) 1f - out else stepped(local, listOf(0f to 0f, 0.35f to 1f, 1f to 1f))
                    translationX = if (leaving) 0f else 0f
                }
        )
    }
}

/* ------------------------------------------------------------------ */
/* The dock: chrome pills                                               */
/* ------------------------------------------------------------------ */

@Composable
private fun BoxScope.VaporDock(
    data: TickerData,
    interactions: TickerInteractions?,
    scale: Float,
    enter: Animatable<Float, *>,
    ex: Float,
    leaving: Boolean,
    modifier: Modifier = Modifier
) {
    val specs = notificationActionSpecs(data, interactions)
    if (specs.isEmpty()) return

    Row(
        modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(9.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically
    ) {
        specs.forEach { spec ->
            val source = remember { MutableInteractionSource() }
            val pressed by source.collectIsPressedAsState()
            Box(
                Modifier
                    .height(31.dp)
                    .graphicsLayer {
                        val enterMs = enter.value * VaporTimelineMs
                        val local = ((enterMs - 800f) / 500f).coerceIn(0f, 1f)
                        if (leaving) {
                            val out = (ex * VaporExitMs / 300f).coerceIn(0f, 1f)
                            alpha = 1f - out
                            translationY = -8f * out
                        } else {
                            alpha = stepped(local, listOf(0f to 0f, 0.60f to 1f, 1f to 1f))
                            translationY = 16f * (1f - stepped(local, listOf(0f to 0f, 0.60f to 1f, 1f to 1f)))
                            scaleX = 0.86f + 0.14f * stepped(local, listOf(0f to 0f, 1f to 1f))
                            scaleY = scaleX
                        }
                        if (pressed) scaleX = 0.97f
                    }
                    .clip(RoundedCornerShape(999.dp))
                    .background(
                        Brush.verticalGradient(
                            listOf(Color(0xFFF3F0FF), Color(0xFFB9B2D8), Color(0xFF8F86B8))
                        )
                    )
                    .border(1.5.dp, VaporPink, RoundedCornerShape(999.dp))
                    .drawBehind {
                        // the highlight along the top
                        drawLine(
                            color = Color.White.copy(alpha = 0.8f),
                            start = Offset(6.dp.toPx(), 2.dp.toPx()),
                            end = Offset(size.width - 6.dp.toPx(), 2.dp.toPx()),
                            strokeWidth = 1.dp.toPx()
                        )
                    }
                    .clickable(
                        interactionSource = source,
                        indication = null,
                        enabled = spec.onClick != null
                    ) { spec.onClick?.invoke() }
                    .padding(horizontal = 15.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    spec.label.uppercase(),
                    color = Color(0xFF3A1152),
                    fontSize = (9f * scale).sp,
                    fontFamily = ThemeFonts.Orbitron,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.6.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}
