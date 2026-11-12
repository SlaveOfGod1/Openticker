package com.openticker.ticker.cards

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
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

/* ==================================================================== */
/* 09 — Stained Glass                                                   */
/* ==================================================================== */
/*
 * One thick pane of leaded glass. 182 irregular jewel panes are set
 * into black came, nearest the sun first, and the note is sandblasted
 * into the surface. Leaving, the glass flares and dissolves so there
 * is no black rectangle left behind — just the crystals on the screen.
 */

private val GlassInk = Color(0xFFFFFFFF)
private const val GlassInkArgb = 0xFFFFFFFF.toInt()

/** The whole choreography, in milliseconds. */
private const val GlassTimelineMs = 1700f

/** The way out is a little quicker. */
private const val GlassExitMs = 900f

/** How long one pane takes to be set, or to be taken out. */
private const val GlassPaneMs = 500f

/** The halo that keeps the lettering legible over busy glass. */
private val GlassHalo = Shadow(Color(0xF00A0813), Offset(0f, 1f), 2.5f)

private val GlassHaloBright = Shadow(Color(0xF00A0813), Offset(0f, 1.5f), 3f)

/** Every pane of the window: six coordinates in thousandths, the colour, then the
 *  glaze delay and the dissolve delay, in milliseconds. */
