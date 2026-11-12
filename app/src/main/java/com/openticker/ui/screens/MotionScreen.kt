package com.openticker.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.openticker.data.Prefs
import com.openticker.service.TickerOverlayService
import com.openticker.ticker.StyleRegistry
import com.openticker.ticker.TickerStyle
import com.openticker.ui.components.TickerSlider
import com.openticker.ui.components.IconTile
import com.openticker.ui.components.PillButton
import com.openticker.ui.components.ScreenHeader
import com.openticker.ui.components.MenuButton
import com.openticker.ui.i18n.t
import com.openticker.ui.theme.Hairline
import com.openticker.ui.theme.Ink
import com.openticker.ui.theme.Mint
import com.openticker.ui.theme.Accent
import com.openticker.ui.theme.Surface1
import com.openticker.ui.theme.Surface2
import com.openticker.ui.theme.TextDim
import com.openticker.ui.theme.TextMain
import java.util.Locale

@Composable
fun MotionScreen(onPreview: (TickerStyle) -> Unit, onMenu: () -> Unit) {
    val context = LocalContext.current
    var speed by remember { mutableFloatStateOf(Prefs.speed(context)) }
    var animationSpeed by remember { mutableFloatStateOf(Prefs.animationSpeed(context)) }
    var duration by remember { mutableFloatStateOf(Prefs.duration(context).toFloat()) }
    var width by remember { mutableFloatStateOf(Prefs.widthPercent(context).toFloat()) }
    val style = StyleRegistry.byId(Prefs.styleId(context))

    LazyColumn(
        modifier = Modifier.fillMaxSize().statusBarsPadding(),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 14.dp, bottom = 40.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            ScreenHeader(
                title = t("Motion"),
                subtitle = t("Tune how every ticker moves and rests"),
                trailing = { MenuButton(onClick = onMenu) }
            )
        }
        item { Spacer(Modifier.height(6.dp)) }

        item {
            MotionCard(
                title = t("Scroll speed"),
                valueLabel = String.format(Locale.US, "%.1fx", speed),
                description = t("How fast long messages glide across a single line.")
            ) {
                TickerSlider(
                    value = speed,
                    onValueChange = {
                        speed = it
                        Prefs.setSpeed(context, it)
                    },
                    valueRange = 0.5f..3f,
                    steps = 9
                )
            }
        }

        item {
            MotionCard(
                title = t("Animation speed"),
                valueLabel = String.format(Locale.US, "%.2fx", animationSpeed),
                description = t("How fast the theme plays its entrance and exit. Turn it up if a card feels too slow, down if it is too quick.")
            ) {
                TickerSlider(
                    value = animationSpeed,
                    onValueChange = {
                        animationSpeed = it
                        Prefs.setAnimationSpeed(context, it)
                    },
                    valueRange = 0.5f..2.5f,
                    steps = 7
                )
            }
        }

        item {
            MotionCard(
                title = t("On-screen time"),
                valueLabel = "${duration.toInt()}s",
                description = t("How long a ticker stays before it slides away. Faster speeds shorten it.")
            ) {
                TickerSlider(
                    value = duration,
                    onValueChange = {
                        duration = it
                        Prefs.setDuration(context, it.toInt())
                    },
                    valueRange = 3f..15f,
                    steps = 11
                )
            }
        }

        item {
            MotionCard(
                title = t("Ticker width"),
                valueLabel = "${width.toInt()}%",
                description = t("Narrower tickers feel more ambient; full width fits more text.")
            ) {
                TickerSlider(
                    value = width,
                    onValueChange = {
                        width = it
                        Prefs.setWidthPercent(context, it.toInt())
                        TickerOverlayService.refreshPosition()
                    },
                    valueRange = 70f..100f,
                    steps = 5
                )
            }
        }

        item {
            Row(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(22.dp))
                    .background(Surface1)
                    .border(1.dp, Hairline, RoundedCornerShape(22.dp))
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconTile(icon = style.icon, accent = style.accent, size = 42)
                Spacer(Modifier.width(11.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        style.name,
                        color = TextMain,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        t("Current style"),
                        color = TextDim,
                        fontSize = 10.5.sp
                    )
                }
                PillButton(text = t("Preview"), icon = Icons.Filled.PlayArrow, onClick = {
                    onPreview(style)
                })
            }
        }

        item {
            Column(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(22.dp))
                    .background(Accent.copy(alpha = 0.07f))
                    .border(1.dp, Accent.copy(alpha = 0.22f), RoundedCornerShape(22.dp))
                    .padding(16.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        Modifier
                            .width(3.dp)
                            .height(16.dp)
                            .clip(RoundedCornerShape(50))
                            .background(Accent)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        t("Long messages"),
                        color = TextMain,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                Spacer(Modifier.height(7.dp))
                Text(
                    t("Tickers stay on one line and glide past until the full message has been shown. " +
                        "Nothing gets truncated, nothing gets stacked on top of your screen."),
                    color = TextDim,
                    fontSize = 11.5.sp,
                    lineHeight = 17.sp
                )
            }
        }
    }
}

@Composable
private fun MotionCard(
    title: String,
    valueLabel: String,
    description: String,
    content: @Composable () -> Unit
) {
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
                title,
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
                    valueLabel,
                    color = TextMain,
                    fontSize = 10.5.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
        Spacer(Modifier.height(2.dp))
        content()
        Text(
            description,
            color = TextDim,
            fontSize = 11.sp,
            lineHeight = 16.sp
        )
    }
}
