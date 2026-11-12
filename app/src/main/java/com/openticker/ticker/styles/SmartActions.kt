package com.openticker.ticker.styles

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Reply
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
import androidx.compose.ui.graphics.vector.ImageVector
import com.openticker.data.TickerAction
import com.openticker.data.TickerData
import com.openticker.data.TickerInteractions

/*
 * Buttons that follow the source notification's own actions.
 *
 * Shared by every curated theme: the dock buttons are built from
 * `data.actions`, so a WhatsApp message shows Reply / Mark as read / Mute,
 * a mail app shows its own set, and a notification with no actions at all
 * gets a blank dock — exactly like the stock Android notification.
 */

/** One dock button spec: label, icon, which shape to draw, and the tap handler. */
internal data class NotifActionSpec(
    val label: String,
    val icon: ImageVector,
    /** Themes with two button shapes render this one as the "read"-style button. */
    val shell: Boolean,
    /** Media transport actions render as icon-only buttons (no words). */
    val iconOnly: Boolean,
    val onClick: (() -> Unit)?
)

private fun hasWord(label: String, vararg words: String): Boolean =
    words.any { label.contains(it, ignoreCase = true) }

private fun isArabicText(text: String): Boolean =
    text.any { it.code in 0x600..0x6FF }

/**
 * The dock can hold three buttons on a narrow ticker, so very long
 * notification labels collapse to their short keyword form (localised for
 * Arabic) — e.g. WhatsApp's "Mark as read" becomes "Read".
 */
private fun shortLabel(raw: String): String {
    val arabic = isArabicText(raw)
    val reply = hasWord(raw, "reply", "رد", "الرد")
    val read = !reply && hasWord(raw, "mark", "read", "مقروء", "قراءة")
    val mute = hasWord(raw, "mute", "silent", "كتم", "صامت")
    val archive = hasWord(raw, "archive", "أرشفة")
    val delete = hasWord(raw, "delete", "trash", "حذف")
    val snooze = hasWord(raw, "snooze", "remind", "later", "غفوة", "تذكير", "لاحقا")
    val share = hasWord(raw, "share", "إرسال")
    val open = hasWord(raw, "open", "view", "عرض", "مشاهدة")
    return if (arabic) {
        when {
            reply -> "رد"
            read -> "مقروء"
            mute -> "كتم"
            archive -> "أرشيف"
            delete -> "حذف"
            snooze -> "لاحقا"
            share -> "مشاركة"
            open -> "عرض"
            else -> raw
        }
    } else {
        when {
            reply -> "Reply"
            read -> "Read"
            mute -> "Mute"
            archive -> "Archive"
            delete -> "Delete"
            snooze -> "Later"
            share -> "Share"
            open -> "Open"
            else -> raw
        }
    }
}

/** Buttons that mirror the original notification's actions, up to three. */
internal fun notificationActionSpecs(
    data: TickerData,
    interactions: TickerInteractions?
): List<NotifActionSpec> =
    data.actions.take(3).map { action ->
        val raw = action.label.trim().ifBlank { "Action" }
        val label = if (raw.length > 8) shortLabel(raw).ifBlank { raw } else raw
        val l = raw.lowercase()

        val replyLike = hasWord(l, "reply", "رد", "الرد")
        val markRead = !replyLike && hasWord(l, "mark", "read", "مقروء", "قراءة")
        val muteLike = hasWord(l, "mute", "silent", "كتم", "صامت")
        val archiveLike = hasWord(l, "archive", "أرشفة")
        val deleteLike = hasWord(l, "delete", "trash", "حذف")
        val snoozeLike = hasWord(l, "snooze", "remind", "later", "غفوة", "تذكير", "لاحقا")
        val shareLike = hasWord(l, "share", "إرسال")
        val openLike = hasWord(l, "open", "view", "عرض", "مشاهدة")
        val prevLike = hasWord(l, "previous", "prev", "rewind", "back", "السابق")
        val pauseLike = hasWord(l, "pause", "إيقاف")
        val playLike = !pauseLike && hasWord(l, "play", "resume", "تشغيل")
        val nextLike = hasWord(l, "next", "skip", "forward", "التالي")

        val icon = when {
            replyLike -> Icons.AutoMirrored.Filled.Reply
            markRead -> Icons.Filled.DoneAll
            muteLike -> Icons.Filled.NotificationsOff
            archiveLike -> Icons.Filled.Archive
            deleteLike -> Icons.Filled.Delete
            snoozeLike -> Icons.Filled.Star
            shareLike -> Icons.Filled.Share
            openLike -> Icons.Filled.OpenInNew
            prevLike -> Icons.Filled.SkipPrevious
            pauseLike -> Icons.Filled.Pause
            playLike -> Icons.Filled.PlayArrow
            nextLike -> Icons.Filled.SkipNext
            else -> Icons.Filled.Notifications
        }

        val onClick: (() -> Unit)? = when {
            interactions == null -> null
            replyLike -> interactions.onReply
            markRead -> interactions.onMarkRead
            // Mute works like the system mute: the notification is silenced
            // and cleared, instead of doing nothing.
            muteLike -> interactions.onMute
            action.pendingIntent != null -> {
                { interactions.onAction(action) }
            }
            else -> null
        }

        NotifActionSpec(label, icon, markRead, prevLike || pauseLike || playLike || nextLike, onClick)
    }
