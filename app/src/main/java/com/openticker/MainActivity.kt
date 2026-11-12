package com.openticker

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.openticker.data.Prefs
import com.openticker.service.TickerOverlayService
import com.openticker.ticker.StyleRegistry
import com.openticker.ui.OpenTickerApp
import com.openticker.ui.theme.OpenTickerTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT)
        )

        // Migrate any style saved from a build that had more styles enabled.
        if (StyleRegistry.styles.none { it.id == Prefs.styleId(this) }) {
            Prefs.setStyleId(this, StyleRegistry.styles.first().id)
        }

        // Android 13+ blocks every notification an app posts until the user
        // grants this — including the mirrored notification that replaces the
        // original while the ticker is showing.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            runCatching {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                    REQUEST_NOTIFICATIONS
                )
            }
        }

        // Keep the overlay host warm so tickers can appear instantly.
        if (Settings.canDrawOverlays(this)) {
            runCatching {
                startService(Intent(this, TickerOverlayService::class.java))
            }
        }

        setContent {
            OpenTickerTheme {
                OpenTickerApp()
            }
        }
    }

    private companion object {
        const val REQUEST_NOTIFICATIONS = 7301
    }
}
