package com.openticker.ticker.cards

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.openticker.data.Prefs
import com.openticker.data.TickerData
import com.openticker.data.TickerInteractions
import com.openticker.ticker.styles.notificationActionSpecs
import kotlin.math.min

/* ==================================================================== */
/* 07 — Blueprint                                                       */
/* ==================================================================== */

private val BlueprintInk = Color(0xFFF2FAFF)
private val BlueprintSoft = Color(0xD1EAF6FF)
private val BlueprintAccent = Color(0xFF8FD8FF)
private val BlueprintClock = Color(0xFFB6E9FF)
private val BlueprintLine = Color(0x52B4DCFF)

@Composable
fun CuratedBlueprintTicker(
    data: TickerData,
    interactions: TickerInteractions?,
    animateIn: Boolean,
    exitFraction: () -> Float
) {
    val context = LocalContext.current
    val opts = Prefs.themeOptions(context, "blueprint", 0xFF9FD0FF.toInt())
    val accent = if (opts.accentArgb == 0xFF9FD0FF.toInt()) BlueprintAccent else Color(opts.accentArgb)
    val lines = opts.lines
    val intensity = opts.intensity
    val showTime = opts.showTime
    val showLogo = opts.showLogo
    val logoSize = opts.avatarSize.dp
    val scale = opts.buttonScale / 100f
    val effect = opts.effect
    val hasDock = data.actions.isNotEmpty()

    BlueprintSheet(animateIn = animateIn, exitFraction = exitFraction) {
        CardFrame(
            lines = lines,
            hasDock = hasDock,
            // One body line plus the dock; each extra wrapped line adds 18dp.
            baseHeight = 176.dp,
            dockSpace = 44.dp,
            decorationAlpha = intensity / 100f,
            shape = RoundedCornerShape(4.dp),
            background = Brush.linearGradient(
                listOf(Color(0xFF0C3E70), Color(0xFF082F57), Color(0xFF062444)),
                start = Offset.Zero,
                end = Offset(720f, 1040f)
            ),
            border = BlueprintLine,
            borderWidth = 1.dp,
            decoration = { drawBlueprintPaper(effect) }
        ) {
            BoxWithConstraints(Modifier.fillMaxSize()) {
                val cardHeight = maxHeight
                // Keep the detail viewport above the title block on every height.
                val detailHeight = (cardHeight - 26.dp - 12.dp - 34.dp - 10.dp)
                    .coerceIn(78.dp, 118.dp)
                val titleTop = cardHeight - 46.dp
                val showTitleBlock = titleTop >= 26.dp + detailHeight + 6.dp

                BlueprintBinding()
                BlueprintRuler()
                BlueprintDetail(
                    data = data,
                    showLogo = showLogo,
                    logoSize = logoSize,
                    height = detailHeight,
                    accent = accent
                )
                if (showTitleBlock) BlueprintTitleBlock()

                Column(
                    Modifier
                        .align(Alignment.TopStart)
                        .padding(start = 128.dp, end = 14.dp, top = 26.dp)
                        .fillMaxWidth()
                ) {
                    CardSenderRow(
                        title = data.title,
                        time = data.time,
                        titleColor = BlueprintInk,
                        timeColor = BlueprintClock,
                        titleFont = ThemeFonts.Bebas,
                        timeFont = ThemeFonts.SpaceMonoBold,
                        titleSize = 24f,
                        timeSize = 10f,
                        titleWeight = FontWeight.Normal,
                        titleLetter = 1.6f,
                        timeLetter = 1f,
                        uppercaseTitle = true,
                        showTime = showTime,
                        showLogo = false,
                        logoSize = logoSize,
                        logoData = data
                    )
                    Spacer(Modifier.height(9.dp))
                    CardBody(
                        text = data.body,
                        lines = lines,
                        color = BlueprintSoft,
                        font = ThemeFonts.SpaceMono,
                        weight = FontWeight.Normal,
                        sizeSp = 13f,
                        lineHeightFactor = 18f / 13f
                    )
                }

                if (hasDock) {
                    BlueprintDock(
                        data = data,
                        interactions = interactions,
                        scale = scale,
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(start = 128.dp, end = 14.dp, bottom = 12.dp)
                    )
                }
            }
        }
    }
}

