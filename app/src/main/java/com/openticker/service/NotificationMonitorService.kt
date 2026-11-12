package com.openticker.service

import android.app.Notification
import android.os.SystemClock
import android.provider.Settings
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import androidx.core.graphics.drawable.toBitmap
import com.openticker.data.Prefs
import com.openticker.data.ReplyAction
import com.openticker.data.TickerAction
import com.openticker.data.TickerData
import com.openticker.data.TickerKind
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Listens for posted notifications and forwards them to the ticker overlay.
 * Everything is processed on-device; nothing leaves the phone.
 */
class NotificationMonitorService : NotificationListenerService() {

    override fun onListenerConnected() {
        super.onListenerConnected()
        // The ticker engine has to be alive before the first notification
        // arrives, otherwise the notification only lands in the shade and
        // nothing floats over the other apps. The listener is bound by the
        // system for as long as notification access is granted, so this
        // runs even when the app itself is closed.
        TickerOverlayService.start(this)
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        val notification = sbn?.notification ?: return
        val pkg = sbn.packageName ?: return

        if (!Prefs.isEnabled(this)) return
        if (pkg == packageName) return
        if (!Prefs.isAppAllowed(this, pkg)) return

        // Phone apps often do not tag the ringing notification with
        // CATEGORY_CALL (Samsung's dialer does not), so the category alone is
        // not enough to recognise a call. Check the same signals the call UI
        // uses: call extras, the CallStyle template, answer/decline buttons and
        // an "incoming" title/text.
        val call = isCallNotification(notification)
        if (notification.flags and Notification.FLAG_GROUP_SUMMARY != 0 && !call) return

        // Call notifications are marked ongoing while the phone rings — those
        // are exactly the ones we must NOT skip. Every other ongoing (media
        // session, download…) is ignored as before.
        val ongoing = (notification.flags and Notification.FLAG_ONGOING_EVENT != 0) || sbn.isOngoing
        if (ongoing && !call) return

        // A call that is already answered (or one the user is dialling) must be
        // left to the dialer: no ticker, and any mirror from the ringing phase
        // is removed because its Answer/Decline buttons no longer apply.
        if (call && isOngoingCall(notification)) {
            MirrorNotifier.cancel(this, sbn.key)
            return
        }

        // The dialer re-posts the ringing notification after we take it over.
        // Cancel those re-posts silently instead of flashing the ticker again.
        if (call && isCallRepost(sbn, notification)) {
            cancelQuietly(sbn.key)
            return
        }

        val extras = notification.extras ?: return
        val title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString()?.trim().orEmpty()
        val body = (
            extras.getCharSequence(Notification.EXTRA_BIG_TEXT)
                ?: extras.getCharSequence(Notification.EXTRA_TEXT)
            )?.toString()?.trim().orEmpty()

        if (title.isBlank() && body.isBlank()) return

        val appName = runCatching {
            val info = packageManager.getApplicationInfo(pkg, 0)
            packageManager.getApplicationLabel(info).toString()
        }.getOrDefault(pkg)

        val icon = runCatching {
            packageManager.getApplicationIcon(pkg).toBitmap(width = 96, height = 96)
        }.getOrNull()

        // The original notification's status-bar glyph. Using this (instead of
        // a silhouette of the launcher icon) keeps the shade entry looking
        // like the app's real notification and gives the system a recognisable
        // monochrome shape to show in the status bar.
        val smallIcon = runCatching {
            notification.smallIcon?.loadDrawable(this)?.toBitmap(width = 96, height = 96)
        }.getOrNull()

        val time = SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date(sbn.postTime))

        // Adapt the ticker to the kind of notification.
        val kind = if (call) {
            TickerKind.Call
        } else when (notification.category) {
            Notification.CATEGORY_TRANSPORT -> TickerKind.Music
            Notification.CATEGORY_PROGRESS -> TickerKind.Flight
            Notification.CATEGORY_MESSAGE,
            Notification.CATEGORY_EMAIL,
            Notification.CATEGORY_SOCIAL -> TickerKind.Message
            else -> TickerKind.Message
        }

