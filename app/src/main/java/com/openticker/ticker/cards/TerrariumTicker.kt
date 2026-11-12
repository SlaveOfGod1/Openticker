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
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.translate
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
import com.openticker.ui.components.AppBadge

/* ==================================================================== */
/* 14 — Terrarium                                                       */
/* ==================================================================== */
/*
 * A bell jar on a lamplit workbench at night.
 *
 * The lamp finds the bench, the jar drops in and seats itself with a
 * wobble, the world inside wakes — moss spreads, a fern unfurls, three
 * mushrooms pop and the spores light up — and the words condense out of
 * the damp glass. Leaving, the world folds back up and the jar sinks
 * into the dark.
 */

private val TerRoom0 = Color(0xFF16231C)
private val TerRoom1 = Color(0xFF0D1613)
private val TerRoom2 = Color(0xFF060B09)
private val TerWood0 = Color(0xFF402D1E)
private val TerWood1 = Color(0xFF271A10)
private val TerWood2 = Color(0xFF120C07)
private val TerGold = Color(0xFF9FE0A8)
private val TerInk = Color(0xFFF2E7C8)
private val TerInkSoft = Color(0xFFB4CCB8)
private val TerBrass0 = Color(0xFF6A4E1E)
private val TerBrass1 = Color(0xFFE7CE8A)
private val TerBrass2 = Color(0xFFB78E32)
private val TerBrass3 = Color(0xFF543F14)

/** The whole choreography, in milliseconds. */
private const val TerTimelineMs = 1600f

/** The way out. */
private const val TerExitMs = 780f

/* ------------------------------------------------------------------ */
/* Timing helpers                                                      */
/* ------------------------------------------------------------------ */

private fun terStage(ms: Float, delay: Float, dur: Float): Float =
    ((ms - delay) / dur).coerceIn(0f, 1f)

private fun terEaseOut(x: Float): Float {
    val c = x.coerceIn(0f, 1f)
    val inv = 1f - c
    return 1f - inv * inv * inv
}

private fun terEaseIn(x: Float): Float {
    val c = x.coerceIn(0f, 1f)
    return c * c * c
}

/* ------------------------------------------------------------------ */
/* Ambient loops                                                       */
/* ------------------------------------------------------------------ */

private data class TerPhases(
    val pool: Float = 0.5f,
    val beam: Float = 0.5f,
    val mote: Float = 0f,
    val drop: Float = 0f,
    val spore: Float = 0.5f
)

