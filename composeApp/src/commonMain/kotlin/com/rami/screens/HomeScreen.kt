package com.rami.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rami.model.GameMode
import com.rami.navigation.Screen
import com.rami.ui.theme.RamiColors
import com.rami.ui.theme.RamiTheme

@Composable
fun HomeScreen(onNavigate: (Screen) -> Unit) {
    RamiTheme {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = Brush.verticalGradient(
                        listOf(Color(0xFF060F0A), RamiColors.DarkGreen, Color(0xFF0D2818))
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            // ── Layer 1: animated floating suit symbols ─────────────────────────
            FloatingSuits()

            // ── Layer 2: main content ───────────────────────────────────────────
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp),
                modifier = Modifier.padding(horizontal = 32.dp)
            ) {
                // Logo area
                LogoHeader()

                Spacer(Modifier.height(4.dp))

                // Mode buttons
                ModeButton(
                    emoji     = "🌐",
                    arabic    = "العب أونلاين",
                    english   = "Play Online  •  Real Players",
                    onClick   = { onNavigate(Screen.Auth) },
                    highlight = true
                )
                ModeButton(
                    emoji   = "🎴",
                    arabic  = "عادي ضد الروبوت",
                    english = "vs AI  •  Normal  •  51 pts min",
                    onClick = { onNavigate(Screen.Lobby(GameMode.NORMAL)) }
                )
                ModeButton(
                    emoji   = "⭐",
                    arabic  = "تفضيل ضد الروبوت",
                    english = "vs AI  •  Tafdhil  •  71 pts min",
                    onClick = { onNavigate(Screen.Lobby(GameMode.TAFDHIL)) }
                )

                Spacer(Modifier.height(4.dp))

                // Secondary row: Rules + Settings
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedButton(
                        onClick  = { onNavigate(Screen.Rules) },
                        modifier = Modifier.weight(1f),
                        shape    = RoundedCornerShape(14.dp),
                        border   = androidx.compose.foundation.BorderStroke(
                            1.dp, RamiColors.Gold.copy(alpha = 0.5f)
                        ),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = RamiColors.TextLight
                        )
                    ) {
                        Text("📖  القواعد", fontSize = 13.sp)
                    }
                    OutlinedButton(
                        onClick  = { /* TODO: settings */ },
                        modifier = Modifier.weight(1f),
                        shape    = RoundedCornerShape(14.dp),
                        border   = androidx.compose.foundation.BorderStroke(
                            1.dp, RamiColors.Gold.copy(alpha = 0.3f)
                        ),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = RamiColors.TextLight.copy(alpha = 0.5f)
                        )
                    ) {
                        Text("⚙️  إعدادات", fontSize = 13.sp)
                    }
                }

                Text(
                    "v1.0  •  رامي تونسي",
                    color    = RamiColors.TextLight.copy(alpha = 0.2f),
                    fontSize = 10.sp
                )
            }
        }
    }
}

// ── Animated logo area ────────────────────────────────────────────────────────

