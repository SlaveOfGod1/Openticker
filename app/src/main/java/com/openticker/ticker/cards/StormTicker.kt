package com.openticker.ticker.cards

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.openticker.data.Prefs
import com.openticker.data.TickerData
import com.openticker.data.TickerInteractions
import com.openticker.ticker.styles.notificationActionSpecs

/* ==================================================================== */
/* 16 — Thunderstorm (a storm over the moor at night)                    */
/* ==================================================================== */
/*
 * A bank of low cloud rolls in over dark hills, the wind gets up and the
 * rain comes driving across, the puddles fill and the lightning splits
 * the sky.
 *
 * Appearing: the cloud bank rolls in, the land rises out of the dark,
 * the reeds bend, the rain comes down, the puddles fill and the
 * lightning strikes as the words arrive.
 * Leaving: the rain stops, a beat of quiet, one last strike — and the
 * whole notification is gone in a single frame.
 */

private val StormSky0 = Color(0xFF060B14)
private val StormSky1 = Color(0xFF0B1626)
private val StormSky2 = Color(0xFF14263C)
private val StormSky3 = Color(0xFF1D3A52)
private val StormCloud0 = Color(0xFF47546F)
private val StormCloud1 = Color(0xFF2B364A)
private val StormCloud2 = Color(0xFF1A2434)
private val StormInk = Color(0xFFF2F6FF)
private val StormInkSoft = Color(0xFFB4C4DC)
private val StormFlash = Color(0xFFF2F8FF)
private val StormBolt = Color(0xFFFFD84D)

/** The whole choreography, in milliseconds. */
private const val StormTimelineMs = 2300f

/** The way out. */
private const val StormExitMs = 1200f

/* ------------------------------------------------------------------ */
/* Timing helpers                                                      */
/* ------------------------------------------------------------------ */

private fun stormStage(ms: Float, delay: Float, dur: Float): Float =
    ((ms - delay) / dur).coerceIn(0f, 1f)

private fun stormEaseOut(x: Float): Float {
    val c = x.coerceIn(0f, 1f)
    val inv = 1f - c
    return 1f - inv * inv * inv
}

private fun stormEaseIn(x: Float): Float {
    val c = x.coerceIn(0f, 1f)
    return c * c * c
}

/* ------------------------------------------------------------------ */
/* Ambient loops                                                       */
/* ------------------------------------------------------------------ */

private data class StormPhases(
    val drift1: Float = 0.5f,
    val drift2: Float = 0.5f,
    val drift3: Float = 0.5f,
    val rain: Float = 0f,
    val rainFast: Float = 0f,
    val bend: Float = 0.5f,
    val puddle: Float = 0.5f
)

