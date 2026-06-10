package com.rami.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
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
import com.rami.engine.GameEngine
import com.rami.model.*
import com.rami.ui.components.*
import com.rami.ui.theme.RamiColors
import com.rami.ui.theme.RamiTheme
import kotlinx.coroutines.launch

@Composable
fun GameScreen(
    engine: GameEngine,
    onRoundEnd: () -> Unit,
    onGameOver: () -> Unit
) {
    val state        by engine.state.collectAsState()
    val scope        = rememberCoroutineScope()
    val snackbarHost = remember { SnackbarHostState() }

    // ── UI state ───────────────────────────────────────────────────────────────
    var selectedIds          by remember { mutableStateOf(setOf<String>()) }
    var showFormationBuilder by remember { mutableStateOf(false) }
    // Formation that was tapped for Joker-steal (null = no steal dialog)
    var jokerStealTarget     by remember { mutableStateOf<Formation?>(null) }

    // ── AI turn automation ─────────────────────────────────────────────────────
    LaunchedEffect(state.currentPlayerIndex, state.turnPhase) {
        if (state.currentPlayer.isAI && state.gamePhase == GamePhase.IN_ROUND) {
            scope.launch { engine.processAiTurn() }
        }
    }

    // ── Navigation on phase change ─────────────────────────────────────────────
    LaunchedEffect(state.gamePhase) {
        when (state.gamePhase) {
            GamePhase.ROUND_END -> onRoundEnd()
            GamePhase.GAME_OVER -> onGameOver()
            else                -> {}
        }
    }

    // ── Snackbar for engine messages ───────────────────────────────────────────
    LaunchedEffect(state.message) {
        if (state.message.isNotEmpty()) snackbarHost.showSnackbar(state.message)
    }

    // ── Local player (always index 0) ──────────────────────────────────────────
    val localPlayer  = state.players.firstOrNull() ?: return
    val isMyTurn     = state.currentPlayer.id == localPlayer.id
    val isDrawPhase  = isMyTurn && state.turnPhase == TurnPhase.DRAW
    val isActPhase   = isMyTurn && state.turnPhase == TurnPhase.ACTION

    // Cards selected from local player's hand
    val selectedCards = localPlayer.hand.filter { it.id in selectedIds }

    RamiTheme {
        Scaffold(
            snackbarHost   = { SnackbarHost(snackbarHost) },
            containerColor = RamiColors.FeltGreen
        ) { padding ->

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
            ) {

                // ══════════════════════════════════════════════════════════════
                // 1. OPPONENT STRIP
                // ══════════════════════════════════════════════════════════════
                OpponentStrip(state = state, localPlayerId = localPlayer.id)

                // ══════════════════════════════════════════════════════════════
                // 2. TABLE AREA
                // ══════════════════════════════════════════════════════════════
                TableArea(
                    state           = state,
                    localPlayer     = localPlayer,
                    selectedCards   = selectedCards,
                    onFormationTap  = { formation ->
                        // If player has laid down and has 1 card selected → try to add
                        if (isActPhase && localPlayer.hasLaidDown && selectedIds.size == 1) {
                            val card = selectedCards.firstOrNull()
                            if (card != null) {
                                engine.addCardToFormation(card, formation.id)
                                selectedIds = emptySet()
                            }
                        }
                        // Else if formation has jokers → open steal dialog
                        else if (isActPhase && localPlayer.hasLaidDown && formation.hasStealableJoker()) {
                            jokerStealTarget = formation
                        }
                    },
                    modifier        = Modifier.weight(1f)
                )

                // ══════════════════════════════════════════════════════════════
                // 3. ACTION BAR (piles + action buttons)
                // ══════════════════════════════════════════════════════════════
                ActionBar(
                    state           = state,
                    isDrawPhase     = isDrawPhase,
                    isActPhase      = isActPhase,
                    localPlayer     = localPlayer,
                    selectedCards   = selectedCards,
                    onDrawDeck      = { engine.drawFromDeck() },
                    onDrawDiscard   = { engine.drawFromDiscard() },
                    onOpenBuilder   = { showFormationBuilder = true },
                    onDiscard = {
                        selectedCards.firstOrNull()?.let { card ->
                            engine.discard(card)
                            selectedIds = emptySet()
                        }
                    }
                )

                // ══════════════════════════════════════════════════════════════
                // 4. LOCAL PLAYER HAND
                // ══════════════════════════════════════════════════════════════
                PlayerHandArea(
                    player      = localPlayer,
                    mode        = state.mode,
                    selectedIds = selectedIds,
                    isMyTurn    = isMyTurn,
                    roundNumber = state.roundNumber,
                    onToggle    = { id ->
                        selectedIds = if (id in selectedIds) selectedIds - id else selectedIds + id
                    }
                )
            }

            // ══════════════════════════════════════════════════════════════════
            // 5. FORMATION BUILDER SHEET
            // ══════════════════════════════════════════════════════════════════
            if (showFormationBuilder) {
                FormationBuilderSheet(
                    hand            = localPlayer.hand,
                    mode            = state.mode,
                    lastNazoulValue = state.lastNazoulValue,
                    onConfirm = { formations ->
                        val success = engine.layDown(formations)
                        if (!success) {
                            scope.launch { snackbarHost.showSnackbar("❌ التشكيلة غير صحيحة — Invalid formation") }
                        }
                        showFormationBuilder = false
                        selectedIds = emptySet()
                    },
                    onDismiss = { showFormationBuilder = false }
                )
            }

            // ══════════════════════════════════════════════════════════════════
            // 6. JOKER STEAL DIALOG
            // ══════════════════════════════════════════════════════════════════
            jokerStealTarget?.let { formation ->
                JokerStealDialog(
                    formation   = formation,
                    playerHand  = localPlayer.hand,
                    mode        = state.mode,
                    onConfirm   = { jokerIdx, replacement ->
                        val ok = engine.stealJoker(formation.id, jokerIdx, replacement)
                        if (!ok) scope.launch { snackbarHost.showSnackbar("❌ لا يمكن سرقة هذا الجوكر") }
                        jokerStealTarget = null
                        selectedIds = emptySet()
                    },
                    onDismiss   = { jokerStealTarget = null }
                )
            }
        }
    }
}

