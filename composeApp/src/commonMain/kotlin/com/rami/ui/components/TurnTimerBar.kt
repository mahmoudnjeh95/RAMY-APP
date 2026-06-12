package com.rami.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.*
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rami.online.model.TURN_DURATION_SECONDS
import com.rami.online.model.TurnTimer
import com.rami.ui.theme.RamiColors

@Composable
fun TurnTimerBar(timer: TurnTimer, modifier: Modifier = Modifier) {
    if (!timer.isRunning) return

    val fraction  = timer.remainingSeconds.toFloat() / TURN_DURATION_SECONDS
    val urgent    = timer.remainingSeconds <= 10
    val critical  = timer.remainingSeconds <= 5

    val barColor by animateColorAsState(
        targetValue = when {
            critical -> Color(0xFFE53935)
            urgent   -> Color(0xFFFF9800)
            else     -> RamiColors.Gold
        },
        animationSpec = tween(300)
    )

    // Horizontal shake animation — always running, applied only when critical
    val shakeTransition = rememberInfiniteTransition(label = "shake")
    val rawShake by shakeTransition.animateFloat(
        initialValue  = -3f,
        targetValue   = 3f,
        animationSpec = infiniteRepeatable(
            animation  = tween(80, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "shakeX"
    )
    val shakeOffset = if (critical) rawShake else 0f

    Column(modifier.offset(x = shakeOffset.dp)) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                if (urgent) "⚡ ${timer.remainingSeconds}s" else "${timer.remainingSeconds}s",
                color      = barColor,
                fontWeight = if (urgent) FontWeight.Bold else FontWeight.Normal,
                fontSize   = if (urgent) 15.sp else 13.sp
            )
            Text(
                timer.activePlayerId.take(12),
                color    = RamiColors.TextLight.copy(alpha = 0.5f),
                fontSize = 11.sp
            )
        }
        Spacer(Modifier.height(4.dp))
        LinearProgressIndicator(
            progress   = { fraction },
            modifier   = Modifier.fillMaxWidth().height(6.dp),
            color      = barColor,
            trackColor = RamiColors.TextLight.copy(alpha = 0.1f)
        )
    }
}
