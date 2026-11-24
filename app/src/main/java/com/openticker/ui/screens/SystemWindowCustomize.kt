package com.openticker.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.openticker.data.SystemWindowOptions
import com.openticker.ui.components.SectionLabel
import com.openticker.ui.components.TickerSlider
import com.openticker.ui.i18n.t
import com.openticker.ui.theme.Hairline
import com.openticker.ui.theme.Accent
import com.openticker.ui.theme.Surface1
import com.openticker.ui.theme.Surface2
import com.openticker.ui.theme.TextDim
import com.openticker.ui.theme.TextMain
import kotlin.math.roundToInt

private data class AccentChoice(val name: String, val argb: Int)

@Composable
private fun accentChoices(): List<AccentChoice> = listOf(
    AccentChoice(t("Ink"), 0xFF14161A.toInt()),
    AccentChoice(t("Cyan"), 0xFF6FF6FF.toInt()),
    AccentChoice(t("Violet"), 0xFFA78BFF.toInt()),
    AccentChoice(t("Mint"), 0xFF5CE8AE.toInt()),
    AccentChoice(t("Amber"), 0xFFFFB03A.toInt()),
    AccentChoice(t("Rose"), 0xFFFF6FAE.toInt()),
    AccentChoice(t("Ice white"), 0xFFEAF6FF.toInt()),
    AccentChoice(t("Sunny"), 0xFFFFCF3F.toInt()),
    AccentChoice(t("Grape"), 0xFF7A3BD9.toInt()),
    AccentChoice(t("Tomato"), 0xFFE8453C.toInt()),
    AccentChoice(t("Cream"), 0xFFFFF3D6.toInt()),
    AccentChoice(t("Sky"), 0xFF4EA8DE.toInt()),
    AccentChoice(t("Coral"), 0xFFFF8A65.toInt()),
    AccentChoice(t("Gold"), 0xFFE8B33A.toInt()),
    AccentChoice(t("Needle"), 0xFFD8402C.toInt()),
    AccentChoice(t("Mesh"), 0xFF8A5A2E.toInt()),
    AccentChoice(t("Wood"), 0xFF6B3A1F.toInt()),
    AccentChoice(t("Walnut"), 0xFF4A2412.toInt()),
    AccentChoice(t("Aqua"), 0xFF8FE8EE.toInt()),
    AccentChoice(t("Reef coral"), 0xFFFF7A5C.toInt()),
    AccentChoice(t("Kelp"), 0xFF1E8C74.toInt())
)

@Composable
private fun previewKinds(): List<Pair<String, String>> = listOf(
    "message" to t("Message"),
    "call" to t("Incoming call"),
    "flight" to t("Flight"),
    "music" to t("Music"),
    "notification" to t("Notification"),
    "none" to t("No actions")
)

/**
 * All System Window customisation in one panel. Everything here is free —
 * there is deliberately no Pro gate.
 */
