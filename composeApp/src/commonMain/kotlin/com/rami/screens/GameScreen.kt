package com.rami.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.rami.engine.GameEngine
import com.rami.model.*
import com.rami.ui.components.*
import com.rami.ui.theme.RamiColors
import com.rami.ui.theme.RamiTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// ─── Main composable ──────────────────────────────────────────────────────────

@Composable
fun GameScreen(
    engine: GameEngine,
    onRoundEnd: () -> Unit,
    onGameOver: () -> Unit
) {
    val state        by engine.state.collectAsState()
    val scope        = rememberCoroutineScope()
    val snackbarHost = remember { SnackbarHostState() }

    var selectedIds          by remember { mutableStateOf(setOf<String>()) }
    var showFormationBuilder by remember { mutableStateOf(false) }
    var jokerStealTarget     by remember { mutableStateOf<Formation?>(null) }

    // "نزل!" banner
    var nazoulBannerText  by remember { mutableStateOf("") }
    var showNazoulBanner  by remember { mutableStateOf(false) }

    val prevLaidCount = remember { mutableStateOf(0) }
    LaunchedEffect(state.players.count { it.hasLaidDown }) {
        val nowLaid = state.players.count { it.hasLaidDown }
        if (nowLaid > prevLaidCount.value) {
            val who = state.players.find { it.hasLaidDown && !state.players.take(prevLaidCount.value).any { p -> p.id == it.id && p.hasLaidDown } }
                ?: state.players.firstOrNull { it.hasLaidDown }
            nazoulBannerText = "نزل ${who?.name ?: ""} !"
            showNazoulBanner = true
            delay(2200)
            showNazoulBanner = false
        }
        prevLaidCount.value = nowLaid
    }

    // AI turn
    LaunchedEffect(state.currentPlayerIndex, state.turnPhase) {
        if (state.currentPlayer.isAI && state.gamePhase == GamePhase.IN_ROUND) {
            scope.launch { engine.processAiTurn() }
        }
    }

    // Navigation
    LaunchedEffect(state.gamePhase) {
        when (state.gamePhase) {
            GamePhase.ROUND_END -> onRoundEnd()
            GamePhase.GAME_OVER -> onGameOver()
            else                -> {}
        }
    }

    LaunchedEffect(state.message) {
        if (state.message.isNotEmpty()) snackbarHost.showSnackbar(state.message)
    }

    val localPlayer  = state.players.firstOrNull() ?: return
    val opponents    = state.players.drop(1)
    val isMyTurn     = state.currentPlayer.id == localPlayer.id
    val isDrawPhase  = isMyTurn && state.turnPhase == TurnPhase.DRAW
    val isActPhase   = isMyTurn && state.turnPhase == TurnPhase.ACTION
    val selectedCards = localPlayer.hand.filter { it.id in selectedIds }

    // Per-player formations
    val myFormations  = state.tableFormations.filter { it.ownerId == localPlayer.id }
    val oppFormations = state.tableFormations.groupBy { it.ownerId }

    RamiTheme {
        Scaffold(
            snackbarHost   = { SnackbarHost(snackbarHost) },
            containerColor = RamiColors.FeltGreen
        ) { padding ->

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .background(
                        Brush.verticalGradient(
                            listOf(Color(0xFF0D2E1A), RamiColors.FeltGreen, Color(0xFF1B4332))
                        )
                    )
            ) {
                Column(modifier = Modifier.fillMaxSize()) {

                    // ══════════════════════════════════════════════════════════
                    // TOP: Opponents
                    // ══════════════════════════════════════════════════════════
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(0.30f)
                            .background(RamiColors.DarkGreen.copy(alpha = 0.7f))
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        opponents.forEach { opp ->
                            OpponentPanel(
                                player      = opp,
                                isCurrent   = opp.id == state.currentPlayer.id,
                                formations  = oppFormations[opp.id] ?: emptyList(),
                                mode        = state.mode,
                                modifier    = Modifier.weight(1f)
                            )
                        }
                    }

                    // ══════════════════════════════════════════════════════════
                    // MIDDLE: Table center
                    // ══════════════════════════════════════════════════════════
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(0.32f)
                            .padding(horizontal = 12.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment     = Alignment.CenterVertically
                    ) {
                        // Deck pile
                        DeckPile(
                            deckSize = state.deckSize,
                            enabled  = isDrawPhase,
                            onClick  = { engine.drawFromDeck() },
                            modifier = Modifier.width(56.dp)
                        )

                        // Discard fan
                        DiscardFan(
                            discardPile = state.discardPile,
                            enabled     = isDrawPhase,
                            onClick     = { engine.drawFromDiscard() },
                            modifier    = Modifier.weight(1f)
                        )

                        // Action buttons
                        ActionColumn(
                            isActPhase    = isActPhase,
                            hasLaidDown   = localPlayer.hasLaidDown,
                            selectedCards = selectedCards,
                            onOpenBuilder = { showFormationBuilder = true },
                            onDiscard = {
                                selectedCards.firstOrNull()?.let { card ->
                                    engine.discard(card)
                                    selectedIds = emptySet()
                                }
                            },
                            modifier = Modifier.width(110.dp)
                        )

                        // Turn phase + score summary
                        TurnInfoPanel(
                            state       = state,
                            localPlayer = localPlayer,
                            modifier    = Modifier.width(80.dp)
                        )
                    }

                    // ══════════════════════════════════════════════════════════
                    // BOTTOM: Local player zone
                    // ══════════════════════════════════════════════════════════
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(0.38f)
                            .background(RamiColors.DarkGreen.copy(alpha = 0.85f))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        // My formations row
                        if (myFormations.isNotEmpty()) {
                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(80.dp)
                            ) {
                                itemsIndexed(myFormations, key = { _, f -> f.id }) { _, f ->
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(8.dp))
                                            .border(
                                                1.dp,
                                                if (isActPhase && selectedCards.size == 1)
                                                    RamiColors.Gold.copy(0.7f)
                                                else
                                                    RamiColors.Gold.copy(0.2f),
                                                RoundedCornerShape(8.dp)
                                            )
                                            .clickable {
                                                if (isActPhase && localPlayer.hasLaidDown && selectedCards.size == 1) {
                                                    val card = selectedCards.first()
                                                    engine.addCardToFormation(card, f.id)
                                                    selectedIds = emptySet()
                                                } else if (isActPhase && localPlayer.hasLaidDown && f.hasStealableJoker()) {
                                                    jokerStealTarget = f
                                                }
                                            }
                                    ) {
                                        FormationView(formation = f, mode = state.mode)
                                    }
                                }
                            }
                        }

                        // Player info bar
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 4.dp, vertical = 1.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment     = Alignment.CenterVertically
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalAlignment     = Alignment.CenterVertically
                            ) {
                                // Glow dot when it's your turn
                                if (isMyTurn) {
                                    val pulse = rememberInfiniteTransition(label = "dot")
                                    val a by pulse.animateFloat(
                                        0.4f, 1f,
                                        infiniteRepeatable(tween(700), RepeatMode.Reverse),
                                        label = "dp"
                                    )
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .background(Color(0xFF4CAF50).copy(alpha = a), CircleShape)
                                    )
                                }
                                Text(
                                    "👤 ${localPlayer.name}",
                                    color      = if (isMyTurn) RamiColors.Gold else RamiColors.TextLight,
                                    fontSize   = 13.sp,
                                    fontWeight = if (isMyTurn) FontWeight.Bold else FontWeight.Normal
                                )
                                if (localPlayer.hasLaidDown)
                                    Text("نزل ✓", color = RamiColors.LightGold, fontSize = 10.sp)
                            }
                            Text(
                                "${localPlayer.score} pts  •  ${localPlayer.handSize} 🃏",
                                color    = RamiColors.TextLight.copy(0.65f),
                                fontSize = 11.sp
                            )
                        }

                        // All cards fan — no scrolling
                        val sortedHand = remember(localPlayer.hand) {
                            localPlayer.hand.sortedWith(
                                compareBy(
                                    { (it as? Card.Regular)?.suit?.ordinal ?: 99 },
                                    { (it as? Card.Regular)?.rank?.order ?: 99 }
                                )
                            )
                        }
                        LandscapeHandFan(
                            cards       = sortedHand,
                            selectedIds = if (isMyTurn) selectedIds else emptySet(),
                            onToggle    = { id ->
                                if (isMyTurn) {
                                    selectedIds = if (id in selectedIds) selectedIds - id else selectedIds + id
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f)
                        )

                        // Selection info
                        if (selectedIds.isNotEmpty()) {
                            val pts = localPlayer.hand.filter { it.id in selectedIds }
                                .sumOf { it.pointValue(state.mode) }
                            Text(
                                "محدد: ${selectedIds.size}  •  $pts نقطة",
                                color    = RamiColors.Gold.copy(0.8f),
                                fontSize = 10.sp,
                                modifier = Modifier.padding(start = 8.dp, bottom = 2.dp)
                            )
                        }
                    }
                }

                // ── "نزل!" banner overlay ──────────────────────────────────
                AnimatedVisibility(
                    visible = showNazoulBanner,
                    enter   = slideInVertically { -it } + fadeIn(),
                    exit    = slideOutVertically { -it } + fadeOut(),
                    modifier = Modifier.align(Alignment.TopCenter).zIndex(10f)
                ) {
                    Box(
                        modifier = Modifier
                            .padding(top = 12.dp)
                            .background(RamiColors.Gold, RoundedCornerShape(20.dp))
                            .padding(horizontal = 24.dp, vertical = 8.dp)
                    ) {
                        Text(
                            nazoulBannerText,
                            color      = RamiColors.DarkGreen,
                            fontSize   = 18.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                }

                // ── Joker steal hint ───────────────────────────────────────
                if (localPlayer.hasLaidDown && state.tableFormations.any { it.hasStealableJoker() }) {
                    Text(
                        "★ اضغط تشكيلة بها جوكر لتسرقه",
                        color    = RamiColors.JokerPurple.copy(0.75f),
                        fontSize = 10.sp,
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 4.dp)
                            .zIndex(5f)
                    )
                }
            }

            // ── Formation builder ──────────────────────────────────────────
            if (showFormationBuilder) {
                FormationBuilderSheet(
                    hand            = localPlayer.hand,
                    mode            = state.mode,
                    lastNazoulValue = state.lastNazoulValue,
                    onConfirm = { formations ->
                        val ok = engine.layDown(formations)
                        if (!ok) scope.launch {
                            snackbarHost.showSnackbar("❌ التشكيلة غير صحيحة")
                        }
                        showFormationBuilder = false
                        selectedIds = emptySet()
                    },
                    onDismiss = { showFormationBuilder = false }
                )
            }

            // ── Joker steal dialog ─────────────────────────────────────────
            jokerStealTarget?.let { f ->
                JokerStealDialog(
                    formation  = f,
                    playerHand = localPlayer.hand,
                    mode       = state.mode,
                    onConfirm  = { jokerIdx, replacement ->
                        val ok = engine.stealJoker(f.id, jokerIdx, replacement)
                        if (!ok) scope.launch { snackbarHost.showSnackbar("❌ لا يمكن سرقة هذا الجوكر") }
                        jokerStealTarget = null
                        selectedIds = emptySet()
                    },
                    onDismiss = { jokerStealTarget = null }
                )
            }
        }
    }
}

