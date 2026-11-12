package com.openticker.ticker.styles

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Reply
import androidx.compose.material.icons.filled.CallEnd
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Flight
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import kotlin.math.cos
import kotlin.math.sin
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.openticker.R
import com.openticker.data.Prefs
import com.openticker.data.TickerAction
import com.openticker.data.TickerData
import com.openticker.data.TickerInteractions
import com.openticker.data.TickerKind
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/*
 * Aquatic — the open coral reef.
 *
 * Port of Aquatic-concept.html, line by line: a deep-water column with god
 * rays filtering from the surface, a shimmering surface band, caustic light
 * drifting across the reef, seven ambient bubbles rising, a sand floor where
 * kelp sways beside a starfish and a conch shell, two roaming fish and a
 * peeking octopus whose head doubles as the app icon. The notification
 * floats boxless on the open water above a glass bubble reply button and a
 * scallop-shell read button. The card surfaces like a bubble with a squishy
 * overshoot wobble and deflates back into the deep.
 */

private val AqAquaSoft = Color(0xFFCFF6F8)
private val AqTextWater = Color(0xFFEAFBFF)
private val AqInk = Color(0xFF0B4F6C)
/** Fixed reef colours — the aquatic palette is not user-colourable. */
private val AqAqua = Color(0xFF8FE8EE)
private val AqCoral = Color(0xFFFF9A5C)
private val AqKelp = Color(0xFF1E8C74)

private const val AQ_RAY_SLOPE = -0.25f

@OptIn(ExperimentalTextApi::class)
private val AqFredoka = FontFamily(
    Font(
        R.font.fredoka,
        weight = FontWeight.SemiBold,
        variationSettings = FontVariation.Settings(FontVariation.weight(600))
    )
)

@OptIn(ExperimentalTextApi::class)
private val AqNunitoBold = FontFamily(
    Font(
        R.font.nunito,
        weight = FontWeight.Bold,
        variationSettings = FontVariation.Settings(FontVariation.weight(700))
    )
)

@OptIn(ExperimentalTextApi::class)
private val AqNunitoExtraBold = FontFamily(
    Font(
        R.font.nunito,
        weight = FontWeight.ExtraBold,
        variationSettings = FontVariation.Settings(FontVariation.weight(800))
    )
)

/** Piecewise keyframe util — `stops` as fraction-of-animation to value. */
private fun aqKf(value: Float, vararg stops: Pair<Float, Float>): Float {
    if (value <= stops.first().first) return stops.first().second
    var i = 0
    while (i < stops.size - 1) {
        val (t0, v0) = stops[i]
        val (t1, v1) = stops[i + 1]
        if (value <= t1) {
            val f = (value - t0) / (t1 - t0)
            return v0 + (v1 - v0) * f
        }
        i++
    }
    return stops.last().second
}

/** Delay an exit fraction — mimic the CSS animation-delay stagger. */
private fun aqDelayedEx(ex: Float, offset: Float): Float =
    ((ex - offset) / (1f - offset)).coerceIn(0f, 1f)

private data class AqBubbleSpec(
    val leftFrac: Float,
    val sizeDp: Int,
    val period: Int,
    val phase: Float
)

private val aqBubbles = listOf(
    AqBubbleSpec(0.07f, 12, 8000, -1f / 8f),
    AqBubbleSpec(0.17f, 6, 10000, -5f / 10f),
    AqBubbleSpec(0.31f, 9, 9000, -3f / 9f),
    AqBubbleSpec(0.62f, 7, 11000, -7f / 11f),
    AqBubbleSpec(0.78f, 13, 7500, -2f / 7.5f),
    AqBubbleSpec(0.90f, 5, 12000, -9f / 12f),
    AqBubbleSpec(0.50f, 4, 9500, -4f / 9.5f)
)

private val aqKelpSpecs = listOf(
    Triple(0.05f, 64, -1f / 4.5f),
    Triple(0.11f, 44, -2.4f / 4.5f),
    Triple(0.86f, 56, -0.6f / 4.5f),
    Triple(0.93f, 38, -3.1f / 4.5f)
)

