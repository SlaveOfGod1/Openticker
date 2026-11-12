package com.openticker.ticker

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import com.openticker.data.TickerData
import com.openticker.data.TickerInteractions
import kotlinx.coroutines.launch

/**
 * One animated notification surface, shared by the live overlay AND the
 * in-app previews so every style animates the same way everywhere.
 *
 * - Styles with `selfIntro` stage their whole choreography internally.
 * - Everything else fades + scales + slides up into place, then "lights up"
 *   as a soft scrim dissolves over the settling card.
 * - The whole card is tappable and opens the source notification. The action
 *   pills consume their own taps first.
 * - Exiting mirrors the entrance: fade, shrink and slide away.
 */
@Composable
fun TickerSurface(
    style: TickerStyle,
    data: TickerData,
    interactions: TickerInteractions?,
    exitFraction: () -> Float = { 0f }
) {
    val enter = remember { Animatable(if (style.selfIntro) 1f else 0f) }
    val lit = remember { Animatable(if (style.selfIntro) 1f else 0f) }
    val interactionSource = remember { MutableInteractionSource() }

    LaunchedEffect(style.id) {
        if (style.selfIntro) return@LaunchedEffect
        enter.snapTo(0f)
        lit.snapTo(0f)
        launch {
            enter.animateTo(1f, spring(dampingRatio = 0.78f, stiffness = 320f))
        }
        lit.animateTo(1f, tween(420, delayMillis = 90, easing = FastOutSlowInEasing))
    }

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
        Box(
        Modifier
            .fillMaxWidth()
            .graphicsLayer {
                if (style.internalExit) return@graphicsLayer
                val ex = exitFraction()
                if (ex > 0f) {
                    val p = ex.coerceIn(0f, 1f)
                    alpha = 1f - p
                    translationY = 18.dp.toPx() * p
                    val s = 1f - 0.04f * p
                    scaleX = s
                    scaleY = s
                    transformOrigin = TransformOrigin(0.5f, 0.5f)
                } else {
                    val e = enter.value.coerceIn(0f, 1f)
                    alpha = e
                    translationY = (1f - e) * -22.dp.toPx()
                    val s = 0.94f + 0.06f * e
                    scaleX = s
                    scaleY = s
                    transformOrigin = TransformOrigin(0.5f, 0.5f)
                }
            }
            .then(
                if (interactions != null) {
                    Modifier.clickable(
                        interactionSource = interactionSource,
                        indication = null
                    ) { interactions.onOpen() }
                } else {
                    Modifier
                }
            )
    ) {
        style.content(data, interactions, style.selfIntro, exitFraction)
        if (!style.selfIntro) {
            Box(
                Modifier
                    .matchParentSize()
                    .graphicsLayer {
                        alpha = (1f - lit.value).coerceIn(0f, 1f) * 0.35f
                    }
                    .background(Color.Black)
            )
        }
    }
    }
}