/* ------------------------------------------------------------------ */
/* Sheet unroll / roll animation                                       */
/* ------------------------------------------------------------------ */

@Composable
private fun BlueprintSheet(
    animateIn: Boolean,
    exitFraction: () -> Float,
    content: @Composable BoxScope.() -> Unit
) {
    val enter = rememberCardEnter(animateIn, 850)
    val ex = exitFraction().coerceIn(0f, 1f)
    val reveal = if (ex > 0f) 1f - ex else enter
    val barAlpha = if (ex > 0f) {
        keyframe(ex, 0f to 0f, 0.10f to 1f, 0.88f to 1f, 1f to 0f)
    } else {
        keyframe(enter, 0f to 0f, 0.08f to 1f, 0.90f to 1f, 1f to 0f)
    }

    Box(Modifier.fillMaxWidth()) {
        Box(
            Modifier
                .fillMaxWidth()
                .drawWithContent {
                    val left = size.width * (1f - reveal)
                    if (left <= 0.5f) {
                        drawContent()
                    } else {
                        clipRect(left = left, right = size.width) {
                            this@drawWithContent.drawContent()
                        }
                    }
                }
        ) {
            content()
        }

        // The rolled edge that leads the sheet across the card.
        Canvas(Modifier.matchParentSize()) {
            if (barAlpha <= 0.001f) return@Canvas
            val left = size.width * (1f - reveal)
            val barWidth = 20.dp.toPx()
            val cx = left.coerceIn(barWidth / 2f, size.width - barWidth / 2f)
            val topLeft = Offset(cx - barWidth / 2f, 0f)
            val barSize = Size(barWidth, size.height)
            val radius = CornerRadius(barWidth / 2f, barWidth / 2f)

            drawRoundRect(
                color = Color(0x2E8FD8FF),
                topLeft = Offset(topLeft.x - 3.dp.toPx(), 0f),
                size = Size(barWidth + 6.dp.toPx(), size.height),
                cornerRadius = CornerRadius(radius.x + 3.dp.toPx(), radius.y + 3.dp.toPx()),
                alpha = barAlpha * 0.6f
            )
            drawRoundRect(
                brush = Brush.horizontalGradient(
                    listOf(
                        Color(0xFF06243F),
                        Color(0xFF0B3A61),
                        Color(0xFF17527F),
                        Color(0xFF0B3A61),
                        Color(0xFF06243F)
                    )
                ),
                topLeft = topLeft,
                size = barSize,
                cornerRadius = radius,
                alpha = barAlpha
            )
            drawRoundRect(
                color = Color(0x80B4DCFF),
                topLeft = topLeft,
                size = barSize,
                cornerRadius = radius,
                style = Stroke(1.dp.toPx()),
                alpha = barAlpha
            )
            drawLine(
                color = Color(0x99D2F0FF),
                start = Offset(cx - barWidth / 2f + 5.dp.toPx(), 7.dp.toPx()),
                end = Offset(cx - barWidth / 2f + 5.dp.toPx(), size.height - 7.dp.toPx()),
                strokeWidth = 2.dp.toPx(),
                cap = StrokeCap.Round,
                alpha = barAlpha
            )
        }
    }
}

/* ------------------------------------------------------------------ */
/* Paper, binding strip, ruler                                         */
/* ------------------------------------------------------------------ */

private fun DrawScope.drawBlueprintPaper(effect: Boolean) {
    if (effect) {
        drawRect(
            Brush.radialGradient(
                colors = listOf(Color(0x1F7DD3FF), Color.Transparent),
                center = Offset(size.width * 0.84f, -size.height * 0.10f),
                radius = size.width * 1.15f
            )
        )
    }

    val hairline = 1.dp.toPx()
    val minor = Color(0x0EB4DCFF)
    var x = 0f
    while (x < size.width) {
        drawLine(minor, Offset(x, 0f), Offset(x, size.height), hairline)
        x += 16.dp.toPx()
    }
    var y = 0f
    while (y < size.height) {
        drawLine(minor, Offset(0f, y), Offset(size.width, y), hairline)
        y += 16.dp.toPx()
    }

    val major = Color(0x1CB4DCFF)
    x = 0f
    while (x < size.width) {
        drawLine(major, Offset(x, 0f), Offset(x, size.height), hairline)
        x += 80.dp.toPx()
    }
    y = 0f
    while (y < size.height) {
        drawLine(major, Offset(0f, y), Offset(size.width, y), hairline)
        y += 80.dp.toPx()
    }
}