/** One clock for the lamp, the dust, the condensation and the spores. */
@Composable
private fun rememberTerPhases(effect: Boolean): TerPhases {
    val speed = rememberAnimationSpeed()
    fun d(ms: Int) = scaleDuration(ms, speed)
    val t = rememberInfiniteTransition(label = "terrarium")

    val pool by t.animateFloat(
        0f, 1f,
        infiniteRepeatable(tween(d(5000), easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "pool"
    )
    val beam by t.animateFloat(
        0f, 1f,
        infiniteRepeatable(tween(d(7000), easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "beam"
    )
    val mote by t.animateFloat(
        0f, 1f,
        infiniteRepeatable(tween(d(4800), easing = LinearEasing)),
        label = "mote"
    )
    val drop by t.animateFloat(
        0f, 1f,
        infiniteRepeatable(tween(d(4200), easing = LinearEasing)),
        label = "drop"
    )
    val spore by t.animateFloat(
        0f, 1f,
        infiniteRepeatable(tween(d(5400), easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "spore"
    )

    return TerPhases(
        pool = if (effect) pool else 0.5f,
        beam = if (effect) beam else 0.5f,
        mote = if (effect) mote else 0.1f,
        drop = if (effect) drop else 0.1f,
        spore = if (effect) spore else 0.5f
    )
}

/* ==================================================================== */
/* Card                                                                */
/* ==================================================================== */

@Composable
fun CuratedTerrariumTicker(
    data: TickerData,
    interactions: TickerInteractions?,
    animateIn: Boolean,
    exitFraction: () -> Float
) {
    val context = LocalContext.current
    val opts = Prefs.themeOptions(context, "terrarium", 0xFF4C8A5A.toInt())
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
        enter.animateTo(1f, tween(scaleDuration(TerTimelineMs.toInt(), speed), easing = LinearEasing))
    }
    val enterMs = enter.value * TerTimelineMs
    val exitMs = ex * TerExitMs

    val phases = rememberTerPhases(opts.effect)

    CardFrame(
        modifier = Modifier.terCard(enterMs, exitMs, leaving),
        lines = lines,
        hasDock = hasDock,
        // One line of text plus the dock; every extra line adds 16dp.
        baseHeight = 150.dp,
        dockSpace = 66.dp,
        decorationAlpha = intensity / 100f,
        shape = RoundedCornerShape(20.dp),
        background = Brush.verticalGradient(listOf(TerRoom0, TerRoom1, TerRoom2))
    ) {
        Box(Modifier.fillMaxSize()) {
            // The room: the bench, the lamp, the dust in the beam.
            Canvas(Modifier.fillMaxSize().graphicsLayer { alpha = intensity / 100f }) {
                drawTerBench()
                drawTerPool(phases, enterMs, exitMs, leaving)
                drawTerBeam(phases, enterMs, exitMs, leaving)
                drawTerMotes(phases, enterMs, exitMs, leaving)
            }

            // The vessel.
            TerVessel(
                enterMs = enterMs,
                exitMs = exitMs,
                leaving = leaving,
                phases = phases,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 12.dp, bottom = 8.dp)
            )

            // The note.
            Column(
                Modifier
                    .align(Alignment.TopStart)
                    .fillMaxWidth(0.58f)
                    .padding(start = 22.dp, top = 22.dp)
                    .graphicsLayer {
                        val p = if (leaving) {
                            terEaseIn(terStage(exitMs, 0f, 300f))
                        } else {
                            terEaseOut(terStage(enterMs, 720f, 600f))
                        }
                        alpha = if (leaving) {
                            keyframe(p, 0f to 1f, 1f to 0f)
                        } else {
                            keyframe(p, 0f to 0f, 0.30f to 1f, 1f to 1f)
                        }
                    }
            ) {
                TerSenderRow(
                    data = data,
                    showTime = showTime,
                    showLogo = showLogo,
                    logoSize = logoSize,
                    enterMs = enterMs,
                    exitMs = exitMs,
                    leaving = leaving
                )
                Spacer(Modifier.height(5.dp))
                CardBody(
                    text = data.body,
                    lines = lines,
                    color = TerInkSoft,
                    font = ThemeFonts.Quicksand,
                    weight = FontWeight.SemiBold,
                    sizeSp = 12f,
                    lineHeightFactor = 1.3333f,
                    letterSpacing = terLetter(enterMs, exitMs, leaving, 12f),
                    style = TextStyle(shadow = Shadow(Color(0xE6020A06), Offset(0f, 1f), 8f))
                )
            }

            if (hasDock) {
                TerDock(
                    data = data,
                    interactions = interactions,
                    scale = scale,
                    enterMs = enterMs,
                    exitMs = exitMs,
                    leaving = leaving,
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(start = 22.dp, bottom = 20.dp)
                )
            }
        }
    }
}

/**
 * The card arrives like a native notification and never darkens; on the way
 * out it fades rather than turning black.
 */
private fun Modifier.terCard(enterMs: Float, exitMs: Float, leaving: Boolean): Modifier =
    graphicsLayer {
        if (leaving) {
            alpha = 1f - terEaseIn(terStage(exitMs, 540f, 240f))
        } else {
            alpha = terEaseOut(terStage(enterMs, 0f, 240f))
        }
    }

/* ------------------------------------------------------------------ */
/* The room                                                            */
/* ------------------------------------------------------------------ */

/** The bench, its back edge fading into the dark so no plank crosses the glass. */
private fun DrawScope.drawTerBench() {
    val d = density
    val top = size.height * 0.54f
    drawRect(
        brush = Brush.verticalGradient(
            listOf(
                Color.Transparent,
                Color(0x4D402D1E),
                TerWood0,
                TerWood1,
                TerWood2
            ),
            startY = top,
            endY = size.height
        ),
        topLeft = Offset(0f, top),
        size = Size(size.width, size.height - top)
    )
    // the grain: two dark pixels, two light, then clear — the same slow repeat
    var x = 0f
    var i = 0
    while (x < size.width) {
        val dark = i % 3 == 0
        drawRect(
            color = if (dark) Color(0x33000000) else Color(0x0AFFFFFF),
            topLeft = Offset(x, top + size.height * 0.06f),
            size = Size(2f * d, size.height - top)
        )
        x += 2f * d
        if (i % 3 == 1) x += 2f * d
        if (i % 3 == 2) x += 8f * d
        i++
    }
    // the front lip catching the lamp
    drawRect(
        brush = Brush.horizontalGradient(
            listOf(
                Color.Transparent,
                Color(0x3DFFD6A0),
                Color(0x1AFFD6A0),
                Color.Transparent
            ),
            startX = 0f,
            endX = size.width
        ),
        topLeft = Offset(0f, size.height - 4f * d),
        size = Size(size.width, 2f * d)
    )
}

/** The pool of lamplight the jar stands in. */
private fun DrawScope.drawTerPool(
    phases: TerPhases,
    enterMs: Float,
    exitMs: Float,
    leaving: Boolean
) {
    val p = if (leaving) {
        terEaseIn(terStage(exitMs, 0f, 420f))
    } else {
        terEaseOut(terStage(enterMs, 150f, 720f))
    }
    val alpha = if (leaving) 1f - p else p
    if (alpha <= 0.01f) return
    val breathe = 0.92f + 0.12f * phases.pool
    val d = density
    val cx = size.width - 34f * d
    val cy = size.height - 26f * d
    val rx = 120f * d * breathe
    val ry = 48f * d * breathe
    drawOval(
        brush = Brush.radialGradient(
            listOf(Color(0x38FFD494), Color.Transparent),
            center = Offset(cx, cy),
            radius = maxOf(rx, ry)
        ),
        topLeft = Offset(cx - rx, cy - ry),
        size = Size(rx * 2f, ry * 2f)
    )
}

/** The cone of lamplight coming down across the bench. */
private fun DrawScope.drawTerBeam(
    phases: TerPhases,
    enterMs: Float,
    exitMs: Float,
    leaving: Boolean
) {
    val p = if (leaving) {
        terEaseIn(terStage(exitMs, 0f, 540f))
    } else {
        terEaseOut(terStage(enterMs, 60f, 840f))
    }
    val a = if (leaving) {
        0.85f * (1f - p)
    } else {
        keyframe(p, 0f to 0f, 0.22f to 1f, 0.80f to 1f, 1f to 0.85f) * (0.9f + 0.1f * phases.beam)
    }
    if (a <= 0.01f) return
    val sweep = if (leaving) {
        -0.40f + 0.60f * p
    } else {
        -0.58f + 0.72f * p
    }
    val w = size.width * 2.4f
    val h = size.height * 1.7f
    rotate(-9f, Offset(size.width * 0.5f, size.height * 0.5f)) {
        translate(
            left = size.width * 0.5f - w * 0.5f + sweep * w,
            top = size.height * 0.5f - h * 0.5f
        ) {
            drawRect(
                brush = Brush.linearGradient(
                    listOf(
                        Color.Transparent,
                        Color.Transparent,
                        Color(0x22FFE4AC),
                        Color.Transparent
                    ),
                    start = Offset(w * 0.40f, 0f),
                    end = Offset(w * 0.60f, 0f)
                ),
                topLeft = Offset.Zero,
                size = Size(w, h)
            )
        }
    }
}

/** Dust turning in the beam. */
private fun DrawScope.drawTerMotes(
    phases: TerPhases,
    enterMs: Float,
    exitMs: Float,
    leaving: Boolean
) {
    val motes = listOf(
        Triple(0.22f, 0.18f, 0f),
        Triple(0.38f, 0.30f, 0.30f),
        Triple(0.52f, 0.14f, 0.60f),
        Triple(0.30f, 0.44f, 0.825f),
        Triple(0.60f, 0.38f, 0.15f)
    )
    motes.forEach { (fx, fy, off) ->
        val ph = (phases.mote + off) % 1f
        val a = if (leaving) {
            0.7f * (1f - terStage(exitMs, 0f, 400f))
        } else {
            keyframe(
                terEaseOut(terStage(enterMs, 100f + off * 400f, 360f)),
                0f to 0f, 0.40f to 1f, 1f to 0.7f
            )
        }
        if (a <= 0.01f) return@forEach
        val d = density
        val x = size.width * fx + 9f * ph * d
        val y = size.height * fy - 34f * ph * d
        drawCircle(
            brush = Brush.radialGradient(
                listOf(Color(0xE6FFEAC4), Color.Transparent),
                center = Offset(x, y),
                radius = 5f * d
            ),
            radius = 5f * d,
            center = Offset(x, y)
        )
        drawCircle(
            color = Color(0xE6FFEAC4),
            radius = 1.25f * d,
            center = Offset(x, y)
        )
    }
}

/* ------------------------------------------------------------------ */
/* The vessel                                                          */
/* ------------------------------------------------------------------ */

@Composable
private fun BoxScope.TerVessel(
    enterMs: Float,
    exitMs: Float,
    leaving: Boolean,
    phases: TerPhases,
    modifier: Modifier = Modifier
) {
    Box(modifier.width(122.dp).height(138.dp)) {
        // The glass, its brass collar and its finial drop in as one piece.
        Box(
            Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 24.dp)
                .width(106.dp)
                .height(108.dp)
                .graphicsLayer {
                    if (leaving) {
                        val p = terEaseIn(terStage(exitMs, 300f, 450f))
                        alpha = keyframe(p, 0f to 1f, 1f to 0f)
                        val s = keyframe(p, 0f to 1f, 1f to 0.86f)
                        scaleX = s
                        scaleY = s
                        translationY = 26f * p.dp.toPx()
                    } else {
                        val p = terEaseOut(terStage(enterMs, 300f, 480f))
                        alpha = keyframe(p, 0f to 0f, 0.58f to 1f, 1f to 1f)
                        val s = keyframe(p, 0f to 0.86f, 0.58f to 1.02f, 0.74f to 0.995f, 0.88f to 1.004f, 1f to 1f)
                        scaleX = s
                        scaleY = s
                        rotationZ = keyframe(p, 0f to -3f, 0.58f to 1.7f, 0.74f to -1f, 0.88f to 0.5f, 1f to 0f)
                        translationY = keyframe(p, 0f to -48f, 0.58f to 4f, 0.74f to -2f, 0.88f to 1f, 1f to 0f).dp.toPx()
                    }
                }
        ) {
            Canvas(Modifier.fillMaxSize()) {
                drawTerJar(
                    phases = phases,
                    enterMs = enterMs,
                    exitMs = exitMs,
                    leaving = leaving
                )
            }
            // The brass collar the dome plugs down into, and the finial on it.
            val collarBrush = with(LocalDensity.current) {
                Brush.horizontalGradient(
                    listOf(TerBrass0, TerBrass1, TerBrass2, TerBrass3),
                    startX = 0f,
                    endX = 15.dp.toPx()
                )
            }
            Box(
                Modifier
                    .align(Alignment.TopCenter)
                    .offset(y = (-4).dp)
                    .size(width = 15.dp, height = 10.dp)
                    .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp, bottomEnd = 2.dp, bottomStart = 2.dp))
                    .background(collarBrush)
            )
            Box(
                Modifier
                    .align(Alignment.TopCenter)
                    .offset(y = (-18).dp)
                    .size(13.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            listOf(Color(0xFFF6DCA4), Color(0xFFB98F3F), Color(0xFF6A4E1E)),
                            center = Offset(4f, 4f),
                            radius = 8f
                        )
                    )
            )
        }

        // The turned walnut base.
        Canvas(
            Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .height(24.dp)
        ) {
            drawTerBase(enterMs, exitMs, leaving)
        }
    }
}