        var replyAction: ReplyAction? = null
        val tickerActions = mutableListOf<TickerAction>()
        for (action in notification.actions ?: emptyArray()) {
            val remoteInput = action.remoteInputs?.firstOrNull()
            val actionIntent = action.actionIntent
            if (replyAction == null && remoteInput != null && actionIntent != null) {
                replyAction = ReplyAction(remoteInput, actionIntent)
            }
            val label = action.title?.toString()?.trim().orEmpty()
            if (label.isNotBlank() && actionIntent != null) {
                tickerActions += TickerAction(label, actionIntent, remoteInput)
            }
        }

        // Remember that this call was taken over so the dialer's immediate
        // re-posts of the same ringing notification are cancelled quietly.
        if (call && canTakeOver()) rememberCallTakeover(sbn, notification)

        TickerOverlayService.show(
            context = this,
            data = TickerData(
                title = title.ifBlank { appName },
                body = body,
                appName = appName,
                time = time,
                icon = icon,
                smallIcon = smallIcon,
                packageName = pkg,
                notificationKey = sbn.key,
                contentIntent = notification.contentIntent,
                replyAction = replyAction,
                actions = tickerActions,
                kind = kind
            ),
            styleId = Prefs.styleId(this)
        )
    }

    /**
     * Recognises a phone call even when the dialer does not set
     * [Notification.CATEGORY_CALL] (Samsung's does not). The signals are the
     * same ones the platform's call UI itself relies on.
     */
    private fun isCallNotification(notification: Notification): Boolean {
        if (notification.category == Notification.CATEGORY_MISSED_CALL) return false

        val extras = notification.extras
        val callType = extras?.getInt(EXTRA_CALL_TYPE, -1) ?: -1
        if (callType in 1..3) return true

        if (notification.category == Notification.CATEGORY_CALL) return true

        if (extras != null) {
            if (extras.containsKey(EXTRA_CALL_PERSON) ||
                extras.containsKey(EXTRA_CALL_IS_VIDEO)
            ) return true
            val template = extras.getString(EXTRA_TEMPLATE).orEmpty()
            if (template.contains("CallStyle", ignoreCase = true)) return true
        }

        val labels = actionLabels(notification)
        val answer = labels.any { it == "answer" || it == "accept" }
        val decline = labels.any {
            it == "decline" || it == "reject" || it == "hang up" ||
                it == "end" || it == "end call"
        }
        // Only trust the button labels together with a full-screen intent:
        // "Accept / Decline" alone is also a calendar invite.
        if (answer && decline && notification.fullScreenIntent != null) return true

        val title = extras?.getCharSequence(Notification.EXTRA_TITLE)?.toString().orEmpty()
        val text = (
            extras?.getCharSequence(Notification.EXTRA_TEXT)
                ?: extras?.getCharSequence(Notification.EXTRA_BIG_TEXT)
            )?.toString().orEmpty()
        val incoming = title.contains("incoming", ignoreCase = true) ||
            text.contains("incoming", ignoreCase = true)
        return incoming && (answer || notification.fullScreenIntent != null)
    }

    /**
     * True once the call has been answered (or is being dialled by the user).
     * Those notifications belong to the dialer: no ticker and no mirror.
     */
    private fun isOngoingCall(notification: Notification): Boolean {
        val extras = notification.extras
        val callType = extras?.getInt(EXTRA_CALL_TYPE, -1) ?: -1
        if (callType == 2 || callType == 3) return true

        val labels = actionLabels(notification)
        val answer = labels.any { it == "answer" || it == "accept" }
        val hangUp = labels.any {
            it == "hang up" || it == "end call" || it == "end" || it == "disconnect"
        }
        if (answer && hangUp) return true

        val title = extras?.getCharSequence(Notification.EXTRA_TITLE)?.toString().orEmpty()
        val text = (
            extras?.getCharSequence(Notification.EXTRA_TEXT)
                ?: extras?.getCharSequence(Notification.EXTRA_BIG_TEXT)
            )?.toString().orEmpty()
        return text.contains("in call", ignoreCase = true) ||
            text.contains("call in progress", ignoreCase = true) ||
            text.contains("ongoing call", ignoreCase = true) ||
            title.contains("in call", ignoreCase = true)
    }

    /** Overlay + notification permissions: the two things a takeover needs. */
    private fun canTakeOver(): Boolean =
        Settings.canDrawOverlays(this) &&
            androidx.core.app.NotificationManagerCompat.from(this)
                .areNotificationsEnabled()

    /**
     * Remembers the ringing notification we just replaced. Dialers re-post it
     * straight after a listener cancels it; without this the ticker would
     * appear over and over during the same call.
     */
    private fun rememberCallTakeover(
        sbn: StatusBarNotification,
        notification: Notification
    ) {
        synchronized(callTakeovers) {
            val now = SystemClock.elapsedRealtime()
            callTakeovers.values.removeAll { now - it.at > CALL_REPOST_WINDOW_MS }
            callTakeovers[sbn.key] = CallTakeover(fingerprint(notification), now)
        }
    }

    /** True when this post is a repeat of a call we already took over. */
    private fun isCallRepost(
        sbn: StatusBarNotification,
        notification: Notification
    ): Boolean {
        if (!canTakeOver()) return false
        synchronized(callTakeovers) {
            val record = callTakeovers[sbn.key] ?: return false
            if (SystemClock.elapsedRealtime() - record.at > CALL_REPOST_WINDOW_MS) {
                callTakeovers.remove(sbn.key)
                return false
            }
            return record.fingerprint == fingerprint(notification)
        }
    }

    /** Identity of a notification's visible content, used to spot re-posts. */
    private fun fingerprint(notification: Notification): String {
        val extras = notification.extras
        val title = extras?.getCharSequence(Notification.EXTRA_TITLE)?.toString().orEmpty()
        val text = (
            extras?.getCharSequence(Notification.EXTRA_BIG_TEXT)
                ?: extras?.getCharSequence(Notification.EXTRA_TEXT)
            )?.toString().orEmpty()
        val actions = notification.actions.orEmpty()
            .joinToString("|") { it.title?.toString().orEmpty() }
        return "$title\u0000$text\u0000$actions"
    }

    private fun actionLabels(notification: Notification): List<String> =
        notification.actions.orEmpty().mapNotNull {
            it.title?.toString()?.trim()?.lowercase(Locale.ROOT)
        }

    /**
     * Cancels a notification we are deliberately replacing. The key is marked
     * first so [onNotificationRemoved] can tell our own cancel apart from the
     * user dismissing the original and does not delete the mirror we just
     * posted.
     */
    private fun cancelQuietly(key: String): Boolean {
        synchronized(selfCancelled) {
            val now = SystemClock.elapsedRealtime()
            selfCancelled.values.removeAll { now - it > SELF_CANCEL_WINDOW_MS }
            selfCancelled[key] = now
        }
        return runCatching { cancelNotification(key) }.isSuccess
    }

    /**
     * When the dialer removes its call notification (call declined, missed or
     * ended) the mirrored entry is stale, so it goes too. Our own cancels are
     * skipped: those are followed by a fresh mirror.
     */
    override fun onNotificationRemoved(sbn: StatusBarNotification?) {
        val key = sbn?.key ?: return
        if (sbn.packageName == packageName) return

        val isSelf = synchronized(selfCancelled) {
            val at = selfCancelled.remove(key)
            at != null && SystemClock.elapsedRealtime() - at <= SELF_CANCEL_WINDOW_MS
        }
        if (isSelf) return

        val wasCall = sbn.notification?.let { isCallNotification(it) } == true ||
            synchronized(callTakeovers) { callTakeovers.containsKey(key) }
        if (!wasCall) return

        synchronized(callTakeovers) { callTakeovers.remove(key) }
        MirrorNotifier.cancel(this, key)
    }

    companion object {
        private const val EXTRA_CALL_TYPE = "android.callType"
        private const val EXTRA_CALL_PERSON = "android.callPerson"
        private const val EXTRA_CALL_IS_VIDEO = "android.callIsVideo"
        private const val EXTRA_TEMPLATE = "android.template"
        private const val CALL_REPOST_WINDOW_MS = 30_000L
        private const val SELF_CANCEL_WINDOW_MS = 3_000L

        private data class CallTakeover(val fingerprint: String, val at: Long)

        private val callTakeovers = HashMap<String, CallTakeover>()

        /** Keys we cancelled ourselves, so their removal is not treated as a user dismiss. */
        private val selfCancelled = HashMap<String, Long>()

        @Volatile
        private var bound: NotificationMonitorService? = null

        fun cancelTickerNotification(key: String): Boolean {
            val service = bound ?: return false
            return service.cancelQuietly(key)
        }
    }

    override fun onCreate() {
        super.onCreate()
        bound = this
    }

    override fun onDestroy() {
        bound = null
        super.onDestroy()
    }
}
