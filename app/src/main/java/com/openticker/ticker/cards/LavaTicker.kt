package com.openticker.ticker.cards

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
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
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.openticker.data.Prefs
import com.openticker.data.TickerData
import com.openticker.data.TickerInteractions
import com.openticker.ticker.styles.notificationActionSpecs

/* ==================================================================== */
/* 10 — Lava Lamp                                                       */
/* ==================================================================== */
/*
 * A lamp on the desk beside the note. The foot rises, the cap drops on,
 * the liquid pours into the vessel, the filament flares and the wax
 * starts climbing. Leaving, the wax sinks, the lamp drains and is
 * packed away again.
 *
 * The palette is fixed: this theme has no colour picker.
 */

private val LavaRoomTop = Color(0xFF1C0A2C)
private val LavaRoomMid = Color(0xFF2A0E42)
private val LavaRoomLow = Color(0xFF3A1030)
private val LavaInk = Color(0xFFFFE9D2)
private val LavaInkSoft = Color(0xFFF3C7A6)
private val LavaAmber = Color(0xFFFFB36B)
private val LavaGlassTop = Color(0xFF6E1352)
private val LavaGlassLow = Color(0xFFC03A2A)
private val LavaBrass = Color(0xFFD2A855)
private val LavaChrome = Color(0xFFE9C978)

/** The whole choreography, in milliseconds. */
private const val LavaTimelineMs = 1900f

/** The way out. */
private const val LavaExitMs = 1000f

/** Each blob and each bubble runs on a clock of its own. */
private val LavaWaxPeriods = listOf(6500f, 8500f, 10500f)
private val LavaBubblePeriods = listOf(6000f, 7500f, 8500f)

/* ------------------------------------------------------------------ */
/* Easing                                                              */
/* ------------------------------------------------------------------ */

private fun easeOut(t: Float): Float {
    val x = t.coerceIn(0f, 1f)
    val f = 1f - x
    return 1f - f * f * f
}

private fun easeBack(t: Float): Float {
    val x = t.coerceIn(0f, 1f)
    val c1 = 1.70158f
    val c3 = c1 + 1f
    val u = x - 1f
    return 1f + c3 * u * u * u + c1 * u * u
}

/** Where the wax is in its cycle: the height it has reached, and its size. */
private fun lavaRise(phase: Float): Pair<Float, Float> {
    val p = ((phase % 1f) + 1f) % 1f
    return when {
        p < 0.42f -> {
            val k = easeOut(p / 0.42f)
            (0.02f + (0.76f - 0.02f) * k) to (1f + 0.06f * k)
        }
        p < 0.58f -> {
            val k = (p - 0.42f) / 0.16f
            (0.76f + 0.02f * k) to 1.06f
        }
        p < 0.62f -> 0.78f to 1.05f
        p < 0.70f -> {
            val k = (p - 0.62f) / 0.08f
            (0.78f - 0.76f * k) to (1.05f - 0.15f * k)
        }
        else -> 0.02f to 1f
    }
}

/* ==================================================================== */
/* Card                                                                 */
/* ==================================================================== */

