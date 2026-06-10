package com.rami.ui.components

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rami.online.model.Emote
import com.rami.online.model.EmoteEvent
import com.rami.ui.theme.RamiColors
import kotlinx.coroutines.delay

/** Floating button + expandable emote picker. */
@Composable
fun EmoteButton(
    onEmoteSend: (Emote) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }

    Box(modifier) {
        AnimatedVisibility(
            visible = expanded,
            enter = fadeIn() + expandVertically(expandFrom = Alignment.Bottom),
            exit  = fadeOut() + shrinkVertically(shrinkTowards = Alignment.Bottom),
            modifier = Modifier.align(Alignment.BottomEnd).offset(y = (-52).dp)
        ) {
            LazyVerticalGrid(
                columns = GridCells.Fixed(4),
                modifier = Modifier
                    .width(200.dp)
                    .background(RamiColors.DarkGreen.copy(alpha = 0.95f), RoundedCornerShape(12.dp))
                    .padding(8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                items(Emote.entries) { emote ->
                    Text(
                        emote.emoji, fontSize = 26.sp, textAlign = TextAlign.Center,
                        modifier = Modifier
                            .clickable {
                                onEmoteSend(emote)
                                expanded = false
                            }
                            .padding(4.dp)
                    )
                }
            }
        }

        Box(
            Modifier
                .size(44.dp)
                .background(RamiColors.Gold.copy(alpha = 0.85f), RoundedCornerShape(22.dp))
                .clickable { expanded = !expanded }
                .align(Alignment.BottomEnd),
            contentAlignment = Alignment.Center
        ) { Text("😊", fontSize = 22.sp) }
    }
}

/** Floating toast that shows when someone sends an emote. Auto-dismisses after 2s. */
@Composable
fun EmoteToast(event: EmoteEvent?, modifier: Modifier = Modifier) {
    var visible by remember { mutableStateOf(false) }
    var current by remember { mutableStateOf<EmoteEvent?>(null) }

    LaunchedEffect(event) {
        if (event != null) {
            current = event
            visible = true
            delay(2_000)
            visible = false
        }
    }

    AnimatedVisibility(
        visible = visible,
        enter = fadeIn() + slideInVertically(),
        exit  = fadeOut() + slideOutVertically(),
        modifier = modifier
    ) {
        current?.let { e ->
            Row(
                Modifier
                    .background(Color.Black.copy(alpha = 0.7f), RoundedCornerShape(20.dp))
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(e.fromUsername.take(10), color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Text(e.emote.emoji, fontSize = 22.sp)
            }
        }
    }
}
