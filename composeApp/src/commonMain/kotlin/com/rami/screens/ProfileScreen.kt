package com.rami.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rami.online.model.Achievement
import com.rami.online.model.DailyMission
import com.rami.online.model.OnlinePlayer
import com.rami.online.model.PlayerStats
import com.rami.online.service.StatsService
import com.rami.ui.theme.RamiColors
import com.rami.ui.theme.RamiTheme
import kotlin.math.roundToInt

@Composable
fun ProfileScreen(
    player: OnlinePlayer,
    statsService: StatsService,
    onBack: () -> Unit
) {
    val stats        by statsService.observeStats(player.uid).collectAsState(null)
    val achievements by statsService.observeAchievements(player.uid).collectAsState(emptyList())
    val missions     by statsService.observeDailyMissions(player.uid).collectAsState(emptyList())
    var tab by remember { mutableStateOf(0) }

    RamiTheme {
        Column(
            Modifier.fillMaxSize().background(RamiColors.DarkGreen)
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                TextButton(onClick = onBack) { Text("← رجوع", color = RamiColors.Gold) }
            }

            // Avatar + name
            Row(
                Modifier.fillMaxWidth().padding(vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Box(
                    Modifier.size(72.dp).background(RamiColors.Gold.copy(alpha = 0.2f), CircleShape),
                    contentAlignment = Alignment.Center
                ) { Text(avatarEmoji(player.avatarId), fontSize = 36.sp) }
                Column {
                    Text(player.username, color = RamiColors.Gold, fontWeight = FontWeight.Bold, fontSize = 20.sp)
                    Text("${player.leagueTier.displayAr}  •  Rating: ${player.rating}",
                        color = RamiColors.TextLight.copy(alpha = 0.6f), fontSize = 13.sp)
                }
            }

            // Stats summary
            stats?.let { s ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                    StatChip("🎮", "${s.gamesPlayed}", "ألعاب")
                    StatChip("🏆", "${s.gamesWon}", "انتصار")
                    StatChip("📈", "${(s.winRate * 100).roundToInt()}%", "فوز%")
                    StatChip("🔥", "${s.bestStreak}", "أطول سلسلة")
                }
            }

            Spacer(Modifier.height(8.dp))
            HorizontalDivider(color = RamiColors.Gold.copy(alpha = 0.3f))

            // Tabs
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(vertical = 8.dp)) {
                listOf("مهام اليوم" to "Daily", "الإنجازات" to "Achievements").forEachIndexed { i, (ar, en) ->
                    FilterChip(
                        selected = tab == i, onClick = { tab = i },
                        label = { Text("$ar  •  $en", fontSize = 12.sp, fontWeight = FontWeight.Bold) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = RamiColors.Gold,
                            selectedLabelColor     = RamiColors.DarkGreen,
                            labelColor             = RamiColors.TextLight)
                    )
                }
            }

            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (tab == 0) {
                    items(missions) { MissionRow(it) }
                    if (missions.isEmpty()) item {
                        Text("لا توجد مهام اليوم  •  No missions today",
                            color = RamiColors.TextLight.copy(alpha = 0.4f),
                            modifier = Modifier.padding(16.dp))
                    }
                } else {
                    items(achievements) { AchievementRow(it) }
                }
            }
        }
    }
}

@Composable
private fun StatChip(emoji: String, value: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(emoji, fontSize = 20.sp)
        Text(value, color = RamiColors.Gold, fontWeight = FontWeight.Bold, fontSize = 16.sp)
        Text(label, color = RamiColors.TextLight.copy(alpha = 0.5f), fontSize = 11.sp)
    }
}

@Composable
private fun MissionRow(mission: DailyMission) {
    Card(
        Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (mission.isCompleted) RamiColors.Gold.copy(alpha = 0.15f)
                             else RamiColors.TextLight.copy(alpha = 0.05f)),
        shape = RoundedCornerShape(10.dp)
    ) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically) {
                Text(mission.titleAr, color = if (mission.isCompleted) RamiColors.Gold else RamiColors.TextLight,
                    fontWeight = FontWeight.Bold)
                Text(if (mission.isCompleted) "✅" else "+${mission.xpReward} XP",
                    color = RamiColors.Gold, fontSize = 12.sp)
            }
            Text(mission.descAr, color = RamiColors.TextLight.copy(alpha = 0.6f), fontSize = 12.sp)
            LinearProgressIndicator(
                progress = { mission.progress },
                modifier = Modifier.fillMaxWidth().height(4.dp),
                color = RamiColors.Gold,
                trackColor = RamiColors.TextLight.copy(alpha = 0.1f)
            )
            Text("${mission.current}/${mission.target}",
                color = RamiColors.TextLight.copy(alpha = 0.4f), fontSize = 10.sp)
        }
    }
}

@Composable
private fun AchievementRow(achievement: Achievement) {
    val alpha = if (achievement.isUnlocked) 1f else 0.35f
    Card(
        Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (achievement.isUnlocked) RamiColors.Gold.copy(alpha = 0.15f)
                             else RamiColors.TextLight.copy(alpha = 0.05f)),
        shape = RoundedCornerShape(10.dp)
    ) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(achievement.emoji, fontSize = 28.sp,
                modifier = Modifier.background(RamiColors.TextLight.copy(alpha = 0.07f * alpha), CircleShape).padding(8.dp))
            Column(Modifier.weight(1f)) {
                Text(achievement.titleAr, color = if (achievement.isUnlocked) RamiColors.Gold else RamiColors.TextLight.copy(alpha = alpha),
                    fontWeight = FontWeight.Bold)
                Text(achievement.descAr, color = RamiColors.TextLight.copy(alpha = 0.5f * alpha), fontSize = 12.sp)
            }
            if (!achievement.isUnlocked) Text("🔒", fontSize = 18.sp)
        }
    }
}

private fun avatarEmoji(id: Int) = listOf("👤","🐯","🦁","🐺","🦊","🐻","🐼","🦅","🦋","🐉").getOrElse(id) { "👤" }
