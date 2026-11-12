package com.openticker.ticker.cards

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.openticker.data.Prefs
import com.openticker.data.TickerData
import com.openticker.data.TickerInteractions
import com.openticker.ticker.styles.notificationActionSpecs

/* ==================================================================== */
/* 08 — Origami                                                         */
/* ==================================================================== */
/*
 * A sheet of white paper folded into a triangle. It opens out to the
 * left and the right in stepped frames, the note lands, and every
 * piece — the sheet, the app icon, the buttons — shivers on its own
 * like stop-motion paper. Leaving, it folds back into the triangle.
 *
 * The sheet is not a rectangle: its outline is a hand-cut polygon that
 * morphs from the folded triangle to the flat sheet, and the card is
 * exactly as tall as the text it carries.
 */

private val OriInk = Color(0xFF14161A)
private val OriInkSoft = Color(0xFF2B2F36)
private val OriPaperTop = Color(0xFFFFFFFF)
private val OriPaperBottom = Color(0xFFF2F0EB)

/** Whole choreography length, in milliseconds. */
private const val OriTimelineMs = 1700f

/** The unfold itself finishes at 900 ms; the rest is the sheet settling. */
private const val OriUnfoldMs = 900f

/* ------------------------------------------------------------------ */
/* Stepped keyframes: CSS `steps(1, end)` in Kotlin                     */
/* ------------------------------------------------------------------ */

/** Holds each keyframe value until the next one, exactly like `steps(1, end)`. */
private fun stepped(p: Float, stops: List<Pair<Float, Float>>): Float {
    if (stops.isEmpty()) return 0f
    for (i in 0 until stops.size - 1) {
        if (p < stops[i + 1].first) return stops[i].second
    }
    return stops.last().second
}

/* the sheet settles down onto the table */
private val OriSettleScale = listOf(0f to 0.86f, 0.18f to 0.90f, 0.55f to 0.985f, 0.8f to 1.008f, 1f to 1f)
private val OriSettleRotX = listOf(0f to 10f, 0.18f to 7f, 0.55f to 2f, 0.8f to -0.8f, 1f to 0f)
private val OriSettleRotZ = listOf(0f to -1.6f, 0.18f to -1f, 0.55f to 0.5f, 0.8f to -0.3f, 1f to 0f)
private val OriSettleAlpha = listOf(0f to 0f, 0.18f to 1f, 1f to 1f)

/* and folds away again */
private val OriAwayScale = listOf(0f to 1f, 0.35f to 0.97f, 1f to 0.86f)
private val OriAwayRotX = listOf(0f to 0f, 0.35f to 4f, 1f to 14f)
private val OriAwayRotZ = listOf(0f to 0f, 0.35f to 0.6f, 1f to -2f)
private val OriAwayAlpha = listOf(0f to 1f, 0.85f to 1f, 1f to 0f)

/* the note drops onto the sheet */
private val OriTextAlpha = listOf(0f to 0f, 0.35f to 1f, 1f to 1f)
private val OriTextY = listOf(0f to 9f, 0.35f to -2f, 0.68f to 1f, 1f to 0f)
private val OriTextRot = listOf(0f to -1.2f, 0.35f to 0.6f, 0.68f to -0.2f, 1f to 0f)

/* a scrap of paper lands and pops */
private val OriPopAlpha = listOf(0f to 0f, 0.55f to 1f, 1f to 1f)
private val OriPopRot = listOf(0f to -2f, 0.55f to 1f, 1f to 0f)
private val OriIconScale = listOf(0f to 0.6f, 0.55f to 1.08f, 1f to 1f)
private val OriIconRot = listOf(0f to -6f, 0.55f to 2.5f, 1f to 0f)

/** The outline the sheet shows at a given moment of the unfold. */
private fun origamiPose(unfold: Float): Int = when {
    unfold < 0.32f -> 0
    unfold < 0.60f -> 1
    unfold < 0.82f -> 2
    unfold < 1f -> 3
    else -> 4
}

/** The outline while folding back up: the unfold, played backwards. */
private fun origamiRefoldPose(exitMs: Float): Int {
    val p = (exitMs / 700f).coerceIn(0f, 1f)
    return when {
        p < 0.18f -> 4
        p < 0.40f -> 3
        p < 0.68f -> 2
        p < 0.90f -> 1
        else -> 0
    }
}

