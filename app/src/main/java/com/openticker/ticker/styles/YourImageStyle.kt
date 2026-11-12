package com.openticker.ticker.styles

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color as AndroidColor
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
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.openticker.R
import com.openticker.data.Prefs
import com.openticker.data.TickerAction
import com.openticker.data.TickerData
import com.openticker.data.TickerInteractions
import com.openticker.data.TickerKind
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.max

/*
 * Your Image — a notification over a user-supplied photo.
 * Position and scale come from the image controls in the style settings.
 */

@OptIn(ExperimentalTextApi::class)
private val ImageDisplay = FontFamily(
    Font(
        R.font.sora,
        weight = FontWeight.ExtraBold,
        variationSettings = FontVariation.Settings(FontVariation.weight(800))
    ),
    Font(
        R.font.sora,
        weight = FontWeight.Bold,
        variationSettings = FontVariation.Settings(FontVariation.weight(700))
    )
)

private data class LoadedImage(val bitmap: Bitmap, val light: Boolean)

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun CuratedYourImageTicker(
    data: TickerData,
    interactions: TickerInteractions?
) {
    val context = LocalContext.current
    val options = Prefs.yourImage(context)

    val loaded by produceState<LoadedImage?>(
        initialValue = null,
        options.imagePath
    ) {
        value = withContext(Dispatchers.IO) {
            loadYourImage(options.imagePath)
        }
    }

    val onLight = loaded?.light == true
    val text = if (onLight) Color(0xFF0B0B0F) else Color.White
    val textSoft = if (onLight) Color(0xD10B0B0F) else Color(0xEBFFFFFF)
    val timeColor = if (onLight) Color(0xBD141418) else Color(0xDBE6F0FF)
    val iconBg = if (onLight) Color.White.copy(alpha = 0.74f) else Color(0xFF08121A).copy(alpha = 0.62f)
    val iconBorder = if (onLight) Color.Black.copy(alpha = 0.55f) else Color.White.copy(alpha = 0.75f)
    val iconTint = if (onLight) Color(0xFF0B0B0F) else Color.White
    val pillBg = if (onLight) Color.White.copy(alpha = 0.42f) else Color(0xFF7E8A96).copy(alpha = 0.22f)
    val pillBorder = if (onLight) Color.Black.copy(alpha = 0.30f) else Color.White.copy(alpha = 0.36f)
    val textShadow = if (onLight) {
        Shadow(Color(0x99FFFFFF), Offset(0f, 1f), 8f)
    } else {
        Shadow(Color(0xCC000000), Offset(0f, 2f), 12f)
    }
    val shade = if (onLight) {
        listOf(Color(0x42FFFFFF), Color(0x0FFFFFFF), Color(0x7AFFFFFF))
    } else {
        listOf(Color(0x6B02060C), Color(0x4002060C), Color(0x9E02060C))
    }

    val shape = RoundedCornerShape(26.dp)

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
        BoxWithConstraints(Modifier.fillMaxWidth()) {
            val cardWidth = maxWidth
            val cardHeight = (maxWidth / 2.2f).coerceIn(168.dp, 204.dp)

            Box(
                Modifier
                    .fillMaxWidth()
                    .height(cardHeight)
                    .clip(shape)
                    .background(Color(0xFF0B1524))
                    .border(1.dp, if (onLight) Color.Black.copy(alpha = 0.16f) else Color.White.copy(alpha = 0.2f), shape)
            ) {
                val image = loaded
                if (image != null) {
                    val zoom = options.imageScale / 100f
                    val bmp = image.bitmap
                    // Fill the card edge-to-edge; scale > 100% multiplies from there.
                    val s = max(cardWidth.value / bmp.width, cardHeight.value / bmp.height) * zoom
                    val dw = (bmp.width * s).dp
                    val dh = (bmp.height * s).dp
                    // The image may never travel past its own edge — how far it can
                    // pan depends on the image's shape and the chosen zoom.
                    val slackX = ((dw.value - cardWidth.value) / 2f).coerceAtLeast(0f)
                    val slackY = ((dh.value - cardHeight.value) / 2f).coerceAtLeast(0f)
                    val tx = (options.imageX / 1000f * cardWidth.value).coerceIn(-slackX, slackX)
                    val ty = (options.imageY / 1000f * cardHeight.value).coerceIn(-slackY, slackY)
                    Image(
                        bitmap = bmp.asImageBitmap(),
                        contentDescription = null,
                        contentScale = ContentScale.FillBounds,
                        filterQuality = FilterQuality.High,
                        modifier = Modifier
                            .requiredSize(dw, dh)
                            .align(Alignment.Center)
                            .graphicsLayer {
                                alpha = options.imageOpacity / 100f
                                translationX = tx.dp.toPx()
                                translationY = ty.dp.toPx()
                            }
                    )
                } else {
                    Box(
                        Modifier
                            .matchParentSize()
                            .background(
                                Brush.verticalGradient(
                                    listOf(Color(0xFF071A33), Color(0xFF103152), Color(0xFF040C18))
                                )
                            )
                    )
                }

                Box(
                    Modifier
                        .matchParentSize()
                        .background(Brush.verticalGradient(shade))
                )

                Column(
                    Modifier
                        .fillMaxWidth()
                        .height(cardHeight)
                        .padding(16.dp),
                    verticalArrangement = Arrangement.Bottom
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (options.showLogo) {
                            YourImageAppIcon(
                                data = data,
                                background = iconBg,
                                border = iconBorder,
                                tint = iconTint,
                                size = options.avatarSize.dp
                            )
                            Spacer(Modifier.width(14.dp))
                        }

                        Column(Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.Bottom) {
                                Text(
                                    data.title,
                                    color = text,
                                    fontSize = 21.sp,
                                    lineHeight = 25.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    fontFamily = ImageDisplay,
                                    letterSpacing = (-0.3).sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    style = TextStyle(shadow = textShadow),
                                    modifier = Modifier.weight(1f)
                                )
                                if (options.showTime) {
                                    Spacer(Modifier.width(10.dp))
                                    Text(
                                        data.time,
                                        color = timeColor,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = ImageDisplay,
                                        maxLines = 1,
                                        style = TextStyle(shadow = textShadow)
                                    )
                                }
                            }
                            Spacer(Modifier.height(7.dp))
                            ImageBodyText(
                                body = data.body,
                                lines = options.lines,
                                color = textSoft,
                                shadow = textShadow
                            )
                        }
                    }

                    Spacer(Modifier.height(20.dp))

                    ImageActions(
                        data = data,
                        interactions = interactions,
                        background = pillBg,
                        border = pillBorder,
                        text = text,
                        shadow = textShadow
                    )
                }
            }
        }
    }
}

