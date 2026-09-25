package com.automatelinux.trips.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * Daylight in the Negev. The map is the picture; the chrome around it is paper on sand,
 * ink for text, one warm accent (terracotta) for the thing to press — and the trail-blaze
 * colours themselves for the route, because on the ground those are what you follow.
 */
object Tp {
    val Sand = Color(0xFFF4EEE3)
    val SandDeep = Color(0xFFE7DCC8)
    val Paper = Color(0xFFFFFDF9)
    val PaperGlass = Color(0xF2FFFDF9)
    val Ink = Color(0xFF1F1A15)
    val Muted = Color(0xFF6F665C)
    val Faint = Color(0xFFA39A8F)
    val Line = Color(0x1F1F1A15)
    val Terracotta = Color(0xFFC2410C)
    val TerracottaSoft = Color(0xFFFFE4D5)
    val Sky = Color(0xFF2E7FC0)
    val SkySoft = Color(0x332E7FC0)
    val Ok = Color(0xFF2E8B3D)
    val Warn = Color(0xFFB45309)
    val WarnSoft = Color(0xFFFFEDD5)

    val TrailGreen = Color(0xFF1F9A3E)
    val TrailBlue = Color(0xFF1D5FC4)
    val TrailRed = Color(0xFFD12E2E)
    val TrailBlack = Color(0xFF262626)
    val Road = Color(0xFF8A8078)

    fun marker(name: String): Color = when (name) {
        "green" -> TrailGreen
        "blue" -> TrailBlue
        "red" -> TrailRed
        "black" -> TrailBlack
        else -> Road
    }

    fun markerName(name: String): String = when (name) {
        "green" -> "ירוק"; "blue" -> "כחול"; "red" -> "אדום"; "black" -> "שחור"; else -> "כביש"
    }
}