private fun origamiPath(
    width: Float,
    height: Float,
    pose: Int,
    dx: Float = 0f,
    dy: Float = 0f
): Path {
    val pts = OriSheetPoses[pose]
    return Path().apply {
        moveTo(pts[0] * width + dx, pts[1] * height + dy)
        var i = 2
        while (i < pts.size) {
            lineTo(pts[i] * width + dx, pts[i + 1] * height + dy)
            i += 2
        }
        close()
    }
}

/**
 * How far the sheet itself has travelled, in dp: one jolt as the note lands,
 * then a slow breath. The ink on the sheet never moves with it.
 */
private fun oriSheetShake(enterMs: Float, phase: Float): Pair<Float, Float> {
    val jolt = ((enterMs - 900f) / 700f).coerceIn(0f, 1f)
    val joltPose = shakePose(jolt)
    val joltOn = if (enterMs >= 900f) 1f else 0f
    val breathe = if (enterMs >= 1600f) 1f else 0f
    val breathPose = shakePose(phase)
    val dx = OriShakeDx[joltPose] * joltOn * 2.6f + OriShakeDx[breathPose] * breathe * 0.55f
    val dy = OriShakeDy[joltPose] * joltOn * 2.6f + OriShakeDy[breathPose] * breathe * 0.55f
    return dx to dy
}

/** The five sheet outlines: folded triangle, opening left, opening right, nearly flat, flat. */
private val OriSheetPoses = arrayOf(
    floatArrayOf(0.50000f, 0.04000f, 0.52500f, 0.20000f, 0.56000f, 0.38000f, 0.61000f, 0.56000f, 0.67000f, 0.74000f, 0.73000f, 0.94000f, 0.62000f, 0.94000f, 0.50000f, 0.94000f, 0.38000f, 0.94000f, 0.27000f, 0.94000f, 0.32000f, 0.74000f, 0.39000f, 0.56000f, 0.44000f, 0.38000f),
    floatArrayOf(0.13400f, 0.01000f, 0.37875f, 0.05525f, 0.56000f, 0.38000f, 0.61000f, 0.56000f, 0.67000f, 0.74000f, 0.73000f, 0.94000f, 0.62000f, 0.94000f, 0.50000f, 0.94000f, 0.32000f, 0.98425f, 0.07875f, 0.97750f, 0.08000f, 0.90275f, 0.10425f, 0.63500f, 0.11075f, 0.34250f),
    floatArrayOf(0.23160f, 0.01800f, 0.41775f, 0.09385f, 0.62050f, 0.17155f, 0.81680f, 0.25695f, 0.84655f, 0.52000f, 0.87190f, 0.95815f, 0.80590f, 0.97300f, 0.57150f, 0.96860f, 0.33600f, 0.97245f, 0.12975f, 0.96750f, 0.14400f, 0.85935f, 0.18045f, 0.61500f, 0.19855f, 0.35250f),
    floatArrayOf(0.08520f, 0.00600f, 0.35925f, 0.03595f, 0.65350f, 0.05785f, 0.92960f, 0.09165f, 0.94285f, 0.40000f, 0.94930f, 0.96805f, 0.90730f, 0.99100f, 0.61050f, 0.98420f, 0.31200f, 0.99015f, 0.05325f, 0.98250f, 0.04800f, 0.92445f, 0.06615f, 0.64500f, 0.06685f, 0.33750f),
    floatArrayOf(0.01200f, 0.00000f, 0.33000f, 0.00700f, 0.67000f, 0.00100f, 0.98600f, 0.00900f, 0.99100f, 0.34000f, 0.98800f, 0.97300f, 0.95800f, 1.00000f, 0.63000f, 0.99200f, 0.30000f, 0.99900f, 0.01500f, 0.99000f, 0.00000f, 0.95700f, 0.00900f, 0.66000f, 0.00100f, 0.33000f),
)

