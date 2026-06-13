package com.rami.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
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
                    brush = Brush.radialGradient(
                        0.0f to Color(0xFF1B4332),
                        0.6f to Color(0xFF0A2016),
                        1.0f to Color(0xFF05100B)
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            // ── Layer 1: Ambient light glow ─────────────────────────────────────
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .drawBehind {
                        drawCircle(
                            brush = Brush.radialGradient(
                                0f to RamiColors.Gold.copy(0.08f),
                                1f to Color.Transparent
                            ),
                            radius = size.minDimension * 0.8f,
                            center = Offset(size.width * 0.5f, size.height * 0.3f)
                        )
                    }
            )

            // ── Layer 2: floating suit symbols ─────────────────────────────────
            FloatingSuits()

            // ── Layer 3: main content ──────────────────────────────────────────
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(vertical = 48.dp, horizontal = 32.dp)
            ) {
                // Logo area
                LogoHeader()

                // Actions Card
                Surface(
                    color = Color.White.copy(0.03f),
                    shape = RoundedCornerShape(28.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(0.08f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 20.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        ModeButton(
                            emoji     = "🌍",
                            arabic    = "لعب جماعي مباشر",
                            english   = "Live Multiplayer",
                            onClick   = { onNavigate(Screen.Auth) },
                            highlight = true
                        )
                        
                        HorizontalDivider(color = Color.White.copy(0.05f), thickness = 1.dp, modifier = Modifier.padding(horizontal = 20.dp))
                        
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            SmallModeButton(
                                emoji = "🤖",
                                label = "عادي",
                                modifier = Modifier.weight(1f),
                                onClick = { onNavigate(Screen.Lobby(GameMode.NORMAL)) }
                            )
                            SmallModeButton(
                                emoji = "⭐",
                                label = "تفضيل",
                                modifier = Modifier.weight(1f),
                                onClick = { onNavigate(Screen.Lobby(GameMode.TAFDHIL)) }
                            )
                        }
                    }
                }

                // Quick-access bottom bar
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        QuickNavButton("🏆", "البطولات",  { onNavigate(Screen.Auth) }, Modifier.weight(1f))
                        QuickNavButton("📊", "الترتيب",   { onNavigate(Screen.Auth) }, Modifier.weight(1f))
                        QuickNavButton("🎁", "المكافأة",   { onNavigate(Screen.Auth) }, Modifier.weight(1f))
                        QuickNavButton("🛒", "المتجر",    { onNavigate(Screen.Auth) }, Modifier.weight(1f))
                    }
                    Row(
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment     = Alignment.CenterVertically,
                        modifier              = Modifier.fillMaxWidth()
                    ) {
                        TextButton(onClick = { onNavigate(Screen.Rules) }) {
                            Text("📖 القواعد", color = RamiColors.TextLight.copy(0.4f), fontSize = 11.sp)
                        }
                        Text("رامي تونسي  •  v1.2", color = RamiColors.TextLight.copy(0.15f), fontSize = 10.sp)
                        TextButton(onClick = { onNavigate(Screen.Privacy) }) {
                            Text("🔏 الشروط", color = RamiColors.TextLight.copy(0.4f), fontSize = 11.sp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SmallModeButton(
    emoji: String,
    label: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        modifier = modifier.height(54.dp),
        shape = RoundedCornerShape(16.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = Color.White.copy(0.05f),
            contentColor = RamiColors.TextLight
        ),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(0.1f))
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(emoji, fontSize = 18.sp)
            Text(label, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        }
    }
}

// ── Animated logo area ────────────────────────────────────────────────────────

@Composable
private fun LogoHeader() {
    val infiniteTransition = rememberInfiniteTransition(label = "logo")
    val floatAnim by infiniteTransition.animateFloat(
        initialValue = -8f,
        targetValue = 8f,
        animationSpec = infiniteRepeatable(
            animation = tween(3000, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "float"
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.graphicsLayer { translationY = floatAnim }
    ) {
        // Decorative Fan
        Box(contentAlignment = Alignment.Center, modifier = Modifier.height(80.dp)) {
            listOf(-25f, -12f, 0f, 12f, 25f).forEachIndexed { i, rot ->
                Surface(
                    color = Color.White,
                    shape = RoundedCornerShape(4.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color.Black.copy(0.1f)),
                    modifier = Modifier
                        .size(40.dp, 60.dp)
                        .graphicsLayer { 
                            rotationZ = rot
                            transformOrigin = TransformOrigin(0.5f, 1f)
                            alpha = 0.6f + (i * 0.1f)
                        }
                        .shadow(4.dp)
                ) {}
            }
            Text("🃏", fontSize = 40.sp, modifier = Modifier.offset(y = (-10).dp))
        }

        Spacer(Modifier.height(16.dp))

        Text(
            text = "RAMI",
            fontSize = 48.sp,
            fontWeight = FontWeight.Black,
            color = Color.White,
            letterSpacing = 8.sp,
            modifier = Modifier.alpha(0.9f)
        )
        Text(
            text = "TUNISIEN",
            fontSize = 20.sp,
            fontWeight = FontWeight.Light,
            color = RamiColors.Gold,
            letterSpacing = 4.sp
        )
        
        Spacer(Modifier.height(8.dp))
        
        Box(
            modifier = Modifier
                .width(40.dp)
                .height(3.dp)
                .background(RamiColors.Gold, RoundedCornerShape(2.dp))
        )
    }
}


// ── Animated floating suit symbols ───────────────────────────────────────────

@Composable
private fun FloatingSuits() {
    val t = rememberInfiniteTransition(label = "suits")

    // Brighter alpha ranges (was 0.03–0.13, now 0.08–0.22)
    val a1 by t.animateFloat(0.18f, 0.40f,
        infiniteRepeatable(tween(2100), RepeatMode.Reverse, StartOffset(0)),    label = "s1")
    val a2 by t.animateFloat(0.15f, 0.36f,
        infiniteRepeatable(tween(1800), RepeatMode.Reverse, StartOffset(400)),  label = "s2")
    val a3 by t.animateFloat(0.20f, 0.42f,
        infiniteRepeatable(tween(2400), RepeatMode.Reverse, StartOffset(800)),  label = "s3")
    val a4 by t.animateFloat(0.14f, 0.34f,
        infiniteRepeatable(tween(1900), RepeatMode.Reverse, StartOffset(200)),  label = "s4")
    val a5 by t.animateFloat(0.18f, 0.40f,
        infiniteRepeatable(tween(2200), RepeatMode.Reverse, StartOffset(600)),  label = "s5")
    val a6 by t.animateFloat(0.15f, 0.36f,
        infiniteRepeatable(tween(1700), RepeatMode.Reverse, StartOffset(1000)), label = "s6")
    val a7 by t.animateFloat(0.20f, 0.42f,
        infiniteRepeatable(tween(2000), RepeatMode.Reverse, StartOffset(300)),  label = "s7")
    val a8 by t.animateFloat(0.14f, 0.34f,
        infiniteRepeatable(tween(2300), RepeatMode.Reverse, StartOffset(700)),  label = "s8")

    // Slow individual rotations per symbol
    val r1 by t.animateFloat(0f, 360f,
        infiniteRepeatable(tween(18000, easing = LinearEasing)), label = "r1")
    val r2 by t.animateFloat(360f, 0f,
        infiniteRepeatable(tween(22000, easing = LinearEasing)), label = "r2")
    val r3 by t.animateFloat(0f, 360f,
        infiniteRepeatable(tween(16000, easing = LinearEasing)), label = "r3")
    val r4 by t.animateFloat(360f, 0f,
        infiniteRepeatable(tween(24000, easing = LinearEasing)), label = "r4")
    val r5 by t.animateFloat(0f, 360f,
        infiniteRepeatable(tween(20000, easing = LinearEasing)), label = "r5")
    val r6 by t.animateFloat(360f, 0f,
        infiniteRepeatable(tween(14000, easing = LinearEasing)), label = "r6")
    val r7 by t.animateFloat(0f, 360f,
        infiniteRepeatable(tween(26000, easing = LinearEasing)), label = "r7")
    val r8 by t.animateFloat(360f, 0f,
        infiniteRepeatable(tween(19000, easing = LinearEasing)), label = "r8")

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val w = maxWidth
        val h = maxHeight
        val red = Color(0xFFB71C1C)

        Text("♠", fontSize = 46.sp, color = Color.White.copy(alpha = a1),
             modifier = Modifier.offset(x = w * 0.06f, y = h * 0.07f)
                 .graphicsLayer { rotationZ = r1 })
        Text("♥", fontSize = 38.sp, color = red.copy(alpha = a2),
             modifier = Modifier.offset(x = w * 0.82f, y = h * 0.12f)
                 .graphicsLayer { rotationZ = r2 })
        Text("♦", fontSize = 42.sp, color = red.copy(alpha = a3),
             modifier = Modifier.offset(x = w * 0.05f, y = h * 0.50f)
                 .graphicsLayer { rotationZ = r3 })
        Text("♣", fontSize = 34.sp, color = Color.White.copy(alpha = a4),
             modifier = Modifier.offset(x = w * 0.86f, y = h * 0.42f)
                 .graphicsLayer { rotationZ = r4 })
        Text("♠", fontSize = 50.sp, color = Color.White.copy(alpha = a5),
             modifier = Modifier.offset(x = w * 0.55f, y = h * 0.88f)
                 .graphicsLayer { rotationZ = r5 })
        Text("♥", fontSize = 36.sp, color = red.copy(alpha = a6),
             modifier = Modifier.offset(x = w * 0.20f, y = h * 0.82f)
                 .graphicsLayer { rotationZ = r6 })
        Text("♦", fontSize = 44.sp, color = red.copy(alpha = a7),
             modifier = Modifier.offset(x = w * 0.46f, y = h * 0.03f)
                 .graphicsLayer { rotationZ = r7 })
        Text("♣", fontSize = 40.sp, color = Color.White.copy(alpha = a8),
             modifier = Modifier.offset(x = w * 0.73f, y = h * 0.62f)
                 .graphicsLayer { rotationZ = r8 })
    }
}

// ── Quick nav button (icon + label for the bottom bar grid) ──────────────────

@Composable
private fun QuickNavButton(emoji: String, label: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    OutlinedButton(
        onClick  = onClick,
        modifier = modifier.height(52.dp),
        shape    = RoundedCornerShape(14.dp),
        border   = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(0.08f)),
        colors   = ButtonDefaults.outlinedButtonColors(contentColor = RamiColors.TextLight)
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(emoji, fontSize = 15.sp)
            Text(label, fontSize = 9.sp, color = RamiColors.TextLight.copy(0.55f))
        }
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
        modifier  = Modifier.fillMaxWidth().height(60.dp),
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
            Text(emoji, fontSize = 22.sp)
            Column {
                Text(arabic,  fontSize = 17.sp, fontWeight = FontWeight.Bold)
                Text(english, fontSize = 10.sp,
                    color = if (highlight) RamiColors.DarkGreen.copy(alpha = 0.7f)
                            else RamiColors.TextLight.copy(alpha = 0.55f))
            }
        }
    }
}