// ─── Opponent panel ───────────────────────────────────────────────────────────

@Composable
private fun OpponentPanel(
    player: Player,
    isCurrent: Boolean,
    formations: List<Formation>,
    mode: GameMode,
    modifier: Modifier = Modifier
) {
    val glowAlpha by if (isCurrent) {
        rememberInfiniteTransition(label = "glow").animateFloat(
            0.15f, 0.5f,
            infiniteRepeatable(tween(900), RepeatMode.Reverse),
            label = "g"
        )
    } else remember { mutableStateOf(0f) }

    val thinkAlpha = if (isCurrent && player.isAI) aiThinkingAlpha() else 1f

    Column(
        modifier = modifier
            .fillMaxHeight()
            .clip(RoundedCornerShape(10.dp))
            .background(
                if (isCurrent) RamiColors.Gold.copy(alpha = glowAlpha)
                else Color.Transparent
            )
            .border(
                width = if (isCurrent) 1.5.dp else 0.5.dp,
                color = if (isCurrent) RamiColors.Gold else RamiColors.TextLight.copy(0.15f),
                shape = RoundedCornerShape(10.dp)
            )
            .padding(6.dp)
            .alpha(thinkAlpha),
        verticalArrangement   = Arrangement.SpaceBetween,
        horizontalAlignment   = Alignment.CenterHorizontally
    ) {
        // Name + info row
        Row(
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment     = Alignment.CenterVertically
        ) {
            if (isCurrent) Text("▶", color = RamiColors.Gold, fontSize = 8.sp)
            Text(
                if (player.isAI) "🤖 ${player.name}" else "👤 ${player.name}",
                color      = if (isCurrent) RamiColors.Gold else RamiColors.TextLight,
                fontSize   = 11.sp,
                fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal,
                maxLines   = 1
            )
            Text(
                "${player.score}pts",
                color    = RamiColors.TextLight.copy(0.55f),
                fontSize = 9.sp
            )
            if (player.hasLaidDown) Text("✓", color = RamiColors.LightGold, fontSize = 9.sp)
        }

        // Card backs (show actual count)
        FaceDownHand(
            count    = player.handSize,
            modifier = Modifier.weight(1f).fillMaxWidth()
        )

        // Opponent formations (small)
        if (formations.isNotEmpty()) {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                itemsIndexed(formations, key = { _, f -> f.id }) { _, f ->
                    MiniFormationView(formation = f, mode = mode)
                }
            }
        }
    }
}

