package com.openticker.ticker.cards

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.openticker.data.Prefs
import com.openticker.data.TickerData
import com.openticker.data.TickerInteractions
import com.openticker.ticker.styles.notificationActionSpecs

/* ==================================================================== */
/* 18 — Bento Box                                                       */
/* ==================================================================== */
/*
 * The notification is served inside a black lacquer bento.
 *
 * Appearing: the whole box is pushed down into place from above, it
 * stops, the lid slides straight off and every compartment fills one
 * after another — the note, written on a sheet of rice paper, included.
 *
 * Leaving: the entrance played backwards. The compartments drop away,
 * the lid closes over the box and the whole thing is pulled up and out
 * of frame. It never fades — it is taken away.
 */

private val BentoTable0 = Color(0xFF241A13)
private val BentoTable1 = Color(0xFF171009)
private val BentoTable2 = Color(0xFF0D0906)
private val BentoLid0 = Color(0xFF8E2632)
private val BentoLid1 = Color(0xFF5C1119)
private val BentoLid2 = Color(0xFF3A0A10)
private val BentoTray0 = Color(0xFF3E2419)
private val BentoTray1 = Color(0xFF1E110C)
private val BentoWall0 = Color(0xFF7E1F27)
private val BentoWall1 = Color(0xFF4A0F16)
private val BentoGold0 = Color(0xFFE5C56B)
private val BentoGold1 = Color(0xFF8A6E1A)
private val BentoPaper0 = Color(0xFFF8F1E0)
private val BentoPaper1 = Color(0xFFEBDFC4)
private val BentoTimeColor = Color(0xFF9A7A3A)
private val BentoInk = Color(0xFF2E2419)
private val BentoInkSoft = Color(0xFF4E4232)
private val BentoRoll0 = Color(0xFFF7D979)
private val BentoRoll1 = Color(0xFFE0A93C)
private val BentoRoll2 = Color(0xFFB8801F)
private val BentoFish0 = Color(0xFFE8C79A)
private val BentoFish1 = Color(0xFFC99A63)
private val BentoFish2 = Color(0xFF9A6E3E)
private val BentoRice0 = Color(0xFFFFFDF6)
private val BentoRice1 = Color(0xFFEDE6D2)
private val BentoRice2 = Color(0xFFD6CBB0)
private val BentoGreen0 = Color(0xFF6E9C4A)
private val BentoGreen1 = Color(0xFF4E7C38)
private val BentoGreen2 = Color(0xFF31572A)
private val BentoGreen3 = Color(0xFF23401E)
private val BentoPlum0 = Color(0xFFF08C7E)
private val BentoPlum1 = Color(0xFFC0392B)
private val BentoPlum2 = Color(0xFF7E1E18)
private val BentoChop0 = Color(0xFFC67A48)
private val BentoChop1 = Color(0xFF8A4522)
private val BentoChop2 = Color(0xFFB36E40)
private val BentoChop3 = Color(0xFF7A3E1E)

/** The whole choreography, in milliseconds. */
private const val BentoEnterMs = 2150f

/** The way out. */
private const val BentoExitMs = 1750f

private val BentoEaseServed = CubicBezierEasing(0.22f, 0.9f, 0.3f, 1f)
private val BentoEaseLidOff = CubicBezierEasing(0.4f, 0f, 0.3f, 1f)
private val BentoEaseItem = CubicBezierEasing(0.3f, 1.5f, 0.5f, 1f)
private val BentoEasePaper = CubicBezierEasing(0.25f, 1.15f, 0.4f, 1f)
private val BentoEaseOut = CubicBezierEasing(0f, 0f, 0.58f, 1f)
private val BentoEaseBtn = CubicBezierEasing(0.3f, 1.4f, 0.4f, 1f)
private val BentoEaseLidOn = CubicBezierEasing(0.5f, 0f, 0.6f, 1f)
private val BentoEaseIn = CubicBezierEasing(0.42f, 0f, 1f, 1f)

