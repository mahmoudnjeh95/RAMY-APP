package com.rami.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rami.model.Card
import com.rami.model.Rank
import com.rami.model.Suit
import com.rami.ui.theme.RamiColors

// ─── Card size constants ───────────────────────────────────────────────────────

object CardSize {
    val Width:       Dp = 56.dp
    val Height:      Dp = 82.dp
    val SmallWidth:  Dp = 42.dp
    val SmallHeight: Dp = 60.dp
}

// ─── Main CardView ─────────────────────────────────────────────────────────────

@Composable
fun CardView(
    card: Card,
    modifier: Modifier = Modifier,
    selected: Boolean = false,
    faceDown: Boolean = false,
    small: Boolean = false,
    onClick: (() -> Unit)? = null
) {
    val width  = if (small) CardSize.SmallWidth  else CardSize.Width
    val height = if (small) CardSize.SmallHeight else CardSize.Height
    val radius = if (small) 7.dp else 10.dp
    val shape  = RoundedCornerShape(radius)

    // Press interaction for scale feedback
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    // Gold glow color for selected state
    val glowColor = RamiColors.Gold.copy(alpha = if (selected) 0.8f else 0f)

    val baseModifier = modifier
        .width(width)
        .height(height)
        // Directional cast shadow (offset downward) + selection glow
        .drawBehind {
            if (selected) drawGlow(glowColor, radius.toPx(), 14.dp.toPx())
            // Subtle elliptical cast shadow below card
            val shadowAlpha = if (isPressed) 0.08f else 0.18f
            drawOval(
                color   = Color.Black.copy(alpha = shadowAlpha),
                topLeft = Offset(-3.dp.toPx(), size.height - 3.dp.toPx()),
                size    = Size(size.width + 6.dp.toPx(), 7.dp.toPx())
            )
        }
        .shadow(
            elevation       = when { selected -> 10.dp; isPressed -> 1.dp; else -> 4.dp },
            shape           = shape,
            ambientColor    = if (selected) RamiColors.Gold.copy(alpha = 0.4f) else Color.Black.copy(alpha = 0.25f),
            spotColor       = if (selected) RamiColors.Gold.copy(alpha = 0.6f) else Color.Black.copy(alpha = 0.40f)
        )
        .clip(shape)
        .background(if (faceDown) RamiColors.FeltGreen else Color.Transparent)
        // Card face gradient
        .then(
            if (!faceDown) Modifier.background(
                brush = Brush.verticalGradient(
                    listOf(Color(0xFFFFFDF7), Color(0xFFF5F0E8))
                )
            ) else Modifier
        )
        .border(
            width = if (selected) 2.dp else 0.8.dp,
            color = when {
                selected -> RamiColors.Gold
                faceDown -> Color.White.copy(alpha = 0.15f)
                else     -> Color(0xFFCCCCCC)
            },
            shape = shape
        )
        .then(
            if (onClick != null) Modifier.clickable(
                interactionSource = interactionSource,
                indication        = null,
                onClick           = onClick
            ) else Modifier
        )

    Box(modifier = baseModifier, contentAlignment = Alignment.Center) {
        when {
            faceDown           -> CardBack()
            card is Card.Joker   -> JokerContent(small)
            card is Card.Regular -> RegularContent(card, small)
        }
    }
}

// ─── Card back ────────────────────────────────────────────────────────────────

@Composable
private fun CardBack() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.linearGradient(
                    listOf(Color(0xFF1A3A5C), Color(0xFF0D2137))
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        // Geometric diamond pattern drawn with Canvas
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(5.dp)
                .drawBehind { drawCardBackPattern() }
                .border(1.dp, Color.White.copy(alpha = 0.25f), RoundedCornerShape(5.dp))
        )
        // Centre logo
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("♦", color = Color(0xFFD4AF37), fontSize = 20.sp)
            Text("R", color = Color(0xFFD4AF37), fontSize = 11.sp, fontWeight = FontWeight.Bold)
        }
    }
}

