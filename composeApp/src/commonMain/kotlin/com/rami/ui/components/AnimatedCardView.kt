package com.rami.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.offset
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.unit.dp
import com.rami.model.Card

// ─── Deal animation ───────────────────────────────────────────────────────────

/**
 * Wraps [CardView] with an entrance animation played once when [visible] becomes true.
 * Used for dealing cards at round start.
 *
 * @param delayMs  stagger delay in milliseconds (pass index * 60 for cascade effect)
 */
@Composable
fun DealAnimatedCard(
    card: Card,
    modifier: Modifier = Modifier,
    delayMs: Int = 0,
    selected: Boolean = false,
    onClick: (() -> Unit)? = null
) {
    var appeared by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(delayMs.toLong())
        appeared = true
    }

    val alpha by animateFloatAsState(
        targetValue   = if (appeared) 1f else 0f,
        animationSpec = tween(durationMillis = 250, easing = FastOutSlowInEasing),
        label         = "card_alpha"
    )
    val offsetY by animateDpAsState(
        targetValue   = if (appeared) 0.dp else 24.dp,
        animationSpec = tween(durationMillis = 300, easing = FastOutSlowInEasing),
        label         = "card_offset"
    )

    CardView(
        card     = card,
        selected = selected,
        onClick  = onClick,
        modifier = modifier
            .offset(y = offsetY)
            .alpha(alpha)
    )
}

// ─── Discard animation ────────────────────────────────────────────────────────

/**
 * Wraps [CardView] with an exit animation when the card is discarded.
 * Drive [visible] to false to trigger the animation; the card is removed from
 * state after the animation completes.
 */
@Composable
fun DiscardAnimatedCard(
    card: Card,
    visible: Boolean,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = visible,
        exit    = slideOutHorizontally(
            targetOffsetX = { it },
            animationSpec = tween(250)
        ) + fadeOut(animationSpec = tween(200))
    ) {
        CardView(card = card, modifier = modifier)
    }
}

// ─── Selection lift animation ─────────────────────────────────────────────────

/**
 * Returns the animated Y offset modifier for a card selection lift.
 * Drop this on any [CardView] to get smooth up/down animation on select/deselect.
 */
@Composable
fun Modifier.selectionLift(selected: Boolean): Modifier {
    val lift by animateDpAsState(
        targetValue   = if (selected) (-12).dp else 0.dp,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness    = Spring.StiffnessMedium
        ),
        label = "lift"
    )
    val scale by animateFloatAsState(
        targetValue   = if (selected) 1.05f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label         = "scale"
    )
    return this
        .offset(y = lift)
        .scale(scale)
}

// ─── AI "thinking" pulse ──────────────────────────────────────────────────────

/**
 * Pulsing alpha animation shown on the opponent's hand area while the AI is thinking.
 */
@Composable
fun aiThinkingAlpha(): Float {
    val alpha by rememberInfiniteTransition(label = "ai_think").animateFloat(
        initialValue   = 0.4f,
        targetValue    = 1f,
        animationSpec  = infiniteRepeatable(
            animation  = tween(700, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "alpha"
    )
    return alpha
}
