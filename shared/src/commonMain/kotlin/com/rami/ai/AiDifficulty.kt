package com.rami.ai

/**
 * Controls how long the AI "thinks" and which strategies it uses.
 */
enum class AiDifficulty(
    /** Simulated think delay in ms before each action */
    val thinkDelayMs: Long,
    val displayAr: String,
    val displayEn: String
) {
    EASY(   thinkDelayMs = 1400L, displayAr = "سهل",   displayEn = "Easy"   ),
    MEDIUM( thinkDelayMs = 900L,  displayAr = "متوسط", displayEn = "Medium" ),
    HARD(   thinkDelayMs = 500L,  displayAr = "صعب",   displayEn = "Hard"   )
}
