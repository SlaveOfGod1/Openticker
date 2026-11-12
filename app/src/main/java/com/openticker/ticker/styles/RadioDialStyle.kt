package com.openticker.ticker.styles

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.openticker.R
import com.openticker.data.Prefs
import com.openticker.data.TickerAction
import com.openticker.data.TickerData
import com.openticker.data.TickerInteractions
import com.openticker.data.TickerKind
import kotlinx.coroutines.launch

/*
 * Radio Dial — a vintage wooden table radio notification.
 *
 * Port of radio-dial-concept.html, line by line:
 * wood-grain card with gold pinstripe trim, faint concentric gold rings
 * top-left, cream sunburst behind the radio, brown mesh screen with a
 * gold rim and a sweeping red tuning needle, screen glow pulse, white
 * radial-gradient knobs with dashed rotating gold rings, dark inset
 * border, labels below the knobs, app-icon circle between them, and the
 * full tune-in / radio-drop / knob-pop / knob-sink choreography.
 */

private val DialRighteous = FontFamily(Font(R.font.righteous))
@OptIn(ExperimentalTextApi::class)
private val DialNunitoBold = FontFamily(
    Font(
        R.font.nunito,
        weight = FontWeight.Bold,
        variationSettings = FontVariation.Settings(FontVariation.weight(700))
    )
)
@OptIn(ExperimentalTextApi::class)
private val DialNunitoExtraBold = FontFamily(
    Font(
        R.font.nunito,
        weight = FontWeight.ExtraBold,
        variationSettings = FontVariation.Settings(FontVariation.weight(800))
    )
)

private val DialCream = Color(0xFFF2E8C9)
private val DialText = Color(0xFFFFE9B8)
private val DialMaroonDark = Color(0xFF4A1810)
private val DialWoodLip = Color(0x99100402)

private const val SWEEP_MS = 3000