/** The faceted paper: one triangle per group, coordinates in thousandths, then the grey. */
private const val OriMeshData =
    "0,0,75,0,73,160,232;0,0,73,160,0,136,233;75,0,141,0,153,138,237;75,0,153,138,73,160,225;" +
    "141,0,220,0,153,138,244;220,0,244,202,153,138,246;220,0,286,0,298,121,239;" +
    "220,0,298,121,244,202,233;286,0,375,0,344,91,243;286,0,344,91,298,121,241;" +
    "375,0,417,0,344,91,233;417,0,423,184,344,91,247;417,0,519,0,527,185,248;" +
    "417,0,527,185,423,184,238;519,0,544,0,554,192,237;519,0,554,192,527,185,233;" +
    "544,0,671,0,554,192,235;671,0,672,131,554,192,241;671,0,721,0,722,176,234;" +
    "671,0,722,176,672,131,236;721,0,757,0,722,176,242;757,0,761,123,722,176,233;" +
    "757,0,831,0,873,97,247;757,0,873,97,761,123,234;831,0,913,0,905,90,246;831,0,905,90,873,97,233;" +
    "913,0,1000,0,1000,104,228;913,0,1000,104,905,90,239;0,136,73,160,85,241,234;" +
    "0,136,85,241,0,279,235;73,160,153,138,85,241,229;153,138,120,276,85,241,235;" +
    "153,138,244,202,200,342,236;153,138,200,342,120,276,228;244,202,298,121,200,342,236;" +
    "298,121,274,332,200,342,243;298,121,344,91,274,332,236;344,91,351,328,274,332,247;" +
    "344,91,423,184,405,344,238;344,91,405,344,351,328,237;423,184,527,185,405,344,226;" +
    "527,185,485,318,405,344,238;527,185,554,192,559,235,240;527,185,559,235,485,318,237;" +
    "554,192,672,131,648,255,234;554,192,648,255,559,235,232;672,131,722,176,648,255,241;" +
    "722,176,707,280,648,255,242;722,176,761,123,707,280,240;761,123,785,295,707,280,224;" +
    "761,123,873,97,838,244,238;761,123,838,244,785,295,233;873,97,905,90,948,256,235;" +
    "873,97,948,256,838,244,250;905,90,1000,104,1000,314,235;905,90,1000,314,948,256,239;" +
    "0,279,85,241,94,441,236;0,279,94,441,0,392,237;85,241,120,276,119,373,244;" +
    "85,241,119,373,94,441,231;120,276,200,342,119,373,233;200,342,199,453,119,373,230;" +
    "200,342,274,332,305,440,247;200,342,305,440,199,453,243;274,332,351,328,305,440,236;" +
    "351,328,338,455,305,440,241;351,328,405,344,338,455,236;405,344,412,436,338,455,246;" +
    "405,344,485,318,412,436,242;485,318,507,402,412,436,247;485,318,559,235,507,402,241;" +
    "559,235,554,371,507,402,234;559,235,648,255,640,376,230;559,235,640,376,554,371,236;" +
    "648,255,707,280,640,376,237;707,280,706,437,640,376,237;707,280,785,295,706,437,237;" +
    "785,295,777,475,706,437,242;785,295,838,244,777,475,244;838,244,867,452,777,475,238;" +
    "838,244,948,256,867,452,237;948,256,907,373,867,452,222;948,256,1000,314,1000,478,230;" +
    "948,256,1000,478,907,373,238;0,392,94,441,0,627,241;94,441,80,569,0,627,246;" +
    "94,441,119,373,80,569,240;119,373,132,631,80,569,240;119,373,199,453,217,600,240;" +
    "119,373,217,600,132,631,232;199,453,305,440,300,596,234;199,453,300,596,217,600,244;" +
    "305,440,338,455,300,596,237;338,455,382,554,300,596,236;338,455,412,436,382,554,237;" +
    "412,436,453,616,382,554,226;412,436,507,402,517,615,224;412,436,517,615,453,616,239;" +
    "507,402,554,371,517,615,234;554,371,579,557,517,615,251;554,371,640,376,649,521,243;" +
    "554,371,649,521,579,557,243;640,376,706,437,744,617,243;640,376,744,617,649,521,240;" +
    "706,437,777,475,744,617,244;777,475,779,600,744,617,230;777,475,867,452,854,612,225;" +
    "777,475,854,612,779,600,231;867,452,907,373,944,515,240;867,452,944,515,854,612,233;" +
    "907,373,1000,478,1000,569,242;907,373,1000,569,944,515,251;0,627,80,569,0,738,229;" +
    "80,569,78,765,0,738,246;80,569,132,631,78,765,230;132,631,114,690,78,765,253;" +
    "132,631,217,600,114,690,246;217,600,196,675,114,690,228;217,600,300,596,196,675,248;" +
    "300,596,295,707,196,675,223;300,596,382,554,347,734,236;300,596,347,734,295,707,229;" +
    "382,554,453,616,424,751,232;382,554,424,751,347,734,240;453,616,517,615,523,700,233;" +
    "453,616,523,700,424,751,238;517,615,579,557,523,700,225;579,557,560,671,523,700,243;" +
    "579,557,649,521,560,671,231;649,521,663,756,560,671,236;649,521,744,617,741,688,244;" +
    "649,521,741,688,663,756,234;744,617,779,600,741,688,241;779,600,783,687,741,688,229;" +
    "779,600,854,612,783,687,223;854,612,852,729,783,687,238;854,612,944,515,852,729,238;" +
    "944,515,917,755,852,729,227;944,515,1000,569,917,755,227;1000,569,1000,709,917,755,242;" +
    "0,738,78,765,44,882,229;0,738,44,882,0,801,239;78,765,114,690,131,892,241;" +
    "78,765,131,892,44,882,249;114,690,196,675,131,892,239;196,675,192,852,131,892,230;" +
    "196,675,295,707,305,826,248;196,675,305,826,192,852,229;295,707,347,734,330,873,232;" +
    "295,707,330,873,305,826,231;347,734,424,751,330,873,243;424,751,436,876,330,873,243;" +
    "424,751,523,700,528,879,245;424,751,528,879,436,876,226;523,700,560,671,528,879,237;" +
    "560,671,570,819,528,879,245;560,671,663,756,570,819,229;663,756,641,883,570,819,236;" +
    "663,756,741,688,701,839,246;663,756,701,839,641,883,231;741,688,783,687,787,871,243;" +
    "741,688,787,871,701,839,229;783,687,852,729,787,871,241;852,729,851,892,787,871,233;" +
    "852,729,917,755,851,892,240;917,755,904,909,851,892,239;917,755,1000,709,904,909,241;" +
    "1000,709,1000,813,904,909,245;0,801,44,882,0,1000,245;44,882,96,1000,0,1000,249;" +
    "44,882,131,892,96,1000,239;131,892,147,1000,96,1000,243;131,892,192,852,232,1000,235;" +
    "131,892,232,1000,147,1000,245;192,852,305,826,284,1000,231;192,852,284,1000,232,1000,240;" +
    "305,826,330,873,339,1000,229;305,826,339,1000,284,1000,243;330,873,436,876,448,1000,228;" +
    "330,873,448,1000,339,1000,244;436,876,528,879,513,1000,225;436,876,513,1000,448,1000,228;" +
    "528,879,570,819,513,1000,234;570,819,595,1000,513,1000,230;570,819,641,883,671,1000,239;" +
    "570,819,671,1000,595,1000,242;641,883,701,839,732,1000,237;641,883,732,1000,671,1000,242;" +
    "701,839,787,871,732,1000,244;787,871,810,1000,732,1000,226;787,871,851,892,810,1000,241;" +
    "851,892,848,1000,810,1000,234;851,892,904,909,925,1000,246;851,892,925,1000,848,1000,225;" +
    "904,909,1000,813,1000,1000,238;904,909,1000,1000,925,1000,241"

