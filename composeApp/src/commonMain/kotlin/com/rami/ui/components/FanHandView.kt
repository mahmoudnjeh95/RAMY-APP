package com.rami.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import com.rami.model.Card

/**
 * Renders a list of cards as a fan — each card is slightly rotated and overlapping,
 * like holding a real hand of cards. Selected cards lift above the fan.
 *
 * @param cards       The hand to display (already sorted).
 * @param selectedIds Set of card IDs currently selected.
 * @param onToggle    Called when a card is tapped.
 */
@Composable
fun FanHandView(
    cards: List<Card>,
    selectedIds: Set<String>,
    onToggle: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    if (cards.isEmpty()) return

    val count        = cards.size
    val maxRotation  = 18f                      // total spread in degrees
    val rotPerCard   = if (count > 1) maxRotation / (count - 1) else 0f
    val overlapPx    = if (count > 8) (-18).dp else (-10).dp

    Box(
        modifier          = modifier.fillMaxWidth(),
        contentAlignment  = Alignment.BottomCenter
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(overlapPx),
            verticalAlignment     = Alignment.Bottom,
            modifier              = Modifier.padding(bottom = 12.dp)
        ) {
            cards.forEachIndexed { idx, card ->
                val isSelected = card.id in selectedIds
                // Rotation: left cards tilt left, right cards tilt right
                val rotation = -maxRotation / 2f + idx * rotPerCard
                // Vertical arc: cards at the edges dip slightly
                val arcOffsetDp = if (count > 1) {
                    val normalized = (idx.toFloat() / (count - 1)) * 2f - 1f  // -1..1
                    (normalized * normalized * 10f).dp                          // parabola
                } else 0.dp

                DealAnimatedCard(
                    card      = card,
                    selected  = isSelected,
                    delayMs   = idx * 50,
                    onClick   = { onToggle(card.id) },
                    modifier  = Modifier
                        .selectionLift(isSelected)
                        .offset(y = if (isSelected) 0.dp else arcOffsetDp)
                        .graphicsLayer {
                            rotationZ     = if (isSelected) 0f else rotation
                            transformOrigin = androidx.compose.ui.graphics.TransformOrigin(0.5f, 1.5f)
                        }
                )
            }
        }
    }
}
