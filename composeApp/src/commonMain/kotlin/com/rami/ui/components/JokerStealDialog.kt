package com.rami.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rami.model.Card
import com.rami.model.Formation
import com.rami.model.GameMode
import com.rami.ui.theme.RamiColors
import com.rami.validator.FormationValidator

/**
 * Modal dialog for the Joker steal flow (GDD §5.2).
 *
 * Shows the target formation with Joker(s) highlighted and a picker for the
 * replacement card from the player's hand.
 *
 * @param formation   Formation on the table containing at least one Joker
 * @param playerHand  Current local player's hand
 * @param mode        Game mode (affects point display)
 * @param onConfirm   Called with (jokerIndex, replacement) when the player confirms
 * @param onDismiss   Called when the player cancels
 */
@Composable
fun JokerStealDialog(
    formation: Formation,
    playerHand: List<Card>,
    mode: GameMode,
    onConfirm: (jokerIndex: Int, replacement: Card.Regular) -> Unit,
    onDismiss: () -> Unit
) {
    // Which joker position the player has tapped (null = none)
    var selectedJokerIdx by remember { mutableStateOf<Int?>(null) }
    // Which hand card the player has chosen as replacement
    var selectedReplacement by remember { mutableStateOf<Card.Regular?>(null) }

    // Auto-select first Joker if there's only one
    val jokerPositions = formation.jokerPositions()
    LaunchedEffect(Unit) {
        if (jokerPositions.size == 1) selectedJokerIdx = jokerPositions.first().first
    }

    // Eligible replacements for the currently selected joker slot
    val eligibleReplacements: List<Card.Regular> = remember(selectedJokerIdx) {
        val idx = selectedJokerIdx ?: return@remember emptyList()
        playerHand.filterIsInstance<Card.Regular>().filter { card ->
            FormationValidator.canReplaceJoker(idx, card, formation, mode)
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor   = RamiColors.DarkGreen,
        titleContentColor = RamiColors.Gold,
        title = {
            Text(
                text       = "سرقة الجوكر  •  Steal Joker",
                fontSize   = 18.sp,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {

                // ── Formation preview ──────────────────────────────────────────
                Text("التشكيلة — Formation", color = RamiColors.TextLight.copy(alpha = 0.7f), fontSize = 12.sp)
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(RamiColors.TableSurface.copy(alpha = 0.6f))
                        .padding(8.dp)
                ) {
                    formation.cards.forEachIndexed { idx, card ->
                        val isJoker     = card.isJoker()
                        val isSelected  = isJoker && idx == selectedJokerIdx
                        val isJokerSlot = isJoker && jokerPositions.any { it.first == idx }
                        Box(
                            modifier = Modifier
                                .then(if (isJokerSlot) Modifier.clickable { selectedJokerIdx = idx } else Modifier)
                                .border(
                                    width = if (isSelected) 3.dp else 0.dp,
                                    color = if (isSelected) RamiColors.Gold else Color.Transparent,
                                    shape = RoundedCornerShape(6.dp)
                                )
                        ) {
                            CardView(card = card, small = true)
                        }
                    }
                }

                // ── Joker slot selector (if multiple jokers) ───────────────────
                if (jokerPositions.size > 1) {
                    Text("اختار الجوكر — Pick Joker to steal", color = RamiColors.TextLight.copy(alpha = 0.7f), fontSize = 12.sp)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        jokerPositions.forEach { (idx, _) ->
                            FilterChip(
                                selected = selectedJokerIdx == idx,
                                onClick  = { selectedJokerIdx = idx; selectedReplacement = null },
                                label    = { Text("موقع $idx") },
                                colors   = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = RamiColors.Gold,
                                    selectedLabelColor     = RamiColors.DarkGreen
                                )
                            )
                        }
                    }
                }

                // ── Replacement picker ─────────────────────────────────────────
                if (selectedJokerIdx != null) {
                    Text(
                        "اختار بديل من يدك — Choose replacement",
                        color = RamiColors.TextLight.copy(alpha = 0.7f),
                        fontSize = 12.sp
                    )
                    if (eligibleReplacements.isEmpty()) {
                        Text(
                            "لا يوجد بديل مناسب في يدك",
                            color    = RamiColors.CardRed.copy(alpha = 0.8f),
                            fontSize = 13.sp,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                    } else {
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            items(eligibleReplacements, key = { it.id }) { card ->
                                val picked = selectedReplacement?.id == card.id
                                Box(
                                    modifier = Modifier
                                        .border(
                                            width = if (picked) 2.dp else 0.dp,
                                            color = if (picked) RamiColors.Gold else Color.Transparent,
                                            shape = RoundedCornerShape(8.dp)
                                        )
                                        .clickable { selectedReplacement = card }
                                ) {
                                    CardView(card = card, selected = picked)
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            val canConfirm = selectedJokerIdx != null && selectedReplacement != null
            Button(
                onClick  = { onConfirm(selectedJokerIdx!!, selectedReplacement!!) },
                enabled  = canConfirm,
                colors   = ButtonDefaults.buttonColors(containerColor = RamiColors.Gold)
            ) {
                Text("سرقة — Steal", color = RamiColors.DarkGreen, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("إلغاء — Cancel", color = RamiColors.TextLight.copy(alpha = 0.6f))
            }
        }
    )
}
