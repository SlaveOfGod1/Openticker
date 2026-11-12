package com.openticker.ticker.cards

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.Canvas
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
import com.openticker.ui.components.AppBadge
import kotlin.math.cos
import kotlin.math.sin

/* ==================================================================== */
/* 01 — Neon Arcade                                                     */
/* ==================================================================== */

private val ArcInk = Color(0xFF7DF9FF)
private val ArcBody = Color(0xFFA9B6FF)
private val ArcAccent = Color(0xFFFF4FD8)
private val ArcGold = Color(0xFFFFD84D)

@Composable
fun CuratedArcadeTicker(
    data: TickerData,
    interactions: TickerInteractions?,
    animateIn: Boolean,
    exitFraction: () -> Float
) {
    val context = LocalContext.current
    val opts = Prefs.themeOptions(context, "arcade", 0xFF7DF9FF.toInt(), 40)
    val ink = if (opts.accentArgb == 0xFF7DF9FF.toInt()) ArcInk else Color(opts.accentArgb)
    val lines = opts.lines
    val intensity = opts.intensity
    val showTime = opts.showTime
    val showLogo = opts.showLogo
    val logoSize = opts.avatarSize.dp
    val scale = opts.buttonScale / 100f
    val effect = opts.effect

    val cardT = rememberCardEnter(animateIn, 900)
    val textE = rememberCardEnter(animateIn, 550, 320)
    val dockE = rememberCardEnter(animateIn, 420, 620)
    val coinP = ambientAlternate(2600)
    val flick = if (effect) ambientLoop(4200) else 0f
    // occasional bright dips: a CRT lamp that loses charge, then rights itself
    val flickDip = keyframe(
        flick,
        0f to 0f, 0.012f to 0.55f, 0.022f to 0.08f, 0.030f to 0.7f, 0.042f to 0f,
        0.318f to 0f, 0.326f to 0.35f, 0.334f to 0f,
        0.585f to 0f, 0.599f to 0.5f, 0.606f to 0.1f, 0.614f to 0.4f, 0.626f to 0f,
        0.872f to 0f, 0.881f to 0.28f, 0.889f to 0f
    )
    // and short bursts of extra brightness across the scanlines
    val flickBeam = keyframe(
        flick,
        0f to 0f, 0.055f to 0f, 0.07f to 0.55f, 0.085f to 0f,
        0.44f to 0f, 0.45f to 0.4f, 0.464f to 0f,
        0.74f to 0f, 0.752f to 0.5f, 0.766f to 0f, 1f to 0f
    )
    val ex = exitFraction()

    val stars = listOf(
        Triple(0.14f, 0.66f, Color(0xFF9FE8FF)) to 0f,
        Triple(0.78f, 0.34f, Color(0xFFFF9FE0)) to 0.33f,
        Triple(0.64f, 0.80f, Color(0xFFFFFFFF)) to 0.61f,
        Triple(0.36f, 0.24f, Color(0xFF9FE8FF)) to 0.17f,
        Triple(0.88f, 0.66f, Color(0xFFFFD84D)) to 0.83f
    )
    val starP = stars.map { ambientLoop(1800, it.second) }
    // The name sits right under the HI-SCORE row, the way the reference has it.
    val textTop = 24.dp
    val coinSize = 18.dp

    CardRoot(
        animateIn = animateIn,
        exitFraction = exitFraction,
        enterDurationMs = 900,
        enter = { t ->
            CardLayer(
                alpha = keyframe(t, 0f to 0f, 0.28f to 1f, 0.54f to 0.62f, 0.60f to 1f, 0.67f to 0.72f, 0.73f to 1f),
                sx = keyframe(t, 0f to 0.58f, 0.18f to 0.92f, 0.30f to 1.06f, 0.50f to 0.962f, 0.68f to 1.018f, 0.84f to 0.996f, 1f to 1f),
                sy = keyframe(t, 0f to 0.025f, 0.16f to 0.55f, 0.32f to 0.72f, 0.48f to 1.11f, 0.66f to 0.975f, 0.80f to 1.012f, 1f to 1f),
                ty = keyframe(t, 0f to 5f, 0.30f to 3.5f, 0.48f to -1.5f, 0.70f to 0.8f, 1f to 0f)
            )
        },
        exit = { p ->
            CardLayer(
                alpha = keyframe(p, 0f to 1f, 0.10f to 0.9f, 0.22f to 1f, 0.34f to 0.5f, 0.46f to 0.8f, 0.58f to 0.28f, 0.72f to 0.55f, 0.86f to 0.12f, 1f to 0f),
                tx = keyframe(p, 0f to 0f, 0.12f to -3f, 0.26f to 2.4f, 0.40f to -2f, 0.52f to 1.4f, 0.64f to -0.7f, 1f to 0f),
                sx = keyframe(p, 0f to 1f, 0.30f to 0.97f, 0.58f to 1.02f, 0.78f to 0.93f, 1f to 0.7f),
                sy = keyframe(p, 0f to 1f, 0.52f to 0.98f, 0.68f to 0.72f, 0.82f to 0.28f, 0.92f to 0.045f, 1f to 0.02f)
            )
        }
    ) {
        CardFrame(
            lines = lines,
            hasDock = data.actions.isNotEmpty(),
            // One line of text plus the dock; every extra line adds 16.9dp.
            baseHeight = 146.dp,
            dockSpace = 42.dp,
            decorationAlpha = intensity / 100f,
            shape = RoundedCornerShape(12.dp),
            background = Brush.verticalGradient(
                0f to Color(0xFF0B0420), 0.7f to Color(0xFF070215), 1f to Color(0xFF0A0322)
            ),
            border = Color(0xFF37206E),
            borderWidth = 3.dp
        ) {
            Canvas(Modifier.fillMaxSize().graphicsLayer { alpha = intensity / 100f }) {
                drawRect(
                    Brush.radialGradient(
                        listOf(Color(0x477D3CFF), Color.Transparent),
                        center = Offset(size.width / 2f, 0f),
                        radius = size.width * 0.42f
                    )
                )
                drawRect(
                    Brush.radialGradient(
                        listOf(Color(0x29FF4FD8), Color.Transparent),
                        center = Offset(size.width / 2f, size.height * 1.18f),
                        radius = size.width * 0.55f
                    )
                )
                drawRect(
                    Brush.radialGradient(
                        listOf(Color(0x247DF9FF), Color.Transparent),
                        center = Offset(size.width * 0.22f, size.height * 1.08f),
                        radius = size.width * 0.3f
                    )
                )
                var y = 0f
                while (y < size.height) {
                    drawRect(Color(0x12A078FF), Offset(0f, y), Size(size.width, 1.dp.toPx()))
                    y += 3.dp.toPx()
                }
                stars.forEachIndexed { index, pair ->
                    val (spec, _) = pair
                    val (fx, fy, col) = spec
                    val p = if (effect) keyframe(
                        starP[index],
                        0f to 0f, 0.5f to 1f, 1f to 0f
                    ) else 1f
                    drawCircle(
                        col,
                        radius = 1.5.dp.toPx(),
                        center = Offset(size.width * fx, size.height * fy),
                        alpha = 0.15f + 0.85f * p
                    )
                }
            }

            // HI-SCORE + app coin: the coin flips and carries the app icon
            Row(
                Modifier.align(Alignment.TopStart).offset(x = 12.dp, y = 7.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "HI-SCORE ",
                    color = Color(0xFF5E4FA8),
                    fontSize = 7.sp,
                    fontFamily = ThemeFonts.PressStart,
                    letterSpacing = 1.sp
                )
                Text(
                    "003400",
                    color = ArcGold,
                    fontSize = 7.sp,
                    fontFamily = ThemeFonts.PressStart,
                    letterSpacing = 1.sp
                )
            }
            // app coin: a small flipping token in the corner
            Box(
                Modifier
                    .align(Alignment.TopEnd)
                    .offset(x = (-14).dp, y = 8.dp)
                    .size(coinSize)
                    .graphicsLayer {
                        scaleX = keyframe(coinP, 0f to 1f, 0.5f to 0.18f, 1f to 1f)
                    }
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            listOf(Color(0xFFFFE9A8), Color(0xFFFFC94D), Color(0xFFB8860B)),
                            center = Offset(coinSize.value * 0.35f, coinSize.value * 0.3f),
                            radius = coinSize.value * 0.9f
                        )
                    )
            )

            // text block: the name sits right under the HI-SCORE row
            Column(
                Modifier
                    .align(Alignment.TopStart)
                    .padding(start = 20.dp, end = 20.dp, top = textTop)
                    .fillMaxWidth()
                    .cardLayer(
                        if (ex > 0f) CardLayer(alpha = 1f - ex, tx = -8f * ex)
                        else CardLayer(alpha = textE, ty = 10f * (1f - textE))
                    )
            ) {
                CardSenderRow(
                    title = data.title,
                    showTime = showTime,
                    showLogo = showLogo,
                    logoSize = logoSize,
                    logoData = data,
                    time = data.time,
                    titleColor = ink,
                    timeColor = ArcAccent,
                    titleFont = ThemeFonts.PressStart,
                    timeFont = ThemeFonts.SpaceMonoBold,
                    titleSize = 12f,
                    titleLetter = 3.4f * (1f - textE),
                    titleWeight = FontWeight.Normal,
                    timeSize = 9.5f,
                    titleStyle = TextStyle(
                        shadow = Shadow(Color(0xFF1B0A3A), Offset(2f, 2f), 0f)
                    ),
                    timeStyle = TextStyle(
                        shadow = Shadow(Color(0x66FF4FD8), Offset.Zero, 8f)
                    )
                )
                Spacer(Modifier.height(6.dp))
                CardBody(
                    text = data.body,
                    lines = lines,
                    color = ArcBody,
                    font = ThemeFonts.SpaceMonoBold,
                    weight = FontWeight.Bold,
                    sizeSp = 12.5f,
                    lineHeightFactor = 1.35f
                )
            }

            CardDock(
                data = data,
                interactions = interactions,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 11.dp, start = 14.dp, end = 14.dp)
                    .graphicsLayer {
                        // stepped 4-frame "power on" pop for the controls
                        val d = dockE
                        alpha = if (ex > 0f) 1f - ex else if (d >= 1f) 1f else if (d >= 0.66f) 1f else if (d >= 0.33f) 0.5f else 0f
                        translationY = if (ex > 0f) -6f * ex else (1f - d) * 8f
                        val s = if (ex > 0f) 1f - 0.1f * ex else if (d >= 0.66f) 1f else if (d >= 0.33f) 0.62f else 0.4f
                        scaleX = s
                        scaleY = s
                    },
                background = Color(0xFF160A34),
                foreground = ink,
                border = Color(0xFF5433A8),
                shape = RoundedCornerShape(4.dp),
                font = ThemeFonts.PressStart,
                fontSize = 6.5f,
                letterSpacing = 0f,
                uppercase = true,
                height = 26.dp,
                horizontalPadding = 4.dp,
                verticalPadding = 4.dp,
                gap = 7.dp,
                buttonScale = scale,
                borderFor = { shell -> if (shell) Color(0xFFB13BFF) else Color(0xFF5433A8) },
                foregroundFor = { shell -> if (shell) Color(0xFFE3C8FF) else ink },
                backgroundFor = { shell ->
                    Brush.verticalGradient(
                        if (shell) listOf(Color(0xFF1D0D44), Color(0xFF160A34))
                        else listOf(Color(0xFF160A34), Color(0xFF12072B))
                    )
                }
            )

            // CRT power-on flash: bright line snap, then settle
            val flash = keyframe(
                cardT,
                0f to 0f, 0.14f to 0.14f, 0.28f to 0.62f, 0.36f to 0.24f, 0.47f to 0.5f,
                0.55f to 0.06f, 0.62f to 0.28f, 0.72f to 0f, 1f to 0f
            )
            if (flash > 0.001f) {
                Box(Modifier.fillMaxSize().background(Color(0xFFD8F7FF).copy(alpha = flash * 0.6f)))
            }

            // 8-bit dissolve flash on the way out: two pulses of blown-out white
            if (ex > 0f) {
                val outFlash = keyframe(
                    ex,
                    0f to 0f, 0.10f to 0.5f, 0.18f to 0.06f, 0.30f to 0f,
                    0.56f to 0.34f, 0.68f to 0f, 1f to 0f
                )
                if (outFlash > 0.001f) {
                    Box(Modifier.fillMaxSize().background(Color(0xFFB9F0FF).copy(alpha = outFlash * 0.55f)))
                }
            }

            // CRT lamp sagging: quick dips and back, only while the card is showing
            if (effect && ex == 0f) {
                if (flickDip > 0.001f) {
                    Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.22f * flickDip)))
                }
                if (flickBeam > 0.001f) {
                    Box(Modifier.fillMaxSize().background(Color(0xFFA9E8FF).copy(alpha = 0.055f * flickBeam)))
                }
            }
        }
    }
}
