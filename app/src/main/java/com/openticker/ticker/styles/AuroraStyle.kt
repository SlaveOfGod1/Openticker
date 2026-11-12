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
import androidx.compose.foundation.ExperimentalFoundationApi
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
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
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
import kotlin.math.PI
import kotlin.math.sin
import kotlin.random.Random

/*
 * Aurora — a compact night-sky notification.
 *
 * The card itself is still a solid night panel, but the sky inside flows
 * continuously: three wavy aurora ribbons, drifting rays and twinkling stars.
 * Only the buttons are glass, with a single mint wavy border that animates.
 */

private val AuroraDisplay = FontFamily(
    Font(R.font.sora, FontWeight.Light),
    Font(R.font.sora, FontWeight.Normal),
    Font(R.font.sora, FontWeight.Medium),
    Font(R.font.sora, FontWeight.SemiBold)
)
private val AuroraMono = FontFamily(Font(R.font.share_tech_mono))

private val AuroraInk = Color(0xFFF4FFF9)
private val AuroraBody = Color(0xFFC1DED3)
private val AuroraTime = Color(0xFFA9D8D0)
private val AuroraTeal = Color(0xFF4EA8DE)
private val AuroraViolet = Color(0xFF8B7BFF)

private val AuroraTextShadow = TextStyle(
    shadow = Shadow(
        color = Color(0xB0000000),
        offset = Offset(0f, 2f),
        blurRadius = 10f
    )
)

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun CuratedAuroraTicker(
    data: TickerData,
    interactions: TickerInteractions?,
    animateIn: Boolean,
    exitFraction: () -> Float
) {
    val context = LocalContext.current
    val options = Prefs.aurora(context)
    val accent = Color(options.accentArgb)
    val intensity = (options.intensity / 100f).coerceIn(0f, 1f)
    val effect = options.effect
    val showLogo = options.showLogo
    val showTime = options.showTime
    val avatarSize = options.avatarSize.dp
    val lines = options.lines

    val enter = remember { Animatable(if (animateIn) 0f else 1f) }
    LaunchedEffect(animateIn) {
        if (!animateIn) {
            enter.snapTo(1f)
            return@LaunchedEffect
        }
        enter.snapTo(0f)
        enter.animateTo(1f, tween(820, easing = FastOutSlowInEasing))
    }

    val transition = rememberInfiniteTransition(label = "aurora")
    val phaseState = transition.animateFloat(
        initialValue = 0f,
        targetValue = (2f * PI).toFloat(),
        animationSpec = infiniteRepeatable(tween(16000, easing = LinearEasing)),
        label = "aurora-phase"
    )
    val twinkleState = transition.animateFloat(
        initialValue = 0.45f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(3200, easing = LinearEasing), RepeatMode.Reverse),
        label = "star-twinkle"
    )
    val dashState = transition.animateFloat(
        initialValue = 0f,
        targetValue = 28f,
        animationSpec = infiniteRepeatable(tween(3200, easing = LinearEasing)),
        label = "border-dash"
    )

    val shape = RoundedCornerShape(20.dp)

    BoxWithConstraints(Modifier.fillMaxWidth()) {
        // Keeps the card wide and rectangular on every phone width.
        val minHeight = (maxWidth / 2.2f).coerceIn(168.dp, 204.dp)

        Box(
            Modifier
                .fillMaxWidth()
                .graphicsLayer {
                    val ex = exitFraction().coerceIn(0f, 1f)
                    val e = enter.value
                    alpha = (if (ex > 0f) 1f - ex else e).coerceIn(0f, 1f)
                    translationY = if (ex > 0f) {
                        ex * 10.dp.toPx()
                    } else {
                        (1f - e) * 12.dp.toPx()
                    }
                    val scale = if (ex > 0f) 1f - 0.02f * ex else 0.97f + 0.03f * e
                    scaleX = scale
                    scaleY = scale
                    transformOrigin = TransformOrigin(0.5f, 0.55f)
                }
                .clip(shape)
                .background(Color(0xFF020A10))
                .border(1.dp, accent.copy(alpha = 0.14f + 0.12f * intensity), shape)
        ) {
            AuroraScene(
                accent = accent,
                teal = AuroraTeal,
                violet = AuroraViolet,
                intensity = intensity,
                effect = effect,
                phase = { phaseState.value },
                twinkle = { twinkleState.value },
                modifier = Modifier.matchParentSize()
            )

            // Keeps the message readable without hiding the sky.
            Box(
                Modifier
                    .matchParentSize()
                    .background(
                        Brush.verticalGradient(
                            0f to Color.Transparent,
                            0.32f to Color.Transparent,
                            0.68f to Color(0x88010A10),
                            1f to Color(0xCC010A10)
                        )
                    )
            )

            Column(
                Modifier
                    .fillMaxWidth()
                    .heightIn(min = minHeight)
                    .graphicsLayer {
                        val ex = exitFraction().coerceIn(0f, 1f)
                        val e = enter.value
                        alpha = if (ex > 0f) {
                            (1f - ex * 2.4f).coerceIn(0f, 1f)
                        } else {
                            ((e - 0.28f) / 0.72f).coerceIn(0f, 1f)
                        }
                        translationY = if (ex > 0f) {
                            ex * 8.dp.toPx()
                        } else {
                            (1f - e) * 10.dp.toPx()
                        }
                    }
                    .padding(14.dp),
                verticalArrangement = Arrangement.Bottom
            ) {
                Row(verticalAlignment = Alignment.Top) {
                    if (showLogo) {
                        AuroraLogo(data, accent, avatarSize, intensity)
                        Spacer(Modifier.width(10.dp))
                    }
                    Column(Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.Bottom) {
                            Text(
                                data.title,
                                color = AuroraInk,
                                fontSize = 20.sp,
                                lineHeight = 24.sp,
                                fontWeight = FontWeight.SemiBold,
                                fontFamily = AuroraDisplay,
                                letterSpacing = (-0.3).sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                style = AuroraTextShadow,
                                modifier = Modifier.weight(1f)
                            )
                            if (showTime) {
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    data.time.uppercase(),
                                    color = AuroraTime,
                                    fontSize = 9.sp,
                                    fontFamily = AuroraMono,
                                    maxLines = 1,
                                    style = AuroraTextShadow
                                )
                            }
                        }
                        Spacer(Modifier.height(4.dp))
                        AuroraBodyText(data.body, lines)
                    }
                }

                Spacer(Modifier.height(25.dp))
                AuroraActions(
                    data = data,
                    interactions = interactions,
                    accent = accent,
                    intensity = intensity,
                    effect = effect,
                    dashPhase = { dashState.value }
                )
            }
        }
    }
}

