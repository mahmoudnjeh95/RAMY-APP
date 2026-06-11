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
        targetValue   = if (appeared) 0.dp else 30.dp,
        animationSpec = tween(durationMillis = 320, easing = FastOutSlowInEasing),
        label         = "card_offset"
    )
    val scale by animateFloatAsState(
        targetValue   = if (appeared) 1f else 0.85f,
        animationSpec = tween(durationMillis = 300, easing = FastOutSlowInEasing),
        label         = "card_scale"
    )

    CardView(
        card     = card,
        selected = selected,
        onClick  = onClick,
        modifier = modifier
            .offset(y = offsetY)
            .alpha(alpha)
            .scale(scale)
    )
}

// ─── Discard animation ────────────────────────────────────────────────────────

@Composable
fun DiscardAnimatedCard(
    card: Card,
    visible: Boolean,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = visible,
        enter   = scaleIn(initialScale = 0.7f) + fadeIn(),
        exit    = slideOutHorizontally(
            targetOffsetX = { it },
            animationSpec = tween(220)
        ) + fadeOut(animationSpec = tween(180))
    ) {
        CardView(card = card, modifier = modifier)
    }
}

// ─── Selection lift animation ─────────────────────────────────────────────────

@Composable
fun Modifier.selectionLift(selected: Boolean): Modifier {
    val lift by animateDpAsState(
        targetValue   = if (selected) (-14).dp else 0.dp,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness    = Spring.StiffnessMedium
        ),
        label = "lift"
    )
    val scale by animateFloatAsState(
        targetValue   = if (selected) 1.06f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness    = Spring.StiffnessMedium
        ),
        label = "scale"
    )
    return this
        .offset(y = lift)
        .scale(scale)
}

// ─── AI "thinking" pulse ──────────────────────────────────────────────────────

@Composable
fun aiThinkingAlpha(): Float {
    val alpha by rememberInfiniteTransition(label = "ai_think").animateFloat(
        initialValue  = 0.4f,
        targetValue   = 1f,
        animationSpec = infiniteRepeatable(
            animation  = tween(700, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "alpha"
    )
    return alpha
}

// ─── Card flip (back → face) — used when drawing from deck ────────────────────

@Composable
fun FlipInCard(
    card: Card,
    modifier: Modifier = Modifier,
    selected: Boolean = false,
    onClick: (() -> Unit)? = null
) {
    var done by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(60)
        done = true
    }
    val rotY by animateFloatAsState(
        targetValue   = if (done) 0f else 180f,
        animationSpec = tween(400, easing = FastOutSlowInEasing),
        label         = "flipY"
    )
    CardView(
        card     = card,
        faceDown = rotY > 90f,
        selected = selected && rotY <= 90f,
        onClick  = onClick,
        modifier = modifier.graphicsLayer { rotationY = rotY }
    )
}