@Composable
fun CuratedLavaTicker(
    data: TickerData,
    interactions: TickerInteractions?,
    animateIn: Boolean,
    exitFraction: () -> Float
) {
    val context = LocalContext.current
    val opts = Prefs.themeOptions(context, "lava", 0xFFFFB36B.toInt())
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
        enter.animateTo(1f, tween(scaleDuration(LavaTimelineMs.toInt(), speed), easing = LinearEasing))
    }

    // The wax runs on its own clocks.
    val waxPhases = listOf(
        rememberLavaPhase(6500, 0f),
        rememberLavaPhase(8500, -2400f),
        rememberLavaPhase(10500, -5200f)
    )
    val bubblePhases = listOf(
        rememberLavaPhase(6000, 0f),
        rememberLavaPhase(7500, -2600f),
        rememberLavaPhase(8500, -5100f)
    )

    CardFrame(
        modifier = Modifier.lavaRoom(enter, ex, leaving),
        lines = lines,
        hasDock = hasDock,
        // One line of text plus the dock; every extra line adds 16dp.
        baseHeight = 125.dp,
        dockSpace = 61.dp,
        decorationAlpha = intensity / 100f,
        shape = RoundedCornerShape(22.dp),
        background = Brush.linearGradient(listOf(Color.Transparent, Color.Transparent)),
        border = null
    ) {
        Box(Modifier.fillMaxSize()) {
            // The room, and the halo the lamp throws on the wall.
            Canvas(Modifier.fillMaxSize()) {
                drawRect(
                    Brush.radialGradient(
                        listOf(Color(0x42FF8A36), Color.Transparent),
                        center = Offset(size.width * 0.86f, size.height * 0.98f),
                        radius = size.width * 0.46f
                    )
                )
                drawRect(
                    Brush.radialGradient(
                        listOf(Color(0x381E3CA0), Color.Transparent),
                        center = Offset(size.width * 0.08f, size.height * 0.04f),
                        radius = size.width * 0.78f
                    )
                )
                drawRect(
                    Brush.linearGradient(
                        listOf(LavaRoomTop, LavaRoomMid, LavaRoomLow),
                        start = Offset.Zero,
                        end = Offset(size.width * 0.6f, size.height)
                    )
                )
            }

            // The lamp.
            Box(
                Modifier
                    .align(Alignment.CenterEnd)
                    .fillMaxHeight()
                    .width(108.dp)
                    .padding(top = 10.dp, bottom = 10.dp)
            ) {
                Column(
                    Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    LavaCap(enter = enter, ex = ex, leaving = leaving)
                    LavaGlass(
                        enter = enter,
                        ex = ex,
                        leaving = leaving,
                        waxPhases = waxPhases,
                        bubblePhases = bubblePhases,
                        modifier = Modifier
                            .weight(1f)
                            .width(76.dp)
                    )
                    LavaBase(enter = enter, ex = ex, leaving = leaving)
                }
            }

            // The note.
            Column(
                Modifier
                    .align(Alignment.TopStart)
                    .padding(start = 20.dp, top = 17.dp)
                    .fillMaxWidth(0.58f)
                    .graphicsLayer {
                        val enterMs = enter.value * LavaTimelineMs
                        val local = ((enterMs - 720f) / 600f).coerceIn(0f, 1f)
                        alpha = if (leaving) {
                            (1f - (ex * LavaExitMs) / 250f).coerceIn(0f, 1f)
                        } else {
                            easeOut(local)
                        }
                        translationY = if (leaving) {
                            0f
                        } else {
                            8f * (1f - easeOut(local))
                        }
                    }
            ) {
                LavaSenderRow(
                    data = data,
                    showTime = showTime,
                    showLogo = showLogo,
                    logoSize = logoSize
                )
                Spacer(Modifier.height(5.dp))
                CardBody(
                    text = data.body,
                    lines = lines,
                    color = LavaInkSoft,
                    font = ThemeFonts.Nunito,
                    weight = FontWeight.Bold,
                    sizeSp = 12f,
                    lineHeightFactor = 1.3f
                )
            }

            if (hasDock) {
                LavaDock(
                    data = data,
                    interactions = interactions,
                    scale = scale,
                    enter = enter,
                    ex = ex,
                    leaving = leaving,
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(start = 20.dp, end = 20.dp, bottom = 16.dp)
                )
            }
        }
    }
}