/* ------------------------------------------------------------------ */
/* The faceted paper                                                    */
/* ------------------------------------------------------------------ */

/** One path per grey level, in unit coordinates. */
private val OriMeshLayers: List<Pair<Color, Path>> by lazy {
    val groups = LinkedHashMap<Int, Path>()
    OriMeshData.split(';').forEach { tri ->
        val p = tri.split(',')
        if (p.size < 7) return@forEach
        val grey = (p[6].toInt() / 16) * 16
        val path = groups.getOrPut(grey) { Path() }
        path.moveTo(p[0].toFloat() / 1000f, p[1].toFloat() / 1000f)
        path.lineTo(p[2].toFloat() / 1000f, p[3].toFloat() / 1000f)
        path.lineTo(p[4].toFloat() / 1000f, p[5].toFloat() / 1000f)
        path.close()
    }
    groups.map { (grey, path) ->
        val c = grey.coerceIn(0, 255)
        Color(0xFF000000.toInt() or (c shl 16) or (c shl 8) or c) to path
    }
}

/* ------------------------------------------------------------------ */
/* Stop-motion shake                                                    */
/* ------------------------------------------------------------------ */

private val OriShakeDx = floatArrayOf(0f, -1.1f, 1.1f, -0.9f, 1f)
private val OriShakeDy = floatArrayOf(0f, 0.7f, -0.7f, -0.9f, 0.8f)
private val OriShakeRot = floatArrayOf(0f, -1.1f, 1f, -0.8f, 0.7f)

