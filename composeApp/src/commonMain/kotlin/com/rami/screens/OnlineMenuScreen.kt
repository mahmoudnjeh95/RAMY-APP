package com.rami.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rami.model.GameMode
import com.rami.navigation.Screen
import com.rami.online.model.OnlinePlayer
import com.rami.ui.theme.RamiColors
import com.rami.ui.theme.RamiTheme

@Composable
fun OnlineMenuScreen(
    player: OnlinePlayer,
    onNavigate: (Screen) -> Unit,
    onBack: () -> Unit
) {
    RamiTheme {
        Column(
            modifier = Modifier.fillMaxSize().background(RamiColors.DarkGreen)
                .padding(horizontal = 24.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                TextButton(onClick = onBack) { Text("← رجوع", color = RamiColors.Gold) }
                Spacer(Modifier.weight(1f))
                Text("مرحبا  ${player.username}", color = RamiColors.Gold, fontWeight = FontWeight.Bold)
                Spacer(Modifier.width(8.dp))
                Text(player.leagueTier.displayAr, color = RamiColors.TextLight.copy(alpha = 0.7f), fontSize = 12.sp)
            }

            HorizontalDivider(color = RamiColors.Gold.copy(alpha = 0.3f))

            Text("العب أونلاين  •  Play Online",
                fontSize = 24.sp, fontWeight = FontWeight.Bold, color = RamiColors.Gold)
            Spacer(Modifier.height(4.dp))

            OnlineMenuButton("⚡", "لعب سريع", "Quick Match — عادي Normal",
                onClick = { onNavigate(Screen.Matchmaking(GameMode.NORMAL)) })
            OnlineMenuButton("⭐", "لعب سريع تفضيل", "Quick Match — تفضيل Tafdhil",
                onClick = { onNavigate(Screen.Matchmaking(GameMode.TAFDHIL)) })
            OnlineMenuButton("🔒", "طاولة خاصة", "Private Table — شارك الكود مع أصدقائك",
                onClick = { onNavigate(Screen.PrivateTable) })
            OnlineMenuButton("🏆", "الدوري الأسبوعي", "Weekly League — الترتيب الأسبوعي",
                onClick = { onNavigate(Screen.League) })
            OnlineMenuButton("🤖", "ضد الروبوت", "vs AI — تدرب ضد الذكاء الاصطناعي",
                onClick = { onNavigate(Screen.Lobby(GameMode.NORMAL)) })
            OnlineMenuButton("👥", "الأصدقاء", "Friends — أضف وتحدى أصدقاءك",
                onClick = { onNavigate(Screen.Friends) })
            OnlineMenuButton("🏅", "البطولات", "Tournaments — بطولات مجدولة",
                onClick = { onNavigate(Screen.Tournaments) })
            OnlineMenuButton("👤", "ملفي", "My Profile — الإحصائيات والإنجازات",
                onClick = { onNavigate(Screen.Profile) })
        }
    }
}

@Composable
private fun OnlineMenuButton(emoji: String, arabic: String, english: String, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().height(64.dp),
        shape  = RoundedCornerShape(16.dp),
        colors = ButtonDefaults.buttonColors(containerColor = RamiColors.Gold.copy(alpha = 0.15f),
            contentColor = RamiColors.TextLight),
        border = androidx.compose.foundation.BorderStroke(1.dp, RamiColors.Gold.copy(alpha = 0.4f))
    ) {
        Row(verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxWidth()) {
            Text(emoji, fontSize = 26.sp)
            Column {
                Text(arabic, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = RamiColors.Gold)
                Text(english, fontSize = 11.sp, color = RamiColors.TextLight.copy(alpha = 0.5f))
            }
        }
    }
}
