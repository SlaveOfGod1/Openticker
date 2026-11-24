package com.openticker.ticker.cards

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.openticker.R
import com.openticker.data.Prefs
import com.openticker.data.TickerData
import com.openticker.data.TickerInteractions
import com.openticker.ticker.styles.notificationActionSpecs
import com.openticker.ui.components.AppBadge
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.launch


/** IDs of the twenty-two illustrated card themes. */
object CardCatalog {
    val ids: Set<String> = setOf(
        "arcade", "flap", "vinyl", "polaroid", "sticky",
        "terminal", "blueprint", "origami", "glass", "lava",
        "vapor", "crystal", "map", "terrarium", "frost",
        "storm", "zen", "bento", "balloon", "marquee", "paws", "bamboo"
    )
}

/* ==================================================================== */
/* Fonts used by the twenty-two illustrated card themes                         */
/* ==================================================================== */

@OptIn(ExperimentalTextApi::class)
internal object ThemeFonts {

    private fun family(res: Int, weight: FontWeight? = null, variation: Float? = null) =
        FontFamily(
            Font(
                res,
                weight = weight ?: FontWeight.Normal,
                variationSettings = variation?.let {
                    FontVariation.Settings(FontVariation.weight(it.toInt()))
                } ?: FontVariation.Settings()
            )
        )

    val Bebas = family(R.font.bebas_neue)
    val Caveat = family(R.font.caveat, FontWeight.Bold, 700f)
    val Cormorant = family(R.font.cormorant_garamond, FontWeight.SemiBold, 600f)
    val CormorantBold = family(R.font.cormorant_garamond, FontWeight.Bold, 700f)
    val CormorantItalic = FontFamily(
        Font(
            R.font.cormorant_garamond,
            weight = FontWeight.SemiBold,
            style = FontStyle.Italic,
            variationSettings = FontVariation.Settings(FontVariation.weight(600))
        )
    )
    val IMFell = family(R.font.im_fell_english)
    val Oswald = family(R.font.oswald, FontWeight.SemiBold, 600f)
    val OswaldBold = family(R.font.oswald, FontWeight.Bold, 700f)
    val PressStart = family(R.font.press_start_2p)
    val Quicksand = family(R.font.quicksand, FontWeight.SemiBold, 600f)
    val QuicksandBold = family(R.font.quicksand, FontWeight.Bold, 700f)
    val SpaceMono = family(R.font.space_mono)
    val SpaceMonoBold = family(R.font.space_mono_bold)
    val VT323 = family(R.font.vt323)
    val Yeseva = family(R.font.yeseva_one)
    val Zen = family(R.font.zen_maru_gothic, FontWeight.Medium)
    val ZenBold = family(R.font.zen_maru_gothic, FontWeight.Bold, 700f)
    val Baloo = family(R.font.baloo2, FontWeight.Bold, 700f)
    val BalooBlack = family(R.font.baloo2, FontWeight.ExtraBold, 800f)
    val Fredoka = family(R.font.fredoka, FontWeight.SemiBold, 600f)
    val FredokaBold = family(R.font.fredoka, FontWeight.Bold, 700f)
    val Nunito = family(R.font.nunito, FontWeight.Bold, 700f)
    val NunitoSemi = family(R.font.nunito, FontWeight.SemiBold, 600f)
    val NunitoBlack = family(R.font.nunito, FontWeight.ExtraBold, 800f)
    val Righteous = family(R.font.righteous)
    val Archivo = family(R.font.archivo_black)
    val Orbitron = family(R.font.orbitron)
    val Sora = family(R.font.sora)
}

/* ==================================================================== */
/* Keyframes                                                            */
/* ==================================================================== */

/** Piecewise-linear keyframe lookup: `stops` are fraction-of-animation to value. */
internal fun keyframe(t: Float, vararg stops: Pair<Float, Float>): Float {
    if (stops.isEmpty()) return 0f
    if (t <= stops.first().first) return stops.first().second
    var i = 0
    while (i < stops.size - 1) {
        val (t0, v0) = stops[i]
        val (t1, v1) = stops[i + 1]
        if (t <= t1) {
            val f = if (t1 - t0 <= 0.00001f) 1f else (t - t0) / (t1 - t0)
            return v0 + (v1 - v0) * f
        }
        i++
    }
    return stops.last().second
}

