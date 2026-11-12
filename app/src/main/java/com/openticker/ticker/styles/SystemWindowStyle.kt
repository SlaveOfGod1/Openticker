package com.openticker.ticker.styles

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
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
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
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
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
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
import kotlinx.coroutines.launch

/*
 * System Window — cyan hologram projection.
 *
 * The card is "printed" upward from a thin projector line: the line ignites,
 * then the panel is scanned open behind a bright edge. Dismissing reverses it
 * and drops the card back into the line.
 */

private val HoloDisplay = FontFamily(
    Font(R.font.orbitron, FontWeight.Normal),
    Font(R.font.orbitron, FontWeight.Bold),
    Font(R.font.orbitron, FontWeight.Black)
)
private val HoloMono = FontFamily(Font(R.font.share_tech_mono))

private val HoloInk = Color(0xFFEFFFFF)
private val HoloBody = Color(0xFFA9E9F7)

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun CuratedSystemWindowTicker(
    data: TickerData,
    interactions: TickerInteractions?,
    animateIn: Boolean,
    exitFraction: () -> Float
) {
    val context = LocalContext.current
    val options = Prefs.systemWindow(context)
    val accent = Color(options.accentArgb)
    val intensity = (options.intensity / 100f).coerceIn(0f, 1f)
    val effect = options.effect
    val showLogo = options.showLogo
    val showTime = options.showTime
    val avatarSize = options.avatarSize.dp
    val lines = options.lines

    val enter = remember { Animatable(if (animateIn) 0f else 1f) }
    val line = remember { Animatable(if (animateIn) 0f else 1f) }

    LaunchedEffect(animateIn) {
        if (!animateIn) {
            enter.snapTo(1f)
            line.snapTo(1f)
            return@LaunchedEffect
        }
        enter.snapTo(0f)
        line.snapTo(0f)
        launch { line.animateTo(1f, tween(260, easing = FastOutSlowInEasing)) }
        enter.animateTo(
            1f,
            tween(560, delayMillis = 120, easing = FastOutSlowInEasing)
        )
    }

    val transition = androidx.compose.animation.core.rememberInfiniteTransition(label = "holo")
    val sweepState = transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(5200, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "sweep"
    )
    val flickerState = transition.animateFloat(
        initialValue = 1f,
        targetValue = 0.955f,
        animationSpec = infiniteRepeatable(
            animation = tween(1900, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "flicker"
    )

    val shape = RoundedCornerShape(8.dp)
    val borderAlpha = 0.35f + 0.5f * intensity
    val cardTop = lerp(Color(0xFF06232F), accent, 0.10f)
    val cardBottom = lerp(Color(0xFF02141D), accent, 0.03f)
    val showBeam = effect && intensity > 0.01f
    val gap = if (showBeam) 30.dp else 0.dp

    Box(Modifier.fillMaxWidth()) {
        // Projector cone + line behind the card.
        HoloBeam(
            accent = accent,
            intensity = intensity,
            effect = effect,
            lineProgress = { line.value },
            exit = exitFraction,
            modifier = Modifier
                .fillMaxWidth()
                .height(gap + 6.dp)
                .align(Alignment.BottomCenter)
        )

        Column(Modifier.fillMaxWidth()) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .graphicsLayer {
                        val ex = exitFraction().coerceIn(0f, 1f)
                        translationY = ((1f - enter.value) + ex) * gap.toPx()
                        alpha = if (ex > 0f) 1f - ex * 0.15f else 1f
                    }
                    .drawWithContent {
                        val ex = exitFraction().coerceIn(0f, 1f)
                        val reveal = if (ex > 0f) 1f - ex else enter.value
                        val edgeY = size.height * (1f - reveal)
                        clipRect(top = edgeY) { this@drawWithContent.drawContent() }
                        if (effect && intensity > 0.02f && reveal > 0.01f && reveal < 0.995f) {
                            val inset = 3.dp.toPx()
                            drawLine(
                                brush = Brush.horizontalGradient(
                                    listOf(
                                        Color.Transparent,
                                        accent.copy(alpha = 0.45f),
                                        Color.White.copy(alpha = 0.9f * intensity),
                                        accent.copy(alpha = 0.45f),
                                        Color.Transparent
                                    )
                                ),
                                start = Offset(inset, edgeY),
                                end = Offset(size.width - inset, edgeY),
                                strokeWidth = 2.dp.toPx()
                            )
                        }
                    }
            ) {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .clip(shape)
                        .background(Brush.verticalGradient(listOf(cardTop, cardBottom)))
                        .border(1.dp, accent.copy(alpha = borderAlpha), shape)
                        .graphicsLayer {
                            alpha = if (effect) flickerState.value else 1f
                        }
                ) {
                    HoloDecoration(
                        accent = accent,
                        intensity = intensity,
                        effect = effect,
                        sweepState = { sweepState.value }
                    )

                    Column(Modifier.fillMaxWidth()) {
                        HoloHeader(data, accent, intensity, effect)

                        Column(Modifier.padding(13.dp)) {
                            Row(verticalAlignment = Alignment.Top) {
                                if (showLogo) {
                                    HoloAppLogo(data, accent, avatarSize, intensity)
                                    Spacer(Modifier.width(10.dp))
                                }
                                Column(Modifier.weight(1f)) {
                                    Row(verticalAlignment = Alignment.Bottom) {
                                        Text(
                                            data.title,
                                            color = HoloInk,
                                            fontSize = 17.sp,
                                            lineHeight = 22.sp,
                                            fontWeight = FontWeight.Bold,
                                            fontFamily = HoloDisplay,
                                            letterSpacing = 0.3.sp,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            modifier = Modifier.weight(1f)
                                        )
                                        if (showTime) {
                                            Spacer(Modifier.width(8.dp))
                                            Text(
                                                data.time.uppercase(),
                                                color = accent.copy(alpha = 0.85f),
                                                fontSize = 9.5.sp,
                                                fontFamily = HoloMono,
                                                maxLines = 1
                                            )
                                        }
                                    }
                                    Spacer(Modifier.height(3.dp))
                                    HoloBodyText(
                                        body = data.body,
                                        lines = lines,
                                        effect = effect,
                                        color = HoloBody
                                    )
                                }
                            }

                            Spacer(Modifier.height(8.dp))
                            HoloDivider(accent, intensity)
                            Spacer(Modifier.height(8.dp))
                            HoloActions(data, interactions, accent, intensity)
                            Spacer(Modifier.height(7.dp))
                            HoloDivider(accent, intensity)
                        }
                    }
                }
            }
            Spacer(Modifier.height(gap))
        }
    }
}

