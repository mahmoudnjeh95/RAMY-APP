package com.rami.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.*
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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

    val fraction = timer.remainingSeconds.toFloat() / TURN_DURATION_SECONDS
    val urgent   = timer.remainingSeconds <= 10

    val barColor by animateColorAsState(
        targetValue = when {
            timer.remainingSeconds <= 5  -> Color(0xFFE53935)
            timer.remainingSeconds <= 10 -> Color(0xFFFF9800)
            else                         -> RamiColors.Gold
        },
        animationSpec = tween(300)
    )

    Column(modifier) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                if (urgent) "⚡ ${timer.remainingSeconds}s" else "${timer.remainingSeconds}s",
                color = barColor,
                fontWeight = if (urgent) FontWeight.Bold else FontWeight.Normal,
                fontSize = if (urgent) 15.sp else 13.sp
            )
            Text(timer.activePlayerId.take(12),
                color = RamiColors.TextLight.copy(alpha = 0.5f), fontSize = 11.sp)
        }
        Spacer(Modifier.height(4.dp))
        LinearProgressIndicator(
            progress = { fraction },
            modifier = Modifier.fillMaxWidth().height(6.dp),
            color    = barColor,
            trackColor = RamiColors.TextLight.copy(alpha = 0.1f)
        )
    }
}
