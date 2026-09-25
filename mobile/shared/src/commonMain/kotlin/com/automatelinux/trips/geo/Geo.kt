package com.automatelinux.trips.geo

import com.automatelinux.trips.model.ProfilePoint
import com.automatelinux.trips.model.Trip
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.asin
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt

data class LatLon(val lat: Double, val lon: Double)

private const val EARTH_M = 6_371_000.0

fun distanceM(a: LatLon, b: LatLon): Double {
    val dLat = (b.lat - a.lat) * PI / 180
    val dLon = (b.lon - a.lon) * PI / 180
    val la1 = a.lat * PI / 180
    val la2 = b.lat * PI / 180
    val h = sin(dLat / 2) * sin(dLat / 2) + cos(la1) * cos(la2) * sin(dLon / 2) * sin(dLon / 2)
    return 2 * EARTH_M * asin(sqrt(h))
}

/** Initial bearing from a to b, degrees clockwise from north. */
fun bearingDeg(a: LatLon, b: LatLon): Double {
    val la1 = a.lat * PI / 180; val la2 = b.lat * PI / 180
    val dLon = (b.lon - a.lon) * PI / 180
    val y = sin(dLon) * cos(la2)
    val x = cos(la1) * sin(la2) - sin(la1) * cos(la2) * cos(dLon)
    return (atan2(y, x) * 180 / PI + 360) % 360
}

/** Where a position sits relative to the route. */
data class Located(
    val chainageM: Double,      // distance along the route of the nearest point
    val offRouteM: Double,      // how far the phone is from that point
    val onRoute: LatLon,        // the nearest point itself
)

/**
 * The trip's route as one polyline with a running distance, so a position can be turned
 * into "how far along, how far off".
 *
 * A loop closes on itself, so the nearest piece of line to the parking lot is ambiguous:
 * metre 0 and metre 3717 are the same spot. [locate] therefore prefers the candidate
 * near where the phone was last seen, and only jumps when the old answer has clearly
 * become wrong. Without that, the progress read-out would flap between "just started"
 * and "done" while you tie your shoes.
 */
class RouteLine(trip: Trip) {
    val points: List<LatLon>
    val segmentOfVertex: IntArray
    val chainage: DoubleArray
    val totalM: Double
    private val cumGain: DoubleArray      // metres of climb from the start to each profile point
    private val profile: List<ProfilePoint> = trip.profile

    init {
        val pts = ArrayList<LatLon>()
        val segs = ArrayList<Int>()
        trip.segments.forEachIndexed { si, s ->
            for (p in s.points) {
                val ll = LatLon(p[0], p[1])
                if (pts.isNotEmpty() && pts.last() == ll) continue
                pts += ll; segs += si
            }
        }
        points = pts
        segmentOfVertex = segs.toIntArray()
        chainage = DoubleArray(pts.size)
        for (i in 1 until pts.size) chainage[i] = chainage[i - 1] + distanceM(pts[i - 1], pts[i])
        totalM = chainage.lastOrNull() ?: 0.0
        cumGain = DoubleArray(profile.size)
        for (i in 1 until profile.size) {
            cumGain[i] = cumGain[i - 1] + max(0.0, profile[i].e - profile[i - 1].e)
        }
    }

    private data class Candidate(val chainageM: Double, val distM: Double, val at: LatLon)

    private fun nearestOnSegment(i: Int, p: LatLon): Candidate {
        val a = points[i]; val b = points[i + 1]
        val kx = cos(a.lat * PI / 180)
        val ax = a.lon * kx; val ay = a.lat; val bx = b.lon * kx; val by = b.lat
        val px = p.lon * kx; val py = p.lat
        val dx = bx - ax; val dy = by - ay
        val l2 = dx * dx + dy * dy
        val t = if (l2 == 0.0) 0.0 else ((px - ax) * dx + (py - ay) * dy) / l2
        val tc = min(1.0, max(0.0, t))
        val q = LatLon(ay + dy * tc, (ax + dx * tc) / kx)
        return Candidate(chainage[i] + tc * (chainage[i + 1] - chainage[i]), distanceM(p, q), q)
    }

    fun locate(p: LatLon, lastChainageM: Double?): Located {
        if (points.size < 2) return Located(0.0, 0.0, p)
        val all = (0 until points.size - 1).map { nearestOnSegment(it, p) }
        val best = all.minBy { it.distM }
        val chosen = if (lastChainageM != null) {
            // stay near where you were unless the old answer is clearly wrong now
            val near = all.filter { abs(it.chainageM - lastChainageM) <= WINDOW_M }.minByOrNull { it.distM }
            if (near != null && near.distM <= best.distM + STICKINESS_M) near else best
        } else {
            // first fix: of the places that fit reasonably well, the earliest metre —
            // standing at the parking lot means "about to start", not "just finished"
            all.filter { it.distM <= best.distM + FIRST_FIX_SLACK_M }.minBy { it.chainageM }
        }
        return Located(chosen.chainageM, chosen.distM, chosen.at)
    }

    fun elevationAt(chainageM: Double): Double {
        if (profile.isEmpty()) return 0.0
        val i = profile.indexOfFirst { it.d >= chainageM }
        if (i <= 0) return profile.first().e
        if (i >= profile.size) return profile.last().e
        val a = profile[i - 1]; val b = profile[i]
        val t = if (b.d == a.d) 0.0 else (chainageM - a.d) / (b.d - a.d)
        return a.e + (b.e - a.e) * t
    }

    /** Metres of climb still ahead from [chainageM] to the end. */
    fun remainingGainM(chainageM: Double): Double {
        if (profile.isEmpty()) return 0.0
        val i = profile.indexOfFirst { it.d >= chainageM }.let { if (it < 0) profile.size - 1 else it }
        return max(0.0, cumGain.last() - cumGain[i])
    }

    fun pointAt(chainageM: Double): LatLon {
        if (points.size < 2) return points.firstOrNull() ?: LatLon(0.0, 0.0)
        val c = chainageM.coerceIn(0.0, totalM)
        var i = 0
        while (i < chainage.size - 2 && chainage[i + 1] < c) i++
        val a = points[i]; val b = points[i + 1]
        val span = chainage[i + 1] - chainage[i]
        val t = if (span == 0.0) 0.0 else (c - chainage[i]) / span
        return LatLon(a.lat + (b.lat - a.lat) * t, a.lon + (b.lon - a.lon) * t)
    }

    private companion object {
        const val WINDOW_M = 350.0       // "near where you were" — a few minutes of walking
        const val STICKINESS_M = 30.0    // the old answer keeps winning until it is this much worse
        const val FIRST_FIX_SLACK_M = 80.0
    }
}

/**
 * Walking time for what is left: 3 km/h on a desert trail, plus Naismith's ten minutes
 * per hundred metres of climb. Rounded to something a person would say.
 */
fun estimateMinutes(distanceM: Double, gainM: Double): Int =
    ((distanceM / 3000.0) * 60.0 + gainM / 100.0 * 10.0).toInt()
