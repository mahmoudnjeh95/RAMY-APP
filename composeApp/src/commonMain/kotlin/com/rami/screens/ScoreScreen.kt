package com.rami.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rami.model.GameMode
import com.rami.model.GamePhase
import com.rami.model.GameState
import com.rami.model.Player
import com.rami.ui.theme.RamiColors
import com.rami.ui.theme.RamiTheme

@Composable
fun ScoreScreen(
    state: GameState,
    onNextRound: () -> Unit,
    onSecondLife: (playerId: String) -> Unit,
    onMainMenu: () -> Unit
) {
    val isGameOver = state.gamePhase == GamePhase.GAME_OVER
    val sorted     = state.players.sortedBy { it.score }

    RamiTheme {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(RamiColors.DarkGreen)
                .padding(horizontal = 24.dp, vertical = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // ── Title ──────────────────────────────────────────────────────────
            Text(
                text = if (isGameOver) "🏆  انتهت اللعبة!\nGame Over"
                       else "✅  الجولة ${state.roundNumber}\nRound ${state.roundNumber} Results",
                fontSize   = 24.sp,
                fontWeight = FontWeight.Bold,
                color      = RamiColors.Gold,
                textAlign  = TextAlign.Center
            )

            HorizontalDivider(color = RamiColors.Gold.copy(alpha = 0.4f))

            // ── Player score rows ──────────────────────────────────────────────
            sorted.forEachIndexed { rank, player ->
                PlayerScoreRow(
                    rank       = rank + 1,
                    player     = player,
                    isWinner   = player.id == state.roundWinnerId,
                    scoreLimit = state.scoreLimit,
                    mode       = state.mode
                )
            }

            // ── Score limit reminder ───────────────────────────────────────────
            Text(
                "حد النقاط — Limit: ${state.scoreLimit}",
                color    = RamiColors.TextLight.copy(alpha = 0.5f),
                fontSize = 12.sp
            )

            Spacer(Modifier.weight(1f))

            // ── Second-life offers (4-player only) ────────────────────────────
            if (!isGameOver && state.players.size == 4) {
                val atLimit = state.players.filter {
                    it.score >= state.scoreLimit && !it.isEliminated && !it.hasSecondLife
                }
                atLimit.forEach { p ->
                    SecondLifeCard(player = p, scoreLimit = state.scoreLimit, onAccept = { onSecondLife(p.id) })
                }
            }

            // ── Navigation buttons ────────────────────────────────────────────
            if (isGameOver) {
                GoldButton(label = "🏠  القائمة الرئيسية  •  Main Menu", onClick = onMainMenu)
            } else {
                GoldButton(label = "الجولة التالية  →  Next Round", onClick = onNextRound)
                TextButton(onClick = onMainMenu) {
                    Text("خروج — Quit", color = RamiColors.TextLight.copy(alpha = 0.5f))
                }
            }
        }
    }
}

// ─── Player score row ─────────────────────────────────────────────────────────

@Composable
private fun PlayerScoreRow(
    rank: Int,
    player: Player,
    isWinner: Boolean,
    scoreLimit: Int,
    mode: GameMode
) {
    val atLimit    = player.score >= scoreLimit
    val bgColor    = when {
        isWinner -> RamiColors.Gold.copy(alpha = 0.18f)
        atLimit  -> Color(0xFFD32F2F).copy(alpha = 0.18f)
        else     -> RamiColors.TableSurface.copy(alpha = 0.5f)
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(bgColor, RoundedCornerShape(12.dp))
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment     = Alignment.CenterVertically
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment     = Alignment.CenterVertically
        ) {
            Text(
                text = when (rank) { 1 -> "🥇"; 2 -> "🥈"; 3 -> "🥉"; else -> "$rank." },
                fontSize = 20.sp
            )
            Column {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(player.name, color = RamiColors.TextLight, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                    if (isWinner) Text("فاز ✓", color = RamiColors.Gold,     fontSize = 11.sp)
                    if (atLimit)  Text("حدّ ✗",  color = Color(0xFFFF7043),   fontSize = 11.sp)
                    if (player.hasSecondLife) Text("حياة 2", color = RamiColors.JokerPurple, fontSize = 11.sp)
                }
                if (mode == GameMode.TAFDHIL && player.jokerBankCount > 0) {
                    Text(
                        "جوكر بنك: ${player.jokerBankCount}/4",
                        color    = RamiColors.JokerPurple,
                        fontSize = 10.sp
                    )
                }
            }
        }

        Text(
            text       = "${player.score}",
            color      = when {
                atLimit  -> Color(0xFFFF7043)
                isWinner -> RamiColors.Gold
                else     -> RamiColors.TextLight
            },
            fontSize   = 22.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

// ─── Second-life offer card ────────────────────────────────────────────────────

@Composable
private fun SecondLifeCard(player: Player, scoreLimit: Int, onAccept: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape    = RoundedCornerShape(14.dp),
        colors   = CardDefaults.cardColors(containerColor = RamiColors.JokerPurple.copy(alpha = 0.2f))
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment     = Alignment.CenterVertically
        ) {
            Column {
                Text("حياة ثانية لـ ${player.name}?", color = RamiColors.TextLight, fontSize = 14.sp)
                Text(
                    "الحد الجديد: ${scoreLimit + 50} نقطة",
                    color    = RamiColors.TextLight.copy(alpha = 0.6f),
                    fontSize = 11.sp
                )
            }
            Button(
                onClick = onAccept,
                colors  = ButtonDefaults.buttonColors(containerColor = RamiColors.JokerPurple),
                shape   = RoundedCornerShape(8.dp)
            ) {
                Text("نعم", color = Color.White)
            }
        }
    }
}

// ─── Shared button ────────────────────────────────────────────────────────────

@Composable
private fun GoldButton(label: String, onClick: () -> Unit) {
    Button(
        onClick  = onClick,
        modifier = Modifier.fillMaxWidth().height(58.dp),
        shape    = RoundedCornerShape(16.dp),
        colors   = ButtonDefaults.buttonColors(
            containerColor = RamiColors.Gold,
            contentColor   = RamiColors.DarkGreen
        )
    ) {
        Text(label, fontSize = 17.sp, fontWeight = FontWeight.Bold)
    }
}
