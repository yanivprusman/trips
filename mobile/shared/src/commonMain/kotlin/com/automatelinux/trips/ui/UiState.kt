package com.automatelinux.trips.ui

import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.ImageBitmap
import com.automatelinux.trips.model.Trip
import com.automatelinux.trips.model.Waypoint

sealed interface LocationState {
    /** The app has not been allowed to know where the phone is. */
    data object NoPermission : LocationState
    /** Allowed, but every provider is switched off in the phone's settings. */
    data object Off : LocationState
    /** Allowed and on; no fix has arrived yet. */
    data object Waiting : LocationState
    data class Fix(val lat: Double, val lon: Double, val accuracyM: Float, val ageS: Long) : LocationState
}

/** What the phone's position means on this trip. */
data class Progress(
    val chainageM: Double,
    val offRouteM: Double,
    val remainingM: Double,
    val remainingGainM: Double,
    val etaMin: Int,
    val elevationM: Double,
    val nextWaypoint: Waypoint?,
    val toNextM: Double,
    val bearingToRoute: Double,
) {
    val offRoute: Boolean get() = offRouteM > OFF_ROUTE_M
    val finished: Boolean get() = remainingM < 40 && chainageM > 800

    companion object { const val OFF_ROUTE_M = 60.0 }
}

data class TripsUiState(
    val trips: List<Trip> = emptyList(),
    val selected: Trip? = null,
    val location: LocationState = LocationState.NoPermission,
    val headingDeg: Float? = null,
    val progress: Progress? = null,
    val follow: Boolean = false,
    val focusedWaypointId: String? = null,
)

interface TripsActions {
    fun openTrip(id: String)
    fun closeTrip()
    fun askLocation()
    fun toggleFollow()
    fun fitTrip()
    fun focusWaypoint(waypoint: Waypoint)
    fun clearFocus()
}

/** How a photo named in the trip file becomes pixels — the platform decides. */
val LocalAssetImage = staticCompositionLocalOf<@androidx.compose.runtime.Composable (String, Int) -> ImageBitmap?> {
    { _, _ -> null }
}