/** The bell jar: the glass, the world inside and the light on it. */
private fun DrawScope.drawTerJar(
    phases: TerPhases,
    enterMs: Float,
    exitMs: Float,
    leaving: Boolean
) {
    val w = size.width
    val h = size.height
    val d = density
    val jar = terJarPath(w, h, d)

    // the glass itself
    drawPath(
        path = jar,
        brush = Brush.verticalGradient(
            listOf(Color(0x571A3028), Color(0x7510221C)),
            startY = 0f,
            endY = h
        )
    )
    drawPath(
        path = jar,
        brush = Brush.verticalGradient(
            listOf(Color(0x1AC4E8D6), Color(0x0A96C8B0)),
            startY = 0f,
            endY = h
        )
    )
    // the green glow pooling at the bottom
    drawPath(
        path = jar,
        brush = Brush.radialGradient(
            listOf(Color(0x2978D2A0), Color.Transparent),
            center = Offset(w * 0.5f, h),
            radius = w * 0.80f
        )
    )

    clipPath(jar) {
        // the diagonal streak of light on the glass
        rotate(-12f, Offset(w * 0.16f, h * 0.5f)) {
            drawRect(
                brush = Brush.horizontalGradient(
                    listOf(Color(0x33E2F8EC), Color(0x00E2F8EC)),
                    startX = 0f,
                    endX = 12f * d
                ),
                topLeft = Offset(w * 0.10f, -10f * d),
                size = Size(12f * d, h + 20f * d)
            )
        }

        // the soil bed
        val soilH = 26f * d
        drawPath(
            path = terMoundPath(0f, h - soilH, w, soilH, w * 0.40f, soilH * 0.60f),
            brush = Brush.verticalGradient(
                listOf(Color(0xFF3A2A1C), Color(0xFF1C1410)),
                startY = h - soilH,
                endY = h
            )
        )
        listOf(0.20f to 0.32f, 0.52f to 0.24f, 0.78f to 0.36f).forEach { (px, py) ->
            drawCircle(
                color = Color(0xFF4A3524),
                radius = 2.4f * d,
                center = Offset(w * px, h - soilH + soilH * py)
            )
        }

        // the moss spreading over it
        val mossP = if (leaving) {
            1f - terEaseIn(terStage(exitMs, 210f, 300f))
        } else {
            terEaseOut(terStage(enterMs, 630f, 420f))
        }
        if (mossP > 0.01f) {
            val mossH = 22f * d
            val mossW = w - 2f * d
            val s = keyframe(mossP, 0f to 0f, 0.60f to 1.06f, 1f to 1f)
            clipRect(
                left = w * 0.5f - mossW * s * 0.5f,
                top = h - 12f * d - mossH,
                right = w * 0.5f + mossW * s * 0.5f,
                bottom = h
            ) {
                drawPath(
                    path = terMoundPath(d, h - 12f * d - mossH, mossW, mossH, mossW * 0.5f, mossH * 0.72f),
                    brush = Brush.verticalGradient(
                        listOf(Color(0xFF4E9A63), Color(0xFF2A5738)),
                        startY = h - 12f * d - mossH,
                        endY = h - 12f * d
                    )
                )
                listOf(0.18f to 0.42f, 0.44f to 0.28f, 0.72f to 0.46f).forEach { (px, py) ->
                    drawCircle(
                        color = Color(0xFF6FCB86),
                        radius = 2.2f * d,
                        center = Offset(mossW * px, h - 12f * d - mossH + mossH * py)
                    )
                }
            }
        }

        // the fern
        val stemP = if (leaving) {
            1f - terEaseIn(terStage(exitMs, 180f, 330f))
        } else {
            terEaseOut(terStage(enterMs, 720f, 480f))
        }
        val fernX = 18f * d
        val fernBottom = h - 22f * d
        val stemH = 64f * d
        val stemW = 3f * d
        val fernMid = fernX + 29f * d
        if (stemP > 0.01f) {
            val sh = stemH * keyframe(stemP, 0f to 0f, 0.70f to 1.12f, 1f to 1f)
            drawRoundRect(
                brush = Brush.verticalGradient(
                    listOf(Color(0xFF9BE0A8), Color(0xFF3E8A56)),
                    startY = fernBottom - sh,
                    endY = fernBottom
                ),
                topLeft = Offset(fernMid - stemW * 0.5f, fernBottom - sh),
                size = Size(stemW, sh),
                cornerRadius = CornerRadius(stemW * 0.5f, stemW * 0.5f)
            )
        }
        // the leaflets, one after another
        val leafBase = listOf(12f, 26f, 38f, 50f, 58f)
        leafBase.forEachIndexed { i, byDp ->
            val delay = 810f + i * 90f
            val p = if (leaving) {
                1f - terEaseIn(terStage(exitMs, 180f + i * 36f, 270f))
            } else {
                terEaseOut(terStage(enterMs, delay, 270f))
            }
            if (p <= 0.01f) return@forEachIndexed
            val mirrored = i % 2 == 1
            val s = keyframe(p, 0f to 0.1f, 0.60f to 1.22f, 1f to 1f)
            val rot = keyframe(p, 0f to -38f, 0.60f to 7f, 1f to 0f)
            val bx = fernMid
            val byy = fernBottom - byDp * d
            val lw = 18f * d * s
            val lh = 9f * d * s
            rotate(rot, Offset(bx, byy)) {
                val dir = if (mirrored) -1f else 1f
                drawPath(
                    path = terLeafPath(bx, byy, lw * dir, lh),
                    brush = Brush.linearGradient(
                        listOf(Color(0xFF8FE3A2), Color(0xFF3F8F58)),
                        start = Offset(bx, byy),
                        end = Offset(bx + lw * dir, byy - lh)
                    ),
                    alpha = p
                )
            }
        }

        // the mushrooms
        val shrooms = listOf(
            Triple(9f, 17f, 1f),
            Triple(56f, 21f, 0.78f),
            Triple(37f, 15f, 0.60f)
        )
        shrooms.forEachIndexed { i, (mxDp, myDp, sc) ->
            val p = if (leaving) {
                1f - terEaseIn(terStage(exitMs, 132f + i * 36f, 240f))
            } else {
                terEaseOut(terStage(enterMs, 870f + i * 102f, 300f))
            }
            if (p <= 0.01f) return@forEachIndexed
            val s = keyframe(p, 0f to 0f, 0.60f to 1.2f, 1f to 1f)
            val cw = 16f * d * sc * s
            val ch = 10f * d * sc * s
            val cx = mxDp * d + 8f * d * sc
            val cy = h - myDp * d
            // the stalk
            drawRoundRect(
                color = Color(0xFFF2E0BE),
                topLeft = Offset(cx - 2f * d * sc * s, cy - 8f * d * sc * s),
                size = Size(4f * d * sc * s, 9f * d * sc * s),
                cornerRadius = CornerRadius(2f * d, 2f * d)
            )
            // the cap
            drawPath(
                path = terCapPath(cx, cy - 7f * d * sc * s, cw, ch),
                brush = Brush.radialGradient(
                    listOf(Color(0xFFFFDCA6), Color(0xFFD9762F), Color(0xFF8A3F16)),
                    center = Offset(cx - cw * 0.14f, cy - 11f * d * sc * s),
                    radius = cw
                )
            )
            // the glow under the cap
            drawCircle(
                brush = Brush.radialGradient(
                    listOf(Color(0x59FFA84E), Color.Transparent),
                    center = Offset(cx, cy - 6f * d * sc * s),
                    radius = 14f * d * sc * s
                ),
                radius = 14f * d * sc * s,
                center = Offset(cx, cy - 6f * d * sc * s)
            )
        }

        // the spores
        val spores = listOf(
            Triple(0.22f, 0.26f, 0.900f),
            Triple(0.62f, 0.18f, 1.008f),
            Triple(0.40f, 0.40f, 1.104f),
            Triple(0.76f, 0.44f, 1.200f),
            Triple(0.30f, 0.12f, 1.284f)
        )
        spores.forEachIndexed { i, (fx, fy, delay) ->
            val p = if (leaving) {
                1f - terStage(exitMs, 30f + i * 42f, 480f)
            } else {
                terEaseOut(terStage(enterMs, delay * 1000f, 300f))
            }
            if (p <= 0.01f) return@forEachIndexed
            val drift = phases.spore
            val x = w * fx + keyframe(drift, 0f to 0f, 0.5f to 5f, 1f to -3f) * d
            val y = h * fy + keyframe(drift, 0f to 0f, 0.5f to -12f, 1f to -22f) * d
            val r = 3.5f * d * keyframe(p, 0f to 0.2f, 0.40f to 1.5f, 1f to 1f)
            drawCircle(
                brush = Brush.radialGradient(
                    listOf(Color(0xBEE6FFD4), Color.Transparent),
                    center = Offset(x, y),
                    radius = r * 2.6f
                ),
                radius = r * 2.6f,
                center = Offset(x, y)
            )
            drawCircle(
                color = Color(0xBEE6FFD4),
                radius = r,
                center = Offset(x, y)
            )
        }

        // the light sliding down the glass
        drawRoundRect(
            brush = Brush.verticalGradient(
                listOf(Color(0x70FFFFFF), Color(0x0DFFFFFF)),
                startY = h * 0.09f,
                endY = h * 0.65f
            ),
            topLeft = Offset(w * 0.12f, h * 0.09f),
            size = Size(8f * d, h * 0.56f),
            cornerRadius = CornerRadius(4f * d, 4f * d)
        )
        drawRoundRect(
            color = Color(0x80FFFFFF),
            topLeft = Offset(w * 0.23f, h * 0.15f),
            size = Size(4f * d, h * 0.28f),
            cornerRadius = CornerRadius(2f * d, 2f * d),
            alpha = 0.5f
        )
    }

    // the rim of the glass
    drawPath(
        path = jar,
        color = Color(0x4DC6ECD6),
        style = Stroke(width = 1f * d)
    )
}