/** Remaps an exit fraction so this layer starts leaving after `delay`. */
internal fun exitDelay(ex: Float, delay: Float): Float =
    ((ex - delay) / (1f - delay).coerceAtLeast(0.0001f)).coerceIn(0f, 1f)

/** One transform layer produced by an entrance/exit keyframe function. */
internal data class CardLayer(
    val alpha: Float = 1f,
    val tx: Float = 0f,
    val ty: Float = 0f,
    val sx: Float = 1f,
    val sy: Float = 1f,
    val rot: Float = 0f,
    val rotX: Float = 0f,
    val rotY: Float = 0f,
    val originX: Float = 0.5f,
    val originY: Float = 0.5f
)

internal fun Modifier.cardLayer(layer: CardLayer): Modifier = graphicsLayer {
    alpha = layer.alpha.coerceIn(0f, 1f)
    translationX = layer.tx.dp.toPx()
    translationY = layer.ty.dp.toPx()
    scaleX = layer.sx
    scaleY = layer.sy
    rotationZ = layer.rot
    rotationX = layer.rotX
    rotationY = layer.rotY
    transformOrigin = TransformOrigin(layer.originX, layer.originY)
}

/** Reveals content left-to-right, matching CSS `clip-path: inset(0 100% 0 0)`. */
internal fun Modifier.cardReveal(progress: Float): Modifier =
    drawWithContent {
        val p = progress.coerceIn(0f, 1f)
        if (p >= 0.999f) {
            drawContent()
        } else {
            clipRect(right = size.width * p) {
                this@drawWithContent.drawContent()
            }
        }
    }

/**
 * The global animation speed. 1.0 is the designed pace, 2.0 twice as fast.
 * Every theme reads this, so the slider in the Motion screen retimes the
 * whole show without touching each theme.
 */
@Composable
internal fun rememberAnimationSpeed(): Float {
    val context = LocalContext.current
    return remember(context) { Prefs.animationSpeed(context) }
}

/** Scales a duration by the animation speed: faster means fewer milliseconds. */
internal fun scaleDuration(durationMs: Int, speed: Float): Int =
    (durationMs / speed.coerceAtLeast(0.05f)).toInt().coerceIn(1, 60000)

/** A phase-shifted 0..1 ambient loop. */
@Composable
internal fun ambientLoop(durationMs: Int, phase: Float = 0f): Float {
    val speed = rememberAnimationSpeed()
    val transition = androidx.compose.animation.core.rememberInfiniteTransition(label = "card-loop")
    val v by transition.animateFloat(
        initialValue = phase,
        targetValue = phase + 1f,
        animationSpec = androidx.compose.animation.core.infiniteRepeatable(
            animation = androidx.compose.animation.core.tween(
                scaleDuration(durationMs, speed),
                easing = LinearEasing
            ),
            repeatMode = androidx.compose.animation.core.RepeatMode.Restart
        ),
        label = "card-loop-value"
    )
    return (v - phase).coerceIn(0f, 1f)
}

/** Ambient loop that ping-pongs (CSS `alternate`). */
@Composable
internal fun ambientAlternate(durationMs: Int): Float {
    val speed = rememberAnimationSpeed()
    val transition = androidx.compose.animation.core.rememberInfiniteTransition(label = "card-alt")
    val v by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = androidx.compose.animation.core.infiniteRepeatable(
            animation = androidx.compose.animation.core.tween(
                scaleDuration(durationMs, speed),
                easing = androidx.compose.animation.core.FastOutSlowInEasing
            ),
            repeatMode = androidx.compose.animation.core.RepeatMode.Reverse
        ),
        label = "card-alt-value"
    )
    return v
}

