package com.rami.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rami.ui.theme.RamiColors
import com.rami.ui.theme.RamiTheme
import kotlinx.coroutines.delay
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun SplashScreen(onFinished: () -> Unit) {

    LaunchedEffect(Unit) {
        delay(2800)
        onFinished()
    }

    RamiTheme {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.radialGradient(
                        colors  = listOf(Color(0xFF0D3320), Color(0xFF051A0D)),
                        radius  = 1200f
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            SplashParticles()

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Animated card fan
                SplashCardFan()

                // Gold divider
                Spacer(
                    Modifier
                        .width(140.dp)
                        .height(1.5.dp)
                        .background(
                            Brush.horizontalGradient(
                                listOf(Color.Transparent, RamiColors.Gold, Color.Transparent)
                            )
                        )
                )

                // Title
                SplashTitle()

                Spacer(Modifier.height(4.dp))

                // Sub-brand
                Text(
                    "Rami Tunisien",
                    color    = RamiColors.TextLight.copy(alpha = 0.45f),
                    fontSize = 14.sp,
                    letterSpacing = 4.sp
                )

                Spacer(Modifier.height(20.dp))

                // Loading dots
                LoadingDots()
            }
        }
    }
}

// ── Animated card fan ─────────────────────────────────────────────────────────

@Composable
private fun SplashCardFan() {
    val anim = rememberInfiniteTransition(label = "fan")
    val spread by anim.animateFloat(
        initialValue  = 0.7f,
        targetValue   = 1f,
        animationSpec = infiniteRepeatable(tween(1800, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label         = "spread"
    )

    val suits   = listOf("♠", "♥", "♦", "♣", "♠")
    val rotations = listOf(-20f, -10f, 0f, 10f, 20f)
    val colors  = listOf(
        Color.White, Color(0xFFE53935), Color(0xFFE53935), Color.White, Color(0xFFD4AF37)
    )

    Row(
        horizontalArrangement = Arrangement.spacedBy((-18).dp),
        verticalAlignment     = Alignment.CenterVertically
    ) {
        suits.forEachIndexed { i, suit ->
            Text(
                text     = suit,
                fontSize = (28 + i * 2).sp,
                color    = colors[i],
                modifier = Modifier.graphicsLayer {
                    rotationZ     = rotations[i] * spread
                    scaleX        = 0.85f + spread * 0.15f
                    scaleY        = 0.85f + spread * 0.15f
                    alpha         = 0.55f + i * 0.09f
                    shadowElevation = 8f
                }
            )
        }
    }
}

// ── Title with pulsing glow ───────────────────────────────────────────────────

@Composable
private fun SplashTitle() {
    val pulse = rememberInfiniteTransition(label = "title_pulse")
    val scale by pulse.animateFloat(
        initialValue  = 0.97f,
        targetValue   = 1.03f,
        animationSpec = infiniteRepeatable(tween(1600, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label         = "ts"
    )
    val glowAlpha by pulse.animateFloat(
        initialValue  = 0.3f,
        targetValue   = 0.7f,
        animationSpec = infiniteRepeatable(tween(1600, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label         = "tg"
    )
    Text(
        text       = "رامي تونسي",
        fontSize   = 42.sp,
        fontWeight = FontWeight.ExtraBold,
        color      = RamiColors.Gold,
        textAlign  = TextAlign.Center,
        modifier   = Modifier.graphicsLayer {
            scaleX          = scale
            scaleY          = scale
            shadowElevation = 30f
        }
    )
}

// ── Floating gold spark particles ─────────────────────────────────────────────

@Composable
private fun SplashParticles() {
    val anim = rememberInfiniteTransition(label = "particles")

    data class Particle(val x: Float, val y: Float, val speed: Int, val offset: Int, val size: Float)

    val particles = remember {
        listOf(
            Particle(0.12f, 0.15f, 3200, 0,    6f),
            Particle(0.80f, 0.22f, 2800, 400,  4f),
            Particle(0.25f, 0.70f, 3600, 800,  5f),
            Particle(0.70f, 0.65f, 2600, 200,  7f),
            Particle(0.50f, 0.10f, 3100, 600,  4f),
            Particle(0.15f, 0.45f, 2900, 1000, 5f),
            Particle(0.88f, 0.50f, 3400, 300,  6f),
            Particle(0.40f, 0.88f, 3000, 700,  4f),
        )
    }

    val alphas = particles.mapIndexed { i, p ->
        anim.animateFloat(
            initialValue  = 0.05f,
            targetValue   = 0.35f,
            animationSpec = infiniteRepeatable(
                tween(p.speed, easing = FastOutSlowInEasing),
                RepeatMode.Reverse,
                StartOffset(p.offset)
            ),
            label = "pa$i"
        )
    }

    val floatYs = particles.mapIndexed { i, p ->
        anim.animateFloat(
            initialValue  = 0f,
            targetValue   = -18f,
            animationSpec = infiniteRepeatable(tween(p.speed), RepeatMode.Reverse, StartOffset(p.offset)),
            label         = "py$i"
        )
    }

    BoxWithConstraints(Modifier.fillMaxSize()) {
        val w = maxWidth
        val h = maxHeight
        particles.forEachIndexed { i, p ->
            val alpha by alphas[i]
            val floatY by floatYs[i]
            Box(
                modifier = Modifier
                    .offset(x = w * p.x, y = h * p.y + floatY.dp)
                    .size(p.size.dp)
                    .drawBehind {
                        drawCircle(RamiColors.Gold.copy(alpha = alpha))
                    }
            )
        }
    }
}

// ── Loading dots ──────────────────────────────────────────────────────────────

@Composable
private fun LoadingDots() {
    val anim = rememberInfiniteTransition(label = "dots")
    val alphas = (0..2).map { i ->
        anim.animateFloat(
            initialValue  = 0.2f,
            targetValue   = 1f,
            animationSpec = infiniteRepeatable(
                tween(600),
                RepeatMode.Reverse,
                StartOffset(i * 200)
            ),
            label = "dot$i"
        )
    }
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        alphas.forEach { a ->
            val alpha by a
            Box(
                Modifier
                    .size(7.dp)
                    .drawBehind { drawCircle(RamiColors.Gold.copy(alpha = alpha)) }
            )
        }
    }
}
