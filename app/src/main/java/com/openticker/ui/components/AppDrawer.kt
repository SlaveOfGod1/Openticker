package com.openticker.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.openticker.ui.i18n.t
import com.openticker.ui.theme.TextDim
import com.openticker.ui.theme.TextMain

/** The five places the drawer can take you. */
data class DrawerEntry(val label: String, val icon: ImageVector)

@Composable
fun drawerEntries(): List<DrawerEntry> = listOf(
    DrawerEntry(t("Styles"), Icons.Filled.GridView),
    DrawerEntry(t("Apps"), Icons.Filled.Apps),
    DrawerEntry(t("Motions"), Icons.Filled.Tune),
    DrawerEntry(t("Settings"), Icons.Filled.Settings),
    DrawerEntry(t("About"), Icons.Filled.Info)
)

/** The burger in the top corner of every screen. */
@Composable
fun MenuButton(onClick: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier
            .size(42.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(Color(0xFF111117))
            .border(1.dp, Color.White.copy(alpha = 0.10f), RoundedCornerShape(14.dp))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            Icons.Filled.Menu,
            contentDescription = "Menu",
            tint = TextMain,
            modifier = Modifier.size(20.dp)
        )
    }
}

/**
 * The navigation drawer. It always slides in from the **left** and always
 * reads left-to-right, whatever the device language is — so the mark, the
 * names and the icons always sit where you expect them.
 */
@Composable
fun AppDrawer(
    open: Boolean,
    selected: Int,
    onSelect: (Int) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(modifier.fillMaxSize()) {
        // The scrim. While the drawer is closed this does not exist at all,
        // so it can never swallow a tap or a drag.
        AnimatedVisibility(
            visible = open,
            enter = fadeIn(tween(180)),
            exit = fadeOut(tween(150))
        ) {
            Box(
                Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.6f))
                    .clickable { onDismiss() }
            )
        }

        // The panel. Forced left-to-right and always pinned to the left edge,
        // whatever direction the device is in.
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
            AnimatedVisibility(
                visible = open,
                enter = slideInHorizontally(tween(250)) { -it },
                exit = slideOutHorizontally(tween(190)) { -it },
                modifier = Modifier.fillMaxSize()
            ) {
                Row(Modifier.fillMaxSize()) {
                    Column(
                        Modifier
                            .fillMaxHeight()
                            .width(272.dp)
                            .background(Color(0xFF14141B))
                            .border(
                                1.dp,
                                Color.White.copy(alpha = 0.12f),
                                RoundedCornerShape(topEnd = 22.dp, bottomEnd = 22.dp)
                            )
                            .statusBarsPadding()
                            .padding(top = 10.dp)
                    ) {
                    Row(
                        Modifier.padding(horizontal = 20.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        LogoMark(44)
                        Spacer(Modifier.width(13.dp))
                        Column {
                            Text(
                                "OpenTicker",
                                color = TextMain,
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Black,
                                letterSpacing = (-0.5).sp
                            )
                            Text(
                                t("Notifications, but better"),
                                color = TextDim,
                                fontSize = 10.5.sp
                            )
                        }
                    }

                    Spacer(Modifier.height(20.dp))
                    ThinDivider(Modifier.padding(horizontal = 20.dp))
                    Spacer(Modifier.height(10.dp))

                    drawerEntries().forEachIndexed { index, entry ->
                        val active = index == selected
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 2.dp)
                                .clip(RoundedCornerShape(15.dp))
                                .background(
                                    if (active) Color.White.copy(alpha = 0.10f)
                                    else Color.Transparent
                                )
                                .clickable { onSelect(index) }
                                .padding(horizontal = 13.dp, vertical = 13.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                entry.icon,
                                contentDescription = null,
                                tint = if (active) TextMain else TextDim,
                                modifier = Modifier.size(19.dp)
                            )
                            Spacer(Modifier.width(13.dp))
                            Text(
                                entry.label,
                                color = if (active) TextMain else TextDim,
                                fontSize = 14.5.sp,
                                fontWeight = if (active) FontWeight.Bold else FontWeight.Medium
                            )
                            Spacer(Modifier.weight(1f))
                            if (active) {
                                Box(
                                    Modifier
                                        .size(6.dp)
                                        .clip(CircleShape)
                                        .background(TextMain)
                                )
                            }
                        }
                    }

                    Spacer(Modifier.weight(1f))
                    Text(
                        "v1.0.1",
                        color = TextDim.copy(alpha = 0.55f),
                        fontSize = 10.sp,
                        letterSpacing = 1.4.sp,
                        modifier = Modifier.padding(start = 33.dp, bottom = 22.dp)
                    )
                }
                Spacer(Modifier.weight(1f))
                }
            }
        }
    }
}
