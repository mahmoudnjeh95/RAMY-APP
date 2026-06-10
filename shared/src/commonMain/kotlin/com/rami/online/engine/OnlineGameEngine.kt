package com.rami.online.engine

import com.rami.engine.GameEngine
import com.rami.model.Card
import com.rami.model.Formation
import com.rami.model.GameMode
import com.rami.model.GameState
import com.rami.online.model.EmoteEvent
import com.rami.online.model.GameRoom
import com.rami.online.model.RoomSlot
import com.rami.online.model.RoomStatus
import com.rami.online.service.RoomService
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json

class OnlineGameEngine(
    private val roomService: RoomService,
    private val localUid: String,
    private val scope: CoroutineScope
) {
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    private val _room = MutableStateFlow<GameRoom?>(null)
    val room: StateFlow<GameRoom?> = _room.asStateFlow()

    private val _emotes = MutableSharedFlow<EmoteEvent>(replay = 0, extraBufferCapacity = 10)
    val emotes: SharedFlow<EmoteEvent> = _emotes.asSharedFlow()

    private val _kickedPlayers = MutableSharedFlow<String>(replay = 0, extraBufferCapacity = 4)
    val kickedPlayers: SharedFlow<String> = _kickedPlayers.asSharedFlow()

    val timerEngine = TurnTimerEngine(scope)

    private var roomId: String = ""
    private var innerEngine: GameEngine? = null

    val gameState: StateFlow<GameState?>
        get() = innerEngine?.state?.map { it }?.stateIn(scope, SharingStarted.Eagerly, null)
            ?: MutableStateFlow(null)

    val isMyTurn: Boolean
        get() = room.value?.players?.values
            ?.indexOfFirst { it.uid == localUid }
            ?.let { idx -> innerEngine?.state?.value?.currentPlayerIndex == idx } ?: false

    // ── Room observation ───────────────────────────────────────────────────────

    fun attach(roomId: String) {
        this.roomId = roomId
        scope.launch {
            roomService.observeRoom(roomId).collect { room ->
                _room.value = room
                if (room?.status == RoomStatus.IN_GAME && innerEngine == null) {
                    initLocalEngine(room)
                }
            }
        }
        scope.launch {
            roomService.observeEmotes(roomId).collect { event ->
                event?.let { _emotes.emit(it) }
            }
        }
        scope.launch {
            timerEngine.timedOutPlayer.filterNotNull().collect { timedOutUid ->
                handleTimeout(timedOutUid)
            }
        }
    }

    private fun initLocalEngine(room: GameRoom) {
        val slots = room.players.values.toList()
        innerEngine = GameEngine(
            mode = room.mode,
            playerNames = slots.map { it.username },
            aiIndices = slots.mapIndexedNotNull { i, s -> if (s.isAi) i else null }.toSet(),
            scoreLimit = room.scoreLimit
        )
        val mySlotIndex = slots.indexOfFirst { it.uid == localUid }
        if (mySlotIndex == innerEngine!!.state.value.currentPlayerIndex) {
            timerEngine.startFor(localUid)
        }
    }

    // ── Actions (only allowed on my turn) ─────────────────────────────────────

    fun drawFromDeck() {
        if (!isMyTurn) return
        innerEngine?.drawFromDeck()
        timerEngine.stop()
        pushState()
    }

    fun drawFromDiscard() {
        if (!isMyTurn) return
        innerEngine?.drawFromDiscard()
        timerEngine.stop()
        pushState()
    }

    fun discard(card: Card) {
        if (!isMyTurn) return
        innerEngine?.discard(card)
        pushState()
        advanceTurn()
    }

    fun layDownFormation(cards: List<Card>) {
        if (!isMyTurn) return
        innerEngine?.layDown(cards)
        pushState()
    }

    fun addToFormation(card: Card, formationIndex: Int) {
        if (!isMyTurn) return
        innerEngine?.addCardToFormation(card, formationIndex)
        pushState()
    }

    fun sendEmote(emote: com.rami.online.model.Emote, username: String) {
        scope.launch {
            roomService.sendEmote(
                roomId,
                EmoteEvent(fromUid = localUid, fromUsername = username, emote = emote,
                    timestamp = currentTimeMs())
            )
        }
    }

    // ── Timer & strikes ────────────────────────────────────────────────────────

    private suspend fun handleTimeout(uid: String) {
        val strikes = roomService.recordStrike(roomId, uid)
        if (strikes >= 3) {
            roomService.kickPlayer(roomId, uid)
            innerEngine?.eliminatePlayer(uid)
            _kickedPlayers.emit(uid)
        }
        timerEngine.acknowledgeTimeout()
        advanceTurn()
        pushState()
    }

    private fun advanceTurn() {
        val state = innerEngine?.state?.value ?: return
        val nextPlayerSlot = room.value?.players?.values
            ?.getOrNull(state.currentPlayerIndex)
        val nextUid = nextPlayerSlot?.uid ?: return
        timerEngine.startFor(nextUid)
    }

    // ── State sync ─────────────────────────────────────────────────────────────

    private fun pushState() {
        val state = innerEngine?.state?.value ?: return
        scope.launch {
            roomService.pushGameState(roomId, json.encodeToString(state))
        }
    }

    fun dispose() {
        timerEngine.stop()
    }
}

expect fun currentTimeMs(): Long