@Composable
fun CuratedAquaticTicker(
    data: TickerData,
    interactions: TickerInteractions?,
    animateIn: Boolean,
    exitFraction: () -> Float
) {
    val context = LocalContext.current
    val options = Prefs.aquatic(context)
    val aqua = AqAqua
    val coral = AqCoral
    val kelpCol = AqKelp
    val intensity = (options.intensity / 100f).coerceIn(0f, 1f)
    val effect = options.effect
    val showTime = options.showTime
    val lines = options.lines
    val showLogo = options.showLogo
    // Icon sits on the octopus head (head centre ≈ 43dp, 29.2dp on the 86dp
    // canvas). It fills the head: old defaults (28/42) migrate to a
    // full-head 46dp, and the slider range is coerced to head-filling sizes.
    val iconSize = when (options.avatarSize) {
        28, 42 -> 46
        else -> options.avatarSize
    }.dp.coerceIn(40.dp, 47.dp)
    // Octopus colour is user-settable; the pre-option aqua default maps back
    // to the classic purple.
    val octopusCol = when (options.accentArgb) {
        0, 0xFF8FE8EE.toInt() -> Color(0xFFB57BD9)
        else -> Color(options.accentArgb)
    }

    // ----- "rig.in" entrance choreography ---------------------------------
    val cardEnter = remember { Animatable(if (animateIn) 0f else 1f) }
    val textEnter = remember { Animatable(if (animateIn) 0f else 1f) }
    val octoEnter = remember { Animatable(if (animateIn) 0f else 1f) }
    val btnEnter = remember { List(3) { Animatable(if (animateIn) 0f else 1f) } }

    LaunchedEffect(animateIn) {
        if (!animateIn) {
            cardEnter.snapTo(1f)
            textEnter.snapTo(1f)
            octoEnter.snapTo(1f)
            btnEnter.forEach { it.snapTo(1f) }
            return@LaunchedEffect
        }
        cardEnter.snapTo(0f)
        textEnter.snapTo(0f)
        octoEnter.snapTo(0f)
        btnEnter.forEach { it.snapTo(0f) }
        launch { cardEnter.animateTo(1f, tween(1100, easing = LinearEasing)) }
        launch { textEnter.animateTo(1f, tween(700, delayMillis = 160, easing = LinearEasing)) }
        launch { octoEnter.animateTo(1f, tween(850, delayMillis = 300, easing = LinearEasing)) }
        btnEnter.forEachIndexed { i, anim ->
            launch { anim.animateTo(1f, tween(600, delayMillis = 400 + i * 80, easing = LinearEasing)) }
        }
    }

    // ----- one-shot bubble burst when the card appears --------------------
    // burstUp .95s per bubble, delays .18/.26/.14/.32/.22 of a 1.3s window.
    val burst = remember { Animatable(1f) }
    LaunchedEffect(animateIn) {
        if (!animateIn) {
            burst.snapTo(1f)
            return@LaunchedEffect
        }
        burst.snapTo(0f)
        burst.animateTo(1f, tween(1300, easing = LinearEasing))
    }

    // ----- a second round of bubbles escapes as the card sinks ------------
    val burstOut = remember { Animatable(1f) }
    LaunchedEffect(Unit) {
        snapshotFlow { exitFraction() }
            .first { it > 0f }
        burstOut.snapTo(0f)
        burstOut.animateTo(1f, tween(900, easing = LinearEasing))
    }

    // ----- ambient motion --------------------------------------------------
    val transition = rememberInfiniteTransition(label = "aquatic")
    // rayShift 7s ease-in-out alternate
    val raysState = transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(7000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "rays"
    )
    // shimmer 4s alternate: opacity .55 -> 1
    val shimmerState = transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(4000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "shimmer"
    )
    // causticShift 6s alternate: translateX(-12px) -> (+14px)
    val causticState = transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(6000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "caustics"
    )
    // kelp sway 4.5s alternate with per-stalk phase delays
    val kelpStates = Array(aqKelpSpecs.size) { i ->
        val phase = aqKelpSpecs[i].third
        transition.animateFloat(
            initialValue = phase,
            targetValue = phase + 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(4500, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "kelp$i"
        )
    }
    // octoBob 3.2s alternate: translateY 1.5 -> -2, rotate -1 -> 1.5deg
    val octoBob = transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(3200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "octoBob"
    )
    // tent wiggle 2.3s alternate, staggered delays -0 / -.5 / -1.1 / -1.7
    val tentPhases = listOf(0f, 0.5f / 2.3f, 1.1f / 2.3f, 1.7f / 2.3f)
    val tentStates = Array(4) { i ->
        transition.animateFloat(
            initialValue = tentPhases[i],
            targetValue = tentPhases[i] + 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(2300, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "tent$i"
        )
    }
    // fishBob 1.6s alternate: ±2.5px
    val fishBob = transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "fishBob"
    )
    // satellite bubble floating off the reply button — floatUp 2.8s
    val satellite = transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2800, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "satellite"
    )

    // swimming fish: swim 11s / 15s linear infinite, phase-shifted starts
    val fish1 = remember { Animatable(0f) }
    val fish2 = remember { Animatable(6.5f / 15f) }
    LaunchedEffect(Unit) {
        val cycle1 = 11000
        val cycle2 = 15000
        launch {
            while (true) {
                val from = fish1.value
                val remaining = ((1f - from) * cycle1).toInt().coerceAtLeast(1)
                fish1.animateTo(1f, tween(remaining, easing = LinearEasing))
                fish1.snapTo(0f)
            }
        }
        launch {
            while (true) {
                val from = fish2.value
                val remaining = ((1f - from) * cycle2).toInt().coerceAtLeast(1)
                fish2.animateTo(1f, tween(remaining, easing = LinearEasing))
                fish2.snapTo(0f)
            }
        }
    }
    // ambient bubbles — `rise` per bubble with own period & phase
    val bubbleStates = Array(aqBubbles.size) { i ->
        val s = aqBubbles[i]
        transition.animateFloat(
            initialValue = s.phase,
            targetValue = s.phase + 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(s.period, easing = LinearEasing),
                repeatMode = RepeatMode.Restart
            ),
            label = "bub$i"
        )
    }
    // helper: 0..1 phase of an animatable that runs phase -> phase+1
    fun ph(state: androidx.compose.runtime.State<Float>, phase: Float): Float =
        (state.value - phase).coerceIn(0f, 1f)

    val shape = RoundedCornerShape(26.dp)

    androidx.compose.runtime.CompositionLocalProvider(
        androidx.compose.ui.platform.LocalLayoutDirection provides
            androidx.compose.ui.unit.LayoutDirection.Ltr
    ) {
        BoxWithConstraints(Modifier.fillMaxWidth()) {
            val cardHeight = (maxWidth / 2.067f).coerceIn(196.dp, 224.dp)

            Box(
                Modifier
                    .fillMaxWidth()
                    .height(cardHeight)
                    .graphicsLayer {
                        val ex = exitFraction().coerceIn(0f, 1f)
                        if (ex > 0f) {
                            // bubbleSink: wobble deflate, then sink away
                            alpha = aqKf(ex, 0f to 1f, 1f to 0f).coerceIn(0f, 1f)
                            translationY =
                                aqKf(ex, 0f to 0f, 0.38f to -2f, 1f to 46f).dp.toPx()
                            scaleX = aqKf(ex, 0f to 1f, 0.20f to 1.06f, 0.38f to 0.95f, 1f to 0.8f)
                            scaleY = aqKf(ex, 0f to 1f, 0.20f to 0.94f, 0.38f to 1.06f, 1f to 0.72f)
                        } else {
                            // bubbleRise: surfaces like a bubble, squishy wobble
                            val e = cardEnter.value.coerceIn(0f, 1f)
                            alpha = (e / 0.32f).coerceIn(0f, 1f)
                            translationY = aqKf(
                                e, 0f to 64f, 0.32f to -12f, 0.52f to 3f,
                                0.70f to -4f, 0.86f to 1f, 1f to 0f
                            ).dp.toPx()
                            scaleX = aqKf(
                                e, 0f to 0.78f, 0.32f to 1.09f, 0.52f to 0.93f,
                                0.70f to 1.04f, 0.86f to 0.99f, 1f to 1f
                            )
                            scaleY = aqKf(
                                e, 0f to 0.68f, 0.32f to 0.88f, 0.52f to 1.07f,
                                0.70f to 0.97f, 0.86f to 1.015f, 1f to 1f
                            )
                        }
                        transformOrigin = TransformOrigin(0.5f, 0.5f)
                    }
                    .clip(shape)
                    .background(
                        Brush.verticalGradient(
                            0f to Color(0xFF2A93C0),
                            0.22f to Color(0xFF1D74A0),
                            0.48f to Color(0xFF155C85),
                            0.74f to Color(0xFF10466B),
                            1f to Color(0xFF0B3552)
                        )
                    )
                    .border(2.dp, aqua.copy(alpha = 0.28f), shape)
            ) {
                // ----- water grain, inset highlights, deep vignette -----
                Canvas(Modifier.matchParentSize()) {
                    drawAqWaterLayers(size.width, size.height)
                }

                // ----- god rays filtering from the surface -----
                if (effect) {
                    Canvas(Modifier.matchParentSize()) {
                        drawAqRays(raysState.value, intensity, size.width, size.height)
                    }
                }

                // ----- surface shimmer band at the very top -----
                Canvas(
                    Modifier
                        .matchParentSize()
                        .graphicsLayer {
                            alpha = (0.55f + 0.45f * shimmerState.value) * intensity
                        }
                ) {
                    drawRect(
                        Brush.verticalGradient(
                            listOf(Color(0x59CFF6F8), Color.Transparent),
                            startY = 0f,
                            endY = 14.dp.toPx()
                        ),
                        Offset.Zero,
                        Size(size.width, 14.dp.toPx())
                    )
                }

                // ----- caustic light drifting across the whole reef -----
                if (effect) {
                    Canvas(Modifier.matchParentSize()) {
                        drawAqCaustics(causticState.value, intensity, size.width, size.height)
                    }
                }

                // ----- swaying kelp (drawn first so floor props sit above it) -----
                if (effect) {
                    Canvas(Modifier.matchParentSize()) {
                        drawAqKelp(
                            phases = FloatArray(aqKelpSpecs.size) { i ->
                                ph(kelpStates[i], aqKelpSpecs[i].third)
                            },
                            kelpCol = kelpCol,
                            width = size.width,
                            height = size.height,
                            intensity = intensity
                        )
                    }
                }

                // ----- sand floor, starfish, conch -----
                Canvas(Modifier.matchParentSize()) {
                    drawAqFloor(size.width, size.height, coral, intensity)
                }

                // ----- ambient bubbles -----
                if (effect) {
                    Canvas(Modifier.matchParentSize()) {
                        drawAqAmbient(
                            states = FloatArray(aqBubbles.size) { i ->
                                ph(bubbleStates[i], aqBubbles[i].phase)
                            },
                            width = size.width,
                            height = size.height,
                            intensity = intensity
                        )
                    }
                }

                // ----- fish roaming the whole card -----
                if (effect) {
                    Canvas(Modifier.matchParentSize()) {
                        drawAqFish(
                            progress = fish1.value.coerceIn(0f, 1f),
                            bob = 2.5f * (fishBob.value * 2f - 1f),
                            yFrac = 0.24f,
                            widthDp = 40f,
                            alpha = 0.65f * intensity,
                            big = true,
                            body = coral,
                            tail = lerp(coral, Color(0xFFE05A3E), 0.4f)
                        )
                        drawAqFish(
                            progress = fish2.value.coerceIn(0f, 1f),
                            bob = 2.5f * (fishBob.value * 2f - 1f),
                            yFrac = 0.54f,
                            widthDp = 23f,
                            alpha = 0.5f * intensity,
                            big = false,
                            body = Color(0xFF8FD4EF),
                            tail = Color(0xFF7FC9E8)
                        )
                    }
                }

                // ----- octopus peeking beside the text ---------------------
                Box(
                    Modifier
                        .align(Alignment.TopEnd)
                        .padding(top = 18.dp)
                        .graphicsLayer {
                            val ex = exitFraction().coerceIn(0f, 1f)
                            if (ex > 0f) {
                                // octoDive, staggered run-out
                                val de = aqDelayedEx(ex, 0.23f)
                                alpha = 1f - de
                                translationY =
                                    aqKf(de, 0f to 0f, 0.30f to -4f, 1f to 38f).dp.toPx()
                                scaleX = aqKf(de, 0f to 1f, 0.30f to 1.06f, 1f to 0.7f)
                                scaleY = aqKf(de, 0f to 1f, 0.30f to 0.94f, 1f to 0.8f)
                                rotationZ = 6f * de
                            } else {
                                val e = octoEnter.value.coerceIn(0f, 1f)
                                alpha = (e / 0.55f).coerceIn(0f, 1f)
                                translationY =
                                    aqKf(e, 0f to 26f, 0.55f to -5f, 0.75f to 1f, 1f to 0f).dp.toPx()
                                scaleX = aqKf(e, 0f to 0.55f, 0.55f to 1.12f, 0.75f to 0.94f, 1f to 1f)
                                scaleY = aqKf(e, 0f to 0.5f, 0.55f to 0.9f, 0.75f to 1.06f, 1f to 1f)
                            }
                        }
                ) {
                    Box(
                        Modifier.graphicsLayer {
                            val p = octoBob.value
                            translationY = aqKf(p, 0f to 1.5f, 1f to -2f).dp.toPx()
                            rotationZ = aqKf(p, 0f to -1f, 1f to 1.5f)
                            transformOrigin = TransformOrigin(0.5f, 0.5f)
                        }
                    ) {
                        Canvas(
                            Modifier.size(width = 86.dp, height = 79.dp)
                        ) {
                            drawAqOctopus(
                                phases = FloatArray(4) { i -> ph(tentStates[i], tentPhases[i]) },
                                purple = octopusCol,
                                width = size.width,
                                height = size.height
                            )
                        }
                        // the app icon fills the octopus head
                        if (showLogo) {
                            Box(
                                Modifier
                                    .offset(
                                        x = 43.dp - iconSize / 2,
                                        y = 29.2f.dp - iconSize / 2
                                    )
                                    .size(iconSize)
                                    .clip(CircleShape)
                                    .background(Color(0xFF9AD4DE)),
                                contentAlignment = Alignment.Center
                            ) {
                                val icon = data.icon
                                if (icon != null) {
                                    Image(
                                        bitmap = icon.asImageBitmap(),
                                        contentDescription = null,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .clip(CircleShape)
                                    )
                                } else {
                                    Icon(
                                        aqKindIcon(data.kind),
                                        contentDescription = null,
                                        tint = AqInk,
                                        modifier = Modifier.size(iconSize * 0.55f)
                                    )
                                }
                            }
                        }
                    }
                }

                // ----- notification text — floats on the open water --------
                Column(
                    Modifier
                        .align(Alignment.TopStart)
                        .padding(start = 20.dp, end = 92.dp, top = 34.dp)
                        .fillMaxWidth()
                        .graphicsLayer {
                            val ex = exitFraction().coerceIn(0f, 1f)
                            if (ex > 0f) {
                                // textOut, staggered .05s of .65s ≈ .08
                                val de = aqDelayedEx(ex, 0.08f)
                                alpha = 1f - de
                                translationY = -(16f * de).dp.toPx()
                                scaleX = 1f - 0.04f * de
                                scaleY = 1f + 0.03f * de
                            } else {
                                // textBubbleIn: drifts up like a bubble of air
                                val e = textEnter.value.coerceIn(0f, 1f)
                                alpha = (e / 0.55f).coerceIn(0f, 1f)
                                translationY =
                                    aqKf(e, 0f to 18f, 0.55f to -4f, 0.75f to 1f, 1f to 0f).dp.toPx()
                                scaleX = aqKf(e, 0f to 0.94f, 0.55f to 1.03f, 0.75f to 0.99f, 1f to 1f)
                                scaleY = aqKf(e, 0f to 0.9f, 0.55f to 0.98f, 0.75f to 1.01f, 1f to 1f)
                            }
                        }
                ) {
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(
                            data.title,
                            color = AqTextWater,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.SemiBold,
                            fontFamily = AqFredoka,
                            letterSpacing = 0.3.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f),
                            style = TextStyle(
                                shadow = Shadow(
                                    color = Color(0x99021617),
                                    offset = Offset(0f, 1f),
                                    blurRadius = 4f
                                )
                            )
                        )
                        if (showTime) {
                            Spacer(Modifier.width(8.dp))
                            Text(
                                data.time.uppercase(),
                                color = aqua,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.ExtraBold,
                                fontFamily = AqNunitoExtraBold,
                                letterSpacing = 0.5.sp,
                                maxLines = 1
                            )
                        }
                    }
                    Spacer(Modifier.height(5.dp))
                    AquaticBody(data.body, lines)
                }

                // ----- dock — bubble & shell buttons -----------------------
                AquaticDock(
                    data = data,
                    interactions = interactions,
                    aquaSoft = AqAquaSoft,
                    btnEnter = { i -> btnEnter[i.coerceIn(0, 2)].value },
                    exitFraction = exitFraction,
                    satellite = { satellite.value },
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 18.dp)
                )

                // ----- burst bubbles (appear + escape round) ---------------
                if (effect) {
                    Canvas(Modifier.matchParentSize()) {
                        drawAqBurst(
                            inProgress = burst.value,
                            outProgress = burstOut.value,
                            intensity = intensity,
                            width = size.width,
                            height = size.height
                        )
                    }
                }
            }
        }
    }
}

