package com.openticker.ui

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import com.openticker.data.DemoData
import com.openticker.data.Prefs
import com.openticker.service.TickerOverlayService
import com.openticker.ticker.StyleRegistry
import com.openticker.ticker.TickerStyle
import com.openticker.ticker.cards.CardCatalog
import com.openticker.ui.components.AppDrawer
import com.openticker.ui.components.BrandBackdrop
import com.openticker.ui.i18n.Loc
import com.openticker.ui.i18n.ProvideStrings
import com.openticker.ui.screens.AboutScreen
import com.openticker.ui.screens.AppsScreen
import com.openticker.ui.screens.MotionScreen
import com.openticker.ui.screens.SettingsScreen
import com.openticker.ui.screens.StyleDetailScreen
import com.openticker.ui.screens.StylesScreen

@Composable
fun OpenTickerApp() {
    val context = LocalContext.current
    var tab by rememberSaveable { mutableIntStateOf(0) }
    var detailId by rememberSaveable { mutableStateOf<String?>(null) }
    var drawerOpen by rememberSaveable { mutableStateOf(false) }
    var language by rememberSaveable { mutableStateOf(Prefs.language(context)) }

    val overlayPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { }

    fun showTicker(style: TickerStyle) {
        if (!Settings.canDrawOverlays(context)) {
            Toast.makeText(
                context,
                Loc.t("Allow \u201cDisplay over other apps\u201d so tickers can float"),
                Toast.LENGTH_LONG
            ).show()
            overlayPermissionLauncher.launch(
                Intent(
                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:${context.packageName}")
                )
            )
        } else {
            val data = when {
                style.id == "system_window" -> DemoData.systemWindowPreview(Prefs.systemWindow(context))
                style.id == "aurora" -> DemoData.systemWindowPreview(Prefs.aurora(context))
                style.id == "saturday_morning" -> DemoData.systemWindowPreview(Prefs.saturdayMorning(context))
                style.id == "your_image" -> DemoData.systemWindowPreview(Prefs.yourImage(context))
                style.id == "radio_dial" -> DemoData.systemWindowPreview(Prefs.radioDial(context))
                style.id == "aquatic" -> DemoData.systemWindowPreview(Prefs.aquatic(context))
                style.id in CardCatalog.ids -> DemoData.cardPreview(Prefs.themeOptions(context, style.id, style.accent.toArgb()))
                else -> DemoData.sampleFor(style.id)
            }
            TickerOverlayService.show(context, data, style.id)
            Toast.makeText(
                context,
                "${style.name} ${Loc.t("sent to the ticker")}",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    ProvideStrings(language) {
        Box(Modifier.fillMaxSize().background(Color.Black)) {
            BrandBackdrop()

            Box(Modifier.fillMaxSize()) {
                when {
                    detailId != null -> StyleDetailScreen(
                        style = StyleRegistry.byId(detailId!!),
                        onBack = { detailId = null },
                        onShow = { showTicker(it) }
                    )

                    tab == 0 -> StylesScreen(
                        onOpen = { detailId = it.id },
                        onTest = { showTicker(it) },
                        onUse = { Prefs.setStyleId(context, it.id) },
                        onMenu = { drawerOpen = true }
                    )

                    tab == 1 -> AppsScreen(onMenu = { drawerOpen = true })

                    tab == 2 -> MotionScreen(
                        onPreview = { showTicker(it) },
                        onMenu = { drawerOpen = true }
                    )

                    tab == 3 -> SettingsScreen(
                        onTest = { showTicker(StyleRegistry.byId(Prefs.styleId(context))) },
                        onMenu = { drawerOpen = true },
                        onLanguageChanged = { language = it }
                    )

                    else -> AboutScreen(onMenu = { drawerOpen = true })
                }
            }

            AppDrawer(
                open = drawerOpen,
                selected = tab,
                onSelect = {
                    tab = it
                    detailId = null
                    drawerOpen = false
                },
                onDismiss = { drawerOpen = false }
            )
        }
    }
}
