package com.openticker.ticker.cards

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
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
/* 22 — Bamboo                                                         */
/* ==================================================================== */
/*
 * A grove of thin bamboo canes. On the way in each rod falls from above
 * and plants itself on a transparent backdrop, then the green field fills
 * in behind them, the sender row (woody app icon + name + clock) rises and
 * three leaf buttons with their icons sprout last. On the way out the field
 * fades away first, leaving the bare rods, which are then hoisted back up.
 * The card hugs the message: a one-liner is short, more wrapped lines make
 * it taller (CardFrame reports the real line count).
 */

/** The whole appear choreography, in milliseconds. */
private const val BambooEnterMs = 2300f

/** The way out. */
private const val BambooExitMs = 1250f

private val BambooCaneColors = listOf(
    Color(0xFFBFD464),
    Color(0xFFB3C84D),
    Color(0xFFB9CE5A),
    Color(0xFFADC247)
)

private val BambooCaneTilts = floatArrayOf(
    -1.6f, 1.2f, -1f, 1.6f, -1.4f, 1f, -1.2f,
    1.5f, -1f, 1.3f, -1.5f, 1.1f, -1.3f, 1.4f
)

private val BambooCaneNodes = floatArrayOf(
    0.18f, 0.64f, 0.38f, 0.74f, 0.26f, 0.52f, 0.82f,
    0.44f, 0.20f, 0.68f, 0.34f, 0.58f, 0.76f, 0.30f
)

private val BambooEaseCaneFall = CubicBezierEasing(0.3f, 0.55f, 0.3f, 1.08f)
private val BambooEaseCaneRise = CubicBezierEasing(0.55f, 0f, 0.75f, 0.45f)
private val BambooEaseText = CubicBezierEasing(0.25f, 1.3f, 0.4f, 1f)
private val BambooEaseLeaf = CubicBezierEasing(0.3f, 1.55f, 0.5f, 1f)
private val BambooEaseIn = CubicBezierEasing(0.42f, 0f, 1f, 1f)
private val BambooEaseField = CubicBezierEasing(0.25f, 0.1f, 0.25f, 1f)

private val BambooInk = Color(0xFFFFFFFF)
private val BambooInkSoft = Color(0xFFF4F9E3)
private val BambooLeafInk = Color(0xFF2F5D36)
private val BambooLeafBorder = Color(0x662F5D36)
private val BambooFieldBorder = Color(0x996E8428)
private val BambooCaneSeam = Color(0xB261781E)
private val BambooCaneNode = Color(0x5261781E)
private val BambooTextShadow = TextStyle(
    // A deep black halo keeps the white type readable on the bright grove.
    shadow = Shadow(Color(0xE6000000), Offset(0f, 1.5f), 2.5f)
)

/** Progress inside one part of the timeline. */
private fun bambooStage(ms: Float, delay: Float, duration: Float): Float =
    ((ms - delay) / duration).coerceIn(0f, 1f)

/* ------------------------------------------------------------------ */
/* The grove: the field and the fourteen canes                         */
/* ------------------------------------------------------------------ */

private fun DrawScope.drawBambooCane(
    left: Float,
    top: Float,
    width: Float,
    height: Float,
    colour: Color,
    node: Float
) {
    val rectTopLeft = Offset(left, top)
    val rectSize = Size(width, height)
    drawRect(color = colour, topLeft = rectTopLeft, size = rectSize)
    drawRect(
        brush = Brush.verticalGradient(
            colorStops = arrayOf(
                0f to Color(0x21FFFFFF),
                0.42f to Color.Transparent,
                1f to Color(0x29384A0C)
            )
        ),
        topLeft = rectTopLeft,
        size = rectSize
    )
    drawRect(
        color = BambooCaneSeam,
        topLeft = Offset(left + width - 3.dp.toPx(), top),
        size = Size(3.dp.toPx(), height)
    )
    drawRect(
        color = BambooCaneNode,
        topLeft = Offset(left, top + node * height),
        size = Size(width, 3.dp.toPx())
    )
}