@Composable
fun SystemWindowCustomizer(
    options: SystemWindowOptions,
    onChange: (SystemWindowOptions) -> Unit,
    title: String = "",
    subtitle: String = "",
    effectTitle: String = "",
    effectSubtitle: String = "",
    threeColourMode: Boolean = false,
    threeColourLabels: Triple<String, String, String> = Triple("", "", ""),
    accentLabel: String = "",
    showColour: Boolean = true,
    showIntensity: Boolean = true,
    showEffect: Boolean = true
) {
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(Surface1)
            .border(1.dp, Hairline, RoundedCornerShape(22.dp))
            .padding(16.dp)
    ) {
        Text(
            title,
            color = TextMain,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold
        )
        Text(
            subtitle,
            color = TextDim,
            fontSize = 10.5.sp,
            lineHeight = 15.sp
        )

        Spacer(Modifier.height(16.dp))
        SectionLabel(t("Preview with"))
        Spacer(Modifier.height(9.dp))
        Row(
            Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(7.dp)
        ) {
            previewKinds().forEach { (id, label) ->
                ChoiceChip(
                    label = label,
                    selected = options.previewKind == id,
                    onClick = { onChange(options.copy(previewKind = id)) }
                )
            }
        }

        Spacer(Modifier.height(14.dp))
        CustomField(
            label = t("Sender"),
            value = options.sender,
            singleLine = true,
            onValueChange = { onChange(options.copy(sender = it)) }
        )
        Spacer(Modifier.height(12.dp))
        CustomField(
            label = t("Message"),
            value = options.message,
            singleLine = false,
            minHeight = 56.dp,
            onValueChange = { onChange(options.copy(message = it)) }
        )

        if (showColour) {
            Spacer(Modifier.height(18.dp))
            if (threeColourMode) {
                ColourRow(
                    label = threeColourLabels.first,
                    selectedArgb = options.secondaryArgb,
                    onPick = { onChange(options.copy(secondaryArgb = it)) }
                )
                Spacer(Modifier.height(12.dp))
                ColourRow(
                    label = threeColourLabels.second,
                    selectedArgb = options.accentArgb,
                    onPick = { onChange(options.copy(accentArgb = it)) }
                )
                Spacer(Modifier.height(12.dp))
                ColourRow(
                    label = threeColourLabels.third,
                    selectedArgb = options.tertiaryArgb,
                    onPick = { onChange(options.copy(tertiaryArgb = it)) }
                )
            } else {
                ColourRow(
                    label = accentLabel,
                    selectedArgb = options.accentArgb,
                    onPick = { onChange(options.copy(accentArgb = it)) }
                )
            }
        }

        if (showIntensity) {
            Spacer(Modifier.height(18.dp))
            SettingHeader(
                title = t("Intensity"),
                value = "${options.intensity}%"
            )
            TickerSlider(
                value = options.intensity.toFloat(),
                onValueChange = { onChange(options.copy(intensity = it.roundToInt())) },
                valueRange = 0f..100f,
                steps = 9
            )
        }

        Spacer(Modifier.height(10.dp))
        SettingHeader(
            title = t("Logo / avatar size"),
            value = "${options.avatarSize} dp"
        )
        TickerSlider(
            value = options.avatarSize.toFloat(),
            onValueChange = { onChange(options.copy(avatarSize = it.roundToInt())) },
            valueRange = 24f..48f,
            steps = 11
        )

        Spacer(Modifier.height(8.dp))
        ToggleRow(
            title = t("Show app logo"),
            subtitle = t("Use the source app icon in the card"),
            checked = options.showLogo,
            onChange = { onChange(options.copy(showLogo = it)) }
        )
        ToggleRow(
            title = t("Show time"),
            subtitle = t("Display the notification time"),
            checked = options.showTime,
            onChange = { onChange(options.copy(showTime = it)) }
        )
        if (showEffect) {
            ToggleRow(
                title = effectTitle,
                subtitle = effectSubtitle,
                checked = options.effect,
                onChange = { onChange(options.copy(effect = it)) }
            )
        }

        Spacer(Modifier.height(14.dp))
        SectionLabel(t("Message lines"))
        Spacer(Modifier.height(9.dp))
        Row(
            Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(7.dp)
        ) {
            listOf(1, 2, 3, 0).forEach { count ->
                ChoiceChip(
                    label = if (count == 0) t("Ticker") else "$count line${if (count == 1) "" else "s"}",
                    selected = options.lines == count,
                    onClick = { onChange(options.copy(lines = count)) }
                )
            }
        }
        Spacer(Modifier.height(8.dp))
        Text(
            t("Ticker runs the message like a news line. Wrapped modes show up to 3 lines, then ellipsize."),
            color = TextDim,
            fontSize = 10.5.sp,
            lineHeight = 15.sp
        )
    }
}

/**
 * Controls for one of the twenty-two illustrated card themes.
 *
 * Same settings as the other notification layouts — minus the colour picker:
 * these themes keep their designed palette.
 */