/** An endless phase so every piece can shiver on its own clock. */
@Composable
private fun rememberShakePhase(periodMs: Int, offset: Float): State<Float> {
    val speed = rememberAnimationSpeed()
    val transition = rememberInfiniteTransition(label = "ori-shake")
    return transition.animateFloat(
        initialValue = offset,
        targetValue = offset + 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(scaleDuration(periodMs, speed), easing = LinearEasing)
        ),
        label = "ori-phase"
    )
}

/** The five stop-motion poses of a shivering scrap of paper. */
private fun shakePose(phase: Float): Int {
    val p = ((phase % 1f) + 1f) % 1f
    return (p * 5f).toInt().coerceIn(0, 4)
}

/* ==================================================================== */
/* Card                                                                 */
/* ==================================================================== */

@Composable
fun CuratedOrigamiTicker(
    data: TickerData,
    interactions: TickerInteractions?,
    animateIn: Boolean,
    exitFraction: () -> Float
) {
    val context = LocalContext.current
    val opts = Prefs.themeOptions(context, "origami", 0xFF14161A.toInt())
    val lines = opts.lines
    val intensity = opts.intensity
    val showTime = opts.showTime
    val showLogo = opts.showLogo
    val logoSize = opts.avatarSize.dp
    val scale = opts.buttonScale / 100f

    val hasDock = data.actions.isNotEmpty()
    val ex = exitFraction().coerceIn(0f, 1f)
    val leaving = ex > 0f

    // One clock for the whole choreography.
    val speed = rememberAnimationSpeed()
    val enter = remember { Animatable(if (animateIn) 0f else 1f) }
    LaunchedEffect(animateIn) {
        if (!animateIn) {
            enter.snapTo(1f)
            return@LaunchedEffect
        }
        enter.snapTo(0f)
        enter.animateTo(1f, tween(scaleDuration(OriTimelineMs.toInt(), speed), easing = LinearEasing))
    }

    // Every piece shivers on a clock of its own.
    val paperPhase = rememberShakePhase(2400, 0f)
    val iconPhase = rememberShakePhase(1700, 0f)
    val buttonPhases = listOf(
        rememberShakePhase(2100, 0.37f),
        rememberShakePhase(2700, 0.71f),
        rememberShakePhase(2300, 0.19f)
    )

    CardFrame(
        modifier = Modifier
            .origamiLayers(enter, ex, leaving)
            .origamiSheetClip(enter, exitFraction, leaving, paperPhase),
        lines = lines,
        hasDock = hasDock,
        // One line of text plus the dock; every extra line adds 15dp.
        baseHeight = 126.dp,
        dockSpace = 44.dp,
        decorationAlpha = intensity / 100f,
        shape = RoundedCornerShape(0.dp),
        background = Brush.linearGradient(listOf(Color.Transparent, Color.Transparent)),
        border = null
    ) {
        Box(Modifier.fillMaxSize()) {
            // The paper itself: base stock, facets, grain, fold. It is the only
            // thing that shivers — the ink printed on it stays put.
            Canvas(Modifier.fillMaxSize()) {
                val enterMs = enter.value * OriTimelineMs
                val exitMs = ex * 700f
                val (shakeX, shakeY) = if (leaving) {
                    0f to 0f
                } else {
                    oriSheetShake(enterMs, paperPhase.value)
                }

                translate(shakeX * density, shakeY * density) {

                drawRect(
                    Brush.linearGradient(
                        listOf(OriPaperTop, OriPaperBottom),
                        start = Offset.Zero,
                        end = Offset(size.width * 0.35f, size.height)
                    )
                )

                // Low-poly facets.
                val meshAlpha = (intensity / 100f).coerceIn(0f, 1f)
                if (meshAlpha > 0.01f) {
                    scale(size.width, size.height, pivot = Offset.Zero) {
                        OriMeshLayers.forEach { (color, path) ->
                            drawPath(path, color.copy(alpha = meshAlpha))
                        }
                    }
                }

                // Soft light on the paper.
                drawRect(
                    Brush.radialGradient(
                        listOf(Color.White.copy(alpha = 0.72f), Color.Transparent),
                        center = Offset(size.width * 0.24f, size.height * 0.12f),
                        radius = size.width * 0.48f
                    )
                )
                drawRect(
                    Brush.radialGradient(
                        listOf(Color.Black.copy(alpha = 0.05f), Color.Transparent),
                        center = Offset(size.width * 0.86f, size.height * 0.90f),
                        radius = size.width * 0.50f
                    )
                )
                drawRect(
                    Brush.verticalGradient(
                        listOf(Color.White.copy(alpha = 0.34f), Color.White.copy(alpha = 0.14f))
                    )
                )

                // The folded packet, until the sheet opens.
                val fold = if (leaving) {
                    ((exitMs - 500f) / 200f).coerceIn(0f, 1f)
                } else {
                    (1f - (enterMs - 110f) / 500f).coerceIn(0f, 1f)
                }
                if (fold > 0.01f) {
                    clipPath(origamiPath(size.width, size.height, 0)) {
                        drawRect(
                            Brush.linearGradient(
                                listOf(
                                    Color.Black.copy(alpha = 0.10f * fold),
                                    Color.White.copy(alpha = 0.30f * fold),
                                    Color.Black.copy(alpha = 0.12f * fold)
                                ),
                                start = Offset.Zero,
                                end = Offset(size.width, size.height)
                            )
                        )
                        drawLine(
                            color = Color.Black.copy(alpha = 0.07f * fold),
                            start = Offset(size.width / 2f, 0f),
                            end = Offset(size.width / 2f, size.height),
                            strokeWidth = 0.6.dp.toPx()
                        )
                    }
                }
                }
            }

            // The note, dropped onto the sheet.
            Column(
                Modifier
                    .align(Alignment.TopStart)
                    .padding(start = 22.dp, end = 22.dp, top = 18.dp)
                    .fillMaxWidth()
                    .graphicsLayer {
                        val enterMs = enter.value * OriTimelineMs
                        val local = ((enterMs - 550f) / 420f).coerceIn(0f, 1f)
                        alpha = if (leaving) {
                            (1f - (ex * 700f) / 420f).coerceIn(0f, 1f)
                        } else {
                            stepped(local, OriTextAlpha)
                        }
                        translationY = if (leaving) {
                            -(ex * 700f) / 420f * 9f * density
                        } else {
                            stepped(local, OriTextY) * density
                        }
                        rotationZ = if (leaving) {
                            -(ex * 700f) / 420f * 1.2f
                        } else {
                            stepped(local, OriTextRot)
                        }
                    }
            ) {
                OriSenderRow(
                    data = data,
                    showTime = showTime,
                    showLogo = showLogo,
                    logoSize = logoSize,
                    iconPhase = iconPhase,
                    enter = enter,
                    ex = ex,
                    leaving = leaving
                )
                Spacer(Modifier.height(5.dp))
                CardBody(
                    text = data.body,
                    lines = lines,
                    color = OriInkSoft,
                    font = ThemeFonts.Nunito,
                    weight = FontWeight.Bold,
                    sizeSp = 11.5f,
                    lineHeightFactor = 1.3f
                )
            }

            if (hasDock) {
                OriPaperDock(
                    data = data,
                    interactions = interactions,
                    scale = scale,
                    enter = enter,
                    phases = buttonPhases,
                    ex = ex,
                    leaving = leaving,
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(start = 22.dp, end = 22.dp, bottom = 16.dp)
                )
            }
        }
    }
}

