package com.rami.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rami.ui.theme.RamiColors
import com.rami.ui.theme.RamiTheme

// ─── Data model ───────────────────────────────────────────────────────────────

private data class RuleSection(
    val icon:    String,
    val titleAr: String,
    val titleEn: String,
    val color:   Color,
    val items:   List<RuleItem>
)

private data class RuleItem(
    val ar: String,
    val en: String = ""
)

// ─── Rule content ─────────────────────────────────────────────────────────────

private val RULES = listOf(

    RuleSection(
        icon    = "🎯",
        titleAr = "الهدف",
        titleEn = "Objective",
        color   = Color(0xFF4CAF50),
        items   = listOf(
            RuleItem("تجميع التشكيلات وإفراغ يدك من الأوراق.",
                     "Form combinations and empty your hand."),
            RuleItem("اللاعب الأول الذي يتجاوز حد النقاط يخسر.",
                     "First player to exceed the score limit loses."),
            RuleItem("أقل نقطة في اليد = أقل خسارة في كل دورة.",
                     "Lowest hand value = least penalty per round.")
        )
    ),

    RuleSection(
        icon    = "🔄",
        titleAr = "خطوات الدور",
        titleEn = "Turn Flow",
        color   = RamiColors.Gold,
        items   = listOf(
            RuleItem("١. سحب — اسحب ورقة من الكومة أو من المرمى.",
                     "1. Draw — from deck or discard pile."),
            RuleItem("٢. لعب — ضع تشكيلات، أضف أوراقاً، أو اسرق الجوكر.",
                     "2. Play — lay formations, add cards, or steal Joker."),
            RuleItem("٣. رمي — ارمِ ورقة واحدة لإنهاء دورك.",
                     "3. Discard — throw one card to end your turn.")
        )
    ),

    RuleSection(
        icon    = "🃏",
        titleAr = "التشكيلات",
        titleEn = "Formations",
        color   = Color(0xFF2196F3),
        items   = listOf(
            RuleItem("المجموعة (Set): 3 أو 4 أوراق بنفس الرقم وأنواع مختلفة.",
                     "Set: 3–4 cards of same rank, different suits."),
            RuleItem("مثال: 7♠  7♥  7♦  ✅",
                     "Example: 7♠  7♥  7♦  ✅"),
            RuleItem("المتسلسلة (Suite): 3 أوراق فأكثر من نفس النوع بأرقام متتالية.",
                     "Sequence: 3+ consecutive cards of same suit."),
            RuleItem("مثال: 5♥  6♥  7♥  8♥  ✅",
                     "Example: 5♥  6♥  7♥  8♥  ✅"),
            RuleItem("بعد النزول يمكنك إضافة ورقة لأي تشكيلة على الطاولة.",
                     "After laying down you may extend any table formation.")
        )
    ),

    RuleSection(
        icon    = "⬇️",
        titleAr = "النزول الأول",
        titleEn = "First Lay-Down (Nazoul)",
        color   = RamiColors.LightGold,
        items   = listOf(
            RuleItem("وضع عادي: يجب أن تساوي قيمة التشكيلات 51 نقطة أو أكثر.",
                     "Normal mode: formations must total ≥ 51 pts."),
            RuleItem("وضع تفضيل: يجب تجاوز قيمة نزول اللاعب السابق.",
                     "Tafdhil mode: must exceed the last player's Nazoul value."),
            RuleItem("سحب ورقة من المرمى قبل النزول يُعطيك عقوبة.",
                     "Drawing from discard before Nazoul gives a penalty."),
            RuleItem("بعد النزول يمكنك السحب من المرمى بحرية.",
                     "After Nazoul you may freely draw from discard.")
        )
    ),

    RuleSection(
        icon    = "★",
        titleAr = "الجوكر",
        titleEn = "Joker Rules",
        color   = RamiColors.JokerPurple,
        items   = listOf(
            RuleItem("الجوكر يحل محل أي ورقة في أي تشكيلة.",
                     "Joker substitutes any card in any formation."),
            RuleItem("يمكن سرقة الجوكر من الطاولة بوضع الورقة الصحيحة مكانه.",
                     "Steal a Joker by placing the correct replacement card."),
            RuleItem("في نهاية اللعب: الجوكر في اليد = 50 نقطة عقوبة.",
                     "In hand at round end: Joker = 50 penalty points."),
            RuleItem("في وضع تفضيل: الجوكر في اليد يُودَع في البنك.",
                     "In Tafdhil mode: Joker in hand goes to your bank.")
        )
    ),

    RuleSection(
        icon    = "⭐",
        titleAr = "وضع التفضيل",
        titleEn = "Tafdhil Mode",
        color   = Color(0xFFFF9800),
        items   = listOf(
            RuleItem("الحد الأدنى للنزول الأول هو 71 نقطة.",
                     "Minimum first Nazoul is 71 pts."),
            RuleItem("كل نزول يجب أن يتجاوز النزول السابق في الدورة.",
                     "Each Nazoul must exceed the previous in this round."),
            RuleItem("الجوكر في اليد يُضاف إلى بنكك (من 1 إلى 4 جوكر).",
                     "Joker in hand = added to your bank (1–4 Jokers)."),
            RuleItem("من يجمع 4 جوكر في البنك يطرح كامل قيمة يد خصومه منهم.",
                     "Collect 4 Jokers: subtract all opponents' hand values from your score."),
            RuleItem("سحب المرمى قبل النزول = تخسر جميع أوراق يدك في البنك.",
                     "Draw discard before Nazoul = lose all hand cards to bank.")
        )
    ),

    RuleSection(
        icon    = "📊",
        titleAr = "حساب النقاط",
        titleEn = "Scoring",
        color   = Color(0xFFCF6679),
        items   = listOf(
            RuleItem("من لم ينزل: يأخذ قيمة كامل يده كعقوبة.",
                     "Players who didn't lay down: score full hand value."),
            RuleItem("من نزل: يأخذ فقط قيمة الأوراق المتبقية في يده.",
                     "Players who laid down: score remaining hand cards only."),
            RuleItem("قيمة الأرقام: وجهها المكتوب (أس=11، صبي=12، بنت=13).",
                     "Number cards = face value (A=11, J=12, Q=13, K=14)."),
            RuleItem("جوكر في اليد عند النهاية = 50 نقطة (إلا في تفضيل).",
                     "Joker in hand at end = 50 pts (except in Tafdhil)."),
            RuleItem("من يتجاوز حد النقاط يخرج من اللعبة.",
                     "Exceeding the score limit eliminates a player.")
        )
    ),

    RuleSection(
        icon    = "💸",
        titleAr = "الحياة الثانية",
        titleEn = "Second Life (Buy-In)",
        color   = Color(0xFF00BCD4),
        items   = listOf(
            RuleItem("عند الوصول لحد النقاط يمكنك شراء حياة ثانية مرة واحدة.",
                     "On reaching the limit you may buy one Second Life."),
            RuleItem("تعود برصيد يساوي أعلى لاعب في اللعبة حالياً.",
                     "You re-enter with the highest current player's score."),
            RuleItem("إذا وصلت الحد مرة ثانية تخرج نهائياً.",
                     "If you reach the limit again you are eliminated.")
        )
    )
)