@Composable
private fun BoxScope.BlueprintBinding() {
    Canvas(
        Modifier
            .align(Alignment.TopStart)
            .fillMaxHeight()
            .width(16.dp)
    ) {
        drawRect(Color(0x8C031223))
        drawLine(
            color = Color(0x47B4DCFF),
            start = Offset(size.width - 0.5.dp.toPx(), 0f),
            end = Offset(size.width - 0.5.dp.toPx(), size.height),
            strokeWidth = 1.dp.toPx()
        )

        val holeRadius = 3.dp.toPx()
        listOf(0.15f, 0.48f, 0.82f).forEach { fraction ->
            val center = Offset(size.width / 2f, size.height * fraction)
            drawCircle(Color(0xFF061C33), holeRadius, center)
            drawCircle(
                Color(0x66000000),
                holeRadius * 0.7f,
                Offset(center.x, center.y - holeRadius * 0.25f)
            )
            drawCircle(
                Color(0x66B4DCFF),
                holeRadius,
                center,
                style = Stroke(1.dp.toPx())
            )
        }
    }
}

@Composable
private fun BoxScope.BlueprintRuler() {
    Box(
        Modifier
            .align(Alignment.TopStart)
            .fillMaxWidth()
            .padding(start = 16.dp)
            .height(18.dp)
    ) {
        Canvas(Modifier.fillMaxSize()) {
            drawRect(Color(0x80031223))
            var x = 0f
            while (x < size.width) {
                drawLine(
                    color = Color(0x47B4DCFF),
                    start = Offset(x, size.height - 4.dp.toPx()),
                    end = Offset(x, size.height),
                    strokeWidth = 1.dp.toPx()
                )
                x += 12.dp.toPx()
            }
            drawLine(
                color = Color(0x59B4DCFF),
                start = Offset(0f, size.height - 0.5.dp.toPx()),
                end = Offset(size.width, size.height - 0.5.dp.toPx()),
                strokeWidth = 1.dp.toPx()
            )
        }

        Row(
            Modifier
                .fillMaxSize()
                .padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "NTF-020 · BEARING ASSY",
                color = Color(0xB3C8E8FF),
                fontSize = 6.5.sp,
                fontFamily = ThemeFonts.SpaceMono,
                letterSpacing = 1.1.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                repeat(3) { index ->
                    Box(
                        Modifier
                            .size(width = 9.dp, height = 4.dp)
                            .background(
                                if (index == 1) Color(0x59B4DCFF) else Color.Transparent
                            )
                            .border(1.dp, Color(0x8CB4DCFF))
                    )
                }
                Text(
                    "1:1",
                    color = Color(0x8CC8E8FF),
                    fontSize = 5.5.sp,
                    fontFamily = ThemeFonts.SpaceMono,
                    modifier = Modifier.padding(start = 5.dp)
                )
            }
        }
    }
}

/* ------------------------------------------------------------------ */
/* Bearing detail                                                      */
/* ------------------------------------------------------------------ */

