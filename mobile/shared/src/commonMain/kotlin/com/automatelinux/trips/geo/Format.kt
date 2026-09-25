package com.automatelinux.trips.geo

import kotlin.math.roundToInt

/** "320 מ׳" under a kilometre, "1.2 ק״מ" above it. */
fun formatDistance(m: Double): String {
    if (m < 1000) return "${(m / 10).roundToInt() * 10} מ׳"
    val km = m / 1000
    val s = if (km >= 10) km.roundToInt().toString() else ((km * 10).roundToInt() / 10.0).toString().trimEnd('0').trimEnd('.')
    return "$s ק״מ"
}

fun formatDistance(m: Int): String = formatDistance(m.toDouble())

/** "כ־20 דק׳", "כשעה ו־20 דק׳", "כשעתיים" — how long, the way people say it. */
fun formatDuration(minutes: Int): String {
    val m = ((minutes + 2) / 5) * 5      // nobody plans to the minute
    if (m < 60) return "כ־${maxOf(m, 5)} דק׳"
    val h = m / 60; val r = m % 60
    val hours = when (h) { 1 -> "כשעה"; 2 -> "כשעתיים"; else -> "כ־$h שעות" }
    return if (r == 0) hours else "$hours ו־$r דק׳"
}

fun formatElevation(m: Int): String = "$m מ׳"

/** Compass direction in words, for when the map is not in front of the reader. */
fun compassWord(bearing: Double): String = when (((bearing % 360 + 360) % 360 / 45.0).roundToInt() % 8) {
    0 -> "צפון"; 1 -> "צפון־מזרח"; 2 -> "מזרח"; 3 -> "דרום־מזרח"
    4 -> "דרום"; 5 -> "דרום־מערב"; 6 -> "מערב"; else -> "צפון־מערב"
}