/* ------------------------------------------------------------------ */
/* Image loading + contrast                                            */
/* ------------------------------------------------------------------ */

private fun loadYourImage(path: String): LoadedImage? {
    if (path.isBlank()) return null

    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    BitmapFactory.decodeFile(path, bounds)
    if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null

    var sample = 1
    while (bounds.outWidth / sample > 2048 || bounds.outHeight / sample > 2048) {
        sample *= 2
    }

    val bitmap = BitmapFactory.decodeFile(
        path,
        BitmapFactory.Options().apply {
            inSampleSize = sample
            inPreferredConfig = Bitmap.Config.ARGB_8888
        }
    ) ?: return null

    return LoadedImage(bitmap, averageIsLight(bitmap))
}

private fun averageIsLight(bitmap: Bitmap): Boolean {
    val width = bitmap.width
    val height = bitmap.height
    if (width <= 0 || height <= 0) return false

    val left = (width * 0.06f).toInt()
    val right = (width * 0.94f).toInt()
    val top = (height * 0.32f).toInt()
    val bottom = height - 1

    val stepX = max(1, width / 48)
    val stepY = max(1, height / 48)

    var total = 0.0
    var count = 0

    var y = top
    while (y in top..bottom) {
        var x = left
        while (x in left..right) {
            val pixel = bitmap.getPixel(x, y)
            if (AndroidColor.alpha(pixel) >= 52) {
                total += 0.2126 * AndroidColor.red(pixel) +
                    0.7152 * AndroidColor.green(pixel) +
                    0.0722 * AndroidColor.blue(pixel)
                count++
            }
            x += stepX
        }
        y += stepY
    }

    return count > 0 && total / count > 145.0
}

/* ------------------------------------------------------------------ */
/* Pieces                                                              */
/* ------------------------------------------------------------------ */

