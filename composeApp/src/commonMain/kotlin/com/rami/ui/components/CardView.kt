package com.rami.ui.components

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
    val radius = if (small) 6.dp else 9.dp
    val shape  = RoundedCornerShape(radius)

    // Press interaction for scale feedback
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val animatedScale by animateFloatAsState(if (isPressed) 0.96f else 1f, label = "scale")

    // Gold glow color for selected state
    val glowColor = RamiColors.Gold.copy(alpha = if (selected) 0.85f else 0f)

    val baseModifier = modifier
        .width(width)
        .height(height)
        .graphicsLayer {
            scaleX = animatedScale
            scaleY = animatedScale
        }
        // Directional cast shadow + selection glow
        .drawBehind {
            if (selected) drawGlow(glowColor, radius.toPx(), 16.dp.toPx())
            
            // Subtle elliptical shadow for "floating" effect
            val shadowAlpha = if (isPressed) 0.12f else 0.22f
            drawOval(
                color   = Color.Black.copy(alpha = shadowAlpha),
                topLeft = Offset(-2.dp.toPx(), size.height - 2.dp.toPx()),
                size    = Size(size.width + 4.dp.toPx(), 6.dp.toPx())
            )
        }
        .shadow(
            elevation       = when { selected -> 12.dp; isPressed -> 2.dp; else -> 5.dp },
            shape           = shape,
            ambientColor    = if (selected) RamiColors.Gold.copy(alpha = 0.5f) else Color.Black.copy(alpha = 0.3f),
            spotColor       = if (selected) RamiColors.Gold.copy(alpha = 0.7f) else Color.Black.copy(alpha = 0.45f)
        )
        .clip(shape)
        .background(if (faceDown) RamiColors.DarkGreen else Color.Transparent)
        // Card face with premium "ivory/linen" look
        .then(
            if (!faceDown) Modifier
                .background(
                    brush = Brush.verticalGradient(
                        0.0f to Color(0xFFFFFFFF),
                        0.7f to Color(0xFFF9F6F0),
                        1.0f to Color(0xFFF0EAE0)
                    )
                )
                .drawBehind {
                    drawCardTexture() // Linen/Paper texture
                }
            else Modifier
        )
        .border(
            width = if (selected) 2.2.dp else 0.8.dp,
            color = when {
                selected -> RamiColors.Gold
                faceDown -> Color.White.copy(alpha = 0.2f)
                else     -> Color(0xFFDED9D0)
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

private fun DrawScope.drawCardTexture() {
    // Subtle linen-like grain
    val color = Color.Black.copy(alpha = 0.03f)
    val step = 2.dp.toPx()
    // Vertical lines
    var x = 0f
    while (x < size.width) {
        drawLine(color, Offset(x, 0f), Offset(x, size.height), 0.5f)
        x += step
    }
    // Horizontal lines
    var y = 0f
    while (y < size.height) {
        drawLine(color, Offset(0f, y), Offset(size.width, y), 0.5f)
        y += step
    }
}

// ─── Card back ────────────────────────────────────────────────────────────────

@Composable
private fun CardBack() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.radialGradient(
                    colors = listOf(Color(0xFF2E5A88), Color(0xFF132A42)),
                    center = Offset.Unspecified,
                    radius = Float.POSITIVE_INFINITY
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize().padding(6.dp)) {
            // Intricate border
            drawRoundRect(
                color = Color.White.copy(0.15f),
                style = Stroke(width = 1.5.dp.toPx()),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(4.dp.toPx())
            )
            
            // Intricate fill pattern
            drawCardBackPattern()
        }
        
        // Centre Emblem
        Surface(
            color = Color.Transparent,
            shape = RoundedCornerShape(4.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFD4AF37).copy(0.4f)),
            modifier = Modifier.padding(12.dp)
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("♦", color = Color(0xFFD4AF37), fontSize = 24.sp)
                Text("RAMI", color = Color(0xFFD4AF37), fontSize = 9.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 2.sp)
            }
        }
    }
}

private fun DrawScope.drawCardBackPattern() {
    val step = 10.dp.toPx()
    val color = Color.White.copy(alpha = 0.08f)
    // Diamond lattice
    for (i in -10..20) {
        val offset = i * step
        drawLine(color, Offset(offset, 0f), Offset(offset + size.height, size.height), 1f)
        drawLine(color, Offset(offset, size.height), Offset(offset + size.height, 0f), 1f)
    }
    
    // Tiny dots at intersections
    val dotColor = Color.White.copy(0.12f)
    for (ix in 0..10) {
        for (iy in 0..15) {
            val px = ix * step
            val py = iy * step
            drawCircle(dotColor, 1.dp.toPx(), center = Offset(px, py))
        }
    }
}

