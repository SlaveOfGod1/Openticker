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
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shadow
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
/* 15 — Frost Pane (winter night outside the house)                      */
/* ==================================================================== */
/*
 * A winter night seen from outside the house: a moon, a far ridge, four
 * spruces with little stoves burning on them and three layers of snow
 * drifting across the note. The buttons are blocks of packed ice.
 *
 * Appearing: the stars come out, the moon rises, the ridge comes up, the
 * spruces rise out of the snow and their stoves catch, the drifts stack
 * up and the whole note arrives at once.
 * Leaving: the stoves go out, the snow sinks back and the night takes it.
 */

private val FrostSky0 = Color(0xFF071527)
private val FrostSky1 = Color(0xFF0B2137)
private val FrostSky2 = Color(0xFF143852)
private val FrostSky3 = Color(0xFF1D4262)
private val FrostMoon = Color(0xFFFDFEFF)
private val FrostStar = Color(0xFFE2F0FF)
private val FrostHill0 = Color(0xFF7FA0C0)
private val FrostHill1 = Color(0xFF43607E)
private val FrostTree0 = Color(0xFF1B3A56)
private val FrostTree1 = Color(0xFF0B1F31)
private val FrostDrift0 = Color(0xFFB9CFE4)
private val FrostDrift1 = Color(0xFF93B2CE)
private val FrostDrift2 = Color(0xFFD8E7F5)
private val FrostDrift3 = Color(0xFFB3CBE1)
private val FrostDrift4 = Color(0xFFF0F7FF)
private val FrostDrift5 = Color(0xFFCFE0F0)
private val FrostInk = Color(0xFFEAF6FF)
private val FrostInkSoft = Color(0xFFB6D2E8)
private val FrostFire = Color(0xFFFFD79A)
private val FrostFireDeep = Color(0xFFFF9B3D)

/** The whole choreography, in milliseconds. */
private const val FrostTimelineMs = 1350f

/** The way out. */
private const val FrostExitMs = 620f

/* ------------------------------------------------------------------ */
/* The cast                                                            */
/* ------------------------------------------------------------------ */

private data class FrostTree(
    val right: Float,
    val bottom: Float,
    val w: Float,
    val h: Float,
    val stove: Boolean
)

private val FrostTrees = listOf(
    FrostTree(0.02f, 24f, 58f, 84f, true),
    FrostTree(0.22f, 22f, 46f, 66f, true),
    FrostTree(0.38f, 20f, 36f, 52f, true),
    FrostTree(0.51f, 18f, 28f, 40f, false)
)

private data class FrostFlake(val x: Float, val size: Float, val layer: Int, val offset: Float)

private val FrostFlakes = listOf(
    // far: small and dim
    FrostFlake(0.04f, 2f, 2, 0.04f),
    FrostFlake(0.13f, 2.5f, 2, 0.30f),
    FrostFlake(0.21f, 2f, 2, 0.66f),
    FrostFlake(0.29f, 2.5f, 2, 0.17f),
    FrostFlake(0.37f, 2f, 2, 0.82f),
    FrostFlake(0.45f, 2.5f, 2, 0.45f),
    FrostFlake(0.53f, 2f, 2, 0.21f),
    FrostFlake(0.61f, 2.5f, 2, 0.90f),
    FrostFlake(0.69f, 2f, 2, 0.53f),
    FrostFlake(0.77f, 2.5f, 2, 0.72f),
    FrostFlake(0.85f, 2f, 2, 0.09f),
    FrostFlake(0.94f, 2.5f, 2, 0.98f),
    // middle
    FrostFlake(0.09f, 3.5f, 1, 0.16f),
    FrostFlake(0.19f, 4f, 1, 0.50f),
    FrostFlake(0.31f, 3f, 1, 0.23f),
    FrostFlake(0.43f, 4f, 1, 0.69f),
    FrostFlake(0.56f, 3.5f, 1, 0.40f),
    FrostFlake(0.67f, 3f, 1, 0.83f),
    FrostFlake(0.79f, 4f, 1, 0.07f),
    FrostFlake(0.91f, 3.5f, 1, 0.58f),
    // near: big, blurred and driven by the wind
    FrostFlake(0.16f, 6f, 0, 0.20f),
    FrostFlake(0.48f, 7f, 0, 0.48f),
    FrostFlake(0.73f, 5.5f, 0, 0.31f),
    FrostFlake(0.96f, 6.5f, 0, 0.65f)
)

