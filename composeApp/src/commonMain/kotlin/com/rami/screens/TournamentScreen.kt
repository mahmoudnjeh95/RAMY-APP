package com.rami.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rami.online.model.Tournament
import com.rami.online.model.TournamentStatus
import com.rami.online.service.TournamentService
import com.rami.ui.theme.RamiColors
import com.rami.ui.theme.RamiTheme
import kotlinx.coroutines.launch

@Composable
fun TournamentScreen(
    tournamentService: TournamentService,
    localUid: String,
    onBack: () -> Unit
) {
    val tournaments by tournamentService.observeOpenTournaments().collectAsState(emptyList())
    val scope = rememberCoroutineScope()

    RamiTheme {
        Column(
            Modifier.fillMaxSize().background(RamiColors.DarkGreen)
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                TextButton(onClick = onBack) { Text("← رجوع", color = RamiColors.Gold) }
                Spacer(Modifier.weight(1f))
                Text("🏅 البطولات  •  Tournaments",
                    color = RamiColors.Gold, fontWeight = FontWeight.Bold)
            }

            HorizontalDivider(color = RamiColors.Gold.copy(alpha = 0.3f))
            Spacer(Modifier.height(8.dp))

            LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                items(tournaments) { t ->
                    TournamentCard(
                        tournament = t,
                        isRegistered = localUid in t.registeredPlayers,
                        onRegister = {
                            scope.launch { tournamentService.register(t.id, localUid) }
                        },
                        onUnregister = {
                            scope.launch { tournamentService.unregister(t.id, localUid) }
                        }
                    )
                }
                if (tournaments.isEmpty()) item {
                    Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                        Text("لا توجد بطولات قادمة  •  No upcoming tournaments",
                            color = RamiColors.TextLight.copy(alpha = 0.4f))
                    }
                }
            }
        }
    }
}

@Composable
private fun TournamentCard(
    tournament: Tournament,
    isRegistered: Boolean,
    onRegister: () -> Unit,
    onUnregister: () -> Unit
) {
    Card(
        Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = RamiColors.Gold.copy(alpha = 0.1f)),
        shape  = RoundedCornerShape(14.dp)
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically) {
                Text(tournament.titleAr, color = RamiColors.Gold,
                    fontWeight = FontWeight.Bold, fontSize = 16.sp)
                StatusChip(tournament.status)
            }
            Text(tournament.mode.displayAr + "  •  " + tournament.mode.displayEn,
                color = RamiColors.TextLight.copy(alpha = 0.6f), fontSize = 12.sp)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("👥 ${tournament.registeredPlayers.size}/${tournament.maxPlayers}",
                    color = RamiColors.TextLight.copy(alpha = 0.7f), fontSize = 13.sp)
                Text(tournament.prizeDescription, color = RamiColors.Gold, fontSize = 13.sp)
            }
            if (tournament.status == TournamentStatus.OPEN) {
                Button(
                    onClick = if (isRegistered) onUnregister else onRegister,
                    modifier = Modifier.fillMaxWidth().height(42.dp),
                    shape    = RoundedCornerShape(10.dp),
                    colors   = ButtonDefaults.buttonColors(
                        containerColor = if (isRegistered) MaterialTheme.colorScheme.error else RamiColors.Gold,
                        contentColor   = if (isRegistered) MaterialTheme.colorScheme.onError else RamiColors.DarkGreen
                    )
                ) {
                    Text(if (isRegistered) "إلغاء التسجيل  •  Unregister" else "تسجيل  •  Register",
                        fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }
            }
        }
    }
}

@Composable
private fun StatusChip(status: TournamentStatus) {
    val (text, color) = when (status) {
        TournamentStatus.OPEN        -> "مفتوح  •  Open"      to RamiColors.Gold
        TournamentStatus.IN_PROGRESS -> "جارٍ  •  Live"       to androidx.compose.ui.graphics.Color(0xFF4CAF50)
        TournamentStatus.FINISHED    -> "منتهي  •  Finished"  to RamiColors.TextLight.copy(alpha = 0.4f)
    }
    Text(text, color = color, fontSize = 11.sp,
        modifier = Modifier.background(color.copy(alpha = 0.15f), RoundedCornerShape(6.dp)).padding(4.dp, 2.dp))
}
