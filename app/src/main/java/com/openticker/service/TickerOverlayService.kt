package com.openticker.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.RemoteInput
import android.app.Service
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.graphics.PixelFormat
import android.os.Binder
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.provider.Settings
import android.util.Log
import android.view.Gravity
import android.view.WindowManager
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.ui.platform.ComposeView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.openticker.R
import com.openticker.data.Prefs
import com.openticker.data.TickerAction
import com.openticker.data.TickerData
import com.openticker.ticker.OverlayHost
import com.openticker.ticker.StyleRegistry
import com.openticker.ticker.TickerItem
import com.openticker.ui.theme.OpenTickerTheme
import androidx.compose.runtime.snapshots.SnapshotStateList
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import java.util.UUID

/**
 * Owns the system overlay window that draws tickers on top of other apps.
 * Also executes ticker actions: direct reply, mark as read, tap to open.
 */
class TickerOverlayService : Service() {

    private lateinit var windowManager: WindowManager
    private var overlayView: ComposeView? = null
    private var overlayParams: WindowManager.LayoutParams? = null
    private var foreground = false
    private val owner = OverlayLifecycleOwner()
    private val items: SnapshotStateList<TickerItem> = mutableStateListOf()
    private val mainHandler = Handler(Looper.getMainLooper())
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    override fun onBind(intent: Intent?): IBinder = engineBinder

    private val engineBinder = Binder()

    override fun onCreate() {
        super.onCreate()
        instance = this
        goForeground()
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        showOverlay()
        drainPending()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        goForeground()
        showOverlay()
        drainPending()
        return START_STICKY
    }