private fun DrawScope.drawBambooGrove(
    inMs: Float,
    exitMs: Float,
    leaving: Boolean
) {
    val caneWidth = size.width / 14f
    val caneTop = -8.dp.toPx()
    val caneHeight = size.height + 16.dp.toPx()

    for (index in 0 until 14) {
        val tilt = BambooCaneTilts[index]
        val pIn = BambooEaseCaneFall.transform(
            bambooStage(inMs, index * 35f, 550f)
        )
        val pOut = BambooEaseCaneRise.transform(
            bambooStage(exitMs, 350f + index * 30f, 500f)
        )

        val ty = if (leaving) {
            -1.85f * pOut
        } else {
            keyframe(pIn, 0f to -1.85f, 0.62f to 0f, 0.80f to -0.04f, 1f to 0f)
        }
        val rotation = if (leaving) {
            tilt * pOut
        } else {
            keyframe(pIn, 0f to tilt, 0.62f to 0f, 1f to 0f)
        }

        val left = index * caneWidth
        val shift = ty * caneHeight
        val centre = Offset(left + caneWidth / 2f, caneTop + caneHeight / 2f + shift)
        rotate(rotation, centre) {
            drawBambooCane(
                left = left,
                top = caneTop + shift,
                width = caneWidth + 0.5f,
                height = caneHeight,
                colour = BambooCaneColors[index % BambooCaneColors.size],
                node = BambooCaneNodes[index]
            )
        }
    }
}

/* ------------------------------------------------------------------ */
/* The sender row: app icon + name + clock                             */
/* ------------------------------------------------------------------ */

@Composable
private fun BambooSenderRow(
    data: TickerData,
    showTime: Boolean,
    showLogo: Boolean,
    logoSize: Dp,
    timeColour: Color
) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        if (showLogo) {
            AppBadge(
                data = data,
                size = logoSize,
                background = Color(0xFFB2C736),
                letterColor = BambooLeafInk
            )
            Spacer(Modifier.width(9.dp))
        }
        Text(
            data.title,
            color = BambooInk,
            fontSize = 18.sp,
            fontFamily = ThemeFonts.Baloo,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 0.3.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            style = BambooTextShadow,
            modifier = Modifier.weight(1f, fill = false)
        )
        if (showTime) {
            Spacer(Modifier.weight(1f))
            Spacer(Modifier.width(8.dp))
            Text(
                data.time.uppercase(),
                color = timeColour,
                fontSize = 12.5.sp,
                fontFamily = ThemeFonts.QuicksandBold,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 0.5.sp,
                maxLines = 1,
                style = BambooTextShadow
            )
        }
    }
}

/* ------------------------------------------------------------------ */
/* The leaf buttons with their little outline icons                    */
/* ------------------------------------------------------------------ */

private enum class BambooActionKind { Reply, Read, Mute, Other }

private fun bambooActionKind(label: String): BambooActionKind {
    val lower = label.lowercase()
    return when {
        lower.contains("reply") || lower.contains("رد") -> BambooActionKind.Reply
        lower.contains("read") || lower.contains("mark") ||
            lower.contains("مقروء") || lower.contains("قراءة") -> BambooActionKind.Read
        lower.contains("mute") || lower.contains("silent") ||
            lower.contains("كتم") || lower.contains("صامت") -> BambooActionKind.Mute
        else -> BambooActionKind.Other
    }
}

private fun bambooLeafColour(kind: BambooActionKind): Color = when (kind) {
    BambooActionKind.Reply -> Color(0xFFABD9A6)
    BambooActionKind.Read -> Color(0xFF9FD09B)
    BambooActionKind.Mute -> Color(0xFFB4E0AC)
    BambooActionKind.Other -> Color(0xFFA9D8A4)
}