/** Piecewise keyframe interpolation used for the card's tune-in wobble. */
private fun kf(value: Float, vararg stops: Pair<Float, Float>): Float {
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

/** Delay a 0..1 exit fraction — the mockup sinks the radio/icon 40ms after the card/knobs. */
private fun delayedEx(ex: Float, offset: Float): Float =
    ((ex - offset) / (1f - offset)).coerceIn(0f, 1f)

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun CuratedRadioDialTicker(
    data: TickerData,
    interactions: TickerInteractions?,
    animateIn: Boolean,
    exitFraction: () -> Float
) {
    val context = LocalContext.current
    val options = Prefs.radioDial(context)
    val gold = Color(options.accentArgb)
    val needle = Color(options.secondaryArgb)
    val mesh = Color(options.tertiaryArgb)
    val meshDark = lerp(mesh, Color(0xFF000000), 0.34f)
    val intensity = (options.intensity / 100f).coerceIn(0f, 1f)
    val effect = options.effect
    val showLogo = options.showLogo
    val showTime = options.showTime
    val lines = options.lines
    val iconSize = options.avatarSize.dp

    // ----- entrance choreography ---------------------------------------
    val cardEnter = remember { Animatable(if (animateIn) 0f else 1f) }
    val radioEnter = remember { Animatable(if (animateIn) 0f else 1f) }
    val knobEnter = remember { Animatable(if (animateIn) 0f else 1f) }
    val iconEnter = remember { Animatable(if (animateIn) 0f else 1f) }

    LaunchedEffect(animateIn) {
        if (!animateIn) {
            cardEnter.snapTo(1f)
            radioEnter.snapTo(1f)
            knobEnter.snapTo(1f)
            iconEnter.snapTo(1f)
            return@LaunchedEffect
        }
        cardEnter.snapTo(0f)
        radioEnter.snapTo(0f)
        knobEnter.snapTo(0f)
        iconEnter.snapTo(0f)
        launch {
            cardEnter.animateTo(1f, tween(1000, easing = LinearEasing))
        }
        launch {
            radioEnter.animateTo(
                1f,
                tween(700, delayMillis = 150, easing = CubicBezierEasing(0.2f, 1.3f, 0.35f, 1f))
            )
        }
        launch {
            knobEnter.animateTo(
                1f,
                tween(500, delayMillis = 420, easing = CubicBezierEasing(0.2f, 1.4f, 0.3f, 1f))
            )
        }
        iconEnter.animateTo(
            1f,
            tween(450, delayMillis = 520, easing = CubicBezierEasing(0.2f, 1.4f, 0.3f, 1f))
        )
    }

    // ----- ambient motion ----------------------------------------------
    val transition = rememberInfiniteTransition(label = "radio-dial")
    val needleState = transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(SWEEP_MS, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "sweep"
    )
    val glowState = transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(SWEEP_MS, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glow"
    )
    // dialAdjust — the dashed rings "adjust themselves" every 3 seconds
    val ringAngle = transition.animateFloat(
        initialValue = 0f,
        targetValue = 14f,
        animationSpec = infiniteRepeatable(
            animation = keyframes {
                durationMillis = SWEEP_MS
                0f at 0
                42f at 450
                42f at 1050
                -18f at 1500
                -18f at 1950
                14f at 2400
                14f at SWEEP_MS
            },
            repeatMode = RepeatMode.Restart
        ),
        label = "ring"
    )

    val shape = RoundedCornerShape(24.dp)
    val radioShape = RoundedCornerShape(28.dp, 28.dp, 18.dp, 18.dp)

    androidx.compose.runtime.CompositionLocalProvider(
        androidx.compose.ui.platform.LocalLayoutDirection provides androidx.compose.ui.unit.LayoutDirection.Ltr
    ) {
        BoxWithConstraints(Modifier.fillMaxWidth()) {
            val cardHeight = (maxWidth / 2.05f).coerceIn(198.dp, 222.dp)

            Box(
                Modifier
                    .fillMaxWidth()
                    .height(cardHeight)
                    .graphicsLayer {
                        val ex = exitFraction().coerceIn(0f, 1f)
                        if (ex > 0f) {
                            // tuneOut: sink 12px, shrink to .94, tilt 1deg
                            alpha = 1f - ex
                            translationY = 12.dp.toPx() * ex
                            val s = 1f - 0.06f * ex
                            scaleX = s
                            scaleY = s
                            rotationZ = 1f * ex
                        } else {
                            // tuneIn: drop in with a tiny rotate wobble
                            val e = cardEnter.value.coerceIn(0f, 1f)
                            alpha = kf(e, 0f to 0f, 0.45f to 1f).coerceIn(0f, 1f)
                            translationY = kf(e, 0f to -30f, 0.45f to 5f, 0.7f to -3f, 1f to 0f)
                                .dp.toPx()
                            scaleX = kf(e, 0f to 0.93f, 0.45f to 1.015f, 0.7f to 0.995f, 1f to 1f)
                            scaleY = kf(e, 0f to 0.93f, 0.45f to 0.985f, 0.7f to 1.005f, 1f to 1f)
                            rotationZ = kf(e, 0f to -1.5f, 0.45f to 0.5f, 0.7f to -0.2f, 1f to 0f)
                            transformOrigin = TransformOrigin(0.5f, 0.5f)
                        }
                    }
                    .clip(shape)
                    .background(
                        Brush.verticalGradient(
                            0f to Color(0xFF6B3A1F),
                            0.25f to Color(0xFF5E3018),
                            0.5f to Color(0xFF542A14),
                            0.75f to Color(0xFF4A2412),
                            1f to Color(0xFF3E1C0E)
                        )
                    )
                    .border(2.dp, gold.copy(alpha = 0.25f), shape)
            ) {
                // ----- wood grain, top highlight, inner vignette -----
                Canvas(Modifier.matchParentSize()) {
                    drawWoodGrain(size.width, size.height)
                }

                // ----- radio body -----
                Column(
                    Modifier.padding(start = 16.dp, end = 16.dp, top = 14.dp)
                ) {
                    Column(
                        Modifier.graphicsLayer {
                            // radioOut runs 40ms after the card finishes sinking
                            val ex = delayedEx(exitFraction().coerceIn(0f, 1f), 0.07f)
                            if (ex > 0f) {
                                // radioOut
                                alpha = 1f - ex
                                translationY = 10.dp.toPx() * ex
                                val s = 1f - 0.05f * ex
                                scaleX = s
                                scaleY = s
                            } else {
                                // radioDrop: fall in from 18px with overshoot
                                val e = radioEnter.value.coerceIn(0f, 1.4f)
                                alpha = e.coerceIn(0f, 1f)
                                translationY = (1f - e) * 18.dp.toPx()
                                val s = 0.95f + 0.05f * e
                                scaleX = s
                                scaleY = s
                            }
                        }
                    ) {
                        Box(
                            Modifier
                                .fillMaxWidth()
                                .clip(radioShape)
                                .background(
                                    Brush.verticalGradient(
                                        listOf(Color(0xFF3A1410), Color(0xFF2A0E0A))
                                    )
                                )
                        ) {
                            Column(Modifier.padding(start = 14.dp, end = 14.dp, top = 12.dp, bottom = 10.dp)) {
                                // ----- screen -----
                                Box(
                                    Modifier
                                        .fillMaxWidth()
                                        .height(92.dp)
                                        .clip(RoundedCornerShape(14.dp))
                                        .background(
                                            Brush.verticalGradient(listOf(mesh, meshDark))
                                        )
                                        .border(3.dp, gold, RoundedCornerShape(14.dp))
                                ) {
                                    // mesh lines
                                    Canvas(Modifier.matchParentSize()) {
                                        var x = 0f
                                        val step = 9.dp.toPx()
                                        while (x < size.width) {
                                            drawLine(
                                                Color(0x4720160B),
                                                Offset(x, 0f),
                                                Offset(x, size.height),
                                                2.dp.toPx()
                                            )
                                            x += step
                                        }
                                    }
                                    // red tuning needle
                                    Canvas(Modifier.matchParentSize()) {
                                        val p = if (effect) needleState.value else 0.5f
                                        val x = size.width * (0.06f + 0.87f * p)
                                        val y0 = 5.dp.toPx()
                                        val y1 = size.height - 5.dp.toPx()
                                        val glow = 0.35f + 0.65f * intensity
                                        drawLine(
                                            needle.copy(alpha = 0.18f * glow),
                                            Offset(x, y0), Offset(x, y1),
                                            9.dp.toPx(), StrokeCap.Round
                                        )
                                        drawLine(
                                            needle.copy(alpha = 0.38f * glow),
                                            Offset(x, y0), Offset(x, y1),
                                            5.dp.toPx(), StrokeCap.Round
                                        )
                                        drawLine(
                                            needle.copy(alpha = 0.95f),
                                            Offset(x, y0), Offset(x, y1),
                                            2.5.dp.toPx(), StrokeCap.Round
                                        )
                                    }
                                    // screen glow pulse
                                    if (effect) {
                                        Box(
                                            Modifier
                                                .matchParentSize()
                                                .graphicsLayer { alpha = glowState.value * intensity * 0.9f }
                                                .background(Color(0x0FFFE9B8))
                                        )
                                    }
                                    // screen text
                                    Column(
                                        Modifier
                                            .padding(horizontal = 12.dp, vertical = 10.dp)
                                    ) {
                                        Row(verticalAlignment = Alignment.Bottom) {
                                            Text(
                                                data.title,
                                                color = DialText,
                                                fontSize = 18.sp,
                                                lineHeight = 22.sp,
                                                fontFamily = DialRighteous,
                                                letterSpacing = 0.3.sp,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis,
                                                modifier = Modifier.weight(1f)
                                            )
                                            if (showTime) {
                                                Spacer(Modifier.width(8.dp))
                                                Text(
                                                    data.time.uppercase(),
                                                    color = gold,
                                                    fontSize = 9.sp,
                                                    fontFamily = DialNunitoExtraBold,
                                                    letterSpacing = 0.5.sp,
                                                    maxLines = 1
                                                )
                                            }
                                        }
                                        Spacer(Modifier.height(5.dp))
                                        RadioDialBody(data.body, lines)
                                    }
                                }

                                Spacer(Modifier.height(12.dp))

                                // ----- knobs row -----
                                RadioDialKnobs(
                                    data = data,
                                    interactions = interactions,
                                    gold = gold,
                                    iconSize = iconSize,
                                    showLogo = showLogo,
                                    effect = effect,
                                    ringAngle = { ringAngle.value },
                                    knobEnter = { knobEnter.value },
                                    iconEnter = { iconEnter.value },
                                    exitFraction = exitFraction
                                )
                            }
                        }
                        // solid 6px shadow lip under the radio body
                        Box(
                            Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(RoundedCornerShape(0.dp, 0.dp, 18.dp, 18.dp))
                                .background(DialWoodLip)
                        )
                    }
                }
            }
        }
    }
}

/* ------------------------------------------------------------------ */
/* Wood grain / vignette                                               */
/* ------------------------------------------------------------------ */

private fun DrawScope.drawWoodGrain(width: Float, height: Float) {
    // fine vertical grain — 9px cycle
    var x = 0f
    val fine = 9.dp.toPx()
    while (x < width) {
        drawLine(Color(0x0A000000), Offset(x, 0f), Offset(x, height), 1.dp.toPx())
        drawLine(Color(0x06000000), Offset(x + 4.dp.toPx(), 0f), Offset(x + 4.dp.toPx(), height), 1.dp.toPx())
        x += fine
    }
    // wider dark grain streaks — 38px cycle
    x = 0f
    val wide = 38.dp.toPx()
    while (x < width) {
        drawLine(Color(0x0D000000), Offset(x + 12.dp.toPx(), 0f), Offset(x + 12.dp.toPx(), height), 2.dp.toPx())
        drawLine(Color(0x09000000), Offset(x + 24.dp.toPx(), 0f), Offset(x + 24.dp.toPx(), height), 3.dp.toPx())
        x += wide
    }
    // lighter wood highlights — 42px cycle
    x = 0f
    val hi = 42.dp.toPx()
    while (x < width) {
        drawLine(Color(0x08FFDAA5), Offset(x + 20.dp.toPx(), 0f), Offset(x + 20.dp.toPx(), height), 4.dp.toPx())
        x += hi
    }
    // top inner highlight — inset 0 2px rgba(255,218,165,.08)
    drawRect(Color(0x14FFDAA5), Offset(0f, 0f), androidx.compose.ui.geometry.Size(width, 2.dp.toPx()))
    // inner shadow — inset 0 0 50px rgba(0,0,0,.25)
    drawRect(
        Brush.radialGradient(
            colors = listOf(Color.Transparent, Color(0x40000000)),
            center = Offset(width / 2f, height * 0.42f),
            radius = maxOf(width, height) * 0.85f
        )
    )
}

/* ------------------------------------------------------------------ */
/* Body text                                                           */
/* ------------------------------------------------------------------ */

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun RadioDialBody(body: String, lines: Int) {
    val bodySp = Prefs.fontSize(LocalContext.current)
    if (lines <= 0) {
        Text(
            body,
            color = DialCream,
            fontSize = bodySp.sp,
            lineHeight = (bodySp * 1.3f).sp,
            fontFamily = DialNunitoBold,
            maxLines = 1,
            softWrap = false,
            overflow = TextOverflow.Clip,
            modifier = Modifier.fillMaxWidth().basicMarquee(
                iterations = Int.MAX_VALUE,
                velocity = (30f * Prefs.speed(LocalContext.current)).dp
            )
        )
    } else {
        Text(
            body,
            color = DialCream,
            fontSize = bodySp.sp,
            lineHeight = (bodySp * 1.3f).sp,
            fontFamily = DialNunitoBold,
            minLines = lines,
            maxLines = lines,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

/* ------------------------------------------------------------------ */
/* Knobs row                                                           */
/* ------------------------------------------------------------------ */

@Composable
private fun RadioDialKnobs(
    data: TickerData,
    interactions: TickerInteractions?,
    gold: Color,
    iconSize: androidx.compose.ui.unit.Dp,
    showLogo: Boolean,
    effect: Boolean,
    ringAngle: () -> Float,
    knobEnter: () -> Float,
    iconEnter: () -> Float,
    exitFraction: () -> Float
) {
    val music = if (data.kind == TickerKind.Music) musicControls(data) else null
    val call = if (data.kind == TickerKind.Call) callActions(data, interactions) else null

    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (music != null) {
            music.forEachIndexed { index, control ->
                if (index > 0) Spacer(Modifier.width(10.dp))
                DialKnob(
                    icon = control.icon,
                    label = control.label,
                    onClick = if (interactions != null && control.action?.pendingIntent != null) {
                        { control.action?.let { interactions.onAction(it) } }
                    } else null,
                    gold = gold,
                    effect = effect,
                    ringAngle = ringAngle,
                    knobEnter = knobEnter,
                    exitFraction = exitFraction
                )
            }
            return@Row
        }

        if (call != null) {
            val (answer, reject) = call
            DialKnob(
                icon = Icons.Filled.CallEnd,
                label = "Reject",
                onClick = if (interactions != null && reject?.pendingIntent != null) {
                    {
                        reject?.let { interactions.onAction(it) }
                        interactions.onDismiss()
                    }
                } else null,
                gold = gold,
                effect = effect,
                ringAngle = ringAngle,
                knobEnter = knobEnter,
                exitFraction = exitFraction
            )
            Spacer(Modifier.width(14.dp))
            if (showLogo) {
                DialAppIcon(data, gold, iconSize, iconEnter, exitFraction)
                Spacer(Modifier.width(14.dp))
            }
            DialKnob(
                icon = Icons.Filled.Phone,
                label = "Answer",
                onClick = if (interactions != null && answer?.pendingIntent != null) {
                    {
                        answer?.let { interactions.onAction(it) }
                        interactions.onDismiss()
                    }
                } else null,
                gold = gold,
                effect = effect,
                ringAngle = ringAngle,
                knobEnter = knobEnter,
                exitFraction = exitFraction
            )
            return@Row
        }

        val knobs = notificationActionSpecs(data, interactions)
        knobs.forEachIndexed { index, spec ->
            if (index > 0) Spacer(Modifier.width(14.dp))
            if (index == 1 && showLogo) {
                DialAppIcon(data, gold, iconSize, iconEnter, exitFraction)
                Spacer(Modifier.width(14.dp))
            }
            DialKnob(
                icon = spec.icon,
                label = spec.label,
                onClick = spec.onClick,
                gold = gold,
                effect = effect,
                ringAngle = ringAngle,
                knobEnter = knobEnter,
                exitFraction = exitFraction
            )
        }
    }
}

@Composable
private fun DialAppIcon(
    data: TickerData,
    gold: Color,
    iconSize: androidx.compose.ui.unit.Dp,
    iconEnter: () -> Float,
    exitFraction: () -> Float
) {
    Box(
        Modifier
            .size(iconSize)
            .graphicsLayer {
                // knobSink, 40ms after the knobs
                val ex = delayedEx(exitFraction().coerceIn(0f, 1f), 0.07f)
                if (ex > 0f) {
                    alpha = 1f - ex
                    val s = 1f - 0.5f * ex
                    scaleX = s
                    scaleY = s
                    rotationZ = -10f * ex
                } else {
                    val e = iconEnter().coerceIn(0f, 1.5f)
                    alpha = e.coerceIn(0f, 1f)
                    val s = 0.4f + 0.6f * e
                    scaleX = s
                    scaleY = s
                    rotationZ = (1f - e) * -15f
                }
            }
            .clip(CircleShape)
            .background(DialMaroonDark)
            .border(2.dp, gold, CircleShape),
        contentAlignment = Alignment.Center
    ) {
        val icon = data.icon
        if (icon != null) {
            Image(
                bitmap = icon.asImageBitmap(),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize().clip(CircleShape)
            )
        } else {
            Icon(
                kindIcon(data.kind),
                contentDescription = null,
                tint = gold,
                modifier = Modifier.size(iconSize * 0.5f)
            )
        }
    }
}

private fun kindIcon(kind: TickerKind) = when (kind) {
    TickerKind.Message -> Icons.Filled.Chat
    TickerKind.Call -> Icons.Filled.Phone
    TickerKind.Flight -> Icons.Filled.Flight
    TickerKind.Music -> Icons.Filled.MusicNote
    TickerKind.Generic -> Icons.Filled.Notifications
}

@Composable
private fun DialKnob(
    icon: ImageVector,
    label: String,
    onClick: (() -> Unit)?,
    gold: Color,
    effect: Boolean,
    ringAngle: () -> Float,
    knobEnter: () -> Float,
    exitFraction: () -> Float
) {
    Column(
        Modifier.width(80.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Box(
            Modifier
                .size(48.dp)
                .graphicsLayer {
                    val ex = exitFraction().coerceIn(0f, 1f)
                    if (ex > 0f) {
                        // knobSink: shrink to .5, rotate -10deg
                        alpha = 1f - ex
                        val s = 1f - 0.5f * ex
                        scaleX = s
                        scaleY = s
                        rotationZ = -10f * ex
                    } else {
                        // knobPop: grow from .4 with a -15deg twist
                        val e = knobEnter().coerceIn(0f, 1.6f)
                        alpha = e.coerceIn(0f, 1f)
                        val s = 0.4f + 0.6f * e
                        scaleX = s
                        scaleY = s
                        rotationZ = (1f - e) * -15f
                    }
                }
                .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier)
        ) {
            Canvas(Modifier.matchParentSize()) {
                val r = size.minDimension / 2f
                // solid bottom edge — 0 3px 0 rgba(32,22,11,.5)
                drawCircle(
                    color = Color(0x8020160B),
                    radius = r,
                    center = Offset(center.x, center.y + 3.dp.toPx())
                )
                // face — radial-gradient(circle at 35% 30%, #FFFDF5, cream 65%)
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(Color(0xFFFFFDF5), DialCream),
                        center = Offset(size.width * 0.35f, size.height * 0.30f),
                        radius = size.minDimension * 0.75f
                    ),
                    radius = r,
                    center = center
                )
                // 2.5px border rgba(32,22,11,.7)
                drawCircle(
                    color = Color(0xB320160B),
                    radius = r - 1.25.dp.toPx(),
                    style = Stroke(2.5.dp.toPx())
                )
                // inset ring — inset 0 0 0 4px rgba(32,22,11,.1)
                drawCircle(
                    color = Color(0x1A20160B),
                    radius = r - 4.5.dp.toPx(),
                    style = Stroke(4.dp.toPx())
                )
                // rotating dashed gold ring — inset -7px, 2px dashed rgba(232,179,58,.5)
                if (effect) {
                    rotate(ringAngle(), pivot = center) {
                        drawCircle(
                            color = gold.copy(alpha = 0.5f),
                            radius = r + 7.dp.toPx(),
                            style = Stroke(
                                width = 2.dp.toPx(),
                                pathEffect = PathEffect.dashPathEffect(
                                    floatArrayOf(6.dp.toPx(), 5.dp.toPx()),
                                    0f
                                )
                            )
                        )
                    }
                } else {
                    drawCircle(
                        color = gold.copy(alpha = 0.5f),
                        radius = r + 7.dp.toPx(),
                        style = Stroke(
                            width = 2.dp.toPx(),
                            pathEffect = PathEffect.dashPathEffect(
                                floatArrayOf(6.dp.toPx(), 5.dp.toPx()),
                                0f
                            )
                        )
                    )
                }
            }
            Icon(
                icon,
                contentDescription = label,
                tint = DialMaroonDark,
                modifier = Modifier
                    .size(18.dp)
                    .align(Alignment.Center)
            )
        }
        Text(
            label.uppercase(),
            color = DialCream,
            fontSize = 8.sp,
            fontFamily = DialNunitoExtraBold,
            letterSpacing = 0.8.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center
        )
    }
}

/* ------------------------------------------------------------------ */
/* Kind-aware actions                                                  */
/* ------------------------------------------------------------------ */

private fun labelHas(label: String, vararg keys: String): Boolean =
    keys.any { label.contains(it, ignoreCase = true) }

private fun findAction(data: TickerData, vararg keys: String): TickerAction? =
    data.actions.firstOrNull { labelHas(it.label, *keys) }

private fun isPauseLabel(label: String): Boolean =
    labelHas(label, "pause", "paus", "إيقاف", "ايقاف", "مؤقت", "duraklat")

private fun callActions(
    data: TickerData,
    interactions: TickerInteractions?
): Pair<TickerAction?, TickerAction?>? {
    val answer = findAction(
        data,
        "answer", "accept", "pick up", "pickup",
        "رد", "إجابة", "قبول",
        "répondre", "responder", "annehmen", "yanıtla", "cevapla"
    )
    val reject = findAction(
        data,
        "declin", "reject", "hang", "end call", "end",
        "رفض", "إنهاء", "انهاء", "تجاهل",
        "refuser", "rechazar", "ablehnen", "reddet"
    )
    if (answer == null && reject == null) return null
    // In the live overlay, fall back to Open/Dismiss unless the
    // notification actually carries a call action we can fire.
    if (interactions != null && answer?.pendingIntent == null && reject?.pendingIntent == null) {
        return null
    }
    return answer to reject
}

private data class RadioDialControl(val action: TickerAction?, val icon: ImageVector, val label: String)

private fun musicControls(data: TickerData): List<RadioDialControl>? {
    val prev = findAction(
        data,
        "previous", "prev", "rewind", "back",
        "السابق", "سابق", "précédent", "anterior", "zurück", "önceki"
    )
    val play = findAction(
        data,
        "play", "pause", "resume", "paus",
        "تشغيل", "إيقاف", "ايقاف", "استئناف", "مؤقت",
        "lecture", "reproduc", "pausa", "wiedergabe", "oynat", "duraklat"
    )
    val next = findAction(
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
                isPauseLabel(action.label) -> Icons.Filled.Pause
                else -> Icons.Filled.PlayArrow
            }
            RadioDialControl(action, icon, action.label)
        }
    }

    val controls = mutableListOf<RadioDialControl>()
    prev?.let { controls += RadioDialControl(it, Icons.Filled.SkipPrevious, "Prev") }
    play?.let {
        controls += RadioDialControl(
            it,
            if (isPauseLabel(it.label)) Icons.Filled.Pause else Icons.Filled.PlayArrow,
            if (isPauseLabel(it.label)) "Pause" else "Play"
        )
    }
    next?.let { controls += RadioDialControl(it, Icons.Filled.SkipNext, "Next") }
    return controls
}