// ─── Face-down hand ───────────────────────────────────────────────────────────

@Composable
private fun FaceDownHand(count: Int, modifier: Modifier = Modifier) {
    BoxWithConstraints(modifier = modifier) {
        val cardW  = 22.dp
        val cardH  = 34.dp
        val visible = if (count > 1) (maxWidth - cardW) / (count - 1) else cardW
        val spacing = visible.coerceAtMost(cardW * 0.85f)

        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            repeat(count.coerceAtMost(14)) { i ->
                Box(
                    modifier = Modifier
                        .width(cardW)
                        .height(cardH)
                        .offset(x = spacing * i)
                        .zIndex(i.toFloat())
                        .clip(RoundedCornerShape(3.dp))
                        .background(
                            Brush.verticalGradient(
                                listOf(Color(0xFF1A3A5C), Color(0xFF0D2340))
                            )
                        )
                        .border(0.5.dp, Color.White.copy(0.2f), RoundedCornerShape(3.dp))
                )
            }
        }
    }
}

// ─── Discard fan (last 3 cards) ───────────────────────────────────────────────

@Composable
private fun DiscardFan(
    discardPile: List<Card>,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier          = modifier
            .fillMaxHeight()
            .clip(RoundedCornerShape(10.dp))
            .border(
                1.5.dp,
                if (enabled) RamiColors.Gold else RamiColors.Gold.copy(0.2f),
                RoundedCornerShape(10.dp)
            )
            .clickable(enabled = enabled, onClick = onClick)
            .padding(6.dp),
        contentAlignment  = Alignment.Center
    ) {
        if (discardPile.isEmpty()) {
            Text("المرمى\nفارغ", color = RamiColors.TextLight.copy(0.3f),
                fontSize = 11.sp, textAlign = TextAlign.Center)
        } else {
            val last3 = discardPile.takeLast(3)
            Box {
                last3.forEachIndexed { i, card ->
                    val offset = ((i - last3.lastIndex) * 5).dp
                    CardView(
                        card     = card,
                        modifier = Modifier
                            .offset(x = offset * i, y = offset)
                            .zIndex(i.toFloat())
                            .graphicsLayer { rotationZ = (i - last3.lastIndex) * 4f }
                    )
                }
            }
        }
        if (enabled) {
            Text(
                "اسحب من المرمى",
                color    = RamiColors.Gold.copy(0.8f),
                fontSize = 9.sp,
                modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 2.dp)
            )
        }
    }
}