@Composable
private fun YourImageAppIcon(
    data: TickerData,
    background: Color,
    border: Color,
    tint: Color,
    size: Dp
) {
    Box(
        Modifier
            .size(size)
            .clip(CircleShape)
            .background(background)
            .border(2.dp, border, CircleShape),
        contentAlignment = Alignment.Center
    ) {
        val icon = data.icon
        if (icon != null) {
            Image(
                bitmap = icon.asImageBitmap(),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize().clip(CircleShape)
            )
        } else {
            Icon(
                imageIconFor(data.kind),
                contentDescription = null,
                tint = tint,
                modifier = Modifier.size(size * 0.42f)
            )
        }
    }
}

private fun imageIconFor(kind: TickerKind) = when (kind) {
    TickerKind.Message -> Icons.Filled.Chat
    TickerKind.Call -> Icons.Filled.Phone
    TickerKind.Flight -> Icons.Filled.Flight
    TickerKind.Music -> Icons.Filled.MusicNote
    TickerKind.Generic -> Icons.Filled.Notifications
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ImageBodyText(
    body: String,
    lines: Int,
    color: Color,
    shadow: Shadow
) {
    val bodySp = Prefs.fontSize(LocalContext.current)
    val scrollSpeed = Prefs.speed(LocalContext.current)
    if (lines <= 0) {
        Text(
            body,
            color = color,
            fontSize = bodySp.sp,
            lineHeight = (bodySp * 1.36f).sp,
            fontWeight = FontWeight.Bold,
            fontFamily = ImageDisplay,
            maxLines = 1,
            softWrap = false,
            overflow = TextOverflow.Clip,
            style = TextStyle(shadow = shadow),
            modifier = Modifier
                .fillMaxWidth()
                .basicMarquee(iterations = Int.MAX_VALUE, velocity = (30f * scrollSpeed).dp)
        )
    } else {
        Text(
            body,
            color = color,
            fontSize = bodySp.sp,
            lineHeight = (bodySp * 1.36f).sp,
            fontWeight = FontWeight.Bold,
            fontFamily = ImageDisplay,
            minLines = lines,
            maxLines = lines,
            overflow = TextOverflow.Ellipsis,
            style = TextStyle(shadow = shadow),
            modifier = Modifier.fillMaxWidth()
        )
    }
}

/* ------------------------------------------------------------------ */
/* Actions                                                             */
/* ------------------------------------------------------------------ */

@Composable
private fun ImageActions(
    data: TickerData,
    interactions: TickerInteractions?,
    background: Color,
    border: Color,
    text: Color,
    shadow: Shadow
) {
    when (data.kind) {
        TickerKind.Call -> ImageCallActions(data, interactions, background, border, text, shadow)
        TickerKind.Music -> ImageMusicActions(data, interactions, background, border, text, shadow)
        else -> ImageDefaultActions(data, interactions, background, border, text, shadow)
    }
}

@Composable
private fun ImageDefaultActions(
    data: TickerData,
    interactions: TickerInteractions?,
    background: Color,
    border: Color,
    text: Color,
    shadow: Shadow
) {
    val buttons = notificationActionSpecs(data, interactions)

    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterHorizontally)
    ) {
        buttons.forEachIndexed { i, spec ->
            if (i > 0) Spacer(Modifier.width(10.dp))
            // Three (or more) actions: share the row equally, with smaller
            // type and icon so every label stays readable.
            val compact = buttons.size > 2
            GlassButton(
                spec.label,
                spec.icon,
                spec.onClick,
                background,
                border,
                text,
                shadow,
                modifier = if (compact) Modifier.weight(1f) else Modifier,
                minWidth = if (compact) 0.dp else 126.dp,
                labelSize = if (compact) 12.sp else 15.sp,
                paddingH = if (compact) 10.dp else 20.dp,
                compactIcon = compact
            )
        }
    }
}

@Composable
private fun ImageCallActions(
    data: TickerData,
    interactions: TickerInteractions?,
    background: Color,
    border: Color,
    text: Color,
    shadow: Shadow
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
    if (answer == null && reject == null || (live && !canAct)) {
        ImageDefaultActions(data, interactions, background, border, text, shadow)
        return
    }

    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterHorizontally)
    ) {
        GlassButton(
            "Reject",
            Icons.Filled.CallEnd,
            if (live && reject?.pendingIntent != null) {
                {
                    reject?.let { interactions?.onAction(it) }
                    interactions?.onDismiss()
                }
            } else null,
            background, border, text, shadow
        )
        GlassButton(
            "Answer",
            Icons.Filled.Phone,
            if (live && answer?.pendingIntent != null) {
                {
                    answer?.let { interactions?.onAction(it) }
                    interactions?.onDismiss()
                }
            } else null,
            background, border, text, shadow
        )
    }
}

