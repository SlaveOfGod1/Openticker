package com.openticker.ticker

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.openticker.data.Prefs
import com.openticker.data.TickerAction
import com.openticker.data.TickerData
import com.openticker.data.TickerInteractions
import com.openticker.ui.theme.Hairline
import com.openticker.ui.theme.Ink
import com.openticker.ui.theme.TextDim
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.abs

data class TickerItem(
    val id: String,
    val data: TickerData,
    val style: TickerStyle,
    val durationMs: Long
)

@Composable
fun OverlayHost(
    items: List<TickerItem>,
    onFocus: (Boolean) -> Unit,
    onDismiss: (String) -> Unit,
    onMarkRead: (TickerItem) -> Unit,
    onReply: (TickerItem, String) -> Unit,
    onMute: (TickerItem) -> Unit,
    onAction: (TickerItem, TickerAction) -> Unit,
    onOpen: (TickerItem) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .wrapContentHeight()
            .padding(horizontal = 4.dp, vertical = 2.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items.takeLast(3).forEach { item ->
            key(item.id) {
                OverlayTicker(
                    item = item,
                    onFocus = onFocus,
                    onDismiss = onDismiss,
                    onMarkRead = onMarkRead,
                    onReply = onReply,
                    onMute = onMute,
                    onAction = onAction,
                    onOpen = onOpen
                )
            }
        }
    }
}

@Composable
private fun OverlayTicker(
    item: TickerItem,
    onFocus: (Boolean) -> Unit,
    onDismiss: (String) -> Unit,
    onMarkRead: (TickerItem) -> Unit,
    onReply: (TickerItem, String) -> Unit,
    onMute: (TickerItem) -> Unit,
    onAction: (TickerItem, TickerAction) -> Unit,
    onOpen: (TickerItem) -> Unit
) {
    var leaving by remember { mutableStateOf(false) }
    var replyOpen by remember { mutableStateOf(false) }
    var replyText by remember { mutableStateOf("") }

    val exit = remember { Animatable(0f) }
    val focusRequester = remember { FocusRequester() }
    val scope = rememberCoroutineScope()
    var dragX by remember { mutableFloatStateOf(0f) }
    val dismissThreshold = with(LocalDensity.current) { 72.dp.toPx() }
    val screenWidthPx = with(LocalDensity.current) {
        LocalConfiguration.current.screenWidthDp.dp.toPx()
    }
    val context = LocalContext.current
    val replyAccent = when (item.style.id) {
        "system_window" -> Color(Prefs.systemWindow(context).accentArgb)
        "aurora" -> Color(Prefs.aurora(context).accentArgb)
        "saturday_morning" -> Color(Prefs.saturdayMorning(context).accentArgb)
        "your_image" -> Color(Prefs.yourImage(context).accentArgb)
        "radio_dial" -> Color(Prefs.radioDial(context).accentArgb)
        "aquatic" -> Color(Prefs.aquatic(context).accentArgb)
        else -> item.style.accent
    }

    val close = {
        if (!leaving) {
            leaving = true
        }
    }

    // Countdown; frozen while the reply box is open.
    LaunchedEffect(item.id) {
        var elapsed = 0L
        val tick = 100L
        while (elapsed < item.durationMs && !leaving) {
            delay(tick)
            if (!replyOpen) elapsed += tick
        }
        close()
    }

    // Exit animation, then removal.
    LaunchedEffect(leaving) {
        if (leaving) {
            onFocus(false)
            exit.animateTo(
                1f,
                tween(
                    if (item.style.internalExit) 560 else 300,
                    easing = FastOutLinearInEasing
                )
            )
            onDismiss(item.id)
        }
    }

    LaunchedEffect(replyOpen) {
        onFocus(replyOpen)
        if (replyOpen) {
            delay(80)
            runCatching { focusRequester.requestFocus() }
        }
    }

    val interactions = remember(item.id) {
        TickerInteractions(
            onReply = {
                if (!leaving) replyOpen = !replyOpen
            },
            onMarkRead = {
                onMarkRead(item)
                close()
            },
            onMute = {
                onMute(item)
                close()
            },
            onOpen = {
                onOpen(item)
                close()
            },
            onAction = { action -> onAction(item, action) },
            onDismiss = close
        )
    }

    Column(Modifier.fillMaxWidth()) {
        Box(
            Modifier
                .fillMaxWidth()
                .graphicsLayer {
                    translationX = dragX
                    alpha = 1f - (abs(dragX) / 900f).coerceIn(0f, 0.35f)
                }
                .pointerInput(item.id, leaving) {
                    if (leaving) return@pointerInput
                    detectHorizontalDragGestures(
                        onHorizontalDrag = { change, amount ->
                            change.consume()
                            dragX += amount
                        },
                        onDragEnd = {
                            if (abs(dragX) > dismissThreshold) {
                                val target = if (dragX < 0) -screenWidthPx else screenWidthPx
                                scope.launch {
                                    animate(
                                        initialValue = dragX,
                                        targetValue = target,
                                        animationSpec = tween(160)
                                    ) { value, _ -> dragX = value }
                                    // Swipe away behaves like a real notification: it leaves
                                    // the screen and is removed immediately, with no extra exit.
                                    onDismiss(item.id)
                                }
                            } else {
                                scope.launch {
                                    animate(
                                        initialValue = dragX,
                                        targetValue = 0f,
                                        animationSpec = spring(
                                            dampingRatio = 0.8f,
                                            stiffness = 500f
                                        )
                                    ) { value, _ -> dragX = value }
                                }
                            }
                        },
                        onDragCancel = {
                            scope.launch {
                                animate(
                                    initialValue = dragX,
                                    targetValue = 0f,
                                    animationSpec = spring()
                                ) { value, _ -> dragX = value }
                            }
                        }
                    )
                }
        ) {
            TickerSurface(
                style = item.style,
                data = item.data,
                interactions = interactions,
                exitFraction = { exit.value }
            )
        }

        AnimatedVisibility(
            visible = replyOpen && !leaving,
            enter = fadeIn(tween(160)) + slideInVertically(tween(220)) { -it / 2 },
            exit = fadeOut(tween(140))
        ) {
            ReplyBox(
                data = item.data,
                accent = replyAccent,
                text = replyText,
                onTextChange = { replyText = it },
                onCancel = { replyOpen = false },
                onSend = {
                    val message = replyText.trim()
                    if (message.isNotEmpty()) {
                        onReply(item, message)
                    }
                    replyOpen = false
                    replyText = ""
                },
                focusRequester = focusRequester
            )
        }
    }
}