// ─── Deck pile ────────────────────────────────────────────────────────────────

@Composable
private fun DeckPile(
    deckSize: Int,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier            = modifier.fillMaxHeight(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(50.dp, 72.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(
                    if (enabled) RamiColors.Gold.copy(0.2f) else Color.Gray.copy(0.1f)
                )
                .border(
                    1.5.dp,
                    if (enabled) RamiColors.Gold else Color.Gray.copy(0.3f),
                    RoundedCornerShape(8.dp)
                )
                .clickable(enabled = enabled, onClick = onClick),
            contentAlignment = Alignment.Center
        ) {
            Text("🂠", fontSize = 28.sp)
        }
        Text(
            "$deckSize 🃏",
            color    = if (enabled) RamiColors.TextLight else RamiColors.TextLight.copy(0.4f),
            fontSize = 10.sp
        )
        if (enabled) Text("اسحب", color = RamiColors.Gold, fontSize = 9.sp)
    }
}

// ─── Action column ────────────────────────────────────────────────────────────

@Composable
private fun ActionColumn(
    isActPhase: Boolean,
    hasLaidDown: Boolean,
    selectedCards: List<Card>,
    onOpenBuilder: () -> Unit,
    onDiscard: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier            = modifier.fillMaxHeight(),
        verticalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (isActPhase && !hasLaidDown) {
            Button(
                onClick = onOpenBuilder,
                shape   = RoundedCornerShape(12.dp),
                colors  = ButtonDefaults.buttonColors(containerColor = RamiColors.Gold),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("⬇ نزول", color = RamiColors.DarkGreen, fontWeight = FontWeight.Bold, fontSize = 13.sp)
            }
        }
        if (isActPhase && selectedCards.size == 1) {
            Button(
                onClick = onDiscard,
                shape   = RoundedCornerShape(12.dp),
                colors  = ButtonDefaults.buttonColors(containerColor = Color(0xFFE53935)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("→ رمي", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
            }
        }
        if (!isActPhase) {
            Text(
                "انتظر دورك",
                color    = RamiColors.TextLight.copy(0.35f),
                fontSize = 10.sp,
                textAlign = TextAlign.Center
            )
        }
    }
}

// ─── Turn info panel ──────────────────────────────────────────────────────────

@Composable
private fun TurnInfoPanel(
    state: GameState,
    localPlayer: Player,
    modifier: Modifier = Modifier
) {
    val isMyTurn = state.currentPlayer.id == localPlayer.id
    Column(
        modifier            = modifier.fillMaxHeight(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        val (phaseLabel, phaseColor) = when {
            !isMyTurn                               -> "انتظار" to Color.Gray
            state.turnPhase == TurnPhase.DRAW       -> "سحب"  to RamiColors.Gold
            state.turnPhase == TurnPhase.ACTION     -> "لعب"  to Color(0xFF4CAF50)
            else                                    -> "رمي"  to Color(0xFFFF7043)
        }
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .background(phaseColor.copy(0.85f))
                .padding(horizontal = 10.dp, vertical = 4.dp)
        ) {
            Text(phaseLabel, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.height(4.dp))
        Text("دور ${state.currentPlayer.name}", color = RamiColors.TextLight.copy(0.6f),
            fontSize = 9.sp, textAlign = TextAlign.Center, maxLines = 2)
        Text("جولة ${state.roundNumber}", color = RamiColors.TextLight.copy(0.4f), fontSize = 9.sp)
    }
}

// ─── Mini formation (small version for opponent panels) ───────────────────────

@Composable
private fun MiniFormationView(formation: Formation, mode: GameMode) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(4.dp))
            .background(RamiColors.FeltGreen.copy(0.6f))
            .border(0.5.dp, RamiColors.Gold.copy(0.3f), RoundedCornerShape(4.dp))
            .padding(2.dp),
        horizontalArrangement = Arrangement.spacedBy((-6).dp)
    ) {
        formation.cards.forEach { card ->
            CardView(
                card     = card,
                modifier = Modifier
                    .width(20.dp)
                    .height(30.dp)
                    .graphicsLayer { scaleX = 0.5f; scaleY = 0.5f }
            )
        }
    }
}

// ─── Landscape hand fan — all cards visible ────────────────────────────────────

@Composable
fun LandscapeHandFan(
    cards: List<Card>,
    selectedIds: Set<String>,
    onToggle: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    if (cards.isEmpty()) return

    BoxWithConstraints(modifier = modifier) {
        val count    = cards.size
        val cardW    = 42.dp
        val cardH    = (maxHeight * 0.92f).coerceAtMost(68.dp)
        // Distribute cards evenly across available width
        val spacing  = if (count > 1) ((maxWidth - cardW) / (count - 1)).coerceAtMost(cardW)
                       else cardW

        Box(modifier = Modifier.fillMaxSize()) {
            cards.forEachIndexed { idx, card ->
                val isSelected = card.id in selectedIds
                DealAnimatedCard(
                    card     = card,
                    selected = isSelected,
                    delayMs  = idx * 25,
                    onClick  = { onToggle(card.id) },
                    modifier = Modifier
                        .width(cardW)
                        .height(cardH)
                        .offset(
                            x = spacing * idx,
                            y = if (isSelected) (-10).dp else 0.dp
                        )
                        .zIndex(if (isSelected) 100f + idx else idx.toFloat())
                )
            }
        }
    }
}
