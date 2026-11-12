package com.openticker.ticker.cards

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.openticker.data.Prefs
import com.openticker.data.TickerData
import com.openticker.data.TickerInteractions
import com.openticker.ticker.styles.notificationActionSpecs
import kotlin.math.cos
import kotlin.math.sin

/* ==================================================================== */
/* 17 — Zen Garden (nature — button size only)                          */
/* ==================================================================== */
/*
 * Raked sand with a smooth stone set into it, bamboo stalks swaying at
 * the edge and a few pebbles scattered about.
 *
 * Appearing: the stone is placed and its rings spread across the sand,
 * the bamboo and pebbles come in and the words brush on.
 * Leaving: a rake drags down over the whole card and smooths it flat.
 */

/* ------------------------------------------------------------------ */
/* The dock: plain plaques, centred so the stone keeps its corner      */
/* ------------------------------------------------------------------ */

@Composable
private fun ZenDock(
    data: TickerData,
    interactions: TickerInteractions?,
    scale: Float,
    btnE: List<Float>,
    ex: Float,
    modifier: Modifier = Modifier
) {
    val specs = notificationActionSpecs(data, interactions)
    if (specs.isEmpty()) return

    Row(
        modifier,
        horizontalArrangement = Arrangement.spacedBy(9.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically
    ) {
        specs.forEachIndexed { index, spec ->
            val source = remember { MutableInteractionSource() }
            val pressed by source.collectIsPressedAsState()
            val read = spec.label.lowercase().contains("read") ||
                spec.label.contains("مقروء") ||
                spec.label.contains("قراءة")
            val p = btnE.getOrElse(index) { 1f }

            Box(
                Modifier
                    .cardLayer(
                        if (ex > 0f) {
                            CardLayer(alpha = 1f - exitDelay(ex, 0.20f))
                        } else {
                            CardLayer(
                                alpha = keyframe(p, 0f to 0f, 0.60f to 1f, 1f to 1f),
                                sx = keyframe(p, 0f to 0.4f, 0.60f to 1.08f, 1f to 1f),
                                sy = keyframe(p, 0f to 0.4f, 0.60f to 1.08f, 1f to 1f),
                                rot = keyframe(p, 0f to -8f, 0.60f to 2f, 1f to 0f)
                            )
                        }
                    )
                    .clip(RoundedCornerShape(2.dp))
                    .background(
                        if (read) {
                            Brush.verticalGradient(listOf(Color(0xFF3E3A32), Color(0xFF33302A)))
                        } else {
                            Brush.verticalGradient(listOf(Color.Transparent, Color.Transparent))
                        }
                    )
                    .border(1.dp, Color(0xFF5A5751), RoundedCornerShape(2.dp))
                    .clickable(
                        interactionSource = source,
                        indication = null,
                        enabled = spec.onClick != null
                    ) { spec.onClick?.invoke() }
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    spec.label.uppercase(),
                    color = if (read) Color(0xFFEFE7D6) else Color(0xFF3E3A32),
                    fontSize = (10f * scale).sp,
                    fontFamily = ThemeFonts.ZenBold,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.5.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
fun CuratedZenTicker(
    data: TickerData,
    interactions: TickerInteractions?,
    animateIn: Boolean,
    exitFraction: () -> Float
) {
    val context = LocalContext.current
    val opts = Prefs.themeOptions(context, "zen", 0xFF8A8272.toInt())
    val lines = opts.lines
    val intensity = opts.intensity
    val showTime = opts.showTime
    val showLogo = opts.showLogo
    val logoSize = opts.avatarSize.dp
    val scale = opts.buttonScale / 100f
    val effect = opts.effect

    val stoneE = rememberCardEnter(animateIn, 600, 250)
    val textE = rememberCardEnter(animateIn, 700, 550)
    val lateE = rememberCardEnter(animateIn, 500, 450)
    val btnE = listOf(
        rememberCardEnter(animateIn, 400, 800),
        rememberCardEnter(animateIn, 400, 900)
    )
    val ripple1 = if (effect) ambientLoop(5000) else 0f
    val ripple2 = if (effect) ambientLoop(5000, 0.5f) else 0.5f
    val sway1 = if (effect) ambientAlternate(4500) else 0.5f
    val sway2 = if (effect) (1f - ambientAlternate(4500)) else 0.5f
    val leafSway1 = if (effect) ambientAlternate(3800) else 0.5f
    val leafSway2 = if (effect) (1f - ambientAlternate(3800)) else 0.5f
    val ex = exitFraction()

    CardRoot(
        animateIn = animateIn,
        exitFraction = exitFraction,
        enterDurationMs = 550,
        enter = { t ->
            CardLayer(
                alpha = t,
                sx = 0.96f + 0.04f * t,
                sy = 0.96f + 0.04f * t,
                ty = 8f * (1f - t)
            )
        },
        exit = { p -> CardLayer(alpha = 1f - exitDelay(p, 0.90f)) }
    ) {
        CardFrame(
            lines = lines,
            hasDock = data.actions.isNotEmpty(),
            // The stones and bonsai fill the space above the dock.
            baseHeight = 130.dp,
            dockSpace = 44.dp,
            decorationAlpha = intensity / 100f,
            shape = RoundedCornerShape(12.dp),
            background = Brush.verticalGradient(listOf(Color(0xFFF1E9D8), Color(0xFFE7DCC6)))
        ) {
            Canvas(Modifier.fillMaxSize().graphicsLayer { alpha = intensity / 100f }) {
                // rake rings
                val c1x = size.width * 0.30f
                val c1y = size.height * 1.28f
                var r = 26.dp.toPx()
                while (r < size.width * 0.9f) {
                    drawCircle(
                        Color(0x21786446),
                        radius = r,
                        center = Offset(c1x, c1y),
                        style = Stroke(2.dp.toPx())
                    )
                    r += 55.dp.toPx()
                }
                val c2x = size.width * 0.88f
                val c2y = -size.height * 0.30f
                r = 24.dp.toPx()
                while (r < size.width * 0.8f) {
                    drawCircle(
                        Color(0x1A786446),
                        radius = r,
                        center = Offset(c2x, c2y),
                        style = Stroke(2.dp.toPx())
                    )
                    r += 50.dp.toPx()
                }

                // stone
                val stoneLeft = 24.dp.toPx()
                val stoneTop = size.height - 22.dp.toPx() - 42.dp.toPx()
                val stoneW = 66.dp.toPx()
                val stoneH = 42.dp.toPx()
                val stoneDrop = keyframe(stoneE, 0f to -20f, 0.65f to 2f, 1f to 0f)
                val stoneScale = keyframe(stoneE, 0f to 1.06f, 0.65f to 0.99f, 1f to 1f)
                val sx = stoneLeft + stoneW / 2f
                val sy = stoneTop + stoneH / 2f + stoneDrop.dp.toPx()
                // ripples
                listOf(ripple1, ripple2).forEach { rp ->
                    val scale = 0.55f + 1.05f * rp
                    drawOval(
                        Color(0x59786464).copy(alpha = (0.85f * (1f - rp)).coerceIn(0f, 1f)),
                        topLeft = Offset(sx - 60.dp.toPx() * scale, sy - 29.dp.toPx() * scale),
                        size = Size(120.dp.toPx() * scale, 58.dp.toPx() * scale),
                        style = Stroke(1.5.dp.toPx())
                    )
                }
                rotate(0f, Offset(sx, sy)) {
                    drawOval(
                        Brush.radialGradient(
                            listOf(Color(0xFF6E6A62), Color(0xFF46443E), Color(0xFF33322D)),
                            center = Offset(sx - 12.dp.toPx(), sy - 10.dp.toPx()),
                            radius = stoneW * 0.7f
                        ),
                        topLeft = Offset(sx - stoneW * 0.5f * stoneScale, sy - stoneH * 0.5f * stoneScale),
                        size = Size(stoneW * stoneScale, stoneH * stoneScale),
                        alpha = stoneE
                    )
                }

                // bamboo
                val bambooAlpha = lateE
                val bambooRight = size.width - 20.dp.toPx()
                fun stalk(right: Float, h: Float, sway: Float, phaseDelay: Float) {
                    val rot = -6f + 13f * sway
                    val bx = bambooRight - right
                    val by = 12.dp.toPx() + 90.dp.toPx()
                    rotate(rot, Offset(bx, by)) {
                        drawRoundRect(
                            Brush.verticalGradient(listOf(Color(0xFF8FAE68), Color(0xFF5F7E44))),
                            Offset(bx - 2.25.dp.toPx(), by - h),
                            Size(4.5.dp.toPx(), h),
                            CornerRadius(3.dp.toPx()),
                            alpha = bambooAlpha
                        )
                        drawRect(
                            Color(0x8050461E),
                            Offset(bx - 2.25.dp.toPx(), by - h * 0.34f),
                            Size(4.5.dp.toPx(), 2.dp.toPx()),
                            alpha = bambooAlpha
                        )
                        drawRect(
                            Color(0x8050461E),
                            Offset(bx - 2.25.dp.toPx(), by - h * 0.68f),
                            Size(4.5.dp.toPx(), 2.dp.toPx()),
                            alpha = bambooAlpha
                        )
                    }
                }
                stalk(20.dp.toPx(), 86.dp.toPx(), sway1, 0f)
                stalk(6.dp.toPx(), 62.dp.toPx(), sway2, 2.2f)

                // leaf blades
                fun blade(right: Float, top: Float, mirrored: Boolean, sway: Float) {
                    val bx = bambooRight - right
                    val by = 12.dp.toPx() + top
                    rotate(-6f + 13f * sway, Offset(bx, by + 9.dp.toPx())) {
                        val path = Path().apply {
                            moveTo(bx, by + 9.dp.toPx())
                            quadraticBezierTo(
                                bx + 20.dp.toPx(), by + 9.dp.toPx(),
                                bx + 20.dp.toPx(), by
                            )
                            quadraticBezierTo(
                                bx, by,
                                bx, by + 9.dp.toPx()
                            )
                            close()
                        }
                        drawPath(
                            path,
                            Brush.linearGradient(
                                listOf(Color(0xFF8FAE68), Color(0xFF5F7E44)),
                                start = Offset(bx, by),
                                end = Offset(bx + 20.dp.toPx(), by + 9.dp.toPx())
                            ),
                            alpha = bambooAlpha
                        )
                    }
                }
                blade(16.dp.toPx(), 8.dp.toPx(), false, leafSway1)
                blade(2.dp.toPx(), 20.dp.toPx(), true, leafSway2)

                // pebbles
                listOf(
                    Triple(84f, 20f, 18f to 10f),
                    Triple(108f, 18f, 12f to 8f),
                    Triple(64f, 17f, 10f to 7f)
                ).forEach { (r, b, sz) ->
                    drawOval(
                        Brush.radialGradient(
                            listOf(Color(0xFFA89878), Color(0xFF7A6A4E)),
                            center = Offset(size.width - r.dp.toPx() - sz.first.dp.toPx() * 0.3f, size.height - b.dp.toPx() - sz.second.dp.toPx() * 0.4f),
                            radius = sz.first.dp.toPx()
                        ),
                        topLeft = Offset(
                            size.width - r.dp.toPx() - sz.first.dp.toPx() / 2f,
                            size.height - b.dp.toPx() - sz.second.dp.toPx() / 2f
                        ),
                        size = Size(sz.first.dp.toPx(), sz.second.dp.toPx()),
                        alpha = lateE
                    )
                }

                // rake on exit
                if (ex > 0f) {
                    val rp = keyframe(ex, 0f to -1.02f, 0.78f to 0f, 1f to 0f)
                    clipPath(Path().apply {
                        addRect(
                            androidx.compose.ui.geometry.Rect(
                                0f, size.height * rp, size.width, size.height
                            )
                        )
                    }) {
                        var y = size.height * rp
                        while (y < size.height) {
                            drawRect(
                                Color(0xFFF1E9D8),
                                Offset(0f, y),
                                Size(size.width, 9.dp.toPx())
                            )
                            drawRect(
                                Color(0xFFE0D4BC),
                                Offset(0f, y + 9.dp.toPx()),
                                Size(size.width, 2.5.dp.toPx())
                            )
                            y += 11.5.dp.toPx()
                        }
                    }
                }
            }

            Column(
                Modifier
                    .align(Alignment.TopStart)
                    .padding(start = 22.dp, end = 84.dp, top = 18.dp)
                    .fillMaxWidth()
                    .cardLayer(
                        if (ex > 0f) CardLayer(alpha = 1f - exitDelay(ex, 0.12f))
                        else CardLayer(alpha = textE, ty = 6f * (1f - textE))
                    )
            ) {
                CardSenderRow(
                    title = data.title,
                    showTime = showTime,
                    showLogo = showLogo,
                    logoSize = logoSize,
                    logoData = data,
                    time = data.time,
                    titleColor = Color(0xFF3E3A32),
                    timeColor = Color(0xFF8A8272),
                    titleFont = ThemeFonts.ZenBold,
                    timeFont = ThemeFonts.ZenBold,
                    titleSize = 17f,
                    titleLetter = 0.3f,
                    titleWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(5.dp))
                CardBody(
                    text = data.body,
                    lines = lines,
                    color = Color(0xFF5A554A),
                    font = ThemeFonts.ZenBold,
                    weight = FontWeight.Bold,
                    sizeSp = 12f,
                    lineHeightFactor = 1.3333f
                )
            }

            ZenDock(
                data = data,
                interactions = interactions,
                scale = scale,
                btnE = btnE,
                ex = ex,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 14.dp)
            )
        }
    }
}
