package com.rami.model

// ─── Currency balance ─────────────────────────────────────────────────────────

data class CurrencyBalance(
    val coins: Long = 1000L,
    val gems:  Int  = 50
)

// ─── VIP tier ─────────────────────────────────────────────────────────────────

enum class VipTier(
    val displayEn: String,
    val displayAr: String,
    val badge:     String,
    val color:     Long,
    val priceGems: Int,
    val dailyCoins: Int,
    val xpMultiplier: Float
) {
    NONE(      "None",    "بدون",   "",    0xFF888888L,   0,   0,  1.0f),
    BRONZE(    "Bronze",  "برونزي", "🥉", 0xFFCD7F32L,  99, 200, 1.2f),
    SILVER(    "Silver",  "فضي",   "🥈", 0xFFC0C0C0L, 249, 500, 1.5f),
    GOLD(      "Gold",   "ذهبي",   "🥇", 0xFFFFD700L, 499, 1000, 2.0f),
    DIAMOND(   "Diamond","ألماس",  "💎", 0xFF00D4FFL, 999, 2500, 3.0f);
}

// ─── Shop item types ──────────────────────────────────────────────────────────

enum class ShopCategory {
    COINS, GEMS, VIP, CARD_BACKS, TABLE_THEMES, AVATARS
}

data class CoinPack(
    val id:           String,
    val coins:        Long,
    val bonusCoins:   Long = 0,
    val priceDisplay: String,
    val isPopular:    Boolean = false,
    val isBestValue:  Boolean = false
)

data class GemPack(
    val id:           String,
    val gems:         Int,
    val bonusGems:    Int = 0,
    val priceDisplay: String,
    val isPopular:    Boolean = false
)

data class CosmeticItem(
    val id:          String,
    val nameAr:      String,
    val nameEn:      String,
    val emoji:       String,
    val priceCoin:   Long = 0,
    val priceGems:   Int  = 0,
    val isOwned:     Boolean = false,
    val isEquipped:  Boolean = false,
    val requiredVip: VipTier = VipTier.NONE
)

// ─── Catalogue singletons ─────────────────────────────────────────────────────

object ShopCatalogue {

    val coinPacks = listOf(
        CoinPack("coins_1",  1_000,  0,       "$0.99"),
        CoinPack("coins_2",  5_000,  500,     "$3.99",  isPopular = true),
        CoinPack("coins_3", 12_000,  2_000,   "$7.99"),
        CoinPack("coins_4", 30_000,  8_000,   "$14.99", isBestValue = true),
        CoinPack("coins_5", 80_000, 30_000,   "$34.99")
    )

    val gemPacks = listOf(
        GemPack("gems_1",  50,   0,    "$0.99"),
        GemPack("gems_2", 150,  20,    "$2.99", isPopular = true),
        GemPack("gems_3", 400,  80,    "$6.99"),
        GemPack("gems_4", 1000, 250,   "$14.99"),
        GemPack("gems_5", 3000, 1000,  "$39.99")
    )

    val cardBacks = listOf(
        CosmeticItem("back_classic",  "كلاسيك",   "Classic",     "🟦", priceCoin = 0,      isOwned = true),
        CosmeticItem("back_gold",     "ذهبي",      "Gold",        "🟨", priceCoin = 5_000),
        CosmeticItem("back_ruby",     "ياقوت",     "Ruby",        "🟥", priceCoin = 8_000),
        CosmeticItem("back_emerald",  "زمرد",      "Emerald",     "🟩", priceGems = 50),
        CosmeticItem("back_night",    "ليلي",      "Night",       "⬛", priceGems = 80,  requiredVip = VipTier.SILVER),
        CosmeticItem("back_diamond",  "ألماس",     "Diamond",     "💎", priceGems = 150, requiredVip = VipTier.GOLD)
    )

    val tableThemes = listOf(
        CosmeticItem("table_felt",    "قماش كلاسيك", "Classic Felt", "🟢", priceCoin = 0,     isOwned = true),
        CosmeticItem("table_marble",  "رخام",         "Marble",       "⬜", priceCoin = 10_000),
        CosmeticItem("table_night",   "ليلي فاخر",   "Luxury Night", "🌙", priceGems = 100, requiredVip = VipTier.BRONZE)
    )

    val avatarPacks = listOf(
        CosmeticItem("ava_default",   "الافتراضي",  "Default",      "👤", priceCoin = 0,     isOwned = true),
        CosmeticItem("ava_lion",      "الأسد",      "Lion",         "🦁", priceCoin = 3_000),
        CosmeticItem("ava_eagle",     "النسر",      "Eagle",        "🦅", priceCoin = 3_000),
        CosmeticItem("ava_fox",       "الثعلب",     "Fox",          "🦊", priceCoin = 3_000),
        CosmeticItem("ava_wolf",      "الذئب",      "Wolf",         "🐺", priceGems = 30),
        CosmeticItem("ava_tiger",     "النمر",      "Tiger",        "🐯", priceGems = 30),
        CosmeticItem("ava_dragon",    "التنين",     "Dragon",       "🐉", priceGems = 80,  requiredVip = VipTier.SILVER),
        CosmeticItem("ava_crown",     "التاج",      "Crown",        "👑", priceGems = 150, requiredVip = VipTier.GOLD)
    )
}

// ─── Daily reward ─────────────────────────────────────────────────────────────

enum class RewardType { COINS, GEMS, CARD_BACK, VIP_TRIAL }

data class DayReward(
    val day:         Int,
    val type:        RewardType,
    val amount:      Int,
    val description: String,
    val emoji:       String
)

object DailyRewards {
    val cycle = listOf(
        DayReward(1, RewardType.COINS, 500,   "500 عملة",     "🪙"),
        DayReward(2, RewardType.COINS, 1000,  "1,000 عملة",   "🪙"),
        DayReward(3, RewardType.GEMS,  10,    "10 جواهر",     "💎"),
        DayReward(4, RewardType.COINS, 2000,  "2,000 عملة",   "🪙"),
        DayReward(5, RewardType.COINS, 3000,  "3,000 عملة",   "🪙"),
        DayReward(6, RewardType.GEMS,  25,    "25 جواهر",     "💎"),
        DayReward(7, RewardType.GEMS,  50,    "50 جواهر + صندوق مكافأة", "🎁")
    )
}
