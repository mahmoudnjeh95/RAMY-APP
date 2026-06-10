package com.rami.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rami.model.GameMode
import com.rami.online.service.RoomService
import com.rami.ui.theme.RamiColors
import com.rami.ui.theme.RamiTheme
import kotlinx.coroutines.launch

@Composable
fun MatchmakingScreen(
    mode: GameMode,
    roomService: RoomService,
    onMatchFound: (roomId: String) -> Unit,
    onCancel: () -> Unit
) {
    var secondsWaiting by remember { mutableStateOf(0) }
    val scope = rememberCoroutineScope()

    // Pulse animation
    val pulse = rememberInfiniteTransition()
    val scale by pulse.animateFloat(
        initialValue = 0.9f, targetValue = 1.1f,
        animationSpec = infiniteRepeatable(tween(900), RepeatMode.Reverse)
    )

    // Start matchmaking and timer
    LaunchedEffect(Unit) {
        scope.launch {
            roomService.findMatch(mode).collect { room ->
                if (room != null) onMatchFound(room.roomId)
            }
        }
        while (true) {
            kotlinx.coroutines.delay(1_000)
            secondsWaiting++
        }
    }

    DisposableEffect(Unit) {
        onDispose { scope.launch { roomService.cancelMatchmaking() } }
    }

    RamiTheme {
        Box(
            modifier = Modifier.fillMaxSize().background(RamiColors.DarkGreen),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(24.dp),
                modifier = Modifier.padding(32.dp)
            ) {
                Box(
                    Modifier.size(120.dp).scale(scale)
                        .background(RamiColors.Gold.copy(alpha = 0.2f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text("🔍", fontSize = 48.sp)
                }

                Text("نبحث عن خصم…\nFinding opponent…",
                    fontSize = 22.sp, fontWeight = FontWeight.Bold,
                    color = RamiColors.Gold, textAlign = TextAlign.Center)

                Text(mode.displayAr + "  •  " + mode.displayEn,
                    color = RamiColors.TextLight.copy(alpha = 0.6f), fontSize = 14.sp)

                Text("${secondsWaiting}s", fontSize = 16.sp, color = RamiColors.TextLight.copy(alpha = 0.5f))

                CircularProgressIndicator(color = RamiColors.Gold, strokeWidth = 3.dp)

                OutlinedButton(
                    onClick = {
                        scope.launch { roomService.cancelMatchmaking() }
                        onCancel()
                    },
                    border = androidx.compose.foundation.BorderStroke(1.dp, RamiColors.Gold.copy(alpha = 0.5f))
                ) {
                    Text("إلغاء  •  Cancel", color = RamiColors.Gold)
                }
            }
        }
    }
}
