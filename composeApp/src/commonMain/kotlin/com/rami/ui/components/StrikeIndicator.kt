package com.rami.ui.components

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rami.online.model.MAX_STRIKES

/** Shows the strike dots for a single player (●●○ style). */
@Composable
fun StrikeIndicator(
    username: String,
    strikes: Int,
    modifier: Modifier = Modifier
) {
    if (strikes == 0) return
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(username.take(10), fontSize = 11.sp, color = Color(0xFFFF9800))
        repeat(MAX_STRIKES) { i ->
            Text(if (i < strikes) "●" else "○",
                color = if (i < strikes) Color(0xFFE53935) else Color.White.copy(alpha = 0.3f),
                fontSize = 12.sp)
        }
    }
}

/** Full-screen warning overlay shown when a player gets a strike. */
@Composable
fun StrikeWarningBanner(username: String, strikes: Int, modifier: Modifier = Modifier) {
    AnimatedVisibility(
        visible = strikes in 1 until MAX_STRIKES,
        enter = fadeIn() + slideInVertically(),
        exit  = fadeOut() + slideOutVertically(),
        modifier = modifier
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .background(Color(0xFFE53935).copy(alpha = 0.9f), RoundedCornerShape(12.dp))
                .padding(horizontal = 16.dp, vertical = 10.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                "⚠️  $username — إنذار $strikes/$MAX_STRIKES  •  Strike $strikes/$MAX_STRIKES",
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp
            )
        }
    }
}

/** Kick banner shown when a player is removed. */
@Composable
fun KickBanner(username: String, modifier: Modifier = Modifier) {
    Box(
        modifier.fillMaxWidth()
            .background(Color(0xFF880E4F).copy(alpha = 0.95f), RoundedCornerShape(12.dp))
            .padding(horizontal = 16.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            "🚫  $username طُرد من اللعبة  •  was kicked",
            color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp
        )
    }
}