/* ------------------------------------------------------------------ */
/* The sheet: outline, shadow and the fold                             */
/* ------------------------------------------------------------------ */

/** The settle on the way in and the fold-away on the way out. */
private fun Modifier.origamiLayers(
    enter: Animatable<Float, *>,
    ex: Float,
    leaving: Boolean
): Modifier = graphicsLayer {
    val enterMs = enter.value * OriTimelineMs
    val exitMs = ex * 700f
    cameraDistance = 14f * density

    if (leaving) {
        val p = (exitMs / 700f).coerceIn(0f, 1f)
        scaleX = stepped(p, OriAwayScale)
        scaleY = scaleX
        rotationX = stepped(p, OriAwayRotX)
        rotationZ = stepped(p, OriAwayRotZ)
        alpha = stepped(p, OriAwayAlpha)
    } else {
        val p = (enterMs / OriUnfoldMs).coerceIn(0f, 1f)
        scaleX = stepped(p, OriSettleScale)
        scaleY = scaleX
        rotationX = stepped(p, OriSettleRotX)
        rotationZ = stepped(p, OriSettleRotZ)
        alpha = stepped(p, OriSettleAlpha)
    }
}

/** Clips the card to the sheet outline and drops its shadow behind it. */
private fun Modifier.origamiSheetClip(
    enter: Animatable<Float, *>,
    exitFraction: () -> Float,
    leaving: Boolean,
    paperPhase: State<Float>
): Modifier = drawWithContent {
    val enterMs = enter.value * OriTimelineMs
    val exitMs = exitFraction().coerceIn(0f, 1f) * 700f
    val pose = if (leaving) {
        origamiRefoldPose(exitMs)
    } else {
        origamiPose((enterMs / OriUnfoldMs).coerceIn(0f, 1f))
    }
    // Only the sheet moves; the ink stays put on top of it.
    val (shakeX, shakeY) = if (leaving) {
        0f to 0f
    } else {
        oriSheetShake(enterMs, paperPhase.value)
    }
    val path = origamiPath(
        size.width,
        size.height,
        pose,
        shakeX * density,
        shakeY * density
    )

    // A soft shadow that follows the outline.
    val lift = 9.dp.toPx()
    for (i in 4 downTo 1) {
        val grow = i * 1.7f * density
        scale(size.width + grow * 2f, size.height + grow * 2f, pivot = Offset(size.width / 2f, size.height / 2f)) {
            translate(0f, lift) {
                drawPath(path, Color.Black.copy(alpha = 0.035f))
            }
        }
    }

    clipPath(path) {
        this@drawWithContent.drawContent()
    }
}

