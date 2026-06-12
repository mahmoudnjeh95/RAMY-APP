package com.rami.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rami.ai.FormationFinder
import com.rami.model.*
import com.rami.ui.theme.RamiColors
import com.rami.validator.FormationValidator

/**
 * ModalBottomSheet that lets the player organise their selected cards into one or
 * more formations before confirming a Nazoul lay-down.
 *
 * UX flow:
 *  1. Hand cards shown at bottom — tap to select.
 *  2. "Assign to Group" buttons map selected cards into Formation groups.
 *  3. Each group shows its validity status and point value in real time.
 *  4. Running total vs. threshold shown prominently.
 *  5. "Confirm Nazoul" enabled only when all groups valid AND total ≥ threshold.
 *
 * @param hand             Current player's full hand
 * @param mode             Game mode (affects threshold, point values)
 * @param lastNazoulValue  For Tafdhil: the previous player's Nazoul value
 * @param onConfirm        Called with the list-of-lists to pass to [GameEngine.layDown]
 * @param onDismiss        Sheet dismissed without action
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FormationBuilderSheet(
    hand: List<Card>,
    mode: GameMode,
    lastNazoulValue: Int,
    onConfirm: (List<List<Card>>) -> Unit,
    onDismiss: () -> Unit
) {
    // ── State ─────────────────────────────────────────────────────────────────
    // List of formation groups; each group is a list of card IDs
    var groups by remember { mutableStateOf(listOf(mutableListOf<String>())) }
    // Cards selected in hand (not yet assigned to any group)
    var selectedInHand by remember { mutableStateOf(setOf<String>()) }
    // Index of the active group to receive the next assignment
    var activeGroupIdx by remember { mutableStateOf(0) }

    // Cards already assigned to any group
    val assignedIds: Set<String> by derivedStateOf { groups.flatten().toSet() }

    val threshold = FormationFinder.nazoulThreshold(mode, lastNazoulValue)

    // Resolve card objects for each group
    fun groupCards(idx: Int): List<Card> =
        groups[idx].mapNotNull { id -> hand.firstOrNull { it.id == id } }

    // Running total
    val totalValue by derivedStateOf {
        groups.indices.sumOf { idx -> groupCards(idx).sumOf { it.pointValue(mode) } }
    }

    // All groups must be individually valid
    val allGroupsValid by derivedStateOf {
        groups.all { g ->
            g.isEmpty() || FormationValidator.isValid(
                g.mapNotNull { id -> hand.firstOrNull { it.id == id } }, mode
            )
        }
    }
    val hasNonEmptyGroup by derivedStateOf { groups.any { it.isNotEmpty() } }
    val canConfirm = hasNonEmptyGroup && allGroupsValid && totalValue >= threshold

    // ── AI suggestion ─────────────────────────────────────────────────────────
    LaunchedEffect(Unit) {
        val suggestion = FormationFinder.findNazoulCombination(hand, mode, lastNazoulValue)
        if (suggestion != null) {
            // Pre-fill groups with AI suggestion so player just reviews & confirms
            groups = suggestion.map { it.map { c -> c.id }.toMutableList() }
        }
    }

    // ── Sheet ──────────────────────────────────────────────────────────────────
    ModalBottomSheet(
        onDismissRequest  = onDismiss,
        containerColor    = RamiColors.DarkGreen,
        dragHandle        = {
            Box(
                Modifier
                    .padding(top = 8.dp, bottom = 4.dp)
                    .size(40.dp, 4.dp)
                    .background(RamiColors.Gold.copy(alpha = 0.5f), CircleShape)
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // ── Header ────────────────────────────────────────────────────────
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "بناء النزول",
                    color = RamiColors.Gold, fontSize = 20.sp, fontWeight = FontWeight.Bold
                )
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        "$totalValue / $threshold نقطة",
                        color    = if (totalValue >= threshold) Color(0xFF4CAF50) else RamiColors.CardRed,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        if (totalValue >= threshold) "✓ يكفي" else "لا يكفي",
                        color    = if (totalValue >= threshold) Color(0xFF4CAF50) else RamiColors.CardRed,
                        fontSize = 11.sp
                    )
                }
            }

            HorizontalDivider(color = RamiColors.Gold.copy(alpha = 0.25f))

            // ── Formation groups ──────────────────────────────────────────────
            groups.forEachIndexed { idx, group ->
                val cards     = groupCards(idx)
                val isValid   = group.isEmpty() || FormationValidator.isValid(cards, mode)
                val groupType = FormationValidator.detectType(cards)
                val groupPts  = cards.sumOf { it.pointValue(mode) }
                val isActive  = idx == activeGroupIdx

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            when {
                                isActive && isValid -> RamiColors.Gold.copy(alpha = 0.12f)
                                isActive           -> RamiColors.CardRed.copy(alpha = 0.12f)
                                else               -> RamiColors.TableSurface.copy(alpha = 0.4f)
                            }
                        )
                        .border(
                            width = if (isActive) 2.dp else 1.dp,
                            color = if (isActive) (if (isValid) RamiColors.Gold else RamiColors.CardRed)
                                    else Color.Gray.copy(alpha = 0.3f),
                            shape = RoundedCornerShape(12.dp)
                        )
                        .validationShimmer(isValid = isValid, nonEmpty = group.isNotEmpty())
                        .clickable { activeGroupIdx = idx }
                        .padding(10.dp)
                ) {
                    // Group header
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment     = Alignment.CenterVertically
                    ) {
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                "تشكيلة ${idx + 1}",
                                color = if (isActive) RamiColors.Gold else RamiColors.TextLight.copy(alpha = 0.8f),
                                fontSize = 13.sp, fontWeight = FontWeight.SemiBold
                            )
                            if (groupType != null) {
                                Text(
                                    groupType.arabicName,
                                    color    = RamiColors.TextLight.copy(alpha = 0.6f),
                                    fontSize = 11.sp
                                )
                            }
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                "$groupPts pts",
                                color    = if (isValid && group.isNotEmpty()) Color(0xFF4CAF50) else RamiColors.TextLight.copy(alpha = 0.5f),
                                fontSize = 12.sp, fontWeight = FontWeight.Bold
                            )
                            if (group.isNotEmpty()) {
                                Text(if (isValid) "✓" else "✗",
                                    color = if (isValid) Color(0xFF4CAF50) else RamiColors.CardRed,
                                    fontSize = 14.sp)
                            }
                        }
                    }

                    Spacer(Modifier.height(6.dp))

                    // Cards in group
                    if (group.isEmpty()) {
                        Text(
                            "اضغط على ورقة في يدك ثم اضغط هنا",
                            color    = RamiColors.TextLight.copy(alpha = 0.35f),
                            fontSize = 12.sp,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                    } else {
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            items(cards, key = { it.id }) { card ->
                                Box(
                                    modifier = Modifier.clickable {
                                        // Tap card in group to remove it back to hand
                                        groups = groups.toMutableList().also {
                                            it[idx] = it[idx].toMutableList().also { g -> g.remove(card.id) }
                                        }
                                    }
                                ) {
                                    CardView(card = card, small = true)
                                }
                            }
                        }
                    }
                }
            }

            // ── Add group button ──────────────────────────────────────────────
            if (groups.size < 4) {
                OutlinedButton(
                    onClick = {
                        groups = groups + mutableListOf()
                        activeGroupIdx = groups.lastIndex
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape    = RoundedCornerShape(10.dp),
                    colors   = ButtonDefaults.outlinedButtonColors(contentColor = RamiColors.Gold)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, tint = RamiColors.Gold)
                    Spacer(Modifier.width(6.dp))
                    Text("تشكيلة جديدة — New Formation")
                }
            }

            HorizontalDivider(color = RamiColors.Gold.copy(alpha = 0.2f))

            // ── Hand picker ────────────────────────────────────────────────────
            Text(
                "يدك — Your Hand  (اضغط لإضافة للتشكيلة ${activeGroupIdx + 1})",
                color = RamiColors.TextLight.copy(alpha = 0.7f), fontSize = 12.sp
            )
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                contentPadding        = PaddingValues(horizontal = 4.dp)
            ) {
                items(hand, key = { it.id }) { card ->
                    val isAssigned = card.id in assignedIds
                    val isSel      = card.id in selectedInHand
                    Box(
                        modifier = Modifier
                            .alpha(if (isAssigned) 0.35f else 1f)
                            .clickable(enabled = !isAssigned) {
                                // Assign card directly to active group
                                val newGroups = groups.toMutableList()
                                newGroups[activeGroupIdx] = (newGroups[activeGroupIdx] + card.id).toMutableList()
                                groups = newGroups
                            }
                    ) {
                        CardView(
                            card     = card,
                            selected = isSel,
                            small    = false
                        )
                    }
                }
            }

            Spacer(Modifier.height(8.dp))

            // ── Confirm button ────────────────────────────────────────────────
            Button(
                onClick  = {
                    val result = groups
                        .filter { it.isNotEmpty() }
                        .map { g -> g.mapNotNull { id -> hand.firstOrNull { it.id == id } } }
                    onConfirm(result)
                },
                enabled  = canConfirm,
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape    = RoundedCornerShape(14.dp),
                colors   = ButtonDefaults.buttonColors(containerColor = RamiColors.Gold)
            ) {
                Text(
                    "✓  تأكيد النزول — Confirm Nazoul ($totalValue pts)",
                    fontSize   = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color      = RamiColors.DarkGreen
                )
            }
        }
    }
}