/** The walnut base with its brass collar. */
private fun DrawScope.drawTerBase(enterMs: Float, exitMs: Float, leaving: Boolean) {
    val p = if (leaving) {
        terEaseIn(terStage(exitMs, 450f, 330f))
    } else {
        terEaseOut(terStage(enterMs, 210f, 360f))
    }
    val a = if (leaving) 1f - p else p
    if (a <= 0.01f) return
    val d = density
    val ty = (if (leaving) 22f * p else 26f * (1f - p)) * d
    val sx = if (leaving) 1f - 0.08f * p else 0.92f + 0.08f * p
    val w = size.width * sx
    val x0 = (size.width - w) * 0.5f
    val h = size.height

    drawRoundRect(
        brush = Brush.verticalGradient(
            listOf(Color(0xFF6E4C2E), Color(0xFF4A3220), Color(0xFF26190F)),
            startY = 0f,
            endY = h
        ),
        topLeft = Offset(x0, ty),
        size = Size(w, h),
        cornerRadius = CornerRadius(5f * d, 5f * d),
        alpha = a
    )
    // the brass band
    drawRect(
        brush = Brush.horizontalGradient(
            listOf(TerBrass0, TerBrass1, TerBrass2, TerBrass3),
            startX = x0 + 4f * d,
            endX = x0 + w - 4f * d
        ),
        topLeft = Offset(x0 + 4f * d, 5f * d + ty),
        size = Size(w - 8f * d, 3f * d),
        alpha = a
    )
    // the collar the glass sits down into
    drawRoundRect(
        brush = Brush.horizontalGradient(
            listOf(TerBrass0, TerBrass1, TerBrass2, TerBrass3),
            startX = x0 + 7f * d,
            endX = x0 + w - 7f * d
        ),
        topLeft = Offset(x0 + 7f * d, -5f * d + ty),
        size = Size(w - 14f * d, 8f * d),
        cornerRadius = CornerRadius(4f * d, 4f * d),
        alpha = a
    )
}

