package com.openticker.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.openticker.R
import com.openticker.ui.i18n.t
import com.openticker.ui.theme.Hairline
import com.openticker.ui.theme.Ink
import com.openticker.ui.theme.Mint
import com.openticker.ui.theme.Night
import com.openticker.ui.theme.Accent
import com.openticker.ui.theme.Surface1
import com.openticker.ui.theme.Surface2
import com.openticker.ui.theme.Surface3
import com.openticker.ui.theme.TextDim
import com.openticker.ui.theme.TextMain

/** Deep black plus the faint contour-line brand texture. */
@Composable
fun BrandBackdrop() {
    Canvas(Modifier.fillMaxSize()) {
        drawRect(Color.Black)
        for (i in 1..7) {
            drawCircle(
                color = Color.White.copy(alpha = 0.016f),
                radius = size.width * 0.16f * i,
                center = Offset(size.width * 0.05f, size.height * 0.04f),
                style = Stroke(width = 1.2f)
            )
        }
    }
}

/** The OpenTicker speech-bubble mark, drawn in Compose. */
@Composable
fun LogoMark(size: Int = 42) {
    Image(
        painter = painterResource(R.drawable.logo_openticker),
        contentDescription = "OpenTicker",
        contentScale = ContentScale.Crop,
        modifier = Modifier
            .size(size.dp)
            .clip(RoundedCornerShape((size * 0.32f).dp))
    )
}


@Composable
fun SearchPill(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String = t("Search styles"),
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(50))
            .background(Surface1)
            .border(1.dp, Hairline, RoundedCornerShape(50))
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            Icons.Filled.Search,
            contentDescription = null,
            tint = TextDim,
            modifier = Modifier.size(18.dp)
        )
        Spacer(Modifier.width(10.dp))
        Box(Modifier.weight(1f)) {
            if (value.isEmpty()) {
                Text(placeholder, color = TextDim.copy(alpha = 0.75f), fontSize = 14.sp)
            }
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                textStyle = TextStyle(color = Color.White, fontSize = 14.sp),
                cursorBrush = SolidColor(Accent),
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        }
        if (value.isNotEmpty()) {
            Icon(
                Icons.Filled.Close,
                contentDescription = t("Clear"),
                tint = TextDim,
                modifier = Modifier
                    .size(18.dp)
                    .clickable { onValueChange("") }
            )
        }
    }
}


@Composable
fun ScreenHeader(
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
    trailing: (@Composable () -> Unit)? = null
) {
    Row(modifier, verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(
                title,
                color = TextMain,
                fontSize = 27.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = (-0.8).sp
            )
            Spacer(Modifier.height(3.dp))
            Text(subtitle, color = TextDim, fontSize = 11.5.sp)
        }
        if (trailing != null) {
            Spacer(Modifier.width(12.dp))
            trailing()
        }
    }
}

/**
 * The big centred page title with the burger in the corner — used by the
 * pages that lead with a single word.
 */
@Composable
fun ScreenTitle(
    title: String,
    subtitle: String? = null,
    onMenu: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Spacer(Modifier.width(42.dp))
        Column(
            Modifier.weight(1f),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                title,
                color = TextMain,
                fontSize = 27.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = (-0.8).sp
            )
            if (subtitle != null) {
                Spacer(Modifier.height(2.dp))
                Text(subtitle, color = TextDim, fontSize = 11.sp)
            }
        }
        Spacer(Modifier.width(42.dp))
        MenuButton(onClick = onMenu)
    }
}

@Composable
fun SectionLabel(text: String, modifier: Modifier = Modifier) {
    Text(
        text.uppercase(),
        color = TextDim,
        fontSize = 10.sp,
        fontWeight = FontWeight.SemiBold,
        letterSpacing = 1.8.sp,
        modifier = modifier
    )
}

@Composable
fun TickerSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    valueRange: ClosedFloatingPointRange<Float>,
    steps: Int = 0,
    modifier: Modifier = Modifier
) {
    Slider(
        value = value,
        onValueChange = onValueChange,
        valueRange = valueRange,
        steps = steps,
        modifier = modifier,
        colors = SliderDefaults.colors(
            thumbColor = Accent,
            activeTrackColor = Accent,
            inactiveTrackColor = Surface3,
            activeTickColor = Color.Transparent,
            inactiveTickColor = Color.Transparent
        )
    )
}

@Composable
fun PermissionCard(
    title: String,
    subtitle: String,
    granted: Boolean,
    actionLabel: String,
    onAction: () -> Unit
) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(Surface1)
            .border(1.dp, Hairline, RoundedCornerShape(20.dp))
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier
                .size(34.dp)
                .clip(CircleShape)
                .background(if (granted) Mint.copy(alpha = 0.16f) else Accent.copy(alpha = 0.14f)),
            contentAlignment = Alignment.Center
        ) {
            if (granted) {
                Icon(
                    Icons.Filled.Check,
                    contentDescription = null,
                    tint = Mint,
                    modifier = Modifier.size(18.dp)
                )
            } else {
                Box(
                    Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(Accent)
                )
            }
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(title, color = TextMain, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(2.dp))
            Text(subtitle, color = TextDim, fontSize = 11.sp, lineHeight = 15.sp)
        }
        Spacer(Modifier.width(10.dp))
        Box(
            Modifier
                .clip(RoundedCornerShape(50))
                .background(if (granted) Surface2 else Accent)
                .clickable(enabled = !granted, onClick = onAction)
                .padding(horizontal = 13.dp, vertical = 8.dp)
        ) {
            Text(
                if (granted) "On" else actionLabel,
                color = if (granted) TextDim else Color(0xFF0A0A0F),
                fontSize = 10.5.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun IconTile(
    icon: ImageVector,
    accent: Color,
    size: Int = 44,
    modifier: Modifier = Modifier
) {
    Box(
        modifier
            .size(size.dp)
            .clip(RoundedCornerShape((size * 0.32f).dp))
            .background(
                Brush.linearGradient(
                    listOf(accent.copy(alpha = 0.32f), accent.copy(alpha = 0.10f))
                )
            )
            .border(1.dp, accent.copy(alpha = 0.28f), RoundedCornerShape((size * 0.32f).dp)),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = accent,
            modifier = Modifier.size((size * 0.46f).dp)
        )
    }
}

@Composable
fun ThinDivider(modifier: Modifier = Modifier) {
    Box(
        modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(Hairline)
    )
}

@Composable
fun PillButton(
    text: String,
    icon: ImageVector? = null,
    filled: Boolean = true,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier
            .clip(RoundedCornerShape(50))
            .background(if (filled) Accent else Surface2)
            .border(
                1.dp,
                if (filled) Color.Transparent else Hairline,
                RoundedCornerShape(50)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        if (icon != null) {
            Icon(
                icon,
                contentDescription = null,
                tint = if (filled) Color(0xFF0A0A0F) else TextMain,
                modifier = Modifier.size(14.dp)
            )
            Spacer(Modifier.width(5.dp))
        }
        Text(
            text,
            color = if (filled) Color(0xFF0A0A0F) else TextMain,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold
        )
    }
}