/* ------------------------------------------------------------------ */
/* Water layers: grain, inset highlights, deep vignette                */
/* ------------------------------------------------------------------ */

private fun DrawScope.drawAqWaterLayers(width: Float, height: Float) {
    // soft vertical water grain — repeating 2px on / 5px off columns
    var x = 0f
    while (x < width) {
        drawRect(Color(0x04FFFFFF), Offset(x, 0f), Size(2.dp.toPx(), height))
        x += 7.dp.toPx()
    }
    // inset 0 2px 0 rgba(255,255,255,.1)
    drawRect(Color(0x1AFFFFFF), Offset(0f, 0f), Size(width, 2.dp.toPx()))
    // inset 0 -18px 40px rgba(2,12,22,.3) — deep bottom shadow
    drawRect(
        Brush.verticalGradient(
            colors = listOf(Color.Transparent, Color(0x4D020C16)),
            startY = height - 46.dp.toPx(),
            endY = height
        ),
        Offset(0f, height - 46.dp.toPx()),
        Size(width, 46.dp.toPx())
    )
    // inset 0 0 60px rgba(2,16,28,.16) — inner vignette
    drawRect(
        Brush.radialGradient(
            colors = listOf(Color.Transparent, Color(0x2902101C)),
            center = Offset(width / 2f, height * 0.42f),
            radius = maxOf(width, height) * 0.85f
        )
    )
}

