package com.openticker.data

import android.app.PendingIntent
import android.app.RemoteInput
import android.graphics.Bitmap

/** A direct-reply action found on the source notification. */
data class ReplyAction(
    val remoteInput: RemoteInput,
    val pendingIntent: PendingIntent
)

/** What kind of source notification this is, so the ticker can adapt. */
enum class TickerKind { Message, Call, Flight, Music, Generic }

/** A plain notification action (answer, decline, media control, …). */
data class TickerAction(
    val label: String,
    val pendingIntent: PendingIntent? = null,
    /** The text field a reply action accepts, when it has one. */
    val remoteInput: RemoteInput? = null
)

data class TickerData(
    val title: String,
    val body: String,
    val appName: String,
    val time: String,
    val icon: Bitmap? = null,
    /**
     * The source notification's own small (status-bar) icon, rendered to a
     * bitmap. Mirrors use this for the shade's status-bar glyph so it looks
     * like the original app's notification instead of a solid app-icon blob.
     */
    val smallIcon: Bitmap? = null,
    val packageName: String? = null,
    /** Source notification key — enables "mark as read". */
    val notificationKey: String? = null,
    /** Tap-to-open intent from the source notification. */
    val contentIntent: PendingIntent? = null,
    /** Direct-reply remote input action, if the app supports it. */
    val replyAction: ReplyAction? = null,
    /** Answer/decline, media transport controls, etc. */
    val actions: List<TickerAction> = emptyList(),
    val kind: TickerKind = TickerKind.Message
)

/**
 * Callbacks a ticker style receives while running in the live overlay.
 * Preview contexts pass null so decorative chips stay non-interactive.
 */
class TickerInteractions(
    val onReply: () -> Unit = {},
    val onMarkRead: () -> Unit = {},
    val onOpen: () -> Unit = {},
    /** Silences the notification, the way Android's own mute does. */
    val onMute: () -> Unit = {},
    val onDismiss: () -> Unit = {},
    /** Sends a notification action (answer, decline, media control…). */
    val onAction: (TickerAction) -> Unit = {}
)

object DemoData {

    /** The demo notification: a servant reporting to his master. */
    const val DEMO_SENDER = "Mark"
    const val DEMO_MESSAGE =
        "Sir, your AI agent has successfully ordered the pizza. It ordered the large one with extra " +
            "cheese, paid with the card you left it, and the kitchen says twenty minutes. " +
            "Shall I set the table?"

    val ava = TickerData(
        title = DEMO_SENDER,
        body = DEMO_MESSAGE,
        appName = "Messages",
        time = "9:33 pm"
    )

    val group = TickerData(
        title = "Design crew · 2 new",
        body = "Maya: pushed the new ticker mocks, take a look when you can",
        appName = "Messages",
        time = "9:41 pm"
    )

    val delivery = TickerData(
        title = "Your order is on the way",
        body = "Rider picked up your food and is heading to your address",
        appName = "Delivery",
        time = "9:48 pm"
    )

    val mail = TickerData(
        title = "Light Source Labs",
        body = "Your OpenTicker style pack is ready to download",
        appName = "Mail",
        time = "10:02 pm"
    )

    val all = listOf(ava, group, delivery, mail)

    fun sampleFor(styleId: String): TickerData {
        val index = (styleId.hashCode().let { if (it < 0) -it else it }) % all.size
        return all[index]
    }

    /**
     * Preview for the twenty illustrated card themes — same preview kinds
     * (message / call / flight / music / generic / no actions) as the System
     * Window, so the dock buttons match what a real notification of that kind
     * would carry.
     */
    fun cardPreview(options: SystemWindowOptions): TickerData =
        systemWindowPreview(options)

    /** Live data for the System Window customizer preview. */
    fun systemWindowPreview(options: SystemWindowOptions): TickerData {
        val kind = when (options.previewKind) {
            "call" -> TickerKind.Call
            "flight" -> TickerKind.Flight
            "music" -> TickerKind.Music
            "notification" -> TickerKind.Generic
            else -> TickerKind.Message
        }
        val sender = options.sender.ifBlank { DEMO_SENDER }
        val message = options.message.ifBlank { DEMO_MESSAGE }
        return when (kind) {
            TickerKind.Message -> TickerData(
                title = sender,
                body = message,
                appName = "Messages",
                time = "5:39 am",
                actions = if (options.previewKind == "none") emptyList() else listOf(
                    TickerAction("Reply"),
                    TickerAction("Mark as read"),
                    TickerAction("Mute")
                ),
                kind = kind
            )
            TickerKind.Call -> TickerData(
                title = sender,
                body = "Incoming call",
                appName = "Phone",
                time = "5:39 am",
                actions = listOf(TickerAction("Answer"), TickerAction("Reject")),
                kind = kind
            )
            TickerKind.Flight -> TickerData(
                title = "FL 204 · CMB → DXB",
                body = "Boarding at gate B12 · On time",
                appName = "Flights",
                time = "5:39 am",
                kind = kind
            )
            TickerKind.Music -> TickerData(
                title = "Midnight City",
                body = "M83 · Hurry Up, We're Dreaming",
                appName = "Music",
                time = "5:39 am",
                actions = listOf(
                    TickerAction("Previous"),
                    TickerAction("Pause"),
                    TickerAction("Next")
                ),
                kind = kind
            )
            TickerKind.Generic -> TickerData(
                title = sender,
                body = message,
                appName = "System",
                time = "5:39 am",
                kind = kind
            )
        }
    }
}
