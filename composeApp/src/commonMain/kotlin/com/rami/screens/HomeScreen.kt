package com.rami.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
                .background(RamiColors.DarkGreen),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(20.dp),
                modifier = Modifier.padding(horizontal = 32.dp)
            ) {
                // ── Logo / title ───────────────────────────────────────────────
                Text(text = "🃏", fontSize = 72.sp, textAlign = TextAlign.Center)

                Text(
                    text       = "رامي تونسي",
                    fontSize   = 38.sp,
                    fontWeight = FontWeight.Bold,
                    color      = RamiColors.Gold,
                    textAlign  = TextAlign.Center
                )
                Text(
                    text      = "Rami Tunisien",
                    fontSize  = 16.sp,
                    color     = RamiColors.TextLight.copy(alpha = 0.6f),
                    textAlign = TextAlign.Center
                )

                Spacer(Modifier.height(12.dp))

                // ── Mode buttons ───────────────────────────────────────────────
                ModeButton(
                    emoji   = "🌐",
                    arabic  = "العب أونلاين",
                    english = "Play Online  •  Real Players",
                    onClick = { onNavigate(Screen.Auth) },
                    highlight = true
                )
                ModeButton(
                    emoji     = "🎴",
                    arabic    = "عادي ضد الروبوت",
                    english   = "vs AI  •  Normal  •  51 pts min",
                    onClick   = { onNavigate(Screen.Lobby(GameMode.NORMAL)) }
                )
                ModeButton(
                    emoji     = "⭐",
                    arabic    = "تفضيل ضد الروبوت",
                    english   = "vs AI  •  Tafdhil  •  71 pts min",
                    onClick   = { onNavigate(Screen.Lobby(GameMode.TAFDHIL)) }
                )

                Spacer(Modifier.height(8.dp))

                TextButton(onClick = { /* TODO: settings */ }) {
                    Text(
                        "⚙️  الإعدادات  •  Settings",
                        color    = RamiColors.TextLight.copy(alpha = 0.5f),
                        fontSize = 13.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun ModeButton(
    emoji: String,
    arabic: String,
    english: String,
    onClick: () -> Unit,
    highlight: Boolean = false
) {
    Button(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().height(76.dp),
        shape  = RoundedCornerShape(18.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = if (highlight) RamiColors.Gold else RamiColors.Gold.copy(alpha = 0.15f),
            contentColor   = if (highlight) RamiColors.DarkGreen else RamiColors.TextLight
        ),
        border = if (!highlight) androidx.compose.foundation.BorderStroke(1.dp, RamiColors.Gold.copy(alpha = 0.4f)) else null,
        elevation = ButtonDefaults.buttonElevation(defaultElevation = if (highlight) 6.dp else 2.dp)
    ) {
        Row(
            verticalAlignment    = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(emoji,   fontSize = 28.sp)
            Column {
                Text(arabic,  fontSize = 20.sp, fontWeight = FontWeight.Bold)
                Text(english, fontSize = 11.sp)
            }
        }
    }
}