// ════════════════════════════════════════════════════════════════════════════════
// SUB-COMPOSABLES
// ════════════════════════════════════════════════════════════════════════════════

@Composable
private fun OpponentStrip(state: GameState, localPlayerId: String) {
    val opponents = state.players.filter { it.id != localPlayerId }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(RamiColors.DarkGreen.copy(alpha = 0.9f))
            .padding(horizontal = 8.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        opponents.forEach { opp ->
            val isCurrent = opp.id == state.currentPlayer.id
            val isAI      = opp.isAI
            val thinkAlpha = if (isCurrent && isAI) aiThinkingAlpha() else 1f

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .alpha(thinkAlpha)
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (isCurrent) RamiColors.Gold.copy(alpha = 0.15f) else Color.Transparent)
                    .padding(horizontal = 10.dp, vertical = 4.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    if (isCurrent) Text("▶", color = RamiColors.Gold, fontSize = 9.sp)
                    Text(opp.name, color = RamiColors.Gold, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    if (isAI) Text("🤖", fontSize = 10.sp)
                }
                Text("🃏 ${opp.handSize}", color = RamiColors.TextLight, fontSize = 11.sp)
                Text("${opp.score} pts",   color = RamiColors.TextLight.copy(alpha = 0.6f), fontSize = 10.sp)
                if (opp.hasLaidDown) Text("نزل ✓", color = RamiColors.LightGold, fontSize = 9.sp)
                if (state.mode == GameMode.TAFDHIL && opp.jokerBankCount > 0) {
                    Text("★ ${opp.jokerBankCount}/4", color = RamiColors.JokerPurple, fontSize = 9.sp)
                }
            }
        }
    }
}

// ── Table area ────────────────────────────────────────────────────────────────