private fun DrawScope.drawCardBackPattern() {
    val spacing = 14.dp.toPx()
    val dotR    = 1.5.dp.toPx()
    val color   = Color.White.copy(alpha = 0.12f)
    var x = 0f
    while (x < size.width) {
        var y = 0f
        while (y < size.height) {
            drawCircle(color, dotR, center = Offset(x, y))
            y += spacing
        }
        x += spacing
    }
    // Diagonal cross-lines
    val lineColor = Color.White.copy(alpha = 0.06f)
    var i = -size.height
    while (i < size.width) {
        drawLine(lineColor,
            start = Offset(i, 0f),
            end   = Offset(i + size.height, size.height),
            strokeWidth = 1.dp.toPx()
        )
        i += spacing
    }
}

// ─── Regular card content ─────────────────────────────────────────────────────

@Composable
private fun RegularContent(card: Card.Regular, small: Boolean) {
    val suitColor = when (card.suit) {
        Suit.HEARTS, Suit.DIAMONDS -> Color(0xFFCC1111)
        Suit.CLUBS,  Suit.SPADES   -> Color(0xFF111111)
    }
    val rankFontSize = if (small) 13.sp else 16.sp
    val suitFontSize = if (small) 11.sp else 14.sp
    val centerSize   = if (small) 18.sp else 24.sp
    val pad          = if (small) 3.dp  else 5.dp

    val isFaceCard = card.rank in listOf(Rank.JACK, Rank.QUEEN, Rank.KING)
    val faceEmoji  = when (card.rank) {
        Rank.JACK  -> "🃎"
        Rank.QUEEN -> "🃍"
        Rank.KING  -> "🃑"
        else       -> null
    }
    val faceAccent = when (card.rank) {
        Rank.JACK  -> Color(0xFF1565C0)
        Rank.QUEEN -> Color(0xFF880E4F)
        Rank.KING  -> Color(0xFF4A148C)
        else       -> suitColor
    }

    Box(Modifier.fillMaxSize()) {
        // Face card color band at top
        if (isFaceCard) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.28f)
                    .background(
                        brush = Brush.verticalGradient(
                            listOf(faceAccent.copy(alpha = 0.18f), Color.Transparent)
                        )
                    )
            )
        }

        Column(
            modifier = Modifier.fillMaxSize().padding(pad),
            horizontalAlignment = Alignment.Start,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // ── Top-left corner ────────────────────────────────────────────────
            Column(horizontalAlignment = Alignment.Start) {
                Text(
                    text       = card.rank.display,
                    color      = if (isFaceCard) faceAccent else suitColor,
                    fontSize   = rankFontSize,
                    fontWeight = FontWeight.Bold,
                    lineHeight = rankFontSize
                )
                Text(
                    text      = card.suit.symbol,
                    color     = suitColor,
                    fontSize  = suitFontSize,
                    lineHeight = suitFontSize
                )
            }

            // ── Centre ─────────────────────────────────────────────────────────
            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                if (faceEmoji != null && !small) {
                    Text(faceEmoji, fontSize = centerSize)
                } else {
                    Text(
                        text     = card.suit.symbol,
                        color    = suitColor,
                        fontSize = centerSize,
                        fontWeight = if (isFaceCard) FontWeight.Bold else FontWeight.Normal
                    )
                }
            }

            // ── Bottom-right corner (rotated 180°) ─────────────────────────────
            Column(
                horizontalAlignment = Alignment.End,
                modifier = Modifier
                    .fillMaxWidth()
                    .graphicsLayer { rotationZ = 180f }
            ) {
                Text(
                    text       = card.rank.display,
                    color      = if (isFaceCard) faceAccent else suitColor,
                    fontSize   = rankFontSize,
                    fontWeight = FontWeight.Bold,
                    lineHeight = rankFontSize
                )
                Text(
                    text      = card.suit.symbol,
                    color     = suitColor,
                    fontSize  = suitFontSize,
                    lineHeight = suitFontSize
                )
            }
        }
    }
}