/* ------------------------------------------------------------------ */
/* The note                                                             */
/* ------------------------------------------------------------------ */

@Composable
private fun OriSenderRow(
    data: TickerData,
    showTime: Boolean,
    showLogo: Boolean,
    logoSize: Dp,
    iconPhase: State<Float>,
    enter: Animatable<Float, *>,
    ex: Float,
    leaving: Boolean
) {
    val badgeSize = logoSize.coerceIn(26.dp, 36.dp)
    Row(
        Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (showLogo) {
            Box(
                Modifier
                    .size(badgeSize)
                    .graphicsLayer {
                        val enterMs = enter.value * OriTimelineMs
                        val local = ((enterMs - 520f) / 380f).coerceIn(0f, 1f)
                        val pose = shakePose(iconPhase.value)
                        val gate = if (enterMs >= 1450f) 1f else 0f
                        translationX = OriShakeDx[pose] * gate * density
                        translationY = OriShakeDy[pose] * gate * density
                        var s = stepped(local, OriIconScale)
                        var rot = stepped(local, OriIconRot)
                        var a = 1f
                        if (leaving) {
                            // the icon folds back in, exactly as it popped out
                            val out = ((ex * 700f) / 300f).coerceIn(0f, 1f)
                            s = 1f - 0.4f * out
                            rot = -6f * out
                            a = 1f - out
                        }
                        rotationZ = OriShakeRot[pose] * gate + rot
                        scaleX = s
                        scaleY = s
                        alpha = a
                    }
                    .clip(RoundedCornerShape(7.dp))
                    .background(Color.White)
                    .border(1.dp, OriInk, RoundedCornerShape(7.dp)),
                contentAlignment = Alignment.Center
            ) {
                OriAppBadge(data = data, size = badgeSize - 5.dp)
            }
            Spacer(Modifier.width(9.dp))
        }
        Text(
            data.title,
            color = OriInk,
            fontSize = 18.sp,
            fontFamily = ThemeFonts.FredokaBold,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 0.3.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
        if (showTime) {
            Spacer(Modifier.width(10.dp))
            Text(
                data.time.uppercase(),
                color = OriInk,
                fontSize = 9.sp,
                fontFamily = ThemeFonts.Nunito,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 0.5.sp,
                maxLines = 1
            )
        }
    }
}

/** The app icon, sized to fill its little paper square. */
@Composable
private fun OriAppBadge(data: TickerData, size: Dp) {
    val bitmap = data.icon
    if (bitmap != null) {
        Image(
            bitmap = bitmap.asImageBitmap(),
            contentDescription = null,
            contentScale = ContentScale.Fit,
            modifier = Modifier.fillMaxSize()
        )
    } else {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(
                data.appName.take(1).uppercase(),
                color = OriInk,
                fontSize = (size.value * 0.44f).sp,
                fontWeight = FontWeight.Black,
                textAlign = TextAlign.Center
            )
        }
    }
}