@Composable
private fun TableArea(
    state: GameState,
    localPlayer: Player,
    selectedCards: List<Card>,
    onFormationTap: (Formation) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(RamiColors.FeltGreen)
            .padding(8.dp)
    ) {
        if (state.tableFormations.isEmpty()) {
            Text(
                text      = "الطاولة فارغة\nNo formations yet",
                color     = RamiColors.TextLight.copy(alpha = 0.3f),
                fontSize  = 14.sp,
                textAlign = TextAlign.Center,
                modifier  = Modifier.align(Alignment.Center)
            )
        } else {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding        = PaddingValues(horizontal = 4.dp)
            ) {
                itemsIndexed(state.tableFormations, key = { _, f -> f.id }) { _, formation ->
                    // Highlight formations that are valid drop targets for current selection
                    val isDropTarget = localPlayer.hasLaidDown &&
                                       selectedCards.size == 1 &&
                                       selectedCards.isNotEmpty()
                    val hasJoker = formation.hasStealableJoker()

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .border(
                                width = if (isDropTarget || hasJoker) 2.dp else 0.dp,
                                color = when {
                                    isDropTarget -> RamiColors.Gold.copy(alpha = 0.6f)
                                    hasJoker     -> RamiColors.JokerPurple.copy(alpha = 0.6f)
                                    else         -> Color.Transparent
                                },
                                shape = RoundedCornerShape(10.dp)
                            )
                            .clickable { onFormationTap(formation) }
                    ) {
                        AnimatedVisibility(visible = true, enter = fadeIn(), exit = fadeOut()) {
                            FormationView(formation = formation, mode = state.mode)
                        }
                    }
                }
            }
        }

        // Turn phase badge
        TurnPhaseBadge(
            phase    = state.turnPhase,
            isMyTurn = state.currentPlayer.id == localPlayer.id,
            modifier = Modifier.align(Alignment.TopEnd)
        )

        // Joker steal hint
        if (localPlayer.hasLaidDown && state.tableFormations.any { it.hasStealableJoker() }) {
            Text(
                "★ اضغط على تشكيلة بجوكر لتسرقه",
                color    = RamiColors.JokerPurple.copy(alpha = 0.7f),
                fontSize = 10.sp,
                modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 4.dp)
            )
        }
    }
}

// ── Turn phase badge ──────────────────────────────────────────────────────────

@Composable
private fun TurnPhaseBadge(phase: TurnPhase, isMyTurn: Boolean, modifier: Modifier = Modifier) {
    val (label, color) = when {
        !isMyTurn             -> "انتظار" to Color.Gray
        phase == TurnPhase.DRAW    -> "سحب" to RamiColors.Gold
        phase == TurnPhase.ACTION  -> "لعب" to Color(0xFF4CAF50)
        else                       -> "رمي" to Color(0xFFFF7043)
    }
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(bottomStart = 10.dp))
            .background(color.copy(alpha = 0.85f))
            .padding(horizontal = 10.dp, vertical = 4.dp)
    ) {
        Text(label, color = RamiColors.DarkGreen, fontSize = 12.sp, fontWeight = FontWeight.Bold)
    }
}

// ── Action bar ────────────────────────────────────────────────────────────────