@Composable
fun ThemeCustomizer(
    options: SystemWindowOptions,
    onChange: (SystemWindowOptions) -> Unit,
    showEffects: Boolean = true,
    /** Themes whose lettering is the accent offer a colour picker for it. */
    showInkColour: Boolean = false
) {
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(Surface1)
            .border(1.dp, Hairline, RoundedCornerShape(22.dp))
            .padding(16.dp)
    ) {
        Text(
            t("Customize card"),
            color = TextMain,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold
        )
        Text(
            t("Preview with, motion, message lines and the button size — these themes keep their designed palette."),
            color = TextDim,
            fontSize = 10.5.sp,
            lineHeight = 15.sp
        )

        Spacer(Modifier.height(16.dp))
        SectionLabel(t("Preview with"))
        Spacer(Modifier.height(9.dp))
        Row(
            Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(7.dp)
        ) {
            previewKinds().forEach { (id, label) ->
                ChoiceChip(
                    label = label,
                    selected = options.previewKind == id,
                    onClick = { onChange(options.copy(previewKind = id)) }
                )
            }
        }
        Spacer(Modifier.height(6.dp))
        Text(
            t("Mirrors the real thing: a message shows Reply / Read / Mute, a call shows Answer / Reject, music shows transport controls — and a notification with no actions gets a blank dock."),
            color = TextDim,
            fontSize = 10.5.sp,
            lineHeight = 15.sp
        )
        Spacer(Modifier.height(14.dp))
        CustomField(
            label = t("Sender"),
            value = options.sender,
            singleLine = true,
            onValueChange = { onChange(options.copy(sender = it)) }
        )
        Spacer(Modifier.height(12.dp))
        CustomField(
            label = t("Message"),
            value = options.message,
            singleLine = false,
            minHeight = 56.dp,
            onValueChange = { onChange(options.copy(message = it)) }
        )

        Spacer(Modifier.height(18.dp))
        if (showInkColour) {
            ColourRow(
                label = t("Font colour"),
                selectedArgb = options.accentArgb,
                onPick = { onChange(options.copy(accentArgb = it)) }
            )
            Spacer(Modifier.height(6.dp))
            Text(
                t("The colour of the sender, the clock, the message and the button labels — the glass keeps its own jewel tones."),
                color = TextDim,
                fontSize = 10.5.sp,
                lineHeight = 15.sp
            )
            Spacer(Modifier.height(18.dp))
        }
        SettingHeader(
            title = t("Intensity"),
            value = "${options.intensity}%"
        )
        TickerSlider(
            value = options.intensity.toFloat(),
            onValueChange = { onChange(options.copy(intensity = it.roundToInt())) },
            valueRange = 0f..100f,
            steps = 9
        )

        Spacer(Modifier.height(10.dp))
        SettingHeader(
            title = t("Logo / avatar size"),
            value = "${options.avatarSize} dp"
        )
        TickerSlider(
            value = options.avatarSize.toFloat(),
            onValueChange = { onChange(options.copy(avatarSize = it.roundToInt())) },
            valueRange = 24f..48f,
            steps = 11
        )

        Spacer(Modifier.height(10.dp))
        ToggleRow(
            title = t("Show app logo"),
            subtitle = t("Use the source app icon in the card"),
            checked = options.showLogo,
            onChange = { onChange(options.copy(showLogo = it)) }
        )
        ToggleRow(
            title = t("Show time"),
            subtitle = t("Display the notification time"),
            checked = options.showTime,
            onChange = { onChange(options.copy(showTime = it)) }
        )
        if (showEffects) {
            ToggleRow(
                title = t("Ambient effects"),
                subtitle = t("Stars, scanlines, weather, light rays and other motion"),
                checked = options.effect,
                onChange = { onChange(options.copy(effect = it)) }
            )
        }

        Spacer(Modifier.height(14.dp))
        SectionLabel(t("Message lines"))
        Spacer(Modifier.height(9.dp))
        Row(
            Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(7.dp)
        ) {
            listOf(1, 2, 3, 0).forEach { count ->
                ChoiceChip(
                    label = if (count == 0) t("Ticker") else "$count line${if (count == 1) "" else "s"}",
                    selected = options.lines == count,
                    onClick = { onChange(options.copy(lines = count)) }
                )
            }
        }
        Spacer(Modifier.height(6.dp))
        Text(
            t("One line = thinner card, three lines = taller card. Ticker runs the message like a news line."),
            color = TextDim,
            fontSize = 10.5.sp,
            lineHeight = 15.sp
        )

        Spacer(Modifier.height(14.dp))
        SettingHeader(
            title = t("Button size"),
            value = "${options.buttonScale}%"
        )
        TickerSlider(
            value = options.buttonScale.toFloat(),
            onValueChange = { onChange(options.copy(buttonScale = it.roundToInt())) },
            valueRange = 70f..150f,
            steps = 7
        )
        Text(
            t("Scales the dock buttons only — the card, artwork and text stay put."),
            color = TextDim,
            fontSize = 10.5.sp,
            lineHeight = 15.sp
        )
    }
}