/* ------------------------------------------------------------------ */
/* Shapes                                                              */
/* ------------------------------------------------------------------ */

/** The bell: a dome over straight sides with a soft foot. */
private fun terJarPath(w: Float, h: Float, d: Float): Path {
    val ry = h * 0.42f
    val foot = 8f * d
    return Path().apply {
        // a true half ellipse, so the apex really is the top of the box
        addArc(
            oval = Rect(0f, 0f, w, ry * 2f),
            startAngleDegrees = 180f,
            sweepAngleDegrees = 180f
        )
        lineTo(w, h - foot)
        quadraticBezierTo(w, h, w - foot, h)
        lineTo(foot, h)
        quadraticBezierTo(0f, h, 0f, h - foot)
        close()
    }
}

/** A mound with a rounded top — the soil and the moss. */
private fun terMoundPath(x0: Float, y0: Float, w: Float, h: Float, rx: Float, ry: Float): Path {
    return Path().apply {
        moveTo(x0, y0 + h)
        lineTo(x0, y0 + ry)
        quadraticBezierTo(x0, y0, x0 + rx, y0)
        lineTo(x0 + w - rx, y0)
        quadraticBezierTo(x0 + w, y0, x0 + w, y0 + ry)
        lineTo(x0 + w, y0 + h)
        close()
    }
}

