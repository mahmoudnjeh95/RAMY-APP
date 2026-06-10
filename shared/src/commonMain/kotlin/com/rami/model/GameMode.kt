package com.rami.model

/**
 * Two distinct rule-sets described in GDD §1.
 *
 * @param displayAr      Arabic label shown in UI
 * @param jokerCount     Total jokers in the double-deck (GDD §1.1)
 * @param jokerValue     Point value of each joker (GDD §2)
 * @param minFirstNazoul Minimum point value to lay down for the first time (GDD §4.2)
 */
enum class GameMode(
    val displayAr: String,
    val displayEn: String,
    val jokerCount: Int,
    val jokerValue: Int,
    val minFirstNazoul: Int
) {
    NORMAL(
        displayAr      = "عادي",
        displayEn      = "Normal",
        jokerCount     = 4,   // 2×52 + 4 Jokers = 108 cards
        jokerValue     = 10,
        minFirstNazoul = 51
    ),
    TAFDHIL(
        displayAr      = "تفضيل",
        displayEn      = "Tafdhil",
        jokerCount     = 6,   // 2×52 + 6 Jokers = 110 cards
        jokerValue     = 50,
        minFirstNazoul = 71
    );

    /** Total deck size for this mode */
    val deckSize get() = 104 + jokerCount
}