@Composable
private fun ActionBar(
    state: GameState,
    isDrawPhase: Boolean,
    isActPhase: Boolean,
    localPlayer: Player,
    selectedCards: List<Card>,
    onDrawDeck: () -> Unit,
    onDrawDiscard: () -> Unit,
    onOpenBuilder: () -> Unit,
    onDiscard: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(RamiColors.DarkGreen.copy(alpha = 0.95f))
            .padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment     = Alignment.CenterVertically
    ) {
        // Deck pile
        PileButton(
            label   = "سحب\n${state.deckSize}🂠",
            enabled = isDrawPhase,
            onClick = onDrawDeck
        )

        // Discard pile
        Box(
            modifier = Modifier
                .border(
                    width = if (isDrawPhase) 2.dp else 0.5.dp,
                    color = if (isDrawPhase) RamiColors.Gold else Color.Gray.copy(alpha = 0.3f),
                    shape = RoundedCornerShape(8.dp)
                )
                .clickable(enabled = isDrawPhase) { onDrawDiscard() }
        ) {
            if (state.discardTop != null) {
                CardView(card = state.discardTop!!)
            } else {
                Box(
                    Modifier.size(54.dp, 78.dp).clip(RoundedCornerShape(8.dp))
                        .background(Color.Gray.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text("—", color = Color.Gray)
                }
            }
        }

        Spacer(Modifier.weight(1f))

        // Action buttons column
        Column(verticalArrangement = Arrangement.spacedBy(6.dp), horizontalAlignment = Alignment.End) {
            // Nazoul button — opens formation builder
            if (isActPhase && !localPlayer.hasLaidDown) {
                Button(
                    onClick = onOpenBuilder,
                    shape   = RoundedCornerShape(10.dp),
                    colors  = ButtonDefaults.buttonColors(containerColor = RamiColors.Gold)
                ) {
                    Text("↓ نزول", color = RamiColors.DarkGreen, fontWeight = FontWeight.Bold)
                }
            }

            // Discard button — only when exactly one card selected in action phase
            if (isActPhase && selectedCards.size == 1) {
                Button(
                    onClick = onDiscard,
                    shape   = RoundedCornerShape(10.dp),
                    colors  = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF7043))
                ) {
                    Text("→ رمي", color = Color.White, fontWeight = FontWeight.Bold)
                }
            }

            // Context hint
            if (isActPhase && selectedCards.isEmpty() && localPlayer.hasLaidDown) {
                Text(
                    "اختار ورقة للرمي\أو اضغط تشكيلة",
                    color = RamiColors.TextLight.copy(alpha = 0.4f),
                    fontSize = 9.sp, textAlign = TextAlign.End
                )
            }
        }
    }
}

@Composable
private fun PileButton(label: String, enabled: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(54.dp, 78.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(if (enabled) RamiColors.Gold.copy(alpha = 0.2f) else Color.Gray.copy(alpha = 0.1f))
            .border(1.dp, if (enabled) RamiColors.Gold else Color.Gray.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(label, color = if (enabled) RamiColors.TextLight else RamiColors.TextLight.copy(0.4f),
            fontSize = 11.sp, textAlign = TextAlign.Center)
    }
}

// ── Player hand ───────────────────────────────────────────────────────────────

@Composable
private fun PlayerHandArea(
    player: Player,
    mode: GameMode,
    selectedIds: Set<String>,
    isMyTurn: Boolean,
    roundNumber: Int,
    onToggle: (String) -> Unit
) {
    val sortedHand = remember(player.hand) {
        player.hand.sortedWith(compareBy(
            { (it as? Card.Regular)?.suit?.ordinal ?: 99 },
            { (it as? Card.Regular)?.rank?.order ?: 99 }
        ))
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(RamiColors.DarkGreen)
            .padding(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 2.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("👤 ${player.name}", color = RamiColors.Gold, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                if (isMyTurn)          Text("◀ دورك",  color = Color(0xFF4CAF50), fontSize = 10.sp)
                if (player.hasLaidDown) Text("نزل ✓", color = RamiColors.LightGold, fontSize = 10.sp)
            }
            Text("${player.score} pts  •  ${player.handSize} 🃏",
                color = RamiColors.TextLight.copy(0.6f), fontSize = 12.sp)
        }

        Spacer(Modifier.height(4.dp))

        LazyRow(
            horizontalArrangement = Arrangement.spacedBy((-10).dp),
            contentPadding        = PaddingValues(horizontal = 8.dp)
        ) {
            itemsIndexed(sortedHand, key = { _, c -> c.id }) { idx, card ->
                val isSelected = card.id in selectedIds
                DealAnimatedCard(
                    card     = card,
                    selected = isSelected,
                    delayMs  = idx * 55,  // cascade deal animation
                    onClick  = { if (isMyTurn) onToggle(card.id) },
                    modifier = Modifier.selectionLift(isSelected)
                )
            }
        }

        // Selection summary
        if (selectedIds.isNotEmpty()) {
            val pts = player.hand.filter { it.id in selectedIds }.sumOf { it.pointValue(mode) }
            Spacer(Modifier.height(3.dp))
            Text(
                "محدد: ${selectedIds.size} ورقة  •  $pts نقطة",
                color    = RamiColors.Gold.copy(0.8f),
                fontSize = 11.sp,
                modifier = Modifier.padding(horizontal = 12.dp)
            )
        }
    }
}