    /** Runs as a silent foreground service so the notification listener can
     *  wake it from the background with no restriction. */
    private fun goForeground() {
        if (foreground) return
        runCatching {
            val nm = getSystemService(Context.NOTIFICATION_SERVICE)
                as NotificationManager
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                nm.createNotificationChannel(
                    NotificationChannel(
                        FOREGROUND_CHANNEL,
                        "Ticker engine",
                        NotificationManager.IMPORTANCE_MIN
                    ).apply { setShowBadge(false) }
                )
            }
            val builder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                Notification.Builder(this, FOREGROUND_CHANNEL)
            } else {
                @Suppress("DEPRECATION")
                Notification.Builder(this).setPriority(Notification.PRIORITY_MIN)
            }
            val notification = builder
                .setContentTitle("OpenTicker")
                .setContentText("Showing tickers over other apps")
                .setSmallIcon(R.mipmap.ic_launcher)
                .setOngoing(true)
                .build()
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                startForeground(
                    FOREGROUND_ID,
                    notification,
                    android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_SYSTEM_EXEMPTED
                )
            } else {
                @Suppress("DEPRECATION")
                startForeground(FOREGROUND_ID, notification)
            }
            foreground = true
        }
    }

    override fun onDestroy() {
        instance = null
        scope.cancel()
        overlayView?.let { view ->
            runCatching { windowManager.removeView(view) }
        }
        overlayView = null
        overlayParams = null
        owner.destroy()
        super.onDestroy()
    }

    private fun showOverlay() {
        if (overlayView != null) return
        if (!Settings.canDrawOverlays(this)) return

        val view = ComposeView(this)
        view.setViewTreeLifecycleOwner(owner)
        view.setViewTreeSavedStateRegistryOwner(owner)
        owner.create()
        view.setContent {
            OpenTickerTheme {
                OverlayHost(
                    items = items,
                    onFocus = { focusable -> setOverlayFocusable(focusable) },
                    onDismiss = { id -> items.removeAll { it.id == id } },
                    onMarkRead = { item -> markRead(item) },
                    onReply = { item, text -> sendReply(item, text) },
                    onMute = { item -> mute(item) },
                    onAction = { item, action -> sendAction(item, action) },
                    onOpen = { item -> openApp(item) }
                )
            }
        }

        val screenWidth = resources.displayMetrics.widthPixels
        val width = (screenWidth * Prefs.widthPercent(this) / 100f).toInt()
        val params = WindowManager.LayoutParams(
            width,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
            y = overlayY()
            softInputMode = WindowManager.LayoutParams.SOFT_INPUT_STATE_UNSPECIFIED or
                WindowManager.LayoutParams.SOFT_INPUT_ADJUST_NOTHING
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                layoutInDisplayCutoutMode =
                    WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
            }
        }

        runCatching {
            windowManager.addView(view, params)
            overlayView = view
            overlayParams = params
        }
    }

    private fun dp(value: Int): Int =
        (value * resources.displayMetrics.density).toInt()

    /** Default top inset plus the user's vertical position preference. */
    private fun overlayY(): Int =
        dp(16 + Prefs.overlayOffsetY(this))

    /** Applies a changed position or width preference to the live overlay. */
    private fun applyPosition() {
        val view = overlayView ?: return
        val params = overlayParams ?: return
        params.y = overlayY()
        val screenWidth = resources.displayMetrics.widthPixels
        params.width = (screenWidth * Prefs.widthPercent(this) / 100f).toInt()
        runCatching { windowManager.updateViewLayout(view, params) }
    }

    /** Makes the overlay window focusable so the reply field can take keyboard input. */
    private fun setOverlayFocusable(focusable: Boolean) {
        val view = overlayView ?: return
        val params = overlayParams ?: return
        runCatching {
            if (focusable) {
                params.flags = params.flags and
                    (WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE.inv())
                params.softInputMode = WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE or
                    WindowManager.LayoutParams.SOFT_INPUT_ADJUST_NOTHING
            } else {
                params.flags = params.flags or
                    WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                params.softInputMode = WindowManager.LayoutParams.SOFT_INPUT_STATE_UNSPECIFIED or
                    WindowManager.LayoutParams.SOFT_INPUT_ADJUST_NOTHING
            }
            windowManager.updateViewLayout(view, params)
        }
    }

    /** Clears the real notification behind the ticker ("mark as read"). */
    private fun markRead(item: TickerItem) {
        item.data.notificationKey?.let { key ->
            NotificationMonitorService.cancelTickerNotification(key)
            MirrorNotifier.cancel(this, key)
        }
    }

    /**
     * Silences a notification the way Android's own mute does: the source
     * app's own mute intent is sent when it has one, and the notification is
     * silenced and cleared so it stops sounding and leaves the shade.
     */
    private fun mute(item: TickerItem) {
        val action = item.data.actions.firstOrNull {
            it.label.contains("mute", ignoreCase = true) ||
                it.label.contains("silent", ignoreCase = true)
        }
        action?.pendingIntent?.let { intent ->
            scope.launch(Dispatchers.IO) {
                runCatching { intent.send(applicationContext, 0, null) }
            }
        }
        item.data.notificationKey?.let { key ->
            NotificationMonitorService.cancelTickerNotification(key)
            MirrorNotifier.cancel(this, key)
        }
    }

    /** Sends a direct reply through the notification's remote input action. */
    private fun sendReply(item: TickerItem, text: String) {
        val action = item.data.replyAction
        val key = item.data.notificationKey
        scope.launch(Dispatchers.IO) {
            if (action != null) {
                runCatching {
                    val fill = Intent()
                    val results = Bundle()
                    results.putCharSequence(action.remoteInput.resultKey, text)
                    RemoteInput.addResultsToIntent(
                        arrayOf(action.remoteInput),
                        fill,
                        results
                    )
                    action.pendingIntent.send(applicationContext, 0, fill)
                }
            }
            if (key != null) {
                NotificationMonitorService.cancelTickerNotification(key)
                MirrorNotifier.cancel(applicationContext, key)
            }
        }
    }

    /** Fires the notification's content intent (tap to open the conversation). */
    private fun openApp(item: TickerItem) {
        val intent = item.data.contentIntent
        val pkg = item.data.packageName
        scope.launch(Dispatchers.IO) {
            // Direct activity launch first: many apps implement their
            // notification tap as a service/broadcast, which Android 12+
            // blocks as a "trampoline", so the tap would silently do nothing.
            val launched = pkg != null && runCatching {
                packageManager.getLaunchIntentForPackage(pkg)?.let { launch ->
                    launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    startActivity(launch)
                }
            }.isSuccess
            if (!launched && intent != null) {
                runCatching { intent.send(applicationContext, 0, null) }
            }
        }
    }

    /** Fires a notification action: answer/decline call, skip track, pause… */
    private fun sendAction(item: TickerItem, action: TickerAction) {
        val intent = action.pendingIntent ?: return
        scope.launch(Dispatchers.IO) {
            runCatching { intent.send(applicationContext, 0, null) }
        }
    }

    private fun enqueue(data: TickerData, styleId: String) {
        if (!Settings.canDrawOverlays(this)) return
        // One ticker at a time — a newer notification replaces the current one.
        if (items.isNotEmpty()) items.clear()
        items.add(
            TickerItem(
                id = UUID.randomUUID().toString(),
                data = data,
                style = StyleRegistry.byId(styleId),
                durationMs = Prefs.tickerDurationMs(this)
            )
        )
        // The ticker is now on screen: replace the original with a single
        // mirror entry in the shade. Skip the takeover entirely if this app
        // cannot post notifications (permission denied) or the original could
        // not be cancelled (listener disconnected) — otherwise the user would
        // be left with nothing.
        data.notificationKey?.let { key ->
            val canMirror = androidx.core.app.NotificationManagerCompat
                .from(this).areNotificationsEnabled()
            if (canMirror) {
                val replaced = NotificationMonitorService.cancelTickerNotification(key)
                if (replaced) MirrorNotifier.post(this, data)
            }
        }
    }

    private fun drainPending() {
        val queued: List<Pair<TickerData, String>>
        synchronized(pending) {
            queued = pending.toList()
            pending.clear()
        }
        queued.forEach { enqueue(it.first, it.second) }
    }

    companion object {
        private const val TAG = "OpenTicker"

        @Volatile
        private var instance: TickerOverlayService? = null
        private val pending = mutableListOf<Pair<TickerData, String>>()

        fun show(context: Context, data: TickerData, styleId: String = Prefs.styleId(context)) {
            val app = context.applicationContext
            if (!Settings.canDrawOverlays(app)) return

            synchronized(pending) {
                if (pending.size >= 4) pending.removeAt(0)
                pending.add(data to styleId)
            }
            start(app)
            // Belt and braces: some OEMs (notably Samsung) still block
            // background service starts. Binding has no such restriction,
            // and the system keeps the notification listener — and
            // therefore this bind — alive while notifications arrive.
            ensureBound(app)
            instance?.let { service ->
                service.mainHandler.post { service.drainPending() }
            }
        }

        /**
         * Starts the ticker engine if it is not already running. Safe to call
         * from the background: the engine promotes itself to a foreground
         * service as soon as it comes up.
         */
        fun start(context: Context) {
            val app = context.applicationContext
            runCatching {
                val intent = Intent(app, TickerOverlayService::class.java)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    app.startForegroundService(intent)
                } else {
                    app.startService(intent)
                }
            }.onFailure {
                Log.w(TAG, "OpenTicker: could not start the ticker engine", it)
            }
        }

        private val bindLock = Any()

        @Volatile
        private var engineBound = false

        private val engineConnection = object : ServiceConnection {
            override fun onServiceConnected(name: ComponentName?, binder: IBinder?) {
                instance?.let { service ->
                    service.mainHandler.post { service.drainPending() }
                }
            }

            override fun onServiceDisconnected(name: ComponentName?) {}
        }

        private fun ensureBound(app: Context) {
            synchronized(bindLock) {
                if (engineBound) return
                engineBound = runCatching {
                    app.bindService(
                        Intent(app, TickerOverlayService::class.java),
                        engineConnection,
                        Context.BIND_AUTO_CREATE
                    )
                }.getOrDefault(false)
            }
        }

        private const val FOREGROUND_ID = 4711
        private const val FOREGROUND_CHANNEL = "ticker_engine"

        fun isRunning(): Boolean = instance != null

        /** Called from Settings when the vertical position or width changes. */
        fun refreshPosition() {
            instance?.mainHandler?.post { instance?.applyPosition() }
        }
    }
}

/**
 * Minimal lifecycle owner so Compose can live inside a Service window.
 */
private class OverlayLifecycleOwner : LifecycleOwner, SavedStateRegistryOwner {

    private val lifecycleRegistry = LifecycleRegistry(this)
    private val savedStateController = SavedStateRegistryController.create(this)

    override val lifecycle: Lifecycle
        get() = lifecycleRegistry

    override val savedStateRegistry: SavedStateRegistry
        get() = savedStateController.savedStateRegistry

    fun create() {
        savedStateController.performAttach()
        savedStateController.performRestore(null)
        lifecycleRegistry.currentState = Lifecycle.State.RESUMED
    }

    fun destroy() {
        lifecycleRegistry.currentState = Lifecycle.State.DESTROYED
    }
}