private fun DrawScope.drawBambooActionIcon(kind: BambooActionKind, tint: Color) {
    val s = size.minDimension / 24f
    val stroke = Stroke(
        width = 2.4f * s,
        cap = StrokeCap.Round,
        join = StrokeJoin.Round
    )

    fun outline(block: Path.() -> Unit) {
        drawPath(Path().apply(block), color = tint, style = stroke)
    }

    when (kind) {
        BambooActionKind.Reply -> {
            outline {
                moveTo(9f * s, 14f * s)
                lineTo(4f * s, 9f * s)
                lineTo(9f * s, 4f * s)
            }
            outline {
                moveTo(4f * s, 9f * s)
                lineTo(13.5f * s, 9f * s)
                cubicTo(
                    17.1f * s, 9f * s,
                    20f * s, 11.9f * s,
                    20f * s, 15.5f * s
                )
                lineTo(20f * s, 19f * s)
            }
        }
        BambooActionKind.Read -> {
            drawRoundRect(
                color = tint,
                topLeft = Offset(3f * s, 5.5f * s),
                size = Size(18f * s, 13f * s),
                cornerRadius = CornerRadius(2.5f * s, 2.5f * s),
                style = stroke
            )
            outline {
                moveTo(4f * s, 7.5f * s)
                lineTo(12f * s, 13f * s)
                lineTo(20f * s, 7.5f * s)
            }
        }
        BambooActionKind.Mute -> {
            outline {
                moveTo(18f * s, 9f * s)
                cubicTo(
                    18f * s, 5.7f * s,
                    15.3f * s, 3f * s,
                    12f * s, 3f * s
                )
                cubicTo(
                    8.7f * s, 3f * s,
                    6f * s, 5.7f * s,
                    6f * s, 9f * s
                )
                lineTo(6f * s, 10f * s)
                cubicTo(
                    6f * s, 13f * s,
                    4f * s, 15f * s,
                    4f * s, 15f * s
                )
                lineTo(20f * s, 15f * s)
                cubicTo(
                    20f * s, 15f * s,
                    18f * s, 13f * s,
                    18f * s, 10f * s
                )
                close()
            }
            outline {
                moveTo(10f * s, 19.5f * s)
                cubicTo(
                    10f * s, 20.7f * s,
                    10.9f * s, 21.7f * s,
                    12f * s, 21.7f * s
                )
                cubicTo(
                    13.1f * s, 21.7f * s,
                    14f * s, 20.7f * s,
                    14f * s, 19.5f * s
                )
            }
            outline {
                moveTo(4f * s, 4f * s)
                lineTo(20f * s, 20f * s)
            }
        }
        BambooActionKind.Other -> Unit
    }
}

@Composable
private fun BambooActionIcon(
    kind: BambooActionKind,
    fallback: ImageVector,
    tint: Color,
    modifier: Modifier = Modifier
) {
    if (kind == BambooActionKind.Other) {
        Icon(fallback, contentDescription = null, tint = tint, modifier = modifier)
    } else {
        Canvas(modifier) { drawBambooActionIcon(kind, tint) }
    }
}

