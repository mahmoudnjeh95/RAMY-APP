package com.rami.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rami.model.Formation
import com.rami.model.GameMode
import com.rami.ui.theme.RamiColors

/**
 * Renders one formation lying on the table.
 * Cards overlap slightly for a natural card-game look.
 */
@Composable
fun FormationView(
    formation: Formation,
    mode: GameMode,
    modifier: Modifier = Modifier,
    onFormationClick: ((Formation) -> Unit)? = null
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(RamiColors.DarkGreen.copy(alpha = 0.55f))
            .padding(horizontal = 6.dp, vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Formation type label
        Text(
            text     = formation.type.arabicName,
            color    = RamiColors.Gold.copy(alpha = 0.7f),
            fontSize = 9.sp,
            modifier = Modifier.padding(bottom = 2.dp)
        )

        // Cards in a row with slight overlap
        Row(horizontalArrangement = Arrangement.spacedBy((-6).dp)) {
            formation.cards.forEach { card ->
                CardView(card = card, small = true)
            }
        }

        // Point total
        Text(
            text     = "${formation.pointValue(mode)} pts",
            color    = RamiColors.TextLight.copy(alpha = 0.6f),
            fontSize = 8.sp,
            modifier = Modifier.padding(top = 2.dp)
        )
    }
}