private const val GlassPaneData =
    "1,4,67,4,1,174,143B6D,568,232;68,9,71,103,4,174,2C69B4,553,247;" +
    "71,2,134,2,75,99,3174C8,528,272;136,6,131,163,76,103,3277CD,515,285;" +
    "141,4,208,4,209,153,2E6EBC,475,325;139,9,206,156,135,163,3073C4,489,311;" +
    "218,3,328,3,312,86,3072C4,419,381;213,7,309,92,213,154,3071C2,441,359;" +
    "336,2,388,2,400,86,3681DD,369,431;335,6,397,87,319,89,347BD2,381,419;" +
    "391,2,470,2,403,86,3783E1,342,458;471,8,481,174,407,90,367FDA,322,478;" +
    "476,6,537,6,486,169,357DD6,296,504;539,12,520,163,487,175,357FD9,287,513;" +
    "543,6,602,6,525,156,3784E2,264,536;607,6,627,99,526,163,D44271,246,554;" +
    "615,3,703,3,700,93,E9497D,201,599;611,4,699,98,631,98,DA4475,213,587;" +
    "709,4,769,4,706,92,FFFCDE,171,629;772,8,791,158,708,99,E04677,149,651;" +
    "778,2,862,2,873,88,F24C81,124,676;779,10,869,91,797,155,F84D84,117,683;" +
    "866,3,931,3,911,150,E4477A,123,677;865,5,910,152,876,91,F44C82,106,694;" +
    "937,5,998,5,998,146,E9497C,161,639;935,11,994,150,916,151,E14678,140,660;" +
    "2,181,69,111,2,243,133868,568,232;72,116,97,315,6,247,2E6DBB,551,249;" +
    "76,114,129,172,101,311,2D6BB8,525,275;132,176,140,245,103,317,3071C2,515,285;" +
    "134,170,207,162,143,242,3277CD,491,309;209,168,236,270,148,243,3072C4,472,328;" +
    "215,162,309,101,243,265,3174C7,437,363;313,105,315,278,246,269,357ED8,419,381;" +
    "318,95,400,93,377,225,347CD4,373,427;317,101,375,229,319,278,347BD4,392,408;" +
    "404,96,479,181,382,226,357ED8,342,458;478,187,438,275,384,231,357DD6,340,460;" +
    "488,185,521,173,549,270,FDF1D4,291,509;485,188,547,274,444,278,347DD6,310,490;" +
    "526,170,627,106,628,283,3A8BEE,247,553;525,175,626,287,554,275,3988E9,269,531;" +
    "635,107,701,106,713,290,3987E8,196,604;633,113,710,296,633,284,3B8CF0,220,580;" +
    "706,107,790,166,717,294,FFFCDE,171,629;791,172,758,244,720,297,ED4A7F,176,624;" +
    "799,164,870,100,821,225,EB497E,124,676;794,169,817,231,761,242,E6487B,155,645;" +
    "879,100,912,159,943,318,E9497C,141,659;876,101,941,320,824,232,E4477A,145,655;" +
    "916,160,998,159,998,307,DD4576,170,630;917,169,994,309,946,321,E9497C,182,618;" +
    "3,255,95,325,3,379,E2D8BE,572,228;97,329,84,384,6,382,2E6EBC,561,239;" +
    "105,327,141,255,174,435,2F70BF,515,285;103,332,171,439,91,386,E6DCC1,531,269;" +
    "146,252,236,280,179,435,3072C4,487,313;240,284,244,437,181,442,EBE1C5,477,323;" +
    "245,280,315,289,301,459,337AD1,435,365;242,282,300,464,247,438,3175C9,456,344;" +
    "319,289,374,240,305,456,3174C6,408,392;377,246,408,395,310,457,347CD5,398,402;" +
    "382,238,438,284,455,445,3378CE,358,442;381,239,455,449,413,397,3782DF,370,430;" +
    "444,287,545,284,460,442,3782DF,331,469;551,284,548,416,461,451,FAEED2,323,477;" +
    "555,282,625,294,552,410,347CD5,282,518;628,297,611,477,553,417,357ED8,292,508;" +
    "633,297,709,309,616,470,3988E9,257,543;713,309,678,395,616,476,DA4475,263,537;" +
    "716,304,754,251,682,388,FFF8DA,217,583;757,254,777,398,684,391,E24779,222,578;" +
    "761,249,816,238,781,393,E14678,188,612;820,247,865,401,786,397,EB497E,205,595;" +
    "826,243,938,328,871,400,E9497D,193,607;943,333,947,412,875,407,25B0CA,224,576;" +
    "947,330,998,317,950,411,26B6D1,220,580;998,322,998,466,952,414,25B1CC,244,556;" +
    "5,389,84,391,88,530,133969,572,228;4,398,84,535,4,589,123665,598,202;" +
    "90,394,171,447,94,530,3073C5,543,257;173,452,133,516,96,535,2F70C0,543,257;" +
    "179,449,239,444,139,512,3173C6,510,290;243,446,210,586,137,518,2F6FBF,513,287;" +
    "248,444,301,469,281,537,3072C4,466,334;246,447,278,540,214,586,3379D0,490,310;" +
    "310,468,408,407,386,525,3379CF,417,383;304,471,385,531,284,540,3379D0,449,351;" +
    "413,403,455,456,391,527,357FDA,391,409;455,461,449,537,393,530,3173C6,397,403;" +
    "465,457,546,424,546,521,347BD4,347,453;461,461,543,526,455,537,357ED8,373,427;" +
    "552,424,608,481,551,523,D64372,328,472;612,486,643,579,553,528,CF416F,335,465;" +
    "615,482,676,403,646,575,D1416F,305,495;678,405,703,573,648,578,DE4577,308,492;" +
    "683,401,778,408,767,614,D84373,274,526;681,406,765,618,706,574,DE4576,304,496;" +
    "784,407,869,413,869,600,DD4576,261,539;783,416,865,603,772,618,DE4576,296,504;" +
    "873,416,944,421,873,595,24ADC7,263,537;945,427,895,577,874,601,25B2CD,290,510;" +
    "949,420,996,474,898,575,24ACC6,275,525;997,480,997,617,900,580,FBF0D3,308,492;" +
    "5,602,88,546,76,722,5A227E,607,193;2,608,73,727,2,726,56217A,636,164;" +
    "92,543,131,524,80,720,2B67B1,579,221;131,535,132,731,82,724,3072C4,587,213;" +
    "135,526,207,593,136,730,3174C8,554,246;210,596,244,665,139,734,B93A63,547,253;" +
    "216,593,280,548,305,716,B63961,503,297;216,598,302,718,249,665,B73962,519,281;" +
    "285,547,383,538,310,714,3175C9,470,330;385,547,374,727,314,718,B93A63,475,325;" +
    "390,538,447,546,378,724,CC406D,436,364;450,555,482,752,382,729,C33D68,447,353;" +
    "454,545,546,533,488,751,CF416F,402,398;547,538,526,673,490,752,D44271,408,392;" +
    "557,538,642,586,634,713,F6EBCE,364,436;551,535,633,722,530,672,C63E6A,388,412;" +
    "647,587,702,582,639,719,CA3F6C,356,444;704,585,711,752,640,724,219DB5,376,424;" +
    "709,583,767,627,755,717,23A8C2,346,454;707,586,753,721,714,751,23A7C0,368,432;" +
    "775,629,866,614,833,739,22A4BD,347,453;771,634,828,742,759,721,219EB6,367,433;" +
    "870,611,892,587,837,740,23A6BF,339,461;894,588,932,684,838,743,23A6BF,351,449;" +
    "901,587,997,624,997,707,24ADC8,343,457;899,589,994,709,936,682,219FB7,350,450;" +
    "5,733,74,735,59,814,59227D,644,156;2,739,56,819,2,861,56217A,667,133;" +
    "79,735,129,743,64,814,59227E,623,177;132,745,149,806,66,818,57217B,619,181;" +
    "139,740,241,673,155,801,562179,576,224;246,673,229,900,156,807,E2D8BD,577,223;" +
    "252,671,307,726,325,800,B73962,523,277;250,678,322,805,234,896,AE365D,552,248;" +
    "313,730,374,739,401,872,BC3B65,511,289;312,733,399,874,330,806,E7DCC2,525,275;" +
    "381,741,481,764,407,872,C73E6A,493,307;484,767,472,804,410,875,1F96AC,490,310;" +
    "488,760,524,680,476,799,CB3F6D,451,349;526,680,552,819,477,801,BE3B65,451,349;" +
    "532,679,635,728,632,831,C03C67,420,380;531,686,627,831,556,818,C53E69,440,360;" +
    "639,730,709,758,636,832,F1E6CA,418,382;709,761,683,818,638,834,23A5BE,428,372;" +
    "716,757,755,728,789,821,209AB2,401,399;714,760,786,823,688,818,22A0B9,418,382;" +
    "758,727,831,749,792,821,22A2BA,396,404;834,752,867,856,794,824,FAAE37,415,385;" +
    "838,750,932,690,901,867,FFB238,396,404;837,754,899,871,869,856,FFB93A,422,378;" +
    "936,691,995,718,906,864,FFB93A,394,406;997,724,997,857,908,869,F3AA35,423,377;" +
    "3,870,59,826,47,993,521F74,684,116;2,878,44,995,2,995,562179,710,90;" +
    "62,824,148,813,50,992,521F74,656,144;149,822,168,993,56,993,562179,657,143;" +
    "156,815,228,907,216,993,58217C,616,184;156,823,213,993,175,993,59227D,634,166;" +
    "235,907,323,816,278,990,57217A,585,215;232,913,275,997,219,997,59227E,621,179;" +
    "332,814,402,883,399,991,B73962,546,254;328,815,397,996,282,996,C03C67,575,225;" +
    "409,883,472,812,473,990,2099B0,521,279;407,889,471,996,403,996,1F93A9,554,246;" +
    "479,810,551,827,530,988,219BB3,493,307;477,818,527,992,478,992,219BB2,524,276;" +
    "558,827,631,841,596,991,1E91A7,479,321;555,835,592,994,535,994,E8DEC3,510,290;" +
    "637,841,683,826,692,990,22A2BB,464,336;635,847,689,995,600,995,209AB1,496,304;" +
    "687,825,786,830,696,991,2099B1,455,345;789,834,773,996,698,996,F3AA35,479,321;" +
    "794,832,863,862,778,990,F1A835,454,346;867,864,856,997,778,997,FBAF37,480,320;" +
    "870,863,899,878,859,993,FFB238,460,340;901,881,920,997,861,997,F2A935,482,318;" +
    "904,878,996,865,924,993,F1A835,463,337;998,870,998,996,927,996,EFA634,485,315"