// ─── Screen ───────────────────────────────────────────────────────────────────

@Composable
fun RulesScreen(onBack: () -> Unit) {
    RamiTheme {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(RamiColors.DarkGreen)
        ) {
            // Header
            Row(
                modifier          = Modifier
                    .fillMaxWidth()
                    .background(RamiColors.DarkGreen)
                    .padding(horizontal = 8.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = onBack) {
                    Text("← رجوع", color = RamiColors.Gold, fontSize = 15.sp)
                }
                Spacer(Modifier.weight(1f))
                Text(
                    "📖  قواعد اللعبة",
                    color      = RamiColors.Gold,
                    fontSize   = 20.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.weight(1f))
                Spacer(Modifier.width(72.dp))
            }

            HorizontalDivider(color = RamiColors.Gold.copy(alpha = 0.3f))

            // Rules list
            LazyColumn(
                modifier       = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                items(RULES) { section ->
                    RuleSectionCard(section)
                }
                item { Spacer(Modifier.height(8.dp)) }
            }
        }
    }
}

@Composable
private fun RuleSectionCard(section: RuleSection) {
    Card(
        shape  = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = RamiColors.FeltGreen.copy(alpha = 0.7f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Section header
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier          = Modifier
                        .size(36.dp)
                        .background(section.color.copy(alpha = 0.2f), RoundedCornerShape(10.dp)),
                    contentAlignment  = Alignment.Center
                ) {
                    Text(section.icon, fontSize = 18.sp)
                }
                Spacer(Modifier.width(10.dp))
                Column {
                    Text(
                        text       = section.titleAr,
                        color      = section.color,
                        fontSize   = 17.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text    = section.titleEn,
                        color   = section.color.copy(alpha = 0.6f),
                        fontSize = 11.sp
                    )
                }
            }

            Spacer(Modifier.height(10.dp))
            HorizontalDivider(color = section.color.copy(alpha = 0.2f))
            Spacer(Modifier.height(10.dp))

            // Rule items
            section.items.forEach { item ->
                Row(
                    modifier = Modifier.padding(vertical = 3.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Text(
                        "•",
                        color    = section.color.copy(alpha = 0.7f),
                        fontSize = 13.sp,
                        modifier = Modifier.padding(end = 8.dp, top = 1.dp)
                    )
                    Column {
                        Text(
                            text      = item.ar,
                            color     = RamiColors.TextLight,
                            fontSize  = 14.sp,
                            textAlign = TextAlign.Start
                        )
                        if (item.en.isNotEmpty()) {
                            Text(
                                text     = item.en,
                                color    = RamiColors.TextLight.copy(alpha = 0.45f),
                                fontSize = 11.sp
                            )
                        }
                    }
                }
            }
        }
    }
}