/* ------------------------------------------------------------------ */
/* God rays                                                            */
/* ------------------------------------------------------------------ */

private fun DrawScope.drawAqRays(phase: Float, intensity: Float, width: Float, height: Float) {
    val shift = (-14.dp + 32.dp * phase).toPx()
    val alphaBoost = 0.8f + 0.2f * phase
    val fadeEnd = height * 0.72f
    val slope = AQ_RAY_SLOPE
    val cyclePx = 150.dp.toPx()
    val unit = cyclePx / 150f
    var cycleX = shift % cyclePx - cyclePx
    while (cycleX < width + slope * fadeEnd + cyclePx) {
        // band 1: 34..46 — rgba(190,240,246,.055)
        drawAqRayBand(
            cycleX + 34f * unit, 12f * unit,
            Color(0xFFBEF0F6), 0.055f * alphaBoost * intensity,
            slope, height, fadeEnd
        )
        // band 2: 96..104 — rgba(255,255,255,.04)
        drawAqRayBand(
            cycleX + 96f * unit, 8f * unit,
            Color(0xFFFFFFFF), 0.04f * alphaBoost * intensity,
            slope, height, fadeEnd
        )
        cycleX += cyclePx
    }
}

private fun DrawScope.drawAqRayBand(
    xTop: Float,
    bandWidth: Float,
    color: Color,
    alpha: Float,
    slope: Float,
    height: Float,
    fadeEnd: Float
) {
    if (alpha <= 0.001f) return
    val path = Path().apply {
        moveTo(xTop, 0f)
        lineTo(xTop + bandWidth, 0f)
        lineTo(xTop + bandWidth + slope * fadeEnd, fadeEnd)
        lineTo(xTop + slope * fadeEnd, fadeEnd)
        close()
    }
    drawPath(
        path,
        Brush.verticalGradient(
            colors = listOf(color.copy(alpha = alpha), Color.Transparent),
            startY = 0f,
            endY = fadeEnd
        )
    )
}

/* ------------------------------------------------------------------ */
/* Caustics                                                            */
/* ------------------------------------------------------------------ */

private fun DrawScope.drawAqCaustics(
    phase: Float,
    intensity: Float,
    width: Float,
    height: Float
) {
    val shift = (-12.dp + 26.dp * phase).toPx()
    drawAqBlob(width * 0.22f + shift, 0f, width * 0.60f, height * 0.42f, Color(0x17FFFFFF))
    drawAqBlob(width * 0.74f + shift, 0f, width * 0.55f, height * 0.40f, Color(0x1A8FE8EE))
    drawAqBlob(width * 0.45f + shift, height * 0.34f, width * 0.50f, height * 0.36f, Color(0x0DFFFFFF))
}

private fun DrawScope.drawAqBlob(cx: Float, cy: Float, rx: Float, ry: Float, color: Color) {
    drawOval(
        brush = Brush.radialGradient(
            colors = listOf(color, Color.Transparent),
            center = Offset(cx, cy + ry / 2f),
            radius = maxOf(rx, ry) * 0.95f
        ),
        topLeft = Offset(cx - rx, cy - ry),
        size = Size(rx * 2f, ry * 2f)
    )
}

/* ------------------------------------------------------------------ */
/* Sand floor, starfish, conch                                         */
/* ------------------------------------------------------------------ */

private fun DrawScope.drawAqFloor(
    width: Float,
    height: Float,
    coral: Color,
    intensity: Float
) {
    val sandTop = height - 19.dp.toPx()
    // upward cast shadow — 0 -5px 14px rgba(1,10,18,.35)
    drawRect(
        Brush.verticalGradient(
            colors = listOf(Color.Transparent, Color(0x59010A12)),
            startY = sandTop - 14.dp.toPx(),
            endY = sandTop
        ),
        Offset(0f, sandTop - 14.dp.toPx()),
        Size(width, 14.dp.toPx())
    )
    // sand gradient E8CE9C -> D9B87E
    drawRect(
        Brush.verticalGradient(listOf(Color(0xFFE8CE9C), Color(0xFFD9B87E))),
        Offset(0f, sandTop),
        Size(width, 19.dp.toPx())
    )
    // sand highlight bumps
    val bumps = listOf(
        0.15f to 22.dp, 0.55f to 30.dp, 0.88f to 26.dp
    )
    bumps.forEach { (fx, r) ->
        drawOval(
            brush = Brush.radialGradient(
                colors = listOf(Color(0x2EFFFFFF), Color.Transparent),
                center = Offset(width * fx, sandTop),
                radius = r.toPx()
            ),
            topLeft = Offset(width * fx - r.toPx(), sandTop - r.toPx() * 0.4f),
            size = Size(r.toPx() * 2f, r.toPx() * 0.8f)
        )
    }
    // starfish — left 30px, bottom 7px, 22px
    drawAqStarfish(30.dp.toPx(), height - 7.dp.toPx(), 22.dp.toPx(), coral)
    // shell cone — right 34px, bottom 6px, 18x13
    drawAqShellCone(width - 34.dp.toPx(), height - 6.dp.toPx())
}

