package com.rami.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rami.model.DailyRewards
import com.rami.model.RewardType
import com.rami.ui.theme.RamiColors
import com.rami.ui.theme.RamiTheme

@Composable
fun DailyRewardScreen(
    currentDay:    Int,       // 1-7, which day the user is on
    lastClaimDay:  Int,       // 0 = never, 1-7 = last claimed day index
    onClaim:       () -> Unit,
    onBack:        () -> Unit
) {
    val alreadyClaimedToday = lastClaimDay == currentDay
    val rewards = DailyRewards.cycle

    RamiTheme {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(Color(0xFF0A1F12), RamiColors.DarkGreen, Color(0xFF0D2818))
                    )
                )
        ) {
            Column(
                modifier            = Modifier.fillMaxSize().padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Header
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    TextButton(onClick = onBack) { Text("← رجوع", color = RamiColors.Gold) }
                    Spacer(Modifier.weight(1f))
                    Text(
                        "مكافأة يومية  •  Daily Bonus",
                        color      = RamiColors.Gold,
                        fontSize   = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                HorizontalDivider(color = RamiColors.Gold.copy(0.3f))

                // Animated chest icon
                AnimatedChest(alreadyClaimedToday)

                Text(
                    if (alreadyClaimedToday) "عدت من جديد؟ تعال غداً!" else "افتح مكافأة اليوم!",
                    color      = RamiColors.TextLight,
                    fontSize   = 17.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign  = TextAlign.Center
                )

                Text(
                    "اليوم ${currentDay} من 7  •  Day $currentDay of 7",
                    color    = RamiColors.Gold.copy(0.7f),
                    fontSize = 12.sp
                )

                Spacer(Modifier.height(4.dp))

                // 7-day grid
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    // Row 1: days 1-4
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        rewards.take(4).forEachIndexed { i, reward ->
                            DayCard(
                                reward   = reward,
                                state    = when {
                                    i + 1 < currentDay  -> DayState.CLAIMED
                                    i + 1 == currentDay -> DayState.TODAY
                                    else                -> DayState.LOCKED
                                },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                    // Row 2: days 5-7
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        rewards.drop(4).take(2).forEachIndexed { i, reward ->
                            DayCard(
                                reward   = reward,
                                state    = when {
                                    i + 5 < currentDay  -> DayState.CLAIMED
                                    i + 5 == currentDay -> DayState.TODAY
                                    else                -> DayState.LOCKED
                                },
                                modifier = Modifier.weight(1f)
                            )
                        }
                        // Day 7 - special big reward
                        DayCard(
                            reward   = rewards[6],
                            state    = when {
                                currentDay > 7 -> DayState.CLAIMED
                                7 == currentDay -> DayState.TODAY
                                else            -> DayState.LOCKED
                            },
                            modifier = Modifier.weight(1f),
                            isSpecial = true
                        )
                    }
                }

                Spacer(Modifier.weight(1f))

                // Claim button
                Button(
                    onClick  = onClaim,
                    enabled  = !alreadyClaimedToday,
                    modifier = Modifier.fillMaxWidth().height(60.dp),
                    shape    = RoundedCornerShape(18.dp),
                    colors   = ButtonDefaults.buttonColors(
                        containerColor         = RamiColors.Gold,
                        contentColor           = RamiColors.DarkGreen,
                        disabledContainerColor = Color.Gray.copy(0.3f),
                        disabledContentColor   = RamiColors.TextLight.copy(0.4f)
                    )
                ) {
                    if (alreadyClaimedToday) {
                        Text("✓ تم الاستلام — Come Back Tomorrow", fontSize = 15.sp, fontWeight = FontWeight.Bold)
                    } else {
                        Text(
                            "🎁 استلم مكافأة اليوم  •  Claim Reward",
                            fontSize   = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Text(
                    "احضر كل يوم للحصول على مكافأة اليوم السابع المميزة",
                    color    = RamiColors.TextLight.copy(0.35f),
                    fontSize = 10.sp,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

// ── Day state ─────────────────────────────────────────────────────────────────

private enum class DayState { CLAIMED, TODAY, LOCKED }

// ── Day card ──────────────────────────────────────────────────────────────────

@Composable
private fun DayCard(
    reward:    com.rami.model.DayReward,
    state:     DayState,
    modifier:  Modifier = Modifier,
    isSpecial: Boolean  = false
) {
    val pulse = rememberInfiniteTransition(label = "day_pulse")
    val glowA by pulse.animateFloat(
        0.5f, 1f,
        infiniteRepeatable(tween(900, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "gp"
    )

    val bgColor = when (state) {
        DayState.CLAIMED -> Color(0xFF1B5E20).copy(0.6f)
        DayState.TODAY   -> if (isSpecial) Color(0xFF7B1FA2).copy(0.5f) else Color(0xFF1A3A5C).copy(0.8f)
        DayState.LOCKED  -> Color(0xFF1A1A1A).copy(0.5f)
    }
    val borderColor = when (state) {
        DayState.CLAIMED -> Color(0xFF4CAF50).copy(0.6f)
        DayState.TODAY   -> if (isSpecial) Color(0xFFCE93D8) else RamiColors.Gold
        DayState.LOCKED  -> RamiColors.TextLight.copy(0.15f)
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(bgColor)
            .border(
                width = if (state == DayState.TODAY) 2.dp else 1.dp,
                color = if (state == DayState.TODAY) borderColor.copy(alpha = glowA) else borderColor,
                shape = RoundedCornerShape(12.dp)
            )
            .padding(8.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                "يوم ${reward.day}",
                color    = if (state == DayState.LOCKED) RamiColors.TextLight.copy(0.3f) else RamiColors.TextLight.copy(0.7f),
                fontSize = 8.sp
            )
            Text(
                if (state == DayState.CLAIMED) "✓" else reward.emoji,
                fontSize = if (isSpecial) 28.sp else 22.sp
            )
            Text(
                reward.description,
                color     = when (state) {
                    DayState.CLAIMED -> Color(0xFF4CAF50)
                    DayState.TODAY   -> if (isSpecial) Color(0xFFCE93D8) else RamiColors.Gold
                    DayState.LOCKED  -> RamiColors.TextLight.copy(0.25f)
                },
                fontSize  = 9.sp,
                fontWeight = FontWeight.Bold,
                textAlign  = TextAlign.Center,
                maxLines   = 2
            )
        }
    }
}

// ── Animated chest ────────────────────────────────────────────────────────────

@Composable
private fun AnimatedChest(claimed: Boolean) {
    val anim = rememberInfiniteTransition(label = "chest")
    val bounce by anim.animateFloat(
        0f, if (claimed) 0f else -8f,
        infiniteRepeatable(tween(800, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "cb"
    )
    val glow by anim.animateFloat(
        0.4f, if (claimed) 0.4f else 1f,
        infiniteRepeatable(tween(1200), RepeatMode.Reverse),
        label = "cg"
    )
    Text(
        text     = if (claimed) "🔒" else "🎁",
        fontSize = 64.sp,
        modifier = Modifier.graphicsLayer {
            translationY    = bounce
            shadowElevation = 40f * glow
        }
    )
}
