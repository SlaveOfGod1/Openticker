package com.openticker.ui.screens

import android.Manifest
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
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
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.NotificationManagerCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.openticker.data.Prefs
import com.openticker.service.TickerOverlayService
import com.openticker.ui.components.LogoMark
import com.openticker.ui.components.MenuButton
import com.openticker.ui.components.PermissionCard
import com.openticker.ui.i18n.I18n
import com.openticker.ui.i18n.t
import com.openticker.ui.components.ScreenHeader
import com.openticker.ui.components.SectionLabel
import com.openticker.ui.components.TickerSlider
import com.openticker.ui.theme.Hairline
import com.openticker.ui.theme.Ink
import com.openticker.ui.theme.Mint
import com.openticker.ui.theme.Accent
import com.openticker.ui.theme.Surface1
import com.openticker.ui.theme.Surface3
import com.openticker.ui.theme.TextDim
import com.openticker.ui.theme.TextMain
import kotlin.math.roundToInt

@Composable
fun SettingsScreen(onTest: () -> Unit, onMenu: () -> Unit, onLanguageChanged: (String) -> Unit) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var listenerGranted by remember { mutableStateOf(hasNotificationAccess(context)) }
    var overlayGranted by remember { mutableStateOf(Settings.canDrawOverlays(context)) }
    var batteryExempt by remember { mutableStateOf(batteryExempted(context)) }
    var notifGranted by remember {
        mutableStateOf(NotificationManagerCompat.from(context).areNotificationsEnabled())
    }
    val notifLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        notifGranted = granted
    }
    var enabled by remember { mutableStateOf(Prefs.isEnabled(context)) }
    var overlayOffset by remember { mutableIntStateOf(Prefs.overlayOffsetY(context)) }
    var fontSize by remember { mutableFloatStateOf(Prefs.fontSize(context)) }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                listenerGranted = hasNotificationAccess(context)
                overlayGranted = Settings.canDrawOverlays(context)
                batteryExempt = batteryExempted(context)
                notifGranted = NotificationManagerCompat.from(context).areNotificationsEnabled()
                enabled = Prefs.isEnabled(context)
                overlayOffset = Prefs.overlayOffsetY(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize().statusBarsPadding(),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 14.dp, bottom = 40.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            ScreenHeader(
                title = t("Settings"),
                subtitle = t("Permissions, ticker position and privacy"),
                trailing = { MenuButton(onClick = onMenu) }
            )
        }
        item { Spacer(Modifier.height(6.dp)) }

        item {
            SectionLabel(t("Access"))
        }
        item {
            PermissionCard(
                title = t("Notification access"),
                subtitle = t("Required to read notifications. OpenTicker never uploads them."),
                granted = listenerGranted,
                actionLabel = t("Enable"),
                onAction = {
                    context.startActivity(
                        Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
                            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    )
                }
            )
        }
        item {
            PermissionCard(
                title = t("Display over other apps"),
                subtitle = t("Lets tickers float above games, video and everything else."),
                granted = overlayGranted,
                actionLabel = t("Allow"),
                onAction = {
                    context.startActivity(
                        Intent(
                            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                            Uri.parse("package:${context.packageName}")
                        ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    )
                }
            )
        }
        item {
            PermissionCard(
                title = t("Unrestricted battery"),
                subtitle = t("Samsung kills background apps without this — tickers would stop arriving."),
                granted = batteryExempt,
                actionLabel = t("Exempt"),
                onAction = {
                    runCatching {
                        context.startActivity(
                            Intent(
                                Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS,
                                Uri.parse("package:${context.packageName}")
                            ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        )
                    }
                }
            )
        }
        item {
            PermissionCard(
                title = t("Show notifications"),
                subtitle = t("Lets OpenTicker keep one mirrored entry in the shade after the ticker."),
                granted = notifGranted,
                actionLabel = t("Allow"),
                onAction = {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        notifLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    } else {
                        runCatching {
                            context.startActivity(
                                Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                                    .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            )
                        }
                    }
                }
            )
        }

        item { Spacer(Modifier.height(6.dp)) }
        item { SectionLabel(t("Ticker")) }

        item {
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
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(if (enabled) Mint else TextDim.copy(alpha = 0.4f))
                )
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        t("Ticker enabled"),
                        color = TextMain,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        if (enabled) t("Notifications tick quietly") else t("Paused — nothing will appear"),
                        color = TextDim,
                        fontSize = 11.sp
                    )
                }
                Switch(
                    checked = enabled,
                    onCheckedChange = {
                        enabled = it
                        Prefs.setEnabled(context, it)
                    },
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

        item {
            Row(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .background(Accent)
                    .clickable(onClick = onTest)
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
                    t("Send a test ticker"),
                    color = Ink,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        item {
            FontSizeCard(
                value = fontSize,
                onValueChange = {
                    fontSize = it
                    Prefs.setFontSize(context, it)
                }
            )
        }

        item { Spacer(Modifier.height(6.dp)) }
        item { SectionLabel(t("Language")) }

        item {
            LanguageCard(
                current = Prefs.language(context),
                onChange = { tag ->
                    Prefs.setLanguage(context, tag)
                    onLanguageChanged(tag)
                }
            )
        }

        item { Spacer(Modifier.height(6.dp)) }
        item { SectionLabel(t("Position")) }

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
                        t("Vertical position"),
                        color = TextMain,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(Modifier.weight(1f))
                    Box(
                        Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(Surface3)
                            .padding(horizontal = 9.dp, vertical = 4.dp)
                    ) {
                        Text(
                            "${if (overlayOffset >= 0) "+" else ""}$overlayOffset dp",
                            color = TextMain,
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
                Spacer(Modifier.height(2.dp))
                TickerSlider(
                    value = overlayOffset.toFloat(),
                    onValueChange = {
                        val next = it.roundToInt()
                        overlayOffset = next
                        Prefs.setOverlayOffsetY(context, next)
                        TickerOverlayService.refreshPosition()
                    },
                    valueRange = -20f..140f,
                    steps = 15
                )
                Text(
                    t("Negative values raise the ticker, positive values lower it. The default is +10 dp."),
                    color = TextDim,
                    fontSize = 11.sp,
                    lineHeight = 16.sp
                )
            }
        }

        item { Spacer(Modifier.height(6.dp)) }

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
                LogoMark(44)
                Spacer(Modifier.width(13.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        "OpenTicker",
                        color = TextMain,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        t("Version 1.0.1 · Local only"),
                        color = TextDim,
                        fontSize = 11.sp
                    )
                }
                Box(
                    Modifier
                        .clip(RoundedCornerShape(50))
                        .background(Mint.copy(alpha = 0.10f))
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Text(
                        t("PRIVACY FIRST"),
                        color = Mint,
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                }
            }
        }

        item {
            Text(
                t("Your notifications are processed on this device and never uploaded, sold or used for ads. " +
                    "OpenTicker does not replace Android's notification shade — it is simply a calmer way to see what arrived."),
                color = TextDim.copy(alpha = 0.7f),
                fontSize = 11.sp,
                lineHeight = 16.sp,
                modifier = Modifier.padding(top = 4.dp, start = 2.dp, end = 2.dp)
            )
        }
    }
}