@Composable
private fun BoxScope.BlueprintDetail(
    data: TickerData,
    showLogo: Boolean,
    logoSize: Dp,
    height: Dp,
    accent: Color
) {
    val canvasHeight = (height - 13.dp).coerceAtLeast(1.dp)
    val bearingScale = min(92f / 90f, canvasHeight.value / 105f)
    val maxIcon = (34f * bearingScale - 3f).dp
    val iconSize = logoSize
        .coerceIn(18.dp, 30.dp)
        .coerceAtMost(maxIcon)
        .coerceAtLeast(12.dp)

    Box(
        Modifier
            .align(Alignment.TopStart)
            .offset(x = 24.dp, y = 26.dp)
            .size(width = 92.dp, height = height)
            .background(Color(0x4704182C))
            .border(1.dp, Color(0x61B4DCFF))
    ) {
        Text(
            "+",
            color = Color(0x73B4DCFF),
            fontSize = 8.sp,
            fontFamily = ThemeFonts.SpaceMono,
            modifier = Modifier.offset(x = (-4).dp, y = (-4).dp)
        )
        Text(
            "+",
            color = Color(0x73B4DCFF),
            fontSize = 8.sp,
            fontFamily = ThemeFonts.SpaceMono,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .offset(x = 4.dp, y = 4.dp)
        )

        Box(
            Modifier
                .fillMaxSize()
                .padding(bottom = 13.dp),
            contentAlignment = Alignment.Center
        ) {
            Canvas(Modifier.fillMaxSize()) {
                drawBearing(showHub = !showLogo, accent = accent)
            }
            if (showLogo) {
                CardIcon(
                    data = data,
                    size = iconSize,
                    shape = CircleShape,
                    fallbackBg = Color(0xFF061C33),
                    fallbackFg = accent
                )
            }
        }

        Text(
            "Ø 68 · 8 BALL",
            color = Color(0x99C8E8FF),
            fontSize = 5.5.sp,
            fontFamily = ThemeFonts.SpaceMono,
            letterSpacing = 0.6.sp,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 13.dp)
        )
        Text(
            "DETAIL A · 2:1",
            color = Color(0x99C8E8FF),
            fontSize = 5.5.sp,
            fontFamily = ThemeFonts.SpaceMono,
            letterSpacing = 1.sp,
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(start = 5.dp, bottom = 3.dp)
        )
    }
}

private fun DrawScope.drawBearing(showHub: Boolean, accent: Color) {
    val scale = min(size.width / 90f, size.height / 105f)
    val center = Offset(size.width / 2f, size.height / 2f)
    val hairline = 0.8.dp.toPx()
    val dash = PathEffect.dashPathEffect(
        floatArrayOf(3.dp.toPx(), 3.dp.toPx()),
        0f
    )

    // Centre lines.
    drawLine(
        color = accent.copy(alpha = 0.40f),
        start = Offset(center.x, center.y - 46.5f * scale),
        end = Offset(center.x, center.y + 46.5f * scale),
        strokeWidth = hairline,
        pathEffect = dash
    )
    drawLine(
        color = accent.copy(alpha = 0.40f),
        start = Offset(center.x - 35f * scale, center.y),
        end = Offset(center.x + 35f * scale, center.y),
        strokeWidth = hairline,
        pathEffect = dash
    )

    // Bearing races. The inner race is the ring around the app icon.
    listOf(34f, 27.5f, 17f).forEach { radius ->
        drawCircle(
            color = Color(0xC7D2F0FF),
            radius = radius * scale,
            center = center,
            style = Stroke(1.dp.toPx())
        )
    }
    if (showHub) {
        drawCircle(
            color = Color(0xC7D2F0FF),
            radius = 10f * scale,
            center = center,
            style = Stroke(1.dp.toPx())
        )
    }

    // The eight bearing balls.
    val balls = listOf(
        67.25f to 52.5f,
        60.73f to 68.23f,
        45f to 74.75f,
        29.27f to 68.23f,
        22.75f to 52.5f,
        29.27f to 36.77f,
        45f to 30.25f,
        60.73f to 36.77f
    )
    balls.forEach { (bx, by) ->
        val point = Offset(
            center.x + (bx - 45f) * scale,
            center.y + (by - 52.5f) * scale
        )
        drawCircle(accent.copy(alpha = 0.16f), 4.2f * scale, point)
        drawCircle(
            color = Color(0xB3D2F0FF),
            radius = 4.2f * scale,
            center = point,
            style = Stroke(0.9.dp.toPx())
        )
    }
}

/* ------------------------------------------------------------------ */
/* Title block                                                         */
/* ------------------------------------------------------------------ */

