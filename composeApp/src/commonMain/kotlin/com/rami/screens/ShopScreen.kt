package com.rami.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rami.model.*
import com.rami.ui.theme.RamiColors
import com.rami.ui.theme.RamiTheme

@Composable
fun ShopScreen(
    balance:   CurrencyBalance = CurrencyBalance(),
    vipTier:   VipTier = VipTier.NONE,
    onBuyCoins:  (CoinPack)      -> Unit = {},
    onBuyGems:   (GemPack)       -> Unit = {},
    onBuyVip:    (VipTier)       -> Unit = {},
    onBuyItem:   (CosmeticItem)  -> Unit = {},
    onWatchAd:   () -> Unit = {},
    onBack:      () -> Unit
) {
    var tab by remember { mutableStateOf(ShopCategory.COINS) }

    RamiTheme {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Brush.verticalGradient(listOf(Color(0xFF0A1A10), RamiColors.DarkGreen)))
        ) {
            // ── Top bar ────────────────────────────────────────────────────────
            ShopTopBar(balance = balance, vipTier = vipTier, onBack = onBack)

            // ── Category tabs ──────────────────────────────────────────────────
            ShopTabs(selected = tab, onSelect = { tab = it })

            HorizontalDivider(color = RamiColors.Gold.copy(0.25f))

            // ── Content ────────────────────────────────────────────────────────
            Box(modifier = Modifier.fillMaxSize()) {
                when (tab) {
                    ShopCategory.COINS       -> CoinsTab(ShopCatalogue.coinPacks, onBuyCoins, onWatchAd)
                    ShopCategory.GEMS        -> GemsTab(ShopCatalogue.gemPacks, onBuyGems)
                    ShopCategory.VIP         -> VipTab(current = vipTier, onBuyVip = onBuyVip)
                    ShopCategory.CARD_BACKS  -> CosmeticsTab("ظهر الورق", ShopCatalogue.cardBacks, vipTier, onBuyItem)
                    ShopCategory.TABLE_THEMES-> CosmeticsTab("طاولات", ShopCatalogue.tableThemes, vipTier, onBuyItem)
                    ShopCategory.AVATARS     -> CosmeticsTab("أفاتار", ShopCatalogue.avatarPacks, vipTier, onBuyItem)
                }
            }
        }
    }
}

// ── Top bar with currency display ─────────────────────────────────────────────

@Composable
private fun ShopTopBar(balance: CurrencyBalance, vipTier: VipTier, onBack: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(RamiColors.DarkGreen.copy(0.9f))
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment     = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        TextButton(onClick = onBack) { Text("← رجوع", color = RamiColors.Gold) }

        Text(
            "المتجر  •  Shop",
            color      = RamiColors.Gold,
            fontSize   = 17.sp,
            fontWeight = FontWeight.Bold
        )

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
            if (vipTier != VipTier.NONE) {
                Text(vipTier.badge, fontSize = 16.sp)
            }
            CurrencyBadge("🪙", "${formatCoins(balance.coins)}")
            CurrencyBadge("💎", "${balance.gems}")
        }
    }
}

@Composable
private fun CurrencyBadge(icon: String, amount: String) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(RamiColors.Gold.copy(0.12f))
            .border(1.dp, RamiColors.Gold.copy(0.3f), RoundedCornerShape(20.dp))
            .padding(horizontal = 8.dp, vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(icon, fontSize = 13.sp)
        Text(amount, color = RamiColors.Gold, fontSize = 12.sp, fontWeight = FontWeight.Bold)
    }
}

// ── Category tabs ─────────────────────────────────────────────────────────────

@Composable
private fun ShopTabs(selected: ShopCategory, onSelect: (ShopCategory) -> Unit) {
    val items = listOf(
        ShopCategory.COINS       to ("🪙" to "عملات"),
        ShopCategory.GEMS        to ("💎" to "جواهر"),
        ShopCategory.VIP         to ("👑" to "VIP"),
        ShopCategory.CARD_BACKS  to ("🃏" to "ورق"),
        ShopCategory.TABLE_THEMES to ("🟩" to "طاولة"),
        ShopCategory.AVATARS     to ("👤" to "أفاتار")
    )
    LazyRow(
        modifier       = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(items) { (cat, info) ->
            val (emoji, label) = info
            val isSelected = cat == selected
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(14.dp))
                    .background(if (isSelected) RamiColors.Gold else RamiColors.Gold.copy(0.1f))
                    .border(1.dp, if (isSelected) RamiColors.Gold else RamiColors.Gold.copy(0.25f), RoundedCornerShape(14.dp))
                    .clickable { onSelect(cat) }
                    .padding(horizontal = 12.dp, vertical = 7.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(emoji, fontSize = 16.sp)
                    Text(
                        label,
                        color      = if (isSelected) RamiColors.DarkGreen else RamiColors.TextLight,
                        fontSize   = 10.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                    )
                }
            }
        }
    }
}