/* ------------------------------------------------------------------ */
/* Sky                                                                 */
/* ------------------------------------------------------------------ */

@Composable
private fun AuroraScene(
    accent: Color,
    teal: Color,
    violet: Color,
    intensity: Float,
    effect: Boolean,
    phase: () -> Float,
    twinkle: () -> Float,
    modifier: Modifier = Modifier
) {
    val stars = remember {
        val random = Random(11)
        List(20) {
            Triple(
                random.nextFloat(),
                random.nextFloat() * 0.68f,
                0.55f + random.nextFloat() * 0.9f
            )
        }
    }

    Canvas(modifier) {
        drawRect(
            Brush.verticalGradient(
                listOf(
                    Color(0xFF01050D),
                    Color(0xFF03101D),
                    Color(0xFF05202A),
                    Color(0xFF020B11)
                )
            )
        )

        val tw = twinkle()
        stars.forEachIndexed { index, star ->
            val sparkle = (0.5f + 0.5f * sin(tw * 2f * PI.toFloat() + index * 1.7f))
                .coerceIn(0f, 1f)
            drawCircle(
                Color.White.copy(alpha = 0.25f + 0.5f * sparkle),
                radius = 0.65.dp.toPx() * star.third,
                center = Offset(size.width * star.first, size.height * star.second)
            )
        }

        if (effect && intensity > 0.01f) {
            val p = phase()

            drawAuroraRibbon(
                baseY = size.height * 0.01f,
                ribbonHeight = size.height * 0.30f,
                amplitude = 10.dp.toPx(),
                frequency = 1.6f,
                phase = p,
                colors = listOf(
                    accent.copy(alpha = 0.52f * intensity),
                    accent.copy(alpha = 0.18f * intensity),
                    Color.Transparent
                )
            )
            drawAuroraRibbon(
                baseY = size.height * 0.10f,
                ribbonHeight = size.height * 0.24f,
                amplitude = 13.dp.toPx(),
                frequency = 2.1f,
                phase = p * 1.18f + 1.1f,
                colors = listOf(
                    teal.copy(alpha = 0.38f * intensity),
                    violet.copy(alpha = 0.16f * intensity),
                    Color.Transparent
                )
            )
            drawAuroraRibbon(
                baseY = size.height * 0.21f,
                ribbonHeight = size.height * 0.18f,
                amplitude = 8.dp.toPx(),
                frequency = 2.9f,
                phase = p * 0.82f + 2.3f,
                colors = listOf(
                    accent.copy(alpha = 0.28f * intensity),
                    teal.copy(alpha = 0.12f * intensity),
                    Color.Transparent
                )
            )

            // thin vertical rays moving with the same flow
            val flow = p / (2f * PI.toFloat())
            for (i in 0 until 18) {
                val x = size.width * ((i / 18f + flow * 0.12f) % 1f)
                drawLine(
                    accent.copy(alpha = 0.055f * intensity),
                    Offset(x, 0f),
                    Offset(x + 3.dp.toPx(), size.height * 0.42f),
                    1.dp.toPx()
                )
            }

            // soft light resting on the bottom of the card
            drawRect(
                Brush.verticalGradient(
                    listOf(Color.Transparent, accent.copy(alpha = 0.10f * intensity)),
                    startY = size.height * 0.72f,
                    endY = size.height
                )
            )
        }

        // vignette
        drawRect(
            Brush.radialGradient(
                colors = listOf(Color.Transparent, Color(0x88000408)),
                center = Offset(size.width * 0.5f, size.height * 0.42f),
                radius = size.maxDimension * 0.82f
            )
        )
    }
}