@Composable
private fun rememberStormPhases(effect: Boolean): StormPhases {
    val speed = rememberAnimationSpeed()
    fun d(ms: Int) = scaleDuration(ms, speed)
    val t = rememberInfiniteTransition(label = "storm")

    val drift1 by t.animateFloat(
        0f, 1f,
        infiniteRepeatable(tween(d(11000), easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "drift1"
    )
    val drift2 by t.animateFloat(
        0f, 1f,
        infiniteRepeatable(tween(d(8000), easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "drift2"
    )
    val drift3 by t.animateFloat(
        0f, 1f,
        infiniteRepeatable(tween(d(14000), easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "drift3"
    )
    val rain by t.animateFloat(
        0f, 1f,
        infiniteRepeatable(tween(d(420), easing = LinearEasing)),
        label = "rain"
    )
    val rainFast by t.animateFloat(
        0f, 1f,
        infiniteRepeatable(tween(d(260), easing = LinearEasing)),
        label = "rain-fast"
    )
    val bend by t.animateFloat(
        0f, 1f,
        infiniteRepeatable(tween(d(2600), easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "bend"
    )
    val puddle by t.animateFloat(
        0f, 1f,
        infiniteRepeatable(tween(d(5000), easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "puddle"
    )

    return StormPhases(
        drift1 = if (effect) drift1 else 0.5f,
        drift2 = if (effect) drift2 else 0.5f,
        drift3 = if (effect) drift3 else 0.5f,
        rain = if (effect) rain else 0f,
        rainFast = if (effect) rainFast else 0f,
        bend = if (effect) bend else 0.5f,
        puddle = if (effect) puddle else 0.5f
    )
}

/* ==================================================================== */
/* Card                                                                */
/* ==================================================================== */

@Composable
fun CuratedStormTicker(
    data: TickerData,
    interactions: TickerInteractions?,
    animateIn: Boolean,
    exitFraction: () -> Float
) {
    val context = LocalContext.current
    val opts = Prefs.themeOptions(context, "storm", 0xFFFFD84D.toInt())
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
        enter.animateTo(1f, tween(scaleDuration(StormTimelineMs.toInt(), speed), easing = LinearEasing))
    }
    val enterMs = enter.value * StormTimelineMs
    val exitMs = ex * StormExitMs

    val phases = rememberStormPhases(opts.effect)

    // the last strike: everything is swallowed in one frame
    val snuff = if (leaving) {
        val p = stormStage(exitMs, 150f, 450f)
        if (p >= 0.32f) 0f else 1f
    } else {
        1f
    }

    CardFrame(
        modifier = Modifier.stormCard(enterMs, exitMs, leaving),
        lines = lines,
        hasDock = hasDock,
        // One line of text plus the dock; every extra line adds 16dp.
        baseHeight = 140.dp,
        dockSpace = 54.dp,
        decorationAlpha = intensity / 100f,
        shape = RoundedCornerShape(14.dp),
        background = Brush.verticalGradient(
            listOf(StormSky0, StormSky1, StormSky2, StormSky3)
        ),
        decoration = {
            // the moon behind the cloud
            drawRect(
                Brush.radialGradient(
                    listOf(Color(0x3896C4F6), Color.Transparent),
                    center = Offset(size.width * 0.74f, -size.height * 0.06f),
                    radius = size.width * 0.55f
                )
            )
            drawRect(
                Brush.radialGradient(
                    listOf(Color(0x1E34789C), Color.Transparent),
                    center = Offset(size.width * 0.16f, size.height * 0.98f),
                    radius = size.width * 0.5f
                )
            )
        }
    ) {
        Box(Modifier.fillMaxSize()) {
            // The storm.
            Canvas(Modifier.fillMaxSize().graphicsLayer { alpha = intensity / 100f }) {
                drawStormScene(enterMs, exitMs, leaving, phases, snuff)
            }

            // The note, over the sky.
            Column(
                Modifier
                    .align(Alignment.TopStart)
                    .fillMaxWidth()
                    .padding(start = 26.dp, end = 30.dp, top = 22.dp)
                    .graphicsLayer {
                        val p = if (leaving) {
                            stormEaseIn(stormStage(exitMs, 150f, 450f))
                        } else {
                            stormEaseOut(stormStage(enterMs, 1450f, 600f))
                        }
                        alpha = if (leaving) (1f - p) * snuff else p
                    }
            ) {
                CardSenderRow(
                    title = data.title,
                    showTime = showTime,
                    showLogo = showLogo,
                    logoSize = logoSize,
                    logoData = data,
                    time = data.time,
                    titleColor = StormInk,
                    timeColor = Color(0xFFCFE0F5),
                    titleFont = ThemeFonts.OswaldBold,
                    timeFont = ThemeFonts.NunitoBlack,
                    titleSize = 18f,
                    timeSize = 9f,
                    titleLetter = 0.8f,
                    uppercaseTitle = true,
                    titleWeight = FontWeight.Bold,
                    titleStyle = TextStyle(shadow = Shadow(Color(0xCC000000), Offset(0f, 2f), 10f))
                )
                Spacer(Modifier.height(5.dp))
                CardBody(
                    text = data.body,
                    lines = lines,
                    color = StormInkSoft,
                    font = ThemeFonts.NunitoBlack,
                    weight = FontWeight.ExtraBold,
                    sizeSp = 12f,
                    lineHeightFactor = 1.3333f,
                    style = TextStyle(shadow = Shadow(Color(0xE6000000), Offset(0f, 2f), 8f))
                )
            }

            if (hasDock) {
                StormDock(
                    data = data,
                    interactions = interactions,
                    scale = scale,
                    enterMs = enterMs,
                    exitMs = exitMs,
                    leaving = leaving,
                    snuff = snuff,
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(start = 26.dp, end = 30.dp, bottom = 20.dp)
                )
            }

            // The lightning.
            StormLightning(
                enterMs = enterMs,
                exitMs = exitMs,
                leaving = leaving
            )
        }
    }
}

/**
 * The card arrives like a native notification and never darkens to black.
 * On the way out the strike takes it and the night closes over it.
 */
private fun Modifier.stormCard(enterMs: Float, exitMs: Float, leaving: Boolean): Modifier =
    graphicsLayer {
        if (leaving) {
            val p = stormEaseIn(stormStage(exitMs, 450f, 450f))
            alpha = 1f - p
        } else {
            alpha = stormEaseOut(stormStage(enterMs, 0f, 350f))
        }
    }

/* ------------------------------------------------------------------ */
/* The scene                                                           */
/* ------------------------------------------------------------------ */

private fun DrawScope.drawStormScene(
    enterMs: Float,
    exitMs: Float,
    leaving: Boolean,
    phases: StormPhases,
    snuff: Float
) {
    val d = density
    val w = size.width
    val h = size.height

    // ---- the cloud bank --------------------------------------------
    val clouds = listOf(
        Triple(0.0f, 0.62f, 0.10f),
        Triple(0.28f, 0.46f, 0.22f),
        Triple(0.50f, 0.34f, 0.38f)
    )
    clouds.forEachIndexed { i, (delay, top, dx) ->
        val p = if (leaving) 1f else {
            stormEaseOut(stormStage(enterMs, 120f + i * 120f, 800f))
        }
        if (p <= 0.01f) return@forEachIndexed
        val drift = when (i) {
            0 -> phases.drift1
            1 -> phases.drift2
            else -> phases.drift3
        }
        val cw = w * (0.62f - i * 0.08f)
        val ch = (58f - i * 10f) * d
        val cx = w * (if (i == 0) -0.14f else if (i == 1) 0.42f else 0.20f) + (dx + drift * 0.1f) * w
        val cy = -14f * d + i * 22f * d + (1f - p) * -34f * d
        val brush = Brush.verticalGradient(
            listOf(StormCloud0, StormCloud1, StormCloud2),
            startY = cy,
            endY = cy + ch
        )
        fun lobe(x: Float, y: Float, lw: Float, lh: Float) {
            drawOval(
                brush = brush,
                topLeft = Offset(cx + x, cy + y),
                size = Size(lw, lh),
                alpha = p * snuff
            )
        }
        lobe(0f, ch * 0.20f, cw * 0.58f, ch * 0.70f)
        lobe(cw * 0.38f, ch * 0.10f, cw * 0.62f, ch * 0.88f)
        lobe(cw * 0.26f, 0f, cw * 0.46f, ch)
    }

    // ---- the land ---------------------------------------------------
    val hills = listOf(
        Triple(46f * d, 200f, 0f),
        Triple(30f * d, 100f, 100f)
    )
    hills.forEachIndexed { i, (hh, rise, delay) ->
        val p = if (leaving) 1f else {
            stormEaseOut(stormStage(enterMs, delay, 700f))
        }
        if (p <= 0.01f) return@forEachIndexed
        val top = h - hh + (1f - p) * 30f * d
        val path = Path().apply {
            moveTo(-w * 0.1f, h)
            cubicTo(
                w * 0.2f, top - rise * 0.28f,
                w * 0.55f, top - rise * 0.16f,
                w * 1.1f, h
            )
            close()
        }
        drawPath(
            path = path,
            brush = Brush.verticalGradient(
                listOf(if (i == 0) Color(0xFF0E2136) else Color(0xFF0A1826), Color(0xFF050C15)),
                startY = top,
                endY = h
            ),
            alpha = p * snuff
        )
    }

    // ---- the reeds bending in the wind ------------------------------
    val reedP = if (leaving) 1f else {
        stormEaseOut(stormStage(enterMs, 450f, 500f))
    }
    if (reedP > 0.01f) {
        val lean = (-7f + 13f * phases.bend)
        rotate(lean, Offset(size.width * 0.5f, h - 6f * d)) {
            var x = -6f * d
            var i = 0
            while (x < w + 6f * d) {
                val tall = (20f + (i % 4) * 6f) * d
                drawLine(
                    color = Color(0xE60C1A28),
                    start = Offset(x, h - 6f * d),
                    end = Offset(x + (i % 3 - 1) * 3f * d, h - 6f * d - tall),
                    strokeWidth = 1.6f * d
                )
                x += 11f * d
                i++
            }
        }
    }

    // ---- the puddles ------------------------------------------------
    val puddles = listOf(
        Triple(0.06f, 8f, 850f),
        Triple(0.66f, 4f, 1000f)
    )
    puddles.forEach { (fx, fy, delay) ->
        val p = if (leaving) 1f else {
            stormEaseOut(stormStage(enterMs, delay, 600f))
        }
        if (p <= 0.01f) return@forEach
        val pw = 78f * d
        val ph = 15f * d
        val cx = w * fx
        val cy = h - 8f * d - fy * d
        drawOval(
            brush = Brush.radialGradient(
                listOf(
                    Color(0x57A0C8FF),
                    Color(0x6E34507A),
                    Color.Transparent
                ),
                center = Offset(cx + pw * 0.4f, cy - ph * 0.3f),
                radius = pw * 0.7f
            ),
            topLeft = Offset(cx, cy - ph),
            size = Size(pw, ph * 2f),
            alpha = p * snuff * (0.85f + 0.15f * phases.puddle)
        )
        drawOval(
            color = Color(0x3C8CAADC),
            topLeft = Offset(cx, cy - ph),
            size = Size(pw, ph * 2f),
            alpha = p * snuff
        )
    }

    // ---- the rain ---------------------------------------------------
    fun sheet(alpha: Float, phase: Float, step: Float, len: Float, weight: Float, tint: Color) {
        if (alpha <= 0.01f) return
        val offset = phase * len
        var x = -step + (offset % step)
        while (x < w + step) {
            var y = -len + ((offset * 1.7f) % len)
            while (y < h + len) {
                drawLine(
                    color = tint,
                    start = Offset(x, y),
                    end = Offset(x - len * 0.18f, y + len * 0.62f),
                    strokeWidth = weight,
                    alpha = alpha
                )
                y += len
            }
            x += step
        }
    }

    val rainA = if (leaving) {
        if (stormStage(exitMs, 0f, 150f) >= 1f) 0f else 1f
    } else {
        stormEaseOut(stormStage(enterMs, 500f, 400f))
    }
    sheet(rainA * 0.5f * snuff, phases.rain, 19f * d, 46f * d, 1.2f * d, Color(0x66BAD6FF))

    val rainFastA = if (leaving) {
        if (stormStage(exitMs, 0f, 150f) >= 1f) 0f else 1f
    } else {
        stormEaseOut(stormStage(enterMs, 620f, 400f))
    }
    sheet(rainFastA * 0.42f * snuff, phases.rainFast, 34f * d, 70f * d, 1.5f * d, Color(0x73D6EAFF))
}

/* ------------------------------------------------------------------ */
/* The lightning                                                       */
/* ------------------------------------------------------------------ */

@Composable
private fun BoxScope.StormLightning(
    enterMs: Float,
    exitMs: Float,
    leaving: Boolean
) {
    // the flash: a hard stutter of white
    val flashA = if (leaving) {
        val p = stormStage(exitMs, 150f, 550f)
        keyframe(
            p,
            0f to 0f, 0.04f to 1f, 0.10f to 0.22f, 0.16f to 1f,
            0.24f to 0.18f, 0.31f to 1f, 0.42f to 0.45f, 1f to 0f
        )
    } else {
        val p = stormStage(enterMs, 1150f, 1200f)
        keyframe(
            p,
            0f to 0f, 0.04f to 1f, 0.09f to 0.28f, 0.14f to 1f,
            0.20f to 0.2f, 0.27f to 0.95f, 0.36f to 0.12f, 0.46f to 0.7f, 1f to 0f
        )
    }
    if (flashA > 0.002f) {
        Box(
            Modifier
                .fillMaxSize()
                .graphicsLayer { alpha = flashA }
                .background(StormFlash)
        )
    }

    // the bolt itself
    val boltP = if (leaving) {
        stormStage(exitMs, 150f, 550f)
    } else {
        stormStage(enterMs, 1150f, 1100f)
    }
    val boltAlpha = if (leaving) {
        keyframe(boltP, 0f to 0f, 0.06f to 1f, 0.55f to 1f, 1f to 0f)
    } else {
        keyframe(boltP, 0f to 0f, 0.06f to 1f, 0.40f to 0.9f, 0.70f to 0f, 1f to 0f)
    }
    if (boltAlpha > 0.002f) {
        val boltScale = if (leaving) {
            1f
        } else {
            keyframe(boltP, 0f to 0.2f, 0.06f to 1f, 1f to 1f)
        }
        Canvas(
            Modifier
                .align(Alignment.TopEnd)
                .padding(end = 0.dp)
                .size(width = 20.dp, height = 44.dp)
                .graphicsLayer {
                    alpha = boltAlpha
                    scaleY = boltScale
                    transformOrigin = androidx.compose.ui.graphics.TransformOrigin(0.5f, 0f)
                }
        ) {
            val path = Path().apply {
                moveTo(size.width * 0.55f, 0f)
                lineTo(size.width * 0.15f, size.height * 0.52f)
                lineTo(size.width * 0.45f, size.height * 0.52f)
                lineTo(size.width * 0.25f, size.height)
                lineTo(size.width * 0.85f, size.height * 0.38f)
                lineTo(size.width * 0.52f, size.height * 0.38f)
                lineTo(size.width * 0.80f, 0f)
                close()
            }
            drawPath(path, Color(0x66FFD84D))
            drawPath(path, StormBolt)
        }
    }
}

/* ------------------------------------------------------------------ */
/* The dock                                                            */
/* ------------------------------------------------------------------ */

@Composable
private fun BoxScope.StormDock(
    data: TickerData,
    interactions: TickerInteractions?,
    scale: Float,
    enterMs: Float,
    exitMs: Float,
    leaving: Boolean,
    snuff: Float,
    modifier: Modifier = Modifier
) {
    val specs = notificationActionSpecs(data, interactions)
    if (specs.isEmpty()) return

    val dockP = if (leaving) {
        stormEaseIn(stormStage(exitMs, 150f, 450f))
    } else {
        stormEaseOut(stormStage(enterMs, 1700f, 450f))
    }
    val dockAlpha = if (leaving) (1f - dockP) * snuff else dockP

    Row(
        modifier.graphicsLayer { alpha = dockAlpha },
        horizontalArrangement = Arrangement.spacedBy(9.dp, Alignment.Start),
        verticalAlignment = Alignment.CenterVertically
    ) {
        specs.forEachIndexed { index, spec ->
            val source = remember { MutableInteractionSource() }
            val pressed by source.collectIsPressedAsState()
            val keyP = if (leaving) {
                stormEaseIn(stormStage(exitMs, 150f + index * 40f, 450f))
            } else {
                stormEaseOut(stormStage(enterMs, 1780f + index * 120f, 400f))
            }

            Box(
                Modifier
                    .graphicsLayer {
                        if (leaving) {
                            alpha = 1f - keyP
                        } else {
                            alpha = keyframe(keyP, 0f to 0f, 0.60f to 1f, 1f to 1f)
                            val s = keyframe(keyP, 0f to 0.5f, 0.60f to 1.1f, 1f to 1f)
                            scaleX = s
                            scaleY = s
                            rotationZ = keyframe(keyP, 0f to -8f, 0.60f to 2f, 1f to 0f)
                        }
                        if (pressed) scaleX *= 0.97f
                    }
                    .clip(RoundedCornerShape(4.dp))
                    .background(
                        Brush.horizontalGradient(
                            listOf(
                                Color(0x2ED6E8FF),
                                Color(0x1FB4C8F0)
                            )
                        )
                    )
                    .border(1.dp, Color(0x8CA8C6F0), RoundedCornerShape(4.dp))
                    .clickable(
                        interactionSource = source,
                        indication = null,
                        enabled = spec.onClick != null
                    ) { spec.onClick?.invoke() }
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    spec.label.uppercase(),
                    color = StormInk,
                    fontSize = (11f * scale).sp,
                    fontFamily = ThemeFonts.OswaldBold,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.6.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = TextStyle(shadow = Shadow(Color(0x6696BCFF), Offset.Zero, 10f))
                )
            }
        }
    }
}