private fun DrawScope.drawAqStarfish(cx: Float, bottomY: Float, sizePx: Float, coral: Color) {
    val left = cx - sizePx / 2f
    val top = bottomY - sizePx
    // drop shadow
    drawOval(
        color = Color(0x40000000),
        topLeft = Offset(left + 1f, bottomY - 2.dp.toPx()),
        size = Size(sizePx, 3.dp.toPx())
    )
    val pts = listOf(
        0.5f to 0f, 0.63f to 0.34f, 0.98f to 0.36f, 0.71f to 0.59f,
        0.81f to 0.95f, 0.5f to 0.74f, 0.19f to 0.95f, 0.29f to 0.59f,
        0.02f to 0.36f, 0.37f to 0.34f
    )
    // Lying flat on the sand rather than standing: rotate ~55° around the
    // centre, then settle the lowest point back onto the sand line.
    val angle = Math.toRadians(55.0)
    val cos = cos(angle).toFloat()
    val sin = sin(angle).toFloat()
    val cxp = left + sizePx / 2f
    val cyp = top + sizePx / 2f
    val rotated = pts.map { (px, py) ->
        val x = left + px * sizePx
        val y = top + py * sizePx
        val rx = cxp + (x - cxp) * cos - (y - cyp) * sin
        val ry = cyp + (x - cxp) * sin + (y - cyp) * cos
        rx to ry
    }
    val lowest = rotated.maxOf { it.second }
    val settle = bottomY - lowest
    val path = Path().apply {
        rotated.forEachIndexed { i, (rx, ry) ->
            // settle up so the flattened star rests on the sand
            if (i == 0) moveTo(rx, ry + settle) else lineTo(rx, ry + settle)
        }
        close()
    }
    drawPath(
        path,
        Brush.horizontalGradient(
            listOf(lerp(coral, Color(0xFFFFFFFF), 0.35f), lerp(coral, Color(0xFF5A1508), 0.35f)),
            startX = left - sizePx * 0.2f,
            endX = left + sizePx * 1.2f
        )
    )
}

private fun DrawScope.drawAqShellCone(rightX: Float, bottomY: Float) {
    val widthPx = 18.dp.toPx()
    val heightPx = 13.dp.toPx()
    val left = rightX - widthPx
    val top = bottomY - heightPx
    // shadow
    drawOval(
        color = Color(0x40000000),
        topLeft = Offset(left, bottomY - 2.5f),
        size = Size(widthPx, 3.dp.toPx())
    )
    val path = Path().apply {
        addRoundRect(
            RoundRect(
                left, top, rightX, bottomY,
                topLeftCornerRadius = CornerRadius(widthPx / 2f, widthPx / 2f),
                topRightCornerRadius = CornerRadius(widthPx / 2f, widthPx / 2f),
                bottomLeftCornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx()),
                bottomRightCornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx())
            )
        )
    }
    drawPath(path, Brush.verticalGradient(listOf(Color(0xFFFBEED2), Color(0xFFE8C894))))
    // repeating diagonal stripes rgba(160,110,60,.35)
    clipPath(path) {
        var x = left - heightPx
        while (x < rightX + heightPx) {
            drawLine(
                Color(0x59A06E3C),
                Offset(x, bottomY),
                Offset(x + heightPx, top),
                1.5.dp.toPx()
            )
            x += 4.5.dp.toPx()
        }
    }
}

private fun DrawScope.drawAqKelp(
    phases: FloatArray,
    kelpCol: Color,
    width: Float,
    height: Float,
    intensity: Float
) {
    val sandTop = height - 19.dp.toPx()
    aqKelpSpecs.forEachIndexed { i, (leftFrac, heightDp, phase) ->
        val p = phases[i]
        val deg = aqKf(p, 0f to -5f, 1f to 6f)
        val h = heightDp.dp.toPx()
        val w = 9.dp.toPx()
        val bottom = sandTop + 4.dp.toPx() // bottom 17px → rooted at the sand
        val x = width * leftFrac + w / 2f
        rotate(deg, pivot = Offset(x, bottom)) {
            drawRoundRect(
                brush = Brush.verticalGradient(
                    0f to Color(0xFF2FB08E),
                    0.45f to kelpCol,
                    1f to Color(0xFF0E5E4E)
                ),
                topLeft = Offset(x - w / 2f, bottom - h),
                size = Size(w, h),
                cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx()),
                alpha = 0.9f * intensity.coerceAtLeast(0.2f)
            )
        }
    }
}

/* ------------------------------------------------------------------ */
/* Bubbles                                                             */
/* ------------------------------------------------------------------ */

private fun DrawScope.drawAqBubble(cx: Float, cy: Float, r: Float, scale: Float, alpha: Float) {
    if (alpha <= 0.001f) return
    val rr = r * scale
    drawCircle(
        brush = Brush.radialGradient(
            0f to Color(0xD9FFFFFF),
            0.55f to Color(0x40BEEBF5),
            1f to Color(0x10FFFFFF),
            center = Offset(cx - rr * 0.4f, cy - rr * 0.4f),
            radius = rr * 1.8f
        ),
        radius = rr,
        center = Offset(cx, cy),
        alpha = alpha
    )
    drawCircle(
        Color(0x59FFFFFF),
        radius = rr,
        center = Offset(cx, cy),
        alpha = alpha,
        style = Stroke(1.dp.toPx())
    )
}

private fun DrawScope.drawAqAmbient(
    states: FloatArray,
    width: Float,
    height: Float,
    intensity: Float
) {
    aqBubbles.forEachIndexed { i, s ->
        // `rise` keyframes — sway / travel / scale / opacity over one cycle
        val p = states[i]
        val travel = height * 1.15f + 40.dp.toPx()
        val tx = aqKf(p, 0f to 0f, 0.5f to 7f, 1f to -5f).dp.toPx()
        val sc = aqKf(p, 0f to 1f, 0.5f to 1.05f, 1f to 0.9f)
        val alpha = aqKf(p, 0f to 0f, 0.08f to 0.75f, 0.9f to 0.4f, 1f to 0f)
        if (alpha <= 0.001f) return@forEachIndexed
        val r = s.sizeDp.dp.toPx() / 2f
        val cx = width * s.leftFrac + tx
        val cy = height + 16.dp.toPx() - travel * p - r
        drawAqBubble(cx, cy, r, sc, alpha * intensity)
    }
}

private fun DrawScope.drawAqBurst(
    inProgress: Float,
    outProgress: Float,
    intensity: Float,
    width: Float,
    height: Float
) {
    // On appear: one 0.95s burstUp per bubble, delays .14/.18/.22/.26/.32 of a 1.3s window.
    // On sink: a 0.7s escape round, delays .04/.08/.12/.16/.20 of a 0.9s window.
    val enterStart = floatArrayOf(0.18f / 1.3f, 0.26f / 1.3f, 0.14f / 1.3f, 0.32f / 1.3f, 0.22f / 1.3f)
    val enterSpan = 0.95f / 1.3f
    val exitStart = floatArrayOf(0.08f / 0.9f, 0.16f / 0.9f, 0.04f / 0.9f, 0.20f / 0.9f, 0.12f / 0.9f)
    val exitSpan = 0.7f / 0.9f
    val lefts = floatArrayOf(0.12f, 0.34f, 0.55f, 0.74f, 0.88f)
    val bottoms = floatArrayOf(0.26f, 0.18f, 0.24f, 0.16f, 0.28f)
    val sizes = floatArrayOf(12f, 8f, 14f, 9f, 7f)
    for (i in 0 until 5) {
        val baseX = width * lefts[i]
        val baseY = height * (1f - bottoms[i])
        val r = sizes[i].dp.toPx() / 2f
        if (inProgress > 0.001f && inProgress < 0.998f && outProgress > 0.99f) {
            val p = ((inProgress - enterStart[i]) / enterSpan).coerceIn(0f, 1f)
            if (p > 0.001f && p < 0.999f) {
                drawAqBurstBubble(
                    baseX, baseY - (10f + 82f * p).dp.toPx(), r,
                    0.4f + 0.75f * p,
                    aqKf(p, 0f to 0f, 0.25f to 0.9f, 1f to 0f) * intensity
                )
            }
        }
        if (outProgress > 0.001f && outProgress < 0.998f) {
            val p = ((outProgress - exitStart[i]) / exitSpan).coerceIn(0f, 1f)
            if (p > 0.001f && p < 0.999f) {
                drawAqBurstBubble(
                    baseX, baseY - (10f + 82f * p).dp.toPx(), r,
                    0.4f + 0.75f * p,
                    aqKf(p, 0f to 0f, 0.25f to 0.9f, 1f to 0f) * intensity
                )
            }
        }
    }
}

