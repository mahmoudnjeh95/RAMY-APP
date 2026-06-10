package com.rami.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rami.ai.AiDifficulty
import com.rami.model.GameMode
import com.rami.ui.theme.RamiColors
import com.rami.ui.theme.RamiTheme

@Composable
fun LobbyScreen(
    mode: GameMode,
    onStartGame: (playerNames: List<String>, scoreLimit: Int, aiIndices: Set<Int>, difficulty: AiDifficulty) -> Unit,
    onBack: () -> Unit
) {
    var playerCount  by remember { mutableStateOf(2) }
    var scoreLimit   by remember { mutableStateOf(150) }
    var names        by remember {
        mutableStateOf(listOf("أنت", "لاعب 2", "لاعب 3", "لاعب 4"))
    }
    var aiSlots      by remember { mutableStateOf(setOf(1, 2, 3)) }
    var aiDifficulty by remember { mutableStateOf(AiDifficulty.MEDIUM) }

    RamiTheme {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(RamiColors.DarkGreen)
                .padding(horizontal = 24.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            // ── Header ─────────────────────────────────────────────────────────
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = onBack) {
                    Text("← رجوع", color = RamiColors.Gold)
                }
                Spacer(Modifier.weight(1f))
                Text(
                    text       = "${mode.displayAr}  •  ${mode.displayEn}",
                    fontSize   = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color      = RamiColors.Gold
                )
            }

            HorizontalDivider(color = RamiColors.Gold.copy(alpha = 0.3f))

            // ── Player count ───────────────────────────────────────────────────
            SectionLabel("عدد اللاعبين — Players")
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                listOf(2, 4).forEach { count ->
                    FilterChip(
                        selected = playerCount == count,
                        onClick  = { playerCount = count },
                        label    = { Text("$count لاعبين", fontWeight = FontWeight.Bold) },
                        colors   = filterChipColors()
                    )
                }
            }

            // ── Player names & AI toggle ────────────────────────────────────────
            SectionLabel("أسماء اللاعبين — Player Names")
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                repeat(playerCount) { idx ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value          = names[idx],
                            onValueChange  = { v -> names = names.toMutableList().also { it[idx] = v } },
                            label          = { Text("لاعب ${idx + 1}") },
                            singleLine     = true,
                            modifier       = Modifier.weight(1f),
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                            colors         = textFieldColors(),
                            enabled        = idx > 0  // slot 0 always human
                        )
                        if (idx > 0) {
                            // AI toggle
                            FilterChip(
                                selected = idx in aiSlots,
                                onClick  = {
                                    aiSlots = if (idx in aiSlots) aiSlots - idx else aiSlots + idx
                                },
                                label    = { Text(if (idx in aiSlots) "AI" else "إنسان", fontSize = 11.sp) },
                                colors   = filterChipColors()
                            )
                        } else {
                            // Fixed label for local player
                            Text("👤", fontSize = 22.sp)
                        }
                    }
                }
            }

            // ── Score limit ────────────────────────────────────────────────────
            SectionLabel("حد النقاط — Score Limit")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(100, 150, 200, 250).forEach { limit ->
                    FilterChip(
                        selected = scoreLimit == limit,
                        onClick  = { scoreLimit = limit },
                        label    = { Text("$limit", fontWeight = FontWeight.Bold) },
                        colors   = filterChipColors()
                    )
                }
            }

            // ── AI difficulty (shown only when any AI slots selected) ──────────
            if (aiSlots.any { it < playerCount }) {
                SectionLabel("صعوبة الذكاء — AI Difficulty")
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    AiDifficulty.entries.forEach { diff ->
                        FilterChip(
                            selected = aiDifficulty == diff,
                            onClick  = { aiDifficulty = diff },
                            label    = { Text(diff.displayAr, fontWeight = FontWeight.Bold) },
                            colors   = filterChipColors()
                        )
                    }
                }
            }

            Spacer(Modifier.weight(1f))

            // ── Start button ───────────────────────────────────────────────────
            Button(
                onClick  = {
                    onStartGame(
                        names.take(playerCount),
                        scoreLimit,
                        aiSlots.filter { it < playerCount }.toSet(),
                        aiDifficulty
                    )
                },
                modifier = Modifier.fillMaxWidth().height(60.dp),
                shape    = RoundedCornerShape(18.dp),
                colors   = ButtonDefaults.buttonColors(
                    containerColor = RamiColors.Gold,
                    contentColor   = RamiColors.DarkGreen
                )
            ) {
                Text("ابدأ اللعبة  —  Start Game", fontSize = 18.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

// ── Helpers ───────────────────────────────────────────────────────────────────

@Composable
private fun SectionLabel(text: String) {
    Text(text, color = RamiColors.TextLight.copy(alpha = 0.7f), fontSize = 13.sp)
}

@Composable
private fun filterChipColors() = FilterChipDefaults.filterChipColors(
    selectedContainerColor = RamiColors.Gold,
    selectedLabelColor     = RamiColors.DarkGreen,
    labelColor             = RamiColors.TextLight
)

@Composable
private fun textFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor   = RamiColors.Gold,
    unfocusedBorderColor = RamiColors.TextLight.copy(alpha = 0.3f),
    focusedLabelColor    = RamiColors.Gold,
    unfocusedLabelColor  = RamiColors.TextLight.copy(alpha = 0.5f),
    focusedTextColor     = RamiColors.TextLight,
    unfocusedTextColor   = RamiColors.TextLight,
    disabledTextColor    = RamiColors.Gold,
    disabledBorderColor  = RamiColors.Gold.copy(alpha = 0.4f),
    disabledLabelColor   = RamiColors.Gold.copy(alpha = 0.6f)
)