@Composable
private fun LogoHeader() {
    val pulse = rememberInfiniteTransition(label = "logo")
    val scale by pulse.animateFloat(
        initialValue  = 0.96f,
        targetValue   = 1.04f,
        animationSpec = infiniteRepeatable(
            animation  = tween(2200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        // Card fan decoration
        Row(
            horizontalArrangement = Arrangement.spacedBy((-12).dp),
            verticalAlignment     = Alignment.CenterVertically,
            modifier              = Modifier.padding(bottom = 4.dp)
        ) {
            listOf(-14f, -7f, 0f, 7f, 14f).forEachIndexed { i, rot ->
                Text(
                    text     = "🃏",
                    fontSize = (20 + i).sp,
                    modifier = Modifier
                        .graphicsLayer { rotationZ = rot; alpha = 0.55f + i * 0.09f }
                )
            }
        }

        // Main title
        Text(
            text      = "رامي تونسي",
            fontSize  = 40.sp,
            fontWeight = FontWeight.ExtraBold,
            color     = RamiColors.Gold,
            textAlign = TextAlign.Center,
            modifier  = Modifier.graphicsLayer { scaleX = scale; scaleY = scale }
        )

        // Gold divider line
        Spacer(
            modifier = Modifier
                .padding(top = 6.dp, bottom = 2.dp)
                .width(120.dp)
                .height(1.5.dp)
                .background(
                    brush = Brush.horizontalGradient(
                        listOf(Color.Transparent, RamiColors.Gold, Color.Transparent)
                    )
                )
        )

        Text(
            text     = "Rami Tunisien",
            fontSize = 14.sp,
            color    = RamiColors.TextLight.copy(alpha = 0.5f)
        )
    }
}

// ── Animated floating suit symbols ───────────────────────────────────────────

@Composable
private fun FloatingSuits() {
    val t = rememberInfiniteTransition(label = "suits")

    val a1 by t.animateFloat(0.04f, 0.13f,
        infiniteRepeatable(tween(2100), RepeatMode.Reverse, StartOffset(0)),    label = "s1")
    val a2 by t.animateFloat(0.03f, 0.11f,
        infiniteRepeatable(tween(1800), RepeatMode.Reverse, StartOffset(400)),  label = "s2")
    val a3 by t.animateFloat(0.04f, 0.12f,
        infiniteRepeatable(tween(2400), RepeatMode.Reverse, StartOffset(800)),  label = "s3")
    val a4 by t.animateFloat(0.03f, 0.10f,
        infiniteRepeatable(tween(1900), RepeatMode.Reverse, StartOffset(200)),  label = "s4")
    val a5 by t.animateFloat(0.04f, 0.13f,
        infiniteRepeatable(tween(2200), RepeatMode.Reverse, StartOffset(600)),  label = "s5")
    val a6 by t.animateFloat(0.03f, 0.11f,
        infiniteRepeatable(tween(1700), RepeatMode.Reverse, StartOffset(1000)), label = "s6")
    val a7 by t.animateFloat(0.04f, 0.12f,
        infiniteRepeatable(tween(2000), RepeatMode.Reverse, StartOffset(300)),  label = "s7")
    val a8 by t.animateFloat(0.03f, 0.10f,
        infiniteRepeatable(tween(2300), RepeatMode.Reverse, StartOffset(700)),  label = "s8")

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val w = maxWidth
        val h = maxHeight
        val red = Color(0xFFB71C1C)

        Text("♠", fontSize = 46.sp, color = Color.White.copy(alpha = a1),
             modifier = Modifier.offset(x = w * 0.06f, y = h * 0.07f))
        Text("♥", fontSize = 38.sp, color = red.copy(alpha = a2),
             modifier = Modifier.offset(x = w * 0.82f, y = h * 0.12f))
        Text("♦", fontSize = 42.sp, color = red.copy(alpha = a3),
             modifier = Modifier.offset(x = w * 0.05f, y = h * 0.50f))
        Text("♣", fontSize = 34.sp, color = Color.White.copy(alpha = a4),
             modifier = Modifier.offset(x = w * 0.86f, y = h * 0.42f))
        Text("♠", fontSize = 50.sp, color = Color.White.copy(alpha = a5),
             modifier = Modifier.offset(x = w * 0.55f, y = h * 0.88f))
        Text("♥", fontSize = 36.sp, color = red.copy(alpha = a6),
             modifier = Modifier.offset(x = w * 0.20f, y = h * 0.82f))
        Text("♦", fontSize = 44.sp, color = red.copy(alpha = a7),
             modifier = Modifier.offset(x = w * 0.46f, y = h * 0.03f))
        Text("♣", fontSize = 40.sp, color = Color.White.copy(alpha = a8),
             modifier = Modifier.offset(x = w * 0.73f, y = h * 0.62f))
    }
}

// ── Mode button ───────────────────────────────────────────────────────────────

@Composable
private fun ModeButton(
    emoji: String,
    arabic: String,
    english: String,
    onClick: () -> Unit,
    highlight: Boolean = false
) {
    Button(
        onClick   = onClick,
        modifier  = Modifier.fillMaxWidth().height(76.dp),
        shape     = RoundedCornerShape(18.dp),
        colors    = ButtonDefaults.buttonColors(
            containerColor = if (highlight) RamiColors.Gold else RamiColors.Gold.copy(alpha = 0.12f),
            contentColor   = if (highlight) RamiColors.DarkGreen else RamiColors.TextLight
        ),
        border    = if (!highlight) androidx.compose.foundation.BorderStroke(
            1.dp, RamiColors.Gold.copy(alpha = 0.35f)
        ) else null,
        elevation = ButtonDefaults.buttonElevation(defaultElevation = if (highlight) 8.dp else 2.dp)
    ) {
        Row(
            verticalAlignment     = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(emoji, fontSize = 28.sp)
            Column {
                Text(arabic,  fontSize = 20.sp, fontWeight = FontWeight.Bold)
                Text(english, fontSize = 11.sp,
                    color = if (highlight) RamiColors.DarkGreen.copy(alpha = 0.7f)
                            else RamiColors.TextLight.copy(alpha = 0.55f))
            }
        }
    }
}