@Composable
private fun ReplyBox(
    data: TickerData,
    accent: Color,
    text: String,
    onTextChange: (String) -> Unit,
    onCancel: () -> Unit,
    onSend: () -> Unit,
    focusRequester: FocusRequester
) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(Color(0xEE101016))
            .border(1.dp, Hairline, RoundedCornerShape(14.dp))
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier
                .size(26.dp)
                .clip(CircleShape)
                .background(accent.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            val icon = data.icon
            if (icon != null) {
                Image(
                    bitmap = icon.asImageBitmap(),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize().clip(CircleShape)
                )
            } else {
                Text(
                    data.title.take(1).uppercase(),
                    color = accent,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Black
                )
            }
        }
        Spacer(Modifier.size(9.dp))
        Box(Modifier.weight(1f)) {
            if (text.isEmpty()) {
                Text(
                    "Reply to ${data.title}…",
                    color = TextDim.copy(alpha = 0.75f),
                    fontSize = 12.5.sp
                )
            }
            BasicTextField(
                value = text,
                onValueChange = onTextChange,
                textStyle = TextStyle(color = Color(0xFFF5F5F7), fontSize = 13.sp),
                cursorBrush = SolidColor(accent),
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(focusRequester)
            )
        }
        Spacer(Modifier.size(5.dp))
        Box(
            Modifier
                .size(30.dp)
                .clip(CircleShape)
                .background(accent)
                .clickable(onClick = onSend),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.AutoMirrored.Filled.Send,
                contentDescription = "Send",
                tint = Ink,
                modifier = Modifier.size(14.dp)
            )
        }
        Spacer(Modifier.size(3.dp))
        Icon(
            Icons.Filled.Close,
            contentDescription = "Cancel",
            tint = TextDim,
            modifier = Modifier
                .size(20.dp)
                .clickable(onClick = onCancel)
        )
    }
}