private fun hasNotificationAccess(context: Context): Boolean =
    NotificationManagerCompat.getEnabledListenerPackages(context)
        .contains(context.packageName)

private fun batteryExempted(context: Context): Boolean {
    val pm = context.getSystemService(Context.POWER_SERVICE) as PowerManager
    return pm.isIgnoringBatteryOptimizations(context.packageName)
}

/** The list of languages the app can speak. */
@Composable
private fun LanguageCard(current: String, onChange: (String) -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(Surface1)
            .border(1.dp, Hairline, RoundedCornerShape(22.dp))
            .padding(16.dp)
    ) {
        Text(
            t("Choose the language the app speaks."),
            color = TextDim,
            fontSize = 11.sp,
            lineHeight = 16.sp
        )
        Spacer(Modifier.height(12.dp))
        I18n.languages.forEach { language ->
            val selected = language.tag == current
            val label = if (language.tag == "system") {
                t("System default")
            } else {
                language.nativeName
            }
            Row(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (selected) Color.White.copy(alpha = 0.09f) else Color.Transparent)
                    .clickable { onChange(language.tag) }
                    .padding(horizontal = 12.dp, vertical = 11.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    label,
                    color = if (selected) TextMain else TextDim,
                    fontSize = 13.5.sp,
                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
                )
                Spacer(Modifier.weight(1f))
                if (selected) {
                    Box(
                        Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(TextMain)
                    )
                }
            }
        }
    }
}

@Composable
private fun FontSizeCard(value: Float, onValueChange: (Float) -> Unit) {
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
                t("Font size"),
                color = TextMain,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(Modifier.weight(1f))
            Box(
                Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(Surface3)
                    .padding(horizontal = 9.dp, vertical = 4.dp)
            ) {
                Text(
                    formatSp(value) + " sp",
                    color = TextMain,
                    fontSize = 10.5.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
        Spacer(Modifier.height(2.dp))
        TickerSlider(
            value = value,
            onValueChange = onValueChange,
            valueRange = 10f..18f,
            steps = 15
        )
        Text(
            t("Applies to the message text in every theme. The default is 13 sp."),
            color = TextDim,
            fontSize = 11.sp,
            lineHeight = 16.sp
        )
    }
}

private fun formatSp(value: Float): String =
    if (value % 1f == 0f) value.toInt().toString()
    else String.format(java.util.Locale.US, "%.1f", value)