/* ------------------------------------------------------------------ */
/* Timing helpers                                                      */
/* ------------------------------------------------------------------ */

private fun frostStage(ms: Float, delay: Float, dur: Float): Float =
    ((ms - delay) / dur).coerceIn(0f, 1f)

private fun frostEaseOut(x: Float): Float {
    val c = x.coerceIn(0f, 1f)
    val inv = 1f - c
    return 1f - inv * inv * inv
}

private fun frostEaseIn(x: Float): Float {
    val c = x.coerceIn(0f, 1f)
    return c * c * c
}

/* ------------------------------------------------------------------ */
/* Ambient loops                                                       */
/* ------------------------------------------------------------------ */

private data class FrostPhases(
    val near: Float = 0f,
    val mid: Float = 0f,
    val far: Float = 0f,
    val smoke: Float = 0f
)

@Composable
private fun rememberFrostPhases(effect: Boolean): FrostPhases {
    val speed = rememberAnimationSpeed()
    fun d(ms: Int) = scaleDuration(ms, speed)
    val t = rememberInfiniteTransition(label = "frost")

    val near by t.animateFloat(
        0f, 1f,
        infiniteRepeatable(tween(d(6000), easing = LinearEasing)),
        label = "near"
    )
    val mid by t.animateFloat(
        0f, 1f,
        infiniteRepeatable(tween(d(9000), easing = LinearEasing)),
        label = "mid"
    )
    val far by t.animateFloat(
        0f, 1f,
        infiniteRepeatable(tween(d(12000), easing = LinearEasing)),
        label = "far"
    )
    val smoke by t.animateFloat(
        0f, 1f,
        infiniteRepeatable(tween(d(5500), easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "smoke"
    )

    return FrostPhases(
        near = if (effect) near else 0.3f,
        mid = if (effect) mid else 0.5f,
        far = if (effect) far else 0.7f,
        smoke = if (effect) smoke else 0.4f
    )
}

/* ==================================================================== */
/* Card                                                                */
/* ==================================================================== */

@Composable
fun CuratedFrostTicker(
    data: TickerData,
    interactions: TickerInteractions?,
    animateIn: Boolean,
    exitFraction: () -> Float
) {
    val context = LocalContext.current
    val opts = Prefs.themeOptions(context, "frost", 0xFF3A6E94.toInt())
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
        enter.animateTo(1f, tween(scaleDuration(FrostTimelineMs.toInt(), speed), easing = LinearEasing))
    }
    val enterMs = enter.value * FrostTimelineMs
    val exitMs = ex * FrostExitMs

    val phases = rememberFrostPhases(opts.effect)

    CardFrame(
        modifier = Modifier.frostCard(enterMs, exitMs, leaving),
        lines = lines,
        hasDock = hasDock,
        // One line of text plus the dock; every extra line adds 16dp. The card
        // is snug, so the buttons sit right under the message.
        baseHeight = 152.dp,
        dockSpace = 60.dp,
        decorationAlpha = intensity / 100f,
        shape = RoundedCornerShape(16.dp),
        background = Brush.verticalGradient(
            listOf(FrostSky0, FrostSky1, FrostSky2, FrostSky3)
        ),
        decoration = {
            // the glow of the moon and the cold light behind it
            drawRect(
                Brush.radialGradient(
                    listOf(Color(0x3396C4F6), Color.Transparent),
                    center = Offset(size.width * 0.74f, -size.height * 0.08f),
                    radius = size.width * 0.63f
                )
            )
            drawRect(
                Brush.radialGradient(
                    listOf(Color(0x1F6094D2), Color.Transparent),
                    center = Offset(size.width * 0.22f, size.height * 0.08f),
                    radius = size.width * 0.50f
                )
            )
        }
    ) {
        Box(Modifier.fillMaxSize()) {
            // The winter scene.
            Canvas(Modifier.fillMaxSize().graphicsLayer { alpha = intensity / 100f }) {
                drawFrostScene(enterMs, exitMs, leaving, phases)
            }

            // The note, over the sky.
            Column(
                Modifier
                    .align(Alignment.TopStart)
                    .fillMaxWidth()
                    .padding(start = 24.dp, top = 22.dp)
                    .graphicsLayer {
                        val p = if (leaving) {
                            frostEaseIn(frostStage(exitMs, 0f, 300f))
                        } else {
                            frostEaseOut(frostStage(enterMs, 780f, 400f))
                        }
                        alpha = if (leaving) 1f - p else p
                    }
            ) {
                CardSenderRow(
                    title = data.title,
                    showTime = showTime,
                    showLogo = showLogo,
                    logoSize = logoSize,
                    logoData = data,
                    time = data.time,
                    titleColor = FrostInk,
                    timeColor = Color(0xFFFFE0B0),
                    titleFont = ThemeFonts.QuicksandBold,
                    timeFont = ThemeFonts.Nunito,
                    titleSize = 19f,
                    timeSize = 9f,
                    titleWeight = FontWeight.Bold,
                    titleStyle = TextStyle(
                        shadow = Shadow(Color(0x6696D2FF), Offset.Zero, 16f)
                    ),
                    timeStyle = TextStyle(
                        shadow = Shadow(Color(0x99FFBE6E), Offset.Zero, 12f)
                    )
                )
                Spacer(Modifier.height(5.dp))
                CardBody(
                    text = data.body,
                    lines = lines,
                    color = FrostInkSoft,
                    font = ThemeFonts.Quicksand,
                    weight = FontWeight.SemiBold,
                    sizeSp = 12f,
                    lineHeightFactor = 1.3333f,
                    style = TextStyle(shadow = Shadow(Color(0xE6020C18), Offset(0f, 2f), 8f))
                )
            }

            if (hasDock) {
                FrostDock(
                    data = data,
                    interactions = interactions,
                    scale = scale,
                    enterMs = enterMs,
                    exitMs = exitMs,
                    leaving = leaving,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(start = 24.dp, end = 24.dp, bottom = 18.dp)
                )
            }
        }
    }
}

/**
 * The card arrives like a native notification and never darkens to black.
 * On the way out the night simply closes over it.
 */
private fun Modifier.frostCard(enterMs: Float, exitMs: Float, leaving: Boolean): Modifier =
    graphicsLayer {
        if (leaving) {
            val p = frostEaseIn(frostStage(exitMs, 120f, 440f))
            alpha = 1f - p
            scaleY = 1f - 0.08f * p
            translationY = 18f * p
        } else {
            alpha = frostEaseOut(frostStage(enterMs, 0f, 250f))
        }
    }

/* ------------------------------------------------------------------ */
/* The scene                                                           */
/* ------------------------------------------------------------------ */

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawFrostScene(
    enterMs: Float,
    exitMs: Float,
    leaving: Boolean,
    phases: FrostPhases
) {
    val d = density
    val w = size.width
    val h = size.height

    // ---- the stars -------------------------------------------------
    val stars = listOf(
        Triple(0.14f, 0.12f, 70f),
        Triple(0.33f, 0.07f, 130f),
        Triple(0.52f, 0.16f, 180f),
        Triple(0.88f, 0.24f, 230f)
    )
    stars.forEach { (fx, fy, delay) ->
        val p = if (leaving) {
            1f - frostEaseIn(frostStage(exitMs, 200f, 200f))
        } else {
            frostEaseOut(frostStage(enterMs, delay, 300f))
        }
        if (p <= 0.01f) return@forEach
        val s = keyframe(p, 0f to 0.2f, 0.4f to 1.6f, 1f to 1f)
        drawCircle(
            color = FrostStar.copy(alpha = (p * 0.8f).coerceIn(0f, 1f)),
            radius = 1.4f * d * s,
            center = Offset(w * fx, h * fy)
        )
    }

    // ---- the moon --------------------------------------------------
    val moonP = if (leaving) {
        1f - frostEaseIn(frostStage(exitMs, 180f, 250f))
    } else {
        frostEaseOut(frostStage(enterMs, 50f, 450f))
    }
    if (moonP > 0.01f) {
        val mr = 17f * d
        val mc = Offset(w * 0.88f - mr, h * 0.08f + mr)
        drawCircle(
            brush = Brush.radialGradient(
                listOf(
                    FrostMoon.copy(alpha = 0.95f * moonP),
                    Color(0x8CE2F0FF).copy(alpha = 0.55f * moonP),
                    Color.Transparent
                ),
                center = mc,
                radius = mr
            ),
            radius = mr,
            center = mc
        )
    }

    // ---- the far ridge ---------------------------------------------
    val hillP = if (leaving) {
        1f - frostEaseIn(frostStage(exitMs, 90f, 320f))
    } else {
        frostEaseOut(frostStage(enterMs, 140f, 450f))
    }
    if (hillP > 0.01f) {
        val hh = 86f * d
        val path = frostMoundPath(-w * 0.14f, h - 26f * d - hh, w * 1.28f, hh, 62f * d, 0.5f)
        drawPath(
            path = path,
            brush = Brush.verticalGradient(
                listOf(FrostHill0, FrostHill1),
                startY = h - 26f * d - hh,
                endY = h - 26f * d
            ),
            alpha = hillP
        )
    }

    // ---- the spruces -----------------------------------------------
    val treeP = listOf(250f, 320f, 380f, 450f)
    FrostTrees.forEachIndexed { i, tree ->
        val p = if (leaving) {
            1f - frostEaseIn(frostStage(exitMs, 60f, 300f))
        } else {
            frostEaseOut(frostStage(enterMs, treeP[i], 400f))
        }
        if (p <= 0.01f) return@forEachIndexed
        val tw = tree.w * d
        val th = tree.h * d
        val left = w * (1f - tree.right) - tw
        val base = h - tree.bottom * d
        val top = base - th
        val scaleY = keyframe(p, 0f to 0.04f, 1f to 1f)
        // the tree grows up out of the snow, its feet staying put
        val spruce = frostSprucePath(left, top + th * (1f - scaleY), tw, th * scaleY)

        drawPath(
            path = spruce,
            brush = Brush.verticalGradient(
                listOf(FrostTree0, FrostTree1),
                startY = top,
                endY = base
            ),
            alpha = p
        )
    }

    // ---- the stoves -------------------------------------------------
    FrostTrees.forEachIndexed { i, tree ->
        if (!tree.stove) return@forEachIndexed
        val p = if (leaving) {
            1f - frostEaseIn(frostStage(exitMs, i * 40f, 260f))
        } else {
            frostEaseOut(frostStage(enterMs, 380f + i * 90f, 350f))
        }
        if (p <= 0.01f) return@forEachIndexed
        val tw = tree.w * d
        val th = tree.h * d
        val left = w * (1f - tree.right) - tw
        val base = h - tree.bottom * d
        val sx = left + tw * 0.62f
        val sy = base - th * 0.46f
        val sw = 13f * d
        val sh = 11f * d
        val flick = keyframe(p, 0f to 0.4f, 0.3f to 1.2f, 0.55f to 0.94f, 1f to 1f)

        // the glow behind the little stove
        drawCircle(
            brush = Brush.radialGradient(
                listOf(Color(0x99FFA84E).copy(alpha = 0.5f * p), Color.Transparent),
                center = Offset(sx + sw * 0.5f, sy + sh * 0.5f),
                radius = 16f * d
            ),
            radius = 16f * d,
            center = Offset(sx + sw * 0.5f, sy + sh * 0.5f)
        )
        // the firebox
        drawRoundRect(
            brush = Brush.verticalGradient(
                listOf(Color(0xFF7A563A), Color(0xFF402A19)),
                startY = sy,
                endY = sy + sh
            ),
            topLeft = Offset(sx, sy),
            size = Size(sw, sh),
            cornerRadius = CornerRadius(2f * d, 2f * d)
        )
        // the fire inside
        drawRoundRect(
            color = FrostFire.copy(alpha = p),
            topLeft = Offset(sx + 2f * d, sy + 3f * d),
            size = Size(sw - 4f * d, 5f * d),
            cornerRadius = CornerRadius(1f * d, 1f * d)
        )
        drawRoundRect(
            brush = Brush.verticalGradient(
                listOf(FrostFire, FrostFireDeep),
                startY = sy + 3f * d,
                endY = sy + 8f * d
            ),
            topLeft = Offset(sx + 2f * d, sy + 3f * d),
            size = Size(sw - 4f * d, 5f * d),
            cornerRadius = CornerRadius(1f * d),
            alpha = flick
        )
        // the pipe
        drawRect(
            color = Color(0xFF3A2718),
            topLeft = Offset(sx + sw * 0.5f - 1.5f * d, sy - 7f * d),
            size = Size(3f * d, 8f * d)
        )
    }

    // ---- the smoke --------------------------------------------------
    FrostTrees.forEachIndexed { i, tree ->
        if (!tree.stove) return@forEachIndexed
        val tw = tree.w * d
        val th = tree.h * d
        val left = w * (1f - tree.right) - tw
        val base = h - tree.bottom * d
        val sx = left + tw * 0.62f + 6.5f * d
        val sy = base - th * 0.46f - 9f * d
        val ph = (phases.smoke + i * 0.33f) % 1f
        val a = if (ph < 0.2f) ph / 0.2f * 0.55f else 0.55f * (1f - (ph - 0.2f) / 0.8f)
        if (a <= 0.01f) return@forEachIndexed
        val rise = -42f * d * ph
        val drift = -14f * d * ph
        val rr = (5.5f + 4f * ph) * d
        drawCircle(
            brush = Brush.radialGradient(
                listOf(Color(0x8CD8E4F0).copy(alpha = a), Color.Transparent),
                center = Offset(sx + drift, sy + rise),
                radius = rr
            ),
            radius = rr,
            center = Offset(sx + drift, sy + rise)
        )
    }

    // ---- the snow layers --------------------------------------------
    val drifts = listOf(
        // height, appear delay, leave delay, colours
        Quad(52f, 430f, 100f, FrostDrift0 to FrostDrift1),
        Quad(38f, 520f, 50f, FrostDrift2 to FrostDrift3),
        Quad(26f, 610f, 0f, FrostDrift4 to FrostDrift5)
    )
    drifts.forEach { drift ->
        val p = if (leaving) {
            1f - frostEaseIn(frostStage(exitMs, drift.b, 320f))
        } else {
            frostEaseOut(frostStage(enterMs, drift.a, 400f))
        }
        if (p <= 0.01f) return@forEach
        val hh = drift.a0 * d
        val path = frostMoundPath(-w * 0.12f, h - hh, w * 1.24f, hh, hh * 0.9f, 0.5f)
        drawPath(
            path = path,
            brush = Brush.verticalGradient(
                listOf(drift.d.first, drift.d.second),
                startY = h - hh,
                endY = h
            ),
            alpha = p
        )
    }

    // ---- the snow ---------------------------------------------------
    FrostFlakes.forEach { flake ->
        val clock = when (flake.layer) {
            0 -> phases.near
            1 -> phases.mid
            else -> phases.far
        }
        val ph = (clock + flake.offset) % 1f
        val a = if (flake.layer == 2) {
            (if (ph < 0.12f) ph / 0.12f else 1f - (ph - 0.12f) / 0.88f) * 0.6f
        } else {
            (if (ph < 0.12f) ph / 0.12f else 1f - (ph - 0.12f) / 0.88f) * 0.92f
        }
        if (a <= 0.01f) return@forEach
        val spread = if (flake.layer == 0) 60f * d else 28f * d
        val x = w * flake.x + (-spread * 0.4f + spread * ph)
        val y = -8f * d + (h + 40f * d) * ph
        val r = flake.size * 0.5f * d
        drawCircle(
            brush = Brush.radialGradient(
                listOf(
                    Color(0xE6F0FAFF).copy(alpha = a),
                    Color.Transparent
                ),
                center = Offset(x, y),
                radius = r * 2.6f
            ),
            radius = r * 2.6f,
            center = Offset(x, y)
        )
        drawCircle(
            color = Color(0xE6F0FAFF).copy(alpha = a),
            radius = r,
            center = Offset(x, y)
        )
    }
}

/** Four values: the first three are numbers, the last is the colour pair. */
private data class Quad(
    val a0: Float,
    val a: Float,
    val b: Float,
    val d: Pair<Color, Color>
)

/** The spruce silhouette — the same stepped outline as the HTML shape. */
private fun frostSprucePath(x: Float, y: Float, w: Float, h: Float): Path {
    val pts = listOf(
        0.50f to 0.00f,
        0.62f to 0.22f, 0.56f to 0.22f,
        0.69f to 0.45f, 0.61f to 0.45f,
        0.76f to 0.71f, 0.66f to 0.71f,
        0.84f to 1.00f,
        0.16f to 1.00f,
        0.34f to 0.71f, 0.24f to 0.71f,
        0.39f to 0.45f, 0.31f to 0.45f,
        0.44f to 0.22f, 0.38f to 0.22f
    )
    return Path().apply {
        moveTo(x + pts[0].first * w, y + pts[0].second * h)
        for (i in 1 until pts.size) lineTo(x + pts[i].first * w, y + pts[i].second * h)
        close()
    }
}

/** A rolling snow hill, drawn at (x, y) with the given width and height. */
private fun frostMoundPath(
    x: Float,
    y: Float,
    w: Float,
    h: Float,
    rise: Float,
    skew: Float
): Path {
    return Path().apply {
        moveTo(x, y + h)
        cubicTo(
            x + w * 0.16f, y + h - rise * 0.86f,
            x + w * 0.34f, y + h - rise,
            x + w * (0.5f + skew), y + h - rise * 0.88f
        )
        cubicTo(
            x + w * 0.68f, y + h - rise * 0.74f,
            x + w * 0.86f, y + h - rise * 0.94f,
            x + w, y + h
        )
        close()
    }
}

/* ------------------------------------------------------------------ */
/* The dock: blocks of packed ice                                     */
/* ------------------------------------------------------------------ */

@Composable
private fun BoxScope.FrostDock(
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
        frostEaseIn(frostStage(exitMs, 40f, 260f))
    } else {
        frostEaseOut(frostStage(enterMs, 900f, 300f))
    }
    val dockAlpha = if (leaving) 1f - dockP else dockP

    Row(
        modifier.graphicsLayer {
            alpha = dockAlpha
            translationY = (if (leaving) 10f * dockP else 12f * (1f - dockP)).dp.toPx()
        },
        horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically
    ) {
        specs.forEachIndexed { index, spec ->
            val source = remember { MutableInteractionSource() }
            val pressed by source.collectIsPressedAsState()
            val keyP = if (leaving) {
                frostEaseIn(frostStage(exitMs, 40f + index * 40f, 240f))
            } else {
                frostEaseOut(frostStage(enterMs, 940f + index * 60f, 300f))
            }

            Box(
                Modifier
                    .graphicsLayer {
                        if (leaving) {
                            alpha = 1f - keyP
                            val s = keyframe(keyP, 0f to 1f, 1f to 0.86f)
                            scaleX = s
                            scaleY = s
                            translationY = 6f * keyP.dp.toPx()
                        } else {
                            alpha = keyframe(keyP, 0f to 0f, 0.55f to 1f, 1f to 1f)
                            val s = keyframe(keyP, 0f to 0.5f, 0.55f to 1.08f, 1f to 1f)
                            scaleX = s
                            scaleY = s
                            rotationZ = keyframe(keyP, 0f to -10f, 0.55f to 2f, 1f to 0f)
                        }
                        if (pressed) scaleX *= 0.97f
                    }
                    .clip(RoundedCornerShape(12.dp))
                    .drawBehind {
                        // the block of packed ice
                        drawRect(
                            brush = Brush.linearGradient(
                                listOf(
                                    Color(0x94C6EAFF),
                                    Color(0x705896C6),
                                    Color(0x66285C88)
                                ),
                                start = Offset.Zero,
                                end = Offset(size.width, size.height)
                            )
                        )
                        // the shine lying across the top of the block
                        drawRoundRect(
                            brush = Brush.verticalGradient(
                                listOf(Color(0x6BFFFFFF), Color.Transparent)
                            ),
                            topLeft = Offset(size.width * 0.06f, 1f),
                            size = Size(size.width * 0.88f, size.height * 0.42f),
                            cornerRadius = CornerRadius(size.height, size.height)
                        )
                        // the fractures running through the ice
                        fun hair(angle: Float, spacing: Float, alpha: Float) {
                            val a = angle * Math.PI / 180.0
                            val dx = kotlin.math.cos(a).toFloat()
                            val dy = kotlin.math.sin(a).toFloat()
                            val nx = -dy
                            val ny = dx
                            var o = -size.height * 2f
                            while (o < size.width + size.height * 2f) {
                                val sx = nx * o
                                val sy = ny * o
                                drawLine(
                                    color = Color.White.copy(alpha = alpha),
                                    start = Offset(sx, sy),
                                    end = Offset(sx + dx * size.height * 3f, sy + dy * size.height * 3f),
                                    strokeWidth = 1f
                                )
                                o += spacing * density
                            }
                        }
                        hair(62f, 7f, 0.18f)
                        hair(-46f, 10f, 0.13f)
                        // the light caught along the top edge and down the left
                        drawLine(
                            color = Color(0xE6FFFFFF),
                            start = Offset(size.width * 0.05f, 1f),
                            end = Offset(size.width * 0.95f, 1f),
                            strokeWidth = 1.5f
                        )
                        drawRect(
                            brush = Brush.horizontalGradient(
                                listOf(Color(0x80FFFFFF), Color.Transparent),
                                startX = 0f,
                                endX = size.width * 0.22f
                            ),
                            topLeft = Offset(1f, 1f),
                            size = Size(size.width * 0.22f, size.height - 2f)
                        )
                        drawRect(
                            brush = Brush.verticalGradient(
                                listOf(Color.Transparent, Color(0x73FFFFFF)),
                                startY = size.height * 0.5f,
                                endY = size.height
                            ),
                            topLeft = Offset(1f, size.height * 0.5f),
                            size = Size(size.width - 2f, size.height * 0.5f)
                        )
                    }
                    .border(1.dp, Color(0xB8D6F0FF), RoundedCornerShape(12.dp))
                    .clickable(
                        interactionSource = source,
                        indication = null,
                        enabled = spec.onClick != null
                    ) { spec.onClick?.invoke() }
                    .padding(horizontal = 22.dp, vertical = 13.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    spec.label.uppercase(),
                    color = Color(0xFFF2FAFF),
                    fontSize = (11.5f * scale).sp,
                    fontFamily = ThemeFonts.QuicksandBold,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.4.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = TextStyle(shadow = Shadow(Color(0x9996DCFF), Offset.Zero, 12f))
                )
            }
        }
    }
}
