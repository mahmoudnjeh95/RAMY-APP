package com.rami.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rami.model.GameMode
import com.rami.navigation.Screen
import com.rami.online.model.GameRoom
import com.rami.online.model.RoomStatus
import com.rami.online.service.RoomService
import com.rami.ui.theme.RamiColors
import com.rami.ui.theme.RamiTheme
import kotlinx.coroutines.launch

@Composable
fun PrivateTableScreen(
    roomService: RoomService,
    localUid: String,
    onRoomReady: (roomId: String) -> Unit,
    onBack: () -> Unit
) {
    var tab by remember { mutableStateOf(0) }                   // 0=Create, 1=Join
    var selectedMode by remember { mutableStateOf(GameMode.NORMAL) }
    var scoreLimit by remember { mutableStateOf(150) }
    var maxPlayers by remember { mutableStateOf(4) }
    var joinCode by remember { mutableStateOf("") }
    var room by remember { mutableStateOf<GameRoom?>(null) }
    var errorMsg by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    // Observe room once created/joined
    LaunchedEffect(room?.roomId) {
        val roomId = room?.roomId ?: return@LaunchedEffect
        roomService.observeRoom(roomId).collect { r ->
            room = r
            if (r?.status == RoomStatus.IN_GAME) onRoomReady(roomId)
        }
    }

    RamiTheme {
        Column(
            modifier = Modifier.fillMaxSize().background(RamiColors.DarkGreen)
                .padding(horizontal = 24.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                TextButton(onClick = onBack) { Text("← رجوع", color = RamiColors.Gold) }
                Spacer(Modifier.weight(1f))
                Text("طاولة خاصة  •  Private Table",
                    color = RamiColors.Gold, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }

            HorizontalDivider(color = RamiColors.Gold.copy(alpha = 0.3f))

            // Show lobby if already in a room
            if (room != null) {
                RoomLobby(room = room!!, localUid = localUid,
                    onStart = {
                        scope.launch {
                            roomService.startGame(room!!.roomId)
                                .onFailure { errorMsg = it.message ?: "فشل البدء" }
                        }
                    },
                    onReady = { ready ->
                        scope.launch { roomService.setReady(room!!.roomId, ready) }
                    }
                )
            } else {
                // Tab: Create / Join
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("أنشئ طاولة" to "Create", "انضم" to "Join").forEachIndexed { i, (ar, en) ->
                        FilterChip(
                            selected = tab == i, onClick = { tab = i; errorMsg = "" },
                            label = { Text("$ar  •  $en", fontWeight = FontWeight.Bold) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = RamiColors.Gold,
                                selectedLabelColor     = RamiColors.DarkGreen,
                                labelColor             = RamiColors.TextLight
                            )
                        )
                    }
                }

                if (tab == 0) {
                    // ── Create ────────────────────────────────────────────────
                    Text("وضع اللعب — Mode", color = RamiColors.TextLight.copy(alpha = 0.7f), fontSize = 13.sp)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        GameMode.entries.forEach { mode ->
                            FilterChip(selected = selectedMode == mode, onClick = { selectedMode = mode },
                                label = { Text(mode.displayAr, fontWeight = FontWeight.Bold) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = RamiColors.Gold,
                                    selectedLabelColor     = RamiColors.DarkGreen,
                                    labelColor             = RamiColors.TextLight))
                        }
                    }
                    Text("عدد اللاعبين — Players", color = RamiColors.TextLight.copy(alpha = 0.7f), fontSize = 13.sp)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(2, 4).forEach { n ->
                            FilterChip(selected = maxPlayers == n, onClick = { maxPlayers = n },
                                label = { Text("$n", fontWeight = FontWeight.Bold) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = RamiColors.Gold,
                                    selectedLabelColor     = RamiColors.DarkGreen,
                                    labelColor             = RamiColors.TextLight))
                        }
                    }
                    Text("حد النقاط — Score Limit", color = RamiColors.TextLight.copy(alpha = 0.7f), fontSize = 13.sp)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(100, 150, 200, 250).forEach { s ->
                            FilterChip(selected = scoreLimit == s, onClick = { scoreLimit = s },
                                label = { Text("$s", fontWeight = FontWeight.Bold) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = RamiColors.Gold,
                                    selectedLabelColor     = RamiColors.DarkGreen,
                                    labelColor             = RamiColors.TextLight))
                        }
                    }
                    Spacer(Modifier.weight(1f))
                    Button(
                        onClick = {
                            scope.launch {
                                isLoading = true
                                roomService.createPrivateRoom(selectedMode, scoreLimit, maxPlayers)
                                    .onSuccess { room = it }
                                    .onFailure { errorMsg = it.message ?: "فشل إنشاء الطاولة" }
                                isLoading = false
                            }
                        },
                        enabled  = !isLoading,
                        modifier = Modifier.fillMaxWidth().height(56.dp),
                        shape    = RoundedCornerShape(16.dp),
                        colors   = ButtonDefaults.buttonColors(containerColor = RamiColors.Gold, contentColor = RamiColors.DarkGreen)
                    ) {
                        if (isLoading) CircularProgressIndicator(modifier = Modifier.size(20.dp), color = RamiColors.DarkGreen, strokeWidth = 2.dp)
                        else Text("إنشاء الطاولة  •  Create Table", fontWeight = FontWeight.Bold)
                    }
                } else {
                    // ── Join ──────────────────────────────────────────────────
                    Text("أدخل الكود  •  Enter Code", color = RamiColors.TextLight.copy(alpha = 0.7f))
                    OutlinedTextField(
                        value = joinCode, onValueChange = { joinCode = it.take(6) },
                        label = { Text("الكود  •  Code") },
                        singleLine = true, modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(imeAction = ImeAction.Done),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor   = RamiColors.Gold,
                            unfocusedBorderColor = RamiColors.TextLight.copy(alpha = 0.3f),
                            focusedLabelColor    = RamiColors.Gold,
                            focusedTextColor     = RamiColors.TextLight,
                            unfocusedTextColor   = RamiColors.TextLight
                        )
                    )
                    Spacer(Modifier.weight(1f))
                    Button(
                        onClick = {
                            scope.launch {
                                isLoading = true
                                roomService.joinByCode(joinCode)
                                    .onSuccess { room = it }
                                    .onFailure { errorMsg = it.message ?: "كود غير صحيح" }
                                isLoading = false
                            }
                        },
                        enabled  = joinCode.length == 6 && !isLoading,
                        modifier = Modifier.fillMaxWidth().height(56.dp),
                        shape    = RoundedCornerShape(16.dp),
                        colors   = ButtonDefaults.buttonColors(containerColor = RamiColors.Gold, contentColor = RamiColors.DarkGreen)
                    ) {
                        if (isLoading) CircularProgressIndicator(modifier = Modifier.size(20.dp), color = RamiColors.DarkGreen, strokeWidth = 2.dp)
                        else Text("انضم  •  Join", fontWeight = FontWeight.Bold)
                    }
                }

                AnimatedVisibility(errorMsg.isNotEmpty()) {
                    Text(errorMsg, color = MaterialTheme.colorScheme.error, fontSize = 13.sp)
                }
            }
        }
    }
}

