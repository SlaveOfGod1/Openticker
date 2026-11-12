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
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.openticker.data.Prefs
import com.openticker.data.TickerData
import com.openticker.data.TickerInteractions
import com.openticker.ticker.styles.notificationActionSpecs

/* ==================================================================== */
/* 12 — Crystal Ball                                                    */
/* ==================================================================== */
/*
 * A velvet midnight room that wakes up: the nebula swells, the stars
 * ignite one by one, a beam of moonlight crosses the room and a leaded
 * orb grows out of the mist with the app icon burning as the flame
 * inside. The note condenses out of the glass in a split of cyan and red.
 *
 * Leaving, the flame dies, the orb bursts into shards, the mist scatters
 * and the card itself dissolves into the dark.
 */

private val CrystalVelvet0 = Color(0xFF1B1242)
private val CrystalVelvet1 = Color(0xFF130D2E)
private val CrystalVelvet2 = Color(0xFF0A0719)
private val CrystalGold = Color(0xFFE8C46A)
private val CrystalGoldSoft = Color(0xFFF0E2B8)
private val CrystalInk = Color(0xFFF3ECFF)
private val CrystalInkSoft = Color(0xFFC8BDE8)
private val CrystalCyan = Color(0xFF00FFF7)
private val CrystalRed = Color(0xFFFF3E3E)
private val CrystalMist = Color(0xFFD4C6FF)
private val CrystalBeam = Color(0xFFFFF6DE)

/** The whole choreography, in milliseconds. */
private const val CrystalTimelineMs = 2150f

/** The way out. */
private const val CrystalExitMs = 1000f

/* ------------------------------------------------------------------ */
/* The cast                                                            */
/* ------------------------------------------------------------------ */

private data class CrystalStar(val x: Float, val y: Float, val size: Float, val delay: Float)

private val CrystalStars = listOf(
    CrystalStar(0.12f, 0.18f, 3.0f, 0.00f),
    CrystalStar(0.27f, 0.62f, 2.4f, 0.10f),
    CrystalStar(0.38f, 0.26f, 2.0f, 0.20f),
    CrystalStar(0.46f, 0.82f, 2.8f, 0.05f),
    CrystalStar(0.58f, 0.14f, 2.2f, 0.26f),
    CrystalStar(0.66f, 0.48f, 3.0f, 0.14f),
    CrystalStar(0.08f, 0.46f, 2.0f, 0.32f),
    CrystalStar(0.33f, 0.40f, 1.8f, 0.38f),
    CrystalStar(0.52f, 0.68f, 2.6f, 0.22f),
    CrystalStar(0.22f, 0.84f, 2.2f, 0.44f),
    CrystalStar(0.72f, 0.74f, 2.0f, 0.34f),
    CrystalStar(0.43f, 0.52f, 2.4f, 0.50f)
)

private data class CrystalRune(val x: Float, val y: Float, val delay: Float)

/** The runes sit around the orb, centred on the given fraction of the group. */
private val CrystalRunes = listOf(
    CrystalRune(0.10f, 0.39f, 0.10f),
    CrystalRune(0.68f, 0.27f, 0.22f),
    CrystalRune(0.22f, 0.17f, 0.34f),
    CrystalRune(0.86f, 0.71f, 0.46f)
)

private data class CrystalCloud(
    val x: Float,
    val y: Float,
    val w: Float,
    val h: Float,
    val phase: Float,
    val alpha: Float
)

private val CrystalClouds = listOf(
    CrystalCloud(-14f, 14f, 62f, 40f, 0.125f, 1.00f),
    CrystalCloud(62f, 4f, 70f, 44f, 0.406f, 1.00f),
    CrystalCloud(22f, -26f, 84f, 44f, 0.625f, 0.80f),
    CrystalCloud(6f, 76f, 58f, 36f, 0.813f, 0.70f)
)

private data class CrystalMote(val x: Float, val y: Float, val size: Float, val phase: Float)

private data class CrystalHaze(
    val cx: Float,
    val cy: Float,
    val w: Float,
    val h: Float,
    val color: Color,
    val phase: Float
)
private val CrystalMotes = listOf(
    CrystalMote(98f, 84f, 3.0f, 0.133f),
    CrystalMote(38f, 64f, 2.0f, 0.489f),
    CrystalMote(72f, 34f, 2.5f, 0.756f),
    CrystalMote(16f, 78f, 2.0f, 0.900f)
)

/** The twelve splinters the orb bursts into. */
private val CrystalShards = listOf(
    58f to -30f, 72f to 6f, 54f to 42f, 18f to -58f, -16f to -60f, -52f to -38f,
    -72f to -6f, -64f to 34f, -32f to 58f, 26f to 60f, 64f to 26f, 30f to -18f
)

/* ------------------------------------------------------------------ */
/* Timing helpers                                                      */
/* ------------------------------------------------------------------ */

private fun easeOutCubic(x: Float): Float {
    val c = x.coerceIn(0f, 1f)
    val inv = 1f - c
    return 1f - inv * inv * inv
}

private fun easeInCubic(x: Float): Float {
    val c = x.coerceIn(0f, 1f)
    return c * c * c
}

/** Progress of a stage that starts at [delay] and lasts [dur] milliseconds. */
private fun stage(ms: Float, delay: Float, dur: Float): Float =
    ((ms - delay) / dur).coerceIn(0f, 1f)

private fun lerp(a: Float, b: Float, t: Float): Float = a + (b - a) * t

/* ------------------------------------------------------------------ */
/* Ambient loops                                                       */
/* ------------------------------------------------------------------ */