@Composable
private fun BambooDock(
    data: TickerData,
    interactions: TickerInteractions?,
    scale: Float,
    inMs: Float,
    exitMs: Float,
    leaving: Boolean,
    modifier: Modifier = Modifier
) {
    val specs = notificationActionSpecs(data, interactions)
    if (specs.isEmpty()) return

    val shape = RoundedCornerShape((15f * scale).dp)
    val fontSp = (10.5f * scale).coerceIn(7.5f, 16f)
    val iconSize = 13.dp * scale
    val gap = 6.dp * scale
    val padH = 11.dp * scale
    val padV = 9.dp * scale

    Row(
        modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically
    ) {
        specs.forEachIndexed { index, spec ->
            val source = remember { MutableInteractionSource() }
            val pressed by source.collectIsPressedAsState()
            val kind = bambooActionKind(spec.label)

            val pIn = BambooEaseLeaf.transform(
                bambooStage(inMs, 1550f + index * 120f, 400f)
            )
            val pOut = BambooEaseIn.transform(
                bambooStage(exitMs, index * 60f, 220f)
            )

            val buttonAlpha = if (leaving) {
                (1f - pOut).coerceIn(0f, 1f)
            } else {
                keyframe(pIn, 0f to 0f, 0.70f to 1f, 1f to 1f).coerceIn(0f, 1f)
            }
            val s = if (leaving) {
                1f - 0.8f * pOut
            } else {
                keyframe(pIn, 0f to 0.3f, 0.70f to 1.09f, 1f to 1f)
            }
            val rotation = if (leaving) {
                -10f * pOut
            } else {
                keyframe(pIn, 0f to -7f, 0.70f to 2f, 1f to 0f)
            }

            Box(
                Modifier
                    .weight(1f, fill = false)
                    .graphicsLayer {
                        alpha = buttonAlpha
                        scaleX = s * if (pressed) 0.97f else 1f
                        scaleY = s * if (pressed) 0.97f else 1f
                        rotationZ = rotation
                        translationY = if (pressed) 1.dp.toPx() else 0f
                    }
                    .shadow(
                        elevation = 5.dp * scale,
                        shape = shape,
                        clip = false,
                        ambientColor = Color(0x40345828),
                        spotColor = Color(0x80345828)
                    )
                    .clip(shape)
                    .background(bambooLeafColour(kind))
                    .border(1.5.dp, BambooLeafBorder, shape)
                    .clickable(
                        interactionSource = source,
                        indication = null,
                        enabled = spec.onClick != null
                    ) { spec.onClick?.invoke() }
                    .padding(horizontal = padH, vertical = padV),
                contentAlignment = Alignment.Center
            ) {
                if (spec.iconOnly) {
                    BambooActionIcon(
                        kind = kind,
                        fallback = spec.icon,
                        tint = BambooLeafInk,
                        modifier = Modifier.size(iconSize + 2.dp)
                    )
                } else {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        BambooActionIcon(
                            kind = kind,
                            fallback = spec.icon,
                            tint = BambooLeafInk,
                            modifier = Modifier.size(iconSize)
                        )
                        Spacer(Modifier.width(gap))
                        Text(
                            spec.label.uppercase(),
                            color = BambooLeafInk,
                            fontSize = fontSp.sp,
                            fontFamily = ThemeFonts.QuicksandBold,
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 0.7.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}

/* ------------------------------------------------------------------ */
/* The card                                                            */
/* ------------------------------------------------------------------ */

@Composable
fun CuratedBambooTicker(
    data: TickerData,
    interactions: TickerInteractions?,
    animateIn: Boolean,
    exitFraction: () -> Float
) {
    val context = LocalContext.current
    val opts = Prefs.themeOptions(context, "bamboo", 0xFFDFF3B2.toInt())
    val accent = Color(opts.accentArgb)
    val lines = opts.lines
    val showTime = opts.showTime
    val showLogo = opts.showLogo
    val logoSize = opts.avatarSize.dp
    val scale = opts.buttonScale / 100f
    val speed = rememberAnimationSpeed()

    val enter = remember { Animatable(if (animateIn) 0f else 1f) }
    LaunchedEffect(animateIn) {
        if (!animateIn) {
            enter.snapTo(1f)
            return@LaunchedEffect
        }
        enter.snapTo(0f)
        enter.animateTo(
            1f,
            tween(scaleDuration(BambooEnterMs.toInt(), speed), easing = LinearEasing)
        )
    }

    val inMs = enter.value.coerceIn(0f, 1f) * BambooEnterMs
    val ex = exitFraction().coerceIn(0f, 1f)
    val leaving = ex > 0f
    val exitMs = ex * BambooExitMs

    // One message line plus the dock, measured with the live font-size
    // preference, so the card hugs the text at every text size. Extra
    // wrapped lines are added by CardFrame using the reported line height.
    val bodySp = (11.5f * (Prefs.fontSize(context) / FontSizeDefaultSp))
        .coerceIn(8f, 24f)
    val bodyLine = with(LocalDensity.current) {
        (bodySp * 1.3043f).sp.toDp()
    }
    val buttonHeight = 33.dp * scale
    val baseHeight = 76.dp + bodyLine + buttonHeight
    val dockSpace = 12.dp + buttonHeight

    val fieldP = if (leaving) {
        1f - BambooEaseField.transform(bambooStage(exitMs, 120f, 300f))
    } else {
        BambooEaseField.transform(bambooStage(inMs, 850f, 400f))
    }.coerceIn(0f, 1f)

    val textP = if (leaving) {
        BambooEaseIn.transform(bambooStage(exitMs, 50f, 200f))
    } else {
        BambooEaseText.transform(bambooStage(inMs, 1300f, 450f))
    }
    val textAlpha = if (leaving) (1f - textP).coerceIn(0f, 1f) else textP.coerceIn(0f, 1f)
    val textTy = if (leaving) 10f * textP else 14f * (1f - textP)
    val textScale = if (leaving) 1f - 0.04f * textP else 0.96f + 0.04f * textP

    val cardShape = RoundedCornerShape(22.dp)

    CardFrame(
        modifier = Modifier.shadow(
            elevation = (18f * fieldP).dp,
            shape = cardShape,
            clip = false,
            ambientColor = Color(0x80465F14),
            spotColor = Color(0xCC465F14)
        ),
        lines = lines,
        hasDock = data.actions.isNotEmpty(),
        baseHeight = baseHeight,
        dockSpace = dockSpace,
        decorationAlpha = 1f,
        shape = cardShape,
        background = Brush.verticalGradient(listOf(Color.Transparent, Color.Transparent))
    ) {
        Canvas(Modifier.fillMaxSize()) {
            // the green grove field, which fills in behind the planted rods
            drawRect(
                brush = Brush.verticalGradient(
                    colorStops = arrayOf(
                        0f to Color(0xFFC7DA6B),
                        0.58f to Color(0xFFB5CA4F),
                        1f to Color(0xFFA3B941)
                    )
                ),
                alpha = fieldP
            )

            // the thin rim around the field (drawn inside the clipped card)
            val inset = 0.75.dp.toPx()
            drawRoundRect(
                color = BambooFieldBorder,
                topLeft = Offset(inset, inset),
                size = Size(size.width - inset * 2f, size.height - inset * 2f),
                cornerRadius = CornerRadius(22.dp.toPx() - inset, 22.dp.toPx() - inset),
                style = Stroke(1.5.dp.toPx()),
                alpha = fieldP
            )

            // the rods
            drawBambooGrove(inMs = inMs, exitMs = exitMs, leaving = leaving)
        }

        Column(
            Modifier
                .align(Alignment.TopStart)
                .padding(start = 20.dp, end = 20.dp, top = 16.dp)
                .fillMaxWidth()
                .graphicsLayer {
                    alpha = textAlpha
                    translationY = textTy.dp.toPx()
                    scaleX = textScale
                    scaleY = textScale
                }
        ) {
            BambooSenderRow(
                data = data,
                showTime = showTime,
                showLogo = showLogo,
                logoSize = logoSize,
                timeColour = accent
            )
            Spacer(Modifier.height(5.dp))
            CardBody(
                text = data.body,
                lines = lines,
                color = BambooInkSoft,
                font = ThemeFonts.QuicksandBold,
                weight = FontWeight.Bold,
                sizeSp = 11.5f,
                lineHeightFactor = 1.3043f,
                style = BambooTextShadow
            )
        }

        BambooDock(
            data = data,
            interactions = interactions,
            scale = scale,
            inMs = inMs,
            exitMs = exitMs,
            leaving = leaving,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 12.dp)
        )
    }
}
