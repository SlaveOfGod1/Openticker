package com.openticker.ui.screens

import android.content.Intent
import android.net.Uri
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
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Policy
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.openticker.ui.components.LogoMark
import com.openticker.ui.components.ScreenTitle
import com.openticker.ui.i18n.t
import com.openticker.ui.theme.Hairline
import com.openticker.ui.theme.Surface1
import com.openticker.ui.theme.TextDim
import com.openticker.ui.theme.TextMain

/** Where the project lives. */
private const val REPO_URL = "https://github.com/SlaveOfGod1/Openticker"
private const val ISSUES_URL = "https://github.com/SlaveOfGod1/Openticker/issues"
private const val LICENSE_URL = "https://github.com/SlaveOfGod1/Openticker/blob/main/LICENSE"

@Composable
fun AboutScreen(onMenu: () -> Unit) {
    val context = LocalContext.current

    fun open(url: String) {
        runCatching {
            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize().statusBarsPadding(),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 14.dp, bottom = 40.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            ScreenTitle(
                title = t("About"),
                onMenu = onMenu
            )
        }

        item {
            Column(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(22.dp))
                    .background(Surface1)
                    .border(1.dp, Hairline, RoundedCornerShape(22.dp))
                    .padding(22.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                LogoMark(78)
                Spacer(Modifier.height(14.dp))
                Text(
                    "OpenTicker",
                    color = TextMain,
                    fontSize = 21.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = (-0.4).sp
                )
                Spacer(Modifier.height(3.dp))
                Text(
                    t("Version 1.0.1"),
                    color = TextDim,
                    fontSize = 11.5.sp
                )
                Spacer(Modifier.height(14.dp))
                Text(
                    t("Notifications, but better"),
                    color = TextDim,
                    fontSize = 12.sp
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
            ) {
                AboutRow(
                    icon = Icons.Filled.Code,
                    title = t("Source code"),
                    subtitle = t("See the project on GitHub"),
                    onClick = { open(REPO_URL) }
                )
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(Hairline)
                )
                AboutRow(
                    icon = Icons.Filled.BugReport,
                    title = t("Report a bug"),
                    subtitle = t("Open an issue on GitHub"),
                    onClick = { open(ISSUES_URL) }
                )
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(Hairline)
                )
                AboutRow(
                    icon = Icons.Filled.Policy,
                    title = t("Licence"),
                    subtitle = t("MIT License"),
                    onClick = { open(LICENSE_URL) }
                )
            }
        }

        item {
            Text(
                t("OpenTicker reads notifications on your device and floats them over " +
                    "other apps. Nothing is uploaded, nothing is collected."),
                color = TextDim.copy(alpha = 0.75f),
                fontSize = 11.5.sp,
                lineHeight = 17.sp
            )
        }
    }
}

@Composable
private fun AboutRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 15.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.07f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = TextMain,
                modifier = Modifier.size(18.dp)
            )
        }
        Spacer(Modifier.width(13.dp))
        Column(Modifier.weight(1f)) {
            Text(
                title,
                color = TextMain,
                fontSize = 14.5.sp,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                subtitle,
                color = TextDim,
                fontSize = 11.sp
            )
        }
        Icon(
            Icons.Filled.OpenInNew,
            contentDescription = null,
            tint = TextDim.copy(alpha = 0.7f),
            modifier = Modifier.size(15.dp)
        )
    }
}
