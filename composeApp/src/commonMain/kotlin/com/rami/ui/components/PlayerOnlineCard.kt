package com.rami.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rami.online.model.RoomSlot
import com.rami.ui.theme.RamiColors

@Composable
fun PlayerOnlineCard(
    slot: RoomSlot,
    isCurrentTurn: Boolean,
    strikeCount: Int,
    cardCount: Int,
    modifier: Modifier = Modifier
) {
    val border = if (isCurrentTurn) RamiColors.Gold else RamiColors.TextLight.copy(alpha = 0.15f)

    Row(
        modifier = modifier
            .background(
                if (isCurrentTurn) RamiColors.Gold.copy(alpha = 0.12f) else RamiColors.TextLight.copy(alpha = 0.04f),
                RoundedCornerShape(10.dp)
            )
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Avatar circle
        Box(
            Modifier.size(36.dp)
                .background(RamiColors.Gold.copy(alpha = 0.2f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(if (slot.isAi) "🤖" else "👤", fontSize = 18.sp)
        }

        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(slot.username.take(12),
                    color = if (isCurrentTurn) RamiColors.Gold else RamiColors.TextLight,
                    fontWeight = if (isCurrentTurn) FontWeight.Bold else FontWeight.Normal,
                    fontSize = 13.sp)
                if (!slot.isConnected) Text("📡", fontSize = 12.sp)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("🃏 $cardCount", color = RamiColors.TextLight.copy(alpha = 0.6f), fontSize = 11.sp)
                if (strikeCount > 0) {
                    Text("⚠️ $strikeCount", color = androidx.compose.ui.graphics.Color(0xFFFF9800), fontSize = 11.sp)
                }
            }
        }

        if (isCurrentTurn) {
            Text("▶", color = RamiColors.Gold, fontSize = 14.sp, fontWeight = FontWeight.Bold)
        }
    }
}