@Composable
private fun RoomLobby(
    room: GameRoom,
    localUid: String,
    onStart: () -> Unit,
    onReady: (Boolean) -> Unit
) {
    val mySlot = room.players[localUid]
    val allReady = room.players.values.all { it.isReady }

    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        // Invite code
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = RamiColors.Gold.copy(alpha = 0.15f)),
            shape  = RoundedCornerShape(12.dp)
        ) {
            Column(Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("كود الدعوة  •  Invite Code", color = RamiColors.TextLight.copy(alpha = 0.7f), fontSize = 12.sp)
                Text(room.inviteCode, fontSize = 36.sp, fontWeight = FontWeight.Bold,
                    color = RamiColors.Gold, letterSpacing = 8.sp)
                Text("شارك الكود مع أصدقائك  •  Share with friends",
                    color = RamiColors.TextLight.copy(alpha = 0.5f), fontSize = 11.sp)
            }
        }

        // Players list
        room.players.values.forEach { slot ->
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(slot.username, color = RamiColors.TextLight, fontWeight = FontWeight.Medium)
                Text(if (slot.isReady) "✅ جاهز" else "⏳ ينتظر",
                    color = if (slot.isReady) RamiColors.Gold else RamiColors.TextLight.copy(alpha = 0.5f),
                    fontSize = 13.sp)
            }
        }

        // Empty slots
        repeat(room.maxPlayers - room.players.size) {
            Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically) {
                Text("--- فارغ  •  Empty ---",
                    color = RamiColors.TextLight.copy(alpha = 0.3f), fontSize = 13.sp)
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            // Ready toggle
            FilterChip(
                selected = mySlot?.isReady == true,
                onClick  = { onReady(mySlot?.isReady != true) },
                label    = { Text(if (mySlot?.isReady == true) "✅ جاهز" else "جاهز؟", fontWeight = FontWeight.Bold) },
                colors   = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = RamiColors.Gold,
                    selectedLabelColor     = RamiColors.DarkGreen,
                    labelColor             = RamiColors.TextLight)
            )

            if (room.hostUid == localUid) {
                Button(
                    onClick  = onStart,
                    enabled  = allReady && room.players.size >= 2,
                    shape    = RoundedCornerShape(12.dp),
                    colors   = ButtonDefaults.buttonColors(containerColor = RamiColors.Gold, contentColor = RamiColors.DarkGreen)
                ) { Text("ابدأ  •  Start", fontWeight = FontWeight.Bold) }
            }
        }
    }
}
