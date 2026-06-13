package com.rami.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rami.generated.resources.Res
import com.rami.generated.resources.logo_main
import com.rami.ui.theme.RamiColors
import com.rami.ui.theme.RamiTheme
import kotlinx.coroutines.delay
import org.jetbrains.compose.resources.painterResource

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
                val pulse = rememberInfiniteTransition(label = "logo_pulse")
                val scale by pulse.animateFloat(
                    initialValue  = 0.95f,
                    targetValue   = 1.05f,
                    animationSpec = infiniteRepeatable(
                        tween(1800, easing = FastOutSlowInEasing),
                        RepeatMode.Reverse
                    ),
                    label = "lp"
                )

                Image(
                    painter            = painterResource(Res.drawable.logo_main),
                    contentDescription = null,
                    modifier           = Modifier
                        .size(260.dp)
                        .graphicsLayer { scaleX = scale; scaleY = scale },
                    contentScale       = ContentScale.Fit
                )

                Spacer(Modifier.height(24.dp))

                LoadingDots()
            }
        }
    }
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