/* ------------------------------------------------------------------ */
/* The panes                                                            */
/* ------------------------------------------------------------------ */

private class GlassPane(
    val path: Path,
    val color: Color,
    val bright: Color,
    val delayIn: Int,
    val delayOut: Int
)

private val GlassPanes: List<GlassPane> by lazy {
    GlassPaneData.split(';').mapNotNull { raw ->
        val p = raw.split(',')
        if (p.size < 9) return@mapNotNull null
        fun f(i: Int) = p[i].toFloat() / 1000f
        val path = Path().apply {
            moveTo(f(0), f(1))
            lineTo(f(2), f(3))
            lineTo(f(4), f(5))
            close()
        }
        val color = Color(0xFF000000.toInt() or p[6].toInt(16))
        val bright = Color(
            red = (color.red * 1.9f).coerceAtMost(1f),
            green = (color.green * 1.9f).coerceAtMost(1f),
            blue = (color.blue * 1.9f).coerceAtMost(1f),
            alpha = 1f
        )
        GlassPane(
            path = path,
            color = color,
            bright = bright,
            delayIn = p[7].toInt(),
            delayOut = p[8].toInt()
        )
    }
}

/* ------------------------------------------------------------------ */
/* Stepped keyframes                                                   */
/* ------------------------------------------------------------------ */