private fun DrawScope.drawAuroraRibbon(
    baseY: Float,
    ribbonHeight: Float,
    amplitude: Float,
    frequency: Float,
    phase: Float,
    colors: List<Color>
) {
    val width = size.width
    val steps = 36

    fun topY(t: Float): Float =
        baseY + sin(phase + t * frequency * 2f * PI.toFloat()) * amplitude

    fun bottomY(t: Float): Float =
        baseY + ribbonHeight +
            sin(phase * 1.15f + t * frequency * 2f * PI.toFloat() + 1.4f) * amplitude * 1.2f

    val path = Path()
    path.moveTo(0f, topY(0f))
    for (i in 1..steps) {
        val t = i / steps.toFloat()
        path.lineTo(width * t, topY(t))
    }
    for (i in steps downTo 0) {
        val t = i / steps.toFloat()
        path.lineTo(width * t, bottomY(t))
    }
    path.close()

    drawPath(
        path = path,
        brush = Brush.verticalGradient(
            colors = colors,
            startY = baseY - amplitude,
            endY = baseY + ribbonHeight + amplitude
        ),
        blendMode = BlendMode.Screen
    )
}

/* ------------------------------------------------------------------ */
/* Logo                                                                */
/* ------------------------------------------------------------------ */

@Composable
private fun AuroraLogo(
    data: TickerData,
    accent: Color,
    size: Dp,
    intensity: Float
) {
    Box(
        Modifier
            .size(size)
            .clip(CircleShape)
            .background(
                Brush.radialGradient(
                    listOf(
                        accent.copy(alpha = 0.3f + 0.10f * intensity),
                        Color(0xFF03141A)
                    )
                )
            )
            .border(1.dp, accent.copy(alpha = 0.5f + 0.2f * intensity), CircleShape),
        contentAlignment = Alignment.Center
    ) {
        val bitmap = data.icon
        if (bitmap != null) {
            Image(
                bitmap = bitmap.asImageBitmap(),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize().clip(CircleShape)
            )
        } else {
            Icon(
                auroraIconFor(data.kind),
                contentDescription = null,
                tint = accent,
                modifier = Modifier.size(size * 0.46f)
            )
        }
    }
}

