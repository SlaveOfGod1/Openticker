package com.openticker.ticker.styles

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
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
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.openticker.R
import com.openticker.data.Prefs
import com.openticker.data.TickerAction
import com.openticker.data.TickerData
import com.openticker.data.TickerInteractions
import com.openticker.data.TickerKind
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/*
 * Saturday Morning — chunky cartoon card.
 *
 * Drops in with a squash-and-bounce, then the content pops. Dismissing is a
 * smooth single lift upward. The three main colours (background, message box,
 * buttons) are user-customisable.
 */

@OptIn(ExperimentalTextApi::class)
private val CartoonDisplay = FontFamily(
    Font(
        R.font.baloo2,
        weight = FontWeight.ExtraBold,
        variationSettings = FontVariation.Settings(FontVariation.weight(800))
    )
)

@OptIn(ExperimentalTextApi::class)
private val CartoonBody = FontFamily(
    Font(
        R.font.nunito,
        weight = FontWeight.Normal,
        variationSettings = FontVariation.Settings(FontVariation.weight(500))
    ),
    Font(
        R.font.nunito,
        weight = FontWeight.Bold,
        variationSettings = FontVariation.Settings(FontVariation.weight(700))
    ),
    Font(
        R.font.nunito,
        weight = FontWeight.ExtraBold,
        variationSettings = FontVariation.Settings(FontVariation.weight(800))
    )
)

private val CartoonInk = Color(0xFF20160B)
private val CartoonCream = Color(0xFFFFF3D6)
private val CartoonYellow = Color(0xFFFFE9B8)
private val CartoonLavender = Color(0xFFF2E6FF)