private data class CrystalPhases(
    val haze1: Float = 0.5f,
    val haze2: Float = 0.5f,
    val haze3: Float = 0.5f,
    val swirl: Float = 0f,
    val prism: Float = 0f,
    val bob: Float = 0.5f,
    val corePulse: Float = 0.5f,
    val coreSpin: Float = 0.5f,
    val cloud: Float = 0.5f,
    val mote: Float = 0f,
    val moon: Float = 0.5f
)

/**
 * One clock drives every ambient loop of the theme: the nebula breathes,
 * the mist turns inside the glass, the prism rides the rim, the orb bobs,
 * the flame pulses, the clouds drift and the motes rise.
 */
@Composable
private fun rememberCrystalPhases(effect: Boolean): CrystalPhases {
    val speed = rememberAnimationSpeed()
    fun d(ms: Int) = scaleDuration(ms, speed)
    val t = rememberInfiniteTransition(label = "crystal")

    val haze1 by t.animateFloat(
        0f, 1f,
        infiniteRepeatable(tween(d(9000), easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "haze1"
    )
    val haze2 by t.animateFloat(
        0f, 1f,
        infiniteRepeatable(tween(d(11000), easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "haze2"
    )
    val haze3 by t.animateFloat(
        0f, 1f,
        infiniteRepeatable(tween(d(13000), easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "haze3"
    )
    val swirl by t.animateFloat(
        0f, 1f,
        infiniteRepeatable(tween(d(11000), easing = LinearEasing)),
        label = "swirl"
    )
    val prism by t.animateFloat(
        0f, 1f,
        infiniteRepeatable(tween(d(15000), easing = LinearEasing)),
        label = "prism"
    )
    val bob by t.animateFloat(
        0f, 1f,
        infiniteRepeatable(tween(d(5500), easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "bob"
    )
    val corePulse by t.animateFloat(
        0f, 1f,
        infiniteRepeatable(tween(d(4500), easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "core-pulse"
    )
    val coreSpin by t.animateFloat(
        0f, 1f,
        infiniteRepeatable(tween(d(18000), easing = LinearEasing)),
        label = "core-spin"
    )
    val cloud by t.animateFloat(
        0f, 1f,
        infiniteRepeatable(tween(d(16000), easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "cloud"
    )
    val mote by t.animateFloat(
        0f, 1f,
        infiniteRepeatable(tween(d(9000), easing = LinearEasing)),
        label = "mote"
    )
    val moon by t.animateFloat(
        0f, 1f,
        infiniteRepeatable(tween(d(6000), easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "moon"
    )

    return CrystalPhases(
        haze1 = if (effect) haze1 else 0.5f,
        haze2 = if (effect) haze2 else 0.5f,
        haze3 = if (effect) haze3 else 0.5f,
        swirl = if (effect) swirl else 0.12f,
        prism = if (effect) prism else 0.2f,
        bob = if (effect) bob else 0.5f,
        corePulse = if (effect) corePulse else 0.6f,
        coreSpin = if (effect) coreSpin else 0.5f,
        cloud = if (effect) cloud else 0.5f,
        mote = if (effect) mote else 0.1f,
        moon = if (effect) moon else 0.6f
    )
}

/* ==================================================================== */
/* The card                                                            */
/* ==================================================================== */

@Composable
fun CuratedCrystalTicker(
    data: TickerData,
    interactions: TickerInteractions?,
    animateIn: Boolean,
    exitFraction: () -> Float
) {
    val context = LocalContext.current
    val opts = Prefs.themeOptions(context, "crystal", 0xFFE8C46A.toInt())
    val lines = opts.lines
    val intensity = opts.intensity
    val showTime = opts.showTime
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
        enter.animateTo(1f, tween(scaleDuration(CrystalTimelineMs.toInt(), speed), easing = LinearEasing))
    }
    val enterMs = enter.value * CrystalTimelineMs
    val exitMs = ex * CrystalExitMs

    val phases = rememberCrystalPhases(opts.effect)

    CardFrame(
        modifier = Modifier.crystalCard(enterMs, exitMs, leaving),
        lines = lines,
        hasDock = hasDock,
        // One line of text plus the dock; every extra line adds 16dp.
        baseHeight = 137.dp,
        dockSpace = 65.dp,
        decorationAlpha = intensity / 100f,
        shape = RoundedCornerShape(20.dp),
        background = Brush.verticalGradient(
            0f to CrystalVelvet0, 0.52f to CrystalVelvet1, 1f to CrystalVelvet2
        ),
        decoration = {
            // the two pools of light the velvet catches
            drawRect(
                Brush.radialGradient(
                    listOf(Color(0x437E60E2), Color.Transparent),
                    center = Offset(size.width * 0.80f, size.height * 1.04f),
                    radius = size.width * 0.63f
                )
            )
            drawRect(
                Brush.radialGradient(
                    listOf(Color(0x385848A8), Color.Transparent),
                    center = Offset(size.width * 0.08f, -size.height * 0.02f),
                    radius = size.width * 0.50f
                )
            )
        }
    ) {
        Box(Modifier.fillMaxSize()) {
            // The nebula behind everything.
            CrystalNebula(
                enterMs = enterMs,
                exitMs = exitMs,
                leaving = leaving,
                phases = phases
            )

            // The sky: the moon, the stars and the beam of moonlight.
            Canvas(Modifier.fillMaxSize()) {
                drawCrystalMoon(phases, enterMs, exitMs, leaving)
                drawCrystalStars(phases, enterMs, exitMs, leaving)
                drawCrystalBeam(enterMs, exitMs, leaving)
            }

            // The instrument: orb, stand, mist and shards.
            CrystalInstrument(
                data = data,
                enterMs = enterMs,
                exitMs = exitMs,
                leaving = leaving,
                phases = phases,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 16.dp, bottom = 12.dp)
            )

            // The note.
            Column(
                Modifier
                    .align(Alignment.TopStart)
                    .fillMaxWidth(0.62f)
                    .padding(start = 24.dp, top = 26.dp)
                    .graphicsLayer {
                        val p = if (leaving) {
                            easeInCubic(stage(exitMs, 0f, 600f))
                        } else {
                            easeOutCubic(stage(enterMs, 620f, 900f))
                        }
                        alpha = if (leaving) {
                            keyframe(p, 0f to 1f, 0.35f to 1f, 1f to 0f)
                        } else {
                            keyframe(p, 0f to 0f, 0.30f to 1f, 1f to 1f)
                        }
                    }
            ) {
                CrystalSenderRow(
                    data = data,
                    showTime = showTime,
                    enterMs = enterMs,
                    exitMs = exitMs,
                    leaving = leaving
                )
                Spacer(Modifier.height(5.dp))
                CrystalBody(
                    text = data.body,
                    lines = lines,
                    enterMs = enterMs,
                    exitMs = exitMs,
                    leaving = leaving
                )
            }

            if (hasDock) {
                CrystalDock(
                    data = data,
                    interactions = interactions,
                    scale = scale,
                    enterMs = enterMs,
                    exitMs = exitMs,
                    leaving = leaving,
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(start = 24.dp, bottom = 20.dp)
                )
            }
        }
    }
}

/* ------------------------------------------------------------------ */
/* The card itself                                                     */
/* ------------------------------------------------------------------ */

/**
 * The card fades in like a native notification and never darkens — on the
 * way out it dissolves into mist instead of turning black.
 */
private fun Modifier.crystalCard(enterMs: Float, exitMs: Float, leaving: Boolean): Modifier =
    graphicsLayer {
        if (leaving) {
            val p = easeInCubic(stage(exitMs, 550f, 450f))
            alpha = 1f - p
            val s = lerp(1f, 0.94f, p)
            scaleX = s
            scaleY = s
        } else {
            alpha = easeOutCubic(stage(enterMs, 0f, 420f))
        }
    }

/* ------------------------------------------------------------------ */
/* The nebula                                                          */
/* ------------------------------------------------------------------ */

@Composable
private fun CrystalNebula(
    enterMs: Float,
    exitMs: Float,
    leaving: Boolean,
    phases: CrystalPhases
) {
    val wake = if (leaving) {
        easeInCubic(stage(exitMs, 0f, 600f))
    } else {
        easeOutCubic(stage(enterMs, 0f, 1400f))
    }
    val scale = if (leaving) lerp(1f, 0.92f, wake) else keyframe(wake, 0f to 0.86f, 0.55f to 1.06f, 1f to 1f)
    val alpha = if (leaving) lerp(1f, 0.3f, wake) else keyframe(wake, 0f to 0.6f, 0.55f to 1f, 1f to 1f)
    val bright = if (leaving) lerp(1f, 0.85f, wake) else keyframe(wake, 0f to 0.8f, 0.55f to 1.4f, 1f to 1f)

    Box(
        Modifier
            .fillMaxSize()
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
                this.alpha = alpha
            }
    ) {
        Canvas(Modifier.fillMaxSize()) {
            // violet top-left, blue bottom-right, magenta in the middle
            val blobs = listOf(
                CrystalHaze(
                    size.width * 0.02f + 120f * density,
                    size.height * 0.04f + 85f * density,
                    240f * density, 170f * density,
                    Color(0x998668F0), phases.haze1
                ),
                CrystalHaze(
                    size.width * 0.97f - 125f * density,
                    size.height * 0.94f - 85f * density,
                    250f * density, 170f * density,
                    Color(0x8054A6EC), phases.haze2
                ),
                CrystalHaze(
                    size.width * 0.76f - 95f * density,
                    size.height * 0.16f + 75f * density,
                    190f * density, 150f * density,
                    Color(0x61C67EEE), phases.haze3
                )
            )
            blobs.forEach { haze ->
                val drift = 26f * haze.phase * density
                val rise = -18f * haze.phase * density
                val grow = 0.9f + 0.28f * haze.phase
                val cx = haze.cx + drift
                val cy = haze.cy + rise
                val rx = haze.w * 0.5f * grow
                val ry = haze.h * 0.5f * grow
                drawOval(
                    brush = Brush.radialGradient(
                        listOf(
                            haze.color.copy(alpha = (haze.color.alpha * bright).coerceIn(0f, 1f)),
                            Color.Transparent
                        ),
                        center = Offset(cx, cy),
                        radius = maxOf(rx, ry)
                    ),
                    topLeft = Offset(cx - rx, cy - ry),
                    size = Size(rx * 2f, ry * 2f)
                )
            }
        }
    }
}

/* ------------------------------------------------------------------ */
/* The sky                                                             */
/* ------------------------------------------------------------------ */

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawCrystalMoon(
    phases: CrystalPhases,
    enterMs: Float,
    exitMs: Float,
    leaving: Boolean
) {
    val p = if (leaving) easeInCubic(stage(exitMs, 0f, 500f)) else phases.moon
    val breathe = 0.96f + 0.10f * p
    val a = (if (leaving) 1f - p else 0.5f + 0.4f * p) * 0.9f
    val r = 60.dp.toPx() * breathe
    val cx = size.width * 0.06f + r * 0.2f
    val cy = size.height * 0.06f + r * 0.2f
    drawCircle(
        brush = Brush.radialGradient(
            listOf(Color(0x38E2D8FF).copy(alpha = Color(0x38E2D8FF).alpha * a), Color.Transparent),
            center = Offset(cx, cy),
            radius = r
        ),
        radius = r,
        center = Offset(cx, cy)
    )
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawCrystalStars(
    phases: CrystalPhases,
    enterMs: Float,
    exitMs: Float,
    leaving: Boolean
) {
    CrystalStars.forEach { star ->
        val p = if (leaving) {
            easeInCubic(stage(exitMs, 0f, 550f))
        } else {
            easeOutCubic(stage(enterMs, star.delay * 1000f, 1100f))
        }
        val a = if (leaving) {
            0.72f * (1f - p)
        } else {
            keyframe(p, 0f to 0f, 0.30f to 1f, 1f to 0.72f)
        }
        if (a <= 0.01f) return@forEach
        val s = if (leaving) {
            lerp(1f, 0.2f, p)
        } else {
            keyframe(p, 0f to 0.2f, 0.30f to 1.7f, 1f to 1f)
        }
        val halo = if (leaving) {
            5f * (1f - p)
        } else {
            keyframe(p, 0f to 0f, 0.30f to 12f, 1f to 5f)
        }
        val cx = size.width * star.x
        val cy = size.height * star.y
        val core = star.size * 0.5f * s
        if (halo > 0.2f) {
            val hr = halo.dp.toPx()
            drawCircle(
                brush = Brush.radialGradient(
                    listOf(Color.White.copy(alpha = 0.85f * a), Color.Transparent),
                    center = Offset(cx, cy),
                    radius = hr
                ),
                radius = hr,
                center = Offset(cx, cy)
            )
        }
        drawCircle(
            color = Color.White.copy(alpha = a * 0.92f),
            radius = core,
            center = Offset(cx, cy)
        )
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawCrystalBeam(
    enterMs: Float,
    exitMs: Float,
    leaving: Boolean
) {
    val p = if (leaving) {
        easeInCubic(stage(exitMs, 0f, 700f))
    } else {
        easeOutCubic(stage(enterMs, 80f, 1300f))
    }
    val a = if (leaving) {
        0.8f * (1f - p)
    } else {
        keyframe(p, 0f to 0f, 0.16f to 1f, 0.78f to 1f, 1f to 0f)
    }
    if (a <= 0.01f) return
    val sweep = if (leaving) {
        lerp(0.20f, -0.72f, p)
    } else {
        lerp(-0.72f, 0.72f, p)
    }
    val beamW = size.width * 2.2f
    val beamH = size.height * 1.8f
    rotate(-8f, Offset(size.width * 0.5f, size.height * 0.5f)) {
        translate(
            left = size.width * 0.5f - beamW * 0.5f + sweep * beamW,
            top = size.height * 0.5f - beamH * 0.5f
        ) {
            drawRect(
                brush = Brush.linearGradient(
                    listOf(
                        Color.Transparent,
                        Color.Transparent,
                        CrystalBeam.copy(alpha = 0.17f * a),
                        Color.Transparent
                    ),
                    start = Offset(beamW * 0.43f, 0f),
                    end = Offset(beamW * 0.57f, 0f)
                ),
                topLeft = Offset.Zero,
                size = Size(beamW, beamH)
            )
        }
    }
}

/* ------------------------------------------------------------------ */
/* The instrument                                                      */
/* ------------------------------------------------------------------ */

@Composable
private fun BoxScope.CrystalInstrument(
    data: TickerData,
    enterMs: Float,
    exitMs: Float,
    leaving: Boolean,
    phases: CrystalPhases,
    modifier: Modifier = Modifier
) {
    Box(modifier.width(116.dp).height(130.dp)) {
        // The mist around the orb.
        Canvas(Modifier.fillMaxSize()) {
            CrystalClouds.forEach { cloud ->
                val drift = phases.cloud
                val dx = 8f * drift
                val dy = -10f * drift
                val gs = 1f + 0.12f * drift
                val ga = 0.65f + 0.35f * drift

                val gx: Float
                val gy: Float
                val gScale: Float
                val gAlpha: Float
                if (leaving) {
                    val p = easeInCubic(stage(exitMs, 0f, 700f))
                    gx = lerp(0f, -38f, p)
                    gy = lerp(0f, -26f, p)
                    gScale = lerp(1f, 1.5f, p)
                    gAlpha = lerp(0.75f, 0f, p)
                } else {
                    val gp = easeOutCubic(stage(enterMs, 0f, 900f))
                    gx = lerp(46f, dx, gp)
                    gy = lerp(34f, dy, gp)
                    gScale = lerp(0.35f, gs, gp)
                    gAlpha = lerp(0f, ga, gp) * cloud.alpha
                }
                if (gAlpha <= 0.01f) return@forEach

                val cx = (cloud.x + cloud.w * 0.5f + gx) * density
                val cy = (cloud.y + cloud.h * 0.5f + gy) * density
                val rx = cloud.w * 0.5f * gScale * density
                val ry = cloud.h * 0.5f * gScale * density
                drawOval(
                    brush = Brush.radialGradient(
                        listOf(
                            CrystalMist.copy(alpha = 0.42f * gAlpha),
                            Color(0x29A08CE8).copy(alpha = 0.16f * gAlpha),
                            Color.Transparent
                        ),
                        center = Offset(cx, cy),
                        radius = maxOf(rx, ry)
                    ),
                    topLeft = Offset(cx - rx, cy - ry),
                    size = Size(rx * 2f, ry * 2f)
                )
            }
        }

        // The orb.
        Box(
            Modifier
                .align(Alignment.TopCenter)
                .size(100.dp)
                .graphicsLayer {
                    if (leaving) {
                        val p = easeInCubic(stage(exitMs, 0f, 700f))
                        alpha = keyframe(p, 0f to 1f, 0.22f to 1f, 1f to 0f)
                        val s = keyframe(p, 0f to 1f, 0.22f to 1.14f, 1f to 0.35f)
                        scaleX = s
                        scaleY = s
                        rotationZ = keyframe(p, 0f to 0f, 1f to 30f)
                    } else {
                        val p = easeOutCubic(stage(enterMs, 150f, 1000f))
                        alpha = keyframe(p, 0f to 0f, 0.40f to 1f, 1f to 1f)
                        val s = keyframe(p, 0f to 0.05f, 0.40f to 1.16f, 0.72f to 0.96f, 1f to 1f)
                        scaleX = s
                        scaleY = s
                        rotationZ = keyframe(p, 0f to -40f, 0.40f to 12f, 0.72f to -4f, 1f to 0f)
                        translationY = (2f - 6f * phases.bob).dp.toPx()
                    }
                }
        ) {
            Canvas(Modifier.fillMaxSize()) {
                // the violet glow the orb throws into the room
                val r = size.minDimension * 0.5f
                val c = Offset(size.width * 0.5f, size.height * 0.5f)
                drawCircle(
                    brush = Brush.radialGradient(
                        listOf(Color(0x99966EFF).copy(alpha = 0.55f), Color.Transparent),
                        center = c,
                        radius = r * 1.55f
                    ),
                    radius = r * 1.55f,
                    center = c
                )
            }
            Box(Modifier.fillMaxSize().clip(CircleShape)) {
                Canvas(Modifier.fillMaxSize()) {
                    drawCrystalOrb(phases)
                }
            }
            // The flame: the app icon burning inside the glass.
            Box(
                Modifier
                    .align(Alignment.Center)
                    .size(58.dp)
                    .graphicsLayer {
                        val pulse = 0.92f + 0.14f * phases.corePulse
                        if (leaving) {
                            val p = easeInCubic(stage(exitMs, 0f, 400f))
                            alpha = 1f - p
                            val s = keyframe(p, 0f to 1f, 1f to 0.2f)
                            scaleX = s * pulse
                            scaleY = s * pulse
                            rotationZ = -6f + 12f * phases.coreSpin
                        } else {
                            val p = easeOutCubic(stage(enterMs, 550f, 800f))
                            alpha = keyframe(p, 0f to 0f, 0.40f to 1f, 1f to 1f)
                            val s = keyframe(p, 0f to 0f, 0.40f to 1.7f, 1f to 1f)
                            scaleX = s * pulse
                            scaleY = s * pulse
                            rotationZ = -6f + 12f * phases.coreSpin
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                val coreBrush = with(LocalDensity.current) {
                    Brush.radialGradient(
                        listOf(
                            Color(0x9EFFE8E2),
                            Color(0x43FFE2A8),
                            Color(0x14FFC882),
                            Color.Transparent
                        ),
                        center = Offset(29.dp.toPx(), 29.dp.toPx()),
                        radius = 29.dp.toPx()
                    )
                }
                Box(Modifier.fillMaxSize().background(coreBrush))
                CardIcon(
                    data = data,
                    size = 27.dp,
                    shape = CircleShape,
                    fallbackBg = Color(0xFFFFF6DC),
                    fallbackFg = Color(0xFF3B2C0A)
                )
            }
        }

        // The shards the orb bursts into.
        Canvas(Modifier.fillMaxSize()) {
            if (!leaving) return@Canvas
            val p = easeOutCubic(stage(exitMs, 0f, 850f))
            if (p <= 0f) return@Canvas
            val a = keyframe(p, 0f to 0f, 0.12f to 1f, 1f to 0f)
            if (a <= 0.01f) return@Canvas
            val cx = 58f * density
            val cy = 50f * density
            CrystalShards.forEach { (dx, dy) ->
                val rot = keyframe(p, 0f to 0f, 0.12f to 60f, 1f to 340f)
                val s = keyframe(p, 0f to 0.3f, 0.12f to 1f, 1f to 0.15f)
                val tx = dx * density
                val ty = dy * density
                rotate(rot, Offset(cx + tx, cy + ty)) {
                    val w = 11f * density * s
                    val h = 13f * density * s
                    val path = Path().apply {
                        moveTo(cx + tx, cy + ty - h * 0.5f)
                        lineTo(cx + tx + w * 0.5f, cy + ty + h * 0.5f)
                        lineTo(cx + tx - w * 0.5f, cy + ty + h * 0.31f)
                        close()
                    }
                    drawPath(
                        path = path,
                        brush = Brush.linearGradient(
                            listOf(Color(0xE6FFFFFF), Color(0x66B09CFF)),
                            start = Offset(cx + tx - w * 0.5f, cy + ty - h * 0.5f),
                            end = Offset(cx + tx + w * 0.5f, cy + ty + h * 0.5f)
                        ),
                        alpha = a
                    )
                }
            }
        }

        // The brass pedestal.
        Canvas(Modifier.fillMaxSize()) {
            drawCrystalStand(enterMs, exitMs, leaving)
        }

        // The motes of dust rising past the orb.
        Canvas(Modifier.fillMaxSize()) {
            CrystalMotes.forEach { mote ->
                val ph = (phases.mote + mote.phase) % 1f
                val a = keyframe(ph, 0f to 0f, 0.16f to 0.9f, 0.84f to 0.7f, 1f to 0f)
                if (a <= 0.01f) return@forEach
                val x = mote.x * density + 10f * ph * density
                val y = mote.y * density - 120f * ph * density
                val r = mote.size * 0.5f * density
                drawCircle(
                    brush = Brush.radialGradient(
                        listOf(Color(0xD9E2D6FF), Color(0x00E2D6FF)),
                        center = Offset(x, y),
                        radius = r * 3.2f
                    ),
                    radius = r * 3.2f,
                    center = Offset(x, y)
                )
                drawCircle(
                    color = Color(0xD9E2D6FF).copy(alpha = a),
                    radius = r,
                    center = Offset(x, y)
                )
            }
        }

        // The runes flaring around the orb.
        CrystalRunes.forEach { rune ->
            Text(
                text = "✦",
                color = CrystalGold,
                fontSize = 14.sp,
                lineHeight = 14.sp,
                modifier = Modifier
                    .offset(
                        x = (rune.x * 116f - 7f).dp,
                        y = (rune.y * 130f - 7f).dp
                    )
                    .graphicsLayer {
                        if (leaving) {
                            val p = easeInCubic(stage(exitMs, 0f, 450f))
                            alpha = 0.8f * (1f - p)
                            val s = lerp(1f, 2.2f, p)
                            scaleX = s
                            scaleY = s
                            translationY = -10f * p
                        } else {
                            val p = easeOutCubic(stage(enterMs, rune.delay * 1000f, 1500f))
                            alpha = keyframe(p, 0f to 0f, 0.22f to 1f, 0.55f to 0.85f, 1f to 0f)
                            val s = keyframe(p, 0f to 0.2f, 0.22f to 1.3f, 0.55f to 1f, 1f to 0.7f)
                            scaleX = s
                            scaleY = s
                            rotationZ = keyframe(p, 0f to -40f, 0.22f to 10f, 0.55f to 0f, 1f to 16f)
                            translationY = keyframe(p, 0f to 8f, 0.22f to -5f, 0.55f to 0f, 1f to -16f)
                        }
                    }
            )
        }
    }
}

/** The glass: the mist turning inside and the prism riding the rim. */
private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawCrystalOrb(phases: CrystalPhases) {
    val r = size.minDimension * 0.5f
    val c = Offset(size.width * 0.5f, size.height * 0.5f)

    // the glass itself
    drawCircle(
        brush = Brush.radialGradient(
            listOf(
                Color(0x94FFFFFF),
                Color(0x29D4C6FF),
                Color(0x476042B0),
                Color(0xE01C0E3E)
            ),
            center = Offset(c.x - r * 0.32f, c.y - r * 0.48f),
            radius = r * 1.25f
        ),
        radius = r,
        center = c
    )

    // the mist turning inside the glass
    rotate(360f * phases.swirl, c) {
        drawCircle(
            brush = Brush.sweepGradient(
                listOf(
                    Color(0x4D966EFF),
                    Color(0x335AC8E6),
                    Color(0x42C896FF),
                    Color(0x2E9678DC),
                    Color(0x4DA078FF),
                    Color(0x4D966EFF)
                ),
                center = c
            ),
            radius = r * 1.6f,
            center = c
        )
    }

    // the rainbow bending along the edge of the glass
    rotate(360f * phases.prism, c) {
        drawCircle(
            brush = Brush.sweepGradient(
                listOf(
                    Color(0x99FF78BE),
                    Color(0x8CFFDE96),
                    Color(0x8C8CFFD6),
                    Color(0x9996B4FF),
                    Color(0x8CE096FF),
                    Color(0x99FF78BE)
                ),
                center = c
            ),
            radius = r * 0.93f,
            center = c,
            style = Stroke(width = r * 0.13f),
            alpha = 0.7f
        )
    }

    // the highlight on the glass
    drawOval(
        brush = Brush.radialGradient(
            listOf(Color(0x99FFFFFF), Color.Transparent),
            center = Offset(c.x - r * 0.42f, c.y - r * 0.5f),
            radius = r * 0.5f
        ),
        topLeft = Offset(c.x - r * 0.72f, c.y - r * 0.8f),
        size = Size(r * 0.9f, r * 0.6f)
    )
}

/** The turned brass pedestal the orb sits in. */
private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawCrystalStand(
    enterMs: Float,
    exitMs: Float,
    leaving: Boolean
) {
    val p = if (leaving) {
        easeInCubic(stage(exitMs, 150f, 500f))
    } else {
        easeOutCubic(stage(enterMs, 500f, 600f))
    }
    val a = if (leaving) 1f - p else p
    if (a <= 0.01f) return
    val ty = if (leaving) 14f * p else 16f * (1f - p)
    val s = if (leaving) lerp(1f, 0.94f, p) else lerp(0.9f, 1f, p)

    val cx = size.width * 0.5f
    val standTop = 100f * density

    // the shadow the instrument casts on the table
    drawOval(
        brush = Brush.radialGradient(
            listOf(Color(0xA8000000).copy(alpha = 0.66f * a), Color.Transparent),
            center = Offset(cx, 130f * density + 4f * density),
            radius = 59f * density
        ),
        topLeft = Offset(cx - 59f * density, 130f * density - 6f * density),
        size = Size(118f * density, 20f * density)
    )

    // the socket cup the orb sits in
    val socketW = 44f * density * s
    val socketH = 13f * density * s
    drawRoundRect(
        brush = Brush.horizontalGradient(
            listOf(Color(0xFF4A3810), Color(0xFFE7CE8A), Color(0xFFB78E32), Color(0xFF5A4413)),
            startX = cx - socketW * 0.5f,
            endX = cx + socketW * 0.5f
        ),
        topLeft = Offset(cx - socketW * 0.5f, standTop - 2f * density + ty),
        size = Size(socketW, socketH),
        cornerRadius = androidx.compose.ui.geometry.CornerRadius(7f * density, 4f * density)
    )

    // the narrow turned stem
    val stemW = 18f * density * s
    val stemH = 13f * density * s
    drawRoundRect(
        brush = Brush.horizontalGradient(
            listOf(Color(0xFF3B2C0A), Color(0xFFE7CE8A), Color(0xFFA87F2E), Color(0xFF2E2208)),
            startX = cx - stemW * 0.5f,
            endX = cx + stemW * 0.5f
        ),
        topLeft = Offset(cx - stemW * 0.5f, standTop + 9f * density + ty),
        size = Size(stemW, stemH),
        cornerRadius = androidx.compose.ui.geometry.CornerRadius(2f * density, 2f * density)
    )

    // the domed foot
    val footW = 96f * density * s
    val footH = 19f * density * s
    drawOval(
        brush = Brush.verticalGradient(
            listOf(Color(0xFFF6E3AC), Color(0xFFC69E3E), Color(0xFF7A5C18), Color(0xFF33260A)),
            startY = 130f * density - footH + ty,
            endY = 130f * density + ty
        ),
        topLeft = Offset(cx - footW * 0.5f, 130f * density - footH + ty),
        size = Size(footW, footH)
    )
}

/* ------------------------------------------------------------------ */
/* The note                                                            */
/* ------------------------------------------------------------------ */

@Composable
private fun CrystalSenderRow(
    data: TickerData,
    showTime: Boolean,
    enterMs: Float,
    exitMs: Float,
    leaving: Boolean
) {
    val split = crystalSplit(enterMs, exitMs, leaving)
    val letter = crystalLetterSpacing(enterMs, exitMs, leaving, 20f)
    val alpha = if (leaving) {
        keyframe(easeInCubic(stage(exitMs, 0f, 600f)), 0f to 1f, 0.35f to 1f, 1f to 0f)
    } else {
        keyframe(easeOutCubic(stage(enterMs, 620f, 900f)), 0f to 0f, 0.30f to 1f, 1f to 1f)
    }

    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        CrystalSplitText(
            text = data.title,
            color = CrystalGoldSoft,
            font = ThemeFonts.CormorantItalic,
            fontSize = 20.sp,
            lineHeight = 24.sp,
            weight = FontWeight.SemiBold,
            letterSpacing = letter,
            split = split,
            alpha = alpha,
            maxLines = 1,
            modifier = Modifier.weight(1f, fill = false)
        )
        if (showTime) {
            Spacer(Modifier.weight(1f))
            Spacer(Modifier.width(8.dp))
            Text(
                data.time.uppercase(),
                color = CrystalGold,
                fontSize = 9.sp,
                fontFamily = ThemeFonts.Nunito,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 1.5.sp,
                maxLines = 1,
                style = TextStyle(shadow = Shadow(CrystalGold.copy(alpha = 0.4f), Offset.Zero, 10f)),
                modifier = Modifier.graphicsLayer { this.alpha = alpha }
            )
        }
    }
}

@Composable
private fun CrystalBody(
    text: String,
    lines: Int,
    enterMs: Float,
    exitMs: Float,
    leaving: Boolean
) {
    val metrics = LocalCardTextMetrics.current
    val split = crystalSplit(enterMs, exitMs, leaving)
    val letter = crystalLetterSpacing(enterMs, exitMs, leaving, 12f)
    val alpha = if (leaving) {
        keyframe(easeInCubic(stage(exitMs, 0f, 600f)), 0f to 1f, 0.35f to 1f, 1f to 0f)
    } else {
        keyframe(easeOutCubic(stage(enterMs, 620f, 900f)), 0f to 0f, 0.30f to 1f, 1f to 1f)
    }
    CrystalSplitText(
        text = text,
        color = CrystalInkSoft,
        font = ThemeFonts.Nunito,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        weight = FontWeight.Bold,
        letterSpacing = letter,
        split = split,
        alpha = alpha,
        maxLines = lines,
        shadow = Shadow(Color(0xE604020E), Offset(0f, 1f), 8f),
        onLines = { count -> metrics?.report(count, 16.dp) }
    )
}

/** How far the writing is split into cyan and red, in dp. */
private fun crystalSplit(enterMs: Float, exitMs: Float, leaving: Boolean): Float {
    if (leaving) {
        val p = easeInCubic(stage(exitMs, 0f, 600f))
        return keyframe(p, 0f to 0f, 0.35f to 10f, 1f to 24f)
    }
    val p = easeOutCubic(stage(enterMs, 620f, 900f))
    return keyframe(p, 0f to 14f, 0.30f to 7f, 0.65f to -2f, 1f to 0f)
}

/** The letter-spacing the writing condenses from, in sp. */
private fun crystalLetterSpacing(
    enterMs: Float,
    exitMs: Float,
    leaving: Boolean,
    sizeSp: Float
): TextUnit {
    val em = if (leaving) {
        val p = easeInCubic(stage(exitMs, 0f, 600f))
        keyframe(p, 0f to 0f, 0.35f to 0.18f, 1f to 0.5f)
    } else {
        val p = easeOutCubic(stage(enterMs, 620f, 900f))
        keyframe(p, 0f to 0.30f, 0.30f to 0.14f, 0.65f to 0.02f, 1f to 0f)
    }
    return (em * sizeSp).sp
}

/**
 * The writing, three times over: a cyan ghost to the right, a red ghost to
 * the left and the real letters on top.
 */
@Composable
private fun CrystalSplitText(
    text: String,
    color: Color,
    font: FontFamily,
    fontSize: TextUnit,
    lineHeight: TextUnit,
    weight: FontWeight,
    letterSpacing: TextUnit,
    split: Float,
    alpha: Float,
    maxLines: Int,
    modifier: Modifier = Modifier,
    shadow: Shadow? = null,
    onLines: ((Int) -> Unit)? = null
) {
    Box(modifier) {
        if (split > 0.4f) {
            CrystalText(
                text = text,
                color = CrystalCyan.copy(alpha = 0.9f),
                font = font,
                fontSize = fontSize,
                lineHeight = lineHeight,
                weight = weight,
                letterSpacing = letterSpacing,
                maxLines = maxLines,
                shadow = null,
                modifier = Modifier
                    .offset(x = split.dp)
                    .graphicsLayer { this.alpha = alpha * 0.9f }
            )
            CrystalText(
                text = text,
                color = CrystalRed.copy(alpha = 0.9f),
                font = font,
                fontSize = fontSize,
                lineHeight = lineHeight,
                weight = weight,
                letterSpacing = letterSpacing,
                maxLines = maxLines,
                shadow = null,
                modifier = Modifier
                    .offset(x = -split.dp)
                    .graphicsLayer { this.alpha = alpha * 0.9f }
            )
        }
        CrystalText(
            text = text,
            color = color,
            font = font,
            fontSize = fontSize,
            lineHeight = lineHeight,
            weight = weight,
            letterSpacing = letterSpacing,
            maxLines = maxLines,
            shadow = shadow,
            modifier = Modifier.graphicsLayer { this.alpha = alpha },
            onLines = onLines
        )
    }
}

/**
 * One run of the writing. Zero lines means the rolling news ticker, which
 * scrolls instead of wrapping — `Text` refuses a line count of zero.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun CrystalText(
    text: String,
    color: Color,
    font: FontFamily,
    fontSize: TextUnit,
    lineHeight: TextUnit,
    weight: FontWeight,
    letterSpacing: TextUnit,
    maxLines: Int,
    shadow: Shadow?,
    modifier: Modifier = Modifier,
    onLines: ((Int) -> Unit)? = null
) {
    val context = LocalContext.current
    val speed = Prefs.speed(context)
    if (maxLines <= 0) {
        Text(
            text = text,
            color = color,
            fontSize = fontSize,
            lineHeight = lineHeight,
            fontWeight = weight,
            fontFamily = font,
            letterSpacing = letterSpacing,
            maxLines = 1,
            softWrap = false,
            overflow = TextOverflow.Clip,
            modifier = modifier.basicMarquee(
                iterations = Int.MAX_VALUE,
                velocity = (30f * speed).dp
            ),
            onTextLayout = { onLines?.invoke(1) }
        )
    } else {
        Text(
            text = text,
            color = color,
            fontSize = fontSize,
            lineHeight = lineHeight,
            fontWeight = weight,
            fontFamily = font,
            letterSpacing = letterSpacing,
            maxLines = maxLines,
            overflow = TextOverflow.Ellipsis,
            style = if (shadow != null) TextStyle(shadow = shadow) else TextStyle.Default,
            onTextLayout = { result -> onLines?.invoke(result.lineCount) },
            modifier = modifier
        )
    }
}

/* ------------------------------------------------------------------ */
/* The dock                                                            */
/* ------------------------------------------------------------------ */

@Composable
private fun BoxScope.CrystalDock(
    data: TickerData,
    interactions: TickerInteractions?,
    scale: Float,
    enterMs: Float,
    exitMs: Float,
    leaving: Boolean,
    modifier: Modifier = Modifier
) {
    val specs = notificationActionSpecs(data, interactions)
    if (specs.isEmpty()) return

    val dockP = if (leaving) {
        easeInCubic(stage(exitMs, 0f, 450f))
    } else {
        easeOutCubic(stage(enterMs, 820f, 500f))
    }
    val dockAlpha = if (leaving) 1f - dockP else dockP

    Row(
        modifier.graphicsLayer {
            alpha = dockAlpha
            translationY = if (leaving) 12f * dockP else 10f * (1f - dockP)
        },
        horizontalArrangement = Arrangement.spacedBy(9.dp, Alignment.Start),
        verticalAlignment = Alignment.CenterVertically
    ) {
        specs.forEachIndexed { index, spec ->
            val source = remember { MutableInteractionSource() }
            val pressed by source.collectIsPressedAsState()
            val read = spec.label.lowercase().contains("read") ||
                spec.label.contains("مقروء") ||
                spec.label.contains("قراءة")
            val bg = if (read) Color(0xB8704A22) else Color(0xD13A2C70)
            val bd = if (read) Color(0xB3E8C46A) else Color(0x80E8C46A)

            val keyP = if (leaving) {
                easeInCubic(stage(exitMs, index * 60f, 400f))
            } else {
                easeOutCubic(stage(enterMs, 880f + index * 100f, 500f))
            }

            Box(
                Modifier
                    .graphicsLayer {
                        if (leaving) {
                            alpha = 1f - keyP
                            val s = keyframe(keyP, 0f to 1f, 1f to 0.3f)
                            scaleX = s
                            scaleY = s
                            rotationZ = keyframe(keyP, 0f to 0f, 1f to 16f)
                        } else {
                            alpha = keyframe(keyP, 0f to 0f, 0.50f to 1f, 1f to 1f)
                            val s = keyframe(keyP, 0f to 0.25f, 0.50f to 1.12f, 1f to 1f)
                            scaleX = s
                            scaleY = s
                            rotationZ = keyframe(keyP, 0f to -22f, 0.50f to 4f, 1f to 0f)
                        }
                        if (pressed) scaleX *= 0.97f
                    }
                    .clip(RoundedCornerShape(999.dp))
                    .background(bg)
                    .border(1.dp, bd, RoundedCornerShape(999.dp))
                    .clickable(
                        interactionSource = source,
                        indication = null,
                        enabled = spec.onClick != null
                    ) { spec.onClick?.invoke() }
                    .padding(horizontal = 13.dp, vertical = 9.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    spec.label.uppercase(),
                    color = CrystalInk,
                    fontSize = (10f * scale).sp,
                    fontFamily = ThemeFonts.Nunito,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 1.2.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}
