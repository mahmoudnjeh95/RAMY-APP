# 🃏 Rami Tunisien — KMP Skeleton

**Kotlin Multiplatform + Compose Multiplatform** skeleton for the authentic
Tunisian Rami card game, implementing all rules from the GDD v1.0.

---

## Tech Stack

| Layer | Technology |
|-------|-----------|
| Language | Kotlin 2.0 |
| UI (shared) | Compose Multiplatform 1.6 |
| State | `kotlinx.coroutines` StateFlow |
| Android target | AGP 8.2 / minSdk 24 |
| iOS target | iosArm64 / iosSimulatorArm64 |

---

## Project Structure

```
rami-tunisien/
├── shared/                              ← Pure Kotlin game logic (no UI)
│   └── src/commonMain/kotlin/com/rami/
│       ├── model/
│       │   ├── Card.kt                  sealed class Regular | Joker
│       │   ├── Deck.kt                  mutable deal/shuffle helper
│       │   ├── Formation.kt             Set | Sequence on table
│       │   ├── GameMode.kt              NORMAL | TAFDHIL constants
│       │   ├── GameState.kt             immutable state snapshot
│       │   └── Player.kt               per-player state
│       ├── engine/
│       │   ├── GameEngine.kt            StateFlow state machine ← start here
│       │   ├── NormalRules.kt           Normal-mode rule helpers
│       │   └── TafdhilRules.kt          Tafdhil-mode rule helpers
│       ├── validator/
│       │   └── FormationValidator.kt    Set/Sequence/AddCard/ReplaceJoker
│       └── scorer/
│           └── ScoreCalculator.kt       Round scoring + Joker bank + elimination
│
└── composeApp/                          ← Shared Compose UI (Android + iOS)
    └── src/commonMain/kotlin/com/rami/
        ├── App.kt                       Root composable + nav state
        ├── navigation/Screen.kt         Sealed nav destinations
        ├── ui/theme/Theme.kt            Café palette (dark green + gold)
        ├── ui/components/
        │   ├── CardView.kt              Renders Regular / Joker cards
        │   └── FormationView.kt         Formation strip on table
        └── screens/
            ├── HomeScreen.kt            Mode selection
            ├── LobbyScreen.kt           Player setup + AI toggles
            ├── GameScreen.kt            Main table + hand + action bar
            └── ScoreScreen.kt           Round results + second-life offer
```

---

## Getting Started

### Prerequisites
- Android Studio Hedgehog (or newer) with KMP plugin
- Xcode 15+ (iOS only)
- JDK 17

### Run on Android
```bash
./gradlew :composeApp:installDebug
```

### Run on iOS
Open `iosApp/iosApp.xcodeproj` in Xcode, select a simulator, and press ▶.

---

## GDD Rules Implemented ✅

| Rule | Location |
|------|----------|
| Deck sizes (108 / 110) | `GameMode`, `Deck` |
| Deal 15/14 cards | `GameEngine.startGame` |
| Turn phases (Draw → Action → Discard) | `TurnPhase`, `GameEngine` |
| Valid Set (same rank, diff suits, ≤4) | `FormationValidator.checkSet` |
| Valid Sequence (same suit, consecutive) | `FormationValidator.checkSequence` |
| Joker fills gaps | `FormationValidator.canJokersFillGaps` |
| Minimum first Nazoul (51 / 71) | `NormalRules`, `TafdhilRules` |
| Tafdhil escalating Nazoul | `TafdhilRules.canLayDown` |
| Joker steal | `GameEngine.stealJoker` |
| Tafdhil steal bank bonus | `TafdhilRules.doesStealCountForBank` |
| Tafdhil Joker bank (4 → −100) | `ScoreCalculator` |
| Round scoring (−10 winner, +hand losers) | `ScoreCalculator.calculateRound` |
| Score limit + game-over detection | `ScoreCalculator.isGameOver` |
| Second-life buy-in | `ScoreCalculator.applySecondLife` |
| Normal draw penalty | `NormalRules.applyDrawPenalty` |
| Tafdhil draw penalty (all cards lost) | `TafdhilRules.applyDrawPenalty` |
| Deck reshuffle from discard | `GameEngine.ensureNonEmptyDeck` |

---

## Next Steps / TODOs

- [ ] **AI opponent** — `shared/src/commonMain/kotlin/com/rami/ai/AiPlayer.kt`
- [ ] **Multi-formation lay-down UI** — let player split selection into ≥2 formations
- [ ] **Joker steal UI** — tap Joker on table → pick replacement from hand
- [ ] **Tafdhil finish flow** — throw Jokers + assign to opponents UI
- [ ] **Animations** — `CardView` flip, deal, slide via `AnimatedVisibility` / `animateOffsetAsState`
- [ ] **Haptic feedback** — `LocalHapticFeedback` on valid/invalid actions
- [ ] **Persistence** — save/resume game with `DataStore` or `MMKV`
- [ ] **Online multiplayer** — WebSocket / Socket.io via Ktor client

---

## Architecture Notes

- **GameEngine** emits a new `GameState` snapshot for every action. UI is purely reactive.
- All game logic lives in `shared` — zero Android/iOS imports. Write tests in `commonTest`.
- `FormationValidator` is a pure-function `object` — easy to unit-test without mocking.
- `ScoreCalculator.RoundResult` separates base deltas from Joker-bank bonuses for clear auditability.
