package com.openticker.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.Icon
import com.openticker.R
import com.openticker.data.TickerData
import com.openticker.data.TickerKind

/**
 * Re-posts a mirror of a notification that OpenTicker has taken over.
 *
 * A notification listener cannot modify another app's notification, so once
 * the original is cancelled (to avoid showing twice), this clone keeps one
 * tappable entry in the shade with the same title, text, icon and action
 * buttons — the buttons fire the original app's own PendingIntents, so reply /
 * mark-as-read / call controls keep working.
 */
internal object MirrorNotifier {

    private const val CHANNEL = "mirrored_notifications"
    private const val ID_BASE = 900_000

    fun post(context: Context, data: TickerData) {
        val key = data.notificationKey ?: return
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        ensureChannel(nm)

        // Tapping behaves exactly like the source notification: prefer the
        // app's own content intent (the exact thing a normal tap fires), and
        // fall back to the app's launcher activity for apps whose tap intent
        // is routed through services Android 12+ blocks as trampolines.
        val tap: PendingIntent? = data.contentIntent ?: data.packageName?.let { pkg ->
            context.packageManager.getLaunchIntentForPackage(pkg)?.let { launch ->
                launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                PendingIntent.getActivity(
                    context,
                    idFor(key),
                    launch,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
            }
        }

        val builder = Notification.Builder(context, CHANNEL)
            // Prefer the source notification's own status-bar glyph (tinted
            // white) so the shade entry is recognisable. Fall back to the app
            // icon silhouette when the source did not expose one.
            .setSmallIcon(Icon.createWithBitmap(monochromeIcon(data.smallIcon ?: data.icon)))
            .setContentTitle(data.title)
            .setContentText(data.body)
            .setStyle(Notification.BigTextStyle().bigText(data.body))
            .setShowWhen(true)
            .setCategory(
                when (data.kind) {
                    TickerKind.Call -> Notification.CATEGORY_CALL
                    TickerKind.Music -> Notification.CATEGORY_TRANSPORT
                    else -> Notification.CATEGORY_MESSAGE
                }
            )
            .setVisibility(Notification.VISIBILITY_PUBLIC)
            .setOnlyAlertOnce(true)
            // Only offer auto-cancel when a tap can actually do something.
            .setAutoCancel(tap != null)

        data.icon?.let { builder.setLargeIcon(it) }
        tap?.let { builder.setContentIntent(it) }

        data.actions.take(3).forEach { action ->
            action.pendingIntent?.let { intent ->
                val actionBuilder = Notification.Action.Builder(
                    fallbackIcon(context, action.label),
                    action.label,
                    intent
                )
                // A reply action only works when its text field comes along
                // with it — without it the button fires with no text and the
                // source app has nothing to send.
                action.remoteInput?.let { actionBuilder.addRemoteInput(it) }
                builder.addAction(actionBuilder.build())
            }
        }

        runCatching { nm.notify(idFor(key), builder.build()) }
    }

    /** A sensible icon for an action whose source notification had none. */
    private fun fallbackIcon(context: Context, label: String): Icon {
        val l = label.lowercase()
        val res = when {
            l.contains("mute") || l.contains("silent") ->
                android.R.drawable.ic_menu_close_clear_cancel
            l.contains("read") || l.contains("mark") ->
                android.R.drawable.ic_menu_agenda
            l.contains("delete") || l.contains("clear") ->
                android.R.drawable.ic_menu_delete
            l.contains("reply") || l.contains("answer") ->
                android.R.drawable.ic_menu_send
            else -> android.R.drawable.ic_menu_more
        }
        return Icon.createWithResource(context, res)
    }

    /** Removes the mirror (e.g. after "mark as read" or a reply). */
    fun cancel(context: Context, key: String?) {
        if (key.isNullOrEmpty()) return
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        runCatching { nm.cancel(idFor(key)) }
    }

    private fun ensureChannel(nm: NotificationManager) {
        nm.createNotificationChannel(
            NotificationChannel(
                CHANNEL,
                "Mirrored notifications",
                NotificationManager.IMPORTANCE_LOW
            ).apply { setShowBadge(false) }
        )
    }

    private fun idFor(key: String): Int = ID_BASE + (key.hashCode() and 0x3FFF_FFFF)

    /** App icon rendered as an alpha-only silhouette for the small icon slot. */
    private fun monochromeIcon(source: Bitmap?): Bitmap {
        val size = 96
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        if (source == null) {
            canvas.drawColor(0xFFFFFFFF.toInt())
            return bitmap
        }
        canvas.drawBitmap(Bitmap.createScaledBitmap(source, size, size, true), 0f, 0f, null)
        val pixels = IntArray(size * size)
        bitmap.getPixels(pixels, 0, size, 0, 0, size, size)
        for (index in pixels.indices) {
            pixels[index] = if (pixels[index] and 0xFF000000.toInt() != 0) {
                0xFFFFFFFF.toInt()
            } else {
                0x00000000
            }
        }
        bitmap.setPixels(pixels, 0, size, 0, 0, size, size)
        return bitmap
    }
}