private fun contrastOn(color: Color): Color =
    if (color.luminance() > 0.52f) CartoonInk else CartoonCream

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun CuratedSaturdayMorningTicker(
    data: TickerData,
    interactions: TickerInteractions?,
    animateIn: Boolean,
    exitFraction: () -> Float
) {
    val context = LocalContext.current
    val options = Prefs.saturdayMorning(context)

    val background = Color(options.secondaryArgb.takeIf { it != 0 } ?: 0xFFFFCF3F.toInt())
    val panel = Color(options.accentArgb.takeIf { it != 0 } ?: 0xFF7A3BD9.toInt())
    val button = Color(options.tertiaryArgb.takeIf { it != 0 } ?: 0xFFE8453C.toInt())

    val ink = contrastOn(background)
    val panelText = contrastOn(panel)
    val buttonText = contrastOn(button)
    val panelTitle = if (panel.luminance() > 0.52f) CartoonInk else CartoonYellow
    val panelBody = if (panel.luminance() > 0.52f) CartoonInk.copy(alpha = 0.78f) else CartoonLavender
    val panelTime = panelTitle.copy(alpha = 0.85f)

    val intensity = (options.intensity / 100f).coerceIn(0f, 1f)
    val effect = options.effect
    val showLogo = options.showLogo
    val showTime = options.showTime
    val avatarSize = options.avatarSize.dp
    val lines = options.lines

    val enter = remember { Animatable(if (animateIn) 0f else 1f) }
    val contentIn = remember { Animatable(if (animateIn) 0f else 1f) }
    val buttonsIn = remember { Animatable(if (animateIn) 0f else 1f) }
    LaunchedEffect(animateIn) {
        if (!animateIn) {
            enter.snapTo(1f)
            contentIn.snapTo(1f)
            buttonsIn.snapTo(1f)
            return@LaunchedEffect
        }
        enter.snapTo(0f)
        contentIn.snapTo(0f)
        buttonsIn.snapTo(0f)
        launch { enter.animateTo(1f, spring(dampingRatio = 0.55f, stiffness = 220f)) }
        launch {
            delay(280)
            contentIn.animateTo(1f, tween(320, easing = FastOutSlowInEasing))
        }
        launch {
            delay(480)
            buttonsIn.animateTo(1f, spring(dampingRatio = 0.5f, stiffness = 500f))
        }
    }

    val transition = rememberInfiniteTransition(label = "cartoon")
    val spinState = transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(26000, easing = LinearEasing)),
        label = "sunburst-spin"
    )
    val bobState = transition.animateFloat(
        initialValue = 0f,
        targetValue = (2f * PI).toFloat(),
        animationSpec = infiniteRepeatable(tween(2600, easing = LinearEasing)),
        label = "dots-bob"
    )
    val wiggleState = transition.animateFloat(
        initialValue = 0f,
        targetValue = (2f * PI).toFloat(),
        animationSpec = infiniteRepeatable(tween(3400, easing = LinearEasing)),
        label = "asterisk-wiggle"
    )

    val shape = RoundedCornerShape(20.dp)

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
        BoxWithConstraints(Modifier.fillMaxWidth()) {
            val minHeight = (maxWidth / 2.2f).coerceIn(168.dp, 204.dp)

        Box(
            Modifier
                .fillMaxWidth()
                .graphicsLayer {
                    val ex = exitFraction().coerceIn(0f, 1f)
                    val e = enter.value
                    alpha = if (ex > 0f) {
                        if (ex < 0.58f) 1f else (1f - (ex - 0.58f) / 0.42f).coerceIn(0f, 1f)
                    } else {
                        (e * 1.6f).coerceIn(0f, 1f)
                    }
                    translationY = if (ex > 0f) {
                        -140.dp.toPx() * ex
                    } else {
                        (1f - e) * -64.dp.toPx()
                    }
                    scaleX = if (ex > 0f) 1f - 0.07f * ex else 0.90f + 0.10f * e
                    scaleY = if (ex > 0f) 1f + 0.05f * ex else 0.90f + 0.10f * e
                    rotationZ = if (ex > 0f) -3f * ex else (1f - e) * -5f
                    transformOrigin = TransformOrigin(0.5f, 0.55f)
                }
                .drawBehind {
                    drawRoundRect(
                        color = ink.copy(alpha = 0.9f),
                        topLeft = Offset(0f, 6.dp.toPx()),
                        size = size,
                        cornerRadius = CornerRadius(20.dp.toPx())
                    )
                }
                .clip(shape)
                .background(background)
                .border(3.dp, ink, shape)
        ) {
            CartoonDecorations(
                background = background,
                panel = panel,
                button = button,
                ink = ink,
                intensity = intensity,
                effect = effect,
                spin = { spinState.value },
                bob = { bobState.value },
                wiggle = { wiggleState.value },
                modifier = Modifier.matchParentSize()
            )

            Column(
                Modifier
                    .fillMaxWidth()
                    .heightIn(min = minHeight)
                    .graphicsLayer {
                        val ex = exitFraction().coerceIn(0f, 1f)
                        alpha = if (ex > 0f) {
                            (1f - ex * 2.5f).coerceIn(0f, 1f)
                        } else {
                            contentIn.value
                        }
                        translationY = if (ex > 0f) {
                            -16.dp.toPx() * ex
                        } else {
                            (1f - contentIn.value) * 10.dp.toPx()
                        }
                        val s = 0.96f + 0.04f * contentIn.value
                        scaleX = s
                        scaleY = s
                    }
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.Bottom
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (showLogo) {
                        CartoonLogo(
                            data = data,
                            panel = panel,
                            ink = ink,
                            iconTint = panelText,
                            size = avatarSize
                        )
                        Spacer(Modifier.width(10.dp))
                    }

                    Column(
                        Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(panel)
                            .border(2.5.dp, ink, RoundedCornerShape(12.dp))
                            .padding(horizontal = 11.dp, vertical = 8.dp)
                    ) {
                        Row(verticalAlignment = Alignment.Bottom) {
                            Text(
                                data.title,
                                color = panelTitle,
                                fontSize = 19.sp,
                                lineHeight = 23.sp,
                                fontWeight = FontWeight.ExtraBold,
                                fontFamily = CartoonDisplay,
                                letterSpacing = (-0.2).sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f)
                            )
                            if (showTime) {
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    data.time.uppercase(),
                                    color = panelTime,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    fontFamily = CartoonBody,
                                    maxLines = 1
                                )
                            }
                        }
                        Spacer(Modifier.height(3.dp))
                        CartoonBodyText(
                            body = data.body,
                            lines = lines,
                            color = panelBody
                        )
                    }
                }

                Spacer(Modifier.height(18.dp))

    CartoonActions(
        data = data,
        interactions = interactions,
        button = button,
        ink = ink,
        text = buttonText,
        exitFraction = exitFraction,
        buttonsIn = { buttonsIn.value }
    )
            }
        }
    }
    }
}