private fun auroraIconFor(kind: TickerKind) = when (kind) {
    TickerKind.Message -> Icons.Filled.Chat
    TickerKind.Call -> Icons.Filled.Phone
    TickerKind.Flight -> Icons.Filled.Flight
    TickerKind.Music -> Icons.Filled.MusicNote
    TickerKind.Generic -> Icons.Filled.Notifications
}

/* ------------------------------------------------------------------ */
/* Body                                                                */
/* ------------------------------------------------------------------ */

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun AuroraBodyText(body: String, lines: Int) {
    val bodySp = Prefs.fontSize(LocalContext.current)
    val scrollSpeed = Prefs.speed(LocalContext.current)
    if (lines <= 0) {
        Text(
            body,
            color = AuroraBody,
            fontSize = bodySp.sp,
            lineHeight = (bodySp * 1.28f).sp,
            fontFamily = AuroraDisplay,
            fontWeight = FontWeight.Normal,
            maxLines = 1,
            softWrap = false,
            overflow = TextOverflow.Clip,
            style = AuroraTextShadow,
            modifier = Modifier
                .fillMaxWidth()
                .basicMarquee(iterations = Int.MAX_VALUE, velocity = (30f * scrollSpeed).dp)
        )
    } else {
        Text(
            body,
            color = AuroraBody,
            fontSize = bodySp.sp,
            lineHeight = (bodySp * 1.28f).sp,
            fontFamily = AuroraDisplay,
            fontWeight = FontWeight.Normal,
            minLines = lines,
            maxLines = lines,
            overflow = TextOverflow.Ellipsis,
            style = AuroraTextShadow,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

/* ------------------------------------------------------------------ */
/* Actions                                                             */
/* ------------------------------------------------------------------ */

@Composable
private fun AuroraActions(
    data: TickerData,
    interactions: TickerInteractions?,
    accent: Color,
    intensity: Float,
    effect: Boolean,
    dashPhase: () -> Float
) {
    when (data.kind) {
        TickerKind.Call -> AuroraCallActions(data, interactions, accent, intensity, effect, dashPhase)
        TickerKind.Music -> AuroraMusicActions(data, interactions, accent, intensity, effect, dashPhase)
        else -> AuroraDefaultActions(data, interactions, accent, intensity, effect, dashPhase)
    }
}

@Composable
private fun AuroraDefaultActions(
    data: TickerData,
    interactions: TickerInteractions?,
    accent: Color,
    intensity: Float,
    effect: Boolean,
    dashPhase: () -> Float
) {
    val buttons = notificationActionSpecs(data, interactions)

    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        buttons.forEach { spec ->
            AuroraWavyButton(
                onClick = spec.onClick,
                accent = accent,
                intensity = intensity,
                effect = effect,
                dashPhase = dashPhase,
                modifier = Modifier.weight(1f)
            ) {
                Icon(spec.icon, null, tint = accent, modifier = Modifier.size(14.dp))
                Spacer(Modifier.width(6.dp))
                Text(
                    spec.label,
                    color = Color(0xFFEAFFFA),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    fontFamily = AuroraDisplay,
                    maxLines = 1
                )
            }
        }
    }
}

@Composable
private fun AuroraCallActions(
    data: TickerData,
    interactions: TickerInteractions?,
    accent: Color,
    intensity: Float,
    effect: Boolean,
    dashPhase: () -> Float
) {
    val answer = data.actions.firstOrNull {
        labelHas(
            it.label,
            "answer", "accept", "pick up", "pickup",
            "رد", "إجابة", "قبول",
            "répondre", "responder", "annehmen", "yanıtla", "cevapla"
        )
    }
    val reject = data.actions.firstOrNull {
        labelHas(
            it.label,
            "declin", "reject", "hang", "end call", "end",
            "رفض", "إنهاء", "انهاء", "تجاهل",
            "refuser", "rechazar", "ablehnen", "reddet"
        )
    }

    val live = interactions != null
    val canAct = answer?.pendingIntent != null || reject?.pendingIntent != null
    if (answer == null && reject == null) {
        AuroraDefaultActions(data, interactions, accent, intensity, effect, dashPhase)
        return
    }
    if (live && !canAct) {
        AuroraDefaultActions(data, interactions, accent, intensity, effect, dashPhase)
        return
    }

    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        AuroraWavyButton(
            onClick = if (live && reject?.pendingIntent != null) {
                {
                    reject?.let { interactions?.onAction(it) }
                    interactions?.onDismiss()
                }
            } else null,
            accent = accent,
            intensity = intensity,
            effect = effect,
            dashPhase = dashPhase,
            modifier = Modifier.weight(1f)
        ) {
            Icon(Icons.Filled.CallEnd, null, tint = accent, modifier = Modifier.size(14.dp))
            Spacer(Modifier.width(6.dp))
            Text(
                "Reject",
                color = Color(0xFFEAFFFA),
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                fontFamily = AuroraDisplay,
                maxLines = 1
            )
        }

        AuroraWavyButton(
            onClick = if (live && answer?.pendingIntent != null) {
                {
                    answer?.let { interactions?.onAction(it) }
                    interactions?.onDismiss()
                }
            } else null,
            accent = accent,
            intensity = intensity,
            effect = effect,
            dashPhase = dashPhase,
            modifier = Modifier.weight(1f)
        ) {
            Icon(Icons.Filled.Phone, null, tint = accent, modifier = Modifier.size(14.dp))
            Spacer(Modifier.width(6.dp))
            Text(
                "Answer",
                color = Color(0xFFEAFFFA),
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                fontFamily = AuroraDisplay,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun AuroraMusicActions(
    data: TickerData,
    interactions: TickerInteractions?,
    accent: Color,
    intensity: Float,
    effect: Boolean,
    dashPhase: () -> Float
) {
    val controls = mutableListOf<AuroraMediaControl>()

    data.actions.firstOrNull {
        labelHas(
            it.label,
            "previous", "prev", "rewind", "back",
            "السابق", "سابق", "précédent", "anterior", "zurück", "önceki"
        )
    }?.let { controls += AuroraMediaControl(it, Icons.Filled.SkipPrevious) }

    data.actions.firstOrNull {
        labelHas(
            it.label,
            "play", "pause", "resume", "paus",
            "تشغيل", "إيقاف", "ايقاف", "استئناف", "مؤقت",
            "lecture", "reproduc", "pausa", "wiedergabe", "oynat", "duraklat"
        )
    }?.let {
        val icon = if (isPauseLabel(it.label)) Icons.Filled.Pause else Icons.Filled.PlayArrow
        controls += AuroraMediaControl(it, icon)
    }

    data.actions.firstOrNull {
        labelHas(
            it.label,
            "next", "forward", "skip",
            "التالي", "تالي", "suivant", "siguiente", "weiter", "sonraki"
        )
    }?.let { controls += AuroraMediaControl(it, Icons.Filled.SkipNext) }

    if (controls.isEmpty() && data.actions.isNotEmpty()) {
        data.actions.take(3).forEachIndexed { index, action ->
            val icon = when {
                data.actions.size == 1 -> Icons.Filled.PlayArrow
                index == 0 -> Icons.Filled.SkipPrevious
                index == data.actions.lastIndex -> Icons.Filled.SkipNext
                isPauseLabel(action.label) -> Icons.Filled.Pause
                else -> Icons.Filled.PlayArrow
            }
            controls += AuroraMediaControl(action, icon)
        }
    }

    if (controls.isEmpty()) {
        AuroraDefaultActions(data, interactions, accent, intensity, effect, dashPhase)
        return
    }

    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        controls.forEach { control ->
            AuroraWavyButton(
                onClick = if (interactions != null && control.action.pendingIntent != null) {
                    { interactions?.onAction(control.action) }
                } else null,
                accent = accent,
                intensity = intensity,
                effect = effect,
                dashPhase = dashPhase,
                modifier = Modifier.weight(1f)
            ) {
                Icon(
                    control.icon,
                    contentDescription = control.action.label,
                    tint = accent,
                    modifier = Modifier.size(17.dp)
                )
            }
        }
    }
}

private data class AuroraMediaControl(val action: TickerAction, val icon: ImageVector)

private fun labelHas(label: String, vararg keys: String): Boolean =
    keys.any { label.contains(it, ignoreCase = true) }

private fun isPauseLabel(label: String): Boolean =
    labelHas(label, "pause", "paus", "إيقاف", "ايقاف", "مؤقت", "duraklat")

/* ------------------------------------------------------------------ */
/* Wavy glass button                                                   */
/* ------------------------------------------------------------------ */

@Composable
private fun AuroraWavyButton(
    onClick: (() -> Unit)?,
    accent: Color,
    intensity: Float,
    effect: Boolean,
    dashPhase: () -> Float,
    modifier: Modifier = Modifier,
    content: @Composable RowScope.() -> Unit
) {
    Box(
        modifier
            .height(38.dp)
            .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier),
        contentAlignment = Alignment.Center
    ) {
        Box(
            Modifier
                .matchParentSize()
                .clip(RoundedCornerShape(12.dp))
                .background(
                    Brush.verticalGradient(
                        listOf(
                            Color.White.copy(alpha = 0.055f),
                            Color(0xFF061A20).copy(alpha = 0.32f)
                        )
                    )
                )
        )

        Row(
            Modifier.padding(horizontal = 8.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
            content = content
        )

        Canvas(Modifier.matchParentSize()) {
            val path = wavyBorderPath(
                size = size,
                corner = 11.dp.toPx(),
                inset = 1.5.dp.toPx(),
                amplitude = 4.dp.toPx()
            )
            drawPath(
                path = path,
                color = accent.copy(alpha = 0.18f + 0.1f * intensity),
                style = Stroke(width = 1.dp.toPx())
            )
            if (effect) {
                val dash = PathEffect.dashPathEffect(
                    floatArrayOf(10.dp.toPx(), 18.dp.toPx()),
                    -dashPhase().dp.toPx()
                )
                drawPath(
                    path = path,
                    color = accent.copy(alpha = 0.55f + 0.35f * intensity),
                    style = Stroke(
                        width = 1.3.dp.toPx(),
                        cap = StrokeCap.Round,
                        pathEffect = dash
                    )
                )
            } else {
                drawPath(
                    path = path,
                    color = accent.copy(alpha = 0.5f + 0.2f * intensity),
                    style = Stroke(width = 1.2.dp.toPx(), cap = StrokeCap.Round)
                )
            }
        }
    }
}

private fun wavyBorderPath(
    size: Size,
    corner: Float,
    inset: Float,
    amplitude: Float
): Path {
    val left = inset
    val right = size.width - inset
    val top = inset
    val bottom = size.height - inset
    val radius = corner.coerceAtMost(minOf((right - left) / 3f, (bottom - top) / 2.2f))

    val path = Path()
    path.moveTo(left + radius, top)

    val segments = 6
    val segmentWidth = (right - left - 2f * radius) / segments
    var x = left + radius
    for (i in 0 until segments) {
        val nextX = x + segmentWidth
        val controlY = if (i % 2 == 0) top - amplitude else top + amplitude
        path.quadraticBezierTo((x + nextX) / 2f, controlY, nextX, top)
        x = nextX
    }

    path.quadraticBezierTo(right, top, right, top + radius)
    path.lineTo(right, bottom - radius)
    path.quadraticBezierTo(right, bottom, right - radius, bottom)

    x = right - radius
    for (i in 0 until segments) {
        val nextX = x - segmentWidth
        val controlY = if (i % 2 == 0) bottom + amplitude else bottom - amplitude
        path.quadraticBezierTo((x + nextX) / 2f, controlY, nextX, bottom)
        x = nextX
    }

    path.quadraticBezierTo(left, bottom, left, bottom - radius)
    path.lineTo(left, top + radius)
    path.quadraticBezierTo(left, top, left + radius, top)
    path.close()
    return path
}