@Composable
private fun ColourRow(
    label: String,
    selectedArgb: Int,
    onPick: (Int) -> Unit
) {
    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                label,
                color = TextMain,
                fontSize = 12.5.sp,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(Modifier.weight(1f))
            Text(
                accentChoices().firstOrNull { it.argb == selectedArgb }?.name ?: t("Custom"),
                color = TextDim,
                fontSize = 10.sp
            )
        }
        Spacer(Modifier.height(8.dp))
        Row(
            Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            accentChoices().forEach { choice ->
                val selected = selectedArgb == choice.argb
                Box(
                    Modifier
                        .size(if (selected) 32.dp else 28.dp)
                        .clip(CircleShape)
                        .background(Color(choice.argb).copy(alpha = 0.22f))
                        .border(
                            width = if (selected) 2.dp else 1.dp,
                            color = if (selected) {
                                Color(choice.argb)
                            } else {
                                Color(choice.argb).copy(alpha = 0.45f)
                            },
                            shape = CircleShape
                        )
                        .clickable { onPick(choice.argb) },
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        Modifier
                            .size(if (selected) 16.dp else 14.dp)
                            .clip(CircleShape)
                            .background(Color(choice.argb))
                    )
                }
            }
        }
    }
}

@Composable
private fun SettingHeader(title: String, value: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            title,
            color = TextMain,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(Modifier.weight(1f))
        Box(
            Modifier
                .clip(RoundedCornerShape(8.dp))
                .background(Surface2)
                .padding(horizontal = 8.dp, vertical = 4.dp)
        ) {
            Text(
                value,
                color = TextMain,
                fontSize = 10.5.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
private fun ChoiceChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Box(
        Modifier
            .clip(RoundedCornerShape(50))
            .background(if (selected) Accent.copy(alpha = 0.15f) else Surface2)
            .border(
                1.dp,
                if (selected) Accent else Hairline,
                RoundedCornerShape(50)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        Text(
            label,
            color = if (selected) Accent else TextDim,
            fontSize = 10.5.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1
        )
    }
}

@Composable
private fun CustomField(
    label: String,
    value: String,
    singleLine: Boolean,
    minHeight: androidx.compose.ui.unit.Dp = 0.dp,
    onValueChange: (String) -> Unit
) {
    Column {
        Text(
            label.uppercase(),
            color = TextDim,
            fontSize = 9.5.sp,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 1.6.sp
        )
        Spacer(Modifier.height(6.dp))
        Box(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(Surface2)
                .border(1.dp, Hairline, RoundedCornerShape(12.dp))
                .padding(horizontal = 12.dp, vertical = 11.dp)
        ) {
            if (value.isEmpty()) {
                Text(
                    t("Type here…"),
                    color = TextDim.copy(alpha = 0.6f),
                    fontSize = 12.5.sp
                )
            }
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                textStyle = TextStyle(
                    color = TextMain,
                    fontSize = 12.5.sp,
                    lineHeight = 17.sp
                ),
                cursorBrush = SolidColor(Accent),
                singleLine = singleLine,
                minLines = if (singleLine) 1 else 2,
                maxLines = if (singleLine) 1 else 4,
                modifier = Modifier
                    .fillMaxWidth()
                    .then(if (minHeight > 0.dp) Modifier.height(minHeight) else Modifier)
            )
        }
    }
}

@Composable
private fun ToggleRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onChange: (Boolean) -> Unit
) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                title,
                color = TextMain,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                subtitle,
                color = TextDim,
                fontSize = 10.sp,
                lineHeight = 14.sp
            )
        }
        Spacer(Modifier.width(10.dp))
        Switch(
            checked = checked,
            onCheckedChange = onChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color(0xFF0A0A0F),
                checkedTrackColor = Accent,
                uncheckedThumbColor = TextDim,
                uncheckedTrackColor = Surface2,
                uncheckedBorderColor = Hairline
            )
        )
    }
}