private fun stepped(p: Float, stops: List<Pair<Float, Float>>): Float {
    if (stops.isEmpty()) return 0f
    for (i in 0 until stops.size - 1) {
        if (p < stops[i + 1].first) return stops[i].second
    }
    return stops.last().second
}

/* ==================================================================== */
/* Card                                                                 */
/* ==================================================================== */

@Composable
fun CuratedGlassTicker(
    data: TickerData,
    interactions: TickerInteractions?,
    animateIn: Boolean,
    exitFraction: () -> Float
) {
    val context = LocalContext.current
    val opts = Prefs.themeOptions(
        context = context,
        id = "glass",
        defaultAccent = GlassInkArgb,
        keepAnyAccent = true
    )
    // The old glass design was gold; that leftover reads as "unset".
    val ink = if (opts.accentArgb == 0xFFD9B45B.toInt()) GlassInk else Color(opts.accentArgb)
    val lines = opts.lines
    val intensity = opts.intensity
    val showTime = opts.showTime
    val showLogo = opts.showLogo
    val logoSize = opts.avatarSize.dp
    val scale = opts.buttonScale / 100f

    val hasDock = data.actions.isNotEmpty()
    val ex = exitFraction().coerceIn(0f, 1f)
    val leaving = ex > 0f

    val speed = rememberAnimationSpeed()
    val enter = remember { Animatable(if (animateIn) 0f else 1f) }
    LaunchedEffect(animateIn) {
        if (!animateIn) {
            enter.snapTo(1f)
            return@LaunchedEffect
        }
        enter.snapTo(0f)
        enter.animateTo(1f, tween(scaleDuration(GlassTimelineMs.toInt(), speed), easing = LinearEasing))
    }

    CardFrame(
        modifier = Modifier.glassLayers(enter, ex, leaving),
        lines = lines,
        hasDock = hasDock,
        // One line of text plus the dock; every extra line adds 18dp.
        baseHeight = 133.dp,
        dockSpace = 49.dp,
        decorationAlpha = intensity / 100f,
        shape = RoundedCornerShape(4.dp),
        background = Brush.linearGradient(listOf(Color.Transparent, Color.Transparent)),
        border = null
    ) {
        Box(Modifier.fillMaxSize()) {
            // The window itself.
            Canvas(Modifier.fillMaxSize()) {
                val enterMs = enter.value * GlassTimelineMs
                val exitMs = ex * GlassExitMs

                // The glass.
                scale(size.width, size.height, pivot = Offset.Zero) {
                    GlassPanes.forEach { pane ->
                        val local = if (leaving) {
                            ((exitMs - pane.delayOut) / GlassPaneMs).coerceIn(0f, 1f)
                        } else {
                            ((enterMs - pane.delayIn) / GlassPaneMs).coerceIn(0f, 1f)
                        }
                        when {
                            leaving -> if (local < 1f) drawPath(pane.path, pane.color)
                            local >= 1f -> drawPath(pane.path, pane.color)
                            local >= 0.55f -> drawPath(pane.path, pane.bright)
                        }
                    }
                }

                // The light lying on the surface of the glass.
                val sheen = if (leaving) {
                    (1f - exitMs / 450f).coerceIn(0f, 1f)
                } else {
                    ((enterMs - 500f) / 800f).coerceIn(0f, 1f)
                }
                if (sheen > 0.01f) {
                    val brush = Brush.linearGradient(
                        listOf(
                            Color.White.copy(alpha = 0.30f * sheen),
                            Color.White.copy(alpha = 0.07f * sheen),
                            Color.Transparent
                        ),
                        start = Offset.Zero,
                        end = Offset(size.width * 0.75f, size.height)
                    )
                    scale(size.width, size.height, pivot = Offset.Zero) {
                        GlassPanes.forEach { drawPath(it.path, brush) }
                    }
                }

                // The sun burning through the far corner.
                val bloom = if (leaving) {
                    ((1f - exitMs / 600f) * 0.9f).coerceIn(0f, 1f)
                } else {
                    val local = ((enterMs - 300f) / 950f).coerceIn(0f, 1f)
                    stepped(local, listOf(0f to 0f, 0.55f to 1f, 1f to 0.9f))
                }
                if (bloom > 0.01f) {
                    val sun = Offset(size.width * 0.88f, size.height * 0.04f)
                    drawRect(
                        Brush.radialGradient(
                            listOf(
                                Color(0xE6FFFAE8).copy(alpha = 0.9f * bloom),
                                Color(0x57FFDA8A).copy(alpha = 0.34f * bloom),
                                Color.Transparent
                            ),
                            center = sun,
                            radius = size.width * 0.32f
                        )
                    )
                    drawRect(
                        Brush.radialGradient(
                            listOf(
                                Color(0x38FFE2A8).copy(alpha = 0.26f * bloom),
                                Color.Transparent
                            ),
                            center = sun,
                            radius = size.width * 0.62f
                        )
                    )
                }

                // A shaft of daylight sweeping across.
                val rayLocal = if (leaving) {
                    (exitMs / 500f).coerceIn(0f, 1f)
                } else {
                    ((enterMs - 400f) / 1200f).coerceIn(0f, 1f)
                }
                val rayAlpha = if (leaving) {
                    ((1f - rayLocal) * 0.45f).coerceIn(0f, 1f)
                } else if (rayLocal < 0.15f || rayLocal > 0.85f) {
                    0f
                } else {
                    1f
                }
                if (rayAlpha > 0.01f) {
                    val travel = if (leaving) {
                        0.40f - rayLocal * 1.0f
                    } else {
                        -0.60f + rayLocal * 1.2f
                    }
                    val band = size.width * (0.5f + travel)
                    rotate(-12f, pivot = Offset(size.width / 2f, size.height / 2f)) {
                        drawRect(
                            Brush.horizontalGradient(
                                listOf(
                                    Color.Transparent,
                                    Color(0x4DFFFCEC).copy(alpha = rayAlpha),
                                    Color.Transparent
                                ),
                                startX = band - size.width * 0.18f,
                                endX = band + size.width * 0.18f
                            )
                        )
                    }
                }

                // The pane has thickness: a lit top edge, a dark bottom edge.
                val edge = if (leaving) {
                    (1f - exitMs / 600f).coerceIn(0f, 1f)
                } else {
                    1f
                }
                if (edge > 0.01f) {
                    drawRect(
                        Brush.verticalGradient(
                            listOf(Color.White.copy(alpha = 0.42f * edge), Color.Transparent),
                            startY = 0f,
                            endY = 4f
                        ),
                        topLeft = Offset.Zero,
                        size = Size(size.width, 4f)
                    )
                    drawRect(
                        Brush.verticalGradient(
                            listOf(Color.Transparent, Color.Black.copy(alpha = 0.40f * edge)),
                            startY = size.height - 12f,
                            endY = size.height
                        ),
                        topLeft = Offset(0f, size.height - 12f),
                        size = Size(size.width, 12f)
                    )
                    drawRect(
                        Brush.horizontalGradient(
                            listOf(Color.Black.copy(alpha = 0.26f * edge), Color.Transparent),
                            startX = 0f,
                            endX = 10f
                        ),
                        topLeft = Offset.Zero,
                        size = Size(10f, size.height)
                    )
                    drawRect(
                        Brush.horizontalGradient(
                            listOf(Color.Transparent, Color.White.copy(alpha = 0.07f * edge)),
                            startX = size.width - 10f,
                            endX = size.width
                        ),
                        topLeft = Offset(size.width - 10f, 0f),
                        size = Size(10f, size.height)
                    )
                }
            }

            // The note, sandblasted into the glass.
            Column(
                Modifier
                    .align(Alignment.TopStart)
                    .padding(start = 20.dp, end = 20.dp, top = 18.dp)
                    .fillMaxWidth()
                    .graphicsLayer {
                        val enterMs = enter.value * GlassTimelineMs
                        val local = ((enterMs - 900f) / 650f).coerceIn(0f, 1f)
                        alpha = if (leaving) {
                            (1f - (ex * GlassExitMs) / 200f).coerceIn(0f, 1f)
                        } else {
                            stepped(local, listOf(0f to 0f, 0.6f to 1f, 1f to 1f))
                        }
                    }
            ) {
                GlassSenderRow(
                    data = data,
                    showTime = showTime,
                    showLogo = showLogo,
                    logoSize = logoSize,
                    ink = ink
                )
                Spacer(Modifier.height(5.dp))
                CardBody(
                    text = data.body,
                    lines = lines,
                    color = ink,
                    font = ThemeFonts.Yeseva,
                    weight = FontWeight.Bold,
                    sizeSp = 14.5f,
                    lineHeightFactor = 18f / 14.5f,
                    style = TextStyle(shadow = GlassHalo)
                )
            }

            if (hasDock) {
                GlassDock(
                    data = data,
                    interactions = interactions,
                    scale = scale,
                    ink = ink,
                    enter = enter,
                    ex = ex,
                    leaving = leaving,
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(start = 20.dp, end = 20.dp, bottom = 17.dp)
                )
            }
        }
    }
}

