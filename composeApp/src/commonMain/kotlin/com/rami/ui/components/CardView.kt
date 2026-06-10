package com.rami.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rami.model.Card
import com.rami.model.Suit
import com.rami.ui.theme.RamiColors

// ─── Standard card size constants ─────────────────────────────────────────────

object CardSize {
    val Width:  Dp = 54.dp
    val Height: Dp = 78.dp
    val SmallWidth:  Dp = 40.dp
    val SmallHeight: Dp = 58.dp
}

// ─── Card composable ──────────────────────────────────────────────────────────

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
    val shape  = RoundedCornerShape(if (small) 6.dp else 8.dp)

    val borderColor = when {
        selected  -> RamiColors.Gold
        faceDown  -> RamiColors.FeltGreen.copy(alpha = 0.4f)
        else      -> Color.Gray.copy(alpha = 0.5f)
    }

    Box(
        modifier = modifier
            .width(width)
            .height(height)
            .shadow(if (selected) 6.dp else 2.dp, shape)
            .clip(shape)
            .background(if (faceDown) RamiColors.FeltGreen else RamiColors.CardWhite)
            .border(if (selected) 2.dp else 0.5.dp, borderColor, shape)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
        contentAlignment = Alignment.Center
    ) {
        when {
            faceDown         -> CardBack()
            card is Card.Joker   -> JokerContent(small)
            card is Card.Regular -> RegularContent(card, small)
        }
    }
}

// ─── Card content ─────────────────────────────────────────────────────────────

@Composable
private fun CardBack() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(3.dp)
            .background(RamiColors.DarkGreen.copy(alpha = 0.5f), RoundedCornerShape(6.dp)),
        contentAlignment = Alignment.Center
    ) {
        Text("🃏", fontSize = 20.sp, textAlign = TextAlign.Center)
    }
}

@Composable
private fun RegularContent(card: Card.Regular, small: Boolean) {
    val color = when (card.suit) {
        Suit.HEARTS, Suit.DIAMONDS -> RamiColors.CardRed
        Suit.CLUBS,  Suit.SPADES   -> RamiColors.CardBlack
    }
    val rankSize = if (small) 12.sp else 15.sp
    val suitSize = if (small) 14.sp else 18.sp

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(if (small) 2.dp else 4.dp),
        horizontalAlignment = Alignment.Start,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // Top-left corner
        Column(horizontalAlignment = Alignment.Start) {
            Text(card.rank.display,  color = color, fontSize = rankSize, fontWeight = FontWeight.Bold, lineHeight = rankSize)
            Text(card.suit.symbol,   color = color, fontSize = rankSize, lineHeight = rankSize)
        }
        // Centre symbol
        Text(
            text      = card.suit.symbol,
            color     = color,
            fontSize  = suitSize,
            modifier  = Modifier.align(Alignment.CenterHorizontally)
        )
        // Bottom-right corner (rotated 180°)
        Column(
            horizontalAlignment = Alignment.End,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(card.suit.symbol,   color = color, fontSize = rankSize, lineHeight = rankSize)
            Text(card.rank.display,  color = color, fontSize = rankSize, fontWeight = FontWeight.Bold, lineHeight = rankSize)
        }
    }
}

@Composable
private fun JokerContent(small: Boolean) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text("★",    color = RamiColors.JokerPurple, fontSize = if (small) 16.sp else 22.sp)
        Text("JKR",  color = RamiColors.JokerPurple, fontSize = if (small) 8.sp  else 11.sp, fontWeight = FontWeight.Bold)
    }
}
