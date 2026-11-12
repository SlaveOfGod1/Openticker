package com.openticker.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.openticker.data.TickerData
import com.openticker.ui.theme.Accent

/** App icon with a graceful letter fallback for demo notifications. */
@Composable
fun AppBadge(
    data: TickerData,
    size: Dp,
    background: Color = Accent,
    letterColor: Color = Color(0xFF0A0A0F)
) {
    val bitmap = data.icon
    if (bitmap != null) {
        Image(
            bitmap = bitmap.asImageBitmap(),
            contentDescription = data.appName,
            contentScale = ContentScale.Crop,
            modifier = Modifier.size(size).clip(CircleShape)
        )
    } else {
        Box(
            modifier = Modifier.size(size).clip(CircleShape).background(background),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = data.appName.take(1).uppercase(),
                color = letterColor,
                fontWeight = FontWeight.Black,
                fontSize = (size.value * 0.42f).sp
            )
        }
    }
}