/** A leaf, pointing up and to the right from its base. */
private fun terLeafPath(bx: Float, by: Float, w: Float, h: Float): Path {
    return Path().apply {
        moveTo(bx, by)
        quadraticBezierTo(bx + w, by, bx + w, by - h)
        quadraticBezierTo(bx, by - h, bx, by)
        close()
    }
}

/** A mushroom cap: a dome with a flat foot. */
private fun terCapPath(cx: Float, baseY: Float, w: Float, h: Float): Path {
    return Path().apply {
        moveTo(cx - w * 0.5f, baseY)
        quadraticBezierTo(cx - w * 0.5f, baseY - h, cx, baseY - h)
        quadraticBezierTo(cx + w * 0.5f, baseY - h, cx + w * 0.5f, baseY)
        close()
    }
}

/* ------------------------------------------------------------------ */
/* The note                                                            */
/* ------------------------------------------------------------------ */

@Composable
private fun TerSenderRow(
    data: TickerData,
    showTime: Boolean,
    showLogo: Boolean,
    logoSize: Dp,
    enterMs: Float,
    exitMs: Float,
    leaving: Boolean
) {
    val letter = terLetter(enterMs, exitMs, leaving, 21f)
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        if (showLogo) {
            Box(Modifier.size(22.dp).clip(CircleShape), contentAlignment = Alignment.Center) {
                AppBadge(data, 22.dp, background = Color(0xFF1E3329), letterColor = Color(0xFFBFE8C6))
            }
            Spacer(Modifier.width(8.dp))
        }
        Text(
            data.title,
            color = TerInk,
            fontSize = 21.sp,
            fontFamily = ThemeFonts.CormorantItalic,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = letter.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f, fill = false)
        )
        if (showTime) {
            Spacer(Modifier.weight(1f))
            Spacer(Modifier.width(8.dp))
            Text(
                data.time.uppercase(),
                color = TerGold,
                fontSize = 9.sp,
                fontFamily = ThemeFonts.Nunito,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 1.5.sp,
                maxLines = 1,
                style = TextStyle(shadow = Shadow(TerGold.copy(alpha = 0.45f), Offset.Zero, 10f))
            )
        }
    }
}