/** The window grows out of the screen, and folds back into it. */
private fun Modifier.glassLayers(
    enter: Animatable<Float, *>,
    ex: Float,
    leaving: Boolean
): Modifier = graphicsLayer {
    val enterMs = enter.value * GlassTimelineMs
    val exitMs = ex * GlassExitMs
    if (leaving) {
        val p = (exitMs / 700f).coerceIn(0f, 1f)
        val s = 1f + (0.93f - 1f) * p
        scaleX = s
        scaleY = s
    } else {
        val p = (enterMs / 750f).coerceIn(0f, 1f)
        val s = 0.94f + (1f - 0.94f) * p
        scaleX = s
        scaleY = s
    }
}

/* ------------------------------------------------------------------ */
/* The note                                                             */
/* ------------------------------------------------------------------ */

@Composable
private fun GlassSenderRow(
    data: TickerData,
    showTime: Boolean,
    showLogo: Boolean,
    logoSize: Dp,
    ink: Color
) {
    Row(
        Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (showLogo) {
            // The app icon, loose on the glass — no box around it.
            CardIcon(
                data = data,
                size = logoSize.coerceIn(20.dp, 30.dp),
                shape = RoundedCornerShape(5.dp),
                fallbackBg = Color.Transparent,
                fallbackFg = GlassInk,
                modifier = Modifier.shadow(3.dp, RoundedCornerShape(6.dp), clip = false)
            )
            Spacer(Modifier.width(10.dp))
        }
        Text(
            data.title,
            color = ink,
            fontSize = 22.sp,
            fontFamily = ThemeFonts.CormorantBold,
            fontWeight = FontWeight.ExtraBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            style = TextStyle(shadow = GlassHaloBright),
            modifier = Modifier.weight(1f)
        )
        if (showTime) {
            Spacer(Modifier.width(10.dp))
            Text(
                data.time.uppercase(),
                color = ink,
                fontSize = 12.sp,
                fontFamily = ThemeFonts.Nunito,
                fontWeight = FontWeight.Black,
                letterSpacing = 1.6.sp,
                maxLines = 1,
                style = TextStyle(shadow = GlassHalo)
            )
        }
    }
}

