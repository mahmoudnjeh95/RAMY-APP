package com.rami.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rami.online.model.LeagueEntry
import com.rami.online.model.LeagueTier
import com.rami.online.service.LeagueService
import com.rami.ui.theme.RamiColors
import com.rami.ui.theme.RamiTheme

@Composable
fun LeagueScreen(
    leagueService: LeagueService,
    localUid: String,
    onBack: () -> Unit
) {
    val leaderboard by leagueService.observeLeaderboard(100).collectAsState(emptyList())
    val myEntry     by leagueService.observeMyEntry(localUid).collectAsState(null)

    RamiTheme {
        Column(
            modifier = Modifier.fillMaxSize().background(RamiColors.DarkGreen)
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                TextButton(onClick = onBack) { Text("← رجوع", color = RamiColors.Gold) }
                Spacer(Modifier.weight(1f))
                Text("🏆 الدوري الأسبوعي  •  Weekly League",
                    color = RamiColors.Gold, fontWeight = FontWeight.Bold, fontSize = 15.sp)
            }

            HorizontalDivider(color = RamiColors.Gold.copy(alpha = 0.3f))
            Spacer(Modifier.height(8.dp))

            // My rank card
            myEntry?.let { my ->
                Card(
                    Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = RamiColors.Gold.copy(alpha = 0.15f)),
                    shape  = RoundedCornerShape(12.dp)
                ) {
                    Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween) {
                        Column {
                            Text("ترتيبي  •  My Rank", color = RamiColors.TextLight.copy(alpha = 0.6f), fontSize = 11.sp)
                            Text("#${my.rank}  ${my.username}", color = RamiColors.Gold,
                                fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text(tierEmoji(my.tier) + " " + my.tier.displayAr,
                                color = RamiColors.Gold, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                            Text("${my.points} نقطة  •  pts",
                                color = RamiColors.TextLight.copy(alpha = 0.7f), fontSize = 13.sp)
                        }
                    }
                }
                Spacer(Modifier.height(8.dp))
            }

            // Leaderboard
            Text("الترتيب العام  •  Leaderboard",
                color = RamiColors.TextLight.copy(alpha = 0.6f), fontSize = 12.sp,
                modifier = Modifier.padding(vertical = 4.dp))

            LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                itemsIndexed(leaderboard) { idx, entry ->
                    LeagueRow(rank = idx + 1, entry = entry, isMe = entry.uid == localUid)
                }
            }
        }
    }
}

@Composable
private fun LeagueRow(rank: Int, entry: LeagueEntry, isMe: Boolean) {
    val bg = when {
        isMe  -> RamiColors.Gold.copy(alpha = 0.2f)
        rank == 1 -> RamiColors.Gold.copy(alpha = 0.12f)
        else  -> RamiColors.TextLight.copy(alpha = 0.05f)
    }
    Row(
        modifier = Modifier.fillMaxWidth().background(bg, RoundedCornerShape(8.dp)).padding(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(rankDisplay(rank), fontSize = 18.sp, modifier = Modifier.width(36.dp))
        Spacer(Modifier.width(8.dp))
        Text(entry.username, color = if (isMe) RamiColors.Gold else RamiColors.TextLight,
            fontWeight = if (isMe) FontWeight.Bold else FontWeight.Normal,
            modifier = Modifier.weight(1f))
        Text(tierEmoji(entry.tier), fontSize = 14.sp)
        Spacer(Modifier.width(8.dp))
        Text("${entry.points}", color = RamiColors.Gold, fontWeight = FontWeight.Bold, fontSize = 14.sp)
    }
}

private fun rankDisplay(rank: Int) = when (rank) {
    1 -> "🥇"; 2 -> "🥈"; 3 -> "🥉"; else -> "#$rank"
}

private fun tierEmoji(tier: LeagueTier) = when (tier) {
    LeagueTier.BRONZE   -> "🥉"
    LeagueTier.SILVER   -> "🥈"
    LeagueTier.GOLD     -> "🥇"
    LeagueTier.PLATINUM -> "💠"
    LeagueTier.DIAMOND  -> "💎"
}
