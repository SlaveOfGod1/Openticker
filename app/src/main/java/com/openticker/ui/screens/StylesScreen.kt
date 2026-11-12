package com.openticker.ui.screens

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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.openticker.data.DemoData
import com.openticker.data.Prefs
import com.openticker.data.TickerData
import com.openticker.ticker.StyleRegistry
import com.openticker.ticker.TickerStyle
import com.openticker.ticker.TickerSurface
import com.openticker.ticker.cards.CardCatalog
import com.openticker.ui.components.LogoMark
import com.openticker.ui.components.MenuButton
import com.openticker.ui.components.ScreenTitle
import com.openticker.ui.components.SearchPill
import com.openticker.ui.components.SectionLabel
import com.openticker.ui.i18n.t
import com.openticker.ui.theme.Hairline
import com.openticker.ui.theme.Accent
import com.openticker.ui.theme.Surface1
import com.openticker.ui.theme.TextDim
import com.openticker.ui.theme.TextMain

@Composable
fun StylesScreen(
    onOpen: (TickerStyle) -> Unit,
    onTest: (TickerStyle) -> Unit,
    onUse: (TickerStyle) -> Unit,
    onMenu: () -> Unit
) {
    val context = LocalContext.current
    var query by rememberSaveable { mutableStateOf("") }
    var activeId by remember { mutableStateOf(Prefs.styleId(context)) }

    val filtered = remember(query) {
        StyleRegistry.styles.filter { style ->
            query.isBlank() || style.name.contains(query, ignoreCase = true)
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize().statusBarsPadding(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 14.dp, bottom = 40.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            ScreenTitle(
                title = t("Styles"),
                onMenu = onMenu
            )
        }
        item {
            SearchPill(value = query, onValueChange = { query = it })
        }
        items(filtered, key = { it.id }) { style ->
            StyleScene(
                style = style,
                activeId = activeId,
                onClick = { onOpen(style) },
                onTest = { onTest(style) },
                onUse = {
                    Prefs.setStyleId(context, it.id)
                    activeId = it.id
                    onUse(it)
                }
            )
        }
    }
}

/**
 * One style: its name and the buttons on top, and the notification itself
 * rendered live underneath so you can see the scene before opening it.
 */
@Composable
private fun StyleScene(
    style: TickerStyle,
    activeId: String,
    onClick: () -> Unit,
    onTest: () -> Unit,
    onUse: (TickerStyle) -> Unit
) {
    val data = rememberSceneData(style)

    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(Surface1)
            .border(1.dp, Hairline, RoundedCornerShape(22.dp))
            .clickable(onClick = onClick)
            .padding(12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                style.name,
                color = TextMain,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            Spacer(Modifier.width(8.dp))
            Row(
                Modifier
                    .clip(RoundedCornerShape(50))
                    .background(Color.White.copy(alpha = 0.07f))
                    .border(1.dp, Color.White.copy(alpha = 0.16f), RoundedCornerShape(50))
                    .clickable(onClick = onTest)
                    .padding(horizontal = 11.dp, vertical = 7.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Filled.PlayArrow,
                    contentDescription = null,
                    tint = TextMain,
                    modifier = Modifier.size(12.dp)
                )
                Spacer(Modifier.width(3.dp))
                Text(
                    t("Test"),
                    color = TextMain,
                    fontSize = 10.5.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(Modifier.width(6.dp))
            val isActive = activeId == style.id
            Row(
                Modifier
                    .clip(RoundedCornerShape(50))
                    .background(
                        if (isActive) Color.White.copy(alpha = 0.16f)
                        else Color.Transparent
                    )
                    .border(
                        1.dp,
                        if (isActive) Color.White.copy(alpha = 0.34f)
                        else Color.White.copy(alpha = 0.16f),
                        RoundedCornerShape(50)
                    )
                    .clickable { onUse(style) }
                    .padding(horizontal = 11.dp, vertical = 7.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Filled.Check,
                    contentDescription = null,
                    tint = if (isActive) TextMain else TextDim,
                    modifier = Modifier.size(12.dp)
                )
                Spacer(Modifier.width(3.dp))
                Text(
                    if (isActive) t("Active") else t("Use this"),
                    color = if (isActive) TextMain else TextDim,
                    fontSize = 10.5.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
        Spacer(Modifier.height(10.dp))
        Box(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(Color(0xFF06060A))
        ) {
            TickerSurface(style = style, data = data, interactions = null)
        }
    }
}

/** The demo notification a style is previewed with. */
@Composable
private fun rememberSceneData(style: TickerStyle): TickerData {
    val context = LocalContext.current
    return remember(style.id) {
        when {
            style.id == "system_window" -> DemoData.systemWindowPreview(Prefs.systemWindow(context))
            style.id == "aurora" -> DemoData.systemWindowPreview(Prefs.aurora(context))
            style.id == "saturday_morning" -> DemoData.systemWindowPreview(Prefs.saturdayMorning(context))
            style.id == "your_image" -> DemoData.systemWindowPreview(Prefs.yourImage(context))
            style.id == "radio_dial" -> DemoData.systemWindowPreview(Prefs.radioDial(context))
            style.id == "aquatic" -> DemoData.systemWindowPreview(Prefs.aquatic(context))
            style.id in CardCatalog.ids ->
                DemoData.cardPreview(Prefs.themeOptions(context, style.id, style.accent.toArgb()))
            else -> DemoData.sampleFor(style.id)
        }
    }
}