/**
 * Root of a card theme. Drives the whole card's entrance and exit with
 * the theme's own keyframe functions so every port matches its HTML source.
 */
@Composable
internal fun CardRoot(
    animateIn: Boolean,
    exitFraction: () -> Float,
    enterDurationMs: Int = 1100,
    enter: (Float) -> CardLayer,
    exit: (Float) -> CardLayer,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    val speed = rememberAnimationSpeed()
    val t = remember { Animatable(if (animateIn) 0f else 1f) }
    LaunchedEffect(animateIn) {
        if (!animateIn) {
            t.snapTo(1f)
            return@LaunchedEffect
        }
        t.snapTo(0f)
        t.animateTo(1f, tween(scaleDuration(enterDurationMs, speed), easing = LinearEasing))
    }
    Box(
        modifier
            .fillMaxWidth()
            .cardLayer(
                if (exitFraction() > 0f) exit(exitFraction().coerceIn(0f, 1f))
                else enter(t.value.coerceIn(0f, 1f))
            ),
        content = content
    )
}

/** A separately-staggered 0..1 entrance progress. */
@Composable
internal fun rememberCardEnter(
    animateIn: Boolean,
    durationMs: Int,
    delayMs: Int = 0
): Float {
    val speed = rememberAnimationSpeed()
    val a = remember { Animatable(if (animateIn) 0f else 1f) }
    LaunchedEffect(animateIn) {
        if (!animateIn) {
            a.snapTo(1f)
            return@LaunchedEffect
        }
        a.snapTo(0f)
        launch {
            a.animateTo(
                1f,
                tween(
                    scaleDuration(durationMs, speed),
                    delayMillis = scaleDuration(delayMs, speed),
                    easing = LinearEasing
                )
            )
        }
    }
    return a.value
}

/* ==================================================================== */
/* Frame                                                                */
/* ==================================================================== */

/**
 * Measured body text, reported by [CardBody] so the card can grow by exactly
 * one line per extra wrapped line instead of reserving empty space for the
 * maximum line setting. The desired height always follows the real, wrapped
 * line count.
 */
internal class CardTextMetrics(initialLines: Int) {
    var lineCount by mutableIntStateOf(initialLines.coerceAtLeast(1))
    var lineHeight by mutableStateOf(24.dp)

    fun report(count: Int, height: Dp) {
        val nextCount = count.coerceAtLeast(1)
        if (lineCount != nextCount) lineCount = nextCount
        if (lineHeight != height) lineHeight = height
    }
}

internal val LocalCardTextMetrics = compositionLocalOf<CardTextMetrics?> { null }

/**
 * The card itself. The base height fits one body line; every *actually
 * wrapped* line after that adds only that theme's line height, so a short
 * message never leaves a three-line hole under the text.
 */
@Composable
internal fun CardFrame(
    shape: Shape,
    background: Brush,
    border: Color? = null,
    borderWidth: Dp = 1.dp,
    lines: Int = 2,
    decorationAlpha: Float = 1f,
    /**
     * False when the source notification has no actions: no dock is drawn, so
     * the frame also drops the space that dock would have occupied instead of
     * leaving an empty band at the bottom.
     */
    hasDock: Boolean = true,
    /** Height + bottom padding of the theme's dock, removed when [hasDock] is false. */
    dockSpace: Dp = 42.dp,
    /** Height of a one-line card; compact themes pass a smaller base. */
    baseHeight: Dp = 170.dp,
    modifier: Modifier = Modifier,
    decoration: (androidx.compose.ui.graphics.drawscope.DrawScope.() -> Unit)? = null,
    content: @Composable BoxScope.() -> Unit
) {
    BoxWithConstraints(modifier.fillMaxWidth()) {
        val metrics = remember { CardTextMetrics(lines) }
        val extraLines = (metrics.lineCount - 1).coerceAtLeast(0)
        val base = baseHeight + metrics.lineHeight * extraLines
        val h = if (hasDock) base else (base - dockSpace).coerceAtLeast(104.dp)
        CompositionLocalProvider(LocalCardTextMetrics provides metrics) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(h)
                    .clip(shape)
                    .background(background)
                    .then(
                        if (border != null) Modifier.border(borderWidth, border, shape) else Modifier
                    )
            ) {
                if (decoration != null) {
                    androidx.compose.foundation.Canvas(
                        Modifier.matchParentSize().graphicsLayer {
                            alpha = decorationAlpha.coerceIn(0f, 1f)
                        }
                    ) {
                        with(this) { decoration() }
                    }
                }
                content()
            }
        }
    }
}