// ─── Joker content ─────────────────────────────────────────────────────────────

@Composable
private fun JokerContent(small: Boolean) {
    // Always call composable (Rules of Compose), but only use value when full-size
    val infiniteT = rememberInfiniteTransition(label = "joker_rot")
    val rawRotation by infiniteT.animateFloat(
        initialValue  = 0f,
        targetValue   = 360f,
        animationSpec = infiniteRepeatable(tween(6000, easing = LinearEasing)),
        label         = "joker_r"
    )
    val rotation = if (!small) rawRotation else 0f

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color(0xFF6A0080).copy(alpha = 0.35f),
                        Color(0xFF1A0030).copy(alpha = 0.70f)
                    )
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        // Radiating lines canvas
        if (!small) {
            Canvas(modifier = Modifier.fillMaxSize()) { drawJokerRays(rotation) }
        }

        // Center star + label
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text       = "★",
                fontSize   = if (small) 16.sp else 24.sp,
                color      = Color(0xFFFFD700),
                fontWeight = FontWeight.Bold,
                modifier   = if (!small) Modifier.graphicsLayer { rotationZ = rotation * 0.4f } else Modifier
            )
            if (!small) {
                Text(
                    text          = "JOKER",
                    fontSize      = 8.sp,
                    fontWeight    = FontWeight.ExtraBold,
                    letterSpacing = 2.sp,
                    color         = Color(0xFFE040FB)
                )
            } else {
                Text("JKR", fontSize = 7.sp, color = Color(0xFFE040FB), fontWeight = FontWeight.Bold)
            }
        }

        // Corner labels — suit symbols instead of plain stars
        if (!small) {
            Column(
                modifier            = Modifier.align(Alignment.TopStart).padding(3.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("♦", fontSize = 8.sp, color = Color(0xFFD4AF37))
                Text("♣", fontSize = 7.sp, color = Color(0xFFCE93D8))
            }
            Column(
                modifier            = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(3.dp)
                    .graphicsLayer { rotationZ = 180f },
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("♦", fontSize = 8.sp, color = Color(0xFFD4AF37))
                Text("♣", fontSize = 7.sp, color = Color(0xFFCE93D8))
            }
        }
    }
}

// ─── Joker radiating rays helper ──────────────────────────────────────────────

private fun DrawScope.drawJokerRays(rotation: Float) {
    val cx     = size.width / 2f
    val cy     = size.height / 2f
    val innerR = 14f * density
    val outerR = 26f * density
    val rays   = 12
    repeat(rays) { i ->
        val angle = (i.toDouble() / rays) * 2.0 * kotlin.math.PI + rotation.toDouble() * kotlin.math.PI / 180.0
        val cosA  = kotlin.math.cos(angle).toFloat()
        val sinA  = kotlin.math.sin(angle).toFloat()
        drawLine(
            color       = Color(0xFFD4AF37).copy(alpha = 0.35f),
            start       = Offset(cx + cosA * innerR, cy + sinA * innerR),
            end         = Offset(cx + cosA * outerR, cy + sinA * outerR),
            strokeWidth = density
        )
    }
    drawCircle(
        color  = Color(0xFFD4AF37).copy(alpha = 0.20f),
        radius = 26f * density,
        center = Offset(cx, cy),
        style  = Stroke(width = 0.8f * density)
    )
}

// ─── Glow helper ──────────────────────────────────────────────────────────────

private fun DrawScope.drawGlow(color: Color, cornerRadius: Float, spread: Float) {
    val steps = 5
    repeat(steps) { i ->
        val fraction = (i + 1).toFloat() / (steps + 1)
        val expand = spread * fraction
        drawRoundRect(
            color        = color.copy(alpha = 0.10f * (1f - fraction * 0.6f)),
            topLeft      = Offset(-expand / 2f, -expand / 2f),
            size         = Size(size.width + expand, size.height + expand),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(cornerRadius + expand / 2f)
        )
    }
}