private fun bentoStage(ms: Float, delay: Float, dur: Float): Float =
    ((ms - delay) / dur).coerceIn(0f, 1f)

/** Piecewise keyframes with the CSS timing function applied per segment. */
private fun bentoKey(easing: Easing, t: Float, vararg stops: Pair<Float, Float>): Float {
    if (stops.isEmpty()) return 0f
    if (t <= stops.first().first) return stops.first().second
    var i = 0
    while (i < stops.size - 1) {
        val (t0, v0) = stops[i]
        val (t1, v1) = stops[i + 1]
        if (t <= t1) {
            val f = if (t1 - t0 <= 0.00001f) 1f else (t - t0) / (t1 - t0)
            return v0 + (v1 - v0) * easing.transform(f)
        }
        i++
    }
    return stops.last().second
}

/** One pop of a compartment, shared by every piece of food. */
private class BentoFood(
    val roll: Float,
    val fish: Float,
    val rice: Float,
    val soy: Float,
    val green: Float,
    val chop: Float,
    val plum: Float,
    /** Divider progress on the way in (draws out) and on the way out (drops away). */
    val divIn: Float,
    val divOut: Float
)

/* ==================================================================== */
/* Card                                                                 */
/* ==================================================================== */

@Composable
fun CuratedBentoTicker(
    data: TickerData,
    interactions: TickerInteractions?,
    animateIn: Boolean,
    exitFraction: () -> Float
) {
    val context = LocalContext.current
    val opts = Prefs.themeOptions(context, "bento", 0xFFC9A227.toInt())
    val accent = if (opts.accentArgb == 0xFFC9A227.toInt()) Color(0xFFC9A227) else Color(opts.accentArgb)
    val lines = opts.lines
    val intensity = opts.intensity
    val showTime = opts.showTime
    val showLogo = opts.showLogo
    val logoSize = opts.avatarSize.dp
    val scale = opts.buttonScale / 100f
    val effect = opts.effect

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
            tween(scaleDuration(BentoEnterMs.toInt(), speed), easing = LinearEasing)
        )
    }
    val enterMs = enter.value * BentoEnterMs

    val ex = exitFraction().coerceIn(0f, 1f)
    val exitMs = ex * BentoExitMs
    val leaving = ex > 0f

    // Staged entrances — linear 0..1, eased per element below.
    val lidIn = rememberCardEnter(animateIn, 600, 550)
    val divIn = rememberCardEnter(animateIn, 450, 950)
    val rollIn = rememberCardEnter(animateIn, 450, 1000)
    val fishIn = rememberCardEnter(animateIn, 450, 1100)
    val riceIn = rememberCardEnter(animateIn, 450, 1200)
    val soyIn = rememberCardEnter(animateIn, 450, 1260)
    val greenIn = rememberCardEnter(animateIn, 450, 1320)
    val chopIn = rememberCardEnter(animateIn, 450, 1380)
    val plumIn = rememberCardEnter(animateIn, 450, 1440)
    val paperIn = rememberCardEnter(animateIn, 600, 950)
    val textIn = rememberCardEnter(animateIn, 550, 1050)
    val dockIn = rememberCardEnter(animateIn, 450, 1500)
    val btn1In = rememberCardEnter(animateIn, 400, 1550)
    val btn2In = rememberCardEnter(animateIn, 400, 1650)
    val steamIn = rememberCardEnter(animateIn, 600, 1300)

    val steam1 = if (effect) ambientLoop(3200) else 0f
    val steam2 = if (effect) ambientLoop(3200, 0.34f) else 0f
    val steam3 = if (effect) ambientLoop(3200, 0.69f) else 0f

    // ---- the food -----------------------------------------------------
    fun food(p: Float, delayOut: Float): Float =
        if (leaving) 1f - BentoEaseIn.transform(bentoStage(exitMs, delayOut, 350f))
        else BentoEaseItem.transform(p)

    val food = BentoFood(
        roll = food(rollIn, 440f),
        fish = food(fishIn, 380f),
        rice = food(riceIn, 320f),
        soy = food(soyIn, 260f),
        green = food(greenIn, 200f),
        chop = food(chopIn, 140f),
        plum = food(plumIn, 80f),
        divIn = BentoEaseItem.transform(divIn),
        divOut = BentoEaseIn.transform(bentoStage(exitMs, 500f, 300f))
    )

    // ---- the lid ------------------------------------------------------
    val lidP = if (leaving) {
        1f - BentoEaseLidOn.transform(bentoStage(exitMs, 500f, 450f))
    } else {
        BentoEaseLidOff.transform(lidIn)
    }

    // ---- the note -----------------------------------------------------
    val textP = if (leaving) {
        1f - BentoEaseIn.transform(bentoStage(exitMs, 50f, 300f))
    } else {
        BentoEaseOut.transform(textIn)
    }
    val dockP = if (leaving) {
        1f - BentoEaseIn.transform(bentoStage(exitMs, 50f, 300f))
    } else {
        BentoEaseOut.transform(dockIn)
    }
    val paperP = BentoEasePaper.transform(paperIn)
    val steamP = if (leaving) {
        1f - BentoEaseIn.transform(bentoStage(exitMs, 0f, 300f))
    } else {
        BentoEaseOut.transform(steamIn)
    }

    CardFrame(
        modifier = Modifier
            .fillMaxWidth()
            // On the way out the box is taken up through the top of its own
            // frame, so it is cut at the frame's edge rather than drifting
            // over whatever sits above it — exactly like the reference.
            .then(if (leaving) Modifier.clip(RoundedCornerShape(16.dp)) else Modifier)
            .graphicsLayer {
                if (leaving) {
                    // taken away, never faded
                    alpha = 1f
                    translationY = keyframe(
                        bentoStage(exitMs, 950f, 620f),
                        0f to 0f, 0.2f to -25f, 0.4f to -61f,
                        0.6f to -111f, 0.8f to -177f, 1f to -264f
                    ).dp.toPx()
                } else {
                    val p = bentoStage(enterMs, 0f, 500f)
                    alpha = bentoKey(
                        BentoEaseServed, p,
                        0f to 0f, 0.7f to 1f, 1f to 1f
                    )
                    translationY = bentoKey(
                        BentoEaseServed, p,
                        0f to -58f, 0.7f to 5f, 1f to 0f
                    ).dp.toPx()
                    val s = bentoKey(
                        BentoEaseServed, p,
                        0f to 0.97f, 0.7f to 1.01f, 1f to 1f
                    )
                    scaleX = s
                    scaleY = s
                }
            },
        // The tray is a fixed 196dp, exactly like the reference; the note
        // inside it is clamped to three lines.
        lines = 1,
        hasDock = false,
        baseHeight = 238.dp,
        dockSpace = 42.dp,
        decorationAlpha = intensity / 100f,
        shape = RoundedCornerShape(16.dp),
        background = Brush.verticalGradient(
            colorStops = arrayOf(
                0f to BentoTable0,
                0.52f to BentoTable1,
                1f to BentoTable2
            )
        ),
        border = null
    ) {
        BoxWithConstraints(Modifier.fillMaxSize()) {
            val d = LocalDensity.current.density
            val wDp = maxWidth.value

            // The reference lays the compartments out on a 444 wide card; the
            // note keeps at least 156dp so three buttons always have room.
            val paperLeftDp = 24f
            val paperTopDp = 39f
            val paperWidthDp = maxOf(wDp * 0.52f, 156f)
            val dividerXdp = paperLeftDp + paperWidthDp + 8f
            val paperHeightDp = 136f

            // ---- the table, the tray and the food ---------------------
            Canvas(
                Modifier
                    .fillMaxSize()
                    .graphicsLayer { alpha = intensity / 100f }
            ) {
                drawBentoTable(d)
                drawBentoTray(d, food, leaving, steamP, steam1, steam2, steam3)
            }

            // ---- the note on its rice paper ---------------------------
            Box(
                Modifier
                    .offset(x = paperLeftDp.dp, y = paperTopDp.dp)
                    .width(paperWidthDp.dp)
                    .height(paperHeightDp.dp)
                    .graphicsLayer {
                        val p = paperP
                        alpha = p.coerceIn(0f, 1f)
                        translationY = 16f * (1f - p) * d
                        rotationZ = -2.5f * (1f - p)
                    }
                    .clip(RoundedCornerShape(4.dp))
                    .background(Brush.verticalGradient(listOf(BentoPaper0, BentoPaper1)))
                    .border(1.dp, Color(0x38826437), RoundedCornerShape(4.dp))
            ) {
                // rice-paper fibres and the shade at the foot of the sheet
                Canvas(Modifier.fillMaxSize()) {
                    var x = 0f
                    while (x < size.width) {
                        drawRect(
                            Color(0x0D96783F),
                            Offset(x, 0f),
                            Size(1f * d, size.height)
                        )
                        x += 4f * d
                    }
                    drawRect(
                        Brush.verticalGradient(
                            listOf(Color.Transparent, Color(0x1F96783F)),
                            startY = size.height - 12f * d,
                            endY = size.height
                        ),
                        size = size
                    )
                }

                Column(
                    Modifier
                        .fillMaxSize()
                        .graphicsLayer { alpha = textP.coerceIn(0f, 1f) }
                        .padding(start = 10.dp, end = 10.dp, top = 11.dp, bottom = 10.dp)
                ) {
                    CardSenderRow(
                        title = data.title,
                        showTime = showTime,
                        showLogo = showLogo,
                        logoSize = logoSize,
                        logoData = data,
                        time = data.time,
                        titleColor = BentoInk,
                        timeColor = if (opts.accentArgb == 0xFFC9A227.toInt()) BentoTimeColor else accent,
                        titleFont = ThemeFonts.ZenBold,
                        timeFont = ThemeFonts.NunitoBlack,
                        titleSize = 17f,
                        timeSize = 9f,
                        titleLetter = 0.3f,
                        timeLetter = 1.2f,
                        titleWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.height(5.dp))
                    BentoBody(text = data.body, lines = lines)
                    Spacer(Modifier.height(9.dp))
                    BentoDock(
                        data = data,
                        interactions = interactions,
                        scale = scale,
                        dockP = dockP,
                        btn1P = BentoEaseBtn.transform(btn1In),
                        btn2P = BentoEaseBtn.transform(btn2In),
                        leaving = leaving
                    )
                }
            }

            // ---- the lid ---------------------------------------------
            Box(
                Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        translationY = -size.height * lidP
                        rotationZ = -3f * lidP
                    }
                    .background(
                        Brush.verticalGradient(listOf(BentoLid0, BentoLid1, BentoLid2))
                    )
                    .border(2.dp, Color(0x99C9A227), RoundedCornerShape(16.dp))
            ) {
                Canvas(Modifier.fillMaxSize()) {
                    // the gold mon
                    drawCircle(
                        Color(0x59E8C46A),
                        radius = 22f * d,
                        center = center
                    )
                    drawCircle(
                        Color(0xE6E8C46A),
                        radius = 22f * d,
                        center = center,
                        style = Stroke(2f * d)
                    )
                }
            }
        }
    }
}