/* ==================================================================== */
/* Text                                                                 */
/* ==================================================================== */

internal fun cardShadow(color: Color, dy: Float = 1f, blur: Float = 3f) =
    TextStyle(shadow = Shadow(color = color, offset = Offset(0f, dy), blurRadius = blur))

/** App name + time strip. */
@Composable
internal fun CardMeta(
    data: TickerData,
    modifier: Modifier = Modifier,
    nameColor: Color,
    timeColor: Color,
    font: FontFamily = FontFamily.Default,
    nameSize: Float = 9.5f,
    timeSize: Float = 9f,
    caps: Boolean = true,
    letterSpacing: Float = 1.2f,
    showLogo: Boolean = true,
    showTime: Boolean = true,
    badgeBg: Color = Color(0xFF222222),
    badgeFg: Color = Color.White,
    badgeSize: Dp = 18.dp,
    badgeShape: Shape = CircleShape,
    prefix: (@Composable () -> Unit)? = null
) {
    Row(modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        if (prefix != null) {
            prefix()
            Spacer(Modifier.width(6.dp))
        }
        if (showLogo) {
            Box(
                Modifier.size(badgeSize).clip(badgeShape),
                contentAlignment = Alignment.Center
            ) {
                AppBadge(data, badgeSize, background = badgeBg, letterColor = badgeFg)
            }
            Spacer(Modifier.width(6.dp))
        }
        Text(
            if (caps) data.appName.uppercase() else data.appName,
            color = nameColor,
            fontSize = nameSize.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = font,
            letterSpacing = letterSpacing.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f, fill = false)
        )
        Spacer(Modifier.weight(1f))
        if (showTime) {
            Text(
                if (caps) data.time.uppercase() else data.time,
                color = timeColor,
                fontSize = timeSize.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = font,
                maxLines = 1
            )
        }
    }
}

/** Sender (notification title) + optional time row, shared by the cards. */
@Composable
internal fun CardSenderRow(
    title: String,
    time: String,
    titleColor: Color,
    timeColor: Color,
    titleFont: FontFamily,
    timeFont: FontFamily,
    titleSize: Float,
    modifier: Modifier = Modifier,
    timeSize: Float = 9f,
    titleWeight: FontWeight = FontWeight.SemiBold,
    titleLetter: Float = 0.3f,
    timeLetter: Float = 0.5f,
    uppercaseTitle: Boolean = false,
    timePrefix: String = "",
    timeSuffix: String = "",
    titleStyle: TextStyle = TextStyle.Default,
    timeStyle: TextStyle = TextStyle.Default,
    showTime: Boolean = true,
    showLogo: Boolean = true,
    logoSize: Dp = 22.dp,
    logoData: TickerData? = null
) {
    Row(modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        if (showLogo && logoData != null) {
            Box(Modifier.size(logoSize), contentAlignment = Alignment.Center) {
                AppBadge(logoData, logoSize, background = Color(0xFF222222), letterColor = Color.White)
            }
            Spacer(Modifier.width(7.dp))
        }
        Text(
            if (uppercaseTitle) title.uppercase() else title,
            color = titleColor,
            fontSize = titleSize.sp,
            fontWeight = titleWeight,
            fontFamily = titleFont,
            letterSpacing = titleLetter.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            style = titleStyle,
            modifier = Modifier.weight(1f, fill = false)
        )
        if (showTime) {
            Spacer(Modifier.weight(1f))
            Spacer(Modifier.width(8.dp))
            Text(
                "$timePrefix${time.uppercase()}$timeSuffix",
                color = timeColor,
                fontSize = timeSize.sp,
                fontWeight = FontWeight.ExtraBold,
                fontFamily = timeFont,
                letterSpacing = timeLetter.sp,
                maxLines = 1,
                style = timeStyle
            )
        }
    }
}