// ─── Regular card content ─────────────────────────────────────────────────────

@Composable
private fun RegularContent(card: Card.Regular, small: Boolean) {
    val suitColor = when (card.suit) {
        Suit.HEARTS, Suit.DIAMONDS -> Color(0xFFCC1111)
        Suit.CLUBS,  Suit.SPADES   -> Color(0xFF1A1A1A)
    }
    val rankFontSize = if (small) 14.sp else 18.sp
    val suitFontSize = if (small) 12.sp else 15.sp
    val centerSize   = if (small) 22.sp else 36.sp
    val pad          = if (small) 4.dp  else 6.dp

    val isFaceCard = card.rank in listOf(Rank.JACK, Rank.QUEEN, Rank.KING)
    val faceAccent = when (card.rank) {
        Rank.JACK  -> Color(0xFF1E4D8C)
        Rank.QUEEN -> Color(0xFF9C235A)
        Rank.KING  -> Color(0xFF5A2A8C)
        else       -> suitColor
    }

    Box(Modifier.fillMaxSize()) {
        // Sophisticated background for face cards
        if (isFaceCard && !small) {
            Box(
                Modifier
                    .fillMaxSize()
                    .padding(8.dp)
                    .border(0.5.dp, faceAccent.copy(0.2f), RoundedCornerShape(4.dp))
                    .background(faceAccent.copy(alpha = 0.04f))
            )
        }

        Column(
            modifier = Modifier.fillMaxSize().padding(pad),
            horizontalAlignment = Alignment.Start,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // ── Top-left corner ────────────────────────────────────────────────
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text       = card.rank.display,
                    color      = if (isFaceCard) faceAccent else suitColor,
                    fontSize   = rankFontSize,
                    fontWeight = FontWeight.Black,
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
            Box(Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                if (isFaceCard && !small) {
                    // Larger, more artistic suit symbol with accent
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text     = card.suit.symbol,
                            color    = faceAccent.copy(alpha = 0.1f),
                            fontSize = 60.sp
                        )
                        Text(
                            text     = card.rank.display,
                            color    = faceAccent,
                            fontSize = 32.sp,
                            fontWeight = FontWeight.Black
                        )
                    }
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
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .fillMaxWidth()
                    .graphicsLayer { rotationZ = 180f }
            ) {
                Text(
                    text       = card.rank.display,
                    color      = if (isFaceCard) faceAccent else suitColor,
                    fontSize   = rankFontSize,
                    fontWeight = FontWeight.Black,
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


// ─── Joker content — traditional jester design ────────────────────────────────

@Composable
private fun JokerContent(small: Boolean) {
    val red = Color(0xFFCC1111)
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(Color(0xFFFFFDF7), Color(0xFFF0EBE0)))),
        contentAlignment = Alignment.Center
    ) {
        // Top-left corner
        Column(
            modifier = Modifier.align(Alignment.TopStart).padding(3.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("J",  color = red, fontSize = if (small) 12.sp else 14.sp, fontWeight = FontWeight.ExtraBold, lineHeight = if (small) 12.sp else 14.sp)
            Text("★", color = red, fontSize = if (small) 9.sp  else 10.sp, lineHeight = if (small) 9.sp else 10.sp)
        }

        // Centre jester
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(text = "🃏", fontSize = if (small) 20.sp else 34.sp)
            if (!small) {
                Spacer(Modifier.height(2.dp))
                Text(
                    text          = "JOKER",
                    color         = red,
                    fontSize      = 7.sp,
                    fontWeight    = FontWeight.ExtraBold,
                    letterSpacing = 2.sp
                )
            } else {
                Text("J", color = red, fontSize = 7.sp, fontWeight = FontWeight.Bold)
            }
        }

        // Bottom-right corner (rotated)
        Column(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(3.dp)
                .graphicsLayer { rotationZ = 180f },
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("J",  color = red, fontSize = if (small) 12.sp else 14.sp, fontWeight = FontWeight.ExtraBold, lineHeight = if (small) 12.sp else 14.sp)
            Text("★", color = red, fontSize = if (small) 9.sp  else 10.sp, lineHeight = if (small) 9.sp else 10.sp)
        }
    }
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