private fun DrawScope.drawAqBurstBubble(cx: Float, cy: Float, r: Float, scale: Float, alpha: Float) {
    if (alpha <= 0.001f) return
    val rr = r * scale
    drawCircle(
        brush = Brush.radialGradient(
            0f to Color(0xFFFFFFFF),
            0.60f to Color(0x66BEF0F6),
            1f to Color(0x1AFFFFFF),
            center = Offset(cx - rr * 0.35f, cy - rr * 0.3f),
            radius = rr * 1.7f
        ),
        radius = rr,
        center = Offset(cx, cy),
        alpha = alpha
    )
    drawCircle(
        Color(0x80FFFFFF),
        radius = rr,
        center = Offset(cx, cy),
        alpha = alpha,
        style = Stroke(1.dp.toPx())
    )
}

/* ------------------------------------------------------------------ */
/* Fish — drawFish4f uses the 64x36 viewBox from the concept           */
/* ------------------------------------------------------------------ */

private fun DrawScope.drawAqFish(
    progress: Float,
    bob: Float,
    yFrac: Float,
    widthDp: Float,
    alpha: Float,
    big: Boolean,
    body: Color,
    tail: Color
) {
    if (alpha <= 0.002f) return
    // `swim` keyframes: left -22% -> 104% (0..49%), back (50..99%), flip at the turn
    val leftFrac = aqKf(
        progress,
        0f to -0.22f, 0.49f to 1.04f, 0.50f to 1.04f, 0.99f to -0.22f, 1f to -0.22f
    )
    val faceRight = progress < 0.493f
    val u = widthDp.dp.toPx() / 64f
    val x = leftFrac * size.width
    val y = size.height * yFrac + bob.dp.toPx()
    val stripe = if (big) Color(0xFFFFF6E8) else Color(0xFFEAF8FF)
    translate(x, y) {
        scale(u * (if (faceRight) -1f else 1f), u, pivot = Offset(32f, 18f)) {            // body
            drawOval(body, Offset(6f, 5f), Size(48f, 26f))
            // tail
            val tailPath = Path().apply {
                moveTo(50f, 18f)
                lineTo(63f, 8f)
                lineTo(59f, 18f)
                lineTo(63f, 28f)
                close()
            }
            drawPath(tailPath, tail)
            if (big) {
                drawPath(
                    Path().apply {
                        moveTo(22f, 6.5f)
                        quadraticBezierTo(26f, 18f, 22f, 29.5f)
                    },
                    stripe, style = Stroke(5f, cap = StrokeCap.Round)
                )
                drawPath(
                    Path().apply {
                        moveTo(38f, 7.5f)
                        quadraticBezierTo(42f, 18f, 38f, 28.5f)
                    },
                    stripe, style = Stroke(4f, cap = StrokeCap.Round)
                )
                drawPath(
                    Path().apply {
                        moveTo(28f, 8f)
                        quadraticBezierTo(31f, 13f, 28f, 17f)
                    },
                    Color(0xFFE0703A), style = Stroke(3f, cap = StrokeCap.Round)
                )
            } else {
                drawPath(
                    Path().apply {
                        moveTo(22f, 6.5f)
                        quadraticBezierTo(26f, 18f, 22f, 29.5f)
                    },
                    stripe, style = Stroke(5f, cap = StrokeCap.Round)
                )
            }
            drawCircle(Color(0xFF1B2B36), 2.7f, Offset(14f, 15f))
            drawCircle(Color(0xFFFFFFFF), 0.9f, Offset(13f, 14f))
        }
    }
}

/* ------------------------------------------------------------------ */
/* Octopus                                                             */
/* ------------------------------------------------------------------ */

private fun DrawScope.drawAqOctopus(
    phases: FloatArray,
    purple: Color,
    width: Float,
    height: Float
) {
    val u = width / 100f // unit scale — the concept octopus box is 100x92
    val purpleDark = lerp(purple, Color(0xFF150A20), 0.35f)

    // tentacles — 9-unit strokes, round caps, per-tentacle wiggle
    fun quad(
        x1: Float, y1: Float,
        cx1: Float, cy1: Float, x2: Float, y2: Float,
        cx2: Float, cy2: Float, x3: Float, y3: Float
    ): Path = Path().apply {
        moveTo(x1 * u, y1 * u)
        quadraticBezierTo(cx1 * u, cy1 * u, x2 * u, y2 * u)
        quadraticBezierTo(cx2 * u, cy2 * u, x3 * u, y3 * u)
    }

    val tents = listOf(
        Triple(quad(32f, 61f, 22f, 70f, 26f, 80f, 29f, 86f, 36f, 83f), Offset(32f, 61f), purple),
        Triple(quad(44f, 65f, 42f, 76f, 48f, 82f, 52f, 87f, 57f, 82f), Offset(44f, 65f), purpleDark),
        Triple(quad(58f, 65f, 62f, 76f, 70f, 80f, 75f, 83f, 77f, 77f), Offset(58f, 65f), purple),
        Triple(quad(70f, 59f, 84f, 66f, 88f, 76f, 90f, 82f, 84f, 83f), Offset(70f, 59f), purpleDark)
    )
    tents.forEachIndexed { i, (path, anchor, col) ->
        val deg = aqKf(phases[i], 0f to -7f, 1f to 8f)
        rotate(deg, pivot = Offset(anchor.x * u, anchor.y * u)) {
            drawPath(path, col, style = Stroke(9f * u, cap = StrokeCap.Round))
        }
    }

    // head — radial white -> D8F2F4 (45%) -> 9AD4DE with a purple stroke
    val headR = 28f * u
    drawCircle(
        brush = Brush.radialGradient(
            0f to Color(0xFFFFFFFF),
            0.45f to Color(0xFFD8F2F4),
            1f to Color(0xFF9AD4DE),
            center = Offset((50f - 18f) * u, (34f - 14f) * u),
            radius = headR * 1.6f
        ),
        radius = headR,
        center = Offset(50f * u, 34f * u)
    )
    drawCircle(
        purple,
        headR,
        Offset(50f * u, 34f * u),
        style = Stroke(3f * u)
    )
    // glossy highlight — ellipse (38,18) rx9 ry5.5 white .85 rotate -24
    rotate(-24f, pivot = Offset(38f * u, 18f * u)) {
        drawOval(
            Color(0xD9FFFFFF),
            topLeft = Offset((38f - 9f) * u, (18f - 5.5f) * u),
            size = Size(18f * u, 11f * u)
        )
    }
}