/* ------------------------------------------------------------------ */
/* The dock: frosted tiles of glass laid over the pane                 */
/* ------------------------------------------------------------------ */

@Composable
private fun BoxScope.GlassDock(
    data: TickerData,
    interactions: TickerInteractions?,
    scale: Float,
    ink: Color,
    enter: Animatable<Float, *>,
    ex: Float,
    leaving: Boolean,
    modifier: Modifier = Modifier
) {
    val specs = notificationActionSpecs(data, interactions)
    if (specs.isEmpty()) return

    Row(
        modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(9.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically
    ) {
        specs.forEachIndexed { index, spec ->
            val source = remember { MutableInteractionSource() }
            val pressed by source.collectIsPressedAsState()
            val appearAt = 1040f + index * 90f

            Box(
                Modifier
                    .height(35.dp)
                    .graphicsLayer {
                        val enterMs = enter.value * GlassTimelineMs
                        val local = ((enterMs - appearAt) / 500f).coerceIn(0f, 1f)
                        alpha = if (leaving) {
                            (1f - (ex * GlassExitMs) / 280f).coerceIn(0f, 1f)
                        } else {
                            stepped(local, listOf(0f to 0f, 0.6f to 1f, 1f to 1f))
                        }
                        // The tiles pop in place — a stagger that lifted them
                        // left the last one hanging above the others.
                        val pop = if (leaving) 1f - alpha else
                            stepped(local, listOf(0f to 0.92f, 0.6f to 1.03f, 1f to 1f))
                        val s = if (pressed) 0.97f else pop
                        scaleX = s
                        scaleY = s
                    }
                    .shadow(6.dp, RoundedCornerShape(3.dp), clip = false)
                    .clip(RoundedCornerShape(3.dp))
                    .background(Color(0x1AFFFFFF))
                    .drawBehind { drawGlassTile() }
                    .clickable(
                        interactionSource = source,
                        indication = null,
                        enabled = spec.onClick != null
                    ) { spec.onClick?.invoke() }
                    .padding(horizontal = 14.dp),
                contentAlignment = Alignment.Center
            ) {
                if (spec.iconOnly) {
                    Icon(
                        spec.icon,
                        contentDescription = spec.label,
                        tint = ink,
                        modifier = Modifier.size(15.dp * scale)
                    )
                } else {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            spec.icon,
                            contentDescription = null,
                            tint = ink,
                            modifier = Modifier.size(12.dp * scale)
                        )
                        Spacer(Modifier.width(5.dp))
                        Text(
                            spec.label.uppercase(),
                            color = ink,
                            fontSize = (11f * scale).sp,
                            fontFamily = ThemeFonts.Nunito,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 1.2.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            style = TextStyle(shadow = GlassHalo)
                        )
                    }
                }
            }
        }
    }
}

/** The sheen across a frosted glass tile. */
private fun DrawScope.drawGlassTile() {
    drawRect(
        Brush.linearGradient(
            listOf(
                Color.White.copy(alpha = 0.34f),
                Color.White.copy(alpha = 0.07f),
                Color.Transparent
            ),
            start = Offset.Zero,
            end = Offset(size.width * 0.9f, size.height)
        )
    )
    drawRect(
        Brush.verticalGradient(
            listOf(Color.White.copy(alpha = 0.35f), Color.Transparent),
            startY = 0f,
            endY = 2f
        ),
        topLeft = Offset.Zero,
        size = Size(size.width, 2f)
    )
    val inset = 2.dp.toPx()
    drawRect(
        color = Color.White.copy(alpha = 0.16f),
        topLeft = Offset(inset, inset),
        size = Size(size.width - inset * 2f, size.height - inset * 2f),
        style = Stroke(width = 1.dp.toPx())
    )
}
