package com.automatelinux.trips.model

import kotlinx.serialization.Serializable

/** One trip, exactly as `assets/trips/<id>.json` describes it (built by scripts/build-trip-*.py). */
@Serializable
data class Trip(
    val id: String,
    val name: String,
    val subtitle: String,
    val kind: String,                 // "loop" | "linear"
    val lengthM: Int,
    val durationMin: Int,
    val gainM: Int,
    val lossM: Int,
    val minEleM: Int,
    val maxEleM: Int,
    val difficulty: String,
    val family: Boolean = false,
    val markers: List<String>,        // trail-blaze colours walked, in order
    val summary: String,
    val description: List<String>,
    val practical: List<Practical> = emptyList(),
    val rules: List<String> = emptyList(),
    val startLat: Double,
    val startLon: Double,
    val bounds: List<Double>,         // [minLon, minLat, maxLon, maxLat]
    val segments: List<Segment>,
    val profile: List<ProfilePoint>,
    val waypoints: List<Waypoint>,
    val linear: LinearAlternative? = null,
    val photos: List<Photo> = emptyList(),
    val credits: List<String> = emptyList(),
    val mapMaxZoom: Int = 16,
) {
    val hero: Photo? get() = photos.firstOrNull { it.hero } ?: photos.firstOrNull()
    val isLoop: Boolean get() = kind == "loop"
}

@Serializable
data class Segment(
    val name: String,
    val marker: String,               // "green" | "blue" | "red" | "black" | "road"
    val kind: String,                 // "trail" | "road"
    val lengthM: Int,
    val points: List<List<Double>>,   // [lat, lon]
)

@Serializable
data class ProfilePoint(val d: Double, val e: Double)

@Serializable
data class Waypoint(
    val id: String,
    val kind: String,                 // start | view | nature | junction | road | climb | finish
    val lat: Double,
    val lon: Double,
    val atM: Int,
    val title: String,
    val text: String,
)

@Serializable
data class Practical(val icon: String, val title: String, val text: String)

@Serializable
data class LinearAlternative(val lengthM: Int, val durationMin: Int, val endWaypointId: String, val text: String)

@Serializable
data class Photo(val file: String, val caption: String, val hero: Boolean = false)