/** An endless clock for the wax and the bubbles. */
@Composable
private fun rememberLavaPhase(periodMs: Int, offsetMs: Float): State<Float> {
    val speed = rememberAnimationSpeed()
    val scaled = scaleDuration(periodMs, speed)
    val transition = rememberInfiniteTransition(label = "lava-phase")
    return transition.animateFloat(
        initialValue = offsetMs.toFloat(),
        targetValue = offsetMs + scaled.toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(scaled, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "lava"
    )
}

/* ------------------------------------------------------------------ */
/* The lamp                                                             */
/* ------------------------------------------------------------------ */

@Composable
private fun LavaCap(enter: Animatable<Float, *>, ex: Float, leaving: Boolean) {
    Box(
        Modifier
            .size(width = 40.dp, height = 15.dp)
            .graphicsLayer {
                val enterMs = enter.value * LavaTimelineMs
                val exitMs = ex * LavaExitMs
                if (leaving) {
                    val k = ((exitMs - 300f) / 500f).coerceIn(0f, 1f)
                    translationY = -190f * k * density
                    alpha = 1f - k
                } else {
                    val k = ((enterMs - 300f) / 550f).coerceIn(0f, 1f)
                    translationY = -190f * (1f - easeBack(k)) * density
                    alpha = if (k <= 0f) 0f else 1f
                }
            }
            .clip(RoundedCornerShape(topStart = 3.dp, topEnd = 3.dp, bottomStart = 10.dp, bottomEnd = 10.dp))
            .background(
                Brush.horizontalGradient(
                    listOf(
                        Color(0xFF5E4414), LavaChrome, Color(0xFFFFF6D2),
                        Color(0xFFB98C33), Color(0xFF45320C)
                    )
                )
            )
            .drawBehind {
                // the ribs
                var x = 0f
                val step = 5.dp.toPx()
                while (x < size.width) {
                    drawLine(
                        Color.Black.copy(alpha = 0.14f),
                        Offset(x, 0f),
                        Offset(x, size.height),
                        strokeWidth = 1.dp.toPx()
                    )
                    x += step
                }
            }
    ) {
        // the finial
        Box(
            Modifier
                .align(Alignment.TopCenter)
                .offset(y = (-8).dp)
                .size(width = 10.dp, height = 9.dp)
                .clip(RoundedCornerShape(50.dp, 50.dp, 38.dp, 38.dp))
                .background(
                    Brush.horizontalGradient(listOf(Color(0xFFFBF0C0), Color(0xFF7E5A1C)))
                )
        )
    }
}

@Composable
private fun LavaGlass(
    enter: Animatable<Float, *>,
    ex: Float,
    leaving: Boolean,
    waxPhases: List<State<Float>>,
    bubblePhases: List<State<Float>>,
    modifier: Modifier = Modifier
) {
    Box(
        modifier
            .clip(
                RoundedCornerShape(
                    topStart = 38.dp,
                    topEnd = 38.dp,
                    bottomStart = 12.dp,
                    bottomEnd = 12.dp
                )
            )
            .background(
                Brush.verticalGradient(
                    listOf(Color(0x577EC0AA), Color(0x8C260C3A))
                )
            )
    ) {
        Canvas(Modifier.fillMaxSize()) {
            val enterMs = enter.value * LavaTimelineMs
            val exitMs = ex * LavaExitMs

            // the liquid pours in, and drains away again
            val fillP = if (leaving) {
                1f - ((exitMs - 200f) / 600f).coerceIn(0f, 1f)
            } else {
                ((enterMs - 500f) / 750f).coerceIn(0f, 1f)
            }
            val fill = (easeOut(fillP) * 0.02f + fillP * 0.98f).coerceIn(0.02f, 1f)
            scale(1f, fill, pivot = Offset(size.width / 2f, size.height)) {
                drawRect(
                    Brush.radialGradient(
                        listOf(Color(0x59FF963C), Color.Transparent),
                        center = Offset(size.width / 2f, size.height),
                        radius = size.width * 0.7f
                    )
                )
                drawRect(
                    Brush.verticalGradient(
                        listOf(Color(0xFF4A1060), Color(0xFF7E1352), LavaGlassLow),
                        startY = 0f,
                        endY = size.height
                    )
                )
            }

            // the wax
            val waxes = listOf(
                Triple(0.03f, 42f, listOf(Color(0xFFFFE0B4), Color(0xFFFF8A3D), Color(0xFFE24E1E))),
                Triple(0.41f, 32f, listOf(Color(0xFFFFC48C), Color(0xFFFF6A2A), Color(0xFFD63F14))),
                Triple(0.67f, 23f, listOf(Color(0xFFFFE9C8), Color(0xFFFF9A4C), Color(0xFFE05A20)))
            )
            waxes.forEachIndexed { index, (xFrac, dp, colors) ->
                val d = dp.dp.toPx()
                val phase = waxPhases[index].value
                val (bottomFrac, grow) = lavaRise(phase / LavaWaxPeriods[index])
                var y = size.height * (1f - bottomFrac) - d
                var alpha = 1f
                if (leaving) {
                    val k = (exitMs / 600f).coerceIn(0f, 1f)
                    y = size.height * (1f - (0.70f - 0.68f * k)) - d
                    alpha = 1f - k
                }
                val cx = size.width * xFrac + d / 2f
                val cy = y + d / 2f
                // the glow
                drawCircle(
                    color = Color(0x8CFF8C3C),
                    radius = d * 0.75f,
                    center = Offset(cx, cy),
                    alpha = alpha
                )
                drawCircle(
                    brush = Brush.radialGradient(
                        colors,
                        center = Offset(cx - d * 0.16f, cy - d * 0.22f),
                        radius = d * 0.72f
                    ),
                    radius = d * 0.5f * grow,
                    center = Offset(cx, cy),
                    alpha = alpha
                )
            }

            // the bubbles
            bubblePhases.forEachIndexed { index, phase ->
                val p = (((phase.value / LavaBubblePeriods[index]) % 1f) + 1f) % 1f
                val travel = (p / 0.82f).coerceIn(0f, 1f)
                val alpha = if (p < 0.16f) (p / 0.16f) * 0.85f else (1f - travel) * 0.85f
                val x = size.width * listOf(0.22f, 0.52f, 0.72f)[index]
                val y = size.height * 0.92f - travel * size.height * 0.9f
                drawCircle(
                    color = Color(0x99FFCE9E),
                    radius = 2.dp.toPx(),
                    center = Offset(x, y),
                    alpha = alpha
                )
            }

            // the heat climbing the glass
            if (!leaving) {
                val k = ((enterMs - 750f) / 1100f).coerceIn(0f, 1f)
                if (k > 0f && k < 1f) {
                    val rise = (0.60f - 0.90f * k) * size.height
                    val alpha = if (k < 0.4f) (k / 0.4f) * 0.9f else (1f - (k - 0.4f) / 0.6f) * 0.9f
                    drawRect(
                        brush = Brush.verticalGradient(
                            listOf(
                                Color(0x8CFFB45A), Color(0x2EFF8C3C), Color.Transparent
                            ),
                            startY = rise,
                            endY = rise + size.height * 0.9f
                        ),
                        alpha = alpha
                    )
                }
            }
        }

        // the highlight down the glass
        Box(
            Modifier
                .align(Alignment.TopStart)
                .offset(x = 9.dp, y = 12.dp)
                .width(9.dp)
                .fillMaxHeight(0.82f)
                .clip(RoundedCornerShape(5.dp))
                .background(
                    Brush.verticalGradient(
                        listOf(Color(0x73FFFFFF), Color(0x0AFFFFFF))
                    )
                )
        )

        // the metal collars
        Box(
            Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .height(7.dp)
                .clip(RoundedCornerShape(topStart = 38.dp, topEnd = 38.dp))
                .background(
                    Brush.verticalGradient(
                        listOf(Color(0xFFF0D692), Color(0xFFA87C2C), Color(0xFF4A3510))
                    )
                )
        )
        Box(
            Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .height(9.dp)
                .clip(RoundedCornerShape(bottomStart = 12.dp, bottomEnd = 12.dp))
                .background(
                    Brush.verticalGradient(
                        listOf(Color(0xFFF0D692), Color(0xFFA87C2C), Color(0xFF4A3510))
                    )
                )
        )

        // a little condensation
        listOf(
            Triple(0.22f, 0.26f, 2f),
            Triple(0.68f, 0.44f, 3f),
            Triple(0.40f, 0.62f, 2f),
            Triple(0.79f, 0.18f, 2f)
        ).forEach { (x, y, d) ->
            Box(
                Modifier
                    .align(Alignment.TopStart)
                    .offset(x = sizeWidth * x, y = sizeHeight * y)
                    .size(d.dp)
                    .clip(RoundedCornerShape(50.dp))
                    .background(Color.White.copy(alpha = 0.28f))
            )
        }
    }
}

/* The glass is a fixed size inside the card, so the specks use fixed offsets. */
private val sizeWidth = 76.dp
private val sizeHeight = 110.dp

@Composable
private fun LavaBase(enter: Animatable<Float, *>, ex: Float, leaving: Boolean) {
    Box(
        Modifier
            .size(width = 94.dp, height = 30.dp)
            .graphicsLayer {
                val enterMs = enter.value * LavaTimelineMs
                val exitMs = ex * LavaExitMs
                if (leaving) {
                    val k = ((exitMs - 420f) / 500f).coerceIn(0f, 1f)
                    translationY = 190f * k * density
                    scaleY = 1f - 0.3f * k
                    alpha = 1f - k
                } else {
                    val k = ((enterMs) / 600f).coerceIn(0f, 1f)
                    translationY = 190f * (1f - easeBack(k)) * density
                    scaleY = 0.7f + 0.3f * easeBack(k)
                    alpha = if (k <= 0f) 0f else 1f
                }
            }
    ) {
        Canvas(Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            val shape = Path().apply {
                moveTo(w * 0.15f, 0f)
                lineTo(w * 0.85f, 0f)
                lineTo(w, h * 0.68f)
                lineTo(w * 0.95f, h)
                lineTo(w * 0.05f, h)
                lineTo(0f, h * 0.68f)
                close()
            }
            clipPath(shape) {
                drawRect(
                    Brush.verticalGradient(
                        listOf(
                            Color(0xFFF6E1AC), Color(0xFFD2A855),
                            Color(0xFF96692A), Color(0xFF4C3411)
                        ),
                        startY = 0f,
                        endY = h
                    )
                )
                drawRect(
                    Brush.linearGradient(
                        0f to Color.Transparent,
                        1f to Color.Black.copy(alpha = 0.14f),
                        7f to Color.Transparent,
                        start = Offset.Zero,
                        end = Offset(7.dp.toPx(), 0f),
                        tileMode = TileMode.Repeated
                    )
                )
            }
            // the collar under the glass
            drawRect(
                brush = Brush.verticalGradient(
                    listOf(Color(0xFFFFF6D6), Color(0xFFC08F32)),
                    startY = 0f,
                    endY = 4.dp.toPx()
                ),
                topLeft = Offset(w * 0.13f, 0f),
                size = androidx.compose.ui.geometry.Size(w * 0.74f, 4.dp.toPx())
            )
        }
        // the filament
        Box(
            Modifier
                .align(Alignment.TopCenter)
                .offset(y = (-2).dp)
                .fillMaxWidth(0.7f)
                .height(5.dp)
                .clip(RoundedCornerShape(3.dp))
                .background(Color(0xFFFFE9B8))
                .graphicsLayer {
                    val enterMs = enter.value * LavaTimelineMs
                    val exitMs = ex * LavaExitMs
                    if (leaving) {
                        val k = (exitMs / 350f).coerceIn(0f, 1f)
                        alpha = 1f - 0.85f * k
                        scaleX = 1f - 0.5f * k
                    } else {
                        val k = ((enterMs - 600f) / 900f).coerceIn(0f, 1f)
                        alpha = if (k <= 0f) 0.2f else if (k < 0.35f) {
                            0.2f + 0.8f * (k / 0.35f)
                        } else {
                            1f
                        }
                        scaleX = if (k <= 0f) 0.3f else if (k < 0.35f) {
                            0.3f + 0.85f * (k / 0.35f)
                        } else {
                            1f - 0.15f * ((k - 0.35f) / 0.65f)
                        }
                    }
                }
        )
        // the rocker switch
        Box(
            Modifier
                .align(Alignment.BottomEnd)
                .offset(x = (-12).dp, y = (-6).dp)
                .size(width = 13.dp, height = 7.dp)
                .graphicsLayer { rotationZ = -8f }
                .clip(RoundedCornerShape(2.dp))
                .background(Brush.verticalGradient(listOf(Color(0xFF6E5326), Color(0xFF2E2110))))
                .border(0.5.dp, Color(0x33FFE6AA), RoundedCornerShape(2.dp))
        )
    }
}

/* ------------------------------------------------------------------ */
/* The note                                                             */
/* ------------------------------------------------------------------ */

@Composable
private fun LavaSenderRow(
    data: TickerData,
    showTime: Boolean,
    showLogo: Boolean,
    logoSize: Dp
) {
    Row(
        Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (showLogo) {
            Box(
                Modifier
                    .size(26.dp)
                    .clip(RoundedCornerShape(50.dp))
                    .background(
                        with(LocalDensity.current) {
                            Brush.radialGradient(
                                listOf(Color(0xFFFFE0B4), Color(0xFFFF8A3D), Color(0xFFC24E17)),
                                center = Offset(9.dp.toPx(), 7.dp.toPx()),
                                radius = 22.dp.toPx()
                            )
                        }
                    )
                    .border(1.5.dp, Color(0x994A1500), RoundedCornerShape(50.dp)),
                contentAlignment = Alignment.Center
            ) {
                CardIcon(
                    data = data,
                    size = logoSize.coerceIn(14.dp, 18.dp),
                    shape = RoundedCornerShape(50.dp),
                    fallbackBg = Color.Transparent,
                    fallbackFg = Color(0xFF4A1500)
                )
            }
            Spacer(Modifier.width(9.dp))
        }
        Text(
            data.title,
            color = LavaInk,
            fontSize = 17.sp,
            fontFamily = ThemeFonts.Righteous,
            style = TextStyle(shadow = Shadow(Color(0x80FF9650), Offset.Zero, 14f)),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
        if (showTime) {
            Spacer(Modifier.width(10.dp))
            Text(
                data.time.uppercase(),
                color = LavaAmber,
                fontSize = 9.sp,
                fontFamily = ThemeFonts.Nunito,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 1.2.sp,
                maxLines = 1
            )
        }
    }
}

/* ------------------------------------------------------------------ */
/* The dock: keys in chrome bezels                                      */
/* ------------------------------------------------------------------ */

@Composable
private fun BoxScope.LavaDock(
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
        modifier.fillMaxWidth(0.58f),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        specs.forEachIndexed { index, spec ->
            val source = remember { MutableInteractionSource() }
            val pressed by source.collectIsPressedAsState()
            val lens = when (index % 3) {
                1 -> listOf(Color(0xFFD9C0FF), Color(0xFF8A5BD6))
                2 -> listOf(Color(0xFFFFC2C2), Color(0xFFD6485F))
                else -> listOf(Color(0xFFFFD9A0), Color(0xFFF08A2E))
            }
            val ink = when (index % 3) {
                1 -> Color(0xFF2A1046)
                2 -> Color(0xFF3A0C18)
                else -> Color(0xFF3A1200)
            }
            val bezel = when (index % 3) {
                1 -> Color(0xFF9C7BD8)
                2 -> Color(0xFFC98A72)
                else -> Color(0xFFB98C33)
            }
            val gasket = when (index % 3) {
                1 -> Color(0xFF1E1330)
                2 -> Color(0xFF2A0D14)
                else -> Color(0xFF22150E)
            }

            Box(
                Modifier
                    .weight(1f)
                    .height(32.dp)
                    .graphicsLayer {
                        val enterMs = enter.value * LavaTimelineMs
                        val exitMs = ex * LavaExitMs
                        val appearAt = 860f + index * 40f
                        val local = ((enterMs - appearAt) / 500f).coerceIn(0f, 1f)
                        alpha = if (leaving) {
                            (1f - (ex * LavaExitMs) / 250f).coerceIn(0f, 1f)
                        } else {
                            easeOut(local)
                        }
                        val s = if (leaving) {
                            1f
                        } else {
                            0.9f + 0.1f * easeBack(local)
                        }
                        translationY = if (leaving) 0f else 16f * (1f - easeOut(local))
                        val p = if (pressed) 0.97f else s
                        scaleX = p
                        scaleY = p
                    }
                    .clip(RoundedCornerShape(5.dp))
                    .background(Brush.verticalGradient(lens))
                    .drawBehind {
                        // the gasket and the chrome bezel
                        drawRect(
                            color = gasket,
                            style = Stroke(width = 2.dp.toPx())
                        )
                        drawRect(
                            color = bezel,
                            style = Stroke(width = 3.5.dp.toPx())
                        )
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
                    .padding(horizontal = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    spec.label.uppercase(),
                    color = ink,
                    fontSize = (10.5f * scale).sp,
                    fontFamily = ThemeFonts.Nunito,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 0.7.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

/* ------------------------------------------------------------------ */
/* The room                                                             */
/* ------------------------------------------------------------------ */

/** The room settles, and goes dark when the lamp is packed away. */
private fun Modifier.lavaRoom(
    enter: Animatable<Float, *>,
    ex: Float,
    leaving: Boolean
): Modifier = graphicsLayer {
    val enterMs = enter.value * LavaTimelineMs
    val exitMs = ex * LavaExitMs
    if (leaving) {
        val k = ((exitMs - 200f) / 850f).coerceIn(0f, 1f)
        alpha = 1f - k
        val s = 1f - 0.04f * k
        scaleX = s
        scaleY = s
    } else {
        val k = (enterMs / 600f).coerceIn(0f, 1f)
        alpha = easeOut(k)
        val s = 0.96f + 0.04f * easeOut(k)
        scaleX = s
        scaleY = s
    }
}