/** The body text size the font-size preference is measured against. */
internal const val FontSizeDefaultSp = 13f

/** Message body: fixed line count, or a one-line marquee when `lines == 0`. */
@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun CardBody(
    text: String,
    lines: Int,
    color: Color,
    font: FontFamily,
    modifier: Modifier = Modifier,
    weight: FontWeight = FontWeight.Normal,
    sizeSp: Float? = null,
    lineHeightFactor: Float = 1.3f,
    letterSpacing: Float = 0f,
    style: TextStyle = TextStyle.Default
) {
    val context = LocalContext.current
    // The theme's own size is its baseline; the user's font-size preference
    // scales it, so the slider reaches every theme.
    val bodySp = if (sizeSp != null) {
        (sizeSp * (Prefs.fontSize(context) / FontSizeDefaultSp)).coerceIn(8f, 24f)
    } else {
        Prefs.fontSize(context)
    }
    val speed = Prefs.speed(context)
    val metrics = LocalCardTextMetrics.current
    val lineHeightDp = with(LocalDensity.current) {
        (bodySp * lineHeightFactor).sp.toDp()
    }
    if (lines <= 0) {
        Text(
            text,
            color = color,
            fontSize = bodySp.sp,
            lineHeight = (bodySp * lineHeightFactor).sp,
            fontWeight = weight,
            fontFamily = font,
            letterSpacing = letterSpacing.sp,
            maxLines = 1,
            softWrap = false,
            overflow = TextOverflow.Clip,
            style = style,
            onTextLayout = { metrics?.report(1, lineHeightDp) },
            modifier = modifier
                .fillMaxWidth()
                .basicMarquee(
                    iterations = Int.MAX_VALUE,
                    velocity = (30f * speed).dp
                )
        )
    } else {
        Text(
            text,
            color = color,
            fontSize = bodySp.sp,
            lineHeight = (bodySp * lineHeightFactor).sp,
            fontWeight = weight,
            fontFamily = font,
            letterSpacing = letterSpacing.sp,
            minLines = 1,
            maxLines = lines,
            overflow = TextOverflow.Ellipsis,
            style = style,
            onTextLayout = { result -> metrics?.report(result.lineCount, lineHeightDp) },
            modifier = modifier
        )
    }
}

/* ==================================================================== */
/* Dock                                                                 */
/* ==================================================================== */

/**
 * Action buttons mirroring the source notification (max three). A
 * notification with no actions gets an empty dock, exactly like Android.
 */