/* ------------------------------------------------------------------ */
/* The dock: little scraps of paper lying on the sheet                 */
/* ------------------------------------------------------------------ */

@Composable
private fun BoxScope.OriPaperDock(
    data: TickerData,
    interactions: TickerInteractions?,
    scale: Float,
    enter: Animatable<Float, *>,
    phases: List<State<Float>>,
    ex: Float,
    leaving: Boolean,
    modifier: Modifier = Modifier
) {
    val specs = notificationActionSpecs(data, interactions)
    if (specs.isEmpty()) return

    Row(
        modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        specs.forEachIndexed { index, spec ->
            val source = remember { MutableInteractionSource() }
            val pressed by source.collectIsPressedAsState()
            val phase = phases[index % phases.size]
            val tilt = if (index % 2 == 0) -1.1f else 0.9f
            val appearAt = 660f + index * 80f

            Box(
                Modifier
                    .weight(1f)
                    .height(32.dp)
                    .graphicsLayer {
                        val enterMs = enter.value * OriTimelineMs
                        val local = ((enterMs - appearAt) / 380f).coerceIn(0f, 1f)
                        val pose = shakePose(phase.value)
                        val gate = if (enterMs >= appearAt + 840f) 1f else 0f
                        translationX = OriShakeDx[pose] * gate * density
                        translationY = OriShakeDy[pose] * gate * density
                        var rot = tilt + OriShakeRot[pose] * gate + stepped(local, OriPopRot)
                        var s = if (pressed) 0.97f else 1f
                        var a = stepped(local, OriPopAlpha)
                        if (leaving) {
                            // each scrap folds back in, in reverse of its pop
                            val out = ((ex * 700f) / 260f).coerceIn(0f, 1f)
                            rot = tilt - 2f * out
                            s = 1f - 0.28f * out
                            a = 1f - out
                        }
                        rotationZ = rot
                        scaleX = s
                        scaleY = s
                        alpha = a
                    }
                    .shadow(6.dp, RoundedCornerShape(2.dp), clip = false)
                    .clip(RoundedCornerShape(2.dp))
                    .background(
                        if (index % 2 == 0) {
                            Brush.linearGradient(listOf(Color(0xFFFFFFFF), Color(0xFFEFECE4)))
                        } else {
                            Brush.linearGradient(listOf(Color(0xFFF8F6F0), Color(0xFFE7E3D9)))
                        }
                    )
                    .drawBehind { drawDogEar() }
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
                        tint = OriInk,
                        modifier = Modifier.size(15.dp * scale)
                    )
                } else {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            spec.icon,
                            contentDescription = null,
                            tint = OriInk,
                            modifier = Modifier.size(12.dp * scale)
                        )
                        Spacer(Modifier.width(5.dp))
                        Text(
                            spec.label.uppercase(),
                            color = OriInk,
                            fontSize = (10.5f * scale).sp,
                            fontFamily = ThemeFonts.Nunito,
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

/** The folded-over corner of a paper scrap. */
private fun DrawScope.drawDogEar() {
    val s = 8.dp.toPx()
    val path = Path().apply {
        moveTo(size.width, 0f)
        lineTo(size.width, s)
        lineTo(size.width - s, 0f)
        close()
    }
    drawPath(path, Color(0x14000000))
    drawLine(
        color = Color.White.copy(alpha = 0.9f),
        start = Offset(size.width - s, 0f),
        end = Offset(size.width, s),
        strokeWidth = 1.dp.toPx()
    )
}