@Composable
private fun ImageMusicActions(
    data: TickerData,
    interactions: TickerInteractions?,
    background: Color,
    border: Color,
    text: Color,
    shadow: Shadow
) {
    val controls = mutableListOf<ImageMediaControl>()

    data.actions.firstOrNull {
        labelHas(
            it.label,
            "previous", "prev", "rewind", "back",
            "السابق", "سابق", "précédent", "anterior", "zurück", "önceki"
        )
    }?.let { controls += ImageMediaControl(it, Icons.Filled.SkipPrevious) }

    data.actions.firstOrNull {
        labelHas(
            it.label,
            "play", "pause", "resume", "paus",
            "تشغيل", "إيقاف", "ايقاف", "استئناف", "مؤقت",
            "lecture", "reproduc", "pausa", "wiedergabe", "oynat", "duraklat"
        )
    }?.let {
        val icon = if (isPauseLabel(it.label)) Icons.Filled.Pause else Icons.Filled.PlayArrow
        controls += ImageMediaControl(it, icon)
    }

    data.actions.firstOrNull {
        labelHas(
            it.label,
            "next", "forward", "skip",
            "التالي", "تالي", "suivant", "siguiente", "weiter", "sonraki"
        )
    }?.let { controls += ImageMediaControl(it, Icons.Filled.SkipNext) }

    if (controls.isEmpty() && data.actions.isNotEmpty()) {
        data.actions.take(3).forEachIndexed { index, action ->
            val icon = when {
                data.actions.size == 1 -> Icons.Filled.PlayArrow
                index == 0 -> Icons.Filled.SkipPrevious
                index == data.actions.lastIndex -> Icons.Filled.SkipNext
                isPauseLabel(action.label) -> Icons.Filled.Pause
                else -> Icons.Filled.PlayArrow
            }
            controls += ImageMediaControl(action, icon)
        }
    }

    if (controls.isEmpty()) {
        ImageDefaultActions(data, interactions, background, border, text, shadow)
        return
    }

    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterHorizontally)
    ) {
        controls.forEach { control ->
            GlassButton(
                label = null,
                icon = control.icon,
                onClick = if (interactions != null && control.action.pendingIntent != null) {
                    { interactions?.onAction(control.action) }
                } else null,
                background = background,
                border = border,
                text = text,
                shadow = shadow
            )
        }
    }
}

private data class ImageMediaControl(val action: TickerAction, val icon: ImageVector)

private fun labelHas(label: String, vararg keys: String): Boolean =
    keys.any { label.contains(it, ignoreCase = true) }

private fun isPauseLabel(label: String): Boolean =
    labelHas(label, "pause", "paus", "إيقاف", "ايقاف", "مؤقت", "duraklat")

@Composable
private fun GlassButton(
    label: String?,
    icon: ImageVector,
    onClick: (() -> Unit)?,
    background: Color,
    border: Color,
    text: Color,
    shadow: Shadow,
    modifier: Modifier = Modifier,
    minWidth: Dp = 126.dp,
    labelSize: TextUnit = 15.sp,
    paddingH: Dp = 20.dp,
    compactIcon: Boolean = false
) {
    val shape = RoundedCornerShape(50)
    Row(
        modifier
            .height(44.dp)
            .widthIn(min = if (label == null) 0.dp else minWidth)
            .clip(shape)
            .background(background)
            .border(1.dp, border, shape)
            .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier)
            .padding(horizontal = paddingH),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            icon,
            contentDescription = label,
            tint = text,
            modifier = Modifier.size(
                if (label == null) 18.dp else if (compactIcon) 13.dp else 15.dp
            )
        )
        if (label != null) {
            Spacer(Modifier.width(if (compactIcon) 5.dp else 7.dp))
            Text(
                label,
                color = text,
                fontSize = labelSize,
                fontWeight = FontWeight.Bold,
                fontFamily = ImageDisplay,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = TextStyle(shadow = shadow)
            )
        }
    }
}
