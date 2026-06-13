package com.rami.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
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
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.compose.foundation.Image
import androidx.compose.ui.layout.ContentScale
import com.rami.engine.GameEngine
import com.rami.generated.resources.Res
import com.rami.generated.resources.table_bg
import com.rami.model.*
import com.rami.ui.components.*
import com.rami.ui.theme.RamiColors
import com.rami.ui.theme.RamiTheme
import org.jetbrains.compose.resources.painterResource
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// ─── Drag state ───────────────────────────────────────────────────────────────

private data class DragState(
    val cards: List<Card>,
    val currentOffset: Offset
)

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

    // Selection / overlay state
    var selectedIds          by remember { mutableStateOf(setOf<String>()) }
    var showFormationBuilder by remember { mutableStateOf(false) }
    var jokerStealTarget     by remember { mutableStateOf<Formation?>(null) }

    // "نزل!" banner
    var nazoulBannerText by remember { mutableStateOf("") }
    var showNazoulBanner by remember { mutableStateOf(false) }

    // Drag & drop
    var dragState          by remember { mutableStateOf<DragState?>(null) }
    var nazoulDropZoneRect by remember { mutableStateOf<Rect?>(null) }

    // Turn timer (seconds remaining)
    var timerSeconds by remember { mutableStateOf(30) }

    // Card flip — track newest drawn card
    var newestCardId by remember { mutableStateOf<String?>(null) }

    // Particle effects
    var showConfetti    by remember { mutableStateOf(false) }
    var showJokerSparks by remember { mutableStateOf(false) }

    // Turn ripple trigger — increments each time the active player changes
    var turnRippleTick by remember { mutableStateOf(0) }

    // Discard fly animation — card that just landed on discard pile
    var discardFlyCard       by remember { mutableStateOf<Card?>(null) }
    val prevDiscardId        = remember { mutableStateOf<String?>(null) }

    // ── Effects ───────────────────────────────────────────────────────────────

    // "نزل!" banner + confetti
    val prevLaidCount = remember { mutableStateOf(0) }
    LaunchedEffect(state.players.count { it.hasLaidDown }) {
        val nowLaid = state.players.count { it.hasLaidDown }
        if (nowLaid > prevLaidCount.value) {
            val who = state.players.firstOrNull { it.hasLaidDown }
            nazoulBannerText = "نزل ${who?.name ?: ""} !"
            showNazoulBanner = true
            showConfetti     = true
            delay(2200)
            showNazoulBanner = false
            showConfetti     = false
        }
        prevLaidCount.value = nowLaid
    }

    // Turn ripple — fires each time the active player index changes
    LaunchedEffect(state.currentPlayerIndex) {
        turnRippleTick++
    }

    // Discard fly — detect new top of discard pile
    LaunchedEffect(state.discardPile.size) {
        val newTop = state.discardPile.lastOrNull() ?: return@LaunchedEffect
        if (newTop.id != prevDiscardId.value) {
            prevDiscardId.value = newTop.id
            discardFlyCard = newTop
            delay(480)
            discardFlyCard = null
        }
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

    // Snackbar messages
    LaunchedEffect(state.message) {
        if (state.message.isNotEmpty()) snackbarHost.showSnackbar(state.message)
    }

    // Turn timer — resets when current player changes
    LaunchedEffect(state.currentPlayerIndex, state.gamePhase) {
        timerSeconds = 30
        if (state.gamePhase != GamePhase.IN_ROUND) return@LaunchedEffect
        for (i in 30 downTo 1) {
            delay(1000L)
            timerSeconds = i - 1
        }
    }

    // ── Guard against empty state ─────────────────────────────────────────────

    val localPlayer  = state.players.firstOrNull() ?: return
    val opponents    = state.players.drop(1)
    val isMyTurn     = state.currentPlayer.id == localPlayer.id
    val isDrawPhase  = isMyTurn && state.turnPhase == TurnPhase.DRAW
    val isActPhase   = isMyTurn && state.turnPhase == TurnPhase.ACTION
    val selectedCards = localPlayer.hand.filter { it.id in selectedIds }

    val myFormations  = state.tableFormations.filter { it.ownerId == localPlayer.id }
    val oppFormations = state.tableFormations.groupBy { it.ownerId }

    val isDragging = dragState != null
    val isDraggingOverZone = isDragging &&
        nazoulDropZoneRect?.contains(dragState!!.currentOffset) == true

    // Detect newest drawn card for flip animation
    val localHandSize = localPlayer.hand.size
    val prevHandSize  = remember { mutableStateOf(localHandSize) }
    LaunchedEffect(localHandSize) {
        if (localHandSize > prevHandSize.value) {
            newestCardId = localPlayer.hand.lastOrNull()?.id
            delay(550)
            newestCardId = null
        }
        prevHandSize.value = localHandSize
    }

    // ── UI ────────────────────────────────────────────────────────────────────

    RamiTheme {
        Scaffold(
            snackbarHost   = { SnackbarHost(snackbarHost) },
            containerColor = RamiColors.FeltGreen
        ) { padding ->

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
            ) {
                // Photorealistic table background
                Image(
                    painter      = painterResource(Res.drawable.table_bg),
                    contentDescription = null,
                    modifier     = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
                // Darkening overlay for UI readability
                Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(0.22f)))
                // Ambient felt texture overlay
                TableTexture(modifier = Modifier.fillMaxSize())
                Column(modifier = Modifier.fillMaxSize()) {

                    // ══════════════════════════════════════════════════════════
                    // TOP: Opponents
                    // ══════════════════════════════════════════════════════════
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(0.22f)
                            .background(RamiColors.DarkGreen.copy(alpha = 0.7f))
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        opponents.forEach { opp ->
                            val isOppCurrent = opp.id == state.currentPlayer.id
                            OpponentPanel(
                                player     = opp,
                                isCurrent  = isOppCurrent,
                                formations = oppFormations[opp.id] ?: emptyList(),
                                mode       = state.mode,
                                timerProg  = if (isOppCurrent) timerSeconds / 30f else 1f,
                                modifier   = Modifier.weight(1f)
                            )
                        }
                    }

                    // ══════════════════════════════════════════════════════════
                    // MIDDLE: Table center
                    // ══════════════════════════════════════════════════════════
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(0.28f)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 8.dp, vertical = 4.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment     = Alignment.CenterVertically
                        ) {
                            // Deck + Discard side by side — NazoulDropZone overlays them during drag
                            Box(modifier = Modifier.weight(2f).fillMaxHeight()) {
                                Row(
                                    modifier = Modifier.fillMaxSize(),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    DeckPile(
                                        deckSize = state.deckSize,
                                        enabled  = isDrawPhase,
                                        onClick  = { engine.drawFromDeck() },
                                        modifier = Modifier.weight(1f).fillMaxHeight()
                                    )
                                    DiscardFan(
                                        discardPile = state.discardPile,
                                        enabled     = isDrawPhase,
                                        onClick     = { engine.drawFromDiscard() },
                                        modifier    = Modifier.weight(1f).fillMaxHeight()
                                    )
                                }
                                // Drop zone overlays the entire deck+discard area during drag
                                NazoulDropZone(
                                    visible      = isDragging && isActPhase,
                                    isOver       = isDraggingOverZone,
                                    onRectUpdate = { nazoulDropZoneRect = it },
                                    modifier     = Modifier.fillMaxSize()
                                )
                            }
                            // Right: turn info + action buttons
                            Column(
                                modifier = Modifier.weight(1.2f).fillMaxHeight(),
                                verticalArrangement   = Arrangement.spacedBy(4.dp, Alignment.CenterVertically),
                                horizontalAlignment   = Alignment.CenterHorizontally
                            ) {
                                TurnInfoPanel(
                                    state        = state,
                                    localPlayer  = localPlayer,
                                    timerSeconds = if (isMyTurn) timerSeconds else 30
                                )
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
                                    }
                                )
                            }
                        }
                        // Discard fly overlay
                        discardFlyCard?.let { flyCard ->
                            DiscardFlyOverlay(card = flyCard, modifier = Modifier.fillMaxSize())
                        }
                    } // end MIDDLE Box

                    // ══════════════════════════════════════════════════════════
                    // BOTTOM: Local player zone
                    // ══════════════════════════════════════════════════════════
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(0.50f)
                            .background(RamiColors.DarkGreen.copy(alpha = 0.85f))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        // My formations — horizontally scrollable
                        if (myFormations.isNotEmpty()) {
                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(62.dp)
                            ) {
                                itemsIndexed(myFormations, key = { _, f -> f.id }) { _, f ->
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(8.dp))
                                            .border(
                                                1.dp,
                                                if (isActPhase && selectedCards.size == 1)
                                                    RamiColors.Gold.copy(0.7f)
                                                else RamiColors.Gold.copy(0.2f),
                                                RoundedCornerShape(8.dp)
                                            )
                                            .clickable {
                                                if (isActPhase && localPlayer.hasLaidDown && selectedCards.size == 1) {
                                                    engine.addCardToFormation(selectedCards.first(), f.id)
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

                        // Player info bar with animated score
                        val animMyScore by animateIntAsState(
                            targetValue   = localPlayer.score,
                            animationSpec = tween(600),
                            label         = "my_score"
                        )
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
                                if (isMyTurn) {
                                    val pulse = rememberInfiniteTransition(label = "dot")
                                    val a by pulse.animateFloat(
                                        0.4f, 1f,
                                        infiniteRepeatable(tween(700), RepeatMode.Reverse),
                                        label = "dp"
                                    )
                                    Box(
                                        Modifier
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
                            Box {
                                Text(
                                    "$animMyScore pts  •  ${localPlayer.handSize} 🃏",
                                    color    = RamiColors.TextLight.copy(0.65f),
                                    fontSize = 11.sp
                                )
                                ScoreDelta(
                                    score    = localPlayer.score,
                                    modifier = Modifier.align(Alignment.TopCenter)
                                )
                            }
                        }

                        // Hand fan — all cards visible, drag & drop + flip animation
                        val sortedHand = remember(localPlayer.hand) {
                            localPlayer.hand.sortedWith(
                                compareBy(
                                    { (it as? Card.Regular)?.suit?.ordinal ?: 99 },
                                    { (it as? Card.Regular)?.rank?.order ?: 99 }
                                )
                            )
                        }
                        LandscapeHandFan(
                            cards        = sortedHand,
                            selectedIds  = if (isMyTurn) selectedIds else emptySet(),
                            onToggle     = { id ->
                                if (isMyTurn) {
                                    selectedIds = if (id in selectedIds) selectedIds - id else selectedIds + id
                                }
                            },
                            newestCardId = newestCardId,
                            onDragStart  = if (isActPhase) { dragCards, startOffset ->
                                dragState = DragState(dragCards, startOffset)
                            } else null,
                            onDragMove   = { delta ->
                                dragState = dragState?.let { it.copy(currentOffset = it.currentOffset + delta) }
                            },
                            onDragEnd    = {
                                val finalOffset = dragState?.currentOffset ?: Offset.Zero
                                val droppedOnZone = nazoulDropZoneRect?.contains(finalOffset) == true
                                dragState = null
                                if (droppedOnZone && isActPhase) showFormationBuilder = true
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f)
                        )

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
                            fontWeight = FontWeight.ExtraBold,
                            modifier   = Modifier.infiniteGoldShimmer()
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

                // ── Turn transition ripple ────────────────────────────────
                TurnRipple(
                    trigger  = turnRippleTick,
                    modifier = Modifier.fillMaxSize().zIndex(3f)
                )

                // ── Confetti burst on Nazoul ──────────────────────────────
                ConfettiBurst(
                    trigger  = showConfetti,
                    modifier = Modifier.fillMaxSize().zIndex(20f)
                )

                // ── Joker steal sparks ────────────────────────────────────
                SparkBurst(
                    trigger  = showJokerSparks,
                    modifier = Modifier.fillMaxSize().zIndex(20f)
                )

                // ── Floating drag card overlay ─────────────────────────────
                dragState?.let { ds ->
                    val topCard = ds.cards.firstOrNull() ?: return@let
                    Box(
                        modifier = Modifier
                            .offset {
                                IntOffset(
                                    (ds.currentOffset.x - 21.dp.roundToPx()).toInt(),
                                    (ds.currentOffset.y - 30.dp.roundToPx()).toInt()
                                )
                            }
                            .zIndex(999f)
                            .size(42.dp, 60.dp)
                            .graphicsLayer {
                                shadowElevation = 20.dp.toPx()
                                rotationZ = if (ds.cards.size > 1) -6f else 0f
                            }
                    ) {
                        CardView(card = topCard, modifier = Modifier.fillMaxSize())
                        if (ds.cards.size > 1) {
                            Box(
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .offset(x = 4.dp, y = (-4).dp)
                                    .size(16.dp)
                                    .background(RamiColors.Gold, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    "${ds.cards.size}",
                                    color      = RamiColors.DarkGreen,
                                    fontSize   = 9.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
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
                        else scope.launch { showJokerSparks = true; delay(800); showJokerSparks = false }
                        jokerStealTarget = null
                        selectedIds = emptySet()
                    },
                    onDismiss = { jokerStealTarget = null }
                )
            }
        }
    }
}

// ─── Discard fly overlay ─────────────────────────────────────────────────────

@Composable
private fun DiscardFlyOverlay(card: Card, modifier: Modifier = Modifier) {
    var ready by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        delay(30)
        ready = true
    }
    // Card slides from hand area (bottom = large Y) up to discard pile (Y = 0)
    val offsetY by animateDpAsState(
        targetValue   = if (ready) 0.dp else 180.dp,
        animationSpec = tween(420, easing = FastOutSlowInEasing),
        label         = "discardFlyY"
    )
    val alpha by animateFloatAsState(
        targetValue   = if (ready) 1f else 0f,
        animationSpec = tween(150),
        label         = "discardFlyA"
    )
    val rot by animateFloatAsState(
        targetValue   = if (ready) -8f else 0f,
        animationSpec = tween(420, easing = FastOutSlowInEasing),
        label         = "discardFlyR"
    )
    Box(
        modifier = modifier.zIndex(50f),
        contentAlignment = Alignment.Center
    ) {
        CardView(
            card     = card,
            modifier = Modifier
                .offset(y = offsetY)
                .alpha(alpha)
                .graphicsLayer { rotationZ = rot; shadowElevation = 24f }
        )
    }
}

// ─── Opponent panel ───────────────────────────────────────────────────────────

@Composable
private fun OpponentPanel(
    player: Player,
    isCurrent: Boolean,
    formations: List<Formation>,
    mode: GameMode,
    timerProg: Float = 1f,
    modifier: Modifier = Modifier
) {
    val glowAlpha by if (isCurrent) {
        rememberInfiniteTransition(label = "glow").animateFloat(
            0.1f, 0.4f,
            infiniteRepeatable(tween(1000, easing = EaseInOutSine), RepeatMode.Reverse),
            label = "g"
        )
    } else remember { mutableStateOf(0f) }

    val thinkAlpha = if (isCurrent && player.isAI) aiThinkingAlpha() else 1f

    val animScore by animateIntAsState(
        targetValue   = player.score,
        animationSpec = tween(600),
        label         = "score_${player.id}"
    )

    Surface(
        modifier = modifier
            .fillMaxHeight()
            .alpha(thinkAlpha),
        shape = RoundedCornerShape(12.dp),
        color = if (isCurrent) Color.White.copy(0.05f) else Color.Transparent,
        border = androidx.compose.foundation.BorderStroke(
            width = if (isCurrent) 1.5.dp else 1.dp,
            color = if (isCurrent) RamiColors.Gold.copy(0.8f) else Color.White.copy(0.1f)
        ),
        shadowElevation = if (isCurrent) 8.dp else 0.dp
    ) {
        Column(
            modifier = Modifier.padding(6.dp),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header: Name + Timer
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.size(24.dp)) {
                    if (isCurrent) {
                        TurnTimerArc(progress = timerProg, modifier = Modifier.fillMaxSize())
                        Text("⏳", fontSize = 10.sp)
                    } else {
                        Text(if (player.isAI) "🤖" else "👤", fontSize = 12.sp)
                    }
                }
                
                Text(
                    player.name,
                    color = if (isCurrent) Color.White else RamiColors.TextLight.copy(0.7f),
                    fontSize = 11.sp,
                    fontWeight = if (isCurrent) FontWeight.ExtraBold else FontWeight.Medium,
                    maxLines = 1,
                    modifier = Modifier.weight(1f).padding(horizontal = 4.dp),
                    textAlign = TextAlign.Start
                )

                if (player.hasLaidDown) {
                    Surface(
                        color = RamiColors.Gold.copy(0.2f),
                        shape = CircleShape,
                        modifier = Modifier.size(14.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text("✓", color = RamiColors.Gold, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // Cards visualization
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                contentAlignment = Alignment.Center
            ) {
                FaceDownHand(count = player.handSize)
                
                // Score Badge overlay
                Surface(
                    color = Color.Black.copy(0.4f),
                    shape = RoundedCornerShape(4.dp),
                    modifier = Modifier.align(Alignment.BottomEnd)
                ) {
                    Text(
                        "${animScore} pts",
                        color = RamiColors.Gold,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                    )
                }
            }

            // Formations row (mini)
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
}


// ─── Turn timer arc ───────────────────────────────────────────────────────────

@Composable
private fun TurnTimerArc(progress: Float, modifier: Modifier = Modifier) {
    val arcColor = when {
        progress > 0.5f  -> Color(0xFF4CAF50)
        progress > 0.25f -> Color(0xFFFFC107)
        else             -> Color(0xFFE53935)
    }
    Canvas(modifier = modifier) {
        drawArc(
            color      = Color.Gray.copy(0.3f),
            startAngle = -90f,
            sweepAngle = 360f,
            useCenter  = false,
            style      = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
        )
        if (progress > 0f) {
            drawArc(
                color      = arcColor,
                startAngle = -90f,
                sweepAngle = 360f * progress.coerceIn(0f, 1f),
                useCenter  = false,
                style      = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
            )
        }
    }
}

// ─── Face-down hand ───────────────────────────────────────────────────────────

@Composable
private fun FaceDownHand(count: Int, modifier: Modifier = Modifier) {
    BoxWithConstraints(modifier = modifier) {
        val cardW  = 22.dp
        val cardH  = 34.dp
        val spacing = if (count > 1)
            ((maxWidth - cardW) / (count - 1)).coerceAtMost(cardW * 0.85f)
        else cardW

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
                            Brush.verticalGradient(listOf(Color(0xFF1A3A5C), Color(0xFF0D2340)))
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
        modifier = modifier
            .fillMaxHeight()
            .clip(RoundedCornerShape(12.dp))
            .background(Color.Black.copy(0.15f))
            .border(
                1.5.dp,
                if (enabled) RamiColors.Gold else Color.White.copy(0.05f),
                RoundedCornerShape(12.dp)
            )
            .clickable(enabled = enabled, onClick = onClick)
            .padding(4.dp),
        contentAlignment = Alignment.Center
    ) {
        if (discardPile.isEmpty()) {
            Text(
                "المرمى فارغ",
                color = Color.White.copy(0.2f),
                fontSize = 11.sp,
                textAlign = TextAlign.Center,
                fontWeight = FontWeight.Bold
            )
        } else {
            // Visual stack representation
            val topCards = discardPile.takeLast(3)
            Box(contentAlignment = Alignment.Center) {
                topCards.forEachIndexed { i, card ->
                    val rot = (i - topCards.size / 2) * 5f
                    val offX = (i - topCards.size / 2) * 4.dp
                    CardView(
                        card = card,
                        small = true,
                        modifier = Modifier
                            .offset(x = offX)
                            .graphicsLayer { rotationZ = rot }
                    )
                }
            }
        }
        
        if (enabled) {
            Surface(
                color = RamiColors.Gold,
                shape = RoundedCornerShape(topStart = 12.dp, bottomEnd = 12.dp),
                modifier = Modifier.align(Alignment.BottomEnd)
            ) {
                Text(
                    "سحب",
                    color = Color.Black,
                    fontSize = 8.sp,
                    fontWeight = FontWeight.Black,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                )
            }
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
    val infiniteTransition = rememberInfiniteTransition(label = "deck")
    val glow by infiniteTransition.animateFloat(
        0.1f, 0.6f,
        infiniteRepeatable(tween(1200, easing = EaseInOutSine), RepeatMode.Reverse),
        label = "glow"
    )

    Box(
        modifier = modifier
            .fillMaxHeight()
            .clip(RoundedCornerShape(12.dp))
            .background(
                brush = Brush.verticalGradient(
                    listOf(Color(0xFF2E5A88), Color(0xFF132A42))
                )
            )
            .border(
                width = if (enabled) 2.dp else 1.dp,
                color = if (enabled) RamiColors.Gold.copy(glow) else Color.White.copy(0.1f),
                shape = RoundedCornerShape(12.dp)
            )
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        // Deck pattern
        Canvas(modifier = Modifier.fillMaxSize()) {
            val step = 8.dp.toPx()
            for (i in 0..10) {
                drawLine(
                    color = Color.White.copy(0.05f),
                    start = Offset(0f, i * step),
                    end = Offset(size.width, i * step),
                    strokeWidth = 1f
                )
            }
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("🃏", fontSize = 24.sp)
            Text(
                "$deckSize",
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.Black
            )
        }

        if (enabled) {
            Surface(
                color = RamiColors.Gold,
                shape = RoundedCornerShape(topStart = 12.dp, bottomEnd = 12.dp),
                modifier = Modifier.align(Alignment.BottomEnd)
            ) {
                Text(
                    "سحب",
                    color = Color.Black,
                    fontSize = 8.sp,
                    fontWeight = FontWeight.Black,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                )
            }
        }
    }
}


// ─── Nazoul drop zone (appears during drag) ───────────────────────────────────

@Composable
private fun NazoulDropZone(
    visible: Boolean,
    isOver: Boolean,
    onRectUpdate: (Rect) -> Unit,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible  = visible,
        enter    = fadeIn(tween(150)) + scaleIn(tween(150), initialScale = 0.8f),
        exit     = fadeOut(tween(120)) + scaleOut(tween(120), targetScale = 0.8f),
        modifier = modifier
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .onGloballyPositioned { coords ->
                    val pos = coords.positionInRoot()
                    onRectUpdate(
                        Rect(
                            left   = pos.x,
                            top    = pos.y,
                            right  = pos.x + coords.size.width,
                            bottom = pos.y + coords.size.height
                        )
                    )
                }
                .clip(RoundedCornerShape(12.dp))
                .background(
                    if (isOver) RamiColors.Gold.copy(0.35f) else Color(0xFF1B5E20).copy(0.7f)
                )
                .border(
                    width = 2.dp,
                    color = if (isOver) RamiColors.Gold else RamiColors.Gold.copy(0.5f),
                    shape = RoundedCornerShape(12.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.padding(4.dp)
            ) {
                Text(
                    if (isOver) "✓" else "⬇",
                    fontSize = 18.sp,
                    color    = RamiColors.Gold
                )
                Text(
                    "نزول هنا",
                    color      = if (isOver) RamiColors.Gold else RamiColors.Gold.copy(0.8f),
                    fontSize   = 10.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign  = TextAlign.Center
                )
            }
        }
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
                onClick  = onOpenBuilder,
                shape    = RoundedCornerShape(12.dp),
                colors   = ButtonDefaults.buttonColors(containerColor = RamiColors.Gold),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("⬇ نزول", color = RamiColors.DarkGreen, fontWeight = FontWeight.Bold, fontSize = 13.sp)
            }
        }
        if (isActPhase && selectedCards.size == 1) {
            Button(
                onClick  = onDiscard,
                shape    = RoundedCornerShape(12.dp),
                colors   = ButtonDefaults.buttonColors(containerColor = Color(0xFFE53935)),
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

// ─── Turn info panel (with timer arc) ────────────────────────────────────────

@Composable
private fun TurnInfoPanel(
    state: GameState,
    localPlayer: Player,
    timerSeconds: Int = 30,
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
            state.turnPhase == TurnPhase.DRAW       -> "سحب"   to RamiColors.Gold
            state.turnPhase == TurnPhase.ACTION     -> "لعب"   to Color(0xFF4CAF50)
            else                                    -> "رمي"   to Color(0xFFFF7043)
        }

        // Phase badge with timer arc
        Box(contentAlignment = Alignment.Center) {
            if (isMyTurn) {
                TurnTimerArc(
                    progress = timerSeconds / 30f,
                    modifier = Modifier.size(50.dp)
                )
            }
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(phaseColor.copy(0.85f))
                    .padding(horizontal = 10.dp, vertical = 4.dp)
            ) {
                Text(phaseLabel, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }

        // Countdown warning when <= 10 seconds
        if (isMyTurn && timerSeconds in 1..10) {
            Text(
                "$timerSeconds ث",
                color      = Color(0xFFE53935),
                fontSize   = 11.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(Modifier.height(4.dp))
        Text(
            "دور ${state.currentPlayer.name}",
            color    = RamiColors.TextLight.copy(0.6f),
            fontSize = 9.sp,
            textAlign = TextAlign.Center,
            maxLines = 2
        )
        Text("جولة ${state.roundNumber}", color = RamiColors.TextLight.copy(0.4f), fontSize = 9.sp)
        Text(
            "خسارة: ${state.scoreLimit} نقطة",
            color    = RamiColors.Gold.copy(0.55f),
            fontSize = 8.sp,
            textAlign = TextAlign.Center
        )
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

// ─── Individual hand card ─────────────────────────────────────────────────────

@Composable
private fun HandCard(
    card: Card,
    idx: Int,
    isSelected: Boolean,
    isNewest: Boolean,
    onToggle: (String) -> Unit,
    allSelectedIds: Set<String>,
    allCards: List<Card>,
    onDragStart: ((List<Card>, Offset) -> Unit)?,
    onDragMove: ((Offset) -> Unit)?,
    onDragEnd: (() -> Unit)?
) {
    var cardAbsPos by remember { mutableStateOf(Offset.Zero) }

    val cardModifier = Modifier
        .width(CardSize.Width)
        .height(CardSize.Height)
        .offset(y = if (isSelected) (-14).dp else 0.dp)
        .zIndex(if (isSelected) 100f + idx else idx.toFloat())
        .onGloballyPositioned { coords -> cardAbsPos = coords.positionInRoot() }
        .then(
            if (onDragStart != null) {
                Modifier.pointerInput(card.id, allSelectedIds) {
                    detectDragGesturesAfterLongPress(
                        onDragStart = { localOffset ->
                            val absStart = cardAbsPos + localOffset
                            val toDrag = if (card.id in allSelectedIds)
                                allCards.filter { it.id in allSelectedIds }
                            else listOf(card)
                            onDragStart.invoke(toDrag, absStart)
                        },
                        onDrag = { change, dragAmount ->
                            change.consume()
                            onDragMove?.invoke(dragAmount)
                        },
                        onDragEnd    = { onDragEnd?.invoke() },
                        onDragCancel = { onDragEnd?.invoke() }
                    )
                }
            } else Modifier
        )

    if (isNewest) {
        FlipInCard(card = card, selected = isSelected, onClick = { onToggle(card.id) }, modifier = cardModifier)
    } else {
        DealAnimatedCard(card = card, selected = isSelected, delayMs = idx * 25, onClick = { onToggle(card.id) }, modifier = cardModifier)
    }
}

// ─── Hand row — LazyRow with natural card overlap, full-size cards ────────────

@Composable
fun LandscapeHandFan(
    cards: List<Card>,
    selectedIds: Set<String>,
    onToggle: (String) -> Unit,
    newestCardId: String? = null,
    onDragStart: ((List<Card>, Offset) -> Unit)? = null,
    onDragMove: ((Offset) -> Unit)? = null,
    onDragEnd: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    if (cards.isEmpty()) return
    LazyRow(
        modifier              = modifier.fillMaxHeight(),
        horizontalArrangement = Arrangement.spacedBy((-18).dp),
        contentPadding        = PaddingValues(horizontal = 8.dp, vertical = 8.dp)
    ) {
        itemsIndexed(cards, key = { _, c -> c.id }) { idx, card ->
            HandCard(
                card          = card,
                idx           = idx,
                isSelected    = card.id in selectedIds,
                isNewest      = card.id == newestCardId,
                onToggle      = onToggle,
                allSelectedIds = selectedIds,
                allCards      = cards,
                onDragStart   = onDragStart,
                onDragMove    = onDragMove,
                onDragEnd     = onDragEnd
            )
        }
    }
}
