package com.rami.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.foundation.Image
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import com.rami.generated.resources.Res
import com.rami.generated.resources.*
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource
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

                // Image-based navigation buttons
                Column(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 20.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    NavImageButton(Res.drawable.btn_play_online,  "Play Online")       { onNavigate(Screen.Auth) }
                    NavImageButton(Res.drawable.btn_play_friends, "Play with Friends") { onNavigate(Screen.Lobby(GameMode.NORMAL)) }

                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        NavImageButton(Res.drawable.btn_tournament, "Tournament", Modifier.weight(1f)) { onNavigate(Screen.Auth) }
                        NavImageButton(Res.drawable.btn_ranking,    "Ranking",    Modifier.weight(1f)) { onNavigate(Screen.Auth) }
                    }
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        NavImageButton(Res.drawable.btn_shop,       "Shop",       Modifier.weight(1f)) { onNavigate(Screen.Auth) }
                        NavImageButton(Res.drawable.btn_daily_gift, "Daily Gift", Modifier.weight(1f)) { onNavigate(Screen.Auth) }
                    }

                    NavImageButton(Res.drawable.btn_settings, "Settings") { onNavigate(Screen.Rules) }

                    // Footer
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
private fun NavImageButton(
    res: DrawableResource,
    contentDesc: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    OutlinedButton(
        onClick        = onClick,
        modifier       = modifier.fillMaxWidth().height(56.dp),
        shape          = RoundedCornerShape(14.dp),
        border         = null,
        colors         = ButtonDefaults.outlinedButtonColors(containerColor = Color.Transparent),
        contentPadding = PaddingValues(0.dp)
    ) {
        Image(
            painter            = painterResource(res),
            contentDescription = contentDesc,
            modifier           = Modifier.fillMaxSize(),
            contentScale       = ContentScale.FillBounds
        )
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
        Image(
            painter            = painterResource(Res.drawable.cards_fan),
            contentDescription = null,
            modifier           = Modifier.size(220.dp, 110.dp),
            contentScale       = ContentScale.Fit
        )
        Image(
            painter            = painterResource(Res.drawable.logo_main),
            contentDescription = "Rami Tunisien",
            modifier           = Modifier.size(220.dp, 140.dp).offset(y = (-14).dp),
            contentScale       = ContentScale.Fit
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