/* ------------------------------------------------------------------ */
/* Header strip                                                        */
/* ------------------------------------------------------------------ */

@Composable
private fun HoloHeader(
    data: TickerData,
    accent: Color,
    intensity: Float,
    effect: Boolean
) {
    Row(
        Modifier
            .fillMaxWidth()
            .height(28.dp)
            .background(accent.copy(alpha = 0.12f + 0.04f * intensity))
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier
                .size(13.dp)
                .clip(RoundedCornerShape(1.dp))
                .background(accent.copy(alpha = 0.12f))
                .border(1.dp, accent.copy(alpha = 0.9f), RoundedCornerShape(1.dp)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                "!",
                color = HoloInk,
                fontSize = 9.sp,
                fontFamily = HoloMono
            )
        }
        Spacer(Modifier.width(7.dp))
        Text(
            "NOTIFICATION",
            color = accent,
            fontSize = 7.5.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = HoloDisplay,
            letterSpacing = 2.6.sp,
            maxLines = 1
        )
        Spacer(Modifier.weight(1f))
        Text(
            data.appName.uppercase().ifBlank { "SYS.01" },
            color = accent.copy(alpha = if (effect) 0.55f else 0.75f),
            fontSize = 8.sp,
            fontFamily = HoloMono,
            letterSpacing = 1.2.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.widthIn(max = 122.dp)
        )
    }
}

/* ------------------------------------------------------------------ */
/* Logo                                                                */
/* ------------------------------------------------------------------ */

@Composable
private fun HoloAppLogo(
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
                        accent.copy(alpha = 0.26f + 0.1f * intensity),
                        Color(0xFF04222E)
                    )
                )
            )
            .border(1.5.dp, accent.copy(alpha = 0.55f + 0.25f * intensity), CircleShape),
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
                iconFor(data.kind),
                contentDescription = null,
                tint = accent,
                modifier = Modifier.size(size * 0.46f)
            )
        }
    }
}

private fun iconFor(kind: TickerKind) = when (kind) {
    TickerKind.Message -> Icons.Filled.Chat
    TickerKind.Call -> Icons.Filled.Phone
    TickerKind.Flight -> Icons.Filled.Flight
    TickerKind.Music -> Icons.Filled.MusicNote
    TickerKind.Generic -> Icons.Filled.Notifications
}