/* ------------------------------------------------------------------ */
/* Decorations                                                         */
/* ------------------------------------------------------------------ */

@Composable
private fun CartoonDecorations(
    background: Color,
    panel: Color,
    button: Color,
    ink: Color,
    intensity: Float,
    effect: Boolean,
    spin: () -> Float,
    bob: () -> Float,
    wiggle: () -> Float,
    modifier: Modifier = Modifier
) {
    if (!effect || intensity <= 0.01f) return

    Canvas(modifier) {
        val sunColor = lerp(background, CartoonInk, 0.22f)
        val rayAlpha = 0.18f * intensity

        // rotating dark-yellow sunburst in the top-left
        rotate(spin(), pivot = Offset.Zero) {
            val rays = 20
            repeat(rays) { index ->
                val angle = (index * 360f / rays) * PI.toFloat() / 180f
                drawLine(
                    color = sunColor.copy(alpha = rayAlpha),
                    start = Offset.Zero,
                    end = Offset(
                        cos(angle) * 300.dp.toPx(),
                        sin(angle) * 300.dp.toPx()
                    ),
                    strokeWidth = 18.dp.toPx(),
                    cap = StrokeCap.Butt
                )
            }
        }

        // three separated dots, each bobbing independently
        fun dot(x: Float, y: Float, radius: Float, color: Color, phase: Float) {
            val bobbedY = y + sin(bob() + phase) * 3.5.dp.toPx()
            drawCircle(ink, radius + 1.6.dp.toPx(), Offset(x, bobbedY))
            drawCircle(color.copy(alpha = 0.65f + 0.35f * intensity), radius, Offset(x, bobbedY))
        }

        dot(
            x = 25.dp.toPx(),
            y = size.height - 100.dp.toPx(),
            radius = 8.dp.toPx(),
            color = button,
            phase = 0f
        )
        dot(
            x = size.width * 0.34f,
            y = 10.dp.toPx(),
            radius = 5.5.dp.toPx(),
            color = panel,
            phase = 2f
        )
        dot(
            x = size.width - 38.dp.toPx(),
            y = 10.dp.toPx(),
            radius = 4.dp.toPx(),
            color = contrastOn(background),
            phase = 4f
        )

        // hand-drawn asterisk, wiggling
        val cx = 24.dp.toPx()
        val cy = 22.dp.toPx()
        val angle = sin(wiggle()) * 8f
        rotate(angle, pivot = Offset(cx, cy)) {
            for (i in 0..2) {
                val a = ((i * 60f + 90f) * PI / 180.0).toFloat()
                val dx = cos(a) * 9.dp.toPx()
                val dy = sin(a) * 9.dp.toPx()
                drawLine(
                    ink,
                    Offset(cx - dx, cy - dy),
                    Offset(cx + dx, cy + dy),
                    4.dp.toPx(),
                    StrokeCap.Round
                )
                drawLine(
                    button,
                    Offset(cx - dx, cy - dy),
                    Offset(cx + dx, cy + dy),
                    2.dp.toPx(),
                    StrokeCap.Round
                )
            }
        }
    }
}

