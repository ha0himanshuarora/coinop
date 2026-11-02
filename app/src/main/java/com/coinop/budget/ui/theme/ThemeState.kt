package com.coinop.budget.ui.theme

enum class UiStyle { ARCADIA, CYBERPUNK }

enum class Face { SYSTEM, LIGHT, DARK }

enum class BackdropStyle(val label: String) {
    DOTS("Dots"), GRID("Grid"), SCAN("Scan"), CHECK("Check"), STRIPE("Stripe"), PLAIN("Plain")
}

enum class FontChoice(val label: String, val tag: String) {
    DEFAULT("App default", "style"),
    SILKSCREEN("Silkscreen", "retro"),
    PRESS_START("Press Start 2P", "retro"),
    VT323("VT323", "retro"),
    RAJDHANI("Rajdhani", "sci-fi"),
    CYBER_HORIZON("Cyber Horizon", "sci-fi"),
    SD_DYSTOPIAN("SD Dystopian", "sci-fi, no digits"),
    SYSTEM("System", "modern"),
}

/** Everything about how the app looks. Stored on-device, instantly applied. */
data class Appearance(
    val uiStyle: UiStyle = UiStyle.ARCADIA,
    val face: Face = Face.SYSTEM,
    val themeId: String = "arcadia",
    val font: FontChoice = FontChoice.DEFAULT,
    val textScale: Float = 1f,
    val backdrop: BackdropStyle = BackdropStyle.DOTS,
    val backdropStrength: Float = 0.4f,
)