/** The letter-spacing the writing condenses from. */
private fun terLetter(enterMs: Float, exitMs: Float, leaving: Boolean, sizeSp: Float): Float {
    val em = if (leaving) {
        val p = terEaseIn(terStage(exitMs, 0f, 300f))
        keyframe(p, 0f to 0f, 1f to 0.34f)
    } else {
        val p = terEaseOut(terStage(enterMs, 720f, 600f))
        keyframe(p, 0f to 0.26f, 0.30f to 0.12f, 0.70f to 0.02f, 1f to 0f)
    }
    return em * sizeSp
}

/* ------------------------------------------------------------------ */
/* The dock                                                            */
/* ------------------------------------------------------------------ */

@Composable
private fun BoxScope.TerDock(
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
        terEaseIn(terStage(exitMs, 0f, 240f))
    } else {
        terEaseOut(terStage(enterMs, 960f, 300f))
    }
    val dockAlpha = if (leaving) 1f - dockP else dockP

    Row(
        modifier.graphicsLayer {
            alpha = dockAlpha
            translationY = (if (leaving) 12f * dockP else 10f * (1f - dockP)).dp.toPx()
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
            val bg = if (read) Color(0xB83A2E18) else Color(0xBD10241A)
            val bd = if (read) Color(0xBFD6B060) else Color(0x80D6B060)

            val keyP = if (leaving) {
                terEaseIn(terStage(exitMs, index * 36f, 210f))
            } else {
                terEaseOut(terStage(enterMs, 996f + index * 72f, 270f))
            }

            Box(
                Modifier
                    .graphicsLayer {
                        if (leaving) {
                            alpha = 1f - keyP
                            val s = keyframe(keyP, 0f to 1f, 1f to 0.4f)
                            scaleX = s
                            scaleY = s
                            rotationZ = keyframe(keyP, 0f to 0f, 1f to 12f)
                        } else {
                            alpha = keyframe(keyP, 0f to 0f, 0.60f to 1f, 1f to 1f)
                            val s = keyframe(keyP, 0f to 0.4f, 0.60f to 1.1f, 1f to 1f)
                            scaleX = s
                            scaleY = s
                            rotationZ = keyframe(keyP, 0f to -16f, 0.60f to 3f, 1f to 0f)
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
                    .padding(horizontal = 15.dp, vertical = 9.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    spec.label.uppercase(),
                    color = Color(0xFFF3E4C4),
                    fontSize = (10f * scale).sp,
                    fontFamily = ThemeFonts.QuicksandBold,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.2.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}