/* ------------------------------------------------------------------ */
/* Logo                                                                */
/* ------------------------------------------------------------------ */

@Composable
private fun CartoonLogo(
    data: TickerData,
    panel: Color,
    ink: Color,
    iconTint: Color,
    size: Dp
) {
    Box(
        Modifier
            .size(size)
            .clip(CircleShape)
            .background(panel)
            .border(2.5.dp, ink, CircleShape),
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
                cartoonIconFor(data.kind),
                contentDescription = null,
                tint = iconTint,
                modifier = Modifier.size(size * 0.46f)
            )
        }
    }
}

private fun cartoonIconFor(kind: TickerKind) = when (kind) {
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
private fun CartoonBodyText(body: String, lines: Int, color: Color) {
    val bodySp = Prefs.fontSize(LocalContext.current)
    val scrollSpeed = Prefs.speed(LocalContext.current)
    if (lines <= 0) {
        Text(
            body,
            color = color,
            fontSize = bodySp.sp,
            lineHeight = (bodySp * 1.3f).sp,
            fontWeight = FontWeight.Bold,
            fontFamily = CartoonBody,
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
            fontWeight = FontWeight.Bold,
            fontFamily = CartoonBody,
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
private fun CartoonActions(
    data: TickerData,
    interactions: TickerInteractions?,
    button: Color,
    ink: Color,
    text: Color,
    exitFraction: () -> Float,
    buttonsIn: () -> Float
) {
    when (data.kind) {
        TickerKind.Call -> CartoonCallActions(data, interactions, button, ink, text, exitFraction, buttonsIn)
        TickerKind.Music -> CartoonMusicActions(data, interactions, button, ink, text, exitFraction, buttonsIn)
        else -> CartoonDefaultActions(data, interactions, button, ink, text, exitFraction, buttonsIn)
    }
}

@Composable
private fun CartoonDefaultActions(
    data: TickerData,
    interactions: TickerInteractions?,
    button: Color,
    ink: Color,
    text: Color,
    exitFraction: () -> Float,
    buttonsIn: () -> Float
) {
    val buttons = notificationActionSpecs(data, interactions)

    Row(
        Modifier
            .fillMaxWidth()
            .graphicsLayer {
                val ex = exitFraction().coerceIn(0f, 1f)
                alpha = if (ex > 0f) {
                    (1f - ex * 2.5f).coerceIn(0f, 1f)
                } else {
                    buttonsIn()
                }
                val pop = 0.7f + 0.3f * buttonsIn()
                scaleX = pop
                scaleY = pop
            },
        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally)
    ) {
        buttons.forEach { spec ->
            CartoonButton(
                label = spec.label,
                icon = spec.icon,
                onClick = spec.onClick,
                button = button,
                ink = ink,
                text = text,
            )
        }
    }
}

@Composable
private fun CartoonCallActions(
    data: TickerData,
    interactions: TickerInteractions?,
    button: Color,
    ink: Color,
    text: Color,
    exitFraction: () -> Float,
    buttonsIn: () -> Float
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
        CartoonDefaultActions(data, interactions, button, ink, text, exitFraction, buttonsIn)
        return
    }
    if (live && !canAct) {
        CartoonDefaultActions(data, interactions, button, ink, text, exitFraction, buttonsIn)
        return
    }

    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally)
    ) {
        CartoonButton(
            label = "Reject",
            icon = Icons.Filled.CallEnd,
            onClick = if (live && reject?.pendingIntent != null) {
                {
                    reject?.let { interactions?.onAction(it) }
                    interactions?.onDismiss()
                }
            } else null,
            button = button,
            ink = ink,
            text = text,
        )
        CartoonButton(
            label = "Answer",
            icon = Icons.Filled.Phone,
            onClick = if (live && answer?.pendingIntent != null) {
                {
                    answer?.let { interactions?.onAction(it) }
                    interactions?.onDismiss()
                }
            } else null,
            button = button,
            ink = ink,
            text = text,
        )
    }
}

