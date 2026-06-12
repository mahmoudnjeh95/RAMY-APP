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
                .padding(horizontal = 20.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
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
                    fontSize   = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color      = RamiColors.Gold
                )
            }

            HorizontalDivider(color = RamiColors.Gold.copy(alpha = 0.3f))

            // ── Player count + Score limit in one row ──────────────────────────
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    SectionLabel("عدد اللاعبين — Players")
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(2, 4).forEach { count ->
                            FilterChip(
                                selected = playerCount == count,
                                onClick  = { playerCount = count },
                                label    = { Text("$count لاعبين", fontWeight = FontWeight.Bold, fontSize = 13.sp) },
                                colors   = filterChipColors()
                            )
                        }
                    }
                }
                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    SectionLabel("حد النقاط — Score Limit")
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf(100, 150, 200, 250).forEach { limit ->
                            FilterChip(
                                selected = scoreLimit == limit,
                                onClick  = { scoreLimit = limit },
                                label    = { Text("$limit", fontWeight = FontWeight.Bold, fontSize = 12.sp) },
                                colors   = filterChipColors()
                            )
                        }
                    }
                }
            }

            // ── Player names ────────────────────────────────────────────────────
            SectionLabel("أسماء اللاعبين — Player Names")

            if (playerCount == 4) {
                // 2×2 grid: players 0,2 left column — players 1,3 right column
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf(0, 2).forEach { idx ->
                            PlayerNameRow(
                                idx         = idx,
                                names       = names,
                                aiSlots     = aiSlots,
                                onName      = { i, v -> names = names.toMutableList().also { it[i] = v } },
                                onAiToggle  = { i -> aiSlots = if (i in aiSlots) aiSlots - i else aiSlots + i }
                            )
                        }
                    }
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf(1, 3).forEach { idx ->
                            PlayerNameRow(
                                idx         = idx,
                                names       = names,
                                aiSlots     = aiSlots,
                                onName      = { i, v -> names = names.toMutableList().also { it[i] = v } },
                                onAiToggle  = { i -> aiSlots = if (i in aiSlots) aiSlots - i else aiSlots + i }
                            )
                        }
                    }
                }
            } else {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    repeat(playerCount) { idx ->
                        PlayerNameRow(
                            idx         = idx,
                            names       = names,
                            aiSlots     = aiSlots,
                            onName      = { i, v -> names = names.toMutableList().also { it[i] = v } },
                            onAiToggle  = { i -> aiSlots = if (i in aiSlots) aiSlots - i else aiSlots + i }
                        )
                    }
                }
            }

            // ── AI difficulty ──────────────────────────────────────────────────
            if (aiSlots.any { it < playerCount }) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    SectionLabel("صعوبة الذكاء — AI:")
                    AiDifficulty.entries.forEach { diff ->
                        FilterChip(
                            selected = aiDifficulty == diff,
                            onClick  = { aiDifficulty = diff },
                            label    = {
                                Text(
                                    "${diff.displayAr} ${diffEmoji(diff)}",
                                    fontWeight = FontWeight.Bold,
                                    fontSize   = 12.sp
                                )
                            },
                            colors = filterChipColors()
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
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape    = RoundedCornerShape(16.dp),
                colors   = ButtonDefaults.buttonColors(
                    containerColor = RamiColors.Gold,
                    contentColor   = RamiColors.DarkGreen
                )
            ) {
                Text("ابدأ اللعبة  —  Start Game", fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

// ── Single player name row ────────────────────────────────────────────────────

@Composable
private fun PlayerNameRow(
    idx: Int,
    names: List<String>,
    aiSlots: Set<Int>,
    onName: (Int, String) -> Unit,
    onAiToggle: (Int) -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        OutlinedTextField(
            value          = names[idx],
            onValueChange  = { v -> onName(idx, v) },
            label          = { Text("لاعب ${idx + 1}", fontSize = 11.sp) },
            singleLine     = true,
            modifier       = Modifier.weight(1f),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
            colors         = textFieldColors(),
            enabled        = idx > 0
        )
        if (idx > 0) {
            FilterChip(
                selected = idx in aiSlots,
                onClick  = { onAiToggle(idx) },
                label    = {
                    Text(
                        if (idx in aiSlots) "AI" else "👤",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                },
                colors   = filterChipColors(),
                modifier = Modifier.width(52.dp)
            )
        } else {
            Text("👤", fontSize = 20.sp, modifier = Modifier.width(52.dp))
        }
    }
}

// ── Helpers ───────────────────────────────────────────────────────────────────

@Composable
private fun SectionLabel(text: String) {
    Text(text, color = RamiColors.TextLight.copy(alpha = 0.7f), fontSize = 11.sp)
}

@Composable
private fun filterChipColors() = FilterChipDefaults.filterChipColors(
    selectedContainerColor = RamiColors.Gold,
    selectedLabelColor     = RamiColors.DarkGreen,
    labelColor             = RamiColors.TextLight
)

private fun diffEmoji(diff: AiDifficulty) = when (diff) {
    AiDifficulty.EASY   -> "🟢"
    AiDifficulty.MEDIUM -> "🟡"
    AiDifficulty.HARD   -> "🔴"
}

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