// ── Coins tab ─────────────────────────────────────────────────────────────────

@Composable
private fun CoinsTab(
    packs:     List<CoinPack>,
    onBuy:     (CoinPack) -> Unit,
    onWatchAd: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Free coins via ad
        FreeAdBanner(
            emoji       = "📺",
            titleAr     = "شاهد إعلان للحصول على 500 عملة مجانية",
            titleEn     = "Watch an ad — get 500 free coins",
            buttonLabel = "شاهد الآن  •  Watch Now",
            onClick     = onWatchAd
        )

        Text("حزم العملات  •  Coin Packs", color = RamiColors.Gold, fontSize = 15.sp, fontWeight = FontWeight.Bold)

        packs.forEach { pack ->
            CoinPackCard(pack = pack, onBuy = { onBuy(pack) })
        }
    }
}

@Composable
private fun CoinPackCard(pack: CoinPack, onBuy: () -> Unit) {
    val totalCoins = pack.coins + pack.bonusCoins
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(
                if (pack.isPopular || pack.isBestValue)
                    Brush.horizontalGradient(listOf(Color(0xFF1A3A5C), Color(0xFF0D2844)))
                else Brush.horizontalGradient(listOf(RamiColors.DarkGreen, Color(0xFF0A2010)))
            )
            .border(
                width = if (pack.isPopular || pack.isBestValue) 2.dp else 1.dp,
                color = if (pack.isBestValue) RamiColors.Gold else if (pack.isPopular) Color(0xFF4FC3F7) else RamiColors.Gold.copy(0.2f),
                shape = RoundedCornerShape(16.dp)
            )
            .clickable { onBuy() }
            .padding(16.dp)
    ) {
        Row(
            verticalAlignment     = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier              = Modifier.fillMaxWidth()
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("🪙", fontSize = 32.sp)
                Column {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            formatCoins(totalCoins),
                            color      = RamiColors.Gold,
                            fontSize   = 20.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                        if (pack.bonusCoins > 0) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color(0xFFE53935))
                                    .padding(horizontal = 5.dp, vertical = 2.dp)
                            ) {
                                Text("+${formatCoins(pack.bonusCoins)} مجاناً", color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                    Text("عملة", color = RamiColors.TextLight.copy(0.5f), fontSize = 11.sp)
                }
            }
            Column(horizontalAlignment = Alignment.End) {
                if (pack.isPopular) Text("الأكثر شعبية", color = Color(0xFF4FC3F7), fontSize = 9.sp)
                if (pack.isBestValue) Text("أفضل قيمة ✨", color = RamiColors.Gold, fontSize = 9.sp)
                Text(
                    pack.priceDisplay,
                    color      = Color.White,
                    fontSize   = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

// ── Gems tab ──────────────────────────────────────────────────────────────────

@Composable
private fun GemsTab(packs: List<GemPack>, onBuy: (GemPack) -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("الجواهر  •  Gems", color = RamiColors.Gold, fontSize = 15.sp, fontWeight = FontWeight.Bold)
        Text(
            "الجواهر للعناصر الحصرية وميزات VIP",
            color    = RamiColors.TextLight.copy(0.5f),
            fontSize = 12.sp
        )
        packs.forEach { pack ->
            GemPackCard(pack = pack, onBuy = { onBuy(pack) })
        }
    }
}

@Composable
private fun GemPackCard(pack: GemPack, onBuy: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Brush.horizontalGradient(listOf(Color(0xFF1A0830), Color(0xFF0D0520))))
            .border(
                width = if (pack.isPopular) 2.dp else 1.dp,
                color = if (pack.isPopular) Color(0xFFCE93D8) else Color(0xFF7B1FA2).copy(0.4f),
                shape = RoundedCornerShape(16.dp)
            )
            .clickable { onBuy() }
            .padding(16.dp)
    ) {
        Row(
            verticalAlignment     = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier              = Modifier.fillMaxWidth()
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("💎", fontSize = 32.sp)
                Column {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            "${pack.gems + pack.bonusGems}",
                            color      = Color(0xFFCE93D8),
                            fontSize   = 22.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                        if (pack.bonusGems > 0) {
                            Box(
                                Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color(0xFF7B1FA2))
                                    .padding(horizontal = 5.dp, vertical = 2.dp)
                            ) {
                                Text("+${pack.bonusGems} مجاناً", color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                    Text("جواهر", color = Color(0xFFCE93D8).copy(0.5f), fontSize = 11.sp)
                }
            }
            Column(horizontalAlignment = Alignment.End) {
                if (pack.isPopular) Text("الأكثر شعبية", color = Color(0xFFCE93D8), fontSize = 9.sp)
                Text(pack.priceDisplay, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

// ── VIP tab ───────────────────────────────────────────────────────────────────

@Composable
private fun VipTab(current: VipTier, onBuyVip: (VipTier) -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text("عضوية VIP", color = RamiColors.Gold, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold)
        Text(
            "احصل على مزايا حصرية وبونسات يومية",
            color = RamiColors.TextLight.copy(0.6f), fontSize = 13.sp
        )

        VipTier.entries.filter { it != VipTier.NONE }.forEach { tier ->
            VipTierCard(tier = tier, isCurrent = tier == current, onBuy = { onBuyVip(tier) })
        }
    }
}

@Composable
private fun VipTierCard(tier: VipTier, isCurrent: Boolean, onBuy: () -> Unit) {
    val tierColor = Color(tier.color)
    val pulse = rememberInfiniteTransition(label = "vip_pulse")
    val glowA by pulse.animateFloat(
        if (isCurrent) 0.5f else 0f,
        if (isCurrent) 1f else 0f,
        infiniteRepeatable(tween(1200), RepeatMode.Reverse),
        label = "vg"
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(tierColor.copy(0.08f))
            .border(
                width = if (isCurrent) 2.dp else 1.dp,
                color = tierColor.copy(if (isCurrent) glowA else 0.4f),
                shape = RoundedCornerShape(18.dp)
            )
            .then(if (!isCurrent) Modifier.clickable { onBuy() } else Modifier)
            .padding(18.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier              = Modifier.fillMaxWidth(),
                verticalAlignment     = Alignment.CenterVertically
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(tier.badge, fontSize = 28.sp)
                    Column {
                        Text(
                            "VIP ${tier.displayEn}",
                            color = tierColor, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold
                        )
                        Text(tier.displayAr, color = tierColor.copy(0.7f), fontSize = 12.sp)
                    }
                }
                if (isCurrent) {
                    Box(
                        Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(tierColor.copy(0.2f))
                            .padding(horizontal = 12.dp, vertical = 4.dp)
                    ) {
                        Text("✓ مفعّل", color = tierColor, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                } else {
                    Button(
                        onClick = onBuy,
                        shape   = RoundedCornerShape(12.dp),
                        colors  = ButtonDefaults.buttonColors(containerColor = tierColor)
                    ) {
                        Text("💎 ${tier.priceGems}", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }
            HorizontalDivider(color = tierColor.copy(0.2f))
            VipBenefitRow("🪙", "عملات يومية", "${tier.dailyCoins} عملة/يوم", tierColor)
            VipBenefitRow("⚡", "مضاعف XP", "×${tier.xpMultiplier} نقاط XP", tierColor)
            VipBenefitRow("🎨", "أفاتار حصري", "شارة VIP مميزة", tierColor)
            VipBenefitRow("🃏", "ظهر ورق حصري", "تصميم خاص بالعضوية", tierColor)
            VipBenefitRow("🎁", "مكافأة يومية مضاعفة", "ضعف مكافآت الحضور اليومي", tierColor)
        }
    }
}

@Composable
private fun VipBenefitRow(icon: String, labelAr: String, detail: String, color: Color) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment     = Alignment.CenterVertically
    ) {
        Text(icon, fontSize = 16.sp)
        Column {
            Text(labelAr, color = color.copy(0.9f), fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            Text(detail, color = RamiColors.TextLight.copy(0.45f), fontSize = 10.sp)
        }
    }
}

// ── Cosmetics tab (card backs, tables, avatars) ───────────────────────────────

@Composable
private fun CosmeticsTab(
    titleAr: String,
    items:   List<CosmeticItem>,
    vipTier: VipTier,
    onBuy:   (CosmeticItem) -> Unit
) {
    LazyVerticalGrid(
        columns  = GridCells.Fixed(3),
        modifier = Modifier.fillMaxSize().padding(12.dp),
        verticalArrangement   = Arrangement.spacedBy(10.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        contentPadding        = PaddingValues(bottom = 16.dp)
    ) {
        item(span = { GridItemSpan(3) }) {
            Text(titleAr, color = RamiColors.Gold, fontSize = 15.sp, fontWeight = FontWeight.Bold)
        }
        items(items) { item ->
            CosmeticCard(item = item, playerVip = vipTier, onBuy = { onBuy(item) })
        }
    }
}

@Composable
private fun CosmeticCard(item: CosmeticItem, playerVip: VipTier, onBuy: () -> Unit) {
    val locked = item.requiredVip.ordinal > playerVip.ordinal && !item.isOwned
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(14.dp))
            .background(
                if (item.isEquipped) RamiColors.Gold.copy(0.15f)
                else if (item.isOwned) Color(0xFF1B3A1B).copy(0.8f)
                else RamiColors.DarkGreen.copy(0.5f)
            )
            .border(
                1.5.dp,
                when {
                    item.isEquipped -> RamiColors.Gold
                    item.isOwned    -> Color(0xFF4CAF50).copy(0.5f)
                    locked          -> Color.Gray.copy(0.2f)
                    else            -> RamiColors.Gold.copy(0.25f)
                },
                RoundedCornerShape(14.dp)
            )
            .then(if (!item.isOwned && !locked) Modifier.clickable { onBuy() } else Modifier)
            .padding(10.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(if (locked) "🔒" else item.emoji, fontSize = 28.sp)
            Text(
                item.nameAr,
                color     = if (locked) RamiColors.TextLight.copy(0.3f) else RamiColors.TextLight,
                fontSize  = 10.sp,
                textAlign = TextAlign.Center,
                maxLines  = 2
            )
            when {
                item.isEquipped -> Text("مجهّز ✓", color = RamiColors.Gold, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                item.isOwned    -> Text("مملوك ✓", color = Color(0xFF4CAF50), fontSize = 9.sp)
                locked          -> Text(
                    "VIP ${item.requiredVip.displayEn}+",
                    color = Color(item.requiredVip.color).copy(0.8f),
                    fontSize = 9.sp, fontWeight = FontWeight.Bold
                )
                item.priceGems > 0 -> Text("💎 ${item.priceGems}", color = Color(0xFFCE93D8), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                else -> Text("🪙 ${formatCoins(item.priceCoin)}", color = RamiColors.Gold, fontSize = 10.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

// ── Free ad banner ────────────────────────────────────────────────────────────

@Composable
private fun FreeAdBanner(
    emoji:       String,
    titleAr:     String,
    titleEn:     String,
    buttonLabel: String,
    onClick:     () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Brush.horizontalGradient(listOf(Color(0xFF1A3A1A), Color(0xFF0D2010))))
            .border(1.dp, Color(0xFF4CAF50).copy(0.5f), RoundedCornerShape(16.dp))
            .padding(14.dp)
    ) {
        Row(
            verticalAlignment     = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier              = Modifier.fillMaxWidth()
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(emoji, fontSize = 28.sp)
                Column {
                    Text(titleAr, color = Color(0xFF4CAF50), fontSize = 12.sp, fontWeight = FontWeight.Bold, maxLines = 2)
                    Text(titleEn, color = RamiColors.TextLight.copy(0.4f), fontSize = 10.sp)
                }
            }
            Button(
                onClick = onClick,
                shape   = RoundedCornerShape(12.dp),
                colors  = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32))
            ) {
                Text(buttonLabel, fontSize = 10.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
            }
        }
    }
}

// ── Helpers ───────────────────────────────────────────────────────────────────

private fun formatCoins(amount: Long): String = when {
    amount >= 1_000_000 -> "${amount / 1_000_000}M"
    amount >= 1_000     -> "${amount / 1_000}K"
    else                -> "$amount"
}