/* ------------------------------------------------------------------ */
/* Body text                                                           */
/* ------------------------------------------------------------------ */

@Composable
private fun AquaticBody(body: String, lines: Int) {
    val bodySp = Prefs.fontSize(LocalContext.current)
    val scrollSpeed = Prefs.speed(LocalContext.current)
    val shadow = TextStyle(
        shadow = Shadow(color = Color(0xCC021019), offset = Offset(0f, 2f), blurRadius = 3f)
    )
    if (lines <= 0) {
        Text(
            body,
            color = AqAquaSoft,
            fontSize = bodySp.sp,
            lineHeight = (bodySp * 1.3f).sp,
            fontWeight = FontWeight.Bold,
            fontFamily = AqNunitoBold,
            maxLines = 1,
            softWrap = false,
            overflow = TextOverflow.Clip,
            modifier = Modifier
                .fillMaxWidth()
                .basicMarquee(iterations = Int.MAX_VALUE, velocity = (30f * scrollSpeed).dp),
            style = shadow
        )
    } else {
        Text(
            body,
            color = AqAquaSoft,
            fontSize = bodySp.sp,
            lineHeight = (bodySp * 1.3f).sp,
            fontWeight = FontWeight.Bold,
            fontFamily = AqNunitoBold,
            minLines = lines,
            maxLines = lines,
            overflow = TextOverflow.Ellipsis,
            style = shadow
        )
    }
}

/* ------------------------------------------------------------------ */
/* Dock — glass bubble & scallop shell buttons                         */
/* ------------------------------------------------------------------ */

private data class AqDockButton(
    val bubble: Boolean,
    val icon: ImageVector,
    val label: String,
    val onClick: (() -> Unit)?
)

@Composable
private fun AquaticDock(
    data: TickerData,
    interactions: TickerInteractions?,
    aquaSoft: Color,
    btnEnter: (Int) -> Float,
    exitFraction: () -> Float,
    satellite: () -> Float,
    modifier: Modifier = Modifier
) {
    val music = if (data.kind == TickerKind.Music) aqMusicControls(data) else null
    val call = if (data.kind == TickerKind.Call) aqCallActions(data, interactions) else null

    val buttons: List<AqDockButton> = when {
        music != null -> music.map { control ->
            AqDockButton(
                bubble = true,
                icon = control.icon,
                label = control.label,
                onClick = if (interactions != null && control.action?.pendingIntent != null) {
                    { control.action?.let { interactions.onAction(it) } }
                } else null
            )
        }
        call != null -> {
            val (answer, reject) = call
            listOf(
                // Reject on the left, Answer on the right — all themes
                AqDockButton(
                    bubble = true,
                    icon = Icons.Filled.CallEnd,
                    label = "Reject",
                    onClick = if (interactions != null && reject?.pendingIntent != null) {
                        { reject?.let { interactions.onAction(it) }; interactions.onDismiss() }
                    } else null
                ),
                AqDockButton(
                    bubble = false,
                    icon = Icons.Filled.Phone,
                    label = "Answer",
                    onClick = if (interactions != null && answer?.pendingIntent != null) {
                        { answer?.let { interactions.onAction(it) }; interactions.onDismiss() }
                    } else null
                )
            )
        }
        else -> notificationActionSpecs(data, interactions).map { spec ->
            // Buttons follow the source notification's own actions; a
            // notification with no actions gets a blank dock.
            AqDockButton(
                bubble = !spec.shell,
                icon = spec.icon,
                label = spec.label,
                onClick = spec.onClick
            )
        }
    }
    val gap = if (buttons.size > 2) 12.dp else 20.dp
    val colWidth = if (buttons.size > 2) 74.dp else 84.dp

    Row(modifier, horizontalArrangement = Arrangement.Center) {
        buttons.forEachIndexed { i, btn ->
            if (i > 0) Spacer(Modifier.width(gap))
            AqDockButtonView(
                btn = btn,
                aquaSoft = aquaSoft,
                enter = btnEnter(i),
                exitFraction = exitFraction,
                exitDelay = i / 10f,
                satellite = satellite,
                colWidth = colWidth
            )
        }
    }
}

@Composable
private fun AqDockButtonView(
    btn: AqDockButton,
    aquaSoft: Color,
    enter: Float,
    exitFraction: () -> Float,
    exitDelay: Float,
    satellite: () -> Float,
    colWidth: Dp
) {
    Column(
        Modifier.width(colWidth),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Box(
            Modifier
                .size(48.dp)
                .graphicsLayer {
                    val ex = exitFraction().coerceIn(0f, 1f)
                    if (ex > 0f) {
                        // bubbleBurst: inflate, wobble, pop to nothing
                        val de = aqDelayedEx(ex, exitDelay)
                        alpha = aqKf(de, 0f to 1f, 0.70f to 1f, 1f to 0f)
                        scaleX = aqKf(de, 0f to 1f, 0.45f to 1.22f, 0.70f to 0.9f, 1f to 0.08f)
                        scaleY = aqKf(de, 0f to 1f, 0.45f to 0.92f, 0.70f to 1.15f, 1f to 0.14f)
                    } else {
                        // bubblePop: inflates like a bubble, wobbling as it settles
                        val e = enter.coerceIn(0f, 1f)
                        alpha = (e / 0.5f).coerceIn(0f, 1f)
                        translationY = aqKf(e, 0f to 10f, 0.5f to -3f, 0.7f to 1f, 1f to 0f).dp.toPx()
                        scaleX = aqKf(e, 0f to 0.18f, 0.5f to 1.16f, 0.7f to 0.9f, 0.88f to 1.05f, 1f to 1f)
                        scaleY = aqKf(e, 0f to 0.14f, 0.5f to 0.88f, 0.7f to 1.1f, 0.88f to 0.97f, 1f to 1f)
                    }
                    transformOrigin = TransformOrigin(0.5f, 0.5f)
                }
                .then(
                    if (btn.onClick != null) Modifier.clickable { btn.onClick() } else Modifier
                )
        ) {
            if (btn.bubble) {
                // solid bottom edge — 0 4px 0 rgba(2,16,25,.55)
                Canvas(
                    Modifier
                        .fillMaxSize()
                        .offset(y = 3.dp)
                ) {
                    drawCircle(Color(0x8C021019))
                }
                Canvas(Modifier.fillMaxSize()) {
                    val r = size.minDimension / 2f
                    // face — radial-gradient(circle at 32% 28%, white .95, deep .6)
                    drawCircle(
                        brush = Brush.radialGradient(
                            0f to Color(0xF2FFFFFF),
                            0.38f to Color(0x8CBEF0F5),
                            1f to Color(0x99145A78),
                            center = Offset(size.width * 0.32f, size.height * 0.28f),
                            radius = r * 1.15f
                        ),
                        radius = r,
                        center = center
                    )
                    // glossy highlight blob — top 7 left 9, 13x8, rotate -24
                    rotate(-24f, pivot = Offset(15.dp.toPx(), 11.dp.toPx())) {
                        drawOval(
                            Color(0xE6FFFFFF),
                            topLeft = Offset(9.dp.toPx(), 7.dp.toPx()),
                            size = Size(13.dp.toPx(), 8.dp.toPx())
                        )
                    }
                    // border — 2px rgba(207,246,248,.75)
                    drawCircle(
                        Color(0xBFCFF6F8),
                        radius = r - 1.dp.toPx(),
                        style = Stroke(2.dp.toPx())
                    )
                    // tiny satellite bubble floating off the button
                    val p = satellite().coerceIn(0f, 1f)
                    if (p > 0.001f && p < 0.999f) {
                        val ty = 3f - 27f * p // translateY(3px) -> (-24px)
                        val sc = 0.6f + 0.4f * p
                        val alpha = aqKf(p, 0f to 0f, 0.25f to 0.85f, 1f to 0f)
                        drawCircle(
                            Color(0xA1FFFFFF),
                            radius = 3.5.dp.toPx() * sc,
                            center = Offset(size.width - 5.dp.toPx(), 4.dp.toPx() + ty.dp.toPx()),
                            alpha = alpha,
                            style = Stroke(1.dp.toPx())
                        )
                    }
                }
                Icon(
                    btn.icon,
                    contentDescription = btn.label,
                    tint = AqInk,
                    modifier = Modifier
                        .size(18.dp)
                        .align(Alignment.Center)
                )
            } else {
                // scallop shell button
                Canvas(Modifier.size(46.dp)) {
                    drawAqShell46()
                }
                Icon(
                    btn.icon,
                    contentDescription = btn.label,
                    tint = Color(0xFF8A5426),
                    modifier = Modifier
                        .size(17.dp)
                        .align(Alignment.Center)
                )
            }
        }
        Text(
            btn.label.uppercase(),
            color = aquaSoft,
            fontSize = 8.sp,
            fontWeight = FontWeight.ExtraBold,
            fontFamily = AqNunitoExtraBold,
            letterSpacing = 0.8.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
            style = TextStyle(
                shadow = Shadow(
                    color = Color(0xB3021019),
                    offset = Offset(0f, 1f),
                    blurRadius = 6f
                )
            )
        )
    }
}

