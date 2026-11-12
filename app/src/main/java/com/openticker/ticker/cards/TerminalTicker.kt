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
import kotlin.math.cos
import kotlin.math.sin

/* ==================================================================== */
/* 06 — Green Terminal                                                  */
/* ==================================================================== */

private val TermInk = Color(0xFF7CFF9B)
private val TermBody = Color(0xFF56D364)
private val TermDim = Color(0xFF2E8B44)

@Composable
fun CuratedTerminalTicker(
    data: TickerData,
    interactions: TickerInteractions?,
    animateIn: Boolean,
    exitFraction: () -> Float
) {
    val context = LocalContext.current
    val opts = Prefs.themeOptions(context, "terminal", 0xFF7CFF9B.toInt())
    val ink = if (opts.accentArgb == 0xFF7CFF9B.toInt()) TermInk else Color(opts.accentArgb)
    val lines = opts.lines
    val intensity = opts.intensity
    val showTime = opts.showTime
    val showLogo = opts.showLogo
    val logoSize = opts.avatarSize.dp
    val scale = opts.buttonScale / 100f
    val effect = opts.effect

    val shellE = rememberCardEnter(animateIn, 620)
    val promptE = rememberCardEnter(animateIn, 480, 100)
    val senderE = rememberCardEnter(animateIn, 380, 300)
    val msgE = rememberCardEnter(animateIn, 460, 420)
    val btnE = rememberCardEnter(animateIn, 300, 620)
    val cursor = ambientLoop(1000)
    val band = ambientLoop(5000)
    val flick = ambientLoop(4000)
    val ex = exitFraction()

    // The prompt types itself in, and untypes on the way out.
    val prompt = "ava@openticker:~$ notify --urgent"
    val promptP = if (ex > 0f) 1f - exitDelay(ex, 0.45f) else promptE
    val typed = (prompt.length * promptP.coerceIn(0f, 1f)).toInt()
        .coerceIn(0, prompt.length)
    val typing = typed < prompt.length
    val cursorAlpha = when {
        ex > 0f -> (1f - exitDelay(ex, 0.45f)).coerceIn(0f, 1f)
        typing -> 1f
        effect && cursor >= 0.5f -> 0f
        else -> 1f
    }
    // Elements retract in the reverse of the order they were built.
    val senderP = if (ex > 0f) 1f - exitDelay(ex, 0.30f) else senderE
    val bodyP = if (ex > 0f) 1f - exitDelay(ex, 0.18f) else msgE
    val dockP = if (ex > 0f) 1f - exitDelay(ex, 0.06f) else btnE
    val frameP = if (ex > 0f) 1f - exitDelay(ex, 0.55f) else shellE

    CardRoot(
        animateIn = animateIn,
        exitFraction = exitFraction,
        enterDurationMs = 900,
        enter = { t ->
            CardLayer(
                alpha = keyframe(t, 0f to 0f, 0.14f to 1f, 1f to 1f),
                sy = keyframe(t, 0f to 0.6f, 0.5f to 1f, 1f to 1f),
                originY = 0f
            )
        },
        exit = { p ->
            CardLayer(
                alpha = keyframe(p, 0f to 1f, 0.8f to 1f, 1f to 0f),
                sy = keyframe(p, 0f to 1f, 0.72f to 1f, 1f to 0.02f),
                originY = 0f
            )
        }
    ) {
        CardFrame(
            lines = lines,
            hasDock = data.actions.isNotEmpty(),
            // VT323 is compact: a smaller base removes the empty band that the
            // default 170dp left between the text and the dock.
            baseHeight = 140.dp,
            dockSpace = 41.dp,
            decorationAlpha = intensity / 100f,
            shape = RoundedCornerShape(8.dp),
            background = Brush.verticalGradient(listOf(Color(0xFF030904), Color(0xFF030904))),
            border = Color(0xFF0F2A12),
            borderWidth = 2.dp
        ) {
            val bandY = -0.12f + 1.2f * band
            Canvas(Modifier.fillMaxSize().graphicsLayer { alpha = intensity / 100f }) {
                drawRect(
                    Brush.radialGradient(
                        listOf(Color(0x291E6E2D), Color.Transparent),
                        center = Offset(size.width / 2f, size.height * 0.42f),
                        radius = size.width * 0.5f
                    )
                )
                var y = 0f
                while (y < size.height) {
                    drawRect(Color(0x0D00FF78), Offset(0f, y), Size(size.width, 1.dp.toPx()))
                    y += 3.dp.toPx()
                }
                if (effect) {
                    drawRect(
                        Brush.verticalGradient(listOf(Color.Transparent, Color(0x1250FF82), Color.Transparent)),
                        Offset(0f, size.height * bandY),
                        Size(size.width, 34.dp.toPx())
                    )
                    val flickAlpha = keyframe(
                        flick,
                        0f to 0f, 0.07f to 1f, 0.09f to 0f, 0.46f to 1f,
                        0.48f to 0f, 0.71f to 1f, 0.73f to 0f, 1f to 0f
                    )
                    drawRect(Color(0x0878FF8C).copy(alpha = 0.03f * flickAlpha))
                }
            }

            Row(
                Modifier.align(Alignment.TopStart).padding(start = 14.dp, top = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    prompt.take(typed),
                    color = Color(0xFF3FA34D),
                    fontSize = 12.5.sp,
                    fontFamily = ThemeFonts.VT323
                )
                Box(
                    Modifier
                        .padding(start = 3.dp)
                        .size(width = 7.dp, height = 11.dp)
                        .background(ink.copy(alpha = cursorAlpha))
                )
            }

            Column(
                Modifier
                    .align(Alignment.TopStart)
                    .padding(start = 20.dp, end = 20.dp, top = 40.dp)
                    .fillMaxWidth()
            ) {
                Row(
                    Modifier.fillMaxWidth().cardReveal(senderP),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "$ ",
                        color = TermDim,
                        fontSize = 21.sp,
                        fontFamily = ThemeFonts.VT323
                    )
                    Text(
                        data.title,
                        color = ink,
                        fontSize = 21.sp,
                        fontFamily = ThemeFonts.VT323,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        style = TextStyle(shadow = Shadow(Color(0x8C3CFF78), Offset.Zero, 7f)),
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    Spacer(Modifier.weight(1f))
                    if (showTime) {
                        Text(
                            "[${data.time.uppercase()}]",
                            color = TermDim,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.ExtraBold,
                            fontFamily = ThemeFonts.VT323,
                            letterSpacing = 0.5.sp
                        )
                    }
                }
                Spacer(Modifier.height(5.dp))
                CardBody(
                    text = data.body,
                    lines = lines,
                    color = TermBody,
                    font = ThemeFonts.VT323,
                    weight = FontWeight.Normal,
                    sizeSp = 14.5f,
                    lineHeightFactor = 1f,
                    modifier = Modifier.cardReveal(bodyP)
                )
            }

            CardDock(
                data = data,
                interactions = interactions,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 11.dp, start = 14.dp, end = 14.dp)
                    .cardReveal(dockP),
                background = Color.Transparent,
                foreground = ink,
                border = Color(0xFF1F5B2A),
                shape = RoundedCornerShape(3.dp),
                font = ThemeFonts.VT323,
                fontSize = 14f,
                letterSpacing = 1f,
                uppercase = false,
                bold = false,
                height = 30.dp,
                horizontalPadding = 6.dp,
                verticalPadding = 5.dp,
                gap = 8.dp,
                buttonScale = scale,
                borderFor = { Color(0xFF1F5B2A) },
                foregroundFor = { ink },
                backgroundFor = {
                    Brush.verticalGradient(listOf(Color.Transparent, Color.Transparent))
                }
            )

            // The "reconstruction" layer: the frame draws itself, a scanline
            // sweeps the panel and a few glitch bars flicker while the rest of
            // the interface types itself in. On exit the whole thing rewinds.
            Canvas(Modifier.fillMaxSize()) {
                val reveal = frameP.coerceIn(0f, 1f)
                if (reveal < 0.999f) {
                    val w = size.width
                    val h = size.height
                    val stroke = 1.3.dp.toPx()
                    var left = 2f * (w + h) * reveal

                    fun edge(x1: Float, y1: Float, x2: Float, y2: Float) {
                        if (left <= 0f) return
                        val len = kotlin.math.sqrt(
                            (x2 - x1) * (x2 - x1) + (y2 - y1) * (y2 - y1)
                        )
                        if (len <= 0f) return
                        val f = (left / len).coerceIn(0f, 1f)
                        val endX = x1 + (x2 - x1) * f
                        val endY = y1 + (y2 - y1) * f
                        drawLine(
                            Color(0x667CFF9B),
                            Offset(x1, y1),
                            Offset(endX, endY),
                            strokeWidth = stroke * 4f,
                            cap = StrokeCap.Round
                        )
                        drawLine(
                            Color(0xFFB8FFC7),
                            Offset(x1, y1),
                            Offset(endX, endY),
                            strokeWidth = stroke,
                            cap = StrokeCap.Round
                        )
                        left -= len
                    }

                    edge(0f, 0f, w, 0f)
                    edge(w, 0f, w, h)
                    edge(w, h, 0f, h)
                    edge(0f, h, 0f, 0f)

                    val scanY = h * reveal
                    drawRect(
                        Brush.verticalGradient(
                            listOf(Color.Transparent, Color(0x887CFF9B), Color.Transparent)
                        ),
                        Offset(0f, scanY - 12.dp.toPx()),
                        Size(w, 24.dp.toPx())
                    )
                    drawRect(
                        Color(0xFFB8FFC7),
                        Offset(0f, scanY),
                        Size(w, 1.2.dp.toPx())
                    )

                    val glitch = 1f - reveal
                    if (glitch > 0f) {
                        drawRect(
                            Color(0x557CFF9B).copy(alpha = 0.35f * glitch),
                            Offset(w * 0.10f, h * 0.22f),
                            Size(w * 0.6f, 2.dp.toPx())
                        )
                        drawRect(
                            Color(0x557CFF9B).copy(alpha = 0.22f * glitch),
                            Offset(w * 0.32f, h * 0.55f),
                            Size(w * 0.45f, 1.5.dp.toPx())
                        )
                        drawRect(
                            Color(0x557CFF9B).copy(alpha = 0.16f * glitch),
                            Offset(0f, h * 0.78f),
                            Size(w * 0.3f, 1.dp.toPx())
                        )
                    }
                }
            }
        }
    }
}