/* ------------------------------------------------------------------ */
/* Body                                                                */
/* ------------------------------------------------------------------ */

@Composable
private fun HoloBodyText(
    body: String,
    lines: Int,
    effect: Boolean,
    color: Color
) {
    val bodySp = Prefs.fontSize(LocalContext.current)
    val scrollSpeed = Prefs.speed(LocalContext.current)
    if (lines <= 0) {
        Text(
            body,
            color = color,
            fontSize = bodySp.sp,
            lineHeight = (bodySp * 1.3f).sp,
            fontFamily = HoloMono,
            maxLines = 1,
            softWrap = false,
            overflow = TextOverflow.Clip,
            modifier = Modifier
                .fillMaxWidth()
                .basicMarquee(iterations = Int.MAX_VALUE, velocity = (30f * scrollSpeed).dp)
        )
    } else {
        Text(
            body,
            color = color,
            fontSize = bodySp.sp,
            lineHeight = (bodySp * 1.3f).sp,
            fontFamily = HoloMono,
            minLines = lines,
            maxLines = lines,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

/* ------------------------------------------------------------------ */
/* Actions                                                             */
/* ------------------------------------------------------------------ */

@Composable
private fun HoloActions(
    data: TickerData,
    interactions: TickerInteractions?,
    accent: Color,
    intensity: Float
) {
    when (data.kind) {
        TickerKind.Call -> HoloCallActions(data, interactions, accent, intensity)
        TickerKind.Music -> HoloMusicActions(data, interactions, accent, intensity)
        else -> HoloDefaultActions(data, interactions, accent, intensity)
    }
}

@Composable
private fun HoloDefaultActions(
    data: TickerData,
    interactions: TickerInteractions?,
    accent: Color,
    intensity: Float
) {
    val buttons = notificationActionSpecs(data, interactions)

    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        buttons.forEach { spec ->
            HoloButton(
                label = spec.label,
                icon = spec.icon,
                onClick = spec.onClick,
                accent = accent,
                intensity = intensity,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

/* Call: answer / reject, wired to the notification's own actions. */
@Composable
private fun HoloCallActions(
    data: TickerData,
    interactions: TickerInteractions?,
    accent: Color,
    intensity: Float
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
        HoloDefaultActions(data, interactions, accent, intensity)
        return
    }
    if (live && !canAct) {
        HoloDefaultActions(data, interactions, accent, intensity)
        return
    }

    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        HoloButton(
            label = "Reject",
            icon = Icons.Filled.CallEnd,
            onClick = if (live && reject?.pendingIntent != null) {
                {
                    reject?.let { interactions?.onAction(it) }
                    interactions?.onDismiss()
                }
            } else null,
            accent = accent,
            intensity = intensity,
            modifier = Modifier.weight(1f)
        )
        HoloButton(
            label = "Answer",
            icon = Icons.Filled.Phone,
            onClick = if (live && answer?.pendingIntent != null) {
                {
                    answer?.let { interactions?.onAction(it) }
                    interactions?.onDismiss()
                }
            } else null,
            accent = accent,
            intensity = intensity,
            modifier = Modifier.weight(1f)
        )
    }
}

/* Music: previous / play-pause / next transport controls. */
@Composable
private fun HoloMusicActions(
    data: TickerData,
    interactions: TickerInteractions?,
    accent: Color,
    intensity: Float
) {
    val controls = mutableListOf<MediaControl>()

    data.actions.firstOrNull {
        labelHas(
            it.label,
            "previous", "prev", "rewind", "back",
            "السابق", "سابق", "précédent", "anterior", "zurück", "önceki"
        )
    }?.let { controls += MediaControl(it, Icons.Filled.SkipPrevious) }

    data.actions.firstOrNull {
        labelHas(
            it.label,
            "play", "pause", "resume", "paus",
            "تشغيل", "إيقاف", "ايقاف", "استئناف", "مؤقت",
            "lecture", "reproduc", "pausa", "wiedergabe", "oynat", "duraklat"
        )
    }?.let {
        val icon = if (isPauseLabel(it.label)) Icons.Filled.Pause else Icons.Filled.PlayArrow
        controls += MediaControl(it, icon)
    }

    data.actions.firstOrNull {
        labelHas(
            it.label,
            "next", "forward", "skip",
            "التالي", "تالي", "suivant", "siguiente", "weiter", "sonraki"
        )
    }?.let { controls += MediaControl(it, Icons.Filled.SkipNext) }

    // Localised media actions: keep the player and use the notification's order.
    if (controls.isEmpty() && data.actions.isNotEmpty()) {
        data.actions.take(3).forEachIndexed { index, action ->
            val icon = when {
                data.actions.size == 1 -> Icons.Filled.PlayArrow
                index == 0 -> Icons.Filled.SkipPrevious
                index == data.actions.lastIndex -> Icons.Filled.SkipNext
                isPauseLabel(action.label) -> Icons.Filled.Pause
                else -> Icons.Filled.PlayArrow
            }
            controls += MediaControl(action, icon)
        }
    }

    if (controls.isEmpty()) {
        HoloDefaultActions(data, interactions, accent, intensity)
        return
    }

    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        controls.forEach { control ->
            HoloPlayerButton(
                icon = control.icon,
                description = control.action.label,
                onClick = if (interactions != null && control.action.pendingIntent != null) {
                    { interactions?.onAction(control.action) }
                } else null,
                accent = accent,
                intensity = intensity,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

private data class MediaControl(val action: TickerAction, val icon: ImageVector)

private fun labelHas(label: String, vararg keys: String): Boolean =
    keys.any { label.contains(it, ignoreCase = true) }

private fun isPauseLabel(label: String): Boolean =
    labelHas(label, "pause", "paus", "إيقاف", "ايقاف", "مؤقت", "duraklat")

@Composable
private fun HoloButton(
    label: String,
    icon: ImageVector,
    onClick: (() -> Unit)?,
    accent: Color,
    intensity: Float,
    modifier: Modifier = Modifier
) {
    Row(
        modifier
            .height(36.dp)
            .clip(RoundedCornerShape(2.dp))
            .background(accent.copy(alpha = 0.10f + 0.06f * intensity))
            .border(1.dp, accent.copy(alpha = 0.4f + 0.4f * intensity), RoundedCornerShape(2.dp))
            .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = accent,
            modifier = Modifier.size(12.dp)
        )
        Spacer(Modifier.width(5.dp))
        Text(
            label.uppercase(),
            color = HoloInk,
            fontSize = 8.5.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = HoloDisplay,
            letterSpacing = 0.7.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun HoloPlayerButton(
    icon: ImageVector,
    description: String,
    onClick: (() -> Unit)?,
    accent: Color,
    intensity: Float,
    modifier: Modifier = Modifier
) {
    Box(
        modifier
            .height(36.dp)
            .clip(RoundedCornerShape(2.dp))
            .background(accent.copy(alpha = 0.10f + 0.06f * intensity))
            .border(1.dp, accent.copy(alpha = 0.4f + 0.4f * intensity), RoundedCornerShape(2.dp))
            .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            icon,
            contentDescription = description,
            tint = accent,
            modifier = Modifier.size(16.dp)
        )
    }
}

@Composable
private fun HoloDivider(accent: Color, intensity: Float) {
    Box(
        Modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(
                Brush.horizontalGradient(
                    listOf(
                        Color.Transparent,
                        accent.copy(alpha = 0.24f * intensity),
                        Color.Transparent
                    )
                )
            )
    )
}

/* ------------------------------------------------------------------ */
/* Card decoration                                                     */
/* ------------------------------------------------------------------ */

@Composable
private fun BoxScope.HoloDecoration(
    accent: Color,
    intensity: Float,
    effect: Boolean,
    sweepState: () -> Float
) {
    Canvas(Modifier.matchParentSize()) {
        // holographic sheen
        drawRect(
            Brush.radialGradient(
                colors = listOf(accent.copy(alpha = 0.16f * intensity), Color.Transparent),
                center = Offset(size.width * 0.12f, 0f),
                radius = size.width * 0.95f
            )
        )
        drawRect(
            Brush.radialGradient(
                colors = listOf(accent.copy(alpha = 0.08f * intensity), Color.Transparent),
                center = Offset(size.width, size.height),
                radius = size.width * 0.9f
            )
        )

        if (effect) {
            // scanlines
            var y = 0f
            while (y < size.height) {
                drawLine(
                    accent.copy(alpha = 0.055f * intensity),
                    Offset(0f, y),
                    Offset(size.width, y),
                    1f
                )
                y += 3.dp.toPx()
            }
            // idle scan line drifting down and back up
            val sy = size.height * sweepState().coerceIn(0f, 1f)
            drawLine(
                Brush.horizontalGradient(
                    listOf(
                        Color.Transparent,
                        accent.copy(alpha = 0.12f * intensity),
                        accent.copy(alpha = 0.55f * intensity),
                        accent.copy(alpha = 0.12f * intensity),
                        Color.Transparent
                    )
                ),
                Offset(0f, sy),
                Offset(size.width, sy),
                1.5.dp.toPx()
            )
        }

        // light landing at the bottom of the panel
        drawRect(
            Brush.verticalGradient(
                listOf(Color.Transparent, accent.copy(alpha = 0.10f * intensity)),
                startY = size.height - 44.dp.toPx(),
                endY = size.height
            ),
            Offset(0f, size.height - 44.dp.toPx()),
            Size(size.width, 44.dp.toPx())
        )

        // corner brackets
        val arm = 15.dp.toPx()
        val w = 2.dp.toPx()
        val c = accent.copy(alpha = 0.85f)
        fun bracket(x1: Float, y1: Float, x2: Float, y2: Float) =
            drawLine(c, Offset(x1, y1), Offset(x2, y2), w, StrokeCap.Square)
        bracket(0f, 0f, arm, 0f); bracket(0f, 0f, 0f, arm)
        bracket(size.width - arm, 0f, size.width, 0f)
        bracket(size.width, 0f, size.width, arm)
        bracket(0f, size.height - arm, 0f, size.height)
        bracket(0f, size.height, arm, size.height)
        bracket(size.width, size.height - arm, size.width, size.height)
        bracket(size.width - arm, size.height, size.width, size.height)

        // faint outer glow stroke
        drawRoundRect(
            accent.copy(alpha = 0.18f * intensity),
            topLeft = Offset(-1f, -1f),
            size = Size(size.width + 2f, size.height + 2f),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(9.dp.toPx()),
            style = Stroke(width = 3.dp.toPx())
        )
    }
}

/* ------------------------------------------------------------------ */
/* Projector beam                                                      */
/* ------------------------------------------------------------------ */

@Composable
private fun HoloBeam(
    accent: Color,
    intensity: Float,
    effect: Boolean,
    lineProgress: () -> Float,
    exit: () -> Float,
    modifier: Modifier = Modifier
) {
    Canvas(modifier) {
        if (!effect || intensity <= 0.01f) return@Canvas
        val lp = lineProgress().coerceIn(0f, 1f)
        val ex = exit().coerceIn(0f, 1f)
        if (lp <= 0.001f || ex >= 0.999f) return@Canvas

        val alpha = intensity * (1f - ex)
        val lineY = size.height - 1.dp.toPx()
        val coneHeight = size.height - 4.dp.toPx()

        // light cone growing from the line
        val cone = Path().apply {
            moveTo(size.width / 2f - 2.dp.toPx(), coneHeight)
            lineTo(size.width / 2f + 2.dp.toPx(), coneHeight)
            lineTo(size.width * 0.78f, 0f)
            lineTo(size.width * 0.22f, 0f)
            close()
        }
        drawPath(
            cone,
            Brush.verticalGradient(
                listOf(Color.Transparent, accent.copy(alpha = 0.20f * alpha)),
                startY = 0f,
                endY = coneHeight
            )
        )

        // individual rays
        val rays = 9
        for (i in 0 until rays) {
            val t = i / (rays - 1f)
            val bottomX = size.width / 2f + (t - 0.5f) * 14.dp.toPx()
            val topX = size.width / 2f + (t - 0.5f) * size.width * 0.56f
            drawLine(
                accent.copy(alpha = 0.10f * alpha),
                Offset(bottomX, coneHeight),
                Offset(topX, 0f),
                1.dp.toPx()
            )
        }

        val lineWidth = size.width * 0.64f * lp
        if (lineWidth > 1f) {
            drawLine(
                accent.copy(alpha = 0.28f * alpha),
                Offset(size.width / 2f - lineWidth / 2f, lineY),
                Offset(size.width / 2f + lineWidth / 2f, lineY),
                7.dp.toPx(),
                StrokeCap.Round
            )
            drawLine(
                Color.White.copy(alpha = 0.9f * alpha),
                Offset(size.width / 2f - lineWidth / 2f, lineY),
                Offset(size.width / 2f + lineWidth / 2f, lineY),
                2.dp.toPx(),
                StrokeCap.Round
            )
        }
    }
}