/* Scallop shell painted at 46dp from the concept's 48px viewBox. */
private fun DrawScope.drawAqShell46() {
    val s = size.width / 48f
    val shellPath = Path().apply {
        moveTo(24f * s, 45f * s)
        cubicTo(9f * s, 38f * s, 3.5f * s, 27f * s, 6.5f * s, 16.5f * s)
        cubicTo(10f * s, 8f * s, 16f * s, 3.5f * s, 24f * s, 3.5f * s)
        cubicTo(32f * s, 3.5f * s, 38f * s, 8f * s, 41.5f * s, 16.5f * s)
        cubicTo(44.5f * s, 27f * s, 39f * s, 38f * s, 24f * s, 45f * s)
        close()
    }
    // drop shadow — 0 4px 0 rgba(2,16,25,.45)
    translate(0f, 4f * s) {
        drawPath(shellPath, Color(0x73021019))
    }
    // sand gradient
    drawPath(
        shellPath,
        Brush.verticalGradient(
            0f to Color(0xFFFFE6BC),
            0.55f to Color(0xFFF5C58F),
            1f to Color(0xFFE0A068),
            startY = 3.5f * s,
            endY = 45f * s
        )
    )
    drawPath(
        shellPath,
        Color(0xFFB57E44),
        style = Stroke(1.6f * s)
    )
    // interior rays
    val rays = listOf(24f to 5f, 13.5f to 8f, 34.5f to 8f, 8f to 17f, 40f to 17f)
    rays.forEach { (rx, ry) ->
        drawLine(
            Color(0xBFC98B52),
            Offset(24f * s, 44f * s),
            Offset(rx * s, ry * s),
            1.4f * s,
            cap = StrokeCap.Round
        )
    }
}

/* ------------------------------------------------------------------ */
/* Kind-aware actions                                                  */
/* ------------------------------------------------------------------ */

private fun aqLabelHas(label: String, vararg keys: String): Boolean =
    keys.any { label.contains(it, ignoreCase = true) }

private fun aqFindAction(data: TickerData, vararg keys: String): TickerAction? =
    data.actions.firstOrNull { aqLabelHas(it.label, *keys) }

private fun aqIsPauseLabel(label: String): Boolean =
    aqLabelHas(label, "pause", "paus", "إيقاف", "ايقاف", "مؤقت", "duraklat")

private fun aqCallActions(
    data: TickerData,
    interactions: TickerInteractions?
): Pair<TickerAction?, TickerAction?>? {
    val answer = aqFindAction(
        data,
        "answer", "accept", "pick up", "pickup",
        "رد", "إجابة", "قبول",
        "répondre", "responder", "annehmen", "yanıtla", "cevapla"
    )
    val reject = aqFindAction(
        data,
        "declin", "reject", "hang", "end call", "end",
        "رفض", "إنهاء", "انهاء", "تجاهل",
        "refuser", "rechazar", "ablehnen", "reddet"
    )
    if (answer == null && reject == null) return null
    // In the live overlay, fall back unless the notification actually
    // carries a call action we can fire.
    if (interactions != null && answer?.pendingIntent == null && reject?.pendingIntent == null) {
        return null
    }
    return answer to reject
}

private data class AqControl(
    val action: TickerAction?,
    val icon: ImageVector,
    val label: String
)

private fun aqMusicControls(data: TickerData): List<AqControl>? {
    val prev = aqFindAction(
        data,
        "previous", "prev", "rewind", "back",
        "السابق", "سابق", "précédent", "anterior", "zurück", "önceki"
    )
    val play = aqFindAction(
        data,
        "play", "pause", "resume", "paus",
        "تشغيل", "إيقاف", "ايقاف", "استئناف", "مؤقت",
        "lecture", "reproduc", "pausa", "wiedergabe", "oynat", "duraklat"
    )
    val next = aqFindAction(
        data,
        "next", "forward", "skip",
        "التالي", "تالي", "suivant", "siguiente", "weiter", "sonraki"
    )
    if (prev == null && play == null && next == null) {
        if (data.actions.isEmpty()) return null
        return data.actions.take(3).mapIndexed { index, action ->
            val icon = when {
                data.actions.size == 1 -> Icons.Filled.PlayArrow
                index == 0 -> Icons.Filled.SkipPrevious
                index == data.actions.lastIndex -> Icons.Filled.SkipNext
                aqIsPauseLabel(action.label) -> Icons.Filled.Pause
                else -> Icons.Filled.PlayArrow
            }
            AqControl(action, icon, action.label)
        }
    }
    val controls = mutableListOf<AqControl>()
    prev?.let { controls += AqControl(it, Icons.Filled.SkipPrevious, "Prev") }
    play?.let {
        controls += AqControl(
            it,
            if (aqIsPauseLabel(it.label)) Icons.Filled.Pause else Icons.Filled.PlayArrow,
            if (aqIsPauseLabel(it.label)) "Pause" else "Play"
        )
    }
    next?.let { controls += AqControl(it, Icons.Filled.SkipNext, "Next") }
    return controls
}

private fun aqKindIcon(kind: TickerKind) = when (kind) {
    TickerKind.Message -> Icons.Filled.Chat
    TickerKind.Call -> Icons.Filled.Phone
    TickerKind.Flight -> Icons.Filled.Flight
    TickerKind.Music -> Icons.Filled.MusicNote
    TickerKind.Generic -> Icons.Filled.Notifications
}
