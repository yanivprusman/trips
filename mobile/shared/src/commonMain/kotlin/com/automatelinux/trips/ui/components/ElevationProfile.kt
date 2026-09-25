package com.automatelinux.trips.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.automatelinux.trips.model.Trip
import com.automatelinux.trips.ui.theme.Tp
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.roundToInt

/**
 * The trip seen from the side. Distance runs left to right — even in a Hebrew UI a
 * profile reads that way, as on every trail sign — and the band under the axis is the
 * blaze colour you are following at that point. A vertical line marks where you are.
 */
@Composable
fun ElevationProfile(
    trip: Trip,
    chainageM: Double?,
    modifier: Modifier = Modifier,
    height: Dp = 104.dp,
) {
    val measurer = rememberTextMeasurer()
    val labelStyle = TextStyle(fontSize = 10.sp, color = Tp.Muted, fontFamily = MaterialTheme.typography.labelSmall.fontFamily)
    val prof = trip.profile
    val total = trip.lengthM.toDouble().coerceAtLeast(1.0)
    val eMin = trip.minEleM.toDouble(); val eMax = trip.maxEleM.toDouble()
    val step = if (eMax - eMin > 150) 50.0 else 25.0
    val gMin = floor(eMin / step) * step
    val gMax = (floor(eMax / step) + 1) * step
    val segs = trip.segments
    Canvas(modifier.fillMaxWidth().height(height)) {
        val padL = 30f * density
        val padR = 8f * density
        val padT = 6f * density
        val band = 5f * density
        val padB = 16f * density + band
        val w = size.width - padL - padR
        val h = size.height - padT - padB
        fun x(d: Double) = padL + (d / total * w).toFloat()
        fun y(e: Double) = padT + ((gMax - e) / (gMax - gMin) * h).toFloat()

        // grid + elevation labels
        var g = gMin
        while (g <= gMax + 0.1) {
            val yy = y(g)
            drawLine(Tp.Line, Offset(padL, yy), Offset(padL + w, yy), strokeWidth = 1f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(4f, 6f)))
            val t = measurer.measure("${g.roundToInt()}", labelStyle)
            drawText(t, topLeft = Offset(padL - t.size.width - 4f * density, yy - t.size.height / 2))
            g += step
        }
        if (prof.size >= 2) {
            val line = Path().apply {
                moveTo(x(prof[0].d), y(prof[0].e))
                for (p in prof.drop(1)) lineTo(x(p.d), y(p.e))
            }
            val fill = Path().apply {
                addPath(line)
                lineTo(x(prof.last().d), padT + h); lineTo(x(prof[0].d), padT + h); close()
            }
            drawPath(fill, Brush.verticalGradient(listOf(Tp.Terracotta.copy(alpha = 0.28f), Tp.Terracotta.copy(alpha = 0.03f)), startY = padT, endY = padT + h))
            drawPath(line, Tp.Terracotta, style = Stroke(width = 2.2f * density, cap = StrokeCap.Round))
        }
        // blaze band along the axis
        var acc = 0.0
        for (s in segs) {
            val x0 = x(acc); val x1 = x(acc + s.lengthM)
            drawRect(Tp.marker(s.marker), topLeft = Offset(x0, padT + h + 3f * density), size = Size(max(0f, x1 - x0), band))
            acc += s.lengthM
        }
        // km ticks
        var km = 0
        while (km * 1000 <= total) {
            val xx = x(km * 1000.0)
            val t = measurer.measure(if (km == 0) "0" else "$km ק״מ", labelStyle)
            drawText(t, topLeft = Offset((xx - t.size.width / 2).coerceIn(padL, padL + w - t.size.width), padT + h + band + 5f * density))
            km += 1
        }
        // you
        if (chainageM != null) {
            val xx = x(chainageM.coerceIn(0.0, total))
            var e = prof.firstOrNull()?.e ?: 0.0
            for (i in 1 until prof.size) if (prof[i].d >= chainageM) {
                val a = prof[i - 1]; val b = prof[i]
                val t = if (b.d == a.d) 0.0 else (chainageM - a.d) / (b.d - a.d)
                e = a.e + (b.e - a.e) * t; break
            }
            val yy = y(e)
            drawLine(Tp.Ink.copy(alpha = 0.55f), Offset(xx, padT), Offset(xx, padT + h + band + 3f * density), strokeWidth = 1.2f * density)
            drawCircle(Color.White, radius = 6.5f * density, center = Offset(xx, yy))
            drawCircle(Tp.Sky, radius = 4.5f * density, center = Offset(xx, yy))
        }
    }
}

/** The whole trip as one bar, segment by segment in its blaze colour, with you on it. */
@Composable
fun SegmentBar(trip: Trip, chainageM: Double?, modifier: Modifier = Modifier) {
    val total = trip.lengthM.toFloat().coerceAtLeast(1f)
    // Distance runs left to right here, the same way as on the profile below it.
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
        Box(modifier.fillMaxWidth().height(16.dp), contentAlignment = Alignment.Center) {
            Row(Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp))) {
                trip.segments.forEach { s ->
                    Box(Modifier.weight(s.lengthM / total).height(8.dp).background(Tp.marker(s.marker)))
                }
            }
            if (chainageM != null) {
                val f = (chainageM / total).toFloat().coerceIn(0f, 1f)
                Canvas(Modifier.fillMaxWidth().height(16.dp)) {
                    val xx = f * size.width
                    drawCircle(Color.White, radius = 7f * density, center = Offset(xx, size.height / 2))
                    drawCircle(Tp.Ink, radius = 4.5f * density, center = Offset(xx, size.height / 2))
                }
            }
        }
    }
}