@Composable
internal fun CardDock(
    data: TickerData,
    interactions: TickerInteractions?,
    modifier: Modifier = Modifier,
    background: Color,
    foreground: Color,
    border: Color? = null,
    borderWidth: Dp = 1.dp,
    shape: Shape = RoundedCornerShape(50),
    font: FontFamily = FontFamily.Default,
    fontSize: Float = 10f,
    letterSpacing: Float = 0.4f,
    uppercase: Boolean = false,
    bold: Boolean = true,
    showIcons: Boolean = false,
    iconSize: Dp = 13.dp,
    gap: Dp = 6.dp,
    height: Dp? = null,
    horizontalPadding: Dp = 8.dp,
    verticalPadding: Dp = 6.dp,
    buttonScale: Float = 1f,
    textStyle: TextStyle = TextStyle.Default,
    backgroundFor: (shell: Boolean) -> Brush = { Brush.linearGradient(listOf(background, background)) },
    foregroundFor: (shell: Boolean) -> Color = { foreground },
    borderFor: (shell: Boolean) -> Color? = { border },
    shapeFor: (shell: Boolean) -> Shape = { shape }
) {
    val specs = notificationActionSpecs(data, interactions)
    if (specs.isEmpty()) return
    Row(
        modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(gap),
        verticalAlignment = Alignment.CenterVertically
    ) {
        specs.forEach { spec ->
            val source = remember { MutableInteractionSource() }
            val pressed by source.collectIsPressedAsState()
            val fg = foregroundFor(spec.shell)
            val bd = borderFor(spec.shell)
            val sh = shapeFor(spec.shell)
            Box(
                Modifier
                    .weight(1f)
                    .scale(if (pressed) 0.96f else 1f)
                    .clip(sh)
                    .background(backgroundFor(spec.shell))
                    .then(
                        if (bd != null) Modifier.border(borderWidth, bd, sh) else Modifier
                    )
                    .clickable(
                        interactionSource = source,
                        indication = null,
                        enabled = spec.onClick != null
                    ) { spec.onClick?.invoke() }
                    .then(if (height != null) Modifier.height(height) else Modifier)
                    .padding(horizontal = horizontalPadding, vertical = verticalPadding),
                contentAlignment = Alignment.Center
            ) {
                if (spec.iconOnly) {
                    androidx.compose.material3.Icon(
                        spec.icon,
                        contentDescription = spec.label.trim(),
                        tint = fg,
                        modifier = Modifier.size((fontSize + 8f).dp * buttonScale)
                    )
                } else {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                    if (showIcons) {
                        androidx.compose.material3.Icon(
                            spec.icon,
                            contentDescription = null,
                            tint = fg,
                            modifier = Modifier
                                .size(iconSize * buttonScale)
                                .padding(end = 3.dp)
                        )
                    }
                    Text(
                        if (uppercase) spec.label.uppercase() else spec.label,
                        color = fg,
                        fontSize = (fontSize * buttonScale).sp,
                        fontWeight = if (bold) FontWeight.Bold else FontWeight.Normal,
                        fontFamily = font,
                        letterSpacing = letterSpacing.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                            style = textStyle
                        )
                    }
                }
            }
        }
    }
}

/* ==================================================================== */
/* Small shared pieces                                                  */
/* ==================================================================== */

/** A soft dot of light — used by several themes for lamps, bokeh or dust. */
@Composable
internal fun CardGlowDot(
    color: Color,
    size: Dp,
    modifier: Modifier = Modifier,
    blurAlpha: Float = 0.35f
) {
    Box(
        modifier
            .size(size)
            .clip(CircleShape)
            .background(
                Brush.radialGradient(
                    listOf(color.copy(alpha = blurAlpha), color, color.copy(alpha = blurAlpha * 0.2f))
                )
            )
    )
}

/** App icon rendered as a bitmap with a rounded fallback letter. */
@Composable
internal fun CardIcon(
    data: TickerData,
    size: Dp,
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(6.dp),
    fallbackBg: Color = Color(0xFF222222),
    fallbackFg: Color = Color.White
) {
    val bitmap = data.icon
    if (bitmap != null) {
        Image(
            bitmap = bitmap.asImageBitmap(),
            contentDescription = data.appName,
            contentScale = ContentScale.Crop,
            modifier = modifier.size(size).clip(shape)
        )
    } else {
        Box(
            modifier.size(size).clip(shape).background(fallbackBg),
            contentAlignment = Alignment.Center
        ) {
            Text(
                data.appName.take(1).uppercase(),
                color = fallbackFg,
                fontSize = (size.value * 0.42f).sp,
                fontWeight = FontWeight.Black
            )
        }
    }
}

/** Column helper for the standard "meta / title / body" block. */
@Composable
internal fun CardTextColumn(
    modifier: Modifier = Modifier,
    meta: (@Composable () -> Unit)? = null,
    title: @Composable () -> Unit,
    body: @Composable () -> Unit
) {
    Column(modifier) {
        if (meta != null) {
            meta()
            Spacer(Modifier.height(4.dp))
        }
        title()
        Spacer(Modifier.height(3.dp))
        body()
    }
}
