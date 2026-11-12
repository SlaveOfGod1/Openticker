package com.openticker.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.openticker.data.SystemWindowOptions
import com.openticker.ui.components.TickerSlider
import com.openticker.ui.i18n.t
import com.openticker.ui.theme.Hairline
import com.openticker.ui.theme.Accent
import com.openticker.ui.theme.Surface1
import com.openticker.ui.theme.Surface2
import com.openticker.ui.theme.TextDim
import com.openticker.ui.theme.TextMain
import kotlin.math.roundToInt

/**
 * Image-specific controls for the Your Image theme. Everything is free.
 */
@Composable
fun YourImageCustomizer(
    options: SystemWindowOptions,
    onChange: (SystemWindowOptions) -> Unit,
    onPickImage: () -> Unit,
    onRemoveImage: () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val hasImage = options.imagePath.isNotBlank()

    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(Surface1)
            .border(1.dp, Hairline, RoundedCornerShape(22.dp))
            .padding(16.dp)
    ) {
        Text(
            t("Your image"),
            color = TextMain,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(Modifier.height(14.dp))

        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                Modifier
                    .weight(1f)
                    .height(44.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Surface2)
                    .border(1.dp, Hairline, RoundedCornerShape(12.dp))
                    .clickable(onClick = onPickImage),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Filled.Image,
                    contentDescription = null,
                    tint = Accent,
                    modifier = Modifier.size(17.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    if (hasImage) t("Change image") else t("Choose image"),
                    color = TextMain,
                    fontSize = 12.5.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Box(
                Modifier
                    .height(44.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Surface2)
                    .border(1.dp, Hairline, RoundedCornerShape(12.dp))
                    .clickable(enabled = hasImage, onClick = onRemoveImage)
                    .padding(horizontal = 16.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    t("Remove"),
                    color = if (hasImage) TextMain else TextDim.copy(alpha = 0.45f),
                    fontSize = 12.5.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        Spacer(Modifier.height(14.dp))

        Row(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .clickable { expanded = !expanded }
                .padding(vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Filled.Tune,
                contentDescription = null,
                tint = TextDim,
                modifier = Modifier.size(17.dp)
            )
            Spacer(Modifier.width(9.dp))
            Text(
                t("Adjust Image"),
                color = TextMain,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(Modifier.weight(1f))
            Icon(
                if (expanded) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown,
                contentDescription = null,
                tint = TextDim,
                modifier = Modifier.size(19.dp)
            )
        }

        if (expanded) {
            Spacer(Modifier.height(8.dp))

            SettingHeader(
                title = t("Horizontal position"),
                value = "${options.imageX}%"
            )
            TickerSlider(
                value = options.imageX.toFloat(),
                onValueChange = { onChange(options.copy(imageX = it.roundToInt())) },
                valueRange = -10000f..10000f
            )

            Spacer(Modifier.height(10.dp))
            SettingHeader(
                title = t("Vertical position"),
                value = "${options.imageY}%"
            )
            TickerSlider(
                value = options.imageY.toFloat(),
                onValueChange = { onChange(options.copy(imageY = it.roundToInt())) },
                valueRange = -10000f..10000f
            )

            Spacer(Modifier.height(10.dp))
            SettingHeader(
                title = t("Scale"),
                value = "${options.imageScale}%"
            )
            TickerSlider(
                value = options.imageScale.toFloat(),
                onValueChange = { onChange(options.copy(imageScale = it.roundToInt())) },
                valueRange = 100f..10000f
            )

            Spacer(Modifier.height(10.dp))
            SettingHeader(
                title = t("Image opacity"),
                value = "${options.imageOpacity}%"
            )
            TickerSlider(
                value = options.imageOpacity.toFloat(),
                onValueChange = { onChange(options.copy(imageOpacity = it.roundToInt())) },
                valueRange = 0f..100f
            )

            Spacer(Modifier.height(10.dp))
            Text(
                t("Your image stays on this device. Position and scale apply to the floating notification."),
                color = TextDim,
                fontSize = 10.5.sp,
                lineHeight = 15.sp
            )
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
