package com.openticker.ui.screens

import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.openticker.data.DemoData
import com.openticker.data.Prefs
import com.openticker.data.SystemWindowOptions
import com.openticker.ticker.cards.CardCatalog
import com.openticker.ticker.StyleRegistry
import com.openticker.ticker.TickerStyle
import com.openticker.ticker.TickerSurface
import com.openticker.ui.components.TickerSlider
import com.openticker.ui.components.IconTile
import com.openticker.ui.components.PillButton
import com.openticker.ui.components.SectionLabel
import com.openticker.ui.i18n.t
import com.openticker.ui.theme.Hairline
import com.openticker.ui.theme.Ink
import com.openticker.ui.theme.Mint
import com.openticker.ui.theme.Accent
import com.openticker.ui.theme.Surface1
import com.openticker.ui.theme.Surface2
import com.openticker.ui.theme.TextDim
import com.openticker.ui.theme.TextMain
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.Locale

@Composable
fun StyleDetailScreen(
    style: TickerStyle,
    onBack: () -> Unit,
    onShow: (TickerStyle) -> Unit
) {
    val context = LocalContext.current
    val isSystemWindow = style.id == "system_window"
    val isAurora = style.id == "aurora"
    val isSaturday = style.id == "saturday_morning"
    val isYourImage = style.id == "your_image"
    val isRadioDial = style.id == "radio_dial"
    val isAquatic = style.id == "aquatic"
    val isCardTheme = style.id in CardCatalog.ids
    var speed by remember { mutableFloatStateOf(Prefs.speed(context)) }
    var swOptions by remember { mutableStateOf(Prefs.systemWindow(context)) }
    var auOptions by remember { mutableStateOf(Prefs.aurora(context)) }
    var smOptions by remember { mutableStateOf(Prefs.saturdayMorning(context)) }
    var yiOptions by remember { mutableStateOf(Prefs.yourImage(context)) }
    var rdOptions by remember { mutableStateOf(Prefs.radioDial(context)) }
    var aqOptions by remember { mutableStateOf(Prefs.aquatic(context)) }
    var cardOptions by remember(style.id) {
        mutableStateOf(Prefs.themeOptions(context, style.id, style.accent.toArgb()))
    }
    val scope = rememberCoroutineScope()

    val imagePicker = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            scope.launch {
                val path = withContext(Dispatchers.IO) {
                    copyYourImageToFiles(context, uri)
                }
                if (!path.isNullOrBlank()) {
                    val previous = yiOptions.imagePath
                    if (previous.isNotBlank() && previous != path) {
                        runCatching { File(previous).delete() }
                    }
                    val next = yiOptions.copy(imagePath = path)
                    yiOptions = next
                    Prefs.saveYourImage(context, next)
                }
            }
        }
    }

    var active by remember {
        mutableStateOf(StyleRegistry.byId(Prefs.styleId(context)).id == style.id)
    }
    val previewData = when {
        isSystemWindow -> DemoData.systemWindowPreview(swOptions)
        isAurora -> DemoData.systemWindowPreview(auOptions)
        isSaturday -> DemoData.systemWindowPreview(smOptions)
        isYourImage -> DemoData.systemWindowPreview(yiOptions)
        isRadioDial -> DemoData.systemWindowPreview(rdOptions)
        isAquatic -> DemoData.systemWindowPreview(aqOptions)
        isCardTheme -> DemoData.cardPreview(cardOptions)
        else -> DemoData.sampleFor(style.id)
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize().statusBarsPadding(),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 14.dp, bottom = 130.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(Surface1)
                        .border(1.dp, Hairline, CircleShape)
                        .clickable(onClick = onBack),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Filled.ArrowBack,
                        contentDescription = t("Back"),
                        tint = TextMain,
                        modifier = Modifier.size(19.dp)
                    )
                }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        style.name,
                        color = TextMain,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = (-0.5).sp
                    )
                    Text(
                        style.description,
                        color = TextDim,
                        fontSize = 10.sp,
                        lineHeight = 14.sp,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Spacer(Modifier.width(10.dp))
                IconTile(icon = style.icon, accent = style.accent, size = 40)
            }
        }

        item {
            Box(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(28.dp))
                    .background(
                        Brush.verticalGradient(
                            listOf(Color(0xFF14141E), Color(0xFF09090F))
                        )
                    )
                    .border(1.dp, Hairline, RoundedCornerShape(28.dp))
                    .padding(14.dp)
            ) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        SectionLabel(t("Live preview"))
                        Spacer(Modifier.weight(1f))
                        Box(
                            Modifier.size(5.dp).clip(CircleShape).background(Mint)
                        )
                        Spacer(Modifier.width(5.dp))
                        Text(
                            t("ANIMATED"),
                            color = Mint,
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.4.sp
                        )
                    }
                    Spacer(Modifier.height(14.dp))
                    TickerSurface(
                        style = style,
                        data = previewData,
                        interactions = null
                    )
                    Spacer(Modifier.height(22.dp))
                    Box(
                        Modifier
                            .align(Alignment.CenterHorizontally)
                            .width(84.dp)
                            .height(4.dp)
                            .clip(RoundedCornerShape(50))
                            .background(Color.White.copy(alpha = 0.12f))
                    )
                }
            }
        }

        if (isYourImage) {
            item {
                YourImageCustomizer(
                    options = yiOptions,
                    onChange = { next ->
                        yiOptions = next
                        Prefs.saveYourImage(context, next)
                    },
                    onPickImage = { imagePicker.launch("image/*") },
                    onRemoveImage = {
                        if (yiOptions.imagePath.isNotBlank()) {
                            runCatching { File(yiOptions.imagePath).delete() }
                        }
                        val next = yiOptions.copy(imagePath = "")
                        yiOptions = next
                        Prefs.saveYourImage(context, next)
                    }
                )
            }
        }

        if (isSystemWindow || isAurora || isSaturday || isYourImage || isRadioDial || isAquatic) {
            item {
                SystemWindowCustomizer(
                    options = when {
                        isYourImage -> yiOptions
                        isRadioDial -> rdOptions
                        isAquatic -> aqOptions
                        isSaturday -> smOptions
                        isAurora -> auOptions
                        else -> swOptions
                    },
                    onChange = { next ->
                        when {
                            isYourImage -> {
                                yiOptions = next
                                Prefs.saveYourImage(context, next)
                            }
                            isRadioDial -> {
                                rdOptions = next
                                Prefs.saveRadioDial(context, next)
                            }
                            isAquatic -> {
                                aqOptions = next
                                Prefs.saveAquatic(context, next)
                            }
                            isSaturday -> {
                                smOptions = next
                                Prefs.saveSaturdayMorning(context, next)
                            }
                            isAurora -> {
                                auOptions = next
                                Prefs.saveAurora(context, next)
                            }
                            else -> {
                                swOptions = next
                                Prefs.saveSystemWindow(context, next)
                            }
                        }
                    },
                    title = when {
                        isYourImage -> t("Notification layout")
                        isRadioDial -> t("Customize radio")
                        isAquatic -> t("Customize aquatic")
                        isSaturday -> t("Customize cartoon")
                        isAurora -> t("Customize aurora")
                        else -> t("Customize hologram")
                    },
                    subtitle = when {
                        isYourImage -> t("Sender, message, avatar and line count.")
                        isRadioDial -> t("Needle, gold trim, screen mesh, lines and tuning-in motion.")
                        isAquatic -> t("Octopus colour, lines, preview kinds and reef motion.")
                        isSaturday -> t("Background, message bubble, buttons, lines and movement.")
                        isAurora -> t("Night sky, colour, message lines and wavy glass buttons.")
                        else -> t("Changes preview live and apply to the floating notification.")
                    },
                    effectTitle = when {
                        isSaturday -> t("Cartoon effects")
                        isAurora -> t("Aurora effect")
                        isRadioDial -> t("Radio effects")
                        isAquatic -> t("Reef effects")
                        else -> t("Hologram effect")
                    },
                    effectSubtitle = when {
                        isSaturday -> t("Sunburst, bouncing dots and wiggle")
                        isAurora -> t("Flowing lights and the animated button border")
                        isRadioDial -> t("Needle sweep, screen glow, sunburst and ring dial-in")
                        isAquatic -> t("God rays, ambient bubbles, caustics, fish and swaying kelp")
                        else -> t("Projector beam, scanline and glow")
                    },
                    threeColourMode = isSaturday || isRadioDial,
                    threeColourLabels = if (isRadioDial) {
                        Triple(t("Needle"), t("Gold trim"), t("Screen mesh"))
                    } else {
                        Triple(t("Background"), t("Message box"), t("Buttons"))
                    },
                    showColour = !isYourImage,
                    accentLabel = if (isAquatic) t("Octopus colour") else t("Colour"),
                    showIntensity = !isYourImage,
                    showEffect = !isYourImage
                )
            }
        }

        if (isCardTheme) {
            item {
                ThemeCustomizer(
                    options = cardOptions,
                    onChange = { next ->
                        cardOptions = next
                        Prefs.saveThemeOptions(context, style.id, next)
                    },
                    // The corkboard has no ambient layer, so no toggle for it.
                    showEffects = style.id != "sticky",
                    // The stained glass letter is the accent, so it gets a picker.
                    showInkColour = style.id == "glass"
                )
            }
        }

        item {
            Row(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .background(Accent)
                    .clickable { onShow(style) }
                    .padding(vertical = 16.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Filled.PlayArrow,
                    contentDescription = null,
                    tint = Ink,
                    modifier = Modifier.size(19.dp)
                )
                Spacer(Modifier.width(7.dp))
                Text(
                    t("Show on screen"),
                    color = Ink,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        item {
            Column(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(22.dp))
                    .background(Surface1)
                    .border(1.dp, Hairline, RoundedCornerShape(22.dp))
                    .padding(16.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconTile(icon = style.icon, accent = style.accent, size = 40)
                    Spacer(Modifier.width(11.dp))
                    Column(Modifier.weight(1f)) {
                        Text(
                            style.name,
                            color = TextMain,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            t("Notification style"),
                            color = TextDim,
                            fontSize = 10.5.sp
                        )
                    }
                    if (active) {
                        Box(
                            Modifier
                                .clip(RoundedCornerShape(50))
                                .background(Mint.copy(alpha = 0.12f))
                                .border(1.dp, Mint.copy(alpha = 0.3f), RoundedCornerShape(50))
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Text(
                                t("ACTIVE"),
                                color = Mint,
                                fontSize = 8.5.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.2.sp
                            )
                        }
                    } else {
                        PillButton(text = t("Use this"), filled = false, onClick = {
                            Prefs.setStyleId(context, style.id)
                            active = true
                        })
                    }
                }
                Spacer(Modifier.height(12.dp))
                Text(
                    style.description,
                    color = TextDim,
                    fontSize = 12.5.sp,
                    lineHeight = 18.sp
                )
            }
        }

        item {
            Column(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(22.dp))
                    .background(Surface1)
                    .border(1.dp, Hairline, RoundedCornerShape(22.dp))
                    .padding(16.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        t("Scroll speed"),
                        color = TextMain,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(Modifier.weight(1f))
                    Box(
                        Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(Surface2)
                            .padding(horizontal = 9.dp, vertical = 4.dp)
                    ) {
                        Text(
                            String.format(Locale.US, "%.1fx", speed),
                            color = TextMain,
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
                Spacer(Modifier.height(2.dp))
                TickerSlider(
                    value = speed,
                    onValueChange = {
                        speed = it
                        Prefs.setSpeed(context, it)
                    },
                    valueRange = 0.5f..3f,
                    steps = 9
                )
                Text(
                    t("Long messages glide past in one line until they've been shown in full."),
                    color = TextDim,
                    fontSize = 11.sp,
                    lineHeight = 16.sp
                )
            }
        }
    }
}

private fun copyYourImageToFiles(context: Context, uri: Uri): String? = runCatching {
    val file = File(context.filesDir, "your_image_${System.currentTimeMillis()}.img")
    val input = context.contentResolver.openInputStream(uri) ?: return null
    input.use { source ->
        file.outputStream().use { target ->
            source.copyTo(target)
        }
    }
    file.absolutePath
}.getOrNull()