@Composable
private fun CartoonMusicActions(
    data: TickerData,
    interactions: TickerInteractions?,
    button: Color,
    ink: Color,
    text: Color,
    exitFraction: () -> Float,
    buttonsIn: () -> Float
) {
    val controls = mutableListOf<CartoonMediaControl>()

    data.actions.firstOrNull {
        labelHas(
            it.label,
            "previous", "prev", "rewind", "back",
            "السابق", "سابق", "précédent", "anterior", "zurück", "önceki"
        )
    }?.let { controls += CartoonMediaControl(it, Icons.Filled.SkipPrevious) }

    data.actions.firstOrNull {
        labelHas(
            it.label,
            "play", "pause", "resume", "paus",
            "تشغيل", "إيقاف", "ايقاف", "استئناف", "مؤقت",
            "lecture", "reproduc", "pausa", "wiedergabe", "oynat", "duraklat"
        )
    }?.let {
        val icon = if (isPauseLabel(it.label)) Icons.Filled.Pause else Icons.Filled.PlayArrow
        controls += CartoonMediaControl(it, icon)
    }

    data.actions.firstOrNull {
        labelHas(
            it.label,
            "next", "forward", "skip",
            "التالي", "تالي", "suivant", "siguiente", "weiter", "sonraki"
        )
    }?.let { controls += CartoonMediaControl(it, Icons.Filled.SkipNext) }

    if (controls.isEmpty() && data.actions.isNotEmpty()) {
        data.actions.take(3).forEachIndexed { index, action ->
            val icon = when {
                data.actions.size == 1 -> Icons.Filled.PlayArrow
                index == 0 -> Icons.Filled.SkipPrevious
                index == data.actions.lastIndex -> Icons.Filled.SkipNext
                isPauseLabel(action.label) -> Icons.Filled.Pause
                else -> Icons.Filled.PlayArrow
            }
            controls += CartoonMediaControl(action, icon)
        }
    }

    if (controls.isEmpty()) {
        CartoonDefaultActions(data, interactions, button, ink, text, exitFraction, buttonsIn)
        return
    }

    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally)
    ) {
        controls.forEach { control ->
            CartoonButton(
                label = null,
                icon = control.icon,
                onClick = if (interactions != null && control.action.pendingIntent != null) {
                    { interactions?.onAction(control.action) }
                } else null,
                button = button,
                ink = ink,
                text = text
            )
        }
    }
}

private data class CartoonMediaControl(val action: TickerAction, val icon: ImageVector)

private fun labelHas(label: String, vararg keys: String): Boolean =
    keys.any { label.contains(it, ignoreCase = true) }

private fun isPauseLabel(label: String): Boolean =
    labelHas(label, "pause", "paus", "إيقاف", "ايقاف", "مؤقت", "duraklat")

/* ------------------------------------------------------------------ */
/* Button                                                              */
/* ------------------------------------------------------------------ */

@Composable
private fun CartoonButton(
    label: String?,
    icon: ImageVector,
    onClick: (() -> Unit)?,
    button: Color,
    ink: Color,
    text: Color,
    modifier: Modifier = Modifier
) {
    val shape = RoundedCornerShape(12.dp)

    Box(
        modifier
            .height(44.dp)
            .drawBehind {
                drawRoundRect(
                    color = ink,
                    topLeft = Offset(0f, 3.dp.toPx()),
                    size = size,
                    cornerRadius = CornerRadius(12.dp.toPx())
                )
            }
            .clip(shape)
            .background(button)
            .border(2.5.dp, ink, shape)
            .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier)
            .padding(horizontal = 22.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                icon,
                contentDescription = label,
                tint = text,
                modifier = Modifier.size(if (label == null) 19.dp else 16.dp)
            )
            if (label != null) {
                Spacer(Modifier.width(6.dp))
                Text(
                    label,
                    color = text,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.ExtraBold,
                    fontFamily = CartoonDisplay,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}
