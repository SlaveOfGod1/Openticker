package com.openticker.ui.screens

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmap
import com.openticker.data.Prefs
import com.openticker.ui.components.ScreenHeader
import com.openticker.ui.components.MenuButton
import com.openticker.ui.components.SearchPill
import com.openticker.ui.i18n.t
import com.openticker.ui.components.SectionLabel
import com.openticker.ui.theme.Hairline
import com.openticker.ui.theme.Ink
import com.openticker.ui.theme.Accent
import com.openticker.ui.theme.Surface1
import com.openticker.ui.theme.Surface3
import com.openticker.ui.theme.TextDim
import com.openticker.ui.theme.TextMain
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class InstalledApp(
    val packageName: String,
    val label: String,
    val icon: Bitmap?
)

@Composable
fun AppsScreen(onMenu: () -> Unit) {
    val context = LocalContext.current
    var query by rememberSaveable { mutableStateOf("") }

    val apps by produceState(initialValue = emptyList<InstalledApp>()) {
        value = withContext(Dispatchers.IO) { loadInstalledApps(context) }
    }

    val blocked = remember {
        mutableStateListOf<String>().apply { addAll(Prefs.blockedApps(context)) }
    }

    val filtered = remember(query, apps) {
        if (query.isBlank()) apps
        else apps.filter {
            it.label.contains(query, ignoreCase = true) ||
                it.packageName.contains(query, ignoreCase = true)
        }
    }

    val allowedCount = apps.count { !blocked.contains(it.packageName) }

    LazyColumn(
        modifier = Modifier.fillMaxSize().statusBarsPadding(),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 14.dp, bottom = 40.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            ScreenHeader(
                title = t("Apps"),
                subtitle = t("Choose which apps appear as tickers"),
                trailing = { MenuButton(onClick = onMenu) }
            )
        }
        item { Spacer(Modifier.height(6.dp)) }
        item {
            SearchPill(
                value = query,
                onValueChange = { query = it },
                placeholder = t("Search apps")
            )
        }
        item {
            Row(
                Modifier.fillMaxWidth().padding(top = 6.dp, bottom = 2.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                SectionLabel(t("Allowed apps"))
                Spacer(Modifier.weight(1f))
                Text(
                    "$allowedCount of ${apps.size}",
                    color = TextDim.copy(alpha = 0.7f),
                    fontSize = 10.sp
                )
            }
        }
        items(filtered, key = { it.packageName }) { app ->
            AppRow(
                app = app,
                allowed = !blocked.contains(app.packageName),
                onToggle = { allowed ->
                    Prefs.setAppAllowed(context, app.packageName, allowed)
                    if (allowed) blocked.remove(app.packageName)
                    else if (!blocked.contains(app.packageName)) blocked.add(app.packageName)
                }
            )
        }
        if (apps.isNotEmpty() && filtered.isEmpty()) {
            item {
                Text(
                    t("No apps match “query”").replace("query", query),
                    color = TextDim,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(top = 24.dp)
                )
            }
        }
        item {
            Text(
                t("System apps and OpenTicker itself are always hidden from the ticker. " +
                    "Everything stays on your device."),
                color = TextDim.copy(alpha = 0.7f),
                fontSize = 11.sp,
                lineHeight = 16.sp,
                modifier = Modifier.padding(top = 12.dp, start = 2.dp, end = 2.dp)
            )
        }
    }
}

@Composable
private fun AppRow(
    app: InstalledApp,
    allowed: Boolean,
    onToggle: (Boolean) -> Unit
) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(Surface1)
            .border(1.dp, Hairline, RoundedCornerShape(18.dp))
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (app.icon != null) {
            Image(
                bitmap = app.icon.asImageBitmap(),
                contentDescription = app.label,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(38.dp)
                    .clip(RoundedCornerShape(11.dp))
            )
        } else {
            Box(
                Modifier
                    .size(38.dp)
                    .clip(RoundedCornerShape(11.dp))
                    .background(Surface3),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    app.label.take(1).uppercase(),
                    color = TextDim,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                app.label,
                color = TextMain,
                fontSize = 13.5.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1
            )
            Text(
                app.packageName,
                color = TextDim.copy(alpha = 0.65f),
                fontSize = 9.5.sp,
                maxLines = 1
            )
        }
        Switch(
            checked = allowed,
            onCheckedChange = onToggle,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Ink,
                checkedTrackColor = Accent,
                uncheckedThumbColor = TextDim,
                uncheckedTrackColor = Surface3,
                uncheckedBorderColor = Hairline
            )
        )
    }
}

private fun loadInstalledApps(context: Context): List<InstalledApp> {
    val pm = context.packageManager
    val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
    val infos = runCatching {
        pm.queryIntentActivities(intent, 0)
    }.getOrDefault(emptyList())

    return infos.asSequence()
        .map { it.activityInfo.applicationInfo }
        .distinctBy { it.packageName }
        .filter { it.packageName != context.packageName }
        .map { info ->
            InstalledApp(
                packageName = info.packageName,
                label = runCatching {
                    pm.getApplicationLabel(info).toString()
                }.getOrDefault(info.packageName),
                icon = runCatching {
                    pm.getApplicationIcon(info).toBitmap(width = 96, height = 96)
                }.getOrNull()
            )
        }
        .sortedBy { it.label.lowercase() }
        .toList()
}
