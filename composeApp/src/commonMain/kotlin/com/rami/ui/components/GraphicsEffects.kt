package com.rami.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlin.math.*
import kotlin.random.Random

// ─── Confetti burst (Nazoul celebration) ─────────────────────────────────────

private data class Confetti(
    val x: Float, val y: Float,
    val vx: Float, val vy: Float,
    val color: Color,
    val w: Float, val h: Float,
    val rotZ: Float, val rotSpeed: Float,
    val alpha: Float, val lifespan: Float, val age: Float = 0f
) {
    fun alive() = age < lifespan
    fun next(dt: Float): Confetti {
        val secs = age / 1000f
        val newAge = age + dt
        return copy(
            x     = x + vx * (dt / 1000f),
            y     = y + (vy + 0.6f * secs) * (dt / 1000f),
            rotZ  = rotZ + rotSpeed * (dt / 1000f),
            alpha = (1f - newAge / lifespan).coerceIn(0f, 1f),
            age   = newAge
        )
    }
}

@Composable
fun ConfettiBurst(trigger: Boolean, modifier: Modifier = Modifier) {
    var items by remember { mutableStateOf<List<Confetti>>(emptyList()) }
    val palette = remember {
        listOf(
            Color(0xFFD4AF37), Color(0xFFFFD700), Color(0xFF66BB6A),
            Color(0xFFEF5350), Color(0xFFAB47BC), Color(0xFF42A5F5), Color(0xFFFF7043)
        )
    }
    LaunchedEffect(trigger) {
        if (!trigger) { items = emptyList(); return@LaunchedEffect }
        items = List(55) { i ->
            val angle = (Random.nextFloat() * 360f) * (PI.toFloat() / 180f)
            val speed = Random.nextFloat() * 0.6f + 0.25f
            Confetti(
                x        = 0.5f, y = 0.18f,
                vx       = cos(angle) * speed,
                vy       = sin(angle) * speed - 0.55f,
                color    = palette[i % palette.size],
                w        = Random.nextFloat() * 13f + 5f,
                h        = Random.nextFloat() * 7f + 3f,
                rotZ     = Random.nextFloat() * 360f,
                rotSpeed = (Random.nextFloat() - 0.5f) * 720f,
                alpha    = 1f,
                lifespan = Random.nextFloat() * 700f + 900f
            )
        }
        while (isActive && items.any { it.alive() }) {
            delay(16)
            items = items.map { it.next(16f) }.filter { it.alive() }
        }
        items = emptyList()
    }
    Canvas(modifier) {
        items.forEach { c ->
            val px = c.x * size.width
            val py = c.y * size.height
            rotate(c.rotZ, pivot = Offset(px, py)) {
                drawRect(
                    color   = c.color.copy(alpha = c.alpha),
                    topLeft = Offset(px - c.w / 2f, py - c.h / 2f),
                    size    = Size(c.w, c.h)
                )
            }
        }
    }
}

// ─── Spark burst (joker steal) ────────────────────────────────────────────────

private data class Spark(
    val x: Float, val y: Float,
    val vx: Float, val vy: Float,
    val alpha: Float, val len: Float, val angle: Float,
    val lifespan: Float, val age: Float = 0f, val gold: Boolean
) {
    fun alive() = age < lifespan
    fun next(dt: Float): Spark {
        val newAge = age + dt
        return copy(
            x     = x + vx * (dt / 1000f),
            y     = y + vy * (dt / 1000f),
            alpha = (1f - newAge / lifespan).coerceIn(0f, 1f),
            age   = newAge
        )
    }
}

@Composable
fun SparkBurst(trigger: Boolean, modifier: Modifier = Modifier) {
    var items by remember { mutableStateOf<List<Spark>>(emptyList()) }
    LaunchedEffect(trigger) {
        if (!trigger) { items = emptyList(); return@LaunchedEffect }
        val n = 28
        items = List(n) { i ->
            val angle = (i.toFloat() / n) * 2f * PI.toFloat()
            val speed = Random.nextFloat() * 0.28f + 0.12f
            Spark(
                x = 0.5f, y = 0.5f,
                vx = cos(angle) * speed, vy = sin(angle) * speed,
                alpha = 1f, len = Random.nextFloat() * 22f + 10f,
                angle = angle * (180f / PI.toFloat()),
                lifespan = Random.nextFloat() * 300f + 400f,
                gold = i % 2 == 0
            )
        }
        while (isActive && items.any { it.alive() }) {
            delay(16)
            items = items.map { it.next(16f) }.filter { it.alive() }
        }
        items = emptyList()
    }
    Canvas(modifier) {
        val w = size.width; val h = size.height
        items.forEach { s ->
            val px = s.x * w; val py = s.y * h
            val rad = s.angle * PI.toFloat() / 180f
            drawLine(
                color       = (if (s.gold) Color(0xFFFFD700) else Color(0xFFCE93D8)).copy(alpha = s.alpha),
                start       = Offset(px, py),
                end         = Offset(px + cos(rad) * s.len, py + sin(rad) * s.len),
                strokeWidth = 2.5f
            )
        }
    }
}