/* ==================================================================== */
/* The note                                                             */
/* ==================================================================== */

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun BentoBody(text: String, lines: Int) {
    val speed = Prefs.speed(LocalContext.current)
    val maxLines = if (lines <= 0) 1 else lines.coerceAtMost(3)
    if (lines <= 0) {
        Text(
            text,
            color = BentoInkSoft,
            fontSize = 12.sp,
            lineHeight = 16.sp,
            fontWeight = FontWeight.SemiBold,
            fontFamily = ThemeFonts.NunitoSemi,
            maxLines = 1,
            softWrap = false,
            overflow = TextOverflow.Clip,
            modifier = Modifier
                .fillMaxWidth()
                .basicMarquee(iterations = Int.MAX_VALUE, velocity = (30f * speed).dp)
        )
    } else {
        Text(
            text,
            color = BentoInkSoft,
            fontSize = 12.sp,
            lineHeight = 16.sp,
            fontWeight = FontWeight.SemiBold,
            fontFamily = ThemeFonts.NunitoSemi,
            minLines = 1,
            maxLines = maxLines,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

/* ==================================================================== */
/* The buttons                                                          */
/* ==================================================================== */

@Composable
private fun BentoDock(
    data: TickerData,
    interactions: TickerInteractions?,
    scale: Float,
    dockP: Float,
    btn1P: Float,
    btn2P: Float,
    leaving: Boolean
) {
    val specs = notificationActionSpecs(data, interactions)
    if (specs.isEmpty()) return

    Row(
        Modifier
            .fillMaxWidth()
            .graphicsLayer {
                alpha = dockP.coerceIn(0f, 1f)
                if (!leaving) translationY = 10f.dp.toPx() * (1f - dockP)
            },
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        specs.forEachIndexed { index, spec ->
            val source = remember { MutableInteractionSource() }
            val pressed by source.collectIsPressedAsState()
            val p = if (leaving) 1f else if (index == 0) btn1P else btn2P
            val s = bentoKey(BentoEaseBtn, p, 0f to 0.4f, 0.6f to 1.1f, 1f to 1f)
            val rot = bentoKey(BentoEaseBtn, p, 0f to -14f, 0.6f to 3f, 1f to 0f)
            Box(
                Modifier
                    // Hug the label, but never take more than an equal share
                    // of the paper — so a third action can never be squeezed.
                    .weight(1f, fill = false)
                    .graphicsLayer {
                        scaleX = s
                        scaleY = s
                        rotationZ = rot
                        alpha = p.coerceIn(0f, 1f)
                        if (pressed) {
                            scaleX *= 0.96f
                            scaleY *= 0.96f
                        }
                    }
                    .clip(RoundedCornerShape(5.dp))
                    .background(if (spec.shell) Color(0xFF221A16) else Color(0xFF8C1F28))
                    .border(1.dp, Color(0xFFC9A227), RoundedCornerShape(5.dp))
                    .clickable(
                        interactionSource = source,
                        indication = null,
                        enabled = spec.onClick != null
                    ) { spec.onClick?.invoke() }
                    .padding(horizontal = 6.dp, vertical = 6.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    spec.label,
                    color = if (spec.shell) Color(0xFFEBD9A0) else Color(0xFFFFE9C9),
                    fontSize = (8.5f * scale).sp,
                    fontWeight = FontWeight.ExtraBold,
                    fontFamily = ThemeFonts.NunitoBlack,
                    letterSpacing = 0.5.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

/* ==================================================================== */
/* The table and the tray                                               */
/* ==================================================================== */

/** The dark table the box is served on. */
private fun DrawScope.drawBentoTable(d: Float) {
    drawRect(
        Brush.radialGradient(
            listOf(Color(0x1AFFD696), Color.Transparent),
            center = Offset(size.width * 0.5f, size.height * 0.06f),
            radius = 190f * d
        ),
        size = size
    )
    // the faint stripes of the table
    var x = 0f
    while (x < size.width) {
        drawRect(Color(0x06FFFFFF), Offset(x, 0f), Size(2f * d, size.height))
        x += 7f * d
    }
}

/** The lacquer tray, its gold dividers and every compartment. */
private fun DrawScope.drawBentoTray(
    d: Float,
    food: BentoFood,
    leaving: Boolean,
    steamP: Float,
    steam1: Float,
    steam2: Float,
    steam3: Float
) {
    val w = size.width
    val h = size.height

    val trayL = 14f * d
    val trayR = w - 14f * d
    val trayT = 29f * d
    val trayB = h - 11f * d
    val trayW = trayR - trayL
    val trayH = trayB - trayT

    // ---- the tray -----------------------------------------------------
    drawRoundRect(
        Brush.linearGradient(
            listOf(BentoTray0, BentoTray1),
            start = Offset(trayL, trayT),
            end = Offset(trayR, trayB)
        ),
        topLeft = Offset(trayL, trayT),
        size = Size(trayW, trayH),
        cornerRadius = CornerRadius(10f * d)
    )
    // inset shade, faked with four steps so it falls off like a blur
    var shadeStep = 0
    while (shadeStep < 4) {
        val inset = shadeStep * 5f * d
        drawRoundRect(
            Color(0x40000000),
            topLeft = Offset(trayL + inset, trayT + inset),
            size = Size(trayW - 2f * inset, trayH - 2f * inset),
            cornerRadius = CornerRadius((10f * d - inset).coerceAtLeast(0f)),
            style = Stroke((20f - shadeStep * 5f) * d)
        )
        shadeStep++
    }
    // the gold rim
    drawRoundRect(
        Color(0x73C9A227),
        topLeft = Offset(trayL + 1.5f * d, trayT + 1.5f * d),
        size = Size(trayW - 3f * d, trayH - 3f * d),
        cornerRadius = CornerRadius(8.5f * d),
        style = Stroke(3f * d)
    )

    // ---- the inner back wall -----------------------------------------
    drawRect(
        Brush.verticalGradient(
            listOf(BentoWall0, BentoWall1),
            startY = trayT,
            endY = trayT + 15f * d
        ),
        topLeft = Offset(trayL, trayT),
        size = Size(trayW, 15f * d)
    )
    drawLine(
        Color(0x4DC9A227),
        Offset(trayL, trayT + 15f * d),
        Offset(trayR, trayT + 15f * d),
        strokeWidth = 1f * d
    )

    // ---- the geometry of the compartments ----------------------------
    val dividerX = (24f + maxOf((w / d) * 0.52f, 156f) + 8f) * d
    val foodL = dividerX + 8f * d
    val foodR = trayR
    val regionW = foodR - foodL
    val s = regionW / (158f * d)

    fun pop(
        p: Float,
        x: Float,
        y: Float,
        w0: Float,
        h0: Float,
        bottom: Boolean = false
    ): Pair<Offset, Size> {
        val sc = 0.5f + 0.5f * p
        val ty = 8f * d * (1f - p)
        val left = x + (w0 - w0 * sc) / 2f
        val top = if (bottom) y + h0 * (1f - sc) + ty else y + (h0 - h0 * sc) / 2f + ty
        return Offset(left, top) to Size(w0 * sc, h0 * sc)
    }

    // ---- the dividers ------------------------------------------------
    val dv: Float
    val dh: Float
    val divA: Float
    val divTy: Float
    if (leaving) {
        val sc = 1f - 0.5f * food.divOut
        dv = sc
        dh = sc
        divA = 1f - food.divOut
        divTy = 8f * d * food.divOut
    } else {
        dv = 0.2f + 0.8f * food.divIn
        dh = 0.5f + 0.5f * food.divIn
        divA = food.divIn.coerceIn(0f, 1f)
        divTy = 0f
    }
    if (divA > 0.002f) {
        drawRect(
            Brush.horizontalGradient(listOf(BentoGold0, BentoGold1)),
            Offset(dividerX - 1.25f * d, trayT + 8f * d + (132f * d * (1f - dv)) / 2f + divTy),
            Size(2.5f * d, 132f * d * dv),
            alpha = divA
        )
        drawRect(
            Brush.horizontalGradient(listOf(BentoGold0, BentoGold1)),
            Offset(dividerX, trayT + 54f * d - 1.25f * d + divTy),
            Size(146f * d * s * dh, 2.5f * d),
            alpha = divA
        )
        drawRect(
            Brush.horizontalGradient(listOf(BentoGold0, BentoGold1)),
            Offset(dividerX, trayT + 102f * d - 1.25f * d + divTy),
            Size(146f * d * s * dh, 2.5f * d),
            alpha = divA
        )
    }

    // ---- the golden roll ---------------------------------------------
    run {
        val (tl, sz) = pop(food.roll, foodL, trayT + 12f * d, 132f * d * s, 18f * d)
        val a = food.roll.coerceIn(0f, 1f)
        drawRoundRect(
            Brush.verticalGradient(
                listOf(BentoRoll0, BentoRoll1, BentoRoll2),
                startY = tl.y,
                endY = tl.y + sz.height
            ),
            tl,
            sz,
            CornerRadius(5f * d),
            alpha = a
        )
        // the two darker bands across the roll
        drawRect(
            Color(0x80966E14),
            Offset(tl.x + 6f * d, tl.y + 5f * d),
            Size(sz.width - 12f * d, 2f * d),
            alpha = a
        )
        drawRect(
            Color(0x66966E14),
            Offset(tl.x + 6f * d, tl.y + 10f * d),
            Size(sz.width - 12f * d, 2f * d),
            alpha = a
        )
    }

    // ---- the grilled fish --------------------------------------------
    run {
        val (tl, sz) = pop(food.fish, foodL, trayT + 34f * d, 132f * d * s, 16f * d)
        val a = food.fish.coerceIn(0f, 1f)
        drawRoundRect(
            Brush.linearGradient(
                listOf(BentoFish0, BentoFish1, BentoFish2),
                start = tl,
                end = Offset(tl.x + sz.width, tl.y + sz.height)
            ),
            tl,
            sz,
            CornerRadius(8f * d),
            alpha = a
        )
        // the char lines
        val fishPath = Path().apply {
            addRoundRect(
                RoundRect(
                    tl.x,
                    tl.y,
                    tl.x + sz.width,
                    tl.y + sz.height,
                    CornerRadius(8f * d)
                )
            )
        }
        clipPath(fishPath) {
            var lx = tl.x - sz.height
            while (lx < tl.x + sz.width) {
                drawLine(
                    Color(0x80462812),
                    Offset(lx, tl.y + sz.height),
                    Offset(lx + sz.height * 0.55f, tl.y),
                    strokeWidth = 1.5f * d,
                    alpha = a
                )
                lx += 7f * d
            }
        }
    }

    // ---- the mound of rice -------------------------------------------
    run {
        val (tl, sz) = pop(food.rice, foodL, trayT + 62f * d, 46f * d * s, 34f * d, bottom = true)
        val a = food.rice.coerceIn(0f, 1f)
        val mound = Path().apply {
            moveTo(tl.x, tl.y + sz.height)
            cubicTo(
                tl.x,
                tl.y + sz.height * 0.18f,
                tl.x + sz.width * 0.16f,
                tl.y,
                tl.x + sz.width * 0.5f,
                tl.y
            )
            cubicTo(
                tl.x + sz.width * 0.84f,
                tl.y,
                tl.x + sz.width,
                tl.y + sz.height * 0.18f,
                tl.x + sz.width,
                tl.y + sz.height
            )
            close()
        }
        drawPath(
            mound,
            Brush.verticalGradient(
                listOf(BentoRice0, BentoRice1, BentoRice2),
                startY = tl.y,
                endY = tl.y + sz.height
            ),
            alpha = a
        )
        // the glossy highlight
        drawOval(
            Brush.radialGradient(
                listOf(Color(0xF2FFFFFF), Color.Transparent),
                center = Offset(tl.x + sz.width * 0.26f, tl.y + sz.height * 0.24f),
                radius = sz.width * 0.22f
            ),
            topLeft = Offset(tl.x, tl.y),
            size = Size(sz.width, sz.height),
            alpha = a
        )
    }

    // ---- the umeboshi -------------------------------------------------
    run {
        val (tl, sz) = pop(food.plum, foodL + 16f * d * s, trayT + 70f * d, 14f * d * s, 12f * d)
        val a = food.plum.coerceIn(0f, 1f)
        drawOval(
            Brush.radialGradient(
                listOf(BentoPlum0, BentoPlum1, BentoPlum2),
                center = Offset(tl.x + sz.width * 0.34f, tl.y + sz.height * 0.28f),
                radius = sz.width * 0.75f
            ),
            tl,
            sz,
            alpha = a
        )
    }

    // ---- the greens ---------------------------------------------------
    run {
        val (tl, sz) = pop(food.green, foodL, trayT + 110f * d, 62f * d * s, 28f * d)
        val a = food.green.coerceIn(0f, 1f)
        drawOval(
            Brush.verticalGradient(
                listOf(BentoGreen1, BentoGreen2, BentoGreen3),
                startY = tl.y,
                endY = tl.y + sz.height
            ),
            tl,
            sz,
            alpha = a
        )
        drawOval(
            Brush.radialGradient(
                listOf(BentoGreen0, Color.Transparent),
                center = Offset(tl.x + sz.width * 0.32f, tl.y + sz.height * 0.3f),
                radius = sz.width * 0.3f
            ),
            tl,
            sz,
            alpha = a
        )
    }

    // ---- the soy dish -------------------------------------------------
    run {
        val (tl, sz) = pop(food.soy, foodL + 82f * d * s, trayT + 108f * d, 28f * d * s, 28f * d * s)
        val a = food.soy.coerceIn(0f, 1f)
        drawOval(
            Brush.radialGradient(
                listOf(Color(0xFF4A3020), Color(0xFF2A1A12), Color(0xFFC9A227), BentoGold1),
                center = Offset(tl.x + sz.width * 0.4f, tl.y + sz.height * 0.34f),
                radius = sz.width * 0.62f
            ),
            tl,
            sz,
            alpha = a
        )
    }

    // ---- the chopsticks ----------------------------------------------
    run {
        val p = food.chop
        val sc = 0.5f + 0.5f * p
        val ty = 8f * d * (1f - p)
        val a = p.coerceIn(0f, 1f)
        val chopW = 92f * d * s * sc
        val chopH = 4f * d * sc
        val chopX = dividerX + (92f * d * s - chopW) / 2f
        val chopY = trayT + 4f * d + ty
        rotate(-17f, Offset(chopX + chopW * 0.3f, chopY + chopH / 2f)) {
            drawRoundRect(
                Brush.horizontalGradient(listOf(BentoChop0, BentoChop1)),
                Offset(chopX, chopY),
                Size(chopW, chopH),
                CornerRadius(2f * d),
                alpha = a
            )
            drawRoundRect(
                Brush.horizontalGradient(listOf(BentoChop2, BentoChop3)),
                Offset(chopX, chopY + 6f * d * sc),
                Size(chopW, chopH),
                CornerRadius(2f * d),
                alpha = a
            )
        }
    }

    // ---- the steam ----------------------------------------------------
    if (steamP > 0.002f) {
        fun wisp(x: Float, y: Float, phase: Float) {
            if (phase <= 0f) return
            val yy = y + (4f - 34f * phase) * d
            val ww = 9f * d * (1f + 0.6f * phase)
            val a = (when {
                phase < 0.3f -> phase / 0.3f * 0.9f
                else -> (1f - phase) / 0.7f * 0.9f
            } * steamP).coerceIn(0f, 1f)
            drawOval(
                Brush.verticalGradient(
                    listOf(Color.Transparent, Color(0xBFFFF8EB), Color.Transparent),
                    startY = yy,
                    endY = yy + 28f * d
                ),
                Offset(x - ww / 2f, yy),
                Size(ww, 28f * d),
                alpha = a
            )
        }
        wisp(foodL + 14f * d * s, trayT + 44f * d, steam1)
        wisp(foodL + 26f * d * s, trayT + 40f * d, steam2)
        wisp(foodL + 4f * d * s, trayT + 42f * d, steam3)
    }
}