@Composable
private fun BoxScope.BlueprintTitleBlock() {
    Column(
        Modifier
            .align(Alignment.BottomStart)
            .offset(x = 24.dp, y = (-12).dp)
            .size(width = 92.dp, height = 34.dp)
            .background(Color(0x8004182C))
            .border(1.dp, Color(0x61B4DCFF))
            .padding(horizontal = 6.dp, vertical = 4.dp),
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            "NOTIFICATION SHEET",
            color = Color(0xFFEAF6FF),
            fontSize = 9.5.sp,
            fontFamily = ThemeFonts.Bebas,
            letterSpacing = 1.6.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Spacer(Modifier.height(2.dp))
        Text(
            "DWG NTF-020 · SHEET 1/1",
            color = Color(0x8CC8E8FF),
            fontSize = 5.5.sp,
            fontFamily = ThemeFonts.SpaceMono,
            letterSpacing = 0.7.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun BlueprintDock(
    data: TickerData,
    interactions: TickerInteractions?,
    scale: Float,
    modifier: Modifier = Modifier
) {
    val specs = notificationActionSpecs(data, interactions)
    if (specs.isEmpty()) return

    Row(
        modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        specs.forEachIndexed { index, spec ->
            val source = remember { MutableInteractionSource() }
            val pressed by source.collectIsPressedAsState()
            Box(
                Modifier
                    .weight(1f)
                    .height(32.dp)
                    .scale(if (pressed) 0.97f else 1f)
                    .drawBehind { drawChalkButton(index, pressed) }
                    .clickable(
                        interactionSource = source,
                        indication = null,
                        enabled = spec.onClick != null
                    ) { spec.onClick?.invoke() }
                    .padding(horizontal = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                if (spec.iconOnly) {
                    Icon(
                        spec.icon,
                        contentDescription = spec.label,
                        tint = Color(0xFFEAF6FF),
                        modifier = Modifier.size(16.dp * scale)
                    )
                } else {
                    Text(
                        spec.label.uppercase(),
                        color = Color(0xFFEAF6FF),
                        fontSize = (12f * scale).sp,
                        fontFamily = ThemeFonts.SpaceMono,
                        letterSpacing = 1.2.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

/** A double, slightly broken outline so the button looks chalked on. */
private fun DrawScope.drawChalkButton(index: Int, pressed: Boolean) {
    val chalk = if (pressed) Color.White else Color(0xFFE8F8FF)
    val inset = 0.75.dp.toPx()
    val corner = 2.dp.toPx()
    val width = size.width - inset * 2f
    val height = size.height - inset * 2f
    if (width <= 0f || height <= 0f) return

    drawRoundRect(
        color = chalk.copy(alpha = 0.86f),
        topLeft = Offset(inset, inset),
        size = Size(width, height),
        cornerRadius = CornerRadius(corner, corner),
        style = Stroke(width = 1.5.dp.toPx(), cap = StrokeCap.Round)
    )
    drawRoundRect(
        color = chalk.copy(alpha = 0.40f),
        topLeft = Offset(inset + 0.7.dp.toPx(), inset + 0.5.dp.toPx()),
        size = Size(width - 1.4.dp.toPx(), height - 1.dp.toPx()),
        cornerRadius = CornerRadius(corner, corner),
        style = Stroke(
            width = 1.dp.toPx(),
            cap = StrokeCap.Round,
            pathEffect = PathEffect.dashPathEffect(
                floatArrayOf(2.4.dp.toPx(), 1.6.dp.toPx()),
                phase = index * 1.3f
            )
        )
    )
    drawRoundRect(
        color = chalk.copy(alpha = 0.24f),
        topLeft = Offset(inset + 1.8.dp.toPx(), inset + 1.6.dp.toPx()),
        size = Size(width - 3.6.dp.toPx(), height - 3.2.dp.toPx()),
        cornerRadius = CornerRadius(corner, corner),
        style = Stroke(
            width = 1.dp.toPx(),
            cap = StrokeCap.Round,
            pathEffect = PathEffect.dashPathEffect(
                floatArrayOf(1.2.dp.toPx(), 2.6.dp.toPx()),
                phase = index * 2.1f + 0.5f
            )
        )
    )
}