// ─── Table felt texture overlay ───────────────────────────────────────────────

@Composable
fun TableTexture(modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val spacing  = 20f
        val dotColor = Color.White.copy(alpha = 0.020f)
        var col = 0
        var x = 0f
        while (x < size.width) {
            var y = if (col % 2 == 0) 0f else spacing / 2f
            while (y < size.height) {
                drawCircle(dotColor, 1f, center = Offset(x, y))
                y += spacing
            }
            x += spacing; col++
        }
        val lineColor = Color.White.copy(alpha = 0.012f)
        val step = 40f
        var i = -size.height
        while (i < size.width + size.height) {
            drawLine(lineColor, Offset(i, 0f), Offset(i + size.height, size.height), 1f)
            drawLine(lineColor, Offset(i, size.height), Offset(i + size.height, 0f), 1f)
            i += step
        }
    }
}

// ─── Turn transition ripple ───────────────────────────────────────────────────

@Composable
fun TurnRipple(trigger: Int, modifier: Modifier = Modifier) {
    var radius by remember { mutableStateOf(0f) }
    var alpha  by remember { mutableStateOf(0f) }
    LaunchedEffect(trigger) {
        radius = 0f; alpha = 0.30f
        while (isActive && radius < 900f) {
            delay(16)
            radius += 28f
            alpha = (0.30f * (1f - radius / 900f)).coerceAtLeast(0f)
        }
        radius = 0f; alpha = 0f
    }
    if (radius > 0f) {
        Canvas(modifier) {
            drawCircle(
                color  = Color(0xFFD4AF37).copy(alpha = alpha),
                radius = radius,
                center = Offset(size.width / 2f, size.height / 2f),
                style  = Stroke(width = 3f)
            )
        }
    }
}

// ─── Floating score delta (+N / -N) ──────────────────────────────────────────

@Composable
fun ScoreDelta(score: Int, modifier: Modifier = Modifier) {
    var prevScore by remember { mutableStateOf(score) }
    var delta   by remember { mutableStateOf(0) }
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(score) {
        val d = score - prevScore
        prevScore = score
        if (d == 0) return@LaunchedEffect
        delta = d; visible = true
        delay(1300)
        visible = false
    }
    AnimatedVisibility(
        visible  = visible,
        enter    = fadeIn() + slideInVertically { it / 2 },
        exit     = fadeOut(tween(500)) + slideOutVertically { -it * 2 },
        modifier = modifier
    ) {
        Text(
            text       = if (delta > 0) "+$delta" else "$delta",
            color      = if (delta > 0) Color(0xFF4CAF50) else Color(0xFFEF5350),
            fontSize   = 13.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

// ─── Infinite gold shimmer (for nazoul banner text) ───────────────────────────

@Composable
fun Modifier.infiniteGoldShimmer(): Modifier {
    val t = rememberInfiniteTransition(label = "gold_shimmer")
    val offset by t.animateFloat(
        initialValue  = -1f,
        targetValue   = 1.8f,
        animationSpec = infiniteRepeatable(
            animation  = tween(1300, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmer_x"
    )
    return drawWithContent {
        drawContent()
        val w = size.width
        drawRect(
            brush = Brush.linearGradient(
                colors  = listOf(
                    Color.Transparent,
                    Color(0xFFFFFFFF).copy(alpha = 0.55f),
                    Color.Transparent
                ),
                start = Offset(offset * w, 0f),
                end   = Offset(offset * w + w * 0.35f, size.height)
            )
        )
    }
}

// ─── One-shot validation shimmer (formation becomes valid) ────────────────────

@Composable
fun Modifier.validationShimmer(isValid: Boolean, nonEmpty: Boolean): Modifier {
    val progress = remember { Animatable(0f) }
    LaunchedEffect(isValid, nonEmpty) {
        if (isValid && nonEmpty) {
            progress.snapTo(0f)
            progress.animateTo(1f, tween(500, easing = LinearEasing))
            delay(150)
            progress.snapTo(0f)
        } else {
            progress.snapTo(0f)
        }
    }
    val p = progress.value
    return if (p > 0f) {
        drawWithContent {
            drawContent()
            val sweep = p * (size.width + 130f) - 65f
            drawRect(
                brush = Brush.linearGradient(
                    colors = listOf(
                        Color.Transparent,
                        Color(0xFFFFD700).copy(alpha = 0.55f),
                        Color.Transparent
                    ),
                    start = Offset(sweep, 0f),
                    end   = Offset(sweep + 130f, size.height)
                )
            )
        }
    } else this
}
